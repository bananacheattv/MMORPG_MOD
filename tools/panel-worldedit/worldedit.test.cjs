const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs'), path = require('node:path'), os = require('node:os');
const { Readable } = require('node:stream');
const { createWorldEdit, validName } = require('./worldedit');

test('schematics share one directory across all game servers, preserving uploads and excluding db/proxy', async () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'aeloria-worldedit-'));
  const services = ['hub','serveur','mines','plaines','donjons','db','proxy'].map(id => ({id,name:id,type:['db','proxy'].includes(id)?id:'game',dir:'servers/'+id}));
  const library = createWorldEdit({root,services:()=>services});
  assert.equal(library.list().servers.length,5);
  for(const s of services.filter(s=>s.type==='game')) assert.match(fs.readFileSync(path.join(root,s.dir,'config/worldedit/worldedit.properties'),'utf8'),/allow-symbolic-links=true/);
  assert(!fs.existsSync(path.join(root,'servers/db/config/worldedit')));
  assert(!fs.existsSync(path.join(root,'servers/proxy/config/worldedit')));
  const bytes=Buffer.from([10,0,0,0]); // valid empty, uncompressed root NBT compound
  const request = () => Object.assign(Readable.from([bytes]),{headers:{'content-length':bytes.length}});
  const result = await library.upload(request(),'spawn.schem');assert.equal(result.sharedWith.length,5);
  for (const s of services.filter(s=>s.type==='game')) assert.deepEqual(fs.readFileSync(path.join(root,s.dir,'config/worldedit/schematics/spawn.schem')),bytes);
  await assert.rejects(library.upload(request(),'spawn.schem'),e=>e.code===409);
  assert.deepEqual(fs.readFileSync(path.join(root,'shared/schematics/spawn.schem')),bytes);
  for(const name of ['../escape.schem','x/escape.schem','.secret.schem','a.exe','a.schem:evil']) assert.throws(()=>validName(name));
  await assert.rejects(library.upload(Object.assign(Readable.from([Buffer.from('wrong')]),{headers:{}}),'bad.schem'),e=>e.code===400);
  // WorldEdit saves in any backend become immediately visible to all others and the panel.
  fs.writeFileSync(path.join(root,'servers/mines/config/worldedit/schematics/from-game.schem'),bytes);
  assert.equal(library.list().files.length,2);
  for(const s of services.filter(s=>s.type==='game')) fs.unlinkSync(path.join(root,s.dir,'config/worldedit/schematics'));
  // Fixture is a resolved, freshly-created directory within the OS temporary directory.
  assert(root.startsWith(fs.realpathSync(os.tmpdir())+path.sep));fs.rmSync(root,{recursive:true});
});
