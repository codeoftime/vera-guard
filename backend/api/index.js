'use strict';
// Vercel entry. Serverless is stateless: the database lives in /tmp (per instance) and the watchdog timer cannot run here.
// Use the Docker/Render deployment (render.yaml) when you need the server-side dead-man's switch and durable tracking links.
process.env.DB_PATH = process.env.DB_PATH || '/tmp/vera.db';
const { createApp } = require('../src/app');
const { app } = createApp();
module.exports = app;
