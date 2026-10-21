# -*- coding: utf-8 -*-
"""Maitre Archer : cape a capuche vert foret, cuir clair, carquois garni de fleches, grand arc long tenu dans la main gauche,
brassards de cuir. Yeux : vert pale lumineux ; la corde de l'arc et les plumes des fleches captent la lumiere."""
import classes_common as cc
import entity_kit as ek
from classes_common import pal, Mat, Decal

KEY = 'maitre_archer'
TITLE = 'Maître Archer'
ENTITY_CLASS = 'MaitreArcherEntity'
VISIBLE_BOX = (0.8, 2.2, 0)

P_VERT = pal('#14301a', '#1f4a28', '#2e6a3a', '#43904e', '#68b872')
P_CUIR = pal('#3a2410', '#563618', '#74502a', '#96703e', '#b89258')
P_BOIS = pal('#3a2410', '#563618', '#74502a', '#96703e')
VERT_PALE = (170, 255, 150)


def build():
    m = ek.Model(KEY, TITLE)
    vert = cc.cloth(P_VERT, 0.5)
    cuir = Mat(P_CUIR, level=0.5, noise=0.25, scale=1.5, grad=0.2)
    cc.humanoid(m, torso=cuir, arms=vert, legs=cc.cloth(pal('#2a2218', '#3c3224', '#54462e'), 0.5), boots=cc.leather(), face_deco=cc.face(VERT_PALE))
    co = m.bones['corps']
    m.cube(co, 'gilet', (-4.4, 13, -2.4), (9, 8, 5), vert)
    m.cube(co, 'ceinture', (-4.6, 12, -2.6), (9, 2, 5), cc.leather(0.4))
    m.cube(co, 'boucle', (-1, 12.2, -3.1), (2, 2, 1), cc.trim())
    for bone, x, sx in ((m.bones['bras_droit'], 3.9, 1), (m.bones['bras_gauche'], -8.1, -1)):
        m.cube(bone, f'brassard_{sx}', (x, 12, -2.2), (4, 5, 4), cuir, inflate=0.2, share='brassard', mirror=sx > 0)
    t = m.bones['tete']
    # capuche ouverte devant : dessus, dos et cotes
    m.cube(t, 'capuche_haut', (-4.5, 31.5, -4.5), (9, 2, 9), vert)
    m.cube(t, 'capuche_dos', (-4.5, 23, 3.5), (9, 9, 2), vert)
    m.cube(t, 'capuche_cote_d', (4, 23.5, -4), (1, 8, 8), vert, share='cote')
    m.cube(t, 'capuche_cote_g', (-5, 23.5, -4), (1, 8, 8), vert, share='cote', mirror=True)
    m.cube(t, 'capuche_bord', (-4.5, 31, -5), (9, 1, 1), cc.trim(pal('#2e6a3a', '#43904e', '#68b872', '#8ad494')))
    m.cube(t, 'capuche_pointe', (-1.5, 33, -1.5), (3, 2, 5), vert, inflate=0.0)
    cape = m.bone('cape', (0, 23, 2), co)
    m.cube(cape, 'cape_dos', (-4.5, 6, 2), (9, 17, 1), vert)
    m.cube(cape, 'cape_pointes', (-3.5, 3, 2.1), (7, 3, 1), vert)
    # carquois et fleches
    q = m.bone('carquois', (-2, 20, 3), co, rot=(0, 0, -12))
    m.cube(q, 'carquois_corps', (-4, 8, 3), (4, 14, 3), cuir)
    for i, dx in enumerate((-3.5, -2.5, -1.5)):
        m.cube(q, f'fleche_{i}', (dx, 22, 3.5 + 0.4 * i), (1, 4, 1), Mat(pal('#50a850', '#70d070', '#a8f0a0'), level=0.6, noise=0.2, glow=True))
        m.cube(q, f'hampe_{i}', (dx + 0.25, 20, 3.7 + 0.4 * i), (0.5 if False else 1, 3, 1), cc.leather(0.5), inflate=-0.25)
    # arc long (main gauche) : trois segments qui forment l'arc ; corde lumineuse
    bg = m.bones['bras_gauche']
    arme = m.bone('arme', (-6, 12, -2), bg)
    bois = Mat(P_BOIS, level=0.55, noise=0.25, scale=(1, 2), grad=0.2)
    m.cube(arme, 'arc_milieu', (-7, 8, -4), (1, 8, 2), bois)
    m.cube(arme, 'arc_haut_1', (-6.5, 16, -3), (1, 7, 2), bois, rot=(-14, 0, 0), origin=(-6.5, 16, -3))
    m.cube(arme, 'arc_haut_2', (-6, 22, -1.2), (1, 6, 2), bois, rot=(-30, 0, 0), origin=(-6, 22, -1.2))
    m.cube(arme, 'arc_bas_1', (-6.5, 1, -3), (1, 7, 2), bois, rot=(14, 0, 0), origin=(-6.5, 8, -3))
    m.cube(arme, 'arc_bas_2', (-6, -4, -1.2), (1, 6, 2), bois, rot=(30, 0, 0), origin=(-6, 2, -1.2))
    m.cube(arme, 'corde', (-5.5, -2, 0.3), (0.5 if False else 1, 28, 0), Decal(lambda f: [f.px(x, y, (230, 255, 230), glow=True) for y in range(f.h) for x in range(f.w)]))
    return m


COLORS = cc.COLORS


def animations(m):
    P = ek.key_poses

    def extra(a, length):
        q = length / 4.0
        a.rot('cape', [(0, 0, 0, 0), (q, 4, 0, 2), (2 * q, 0, 0, 0), (3 * q, 4, 0, -2), (length, 0, 0, 0)])
        a.rot('carquois', [(0, 0, 0, 0), (q * 2, 1.5, 0, 0), (length, 0, 0, 0)])
    cc.base_idle(m, extra)
    # presentation : bande l'arc (bras gauche tendu, bras droit tire la corde)
    a = m.anim('present', 2.4, 'once')
    P(a, [(0, {}), (0.5, {'bras_gauche': (80, 0, -4), 'bras_droit': (60, -25, 0), 'tete': (0, 12, 0), 'corps': (0, -10, 0)}),
          (1.4, {'bras_gauche': (90, 0, -4), 'bras_droit': (75, -35, 0), 'tete': (0, 12, 0), 'corps': (0, -12, 0)}),
          (2.4, {})])
    a.rot('arme', [(0, 0, 0, 0), (0.5, 0, 0, 0), (1.4, 0, 0, 0), (2.4, 0, 0, 0)])
