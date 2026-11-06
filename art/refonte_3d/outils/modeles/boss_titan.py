#!/usr/bin/env python3
"""Titan de glace : roche sombre fissuree, plaques de glace bleue, givre sur les aretes, noyau de cristal lumineux, cristaux sur le dos et les epaules, poings en masses. Meme squelette que art/boss_blockbench/titan_glace.py."""
import titan_glace as O
from boss_kit import B, P, ek
from entity_kit import stamp

ROC = P('roche', 'pierre_noire', base=3)
ROC_C = P('roche', 'pierre_noire', base=3.6)
ROC_S = P('roche', 'pierre_noire', base=2.2)
GLACE = P('cristal', 'glace', base=2)
GLACE_C = P('cristal', 'glace', base=2.6)
CRIS = P('cristal', 'glace', base=1.2)
COEUR = P('cristal', 'ame', base=2.6, glow=True, no_ao=True)
CREUX = P('uni', 'nuit', base=1.2, no_ao=True)
Y_CORPS = (0, 100)
VIEW_SCALE = 3.4
GIVRE = O.frost_cap


def build():
    m = ek.Model(O.KEY, O.TITLE)
    b = B(m)
    m.bone('racine', (0, 0, 0))
    bs = m.bone('bassin', (0, 42, 0), 'racine')
    b.c(bs, 'bassin_roc', (-16, 36, -10), (16, 48, 10), ROC, deco={'sides': [GIVRE(2, .5)], 'up': [GIVRE()]})
    b.c(bs, 'ceinture_glace', (-11, 38, -12), (11, 44, -10), GLACE, deco={'*': [O.plate_edges]})
    # torse enorme, ventre plus etroit
    t = m.bone('torse', (0, 48, 0), bs)
    b.c(t, 'ventre', (-18, 46, -12), (18, 59, 12), ROC_S, deco={'sides': [GIVRE(2, 1.1)]})
    b.c(t, 'poitrine', (-24, 58, -14), (24, 80, 14), ROC, deco={'sides': [GIVRE(3, 2.3)], 'up': [GIVRE()]})
    b.c(t, 'poitrine_devant', (-18, 60, -16), (18, 78, -14), ROC_C)
    b.c(t, 'cavite_noyau', (-7, 58, -17), (7, 73, -16), CREUX)
    b.c(t, 'plaque_dos', (-16, 60, 14), (16, 76, 17), GLACE, deco={'*': [O.plate_edges]})
    n = m.bone('noyau', (0, 66, -15), t)
    m.cube(n, 'noyau_cristal', (-4, 62, -19), (8, 8, 4), COEUR, deco={'*': [O.core_face]}, rot=(0, 0, 45), origin=(0, 66, -17))
    b.c(n, 'noyau_eclats', (-1.5, 59, -18.5), (1.5, 73, -16.5), COEUR)
    for side, sx in (('droite', 1), ('gauche', -1)):
        pq = m.bone(f'plaque_{side}', (15 * sx, 66, -15), t)
        m.cube(pq, f'plaque_{side}_glace', (2 if sx > 0 else -15, 57, -19), (13, 17, 3), GLACE, deco={'*': [O.plate_edges], 'up': [GIVRE()]}, share='plaque_poitrine', mirror=sx < 0)
    pv = m.bone('plaque_ventre', (0, 57, -14), t)
    b.c(pv, 'plaque_ventre_glace', (-10, 47, -16), (10, 57, -13), GLACE, deco={'*': [O.plate_edges]})
    # tete enfoncee, machoire, front de glace
    h = m.bone('tete', (0, 78, -12), t)
    b.c(h, 'tete_roc', (-7, 74, -22), (7, 86, -9), ROC_C, deco={'north': [O.EYES], 'sides': [GIVRE(2, 3.3)], 'up': [GIVRE()]})
    b.c(h, 'arcade_glace', (-8, 82, -24), (8, 85, -20), GLACE, deco={'*': [O.plate_edges]})
    b.c(h, 'machoire', (-6, 71, -21.5), (6, 75, -10.5), ROC_S)
    for i, (x, hh, rz) in enumerate(((-5, 7, 14), (0, 9, 0), (5, 6, -14))):
        m.cube(h, f'corne_glace_{i + 1}', (x - 1.5, 85, -18), (3, hh, 3), CRIS, rot=(-10, 45, rz), origin=(x, 85, -16.5))
    # cristaux du dos
    cr = m.bone('cristaux', (0, 78, 12), t)
    for i, (x, y, z, w, hh, d, rx, rz) in enumerate(((-12, 74, 10, 6, 18, 6, -25, 18), (-4, 76, 12, 7, 24, 7, -30, 4), (6, 75, 11, 6, 17, 6, -22, -14), (14, 72, 10, 5, 13, 5, -18, -30), (-17, 70, 9, 5, 11, 5, -14, 34))):
        m.cube(cr, f'cristal_dos_{i + 1}', (x, y, z), (w, hh, d), CRIS, rot=(rx, 0, rz), origin=(x + w / 2, y, z + d / 2))
        m.cube(cr, f'cristal_dos_p{i + 1}', (x + 1, y + hh, z + 1), (w - 2, max(3, hh // 3), d - 2), GLACE_C, rot=(rx, 0, rz), origin=(x + w / 2, y, z + d / 2))
    # epaules, bras, avant-bras, poings
    for side, sx in (('droite', 1), ('gauche', -1)):
        bside = 'droit' if sx > 0 else 'gauche'
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        ep = m.bone(f'epaule_{side}', (24 * sx, 76, 0), t)
        m.cube(ep, f'epaule_{side}_roc', (xo(20, 18), 68, -13), (18, 16, 26), ROC, share='epaule', mirror=sx < 0, deco={'sides': [GIVRE(3, 4.4)], 'up': [GIVRE()]})
        m.cube(ep, f'epaule_{side}_glace', (xo(21, 18), 84, -12), (18, 3, 24), GLACE, share='epaule_glace', mirror=sx < 0, deco={'*': [O.plate_edges]})
        cc = m.bone(f'cristaux_epaule_{side}', (30 * sx, 86, 0), ep)
        big = sx > 0
        specs = ((25, -8, 6, 16 if big else 12, 6, -10, -14), (31, -1, 7, 21 if big else 15, 7, 6, -6), (27, 6, 5, 13, 5, 18, -20))
        if big:
            specs += ((34, -10, 4, 10, 4, -16, -30),)
        for k, (x, z, w, hh, d, rx, rz) in enumerate(specs):
            m.cube(cc, f'cristal_epaule_{side}_{k + 1}', (xo(x, w), 86, z), (w, hh, d), CRIS, rot=(rx, 0, rz * sx), origin=(xo(x, w) + w / 2, 86, z + d / 2))
        bo = m.bone(f'bras_{bside}', (30 * sx, 74, 0), ep, rot=(0, 0, 4 * sx))
        m.cube(bo, f'bras_{side}_roc', (xo(23, 14), 56, -7), (14, 20, 14), ROC_S, share='bras', mirror=sx < 0, deco={'sides': [GIVRE(2, 5.1)]})
        fa = m.bone(f'avant_bras_{bside}', (30 * sx, 57, 0), bo, rot=(10, 0, 0))
        m.cube(fa, f'avant_bras_{side}_roc', (xo(22, 16), 32, -8), (16, 26, 16), ROC, share='avant_bras', mirror=sx < 0, deco={'sides': [GIVRE(2, 6.7)]})
        m.cube(fa, f'brassard_{side}_glace', (xo(21, 18), 40, -9), (18, 10, 18), GLACE, share='brassard', mirror=sx < 0, deco={'*': [O.plate_edges], 'up': [GIVRE()]})
        pg = m.bone(f'poing_{bside}', (30 * sx, 33, 0), fa)
        m.cube(pg, f'poing_{side}_roc', (xo(19, 22), 10, -11), (22, 22, 22), ROC_C, share='poing', mirror=sx < 0, deco={'sides': [GIVRE(4, 7.9)], 'up': [GIVRE()], 'north': [O.knuckles]})
        m.cube(pg, f'poing_{side}_glace', (xo(18, 24), 24, -12), (24, 6, 24), GLACE, share='poing_glace', mirror=sx < 0, deco={'*': [O.plate_edges], 'up': [GIVRE()]})
        m.cube(pg, f'poing_{side}_cristal', (xo(26, 6), 30, -4), (6, 9, 6), CRIS, share='poing_cristal', mirror=sx < 0, rot=(0, 0, -15 * sx), origin=(xo(26, 6) + 3, 30, -1))
        for k, (x, z) in enumerate(((21, -6), (29, -6), (21, 4), (29, 4))):
            m.cube(pg, f'poing_{side}_pointe_{k + 1}', (xo(x, 4), 6, z), (4, 5, 4), GLACE_C, share='poing_pointe', mirror=sx < 0)
    # jambes
    for side, sx in (('droite', 1), ('gauche', -1)):
        bside = 'droit' if sx > 0 else 'gauche'
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        lg = m.bone(f'jambe_{side}', (10 * sx, 41, 0), bs)
        m.cube(lg, f'cuisse_{side}', (xo(3, 15), 27, -7.5), (15, 15, 15), ROC, share='cuisse', mirror=sx < 0, deco={'sides': [GIVRE(2, 8.3)]})
        tb = m.bone(f'tibia_{bside}', (10.5 * sx, 27, 0), lg)
        m.cube(tb, f'tibia_{side}_roc', (xo(3, 15), 8, -7.5), (15, 19, 15), ROC_S, share='tibia', mirror=sx < 0, deco={'sides': [GIVRE(2, 9.1)]})
        m.cube(tb, f'genou_{side}_glace', (xo(4, 13), 20, -10), (13, 9, 3), GLACE, share='genou', mirror=sx < 0, deco={'*': [O.plate_edges]})
        pd = m.bone(f'pied_{bside}', (10.5 * sx, 9, 0), tb)
        m.cube(pd, f'pied_{side}_roc', (xo(0, 21), 0, -13), (21, 9, 25), ROC, share='pied', mirror=sx < 0, deco={'sides': [GIVRE(3, 10.2)], 'up': [GIVRE()]})
        for k in range(3):
            m.cube(pd, f'orteil_{side}_{k + 1}', (xo(.5 + 7 * k, 6), .5, -16), (6, 6, 4), GLACE, share='orteil', mirror=sx < 0, deco={'*': [O.plate_edges]})
    return m
