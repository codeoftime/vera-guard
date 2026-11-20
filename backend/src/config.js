'use strict';
// All tunables come from environment variables so nothing secret lives in the repo.
const env = process.env;
module.exports = {
  port: Number(env.PORT || 8080),
  publicUrl: (env.PUBLIC_URL || 'http://localhost:8080').replace(/\/$/, ''),
  dbPath: env.DB_PATH || './vera.db',
  // LLM (Anthropic Messages API). Key stays on the server.
  llmKey: env.LLM_API_KEY || '',
  llmModel: env.LLM_MODEL || 'claude-haiku-5-5',
  llmTimeoutMs: Number(env.LLM_TIMEOUT_MS || 3000),
  chatTimeoutMs: Number(env.CHAT_TIMEOUT_MS || 20000),
  // Places
  placesProvider: env.PLACES_PROVIDER || 'osm', // osm | google
  googlePlacesKey: env.GOOGLE_PLACES_KEY || '',
  overpassUrl: env.OVERPASS_URL || 'https://overpass-api.de/api/interpreter',
  // Routing (OSRM public demo server by default)
  osrmUrl: env.OSRM_URL || 'https://router.project-osrm.org',
  // SMS (Twilio). If unset, SMS is logged to the outbox table only.
  twilioSid: env.TWILIO_SID || '',
  twilioToken: env.TWILIO_TOKEN || '',
  twilioFrom: env.TWILIO_FROM || '',
  // Safety net
  watchdogMinutes: Number(env.WATCHDOG_MINUTES || 5),
  watchdogTickMs: Number(env.WATCHDOG_TICK_MS || 5000),
  sessionTtlHours: Number(env.SESSION_TTL_HOURS || 72),
  journeyTtlHours: Number(env.JOURNEY_TTL_HOURS || 72),
  // Where the built APK lives. If public/VERA-Guard.apk is absent the server fetches this URL (e.g. the GitHub release asset) and caches it.
  apkUrl: env.APK_URL || '',
  maxBodyKb: 256,
};
