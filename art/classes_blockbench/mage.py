# -*- coding: utf-8 -*-
"""Maitre Mage : longue robe violette a liseres d'or, chapeau de sorcier, barbe argentee, baton surmonte d'une orbe arcanique
flottante, grimoire flottant dont les pages brillent. Yeux et orbe : cyan."""
import classes_common as cc
import entity_kit as ek
from classes_common import pal, Mat, Decal

KEY = 'maitre_mage'
TITLE = 'Maître Mage'
ENTITY_CLASS = 'MaitreMageEntity'
VISIBLE_BOX = (0.8, 2.6, 0)

P_ROBE = pal('#1c0f3a', '#2c1860', '#43268c', '#5e38b4', '#8056d6')
P_BARBE = pal('#8a8a96', '#a8a8b6', '#c8c8d6', '#e6e6f0')
P_ORBE = pal('#1a8aa8', '#2ab8d8', '#6ae0f6', '#b8f6ff', '#f0ffff')
CYAN = (110, 230, 255)


def runes_hem(f):
    if f.side and f.h >= 3:
        for x in range(1, f.w, 3):
            f.px(x, 1, CYAN, glow=True)
            f.px(x, 2, (60, 150, 190), glow=True)


def build():
    m = ek.Model(KEY, TITLE)
    robe = cc.cloth(P_ROBE, 0.5)
    cc.humanoid(m, torso=robe, arms=robe, legs=robe, boots=cc.leather(), face_deco=cc.face(CYAN))
    co = m.bones['corps']
    m.cube(co, 'ceinture', (-4.5, 12, -2.5), (9, 2, 5), cc.trim())
    m.cube(co, 'robe_bas', (-5, 2, -3), (10, 10, 6), Mat(P_ROBE, level=0.5, noise=0.22, scale=1.5, grad=0.2, decos=[runes_hem]))
    m.cube(co, 'robe_ourlet', (-5.2, 1, -3.2), (10, 1, 6), cc.trim())
    m.cube(co, 'col', (-4.5, 22, -2.5), (9, 2, 5), cc.trim(), inflate=0.0)
    t = m.bones['tete']
    m.cube(t, 'barbe', (-3, 20, -4.6), (6, 5, 1), Mat(P_BARBE, level=0.6, noise=0.2, scale=(1, 2), grad=0.3))
    m.cube(t, 'barbe_pointe', (-2, 17, -4.6), (4, 3, 1), Mat(P_BARBE, level=0.6, noise=0.2, scale=(1, 2), grad=0.3))
    m.cube(t, 'cheveux', (-4, 28, 3.6), (8, 5, 1), Mat(P_BARBE, level=0.5, noise=0.2))
    # chapeau de sorcier : paliers qui se courbent
    m.cube(t, 'chapeau_bord', (-6, 31.5, -6), (12, 1, 12), robe)
    m.cube(t, 'chapeau_1', (-4, 32.5, -4), (8, 3, 8), robe)
    h2 = m.bone('chapeau_haut', (0, 35, 0), t, rot=(8, 0, 6))
    m.cube(h2, 'chapeau_2', (-3, 35.5, -3), (6, 3, 6), robe)
    h3 = m.bone('chapeau_pointe', (0, 38, 0), h2, rot=(12, 0, 8))
    m.cube(h3, 'chapeau_3', (-2, 38.5, -2), (4, 3, 4), robe)
    m.cube(h3, 'chapeau_4', (-1, 41.5, -1), (2, 3, 2), robe)
    m.cube(t, 'chapeau_ruban', (-4.2, 32.2, -4.2), (8, 1, 8), cc.trim(), inflate=0.2)
    # baton et orbe (suivent le bras droit)
    bd = m.bones['bras_droit']
    arme = m.bone('arme', (6, 12, -1), bd)
    m.cube(arme, 'baton', (5.5, 2, -1.5), (1, 36, 1), cc.leather(0.4))
    m.cube(arme, 'baton_anneau', (4.5, 38, -2.5), (3, 1, 3), cc.trim())
    for dx, dz in ((4.5, -2.5), (6.5, -2.5), (4.5, -0.5), (6.5, -0.5)):
        m.cube(arme, f'griffe_{dx}_{dz}', (dx, 39, dz), (1, 3, 1), cc.trim())
    orbe = m.bone('effet', (6, 41, -1), arme)
    m.cube(orbe, 'orbe', (4, 39.5, -3), (4, 4, 4), Mat(P_ORBE, level=0.75, noise=0.2, scale=1, grad=0.2, glow=True))
    m.cube(orbe, 'orbe_noyau', (5, 40.5, -2), (2, 2, 2), Mat(P_ORBE, level=0.98, noise=0.02, glow=True))
    # grimoire flottant a gauche
    bg = m.bones['bras_gauche']
    gr = m.bone('grimoire', (-8, 20, -4), co)
    m.cube(gr, 'grimoire_couverture', (-11, 18, -5), (6, 8, 1), Mat(pal('#4a1010', '#6a1a1a', '#8a2828'), level=0.5, noise=0.2, decos=[]))
    m.cube(gr, 'grimoire_pages', (-10.5, 18.5, -4), (5, 7, 1), Mat(pal('#c8b890', '#e0d0a8', '#f4e8c4'), level=0.6, noise=0.1, glow=True, decos=[]))
    m.cube(gr, 'grimoire_runes', (-10, 19, -3.95), (4, 6, 0), Decal(lambda f: [f.px(x, y, CYAN, glow=True) for y in range(1, f.h, 2) for x in range(1, f.w - 1)]))
    return m


COLORS = cc.COLORS


def animations(m):
    P = ek.key_poses

    def extra(a, length):
        q = length / 4.0
        a.pos('grimoire', [(0, 0, 0, 0), (q, 0, 1.2, 0), (2 * q, 0, 0, 0), (3 * q, 0, -1.2, 0), (length, 0, 0, 0)])
        a.rot('grimoire', [(0, 0, 0, 0), (q * 2, 0, 20, 0), (length, 0, 0, 0)], 'catmullrom')
        a.pos('effet', [(0, 0, 0, 0), (q, 0, 0.8, 0), (2 * q, 0, 0, 0), (3 * q, 0, -0.8, 0), (length, 0, 0, 0)])
        a.scale('effet', [(0, 1, 1, 1), (q, 1.15, 1.15, 1.15), (2 * q, 1, 1, 1), (3 * q, 1.15, 1.15, 1.15), (length, 1, 1, 1)])
        a.rot('chapeau_haut', [(0, 0, 0, 0), (q * 2, 3, 0, 2), (length, 0, 0, 0)])
    cc.base_idle(m, extra)
    # presentation : leve le baton, l'orbe grossit, le grimoire s'ouvre
    a = m.anim('present', 2.4, 'once')
    P(a, [(0, {}), (0.6, {'bras_droit': (-35, 0, -8), 'bras_gauche': (-30, 0, 10), 'tete': (-10, 0, 0), 'corps': (-3, 0, 0)}),
          (1.3, {'bras_droit': (-45, 0, -8), 'bras_gauche': (-40, 0, 10), 'tete': (-14, 0, 0), 'corps': (-5, 0, 0)}),
          (2.4, {})])
    a.scale('effet', [(0, 1, 1, 1), (0.6, 1.5, 1.5, 1.5), (1.3, 2.0, 2.0, 2.0), (2.4, 1, 1, 1)])
    a.rot('grimoire', [(0, 0, 0, 0), (0.8, 0, 160, 0), (1.6, 0, 360, 0), (2.4, 0, 360, 0)], 'catmullrom')
