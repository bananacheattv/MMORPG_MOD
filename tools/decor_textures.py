"""Textures des decors : une tuile raccordable de 256 x 256 par matiere, reunies dans un atlas partage par tous les modeles.

Echelle : une tuile couvre 48 unites de modele (1 bloc de citadelle = 3 blocs Minecraft), soit 5,33 pixels par unite.
    python tools/decor_textures.py     -> art/decor_textures/<matiere>.png et textures/imported/props/decor_atlas.png
"""
import colorsys
import math
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
TILES_DIR = ROOT / 'art/decor_textures'
ATLAS_FILE = ROOT / 'src/main/resources/assets/mmorpg/textures/imported/props/decor_atlas.png'
N = 256
COLS = 8
Y, X = np.mgrid[0:N, 0:N].astype(float)


def rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=float)


def pn(fx, fy, seed):
    """Bruit de valeur periodique."""
    r = np.random.default_rng(seed)
    g = r.random((fy, fx))
    x, y = X / N * fx, Y / N * fy
    ix, iy = np.floor(x).astype(int), np.floor(y).astype(int)
    tx, ty = x - ix, y - iy
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[iy % fy, ix % fx] * (1 - tx) + g[iy % fy, (ix + 1) % fx] * tx
    b = g[(iy + 1) % fy, ix % fx] * (1 - tx) + g[(iy + 1) % fy, (ix + 1) % fx] * tx
    return a * (1 - ty) + b * ty


def fbm(f, seed, oct=5, fy=None):
    fy = fy or f
    out, amp, tot = 0, 1.0, 0
    for k in range(oct):
        fx2, fy2 = min(f << k, 128), min(fy << k, 128)
        out = out + pn(fx2, fy2, seed + k * 17) * amp
        tot += amp
        amp *= .55
    return out / tot


def relief(h, k):
    d = (np.roll(h, 1, 0) - np.roll(h, -1, 0)) + (np.roll(h, 1, 1) - np.roll(h, -1, 1))
    return np.clip(1 - k * d, .35, 1.8)[..., None]


def sstep(a, b, v):
    t = np.clip((v - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def mix(a, b, t):
    return a + (b - a) * (t[..., None] if np.ndim(t) == 2 else t)


def ramp(t, stops):
    t = np.clip(t, 0, 1)
    pos = np.array([s[0] for s in stops])
    cols = np.array([rgb(s[1]) for s in stops])
    return np.stack([np.interp(t, pos, cols[:, c]) for c in range(3)], -1)


def voronoi(n, seed):
    r = np.random.default_rng(seed)
    pts = r.random((n, 2)) * N
    d1 = np.full((N, N), 1e9)
    d2 = np.full((N, N), 1e9)
    ids = np.zeros((N, N), dtype=int)
    for i, (px, py) in enumerate(pts):
        dx = np.abs(X - px)
        dy = np.abs(Y - py)
        dx = np.minimum(dx, N - dx)
        dy = np.minimum(dy, N - dy)
        d = np.sqrt(dx * dx + dy * dy)
        closer = d < d1
        d2 = np.where(closer, d1, np.minimum(d2, d))
        ids = np.where(closer, i, ids)
        d1 = np.minimum(d1, d)
    return d1, d2, ids


# ------------------------------------------------------------------ matieres
def ashlar(base, seed, rows=4, moss=0.0, crack=1.0):
    c = rgb(base)
    r = np.random.default_rng(seed)
    rh = N // rows
    row = (Y // rh).astype(int)
    fy = Y % rh
    dy = np.minimum(fy, rh - fy)
    dx = np.full((N, N), 1e9)
    bid = np.zeros((N, N), dtype=int)
    tones = r.uniform(.8, 1.14, 200)
    for ro in range(rows):
        n = 2 + (ro % 2)
        joints = (np.cumsum(r.uniform(.7, 1.3, n)) / 1.0)
        joints = joints / joints[-1] * N
        joints = (joints + r.uniform(0, N)) % N
        joints.sort()
        m = row == ro
        for j in joints:
            d = np.abs(X - j)
            d = np.minimum(d, N - d)
            dx = np.where(m, np.minimum(dx, d), dx)
        idx = np.searchsorted(joints, X) % n
        bid = np.where(m, ro * 8 + idx, bid)
    wob = (fbm(16, seed + 1, 3) - .5) * 7
    edge = np.minimum(dx, dy) + wob
    tone = tones[bid]
    surf = fbm(8, seed + 2, 6)
    grain = fbm(48, seed + 3, 3)
    h = sstep(1.0, 6.0, edge) * 1.3 + surf * .9 + grain * .5
    alb = c[None, None] * tone[..., None] * (.8 + .42 * surf[..., None]) * (.9 + .2 * grain[..., None])
    stain = sstep(.56, .74, fbm(5, seed + 4, 4))
    alb = mix(alb, alb * .72, stain * .75)
    # fissures
    v = np.abs(fbm(6, seed + 5, 5) - .5)
    alb = mix(alb, alb * .5, (v < .0055) * crack * sstep(.45, .6, fbm(4, seed + 6, 2)))
    if moss:
        m = sstep(.62 - moss * .1, .72, fbm(9, seed + 7, 5)) * (1 - sstep(4, 16, edge) * .5) * moss
        alb = mix(alb, rgb('#56703a')[None, None] * (.7 + .5 * grain[..., None]), m)
    alb = mix(alb, c[None, None] * .42 * (.8 + .4 * grain[..., None]), 1 - sstep(.5, 2.8, edge))
    return alb * relief(h, .72)


def smooth_stone(base, seed):
    c = rgb(base)
    a, b, s = fbm(6, seed, 6), fbm(32, seed + 3, 3), pn(128, 128, seed + 5)
    h = a + b * .5 + s * .25
    alb = c[None, None] * (.8 + .36 * a[..., None]) * (.9 + .2 * b[..., None])
    alb = mix(alb, alb * .72, s > .86)
    return alb * relief(h, .9)


def marble(base, vein, seed):
    c, v = rgb(base), rgb(vein)
    n1, n2 = fbm(3, seed, 6), fbm(4, seed + 9, 6)
    a = np.sin(2 * np.pi * (2 * X / N + Y / N) + n1 * 15)
    b = np.sin(2 * np.pi * (X / N - 3 * Y / N) + n2 * 18)
    v1 = np.exp(-(a * a) / .012)
    v2 = np.exp(-(b * b) / .004) * .7
    cloud = fbm(5, seed + 3, 5)
    alb = c[None, None] * (.9 + .2 * cloud[..., None])
    alb = mix(alb, mix(c[None, None], v[None, None], .55), sstep(.45, .85, np.abs(a)) * 0 + np.exp(-(a * a) / .09) * .35)
    alb = mix(alb, v[None, None], np.clip(v1 * .95 + v2, 0, 1))
    gl = sstep(.55, .8, fbm(3, seed + 6, 3)) * 12
    return alb + gl[..., None]


def planks(base, seed, nails=True):
    c = rgb(base)
    r = np.random.default_rng(seed)
    w = np.array([52, 48, 56, 52, 48])
    ed = np.cumsum(w)
    p = np.searchsorted(ed, X, side='right')
    p = np.clip(p, 0, 4)
    starts = np.concatenate([[0], ed[:-1]])
    lx = X - starts[p]
    tone = r.uniform(.78, 1.14, 5)[p]
    off = r.uniform(0, 7, 5)[p]
    warp = fbm(3, seed + 1, 4, fy=2)
    rings = np.sin((lx * .34 + off * 6 + warp * 26))
    fine = pn(128, 8, seed + 2)
    fine2 = pn(64, 4, seed + 3)
    v = .5 + .24 * rings + .3 * (fine - .5) + .25 * (fine2 - .5)
    # noeuds
    alb = c[None, None] * tone[..., None] * (.58 + .78 * v[..., None])
    for k in range(3):
        kx, ky = r.uniform(0, N), r.uniform(0, N)
        dx, dy = np.abs(X - kx), np.abs(Y - ky)
        dx, dy = np.minimum(dx, N - dx), np.minimum(dy, N - dy)
        d = np.sqrt((dx * 1.6) ** 2 + (dy * .6) ** 2)
        kn = np.exp(-(d / 4.5) ** 2) + .5 * np.exp(-(d / 9) ** 2) * (np.sin(d * 2.1) * .5 + .5)
        alb = mix(alb, alb * .4, np.clip(kn, 0, .9))
    gap = lx < 2
    gap = gap | (lx > w[p] - 1)
    h = v * .9
    # bouts de planches
    ends = (r.uniform(0, N, 5))[p]
    dyy = np.abs(Y - ends)
    dyy = np.minimum(dyy, N - dyy)
    gap = gap | (dyy < 1.2)
    if nails:
        for s in (-5.5, 5.5):
            for side in (7, 0):
                nx = lx - (side if side else w[p] - 7)
                nd = np.sqrt(nx * nx + (dyy - abs(s)) ** 2)
                alb = mix(alb, rgb('#2c2826')[None, None], nd < 1.8)
                alb = mix(alb, rgb('#6d6a64')[None, None], nd < .8)
    h = np.where(gap, h - 1.6, h)
    alb = mix(alb, c[None, None] * .16, gap)
    return alb * relief(h, .55)


def gold(seed, base='#d9a830', ornate=False):
    streak = pn(128, 6, seed)
    sheen = fbm(3, seed + 1, 3)
    d1, d2, _ = voronoi(40, seed + 2)
    dim = sstep(0, 22, d1)
    v = .22 + .56 * sheen + .13 * (streak - .5) + .2 * dim
    c = rgb(base)
    alb = ramp(v, [(0, '#' + ''.join(f'{int(x * .36):02x}' for x in c)), (.45, '#' + ''.join(f'{int(x * .82):02x}' for x in c)),
                   (.72, base), (.9, '#f8dd80'), (1, '#fff5c8')])
    h = dim * .35 + streak * .15
    if ornate:
        u, w = ((X + 32) % 64) - 32, ((Y + 32) % 64) - 32
        lat = np.abs(np.abs(u) + np.abs(w) - 30)
        ring = np.abs(np.sqrt(u * u + w * w) - 11)
        crossm = np.minimum(np.abs(u), np.abs(w))
        dot = np.sqrt(u * u + w * w)
        raised = (lat < 2.5) | (ring < 1.8) | ((crossm < 1.2) & (dot < 11)) | (dot < 3)
        rec = ~raised
        hm = sstep(0, 1, raised.astype(float))
        h = h * .4 + np.where(raised, 1.5, 0) + hm * .4
        alb = np.where(rec[..., None], alb * .46, alb * 1.14)
    return alb * relief(h, .55)


def iron(base, seed, plate=False):
    c = rgb(base)
    a, b = fbm(5, seed, 6), pn(128, 128, seed + 2)
    d1, d2, _ = voronoi(46, seed + 3)
    hm = sstep(0, 20, d1)
    h = hm * .7 + a * .4
    alb = c[None, None] * (.66 + .62 * a[..., None]) * (.92 + .16 * b[..., None])
    rust = sstep(.66, .78, fbm(10, seed + 4, 5))
    alb = mix(alb, rgb('#6a3f24')[None, None] * (.7 + .6 * b[..., None]), rust * .42)
    if plate:
        u, w = (X % 128), (Y % 128)
        seam = (u < 2) | (w < 2)
        h = np.where(seam, h - 2, h)
        alb = mix(alb, alb * .35, seam)
        for ox in (10, 118):
            for oy in (10, 118):
                d = np.sqrt((u - ox) ** 2 + (w - oy) ** 2)
                h = np.where(d < 5, h + (5 - d) * .4, h)
    return alb * relief(h, .65)


def steel(base, seed):
    c = rgb(base)
    s = pn(128, 5, seed)
    a = fbm(3, seed + 1, 3)
    v = .8 + .38 * (a - .5) + .2 * (s - .5)
    return c[None, None] * v[..., None] * relief(s * .3, .5)


def glass(palette, seed, cells=26):
    d1, d2, ids = voronoi(cells, seed)
    r = np.random.default_rng(seed + 1)
    cols = np.array([rgb(palette[r.integers(0, len(palette))]) for _ in range(cells)])
    fill = cols[ids]
    edge = d2 - d1
    glow = .72 + .42 * sstep(0, 26, edge)
    tex = .9 + .24 * fbm(14, seed + 2, 4)
    alb = fill * glow[..., None] * tex[..., None]
    alb = np.clip(alb + 26 * sstep(.62, .85, fbm(5, seed + 3, 3))[..., None], 0, 255)
    lead = edge < 4.2
    high = (edge > 1.4) & (edge < 2.6)
    alb = np.where(lead[..., None], rgb('#201c26')[None, None], alb)
    alb = np.where(high[..., None], rgb('#4e4858')[None, None], alb)
    return alb


def foliage(seed, flowers=None, dark='#16300f', leafs=('#2f6b23', '#3f8a2e', '#57a33a', '#27591c')):
    r = np.random.default_rng(seed)
    img = rgb(dark)[None, None] * np.ones((N, N, 3))
    img *= (.7 + .7 * fbm(10, seed + 1, 3))[..., None]
    pal = [rgb(c) for c in leafs]

    def stamp(cx, cy, L_, W, ang, col, midrib=True):
        sz = int(L_ + 2)
        xs = (np.arange(int(cx) - sz, int(cx) + sz + 1)) % N
        ys = (np.arange(int(cy) - sz, int(cy) + sz + 1)) % N
        gy, gx = np.mgrid[-sz:sz + 1, -sz:sz + 1].astype(float)
        ca, sa = math.cos(ang), math.sin(ang)
        u = gx * ca + gy * sa
        v = -gx * sa + gy * ca
        width = W * np.sqrt(np.clip(1 - (u / L_) ** 2, 0, 1))
        mask = np.abs(v) < width
        sh = (.82 + .3 * (u / L_ + 1) * .5 - .25 * (v / (W + .01)) * (1 if sa > 0 else -1))[..., None]
        c = col * sh
        if midrib:
            c = np.where((np.abs(v) < .55)[..., None], col * 1.25, c)
        idx = np.ix_(ys, xs)
        patch = img[idx]
        img[idx] = np.where(mask[..., None], c, patch)
    for _ in range(1500):
        stamp(r.uniform(0, N), r.uniform(0, N), r.uniform(5, 9), r.uniform(2, 3.6), r.uniform(0, 6.28),
              pal[r.integers(0, len(pal))] * r.uniform(.72, 1.15))
    if flowers:
        for _ in range(70):
            cx, cy = r.uniform(0, N), r.uniform(0, N)
            col = rgb(flowers[r.integers(0, len(flowers))]) * r.uniform(.85, 1.1)
            for k in range(5):
                a = k * 1.2566 + r.uniform(0, .3)
                stamp(cx + math.cos(a) * 3.2, cy + math.sin(a) * 3.2, 3.6, 2.2, a, col, False)
            stamp(cx, cy, 1.8, 1.8, 0, rgb('#f2cf4a'), False)
    return img


def cloth(base, seed, damask=None):
    c = rgb(base)
    w = (((X // 2) + (Y // 2)) % 2) * .09 + .955
    folds = fbm(4, seed, 3, fy=1)
    th = pn(128, 128, seed + 1)
    alb = c[None, None] * w[..., None] * (.68 + .62 * folds[..., None]) * (.94 + .12 * th[..., None])
    h = folds * 2.4
    if damask:
        u, v = (((X + 64) % 128) - 64) / 64, (((Y + 64) % 128) - 64) / 64
        s = np.abs(u) + np.abs(v)
        rr = np.sqrt(u * u + v * v)
        m = ((s < .86) & (s > .75)) | ((rr < .4) & (rr > .29)) | ((np.abs(u) < .06) & (np.abs(v) < .78)) | ((np.abs(v) < .06) & (np.abs(u) < .78)) | (rr < .12)
        # petites fleurs de lys aux coins
        uu, vv = (((X) % 128) - 64) / 64, (((Y) % 128) - 64) / 64
        s2 = np.abs(uu) * .9 + np.abs(vv) * .9
        m = m | (s2 < .14)
        g = rgb(damask)[None, None] * (.72 + .5 * fbm(20, seed + 3, 2)[..., None])
        alb = np.where(m[..., None], g * w[..., None], alb)
        h = h + np.where(m, .9, 0)
    return alb * relief(h, .35)


def water(seed):
    a, b = fbm(4, seed, 5), fbm(5, seed + 3, 5)
    r = np.abs(np.sin(2 * np.pi * (X / N + Y / N) + a * 10)) * np.abs(np.sin(2 * np.pi * (2 * X / N - Y / N) + b * 11))
    v = np.clip(.25 + .4 * b + .42 * r ** 3, 0, 1)
    return ramp(v, [(0, '#123f76'), (.4, '#2b78b8'), (.75, '#62b8e0'), (1, '#e0f6ff')])


def flame(seed):
    n = fbm(5, seed, 5, fy=2)
    t = (Y / N)
    tong = np.sin(2 * np.pi * (X / N * 4) + n * 9) * .5 + .5
    w = (np.sin(2 * np.pi * t) * .5 + .5)
    v = np.clip(.2 + .55 * n + .35 * tong * w + .35 * (1 - w) * n, 0, 1)
    return ramp(v, [(0, '#b02408'), (.32, '#ee5a10'), (.58, '#ffa020'), (.8, '#ffe066'), (1, '#fffbe0')])


def wax(base, seed):
    c = rgb(base)
    a = fbm(5, seed, 4)
    drip = fbm(14, seed + 2, 2, fy=2)
    return c[None, None] * (.86 + .22 * a[..., None]) * relief(a + drip * .7, .5)


def leather(base, seed):
    c = rgb(base)
    d1, d2, _ = voronoi(120, seed)
    cr = sstep(0, 5, d2 - d1)
    a = fbm(5, seed + 1, 4)
    alb = c[None, None] * (.72 + .55 * a[..., None]) * (.66 + .4 * cr[..., None])
    return alb * relief(cr * 1.2, .6)


def crystal(c0, c1, c2, seed):
    d1, d2, ids = voronoi(16, seed)
    r = np.random.default_rng(seed + 1)
    t = r.uniform(.2, .9, 16)[ids]
    ang = r.uniform(0, 6.28, 16)[ids]
    grad = ((X * np.cos(ang) + Y * np.sin(ang)) % 64) / 64 * .25
    v = np.clip(t + grad - .1, 0, 1)
    alb = ramp(v, [(0, c0), (.5, c1), (1, c2)])
    e = d2 - d1
    alb = np.where((e < 2.2)[..., None], np.clip(alb * 1.35 + 40, 0, 255), alb)
    alb = alb * (.95 + .12 * fbm(20, seed + 2, 2)[..., None])
    return alb


def slate(base, seed, rows=8):
    c = rgb(base)
    r = np.random.default_rng(seed)
    rh = N // rows
    ro = (Y // rh).astype(int)
    cw = 32
    xs = X + (ro % 2) * (cw // 2)
    col = (xs // cw).astype(int) % (N // cw)
    lx = xs % cw
    ly = Y % rh
    tones = r.uniform(.72, 1.18, (rows, N // cw))
    tone = tones[ro, col]
    rnd = np.abs(lx - cw / 2) / (cw / 2)
    bottom = rh - 2 - (rnd ** 3) * 8
    gap = (ly > bottom) | (lx < 1.2) | (lx > cw - 1.2)
    a = fbm(16, seed + 1, 4)
    alb = c[None, None] * tone[..., None] * (.76 + .5 * a[..., None])
    alb = mix(alb, alb * (.8 + .35 * (ly / rh))[..., None] * 1.0, 1.0) if False else alb * (1.1 - .3 * (ly / rh))[..., None]
    h = -(ly / rh) * 1.4 + a * .5
    alb = np.where(gap[..., None], c[None, None] * .22, alb)
    return alb * relief(np.where(gap, h - 1.2, h), .6)


def weathered(base, seed, streaks=False):
    """Pierre sculptee patinee : creux presque noirs, reliefs usés, fines fissures."""
    c = rgb(base)
    a, b = fbm(5, seed, 6), fbm(24, seed + 2, 4)
    alb = c[None, None] * (.8 + .36 * a[..., None]) * (.9 + .18 * b[..., None])
    grime = sstep(.5, .66, fbm(7, seed + 3, 5))
    v = np.abs(fbm(5, seed + 5, 5) - .5)
    h = a * 1.4 + b * .6 - grime * .6
    if streaks:
        ribs = np.sin(2 * np.pi * (X / N * 7) + fbm(4, seed + 6, 3) * 6)
        grime = np.clip(grime * .5 + sstep(.55, .95, ribs) * .75, 0, 1)
        h = h + ribs * .3
    alb = mix(alb, rgb('#16242a')[None, None], grime * .3)
    alb = mix(alb, rgb('#0a0f12')[None, None], (v < .007) * .55)
    return alb * relief(h, .42)


def glow(base, seed):
    c = rgb(base)
    a = fbm(4, seed, 4)
    return np.clip(c[None, None] * (.9 + .22 * a[..., None]) + 8, 0, 255)


TILES = {
    'pierre_grise': lambda: ashlar('#92918c', 11, moss=.35),
    'pierre_claire': lambda: ashlar('#bfb6a2', 12),
    'pierre_sombre': lambda: ashlar('#5c5b60', 13, moss=.25),
    'pierre_violette': lambda: ashlar('#74668f', 14),
    'lisse_grise': lambda: smooth_stone('#94938e', 21),
    'lisse_claire': lambda: smooth_stone('#c2baa8', 22),
    'lisse_sombre': lambda: smooth_stone('#5e5d63', 23),
    'lisse_violette': lambda: smooth_stone('#786a94', 24),
    'marbre_blanc': lambda: marble('#ece7dc', '#8f8a80', 31),
    'marbre_gris': lambda: marble('#c4bdb0', '#6e695f', 32),
    'marbre_violet': lambda: marble('#6e4aa8', '#e6d2a0', 33),
    'marbre_nuit': lambda: marble('#241a44', '#c9a24a', 34),
    'bois_chene': lambda: planks('#8a5a30', 41),
    'bois_sombre': lambda: planks('#4c2f1c', 42),
    'bois_violet': lambda: planks('#3e2758', 43),
    'or_lisse': lambda: gold(51),
    'or_ouvrage': lambda: gold(52, ornate=True),
    'or_sombre': lambda: gold(53, base='#a88020'),
    'fer_forge': lambda: iron('#4b4e57', 61),
    'fer_noir': lambda: iron('#2b2c32', 62),
    'fer_plaque': lambda: iron('#55585f', 63, plate=True),
    'acier': lambda: steel('#c0c4cc', 64),
    'vitrail_violet': lambda: glass(['#8a4fe0', '#b58aff', '#6a32c8', '#d0b0ff', '#ffd86a'], 71),
    'vitrail_or': lambda: glass(['#ffd86a', '#ffb838', '#ffe9a0', '#e89a20', '#b58aff'], 72),
    'vitrail_mixte': lambda: glass(['#8a4fe0', '#ffd86a', '#3a6ae0', '#d03838', '#58b858', '#b58aff'], 73),
    'feuillage': lambda: foliage(81),
    'feuillage_clair': lambda: foliage(82, leafs=('#4f9a34', '#6ab240', '#83c44c', '#3f8a2e')),
    'feuillage_fleuri': lambda: foliage(83, flowers=['#d84a8a', '#e87ab0', '#f0d8f0', '#a050d8']),
    'tissu_violet': lambda: cloth('#5e2f96', 91),
    'tissu_rouge': lambda: cloth('#9a2424', 92),
    'tissu_creme': lambda: cloth('#e6dcc0', 93),
    'tissu_vert': lambda: cloth('#3f7a38', 94),
    'tissu_or': lambda: cloth('#c79a34', 95),
    'damas_violet': lambda: cloth('#54288a', 96, damask='#e6b84a'),
    'damas_rouge': lambda: cloth('#8c1c20', 97, damask='#e6b84a'),
    'damas_bleu': lambda: cloth('#20408c', 98, damask='#dfe4f0'),
    'eau': lambda: water(101),
    'flamme': lambda: flame(102),
    'cire': lambda: wax('#f2ecd8', 103),
    'os': lambda: wax('#ddd2b4', 104),
    'cuir_brun': lambda: leather('#6e4426', 105),
    'cuir_sombre': lambda: leather('#3a2a2e', 106),
    'amethyste': lambda: crystal('#3e1c78', '#8a4fe0', '#e8d4ff', 111),
    'cristal_rose': lambda: crystal('#7a1c70', '#d860c0', '#ffe0f4', 112),
    'cristal_bleu': lambda: crystal('#143c88', '#4a9ae8', '#dcf2ff', 113),
    'ardoise_violette': lambda: slate('#5a4a82', 121),
    'ardoise_grise': lambda: slate('#50545c', 122),
    'tuile_rouge': lambda: slate('#9a4a30', 123),
    'lumiere_violette': lambda: glow('#dcc0ff', 131),
    'lumiere_or': lambda: glow('#ffe39a', 132),
    'lumiere_blanche': lambda: glow('#f0ecff', 133),
    'sable': lambda: smooth_stone('#d8bc80', 141),
    'gargouille_bleue': lambda: weathered('#46707e', 151),
    'gargouille_sombre': lambda: weathered('#27424c', 152),
    'gargouille_membrane': lambda: weathered('#37606e', 153, streaks=True),
}


def slots():
    """Nom de tuile -> (x, y) en pixels dans l'atlas."""
    return {name: (i % COLS * N, i // COLS * N) for i, name in enumerate(TILES)}


def atlas_size():
    rows = math.ceil(len(TILES) / COLS)
    return COLS * N, rows * N


def build(force=False):
    if ATLAS_FILE.exists() and not force:
        return Image.open(ATLAS_FILE).convert('RGB')
    TILES_DIR.mkdir(parents=True, exist_ok=True)
    w, h = atlas_size()
    atlas = Image.new('RGB', (w, h))
    pos = slots()
    for name, fn in TILES.items():
        im = Image.fromarray(np.clip(fn(), 0, 255).astype(np.uint8), 'RGB')
        im.save(TILES_DIR / f'{name}.png', optimize=True)
        atlas.paste(im, pos[name])
    ATLAS_FILE.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(ATLAS_FILE, optimize=True)
    return atlas


# ------------------------------------------------------------------ materiau du modele -> tuile
def _nearest(color, table):
    c = rgb(color)
    return min(table, key=lambda k: float(np.sum((rgb(k[0]) - c) ** 2)))[1]


def tile_for(mat, dims):
    """dims = dimensions du cube. Petites pieces : matiere sans joints ni motif trop large."""
    kind = mat[0]
    if kind == 'tile':
        return mat[1]
    col = mat[1] if len(mat) > 1 and isinstance(mat[1], str) else None
    s = sorted(dims)
    small = s[1] < 14
    large = s[1] >= 20
    if kind == 'stone':
        if col and col.lower() in ('#6a3fa8', '#3f6bb8'):
            return 'ardoise_violette'
        name = _nearest(col or '#92918c', [('#92918c', 'grise'), ('#6f737b', 'sombre'), ('#5f5878', 'sombre'), ('#b8b2a4', 'claire'), ('#8a82a0', 'violette')])
        if col and col.lower() == '#d8b060':
            return 'sable'
        return ('lisse_' if small else 'pierre_') + name
    if kind == 'marble':
        return _nearest(col or '#ece7dc', [('#ece7dc', 'marbre_blanc'), ('#c9c2b4', 'marbre_gris'), ('#6e4aa8', 'marbre_violet'), ('#241a44', 'marbre_nuit')])
    if kind == 'wood':
        return _nearest(col or '#6b4528', [('#6b4528', 'bois_chene'), ('#4a2e1a', 'bois_sombre'), ('#3e2758', 'bois_violet'), ('#2d1b45', 'bois_violet')])
    if kind == 'gold':
        if col and col.lower() == '#a88020':
            return 'or_sombre'
        return 'or_ouvrage' if large else 'or_lisse'
    if kind == 'metal':
        lum = float(np.mean(rgb(col or '#4b4e57')))
        if lum > 125:
            return 'acier'
        if lum < 60:
            return 'fer_noir'
        return 'fer_plaque' if large else 'fer_forge'
    if kind == 'glowc':
        c = rgb(col or '#b78aff')
        h, l, sa = colorsys.rgb_to_hls(*(c / 255))
        if sa < .4 or l > .86:
            return 'lumiere_blanche'
        if .08 < h < .17:
            return 'vitrail_or' if large or not small else 'lumiere_or'
        return 'vitrail_violet' if not small or l < .72 else 'lumiere_violette'
    if kind == 'flame':
        if col and col.lower() == '#e8c070':
            return 'sable'
        return 'flamme'
    if kind == 'water':
        return 'eau'
    if kind == 'crystal':
        return _nearest(col or '#b070f0', [('#b070f0', 'amethyste'), ('#e070d0', 'cristal_rose'), ('#62b0ff', 'cristal_bleu')])
    if kind == 'banner':
        return _nearest(col or '#8a1e1e', [('#5a2a8a', 'damas_violet'), ('#8a1e1e', 'damas_rouge'), ('#1e3f8a', 'damas_bleu'), ('#e0b040', 'tissu_or'), ('#5a9a3a', 'tissu_vert'),
                                          ('#c03020', 'tissu_rouge'), ('#c03a8a', 'feuillage_fleuri')])
    if kind in ('cloth_plain', 'cloth'):
        return _nearest(col or '#a82a2a', [('#6a3fa8', 'tissu_violet'), ('#a82a2a', 'tissu_rouge'), ('#f0e8d0', 'tissu_creme')])
    if kind == 'bone':
        return 'cire' if float(np.mean(rgb(col or '#f4efe0'))) > 225 else 'os'
    if kind == 'leather':
        return _nearest(col or '#6b4024', [('#6b4024', 'cuir_brun'), ('#3a2358', 'cuir_sombre')])
    if kind == 'leaf':
        return 'feuillage'
    if kind == 'dark':
        return 'lisse_sombre'
    return 'lisse_grise'


if __name__ == '__main__':
    a = build(force=True)
    print(a.size, ATLAS_FILE.stat().st_size // 1024, 'Ko')
