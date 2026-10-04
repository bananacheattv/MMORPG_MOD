# -*- coding: utf-8 -*-
"""Matieres et decorations communes aux structures multiblocs (autel, teleporteur, forge) :
pierre noire maconnee alignee sur le monde (les joints se raccordent d'un cube a l'autre), renforts argentes,
panneaux de runes violettes lumineuses, cristaux, lignes d'energie au sol.

Convention : la structure regarde vers -Z (face avant, escaliers) ; 'gauche' / 'droite' sont vus de face,
par quelqu'un qui se tient devant la structure : gauche = +X, droite = -X. Le bloc fonctionnel occupe
x -8..8, z -8..8, y 0..16 (origine au centre de sa face inferieure)."""
import hashlib
import math

import numpy as np

from entity_kit import pal, Mat, stamp, BAYER

PIERRE = pal('#141319', '#1c1b23', '#25242d', '#2f2e39', '#3a3946', '#474655')
ARGENT = pal('#575a64', '#767a85', '#959aa4', '#b6bac2', '#d9dbe1')
RUNE = pal('#5a22c8', '#7c3cff', '#a06eff', '#cdb2ff')
CRISTAL_V = pal('#3a1688', '#5527c4', '#7845f2', '#9d74ff', '#c6adff', '#ece2ff')
CRISTAL_B = pal('#1b2a8f', '#2a45d0', '#4470ff', '#7aa0ff', '#b8ccff', '#e6eeff')
BOIS = pal('#22160e', '#322114', '#432e1b', '#553b23', '#694b2d')
FER = pal('#1d1f25', '#2a2d35', '#393d46', '#4b505b', '#626874', '#7f8592')
FLAMME = pal('#2a1890', '#432ed8', '#6850ff', '#9a84ff', '#d4caff')
BRAISE = pal('#4a1505', '#97320b', '#df6212', '#ffab3d', '#ffe08a')
PORTAIL = pal('#150c58', '#24178f', '#3523c8', '#4e3ce8', '#7766ff', '#b4a8ff')
GLOW = {'r': RUNE[1], 'R': RUNE[3], 'm': RUNE[2]}


def h01(*key):
    """Valeur pseudo-aleatoire stable dans [0, 1)."""
    return int(hashlib.md5(repr(key).encode()).hexdigest()[:8], 16) / 4294967296.0


class Masonry:
    """Pierre maconnee alignee sur le monde : briques bw x bh sur les faces laterales, dalles 'tile' x 'tile'
    sur le dessus et le dessous, joints sombres, chaque brique avec sa propre nuance."""

    def __init__(self, palette=PIERRE, level=0.45, var=0.16, noise=0.12, bw=8, bh=4, tile=8, top=0.06, seed=0, decos=()):
        self.pal, self.level, self.var, self.noise = palette, level, var, noise
        self.bw, self.bh, self.tile, self.top, self.seed, self.decos = bw, bh, tile, top, seed, list(decos)

    def paint(self, f):
        n = len(self.pal)
        for y in range(f.h):
            for x in range(f.w):
                wx, wy, wz = f.world(x, y)
                if f.side:
                    u = wx if f.name in ('north', 'south') else wz
                    row = math.floor(wy / self.bh)
                    shift = (self.bw // 2) * (row % 2)
                    mortar = math.floor(wy) % self.bh == 0 or (math.floor(u) + shift) % self.bw == 0
                    brick = (row, math.floor((u + shift) / self.bw), f.name in ('north', 'south'))
                    lv = self.level + (0.12 * (0.5 - (wy % self.bh) / self.bh))
                else:
                    mortar = math.floor(wx) % self.tile == 0 or math.floor(wz) % self.tile == 0
                    brick = (math.floor(wx / self.tile), math.floor(wz / self.tile), 'dalle')
                    lv = self.level + (self.top if f.name == 'up' else -0.2)
                lv += self.var * (h01(self.seed, *brick) - 0.5) * 2 + self.noise * (f.rng.random() - 0.5)
                idx = 0 if mortar else int(np.clip(math.floor(lv * (n - 1) + BAYER[y % 4, x % 4]), 1, n - 1))
                f.px(x, y, self.pal[idx])
        for d in self.decos:
            d(f)


def silver(level=0.55, rivet=False):
    decos = []
    if rivet:
        def rv(f):
            if f.side and f.w >= 5 and f.h >= 5:
                for x, y in ((1, 1), (f.w - 2, 1), (1, f.h - 2), (f.w - 2, f.h - 2)):
                    f.px(x, y, ARGENT[4])
                    f.px(x + 1, y + 1, ARGENT[0])
        decos.append(rv)
    return Mat(ARGENT, level=level, noise=0.14, scale=1.5, grad=0.25, rim=0.18, decos=decos)


def crystal(palette=CRISTAL_V, level=0.6):
    def shine(f):
        if f.side and f.w >= 2:
            for y in range(1, max(1, f.h - 1)):
                f.px(1, y, palette[-1], glow=True)
    return Mat(palette, level=level, noise=0.22, scale=1.2, grad=0.35, up=0.25, down=-0.15, glow=True, decos=[shine])


GLYPHES = [
    ['.r.', 'rrr', '.r.', 'r.r', '.r.', 'rr.', '.r.', '.rr', '.r.'],      # rune verticale
    ['..r..', '.rRr.', 'rR.Rr', '.rRr.', '..r..'],                       # losange
    ['r.r', '.r.', 'rRr', '.r.', 'r.r'],                                 # croix
    ['rr.', '.r.', '.rr', 'r..', 'rr.', '.r.'],                          # serpentin
    ['.r.', 'r.r', '.R.', 'r.r', '.r.'],
]


def rune_panel(glyph=0, faces=('north', 'south', 'east', 'west'), margin=2, frame=True):
    """Panneau sombre encadre d'argent avec une rune violette lumineuse (le motif le plus grand qui tient)."""
    def d(f):
        if f.name not in faces or f.w < 2 * margin + 3 or f.h < 2 * margin + 3:
            return
        for y in range(margin, f.h - margin):
            for x in range(margin, f.w - margin):
                edge = frame and (x in (margin, f.w - margin - 1) or y in (margin, f.h - margin - 1))
                f.px(x, y, ARGENT[2] if edge else PIERRE[0])
        order = [glyph] + [i for i in range(len(GLYPHES)) if i != glyph]
        for g in order:
            rows = GLYPHES[g]
            if len(rows[0]) <= f.w - 2 * margin - 2 and len(rows) <= f.h - 2 * margin - 2:
                stamp(rows, GLOW, glow='rRm')(f)
                return
    return d


def trim(sides='lr', color=None):
    """Arretes argentees sur les faces laterales."""
    def d(f):
        if not f.side:
            return
        c = color or ARGENT[2]
        for y in range(f.h):
            if 'l' in sides:
                f.px(0, y, c)
            if 'r' in sides:
                f.px(f.w - 1, y, c)
        for x in range(f.w):
            if 't' in sides:
                f.px(x, 0, ARGENT[3])
            if 'b' in sides:
                f.px(x, f.h - 1, ARGENT[1])
    return d


def floor_lines(segments, faces=('up',), width=1.0, color=None, core=None):
    """Lignes d'energie lumineuses tracees en coordonnees monde (x0, z0, x1, z1) sur le dessus des cubes."""
    col, cor = color or RUNE[1], core or RUNE[2]

    def d(f):
        if f.name not in faces:
            return
        for y in range(f.h):
            for x in range(f.w):
                wx, _, wz = f.world(x, y)
                for (x0, z0, x1, z1) in segments:
                    dx, dz = x1 - x0, z1 - z0
                    L = dx * dx + dz * dz
                    t = 0.0 if L == 0 else max(0.0, min(1.0, ((wx - x0) * dx + (wz - z0) * dz) / L))
                    dist = math.hypot(wx - (x0 + t * dx), wz - (z0 + t * dz))
                    if dist <= width / 2 + 0.01:
                        f.px(x, y, cor if (math.floor(wx) + math.floor(wz)) % 3 == 0 else col, glow=True)
                        break
    return d


def stripes_x(xs, faces=('north', 'up'), width=2):
    """Bandes lumineuses verticales (faces avant) ou longitudinales (dessus) aux abscisses monde xs."""
    def d(f):
        if f.name not in faces:
            return
        for y in range(f.h):
            for x in range(f.w):
                wx = f.world(x, y)[0]
                if any(x0 <= wx < x0 + width for x0 in xs):
                    f.px(x, y, RUNE[2] if y % 3 else RUNE[3], glow=True)
    return d


def decal(*decos):
    """Matiere 'decalque' : tout transparent sauf les motifs (plans de portail, flammes)."""
    class _D:
        def paint(self, f):
            for y in range(f.h):
                for x in range(f.w):
                    f.clear(x, y)
            for dd in decos:
                dd(f)
    return _D()


def base_panels(step=16, width=8, margin=2, faces=('north', 'south', 'east', 'west')):
    """Petits panneaux runiques encastres a intervalles reguliers (alignes sur le monde) le long d'un soubassement."""
    def d(f):
        if f.name not in faces or f.h < 2 * margin + 4:
            return
        for x in range(f.w):
            wx, _, wz = f.world(x, 0)
            u = (wx if f.name in ('north', 'south') else wz) + 8
            k = u % step
            if (step - width) / 2 <= k < (step + width) / 2:
                edge = k < (step - width) / 2 + 1 or k >= (step + width) / 2 - 1
                for y in range(margin, f.h - margin):
                    rim = edge or y in (margin, f.h - margin - 1)
                    f.px(x, y, ARGENT[1] if rim else PIERRE[0])
                    mid = (step - width) / 2 + width / 2
                    if not rim and abs(k - mid + 0.5) < 1.1 and (y - margin) % 2 == 1:
                        f.px(x, y, RUNE[1] if abs(k - mid + 0.5) > 0.6 else RUNE[2], glow=True)
    return d
