#!/usr/bin/env python3
"""Rendu logiciel de controle (z-buffer, plus proche voisin) d'un .bbmodel reel : memes cubes, memes UV, meme PNG.
Ce n'est PAS une illustration : tout vient du fichier. Sert aux planches, aux poses d'animation et au controle du cavalier.
"""
import base64
import io
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image


def load_texture(tex, base=None):
    src = tex.get('source') or ''
    if src.startswith('data:'):
        raw = base64.b64decode(src.split(',', 1)[1])
        return np.asarray(Image.open(io.BytesIO(raw)).convert('RGBA'), dtype=np.uint8)
    p = tex.get('path') or ''
    for c in ([Path(p)] if p else []) + ([Path(base) / tex.get('name', '')] if base else []):
        if c.exists():
            return np.asarray(Image.open(c).convert('RGBA'), dtype=np.uint8)
    return np.full((16, 16, 4), 200, dtype=np.uint8)


def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def euler(r):
    # ordre Blockbench : Z * Y * X applique a v (rotation X d'abord)
    return rz(math.radians(r[2])) @ ry(math.radians(r[1])) @ rx(math.radians(r[0]))


def m4(R=None, t=None):
    M = np.eye(4)
    if R is not None:
        M[:3, :3] = R
    if t is not None:
        M[:3, 3] = t
    return M


def catmull(p0, p1, p2, p3, t):
    t2, t3 = t * t, t * t * t
    return .5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3)


FACE_CORNERS = {
    # coins (haut-gauche, haut-droit, bas-droit, bas-gauche) vus de l'exterieur, conformes a Blockbench
    'north': lambda x0, y0, z0, x1, y1, z1: [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
    'south': lambda x0, y0, z0, x1, y1, z1: [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
    'east': lambda x0, y0, z0, x1, y1, z1: [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
    'west': lambda x0, y0, z0, x1, y1, z1: [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
    'up': lambda x0, y0, z0, x1, y1, z1: [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
    'down': lambda x0, y0, z0, x1, y1, z1: [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
}


class Model:
    def __init__(self, path):
        self.path = Path(path)
        self.d = json.loads(self.path.read_text(encoding='utf-8'))
        self.fmt = self.d['meta'].get('model_format')
        self.res = self.d.get('resolution', {'width': 16, 'height': 16})
        self.tex = [load_texture(t, self.path.parent) for t in self.d.get('textures', [])]
        self.el = {e['uuid']: e for e in self.d['elements']}
        self.anim = {a['name']: a for a in self.d.get('animations', [])}
        legacy = tuple(int(v) for v in str(self.d['meta'].get('format_version', '4.0')).split('.')[:2]) < (5, 0)
        self.rot_sign = np.array([-1, -1, 1]) if legacy else np.array([1, 1, 1])
        self.pos_sign = np.array([-1, 1, 1]) if legacy else np.array([1, 1, 1])
        # outliner 5.0 : les donnees d'os sont dans 'groups'
        self.groups = {g['uuid']: g for g in self.d.get('groups', [])}

    def channel(self, keys, t, default):
        keys = sorted(keys, key=lambda k: k['time'])
        vals = [np.array([float(k['data_points'][0][c]) for c in 'xyz']) for k in keys]
        if t <= keys[0]['time']:
            return vals[0]
        if t >= keys[-1]['time']:
            return vals[-1]
        for i in range(len(keys) - 1):
            a, b = keys[i], keys[i + 1]
            if a['time'] <= t <= b['time']:
                u = (t - a['time']) / max(1e-6, b['time'] - a['time'])
                if a.get('interpolation') == 'catmullrom' or b.get('interpolation') == 'catmullrom':
                    p0 = vals[max(0, i - 1)]
                    p3 = vals[min(len(vals) - 1, i + 2)]
                    return catmull(p0, vals[i], vals[i + 1], p3, u)
                return vals[i] * (1 - u) + vals[i + 1] * u
        return default

    def pose_of(self, bone_uuid, anim, t):
        rot, pos = np.zeros(3), np.zeros(3)
        if anim is None:
            return rot, pos
        an = anim.get('animators', {}).get(bone_uuid)
        if not an:
            return rot, pos
        by = {}
        for k in an.get('keyframes', []):
            by.setdefault(k['channel'], []).append(k)
        if 'rotation' in by:
            rot = self.channel(by['rotation'], t, rot)
        if 'position' in by:
            pos = self.channel(by['position'], t, pos)
        return rot, pos

    def scale_of(self, bone_uuid, anim, t):
        if anim is None:
            return None
        an = anim.get('animators', {}).get(bone_uuid)
        ks = [k for k in an.get('keyframes', []) if k['channel'] == 'scale'] if an else []
        return self.channel(ks, t, np.ones(3)) if ks else None

    def quads(self, anim_name=None, t=0.0, hidden=()):
        """Liste (corners[4,3], uv[4,2], tex_index, face) dans le repere modele."""
        anim = self.anim.get(anim_name) if anim_name else None
        out = []

        def cube(e, M):
            if e.get('type') == 'mesh' and e.get('visibility', True):
                # maillage Blockbench (planches AVANT seulement) : sommets relatifs a l'origine de l'element
                o = np.array(e.get('origin', [0, 0, 0]), float)
                CM = M @ m4(t=o) @ m4(euler(e.get('rotation', [0, 0, 0])))
                V = e.get('vertices', {})
                for f in e.get('faces', {}).values():
                    ids = f.get('vertices') or []
                    tid = f.get('texture')
                    if tid is None or len(ids) < 3:
                        continue
                    ids = list(ids)
                    if len(ids) == 4:
                        # ordre en tour de face : Blockbench range les quatre sommets sans garantie
                        P = np.array([V[i] for i in ids], float)
                        c0 = P.mean(0)
                        n = np.cross(P[1] - P[0], P[2] - P[0])
                        if np.linalg.norm(n) < 1e-9:
                            n = np.cross(P[2] - P[0], P[3] - P[0])
                        a0 = P[0] - c0
                        b0 = np.cross(n, a0)
                        ang = [math.atan2(np.dot(q - c0, b0), np.dot(q - c0, a0)) for q in P]
                        ids = [ids[k] for k in np.argsort(ang)]
                    else:
                        ids = ids + [ids[-1]]
                    c = np.c_[np.array([V[i] for i in ids], float), np.ones(4)]
                    c = (CM @ c.T).T[:, :3]
                    uv = np.array([f['uv'][i] for i in ids], float)
                    out.append((c, uv, int(tid) if not isinstance(tid, str) else 0, 'mesh'))
                return
            if not e.get('visibility', True) or e.get('type', 'cube') != 'cube':
                return
            g = float(e.get('inflate', 0) or 0)
            a = np.array(e['from'], float) - g
            b = np.array(e['to'], float) + g
            o = np.array(e.get('origin', [0, 0, 0]), float)
            R = euler(e.get('rotation', [0, 0, 0]))
            CM = M @ m4(t=o) @ m4(R) @ m4(t=-o)
            for name, f in e.get('faces', {}).items():
                tid = f.get('texture')
                if tid is None or name not in FACE_CORNERS:
                    continue
                u0, v0, u1, v1 = f['uv']
                UV = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
                rot = int(f.get('rotation', 0) or 0) // 90 % 4
                if rot:
                    UV = UV[-rot:] + UV[:-rot]
                c = np.c_[np.array(FACE_CORNERS[name](*a, *b), float), np.ones(4)]
                c = (CM @ c.T).T[:, :3]
                out.append((c, np.array(UV, float), int(tid) if not isinstance(tid, str) else 0, name))

        def walk(node, M, inherited_hidden=False):
            if isinstance(node, str):
                if node in self.el and not inherited_hidden:
                    cube(self.el[node], M)
                return
            g = {**self.groups.get(node.get('uuid'), {}), **node}
            hide = inherited_hidden or g.get('name') in hidden or g.get('visibility') is False
            o = np.array(g.get('origin', [0, 0, 0]), float)
            r0 = np.array(g.get('rotation', [0, 0, 0]), float)
            ar, ap = self.pose_of(node.get('uuid'), anim, t)
            # Blockbench 5 : une cle 5.0 s'ajoute telle quelle a mesh.rotation/position ; un fichier < 5.0 est inverse en X (et Y en rotation) au chargement
            R = euler(r0 + ar * self.rot_sign)
            sc = self.scale_of(node.get('uuid'), anim, t)
            N = M @ m4(t=ap * self.pos_sign) @ m4(t=o) @ m4(R) @ (np.diag([*sc, 1.0]) if sc is not None else np.eye(4)) @ m4(t=-o)
            for c in node.get('children', []):
                walk(c, N, hide)

        for n in self.d.get('outliner', []):
            walk(n, np.eye(4))
        return out


def camera(yaw, pitch):
    """Matrice monde -> camera : yaw autour de Y puis pitch autour de X."""
    return rx(math.radians(pitch)) @ ry(math.radians(yaw))


def render(quads, textures, res, yaw=-35, pitch=25, size=(900, 700), margin=.08, bg=(232, 232, 236, 255), scale=None, center=None, ssaa=1, extra=None):
    """quads : sortie de Model.quads ; extra : liste (corners, rgb) non texturee (mannequin de controle)."""
    W, H = size[0] * ssaa, size[1] * ssaa
    Cam = camera(yaw, pitch)
    allc = [q[0] for q in quads] + ([q[0] for q in extra] if extra else [])
    pts = np.concatenate(allc, 0)
    P = (Cam @ pts.T).T
    mn, mx = P.min(0), P.max(0)
    if scale is None:
        scale = min(W * (1 - 2 * margin) / max(1e-6, mx[0] - mn[0]), H * (1 - 2 * margin) / max(1e-6, mx[1] - mn[1]))
    else:
        scale *= ssaa
    if center is None:
        cx, cy = (mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2
    else:
        cc = Cam @ np.array(center, float)
        cx, cy = cc[0], cc[1]
    img = np.zeros((H, W, 4), dtype=np.float32)
    img[..., :] = np.array(bg, dtype=np.float32)
    zb = np.full((H, W), -1e9, dtype=np.float32)

    L0 = np.array([.2, 1.0, -.7]) / np.linalg.norm([.2, 1.0, -.7])
    L1 = np.array([-.2, 1.0, .7]) / np.linalg.norm([-.2, 1.0, .7])

    def face_light(c):
        n = np.cross(c[3] - c[0], c[1] - c[0])
        ln = np.linalg.norm(n)
        if ln < 1e-9:
            return None
        n = n / ln
        # eclairage d'entite Minecraft (Lighting.java) : ambiante 0,4, diffuse 0,6, deux lumieres fixes ; modele oriente vers le nord
        return min(1.0, .4 + .6 * (max(0, n @ L0) + max(0, n @ L1)))

    def raster(corners, uvs, tex, flat=None):
        l = face_light(corners)
        if l is None:
            return
        Pc = (Cam @ corners.T).T
        sx = (Pc[:, 0] - cx) * scale + W / 2
        sy = H / 2 - (Pc[:, 1] - cy) * scale
        sz = Pc[:, 2]
        for tri in ((0, 1, 2), (0, 2, 3)):
            ax, ay, az = sx[tri[0]], sy[tri[0]], sz[tri[0]]
            bx, by, bz = sx[tri[1]], sy[tri[1]], sz[tri[1]]
            kx, ky, kz = sx[tri[2]], sy[tri[2]], sz[tri[2]]
            area = (bx - ax) * (ky - ay) - (by - ay) * (kx - ax)
            if abs(area) < 1e-6:
                continue
            xmin = max(int(math.floor(min(ax, bx, kx))), 0)
            xmax = min(int(math.ceil(max(ax, bx, kx))), W - 1)
            ymin = max(int(math.floor(min(ay, by, ky))), 0)
            ymax = min(int(math.ceil(max(ay, by, ky))), H - 1)
            if xmax < xmin or ymax < ymin:
                continue
            xs, ys = np.meshgrid(np.arange(xmin, xmax + 1) + .5, np.arange(ymin, ymax + 1) + .5)
            w0 = ((bx - xs) * (ky - ys) - (by - ys) * (kx - xs)) / area
            w1 = ((kx - xs) * (ay - ys) - (ky - ys) * (ax - xs)) / area
            w2 = 1 - w0 - w1
            mask = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
            if not mask.any():
                continue
            z = w0 * az + w1 * bz + w2 * kz
            yy, xx = np.nonzero(mask)
            ry0, rx0 = yy + ymin, xx + xmin
            zz = z[mask]
            front = zz > zb[ry0, rx0] - 1e-5
            if not front.any():
                continue
            ry0, rx0, zz = ry0[front], rx0[front], zz[front]
            w0m, w1m, w2m = w0[mask][front], w1[mask][front], w2[mask][front]
            if flat is None:
                u = w0m * uvs[tri[0], 0] + w1m * uvs[tri[1], 0] + w2m * uvs[tri[2], 0]
                v = w0m * uvs[tri[0], 1] + w1m * uvs[tri[1], 1] + w2m * uvs[tri[2], 1]
                th, tw = tex.shape[:2]
                tu = np.clip((u / res['width'] * tw).astype(int), 0, tw - 1)
                tv = np.clip((v / res['height'] * th).astype(int), 0, th - 1)
                col = tex[tv, tu].astype(np.float32)
            else:
                col = np.tile(np.array(list(flat) + [255], dtype=np.float32), (len(zz), 1))
            keep = col[:, 3] > 8
            if not keep.any():
                continue
            ry1, rx1, z1 = ry0[keep], rx0[keep], zz[keep]
            c1 = col[keep]
            rgb = c1[:, :3] * l
            al = (c1[:, 3] / 255.)[:, None]
            img[ry1, rx1, :3] = rgb * al + img[ry1, rx1, :3] * (1 - al)
            img[ry1, rx1, 3] = 255
            opaque = c1[:, 3] > 200
            zb[ry1[opaque], rx1[opaque]] = z1[opaque]

    items = [(q[0], q[1], textures[q[2]] if q[2] < len(textures) else None, None) for q in quads]
    if extra:
        items += [(e[0], None, None, e[1]) for e in extra]
    items.sort(key=lambda it: (Cam @ it[0].mean(0))[2])          # du plus loin au plus proche (translucides)
    for corners, uvs, tex, flat in items:
        if tex is None and flat is None:
            continue
        raster(corners, uvs, tex, flat)
    im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')
    if ssaa > 1:
        im = im.resize(size, Image.LANCZOS)
    return im


def mannequin(seat_x, seat_y, seat_z, scale=.5):
    """Joueur assis (cuisses horizontales vers l'avant, -z dans le repere du modele), en unites modele.
    Une monture est rendue x2 : le joueur (16 px = 1 bloc) mesure donc 0,5 unite modele par pixel. seat_y = dessous des cuisses."""
    s = scale
    parts = []

    def box(a, b, col):
        a = np.array(a, float) * s + np.array([seat_x, seat_y, seat_z])
        b = np.array(b, float) * s + np.array([seat_x, seat_y, seat_z])
        for name, fn in FACE_CORNERS.items():
            parts.append((np.array(fn(*a, *b), float), col))

    skin = (222, 176, 140)
    shirt = (60, 120, 170)
    pants = (52, 58, 110)
    hair = (70, 44, 26)
    box((-4, 2, -2), (4, 14, 2), shirt)
    box((-4, 14, -4), (4, 22, 4), skin)
    box((-4.3, 19, -4.3), (4.3, 22.5, 4.3), hair)
    box((-8, 3, -8), (-4, 14, -2), shirt)
    box((4, 3, -8), (8, 14, -2), shirt)
    box((-4, 0, -12), (0, 4, 0), pants)
    box((0, 0, -12), (4, 4, 0), pants)
    return parts


if __name__ == '__main__':
    src, dst = sys.argv[1], sys.argv[2]
    m = Model(src)
    render(m.quads(), m.tex, m.res).save(dst)
