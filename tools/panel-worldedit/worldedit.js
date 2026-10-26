'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

const error = (code, message) => Object.assign(new Error(message), { code });
const MAX_BYTES = 32 * 1024 * 1024;
function validName(name) {
  if (typeof name !== 'string' || name.length > 120 || !/^[a-zA-Z0-9][a-zA-Z0-9_. -]*\.(schem|schematic|nbt)$/i.test(name) || name.includes('..'))
    throw error(400, 'Nom invalide : lettres, chiffres, espaces, tirets et extension .schem, .schematic ou .nbt.');
  return name;
}
function createWorldEdit({ root, services }) {
  const shared = path.join(root, 'shared', 'schematics');
  fs.mkdirSync(shared, { recursive: true });
  const targets = () => services().filter(s => s.type === 'game').map(s => {
    const directory = path.resolve(root, s.dir);
    if (!directory.startsWith(path.resolve(root) + path.sep)) throw error(400, 'Serveur situé hors du dossier du panel.');
    return { id: s.id, name: s.name, directory, link: path.join(directory, 'config', 'worldedit', 'schematics') };
  });
  function setup() {
    for (const target of targets()) {
      fs.mkdirSync(path.dirname(target.link), { recursive: true });
      const properties = path.join(path.dirname(target.link), 'worldedit.properties');
      const previous = fs.existsSync(properties) ? fs.readFileSync(properties, 'utf8') : '';
      let settings = previous;
      for (const [key, value] of [['allow-symbolic-links', 'true'], ['schematic-save-dir', 'schematics']]) {
        const pattern = new RegExp('^' + key + '=.*$', 'm');
        settings = pattern.test(settings) ? settings.replace(pattern, key + '=' + value) : settings + '\n' + key + '=' + value + '\n';
      }
      if (settings !== previous) {
        if (previous && !fs.existsSync(properties + '.before-sharing')) fs.copyFileSync(properties, properties + '.before-sharing');
        fs.writeFileSync(properties, settings);
      }
      if (fs.existsSync(target.link)) {
        if (fs.realpathSync(target.link) === fs.realpathSync(shared)) continue;
        if (fs.lstatSync(target.link).isSymbolicLink()) throw error(409, 'Lien WorldEdit existant différent pour ' + target.id);
        const entries = fs.readdirSync(target.link, { withFileTypes: true });
        // Validate every migration before changing the directory.
        for (const entry of entries) {
          if (!entry.isFile()) throw error(409, 'Sous-dossier WorldEdit à migrer manuellement : ' + target.id);
          validName(entry.name);
          const dest = path.join(shared, entry.name), from = path.join(target.link, entry.name);
          if (fs.existsSync(dest) && !fs.readFileSync(dest).equals(fs.readFileSync(from))) throw error(409, 'Deux schématics différents portent le nom ' + entry.name);
        }
        for (const entry of entries) {
          const dest = path.join(shared, entry.name);
          if (!fs.existsSync(dest)) fs.copyFileSync(path.join(target.link, entry.name), dest, fs.constants.COPYFILE_EXCL);
        }
        // Preserve the original directory. Never recursively delete or move a computed tree.
        fs.renameSync(target.link, target.link + '.before-sharing-' + Date.now());
      }
      fs.symlinkSync(shared, target.link, process.platform === 'win32' ? 'junction' : 'dir');
    }
  }
  setup();
  function list() {
    return { directory: shared, maxBytes: MAX_BYTES,
      servers: targets().map(t => ({ id: t.id, name: t.name, shared: fs.existsSync(t.link) && fs.realpathSync(t.link) === fs.realpathSync(shared),
        installed: fs.existsSync(path.join(t.directory, 'mods')) && fs.readdirSync(path.join(t.directory, 'mods')).some(n => /^worldedit.*\.jar$/i.test(n)) })),
      files: fs.readdirSync(shared, { withFileTypes: true }).filter(e => e.isFile() && /\.(schem|schematic|nbt)$/i.test(e.name)).map(e => {
        const stat = fs.statSync(path.join(shared, e.name));return { name: e.name, size: stat.size, modified: stat.mtimeMs };
      }).sort((a,b) => a.name.localeCompare(b.name)) };
  }
  async function upload(req, name) {
    validName(name);
    const dest = path.join(shared, name);
    if (fs.existsSync(dest)) throw error(409, 'Ce nom existe déjà. Renomme le fichier pour conserver les deux versions.');
    if (Number(req.headers['content-length']) > MAX_BYTES) throw error(413, 'Fichier trop volumineux (32 Mo maximum).');
    let size = 0; const chunks = [];
    for await (const chunk of req) { size += chunk.length; if (size > MAX_BYTES) throw error(413, 'Fichier trop volumineux (32 Mo maximum).'); chunks.push(chunk); }
    const bytes = Buffer.concat(chunks);
    if (!bytes.length || !(bytes[0] === 10 || bytes[0] === 0x1f && bytes[1] === 0x8b)) throw error(400, 'Le fichier doit contenir des données NBT ou NBT compressées.');
    const tmp = path.join(shared, '.upload-' + crypto.randomUUID());
    try {
      fs.writeFileSync(tmp, bytes, { flag: 'wx' });
      // Exclusive, atomic publication: concurrent uploads never overwrite an existing schematic.
      try { fs.linkSync(tmp, dest); } catch(e) { if (e.code === 'EEXIST') throw error(409, 'Ce nom existe déjà.'); throw e; }
    } finally { fs.rmSync(tmp, { force: true }); }
    return { ok: true, name, sharedWith: targets().map(t => t.id) };
  }
  async function route(req, res, url, send) {
    if (url.pathname === '/api/worldedit' && req.method === 'GET') { send(res, 200, list()); return true; }
    if (url.pathname === '/api/worldedit/upload' && req.method === 'POST') { send(res, 201, await upload(req, url.searchParams.get('name'))); return true; }
    if (url.pathname === '/api/worldedit/download' && req.method === 'GET') {
      const name = validName(url.searchParams.get('name')), file = path.join(shared, name);
      if (!fs.existsSync(file) || !fs.lstatSync(file).isFile()) throw error(404, 'Schématic introuvable.');
      res.writeHead(200, { 'Content-Type': 'application/octet-stream', 'Content-Disposition': 'attachment; filename="' + name + '"', 'Content-Length': fs.statSync(file).size });
      fs.createReadStream(file).pipe(res); return true;
    }
    return false;
  }
  return { list, route, upload };
}
module.exports = { createWorldEdit, validName, MAX_BYTES };
