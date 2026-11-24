"""Voxel geometry and reproducible terrain for the playable 800-block map."""
from pathlib import Path
import math, random, json
import numpy as np

SIZE, YMIN, YMAX, SEA = 800, -64, 255, 62
SEED = 161126
REGIONS = ["Sylveronce et Brumevert", "Val des Cryptes", "Hauts de Boréalis", "Dunes d'Ignis", "Éclats d'Astréon"]

def noise(scale, seed):
    rng = np.random.default_rng(seed)
    n = int(math.ceil(SIZE / scale)) + 2
    a = rng.uniform(-1, 1, (n, n))
    u = np.arange(SIZE) / scale
    i = u.astype(int); f = u-i; f = f*f*(3-2*f)
    return ((a[i[:,None],i[None,:]]*(1-f[None,:]) + a[i[:,None],i[None,:]+1]*f[None,:])*(1-f[:,None])
            +(a[i[:,None]+1,i[None,:]]*(1-f[None,:]) + a[i[:,None]+1,i[None,:]+1]*f[None,:])*f[:,None])

class World:
    def __init__(self, out):
        self.out = Path(out); self.out.mkdir(parents=True, exist_ok=True)
        self.rng = random.Random(SEED)
        self.a = np.zeros((YMAX-YMIN+1, SIZE, SIZE), dtype=np.uint16)
        self.palette = ["minecraft:air"]; self.ids = {self.palette[0]:0}
        self.reserved = np.zeros((SIZE,SIZE), bool)
        self.roads = np.zeros((SIZE,SIZE), bool)
        self.pois, self.signs, self.routes, self.safe_areas = [], [], [], []
        self.counts = {}
        self.x, self.z = np.meshgrid(np.arange(-400,400), np.arange(-400,400))
        self.prepare_terrain()

    def block(self, name):
        if ':' not in name: name = 'minecraft:' + name
        if name not in self.ids:
            self.ids[name] = len(self.palette); self.palette.append(name)
        return self.ids[name]

    def put(self, x,y,z,b):
        x,y,z = int(round(x))+400, int(round(y))-YMIN, int(round(z))+400
        if 0 <= x < SIZE and 0 <= z < SIZE and 0 <= y < self.a.shape[0]: self.a[y,z,x] = self.block(b)

    def get(self, x,y,z):
        x,y,z = int(round(x))+400, int(round(y))-YMIN, int(round(z))+400
        return self.palette[self.a[y,z,x]] if 0<=x<SIZE and 0<=z<SIZE and 0<=y<self.a.shape[0] else 'minecraft:air'

    def box(self,x1,y1,z1,x2,y2,z2,b):
        x1,x2=sorted((int(round(x1))+400,int(round(x2))+400)); z1,z2=sorted((int(round(z1))+400,int(round(z2))+400)); y1,y2=sorted((int(round(y1))-YMIN,int(round(y2))-YMIN))
        if x2<0 or z2<0 or y2<0 or x1>=SIZE or z1>=SIZE or y1>=self.a.shape[0]:return
        self.a[max(0,y1):min(self.a.shape[0],y2+1),max(0,z1):min(SIZE,z2+1),max(0,x1):min(SIZE,x2+1)] = self.block(b)

    def disk(self,cx,y,cz,r,b,inner=-1):
        x1,x2=max(-400,math.floor(cx-r)),min(399,math.ceil(cx+r)); z1,z2=max(-400,math.floor(cz-r)),min(399,math.ceil(cz+r))
        if not YMIN<=y<=YMAX or x1>x2 or z1>z2:return
        xx,zz=np.meshgrid(np.arange(x1,x2+1)-cx,np.arange(z1,z2+1)-cz)
        d=xx*xx+zz*zz; mask=d<=r*r
        if inner>=0:mask &= d>=inner*inner
        self.a[int(y)-YMIN,z1+400:z2+401,x1+400:x2+401][mask]=self.block(b)

    def sphere(self,cx,cy,cz,rx,ry,rz,b,air_only=False):
        x1,x2=max(-400,math.floor(cx-rx)),min(399,math.ceil(cx+rx)); z1,z2=max(-400,math.floor(cz-rz)),min(399,math.ceil(cz+rz))
        y1,y2=max(YMIN,math.floor(cy-ry)),min(YMAX,math.ceil(cy+ry))
        if x1>x2 or z1>z2 or y1>y2:return
        xx=(np.arange(x1,x2+1)-cx)/rx;zz=(np.arange(z1,z2+1)-cz)/rz
        base=zz[:,None]**2+xx[None,:]**2
        for y in range(y1,y2+1):
            target=self.a[y-YMIN,z1+400:z2+401,x1+400:x2+401]
            mask=base+((y-cy)/ry)**2<=1
            if air_only:mask &= target==0
            target[mask]=self.block(b)

    def line(self,a,b,block,r=0):
        n=max(1,int(max(abs(b[i]-a[i]) for i in range(3))*1.4))
        for t in np.linspace(0,1,n+1):
            p=[round(a[i]*(1-t)+b[i]*t) for i in range(3)]
            if r:self.sphere(*p,r,r,r,block)
            else:self.put(*p,block)

    def river_x(self,z):return int(round(-115-40*math.sin((z+60)/95)-max(0,z-50)*.32))

    def ground(self,x,z):return int(self.height[max(0,min(799,int(z)+400)),max(0,min(799,int(x)+400))])
    def region(self,x,z):return int(self.region_map[int(z)+400,int(x)+400])

    def reserve(self,x,z,r):
        self.reserved |= (self.x-x)**2+(self.z-z)**2 <= r*r

    def pad(self,x,z,r,y,feather=12):
        d=np.hypot(self.x-x,self.z-z)
        k=np.clip((r+feather-d)/feather,0,1);k=k*k*(3-2*k)
        self.height=np.rint(self.height*(1-k)+y*k).astype(np.int16)
        self.water[d<r+2]=-300
        self.reserve(x,z,r+4)

    def poi(self,name,kind,x,z,y=None,region=None,**extra):
        self.pois.append(dict(name=name,kind=kind,x=x,y=self.ground(x,z)+1 if y is None else y,z=z,
                             region=REGIONS[self.region(x,z)] if region is None else region,**extra))
        self.counts[kind]=self.counts.get(kind,0)+1

    def prepare_terrain(self):
        x,z=self.x,self.z;n1=noise(110,SEED);n2=noise(40,SEED+1);n3=noise(13,SEED+2)
        wx=x+20*noise(90,SEED+5)+12*np.sin(z/67);wz=z+24*noise(115,SEED+6)
        centers=[(-220,90),(65,285),(-205,-225),(235,-205),(285,160)]
        distances=np.stack([(wx-cx)**2+(wz-cz)**2 for cx,cz in centers])
        self.region_map=np.argmin(distances,axis=0).astype(np.uint8)
        weights=np.exp(-distances/(175**2));weights/=weights.sum(axis=0)
        mountains=75*np.exp(-((x+260)**2/(95**2)+(z+300)**2/(85**2)))
        ridge=17*np.abs(np.sin((x+z*.65)/24))*np.exp(-((x+210)**2+(z+240)**2)/(190**2))
        volcanic=100*np.exp(-((x-265)**2+(z+310)**2)/(69**2))
        h0=77+9*n1+4*n2+2*n3
        h1=79+13*n1+6*n2+3*n3
        h2=108+18*n1+8*n2+ridge+mountains
        h3=87+16*n1+7*n2+3*np.sin((x+z*.3)/17)+volcanic
        h4=97+21*n1+10*n2+4*n3
        h=sum(weights[i]*h for i,h in enumerate([h0,h1,h2,h3,h4]))
        self.swamp=(self.region_map==0)&(((x+245)/125)**2+((z-235)/108)**2<1)
        swampblend=np.clip(1.5-(((x+245)/135)**2+((z-235)/120)**2),0,1)*(self.region_map==0)
        h=h*(1-swampblend)+(63+4*n2+2*n3)*swampblend
        edge=np.minimum(400-np.abs(x),400-np.abs(z))
        coast=np.clip((edge-10-4*n2)/24,0,1);coast=coast*coast*(3-2*coast)
        h=49*(1-coast)+h*coast
        self.water=np.where(h<SEA,SEA,-300).astype(np.int16)
        # A wooded river gorge, with a widening estuary and natural banks.
        riverx=-115-40*np.sin((z+60)/95)-np.maximum(0,z-50)*.32
        rd=np.abs(x-riverx)
        active=(z>-105)&(z<380)&(self.region_map==0)
        bed=59+np.minimum(34,np.maximum(0,rd-5)*1.4)
        h=np.where(active&(rd<22),np.minimum(h,bed),h)
        self.water=np.where(active&(rd<8),SEA,self.water)
        # Tarn at altitude; the glacier continues into the frozen shore.
        lake=((x+175)/36)**2+((z+180)/26)**2
        h=np.where(lake<1,104+3*lake,h);self.water=np.where(lake<1,110,self.water)
        self.height=np.rint(h).astype(np.int16)
        self.biomes=np.zeros((SIZE,SIZE),np.uint8)
        self.biome_names=['minecraft:forest','minecraft:swamp','minecraft:dark_forest','minecraft:snowy_plains','minecraft:badlands','minecraft:end_highlands','minecraft:plains','minecraft:frozen_peaks','minecraft:desert','minecraft:deep_dark']
        for rid,b in enumerate([0,2,3,4,5]):self.biomes[self.region_map==rid]=b
        self.biomes[self.swamp]=1
        self.biomes[(self.region_map==0)&(n1<-.1)&~self.swamp]=6
        self.biomes[(self.region_map==2)&(self.height>133)]=7
        self.biomes[(self.region_map==3)&(self.height<100)]=8
        self.pad(0,0,52,83,26)
        self.villages=[('Aéloria — Place des Horizons',0,0,44,83,0),('Clairbois',-230,45,39,83,0),
            ('Brumevert',-245,220,31,65,0),('Veillepierre',70,245,37,82,1),('Boréalis',-200,-185,38,112,2),
            ('Sahr-Éclat',210,-175,41,92,3),('Astréon',260,145,36,108,4)]
        for name,vx,vz,r,y,rid in self.villages:
            self.pad(vx,vz,r,y,15)
            self.safe_areas.append(dict(name=name,x=vx,z=vz,radius=r+12))
        self.landmarks=[('Arbre-sanctuaire de Sylveronce',-315,-5,28,86,0),('Abbaye des Veilleurs',55,332,46,91,1),
            ('Citadelle du Souffle boréal',-235,-300,43,147,2),('Temple du Soleil brisé',300,-55,43,105,3),
            ('Observatoire des Constellations',280,285,42,124,4)]
        for name,lx,lz,r,y,rid in self.landmarks:self.pad(lx,lz,r,y,15)

    def path(self,points,width=5,main=True):
        # Smooth Chaikin curves avoid geometric straight spokes between regions.
        pts=np.array(points,float)
        for _ in range(2):
            fresh=[pts[0]]
            for a,b in zip(pts[:-1],pts[1:]):fresh.extend((.75*a+.25*b,.25*a+.75*b))
            fresh.append(pts[-1]);pts=np.array(fresh)
        samples=[]
        for a,b in zip(pts[:-1],pts[1:]):
            n=max(1,int(np.linalg.norm(b-a)))
            samples.extend(a+(b-a)*t for t in np.linspace(0,1,n,endpoint=False))
        samples.append(pts[-1]); coords=np.rint(samples).astype(int)
        heights=np.array([self.ground(x,z) for x,z in coords],float)
        heights=np.convolve(np.pad(heights,(6,6),mode='edge'),np.ones(13)/13,mode='valid')
        # Water is read BEFORE any cell is modified: otherwise each sample dries the cells ahead of it
        # and the road ends up damming the river instead of crossing it on a bridge.
        wl=np.array([self.water[max(0,min(799,z+400-5)):max(0,min(799,z+400+5))+1,max(0,min(799,x+400-5)):max(0,min(799,x+400+5))+1].max() for x,z in coords])
        wet=np.array([self.water[max(0,min(799,z+400)),max(0,min(799,x+400))]>-200 for x,z in coords])
        wetd=np.convolve(wet.astype(int),np.ones(11,int),mode='same')>0
        i=0
        while i<len(wetd):
            if not wetd[i]:i+=1;continue
            j=i
            while j+1<len(wetd) and wetd[j+1]:j+=1
            a,b=max(0,i-6),min(len(heights)-1,j+6)
            for k in range(i,j+1):
                heights[k]=heights[a]+(heights[b]-heights[a])*(k-a)/max(1,b-a)
                if wl[k]>-200:heights[k]=max(heights[k],wl[k]+3)
            i=j+1
        for _ in range(3):
            for i in range(1,len(heights)):heights[i]=np.clip(heights[i],heights[i-1]-.48,heights[i-1]+.48)
            for i in range(len(heights)-2,-1,-1):heights[i]=np.clip(heights[i],heights[i+1]-.48,heights[i+1]+.48)
        route=[]
        for n_i,((x,z),y) in enumerate(zip(coords,np.rint(heights).astype(int))):
            if not -390<x<390 or not -390<z<390:continue
            wet=bool(wetd[n_i])
            for dz in range(-width//2-2,width//2+3):
                for dx in range(-width//2-2,width//2+3):
                    if not -400<=x+dx<400 or not -400<=z+dz<400:continue
                    d=math.hypot(dx,dz)
                    if d>width/2+2:continue
                    iz,ix=z+dz+400,x+dx+400
                    self.reserved[iz,ix]=True
                    if d<=width/2:
                        self.roads[iz,ix]=True
                        if not wet and self.water[iz,ix]<=-200:self.height[iz,ix]=y
                    elif not wet and self.water[iz,ix]<=-200:
                        k=(width/2+2-d)/2; self.height[iz,ix]=round(self.height[iz,ix]*(1-k)+y*k)
            route.append((int(x),int(y),int(z),bool(wet)))
        self.routes.append((route,width,main))

    def plan_roads(self):
        for points in [[(0,0),(-85,20),(-160,15),(-230,45)],[(0,0),(75,110),(30,185),(70,245)],
                       [(0,0),(-55,-80),(-125,-110),(-200,-185)],[(0,0),(75,-75),(155,-65),(210,-175)],
                       [(0,0),(100,55),(180,95),(260,145)]]:self.path(points,7)
        for points in [[(-230,45),(-290,120),(-245,220)],[(-200,-185),(-75,-245),(90,-225),(210,-175)],
                       [(210,-175),(305,0),(260,145)],[(260,145),(205,255),(70,245)],
                       [(70,245),(-80,280),(-245,220)],[(-245,220),(-320,140),(-230,45)],
                       [(-230,45),(-290,35),(-315,-5)],[(70,245),(50,275),(55,332)],
                       [(-200,-185),(-260,-230),(-235,-300)],[(210,-175),(260,-125),(300,-55)],
                       [(260,145),(235,215),(280,285)]]:self.path(points,5,False)

    def fill_terrain(self):
        print('Terrain et biomes...',flush=True)
        n=noise(9,SEED+10); slope=np.hypot(*np.gradient(self.height.astype(float)))
        # Surface blocks use a dithered region map: the biome edges become a 20-block mottled mix
        # instead of a ruler-straight line (snow touching grass at the spawn).
        cs=[(-220,90),(65,285),(-205,-225),(235,-205),(285,160)]
        dist=np.stack([(self.x-cx)**2+(self.z-cz)**2+32000*noise(24+i,SEED+40+i) for i,(cx,cz) in enumerate(cs)])
        rj=np.argmin(dist,axis=0).astype(np.uint8)
        topnames=['grass_block','coarse_dirt','snow_block','red_sand','end_stone']
        tops=np.array([self.block(x) for x in topnames],np.uint16)[rj]
        tops[(rj==0)&self.swamp]=self.block('mud')
        tops[(rj==1)&(n>.22)]=self.block('podzol')
        tops[(rj==1)&(n<-.3)]=self.block('gravel')
        tops[(rj==4)&(n>.2)]=self.block('purple_terracotta')
        tops[(rj==4)&(n<-.3)]=self.block('blackstone')
        tops[(rj==2)&(slope>2.5)]=self.block('stone')
        tops[(rj==3)&(slope>1.8)]=self.block('red_sandstone')
        tops[(rj==0)&(slope>2.4)]=self.block('andesite')
        tops[(self.height<64)&(rj!=2)]=self.block('gravel')
        tops[(self.height<64)&(rj==0)&~self.swamp]=self.block('sand')
        stone=self.block('stone'); deep=self.block('deepslate');dirt=self.block('dirt')
        for y in range(YMIN,YMAX+1):
            target=self.a[y-YMIN];solid=y<=self.height
            if y==YMIN:target[:]=self.block('bedrock');continue
            target[solid]=deep if y<8 else stone
            surface=solid&(y>self.height-4)
            target[surface]=dirt
            target[surface&(self.region_map==3)]=self.block('red_sandstone')
            target[surface&(self.region_map==4)]=self.block('end_stone')
            target[y==self.height]=tops[y==self.height]
            flooded=(y>self.height)&(y<=self.water)
            target[flooded]=self.block('water[level=0]')
            target[flooded&(self.region_map==2)&(y==self.water)]=self.block('ice')
        # Authored mineral seams and caves do not depend on vanilla world generation.
        for y in [18,24,31]:
            cave=(noise(28,SEED+y)>.33)&(noise(65,SEED+y+100)>.03)
            for dy in range(5):self.a[y+dy-YMIN][cave]=0
            self.a[y-1-YMIN][cave&(n>.45)]=self.block('glowstone')
        self.surface=tops

    def finish_roads(self):
        for route,width,main in self.routes:
            for i,(x,y,z,wet) in enumerate(route):
                rid=self.region(x,z)
                b=['dirt_path','cobbled_deepslate','stone_bricks','smooth_sandstone','polished_blackstone_bricks'][rid]
                if wet:b='dark_oak_planks'
                self.disk(x,y,z,width/2,b)
                if wet:
                    # Tangent of the route, to put piles and railings on both sides of the deck.
                    p0=route[max(0,i-2)];p1=route[min(len(route)-1,i+2)]
                    tx,tz=p1[0]-p0[0],p1[2]-p0[2];nrm=math.hypot(tx,tz) or 1
                    nx,nz=-tz/nrm,tx/nrm;off=width/2-.5
                    for sgn in (-1,1):
                        qx,qz=round(x+sgn*nx*off),round(z+sgn*nz*off)
                        self.put(qx,y+1,qz,'dark_oak_fence')
                        if i%7==0:
                            for yy in range(y-1,max(YMIN,self.ground(qx,qz)-1),-1):
                                if self.get(qx,yy,qz) in ('minecraft:air','minecraft:water[level=0]'):self.put(qx,yy,qz,'stripped_dark_oak_log')
                                else:break
                            if i%14==0:self.put(qx,y+2,qz,'lantern')
                if i%47==0 and i>15 and i<len(route)-15:
                    self.box(x+width//2+1,y+1,z,x+width//2+1,y+3,z,'cobblestone_wall')
                    self.put(x+width//2+1,y+4,z,'lantern')

    def save_arrays(self):
        np.save(self.out/'blocks.npy',self.a)
        np.savez_compressed(self.out/'terrain.npz',height=self.height,biomes=self.biomes,regions=self.region_map,roads=self.roads,reserved=self.reserved)
        meta=dict(name='Aéloria — Les Cinq Horizons',seed=SEED,dimensions=[SIZE,YMAX-YMIN+1,SIZE],origin=[-400,YMIN,-400],spawn=[.5,84,.5],
                  palette=self.palette,biome_palette=self.biome_names,regions=REGIONS,pois=self.pois,signs=self.signs,safe_areas=self.safe_areas,counts=self.counts)
        (self.out/'map.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf-8')
        print(f'Blocs : {np.count_nonzero(self.a):,} ; états : {len(self.palette)} ; points remarquables : {len(self.pois)}',flush=True)
