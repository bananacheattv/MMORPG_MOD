# -*- coding: utf-8 -*-
"""Gardien de l'epreuve : long manteau blanc et or, plastron dore, halo lumineux qui tourne derriere la tete, sablier d'or
flottant, baton de pelerin. Veille sur la salle d'essai des classes."""
import classes_common as cc
import entity_kit as ek
from classes_common import pal, Mat, Decal

KEY = 'gardien_epreuve'
TITLE = "Gardien de l'épreuve"
ENTITY_CLASS = 'GardienEpreuveEntity'
VISIBLE_BOX = (0.9, 2.4, 0)

P_BLANC = pal('#a8a49a', '#c4c0b6', '#dedad0', '#f0ece2', '#fffaf0')
P_DORE = cc.P_OR
OR_CLAIR = (255, 232, 140)


def build():
    m = ek.Model(KEY, TITLE)
    blanc = cc.cloth(P_BLANC, 0.6)
    dore = Mat(P_DORE, level=0.6, noise=0.15, scale=1.5, grad=0.3, decos=[cc.rivets(P_DORE[4], P_DORE[0], inset=1, every=4)])
    cc.humanoid(m, torso=blanc, arms=blanc, legs=blanc, boots=dore, face_deco=cc.face(OR_CLAIR))
    co = m.bones['corps']
    m.cube(co, 'plastron', (-4.5, 13, -2.6), (9, 9, 5), dore)
    m.cube(co, 'manteau_bas', (-5, 1, -3), (10, 11, 6), Mat(P_BLANC, level=0.6, noise=0.2, grad=0.2, decos=[]))
    m.cube(co, 'manteau_ourlet', (-5.2, 0.5, -3.2), (10, 1, 6), dore)
    m.cube(co, 'etole_d', (1, 2, -3.1), (2, 20, 0), cc.cloth(P_DORE, 0.6) if False else Decal(lambda f: [f.px(x, y, (232, 190, 68)) for y in range(f.h) for x in range(f.w)]))
    m.cube(co, 'etole_g', (-3, 2, -3.1), (2, 20, 0), Decal(lambda f: [f.px(x, y, (232, 190, 68)) for y in range(f.h) for x in range(f.w)]))
    for bone, x, sx in ((m.bones['bras_droit'], 3.6, 1), (m.bones['bras_gauche'], -8.6, -1)):
        m.cube(bone, f'epaulette_{sx}', (x, 21, -2.6), (5, 2, 5), dore, share='epaulette', mirror=sx > 0)
    t = m.bones['tete']
    m.cube(t, 'cheveux', (-4, 31, -4), (8, 2, 8), Mat(P_BLANC, level=0.8, noise=0.1), inflate=0.3)
    m.cube(t, 'cheveux_dos', (-4, 22, 3.5), (8, 10, 1), Mat(P_BLANC, level=0.8, noise=0.1))
    halo = m.bone('effet', (0, 28, 4.5), t)
    for i, (x, y, w, h) in enumerate(((-6, 32.5, 12, 1), (-6, 23.5, 12, 1), (-6.5, 24.5, 1, 8), (5.5, 24.5, 1, 8))):
        m.cube(halo, f'halo_{i}', (x, y, 4.2), (w, h, 1), Mat(pal('#a88410', '#e8be44', '#fff0a0', '#ffffe0'), level=0.8, noise=0.1, glow=True))
    # baton de pelerin et sablier flottant
    arme = m.bone('arme', (6, 12, -1), m.bones['bras_droit'])
    m.cube(arme, 'baton', (5.5, 0, -1.5), (1, 38, 1), cc.leather(0.5))
    m.cube(arme, 'baton_tete', (4.5, 38, -2.5), (3, 3, 3), dore)
    sab = m.bone('sablier', (-9, 22, -3), co)
    m.cube(sab, 'sablier_haut', (-11, 23, -4), (4, 1, 3), dore)
    m.cube(sab, 'sablier_bas', (-11, 17, -4), (4, 1, 3), dore)
    m.cube(sab, 'sablier_verre', (-10, 18, -3.5), (2, 5, 2), Mat(pal('#e8be44', '#f6d870', '#fff0a8'), level=0.8, noise=0.1, glow=True))
    return m


COLORS = cc.COLORS


def animations(m):
    P = ek.key_poses

    def extra(a, length):
        q = length / 4.0
        a.rot('effet', [(0, 0, 0, 0), (q * 2, 0, 0, 180), (length, 0, 0, 360)], 'linear')
        a.rot('sablier', [(0, 0, 0, 0), (length, 0, 360, 0)], 'linear')
        a.pos('sablier', [(0, 0, 0, 0), (q, 0, 1.0, 0), (2 * q, 0, 0, 0), (3 * q, 0, -1.0, 0), (length, 0, 0, 0)])
    cc.base_idle(m, extra, length=4.0)
    a = m.anim('present', 2.4, 'once')
    P(a, [(0, {}), (0.6, {'bras_gauche': (-60, 0, 20), 'bras_droit': (0, 0, -6), 'tete': (-6, 0, 0)}), (1.6, {'bras_gauche': (-60, 0, 20), 'tete': (-6, 0, 0)}), (2.4, {})])
