'use strict';
const $ = (s, r = document) => r.querySelector(s);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const app = $('#app');

const ICONS = {
  dash: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/></svg>',
  console: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M7 9l3 3-3 3M13 15h4"/></svg>',
  players: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c.6-3.6 3.2-5.5 6.5-5.5s5.9 1.9 6.5 5.5M16 4.8a3.5 3.5 0 010 6.4M18 14.8c2 .6 3.2 2.3 3.5 5.2"/></svg>',
  files: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 5a2 2 0 012-2h5l2 3h5a2 2 0 012 2v10a2 2 0 01-2 2H6a2 2 0 01-2-2z"/></svg>',
  set: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M19 5l-2 2M7 17l-2 2"/></svg>',
  shield: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3l8 3v6c0 4.5-3.2 8-8 9-4.8-1-8-4.5-8-9V6z"/><path d="M9 12l2 2 4-4"/></svg>',
  mod: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3v12M7 10l5 5 5-5M4 20h16"/></svg>',
  logo: '<svg viewBox="0 0 32 32"><path d="M16 2l12 8v12l-12 8-12-8V10z" fill="none" stroke="#d4a64a" stroke-width="2"/><path d="M16 8l6 4v8l-6 4-6-4v-8z" fill="#d4a64a"/></svg>',
};

/* ---------------------------------------------------------------- helpers */
async function api(path, opts = {}) {
  const o = { method: opts.method || 'GET', headers: {} };
  if (opts.body !== undefined) { o.headers['Content-Type'] = 'application/json'; o.body = JSON.stringify(opts.body); }
  else if (o.method !== 'GET') o.headers['Content-Type'] = 'application/json';
  const r = await fetch(path, o);
  const data = await r.json().catch(() => ({}));
  if (r.status === 401 && !path.startsWith('/api/login')) { location.reload(); throw new Error('Session expirée'); }
  if (!r.ok) throw new Error(data.error || 'Erreur ' + r.status);
  return data;
}
function toast(msg, err) {
  const t = document.createElement('div'); t.className = 'toast' + (err ? ' err' : ''); t.textContent = msg;
  $('#toasts').append(t); setTimeout(() => t.remove(), err ? 6000 : 3000);
}
const act = async (fn, okMsg) => { try { await fn(); if (okMsg) toast(okMsg); } catch (e) { toast(e.message, true); } };
const fmtBytes = (b) => b > 1073741824 ? (b / 1073741824).toFixed(1) + ' Go' : Math.round(b / 1048576) + ' Mo';
const fmtDur = (ms) => { const s = Math.floor(ms / 1000), d = Math.floor(s / 86400), h = Math.floor(s % 86400 / 3600), m = Math.floor(s % 3600 / 60); return d ? `${d}j ${h}h` : h ? `${h}h ${m}m` : m ? `${m}m ${s % 60}s` : s + 's'; };
const fmtDate = (t) => t ? new Date(t).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : '—';
const STATUS = { stopped: 'Arrêté', starting: 'Démarrage…', running: 'En ligne', stopping: 'Arrêt…', crashed: 'Planté' };

function modal(title, bodyHtml, onOk, okLabel = 'Valider') {
  const bg = document.createElement('div'); bg.className = 'modal-bg';
  bg.innerHTML = `<div class="modal"><h3>${esc(title)}</h3>${bodyHtml}<div class="row" style="justify-content:flex-end;margin-top:16px"><button class="btn" data-x>Annuler</button><button class="btn pri" data-ok>${esc(okLabel)}</button></div></div>`;
  document.body.append(bg);
  const close = () => bg.remove();
  bg.addEventListener('click', (e) => { if (e.target === bg || e.target.dataset.x !== undefined) close(); });
  $('[data-ok]', bg).addEventListener('click', async () => { try { await onOk(bg); close(); } catch (e) { toast(e.message, true); } });
  const f = $('input,select,textarea', bg); if (f) f.focus();
}

/* ------------------------------------------------------------------ état */
let state = null, page = 'dash', es = null, secUnread = 0;
function updateSecBadge() { const b = $('#secb'); if (!b) return; b.hidden = !secUnread; b.textContent = secUnread > 99 ? '99+' : secUnread; }
let pageHooks = {}; // { status(snap), log(ev) }

function connectStream() {
  es = new EventSource('/api/stream');
  es.addEventListener('status', (e) => { state = JSON.parse(e.data); updateFoot(); pageHooks.status && pageHooks.status(state); });
  es.addEventListener('alert', (e) => {
    const ev = JSON.parse(e.data);
    if (page !== 'sec') { secUnread++; updateSecBadge(); }
    if (ev.sev === 'high') toast(`⚠ ${ev.player || ev.server} — ${ev.detail}`, true);
    pageHooks.alert && pageHooks.alert(ev);
  });
  es.addEventListener('log', (e) => { pageHooks.log && pageHooks.log(JSON.parse(e.data)); });
}
function updateFoot() {
  const f = $('#foot'); if (!f || !state) return;
  const mb = $('#modb'); if (mb) mb.hidden = !(state.mod && state.mod.available);
  const on = state.services.filter((s) => s.status === 'running').length;
  const pl = state.services.reduce((n, s) => n + s.players.length, 0);
  f.innerHTML = `${on}/${state.services.length} services en ligne<br>${pl} joueur${pl > 1 ? 's' : ''} connecté${pl > 1 ? 's' : ''}`;
}

/* ----------------------------------------------------------------- login */
async function boot() {
  const st = await api('/api/state');
  if (!st.authed) return loginView(st.setup);
  state = await api('/api/status'); // les pages ont besoin de l'état avant le premier évènement SSE
  shell();
  connectStream();
  route();
}
function loginView(setup) {
  app.innerHTML = `<div class="login"><form class="box" id="lf">${ICONS.logo.replace('<svg', '<svg width="54" height="54"')}<h1>AELORIA</h1>
    <p>${setup ? 'Première utilisation : choisis un mot de passe admin (8 caractères min.)' : 'Panel d\'administration'}</p>
    <input type="password" name="pw" placeholder="Mot de passe" autocomplete="${setup ? 'new-password' : 'current-password'}" required>
    ${setup ? '<input type="password" name="pw2" placeholder="Confirmer" autocomplete="new-password" required>' : ''}
    <button class="btn pri">${setup ? 'Créer le compte' : 'Entrer'}</button></form></div>`;
  $('#lf').addEventListener('submit', (e) => {
    e.preventDefault(); const f = e.target;
    if (setup && f.pw.value !== f.pw2.value) return toast('Les mots de passe diffèrent', true);
    act(async () => { await api(setup ? '/api/setup' : '/api/login', { method: 'POST', body: { password: f.pw.value } }); location.reload(); });
  });
}

/* ----------------------------------------------------------------- shell */
const PAGES = [['dash', 'Tableau de bord', ICONS.dash], ['console', 'Consoles', ICONS.console], ['players', 'Joueurs', ICONS.players], ['sec', 'Sécurité', ICONS.shield], ['files', 'Fichiers', ICONS.files], ['mod', 'Mise à jour du mod', ICONS.mod], ['set', 'Paramètres', ICONS.set]];
function shell() {
  app.innerHTML = `<div class="layout"><aside class="side"><div class="brand">${ICONS.logo}<div>AELORIA<small>Panel</small></div></div>
    <nav class="nav">${PAGES.map(([id, t, ic]) => `<a data-page="${id}">${ic}${t}${id === 'sec' ? '<span class="badge" id="secb" hidden></span>' : ''}${id === 'mod' ? '<span class="badge" id="modb" hidden>!</span>' : ''}</a>`).join('')}</nav>
    <div class="foot" id="foot"></div></aside><main class="main" id="main"></main></div>`;
  $('.nav').addEventListener('click', (e) => { const a = e.target.closest('a'); if (a) location.hash = a.dataset.page; });
  window.addEventListener('hashchange', route);
}
function route() {
  page = (location.hash.slice(1).split('/')[0]) || 'dash';
  if (!PAGES.some((p) => p[0] === page)) page = 'dash';
  document.querySelectorAll('.nav a').forEach((a) => a.classList.toggle('on', a.dataset.page === page));
  pageHooks = {};
  const m = main(); m.replaceWith(m.cloneNode(false)); // supprime les anciens écouteurs
  ({ dash: dashPage, console: consolePage, players: playersPage, sec: securityPage, files: filesPage, mod: modPage, set: settingsPage })[page]();
  updateFoot();
}
const main = () => $('#main');

/* ------------------------------------------------------------- dashboard */
function dashPage() {
  main().innerHTML = `<div class="top"><h2>Tableau de bord</h2><div class="row">
    <button class="btn pri" data-a="startall">▶ Tout démarrer</button><button class="btn" data-a="stopall">■ Tout arrêter</button></div></div>
    <div id="op"></div><div class="gauges" id="gauges"></div><div class="cards" id="cards"></div>
    <div class="box" style="margin-top:18px"><div class="row" style="justify-content:space-between;margin-bottom:10px"><h3 style="margin:0">Console globale</h3><div class="tabs" id="gf" style="margin:0"></div></div>
      <div class="console" id="gcon" style="height:340px;min-height:0"></div>
      <form class="cmdline" id="gcf"><select id="gt" style="width:200px"></select><input type="text" id="gci" placeholder="Commande (sans /)" autocomplete="off"><button class="btn pri">Envoyer</button></form></div>
    <div class="box"><h3>Annonce globale</h3><div class="cmdline"><input type="text" id="bc" placeholder="Message envoyé à tous les serveurs de jeu (/say)"><button class="btn" data-a="bc">Envoyer</button></div></div>`;
  const draw = (s) => {
    if (!s) return;
    const sys = s.system, memUsed = sys.memTotal - sys.memFree;
    const proc = s.services.reduce((n, x) => n + x.mem, 0);
    $('#op').innerHTML = (s.operation ? `<div class="banner">⏳ ${esc(s.operation.name)}…</div>` : '') + (s.schedule.enabled && s.schedule.next ? `<div class="status-txt" style="margin-bottom:12px">⟳ Prochain redémarrage quotidien : ${new Date(s.schedule.next).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })}</div>` : '');
    $('#gauges').innerHTML = `
      <div class="gauge"><span class="status-txt">CPU machine</span><br><b>${sys.cpu}%</b><div class="bar"><i style="width:${sys.cpu}%"></i></div></div>
      <div class="gauge"><span class="status-txt">RAM machine</span><br><b>${fmtBytes(memUsed)}</b> <span class="status-txt">/ ${fmtBytes(sys.memTotal)}</span><div class="bar"><i style="width:${memUsed / sys.memTotal * 100}%"></i></div></div>
      <div class="gauge"><span class="status-txt">RAM des services</span><br><b>${fmtBytes(proc)}</b><div class="bar"><i style="width:${Math.min(100, proc / sys.memTotal * 100)}%"></i></div></div>`;
    $('#cards').innerHTML = s.services.map((x) => {
      const busy = ['starting', 'stopping'].includes(x.status), up = x.status === 'running';
      return `<div class="card ${x.status}" data-id="${x.id}"><h3><span class="dot"></span>${esc(x.name)}<span class="tag">${esc(x.type)}</span></h3>
        <div class="status-txt">${STATUS[x.status]}${x.status === 'crashed' && x.lastExit ? ` (code ${x.lastExit.code ?? x.lastExit.signal})` : ''}${x.restarts ? ` · ${x.restarts} redémarrage(s) auto` : ''}</div>
        <div class="meta"><div><span>Port</span><b>${x.port || '—'}</b></div><div><span>RAM</span><b>${x.mem ? fmtBytes(x.mem) : '—'}</b></div>
        <div><span>${x.type === 'game' ? 'Joueurs' : 'Uptime'}</span><b>${x.type === 'game' ? x.players.length + (x.maxPlayers ? '/' + x.maxPlayers : '') : (x.uptime ? fmtDur(x.uptime) : '—')}</b></div></div>
        <div class="row"><button class="btn sm pri" data-a="start" ${up || busy ? 'disabled' : ''}>▶ Start</button>
        <button class="btn sm" data-a="stop" ${!x.pid || x.status === 'stopping' ? 'disabled' : ''}>■ Stop</button>
        <button class="btn sm" data-a="restart" ${!x.pid || busy ? 'disabled' : ''}>↻ Restart</button>
        <button class="btn sm bad" data-a="kill" ${!x.pid ? 'disabled' : ''} title="Tue le processus">Kill</button>
        <button class="btn sm" data-a="console" style="margin-left:auto">Console</button></div></div>`;
    }).join('');
  };
  /* console globale : tous les services mélangés, filtrable, avec cible de commande */
  const COLORS = ['#d4a64a', '#6aa8e8', '#5fbf7a', '#e0632f', '#b48ae8', '#e06aa8', '#4fc8c8', '#c8c84f'];
  const gcon = $('#gcon'); let gpin = true; const hidden = new Set(); const ghist = []; let ghi = 0;
  const colorOf = (id) => COLORS[Math.max(0, state.services.findIndex((s) => s.id === id)) % COLORS.length];
  const nameOf = (id) => state.services.find((s) => s.id === id)?.name || id;
  gcon.addEventListener('scroll', () => { gpin = gcon.scrollTop + gcon.clientHeight > gcon.scrollHeight - 30; });
  const gline = (id, e) => {
    const d = document.createElement('div'); d.dataset.svc = id;
    d.className = e.k || (/\b(ERROR|SEVERE|FATAL)\b|Exception/.test(e.l) ? 'l-err' : /\bWARN/.test(e.l) ? 'l-warn' : '');
    if (hidden.has(id)) d.style.display = 'none';
    const ts = document.createElement('span'); ts.className = 'ts'; ts.textContent = new Date(e.t).toLocaleTimeString('fr-FR');
    const tag = document.createElement('span'); tag.className = 'ts'; tag.style.cssText = `color:${colorOf(id)};font-weight:600`; tag.textContent = `[${nameOf(id)}]`;
    d.append(ts, tag, document.createTextNode(' ' + e.l.replace(/\x1b\[[0-9;?]*[ -/]*[@-~]/g, '')));
    return d;
  };
  const gadd = (id, e) => { gcon.append(gline(id, e)); while (gcon.childElementCount > 3000) gcon.firstChild.remove(); if (gpin) gcon.scrollTop = gcon.scrollHeight; };
  const gfilters = () => {
    $('#gf').innerHTML = state.services.map((s) => `<button data-f="${s.id}" class="${hidden.has(s.id) ? '' : 'on'}" style="padding:3px 10px;font-size:12px">${esc(s.name)}</button>`).join('');
    const sel = $('#gt'), prev = sel.value;
    sel.innerHTML = '<option value="*game">Tous les serveurs de jeu</option><option value="*all">Tous les services</option>' + state.services.map((s) => `<option value="${s.id}">${esc(s.name)}</option>`).join('');
    if (prev) sel.value = prev;
  };
  $('#gf').addEventListener('click', (e) => {
    const b = e.target.closest('button'); if (!b) return;
    const id = b.dataset.f; hidden.has(id) ? hidden.delete(id) : hidden.add(id);
    b.classList.toggle('on', !hidden.has(id));
    gcon.querySelectorAll('[data-svc]').forEach((d) => { d.style.display = hidden.has(d.dataset.svc) ? 'none' : ''; });
    gcon.scrollTop = gcon.scrollHeight;
  });
  $('#gcf').addEventListener('submit', (e) => {
    e.preventDefault(); const v = $('#gci').value.trim(); if (!v) return;
    const t = $('#gt').value;
    const ids = t === '*game' ? state.services.filter((s) => s.type === 'game') : t === '*all' ? state.services : state.services.filter((s) => s.id === t);
    const live = ids.filter((s) => s.pid);
    if (!live.length) return toast('Aucun service démarré parmi la cible', true);
    ghist.push(v); ghi = ghist.length; $('#gci').value = '';
    act(() => Promise.all(live.map((s) => api(`/api/services/${s.id}/command`, { method: 'POST', body: { command: v } }))));
  });
  $('#gci').addEventListener('keydown', (e) => {
    if (e.key === 'ArrowUp' && ghi > 0) { $('#gci').value = ghist[--ghi]; e.preventDefault(); }
    if (e.key === 'ArrowDown') { $('#gci').value = ghist[++ghi] || ''; if (ghi > ghist.length) ghi = ghist.length; }
  });
  let gsig = '';
  pageHooks.log = (e) => gadd(e.id, e);
  pageHooks.status = (s) => { draw(s); const sig = s.services.map((x) => x.id + x.name).join(); if (sig !== gsig) { gsig = sig; gfilters(); } };
  draw(state); gfilters(); gsig = state.services.map((x) => x.id + x.name).join();
  Promise.all(state.services.map((s) => api(`/api/services/${s.id}/logs`).then((r) => r.logs.slice(-300).map((e) => ({ id: s.id, ...e })))))
    .then((all) => { all.flat().sort((a, b) => a.t - b.t).slice(-1200).forEach((e) => gcon.append(gline(e.id, e))); gcon.scrollTop = gcon.scrollHeight; }).catch(() => {});
  main().addEventListener('click', (e) => {
    const b = e.target.closest('[data-a]'); if (!b) return;
    const id = b.closest('.card')?.dataset.id, a = b.dataset.a;
    if (a === 'startall') act(() => api('/api/start-all', { method: 'POST' }), 'Démarrage lancé');
    else if (a === 'stopall') { if (confirm('Arrêter tous les services ?')) act(() => api('/api/stop-all', { method: 'POST' }), 'Arrêt lancé'); }
    else if (a === 'bc') { const v = $('#bc').value.trim(); if (v) act(async () => { await api('/api/broadcast', { method: 'POST', body: { message: v } }); $('#bc').value = ''; }, 'Annonce envoyée'); }
    else if (a === 'console') { localStorage.setItem('consoleSvc', id); location.hash = 'console'; }
    else if (a === 'kill') { if (confirm('Tuer le processus de force ? (risque de perte de données)')) act(() => api(`/api/services/${id}/kill`, { method: 'POST' })); }
    else act(() => api(`/api/services/${id}/${a}`, { method: 'POST' }));
  });
}

/* --------------------------------------------------------------- console */
function consolePage() {
  let cur = localStorage.getItem('consoleSvc') || state.services[0]?.id;
  if (!state.services.some((s) => s.id === cur)) cur = state.services[0]?.id;
  main().innerHTML = `<div class="top"><h2>Consoles</h2></div><div class="tabs" id="tabs"></div>
    <div class="console" id="con"></div>
    <form class="cmdline" id="cf"><input type="text" id="ci" placeholder="Commande (sans /)" autocomplete="off"><button class="btn pri">Envoyer</button></form>`;
  const con = $('#con'); let pinned = true; const hist = []; let hi = 0;
  con.addEventListener('scroll', () => { pinned = con.scrollTop + con.clientHeight > con.scrollHeight - 30; });
  const line = (e) => {
    const d = document.createElement('div');
    let cls = e.k || '';
    if (!cls) cls = /\b(ERROR|SEVERE|FATAL)\b|Exception/.test(e.l) ? 'l-err' : /\bWARN/.test(e.l) ? 'l-warn' : '';
    d.className = cls;
    const ts = document.createElement('span'); ts.className = 'ts'; ts.textContent = new Date(e.t).toLocaleTimeString('fr-FR');
    d.append(ts, document.createTextNode(e.l.replace(/\x1b\[[0-9;?]*[ -/]*[@-~]/g, '')));
    return d;
  };
  const addLine = (e) => { con.append(line(e)); while (con.childElementCount > 2500) con.firstChild.remove(); if (pinned) con.scrollTop = con.scrollHeight; };
  const tabs = () => { $('#tabs').innerHTML = state.services.map((s) => `<button data-id="${s.id}" class="${s.id === cur ? 'on' : ''}"><span class="dot" style="display:inline-block;background:${s.status === 'running' ? 'var(--ok)' : s.status === 'crashed' ? 'var(--bad)' : s.status === 'stopped' ? 'var(--dim)' : 'var(--warn)'}"></span>${esc(s.name)}</button>`).join(''); };
  const load = async () => {
    con.innerHTML = ''; tabs(); pinned = true;
    const r = await api(`/api/services/${cur}/logs`); r.logs.forEach(addLine); con.scrollTop = con.scrollHeight;
  };
  $('#tabs').addEventListener('click', (e) => { const b = e.target.closest('button'); if (b) { cur = b.dataset.id; localStorage.setItem('consoleSvc', cur); load(); } });
  $('#cf').addEventListener('submit', (e) => {
    e.preventDefault(); const v = $('#ci').value.trim(); if (!v) return;
    hist.push(v); hi = hist.length; $('#ci').value = '';
    act(() => api(`/api/services/${cur}/command`, { method: 'POST', body: { command: v } }));
  });
  $('#ci').addEventListener('keydown', (e) => {
    if (e.key === 'ArrowUp' && hi > 0) { $('#ci').value = hist[--hi]; e.preventDefault(); }
    if (e.key === 'ArrowDown') { $('#ci').value = hist[++hi] || ''; if (hi > hist.length) hi = hist.length; }
  });
  pageHooks.log = (e) => { if (e.id === cur) addLine(e); };
  pageHooks.status = tabs;
  load();
}

/* --------------------------------------------------------------- joueurs */
async function playersPage() {
  main().innerHTML = `<div class="top"><h2>Joueurs</h2><div class="row"><input type="text" id="q" placeholder="Rechercher un pseudo…" style="width:240px"><button class="btn" data-a="add">+ Action sur un pseudo</button></div></div>
    <div class="box"><h3>En ligne</h3><div id="on"></div></div>
    <div class="grid2"><div class="box"><h3>Historique</h3><div id="kn" style="max-height:420px;overflow:auto"></div></div>
    <div class="box"><h3>Bannis</h3><div id="bn" style="max-height:420px;overflow:auto"></div></div></div>`;
  let d;
  const draw = () => {
    const q = $('#q').value.trim().toLowerCase();
    const btn = (a, n, l, cls = '') => `<button class="btn sm ${cls}" data-a="${a}" data-n="${esc(n)}">${l}</button>`;
    $('#on').innerHTML = d.online.length ? `<table><tr><th>Joueur</th><th>Serveur</th><th></th></tr>${d.online.filter((p) => p.name.toLowerCase().includes(q)).map((p) => `<tr><td><b>${esc(p.name)}</b> ${Object.values(d.ops).some((o) => o.includes(p.name)) ? '<span class="pill ok">OP</span>' : ''}</td><td><span class="pill">${esc(p.server)}</span></td><td style="text-align:right">${btn('act', p.name, 'Actions')}</td></tr>`).join('')}</table>` : '<div class="empty">Personne en ligne</div>';
    const known = Object.entries(d.known).filter(([n]) => n.toLowerCase().includes(q)).sort((a, b) => b[1].lastSeen - a[1].lastSeen);
    $('#kn').innerHTML = known.length ? `<table><tr><th>Joueur</th><th>Dernière visite</th><th>Visites</th><th></th></tr>${known.map(([n, p]) => `<tr><td>${esc(n)}</td><td>${fmtDate(p.lastSeen)}<br><span class="status-txt">${esc(p.lastServer || '')}</span></td><td>${p.sessions}</td><td style="text-align:right">${btn('act', n, 'Actions')}</td></tr>`).join('')}</table>` : '<div class="empty">Aucun joueur enregistré</div>';
    const bans = {}; Object.entries(d.bans).forEach(([sid, l]) => l.forEach((b) => (bans[b.name] ||= { ...b, on: [] }).on.push(sid)));
    const bl = Object.values(bans).filter((b) => b.name.toLowerCase().includes(q));
    $('#bn').innerHTML = bl.length ? `<table><tr><th>Joueur</th><th>Raison</th><th></th></tr>${bl.map((b) => `<tr><td>${esc(b.name)}<br>${b.on.map((s) => `<span class="pill bad">${esc(s)}</span>`).join('')}</td><td>${esc(b.reason || '')}</td><td style="text-align:right">${btn('unban', b.name, 'Débannir')}</td></tr>`).join('')}</table>` : '<div class="empty">Aucun ban (lu dans banned-players.json)</div>';
  };
  const refresh = async () => { d = await api('/api/players'); draw(); };
  await refresh();
  pageHooks.status = () => { clearTimeout(playersPage._t); playersPage._t = setTimeout(() => page === 'players' && refresh().catch(() => {}), 800); };
  $('#q').addEventListener('input', draw);
  main().addEventListener('click', (e) => {
    const b = e.target.closest('[data-a]'); if (!b) return;
    if (b.dataset.a === 'unban') act(async () => { await api('/api/players/action', { method: 'POST', body: { action: 'pardon', player: b.dataset.n, targets: 'all' } }); setTimeout(refresh, 800); }, 'Pardon envoyé');
    else playerModal(b.dataset.a === 'add' ? '' : b.dataset.n, d, refresh);
  });
}
function playerModal(name, d, refresh) {
  const srv = d.servers;
  modal('Action sur un joueur', `
    <div class="field"><label>Pseudo</label><input type="text" id="pn" value="${esc(name)}"></div>
    <div class="field"><label>Action</label><select id="pa">
      <option value="kick">Kick</option><option value="ban">Bannir</option><option value="pardon">Débannir</option>
      <option value="msg">Message privé</option><option value="op">Donner OP</option><option value="deop">Retirer OP</option>
      <option value="whitelist_add">Whitelist +</option><option value="whitelist_remove">Whitelist −</option>
      <option value="gamemode">Gamemode…</option><option value="tp">Téléporter vers…</option><option value="kill">Tuer</option><option value="clear">Vider l'inventaire</option></select></div>
    <div class="field"><label id="pel">Raison / valeur</label><input type="text" id="pe" placeholder="optionnel"></div>
    <div class="field"><label>Serveurs ciblés</label><div>${srv.map((s) => `<label style="display:inline-flex;gap:6px;margin-right:12px;text-transform:none;font-size:13px;color:var(--text)"><input type="checkbox" class="pt" value="${s.id}" ${d.online.some((o) => o.name === name && o.server === s.id) || !d.online.some((o) => o.name === name) ? 'checked' : ''}>${esc(s.name)}</label>`).join('')}</div></div>`,
    async (bg) => {
      const targets = [...bg.querySelectorAll('.pt:checked')].map((c) => c.value);
      const a = $('#pa', bg).value;
      if (a === 'ban' && !confirm('Bannir ce joueur sur les serveurs sélectionnés ?')) throw new Error('Annulé');
      const r = await api('/api/players/action', { method: 'POST', body: { action: a, player: $('#pn', bg).value.trim(), extra: $('#pe', bg).value, targets } });
      toast(`/${r.command} → ${r.sent.join(', ') || 'aucun serveur en ligne'}`, !r.sent.length);
      setTimeout(refresh, 900);
    }, 'Exécuter');
}

/* -------------------------------------------------------------- fichiers */
function filesPage() {
  let cur = localStorage.getItem('filesSvc') || state.services[0]?.id, dir = '', file = null;
  main().innerHTML = `<div class="top"><h2>Fichiers</h2><select id="fs" style="width:240px">${state.services.map((s) => `<option value="${s.id}">${esc(s.name)}</option>`).join('')}</select></div>
    <div class="files"><div class="box"><div class="status-txt" id="crumb" style="margin-bottom:8px"></div><div class="flist" id="fl"></div></div>
    <div class="box"><div class="row" style="justify-content:space-between;margin-bottom:10px"><b id="fn">Aucun fichier ouvert</b><button class="btn pri sm" id="sv" disabled>Enregistrer</button></div>
    <textarea class="editor" id="ed" spellcheck="false" disabled></textarea><div class="status-txt" style="margin-top:6px">Une copie .bak est créée à chaque enregistrement. Redémarre le service pour appliquer.</div></div></div>`;
  $('#fs').value = cur;
  const list = () => act(async () => {
    const r = await api(`/api/files?id=${cur}&path=${encodeURIComponent(dir)}`);
    $('#crumb').textContent = '/' + r.path;
    $('#fl').innerHTML = (dir ? '<a data-up>⬑ ..</a>' : '') + r.entries.map((e) => e.dir ? `<a data-d="${esc(e.name)}">📁 ${esc(e.name)}</a>` : e.editable ? `<a data-f="${esc(e.name)}">📄 ${esc(e.name)}<span class="status-txt">${(e.size / 1024).toFixed(1)} Ko</span></a>` : `<a class="dim">${esc(e.name)}<span class="status-txt">${(e.size / 1024 | 0)} Ko</span></a>`).join('');
  });
  $('#fs').addEventListener('change', (e) => { cur = e.target.value; localStorage.setItem('filesSvc', cur); dir = ''; file = null; $('#ed').value = ''; $('#ed').disabled = $('#sv').disabled = true; list(); });
  $('#fl').addEventListener('click', (e) => {
    const a = e.target.closest('a'); if (!a) return;
    if (a.dataset.up !== undefined) { dir = dir.split('/').slice(0, -1).join('/'); list(); }
    else if (a.dataset.d) { dir = (dir ? dir + '/' : '') + a.dataset.d; list(); }
    else if (a.dataset.f) act(async () => {
      file = (dir ? dir + '/' : '') + a.dataset.f;
      const r = await api(`/api/file?id=${cur}&path=${encodeURIComponent(file)}`);
      $('#ed').value = r.content; $('#ed').disabled = $('#sv').disabled = false; $('#fn').textContent = file;
    });
  });
  $('#sv').addEventListener('click', () => act(() => api(`/api/file?id=${cur}&path=${encodeURIComponent(file)}`, { method: 'PUT', body: { content: $('#ed').value } }), 'Fichier enregistré'));
  list();
}

/* -------------------------------------------------------------- sécurité */
const SEC_TYPES = {
  speed: 'Déplacement trop rapide', speed_burst: 'Rafale de vitesse (speed/TP)', fly: 'Vol non autorisé', cmd: 'Commande sensible',
  chat_flood: 'Flood de chat', chat_repeat: 'Message répété', ip_flood: 'Flood de connexions', multi_account: 'Multi-comptes (même IP)',
  bad_disconnect: 'Déconnexion anormale', packet: 'Erreur de paquet', keyword: 'Mot-clé surveillé',
};
const SEV_LABEL = { low: 'Faible', medium: 'Moyen', high: 'Élevé' };
const sevPill = (s) => `<span class="pill ${s === 'high' ? 'bad' : s === 'medium' ? 'warn' : ''}">${SEV_LABEL[s] || s}</span>`;

async function securityPage() {
  secUnread = 0; updateSecBadge();
  main().innerHTML = `<div class="top"><h2>Sécurité</h2><div class="row">
      <select id="sh" style="width:150px"><option value="1">1 heure</option><option value="24" selected>24 heures</option><option value="168">7 jours</option><option value="720">30 jours</option></select>
      <button class="btn" data-a="scan">Analyser les logs existants</button><button class="btn bad" data-a="clear">Vider</button></div></div>
    <div class="banner" style="color:var(--text)">Détection basée sur les <b>logs du serveur</b> (déplacements anormaux, vol, flood, multi-comptes, commandes sensibles…). Elle donne des indices, pas des preuves : un lag peut ressembler à du speed. Un vrai anticheat en jeu reste nécessaire pour le killaura, le x-ray, etc.</div>
    <div class="gauges" id="ss"></div>
    <div class="grid2"><div class="box"><h3>Joueurs à risque</h3><div id="sp" style="max-height:380px;overflow:auto"></div></div>
    <div class="box"><h3>Comptes partageant une IP</h3><div id="sg" style="max-height:380px;overflow:auto"></div></div></div>
    <div class="box"><div class="row" style="justify-content:space-between;margin-bottom:10px"><h3 style="margin:0">Alertes</h3><div class="row">
      <select id="fsev" style="width:130px"><option value="">Toute gravité</option><option value="high">Élevé</option><option value="medium">Moyen</option><option value="low">Faible</option></select>
      <select id="ftype" style="width:200px"><option value="">Tous les types</option>${Object.entries(SEC_TYPES).map(([k, v]) => `<option value="${k}">${v}</option>`).join('')}</select>
      <input type="text" id="fpl" placeholder="Joueur…" style="width:150px"></div></div><div id="se" style="max-height:460px;overflow:auto"></div></div>
    <div class="box"><h3>Recherche dans les logs</h3><form class="cmdline" id="lf2" style="margin:0 0 10px"><select id="lsv" style="width:200px"><option value="">Tous les services</option>${state.services.map((s) => `<option value="${s.id}">${esc(s.name)}</option>`).join('')}</select>
      <input type="text" id="lq" placeholder="Texte ou pseudo à chercher (min. 2 caractères)"><label style="display:flex;gap:6px;align-items:center;margin:0;text-transform:none;white-space:nowrap"><input type="checkbox" id="lrx"> regex</label><button class="btn pri">Chercher</button></form>
      <div class="console" id="lres" style="height:260px;min-height:0"></div></div>`;
  let d = null;
  const draw = () => {
    if (!d) return;
    $('#ss').innerHTML = [['Alertes', d.stats.total, ''], ['Gravité élevée', d.stats.high, 'var(--bad)'], ['Gravité moyenne', d.stats.medium, 'var(--warn)'], ['Joueurs à risque', d.stats.risky, 'var(--gold2)']]
      .map(([l, v, c]) => `<div class="gauge"><span class="status-txt">${l}</span><br><b style="${c ? 'color:' + c : ''}">${v}</b></div>`).join('');
    $('#sp').innerHTML = d.players.length ? `<table><tr><th>Joueur</th><th>Score</th><th>Détail</th><th></th></tr>${d.players.slice(0, 50).map((p) => `<tr><td><b>${esc(p.name)}</b><br><span class="status-txt">${fmtDate(p.last)}</span></td>
      <td>${sevPill(p.level)} ${p.score}</td><td>${Object.entries(p.counts).map(([t, n]) => `<span class="pill">${esc(SEC_TYPES[t] || t)} ×${n}</span>`).join('')}</td>
      <td style="text-align:right"><button class="btn sm" data-a="filter" data-n="${esc(p.name)}">Voir</button> <button class="btn sm" data-a="act" data-n="${esc(p.name)}">Actions</button></td></tr>`).join('')}</table>` : '<div class="empty">Aucun joueur signalé sur cette période</div>';
    $('#sg').innerHTML = d.groups.length ? `<table><tr><th>IP</th><th>Comptes</th></tr>${d.groups.map((g) => `<tr><td>${esc(g.ip)}</td><td>${g.names.map((n) => `<span class="pill ${g.names.length >= 3 ? 'bad' : ''}">${esc(n)}</span>`).join('')}</td></tr>`).join('')}</table>
      <div class="status-txt" style="margin-top:8px">Les IP locales sont ignorées. Sans transfert d'IP par le proxy, seules celles vues par Velocity sont fiables.</div>` : '<div class="empty">Aucune IP partagée détectée</div>';
    const sev = $('#fsev').value, ty = $('#ftype').value, pl = $('#fpl').value.trim().toLowerCase();
    const ev = d.events.filter((e) => (!sev || e.sev === sev) && (!ty || e.type === ty) && (!pl || e.player.toLowerCase().includes(pl)));
    $('#se').innerHTML = ev.length ? `<table><tr><th>Heure</th><th>Gravité</th><th>Serveur</th><th>Joueur</th><th>Type</th><th>Détail</th></tr>${ev.slice(0, 400).map((e) => `<tr><td style="white-space:nowrap">${fmtDate(e.t)}</td><td>${sevPill(e.sev)}</td><td><span class="pill">${esc(e.server)}</span></td>
      <td>${e.player ? `<a href="#" data-a="filter" data-n="${esc(e.player)}" style="color:var(--gold2)">${esc(e.player)}</a>` : '—'}</td><td>${esc(SEC_TYPES[e.type] || e.type)}</td><td style="word-break:break-word;font-size:12.5px">${esc(e.detail)}</td></tr>`).join('')}</table>` : '<div class="empty">Aucune alerte</div>';
  };
  const refresh = () => act(async () => { d = await api('/api/security?hours=' + $('#sh').value); draw(); });
  await refresh();
  let tm; pageHooks.alert = () => { clearTimeout(tm); tm = setTimeout(() => page === 'sec' && refresh(), 1000); };
  ['#sh'].forEach((q) => $(q).addEventListener('change', refresh));
  ['#fsev', '#ftype'].forEach((q) => $(q).addEventListener('change', draw)); $('#fpl').addEventListener('input', draw);
  $('#lf2').addEventListener('submit', (e) => {
    e.preventDefault(); const q = $('#lq').value.trim();
    act(async () => {
      const r = await api(`/api/security/search?q=${encodeURIComponent(q)}&id=${encodeURIComponent($('#lsv').value)}&regex=${$('#lrx').checked ? 1 : 0}`);
      const box = $('#lres'); box.innerHTML = '';
      if (!r.lines.length) box.innerHTML = '<div class="empty">Aucun résultat</div>';
      r.lines.forEach((x) => { const dv = document.createElement('div'); const t = document.createElement('span'); t.className = 'ts'; t.style.color = 'var(--gold)'; t.textContent = `[${x.id}]`; dv.append(t, document.createTextNode(' ' + x.l)); box.append(dv); });
      box.scrollTop = box.scrollHeight;
    });
  });
  main().addEventListener('click', (e) => {
    const b = e.target.closest('[data-a]'); if (!b) return; e.preventDefault();
    const a = b.dataset.a;
    if (a === 'filter') { $('#fpl').value = b.dataset.n; draw(); }
    else if (a === 'scan') act(async () => { const r = await api('/api/security/scan', { method: 'POST' }); await refresh(); toast(`${r.files} fichier(s) de logs analysé(s), ${r.added} nouvelle(s) alerte(s)`); });
    else if (a === 'clear') { if (confirm('Effacer toutes les alertes enregistrées ?')) act(async () => { await api('/api/security/clear', { method: 'POST' }); await refresh(); }, 'Alertes effacées'); }
    else if (a === 'act') act(async () => { const pd = await api('/api/players'); playerModal(b.dataset.n, pd, () => {}); });
  });
}

/* ------------------------------------------------------- mise à jour du mod */
async function modPage() {
  let d = await api('/api/mod');
  const draw = () => {
    const s = d.state, c = d.cfg, rel = s.release, busy = state && state.operation;
    const tg = (s.targets || []).map((t) => `<tr><td>${esc(t.name)}</td><td style="font-family:var(--mono)">${esc(t.files.join(', ') || '—')}</td><td>${t.upToDate ? '✔ à jour' : '⬆ mise à jour disponible'}</td><td>${t.running ? 'en ligne' : 'arrêté'}</td></tr>`).join('');
    const last = s.last;
    main().innerHTML = `<div class="top"><h2>Mise à jour du mod</h2><div class="row"><button class="btn" id="chk">Vérifier</button><button class="btn pri" id="upd" ${busy ? 'disabled' : ''}>Mettre à jour le mod</button></div></div>
      ${s.error ? `<div class="banner">⚠ ${esc(s.error)}</div>` : ''}
      <div class="box"><h3>Dernière version sur GitHub</h3>
        <div>${rel ? `${esc(rel.name)} — <code>${esc(rel.asset.name)}</code> (${fmtBytes(rel.asset.size)}), publiée ${fmtDate(rel.publishedAt)}` : 'Pas encore vérifié.'}</div>
        <div class="status-txt" style="margin-top:6px">Vérifié : ${fmtDate(s.checkedAt)} — ${s.available ? '<b>mise à jour disponible</b>' : 'tout est à jour'}</div>
        ${tg ? `<table style="width:100%;margin-top:10px"><tr><th align="left">Serveur</th><th align="left">Jar installé</th><th align="left">État</th><th align="left">Service</th></tr>${tg}</table>` : ''}
        <div class="status-txt" style="margin-top:10px">Mettre à jour : télécharge le jar (vérifié par SHA-256), sauvegarde l'ancien dans <code>servers/&lt;id&gt;/mods_backup/&lt;date&gt;</code>, le remplace sur chaque serveur coché, puis redémarre ceux qui tournaient. Mondes, base et configs ne sont pas touchés. Pense à copier le même jar côté client.</div></div>
      ${last ? `<div class="box"><h3>Dernière mise à jour — ${fmtDate(last.at)} ${last.ok === true ? '✔' : last.ok === false ? '✘' : '⏳'}</h3><pre style="white-space:pre-wrap;margin:0">${esc((last.steps || []).join('\n'))}</pre></div>` : ''}
      <div class="box"><h3>Réglages</h3><div class="fields">
        <div><label>Dépôt GitHub</label><input id="m-repo" value="${esc(c.repo)}"></div>
        <div><label>Tag de la release</label><input id="m-tag" value="${esc(c.tag)}"></div>
        <div><label>Fichier à télécharger (regex)</label><input id="m-ap" value="${esc(c.assetPattern)}"></div>
        <div><label>Anciens jars à remplacer (regex)</label><input id="m-rp" value="${esc(c.replacePattern)}"></div>
        <div><label>Dossier mods</label><input id="m-md" value="${esc(c.modsDir)}"></div>
        <div><label>Token GitHub (dépôt privé)</label><input id="m-tk" type="password" placeholder="${d.tokenFromEnv ? 'fourni par AELORIA_GITHUB_TOKEN' : d.hasToken ? '•••• enregistré (laisser vide = inchangé)' : 'optionnel si le dépôt est public'}" ${d.tokenFromEnv ? 'disabled' : ''}></div>
        <div class="wide"><label>Serveurs cibles</label>${d.services.map((x) => `<label style="display:inline-flex;gap:6px;margin-right:14px;text-transform:none;font-size:14px;color:var(--text)"><input type="checkbox" data-t="${esc(x.id)}" ${c.targets.includes(x.id) ? 'checked' : ''}> ${esc(x.name)}</label>`).join('')}</div>
        <div class="wide"><label style="display:flex;gap:8px;align-items:center;text-transform:none;font-size:14px;color:var(--text)"><input type="checkbox" id="m-boot" ${c.checkOnBoot ? 'checked' : ''}> Vérifier au démarrage du panel puis toutes les 6 h</label></div>
      </div><div class="row" style="margin-top:12px"><button class="btn" id="m-save">Enregistrer les réglages</button></div></div>`;
    $('#chk').addEventListener('click', () => act(async () => { await api('/api/mod/check', { method: 'POST' }); d = await api('/api/mod'); draw(); }, 'Vérification terminée'));
    $('#m-save').addEventListener('click', () => act(async () => {
      const body = { repo: $('#m-repo').value, tag: $('#m-tag').value, assetPattern: $('#m-ap').value, replacePattern: $('#m-rp').value, modsDir: $('#m-md').value, checkOnBoot: $('#m-boot').checked, targets: [...document.querySelectorAll('[data-t]:checked')].map((x) => x.dataset.t) };
      if ($('#m-tk').value) body.token = $('#m-tk').value;
      await api('/api/mod/config', { method: 'PUT', body }); d = await api('/api/mod'); draw();
    }, 'Réglages enregistrés'));
    $('#upd').addEventListener('click', () => {
      const run = (restart) => act(async () => { await api('/api/mod/update', { method: 'POST', body: { restart } }); toast('Mise à jour lancée'); }, null);
      const running = (s.targets || []).filter((t) => t.running).map((t) => t.name);
      if (running.length) { if (confirm(`Ces serveurs tournent : ${running.join(', ')}.\nIls seront arrêtés proprement (annonce 15 s avant), mis à jour, puis redémarrés. Continuer ?`)) run(true); }
      else if (confirm('Installer la dernière version du mod sur les serveurs cibles ?')) run(false);
    });
  };
  draw();
  pageHooks.status = async () => {
    try {
      const n = await api('/api/mod'); const changed = JSON.stringify(n.state) !== JSON.stringify(d.state); d = n;
      if (changed && !/INPUT|SELECT/.test(document.activeElement.tagName)) draw();
    } catch {}
  };
}

/* ------------------------------------------------------------ paramètres */
async function settingsPage() {
  const cfg = await api('/api/config');
  let list = cfg.services;
  main().innerHTML = `<div class="top"><h2>Paramètres</h2><div class="row"><button class="btn" id="add">+ Ajouter un service</button><button class="btn pri" id="save">Enregistrer</button></div></div>
    <div class="banner" style="color:var(--text)">Dossier du panel : <code>${esc(cfg.root)}</code> — les chemins relatifs sont résolus depuis ce dossier. Les services sont démarrés dans l'ordre croissant et arrêtés dans l'ordre inverse.</div>
    <label style="display:flex;gap:8px;align-items:center;text-transform:none;font-size:14px;color:var(--text);margin-bottom:16px"><input type="checkbox" id="resume" ${cfg.panel.resumeOnBoot ? 'checked' : ''}> Relancer automatiquement les services qui tournaient quand le panel redémarre</label>
    <div class="box"><h3>Redémarrage quotidien</h3><div class="row"><label style="display:flex;gap:8px;align-items:center;text-transform:none;font-size:14px;color:var(--text);margin:0"><input type="checkbox" id="sc-on" ${cfg.panel.schedule.enabled ? 'checked' : ''}> Activer, tous les jours à</label><input type="time" id="sc-time" value="${esc(cfg.panel.schedule.time)}" style="width:120px"></div><div class="status-txt" style="margin-top:8px">Annonce en jeu 5 min et 1 min avant. Seuls les services dont « Redémarrage quotidien » est sur Oui sont redémarrés (arrêt en ordre inverse, puis démarrage dans l'ordre).</div></div>
    <div class="box"><h3>Mots-clés à surveiller (page Sécurité)</h3><textarea id="kw" rows="3" placeholder="Un par ligne (regex acceptée), ex. : dupe, exploit, hack" style="font-family:var(--mono)">${esc((cfg.panel.security.keywords || []).join('\n'))}</textarea><div class="status-txt" style="margin-top:6px">Toute ligne de log qui correspond crée une alerte.</div></div>
    <div id="sv"></div>
    <div class="box"><h3>Mot de passe admin</h3><div class="row"><input type="password" id="p1" placeholder="Actuel" style="width:200px"><input type="password" id="p2" placeholder="Nouveau (8+)" style="width:200px"><button class="btn" id="pw">Changer</button><button class="btn bad" id="lo">Se déconnecter</button></div></div>`;
  const F = [['id', 'Identifiant', 'text'], ['name', 'Nom', 'text'], ['type', 'Type', 'select'], ['order', 'Ordre', 'number'], ['port', 'Port (détection « prêt »)', 'number'], ['stopTimeout', 'Délai d\'arrêt (s)', 'number'], ['dir', 'Dossier', 'text', 1], ['command', 'Commande de démarrage', 'text', 1], ['stopCommand', 'Commande d\'arrêt (console)', 'text'], ['stopExec', 'Ou programme d\'arrêt', 'text']];
  const draw = () => {
    $('#sv').innerHTML = list.map((s, i) => `<div class="svc-edit" data-i="${i}"><div class="row" style="justify-content:space-between;margin-bottom:10px"><b style="font-family:var(--serif);font-size:16px">${esc(s.name || s.id)}</b><button class="btn sm bad" data-del>Supprimer</button></div><div class="fields">
      ${F.map(([k, l, t, w]) => `<div class="${w ? 'wide' : ''}"><label>${l}</label>${t === 'select' ? `<select data-k="${k}">${['game', 'proxy', 'db', 'other'].map((o) => `<option ${s[k] === o ? 'selected' : ''}>${o}</option>`).join('')}</select>` : `<input type="${t}" data-k="${k}" value="${esc(s[k])}">`}</div>`).join('')}
      <div><label>Redémarrage quotidien</label><select data-k="dailyRestart"><option value="1" ${s.dailyRestart ? 'selected' : ''}>Oui</option><option value="0" ${!s.dailyRestart ? 'selected' : ''}>Non</option></select></div>
      <div><label>Redémarrage auto en cas de crash</label><select data-k="autoRestart"><option value="1" ${s.autoRestart ? 'selected' : ''}>Oui</option><option value="0" ${!s.autoRestart ? 'selected' : ''}>Non</option></select></div></div></div>`).join('');
  };
  const collect = () => document.querySelectorAll('.svc-edit').forEach((el) => { const s = list[+el.dataset.i]; el.querySelectorAll('[data-k]').forEach((f) => { const k = f.dataset.k; s[k] = (k === 'autoRestart' || k === 'dailyRestart') ? f.value === '1' : f.type === 'number' ? +f.value : f.value; }); });
  draw();
  $('#add').addEventListener('click', () => { collect(); list.push({ id: 'nouveau', name: 'Nouveau service', type: 'game', order: list.length + 1, dir: 'servers/nouveau', command: 'java -Xmx2G -jar server.jar nogui', port: 25580, stopCommand: 'stop', stopExec: '', stopTimeout: 60, autoRestart: true, dailyRestart: true }); draw(); });
  $('#sv').addEventListener('click', (e) => { if (e.target.dataset.del !== undefined && confirm('Supprimer ce service de la config ? (les fichiers ne sont pas touchés)')) { collect(); list.splice(+e.target.closest('.svc-edit').dataset.i, 1); draw(); } });
  $('#save').addEventListener('click', () => { collect(); act(() => api('/api/config', { method: 'PUT', body: { services: list, panel: { resumeOnBoot: $('#resume').checked, schedule: { enabled: $('#sc-on').checked, time: $('#sc-time').value }, security: { keywords: $('#kw').value.split('\n') } } } }), 'Configuration enregistrée'); });
  $('#pw').addEventListener('click', () => act(async () => { await api('/api/password', { method: 'POST', body: { current: $('#p1').value, next: $('#p2').value } }); $('#p1').value = $('#p2').value = ''; }, 'Mot de passe changé'));
  $('#lo').addEventListener('click', () => act(async () => { await api('/api/logout', { method: 'POST' }); location.reload(); }));
}

boot().catch((e) => { app.textContent = 'Erreur : ' + e.message; });
