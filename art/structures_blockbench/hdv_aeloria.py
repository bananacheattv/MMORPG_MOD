# -*- coding: utf-8 -*-
"""Hotel des ventes d'Aeloria : estrade de commissaire-priseur (5 x 3 blocs). Un pupitre de bois sombre sur une estrade de
pierre, un marteau de vente et une cloche d'or suspendue ; a l'arriere, un grand tableau d'annonces couvert de parchemins sous une
corniche d'argent ; de part et d'autre, deux piedestaux ou flottent des objets de cristal ; des bannieres rouge et or pendent
des montants. Le bloc fonctionnel (ouvre l'interface des enchères) est la base du pupitre."""
import entity_kit as ek
from structure_common import (PIERRE, ARGENT, RUNE, BOIS, FER, CRISTAL_V, Masonry, silver, crystal, trim, floor_lines, rune_panel,
                              base_panels)
from entity_kit import Mat, pal

KEY = 'hdv_aeloria'
TITLE = 'Hôtel des ventes'
ENTITY_CLASS = 'HdvAeloria'
JAVA = 'HdvAeloria'
VISIBLE_BOX = (5, 4, 0)
VIEW_SCALE = 5.0
SHEET_SCALE = 3.2
GIF_SCALE = 3.0
SHEET_YAW = 35
NO_COLLISION = ('objets', 'cloche', 'marteau', 'parchemins', 'bannieres')
STATES = {'inactif': 'repos', 'activation': 'ouverture', 'actif': 'active'}

OR = pal('#5a3a06', '#9a6a0c', '#d9a21a', '#ffd34a', '#fff1a0')
ROUGE = pal('#2a0a0a', '#561212', '#8a1c1c', '#b82c2c', '#e05a5a')
PARCHEMIN = pal('#6a5a3a', '#a08c5c', '#d8c890', '#efe3b8', '#fff8dc')
STONE = Masonry(PIERRE, 0.42, decos=[base_panels()])
STONE_TOP = Masonry(PIERRE, 0.5, seed=1)
WALL = Masonry(PIERRE, 0.4, seed=4)
DARK = Masonry(PIERRE, 0.3, bw=6, bh=3, seed=2)
SILVER = silver()
SILVER_RIV = silver(rivet=True)
GOLD = Mat(OR, level=0.6, noise=0.2, scale=1.3, grad=0.3, up=0.25, down=-0.2, rim=0.2)
WOOD = Mat(BOIS, level=0.5, noise=0.3, scale=(4, 1), grad=0.1, decos=[lambda f: [f.px(x, y, BOIS[0]) for y in range(f.h)
                                                                                  for x in range(f.w) if f.side and x % 5 == 4]])
WOOD_DARK = Mat(BOIS, level=0.3, noise=0.3, scale=(4, 1), grad=0.1)
CLOTH = Mat(ROUGE, level=0.55, noise=0.25, scale=1.5, grad=0.2, decos=[ek.border(OR[3], 'tb', 1)])
PAPER = Mat(PARCHEMIN, level=0.6, noise=0.2, scale=1.5, grad=0.15, decos=[ek.scratches(2, PARCHEMIN[1], (2, 4))])
CRYS = crystal(CRISTAL_V, level=0.6)
LINES = [(-34, 18, 34, 18), (-34, -12, 34, -12), (0, -12, 0, 18)]


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))

    p = m.bone('plateforme', (0, 0, 0), 'racine')
    m.cube(p, 'dalle', (-48, 0, -20), (96, 4, 56), STONE, deco={'up': [floor_lines(LINES)]})
    m.cube(p, 'marche', (-24, 0, -26), (48, 2, 6), STONE_TOP)
    m.cube(p, 'borne_gauche', (36, 0, -22), (8, 10, 8), DARK, share='borne', deco={'sides': [rune_panel(1, margin=1), trim('lrt')]})
    m.cube(p, 'borne_droite', (-44, 0, -22), (8, 10, 8), DARK, share='borne', deco={'sides': [rune_panel(1, margin=1), trim('lrt')]})

    # ------------------------------------------------------------ estrade, pupitre (bloc fonctionnel) et marteau
    es = m.bone('estrade', (0, 4, 0), 'racine')
    m.cube(es, 'estrade_base', (-26, 4, -10), (52, 6, 24), DARK, deco={'north': [rune_panel(2, margin=1)], 'sides': [trim('lr')]})
    m.cube(es, 'estrade_dessus', (-28, 10, -12), (56, 2, 28), SILVER_RIV)
    m.cube(es, 'pupitre_corps', (-7, 12, -3), (14, 12, 8), WOOD_DARK, deco={'sides': [trim('tb', OR[3])]})
    m.cube(es, 'pupitre_pente', (-8, 24, -4), (16, 2, 10), WOOD, rot=(-22.5, 0, 0), origin=(0, 24, 0))
    m.cube(es, 'pupitre_rebord', (-8, 22, -5), (16, 2, 2), GOLD)
    m.cube(es, 'pupitre_ornement', (-3, 14, -4), (6, 6, 1), GOLD)
    mt = m.bone('marteau', (6, 26, 2), 'racine')
    m.cube(mt, 'marteau_manche', (3, 26, 1), (7, 1, 1), WOOD)
    m.cube(mt, 'marteau_tete', (9, 25, 0), (3, 3, 3), WOOD_DARK)
    m.cube(mt, 'marteau_bague', (8, 25, 0), (1, 3, 3), GOLD)
    m.cube(es, 'billot', (3, 24, 3), (5, 2, 5), GOLD)

    # ------------------------------------------------------------ portique, banniere et cloche
    po = m.bone('portique', (0, 12, 10), 'racine')
    for nom, x in (('g', 40), ('d', -44)):
        m.cube(po, f'montant_{nom}', (x, 4, 6), (4, 54, 4), WOOD_DARK, share='montant')
        m.cube(po, f'montant_{nom}_bague', (x - 1, 24, 5), (6, 3, 6), GOLD, share='montant_bague')
    m.cube(po, 'traverse', (-46, 58, 5), (96, 4, 6), WOOD_DARK)
    m.cube(po, 'traverse_or', (-46, 62, 5), (96, 1, 6), GOLD)
    for nom, x in (('g', 14), ('d', -20)):
        m.cube(po, f'poteau_cloche_{nom}', (x, 46, 6), (3, 12, 3), WOOD_DARK, share='poteau')
    ba = m.bone('bannieres', (0, 56, 8), 'racine')
    for nom, x in (('g', 30), ('d', -36)):
        m.cube(ba, f'banniere_{nom}', (x, 26, 4), (8, 30, 1), CLOTH, share='banniere')
        m.cube(ba, f'banniere_{nom}_pointe', (x + 2, 22, 4), (4, 4, 1), CLOTH, share='banniere_pointe')
        m.cube(ba, f'banniere_{nom}_embleme', (x + 2, 38, 3), (4, 4, 1), GOLD, share='embleme')
    cl = m.bone('cloche', (-2, 50, 8), 'racine')
    m.cube(cl, 'cloche_anse', (-3, 51, 7), (3, 3, 3), GOLD)
    m.cube(cl, 'cloche_haut', (-6, 44, 5), (9, 7, 7), GOLD)
    m.cube(cl, 'cloche_corps', (-8, 38, 3), (13, 6, 11), GOLD)
    m.cube(cl, 'cloche_bord', (-9, 36, 2), (15, 2, 13), GOLD)
    m.cube(cl, 'cloche_battant', (-2, 33, 7), (3, 4, 3), Mat(OR, level=0.35, noise=0.2, scale=1.3))

    # ------------------------------------------------------------ tableau d'annonces
    ta = m.bone('tableau', (0, 22, 26), 'racine')
    m.cube(ta, 'tableau_planche', (-40, 14, 24), (80, 40, 3), WOOD_DARK)
    m.cube(ta, 'tableau_cadre_haut', (-42, 54, 22), (84, 4, 6), SILVER_RIV)
    m.cube(ta, 'tableau_cadre_bas', (-42, 12, 22), (84, 3, 6), SILVER_RIV)
    m.cube(ta, 'tableau_cadre_g', (38, 15, 22), (4, 39, 6), SILVER_RIV, share='tableau_cadre')
    m.cube(ta, 'tableau_cadre_d', (-42, 15, 22), (4, 39, 6), SILVER_RIV, share='tableau_cadre')
    pa = m.bone('parchemins', (0, 34, 24), 'racine')
    k = 0
    for row, y in enumerate((38, 24)):
        for col in range(6):
            x = -34 + col * 12
            m.cube(pa, f'parchemin_{k}', (x, y, 21), (9, 11, 1), PAPER, share='parchemin', rot=(0, 0, (-6, 4, -2, 6, -4, 2)[(col + row) % 6]),
                   origin=(x + 4, y + 10, 21))
            m.cube(pa, f'clou_{k}', (x + 4, y + 9, 20), (1, 1, 1), GOLD, share='clou')
            k += 1

    # ------------------------------------------------------------ piedestaux et objets flottants
    for nom, x in (('g', 30), ('d', -30)):
        pd = m.bone(f'piedestal_{nom}', (x, 4, 8), 'racine')
        m.cube(pd, f'piedestal_{nom}_socle', (x - 6, 12, 2), (12, 4, 12), DARK, share='pied_socle')
        m.cube(pd, f'piedestal_{nom}_fut', (x - 4, 16, 4), (8, 12, 8), STONE_TOP, share='pied_fut', deco={'sides': [rune_panel(0, margin=1)]})
        m.cube(pd, f'piedestal_{nom}_plateau', (x - 6, 28, 2), (12, 2, 12), SILVER_RIV, share='pied_plateau')
        ob = m.bone(f'objets_{nom}', (x, 34, 8), pd)
        m.cube(ob, f'objet_{nom}_cristal', (x - 3, 31, 5), (6, 10, 6), CRYS, share='objet_cristal')
        m.cube(ob, f'objet_{nom}_pointe', (x - 2, 41, 6), (4, 3, 4), CRYS, share='objet_pointe')
        m.cube(ob, f'objet_{nom}_eclat', (x + 3, 35, 7), (2, 5, 2), CRYS, rot=(0, 0, 22.5), origin=(x + 4, 35, 8), share='objet_eclat')
    return m


COLORS = {'racine': 0, 'plateforme': 1, 'estrade': 5, 'marteau': 4, 'portique': 3, 'bannieres': 6, 'cloche': 4, 'tableau': 3,
          'parchemins': 4, 'piedestal_g': 2, 'piedestal_d': 2, 'objets_g': 6, 'objets_d': 6}


def animations(m):
    P = ek.key_poses
    L = 'linear'

    a = m.anim('repos', 4.0, 'loop')
    a.rot('cloche', [(0, 0, 0, -1.5, L), (2.0, 0, 0, 1.5, L), (4.0, 0, 0, -1.5, L)])
    P(a, [(0, {}), (2.0, {'objets_g': (0, 0.8, 0), 'objets_d': (0, 0.8, 0)}), (4.0, {})], 'position', bones=['objets_g', 'objets_d'])

    a = m.anim('ouverture', 1.0, 'once')
    a.rot('marteau', [(0, 0, 0, 0, L), (0.3, 0, 0, 40, L), (0.5, 0, 0, -10, L), (1.0, 0, 0, 0, L)])
    a.rot('cloche', [(0, 0, 0, 0, L), (0.5, 0, 0, 14, L), (1.0, 0, 0, -8, L)])
    P(a, [(0, {}), (1.0, {'objets_g': (0, 2, 0), 'objets_d': (0, 2, 0)})], 'position', interp='ease', bones=['objets_g', 'objets_d'])

    a = m.anim('active', 2.0, 'loop')
    a.rot('marteau', [(0, 0, 0, 0, L), (0.8, 0, 0, 0, L), (1.0, 0, 0, 40, L), (1.2, 0, 0, -8, L), (1.4, 0, 0, 0, L), (2.0, 0, 0, 0, L)])
    a.rot('cloche', [(0, 0, 0, 0, L), (1.2, 0, 0, 12, L), (1.6, 0, 0, -9, L), (2.0, 0, 0, 0, L)])
    for b, ph in (('objets_g', 0), ('objets_d', 1)):
        a.rot(b, [(0, 0, 0 + ph * 45, 0, L), (2.0, 0, 180 + ph * 45, 0, L)])
    P(a, [(0, {'objets_g': (0, 2, 0), 'objets_d': (0, 2, 0)}), (1.0, {'objets_g': (0, 3, 0), 'objets_d': (0, 3, 0)}),
          (2.0, {'objets_g': (0, 2, 0), 'objets_d': (0, 2, 0)})], 'position', bones=['objets_g', 'objets_d'])
