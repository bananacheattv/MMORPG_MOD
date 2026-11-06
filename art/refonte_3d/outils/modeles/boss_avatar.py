#!/usr/bin/env python3
"""Avatar du neant : armure noire disjointe qui flotte, fentes violettes lumineuses, cage ouverte sur un noyau, masque sans visage, couronne brisee, longs bras et doigts angulaires. Meme squelette que art/boss_blockbench/avatar_neant.py."""
import avatar_neant as O
from boss_kit import B, P, ek

ARM = P('lave', 'nuit_violet', base=2.6, large=.012, glow_min=4)
ARM_C = P('plaque', 'nuit', base=3.2)
ARM_B = P('plaque', 'nuit', base=4)
VIO = P('cristal', 'amethyste', base=2.2, glow=True, no_ao=True)
Y_CORPS = (0, 70)
VIEW_SCALE = 5.0


def build():
    m = ek.Model(O.KEY, O.TITLE)
    b = B(m)
    m.bone('racine', (0, 0, 0))
    t = m.bone('torse', (0, 30, 0), 'racine')
    b.c(t, 'colonne', (-2.5, 26, 3), (2.5, 43, 6), ARM, deco={'south': [O.slit(1, False)]})
    b.c(t, 'taille', (-4, 26, -3), (4, 30, 3), ARM_C)
    b.c(t, 'clavicule', (-12, 41, -5), (12, 44, 5), ARM, deco={'sides': [O.vline]})
    b.c(t, 'gorgerin', (-3.5, 43, -3), (3.5, 46, 3), ARM_B)
    for side, sx in (('droite', 1), ('gauche', -1)):
        cg = m.bone(f'cage_{side}', (9 * sx, 36, 2), t)
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        m.cube(cg, f'cage_{side}_flanc', (xo(7, 3), 30, -5), (3, 11, 9), ARM, share='cage_flanc', mirror=sx < 0)
        for k, y in enumerate((38, 34, 30.5)):
            m.cube(cg, f'cote_{side}_{k + 1}', (xo(3, 5), y, -6), (5, 2, 2), ARM_B, share=f'cote_{k}', mirror=sx < 0, deco={'north': [lambda f: f.px(0 if f.cube.frm[0] > 0 else f.w - 1, 0, O.P_VIO[3], glow=True)]})
        m.cube(cg, f'cage_{side}_dos', (xo(3, 4), 29, 2), (4, 12, 2), ARM_C, share='cage_dos', mirror=sx < 0)
    n = m.bone('noyau', (0, 35.5, -1), t)
    m.cube(n, 'noyau_cristal', (-3, 32.5, -4), (6, 6, 6), VIO, deco={'*': [O.core_face]}, rot=(35, 45, 0), origin=(0, 35.5, -1))
    m.cube(n, 'noyau_eclat_haut', (-1, 39, -2), (2, 2, 2), VIO, rot=(45, 0, 45), origin=(0, 40, -1))
    m.cube(n, 'noyau_eclat_bas', (-1, 30, -2), (2, 2, 2), VIO, rot=(45, 0, 45), origin=(0, 31, -1))
    # masque sans visage, arete, joues, couronne brisee
    h = m.bone('masque', (0, 45, 0), t)
    b.c(h, 'masque_plaque', (-4, 45, -4.5), (4, 57, 3.5), ARM_B, deco={'north': [O.slit(1)]})
    b.c(h, 'masque_arete', (-1, 56, -5.5), (1, 59, 2.5), ARM_C)
    b.c(h, 'masque_joue_droite', (4, 47, -3.5), (5, 54, 1.5), ARM_C, share='joue')
    b.c(h, 'masque_joue_gauche', (-5, 47, -3.5), (-4, 54, 1.5), ARM_C, share='joue', mirror=True)
    b.c(h, 'masque_pointe', (-1, 43, -5), (1, 46, -3), ARM_C)
    cr = m.bone('couronne', (0, 63, 0), h)
    for name, c, s, r in O.COURONNE:
        bo = m.bone(name, c, cr, rot=r)
        mat = VIO if name == 'couronne_centre' else ARM_C
        m.cube(bo, f'{name}_piece', tuple(c[i] - s[i] / 2 for i in range(3)), s, mat, deco={'*': [O.vline]} if mat is ARM_C and s[1] > 3 else None)
    # epaulieres detachees qui flottent
    for side, sx in (('droite', 1), ('gauche', -1)):
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        ep = m.bone(f'epauliere_{side}', (15 * sx, 42, 0), t, rot=(0, 0, -12 * sx))
        m.cube(ep, f'epauliere_{side}_plaque', (xo(11, 10), 38, -7), (10, 7, 14), ARM, share='epaul', mirror=sx < 0, deco={'*': [O.rune_ring]})
        m.cube(ep, f'epauliere_{side}_crete', (xo(12, 7), 45, -5.5), (7, 2, 11), ARM_B, share='epaul_crete', mirror=sx < 0)
        m.cube(ep, f'epauliere_{side}_lame', (xo(13, 8), 36, -6), (8, 2, 12), ARM_C, share='epaul_lame', mirror=sx < 0, deco={'sides': [lambda f: O.border(O.P_VIO[2], 'b', glow=True)(f)]})
        for k, (x, z, hh) in enumerate(((13, -4, 6), (16, 0, 8), (13, 3, 5))):
            m.cube(ep, f'epauliere_{side}_pique_{k + 1}', (xo(x, 2), 47, z), (2, hh, 2), VIO if k == 1 else ARM_B, share=f'epaul_pique{k}', mirror=sx < 0, rot=(0, 0, -8 * sx), origin=(xo(x, 2) + 1, 47, z + 1))
    # bras segmentes, doigts angulaires
    for side, sx in (('droit', 1), ('gauche', -1)):
        cote = 'droite' if sx > 0 else 'gauche'
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        bo = m.bone(f'bras_{side}', (10 * sx, 40, 0), t, rot=(0, 0, 15 * sx))
        m.cube(bo, f'bras_{side}_segment', (xo(8, 4), 31, -2), (4, 9, 4), ARM, share='bras', mirror=sx < 0, deco={'*': [O.vline]})
        fa = m.bone(f'avant_bras_{side}', (10 * sx, 30, 0), bo, rot=(14, 0, 0))
        m.cube(fa, f'avant_bras_{side}_segment', (xo(7.5, 5), 20, -3), (5, 10, 6), ARM, share='avant_bras', mirror=sx < 0, deco={'north': [O.slit(1)]})
        m.cube(fa, f'coude_{side}', (xo(8.5, 3), 27, 2), (3, 3, 3), ARM_B, share='coude', mirror=sx < 0, rot=(-35, 0, 0), origin=(10 * sx, 28.5, 3.5))
        m.cube(fa, f'aileron_{side}', (xo(12.5, 2), 21, -1), (2, 8, 3), ARM_C, share='aileron_av', mirror=sx < 0)
        hd = m.bone(f'main_{cote}', (10 * sx, 19, 0), fa)
        m.cube(hd, f'paume_{side}', (xo(8, 4), 15, -2.5), (4, 4, 5), ARM_B, share='paume', mirror=sx < 0)
        dg = m.bone(f'doigts_{cote}', (10 * sx, 15, 0), hd, rot=(-12, 0, 0))
        for k, z in enumerate((-2.5, -.5, 1.5)):
            m.cube(dg, f'doigt_{side}_{k + 1}', (xo(8.5 + 1.2 * (k == 1), 1), 8, z), (1, 7, 1), ARM_B, share='doigt', mirror=sx < 0, deco={'*': [lambda f: f.px(0, f.h - 1, O.P_VIO[4], glow=True) if f.side else None]}, rot=(0, 0, 8 * sx * (k - 1)), origin=(xo(8.5 + 1.2 * (k == 1), 1) + .5, 15, z + .5))
        m.cube(dg, f'pouce_{side}', (xo(11.5, 1), 11, -2), (1, 4, 1), ARM_B, share='pouce', mirror=sx < 0, rot=(0, 0, -20 * sx), origin=(xo(11.5, 1) + .5, 15, -1.5))
    # bas du corps en fragments effiles, eclats en orbite
    for name, c, s, r in O.BAS:
        bo = m.bone(name, c, t, rot=r)
        m.cube(bo, f'{name}_piece', tuple(c[i] - s[i] / 2 for i in range(3)), s, ARM, deco={'north': [O.slit(1, False)]} if s[1] >= 5 else None)
    fr = m.bone('fragments', (0, 36, 0), 'racine')
    for name, c, s in O.ECLATS:
        bo = m.bone(name, c, fr, rot=(35, 45, 0))
        m.cube(bo, f'{name}_piece', tuple(c[i] - s / 2 for i in range(3)), (s, s, s), ARM_B if s > 2 else VIO, deco={'*': [O.vline]} if s > 2 else None)
    return m
