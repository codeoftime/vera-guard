'use strict';
const { distanceToRoute, haversine } = require('./util');

// Server-side thresholds mirror the Android constants (configurable there; deviation is re-checked here).
const DEVIATION_M = 300;
const DEVIATION_SEC = 60;
const ARRIVAL_M = 100;

// Pure function. Decides the status chip from stored facts.
function evaluate(j, now = Date.now()) {
  if (j.ended_at && j.status === 'arrived') return 'arrived';
  if (j.watchdog_alerted || j.status === 'alert') return 'alert';
  if (j.lat != null && j.dest_lat != null && haversine(j.lat, j.lng, j.dest_lat, j.dest_lng) <= ARRIVAL_M) return 'arrived';
  if (j.deviated_since && now - j.deviated_since > DEVIATION_SEC * 1000) return 'deviated';
  if (j.stopped) return 'stopped';
  if (now > j.eta_ts + (j.buffer_s || 0) * 1000) return 'delayed';
  return 'on_route';
}

// Returns the new deviated_since after a location sample.
function updateDeviation(j, lat, lng, ts) {
  if (!j.route || j.route.length < 2) return null;
  const d = distanceToRoute(lat, lng, j.route);
  if (d == null || d <= DEVIATION_M) return null;
  return j.deviated_since || ts;
}

// When must the watchdog next look at this journey? null = nothing pending.
function nextCheck(j) {
  if (j.ended_at) return null;
  const times = [];
  if (!j.watchdog_alerted) times.push((j.loc_ts || j.started_at) + j.watchdog_sec * 1000);
  if (!j.overdue_alerted) times.push(j.eta_ts + (j.buffer_s || 0) * 1000);
  return times.length ? Math.min(...times) : null;
}

module.exports = { evaluate, updateDeviation, nextCheck, DEVIATION_M, DEVIATION_SEC, ARRIVAL_M };
