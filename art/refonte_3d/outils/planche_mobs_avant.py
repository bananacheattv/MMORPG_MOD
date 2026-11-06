#!/usr/bin/env python3
"""Planche AVANT des mobs : python planche_mobs_avant.py sortie.png colonnes id1 id2 ... Lit apercus/avant/bbmodel_mobs/<id>.bbmodel."""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from render import Model, render

ROOT = Path(__file__).resolve().parents[1]
FONT = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 12)


def main(out, cols, ids):
    cw, ch = 240, 240
    rows = (len(ids) + cols - 1) // cols
    sheet = Image.new('RGB', (cw * cols, ch * rows), (225, 226, 232))
    for i, key in enumerate(ids):
        f = ROOT / f'apercus/avant/bbmodel_mobs/{key}.bbmodel'
        x, y = (i % cols) * cw, (i // cols) * ch
        try:
            m = Model(f)
            im = render(m.quads(), m.tex, m.res, yaw=148, pitch=16, size=(cw, ch), margin=.06).convert('RGB')
            d = ImageDraw.Draw(im)
            d.text((4, 3), key, fill=(30, 26, 40), font=FONT)
            sheet.paste(im, (x, y))
        except Exception as e:
            ImageDraw.Draw(sheet).text((x + 4, y + 20), f'{key}\n{e!r}'[:90], fill=(160, 30, 30), font=FONT)
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print(out, sheet.size)


if __name__ == '__main__':
    a = sys.argv[1:]
    main(a[0], int(a[1]), a[2:])
