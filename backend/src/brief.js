'use strict';
const crypto = require('node:crypto');
const { t } = require('./i18n');
const { p, j } = require('./db');
const llm = require('./llm');
const geocode = require('./geocode');

const SYSTEM = 'You write a concise briefing for emergency responders from the provided JSON only. Do not diagnose. Do not invent facts. Mark missing data as Unknown. Keep it under 90 words. Write in the requested language.';
const LANG_NAME = { en: 'English', hi: 'Hindi', kn: 'Kannada' };
const LLM_MIN_INTERVAL_MS = 20000;
const lastLlmCall = new Map(); // token -> ts (rate limit per session)

const mapsUrl = (lat, lng) => `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`;
const dirUrl = (lat, lng) => `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
const osmUrl = (lat, lng) => `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=17/${lat}/${lng}`;

function fmtTime(ts, lang) {
  try { return new Date(ts).toLocaleString(lang === 'en' ? 'en-IN' : lang + '-IN', { timeZone: 'Asia/Kolkata', hour12: true, dateStyle: 'medium', timeStyle: 'short' }); }
  catch { return new Date(ts).toISOString(); }
}

function patientSection(patient, contacts, T) {
  const pt = patient || {};
  const val = (v) => (v === undefined || v === null || v === '' || (Array.isArray(v) && !v.length) ? T.unknown : Array.isArray(v) ? v.join(', ') : String(v));
  // Only fields the user enabled are present in `pt`; others are shown as Unknown (never invented).
  const fields = [
    ['name', T.name], ['age', T.age], ['gender', T.gender], ['bloodGroup', T.blood],
    ['allergies', T.allergies], ['conditions', T.conditions], ['medications', T.medications],
  ].map(([k, label]) => ({ key: k, label, value: val(pt[k]) }));
  return { title: T.patient, fields, contacts: (contacts || []).map((c) => ({ name: c.name, phone: c.phone, relationship: c.relationship || null })) };
}

function analysisSection(ev, T) {
  if (!ev || !ev.severity) return null;
  const n = (v, unit, d = 1) => (v == null ? T.unknown : `${Number(v).toFixed(d)}${unit}`);
  const rows = [
    { label: T.severity, value: `${T.sev[ev.severity] || ev.severity} (${ev.score ?? T.unknown}/100)` },
    { label: T.peakG, value: n(ev.peakG, ' g') },
    { label: T.freeFall, value: ev.freeFallMs == null ? T.unknown : `${Math.round(ev.freeFallMs)} ms` },
    { label: T.orient, value: n(ev.orientationDeg, '°', 0) },
    { label: T.stillness, value: ev.stillnessSec == null ? T.unknown : `${Number(ev.stillnessSec).toFixed(0)} ${T.seconds}` },
  ];
  return { title: T.analysis, level: ev.severity, score: ev.score ?? null, rows, interpretation: T.interp({ kind: ev.kind, severityLevel: ev.severity, stillnessSec: ev.stillnessSec }) };
}

function top(list, n = 3) { return (list || []).slice(0, n).map((f) => ({ ...f, directionsUrl: dirUrl(f.lat, f.lng) })); }

// Deterministic part: always available, never needs the LLM.
async function buildDeterministic(session, lang, { nearby, now = Date.now(), reverse = geocode.reverse } = {}) {
  const T = t(lang);
  const ev = session.event || null;
  const kind = session.kind;
  const ageMin = Math.floor((now - session.created_at) / 60000);
  const statusKey = session.acked_at ? 'responder_on_way' : session.status;
  let address = null;
  if (session.lat != null) address = await reverse(session.lat, session.lng);
  const locAge = session.loc_ts ? Math.max(0, Math.round((now - session.loc_ts) / 1000)) : null;
  const facilities = nearby || session.facilities || null;
  const hospitals = top(facilities?.hospitals);
  const guidance = [...T.guide.common, ...(T.guide[kind] || T.guide.fall)].slice(0, 5);
  return {
    lang, id: session.token, type: 'session',
    header: { kind, typeLabel: T.type[kind] || kind, time: fmtTime(session.created_at, lang), minutesAgo: T.minutesAgo(ageMin), status: statusKey, statusLabel: T.status[statusKey] || statusKey, acked: !!session.acked_at, ended: !!session.ended_at, demo: !!session.demo },
    patient: patientSection(session.patient, session.contacts, T),
    analysis: analysisSection(ev ? { ...ev, kind } : null, T),
    location: session.lat == null ? { unknown: T.unknown } : {
      title: T.location, address: address || T.unknown, lat: session.lat, lng: session.lng,
      accuracy: session.accuracy ?? null, ageSec: locAge, battery: session.battery ?? null,
      mapsUrl: mapsUrl(session.lat, session.lng), osmUrl: osmUrl(session.lat, session.lng), directionsUrl: dirUrl(session.lat, session.lng),
    },
    nearby: { title: T.nearest, hospitals, clinics: top(facilities?.clinics, 2), pharmacies: top(facilities?.pharmacies, 2) },
    guidance,
    summary: T.summaryFallback({
      type: T.type[kind] || kind, time: fmtTime(session.created_at, lang), severity: ev?.severity ? `${T.sev[ev.severity]} (${ev.score})` : null,
      status: T.status[statusKey], address: address || T.unknown, hospital: hospitals[0]?.name || T.unknown,
    }),
    summaryAi: false,
    aiLabel: T.aiLabel,
    labels: { location: T.location, seen: T.seen, acked: T.acked, directions: T.directions, call: T.call, hospitals: T.hospitals, clinics: T.clinics, pharmacies: T.pharmacies, guidance: T.guidance, nearest: T.nearest, address: T.address, coords: T.coords, accuracy: T.accuracy, lastUpdate: T.lastUpdate },
  };
}

function llmPayload(b) {
  return {
    eventType: b.header.typeLabel, time: b.header.time, minutesAgo: b.header.minutesAgo, status: b.header.statusLabel,
    patient: Object.fromEntries(b.patient.fields.map((f) => [f.key, f.value])),
    analysis: b.analysis ? { severity: b.analysis.level, score: b.analysis.score, interpretation: b.analysis.interpretation } : 'Unknown',
    address: b.location.address || 'Unknown',
    nearestHospital: b.nearby.hospitals[0] ? { name: b.nearby.hospitals[0].name, distanceM: b.nearby.hospitals[0].distanceM } : 'Unknown',
  };
}

function parseLlm(text) {
  if (!text) return null;
  try {
    const m = text.match(/\{[\s\S]*\}/);
    const obj = JSON.parse(m ? m[0] : text);
    if (typeof obj.summary !== 'string' || !obj.summary.trim()) return null;
    const guidance = Array.isArray(obj.guidance) ? obj.guidance.filter((g) => typeof g === 'string').slice(0, 5) : [];
    return { summary: obj.summary.trim(), guidance };
  } catch { return null; }
}

async function buildBrief(db, session, lang, { nearby, now = Date.now(), reverse, complete = llm.complete } = {}) {
  const base = await buildDeterministic(session, lang, { nearby, now, reverse });
  const payload = llmPayload(base);
  const key = 'brief:' + session.token + ':' + lang + ':' + crypto.createHash('sha1').update(JSON.stringify(payload)).digest('hex');
  const hit = db.prepare('SELECT value FROM brief_cache WHERE key=?').get(key);
  let ai = hit ? p(hit.value) : null;
  if (!ai) {
    const last = lastLlmCall.get(session.token) || 0;
    if (now - last >= LLM_MIN_INTERVAL_MS) {
      lastLlmCall.set(session.token, now);
      const text = await complete({
        system: SYSTEM,
        messages: [{ role: 'user', content: `Language: ${LANG_NAME[lang] || 'English'}.\nReturn ONLY JSON: {"summary": "<2-3 sentences>", "guidance": ["<3 to 5 short first-aid reminders for bystanders, relevant to the event type, first one: Call 112 / 108>"]}\nData:\n${JSON.stringify(payload)}` }],
        maxTokens: 400,
      });
      ai = parseLlm(text);
      if (ai) db.prepare('INSERT OR REPLACE INTO brief_cache(key,ts,value) VALUES (?,?,?)').run(key, now, j(ai));
    }
  }
  if (ai) {
    base.summary = ai.summary; base.summaryAi = true;
    if (ai.guidance.length >= 3) {
      const first = base.guidance[0];
      const g = ai.guidance.slice(0, 5);
      if (!/112|108/.test(g[0])) g.unshift(first);
      base.guidance = g.slice(0, 5);
    }
  }
  return base;
}

// Short SMS for no-internet situations: type, severity, coordinates, blood group (if consented), nearest hospital.
function shortSms({ kind, event, lat, lng, patient, hospital }, lang = 'en') {
  const T = t(lang);
  const parts = [`VERA ${T.type[kind] || kind}`];
  if (event?.severity) parts.push(`${event.severity} ${event.score ?? ''}`.trim());
  if (lat != null) parts.push(`${lat.toFixed(5)},${lng.toFixed(5)}`);
  if (patient?.bloodGroup) parts.push(`${T.blood}: ${patient.bloodGroup}`);
  if (hospital) parts.push(`${hospital}`);
  return parts.join(' | ');
}

module.exports = { buildBrief, buildDeterministic, parseLlm, shortSms, SYSTEM };
