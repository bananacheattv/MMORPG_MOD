#!/usr/bin/env python3
"""Planches AVANT : montures et decors rendus depuis les fichiers du depot (copie)."""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from render import Model, render
from runtime_to_bbmodel import convert

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / 'originaux/git_main/src/main/resources/assets/mmorpg'
OUT = ROOT / 'apercus/avant'
(OUT / 'bbmodel').mkdir(parents=True, exist_ok=True)
font = ImageFont.load_default()


def sheet(cells, cols, name, cw=300, ch=240):
    rows = (len(cells) + cols - 1) // cols
    S = Image.new('RGB', (cols * cw, rows * ch), (40, 40, 46))
    d = ImageDraw.Draw(S)
    for i, (lab, im) in enumerate(cells):
        x, y = (i % cols) * cw, (i // cols) * ch
        S.paste(im.convert('RGB').resize((cw - 4, ch - 22), Image.LANCZOS), (x + 2, y + 20))
        d.text((x + 6, y + 4), lab, fill=(255, 255, 255), font=font)
    S.save(OUT / name)
    print(name, S.size)


def mounts():
    ids = sorted(p.stem for p in (A / 'bbmodels/mounts').glob('*.json'))
    cells = []
    for i in ids:
        bb = OUT / 'bbmodel' / f'{i}.bbmodel'
        tex = A / f'textures/imported/mounts/{i}.png'
        convert(A / f'bbmodels/mounts/{i}.json', tex if tex.exists() else A / 'textures/imported/mounts/hippogriffe.png', bb)
        m = Model(bb)
        cells.append((f'{i} ({len(m.el)} cubes, {m.res["width"]}px)', render(m.quads(), m.tex, m.res, yaw=-35, pitch=20, size=(600, 480), margin=.06)))
    sheet(cells, 6, 'planche_montures_git_main.png')
    # versions locales non poussees des 8 montures multi-places
    L = ROOT / 'originaux/local_non_pousse/bbmodels'
    cells = []
    for p in sorted(L.glob('*.bbmodel')):
        m = Model(p)
        cells.append((f'{p.stem} ({len(m.el)} cubes, {m.res["width"]}px)', render(m.quads(), m.tex, m.res, yaw=-35, pitch=20, size=(600, 480), margin=.06)))
    sheet(cells, 4, 'planche_montures_local.png')


if __name__ == '__main__' and len(sys.argv) == 1:
    mounts()


def decors():
    import numpy as np
    atlas = np.asarray(Image.open(A / 'textures/imported/props/decor_atlas.png').convert('RGBA'), dtype=np.uint8)
    tiny = OUT / 'bbmodel' / '_tiny.png'
    Image.new('RGBA', (1, 1), (0, 0, 0, 0)).save(tiny)
    tmp = OUT / 'bbmodel' / '_tmp.bbmodel'
    fam = {'deco_cat_': [], 'decob_': [], 'deco_': []}
    for p in sorted((A / 'bbmodels/props').glob('*.json')):
        key = p.stem
        if key in ('camp', 'drapeau'):
            continue
        convert(p, tiny, tmp)
        m = Model(tmp)
        m.tex = [atlas]
        fam['deco_cat_' if key.startswith('deco_cat_') else 'decob_' if key.startswith('decob_') else 'deco_'].append(
            (f'{key} ({len(m.el)})', render(m.quads(), m.tex, m.res, yaw=-35, pitch=20, size=(400, 320), margin=.05)))
    for k, cells in fam.items():
        sheet(cells, 8, f'planche_decors_{k.rstrip("_")}.png', cw=225, ch=190)
    tmp.unlink()
    tiny.unlink()


if __name__ == '__main__' and len(sys.argv) > 1 and sys.argv[1] == 'decors':
    decors()
