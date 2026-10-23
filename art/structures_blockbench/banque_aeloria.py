# -*- coding: utf-8 -*-
"""Banque d'Aeloria : coffre-fort monumental (5 x 3 blocs). Au fond, une porte de chambre forte ronde cerclee d'argent et d'or
avec son volant ; devant, un long comptoir de pierre noire a grilles de guichet, piles de lingots d'or, coffres cercles et
balance de changeur ; un cristal ambre eclaire le tout. Le bloc fonctionnel (ouvre l'interface bancaire) est le milieu du comptoir."""
import entity_kit as ek
from structure_common import (PIERRE, ARGENT, RUNE, BOIS, FER, Masonry, silver, trim, floor_lines, rune_panel, base_panels)
from entity_kit import Mat, pal

KEY = 'banque_aeloria'
TITLE = "Banque d'Aeloria"
ENTITY_CLASS = 'BanqueAeloria'
JAVA = 'BanqueAeloria'
VISIBLE_BOX = (5, 4, 0)
VIEW_SCALE = 5.0
SHEET_SCALE = 3.2
GIF_SCALE = 3.0
SHEET_YAW = 35
NO_COLLISION = ('lingots', 'cristal_ambre', 'balance', 'grilles')
STATES = {'inactif': 'repos', 'activation': 'ouverture', 'actif': 'active'}

OR = pal('#5a3a06', '#9a6a0c', '#d9a21a', '#ffd34a', '#fff1a0')
AMBRE = pal('#5a2b05', '#a85a0a', '#e8901c', '#ffc255', '#fff0c8')
STONE = Masonry(PIERRE, 0.42, decos=[base_panels()])
STONE_TOP = Masonry(PIERRE, 0.5, seed=1)
WALL = Masonry(PIERRE, 0.4, seed=4)
DARK = Masonry(PIERRE, 0.3, bw=6, bh=3, seed=2)
SILVER = silver()
SILVER_RIV = silver(rivet=True)
IRON = Mat(FER, level=0.5, noise=0.18, scale=1.5, grad=0.3, up=0.3, down=-0.2, rim=0.15)
IRON_DARK = Mat(FER, level=0.32, noise=0.18, scale=1.5, grad=0.25, up=0.2, down=-0.2, rim=0.1)
GOLD = Mat(OR, level=0.6, noise=0.2, scale=1.3, grad=0.3, up=0.25, down=-0.2, rim=0.2)
GOLD_GLOW = Mat(OR, level=0.7, noise=0.15, scale=1.2, grad=0.3, up=0.2, glow=True)
WOOD = Mat(BOIS, level=0.5, noise=0.3, scale=(4, 1), grad=0.1, decos=[lambda f: [f.px(x, y, BOIS[0]) for y in range(f.h)
                                                                                  for x in range(f.w) if f.side and x % 5 == 4]])
AMBER = Mat(AMBRE, level=0.6, noise=0.22, scale=1.2, grad=0.35, up=0.25, down=-0.15, glow=True)
LINES = [(-30, 20, 30, 20), (-30, -14, 30, -14), (0, -14, 0, 20)]


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))

    p = m.bone('plateforme', (0, 0, 0), 'racine')
    m.cube(p, 'dalle', (-48, 0, -20), (96, 4, 52), STONE, deco={'up': [floor_lines(LINES)]})
    m.cube(p, 'marche', (-24, 0, -26), (48, 2, 6), STONE_TOP)
    m.cube(p, 'borne_gauche', (36, 0, -22), (8, 10, 8), DARK, share='borne', deco={'sides': [rune_panel(1, margin=1), trim('lrt')]})
    m.cube(p, 'borne_droite', (-44, 0, -22), (8, 10, 8), DARK, share='borne', deco={'sides': [rune_panel(1, margin=1), trim('lrt')]})

    # ------------------------------------------------------------ mur de la chambre forte et porte ronde
    mu = m.bone('chambre_forte', (0, 4, 18), 'racine')
    m.cube(mu, 'mur_fond', (-48, 4, 20), (96, 44, 8), WALL, deco={'north': [rune_panel(0, margin=3)], 'sides': [trim('lr')]})
    m.cube(mu, 'corniche', (-50, 48, 19), (100, 3, 10), SILVER_RIV)
    m.cube(mu, 'frise', (-48, 44, 17), (96, 4, 3), DARK, deco={'sides': [trim('tb', OR[3])]})
    m.cube(mu, 'cadre_porte', (-20, 4, 15), (40, 40, 5), SILVER_RIV)
    m.cube(mu, 'cadre_or', (-17, 7, 14), (34, 34, 2), GOLD)
    m.cube(mu, 'porte_creux', (-14, 10, 13), (28, 28, 2), IRON_DARK)
    m.cube(mu, 'porte_carre', (-12, 12, 9), (24, 24, 4), IRON)
    m.cube(mu, 'porte_carre_tourne', (-12, 12, 9), (24, 24, 4), IRON_DARK, rot=(0, 0, 45), origin=(0, 24, 11))
    m.cube(mu, 'porte_anneau', (-14, 10, 8), (28, 3, 2), GOLD)
    m.cube(mu, 'porte_anneau_bas', (-14, 35, 8), (28, 3, 2), GOLD)
    m.cube(mu, 'pilier_gauche', (36, 4, 12), (8, 40, 8), SILVER_RIV, share='pilier')
    m.cube(mu, 'pilier_droit', (-44, 4, 12), (8, 40, 8), SILVER_RIV, share='pilier')
    vo = m.bone('volant', (0, 24, 7), mu)
    m.cube(vo, 'volant_moyeu', (-3, 21, 6), (6, 6, 4), GOLD)
    m.cube(vo, 'volant_barre_h', (-9, 23, 6), (18, 2, 2), GOLD)
    m.cube(vo, 'volant_barre_v', (-1, 15, 6), (2, 18, 2), GOLD)
    m.cube(vo, 'volant_diag_1', (-8, 23, 6), (16, 2, 2), GOLD, rot=(0, 0, 45), origin=(0, 24, 7), share='volant_d')
    m.cube(vo, 'volant_diag_2', (-8, 23, 6), (16, 2, 2), GOLD, rot=(0, 0, -45), origin=(0, 24, 7), share='volant_d')

    # ------------------------------------------------------------ comptoir (le bloc fonctionnel est au milieu)
    co = m.bone('comptoir', (0, 4, 0), 'racine')
    m.cube(co, 'comptoir_corps', (-36, 4, -8), (72, 12, 14), DARK,
           deco={'north': [rune_panel(2, margin=2)], 'sides': [trim('lr')]})
    m.cube(co, 'comptoir_plateau', (-38, 16, -10), (76, 3, 18), SILVER_RIV)
    m.cube(co, 'comptoir_lisere', (-38, 19, -10), (76, 1, 2), GOLD)
    m.cube(co, 'comptoir_pied_gauche', (30, 4, -10), (6, 12, 2), GOLD, share='pied_or')
    m.cube(co, 'comptoir_pied_droit', (-36, 4, -10), (6, 12, 2), GOLD, share='pied_or')
    # grilles de guichet : traverse haute et barreaux, trois fenetres
    gr = m.bone('grilles', (0, 20, 0), 'racine')
    m.cube(gr, 'grilles_traverse', (-38, 38, -8), (76, 4, 4), SILVER_RIV)
    for k in range(-5, 6):
        if k in (-3, 0, 3):
            continue
        m.cube(gr, f'barreau_{k + 5}', (k * 7 - 1, 20, -7), (2, 18, 2), IRON, share='barreau')
    for nom, x in (('g', 26), ('c', -2), ('d', -30)):
        m.cube(gr, f'montant_{nom}', (x, 20, -8), (4, 18, 4), SILVER_RIV, share='montant')

    # ------------------------------------------------------------ or, coffres et balance sur le comptoir
    li = m.bone('lingots', (0, 20, 0), 'racine')
    for i, (x, z) in enumerate(((-34, -6), (-34, 1), (-28, -6))):
        for h in range(3 - i % 2):
            m.cube(li, f'pile_g_{i}_{h}', (x - h * 0, 20 + h * 3, z + (h % 2)), (6, 3, 4), GOLD_GLOW, share='lingot')
    for i, (x, z) in enumerate(((26, -6), (32, -6), (29, 1))):
        for h in range(2 + i % 2):
            m.cube(li, f'pile_d_{i}_{h}', (x, 20 + h * 3, z), (6, 3, 4), GOLD_GLOW, share='lingot')
    m.cube(li, 'coffre_corps', (8, 20, -6), (12, 7, 8), WOOD, deco={'sides': [trim('tb', OR[3])]})
    m.cube(li, 'coffre_couvercle', (8, 27, -6), (12, 3, 8), WOOD)
    m.cube(li, 'coffre_serrure', (13, 24, -7), (2, 3, 1), GOLD)
    m.cube(li, 'coffre_or', (10, 30, -4), (8, 1, 4), GOLD_GLOW)
    m.cube(li, 'sac_or', (-18, 20, -4), (6, 7, 6), Mat(BOIS, level=0.65, noise=0.2, scale=2, grad=0.2), rot=(0, 20, 0), origin=(-15, 20, -1))

    ba = m.bone('balance', (-4, 36, 16), 'racine')
    m.cube(ba, 'balance_mat', (-5, 20, 14), (2, 18, 2), GOLD, share='mat')
    m.cube(ba, 'balance_socle', (-8, 20, 12), (8, 2, 6), GOLD)
    fl = m.bone('balance_fleau', (-4, 38, 15), ba)
    m.cube(fl, 'fleau', (-14, 37, 14), (20, 2, 2), GOLD)
    m.cube(fl, 'plateau_g', (-17, 28, 12), (6, 1, 6), GOLD, share='plateau')
    m.cube(fl, 'plateau_d', (3, 28, 12), (6, 1, 6), GOLD, share='plateau')
    m.cube(fl, 'chaine_g', (-14, 29, 15), (1, 8, 1), IRON, share='chaine')
    m.cube(fl, 'chaine_d', (6, 29, 15), (1, 8, 1), IRON, share='chaine')

    # ------------------------------------------------------------ cristal ambre au-dessus du comptoir
    ca = m.bone('cristal_ambre', (0, 52, 14), 'racine')
    m.cube(ca, 'cristal_support', (-2, 49, 12), (4, 3, 4), SILVER_RIV)
    m.cube(ca, 'cristal_coeur', (-3, 52, 11), (6, 14, 6), AMBER)
    m.cube(ca, 'cristal_pointe', (-2, 66, 12), (4, 4, 4), AMBER)
    m.cube(ca, 'cristal_base', (-2, 50, 12), (4, 2, 4), AMBER)
    return m


COLORS = {'racine': 0, 'plateforme': 1, 'chambre_forte': 2, 'volant': 4, 'comptoir': 5, 'grilles': 3, 'lingots': 4,
          'balance': 4, 'balance_fleau': 4, 'cristal_ambre': 6}


def animations(m):
    P = ek.key_poses
    L = 'linear'

    a = m.anim('repos', 4.0, 'loop')
    a.rot('balance_fleau', [(0, 0, 0, -3, L), (2.0, 0, 0, 3, L), (4.0, 0, 0, -3, L)])
    P(a, [(0, {}), (2.0, {'cristal_ambre': (0, 0.8, 0)}), (4.0, {})], 'position', bones=['cristal_ambre'])

    a = m.anim('ouverture', 1.0, 'once')
    a.rot('volant', [(0, 0, 0, 0, L), (1.0, 0, 0, 180, L)])
    P(a, [(0, {}), (1.0, {'cristal_ambre': (0, 2, 0)})], 'position', interp='ease', bones=['cristal_ambre'])
    a.rot('balance_fleau', [(0, 0, 0, -3, L), (1.0, 0, 0, 8, L)])

    a = m.anim('active', 2.0, 'loop')
    a.rot('volant', [(0, 0, 0, 180, L), (2.0, 0, 0, 540, L)])
    a.rot('balance_fleau', [(0, 0, 0, 8, L), (1.0, 0, 0, -8, L), (2.0, 0, 0, 8, L)])
    P(a, [(0, {'cristal_ambre': (0, 2, 0)}), (1.0, {'cristal_ambre': (0, 2.8, 0)}), (2.0, {'cristal_ambre': (0, 2, 0)})], 'position', bones=['cristal_ambre'])
    a.rot('cristal_ambre', [(0, 0, 0, 0, L), (2.0, 0, 90, 0, L)])
