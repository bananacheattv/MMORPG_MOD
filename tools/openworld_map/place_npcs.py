#!/usr/bin/env python3
"""Replace les PNJ de quetes du monde ouvert (world_npcs.json) sur la vraie carte Aeloria 800x800 (maps/openworld-800) :
lit les secteurs .schem, ne garde que des cases de sol naturel plat, hors batiments et eau, atteignables a pied depuis l'arrivee,
espacees d'au moins 12 blocs, regroupees par lieu (village, halte, monument) selon la region de la quete. Deterministe.
Usage : python tools/openworld_map/place_npcs.py   (necessite numpy, nbtlib)"""
import random,math,collections
from collections import deque
import nbtlib,csv,numpy as np,re,pickle
import pathlib,json,sys
ROOT=pathlib.Path(__file__).resolve().parents[2]
base=str(ROOT/'maps/openworld-800/schematics')+'/'
def varints(b,n):
    out=np.empty(n,dtype=np.int32);i=0;k=0;L=len(b)
    while k<n:
        v=0;s=0
        while True:
            c=b[i];i+=1;v|=(c&127)<<s;s+=7
            if c<128:break
        out[k]=v;k+=1
    return out
H=np.zeros((800,800),dtype=np.int16)      # surface y (top solid/liquid, ignoring foliage)
SB=np.empty((800,800),dtype=object)       # surface block name
for row in csv.DictReader(open(base+'importation.csv')):
    f=nbtlib.load(base+row['fichier'])['Schematic']
    W,Hh,L=int(f['Width']),int(f['Height']),int(f['Length'])
    pal={int(v):re.sub(r'\[.*','',str(k)) for k,v in f['Blocks']['Palette'].items()}
    data=varints(bytes(np.array(f['Blocks']['Data'],dtype=np.uint8)),W*Hh*L).reshape(Hh,L,W)
    ignore=np.array([pal[i].split(':')[1].endswith(('air','leaves','grass','fern','flower','bush','vine','mushroom','roots','rose','tulip','dandelion','poppy','cornflower','lily_of_the_valley','_stem','sapling','sweet_berry_bush','torch','sign','banner','sugar_cane','kelp','seagrass')) or pal[i].split(':')[1] in('short_grass','tall_grass','large_fern','dead_bush','cave_air','void_air','light','moss_carpet','pink_petals') for i in range(max(pal)+1)])
    ox,oz=int(row['collage_x'])+400,int(row['collage_z'])+400
    solid=~ignore[data]   # (Hh,L,W)
    top=Hh-1-np.argmax(solid[::-1],axis=0)
    has=solid.any(axis=0)
    top=np.where(has,top-64,-999)
    H[oz:oz+L,ox:ox+W]=top
    names=np.array([pal.get(i,'?') for i in range(max(pal)+1)],dtype=object)
    yy=np.clip(top+64,0,Hh-1)
    SB[oz:oz+L,ox:ox+W]=names[np.take_along_axis(data,yy[None],axis=0)[0]]

m=json.load(open(ROOT/'maps/openworld-800/map.json'))
OX=400
def idx(x,z): return z+OX,x+OX
NAT={'grass_block','coarse_dirt','gravel','podzol','red_sand','snow_block','end_stone','purple_terracotta','mud','dirt','stone','red_sandstone','moss_block','sand','blackstone','packed_ice','andesite','cobbled_deepslate'}
PAVE={'stone_bricks','cobblestone','smooth_sandstone','mossy_stone_bricks','polished_blackstone_bricks','polished_deepslate','smooth_stone'}
sb=np.vectorize(lambda s:str(s).split(':')[1])(SB)
good=np.isin(sb,list(NAT))|np.isin(sb,list(PAVE))
# footprints
bad=np.zeros((800,800),bool)
for p in m['pois']:
    if p['kind']=='bâtiment' or p['kind']=='étal':
        sx,_,sz=p.get('size',[5,0,5]); hx,hz=sx//2+3,sz//2+3
        x0,x1,z0,z1=max(p['x']-hx,-400),min(p['x']+hx,399),max(p['z']-hz,-400),min(p['z']+hz,399)
        bad[z0+OX:z1+OX+1,x0+OX:x1+OX+1]=True
for p in m['pois']:
    if p['kind'] in('monument','pont','grotte','site naturel'):
        r={'monument':14,'pont':6,'grotte':5,'site naturel':10}[p['kind']]
        # not used as bad: monuments are walkable ground outside; only exclude their core
        pass
# local flatness
from numpy.lib.stride_tricks import sliding_window_view as sw
Hp=np.pad(H,2,mode='edge').astype(int)
w=sw(Hp,(5,5)); rng=w.max((2,3))-w.min((2,3))
pave_flat=np.isin(sb,list(PAVE))&(rng<=1)
ok=(np.isin(sb,list(NAT))&(rng<=2))|pave_flat
ok&=~bad
ok[:12,:]=ok[-12:,:]=False; ok[:,:12]=ok[:,-12:]=False
path=(sb=='dirt_path')
print('ok cells',ok.sum(),'of',ok.size)
walk=~np.isin(sb,['water','lava','ice','blue_ice'])&(H>-900)
walk&=~np.char.endswith(sb.astype(str),'_log')
def comp(start):
    seen=np.zeros_like(ok); q=deque([start]); seen[start]=True
    while q:
        a,b=q.popleft()
        for da,db in((1,0),(-1,0),(0,1),(0,-1)):
            c,d=a+da,b+db
            if 0<=c<800 and 0<=d<800 and not seen[c,d] and (walk[c,d]) and abs(int(H[c,d])-int(H[a,b]))<=1:
                seen[c,d]=True;q.append((c,d))
    return seen
def nearest_ok(x,z):
    for r in range(0,80):
        best=None
        for dz in range(-r,r+1):
            for dx in range(-r,r+1):
                if max(abs(dx),abs(dz))!=r:continue
                a,b=idx(x+dx,z+dz)
                if 0<=a<800 and 0<=b<800 and ok[a,b]: return a,b
    return None
SITES={ # region -> [(site name, cx,cz, count)]
"Valdhéris et les Contreforts":[("Aéloria",0,0,8)],
"Bois de Murmurelle":[("Clairbois",-230,45,9),("Cercle des druides",-130,130,5),("Pont des pèlerins",-115,20,3)],
"Marais de Fangeterre":[("Brumevert",-245,220,6),("Tour des Roseaux",-340,155,5)],
"Mines de Creuse-Braise":[("Entrée des mines boréales",-315,-150,8)],
"Marches du Rempart":[("Refuge des cols",-80,-160,11)],
"Plaines de Cendrelune":[("Porte des Dunes",140,-95,12)],
"Ruines de Haute-Cadette":[("Camp des Veilleurs",110,155,6),("Ruines de la Veille astrale",160,200,5)],
"Désert de Verre":[("Sahr-Éclat",210,-175,8)],
"Vallée de Refuge-Bas":[("Astréon",260,145,11)],
"Terres du Fort de la Brisure":[("Tour du Vent de cuivre",345,-190,8)],
"Pics d'Hivernoir":[("Boréalis",-200,-185,11)],
"Forêt Pétrifiée":[("Arbre-sanctuaire de Sylveronce",-315,-5,8)],
"Abysses de Sombrelac":[("Phare du Glacier",-105,-320,11)],
"Faille des Murmures":[("Éclat du Zénith",330,65,8)],
"Marches Renégates":[("Moulin des Brumes",-165,325,11)],
"Lisières du Cratère":[("Caldeira du Brasier ancien",265,-310,8)],
"Forge de Malvorn":[("Fonderie des Cendres",140,-315,11)],
"Nécropole d'Aelis":[("Abbaye des Veilleurs",55,305,8)],
"Entre-Monde":[("Observatoire des Constellations",280,323,11)],
"Cœur de la Brisure":[("Temple du Soleil brisé",300,-19,8)],
}
random.seed(161126)
MAIN=comp(nearest_ok(0,0))&ok
print('main',MAIN.sum())
placed=[]  # (x,z,region,site)
res={}
for reg,sites in SITES.items():
    res[reg]=[]
    for name,cx,cz,n in sites:
        cc=MAIN
        for D in (16,14,12):
            pts=[]
            for R in (30,45,60,80,100,130):
                cand=[(a,b) for a,b in zip(*np.nonzero(cc)) if 5<=math.hypot(b-OX-cx,a-OX-cz)<=R]
                random.shuffle(cand)
                # prefer cells near a path
                cand.sort(key=lambda c:(0 if path[max(c[0]-3,0):c[0]+4,max(c[1]-3,0):c[1]+4].any() else 1)+random.random()*0.9)
                pts=[]
                for a,b in cand:
                    x,z=b-OX,a-OX
                    if all(math.hypot(x-px,z-pz)>=D for px,pz,_,_ in placed) and all(math.hypot(x-p[0],z-p[1])>=D for p in pts):
                        pts.append((x,z))
                        if len(pts)==n:break
                if len(pts)==n:break
            if len(pts)==n:break
        assert len(pts)==n,(name,len(pts),n,len(cand))
        print(reg,name,n,'D',D,'R',R, 'surf',collections.Counter(sb[z+OX,x+OX] for x,z in pts).most_common(3))
        for x,z in pts:
            placed.append((x,z,reg,name)); res[reg].append((x,z,int(H[z+OX,x+OX])))

# --- ecriture dans world_npcs.json
f=ROOT/"src/main/resources/data/mmorpg/lore/world_npcs.json"
d=json.load(open(f,encoding="utf-8"))
anchors={}
for reg,sites in SITES.items(): anchors[reg]=[sites[0][1],sites[0][2]]
d["regions"]=anchors
nxt={r:0 for r in res}
for n in d["npcs"]:
    if n["dim"]!="openworld": continue
    r=n["region"]; x,z,_=res[r][nxt[r]]; nxt[r]+=1
    n["dx"]=int(x-anchors[r][0]); n["dz"]=int(z-anchors[r][1])
assert all(nxt[r]==len(res[r]) for r in res)
json.dump(d,open(f,"w",encoding="utf-8"),ensure_ascii=False,indent=1)
print("ecrit",f)
