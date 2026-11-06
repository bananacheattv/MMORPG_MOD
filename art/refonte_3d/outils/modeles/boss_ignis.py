#!/usr/bin/env python3
"""Ignis, seigneur demon : armure d'obsidienne fendue de lave, filets de bronze, tabard rouge, heaume a grandes cornes, epaulieres a piques, longue epee a coeur de fusion. Meme squelette que art/boss_blockbench/ignis.py."""
import ignis as O
from boss_kit import B, P, ek
from entity_kit import border, rivets, stamp, tatter

OBS = P('lave', 'basalte', base=1.5, large=.016, glow_min=4)
OBS_L = P('lave', 'basalte', base=1.7, large=.01, glow_min=4)
OBS_C = P('plaque', 'pierre_noire', base=2.6)
BRONZE = P('plaque', 'cuivre', base=1.8, rivets=4)
BRONZE_U = P('metal', 'cuivre', base=2)
LAVE = P('uni', 'flamme', base=4.2, glow=True, no_ao=True)
LAVE_F = P('flamme_pleine', 'flamme', base=3.6, glow=True, no_ao=True)
TISSU = P('peau_tissu', 'rouge', base=2.2, pli=5)
Y_CORPS = (0, 70)
VIEW_SCALE = 5.0
FILET = lambda s='b': border((142, 106, 51), s)


def build():
    m = ek.Model(O.KEY, O.TITLE)
    b = B(m)
    m.bone('racine', (0, 0, 0))
    bs = m.bone('bassin', (0, 30, 0), 'racine')
    b.c(bs, 'bassin_armure', (-11, 26, -7), (11, 33, 7), OBS)
    b.c(bs, 'ceinture', (-12, 31, -8), (12, 34, 8), BRONZE)
    b.c(bs, 'boucle', (-3, 29.5, -9), (3, 34.5, -8), OBS_C, deco={'north': [stamp(['.oo.', 'oEEo', 'oEEo', '.oo.'], O.E, glow='oE')]})
    for sx, n in ((1, 'd'), (-1, 'g')):
        x0, x1 = sorted((sx * 7, sx * 13))
        b.c(bs, f'tassette_{n}', (x0, 19, -6), (x1, 31, 6), OBS, deco={'sides': [FILET('b')]}, rot=(0, 0, 8 * sx), origin=(sx * 11, 31, 0), share='tassette', mirror=sx < 0)
    pg = m.bone('pagne', (0, 31, -8.5), bs)
    b.c(pg, 'pagne_tissu', (-5, 10, -9.5), (5, 31, -8.5), TISSU, deco={'sides': [border(O.P_BRONZE[3], 'lr'), O.tabard_rune, tatter(3, seed=.8)]})
    pa = m.bone('pagne_arriere', (0, 31, 8.5), bs)
    b.c(pa, 'pagne_arriere_tissu', (-7, 12, 8), (7, 31, 9), TISSU, deco={'sides': [border(O.P_BRONZE[2], 'lr'), tatter(3, seed=2.1)]})
    # torse en V : taille abdominale, grande poitrine, noyau de lave
    t = m.bone('torse', (0, 33, 0), bs)
    b.c(t, 'taille', (-10, 33, -6), (10, 39, 6), OBS_C, deco={'*': [O.abs_lines]})
    b.c(t, 'poitrine', (-14, 39, -8), (14, 51, 8), OBS)
    b.c(t, 'pectoral_droit', (2, 41, -10), (12, 50, -8), OBS_C, deco={'*': [O.chest_lines]}, share='pectoral')
    b.c(t, 'pectoral_gauche', (-12, 41, -10), (-2, 50, -8), OBS_C, deco={'*': [O.chest_lines]}, share='pectoral', mirror=True)
    b.c(t, 'gorgerin', (-7, 49, -7), (7, 53, 6), OBS_C, deco={'sides': [border(O.P_BRONZE[3], 'b')]})
    b.c(t, 'cadre_noyau', (-3, 39.5, -9.5), (3, 40.5, -8.5), BRONZE_U)
    for i, y in enumerate((41, 45, 49)):
        m.cube(t, f'pointe_dos_{i + 1}', (-2, y, 7.5), (4, 3, 5), OBS_C, rot=(-30, 0, 0), origin=(0, y, 8), deco={'*': [O.glow_tip(1)]})
    n = m.bone('noyau', (0, 45, -8.5), t)
    b.c(n, 'noyau_coeur', (-2, 41, -11), (2, 49, -9), LAVE, deco={'*': [O.core_glow]})
    b.c(n, 'noyau_branches', (-5, 44, -10.5), (5, 46, -9.5), LAVE)
    # heaume et cornes en chaine
    h = m.bone('tete', (0, 52, -1), t)
    b.c(h, 'casque', (-5, 52, -6.5), (5, 63, 3.5), OBS_C, deco={'north': [O.VISOR]})
    b.c(h, 'mentonniere', (-4, 50, -7.5), (4, 54, -5.5), OBS_C, deco={'north': [O.CHIN]})
    b.c(h, 'crete', (-1, 63, -7), (1, 66, 3), OBS)
    b.c(h, 'arcade', (-5.5, 60, -7.5), (5.5, 62, -6.5), BRONZE_U)
    b.c(h, 'couvre_nuque', (-6, 53, 1), (6, 60, 5), OBS, rot=(10, 0, 0), origin=(0, 60, 3))
    cs = m.bone('cornes', (0, 59, -2), h)
    for side, sx in (('droite', 1), ('gauche', -1)):
        segs = [(7, 6, 6, 10), (6, 5, 5, 32), (5, 4, 4, 32), (5, 3, 3, 28)]
        parent, x = cs, 4.5
        for k, (ln, hh, dd, rz) in enumerate(segs):
            bo = m.bone(f'corne_{side}' + ('' if k == 0 else f'_{k + 1}'), (x * sx, 59, -2), parent, rot=(-6 if k == 3 else 0, 0, rz * sx))
            x0 = x if sx > 0 else -x - ln
            m.cube(bo, f'corne_{side}_{k + 1}', (x0, 59 - hh / 2, -2 - dd / 2), (ln, hh, dd), OBS_L, deco={'*': [O.tip_x]} if k == 3 else None, share=f'corne_{k}', mirror=sx < 0)
            parent, x = bo, x + ln - .5
    # bras, epaulieres a piques, gantelets griffus
    for side, sx in (('droit', 1), ('gauche', -1)):
        hand = 'main_droite' if sx > 0 else 'main_gauche'
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        bo = m.bone(f'bras_{side}', (14 * sx, 47, 0), t, rot=(0, 0, 6 * sx))
        m.cube(bo, f'bras_{side}_armure', (xo(11, 8), 36, -4), (8, 12, 8), OBS, share='bras_haut', mirror=sx < 0)
        ep = m.bone(f'epauliere_{"droite" if sx > 0 else "gauche"}', (16 * sx, 50, 0), bo)
        m.cube(ep, f'epauliere_{side}_plaque', (xo(9, 15), 46, -9), (15, 8, 18), OBS, share='epaul', mirror=sx < 0, deco={'sides': [border(O.P_BRONZE[3], 'b')]})
        m.cube(ep, f'epauliere_{side}_lame', (xo(10.5, 13), 43, -8), (13, 3, 16), OBS_C, share='epaul_lame', mirror=sx < 0, deco={'sides': [border(O.P_BRONZE[2], 'b')]})
        m.cube(ep, f'epauliere_{side}_lame2', (xo(12.5, 11), 41, -6.5), (11, 2, 13), OBS_C, share='epaul_lame2', mirror=sx < 0)
        for k, (z, hh) in enumerate(((-6, 7), (0, 6), (5, 4))):
            pk = m.bone(f'pique_{side}_{k + 1}', (16.5 * sx, 54, z + 1.5), ep, rot=(0, 0, -20 * sx))
            m.cube(pk, f'pique_{side}_{k + 1}_base', (xo(15, 3), 54, z), (3, hh, 3), OBS_C, share=f'pique_{k}', mirror=sx < 0)
            m.cube(pk, f'pique_{side}_{k + 1}_bout', (xo(15.5, 2), 54 + hh, z + .5), (2, 3, 2), OBS_C, share='pique_bout', mirror=sx < 0, deco={'*': [O.glow_tip(1)]})
        fa = m.bone(f'avant_bras_{side}', (16 * sx, 37, 0), bo, rot=(22, 0, 0) if sx > 0 else (12, 0, 4))
        m.cube(fa, f'gantelet_{side}', (xo(11.5, 9), 26, -4.5), (9, 12, 9), OBS, share='gantelet', mirror=sx < 0, deco={'sides': [border(O.P_BRONZE[3], 't')]})
        m.cube(fa, f'aileron_{side}', (xo(20, 3), 27, -2), (3, 10, 4), OBS_C, share='aileron', mirror=sx < 0, deco={'*': [O.glow_tip(1)]})
        hd = m.bone(hand, (16 * sx, 27, 0), fa)
        m.cube(hd, f'poing_{side}', (xo(12, 8), 20, -4), (8, 7, 8), OBS_C, share='poing', mirror=sx < 0)
        for k in range(3):
            m.cube(hd, f'griffe_{side}_{k + 1}', (xo(12.5 + 2.5 * k, 2), 16, -4.5), (2, 5, 2), OBS_C, share='griffe', mirror=sx < 0, deco={'*': [O.glow_tip(2)]}, rot=(-25, 0, 0) if sx > 0 else (-10, 0, (k - 1) * 12 * sx), origin=(xo(12.5 + 2.5 * k, 2) + 1, 20, -3.5))
    # grande epee : poignee, garde en croissant, lame noire a coeur de fusion
    ep = m.bone('epee', (16, 23.5, -.5), 'main_droite', rot=(-48, 0, 0))
    b.c(ep, 'poignee', (15, 22.5, -6), (17, 24.5, 6), BRONZE_U, deco={'*': [lambda f: [f.px(x, y, O.P_BRONZE[1]) for y in range(f.h) for x in range(f.w) if (x + y) % 2 == 0]]})
    b.c(ep, 'pommeau', (14.5, 22, 6), (17.5, 25, 9), OBS_C, deco={'*': [O.glow_tip(1)]})
    b.c(ep, 'garde', (14, 15, -8.5), (18, 32, -6.5), OBS, deco={'sides': [border(O.P_BRONZE[3], 'tb')]})
    b.c(ep, 'garde_pointes', (14.5, 13, -9), (17.5, 34, -8), OBS_C, deco={'*': [O.glow_tip(1)]})
    b.c(ep, 'lame', (15, 18.5, -48.5), (17, 28.5, -8.5), OBS_C, deco={'*': [O.molten_core]})
    b.c(ep, 'pointe', (15, 20.5, -53.5), (17, 26.5, -48.5), OBS_C, deco={'*': [O.molten_core]})
    # jambes
    for side, sx in (('droite', 1), ('gauche', -1)):
        foot = 'droit' if sx > 0 else 'gauche'
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        lg = m.bone(f'jambe_{side}', (6.5 * sx, 28, 0), bs)
        m.cube(lg, f'cuissard_{side}', (xo(2, 9), 18, -4.5), (9, 11, 9), OBS, share='cuissard', mirror=sx < 0, deco={'sides': [border(O.P_BRONZE[3], 't')]})
        tb = m.bone(f'tibia_{foot}', (6.5 * sx, 18, 0), lg)
        m.cube(tb, f'jambiere_{side}', (xo(2.5, 8), 5, -4), (8, 13, 8), OBS, share='jambiere', mirror=sx < 0)
        m.cube(tb, f'genouillere_{side}', (xo(3.5, 6), 14, -5.5), (6, 5, 2), BRONZE, share='genou', mirror=sx < 0, deco={'north': [stamp(['..o...', '.oOo..'], O.E, glow='oO', ay='b', dy=-1)]})
        m.cube(tb, f'aileron_mollet_{side}', (xo(5, 3), 7, 4), (3, 9, 3), OBS_C, share='mollet', mirror=sx < 0, rot=(-14, 0, 0), origin=(xo(5, 3) + 1.5, 16, 4))
        pd = m.bone(f'pied_{foot}', (6.5 * sx, 5, 0), tb)
        m.cube(pd, f'soleret_{side}', (xo(1.5, 10), 0, -8), (10, 5, 12), OBS, share='soleret', mirror=sx < 0)
        for k in range(3):
            m.cube(pd, f'orteil_{side}_{k + 1}', (xo(2 + 3.2 * k, 2), 0, -11), (2, 3, 3), OBS_C, share='orteil', mirror=sx < 0, deco={'north': [O.glow_tip(1)]})
        m.cube(pd, f'eperon_{side}', (xo(5.5, 2), 1, 4), (2, 2, 3), OBS_C, share='eperon', mirror=sx < 0)
    return m
