'use strict';
// Aeloria Panel — gestionnaire de serveurs Minecraft (zéro dépendance, Node >= 18)
const http = require('http');
const fs = require('fs');
const path = require('path');
const os = require('os');
const net = require('net');
const crypto = require('crypto');
const zlib = require('zlib');
const { spawn, execFile, exec } = require('child_process');

const ROOT = __dirname;
const DATA = path.join(ROOT, 'data');
const LOGS = path.join(DATA, 'logs');
const CONFIG_FILE = path.join(ROOT, 'config.json');
const AUTH_FILE = path.join(DATA, 'auth.json');
const SESSIONS_FILE = path.join(DATA, 'sessions.json');
const STATE_FILE = path.join(DATA, 'state.json');
const PLAYERS_FILE = path.join(DATA, 'players.json');
fs.mkdirSync(LOGS, { recursive: true });

process.on('uncaughtException', (e) => console.error('[panel] uncaughtException:', e));
process.on('unhandledRejection', (e) => console.error('[panel] unhandledRejection:', e));

/* ------------------------------------------------------------------ config */
const DEFAULT_CONFIG = {
  panel: { host: '127.0.0.1', port: 8090, resumeOnBoot: true, schedule: { enabled: false, time: '05:00', warnMinutes: [5, 1] }, security: { keywords: [] } },
  services: [
    { id: 'db', name: 'Base de données', type: 'db', order: 1, dir: 'servers/db', command: 'mysqld --console', port: 3306, stopCommand: '', stopExec: 'mysqladmin -u root shutdown', stopTimeout: 30, autoRestart: true },
    { id: 'proxy', name: 'Proxy (Velocity)', type: 'proxy', order: 2, dir: 'servers/proxy', command: 'java -Xms512M -Xmx1G -jar velocity.jar', port: 25565, stopCommand: 'end', stopExec: '', stopTimeout: 30, autoRestart: true },
    { id: 'serveur', name: 'Serveur principal', type: 'game', order: 3, dir: 'servers/serveur', command: 'java -Xms2G -Xmx4G -jar server.jar nogui', port: 25570, stopCommand: 'stop', stopExec: '', stopTimeout: 90, autoRestart: true },
    { id: 'hub', name: 'Hub', type: 'game', order: 4, dir: 'servers/hub', command: 'java -Xms1G -Xmx2G -jar server.jar nogui', port: 25571, stopCommand: 'stop', stopExec: '', stopTimeout: 60, autoRestart: true },
    { id: 'mines', name: 'Mines', type: 'game', order: 5, dir: 'servers/mines', command: 'java -Xms1G -Xmx3G -jar server.jar nogui', port: 25572, stopCommand: 'stop', stopExec: '', stopTimeout: 60, autoRestart: true },
    { id: 'plaines', name: 'Plaines', type: 'game', order: 6, dir: 'servers/plaines', command: 'java -Xms1G -Xmx3G -jar server.jar nogui', port: 25573, stopCommand: 'stop', stopExec: '', stopTimeout: 60, autoRestart: true },
  ],
};

function readJson(file, fallback) {
  try { return JSON.parse(fs.readFileSync(file, 'utf8')); } catch { return fallback; }
}
function writeJson(file, obj) {
  const tmp = file + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(obj, null, 2));
  fs.renameSync(tmp, file);
}

let config = readJson(CONFIG_FILE, null);
if (!config) { config = DEFAULT_CONFIG; writeJson(CONFIG_FILE, config); }
config.panel = Object.assign({}, DEFAULT_CONFIG.panel, config.panel);
config.panel.schedule = Object.assign({}, DEFAULT_CONFIG.panel.schedule, config.panel.schedule);
config.panel.security = Object.assign({}, DEFAULT_CONFIG.panel.security, config.panel.security);

const resolveDir = (d) => path.resolve(ROOT, d || '.');

/* ---------------------------------------------------------------- services */
const services = new Map(); // id -> runtime
const subscribers = new Set(); // SSE responses
let shuttingDown = false;
let operation = null; // { name, startedAt }
const LOG_BUFFER = 3000;
const ID_RE = /^[a-z0-9_-]{1,32}$/;

function sanitizeService(s, i) {
  if (!s || !ID_RE.test(s.id || '')) throw new Error('Identifiant invalide (a-z, 0-9, - et _ uniquement)');
  return {
    id: s.id,
    name: String(s.name || s.id).slice(0, 60),
    type: ['db', 'proxy', 'game', 'other'].includes(s.type) ? s.type : 'game',
    order: Number.isFinite(+s.order) ? +s.order : i + 1,
    dir: String(s.dir || ''),
    command: String(s.command || ''),
    port: s.port ? Math.max(0, Math.min(65535, +s.port | 0)) : 0,
    stopCommand: String(s.stopCommand || ''),
    stopExec: String(s.stopExec || ''),
    stopTimeout: Math.max(5, Math.min(600, +s.stopTimeout || 60)),
    autoRestart: !!s.autoRestart,
    dailyRestart: s.dailyRestart === undefined ? s.type === 'game' : !!s.dailyRestart,
  };
}

function applyConfigServices(list) {
  const seen = new Set();
  const clean = list.map((s, i) => {
    const c = sanitizeService(s, i);
    if (seen.has(c.id)) throw new Error('Identifiant en double : ' + c.id);
    seen.add(c.id);
    return c;
  });
  for (const id of [...services.keys()]) {
    if (!seen.has(id)) {
      const s = services.get(id);
      if (s.proc) throw new Error(`Arrête « ${s.cfg.name} » avant de le supprimer`);
      services.delete(id);
    }
  }
  for (const c of clean) {
    const s = services.get(c.id);
    if (s) s.cfg = c; else services.set(c.id, newRuntime(c));
  }
  config.services = clean;
}

function newRuntime(cfg) {
  return {
    cfg, proc: null, status: 'stopped', pid: null, startedAt: null, mem: 0,
    logs: [], lineBuf: '', desired: false, restarts: 0, crashTimes: [],
    lastExit: null, stopTimer: null, restartTimer: null, waiters: [],
    players: [], maxPlayers: 0, listPendingUntil: 0, stopping: false,
  };
}

for (const [i, c] of config.services.entries()) {
  try { const cc = sanitizeService(c, i); services.set(cc.id, newRuntime(cc)); }
  catch (e) { console.error('[config] service ignoré :', e.message); }
}

const ordered = () => [...services.values()].sort((a, b) => a.cfg.order - b.cfg.order);
const get = (id) => { const s = services.get(id); if (!s) throw httpError(404, 'Service inconnu'); return s; };

function httpError(code, msg) { const e = new Error(msg); e.code = code; return e; }

function tokenize(line) {
  const out = []; const re = /"([^"]*)"|'([^']*)'|(\S+)/g; let m;
  while ((m = re.exec(line))) out.push(m[1] ?? m[2] ?? m[3]);
  return out;
}

const ANSI_RE = /\x1b\[[0-9;?]*[ -/]*[@-~]/g;
const stripAnsi = (s) => s.replace(ANSI_RE, '');

function broadcast(event, data) {
  const msg = `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`;
  for (const r of subscribers) { try { r.write(msg); } catch { subscribers.delete(r); } }
}

function pushLog(s, line, kind) {
  const entry = { t: Date.now(), l: line, k: kind || '' };
  s.logs.push(entry);
  if (s.logs.length > LOG_BUFFER) s.logs.splice(0, s.logs.length - LOG_BUFFER);
  try { fs.appendFile(path.join(LOGS, s.cfg.id + '.log'), `[${new Date(entry.t).toISOString()}] ${line}\n`, () => {}); } catch {}
  broadcast('log', { id: s.cfg.id, ...entry });
}

/* ------------------------------------------------------------------ joueurs */
const knownPlayers = readJson(PLAYERS_FILE, {});
let playersDirty = false;
setInterval(() => { if (playersDirty) { playersDirty = false; writeJson(PLAYERS_FILE, knownPlayers); } }, 10000);
const NAME = '[A-Za-z0-9_.]{1,17}';
const JOIN_RE = new RegExp(`\\]: (${NAME}) joined the game\\s*$`);
const LEAVE_RE = new RegExp(`\\]: (${NAME}) left the game\\s*$`);
const LIST_RE = /There are (\d+) of a max of (\d+) players online:\s*(.*)$/;

function touchPlayer(name, serverId, joined) {
  const p = knownPlayers[name] || (knownPlayers[name] = { firstSeen: Date.now(), sessions: 0 });
  p.lastSeen = Date.now(); p.lastServer = serverId;
  if (joined) p.sessions++;
  playersDirty = true;
}

function parseLine(s, raw) {
  const line = stripAnsi(raw);
  let m;
  if ((m = LIST_RE.exec(line))) {
    s.maxPlayers = +m[2];
    s.players = m[3].split(',').map((x) => x.trim()).filter(Boolean);
    s.players.forEach((n) => touchPlayer(n, s.cfg.id, false));
    return Date.now() < s.listPendingUntil; // true = masquer (sondage automatique)
  }
  if ((m = JOIN_RE.exec(line))) { if (!s.players.includes(m[1])) s.players.push(m[1]); touchPlayer(m[1], s.cfg.id, true); }
  else if ((m = LEAVE_RE.exec(line))) { s.players = s.players.filter((n) => n !== m[1]); touchPlayer(m[1], s.cfg.id, false); }
  return false;
}

function pollList(s) {
  if (s.status !== 'running' || s.cfg.type !== 'game') return;
  s.listPendingUntil = Date.now() + 4000;
  sendStdin(s, 'list');
}
setInterval(() => ordered().forEach(pollList), 60000);

/* ------------------------------------------------------------ cycle de vie */
function sendStdin(s, text) {
  if (!s.proc || !s.proc.stdin || !s.proc.stdin.writable) return false;
  try { s.proc.stdin.write(String(text).replace(/[\r\n]+/g, ' ') + '\n'); return true; } catch { return false; }
}

function killTree(s) {
  if (!s.proc) return;
  const pid = s.proc.pid;
  if (process.platform === 'win32') execFile('taskkill', ['/PID', String(pid), '/T', '/F'], () => {});
  else { try { process.kill(pid, 'SIGKILL'); } catch {} }
}

function probePort(port, timeout = 600) {
  return new Promise((resolve) => {
    const sock = net.connect({ host: '127.0.0.1', port });
    const done = (ok) => { sock.destroy(); resolve(ok); };
    sock.setTimeout(timeout, () => done(false));
    sock.once('connect', () => done(true));
    sock.once('error', () => done(false));
  });
}

function setStatus(s, status) {
  if (s.status === status) return;
  s.status = status;
  broadcast('status', snapshot());
}

function startService(id) {
  const s = get(id); const c = s.cfg;
  if (['starting', 'running', 'stopping'].includes(s.status)) return;
  clearTimeout(s.restartTimer);
  const dir = resolveDir(c.dir);
  if (!fs.existsSync(dir)) {
    pushLog(s, `[panel] Dossier introuvable : ${dir}`, 'error');
    s.desired = false; saveState(); setStatus(s, 'crashed');
    throw httpError(400, `Dossier introuvable : ${dir}`);
  }
  const [cmd, ...args] = tokenize(c.command);
  if (!cmd) throw httpError(400, 'Commande de démarrage vide');
  s.desired = true; saveState();
  s.stopping = false; s.players = []; s.lineBuf = '';
  pushLog(s, `[panel] Démarrage : ${c.command}`, 'panel');
  let proc;
  try {
    proc = spawn(cmd, args, { cwd: dir, windowsHide: true, shell: /\.(bat|cmd)$/i.test(cmd), stdio: ['pipe', 'pipe', 'pipe'] });
  } catch (e) {
    pushLog(s, `[panel] Échec du lancement : ${e.message}`, 'error'); setStatus(s, 'crashed'); return;
  }
  s.proc = proc; s.pid = proc.pid; s.startedAt = Date.now();
  setStatus(s, 'starting');
  const onData = (buf) => {
    s.lineBuf += buf.toString('utf8');
    let i;
    while ((i = s.lineBuf.search(/\r?\n/)) >= 0) {
      const line = s.lineBuf.slice(0, i); s.lineBuf = s.lineBuf.slice(s.lineBuf[i] === '\r' ? i + 2 : i + 1);
      if (!line.length) continue;
      if (!parseLine(s, line)) pushLog(s, line);
      try { analyze(liveCtx, s.cfg.id, line, Date.now(), true); } catch (e) { console.error('[sec]', e.message); }
    }
  };
  proc.stdout.on('data', onData); proc.stderr.on('data', onData);
  proc.stdin.on('error', () => {});
  proc.on('error', (e) => {
    pushLog(s, `[panel] Impossible de lancer « ${cmd} » : ${e.message}`, 'error');
    if (e.code === 'ENOENT') pushLog(s, '[panel] Vérifie que le programme est installé et dans le PATH, ou utilise un chemin complet.', 'error');
    if (!proc.pid) { s.desired = false; saveState(); onExit(s, proc, -1, null); setStatus(s, 'crashed'); } // jamais lancé : pas d'évènement exit
  });
  proc.on('exit', (code, signal) => onExit(s, proc, code, signal));

  // détection du "prêt" : port ouvert (si défini) sinon 5 s après le lancement
  const t0 = Date.now();
  const iv = setInterval(async () => {
    if (s.proc !== proc || s.status !== 'starting') return clearInterval(iv);
    const ok = c.port ? await probePort(c.port) : Date.now() - t0 > 5000;
    if (ok && s.proc === proc && s.status === 'starting') {
      clearInterval(iv); setStatus(s, 'running'); pushLog(s, '[panel] Service prêt.', 'panel');
      s.waiters.splice(0).forEach((w) => w(true));
      setTimeout(() => pollList(s), 3000);
    }
  }, 1500);
}

function onExit(s, proc, code, signal) {
  if (s.proc !== proc) return;
  clearTimeout(s.stopTimer);
  const wanted = s.stopping || !s.desired || shuttingDown;
  s.proc = null; s.pid = null; s.mem = 0; s.players = []; s.startedAt = null;
  s.lastExit = { code, signal, at: Date.now() };
  s.waiters.splice(0).forEach((w) => w(false));
  if (wanted) {
    pushLog(s, `[panel] Arrêté (code ${code}).`, 'panel');
    setStatus(s, 'stopped');
  } else {
    pushLog(s, `[panel] ⚠ Le processus s'est arrêté de façon inattendue (code ${code ?? signal}).`, 'error');
    setStatus(s, 'crashed');
    const now = Date.now();
    s.crashTimes = s.crashTimes.filter((t) => now - t < 10 * 60 * 1000).concat(now);
    if (s.cfg.autoRestart && s.crashTimes.length <= 5) {
      const delay = Math.min(30000, 3000 * s.crashTimes.length);
      s.restarts++;
      pushLog(s, `[panel] Redémarrage automatique dans ${Math.round(delay / 1000)} s (tentative ${s.crashTimes.length}/5).`, 'panel');
      s.restartTimer = setTimeout(() => { try { startService(s.cfg.id); } catch {} }, delay);
    } else if (s.cfg.autoRestart) {
      pushLog(s, '[panel] Trop de plantages en 10 min : redémarrage automatique abandonné.', 'error');
      s.desired = false; saveState();
    }
  }
}

function stopService(id, force) {
  const s = get(id); const c = s.cfg;
  clearTimeout(s.restartTimer);
  s.desired = false; if (!shuttingDown) saveState();
  if (!s.proc) { if (s.status === 'crashed') setStatus(s, 'stopped'); return Promise.resolve(); }
  s.stopping = true; setStatus(s, 'stopping');
  const done = new Promise((res) => s.waiters.push(() => res()));
  if (force) { pushLog(s, '[panel] Arrêt forcé.', 'panel'); killTree(s); return done; }
  pushLog(s, '[panel] Arrêt demandé…', 'panel');
  if (c.stopExec) exec(c.stopExec, { cwd: resolveDir(c.dir), windowsHide: true }, (err) => { if (err) pushLog(s, '[panel] stopExec : ' + err.message, 'error'); });
  else if (c.stopCommand) sendStdin(s, c.stopCommand);
  else killTree(s);
  clearTimeout(s.stopTimer);
  s.stopTimer = setTimeout(() => { if (s.proc) { pushLog(s, `[panel] Pas d'arrêt après ${c.stopTimeout} s : kill.`, 'error'); killTree(s); } }, c.stopTimeout * 1000);
  return done;
}

function waitReady(s, timeoutMs) {
  if (s.status === 'running') return Promise.resolve(true);
  return new Promise((res) => {
    const t = setTimeout(() => res(false), timeoutMs);
    s.waiters.push((ok) => { clearTimeout(t); res(ok); });
  });
}

async function runOperation(name, fn) {
  if (operation) throw httpError(409, `Opération en cours : ${operation.name}`);
  operation = { name, startedAt: Date.now() }; broadcast('status', snapshot());
  (async () => { try { await fn(); } catch (e) { console.error('[op]', e); } finally { operation = null; broadcast('status', snapshot()); } })();
}

const startAll = () => runOperation('Démarrage de tous les services', async () => {
  for (const s of ordered()) {
    if (['running', 'starting'].includes(s.status)) { await waitReady(s, 180000); continue; }
    try { startService(s.cfg.id); } catch { continue; }
    const ok = await waitReady(s, 180000);
    if (!ok) pushLog(s, '[panel] Toujours pas prêt après 3 min, on continue.', 'error');
  }
});
const stopAll = () => runOperation('Arrêt de tous les services', async () => {
  for (const s of ordered().reverse()) await stopService(s.cfg.id, false);
});
const restartService = async (id) => { await stopService(id, false); await new Promise((r) => setTimeout(r, 1500)); startService(id); };

/* ------------------------------------------------------- redémarrage quotidien */
const schedState = readJson(path.join(DATA, 'schedule.json'), { lastRestartDay: '' });
const warned = new Set();
const dayKey = (d) => d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate();
const targetSecs = () => { const [h, m] = String(config.panel.schedule.time).split(':').map(Number); return h * 3600 + m * 60; };

function nextRestartAt() {
  const sc = config.panel.schedule; if (!sc.enabled) return null;
  const d = new Date(); d.setHours(0, 0, 0, 0); d.setSeconds(targetSecs());
  if (d.getTime() <= Date.now() || schedState.lastRestartDay === dayKey(new Date())) d.setDate(d.getDate() + 1);
  return d.getTime();
}

function restartScheduled() {
  const targets = ordered().filter((s) => s.cfg.dailyRestart && s.proc);
  if (!targets.length) return;
  runOperation('Redémarrage quotidien', async () => {
    for (const s of [...targets].reverse()) await stopService(s.cfg.id, false);
    await new Promise((r) => setTimeout(r, 3000));
    for (const s of targets) {
      try { startService(s.cfg.id); } catch { continue; }
      await waitReady(s, 180000);
    }
  }).catch((e) => console.error('[schedule]', e.message));
}

function scheduleTick() {
  const sc = config.panel.schedule; if (!sc.enabled || shuttingDown) return;
  const now = new Date(); const secs = now.getHours() * 3600 + now.getMinutes() * 60 + now.getSeconds();
  const tgt = targetSecs(), today = dayKey(now);
  for (const w of sc.warnMinutes) {
    const at = tgt - w * 60, key = today + 'w' + w;
    if (secs - at >= 0 && secs - at < 45 && !warned.has(key)) {
      warned.add(key);
      ordered().filter((s) => s.cfg.dailyRestart && s.status === 'running').forEach((s) => sendStdin(s, `say Redemarrage automatique dans ${w} minute${w > 1 ? 's' : ''}`));
    }
  }
  if (secs - tgt >= 0 && secs - tgt < 60 && schedState.lastRestartDay !== today) {
    schedState.lastRestartDay = today; writeJson(path.join(DATA, 'schedule.json'), schedState);
    ordered().filter((s) => s.cfg.dailyRestart && s.status === 'running').forEach((s) => sendStdin(s, 'say Redemarrage en cours...'));
    try { restartScheduled(); } catch (e) { console.error('[schedule]', e.message); }
  }
}
setInterval(scheduleTick, 15000);

function saveState() {
  if (shuttingDown) return;
  writeJson(STATE_FILE, { desired: ordered().filter((s) => s.desired).map((s) => s.cfg.id) });
}

/* ---------------------------------------------------------------- métriques */
let cpuPrev = os.cpus(); let cpuPct = 0;
function sampleCpu() {
  const cur = os.cpus(); let idle = 0, total = 0;
  cur.forEach((c, i) => { const p = cpuPrev[i]; for (const k in c.times) total += c.times[k] - p.times[k]; idle += c.times.idle - p.times.idle; });
  cpuPrev = cur; cpuPct = total ? Math.round((1 - idle / total) * 100) : 0;
}
function sampleMem() {
  const live = ordered().filter((s) => s.pid);
  if (!live.length) return;
  if (process.platform === 'win32') {
    execFile('tasklist', ['/FO', 'CSV', '/NH'], { windowsHide: true, maxBuffer: 8e6 }, (err, out) => {
      if (err) return;
      const map = new Map();
      for (const row of out.split(/\r?\n/)) {
        const cols = row.match(/"([^"]*)"/g); if (!cols || cols.length < 5) continue;
        map.set(+cols[1].replace(/"/g, ''), +cols[4].replace(/\D/g, '') * 1024);
      }
      live.forEach((s) => { s.mem = map.get(s.pid) || 0; });
    });
  } else {
    execFile('ps', ['-o', 'pid=,rss=', '-p', live.map((s) => s.pid).join(',')], (err, out) => {
      if (err) return;
      const map = new Map(out.trim().split('\n').map((l) => l.trim().split(/\s+/).map(Number)).map(([p, r]) => [p, r * 1024]));
      live.forEach((s) => { s.mem = map.get(s.pid) || 0; });
    });
  }
}
setInterval(() => { sampleCpu(); sampleMem(); broadcast('status', snapshot()); }, 4000);

function snapshot() {
  return {
    operation,
    mod: { available: !!modState.available, checkedAt: modState.checkedAt, release: modState.release && modState.release.name },
    schedule: { enabled: config.panel.schedule.enabled, time: config.panel.schedule.time, next: nextRestartAt() },
    system: { cpu: cpuPct, memTotal: os.totalmem(), memFree: os.freemem(), uptime: os.uptime(), host: os.hostname() },
    services: ordered().map((s) => ({
      id: s.cfg.id, name: s.cfg.name, type: s.cfg.type, port: s.cfg.port, status: s.status, pid: s.pid,
      uptime: s.startedAt ? Date.now() - s.startedAt : 0, mem: s.mem, restarts: s.restarts,
      players: s.players, maxPlayers: s.maxPlayers, autoRestart: s.cfg.autoRestart, lastExit: s.lastExit, dailyRestart: s.cfg.dailyRestart,
    })),
  };
}

/* ---------------------------------------------------------------------- auth */
const hashPw = (pw, salt) => crypto.scryptSync(pw, salt, 64).toString('hex');
let auth = readJson(AUTH_FILE, null);
let sessions = readJson(SESSIONS_FILE, {});
const SESSION_TTL = 7 * 24 * 3600 * 1000;
const saveSessions = () => writeJson(SESSIONS_FILE, sessions);
const failures = new Map(); // ip -> {n, until}

function cookieOf(req) {
  const m = /(?:^|;\s*)ap_session=([a-f0-9]{64})/.exec(req.headers.cookie || '');
  return m ? m[1] : null;
}
function isAuthed(req) {
  const tok = cookieOf(req); const s = tok && sessions[tok];
  if (!s) return false;
  if (Date.now() > s.exp) { delete sessions[tok]; saveSessions(); return false; }
  return true;
}
function newSession(res) {
  const tok = crypto.randomBytes(32).toString('hex');
  sessions[tok] = { exp: Date.now() + SESSION_TTL }; saveSessions();
  res.setHeader('Set-Cookie', `ap_session=${tok}; HttpOnly; SameSite=Strict; Path=/; Max-Age=${SESSION_TTL / 1000}`);
}
function checkPassword(pw) {
  if (!auth) return false;
  const a = Buffer.from(hashPw(String(pw), auth.salt)); const b = Buffer.from(auth.hash);
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}
function setPassword(pw) {
  if (typeof pw !== 'string' || pw.length < 8) throw httpError(400, 'Mot de passe : 8 caractères minimum');
  const salt = crypto.randomBytes(16).toString('hex');
  auth = { salt, hash: hashPw(pw, salt) }; writeJson(AUTH_FILE, auth);
}

/* ------------------------------------------------------------------ fichiers */
const TEXT_EXT = /\.(properties|ya?ml|json|toml|txt|conf|cfg|ini|cnf|secret|bat|sh|md|mcmeta|json5|xml)$/i;
function safePath(id, rel) {
  const base = resolveDir(get(id).cfg.dir);
  const full = path.resolve(base, '.' + path.sep + (rel || ''));
  if (full !== base && !full.startsWith(base + path.sep)) throw httpError(400, 'Chemin interdit');
  return { base, full };
}

/* ------------------------------------------------------------------- joueurs */
const NAME_ONLY = new RegExp(`^${NAME}$`);
const clean = (s) => String(s || '').replace(/[\r\n]+/g, ' ').slice(0, 200);
function playerCommand(action, p, extra) {
  const e = clean(extra);
  switch (action) {
    case 'kick': return `kick ${p} ${e}`.trim();
    case 'ban': return `ban ${p} ${e}`.trim();
    case 'pardon': return `pardon ${p}`;
    case 'op': return `op ${p}`;
    case 'deop': return `deop ${p}`;
    case 'whitelist_add': return `whitelist add ${p}`;
    case 'whitelist_remove': return `whitelist remove ${p}`;
    case 'msg': return `tell ${p} ${e}`;
    case 'kill': return `kill ${p}`;
    case 'clear': return `clear ${p}`;
    case 'gamemode': if (!/^(survival|creative|adventure|spectator)$/.test(e)) throw httpError(400, 'Mode invalide'); return `gamemode ${e} ${p}`;
    case 'tp': if (!NAME_ONLY.test(e)) throw httpError(400, 'Cible invalide'); return `tp ${p} ${e}`;
    default: throw httpError(400, 'Action inconnue');
  }
}

function listJsonFile(id, file) {
  try { return JSON.parse(fs.readFileSync(path.join(resolveDir(get(id).cfg.dir), file), 'utf8')); } catch { return []; }
}

/* ------------------------------------------------------------------ sécurité */
// Détection basée sur les logs : elle repère des signaux (vitesse, vol, flood, multi-comptes, commandes sensibles…),
// elle ne remplace pas un anticheat qui surveille les paquets en jeu.
const SEC_FILE = path.join(DATA, 'security.json');
const sec = readJson(SEC_FILE, { events: [], playerIps: {} });
const seenKeys = new Set(sec.events.map((e) => e.k).filter(Boolean));
let secSeq = sec.events.reduce((m, e) => Math.max(m, e.id), 0);
let secDirty = false;
setInterval(() => { if (secDirty) { secDirty = false; writeJson(SEC_FILE, sec); } }, 15000);

const SEV = { low: 1, medium: 2, high: 3 };
const WEIGHTS = { speed: 1, speed_burst: 10, fly: 15, cmd: 2, chat_flood: 4, chat_repeat: 3, ip_flood: 10, multi_account: 8, bad_disconnect: 3, packet: 2, keyword: 5 };
const SENSITIVE_CMDS = new Set(['op', 'deop', 'gamemode', 'give', 'tp', 'teleport', 'summon', 'setblock', 'fill', 'clone', 'kill', 'effect', 'enchant', 'xp', 'experience', 'attribute', 'whitelist', 'ban', 'ban-ip', 'pardon', 'stop', 'execute', 'data', 'item', 'loot', 'mmorpg', 'setworldspawn', 'difficulty']);
const MEDIUM_CMDS = new Set(['op', 'deop', 'gamemode', 'give', 'execute', 'stop', 'data', 'mmorpg', 'item', 'loot']);
const PRIVATE_IP = /^(127\.|10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.|169\.254\.|::1$|0:0:0:0:0:0:0:1$|fe80|fc|fd)/i;

const MOVED_RE = new RegExp(`\\]: (${NAME}) moved (?:too quickly|wrongly)!(.*)$`);
const FLY_RE = new RegExp(`\\]: (?:Disconnecting )?(${NAME})(?: \\([^)]*\\))?(?:: .*Flying is not enabled| was kicked for floating too long!)`);
const LOGIN_RE = new RegExp(`\\]: (${NAME})\\[/([^\\]]+)\\] logged in with entity id`);
const PROXY_LOGIN_RE = new RegExp(`\\[connected player\\] (${NAME}) \\(/([^)]+)\\) has connected`);
const LOSTCONN_RE = new RegExp(`\\]: (${NAME}) lost connection: (.*)$`);
const CMD_RE = new RegExp(`\\]: (${NAME}) issued server command: /(\\S+)(.*)$`);
const CHAT_RE = new RegExp(`\\]: (?:\\[Not Secure\\] )?<(${NAME})> (.*)$`);
const PACKET_RE = /(Failed to handle packet|Exception handling packet|Invalid packet|Illegal characters in chat|Chat message validation failure|Invalid player data)/;
const BAD_REASON = /Invalid|Illegal|Flying|spam|Kicked|too many|packet|exceeded/i;
const NAME_IN = new RegExp(`(?:for|Disconnecting|player) (${NAME})`);

let keywordRes = [];
function compileKeywords() {
  keywordRes = ((config.panel.security && config.panel.security.keywords) || []).map((k) => { try { return new RegExp(k, 'i'); } catch { return null; } }).filter(Boolean);
}
compileKeywords();

const newSecCtx = () => ({ speed: new Map(), chat: new Map(), rep: new Map(), logins: new Map(), cool: new Map() });
const liveCtx = newSecCtx();
const dayStr = (t) => { const d = new Date(t); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; };

function windowPush(map, key, t, span, val) {
  const arr = (map.get(key) || []).filter((x) => t - x.t < span); arr.push({ t, v: val }); map.set(key, arr); return arr;
}
function cooled(ctx, key, t, ms) {
  const last = ctx.cool.get(key); if (last && t - last < ms) return false; ctx.cool.set(key, t); return true;
}
function addEvent(ev, live) {
  ev.id = ++secSeq; sec.events.push(ev); seenKeys.add(ev.k);
  if (sec.events.length > 5000) sec.events.splice(0, sec.events.length - 5000);
  secDirty = true;
  if (live && SEV[ev.sev] >= 2) broadcast('alert', ev);
}
function noteIp(player, ip) {
  if (!ip || PRIVATE_IP.test(ip)) return null;
  const list = sec.playerIps[player] || (sec.playerIps[player] = []);
  if (!list.includes(ip)) { list.push(ip); if (list.length > 20) list.shift(); secDirty = true; }
  return ip;
}
const namesOnIp = (ip) => Object.entries(sec.playerIps).filter(([, ips]) => ips.includes(ip)).map(([n]) => n);

function analyze(ctx, serverId, raw, t, live) {
  const line = stripAnsi(raw);
  const lineKey = `${serverId}|${dayStr(t)}|${line}`;
  if (!live && seenKeys.has(lineKey)) return; // le live voit chaque ligne ; le scan ignore ce qui est déjà enregistré
  const mk = (type, sev, player, detail, key) => addEvent({ t, server: serverId, player: player || '', type, sev, detail: String(detail).slice(0, 300), k: key || lineKey }, live);
  const derived = (type, id) => `${serverId}|${type}|${id}|${Math.floor(t / 60000)}`;
  let m;

  if ((m = MOVED_RE.exec(line))) {
    mk('speed', 'low', m[1], m[0].replace(/^\]: /, '').trim());
    const n = windowPush(ctx.speed, m[1], t, 60000).length;
    if (n >= 5 && cooled(ctx, 'sb' + m[1], t, 120000)) mk('speed_burst', 'high', m[1], `${n} déplacements trop rapides en 60 s (speed / téléportation / lag)`, derived('sb', m[1]));
  } else if ((m = FLY_RE.exec(line))) {
    mk('fly', 'high', m[1], 'Expulsé pour vol non autorisé');
  } else if ((m = LOGIN_RE.exec(line) || PROXY_LOGIN_RE.exec(line))) {
    const ip = noteIp(m[1], m[2].replace(/:\d+$/, ''));
    if (ip) {
      const n = new Set(windowPush(ctx.logins, ip, t, 60000, m[1]).map((x) => x.v)).size;
      const total = ctx.logins.get(ip).length;
      if (total >= 6 && cooled(ctx, 'if' + ip, t, 120000)) mk('ip_flood', 'high', m[1], `${total} connexions en 60 s depuis ${ip}`, derived('if', ip));
      const names = namesOnIp(ip);
      if (names.length >= 3 && cooled(ctx, 'ma' + ip + names.length, t, 6 * 3600000)) mk('multi_account', 'medium', m[1], `${names.length} comptes sur ${ip} : ${names.join(', ')}`, derived('ma', ip + names.length));
    }
  } else if ((m = LOSTCONN_RE.exec(line))) {
    if (BAD_REASON.test(m[2])) mk('bad_disconnect', 'low', m[1], m[2]);
  } else if ((m = CMD_RE.exec(line))) {
    const c = m[2].toLowerCase().replace(/^minecraft:/, '');
    if (SENSITIVE_CMDS.has(c)) mk('cmd', MEDIUM_CMDS.has(c) ? 'medium' : 'low', m[1], '/' + m[2] + m[3]);
  } else if ((m = CHAT_RE.exec(line))) {
    const p = m[1];
    if (windowPush(ctx.chat, p, t, 6000).length >= 8 && cooled(ctx, 'cf' + p, t, 60000)) mk('chat_flood', 'medium', p, '8 messages en moins de 6 s', derived('cf', p));
    const rep = windowPush(ctx.rep, p, t, 30000, m[2]).filter((x) => x.v === m[2]).length;
    if (rep >= 4 && cooled(ctx, 'cr' + p, t, 60000)) mk('chat_repeat', 'low', p, `Même message ×${rep} : ${m[2].slice(0, 80)}`, derived('cr', p));
  } else if (PACKET_RE.test(line)) {
    const n = NAME_IN.exec(line);
    mk('packet', 'low', n ? n[1] : '', line.replace(/^\[[^\]]*\] \[[^\]]*\] (\[[^\]]*\])?: ?/, ''));
  }
  for (const re of keywordRes) {
    if (re.test(line)) { const n = NAME_IN.exec(line); mk('keyword', 'medium', n ? n[1] : '', line.slice(0, 250), `${lineKey}|kw|${re.source}`); break; }
  }
}

function securitySummary(hours) {
  const since = Date.now() - hours * 3600000;
  const ev = sec.events.filter((e) => e.t >= since);
  const players = new Map();
  for (const e of ev) {
    if (!e.player) continue;
    const p = players.get(e.player) || { name: e.player, score: 0, counts: {}, last: 0, servers: new Set() };
    p.score += WEIGHTS[e.type] || 1; p.counts[e.type] = (p.counts[e.type] || 0) + 1; p.last = Math.max(p.last, e.t); p.servers.add(e.server);
    players.set(e.player, p);
  }
  const list = [...players.values()].map((p) => ({ ...p, servers: [...p.servers], level: p.score >= 30 ? 'high' : p.score >= 10 ? 'medium' : 'low' })).sort((a, b) => b.score - a.score);
  const ips = {};
  Object.entries(sec.playerIps).forEach(([n, l]) => l.forEach((ip) => { (ips[ip] ||= []).push(n); }));
  const groups = Object.entries(ips).filter(([, n]) => n.length >= 2).map(([ip, names]) => ({ ip, names })).sort((a, b) => b.names.length - a.names.length);
  return {
    events: ev.slice().reverse().slice(0, 1500),
    players: list, groups,
    stats: { total: ev.length, high: ev.filter((e) => e.sev === 'high').length, medium: ev.filter((e) => e.sev === 'medium').length, risky: list.filter((p) => p.level !== 'low').length },
  };
}

function scanHistoricalLogs() {
  const ctx = newSecCtx(); const before = sec.events.length; let files = 0;
  for (const s of ordered()) {
    if (s.cfg.type === 'db') continue;
    const dir = path.join(resolveDir(s.cfg.dir), 'logs');
    let names = []; try { names = fs.readdirSync(dir).filter((n) => /\.log(\.gz)?$/.test(n)); } catch { continue; }
    names = names.filter((n) => n !== 'debug.log').sort().slice(-20);
    for (const n of names) {
      const file = path.join(dir, n);
      let st; try { st = fs.statSync(file); } catch { continue; }
      if (st.size > 40e6) continue;
      let text;
      try { const buf = fs.readFileSync(file); text = (n.endsWith('.gz') ? zlib.gunzipSync(buf) : buf).toString('utf8'); } catch { continue; }
      const dm = /^(\d{4})-(\d{2})-(\d{2})/.exec(n);
      const base = dm ? new Date(+dm[1], +dm[2] - 1, +dm[3]) : new Date(st.mtime.getFullYear(), st.mtime.getMonth(), st.mtime.getDate());
      files++;
      for (const line of text.split(/\r?\n/)) {
        if (!line) continue;
        const tm = /^\[(\d\d):(\d\d):(\d\d)\]/.exec(line);
        const t = tm ? base.getTime() + ((+tm[1]) * 3600 + (+tm[2]) * 60 + (+tm[3])) * 1000 : st.mtimeMs;
        try { analyze(ctx, s.cfg.id, line, t, false); } catch {}
      }
    }
  }
  sec.events.sort((a, b) => a.t - b.t); secDirty = true;
  return { files, added: sec.events.length - before };
}

function searchLogs(q, id, isRegex, limit) {
  let re;
  if (isRegex) { if (q.length > 80) throw httpError(400, 'Regex trop longue'); try { re = new RegExp(q, 'i'); } catch { throw httpError(400, 'Regex invalide'); } }
  const needle = q.toLowerCase(); const out = [];
  const targets = id ? [get(id)] : ordered();
  for (const s of targets) {
    const cand = [path.join(resolveDir(s.cfg.dir), 'logs', 'latest.log'), path.join(LOGS, s.cfg.id + '.log')];
    const file = cand.find((f) => fs.existsSync(f)); if (!file) continue;
    const st = fs.statSync(file); const len = Math.min(st.size, 8e6);
    const fd = fs.openSync(file, 'r'); const buf = Buffer.alloc(len);
    try { fs.readSync(fd, buf, 0, len, st.size - len); } finally { fs.closeSync(fd); }
    for (const line of buf.toString('utf8').split(/\r?\n/)) {
      if (!line) continue;
      const clean = stripAnsi(line);
      if (re ? re.test(clean) : clean.toLowerCase().includes(needle)) { out.push({ id: s.cfg.id, l: clean.slice(0, 400) }); }
    }
  }
  return out.slice(-limit);
}

/* ----------------------------------------------------------------------- HTTP */
/* ------------------------------------------------------- mise à jour du mod */
const MOD_FILE = path.join(DATA, 'modupdate.json');
const MOD_DEFAULT = {
  repo: 'bananacheattv/MMORPG_MOD', tag: 'latest', assetPattern: '^aeloria-.*\\.jar$', replacePattern: '^(mmorpg|aeloria)-.*\\.jar$',
  targets: ['serveur', 'hub', 'mines', 'plaines'], modsDir: 'mods', checkOnBoot: true, token: '',
};
config.panel.modUpdate = Object.assign({}, MOD_DEFAULT, config.panel.modUpdate);
const modCfg = () => config.panel.modUpdate;
const modToken = () => process.env.AELORIA_GITHUB_TOKEN || modCfg().token || '';
const modState = Object.assign({ checkedAt: 0, available: false, release: null, error: null, targets: [], last: null }, readJson(MOD_FILE, {}));
const hashCache = new Map(); // fichier -> { key, sha }

function sha256File(file) {
  const st = fs.statSync(file); const key = st.size + ':' + st.mtimeMs;
  const c = hashCache.get(file); if (c && c.key === key) return Promise.resolve(c.sha);
  return new Promise((resolve, reject) => {
    const h = crypto.createHash('sha256');
    fs.createReadStream(file).on('data', (d) => h.update(d)).on('error', reject).on('end', () => { const sha = h.digest('hex'); hashCache.set(file, { key, sha }); resolve(sha); });
  });
}

async function ghFetch(url, accept) {
  const headers = { 'User-Agent': 'aeloria-panel', Accept: accept || 'application/vnd.github+json' };
  const t = modToken(); if (t) headers.Authorization = 'Bearer ' + t;
  const r = await fetch(url, { headers, redirect: 'follow', signal: AbortSignal.timeout(600000) });
  if (!r.ok) throw new Error(`GitHub a répondu ${r.status}` + (r.status === 404 && !t ? ' (dépôt privé ? ajoute un token GitHub dans les paramètres du mod)' : ''));
  return r;
}

async function fetchRelease() {
  const mc = modCfg();
  const r = await (await ghFetch(`https://api.github.com/repos/${mc.repo}/releases/tags/${encodeURIComponent(mc.tag)}`)).json();
  const re = new RegExp(mc.assetPattern);
  const a = (r.assets || []).find((x) => re.test(x.name));
  if (!a) throw new Error(`Aucun fichier ne correspond à « ${mc.assetPattern} » dans la release « ${mc.tag} »`);
  return { name: r.name || r.tag_name, tag: r.tag_name, publishedAt: r.published_at, asset: { name: a.name, size: a.size, url: a.url, sha: /^sha256:([0-9a-f]{64})$/.exec(a.digest || '')?.[1] || '' } };
}

const modsPath = (s) => {
  const base = resolveDir(s.cfg.dir); const dir = path.resolve(base, modCfg().modsDir || 'mods');
  if (dir !== base && !dir.startsWith(base + path.sep)) throw new Error('Dossier mods invalide');
  return dir;
};
const modTargets = () => modCfg().targets.map((id) => services.get(id)).filter(Boolean);

async function checkMod() {
  try {
    const rel = await fetchRelease(); const re = new RegExp(modCfg().replacePattern);
    const targets = [];
    for (const s of modTargets()) {
      const dir = modsPath(s); const files = [];
      try { for (const n of fs.readdirSync(dir)) if (re.test(n)) files.push({ name: n, sha: await sha256File(path.join(dir, n)) }); } catch {}
      const ok = rel.asset.sha ? files.length === 1 && files[0].sha === rel.asset.sha : files.length === 1 && files[0].name === rel.asset.name;
      targets.push({ id: s.cfg.id, name: s.cfg.name, files: files.map((f) => f.name), upToDate: ok, running: !!s.proc });
    }
    Object.assign(modState, { checkedAt: Date.now(), release: rel, targets, error: null, available: targets.some((t) => !t.upToDate) });
  } catch (e) {
    Object.assign(modState, { checkedAt: Date.now(), error: e.message });
  }
  broadcast('status', snapshot());
  return modState;
}

async function installMod(restart) {
  const last = modState.last = { at: Date.now(), ok: null, steps: [], error: null };
  const step = (t) => { last.steps.push(t); broadcast('status', snapshot()); };
  const targets = modTargets(); const running = targets.filter((s) => s.proc); let stopped = false;
  try {
    const rel = await fetchRelease(); const a = rel.asset;
    step(`Téléchargement de ${a.name} (${fmtMb(a.size)})…`);
    const tmpDir = path.join(DATA, 'modupdate'); fs.mkdirSync(tmpDir, { recursive: true });
    const part = path.join(tmpDir, a.name + '.part');
    const resp = await ghFetch(a.url, 'application/octet-stream');
    const buf = Buffer.from(await resp.arrayBuffer());
    if (a.size && buf.length !== a.size) throw new Error(`Taille reçue ${buf.length} ≠ ${a.size} attendus`);
    if (buf.subarray(0, 2).toString() !== 'PK') throw new Error('Le fichier téléchargé n\'est pas un jar valide');
    if (a.sha && crypto.createHash('sha256').update(buf).digest('hex') !== a.sha) throw new Error('Empreinte SHA-256 incorrecte');
    fs.writeFileSync(part, buf);
    step('Téléchargement vérifié (taille, format, SHA-256).');

    if (running.length) {
      step('Arrêt de : ' + running.map((s) => s.cfg.name).join(', '));
      running.forEach((s) => sendStdin(s, 'say [Panel] Mise à jour du mod : redémarrage dans 15 s'));
      await new Promise((r) => setTimeout(r, 15000));
      for (const s of [...running].reverse()) await stopService(s.cfg.id, false);
      stopped = true;
    }

    const re = new RegExp(modCfg().replacePattern); const stamp = new Date().toISOString().replace(/[:.]/g, '-');
    for (const s of targets) {
      const dir = modsPath(s); fs.mkdirSync(dir, { recursive: true });
      const olds = fs.readdirSync(dir).filter((n) => re.test(n));
      const bk = path.join(dir, '..', 'mods_backup', stamp);
      const fresh = path.join(dir, a.name + '.new');
      try {
        if (olds.length) fs.mkdirSync(bk, { recursive: true });
        for (const n of olds) fs.copyFileSync(path.join(dir, n), path.join(bk, n));
        fs.copyFileSync(part, fresh);
        for (const n of olds) fs.unlinkSync(path.join(dir, n));
        fs.renameSync(fresh, path.join(dir, a.name));
        step(`${s.cfg.name} : ${olds.length ? olds.join(', ') + ' sauvegardé, ' : ''}${a.name} installé.`);
      } catch (e) {
        try { fs.rmSync(fresh, { force: true }); for (const n of olds) if (!fs.existsSync(path.join(dir, n))) fs.copyFileSync(path.join(bk, n), path.join(dir, n)); } catch {}
        throw new Error(`${s.cfg.name} : ${e.message} (ancien jar restauré)`);
      }
    }
    fs.rmSync(part, { force: true });
    last.ok = true;
  } catch (e) {
    last.ok = false; last.error = e.message; step('Échec : ' + e.message);
  }
  if (stopped) {
    step('Redémarrage des serveurs…');
    for (const s of running) { try { startService(s.cfg.id); } catch {} await waitReady(s, 180000); }
  }
  step(last.ok ? 'Terminé.' : 'Terminé avec erreur.');
  try { writeJson(MOD_FILE, { last }); } catch {}
  await checkMod();
}
const fmtMb = (b) => (b / 1048576).toFixed(1) + ' Mo';

const MIME = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.svg': 'image/svg+xml', '.png': 'image/png', '.ico': 'image/x-icon' };
const PUBLIC = path.join(ROOT, 'public');

function readBody(req) {
  return new Promise((resolve, reject) => {
    let n = 0; const chunks = [];
    req.on('data', (c) => { n += c.length; if (n > 3e6) { reject(httpError(413, 'Corps trop gros')); req.destroy(); } else chunks.push(c); });
    req.on('end', () => { try { resolve(chunks.length ? JSON.parse(Buffer.concat(chunks).toString('utf8')) : {}); } catch { reject(httpError(400, 'JSON invalide')); } });
    req.on('error', reject);
  });
}
const send = (res, code, obj) => { res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' }); res.end(JSON.stringify(obj)); };

async function api(req, res, url) {
  const p = url.pathname; const m = req.method;
  const ip = req.socket.remoteAddress;

  if (p === '/api/state' && m === 'GET') return send(res, 200, { setup: !auth, authed: isAuthed(req) });
  if (p === '/api/setup' && m === 'POST') {
    if (auth) throw httpError(403, 'Déjà configuré');
    const b = await readBody(req); setPassword(b.password); newSession(res); return send(res, 200, { ok: true });
  }
  if (p === '/api/login' && m === 'POST') {
    const f = failures.get(ip);
    if (f && f.until > Date.now()) throw httpError(429, 'Trop d\'essais, réessaie dans quelques secondes');
    const b = await readBody(req);
    if (!checkPassword(b.password)) {
      const n = (f ? f.n : 0) + 1; failures.set(ip, { n, until: Date.now() + Math.min(60000, 1000 * 2 ** n) });
      throw httpError(401, 'Mot de passe incorrect');
    }
    failures.delete(ip); newSession(res); return send(res, 200, { ok: true });
  }
  if (!isAuthed(req)) throw httpError(401, 'Non connecté');
  if (m !== 'GET' && !/^application\/json/.test(req.headers['content-type'] || '')) throw httpError(415, 'JSON requis');

  if (p === '/api/logout' && m === 'POST') {
    const t = cookieOf(req); delete sessions[t]; saveSessions();
    res.setHeader('Set-Cookie', 'ap_session=; Max-Age=0; Path=/'); return send(res, 200, { ok: true });
  }
  if (p === '/api/password' && m === 'POST') {
    const b = await readBody(req);
    if (!checkPassword(b.current)) throw httpError(403, 'Mot de passe actuel incorrect');
    setPassword(b.next); sessions = {}; newSession(res); return send(res, 200, { ok: true });
  }
  if (p === '/api/stream') {
    res.writeHead(200, { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-store', Connection: 'keep-alive' });
    res.write(`event: status\ndata: ${JSON.stringify(snapshot())}\n\n`);
    subscribers.add(res);
    const hb = setInterval(() => { try { res.write(': hb\n\n'); } catch {} }, 15000);
    req.on('close', () => { clearInterval(hb); subscribers.delete(res); });
    return;
  }
  if (p === '/api/status') return send(res, 200, snapshot());
  if (p === '/api/start-all' && m === 'POST') { await startAll(); return send(res, 200, { ok: true }); }
  if (p === '/api/stop-all' && m === 'POST') { await stopAll(); return send(res, 200, { ok: true }); }

  let mm;
  if ((mm = /^\/api\/services\/([a-z0-9_-]+)\/(start|stop|restart|kill|command|logs)$/.exec(p))) {
    const s = get(mm[1]); const act = mm[2];
    if (act === 'logs') return send(res, 200, { logs: s.logs.slice(-1500) });
    if (act === 'start') { startService(s.cfg.id); return send(res, 200, { ok: true }); }
    if (act === 'stop') { stopService(s.cfg.id, false); return send(res, 200, { ok: true }); }
    if (act === 'kill') { stopService(s.cfg.id, true); return send(res, 200, { ok: true }); }
    if (act === 'restart') { restartService(s.cfg.id).catch((e) => pushLog(s, '[panel] ' + e.message, 'error')); return send(res, 200, { ok: true }); }
    if (act === 'command') {
      const b = await readBody(req); const cmd = clean(b.command).trim();
      if (!cmd) throw httpError(400, 'Commande vide');
      if (!sendStdin(s, cmd)) throw httpError(409, 'Le service n\'est pas démarré');
      pushLog(s, '> ' + cmd, 'cmd'); return send(res, 200, { ok: true });
    }
  }

  if (p === '/api/config') {
    if (m === 'GET') return send(res, 200, { services: config.services, panel: Object.assign({}, config.panel, { modUpdate: undefined }), root: ROOT });
    if (m === 'PUT') {
      const b = await readBody(req);
      if (!Array.isArray(b.services)) throw httpError(400, 'services manquant');
      applyConfigServices(b.services);
      if (b.panel) {
        config.panel.resumeOnBoot = !!b.panel.resumeOnBoot;
        if (b.panel.security && Array.isArray(b.panel.security.keywords)) {
          const kws = b.panel.security.keywords.map((k) => String(k).trim()).filter(Boolean).slice(0, 30);
          for (const k of kws) { if (k.length > 80) throw httpError(400, 'Mot-clé trop long : ' + k.slice(0, 20)); try { new RegExp(k); } catch { throw httpError(400, 'Regex invalide : ' + k); } }
          config.panel.security.keywords = kws; compileKeywords();
        }
        const sc = b.panel.schedule;
        if (sc) {
          if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(sc.time || '')) throw httpError(400, 'Heure invalide (HH:MM)');
          config.panel.schedule = { enabled: !!sc.enabled, time: sc.time, warnMinutes: [5, 1] };
        }
      }
      writeJson(CONFIG_FILE, config); broadcast('status', snapshot()); return send(res, 200, { ok: true });
    }
  }

  if (p === '/api/files' && m === 'GET') {
    const { base, full } = safePath(url.searchParams.get('id'), url.searchParams.get('path'));
    const entries = fs.readdirSync(full, { withFileTypes: true }).map((d) => {
      let size = 0; try { size = d.isFile() ? fs.statSync(path.join(full, d.name)).size : 0; } catch {}
      return { name: d.name, dir: d.isDirectory(), size, editable: d.isFile() && TEXT_EXT.test(d.name) && size < 2e6 };
    }).sort((a, b) => (b.dir - a.dir) || a.name.localeCompare(b.name));
    return send(res, 200, { path: path.relative(base, full).split(path.sep).join('/'), entries });
  }
  if (p === '/api/file') {
    const { full } = safePath(url.searchParams.get('id'), url.searchParams.get('path'));
    if (!TEXT_EXT.test(full)) throw httpError(400, 'Type de fichier non éditable');
    if (m === 'GET') {
      if (fs.statSync(full).size > 2e6) throw httpError(413, 'Fichier trop gros');
      return send(res, 200, { content: fs.readFileSync(full, 'utf8') });
    }
    if (m === 'PUT') {
      const b = await readBody(req); if (typeof b.content !== 'string') throw httpError(400, 'content manquant');
      if (fs.existsSync(full)) fs.copyFileSync(full, full + '.bak');
      fs.writeFileSync(full, b.content); return send(res, 200, { ok: true });
    }
  }

  if (p === '/api/players' && m === 'GET') {
    const games = ordered().filter((s) => s.cfg.type === 'game');
    const bans = {}, ops = {};
    games.forEach((s) => {
      bans[s.cfg.id] = listJsonFile(s.cfg.id, 'banned-players.json').map((x) => ({ name: x.name, reason: x.reason, created: x.created }));
      ops[s.cfg.id] = listJsonFile(s.cfg.id, 'ops.json').map((x) => x.name);
    });
    const online = [];
    games.forEach((s) => s.players.forEach((n) => online.push({ name: n, server: s.cfg.id })));
    return send(res, 200, { online, known: knownPlayers, bans, ops, servers: games.map((s) => ({ id: s.cfg.id, name: s.cfg.name, status: s.status })) });
  }
  if (p === '/api/players/action' && m === 'POST') {
    const b = await readBody(req);
    if (!NAME_ONLY.test(b.player || '')) throw httpError(400, 'Pseudo invalide');
    const cmd = playerCommand(b.action, b.player, b.extra);
    const targets = b.targets === 'all' ? ordered().filter((s) => s.cfg.type === 'game') : (Array.isArray(b.targets) ? b.targets : []).map(get);
    if (!targets.length) throw httpError(400, 'Aucun serveur ciblé');
    targets.forEach((s) => { if (sendStdin(s, cmd)) pushLog(s, '> ' + cmd, 'cmd'); });
    return send(res, 200, { ok: true, command: cmd, sent: targets.filter((s) => s.proc).map((s) => s.cfg.id), skipped: targets.filter((s) => !s.proc).map((s) => s.cfg.id) });
  }
  if (p === '/api/mod' && m === 'GET') {
    const { token, ...mc } = modCfg();
    return send(res, 200, { cfg: mc, hasToken: !!modToken(), tokenFromEnv: !!process.env.AELORIA_GITHUB_TOKEN, services: ordered().filter((s) => s.cfg.type === 'game').map((s) => ({ id: s.cfg.id, name: s.cfg.name })), state: modState });
  }
  if (p === '/api/mod/check' && m === 'POST') return send(res, 200, await checkMod());
  if (p === '/api/mod/update' && m === 'POST') {
    const b = await readBody(req);
    if (operation) throw httpError(409, `Opération en cours : ${operation.name}`);
    if (!modTargets().length) throw httpError(400, 'Aucun serveur cible configuré');
    const running = modTargets().filter((s) => s.proc);
    if (running.length && !b.restart) throw httpError(409, 'Serveurs en marche (' + running.map((s) => s.cfg.name).join(', ') + ') : le jar est verrouillé, confirme le redémarrage');
    await runOperation('Mise à jour du mod', () => installMod(!!b.restart));
    return send(res, 200, { ok: true });
  }
  if (p === '/api/mod/config' && m === 'PUT') {
    const b = await readBody(req); const mc = modCfg();
    const repo = String(b.repo || '').trim(), tag = String(b.tag || '').trim(), modsDir = String(b.modsDir || 'mods').trim();
    if (!/^[\w.-]+\/[\w.-]+$/.test(repo)) throw httpError(400, 'Dépôt invalide (propriétaire/nom)');
    if (!/^[\w.-]+$/.test(tag)) throw httpError(400, 'Tag invalide');
    if (path.isAbsolute(modsDir) || modsDir.split(/[\\/]/).includes('..')) throw httpError(400, 'Dossier mods invalide');
    for (const k of ['assetPattern', 'replacePattern']) { try { new RegExp(b[k]); } catch { throw httpError(400, 'Regex invalide : ' + k); } }
    const targets = (Array.isArray(b.targets) ? b.targets : []).filter((id) => services.has(id));
    Object.assign(mc, { repo, tag, modsDir, assetPattern: String(b.assetPattern), replacePattern: String(b.replacePattern), targets, checkOnBoot: !!b.checkOnBoot });
    if (typeof b.token === 'string') mc.token = b.token.trim();
    writeJson(CONFIG_FILE, config); return send(res, 200, { ok: true });
  }
  if (p === '/api/security' && m === 'GET') return send(res, 200, securitySummary(Math.min(720, Math.max(1, +url.searchParams.get('hours') || 24))));
  if (p === '/api/security/scan' && m === 'POST') return send(res, 200, scanHistoricalLogs());
  if (p === '/api/security/clear' && m === 'POST') { sec.events = []; seenKeys.clear(); secDirty = true; return send(res, 200, { ok: true }); }
  if (p === '/api/security/search' && m === 'GET') {
    const q = (url.searchParams.get('q') || '').trim(); if (q.length < 2) throw httpError(400, 'Recherche trop courte');
    return send(res, 200, { lines: searchLogs(q, url.searchParams.get('id') || '', url.searchParams.get('regex') === '1', 400) });
  }
  if (p === '/api/broadcast' && m === 'POST') {
    const b = await readBody(req); const msg = clean(b.message).trim();
    if (!msg) throw httpError(400, 'Message vide');
    ordered().filter((s) => s.cfg.type === 'game').forEach((s) => sendStdin(s, 'say ' + msg));
    return send(res, 200, { ok: true });
  }
  throw httpError(404, 'Route inconnue');
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://x');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:");
  try {
    if (url.pathname.startsWith('/api/')) return await api(req, res, url);
    let rel = url.pathname === '/' ? '/index.html' : decodeURIComponent(url.pathname);
    const file = path.join(PUBLIC, path.normalize(rel));
    if (!file.startsWith(PUBLIC + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) { res.writeHead(404); return res.end('404'); }
    res.writeHead(200, { 'Content-Type': MIME[path.extname(file)] || 'application/octet-stream', 'Cache-Control': 'no-cache' });
    fs.createReadStream(file).pipe(res);
  } catch (e) {
    if (res.headersSent) return res.end();
    send(res, e.code >= 400 && e.code < 600 ? e.code : 500, { error: e.message });
    if (!(e.code >= 400 && e.code < 600)) console.error('[http]', e);
  }
});

server.on('error', (e) => { console.error('[panel] Erreur serveur HTTP :', e.message); if (e.code === 'EADDRINUSE') process.exit(1); });
server.listen(config.panel.port, config.panel.host, () => {
  console.log(`\n  ⚔  Aeloria Panel  →  http://${config.panel.host === '0.0.0.0' ? 'localhost' : config.panel.host}:${config.panel.port}\n`);
  sampleCpu();
  if (modCfg().checkOnBoot) {
    setTimeout(() => checkMod().catch(() => {}), 3000);
    setInterval(() => { if (!operation) checkMod().catch(() => {}); }, 6 * 3600 * 1000);
  }
  const st = readJson(STATE_FILE, { desired: [] });
  if (config.panel.resumeOnBoot && st.desired.length) {
    console.log('[panel] Reprise des services qui tournaient :', st.desired.join(', '));
    runOperation('Reprise après redémarrage du panel', async () => {
      for (const s of ordered()) {
        if (!st.desired.includes(s.cfg.id)) continue;
        try { startService(s.cfg.id); } catch { continue; }
        await waitReady(s, 180000);
      }
    }).catch(() => {});
  }
});

/* --------------------------------------------------------------- extinction */
async function shutdown() {
  if (shuttingDown) return; shuttingDown = true;
  console.log('\n[panel] Arrêt propre des services…');
  for (const s of ordered().reverse()) { try { await stopService(s.cfg.id, false); } catch {} }
  writeJson(PLAYERS_FILE, knownPlayers);
  process.exit(0);
}
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
