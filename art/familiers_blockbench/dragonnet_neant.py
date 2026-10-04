# -*- coding: utf-8 -*-
"""Dragonnet du neant : bebe dragon d'obsidienne (environ 1 bloc de haut), grosse tete aux grands yeux violets lumineux,
cornes a pointe de cristal, petites epines, runes violettes luisantes sur le corps et les pattes, quatre pattes trapues
a griffes violettes, ailes de chauve-souris a membrane violette (deux segments), queue segmentee terminee par un cristal."""
import entity_kit as ek
from entity_kit import pal, Mat, stamp

KEY = 'dragonnet_neant'
TITLE = 'Dragonnet du néant'
ENTITY_CLASS = 'DragonnetNeantEntity'
VISIBLE_BOX = (0.9, 1.2, 0)
VIEW_SCALE = 15.0
SHEET_SCALE = 9.5
GIF_SCALE = 8.0
SHEET_YAW = 50

P_NOIR = pal('#110d16', '#1a1422', '#241c2e', '#2e243a', '#392d48')
P_VIOLET = pal('#3e1878', '#5a26ac', '#7a3cdc', '#9a5cf6', '#ba8cff', '#dcc4ff')
P_MEMBRANE = pal('#2a1450', '#3c1d70', '#522a92', '#6a3cb2')
C = {'E': P_VIOLET[3], 'e': P_VIOLET[2], 'W': (240, 228, 255), 'K': (8, 6, 12), 'v': P_VIOLET[1], 'V': P_VIOLET[4], 'n': P_NOIR[0]}

RUNES = [['V.', 'V.', '.V'], ['VV.', '.V.'], ['V..', '.V.', 'V..'], ['V', 'V', '.'], ['.V', 'V.', 'V.'], ['V.V', '.V.']]


def runes(n=1, sides_only=True):
    """Runes violettes lumineuses gravees sur les faces."""
    def d(f):
        if sides_only and not f.side:
            return
        if f.w < 4 or f.h < 4:
            return
        for _ in range(n):
            r = f.rng.choice(RUNES)
            w, h = len(r[0]), len(r)
            if f.w < w + 1 or f.h < h + 1:
                continue
            x0, y0 = f.rng.randrange(0, f.w - w + 1), f.rng.randrange(0, f.h - h + 1)
            for j, row in enumerate(r):
                for i, ch in enumerate(row):
                    if ch == 'V':
                        f.px(x0 + i, y0 + j, P_VIOLET[2] if (i + j) % 2 else P_VIOLET[3], glow=True)
    return d


def scales(f):
    """Ecailles : petits reflets violaces en quinconce."""
    for y in range(0, f.h, 2):
        for x in range((y // 2) % 2, f.w, 3):
            f.px(x, y, P_NOIR[4])


def belly(f):
    if f.name == 'down':
        for y in range(f.h):
            for x in range(f.w):
                f.px(x, y, P_VIOLET[0] if y % 2 else P_VIOLET[1])


ECAILLE = Mat(P_NOIR, level=0.5, noise=0.25, scale=1.5, grad=0.2, up=0.15, decos=[scales, runes(1), belly])
ECAILLE_RUNES = Mat(P_NOIR, level=0.5, noise=0.25, scale=1.5, grad=0.2, up=0.15, decos=[scales, runes(2)])
ECAILLE_LISSE = Mat(P_NOIR, level=0.5, noise=0.2, scale=1.5, grad=0.2, up=0.15, decos=[scales])
CRISTAL = Mat(P_VIOLET, level=0.68, noise=0.25, scale=1, grad=0.35, up=0.25, glow=True)
GRIFFE = Mat(P_VIOLET, level=0.55, noise=0.1, scale=1, grad=0.3, glow=True)


class Decal:
    def __init__(self, *decos):
        self.decos = decos

    def paint(self, f):
        for y in range(f.h):
            for x in range(f.w):
                f.clear(x, y)
        for d in self.decos:
            d(f)


class Membrane:
    """Membrane d'aile (plan) : violet sombre translucide, bord inferieur festonne, nervures plus claires."""

    def __init__(self, scallop=3):
        self.scallop = scallop

    def paint(self, f):
        for j in range(f.h):
            for i in range(f.w):
                k = i % self.scallop
                cut = f.h - 1 - (1 if k in (0, self.scallop - 1) else 0) - (1 if k == self.scallop // 2 else 0) * 0
                if j > cut or (j == f.h - 1 and k != self.scallop // 2):
                    f.clear(i, j)
                elif i % self.scallop == 0:
                    f.px(i, j, P_VIOLET[2], glow=True, a=235)
                else:
                    f.px(i, j, P_MEMBRANE[2 + (i + j) % 2], a=215)


FACE = stamp([
    '........',
    'eEE..EEe',
    'EWE..EWE',
    'eEE..EEe',
    '........',
], C, glow='EeW', ay='t')
NOSTRILS = stamp(['K..K'], C, ay='t', dy=0)
LIDS = Decal(lambda f: stamp(['nnn..nnn', 'nnn..nnn', 'vvv..vvv'], C)(f) if f.name == 'north' else None)


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))

    # ---------------------------------------------------------------- pattes (avant / arriere)
    for avant, z in (('avant', -2.5), ('arriere', 4.0)):
        for side, sx in (('droite', 1), ('gauche', -1)):
            name = f'patte_{avant}_{side}'
            p = m.bone(name, (2.6 * sx, 6.5, z), 'racine')
            x0 = 2.6 * sx - 1.5
            if avant == 'arriere':
                m.cube(p, f'{name}_cuisse', (x0 - 0.25 * sx, 3, z - 2), (3, 4, 4), ECAILLE_RUNES, share='cuisse', mirror=sx < 0)
            m.cube(p, f'{name}_jambe', (x0, 0, z - 1.5), (3, 6, 3), ECAILLE_RUNES if avant == 'avant' else ECAILLE_LISSE,
                   share=f'jambe_{avant}', mirror=sx < 0)
            for k in range(3):
                m.cube(p, f'{name}_griffe_{k + 1}', (x0 + k, 0, z - 2.2), (1, 1, 1), GRIFFE, inflate=-0.08, share='griffe')

    # ---------------------------------------------------------------- corps
    co = m.bone('corps', (0, 7, 1), 'racine')
    m.cube(co, 'torse', (-3.5, 4.5, -4), (7, 6, 10), ECAILLE)
    m.cube(co, 'poitrail', (-3, 4.2, -4.6), (6, 5, 2), ECAILLE_RUNES)
    for k, z in enumerate((-2, 1, 4)):
        m.cube(co, f'epine_dos_{k + 1}', (-0.5, 10.5, z), (1, 1 + (k % 2), 1), CRISTAL if k == 1 else ECAILLE_LISSE, share=f'epine_{k}')

    # ---------------------------------------------------------------- cou et tete
    cou = m.bone('cou', (0, 9.5, -3.5), co, rot=(-14, 0, 0))
    m.cube(cou, 'cou_ecailles', (-2, 8.5, -6), (4, 6, 4), ECAILLE_RUNES)
    t = m.bone('tete', (0, 13.5, -5), cou)
    m.cube(t, 'crane', (-4, 12, -10), (8, 7, 7), Mat(P_NOIR, level=0.5, noise=0.2, scale=1.5, grad=0.15, up=0.15,
                                                       decos=[scales, runes(1)]), deco={'north': [FACE]})
    m.cube(t, 'museau', (-2.5, 12, -13), (5, 3, 3), ECAILLE_LISSE, deco={'north': [NOSTRILS]})
    mc = m.bone('machoire', (0, 12.2, -10), t)
    m.cube(mc, 'machoire_bas', (-2.5, 11, -13), (5, 1, 3), Mat(P_NOIR, level=0.4, noise=0.15, decos=[belly]))
    pp = m.bone('paupieres', (0, 18, -10.05), t, rot=(-90, 0, 0))
    m.cube(pp, 'paupieres_plan', (-4, 15, -10.05), (8, 3, 0), LIDS)
    for side, sx in (('droite', 1), ('gauche', -1)):
        cn = m.bone(f'corne_{side}', (2.5 * sx, 19, -5), t, rot=(28, 0, -12 * sx))
        m.cube(cn, f'corne_{side}_base', (2.5 * sx - 1, 18.5, -6), (2, 3, 2), ECAILLE_LISSE, share='corne', mirror=sx < 0)
        m.cube(cn, f'corne_{side}_cristal', (2.5 * sx - 0.5, 21.5, -5.5), (1, 3, 1), CRISTAL, inflate=0.15, share='corne_cristal')
        m.cube(t, f'oreillon_{side}', (4 if sx > 0 else -5, 15, -5), (1, 3, 2), ECAILLE_LISSE, share='oreillon', mirror=sx < 0,
               rot=(0, 0, -20 * sx), origin=(4 * sx, 16, -4))
    for k, z in enumerate((-8.5, -6.5)):
        m.cube(t, f'epine_tete_{k + 1}', (-0.5, 19, z), (1, 1 + k, 1), ECAILLE_LISSE, share=f'epine_tete_{k}')

    # ---------------------------------------------------------------- ailes de chauve-souris (bras + doigt)
    for side, sx in (('droite', 1), ('gauche', -1)):
        a = m.bone(f'aile_{side}', (3 * sx, 10.5, -1), co, rot=AILE_REPOS[side])
        m.cube(a, f'aile_{side}_os', (3 * sx - 0.5, 10.5, -1.5), (1, 7, 1), ECAILLE_LISSE, share='aile_os', mirror=sx < 0)
        m.cube(a, f'aile_{side}_membrane', (3 * sx, 11, -1), (0, 6, 6), Membrane(3), share='aile_membrane')
        b = m.bone(f'aile_{side}_bout', (3 * sx, 17, -1), a, rot=(-35, 0, 0))
        m.cube(b, f'aile_{side}_doigt', (3 * sx - 0.5, 17, -1.5), (1, 1, 8), ECAILLE_LISSE, share='aile_doigt', mirror=sx < 0)
        m.cube(b, f'aile_{side}_pointe', (3 * sx - 0.5, 17.5, -2.5), (1, 1, 1), CRISTAL, share='aile_pointe')
        m.cube(b, f'aile_{side}_voile', (3 * sx, 12, -1), (0, 5, 8), Membrane(4), share='aile_voile')

    # ---------------------------------------------------------------- queue (4 segments, cristal au bout)
    parent = co
    z0 = 5.5
    for k, (w, ln, rx) in enumerate(((4, 4, 8), (3, 4, -10), (2, 4, -18), (2, 3, -22))):
        q = m.bone(f'queue_{k + 1}', (0, 8, z0), parent, rot=(rx, 0, 0))
        m.cube(q, f'queue_{k + 1}_segment', (-w / 2, 8 - w / 2, z0), (w, w, ln), ECAILLE_RUNES if k % 2 == 0 else ECAILLE_LISSE)
        parent = q
        z0 += ln
    m.cube(parent, 'queue_cristal', (-1.5, 6.5, z0 - 0.5), (3, 3, 3), CRISTAL, rot=(45, 0, 45), origin=(0, 8, z0 + 1))
    m.cube(parent, 'queue_cristal_pointe', (-0.5, 7.5, z0 + 2), (1, 1, 2), CRISTAL, inflate=0.1)
    return m


AILE_REPOS = {'droite': (28, 0, -30), 'gauche': (28, 0, 30)}
COLORS = {'racine': 0, 'corps': 1, 'cou': 1, 'tete': 2, 'machoire': 2, 'aile_droite': 3, 'aile_gauche': 3, 'aile_droite_bout': 3,
          'aile_gauche_bout': 3, 'queue_1': 6, 'queue_2': 6, 'queue_3': 6, 'queue_4': 6, 'patte_avant_droite': 4,
          'patte_avant_gauche': 4, 'patte_arriere_droite': 4, 'patte_arriere_gauche': 4, 'corne_droite': 5, 'corne_gauche': 5}
QUEUE = ['queue_1', 'queue_2', 'queue_3', 'queue_4']


def ailes(open_=0.0, flap=0.0, bout=0.0):
    """open_ : ouverture laterale (0 = repos, 1 = deployees a l'horizontale) ; flap : battement (+ = vers le haut)."""
    z = -55 * open_ - flap
    return {'aile_droite': (-20 * open_, 0, z), 'aile_gauche': (-20 * open_, 0, -z),
            'aile_droite_bout': (bout, 0, 0), 'aile_gauche_bout': (bout, 0, 0)}


def queue(ry, rx=0.0):
    return {b: (rx, ry * (0.6 + 0.4 * i), 0) for i, b in enumerate(QUEUE)}


def animations(m):
    P, M = ek.key_poses, ek.M
    L = 'linear'

    # --- repos (3 s) : respiration, queue qui ondule, ailes qui frissonnent, tete qui penche
    a = m.anim('repos', 3.0, 'loop')
    P(a, [(0, M(queue(8))), (0.75, M(queue(0, -4), {'tete': (4, 0, 6)}, ailes(0.1))), (1.5, M(queue(-8), {'tete': (0, 0, 0)})),
          (2.25, M(queue(0, -4), {'tete': (4, 0, -6)}, ailes(0.1))), (3.0, M(queue(8)))])
    a.scale('corps', [(0, 1, 1, 1), (1.5, 1.04, 1.04, 1.02), (3.0, 1, 1, 1)])

    # --- marche (0,9 s) : pas diagonaux, tete qui dodeline, queue qui balance
    a = m.anim('marche', 0.9, 'loop')
    s = 26
    pa = {'patte_avant_droite': (s, 0, 0), 'patte_arriere_gauche': (s, 0, 0), 'patte_avant_gauche': (-s, 0, 0), 'patte_arriere_droite': (-s, 0, 0)}
    pb = {k: (-v[0], 0, 0) for k, v in pa.items()}
    P(a, [(0, M(pa, queue(10), {'tete': (0, 6, 0)})), (0.45, M(pb, queue(-10), {'tete': (0, -6, 0)})), (0.9, M(pa, queue(10), {'tete': (0, 6, 0)}))])
    P(a, [(0, {}), (0.225, {'racine': (0, 0.8, 0)}), (0.45, {}), (0.675, {'racine': (0, 0.8, 0)}), (0.9, {})], 'position', bones=['racine'])

    # --- vol (0,7 s) : ailes deployees qui battent, pattes repliees, queue tendue
    a = m.anim('vol', 0.7, 'loop')
    pose = {'patte_avant_droite': (-50, 0, 0), 'patte_avant_gauche': (-50, 0, 0), 'patte_arriere_droite': (60, 0, 0),
            'patte_arriere_gauche': (60, 0, 0), 'queue_1': (-6, 0, 0), 'queue_2': (10, 0, 0), 'queue_3': (16, 0, 0), 'queue_4': (18, 0, 0),
            'cou': (6, 0, 0), 'tete': (-4, 0, 0)}
    P(a, [(0, M(pose, ailes(1, 35, -10))), (0.35, M(pose, ailes(1, -35, 25))), (0.7, M(pose, ailes(1, 35, -10)))])
    P(a, [(0, {'racine': (0, 5, 0)}), (0.35, {'racine': (0, 6.2, 0)}), (0.7, {'racine': (0, 5, 0)})], 'position', bones=['racine'])

    # --- joie (1,6 s) : petits bonds, ailes ouvertes, queue qui fretille
    a = m.anim('joie', 1.6, 'once')
    P(a, [(0, {}), (0.2, M(ailes(0.8, 10), queue(14), {'tete': (8, 0, 0)})), (0.4, M(ailes(0.8, -15), queue(-14), {'tete': (8, 0, 0)})),
          (0.6, M(ailes(0.8, 10), queue(14), {'tete': (8, 0, 0)})), (0.8, M(ailes(0.8, -15), queue(-14), {'tete': (8, 0, 0)})),
          (1.0, M(ailes(0.8, 10), queue(14), {'tete': (8, 0, 0)})), (1.6, {})])
    P(a, [(0, {}), (0.2, {'racine': (0, 3, 0)}), (0.4, {}), (0.6, {'racine': (0, 3, 0)}), (0.8, {}), (1.6, {})], 'position', bones=['racine'])
    P(a, [(0, {}), (0.2, {'patte_avant_droite': (-30, 0, 0), 'patte_avant_gauche': (-30, 0, 0)}), (0.4, {}),
          (0.6, {'patte_avant_droite': (-30, 0, 0), 'patte_avant_gauche': (-30, 0, 0)}), (0.8, {}), (1.6, {})],
      bones=['patte_avant_droite', 'patte_avant_gauche'])

    # --- souffle (1,6 s) : inspire (tete en arriere), puis crache le souffle du neant gueule ouverte
    a = m.anim('souffle', 1.6, 'once')
    inspire = {'cou': (22, 0, 0), 'tete': (14, 0, 0), 'corps': (6, 0, 0), 'machoire': (-10, 0, 0)}
    crache = {'cou': (-14, 0, 0), 'tete': (-6, 0, 0), 'corps': (-6, 0, 0), 'machoire': (-38, 0, 0), 'patte_avant_droite': (-8, 0, 0),
              'patte_avant_gauche': (-8, 0, 0)}
    P(a, [(0, {}), (0.5, M(inspire, ailes(0.4))), (0.65, M(crache, ailes(0.6)), L), (1.2, M(crache, ailes(0.6))), (1.6, {})])
    P(a, [(0, {}), (0.5, {'corne_droite': (1.2, 1.2, 1.2), 'corne_gauche': (1.2, 1.2, 1.2)}), (1.2, {'corne_droite': (1.2, 1.2, 1.2),
                                                                                                      'corne_gauche': (1.2, 1.2, 1.2)}),
          (1.6, {})], 'scale', bones=['corne_droite', 'corne_gauche'])
