"""Apercu logiciel d'un cosmetique 3D (JSON + texture) : vues face / 3-4 / dos / profil -> PNG.
Usage : python3 tools/preview_cosmetic.py cle [cle...] [-o sortie.png]
Un mannequin gris (tete 8, torse 8x12x4) situe le point d'attache selon la categorie (coiffe/visage/dos/epaules).
"""
import json
import math
import pathlib
import sys

import numpy as np
from PIL import Image, ImageDraw

ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/mmorpg'


def rot_matrix(r):
    x, y, z = map(math.radians, r)
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz @ ry @ rx


FACES = {
    'north': ([(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)], (0, 0, -1)),
    'south': ([(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)], (0, 0, 1)),
    'west': ([(0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)], (-1, 0, 0)),
    'east': ([(1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0)], (1, 0, 0)),
    'up': ([(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)], (0, 1, 0)),
    'down': ([(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)], (0, -1, 0)),
}


def collect(nodes, tex, M=None, off=None, out=None, parent_origin=None):
    out = [] if out is None else out
    M = np.eye(3) if M is None else M
    off = np.zeros(3) if off is None else off
    for n in nodes:
        o = np.array(n.get('origin', [0, 0, 0]), float)
        r = n.get('rotation', [0, 0, 0])
        R = rot_matrix(r)
        # point p (absolute, bb) -> parent_M @ (R @ (p-o) + o) + parent_off
        M2 = M @ R
        off2 = M @ (o - R @ o) + off
        if 'from' in n:
            fr, to = np.array(n['from'], float), np.array(n['to'], float)
            for name, (corners, normal) in FACES.items():
                f = n['faces'].get(name)
                if not f:
                    continue
                u = f['uv']
                px = tex.getpixel((int(u[0] + .5), int(u[1] + .5)))
                pts = [M2 @ (fr + np.array(c) * (to - fr)) + off2 for c in corners]
                nm = M2 @ np.array(normal)
                out.append((pts, nm, px))
        else:
            collect(n.get('children', []), tex, M2, off2, out)
        if 'children' in n and 'from' not in n:
            pass
    return out


def render(key, cat, view, size=420):
    d = json.loads((RES / f'bbmodels/cosmetics/{key}.json').read_text())
    tex = Image.open(RES / f'textures/imported/cosmetics/{key}.png').convert('RGBA')
    polys = collect(d['parts'], tex)
    # mannequin : coordonnees bb du point d'attache
    dummy = []
    def box(a, b, col):
        a, b = np.array(a, float), np.array(b, float)
        for name, (corners, normal) in FACES.items():
            dummy.append(([a + np.array(c) * (b - a) for c in corners], np.array(normal), col))
    # montage dans le repere du modele
    if cat == 'coiffe':
        mount = np.array([0, 0, 0]); head = (-4, -8, -4, 4, 0, 4)
        box((-4, -8, -4), (4, 0, 4), (200, 160, 130, 255)); box((-4, -20, -2), (4, -8, 2), (90, 90, 110, 255))
    elif cat == 'visage':
        box((-4, -4 - 0.64, -4 - .64), (4, 4, 0.64 - .64 + 3.36 + 0.64), (200, 160, 130, 255)); box((-4, -16, 3), (4, -4, 7), (90, 90, 110, 255))
        box((-4, -4, 0.7), (4, 4, 4.7), (200, 160, 130, 255))
        dummy.clear(); box((-4, -4, .64), (4, 4, 8.64), (200, 160, 130, 255)); box((-4, -16, 4.64), (4, -4, 8.64), (90, 90, 110, 255))
    elif cat == 'epaules':
        box((-4, -12, -2), (4, 0, 2), (90, 90, 110, 255)); box((4, -12, -2), (8, 0, 2), (200, 160, 130, 255)); box((-8, -12, -2), (-4, 0, 2), (200, 160, 130, 255))
        box((-4, 0, -2), (4, 8, 2), (200, 160, 130, 255))
    else:  # dos : origine = surface du dos, 4 px sous la nuque
        box((-4, -8, -4), (4, 4, 0), (90, 90, 110, 255)); box((4, -8, -4), (8, 4, 0), (200, 160, 130, 255)); box((-8, -8, -4), (-4, 4, 0), (200, 160, 130, 255))
        box((-4, 4, -6), (4, 12, 2), (200, 160, 130, 255)); box((-4, -20, -4), (4, -8, 0), (60, 60, 80, 255))
    yaw = {'face': 0, 'dos': 180, 'tq': 30, 'profil': 90}[view]
    a = math.radians(yaw)
    ry = np.array([[math.cos(a), 0, math.sin(a)], [0, 1, 0], [-math.sin(a), 0, math.cos(a)]])
    tilt = math.radians(-12)
    rxm = np.array([[1, 0, 0], [0, math.cos(tilt), -math.sin(tilt)], [0, math.sin(tilt), math.cos(tilt)]])
    V = rxm @ ry
    items = []
    for pts, nm, px in dummy + polys:
        p3 = [V @ p for p in pts]
        n3 = V @ nm
        items.append((sum(p[2] for p in p3) / 4, p3, n3, px))
    items.sort(key=lambda t: -t[0])  # camera regarde vers +z apres rotation : loin = z grand -> peint d'abord
    allp = [p for it in items for p in it[1]]
    xs = [p[0] for p in allp]; ys = [p[1] for p in allp]
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    span = max(max(xs) - min(xs), max(ys) - min(ys)) * 1.1
    s = size / span
    S = 3
    im = Image.new('RGBA', (size * S, size * S), (38, 32, 56, 255))
    dr = ImageDraw.Draw(im, 'RGBA')
    for z, p3, n3, px in items:
        if px[3] < 8:
            continue
        light = .62 + .38 * max(0, -n3[2] * .8 + n3[1] * .5 + 0.15)
        col = tuple(int(min(255, c * light)) for c in px[:3]) + (px[3],)
        poly = [((p[0] - cx) * s * S + size * S / 2, size * S / 2 - (p[1] - cy) * s * S) for p in p3]
        dr.polygon(poly, fill=col)
    return im.resize((size, size), Image.LANCZOS)


if __name__ == '__main__':
    args = sys.argv[1:]
    out = 'preview.png'
    if '-o' in args:
        i = args.index('-o'); out = args[i + 1]; del args[i:i + 2]
    cats = json.loads((pathlib.Path(__file__).parent / 'cosm_cats.json').read_text()) if (pathlib.Path(__file__).parent / 'cosm_cats.json').exists() else {}
    rows = []
    for k in args:
        cat = cats.get(k, 'dos')
        views = ['face', 'tq', 'dos', 'profil']
        row = Image.new('RGBA', (420 * 4, 420))
        for i, v in enumerate(views):
            row.paste(render(k, cat, v), (420 * i, 0))
        rows.append(row)
    sheet = Image.new('RGBA', (420 * 4, 420 * len(rows)))
    for i, r in enumerate(rows):
        sheet.paste(r, (0, 420 * i))
    sheet.save(out)
    print(out)
