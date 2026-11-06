#!/usr/bin/env python3
"""Planche de travail des mobs, rendu logiciel d'un .bbmodel reel : face 3/4, dos 3/4, profil, et poses d'attaque. python vues_mob.py sortie.png id1 id2 ..."""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from render import Model, render

ROOT = Path(__file__).resolve().parents[1]
FONT = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 13)


def main(out, ids):
    cw, ch = 330, 330
    n = 5
    sheet = Image.new('RGB', (cw * n, ch * len(ids)), (232, 232, 236))
    for r, key in enumerate(ids):
        m = Model(ROOT / 'livraison/bbmodels' / f'{key}.bbmodel')
        base = m.quads(None, 0.0)
        vues = [(None, 0, 148, 16, 'face'), (None, 0, -38, 18, 'dos'), (None, 0, 90, 4, 'profil')]
        mv = 'walk' if 'walk' in m.anim else 'fly' if 'fly' in m.anim else 'slither'
        L = m.anim[mv]['length'] if mv in m.anim else 1
        vues.append((mv, L * .25, 148, 16, mv))
        if 'attack' in m.anim:
            vues.append(('attack', m.anim['attack']['length'] * .22, 148, 16, 'armement'))
        for c, (cl, t, yaw, pitch, lab) in enumerate(vues):
            q = m.quads(cl, t) if cl else base
            im = render(q, m.tex, m.res, yaw=yaw, pitch=pitch, size=(cw, ch), margin=.05).convert('RGB')
            d = ImageDraw.Draw(im)
            d.text((5, 3), f'{key} : {lab}' + (f' ({len(m.el)} cubes, {m.res["width"]} px)' if c == 0 else ''), fill=(30, 26, 40), font=FONT)
            sheet.paste(im, (c * cw, r * ch))
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print(out, sheet.size)


if __name__ == '__main__':
    a = sys.argv[1:]
    main(a[0], a[1:])
