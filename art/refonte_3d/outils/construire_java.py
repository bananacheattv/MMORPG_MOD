#!/usr/bin/env python3
"""Blocs au format Java (models/block/<id>.json + textures/block/<id>.png) : caisses, Lucky Blocks, classements, accessoires.
Le modele est decrit comme les decors (kit_decor.D, coordonnees centrees sur x et z), peint par la meme chaine, puis ecrit en JSON Java :
x et z decales de +8, UV ramenes a 0..16, rotation sur un seul axe a +-22,5 ou +-45 degres, coordonnees dans -16..32, comme l'exige le format.
    python construire_java.py [prefixe ...]"""
import contextlib
import io
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE / 'modeles'))
from materiaux import PEINTRES, RAMPS, post_process

ROOT = HERE.parent
LIV = ROOT / 'livraison'
RES = LIV / 'src/main/resources/assets/mmorpg'
ORI = ROOT / 'originaux/git_main/src/main/resources/assets/mmorpg/models/block'


def rot_java(c):
    axes = [(a, v) for a, v in zip('xyz', c.rot) if abs(v) > 1e-6]
    if not axes:
        return None
    if len(axes) > 1 or abs(abs(axes[0][1]) - 22.5) > 1e-6 and abs(abs(axes[0][1]) - 45) > 1e-6:
        raise ValueError(f'{c.name} : rotation {c.rot} interdite en JSON Java (un seul axe, +-22,5 ou +-45)')
    a, v = axes[0]
    px, py, pz = c.pivot
    return {'origin': [px + 8, py, pz + 8], 'axis': a, 'angle': v}


def vers_json(p, tex_ref, base=None):
    res = p.res
    els = []
    for c in p.cubes:
        if not c.faces:
            continue
        fr = [float(c.frm[0] + 8), float(c.frm[1]), float(c.frm[2] + 8)]
        to = [float(c.to[0] + 8), float(c.to[1]), float(c.to[2] + 8)]
        if min(fr + to) < -16 or max(fr + to) > 32:
            raise ValueError(f'{p.name}/{c.name} : hors des limites -16..32 : {fr} {to}')
        e = {'name': c.name, 'from': fr, 'to': to}
        r = rot_java(c)
        if r:
            e['rotation'] = r
        if c.opts.get('sans_ombre'):
            e['shade'] = False
        e['faces'] = {f: {'uv': [round(v / res * 16, 4) for v in uv], 'texture': '#0'} for f, uv in c.faces.items()}
        els.append(e)
    d = {'credit': 'Aeloria : refonte (16 texels par bloc)', 'render_type': 'minecraft:cutout', 'textures': {'0': tex_ref, 'particle': tex_ref}, 'elements': els}
    if base and 'display' in base:
        d['display'] = base['display']
    return d


def construire(key, fn):
    from aeloria3d import Project
    p = fn()
    p.resoudre_coplanaires()
    p.pack(min_size=32)
    p.paint(PEINTRES, RAMPS)
    post_process(p)
    bb = p.to_bbmodel()
    (LIV / 'bbmodels').mkdir(parents=True, exist_ok=True)
    (LIV / 'bbmodels' / f'{key}.bbmodel').write_text(json.dumps(bb, separators=(',', ':')), encoding='utf-8')
    (RES / 'textures/block').mkdir(parents=True, exist_ok=True)
    (RES / 'models/block').mkdir(parents=True, exist_ok=True)
    (RES / f'textures/block/{key}.png').write_bytes(p.png_bytes())
    of = ORI / f'{key}.json'
    base = json.loads(of.read_text(encoding='utf-8')) if of.exists() else None
    j = vers_json(p, f'mmorpg:block/{key}', base)
    (RES / f'models/block/{key}.json').write_text(json.dumps(j, ensure_ascii=False, separators=(',', ':')), encoding='utf-8')
    cop = p.coplanar_overlaps()
    return p, cop


def main(args):
    from blocs_java import BLOCS
    pref = args or ['']
    ok = 0
    cles = [k for k in BLOCS if any(k.startswith(x) for x in pref)]
    for k in cles:
        try:
            p, cop = construire(k, BLOCS[k])
            ok += 1
            print(f'{k:34s} {len(p.cubes):3d} cubes tex {p.res:4d}{"  COPLANAIRES " + str(cop[:6]) if cop else ""}')
        except Exception as e:
            print('ERREUR', k, repr(e))
    print(ok, 'sur', len(cles))


if __name__ == '__main__':
    main(sys.argv[1:])
