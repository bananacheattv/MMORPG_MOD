#!/usr/bin/env python3
"""Copie de CONTROLE d'un .bbmodel avec un mannequin de joueur assis (jamais livre) : position calculee comme le jeu.

Le jeu pose le joueur a seat*2 + 0,1 bloc moins 0,6 (attache vehicule d'Avatar) ; la monture est rendue x2, donc en unites modele :
dessous des cuisses = 16*seat + 1, hanche = 16*seat + 2. Pose assise vanilla (HumanoidModel) : cuisses a 81 degres, ecartees de 18 degres, bras a 36 degres.
    python avec_cavalier.py entree.bbmodel sortie.bbmodel seat [x z]
"""
import base64
import io
import json
import sys
import uuid as uuidlib
from pathlib import Path

import numpy as np
from PIL import Image

NS = uuidlib.UUID('d1f5d0a8-9f3e-49f4-8d1d-1a2b3c4d5e6f')


def add_rider(src, dst, seat, seats=((0.0, 0.0),)):
    d = json.loads(Path(src).read_text(encoding='utf-8'))
    tex = d['textures'][0]
    img = np.array(Image.open(io.BytesIO(base64.b64decode(tex['source'].split(',', 1)[1]))).convert('RGBA'))
    H, W = img.shape[:2]
    resw = d['resolution']['width']
    k = W / resw
    # quatre aplats dans un coin vide de la texture (copie de controle uniquement)
    cols = {'peau': (214, 170, 135), 'chemise': (58, 120, 172), 'pantalon': (50, 56, 110), 'cheveux': (66, 42, 26)}
    uv = {}
    s = max(2, int(2 * k))
    for i, (n, c) in enumerate(cols.items()):
        x0 = W - s * (i + 1) - 1
        y0 = H - s - 1
        img[y0:y0 + s, x0:x0 + s, :3] = c
        img[y0:y0 + s, x0:x0 + s, 3] = 255
        uv[n] = [x0 / k, y0 / k, (x0 + s) / k, (y0 + s) / k]
    buf = io.BytesIO()
    Image.fromarray(img, 'RGBA').save(buf, 'PNG')
    tex['source'] = 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()

    def cube(name, a, b, col, rot=None, pivot=None):
        u = str(uuidlib.uuid5(NS, name))
        e = {'name': name, 'box_uv': False, 'render_order': 'default', 'locked': False, 'export': False, 'scope': 0, 'allow_mirror_modeling': False, 'from': list(a), 'to': list(b), 'autouv': 0, 'color': 4,
             'origin': list(pivot if pivot is not None else ((np.array(a) + np.array(b)) / 2).tolist()), 'faces': {f: {'uv': uv[col], 'texture': 0} for f in ('north', 'east', 'south', 'west', 'up', 'down')},
             'type': 'cube', 'uuid': u}
        if rot:
            e['rotation'] = list(rot)
        d['elements'].append(e)
        return u

    kids = []
    for i, (sx, sz) in enumerate(seats):
        hip = np.array([sx, 16 * seat + 2, sz])
        px = .5
        t = f'c{i}_'
        def box(n, a, b, col, rot=None, piv=None):
            A = hip + np.array(a) * px
            B = hip + np.array(b) * px
            P = hip + np.array(piv) * px if piv is not None else None
            kids.append(cube(t + n, A.tolist(), B.tolist(), col, rot, P.tolist() if P is not None else None))
        box('torse', (-4, 0, -2), (4, 12, 2), 'chemise')
        box('tete', (-4, 12, -4), (4, 20, 4), 'peau')
        box('cheveux', (-4.3, 17, -4.3), (4.3, 20.3, 4.3), 'cheveux')
        box('bras_p', (4, -2, -2), (8, 10, 2), 'chemise', (36, 0, 0), (6, 10, 0))
        box('bras_m', (-8, -2, -2), (-4, 10, 2), 'chemise', (36, 0, 0), (-6, 10, 0))
        box('jambe_p', (-.1, -12, -2), (3.9, 0, 2), 'pantalon', (81, -18, 0), (1.9, 0, 0))
        box('jambe_m', (-3.9, -12, -2), (.1, 0, 2), 'pantalon', (81, 18, 0), (-1.9, 0, 0))
    gid = str(uuidlib.uuid5(NS, 'groupe'))
    if 'groups' not in d:
        d['groups'] = []
    d['groups'].append({'name': 'cavalier_controle', 'uuid': gid, 'export': False, 'locked': False, 'scope': 0, 'selected': False, 'visibility': True, 'origin': [0, 0, 0], 'rotation': [0, 0, 0],
                        'color': 4, 'children': [], 'reset': False, 'shade': True, 'mirror_uv': False, 'autouv': 0, 'isOpen': False, 'primary_selected': False})
    d['outliner'].append({'uuid': gid, 'isOpen': False, 'children': kids})
    d['name'] = Path(dst).stem
    Path(dst).parent.mkdir(parents=True, exist_ok=True)
    Path(dst).write_text(json.dumps(d, separators=(',', ':')), encoding='utf-8')


if __name__ == '__main__':
    a = sys.argv
    add_rider(a[1], a[2], float(a[3]), ((float(a[4]), float(a[5])),) if len(a) > 5 else ((0.0, 0.0),))
