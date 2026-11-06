#!/usr/bin/env python3
"""Apercus de controle d'un .bbmodel de livraison : vues fixes, cavalier mannequin, poses d'animation. Tout vient du fichier reel."""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from render import Model, mannequin, render

ROOT = Path(__file__).resolve().parents[1]
FONT = ImageFont.load_default()


def label(im, text):
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, im.width, 14), fill=(30, 30, 36, 255))
    d.text((4, 2), text, fill=(255, 255, 255, 255), font=FONT)
    return im


def fiche(path, out, seat=None, size=(520, 400), yaw_list=(-35, 35, 145, 215), seats=None):
    """seat : hauteur du dessous des cuisses en unites modele ; seats : [(x, z), ...] en unites modele."""
    m = Model(path)
    q = m.quads()
    # meme echelle pour toutes les vues
    pts = np.concatenate([c[0] for c in q], 0)
    span = np.linalg.norm(pts.max(0) - pts.min(0))
    extra = []
    if seat is not None:
        for (x, z) in (seats or [(0, 0)]):
            extra += mannequin(x, seat, z)
    cells = []
    sc = min(size) * 1.65 / span
    ctr = (pts.min(0) + pts.max(0)) / 2
    for yaw in yaw_list:
        cells.append(label(render(q, m.tex, m.res, yaw=yaw, pitch=22, size=size, scale=sc, center=ctr), f'3/4 yaw {yaw}'))
    for yaw, name in ((90, 'profil'), (0, 'face'), (180, 'dos')):
        cells.append(label(render(q, m.tex, m.res, yaw=yaw, pitch=0, size=size, scale=sc, center=ctr), name))
    if seat is not None:
        cells.append(label(render(q, m.tex, m.res, yaw=62, pitch=10, size=size, scale=sc, center=ctr, extra=extra), 'cavalier mannequin'))
    else:
        cells.append(label(render(q, m.tex, m.res, yaw=-35, pitch=60, size=size, scale=sc, center=ctr), 'dessus'))
    W = 4
    H = (len(cells) + W - 1) // W
    S = Image.new('RGB', (W * size[0], H * size[1]), (40, 40, 46))
    for i, c in enumerate(cells):
        S.paste(c.convert('RGB'), ((i % W) * size[0], (i // W) * size[1]))
    S.save(out)
    return S.size


def poses(path, out, clips=('idle', 'walk', 'run', 'jump'), n=6, size=(300, 240), yaw=70, pitch=10):
    m = Model(path)
    q0 = m.quads()
    pts = np.concatenate([c[0] for c in q0], 0)
    span = np.linalg.norm(pts.max(0) - pts.min(0))
    sc = min(size) * .92 / span * 1.3
    ctr = (pts.min(0) + pts.max(0)) / 2
    rows = []
    for clip in clips:
        if clip not in m.anim:
            continue
        L = m.anim[clip]['length']
        rows.append((clip, [(t, m.quads(clip, t)) for t in [L * i / n for i in range(n)]]))
    S = Image.new('RGB', (n * size[0], len(rows) * size[1]), (40, 40, 46))
    for r, (clip, frames) in enumerate(rows):
        for i, (t, q) in enumerate(frames):
            im = label(render(q, m.tex, m.res, yaw=yaw, pitch=pitch, size=size, scale=sc, center=ctr), f'{clip} t={t:.2f}s')
            S.paste(im.convert('RGB'), (i * size[0], r * size[1]))
    S.save(out)
    return S.size


if __name__ == '__main__':
    key = sys.argv[1]
    seat = float(sys.argv[2]) if len(sys.argv) > 2 else None
    bb = ROOT / 'livraison/bbmodels' / f'{key}.bbmodel'
    out = ROOT / 'apercus/travail'
    out.mkdir(parents=True, exist_ok=True)
    print(fiche(bb, out / f'{key}_fiche.png', seat=seat))
    if '--poses' in sys.argv:
        print(poses(bb, out / f'{key}_poses.png'))
