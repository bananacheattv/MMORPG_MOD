/**
 * Aéloria · espace joueur — espace.js
 * Pages compte.html (connexion, inscription, tableau de bord) et boutique.html (packs de crédits, paiement Stripe).
 * Chargé après script.js : réutilise CONFIG, $, $$ et toast. Back-end : dossier backend/ du dépôt.
 */
'use strict';

const portal = (() => {
  const STORAGE_KEY = 'aeloria.session';
  const base = CONFIG.api.url === '/' ? '' : CONFIG.api.url.replace(/\/+$/, '');

  const session = {
    get() {
      try { return window.localStorage.getItem(STORAGE_KEY); } catch { return null; }
    },
    set(token) {
      try {
        if (token) window.localStorage.setItem(STORAGE_KEY, token);
        else window.localStorage.removeItem(STORAGE_KEY);
      } catch { /* stockage indisponible (navigation privée stricte) : session limitée à la page */ }
    },
  };

  /** Appel JSON au back-end ; lève une Error au message prêt à afficher. */
  async function api(path, { method = 'GET', body } = {}) {
    const headers = {};
    const token = session.get();
    if (body) headers['Content-Type'] = 'application/json';
    if (token) headers.Authorization = `Bearer ${token}`;
    let response;
    try {
      response = await fetch(`${base}/api${path}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
        signal: AbortSignal.timeout(15000),
      });
    } catch {
      throw Object.assign(new Error('Le serveur des comptes ne répond pas. Réessaie dans un instant.'), { status: 0 });
    }
    const data = await response.json().catch(() => ({}));
    if (response.status === 401 && token) session.set(null);
    if (!response.ok) throw Object.assign(new Error(data.error || `Erreur ${response.status}.`), { status: response.status });
    return data;
  }

  const money = (cents, currency) => new Intl.NumberFormat('fr-FR', { style: 'currency', currency: currency.toUpperCase() }).format(cents / 100);
  const STATUS = {
    pending: 'En attente',
    paid: 'Payée, livraison…',
    delivered: 'Livrée',
    expired: 'Abandonnée',
    failed: 'Échouée',
    refunded: 'Remboursée',
  };

  /** Bouton occupé pendant une requête. */
  async function busy(button, work) {
    button.disabled = true;
    try { return await work(); } finally { button.disabled = false; }
  }

  return { api, session, money, STATUS, busy, enabled: Boolean(CONFIG.api.url) };
})();

/* =========================================================================
   Page Compte
   ========================================================================= */
function initAccountPage() {
  const auth = $('[data-auth]');
  const dashboard = $('[data-dashboard]');
  const params = new URLSearchParams(window.location.search);

  const showAuth = () => {
    dashboard.hidden = true;
    auth.hidden = false;
    $('[data-portal-title]').textContent = 'Mon compte Aéloria';
    $('[data-portal-lead]').textContent = 'Un seul compte pour le site, le launcher et la boutique.';
  };

  function renderDashboard({ account, credits, orders }) {
    auth.hidden = true;
    dashboard.hidden = false;
    $('[data-portal-title]').textContent = `Bonjour, ${account.username}`;
    $('[data-portal-lead]').textContent = `Compte créé le ${dateFormatter.format(new Date(account.createdAt))} · ${account.email}`;

    const head = $('[data-player-head]');
    if (account.minecraft) {
      $('[data-player-name]').textContent = account.minecraft.name;
      $('[data-player-text]').textContent = 'Lié au launcher : tes achats arrivent sur ce personnage.';
      head.src = `https://mc-heads.net/avatar/${account.minecraft.uuid.replace(/-/g, '')}/112`;
      head.onerror = () => { head.hidden = true; };
      head.hidden = false;
    } else {
      $('[data-player-name]').textContent = 'Pas encore lié';
      head.hidden = true;
    }

    $('[data-credits]').textContent = typeof credits === 'number' ? numberFormatter.format(credits) : '—';
    $('[data-credits-text]').textContent = typeof credits === 'number'
      ? 'Ton solde en jeu, utilisable avec /boutique.'
      : account.minecraft ? 'Solde indisponible : le serveur est peut-être en maintenance.' : 'Lie ton personnage pour voir ton solde.';

    const list = $('[data-orders]');
    const template = $('#order-template');
    list.replaceChildren(...orders.map((order) => {
      const item = template.content.firstElementChild.cloneNode(true);
      $('[data-order-what]', item).textContent = `${numberFormatter.format(order.credits)} crédits`;
      $('[data-order-meta]', item).textContent = `${dateFormatter.format(new Date(order.createdAt))} · ${portal.money(order.amount, order.currency)}${order.test ? ' · test' : ''} · ${order.id}`;
      const chip = $('[data-order-status]', item);
      chip.textContent = portal.STATUS[order.status] || order.status;
      chip.classList.add(`chip--${order.status}`);
      return item;
    }));
    $('[data-orders-empty]').hidden = orders.length > 0;
  }

  async function load() {
    if (!portal.session.get()) return showAuth();
    try {
      renderDashboard(await portal.api('/account'));
    } catch (error) {
      showAuth();
      if (error.status !== 401) toast(error.message, { error: true, duration: 6000 });
    }
  }

  // Onglets Connexion / Créer un compte (flèches gauche/droite, cf. motif ARIA « tabs »).
  const tabs = $$('[data-auth-tab]');
  const selectTab = (name, focus = false) => {
    tabs.forEach((tab) => {
      const selected = tab.dataset.authTab === name;
      tab.setAttribute('aria-selected', String(selected));
      tab.tabIndex = selected ? 0 : -1;
      document.getElementById(tab.getAttribute('aria-controls')).hidden = !selected;
      if (selected && focus) tab.focus();
    });
  };
  tabs.forEach((tab, index) => {
    tab.addEventListener('click', () => selectTab(tab.dataset.authTab));
    tab.addEventListener('keydown', (event) => {
      if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;
      const next = tabs[(index + (event.key === 'ArrowRight' ? 1 : tabs.length - 1)) % tabs.length];
      selectTab(next.dataset.authTab, true);
    });
  });
  if (window.location.hash === '#inscription') selectTab('register');

  /** Valide les champs natifs, affiche la première erreur et la met en évidence. */
  const validate = (form) => {
    const error = $('[data-form-error]', form);
    error.textContent = '';
    for (const input of $$('input', form)) {
      input.removeAttribute('aria-invalid');
      if (!input.checkValidity()) {
        input.setAttribute('aria-invalid', 'true');
        const label = input.type === 'checkbox' ? 'Coche la case pour continuer.' : `${$(`label[for="${input.id}"]`, form)?.textContent || 'Champ'} : ${input.validationMessage}`;
        error.textContent = label;
        input.focus();
        return false;
      }
    }
    return true;
  };

  const afterLogin = async (data) => {
    portal.session.set(data.token);
    if (params.get('suite') === 'boutique') {
      window.location.assign('boutique.html');
      return;
    }
    await load();
    $('#contenu').focus();
  };

  const submit = (name, work) => {
    const form = $(`[data-form="${name}"]`);
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      if (!validate(form)) return;
      const values = Object.fromEntries(new FormData(form));
      await portal.busy($('[type="submit"]', form), async () => {
        try {
          await work(values, form);
        } catch (error) {
          $('[data-form-error]', form).textContent = error.message;
        }
      });
    });
  };

  submit('login', async (values) => {
    await afterLogin(await portal.api('/auth/login', { method: 'POST', body: { login: values.login, password: values.password, client: 'web' } }));
  });
  submit('register', async (values) => {
    const data = await portal.api('/auth/register', {
      method: 'POST',
      body: { username: values.username, email: values.email, password: values.password, acceptRules: values.acceptRules === 'on', client: 'web' },
    });
    toast('Compte créé ! Connecte-toi maintenant dans le launcher.', { duration: 6000 });
    await afterLogin(data);
  });
  submit('password', async (values, form) => {
    const data = await portal.api('/account/password', { method: 'POST', body: values });
    portal.session.set(data.token);
    form.reset();
    form.closest('details').open = false;
    toast('Mot de passe modifié. Reconnecte-toi dans le launcher.');
  });

  $('[data-action="logout"]').addEventListener('click', async () => {
    await portal.api('/auth/logout', { method: 'POST' }).catch(() => {});
    portal.session.set(null);
    showAuth();
    toast('Tu es déconnecté.');
  });

  load();
}

/* =========================================================================
   Page Boutique
   ========================================================================= */
function initShopPage() {
  const params = new URLSearchParams(window.location.search);
  const accountNotice = $('[data-shop-account]');
  const dialog = $('[data-checkout]');
  const waiver = $('[data-checkout-waiver]');
  const errorText = $('[data-checkout-error]');
  let shop = null;
  let account = null;
  let selected = null;

  if (CONFIG.legal.seller) {
    const seller = $('[data-seller]');
    seller.textContent = `Vendeur : ${CONFIG.legal.seller}.`;
    seller.hidden = false;
  }

  const showAccountNotice = (text, linkLabel, href) => {
    accountNotice.replaceChildren(text);
    if (linkLabel) {
      const link = document.createElement('a');
      link.className = 'link';
      link.href = href;
      link.textContent = linkLabel;
      accountNotice.append(' ', link);
    }
    accountNotice.hidden = false;
  };

  function renderPacks() {
    const list = $('[data-packs]');
    const template = $('#pack-template');
    list.replaceChildren(...shop.packs.map((pack) => {
      const item = template.content.firstElementChild.cloneNode(true);
      item.classList.toggle('pack--highlight', pack.highlight);
      $('[data-pack-name]', item).textContent = pack.name;
      $('[data-pack-credits]', item).textContent = numberFormatter.format(pack.credits);
      $('[data-pack-price]', item).textContent = portal.money(pack.price, shop.currency);
      const bonus = $('[data-pack-bonus]', item);
      if (pack.bonus) {
        bonus.textContent = `Bonus ${pack.bonus}`;
        bonus.hidden = false;
      }
      const button = $('[data-pack-buy]', item);
      button.disabled = !shop.open;
      if (!shop.open) button.textContent = 'Bientôt';
      button.setAttribute('aria-label', `Acheter le ${pack.name}, ${pack.credits} crédits, ${portal.money(pack.price, shop.currency)}`);
      button.addEventListener('click', () => buy(pack));
      return item;
    }));
  }

  function buy(pack) {
    if (!account) {
      window.location.assign('compte.html?suite=boutique');
      return;
    }
    if (!account.minecraft) {
      toast('Lie d’abord ton personnage : connecte-toi une fois dans le launcher.', { error: true, duration: 6000 });
      return;
    }
    selected = pack;
    $('[data-checkout-pack]').textContent = `${pack.name} : ${numberFormatter.format(pack.credits)} crédits`;
    $('[data-checkout-price]').textContent = portal.money(pack.price, shop.currency);
    $('[data-checkout-player]').textContent = account.minecraft.name;
    waiver.checked = false;
    errorText.textContent = '';
    dialog.showModal();
  }

  $('[data-checkout-pay]').addEventListener('click', (event) => portal.busy(event.currentTarget, async () => {
    errorText.textContent = '';
    if (!waiver.checked) {
      errorText.textContent = 'Coche la case pour confirmer la livraison immédiate.';
      waiver.focus();
      return;
    }
    try {
      const { url } = await portal.api('/shop/checkout', { method: 'POST', body: { pack: selected.id, waiver: true } });
      window.location.assign(url);
    } catch (error) {
      errorText.textContent = error.message;
    }
  }));

  /** Retour de Stripe : suit la commande jusqu’à la livraison (≈ 1 minute au plus). */
  async function followOrder(id) {
    const box = $('[data-order-status]');
    box.hidden = false;
    box.className = 'notice';
    box.textContent = 'Paiement reçu, confirmation en cours…';
    window.history.replaceState(null, '', 'boutique.html');
    for (let attempt = 0; attempt < 20; attempt += 1) {
      try {
        const order = (await portal.api('/shop/orders')).orders.find((o) => o.id === id);
        if (order?.status === 'delivered') {
          box.className = 'notice notice--success';
          box.textContent = `${numberFormatter.format(order.credits)} crédits livrés sur ${account?.minecraft?.name || 'ton personnage'}. Bon jeu !`;
          return;
        }
        if (order && ['failed', 'expired', 'refunded'].includes(order.status)) break;
      } catch { /* on réessaie */ }
      await new Promise((resolve) => { window.setTimeout(resolve, 3000); });
    }
    box.className = 'notice notice--error';
    box.textContent = `Commande ${id} pas encore livrée. Elle le sera dès que le serveur répond ; sinon, contacte le staff sur Discord avec ce numéro.`;
  }

  async function load() {
    try {
      shop = await portal.api('/shop/packs');
    } catch (error) {
      $('[data-portal-off]').hidden = false;
      toast(error.message, { error: true, duration: 6000 });
      return;
    }
    $('[data-shop-test]').hidden = !(shop.open && shop.test);
    if (!shop.open) $('[data-portal-off]').hidden = false;
    if (portal.session.get()) {
      try {
        account = (await portal.api('/account')).account;
      } catch { account = null; }
    }
    renderPacks();
    if (!shop.open) return;
    if (!account) showAccountNotice('Connecte-toi pour acheter :', 'se connecter ou créer un compte', 'compte.html?suite=boutique');
    else if (!account.minecraft) showAccountNotice('Ton personnage n’est pas encore lié : ouvre le launcher et connecte-toi avec ton compte Aéloria.');
    else showAccountNotice(`Connecté : ${account.username} · crédits livrés sur ${account.minecraft.name}.`, 'Mon compte', 'compte.html');
    if (params.get('annule')) {
      toast('Paiement annulé : aucun débit.');
      window.history.replaceState(null, '', 'boutique.html');
    }
    const order = params.get('commande');
    if (order && /^cmd_[A-Za-z0-9_-]{10,40}$/.test(order) && account) followOrder(order);
  }

  load();
}

/* =========================================================================
   Démarrage
   ========================================================================= */
if (!portal.enabled) {
  $('[data-portal-off]').hidden = false;
} else if (document.body.dataset.portal === 'compte') {
  initAccountPage();
} else if (document.body.dataset.portal === 'boutique') {
  initShopPage();
}
