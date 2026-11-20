'use strict';
const cfg = require('./config');
// Sends SMS through Twilio when configured; always records to the outbox table (useful for the demo).
async function sendSms(db, to, body) {
  const ts = Date.now();
  let status = 'logged';
  if (cfg.twilioSid && cfg.twilioToken && cfg.twilioFrom) {
    try {
      const res = await fetch(`https://api.twilio.com/2010-04-01/Accounts/${cfg.twilioSid}/Messages.json`, {
        method: 'POST',
        headers: {
          Authorization: 'Basic ' + Buffer.from(`${cfg.twilioSid}:${cfg.twilioToken}`).toString('base64'),
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({ To: to, From: cfg.twilioFrom, Body: body }),
        signal: AbortSignal.timeout(8000),
      });
      status = res.ok ? 'sent' : `failed:${res.status}`;
    } catch (e) { status = 'failed:' + (e.name || 'error'); }
  }
  db.prepare('INSERT INTO outbox(ts,to_phone,body,status) VALUES (?,?,?,?)').run(ts, to, body, status);
  if (status === 'logged') console.log(`[sms:log] -> ${to}: ${body}`);
  return status;
}
module.exports = { sendSms };
