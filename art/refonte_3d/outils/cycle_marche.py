#!/usr/bin/env python3
"""Controle d'une marche : N images du cycle en vue de dessus et de profil, rendu du modele reel. python cycle_marche.py sortie.png id [clip] [n]"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import numpy as np
sys.path.insert(0, str(Path(__file__).parent))
from render import Model, render
ROOT = Path(__file__).resolve().parents[1]
FONT = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 13)


def planche(out, key, clip='walk', n=8, cw=240, vues=((180, 62, 'dessus'), (90, 8, 'profil'), (150, 18, 'face'))):
    m = Model(ROOT / 'livraison/bbmodels' / f'{key}.bbmodel')
    L = m.anim[clip]['length']
    q0 = np.concatenate([c[0] for c in m.quads()], 0)
    ctr = (q0.min(0) + q0.max(0)) / 2
    span = float(np.linalg.norm(q0.max(0) - q0.min(0)))
    sc = cw * .95 / span
    sheet = Image.new('RGB', (cw * n, cw * len(vues)), (232, 232, 236))
    for r, (yaw, pitch, lab) in enumerate(vues):
        for i in range(n):
            t = L * i / n
            im = render(m.quads(clip, t), m.tex, m.res, yaw=yaw, pitch=pitch, size=(cw, cw), scale=sc, center=ctr).convert('RGB')
            ImageDraw.Draw(im).text((4, 2), f'{key} {clip} {lab} {i + 1}/{n}', fill=(30, 26, 40), font=FONT)
            sheet.paste(im, (i * cw, r * cw))
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print(out, sheet.size)


if __name__ == '__main__':
    a = sys.argv[1:]
    planche(a[0], a[1], a[2] if len(a) > 2 else 'walk', int(a[3]) if len(a) > 3 else 8)
