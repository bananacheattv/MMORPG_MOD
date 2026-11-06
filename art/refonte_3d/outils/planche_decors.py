#!/usr/bin/env python3
"""Planche de controle des decors refaits : vue 3/4 de face, meme angle pour tous.
    python planche_decors.py sortie.png colonnes id1 id2 ..."""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from render import Model, render

ROOT = Path(__file__).resolve().parents[1]
try:
    FONT = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 13)
except OSError:
    FONT = ImageFont.load_default()


def main(out, cols, ids):
    cw, ch = 300, 300
    rows = (len(ids) + cols - 1) // cols
    sheet = Image.new('RGB', (cw * cols, ch * rows), (225, 226, 232))
    for i, key in enumerate(ids):
        m = Model(ROOT / 'livraison/bbmodels' / f'{key}.bbmodel')
        im = render(m.quads(), m.tex, m.res, yaw=148, pitch=20, size=(cw, ch), margin=.06).convert('RGB')
        d = ImageDraw.Draw(im)
        d.text((5, 3), f'{key} ({len(m.el)})', fill=(30, 26, 40), font=FONT)
        sheet.paste(im, ((i % cols) * cw, (i // cols) * ch))
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print(out, sheet.size)


if __name__ == '__main__':
    a = sys.argv[1:]
    main(a[0], int(a[1]), a[2:])
