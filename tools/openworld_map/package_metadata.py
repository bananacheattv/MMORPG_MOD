"""Prepare server settings and independent native-read checks from the generated artifacts."""
import sys,json
from pathlib import Path
import numpy as np
out=Path(sys.argv[1]);meta=json.loads((out/'map.json').read_text(encoding='utf-8'))
a=np.load(out/'blocks.npy',mmap_mode='r');terrain=np.load(out/'terrain.npz')
settings={'centerX':0,'centerZ':0,'mapRadius':400,'safeRadius':58,'safeAreas':meta['safe_areas']}
(out/'general-openworld-fragment.json').write_text(json.dumps({'openWorld':settings},ensure_ascii=False,indent=2),encoding='utf-8')
samples=[]
for p in meta['pois']:
    x,y,z=int(p['x']),int(p['y'])-1,int(p['z'])
    if -64<=y<=255:samples.append({'x':x,'y':y,'z':z,'state':meta['palette'][int(a[y+64,z+400,x+400])],'name':p['name']})
biomes=[]
for x,z in [(-300,80),(-245,240),(55,320),(-235,-300),(300,-55),(280,285),(0,0)]:
    ix=(x+400)//4*4;iz=(z+400)//4*4
    biomes.append({'x':x,'y':90,'z':z,'biome':meta['biome_palette'][int(terrain['biomes'][iz,ix])]})
check={'samples':samples,'biomes':biomes,'signs':meta['signs'],'safeAreas':meta['safe_areas']}
(out/'native-checks.json').write_text(json.dumps(check,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'{len(samples)} block samples, {len(biomes)} biome samples, {len(meta["signs"])} signs.')
