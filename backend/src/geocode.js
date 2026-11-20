'use strict';
// Reverse geocoding via Nominatim (OSM). Cached in memory, short timeout, never throws.
const cache = new Map();
async function reverse(lat, lng, fetchImpl = fetch) {
  const key = lat.toFixed(4) + ',' + lng.toFixed(4);
  if (cache.has(key)) return cache.get(key);
  try {
    const res = await fetchImpl(`https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}&zoom=17`, {
      headers: { 'User-Agent': 'VERA-Guard/1.0 (safety app)' }, signal: AbortSignal.timeout(2500),
    });
    if (!res.ok) return null;
    const data = await res.json();
    const addr = data.display_name || null;
    if (addr) cache.set(key, addr);
    return addr;
  } catch { return null; }
}
module.exports = { reverse };
