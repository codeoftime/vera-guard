'use strict';
const { DatabaseSync } = require('node:sqlite');

function open(path) {
  const db = new DatabaseSync(path);
  db.exec(`
    PRAGMA journal_mode = WAL;
    CREATE TABLE IF NOT EXISTS sessions (
      token TEXT PRIMARY KEY, write_key TEXT NOT NULL, owner_id TEXT,
      kind TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'not_responding',
      created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, expires_at INTEGER NOT NULL,
      ended_at INTEGER, acked_at INTEGER, acked_by TEXT, demo INTEGER DEFAULT 0,
      lat REAL, lng REAL, accuracy REAL, loc_ts INTEGER, battery INTEGER,
      patient TEXT, event TEXT, contacts TEXT, facilities TEXT, journey_token TEXT,
      audio_count INTEGER DEFAULT 0
    );
    CREATE TABLE IF NOT EXISTS locations (
      id INTEGER PRIMARY KEY AUTOINCREMENT, token TEXT NOT NULL,
      lat REAL NOT NULL, lng REAL NOT NULL, accuracy REAL, ts INTEGER NOT NULL
    );
    CREATE INDEX IF NOT EXISTS idx_loc_token ON locations(token, ts);
    CREATE TABLE IF NOT EXISTS journeys (
      token TEXT PRIMARY KEY, write_key TEXT NOT NULL, owner_id TEXT,
      name TEXT, phone TEXT, mode TEXT, dest_name TEXT, dest_lat REAL, dest_lng REAL,
      route TEXT, eta_ts INTEGER NOT NULL, buffer_s INTEGER DEFAULT 0,
      status TEXT NOT NULL DEFAULT 'on_route', started_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL, expires_at INTEGER NOT NULL, ended_at INTEGER,
      lat REAL, lng REAL, loc_ts INTEGER, battery INTEGER, online INTEGER DEFAULT 1,
      followers TEXT, cab TEXT, patient TEXT, watchdog_sec INTEGER DEFAULT 300, stopped INTEGER DEFAULT 0, low_bat_alerted INTEGER DEFAULT 0, deviated_since INTEGER,
      watchdog_alerted INTEGER DEFAULT 0, overdue_alerted INTEGER DEFAULT 0,
      alert_session TEXT, demo INTEGER DEFAULT 0, notify INTEGER DEFAULT 1, lang TEXT DEFAULT 'en'
    );
    CREATE TABLE IF NOT EXISTS jobs (
      id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, ref TEXT NOT NULL,
      run_at INTEGER NOT NULL, done INTEGER DEFAULT 0,
      UNIQUE(kind, ref)
    );
    CREATE INDEX IF NOT EXISTS idx_jobs_due ON jobs(done, run_at);
    CREATE TABLE IF NOT EXISTS outbox (
      id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, to_phone TEXT,
      body TEXT, status TEXT
    );
    CREATE TABLE IF NOT EXISTS brief_cache (
      key TEXT PRIMARY KEY, ts INTEGER NOT NULL, value TEXT NOT NULL
    );
  `);
  return db;
}

const j = (v) => (v === undefined || v === null ? null : JSON.stringify(v));
const p = (s, d = null) => { try { return s ? JSON.parse(s) : d; } catch { return d; } };

module.exports = { open, j, p };
