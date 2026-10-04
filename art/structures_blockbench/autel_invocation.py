# -*- coding: utf-8 -*-
"""Autel d'invocation : plateforme carree en pierre noire (5 x 5 blocs) accessible par un escalier de 3 marches,
quatre piliers runiques aux angles (les piliers arriere plus hauts), chacun coiffe d'un cristal violet ; au centre,
un autel massif avec un cristal enchasse dans une cavite ; gravures lumineuses reliant les piliers au centre ;
renforts argentes. Le bloc fonctionnel (clic avec un objet d'invocation) est l'autel central."""
import entity_kit as ek
from structure_common import (PIERRE, ARGENT, CRISTAL_V, CRISTAL_B, Masonry, silver, crystal, rune_panel, trim,
                              floor_lines, base_panels)

KEY = 'autel_invocation'
TITLE = "Autel d'invocation"
ENTITY_CLASS = 'AutelInvocation'
JAVA = 'AutelInvocation'
VISIBLE_BOX = (5, 4.5, 0)
VIEW_SCALE = 5.0
SHEET_SCALE = 3.2
GIF_SCALE = 3.0
SHEET_YAW = 35
# os purement decoratifs : pas de collision (cristaux flottants)
NO_COLLISION = ('cristal',)
STATES = {'inactif': 'inactif', 'activation': 'activation', 'actif': 'actif'}

STONE = Masonry(PIERRE, 0.44, decos=[base_panels()])
STONE_TOP = Masonry(PIERRE, 0.52, seed=1)
DARK = Masonry(PIERRE, 0.32, bw=6, bh=3, seed=2)
SILVER = silver()
SILVER_RIV = silver(rivet=True)
CRYS = crystal(CRISTAL_V)
CRYS_B = crystal(CRISTAL_B, level=0.62)

PILIERS = {   # nom : (centre x, centre z, hauteur du fut bas, hauteur du fut haut)
    'pilier_arriere_gauche': (25, 25, 22, 12),
    'pilier_arriere_droit': (-25, 25, 22, 12),
    'pilier_avant_gauche': (25, -25, 14, 8),
    'pilier_avant_droit': (-25, -25, 14, 8),
}

# gravures du sol : anneau autour de l'autel, liaisons vers les piliers et vers les quatre cotes
SEGMENTS = [(-16, -16, 16, -16), (16, -16, 16, 16), (16, 16, -16, 16), (-16, 16, -16, -16),
            (16, 16, 19, 19), (-16, 16, -19, 19), (16, -16, 19, -19), (-16, -16, -19, -19),
            (0, -16, 0, -24), (0, 16, 0, 24), (16, 0, 24, 0), (-16, 0, -24, 0),
            (-3, -20, 3, -20), (-3, 20, 3, 20), (20, -3, 20, 3), (-20, -3, -20, 3)]


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))

    p = m.bone('plateforme', (0, 0, 0), 'racine')
    m.cube(p, 'dalle_arriere', (-40, 0, -28), (80, 8, 68), STONE)
    m.cube(p, 'dalle_avant_gauche', (18, 0, -40), (22, 8, 12), STONE)
    m.cube(p, 'dalle_avant_droite', (-40, 0, -40), (22, 8, 12), STONE)
    m.cube(p, 'rebord_arriere', (-32, 8, 32), (64, 2, 8), STONE_TOP)
    m.cube(p, 'rebord_gauche', (32, 8, -32), (8, 2, 64), STONE_TOP)
    m.cube(p, 'rebord_droit', (-40, 8, -32), (8, 2, 64), STONE_TOP)
    m.cube(p, 'rebord_avant_gauche', (18, 8, -40), (14, 2, 8), STONE_TOP)
    m.cube(p, 'rebord_avant_droit', (-32, 8, -40), (14, 2, 8), STONE_TOP)
    for nom, (x, z) in {'borne_avant_gauche': (32, -40), 'borne_avant_droite': (-40, -40),
                        'borne_arriere_gauche': (32, 32), 'borne_arriere_droite': (-40, 32)}.items():
        m.cube(p, nom, (x, 0, z), (8, 14, 8), DARK, share='borne', deco={'sides': [rune_panel(1, margin=1), trim('lrt')]})
    m.cube(p, 'sol_runique', (-26, 8, -24), (52, 1, 52), STONE_TOP, deco={'up': [floor_lines(SEGMENTS)]})

    mk = m.bone('marches', (0, 0, -34), 'racine')
    m.cube(mk, 'marche_1', (-12, 0, -40), (24, 2, 4), STONE)
    m.cube(mk, 'marche_2', (-12, 0, -36), (24, 4, 4), STONE)
    m.cube(mk, 'marche_3', (-12, 0, -32), (24, 6, 4), STONE)
    m.cube(mk, 'joue_gauche', (12, 0, -40), (6, 12, 14), DARK, share='joue', deco={'sides': [rune_panel(0, margin=1), trim('t')]})
    m.cube(mk, 'joue_droite', (-18, 0, -40), (6, 12, 14), DARK, share='joue', deco={'sides': [rune_panel(0, margin=1), trim('t')]})
    m.cube(mk, 'chapeau_joue_gauche', (11.5, 12, -40), (7, 2, 7), SILVER_RIV, share='chapeau_joue')
    m.cube(mk, 'chapeau_joue_droite', (-18.5, 12, -40), (7, 2, 7), SILVER_RIV, share='chapeau_joue')

    a = m.bone('autel', (0, 9, 0), 'racine')
    m.cube(a, 'socle_autel', (-13, 9, -13), (26, 2, 26), STONE_TOP, deco={'sides': [trim('t')]})
    m.cube(a, 'corps_autel', (-11, 11, -11), (22, 10, 22), DARK, deco={'sides': [rune_panel(1, margin=1)]})
    for nom, (x, z) in {'colonne_avant_gauche': (8, -12), 'colonne_avant_droite': (-11, -12),
                        'colonne_arriere_gauche': (8, 9), 'colonne_arriere_droite': (-11, 9)}.items():
        m.cube(a, nom, (x, 10, z), (3, 12, 3), SILVER, share='colonne')
    m.cube(a, 'couronne_avant', (-11, 21, -11), (22, 3, 5), SILVER_RIV, share='couronne_ab')
    m.cube(a, 'couronne_arriere', (-11, 21, 6), (22, 3, 5), SILVER_RIV, share='couronne_ab')
    m.cube(a, 'couronne_gauche', (6, 21, -6), (5, 3, 12), SILVER, share='couronne_gd')
    m.cube(a, 'couronne_droite', (-11, 21, -6), (5, 3, 12), SILVER, share='couronne_gd')
    m.cube(a, 'fond_cavite', (-6, 20.5, -6), (12, 1, 12), Masonry(PIERRE, 0.2, seed=3))
    c = m.bone('cristal_autel', (0, 21, 0), a)
    m.cube(c, 'cristal_autel_coeur', (-2, 21, -2), (4, 10, 4), CRYS_B)
    m.cube(c, 'cristal_autel_pointe', (-1, 31, -1), (2, 3, 2), CRYS_B)
    for i, (dx, dz, rx, rz) in enumerate(((-3, 0, 0, 22), (3, 0, 0, -22), (0, -3, -22, 0), (0, 3, 22, 0))):
        m.cube(c, f'cristal_autel_eclat_{i + 1}', (dx - 1, 21, dz - 1), (2, 6, 2), CRYS_B, share='eclat_autel',
               rot=(rx, 0, rz), origin=(dx, 21, dz))

    for nom, (cx, cz, h1, h2) in PILIERS.items():
        b = m.bone(nom, (cx, 8, cz), 'racine')
        y = 8
        m.cube(b, f'{nom}_base', (cx - 7, y, cz - 7), (14, 6, 14), STONE_TOP, share='pilier_base', deco={'sides': [trim('t')]})
        y += 6
        m.cube(b, f'{nom}_fut_bas', (cx - 6, y, cz - 6), (12, h1, 12), DARK,
               share=f'fut_bas_{h1}', deco={'sides': [rune_panel(0, margin=2), trim('lr')]})
        y += h1
        m.cube(b, f'{nom}_ressaut', (cx - 6.5, y, cz - 6.5), (13, 2, 13), SILVER, share='ressaut')
        y += 2
        m.cube(b, f'{nom}_fut_haut', (cx - 5, y, cz - 5), (10, h2, 10), DARK,
               share=f'fut_haut_{h2}', deco={'sides': [rune_panel(3, margin=1), trim('lr')]})
        y += h2
        m.cube(b, f'{nom}_chapiteau', (cx - 6, y, cz - 6), (12, 3, 12), SILVER_RIV, share='chapiteau')
        y += 3
        m.cube(b, f'{nom}_porte_cristal', (cx - 4, y, cz - 4), (8, 3, 8), DARK, share='porte_cristal')
        y += 3
        cb = m.bone(f'cristal_{nom[7:]}', (cx, y, cz), b)
        m.cube(cb, f'cristal_{nom[7:]}_coeur', (cx - 2, y, cz - 2), (4, 9, 4), CRYS, share='cristal_pilier')
        m.cube(cb, f'cristal_{nom[7:]}_pointe', (cx - 1, y + 9, cz - 1), (2, 3, 2), CRYS, share='cristal_pilier_pointe')
        m.cube(cb, f'cristal_{nom[7:]}_eclat_1', (cx - 4, y, cz - 1), (2, 5, 2), CRYS, share='eclat_pilier',
               rot=(0, 0, 20), origin=(cx - 3, y, cz))
        m.cube(cb, f'cristal_{nom[7:]}_eclat_2', (cx + 2, y, cz - 1), (2, 5, 2), CRYS, share='eclat_pilier',
               rot=(0, 0, -20), origin=(cx + 3, y, cz))
    return m


COLORS = {'racine': 0, 'plateforme': 1, 'marches': 2, 'autel': 4, 'cristal_autel': 6, 'pilier_arriere_gauche': 3,
          'pilier_arriere_droit': 3, 'pilier_avant_gauche': 5, 'pilier_avant_droit': 5}


def animations(m):
    P = ek.key_poses
    L = 'linear'
    piliers = [f'cristal_{n[7:]}' for n in PILIERS]

    # --- inactif (4 s) : les cristaux respirent a peine
    a = m.anim('inactif', 4.0, 'loop')
    P(a, [(0, {}), (2.0, dict({b: (0, 0.6, 0) for b in piliers}, cristal_autel=(0, 0.4, 0))), (4.0, {})], 'position',
      bones=piliers + ['cristal_autel'])

    # --- activation (1,5 s) : les cristaux s'elevent et se mettent a tourner, le cristal central sort de sa cavite
    a = m.anim('activation', 1.5, 'once')
    haut = dict({b: (0, 3, 0) for b in piliers}, cristal_autel=(0, 8, 0))
    P(a, [(0, {}), (1.5, haut)], 'position', interp='ease', bones=piliers + ['cristal_autel'])
    P(a, [(0, {}), (1.5, dict({b: (0, 180, 0) for b in piliers}, cristal_autel=(0, -180, 0)))], 'rotation', interp='ease',
      bones=piliers + ['cristal_autel'])
    P(a, [(0, {}), (1.5, {'cristal_autel': (1.15, 1.15, 1.15)})], 'scale', interp='ease', bones=['cristal_autel'])

    # --- actif (2 s, en boucle) : rotation continue, flottement, pulsation du cristal central
    a = m.anim('actif', 2.0, 'loop')
    for b in piliers:
        a.rot(b, [(0, 0, 180, 0, L), (2.0, 0, 540, 0, L)])
    a.rot('cristal_autel', [(0, 0, -180, 0, L), (2.0, 0, -540, 0, L)])
    P(a, [(0, haut), (1.0, dict({b: (0, 4, 0) for b in piliers}, cristal_autel=(0, 9, 0))), (2.0, haut)], 'position',
      bones=piliers + ['cristal_autel'])
    a.scale('cristal_autel', [(0, 1.15, 1.15, 1.15), (1.0, 1.25, 1.25, 1.25), (2.0, 1.15, 1.15, 1.15)])
