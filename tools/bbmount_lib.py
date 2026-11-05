"""Bibliotheque des montures .bbmodel : geometrie lissee (tranches, courbes), peinture 3D continue, export .bbmodel + format runtime.

Repere Blockbench : x largeur, y hauteur (0 = sol), z longueur, tete vers -z. 16 unites = 1 bloc.
Les peintures sont calculees a partir de la position 3D de chaque pixel : une texture continue d'un cube a l'autre (pas de rayures).
"""
import base64
import io
import json
import math
import uuid as uuidlib
from pathlib import Path

import numpy as np
from PIL import Image

import gen_mounts as gm
from gen_mounts import wave, anim, quad_anims, track  # noqa: F401  (reexportes)

ATLAS = 1024
FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=float)


# ------------------------------------------------------------------ bruit 3D

def _hash(ix, iy, iz, seed):
    n = (ix * 374761393 + iy * 668265263 + iz * 2147483647 + seed * 1274126177) & 0xffffffff
    n = ((n ^ (n >> 13)) * 1274126177) & 0xffffffff
    return ((n ^ (n >> 16)) & 0xffff) / 65535.0


def vnoise(P, s, seed=1):
    q = P * np.array(s)
    i = np.floor(q).astype(np.int64)
    f = q - i
    f = f * f * (3 - 2 * f)
    ix, iy, iz = i[..., 0], i[..., 1], i[..., 2]
    fx, fy, fz = f[..., 0], f[..., 1], f[..., 2]
    out = 0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (fx if dx else 1 - fx) * (fy if dy else 1 - fy) * (fz if dz else 1 - fz)
                out = out + w * _hash(ix + dx, iy + dy, iz + dz, seed)
    return out


def fbm(P, s, seed=1):
    s = np.array(s, dtype=float)
    return .6 * vnoise(P, s, seed) + .3 * vnoise(P, s * 2.1, seed + 7) + .1 * vnoise(P, s * 4.3, seed + 13)


def lerp(a, b, t):
    return a + (b - a) * t[..., None]


# ------------------------------------------------------------------ materiaux peints (position 3D -> couleur)

class Shader:
    def __init__(self, height, seed):
        self.ymax = max(height, 1)
        self.seed = seed
        self.legacy = gm.Painter(seed)

    def color(self, mat, P, N, w, h, face):
        kind, c = mat[0], mat[1:]
        fn = getattr(self, 's_' + kind, None)
        if fn is None:                                                     # cloth, eye, glow... : peinture plate existante
            img = self.legacy.paint(mat, h, w, face)[..., :3].astype(float)
            return img, kind in ('eye', 'glow', 'cloth', 'gold_flat')
        return fn(P, *c), False

    # --- poils : touffes allongees dans le sens du corps + eclaircissement du ventre
    def s_fur(self, P, color, dark=None):
        c = rgb(color); d = rgb(dark) if dark else c * .55
        a = fbm(P, (.16, .16, .1), self.seed)
        b = vnoise(P, (1.1, 1.1, .32), self.seed + 3)
        t = np.clip(.55 + (a - .5) * 1.4 + (b - .5) * .55, 0, 1)
        img = lerp(d, c, t)
        belly = np.clip((P[..., 1] - 6) / 14, 0, 1)[..., None]
        return img * (.8 + .2 * belly) * (1 + (b[..., None] - .5) * .12)

    def s_hide(self, P, color):
        c = rgb(color)
        a = fbm(P, (.22, .22, .22), self.seed)
        wr = vnoise(P, (.9, 2.4, .9), self.seed + 5)
        t = np.clip(.5 + (a - .5) * 1.1, 0, 1)
        img = c * (.72 + .5 * t[..., None])
        return img * (1 - .1 * (wr[..., None] > .62))

    def s_dark(self, P, color='#2a2420'):
        c = rgb(color)
        return c * (.8 + .4 * fbm(P, (.3, .3, .3), self.seed)[..., None])

    def s_bone(self, P, color='#e8dfc4'):
        c = rgb(color)
        g = vnoise(P, (.5, .5, 1.4), self.seed + 2)
        return c * (.82 + .3 * g[..., None]) + 6

    def s_leather(self, P, color='#6b4024'):
        c = rgb(color)
        g = fbm(P, (.5, .5, .5), self.seed + 9)
        grain = vnoise(P, (2.4, 2.4, 2.4), self.seed + 4)
        return c * (.75 + .4 * g[..., None] + .12 * grain[..., None])

    def s_metal(self, P, color='#9aa0a8'):
        c = rgb(color)
        br = vnoise(P, (.25, 2.6, 2.6), self.seed + 1)
        sheen = 1 + .22 * np.sin(P[..., 1] * .9 + P[..., 2] * .5)
        return c * (.8 + .3 * br[..., None]) * sheen[..., None]

    def s_gold(self, P, color='#d9a830'):
        c = rgb(color)
        sheen = .86 + .26 * np.sin(P[..., 1] * 1.3 + P[..., 0] * .8 + 1.5)
        n = fbm(P, (.6, .6, .6), self.seed + 8)
        return c * sheen[..., None] * (.9 + .2 * n[..., None])

    def s_plate(self, P, color, dark=None):
        return self.s_scale(P, color, dark, .2)

    def s_scale(self, P, color, dark=None, cell_scale=.42):
        """Ecailles : cellules de Voronoi 3D arrondies, bord sombre, reflet par ecaille."""
        c = rgb(color); d = rgb(dark) if dark else c * .55
        q = P * cell_scale
        i = np.floor(q)
        best = np.full(P.shape[:-1], 9.0); second = best.copy(); cell = np.zeros(P.shape[:-1])
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    g = i + np.array([dx, dy, dz])
                    j = np.stack([_hash(g[..., 0].astype(np.int64), g[..., 1].astype(np.int64), g[..., 2].astype(np.int64), self.seed + k) for k in (1, 2, 3)], -1)
                    dist = np.linalg.norm(g + j - q, axis=-1)
                    new_best = dist < best
                    second = np.where(new_best, best, np.minimum(second, dist))
                    cell = np.where(new_best, _hash(g[..., 0].astype(np.int64), g[..., 1].astype(np.int64), g[..., 2].astype(np.int64), self.seed + 9), cell)
                    best = np.minimum(best, dist)
        edge = np.clip((second - best) * 3.2, 0, 1)
        t = (.45 + .55 * cell) * (.55 + .45 * edge)
        img = lerp(d, c, np.clip(t, 0, 1))
        return img * (.78 + .3 * edge[..., None])

    def s_shell(self, P, color, line='#1f3a2c'):
        """Carapace : plaques polygonales sombres separees par des sillons, usure par taches."""
        c = rgb(color); ln = rgb(line)
        q = P[..., [0, 2]] * .13
        row = np.floor(q[..., 1] + .5)
        qx = q[..., 0] + (row % 2) * .5
        fx, fz = qx - np.floor(qx + .5), q[..., 1] - row
        edge = np.clip(1 - np.maximum(np.abs(fx) * 2, np.abs(fz) * 2) ** 3, 0, 1)
        wear = fbm(P, (.2, .2, .2), self.seed)
        base = c * (.7 + .5 * wear[..., None]) * (.6 + .5 * edge[..., None])
        groove = (edge < .18)[..., None]
        return np.where(groove, ln, base)

    def s_feather(self, P, color, tip=None):                               # membrane d'aile / plumes : nervures
        c = rgb(color); t = rgb(tip) if tip else c * 1.3
        v = vnoise(P, (.3, .3, 1.0), self.seed + 6)
        veins = (np.abs(np.sin(P[..., 2] * .55 + P[..., 0] * .15)) < .12)[..., None]
        return np.where(veins, c * .6, lerp(c, t, v) * 1.0)

    def s_cloth_plain(self, P, color):
        c = rgb(color)
        w = (np.sin(P[..., 0] * 3.1) * np.sin(P[..., 2] * 3.1) > .2)[..., None]
        return c * (.9 + .1 * w) * (.85 + .3 * fbm(P, (.3, .3, .3), self.seed)[..., None])


# ------------------------------------------------------------------ modele

def euler_matrix(rot):
    rx, ry, rz = (math.radians(a) for a in rot)
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


NORMALS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}


def face_points(b, face, w, h):
    """Position 3D (repere modele) de chaque pixel d'une face, selon la convention UV de ImportedModel."""
    (x0, y0, z0), (x1, y1, z1) = b['from'], b['to']
    jj, ii = np.meshgrid((np.arange(w) + .5) / w, (np.arange(h) + .5) / h)
    u, v = jj, ii
    if face == 'north': xr, yr, zr = 1 - u, 1 - v, 0 * u
    elif face == 'south': xr, yr, zr = u, 1 - v, 0 * u + 1
    elif face == 'east': xr, yr, zr = 0 * u + 1, 1 - v, 1 - u
    elif face == 'west': xr, yr, zr = 0 * u, 1 - v, u
    elif face == 'up': xr, yr, zr = u, 0 * u + 1, v
    else: xr, yr, zr = u, 0 * u, 1 - v
    P = np.stack([x0 + (x1 - x0) * xr, y0 + (y1 - y0) * yr, z0 + (z1 - z0) * zr], -1)
    N = np.array(NORMALS[face], dtype=float)
    if any(b['rotation']):
        R = euler_matrix(b['rotation'])
        o = np.array(b['origin'])
        P = (P - o) @ R.T + o
        N = R @ N
    return P, N


class Model:
    """Meme interface que gen_mounts.Model (group / box), avec faces masquables et peinture 3D."""

    def __init__(self, key, seed):
        self.key, self.seed = key, seed
        self.groups, self.top, self.boxes, self.n = {}, [], [], 0

    def group(self, name, pivot, parent=None, rot=(0, 0, 0)):
        g = {'name': name, 'origin': [float(v) for v in pivot], 'rotation': [float(v) for v in rot], 'children': []}
        self.groups[name] = g
        (self.groups[parent]['children'] if parent else self.top).append(g)
        return name

    def box(self, grp, frm, to, mat, rot=None, pivot=None, mirror=False, hide=()):
        self._box(grp, frm, to, mat, rot, pivot, hide)
        if mirror:
            f2 = [-to[0], frm[1], frm[2]]; t2 = [-frm[0], to[1], to[2]]
            r2 = [rot[0], -rot[1], -rot[2]] if rot else None
            p2 = [-pivot[0], pivot[1], pivot[2]] if pivot else None
            self._box(grp, f2, t2, mat, r2, p2, hide)

    def _box(self, grp, frm, to, mat, rot, pivot, hide=()):
        self.n += 1
        g = self.groups[grp]
        lo = [float(min(a, b)) for a, b in zip(frm, to)]; hi = [float(max(a, b)) for a, b in zip(frm, to)]
        for i in range(3):                                       # jamais de cube plat
            if hi[i] - lo[i] < .15: hi[i] = lo[i] + .15
        b = {'name': f'{self.key}_{self.n}', 'from': lo, 'to': hi, 'origin': [float(v) for v in (pivot or g['origin'])],
             'rotation': [float(v) for v in (rot or (0, 0, 0))], 'mat': mat, 'hide': set(hide)}
        g['children'].append(b)
        self.boxes.append(b)

    # ---- pose des textures
    def build(self, anims, density=5.0):
        height = max(b['to'][1] for b in self.boxes)
        shader = Shader(height, self.seed)
        while True:
            rects = []
            for b in self.boxes:
                dx, dy, dz = (b['to'][i] - b['from'][i] for i in range(3))
                dims = {'north': (dx, dy), 'south': (dx, dy), 'east': (dz, dy), 'west': (dz, dy), 'up': (dx, dz), 'down': (dx, dz)}
                for f, (fw, fh) in dims.items():
                    if f in b['hide']: continue
                    rects.append((b, f, max(2, round(fw * density)), max(2, round(fh * density))))
            rects.sort(key=lambda r: -r[3])
            x = y = row = 0; ok = True; placed = []
            for b, f, w, h in rects:
                if x + w + 1 > ATLAS: x, y, row = 0, y + row + 1, 0
                if y + h + 1 > ATLAS: ok = False; break
                placed.append((b, f, x, y, w, h)); x += w + 1; row = max(row, h)
            if ok: break
            density *= .93
        atlas = np.zeros((ATLAS, ATLAS, 4), dtype=np.uint8)
        light = np.array([-.35, .8, -.45]); light /= np.linalg.norm(light)
        for b, f, x, y, w, h in placed:
            P, N = face_points(b, f, w, h)
            img, flat = shader.color(b['mat'], P, N, w, h, f)
            if not flat:
                lam = .5 + .5 * float(np.clip(N @ light, -1, 1))
                ao = (.72 + .28 * np.clip(P[..., 1] / (height * .8), 0, 1))[..., None]
                img = img * (.62 + .5 * lam) * ao
            atlas[y:y + h, x:x + w, :3] = np.clip(img, 0, 255).astype(np.uint8)
            atlas[y:y + h, x:x + w, 3] = 255
            b.setdefault('uv', {})[f] = [x, y, x + w, y + h]
        self.density = density
        return Image.fromarray(atlas, 'RGBA'), anims


# ------------------------------------------------------------------ primitives lissees

def blob(m, g, c, r, mat, rot=None, pv=None, step=2.0, bands=3, mirror=False):
    """Ellipsoide c=(cx,cy,cz), r=(rx,ry,rz) : tranches en z, chaque tranche = 3 bandes pour un profil arrondi."""
    if mirror:
        blob(m, g, (-c[0], c[1], c[2]), r, mat, [rot[0], -rot[1], -rot[2]] if rot else None, [-pv[0], pv[1], pv[2]] if pv else None, step, bands)
    cx, cy, cz = c; rx, ry, rz = r
    pv = pv or c
    n = min(max(2, math.ceil(2 * rz / step)), 11 if rz > 9 else 7)
    for k in range(n):
        t = ((k + .5) / n) * 2 - 1
        f = math.sqrt(max(.0, 1 - t * t)); f = max(f, .18)
        za, zb = cz - rz + 2 * rz * k / n, cz - rz + 2 * rz * (k + 1) / n
        hide = set()
        if k > 0: hide.add('north')
        if k < n - 1: hide.add('south')
        w, hh = rx * f, ry * f
        if bands >= 3 and hh > 2.8:
            m.box(g, (cx - w, cy - hh * .6, za), (cx + w, cy + hh * .6, zb), mat, rot, pv, hide)
            wm = w * .8
            m.box(g, (cx - wm, cy + hh * .6, za), (cx + wm, cy + hh, zb), mat, rot, pv, hide | {'down'})
            m.box(g, (cx - wm, cy - hh, za), (cx + wm, cy - hh * .6, zb), mat, rot, pv, hide | {'up'})
        else:
            m.box(g, (cx - w, cy - hh, za), (cx + w, cy + hh, zb), mat, rot, pv, hide)


def loft(m, g, z0, z1, fn, mat, step=2.2):
    """Corps profile : fn(t) -> (cx, cy, rx, ry), t = 0 (z0, avant) .. 1 (z1, arriere). Tranches de 3 bandes."""
    n = max(3, math.ceil(abs(z1 - z0) / step))
    for k in range(n):
        t = (k + .5) / n
        cx, cy, rx, ry = fn(t)
        za, zb = z0 + (z1 - z0) * k / n, z0 + (z1 - z0) * (k + 1) / n
        hide = set()
        if k > 0: hide.add('north')
        if k < n - 1: hide.add('south')
        m.box(g, (cx - rx, cy - ry * .6, za), (cx + rx, cy + ry * .6, zb), mat, None, None, hide)
        m.box(g, (cx - rx * .82, cy + ry * .6, za), (cx + rx * .82, cy + ry, zb), mat, None, None, hide | {'down'})
        m.box(g, (cx - rx * .82, cy - ry, za), (cx + rx * .82, cy - ry * .6, zb), mat, None, None, hide | {'up'})


def limb(m, g, x, z, y_top, y_bot, w_top, w_bot, d_top, d_bot, mat, step=2.2, bulge=0, shift=0):
    """Colonne effilee (demi-dimensions) de y_top vers y_bot ; bulge = renflement du milieu ; shift = decalage z du bas."""
    n = max(2, math.ceil((y_top - y_bot) / step))
    for k in range(n):
        t = (k + .5) / n
        ya, yb = y_top - (y_top - y_bot) * k / n, y_top - (y_top - y_bot) * (k + 1) / n
        bl = 1 + bulge * math.sin(math.pi * t)
        w = (w_top + (w_bot - w_top) * t) * bl; d = (d_top + (d_bot - d_top) * t) * bl
        zc = z + shift * t
        hide = set()
        if k > 0: hide.add('up')
        if k < n - 1: hide.add('down')
        m.box(g, (x - w, yb, zc - d), (x + w, ya, zc + d), mat, None, None, hide)


def curve(m, g, pts, r0, r1, mat, step_hide=True):
    """Chaine de segments orientes le long des points (corne, defense, trompe, crete) ; rayon r0 -> r1."""
    pts = [np.array(p, dtype=float) for p in pts]
    segs = len(pts) - 1
    for i in range(segs):
        p, q = pts[i], pts[i + 1]
        d = q - p; L = float(np.linalg.norm(d))
        if L < 1e-6: continue
        d = d / L
        a = -math.asin(max(-1, min(1, d[1]))); b = math.atan2(d[0], d[2])
        ra = r0 + (r1 - r0) * i / segs; rb = r0 + (r1 - r0) * (i + 1) / segs
        r = (ra + rb) / 2
        hide = set()
        if i > 0: hide.add('north')
        if i < segs - 1: hide.add('south')
        m.box(g, (p[0] - r, p[1] - r, p[2] - .25), (p[0] + r, p[1] + r, p[2] + L + .25), mat,
              (math.degrees(a), math.degrees(b), 0), tuple(p), hide)


def bez(p0, p1, p2, n=7, p3=None):
    pts = []
    for i in range(n + 1):
        t = i / n
        if p3 is None:
            q = [(1 - t) ** 2 * p0[k] + 2 * (1 - t) * t * p1[k] + t * t * p2[k] for k in range(3)]
        else:
            q = [(1 - t) ** 3 * p0[k] + 3 * (1 - t) ** 2 * t * p1[k] + 3 * (1 - t) * t * t * p2[k] + t ** 3 * p3[k] for k in range(3)]
        pts.append(tuple(q))
    return pts


def mirror_pts(pts):
    return [(-x, y, z) for x, y, z in pts]


# ------------------------------------------------------------------ export

def new_uuid(key, name):
    return str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, f'aeloria/{key}/{name}'))


def to_bbmodel(m, tex, anims):
    key = m.key
    buf = io.BytesIO(); tex.save(buf, 'PNG')
    elements = []
    gid = {}

    def node(n):
        if 'children' in n:
            gid[n['name']] = new_uuid(key, 'g_' + n['name'])
            return {'name': n['name'], 'uuid': gid[n['name']], 'origin': n['origin'], 'rotation': n['rotation'], 'color': 0,
                    'export': True, 'visibility': True, 'children': [node(c) for c in n['children']]}
        faces = {}
        for f in FACES:
            faces[f] = {'uv': n['uv'][f], 'texture': 0} if f in n['uv'] else {'uv': [0, 0, 0, 0], 'texture': None}
        u = new_uuid(key, n['name'])
        elements.append({'name': n['name'], 'type': 'cube', 'uuid': u, 'from': n['from'], 'to': n['to'], 'origin': n['origin'],
                         'rotation': n['rotation'], 'faces': faces, 'box_uv': False, 'color': 0, 'export': True, 'visibility': True,
                         'locked': False, 'autouv': 0, 'rescale': False})
        return u

    outliner = [node(g) for g in m.top]
    animations = []
    for a in anims:
        animators = {}
        for line in a['tracks'].split('\n'):
            bone, code, frames = line.split('|')
            ch = 'rotation' if code == 'r' else 'position'
            ent = animators.setdefault(gid[bone], {'name': bone, 'type': 'bone', 'keyframes': []})
            for fr in frames.split(';'):
                t, x, y, z, interp = fr.split(',')
                x, y, z = float(x), float(y), float(z)
                if code == 'r': x, y = -x, -y
                if code == 'p': x = -x
                ent['keyframes'].append({'channel': ch, 'time': float(t), 'interpolation': 'catmullrom' if interp == 'c' else 'linear',
                                         'uuid': new_uuid(key, f'k{a["name"]}{bone}{code}{t}'),
                                         'data_points': [{'x': f'{x:g}', 'y': f'{y:g}', 'z': f'{z:g}'}]})
        animations.append({'uuid': new_uuid(key, 'a_' + a['name']), 'name': a['name'], 'length': a['length'], 'loop': 'loop' if a['loop'] else 'once',
                           'override': False, 'snapping': 24, 'animators': animators})
    return {'meta': {'format_version': '4.10', 'model_format': 'free', 'box_uv': False, 'creation_time': 0},
            'name': key, 'model_identifier': key, 'visible_box': [1, 1, 0], 'variable_placeholders': '', 'resolution': {'width': ATLAS, 'height': ATLAS},
            'elements': elements, 'outliner': outliner,
            'textures': [{'path': '', 'name': key + '.png', 'folder': '', 'namespace': '', 'id': '0', 'particle': False, 'render_mode': 'default',
                          'render_sides': 'auto', 'frame_time': 1, 'frame_order_type': 'loop', 'frame_order': '', 'frame_interpolate': False,
                          'visible': True, 'internal': True, 'saved': True, 'uuid': new_uuid(key, 'tex'),
                          'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()}],
            'animations': animations}


def bbmodel_to_runtime(data):
    """.bbmodel -> (modele runtime lu par ImportedModel, PNG). Les os sont identifies par leur nom."""
    els = {e['uuid']: e for e in data['elements']}
    names = {}

    def walk(n):
        if isinstance(n, str):
            e = els[n]
            faces = {k: {'uv': v['uv'], 'texture': 0} for k, v in e['faces'].items() if v.get('texture') is not None}
            return {'uuid': e['name'], 'from': e['from'], 'to': e['to'], 'origin': e['origin'], 'rotation': e['rotation'], 'faces': faces}
        names[n['uuid']] = n['name']
        return {'uuid': n['name'], 'origin': n['origin'], 'rotation': n['rotation'], 'children': [walk(c) for c in n['children']]}

    parts = [walk(n) for n in data['outliner']]
    anims = []
    for a in data['animations']:
        lines = []
        for gid, an in a['animators'].items():
            for channel, code in (('rotation', 'r'), ('position', 'p')):
                frames = []
                for k in sorted((k for k in an['keyframes'] if k['channel'] == channel), key=lambda k: k['time']):
                    p = k['data_points'][0]
                    x, y, z = float(p['x']), float(p['y']), float(p['z'])
                    if code == 'r': x, y = -x, -y
                    if code == 'p': x = -x
                    frames.append(','.join(map(str, [k['time'], x, y, z, 'c' if k['interpolation'] == 'catmullrom' else 'l'])))
                if frames: lines.append(an['name'] + '|' + code + '|' + ';'.join(frames))
        anims.append({'name': a['name'], 'length': a['length'], 'loop': a['loop'] == 'loop', 'tracks': '\n'.join(lines)})
    png = base64.b64decode(data['textures'][0]['source'].split(',', 1)[1])
    return {'resolution': data['resolution'], 'parts': parts, 'animations': anims}, png


# ------------------------------------------------------------------ apercu logiciel (z-buffer) pour iterer vite

def render(m, tex, yaw=-35, pitch=18, size=640, anim_rot=None):
    """Rendu orthographique texture ; yaw/pitch en degres. Retourne une image PIL."""
    T = np.array(tex)
    allp = np.array([[*b['from']] for b in m.boxes] + [[*b['to']] for b in m.boxes])
    ctr = (allp.min(0) + allp.max(0)) / 2
    ext = float(np.linalg.norm(allp.max(0) - allp.min(0)))
    sc = size / (ext * 1.05)
    ya, pa = math.radians(yaw), math.radians(pitch)
    Ry = np.array([[math.cos(ya), 0, math.sin(ya)], [0, 1, 0], [-math.sin(ya), 0, math.cos(ya)]])
    Rx = np.array([[1, 0, 0], [0, math.cos(pa), -math.sin(pa)], [0, math.sin(pa), math.cos(pa)]])
    V = Rx @ Ry
    col = np.zeros((size, size, 3)); col[:] = (200, 214, 226)
    zb = np.full((size, size), -1e9)
    for b in m.boxes:
        (x0, y0, z0), (x1, y1, z1) = b['from'], b['to']
        o = np.array(b['origin']); R = euler_matrix(b['rotation']) if any(b['rotation']) else np.eye(3)
        for f, uv in b['uv'].items():
            N = R @ np.array(NORMALS[f], dtype=float)
            if (V @ N)[2] > -1e-3 and False: continue
            wpix, hpix = uv[2] - uv[0], uv[3] - uv[1]
            P, _ = face_points(b, f, wpix, hpix)
            # corners via grid points : project each texel centre and splat as quads (texel step in screen space)
            W = (P - ctr) @ V.T
            if (V @ N)[2] > 0: continue                       # camera regarde vers -z du repere vue : face tournee vers l'arriere
            sx = size / 2 + W[..., 0] * sc; sy = size / 2 - W[..., 1] * sc; sz = -W[..., 2]
            texel = T[uv[1]:uv[3], uv[0]:uv[2], :3].astype(float)
            lam = .8 + .2 * float(np.clip(-(V @ N)[2], 0, 1))
            r = max(1, int(math.ceil(sc * .35 * (b['to'][0] - b['from'][0] + b['to'][1] - b['from'][1] + 0.01) / 6 / max(1, 1))))
            step_px = max(1.0, sc * (1 / 5.0) * 1.1)
            rr = int(math.ceil(step_px / 2))
            xi = np.round(sx).astype(int); yi = np.round(sy).astype(int)
            for dy in range(-rr, rr + 1):
                for dx in range(-rr, rr + 1):
                    px = np.clip(xi + dx, 0, size - 1); py = np.clip(yi + dy, 0, size - 1)
                    better = sz > zb[py, px]
                    zb[py[better], px[better]] = sz[better]
                    col[py[better], px[better]] = texel[better] * lam
    return Image.fromarray(np.clip(col, 0, 255).astype(np.uint8))
