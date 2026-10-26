const {add,mul,mix,bezier}=require('./rig.cjs');
const IDS=['scarabee_racines','mante_epines','raie_celeste','hydre_corail','millepattes_ambre','meduse_oracles','crabe_bastion','papillon_eclipse','serpent_couronnes','oeil_yggdrasil'];
function gems(r,name,p,size,parent='body',material=14){r.ellipsoid(name+'_setting',p,size*1.4,size*.5,size*1.4,12,parent);r.ellipsoid(name+'_gem',add(p,[0,size*.4,0]),size,size*.65,size,material,parent,true,4,8);}
function insectLeg(r,name,side,a,b,c,material,phase=1){let bone=r.group(name,a,'body');r.ellipsoid(name+'_socket',a,5,5,5,9,bone);r.bone(name+'_femur',a,b,4.4,3,material,bone);r.ellipsoid(name+'_knee',b,4.1,4.1,4.1,12,bone);r.bone(name+'_tibia',b,c,3.3,1.4,material,bone);r.tooth(name+'_claw',c,add(c,[side*5,-2,-5]),2.2,bone);r.gait(bone,phase);return bone;}
function antler(r,name,points,radius,parent='body',material=10){r.horn(name,points,radius,material,parent);for(let i=2;i<points.length-1;i+=2){let p=points[i],tip=add(p,[Math.sign(p[0]||1)*6,9,-3]);r.horn(name+'_fork'+i,[p,mix(p,tip,.55),tip],radius*.4,material,parent);}}
function jointAttack(r,bone,amount=35){r.strike(bone,amount);r.anim('cast',bone,'r',[[0,0,0,0],[.6,-amount*.3,0,0],[1.8,-amount*.3,0,0],[2.4,0,0,0]],2.4,false);}
function beetle(r){
 r.ellipsoid('Abdomen_carved_chitin',[0,39,7],28,23,39,0);r.ellipsoid('Segmented_underbelly',[0,25,9],23,11,34,11);
 for(let i=0;i<7;i++){let z=-25+i*10,y=48+Math.sin((i+1)*Math.PI/8)*16;for(let side of[-1,1]){r.plate('Gold_shell_rim_'+side+i,[side*14,y,z],16,8,8,9);r.plate('Emerald_shell_panel_'+side+i,[side*14,y+2,z],15,7,7.4,0);}r.bone('Golden_dorsal_seam_'+i,[0,y+9,z-4],[0,y+9,z+4],.9,.9,12);}
 gems(r,'Dorsal_heart',[0,72,9],4.5);for(let i=0;i<4;i++)r.tooth('Back_spine_'+i,[0,69,i*13-12],[0,83+i*2,i*13-6],4);
 for(let side of[-1,1])for(let i=0;i<3;i++){let z=(i-1)*23;insectLeg(r,'Leg_'+side+'_'+i,side,[side*21,36,z],[side*41,23,z+8],[side*51,4,z+18],0,side*(i%2?1:-1));}
 const head=r.group('head',[0,39,-30],'body');r.ellipsoid('Crowned_head',[0,40,-45],19,14,20,0,head);r.ellipsoid('Mouth',[0,29,-59],12,6,4,11,head);for(let side of[-1,1]){
  r.eye('Eye_'+side,[side*12,47,-60],4.6,head);r.bone('Angry_brow_'+side,[side*5,53,-59],[side*19,49,-57],2,1.5,9,head);
  let jaw=r.group('mandible_'+side,[side*8,28,-51],head);r.horn('Mandible_'+side,bezier([side*8,28,-53],[side*29,27,-59],[side*28,20,-80],[side*9,20,-83],7),5,10,jaw);for(let i=0;i<3;i++)r.tooth('Mandible_teeth_'+side+i,[side*(17+i*3),24,-62-i*6],[side*(11+i*2),22,-64-i*6],2.3,jaw);jointAttack(r,jaw,side*31);
  let feeler=r.group('antenna_'+side,[side*9,51,-45],head);r.chain('Antenna_stalk_'+side,bezier([side*9,51,-45],[side*13,72,-47],[side*26,79,-62],[side*28,78,-69],9),2.1,13,feeler);r.ellipsoid('Antenna_glow_'+side,[side*28,78,-69],2.8,4,2.8,14,feeler,true);r.anim('idle',feeler,'r',[[0,0,0,-side*4],[1.5,4,0,side*5],[3,0,0,-side*4]]);
 }
 r.idle('body',.7);r.anim('idle','head','r',[[0,0,-4,0],[1.5,0,4,0],[3,0,-4,0]]);
}
function mantis(r){
 r.ellipsoid('Thorax',[0,72,3],12,31,15,1);r.chain('Leaf_abdomen',bezier([0,66,10],[0,56,27],[0,25,34],[0,20,22],10),14,1);
 for(let i=0;i<6;i++)r.plate('Abdomen_leaf_armor_'+i,[0,56-i*5,17+i*2],14-i*.7,5,12,1);
 for(let side of[-1,1]){
  for(let i=0;i<2;i++)insectLeg(r,'Walking_leg_'+side+i,side,[side*9,47,7+i*15],[side*25,28,i*29-12],[side*28,3,i*39-20],1,side*(i?1:-1));
  for(let i=0;i<2;i++){let a=[side*10,91-i*27,-3],b=[side*(35+i*4),93-i*25,-17],tip=[side*(29+i*6),40-i*17,-49];const arm=r.group('Scythe_arm_'+side+i,a,'body');r.bone('Arm_armor_'+side+i,a,b,5.8,4.2,1,arm);r.ellipsoid('Elbow_'+side+i,b,5.6,5.6,5.6,9,arm);r.horn('Sickle_spine_'+side+i,bezier(b,add(b,[side*10,-11,-7]),add(tip,[side*13,11,6]),tip,10),6.5,9,arm);r.horn('Ivory_sickle_edge_'+side+i,bezier(add(b,[0,-4,-3]),add(b,[side*7,-17,-9]),add(tip,[side*9,8,2]),tip,10),4.2,10,arm);for(let t=0;t<5;t++){let p=mix(b,tip,(t+1)/7);r.tooth('Scythe_serration_'+side+i+t,p,add(p,[-side*5,-6,-4]),2.2,arm);}jointAttack(r,arm,side*(i?38:53));}
  const wing=r.group('Leaf_mantle_'+side,[side*7,91,9],'body');r.wing('Mantle_'+side,[[side*6,94,12],[side*29,85,17],[side*35,50,22],[side*12,23,26],[side*6,60,13]],1,wing,13);r.anim('idle',wing,'r',[[0,0,0,-side*3],[1.5,0,0,side*4],[3,0,0,-side*3]]);
 }
 const head=r.group('head',[0,106,-3],'body');r.ellipsoid('Triangular_mask',[0,113,-6],18,12,11,1,head);for(let side of[-1,1]){r.eye('Compound_eye_'+side,[side*13,117,-14],5.1,head);r.tooth('Face_blade_'+side,[side*11,108,-13],[side*4,98,-18],3,head);const feeler=r.group('Feeler_'+side,[side*9,122,-4],head);r.chain('Feeler_curve_'+side,bezier([side*9,122,-4],[side*13,139,-6],[side*25,145,-9],[side*27,145,-18],8),1.5,13,feeler);r.anim('idle',feeler,'r',[[0,0,0,-side*5],[1.5,5,0,side*4],[3,0,0,-side*5]]);}r.idle('body',1.1);
}
function ray(r){
 r.ellipsoid('Streamlined_body',[0,62,0],24,10,36,2);r.ellipsoid('Pale_underbody',[0,58,-4],20,5,29,10);
 for(let side of[-1,1]){
  const fin=r.group('Celestial_fin_'+side,[side*14,61,-2],'body');r.wing('Fin_'+side,[[side*15,67,-25],[side*34,72,-31],[side*61,69,-18],[side*91,57,15],[side*75,48,11],[side*56,43,32],[side*42,52,25],[side*24,57,31],[side*14,58,14]],2,fin,10);
  for(let i=0;i<6;i++){let tip=[side*(39+i*8),52+i*.2,24-i*3];r.horn('Fin_talon_'+side+i,[add(tip,[0,5,0]),tip,add(tip,[side*8,-8,7])],2.1,10,fin);}
  r.anim('idle',fin,'r',[[0,0,0,-side*6],[1.5,0,0,side*9],[3,0,0,-side*6]]);r.anim('walk',fin,'r',[[0,0,0,-side*16],[.6,0,0,side*19],[1.2,0,0,-side*16]],1.2,true);jointAttack(r,fin,side*24);
  r.eye('Starlight_eye_'+side,[side*11,69,-29],4.5);for(let i=0;i<5;i++)r.bone('Gill_'+side+i,[side*(10+i*2),56,-15+i*5],[side*(16+i*2),53,-13+i*5],.7,.7,15);
 }
 const tail=r.group('Comet_tail',[0,60,27],'body');const path=bezier([0,60,27],[3,53,58],[-22,42,86],[-8,64,108],24);r.tube('Flexible_tail',path,path.map((_,i)=>6*(1-i/(path.length+1))+.3),2,tail,9);for(let i=3;i<path.length;i+=3){let p=path[i];r.tooth('Tail_spike_'+i,p,add(p,[0,9,5]),2,tail);}r.anim('idle',tail,'r',[[0,0,-6,0],[1.5,0,8,0],[3,0,-6,0]]);r.anim('attack',tail,'r',[[0,0,0,0],[.25,0,-27,0],[.5,0,37,0],[1,0,0,0]],1,false);
 r.ellipsoid('Predatory_mouth',[0,60,-34],11,4.5,3.5,11);for(let s of[-1,1])for(let i=0;i<3;i++)r.tooth('Mouth_fang_'+s+i,[s*(3+i*3),61,-36],[s*(3+i*3),55,-38],1.4);
 for(let i=0;i<5;i++){let z=i*12-12;antler(r,'Dorsal_crown_'+i,[[0,70,z],[0,81+i*2,z+2],[0,88+i*2,z+11]],3.2,'body',10);}r.idle('body',1.4);
}
function hydra(r){
 r.ellipsoid('Coral_body',[0,29,9],29,21,35,3);r.ellipsoid('Scaled_belly',[0,19,6],23,11,31,10);
 for(let side of[-1,1])for(let i=0;i<2;i++){let a=[side*22,28,i*37-6],b=[side*35,14,i*43-12],c=[side*36,4,i*49-14];insectLeg(r,'Reef_leg_'+side+i,side,a,b,c,3,side*(i?1:-1));for(let t=0;t<3;t++)r.tooth('Reef_claw_'+side+i+t,add(c,[(t-1)*3,1,-1]),add(c,[(t-1)*3,-1,-9]),2);}
 for(let i=-1;i<=1;i++){
  const start=[i*15,42,-4],headPos=[i*32,107+(i===0?13:0),-29+(Math.abs(i)*5)],neck=r.group('Neck_'+i,start,'body');const points=bezier(start,[i*24,61,-10],[i*35,86,-18],headPos,14);r.tube('Muscular_neck_'+i,points,points.map((_,n)=>10-n*.22),3,neck,10);
  for(let n=2;n<points.length;n+=2){let p=points[n];r.plate('Neck_armor_'+i+n,add(p,[0,0,6]),10,4,5,9,neck);}
  const head=r.group('Head_'+i,headPos,neck);r.ellipsoid('Hydra_skull_'+i,add(headPos,[0,0,-7]),14,11,16,3,head);r.plate('Skull_crest_'+i,add(headPos,[0,9,-5]),13,4,15,9,head);
  const jaw=r.group('Jaw_'+i,add(headPos,[0,-6,-6]),head);r.ellipsoid('Jaw_bone_'+i,add(headPos,[0,-9,-16]),12,3.5,13,10,jaw);r.ellipsoid('Open_mouth_'+i,add(headPos,[0,-4,-19]),10,5,10,11,head);
  for(let s of[-1,1]){r.eye('Hydra_eye_'+i+s,add(headPos,[s*10,4,-17]),3.8,head);antler(r,'Coral_horn_'+i+s,[add(headPos,[s*9,7,1]),add(headPos,[s*17,19,5]),add(headPos,[s*20,28,1]),add(headPos,[s*18,36,-6])],3.5,head,10);for(let t=0;t<4;t++){r.tooth('Upper_fang_'+i+s+t,add(headPos,[s*(4+t*2),-1,-12-t*5]),add(headPos,[s*(4+t*2),-9,-13-t*5]),1.5,head);r.tooth('Lower_fang_'+i+s+t,add(headPos,[s*(3+t*2),-10,-12-t*5]),add(headPos,[s*(3+t*2),-5,-13-t*5]),1.1,jaw);}}
  r.anim('idle',neck,'r',[[0,0,i*4,0],[1.5,0,-i*5+2,0],[3,0,i*4,0]]);r.strike(head,21);r.anim('attack',jaw,'r',[[0,0,0,0],[.25,31,0,0],[.5,0,0,0],[1,0,0,0]],1,false);r.anim('cast',head,'r',[[0,0,0,0],[.6,-30,0,0],[1.5,-30,0,0],[2.4,0,0,0]],2.4,false);
 }
 const tail=r.group('Coral_tail',[0,27,35],'body');r.chain('Tail',bezier([0,27,35],[7,18,52],[-7,10,77],[11,8,86],16),7,3,tail);for(let n=0;n<9;n++)antler(r,'Reef_growth_'+n,[[Math.sin(n)*21,42,n*6-15],[Math.sin(n)*27,57,n*6-11],[Math.sin(n)*31,67,n*6-16]],2.8);r.idle('body',.8);
}
function centipede(r){
 for(let i=0;i<14;i++){let z=(i-6)*9,x=Math.sin(i*.32)*9,y=24+Math.sin(i*Math.PI/13)*15;const segment=r.group('Segment_'+i,[x,y,z],'body');r.ellipsoid('Segment_core_'+i,[x,y,z],13,11,8,11,segment, false,5,10);r.plate('Amber_carapace_'+i,[x,y+7,z],15,8,10,4,segment);r.plate('Armor_rim_'+i,[x,y+11,z-3],14,2,3,12,segment);r.tooth('Dorsal_barb_'+i,[x,y+14,z],[x,y+27,z+3],3.5,segment);
  for(let side of[-1,1]){let a=[x+side*11,y-4,z],b=[x+side*27,13,z+4],c=[x+side*32,3,z+9],leg=r.group('Leg_'+i+side,a,segment);r.bone('Leg_femur_'+i+side,a,b,2.9,2,4,leg);r.bone('Leg_tibia_'+i+side,b,c,2.1,.8,9,leg);r.tooth('Leg_hook_'+i+side,c,add(c,[side*3,-2,4]),1.4,leg);r.gait(leg,(i%2?1:-1)*side);}
  r.anim('walk',segment,'r',[[0,0,Math.sin(i*.6)*3,0],[.5,0,-Math.sin(i*.6)*3,0],[1,0,Math.sin(i*.6)*3,0]],1,true);
 }
 const head=r.group('head',[0,25,-62],'body');r.ellipsoid('Chitin_mask',[0,27,-69],17,12,15,4,head);r.plate('Crown_rim',[0,37,-67],17,4,13,9,head);for(let side of[-1,1]){r.eye('Amber_eye_'+side,[side*10,32,-81],3.8,head);let jaw=r.group('Mouth_claw_'+side,[side*10,22,-75],head);r.horn('Jaw_curve_'+side,bezier([side*10,22,-75],[side*26,20,-82],[side*23,14,-99],[side*6,17,-99],7),4.5,10,jaw);jointAttack(r,jaw,side*28);r.chain('Antenna_'+side,bezier([side*9,37,-75],[side*17,50,-80],[side*25,52,-95],[side*21,47,-105],9),1.4,13,head);}
 for(let side of[-1,1])r.horn('Forked_tail_'+side,bezier([0,24,68],[side*12,27,81],[side*25,39,91],[side*29,38,101],8),5.5,4);r.idle('body',.6);
}
module.exports={IDS,gems,insectLeg,antler,jointAttack,builders:[beetle,mantis,ray,hydra,centipede]};
