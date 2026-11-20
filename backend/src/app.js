'use strict';
const express = require('express');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const fs = require('node:fs');
const path = require('node:path');
const cfg = require('./config');
const { open, j, p } = require('./db');
const U = require('./util');
const { buildBrief, shortSms } = require('./brief');
const places = require('./places');
const llm = require('./llm');
const J = require('./journey');
const W = require('./watchdog');
const { t, LANGS } = require('./i18n');
const { sendSms } = require('./sms');
const geocode = require('./geocode');

const CHAT_SYSTEM = 'You are VERA, a calm first-aid and safety assistant for people in India. Give short, clear, step-by-step first-aid guidance (CPR, bleeding, burns, fractures, choking, seizures, heat stroke, snake bites, road accidents), safety tips, and help with using the app. You are not a doctor and must not diagnose. For anything serious (unconsciousness, heavy bleeding, chest pain, breathing trouble, suspected head/spine injury, poisoning), first tell the user to call 112 or 108, then give interim steps. Never recommend prescription doses. Reply in the user\'s language, under 120 words unless asked for more.';

const bad = (res, msg, code = 400) => res.status(code).json({ error: msg });
const langOf = (q) => (LANGS.includes(q) ? q : 'en');

function createApp({ db = open(cfg.dbPath), deps = {} } = {}) {
  const app = express();
  const complete = deps.complete || llm.complete;
  const nearbyFn = deps.nearby || places.nearby;
  app.set('trust proxy', 1);
  app.use(cors());
  app.use((req, res, next) => { res.setHeader('X-Content-Type-Options', 'nosniff'); res.setHeader('Referrer-Policy', 'no-referrer'); next(); });
  app.use(express.json({ limit: '512kb' }));
  const limiter = (windowMs, max) => rateLimit({ windowMs, max, standardHeaders: true, legacyHeaders: false, validate: false });
  app.use(limiter(60e3, 600));
  const strict = limiter(60e3, 40);
  const chatLimit = limiter(60e3, 12);

  const getSession = (tok) => {
    const r = db.prepare('SELECT * FROM sessions WHERE token=?').get(tok);
    return r ? { ...r, patient: p(r.patient), event: p(r.event), contacts: p(r.contacts, []), facilities: p(r.facilities) } : null;
  };
  const getJourney = (tok) => W.loadJourney(db.prepare('SELECT * FROM journeys WHERE token=?').get(tok));
  const needKey = (row) => (req) => req.get('x-key') && req.get('x-key') === row.write_key;

  // ---------- Sessions (fall / crash / SOS) ----------
  app.post('/sessions', strict, (req, res) => {
    const b = req.body || {};
    if (!['fall', 'crash', 'sos', 'journey_alert'].includes(b.kind)) return bad(res, 'invalid kind');
    if (b.lat != null && !(U.validLat(b.lat) && U.validLng(b.lng))) return bad(res, 'invalid coordinates');
    const now = U.now(), tok = U.token(), key = U.token(16);
    db.prepare(`INSERT INTO sessions(token,write_key,owner_id,kind,status,created_at,updated_at,expires_at,demo,lat,lng,accuracy,loc_ts,battery,patient,event,contacts,facilities,journey_token)
      VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`).run(
      tok, key, U.str(b.ownerId, 80), b.kind, 'not_responding', now, now, now + cfg.sessionTtlHours * 3600e3, b.demo ? 1 : 0,
      b.lat ?? null, b.lng ?? null, U.num(b.accuracy), b.lat != null ? now : null, U.num(b.battery),
      j(b.patient), j(b.event), j((b.contacts || []).slice(0, 5)), j(b.facilities), U.str(b.journeyToken, 80));
    if (b.lat != null) db.prepare('INSERT INTO locations(token,lat,lng,accuracy,ts) VALUES (?,?,?,?,?)').run(tok, b.lat, b.lng, U.num(b.accuracy), now);
    if (b.journeyToken) {
      db.prepare("UPDATE journeys SET alert_session=?, status='alert', updated_at=? WHERE token=?").run(tok, now, b.journeyToken);
    }
    res.status(201).json({ token: tok, key, url: `${cfg.publicUrl}/track/${tok}`, briefUrl: `${cfg.publicUrl}/brief/${tok}` });
  });

  app.post('/sessions/:token/location', (req, res) => {
    const s = getSession(req.params.token);
    if (!s) return bad(res, 'not found', 404);
    if (!needKey(s)(req)) return bad(res, 'forbidden', 403);
    const { lat, lng } = req.body || {};
    if (!U.validLat(lat) || !U.validLng(lng)) return bad(res, 'invalid coordinates');
    const ts = U.now();
    db.prepare('UPDATE sessions SET lat=?,lng=?,accuracy=?,loc_ts=?,battery=COALESCE(?,battery),updated_at=? WHERE token=?').run(lat, lng, U.num(req.body.accuracy), ts, U.num(req.body.battery), ts, s.token);
    db.prepare('INSERT INTO locations(token,lat,lng,accuracy,ts) VALUES (?,?,?,?,?)').run(s.token, lat, lng, U.num(req.body.accuracy), ts);
    res.json({ ok: true, acked: !!s.acked_at, status: s.acked_at ? 'responder_on_way' : s.status, ended: !!s.ended_at });
  });

  app.post('/sessions/:token/event', (req, res) => {
    const s = getSession(req.params.token);
    if (!s) return bad(res, 'not found', 404);
    if (!needKey(s)(req)) return bad(res, 'forbidden', 403);
    const b = req.body || {};
    const status = ['not_responding', 'responsive', 'cancelled'].includes(b.status) ? b.status : s.status;
    db.prepare('UPDATE sessions SET status=?, event=COALESCE(?,event), patient=COALESCE(?,patient), contacts=COALESCE(?,contacts), facilities=COALESCE(?,facilities), updated_at=? WHERE token=?')
      .run(status, j(b.event), j(b.patient), b.contacts ? j(b.contacts.slice(0, 5)) : null, j(b.facilities), U.now(), s.token);
    res.json({ ok: true });
  });

  app.post('/sessions/:token/audio', express.raw({ type: '*/*', limit: '6mb' }), (req, res) => {
    const s = getSession(req.params.token);
    if (!s) return bad(res, 'not found', 404);
    if (!needKey(s)(req)) return bad(res, 'forbidden', 403);
    if (!Buffer.isBuffer(req.body) || !req.body.length) return bad(res, 'empty body');
    const dir = path.join(path.dirname(path.resolve(cfg.dbPath)), 'audio', s.token.replace(/[^\w-]/g, ''));
    fs.mkdirSync(dir, { recursive: true });
    const n = s.audio_count + 1;
    fs.writeFileSync(path.join(dir, `${n}.bin`), req.body); // already encrypted on-device
    db.prepare('UPDATE sessions SET audio_count=? WHERE token=?').run(n, s.token);
    res.json({ ok: true, n });
  });

  // Responder acknowledgement: public by design (the link is the capability).
  app.post('/sessions/:token/ack', strict, (req, res) => {
    const s = getSession(req.params.token);
    if (!s) return bad(res, 'not found', 404);
    if (!s.acked_at) db.prepare('UPDATE sessions SET acked_at=?, acked_by=?, updated_at=? WHERE token=?').run(U.now(), U.str(req.body?.name, 60) || 'responder', U.now(), s.token);
    res.json({ ok: true });
  });

  app.post('/sessions/:token/end', (req, res) => {
    const s = getSession(req.params.token);
    if (!s) return bad(res, 'not found', 404);
    if (!needKey(s)(req)) return bad(res, 'forbidden', 403);
    const status = req.body?.cancelled ? 'cancelled' : s.status;
    db.prepare('UPDATE sessions SET ended_at=?, status=?, updated_at=? WHERE token=?').run(U.now(), status, U.now(), s.token);
    res.json({ ok: true });
  });

  // ---------- Briefs ----------
  async function sessionBrief(s, lang) {
    let facilities = s.facilities;
    if (!facilities && s.lat != null) { try { facilities = await nearbyFn(s.lat, s.lng); } catch { /* ignore */ } }
    const brief = await buildBrief(db, s, lang, { nearby: facilities, complete });
    const pts = db.prepare('SELECT lat,lng,ts FROM locations WHERE token=? ORDER BY ts DESC LIMIT 200').all(s.token).reverse();
    brief.path = pts.map((x) => [x.lat, x.lng]);
    brief.shortSms = shortSms({ kind: s.kind, event: s.event, lat: s.lat, lng: s.lng, patient: s.patient, hospital: facilities?.hospitals?.[0]?.name }, lang);
    if (s.journey_token) {
      const jr = getJourney(s.journey_token);
      if (jr) brief.journey = { destName: jr.dest_name, dest: [jr.dest_lat, jr.dest_lng], planned: jr.route, vehicle: jr.cab };
    }
    return brief;
  }

  app.get('/brief/:token', async (req, res) => {
    const lang = langOf(req.query.lang);
    const s = getSession(req.params.token);
    if (s) return res.json(await sessionBrief(s, lang));
    const jr = getJourney(req.params.token);
    if (jr && jr.alert_session) { const a = getSession(jr.alert_session); if (a) return res.json(await sessionBrief(a, lang)); }
    return bad(res, 'not found', 404);
  });

  // Unified endpoint for the public /track/<token> page.
  app.get('/api/track/:token', async (req, res) => {
    const lang = langOf(req.query.lang);
    const s = getSession(req.params.token);
    if (s) return res.json({ type: 'session', brief: await sessionBrief(s, lang) });
    const jr = getJourney(req.params.token);
    if (!jr) return bad(res, 'not found', 404);
    const T = t(lang);
    const status = jr.status === 'arrived' || jr.status === 'ended' ? jr.status : J.evaluate(jr);
    const path = db.prepare('SELECT lat,lng,ts FROM locations WHERE token=? ORDER BY ts').all(jr.token).map((x) => [x.lat, x.lng]);
    const out = {
      type: 'journey', lang, status, statusLabel: T.journey.status[status] || status,
      name: jr.name, phone: jr.phone, mode: jr.mode, dest: { name: jr.dest_name, lat: jr.dest_lat, lng: jr.dest_lng },
      etaTs: jr.eta_ts, bufferS: jr.buffer_s, planned: jr.route, path, current: jr.lat != null ? { lat: jr.lat, lng: jr.lng, ts: jr.loc_ts } : null,
      battery: jr.battery, online: jr.loc_ts ? Date.now() - jr.loc_ts < 90e3 : false, cab: jr.cab, ended: !!jr.ended_at,
      labels: { ...T.journey, callName: T.journey.callName(jr.name || '') },
    };
    if (status === 'alert' && jr.alert_session) {
      const a = getSession(jr.alert_session);
      if (a) out.brief = await sessionBrief(a, lang);
    }
    return res.json(out);
  });

  // ---------- Journeys ----------
  app.post('/journeys', strict, async (req, res) => {
    const b = req.body || {};
    const d = b.dest || {};
    if (!U.validLat(d.lat) || !U.validLng(d.lng)) return bad(res, 'invalid destination');
    if (!U.num(b.etaTs) || b.etaTs < U.now() - 60e3) return bad(res, 'invalid etaTs');
    const followers = (Array.isArray(b.followers) ? b.followers : []).slice(0, 5).filter((f) => f && typeof f.phone === 'string').map((f) => ({ name: U.str(f.name, 60), phone: U.str(f.phone, 20), lang: langOf(f.lang) }));
    const route = (Array.isArray(b.route) ? b.route : []).slice(0, 3000).filter((q) => Array.isArray(q) && U.validLat(q[0]) && U.validLng(q[1]));
    const now = U.now(), tok = U.token(), key = U.token(16);
    const wd = Math.max(b.demo ? 15 : 120, Math.min(3600, Number(b.watchdogSeconds) || cfg.watchdogMinutes * 60));
    db.prepare(`INSERT INTO journeys(token,write_key,owner_id,name,phone,mode,dest_name,dest_lat,dest_lng,route,eta_ts,buffer_s,status,started_at,updated_at,expires_at,lat,lng,loc_ts,battery,followers,cab,patient,watchdog_sec,demo,lang,notify)
      VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`).run(
      tok, key, U.str(b.ownerId, 80), U.str(b.name, 80), U.str(b.phone, 20), U.str(b.mode, 20), U.str(d.name, 120), d.lat, d.lng, j(route), b.etaTs, Math.max(0, Math.min(7200, Number(b.bufferS) || 0)),
      'on_route', now, now, now + cfg.journeyTtlHours * 3600e3, U.validLat(b.lat) ? b.lat : null, U.validLng(b.lng) ? b.lng : null, U.validLat(b.lat) ? now : now, U.num(b.battery),
      j(followers), j(b.cab), j(b.patient), wd, b.demo ? 1 : 0, langOf(b.lang), b.notify === false ? 0 : 1);
    if (U.validLat(b.lat)) db.prepare('INSERT INTO locations(token,lat,lng,accuracy,ts) VALUES (?,?,?,?,?)').run(tok, b.lat, b.lng, null, now);
    const eta = new Date(b.etaTs).toLocaleTimeString('en-IN', { timeZone: 'Asia/Kolkata', hour: '2-digit', minute: '2-digit' });
    if (b.notify !== false) await W.notifyFollowers(db, { followers, lang: langOf(b.lang) }, (T) => T.sms.journeyStart(b.name || '', d.name || '', eta, W.link(tok)));
    W.arm(db, db.prepare('SELECT * FROM journeys WHERE token=?').get(tok));
    res.status(201).json({ token: tok, key, url: W.link(tok) });
  });

  const jAuth = (req, res) => {
    const jr = getJourney(req.params.token);
    if (!jr) { bad(res, 'not found', 404); return null; }
    if (!needKey(jr)(req)) { bad(res, 'forbidden', 403); return null; }
    return jr;
  };

  app.post('/journeys/:token/location', async (req, res) => {
    const jr = jAuth(req, res); if (!jr) return;
    const { lat, lng } = req.body || {};
    if (!U.validLat(lat) || !U.validLng(lng)) return bad(res, 'invalid coordinates');
    if (jr.ended_at) return res.json({ ok: true, ended: true, status: jr.status });
    const ts = U.now();
    const deviatedSince = J.updateDeviation(jr, lat, lng, ts);
    const stopped = req.body.stopped ? 1 : 0;
    const battery = U.num(req.body.battery) ?? jr.battery;
    const upd = { ...jr, lat, lng, loc_ts: ts, deviated_since: deviatedSince, stopped, battery, watchdog_alerted: 0 };
    let status = J.evaluate({ ...upd, status: jr.status === 'alert' && jr.alert_session ? 'on_route' : jr.status }, ts);
    // An explicit alert raised by check-in stays until the user confirms OK.
    if (jr.status === 'alert' && jr.alert_session && !jr.watchdog_alerted) status = 'alert';
    db.prepare('UPDATE journeys SET lat=?,lng=?,loc_ts=?,deviated_since=?,stopped=?,battery=?,watchdog_alerted=0,status=?,updated_at=? WHERE token=?').run(lat, lng, ts, deviatedSince, stopped, battery, status, ts, jr.token);
    db.prepare('INSERT INTO locations(token,lat,lng,accuracy,ts) VALUES (?,?,?,?,?)').run(jr.token, lat, lng, U.num(req.body.accuracy), ts);
    if (battery != null && battery < 15 && !jr.low_bat_alerted) {
      db.prepare('UPDATE journeys SET low_bat_alerted=1 WHERE token=?').run(jr.token);
      if (jr.notify) await W.notifyFollowers(db, jr, (T) => `${jr.name || ''}: ${T.journey.battery} ${battery}% — ${W.link(jr.token)}`);
    }
    if (status === 'arrived') {
      db.prepare("UPDATE journeys SET ended_at=?, status='arrived' WHERE token=?").run(ts, jr.token);
      if (jr.notify) await W.notifyFollowers(db, jr, (T) => T.sms.arrived(jr.name || ''));
      db.prepare("UPDATE jobs SET done=1 WHERE kind='journey' AND ref=?").run(jr.token);
    } else W.arm(db, db.prepare('SELECT * FROM journeys WHERE token=?').get(jr.token));
    res.json({ ok: true, status, ended: status === 'arrived' });
  });

  app.post('/journeys/:token/checkin', async (req, res) => {
    const jr = jAuth(req, res); if (!jr) return;
    const s = req.body?.status; // ok | help
    const now = U.now();
    if (s === 'ok') {
      db.prepare("UPDATE journeys SET stopped=0, deviated_since=NULL, status='on_route', updated_at=? WHERE token=?").run(now, jr.token);
      return res.json({ ok: true });
    }
    if (s === 'help') {
      db.prepare("UPDATE journeys SET status='alert', updated_at=? WHERE token=?").run(now, jr.token);
      // The app then creates a journey_alert session via POST /sessions with journeyToken (attaches the Responder Brief).
      return res.json({ ok: true, alertSession: jr.alert_session || null });
    }
    return bad(res, 'status must be ok or help');
  });

  app.post('/journeys/:token/extend', (req, res) => {
    const jr = jAuth(req, res); if (!jr) return;
    const mins = Math.max(1, Math.min(240, Number(req.body?.minutes) || 15));
    db.prepare('UPDATE journeys SET eta_ts=eta_ts+?, overdue_alerted=0, updated_at=? WHERE token=?').run(mins * 60e3, U.now(), jr.token);
    W.arm(db, db.prepare('SELECT * FROM journeys WHERE token=?').get(jr.token));
    res.json({ ok: true });
  });

  app.post('/journeys/:token/end', async (req, res) => {
    const jr = jAuth(req, res); if (!jr) return;
    const arrived = req.body?.arrived !== false;
    db.prepare('UPDATE journeys SET ended_at=?, status=?, updated_at=? WHERE token=?').run(U.now(), arrived ? 'arrived' : 'ended', U.now(), jr.token);
    db.prepare("UPDATE jobs SET done=1 WHERE kind='journey' AND ref=?").run(jr.token);
    if (arrived && !jr.ended_at && jr.notify) await W.notifyFollowers(db, jr, (T) => T.sms.arrived(jr.name || ''));
    res.json({ ok: true });
  });

  // ---------- Nearby, routing helper, chat ----------
  app.get('/nearby', strict, async (req, res) => {
    const lat = Number(req.query.lat), lng = Number(req.query.lng);
    if (!U.validLat(lat) || !U.validLng(lng)) return bad(res, 'invalid coordinates');
    const radius = Math.max(500, Math.min(10000, Number(req.query.radius) || 5000));
    res.json(await nearbyFn(lat, lng, { radius }));
  });

  app.get('/route', strict, async (req, res) => {
    const [a, b] = ['from', 'to'].map((k) => String(req.query[k] || '').split(',').map(Number));
    if (!U.validLat(a[0]) || !U.validLng(a[1]) || !U.validLat(b[0]) || !U.validLng(b[1])) return bad(res, 'invalid from/to');
    const prof = { walking: 'foot', auto: 'driving', cab: 'driving', bike: 'driving', car: 'driving' }[req.query.mode] || 'driving';
    try {
      const r = await fetch(`${cfg.osrmUrl}/route/v1/${prof}/${a[1]},${a[0]};${b[1]},${b[0]}?overview=simplified&geometries=geojson`, { signal: AbortSignal.timeout(8000) });
      const d = await r.json();
      const rt = d.routes?.[0];
      if (!rt) return bad(res, 'no route', 404);
      res.json({ route: rt.geometry.coordinates.map(([lng, lat]) => [lat, lng]), distanceM: rt.distance, durationS: rt.duration });
    } catch { bad(res, 'routing unavailable', 502); }
  });

  app.get('/geocode', strict, async (req, res) => {
    const q = String(req.query.q || '').slice(0, 120);
    if (q.length < 2) return bad(res, 'query too short');
    try {
      const r = await fetch(`https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&countrycodes=in&q=${encodeURIComponent(q)}`, { headers: { 'User-Agent': 'VERA-Guard/1.0' }, signal: AbortSignal.timeout(6000) });
      const d = await r.json();
      res.json(d.map((x) => ({ name: x.display_name, lat: Number(x.lat), lng: Number(x.lon) })));
    } catch { bad(res, 'geocoding unavailable', 502); }
  });

  app.post('/chat', chatLimit, async (req, res) => {
    const b = req.body || {};
    const msgs = (Array.isArray(b.messages) ? b.messages : []).slice(-8)
      .filter((m) => m && (m.role === 'user' || m.role === 'assistant') && typeof m.content === 'string')
      .map((m) => ({ role: m.role, content: m.content.slice(0, 600) }));
    if (!msgs.length || msgs[msgs.length - 1].role !== 'user') return bad(res, 'messages must end with a user message');
    const lang = langOf(b.lang);
    let system = CHAT_SYSTEM + ` The user's profile language is ${{ en: 'English', hi: 'Hindi', kn: 'Kannada' }[lang]}; reply in the language the user writes in, defaulting to it.`;
    if (b.profile && typeof b.profile === 'object') { // only sent by the app when the user consented
      const pr = ['age', 'bloodGroup', 'allergies', 'conditions'].filter((k) => b.profile[k]).map((k) => `${k}: ${String(b.profile[k]).slice(0, 80)}`);
      if (pr.length) system += ' User profile (shared with consent): ' + pr.join('; ') + '.';
    }
    const text = await complete({ system, messages: msgs, maxTokens: 450, timeoutMs: cfg.chatTimeoutMs });
    if (!text) return res.status(503).json({ error: 'assistant unavailable', offline: true });
    res.json({ reply: text });
  });

  // ---------- Dashboard / data ownership ----------
  app.get('/api/dashboard', strict, (req, res) => {
    const owner = String(req.query.owner || '');
    if (owner.length < 16) return bad(res, 'owner required', 401);
    const sessions = db.prepare('SELECT token,kind,status,created_at,ended_at,acked_at,lat,lng,event FROM sessions WHERE owner_id=? ORDER BY created_at DESC LIMIT 50').all(owner)
      .map((r) => ({ ...r, event: p(r.event) }));
    const journeys = db.prepare('SELECT token,dest_name,mode,status,started_at,ended_at,eta_ts,battery FROM journeys WHERE owner_id=? ORDER BY started_at DESC LIMIT 50').all(owner);
    res.json({ sessions, journeys });
  });

  app.delete('/owner/:ownerId', strict, (req, res) => {
    const o = req.params.ownerId;
    if (o.length < 16) return bad(res, 'invalid owner');
    const toks = db.prepare('SELECT token FROM sessions WHERE owner_id=? UNION SELECT token FROM journeys WHERE owner_id=?').all(o, o).map((r) => r.token);
    for (const tk of toks) { db.prepare('DELETE FROM locations WHERE token=?').run(tk); db.prepare("DELETE FROM jobs WHERE ref=?").run(tk); fs.rmSync(path.join(path.dirname(path.resolve(cfg.dbPath)), 'audio', tk.replace(/[^\w-]/g, '')), { recursive: true, force: true }); }
    db.prepare('DELETE FROM sessions WHERE owner_id=?').run(o);
    db.prepare('DELETE FROM journeys WHERE owner_id=?').run(o);
    res.json({ ok: true, deleted: toks.length });
  });

  // Demo helper: shows the SMS the server "sent" (only when DEMO_OUTBOX=1).
  app.get('/api/outbox', (req, res) => {
    if (process.env.DEMO_OUTBOX !== '1') return bad(res, 'disabled', 404);
    res.json(db.prepare('SELECT * FROM outbox ORDER BY id DESC LIMIT 50').all());
  });

  app.get('/health', (req, res) => res.json({ ok: true, time: Date.now() }));

  // ---------- Website ----------
  const pub = path.join(__dirname, '..', 'public');
  app.get('/track/:token', (req, res) => res.sendFile(path.join(pub, 'track.html')));
  app.get('/brief/:token/page', (req, res) => res.redirect('/track/' + encodeURIComponent(req.params.token)));
  for (const pg of ['dashboard', 'chat', 'privacy']) app.get('/' + pg, (req, res) => res.sendFile(path.join(pub, pg + '.html')));
  // APK download: bundled file wins; otherwise fetch APK_URL once, cache it next to the DB and refresh every 6 h.
  const apkCache = cfg.dbPath === ':memory:' ? path.join(require('node:os').tmpdir(), 'VERA-Guard.apk') : path.join(path.dirname(path.resolve(cfg.dbPath)), 'VERA-Guard.apk');
  app.get('/VERA-Guard.apk', async (req, res, next) => {
    if (fs.existsSync(path.join(pub, 'VERA-Guard.apk'))) return next();
    const apkUrl = deps.apkUrl ?? cfg.apkUrl;
    try {
      const stale = !fs.existsSync(apkCache) || Date.now() - fs.statSync(apkCache).mtimeMs > 6 * 3600e3;
      if (apkUrl && stale) {
        const r = await fetch(apkUrl, { redirect: 'follow', signal: AbortSignal.timeout(90000) });
        if (!r.ok) throw new Error('apk fetch ' + r.status);
        const buf = Buffer.from(await r.arrayBuffer());
        if (buf.length < 1024 || buf[0] !== 0x50 || buf[1] !== 0x4b) throw new Error('not an apk (zip) file');
        fs.writeFileSync(apkCache + '.tmp', buf); fs.renameSync(apkCache + '.tmp', apkCache);
      }
    } catch (e) { console.error('apk refresh failed:', e.message); }
    if (!fs.existsSync(apkCache)) return bad(res, 'apk not available yet', 404);
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', 'attachment; filename="VERA-Guard.apk"');
    res.sendFile(apkCache);
  });
  app.use(express.static(pub, { setHeaders: (res, f) => { if (f.endsWith('.apk')) { res.setHeader('Content-Type', 'application/vnd.android.package-archive'); res.setHeader('Content-Disposition', 'attachment; filename="VERA-Guard.apk"'); } } }));
  app.use((err, req, res, next) => { // eslint-disable-line
    if (err.type === 'entity.too.large') return bad(res, 'payload too large', 413);
    if (err instanceof SyntaxError) return bad(res, 'invalid json');
    console.error(err); res.status(500).json({ error: 'server error' });
  });

  return { app, db };
}

module.exports = { createApp, CHAT_SYSTEM };
