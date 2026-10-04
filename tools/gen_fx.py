# -*- coding: utf-8 -*-
"""Textures des effets visuels des competences (assets/mmorpg/textures/fx/).

Les effets lumineux sont dessines en blanc (la couleur vient des sommets) avec un rendu additif :
le noir/transparent ne s'affiche pas, le blanc brille. Les textures 'shard' et 'rock' sont colorees.
"""
import math
import os
import random

from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'mmorpg', 'textures', 'fx')

GLYPHES = [
    ['#.#', '##.', '#.#', '##.', '#..'],
    ['#.#', '#.#', '.#.', '.#.', '.#.'],
    ['#..', '##.', '#.#', '##.', '#..'],
    ['.#.', '#.#', '.#.', '.#.', '.#.'],
    ['.#.', '#.#', '.#.', '#.#', '#.#'],
    ['##.', '#.#', '##.', '#.#', '#.#'],
    ['#.#', '.#.', '#.#', '.#.', '#.#'],
]
EMBLEME = ['..#..', '.###.', '##.##', '.###.', '..#..']


def white(a):
    a = max(0.0, min(1.0, a))
    v = int(round(255 * a))
    return (v, v, v, v)


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, name + '.png'))


def radial(size, fn):
    img = Image.new('RGBA', (size, size))
    c = (size - 1) / 2
    for x in range(size):
        for y in range(size):
            d = math.hypot(x - c, y - c) / (size / 2)
            img.putpixel((x, y), white(fn(d, x, y)))
    return img


def glow():
    return radial(32, lambda d, x, y: (1 - d) ** 2 if d < 1 else 0)


def ring():
    def f(d, x, y):
        band = math.exp(-((d - 0.82) / 0.07) ** 2)
        inner = 0.18 * max(0.0, 1 - abs(d - 0.6) / 0.25)
        return min(1.0, band + inner) if d < 1 else 0
    return radial(64, f)


def wave():
    img = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), white((y / 15) ** 1.6))
    return img


def beam():
    img = Image.new('RGBA', (16, 16))
    for x in range(16):
        a = math.exp(-((x - 7.5) / 3.2) ** 2)
        for y in range(16):
            img.putpixel((x, y), white(a))
    return img


def slash():
    w, h = 64, 16
    img = Image.new('RGBA', (w, h))
    for x in range(w):
        lead = (x / (w - 1)) ** 1.8
        for y in range(h):
            edge = math.exp(-((y - 3) / 2.2) ** 2) + 0.35 * math.exp(-((y - 8) / 4.0) ** 2)
            img.putpixel((x, y), white(min(1.0, lead * edge * 1.3)))
    return img


def streak():
    w, h = 32, 8
    img = Image.new('RGBA', (w, h))
    for x in range(w):
        t = x / (w - 1)
        for y in range(h):
            core = math.exp(-((y - 3.5) / (0.9 + 1.4 * t)) ** 2)
            img.putpixel((x, y), white(core * (0.15 + 0.85 * t ** 1.5)))
    return img


def spark():
    img = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            a = max(math.exp(-dx / 0.9) * math.exp(-dy / 4.5), math.exp(-dy / 0.9) * math.exp(-dx / 4.5))
            a = max(a, (1 - math.hypot(dx, dy) / 4) if math.hypot(dx, dy) < 4 else 0)
            img.putpixel((x, y), white(a))
    return img


def hexagons():
    size = 32
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    r = 8.0
    w = math.sqrt(3) * r
    for x in range(size):
        for y in range(size):
            best = 99
            for i in range(-1, 4):
                for j in range(-1, 4):
                    cx = i * w + (j % 2) * w / 2
                    cy = j * r * 1.5
                    px, py = x - cx, y - cy
                    # distance au bord de l'hexagone (pointe en haut)
                    ax, ay = abs(px), abs(py)
                    dist = max(ax * math.sqrt(3) / 2 + ay / 2, ay)
                    best = min(best, abs(dist - r * math.sqrt(3) / 2 * 0.98))
            a = 1.0 if best < 0.8 else (0.35 if best < 1.6 else 0.08)
            img.putpixel((x, y), white(a))
    return img


def rune_circle():
    size = 128
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    c = (size - 1) / 2
    for x in range(size):
        for y in range(size):
            d = math.hypot(x - c, y - c)
            a = 0
            for rr, wdt in ((62, 1.2), (57, 0.8), (40, 1.0), (36, 0.7)):
                if abs(d - rr) < wdt:
                    a = max(a, 1.0 if abs(d - rr) < wdt * 0.6 else 0.6)
            if d < 62:
                a = max(a, 0.06)
            img.putpixel((x, y), white(a))
    # hexagramme
    pts = [(c + 36 * math.cos(math.radians(90 + k * 60)), c + 36 * math.sin(math.radians(90 + k * 60))) for k in range(6)]
    for tri in ((0, 2, 4), (1, 3, 5)):
        for a_, b_ in ((tri[0], tri[1]), (tri[1], tri[2]), (tri[2], tri[0])):
            (x0, y0), (x1, y1) = pts[a_], pts[b_]
            n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2)
            for k in range(n + 1):
                x = x0 + (x1 - x0) * k / n
                y = y0 + (y1 - y0) * k / n
                img.putpixel((int(round(x)), int(round(y))), white(0.9))
    # runes entre les deux cercles exterieurs (glyphes agrandis x2)
    for k in range(12):
        ang = math.radians(k * 30)
        gx, gy = c + 48.5 * math.cos(ang), c + 48.5 * math.sin(ang)
        g = GLYPHES[k % len(GLYPHES)]
        for j, row in enumerate(g):
            for i, ch in enumerate(row):
                if ch == '#':
                    for ox in range(2):
                        for oy in range(2):
                            img.putpixel((int(gx - 3 + i * 2 + ox), int(gy - 5 + j * 2 + oy)), white(1.0))
    # embleme central
    for j, row in enumerate(EMBLEME):
        for i, ch in enumerate(row):
            if ch == '#':
                for ox in range(3):
                    for oy in range(3):
                        img.putpixel((int(c - 7 + i * 3 + ox), int(c - 7 + j * 3 + oy)), white(1.0))
    return img


def shard():
    w, h = 16, 32
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    for y in range(h):
        half = (w / 2 - 1) * (y / (h - 1)) ** 0.7 if y < h * 0.85 else (w / 2 - 1) * (1 - (y - h * 0.85) / (h * 0.15)) * 0.9
        for x in range(w):
            dx = abs(x - (w - 1) / 2)
            if dx <= half:
                edge = dx > half - 1.2
                t = y / h
                c = (200, 236, 255) if edge else (int(120 + 60 * t), int(190 + 30 * t), 255)
                if x < w / 2 - 1 and not edge:
                    c = (150, 210, 255)
                img.putpixel((x, y), c + (220 if not edge else 255,))
    return img


def rock():
    r = random.Random(7)
    img = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            v = r.uniform(0.7, 1.0)
            c = (int(70 * v), int(36 * v), int(30 * v))
            img.putpixel((x, y), c + (255,))
    for _ in range(5):
        x, y = r.randrange(16), r.randrange(16)
        for _ in range(7):
            img.putpixel((x % 16, y % 16), (255, 150 + r.randrange(80), 40, 255))
            x += r.choice((-1, 0, 1))
            y += r.choice((-1, 0, 1))
    return img


def shield():
    size = 32
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for x in range(size):
        for y in range(size):
            u, v = (x - 15.5) / 14, (y - 4) / 26
            if 0 <= v <= 1:
                halfw = 1.0 if v < 0.55 else 1.0 - (v - 0.55) / 0.45
                if abs(u) <= halfw:
                    edge = abs(u) > halfw - 0.12 or v < 0.06
                    img.putpixel((x, y), white(1.0 if edge else 0.22))
    for j, row in enumerate(EMBLEME):
        for i, ch in enumerate(row):
            if ch == '#':
                for ox in range(2):
                    for oy in range(2):
                        img.putpixel((11 + i * 2 + ox, 11 + j * 2 + oy), white(1.0))
    return img


def rune_mark():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    g = ['.###.', '#...#', '#.#.#', '#...#', '.###.']
    excl = ['.#.', '.#.', '.#.', '...', '.#.']
    for j, row in enumerate(g):
        for i, ch in enumerate(row):
            if ch == '#':
                for o in range(3):
                    for p in range(3):
                        pass
    for j in range(16):
        for i in range(16):
            d = math.hypot(i - 7.5, j - 7.5)
            if 6.0 < d < 7.6:
                img.putpixel((i, j), white(1.0))
    for j, row in enumerate(excl):
        for i, ch in enumerate(row):
            if ch == '#':
                for o in range(2):
                    for p in range(2):
                        img.putpixel((5 + i * 2 + o, 3 + j * 2 + p), white(1.0))
    return img


def generate():
    save(glow(), 'glow')
    save(ring(), 'ring')
    save(wave(), 'wave')
    save(beam(), 'beam')
    save(slash(), 'slash')
    save(streak(), 'streak')
    save(spark(), 'spark')
    save(hexagons(), 'hex')
    save(rune_circle(), 'rune_circle')
    save(shard(), 'shard')
    save(rock(), 'rock')
    save(shield(), 'shield')
    save(rune_mark(), 'rune_mark')


if __name__ == '__main__':
    generate()
    print('textures fx generees dans', os.path.normpath(OUT))
