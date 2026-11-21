'use strict';
// API HTTP : comptes (site + launcher), liaison Minecraft, boutique de crédits, webhook Stripe, livraison au serveur.
const { openStore } = require('./db');
const security = require('./security');
const { HttpError, createCheckoutSession, verifyStripeSignature, bridgeCall, hasJoined } = require('./services');

const DAY = 86400000;
const SESSION_TTL = { web: 30 * DAY, launcher: 180 * DAY };
const CHALLENGE_TTL = 120000;
const USERNAME = /^[A-Za-z0-9_]{3,16}$/;
const EMAIL = /^[^\s@]{1,64}@[^\s@]{1,190}\.[^\s@]{2,24}$/;

function createApp(cfg, { fetchImpl = fetch, now = Date.now, log = console } = {}) {
  const store = openStore(cfg.databaseFile);
  const { st } = store;
  const limits = new Map();

  // ---------- utilitaires ----------
  function rateLimit(req, bucket, max, windowMs) {
    const forwarded = cfg.trustProxy ? String(req.headers['x-forwarded-for'] || '').split(',').pop().trim() : '';
    const key = `${bucket}:${forwarded || req.socket.remoteAddress}`;
    const t = now();
    if (limits.size > 5000) for (const [k, v] of limits) if (v.reset <= t) limits.delete(k);
    const entry = limits.get(key);
    if (!entry || entry.reset <= t) {
      limits.set(key, { count: 1, reset: t + windowMs });
      return;
    }
    if (++entry.count > max) throw new HttpError(429, 'Trop de tentatives. Patiente quelques minutes.');
  }

  function readBody(req, limit) {
    return new Promise((resolve, reject) => {
      const chunks = [];
      let size = 0;
      req.on('data', (chunk) => {
        size += chunk.length;
        if (size > limit) {
          reject(new HttpError(413, 'Requête trop grande.'));
          req.destroy();
        } else chunks.push(chunk);
      });
      req.on('end', () => resolve(Buffer.concat(chunks)));
      req.on('error', reject);
    });
  }

  async function readJson(req) {
    if (!String(req.headers['content-type'] || '').startsWith('application/json')) throw new HttpError(415, 'JSON attendu.');
    try {
      const value = JSON.parse((await readBody(req, 16384)).toString('utf8'));
      if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error();
      return value;
    } catch (error) {
      if (error instanceof HttpError) throw error;
      throw new HttpError(400, 'JSON invalide.');
    }
  }

  const text = (value, max) => (typeof value === 'string' ? value.trim().slice(0, max) : '');

  function checkPassword(password) {
    if (typeof password !== 'string' || password.length < 10 || password.length > 128) {
      throw new HttpError(400, 'Le mot de passe doit faire entre 10 et 128 caractères.');
    }
  }

  function authenticate(req) {
    const match = /^Bearer ([A-Za-z0-9_-]{20,100})$/.exec(String(req.headers.authorization || ''));
    const account = match && st.sessionAccount.get(security.sha256(match[1]), now());
    if (!account) throw new HttpError(401, 'Session expirée : reconnecte-toi.');
    return { account, tokenHash: security.sha256(match[1]) };
  }

  function openSession(account, client) {
    const kind = client === 'launcher' ? 'launcher' : 'web';
    const token = security.randomId('', 32);
    const t = now();
    st.insertSession.run(security.sha256(token), account.id, kind, t, t + SESSION_TTL[kind]);
    return token;
  }

  function publicOrder(o) {
    return {
      id: o.id, pack: o.pack, credits: o.credits, amount: o.amount, currency: o.currency, status: o.status,
      test: !o.live, createdAt: new Date(o.created_at).toISOString(),
      deliveredAt: o.delivered_at ? new Date(o.delivered_at).toISOString() : null,
    };
  }

  function publicAccount(a) {
    return {
      username: a.username,
      email: a.email,
      createdAt: new Date(a.created_at).toISOString(),
      minecraft: a.mc_uuid ? { uuid: a.mc_uuid, name: a.mc_name } : null,
    };
  }

  async function balanceOf(account) {
    if (!cfg.bridge || !account.mc_uuid) return null;
    const result = await bridgeCall(cfg.bridge, fetchImpl, '/balance', { player: account.mc_uuid });
    return result.ok && Number.isFinite(result.credits) ? result.credits : null;
  }

  function newChallenge(accountId, purpose) {
    const t = now();
    st.purgeChallenges.run(t);
    const serverId = security.randomId('', 15).replace(/[^A-Za-z0-9]/g, 'a');
    st.insertChallenge.run(serverId, accountId, purpose, t + CHALLENGE_TTL);
    return { serverId, expiresIn: CHALLENGE_TTL / 1000 };
  }

  async function provePlayer(body, purpose, accountId) {
    const serverId = text(body.serverId, 64);
    const name = text(body.minecraftName, 16);
    if (!serverId || !/^[A-Za-z0-9_]{1,16}$/.test(name)) throw new HttpError(400, 'Preuve Minecraft incomplète.');
    const challenge = st.takeChallenge.get(serverId, purpose, now());
    if (!challenge || (accountId !== undefined && challenge.account_id !== accountId)) {
      throw new HttpError(400, 'Défi Minecraft expiré : recommence.');
    }
    const profile = await hasJoined(cfg.mojangSessionUrl, fetchImpl, name, serverId);
    if (!profile) throw new HttpError(403, 'Mojang n’a pas confirmé ce compte Minecraft.');
    return profile;
  }

  // ---------- livraison des crédits ----------
  async function deliver(order) {
    if (!cfg.bridge) return;
    const result = await bridgeCall(cfg.bridge, fetchImpl, '/credits', { player: order.mc_uuid, pack: order.pack, payment: order.id });
    if (result.ok) {
      st.markDelivered.run(now(), order.id);
      log.info(`[boutique] ${order.id} livrée : ${order.credits} crédits pour ${order.mc_uuid}`);
    } else if (result.retry) {
      const delay = Math.min(3600000, 30000 * 2 ** Math.min(order.attempts, 7));
      st.markRetry.run(now() + delay, result.message, order.id);
      log.warn(`[boutique] ${order.id} en attente (${result.message}), nouvel essai dans ${Math.round(delay / 1000)} s`);
    } else {
      st.markStatus.run('failed', result.message, order.id, 'paid');
      log.error(`[boutique] ${order.id} refusée par le serveur : ${result.message} (à traiter à la main)`);
    }
  }

  let delivering = false;
  async function deliverDue() {
    if (delivering) return;
    delivering = true;
    try {
      for (const order of st.dueOrders.all(now())) await deliver(order);
    } catch (error) {
      log.error('[boutique] livraison :', error);
    } finally {
      delivering = false;
    }
  }

  // ---------- webhook Stripe ----------
  async function stripeWebhook(req) {
    if (!cfg.stripe) throw new HttpError(404, 'Boutique fermée.');
    const raw = await readBody(req, 262144);
    const event = verifyStripeSignature(raw, req.headers['stripe-signature'], cfg.stripe.webhookSecret, Math.floor(now() / 1000));
    if (event.livemode && !cfg.stripe.live) throw new HttpError(400, 'Événement LIVE refusé en mode test.');
    if (!st.insertEvent.run(event.id, now()).changes) return { received: true, duplicate: true };
    const object = event.data?.object || {};
    if (event.type.startsWith('checkout.session.')) {
      const order = st.orderById.get(String(object.client_reference_id || object.metadata?.order || ''));
      if (!order || order.stripe_session !== object.id) {
        log.warn(`[boutique] session Stripe inconnue ignorée : ${object.id}`);
        return { received: true };
      }
      const paid = event.type === 'checkout.session.async_payment_succeeded'
        || (event.type === 'checkout.session.completed' && object.payment_status === 'paid');
      if (paid) {
        if (object.amount_total !== order.amount || String(object.currency).toLowerCase() !== order.currency) {
          st.markStatus.run('failed', 'Montant payé différent du pack', order.id, 'pending');
          log.error(`[boutique] ${order.id} : montant incohérent (${object.amount_total} ${object.currency})`);
          return { received: true };
        }
        st.markPaid.run(now(), object.payment_intent ? String(object.payment_intent) : null, now(), order.id);
        setImmediate(() => deliverDue());
      } else if (event.type === 'checkout.session.expired') {
        st.markStatus.run('expired', null, order.id, 'pending');
      } else if (event.type === 'checkout.session.async_payment_failed') {
        st.markStatus.run('failed', 'Paiement refusé', order.id, 'pending');
      }
    } else if (event.type === 'charge.refunded' && object.payment_intent) {
      // Les crédits déjà livrés ne sont pas retirés automatiquement : l'admin décide en jeu.
      if (st.markRefunded.run(String(object.payment_intent)).changes) log.warn(`[boutique] remboursement Stripe : ${object.payment_intent}`);
    }
    return { received: true };
  }

  // ---------- routes ----------
  const routes = {
    'GET /api/health': async () => ({
      ok: true,
      shop: cfg.stripe ? (cfg.stripe.live ? 'live' : 'test') : 'fermee',
      delivery: Boolean(cfg.bridge),
    }),

    'POST /api/auth/register': async (req) => {
      rateLimit(req, 'register', 5, 3600000);
      const body = await readJson(req);
      const username = text(body.username, 32);
      const email = text(body.email, 254).toLowerCase();
      if (!USERNAME.test(username)) throw new HttpError(400, 'Pseudo : 3 à 16 caractères (lettres, chiffres, _).');
      if (!EMAIL.test(email)) throw new HttpError(400, 'Adresse e-mail invalide.');
      checkPassword(body.password);
      if (body.acceptRules !== true) throw new HttpError(400, 'Accepte le règlement pour créer un compte.');
      if (st.usernameTaken.get(username)) throw new HttpError(409, 'Ce pseudo est déjà pris.');
      if (st.emailTaken.get(email)) throw new HttpError(409, 'Un compte existe déjà avec cette adresse e-mail.');
      const hash = await security.hashPassword(body.password);
      let id;
      try {
        id = st.insertAccount.run(username, email, hash, now()).lastInsertRowid;
      } catch {
        throw new HttpError(409, 'Ce pseudo ou cette adresse e-mail est déjà utilisé.');
      }
      const account = st.accountById.get(id);
      return { status: 201, body: { token: openSession(account, body.client), account: publicAccount(account) } };
    },

    'POST /api/auth/login': async (req) => {
      rateLimit(req, 'login', 10, 900000);
      const body = await readJson(req);
      const login = text(body.login, 254);
      const password = typeof body.password === 'string' ? body.password.slice(0, 128) : '';
      const account = login && st.accountByLogin.get(login, login.toLowerCase());
      const valid = account ? await security.verifyPassword(password, account.password) : await security.burnPasswordCheck(password);
      if (!valid) throw new HttpError(401, 'Identifiant ou mot de passe incorrect.');
      st.purgeSessions.run(now());
      return { token: openSession(account, body.client), account: publicAccount(account) };
    },

    'POST /api/auth/logout': async (req) => {
      const { tokenHash } = authenticate(req);
      st.deleteSession.run(tokenHash);
      return { ok: true };
    },

    'GET /api/account': async (req) => {
      const { account } = authenticate(req);
      return {
        account: publicAccount(account),
        credits: await balanceOf(account),
        orders: st.ordersOf.all(account.id, 10).map(publicOrder),
      };
    },

    'POST /api/account/password': async (req) => {
      rateLimit(req, 'password', 10, 900000);
      const { account } = authenticate(req);
      const body = await readJson(req);
      if (!(await security.verifyPassword(String(body.currentPassword || '').slice(0, 128), account.password))) {
        throw new HttpError(401, 'Mot de passe actuel incorrect.');
      }
      checkPassword(body.newPassword);
      st.setPassword.run(await security.hashPassword(body.newPassword), account.id);
      st.deleteSessions.run(account.id);
      return { token: openSession(account, account.session_client), ok: true };
    },

    // Liaison du compte Minecraft : faite par le launcher après la connexion Microsoft.
    'POST /api/account/minecraft/challenge': async (req) => {
      rateLimit(req, 'challenge', 20, 900000);
      const { account } = authenticate(req);
      return newChallenge(account.id, 'link');
    },

    'POST /api/account/minecraft': async (req) => {
      rateLimit(req, 'link', 10, 900000);
      const { account } = authenticate(req);
      const profile = await provePlayer(await readJson(req), 'link', account.id);
      if (account.mc_uuid && account.mc_uuid !== profile.uuid) {
        throw new HttpError(409, `Ce compte Aéloria est déjà lié au joueur ${account.mc_name}.`);
      }
      const owner = st.accountByUuid.get(profile.uuid);
      if (owner && owner.id !== account.id) throw new HttpError(409, 'Ce compte Minecraft est déjà lié à un autre compte Aéloria.');
      try {
        if (!st.linkMinecraft.run(profile.uuid, profile.name, account.linked_at || now(), account.id, profile.uuid).changes) throw new Error();
      } catch {
        throw new HttpError(409, 'Ce compte Minecraft est déjà lié à un autre compte Aéloria.');
      }
      return { account: publicAccount(st.accountById.get(account.id)) };
    },

    // Mot de passe oublié : prouver la possession du compte Minecraft lié (via le launcher), sans e-mail.
    'POST /api/auth/minecraft-challenge': async (req) => {
      rateLimit(req, 'challenge', 20, 900000);
      return newChallenge(null, 'reset');
    },

    'POST /api/auth/reset-with-minecraft': async (req) => {
      rateLimit(req, 'reset', 5, 900000);
      const body = await readJson(req);
      checkPassword(body.newPassword);
      const profile = await provePlayer(body, 'reset');
      const account = st.accountByUuid.get(profile.uuid);
      if (!account) throw new HttpError(404, `Aucun compte Aéloria n’est lié au joueur ${profile.name}.`);
      st.setPassword.run(await security.hashPassword(body.newPassword), account.id);
      st.deleteSessions.run(account.id);
      return { token: openSession(account, body.client), account: publicAccount(account) };
    },

    'GET /api/shop/packs': async () => ({
      open: Boolean(cfg.stripe && cfg.bridge),
      test: cfg.stripe ? !cfg.stripe.live : true,
      currency: cfg.shop.currency,
      packs: cfg.shop.packs,
    }),

    'POST /api/shop/checkout': async (req) => {
      rateLimit(req, 'checkout', 10, 600000);
      const { account } = authenticate(req);
      if (!cfg.stripe || !cfg.bridge) throw new HttpError(503, 'La boutique n’est pas encore ouverte.');
      const body = await readJson(req);
      const pack = cfg.shop.packs.find((p) => p.id === body.pack);
      if (!pack) throw new HttpError(400, 'Pack inconnu.');
      if (!account.mc_uuid) throw new HttpError(409, 'Lie d’abord ton compte Minecraft : connecte-toi une fois dans le launcher.');
      if (body.waiver !== true) throw new HttpError(400, 'Confirme la livraison immédiate des crédits pour continuer.');
      if (st.recentOrders.get(account.id, now() - 3600000).n >= 10) throw new HttpError(429, 'Trop de commandes en une heure.');
      const orderId = security.randomId('cmd_', 15);
      st.insertOrder.run(orderId, account.id, account.mc_uuid, pack.id, pack.credits, pack.price, cfg.shop.currency, cfg.stripe.live ? 1 : 0, now());
      let session;
      try {
        session = await createCheckoutSession(cfg.stripe, fetchImpl, {
          mode: 'payment',
          locale: 'fr',
          client_reference_id: orderId,
          customer_email: account.email,
          success_url: `${cfg.siteUrl}/boutique.html?commande=${orderId}`,
          cancel_url: `${cfg.siteUrl}/boutique.html?annule=1`,
          metadata: { order: orderId, player: account.mc_uuid },
          payment_intent_data: { metadata: { order: orderId } },
          line_items: [{
            quantity: 1,
            price_data: {
              currency: cfg.shop.currency,
              unit_amount: pack.price,
              product_data: { name: `${pack.name} : ${pack.credits} crédits Aéloria`, description: `Pour ${account.mc_name}` },
            },
          }],
      }, orderId);
      } catch (error) {
        st.markStatus.run('failed', 'Stripe indisponible', orderId, 'pending');
        throw error;
      }
      st.setOrderSession.run(session.id, orderId);
      return { url: session.url, order: orderId };
    },

    'GET /api/shop/orders': async (req) => {
      const { account } = authenticate(req);
      return { orders: st.ordersOf.all(account.id, 20).map(publicOrder) };
    },

    'POST /api/stripe/webhook': stripeWebhook,
  };

  async function handle(req, res) {
    const origin = req.headers.origin;
    const headers = {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      'X-Content-Type-Options': 'nosniff',
      'Referrer-Policy': 'no-referrer',
      Vary: 'Origin',
    };
    if (origin && cfg.origins.has(origin)) {
      headers['Access-Control-Allow-Origin'] = origin;
      headers['Access-Control-Allow-Headers'] = 'Authorization, Content-Type';
      headers['Access-Control-Allow-Methods'] = 'GET, POST, OPTIONS';
      headers['Access-Control-Max-Age'] = '600';
    }
    let status = 200;
    let body;
    try {
      const url = new URL(req.url, 'http://api.local');
      if (req.method === 'OPTIONS') {
        res.writeHead(origin && cfg.origins.has(origin) ? 204 : 403, headers).end();
        return;
      }
      const route = routes[`${req.method} ${url.pathname.replace(/\/+$/, '')}`];
      if (!route) throw new HttpError(404, 'Route inconnue.');
      const result = await route(req, url);
      if (result && result.status && result.body) ({ status, body } = result);
      else body = result;
    } catch (error) {
      status = error instanceof HttpError ? error.status : 500;
      if (status === 500) log.error('[api]', error);
      body = { error: status === 500 ? 'Erreur interne, réessaie plus tard.' : error.message };
    }
    res.writeHead(status, headers).end(JSON.stringify(body));
  }

  return { handle, deliverDue, store, close: () => store.close() };
}

module.exports = { createApp };
