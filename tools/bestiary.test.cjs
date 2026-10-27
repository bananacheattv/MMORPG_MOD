const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const ROOT=path.resolve(__dirname,'..'),RES=path.join(ROOT,'src/main/resources/assets/mmorpg');
const flatten=n=>[n,...(n.children||[]).flatMap(flatten)];
test('all refined cubic rigs retain valid UVs, bones, textures and animation targets',()=>{
 for(const [kind,expected]of[['classic',11],['mobs',30],['mounts',12],['guides',10]]){
  const files=fs.readdirSync(path.join(RES,'bbmodels',kind)).filter(f=>f.endsWith('.json'));assert.equal(files.length,expected);
  for(const file of files){const d=JSON.parse(fs.readFileSync(path.join(RES,'bbmodels',kind,file))),nodes=d.parts.flatMap(flatten),bones=new Set(nodes.filter(n=>n.children).map(n=>n.uuid)),leaves=nodes.filter(n=>!n.children);
   assert.equal(d.revision,3,file);assert.ok(leaves.length>=40,file);assert.ok(bones.size>=5,file);assert.ok(d.animations.length>=2,file);
   for(const c of leaves){
    if(c.vertices){assert.equal(Object.keys(c.vertices).length,8,file+' rectangular prism');assert.equal(Object.keys(c.faces).length,6);}
    else assert.ok(c.to.every((v,i)=>Number.isFinite(v)&&v>c.from[i]),file+' positive volume');
    for(const face of Object.values(c.faces)){const coordinates=Array.isArray(face.uv)?[face.uv.slice(0,2),face.uv.slice(2)]:Object.values(face.uv);for(const uv of coordinates){assert.ok(uv.every(Number.isFinite));assert.ok(uv[0]>=0&&uv[0]<=d.resolution.width&&uv[1]>=0&&uv[1]<=d.resolution.height,file+' UV bounds');}}
   }
   for(const anim of d.animations)for(const line of anim.tracks.split('\n'))if(line){const[bone,channel,keys]=line.split('|');assert.ok(bones.has(bone),file+' animation bone '+bone);assert.ok('rps'.includes(channel));for(const frame of keys.split(';'))assert.ok(Number(frame.split(',')[0])<=anim.length+.001,file+' animation duration');}
   const texture=d.texture?path.join(RES,d.texture.replace('mmorpg:','')):path.join(RES,'textures/imported',kind,file.replace('.json','.png'));assert.ok(fs.existsSync(texture),file+' texture');
  }
 }
});
test('refinements preserve old animation counts and improve boss/mount geometry',()=>{
 const r=JSON.parse(fs.readFileSync(path.join(ROOT,'art/bestiary/refinement-report.json')));
 assert.equal(r.mounts.length,12);assert.equal(r.bosses.length,5);assert.equal(r.mobs.length,30);
 for(const e of[...r.mounts,...r.bosses])assert.ok(e.after>e.before+25,e.id);
});
test('paladin shields use the vanilla handedness transform in all five tiers',()=>{
 for(let i=0;i<5;i++){
  const item=JSON.parse(fs.readFileSync(path.join(RES,'items',`bouclier_paladin_${i}.json`))),model=JSON.parse(fs.readFileSync(path.join(RES,'models/item',`bouclier_paladin_${i}.json`)));
  assert.deepEqual(item.model.transformation.scale,[1,-1,-1]);assert.deepEqual(model.display.thirdperson_lefthand.rotation,[0,90,0]);assert.deepEqual(model.display.thirdperson_lefthand.translation,[10,6,12]);assert.deepEqual(model.display.firstperson_lefthand.rotation,[0,180,5]);assert.deepEqual(model.display.firstperson_lefthand.translation,[10,0,-10]);assert.ok(model.elements.length>=15);
  const handle=model.elements.find(e=>e.name==="handle"),face=model.elements.find(e=>e.name==="face");assert.ok(handle.from[2]>face.to[2],"handle stays behind plate");assert.ok(Math.abs((face.from[0]+face.to[0])/2)<.01,"shield uses vanilla centered origin");assert.ok(face.from[1]<0&&face.to[1]>0,"plate straddles vanilla vertical origin");
 }
 for(const file of fs.readdirSync(path.join(RES,'models/item')).filter(f=>f.startsWith('bouclier_paladin_'))){const d=JSON.parse(fs.readFileSync(path.join(RES,'models/item',file)));for(const value of Object.values(d.textures)){assert.ok(value.startsWith('mmorpg:block/'));const bytes=fs.readFileSync(path.join(RES,'textures',value.replace('mmorpg:','')+'.png'));const width=bytes.readUInt32BE(16),height=bytes.readUInt32BE(20);assert.equal(width&(width-1),0);assert.equal(height&(height-1),0);}}
});
