# -*- coding: utf-8 -*-
"""Base commune des Maitres de classe du hub : silhouette humanoide de 32 px (2 blocs) a laquelle chaque classe ajoute sa
panoplie, son arme et ses effets lumineux. Os communs : racine > corps (torse, cape) > tete, bras_droit, bras_gauche ; jambe_droite,
jambe_gauche. Les animations communes sont 'idle' (respiration, balancement) et 'present' (mise en scene)."""
import entity_kit as ek
from entity_kit import pal, Mat, stamp, rivets, scratches, border, blotches

P_PEAU = pal('#a8704c', '#bd8660', '#d09c76', '#e2b490', '#f0c8a8')
P_ACIER = pal('#262c34', '#3a424c', '#566270', '#7a8898', '#a8b6c6', '#d6e0ec')
P_OR = pal('#6a4a10', '#946a18', '#c49628', '#e8be44', '#fbe07a')
P_CUIR = pal('#2e1c10', '#442a18', '#5e3c22', '#7a5030', '#946440')


class Decal:
    """Matiere 'decalque' : transparent partout sauf les motifs (plans de visage, runes)."""

    def __init__(self, *decos):
        self.decos = decos

    def paint(self, f):
        for y in range(f.h):
            for x in range(f.w):
                f.clear(x, y)
        for d in self.decos:
            d(f)


def face(eye, glow=True, brow=None, mouth=(120, 60, 50)):
    """Visage 8x8 : yeux lumineux, sourcils et bouche ; le reste garde la peau."""
    rows = ['........', '.bb..bb.', '.GG..GG.', '.GG..GG.', '........', '........', '...mm...', '........']
    return stamp(rows, {'G': eye, 'b': brow or (40, 28, 20), 'm': mouth}, glow='G' if glow else '')


def skin(level=0.6):
    return Mat(P_PEAU, level=level, noise=0.1, scale=2, grad=0.1)


def cloth(palette, level=0.5, deco=()):
    return Mat(palette, level=level, noise=0.22, scale=1.5, grad=0.18, decos=list(deco))


def metal(palette=P_ACIER, level=0.55, rivet=True):
    decos = [rivets(palette[-1], palette[0], inset=1, every=4)] if rivet else []
    decos.append(scratches(2, palette[1]))
    return Mat(palette, level=level, noise=0.2, scale=2, grad=0.25, decos=decos)


def trim(palette=P_OR):
    return Mat(palette, level=0.6, noise=0.15, scale=1, grad=0.3)


def leather(level=0.5):
    return Mat(P_CUIR, level=level, noise=0.25, scale=1.5, grad=0.2)


def humanoid(m, *, torso, arms, legs, boots, face_deco, head=None, head_deco=None, hands=None, arm_width=4):
    """Construit l'humanoide de base et renvoie un dictionnaire d'os. Le visage regarde vers -Z."""
    m.bone('racine', (0, 0, 0))
    co = m.bone('corps', (0, 12, 0), 'racine')
    m.cube(co, 'torse', (-4, 12, -2), (8, 12, 4), torso)
    t = m.bone('tete', (0, 24, 0), co)
    m.cube(t, 'crane', (-4, 24, -4), (8, 8, 8), head or skin(), deco={'north': [face_deco]})
    # convention du kit : la creature regarde vers -Z, son cote DROIT est en +X
    bd = m.bone('bras_droit', (4 + arm_width / 2 - 1, 22, 0), co)
    bg = m.bone('bras_gauche', (-4 - arm_width / 2 + 1, 22, 0), co)
    m.cube(bd, 'bras_droit_cube', (4, 12, -2), (arm_width, 12, 4), arms, share='bras', mirror=True)
    m.cube(bg, 'bras_gauche_cube', (-4 - arm_width, 12, -2), (arm_width, 12, 4), arms, share='bras', mirror=False)
    m.cube(bd, 'main_droite', (4, 11, -1.5), (arm_width, 2, 3), hands or skin(), inflate=-0.05)
    m.cube(bg, 'main_gauche', (-4 - arm_width, 11, -1.5), (arm_width, 2, 3), hands or skin(), inflate=-0.05)
    jd = m.bone('jambe_droite', (2, 12, 0), 'racine')
    jg = m.bone('jambe_gauche', (-2, 12, 0), 'racine')
    m.cube(jd, 'jambe_droite_cube', (0, 3, -2), (4, 9, 4), legs, share='jambe', mirror=True)
    m.cube(jg, 'jambe_gauche_cube', (-4, 3, -2), (4, 9, 4), legs, share='jambe')
    m.cube(jd, 'botte_droite', (-0.2, 0, -2.6), (4, 4, 5), boots, share='botte', mirror=True)
    m.cube(jg, 'botte_gauche', (-3.8, 0, -2.6), (4, 4, 5), boots, share='botte')
    return {'racine': m.bones['racine'], 'corps': co, 'tete': t, 'bras_droit': bd, 'bras_gauche': bg, 'jambe_droite': jd, 'jambe_gauche': jg}


COLORS = {'racine': 0, 'corps': 1, 'tete': 2, 'bras_droit': 3, 'bras_gauche': 3, 'jambe_droite': 4, 'jambe_gauche': 4,
          'arme': 5, 'cape': 6, 'effet': 7}


def base_idle(m, extra=None, length=3.6):
    """Respiration (torse), tete qui balaie lentement, bras qui se balancent : commun a tous les maitres."""
    P = ek.key_poses
    a = m.anim('idle', length, 'loop')
    q = length / 4.0
    frames = [(0, {}),
              (q, {'corps': (-0.8, 0, 0.6), 'tete': (0.8, 8, 0), 'bras_droit': (2, 0, 1.5), 'bras_gauche': (-2, 0, -1.5)}),
              (2 * q, {'corps': (0, 0, 0), 'tete': (0, 0, 0)}),
              (3 * q, {'corps': (-0.8, 0, -0.6), 'tete': (0.8, -8, 0), 'bras_droit': (-2, 0, 1.5), 'bras_gauche': (2, 0, -1.5)}),
              (length, {})]
    P(a, frames)
    a.scale('corps', [(0, 1, 1, 1), (q, 1.0, 1.012, 1.0), (2 * q, 1, 1, 1), (3 * q, 1.0, 1.012, 1.0), (length, 1, 1, 1)])
    if extra:
        extra(a, length)
    return a
