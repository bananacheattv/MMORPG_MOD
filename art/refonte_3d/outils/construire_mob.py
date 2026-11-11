#!/usr/bin/env python3
"""Construit les mobs : python construire_mob.py <id ou prefixe...> ; sans argument, tous. Sorties dans livraison (arborescence du mod) ; rien n'est ecrit dans le depot."""
import json
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE / 'modeles'))
from aeloria3d import check_runtime, export_runtime
from materiaux import PEINTRES, RAMPS, post_process
import clips_extra

# le cristal d'arene et le drone n'attaquent pas : pas de clips supplementaires
SANS_EXTRA = {'crystal_blue', 'crystal_red', 'crystal_neutral', 'arena_drone'}

ROOT = HERE.parent
LIV = ROOT / 'livraison'
RES = LIV / 'src/main/resources/assets/mmorpg'


def registre():
    from mobs import TOUS
    return TOUS


def enveloppe(model):
    pts = []

    def w(ns):
        for n in ns:
            if 'children' in n:
                w(n['children'])
            else:
                pts.append(n['from'])
                pts.append(n['to'])
    w(model['parts'])
    return [min(p[i] for p in pts) for i in range(3)], [max(p[i] for p in pts) for i in range(3)]


def build(key, TOUS):
    t0 = time.time()
    p = TOUS[key]()
    if key not in SANS_EXTRA:
        clips_extra.ajoute(p)
    reste = p.resoudre_coplanaires()
    p.pack(min_size=64)
    p.paint(PEINTRES, RAMPS)
    post_process(p)
    s = p.meta.get('echelle', 1.0)
    p.reduire(s)
    bb = p.to_bbmodel()
    out = LIV / 'bbmodels'
    out.mkdir(parents=True, exist_ok=True)
    (out / f'{key}.bbmodel').write_text(json.dumps(bb, separators=(',', ':')), encoding='utf-8')
    model, png = export_runtime(bb, description=p.description)
    errs = check_runtime(model)
    (RES / f'bbmodels/{p.kind}').mkdir(parents=True, exist_ok=True)
    (RES / f'textures/imported/{p.kind}').mkdir(parents=True, exist_ok=True)
    (RES / f'bbmodels/{p.kind}/{key}.json').write_text(json.dumps(model, separators=(',', ':')), encoding='utf-8')
    (RES / f'textures/imported/{p.kind}/{key}.png').write_bytes(png)
    lo, hi = enveloppe(model)
    px = lambda v: round(v * s)
    print(f'{key}: {len(p.cubes)} cubes, {len(p.bones)} os, texture {p.res} ({p.filled:.0%}), {[a["name"] for a in p.anims]}, enveloppe jeu x[{px(lo[0])},{px(hi[0])}] y[{px(lo[1])},{px(hi[1])}] z[{px(lo[2])},{px(hi[2])}] px, {time.time() - t0:.1f}s')
    cop = p.coplanar_overlaps()
    if cop:
        print('  COPLANAIRES :', cop[:6])
    if errs:
        print('  ERREURS :', errs[:6])
    return p


if __name__ == '__main__':
    T = registre()
    a = [x for x in sys.argv[1:] if not x.startswith('--')]
    for k in T:
        if not a or k in a or any(k.startswith(x) for x in a if x.endswith('*')):
            build(k, T)
