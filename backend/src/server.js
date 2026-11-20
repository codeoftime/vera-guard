'use strict';
const cfg = require('./config');
const { createApp } = require('./app');
const watchdog = require('./watchdog');
const { app, db } = createApp();
watchdog.start(db);
app.listen(cfg.port, () => console.log(`VERA Guard backend on :${cfg.port} (public ${cfg.publicUrl})`));
