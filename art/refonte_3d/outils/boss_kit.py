#!/usr/bin/env python3
"""Pont entre le kit des boss (art/entity_kit.py : Modded Entity, box UV, classes Java) et les matieres de la refonte.
Les boss gardent leur format : le Java construit le modele par texOffs/addBox, chaque cube a donc son patron box UV complet, tailles entieres.
Une matiere P() peint chaque face avec les peintres d'outils/materiaux.py (rampes limitees, sans tramage), ombre de contact comprise, puis les decorations du kit passent par-dessus."""
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
for art in (HERE.parents[1], HERE.parents[0] / 'originaux/git_main/art'):      # dans le depot : art/ ; dans l'atelier : la copie des originaux
    sys.path.insert(0, str(art / 'boss_blockbench'))
    sys.path.insert(0, str(art))
sys.path.insert(0, str(HERE))
import entity_kit as ek  # noqa: E402

from aeloria3d import Cube as CubeA, FaceCtx, Project  # noqa: E402
from materiaux import PEINTRES, RAMPS  # noqa: E402
import kit_mob  # noqa: E402,F401  (enregistre frange et remiges)

_ACTIF = {'proj': None, 'par_id': {}}


class P(ek.Mat):
    """Matiere de la refonte : P('plaque', 'fer_noir', base=2.6, rivets=4). glow=True : la face passe aussi dans la couche emissive."""

    def __init__(self, peintre, ramp, glow=False, no_ao=False, glow_min=None, **opts):
        self.peintre, self.ramp_nom, self.opts, self.no_ao, self.glow_min = peintre, ramp, opts, no_ao, glow_min
        r = RAMPS[ramp]
        super().__init__([tuple(int(v) for v in c) for c in r], glow=glow)

    def paint(self, f):
        if f.w <= 0 or f.h <= 0:
            return
        proj = _ACTIF['proj']
        ca = _ACTIF['par_id'].get(id(f.cube)) or _cube_a(f.cube)
        ctx = FaceCtx(proj, ca, f.name, f.w, f.h)
        res = PEINTRES[self.peintre](ctx, self.ramp_nom, **self.opts)
        ramp, tone = res[0], res[1]
        alpha = res[2] if len(res) > 2 else None
        if not self.no_ao:
            tone = tone - proj.occlusion(ctx) * 2.0
        R = RAMPS[ramp] if isinstance(ramp, str) else ramp
        idx = np.clip(np.rint(tone).astype(int), 0, len(R) - 1)
        rgb = R[idx]
        A = np.full(idx.shape, 255, np.uint8) if alpha is None else alpha
        # meme face, repere different : le dessus box UV a x et z inverses, le dessous x seulement
        idx_f = idx
        if f.name == 'up':
            rgb, A, idx_f = rgb[::-1, ::-1], A[::-1, ::-1], idx[::-1, ::-1]
        elif f.name == 'down':
            rgb, A, idx_f = rgb[:, ::-1], A[:, ::-1], idx[:, ::-1]
        for y in range(f.h):
            for x in range(f.w):
                if A[y, x] == 0:
                    f.clear(x, y)
                else:
                    lum = self.glow or (self.glow_min is not None and idx_f[y, x] >= self.glow_min)
                    f.px(x, y, (int(rgb[y, x, 0]), int(rgb[y, x, 1]), int(rgb[y, x, 2])), glow=lum, a=255)
        for d in self.decos:
            d(f)


def _cube_a(c):
    to = [c.frm[i] + c.size[i] for i in range(3)]
    org = c.origin or c.bone.pivot
    ca = CubeA(c.name, None, c.frm, to, None, c.rot, org if c.rot else None, None, {})
    return ca


def prepare(m, seed=7, y_corps=(0, 60)):
    """A appeler avant ek.build_texture : prepare l'ombre de contact (tous les cubes pleins, dans la pose de repos)."""
    proj = Project(m.key, density=1, kind='boss', seed=seed)
    proj.meta['y_corps'] = y_corps
    par_id = {}
    for c in m.cubes():
        ca = _cube_a(c)
        # les plans fins n'assombrissent pas leurs voisins
        ca.opts['no_shadow'] = min(c.size) <= 1
        par_id[id(c)] = ca
        if not ca.opts['no_shadow']:
            proj.cubes.append(ca)
    _ACTIF['proj'], _ACTIF['par_id'] = proj, par_id


class B:
    """Raccourcis de construction : from/to au lieu de from/taille, paires droite-gauche en miroir sur la meme texture."""

    def __init__(self, m):
        self.m = m

    def c(self, bone, nom, a, b, mat, **kw):
        return self.m.cube(bone, nom, a, [b[i] - a[i] for i in range(3)], mat, **kw)

    def paire(self, os_d, os_g, nom, a, b, mat, rot=None, origin=None, **kw):
        """Cote +x (droite de la creature) puis son miroir, texture partagee."""
        share = kw.pop('share', nom)
        deco = kw.pop('deco', None)
        d = self.m.cube(os_d, f'{nom}_d', a, [b[i] - a[i] for i in range(3)], mat, share=share, deco=deco, rot=rot, origin=origin, **kw)
        am = (-b[0], a[1], a[2])
        rm = (rot[0], -rot[1], -rot[2]) if rot else None
        om = (-origin[0], origin[1], origin[2]) if origin else None
        g = self.m.cube(os_g, f'{nom}_g', am, [b[i] - a[i] for i in range(3)], mat, share=share, mirror=True, deco=deco, rot=rm, origin=om, **kw)
        return d, g


def coplanaires(m):
    """Faces de meme sens dans le meme plan qui se recouvrent, entre cubes non tournes du meme os : a corriger, ca scintille."""
    out = []
    for b in m.order:
        cs = [c for c in b.cubes if not c.rot]
        for i, a in enumerate(cs):
            for c in cs[i + 1:]:
                for ax in range(3):
                    for fin in (False, True):
                        va = a.frm[ax] + (a.size[ax] if fin else 0)
                        vc = c.frm[ax] + (c.size[ax] if fin else 0)
                        if abs(va - vc) > 1e-6:
                            continue
                        o = [k for k in range(3) if k != ax]
                        if all(min(a.frm[k] + a.size[k], c.frm[k] + c.size[k]) - max(a.frm[k], c.frm[k]) > 1e-6 for k in o):
                            out.append((a.name, c.name, 'xyz'[ax] + ('+' if fin else '-')))
    return out
