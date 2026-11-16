'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const crypto = require('node:crypto');
const { loadConfig } = require('../src/config');
const { createApp } = require('../src/app');
const { verifyStripeSignature } = require('../src/services');

const SECRET = 'x'.repeat(40);
const WHSEC = 'whsec_testsecret123';
const ENV = {
  PUBLIC_SITE_URL: 'https://aeloria.example',
  DATABASE_FILE: ':memory:',
  STRIPE_SECRET_KEY: 'sk_test_abc123',
  STRIPE_WEBHOOK_SECRET: WHSEC,
  STRIPE_API_BASE: 'https://stripe.mock',
  AELORIA_CREDIT_SECRET: SECRET,
  CREDIT_BRIDGE_URL: 'http://127.0.0.1:8099',
  MOJANG_SESSION_URL: 'https://mojang.mock',
};
const PLAYER = { id: '0123456789abcdef0123456789abcdef', name: 'Copassist' };
const PLAYER_UUID = '01234567-89ab-cdef-0123-456789abcdef';

// Faux services externes : Stripe, Mojang, passerelle crédits du mod (signature vérifiée comme en Java).
function fakeWorld() {
  const world = { sessions: [], joined: new Set(), ledger: new Map(), receipts: new Map(), bridgeUp: true, calls: [] };
  world.fetch = async (input, init = {}) => {
    const url = new URL(input);
    world.calls.push(url.href);
    if (url.origin === 'https://stripe.mock') {
      const form = new URLSearchParams(init.body);
      const session = { id: `cs_test_${world.sessions.length + 1}`, url: `https://checkout.stripe.mock/${world.sessions.length + 1}`, form, headers: init.headers };
      world.sessions.push(session);
      return Response.json({ id: session.id, url: session.url });
    }
    if (url.origin === 'https://mojang.mock') {
      const key = `${url.searchParams.get('username')}|${url.searchParams.get('serverId')}`;
      return world.joined.has(key) ? Response.json(PLAYER) : new Response(null, { status: 204 });
    }
    if (url.origin === 'http://127.0.0.1:8099') {
      if (!world.bridgeUp) throw Object.assign(new TypeError('fetch failed'), { cause: { code: 'ECONNREFUSED' } });
      const signature = crypto.createHmac('sha256', SECRET).update(init.body).digest('hex');
      if (init.headers['X-Aeloria-Signature'] !== signature) return Response.json({ ok: false, message: 'Signature invalide' }, { status: 401 });
      const body = JSON.parse(init.body);
      assert.ok(Math.abs(body.timestamp - Date.now() / 1000) < 5);
      if (url.pathname === '/balance') return Response.json({ ok: true, message: 'Solde', credits: world.ledger.get(body.player) || 0 });
      const amount = { decouverte: 500, aventurier: 1200, legende: 3000 }[body.pack];
      if (!world.receipts.has(body.payment)) {
        world.receipts.set(body.payment, amount);
        world.ledger.set(body.player, (world.ledger.get(body.player) || 0) + amount);
      }
      return Response.json({ ok: true, message: 'Crédits enregistrés', credits: amount });
    }
    throw new Error(`URL inattendue ${url}`);
  };
  return world;
}

async function start(env = ENV) {
  const world = fakeWorld();
  const quiet = { info() {}, warn() {}, error() {} };
  const app = createApp(loadConfig(env), { fetchImpl: world.fetch, log: quiet });
  const server = http.createServer((req, res) => app.handle(req, res));
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  const call = async (method, path, { body, token, headers = {}, raw } = {}) => {
    const res = await fetch(base + path, {
      method,
      headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
      body: raw ?? (body ? JSON.stringify(body) : undefined),
    });
    return { status: res.status, headers: res.headers, json: await res.json().catch(() => null) };
  };
  const stop = () => new Promise((resolve) => server.close(() => { app.close(); resolve(); }));
  return { app, world, call, stop };
}

function stripeEvent(id, type, object) {
  const raw = JSON.stringify({ id, type, livemode: false, data: { object } });
  const t = Math.floor(Date.now() / 1000);
  const v1 = crypto.createHmac('sha256', WHSEC).update(`${t}.${raw}`).digest('hex');
  return { raw, headers: { 'Content-Type': 'application/json', 'Stripe-Signature': `t=${t},v1=${v1}` } };
}

async function registerAndLink(t, name = 'Aventurier', email = 'aventurier@example.com') {
  const reg = await t.call('POST', '/api/auth/register', { body: { username: name, email, password: 'motdepasse-solide', acceptRules: true } });
  assert.equal(reg.status, 201, JSON.stringify(reg.json));
  const token = reg.json.token;
  const challenge = await t.call('POST', '/api/account/minecraft/challenge', { token });
  t.world.joined.add(`${PLAYER.name}|${challenge.json.serverId}`);
  const link = await t.call('POST', '/api/account/minecraft', { token, body: { serverId: challenge.json.serverId, minecraftName: PLAYER.name } });
  return { token, link };
}

test('création de compte, connexion, déconnexion', async () => {
  const t = await start();
  try {
    const bad = await t.call('POST', '/api/auth/register', { body: { username: 'a', email: 'x', password: 'court' } });
    assert.equal(bad.status, 400);
    const reg = await t.call('POST', '/api/auth/register', { body: { username: 'Lyra_01', email: 'Lyra@Example.com', password: 'motdepasse-solide', acceptRules: true } });
    assert.equal(reg.status, 201);
    assert.equal(reg.json.account.email, 'lyra@example.com');
    const dup = await t.call('POST', '/api/auth/register', { body: { username: 'lyra_01', email: 'autre@example.com', password: 'motdepasse-solide', acceptRules: true } });
    assert.equal(dup.status, 409);
    const wrong = await t.call('POST', '/api/auth/login', { body: { login: 'Lyra_01', password: 'mauvais-mot-de-passe' } });
    assert.equal(wrong.status, 401);
    const unknown = await t.call('POST', '/api/auth/login', { body: { login: 'personne', password: 'mauvais-mot-de-passe' } });
    assert.equal(unknown.status, 401);
    const login = await t.call('POST', '/api/auth/login', { body: { login: 'LYRA@example.com', password: 'motdepasse-solide', client: 'launcher' } });
    assert.equal(login.status, 200);
    const me = await t.call('GET', '/api/account', { token: login.json.token });
    assert.equal(me.json.account.username, 'Lyra_01');
    assert.equal(me.json.account.minecraft, null);
    assert.equal(me.json.credits, null);
    assert.equal((await t.call('POST', '/api/auth/logout', { token: login.json.token })).status, 200);
    assert.equal((await t.call('GET', '/api/account', { token: login.json.token })).status, 401);
    assert.equal((await t.call('GET', '/api/account', { token: reg.json.token })).status, 200, 'autre session intacte');
  } finally {
    await t.stop();
  }
});

test('liaison Minecraft prouvée par Mojang, jamais en double', async () => {
  const t = await start();
  try {
    const reg = await t.call('POST', '/api/auth/register', { body: { username: 'Sans_preuve', email: 'a@example.com', password: 'motdepasse-solide', acceptRules: true } });
    const challenge = await t.call('POST', '/api/account/minecraft/challenge', { token: reg.json.token });
    const refused = await t.call('POST', '/api/account/minecraft', { token: reg.json.token, body: { serverId: challenge.json.serverId, minecraftName: PLAYER.name } });
    assert.equal(refused.status, 403, 'pas de join Mojang : refus');
    const reused = await t.call('POST', '/api/account/minecraft', { token: reg.json.token, body: { serverId: challenge.json.serverId, minecraftName: PLAYER.name } });
    assert.equal(reused.status, 400, 'un défi ne sert qu’une fois');

    const { link } = await registerAndLink(t);
    assert.equal(link.status, 200, JSON.stringify(link.json));
    assert.deepEqual(link.json.account.minecraft, { uuid: PLAYER_UUID, name: PLAYER.name });

    const other = await registerAndLink(t, 'Second', 'second@example.com');
    assert.equal(other.link.status, 409, 'un compte Minecraft = un compte Aéloria');
  } finally {
    await t.stop();
  }
});

test('mot de passe oublié via le compte Minecraft lié', async () => {
  const t = await start();
  try {
    await registerAndLink(t);
    const challenge = await t.call('POST', '/api/auth/minecraft-challenge');
    t.world.joined.add(`${PLAYER.name}|${challenge.json.serverId}`);
    const reset = await t.call('POST', '/api/auth/reset-with-minecraft', { body: { serverId: challenge.json.serverId, minecraftName: PLAYER.name, newPassword: 'nouveau-mot-de-passe', client: 'launcher' } });
    assert.equal(reset.status, 200, JSON.stringify(reset.json));
    assert.equal(reset.json.account.username, 'Aventurier');
    assert.equal((await t.call('POST', '/api/auth/login', { body: { login: 'Aventurier', password: 'motdepasse-solide' } })).status, 401);
    assert.equal((await t.call('POST', '/api/auth/login', { body: { login: 'Aventurier', password: 'nouveau-mot-de-passe' } })).status, 200);
  } finally {
    await t.stop();
  }
});

test('achat de crédits : Stripe test, webhook signé, livraison au serveur', async () => {
  const t = await start();
  try {
    const packs = await t.call('GET', '/api/shop/packs');
    assert.equal(packs.json.open, true);
    assert.equal(packs.json.test, true);
    const reg = await t.call('POST', '/api/auth/register', { body: { username: 'Pas_lie', email: 'p@example.com', password: 'motdepasse-solide', acceptRules: true } });
    assert.equal((await t.call('POST', '/api/shop/checkout', { token: reg.json.token, body: { pack: 'aventurier', waiver: true } })).status, 409);

    const { token } = await registerAndLink(t);
    assert.equal((await t.call('POST', '/api/shop/checkout', { token, body: { pack: 'aventurier' } })).status, 400, 'renonciation requise');
    assert.equal((await t.call('POST', '/api/shop/checkout', { token, body: { pack: 'inconnu', waiver: true } })).status, 400);
    const checkout = await t.call('POST', '/api/shop/checkout', { token, body: { pack: 'aventurier', waiver: true } });
    assert.equal(checkout.status, 200, JSON.stringify(checkout.json));
    assert.match(checkout.json.url, /^https:\/\/checkout\.stripe\.mock\//);
    const session = t.world.sessions[0];
    assert.equal(session.form.get('line_items[0][price_data][unit_amount]'), '999');
    assert.equal(session.form.get('client_reference_id'), checkout.json.order);
    assert.equal(session.form.get('success_url'), `https://aeloria.example/boutique.html?commande=${checkout.json.order}`);
    assert.equal(session.headers['Idempotency-Key'], checkout.json.order);

    const forged = stripeEvent('evt_1', 'checkout.session.completed', {});
    assert.equal((await t.call('POST', '/api/stripe/webhook', { raw: forged.raw, headers: { ...forged.headers, 'Stripe-Signature': 't=1,v1=00' } })).status, 400);

    const paidSession = { id: session.id, client_reference_id: checkout.json.order, payment_status: 'paid', amount_total: 999, currency: 'eur', payment_intent: 'pi_1', metadata: { order: checkout.json.order } };
    const event = stripeEvent('evt_paid', 'checkout.session.completed', paidSession);
    assert.equal((await t.call('POST', '/api/stripe/webhook', { raw: event.raw, headers: event.headers })).status, 200);
    assert.equal((await t.call('POST', '/api/stripe/webhook', { raw: event.raw, headers: event.headers })).json.duplicate, true);
    await new Promise((r) => setTimeout(r, 50));
    assert.equal(t.world.ledger.get(PLAYER_UUID), 1200);
    const me = await t.call('GET', '/api/account', { token });
    assert.equal(me.json.credits, 1200);
    assert.equal(me.json.orders[0].status, 'delivered');
    assert.equal(me.json.orders[0].test, true);
  } finally {
    await t.stop();
  }
});

test('serveur Minecraft arrêté : la commande payée est livrée plus tard', async () => {
  const t = await start();
  try {
    const { token } = await registerAndLink(t);
    const checkout = await t.call('POST', '/api/shop/checkout', { token, body: { pack: 'legende', waiver: true } });
    t.world.bridgeUp = false;
    const object = { id: t.world.sessions[0].id, client_reference_id: checkout.json.order, payment_status: 'paid', amount_total: 1999, currency: 'eur' };
    const event = stripeEvent('evt_down', 'checkout.session.completed', object);
    assert.equal((await t.call('POST', '/api/stripe/webhook', { raw: event.raw, headers: event.headers })).status, 200);
    await new Promise((r) => setTimeout(r, 50));
    let orders = (await t.call('GET', '/api/shop/orders', { token })).json.orders;
    assert.equal(orders[0].status, 'paid');
    t.world.bridgeUp = true;
    t.app.store.db.exec('UPDATE orders SET next_attempt_at = 0');
    await t.app.deliverDue();
    orders = (await t.call('GET', '/api/shop/orders', { token })).json.orders;
    assert.equal(orders[0].status, 'delivered');
    assert.equal(t.world.ledger.get(PLAYER_UUID), 3000);
  } finally {
    await t.stop();
  }
});

test('montant incohérent : pas de livraison', async () => {
  const t = await start();
  try {
    const { token } = await registerAndLink(t);
    const checkout = await t.call('POST', '/api/shop/checkout', { token, body: { pack: 'legende', waiver: true } });
    const event = stripeEvent('evt_cheap', 'checkout.session.completed', { id: t.world.sessions[0].id, client_reference_id: checkout.json.order, payment_status: 'paid', amount_total: 50, currency: 'eur' });
    await t.call('POST', '/api/stripe/webhook', { raw: event.raw, headers: event.headers });
    assert.equal((await t.call('GET', '/api/shop/orders', { token })).json.orders[0].status, 'failed');
    assert.equal(t.world.ledger.get(PLAYER_UUID), undefined);
  } finally {
    await t.stop();
  }
});

test('CORS limité au site, boutique fermée sans clé', async () => {
  const env = { ...ENV, STRIPE_SECRET_KEY: '', STRIPE_WEBHOOK_SECRET: '' };
  const t = await start(env);
  try {
    const allowed = await t.call('GET', '/api/health', { headers: { Origin: 'https://aeloria.example' } });
    assert.equal(allowed.headers.get('access-control-allow-origin'), 'https://aeloria.example');
    assert.equal(allowed.json.shop, 'fermee');
    const other = await t.call('GET', '/api/health', { headers: { Origin: 'https://pirate.example' } });
    assert.equal(other.headers.get('access-control-allow-origin'), null);
    const { token } = await registerAndLink(t);
    assert.equal((await t.call('POST', '/api/shop/checkout', { token, body: { pack: 'legende', waiver: true } })).status, 503);
  } finally {
    await t.stop();
  }
});

test('configuration : paiements réels refusés sans accord explicite', () => {
  assert.throws(() => loadConfig({ ...ENV, STRIPE_SECRET_KEY: 'sk_live_abc' }), /LIVE refusée/);
  assert.equal(loadConfig({ ...ENV, STRIPE_SECRET_KEY: 'sk_live_abc', ALLOW_LIVE_PAYMENTS: 'oui' }).stripe.live, true);
  assert.throws(() => loadConfig({ ...ENV, CREDIT_BRIDGE_URL: 'http://192.168.0.10:8099' }), /CREDIT_BRIDGE_URL/);
  assert.throws(() => loadConfig({ ...ENV, AELORIA_CREDIT_SECRET: 'court' }), /32 caractères/);
  assert.throws(() => verifyStripeSignature(Buffer.from('{}'), 't=1,v1=00', WHSEC), /expirée/);
});
