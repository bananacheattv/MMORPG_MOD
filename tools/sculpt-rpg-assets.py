"""Original Aeloria sculpted reliquaries, chests and weapons. Reproducible geometry and painted material atlas."""
from pathlib import Path
import json,math,re,random
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources/assets/mmorpg';MODELS=RES/'models'
COLORS=['#272039','#D6AC59','#A77547','#C6D1DE','#9952DD','#63B5E1','#4F8760','#604735','#ED683C','#B1DFEA','#151224','#614334','#E5DCC6','#43325E','#EBCB76','#638A99']
img=Image.new('RGB',(1024,1024));px=img.load();rnd=random.Random(20261028)
for m,h in enumerate(COLORS):
 base=tuple(int(h[i:i+2],16)for i in (1,3,5));ox=m%4*256;oy=m//4*256
 for y in range(256):
  for x in range(256):
   grain=rnd.uniform(-.045,.045);bevel=.17 if min(x,y,255-x,255-y)<3 else 0
   if m in (1,2,3,12,14):
    line=.06*math.sin(y*.12)+.025*math.sin(x*.8+y*.04)
    # Engraved diagonal lattice and recessed decorative border.
    if (x+y)%48<2 or (x-y)%48<2:line-=.17
    if x%48 in (5,6) and y%48 in (5,6):line+=.3
   elif m in (6,13):line=(.075 if (x//2+y//2)%2 else -.06)+(.1 if x%24<2 else 0)
   elif m in (7,11):
    line=.08*math.sin(x*.17+math.sin(y*.03)*2)-(.2 if x%57<2 else 0)
    if x%57 in (5,6) and y%12<5:line+=.22
   elif m in (4,5,8,9):line=.13*math.sin(x*.04+y*.05)+(.22 if (x+2*y)%83<2 else 0)
   else:line=.045*math.sin(x*.13+y*.07)-(.12 if (x+y)%61<2 else 0)
   px[ox+x,oy+y]=tuple(max(0,min(255,int(c*(1+grain+line+bevel))))for c in base)
tex=RES/'textures/item/aeloria_materials.png';tex.parent.mkdir(parents=True,exist_ok=True);img.save(tex)
armor_tex=RES/'textures/entity/armor/aeloria_sculpted_hd.png';armor_tex.parent.mkdir(parents=True,exist_ok=True);img.save(armor_tex)
block_tex=RES/'textures/block/aeloria_materials.png';block_tex.parent.mkdir(parents=True,exist_ok=True);img.save(block_tex)
FACES=['north','south','east','west','up','down']
def box(a,b,m,name='',rotation=None,glow=False):
 u=(m%4)*4+.12;v=(m//4)*4+.12
 e={'name':name,'from':[round(x,4)for x in a],'to':[round(x,4)for x in b],'faces':{f:{'texture':'#atlas','uv':[u,v,u+3.76,v+3.76]}for f in FACES}}
 if rotation:e['rotation']=rotation
 if glow:e['light_emission']=12
 return e
def B(c,s,m,name='',axis=None,angle=0):
 return box([c[i]-s[i]/2 for i in range(3)],[c[i]+s[i]/2 for i in range(3)],m,name,{'origin':c,'axis':axis,'angle':angle}if axis and angle else None,m in (4,5,8,9,14))
def write(p,d):
 if p.parent==MODELS/'block' and 'textures'in d:d['textures']['atlas']='mmorpg:block/aeloria_materials'
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def model(elements,display=None):return {'credit':'Aeloria · original sculpted geometry','textures':{'atlas':'mmorpg:item/aeloria_materials','particle':'#atlas'},'elements':elements,'display':display or {'gui':{'rotation':[22,35,0],'translation':[0,0,0],'scale':[.8,.8,.8]},'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.4,.4,.4]}}}
def question(elements,m,front=2.7):
 pattern=['111','001','011','010','000','010']
 for row,line in enumerate(pattern):
  for col,p in enumerate(line):
   if p=='1':
    x=6.8+col*1.1;y=11-row*1.1
    elements.append(B([x,y,front],[.88,.88,.5],m,'Raised question rune'))
    elements.append(B([16-x,y,16-front],[.88,.88,.5],m,'Raised question rune back'))
    elements.append(B([front,y,x],[.5,.88,.88],m,'Raised question rune left'))
    elements.append(B([16-front,y,16-x],[.5,.88,.88],m,'Raised question rune right'))
# Mystery dice: beveled amethyst body, inset faces, gold corner fittings, physical question marks.
E=[B([8,8,8],[10,10,10],0,'Amethyst shell'),B([8,8,8],[10.8,8.5,8.5],13),B([8,8,8],[8.5,10.8,8.5],13),B([8,8,8],[8.5,8.5,10.8],13)]
for x in [3,13]:
 for z in [3,13]:
  E.append(B([x,8,z],[1.2,11.2,1.2],1,'Gold corner spine'))
  for y in [3,13]:E.append(B([x,y,z],[1.7,1.7,1.7],1,'Faceted corner'))
for y in [3,13]:
 for x in [3,13]:E.append(B([x,y,8],[.7,.7,10],1))
 for z in [3,13]:E.append(B([8,y,z],[10,.7,.7],1))
question(E,14);d=model(E);d['display']['fixed']['scale']=[1.25]*3;write(MODELS/'item/cube_mystere.json',d);write(RES/'items/cube_mystere.json',{'model':{'type':'minecraft:model','model':'mmorpg:item/cube_mystere'}})
for key,body,trim in [('celeste',5,1),('sylvestre',6,2),('infernal',8,10),('givre',9,3),('neant',4,10)]:
 themed=json.loads(json.dumps(d))
 for e in themed['elements']:
  m=trim if any(t in e.get('name','')for t in ('corner','Corner','Faceted')) else 14 if 'question'in e.get('name','')else body
  u=m%4*4+.12;v=m//4*4+.12
  for face in e['faces'].values():face['uv']=[u,v,u+3.76,v+3.76]
 write(MODELS/f'item/ui_mystery_{key}.json',themed);write(RES/f'items/ui_mystery_{key}.json',{'model':{'type':'minecraft:model','model':f'mmorpg:item/ui_mystery_{key}'}})
# Five different, immediately readable relic silhouettes with hand-built frames and raised front runes.
for tier,(key,body,gem) in enumerate([('celeste',3,5),('sylvestre',7,6),('infernal',10,8),('givre',9,5),('neant',0,4)]):
 E=[B([8,1.3,8],[13,2,13],body,'Beveled foot'),B([8,2.6,8],[14,1,11],1),B([8,2.6,8],[11,1,14],1),B([8,8,8],[9.8,9.5,9.8],body,'Carved body'),B([8,13,8],[12.5,1.3,12.5],body,'Sculpted crown')]
 for x in [2.6,13.4]:
  for z in [2.6,13.4]:
   E+=[B([x,7.8,z],[1.2,10.5,1.2],1,'Rib'),B([x,13,z],[2.2,2,2.2],1,'Capital'),B([x,3,z],[2,2,2],1)]
 for y in [4,11.8]:
  for z in [2.6,13.4]:E.append(B([8,y,z],[10.8,.6,.6],1,'Engraved face border'))
  for x in [2.6,13.4]:E.append(B([x,y,8],[.6,.6,10.8],1))
 question(E,gem,2.8)
 E+=[B([8,14,8],[4,1,4],1),B([8,16,8],[2.4,3.5,2.4],gem,'Suspended keystone','y',45)]
 if tier==0:
  for side in [-1,1]:
   for j in range(3):E.append(B([8+side*(5+j),13.5+j*.75,8],[1.5,4-j*.7,3.2],3,'Celestial wing','z',side*22.5))
 elif tier==1:
  for side in [-1,1]:
   for j in range(4):E.append(B([8+side*(5.9-j*.45),4+j*2.6,7],[2,3,2],7,'Interwoven root','z',side*22.5))
   E.append(B([8+side*4.8,14,8],[5,1.5,5],6,'Leaf canopy','z',side*22.5))
 elif tier==2:
  for side in [-1,1]:
   E+=[B([8+side*5.8,15,6],[1.8,5,2],10,'Obsidian horn','z',side*22.5),B([8+side*5.8,17,6],[.8,2,.8],8,'Ember horn tip')]
  for z in [4,12]:E.append(B([8,2,z],[9,.5,1],8,'Molten inlay'))
 elif tier==3:
  for x,z in [(3,8),(13,8),(8,3),(8,13)]:
   E+=[B([x,14.5,z],[2,6,2],9,'Ice prism','y',45),B([x,18,z],[.8,1.5,.8],9,'Tapered ice tip')]
 else:
  for j in range(4):
   a=j*math.pi/2+.4;E.append(B([8+math.cos(a)*7,12+j*.8,8+math.sin(a)*7],[2.4,3.3,2.4],0,'Fractured obelisk','y',22.5))
  E.append(B([8,18,8],[1.5,2.5,1.5],4,'Void heart','y',45))
 write(MODELS/f'block/lucky_{key}.json',model(E))
# Seven treasure chests: hollow coffer, curved hinged lid, metal ribs, recessed lock and handles.
for tier,(key,wood,metal,gem) in enumerate([('vote',6,2,6),('quete',7,2,14),('commune',7,3,3),('rare',0,3,5),('epique',13,1,4),('legendaire',7,1,14),('mythique',10,1,8)]):
 for opened in [False,True]:
  E=[B([8,1.5,8],[14,2,12],metal,'Plinth'),B([8,2.7,8],[12,1,10],13,'Velvet lining'),B([8,5.8,2.5],[14,6,1],wood,'Front carved planks'),B([8,5.8,13.5],[14,6,1],wood,'Back planks'),B([1.5,5.8,8],[1,6,10],wood),B([14.5,5.8,8],[1,6,10],wood)]
  for x in [1.5,14.5]:
   for z in [2.5,13.5]:
    E+=[B([x,5,z],[1.2,7,1.2],metal,'Corner ironwork'),B([x,1,z],[2.2,2,2.2],metal,'Claw foot')]
  for x in [4,12]:
   E.append(B([x,5,1.85],[1,6,.5],metal,'Front strap'))
   for y in [3,7]:E.append(B([x,y,1.45],[.5,.5,.4],1,'Rivet'))
  for x in [.7,15.3]:
   E+=[B([x,5,8],[.6,.6,5],metal,'Handle grip'),B([x,7,8],[.6,.6,5],metal)]
   for z in [5.5,10.5]:E.append(B([x,6,z],[.6,2,.6],metal))
  lid=[]
  for j in range(6):
   z=3+j*2;h=1.1+2.8*math.sin((j+.5)*math.pi/6)
   lid.append(B([8,9+h/2,z],[14,h,1.92],wood,'Arched lid slat'))
   for x in [4,12]:lid.append(B([x,9+h+.2,z],[1.2,.6,2],metal,'Curved lid strap'))
  for x in [3,13]:lid.append(B([x,9.2,13.9],[2,1.5,1.3],metal,'Back hinge'))
  lid.append(B([8,11,8],[3.5,1,3.5],metal,'Heraldic setting','y',45));lid.append(B([8,12.5,8],[2,2,2],gem,'Crown jewel','y',45))
  for e in lid:
   if opened:e['rotation']={'origin':[8,9,14],'axis':'x','angle':-45}
  E+=lid;E+=[B([8,7,1.4],[3.6,4,.6],metal,'Escutcheon'),B([8,7.4,.95],[1.8,1.8,.5],gem,'Enamel seal','z',45),B([8,5.8,.9],[.5,1.4,.4],10,'Keyhole')]
  if tier>=4:
   for side in [-1,1]:E.append(B([8+side*2.8,7.5,1.7],[1,3,.6],metal,'Heraldic wing','z',side*22.5))
  if tier==6:
   for side in [-1,1]:E.append(B([8+side*5,12,6],[1.3,4.5,1.3],gem,'Drake horn','z',side*22.5))
  if opened:
   for j in range(5):E.append(B([4+j*2,4,6+(j%2)*3],[2,.8,2],1,'Treasure within'))
  for e in E:
   e['from']=[e['from'][0]*2-8,e['from'][1]*2,e['from'][2]*2-8];e['to']=[e['to'][0]*2-8,e['to'][1]*2,e['to'][2]*2-8]
   if 'rotation'in e:o=e['rotation']['origin'];e['rotation']['origin']=[o[0]*2-8,o[1]*2,o[2]*2-8]
  d=model(E);d['display']['gui']['scale']=[.38]*3;d['display']['gui']['translation']=[0,-3,0];d['display']['fixed']['scale']=[.45]*3
  write(MODELS/'block'/f'caisse_{key}_bloc{"_ouverte"if opened else""}.json',d)
print('Reliquaries, mystery cube and fourteen chest states generated')
# All class weapons receive original volumetric silhouettes rather than flat handheld sprites.
source=(ROOT/'src/main/java/com/mmorpg/item/ItemDefs.java').read_text(encoding='utf-8');weapons=re.findall(r'w\("([^"]+)",\s*(\w+),\s*WeaponKind\.(\w+),\s*(\d+)',source)
for id,cls,kind,rawlevel in weapons:
 level=int(rawlevel);rank=0 if level<10 else 1 if level<25 else 2 if level<45 else 3 if level<65 else 4 if level<85 else 5;metal=2 if rank==0 else 3 if rank<3 else 1;gem={'G':8,'M':4,'A':6,'T':5}.get(cls,4)
 def weapon(pull=0):
  E=[B([8,4,8],[1.6,8,1.6],11,'Leather grip'),B([8,.5,8],[2.5,1.4,2.5],metal,'Pommel'),B([8,8,8],[2.7,1,2.7],metal,'Grip ferrule')]
  for j in range(5):E.append(B([8,1+j*1.4,8],[1.85,.28,1.85],2,'Spiral grip binding'))
  if kind=='SWORD':
   length=17+rank*.8
   for j in range(8):
    w=2.4 if j<5 else max(.45,2.4-(j-4)*.6);E.append(B([8,10+j*length/8,8],[w,length/8+.1,.72],3,'Forged tapered blade'))
    if rank>0:E.append(B([8,10+j*length/8,7.56],[.35,length/8+.05,.12],10 if rank<3 else gem,'Recessed fuller'))
   E.append(B([8,9,8],[8+rank*.45,.8,1.8],metal,'Swept crossguard'))
   for side in [-1,1]:E.append(B([8+side*(3.8+rank*.2),9.7,8],[1.7,2.7,2],metal,'Quillon','z',side*22.5))
   E.append(B([8,9.5,7],[1.6,1.6,.8],gem,'Guard cabochon','z',45))
  elif kind=='AXE':
   E.append(B([8,15.5,8],[1.7,15,1.7],7,'Hickory haft'))
   for side in [-1,1]if rank>=3 else[1]:
    for j in range(5):E.append(B([8+side*(2+j*.8),22+(j-2)*.45,8],[1.4,7-abs(j-2)*.6,1.5],3,'Scalloped axe blade','z',side*22.5 if j>2 else 0))
   E+=[B([8,22,8],[3.5,5,3],metal,'Axe collar'),B([8,22,6.2],[1.8,2,.7],gem,'Rune socket','z',45)]
  elif kind=='HAMMER':
   E.append(B([8,15,8],[1.9,15,1.9],7,'Bound haft'))
   E+=[B([8,24,8],[10+rank*.6,6.4,5.5],0 if rank>2 else 3,'Hammer anvil'),B([8,24,8],[7+rank*.6,7.4,4.4],metal,'Beveled crown'),B([8,24,8],[7+rank*.6,5.4,6.4],metal,'Edge bevel')]
   for side in [-1,1]:
    E.append(B([8+side*(5.1+rank*.3),24,8],[1.4,5,5],3,'Striking face'))
    for y in [21.5,26.5]:E.append(B([8+side*3.7,y,5.2],[.6,.6,.5],1,'Boss rivet'))
   E.append(B([8,24,4.5],[3,3,1.3],gem,'Enamel core','z',45))
  elif kind=='STAFF':
   E=[B([8,10.5,8],[1.25,26,1.25],7,'Carved ash shaft'),B([8,-2.3,8],[2,1.4,2],metal,'Ferrule'),B([8,5.5,8],[1.65,7,1.65],11,'Leather handgrip')]
   for y in [2,3.5,5,6.5,8,20,22]:E.append(B([8,y,8],[1.9,.5,1.9],metal,'Engraved collar'))
   E+=[B([8,24.3,8],[5,.7,5],metal,'Crown cradle','y',45),B([8,27.4,8],[3.5,5,3.5],gem,'Faceted focus','y',45),B([8,30.2,8],[1.5,1.7,1.5],gem,'Focus tip','y',45)]
   for side in [-1,1]:
    E+=[B([8+side*2.9,25.5,8],[.9,5.5,.9],metal,'Curved focus prong','z',-side*22.5),B([8+side*3.6,28,8],[.8,2.8,.8],metal,'Crown finial')]
   if rank>=2:
    for j in range(4):a=j*math.pi/2;E.append(B([8+math.cos(a)*2.7,26,8+math.sin(a)*2.7],[1,1.5,1],gem,'Orbiting focus seal','y',45))
  elif kind=='BOW':
   E=[B([9,8,8],[2.2,5,2],11,'Leather riser')]
   for side in [-1,1]:
    for j in range(7):
     y=8+side*(3+j*2.4);x=9-math.sin(j/6*math.pi*.85)*4
     E.append(B([x,y,8],[1.5,2.9,1.7],7 if rank<2 else 0,'Recurve limb','z',side*22.5 if j<4 else -side*22.5))
     if j%2==0:E.append(B([x,y,7],[1.8,.5,.45],metal,'Limb binding'))
    for j in range(12):
     f=j/11;y=8+side*f*17.4;x=(3.8-pull*1.4)*(1-f)+7.2*f;E.append(B([x,y,8],[.12,1.8,.12],12,'Taut bowstring'))
   E.append(B([9,8,6.8],[1.7,2.5,.6],gem,'Riser jewel','z',45))
   if pull>0:E+=[B([1.5,8,8],[15,0.2,.2],7,'Nocked arrow'),B([-6.5,8,8],[1.3,.7,.7],3,'Arrowhead','z',45)]
  else:raise ValueError(kind)
  if kind=='SWORD'and rank>=2:
   for side in [-1,1]:
    for j in range(rank-1):E.append(B([8+side*(1.4+j*.15),12+j*1.7,8],[1.3,2.4,1],metal,'Layered blade root','z',side*22.5))
    if rank>=4:
     for j in range(3):E.append(B([8+side*(3.8+j*.8),10+j*.65,8],[1.2,3.5-j*.7,1.6],metal,'Celestial guard wing','z',-side*22.5))
   for j in range(rank):E.append(B([8,13+j*2,7.38],[.6,.6,.25],gem,'Engraved luminous rune','z',45))
  if kind=='STAFF'and rank>=3:
   for side in [-1,1]:
    E.append(B([8+side*3.8,27,9],[.7,6,.7],metal,'Halo side'))
    for sign in [-1,1]:E.append(B([8+side*2.7,27+sign*3,9],[.7,3.5,.7],metal,'Octagonal focus halo','z',side*sign*45))
   E.append(B([8,31,9],[3,.6,.7],metal,'Halo crown'))
  if kind=='HAMMER'and rank>=3:
   for side in [-1,1]:
    for y in [21.4,26.6]:E.append(B([8+side*5.3,y,8],[3,.7,6.2],metal,'Striking plate bevel'))
  if kind=='BOW'and rank>=2:
   for side in [-1,1]:E.append(B([6.4,8+side*18,8],[3,1,2],metal,'Recurve tip cap','z',side*22.5))
  if rank==0:
   # Apprentice equipment is deliberately practical: small focus, plain guard, no jewels.
   E=[e for e in E if e.get('name') not in ('Guard cabochon','Rune socket','Enamel core','Riser jewel','Curved focus prong','Crown finial','Focus tip')]
   if kind=='STAFF':
    for e in E:
     if e.get('name')=='Faceted focus':e['from']=[7,24,7];e['to']=[9,27,9]
  if rank>=4:
   for j in range(rank+1):
    y=3+j*1.2;E.append(B([8,y,6.95],[.45,.65,.4],gem,'Inlaid grip sigil','z',45))
   if kind in ('SWORD','STAFF'):
    for side in [-1,1]:
     for j in range(3):E.append(B([8+side*(2.1+j*.7),26+j*1.1 if kind=='STAFF'else 17+j*2,8],[.7,2.5,.8],metal,'Ascended forked crown','z',-side*22.5))
  d=model(E);d['display'].update({'gui':{'rotation':[12,-20,-35],'translation':[0,-2.5,0],'scale':[.48]*3},'fixed':{'rotation':[0,0,0],'translation':[0,-3,0],'scale':[.6]*3},'thirdperson_righthand':{'rotation':[90 if kind=='STAFF'else 0,-90,0 if kind=='STAFF'else 10],'translation':[0,5,.5],'scale':[.7]*3},'thirdperson_lefthand':{'rotation':[90 if kind=='STAFF'else 0,90,0 if kind=='STAFF'else -10],'translation':[0,5,.5],'scale':[.7]*3},'firstperson_righthand':{'rotation':[0,-90,0 if kind=='STAFF'else -15],'translation':[.8,3.6,.6],'scale':[.46]*3},'firstperson_lefthand':{'rotation':[0,90,0 if kind=='STAFF'else 15],'translation':[.8,3.6,.6],'scale':[.46]*3}})
  if kind=='BOW':
   d['display']['thirdperson_righthand'].update(rotation=[-80,260,-85],translation=[-1,-2,2.5],scale=[.55]*3);d['display']['thirdperson_lefthand'].update(rotation=[-80,-280,85],translation=[-1,-2,2.5],scale=[.55]*3)
  return d
 write(MODELS/f'item/sculpted_{id}.json',weapon())
 if kind=='BOW':
  for p in range(3):write(MODELS/f'item/sculpted_{id}_pulling_{p}.json',weapon(p+1))
  def ref(suffix=''):return{'type':'minecraft:model','model':f'mmorpg:item/sculpted_{id}{suffix}'}
  graph={'type':'minecraft:condition','property':'minecraft:using_item','on_false':ref(),'on_true':{'type':'minecraft:range_dispatch','property':'minecraft:use_duration','scale':.1,'fallback':ref('_pulling_0'),'entries':[{'threshold':.65,'model':ref('_pulling_1')},{'threshold':.9,'model':ref('_pulling_2')}]}}
 else:graph={'type':'minecraft:model','model':f'mmorpg:item/sculpted_{id}'}
 write(RES/'items'/f'{id}.json',{'model':graph})
print(len(weapons),'sculpted weapons, animated bows and corrected upright staff transforms')
# Volumetric bottle/coin/key/charm rewards for the reel's FIXED display context.
for p in sorted((RES/'items').glob('*.json')):
 id=p.stem
 if not(id.startswith('potion_')or id.startswith('cle_')or id.startswith('charme_')or id=='piece_or'or id in ('oeuf_mystere','sceau_mystere','coffre_cosmetique')or id.startswith('pierre_amelioration')):continue
 old=json.loads(p.read_text());E=[]
 if id=='oeuf_mystere':
  E=[B([8,3+j*1.6,8],[w,1.8,w],12,'Egg shell')for j,w in enumerate([3.5,5.5,7,7.3,6,4,2])];E+=[B([8,8,4.1],[1.4,2,.45],4,'Egg rune','z',45),B([6.3,5,4.4],[1,1,.5],1,'Shell fleck')]
 elif id.startswith('pierre_amelioration'):
  E=[B([8,5,8],[5,4,5],4,'Faceted improvement stone','y',45),B([8,8,8],[3.6,3,3.6],4,'Upper facet','y',45),B([8,10.2,8],[1.8,1.8,1.8],4,'Crystal point','y',45),B([8,2.5,8],[3.5,1.5,3.5],1,'Gold footing','y',45)]
 elif id=='coffre_cosmetique':
  E=[B([8,6,8],[11,7,8],13,'Enchanted coffer'),B([8,10,8],[11,2,8],1,'Bound lid')]
  for x in [4,12]:E.append(B([x,6,3.7],[1,7,.6],1,'Gold binding'))
  E.append(B([8,7,3.4],[2,3,.7],4,'Crystal lock','z',45))
 elif id.startswith('potion_'):
  liquid=5 if'mana'in id else 8;E=[B([8,6,8],[7,7,7],15,'Glass bottle'),B([8,5.8,8],[5.8,5.5,5.8],liquid,'Potion'),B([8,9.7,8],[4.8,1.6,4.8],15,'Shoulder'),B([8,12,8],[3,3.4,3],15,'Neck'),B([8,14,8],[3.4,1.5,3.4],7,'Cork'),B([8,11,8],[3.7,.6,3.7],1,'Sealed ribbon')]
 elif id.startswith('cle_'):
  for x,y,w,h in [(8,12,5,1),(8,7.5,5,1),(5.5,9.7,1,4.5),(10.5,9.7,1,4.5),(8,4,1,7),(9,1.5,3,1),(9,3.5,3,1)]:E.append(B([x,y,8],[w,h,1.1],1,'Forged key'))
  E.append(B([8,10,8],[2.5,2.5,1.7],4,'Key gem','z',45))
 elif id.startswith('charme_'):
  E=[B([8,7,8],[6,6,1.5],1,'Amulet frame','z',45),B([8,7,6.9],[4,4,.8],6 if'chance'in id else 14,'Cabochon','z',45),B([8,12,8],[1,4,1],1,'Chain')]
 else:
  E=[B([8,8,8],[9,6,1.5],1,'Gold coin'),B([8,8,8],[6,9,1.5],1),B([8,8,6.9],[1.5,5,.5],14,'Minted sigil'),B([8,8,6.9],[4.5,1.3,.5],14)]
 write(MODELS/f'item/reel_{id}.json',model(E))
 if old.get('model',{}).get('type')=='minecraft:select'and old['model'].get('property')=='minecraft:display_context':old={'model':old['model']['fallback']}
 write(p,{'model':{'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':'fixed','model':{'type':'minecraft:model','model':f'mmorpg:item/reel_{id}'}}],'fallback':old['model']}})
