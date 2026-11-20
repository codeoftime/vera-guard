'use strict';
const cfg = require('./config');
const { haversine } = require('./util');

const cache = new Map(); // key -> {ts, value}
const TTL = 10 * 60 * 1000;

function classify(tags = {}) {
  if (tags.amenity === 'hospital' || tags.healthcare === 'hospital') return 'hospital';
  if (tags.amenity === 'pharmacy' || tags.healthcare === 'pharmacy') return 'pharmacy';
  if (tags.amenity === 'clinic' || tags.amenity === 'doctors' || tags.healthcare === 'clinic' || tags.healthcare === 'doctor') return 'clinic';
  if (tags.emergency === 'ambulance_station') return 'hospital';
  return null;
}

function parseOverpass(json, lat, lng) {
  const out = [];
  for (const el of json.elements || []) {
    const tags = el.tags || {};
    const type = classify(tags);
    if (!type) continue;
    const plat = el.lat ?? el.center?.lat, plng = el.lon ?? el.center?.lon;
    if (plat == null || plng == null) continue;
    const address = [tags['addr:housenumber'], tags['addr:street'], tags['addr:suburb'] || tags['addr:city']].filter(Boolean).join(', ') || null;
    out.push({
      id: `osm-${el.type}-${el.id}`,
      name: tags.name || tags['name:en'] || (type === 'hospital' ? 'Hospital' : type === 'pharmacy' ? 'Pharmacy' : 'Clinic'),
      type, lat: plat, lng: plng,
      distanceM: Math.round(haversine(lat, lng, plat, plng)),
      address, phone: tags.phone || tags['contact:phone'] || null,
      emergency: tags.emergency === 'yes' || tags.emergency === 'ambulance_station',
      open24x7: tags.opening_hours === '24/7',
      openNow: null,
    });
  }
  return out;
}

async function overpass(lat, lng, radius, fetchImpl) {
  const q = `[out:json][timeout:10];(` +
    ['hospital', 'clinic', 'doctors', 'pharmacy'].map((a) => `nwr["amenity"="${a}"](around:${radius},${lat},${lng});`).join('') +
    `nwr["healthcare"](around:${radius},${lat},${lng});nwr["emergency"="ambulance_station"](around:${radius},${lat},${lng});` +
    `);out center 120;`;
  const res = await fetchImpl(cfg.overpassUrl, {
    method: 'POST', headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: 'data=' + encodeURIComponent(q), signal: AbortSignal.timeout(12000),
  });
  if (!res.ok) throw new Error('overpass ' + res.status);
  return parseOverpass(await res.json(), lat, lng);
}

async function google(lat, lng, radius, fetchImpl) {
  const kinds = [['hospital', 'hospital'], ['doctor', 'clinic'], ['pharmacy', 'pharmacy']];
  const all = [];
  await Promise.all(kinds.map(async ([gtype, type]) => {
    const res = await fetchImpl('https://places.googleapis.com/v1/places:searchNearby', {
      method: 'POST',
      headers: {
        'content-type': 'application/json', 'X-Goog-Api-Key': cfg.googlePlacesKey,
        'X-Goog-FieldMask': 'places.id,places.displayName,places.location,places.formattedAddress,places.nationalPhoneNumber,places.currentOpeningHours.openNow',
      },
      body: JSON.stringify({ includedTypes: [gtype], maxResultCount: 10, locationRestriction: { circle: { center: { latitude: lat, longitude: lng }, radius } } }),
      signal: AbortSignal.timeout(8000),
    });
    if (!res.ok) throw new Error('google ' + res.status);
    const data = await res.json();
    for (const p of data.places || []) {
      all.push({
        id: 'g-' + p.id, name: p.displayName?.text || 'Unknown', type,
        lat: p.location.latitude, lng: p.location.longitude,
        distanceM: Math.round(haversine(lat, lng, p.location.latitude, p.location.longitude)),
        address: p.formattedAddress || null, phone: p.nationalPhoneNumber || null,
        emergency: false, open24x7: false, openNow: p.currentOpeningHours?.openNow ?? null,
      });
    }
  }));
  return all;
}

function group(list) {
  const sortFn = (a, b) => (b.emergency - a.emergency) || (b.open24x7 - a.open24x7) || a.distanceM - b.distanceM;
  const by = (t) => list.filter((x) => x.type === t);
  const hospitals = by('hospital').sort(sortFn);
  const byDist = (a, b) => a.distanceM - b.distanceM;
  return { hospitals, clinics: by('clinic').sort(byDist), pharmacies: by('pharmacy').sort(byDist) };
}

async function nearby(lat, lng, { radius = 5000, fetchImpl = fetch, provider = cfg.placesProvider } = {}) {
  const key = `${provider}:${lat.toFixed(3)}:${lng.toFixed(3)}:${radius}`;
  const hit = cache.get(key);
  if (hit && Date.now() - hit.ts < TTL) return hit.value;
  let list;
  try {
    list = provider === 'google' && cfg.googlePlacesKey ? await google(lat, lng, radius, fetchImpl) : await overpass(lat, lng, radius, fetchImpl);
  } catch (e) {
    if (provider === 'google') { try { list = await overpass(lat, lng, radius, fetchImpl); } catch { list = null; } } else list = null;
  }
  if (!list) return { ok: false, hospitals: [], clinics: [], pharmacies: [], fetchedAt: Date.now() };
  const value = { ok: true, provider, radius, fetchedAt: Date.now(), ...group(list) };
  cache.set(key, { ts: Date.now(), value });
  return value;
}

module.exports = { nearby, parseOverpass, group, classify };
