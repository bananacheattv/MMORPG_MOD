"""Sponge v3 sectors with block and biome data; compatible with AeloriaImport."""
from pathlib import Path
import csv, hashlib, json, gzip, io
import numpy as np
import nbtlib as nbt
from world import SIZE,YMIN,YMAX


def varints(values):
    values=np.asarray(values).ravel()
    if values.max(initial=0)<128:return values.astype(np.uint8).view(np.int8)
    lens=np.where(values<128,1,2);ends=np.cumsum(lens);out=np.empty(int(ends[-1]),np.uint8)
    out[ends-lens]=(values&127)|np.where(lens==2,128,0)
    mask=lens==2;out[ends[mask]-1]=values[mask]>>7
    return out.view(np.int8)


def export(w):
    folder=w.out/'schematics';folder.mkdir(exist_ok=True)
    rows=[];palettes=[]
    for rz in range(10):
        for rx in range(10):
            order=rz*10+rx+1;file=f'cinq_horizons_{order:03d}.schem'
            blocks=w.a[:,rz*80:(rz+1)*80,rx*80:(rx+1)*80].copy()
            used,indices=np.unique(blocks,return_inverse=True)
            if len(used)>256:raise ValueError(f'{file}: {len(used)} states, importer allows 256')
            palms={w.palette[int(old)]:nbt.Int(i) for i,old in enumerate(used)}
            biome=w.biomes[rz*80:(rz+1)*80,rx*80:(rx+1)*80].copy()
            # Minecraft stores biomes at 4x4x4; quantize now so export and native verification agree.
            biome=np.repeat(np.repeat(biome[::4,::4],4,axis=0),4,axis=1)
            broot=np.broadcast_to(biome,(YMAX-YMIN+1,80,80)).copy()
            schematic=nbt.Compound({
                'Version':nbt.Int(3),'DataVersion':nbt.Int(5023),
                'Width':nbt.Short(80),'Height':nbt.Short(YMAX-YMIN+1),'Length':nbt.Short(80),
                'Offset':nbt.IntArray([0,0,0]),
                'Metadata':nbt.Compound({'Name':nbt.String(f'Les Cinq Horizons {order}/100'),'Author':nbt.String('Aeloria')}),
                'Blocks':nbt.Compound({'Palette':nbt.Compound(palms),'Data':nbt.ByteArray(varints(indices))}),
                'Biomes':nbt.Compound({'Palette':nbt.Compound({name:nbt.Int(i) for i,name in enumerate(w.biome_names)}),'Data':nbt.ByteArray(varints(broot))})
            })
            buff=io.BytesIO();nbt.File({'Schematic':schematic}).write(buff)
            path=folder/file;path.write_bytes(gzip.compress(buff.getvalue(),compresslevel=6,mtime=0))
            rows.append([order,file,-400+rx*80,YMIN,-400+rz*80,80,YMAX-YMIN+1,80,path.stat().st_size,hashlib.sha256(path.read_bytes()).hexdigest()])
            palettes.append(len(used))
        print(f'Export : {(rz+1)*10}/100 secteurs',flush=True)
    with (folder/'importation.csv').open('w',newline='',encoding='utf-8') as f:
        writer=csv.writer(f);writer.writerow(['ordre','fichier','collage_x','collage_y','collage_z','largeur_x','hauteur_y','longueur_z','octets','sha256']);writer.writerows(rows)
    (w.out/'export-report.json').write_text(json.dumps({'sectors':100,'bytes':sum(row[8] for row in rows),'maxPalette':max(palettes),'palettes':palettes},indent=2),encoding='utf-8')


def validate(out):
    out=Path(out);rows=list(csv.DictReader((out/'schematics/importation.csv').open(encoding='utf-8')))
    assert len(rows)==100
    seen=set()
    for row in rows:
        p=out/'schematics'/row['fichier'];raw=p.read_bytes()
        assert hashlib.sha256(raw).hexdigest()==row['sha256']
        assert len(raw)==int(row['octets'])
        sc=nbt.load(p)['Schematic'];assert int(sc['Version'])==3 and int(sc['DataVersion'])==5023
        assert (int(sc['Width']),int(sc['Height']),int(sc['Length']))==(80,320,80)
        assert len(sc['Blocks']['Palette'])<=256
        key=(int(row['collage_x']),int(row['collage_z']));assert key not in seen;seen.add(key)
    print('Validation Python : 100 secteurs, sommes SHA-256, dimensions et palettes OK.',flush=True)
