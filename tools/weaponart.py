"""Moteur de peinture d'armes : formes vectorielles -> carte de hauteur -> eclairage -> sprite RGBA haute definition
+ conversion du sprite en modele 3D (extrusion par niveaux d'epaisseur, UV pris dans le sprite).

Coordonnees en unites de modele (16 unites = 1 bloc), y vers le haut, origine en bas a gauche du canevas.
"""
import json
import math
import numpy as np
from PIL import Image, ImageDraw

PPU = 4          # pixels de texture par unite de modele
SS = 4           # sur-echantillonnage de la peinture
THICK = [0.45, 0.8, 1.2, 1.7, 2.3]  # epaisseurs (unites) des niveaux de l'extrusion


def hexc(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=float)


def mix(a, b, t):
    return np.asarray(a, dtype=float) * (1 - t) + np.asarray(b, dtype=float) * t


def shade(c, k):
    return np.clip(np.asarray(c, dtype=float) * k, 0, 255)


def gauss(a, sigma):
    """Flou gaussien separable en numpy."""
    if sigma < .3:
        return a
    r = int(math.ceil(sigma * 3))
    k = np.exp(-.5 * (np.arange(-r, r + 1) / sigma) ** 2)
    k /= k.sum()
    p = np.pad(a, ((r, r), (0, 0)), mode='edge')
    out = np.zeros_like(a)
    for i, w in enumerate(k):
        out += w * p[i:i + a.shape[0]]
    p = np.pad(out, ((0, 0), (r, r)), mode='edge')
    out2 = np.zeros_like(a)
    for i, w in enumerate(k):
        out2 += w * p[:, i:i + a.shape[1]]
    return out2


class Art:
    def __init__(self, w, h, seed=1):
        self.wu, self.hu = w, h
        self.f = PPU * SS
        self.W, self.H = int(w * self.f), int(h * self.f)
        self.rgb = np.zeros((self.H, self.W, 3))
        self.a = np.zeros((self.H, self.W))
        self.t = np.zeros((self.H, self.W))        # epaisseur (unites)
        self.spec = np.zeros((self.H, self.W))
        self.glow = np.zeros((self.H, self.W))
        self.rng = np.random.default_rng(seed)
        yy, xx = np.mgrid[:self.H, :self.W]
        self.X = (xx + .5) / self.f
        self.Y = h - (yy + .5) / self.f

    # ---- masques
    def poly(self, pts):
        im = Image.new('L', (self.W, self.H), 0)
        ImageDraw.Draw(im).polygon([(x * self.f, (self.hu - y) * self.f) for x, y in pts], fill=255)
        return np.asarray(im) > 127

    def rect(self, cx, cy, w, h):
        return self.poly([(cx - w / 2, cy - h / 2), (cx + w / 2, cy - h / 2), (cx + w / 2, cy + h / 2), (cx - w / 2, cy + h / 2)])

    def ellipse(self, cx, cy, rx, ry=None, rot=0):
        ry = rx if ry is None else ry
        a = np.linspace(0, 2 * math.pi, 48, endpoint=False)
        c, s = math.cos(math.radians(rot)), math.sin(math.radians(rot))
        return self.poly([(cx + rx * math.cos(t) * c - ry * math.sin(t) * s, cy + rx * math.cos(t) * s + ry * math.sin(t) * c) for t in a])

    def line(self, x0, y0, x1, y1, w0, w1=None):
        w1 = w0 if w1 is None else w1
        dx, dy = x1 - x0, y1 - y0
        n = math.hypot(dx, dy) or 1
        nx, ny = -dy / n, dx / n
        return self.poly([(x0 + nx * w0 / 2, y0 + ny * w0 / 2), (x1 + nx * w1 / 2, y1 + ny * w1 / 2),
                          (x1 - nx * w1 / 2, y1 - ny * w1 / 2), (x0 - nx * w0 / 2, y0 - ny * w0 / 2)])

    def stroke(self, pts, widths):
        """Trait epais le long d'une polyligne, largeur interpolee (liste ou nombre)."""
        n = len(pts)
        if not isinstance(widths, (list, tuple)):
            widths = [widths] * n
        left, right = [], []
        for i, (x, y) in enumerate(pts):
            x0, y0 = pts[max(0, i - 1)]
            x1, y1 = pts[min(n - 1, i + 1)]
            dx, dy = x1 - x0, y1 - y0
            d = math.hypot(dx, dy) or 1
            nx, ny = -dy / d, dx / d
            w = widths[i] / 2
            left.append((x + nx * w, y + ny * w))
            right.append((x - nx * w, y - ny * w))
        return self.poly(left + right[::-1])

    def union(self, *ms):
        out = ms[0].copy()
        for m in ms[1:]:
            out |= m
        return out

    # ---- peinture d'une piece
    def part(self, mask, col, t0=.5, t1=1.0, sigma=.5, spec=.25, grain=.05, flat=False, glow=0.0, vgrain=False, grad=None):
        """Colle une piece : couleur, epaisseur (t0 au bord, t1 au centre), brillance."""
        if not mask.any():
            return
        if flat:
            h = np.full(mask.shape, t1)
        else:
            b = gauss(mask.astype(float), sigma * self.f)
            h = t0 + (t1 - t0) * np.clip((b - .5) * 2, 0, 1)
        col = np.asarray(col, dtype=float)
        base = np.broadcast_to(col, (self.H, self.W, 3)).copy()
        if grad is not None:           # (couleur2, axe 'y'|'x', y0, y1)
            c2, axis, a0, a1 = grad
            v = self.Y if axis == 'y' else self.X
            tt = np.clip((v - a0) / (a1 - a0), 0, 1)[..., None]
            base = base * (1 - tt) + np.asarray(c2, dtype=float) * tt
        if grain:
            if vgrain:
                n = self.rng.normal(0, 1, (1, self.W))
                n = gauss(np.repeat(n, self.H, 0), 1.2) * 1.0
                n += self.rng.normal(0, .35, (self.H, self.W))
            else:
                n = gauss(self.rng.normal(0, 1, (self.H, self.W)), 1.0) * 1.4
            base *= (1 + grain * n)[..., None]
        m = mask
        self.rgb[m] = base[m]
        self.a[m] = 1
        self.t[m] = h[m]
        self.spec[m] = spec
        self.glow[m] = glow

    def cut(self, mask, dt=.35, col=None, mixk=.5):
        """Creux (gravure, gouttiere) : abaisse l'epaisseur et assombrit."""
        m = mask & (self.a > 0)
        self.t[m] = np.maximum(.3, self.t[m] - dt)
        if col is not None:
            self.rgb[m] = mix(self.rgb[m], col, mixk)

    def paint(self, mask, col, mixk=1.0, glow=0.0):
        m = mask & (self.a > 0)
        self.rgb[m] = mix(self.rgb[m], col, mixk)
        if glow:
            self.glow[m] = glow

    def gem(self, cx, cy, r, col, ry=None, facets=True, glow=.6):
        ry = r if ry is None else ry
        a = np.linspace(0, 2 * math.pi, 8, endpoint=False) + math.pi / 8 if r > 1.6 else np.array([0, math.pi / 2, math.pi, 1.5 * math.pi])
        m = self.poly([(cx + r * math.cos(t), cy + ry * math.sin(t)) for t in a])
        dist = np.maximum(np.abs(self.X - cx) / r, np.abs(self.Y - cy) / ry)
        d1 = (np.abs(self.X - cx) / r + np.abs(self.Y - cy) / ry)
        h = .9 + .9 * (1 - np.minimum(1, (dist + d1 * .7) / 1.7))
        self.part(m, col, t0=.9, t1=1.8, flat=True, spec=.9, grain=0, glow=glow)
        self.t[m] = h[m]
        if facets:
            ang = np.arctan2(self.Y - cy, self.X - cx)
            k = np.floor((ang + math.pi) / (2 * math.pi) * len(a)).astype(int) % len(a)
            tone = (0.78 + 0.5 * ((k * 3 + 1) % len(a)) / len(a))
            self.rgb[m] *= tone[m][:, None]
        # reflet
        hl = self.ellipse(cx - r * .3, cy + ry * .35, r * .22, ry * .16, 30) & m
        self.rgb[hl] = mix(self.rgb[hl], (255, 255, 255), .85)

    def gradient_t(self, mask, fn):
        """Remplace l'epaisseur dans le masque par fn(X, Y) (plans de facettes)."""
        m = mask & (self.a > 0)
        self.t[m] = fn(self.X, self.Y)[m]

    # ---- rendu final
    def render(self, outline=True, lightk=1.0):
        a = self.a > .5
        t = gauss(self.t * a, 1.1)
        t = np.where(a, t, 0)
        gy, gx = np.gradient(t, 1 / self.f)
        nx, ny = -gx * .9, -gy * .9
        inv = 1 / np.sqrt(nx * nx + ny * ny + 1)
        L = np.array([-.5, -.62, .6])
        L = L / np.linalg.norm(L)
        diff = (nx * L[0] + ny * L[1] + L[2]) * inv
        flat = L[2]
        k = .55 + .65 * (diff / flat) ** 1.0
        k = np.clip(k, .3, 1.55)
        # occlusion douce aux jonctions
        occ = gauss(t, 3 * self.f / 4) - t
        k *= np.clip(1 - occ * .35, .6, 1.05)
        Hh = L + np.array([0, 0, 1.0])
        Hh = Hh / np.linalg.norm(Hh)
        sp = np.clip((nx * Hh[0] + ny * Hh[1] + Hh[2]) * inv, 0, 1) ** 28 * self.spec
        rgb = self.rgb * (1 + (k - 1) * lightk)[..., None] + 255 * sp[..., None] * .8
        rgb = rgb + (self.glow[..., None] * np.array([40, 40, 40]) * 0)  # lueur : voir 'emit'
        if outline:
            er = a.copy()
            for _ in range(int(round(self.f * .22))):
                p = np.pad(er, 1, constant_values=False)
                er = er & p[:-2, 1:-1] & p[2:, 1:-1] & p[1:-1, :-2] & p[1:-1, 2:]
            ring = a & ~er
            dark = rgb * .32 + np.array([8, 6, 14])
            rgb = np.where(ring[..., None], dark, rgb)
        rgb = np.clip(rgb, 0, 255)
        return rgb, a.astype(float), np.where(a, self.t, 0), self.glow

    def finish(self):
        """Renvoie (RGBA uint8, niveaux d'epaisseur) a la resolution finale."""
        rgb, a, t, glow = self.render()
        s = SS
        h, w = self.H // s, self.W // s
        a4 = a.reshape(h, s, w, s)
        cov = a4.mean((1, 3))
        wgt = np.maximum(a4.sum((1, 3)), 1e-6)
        out = (rgb.reshape(h, s, w, s, 3) * a4[..., None]).sum((1, 3)) / wgt[..., None]
        tl = (t.reshape(h, s, w, s) * a4).sum((1, 3)) / wgt
        alpha = cov > .5
        img = np.zeros((h, w, 4), dtype=np.uint8)
        img[..., :3] = np.clip(out, 0, 255).astype(np.uint8)
        img[..., 3] = np.where(alpha, 255, 0)
        lv = np.zeros((h, w), dtype=int)
        thr = np.array(THICK)
        idx = np.abs(tl[..., None] - thr[None, None, :]).argmin(-1) + 1
        lv = np.where(alpha, idx, 0)
        return img, lv


# ------------------------------------------------------------------ 3D

def rects(level_map):
    """Fusion gloutonne : liste (x0, y0, x1, y1, niveau) de rectangles de niveau constant (pixels, x1/y1 exclus)."""
    h, w = level_map.shape
    out = []
    for k in range(1, level_map.max() + 1):
        m = level_map == k
        open_ = {}
        for y in range(h + 1):
            runs = []
            if y < h:
                row = m[y]
                x = 0
                while x < w:
                    if row[x]:
                        x0 = x
                        while x < w and row[x]:
                            x += 1
                        runs.append((x0, x))
                    else:
                        x += 1
            cur = set(runs)
            for key in list(open_):
                if key not in cur:
                    y0 = open_.pop(key)
                    out.append((key[0], y0, key[1], y, k))
            for r in runs:
                open_.setdefault(r, y)
    return out


def build_model(lv, tex, hu, display=None, cz=8.0, tex_w=None, tex_h=None):
    """Modele JSON : un element par rectangle, face avant/arriere avec UV du sprite, tranches avec UV du bord."""
    h, w = lv.shape
    tw, th = tex_w or w, tex_h or h
    rs = rects(lv)
    els = []

    def uv(x0, y0, x1, y1):
        return [round(x0 / tw * 16, 3), round(y0 / th * 16, 3), round(x1 / tw * 16, 3), round(y1 / th * 16, 3)]

    for (x0, y0, x1, y1, k) in rs:
        t = THICK[k - 1]
        reg = lv[y0:y1, x0:x1]
        faces = {'south': {'uv': uv(x0, y0, x1, y1), 'texture': '#t'},
                 'north': {'uv': uv(x1, y0, x0, y1), 'texture': '#t'}}

        def exposed(sl):
            return bool((sl < k).any())
        if y0 == 0 or exposed(lv[y0 - 1, x0:x1]):
            faces['up'] = {'uv': uv(x0, y0, x1, y0 + 1), 'texture': '#t'}
        if y1 == h or exposed(lv[y1, x0:x1]):
            faces['down'] = {'uv': uv(x0, y1 - 1, x1, y1), 'texture': '#t'}
        if x0 == 0 or exposed(lv[y0:y1, x0 - 1]):
            faces['west'] = {'uv': uv(x0, y0, x0 + 1, y1), 'texture': '#t'}
        if x1 == w or exposed(lv[y0:y1, x1]):
            faces['east'] = {'uv': uv(x1 - 1, y0, x1, y1), 'texture': '#t'}
        els.append({'from': [round(x0 / PPU, 3), round(hu - y1 / PPU, 3), round(cz - t / 2, 3)],
                    'to': [round(x1 / PPU, 3), round(hu - y0 / PPU, 3), round(cz + t / 2, 3)],
                    'faces': faces})
    return els
