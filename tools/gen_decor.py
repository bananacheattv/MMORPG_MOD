# -*- coding: utf-8 -*-
"""Blocs de decor « Aeloria » (marbre, pierre azur, cristaux, tuiles, bois ancien...) : textures peintes par code dans une palette
douce et coherente, modeles, etats, objets, tables de butin, etiquettes et traductions.

La liste MATERIALS doit rester identique a celle de com.mmorpg.registry.DecorBlocks (Java).
Appele par tools/gen_hub.py ; lancer ce fichier seul genere un planche d'apercu (apercu_decor.png).
"""
import json
import math
import os
import random

from PIL import Image

MODID = 'mmorpg'

# id, nom fr, nom en, variantes (marches + dalle), colonne (axe)
MATERIALS = [
    ('marbre', "Marbre d'Aeloria", 'Aeloria Marble', True, False),
    ('marbre_poli', 'Marbre poli', 'Polished Marble', True, False),
    ('marbre_dore', 'Marbre doré', 'Gilded Marble', False, False),
    ('colonne_marbre', 'Colonne de marbre', 'Marble Column', False, True),
    ('pierre_azur', 'Pierre azur', 'Azure Stone', True, False),
    ('pierre_solaire', 'Pierre solaire', 'Sunstone Bricks', True, False),
    ('dalle_runique', 'Dalle runique', 'Runic Slab', False, False),
    ('cristal_azur', 'Cristal azur', 'Azure Crystal', False, False),
    ('cristal_arcane', 'Cristal arcanique', 'Arcane Crystal', False, False),
    ('cristal_ambre', "Cristal d'ambre", 'Amber Crystal', False, False),
    ('tuile_azur', 'Tuile azur', 'Azure Roof Tile', True, False),
    ('tuile_pourpre', 'Tuile pourpre', 'Purple Roof Tile', True, False),
    ('tuile_ecarlate', 'Tuile écarlate', 'Scarlet Roof Tile', True, False),
    ('tuile_sylve', 'Tuile sylvestre', 'Sylvan Roof Tile', True, False),
    ('tuile_givre', 'Tuile de givre', 'Frost Roof Tile', True, False),
    ('bois_ancien', 'Bois ancien', 'Ancient Wood', True, False),
    ('or_ouvrage', 'Or ouvragé', 'Wrought Gold', True, False),
    ('roche_celeste', 'Roche céleste', 'Sky Rock', True, False),
    ('fer_noirci', 'Fer noirci', 'Blackened Iron', True, False),
]

S = 16


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def add(c, d):
    return tuple(max(0, min(255, c[i] + d)) for i in range(3))


class Tex:
    def __init__(self, seed, base=(128, 128, 128)):
        self.rng = random.Random(seed)
        self.img = Image.new('RGBA', (S, S), base + (255,))

    def put(self, x, y, c):
        if 0 <= x < S and 0 <= y < S:
            self.img.putpixel((x, y), tuple(c) + (255,))

    def get(self, x, y):
        return self.img.getpixel((x % S, y % S))[:3]

    def fill(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, c)

    def noise(self, amount):
        for y in range(S):
            for x in range(S):
                self.put(x, y, add(self.get(x, y), self.rng.randint(-amount, amount)))

    def bevel(self, light=14, dark=16):
        for i in range(S):
            self.put(i, 0, add(self.get(i, 0), light))
            self.put(0, i, add(self.get(0, i), light))
            self.put(i, S - 1, add(self.get(i, S - 1), -dark))
            self.put(S - 1, i, add(self.get(S - 1, i), -dark))

    def walk(self, x, y, length, c, halo=None, drift=0.55):
        dx, dy = self.rng.choice([(1, 0), (0, 1), (1, 1), (1, -1)])
        for _ in range(length):
            if halo:
                for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if (x + ox, y + oy) != (x, y):
                        px = self.get(x + ox, y + oy)
                        self.put(x + ox, y + oy, lerp(px, halo, 0.35))
            self.put(x, y, c)
            if self.rng.random() < drift:
                dx, dy = self.rng.choice([(1, 0), (0, 1), (1, 1), (1, -1), (-1, 1)])
            x = (x + dx) % S
            y = (y + dy) % S


def marbre(name, seed):
    t = Tex(seed, (236, 233, 226))
    for y in range(S):
        for x in range(S):
            g = 0.5 + 0.5 * math.sin((x * 0.6 + y * 0.9) + math.sin(y * 0.7) * 1.5)
            t.put(x, y, lerp((238, 236, 230), (224, 224, 226), g * 0.55))
    for _ in range(3):
        t.walk(t.rng.randrange(S), t.rng.randrange(S), 20, (172, 182, 198), halo=(205, 210, 218))
    for _ in range(4):
        t.put(t.rng.randrange(S), t.rng.randrange(S), (226, 192, 112))
    t.noise(3)
    return t


def marbre_poli(name, seed):
    t = Tex(seed)
    tones = [(242, 240, 234), (234, 232, 226), (230, 228, 224), (240, 238, 232)]
    k = 0
    for ty in range(2):
        for tx in range(2):
            c = tones[k]
            k += 1
            t.fill(tx * 8, ty * 8, tx * 8 + 7, ty * 8 + 7, c)
            for i in range(8):
                t.put(tx * 8 + i, ty * 8, add(c, 10))
                t.put(tx * 8, ty * 8 + i, add(c, 10))
                t.put(tx * 8 + i, ty * 8 + 7, add(c, -26))
                t.put(tx * 8 + 7, ty * 8 + i, add(c, -26))
            t.put(tx * 8 + 3, ty * 8 + 3, add(c, -9))
            t.put(tx * 8 + 4, ty * 8 + 4, add(c, 8))
    t.noise(2)
    return t


def marbre_dore(name, seed):
    t = marbre(name, seed)
    gold = (226, 184, 84)
    for i in range(S):
        for w in range(2):
            lt = lerp((246, 214, 120), (190, 140, 52), (i % 8) / 8)
            t.put(i, w, gold if w else lt)
            t.put(i, S - 1 - w, lt if w else gold)
            t.put(w, i, gold if w else lt)
            t.put(S - 1 - w, i, lt if w else gold)
    for a in range(8):
        ang = a * math.pi / 4
        for r in range(1, 5 if a % 2 == 0 else 3):
            t.put(int(round(7.5 + math.cos(ang) * r)), int(round(7.5 + math.sin(ang) * r)), (240, 204, 104))
    t.fill(7, 7, 8, 8, (255, 232, 150))
    return t


def colonne_side(name, seed):
    t = Tex(seed)
    for x in range(S):
        f = (x % 4)
        c = [(244, 242, 236), (236, 234, 228), (224, 222, 216), (214, 212, 208)][f]
        for y in range(S):
            t.put(x, y, c)
    for y in range(S):
        if y in (0, 1, S - 2, S - 1):
            for x in range(S):
                t.put(x, y, add(t.get(x, y), 8 if y < 2 else -14))
    t.noise(2)
    return t


def colonne_top(name, seed):
    t = Tex(seed, (236, 234, 228))
    for y in range(S):
        for x in range(S):
            r = math.hypot(x - 7.5, y - 7.5)
            if r > 7.4:
                t.put(x, y, (212, 210, 206))
            elif r > 6:
                t.put(x, y, (226, 190, 100))
            elif r > 4.5:
                t.put(x, y, (240, 238, 232))
            else:
                t.put(x, y, lerp((230, 228, 222), (248, 246, 240), 1 - r / 5))
    return t


def bricks(seed, cols, mortar, rows=4, wide=8, hl=14):
    t = Tex(seed, mortar)
    h = S // rows
    for r in range(rows):
        off = (wide // 2) if r % 2 else 0
        for bx in range(-1, S // wide + 1):
            x0 = bx * wide + off
            c = t.rng.choice(cols)
            c = add(c, t.rng.randint(-5, 5))
            for y in range(r * h + 1, r * h + h):
                for x in range(x0 + 1, x0 + wide):
                    t.put(x, y, c)
            for x in range(x0 + 1, x0 + wide):
                t.put(x, r * h + 1, add(c, hl))
                t.put(x, r * h + h - 1, add(c, -hl))
    t.noise(3)
    return t


def pierre_azur(name, seed):
    return bricks(seed, [(122, 152, 190), (110, 140, 180), (132, 160, 196), (116, 146, 184)], (72, 92, 124))


def pierre_solaire(name, seed):
    return bricks(seed, [(228, 202, 150), (218, 190, 136), (236, 212, 164), (222, 196, 142)], (156, 124, 80))


def dalle_runique(name, seed):
    t = Tex(seed, (44, 50, 70))
    for ty in range(2):
        for tx in range(2):
            c = add((50, 56, 78), t.rng.randint(-5, 5))
            t.fill(tx * 8, ty * 8, tx * 8 + 7, ty * 8 + 7, c)
            for i in range(8):
                t.put(tx * 8 + i, ty * 8, add(c, 14))
                t.put(tx * 8, ty * 8 + i, add(c, 14))
                t.put(tx * 8 + i, ty * 8 + 7, add(c, -22))
                t.put(tx * 8 + 7, ty * 8 + i, add(c, -22))
    glyphs = ["#.#/.#./#.#", ".#./###/.#.", "##./#.#/#..", "#../###/..#"]
    k = 0
    for ty in range(2):
        for tx in range(2):
            g = glyphs[(k + seed) % len(glyphs)].split('/')
            k += 1
            ox, oy = tx * 8 + 2, ty * 8 + 2
            for yy, row in enumerate(g):
                for xx, ch in enumerate(row):
                    if ch == '#':
                        for ax, ay in ((xx + 1, yy), (xx - 1, yy), (xx, yy + 1), (xx, yy - 1)):
                            px = t.get(ox + ax, oy + ay)
                            t.put(ox + ax, oy + ay, lerp(px, (60, 130, 170), 0.35))
            for yy, row in enumerate(g):
                for xx, ch in enumerate(row):
                    if ch == '#':
                        t.put(ox + xx, oy + yy, (132, 236, 255))
    return t


def crystal(seed, deep, mid, light):
    t = Tex(seed, mid)
    for y in range(S):
        for x in range(S):
            f = (x + y) / (2 * S - 2)
            t.put(x, y, lerp(light, deep, f))
    for _ in range(3):
        x0, y0 = t.rng.randrange(2, S - 2), t.rng.randrange(2, S - 2)
        ang = t.rng.choice([(1, 1), (1, -1), (1, 0), (0, 1)])
        for k in range(t.rng.randint(5, 9)):
            t.put(x0 + ang[0] * k, y0 + ang[1] * k, add(deep, -18))
            t.put(x0 + ang[0] * k + 1, y0 + ang[1] * k, add(light, 14))
    for _ in range(6):
        x, y = t.rng.randrange(S), t.rng.randrange(S)
        t.put(x, y, (255, 255, 255))
    for i in range(S):
        for (x, y) in ((i, 0), (0, i), (i, S - 1), (S - 1, i)):
            t.put(x, y, add(deep, -22))
    for i in range(1, S - 1):
        t.put(i, 1, add(t.get(i, 1), 22))
        t.put(1, i, add(t.get(1, i), 22))
    return t


def tuile(seed, base, dark, light):
    t = Tex(seed, dark)
    rows = 4
    h = S // rows
    for r in range(rows):
        off = 4 if r % 2 else 0
        for bx in range(-1, 3):
            x0 = bx * 8 + off
            for yy in range(h):
                for xx in range(8):
                    # ecaille arrondie : le bas de chaque tuile est un arc
                    w = 3.5 + 0.6 * yy
                    inside = abs(xx - 3.5) <= w
                    if not inside:
                        continue
                    f = yy / (h - 1)
                    c = lerp(light, base, min(1, f * 1.1))
                    if yy == h - 1:
                        c = dark
                    t.put(x0 + xx, r * h + yy, c)
            for xx in range(2, 6):
                t.put(x0 + xx, r * h, add(light, 10))
    t.noise(3)
    return t


def bois_ancien(name, seed):
    t = Tex(seed, (88, 60, 44))
    for x in range(S):
        board = x // 4
        base = [(96, 66, 48), (86, 58, 42), (102, 72, 52), (90, 62, 46)][board]
        for y in range(S):
            g = math.sin(y * 0.9 + board * 2.1 + math.sin(x * 0.7) * 0.8)
            t.put(x, y, add(base, int(g * 5)))
        if x % 4 == 0:
            for y in range(S):
                t.put(x, y, (58, 38, 28))
        if x % 4 == 1:
            for y in range(S):
                t.put(x, y, add(t.get(x, y), 8))
    for b in range(4):
        t.put(b * 4 + 2, 1, (228, 190, 96))
        t.put(b * 4 + 2, S - 2, (228, 190, 96))
    t.noise(3)
    return t


def or_ouvrage(name, seed):
    t = Tex(seed, (236, 196, 84))
    for y in range(S):
        for x in range(S):
            d = abs((x % 8) - 4) + abs((y % 8) - 4)
            g = 0.5 + 0.5 * math.sin(d * 0.9)
            t.put(x, y, lerp((248, 216, 112), (214, 164, 56), g))
    for y in range(S):
        for x in range(S):
            d = abs((x % 8) - 4) + abs((y % 8) - 4)
            if d == 4:
                t.put(x, y, (172, 120, 36))
    for i in range(S):
        t.put(i, 0, (255, 236, 160))
        t.put(0, i, (255, 236, 160))
        t.put(i, S - 1, (160, 108, 30))
        t.put(S - 1, i, (160, 108, 30))
    return t


def roche_celeste(name, seed):
    t = Tex(seed, (112, 112, 124))
    for y in range(S):
        for x in range(S):
            v = math.sin(x * 0.8 + math.sin(y * 0.6) * 2) * math.cos(y * 0.7 + math.cos(x * 0.5) * 2)
            t.put(x, y, add((112, 114, 126), int(v * 14)))
    for _ in range(3):
        t.walk(t.rng.randrange(S), t.rng.randrange(S), 14, (66, 66, 80), halo=(92, 92, 106))
    for _ in range(14):
        x, y = t.rng.randrange(S), t.rng.randrange(S)
        t.put(x, y, (96, 142, 84) if t.rng.random() < 0.7 else (86, 130, 74))
    for _ in range(4):
        t.put(t.rng.randrange(S), t.rng.randrange(S), (122, 214, 255))
    t.noise(4)
    return t


def fer_noirci(name, seed):
    t = Tex(seed, (60, 62, 72))
    for ty in range(2):
        for tx in range(2):
            c = add((64, 66, 78), t.rng.randint(-4, 4))
            t.fill(tx * 8, ty * 8, tx * 8 + 7, ty * 8 + 7, c)
            for i in range(8):
                t.put(tx * 8 + i, ty * 8, add(c, 22))
                t.put(tx * 8, ty * 8 + i, add(c, 22))
                t.put(tx * 8 + i, ty * 8 + 7, add(c, -24))
                t.put(tx * 8 + 7, ty * 8 + i, add(c, -24))
            for (rx, ry) in ((1, 1), (6, 1), (1, 6), (6, 6)):
                t.put(tx * 8 + rx, ty * 8 + ry, (150, 154, 168))
            t.put(tx * 8 + 4, ty * 8 + 3, (176, 70, 52))
    t.noise(2)
    return t


PAINTERS = {
    'marbre': marbre, 'marbre_poli': marbre_poli, 'marbre_dore': marbre_dore,
    'pierre_azur': pierre_azur, 'pierre_solaire': pierre_solaire, 'dalle_runique': dalle_runique,
    'cristal_azur': lambda n, s: crystal(s, (40, 136, 196), (96, 200, 240), (190, 244, 255)),
    'cristal_arcane': lambda n, s: crystal(s, (104, 64, 176), (160, 112, 228), (226, 200, 255)),
    'cristal_ambre': lambda n, s: crystal(s, (196, 104, 24), (244, 172, 56), (255, 232, 160)),
    'tuile_azur': lambda n, s: tuile(s, (64, 172, 168), (30, 104, 112), (140, 226, 214)),
    'tuile_pourpre': lambda n, s: tuile(s, (134, 98, 196), (74, 50, 120), (200, 168, 244)),
    'tuile_ecarlate': lambda n, s: tuile(s, (200, 70, 66), (112, 32, 40), (246, 140, 120)),
    'tuile_sylve': lambda n, s: tuile(s, (92, 156, 84), (44, 90, 52), (170, 214, 130)),
    'tuile_givre': lambda n, s: tuile(s, (150, 196, 236), (84, 128, 186), (234, 246, 255)),
    'bois_ancien': bois_ancien, 'or_ouvrage': or_ouvrage, 'roche_celeste': roche_celeste, 'fer_noirci': fer_noirci,
}


# ------------------------------------------------------------------ etats, modeles, objets
def stairs_state(base):
    variants = {}
    table = {
        ('bottom', 'east'): (0, {'straight': 0, 'outer_right': 0, 'outer_left': 270, 'inner_right': 0, 'inner_left': 270}),
        ('bottom', 'south'): (0, {'straight': 90, 'outer_right': 90, 'outer_left': 0, 'inner_right': 90, 'inner_left': 0}),
        ('bottom', 'west'): (0, {'straight': 180, 'outer_right': 180, 'outer_left': 90, 'inner_right': 180, 'inner_left': 90}),
        ('bottom', 'north'): (0, {'straight': 270, 'outer_right': 270, 'outer_left': 180, 'inner_right': 270, 'inner_left': 180}),
        ('top', 'east'): (180, {'straight': 0, 'outer_right': 90, 'outer_left': 0, 'inner_right': 90, 'inner_left': 0}),
        ('top', 'south'): (180, {'straight': 90, 'outer_right': 180, 'outer_left': 90, 'inner_right': 180, 'inner_left': 90}),
        ('top', 'west'): (180, {'straight': 180, 'outer_right': 270, 'outer_left': 180, 'inner_right': 270, 'inner_left': 180}),
        ('top', 'north'): (180, {'straight': 270, 'outer_right': 0, 'outer_left': 270, 'inner_right': 0, 'inner_left': 270}),
    }
    for (half, facing), (x, shapes) in table.items():
        for shape, y in shapes.items():
            model = base + ('_stairs' if shape == 'straight' else '_stairs_outer' if shape.startswith('outer') else '_stairs_inner')
            v = {'model': f'{MODID}:block/{model}'}
            if x:
                v['x'] = x
            if y:
                v['y'] = y
            if x or y:
                v['uvlock'] = True
            variants[f'facing={facing},half={half},shape={shape}'] = v
    return {'variants': variants}


def generate(ga):
    assets = ga.ASSETS
    data = ga.DATA
    sheet = Image.new('RGBA', (S * 8, S * 3), (24, 24, 32, 255))
    idx = 0
    mineable = []
    for mid, fr, en, variants, pillar in MATERIALS:
        seed = sum(ord(c) * (i + 1) for i, c in enumerate(mid))
        tex_dir = os.path.join(assets, 'textures', 'block')
        os.makedirs(tex_dir, exist_ok=True)
        if pillar:
            side = colonne_side(mid, seed).img
            top = colonne_top(mid, seed + 1).img
            side.save(os.path.join(tex_dir, mid + '.png'))
            top.save(os.path.join(tex_dir, mid + '_top.png'))
            ga.write_json(os.path.join(assets, 'models', 'block', mid + '.json'),
                          {'parent': 'minecraft:block/cube_column', 'textures': {'end': f'{MODID}:block/{mid}_top', 'side': f'{MODID}:block/{mid}'}})
            ga.write_json(os.path.join(assets, 'models', 'block', mid + '_horizontal.json'),
                          {'parent': 'minecraft:block/cube_column_horizontal', 'textures': {'end': f'{MODID}:block/{mid}_top', 'side': f'{MODID}:block/{mid}'}})
            ga.write_json(os.path.join(assets, 'blockstates', mid + '.json'), {'variants': {
                'axis=y': {'model': f'{MODID}:block/{mid}'},
                'axis=z': {'model': f'{MODID}:block/{mid}_horizontal', 'x': 90},
                'axis=x': {'model': f'{MODID}:block/{mid}_horizontal', 'x': 90, 'y': 90}}})
            img = side
        else:
            img = PAINTERS[mid](mid, seed).img
            img.save(os.path.join(tex_dir, mid + '.png'))
            ga.write_json(os.path.join(assets, 'models', 'block', mid + '.json'),
                          {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{MODID}:block/{mid}'}})
            ga.write_json(os.path.join(assets, 'blockstates', mid + '.json'), {'variants': {'': {'model': f'{MODID}:block/{mid}'}}})
        sheet.paste(img, ((idx % 8) * S, (idx // 8) * S))
        idx += 1
        ga.write_json(os.path.join(assets, 'items', mid + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{MODID}:block/{mid}'}})
        ga.LANG[f'block.{MODID}.{mid}'] = fr
        ga.LANG_EN[f'block.{MODID}.{mid}'] = en
        mineable.append(f'{MODID}:{mid}')
        write_loot(ga, data, mid, False)
        if variants:
            tex = {'bottom': f'{MODID}:block/{mid}', 'top': f'{MODID}:block/{mid}', 'side': f'{MODID}:block/{mid}'}
            for suffix, parent in (('_stairs', 'minecraft:block/stairs'), ('_stairs_inner', 'minecraft:block/inner_stairs'),
                                   ('_stairs_outer', 'minecraft:block/outer_stairs')):
                ga.write_json(os.path.join(assets, 'models', 'block', mid + suffix + '.json'), {'parent': parent, 'textures': tex})
            ga.write_json(os.path.join(assets, 'models', 'block', mid + '_slab.json'),
                          {'parent': 'minecraft:block/slab', 'textures': tex})
            ga.write_json(os.path.join(assets, 'models', 'block', mid + '_slab_top.json'),
                          {'parent': 'minecraft:block/slab_top', 'textures': tex})
            ga.write_json(os.path.join(assets, 'blockstates', mid + '_stairs.json'), stairs_state(mid))
            ga.write_json(os.path.join(assets, 'blockstates', mid + '_slab.json'), {'variants': {
                'type=bottom': {'model': f'{MODID}:block/{mid}_slab'},
                'type=top': {'model': f'{MODID}:block/{mid}_slab_top'},
                'type=double': {'model': f'{MODID}:block/{mid}'}}})
            for kind, fr_s, en_s in (('stairs', f'Escalier : {fr.lower()}', f'{en} Stairs'), ('slab', f'Dalle : {fr.lower()}', f'{en} Slab')):
                bid = f'{mid}_{kind}'
                ga.write_json(os.path.join(assets, 'items', bid + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{MODID}:block/{bid}'}})
                ga.LANG[f'block.{MODID}.{bid}'] = fr_s
                ga.LANG_EN[f'block.{MODID}.{bid}'] = en_s
                mineable.append(f'{MODID}:{bid}')
                write_loot(ga, data, bid, kind == 'slab')
    ga.LANG[f'itemGroup.{MODID}.batisseur'] = "Aeloria : bâtisseur"
    ga.LANG_EN[f'itemGroup.{MODID}.batisseur'] = 'Aeloria: Builder'
    tag = os.path.join(data, 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json')
    with open(tag, encoding='utf-8') as f:
        cur = json.load(f)
    cur['values'] = cur['values'] + [m for m in mineable if m not in cur['values']]
    ga.write_json(tag, cur)
    sheet.resize((sheet.width * 4, sheet.height * 4), Image.NEAREST).save(os.path.join(os.path.dirname(__file__), 'apercu_decor.png'))


def write_loot(ga, data, bid, slab):
    entry = {'type': 'minecraft:item', 'name': f'{MODID}:{bid}'}
    if slab:
        entry['functions'] = [{'function': 'minecraft:set_count', 'count': 2, 'add': False, 'conditions': [{
            'condition': 'minecraft:block_state_property', 'block': f'{MODID}:{bid}', 'properties': {'type': 'double'}}]},
            {'function': 'minecraft:explosion_decay'}]
    ga.write_json(os.path.join(data, MODID, 'loot_table', 'blocks', bid + '.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [entry], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': f'{MODID}:blocks/{bid}'})


if __name__ == '__main__':
    import sys
    sys.path.insert(0, os.path.dirname(__file__))
    import generate_assets as ga
    ga.main()
