# -*- coding: utf-8 -*-
"""Maitre Tank : lourde armure de plates bleu acier et argent, grand heaume a cimier, epaulieres massives, grand bouclier tour
orne d'un blason lumineux au bras gauche, masse d'armes a la ceinture. Yeux et blason : bleu glace."""
import classes_common as cc
import entity_kit as ek
from classes_common import pal, Mat, Decal

KEY = 'maitre_tank'
TITLE = 'Maître Tank'
ENTITY_CLASS = 'MaitreTankEntity'
VISIBLE_BOX = (1.2, 2.4, 0)

P_BLEU = pal('#10243e', '#1a3a64', '#2858a0', '#3c7cd0', '#68a8f0')
P_ARGENT = pal('#3a4452', '#5a6878', '#8496aa', '#aebecf', '#dce6f0')
GLACE = (130, 215, 255)


def blason(f):
    if f.name == 'north':
        cx, cy = f.w // 2, f.h // 2
        for y in range(f.h):
            for x in range(f.w):
                dx, dy = abs(x - cx + (0.5 if f.w % 2 == 0 else 0)), abs(y - cy)
                if dx + dy * 0.6 < min(f.w, f.h) * 0.32 and (dx < 1.2 or dy < 1.2):
                    f.px(x, y, GLACE, glow=True)


def build():
    m = ek.Model(KEY, TITLE)
    plates = Mat(P_BLEU, level=0.55, noise=0.2, scale=2, grad=0.25, decos=[cc.rivets(P_ARGENT[4], P_BLEU[0], inset=1, every=4), cc.scratches(2, P_BLEU[1])])
    argent = Mat(P_ARGENT, level=0.6, noise=0.18, scale=2, grad=0.25, decos=[cc.rivets(P_ARGENT[4], P_ARGENT[0], inset=1, every=4)])
    cc.humanoid(m, torso=plates, arms=plates, legs=plates, boots=argent, face_deco=cc.face(GLACE), arm_width=5)
    co = m.bones['corps']
    m.cube(co, 'plastron', (-5, 12, -3), (10, 12, 6), argent, inflate=0.2)
    m.cube(co, 'ventre', (-4.6, 10, -2.8), (9, 3, 5), plates)
    m.cube(co, 'ceinture', (-5, 11, -3.2), (10, 2, 6), cc.trim())
    m.cube(co, 'embleme', (-2, 15, -3.25), (4, 6, 0), Decal(blason))
    for bone, x, sx in ((m.bones['bras_droit'], 3.2, 1), (m.bones['bras_gauche'], -9.2, -1)):
        m.cube(bone, f'epauliere_{sx}', (x, 21, -3.5), (6, 4, 7), argent, share='epauliere', mirror=sx > 0)
        m.cube(bone, f'epauliere_haut_{sx}', (x + 1, 25, -2.5), (4, 1, 5), plates, share='epauliere_haut')
    t = m.bones['tete']
    m.cube(t, 'heaume', (-4, 24, -4), (8, 8, 8), argent, inflate=0.5, deco={'north': [lambda f: [f.clear(x, y) for x in range(1, f.w - 1) for y in range(2, 5)]]})
    m.cube(t, 'visiere', (-3.5, 27.5, -4.7), (7, 1, 1), Mat(P_ARGENT, level=0.05, noise=0))
    m.cube(t, 'cimier', (-0.5, 32, -4), (1, 3, 8), plates)
    m.cube(t, 'plume', (-1, 34, -3), (2, 2, 6), Mat(pal('#1a6ad0', '#3c9cf0', '#8ad0ff'), level=0.6, noise=0.2, glow=True))
    # bouclier tour au bras gauche
    bg = m.bones['bras_gauche']
    arme = m.bone('arme', (-9, 16, -3), bg)
    m.cube(arme, 'bouclier', (-12, 6, -6), (3, 20, 11), plates)
    m.cube(arme, 'bouclier_bord', (-12.4, 5.5, -6.4), (3, 21, 12), argent, inflate=0.0, deco={'east': [lambda f: None]})
    m.cube(arme, 'bouclier_blason', (-12.55, 11, -3), (0, 9, 5), Decal(blason))
    m.cube(arme, 'bouclier_pointe', (-11.5, 4, -2), (2, 2, 3), argent)
    # masse a la ceinture
    ms = m.bone('effet', (5, 12, 1), co)
    m.cube(ms, 'masse_manche', (4.5, 3, 2.5), (1, 12, 1), cc.leather(0.4))
    m.cube(ms, 'masse_tete', (3.5, 14, 1.5), (3, 4, 3), argent)
    m.cube(ms, 'masse_pointe', (4.5, 18, 2.5), (1, 2, 1), argent)
    return m


COLORS = cc.COLORS


def animations(m):
    P = ek.key_poses

    def extra(a, length):
        q = length / 4.0
        a.rot('arme', [(0, 0, 0, 0), (q * 2, 0, 0, 1.0), (length, 0, 0, 0)])
        a.scale('arme', [(0, 1, 1, 1), (q, 1, 1.004, 1), (2 * q, 1, 1, 1), (length, 1, 1, 1)])
    cc.base_idle(m, extra)
    # presentation : frappe le sol du bouclier, rugit (tete en arriere)
    a = m.anim('present', 2.4, 'once')
    P(a, [(0, {}), (0.45, {'bras_gauche': (45, 0, 10), 'corps': (-6, 0, 0), 'tete': (-8, 0, 0), 'arme': (-20, 0, 0)}),
          (0.7, {'bras_gauche': (-20, 0, 6), 'corps': (8, 0, 0), 'tete': (6, 0, 0), 'arme': (10, 0, 0)}, 'linear'),
          (1.2, {'bras_gauche': (-10, 0, 6), 'corps': (4, 0, 0)}),
          (2.4, {})])
    a.scale('arme', [(0, 1, 1, 1), (0.7, 1.12, 1.12, 1.12), (1.4, 1, 1, 1), (2.4, 1, 1, 1)])
