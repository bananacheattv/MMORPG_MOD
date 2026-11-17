"""Cartographic previews sampled from the produced voxels, never concept illustrations."""
import argparse,json,zipfile,io,math
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw,ImageFont

def colors(palette,jar):
    z=zipfile.ZipFile(jar);files=set(z.namelist());out=[]
    overrides={'water':(40,101,143),'lava':(244,102,22),'grass_block':(102,143,60),'dirt_path':(153,127,76),
        'oak_leaves':(70,113,43),'birch_leaves':(100,139,57),'spruce_leaves':(48,86,71),'mangrove_leaves':(86,115,43),
        'dark_oak_leaves':(65,96,41),'flowering_azalea_leaves':(122,131,89),'red_sand':(178,99,53),'snow':(227,237,242)}
    aliases={'chain':'iron_chain','lantern':'lantern','end_rod':'end_rod','ice':'ice','short_grass':'short_grass','snow_block':'snow',
             'smooth_sandstone':'sandstone_top','smooth_quartz':'quartz_block_side','waxed_copper_block':'copper_block','waxed_cut_copper':'cut_copper'}
    for name in palette:
        name=name.split(':')[-1].split('[')[0]
        if name in overrides:out.append(overrides[name]);continue
        simple=aliases.get(name,name)
        for suffix in ['_stairs','_slab','_wall','_fence','_door','_pressure_plate']:
            if simple.endswith(suffix):simple=simple[:-len(suffix)];break
        if simple in ['oak','spruce','dark_oak','birch']:simple+='_planks'
        if simple=='deepslate_tile':simple='deepslate_tiles'
        texture=next((f'assets/minecraft/textures/block/{n}.png' for n in [simple,simple+'_top',simple+'_side',name,name+'_top'] if f'assets/minecraft/textures/block/{n}.png' in files),None)
        if texture:
            im=Image.open(io.BytesIO(z.read(texture))).convert('RGBA');im=im.crop((0,0,im.width,min(im.width,im.height)))
            a=np.asarray(im);valid=a[:,:,3]>20;c=a[:,:,:3][valid].mean(axis=0) if valid.any() else [100,100,100]
            out.append(tuple(int(v) for v in c))
        else:out.append((119,109,104))
    return np.array(out,np.uint8)

def main():
    p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);p.add_argument('--jar',required=True);args=p.parse_args()
    meta=json.loads((args.out/'map.json').read_text(encoding='utf-8'));a=np.load(args.out/'blocks.npy',mmap_mode='r')
    col=colors(meta['palette'],args.jar);(args.out/'colors.json').write_text(json.dumps(col.tolist()),encoding='utf-8')
    h=np.full((800,800),-64,np.int16);ids=np.zeros((800,800),np.uint16)
    ignored=np.array([name=='minecraft:air' or any(s in name for s in ['_sign','_carpet','_bud','end_rod','fern','grass[','short_grass','flower_pot','lantern','wheat','carrots','beetroot','dandelion','poppy','azure_bluet','oxeye_daisy','dead_bush','_mushroom[']) for name in meta['palette']])
    for iy in range(319,-1,-1):
        layer=a[iy];m=(ids==0)&(layer>0)&~ignored[layer]
        h[m]=iy-64;ids[m]=layer[m]
    dz,dx=np.gradient(h.astype(float));shade=np.clip(.88+(-dx*.45-dz*.7)/9,.43,1.28)
    rgb=np.clip(col[ids]*shade[:,:,None],0,255).astype(np.uint8)
    raw=Image.fromarray(rgb).resize((1200,1200),Image.Resampling.NEAREST);raw.save(args.out/'carte-sans-legende.png')
    page=Image.new('RGB',(1540,1375),'#10121b');page.paste(raw,(32,124));d=ImageDraw.Draw(page)
    font=lambda n:ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',n)
    bold=lambda n:ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf',n)
    d.text((32,22),'AÉLORIA  /  LES CINQ HORIZONS',font=bold(34),fill='#eed6a0')
    d.text((34,73),'OPENWORLD · 800 × 800 BLOCS · CARTE DES BLOCS PRODUITS',font=font(18),fill='#bdacca')
    d.rectangle((31,123,1233,1325),outline='#655c77',width=2)
    for poi in meta['pois']:
        if poi['kind'] not in ['monument','village','grotte']:continue
        px=32+(poi['x']+400)*1.5;py=124+(poi['z']+400)*1.5
        ismon=poi['kind']=='monument';r=5 if ismon else 3
        d.ellipse((px-r,py-r,px+r,py+r),fill='#f5cc78' if ismon else '#fff2d4',outline='#17121e')
    labels=[(-335,75,'SYLVERONCE',(-70,-8)),(-290,295,'BRUMEVERT',(-56,8)),(5,176,'VAL DES CRYPTES',(-52,0)),
            (-285,-350,'HAUTS DE BORÉALIS',(-60,0)),(80,-327,'DUNES D’IGNIS',(-20,0)),(164,332,'ÉCLATS D’ASTRÉON',(-36,0))]
    for x,z,t,(ox,oy) in labels:
        px=32+(x+400)*1.5+ox;py=124+(z+400)*1.5+oy;bb=d.textbbox((px,py),t,font=bold(16))
        d.rounded_rectangle((bb[0]-7,bb[1]-4,bb[2]+7,bb[3]+4),4,fill='#181521');d.text((px,py),t,font=bold(16),fill='#ffe9b5')
    xx=1260;yy=145
    for title,lines in [
        ('LES RÉGIONS',['Forêt ancienne et marais','Cryptes et landes','Montagnes gelées','Désert et volcan','Néant astral']),
        ('LES MONUMENTS',['Arbre-sanctuaire','Abbaye des Veilleurs','Citadelle boréale','Temple du Soleil brisé','Observatoire astral']),
        ('À EXPLORER',['7 lieux habités','71 bâtiments meublés','21 étals de marché','4 galeries souterraines','15 haltes et petites ruines','6 jardins cultivés']),
        ('REPÈRES',['Arrivée : 0 / 84 / 0','Nord en haut','X et Z : −400 à 399'])]:
        d.text((xx,yy),title,font=bold(18),fill='#e4c685');yy+=38
        for s in lines:d.text((xx,yy),s,font=font(16),fill='#ded7e8');yy+=29
        yy+=31
    d.line((1262,1210,1412,1210),fill='#e4c685',width=3);d.text((1262,1220),'100 blocs',font=font(16),fill='#ded7e8')
    d.text((34,1337),'Vue cartographique calculée depuis les fichiers de blocs. Ce n’est pas une capture du client Minecraft.',font=font(16),fill='#9990a5')
    page.save(args.out/'carte-openworld.png')
    np.savez_compressed(args.out/'overview.npz',heights=h,ids=ids,colors=col)
    print(args.out/'carte-openworld.png')

if __name__=='__main__':main()
