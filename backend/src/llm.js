'use strict';
const cfg = require('./config');
// Minimal Anthropic Messages API client. Returns null on any failure so callers fall back.
async function complete({ system, messages, maxTokens = 300, timeoutMs = cfg.llmTimeoutMs, fetchImpl = fetch }) {
  if (!cfg.llmKey) return null;
  try {
    const res = await fetchImpl('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: { 'x-api-key': cfg.llmKey, 'anthropic-version': '2023-06-01', 'content-type': 'application/json' },
      body: JSON.stringify({ model: cfg.llmModel, max_tokens: maxTokens, system, messages }),
      signal: AbortSignal.timeout(timeoutMs),
    });
    if (!res.ok) return null;
    const data = await res.json();
    const text = (data.content || []).filter((b) => b.type === 'text').map((b) => b.text).join('').trim();
    return text || null;
  } catch { return null; }
}
module.exports = { complete };
