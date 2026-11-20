'use strict';
process.env.DB_PATH = ':memory:';
process.env.PUBLIC_URL = 'https://vera.test';
process.env.LLM_API_KEY = '';
const { createApp } = require('../src/app');
const { open } = require('../src/db');

async function boot(deps = {}) {
  const db = open(':memory:');
  const { app } = createApp({ db, deps: { nearby: async () => ({ ok: true, hospitals: [{ name: 'City Hospital', type: 'hospital', lat: 12.97, lng: 77.59, distanceM: 800, phone: '+918012345678', emergency: true }], clinics: [], pharmacies: [] }), ...deps } });
  const server = await new Promise((r) => { const s = app.listen(0, () => r(s)); });
  const base = `http://127.0.0.1:${server.address().port}`;
  const call = async (method, path, body, headers = {}) => {
    const res = await fetch(base + path, { method, headers: { 'content-type': 'application/json', ...headers }, body: body ? JSON.stringify(body) : undefined });
    let json = null; try { json = await res.json(); } catch { /* none */ }
    return { status: res.status, json };
  };
  return { db, call, close: () => server.close() };
}
module.exports = { boot };
