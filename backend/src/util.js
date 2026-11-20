'use strict';
const crypto = require('node:crypto');
const token = (bytes = 24) => crypto.randomBytes(bytes).toString('base64url');
const R = 6371000;
const rad = (d) => (d * Math.PI) / 180;
function haversine(lat1, lng1, lat2, lng2) {
  const dLat = rad(lat2 - lat1), dLng = rad(lng2 - lng1);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(a));
}
// Distance in metres from point to a polyline of [lat,lng] pairs (equirectangular projection, fine for <10 km segments).
function distanceToRoute(lat, lng, route) {
  if (!route || route.length === 0) return null;
  if (route.length === 1) return haversine(lat, lng, route[0][0], route[0][1]);
  const k = Math.cos(rad(lat));
  const px = lng * k * 111320, py = lat * 110540;
  let best = Infinity;
  for (let i = 0; i < route.length - 1; i++) {
    const ax = route[i][1] * k * 111320, ay = route[i][0] * 110540;
    const bx = route[i + 1][1] * k * 111320, by = route[i + 1][0] * 110540;
    const dx = bx - ax, dy = by - ay;
    const len2 = dx * dx + dy * dy;
    let tt = len2 === 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / len2;
    tt = Math.max(0, Math.min(1, tt));
    const cx = ax + tt * dx, cy = ay + tt * dy;
    best = Math.min(best, Math.hypot(px - cx, py - cy));
  }
  return best;
}
const num = (v) => (typeof v === 'number' && Number.isFinite(v) ? v : null);
const validLat = (v) => num(v) !== null && v >= -90 && v <= 90;
const validLng = (v) => num(v) !== null && v >= -180 && v <= 180;
const str = (v, max = 200) => (typeof v === 'string' ? v.slice(0, max) : null);
const now = () => Date.now();
module.exports = { token, haversine, distanceToRoute, num, validLat, validLng, str, now };
