// Original articulated Blockbench polygon meshes. Requires the authored material atlas already in resources.
const fs=require('node:fs'),path=require('node:path');
const {Rig,ROOT}=require('./yggdrasil/rig.cjs');
const {IDS,builders:first}=require('./yggdrasil/creatures.cjs');
const {builders:last,crystal}=require('./yggdrasil/elders.cjs');
const materials=[0,1,2,3,4,5,6,7,8,9],reports=[];
[...first,...last].forEach((build,i)=>{const rig=new Rig(IDS[i],materials[i]);build(rig);reports.push(rig.finish());});
for(const [id,material]of[['crystal_neutral',14],['crystal_red',8],['crystal_blue',2]]){const rig=new Rig(id,material);crystal(rig,material);reports.push(rig.finish());}
fs.mkdirSync(path.join(ROOT,'art/yggdrasil'),{recursive:true});fs.writeFileSync(path.join(ROOT,'art/yggdrasil/quality-report.json'),JSON.stringify(reports,null,2)+'\n');
console.log(JSON.stringify(reports.map(x=>({id:x.id,bones:x.bones,meshes:x.meshes,faces:x.faces,glow:x.emissive}))));
