'use strict';
const test = require('node:test');
const assert = require('node:assert');
const { boot } = require('./helpers');
const { buildDeterministic, parseLlm, shortSms } = require('../src/brief');

const session = (over = {}) => ({
  token: 'tok', kind: 'fall', status: 'not_responding', created_at: Date.now() - 120000, lat: 12.9716, lng: 77.5946, accuracy: 8, loc_ts: Date.now() - 5000, battery: 55,
  patient: { name: 'Asha', bloodGroup: 'B+' }, contacts: [{ name: 'Ravi', phone: '+919999999999' }],
  event: { severity: 'HIGH', score: 82, peakG: 4.2, freeFallMs: 320, orientationDeg: 85, stillnessSec: 40 }, facilities: null, ...over,
});

test('deterministic sections are correct and missing data is Unknown', async () => {
  const b = await buildDeterministic(session(), 'en', { reverse: async () => 'MG Road, Bengaluru', nearby: { hospitals: [{ name: 'City Hospital', lat: 1, lng: 2, distanceM: 800 }] } });
  assert.strictEqual(b.header.typeLabel, 'Fall');
  const f = Object.fromEntries(b.patient.fields.map((x) => [x.key, x.value]));
  assert.strictEqual(f.name, 'Asha'); assert.strictEqual(f.bloodGroup, 'B+');
  assert.strictEqual(f.allergies, 'Unknown'); assert.strictEqual(f.medications, 'Unknown'); assert.strictEqual(f.age, 'Unknown');
  assert.strictEqual(b.analysis.level, 'HIGH');
  assert.match(b.analysis.interpretation, /high-impact fall followed by 40 s without movement/);
  assert.strictEqual(b.location.address, 'MG Road, Bengaluru');
  assert.ok(b.location.directionsUrl.includes('12.9716,77.5946'));
  assert.match(b.guidance[0], /112 \/ 108/);
  assert.strictEqual(b.summaryAi, false);
});

test('SOS has no sensor analysis; unknown location handled', async () => {
  const b = await buildDeterministic(session({ kind: 'sos', event: null, lat: null, lng: null, loc_ts: null }), 'en', { reverse: async () => null });
  assert.strictEqual(b.analysis, null);
  assert.strictEqual(b.location.unknown, 'Unknown');
  assert.strictEqual(b.nearby.hospitals.length, 0);
});

test('all three languages render and start guidance with the 112/108 call', async () => {
  for (const lang of ['en', 'hi', 'kn']) {
    const b = await buildDeterministic(session(), lang, { reverse: async () => null });
    assert.match(b.guidance[0], /112/);
    assert.ok(b.patient.fields.find((f) => f.key === 'allergies').value.length > 0);
  }
  const hi = await buildDeterministic(session(), 'hi', { reverse: async () => null });
  assert.match(hi.header.typeLabel, /[ऀ-ॿ]/);
  const kn = await buildDeterministic(session(), 'kn', { reverse: async () => null });
  assert.match(kn.header.typeLabel, /[ಀ-೿]/);
});

test('LLM output is used when valid; deterministic fallback when LLM fails or returns junk', async () => {
  const { db, close } = await boot();
  const mk = (tok) => session({ token: tok });
  const ok = await require('../src/brief').buildBrief(db, mk('a1'), 'en', { reverse: async () => null, complete: async () => '{"summary":"A person fell. Status unknown.","guidance":["Call 112 / 108","Do not move them","Check breathing"]}' });
  assert.strictEqual(ok.summaryAi, true); assert.match(ok.summary, /A person fell/);
  const fail = await require('../src/brief').buildBrief(db, mk('a2'), 'en', { reverse: async () => null, complete: async () => null });
  assert.strictEqual(fail.summaryAi, false); assert.match(fail.summary, /Fall at/);
  const junk = await require('../src/brief').buildBrief(db, mk('a3'), 'en', { reverse: async () => null, complete: async () => 'not json' });
  assert.strictEqual(junk.summaryAi, false);
  close();
});

test('LLM guidance missing the emergency call gets it prepended', async () => {
  const { db, close } = await boot();
  const b = await require('../src/brief').buildBrief(db, session({ token: 'b1' }), 'en', { reverse: async () => null, complete: async () => '{"summary":"x","guidance":["Stay calm","Keep warm","Do not move"]}' });
  assert.match(b.guidance[0], /112/);
  close();
});

test('parseLlm and shortSms', () => {
  assert.strictEqual(parseLlm('{"summary":""}'), null);
  assert.deepStrictEqual(parseLlm('xx {"summary":"s","guidance":["a",1]} yy'), { summary: 's', guidance: ['a'] });
  const s = shortSms({ kind: 'fall', event: { severity: 'HIGH', score: 80 }, lat: 12.9716, lng: 77.5946, patient: { bloodGroup: 'O+' }, hospital: 'City Hospital' });
  assert.match(s, /Fall.*HIGH 80.*12.97160,77.59460.*O\+.*City Hospital/);
  assert.doesNotMatch(shortSms({ kind: 'sos', lat: 1, lng: 2, patient: {} }), /Blood/);
});
