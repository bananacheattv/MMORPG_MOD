'use strict';
// Local panel installer. Copies the runtime, never an existing world or player data.
const fs = require('node:fs');
const path = require('node:path');
const net = require('node:net');

async function main() {
  const [rootArg, jarArg] = process.argv.slice(2);
  if (!rootArg || !jarArg) throw Error('Usage: node tools/Add-AeloriaOpenworldServer.cjs <panel-root> <mod.jar>');
  const root = fs.realpathSync(rootArg);
  const jar = fs.realpathSync(jarArg);
  const id = 'openworld', port = 25576;
  const within = (...parts) => {
    const result = path.resolve(root, ...parts);
    if (!result.startsWith(root + path.sep)) throw Error('Path outside panel: ' + result);
    return result;
  };
  const readJson = file => JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
  const writeJson = (file, value) => fs.writeFileSync(file, JSON.stringify(value, null, 2) + '\n');
  const config = readJson(within('config.json'));
  const template = config.services.find(s => s.id === 'serveur' && s.type === 'game');
  if (!template) throw Error('Main game server missing');
  if (config.services.some(s => s.id === id || s.port === port)) throw Error('Openworld or port already configured');
  const source = within(template.dir), target = within('servers', id);
  if (fs.existsSync(target)) throw Error('Target directory already exists; nothing overwritten');
  if (fs.readFileSync(jar).subarray(0, 2).toString() !== 'PK') throw Error('Invalid mod JAR');
  const sessions = readJson(within('data', 'sessions.json'));
  const token = Object.keys(sessions).find(k => sessions[k].exp > Date.now());
  if (!token) throw Error('Open a session in the local panel before installation');
  const base = 'http://127.0.0.1:' + config.panel.port;
  const api = async (route, method = 'GET', body) => {
    const response = await fetch(base + route, { method,
      headers: { Cookie: 'ap_session=' + token, 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined });
    if (!response.ok) throw Error(route + ': HTTP ' + response.status);
    return response.json();
  };
  const status = await api('/api/status');
  if (status.operation) throw Error('A panel operation is running');
  // Binding briefly also catches listeners omitted from the panel configuration.
  await new Promise((resolve, reject) => {
    const probe = net.createServer();
    probe.once('error', reject);
    probe.listen({ host: '127.0.0.1', port, exclusive: true }, () => probe.close(resolve));
  });
  const proxy = within(config.services.find(s => s.type === 'proxy').dir, 'velocity.toml');
  const originalProxy = fs.readFileSync(proxy, 'utf8');
  if (!/^\[servers\]\s*$/m.test(originalProxy) || /^\s*openworld\s*=/m.test(originalProxy)) throw Error('Unexpected Velocity servers section');
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const backup = within('backups', 'openworld-' + stamp);
  fs.mkdirSync(backup, { recursive: true });
  fs.copyFileSync(within('config.json'), path.join(backup, 'config.json'));
  fs.copyFileSync(proxy, path.join(backup, 'velocity.toml'));
  fs.mkdirSync(target);
  for (const folder of ['libraries', 'config', 'defaultconfigs', 'mods']) {
    const from = path.join(source, folder);
    if (!fs.existsSync(from)) continue;
    fs.cpSync(from, path.join(target, folder), { recursive: true, filter: file => {
      const relative = path.relative(source, file).replaceAll('\\', '/');
      if (relative === 'config/worldedit/schematics' || relative.startsWith('config/worldedit/schematics/')) return false;
      if (relative.startsWith('mods/') && /^(aeloria|mmorpg)-.*\.jar$/i.test(path.basename(file))) return false;
      return !/\.(log|bak)$/.test(relative);
    }});
  }
  for (const file of ['eula.txt', 'ops.json', 'whitelist.json', 'banned-players.json', 'banned-ips.json']) {
    if (fs.existsSync(path.join(source, file))) fs.copyFileSync(path.join(source, file), path.join(target, file));
  }
  fs.copyFileSync(jar, path.join(target, 'mods', path.basename(jar)));
  fs.writeFileSync(path.join(target, 'user_jvm_args.txt'), '# Aeloria Openworld : 4 Gio minimum, 16 Gio maximum\n-Xms4G\n-Xmx16G\n');
  let properties = fs.readFileSync(path.join(source, 'server.properties'), 'utf8');
  const settings = { 'server-ip': '127.0.0.1', 'server-port': port, 'online-mode': false,
    'level-name': 'world', 'level-type': 'minecraft\\:normal', 'level-seed': '', 'generator-settings': '{}',
    gamemode: 'survival', 'spawn-protection': 0, 'pause-when-empty-seconds': 0,
    'enable-rcon': false, 'enable-query': false, 'management-server-enabled': false,
    'query.port': port, motd: 'Aeloria - Openworld' };
  for (const [key, value] of Object.entries(settings)) {
    const pattern = new RegExp('^' + key.replaceAll('.', '\\.') + '=.*$', 'm');
    const line = key + '=' + value;
    properties = pattern.test(properties) ? properties.replace(pattern, () => line) : properties + '\n' + line + '\n';
  }
  fs.writeFileSync(path.join(target, 'server.properties'), properties);
  const networkFile = path.join(target, 'config', 'mmorpg', 'network.json');
  const network = readJson(networkFile);
  if (!network.enabled || !network.sharedDirectory) throw Error('Template is not part of the network');
  // Relative shared paths are resolved from the original server before cloning.
  network.sharedDirectory = fs.realpathSync(path.resolve(source, network.sharedDirectory)).replaceAll('\\', '/');
  Object.assign(network, { role: 'OPENWORLD', openworld: id });
  writeJson(networkFile, network);
  const service = { ...template, id, name: 'Openworld', dir: 'servers/openworld', port,
    order: Math.max(...config.services.filter(s => s.type === 'game').map(s => s.order)) + 1,
    autoRestart: true, dailyRestart: false };
  const services = [...config.services, service].map(s => s.type === 'proxy' && s.order <= service.order ? { ...s, order: service.order + 1 } : s);
  // Setup for this server only; existing links and world directories are untouched.
  const sharing = require(within('worldedit.js')).createWorldEdit({ root, services: () => [service] }).list();
  await api('/api/config', 'PUT', { services });
  const { cfg } = await api('/api/mod');
  await api('/api/mod/config', 'PUT', { ...cfg, targets: [...new Set([...cfg.targets, id])] });
  fs.writeFileSync(proxy, originalProxy.replace(/^(\[servers\][^\r\n]*\r?\n)/m, '$1\topenworld = "127.0.0.1:' + port + '"\n'));
  console.log(JSON.stringify({ added: id, port, memory: '4G-16G', directory: target,
    sharedSchematics: sharing.servers[0].shared, backup, started: false }));
  console.log('Start Openworld through the panel, then run velocity reload in the proxy console.');
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
