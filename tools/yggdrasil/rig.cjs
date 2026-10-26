const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const uid=s=>{let h=crypto.createHash('sha256').update(s).digest('hex').slice(0,32);return h.slice(0,8)+'-'+h.slice(8,12)+'-5'+h.slice(13,16)+'-a'+h.slice(17,20)+'-'+h.slice(20);};
const ROOT=path.resolve(__dirname,'../..');
const ATLAS=path.join(ROOT,'src/main/resources/assets/mmorpg/textures/entity/yggdrasil/material_atlas.png');
const png=fs.readFileSync(ATLAS),W=png.readUInt32BE(16),H=png.readUInt32BE(20),T=W/4;
const add=(a,b)=>a.map((x,i)=>x+b[i]),sub=(a,b)=>a.map((x,i)=>x-b[i]),mul=(a,n)=>a.map(x=>x*n),dot=(a,b)=>a.reduce((s,x,i)=>s+x*b[i],0);
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],len=a=>Math.sqrt(dot(a,a)),norm=a=>mul(a,1/(len(a)||1));
const mix=(a,b,t)=>add(mul(a,1-t),mul(b,t));
function bezier(a,b,c,d,n=12){const out=[];for(let i=0;i<n;i++){let t=i/(n-1),s=1-t;out.push(add(add(mul(a,s*s*s),mul(b,3*s*s*t)),add(mul(c,3*s*t*t),mul(d,t*t*t))));}return out;}
function rounded(n){return Math.round(n*10000)/10000;}
function clean(value){if(typeof value==='number'){if(!Number.isFinite(value))throw Error('Invalid geometry');return rounded(value);}if(Array.isArray(value))return value.map(clean);if(value&&typeof value==='object')return Object.fromEntries(Object.entries(value).map(([k,v])=>[k,clean(v)]));return value;}
class Rig {
 constructor(id,material){this.id=id;this.material=material;this.seq=0;this.groups=new Map();this.elements=[];this.animations=new Map();this.group('root',[0,0,0],null);this.group('body',[0,50,0]);}
 group(name,origin,parent='root'){if(this.groups.has(name))throw Error('Duplicate bone '+name);const g={uuid:uid(this.id+'__'+name),name,origin,rotation:[0,0,0],children:[]};this.groups.set(name,g);if(parent)this.groups.get(parent).children.push(g);return name;}
 tile(material){return[(material%4)*T+12,Math.floor(material/4)*T+12,T-24];}
 mesh(name,vertices,faces,material=this.material,parent='body',glow=false){
  const id=uid(this.id+'__mesh_'+this.seq++),v=Object.fromEntries(vertices.map((p,i)=>['v'+i,p]));
  const faceObject={};let n=0;for(const f of faces){let keys=f.indices.map(i=>'v'+i);faceObject['f'+n++]={vertices:keys,uv:Object.fromEntries(keys.map((key,i)=>[key,f.uv[i]])),texture:0};}
  const mesh={uuid:id,name,type:'mesh',origin:[0,0,0],vertices:v,faces:faceObject,emissive:glow,material};this.elements.push(mesh);this.groups.get(parent).children.push(mesh);return mesh;
 }
 tube(name,points,radii,material=this.material,parent='body',sides=10,glow=false,flatten=1){
  const vertices=[],faces=[],dist=[0];for(let i=1;i<points.length;i++)dist.push(dist[i-1]+len(sub(points[i],points[i-1])));
  const [u,v,size]=this.tile(material),maxRadius=Math.max(...radii),us=Math.min(size,Math.PI*2*maxRadius*2),vs=Math.min(size,dist.at(-1)*2);
  let lastNormal=null;
  const closed=len(sub(points[0],points.at(-1)))<.00001;
  for(let i=0;i<points.length;i++){
   const tangent=norm(closed&&(i===0||i===points.length-1)?sub(points[1],points[points.length-2]):sub(points[Math.min(points.length-1,i+1)],points[Math.max(0,i-1)]));
   let n=lastNormal?norm(sub(lastNormal,mul(tangent,dot(lastNormal,tangent)))):norm(cross(tangent,Math.abs(tangent[1])>.9?[1,0,0]:[0,1,0]));if(len(n)<.5)n=norm(cross(tangent,Math.abs(tangent[1])>.9?[1,0,0]:[0,1,0]));lastNormal=n;const b=norm(cross(tangent,n));
   for(let s=0;s<sides;s++){let a=s*Math.PI*2/sides;vertices.push(add(points[i],add(mul(n,Math.cos(a)*Math.max(.12,radii[i])),mul(b,Math.sin(a)*Math.max(.12,radii[i])*flatten))));}
   if(i>0)for(let s=0;s<sides;s++){const indices=[(i-1)*sides+s,(i-1)*sides+(s+1)%sides,i*sides+(s+1)%sides,i*sides+s];faces.push({indices,uv:[[u+s/sides*us,v+dist[i-1]/dist.at(-1)*vs],[u+(s+1)/sides*us,v+dist[i-1]/dist.at(-1)*vs],[u+(s+1)/sides*us,v+dist[i]/dist.at(-1)*vs],[u+s/sides*us,v+dist[i]/dist.at(-1)*vs]]});}
  }
  for(const end of closed?[]:[0,points.length-1]){const center=vertices.length;vertices.push(points[end]);for(let s=0;s<sides;s++){const a=s*Math.PI*2/sides,b=(s+1)*Math.PI*2/sides,indices=end===0?[center,end*sides+(s+1)%sides,end*sides+s]:[center,end*sides+s,end*sides+(s+1)%sides];const radial=k=>[u+size*.5+Math.cos(k)*Math.min(size*.4,maxRadius*2),v+size*.5+Math.sin(k)*Math.min(size*.4,maxRadius*2)];faces.push({indices,uv:end===0?[[u+size*.5,v+size*.5],radial(b),radial(a)]:[[u+size*.5,v+size*.5],radial(a),radial(b)]});}}
  return this.mesh(name,vertices,faces,material,parent,glow);
 }
 ellipsoid(name,c,rx,ry,rz,material=this.material,parent='body',glow=false,rings=7,sides=12){
  const points=[],radii=[];for(let i=0;i<=rings;i++){const y=-1+2*i/rings;points.push(add(c,[0,ry*y,0]));radii.push(Math.max(.15,Math.sqrt(Math.max(0,1-y*y))*rz));}return this.tube(name,points,radii,material,parent,sides,glow,rx/rz);
 }
 horn(name,points,radius,material=10,parent='body',glow=false){return this.tube(name,points,points.map((_,i)=>Math.max(.12,radius*Math.pow(1-i/(points.length-1),.85))),material,parent,7,glow);}
 bone(name,a,b,r1,r2,material=this.material,parent='body',sides=8){return this.tube(name,[a,mix(a,b,.16),mix(a,b,.82),b],[r1*.9,r1,r2,r2*.9],material,parent,sides);}
 box(name,c,w,h,d,material=this.material,parent='body',glow=false){const [u,v,size]=this.tile(material),verts=[];for(let i=0;i<8;i++)verts.push(add(c,[(i&1?w:-w)/2,(i&2?h:-h)/2,(i&4?d:-d)/2]));const faces=[[0,2,3,1],[4,5,7,6],[0,1,5,4],[2,6,7,3],[0,4,6,2],[1,3,7,5]].map((indices,i)=>({indices,uv:[[u,v],[u+Math.min(size,(i<2?w:d)*2),v],[u+Math.min(size,(i<2?w:d)*2),v+Math.min(size,(i<2?h:w)*2)],[u,v+Math.min(size,(i<2?h:w)*2)]]}));return this.mesh(name,verts,faces,material,parent,glow);}
 plate(name,c,rx,ry,rz,material=this.material,parent='body'){return this.ellipsoid(name,c,rx,ry,rz,material,parent,false,4,8);}
 eye(name,c,size,parent='body',iris=14){this.ellipsoid(name+'_socket',add(c,[0,0,1]),size*1.35,size*.9,size*.55,15,parent,false,5,10);this.ellipsoid(name+'_iris',c,size,size*.72,size*.35,iris,parent,true,6,12);this.ellipsoid(name+'_pupil',add(c,[0,0,-size*.34]),size*.23,size*.64,size*.1,15,parent,false,4,8);this.box(name+'_glint',add(c,[-size*.29,size*.24,-size*.44]),size*.2,size*.2,.3,14,parent,true);}
 tooth(name,a,b,r,parent='body'){this.horn(name,[a,mix(a,b,.5),b],r,10,parent);}
 chain(name,points,radius,mat=this.material,parent='body'){return this.tube(name,points,points.map((_,i)=>radius*(1-.55*i/(points.length-1))),mat,parent,10);}
 wing(name,points,material,parent='body',vein=12){
  const center=mul(points.reduce(add,[0,0,0]),1/points.length),vertices=[center,...points],faces=[],[u,v,size]=this.tile(material);const minX=Math.min(...points.map(p=>p[0])),maxX=Math.max(...points.map(p=>p[0])),minY=Math.min(...points.map(p=>p[1])),maxY=Math.max(...points.map(p=>p[1]));const uv=p=>[u+(p[0]-minX)/Math.max(1,maxX-minX)*size,v+(maxY-p[1])/Math.max(1,maxY-minY)*size];
  for(let i=0;i<points.length;i++){const a=1+i,b=1+(i+1)%points.length;faces.push({indices:[0,a,b],uv:[uv(center),uv(points[i]),uv(points[(i+1)%points.length])]},{indices:[0,b,a],uv:[uv(center),uv(points[(i+1)%points.length]),uv(points[i])]});}
  this.mesh(name+'_membrane',vertices,faces,material,parent);for(let i=0;i<points.length;i++){if(i%2===0)this.bone(name+'_vein_'+i,center,points[i],1.1,.4,vein,parent,6);}
  for(let i=0;i<points.length;i++)this.bone(name+'_edge_'+i,points[i],points[(i+1)%points.length],.8,.6,vein,parent,6);
 }
 anim(name,bone,channel,keys,length=3,loop=true){let animation=this.animations.get(name);if(!animation){animation={name,length,loop,tracks:[]};this.animations.set(name,animation);}if(animation.length!==length)keys=keys.map(k=>[k[0]*animation.length/length,...k.slice(1)]);animation.tracks.push({bone:this.groups.get(bone).uuid,channel,keys});}
 idle(bone='body',amount=.7){this.anim('idle',bone,'p',[[0,0,0,0],[1.5,0,amount,0],[3,0,0,0]]);}
 gait(bone,phase=1,axis='x'){const a=axis==='x'?0:2;const frames=[[0,0,0,0],[.45,0,0,0],[.9,0,0,0]];frames[0][a+1]=-17*phase;frames[1][a+1]=17*phase;frames[2][a+1]=-17*phase;this.anim('walk',bone,'r',frames,.9,true);}
 strike(bone,amount=40,axis='x'){let keys=[[0,0,0,0],[.24,0,0,0],[.48,0,0,0],[1,0,0,0]],a=axis==='x'?1:axis==='y'?2:3;keys[1][a]=-amount*.5;keys[2][a]=amount;this.anim('attack',bone,'r',keys,1,false);}
 finish(){
  if(!this.animations.has('idle'))this.idle();if(!this.animations.has('walk'))this.anim('walk','body','p',[[0,0,0,0],[.45,0,1,0],[.9,0,0,0]],.9,true);
  if(!this.animations.has('attack'))this.strike('body',9);this.anim('cast','body','p',[[0,0,0,0],[.7,0,3,0],[1.8,0,3,0],[2.4,0,0,0]],2.4,false);
  this.anim('death','root','r',[[0,0,0,0],[1,0,0,75]],1,false);
  const animations=[...this.animations.values()].map(a=>({name:a.name,length:a.length,loop:a.loop,tracks:a.tracks.map(t=>t.bone+'|'+t.channel+'|'+t.keys.map(k=>k.join(',')+',c').join(';')).join('\n')}));
  const data=clean({resolution:{width:W,height:H},texture:'mmorpg:textures/entity/yggdrasil/material_atlas.png',parts:[this.groups.get('root')],animations,author:'Aeloria',revision:2,description:'Original articulated polygon mesh with continuous cylindrical and planar UVs.'});
  const output=path.join(ROOT,'src/main/resources/assets/mmorpg/bbmodels/yggdrasil',this.id+'.json');fs.mkdirSync(path.dirname(output),{recursive:true});fs.writeFileSync(output,JSON.stringify(data));
  const outliner=g=>({name:g.name,origin:g.origin,rotation:g.rotation,uuid:g.uuid,children:g.children.map(c=>c.children?outliner(c):c.uuid)});
  const bbAnimations=[...this.animations.values()].map(a=>{const animators={};for(const t of a.tracks){const animator=animators[t.bone]??={name:[...this.groups.values()].find(g=>g.uuid===t.bone)?.name,type:'bone',keyframes:[]};animator.keyframes.push(...t.keys.map((k,i)=>({channel:t.channel==='r'?'rotation':t.channel==='p'?'position':'scale',data_points:[{x:String(k[1]),y:String(k[2]),z:String(k[3])}],time:k[0],interpolation:'catmullrom',uuid:uid(this.id+'_'+a.name+'_'+t.bone+'_'+t.channel+'_'+i)})));}return{name:a.name,loop:a.loop?'loop':'once',length:a.length,uuid:uid(this.id+'_'+a.name),animators};});
  const bb={meta:{format_version:'4.10',model_format:'free',box_uv:false},name:this.id,resolution:{width:W,height:H},elements:this.elements.map(e=>({...e,visibility:true,autouv:0})),outliner:[outliner(this.groups.get('root'))],textures:[{path:'../material_atlas.png',relative_path:'../material_atlas.png',name:'material_atlas.png',uuid:uid('aeloria-yggdrasil-materials-v2'),id:'0',width:W,height:H,uv_width:W,uv_height:H}],animations:bbAnimations};
  const project=path.join(ROOT,'art/yggdrasil/models',this.id+'.bbmodel');fs.mkdirSync(path.dirname(project),{recursive:true});fs.writeFileSync(project,JSON.stringify(clean(bb)));
  const points=this.elements.flatMap(e=>Object.values(e.vertices));const bounds={min:[0,1,2].map(a=>Math.min(...points.map(p=>p[a]))),max:[0,1,2].map(a=>Math.max(...points.map(p=>p[a])))};
  return{id:this.id,bones:this.groups.size,meshes:this.elements.length,faces:this.elements.reduce((n,e)=>n+Object.keys(e.faces).length,0),vertices:this.elements.reduce((n,e)=>n+Object.keys(e.vertices).length,0),emissive:this.elements.filter(e=>e.emissive).length,animations:animations.map(a=>a.name),materials:[...new Set(this.elements.map(e=>e.material))],bounds};
 }
}
module.exports={Rig,ROOT,ATLAS,add,sub,mul,norm,cross,mix,bezier};
