#!/usr/bin/env python3
"""Planche AVANT a partir des .bbmodel d'origine (avec maillages) : python planche_sources.py sortie.png colonnes chemin1 chemin2 ..."""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from render import Model, render

FONT = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 12)


def main(out, cols, chemins):
    cw, ch = 320, 320
    rows = (len(chemins) + cols - 1) // cols
    sheet = Image.new('RGB', (cw * cols, ch * rows), (225, 226, 232))
    for i, c in enumerate(chemins):
        x, y = (i % cols) * cw, (i // cols) * ch
        try:
            m = Model(c)
            im = render(m.quads(), m.tex, m.res, yaw=148, pitch=16, size=(cw, ch), margin=.05).convert('RGB')
            ImageDraw.Draw(im).text((4, 3), Path(c).stem, fill=(30, 26, 40), font=FONT)
            sheet.paste(im, (x, y))
        except Exception as e:
            ImageDraw.Draw(sheet).text((x + 4, y + 20), f'{Path(c).stem}\n{e!r}'[:120], fill=(160, 30, 30), font=FONT)
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print(out, sheet.size)


if __name__ == '__main__':
    a = sys.argv[1:]
    main(a[0], int(a[1]), a[2:])
