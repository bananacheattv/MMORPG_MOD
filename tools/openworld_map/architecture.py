"""Authored voxel architecture: furnished villages and five unique landmarks."""
import math, random
import numpy as np
from world import REGIONS, SEED

THEMES = [
 dict(wall='white_terracotta',beam='stripped_spruce_log',base='mossy_stone_bricks',floor='spruce_planks',roof='dark_oak',glass='green_stained_glass',accent='moss_block'),
 dict(wall='tuff_bricks',beam='dark_oak_log',base='cobbled_deepslate',floor='dark_oak_planks',roof='deepslate_tile',glass='purple_stained_glass',accent='polished_blackstone_bricks'),
 dict(wall='stripped_spruce_log',beam='spruce_log',base='stone_bricks',floor='spruce_planks',roof='prismarine',glass='light_blue_stained_glass',accent='packed_ice'),
 dict(wall='smooth_sandstone',beam='cut_red_sandstone',base='red_sandstone',floor='smooth_sandstone',roof='waxed_cut_copper',glass='orange_stained_glass',accent='red_terracotta'),
 dict(wall='calcite',beam='polished_blackstone_bricks',base='polished_deepslate',floor='polished_blackstone',roof='purpur',glass='purple_stained_glass',accent='amethyst_block'),
]

class Local:
    def __init__(self,w,x,y,z,rot=0):self.w=w;self.x=x;self.y=y;self.z=z;self.rot=rot%4
    def at(self,x,y,z):
        for _ in range(self.rot):x,z=-z,x
        return self.x+x,self.y+y,self.z+z
    def state(self,b):
        directions=['north','east','south','west']
        for d in directions:
            if 'facing='+d in b:
                b=b.replace('facing='+d,'facing='+directions[(directions.index(d)+self.rot)%4]);break
        if self.rot%2:
            if 'axis=x' in b:b=b.replace('axis=x','axis=z')
            elif 'axis=z' in b:b=b.replace('axis=z','axis=x')
        return b
    def p(self,x,y,z,b):self.w.put(*self.at(x,y,z),self.state(b))
    def box(self,x1,y1,z1,x2,y2,z2,b):self.w.box(*self.at(x1,y1,z1),*self.at(x2,y2,z2),self.state(b))
    def line(self,a,b,block,r=0):self.w.line(self.at(*a),self.at(*b),self.state(block),r)

def sign(w,x,y,z,text,rotation=0):
    w.put(x,y,z,f'oak_sign[rotation={rotation}]')
    w.signs.append(dict(x=int(x),y=int(y),z=int(z),lines=text[:4]))

def lamp(w,x,y,z,rid=0):
    stone=['mossy_stone_bricks','deepslate_bricks','stone_bricks','cut_sandstone','polished_blackstone_bricks'][rid]
    w.put(x,y,z,stone);w.box(x,y+1,z,x,y+3,z,'iron_chain[axis=y]');w.put(x,y+4,z,'lantern')

def level_site(w,b,y,hw,hl,margin=2,skirt=4):
    """Terrain under and around a building: flat at its foundation (y-1) over the footprint + margin, then a
    smooth skirt back to the natural ground, so no house is buried in a slope or hangs above it."""
    seen=set()
    for dz in range(-hl-margin-skirt-2,hl+margin+skirt+3):
        for dx in range(-hw-margin-skirt-2,hw+margin+skirt+3):
            cx,_,cz=b.at(dx,0,dz)
            if (cx,cz) in seen or not -399<=cx<=398 or not -399<=cz<=398:continue
            seen.add((cx,cz))
            ox=max(0,abs(dx)-hw-margin);oz=max(0,abs(dz)-hl-margin);d=math.hypot(ox,oz)
            if d>=skirt or (d>0 and w.roads[cz+400,cx+400]):continue
            k=d/skirt;k=k*k*(3-2*k)
            g=w.ground(cx,cz);target=int(round((y-1)*(1-k)+g*k))
            if g==target:continue
            top=w.get(cx,g,cz)
            if 'air' in top or 'water' in top or 'leaves' in top:top='minecraft:grass_block'
            if g>target:w.box(cx,target+1,cz,cx,g+6,cz,'air')
            else:w.box(cx,g+1,cz,cx,target,cz,'dirt')
            w.put(cx,target,cz,top)
            w.height[cz+400,cx+400]=target

def house(w,x,y,z,rid,index,rot=0,role='Maison'):
    t=THEMES[rid];b=Local(w,x,y,z,rot)
    hw=4+(index%3);hl=5+(index%2)*2;stories=2 if index%4==0 else 1;h=5*stories
    level_site(w,b,y,hw,hl)
    b.box(-hw,0,-hl,hw,0,hl,t['base']);b.box(-hw,1,-hl,hw,h,hl,t['wall'])
    b.box(-hw+1,1,-hl+1,hw-1,h-1,hl-1,'air');b.box(-hw+1,0,-hl+1,hw-1,0,hl-1,t['floor'])
    for xx in [-hw,hw]:
        for zz in [-hl,0,hl]:b.box(xx,1,zz,xx,h+1,zz,t['beam'])
    for yy in [1,5] if stories==2 else [1]:
        for zz in [-hl,hl]:b.box(-hw,yy,zz,hw,yy,zz,t['beam'])
    if stories==2:
        b.box(-hw+1,5,-hl+1,hw-1,5,hl-1,t['floor'])
        b.box(0,1,hl-1,0,8,hl-1,'ladder[facing=north]')
    # Windows have recessed glass, framed sills and projecting shutters.
    for yy in range(2,h,5):
        for zz in [-hl,hl]:
            for xx in [-hw+2,hw-2]:
                b.box(xx,yy,zz,xx,yy+1,zz,t['glass']);b.p(xx,yy-1,zz-1 if zz<0 else zz+1,t['base'])
        for xx in [-hw,hw]:
            for zz in [-hl+2,hl-2]:b.box(xx,yy,zz,xx,yy+1,zz,t['glass'])
    b.box(-1,1,-hl,0,3,-hl,'air')
    for xx in [-1,0]:
        b.p(xx,1,-hl,'spruce_door[half=lower,facing=north,hinge=left,open=false]')
        b.p(xx,2,-hl,'spruce_door[half=upper,facing=north,hinge=left,open=false]')
    b.box(-2,0,-hl-2,2,0,-hl-1,t['base'])
    b.p(2,3,-hl-1,'lantern[hanging=true]')
    if rid==3:
        b.box(-hw-1,h+1,-hl-1,hw+1,h+1,hl+1,'smooth_sandstone')
        for xx in range(-hw-1,hw+2,2):b.p(xx,h+2,-hl-1,'sandstone_wall');b.p(xx,h+2,hl+1,'sandstone_wall')
        for zz in range(-hl,hl+1,2):b.p(-hw-1,h+2,zz,'sandstone_wall');b.p(hw+1,h+2,zz,'sandstone_wall')
        # Roof terrace, textiles and a sunshade.
        b.box(-2,h+2,-2,2,h+2,2,'red_carpet');b.box(-3,h+2,3,-3,h+5,3,'spruce_fence')
        b.box(3,h+2,3,3,h+5,3,'spruce_fence');b.box(-3,h+5,0,3,h+5,3,'orange_wool')
    else:
        for xx in range(-hw-1,hw+2):
            ry=h+1+max(0,hw+1-abs(xx))
            if abs(xx)<=hw:
                for zz in [-hl,hl]:b.box(xx,h+1,zz,xx,ry-1,zz,t['wall'])
            roof=t['roof']+'_stairs[facing='+('east' if xx<0 else 'west')+']'
            b.box(xx,ry,-hl-1,xx,ry,hl+1,roof)
            if rid==2 and (xx+index)%3==0:b.box(xx,ry+1,-hl-1,xx,ry+1,hl+1,'snow[layers=1]')
        b.box(0,h+hw+3,-hl-2,0,h+hw+3,hl+2,t['roof']+'_slab[type=bottom]')
        # Dormer on the long roof, changing side and scale by house.
        if stories==2:
            b.box(-2,h+2,-hl-1,2,h+5,-hl+2,t['beam']);b.box(-1,h+3,-hl-2,1,h+4,-hl-2,t['glass'])
            for k in range(4):b.box(-3+k,h+5+k,-hl-2,3-k,h+5+k,-hl+2,t['roof']+'_slab[type=bottom]')
    # Chimney, hearth, bed, storage, dining table, shelves: every house is furnished.
    b.box(hw-2,1,hl-2,hw-1,h+hw+4,hl-1,'bricks')
    b.p(hw-2,h+hw+5,hl-2,'campfire[lit=true]')
    b.p(hw-2,1,hl-3,'furnace[facing=north]')
    b.p(-hw+2,1,hl-2,'red_bed[part=head,facing=south]');b.p(-hw+2,1,hl-3,'red_bed[part=foot,facing=south]')
    b.p(-hw+1,1,0,'barrel[facing=up]');b.p(-hw+1,2,0,'flower_pot')
    b.box(-hw+1,1,hl-1,-hw+3,2,hl-1,'bookshelf')
    b.box(-1,1,1,2,1,3,'brown_carpet')
    b.p(1,1,0,'spruce_fence');b.p(1,2,0,'spruce_pressure_plate');b.p(1,1,2,'spruce_stairs[facing=north]')
    b.p(1,1,-2,'spruce_stairs[facing=south]');b.p(0,h-1,0,'lantern[hanging=true]')
    if role in ['Bibliothèque','Archives']:b.box(-hw+1,1,-hl+1,-hw+1,3,-1,'bookshelf');b.p(2,1,hl-2,'lectern[facing=north]')
    if role in ['Forge','Atelier']:b.p(2,1,hl-2,'anvil[facing=north]');b.p(0,1,hl-2,'smithing_table')
    sx,sy,sz=b.at(-3,1,-hl-3);sign(w,sx,sy,sz,[role,REGIONS[rid].split(' ')[0],'Aéloria',''],(rot*4)%16)
    # Foundation and doorstep remain clear and accessible.
    w.poi(role,'bâtiment',x,z,y+1,REGIONS[rid],size=[hw*2+1,h+hw+7,hl*2+1])

def stall(w,x,y,z,rid,rot,colour):
    b=Local(w,x,y,z,rot)
    b.box(-3,0,-2,3,0,2,'spruce_planks')
    for xx in [-3,3]:
        for zz in [-2,2]:b.box(xx,1,zz,xx,4,zz,'spruce_fence')
    for xx in range(-3,4):b.box(xx,4,-3,xx,4,2,colour+'_wool' if xx%2 else 'white_wool')
    b.box(-2,1,-2,2,1,-2,'barrel[facing=up]');b.p(-2,2,-2,'flower_pot');b.p(0,2,-2,'melon');b.p(2,2,-2,'hay_block')
    b.p(0,3,0,'lantern[hanging=true]');w.counts['étal']=w.counts.get('étal',0)+1

def farm(w,x,y,z,rid,rot=0):
    b=Local(w,x,y,z,rot)
    b.box(-10,0,-7,10,0,7,'farmland[moisture=7]')
    for xx in [-5,0,5]:b.box(xx,0,-7,xx,0,7,'water[level=0]')
    for xx in range(-9,10):
        if xx in [-5,0,5]:continue
        for zz in range(-6,7):
            if (xx*3+zz)%11:b.p(xx,1,zz,'wheat[age=7]' if rid!=4 else 'beetroots[age=3]')
    for xx in range(-11,12):
        for zz in [-8,8]:b.p(xx,1,zz,'spruce_fence[east=true,west=true]')
    for zz in range(-7,8):
        for xx in [-11,11]:b.p(xx,1,zz,'spruce_fence[north=true,south=true]')
    b.box(-1,1,-8,1,1,-8,'air');b.p(0,2,0,'hay_block');b.p(0,3,0,'carved_pumpkin[facing=north]')
    b.box(-2,2,0,2,2,0,'spruce_fence');w.counts['jardin cultivé']=w.counts.get('jardin cultivé',0)+1

def village(w,spec):
    name,x,z,r,y,rid=spec;t=THEMES[rid]
    w.poi(name,'village',x,z,y+1,REGIONS[rid])
    w.disk(x,y,z,13,t['base']);w.disk(x,y,z,10,'gravel' if rid in [0,1] else t['floor'])
    # Keep the exact network arrival square unobstructed in the capital.
    fx,fz=(x+9,z-14) if x==z==0 else (x,z)
    w.disk(fx,y,fz,5,t['base']);w.disk(fx,y+1,fz,4,t['base'],3)
    w.disk(fx,y+1,fz,2.9,'water[level=0]');w.box(fx,y+1,fz,fx,y+4,fz,t['accent']);w.put(fx,y+5,fz,'sea_lantern')
    plots=[(-27,-23,0),(-10,-29,0),(12,-29,0),(29,-16,1),(29,6,1),(25,27,2),(5,29,2),(-16,27,2),(-29,10,3),(-29,-7,3)]
    if name=='Brumevert':plots=[(-20,-17,0),(1,-23,0),(20,-16,1),(23,8,1),(3,23,2),(-19,17,2)]
    roles=['Taverne','Atelier','Maison des cartes','Écuries','Maison','Bibliothèque','Forge','Réserve','Maison','Vigie']
    for i,(dx,dz,rot) in enumerate(plots):
        rot=(rot+2)%4
        house(w,x+dx,y,z+dz,rid,i,rot,roles[i])
        # Three-wide access path from each door toward the central square.
        angle=rot*math.pi/2;ex=x+dx+round(9*math.sin(angle));ez=z+dz-round(9*math.cos(angle))
        for t0 in np.linspace(0,1,max(2,int(math.hypot(ex-x,ez-z)))):
            w.disk(round(x*(1-t0)+ex*t0),y,round(z*(1-t0)+ez*t0),1.6,t['base'])
    for i,(dx,dz,rot) in enumerate([(-10,10,0),(10,10,0),(-11,-8,1)]):stall(w,x+dx,y,z+dz,rid,rot,['green','purple','light_blue','orange','magenta'][rid])
    for dx,dz in [(-15,-15),(15,-15),(-15,15),(15,15)]:lamp(w,x+dx,y+1,z+dz,rid)
    for dx,dz,rot in [(0,-r+1,0),(0,r-1,2)]:sign(w,x+dx,y+1,z+dz,[name,'Village protégé','Les Cinq Horizons','Aéloria'],rot*4)
    if name!='Brumevert':
        # Small vegetable beds and a shared outdoor eating area.
        for dx in [-10,-6]:
            w.box(x+dx,y,z+15,x+dx+2,y,z+19,'farmland[moisture=7]')
            w.box(x+dx,y+1,z+15,x+dx+2,y+1,z+19,'carrots[age=7]')
    else:
        # Marsh village: pilings, boardwalks, fishing pontoons and a small skiff.
        for dx in [-33,-15,15,33]:
            w.box(x+dx,59,z-31,x+dx,y,z-31,'stripped_dark_oak_log')
        w.box(x-33,y,z-32,x+33,y,z-30,'dark_oak_planks')
        for dz in [-32,32]:w.box(x-2,y,z+dz,x+2,y,z+dz+13,'dark_oak_planks')
        w.box(x+6,63,z+37,x+10,63,z+42,'dark_oak_planks');w.box(x+7,64,z+38,x+9,64,z+41,'air')
        w.box(x+8,64,z+40,x+8,71,z+40,'spruce_fence');w.box(x+9,68,z+40,x+12,71,z+40,'white_wool')

def tree_sanctuary(w):
    x,z,y=-315,-5,86
    for dy in range(48):
        r=max(3,8-dy//10);w.disk(x+round(2*math.sin(dy/15)),y+dy,z,r,'oak_log')
    for i in range(9):
        a=i*math.tau/9
        root=(x+round(23*math.cos(a)),y-1,z+round(23*math.sin(a)))
        w.line((x,y+5,z),root,'oak_log',2)
        branch=(x+round(24*math.cos(a)),y+36+(i%3)*5,z+round(24*math.sin(a)))
        w.line((x,y+25,z),branch,'oak_log',2)
        w.sphere(*branch,15,9,13,'oak_leaves[persistent=true]')
        w.sphere(branch[0]-3,branch[1]+6,branch[2],10,7,10,'flowering_azalea_leaves[persistent=true]')
    w.sphere(x,y+52,z,23,13,20,'oak_leaves[persistent=true]')
    for yy in range(y+1,y+24):w.disk(x,yy,z,4,'air')
    w.box(x-3,y+1,z-10,x+3,y+7,z-2,'air')
    w.disk(x,y,z,5,'mossy_stone_bricks');w.box(x-2,y+1,z+1,x+2,y+1,z+3,'chiseled_stone_bricks')
    w.put(x,y+2,z+2,'amethyst_block');w.put(x,y+3,z+2,'end_rod[facing=up]')
    for a in np.linspace(0,math.tau*1.8,170):
        xx=x+round(11*math.cos(a));zz=z+round(11*math.sin(a));yy=y+2+int(a*2.2)
        w.box(xx-1,yy,zz-1,xx+1,yy,zz+1,'dark_oak_planks')
    for i in range(12):
        a=i*math.tau/12;xx=x+round(25*math.cos(a));zz=z+round(25*math.sin(a))
        w.box(xx-1,y,zz-1,xx+1,y+3+i%3,zz+1,'mossy_cobblestone');w.put(xx,y+4+i%3,zz,'glowstone')
    w.poi('Arbre-sanctuaire de Sylveronce','monument',x,z,y+1,REGIONS[0])
    sign(w,x,y+1,z-24,['Sanctuaire de Sylveronce','Le Cœur des Racines','Chemin des anciens',''],8)

def tower(w,x,y,z,r,h,stone,roof,ice=False):
    for dy in range(h):
        w.disk(x,y+dy,z,r+(1 if dy in [0,1,h-3] else 0),stone,max(0,r-2))
        if dy%9==0:w.disk(x,y+dy,z,r-1,stone)
    for dy in range(5,h-4,9):
        for dx,dz in [(0,r),(0,-r),(r,0),(-r,0)]:
            w.box(x+dx,y+dy,z+dz,x+dx,y+dy+3,z+dz,'light_blue_stained_glass' if ice else 'purple_stained_glass')
            w.put(x+dx,y+dy-1,z+dz,'sea_lantern')
    for dy in range(r*2+6):w.disk(x,y+h+dy,z,max(0,r+2-dy*.55),roof)
    w.put(x,y+h+r*2+6,z,'lightning_rod[facing=up]')
    # Spiral stair inside the tower, with enough headroom between turns.
    for i in range(h*4):
        a=i*math.pi/6;yy=y+i//4
        xx=x+round((r-2)*math.cos(a));zz=z+round((r-2)*math.sin(a));w.put(xx,yy,zz,stone)

def abbey(w):
    x,z,y=55,332,91;b=Local(w,x,y,z)
    b.box(-15,0,-29,15,1,26,'stone_bricks');b.box(-15,2,-24,15,27,24,'stone_bricks')
    b.box(-13,2,-22,13,26,22,'air')
    b.box(-3,1,-25,3,13,-21,'air')
    for zz in [-18,-9,0,9,18]:
        for xx in [-15,15]:
            b.box(xx-1,1,zz-1,xx+1,17,zz+1,'cobbled_deepslate')
            b.box(xx,7,zz+3,xx,18,zz+5,'purple_stained_glass')
        for xx in [-10,10]:b.box(xx,2,zz,xx,20,zz,'polished_andesite')
        for xx in range(-11,12):
            yy=21+int(6*(1-(abs(xx)/12)**1.4));b.box(xx,yy,zz,xx,yy+1,zz,'deepslate_bricks')
    for xx in range(-16,17):
        ry=27+(16-abs(xx))//2
        b.box(xx,ry,-26,xx,ry,26,'deepslate_tiles')
        if abs(xx)%5==0:b.box(xx,ry+1,-26,xx,ry+1,26,'polished_blackstone_bricks')
    # Rose window and stepped archivolts frame the western facade.
    for dx in range(-7,8):
        for dy in range(-7,8):
            d=dx*dx+dy*dy
            if d<=49:b.p(dx,20+dy,-25,'calcite' if d>35 or dx==0 or dy==0 else 'purple_stained_glass')
    for sx in [-22,22]:
        tower(w,x+sx,y,z-22,6,41,'deepslate_bricks','deepslate_tiles')
        w.box(x+sx-2,y+1,z-28,x+sx+2,y+6,z-24,'air')
    for zz in range(-14,15,5):
        for xx in [-8,-5,5,8]:b.box(xx,2,zz,xx+1,2,zz+2,'dark_oak_stairs[facing=north]')
    b.box(-4,2,17,4,3,21,'polished_blackstone_bricks');b.box(-1,4,19,1,4,20,'gold_block')
    for xx in [-6,6]:b.p(xx,4,19,'candle[candles=4,lit=true]')
    # Accessible crypt and burial chambers beneath the nave.
    b.box(-11,-10,-17,11,-3,19,'deepslate_bricks');b.box(-9,-9,-15,9,-4,17,'air')
    for i in range(12):
        b.box(9,-i+1,-15+i,11,-i+4,-13+i,'air');b.box(9,-i,-15+i,11,-i,-13+i,'stone_brick_stairs[facing=north]')
    for xx in [-7,7]:
        for zz in [-9,0,9]:b.box(xx-1,-9,zz-2,xx+1,-8,zz+2,'polished_deepslate');b.p(xx,-7,zz,'skeleton_skull')
    for zz in [-10,0,10]:b.p(0,-4,zz,'soul_lantern[hanging=true]')
    # Grave garden, small chapels, memorial crosses and a walled path.
    for row in range(5):
        for col in range(4):
            gx=x+30+col*4;gz=z-21+row*8;gy=w.ground(gx,gz)
            w.box(gx,gy,gz,gx+1,gy,gz+3,'coarse_dirt');w.box(gx,gy+1,gz,gx,gy+3,gz,'stone_bricks');w.box(gx-1,gy+2,gz,gx+1,gy+2,gz,'stone_bricks')
    w.poi('Abbaye des Veilleurs et crypte funéraire','monument',x,z-27,y+1,REGIONS[1])
    sign(w,x-6,y+2,z-29,['Abbaye des Veilleurs','Le silence a une mémoire','Crypte sous la nef',''],8)

def ice_citadel(w):
    x,z,y=-235,-300,147
    w.disk(x,y,z,39,'stone_bricks');w.disk(x,y+1,z,36,'packed_ice')
    for xx in [-27,27]:w.box(x+xx-1,y+2,z-27,x+xx+1,y+13,z+27,'stone_bricks')
    for zz in [-27,27]:w.box(x-27,y+2,z+zz-1,x+27,y+13,z+zz+1,'stone_bricks')
    for i in range(-26,27,3):
        for zz in [-27,27]:w.box(x+i,y+14,z+zz-1,x+i+1,y+15,z+zz+1,'stone_bricks')
        for xx in [-27,27]:w.box(x+xx-1,y+14,z+i,x+xx+1,y+15,z+i+1,'stone_bricks')
    for xx in [-27,27]:
        for zz in [-27,27]:tower(w,x+xx,y+2,z+zz,6,31,'stone_bricks','blue_ice',True)
    w.box(x-5,y+2,z+25,x+5,y+12,z+31,'air')
    w.box(x-11,y+2,z-15,x+11,y+29,z+10,'calcite');w.box(x-9,y+2,z-13,x+9,y+27,z+8,'air')
    w.box(x-3,y+2,z+8,x+3,y+12,z+12,'air')
    for xx in [-11,11]:
        for zz in [-10,-2,6]:w.box(x+xx,y+8,z+zz,x+xx,y+22,z+zz+2,'blue_stained_glass')
    for k in range(13):w.box(x-13+k,y+30+k,z-18,x+13-k,y+30+k,z+13,'prismarine_bricks')
    w.box(x-8,y+2,z-12,x+8,y+2,z+7,'polished_andesite')
    w.box(x-2,y+3,z-9,x+2,y+4,z-6,'blue_ice');w.box(x-1,y+5,z-9,x+1,y+9,z-8,'blue_ice')
    for xx in [-7,7]:
        for zz in [-9,0,7]:w.box(x+xx,y+3,z+zz,x+xx,y+16,z+zz,'packed_ice');w.put(x+xx,y+17,z+zz,'sea_lantern')
    for xx in [-17,17]:crystal(w,x+xx,y+2,z+15,4,18,ice=True)
    w.box(x-3,y+1,z+11,x+3,y+1,z+31,'blue_ice')
    w.poi('Citadelle du Souffle boréal','monument',x,z+30,y+2,REGIONS[2])
    sign(w,x-7,y+2,z+31,['Citadelle de Boréalis','Gardienne du Glacier','La lumière veille',''],0)

def crystal(w,x,y,z,r,h,ice=False):
    for dy in range(h):
        k=dy/h;rr=r*(.65+.35*min(1,k*4))*(min(1,(1-k)*3))
        for dz in range(-r,r+1):
            for dx in range(-r,r+1):
                if abs(dx)+abs(dz)*.9<=rr:
                    b=('blue_ice' if (dx-dz)%3 else 'packed_ice') if ice else ('amethyst_block' if (dx-dz)%3 else 'purple_stained_glass')
                    if abs(dx)+abs(dz)<rr*.25:b='sea_lantern'
                    w.put(x+dx,y+dy,z+dz,b)

def sun_temple(w):
    x,z,y=300,-55,105
    for tier in range(5):
        r=34-tier*5
        w.box(x-r,y+tier*4,z-r,x+r,y+tier*4+3,z+r,'smooth_sandstone')
        w.box(x-r,y+tier*4,z-r,x+r,y+tier*4,z-r,'cut_red_sandstone')
        for xx in range(-r+3,r-2,6):w.put(x+xx,y+tier*4+2,z+r,'chiseled_sandstone')
    for i in range(22):w.box(x-4,y+i,z+35-i,x+4,y+i,z+36-i,'sandstone_stairs[facing=north]')
    w.box(x-12,y+20,z-12,x+12,y+34,z+9,'smooth_sandstone');w.box(x-9,y+20,z-10,x+9,y+32,z+7,'air')
    w.box(x-4,y+20,z+7,x+4,y+29,z+12,'air')
    for dx in [-11,11]:
        for dz in [-10,9]:w.box(x+dx,y+20,z+dz,x+dx,y+38,z+dz,'cut_red_sandstone')
    w.box(x-14,y+34,z-14,x+14,y+36,z+12,'red_sandstone')
    for dx in [-22,22]:
        w.box(x+dx-3,y+5,z+21,x+dx+3,y+26,z+27,'cut_red_sandstone')
        w.box(x+dx-2,y+27,z+22,x+dx+2,y+30,z+26,'gold_block')
    for a in np.linspace(0,math.tau,230):
        w.sphere(x+18*math.cos(a),y+53+18*math.sin(a),z-3,1.3,1.3,1.3,'gold_block')
    for i in range(12):
        a=i*math.tau/12;w.line((x+18*math.cos(a),y+53+18*math.sin(a),z-3),(x+23*math.cos(a),y+53+23*math.sin(a),z-3),'waxed_copper_block',1)
    w.sphere(x,y+53,z-3,6,6,3,'orange_stained_glass');w.sphere(x,y+53,z-3,3,3,2,'glowstone')
    w.box(x-3,y+20,z-8,x+3,y+22,z-4,'gilded_blackstone');w.put(x,y+23,z-6,'candle[candles=4,lit=true]')
    w.poi('Temple du Soleil brisé','monument',x,z+36,y+1,REGIONS[3])
    sign(w,x-7,y+1,z+36,['Temple du Soleil brisé','Les Dunes d’Ignis','Le brasier se souvient',''],0)

def observatory(w):
    x,z,y=280,285,124
    for dy in range(7):w.disk(x,y+dy,z,36-dy//2,'polished_blackstone_bricks')
    w.disk(x,y+7,z,32,'smooth_quartz');w.disk(x,y+7,z,29,'polished_deepslate')
    for r in [12,22,29]:w.disk(x,y+7,z,r,'amethyst_block',r-1)
    for i in range(8):
        a=i*math.tau/8;px=x+round(28*math.cos(a));pz=z+round(28*math.sin(a))
        w.box(px-2,y+7,pz-2,px+2,y+28,pz+2,'purpur_pillar');crystal(w,px,y+29,pz,3,13)
        w.line((x,y+7,z),(px,y+7,pz),'quartz_block')
    # Three perpendicular orbital rings, built from actual cubic blocks.
    for axis in range(3):
        for a in np.linspace(0,math.tau,320):
            p=np.array([23*math.cos(a),23*math.sin(a),0.])
            tilt=(axis+1)*math.pi/5
            if axis==1:p=np.array([p[0],p[1]*math.cos(tilt),p[1]*math.sin(tilt)])
            if axis==2:p=np.array([p[1]*math.sin(tilt),p[0],p[1]*math.cos(tilt)])
            w.sphere(x+p[0],y+37+p[1],z+p[2],1.1,1.1,1.1,['quartz_block','gold_block','amethyst_block'][axis])
    w.sphere(x,y+37,z,7,7,7,'purple_stained_glass');w.sphere(x,y+37,z,4,4,4,'sea_lantern')
    w.box(x-3,y+8,z-3,x+3,y+10,z+3,'chiseled_quartz_block');crystal(w,x,y+11,z,3,13)
    for i in range(9):w.box(x-4,y+i,z+37-i,x+4,y+i,z+39-i,'polished_blackstone_brick_stairs[facing=north]')
    for i,(dx,dz,iy,r) in enumerate([(66,-24,149,17),(49,-70,170,14),(-54,-38,148,12)]):
        for dy in range(30):w.disk(x+dx,iy-dy,z+dz,max(1,r*(1-dy/31)**.65),'blackstone')
        w.disk(x+dx,iy,z+dz,r,'end_stone');crystal(w,x+dx,iy+1,z+dz,4,17)
        a=(x+int(dx*.4),y+7,z+int(dz*.4));b=(x+dx,iy,z+dz)
        length=int(math.dist(a,b))
        for t in np.linspace(0,1,length*2):
            px=a[0]*(1-t)+b[0]*t;pz=a[2]*(1-t)+b[2]*t;py=a[1]*(1-t)+b[1]*t
            w.box(px-1,py,pz-1,px+1,py,pz+1,'purpur_block')
    w.poi('Observatoire des Constellations','monument',x,z+38,y+1,REGIONS[4])
    sign(w,x-7,y+1,z+38,['Observatoire d’Astréon','Au-delà des Cinq Horizons','Passerelles stellaires',''],0)

def volcano(w):
    x,z=265,-310;y=w.ground(x,z)
    for yy in range(y-22,256):w.disk(x,yy,z,15,'air')
    for yy in range(y-23,y-16):w.disk(x,yy,z,14,'lava[level=0]')
    for i in range(5):
        a=i*math.tau/5+.35
        for r in range(16,65):
            xx=x+round(r*math.cos(a)+3*math.sin(r/8));zz=z+round(r*math.sin(a))
            if not(-390<xx<390 and -390<zz<390):continue
            yy=w.ground(xx,zz)
            w.disk(xx,yy,zz,3,'blackstone');w.disk(xx,yy+1,zz,1.3,'magma_block' if r>49 else 'lava[level=0]')
    for i in range(18):
        a=i*math.tau/18;xx=x+round(20*math.cos(a));zz=z+round(20*math.sin(a));yy=w.ground(xx,zz)
        w.box(xx-1,yy,zz-1,xx+1,yy+4+i%4,zz+1,'basalt[axis=y]')
    w.poi('Caldeira du Brasier ancien','site naturel',x,z,y+2,REGIONS[3])

def secondary_sites(w):
    # Authored ruins/camps punctuate the walks rather than filling them uniformly.
    sites=[(-130,130,0,'Cercle des druides'),(-340,155,0,'Tour des Roseaux'),(-165,325,0,'Moulin des Brumes'),
           (-35,320,1,'Ossuaire des Errants'),(155,330,1,'Chapelle effondrée'),(110,155,1,'Camp des Veilleurs'),
           (-105,-320,2,'Phare du Glacier'),(-315,-150,2,'Entrée des mines boréales'),(-80,-160,2,'Refuge des cols'),
           (115,-310,3,'Caravane de l’Oasis'),(345,-190,3,'Tour du Vent de cuivre'),(140,-95,3,'Porte des Dunes'),
           (330,65,4,'Éclat du Zénith'),(160,200,4,'Ruines de la Veille astrale'),(345,350,4,'Dernier fanal')]
    for i,(x,z,rid,name) in enumerate(sites):
        y=w.ground(x,z);t=THEMES[rid];w.reserve(x,z,13)
        if i%3==0:
            w.disk(x,y,z,10,t['base'])
            for k in range(8):
                a=k*math.tau/8;px=x+round(8*math.cos(a));pz=z+round(8*math.sin(a))
                w.box(px-1,y+1,pz-1,px+1,y+5+k%3,pz+1,t['beam']);w.put(px,y+6+k%3,pz,'lantern')
            w.put(x,y+1,z,'campfire[lit=true]')
        elif i%3==1:
            tower(w,x,y,z,5,16+i%5,t['base'],t['accent'],rid==2)
            w.box(x-2,y+1,z+3,x+2,y+5,z+6,'air')
        else:
            house(w,x,y,z,rid,i,0,'Refuge')
        sign(w,x-5,y+1,z+10,[name,'Route des Horizons','Aéloria',''],0)
        w.poi(name,'halte d’exploration',x,z,y+1,REGIONS[rid])
    # Stone arch bridges, centred on the real river (the route crossings are wooden bridges built by finish_roads).
    for cz,length in [(-25,45),(160,35)]:
        cx=w.river_x(cz)
        yy=max(w.ground(cx-length//2,cz),w.ground(cx+length//2,cz),72)
        for dx in range(-length//2,length//2+1):
            top=yy+round(3*math.cos(dx/(length/2)*math.pi/2))
            w.box(cx+dx,top,cz-3,cx+dx,top,cz+3,'stone_bricks')
            if abs(dx)>=length//2-2:
                # Abutments: the deck ends rest on solid ground whatever the bank height.
                for dz in range(-3,4):
                    g=w.ground(cx+dx,cz+dz)
                    if g<top:w.box(cx+dx,g,cz+dz,cx+dx,top-1,cz+dz,'stone_bricks')
            if dx%6==0:
                for dz in [-3,3]:
                    g=w.ground(cx+dx,cz+dz)
                    if g<top:w.box(cx+dx,g,cz+dz,cx+dx,top,cz+dz,'mossy_stone_bricks')
            for dz in [-3,3]:w.put(cx+dx,top+1,cz+dz,'stone_brick_wall')
            if dx%8==0:w.reserve(cx+dx,cz,7)
        w.poi('Pont des pèlerins','pont',cx,cz,yy+4,REGIONS[0])

def build_all(w):
    print('Villages meublés et marchés...',flush=True)
    for spec in w.villages:village(w,spec)
    print('Les cinq monuments...',flush=True)
    tree_sanctuary(w);abbey(w);ice_citadel(w);sun_temple(w);observatory(w);volcano(w);secondary_sites(w)
    # The center is a clear arrival plaza, with a compass rose in the pavement.
    w.disk(0,83,0,6,'chiseled_stone_bricks');w.disk(0,83,0,4,'smooth_quartz')
    for k in range(-5,6):w.put(k,83,0,'gold_block');w.put(0,83,k,'gold_block')
    w.box(-1,84,-1,1,87,1,'air')
    sign(w,4,84,7,['Les Cinq Horizons','Bienvenue en Aéloria','Choisis ta route','Explore les cinq régions'],0)
