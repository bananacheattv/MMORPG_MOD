"""Reproducible natural set dressing, explorable mine mouths and field systems."""
import math, random
import numpy as np
from world import SEED, REGIONS
from architecture import crystal, farm, sign, lamp


def oak(w,x,y,z,rng,swamp=False):
    h=rng.randint(10,19) if not swamp else rng.randint(8,13)
    wood='dark_oak_log' if swamp else 'oak_log'
    leaf=('mangrove' if swamp else rng.choice(['oak','oak','birch']))+'_leaves[persistent=true]'
    w.box(x,y,z,x+1,y+h,z+1,wood)
    for i in range(4):
        a=i*math.pi/2+rng.uniform(-.35,.35);r=rng.randint(4,7)
        bx=x+round(r*math.cos(a));bz=z+round(r*math.sin(a));by=y+h-rng.randint(1,5)
        w.line((x,y+h//2,z),(bx,by,bz),wood)
        w.sphere(bx,by+2,bz,rng.randint(4,6),3,5,leaf,air_only=True)
        if swamp:
            for dx,dz in [(3,0),(-3,1),(0,4)]:
                w.box(bx+dx,by-3,bz+dz,bx+dx,by+1,bz+dz,leaf)
    w.sphere(x,y+h+1,z,5,4,5,leaf,air_only=True)
    for dx,dz in [(2,0),(-2,0),(0,2),(0,-2)]:
        gy=w.ground(x+dx,z+dz)
        w.line((x,y+2,z),(x+dx,gy+1,z+dz),wood)


def pine(w,x,y,z,rng):
    h=rng.randint(13,24);w.box(x,y,z,x,y+h,z,'spruce_log')
    for dy in range(4,h+1):
        rad=max(1,(h-dy)/4+((h-dy)%3)*.6)
        w.disk(x,y+dy,z,rad,'spruce_leaves[persistent=true]')
        if dy%3==0:
            w.disk(x,y+dy+1,z,rad*.83,'snow[layers=1]')
    w.put(x,y+h+1,z,'snow[layers=1]')


def dead_tree(w,x,y,z,rng):
    h=rng.randint(7,15);w.box(x,y,z,x,y+h,z,'dark_oak_log')
    for i in range(3):
        a=rng.random()*math.tau;r=rng.randint(3,6);dy=y+h-rng.randint(0,5)
        bx=x+round(r*math.cos(a));bz=z+round(r*math.sin(a))
        w.line((x,dy-2,z),(bx,dy,bz),'dark_oak_log');w.box(bx,dy,bz,bx,dy+2,bz,'dark_oak_log')
    if rng.random()<.3:w.put(x+1,y,z,'cobweb')


def mushroom(w,x,y,z,rng):
    h=rng.randint(5,9);r=rng.randint(3,5)
    w.box(x,y,z,x,y+h,z,'mushroom_stem')
    w.sphere(x,y+h,z,r,2,r,'red_mushroom_block' if rng.random()<.7 else 'brown_mushroom_block')
    w.put(x,y+h-2,z,'shroomlight')


def local_pad(w,x,z,r,y,top):
    for dz in range(-r,r+1):
        for dx in range(-r,r+1):
            if dx*dx+dz*dz>r*r:continue
            xx,zz=x+dx,z+dz;old=w.ground(xx,zz)
            w.box(xx,min(old,y)-3,zz,xx,y,zz,'dirt')
            if old>y:w.box(xx,y+1,zz,xx,old+3,zz,'air')
            w.put(xx,y,zz,top);w.height[zz+400,xx+400]=y
    w.reserve(x,z,r+2)


def farms(w):
    for x,z,rid in [(-176,58,0),(-289,64,0),(118,252,1),(-148,-213,2),(166,-170,3),(309,135,4)]:
        y=w.ground(x,z);local_pad(w,x,z,16,y,'dirt')
        farm(w,x,y,z,rid)
        w.poi('Jardins de '+REGIONS[rid].split(' ')[0],'jardin',x,z,y+1,REGIONS[rid])
        for dx,dz in [(-12,-9),(12,9)]:lamp(w,x+dx,y+1,z+dz,rid)


def mine(w,x,z,rid,name,dx=0,dz=1):
    y=w.ground(x,z);w.reserve(x,z,13)
    # Solid hillside shell: an authored entrance still reads as a mine on a shallow hill.
    w.sphere(x+dx*8,y+3,z+dz*8,13,9,13,'stone' if rid!=4 else 'blackstone')
    for t in range(58):
        px=x+dx*t;pz=z+dz*t;py=y-max(0,t-8)//2
        w.sphere(px,py+3,pz,3.3,3,3.3,'air')
        w.disk(px,py,pz,3,'cobblestone')
        if t%8==0:
            for side in [-3,3]:
                w.box(px+dz*side,py,pz+dx*side,px+dz*side,py+5,pz+dx*side,'stripped_spruce_log')
            w.line((px-dz*3,py+5,pz-dx*3),(px+dz*3,py+5,pz+dx*3),'spruce_planks')
            w.put(px+dz*2,py+4,pz+dx*2,'lantern[hanging=true]')
        if t%5==0 and t>12:
            for side in [-4,4]:w.sphere(px+dz*side,py+2,pz+dx*side,2,2,2,'copper_ore' if rid<2 else 'deepslate_iron_ore')
        if t<7:w.put(px,py+1,pz,'rail[shape='+('north_south' if dz else 'east_west')+']')
    px=x+dx*57;pz=z+dz*57;py=y-24
    w.sphere(px,py+4,pz,11,8,11,'air');w.disk(px,py-2,pz,8,'calcite')
    for i in range(8):
        a=i*math.tau/8;crystal(w,px+round(7*math.cos(a)),py-1,pz+round(7*math.sin(a)),2,5+i%4,rid==2)
    w.box(x-2,y-1,z-2,x+2,y-1,z+2,'stone_bricks')
    sign(w,x+4,y+1,z-2,[name,'Galerie accessible','Veines anciennes',''],8)
    w.poi(name,'grotte',x,z,y+1,REGIONS[rid])


def wilderness(w):
    rng=random.Random(SEED+79)
    print('Forêts, marais, rochers, cristaux et sous-bois...',flush=True)
    occupied=np.zeros((800,800),bool)
    count={}
    # Jittered grid and density fields retain open hunting grounds around woods.
    for gz in range(-366,367,8):
        for gx in range(-366,367,8):
            x=gx+rng.randint(-3,3);z=gz+rng.randint(-3,3);ix=x+400;iz=z+400
            if w.reserved[iz-6:iz+7,ix-6:ix+7].any() or occupied[iz-4:iz+5,ix-4:ix+5].any():continue
            y=w.ground(x,z);rid=w.region(x,z)
            if y<64 or w.water[iz,ix]>y or abs(w.ground(x+4,z)-y)>5:continue
            p=[.68,.33,.62,.19,.29][rid]
            if rng.random()>p:continue
            if rid==0:
                if rng.random()<.08:mushroom(w,x,y+1,z,rng);kind='champignon géant'
                else:oak(w,x,y+1,z,rng,bool(w.swamp[iz,ix]));kind='arbre'
            elif rid==1:dead_tree(w,x,y+1,z,rng);kind='arbre mort'
            elif rid==2:pine(w,x,y+1,z,rng);kind='sapin'
            elif rid==3:
                if (x-265)**2+(z+310)**2<75**2:continue
                h=rng.randint(3,6);w.box(x,y+1,z,x,y+h,z,'cactus');kind='cactus'
            else:crystal(w,x,y+1,z,rng.randint(2,3),rng.randint(6,12));kind='cristal naturel'
            occupied[iz-5:iz+6,ix-5:ix+6]=True;count[kind]=count.get(kind,0)+1
    for _ in range(1350):
        x=rng.randint(-367,367);z=rng.randint(-367,367);ix=x+400;iz=z+400;y=w.ground(x,z);rid=w.region(x,z)
        if w.reserved[iz-4:iz+5,ix-4:ix+5].any() or occupied[iz,ix] or y<64:continue
        r=rng.randint(2,5);h=rng.randint(2,4)
        material=['mossy_cobblestone','cobbled_deepslate','packed_ice','red_sandstone','blackstone'][rid]
        w.sphere(x,y,z,r,h,r-1,material)
        if rid==0:w.sphere(x,y+1,z,r-1,2,r-2 if r>2 else 1,'moss_block',air_only=True)
        if rid==2:w.disk(x,y+h,z,1,'snow[layers=2]')
        count['rocher']=count.get('rocher',0)+1
    plants=[['short_grass','fern','dandelion','poppy','azure_bluet','oxeye_daisy'],
            ['brown_mushroom','red_mushroom','dead_bush'],['fern','short_grass'],['dead_bush'],['end_rod[facing=up]','small_amethyst_bud[facing=up]']]
    for _ in range(41000):
        x=rng.randint(-375,375);z=rng.randint(-375,375);ix=x+400;iz=z+400
        if w.reserved[iz,ix]:continue
        y=w.ground(x,z);rid=w.region(x,z)
        if y<64 or w.get(x,y+1,z)!='minecraft:air':continue
        surface=w.get(x,y,z)
        if rid in [0,1] and surface not in ['minecraft:grass_block','minecraft:podzol','minecraft:coarse_dirt','minecraft:mud']:continue
        if rid==2 and surface!='minecraft:grass_block':continue
        w.put(x,y+1,z,rng.choice(plants[rid]));count['plante']=count.get('plante',0)+1
    # Water plants stay on water, with reeds rooted in the adjacent mud bank.
    for _ in range(1900):
        x=rng.randint(-360,-130);z=rng.randint(130,330);ix=x+400;iz=z+400
        if w.reserved[iz,ix] or not w.swamp[iz,ix]:continue
        if w.get(x,62,z)=='minecraft:water[level=0]':w.put(x,63,z,'lily_pad')
        elif w.ground(x,z) in [62,63] and w.get(x,64,z)=='minecraft:air':w.box(x,64,z,x,rng.randint(65,67),z,'sugar_cane')
    # Fallen logs and fern clusters give clearings a less uniform surface.
    for _ in range(70):
        x=rng.randint(-355,-55);z=rng.randint(-60,320);ix=x+400;iz=z+400
        if w.reserved[iz-6:iz+7,ix-6:ix+7].any() or w.region(x,z)!=0:continue
        y=w.ground(x,z)
        if y>64:w.box(x,y+1,z,x+5,y+1,z,'oak_log[axis=x]');w.put(x+2,y+2,z,'brown_mushroom')
    w.counts.update(count)


def polish(w):
    farms(w)
    mine(w,-322,130,0,'Galerie des Racines',dx=1,dz=0)
    mine(w,-105,-280,2,'Mine du Givre',dx=0,dz=-1)
    mine(w,140,-315,3,'Fonderie des Cendres',dx=1,dz=0)
    mine(w,342,210,4,'Géode des Échos',dx=-1,dz=0)
    # Reserve the caldera and its lava channels before vegetation.
    w.reserve(265,-310,78)
    wilderness(w)
