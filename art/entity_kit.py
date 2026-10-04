# -*- coding: utf-8 -*-
"""
Kit commun aux generateurs d'entites animees (boss, familiers) pour Blockbench, format "Modded Entity".

Ce que le kit garantit pour chaque modele (verifie par check()) :
  - box UV (texOffs Java), tailles de cubes entieres, une seule texture ; le gonflement (inflate) sert aux epaisseurs ;
  - noms d'os et d'animations en [a-z0-9_] (identifiants Java valides, compatibles GeckoLib) ;
  - animations limitees a ce que savent lire TOUS les systemes cibles : canaux rotation / position / echelle,
    interpolations lineaire et catmullrom (Java vanilla AnimationDefinition, GeckoLib, Bedrock) ;
  - fichier .bbmodel au format 4.10 (ouvrable dans Blockbench 4.10+ et 5.x).

Conventions (repere Blockbench) : Y vers le haut, sol en y = 0, la creature regarde vers -Z (nord),
son cote DROIT est en +X. Les rotations s'appliquent dans l'ordre Z, Y, X (comme ModelPart en Java).
Dans ce kit, les valeurs d'animation sont exprimees dans la convention interne de Blockbench 5 :
  rotation +X : un os vertical bascule vers l'arriere, un bras pendant se leve vers l'avant ;
  rotation +Y : la creature tourne vers SA gauche ;  rotation +Z : un bras droit se leve sur le cote,
  un os vertical penche vers la gauche de la creature ;  position : +X droite, +Y haut, -Z avant.
L'ecriture au format 4.10 convertit (rotation x, y et position x inversees), comme Blockbench lui-meme.
"""
import base64
import io
import json
import math
import os
import random
import re
import uuid
import zlib

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

FACES = ('north', 'east', 'south', 'west', 'up', 'down')
BAYER = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]], dtype=float) / 16.0
NAME_RE = re.compile(r'^[a-z][a-z0-9_]*$')


def crc(*key):
    return zlib.crc32(repr(key).encode('utf-8'))


def hexc(s):
    s = s.lstrip('#')
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def lerp(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a + (b - a) * t)) for a, b in zip(c1, c2))


def mul(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c[:3])


def pal(*cols):
    """Palette ordonnee du plus sombre au plus clair."""
    return [hexc(c) if isinstance(c, str) else tuple(c) for c in cols]


# ================================================================================================ modele
class Bone:
    def __init__(self, name, pivot, rot=(0, 0, 0), parent=None):
        self.name, self.pivot, self.rot, self.parent = name, [float(v) for v in pivot], [float(v) for v in rot], parent
        self.cubes, self.children = [], []
        if parent is not None:
            parent.children.append(self)


class Cube:
    def __init__(self, bone, name, frm, size, mat, deco=None, inflate=0.0, mirror=False, share=None, rot=None, origin=None):
        for s in size:
            assert abs(s - round(s)) < 1e-9 and s >= 0, (name, size)          # box UV : tailles entieres
        self.bone, self.name = bone, name
        self.frm = [round(float(v), 4) for v in frm]
        self.size = [int(round(s)) for s in size]
        self.mat, self.deco = mat, deco or {}
        self.inflate, self.mirror, self.share = float(inflate), bool(mirror), share
        self.rot = [float(v) for v in rot] if rot else None
        self.origin = [float(v) for v in origin] if origin else None
        self.uv_offset = None

    @property
    def to(self):
        return [round(self.frm[i] + self.size[i], 4) for i in range(3)]


class Model:
    """Modele d'entite : arbre d'os, cubes en box UV, animations."""

    def __init__(self, key, title, tex_scale=1):
        self.key, self.title = key, title
        self.bones, self.order = {}, []
        self.roots = []
        self.animations = []
        self.notes = []

    def bone(self, name, pivot, parent=None, rot=(0, 0, 0)):
        assert name not in self.bones, name
        p = self.bones[parent] if isinstance(parent, str) else parent
        b = Bone(name, pivot, rot, p)
        self.bones[name] = b
        self.order.append(b)
        if p is None:
            self.roots.append(b)
        return b

    def cube(self, bone, name, frm, size, mat, **kw):
        b = self.bones[bone] if isinstance(bone, str) else bone
        c = Cube(b, name, frm, size, mat, **kw)
        b.cubes.append(c)
        return c

    def cubes(self):
        return [c for b in self.order for c in b.cubes]

    def anim(self, name, length, loop='loop'):
        a = Animation(name, length, loop)
        self.animations.append(a)
        return a

    def mirror_x(self, name):
        """Nom du membre symetrique (droit <-> gauche)."""
        for a, b in (('droite', 'gauche'), ('droit', 'gauche')):
            if a in name:
                return name.replace(a, b)
        return name.replace('gauche', 'droite') if 'gauche' in name else name


# ================================================================================================ animations
class Animation:
    """Images-cles par os et par canal. Valeurs dans la convention interne de Blockbench 5 (voir en-tete)."""

    def __init__(self, name, length, loop):
        assert NAME_RE.match(name), name
        assert loop in ('loop', 'once', 'hold')
        self.name, self.length, self.loop = name, float(length), loop
        self.ch = {}          # (bone, channel) -> [(t, (x, y, z), interp)]

    def key(self, bone, channel, frames, interp='catmullrom'):
        lst = self.ch.setdefault((bone, channel), [])
        for fr in frames:
            t, v = fr[0], fr[1:4]
            it = fr[4] if len(fr) > 4 else interp
            lst.append((round(float(t), 4), tuple(float(x) for x in v), it))
        lst.sort(key=lambda k: k[0])
        return self

    def rot(self, bone, frames, interp='catmullrom'):
        return self.key(bone, 'rotation', frames, interp)

    def pos(self, bone, frames, interp='catmullrom'):
        return self.key(bone, 'position', frames, interp)

    def scale(self, bone, frames, interp='catmullrom'):
        return self.key(bone, 'scale', frames, interp)

    def sample(self, bone, channel, t):
        ks = self.ch.get((bone, channel))
        if not ks:
            return None
        if self.loop == 'loop' and self.length > 0:
            t = t % self.length if t < self.length or t > self.length else self.length
        if t <= ks[0][0]:
            return ks[0][1]
        if t >= ks[-1][0]:
            return ks[-1][1]
        for i in range(len(ks) - 1):
            t0, v0, i0 = ks[i]
            t1, v1, i1 = ks[i + 1]
            if t0 <= t <= t1:
                a = (t - t0) / (t1 - t0) if t1 > t0 else 1.0
                if i1 == 'catmullrom':
                    vp = ks[i - 1][1] if i > 0 else v0
                    vn = ks[i + 2][1] if i + 2 < len(ks) else v1
                    return tuple(catmull(vp[k], v0[k], v1[k], vn[k], a) for k in range(3))
                return tuple(v0[k] + (v1[k] - v0[k]) * a for k in range(3))
        return ks[-1][1]


def catmull(p0, p1, p2, p3, t):
    t2, t3 = t * t, t * t * t
    return 0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3)


# ================================================================================================ textures
class Face:
    """Zone de texture d'une face (box UV). Repere de la zone : colonne 0 a gauche, ligne 0 en haut.
    Correspondance avec le monde (cube from = x0, y0, z0 ; taille w, h, d) :
      north : col -> x decroissant (col 0 = cote droit de la creature, +X), ligne -> y decroissant
      south : col -> x croissant ;  east : col -> z decroissant ;  west : col -> z croissant
      up / down : col -> x decroissant, ligne -> z decroissant (ligne 0 = bord arriere, +Z)."""

    def __init__(self, tex, emi, u, v, w, h, name, cube, rng):
        self.tex, self.emi = tex, emi
        self.u, self.v, self.w, self.h = u, v, w, h
        self.name, self.cube, self.rng = name, cube, rng
        self.side = name not in ('up', 'down')

    def ok(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h

    def px(self, x, y, c, glow=False, a=255):
        x, y = int(x), int(y)
        if self.ok(x, y):
            self.tex[self.v + y, self.u + x] = (c[0], c[1], c[2], a)
            if glow:
                self.emi[self.v + y, self.u + x] = (c[0], c[1], c[2], 255)
            else:
                self.emi[self.v + y, self.u + x] = (0, 0, 0, 0)

    def get(self, x, y):
        return tuple(int(v) for v in self.tex[self.v + y, self.u + x])

    def clear(self, x, y):
        if self.ok(x, y):
            self.tex[self.v + y, self.u + x] = (0, 0, 0, 0)
            self.emi[self.v + y, self.u + x] = (0, 0, 0, 0)

    def world(self, i, j):
        """Coordonnees monde du centre du texel (i, j) de la face (avant gonflement)."""
        (x0, y0, z0), (w, h, d) = self.cube.frm, self.cube.size
        hi, vj = i + 0.5, j + 0.5
        if self.name == 'north':
            return (x0 + w - hi, y0 + h - vj, z0)
        if self.name == 'south':
            return (x0 + hi, y0 + h - vj, z0 + d)
        if self.name == 'east':
            return (x0 + w, y0 + h - vj, z0 + d - hi)
        if self.name == 'west':
            return (x0, y0 + h - vj, z0 + hi)
        if self.name == 'up':
            return (x0 + w - hi, y0 + h, z0 + d - vj)
        return (x0 + w - hi, y0, z0 + d - vj)


def value_noise(w, h, scale, rng, octaves=2):
    """Bruit lisse dans [-1, 1]. scale = (sx, sy) ou nombre : taille des taches en texels."""
    sx, sy = (scale, scale) if not isinstance(scale, (tuple, list)) else scale
    out = np.zeros((h, w))
    amp, tot = 1.0, 0.0
    for _ in range(octaves):
        gw, gh = int(w / max(sx, 0.5)) + 3, int(h / max(sy, 0.5)) + 3
        grid = np.array([[rng.uniform(-1, 1) for _ in range(gw)] for _ in range(gh)])
        ox, oy = rng.uniform(0, 1), rng.uniform(0, 1)
        xs = np.arange(w) / max(sx, 0.5) + ox
        ys = np.arange(h) / max(sy, 0.5) + oy
        x0, y0 = np.floor(xs).astype(int), np.floor(ys).astype(int)
        fx, fy = xs - x0, ys - y0
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        a = grid[np.ix_(y0, x0)]
        b = grid[np.ix_(y0, x0 + 1)]
        c = grid[np.ix_(y0 + 1, x0)]
        d = grid[np.ix_(y0 + 1, x0 + 1)]
        top = a + (b - a) * fx[None, :]
        bot = c + (d - c) * fx[None, :]
        out += amp * (top + (bot - top) * fy[:, None])
        tot += amp
        amp *= 0.5
        sx, sy = sx / 2, sy / 2
    return out / tot


class Mat:
    """Matiere pixel art : niveaux de gris -> palette, avec tramage ordonne (Bayer 4x4).
    level : niveau moyen (0 = plus sombre, 1 = plus clair) ; noise / scale : variations en taches ;
    grad : degrade vertical des faces laterales (haut plus clair) ; up / down : decalage des faces du dessus / dessous ;
    rim : liseres (bord haut eclairci, autres bords assombris) ; decos : decorations appliquees a toutes les faces."""

    def __init__(self, palette, level=0.5, noise=0.3, scale=3.0, grad=0.15, up=0.15, down=-0.25, rim=0.0,
                 dither=True, glow=False, decos=(), octaves=2, alpha=255, speck=0.0):
        self.pal, self.level, self.noise, self.scale = palette, level, noise, scale
        self.grad, self.up, self.down, self.rim = grad, up, down, rim
        self.dither, self.glow, self.decos, self.octaves, self.alpha, self.speck = dither, glow, list(decos), octaves, alpha, speck

    def levels(self, f):
        w, h = f.w, f.h
        lv = np.full((h, w), float(self.level))
        if self.noise:
            lv += self.noise * value_noise(w, h, self.scale, f.rng, self.octaves)
        if f.side:
            if h > 1 and self.grad:
                lv += self.grad * (0.5 - np.arange(h) / (h - 1))[:, None]
        else:
            lv += self.up if f.name == 'up' else self.down
        if self.rim:
            if h >= 3:
                lv[0, :] += self.rim
                lv[-1, :] -= self.rim
            if w >= 3:
                lv[:, 0] -= self.rim * 0.6
                lv[:, -1] -= self.rim * 0.6
        if self.speck:
            mask = np.array([[f.rng.random() < self.speck for _ in range(w)] for _ in range(h)])
            lv[mask] += np.array([f.rng.choice((-0.35, 0.3)) for _ in range(int(mask.sum()))])
        return lv

    def paint(self, f):
        if f.w <= 0 or f.h <= 0:
            return
        lv = self.levels(f)
        n = len(self.pal)
        thr = BAYER[np.arange(f.h)[:, None] % 4, np.arange(f.w)[None, :] % 4] if self.dither else 0.5
        idx = np.clip(np.floor(lv * (n - 1) + thr), 0, n - 1).astype(int)
        for y in range(f.h):
            for x in range(f.w):
                f.px(x, y, self.pal[idx[y, x]], glow=self.glow, a=self.alpha)
        for d in self.decos:
            d(f)

    def tone(self, k):
        return self.pal[max(0, min(len(self.pal) - 1, k))]


def paint_cube(c, tex, emi, rng_key):
    """Peint les 6 zones box UV d'un cube : matiere, puis decorations ('*' = toutes, 'sides' = laterales)."""
    for f in box_faces(c, tex, emi, rng_key):
        c.mat.paint(f)
        for key in ('*', 'sides' if f.side else 'caps', f.name):
            for d in c.deco.get(key, []):
                d(f)


def box_regions(c):
    """Zones box UV (u, v, largeur, hauteur) de chaque face, sans miroir (disposition Blockbench / Java)."""
    w, h, d = c.size
    u, v = c.uv_offset
    return {'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h), 'west': (u + d + w, v + d, d, h),
            'south': (u + 2 * d + w, v + d, w, h), 'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d)}


def box_faces(c, tex, emi, rng_key):
    out = []
    for name, (u, v, w, h) in box_regions(c).items():
        if w > 0 and h > 0:
            out.append(Face(tex, emi, u, v, w, h, name, c, random.Random(crc(rng_key, c.name, name))))
    return out


def box_uv(c):
    """UV des faces exactement comme Blockbench (Cube.updateUV en box UV, miroir compris)."""
    w, h, d = c.size
    lst = [('east', [0, d], [d, h]), ('west', [d + w, d], [d, h]), ('up', [d + w, d], [-w, -d]),
           ('down', [d + 2 * w, 0], [-w, d]), ('south', [2 * d + w, d], [w, h]), ('north', [d, d], [w, h])]
    lst = [[n, list(fr), list(sz)] for n, fr, sz in lst]
    if c.mirror:
        for e in lst:
            e[1][0] += e[2][0]
            e[2][0] *= -1
        lst[0][1], lst[1][1] = lst[1][1], lst[0][1]
        lst[0][2], lst[1][2] = lst[1][2], lst[0][2]
    u0, v0 = c.uv_offset
    return {n: [fr[0] + u0, fr[1] + v0, fr[0] + sz[0] + u0, fr[1] + sz[1] + v0] for n, fr, sz in lst}


# ------------------------------------------------------------------------------------------------ decorations
def stamp(rows, colors, ax='c', ay='c', dx=0, dy=0, glow=''):
    """Motif ASCII ('.' = rien). ax : 'l' / 'c' / 'r', ay : 't' / 'c' / 'b' ; dx, dy : decalage en texels.
    Les motifs sont dessines tels qu'on les voit de face (colonne 0 = gauche de la zone)."""
    def d(f):
        pw, ph = max(len(r) for r in rows), len(rows)
        x0 = {'l': 0, 'c': (f.w - pw) // 2, 'r': f.w - pw}[ax] + dx
        y0 = {'t': 0, 'c': (f.h - ph) // 2, 'b': f.h - ph}[ay] + dy
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch == '.' or ch == ' ':
                    continue
                if ch == '_':
                    f.clear(x0 + i, y0 + j)
                    continue
                f.px(x0 + i, y0 + j, colors[ch], glow=ch in glow)
    return d


def rivets(light, dark, inset=1, every=None, rows=('t', 'b')):
    """Rivets aux coins (et tous les 'every' texels le long des bords haut / bas)."""
    def d(f):
        if f.w < 2 * inset + 2 or f.h < 2 * inset + 2:
            return
        xs = [inset, f.w - 1 - inset]
        if every:
            xs = list(range(inset, f.w - inset, every))
        for r in rows:
            y = inset if r == 't' else f.h - 1 - inset
            for x in xs:
                f.px(x, y, light)
                f.px(x + 1, y + 1, dark)
    return d


def border(color, sides='tblr', width=1, glow=False):
    def d(f):
        for k in range(width):
            for x in range(f.w):
                if 't' in sides:
                    f.px(x, k, color, glow)
                if 'b' in sides:
                    f.px(x, f.h - 1 - k, color, glow)
            for y in range(f.h):
                if 'l' in sides:
                    f.px(k, y, color, glow)
                if 'r' in sides:
                    f.px(f.w - 1 - k, y, color, glow)
    return d


def scratches(n, color, length=(2, 4)):
    def d(f):
        r = f.rng
        for _ in range(n if f.w * f.h > 20 else 0):
            x, y = r.randrange(f.w), r.randrange(f.h)
            dx = r.choice((-1, 1))
            for k in range(r.randint(*length)):
                f.px(x + k * dx, y + k, color)
    return d


def blotches(n, colors, rad=(1, 2), sides_only=False):
    """Taches (rouille, mousse, salete) : petits disques pixelises."""
    def d(f):
        if sides_only and not f.side:
            return
        r = f.rng
        for _ in range(max(0, int(n * f.w * f.h / 100.0 + 0.5))):
            cx, cy, rr = r.randrange(f.w), r.randrange(f.h), r.uniform(*rad)
            col = r.choice(colors)
            for y in range(int(cy - rr), int(cy + rr) + 1):
                for x in range(int(cx - rr), int(cx + rr) + 1):
                    if (x - cx) ** 2 + (y - cy) ** 2 <= rr * rr + 0.3 and r.random() < 0.85:
                        f.px(x, y, col)
    return d


def cracks(colors, n=2, length=(4, 9), glow=True, core=None, side_only=False):
    """Fissures en marche aleatoire (lave, glace, energie). colors : bord ; core : coeur plus clair."""
    def d(f):
        if side_only and not f.side:
            return
        r = f.rng
        count = max(1, int(n * (f.w * f.h) / 120.0 + 0.5)) if f.w * f.h >= 6 else 0
        for _ in range(count):
            x, y = r.randrange(f.w), r.randrange(f.h)
            dirx, diry = r.choice(((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, 1)))
            for k in range(r.randint(*length)):
                f.px(x, y, r.choice(colors), glow)
                if core and k % 2 == 0 and r.random() < 0.6:
                    f.px(x, y, core, glow)
                if r.random() < 0.35:
                    dirx, diry = r.choice(((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, 1), (1, -1)))
                x, y = x + dirx, y + diry
                if not f.ok(x, y):
                    break
    return d


def tatter(depth, seed=0, holes=0.0):
    """Bord inferieur dechiquete (texture decoupee) : profil continu autour du cube (coordonnees monde)."""
    def prof(wx, wz):
        a = math.sin(wx * 1.7 + seed) + math.sin(wx * 0.63 + seed * 2.1) * 0.8 + math.sin(wz * 1.3 + seed) * 0.6
        return (a + 2.4) / 4.8
    def d(f):
        if f.name == 'down':
            for y in range(f.h):
                for x in range(f.w):
                    f.clear(x, y)
            return
        if not f.side:
            return
        for i in range(f.w):
            wx, _, wz = f.world(i, 0)
            cut = int(round(prof(wx, wz) * depth))
            for k in range(cut):
                f.clear(i, f.h - 1 - k)
            if holes and f.rng.random() < holes and f.h > depth + 3:
                f.clear(i, f.h - 2 - cut - f.rng.randrange(2))
    return d


def vgrad(colors, glow=False, rows=None):
    """Bande verticale de couleurs (de haut en bas) sur toute la largeur."""
    def d(f):
        n = len(colors)
        for y in range(f.h):
            if rows and y not in rows:
                continue
            c = colors[min(n - 1, int(y * n / max(1, f.h)))]
            for x in range(f.w):
                f.px(x, y, c, glow)
    return d


def only(faces, *decos):
    """Applique des decorations a certaines faces seulement."""
    def d(f):
        if f.name in faces:
            for e in decos:
                e(f)
    return d


# ================================================================================================ atlas
def pack(model, min_w=64, max_w=1024):
    """Range les zones box UV (2*(w+d) x (d+h)) : rayonnages, cubes partages ('share') au meme endroit.
    Choisit la plus petite texture (puissances de 2) qui contient tout."""
    regions, owner = [], {}
    for c in model.cubes():
        w, h, d = c.size
        key = c.share if c.share else ('#', id(c))
        if key in owner:
            assert regions[owner[key]][1] == (2 * (w + d), d + h), ('share de taille differente', c.name)
            continue
        owner[key] = len(regions)
        regions.append((key, (max(1, 2 * (w + d)), max(1, d + h))))
    order = sorted(range(len(regions)), key=lambda i: (-regions[i][1][1], -regions[i][1][0]))
    best = None
    for W in (64, 128, 256, 512, 1024):
        if W < min_w or W > max_w:
            continue
        x = y = shelf = 0
        pos = {}
        for i in order:
            rw, rh = regions[i][1]
            if rw > W:
                pos = None
                break
            if x + rw > W:
                x, y, shelf = 0, y + shelf, 0
            pos[i] = (x, y)
            x += rw
            shelf = max(shelf, rh)
        if pos is None:
            continue
        Hn = y + shelf
        H = 16
        while H < Hn:
            H *= 2
        if H > 4 * W:
            continue
        if best is None or W * H < best[0] * best[1] or (W * H == best[0] * best[1] and abs(W - H) < abs(best[0] - best[1])):
            best = (W, H, pos)
    W, H, pos = best
    for c in model.cubes():
        key = c.share if c.share else ('#', id(c))
        c.uv_offset = pos[owner[key]]
    return W, H


def build_texture(model):
    W, H = pack(model)
    tex = np.zeros((H, W, 4), dtype=np.uint8)
    emi = np.zeros((H, W, 4), dtype=np.uint8)
    done = set()
    for c in model.cubes():
        key = c.share if c.share else ('#', id(c))
        if key in done:
            continue
        done.add(key)
        paint_cube(c, tex, emi, (model.key, key if c.share else c.name))
    return Image.fromarray(tex, 'RGBA'), Image.fromarray(emi, 'RGBA')


# ================================================================================================ export .bbmodel
def uid(*key):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'eldoria-entite/' + '/'.join(map(str, key))))


def fnum(v):
    v = round(float(v), 4)
    return '0' if abs(v) < 1e-9 else ('%g' % v)


def bbmodel(model, tex, entity_class, colors=None, visible_box=(1, 2, 0)):
    """Projet Blockbench (format 4.10, Modded Entity, Mojang mappings 1.17+, texture integree)."""
    colors = colors or {}
    buf = io.BytesIO()
    tex.save(buf, 'PNG')
    W, H = tex.size
    elements = []

    def color_of(b):
        while b is not None:
            if b.name in colors:
                return colors[b.name]
            b = b.parent
        return 0

    def outline(b):
        col = color_of(b)
        node = {'name': b.name, 'origin': [round(v, 4) for v in b.pivot], 'rotation': [round(v, 4) for v in b.rot],
                'color': col, 'uuid': uid(model.key, 'bone', b.name), 'export': True, 'mirror_uv': False,
                'isOpen': b.parent is None or b.parent.parent is None, 'locked': False, 'visibility': True,
                'autouv': 0, 'children': []}
        for c in b.cubes:
            uvs = box_uv(c)
            faces = {f: {'uv': uvs[f], 'texture': 0} for f in FACES}
            el = {'name': c.name, 'box_uv': True, 'rescale': False, 'locked': False, 'render_order': 'default',
                  'allow_mirror_modeling': True, 'from': c.frm, 'to': c.to, 'autouv': 0, 'color': col,
                  'inflate': round(c.inflate, 4), 'origin': [round(v, 4) for v in (c.origin or b.pivot)],
                  'uv_offset': list(c.uv_offset), 'faces': faces, 'type': 'cube', 'uuid': uid(model.key, 'cube', b.name, c.name)}
            if c.mirror:
                el['mirror_uv'] = True
            if c.rot:
                el['rotation'] = [round(v, 4) for v in c.rot]
            elements.append(el)
            node['children'].append(el['uuid'])
        for ch in b.children:
            node['children'].append(outline(ch))
        return node

    def kf_value(channel, v):
        x, y, z = v
        if channel == 'rotation':          # convention des fichiers < 5.0 (Blockbench 5 la reconvertit a l'ouverture)
            x, y = -x, -y
        elif channel == 'position':
            x = -x
        return {'x': fnum(x), 'y': fnum(y), 'z': fnum(z)}

    anims = []
    for a in model.animations:
        animators = {}
        for (bname, channel), ks in sorted(a.ch.items(), key=lambda kv: (model.order.index(model.bones[kv[0][0]]), kv[0][1])):
            gid = uid(model.key, 'bone', bname)
            an = animators.setdefault(gid, {'name': bname, 'type': 'bone', 'keyframes': []})
            for t, v, it in ks:
                an['keyframes'].append({'channel': channel, 'data_points': [kf_value(channel, v)],
                                        'uuid': uid(model.key, 'kf', a.name, bname, channel, t), 'time': t,
                                        'color': -1, 'interpolation': it})
        anims.append({'uuid': uid(model.key, 'anim', a.name), 'name': a.name, 'loop': a.loop, 'override': False,
                      'length': a.length, 'snapping': 20, 'selected': False, 'anim_time_update': '',
                      'blend_weight': '', 'start_delay': '', 'loop_delay': '', 'animators': animators})

    return {
        'meta': {'format_version': '4.10', 'model_format': 'modded_entity', 'box_uv': True},
        'name': model.key, 'model_identifier': model.key,
        'modded_entity_version': '1.17', 'modded_entity_entity_class': entity_class, 'modded_entity_flip_y': True,
        'visible_box': list(visible_box), 'variable_placeholders': '', 'variable_placeholder_buttons': [],
        'timeline_setups': [], 'unhandled_root_fields': {},
        'resolution': {'width': W, 'height': H},
        'elements': elements,
        'outliner': [outline(r) for r in model.roots],
        'textures': [{'path': '', 'name': f'{model.key}.png', 'folder': '', 'namespace': '', 'id': '0', 'group': '',
                      'width': W, 'height': H, 'uv_width': W, 'uv_height': H, 'particle': False,
                      'use_as_default': False, 'layers_enabled': False, 'sync_to_project': '', 'render_mode': 'default',
                      'render_sides': 'auto', 'pbr_channel': 'color', 'frame_time': 1, 'frame_order_type': 'loop',
                      'frame_order': '', 'frame_interpolate': False, 'visible': True, 'internal': True, 'saved': False,
                      'uuid': uid(model.key, 'tex'),
                      'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode('ascii')}],
        'animations': anims,
    }


def check(model, tex):
    """Verifications avant ecriture : noms, tailles, UV dans la texture, zones sans chevauchement, os animes existants."""
    W, H = tex.size
    names = set()
    for b in model.order:
        assert NAME_RE.match(b.name), ('nom d\'os invalide', b.name)
        assert b.name not in names
        names.add(b.name)
    used = np.zeros((H, W), dtype=np.int32)
    seen = set()
    for c in model.cubes():
        key = c.share if c.share else ('#', id(c))
        u, v = c.uv_offset
        w, h, d = c.size
        rw, rh = 2 * (w + d), d + h
        assert u + rw <= W and v + rh <= H, ('UV hors texture', c.name)
        if key in seen:
            continue
        seen.add(key)
        used[v:v + rh, u:u + rw] += 1
    assert used.max() <= 1, 'zones UV qui se chevauchent'
    anames = set()
    for a in model.animations:
        assert a.name not in anames, a.name
        anames.add(a.name)
        for (bname, ch), ks in a.ch.items():
            assert bname in model.bones, (a.name, bname)
            assert ch in ('rotation', 'position', 'scale')
            for t, v, it in ks:
                assert it in ('linear', 'catmullrom'), it
                assert -1e-6 <= t <= a.length + 1e-6, (a.name, bname, t)
    return True


def write_json(path, obj, compact=True):
    with open(path, 'w', encoding='utf-8') as f:
        if compact:
            json.dump(obj, f, ensure_ascii=False, separators=(',', ':'))
        else:
            json.dump(obj, f, ensure_ascii=False, indent=1)


# ================================================================================================ rendu (z-buffer numpy)
def euler_zyx(r):
    """Matrice de rotation (degres) ordre ZYX facon three.js : R = Rz . Ry . Rx."""
    x, y, z = (math.radians(v) for v in r)
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def mat4(R=None, t=(0, 0, 0), s=(1, 1, 1)):
    M = np.eye(4)
    M[:3, :3] = (R if R is not None else np.eye(3)) @ np.diag(s)
    M[:3, 3] = t
    return M


def pose(model, anim=None, t=0.0, extra=None):
    """Matrices monde des os a l'instant t (animation additive sur la pose de repos, comme Blockbench / Java)."""
    out = {}
    for b in model.order:
        rot, pos, scl = list(b.rot), [0.0, 0.0, 0.0], [1.0, 1.0, 1.0]
        for a in ([anim] if anim else []) + (extra or []):
            v = a.sample(b.name, 'rotation', t)
            if v:
                rot = [rot[i] + v[i] for i in range(3)]
            v = a.sample(b.name, 'position', t)
            if v:
                pos = [pos[i] + v[i] for i in range(3)]
            v = a.sample(b.name, 'scale', t)
            if v:
                scl = [scl[i] * v[i] for i in range(3)]
        ppiv = b.parent.pivot if b.parent else [0, 0, 0]
        local = mat4(euler_zyx(rot), [b.pivot[i] - ppiv[i] + pos[i] for i in range(3)], scl)
        out[b.name] = (out[b.parent.name] if b.parent else np.eye(4)) @ local
    return out


FACE_CORNERS = {   # (haut-gauche, haut-droite, bas-gauche) de chaque face, convention des modeles Minecraft
    'north': ((1, 1, 0), (0, 1, 0), (1, 0, 0)), 'south': ((0, 1, 1), (1, 1, 1), (0, 0, 1)),
    'east': ((1, 1, 1), (1, 1, 0), (1, 0, 1)), 'west': ((0, 1, 0), (0, 1, 1), (0, 0, 0)),
    'up': ((0, 1, 0), (1, 1, 0), (0, 1, 1)), 'down': ((0, 0, 1), (1, 0, 1), (0, 0, 0)),
}
FACE_NORMAL = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}
L0 = np.array([0.2, 1.0, -0.7]) / np.linalg.norm([0.2, 1.0, -0.7])
L1 = np.array([-0.2, 1.0, 0.7]) / np.linalg.norm([-0.2, 1.0, 0.7])


def camera(yaw, pitch):
    """yaw 0 = vue de face (camera en -Z), 90 = vue du cote droit de la creature ; pitch > 0 = vue plongeante."""
    yw, pt = math.radians(yaw), math.radians(pitch)
    cam = np.array([math.sin(yw) * math.cos(pt), math.sin(pt), -math.cos(yw) * math.cos(pt)])
    f = -cam
    right = np.cross(f, [0, 1, 0])
    right /= np.linalg.norm(right)
    up = np.cross(right, f)
    return cam, right, up


def render(model, tex, emi=None, anim=None, t=0.0, yaw=30, pitch=12, scale=6.0, bounds=None, glow=True, extra=None,
           hide=()):
    """Rendu orthographique avec z-buffer. bounds = (xmin, xmax, ymin, ymax) en unites ecran pour aligner plusieurs vues."""
    T = np.asarray(tex, dtype=np.float64)
    E = np.asarray(emi, dtype=np.float64) if emi is not None else None
    TW, TH = tex.size
    mats = pose(model, anim, t, extra)
    cam, right, up = camera(yaw, pitch)
    quads = []
    for c in model.cubes():
        if c.bone.name in hide:
            continue
        M = mats[c.bone.name]
        if c.rot:
            org = c.origin or c.bone.pivot
            C = mat4(euler_zyx(c.rot), [org[i] - c.bone.pivot[i] for i in range(3)])
            off = np.array(org)
        else:
            C, off = np.eye(4), np.array(c.bone.pivot)
        lo = np.array(c.frm) - c.inflate - off
        hi = np.array(c.to) + c.inflate - off
        uvs = box_uv(c)
        for fname, (a, b, d) in FACE_CORNERS.items():
            u1, v1, u2, v2 = uvs[fname]
            if u1 == u2 or v1 == v2:
                continue
            pts = []
            for corner in (a, b, d):
                p = np.array([hi[k] if corner[k] else lo[k] for k in range(3)] + [1.0])
                pts.append((M @ C @ p)[:3])
            p0, p1, p3 = pts
            n = np.cross(p1 - p0, p3 - p0)
            nl = np.linalg.norm(n)
            if nl < 1e-9:
                continue
            n = -n / nl
            quads.append((p0, p1, p3, n, (u1, v1, u2, v2)))
    def scr(p):
        return np.array([p @ right, -(p @ up), p @ cam])
    if bounds is None:
        allp = [scr(q[i]) for q in quads for i in range(3)] + [scr(q[1] + q[2] - q[0]) for q in quads]
        xs, ys = [p[0] for p in allp], [p[1] for p in allp]
        bounds = (min(xs) - 2, max(xs) + 2, min(ys) - 2, max(ys) + 2)
    x0b, x1b, y0b, y1b = bounds
    Wp, Hp = int((x1b - x0b) * scale) + 1, int((y1b - y0b) * scale) + 1
    color = np.zeros((Hp, Wp, 4))
    glowb = np.zeros((Hp, Wp, 3))
    zbuf = np.full((Hp, Wp), -1e9)
    translucent = []
    for p0, p1, p3, n, (u1, v1, u2, v2) in quads:
        visible = n @ cam > 1e-6
        both = False
        if not visible:
            continue
        s0, s1, s3 = scr(p0), scr(p1), scr(p3)
        a = (s1 - s0)[:2] * scale
        b = (s3 - s0)[:2] * scale
        o = np.array([(s0[0] - x0b) * scale, (s0[1] - y0b) * scale])
        det = a[0] * b[1] - a[1] * b[0]
        if abs(det) < 1e-9:
            continue
        cs = [o, o + a, o + b, o + a + b]
        xmn, xmx = int(max(0, math.floor(min(c[0] for c in cs)))), int(min(Wp - 1, math.ceil(max(c[0] for c in cs))))
        ymn, ymx = int(max(0, math.floor(min(c[1] for c in cs)))), int(min(Hp - 1, math.ceil(max(c[1] for c in cs))))
        if xmx < xmn or ymx < ymn:
            continue
        X, Y = np.meshgrid(np.arange(xmn, xmx + 1) + 0.5, np.arange(ymn, ymx + 1) + 0.5)
        dx, dy = X - o[0], Y - o[1]
        al = (dx * b[1] - dy * b[0]) / det
        be = (a[0] * dy - a[1] * dx) / det
        inside = (al >= 0) & (al < 1) & (be >= 0) & (be < 1)
        if not inside.any():
            continue
        depth = s0[2] + al * (s1[2] - s0[2]) + be * (s3[2] - s0[2])
        uu = u1 + al * (u2 - u1)
        vv = v1 + be * (v2 - v1)
        tx = np.clip(np.floor(np.minimum(uu, max(u1, u2) - 1e-4)), 0, TW - 1).astype(int)
        ty = np.clip(np.floor(np.minimum(vv, max(v1, v2) - 1e-4)), 0, TH - 1).astype(int)
        texel = T[ty, tx]
        light = min(1.0, 0.4 + 0.6 * (max(0.0, n @ L0) + max(0.0, n @ L1)))
        semi = inside & (texel[..., 3] > 8) & (texel[..., 3] < 250)
        if semi.any():          # pixels translucides : melanges apres la passe opaque
            ys_, xs_ = np.nonzero(semi)
            translucent.append((float(np.mean(depth[semi])), ys_ + ymn, xs_ + xmn, depth[semi], texel[semi][:, :3] * light, texel[semi][:, 3] / 255.0))
        ok = inside & (texel[..., 3] >= 250)
        zb = zbuf[ymn:ymx + 1, xmn:xmx + 1]
        ok &= depth > zb
        if not ok.any():
            continue
        rgb = texel[..., :3] * light
        if E is not None:
            em = E[ty, tx]
            emask = em[..., 3] > 0
            rgb = np.where(emask[..., None], texel[..., :3], rgb)
            gsub = glowb[ymn:ymx + 1, xmn:xmx + 1]
            gsub[ok] = np.where(emask[ok][:, None], em[ok][:, :3], 0)
        csub = color[ymn:ymx + 1, xmn:xmx + 1]
        csub[ok, :3] = rgb[ok]
        csub[ok, 3] = 255
        zb[ok] = depth[ok]
    for _, ys_, xs_, dep, rgb_, al in sorted(translucent, key=lambda f: f[0]):
        front = dep > zbuf[ys_, xs_]
        if not front.any():
            continue
        ys_, xs_, rgb_, al = ys_[front], xs_[front], rgb_[front], al[front]
        base = color[ys_, xs_]
        a0 = base[:, 3] / 255.0
        out_a = al + a0 * (1 - al)
        mix = (rgb_ * al[:, None] + base[:, :3] * (a0 * (1 - al))[:, None]) / np.maximum(out_a, 1e-6)[:, None]
        color[ys_, xs_, :3] = mix
        color[ys_, xs_, 3] = out_a * 255
    img = Image.fromarray(np.clip(color, 0, 255).astype(np.uint8), 'RGBA')
    if glow and E is not None and glowb.any():
        g = Image.fromarray(np.clip(glowb, 0, 255).astype(np.uint8), 'RGB').filter(ImageFilter.GaussianBlur(scale * 1.2))
        ga = np.asarray(g, dtype=np.float64)
        alpha = np.clip(ga.max(axis=2) * 1.6, 0, 200)
        halo = Image.fromarray(np.dstack([np.clip(ga * 1.4, 0, 255), alpha]).astype(np.uint8), 'RGBA')
        out = Image.new('RGBA', img.size, (0, 0, 0, 0))
        out.alpha_composite(halo)
        out.alpha_composite(img)
        img = out
    return img


def screen_bounds(model, anims, yaw, pitch, samples=6, margin=3):
    """Cadre commun a toutes les poses (pour aligner les images d'une planche ou d'un GIF)."""
    cam, right, up = camera(yaw, pitch)
    xs, ys = [], []
    sets = [(None, 0.0)] + [(a, a.length * k / (samples - 1)) for a in anims for k in range(samples)]
    for a, t in sets:
        mats = pose(model, a, t)
        for c in model.cubes():
            M = mats[c.bone.name]
            for cx in (c.frm[0], c.to[0]):
                for cy in (c.frm[1], c.to[1]):
                    for cz in (c.frm[2], c.to[2]):
                        p = np.array([cx - c.bone.pivot[0], cy - c.bone.pivot[1], cz - c.bone.pivot[2], 1.0])
                        q = (M @ p)[:3]
                        xs.append(q @ right)
                        ys.append(-(q @ up))
    return (min(xs) - margin, max(xs) + margin, min(ys) - margin, max(ys) + margin)


# ================================================================================================ planches
def font(size, bold=True):
    for name in (('segoeuib.ttf', 'arialbd.ttf', 'DejaVuSans-Bold.ttf') if bold else ('segoeui.ttf', 'arial.ttf', 'DejaVuSans.ttf')):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def background(W, H, top=(40, 38, 48), bottom=(20, 19, 24)):
    bg = Image.new('RGBA', (W, H), (0, 0, 0, 255))
    d = ImageDraw.Draw(bg)
    for y in range(H):
        d.line([(0, y), (W, y)], fill=lerp(top, bottom, y / max(1, H)) + (255,))
    return bg


def ground_line(d, x0, x1, y, col=(70, 68, 80)):
    d.line([(x0, y), (x1, y)], fill=col + (255,), width=2)


def views_sheet(model, tex, emi, path, title, sub='', scale=6.0,
                specs=(('Face', 0, 8), ('3/4 avant', 35, 14), ('Profil droit', 90, 4), ('3/4 arriere', 150, 14), ('Dos', 180, 8))):
    imgs = []
    for lab, yw, pt in specs:
        imgs.append((lab, render(model, tex, emi, yaw=yw, pitch=pt, scale=scale)))
    Hm = max(i.height for _, i in imgs)
    W = sum(i.width for _, i in imgs) + 36 * (len(imgs) + 1)
    tw = tex.width * max(1, int(Hm * 0.9 / tex.height)) if tex.height * 2 <= Hm else tex.width
    th = tex.height * (tw // tex.width)
    W += tw + 36
    H = Hm + 150
    bg = background(W, H)
    d = ImageDraw.Draw(bg)
    d.text((28, 18), title, font=font(30), fill=(232, 228, 244))
    if sub:
        d.text((28 + d.textlength(title, font=font(30)) + 18, 30), sub, font=font(18, False), fill=(170, 166, 190))
    x = 36
    for lab, im in imgs:
        bg.alpha_composite(im, (x, 76 + Hm - im.height))
        d.text((x + (im.width - d.textlength(lab, font=font(18))) / 2, 90 + Hm), lab, font=font(18), fill=(190, 186, 210))
        x += im.width + 36
    big = tex.resize((tw, th), Image.NEAREST)
    checker = Image.new('RGBA', big.size, (52, 50, 60, 255))
    checker.alpha_composite(big)
    bg.alpha_composite(checker, (x, 76))
    d.text((x, 90 + max(Hm, th)), f'texture {tex.width}x{tex.height} (box UV)', font=font(16, False), fill=(170, 166, 190))
    bg.convert('RGB').save(path)


def anim_sheet(model, tex, emi, path, title, yaw=35, pitch=12, scale=4.0, frames=6, anims=None):
    """Une ligne par animation, 'frames' poses reparties sur la duree (cadrage commun a la ligne)."""
    anims = anims or model.animations
    rows = []
    for a in anims:
        bounds = screen_bounds(model, [a], yaw, pitch)
        ts = [a.length * k / (frames - 1) for k in range(frames)]
        rows.append((a, [(tt, render(model, tex, emi, anim=a, t=tt, yaw=yaw, pitch=pitch, scale=scale, bounds=bounds)) for tt in ts]))
    lab_w = 290
    W = lab_w + max(sum(im.width + 10 for _, im in ims) for _, ims in rows) + 20
    H = 64 + sum(ims[0][1].height + 30 for _, ims in rows)
    bg = background(W, H)
    d = ImageDraw.Draw(bg)
    d.text((20, 16), title, font=font(26), fill=(232, 228, 244))
    y = 60
    for a, ims in rows:
        ch = ims[0][1].height
        d.text((20, y + ch / 2 - 22), a.name, font=font(20), fill=(226, 222, 240))
        d.text((20, y + ch / 2 + 4), f'{a.length:g} s · {a.loop}', font=font(15, False), fill=(160, 156, 182))
        x = lab_w
        for tt, im in ims:
            bg.alpha_composite(im, (x, y))
            d.text((x + 4, y + ch + 2), f't={tt:.2f}', font=font(13, False), fill=(140, 136, 160))
            x += im.width + 10
        ground = y + ch
        y += ch + 30
    bg.convert('RGB').save(path)


def anim_gif(model, tex, emi, anim, path, yaw=35, pitch=12, scale=3.0, fps=12, bounds=None):
    bounds = bounds or screen_bounds(model, [anim], yaw, pitch)
    n = max(2, int(round(anim.length * fps)))
    frames = []
    for k in range(n + (0 if anim.loop == 'loop' else 1)):
        t = min(anim.length, k / fps)
        im = render(model, tex, emi, anim=anim, t=t, yaw=yaw, pitch=pitch, scale=scale, bounds=bounds)
        bg = background(*im.size)
        bg.alpha_composite(im)
        frames.append(bg.convert('RGB').convert('P', palette=Image.ADAPTIVE, colors=255))
    durations = [int(1000 / fps)] * len(frames)
    if anim.loop != 'loop':
        durations[-1] = 900
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=durations, loop=0, optimize=True)


EASE = ((0.2, 0.104), (0.5, 0.5), (0.8, 0.896))      # smoothstep echantillonne


def key_poses(anim, frames, channel='rotation', interp=None, bones=None):
    """Animation par poses-cles : frames = [(t, {os: (x, y, z)}[, interpolation]), ...].
    Chaque os cite dans au moins une pose recoit une image-cle a chaque instant (0 s'il est absent de la pose),
    ce qui evite les derives entre poses. Pour l'echelle, la valeur par defaut est (1, 1, 1).
    Interpolations : 'catmullrom' (boucles), 'linear' (coup sec), 'ease' = lineaire + 3 cles intermediaires
    qui reproduisent un depart et une arrivee en douceur. 'ease' donne le MEME mouvement dans Blockbench,
    en Java vanilla et dans GeckoLib (qui n'interpretent pas tous le catmullrom de la meme facon)."""
    if interp is None:      # par defaut : catmullrom pour les boucles, 'ease' pour les animations jouees une fois
        interp = 'catmullrom' if anim.loop == 'loop' else 'ease'
    names = bones or sorted(set().union(*[set(fr[1]) for fr in frames]))
    neutral = (1.0, 1.0, 1.0) if channel == 'scale' else (0.0, 0.0, 0.0)
    for b in names:
        keys = []
        prev = None
        for fr in frames:
            t, p = fr[0], fr[1]
            it = fr[2] if len(fr) > 2 else interp
            v = p.get(b, neutral)
            if it == 'ease' and prev is not None:
                t0, v0 = prev
                for f, w in EASE:
                    keys.append((t0 + (t - t0) * f,) + tuple(v0[k] + (v[k] - v0[k]) * w for k in range(3)) + ('linear',))
            keys.append((t, v[0], v[1], v[2], 'linear' if it == 'ease' else it))
            prev = (t, v)
        anim.key(b, channel, keys)
    return anim


def interp_warnings(model):
    """Signale les sequences catmullrom -> lineaire : Blockbench les lit en catmullrom, Java et GeckoLib en lineaire."""
    out = []
    for a in model.animations:
        for (b, ch), ks in a.ch.items():
            for k0, k1 in zip(ks, ks[1:]):
                if k0[2] == 'catmullrom' and k1[2] == 'linear' and any(abs(k0[1][i] - k1[1][i]) > 1e-6 for i in range(3)):
                    out.append((a.name, b, ch, k0[0], k1[0]))
    return out


def cycle(anim, bone, channel, times, values, interp='catmullrom'):
    """Piste simple : values[i] (x, y, z) a times[i]."""
    anim.key(bone, channel, [(t, *v, interp) for t, v in zip(times, values)])
    return anim


def sway(anim, bones, period, amp, axis=0, phase_step=0.25, samples=8, base=None, offset=0.0):
    """Oscillation sinusoidale d'un groupe d'os (tissu, fragments) avec un dephasage par os.
    amp : amplitude en degres (ou tuple par os) ; base : rotation de base ajoutee a chaque cle."""
    for i, b in enumerate(bones):
        a_i = amp[i] if isinstance(amp, (list, tuple)) else amp
        ph = (i * phase_step + offset) * 2 * math.pi
        keys = []
        for k in range(samples + 1):
            t = period * k / samples
            v = [0.0, 0.0, 0.0]
            if base and b in base:
                v = list(base[b])
            v[axis] += a_i * math.sin(2 * math.pi * k / samples + ph)
            keys.append((round(t, 4), v[0], v[1], v[2]))
        anim.key(b, 'rotation', keys)
    return anim


def lowest_points(model, samples=24):
    """Point le plus bas de chaque animation (controle des pieds, armes et tissus qui traversent le sol)."""
    out = []
    for an in model.animations:
        worst = (1e9, '', 0.0)
        for k in range(samples + 1):
            t = an.length * k / samples
            M = pose(model, an, t)
            for c in model.cubes():
                b = c.bone
                C = np.eye(4)
                if c.rot:
                    org = c.origin or b.pivot
                    C = mat4(euler_zyx(c.rot), [org[i] - b.pivot[i] for i in range(3)])
                    off = org
                else:
                    off = b.pivot
                for cx in (c.frm[0], c.to[0]):
                    for cy in (c.frm[1], c.to[1]):
                        for cz in (c.frm[2], c.to[2]):
                            y = (M[b.name] @ C @ np.array([cx - off[0], cy - off[1], cz - off[2], 1.0]))[1]
                            if y < worst[0]:
                                worst = (round(float(y), 2), c.name, round(t, 2))
        out.append((an.name,) + worst)
    return out


# ------------------------------------------------------------------------------------------------ poses de jambes
def legs(r, l, names=('jambe_droite', 'tibia_droit', 'pied_droit', 'jambe_gauche', 'tibia_gauche', 'pied_gauche')):
    """r, l : (cuisse, tibia, pied) en degres autour de X pour la jambe droite et la gauche."""
    return {names[0]: (r[0], 0, 0), names[1]: (r[1], 0, 0), names[2]: (r[2], 0, 0),
            names[3]: (l[0], 0, 0), names[4]: (l[1], 0, 0), names[5]: (l[2], 0, 0)}


def crouch(drop, spread=0.0, thigh=10.0, shin=13.0):
    """Flexion des genoux qui abaisse la hanche de 'drop' px, pied sous la hanche et a plat
    (a combiner avec un deplacement du bassin de -drop). spread : jambes ecartees (degres)."""
    a = b = 0.0
    for i in range(0, 900):
        ar = math.radians(i / 10.0)
        sb = thigh * math.sin(ar) / shin
        if sb >= 1:
            break
        br = math.asin(sb)
        if thigh * (1 - math.cos(ar)) + shin * (1 - math.cos(br)) >= drop:
            a, b = math.degrees(ar), math.degrees(br)
            break
    p = legs((a, -(a + b), b), (a, -(a + b), b))
    if spread:
        p['jambe_droite'] = (a, 0, spread)
        p['jambe_gauche'] = (a, 0, -spread)
        p['pied_droit'] = (b, 0, -spread)
        p['pied_gauche'] = (b, 0, spread)
    return p


def M(*dicts):
    """Somme de poses (os absents = 0)."""
    out = {}
    for d in dicts:
        for k, v in d.items():
            out[k] = tuple(out.get(k, (0, 0, 0))[i] + v[i] for i in range(3))
    return out


def SC(v):
    return (v, v, v)


def subtree_min_y(model, M, bone, exclude=()):
    """Point le plus bas (monde) des cubes d'un os et de ses descendants, hors sous-arbres exclus."""
    ys = []

    def walk(b):
        for c in b.cubes:
            C = np.eye(4)
            off = b.pivot
            if c.rot:
                org = c.origin or b.pivot
                C = mat4(euler_zyx(c.rot), [org[i] - b.pivot[i] for i in range(3)])
                off = org
            for cx in (c.frm[0], c.to[0]):
                for cy in (c.frm[1], c.to[1]):
                    for cz in (c.frm[2], c.to[2]):
                        ys.append((M[b.name] @ C @ np.array([cx - off[0], cy - off[1], cz - off[2], 1.0]))[1])
        for ch in b.children:
            if ch.name not in exclude:
                walk(ch)
    walk(model.bones[bone])
    return min(ys) if ys else None


def settle(model, anim, t, bones, ground=0.3):
    """Pose au sol, a l'instant t, chaque os liste (parents d'abord) : sa cle de position a t est corrigee pour que
    le point le plus bas de son sous-arbre (hors autres os listes) soit a 'ground'. Renvoie {os: position finale}."""
    order = [b.name for b in model.order if b.name in bones]
    out = {}
    for name in order:
        M = pose(model, anim, t)
        y = subtree_min_y(model, M, name, exclude=set(bones) - {name})
        if y is None:
            continue
        b = model.bones[name]
        Rp = M[b.parent.name][:3, :3] if b.parent else np.eye(3)
        d = np.linalg.solve(Rp, np.array([0.0, ground - y, 0.0]))
        ks = anim.ch.setdefault((name, 'position'), [])
        hit = [i for i, kk in enumerate(ks) if abs(kk[0] - t) < 1e-6]
        if hit:
            i = hit[0]
            v = ks[i][1]
            ks[i] = (ks[i][0], (v[0] + d[0], v[1] + d[1], v[2] + d[2]), ks[i][2])
        else:
            ks.append((t, tuple(d), 'linear'))
            ks.sort(key=lambda kk: kk[0])
        out[name] = [kk for kk in ks if abs(kk[0] - t) < 1e-6][0][1]
    return out


# ================================================================================================ export Java (Minecraft 26.3)
def _jf(v):
    v = round(float(v), 4)
    if abs(v) < 1e-9:
        v = 0.0
    s = ('%.4f' % v).rstrip('0').rstrip('.')
    return (s if s not in ('', '-0') else '0') + 'F'


def java_layer_code(model, tex):
    """Corps de createBodyLayer() : meme conversion que l'export Java de Blockbench (X et Y inverses, +24 sur les
    racines, rotations (-x, -y, z), cubes tournes places dans un sous-element '<cube>_r')."""
    W, H = tex.size
    out = ['MeshDefinition mesh = new MeshDefinition();', 'PartDefinition root = mesh.getRoot();']
    var = {}
    used = set()

    def ident(name):
        base = 'p_' + re.sub(r'\W', '_', name)
        v, k = base, 2
        while v in used:
            v, k = f'{base}_{k}', k + 1
        used.add(v)
        return v

    def pose(origin, parent_origin, rot, is_root):
        c = [origin[i] - (parent_origin[i] if parent_origin is not None else 0) for i in range(3)]
        c[0] *= -1
        c[1] *= -1
        if is_root:
            c[1] += 24
        if rot and any(abs(r) > 1e-9 for r in rot):
            rr = [math.radians(-rot[0]), math.radians(-rot[1]), math.radians(rot[2])]
            return 'PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)' % tuple(_jf(v) for v in c + rr)
        return 'PartPose.offset(%s, %s, %s)' % tuple(_jf(v) for v in c)

    def box(c, origin):
        x = origin[0] - c.to[0]
        y = -c.frm[1] - c.size[1] + origin[1]
        z = c.frm[2] - origin[2]
        s = '.texOffs(%d, %d)' % tuple(c.uv_offset)
        if c.mirror:
            s += '.mirror()'
        s += '.addBox(%s, %s, %s, %s, %s, %s, new CubeDeformation(%s))' % (
            _jf(x), _jf(y), _jf(z), _jf(c.size[0]), _jf(c.size[1]), _jf(c.size[2]), _jf(c.inflate))
        if c.mirror:
            s += '.mirror(false)'
        return s

    for b in model.order:
        v = ident(b.name)
        var[b.name] = v
        parent = 'root' if b.parent is None else var[b.parent.name]
        plain = [c for c in b.cubes if not c.rot]
        cubes = ''.join(box(c, b.pivot) for c in plain)
        out.append('PartDefinition %s = %s.addOrReplaceChild("%s", CubeListBuilder.create()%s, %s);' % (
            v, parent, b.name, cubes, pose(b.pivot, b.parent.pivot if b.parent else None, b.rot, b.parent is None)))
        for c in b.cubes:
            if not c.rot:
                continue
            org = c.origin or b.pivot
            sub = ident(b.name + '_' + c.name + '_r')
            out.append('%s.addOrReplaceChild("%s", CubeListBuilder.create()%s, %s);' % (
                v, sub[2:], box(c, org), pose(org, b.pivot, c.rot, False)))
    out.append('return LayerDefinition.create(mesh, %d, %d);' % (W, H))
    return out


def java_anim_data(anim):
    """Images-cles d'une animation sous forme de texte compact : 'os|canal|t,x,y,z,i;...' (une ligne par piste),
    valeurs deja dans la convention de l'export Java de Blockbench (a passer a degreeVec / posVec / scaleVec)."""
    lines = []
    for (bone, ch), ks in a_sorted(anim.ch):
        cc = {'rotation': 'r', 'position': 'p', 'scale': 's'}[ch]
        items = []
        for t, (x, y, z), it in ks:
            if ch == 'rotation':
                x, y = -x, -y
            elif ch == 'position':
                x = -x
            items.append(','.join([fnum(t), fnum(x), fnum(y), fnum(z), 'c' if it == 'catmullrom' else 'l']))
        lines.append(f'{bone}|{cc}|' + ';'.join(items))
    return '\n'.join(lines)


def a_sorted(ch):
    return sorted(ch.items(), key=lambda kv: (kv[0][0], kv[0][1]))


def java_string_chunks(s, size=20000):
    chunks = [s[i:i + size] for i in range(0, len(s), size)] or ['']
    return [json.dumps(c, ensure_ascii=True) for c in chunks]


def java_export(model, tex, cls, key, strikes=None, header='', client_pkg='com.mmorpg.client.model.boss',
                common_pkg='com.mmorpg.entity.boss.anim', art_dir='art/boss_blockbench'):
    """Renvoie (code du modele client, code des constantes communes) pour Minecraft 26.3 / NeoForge."""
    strikes = strikes or {}
    anims = model.animations
    const = re.sub(r'\W', '_', key).upper()
    hdr = f'// Genere automatiquement par {header} - ne pas modifier a la main (relancer le script).\n'
    # --- client : geometrie + animations
    body = '\n        '.join(java_layer_code(model, tex))
    adefs = []
    for a in anims:
        chunks = java_string_chunks(java_anim_data(a))
        adefs.append('        BlockbenchAnimations.parse(new String[]{\n                %s\n        }, %sF, %s)' % (
            ',\n                '.join(chunks), fnum(a.length), 'true' if a.loop == 'loop' else 'false'))
    client = f"""{hdr}package {client_pkg};

import com.mmorpg.MMORPG;
import com.mmorpg.client.model.BlockbenchAnimations;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Modele et animations de {model.title} (format Modded Entity de Blockbench, {art_dir}/{key}). */
public final class {cls}Model {{
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("{key}"), "main");

    private {cls}Model() {{
    }}

    public static LayerDefinition createBodyLayer() {{
        {body}
    }}

    /** Dans l'ordre des indices de {cls}Anims. */
    public static AnimationDefinition[] animations() {{
        return new AnimationDefinition[]{{
{(','+chr(10)).join(adefs)}
        }};
    }}
}}
"""
    # --- commun : indices, durees, instants de frappe (utilisables cote serveur)
    names = [a.name.upper() for a in anims]
    consts = '\n'.join(f'    public static final int {n} = {i};' for i, n in enumerate(names))
    lengths = ', '.join(str(int(math.ceil(a.length * 20))) for a in anims)
    strike = ', '.join(str(int(round(strikes.get(a.name, 0.0) * 20))) for a in anims)
    loops = ', '.join('true' if a.loop == 'loop' else 'false' for a in anims)
    holds = ', '.join('true' if a.loop == 'hold' else 'false' for a in anims)
    common = f"""{hdr}package {common_pkg};

/** Animations de {model.title} : indices, durees et instants de frappe (en ticks). */
public final class {cls}Anims {{
{consts}
    public static final int COUNT = {len(anims)};
    public static final int[] LENGTH_TICKS = {{{lengths}}};
    public static final int[] STRIKE_TICKS = {{{strike}}};
    public static final boolean[] LOOPING = {{{loops}}};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {{{holds}}};

    private {cls}Anims() {{
    }}
}}
"""
    return client, common
