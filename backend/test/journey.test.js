'use strict';
const test = require('node:test');
const assert = require('node:assert');
const J = require('../src/journey');
const { distanceToRoute, haversine } = require('../src/util');
const W = require('../src/watchdog');
const { boot } = require('./helpers');

const route = [[12.9700, 77.5900], [12.9700, 77.6000]]; // ~1.08 km east-west
const base = (o = {}) => ({ route, dest_lat: 12.97, dest_lng: 77.6, lat: 12.97, lng: 77.59, eta_ts: Date.now() + 600000, buffer_s: 0, deviated_since: null, stopped: 0, status: 'on_route', watchdog_alerted: 0, ...o });

test('distance to route', () => {
  assert.ok(distanceToRoute(12.9700, 77.5950, route) < 2);
  const off = distanceToRoute(12.9740, 77.5950, route); // ~445 m north
  assert.ok(off > 400 && off < 500, String(off));
  assert.ok(Math.abs(haversine(12.97, 77.59, 12.97, 77.6) - 1084) < 15);
});

test('deviation needs >300 m for >60 s', () => {
  const t0 = 1_000_000;
  assert.strictEqual(J.updateDeviation(base(), 12.9702, 77.595, t0), null);            // on route
  const since = J.updateDeviation(base(), 12.9740, 77.595, t0);                         // 445 m off
  assert.strictEqual(since, t0);
  assert.strictEqual(J.evaluate(base({ deviated_since: t0 }), t0 + 30000), 'on_route'); // < 60 s
  assert.strictEqual(J.evaluate(base({ deviated_since: t0 }), t0 + 61000), 'deviated');
  assert.strictEqual(J.updateDeviation(base({ deviated_since: t0 }), 12.9740, 77.596, t0 + 5000), t0); // keeps original start
});

test('stopped, delayed, arrived, alert', () => {
  assert.strictEqual(J.evaluate(base({ stopped: 1 })), 'stopped');
  assert.strictEqual(J.evaluate(base({ eta_ts: Date.now() - 1000 })), 'delayed');
  assert.strictEqual(J.evaluate(base({ eta_ts: Date.now() - 1000, buffer_s: 600 })), 'on_route'); // buffer respected
  assert.strictEqual(J.evaluate(base({ lat: 12.97, lng: 77.5995 })), 'arrived');              // ~54 m from destination
  assert.strictEqual(J.evaluate(base({ lat: 12.97, lng: 77.598 })), 'on_route');              // ~217 m away
  assert.strictEqual(J.evaluate(base({ watchdog_alerted: 1 })), 'alert');
});

test('nextCheck picks the earliest pending deadline', () => {
  const j = { started_at: 1000, loc_ts: 5000, watchdog_sec: 300, eta_ts: 1_000_000, buffer_s: 0, watchdog_alerted: 0, overdue_alerted: 0 };
  assert.strictEqual(J.nextCheck(j), 5000 + 300000);
  assert.strictEqual(J.nextCheck({ ...j, watchdog_alerted: 1 }), 1_000_000);
  assert.strictEqual(J.nextCheck({ ...j, watchdog_alerted: 1, overdue_alerted: 1 }), null);
  assert.strictEqual(J.nextCheck({ ...j, ended_at: 1 }), null);
});

async function startJourney(call, over = {}) {
  const r = await call('POST', '/journeys', {
    name: 'Asha', phone: '+910000000001', mode: 'cab', dest: { name: 'Office', lat: 12.97, lng: 77.6 }, etaTs: Date.now() + 3600e3, route,
    followers: [{ name: 'Ravi', phone: '+919999999999', lang: 'hi' }, { name: 'Mom', phone: '+918888888888', lang: 'kn' }],
    lat: 12.97, lng: 77.59, battery: 80, demo: true, watchdogSeconds: 120, ...over,
  });
  assert.strictEqual(r.status, 201);
  return r.json;
}

test('watchdog alerts followers when updates stop, survives a "restart", and does not double-send', async () => {
  const { db, call, close } = await boot();
  const jr = await startJourney(call);
  const outbox = () => db.prepare('SELECT to_phone, body FROM outbox').all();
  assert.strictEqual(outbox().length, 2); // start SMS to both followers
  const row = () => db.prepare('SELECT * FROM journeys WHERE token=?').get(jr.token);
  const t0 = row().loc_ts;

  assert.strictEqual(await W.tick(db, t0 + 60_000), 0);                       // not due yet
  assert.strictEqual(outbox().length, 2);
  // Simulated restart = fresh tick call using only DB state; the timer was persisted.
  assert.strictEqual(db.prepare("SELECT run_at FROM jobs WHERE ref=?").get(jr.token).run_at, t0 + 120_000);
  assert.strictEqual(await W.tick(db, t0 + 121_000), 1);
  const ob = outbox();
  assert.strictEqual(ob.length, 4);
  assert.match(ob[2].body, /[ऀ-ॿ]/);  // Hindi follower gets Hindi
  assert.match(ob[3].body, /[ಀ-೿]/);  // Kannada follower gets Kannada
  assert.match(ob[2].body, /maps\.google\.com\/\?q=12\.97,77\.59/);
  assert.strictEqual(row().status, 'alert');
  assert.ok(row().alert_session);
  await W.tick(db, t0 + 200_000);
  assert.strictEqual(outbox().length, 4);       // no duplicate
  // Public page switches to full brief on alert
  const view = await call('GET', `/api/track/${jr.token}?lang=en`);
  assert.strictEqual(view.json.status, 'alert');
  assert.ok(view.json.brief && view.json.brief.header.typeLabel === 'Journey alert');
  close();
});

test('a new location resets the watchdog; overdue journeys alert once; extend re-arms', async () => {
  const { db, call, close } = await boot();
  const jr = await startJourney(call, { etaTs: Date.now() + 5000, bufferS: 0 });
  const H = { 'x-key': jr.key };
  const t0 = db.prepare('SELECT loc_ts FROM journeys WHERE token=?').get(jr.token).loc_ts;
  const loc = await call('POST', `/journeys/${jr.token}/location`, { lat: 12.9701, lng: 77.592, battery: 70 }, H);
  assert.strictEqual(loc.status, 200);
  const t1 = db.prepare('SELECT loc_ts FROM journeys WHERE token=?').get(jr.token).loc_ts;
  assert.ok(t1 >= t0);
  // Overdue (ETA 5 s) well before watchdog window
  await W.tick(db, t1 + 30_000);
  assert.strictEqual(db.prepare('SELECT overdue_alerted FROM journeys WHERE token=?').get(jr.token).overdue_alerted, 1);
  const n = db.prepare('SELECT COUNT(*) c FROM outbox').get().c;
  await W.tick(db, t1 + 40_000);
  assert.strictEqual(db.prepare('SELECT COUNT(*) c FROM outbox').get().c, n);
  const ext = await call('POST', `/journeys/${jr.token}/extend`, { minutes: 30 }, H);
  assert.strictEqual(ext.status, 200);
  assert.strictEqual(db.prepare('SELECT overdue_alerted FROM journeys WHERE token=?').get(jr.token).overdue_alerted, 0);
  close();
});

test('arrival within ~100 m ends the journey and notifies; writes need the key', async () => {
  const { db, call, close } = await boot();
  const jr = await startJourney(call);
  assert.strictEqual((await call('POST', `/journeys/${jr.token}/location`, { lat: 12.97, lng: 77.5 })).status, 403);
  const r = await call('POST', `/journeys/${jr.token}/location`, { lat: 12.97, lng: 77.5995 }, { 'x-key': jr.key });
  assert.strictEqual(r.json.status, 'arrived');
  const row = db.prepare('SELECT status, ended_at FROM journeys WHERE token=?').get(jr.token);
  assert.strictEqual(row.status, 'arrived'); assert.ok(row.ended_at);
  assert.ok(db.prepare("SELECT body FROM outbox WHERE body LIKE '%safely%' OR body LIKE '%सुरक्षित%' OR body LIKE '%ಸುರಕ್ಷಿತ%'").all().length >= 2);
  assert.strictEqual(db.prepare("SELECT done FROM jobs WHERE ref=?").get(jr.token).done, 1);
  close();
});
