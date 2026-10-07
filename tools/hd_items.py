"""
Icones HD (64 x 64) des objets du mod : formes par type d'objet, rendues par hd_render.

render_item(kind, base, accent=None, glow=None, seed=0) -> Image 64 x 64
"""
import math
import random

from PIL import ImageDraw

from hd_render import (S, Layer, compose, hexrgb, mix, lighten, darken, new_mask, poly, ellipse, union, minus,
                       blob, thick_line)


def _c(c):
    return hexrgb(c) if isinstance(c, str) else c


# ---------------------------------------------------------------------------- formes

def ingot(base, acc, glow, seed):
    top = poly([(70, 92), (188, 92), (214, 150), (44, 150)])
    side = poly([(44, 150), (214, 150), (204, 186), (54, 186)])
    shadow_face = poly([(188, 92), (214, 150), (204, 186), (176, 128)])
    layers = [Layer(union(top, side), base, 'metal', seed, bevel=8, spec=.9, glow=glow),
              Layer(side, darken(_c(base), .25), 'metal', seed + 1, bevel=6, spec=.4),
              Layer(shadow_face, darken(_c(base), .35), 'metal', seed + 2, bevel=4, spec=.2)]
    if acc:
        m, d = new_mask()
        for i in range(3):
            d.line([(100 + i * 24, 104), (92 + i * 24, 138)], fill=255, width=6)
        layers.append(Layer(m, acc, 'glow', seed, bevel=2, spec=.3, glow=acc))
    return layers


def gem(base, acc, glow, seed):
    pts = [(128, 30), (196, 84), (176, 196), (80, 196), (60, 84)]
    return [Layer(poly(pts), base, 'gem', seed, bevel=6, spec=1.0, glow=glow or lighten(_c(base), .4))]


def crystal(base, acc, glow, seed):
    rnd = random.Random(seed)
    layers = [Layer(blob(seed, 128, 204, 70, 5, .1), '#4a4450', 'stone', seed, bevel=10, spec=.25)]
    spikes = [(78, 74, 26, -26), (178, 84, 26, 22), (128, 18, 36, 0), (100, 112, 22, -10), (156, 122, 20, 12)]
    for i, (x, y, w, tilt) in enumerate(spikes):
        bx = x + tilt * 1.6
        m = poly([(x, y), (bx + w, 150), (bx + w * .6, 214), (bx - w * .6, 214), (bx - w, 150)])
        layers.append(Layer(m, mix(_c(base), (255, 255, 255), rnd.uniform(0, .15)), 'crystal', seed + i, bevel=5, spec=1, glow=glow))
    return layers

def shard(base, acc, glow, seed):
    m = poly([(150, 18), (196, 96), (138, 236), (70, 168), (92, 70)])
    return [Layer(m, base, 'crystal', seed, bevel=6, spec=1, glow=glow or base)]


def fragment(base, acc, glow, seed):
    m = poly([(128, 20), (206, 110), (150, 232), (58, 180), (70, 72)])
    inner = poly([(128, 70), (166, 118), (138, 180), (98, 150), (102, 104)])
    return [Layer(m, base, 'gem', seed, bevel=8, spec=1, glow=glow), Layer(inner, lighten(_c(base), .5), 'glow', seed, bevel=10, spec=.6, glow='#ffffff')]


def bone(base, acc, glow, seed):
    shaft = thick_line([(70, 186), (186, 70)], 34)
    knobs = union(ellipse((36, 160, 86, 210)), ellipse((60, 184, 110, 234)), ellipse((150, 22, 200, 72)), ellipse((174, 46, 224, 96)))
    layers = [Layer(union(shaft, knobs), base, 'bone', seed, bevel=14, spec=.35)]
    if acc:
        m, d = new_mask()
        for t in (0.35, 0.5, 0.65):
            x, y = 70 + 116 * t, 186 - 116 * t
            d.line([(x - 14, y - 14), (x + 14, y + 14)], fill=255, width=7)
        layers.append(Layer(m, acc, 'glow', seed, bevel=2, spec=.2, glow=glow or acc))
    return layers


def skull(base, acc, glow, seed):
    head = union(ellipse((52, 34, 204, 176)), poly([(84, 150), (172, 150), (164, 218), (92, 218)]))
    holes, d = new_mask()
    d.ellipse((76, 96, 120, 140), fill=255)
    d.ellipse((136, 96, 180, 140), fill=255)
    d.polygon([(128, 146), (116, 172), (140, 172)], fill=255)
    for x in range(98, 166, 16):
        d.rectangle((x, 192, x + 8, 216), fill=255)
    layers = [Layer(minus(head, holes), base, 'bone', seed, bevel=14, spec=.35)]
    if acc:
        eyes = union(ellipse((84, 104, 112, 132)), ellipse((144, 104, 172, 132)))
        layers.append(Layer(eyes, acc, 'glow', seed, bevel=3, spec=.4, glow=glow or acc))
    return layers


def fang(base, acc, glow, seed):
    outer, inner = [], []
    for i in range(31):
        t = i / 30
        a = math.pi * (0.95 - 0.55 * t)
        r = 150 - 40 * t
        w = 40 * (1 - t) ** 0.9 + 2
        cx, cy = 210 + math.cos(a) * r, 60 + math.sin(a) * r
        outer.append((cx + math.cos(a) * w * .5, cy + math.sin(a) * w * .5))
        inner.append((cx - math.cos(a) * w * .5, cy - math.sin(a) * w * .5))
    m = poly(outer + inner[::-1])
    root = blob(seed, 70, 92, 34, 4, .12)
    return [Layer(root, acc or darken(_c(base), .45), 'organic', seed, bevel=10, spec=.2),
            Layer(m, base, 'bone', seed, bevel=14, spec=.8)]

def claw(base, acc, glow, seed):
    layers = [Layer(blob(seed, 128, 192, 64, 6, .12), acc or darken(_c(base), .45), 'fur', seed, bevel=14, spec=.15)]
    for i, dx in enumerate((-58, 0, 58)):
        outer, inner = [], []
        for k in range(21):
            t = k / 20
            x = 128 + dx + 30 * math.sin(t * 1.4)
            y = 186 - 150 * t
            w = 30 * (1 - t) ** 0.8 + 2
            outer.append((x + w * .5, y))
            inner.append((x - w * .5, y + 6 * t))
        layers.append(Layer(poly(outer + inner[::-1]), base, 'bone', seed + i, bevel=10, spec=.8))
    return layers

def tusk(base, acc, glow, seed):
    outer, inner = [], []
    for i in range(31):
        t = i / 30
        a = math.pi * (0.62 + 0.75 * t)
        r = 120
        w = 46 * (1 - t) ** 0.8 + 3
        cx, cy = 150 + math.cos(a) * r, 140 + math.sin(a) * r * .9
        outer.append((cx + math.cos(a) * w * .5, cy + math.sin(a) * w * .5))
        inner.append((cx - math.cos(a) * w * .5, cy - math.sin(a) * w * .5))
    m = poly(outer + inner[::-1])
    ring = thick_line([(80, 190), (112, 236)], 24)
    return [Layer(m, base, 'bone', seed, bevel=16, spec=.8), Layer(ring, acc or '#6a4a2a', 'leather', seed, bevel=8, spec=.3)]

def feather(base, acc, glow, seed):
    vane = []
    for i in range(41):
        t = i / 40
        x = 60 + 130 * t
        y = 210 - 180 * t
        w = 44 * math.sin(math.pi * min(1, t * 1.1)) + 4
        vane.append((x - w * .7, y - w * .7))
    for i in range(40, -1, -1):
        t = i / 40
        x = 60 + 130 * t
        y = 210 - 180 * t
        w = 40 * math.sin(math.pi * min(1, t * 1.1)) + 4
        vane.append((x + w * .7, y + w * .7))
    m = poly(vane)
    m2, d = new_mask()
    for i in range(6):
        t = 0.25 + i * 0.11
        x, y = 60 + 130 * t, 210 - 180 * t
        d.line([(x, y), (x + 30, y + 30)], fill=255, width=3)
    shaft = thick_line([(44, 232), (196, 24)], 7)
    return [Layer(minus(m, m2), base, 'fur', seed, bevel=8, spec=.3), Layer(shaft, acc or lighten(_c(base), .6), 'bone', seed, bevel=3, spec=.5)]


def cloth(base, acc, glow, seed):
    roll = poly([(40, 96), (190, 66), (214, 174), (64, 204)])
    end = ellipse((22, 88, 82, 208))
    layers = [Layer(roll, base, 'cloth', seed, bevel=10, spec=.25, glow=glow), Layer(end, darken(_c(base), .2), 'cloth', seed + 1, bevel=14, spec=.2)]
    layers.append(Layer(ellipse((40, 128, 64, 166)), darken(_c(base), .55), 'cloth', seed, bevel=4, spec=0))
    if acc:
        m, d = new_mask()
        d.polygon([(110, 82), (126, 79), (150, 188), (134, 192)], fill=255)
        layers.append(Layer(m, acc, 'metal', seed, bevel=3, spec=.6))
    return layers


def leather(base, acc, glow, seed):
    hide = poly([(64, 40), (110, 58), (146, 58), (192, 40), (206, 96), (184, 128), (214, 200), (160, 190), (128, 226),
                 (96, 190), (42, 200), (72, 128), (50, 96)], smooth=8)
    layers = [Layer(hide, base, 'leather', seed, bevel=14, spec=.35)]
    if acc:
        m, d = new_mask()
        for i in range(14):
            y = 70 + i * 10
            d.line([(116, y), (140, y + 4)], fill=255, width=4)
        layers.append(Layer(m, acc, 'cloth', seed, bevel=2, spec=.3))
    return layers

def fur(base, acc, glow, seed):
    rnd = random.Random(seed)
    pelt = poly([(40, 92), (100, 60), (170, 64), (222, 104), (210, 180), (150, 210), (80, 206), (32, 166)], smooth=14)
    layers = [Layer(pelt, darken(_c(base), .15), 'fur', seed, bevel=14, spec=.2)]
    for k, shade in enumerate((.0, .22, .4)):
        tufts, d = new_mask()
        for _ in range(34):
            x, y = rnd.uniform(46, 210), rnd.uniform(66, 204)
            a = rnd.uniform(-0.35, 0.35) + math.pi * 0.62
            ln = rnd.uniform(26, 44)
            d.polygon([(x - 7, y), (x + 7, y), (x + math.cos(a) * ln, y + math.sin(a) * ln)], fill=255)
        layers.append(Layer(tufts, lighten(_c(base), shade), 'fur', seed + k + 1, bevel=3, spec=.4))
    return layers

def dust(base, acc, glow, seed):
    pile = poly([(30, 210), (80, 140), (128, 104), (176, 140), (226, 210)], smooth=12)
    layers = [Layer(pile, base, 'stone', seed, bevel=16, spec=.4, glow=glow)]
    rnd = random.Random(seed)
    m, d = new_mask()
    for _ in range(14):
        x, y, r = rnd.uniform(40, 216), rnd.uniform(110, 206), rnd.uniform(3, 7)
        d.ellipse((x - r, y - r, x + r, y + r), fill=255)
    layers.append(Layer(m, lighten(_c(base), .6), 'glow', seed, bevel=2, spec=.8, glow=glow or base))
    return layers


def vial(base, acc, glow, seed, round_=False):
    if round_:
        glass = union(ellipse((52, 86, 204, 238)), poly([(104, 40), (152, 40), (152, 100), (104, 100)]))
        liquid = minus(ellipse((60, 94, 196, 230)), poly([(0, 0), (S, 0), (S, 140), (0, 140)]))
    else:
        glass = union(poly([(92, 52), (164, 52), (164, 210), (92, 210)]), ellipse((92, 186, 164, 236)))
        liquid = union(poly([(100, 100), (156, 100), (156, 210), (100, 210)]), ellipse((100, 192, 156, 228)))
    cork = poly([(100, 22), (156, 22), (152, 58), (104, 58)]) if round_ else poly([(96, 22), (160, 22), (156, 60), (100, 60)])
    return [Layer(glass, '#cfe3f0', 'glass', seed, bevel=10, spec=1, alpha=120),
            Layer(liquid, base, 'organic', seed, bevel=12, spec=.7, glow=glow or lighten(_c(base), .25)),
            Layer(glass, '#ffffff', 'glass', seed, bevel=6, spec=1, alpha=22),
            Layer(cork, acc or '#8a5a32', 'wood', seed, bevel=6, spec=.2)]


def potion(base, acc, glow, seed):
    return vial(base, acc, glow, seed, round_=True)


def heart(base, acc, glow, seed):
    m = union(ellipse((40, 54, 136, 150)), ellipse((120, 54, 216, 150)), poly([(46, 120), (210, 120), (128, 222)]))
    return [Layer(m, base, 'organic', seed, bevel=18, spec=.8, glow=glow)]


def scrap(base, acc, glow, seed):
    rnd = random.Random(seed)
    layers = []
    for i, (x, y, r) in enumerate(((88, 160, 58), (170, 150, 52), (126, 92, 50))):
        pts = [(x + math.cos(a) * r * rnd.uniform(.7, 1.1), y + math.sin(a) * r * rnd.uniform(.6, 1.0)) for a in [k / 7 * 2 * math.pi for k in range(7)]]
        c = mix(_c(base), _c(acc) if acc else _c(base), (i % 2) * .5)
        layers.append(Layer(poly(pts), c, 'metal', seed + i, bevel=8, spec=.7))
    return layers

def coin(base, acc, glow, seed):
    m = ellipse((40, 40, 216, 216))
    inner = minus(ellipse((70, 70, 186, 186)), ellipse((92, 92, 164, 164)))
    layers = [Layer(m, base, 'gold', seed, bevel=10, spec=1), Layer(inner, darken(_c(base), .25), 'gold', seed, bevel=4, spec=.6)]
    if acc:
        layers.append(Layer(ellipse((100, 100, 156, 156)), acc, 'gem', seed, bevel=5, spec=1, glow=glow or acc))
    return layers


def ring(base, acc, glow, seed):
    band = minus(ellipse((44, 70, 212, 230)), ellipse((76, 104, 180, 202)))
    stone = poly([(128, 26), (166, 66), (128, 108), (90, 66)])
    return [Layer(band, base, 'gold', seed, bevel=10, spec=1), Layer(stone, acc or '#40c0ff', 'gem', seed, bevel=5, spec=1, glow=glow)]


def crown(base, acc, glow, seed):
    m = poly([(36, 196), (36, 84), (82, 138), (128, 60), (174, 138), (220, 84), (220, 196)])
    band = poly([(36, 172), (220, 172), (220, 210), (36, 210)])
    gems = union(ellipse((112, 178, 144, 206)), ellipse((60, 178, 88, 204)), ellipse((168, 178, 196, 204)))
    return [Layer(union(m, band), base, 'gold', seed, bevel=10, spec=1), Layer(gems, acc or '#e02040', 'gem', seed, bevel=4, spec=1, glow=glow)]


def seal(base, acc, glow, seed):
    disc = blob(seed, 128, 128, 86, 11, .08)
    emblem = poly([(128, 70), (150, 118), (200, 122), (160, 152), (174, 200), (128, 172), (82, 200), (96, 152), (56, 122), (106, 118)])
    return [Layer(disc, base, 'leather' if _c(base)[0] < 160 else 'metal', seed, bevel=12, spec=.5),
            Layer(emblem, acc or lighten(_c(base), .4), 'gold', seed, bevel=6, spec=.9, glow=glow)]


def flame(base, acc, glow, seed):
    def tongue(x, y, r, tx, ty):
        return union(ellipse((x - r, y - r, x + r, y + r)), poly([(x - r * .95, y - r * .2), (tx, ty), (x + r * .95, y - r * .2)]))
    outer = union(tongue(128, 160, 70, 136, 14), tongue(78, 172, 40, 54, 70), tongue(178, 172, 40, 208, 80))
    inner = tongue(128, 176, 40, 132, 80)
    core = tongue(128, 190, 20, 130, 136)
    return [Layer(outer, base, 'glow', seed, bevel=10, spec=.3, glow=base),
            Layer(inner, acc or '#ffd040', 'glow', seed, bevel=8, spec=.4, glow=acc or '#ffd040'),
            Layer(core, '#fff8d0', 'glow', seed, bevel=6, spec=.4, glow='#ffffff')]


def orb(base, acc, glow, seed):
    m = ellipse((40, 40, 216, 216))
    layers = [Layer(m, base, 'glow', seed, bevel=26, spec=1, glow=glow or acc or lighten(_c(base), .4))]
    core = ellipse((92, 92, 164, 164))
    layers.append(Layer(core, acc or lighten(_c(base), .5), 'glow', seed + 1, bevel=20, spec=.4, glow=acc or glow, alpha=200))
    swirl = []
    for i in range(60):
        t = i / 59
        a = t * 3.6 * math.pi
        r = 18 + 56 * t
        swirl.append((128 + math.cos(a) * r, 128 + math.sin(a) * r))
    layers.append(Layer(thick_line(swirl, 7), lighten(_c(acc or base), .4), 'glow', seed, bevel=3, spec=.3, glow=acc or glow, alpha=150))
    layers.append(Layer(ellipse((70, 62, 110, 92)), '#ffffff', 'glass', seed, bevel=8, spec=.2, alpha=140))
    return layers

def eye(base, acc, glow, seed):
    white = poly([(24, 128), (70, 76), (128, 62), (186, 76), (232, 128), (186, 180), (128, 194), (70, 180)], smooth=6)
    iris = ellipse((86, 86, 170, 170))
    pupil = poly([(128, 92), (140, 128), (128, 164), (116, 128)])
    return [Layer(white, acc or '#e8e0d0', 'organic', seed, bevel=14, spec=.6),
            Layer(iris, base, 'glow', seed, bevel=10, spec=1, glow=glow or base),
            Layer(pupil, '#0c0810', 'organic', seed, bevel=3, spec=.2)]


def shell(base, acc, glow, seed):
    m = union(ellipse((36, 52, 220, 216)))
    ridges, d = new_mask()
    for i in range(5):
        a = math.pi * (1.1 + i * 0.2)
        d.line([(128, 214), (128 + math.cos(a) * 110, 214 + math.sin(a) * 170)], fill=255, width=6)
    return [Layer(m, base, 'scale', seed, bevel=18, spec=.7, glow=glow), Layer(ImageChops_min(ridges, m), darken(_c(base), .4), 'organic', seed, bevel=3, spec=0)]


def ImageChops_min(a, b):
    from PIL import ImageChops
    return ImageChops.darker(a, b)


def scale(base, acc, glow, seed):
    m = union(ellipse((48, 30, 208, 190)), poly([(48, 110), (208, 110), (128, 236)]))
    return [Layer(m, base, 'scale', seed, bevel=16, spec=.9, glow=glow),
            Layer(ellipse((90, 64, 150, 124)), lighten(_c(base), .35), 'glow', seed, bevel=10, spec=.8, alpha=150)]


def chain(base, acc, glow, seed):
    layers = []
    for i in range(3):
        x, y = 74 + i * 54, 74 + i * 54
        link = minus(ellipse((x - 50, y - 34, x + 50, y + 34)), ellipse((x - 26, y - 13, x + 26, y + 13)))
        link = link.rotate(-45, center=(x, y))
        layers.append(Layer(link, base, 'metal', seed + i, bevel=7, spec=1, glow=glow))
    return layers

def wood(base, acc, glow, seed):
    plank = poly([(40, 96), (204, 52), (220, 120), (56, 164)])
    plank2 = poly([(48, 150), (212, 108), (224, 172), (62, 212)])
    layers = [Layer(plank2, darken(_c(base), .15), 'wood', seed + 1, bevel=8, spec=.2),
              Layer(plank, base, 'wood', seed, bevel=8, spec=.25, glow=glow)]
    if acc:
        layers.append(Layer(thick_line([(70, 120), (196, 88)], 8), acc, 'glow', seed, bevel=3, spec=.4, glow=acc))
    return layers


def mushroom(base, acc, glow, seed):
    cap = union(ellipse((30, 40, 226, 170)))
    cap = minus(cap, poly([(0, 120), (S, 120), (S, S), (0, S)]))
    cap = union(cap, ellipse((30, 96, 226, 140)))
    stem = poly([(104, 128), (152, 128), (160, 226), (96, 226)])
    spots = union(ellipse((70, 64, 98, 92)), ellipse((136, 50, 170, 84)), ellipse((178, 86, 202, 110)))
    return [Layer(stem, acc or '#e8dcc0', 'organic', seed, bevel=10, spec=.3),
            Layer(cap, base, 'organic', seed, bevel=16, spec=.6, glow=glow),
            Layer(ImageChops_min(spots, cap), lighten(_c(base), .7), 'glow', seed, bevel=4, spec=.4, glow=glow)]


def leaf(base, acc, glow, seed):
    m = poly([(40, 216), (60, 120), (128, 40), (210, 30), (200, 120), (140, 190)], smooth=10)
    veins = thick_line([(48, 208), (128, 120), (196, 44)], 6)
    return [Layer(m, base, 'organic', seed, bevel=12, spec=.4, glow=glow), Layer(ImageChops_min(veins, m), lighten(_c(base), .35), 'organic', seed, bevel=2, spec=.2)]


def pearl(base, acc, glow, seed):
    return [Layer(ellipse((54, 54, 202, 202)), base, 'glass', seed, bevel=34, spec=1, glow=glow or lighten(_c(base), .5)),
            Layer(ellipse((84, 76, 124, 108)), '#ffffff', 'glass', seed, bevel=10, spec=.3, alpha=170)]

def egg(base, acc, glow, seed):
    m = ellipse((62, 30, 194, 226))
    layers = [Layer(m, base, 'organic', seed, bevel=24, spec=.7, glow=glow)]
    if acc:
        rnd = random.Random(seed)
        sp, d = new_mask()
        for _ in range(9):
            x, y, r = rnd.uniform(80, 176), rnd.uniform(60, 200), rnd.uniform(6, 14)
            d.ellipse((x - r, y - r, x + r, y + r), fill=255)
        layers.append(Layer(ImageChops_min(sp, m), acc, 'organic', seed, bevel=4, spec=.3))
    return layers


def scroll(base, acc, glow, seed):
    paper = poly([(56, 60), (200, 60), (200, 196), (56, 196)])
    rolls = union(ellipse((40, 40, 216, 80)), ellipse((40, 176, 216, 216)))
    m, d = new_mask()
    for y in range(92, 172, 18):
        d.line([(80, y), (176, y)], fill=255, width=5)
    layers = [Layer(paper, base, 'cloth', seed, bevel=8, spec=.1), Layer(rolls, darken(_c(base), .2), 'wood', seed, bevel=12, spec=.3),
              Layer(m, '#5a4030', 'organic', seed, bevel=1, spec=0, alpha=170)]
    if acc:
        layers.append(Layer(ellipse((150, 150, 196, 196)), acc, 'gem', seed, bevel=5, spec=1, glow=glow or acc))
    return layers


def book(base, acc, glow, seed):
    cover = poly([(52, 40), (196, 52), (196, 222), (52, 210)])
    pages = poly([(196, 56), (212, 66), (212, 220), (196, 222)])
    layers = [Layer(pages, '#ece0c0', 'cloth', seed, bevel=4, spec=.1), Layer(cover, base, 'leather', seed, bevel=10, spec=.4)]
    if acc:
        layers.append(Layer(poly([(124, 90), (150, 126), (124, 166), (98, 126)]), acc, 'gem', seed, bevel=5, spec=1, glow=glow or acc))
    return layers


def key(base, acc, glow, seed):
    bow = minus(ellipse((30, 30, 120, 120)), ellipse((54, 54, 96, 96)))
    shaft = thick_line([(104, 104), (212, 212)], 22)
    teeth = union(poly([(170, 190), (192, 168), (214, 190), (192, 212)]), poly([(150, 210), (172, 188), (190, 206), (168, 228)]))
    return [Layer(union(bow, shaft, teeth), base, 'gold', seed, bevel=8, spec=1),
            Layer(ellipse((62, 62, 88, 88)), acc or '#40c0ff', 'gem', seed, bevel=4, spec=1, glow=glow)]


def chest(base, acc, glow, seed):
    body = poly([(32, 112), (224, 112), (224, 218), (32, 218)])
    lid = union(poly([(32, 76), (224, 76), (224, 116), (32, 116)]), ellipse((32, 44, 224, 112)))
    bands = union(poly([(52, 44), (72, 44), (72, 218), (52, 218)]), poly([(184, 44), (204, 44), (204, 218), (184, 218)]))
    lock = poly([(112, 102), (144, 102), (144, 146), (112, 146)])
    return [Layer(union(body, lid), base, 'wood', seed, bevel=8, spec=.3),
            Layer(ImageChops_min(bands, union(body, lid)), acc or '#c8a040', 'metal', seed, bevel=4, spec=.9),
            Layer(lock, acc or '#e0c060', 'gold', seed, bevel=5, spec=1, glow=glow)]


def stone(base, acc, glow, seed):
    m = blob(seed, 128, 132, 86, 6, .16)
    layers = [Layer(m, base, 'stone', seed, bevel=16, spec=.4)]
    if acc:
        rune, d = new_mask()
        d.line([(128, 80), (128, 184)], fill=255, width=10)
        d.line([(128, 108), (164, 82)], fill=255, width=9)
        d.line([(128, 148), (92, 122)], fill=255, width=9)
        layers.append(Layer(rune, acc, 'glow', seed, bevel=3, spec=.6, glow=glow or acc))
    return layers


def amulet(base, acc, glow, seed):
    cord = minus(ellipse((54, 10, 202, 150)), ellipse((68, 22, 188, 140)))
    cord = minus(cord, poly([(0, 90), (S, 90), (S, S), (0, S)]))
    frame = ellipse((70, 96, 186, 220))
    gemm = ellipse((92, 118, 164, 196))
    return [Layer(cord, '#5a4030', 'leather', seed, bevel=3, spec=.2), Layer(frame, base, 'gold', seed, bevel=10, spec=1),
            Layer(gemm, acc or '#40ff90', 'gem', seed, bevel=6, spec=1, glow=glow or acc)]


def meat(base, acc, glow, seed):
    m = blob(seed, 140, 120, 74, 4, .18)
    bonep = union(thick_line([(80, 170), (40, 214)], 22), ellipse((20, 196, 52, 228)), ellipse((36, 212, 68, 244)))
    return [Layer(bonep, '#ece2cc', 'bone', seed, bevel=8, spec=.5), Layer(m, base, 'organic', seed, bevel=16, spec=.5),
            Layer(ImageChops_min(blob(seed + 3, 150, 108, 44, 4, .2), m), acc or lighten(_c(base), .3), 'organic', seed, bevel=10, spec=.4)]


def clover(base, acc, glow, seed, leaves=3):
    layers = []
    angs = [-90, 30, 150] if leaves == 3 else [-135, -45, 45, 135]
    stem = thick_line([(128, 128), (150, 200), (170, 236)], 12)
    layers.append(Layer(stem, darken(_c(base), .3), 'organic', seed, bevel=4, spec=.2))
    for i, a in enumerate(angs):
        r = math.radians(a)
        cx, cy = 128 + math.cos(r) * 50, 120 + math.sin(r) * 50
        ox, oy = math.cos(r + 1.57) * 18, math.sin(r + 1.57) * 18
        leafm = union(ellipse((cx - 44 + ox, cy - 44 + oy, cx + 44 + ox, cy + 44 + oy)),
                      ellipse((cx - 44 - ox, cy - 44 - oy, cx + 44 - ox, cy + 44 - oy)),
                      thick_line([(128, 120), (cx, cy)], 40))
        layers.append(Layer(leafm, base, 'organic', seed + i, bevel=12, spec=.6, glow=glow))
    layers.append(Layer(ellipse((112, 104, 144, 136)), lighten(_c(base), .3), 'organic', seed, bevel=6, spec=.5, glow=glow))
    return layers

def slime(base, acc, glow, seed):
    m = poly([(36, 210), (50, 120), (128, 60), (206, 120), (220, 210)], smooth=16)
    return [Layer(m, base, 'glass', seed, bevel=26, spec=1, glow=glow or lighten(_c(base), .4), alpha=230)]


def herb(base, acc, glow, seed):
    layers = []
    for i, (x, a) in enumerate(((96, -0.4), (128, 0), (160, 0.4))):
        tip = (x + math.sin(a) * 100, 40 + abs(a) * 40)
        m = poly([(x - 14, 220), tip, (x + 14, 220)], smooth=4)
        layers.append(Layer(m, mix(_c(base), (255, 255, 255), i * .08), 'organic', seed + i, bevel=8, spec=.4, glow=glow))
    if acc:
        layers.append(Layer(thick_line([(80, 210), (176, 210)], 16), acc, 'cloth', seed, bevel=4, spec=.2))
    return layers


def ear(base, acc, glow, seed):
    outer = poly([(60, 210), (40, 140), (70, 70), (150, 26), (222, 22), (200, 96), (150, 170), (100, 214)], smooth=10)
    inner = poly([(88, 186), (76, 136), (100, 90), (160, 52), (194, 48), (176, 100), (134, 156), (104, 190)], smooth=8)
    return [Layer(outer, base, 'organic', seed, bevel=16, spec=.4), Layer(inner, acc or lighten(_c(base), .3), 'organic', seed + 1, bevel=10, spec=.3)]


KINDS = {
    'ear': ear,
    'ingot': ingot, 'gem': gem, 'crystal': crystal, 'shard': shard, 'fragment': fragment, 'bone': bone, 'skull': skull,
    'fang': fang, 'claw': claw, 'tusk': tusk, 'feather': feather, 'cloth': cloth, 'leather': leather, 'fur': fur,
    'dust': dust, 'vial': vial, 'potion': potion, 'heart': heart, 'scrap': scrap, 'coin': coin, 'ring': ring,
    'crown': crown, 'seal': seal, 'flame': flame, 'orb': orb, 'eye': eye, 'shell': shell, 'scale': scale,
    'chain': chain, 'wood': wood, 'mushroom': mushroom, 'leaf': leaf, 'pearl': pearl, 'egg': egg, 'scroll': scroll,
    'book': book, 'key': key, 'chest': chest, 'stone': stone, 'amulet': amulet, 'meat': meat, 'slime': slime,
    'herb': herb, 'clover3': lambda b, a, g, s: clover(b, a, g, s, 3), 'clover4': lambda b, a, g, s: clover(b, a, g, s, 4),
}


def render_item(kind, base, accent=None, glow=None, seed=0, halo=None, sparkle=False):
    layers = KINDS[kind](base, accent, glow, seed)
    return compose(layers, halo=halo, sparkle=sparkle)
