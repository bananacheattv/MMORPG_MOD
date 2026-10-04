# -*- coding: utf-8 -*-
"""Feu follet : petite flamme magique flottante (environ 0,6 bloc), noyau cubique cyan, deux yeux blancs,
trois sections de flammes bleues en escalier. Aucun membre."""
import math

import entity_kit as ek
from entity_kit import pal, Mat, stamp

KEY = 'feu_follet'
TITLE = 'Feu follet'
ENTITY_CLASS = 'FeuFolletEntity'
VISIBLE_BOX = (0.5, 0.7, 0)
VIEW_SCALE = 30.0
SHEET_SCALE = 18.0
GIF_SCALE = 16.0

P_FLAMME = pal('#0e2785', '#183bb0', '#2556db', '#367cf2', '#57a2ff', '#8ccdff')
P_NOYAU = pal('#38aedf', '#5bcdf2', '#84e3fb', '#b4f4ff', '#e4feff')
BLANC = (250, 255, 255)

OUTER = Mat(P_FLAMME, level=0.38, noise=0.3, scale=(1, 2), grad=-0.25, up=0.1, down=-0.1, glow=True)
INNER = Mat(P_FLAMME, level=0.68, noise=0.25, scale=(1, 2), grad=-0.2, up=0.15, down=0.0, glow=True)
CORE = Mat(P_NOYAU, level=0.62, noise=0.2, scale=1.5, grad=0.3, glow=True)


class Decal:
    """Matiere 'decalque' : tout transparent sauf les motifs (plans de visage, paupieres)."""

    def __init__(self, *decos):
        self.decos = decos

    def paint(self, f):
        for y in range(f.h):
            for x in range(f.w):
                f.clear(x, y)
        for d in self.decos:
            d(f)


EYES = Decal(lambda f: stamp(['.....', '.W.W.', '.W.W.', '.....', '.....'], {'W': BLANC}, glow='W')(f) if f.name == 'north' else None)


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))
    n = m.bone('noyau', (0, 5, 0), 'racine')
    m.cube(n, 'noyau_cube', (-2.5, 2.5, -2.5), (5, 5, 5), CORE)
    v = m.bone('visage', (0, 5, -2.6), n)
    m.cube(v, 'visage_plan', (-2.5, 2.5, -2.6), (5, 5, 0), EYES)

    f1 = m.bone('flamme_1', (0, 3, 0), 'racine')
    m.cube(f1, 'flamme_1_base', (-3, 1.5, -3), (6, 2, 6), INNER)
    m.cube(f1, 'flamme_1_pointe', (-1.5, 0.5, -1.5), (3, 1, 3), OUTER)
    f2 = m.bone('flamme_2', (0, 5, 0), 'racine')
    m.cube(f2, 'flamme_2_gauche', (-3.5, 3, -2), (1, 5, 4), OUTER)
    m.cube(f2, 'flamme_2_droite', (2.5, 3, -1.5), (1, 4, 3), OUTER)
    m.cube(f2, 'flamme_2_dos', (-2.5, 3, 2.5), (5, 6, 1), OUTER)
    m.cube(f2, 'flamme_2_langue_gauche', (-4.5, 5, -1), (1, 3, 2), OUTER)
    m.cube(f2, 'flamme_2_langue_droite', (3.5, 4, -0.5), (1, 2, 2), OUTER)
    m.cube(f2, 'flamme_2_pointe_gauche', (-3.5, 8, -1), (1, 2, 2), OUTER)
    m.cube(f2, 'flamme_2_pointe_droite', (2.5, 7, -0.5), (1, 2, 1), OUTER)
    m.cube(f2, 'flamme_2_pointe_dos', (-1.5, 9, 2.5), (2, 2, 1), OUTER)
    m.cube(f2, 'flamme_2_meche_dos', (0.5, 9, 2.5), (1, 1, 1), INNER)
    f3 = m.bone('flamme_3', (0, 7.5, 0), 'racine')
    m.cube(f3, 'flamme_3_corps', (-2.5, 7.5, -2), (5, 1, 4), INNER)
    m.cube(f3, 'flamme_3_marche', (-2, 8.5, -1), (3, 1, 3), INNER)
    m.cube(f3, 'flamme_3_volute', (-2.5, 9.5, -0.5), (2, 1, 1), OUTER)
    m.cube(f3, 'flamme_3_pointe', (-3, 10.5, -0.5), (1, 1, 1), OUTER)
    return m


COLORS = {'racine': 0, 'noyau': 1, 'visage': 2, 'flamme_1': 4, 'flamme_2': 5, 'flamme_3': 6}


def animations(m):
    P, M, SC = ek.key_poses, ek.M, ek.SC
    L = 'linear'

    # --- flottement (2 s) : monte et descend, flammes qui vacillent, clignement des yeux
    a = m.anim('flottement', 2.0, 'loop')
    P(a, [(0, {}), (1.0, {'racine': (0, 0.7, 0)}), (2.0, {})], 'position', bones=['racine'])
    P(a, [(0, {'flamme_3': (0, 0, -6)}), (0.5, {'flamme_3': (4, 0, 4), 'flamme_2': (0, 6, 0)}), (1.0, {'flamme_3': (0, 0, -3)}),
          (1.5, {'flamme_3': (-3, 0, 6), 'flamme_2': (0, -6, 0)}), (2.0, {'flamme_3': (0, 0, -6)})])
    P(a, [(0, {}), (0.5, {'flamme_2': (1.08, 0.94, 1.08), 'flamme_1': (0.95, 1.1, 0.95)}), (1.0, {}),
          (1.5, {'flamme_2': (0.95, 1.06, 0.95), 'flamme_1': (1.08, 0.92, 1.08)}), (2.0, {})], 'scale', bones=['flamme_1', 'flamme_2'])
    a.scale('visage', [(0, 1, 1, 1, L), (1.5, 1, 1, 1, L), (1.57, 1, 0.15, 1, L), (1.66, 1, 1, 1, L), (2.0, 1, 1, 1, L)])

    # --- deplacement incline (1 s) : penche vers l'avant, flammes qui trainent
    a = m.anim('deplacement', 1.0, 'loop')
    P(a, [(0, {}), (0.5, {'racine': (0, 0.5, 0)}), (1.0, {})], 'position', bones=['racine'])
    tilt = {'racine': (-14, 0, 0), 'flamme_3': (26, 0, 0), 'flamme_2': (10, 0, 0), 'flamme_1': (-6, 0, 0)}
    P(a, [(0, tilt), (0.5, M(tilt, {'flamme_3': (8, 0, 4), 'flamme_2': (3, 0, 0)})), (1.0, tilt)])

    # --- reaction joyeuse (1,2 s) : petits bonds, pirouette, yeux plisses, flammes qui s'embrasent
    a = m.anim('joie', 1.2, 'once')
    P(a, [(0, {}), (0.3, {'racine': (0, 3, 0)}), (0.55, {}), (0.8, {'racine': (0, 1.5, 0)}), (1.0, {}), (1.2, {})], 'position', bones=['racine'])
    P(a, [(0, {}), (0.1, {}), (0.55, {'racine': (0, 360, 0)}, L), (0.8, {'racine': (0, 360, 0), 'flamme_3': (0, 0, 12)}),
          (1.0, {'racine': (0, 360, 0), 'flamme_3': (0, 0, -8)}), (1.2, {'racine': (0, 360, 0)})])
    P(a, [(0, {}), (0.12, {'noyau': (1.15, 0.85, 1.15)}), (0.3, {'noyau': (0.9, 1.12, 0.9), 'flamme_3': SC(1.3), 'flamme_2': SC(1.2)}),
          (0.55, {'noyau': (1.12, 0.88, 1.12)}), (0.8, {'flamme_3': SC(1.2)}), (1.0, {'noyau': (1.08, 0.92, 1.08)}), (1.2, {})], 'scale',
      bones=['noyau', 'flamme_2', 'flamme_3'])
    a.scale('visage', [(0, 1, 1, 1, L), (0.1, 1, 0.4, 1, L), (1.0, 1, 0.4, 1, L), (1.2, 1, 1, 1, L)])

    # --- canalisation (1,6 s, en boucle) : les flammes se resserrent et tournent autour du noyau, le noyau pulse
    a = m.anim('canalisation', 1.6, 'loop')
    a.rot('flamme_2', [(0, 0, -20, 0), (0.8, 0, 20, 0), (1.6, 0, -20, 0)])
    a.rot('flamme_1', [(0, 0, 15, 0), (0.8, 0, -15, 0), (1.6, 0, 15, 0)])
    P(a, [(0, {'flamme_3': (0, 0, -4)}), (0.4, {'flamme_3': (0, 0, 4)}), (0.8, {'flamme_3': (0, 0, -4)}), (1.2, {'flamme_3': (0, 0, 4)}),
          (1.6, {'flamme_3': (0, 0, -4)})], bones=['flamme_3'])
    serre = {'flamme_1': (0.92, 1, 0.92), 'flamme_2': (0.9, 1.05, 0.9), 'flamme_3': SC(0.9)}
    P(a, [(0, serre), (0.4, dict(serre, noyau=SC(1.07))), (0.8, serre), (1.2, dict(serre, noyau=SC(1.07))), (1.6, serre)], 'scale')
    P(a, [(0, {'flamme_1': (0, 0.8, 0), 'flamme_3': (0, -1, 0), 'racine': (0, 0.5, 0)}), (0.8, {'flamme_1': (0, 0.8, 0), 'flamme_3': (0, -1.2, 0), 'racine': (0, 0.8, 0)}),
          (1.6, {'flamme_1': (0, 0.8, 0), 'flamme_3': (0, -1, 0), 'racine': (0, 0.5, 0)})], 'position')
