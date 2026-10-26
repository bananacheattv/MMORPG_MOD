const {add,mul,mix,bezier}=require('./rig.cjs');
const {gems,antler,jointAttack}=require('./creatures.cjs');
function jellyfish(r){
 r.ellipsoid('Oracle_bell',[0,120,0],33,20,33,5,'body',false,9,16);r.ellipsoid('Inner_shadow',[0,111,0],23,10,23,15);r.ellipsoid('Suspended_heart',[0,105,-2],12,15,12,14,'body',true,8,12);
 for(let i=0;i<12;i++){let angle=i*Math.PI/6,x=Math.cos(angle)*27,z=Math.sin(angle)*27;let crown=r.group('Crown_spire_'+i,[x,128,z],'body');r.horn('Oracle_crown_'+i,bezier([x,128,z],[x*1.15,143,z*1.15],[x*.9,153,z*.9],[x*.6,157,z*.6],7),3.3,12,crown);gems(r,'Crown_jewel_'+i,[x,131,z],2.2,crown);}
 for(let i=0;i<12;i++){
  let angle=i*Math.PI/6,start=[Math.cos(angle)*26,108,Math.sin(angle)*26],joint=r.group('Tentacle_'+i,start,'body');const path=bezier(start,[Math.cos(angle+.2)*33,77,Math.sin(angle+.2)*33],[Math.cos(angle-.25)*40,39,Math.sin(angle-.25)*40],[Math.cos(angle+.18)*29,7+(i%3)*4,Math.sin(angle+.18)*29],24);r.tube('Ribbon_tentacle_'+i,path,path.map((_,n)=>Math.max(.5,3.4-n*.11)),5,joint,8);
  for(let n=3;n<path.length-2;n+=3){let p=path[n];r.ellipsoid('Luminous_organ_'+i+n,p,2,2.5,2,14,joint,true,4,8);r.horn('Barbed_cilia_'+i+n,[p,add(p,[Math.cos(angle)*4,-4,Math.sin(angle)*4]),add(p,[Math.cos(angle)*7,-1,Math.sin(angle)*7])],1.2,10,joint);}
  r.anim('idle',joint,'r',[[0,(i%2?1:-1)*5,0,-Math.cos(angle)*5],[1.5,-(i%2?1:-1)*6,0,Math.cos(angle)*6],[3,(i%2?1:-1)*5,0,-Math.cos(angle)*5]]);r.anim('walk',joint,'r',[[0,-8,0,0],[.7,9,0,0],[1.4,-8,0,0]],1.4,true);jointAttack(r,joint,24+(i%3)*6);
 }
 r.eye('Oracle_eye',[0,120,-31],9.5);r.bone('Oracle_eye_brow',[-12,128,-27],[12,128,-27],2.1,2.1,12);for(let s of[-1,1])for(let n=0;n<3;n++)r.ellipsoid('Lesser_eye_'+s+n,[s*(15+n*4),116-n*2,-24+n*3],2,2,1.5,14,'body',true);r.idle('body',2.4);
}
function crab(r){
 r.ellipsoid('Armored_carapace',[0,42,3],37,23,32,6,'body',false,8,14);r.ellipsoid('Organic_underbody',[0,29,2],29,12,25,11);
 for(let row=0;row<3;row++)for(let side of[-1,1]){let z=(row-1)*18;r.plate('Bastion_panel_rim_'+side+row,[side*16,58,z],20,9,12,9);r.plate('Bastion_panel_'+side+row,[side*16,60,z],18,8,11,6);for(let t=0;t<3;t++)r.ellipsoid('Rivet_'+side+row+t,[side*(6+t*10),66,z],1.7,1.2,1.7,12);}
 for(let side of[-1,1]){
  for(let i=0;i<4;i++){let z=(i-1.5)*15,a=[side*29,36,z],b=[side*48,23,z+10],c=[side*58,4,z+14],leg=r.group('Bastion_leg_'+side+i,a,'body');r.bone('Leg_armor_'+side+i,a,b,4.3,3.3,6,leg);r.ellipsoid('Leg_joint_'+side+i,b,4,4,4,12,leg);r.bone('Leg_tip_'+side+i,b,c,3.1,1.1,9,leg);r.tooth('Leg_claw_'+side+i,c,add(c,[side*5,-2,3]),2,leg);r.gait(leg,side*(i%2?1:-1));}
  let a=[side*27,43,-20],b=[side*54,43,-36],palm=[side*66,43,-53],arm=r.group('Pincer_arm_'+side,a,'body');r.bone('Arm_sleeve_'+side,a,b,9,6.5,6,arm);r.ellipsoid('Arm_joint_'+side,b,7,7,7,9,arm);r.bone('Forearm_'+side,b,palm,6,8,6,arm);r.ellipsoid('Pincer_palm_'+side,palm,11,13,13,6,arm);
  for(let jaw of[-1,1]){let pivot=add(palm,[jaw*7,0,-6]),claw=r.group('Pincer_finger_'+side+'_'+jaw,pivot,arm),points=bezier(pivot,add(palm,[jaw*16,0,-15]),add(palm,[jaw*12,0,-32]),add(palm,[jaw*2,0,-38]),10);r.tube('Pincer_shell_'+side+ jaw,points,points.map((_,i)=>Math.max(.7,7-i*.63)),6,claw,9);r.horn('Pincer_bone_edge_'+side+jaw,points.map(p=>add(p,[-jaw*2,0,-1])),3.8,10,claw);for(let i=2;i<points.length-2;i+=2)r.tooth('Pincer_tooth_'+side+jaw+i,points[i],add(points[i],[-jaw*5,0,-2]),2,claw);r.anim('attack',claw,'r',[[0,0,0,0],[.22,0,jaw*23,0],[.48,0,-jaw*14,0],[1,0,0,0]],1,false);}
  jointAttack(r,arm,side*19);const stalk=r.group('Eye_stalk_'+side,[side*13,58,-18],'body');r.bone('Eye_stem_'+side,[side*13,58,-18],[side*16,80,-24],2.8,2,9,stalk);r.eye('Bastion_eye_'+side,[side*16,81,-27],4.6,stalk);r.anim('idle',stalk,'r',[[0,0,-5*side,0],[1.5,0,5*side,0],[3,0,-5*side,0]]);
 }
 for(let i=-2;i<=2;i++)antler(r,'Citadel_spike_'+i,[[i*12,61,6],[i*13,77,8],[i*10,85,3]],3.8,'body',9);gems(r,'Fortress_reactor',[0,64,5],7);r.idle('body',.7);
}
function moth(r){
 r.ellipsoid('Velvet_thorax',[0,75,1],12,29,14,7);r.ellipsoid('Segmented_abdomen',[0,42,4],9,23,12,8);for(let i=0;i<6;i++)r.plate('Abdomen_belt_'+i,[0,23+i*7,4],9,2.4,12,12);
 for(let side of[-1,1]){
  const fore=r.group('Forewing_'+side,[side*8,91,7],'body');r.wing('Eclipse_forewing_'+side,[[side*8,93,6],[side*29,133,10],[side*71,148,13],[side*92,129,10],[side*86,111,8],[side*93,95,6],[side*74,88,5],[side*65,69,7],[side*37,66,9],[side*10,75,7]],7,fore,12);
  const rear=r.group('Hindwing_'+side,[side*7,62,9],'body');r.wing('Eclipse_hindwing_'+side,[[side*7,67,8],[side*41,78,14],[side*72,65,13],[side*69,44,10],[side*60,21,8],[side*43,11,6],[side*36,29,8],[side*20,37,7],[side*8,50,8]],7,rear,12);
  for(let [bone,c]of[[fore,[side*55,110,6]],[rear,[side*43,47,6]]]){r.ellipsoid('Wing_eye_socket_'+bone,c,10,13,1,15,bone);r.ellipsoid('Wing_eye_iris_'+bone,add(c,[0,0,-.9]),6.6,9,.6,8,bone);r.ellipsoid('Wing_eye_spark_'+bone,add(c,[0,0,-1.7]),2.7,4,.35,14,bone,true,5,8);}
  for(let bone of[fore,rear]){r.anim('idle',bone,'r',[[0,0,side*6,0],[1.5,0,-side*7,0],[3,0,side*6,0]]);r.anim('walk',bone,'r',[[0,0,-side*24,0],[.45,0,side*21,0],[.9,0,-side*24,0]],.9,true);r.anim('cast',bone,'r',[[0,0,0,0],[.65,0,-side*34,0],[1.8,0,-side*34,0],[2.4,0,0,0]],2.4,false);r.strike(bone,side*21,'y');}
  for(let i=0;i<3;i++){let arm=r.group('Claw_arm_'+side+i,[side*8,78-i*13,-3],'body');r.chain('Claw_arm_curve_'+side+i,[[side*8,78-i*13,-3],[side*21,72-i*13,-9],[side*17,60-i*13,-19]],2.5,9,arm);r.tooth('Moth_claw_'+side+i,[side*17,60-i*13,-19],[side*9,56-i*13,-27],2.5,arm);jointAttack(r,arm,32);}
 }
 const head=r.group('head',[0,107,0],'body');r.ellipsoid('Fur_mask',[0,112,-1],15,13,12,7,head);for(let side of[-1,1]){r.eye('Compound_moth_eye_'+side,[side*9,116,-10],5.5,head);let antenna=r.group('Feather_antenna_'+side,[side*7,122,0],head);const curve=bezier([side*7,122,0],[side*15,143,-3],[side*27,154,-7],[side*33,153,-14],12);r.chain('Antenna_stem_'+side,curve,1.5,12,antenna);for(let i=2;i<curve.length-1;i++){let p=curve[i];r.horn('Antenna_feather_'+side+i,[p,add(p,[side*6,6,1]),add(p,[side*8,8,2])],.8,7,antenna);}r.anim('idle',antenna,'r',[[0,0,side*4,0],[1.5,0,-side*4,0],[3,0,side*4,0]]);}r.idle('body',1.3);
}
function serpent(r){
 const path=[];for(let i=0;i<52;i++){let a=i*.3,radius=25*(1-i/72);path.push([Math.cos(a)*radius,7+i*3.1,Math.sin(a)*radius]);}r.tube('Coiled_scaled_body',path,path.map((_,i)=>10.8-i*.08),8,'body',12);
 for(let i=2;i<path.length-2;i+=2){let p=path[i],a=i*.3;r.ellipsoid('Belly_scutum_'+i,add(p,[Math.cos(a)*5,-1,Math.sin(a)*5]),7,4,4,10);if(i%4===0)r.horn('Golden_dorsal_spike_'+i,[p,add(p,[0,9,0]),add(p,[0,16,5])],3.3,12);}
 for(let ring=0;ring<3;ring++){let y=58+ring*34,bone=r.group('Orbital_crown_'+ring,[0,y,0],'body');const points=[];for(let i=0;i<=32;i++){let a=i*Math.PI/16;points.push([Math.cos(a)*(28+ring*3),y,Math.sin(a)*(28+ring*3)]);}r.tube('Orbit_ring_'+ring,points,points.map(()=>1.3),12,bone,6);for(let i=0;i<8;i++){let a=i*Math.PI/4,p=[Math.cos(a)*(28+ring*3),y,Math.sin(a)*(28+ring*3)];r.ellipsoid('Orbit_rune_'+ring+i,p,2.6,5,1.6,14,bone,true,4,6);}r.anim('idle',bone,'r',[[0,0,0,0],[1.5,0,ring%2?180:-180,0],[3,0,ring%2?360:-360,0]],3,true);}
 const head=r.group('head',[0,166,-12],'body');r.ellipsoid('Royal_skull',[0,167,-16],17,13,21,8,head);r.ellipsoid('Serrated_muzzle',[0,161,-34],14,7,15,8,head);r.plate('Crown_mask',[0,176,-13],17,4,18,9,head);r.ellipsoid('Mouth_shadow',[0,156,-36],12,4,11,11,head);
 const jaw=r.group('jaw',[0,155,-25],head);r.ellipsoid('Lower_jaw',[0,152,-34],13,3.5,14,10,jaw);for(let side of[-1,1]){r.eye('Royal_eye_'+side,[side*12,171,-30],4.8,head);r.bone('Royal_brow_'+side,[side*5,178,-31],[side*16,174,-28],2,1.6,12,head);r.horn('Royal_fang_'+side,bezier([side*8,156,-42],[side*11,146,-46],[side*7,137,-45],[side*5,137,-41],7),3.4,10,head);antler(r,'Crown_horn_'+side,[[side*11,177,-5],[side*22,190,0],[side*24,204,-7],[side*17,210,-13]],4.5,head,12);}
 for(let i=-2;i<=2;i++)r.horn('Crown_tine_'+i,[[i*6,180,-14],[i*7,191+Math.abs(i)*3,-14],[i*5,198+Math.abs(i)*3,-20]],2.1,12,head);r.anim('idle',head,'r',[[0,0,-5,0],[1.5,0,5,0],[3,0,-5,0]]);r.anim('attack',jaw,'r',[[0,0,0,0],[.25,34,0,0],[.48,0,0,0],[1,0,0,0]],1,false);jointAttack(r,head,29);r.idle('body',.5);
}
function worldEye(r){
 r.ellipsoid('Ancient_eye_socket',[0,107,0],32,28,20,9,'body',false,10,16);r.ellipsoid('Ivory_sclera',[0,107,-13],25,21,11,10,'body',false,9,16);r.ellipsoid('Luminous_iris',[0,107,-23],17,19,3.3,14,'body',true,10,16);r.ellipsoid('Vertical_pupil',[0,107,-26.1],4.6,17,1.1,15,'body',false,8,10);
 for(let s of[-1,1]){r.chain('Cruel_eyelid_'+s,bezier([s*26,101,-16],[s*25,125,-17],[s*7,133,-18],[0,130,-18],11),3.1,12);r.horn('Eye_corner_spike_'+s,[[s*26,107,-10],[s*39,110,-4],[s*45,124,0]],5,9);}
 for(let ring=0;ring<3;ring++){
  let radius=43+ring*13,bone=r.group('Celestial_annulus_'+ring,[0,107,0],'body'),points=[];for(let i=0;i<=48;i++){let a=i*Math.PI/24;points.push([Math.cos(a)*radius,107+Math.sin(a)*radius,ring*4+3]);}r.tube('Carved_annulus_'+ring,points,points.map(()=>3.1-ring*.4),ring===1?12:9,bone,8);
  for(let i=0;i<12;i++){let a=i*Math.PI/6,p=[Math.cos(a)*radius,107+Math.sin(a)*radius,ring*4];r.ellipsoid('Annulus_setting_'+ring+i,p,4.6,4.6,2.3,12,bone,false,4,8);r.ellipsoid('Annulus_rune_'+ring+i,add(p,[0,0,-2.1]),2.3,2.3,.8,14,bone,true,4,6);}
  r.anim('idle',bone,'r',[[0,0,0,0],[1.5,0,0,ring%2?15:-15],[3,0,0,0]],3,true);r.anim('cast',bone,'r',[[0,0,0,0],[.7,0,0,(ring+1)*25],[1.8,0,0,-(ring+1)*25],[2.4,0,0,0]],2.4,false);
 }
 for(let i=0;i<10;i++){
  let a=i*Math.PI/5,p=[Math.cos(a)*65,107+Math.sin(a)*65,3],bone=r.group('World_branch_'+i,p,'body');let curve=bezier(p,[Math.cos(a)*81,107+Math.sin(a)*81,8],[Math.cos(a+.08)*94,107+Math.sin(a+.08)*94,4],[Math.cos(a-.05)*105,107+Math.sin(a-.05)*105,-6],9);r.horn('Ancient_branch_'+i,curve,5.8,13,bone);
  for(let j=3;j<curve.length-1;j+=2){let root=curve[j],fork=add(root,[Math.cos(a+.6)*15,Math.sin(a+.6)*15,-3]);r.horn('Fractal_branch_'+i+j,[root,mix(root,fork,.6),fork],2.4,12,bone);r.ellipsoid('Branch_crystal_'+i+j,add(root,[0,0,-2]),2.7,3.4,1.6,14,bone,true,4,6);}
  r.anim('idle',bone,'r',[[0,0,0,-2],[1.5,0,0,2],[3,0,0,-2]]);r.anim('attack',bone,'r',[[0,0,0,0],[.24,0,0,-6],[.48,0,0,9],[1,0,0,0]],1,false);
 }
 r.idle('body',1.6);r.anim('attack','body','p',[[0,0,0,0],[.2,0,0,4],[.5,0,0,-7],[1,0,0,0]],1,false);
}
function crystal(r,material){
 const core=r.group('Crystal_heart',[0,34,0],'body');r.tube('Faceted_capture_crystal',[[0,3,0],[0,14,0],[0,31,0],[0,49,0],[0,64,0]],[.2,10,13,8,.2],material,core,8,true);r.tube('Internal_reactor',[[0,17,0],[0,32,0],[0,47,0]],[1.7,4.1,.3],14,core,6,true);
 for(let i=0;i<4;i++){let a=i*Math.PI/2+Math.PI/4,x=Math.cos(a)*15,z=Math.sin(a)*15;r.chain('Obsidian_cradle_'+i,[[x*.5,8,z*.5],[x,20,z],[x*1.1,39,z*1.1],[x*.65,55,z*.65]],2.1,15);r.horn('Cradle_tip_'+i,[[x*.65,54,z*.65],[x*.45,63,z*.45],[x*.15,67,z*.15]],2.2,12);}
 for(let ring=0;ring<2;ring++){let y=20+ring*24,joint=r.group('Gyroscope_'+ring,[0,y,0],'body'),points=[];for(let i=0;i<=32;i++){let a=i*Math.PI/16;points.push([Math.cos(a)*19,y+Math.sin(a)*4,Math.sin(a)*19]);}r.tube('Engraved_gyroscope_'+ring,points,points.map(()=>1.7),12,joint,6);for(let i=0;i<8;i++){let a=i*Math.PI/4;r.ellipsoid('Rune_node_'+ring+i,[Math.cos(a)*19,y+Math.sin(a)*4,Math.sin(a)*19],2,3,2,material,joint,true,4,6);}r.anim('idle',joint,'r',[[0,0,0,0],[1.5,0,ring?180:-180,0],[3,0,ring?360:-360,0]],3,true);}
 for(let i=0;i<6;i++){let a=i*Math.PI/3,joint=r.group('Orbiting_shard_'+i,[0,34,0],'body'),x=Math.cos(a)*28,z=Math.sin(a)*28;r.tube('Floating_shard_'+i,[[x,22,z],[x,31,z],[x,43,z]],[.15,4.5,.15],material,joint,6,true);r.anim('idle',joint,'r',[[0,0,0,0],[1.5,0,180,0],[3,0,360,0]],3,true);}
 r.anim('idle',core,'r',[[0,0,0,0],[1.5,0,180,0],[3,0,360,0]],3,true);r.idle('body',1.1);
}
module.exports={builders:[jellyfish,crab,moth,serpent,worldEye],crystal};
