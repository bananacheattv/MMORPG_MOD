#!/usr/bin/env python3
"""Planche de travail d'un lot de montures : 3/4 avec cavaliers de controle, et profil. python vues_lot.py sortie.png id1 id2 ..."""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from render import Model, mannequin, render

ROOT = Path(__file__).resolve().parents[1]
FONT = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 15)
# siege (MountType) puis sieges {x, z} en blocs
S = {'sanglier': (1.26, [(0, 0)]), 'loup_givre': (1.39, [(0, 0)]), 'cerf_sylvestre': (1.76, [(0, 0)]), 'tortue_cristal': (1.2, [(0, 0)]), 'chevre_celeste': (1.45, [(0, 0)]), 'lezard_lave': (.95, [(0, 0)]),
     'raptor_sables': (1.39, [(0, 0)]), 'ours_runique': (1.45, [(0, 0)]), 'scorpion_dunes': (.9, [(0, 0)]), 'felin_vide': (1.33, [(0, 0)]), 'araignee_cavernes': (1.2, [(0, 0)]), 'hippogriffe': (1.6, [(0, 0)]),
     'chimere_arene': (1.45, [(0, 0)]), 'taureau_guerre': (1.69, [(0, .312), (0, -.375)]), 'lion_dore': (1.63, [(0, .344), (0, -.344)]), 'rhinoceros_cuirasse': (1.63, [(0, .625), (0, 0), (0, -.625)]),
     'bison_geant': (1.975, [(0, .562), (0, -.031), (0, -.625)]), 'mammouth': (2.44, [(-.344, .5), (.344, .5), (-.344, -.5), (.344, -.5)]), 'elephant_guerre': (2.57, [(-.344, .5), (.344, .5), (-.344, -.5), (.344, -.5)]),
     'tortue_forteresse': (1.69, [(0, .812), (-.375, .188), (.375, .188), (-.375, -.5), (.375, -.5)]), 'drake_caravane': (2.0, [(0, .812), (-.344, .188), (.344, .188), (-.344, -.5), (.344, -.5)]), 'drake_ascendant': (1.6, [(0, 0)])}


def main(out, ids, anim=None):
    cw, ch = 520, 420
    sheet = Image.new('RGB', (cw * 3, ch * len(ids)), (40, 40, 46))
    for r, key in enumerate(ids):
        m = Model(ROOT / 'livraison/bbmodels' / f'{key}.bbmodel')
        clip = 'idle' if anim == 'idle' and 'idle' in m.anim else None
        q = m.quads(clip, 0.0)
        extra = []
        if key in S:
            seat, seats = S[key]
            for (x, z) in seats:
                extra += mannequin(16 * x, 16 * seat + 1, -16 * z)
        v = [(148, 18, None, 'avant 3/4'), (-38, 18, extra, 'cavaliers 3/4'), (90, 0, None, 'profil')]
        for c, (yaw, pitch, ex, lab) in enumerate(v):
            im = render(q, m.tex, m.res, yaw=yaw, pitch=pitch, size=(cw, ch), margin=.05, extra=ex).convert('RGB')
            d = ImageDraw.Draw(im)
            d.text((6, 4), f'{key} : {lab}  ({len(m.el)} cubes, {m.res["width"]} px)', fill=(30, 26, 40), font=FONT)
            sheet.paste(im, (c * cw, r * ch))
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print(out, sheet.size)


if __name__ == '__main__':
    a = sys.argv[1:]
    anim = None
    if a and a[0] == '--idle':
        anim = 'idle'
        a = a[1:]
    main(a[0], a[1:], anim)
