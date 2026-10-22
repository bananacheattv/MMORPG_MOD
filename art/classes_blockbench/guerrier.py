# -*- coding: utf-8 -*-
"""Maitre Guerrier : armure d'acier et de laiton rouge, epaulieres a pointes, casque a cornes, cape ecarlate, grande epee
runique planter devant lui. Yeux et runes de la lame : orange vif."""
import classes_common as cc
import entity_kit as ek
from classes_common import pal, Mat, stamp, Decal

KEY = 'maitre_guerrier'
TITLE = 'Maître Guerrier'
ENTITY_CLASS = 'MaitreGuerrierEntity'
VISIBLE_BOX = (0.8, 2.2, 0)

P_ROUGE = pal('#3a0c0c', '#5c1414', '#8a1c1c', '#b02828', '#d44a3a')
P_NOIR = pal('#14141a', '#1e1e26', '#2a2a34', '#3a3a46')
ORANGE = (255, 150, 40)
RUNES = ['R.R.R', '.R.R.', 'R.R.R']


def rune_blade(f):
    if f.name in ('north', 'south') and f.w >= 2:
        for y in range(2, f.h - 3, 4):
            for x in range(f.w):
                f.px(x, y, ORANGE, glow=True)
                f.px(x, y + 1, (255, 210, 120), glow=True)


def build():
    m = ek.Model(KEY, TITLE)
    acier = cc.metal()
    rouge = cc.cloth(P_ROUGE, 0.55)
    sombre = cc.cloth(P_NOIR, 0.5)
    cc.humanoid(m, torso=rouge, arms=acier, legs=sombre, boots=acier, face_deco=cc.face(ORANGE))
    co = m.bones['corps']
    m.cube(co, 'plastron', (-4.5, 13, -2.6), (9, 9, 5), acier)
    m.cube(co, 'ceinture', (-4.6, 12, -2.7), (9, 2, 5), cc.trim())
    m.cube(co, 'gemme', (-1, 13, -3.1), (2, 2, 1), Mat(pal('#8a2a00', '#d65a10', '#ff9a30', '#ffd080'), level=0.8, noise=0.1, glow=True))
    bd, bg = m.bones['bras_droit'], m.bones['bras_gauche']
    for bone, x, sx in ((bd, 3.2, 1), (bg, -8.2, -1)):
        m.cube(bone, f'epauliere_{"d" if sx > 0 else "g"}', (x, 21, -3), (5, 3, 6), acier, share='epauliere', mirror=sx > 0)
        m.cube(bone, f'pointe_{"d" if sx > 0 else "g"}', (x + (2 if sx > 0 else 2), 24, -0.5), (1, 3, 1), cc.metal(cc.P_ACIER, 0.7, False), share='pointe')
    t = m.bones['tete']
    m.cube(t, 'casque', (-4, 30, -4), (8, 2, 8), acier, inflate=0.45)
    m.cube(t, 'casque_cote_d', (3.6, 24, -3), (1, 6, 6), acier, share='joue')
    m.cube(t, 'casque_cote_g', (-4.6, 24, -3), (1, 6, 6), acier, share='joue', mirror=True)
    m.cube(t, 'nasal', (-0.5, 26, -4.7), (1, 5, 1), acier)
    for sx in (1, -1):
        h = m.bone(f'corne_{"d" if sx > 0 else "g"}', (4 * sx, 30, 0), t, rot=(0, 0, -35 * sx))
        m.cube(h, f'corne_{sx}', (3.5 * sx - 0.5 if sx > 0 else 3.5 * sx - 0.5, 30, -0.5), (1, 5, 1), cc.metal(cc.P_OR, 0.7, False), share='corne')
    cape = m.bone('cape', (0, 23, 2), co)
    m.cube(cape, 'cape_corps', (-4, 3, 2), (8, 20, 1), rouge, deco=None)
    m.cube(cape, 'cape_bord', (-4, 3, 2.05), (8, 1, 1), cc.trim())
    arme = m.bone('arme', (6, 13, -3), co)
    m.cube(arme, 'epee_pommeau', (5, 11, -3.5), (2, 2, 2), cc.trim())
    m.cube(arme, 'epee_manche', (5.5, 13, -3), (1, 4, 1), cc.leather())
    m.cube(arme, 'epee_garde', (3, 17, -3.5), (6, 1, 2), cc.trim())
    m.cube(arme, 'epee_lame', (4.5, 18, -3), (3, 18, 1), Mat(cc.P_ACIER, level=0.8, noise=0.1, grad=0.3, decos=[rune_blade]))
    m.cube(arme, 'epee_pointe', (5.5, 36, -3), (1, 2, 1), Mat(cc.P_ACIER, level=0.9, noise=0.05))
    m.cube(arme, 'epee_runes', (5, 20, -2.55), (2, 14, 0), Decal(lambda f: [f.px(x, y, ORANGE, glow=True) for y in range(1, f.h - 1, 3) for x in range(f.w)]))
    return m


COLORS = cc.COLORS


def animations(m):
    P = ek.key_poses

    def extra(a, length):
        q = length / 4.0
        a.rot('cape', [(0, 0, 0, 0), (q, 3, 0, 1.5), (2 * q, 0, 0, 0), (3 * q, 3, 0, -1.5), (length, 0, 0, 0)])
        a.rot('arme', [(0, 0, 0, 0), (q * 2, 0, 0, 1.2), (length, 0, 0, 0)])
    cc.base_idle(m, extra)
    # presentation : brandit l'epee, bras tendus, cri (tete en arriere)
    a = m.anim('present', 2.4, 'once')
    P(a, [(0, {}), (0.5, {'bras_droit': (-30, 0, -10), 'bras_gauche': (25, 0, 12), 'corps': (-4, 0, 0), 'tete': (-10, 0, 0), 'arme': (-18, 0, 4)}),
          (1.2, {'bras_droit': (-30, 0, -10), 'bras_gauche': (25, 0, 12), 'corps': (-6, 0, 0), 'tete': (-14, 0, 0), 'arme': (-26, 0, 6)}),
          (2.4, {})])
    a.scale('arme', [(0, 1, 1, 1), (0.5, 1.06, 1.06, 1.06), (1.2, 1.1, 1.1, 1.1), (2.4, 1, 1, 1)])
