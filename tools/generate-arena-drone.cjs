const fs=require('node:fs'),path=require('node:path');
const {Rig,ROOT}=require('./yggdrasil/rig.cjs');
const r=new Rig('arena_drone',15);r.groups.get('body').origin=[0,0,0];
const head=r.group('head',[0,8,0],'body');
r.box('Obsidian_eye_housing',[0,8,0],13,12,11,15,head);
r.box('Upper_armored_brow',[0,14,-1],16,2,12,6,head);
r.box('Lower_armored_lid',[0,2,-1],15,2,11,6,head);
for(const side of[-1,1]){r.box('Corner_guard_'+side,[side*7,8,-1],2,10,10,12,head);r.box('Rear_stabilizer_'+side,[side*6,9,5.5],3,5,2,6,head);r.box('Inset_rune_'+side,[side*7,10,-6.2],.6,2,.5,8,head,true);}
r.box('Ivory_sclera',[0,8,-5.8],10,8,1.2,4,head);
r.box('Red_iris',[0,8,-6.6],5,6,.8,8,head,true);
r.box('Vertical_pupil',[0,8,-7.1],1.6,4,.35,15,head);
r.box('Eye_glint',[-1.2,9.7,-7.35],1,1,.2,14,head,true);
r.box('Laser_emitter',[0,8,-7.5],.7,.7,.6,8,head,true);
r.box('Rear_core',[0,8,5.8],5,5,1.4,14,head,true);
r.idle('body',.25);const report=r.finish();
fs.mkdirSync(path.join(ROOT,'art/arena-drone'),{recursive:true});fs.writeFileSync(path.join(ROOT,'art/arena-drone/quality-report.json'),JSON.stringify(report,null,2));console.log(report);
