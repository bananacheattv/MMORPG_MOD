# -*- coding: utf-8 -*-
"""Geometrie des modeles 3D de la peche : boites Minecraft (elements JSON), export et apercu logiciel.

Les modeles sont des listes de boites {from, to, rotation?, faces{uv, texture}} comme dans un modele d'objet vanilla.
`render_model` dessine un apercu (projection orthographique, textures echantillonnees, ombrage par face) pour verifier un modele
sans lancer le jeu.
"""
import math

import numpy as np
from PIL import Image

FACES = ('north', 'south', 'east', 'west', 'up', 'down')
SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6}


def box(x0, y0, z0, x1, y1, z1, uv, rot=None, tex='#t', px=16):
    """Une boite. uv : dict face -> [u1,v1,u2,v2] (ou une fonction (face, f, t) -> uv), en pixels d'une texture de px x px.
    rot : (axe, angle, origine)."""
    f = [min(x0, x1), min(y0, y1), min(z0, z1)]
    t = [max(x0, x1), max(y0, y1), max(z0, z1)]
    faces = {}
    for face in FACES:
        u = uv(face, f, t) if callable(uv) else uv.get(face)
        if u is None:
            continue
        k = 16.0 / px
        faces[face] = {'uv': [round(float(v) * k, 4) for v in u], 'texture': tex}
    el = {'from': [round(v, 4) for v in f], 'to': [round(v, 4) for v in t], 'faces': faces}
    if rot:
        axis, angle, origin = rot
        el['rotation'] = {'origin': list(origin), 'axis': axis, 'angle': angle}
    return el


def check_bounds(elements, lo=-16.0, hi=32.0):
    """Renvoie la liste des coordonnees hors limites [-16, 32] (le moteur rejette le modele)."""
    bad = []
    for i, e in enumerate(elements):
        for k in ('from', 'to'):
            for v in e[k]:
                if v < lo or v > hi:
                    bad.append((i, k, v))
    return bad


# ------------------------------------------------------------------ apercu logiciel

def _rot_matrix(axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    if axis == 'x':
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == 'y':
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def _face_frame(face, f, t):
    """(coin haut-gauche, vecteur u, vecteur v) d'une face, selon l'orientation des textures vanilla."""
    fx, fy, fz = f
    tx, ty, tz = t
    if face == 'north':
        return (tx, ty, fz), (-(tx - fx), 0, 0), (0, -(ty - fy), 0)
    if face == 'south':
        return (fx, ty, tz), ((tx - fx), 0, 0), (0, -(ty - fy), 0)
    if face == 'east':
        return (tx, ty, tz), (0, 0, -(tz - fz)), (0, -(ty - fy), 0)
    if face == 'west':
        return (fx, ty, fz), (0, 0, (tz - fz)), (0, -(ty - fy), 0)
    if face == 'up':
        return (fx, ty, fz), ((tx - fx), 0, 0), (0, 0, (tz - fz))
    return (fx, fy, tz), ((tx - fx), 0, 0), (0, 0, -(tz - fz))


NORMALS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}
LIGHT = np.array([0.35, 0.8, 0.55])
LIGHT = LIGHT / np.linalg.norm(LIGHT)


def render_model(model, textures, size=256, gui=(20, -35, 0), scale=0.9, bg=(52, 56, 66, 255)):
    """Dessine le modele. textures : dict nom ('t') -> Image RGBA. gui : rotation (x, y, z) comme l'affichage « gui »."""
    rx, ry, rz = gui
    R = _rot_matrix('z', rz) @ _rot_matrix('y', ry) @ _rot_matrix('x', rx)
    color = np.zeros((size, size, 4), dtype=np.float32)
    color[:] = bg
    zbuf = np.full((size, size), -1e9, dtype=np.float32)
    ppu = size / 16.0 * scale
    texcache = {k: np.asarray(v.convert('RGBA')) for k, v in textures.items()}
    for e in model['elements']:
        f, t = e['from'], e['to']
        rot = e.get('rotation')
        M = np.eye(3)
        o = np.zeros(3)
        if rot:
            M = _rot_matrix(rot['axis'], rot['angle'])
            o = np.array(rot['origin'], float)
        for face, fd in e['faces'].items():
            n = R @ (M @ np.array(NORMALS[face], float))
            if n[2] <= 1e-6:
                continue
            corner, du, dv = _face_frame(face, f, t)
            c0 = R @ (M @ (np.array(corner, float) - o) + o - 8.0)
            u_vec = R @ (M @ np.array(du, float))
            v_vec = R @ (M @ np.array(dv, float))
            s0 = np.array([size / 2 + c0[0] * ppu, size / 2 - c0[1] * ppu])
            su = np.array([u_vec[0] * ppu, -u_vec[1] * ppu])
            sv = np.array([v_vec[0] * ppu, -v_vec[1] * ppu])
            xs = [s0[0], s0[0] + su[0], s0[0] + sv[0], s0[0] + su[0] + sv[0]]
            ys = [s0[1], s0[1] + su[1], s0[1] + sv[1], s0[1] + su[1] + sv[1]]
            x0, x1 = max(0, int(math.floor(min(xs)))), min(size - 1, int(math.ceil(max(xs))))
            y0, y1 = max(0, int(math.floor(min(ys)))), min(size - 1, int(math.ceil(max(ys))))
            det = su[0] * sv[1] - su[1] * sv[0]
            if x1 < x0 or y1 < y0 or abs(det) < 1e-9:
                continue
            gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            dx, dy = gx - s0[0], gy - s0[1]
            a = (dx * sv[1] - dy * sv[0]) / det
            b = (su[0] * dy - su[1] * dx) / det
            inside = (a >= 0) & (a <= 1) & (b >= 0) & (b <= 1)
            if not inside.any():
                continue
            u1, v1, u2, v2 = fd['uv']
            arr_tex = texcache[fd['texture'].lstrip('#')]
            th, tw = arr_tex.shape[:2]
            tu = (u1 + (u2 - u1) * a) / 16.0 * tw
            tv = (v1 + (v2 - v1) * b) / 16.0 * th
            arr = arr_tex[np.clip(tv.astype(int), 0, th - 1), np.clip(tu.astype(int), 0, tw - 1)].astype(np.float32)
            depth = c0[2] + u_vec[2] * a + v_vec[2] * b
            shade = 0.5 + 0.5 * max(0.0, float(np.dot(n, LIGHT)))
            zsub = zbuf[y0:y1 + 1, x0:x1 + 1]
            mask = inside & (arr[..., 3] > 8) & (depth > zsub)
            sub = color[y0:y1 + 1, x0:x1 + 1]
            sub[mask, :3] = arr[mask, :3] * shade
            sub[mask, 3] = 255
            zsub[mask] = depth[mask]
    return Image.fromarray(np.clip(color, 0, 255).astype(np.uint8), 'RGBA')
