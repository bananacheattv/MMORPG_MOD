# -*- coding: utf-8 -*-
"""Grande pancarte des quetes hebdomadaires d'Aeloria (5 x 2 blocs, 6 de haut). Deux hauts poteaux de bois sombre portent un
immense panneau cercle d'argent sous une corniche doree ; trois etendards pendent du panneau (vert = facile, ambre = difficile,
rouge = tres difficile, 1 a 3 etoiles d'or) ; un cristal violet couronne l'ensemble. Le bloc fonctionnel (ouvre l'interface des
quetes hebdomadaires) est le centre du panneau."""
import entity_kit as ek
from structure_common import (PIERRE, ARGENT, RUNE, BOIS, CRISTAL_V, Masonry, silver, crystal, trim, rune_panel, base_panels)
from entity_kit import Mat, pal

KEY = 'quetes_hebdo'
TITLE = 'Pancarte des quêtes hebdomadaires'
ENTITY_CLASS = 'QuetesHebdo'
JAVA = 'QuetesHebdo'
VISIBLE_BOX = (5, 5, 0)
VIEW_SCALE = 5.0
SHEET_SCALE = 3.2
GIF_SCALE = 3.0
SHEET_YAW = 35
NO_COLLISION = ('cristal', 'chaine', 'etendard', 'lanterne', 'parchemin', 'clou')
STATES = {'inactif': 'repos', 'activation': 'ouverture', 'actif': 'active'}

OR = pal('#5a3a06', '#9a6a0c', '#d9a21a', '#ffd34a', '#fff1a0')
VERT = pal('#0e2a14', '#1c5226', '#2c8a3c', '#4cc05c', '#9af0a6')
AMBRE = pal('#3a1c04', '#7a3e0a', '#c26a14', '#f09a30', '#ffd080')
ROUGE = pal('#2a0a0a', '#561212', '#8a1c1c', '#c02c2c', '#f06a6a')
PARCHEMIN = pal('#6a5a3a', '#a08c5c', '#d8c890', '#efe3b8', '#fff8dc')
STONE = Masonry(PIERRE, 0.42, decos=[base_panels()])
STONE_TOP = Masonry(PIERRE, 0.5, seed=1)
DARK = Masonry(PIERRE, 0.3, bw=6, bh=3, seed=2)
SILVER = silver()
SILVER_RIV = silver(rivet=True)
GOLD = Mat(OR, level=0.6, noise=0.2, scale=1.3, grad=0.3, up=0.25, down=-0.2, rim=0.2)
WOOD = Mat(BOIS, level=0.5, noise=0.3, scale=(4, 1), grad=0.1)
WOOD_DARK = Mat(BOIS, level=0.3, noise=0.3, scale=(4, 1), grad=0.1)
PAPER = Mat(PARCHEMIN, level=0.6, noise=0.2, scale=1.5, grad=0.15, decos=[ek.scratches(2, PARCHEMIN[1], (2, 4))])
CRYS = crystal(CRISTAL_V, level=0.6)


def stars(n, color):
    """n petites etoiles d'or lumineuses, empilees au centre de la face avant."""
    def d(f):
        if f.name != 'north' or f.w < 9 or f.h < 8 * n:
            return
        cx = f.w // 2
        for i in range(n):
            cy = int(f.h / 2 + (i - (n - 1) / 2) * 8)
            for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1), (0, -2), (0, 2), (-2, 0), (2, 0)):
                f.px(cx + dx, cy + dy, color, glow=True)
            f.px(cx, cy, OR[4], glow=True)
    return d


def banner(palette, n):
    return Mat(palette, level=0.55, noise=0.25, scale=1.5, grad=0.2, decos=[ek.border(OR[3], 'tblr', 1), stars(n, OR[3])])


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))

    p = m.bone('plateforme', (0, 0, 0), 'racine')
    m.cube(p, 'dalle', (-44, 0, -14), (88, 3, 28), STONE)
    m.cube(p, 'marche', (-20, 0, -18), (40, 2, 4), STONE_TOP)
    for nom, x in (('g', 30), ('d', -38)):
        m.cube(p, f'socle_{nom}', (x, 3, -6), (8, 6, 12), DARK, share='socle', deco={'sides': [rune_panel(1, margin=1), trim('lrt')]})

    po = m.bone('poteaux', (0, 3, 0), 'racine')
    for nom, x in (('g', 31), ('d', -37)):
        m.cube(po, f'poteau_{nom}', (x, 9, -3), (6, 64, 6), WOOD_DARK, share='poteau', deco={'sides': [trim('tb', OR[3])]})
        m.cube(po, f'poteau_{nom}_bague_b', (x - 1, 14, -4), (8, 3, 8), GOLD, share='bague')
        m.cube(po, f'poteau_{nom}_bague_h', (x - 1, 62, -4), (8, 3, 8), GOLD, share='bague')
        m.cube(po, f'poteau_{nom}_chapeau', (x - 1, 73, -4), (8, 3, 8), GOLD, share='chapeau')
    ln = m.bone('lanternes', (0, 78, 0), 'racine')
    for nom, x in (('g', 33), ('d', -35)):
        m.cube(ln, f'lanterne_{nom}', (x, 76, -1), (2, 4, 2), CRYS, share='lanterne')

    # ------------------------------------------------------------ le grand panneau (bloc fonctionnel au centre)
    pa = m.bone('panneau', (0, 44, 0), 'racine')
    m.cube(pa, 'panneau_planche', (-32, 20, -2), (64, 46, 4), WOOD_DARK)
    m.cube(pa, 'cadre_haut', (-35, 66, -3), (70, 4, 6), SILVER_RIV)
    m.cube(pa, 'cadre_bas', (-35, 17, -3), (70, 3, 6), SILVER_RIV)
    m.cube(pa, 'cadre_g', (32, 20, -3), (3, 46, 6), SILVER_RIV, share='cadre')
    m.cube(pa, 'cadre_d', (-35, 20, -3), (3, 46, 6), SILVER_RIV, share='cadre')
    m.cube(pa, 'corniche', (-40, 70, -6), (80, 3, 12), WOOD_DARK)
    m.cube(pa, 'corniche_or', (-40, 73, -6), (80, 1, 12), GOLD)
    m.cube(pa, 'faite', (-26, 74, -4), (52, 3, 8), WOOD)
    m.cube(pa, 'faite_or', (-26, 77, -4), (52, 1, 8), GOLD)
    m.cube(pa, 'plaque_titre', (-12, 60, -4), (24, 5, 2), GOLD)
    cr = m.bone('cristal', (0, 78, 0), 'racine')
    m.cube(cr, 'cristal_socle', (-4, 78, -4), (8, 1, 8), GOLD)
    m.cube(cr, 'cristal_corps', (-3, 79, -3), (6, 8, 6), CRYS)
    m.cube(cr, 'cristal_pointe', (-2, 87, -2), (4, 3, 4), CRYS)

    # ------------------------------------------------------------ parchemins
    pc = m.bone('parchemins', (0, 60, -3), 'racine')
    k = 0
    for x in (-30, -22, 14, 22):
        m.cube(pc, f'parchemin_{k}', (x, 24, -3), (7, 9, 1), PAPER, share='parchemin',
               rot=(0, 0, (-5, 4, -3, 6)[k % 4]), origin=(x + 3, 33, -3))
        m.cube(pc, f'clou_{k}', (x + 3, 32, -4), (1, 1, 1), GOLD, share='clou')
        k += 1

    # ------------------------------------------------------------ trois etendards : facile, difficile, tres difficile
    for nom, x, pal_, n in (('facile', 13, VERT, 1), ('difficile', -10, AMBRE, 2), ('tres_difficile', -33, ROUGE, 3)):
        et = m.bone(f'etendard_{nom}', (x + 10, 62, -4), 'racine')
        m.cube(et, f'etendard_{nom}_barre', (x - 1, 62, -5), (22, 2, 2), GOLD, share='barre')
        m.cube(et, f'etendard_{nom}_tissu', (x + 1, 28, -5), (18, 34, 1), banner(pal_, n))
        m.cube(et, f'etendard_{nom}_pointe', (x + 5, 24, -5), (10, 4, 1), banner(pal_, 0), share=f'pointe_{nom}')
        m.cube(et, f'chaine_{nom}_g', (x + 3, 62, -4), (1, 2, 1), GOLD, share='chaine')
        m.cube(et, f'chaine_{nom}_d', (x + 16, 62, -4), (1, 2, 1), GOLD, share='chaine')
    return m


COLORS = {'racine': 0, 'plateforme': 1, 'poteaux': 3, 'lanternes': 6, 'panneau': 5, 'cristal': 6, 'parchemins': 4,
          'etendard_facile': 2, 'etendard_difficile': 4, 'etendard_tres_difficile': 6}


def animations(m):
    L = 'linear'
    bannieres = ('etendard_facile', 'etendard_difficile', 'etendard_tres_difficile')

    a = m.anim('repos', 4.0, 'loop')
    for i, b in enumerate(bannieres):
        a.rot(b, [(0, 0, 0, -1.2 + 0.4 * i, L), (2.0, 0, 0, 1.2 - 0.4 * i, L), (4.0, 0, 0, -1.2 + 0.4 * i, L)])
    a.pos('cristal', [(0, 0, 0, 0, L), (2.0, 0, 1.0, 0, L), (4.0, 0, 0, 0, L)])

    a = m.anim('ouverture', 1.0, 'once')
    for i, b in enumerate(bannieres):
        a.rot(b, [(0, 0, 0, 0, L), (0.3 + 0.1 * i, 0, 0, 10, L), (0.7, 0, 0, -6, L), (1.0, 0, 0, 0, L)])
    a.pos('cristal', [(0, 0, 0, 0, L), (1.0, 0, 2.0, 0, L)])

    a = m.anim('active', 2.0, 'loop')
    for i, b in enumerate(bannieres):
        a.rot(b, [(0, 0, 0, 0, L), (0.5 + 0.1 * i, 0, 0, 5, L), (1.5 + 0.1 * i, 0, 0, -5, L), (2.0, 0, 0, 0, L)])
    a.rot('cristal', [(0, 0, 0, 0, L), (2.0, 0, 180, 0, L)])
    a.pos('cristal', [(0, 0, 2.0, 0, L), (1.0, 0, 3.0, 0, L), (2.0, 0, 2.0, 0, L)])
