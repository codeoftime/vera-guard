'use strict';
// Dead-man's switch. Timers live in the `jobs` table (run_at), so a server restart loses nothing.
const { p, j } = require('./db');
const { t } = require('./i18n');
const { sendSms } = require('./sms');
const cfg = require('./config');
const J = require('./journey');
const { token } = require('./util');

function loadJourney(row) {
  if (!row) return null;
  return { ...row, route: p(row.route, []), followers: p(row.followers, []), cab: p(row.cab), patient: p(row.patient) };
}

function arm(db, journeyRow) {
  const jr = journeyRow.route && typeof journeyRow.route === 'string' ? loadJourney(journeyRow) : journeyRow;
  const at = J.nextCheck(jr);
  if (at == null) {
    db.prepare("UPDATE jobs SET done=1 WHERE kind='journey' AND ref=?").run(jr.token);
    return;
  }
  db.prepare("INSERT INTO jobs(kind,ref,run_at,done) VALUES ('journey',?,?,0) ON CONFLICT(kind,ref) DO UPDATE SET run_at=excluded.run_at, done=0").run(jr.token, at);
}

const link = (tok) => `${cfg.publicUrl}/track/${tok}`;
const posText = (jr) => (jr.lat != null ? `https://maps.google.com/?q=${jr.lat},${jr.lng}` : 'Unknown');

async function notifyFollowers(db, jr, build) {
  const results = [];
  for (const f of jr.followers || []) {
    if (!f.phone) continue;
    results.push(await sendSms(db, f.phone, build(t(f.lang || jr.lang || 'en'), f)));
  }
  return results;
}

// Creates an alert session so the tracking page can switch to the Responder Brief.
function ensureAlertSession(db, jr, now) {
  if (jr.alert_session) return jr.alert_session;
  const tok = token(), key = token(16);
  db.prepare(`INSERT INTO sessions(token,write_key,owner_id,kind,status,created_at,updated_at,expires_at,demo,lat,lng,loc_ts,patient,event,contacts,journey_token)
    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`).run(tok, key, jr.owner_id, 'journey_alert', 'not_responding', now, now, now + cfg.sessionTtlHours * 3600e3, jr.demo,
    jr.lat, jr.lng, jr.loc_ts, j(jr.patient), j({ reason: 'watchdog' }), j(jr.followers), jr.token);
  db.prepare('UPDATE journeys SET alert_session=? WHERE token=?').run(tok, jr.token);
  return tok;
}

async function processJourney(db, token_, now) {
  const jr = loadJourney(db.prepare('SELECT * FROM journeys WHERE token=?').get(token_));
  if (!jr || jr.ended_at) { db.prepare("UPDATE jobs SET done=1 WHERE kind='journey' AND ref=?").run(token_); return; }
  const lastSeen = jr.loc_ts || jr.started_at;
  if (!jr.watchdog_alerted && now - lastSeen >= jr.watchdog_sec * 1000) {
    const mins = Math.max(1, Math.round(jr.watchdog_sec / 60));
    await notifyFollowers(db, jr, (T) => T.sms.watchdog(jr.name || '', mins, link(jr.token), posText(jr)));
    db.prepare("UPDATE journeys SET watchdog_alerted=1, status='alert', updated_at=? WHERE token=?").run(now, jr.token);
    ensureAlertSession(db, { ...jr, watchdog_alerted: 1 }, now);
  }
  if (!jr.overdue_alerted && now > jr.eta_ts + (jr.buffer_s || 0) * 1000) {
    await notifyFollowers(db, jr, (T) => T.sms.overdue(jr.name || '', link(jr.token), posText(jr)));
    db.prepare("UPDATE journeys SET overdue_alerted=1, updated_at=? WHERE token=?").run(now, jr.token);
    if (!jr.watchdog_alerted) {
      db.prepare("UPDATE journeys SET status='alert' WHERE token=?").run(jr.token);
      ensureAlertSession(db, jr, now);
    }
  }
  arm(db, db.prepare('SELECT * FROM journeys WHERE token=?').get(token_));
}

async function tick(db, now = Date.now()) {
  const due = db.prepare("SELECT kind, ref FROM jobs WHERE done=0 AND run_at<=? ORDER BY run_at LIMIT 50").all(now);
  for (const job of due) {
    try {
      if (job.kind === 'journey') await processJourney(db, job.ref, now);
      else db.prepare('UPDATE jobs SET done=1 WHERE kind=? AND ref=?').run(job.kind, job.ref);
    } catch (e) { console.error('[watchdog] job failed', job, e.message); }
  }
  // Retention: delete expired data
  db.prepare('DELETE FROM locations WHERE token IN (SELECT token FROM sessions WHERE expires_at<?)').run(now);
  db.prepare('DELETE FROM sessions WHERE expires_at<?').run(now);
  db.prepare('DELETE FROM journeys WHERE expires_at<?').run(now);
  db.prepare("DELETE FROM jobs WHERE done=1").run();
  db.prepare('DELETE FROM brief_cache WHERE ts<?').run(now - 3600e3);
  return due.length;
}

function start(db) {
  const h = setInterval(() => tick(db).catch((e) => console.error('[watchdog]', e.message)), cfg.watchdogTickMs);
  h.unref?.();
  return h;
}

module.exports = { arm, tick, start, loadJourney, notifyFollowers, link };
