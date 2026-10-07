"""Montures 3D (12 modeles voxel facon Blockbench) : geometrie, texture peinte 512x512 et animations.

Sortie : assets/mmorpg/bbmodels/mounts/<id>.json (format lu par ImportedModel) et textures/imported/mounts/<id>.png.
Repere Blockbench : x largeur, y hauteur (0 = sol), z longueur, tete vers -z (nord). 16 unites = 1 bloc.
"""
import json
import math
import random
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/mmorpg'
ATLAS = 512
DENSITY = 3  # pixels par unite de modele


def rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=float)


# ---------------------------------------------------------------- peinture des materiaux

class Painter:
    """Peint une face (tableau h x w x 4) selon un materiau ; ligne 0 = haut de la face."""

    def __init__(self, seed):
        self.rng = np.random.default_rng(seed)

    def noise(self, h, w, amp):
        return self.rng.uniform(-amp, amp, (h, w, 1))

    def base(self, h, w, color, amp=10):
        img = np.zeros((h, w, 3)) + rgb(color)
        return img + self.noise(h, w, amp)

    def paint(self, mat, h, w, face):
        kind, c = mat[0], mat[1:]
        f = getattr(self, 'm_' + kind)
        img = f(h, w, face, *c)
        # ombrage par face + degrade vertical
        shade = {'up': 1.12, 'down': .55, 'north': .96, 'south': .92, 'east': .86, 'west': .9}[face]
        if face not in ('up', 'down') and kind not in ('eye', 'glow'):
            grad = np.linspace(1.06, .82, h).reshape(h, 1, 1)
            img = img * grad
        if kind not in ('eye', 'glow'):
            img = img * shade
        img = np.clip(img, 0, 255)
        out = np.concatenate([img, np.full((h, w, 1), 255.0)], axis=2)
        # contour pixel-art (bord legerement plus sombre)
        if h > 3 and w > 3 and kind not in ('eye', 'glow'):
            out[0, :, :3] *= .8; out[-1, :, :3] *= .7; out[:, 0, :3] *= .78; out[:, -1, :3] *= .78
        return out.astype(np.uint8)

    # -- materiaux
    def m_fur(self, h, w, face, color, dark=None):
        img = self.base(h, w, color, 8)
        d = rgb(dark) if dark else rgb(color) * .6
        for _ in range(max(1, w * h // 6)):
            x, y = self.rng.integers(0, w), self.rng.integers(0, h)
            ln = self.rng.integers(2, 5)
            img[y:y + ln, x] = img[y:y + ln, x] * .5 + d * .5
        return img

    def m_hide(self, h, w, face, color):
        img = self.base(h, w, color, 6)
        for _ in range(w * h // 20):
            x, y = self.rng.integers(0, w), self.rng.integers(0, h)
            img[y, x] *= .85
        return img

    def m_dark(self, h, w, face, color='#2a2420'):
        return self.base(h, w, color, 5)

    def m_bone(self, h, w, face, color='#e8dfc4'):
        img = self.base(h, w, color, 5)
        img[-max(1, h // 4):] *= .85
        return img

    def m_eye(self, h, w, face, color):
        img = np.zeros((h, w, 3)) + rgb(color)
        if face in ('north', 'east', 'west'):
            img[0:max(1, h // 2), 0:max(1, w // 2)] = 255
        return img

    def m_glow(self, h, w, face, color):
        img = np.zeros((h, w, 3)) + rgb(color)
        img += self.noise(h, w, 18)
        return np.clip(img * 1.1, 0, 255)

    def m_crystal(self, h, w, face, color):
        c = rgb(color)
        img = np.zeros((h, w, 3)) + c
        yy, xx = np.mgrid[0:h, 0:w]
        t = (xx / max(1, w) * .5 + yy / max(1, h) * .5).reshape(h, w, 1)
        img = c * (1.35 - .6 * t)
        img[(xx + yy) % 7 == 0] = np.minimum(255, c * 1.6)[None]
        img[0, :] = 255
        img[:, 0] = np.minimum(255, c * 1.7)
        return img

    def m_lava(self, h, w, face, color='#2a1c1c', glow='#ff7a1a'):
        img = self.base(h, w, color, 10)
        g = rgb(glow)
        for _ in range(max(1, w * h // 40)):
            x, y = self.rng.integers(0, w), self.rng.integers(0, h)
            for _ in range(self.rng.integers(3, 9)):
                if 0 <= x < w and 0 <= y < h:
                    img[y, x] = g + self.rng.uniform(0, 60)
                    if x + 1 < w: img[y, x + 1] = g * .75
                x += self.rng.integers(-1, 2); y += self.rng.integers(-1, 2)
        return np.clip(img, 0, 255)

    def m_bark(self, h, w, face, color='#6b4a2c', moss='#4f9a2f'):
        img = self.base(h, w, color, 8)
        for x in range(0, w, 3):
            img[:, x] *= .78
        m = rgb(moss)
        for _ in range(max(1, w * h // 30)):
            x, y = self.rng.integers(0, w), self.rng.integers(0, h)
            r = self.rng.integers(1, 4)
            img[max(0, y - r):y + r, max(0, x - r):x + r] = m + self.noise(1, 1, 25)[0, 0]
        return img

    def m_leaf(self, h, w, face, color='#5aa83a'):
        img = self.base(h, w, color, 22)
        img[self.rng.random((h, w)) < .2] *= .7
        return img

    def m_feather(self, h, w, face, color, tip=None):
        img = self.base(h, w, color, 8)
        t = rgb(tip) if tip else rgb(color) * 1.2
        for y in range(0, h, 4):
            img[y] *= .7
            for x in range(0, w, 4):
                img[y + 1:y + 3, x:x + 2] = np.minimum(255, t)
        return img

    def m_scale(self, h, w, face, color, dark=None):
        img = self.base(h, w, color, 8)
        d = rgb(dark) if dark else rgb(color) * .6
        for y in range(0, h, 3):
            off = (y // 3) % 2 * 2
            for x in range(off, w, 4):
                img[y, x:x + 3] = d
        return img

    def m_shell(self, h, w, face, color, line='#1f4a3c'):
        img = self.base(h, w, color, 10)
        ln = rgb(line)
        step = 10
        for y in range(0, h, step):
            img[y] = ln
            off = (y // step) % 2 * (step // 2)
            for x in range(off, w, step):
                img[y:y + step, x] = ln
        yy, xx = np.mgrid[0:h, 0:w]
        img[(xx % step == 2) & (yy % step == 2)] = np.minimum(255, rgb(color) * 1.6)
        return img

    def m_chitin(self, h, w, face, color, dark=None):
        img = self.base(h, w, color, 7)
        d = rgb(dark) if dark else rgb(color) * .55
        for y in range(0, h, 6):
            img[y] = d
        img[1:2, :] = np.minimum(255, img[1:2, :] * 1.3)
        return img

    def m_leather(self, h, w, face, color='#6b4024'):
        img = self.base(h, w, color, 6)
        if h > 4 and w > 4:
            img[1, 1:-1:2] = rgb('#d8b080'); img[-2, 1:-1:2] = rgb('#d8b080')
        return img

    def m_cloth(self, h, w, face, color, trim='#d4a830', emblem=None):
        img = self.base(h, w, color, 5)
        t = rgb(trim)
        if h > 4 and w > 4:
            img[:, :1] = t; img[:, -1:] = t; img[-2:, :] = t
            # pointes du fanion
            for x in range(w):
                if (x // 2) % 2 == 0: img[-1, x] = rgb(color) * .4
            if emblem and h >= 9 and w >= 7:
                cy, cx = h // 2 - 1, w // 2
                pat = EMBLEMS[emblem]
                ph, pw = len(pat), len(pat[0])
                for j in range(ph):
                    for i in range(pw):
                        if pat[j][i] == '#':
                            y, x = cy - ph // 2 + j, cx - pw // 2 + i
                            if 0 <= y < h and 0 <= x < w: img[y, x] = t
        return img

    def m_metal(self, h, w, face, color='#9aa0a8'):
        img = self.base(h, w, color, 6)
        img = img * np.linspace(1.15, .8, h).reshape(h, 1, 1)
        if h >= 4 and w >= 4:
            for y, x in ((1, 1), (1, w - 2), (h - 2, 1), (h - 2, w - 2)):
                img[y, x] = 240
        return img

    def m_gold(self, h, w, face, color='#d9a830'):
        return self.m_metal(h, w, face, color)

    def m_rune(self, h, w, face, color, rune='#40e8e0'):
        img = self.m_fur(h, w, face, color)
        r = rgb(rune)
        if h > 8 and w > 8:
            for _ in range(max(1, w * h // 160)):
                x, y = self.rng.integers(1, w - 5), self.rng.integers(1, h - 5)
                for dx, dy in RUNE_SHAPES[self.rng.integers(0, len(RUNE_SHAPES))]:
                    img[y + dy, x + dx] = r
        return img

    def m_void(self, h, w, face, color='#2a1840', glow='#b060ff'):
        img = self.base(h, w, color, 8)
        g = rgb(glow)
        img[self.rng.random((h, w)) < .04] = g
        for _ in range(max(1, w * h // 120)):
            x, y = self.rng.integers(0, w), self.rng.integers(0, h)
            img[y:y + 3, x] = g * .8
        return img

    def m_wool(self, h, w, face, color='#eef0f4', mark='#5a9ae8'):
        img = self.base(h, w, color, 9)
        yy, xx = np.mgrid[0:h, 0:w]
        img[((xx + yy * 2) % 5 == 0)] *= .9
        m = rgb(mark)
        for _ in range(max(1, w * h // 90)):
            x, y = self.rng.integers(0, w), self.rng.integers(0, h)
            img[y:y + 2, x:x + 3] = m
        return img


EMBLEMS = {
    'flame': ['..#..', '.##..', '.###.', '#####', '.###.'],
    'snow': ['#.#.#', '.###.', '##.##', '.###.', '#.#.#'],
    'leaf': ['..#..', '.###.', '#####', '.###.', '..#..'],
    'sun': ['#.#.#', '.###.', '##.##', '.###.', '#.#.#'],
    'eye': ['.###.', '#...#', '#.#.#', '#...#', '.###.'],
    'chevron': ['#...#', '##.##', '.###.', '..#..', '.....'],
}
RUNE_SHAPES = [
    [(0, 0), (0, 1), (0, 2), (1, 1), (2, 0), (2, 1), (2, 2)],
    [(1, 0), (0, 1), (1, 1), (2, 1), (1, 2), (1, 3)],
    [(0, 0), (1, 1), (2, 2), (0, 2), (2, 0)],
    [(0, 0), (1, 0), (2, 0), (2, 1), (2, 2), (1, 2)],
]


# ---------------------------------------------------------------- modele

class Model:
    def __init__(self, key, seed):
        self.key = key
        self.groups = {}
        self.top = []
        self.boxes = []
        self.n = 0
        self.seed = seed

    def group(self, name, pivot, parent=None, rot=(0, 0, 0)):
        g = {'uuid': name, 'origin': list(map(float, pivot)), 'rotation': list(map(float, rot)), 'children': []}
        self.groups[name] = g
        (self.groups[parent]['children'] if parent else self.top).append(g)
        return name

    def box(self, grp, frm, to, mat, rot=None, pivot=None, mirror=False):
        """Cube dans le groupe ; si mirror, ajoute aussi son symetrique (x -> -x)."""
        self._box(grp, frm, to, mat, rot, pivot)
        if mirror:
            f2 = [-to[0], frm[1], frm[2]]; t2 = [-frm[0], to[1], to[2]]
            r2 = [rot[0], -rot[1], -rot[2]] if rot else None
            p2 = [-pivot[0], pivot[1], pivot[2]] if pivot else None
            self._box(grp, f2, t2, mat, r2, p2)

    def _box(self, grp, frm, to, mat, rot, pivot):
        self.n += 1
        g = self.groups[grp]
        b = {'uuid': f'{self.key}_{self.n}', 'from': [float(v) for v in frm], 'to': [float(v) for v in to],
             'origin': [float(v) for v in (pivot or g['origin'])], 'rotation': [float(v) for v in (rot or (0, 0, 0))],
             'mat': mat}
        g['children'].append(b)
        self.boxes.append(b)

    def build(self, anims):
        painter = Painter(self.seed)
        atlas = np.zeros((ATLAS, ATLAS, 4), dtype=np.uint8)
        rects = []
        for b in self.boxes:
            dx, dy, dz = (abs(b['to'][i] - b['from'][i]) for i in range(3))
            dims = {'north': (dx, dy), 'south': (dx, dy), 'east': (dz, dy), 'west': (dz, dy), 'up': (dx, dz), 'down': (dx, dz)}
            for face, (fw, fh) in dims.items():
                rects.append((b, face, max(1, round(fw * DENSITY)), max(1, round(fh * DENSITY))))
        rects.sort(key=lambda r: -r[3])
        x = y = row = 0
        for b, face, w, h in rects:
            if x + w > ATLAS:
                x, y, row = 0, y + row, 0
            if y + h > ATLAS:
                raise ValueError(f'{self.key} : atlas plein')
            atlas[y:y + h, x:x + w] = painter.paint(b['mat'], h, w, face)
            b.setdefault('faces', {})[face] = {'uv': [x, y, x + w, y + h], 'texture': 0}
            x += w
            row = max(row, h)
        for b in self.boxes:
            del b['mat']
        model = {'resolution': {'width': ATLAS, 'height': ATLAS}, 'parts': self.top, 'animations': anims}
        return model, Image.fromarray(atlas, 'RGBA')


# ---------------------------------------------------------------- animations

def track(bone, channel, keys):
    return bone + '|' + channel + '|' + ';'.join(f'{t:g},{x:g},{y:g},{z:g},c' for t, x, y, z in keys)


def wave(bone, channel, axis, amp, length, phase=0.0, steps=8, base=(0, 0, 0)):
    keys = []
    for i in range(steps + 1):
        t = length * i / steps
        v = amp * math.sin(2 * math.pi * (i / steps + phase))
        k = list(base)
        k['xyz'.index(axis)] += v
        keys.append((t, *k))
    return track(bone, channel, keys)


def anim(name, length, tracks):
    return {'name': name, 'length': length, 'loop': True, 'tracks': '\n'.join(tracks)}


def quad_anims(legs=('leg_fl', 'leg_fr', 'leg_bl', 'leg_br'), swing=28, speed=0.9, tail=True, head=True, extra=()):
    fl, fr, bl, br = legs
    walk = [wave(fl, 'r', 'x', swing, speed, 0), wave(br, 'r', 'x', swing, speed, 0),
            wave(fr, 'r', 'x', swing, speed, .5), wave(bl, 'r', 'x', swing, speed, .5),
            wave('body', 'p', 'y', .6, speed / 2, 0)]
    idle = [wave('body', 'p', 'y', .25, 3.0)]
    if head:
        walk.append(wave('head', 'r', 'x', 4, speed, .25))
        idle.append(wave('head', 'r', 'x', 3, 3.0, .25))
    if tail:
        walk.append(wave('tail', 'r', 'y', 12, speed))
        idle.append(wave('tail', 'r', 'y', 8, 2.4))
    walk += [e[0] for e in extra if e[1] == 'walk']
    idle += [e[0] for e in extra if e[1] == 'idle']
    return [anim('idle', 3.0, idle), anim('walk', speed, walk)]


# ---------------------------------------------------------------- briques communes

def quadruped(m, s):
    """Corps a 4 pattes. s : dict (bw, bh, bl, leg, lt, body_mat, leg_mat, paw_mat, neck..., head...)."""
    bw, bh, bl, leg, lt = s['bw'], s['bh'], s['bl'], s['leg'], s['lt']
    m.group('body', (0, leg + bh / 2, 0))
    m.box('body', (-bw / 2, leg, -bl / 2), (bw / 2, leg + bh, bl / 2), s['body_mat'])
    # pattes
    lx = bw / 2 - lt / 2 - s.get('leg_in', .5)
    for name, sx, sz in (('leg_fl', 1, -1), ('leg_fr', -1, -1), ('leg_bl', 1, 1), ('leg_br', -1, 1)):
        z = sz * (bl / 2 - lt / 2 - s.get('leg_edge', 1.5))
        m.group(name, (sx * lx, leg + 1, z))
        up = s.get('upper', 1.25)
        m.box(name, (sx * lx - lt / 2 * up, leg * .45, z - lt / 2 * up), (sx * lx + lt / 2 * up, leg + 1.5, z + lt / 2 * up), s['leg_mat'])
        m.box(name, (sx * lx - lt / 2, 1.2, z - lt / 2), (sx * lx + lt / 2, leg * .45 + .1, z + lt / 2), s['leg_mat'])
        m.box(name, (sx * lx - lt / 2 - .3, 0, z - lt / 2 - .9), (sx * lx + lt / 2 + .3, 1.3, z + lt / 2 + .3), s['paw_mat'])
    # tete au bout du cou (angle positif = cou releve)
    hz = -bl / 2
    hy = leg + bh * s.get('neck_y', .75)
    m.group('head', (0, hy, hz + 1), 'body')
    nk = s.get('neck', 4)
    nw = s.get('neck_w', bw * .55)
    a = s.get('neck_up', 20)
    m.box('head', (-nw / 2, hy - nw * .5, hz - nk), (nw / 2, hy + nw * .5, hz + 2), s['body_mat'], rot=(a, 0, 0), pivot=(0, hy, hz))
    hw, hh, hl = s['hw'], s['hh'], s['hl']
    hy2 = hy + nk * math.sin(math.radians(a)) + s.get('head_up', 0)
    hz2 = hz - nk * math.cos(math.radians(a)) + nw * .4
    m.box('head', (-hw / 2, hy2 - hh / 2, hz2 - hl), (hw / 2, hy2 + hh / 2, hz2), s.get('head_mat', s['body_mat']))
    sw, sh, sl = s.get('snout', (hw * .6, hh * .5, 3))
    m.box('head', (-sw / 2, hy2 - hh / 2, hz2 - hl - sl), (sw / 2, hy2 - hh / 2 + sh, hz2 - hl), s.get('snout_mat', s.get('head_mat', s['body_mat'])))
    if s.get('nose'):
        m.box('head', (-sw / 2 + .5, hy2 - hh / 2 + sh - 1.2, hz2 - hl - sl - .4), (sw / 2 - .5, hy2 - hh / 2 + sh - .2, hz2 - hl - sl), s['nose'])
    # yeux
    ey = hy2 + hh * .12
    m.box('head', (hw / 2 - .1, ey, hz2 - hl + 1), (hw / 2 + .3, ey + 1.2, hz2 - hl + 2.4), s['eye'], mirror=True)
    # oreilles
    if s.get('ears'):
        ew, eh, emat = s['ears']
        m.box('head', (hw / 2 - ew - .3, hy2 + hh / 2, hz2 - 2.5), (hw / 2 - .3, hy2 + hh / 2 + eh, hz2 - 1), emat, mirror=True)
    # queue
    if s.get('tail'):
        tw, tl, tmat = s['tail']
        ty = leg + bh - 2
        m.group('tail', (0, ty, bl / 2), 'body')
        m.box('tail', (-tw / 2, ty - tw / 2, bl / 2), (tw / 2, ty + tw / 2, bl / 2 + tl), tmat, rot=(-s.get('tail_up', -20), 0, 0), pivot=(0, ty, bl / 2))
    return {'seat': leg + bh, 'head_y': hy2, 'head_z': hz2, 'hl': hl, 'hw': hw, 'hh': hh}


def saddle(m, bw, top, z, length, cloth, trim, emblem, strap='#5a3420', metal=('metal', '#9aa0a8'), banner_h=None, grp='body'):
    """Selle en cuir + sangles + fanions lateraux avec embleme."""
    hl = length / 2
    m.box(grp, (-bw / 2 + 1, top, z - hl), (bw / 2 - 1, top + 1.2, z + hl), ('leather', strap))
    m.box(grp, (-bw / 2 + 1.5, top + 1.2, z + hl - 1.5), (bw / 2 - 1.5, top + 3.2, z + hl), ('leather', strap))       # dossier
    m.box(grp, (-1, top + 1.2, z - hl), (1, top + 3.5, z - hl + 1.5), ('leather', strap))                             # pommeau
    m.box(grp, (-.6, top + 3.5, z - hl + .2), (.6, top + 4.4, z - hl + 1.3), metal)
    m.box(grp, (-bw / 2 - .25, top - 5, z - 1), (-bw / 2, top + .3, z + 1), ('leather', strap), mirror=True)           # sangles
    m.box(grp, (-bw / 2 - .4, top - 3.5, z - 1.3), (-bw / 2 - .15, top - 2, z + 1.3), metal, mirror=True)              # boucles
    bh = banner_h or 7
    m.box(grp, (-bw / 2 - .5, top - bh, z - hl + 1), (-bw / 2 - .2, top + .2, z + hl - 1), ('cloth', cloth, trim, emblem), mirror=True)


def bridle(m, info, strap='#5a3420', metal=('metal', '#9aa0a8')):
    hy, hz, hl, hw, hh = info['head_y'], info['head_z'], info['hl'], info['hw'], info['hh']
    m.box('head', (-hw / 2 - .2, hy - hh / 2 + .5, hz - hl * .6), (hw / 2 + .2, hy - hh / 2 + 1.3, hz - hl * .6 + .8), ('leather', strap))
    m.box('head', (hw / 2, hy - hh / 2 + .3, hz - hl * .6 - .3), (hw / 2 + .45, hy - hh / 2 + 1.5, hz - hl * .6 + 1.1), metal, mirror=True)


# ---------------------------------------------------------------- les 12 montures

def sanglier():
    m = Model('sanglier', 1)
    fur, dark = ('fur', '#6b4a2e', '#3e2a18'), ('dark', '#2e2018')
    info = quadruped(m, dict(bw=13, bh=11, bl=24, leg=8, lt=4, body_mat=fur, leg_mat=fur, paw_mat=dark,
                             neck=3, neck_w=9, neck_y=.55, neck_up=5, hw=10, hh=9, hl=7, snout=(6, 5, 4), snout_mat=('hide', '#8a6a52'),
                             nose=('dark', '#3a2a24'), eye=('eye', '#f0c020'), ears=(2.5, 3, fur), tail=(1.6, 5, fur), tail_up=-40))
    m.box('body', (-6, 17, -12), (6, 21, -2), fur)          # bosse des epaules
    # criniere herissee
    for i in range(6):
        z = -10 + i * 3.6
        m.box('body', (-1.5, 19 - i * .3, z), (1.5, 22 - i * .4, z + 3), ('fur', '#4a3220', '#2a1a10'), rot=(-15, 0, 0), pivot=(0, 19, z))
    # defenses
    hy, hz = info['head_y'], info['head_z'] - info['hl']
    m.box('head', (2.6, hy - 3.5, hz - 3.5), (3.8, hy + .5, hz - 2.3), ('bone',), rot=(-25, 0, 0), pivot=(3.2, hy - 3.5, hz - 3), mirror=True)
    # armure metal : plaque frontale, bracelets
    m.box('head', (-3.5, hy + 1.5, hz - .2), (3.5, hy + 4.2, hz + 1), ('metal', '#8a9098'))
    for leg in ('leg_fl', 'leg_fr', 'leg_bl', 'leg_br'):
        g = m.groups[leg]['origin']
        m.box(leg, (g[0] - 2.4, 2.5, g[2] - 2.4), (g[0] + 2.4, 4, g[2] + 2.4), ('metal', '#8a9098'))
    saddle(m, 13, 19, 1, 9, '#a8281e', '#e0b040', 'chevron')
    bridle(m, info)
    return m, quad_anims()


def loup_givre():
    m = Model('loup_givre', 2)
    fur = ('fur', '#e6ecf2', '#a8b8c8')
    info = quadruped(m, dict(bw=10, bh=10, bl=24, leg=11, lt=3.2, body_mat=fur, leg_mat=fur, paw_mat=('hide', '#c0ccd8'),
                             neck=5, neck_w=7, neck_y=.7, neck_up=30, hw=8, hh=7, hl=6, snout=(4.5, 3.5, 5), nose=('dark', '#20242c'),
                             eye=('eye', '#40d8ff'), ears=(2, 3.5, fur), tail=(3, 10, fur), tail_up=-20))
    cr = ('crystal', '#7cc8ff')
    for i, (z, hgt) in enumerate(((-11, 6), (-7, 8), (-3, 7), (1, 6), (5, 5))):
        m.box('body', (-1.5, 21, z), (1.5, 21 + hgt, z + 2.5), cr, rot=(-20, 0, 0), pivot=(0, 21, z + 1))
        m.box('body', (3.5, 18, z), (5.5, 18 + hgt * .7, z + 2), cr, rot=(-15, 0, -30), pivot=(4.5, 18, z + 1), mirror=True)
    # cristaux de la queue
    m.box('tail', (-1.2, 18, 13), (1.2, 24, 15.5), cr, rot=(30, 0, 0), pivot=(0, 18, 13))
    m.box('tail', (-1, 16, 17), (1, 22, 19), cr, rot=(45, 0, 0), pivot=(0, 16, 17))
    saddle(m, 10, 21, 1, 9, '#2a5aa0', '#e0e8f0', 'snow', metal=('metal', '#b0c0d0'))
    bridle(m, info)
    return m, quad_anims(swing=32, speed=.75)


def cerf_sylvestre():
    m = Model('cerf_sylvestre', 3)
    bark = ('bark', '#7a5634', '#5aa83a')
    info = quadruped(m, dict(bw=10, bh=11, bl=22, leg=16, lt=2.8, body_mat=bark, leg_mat=('bark', '#6a4a2c', '#5aa83a'), paw_mat=('dark', '#2a2018'),
                             neck=8, neck_w=5, neck_y=.8, neck_up=55, head_up=1, hw=6, hh=6, hl=6, snout=(4, 3.5, 3), nose=('dark', '#2a2018'),
                             eye=('eye', '#2a1a10'), ears=(2.5, 2, bark), tail=(2.5, 3, bark), tail_up=30, leg_edge=1))
    hy, hz = info['head_y'], info['head_z']
    # bois (ramure) avec feuilles
    antler, leaf = ('bark', '#8a6a44', '#6ab040'), ('leaf', '#5aa83a')
    for sx in (1, -1):
        m.box('head', (sx * 2 - .6, hy + 3, hz - 3), (sx * 2 + .6, hy + 10, hz - 1.8), antler, rot=(0, 0, -sx * 20), pivot=(sx * 2, hy + 3, hz - 2.4))
        m.box('head', (sx * 4 - .5, hy + 8, hz - 3), (sx * 4 + .5, hy + 14, hz - 2), antler, rot=(0, 0, -sx * 35), pivot=(sx * 4, hy + 8, hz - 2.5))
        m.box('head', (sx * 3 - .5, hy + 6, hz - 3), (sx * 3 + .5, hy + 11, hz - 2), antler, rot=(25, 0, sx * 10), pivot=(sx * 3, hy + 6, hz - 2.5))
        for (dx, dy) in ((5, 13), (7, 11), (3, 15), (6.5, 15.5)):
            m.box('head', (sx * dx - 1, hy + dy, hz - 3.2), (sx * dx + 1, hy + dy + 2, hz - 1.4), leaf)
    # mousse qui pend
    for z in (-8, -3, 2, 7):
        m.box('body', (5, 22, z), (5.6, 25.5, z + 2), leaf, mirror=True)
    saddle(m, 10, 27, 0, 8, '#1e7a6e', '#d4a830', 'leaf', metal=('gold', '#d9a830'))
    bridle(m, info, metal=('gold', '#d9a830'))
    return m, quad_anims(swing=24, speed=.85)


def lezard_lave():
    m = Model('lezard_lave', 4)
    rock = ('lava', '#2e2222', '#ff7a1a')
    info = quadruped(m, dict(bw=12, bh=8, bl=26, leg=6, lt=3.5, body_mat=rock, leg_mat=rock, paw_mat=('dark', '#1a1414'),
                             neck=3, neck_w=8, neck_y=.6, neck_up=5, hw=9, hh=6, hl=8, snout=(7, 4, 4), snout_mat=rock,
                             eye=('glow', '#ffb020'), tail=(5, 18, rock), tail_up=-12, upper=1.4, leg_in=-1.5))
    glow = ('glow', '#ff8a20')
    for i in range(7):
        z = -11 + i * 3.7
        h = 6 - abs(i - 2.5) * .8
        m.box('body', (-1.2, 14, z), (1.2, 14 + h, z + 2.4), glow if i % 2 else rock, rot=(-25, 0, 0), pivot=(0, 14, z + 1))
    for i in range(4):
        m.box('tail', (-1, 13, 15 + i * 4), (1, 15.5 - i * .4, 17 + i * 4), glow, rot=(-25, 0, 0), pivot=(0, 13, 16 + i * 4))
    m.box('tail', (-1.8, 8.5, 26), (1.8, 11, 33), rock, rot=(-10, 0, 0), pivot=(0, 10, 26))
    saddle(m, 12, 14, 0, 9, '#a01e14', '#e8a020', 'flame', metal=('metal', '#6a6a70'), banner_h=5)
    bridle(m, info)
    return m, quad_anims(swing=30, speed=.7, extra=[(wave('tail', 'r', 'y', 20, .7), 'walk')])


def raptor_sables():
    m = Model('raptor_sables', 5)
    feath = ('feather', '#d8c08a', '#f0e0b0')
    m.group('body', (0, 16, 0))
    m.box('body', (-5, 12, -9), (5, 21, 9), feath, rot=(-10, 0, 0), pivot=(0, 16, 0))
    # pattes arriere puissantes
    for name, sx in (('leg_bl', 1), ('leg_br', -1)):
        m.group(name, (sx * 4, 14, 3))
        m.box(name, (sx * 4 - 2, 7, 0), (sx * 4 + 2, 15, 6), feath)
        m.box(name, (sx * 4 - 1.1, 2, 3), (sx * 4 + 1.1, 8, 5.2), ('hide', '#c8a870'))
        m.box(name, (sx * 4 - 1.8, 0, 0), (sx * 4 + 1.8, 2, 5.5), ('dark', '#2a2420'))
        m.box(name, (sx * 4 - .5, 0, -1.5), (sx * 4 + .5, 1.5, 0), ('dark', '#1a1814'))
    # petits bras
    for name, sx in (('leg_fl', 1), ('leg_fr', -1)):
        m.group(name, (sx * 4.5, 15, -7), 'body')
        m.box(name, (sx * 4.5 - 1, 10, -8), (sx * 4.5 + 1, 15, -6), feath)
        m.box(name, (sx * 4.5 - .7, 9, -9), (sx * 4.5 + .7, 10.5, -7), ('dark', '#1a1814'))
    # cou + tete
    m.group('head', (0, 19, -8), 'body')
    m.box('head', (-3, 18, -15), (3, 23, -7), feath, rot=(25, 0, 0), pivot=(0, 19, -8))
    m.box('head', (-3.5, 22, -20), (3.5, 27, -13), feath)
    m.box('head', (-2, 22.5, -25), (2, 25, -20), ('dark', '#2e2a26'))      # bec
    m.box('head', (3.4, 24.5, -18.5), (3.8, 25.7, -17.2), ('eye', '#30e0e0'), mirror=True)
    m.box('head', (-3.8, 22.5, -19), (3.8, 23.5, -18), ('cloth', '#1e8a8a', '#d4a830'))     # bride turquoise
    # plumes de la crete et de la queue
    teal = ('feather', '#2aa8a0', '#80e0d8')
    for i in range(3):
        m.box('head', (-.6, 26, -18 + i * 2.2), (.6, 30 - i, -16.5 + i * 2.2), teal, rot=(-30, 0, 0), pivot=(0, 26, -17 + i * 2))
    m.group('tail', (0, 17, 9), 'body')
    m.box('tail', (-3, 15, 9), (3, 19, 20), feath, rot=(15, 0, 0), pivot=(0, 17, 9))
    for i, sx in enumerate((-1, 0, 1)):
        m.box('tail', (sx * 2.2 - 1, 14.5, 18), (sx * 2.2 + 1, 16, 30), teal, rot=(10, sx * 12, 0), pivot=(sx * 2, 15, 18))
    # ailes vestigiales
    m.box('body', (5, 15, -4), (6, 20, 8), teal, rot=(0, 0, -15), pivot=(5, 20, 0), mirror=True)
    saddle(m, 10, 21, 0, 8, '#1e8a8a', '#d4a830', 'sun')
    walk = [wave('leg_bl', 'r', 'x', 40, .6, 0), wave('leg_br', 'r', 'x', 40, .6, .5), wave('body', 'p', 'y', .8, .3),
            wave('leg_fl', 'r', 'x', 10, .6), wave('leg_fr', 'r', 'x', 10, .6, .5), wave('head', 'r', 'x', 6, .6, .25),
            wave('tail', 'r', 'y', 10, .6)]
    idle = [wave('head', 'r', 'y', 8, 3.0), wave('tail', 'r', 'y', 6, 2.0), wave('body', 'p', 'y', .3, 3.0)]
    return m, [anim('idle', 3.0, idle), anim('walk', .6, walk)]


def felin_vide():
    m = Model('felin_vide', 6)
    void = ('void', '#24163a', '#b060ff')
    info = quadruped(m, dict(bw=10, bh=9, bl=24, leg=11, lt=3.2, body_mat=void, leg_mat=void, paw_mat=('dark', '#140c20'),
                             neck=3, neck_w=8, neck_y=.7, neck_up=25, hw=9, hh=7, hl=6, snout=(5, 3, 2.5),
                             nose=('dark', '#140c20'), eye=('glow', '#d080ff'), ears=(2.5, 3, void), tail=(2.4, 16, void), tail_up=35))
    cr = ('crystal', '#9a4ae8')
    for (x, z, h, rz) in ((3.5, -8, 7, -25), (2, -2, 9, -15), (3.5, 4, 6, -30)):
        m.box('body', (x - 1.2, 19, z), (x + 1.2, 19 + h, z + 2.4), cr, rot=(-15, 0, rz), pivot=(x, 19, z + 1), mirror=True)
    m.box('tail', (-1.5, 26, 24), (1.5, 33, 27), cr, rot=(20, 0, 0), pivot=(0, 26, 25))
    for leg in ('leg_fl', 'leg_fr', 'leg_bl', 'leg_br'):
        g = m.groups[leg]['origin']
        m.box(leg, (g[0] - 2, 3, g[2] - 2), (g[0] + 2, 4.5, g[2] + 2), ('metal', '#5a5a80'))
    saddle(m, 10, 20, 1, 9, '#4a2a7a', '#c090ff', 'eye', metal=('metal', '#6a6a90'))
    bridle(m, info, metal=('metal', '#6a6a90'))
    return m, quad_anims(swing=30, speed=.65, extra=[(wave('tail', 'r', 'x', 10, 2.0), 'idle')])


def ours_runique():
    m = Model('ours_runique', 7)
    fur = ('rune', '#5a3a22', '#40e0d8')
    info = quadruped(m, dict(bw=15, bh=13, bl=24, leg=9, lt=5, body_mat=fur, leg_mat=('fur', '#5a3a22', '#3a2414'), paw_mat=('dark', '#24180e'),
                             neck=3, neck_w=10, neck_y=.6, neck_up=10, hw=10, hh=8, hl=6, snout=(5, 4, 3.5),
                             snout_mat=('hide', '#c8a070'), nose=('dark', '#1a1410'), eye=('glow', '#40f0e0'), ears=(2.5, 2.5, fur),
                             tail=(3, 2, fur), tail_up=-30))
    gold = ('gold', '#d9a830')
    for leg in ('leg_fl', 'leg_fr', 'leg_bl', 'leg_br'):
        g = m.groups[leg]['origin']
        m.box(leg, (g[0] - 3, 4, g[2] - 3), (g[0] + 3, 6, g[2] + 3), gold)
    m.box('body', (-7.8, 16, -11), (7.8, 20, -8), gold)               # collier d'epaules
    m.box('body', (7.4, 13, -6), (7.9, 19, -2), ('gold', '#e8b840'), mirror=True)
    saddle(m, 15, 22, 2, 9, '#1e6a80', '#d9a830', 'eye', metal=gold)
    bridle(m, info, metal=gold)
    return m, quad_anims(swing=22, speed=1.0)


def tortue_cristal():
    m = Model('tortue_cristal', 8)
    skin = ('scale', '#8a9488', '#5a6458')
    m.group('body', (0, 9, 0))
    m.box('body', (-10, 4, -12), (10, 8, 12), ('shell', '#1fa080', '#105a48'))     # bordure basse
    m.box('body', (-9, 8, -11), (9, 13, 11), ('shell', '#25b890', '#106050'))
    m.box('body', (-7, 13, -8), (7, 16, 8), ('shell', '#30c8a0', '#107058'))
    for (x, z) in ((-4, -4), (4, -4), (-4, 4), (4, 4), (0, 0)):
        m.box('body', (x - 2, 15.5, z - 2), (x + 2, 17, z + 2), ('crystal', '#3ae0b0'))
    for name, sx, sz in (('leg_fl', 1, -1), ('leg_fr', -1, -1), ('leg_bl', 1, 1), ('leg_br', -1, 1)):
        m.group(name, (sx * 8, 6, sz * 8))
        m.box(name, (sx * 8 - 2.5, 0, sz * 8 - 2.5), (sx * 8 + 2.5, 6.5, sz * 8 + 2.5), skin)
        m.box(name, (sx * 8 - 2.8, 0, sz * 8 - 3.2), (sx * 8 + 2.8, 1.2, sz * 8 + 2.8), ('dark', '#4a5048'))
    m.group('head', (0, 8, -12), 'body')
    m.box('head', (-3, 6, -15), (3, 10, -10), skin)
    m.box('head', (-3.5, 7, -21), (3.5, 12, -15), skin)
    m.box('head', (3.4, 10, -19.5), (3.8, 11.2, -18), ('glow', '#40f0d0'), mirror=True)
    m.box('head', (-3.7, 7.5, -17), (3.7, 8.3, -16), ('leather', '#6a4024'))
    m.group('tail', (0, 6, 12), 'body')
    m.box('tail', (-1.5, 4.5, 11), (1.5, 7, 16), skin, rot=(-15, 0, 0), pivot=(0, 6, 12))
    # selle + harnais
    m.box('body', (-5, 16.5, -3), (5, 18, 5), ('leather', '#5a3420'))
    m.box('body', (-4.5, 18, 3.5), (4.5, 20, 5), ('leather', '#5a3420'))
    m.box('body', (-1, 18, -3), (1, 20, -1.5), ('leather', '#5a3420'))
    for x in (-5, 4.5):
        m.box('body', (x, 8, -1), (x + .5, 17, 1), ('leather', '#5a3420'))
    m.box('body', (-10.3, 6, -1.5), (-10, 8.5, 1.5), ('gold', '#d9a830'), mirror=True)
    walk = [wave('leg_fl', 'r', 'x', 18, 1.4), wave('leg_br', 'r', 'x', 18, 1.4), wave('leg_fr', 'r', 'x', 18, 1.4, .5),
            wave('leg_bl', 'r', 'x', 18, 1.4, .5), wave('head', 'r', 'y', 6, 1.4), wave('body', 'p', 'y', .3, .7)]
    idle = [wave('head', 'r', 'x', 4, 4.0), wave('head', 'p', 'z', .6, 4.0, .25)]
    return m, [anim('idle', 4.0, idle), anim('walk', 1.4, walk)]


def scorpion_dunes():
    m = Model('scorpion_dunes', 9)
    chit = ('chitin', '#d8b884', '#8a5a30')
    m.group('body', (0, 8, 0))
    m.box('body', (-7, 5, -10), (7, 11, 8), chit)
    m.box('body', (-6, 11, -8), (6, 12, 6), ('chitin', '#e0c494', '#8a5a30'))
    m.box('body', (-4, 6, -14), (4, 10, -10), chit)
    m.box('body', (-2.5, 8.5, -14.4), (2.5, 9.7, -14), ('glow', '#ffa020'))
    legs = []
    for i in range(4):
        z = -7 + i * 4.5
        for sx in (1, -1):
            name = f'leg{i}{"l" if sx > 0 else "r"}'
            legs.append(name)
            m.group(name, (sx * 6.5, 8, z))
            lo, hi = sorted((sx * 6.5, sx * 11))
            m.box(name, (lo, 7.5, z - .9), (hi, 9.5, z + .9), ('chitin', '#c8a070', '#7a4a20'))
            lo, hi = sorted((sx * 10.2, sx * 12.2))
            m.box(name, (lo, 0, z - .8), (hi, 10.5, z + .8), ('chitin', '#b88a58', '#6a3a18'))
    # pinces
    for name, sx in (('claw_l', 1), ('claw_r', -1)):
        m.group(name, (sx * 4, 8, -12), 'body')
        m.box(name, (sx * 4 - 1.2, 7, -20), (sx * 4 + 1.2, 9.4, -12), chit, rot=(0, -sx * 20, 0), pivot=(sx * 4, 8, -12))
        m.box(name, (sx * 7 - 2.5, 6, -28), (sx * 7 + 2.5, 10.5, -21), ('chitin', '#e0c494', '#8a5a30'))
        m.box(name, (sx * 7 - 2, 6.5, -32), (sx * 7 - .2, 9.5, -28), ('chitin', '#b84a20', '#6a2a10'))
        m.box(name, (sx * 7 + .2, 6.5, -31), (sx * 7 + 2, 9.5, -28), ('chitin', '#b84a20', '#6a2a10'))
    # queue segmentee + dard
    m.group('tail', (0, 9, 8), 'body')
    for i, (y, z, w) in enumerate(((10, 9, 4.2), (13, 12, 3.9), (17, 13.5, 3.6), (21, 13, 3.3), (24.5, 11, 3), (26.5, 7.5, 2.8))):
        m.box('tail', (-w / 2, y - w / 2, z - w / 2), (w / 2, y + w / 2, z + w / 2), chit)
    m.box('tail', (-1.4, 23.5, 3), (1.4, 26, 6), ('chitin', '#b84a20', '#6a2a10'))
    m.box('tail', (-.6, 22, 2.2), (.6, 24, 3.4), ('dark', '#2a1a10'))
    m.box('body', (-4.5, 12, -6), (4.5, 13.2, 3), ('leather', '#5a3420'))
    m.box('body', (-4, 13.2, 1.5), (4, 15, 3), ('leather', '#5a3420'))
    m.box('body', (-4.6, 7, -4), (-4.2, 12.5, 1), ('cloth', '#8a1e1e', '#d4a830'), mirror=True)
    walk = [wave(n, 'r', 'y', 18, .6, (i % 2) * .5 + (0 if n.endswith('l') else .25)) for i, n in enumerate(legs)]
    walk += [wave('tail', 'r', 'x', 6, .6), wave('claw_l', 'r', 'y', 8, .6), wave('claw_r', 'r', 'y', 8, .6, .5)]
    idle = [wave('tail', 'r', 'x', 8, 2.5), wave('claw_l', 'r', 'y', 6, 3.0), wave('claw_r', 'r', 'y', 6, 3.0, .5)]
    return m, [anim('idle', 3.0, idle), anim('walk', .6, walk)]


def chevre_celeste():
    m = Model('chevre_celeste', 10)
    wool = ('wool', '#eef0f4', '#5a9ae8')
    info = quadruped(m, dict(bw=10, bh=10, bl=20, leg=12, lt=3, body_mat=wool, leg_mat=('wool', '#e8eaf0', '#5a9ae8'), paw_mat=('dark', '#3a3a44'),
                             neck=5, neck_w=5.5, neck_y=.8, neck_up=50, hw=6.5, hh=6.5, hl=6, snout=(4.5, 4, 2.5),
                             snout_mat=('hide', '#e0e2ea'), eye=('glow', '#40c0ff'), ears=(2.5, 1.5, wool), tail=(2, 3, wool), tail_up=40))
    hy, hz = info['head_y'], info['head_z']
    gold = ('gold', '#d9a830')
    for sx in (1, -1):   # cornes enroulees
        m.box('head', (sx * 2.5 - 1, hy + 3, hz - 4), (sx * 2.5 + 1, hy + 7, hz - 1.5), gold, rot=(30, 0, 0), pivot=(sx * 2.5, hy + 3, hz - 2.5))
        m.box('head', (sx * 3 - 1, hy + 6, hz - 1), (sx * 3 + 1, hy + 8, hz + 3), gold, rot=(-20, 0, 0), pivot=(sx * 3, hy + 7, hz))
        m.box('head', (sx * 3.5 - 1, hy + 3, hz + 2), (sx * 3.5 + 1, hy + 7, hz + 4), gold)
    m.box('head', (-1, hy - 7, hz - 7), (1, hy - 3, hz - 5.5), ('wool', '#f4f4f8', '#5a9ae8'))   # barbiche
    m.box('head', (3.3, hy - 1, hz - 2), (3.8, hy + 3, hz - 1), ('gold', '#e8c040'), mirror=True)
    saddle(m, 10, 22, 1, 8, '#2a5aa8', '#d9a830', 'chevron', metal=gold)
    bridle(m, info, metal=gold)
    return m, quad_anims(swing=26, speed=.75)


def araignee_cavernes():
    m = Model('araignee_cavernes', 11)
    chit = ('chitin', '#3a3a44', '#1e1e24')
    m.group('body', (0, 10, 0))
    m.box('body', (-6, 6, -9), (6, 13, 1), chit)                                # cephalothorax
    m.box('body', (-8, 7, 1), (8, 17, 15), ('chitin', '#34343e', '#a81e1e'))   # abdomen
    m.box('body', (-3, 15, 6), (3, 17.2, 12), ('glow', '#c02020'))
    m.group('head', (0, 9, -9), 'body')
    m.box('head', (-4.5, 5.5, -14), (4.5, 12, -9), chit)
    for (x, y) in ((-2.5, 9.5), (2.5, 9.5), (-1, 10.5), (1, 10.5), (-3.2, 8), (3.2, 8)):
        m.box('head', (x - .7, y - .7, -14.4), (x + .7, y + .7, -14), ('glow', '#ff3030'))
    m.box('head', (-3, 4, -15), (-1.5, 6.5, -13), ('bone',)); m.box('head', (1.5, 4, -15), (3, 6.5, -13), ('bone',))
    legs = []
    for i in range(4):
        z = -8 + i * 3
        for sx in (1, -1):
            name = f'leg{i}{"l" if sx > 0 else "r"}'
            legs.append(name)
            m.group(name, (sx * 5.5, 10, z))
            leg = ('chitin', '#2e2e36', '#a81e1e')
            dz = (i - 1.5) * 3
            lo, hi = sorted((sx * 5.5, sx * 11))
            m.box(name, (lo, 10, z - 1), (hi, 12, z + 1), leg)
            lo, hi = sorted((sx * 10, sx * 12.5))
            m.box(name, (lo, 10, z - 1), (hi, 17, z + 1), leg)
            lo, hi = sorted((sx * 12.5, sx * 15))
            m.box(name, (lo, 14, z - 1 + dz * .5), (hi, 17, z + 1 + dz * .5), leg)
            lo, hi = sorted((sx * 14, sx * 16))
            m.box(name, (lo, 0, z - 1 + dz), (hi, 15, z + 1 + dz), ('chitin', '#3a3a44', '#a81e1e'))
    m.box('body', (-5, 17, 3), (5, 18.5, 11), ('leather', '#5a3420'))
    m.box('body', (-4.5, 18.5, 9.5), (4.5, 20.5, 11), ('leather', '#5a3420'))
    m.box('body', (-8.3, 9, 4), (-8, 17, 10), ('cloth', '#8a1e1e', '#d4a830'), mirror=True)
    walk = [wave(n, 'r', 'y', 16, .5, (i % 2) * .5 + (0 if n.endswith('l') else .25)) for i, n in enumerate(legs)]
    walk += [wave('body', 'p', 'y', .4, .25)]
    idle = [wave('body', 'p', 'y', .3, 2.5), wave('head', 'r', 'y', 5, 3.0)]
    return m, [anim('idle', 3.0, idle), anim('walk', .5, walk)]


def hippogriffe():
    m = Model('hippogriffe', 12)
    fur, white = ('fur', '#7a5230', '#4a2e18'), ('feather', '#f0ece4', '#ffffff')
    info = quadruped(m, dict(bw=11, bh=11, bl=24, leg=13, lt=3.2, body_mat=fur, leg_mat=fur, paw_mat=('dark', '#2a2018'),
                             neck=6, neck_w=7, neck_y=.75, neck_up=45, hw=7, hh=7, hl=6, snout=(3, 3, 1), head_mat=white,
                             snout_mat=white, eye=('glow', '#f0a020'), tail=(3, 9, fur), tail_up=-25))
    hy, hz = info['head_y'], info['head_z']
    # bec d'aigle
    m.box('head', (-1.8, hy - 3.2, hz - 11), (1.8, hy + .5, hz - 6), ('gold', '#e8b830'))
    m.box('head', (-1.2, hy - 4.2, hz - 11.5), (1.2, hy - 2.5, hz - 9.5), ('gold', '#c89020'))
    m.box('body', (-4, 18, -13), (4, 26, -9), white, rot=(-30, 0, 0), pivot=(0, 22, -11))     # plastron blanc
    # pattes avant d'aigle (serres)
    for leg in ('leg_fl', 'leg_fr'):
        g = m.groups[leg]['origin']
        m.box(leg, (g[0] - 2, 0, g[2] - 2.5), (g[0] + 2, 6, g[2] + 2), ('scale', '#e0b030', '#a07018'))
        m.box(leg, (g[0] - 1.8, 0, g[2] - 3.8), (g[0] + 1.8, 1, g[2] - 2.4), ('dark', '#1a1410'))
    # ailes (os pivot sur le dos)
    wing = ('feather', '#6a4424', '#e8e0d0')
    for name, sx in (('wing_l', 1), ('wing_r', -1)):
        m.group(name, (sx * 5, 23, -5), 'body')
        x0, x1 = (5, 21) if sx > 0 else (-21, -5)
        m.box(name, (x0, 22, -7), (x1, 24, 4), wing)
        x0, x1 = (17, 31) if sx > 0 else (-31, -17)
        m.box(name, (x0, 22.5, -6), (x1, 23.8, 8), ('feather', '#e8e0d0', '#ffffff'), rot=(0, 0, sx * 10), pivot=(sx * 17, 23, 0))
        x0, x1 = (8, 20) if sx > 0 else (-20, -8)
        m.box(name, (x0, 21.6, 4), (x1, 23, 12), ('feather', '#f0ece4', '#ffffff'))
    saddle(m, 11, 24, 3, 8, '#1e4a9a', '#d9a830', 'chevron', metal=('gold', '#d9a830'))
    base = quad_anims(swing=26, speed=.8, extra=[(wave('wing_l', 'r', 'z', 6, 3.0), 'idle'), (wave('wing_r', 'r', 'z', 6, 3.0, .5), 'idle')])
    fly = [wave('wing_l', 'r', 'z', 40, .7), wave('wing_r', 'r', 'z', 40, .7, .5), wave('body', 'p', 'y', 1.2, .7, .25),
           track('leg_fl', 'r', [(0, 50, 0, 0), (1, 50, 0, 0)]), track('leg_fr', 'r', [(0, 50, 0, 0), (1, 50, 0, 0)]),
           track('leg_bl', 'r', [(0, -45, 0, 0), (1, -45, 0, 0)]), track('leg_br', 'r', [(0, -45, 0, 0), (1, -45, 0, 0)]),
           wave('tail', 'r', 'x', 8, .7)]
    return m, base + [anim('fly', .7, fly)]


MOUNTS = [sanglier, loup_givre, cerf_sylvestre, lezard_lave, raptor_sables, felin_vide,
          ours_runique, tortue_cristal, scorpion_dunes, chevre_celeste, araignee_cavernes, hippogriffe]


def main():
    out_models = ROOT / 'bbmodels/mounts'
    out_tex = ROOT / 'textures/imported/mounts'
    out_models.mkdir(parents=True, exist_ok=True)
    out_tex.mkdir(parents=True, exist_ok=True)
    for f in MOUNTS:
        m, anims = f()
        model, tex = m.build(anims)
        (out_models / (m.key + '.json')).write_text(json.dumps(model, separators=(',', ':')), encoding='utf-8')
        tex.save(out_tex / (m.key + '.png'))
        ys = [b['to'][1] for b in m.boxes]
        print(f'{m.key}: {len(m.boxes)} cubes, hauteur {max(ys) / 16:.2f} blocs')


if __name__ == '__main__':
    main()
