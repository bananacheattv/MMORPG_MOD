#!/usr/bin/env python3
"""GIF de controle des animations : poses calculees depuis les cles du .bbmodel livre (rendu logiciel, eclairage d'entite Minecraft)."""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from render import Model, render

ROOT = Path(__file__).resolve().parents[1]
key = sys.argv[1]
clips = sys.argv[2].split(',')
out = ROOT / 'apercus/validation' / f'{key}_animations.gif'
m = Model(ROOT / 'livraison/bbmodels' / f'{key}.bbmodel')
q0 = m.quads()
pts = np.concatenate([c[0] for c in q0], 0)
ctr = (pts.min(0) + pts.max(0)) / 2
span = np.linalg.norm(pts.max(0) - pts.min(0))
size = (560, 420)
sc = min(size) * 1.5 / span
font = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 16)
frames = []
FPS = 16
NOMS = {'idle': 'repos (idle)', 'walk': 'marche (walk)', 'run': 'galop (run, à raccorder)', 'jump': 'saut (jump, à raccorder)', 'chest': 'boucle du bloc (chest)'}
for clip in clips:
    L = m.anim[clip]['length']
    n = max(8, int(L * FPS))
    reps = {'idle': 1, 'walk': 3, 'run': 4, 'jump': 2, 'chest': 1}.get(clip, 1)
    seq = []
    for i in range(n):
        t = L * i / n
        im = render(m.quads(clip, t), m.tex, m.res, yaw=62, pitch=14, size=size, scale=sc, center=ctr, bg=(222, 224, 230, 255)).convert('RGB')
        ImageDraw.Draw(im).text((10, 8), NOMS.get(clip, clip), fill=(40, 36, 50), font=font)
        seq.append(im)
    frames += seq * reps
pal = frames[len(frames) // 2].convert('P', palette=Image.ADAPTIVE, colors=128)
small = [f.quantize(palette=pal, dither=Image.NONE) for f in frames]
small[0].save(out, save_all=True, append_images=small[1:], duration=int(1000 / FPS), loop=0, optimize=True)
print(out.name, len(frames), 'images', out.stat().st_size // 1024, 'Ko')
