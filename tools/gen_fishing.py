#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Peche d'Aeloria : modeles 3D (JSON d'elements) et textures peintes par code pour les six cannes, les poissons, les dechets, les coffres
et les ingredients marins, definitions d'objets et traductions.

La liste des objets est lue dans com.mmorpg.fishing.FishCatalog (Java) : un seul endroit a modifier.
Usage : python tools/gen_fishing.py            ecrit les ressources
        python tools/gen_fishing.py --preview  ecrit en plus des planches d'apercu dans docs/screens/peche (apercu logiciel)
"""
import json
import os
import random
import re
import sys
import zlib

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import fishing_geom as G  # noqa: E402
import gen_mine  # noqa: E402  (merge_lang)

MODID = 'mmorpg'
ROOT = os.path.join(HERE, '..')
RES = os.path.join(ROOT, 'src', 'main', 'resources')
ASSETS = os.path.join(RES, 'assets', MODID)
CATALOG = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'mmorpg', 'fishing', 'FishCatalog.java')


# ------------------------------------------------------------------ catalogue (lu dans le code Java)

def read_catalog():
    src = open(CATALOG, encoding='utf-8').read()
    fish, junk, chests, ingredients, rods = [], [], [], [], []
    for m in re.finditer(r'\bfish\("(\w+)",\s*"([^"]+)",\s*"([^"]+)",\s*Rarity\.(\w+),.*?"(\w+)",\s*"([^"]*)"\);', src, re.S):
        fish.append(dict(id=m.group(1), fr=m.group(2), en=m.group(3), rarity=m.group(4), shape=m.group(5)))
    for m in re.finditer(r'\bjunk\("(\w+)",\s*"([^"]+)",\s*"([^"]+)"', src):
        junk.append(dict(id=m.group(1), fr=m.group(2), en=m.group(3)))
    for m in re.finditer(r'\bchest\("(\w+)",\s*"([^"]+)",\s*"([^"]+)",\s*Rarity\.(\w+),\s*(\d+)', src):
        chests.append(dict(id=m.group(1), fr=m.group(2), en=m.group(3), rarity=m.group(4), rank=int(m.group(5))))
    for m in re.finditer(r'new Ingredient\("(\w+)",\s*"([^"]+)",\s*"([^"]+)",\s*Rarity\.(\w+)', src):
        ingredients.append(dict(id=m.group(1), fr=m.group(2), en=m.group(3), rarity=m.group(4)))
    for m in re.finditer(r'new Rod\((\d+),\s*"(\w+)",\s*"([^"]+)",\s*"([^"]+)"', src):
        rods.append(dict(tier=int(m.group(1)), id=m.group(2), fr=m.group(3), en=m.group(4)))
    return fish, junk, chests, ingredients, rods


# ------------------------------------------------------------------ outils de peinture

def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# ------------------------------------------------------------------ poissons

# couleurs : dos, flanc, ventre, accent (motif), nageoires, motif, lueur (ou None)
STYLE = {
    'gardon_argente': ((70, 92, 115), (172, 188, 204), (236, 241, 246), (225, 80, 60), (225, 115, 95), 'scales', None),
    'carpe_doree': ((150, 100, 30), (238, 182, 62), (250, 226, 150), (255, 120, 40), (240, 150, 50), 'bigscales', None),
    'sardine_lunaire': ((40, 52, 112), (150, 170, 222), (226, 236, 255), (255, 255, 205), (112, 132, 192), 'moondots', (255, 255, 205)),
    'maquereau_bleu': ((20, 70, 122), (82, 152, 192), (220, 236, 242), (14, 44, 82), (30, 92, 142), 'wavy', None),
    'goujon_des_marais': ((70, 82, 40), (132, 142, 72), (200, 200, 142), (44, 52, 28), (150, 142, 82), 'spots', None),
    'omble_des_glaces': ((60, 92, 112), (140, 182, 202), (242, 250, 255), (255, 196, 205), (212, 236, 246), 'lightspots', None),
    'tilapia_solaire': ((190, 92, 20), (250, 152, 42), (255, 216, 112), (200, 60, 20), (255, 122, 42), 'bars', None),
    'anchois_des_abysses': ((15, 20, 50), (50, 72, 122), (100, 130, 172), (80, 222, 255), (40, 62, 112), 'glowdots', (80, 222, 255)),
    'truite_arc_en_ciel': ((70, 112, 70), (162, 172, 152), (242, 237, 227), (232, 92, 122), (222, 152, 132), 'rainbow', None),
    'brochet_dore': ((60, 92, 40), (172, 152, 52), (226, 216, 152), (242, 212, 92), (182, 122, 42), 'pikebars', None),
    'bar_ecarlate': ((152, 26, 32), (226, 72, 62), (250, 192, 172), (255, 140, 120), (202, 42, 42), 'scales', None),
    'anguille_voltaique': ((20, 30, 52), (40, 62, 92), (112, 142, 162), (90, 222, 255), (30, 52, 82), 'zigzag', (90, 222, 255)),
    'piranha_pourpre': ((52, 22, 92), (132, 62, 172), (232, 112, 62), (202, 162, 232), (202, 82, 62), 'spots', None),
    'poisson_lanterne': ((10, 25, 62), (32, 72, 132), (72, 122, 172), (112, 242, 255), (22, 52, 102), 'glowdots', (112, 242, 255)),
    'saumon_royal': ((52, 72, 92), (202, 112, 102), (250, 202, 182), (40, 40, 52), (212, 122, 112), 'darkspots', None),
    'esturgeon_ambre': ((82, 62, 32), (172, 122, 52), (236, 206, 142), (250, 232, 172), (152, 102, 42), 'scutes', None),
    'raie_cristalline': ((112, 202, 232), (172, 232, 247), (236, 251, 255), (255, 255, 255), (142, 212, 240), 'facets', (200, 250, 255)),
    'silure_geant': ((42, 46, 36), (86, 92, 66), (192, 192, 152), (28, 30, 24), (72, 76, 56), 'mottled', None),
    'perche_de_givre': ((62, 102, 132), (152, 202, 227), (237, 250, 255), (72, 132, 172), (202, 232, 247), 'bars', None),
    'thon_aurore': ((20, 40, 112), (112, 152, 212), (255, 202, 152), (255, 212, 62), (232, 172, 62), 'dawn', None),
    'baudroie_abyssale': ((30, 22, 30), (72, 52, 62), (122, 97, 102), (22, 14, 22), (62, 42, 52), 'mottled', (120, 255, 200)),
    'dragon_des_lacs': ((22, 92, 72), (62, 172, 132), (202, 242, 202), (242, 202, 72), (242, 202, 72), 'bigscales', (242, 202, 72)),
    'requin_de_corail': ((72, 72, 92), (152, 152, 172), (242, 237, 237), (232, 72, 72), (112, 112, 132), 'coralpatches', None),
    'poisson_etoile': ((52, 42, 122), (122, 102, 232), (192, 172, 255), (255, 255, 205), (162, 142, 252), 'sparkles', (255, 255, 205)),
    'kraken_juvenile': ((82, 26, 62), (152, 52, 102), (222, 122, 162), (255, 202, 222), (122, 36, 82), 'suckers', None),
    'leviathan_nain': ((10, 52, 72), (32, 112, 132), (152, 212, 202), (122, 255, 232), (20, 82, 102), 'runes', (122, 255, 232)),
    'carpe_du_neant': ((26, 10, 46), (72, 32, 122), (142, 92, 202), (232, 192, 255), (92, 42, 152), 'voidswirl', (232, 192, 255)),
}


def paint_fish(fid, seed):
    back, mid, belly, accent, fin, pat, glow = STYLE[fid]
    r = random.Random(seed)
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = im.load()
    # --- flanc (0..32 x 0..20) : degrade dos -> ventre
    for y in range(20):
        t = y / 19.0
        for x in range(32):
            if t < 0.35:
                c = mix(back, mid, t / 0.35)
            elif t < 0.62:
                c = mid
            else:
                c = mix(mid, belly, (t - 0.62) / 0.38)
            px[x, y] = c + (255,)

    def put(x, y, c):
        if 0 <= x < 32 and 0 <= y < 20:
            px[x, y] = tuple(c) + (255,)

    if pat in ('scales', 'bigscales', 'runes', 'voidswirl', 'dragon'):
        step = 3 if pat == 'scales' else 4
        for y in range(1, 19, 2):
            for x in range(-step, 32, step):
                ox = x + (step // 2 if (y // 2) % 2 else 0)
                for dx in range(step):
                    put(ox + dx, y, shade(px[min(31, max(0, ox + dx)), y][:3], 0.82))
                put(ox, y + 1, shade(px[min(31, max(0, ox)), min(19, y + 1)][:3], 1.12))
    if pat == 'moondots':
        for _ in range(26):
            x, y = r.randrange(2, 28), r.randrange(2, 16)
            put(x, y, accent)
    if pat == 'wavy':
        for x in range(32):
            for k in (3, 6, 9):
                put(x, k + int(1.4 * ((x // 2) % 2)), accent)
    if pat == 'spots':
        for _ in range(16):
            x, y = r.randrange(1, 28), r.randrange(2, 14)
            put(x, y, accent)
            put(x + 1, y, accent)
    if pat == 'lightspots':
        for _ in range(20):
            x, y = r.randrange(1, 29), r.randrange(2, 15)
            put(x, y, accent)
    if pat == 'bars':
        for x in range(3, 24, 4):
            for y in range(1, 15):
                put(x, y, accent)
                put(x + 1, y, shade(accent, 1.12))
    if pat == 'rainbow':
        for x in range(32):
            for y in (8, 9, 10):
                put(x, y, mix(accent, (255, 170, 190), (y - 8) / 2.0))
        for _ in range(26):
            put(r.randrange(1, 28), r.randrange(1, 7), (50, 50, 60))
    if pat == 'pikebars':
        for _ in range(26):
            x, y = r.randrange(1, 26), r.randrange(2, 15)
            put(x, y, accent)
            put(x + 1, y, accent)
            put(x + 2, y, accent)
    if pat == 'zigzag':
        y = 9
        for x in range(32):
            y += r.choice((-1, 1, 0))
            y = max(5, min(13, y))
            put(x, y, accent)
            put(x, y + 1, shade(accent, 0.7))
    if pat == 'darkspots':
        for _ in range(26):
            put(r.randrange(1, 28), r.randrange(1, 10), accent)
    if pat == 'scutes':
        for x in range(2, 26, 5):
            for y in (4, 9, 14):
                for dx in range(3):
                    for dy in range(2):
                        put(x + dx, y + dy, accent if dy == 0 else shade(accent, 0.8))
    if pat == 'facets':
        for _ in range(14):
            x, y = r.randrange(0, 28), r.randrange(0, 16)
            for k in range(5):
                put(x + k, y + k // 2, accent)
    if pat == 'mottled':
        for _ in range(60):
            put(r.randrange(0, 31), r.randrange(0, 19), accent if r.random() < 0.6 else belly)
    if pat == 'dawn':
        for y in range(20):
            for x in range(32):
                if 7 <= y <= 9:
                    put(x, y, accent)
    if pat == 'coralpatches':
        for _ in range(6):
            x, y = r.randrange(2, 24), r.randrange(2, 12)
            for dx in range(4):
                for dy in range(3):
                    put(x + dx, y + dy, accent if (dx + dy) % 3 else shade(accent, 0.8))
    if pat == 'sparkles':
        for _ in range(20):
            x, y = r.randrange(1, 29), r.randrange(1, 17)
            put(x, y, accent)
            put(x - 1, y, shade(accent, 0.7))
            put(x + 1, y, shade(accent, 0.7))
            put(x, y - 1, shade(accent, 0.7))
            put(x, y + 1, shade(accent, 0.7))
    if pat == 'suckers':
        for _ in range(18):
            x, y = r.randrange(1, 29), r.randrange(2, 16)
            put(x, y, accent)
            put(x + 1, y, shade(accent, 0.8))
    if pat == 'glowdots':
        for _ in range(22):
            put(r.randrange(1, 29), r.randrange(8, 17), accent)
    if pat == 'runes':
        for x in range(4, 28, 7):
            for k in range(5):
                put(x, 4 + k, accent)
            put(x + 1, 6, accent)
            put(x - 1, 8, accent)
    if pat == 'voidswirl':
        for t in range(60):
            a = t / 60.0 * 6.28 * 2
            import math
            put(int(20 + math.cos(a) * (3 + t / 12.0)), int(9 + math.sin(a) * (3 + t / 14.0)), accent)
    # tete : ouie sombre et bouche
    for y in range(4, 16):
        put(22, y, shade(px[22, y][:3], 0.7))
    for x in range(29, 32):
        put(x, 11, (30, 20, 22))
    if glow:
        for _ in range(5):
            put(r.randrange(2, 28), r.randrange(2, 17), glow)
    # --- dos (0..32 x 20..24) et ventre (0..32 x 24..28)
    for y in range(20, 24):
        for x in range(32):
            c = shade(back, 0.9 + 0.2 * r.random())
            if pat in ('bars', 'pikebars', 'tigerbars') and x % 4 < 2:
                c = mix(c, accent, 0.55)
            px[x, y] = c + (255,)
    for y in range(24, 28):
        for x in range(32):
            px[x, y] = shade(belly, 0.95 + 0.1 * r.random()) + (255,)
    # --- nageoire (0..16 x 28..32) : rayons verticaux
    for y in range(28, 32):
        for x in range(16):
            c = fin if x % 2 == 0 else shade(fin, 0.8)
            if y == 31:
                c = shade(c, 0.75)
            px[x, y] = c + (255,)
    # --- extremite (16..24 x 28..32) : couleur moyenne
    for y in range(28, 32):
        for x in range(16, 24):
            px[x, y] = shade(mid, 0.85 + 0.2 * r.random()) + (255,)
    # --- oeil (24..28) : blanc + pupille
    for y in range(28, 32):
        for x in range(24, 28):
            px[x, y] = (245, 245, 235, 255)
    for y in (29, 30):
        for x in (25, 26):
            px[x, y] = (12, 12, 18, 255)
    # --- accent / lueur (28..32)
    for y in range(28, 32):
        for x in range(28, 32):
            px[x, y] = (glow if glow else accent) + (255,)
    return im


# ------------------------------------------------------------------ formes des poissons

def z8(zh):
    return 8 - zh, 8 + zh


class Shape:
    """Un poisson : liste de (role, x0, x1, y0, y1, z0, z1, option)."""

    def __init__(self):
        self.parts = []

    def body(self, x0, x1, y0, y1, zh, **kw):
        z0, z1 = z8(zh)
        self.parts.append(('body', x0, x1, y0, y1, z0, z1, kw))

    def fin(self, x0, x1, y0, y1, zh=0.2, zc=8.0, **kw):
        self.parts.append(('fin', x0, x1, y0, y1, zc - zh, zc + zh, kw))

    def eyes(self, x0, x1, y0, y1, zside):
        for s in (-1, 1):
            z = 8 + s * zside
            self.parts.append(('eye', x0, x1, y0, y1, min(z, z + s * 0.25), max(z, z + s * 0.25), {}))

    def raw(self, role, x0, x1, y0, y1, z0, z1, **kw):
        self.parts.append((role, x0, x1, y0, y1, z0, z1, kw))


def make_shape(kind):
    s = Shape()
    if kind == 'fuseau':
        s.body(13, 15, 6.5, 8.5, 1.0)
        s.body(10.5, 13, 5.5, 9.5, 1.6)
        s.body(7, 10.5, 5, 10, 2.0)
        s.body(4, 7, 5.5, 9.5, 1.6)
        s.body(2, 4, 6.5, 8.5, 1.0)
        s.fin(0, 2, 4.5, 10.5)
        s.fin(6, 9.5, 10, 12)
        s.fin(3.5, 6, 3.5, 5.5)
        s.fin(8.5, 10.5, 4.2, 5.5, 0.1, 10.4)
        s.fin(8.5, 10.5, 4.2, 5.5, 0.1, 5.6)
        s.eyes(11.8, 12.7, 7.1, 8.0, 1.6)
    elif kind == 'haut':
        s.body(13, 14.5, 6, 9, 1.0)
        s.body(11, 13, 4.5, 10.5, 1.6)
        s.body(7.5, 11, 3, 12, 2.0)
        s.body(4.5, 7.5, 3.5, 11, 1.7)
        s.body(2.5, 4.5, 6, 9, 1.0)
        s.fin(0.5, 2.5, 4, 11)
        s.fin(5, 11, 12, 13.5)
        s.fin(4, 8, 1.5, 3)
        s.fin(9, 11, 3.2, 5, 0.1, 10.4)
        s.fin(9, 11, 3.2, 5, 0.1, 5.6)
        s.eyes(11.5, 12.4, 7.4, 8.3, 1.6)
    elif kind == 'long':
        s.body(14, 16, 7, 8.4, 0.8)
        s.body(11.5, 14, 6.4, 8.8, 1.2)
        s.body(8, 11.5, 6, 9.2, 1.5)
        s.body(4.5, 8, 6, 9.2, 1.5)
        s.body(2.5, 4.5, 6.4, 8.8, 1.1)
        s.fin(0.5, 2.5, 4.8, 10.4)
        s.fin(3, 6.5, 9.2, 11)
        s.fin(3, 6, 4.4, 6)
        s.fin(9, 10.5, 5, 6, 0.1, 10.0)
        s.fin(9, 10.5, 5, 6, 0.1, 6.0)
        s.eyes(12.4, 13.2, 7.6, 8.4, 1.2)
    elif kind == 'anguille':
        s.body(13.5, 15.5, 7.6, 9.4, 0.9)
        s.body(11, 13.5, 8, 10, 1.0)
        s.body(8.5, 11, 7, 9, 1.0)
        s.body(6, 8.5, 6, 8, 1.0)
        s.body(3.5, 6, 6.6, 8.6, 0.9)
        s.body(1, 3.5, 7.6, 9.4, 0.8)
        s.fin(8.5, 13, 10, 11, 0.1)
        s.fin(5, 8.5, 8, 9, 0.1)
        s.fin(1.5, 5, 8.6, 9.4, 0.1)
        s.eyes(14, 14.8, 8.4, 9.1, 0.9)
    elif kind == 'raie':
        s.raw('flatbody', 4, 12, 7, 8.4, 2.5, 13.5)
        s.raw('flatbody', 12, 14.5, 7, 8.6, 5.5, 10.5)
        s.raw('flatbody', 5, 9, 7.1, 8.2, 0.8, 2.5)
        s.raw('flatbody', 5, 9, 7.1, 8.2, 13.5, 15.2)
        s.raw('flatbody', 0.5, 4, 7.4, 8.2, 7.2, 8.8)
        s.raw('flatbody', -1, 0.5, 7.5, 8.1, 7.6, 8.4)
        s.raw('eye', 12.6, 13.4, 8.6, 9.3, 6.6, 7.4)
        s.raw('eye', 12.6, 13.4, 8.6, 9.3, 8.6, 9.4)
    elif kind == 'silure':
        s.body(12.5, 14.5, 5.2, 8.0, 2.6)
        s.body(9, 12.5, 4.8, 8.8, 2.4)
        s.body(5.5, 9, 5, 8.8, 1.8)
        s.body(2.5, 5.5, 5.6, 8.2, 1.2)
        s.fin(0.5, 2.5, 4.4, 9.4)
        s.fin(5, 8, 8.8, 10)
        s.fin(3.5, 7, 3.8, 5.2)
        s.fin(14.5, 16, 6.0, 6.4, 0.1, 10.0)
        s.fin(14.5, 16, 6.0, 6.4, 0.1, 6.0)
        s.fin(14, 15.5, 5.4, 5.8, 0.1, 8.9)
        s.fin(14, 15.5, 5.4, 5.8, 0.1, 7.1)
        s.eyes(13, 13.8, 7.2, 8.0, 2.6)
    elif kind == 'thon':
        s.body(13, 15, 6, 8.6, 1.2)
        s.body(10.5, 13, 4.8, 9.8, 1.9)
        s.body(7, 10.5, 4.2, 10.4, 2.3)
        s.body(4, 7, 4.8, 9.6, 1.8)
        s.body(2, 4, 6, 8.4, 0.9)
        s.fin(0, 2, 8.2, 11.8)
        s.fin(0, 2, 3.2, 6.2)
        s.fin(6.5, 8.5, 10.4, 12.6)
        s.fin(3.5, 6, 9.6, 10.6)
        s.fin(8, 10.5, 3, 4.2, 0.1, 10.2)
        s.fin(8, 10.5, 3, 4.2, 0.1, 5.8)
        s.eyes(12, 12.8, 7.2, 8.0, 1.9)
    elif kind == 'requin':
        s.body(13.5, 16, 6.2, 8.2, 1.0)
        s.body(10.5, 13.5, 5.2, 9.2, 1.8)
        s.body(7, 10.5, 4.8, 9.8, 2.2)
        s.body(4, 7, 5.2, 9, 1.7)
        s.body(2, 4, 6, 8.2, 1.0)
        s.fin(7.5, 10.5, 9.8, 11.8)
        s.fin(8.2, 9.8, 11.8, 13.6)
        s.fin(0, 2.5, 8, 13)
        s.fin(0.8, 3, 4.2, 6.2)
        s.fin(7.5, 11, 3.2, 4.8, 0.1, 10.8)
        s.fin(7.5, 11, 3.2, 4.8, 0.1, 5.2)
        s.eyes(12.4, 13.0, 7.4, 8.0, 1.8)
    elif kind == 'lanterne':
        s.body(11.5, 13.5, 5.2, 8.6, 1.6)
        s.body(7, 11.5, 3.6, 10.4, 2.3)
        s.body(4, 7, 4.4, 9.4, 1.8)
        s.body(2.5, 4, 5.8, 8, 1.0)
        s.fin(0.5, 2.5, 4.6, 9.2)
        s.fin(5, 8, 10.4, 11.6)
        s.fin(4, 7, 2.6, 3.6)
        s.fin(10.5, 11.5, 10.4, 13.6, 0.12)
        s.fin(11.5, 14.2, 13.4, 14.2, 0.12)
        s.raw('glow', 13.8, 15.2, 12.2, 14.2, 7.2, 8.8)
        s.eyes(11.6, 12.4, 7.0, 7.8, 1.6)
    elif kind == 'etoile':
        c = (8, 8, 8)
        s.raw('body', 6.5, 9.5, 6.5, 9.5, 7.1, 8.9)
        for ang, (x0, x1, y0, y1) in (
                (0, (7.2, 8.8, 9.2, 14.2)), (0, (7.2, 8.8, 1.8, 6.8)), (0, (9.2, 14.2, 7.2, 8.8)), (0, (1.8, 6.8, 7.2, 8.8)),
                (45, (7.2, 8.8, 9.2, 13.4)), (45, (7.2, 8.8, 2.6, 6.8)), (-45, (7.2, 8.8, 9.2, 13.4)), (-45, (7.2, 8.8, 2.6, 6.8))):
            s.raw('body', x0, x1, y0, y1, 7.3, 8.7, rot=('z', ang, c))
        s.raw('glow', 7.4, 8.6, 7.4, 8.6, 8.8, 9.2)
        s.raw('glow', 7.4, 8.6, 7.4, 8.6, 6.8, 7.2)
        s.raw('eye', 7.0, 7.6, 7.8, 8.4, 8.9, 9.2)
        s.raw('eye', 8.4, 9.0, 7.8, 8.4, 8.9, 9.2)
    elif kind == 'kraken':
        s.body(5, 11, 9, 13, 2.8)
        s.body(6, 10, 13, 15.4, 2.2)
        s.body(4.5, 11.5, 7.2, 9, 3.0)
        s.eyes(9.5, 10.8, 9.2, 10.4, 2.8)
        o = (8, 7, 8)
        for i, zc in enumerate((4.8, 6.4, 8, 9.6, 11.2)):
            ang = (-22.5, 0, 22.5, 0, -22.5)[i]
            s.raw('body', 6.2 + i * 0.7, 7.2 + i * 0.7, 3.8, 7.3, zc - 0.45, zc + 0.45)
            s.raw('body', 5.4 + i * 0.7 + (0.5 if i % 2 else -0.3), 6.4 + i * 0.7 + (0.5 if i % 2 else -0.3), 0.8, 3.9, zc - 0.4, zc + 0.4)
        s.raw('body', 10.6, 11.6, 3.8, 7.3, 6.0, 7.0)
        s.raw('body', 10.4, 11.4, 1.0, 3.9, 6.0, 7.0)
        s.raw('body', 10.6, 11.6, 3.8, 7.3, 9.0, 10.0)
        s.raw('body', 10.8, 11.8, 1.0, 3.9, 9.0, 10.0)
    else:
        raise ValueError(kind)
    return s


def side_u(x):
    return x * 2.0


def side_v(y):
    return (12.0 - y) * 2.0


def fish_uv(role, opts, topside=False):
    def uv(face, f, t):
        if role == 'eye':
            return [24, 28, 28, 32]
        if role == 'glow':
            return [28, 28, 32, 32]
        if role == 'fin':
            return [f[0], 28, t[0] if t[0] > f[0] + 0.01 else f[0] + 1, 32]
        if role == 'flatbody':
            if face in ('up', 'down'):
                return [max(0, f[0]) * 2, (f[2]) * 20 / 16.0, max(0.5, t[0]) * 2, t[2] * 20 / 16.0]
            return [16, 28, 24, 32]
        # corps
        if face == 'south':
            return [side_u(f[0]), side_v(t[1]), side_u(t[0]), side_v(f[1])]
        if face == 'north':
            return [side_u(t[0]), side_v(t[1]), side_u(f[0]), side_v(f[1])]
        if face == 'up':
            return [side_u(f[0]), 20, side_u(t[0]), 24]
        if face == 'down':
            return [side_u(f[0]), 24, side_u(t[0]), 28]
        return [16, 28, 24, 32]
    return uv


def fish_model(shape_kind):
    sh = make_shape(shape_kind)
    elements = []
    for role, x0, x1, y0, y1, z0, z1, kw in sh.parts:
        rot = kw.get('rot')
        elements.append(G.box(x0, y0, z0, x1, y1, z1, fish_uv(role, kw), rot=rot, px=32))
    return elements


FISH_DISPLAY = {
    'gui': {'rotation': [22, -32, 0], 'translation': [0, 0, 0], 'scale': [1.05, 1.05, 1.05]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.55, 0.55, 0.55]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1.0, 1.0, 1.0]},
    'thirdperson_righthand': {'rotation': [0, -90, 20], 'translation': [0, 2.5, 1], 'scale': [0.6, 0.6, 0.6]},
    'thirdperson_lefthand': {'rotation': [0, 90, -20], 'translation': [0, 2.5, 1], 'scale': [0.6, 0.6, 0.6]},
    'firstperson_righthand': {'rotation': [0, -90, 15], 'translation': [0, 3, 1], 'scale': [0.75, 0.75, 0.75]},
    'firstperson_lefthand': {'rotation': [0, 90, -15], 'translation': [0, 3, 1], 'scale': [0.75, 0.75, 0.75]},
    'head': {'rotation': [0, 0, 0], 'translation': [0, 12, 0], 'scale': [0.9, 0.9, 0.9]},
}


# ------------------------------------------------------------------ modeles a palette (cannes, dechets, ingredients)

class Palette:
    """Texture de 16 couleurs en colonnes : chaque boite pioche une colonne (bruit leger pour eviter l'aplat)."""

    def __init__(self):
        self.colors = []
        self.elements = []

    def col(self, rgb):
        rgb = tuple(rgb)
        if rgb not in self.colors:
            self.colors.append(rgb)
        return self.colors.index(rgb)

    def add(self, rgb, x0, y0, z0, x1, y1, z1, rot=None):
        i = self.col(rgb)
        self.elements.append(G.box(x0, y0, z0, x1, y1, z1, lambda face, f, t, i=i: [i, 0, i + 1, 16], rot=rot))

    def image(self, seed=1):
        r = random.Random(seed)
        if len(self.colors) > 16:
            raise SystemExit('palette de plus de 16 couleurs : %d' % len(self.colors))
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        px = im.load()
        for i, c in enumerate(self.colors):
            for y in range(16):
                px[i, y] = shade(c, 0.93 + 0.14 * r.random()) + (255,)
        return im


def pal_model(p, display, extra_tex=None):
    return {'credit': 'Aeloria · peche', 'textures': {'t': 'PLACEHOLDER', 'particle': '#t'}, 'elements': p.elements, 'display': display}


ROD_DISPLAY = {
    'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    'thirdperson_righthand': {'rotation': [0, 90, 55], 'translation': [0, 4.0, 2.5], 'scale': [0.85, 0.85, 0.85]},
    'thirdperson_lefthand': {'rotation': [0, -90, -55], 'translation': [0, 4.0, 2.5], 'scale': [0.85, 0.85, 0.85]},
    'firstperson_righthand': {'rotation': [0, 90, 25], 'translation': [0, 1.6, 0.8], 'scale': [0.68, 0.68, 0.68]},
    'firstperson_lefthand': {'rotation': [0, -90, -25], 'translation': [0, 1.6, 0.8], 'scale': [0.68, 0.68, 0.68]},
}

# tier : (tige haute, tige basse, noeuds/bagues, guides, poignee, moulinet, pierre/accent, lueur)
ROD_STYLE = {
    1: dict(shaft=(188, 178, 84), shaft2=(160, 156, 70), ring=(82, 110, 40), guide=(150, 150, 140), grip=(150, 106, 60), grip2=(112, 78, 42),
            reel=(128, 92, 56), reel2=(96, 66, 38), gem=(200, 60, 50), glow=None, line=(236, 236, 230)),
    2: dict(shaft=(96, 66, 42), shaft2=(76, 50, 32), ring=(80, 200, 110), guide=(96, 222, 130), grip=(46, 120, 64), grip2=(32, 88, 46),
            reel=(176, 124, 60), reel2=(132, 90, 40), gem=(110, 255, 150), glow=None, line=(236, 236, 230)),
    3: dict(shaft=(190, 180, 160), shaft2=(160, 150, 130), ring=(214, 222, 236), guide=(226, 232, 245), grip=(40, 60, 110), grip2=(28, 42, 80),
            reel=(190, 198, 214), reel2=(140, 148, 170), gem=(250, 248, 240), glow=None, line=(236, 240, 245)),
    4: dict(shaft=(32, 30, 36), shaft2=(24, 22, 28), ring=(255, 150, 40), guide=(255, 176, 60), grip=(150, 40, 30), grip2=(106, 28, 22),
            reel=(60, 56, 64), reel2=(40, 38, 46), gem=(255, 110, 30), glow=(255, 140, 50), line=(255, 226, 190)),
    5: dict(shaft=(150, 214, 240), shaft2=(110, 180, 224), ring=(210, 245, 255), guide=(140, 235, 255), grip=(232, 240, 248), grip2=(186, 202, 218),
            reel=(100, 170, 220), reel2=(70, 130, 190), gem=(170, 250, 255), glow=(150, 245, 255), line=(220, 245, 255)),
    6: dict(shaft=(60, 28, 104), shaft2=(42, 18, 78), ring=(176, 96, 255), guide=(206, 140, 255), grip=(28, 60, 66), grip2=(18, 40, 46),
            reel=(40, 150, 140), reel2=(26, 104, 100), gem=(226, 170, 255), glow=(200, 130, 255), line=(230, 210, 255)),
}


def rod_model(tier, cast):
    st = ROD_STYLE[tier]
    p = Palette()
    O = (8, 8, 8)
    R = ('z', -45, O)

    def add(color, x0, y0, x1, y1, zh, dx=0.0, zc=8.0, rot=R):
        p.add(color, 8 - x0 if False else x0, y0, zc - zh, x1, y1, zc + zh, rot=rot)

    # axe de la canne : x = 8, y de -2,6 (cul) a 18,6 (pointe), tourne de -45 degres
    # poignee et culot
    add(st['grip2'], 7.1, -2.8, 8.9, -1.6, 0.9)
    add(st['grip'], 7.2, -1.6, 8.8, 1.2, 0.8)
    add(st['grip2'], 7.2, 1.2, 8.8, 2.0, 0.8)
    add(st['grip'], 7.2, 2.0, 8.8, 3.6, 0.8)
    # bague porte-moulinet
    add(st['ring'], 6.9, 3.6, 9.1, 4.4, 0.95)
    # moulinet (de cote, vers +x)
    add(st['reel2'], 8.8, 1.8, 11.0, 4.0, 0.35)
    add(st['reel'], 9.0, 2.0, 10.8, 3.8, 0.55)
    add(st['reel2'], 9.0, 2.0, 10.8, 3.8, 0.9, zc=8.0) if False else None
    add(st['gem'], 9.6, 2.6, 10.2, 3.2, 0.75)
    add(st['reel2'], 10.8, 2.6, 11.6, 3.2, 0.3)       # manivelle
    add(st['ring'], 11.4, 2.2, 12.0, 3.6, 0.3)
    # tige : trois sections qui s'affinent
    add(st['shaft'], 7.4, 4.4, 8.6, 10.0, 0.6)
    add(st['shaft2'], 7.55, 10.0, 8.45, 15.0, 0.45)
    add(st['shaft'], 7.7, 15.0, 8.3, 18.6, 0.3)
    # bagues decoratives
    for y in (6.0, 8.4, 12.4):
        add(st['ring'], 7.3, y, 8.7, y + 0.4, 0.68 if y < 10 else 0.52)
    # guides de ligne
    for y in (7.2, 10.6, 13.8, 16.6):
        add(st['guide'], 8.3, y, 9.3, y + 0.5, 0.25)
        add(st['guide'], 8.9, y - 0.3, 9.3, y + 0.8, 0.25)
    # pointe
    add(st['gem'], 7.6, 18.6, 8.4, 19.3, 0.3)
    if tier >= 4:
        add(st['glow'] or st['gem'], 7.65, 19.3, 8.35, 19.9, 0.22)
    if tier == 6:
        # ecaille de Leviathan sur la poignee et runes lumineuses sur la tige
        add(st['reel'], 6.8, 0.0, 7.2, 1.2, 0.5)
        add(st['reel'], 8.8, 0.0, 9.2, 1.2, 0.5)
        for y in (5.0, 9.0, 13.0):
            add(st['glow'], 7.3, y, 8.7, y + 0.25, 0.62 if y < 10 else 0.5)
    if tier == 5:
        for y in (5.4, 9.2, 13.2):
            add(st['gem'], 8.4, y, 9.2, y + 1.0, 0.2, rot=('z', -22.5, O) if False else R)
    if tier == 2:
        add(st['gem'], 7.7, 3.6, 8.3, 4.2, 1.0)
    if tier == 3:
        add((250, 248, 240), 7.4, 3.6, 8.6, 4.2, 1.0)    # perle sur la bague
    if tier == 1:
        for y in (6.0, 12.0):
            add(st['shaft2'], 7.25, y - 0.3, 8.75, y + 0.3, 0.72 if y < 10 else 0.55)
    # fil : pointe -> bouchon (lance) ou petite boucle (range)
    tip = (8 + 0.7071 * 18.9 - 0.7071 * 0.0 - 0.0, 8 + 0.7071 * (18.9 - 8) * 1.0)
    # position monde de la pointe : (x, y) = 8 + 0.7071 * (y_local - 8) pour les deux axes
    wx = 8 + 0.7071 * (19.0 - 8)
    wy = 8 + 0.7071 * (19.0 - 8)
    if cast:
        p.add(st['line'], wx - 0.12, 5.6, 7.9, wx + 0.12, wy, 8.1)
        p.add((235, 60, 50), wx - 0.5, 5.0, 7.5, wx + 0.5, 5.7, 8.5)
        p.add((250, 250, 250), wx - 0.5, 4.4, 7.5, wx + 0.5, 5.0, 8.5)
        p.add(st['gem'] if st['glow'] else (255, 255, 255), wx - 0.25, 4.1, 7.75, wx + 0.25, 4.4, 8.25)
    else:
        p.add(st['line'], wx - 0.1, wy - 2.6, 7.9, wx + 0.1, wy, 8.1)
        p.add((190, 190, 200), wx - 0.3, wy - 3.4, 7.7, wx + 0.3, wy - 2.6, 8.3)
    return p


def junk_model(jid):
    p = Palette()
    if jid == 'botte_rouillee':
        p.add((96, 62, 36), 4, 1, 5, 9, 11, 11)           # tige
        p.add((120, 78, 44), 4, 1, 5, 12, 4, 11)          # semelle + pied
        p.add((76, 46, 28), 3.6, 0.2, 4.6, 12.4, 1.2, 11.4)
        p.add((140, 100, 60), 3.6, 11, 4.6, 9.4, 12, 11.4)  # bord
        p.add((60, 120, 70), 5, 4, 5.0, 7, 6, 4.6)        # algue collee
        p.add((176, 110, 60), 10, 2, 4.8, 12, 3, 5.0)
        p.add((32, 22, 14), 5, 11, 6, 8, 11.2, 10)        # interieur sombre
    elif jid == 'algues_enchevetrees':
        greens = [(40, 110, 50), (60, 140, 64), (30, 90, 56), (80, 160, 76)]
        for i in range(7):
            x = 4 + i * 1.1
            h = 6 + (i * 5) % 6
            p.add(greens[i % 4], x, 1, 6 + (i % 3) * 1.2, x + 0.9, 1 + h, 6.9 + (i % 3) * 1.2, rot=('z', (-22.5, 0, 22.5)[i % 3], (x, 1, 8)))
        p.add((120, 90, 56), 4, 0.5, 5.5, 12, 1.5, 10.5)
        p.add((176, 90, 90), 6, 2, 5.8, 7, 3, 6.4)
    elif jid == 'filet_dechire':
        p.add((176, 160, 120), 2, 6, 4, 14, 7, 5)
        p.add((176, 160, 120), 2, 6, 4, 3, 13, 5)
        p.add((176, 160, 120), 13, 6, 4, 14, 13, 5)
        p.add((176, 160, 120), 2, 12, 4, 7, 13, 5)
        p.add((176, 160, 120), 9, 12, 4, 14, 13, 5)
        for i in range(1, 5):
            p.add((150, 134, 98), 2 + i * 2.4, 6, 4.2, 2.4 + i * 2.4, 12, 4.8)
        for j in range(1, 3):
            p.add((150, 134, 98), 3, 6 + j * 2, 4.2, 13, 6.4 + j * 2, 4.8)
        p.add((110, 150, 90), 5, 7, 4.9, 8, 9, 5.4)
        p.add((190, 190, 200), 1.6, 5.6, 3.8, 2.6, 6.6, 5.4)
        p.add((190, 190, 200), 13.4, 5.6, 3.8, 14.4, 6.6, 5.4)
    elif jid == 'bouteille_vide':
        p.add((120, 190, 150), 5.5, 1, 5.5, 10.5, 8, 10.5)
        p.add((150, 215, 175), 5.5, 8, 5.5, 10.5, 9, 10.5)
        p.add((120, 190, 150), 6.8, 9, 6.8, 9.2, 13, 9.2)
        p.add((176, 130, 80), 6.5, 13, 6.5, 9.5, 14.6, 9.5)
        p.add((200, 235, 220), 6, 2, 10.6, 7, 7, 10.8)
        p.add((90, 150, 120), 5.4, 0.6, 5.4, 10.6, 1, 10.6)
        p.add((255, 255, 255), 9.4, 4, 5.3, 9.8, 6, 5.5)
    elif jid == 'arete_de_poisson':
        bone = (236, 232, 214)
        shade_b = (200, 196, 178)
        p.add(bone, 1.5, 7.4, 7.4, 12, 8.6, 8.6)            # colonne
        for i in range(5):
            x = 3 + i * 1.8
            p.add(shade_b if i % 2 else bone, x, 4.2, 7.6, x + 0.7, 11.8, 8.4)
        p.add(bone, 12, 6.4, 6.4, 15, 9.6, 9.6)             # crane
        p.add((60, 56, 50), 13.6, 8, 5.9, 14.4, 9, 6.5)
        p.add((60, 56, 50), 13.6, 8, 9.5, 14.4, 9, 10.1)
        p.add(shade_b, 0, 6.6, 7.6, 1.5, 9.4, 8.4)
    else:
        raise ValueError(jid)
    return p


def ingredient_model(iid):
    p = Palette()
    if iid == 'algue_lumineuse':
        for i in range(5):
            x = 5 + i * 1.3
            p.add((46, 150, 120), x, 1, 7, x + 0.9, 9 + (i * 3) % 4, 8.2, rot=('z', (-22.5, 0, 22.5, 0, -22.5)[i], (x, 1, 7.6)))
            p.add((130, 255, 220), x - 0.1, 9 + (i * 3) % 4, 6.9, x + 1.0, 10.2 + (i * 3) % 4, 8.3, rot=('z', (-22.5, 0, 22.5, 0, -22.5)[i], (x, 1, 7.6)))
        p.add((90, 70, 50), 4, 0.4, 6.4, 12, 1.4, 9.4)
    elif iid == 'ecaille_argentee':
        p.add((90, 100, 124), 4, 2, 7.2, 12, 12, 8.8)
        p.add((190, 200, 220), 4.6, 2.6, 8.8, 11.4, 11.4, 9.3)
        p.add((235, 242, 255), 5.6, 4, 9.3, 10.4, 10, 9.7)
        p.add((150, 165, 190), 6.6, 3, 9.7, 9.4, 4, 10.0)
        p.add((255, 255, 255), 6.6, 8, 9.7, 8, 9, 10.0)
    elif iid == 'perle_nacree':
        p.add((240, 232, 226), 5, 5, 5, 11, 11, 11)
        p.add((250, 244, 240), 6, 6, 4.5, 10, 10, 11.5)
        p.add((250, 244, 240), 4.5, 6, 6, 11.5, 10, 10)
        p.add((250, 244, 240), 6, 4.5, 6, 10, 11.5, 10)
        p.add((255, 255, 255), 6.4, 8.6, 10.7, 8, 10, 11.9)
        p.add((226, 200, 230), 8.5, 5.4, 10.7, 10, 6.8, 11.8)
        p.add((150, 130, 150), 4, 3.6, 6, 12, 5, 10)
    elif iid == 'corail_ancien':
        cor = (214, 96, 96)
        dark = (150, 54, 66)
        p.add(dark, 6, 1, 6, 10, 6, 10)
        p.add(cor, 6.4, 6, 6.4, 9.6, 10, 9.6)
        p.add(cor, 3.5, 5, 7, 6.4, 6.6, 9)
        p.add(cor, 2.4, 6.6, 7, 4, 11, 9)
        p.add(cor, 9.6, 5, 7, 12.5, 6.6, 9)
        p.add(cor, 12, 6.6, 7, 13.6, 12, 9)
        p.add((240, 130, 120), 7, 10, 7, 9, 14, 9)
        p.add((255, 190, 170), 2.4, 11, 7.2, 4, 11.8, 8.8)
        p.add((255, 190, 170), 12, 12, 7.2, 13.6, 12.8, 8.8)
        p.add((255, 190, 170), 7.2, 14, 7.2, 8.8, 14.8, 8.8)
    elif iid == 'larme_de_sirene':
        p.add((40, 120, 220), 6.5, 2, 6.5, 9.5, 5, 9.5)
        p.add((70, 160, 240), 5.5, 5, 5.5, 10.5, 8, 10.5)
        p.add((110, 200, 255), 6, 8, 6, 10, 10, 10)
        p.add((170, 230, 255), 7, 10, 7, 9, 12.4, 9)
        p.add((230, 250, 255), 7.5, 12.4, 7.5, 8.5, 14.2, 8.5)
        p.add((255, 255, 255), 6.2, 6.4, 10.5, 7.4, 8.6, 11.0)
        p.add((26, 80, 170), 7, 1, 7, 9, 2, 9)
    elif iid == 'ecaille_de_leviathan':
        p.add((10, 60, 70), 3, 1, 7, 13, 13, 9)
        p.add((20, 110, 120), 3.6, 1.6, 9, 12.4, 12.4, 9.6)
        p.add((40, 160, 160), 4.6, 3, 9.6, 11.4, 11, 10.1)
        p.add((90, 220, 210), 5.6, 4.6, 10.1, 10.4, 9.4, 10.5)
        p.add((160, 255, 240), 7, 5.6, 10.5, 9, 8.4, 10.9)
        p.add((10, 40, 50), 4, 0.6, 7.4, 12, 1.6, 8.6)
        p.add((120, 255, 230), 7.4, 11.4, 9.7, 8.6, 12.6, 10.1)
    else:
        raise ValueError(iid)
    return p


INGREDIENT_DISPLAY = FISH_DISPLAY


# ------------------------------------------------------------------ coffres

CHEST_STYLE = {
    0: dict(wood=((78, 54, 36), (58, 40, 28), (96, 68, 44)), metal=((110, 76, 56), (84, 56, 40), (140, 100, 70)), lock=(150, 110, 70), extra='algae'),
    1: dict(wood=((96, 74, 56), (72, 54, 40), (118, 92, 68)), metal=((72, 70, 78), (52, 50, 58), (100, 98, 108)), lock=(170, 170, 180), extra='coral'),
    2: dict(wood=((104, 56, 36), (78, 40, 26), (132, 74, 46)), metal=((214, 168, 62), (158, 118, 40), (250, 216, 112)), lock=(252, 222, 110), extra='gold'),
    3: dict(wood=((24, 34, 56), (16, 22, 40), (36, 52, 84)), metal=((60, 86, 130), (38, 56, 92), (96, 140, 200)), lock=(120, 235, 255), extra='abyss'),
}


def paint_chest(rank, seed):
    st = CHEST_STYLE[rank]
    r = random.Random(seed)
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = im.load()
    base, dark, light = st['wood']
    # (0..16 x 0..16) planches horizontales
    for y in range(16):
        for x in range(16):
            row = y // 4
            c = [base, dark, base, light][row % 4] if y % 4 != 3 else shade(dark, 0.7)
            c = shade(c, 0.92 + 0.16 * r.random())
            if rank == 3 and r.random() < 0.05:
                c = (60, 120, 190)
            px[x, y] = c + (255,)
    for k in range(5):
        px[r.randrange(16), r.randrange(16)] = shade(dark, 0.6) + (255,)
    # (16..32 x 0..16) metal : bandes avec rivets
    mb, md, ml = st['metal']
    for y in range(16):
        for x in range(16, 32):
            c = shade(mb, 0.92 + 0.16 * r.random())
            if y % 8 == 0 or x == 16 or x == 31:
                c = md
            px[x, y] = c + (255,)
    for x, y in ((18, 3), (29, 3), (18, 11), (29, 11), (23, 7)):
        px[x, y] = ml + (255,)
        px[x + 1, y] = ml + (255,)
    if st['extra'] == 'abyss':
        for x in range(17, 31):
            if x % 3 == 0:
                for y in range(2, 14):
                    px[x, y] = st['lock'] + (255,) if y % 4 else md + (255,)
    if st['extra'] == 'gold':
        for x in range(17, 31):
            px[x, 7] = ml + (255,)
    # (0..16 x 16..32) decor : algues / corail / pierres / lueur
    for y in range(16, 32):
        for x in range(16):
            px[x, y] = shade(dark, 0.8) + (255,)
    if st['extra'] == 'algae':
        for y in range(16, 32):
            for x in range(16):
                if r.random() < 0.55:
                    px[x, y] = shade((46, 110, 60), 0.7 + 0.6 * r.random()) + (255,)
        for _ in range(7):
            px[r.randrange(16), r.randrange(16, 32)] = (220, 210, 190, 255)
    elif st['extra'] == 'coral':
        for y in range(16, 32):
            for x in range(16):
                px[x, y] = shade((222, 100, 110) if (x + y) % 3 else (255, 170, 150), 0.8 + 0.4 * r.random()) + (255,)
    elif st['extra'] == 'gold':
        for y in range(16, 32):
            for x in range(16):
                px[x, y] = shade((250, 220, 110) if (x // 2 + y // 2) % 2 else (214, 168, 62), 0.9 + 0.2 * r.random()) + (255,)
    else:
        for y in range(16, 32):
            for x in range(16):
                px[x, y] = shade((40, 120, 170) if (x + y) % 4 else (120, 235, 255), 0.8 + 0.4 * r.random()) + (255,)
    # (16..32 x 16..32) serrure / cadenas
    for y in range(16, 32):
        for x in range(16, 32):
            px[x, y] = shade(st['lock'], 0.55) + (255,)
    for y in range(18, 30):
        for x in range(19, 29):
            px[x, y] = shade(st['lock'], 0.9 + 0.2 * r.random()) + (255,)
    for y in range(22, 27):
        px[23, y] = (20, 16, 14, 255)
        px[24, y] = (20, 16, 14, 255)
    px[23, 21] = (20, 16, 14, 255)
    px[24, 21] = (20, 16, 14, 255)
    return im


def chest_model(rank):
    """Coffre pose : caisse, couvercle, bandes de metal, serrure et decor propre au rang."""
    E = []

    def wood(face, f, t):
        if face in ('north', 'south'):
            return [f[0], 16 - t[1], t[0], 16 - f[1]]
        if face in ('east', 'west'):
            return [f[2], 16 - t[1], t[2], 16 - f[1]]
        return [f[0], f[2], t[0], t[2]]

    def metal(face, f, t):
        return [16, 0, 16 + max(1, min(16, t[0] - f[0] if face in ('up', 'down', 'north', 'south') else t[2] - f[2])), max(1, min(16, t[1] - f[1])) if face not in ('up', 'down') else 8]

    def deco(face, f, t):
        return [0, 16, 16, 32]

    def lockuv(face, f, t):
        return [19, 18, 29, 30]

    E.append(G.box(2, 1, 3, 14, 9, 13, wood, px=32))                       # caisse
    E.append(G.box(1.8, 9, 2.8, 14.2, 11, 13.2, wood, px=32))              # couvercle bas
    E.append(G.box(2.4, 11, 3.4, 13.6, 12.6, 12.6, wood, px=32))           # couvercle haut (bombe)
    for x in (4.0, 10.0):                                           # bandes de metal
        E.append(G.box(x, 0.9, 2.7, x + 1.8, 9, 13.3, lambda face, f, t: [16, 0, 24, 16], px=32))
        E.append(G.box(x - 0.1, 9, 2.6, x + 1.9, 11.1, 13.4, lambda face, f, t: [16, 0, 24, 16], px=32))
        E.append(G.box(x + 0.1, 11, 3.3, x + 1.7, 12.7, 12.7, lambda face, f, t: [16, 0, 24, 16], px=32))
    E.append(G.box(1.7, 0.9, 2.7, 14.3, 2.0, 13.3, lambda face, f, t: [16, 0, 32, 4], px=32))   # socle
    E.append(G.box(7, 7.2, 13.2, 9, 10.6, 13.9, lockuv, px=32))            # serrure
    if rank == 0:       # algues et bernaches
        for x, z, h in ((3, 3, 4), (12.5, 4, 3), (13, 12, 5)):
            E.append(G.box(x, 12.5, z, x + 1, 12.5 + h * 0.35, z + 1, deco, px=32))
        for x, y in ((2.2, 5), (14.1, 3), (5, 0.5)):
            E.append(G.box(x, y, 6, x + 0.6, y + 1.4, 7.4, deco, px=32))
    elif rank == 1:     # branches de corail
        for x, z, h in ((3, 4, 3.5), (6, 10, 2.5), (11, 5, 4), (12.5, 11, 2.8)):
            E.append(G.box(x, 12.6, z, x + 1.2, 12.6 + h, z + 1.2, deco, px=32))
            E.append(G.box(x - 0.8, 12.6 + h * 0.6, z + 0.2, x + 0.4, 12.6 + h * 0.6 + 1, z + 1.0, deco, px=32))
        E.append(G.box(14.1, 2, 4, 14.8, 8, 9, deco, px=32))
    elif rank == 2:     # coins en or, perle et crane
        for x in (1.6, 13.4):
            for z in (2.6, 12.4):
                E.append(G.box(x, 0.9, z, x + 1, 11.2, z + 1, lambda face, f, t: [16, 0, 24, 16], px=32))
        E.append(G.box(6.4, 12.6, 6.4, 9.6, 14.2, 9.6, deco, px=32))
        E.append(G.box(7, 14.2, 7, 9, 15.2, 9, lambda face, f, t: [28, 20, 30, 22], px=32))
    else:               # pointes, chaines et cristaux
        for x in (3.2, 12.2):
            for z in (3.6, 11.4):
                E.append(G.box(x, 12.6, z, x + 0.8, 15, z + 0.8, deco, px=32))
        E.append(G.box(0.8, 5, 6, 1.8, 6, 10, lambda face, f, t: [16, 0, 32, 4], px=32))
        E.append(G.box(14.2, 5, 6, 15.2, 6, 10, lambda face, f, t: [16, 0, 32, 4], px=32))
        E.append(G.box(7, 12.6, 6, 9, 14.4, 10, deco, px=32))
    return E


CHEST_DISPLAY = {
    'gui': {'rotation': [24, -38, 0], 'translation': [0, -0.5, 0], 'scale': [0.98, 0.98, 0.98]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.95, 0.95, 0.95]},
    'thirdperson_righthand': {'rotation': [20, 45, 0], 'translation': [0, 2.5, 2], 'scale': [0.55, 0.55, 0.55]},
    'thirdperson_lefthand': {'rotation': [20, -45, 0], 'translation': [0, 2.5, 2], 'scale': [0.55, 0.55, 0.55]},
    'firstperson_righthand': {'rotation': [10, 50, 0], 'translation': [0, 2, 2], 'scale': [0.7, 0.7, 0.7]},
    'firstperson_lefthand': {'rotation': [10, -50, 0], 'translation': [0, 2, 2], 'scale': [0.7, 0.7, 0.7]},
}


# ------------------------------------------------------------------ ecriture

def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(json.dumps(data, ensure_ascii=False, indent=2))


def write_item_def(item_id, model_id=None):
    write_json(os.path.join(ASSETS, 'items', item_id + '.json'),
               {'model': {'type': 'minecraft:model', 'model': 'mmorpg:item/' + (model_id or item_id)}})


def save_model(item_id, elements, display, tex_name=None, gui_light='side'):
    model = {'credit': 'Aeloria · pêche', 'gui_light': gui_light, 'textures': {'t': 'mmorpg:item/' + (tex_name or item_id), 'particle': '#t'},
             'elements': elements, 'display': display}
    bad = G.check_bounds(elements)
    if bad:
        raise SystemExit('%s : coordonnees hors [-16, 32] : %s' % (item_id, bad[:3]))
    write_json(os.path.join(ASSETS, 'models', 'item', (tex_name or item_id) + '.json'), model)
    return model


def main():
    preview = '--preview' in sys.argv
    fish, junk, chests, ingredients, rods = read_catalog()
    if not (len(fish) >= 25 and len(junk) == 5 and len(chests) == 4 and len(ingredients) == 6 and len(rods) == 6):
        raise SystemExit('catalogue incomplet : poissons %d, dechets %d, coffres %d, ingredients %d, cannes %d' %
                         (len(fish), len(junk), len(chests), len(ingredients), len(rods)))
    tex_dir = os.path.join(ASSETS, 'textures', 'item')
    os.makedirs(tex_dir, exist_ok=True)
    fr, en = {}, {}
    sheets = {'poissons': [], 'cannes': [], 'objets': [], 'coffres': []}

    def keep(group, item_id, model, textures):
        if preview:
            sheets[group].append((item_id, model, textures))

    for i, fi in enumerate(fish):
        im = paint_fish(fi['id'], 1000 + i)
        im.save(os.path.join(tex_dir, fi['id'] + '.png'))
        model = save_model(fi['id'], fish_model(fi['shape']), FISH_DISPLAY)
        write_item_def(fi['id'])
        fr['item.mmorpg.' + fi['id']] = fi['fr']
        en['item.mmorpg.' + fi['id']] = fi['en']
        keep('poissons', fi['id'], model, {'t': im})
    for j in junk:
        p = junk_model(j['id'])
        im = p.image(seed=zlib.crc32(j["id"].encode()) & 0xFFFF)
        im.save(os.path.join(tex_dir, j['id'] + '.png'))
        model = save_model(j['id'], p.elements, FISH_DISPLAY)
        write_item_def(j['id'])
        fr['item.mmorpg.' + j['id']] = j['fr']
        en['item.mmorpg.' + j['id']] = j['en']
        keep('objets', j['id'], model, {'t': im})
    for ing in ingredients:
        p = ingredient_model(ing['id'])
        im = p.image(seed=zlib.crc32(ing["id"].encode()) & 0xFFFF)
        im.save(os.path.join(tex_dir, ing['id'] + '.png'))
        model = save_model(ing['id'], p.elements, INGREDIENT_DISPLAY)
        write_item_def(ing['id'])
        fr['item.mmorpg.' + ing['id']] = ing['fr']
        en['item.mmorpg.' + ing['id']] = ing['en']
        keep('objets', ing['id'], model, {'t': im})
    for c in chests:
        im = paint_chest(c['rank'], 77 + c['rank'])
        im.save(os.path.join(tex_dir, c['id'] + '.png'))
        model = save_model(c['id'], chest_model(c['rank']), CHEST_DISPLAY)
        write_item_def(c['id'])
        fr['item.mmorpg.' + c['id']] = c['fr']
        en['item.mmorpg.' + c['id']] = c['en']
        keep('coffres', c['id'], model, {'t': im})
    for rd in rods:
        models = {}
        for cast in (False, True):
            p = rod_model(rd['tier'], cast)
            name = rd['id'] + ('_lancee' if cast else '')
            im = p.image(seed=rd['tier'])
            im.save(os.path.join(tex_dir, name + '.png'))
            models[cast] = save_model(name, p.elements, ROD_DISPLAY, gui_light='front')
            keep('cannes', name, models[cast], {'t': im})
        write_json(os.path.join(ASSETS, 'items', rd['id'] + '.json'), {'model': {
            'type': 'minecraft:condition', 'property': 'minecraft:fishing_rod/cast',
            'on_false': {'type': 'minecraft:model', 'model': 'mmorpg:item/' + rd['id']},
            'on_true': {'type': 'minecraft:model', 'model': 'mmorpg:item/' + rd['id'] + '_lancee'}}})
        fr['item.mmorpg.' + rd['id']] = rd['fr']
        en['item.mmorpg.' + rd['id']] = rd['en']
    gen_mine.merge_lang(os.path.join(ASSETS, 'lang', 'fr_fr.json'), fr)
    gen_mine.merge_lang(os.path.join(ASSETS, 'lang', 'en_us.json'), en)
    print('peche : %d poissons, %d dechets, %d ingredients, %d coffres, %d cannes' % (len(fish), len(junk), len(ingredients), len(chests), len(rods)))
    if preview:
        out = os.path.join(ROOT, 'docs', 'screens', 'peche')
        os.makedirs(out, exist_ok=True)
        for group, items in sheets.items():
            if not items:
                continue
            cols = 6
            cell = 192
            rows = (len(items) + cols - 1) // cols
            sheet = Image.new('RGBA', (cols * cell, rows * cell), (40, 44, 54, 255))
            for n, (iid, model, textures) in enumerate(items):
                gui = model['display']['gui']['rotation']
                img = G.render_model(model, textures, size=cell, gui=tuple(gui), scale=model['display']['gui']['scale'][0] * 0.95)
                sheet.paste(img, ((n % cols) * cell, (n // cols) * cell))
            sheet.convert('RGB').save(os.path.join(out, 'apercu_%s.png' % group))
        print('apercus ecrits dans', out)


if __name__ == '__main__':
    main()
