#!/usr/bin/env python3
"""Releve des mobs originaux (bbmodels/mobs, classic) : enveloppe, os nommes, clips, cubes emissifs, texture, echelle en jeu. Ecrit apercus/travail/table_mobs.txt et les .bbmodel AVANT."""
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from runtime_to_bbmodel import convert

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / 'originaux/git_main/src/main/resources/assets/mmorpg'


def walk(nodes, cubes, bones):
    for n in nodes:
        if 'children' in n:
            bones.append(n.get('name') or n.get('uuid'))
            walk(n['children'], cubes, bones)
        else:
            cubes.append(n)


def main():
    scales = {}
    try:
        src = Path(r'C:\Users\Utilisateur\Downloads\serv\MMORPG_MOD\src\main\java\com\mmorpg\entity\mob\ImportedMobs.java').read_text(encoding='utf-8')
        for m in re.finditer(r'new Entry\("(\w+)"[^)]*\)', src):
            scales[m.group(1)] = m.group(0)
    except OSError:
        pass
    L = []
    for kind in ('mobs', 'classic'):
        for f in sorted((A / f'bbmodels/{kind}').glob('*.json')):
            d = json.loads(f.read_text(encoding='utf-8'))
            c, b = [], []
            walk(d['parts'], c, b)
            xs = [v for e in c if 'from' in e for v in (e['from'][0], e['to'][0])]
            ys = [v for e in c if 'from' in e for v in (e['from'][1], e['to'][1])]
            zs = [v for e in c if 'from' in e for v in (e['from'][2], e['to'][2])]
            an = {a['name']: round(float(a['length']), 2) for a in d['animations']}
            pistes = sorted({l.split('|')[0] for a in d['animations'] for l in a['tracks'].splitlines() if l})
            noms = {n.get('uuid'): n.get('name') for n in []}
            em = sum(1 for e in c if e.get('emissive'))
            mesh = sum(1 for e in c if 'vertices' in e)
            sc = scales.get(f.stem, '')
            L.append(f"{kind[:3]} {f.stem:24s} n={len(c):3d} mesh={mesh:3d} em={em:2d} x[{min(xs):6.1f},{max(xs):6.1f}] y[{min(ys):6.1f},{max(ys):6.1f}] z[{min(zs):6.1f},{max(zs):6.1f}] tex={d['resolution']['width']}x{d['resolution']['height']} {an} os={b[:26]} {sc}")
            png = A / f'textures/imported/{kind}/{f.stem}.png'
            out = ROOT / f'apercus/avant/bbmodel_mobs/{f.stem}.bbmodel'
            out.parent.mkdir(parents=True, exist_ok=True)
            if png.exists():
                try:
                    convert(str(f), str(png), str(out))
                except Exception as ex:
                    L.append(f'   conversion impossible : {ex!r}')
    o = ROOT / 'apercus/travail/table_mobs.txt'
    o.write_text('\n'.join(L) + '\n', encoding='utf-8')
    print(o, len(L))


if __name__ == '__main__':
    main()
