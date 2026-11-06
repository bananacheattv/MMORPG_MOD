#!/usr/bin/env python3
"""Vitrine pour capture Blockbench (jamais livree) : plusieurs .bbmodel livres, reduits et ranges sur une grille, dans un seul fichier avec leurs vraies textures.
Memes cubes, memes UV, memes PNG que les fichiers livres. Blockbench capture alors toute une famille d'un coup.
    python vitrine.py sortie.bbmodel echelle colonnes id1 id2 ...
"""
import json
import sys
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LIV = ROOT / 'livraison/bbmodels'


def charger(key):
    for base in (LIV, ROOT / 'apercus/avant/bbmodel'):
        f = base / f'{key}.bbmodel'
        if f.exists():
            return json.loads(f.read_text(encoding='utf-8'))
    raise SystemExit(f'{key} introuvable')


def main(out, s, cols, ids):
    res = 512
    docs = [charger(k) for k in ids]
    tailles = []
    for d in docs:
        xs = [v for e in d['elements'] for v in (e['from'][0], e['to'][0])]
        zs = [v for e in d['elements'] for v in (e['from'][2], e['to'][2])]
        tailles.append((max(xs) - min(xs), max(zs) - min(zs), (max(xs) + min(xs)) / 2, (max(zs) + min(zs)) / 2))
    cw = max(t[0] for t in tailles) * s + 4
    cz = max(t[1] for t in tailles) * s + 4
    n = len(ids)
    rows = (n + cols - 1) // cols if cols else 1
    places = None
    if cols == 0:
        # rangement automatique : rangees de largeur limitee, chaque modele garde sa propre largeur
        LIM, GAP = 84.0, 4.0
        rangs, cur, lx, lz = [], [], 0.0, 0.0
        for i, t in enumerate(tailles):
            w, dp = t[0] * s, t[1] * s
            if cur and lx + w > LIM:
                rangs.append((cur, lx - GAP, lz))
                cur, lx, lz = [], 0.0, 0.0
            cur.append((i, lx + w / 2))
            lx += w + GAP
            lz = max(lz, dp)
        rangs.append((cur, lx - GAP, lz))
        total_z = sum(r[2] for r in rangs) + GAP * (len(rangs) - 1)
        places = {}
        z = -total_z / 2
        for items, largeur, prof in rangs:
            for i, xc in items:
                places[i] = (xc - largeur / 2, z + prof / 2)
            z += prof + GAP
    base = dict(docs[0])
    base['name'] = Path(out).stem
    base['resolution'] = {'width': res, 'height': res}
    base['animations'] = []
    base['elements'], base['textures'], base['groups'], base['outliner'] = [], [], [], []
    for i, (key, d, t) in enumerate(zip(ids, docs, tailles)):
        if places:
            ox = places[i][0] - t[2] * s
            oz = places[i][1] - t[3] * s
        else:
            c, r = i % cols, i // cols
            ox = (c - (cols - 1) / 2) * cw - t[2] * s
            oz = (r - (rows - 1) / 2) * cz - t[3] * s
        ku = res / d['resolution']['width']
        kv = res / d['resolution']['height']
        ti = len(base['textures'])
        tex_ids = {}
        for j, tx in enumerate(d['textures']):
            tx = dict(tx)
            tx['uuid'] = str(uuid.uuid4())
            tx['id'] = str(ti + j)
            tx['name'] = f'{key}_{j}.png'
            tx['path'] = ''
            tx['relative_path'] = ''
            tx['saved'] = False
            tx['uv_width'], tx['uv_height'] = res, res
            base['textures'].append(tx)
            tex_ids[j] = ti + j
        group = {'name': key, 'origin': [ox, 0, oz], 'color': 0, 'uuid': str(uuid.uuid4()), 'export': True, 'mirror_uv': False, 'isOpen': False, 'locked': False, 'visibility': True, 'autouv': 0, 'children': []}
        for e in d['elements']:
            e = dict(e)
            e['uuid'] = str(uuid.uuid4())
            tr = lambda v: [v[0] * s + ox, v[1] * s, v[2] * s + oz]
            e['from'], e['to'] = tr(e['from']), tr(e['to'])
            if 'origin' in e:
                e['origin'] = tr(e['origin'])
            faces = {}
            for fn, f in e['faces'].items():
                f = dict(f)
                if f.get('texture') is not None:
                    f['texture'] = tex_ids.get(f['texture'], ti)
                u = f.get('uv')
                if u:
                    f['uv'] = [u[0] * ku, u[1] * kv, u[2] * ku, u[3] * kv]
                faces[fn] = f
            e['faces'] = faces
            base['elements'].append(e)
            group['children'].append(e['uuid'])
        base['groups'].append({k: v for k, v in group.items() if k != 'children'})
        base['outliner'].append({'uuid': group['uuid'], 'isOpen': False, 'children': group['children']})
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    Path(out).write_text(json.dumps(base, separators=(',', ':')), encoding='utf-8')
    print(out, len(base['elements']), 'cubes', len(base['textures']), 'textures', 'cellule', round(cw, 1), round(cz, 1), 'total', round(cw * cols, 1), round(cz * rows, 1))


if __name__ == '__main__':
    a = sys.argv[1:]
    main(a[0], float(a[1]), int(a[2]), a[3:])
