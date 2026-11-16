'use strict';
// Appels sortants : Stripe (API REST, sans SDK), passerelle crédits du serveur Minecraft, serveur de session Mojang.
const { hmacHex, safeEqualHex } = require('./security');

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

// ---------- Stripe ----------
function formEncode(value, prefix = '', out = new URLSearchParams()) {
  if (value === undefined || value === null) return out;
  if (typeof value === 'object') {
    for (const [key, inner] of Object.entries(value)) formEncode(inner, prefix ? `${prefix}[${key}]` : key, out);
  } else {
    out.append(prefix, String(value));
  }
  return out;
}

async function createCheckoutSession(stripe, fetchImpl, params, idempotencyKey) {
  const res = await fetchImpl(`${stripe.apiBase}/v1/checkout/sessions`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${stripe.secretKey}`,
      'Content-Type': 'application/x-www-form-urlencoded',
      'Idempotency-Key': idempotencyKey,
      'Stripe-Version': '2024-06-20',
    },
    body: formEncode(params).toString(),
    signal: AbortSignal.timeout(15000),
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok || !json.id || !json.url) {
    throw new HttpError(502, `Paiement indisponible (${json.error?.message || 'Stripe HTTP ' + res.status}).`);
  }
  return json;
}

// En-tête Stripe-Signature : « t=…,v1=… » ; HMAC-SHA256 de « t.corps » avec le secret whsec_.
function verifyStripeSignature(rawBody, header, secret, nowSeconds = Math.floor(Date.now() / 1000), tolerance = 300) {
  const parts = String(header || '').split(',').map((p) => p.split('='));
  const timestamp = Number(parts.find(([k]) => k === 't')?.[1]);
  const signatures = parts.filter(([k]) => k === 'v1').map(([, v]) => v);
  if (!Number.isInteger(timestamp) || !signatures.length) throw new HttpError(400, 'Signature Stripe absente.');
  if (Math.abs(nowSeconds - timestamp) > tolerance) throw new HttpError(400, 'Signature Stripe expirée.');
  const expected = hmacHex(secret, `${timestamp}.${rawBody.toString('utf8')}`);
  if (!signatures.some((s) => safeEqualHex(s, expected))) throw new HttpError(400, 'Signature Stripe invalide.');
  return JSON.parse(rawBody.toString('utf8'));
}

// ---------- passerelle crédits (CreditBridge du mod, en boucle locale) ----------
async function bridgeCall(bridge, fetchImpl, route, payload) {
  const body = JSON.stringify({ ...payload, timestamp: Math.floor(Date.now() / 1000) });
  let res;
  try {
    res = await fetchImpl(`${bridge.url}${route}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Aeloria-Signature': hmacHex(bridge.secret, body) },
      body,
      signal: AbortSignal.timeout(12000),
    });
  } catch (error) {
    return { ok: false, retry: true, message: `Serveur Minecraft injoignable (${error.cause?.code || error.name}).` };
  }
  const json = await res.json().catch(() => ({}));
  if (res.ok && json.ok) return { ok: true, ...json };
  // 4xx : requête refusée définitivement (pack inconnu, référence en conflit…) ; 5xx : on réessaie plus tard.
  return { ok: false, retry: res.status >= 500 || res.status === 401, message: json.message || `HTTP ${res.status}` };
}

// ---------- Minecraft : preuve de possession par le protocole de session (comme un vrai serveur) ----------
// Le launcher appelle /session/minecraft/join avec son jeton ; le back-end ne voit jamais ce jeton,
// il vérifie seulement auprès de Mojang que ce joueur a bien rejoint « serverId ».
async function hasJoined(sessionUrl, fetchImpl, username, serverId) {
  const url = `${sessionUrl}/session/minecraft/hasJoined?username=${encodeURIComponent(username)}&serverId=${encodeURIComponent(serverId)}`;
  let res;
  try {
    res = await fetchImpl(url, { signal: AbortSignal.timeout(10000) });
  } catch {
    throw new HttpError(502, 'Vérification Minecraft indisponible, réessaie dans un instant.');
  }
  if (res.status === 204) return null;
  if (!res.ok) throw new HttpError(502, 'Vérification Minecraft indisponible, réessaie dans un instant.');
  const json = await res.json().catch(() => null);
  if (!json || !/^[0-9a-f]{32}$/i.test(json.id || '') || !/^[A-Za-z0-9_]{1,16}$/.test(json.name || '')) return null;
  const id = json.id.toLowerCase();
  return { uuid: `${id.slice(0, 8)}-${id.slice(8, 12)}-${id.slice(12, 16)}-${id.slice(16, 20)}-${id.slice(20)}`, name: json.name };
}

module.exports = { HttpError, formEncode, createCheckoutSession, verifyStripeSignature, bridgeCall, hasJoined };
