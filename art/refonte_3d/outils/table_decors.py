#!/usr/bin/env python3
"""Table des decors originaux : enveloppe en unites, nombre de cubes, clips, cellules de collision. Sert de cahier des charges a la refonte. Ecrit apercus/travail/table_decors.txt."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GIT = ROOT / 'originaux/git_main'
P = GIT / 'src/main/resources/assets/mmorpg/bbmodels/props'


def leafs(nodes, out):
    for n in nodes:
        if 'children' in n:
            leafs(n['children'], out)
        else:
            out.append(n)


def main():
    shapes = json.loads((GIT / 'decor_shapes.json').read_text(encoding='utf-8'))
    lignes = []
    for f in sorted(P.glob('*.json')):
        d = json.loads(f.read_text(encoding='utf-8'))
        c = []
        leafs(d['parts'], c)
        xs = [v for e in c for v in (e['from'][0], e['to'][0])]
        ys = [v for e in c for v in (e['from'][1], e['to'][1])]
        zs = [v for e in c for v in (e['from'][2], e['to'][2])]
        an = [(a['name'], round(float(a['length']), 2)) for a in d.get('animations', [])]
        bones = sorted({l.split('|')[0] for a in d.get('animations', []) for l in a['tracks'].splitlines() if l})
        sh = shapes.get(f.stem, '')
        cells = [x.split(':')[0] for x in sh.split(';') if x]
        lignes.append(f"{f.stem:32s} n={len(c):4d} x[{min(xs):7.1f},{max(xs):7.1f}] y[{min(ys):6.1f},{max(ys):6.1f}] z[{min(zs):7.1f},{max(zs):7.1f}] cellules={len(cells):3d} tex={d.get('texture', '')} anim={an} os={bones}")
    out = ROOT / 'apercus/travail/table_decors.txt'
    out.write_text('\n'.join(lignes) + '\n', encoding='utf-8')
    print(len(lignes), out)


if __name__ == '__main__':
    main()
