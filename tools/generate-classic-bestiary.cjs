// Original Minecraft-style cubic rigs. Material atlas is the authored image_gen asset; no raster processing.
const fs=require('node:fs'),path=require('node:path');
const {Rig,ROOT}=require('./yggdrasil/rig.cjs');
const reports=[];
const options={kind:'classic',art:'art/bestiary/classic',texture:'mmorpg:textures/entity/bestiary/material_atlas.png'};
const atlas=path.join(ROOT,'src/main/resources/assets/mmorpg/textures/entity/bestiary/material_atlas.png');
fs.mkdirSync(path.dirname(atlas),{recursive:true});fs.copyFileSync(path.join(ROOT,'art/bestiary/material_atlas.png'),atlas);
function rig(id,material){const r=new Rig(id,material,options);r.groups.get('body').origin=[0,16,0];return r;}
function box(r,n,c,s,mat,parent='body',glow=false){return r.box(n,c,...s,mat,parent,glow);}
function eye(r,n,x,y,z,mat,parent='head'){
 box(r,n+'_socket',[x,y,z+.12],[2.2,1.8,.5],15,parent);
 box(r,n+'_iris',[x,y,z-.18],[1.5,1.1,.35],mat,parent,true);
 box(r,n+'_pupil',[x,y,z-.39],[.38,.9,.12],15,parent);
 box(r,n+'_reflection',[x-.35,y+.24,z-.48],[.3,.3,.1],4,parent,true);
}
function limb(r,n,pivot,center,size,mat,phase){r.group(n,pivot,'body');box(r,n,center,size,mat,n);r.gait(n,phase);return n;}
function human(id,skin,armor,kind){
 const r=rig(id,skin),goblin=kind==='goblin',skeleton=kind==='skeleton',phantom=kind==='wraith';
 box(r,'Torso',[0,18,0],[goblin?9:8,12,5],skin);box(r,'Pelvis',[0,12.3,0],[8,2.5,5],7);
 r.group('head',[0,25,0],'body');box(r,'Skull',[0,27.5,0],[goblin?9:8,7,7],skin,'head');
 if(!skeleton){box(r,'Chestplate',[0,19,-2.8],[8.6,9,.8],armor);box(r,'Backplate',[0,19,2.8],[8.4,9,.8],armor);}
 for(const side of[-1,1]){
  const leg=limb(r,'leg_'+side,[side*2.1,12,0],[side*2.1,6,0],[skeleton?2:3.8,12,skeleton?2:4],skin,side);
  box(r,'Boot_'+side,[side*2.1,1.3,-.7],[4.3,2.6,5.1],7,leg);
  box(r,'Knee_'+side,[side*2.1,5.8,-2.3],[2.8,2.5,.7],armor,leg);
  const arm=limb(r,'arm_'+side,[side*5.3,23.5,0],[side*5.3,18.7,0],[skeleton?1.8:3.4,10,3.5],skin,-side);
  if(!skeleton){box(r,'Pauldron_'+side,[side*5.4,23.2,0],[4.5,3,4.7],armor,arm);box(r,'Vambrace_'+side,[side*5.3,16,0],[4,2.2,4.1],armor,arm);}
  for(let i=0;i<3;i++)box(r,'Finger_'+side+i,[side*5.3+(i-1)*.8,12.9,-.8],[.65,1.9,1.8],skin,arm);
  eye(r,'Eye_'+side,side*2.1,28,-3.8,phantom?9:14);
  box(r,'Brow_'+side,[side*2,29.3,-3.9],[2.9,.7,.7],armor,'head');
  if(goblin||kind==='orc'){
   box(r,'Ear_root_'+side,[side*5,28,0],[2.3,2.4,2],skin,'head');box(r,'Ear_tip_'+side,[side*6.4,28.3,.2],[1.3,1.5,1.3],skin,'head');
   box(r,'Tusk_'+side,[side*2.8,25,-4.1],[.8,2.7,.9],4,'head');
  }
 }
 box(r,'Mouth',[0,25.6,-3.7],[4.5,.8,.5],15,'head');
 if(skeleton){for(let rib=0;rib<5;rib++)for(const side of[-1,1]){box(r,'Rib_'+side+rib,[side*2.4,14.7+rib*1.7,-2.8],[3.6,.8,1.1],4);box(r,'Rib_back_'+side+rib,[side*2.4,14.7+rib*1.7,2.8],[3.6,.8,1.1],4);}box(r,'Spine',[0,18,1.5],[1.3,12,1.6],4);}
 else {
  for(let side of[-1,1])for(let y=15;y<24;y+=3)box(r,'Rivet_'+side+y,[side*3.3,y,-3.4],[.6,.6,.5],12);
  box(r,'Belt_buckle',[0,12,-3.1],[2.5,2,.8],12);box(r,'Buckle_inset',[0,12,-3.6],[1.2,.9,.3],15);
  for(let i=0;i<3;i++)box(r,'Chest_rune_'+i,[0,17+i*2,-3.4],[1.3,.5,.3],14,'body',phantom||kind==='knight');
 }
 if(kind==='bandit'){
  box(r,'Hood_top',[0,31.4,.4],[9,1.3,8.2],5,'head');for(const side of[-1,1])box(r,'Hood_side_'+side,[side*4.3,28,1],[1,6,7],5,'head');
  box(r,'Crossbow_stock',[-5.3,12,-4],[1.5,1.6,9],7,'arm_-1');box(r,'Crossbow_limb',[-5.3,12,-7.7],[10,1.4,1.3],6,'arm_-1');box(r,'Crossbow_string',[-5.3,12,-5.8],[9,.25,.25],4,'arm_-1');
 }else if(kind!=='wraith'){
  box(r,'Blade_grip',[-5.3,11,-1],[1.2,5,1.2],7,'arm_-1');box(r,'Blade_guard',[-5.3,9,-1],[5,.8,1.6],12,'arm_-1');
  box(r,'Blade',[-5.3,3.2,-1],[2,10,1],6,'arm_-1');box(r,'Blade_edge',[-6.4,3.2,-1],[.3,10,1.1],4,'arm_-1');
 }
 if(phantom||kind==='knight'){
  for(let i=0;i<5;i++)box(r,'Tattered_mantle_'+i,[(i-2)*1.7,14,3.6],[1.6,14+(i%2)*3,.8],5);
  for(const side of[-1,1])for(let i=0;i<3;i++)box(r,'Crown_spike_'+side+i,[side*(2+i*1.2),32+i,0],[1,2.5+i*.7,1],kind==='knight'?10:9,'head');
 }
 r.strike('arm_-1',65);r.strike('arm_1',25);r.idle('body',.3);return r;
}
for(const data of[
 ['zombie_des_cryptes',0,7,'zombie'],['bandit_arbaletrier',1,5,'bandit'],['gobelin',0,7,'goblin'],['squelette_maudit',4,6,'skeleton'],
 ['orc_guerrier',0,6,'orc'],['spectre_givre',9,9,'wraith'],['chevalier_neant',15,10,'knight']])reports.push(human(...data).finish());
{
 const r=rig('loup_sombre',15);box(r,'Flank',[0,10,2],[7,7,13],15);box(r,'Chest',[0,11,-4],[8,8,6],1);
 r.group('head',[0,13,-7],'body');box(r,'Skull',[0,15,-8],[7,6,6],15,'head');box(r,'Muzzle',[0,13.5,-12],[4.4,3,4],1,'head');box(r,'Nose',[0,14,-14.1],[3,1.6,.8],15,'head');
 for(const side of[-1,1]){
  box(r,'Ear_'+side,[side*2.3,19,-7],[2.4,3.2,2],15,'head');box(r,'Inner_ear_'+side,[side*2.3,19,-8.1],[1.2,2,.3],5,'head');eye(r,'Eye_'+side,side*2.4,16,-11.1,14);
  for(let n=0;n<3;n++)box(r,'Fang_'+side+n,[side*1.5,12,-11-n],[.5,1.4,.5],4,'head');
  for(let i=0;i<2;i++){const z=i?7:-4,leg=limb(r,'leg_'+side+i,[side*2.5,10,z],[side*2.5,5,z],[2.4,10,2.6],15,side*(i?1:-1));box(r,'Paw_'+side+i,[side*2.5,1,z-.8],[3,2,4],1,leg);for(let toe=-1;toe<=1;toe++)box(r,'Claw_'+side+i+toe,[side*2.5+toe*.8,.7,z-3],[.45,.8,1.2],4,leg);}
  for(let i=0;i<6;i++)box(r,'Mane_'+side+i,[side*(3+i%2*.3),13-i*.65,-5+i*.7],[2,2.2,1.4],1);
 }
 r.group('tail',[0,12,8],'body');r.bone('Tail',[0,12,8],[0,16,16],2.4,1.1,15,'tail');r.anim('idle','tail','r',[[0,0,-12,0],[1.5,0,12,0],[3,0,-12,0]]);r.strike('head',25);reports.push(r.finish());
}
{
 const r=rig('araignee_venimeuse',15);box(r,'Abdomen',[0,5,4],[9,7,10],15);box(r,'Thorax',[0,4,-3],[7,5,6],3);r.group('head',[0,4,-6],'body');box(r,'Mask',[0,4,-7],[6,4,3],15,'head');
 for(const side of[-1,1]){for(let i=0;i<4;i++){const z=-4+i*2.5,n='leg_'+side+i;r.group(n,[side*3,4,z],'body');r.bone(n+'_upper',[side*3,4,z],[side*(7+i%2),6,z+(i-1.5)*1.5],.7,.6,3,n);r.bone(n+'_lower',[side*(7+i%2),6,z+(i-1.5)*1.5],[side*10,.5,z+(i-1.5)*2.5],.6,.3,15,n);r.gait(n,side*(i%2?1:-1));}eye(r,'Eye_'+side,side*1.6,4.6,-8.7,14);r.tooth('Venom_fang_'+side,[side*1.4,2.8,-8],[side*1.4,1.1,-9.3],.65,'head');}
 for(let i=0;i<4;i++)box(r,'Venom_gland_'+i,[(i%2?1:-1)*2,7,1+Math.floor(i/2)*4],[1.8,1.1,2],0);
 r.strike('head',24);reports.push(r.finish());
}
{
 const r=rig('elementaire_feu',8);box(r,'Molten_heart',[0,15,0],[6,8,6],8,'body',true);r.group('head',[0,23,0],'body');box(r,'Basalt_mask',[0,25,0],[7,6,6],15,'head');for(const side of[-1,1])eye(r,'Eye_'+side,side*2,25,-3.3,8);
 for(let ring=0;ring<3;ring++)for(let i=0;i<6;i++){const a=i*Math.PI/3+ring*.5,n='floating_rod_'+ring+i,x=Math.cos(a)*(7+ring),z=Math.sin(a)*(7+ring);r.group(n,[x,10+ring*6,z],'body');box(r,n,[x,9+ring*6,z],[1.8,6,1.8],6,n);box(r,n+'_seam',[x,9+ring*6,z-.95],[.6,4,.25],8,n,true);r.anim('idle',n,'p',[[0,0,0,0],[1.5,0,1.5,0],[3,0,0,0]]);}
 r.idle('body',.8);reports.push(r.finish());
}
{
 const r=rig('golem_cristal',6);box(r,'Monolith_torso',[0,28,0],[17,17,11],6);box(r,'Basin',[0,18,0],[13,5,9],6);r.group('head',[0,39,-2],'body');box(r,'Ancient_mask',[0,40,-2],[9,8,8],9,'head');
 for(const side of[-1,1]){
  const leg=limb(r,'leg_'+side,[side*4,17,0],[side*4,8.5,0],[6,17,7],6,side);box(r,'Foot_'+side,[side*4,2,-1],[7,4,10],6,leg);
  const arm=limb(r,'arm_'+side,[side*11,34,0],[side*12,24,0],[7,22,8],6,-side);box(r,'Shoulder_plate_'+side,[side*12,35,0],[10,5,11],9,arm);
  for(let i=0;i<4;i++)box(r,'Crystal_crest_'+side+i,[side*(10+i),39+i%2*2,(i-1.5)*2],[2.1,5+i%2*2,2.1],14,arm,true);
  for(let i=0;i<3;i++)box(r,'Finger_'+side+i,[side*12+(i-1)*2,11,-1],[1.5,3,4],6,arm);eye(r,'Eye_'+side,side*2.3,41,-6.3,14);
 }
 box(r,'Heart_cavity',[0,29,-5.7],[6,7,1.5],15);box(r,'Heart',[0,29,-6.6],[3.4,4.5,1.3],14,'body',true);
 for(const side of[-1,1])for(let i=0;i<5;i++)box(r,'Rune_'+side+i,[side*5,22+i*2,-5.8],[1,.7,.5],14,'body',true);
 r.strike('arm_-1',75);r.strike('arm_1',75);reports.push(r.finish());
}
fs.writeFileSync(path.join(ROOT,'art/bestiary/classic-report.json'),JSON.stringify(reports,null,2)+'\n');
console.log(JSON.stringify(reports.map(r=>({id:r.id,cuboids:r.meshes,bones:r.bones,faces:r.faces}))));
