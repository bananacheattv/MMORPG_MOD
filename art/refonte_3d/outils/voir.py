#!/usr/bin/env python3
"""Regarder UN modele de pres, rendu logiciel du .bbmodel reel.
python voir.py id                      : face, dos, profil en grand
python voir.py id clip [n]             : n images du clip (defaut 6), de face a gauche, de profil dessous
Ecrit apercus/travail/un_par_un/<id>[_clip].png"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from render import Model, render

ROOT = Path(__file__).resolve().parents[1]
FONT = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 13)
OUT = ROOT / 'apercus/travail/un_par_un'


def cadre(m, poses):
    """Centre et echelle communs a toutes les poses, pour que le mouvement se lise."""
    pts = []
    for clip, t in poses:
        pts.append(np.concatenate([c[0] for c in m.quads(clip, t)], 0))
    q = np.concatenate(pts, 0)
    return (q.min(0) + q.max(0)) / 2, float(np.linalg.norm(q.max(0) - q.min(0)))


def fixe(key, cw=520):
    m = Model(ROOT / 'livraison/bbmodels' / f'{key}.bbmodel')
    ctr, span = cadre(m, [(None, 0)])
    sc = cw * .92 / span
    vues = [(148, 14, 'face'), (-38, 18, 'dos'), (90, 3, 'profil')]
    S = Image.new('RGB', (cw * len(vues), cw), (232, 232, 236))
    for i, (yaw, pitch, lab) in enumerate(vues):
        im = render(m.quads(), m.tex, m.res, yaw=yaw, pitch=pitch, size=(cw, cw), scale=sc, center=ctr).convert('RGB')
        ImageDraw.Draw(im).text((5, 3), f'{key} : {lab}' + (f' ({len(m.el)} cubes, {m.res["width"]} px)' if i == 0 else ''), fill=(30, 26, 40), font=FONT)
        S.paste(im, (i * cw, 0))
    OUT.mkdir(parents=True, exist_ok=True)
    f = OUT / f'{key}.png'
    S.save(f)
    print(f)


def anim(key, clip, n=6, cw=300):
    m = Model(ROOT / 'livraison/bbmodels' / f'{key}.bbmodel')
    L = m.anim[clip]['length']
    fin = 1 if m.anim[clip].get('loop') == 'once' else 0
    ts = [L * i / (n - fin) for i in range(n)]
    ctr, span = cadre(m, [(clip, t) for t in ts])
    sc = cw * .92 / span
    S = Image.new('RGB', (cw * n, cw * 2), (232, 232, 236))
    for r, (yaw, pitch, lab) in enumerate(((148, 14, 'face'), (90, 3, 'profil'))):
        for i, t in enumerate(ts):
            im = render(m.quads(clip, t), m.tex, m.res, yaw=yaw, pitch=pitch, size=(cw, cw), scale=sc, center=ctr).convert('RGB')
            ImageDraw.Draw(im).text((5, 3), f'{key} {clip} {t:.2f}s {lab}', fill=(30, 26, 40), font=FONT)
            S.paste(im, (i * cw, r * cw))
    OUT.mkdir(parents=True, exist_ok=True)
    f = OUT / f'{key}_{clip}.png'
    S.save(f)
    print(f)


if __name__ == '__main__':
    a = sys.argv[1:]
    if len(a) == 1:
        fixe(a[0])
    else:
        anim(a[0], a[1], int(a[2]) if len(a) > 2 else 6)
