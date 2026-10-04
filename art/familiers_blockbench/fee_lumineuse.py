# -*- coding: utf-8 -*-
"""Fee lumineuse : petite fee chibi (environ 1 bloc en vol), longs cheveux blonds ornes de fleurs blanches,
oreilles pointues, yeux verts, robe verte a liseres dores et ceinture brune, jupon creme, bottes brunes,
quatre ailes dorees translucides (bords et nervures lumineux) et cristal lumineux tenu dans la main gauche."""
import math

import entity_kit as ek
from entity_kit import pal, Mat, stamp

KEY = 'fee_lumineuse'
TITLE = 'Fée lumineuse'
ENTITY_CLASS = 'FeeLumineuseEntity'
VISIBLE_BOX = (0.7, 1.4, 0)
VIEW_SCALE = 20.0
SHEET_SCALE = 12.0
GIF_SCALE = 10.0

P_PEAU = pal('#d39a7c', '#e4b293', '#efc6a8', '#f6d8bf', '#fce7d6')
P_CHEVEUX = pal('#c4892a', '#dba336', '#eabb46', '#f4cf5e', '#f9e083', '#fdf0b6')
P_ROBE = pal('#2c4719', '#3b5e22', '#4b752a', '#5e8d33', '#74a33f')
P_CUIR = pal('#3f2614', '#55341b', '#6c4423', '#85552c', '#9c6a38')
P_CREME = pal('#cfc2a0', '#e2d7b8', '#efe6cc', '#f8f2e0')
P_CRISTAL = pal('#d98a00', '#f5ac10', '#ffc928', '#ffe066', '#fff3b0')
OR = (214, 168, 58)
OR_CLAIR = (246, 210, 104)
BLANC = (252, 250, 240)
C = {'H': P_CHEVEUX[3], 'h': P_CHEVEUX[2], 'K': (44, 30, 20), 'G': (86, 146, 52), 'g': (58, 104, 36), 'W': BLANC,
     'p': (246, 168, 160), 'm': (214, 120, 120), 'O': OR, 'o': OR_CLAIR, 'c': P_CREME[2], 'Y': (250, 214, 70), 'v': (88, 138, 48)}

PEAU = Mat(P_PEAU, level=0.6, noise=0.12, scale=2, grad=0.12)
ROBE = Mat(P_ROBE, level=0.5, noise=0.25, scale=1.5, grad=0.18)
CUIR = Mat(P_CUIR, level=0.5, noise=0.25, scale=1.5, grad=0.2)
CREME = Mat(P_CREME, level=0.6, noise=0.2, scale=1.5, grad=0.15)


def strands(f):
    """Meches : stries verticales plus claires et plus sombres."""
    for x in range(f.w):
        if f.side and x % 3 == 1:
            for y in range(f.h):
                if f.rng.random() < 0.5:
                    f.px(x, y, P_CHEVEUX[2])
        elif f.side and x % 3 == 0:
            for y in range(0, f.h, 2):
                f.px(x, y, P_CHEVEUX[4])


CHEVEUX = Mat(P_CHEVEUX, level=0.62, noise=0.15, scale=(1, 3), grad=0.2, up=0.15, decos=[strands])


def jagged_bottom(f):
    """Pointes irregulieres en bas des meches."""
    if f.side:
        for x in range(f.w):
            if (x * 7 + 3) % 3 == 0:
                f.clear(x, f.h - 1)


def gold_trim(sides='b'):
    def d(f):
        if not f.side:
            return
        for x in range(f.w):
            if 'b' in sides:
                f.px(x, f.h - 1, OR if x % 2 else OR_CLAIR)
            if 't' in sides:
                f.px(x, 0, OR if x % 2 else OR_CLAIR)
    return d


def dress_front(f):
    """Corsage : col creme en V, deux bandes dorees verticales, revers des cotes."""
    if f.name == 'north':
        cx = f.w // 2
        for x in range(f.w):
            if abs(x - (f.w - 1) / 2) < 1.2:
                f.px(x, 0, P_CREME[3])
        f.px(cx, 1, P_CREME[2])
        for y in range(f.h):
            f.px(0, y, OR)
            f.px(f.w - 1, y, OR)
    elif f.side:
        f.px(0, 0, OR_CLAIR)


def buckle(f):
    if f.name == 'north':
        stamp(['OoO'], C)(f)
    elif f.side:
        for x in range(f.w):
            f.px(x, 0, P_CUIR[1])


def hem(f):
    """Jupe : liseré dore en dents de scie et plis plus sombres."""
    if not f.side:
        return
    for x in range(f.w):
        if x % 3 == 0:
            for y in range(f.h - 1):
                f.px(x, y, P_ROBE[1])
        f.px(x, f.h - 1, OR if x % 2 else OR_CLAIR)
        if x % 2 == 0 and f.h > 2:
            f.px(x, f.h - 2, OR)


def flower_side(f):
    if f.name == 'east':
        stamp(['.W.', 'WYW', '.W.'], C)(f)


def flower_small(f):
    if f.name == 'east':
        stamp(['WW', 'WY'], C)(f)


def leaf(f):
    for y in range(f.h):
        for x in range(f.w):
            f.px(x, y, C['v'] if (x + y) % 2 else C['g'])


def crystal_faces(f):
    """Cristal : facettes claires, arete centrale tres lumineuse."""
    for y in range(f.h):
        for x in range(f.w):
            k = 4 if x == f.w // 2 else 3 if y < f.h // 2 else 2
            if x == 0 or x == f.w - 1:
                k = 1
            f.px(x, y, P_CRISTAL[k], glow=True)


class Decal:
    def __init__(self, *decos):
        self.decos = decos

    def paint(self, f):
        for y in range(f.h):
            for x in range(f.w):
                f.clear(x, y)
        for d in self.decos:
            d(f)


class Wing:
    """Aile translucide : silhouette en feuille entre la racine et la pointe (coordonnees locales de l'aile),
    membrane jaune pale translucide, bord et nervures dores lumineux."""

    def __init__(self, root, tip, width, side):
        self.root, self.tip, self.width, self.side = root, tip, width, side

    def paint(self, f):
        (x0, y0, _), (w, h, _) = f.cube.frm, f.cube.size
        rx, ry = self.root
        tx, ty = self.tip
        L = math.hypot(tx - rx, ty - ry)
        for j in range(f.h):
            for i in range(f.w):
                wx, wy, _ = f.world(i, j)
                u = (wx - x0) if self.side > 0 else (x0 + w - wx)       # distance depuis le corps
                v = wy - y0
                t = ((u - rx) * (tx - rx) + (v - ry) * (ty - ry)) / (L * L)
                dist = abs((u - rx) * (ty - ry) - (v - ry) * (tx - rx)) / L
                if t < -0.02 or t > 1.0:
                    f.clear(i, j)
                    continue
                half = 0.9 + self.width * math.sin(math.pi * min(1.0, t * 1.08)) * (1 - 0.25 * t)
                if dist > half:
                    f.clear(i, j)
                elif dist > half - 0.9 or t > 0.93:
                    f.px(i, j, OR, glow=True)
                elif dist < 0.45 or abs(dist - half * 0.5) < 0.3 and (int(t * 10) % 2 == 0):
                    f.px(i, j, OR_CLAIR, glow=True, a=210)
                else:
                    f.px(i, j, (255, 236, 150), a=170)                    # membrane : translucide, non emissive


FACE = stamp([
    'HHHHHHHH',
    'HhHHHHhH',
    'H......H',
    'HKK..KKH',
    'HWG..WGH',
    'HGg..GgH',
    'Hp.mm.pH',
], C)


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))
    for side, sx in (('droite', 1), ('gauche', -1)):
        lg = m.bone(f'jambe_{side}', (1.1 * sx, 5, 0), 'racine')
        x0 = 0.1 if sx > 0 else -2.1
        m.cube(lg, f'jambe_{side}_peau', (x0, 3, -1), (2, 2, 2), PEAU, share='jambe', mirror=sx < 0)
        m.cube(lg, f'botte_{side}', (x0, 0, -1), (2, 3, 2), CUIR, inflate=0.1, share='botte', mirror=sx < 0)
        m.cube(lg, f'revers_{side}', (x0, 2.2, -1), (2, 1, 2), Mat(P_CREME, level=0.6, noise=0.15, decos=[gold_trim('t')]),
               inflate=0.3, share='revers', mirror=sx < 0)

    co = m.bone('corps', (0, 5, 0), 'racine')
    m.cube(co, 'jupon', (-3.5, 2, -2.5), (7, 1, 5), CREME, inflate=0.2)
    m.cube(co, 'jupe_bas', (-3.5, 3, -2.5), (7, 2, 5), Mat(P_ROBE, level=0.5, noise=0.2, scale=1.5, decos=[hem]))
    m.cube(co, 'jupe_haut', (-3, 5, -2), (6, 2, 4), ROBE)
    m.cube(co, 'ceinture', (-3, 6.5, -2), (6, 1, 4), Mat(P_CUIR, level=0.45, noise=0.2, decos=[buckle]), inflate=0.15)
    m.cube(co, 'torse', (-2.5, 7, -1.5), (5, 4, 3), Mat(P_ROBE, level=0.5, noise=0.2, scale=1.5, grad=0.15, decos=[dress_front]))

    for side, sx in (('droit', 1), ('gauche', -1)):
        rot = (0, 0, 8 * sx) if sx > 0 else (55, 0, 0)          # main gauche tendue vers l'avant (cristal)
        b = m.bone(f'bras_{side}', (3.2 * sx, 10.5, 0), co, rot=rot)
        x0 = 2.5 if sx > 0 else -4.5
        m.cube(b, f'manche_{side}', (x0, 7.5, -1), (2, 3, 2), ROBE, share='manche', mirror=sx < 0)
        m.cube(b, f'poignet_{side}', (x0, 7, -1), (2, 1, 2), Mat(P_CREME, level=0.6, noise=0.15, decos=[gold_trim('t')]),
               inflate=0.35, share='poignet', mirror=sx < 0)
        m.cube(b, f'main_{side}', (x0, 6, -1), (2, 1, 2), PEAU, inflate=-0.15, share='main', mirror=sx < 0)
        if sx < 0:
            cr = m.bone('cristal', (-3.5, 6, 0), b, rot=(-55, 0, 0))   # compense le bras : le cristal reste droit
            m.cube(cr, 'cristal_pierre', (-4.5, 7, -1), (2, 3, 2), Decal(crystal_faces), rot=(0, 45, 0), origin=(-3.5, 8.5, 0))
            m.cube(cr, 'cristal_pointe', (-4, 10, -0.5), (1, 1, 1), Decal(crystal_faces), rot=(0, 45, 0), origin=(-3.5, 10.5, 0))
            m.cube(cr, 'cristal_base', (-4, 6.4, -0.5), (1, 1, 1), Decal(crystal_faces), rot=(0, 45, 0), origin=(-3.5, 6.9, 0))

    h = m.bone('tete', (0, 11, 0), co)
    m.cube(h, 'tete_cube', (-4, 11, -3.5), (8, 7, 7), PEAU, deco={'north': [FACE]})
    m.cube(h, 'cheveux_dessus', (-4.5, 16.5, -4), (9, 2, 8), CHEVEUX)
    m.cube(h, 'frange', (-4.5, 15.5, -4.4), (9, 2, 1), Mat(P_CHEVEUX, level=0.55, noise=0.2, scale=(1, 3), decos=[strands, jagged_bottom]))
    m.cube(h, 'cheveux_dos', (-4.5, 8, 2.5), (9, 9, 2), Mat(P_CHEVEUX, level=0.5, noise=0.25, scale=(1, 3), grad=0.25,
                                                                decos=[strands, jagged_bottom]))
    for side, sx in (('droite', 1), ('gauche', -1)):
        m.cube(h, f'meche_{side}', (4 if sx > 0 else -5, 9, -3.5), (1, 7, 5),
               Mat(P_CHEVEUX, level=0.55, noise=0.2, scale=(1, 3), grad=0.2, decos=[strands, jagged_bottom]), share='meche', mirror=sx < 0)
        m.cube(h, f'oreille_{side}', (5 if sx > 0 else -7, 12.5, -0.5), (2, 1, 1), PEAU, share='oreille', mirror=sx < 0,
               rot=(0, 0, 28 * sx), origin=(5 * sx, 13, 0))
    m.cube(h, 'fleur', (4.6, 14, -2.5), (1, 3, 3), Decal(flower_side))
    m.cube(h, 'fleur_petite', (4.6, 16, 0.5), (1, 2, 2), Decal(flower_small))
    m.cube(h, 'feuilles', (4.4, 13.5, -0.5), (1, 2, 1), Decal(leaf))
    pp = m.bone('paupieres', (0, 15, -3.55), h, rot=(-90, 0, 0))
    m.cube(pp, 'paupieres_plan', (-3, 12, -3.55), (6, 3, 0),
           Decal(lambda f: stamp(['ss..ss', 'ss..ss', 'KK..KK'], dict(C, s=P_PEAU[2]))(f) if f.name == 'north' else None))

    # ailes : grande aile haute et petite aile basse de chaque cote, attachees au dos
    for side, sx in (('droite', 1), ('gauche', -1)):
        wh = m.bone(f'aile_haut_{side}', (1 * sx, 9.5, 1.6), co, rot=(0, -22 * sx, 12 * sx))
        m.cube(wh, f'aile_haut_{side}_membrane', (1 if sx > 0 else -13, 8, 1.6), (12, 14, 0),
               Wing(root=(0.3, 2.0), tip=(11.6, 13.6), width=3.0, side=sx))
        wb = m.bone(f'aile_bas_{side}', (1 * sx, 8.5, 1.6), co, rot=(0, -26 * sx, -8 * sx))
        m.cube(wb, f'aile_bas_{side}_membrane', (1 if sx > 0 else -10, 1.5, 1.6), (9, 8, 0),
               Wing(root=(0.3, 7.0), tip=(8.6, 0.6), width=2.0, side=sx))
    return m


COLORS = {'racine': 0, 'corps': 1, 'tete': 2, 'bras_droit': 3, 'bras_gauche': 3, 'cristal': 5, 'jambe_droite': 4, 'jambe_gauche': 4,
          'aile_haut_droite': 6, 'aile_haut_gauche': 6, 'aile_bas_droite': 6, 'aile_bas_gauche': 6}

AILES = ['aile_haut_droite', 'aile_haut_gauche', 'aile_bas_droite', 'aile_bas_gauche']
VOL_Y = 3.0                       # la fee flotte a 3 px du sol


def flap(a, length, period, amp, extra=None):
    """Battement d'ailes regulier (ouvertes vers l'avant / repliees vers l'arriere), cycles entiers sur la duree."""
    frames = []
    n = max(1, round(length / period))
    for k in range(2 * n + 1):
        t = length * k / (2 * n)
        s = amp if k % 2 == 0 else -amp
        frames.append((t, {'aile_haut_droite': (0, s, 4 if k % 2 == 0 else -4), 'aile_haut_gauche': (0, -s, -4 if k % 2 == 0 else 4),
                           'aile_bas_droite': (0, s * 0.8, 0), 'aile_bas_gauche': (0, -s * 0.8, 0)}))
    ek.key_poses(a, frames, interp='catmullrom', bones=AILES)


def animations(m):
    P, M = ek.key_poses, ek.M
    BASE = {'racine': (0, VOL_Y, 0)}

    # --- flottement (2,4 s) : vol sur place, leger balancement, jambes qui pendent, cristal qui pulse
    a = m.anim('flottement', 2.4, 'loop')
    flap(a, 2.4, 0.3, 18)
    P(a, [(0, {'racine': (0, VOL_Y, 0)}), (1.2, {'racine': (0, VOL_Y + 1.2, 0)}), (2.4, {'racine': (0, VOL_Y, 0)})], 'position', bones=['racine'])
    P(a, [(0, {'jambe_droite': (-12, 0, 0), 'jambe_gauche': (-35, 0, 0), 'tete': (3, 0, 4), 'bras_droit': (0, 0, 6)}),
          (1.2, {'jambe_droite': (-22, 0, 0), 'jambe_gauche': (-28, 0, 0), 'tete': (-2, 0, -4), 'bras_droit': (8, 0, 12), 'corps': (-3, 0, 0)}),
          (2.4, {'jambe_droite': (-12, 0, 0), 'jambe_gauche': (-35, 0, 0), 'tete': (3, 0, 4), 'bras_droit': (0, 0, 6)})])
    a.scale('cristal', [(0, 1, 1, 1), (1.2, 1.18, 1.18, 1.18), (2.4, 1, 1, 1)])

    # --- deplacement (1,2 s) : penchee vers l'avant, jambes en arriere, battements rapides
    a = m.anim('deplacement', 1.2, 'loop')
    flap(a, 1.2, 0.2, 24)
    vol = {'corps': (-18, 0, 0), 'tete': (14, 0, 0), 'jambe_droite': (-30, 0, 0), 'jambe_gauche': (-42, 0, 0), 'bras_droit': (-25, 0, 10)}
    P(a, [(0, vol), (0.6, M(vol, {'jambe_droite': (-8, 0, 0), 'jambe_gauche': (8, 0, 0)})), (1.2, vol)])
    P(a, [(0, {'racine': (0, VOL_Y + 0.5, 0)}), (0.6, {'racine': (0, VOL_Y + 1.2, 0)}), (1.2, {'racine': (0, VOL_Y + 0.5, 0)})],
      'position', bones=['racine'])

    # --- joie (1,6 s) : pirouette, bras leves, petit bond, cristal qui grossit
    a = m.anim('joie', 1.6, 'once')
    flap(a, 1.6, 0.16, 26)
    bras = {'bras_droit': (0, 0, 120), 'bras_gauche': (0, 0, -60), 'tete': (10, 0, 0), 'jambe_droite': (10, 0, 0), 'jambe_gauche': (-40, 0, 0)}
    P(a, [(0, {}), (0.3, bras), (1.2, bras), (1.6, {})])
    a.rot('racine', [(0, 0, 0, 0), (0.25, 0, 0, 0), (1.15, 0, 360, 0), (1.6, 0, 360, 0)], 'linear')
    P(a, [(0, BASE), (0.5, {'racine': (0, VOL_Y + 3, 0)}), (1.0, {'racine': (0, VOL_Y + 2, 0)}), (1.6, BASE)], 'position', bones=['racine'])
    P(a, [(0, {}), (0.6, {'cristal': (1.5, 1.5, 1.5)}), (1.6, {})], 'scale', bones=['cristal'])

    # --- sortilege (1,4 s) : leve le cristal, se cambre, puis le projette vers l'avant
    a = m.anim('sortilege', 1.4, 'once')
    flap(a, 1.4, 0.2, 20)
    leve = {'bras_gauche': (90, 0, 0), 'cristal': (-90, 0, 0), 'corps': (10, 0, 0), 'tete': (12, 0, 0), 'bras_droit': (0, 0, 40),
            'jambe_droite': (-18, 0, 0), 'jambe_gauche': (-30, 0, 0)}
    lance = {'bras_gauche': (35, 0, 0), 'cristal': (-35, 0, 0), 'corps': (-14, 0, 0), 'tete': (-6, 0, 0), 'bras_droit': (-20, 0, 20),
             'jambe_droite': (-35, 0, 0), 'jambe_gauche': (-45, 0, 0)}
    P(a, [(0, {}), (0.55, leve), (0.75, lance, 'linear'), (1.0, lance), (1.4, {})])
    P(a, [(0, {}), (0.55, {'cristal': (1.4, 1.4, 1.4)}), (0.75, {'cristal': (1.9, 1.9, 1.9)}, 'linear'), (1.0, {'cristal': (1.1, 1.1, 1.1)}),
          (1.4, {})], 'scale', bones=['cristal'])
    P(a, [(0, BASE), (0.55, {'racine': (0, VOL_Y + 1.5, 0.5)}), (0.75, {'racine': (0, VOL_Y + 1, -1)}, 'linear'), (1.4, BASE)], 'position',
      bones=['racine'])
