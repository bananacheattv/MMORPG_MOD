/**
 * Aéloria · site officiel — script.js
 * JavaScript vanilla (ES2020), sans framework ni dépendance.
 *
 * Les particules du hero sont en CSS pur (voir style.css, « Braises »).
 * Tout ce qui se modifie au quotidien est dans CONFIG, juste en dessous :
 * adresse du serveur, version, liens, actualités, staff, réseaux, mentions légales.
 * Le HTML contient déjà des valeurs par défaut identiques (SEO, affichage sans JS) ;
 * au chargement, ce script les remplace par celles de CONFIG.
 */
'use strict';

/* =========================================================================
   CONFIG — seul bloc à éditer
   ========================================================================= */
const CONFIG = {
  slogan: 'Le Seuil s’ouvre. Écris ta légende.',

  server: {
    // Adresse publique (ex. « play.aeloria.fr »). Vide = connexion uniquement via le launcher :
    // le compteur affiche « Connexion via le launcher » et le bouton « Copier l’IP » reste masqué.
    address: '',
    // Texte affiché quand l’adresse est vide.
    launcherOnlyLabel: 'Connexion via le launcher',
    // Rafraîchissement du compteur de joueurs (secondes). L’API mcsrvstat.us met en cache ~1 min.
    refreshSeconds: 60,
  },

  game: {
    minecraftVersion: '26.3',
    loader: 'NeoForge',
  },

  launcher: {
    url: 'https://github.com/bananacheattv/gfrpg-launcher/releases/latest',
    platform: 'Windows 10 / 11',
  },

  links: {
    discord: 'https://discord.gg/aeloria-mmorpg',
  },

  // Réseaux affichés dans le pied de page (Discord est déjà présent). Ex. :
  // { label: 'YouTube', url: 'https://youtube.com/@aeloria' }
  socials: [],

  // Actualités : les 3 plus récentes (par date) sont affichées.
  // type : 'maj' | 'nouveaute' | 'evenement' | 'correctif' (couleur de l’étiquette).
  news: [
    {
      date: '2026-11-16',
      type: 'maj',
      title: 'Un monde plus fluide',
      text: 'Monstres éloignés mis en veille, sauvegardes en arrière-plan, guildes et classements en cache : le serveur respire, même aux heures de pointe.',
    },
    {
      date: '2026-11-14',
      type: 'nouveaute',
      title: 'Flammèche est né',
      text: 'Ton guide du tutoriel éclot désormais d’un œuf à ton arrivée sur le Seuil. Et un grand portail ouvre le monde ouvert à tous les Appelés.',
    },
    {
      date: '2026-11-12',
      type: 'nouveaute',
      title: 'Les quêtes du lore arrivent',
      text: '250 quêtes principales sur 4 actes et 20 régions, avec les Éveils de Flammèche à chaque évolution de classe.',
    },
    {
      date: '2026-11-06',
      type: 'maj',
      title: 'La grande refonte 3D',
      text: '49 monstres, 5 boss, 22 montures dont deux vrais dragons : tout le bestiaire a été remodelé et réanimé.',
    },
  ],
  newsCount: 3,

  // Staff : « skin » = pseudo Minecraft (tête via mc-heads.net). Vide = emblème d’Aéloria.
  staff: [
    { name: 'Copassist', role: 'Fondateur', bio: 'Crée le monde d’Aéloria, ses quêtes et ses systèmes.', skin: '' },
  ],

  legal: {
    host: 'Netlify, Inc., San Francisco, Californie, États-Unis (netlify.com).',
  },
};

/* =========================================================================
   Utilitaires
   ========================================================================= */

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));

/** Lit une valeur de CONFIG à partir d’un chemin « a.b.c ». */
const readConfig = (path) => path.split('.').reduce((value, key) => (value == null ? undefined : value[key]), CONFIG);

const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
const finePointer = window.matchMedia('(hover: hover) and (pointer: fine)');

const dateFormatter = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
const numberFormatter = new Intl.NumberFormat('fr-FR');

/* =========================================================================
   1. Liaison CONFIG → DOM (textes et liens)
   ========================================================================= */
function applyConfig() {
  $$('[data-config-text]').forEach((el) => {
    const value = readConfig(el.dataset.configText);
    if (typeof value === 'string' && value) el.textContent = value;
  });

  $$('[data-config-href]').forEach((el) => {
    const value = readConfig(el.dataset.configHref);
    if (typeof value === 'string' && value) el.href = value;
  });

  $$('[data-year]').forEach((el) => {
    el.textContent = String(new Date().getFullYear());
  });

  // Réseaux supplémentaires dans le pied de page
  const socialList = $('[data-social-list]');
  if (socialList) {
    CONFIG.socials.forEach(({ label, url }) => {
      if (!label || !url) return;
      const item = document.createElement('li');
      const link = document.createElement('a');
      link.className = 'site-footer__link';
      link.href = url;
      link.target = '_blank';
      link.rel = 'noopener';
      link.textContent = label;
      const hint = document.createElement('span');
      hint.className = 'visually-hidden';
      hint.textContent = ' (nouvel onglet)';
      link.append(hint);
      item.append(link);
      socialList.append(item);
    });
  }
}

/* =========================================================================
   2. Toast (notifications courtes, annoncées aux lecteurs d’écran)
   ========================================================================= */
const toast = (() => {
  const el = $('[data-toast]');
  const text = $('[data-toast-text]');
  let timer = 0;

  return function show(message, { error = false, duration = 3800 } = {}) {
    if (!el || !text) return;
    window.clearTimeout(timer);
    el.classList.toggle('toast--error', error);
    text.textContent = message;
    el.classList.add('toast--visible');
    timer = window.setTimeout(() => el.classList.remove('toast--visible'), duration);
  };
})();

/* =========================================================================
   3. Navigation : en-tête au défilement, menu mobile, lien actif
   ========================================================================= */
function initNavigation() {
  const header = $('[data-header]');
  const toggle = $('[data-nav-toggle]');
  const toggleLabel = $('[data-nav-toggle-label]');
  const nav = $('[data-nav]');
  const main = $('main');
  const footer = $('.site-footer');
  const desktop = window.matchMedia('(min-width: 66rem)');
  if (!header || !toggle || !nav) return;

  const setOpen = (open) => {
    toggle.setAttribute('aria-expanded', String(open));
    toggleLabel.textContent = open ? 'Fermer le menu' : 'Ouvrir le menu';
    nav.classList.toggle('site-nav--open', open);
    header.classList.toggle('site-header--menu-open', open);
    document.body.classList.toggle('is-menu-open', open);
    // Le reste de la page devient inerte pendant que le menu plein écran est ouvert.
    [main, footer].forEach((el) => { if (el) el.inert = open; });
    if (open) $('a', nav)?.focus();
  };

  toggle.addEventListener('click', () => setOpen(toggle.getAttribute('aria-expanded') !== 'true'));

  nav.addEventListener('click', (event) => {
    if (event.target.closest('a') && !desktop.matches) setOpen(false);
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && toggle.getAttribute('aria-expanded') === 'true') {
      setOpen(false);
      toggle.focus();
    }
  });

  desktop.addEventListener('change', () => setOpen(false));

  // En-tête opaque dès qu’on quitte le haut de page (sentinelle, sans écouteur de scroll).
  const sentinel = document.createElement('div');
  sentinel.setAttribute('aria-hidden', 'true');
  sentinel.className = 'scroll-sentinel';
  document.body.prepend(sentinel);
  new IntersectionObserver(([entry]) => {
    header.classList.toggle('site-header--scrolled', !entry.isIntersecting);
  }, { rootMargin: '32px 0px 0px 0px' }).observe(sentinel);

  // Lien de navigation actif selon la section visible.
  // Toutes les sections sont observées : sur le hero (sans lien), aucun lien n’est actif.
  const links = new Map($$('.site-nav__link').map((link) => [link.hash.slice(1), link]));
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      links.forEach((link) => link.removeAttribute('aria-current'));
      links.get(entry.target.id)?.setAttribute('aria-current', 'true');
    });
  }, { rootMargin: '-45% 0px -50% 0px' });
  $$('main section[id]').forEach((section) => observer.observe(section));
}

/* =========================================================================
   4. Statut serveur (mcsrvstat.us) + copie de l’IP
   ========================================================================= */
function initServerStatus() {
  const root = $('[data-server-status]');
  if (!root) return;
  const text = $('[data-server-text]', root);
  const meta = $('[data-server-meta]', root);
  const copyButton = $('[data-action="copy-ip"]', root);
  const address = CONFIG.server.address.trim();
  let lastFetch = 0;
  let timer = 0;

  /** Met à jour l’état visuel et le texte (annoncé via role="status"). */
  const render = (state, players = 0, max = 0) => {
    root.dataset.state = state;
    text.replaceChildren();
    switch (state) {
      case 'online': {
        const count = document.createElement('strong');
        count.textContent = numberFormatter.format(players);
        text.append(count, players > 1 ? ' joueurs en ligne' : ' joueur en ligne');
        root.title = max ? `${players} / ${max} places` : '';
        break;
      }
      case 'offline':
        text.textContent = 'Serveur hors ligne';
        break;
      case 'error':
        text.textContent = 'Statut indisponible';
        break;
      case 'loading':
        text.textContent = 'Recherche du serveur…';
        break;
      default:
        text.textContent = CONFIG.server.launcherOnlyLabel;
    }
  };

  // Pas d’adresse publique : on reste sur « Connexion via le launcher », aucune requête réseau.
  if (!address) {
    render('launcher');
    return;
  }

  meta.hidden = true;
  copyButton.hidden = false;
  copyButton.setAttribute('aria-label', `Copier l’adresse du serveur ${address}`);

  copyButton.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(address);
      toast(`Adresse copiée : ${address}`);
    } catch {
      // Presse-papier refusé (navigateur ancien ou permission) : on affiche l’adresse à recopier.
      toast(`Copie impossible. Adresse : ${address}`, { error: true, duration: 7000 });
    }
  });

  const refresh = async () => {
    lastFetch = Date.now();
    const controller = new AbortController();
    const abort = window.setTimeout(() => controller.abort(), 8000);
    try {
      const response = await fetch(`https://api.mcsrvstat.us/3/${encodeURIComponent(address)}`, { signal: controller.signal });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      if (data.online) render('online', Number(data.players?.online) || 0, Number(data.players?.max) || 0);
      else render('offline');
    } catch {
      render('error');
    } finally {
      window.clearTimeout(abort);
    }
  };

  const schedule = () => {
    window.clearInterval(timer);
    timer = window.setInterval(() => { if (!document.hidden) refresh(); }, CONFIG.server.refreshSeconds * 1000);
  };

  // Retour sur l’onglet : rafraîchit si la donnée a vieilli.
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden && Date.now() - lastFetch > CONFIG.server.refreshSeconds * 1000) refresh();
  });

  render('loading');
  refresh();
  schedule();
}

/* =========================================================================
   5. Boutons « Télécharger le launcher »
   ========================================================================= */
function initDownloadLinks() {
  $$('[data-action="download"]').forEach((link) => {
    link.addEventListener('click', () => {
      toast('Téléchargement ouvert dans un nouvel onglet. Suis ensuite les 3 étapes !', { duration: 5000 });
    });
  });
}

/* =========================================================================
   6. Cartes de classes : tap / clic / clavier
   ========================================================================= */
function initClassCards() {
  $$('[data-class-card]').forEach((card) => {
    const button = $('[data-class-toggle]', card);
    const label = $('[data-class-toggle-label]', card);
    if (!button) return;

    const setOpen = (open) => {
      button.setAttribute('aria-expanded', String(open));
      card.classList.toggle('class-card--open', open);
      label.textContent = open ? 'Masquer' : 'Voir le rôle';
    };
    const isOpen = () => button.getAttribute('aria-expanded') === 'true';

    button.addEventListener('click', (event) => {
      event.stopPropagation();
      setOpen(!isOpen());
    });

    // Sur écran tactile, toucher n’importe où sur la carte la retourne.
    card.addEventListener('click', () => {
      if (!finePointer.matches) setOpen(!isOpen());
    });

    card.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && isOpen()) {
        setOpen(false);
        button.focus();
      }
    });
  });
}

/* =========================================================================
   7. Actualités et staff (générés depuis CONFIG, via <template>)
   ========================================================================= */
function renderNews() {
  const list = $('[data-news-list]');
  const template = $('#news-template');
  if (!list || !template) return;

  const labels = { maj: 'Mise à jour', nouveaute: 'Nouveauté', evenement: 'Événement', correctif: 'Correctif' };
  const items = [...CONFIG.news]
    .sort((a, b) => b.date.localeCompare(a.date))
    .slice(0, CONFIG.newsCount);

  list.replaceChildren(...items.map((item) => {
    const node = template.content.firstElementChild.cloneNode(true);
    const card = $('.news-card', node);
    const type = labels[item.type] ? item.type : 'maj';
    card.classList.add(`news-card--${type}`);
    $('.news-card__tag', node).textContent = labels[type];
    const time = $('.news-card__date', node);
    time.dateTime = item.date;
    time.textContent = dateFormatter.format(new Date(`${item.date}T12:00:00`));
    $('.news-card__title', node).textContent = item.title;
    $('.news-card__text', node).textContent = item.text;
    return node;
  }));
}

function renderStaff() {
  const list = $('[data-staff-list]');
  const template = $('#staff-template');
  const recruit = $('[data-staff-recruit]');
  if (!list || !template) return;

  const emblem = 'assets/img/embleme-256.webp';

  CONFIG.staff.forEach((member) => {
    const node = template.content.firstElementChild.cloneNode(true);
    const image = $('.staff-card__image', node);
    const skin = (member.skin || '').trim();

    if (skin) {
      image.src = `https://mc-heads.net/avatar/${encodeURIComponent(skin)}/128`;
      image.alt = `Skin Minecraft de ${member.name}`;
      // Pseudo introuvable ou service indisponible : on retombe sur l’emblème.
      image.addEventListener('error', () => {
        image.src = emblem;
        image.alt = '';
        image.classList.add('staff-card__image--emblem');
      }, { once: true });
    } else {
      image.src = emblem;
      image.alt = '';
      image.classList.add('staff-card__image--emblem');
    }

    $('.staff-card__name', node).textContent = member.name;
    $('.staff-card__role', node).textContent = member.role;
    $('.staff-card__bio', node).textContent = member.bio || '';
    list.insertBefore(node, recruit);
  });
}

/* =========================================================================
   8. Mentions légales (<dialog> natif)
   ========================================================================= */
function initDialogs() {
  $$('[data-dialog-open]').forEach((trigger) => {
    const dialog = document.getElementById(trigger.dataset.dialogOpen);
    if (!dialog || typeof dialog.showModal !== 'function') return;
    trigger.addEventListener('click', () => dialog.showModal());
  });

  $$('[data-dialog]').forEach((dialog) => {
    $('[data-dialog-close]', dialog)?.addEventListener('click', () => dialog.close());
    // Clic sur le fond (hors de la boîte) : fermeture.
    dialog.addEventListener('click', (event) => {
      if (event.target === dialog) dialog.close();
    });
  });
}

/* =========================================================================
   9. Parallaxe légère du hero (transform uniquement, désactivée si mouvement réduit)
   ========================================================================= */
function initParallax() {
  const hero = $('[data-hero]');
  const media = $('[data-parallax]');
  const content = $('[data-hero-content]');
  if (!hero || !media) return;

  let visible = true;
  let ticking = false;

  const update = () => {
    ticking = false;
    if (reducedMotion.matches || !visible) return;
    const y = window.scrollY;
    const height = hero.offsetHeight || 1;
    media.style.transform = `translate3d(0, ${(y * 0.3).toFixed(1)}px, 0)`;
    if (content) {
      content.style.transform = `translate3d(0, ${(y * 0.12).toFixed(1)}px, 0)`;
      content.style.opacity = String(Math.max(0, 1 - y / (height * 0.75)).toFixed(3));
    }
  };

  const reset = () => {
    media.style.transform = '';
    if (content) {
      content.style.transform = '';
      content.style.opacity = '';
    }
  };

  new IntersectionObserver(([entry]) => { visible = entry.isIntersecting; }).observe(hero);

  window.addEventListener('scroll', () => {
    if (!ticking) {
      ticking = true;
      window.requestAnimationFrame(update);
    }
  }, { passive: true });

  reducedMotion.addEventListener('change', () => (reducedMotion.matches ? reset() : update()));
}

/* =========================================================================
   Démarrage
   ========================================================================= */
function init() {
  applyConfig();
  initNavigation();
  initServerStatus();
  initDownloadLinks();
  initClassCards();
  renderNews();
  renderStaff();
  initDialogs();
  initParallax();
}

init();
