#!/usr/bin/env python3
"""Planche AVANT des blocs Java : python planche_java_avant.py sortie.png colonnes id1 id2 ..."""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from java_vers_bb import convertir
from render import Model, render

ROOT = Path(__file__).resolve().parents[1]
FONT = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 12)


def main(out, cols, ids):
    cw, ch = 260, 260
    rows = (len(ids) + cols - 1) // cols
    sheet = Image.new('RGB', (cw * cols, ch * rows), (225, 226, 232))
    for i, key in enumerate(ids):
        f = ROOT / f'apercus/avant/bbmodel_java/{key}.bbmodel'
        info = convertir(key, f)
        m = Model(f)
        im = render(m.quads(), m.tex, m.res, yaw=148, pitch=20, size=(cw, ch), margin=.08).convert('RGB')
        d = ImageDraw.Draw(im)
        d.text((4, 3), f'{key} {info[0]}', fill=(30, 26, 40), font=FONT)
        d.text((4, 18), f'x{info[1]} y{info[2]} z{info[3]}', fill=(90, 86, 100), font=FONT)
        sheet.paste(im, ((i % cols) * cw, (i // cols) * ch))
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print(out, sheet.size)


if __name__ == '__main__':
    a = sys.argv[1:]
    main(a[0], int(a[1]), a[2:])
