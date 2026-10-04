"""Petits utilitaires de pixel-art procedural utilises par generate_assets.py."""
import math
import random
from PIL import Image

T = (0, 0, 0, 0)


def rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def shade(c, f):
    """f > 1 eclaircit, f < 1 assombrit."""
    if f >= 1:
        return mix(c, (255, 255, 255, 255), min(1.0, f - 1))
    return mix(c, (0, 0, 0, 255), min(1.0, 1 - f))


def ramp(base):
    """Rampe de 5 tons a partir d'une couleur de base : outline, sombre, base, clair, eclat."""
    b = rgb(base) if isinstance(base, str) else base
    return {
        'o': shade(b, 0.28),
        'c': shade(b, 0.62),
        'b': b,
        'a': shade(b, 1.35),
        'w': shade(b, 1.7),
    }


class Canvas:
    def __init__(self, w=16, h=16):
        self.w, self.h = w, h
        self.px = [[T for _ in range(w)] for _ in range(h)]
        self.role = [[None for _ in range(w)] for _ in range(h)]

    def inside(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h

    def set(self, x, y, c, role='x'):
        if self.inside(x, y):
            self.px[y][x] = c
            self.role[y][x] = role

    def get(self, x, y):
        return self.px[y][x] if self.inside(x, y) else T

    def filled(self, x, y):
        return self.inside(x, y) and self.px[y][x][3] > 0

    def outline(self, color, diagonal=False, skip_roles=()):
        pts = []
        for y in range(self.h):
            for x in range(self.w):
                if self.filled(x, y):
                    continue
                nb = [(1, 0), (-1, 0), (0, 1), (0, -1)]
                if diagonal:
                    nb += [(1, 1), (-1, -1), (1, -1), (-1, 1)]
                for dx, dy in nb:
                    if self.filled(x + dx, y + dy) and self.role[y + dy][x + dx] not in skip_roles:
                        pts.append((x, y))
                        break
        for x, y in pts:
            self.set(x, y, color, 'o')

    def template(self, rows, pal, ox=0, oy=0):
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in ('.', ' '):
                    continue
                if ch in pal and pal[ch] is not None:
                    self.set(ox + x, oy + y, pal[ch], ch)

    def image(self):
        img = Image.new('RGBA', (self.w, self.h))
        for y in range(self.h):
            for x in range(self.w):
                img.putpixel((x, y), self.px[y][x])
        return img

    def save(self, path):
        self.image().save(path)


def noise_fill(img, x0, y0, w, h, base, var=0.12, seed=0, speck=None, speck_chance=0.0):
    rnd = random.Random(seed)
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            f = 1 + rnd.uniform(-var, var)
            c = shade(base, f)
            if speck and rnd.random() < speck_chance:
                c = speck
            img.putpixel((x, y), c)


def circle(cv, cx, cy, r, color, role='x'):
    for y in range(cv.h):
        for x in range(cv.w):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                cv.set(x, y, color, role)


def shaded_circle(cv, cx, cy, r, pal, light=(-0.6, -0.6)):
    for y in range(cv.h):
        for x in range(cv.w):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.sqrt(dx * dx + dy * dy)
            if d <= r:
                l = -(dx * light[0] + dy * light[1]) / max(r, 0.01)
                if d > r - 0.9:
                    c = pal['c']
                elif l > 0.55:
                    c = pal['w']
                elif l > 0.15:
                    c = pal['a']
                elif l > -0.35:
                    c = pal['b']
                else:
                    c = pal['c']
                cv.set(x, y, c, 'b')
