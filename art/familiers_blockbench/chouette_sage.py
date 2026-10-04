# -*- coding: utf-8 -*-
"""Chouette sage : petite chouette brune et creme (environ 0,75 bloc), grosse tete carree, grands yeux ambres,
sourcils clairs, petit bec dore, ailes articulees (deux segments), queue courte, pendentif bleu a rune."""
import entity_kit as ek
from entity_kit import pal, Mat, stamp, border, blotches

KEY = 'chouette_sage'
TITLE = 'Chouette sage'
ENTITY_CLASS = 'ChouetteSageEntity'
VISIBLE_BOX = (0.6, 0.8, 0)
VIEW_SCALE = 26.0
SHEET_SCALE = 15.0
GIF_SCALE = 13.0

P_BRUN = pal('#3a2213', '#50301b', '#684225', '#825532', '#9d6b42', '#b78557')
P_CREME = pal('#a8906e', '#c2a986', '#d6bf9c', '#e7d4b4', '#f4e7cf')
P_OR = pal('#6e4b12', '#9c6c1b', '#c99428', '#eab944', '#fbd87e')
P_RUNE = pal('#163f9e', '#2a6fe0', '#55a6ff', '#b6e2ff')
AMBRE, AMBRE_F, NOIR, BLANC = (236, 158, 34), (190, 108, 22), (20, 14, 10), (255, 250, 236)

FEATHER = [P_BRUN[1], P_CREME[1]]
BRUN = Mat(P_BRUN, level=0.5, noise=0.3, scale=1.5, grad=0.15, decos=[blotches(4, [P_BRUN[1], P_BRUN[4]], (0.4, 0.8))])
CREME = Mat(P_CREME, level=0.55, noise=0.25, scale=1.5, grad=0.15)
OR = Mat(P_OR, level=0.6, noise=0.15, scale=1, grad=0.3)

C = {'b': P_BRUN[2], 'B': P_BRUN[1], 'c': P_CREME[3], 'C': P_CREME[4], 'A': AMBRE, 'a': AMBRE_F, 'K': NOIR, 'W': BLANC,
     'r': P_RUNE[2], 'R': P_RUNE[3]}


class Decal:
    def __init__(self, *decos):
        self.decos = decos

    def paint(self, f):
        for y in range(f.h):
            for x in range(f.w):
                f.clear(x, y)
        for d in self.decos:
            d(f)


FACE = stamp([
    'CCCbbCCC',
    'AKWccWKA',
    'aAAccAAa',
    'cccccccc',
    'bccccccb',
    'bbccccbb',
], C)
LIDS = Decal(lambda f: stamp(['bbb..bbb', 'BBB..BBB'], C, ay='t')(f) if f.name == 'north' else None)


def belly(f):
    """Ventre creme a chevrons bruns ; plumes du dos tachetees."""
    if f.name == 'north':
        for y in range(f.h):
            for x in range(f.w):
                f.px(x, y, P_CREME[3] if (x + y) % 4 else P_CREME[2])
        for y in range(1, f.h, 2):
            for x in range(f.w):
                if (x + y // 2) % 3 == 0:
                    f.px(x, y, P_BRUN[3])
        border(P_BRUN[2], 'lr')(f)
        for x in range(1, f.w - 1):
            f.px(x, 0, P_OR[2] if x % 2 else P_OR[1])            # cordon du pendentif


def wing_feathers(f):
    """Ailes : barres de plumes claires dehors, dessous creme (face interieure)."""
    if f.name in ('east', 'west'):
        inner = (f.name == 'west') == (f.cube.frm[0] > 0)        # face tournee vers le corps
        for y in range(f.h):
            for x in range(f.w):
                if inner:
                    f.px(x, y, P_CREME[2] if (x + y) % 3 else P_CREME[1])
                elif y % 2 == 1 and x % 2 == 0:
                    f.px(x, y, P_CREME[2])
        if not inner:
            for x in range(f.w):
                f.px(x, f.h - 1, P_BRUN[1])


def tail_bands(f):
    if f.side or f.name == 'up':
        for y in range(f.h):
            for x in range(f.w):
                if (x if f.name == 'up' else y) % 2 == 1:
                    f.px(x, y, P_CREME[2])


def talons(f):
    if f.name == 'north':
        for x in range(f.w):
            f.px(x, f.h - 1, NOIR if x % 2 == 0 else P_OR[1])


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))
    co = m.bone('corps', (0, 1, 0), 'racine')
    m.cube(co, 'corps_plumes', (-3.5, 1, -3), (7, 5, 6), BRUN, deco={'north': [belly]})
    m.cube(co, 'pendentif', (-1, 2.5, -3.5), (2, 2, 1), OR, deco={'north': [stamp(['rR', 'Rr'], C, glow='rR')]})

    h = m.bone('tete', (0, 6, -0.5), co)
    m.cube(h, 'tete_cube', (-4, 6, -4), (8, 6, 7), BRUN, deco={'north': [FACE]})
    for side, sx in (('droite', 1), ('gauche', -1)):
        x0 = 2 if sx > 0 else -4
        m.cube(h, f'aigrette_{side}', (x0, 12, -2.5), (2, 2, 2), BRUN, rot=(0, 0, -22 * sx), origin=(x0 + 1, 12, -1.5),
               share='aigrette', mirror=sx < 0, deco={'north': [lambda f: f.px(0, 0, P_CREME[3])]})
    bc = m.bone('bec', (0, 8.5, -4), h)
    m.cube(bc, 'bec_cube', (-1, 7, -5), (2, 2, 1), OR, deco={'north': [stamp(['..', 'aa'], {'a': P_OR[1]})]})
    pp = m.bone('paupieres', (0, 11, -4.05), h, rot=(-90, 0, 0))
    m.cube(pp, 'paupieres_plan', (-4, 9, -4.05), (8, 2, 0), LIDS)

    for side, sx in (('droite', 1), ('gauche', -1)):
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        aw = m.bone(f'aile_{side}', (3.5 * sx, 5.5, -1), co, rot=(0, 0, 4 * sx))
        m.cube(aw, f'aile_{side}_base', (xo(3.5, 1), 1.5, -2.5), (1, 4, 5), BRUN, share='aile_base', mirror=sx < 0, deco={'*': [wing_feathers]})
        tip = m.bone(f'aile_{side}_bout', (4 * sx, 2.5, 2.5), aw, rot=(-8, 0, 0))
        m.cube(tip, f'aile_{side}_pointe', (xo(3.5, 1), 1.5, 1.5), (1, 3, 3), BRUN, share='aile_pointe', mirror=sx < 0, deco={'*': [wing_feathers]})

    q = m.bone('queue', (0, 2.5, 3), co, rot=(10, 0, 0))
    m.cube(q, 'queue_plumes', (-1.5, 1.5, 3), (3, 2, 3), BRUN, deco={'*': [tail_bands]})

    for side, sx in (('droite', 1), ('gauche', -1)):
        pt = m.bone(f'patte_{side}', (1.5 * sx, 1, -1), 'racine')
        m.cube(pt, f'patte_{side}_serres', (1.5 * sx - 1, 0, -2.5), (2, 1, 2), OR, share='patte', mirror=sx < 0, deco={'*': [talons]})
    return m


COLORS = {'racine': 0, 'corps': 1, 'tete': 2, 'bec': 5, 'aile_droite': 3, 'aile_gauche': 3, 'queue': 6, 'patte_droite': 7, 'patte_gauche': 7}


def animations(m):
    P, M, SC = ek.key_poses, ek.M, ek.SC
    L = 'linear'
    FERME = {'paupieres': (90, 0, 0)}

    # --- repos : inclinaison curieuse de la tete, regard qui tourne, petit souffle (3 s)
    a = m.anim('repos', 3.0, 'loop')
    P(a, [(0, {}), (0.6, {}), (0.9, {'tete': (0, 22, 18)}), (1.5, {'tete': (0, 22, 18)}), (1.8, {}), (2.2, {'tete': (4, -30, -10)}),
          (2.6, {'tete': (4, -30, -10)}), (3.0, {})])
    P(a, [(0, {}), (1.5, {'corps': (0, 0.2, 0)}), (3.0, {})], 'position', bones=['corps'])

    # --- clignement des yeux (0,35 s) : les paupieres basculent devant les yeux
    a = m.anim('clignement', 0.35, 'once')
    P(a, [(0, {}), (0.12, FERME, L), (0.2, FERME, L), (0.35, {}, L)])

    # --- petits pas (0,8 s) : sautillement, pattes alternees, ailes pour l'equilibre
    a = m.anim('petits_pas', 0.8, 'loop')
    P(a, [(0, {'patte_droite': (25, 0, 0), 'patte_gauche': (-15, 0, 0), 'corps': (0, 0, 5), 'aile_droite': (0, 0, 6)}),
          (0.2, {'corps': (-4, 0, 0), 'queue': (8, 0, 0)}),
          (0.4, {'patte_droite': (-15, 0, 0), 'patte_gauche': (25, 0, 0), 'corps': (0, 0, -5), 'aile_gauche': (0, 0, -6)}),
          (0.6, {'corps': (-4, 0, 0), 'queue': (8, 0, 0)}),
          (0.8, {'patte_droite': (25, 0, 0), 'patte_gauche': (-15, 0, 0), 'corps': (0, 0, 5), 'aile_droite': (0, 0, 6)})])
    P(a, [(0, {}), (0.2, {'racine': (0, 1, 0)}), (0.4, {}), (0.6, {'racine': (0, 1, 0)}), (0.8, {})], 'position', bones=['racine'])

    # --- vol (0,6 s, en boucle) : battements, pointes en retard, pattes repliees (en l'air : decalage +5)
    haut = {'aile_droite': (0, 0, 105), 'aile_gauche': (0, 0, -105), 'aile_droite_bout': (8, 0, 20), 'aile_gauche_bout': (8, 0, -20)}
    bas = {'aile_droite': (0, 0, -5), 'aile_gauche': (0, 0, 5), 'aile_droite_bout': (8, 0, -25), 'aile_gauche_bout': (8, 0, 25)}
    vol = {'corps': (-22, 0, 0), 'tete': (16, 0, 0), 'patte_droite': (-50, 0, 0), 'patte_gauche': (-50, 0, 0), 'queue': (-12, 0, 0)}
    a = m.anim('vol', 0.6, 'loop')
    P(a, [(0, M(vol, haut)), (0.3, M(vol, bas)), (0.6, M(vol, haut))])
    P(a, [(0, {'racine': (0, 5, 0)}), (0.3, {'racine': (0, 5.8, 0)}), (0.6, {'racine': (0, 5, 0)})], 'position', bones=['racine'])

    # --- decollage (0,9 s) : se ramasse, ailes levees, premier battement, s'eleve jusqu'a la pose de vol
    a = m.anim('decollage', 0.9, 'once')
    P(a, [(0, {}), (0.25, {'corps': (-10, 0, 0), 'tete': (6, 0, 0), 'aile_droite': (0, 0, 40), 'aile_gauche': (0, 0, -40)}),
          (0.45, M(vol, haut)), (0.65, M(vol, bas), L), (0.9, M(vol, haut))])
    P(a, [(0, {}), (0.25, {'corps': (0, -0.5, 0)}), (0.45, {'racine': (0, 1.5, 0)}), (0.65, {'racine': (0, 3.5, 0)}, L), (0.9, {'racine': (0, 5, 0)})],
      'position', bones=['racine', 'corps'])

    # --- atterrissage (0,8 s) : freine ailes ouvertes, pattes en avant, se pose et replie les ailes
    a = m.anim('atterrissage', 0.8, 'once')
    frein = {'corps': (10, 0, 0), 'tete': (-6, 0, 0), 'aile_droite': (0, 0, 80), 'aile_gauche': (0, 0, -80), 'aile_droite_bout': (-20, 0, 20),
             'aile_gauche_bout': (-20, 0, -20), 'patte_droite': (35, 0, 0), 'patte_gauche': (35, 0, 0), 'queue': (-20, 0, 0)}
    P(a, [(0, M(vol, haut)), (0.3, frein), (0.55, {'corps': (-8, 0, 0), 'aile_droite': (0, 0, 20), 'aile_gauche': (0, 0, -20)}), (0.8, {})])
    P(a, [(0, {'racine': (0, 5, 0)}), (0.3, {'racine': (0, 2, 0)}), (0.55, {'corps': (0, -0.4, 0)}), (0.8, {})], 'position', bones=['racine', 'corps'])

    # --- sommeil (3 s, en boucle) : tete baissee, yeux fermes, respiration lente
    a = m.anim('sommeil', 3.0, 'loop')
    dort = M(FERME, {'tete': (-16, 0, 6), 'aile_droite': (0, 0, -3), 'aile_gauche': (0, 0, 3), 'queue': (8, 0, 0)})
    P(a, [(0, dort), (1.5, M(dort, {'tete': (-3, 0, 0)})), (3.0, dort)])
    P(a, [(0, {'tete': (0, -0.3, 0)}), (1.5, {'corps': (0, 0.25, 0), 'tete': (0, -0.2, 0)}), (3.0, {'tete': (0, -0.3, 0)})], 'position', bones=['corps', 'tete'])

    # --- pose perchee (3 s, en boucle) : serres agrippees, queue tombante, leger balancement, regard alentour
    a = m.anim('perche', 3.0, 'loop')
    perche = {'patte_droite': (18, 0, 0), 'patte_gauche': (18, 0, 0), 'queue': (28, 0, 0), 'corps': (6, 0, 0)}
    P(a, [(0, M(perche, {'corps': (0, 0, 3)})), (0.8, M(perche, {'tete': (0, 35, 0)})), (1.5, M(perche, {'corps': (0, 0, -3)})),
          (2.3, M(perche, {'tete': (0, -35, 0)})), (3.0, M(perche, {'corps': (0, 0, 3)}))])
    P(a, [(0, {'corps': (0, -0.4, 0)}), (1.5, {'corps': (0, -0.3, 0)}), (3.0, {'corps': (0, -0.4, 0)})], 'position', bones=['corps'])
