'use strict';
const fs = require('node:fs');
const path = require('node:path');

function install(root) {
  root = fs.realpathSync(root);
  const server = path.join(root, 'server.js'), app = path.join(root, 'public', 'app.js');
  let backend = fs.readFileSync(server, 'utf8'), frontend = fs.readFileSync(app, 'utf8');
  if (!backend.includes('const worldEdit =')) {
    const marker = "const resolveDir = (d) => path.resolve(ROOT, d || '.');";
    if (!backend.includes(marker)) throw Error('Version de panel incompatible : resolveDir absent.');
    backend = backend.replace(marker, marker + "\nconst worldEdit = require('./worldedit').createWorldEdit({ root: ROOT, services: () => config.services });");
    const auth = "  if (!isAuthed(req)) throw httpError(401, 'Non connecté');";
    if (!backend.includes(auth)) throw Error('Version de panel incompatible : contrôle de session absent.');
    backend = backend.replace(auth, auth + `
  if (p.startsWith('/api/worldedit')) {
    if (m === 'POST' && !/^application\\/octet-stream(?:;|$)/.test(req.headers['content-type'] || '')) throw httpError(415, 'Fichier binaire requis');
    if (await worldEdit.route(req, res, url, send)) return;
  }`);
  }
  if (!frontend.includes("['worldedit', 'WorldEdit'")) {
    const nav = "['set', 'Paramètres', ICONS.set]";
    if (!frontend.includes(nav)) throw Error('Version de panel incompatible : navigation absente.');
    frontend = frontend.replace(nav, "['worldedit', 'WorldEdit', ICONS.files], " + nav);
    const route = 'files: filesPage, set: settingsPage';
    if (!frontend.includes(route)) throw Error('Version de panel incompatible : route absente.');
    frontend = frontend.replace(route, 'files: filesPage, worldedit: worldeditPage, set: settingsPage');
  }
  const marker = '/* Shared WorldEdit schematic library.';
  if (frontend.includes(marker)) frontend = frontend.slice(0, frontend.indexOf(marker));
  frontend += '\n' + fs.readFileSync(path.join(__dirname, 'worldedit-page.js'), 'utf8');
  const backup = path.join(root, 'backups', 'panel-worldedit-' + Date.now());
  fs.mkdirSync(backup, { recursive: true });
  fs.copyFileSync(server, path.join(backup, 'server.js'));
  fs.copyFileSync(app, path.join(backup, 'app.js'));
  if (fs.existsSync(path.join(root, 'worldedit.js'))) fs.copyFileSync(path.join(root, 'worldedit.js'), path.join(backup, 'worldedit.js'));
  fs.copyFileSync(path.join(__dirname, 'worldedit.js'), path.join(root, 'worldedit.js'));
  fs.writeFileSync(server, backend);fs.writeFileSync(app, frontend);
  return { backup };
}
if (require.main === module) console.log(JSON.stringify(install(process.argv[2])));
module.exports = { install };
