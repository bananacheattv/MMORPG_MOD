#!/usr/bin/env python3
"""GIF de controle d'un modele repris un par un : marche, attaque, sort, mort, repos, rendu logiciel du .bbmodel livre (les lueurs ne brillent pas ici).
python gif_mob.py id [id ...] ; ecrit apercus/apres/un_par_un/<id>.gif"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from render import Model, render

ROOT = Path(__file__).resolve().parents[1]
FONT = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 15)
FPS = 14
NOMS = {'idle': 'repos', 'walk': 'marche', 'fly': 'vol', 'slither': 'reptation', 'attack': 'attaque', 'cast': 'sort', 'death': 'mort'}
ORDRE = (('walk', 2), ('fly', 2), ('slither', 2), ('attack', 1), ('cast', 1), ('death', 1), ('idle', 1))


def gif(key, size=(340, 300)):
    m = Model(ROOT / 'livraison/bbmodels' / f'{key}.bbmodel')
    seq = [(c, r) for c, r in ORDRE if c in m.anim]
    pts = []
    for c, _ in seq:
        L = m.anim[c]['length']
        for f in (0, .2, .4, .6, .8, 1):
            pts.append(np.concatenate([q[0] for q in m.quads(c, L * f)], 0))
    q = np.concatenate(pts, 0)
    # cadre : la pose de repos compte le plus, on tolere un depassement pour les grands gestes
    ctr = (q.min(0) + q.max(0)) / 2
    span = float(np.linalg.norm(q.max(0) - q.min(0)))
    sc = min(size) * 1.1 / span
    frames = []
    for c, reps in seq:
        L = m.anim[c]['length']
        n = max(6, int(round(L * FPS)))
        fin = m.anim[c].get('loop') == 'once'
        images = []
        for i in range(n + (1 if fin else 0)):
            t = L * i / n
            im = render(m.quads(c, t), m.tex, m.res, yaw=148, pitch=14, size=size, scale=sc, center=ctr, bg=(226, 226, 232, 255)).convert('RGB')
            ImageDraw.Draw(im).text((8, 5), NOMS.get(c, c), fill=(40, 36, 50), font=FONT)
            images.append(im)
        if fin:
            images += images[-1:] * (FPS // 2)
        frames += images * reps
    pal = frames[len(frames) // 3].convert('P', palette=Image.ADAPTIVE, colors=96)
    small = [f.quantize(palette=pal, dither=Image.NONE) for f in frames]
    out = ROOT / 'apercus/apres/un_par_un' / f'{key}.gif'
    out.parent.mkdir(parents=True, exist_ok=True)
    small[0].save(out, save_all=True, append_images=small[1:], duration=int(1000 / FPS), loop=0, optimize=True)
    print(out.name, len(frames), 'images', out.stat().st_size // 1024, 'Ko')


if __name__ == '__main__':
    for k in sys.argv[1:]:
        gif(k)
