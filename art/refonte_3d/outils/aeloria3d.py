#!/usr/bin/env python3
"""Atelier de modeles Aeloria : construit un projet Blockbench natif (.bbmodel 5.0, format « free », UV par face) et son export
pour ImportedModel (assets/mmorpg/bbmodels/<kind>/<id>.json + PNG).

Repere Blockbench : x largeur, y hauteur (0 = sol), z longueur, avant vers -z. 16 unites = 1 bloc de modele.
Regles garanties ici :
 - grille : chaque coordonnee x densite est entiere, donc chaque face a un nombre entier de texels (aucun texel etire) ;
 - faces cachees retirees (cube contre cube dans le meme os) : moins de triangles, pas de scintillement interne ;
 - surfaces coplanaires superposees detectees (scintillement) ;
 - noms de cubes et d'os uniques (ImportedModel adresse les parties par nom ; une piste sur un os absent fait planter le chargement).
"""
import base64
import hashlib
import io
import json
import math
import uuid as uuidlib
from pathlib import Path

import numpy as np
from PIL import Image

NS = uuidlib.UUID('b9d2a4f6-37c1-4d6e-8f0a-5e1c7a90d3b2')
FACES = ('north', 'east', 'south', 'west', 'up', 'down')
MIRROR_FACE = {'east': 'west', 'west': 'east'}
NORMAL = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}


def uid(*parts):
    return str(uuidlib.uuid5(NS, '/'.join(map(str, parts))))


def hex2rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float32)


# ------------------------------------------------------------------ rotations (ordre Blockbench : Rz Ry Rx)
def euler(r):
    ax, ay, az = (math.radians(v) for v in r)
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


# ------------------------------------------------------------------ bruits deterministes
def hash3(ix, iy, iz, seed=0):
    n = (np.asarray(ix, dtype=np.int64) * 374761393 + np.asarray(iy, dtype=np.int64) * 668265263 + np.asarray(iz, dtype=np.int64) * 1442968193 + seed * 1274126177) & 0xffffffff
    n = ((n ^ (n >> 13)) * 1274126177) & 0xffffffff
    return ((n ^ (n >> 16)) & 0xffff) / 65535.0


def vnoise(P, freq, seed=0):
    """Bruit de valeur 3D continu (P[...,3]) : patches continus d'un cube a l'autre."""
    q = P * np.asarray(freq, dtype=float)
    i = np.floor(q).astype(np.int64)
    f = q - i
    f = f * f * (3 - 2 * f)
    out = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (f[..., 0] if dx else 1 - f[..., 0]) * (f[..., 1] if dy else 1 - f[..., 1]) * (f[..., 2] if dz else 1 - f[..., 2])
                out = out + w * hash3(i[..., 0] + dx, i[..., 1] + dy, i[..., 2] + dz, seed)
    return out


class Bone:
    def __init__(self, name, pivot, parent, rot):
        self.name, self.pivot, self.parent, self.rot = name, tuple(map(float, pivot)), parent, tuple(map(float, rot))
        self.children = []                      # Bone ou Cube, dans l'ordre d'affichage
        self.uuid = None


class Cube:
    def __init__(self, name, bone, frm, to, mat, rot, pivot, hide, opts):
        self.name, self.bone, self.frm, self.to, self.mat = name, bone, np.array(frm, float), np.array(to, float), mat
        self.rot = tuple(map(float, rot)) if rot else (0.0, 0.0, 0.0)
        self.pivot = tuple(map(float, pivot)) if pivot is not None else tuple(((self.frm + self.to) / 2).tolist())
        self.hide = set(hide or ())
        self.opts = opts
        self.faces = {}                         # face -> (u0, v0, u1, v1) en texels
        self.decals = []
        self.uuid = None
        self.twin = None                        # cube +x dont ce cube -x reprend la texture en miroir

    @property
    def rotated(self):
        return any(abs(v) > 1e-6 for v in self.rot)

    def matrix(self):
        return euler(self.rot)

    def to_world(self, P):
        if not self.rotated:
            return P
        o = np.array(self.pivot)
        return (P - o) @ self.matrix().T + o

    def from_world(self, P):
        if not self.rotated:
            return P
        o = np.array(self.pivot)
        return (P - o) @ self.matrix() + o

    def inside_world(self, P, eps=1e-3):
        Q = self.from_world(P)
        return np.all((Q > self.frm + eps) & (Q < self.to - eps), axis=-1)


class FaceCtx:
    """Tout ce qu'un peintre sait d'une face : position de chaque texel, repere de surface, dimensions."""

    def __init__(self, proj, cube, face, w, h):
        self.proj, self.cube, self.face, self.W, self.H = proj, cube, face, w, h
        d = proj.density
        self.d = d
        a, b = cube.frm, cube.to
        col = (np.arange(w) + .5) / w
        row = (np.arange(h) + .5) / h
        U, V = np.meshgrid(col, row)
        x0, y0, z0 = a
        x1, y1, z1 = b
        lerp = lambda p, q, t: p + (q - p) * t
        if face == 'north':
            X, Y, Z = lerp(x1, x0, U), lerp(y1, y0, V), np.full_like(U, z0)
            S, T = -X * d, -Y * d
        elif face == 'south':
            X, Y, Z = lerp(x0, x1, U), lerp(y1, y0, V), np.full_like(U, z1)
            S, T = X * d, -Y * d
        elif face == 'east':
            X, Y, Z = np.full_like(U, x1), lerp(y1, y0, V), lerp(z1, z0, U)
            S, T = -Z * d, -Y * d
        elif face == 'west':
            X, Y, Z = np.full_like(U, x0), lerp(y1, y0, V), lerp(z0, z1, U)
            S, T = Z * d, -Y * d
        elif face == 'up':
            X, Y, Z = lerp(x0, x1, U), np.full_like(U, y1), lerp(z0, z1, V)
            S, T = X * d, Z * d
        else:
            X, Y, Z = lerp(x0, x1, U), np.full_like(U, y0), lerp(z1, z0, V)
            S, T = X * d, -Z * d
        self.Plocal = np.stack([X, Y, Z], -1)
        self.P = cube.to_world(self.Plocal)
        self.N = np.array(NORMAL[face], float) @ (cube.matrix().T if cube.rotated else np.eye(3))
        self.S, self.T = S, T                    # coordonnees de surface en texels (continues d'un cube a l'autre)
        self.Iu, self.Iv = np.meshgrid(np.arange(w), np.arange(h))
        self.is_side = face not in ('up', 'down')
        self.rng = np.random.default_rng(int(hashlib.md5(f'{proj.name}/{cube.name}/{face}'.encode()).hexdigest()[:8], 16))

    def noise(self, freq, seed=0):
        return vnoise(self.P, freq, seed + self.proj.seed)

    def cells(self, cw, ch, stagger=0.5, seed=0):
        """Cellules decalees en surface : (identifiant aleatoire, x local, y local)."""
        row = np.floor(self.T / ch)
        sx = self.S + np.mod(row, 2) * cw * stagger
        col = np.floor(sx / cw)
        axis = {'north': 1, 'south': 2, 'east': 3, 'west': 4, 'up': 5, 'down': 6}[self.face]
        return hash3(col, row, axis, seed + self.proj.seed), sx - col * cw, self.T - row * ch

    def xy(self, along, y):
        """Texel (colonne, ligne) d'un point de surface donne : (z ou x, y) pour un cote, (x, z) pour dessus/dessous."""
        a, b, d = self.cube.frm, self.cube.to, self.d
        if self.face == 'east':
            return (b[2] - along) * d, (b[1] - y) * d
        if self.face == 'west':
            return (along - a[2]) * d, (b[1] - y) * d
        if self.face == 'north':
            return (b[0] - along) * d, (b[1] - y) * d
        if self.face == 'south':
            return (along - a[0]) * d, (b[1] - y) * d
        if self.face == 'up':
            return (along - a[0]) * d, (y - a[2]) * d
        return (along - a[0]) * d, (b[2] - y) * d


class Project:
    def __init__(self, name, density=2, kind='mounts', seed=1, description=''):
        self.name, self.density, self.kind, self.seed, self.description = name, density, kind, seed, description
        self.roots = []
        self.bones = {}
        self.cubes = []
        self.anims = []
        self.meta = {}
        self.res = None
        self.texture = None
        self.warnings = []

    # ---------------------------------------------------------------- construction
    def bone(self, name, pivot, parent=None, rot=(0, 0, 0)):
        if name in self.bones:
            raise ValueError(f'os en double : {name}')
        p = self.bones[parent] if parent else None
        b = Bone(name, pivot, p, rot)
        b.uuid = uid(self.name, 'bone', name)
        self.bones[name] = b
        (p.children if p else self.roots).append(b)
        return b

    def cube(self, bone, name, frm, to, mat, rot=None, pivot=None, hide=None, **opts):
        d = self.density
        for v in list(frm) + list(to):
            if abs(v * d - round(v * d)) > 1e-6:
                raise ValueError(f'{self.name}/{name} : {v} hors grille (densite {d})')
        lo = [min(a, b) for a, b in zip(frm, to)]
        hi = [max(a, b) for a, b in zip(frm, to)]
        if name in {c.name for c in self.cubes} or name in self.bones:
            raise ValueError(f'nom en double : {name}')
        c = Cube(name, self.bones[bone], lo, hi, mat, rot, pivot, hide, opts)
        c.uuid = uid(self.name, 'cube', name)
        self.bones[bone].children.append(c)
        self.cubes.append(c)
        return c

    def sym(self, bone, name, frm, to, mat, rot=None, pivot=None, hide=None, **opts):
        """Paire gauche/droite (+x = _g, -x = _d) : rotations Y et Z inversees, UV du cote -x en miroir du cote +x (texture economisee, symetrie parfaite).
        bone : un os, ou 'os_plus_x|os_moins_x' si chaque cote a son os."""
        bg, bd = bone.split('|') if '|' in bone else (bone, bone)
        g = self.cube(bg, name + '_g', frm, to, mat, rot, pivot, hide, **opts)
        fm = (-to[0], frm[1], frm[2])
        tm = (-frm[0], to[1], to[2])
        rm = (rot[0], -rot[1], -rot[2]) if rot else None
        pm = (-pivot[0], pivot[1], pivot[2]) if pivot is not None else None
        hm = {MIRROR_FACE.get(f, f) for f in (hide or ())}
        d = self.cube(bd, name + '_d', fm, tm, mat, rm, pm, hm, **opts)
        d.twin = g
        return g, d

    def decal(self, cube_name, face, fn):
        c = next(c for c in self.cubes if c.name == cube_name)
        if c.twin is not None:
            raise ValueError(f'{cube_name} : decalque a poser sur le cube +x, le cube -x est en miroir')
        c.decals.append((face, fn))

    def animation(self, name, length, tracks, loop=True):
        """tracks : {os: {'rotation': [(t, x, y, z), ...], 'position': [...]}}, valeurs telles qu'affichees dans Blockbench."""
        for b in tracks:
            if b not in self.bones:
                raise ValueError(f'piste sur un os absent : {b}')
        self.anims.append({'name': name, 'length': float(length), 'loop': loop, 'tracks': tracks})

    # ---------------------------------------------------------------- faces cachees
    def _bone_cubes(self):
        out = {}
        for c in self.cubes:
            out.setdefault(c.bone.name, []).append(c)
        return out

    def cull(self):
        """Retire une face seulement si elle est entierement couverte par des cubes du meme os (qui bougent avec elle)."""
        groups = self._bone_cubes()
        d = self.density
        removed = 0
        for c in self.cubes:
            if c.twin is not None:
                continue
            others = [o for o in groups[c.bone.name] if o is not c]
            for f in FACES:
                if f in c.hide:
                    continue
                w, h = self._face_size(c, f)
                if w == 0 or h == 0:
                    c.hide.add(f)
                    continue
                ctx = FaceCtx(self, c, f, w, h)
                probe = ctx.P + ctx.N * (0.5 / d * .5)
                cov = np.zeros((h, w), bool)
                for o in others:
                    cov |= o.inside_world(probe)
                if cov.all():
                    c.hide.add(f)
                    removed += 1
        for c in self.cubes:
            if c.twin is not None:
                c.hide = {MIRROR_FACE.get(f, f) for f in c.twin.hide}
                removed += len(c.hide)
        self.culled = removed

    def _face_size(self, c, f):
        d = self.density
        sx, sy, sz = (c.to - c.frm) * d
        sx, sy, sz = int(round(sx)), int(round(sy)), int(round(sz))
        return {'north': (sx, sy), 'south': (sx, sy), 'east': (sz, sy), 'west': (sz, sy), 'up': (sx, sz), 'down': (sx, sz)}[f]

    # ---------------------------------------------------------------- UV
    def pack(self, min_size=128):
        """Patron par cube : ligne des cotes (est, nord, ouest, sud), dessus/dessous au-dessus. Rangement par os, puis hauteur."""
        clusters = []
        for c in self.cubes:
            if c.twin is not None:
                continue
            sides = [(f, *self._face_size(c, f)) for f in ('east', 'north', 'west', 'south') if f not in c.hide]
            caps = [(f, *self._face_size(c, f)) for f in ('up', 'down') if f not in c.hide]
            sw = sum(s[1] for s in sides)
            sh = max((s[2] for s in sides), default=0)
            cw = sum(s[1] for s in caps)
            ch = max((s[2] for s in caps), default=0)
            W, H = max(sw, cw), sh + ch
            if W and H:
                clusters.append((c, sides, caps, W, H, ch))
        clusters.sort(key=lambda t: (-t[4], -t[3]))
        area = sum((t[3] + 1) * (t[4] + 1) for t in clusters)
        size = min_size
        while True:
            if size * size >= area:
                pos = self._shelf(clusters, size)
                if pos is not None:
                    break
            size *= 2
        for (c, sides, caps, W, H, ch), (x, y) in zip(clusters, pos):
            cx = x
            for f, w, h in caps:
                c.faces[f] = (cx, y, cx + w, y + h)
                cx += w
            cx = x
            for f, w, h in sides:
                c.faces[f] = (cx, y + ch, cx + w, y + ch + h)
                cx += w
        for c in self.cubes:
            if c.twin is not None:
                for f, (u0, v0, u1, v1) in c.twin.faces.items():
                    c.faces[MIRROR_FACE.get(f, f)] = (u1, v0, u0, v1)
        self.res = size
        self.filled = area / (size * size)

    @staticmethod
    def _shelf(clusters, size):
        """Rangement en ligne d'horizon : chaque patron va au point le plus bas ou il tient (1 texel de marge)."""
        sky = [(1, 1, size - 2)]                # segments (x, y, largeur)
        pos = []
        for t in clusters:
            W, H = t[3] + 1, t[4] + 1
            best = None
            for i in range(len(sky)):
                x = sky[i][0]
                if x + W > size - 1 + 1:
                    continue
                y = 0
                rest = W
                j = i
                while rest > 0 and j < len(sky):
                    y = max(y, sky[j][1])
                    rest -= sky[j][2]
                    j += 1
                if rest > 0 or y + H > size:
                    continue
                if best is None or (y, x) < (best[0], best[1]):
                    best = (y, x, i)
            if best is None:
                return None
            y, x, i = best
            pos.append((x, y))
            # met a jour l'horizon
            new = []
            for (sx, sy, sw) in sky:
                ex = sx + sw
                if ex <= x or sx >= x + W:
                    new.append((sx, sy, sw))
                else:
                    if sx < x:
                        new.append((sx, sy, x - sx))
                    if ex > x + W:
                        new.append((x + W, sy, ex - (x + W)))
            new.append((x, y + H, W))
            new.sort()
            merged = []
            for s in new:
                if merged and merged[-1][1] == s[1] and merged[-1][0] + merged[-1][2] == s[0]:
                    merged[-1] = (merged[-1][0], s[1], merged[-1][2] + s[2])
                else:
                    merged.append(s)
            sky = merged
        return pos

    # ---------------------------------------------------------------- peinture
    def occlusion(self, ctx):
        """Ombre de contact peinte : part d'echantillons bloques autour du texel (tous les cubes, pose de repos)."""
        P, N = ctx.P, ctx.N
        tangent = np.array([0, 1.0, 0]) if abs(N[1]) < .9 else np.array([1.0, 0, 0])
        A = np.cross(N, tangent)
        A /= np.linalg.norm(A)
        B = np.cross(N, A)
        unit = 2 / self.density
        kernel = []
        for r in (0.5, 1.0):
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                kernel.append((N * r + (A * dx + B * dy) * r) * unit)
            kernel.append(N * r * unit * 1.2)
        out = np.zeros(P.shape[:2], dtype=np.float32)
        others = [o for o in self.cubes if o is not ctx.cube and not o.opts.get('no_shadow')]
        for k in kernel:
            Q = P + k
            hit = np.zeros(P.shape[:2], bool)
            for o in others:
                hit |= o.inside_world(Q)
            out += hit
        return out / len(kernel)

    def paint(self, painters, ramps):
        S = self.res
        img = np.zeros((S, S, 4), dtype=np.uint8)
        for c in self.cubes:
            if c.twin is not None:
                continue
            for f, (u0, v0, u1, v1) in c.faces.items():
                w, h = u1 - u0, v1 - v0
                ctx = FaceCtx(self, c, f, w, h)
                mat = c.mat if isinstance(c.mat, (tuple, list)) else (c.mat,)
                fn = painters[mat[0]]
                res = fn(ctx, *mat[1:], **{k: v for k, v in c.opts.items() if k not in ('no_shadow', 'no_ao')})
                ramp, tone = res[0], res[1]
                alpha = res[2] if len(res) > 2 else None
                if not c.opts.get('no_ao'):
                    tone = tone - self.occlusion(ctx) * 2.0
                R = ramps[ramp] if isinstance(ramp, str) else ramp
                idx = np.clip(np.rint(tone).astype(int), 0, len(R) - 1)
                face = np.zeros((h, w, 4), dtype=np.uint8)
                face[..., :3] = R[idx]
                face[..., 3] = 255 if alpha is None else alpha
                for (df, dfn) in c.decals:
                    if df == f or df == '*':
                        dfn(ctx, face)
                img[v0:v1, u0:u1] = face
        self.texture = img
        return img

    # ---------------------------------------------------------------- bbmodel natif 5.0
    def png_bytes(self):
        buf = io.BytesIO()
        Image.fromarray(self.texture, 'RGBA').save(buf, 'PNG', optimize=True)
        return buf.getvalue()

    def to_bbmodel(self):
        elements, groups = [], []

        def color_of(c):
            return int(hashlib.md5(str(c.mat).encode()).hexdigest()[:2], 16) % 8

        def el(c):
            faces = {}
            for f in ('north', 'east', 'south', 'west', 'up', 'down'):
                if f in c.faces:
                    faces[f] = {'uv': list(c.faces[f]), 'texture': 0}
                else:
                    faces[f] = {'uv': [0, 0, 0, 0], 'texture': None}
            e = {'name': c.name, 'box_uv': False, 'render_order': 'default', 'locked': False, 'export': True, 'scope': 0, 'allow_mirror_modeling': True,
                 'from': [round(v, 5) for v in c.frm.tolist()], 'to': [round(v, 5) for v in c.to.tolist()], 'autouv': 0, 'color': color_of(c),
                 'origin': [round(v, 5) for v in c.pivot], 'faces': faces, 'type': 'cube', 'uuid': c.uuid}
            if c.rotated:
                e['rotation'] = [round(v, 5) for v in c.rot]
            return e

        def node(n):
            if isinstance(n, Cube):
                elements.append(el(n))
                return n.uuid
            groups.append({'name': n.name, 'uuid': n.uuid, 'export': True, 'locked': False, 'scope': 0, 'selected': False, 'visibility': True,
                           'origin': list(n.pivot), 'rotation': list(n.rot), 'color': 0, 'children': [], 'reset': False, 'shade': True, 'mirror_uv': False,
                           'autouv': 0, 'isOpen': False, 'primary_selected': False})
            return {'uuid': n.uuid, 'isOpen': False, 'children': [node(ch) for ch in n.children]}

        outliner = [node(r) for r in self.roots]
        anims = []
        for a in self.anims:
            animators = {}
            for bone, chans in a['tracks'].items():
                keyframes = []
                for ch, keys in chans.items():
                    for i, (t, x, y, z) in enumerate(keys):
                        keyframes.append({'channel': ch, 'data_points': [{'x': f'{x:.3f}'.rstrip('0').rstrip('.') or '0', 'y': f'{y:.3f}'.rstrip('0').rstrip('.') or '0',
                                                                         'z': f'{z:.3f}'.rstrip('0').rstrip('.') or '0'}],
                                          'uuid': uid(self.name, a['name'], bone, ch, i), 'time': round(t, 4), 'color': -1, 'interpolation': 'catmullrom'})
                animators[self.bones[bone].uuid] = {'name': bone, 'type': 'bone', 'keyframes': keyframes}
            anims.append({'uuid': uid(self.name, 'anim', a['name']), 'name': a['name'], 'loop': 'loop' if a['loop'] else 'once', 'override': False, 'length': a['length'],
                          'snapping': 24, 'selected': False, 'group_name': '', 'scope': 0, 'anim_time_update': '', 'blend_weight': '', 'start_delay': '', 'loop_delay': '',
                          'animators': animators})
        S = self.res
        tex = {'name': f'{self.name}.png', 'path': '', 'folder': '', 'namespace': '', 'id': '0', 'group': '', 'scope': 0, 'width': S, 'height': S, 'uv_width': S, 'uv_height': S,
               'particle': False, 'use_as_default': False, 'layers_enabled': False, 'sync_to_project': '', 'file_format': 'png', 'render_mode': 'default', 'render_sides': 'auto',
               'wrap_mode': 'limited', 'pbr_channel': 'color', 'fps': 7, 'force_translucent': False, 'frame_time': 1, 'frame_order_type': 'loop', 'frame_order': '',
               'frame_interpolate': False, 'visible': True, 'internal': True, 'saved': False, 'uuid': uid(self.name, 'texture'),
               'source': 'data:image/png;base64,' + base64.b64encode(self.png_bytes()).decode()}
        return {'meta': {'format_version': '5.0', 'model_format': 'free', 'box_uv': False}, 'name': self.name, 'model_identifier': self.name, 'visible_box': [1, 1, 0],
                'variable_placeholders': '', 'multi_file_ruleset': '', 'variable_placeholder_buttons': [], 'timeline_setups': [], 'unhandled_root_fields': {},
                'resolution': {'width': S, 'height': S}, 'elements': elements, 'groups': groups, 'outliner': outliner, 'textures': [tex], 'animations': anims}

    # ---------------------------------------------------------------- controles
    def coplanar_overlaps(self):
        """Paires de faces paralleles, de meme sens, dans le meme plan et qui se recouvrent : c'est ce qui scintille en jeu.
        Traite les cubes non tournes (les plus frequents)."""
        out = []
        cs = [c for c in self.cubes if not c.rotated]
        axis = {'north': (2, 'frm'), 'south': (2, 'to'), 'east': (0, 'to'), 'west': (0, 'frm'), 'up': (1, 'to'), 'down': (1, 'frm')}
        for i, a in enumerate(cs):
            for b in cs[i + 1:]:
                for f, (ax, side) in axis.items():
                    if f not in a.faces or f not in b.faces:
                        continue
                    va = getattr(a, side)[ax]
                    vb = getattr(b, side)[ax]
                    if abs(va - vb) > 1e-6:
                        continue
                    o = [k for k in (0, 1, 2) if k != ax]
                    ov = all(min(a.to[k], b.to[k]) - max(a.frm[k], b.frm[k]) > 1e-6 for k in o)
                    if ov:
                        out.append((a.name, b.name, f))
        return out


def export_runtime(bb, author='Aeloria', description=''):
    """.bbmodel (natif 5.0 ou ancien 4.x) -> (modele lu par ImportedModel, PNG). Accepte aussi un fichier re-sauve par Blockbench.
    Conventions verifiees sur le code de Blockbench 5.2 et de ModelPart : en 5.0 une cle vaut mesh.rotation/position ;
    ModelPart.xRot = -x, yRot = -y, zRot = z ; posVec(x, y, z) = (x, -y, z) avec x modele = -x Blockbench."""
    legacy = tuple(int(v) for v in bb['meta'].get('format_version', '4.0').split('.')[:2]) < (5, 0)
    els = {e['uuid']: e for e in bb['elements']}
    grp = {g['uuid']: g for g in bb.get('groups', [])}
    used = set()
    names = {}

    def uniq(name):
        n = ''.join(ch if ch.isalnum() or ch in '_-.' else '_' for ch in name)
        base, i = n, 2
        while n in used:
            n, i = f'{base}_{i}', i + 1
        used.add(n)
        return n

    def walk(n):
        if isinstance(n, str):
            e = els[n]
            faces = {}
            for k, v in e['faces'].items():
                if v.get('texture') is None:
                    continue
                faces[k] = {'uv': v['uv'], 'texture': 0}
                if v.get('rotation'):
                    faces[k]['rotation'] = v['rotation']
            out = {'uuid': uniq(e['name']), 'from': e['from'], 'to': e['to'], 'origin': e.get('origin', [0, 0, 0]), 'rotation': e.get('rotation', [0, 0, 0]), 'faces': faces}
            if e.get('inflate'):
                out['inflate'] = e['inflate']
            return out
        g = {**grp.get(n['uuid'], {}), **{k: v for k, v in n.items() if k in ('name', 'origin', 'rotation')}} if isinstance(n, dict) else n
        name = uniq(g['name'])
        names[n['uuid']] = name
        return {'uuid': name, 'origin': g.get('origin', [0, 0, 0]), 'rotation': g.get('rotation', [0, 0, 0]), 'children': [walk(c) for c in n.get('children', [])]}

    # les os gardent leur nom exact : on les reserve avant les cubes
    def reserve(nodes):
        for n in nodes:
            if isinstance(n, dict):
                g = {**grp.get(n['uuid'], {}), **n}
                used.add(g['name'])
                reserve(n.get('children', []))

    reserve(bb['outliner'])
    reserved = set(used)
    used.clear()

    def walk_bone_first(n):
        if isinstance(n, dict):
            g = {**grp.get(n['uuid'], {}), **n}
            used.add(g['name'])
            names[n['uuid']] = g['name']
            return {'uuid': g['name'], 'origin': g.get('origin', [0, 0, 0]), 'rotation': g.get('rotation', [0, 0, 0]), 'children': [walk_bone_first(c) for c in n.get('children', [])]}
        return walk(n)

    used |= reserved
    parts = [walk_bone_first(n) for n in bb['outliner']]
    anims = []
    for a in bb.get('animations', []):
        lines = []
        for gid, an in a.get('animators', {}).items():
            bname = names.get(gid, an.get('name'))
            for channel, code in (('rotation', 'r'), ('position', 'p'), ('scale', 's')):
                keys = sorted((k for k in an.get('keyframes', []) if k['channel'] == channel), key=lambda k: k['time'])
                frames = []
                for k in keys:
                    p = k['data_points'][0]
                    x, y, z = float(p['x']), float(p['y']), float(p['z'])
                    if not legacy:
                        if code == 'r':
                            x, y = -x, -y
                        elif code == 'p':
                            x = -x
                    frames.append(','.join([f'{k["time"]:g}', f'{x:g}', f'{y:g}', f'{z:g}', 'c' if k.get('interpolation') == 'catmullrom' else 'l']))
                if frames:
                    lines.append(f'{bname}|{code}|' + ';'.join(frames))
        anims.append({'name': a['name'], 'length': a['length'], 'loop': a.get('loop') == 'loop', 'tracks': '\n'.join(lines)})
    png = base64.b64decode(bb['textures'][0]['source'].split(',', 1)[1])
    model = {'resolution': bb['resolution'], 'parts': parts, 'animations': anims, 'author': author}
    if description:
        model['description'] = description
    return model, png


def check_runtime(model):
    """Reproduit les conditions de chargement d'ImportedModel + KeyframeAnimation.bake (une piste sur un os absent leve une exception)."""
    errs = []
    names = []

    def walk(ns, bones):
        for n in ns:
            names.append(n['uuid'])
            if 'children' in n:
                bones.add(n['uuid'])
                walk(n['children'], bones)
            else:
                for k in ('from', 'to', 'origin'):
                    if k not in n:
                        errs.append(f'{n["uuid"]} : champ {k} absent')
                for f, v in n.get('faces', {}).items():
                    u0, v0, u1, v1 = v['uv']
                    W, H = model['resolution']['width'], model['resolution']['height']
                    if not (0 <= min(u0, u1) and max(u0, u1) <= W and 0 <= min(v0, v1) and max(v0, v1) <= H):
                        errs.append(f'{n["uuid"]}/{f} : UV hors texture')
                    if u0 == u1 or v0 == v1:
                        errs.append(f'{n["uuid"]}/{f} : UV vide')
    bones = set()
    walk(model['parts'], bones)
    dup = {n for n in names if names.count(n) > 1}
    if dup:
        errs.append(f'noms en double : {sorted(dup)[:6]}')
    for a in model['animations']:
        for line in a['tracks'].split('\n'):
            if not line:
                continue
            b, ch, keys = line.split('|')
            if b not in bones:
                errs.append(f'{a["name"]} : piste sur os absent {b}')
            ts = [float(k.split(',')[0]) for k in keys.split(';')]
            if ts != sorted(ts):
                errs.append(f'{a["name"]}/{b} : cles non triees')
            if ts and ts[-1] > a['length'] + 1e-6:
                errs.append(f'{a["name"]}/{b} : cle au-dela de la duree')
    return errs


def write_all(proj, out_dir, runtime_rel):
    """Ecrit <out_dir>/<id>.bbmodel et <out_dir>/<id>.png, plus le JSON runtime dans l'arborescence du mod."""
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    bb = proj.to_bbmodel()
    (out_dir / f'{proj.name}.bbmodel').write_text(json.dumps(bb, separators=(',', ':')), encoding='utf-8')
    (out_dir / f'{proj.name}.png').write_bytes(proj.png_bytes())
    model, png = export_runtime(bb, description=proj.description)
    errs = check_runtime(model)
    root = Path(runtime_rel)
    (root / f'bbmodels/{proj.kind}').mkdir(parents=True, exist_ok=True)
    (root / f'textures/imported/{proj.kind}').mkdir(parents=True, exist_ok=True)
    (root / f'bbmodels/{proj.kind}/{proj.name}.json').write_text(json.dumps(model, separators=(',', ':')), encoding='utf-8')
    (root / f'textures/imported/{proj.kind}/{proj.name}.png').write_bytes(png)
    return model, errs
