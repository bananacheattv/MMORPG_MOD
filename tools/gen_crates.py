"""Caisses en blocs 3D (posees par les admins, ouvertes avec une cle) : textures 64x64, modeles fermes / ouverts, blockstates."""
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/mmorpg'

# id : (nom, bois, metal, gemme)
TIERS = {
    'vote': ('Caisse de vote', '#2f5a3a', '#c8d0c8', '#50ff80'),
    'quete': ('Caisse de quête', '#6a4a2a', '#c09050', '#ffd040'),
    'commune': ('Caisse commune', '#7a5230', '#9a9aa4', '#c8c8c8'),
    'rare': ('Caisse rare', '#3d4a5c', '#c8d4e4', '#3a8cff'),
    'epique': ('Caisse épique', '#3a2450', '#b48cff', '#d040ff'),
    'legendaire': ('Caisse légendaire', '#4a2a18', '#f0c040', '#ff8020'),
    'mythique': ('Caisse mythique', '#2a0f1c', '#ff5070', '#ff2040'),
}
KEYS = {t: 'Clé ' + n.split(' ', 1)[1].replace('de ', '') for t, (n, *_) in TIERS.items()}
KEYS.update({'vote': 'Clé de vote', 'quete': 'Clé de quête'})


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def texture(wood, metal, gem, seed):
    """Atlas 64x64 (1 unite de modele = 4 px) : bois (0-32,0-32), metal (32-64,0-16), serrure (32-48,16-32),
    gemme (48-64,16-32), couvercle (0-32,32-64), dessous (32-64,32-64)."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (64, 64))
    d = ImageDraw.Draw(img)
    w, m, g = rgb(wood), rgb(metal), rgb(gem)

    def planks(x0, y0, x1, y1, vertical):
        n = 4
        size = (x1 - x0) if vertical else (y1 - y0)
        for i in range(n):
            a = x0 + i * size // n if vertical else y0 + i * size // n
            b = x0 + (i + 1) * size // n if vertical else y0 + (i + 1) * size // n
            base = shade(w, rnd.uniform(.85, 1.1))
            box = (a, y0, b - 1, y1 - 1) if vertical else (x0, a, x1 - 1, b - 1)
            d.rectangle(box, fill=base)
            # fibres du bois
            for _ in range(10):
                if vertical:
                    x = rnd.randint(a, b - 2); y = rnd.randint(y0, y1 - 6)
                    d.line((x, y, x, y + rnd.randint(3, 8)), fill=shade(base, rnd.uniform(.7, .9)))
                else:
                    y = rnd.randint(a, b - 2); x = rnd.randint(x0, x1 - 6)
                    d.line((x, y, x + rnd.randint(3, 8), y), fill=shade(base, rnd.uniform(.7, .9)))
            for _ in range(2):
                kx, ky = (rnd.randint(a + 1, b - 3), rnd.randint(y0 + 2, y1 - 3)) if vertical else (rnd.randint(x0 + 2, x1 - 3), rnd.randint(a + 1, b - 3))
                d.ellipse((kx, ky, kx + 2, ky + 1), fill=shade(base, .6))
            # joint sombre entre les planches
            if vertical:
                d.line((b - 1, y0, b - 1, y1 - 1), fill=shade(w, .45))
            else:
                d.line((x0, b - 1, x1 - 1, b - 1), fill=shade(w, .45))

    def metal_plate(x0, y0, x1, y1):
        for y in range(y0, y1):
            t = (y - y0) / max(1, y1 - y0 - 1)
            d.line((x0, y, x1 - 1, y), fill=shade(m, 1.25 - .5 * t))
        d.rectangle((x0, y0, x1 - 1, y1 - 1), outline=shade(m, .55))
        for x in range(x0 + 3, x1 - 2, 6):
            for y in (y0 + 3, y1 - 4):
                d.rectangle((x, y, x + 1, y + 1), fill=shade(m, 1.45))
                d.point((x + 1, y + 1), fill=shade(m, .5))

    planks(0, 0, 32, 32, True)
    metal_plate(32, 0, 64, 16)
    # serrure : plaque metal + trou de serrure + reflet de gemme
    metal_plate(32, 16, 48, 32)
    d.ellipse((37, 19, 43, 25), fill=g, outline=shade(g, .5))
    d.point((38, 20), fill=(255, 255, 255))
    d.rectangle((39, 25, 41, 29), fill=(20, 15, 20))
    # gemme
    for r in range(8, 0, -1):
        c = shade(g, .55 + .07 * (8 - r))
        d.ellipse((56 - r, 24 - r, 56 + r - 1, 24 + r - 1), fill=c)
    d.ellipse((51, 19, 54, 22), fill=(255, 255, 255, 220))
    planks(0, 32, 32, 64, False)
    d.rectangle((0, 32, 31, 63), outline=shade(m, .9))
    d.rectangle((1, 33, 30, 62), outline=shade(m, .6))
    planks(32, 32, 64, 64, False)
    return img


WOOD, METAL, LOCK, GEM, LID, BOTTOM = [0, 0, 8, 8], [8, 0, 16, 4], [8, 4, 12, 8], [12, 4, 16, 8], [0, 8, 8, 16], [8, 8, 16, 16]
ALL = ('north', 'south', 'east', 'west', 'up', 'down')


def box(frm, to, side, top=None, bottom=None, faces=ALL, north=None, rot=None):
    uv = {f: side for f in faces}
    if 'up' in uv and top: uv['up'] = top
    if 'down' in uv and bottom: uv['down'] = bottom
    if 'north' in uv and north: uv['north'] = north
    e = {'from': frm, 'to': to, 'faces': {f: {'uv': u, 'texture': '#t'} for f, u in uv.items()}}
    if rot: e['rotation'] = rot
    return e


def model(tier, open_):
    lid = {'angle': -45, 'axis': 'x', 'origin': [8, 9, 15]} if open_ else None
    els = [
        # coffre
        box([1, 0, 1], [15, 9, 15], WOOD, top=BOTTOM, bottom=BOTTOM),
        box([0.5, 0, 0.5], [15.5, 1.5, 15.5], METAL),
        box([0.5, 8, 0.5], [15.5, 9, 15.5], METAL),
        box([3, 0, 0.6], [5, 9, 15.4], METAL, faces=('north', 'south', 'up', 'down')),
        box([11, 0, 0.6], [13, 9, 15.4], METAL, faces=('north', 'south', 'up', 'down')),
        # couvercle : pivote sur l'arriere quand la caisse est ouverte
        box([1, 9, 1], [15, 13, 15], WOOD, top=LID, bottom=BOTTOM, rot=lid),
        box([0.5, 9, 0.5], [15.5, 10, 15.5], METAL, rot=lid),
        box([3, 9, 0.6], [5, 13.4, 15.4], METAL, faces=('north', 'south', 'up'), rot=lid),
        box([11, 9, 0.6], [13, 13.4, 15.4], METAL, faces=('north', 'south', 'up'), rot=lid),
        box([6.5, 7, 0.2], [9.5, 11, 1], METAL, north=LOCK, faces=('north', 'east', 'west', 'up', 'down'), rot=lid),
    ]
    if tier in ('epique', 'legendaire', 'mythique'):
        els.append(box([7, 13, 7], [9, 14.5, 9], GEM, rot=lid))
    t = f'mmorpg:block/caisse_{tier}'
    return {'parent': 'minecraft:block/block', 'textures': {'particle': t, 't': t}, 'elements': els,
            'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 1, 0], 'scale': [0.7, 0.7, 0.7]},
                        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.3, 0.3, 0.3]},
                        'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
                        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
                        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}}


def generate(g):
    for i, (tier, (name, wood, metal, gem)) in enumerate(TIERS.items()):
        bid = 'caisse_' + tier + '_bloc'
        p = ROOT / 'textures/block' / f'caisse_{tier}.png'
        p.parent.mkdir(parents=True, exist_ok=True)
        texture(wood, metal, gem, 100 + i).save(p)
        g.write_json(str(ROOT / 'models/block' / (bid + '.json')), model(tier, False))
        g.write_json(str(ROOT / 'models/block' / (bid + '_ouverte.json')), model(tier, True))
        variants = {}
        for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            for open_ in (False, True):
                v = {'model': f'mmorpg:block/{bid}' + ('_ouverte' if open_ else '')}
                if y: v['y'] = y
                variants[f'facing={facing},open={str(open_).lower()}'] = v
        g.write_json(str(ROOT / 'blockstates' / (bid + '.json')), {'variants': variants})
        g.write_json(str(ROOT / 'items' / (bid + '.json')), {'model': {'type': 'minecraft:model', 'model': f'mmorpg:block/{bid}'}})
        g.LANG[f'block.mmorpg.{bid}'] = name
    for tier, name in KEYS.items():
        g.LANG[f'item.mmorpg.cle_{tier}'] = name
