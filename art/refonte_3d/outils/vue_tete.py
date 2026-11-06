#!/usr/bin/env python3
"""Gros plans de la tete et de la selle : python vue_tete.py <id> z_tete y_tete."""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from render import Model, mannequin, render

ROOT = Path(__file__).resolve().parents[1]
key = sys.argv[1]
m = Model(ROOT / 'livraison/bbmodels' / f'{key}.bbmodel')
q = m.quads()
ctr_h = (0, float(sys.argv[3]) if len(sys.argv) > 3 else 15, float(sys.argv[2]) if len(sys.argv) > 2 else -24)
S = Image.new('RGB', (1500, 1000), (40, 40, 46))
for i, (yaw, pitch) in enumerate(((0, 0), (40, 15), (-40, 15), (90, 0), (140, 30), (-140, 30))):
    im = render(q, m.tex, m.res, yaw=yaw, pitch=pitch, size=(500, 500), scale=13, center=ctr_h)
    S.paste(im.convert('RGB'), ((i % 3) * 500, (i // 3) * 500))
S.save(ROOT / 'apercus/travail' / f'{key}_tete.png')
tex = Image.fromarray(m.tex[0], 'RGBA')
bg = Image.new('RGB', tex.size, (120, 40, 120))
bg.paste(tex, mask=tex.split()[3])
bg.resize((tex.width * 3, tex.height * 3), Image.NEAREST).save(ROOT / 'apercus/travail' / f'{key}_uv.png')
