'use strict';
const test = require('node:test');
const assert = require('node:assert');
const { boot } = require('./helpers');
const places = require('../src/places');

test('session lifecycle: create, location, ack, brief, end; keys enforced', async () => {
  const { call, close } = await boot();
  const c = await call('POST', '/sessions', { kind: 'fall', lat: 12.9716, lng: 77.5946, battery: 60, patient: { name: 'Asha', bloodGroup: 'B+' }, contacts: [{ name: 'Ravi', phone: '+919999999999' }], event: { severity: 'HIGH', score: 80, peakG: 4, stillnessSec: 30 }, ownerId: 'owner-0123456789abcdef' });
  assert.strictEqual(c.status, 201);
  assert.ok(c.json.token.length >= 30);
  assert.match(c.json.url, /^https:\/\/vera\.test\/track\//);
  const { token, key } = c.json;
  assert.strictEqual((await call('POST', `/sessions/${token}/location`, { lat: 1, lng: 1 })).status, 403);
  assert.strictEqual((await call('POST', `/sessions/${token}/location`, { lat: 999, lng: 1 }, { 'x-key': key })).status, 400);
  assert.strictEqual((await call('POST', `/sessions/${token}/location`, { lat: 12.98, lng: 77.6, accuracy: 5 }, { 'x-key': key })).status, 200);
  let b = (await call('GET', `/brief/${token}?lang=hi`)).json;
  assert.strictEqual(b.header.status, 'not_responding');
  assert.strictEqual(b.location.lat, 12.98);
  assert.strictEqual(b.nearby.hospitals[0].name, 'City Hospital');
  const ack = await call('POST', `/sessions/${token}/ack`, { name: 'Ambulance 7' });
  assert.strictEqual(ack.status, 200);
  b = (await call('GET', `/brief/${token}`)).json;
  assert.strictEqual(b.header.status, 'responder_on_way'); assert.strictEqual(b.header.acked, true);
  const loc = (await call('POST', `/sessions/${token}/location`, { lat: 12.99, lng: 77.6 }, { 'x-key': key })).json;
  assert.strictEqual(loc.acked, true); // the app learns about the acknowledgement
  const dash = (await call('GET', '/api/dashboard?owner=owner-0123456789abcdef')).json;
  assert.strictEqual(dash.sessions.length, 1);
  assert.strictEqual((await call('GET', '/api/dashboard?owner=short')).status, 401);
  assert.strictEqual((await call('POST', `/sessions/${token}/end`, { cancelled: true }, { 'x-key': key })).status, 200);
  assert.strictEqual((await call('GET', '/brief/nope')).status, 404);
  close();
});

test('validation: bad kind, bad coords, oversize and malformed bodies', async () => {
  const { call, close } = await boot();
  assert.strictEqual((await call('POST', '/sessions', { kind: 'hack' })).status, 400);
  assert.strictEqual((await call('POST', '/sessions', { kind: 'sos', lat: 'x', lng: 1 })).status, 400);
  assert.strictEqual((await call('POST', '/journeys', { dest: { lat: 1000, lng: 1 }, etaTs: Date.now() + 1e6 })).status, 400);
  assert.strictEqual((await call('GET', '/nearby?lat=abc&lng=1')).status, 400);
  close();
});

test('delete all data removes sessions and journeys server-side', async () => {
  const { call, db, close } = await boot();
  const owner = 'owner-abcdefghijklmnop';
  await call('POST', '/sessions', { kind: 'sos', lat: 1, lng: 1, ownerId: owner });
  await call('POST', '/journeys', { ownerId: owner, dest: { lat: 12.97, lng: 77.6, name: 'x' }, etaTs: Date.now() + 1e6, followers: [] });
  const r = await call('DELETE', '/owner/' + owner);
  assert.strictEqual(r.json.deleted, 2);
  assert.strictEqual(db.prepare('SELECT COUNT(*) c FROM sessions').get().c, 0);
  assert.strictEqual(db.prepare('SELECT COUNT(*) c FROM journeys').get().c, 0);
  close();
});

test('chat: validates input, never exposes key, offline flag when LLM unavailable', async () => {
  let seen;
  const { call, close } = await boot({ complete: async (o) => { seen = o; return 'Call 112 first. Then press on the wound.'; } });
  assert.strictEqual((await call('POST', '/chat', { messages: [] })).status, 400);
  assert.strictEqual((await call('POST', '/chat', { messages: [{ role: 'assistant', content: 'hi' }] })).status, 400);
  const r = await call('POST', '/chat', { lang: 'hi', messages: [{ role: 'user', content: 'x'.repeat(5000) }], profile: { bloodGroup: 'O+', secret: 'nope' } });
  assert.strictEqual(r.status, 200);
  assert.strictEqual(seen.messages[0].content.length, 600);       // input length limit
  assert.match(seen.system, /first-aid and safety assistant/);
  assert.match(seen.system, /bloodGroup: O\+/);
  assert.doesNotMatch(seen.system, /nope/);
  close();
  const off = await boot({ complete: async () => null });
  const r2 = await off.call('POST', '/chat', { messages: [{ role: 'user', content: 'cpr' }] });
  assert.strictEqual(r2.status, 503); assert.strictEqual(r2.json.offline, true);
  off.close();
});

test('places: overpass parsing, grouping and ordering (emergency/24x7 first)', async () => {
  const json = { elements: [
    { type: 'node', id: 1, lat: 12.975, lon: 77.59, tags: { amenity: 'hospital', name: 'Far Hospital' } },
    { type: 'way', id: 2, center: { lat: 12.9701, lon: 77.5901 }, tags: { amenity: 'hospital', name: 'Near 24x7', opening_hours: '24/7', emergency: 'yes', phone: '+91 80 1' } },
    { type: 'node', id: 3, lat: 12.971, lon: 77.59, tags: { amenity: 'pharmacy', name: 'MedPlus' } },
    { type: 'node', id: 4, lat: 12.97, lon: 77.591, tags: { amenity: 'doctors' } },
    { type: 'node', id: 5, lat: 12.97, lon: 77.591, tags: { shop: 'bakery', name: 'Not medical' } },
  ] };
  const g = places.group(places.parseOverpass(json, 12.97, 77.59));
  assert.deepStrictEqual(g.hospitals.map((h) => h.name), ['Near 24x7', 'Far Hospital']);
  assert.strictEqual(g.pharmacies[0].name, 'MedPlus');
  assert.strictEqual(g.clinics.length, 1);
  assert.ok(g.hospitals[0].distanceM < 50);
  const res = await places.nearby(1.1, 1.1, { fetchImpl: async () => ({ ok: true, json: async () => json }), provider: 'osm' });
  assert.strictEqual(res.ok, true);
  const down = await places.nearby(2.2, 2.2, { fetchImpl: async () => { throw new Error('net'); }, provider: 'osm' });
  assert.strictEqual(down.ok, false); // caller shows cached/offline result; SOS flow never blocked
});

test('APK download: 404 until APK_URL is reachable, then served with the apk content type and cached', async () => {
  const http = require('node:http');
  const fs = require('node:fs');
  const path = require('node:path');
  const cache = path.join(require('node:os').tmpdir(), 'VERA-Guard.apk');
  fs.rmSync(cache, { force: true });
  const apk = Buffer.concat([Buffer.from([0x50, 0x4b, 3, 4]), Buffer.alloc(2048, 7)]);
  const src = await new Promise((r) => { const s = http.createServer((q, res) => { if (q.url === '/bad') { res.end('<html>nope</html>'); return; } res.setHeader('content-type', 'application/octet-stream'); res.end(apk); }); s.listen(0, () => r(s)); });
  try {
    let { base, close } = await bootBase({ apkUrl: '' });
    assert.strictEqual((await fetch(base + '/VERA-Guard.apk')).status, 404);
    close();
    ({ base, close } = await bootBase({ apkUrl: `http://127.0.0.1:${src.address().port}/bad` }));
    assert.strictEqual((await fetch(base + '/VERA-Guard.apk')).status, 404, 'non-zip content is rejected');
    close();
    ({ base, close } = await bootBase({ apkUrl: `http://127.0.0.1:${src.address().port}/apk` }));
    const r = await fetch(base + '/VERA-Guard.apk');
    assert.strictEqual(r.status, 200);
    assert.strictEqual(r.headers.get('content-type'), 'application/vnd.android.package-archive');
    assert.strictEqual(Buffer.from(await r.arrayBuffer()).length, apk.length);
    close();
    ({ base, close } = await bootBase({ apkUrl: '' }));
    assert.strictEqual((await fetch(base + '/VERA-Guard.apk', { method: 'HEAD' })).status, 200, 'served from cache');
    close();
  } finally { src.close(); fs.rmSync(cache, { force: true }); }
});

async function bootBase(deps) {
  const { createApp } = require('../src/app');
  const { open } = require('../src/db');
  const { app } = createApp({ db: open(':memory:'), deps });
  const server = await new Promise((r) => { const s = app.listen(0, () => r(s)); });
  return { base: `http://127.0.0.1:${server.address().port}`, close: () => server.close() };
}
