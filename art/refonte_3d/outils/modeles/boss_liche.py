#!/usr/bin/env python3
"""Liche ancienne : robe pourpre en panneaux dechires, galons d'argent, cage thoracique ouverte sur un cristal d'ame, grande capuche a pointe, couronne d'argent, baton griffu. Meme squelette que art/boss_blockbench/liche_ancienne.py."""
import liche_ancienne as O
from boss_kit import B, P, ek
from entity_kit import border, rivets, stamp, tatter

ROBE = P('voile', 'pourpre', base=2.6, pli=6)
ROBE_C = P('voile', 'pourpre', base=3.2, pli=6)
ROBE_S = P('voile', 'pourpre', base=1.8, pli=6)
ARG = P('plaque', 'acier', base=3.2)
ARG_R = P('plaque', 'acier', base=3.2, rivets=3)
ARG_M = P('metal', 'acier', base=3.6)
OS = P('os', 'os', base=3.6)
CRIS = P('cristal', 'ame', base=2.4, glow=True, no_ao=True)
BOIS = P('ecorce', 'bois_sombre', ton=1, pas=2)
VIDE = P('uni', 'nuit', base=.8, no_ao=True)
Y_CORPS = (0, 55)
TRIM = lambda sides='lr': border((205, 213, 220), sides)


def build():
    m = ek.Model(O.KEY, O.TITLE)
    b = B(m)
    m.bone('racine', (0, 0, 0))
    t = m.bone('torse', (0, 22, 0), 'racine')
    # buste : dos et flancs de robe, poitrine ouverte, cotes, colonne
    b.c(t, 'robe_dos', (-8, 22, 1), (8, 36, 5), ROBE)
    b.c(t, 'robe_flanc_droit', (4, 22, -5), (8, 36, 1), ROBE_C, deco={'west': [O.trim(O.P_ARGENT[4], 'l')]})
    b.c(t, 'robe_flanc_gauche', (-8, 22, -5), (-4, 36, 1), ROBE_C, deco={'east': [O.trim(O.P_ARGENT[4], 'r')]})
    b.c(t, 'revers_droit', (3, 23, -5.5), (4, 35, -4.5), ARG)
    b.c(t, 'revers_gauche', (-4, 23, -5.5), (-3, 35, -4.5), ARG)
    b.c(t, 'cotes', (-4, 24, -4.5), (4, 35, 1.5), VIDE, deco={'north': [O.RIBS], 'east': [O.RIBS_SIDE], 'west': [O.RIBS_SIDE]})
    b.c(t, 'colonne', (-1, 21, -.5), (1, 24, 1.5), OS)
    b.c(t, 'ceinture', (-8.5, 20, -5.5), (8.5, 23, 5.5), P('plaque', 'pourpre', base=1.6), deco={'sides': [O.trim(O.P_ARGENT[3], 't'), O.runes_band]})
    b.c(t, 'boucle', (-2, 19.5, -6.5), (2, 23.5, -5.5), ARG, deco={'north': [stamp(['.cc.', 'cCCc', 'cCCc', '.cc.'], O.C, glow='cC')]})
    # mantelet large, col haut en deux lames, epingles d'argent
    b.c(t, 'mantelet', (-10, 35, -6), (10, 38, 6), ROBE_C, deco={'sides': [O.trim(O.P_ARGENT[4], 'b')]})
    b.c(t, 'col_d', (5, 37, -5), (9, 44, 5), ROBE_S, rot=(0, 0, -14), origin=(7, 37, 0), deco={'sides': [O.trim(O.P_ARGENT[4], 't')]}, share='col', mirror=False)
    b.c(t, 'col_g', (-9, 37, -5), (-5, 44, 5), ROBE_S, rot=(0, 0, 14), origin=(-7, 37, 0), deco={'sides': [O.trim(O.P_ARGENT[4], 't')]}, share='col', mirror=True)
    ri = m.bone('robe_interieure', (0, 22, 0), t)
    b.c(ri, 'jupon', (-7, 6, -5), (7, 22, 5), ROBE_S, deco={'*': [tatter(3, seed=2.2)]})
    ca = m.bone('cristal_ame', (0, 30, -5), t)
    m.cube(ca, 'cristal', (-2, 27, -7), (4, 6, 4), CRIS, rot=(0, 45, 0), origin=(0, 30, -5))
    m.cube(ca, 'cristal_pointe', (-1, 33, -6), (2, 3, 2), CRIS, rot=(0, 45, 0), origin=(0, 34.5, -5))
    m.cube(ca, 'cristal_bas', (-1, 25, -6), (2, 2, 2), CRIS, rot=(0, 45, 0), origin=(0, 26, -5))
    # tete : crane, machoire, capuche profonde, pointe rejetee en arriere
    h = m.bone('tete', (0, 37, -1), t)
    b.c(h, 'crane', (-4, 38, -5), (4, 46, 3), OS, deco={'north': [O.SKULL]})
    jw = m.bone('machoire', (0, 38, 1), h)
    b.c(jw, 'machoire_bas', (-3, 36, -5.5), (3, 38, .5), OS, deco={'north': [O.JAW]})
    b.c(h, 'capuche_haut', (-6, 46, -8), (6, 48, 5), ROBE_C, deco={'north': [O.trim(O.P_ROBE[1], 'b')]})
    b.c(h, 'capuche_droite', (4, 35, -8), (6, 46, 5), ROBE, deco={'north': [O.trim(O.P_ARGENT[4], 'l')], 'west': [O.trim(O.P_ROBE[0], 'tblr')]})
    b.c(h, 'capuche_gauche', (-6, 35, -8), (-4, 46, 5), ROBE, deco={'north': [O.trim(O.P_ARGENT[4], 'r')], 'east': [O.trim(O.P_ROBE[0], 'tblr')]})
    b.c(h, 'capuche_dos', (-4, 35, 3), (4, 46, 5), ROBE_S)
    b.c(h, 'capuche_pointe', (-3, 38, 5), (3, 47, 6), ROBE, deco={'*': [tatter(2, seed=.4)]})
    b.c(h, 'capuche_pointe2', (-2, 43, 6), (2, 49, 9), ROBE_C, rot=(-22, 0, 0), origin=(0, 43, 6))
    cr = m.bone('couronne', (0, 48, -1), h, rot=(0, 0, -4))
    b.c(cr, 'couronne_bandeau', (-6.5, 47.5, -8.5), (6.5, 49.5, 4.5), ARG, deco={'*': [O.runes_band]})
    for i, (x, y, z, w, hh, rz) in enumerate(((-1, 49.5, -9, 2, 6, 0), (-5.5, 49.5, -9, 2, 5, 12), (3.5, 49.5, -9, 2, 5, -12), (-6.5, 49.5, -4, 1, 5, 14), (5.5, 49.5, -4, 1, 5, -14), (-3, 49.5, 4, 1, 4, 0), (2, 49.5, 4, 1, 4, 0))):
        m.cube(cr, f'pointe_{i + 1}', (x, y, z), (w, hh, 1), ARG_M, rot=(0, 0, rz) if rz else None, origin=(x + w / 2, y, z + .5) if rz else None)
    b.c(cr, 'gemme_couronne', (-1.5, 50, -9.5), (1.5, 52, -8.5), CRIS)
    # bras : epaulieres d'argent a trois pointes, manches evasees, mains d'os
    for side, sx in (('droit', 1), ('gauche', -1)):
        hand = 'main_droite' if sx > 0 else 'main_gauche'
        bo = m.bone(f'bras_{side}', (8 * sx, 35, -1), t, rot=(0, 0, 4) if sx > 0 else (10, 0, -24))
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        m.cube(bo, f'epauliere_{side}', (xo(6, 7), 34, -6), (7, 4, 10), ARG_R, share='epauliere', mirror=sx < 0)
        m.cube(bo, f'epauliere_{side}_haut', (xo(8, 5), 38, -5), (5, 2, 8), ARG, share='epauliere_haut', mirror=sx < 0)
        for k, (x, z, hh) in enumerate(((9, -4, 5), (10, -1, 7), (9, 2, 5))):
            m.cube(bo, f'pique_{side}_{k + 1}', (xo(x, 2), 40, z), (2, hh, 2), ARG_M, share=f'pique_ep{k}', mirror=sx < 0, rot=(0, 0, -10 * sx), origin=(xo(x, 2) + 1, 40, z + 1))
        m.cube(bo, f'chaine_{side}', (xo(11, 1), 27, -6), (1, 7, 1), ARG, deco={'*': [O.chain]}, share='chaine', mirror=sx < 0)
        m.cube(bo, f'manche_haut_{side}', (xo(6.5, 5), 26, -4), (5, 9, 6), ROBE, share='manche_haut', mirror=sx < 0)
        fa = m.bone(f'avant_bras_{side}', (9 * sx, 27, -1), bo, rot=(35, 0, 0) if sx > 0 else (45, 0, 10))
        m.cube(fa, f'manche_bas_{side}', (xo(5, 8), 16, -5.5), (8, 11, 9), ROBE_C, share='manche_bas', mirror=sx < 0, deco={'sides': [O.trim(O.P_ARGENT[4], 'b'), tatter(2, seed=1.1)]})
        hd = m.bone(hand, (9 * sx, 18, -1), fa)
        m.cube(hd, f'paume_{side}', (xo(7.5, 3), 14.5, -2.5), (3, 4, 3), OS, share='paume', mirror=sx < 0)
        for k in range(3):
            spread = (0, 0, (k - 1) * 18 * sx) if sx < 0 else None
            m.cube(hd, f'doigt_{side}_{k + 1}', (xo(7.5 + k, 1), 11.5 + (k % 2), -2.2), (1, 3, 1), OS, share=f'doigt_{k % 2}', mirror=sx < 0, deco={'*': [O.fingers]}, rot=spread, origin=(xo(7.5 + k, 1) + .5, 14.5, -2) if spread else None)
        m.cube(hd, f'pouce_{side}', (xo(7.5, 1), 13, -3.5), (1, 2, 1), OS, share='pouce', mirror=sx < 0)
    # baton : hampe noueuse, griffes, grand cristal
    bt = m.bone('baton', (9, 16, -1), 'main_droite', rot=(-35, 0, 0))
    b.c(bt, 'hampe', (8, 1, -2), (10, 39, 0), BOIS, deco={'*': [O.shaft]})
    b.c(bt, 'ferrure', (7.5, -1, -2.5), (10.5, 2, .5), ARG)
    b.c(bt, 'noeud', (7.5, 20, -2.5), (10.5, 23, .5), BOIS)
    b.c(bt, 'collier', (7, 38, -3), (11, 40, 1), ARG, deco={'*': [O.runes_band]})
    for i, (dx, dz) in enumerate(((-1.5, -1.5), (1.5, -1.5), (-1.5, 1.5), (1.5, 1.5))):
        m.cube(bt, f'griffe_{i + 1}', (9 + dx - .5, 40, -1 + dz - .5), (1, 7, 1), ARG_M, rot=(dz * 10, 0, -dx * 10), origin=(9 + dx, 40, -1 + dz))
    cs = m.bone('cristal_baton', (9, 44, -1), bt)
    m.cube(cs, 'cristal_baton_corps', (7, 41, -3), (4, 8, 4), CRIS, rot=(0, 45, 0), origin=(9, 45, -1))
    m.cube(cs, 'cristal_baton_pointe', (8, 49, -2), (2, 4, 2), CRIS, rot=(0, 45, 0), origin=(9, 51, -1))
    # panneaux de robe, chacun sur son os, pivot a la taille
    panels = [
        ('robe_avant', (0, 22, -6), (6, 0, 0), (-4, 3, -7), (8, 19, 1), [TRIM(), O.cross]),
        ('robe_avant_droite', (6, 22, -5.5), (5, 0, 7), (3, 5, -6.5), (6, 17, 1), [TRIM('l')]),
        ('robe_avant_gauche', (-6, 22, -5.5), (5, 0, -7), (-9, 4, -6.5), (6, 18, 1), [TRIM('r')]),
        ('robe_droite', (7.5, 22, 0), (0, 0, 9), (7, 3, -5), (1, 19, 10), []),
        ('robe_gauche', (-7.5, 22, 0), (0, 0, -9), (-8, 4, -5), (1, 18, 10), []),
        ('robe_arriere', (0, 22, 5.5), (-6, 0, 0), (-7, 2, 5), (14, 20, 1), [TRIM(), O.sigil]),
        ('robe_arriere_droite', (6, 22, 5), (-5, 0, 8), (4, 4, 5.5), (5, 18, 1), []),
        ('robe_arriere_gauche', (-6, 22, 5), (-5, 0, -8), (-9, 5, 5.5), (5, 17, 1), []),
    ]
    for i, (name, piv, rot, frm, size, decos) in enumerate(panels):
        bo = m.bone(name, piv, t, rot=rot)
        mat = ROBE_C if i % 2 == 0 else ROBE
        m.cube(bo, f'{name}_tissu', frm, size, mat, deco={'sides': decos + [tatter(6, seed=1.3 * i + .5, holes=.06)], 'down': [tatter(1)]})
    return m
