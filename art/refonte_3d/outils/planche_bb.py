#!/usr/bin/env python3
"""Planche de contact pour capture Blockbench (jamais livree) : chaque modele livre, reduit et tourne en 3/4 dans son propre groupe, range sur une grille verticale face a la camera.
Memes cubes, memes UV, memes PNG que les fichiers livres. Aucun modele ne cache son voisin.
    python planche_bb.py sortie.bbmodel echelle id1 id2 ...
"""
import base64
import io
import json
import math
import sys
import uuid
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
LIV = ROOT / 'livraison/bbmodels'
LARGE, HAUT = 90.0, 66.0
YAW, PITCH = 35.0, -22.0


def charger(key):
    return json.loads((LIV / f'{key}.bbmodel').read_text(encoding='utf-8'))


def ranger(docs, s, egal=False):
    """egal : chaque modele occupe la meme hauteur a l'ecran, quelle que soit sa taille reelle (planches de mobs, tailles tres differentes)."""
    cy, sy = math.cos(math.radians(YAW)), math.sin(math.radians(YAW))
    cp, sp = math.cos(math.radians(PITCH)), math.sin(math.radians(PITCH))
    infos = []
    for d in docs:
        lo = [min(min(e['from'][i], e['to'][i]) for e in d['elements']) for i in range(3)]
        hi = [max(max(e['from'][i], e['to'][i]) for e in d['elements']) for i in range(3)]
        w, h, p = hi[0] - lo[0], hi[1] - lo[1], hi[2] - lo[2]
        rw0 = w * cy + p * sy
        rh0 = h * cp + (w * sy + p * cy) * abs(sp)
        k = s * (30.0 / max(rw0, rh0)) if egal else s
        rw, rh = rw0 * k, rh0 * k
        infos.append(((lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2, (lo[2] + hi[2]) / 2, rw, rh, k))
    gap = 2.0
    rangs, cur, lx, lh = [], [], 0.0, 0.0
    for i, t in enumerate(infos):
        if cur and lx + t[3] > LARGE:
            rangs.append((cur, lx - gap, lh))
            cur, lx, lh = [], 0.0, 0.0
        cur.append((i, lx + t[3] / 2))
        lx += t[3] + gap
        lh = max(lh, t[4])
    rangs.append((cur, lx - gap, lh))
    total = sum(r[2] for r in rangs) + gap * (len(rangs) - 1)
    return infos, rangs, total, gap


def main(out, s, ids, egal=False):
    res = 512
    docs = [charger(k) for k in ids]
    while True:
        infos, rangs, total, gap = ranger(docs, s, egal)
        if total <= HAUT or s < .05:
            break
        s *= .97
    places = {}
    y = total / 2 + 3                                 # la camera vise un peu au-dessus de l'origine
    for items, largeur, h in rangs:
        for i, xc in items:
            places[i] = (xc - largeur / 2, y - h / 2)
        y -= h + gap
    base = dict(docs[0])
    base['name'] = Path(out).stem
    base['resolution'] = {'width': res, 'height': res}
    base['animations'] = []
    base['elements'], base['textures'], base['groups'], base['outliner'] = [], [], [], []
    base['meta'] = {'format_version': '5.0', 'model_format': 'free', 'box_uv': False}
    for i, (key, d, t) in enumerate(zip(ids, docs, infos)):
        xc, yc = places[i]
        # une texture plus haute que large serait lue comme une animation par Blockbench : sur la planche seulement, on la complete en carre
        S_ = max(d['resolution']['width'], d['resolution']['height'])
        ku, kv = res / S_, res / S_
        ti = len(base['textures'])
        tex = {}
        for j, tx in enumerate(d['textures']):
            tx = dict(tx)
            if d['resolution']['width'] != d['resolution']['height']:
                im = Image.open(io.BytesIO(base64.b64decode(tx['source'].split(',', 1)[1]))).convert('RGBA')
                ca = Image.new('RGBA', (S_, S_), (0, 0, 0, 0))
                ca.paste(im, (0, 0))
                buf = io.BytesIO()
                ca.save(buf, 'PNG')
                tx['source'] = 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()
                tx['width'], tx['height'] = S_, S_
            tx['uuid'] = str(uuid.uuid4())
            tx['id'] = str(ti + j)
            tx['name'] = f'{key}_{j}.png'
            tx['path'] = ''
            tx['relative_path'] = ''
            tx['saved'] = False
            tx['uv_width'], tx['uv_height'] = res, res
            base['textures'].append(tx)
            tex[j] = ti + j
        kk = t[5]
        tr = lambda v, kk=kk, t=t, xc=xc, yc=yc: [(v[0] - t[0]) * kk + xc, (v[1] - t[1]) * kk + yc, (v[2] - t[2]) * kk]
        def groupe(nom, rotation):
            return {'name': nom, 'origin': [xc, yc, 0], 'rotation': rotation, 'color': 0, 'uuid': str(uuid.uuid4()), 'export': True, 'mirror_uv': False, 'isOpen': False, 'locked': False, 'visibility': True, 'autouv': 0, 'reset': False, 'shade': True, 'selected': False, 'primary_selected': False, 'scope': 0}
        g = groupe(key + '_lacet', [0, YAW, 0])
        g0 = groupe(key, [PITCH, 0, 0])
        els = {e['uuid']: e for e in d['elements']}
        grp = {x['uuid']: x for x in d.get('groups', [])}

        def conv(n):
            # la hierarchie est gardee : les os tournes au repos (corps en chaine, armes) restent tournes
            if isinstance(n, str):
                e = dict(els[n])
                e['uuid'] = str(uuid.uuid4())
                e['box_uv'] = False
                e.pop('uv_offset', None)
                e.pop('mirror_uv', None)
                e['from'], e['to'] = tr(e['from']), tr(e['to'])
                if 'origin' in e:
                    e['origin'] = tr(e['origin'])
                faces = {}
                for fn, f in e['faces'].items():
                    f = dict(f)
                    if f.get('texture') is not None:
                        f['texture'] = tex.get(f['texture'], ti)
                    u = f.get('uv')
                    if u:
                        f['uv'] = [u[0] * ku, u[1] * kv, u[2] * ku, u[3] * kv]
                    faces[fn] = f
                e['faces'] = faces
                base['elements'].append(e)
                return e['uuid']
            o = grp.get(n['uuid'], n)
            h = dict(o)
            h['uuid'] = str(uuid.uuid4())
            h['name'] = key + '_' + str(o.get('name', 'os'))
            h['origin'] = tr(o.get('origin', [0, 0, 0]))
            h['rotation'] = list(o.get('rotation', [0, 0, 0]))
            h.pop('children', None)
            base['groups'].append(h)
            return {'uuid': h['uuid'], 'isOpen': False, 'children': [conv(c) for c in n.get('children', [])]}
        enfants = [conv(n) for n in d['outliner']]
        base['groups'] += [g0, g]
        base['outliner'].append({'uuid': g0['uuid'], 'isOpen': False, 'children': [{'uuid': g['uuid'], 'isOpen': False, 'children': enfants}]})
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    Path(out).write_text(json.dumps(base, separators=(',', ':')), encoding='utf-8')
    print(Path(out).name, len(base['elements']), 'cubes', len(ids), 'modeles', len(rangs), 'rangs, hauteur', round(total, 1), 'echelle', round(s, 3))


if __name__ == '__main__':
    a = sys.argv[1:]
    e = '--egal' in a
    a = [x for x in a if x != '--egal']
    main(a[0], float(a[1]), a[2:], e)
