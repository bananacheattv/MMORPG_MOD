'use strict';
// Configuration du back-end : variables d'environnement (fichier .env, voir .env.example) + packs.json.
const fs = require('node:fs');
const path = require('node:path');

const ROOT = path.join(__dirname, '..');
const LOCAL_HTTP = /^http:\/\/(localhost|127\.0\.0\.1|\[::1\])(:\d+)?$/;

function loadPacks(file) {
  const raw = JSON.parse(fs.readFileSync(file, 'utf8'));
  const currency = String(raw.currency || 'eur').toLowerCase();
  if (!/^[a-z]{3}$/.test(currency)) throw new Error('packs.json : devise invalide.');
  const ids = new Set();
  const packs = (raw.packs || []).map((p) => {
    const pack = {
      id: String(p.id || ''),
      name: String(p.name || ''),
      credits: Number(p.credits),
      price: Number(p.price),
      bonus: p.bonus ? String(p.bonus) : '',
      highlight: p.highlight === true,
    };
    if (!/^[a-z0-9_-]{2,32}$/.test(pack.id) || ids.has(pack.id)) throw new Error(`packs.json : identifiant invalide ou en double (${pack.id}).`);
    if (!pack.name || pack.name.length > 60) throw new Error(`packs.json : nom invalide (${pack.id}).`);
    if (!Number.isInteger(pack.credits) || pack.credits <= 0 || pack.credits > 1000000) throw new Error(`packs.json : crédits invalides (${pack.id}).`);
    // Stripe : 0,50 € minimum ; plafond volontairement bas pour un pack de crédits.
    if (!Number.isInteger(pack.price) || pack.price < 50 || pack.price > 50000) throw new Error(`packs.json : prix en centimes invalide (${pack.id}).`);
    ids.add(pack.id);
    return pack;
  });
  if (!packs.length || packs.length > 30) throw new Error('packs.json : 1 à 30 packs.');
  return { currency, packs };
}

function loadConfig(env = process.env) {
  const errors = [];
  const siteUrl = String(env.PUBLIC_SITE_URL || '').replace(/\/+$/, '');
  if (!/^https:\/\/[a-z0-9.-]+(:\d+)?$/i.test(siteUrl) && !LOCAL_HTTP.test(siteUrl)) {
    errors.push('PUBLIC_SITE_URL : adresse https du site, sans chemin (ex. https://aeloria.netlify.app).');
  }
  const extraOrigins = String(env.CORS_EXTRA_ORIGINS || '').split(',').map((o) => o.trim().replace(/\/+$/, '')).filter(Boolean);
  for (const origin of extraOrigins) {
    if (!/^https:\/\/[a-z0-9.-]+(:\d+)?$/i.test(origin) && !LOCAL_HTTP.test(origin)) errors.push(`CORS_EXTRA_ORIGINS : origine invalide (${origin}).`);
  }

  const stripeKey = String(env.STRIPE_SECRET_KEY || '');
  const webhookSecret = String(env.STRIPE_WEBHOOK_SECRET || '');
  const live = /^(sk|rk)_live_/.test(stripeKey);
  const allowLive = env.ALLOW_LIVE_PAYMENTS === 'oui';
  if (stripeKey && !/^(sk|rk)_(test|live)_[A-Za-z0-9]+$/.test(stripeKey)) errors.push('STRIPE_SECRET_KEY invalide (sk_test_… attendu).');
  // Garde-fou : aucun paiement réel tant que le propriétaire ne l'a pas explicitement activé.
  if (live && !allowLive) errors.push('Clé Stripe LIVE refusée : les paiements réels sont désactivés (ALLOW_LIVE_PAYMENTS=oui pour les activer).');
  if (stripeKey && !/^whsec_[A-Za-z0-9]+$/.test(webhookSecret)) errors.push('STRIPE_WEBHOOK_SECRET (whsec_…) requis avec STRIPE_SECRET_KEY.');

  const creditSecret = String(env.AELORIA_CREDIT_SECRET || '');
  if (creditSecret && creditSecret.length < 32) errors.push('AELORIA_CREDIT_SECRET : 32 caractères minimum, identique à celui du serveur Minecraft.');
  if (stripeKey && !creditSecret) errors.push('AELORIA_CREDIT_SECRET requis pour livrer les crédits achetés.');
  const bridgeUrl = String(env.CREDIT_BRIDGE_URL || 'http://127.0.0.1:8099').replace(/\/+$/, '');
  if (!LOCAL_HTTP.test(bridgeUrl)) errors.push('CREDIT_BRIDGE_URL doit rester sur la machine du serveur (http://127.0.0.1:8099).');

  const port = Number(env.PORT || 8787);
  if (!Number.isInteger(port) || port < 1 || port > 65535) errors.push('PORT invalide.');

  let shop = null;
  try {
    shop = loadPacks(path.resolve(ROOT, env.PACKS_FILE || 'packs.json'));
  } catch (error) {
    errors.push(error.message);
  }
  if (errors.length) throw new Error('Configuration invalide :\n- ' + errors.join('\n- '));

  return {
    port,
    host: env.HOST || '127.0.0.1',
    trustProxy: env.TRUST_PROXY === 'oui',
    siteUrl,
    origins: new Set([siteUrl, ...extraOrigins]),
    databaseFile: env.DATABASE_FILE === ':memory:' ? ':memory:' : path.resolve(ROOT, env.DATABASE_FILE || 'data/aeloria.db'),
    stripe: stripeKey ? {
      secretKey: stripeKey,
      webhookSecret,
      live,
      apiBase: String(env.STRIPE_API_BASE || 'https://api.stripe.com').replace(/\/+$/, ''),
    } : null,
    bridge: creditSecret ? { url: bridgeUrl, secret: creditSecret } : null,
    mojangSessionUrl: String(env.MOJANG_SESSION_URL || 'https://sessionserver.mojang.com').replace(/\/+$/, ''),
    shop,
  };
}

module.exports = { loadConfig, loadPacks };
