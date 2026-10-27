const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../..'),folder=path.join(root,'src/main/resources/assets/mmorpg/bbmodels/yggdrasil');
const flatten=node=>[node,...(node.children||[]).flatMap(flatten)];
test('all guardians and crystal states have valid authored meshes, continuous UV coordinates, rigs and emissive geometry',()=>{
 const files=fs.readdirSync(folder).filter(n=>n.endsWith('.json'));assert.equal(files.length,13);
 for(const file of files){const data=JSON.parse(fs.readFileSync(path.join(folder,file),'utf8')),nodes=data.parts.flatMap(flatten),bones=new Set(nodes.filter(n=>n.children).map(n=>n.uuid)),meshes=nodes.filter(n=>n.vertices);
  assert.equal(data.revision,3,file);assert.ok(data.texture.endsWith('material_atlas.png'));assert.ok(meshes.length>=30,file);assert.ok(bones.size>=5,file);assert.ok(meshes.some(m=>m.emissive),file);
  let nonRectangular=0,faces=0;for(const mesh of meshes){const points=Object.values(mesh.vertices);if(points.length>8)nonRectangular++;
   for(const face of Object.values(mesh.faces)){faces++;assert.ok(face.vertices.length===3||face.vertices.length===4);const verts=face.vertices.map(k=>mesh.vertices[k]);assert.ok(verts.every(Boolean));
    for(const key of face.vertices){const uv=face.uv[key];assert.equal(uv.length,2);assert.ok(uv.every(Number.isFinite));assert.ok(uv[0]>=0&&uv[0]<=data.resolution.width);assert.ok(uv[1]>=0&&uv[1]<=data.resolution.height);}
    const a=verts[1].map((v,i)=>v-verts[0][i]),b=verts[2].map((v,i)=>v-verts[0][i]);const n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];assert.ok(n.reduce((s,v)=>s+v*v,0)>1e-10,file+' degenerate surface');
   }
  }
  assert.equal(nonRectangular,0,file+" must remain cubic");assert.ok(meshes.every(m=>Object.keys(m.vertices).length===8&&Object.keys(m.faces).length===6),file+" rectangular prisms only");assert.ok(faces>500,file);
  for(const required of['idle','walk','attack','cast','death'])assert.ok(data.animations.some(a=>a.name===required),file+' '+required);
  for(const a of data.animations)for(const line of a.tracks.split('\n'))if(line){let[bone,channel,keys]=line.split('|');assert.ok(bones.has(bone),file+' unknown animation bone');assert.ok('rps'.includes(channel));for(const k of keys.split(';')){const values=k.split(',');assert.ok(values.slice(0,4).every(n=>Number.isFinite(Number(n))));assert.ok(Number(values[0])<=a.length+.0001);}}
  const bb=JSON.parse(fs.readFileSync(path.join(root,'art/yggdrasil/models',file.replace('.json','.bbmodel'))));assert.equal(bb.meta.model_format,'free');assert.equal(bb.elements.length,meshes.length);assert.ok(fs.existsSync(path.resolve(root,'art/yggdrasil/models',bb.textures[0].relative_path)));for(const a of bb.animations)for(const bone of Object.keys(a.animators))assert.ok(bones.has(bone));
 }
});
