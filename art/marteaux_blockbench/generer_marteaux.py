#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Six marteaux de guerre (niveaux 1, 20, 40, 60, 80, 100) pour Blockbench, format "Java Block/Item".
Fichiers : marteau_niv_01 ... marteau_niv_100 (.bbmodel + texture PNG + modele d'item Java exporte).

Usage :  python generer_marteaux.py      (Python 3.8+, Pillow ; utilise ../blockbench_kit.py)

Construction (vue de face, plan XY) : manche vertical centre en x = z = 8, tete horizontale en haut,
deux faces de frappe planes aux extremites -X et +X. Angles de rotation classiques uniquement (-45..45 par pas de 22,5).
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
import blockbench_kit as bk  # noqa: E402

ICI = os.path.dirname(os.path.abspath(__file__))
NS = 'mmorpg'
COLORS = {'manche': 1, 'poignee': 2, 'pommeau': 3, 'tete': 4, 'renforts': 6, 'cristaux': 5, 'fragments': 7}


def face_plate(m, group, name, x, y1, y2, z1, z2, side, mat='rune', deco=None, light=15):
    """Plaque posee sur une face de frappe (east = +X, west = -X) ou sur une face avant/arriere."""
    return bk.plate(m, group, name, side, z1, y1, z2, y2, x, mat, light=light, deco=deco)


def front_back_plates(m, group, name, x1, y1, x2, y2, z_front, z_back, mat='rune', deco=None, light=15):
    bk.plate(m, group, f'{name}_avant', 'north', x1, y1, x2, y2, z_front - 0.1, mat, light=light, deco=deco)
    bk.plate(m, group, f'{name}_arriere', 'south', x1, y1, x2, y2, z_back + 0.1, mat, light=light, deco=deco)


def mirror_x(x1, x2):
    return 16 - x2, 16 - x1


def both_heads(fn):
    fn('gauche', lambda a, b: (a, b))
    fn('droite', mirror_x)


# ============================================================================================== niveaux
def marteau_01():
    m = bk.Model('marteau_niv_01', 1, 'Marteau du conscrit')
    bk.col(m, 'manche', 'manche', -5, 20, 2, 'bois')
    bk.col(m, 'poignee', 'cuir', -5, 3, 2.5, 'cuir')
    bk.col(m, 'poignee', 'bourrelet_haut', 2.5, 3, 2.9, 'cuir')
    bk.col(m, 'poignee', 'bourrelet_bas', -5, -4.5, 2.9, 'cuir')
    bk.col(m, 'pommeau', 'pommeau', -6.5, -5, 2.75, 'fer')
    bk.col(m, 'pommeau', 'pommeau_bout', -7, -6.5, 2.25, 'fer')
    bk.box(m, 'tete', 'tete', 3, 20, 5.5, 13, 25, 10.5, 'fer')
    bk.col(m, 'tete', 'collier', 19, 20, 3, 'fer')
    bk.col(m, 'tete', 'coin', 25, 26, 1.5, 'fer')
    for side, mx in (('gauche', lambda a, b: (a, b)), ('droite', mirror_x)):
        x1, x2 = mx(3, 4)
        bk.box(m, 'renforts', f'bande_{side}', x1, 19.75, 5.25, x2, 25.25, 10.75, 'metal')
    return m, (8, -1.0)


def marteau_20():
    m = bk.Model('marteau_niv_20', 20, 'Marteau renforcé')
    bk.col(m, 'manche', 'manche', -7, 21, 2, 'bois')
    for i, y in enumerate((10, 15)):
        bk.col(m, 'renforts', f'bague_manche_{i + 1}', y, y + 0.75, 2.5, 'argent')
    bk.col(m, 'poignee', 'cuir', -7, 3, 2.5, 'cuir_tresse')
    bk.col(m, 'poignee', 'bague_haut', 3, 3.75, 2.9, 'argent')
    bk.col(m, 'poignee', 'bague_bas', -7.5, -7, 2.9, 'argent')
    bk.col(m, 'pommeau', 'pommeau', -9.5, -7.5, 3.2, 'metal')
    bk.col(m, 'pommeau', 'pommeau_bout', -10, -9.5, 2.6, 'argent')
    bk.diamond_plate(m, 'cristaux', 'pierre_pommeau', (8, -8.5), 1.4, 0.7, 6.4, 9.6, gem_mat='cristal', gem_light=10)
    bk.box(m, 'tete', 'tete', 2, 21, 4.75, 14, 28.5, 11.25, 'metal', deco={'up': None})
    bk.col(m, 'renforts', 'collier', 20, 21, 3.25, 'argent')
    bk.box(m, 'renforts', 'chapeau', 6, 28.5, 5.5, 10, 29.25, 10.5, 'argent')
    for side, mx in (('gauche', lambda a, b: (a, b)), ('droite', mirror_x)):
        x1, x2 = mx(1, 2.25)
        bk.box(m, 'renforts', f'face_de_frappe_{side}', x1, 20.5, 4.25, x2, 29, 11.75, 'argent')
        x1, x2 = mx(4.5, 5.25)
        bk.box(m, 'renforts', f'cerclage_{side}', x1, 20.75, 4.5, x2, 28.75, 11.5, 'argent')
    bk.diamond_plate(m, 'cristaux', 'cristal_incruste', (8, 24.75), 2.4, 1.2, 4.75, 11.25, gem_mat='cristal', gem_light=12)
    return m, (8, -2.0)


def marteau_40():
    m = bk.Model('marteau_niv_40', 40, 'Marteau runique')
    bk.col(m, 'manche', 'manche', -8, 20.5, 2, 'bois')
    for i, y in enumerate((9.5, 13.5, 18.5)):
        bk.col(m, 'renforts', f'bague_manche_{i + 1}', y, y + 0.6, 2.6, 'argent')
    bk.col_plates(m, 'manche/runes', 'runes_manche', 14.4, 18.2, 2, 0.3, 'rune', sides='NS')
    bk.col(m, 'poignee', 'cuir', -8, 3, 2.5, 'cuir_tresse')
    bk.col(m, 'poignee', 'bague_haut', 3, 3.75, 3.0, 'argent')
    bk.col(m, 'poignee', 'bague_bas', -8.5, -8, 3.0, 'argent')
    bk.col(m, 'pommeau', 'pommeau', -10.75, -8.5, 3.4, 'metal')
    bk.col(m, 'pommeau', 'pommeau_bout', -11.25, -10.75, 2.6, 'argent')
    bk.diamond_plate(m, 'cristaux', 'gemme_pommeau', (8, -9.6), 1.6, 0.8, 6.3, 9.7, gem_mat='gemme', gem_light=12)
    bk.col(m, 'renforts', 'collier', 20, 21, 3.4, 'argent')
    bk.box(m, 'tete', 'centre', 5, 21, 4.5, 11, 29, 11.5, 'metal')
    bk.box(m, 'renforts', 'chapeau', 5.5, 29, 5.25, 10.5, 29.75, 10.75, 'argent')
    bk.diamond_plate(m, 'cristaux', 'cristal_central', (8, 25), 3.6, 1.9, 4.5, 11.5, gem_mat='cristal', gem_light=15)

    def head(side, mx):
        x1, x2 = mx(0, 4.5)
        bk.box(m, 'tete', f'masse_{side}', x1, 20.25, 3.75, x2, 29.75, 12.25, 'metal')
        x1, x2 = mx(-1, 0)
        bk.box(m, 'renforts', f'face_de_frappe_{side}', x1, 20.75, 4.25, x2, 29.25, 11.75, 'argent')
        x1, x2 = mx(4.25, 5.0)
        bk.box(m, 'renforts', f'bordure_{side}', x1, 19.75, 3.25, x2, 30.25, 12.75, 'argent')
        x1, x2 = mx(0.75, 3.75)
        front_back_plates(m, 'tete/runes', f'runes_{side}', x1, 22, x2, 28, 3.75, 12.25, deco='embleme')
    both_heads(head)
    return m, (8, -2.5)


def marteau_60():
    m = bk.Model('marteau_niv_60', 60, 'Marteau du bastion')
    bk.col(m, 'manche', 'manche', -8, 19.5, 2, 'bois')
    for i, (y1, y2) in enumerate(((8.5, 12.5), (13.5, 17.5))):
        bk.col(m, 'manche', f'fourreau_{i + 1}', y1, y2, 2.75, 'metal')
        bk.col(m, 'renforts', f'fourreau_{i + 1}_bord_bas', y1, y1 + 0.5, 3.0, 'argent')
        bk.col(m, 'renforts', f'fourreau_{i + 1}_bord_haut', y2 - 0.5, y2, 3.0, 'argent')
        bk.col_plates(m, 'manche/runes', f'runes_fourreau_{i + 1}', y1 + 0.75, y2 - 0.75, 2.75, 0.45, 'rune')
    bk.col_plates(m, 'manche/runes', 'runes_bois', 4.6, 8.3, 2, 0.3, 'rune', sides='NS')
    bk.col(m, 'poignee', 'cuir', -8, 4.25, 2.5, 'cuir_tresse')
    for i, y in enumerate((-8.5, -2.25, 4.25)):
        bk.col(m, 'poignee', f'bague_{i + 1}', y, y + 0.6, 3.0, 'argent')
    bk.col(m, 'pommeau', 'pommeau', -11, -8.5, 3.6, 'metal')
    bk.col(m, 'pommeau', 'pommeau_bout', -11.5, -11, 2.8, 'argent')
    bk.col(m, 'pommeau', 'sertissure', -10.4, -9.1, 3.9, 'argent', d=1.3)
    for z, sd in ((6.0, 'avant'), (10.0, 'arriere')):
        bk.cbox(m, 'cristaux', f'cristal_pommeau_{sd}', (8, -9.75, z), (1.4, 1.4, 0.3), 'cristal', light=15)
    bk.col(m, 'renforts', 'collier', 18.75, 19.5, 3.6, 'argent')
    bk.box(m, 'tete', 'noyau', 1, 19.5, 3.5, 15, 30.5, 12.5, 'metal')
    bk.box(m, 'renforts', 'couche_haut', 2, 30.5, 4, 14, 31.25, 12, 'argent')
    bk.box(m, 'renforts', 'couche_bas', 2, 18.75, 4, 14, 19.5, 12, 'argent')

    def head(side, mx):
        for i, (a, b, y1, y2, z1, z2, mat) in enumerate(((0, 1, 20, 30, 4, 12, 'argent'), (-1, 0, 20.75, 29.25, 4.75, 11.25, 'metal'),
                                                         (-1.75, -1, 21.5, 28.5, 5.5, 10.5, 'argent'))):
            x1, x2 = mx(a, b)
            bk.box(m, 'renforts', f'couche_{side}_{i + 1}', x1, y1, z1, x2, y2, z2, mat)
        x1, x2 = mx(3.5, 4.25)
        bk.box(m, 'renforts', f'sangle_{side}', x1, 19, 3, x2, 31, 13, 'argent')
        x1, x2 = mx(1.25, 3.25)
        front_back_plates(m, 'tete/runes', f'runes_{side}', x1, 21.5, x2, 28.5, 3.5, 12.5)
    both_heads(head)
    # grand cristal carre traversant, dans son cadre
    bk.box(m, 'cristaux', 'grand_cristal', 5.75, 22.75, 3.0, 10.25, 27.25, 13.0, 'cristal', light=15)
    for name, x1, y1, x2, y2 in (('cadre_haut', 5, 27.25, 11, 28), ('cadre_bas', 5, 22, 11, 22.75),
                                 ('cadre_gauche', 5, 22.75, 5.75, 27.25), ('cadre_droit', 10.25, 22.75, 11, 27.25)):
        bk.box(m, 'renforts', name, x1, y1, 3.25, x2, y2, 12.75, 'argent')
    return m, (8, -2.0)


def marteau_80():
    m = bk.Model('marteau_niv_80', 80, 'Marteau légendaire')
    bk.col(m, 'manche', 'manche', -8, 19.5, 2, 'bois')
    for i, (y1, y2) in enumerate(((8.5, 11.5), (12.5, 15.5), (16.5, 19.5))):
        bk.col(m, 'manche', f'fourreau_{i + 1}', y1, y2, 2.75, 'metal')
        bk.col(m, 'renforts', f'fourreau_{i + 1}_bord', y2 - 0.5, y2, 3.0, 'argent')
        bk.col_plates(m, 'manche/runes', f'runes_fourreau_{i + 1}', y1 + 0.4, y2 - 0.8, 2.75, 0.45, 'rune')
    for y1, y2 in ((4.6, 8.5), (11.5, 12.5), (15.5, 16.5)):
        bk.col_plates(m, 'manche/runes', 'canal_energie', y1, y2, 2, 0.7, 'energie', sides='NSEW')
    bk.col(m, 'poignee', 'cuir', -8, 4.25, 2.5, 'cuir_tresse')
    for i, y in enumerate((-8.5, -2.25, 4.25)):
        bk.col(m, 'poignee', f'bague_{i + 1}', y, y + 0.6, 3.0, 'argent')
    bk.col(m, 'pommeau', 'pommeau', -11.25, -8.5, 3.75, 'metal')
    bk.col(m, 'pommeau', 'pommeau_couronne', -10.6, -9.15, 4.5, 'argent')
    bk.col(m, 'pommeau', 'pommeau_bout', -11.75, -11.25, 2.8, 'argent')
    bk.col_plates(m, 'cristaux', 'cristal_pommeau', -10.5, -9.25, 4.5, 1.25, 'cristal', gap=0.05)
    # cadre segmente autour du noyau
    bk.box(m, 'tete', 'traverse_haute', 4, 29, 6, 12, 30.5, 10, 'metal')
    bk.box(m, 'renforts', 'chapeau', 5, 30.5, 6.5, 11, 31.25, 9.5, 'argent')
    bk.box(m, 'tete', 'traverse_basse', 4, 19.5, 6, 12, 21, 10, 'metal')
    for side, (x1, x2) in (('gauche', (3, 4.5)), ('droite', (11.5, 13))):
        bk.box(m, 'tete', f'montant_{side}', x1, 19.5, 5.5, x2, 30.5, 10.5, 'metal')
    bk.crystal(m, 'cristaux', 'noyau', 8, 25.0, 8, 3.6, 4.8)
    for name, x1, x2 in (('axe_gauche', -2.5, 3.0), ('axe_droit', 13.0, 18.5)):
        bk.box(m, 'tete', name, x1, 24.25, 7.25, x2, 25.75, 8.75, 'metal')
        front_back_plates(m, 'tete/runes', f'energie_{name}', x1 + 0.4, 24.75, x2 - 0.4, 25.25, 7.25, 8.75, mat='energie')

    def head(side, mx):
        segs = ((-4, -2.5, 19.5, 30.5, 2.5, 13.5, 'metal'), (-2.25, 0.25, 20.0, 30.0, 3.5, 12.5, 'argent'),
                (0.5, 3.0, 20.5, 29.5, 4.0, 12.0, 'metal'))
        for i, (a, b, y1, y2, z1, z2, mat) in enumerate(segs):
            x1, x2 = mx(a, b)
            bk.box(m, 'tete', f'plaque_{side}_{i + 1}', x1, y1, z1, x2, y2, z2, mat)
        x1, x2 = mx(-4, -2.5)
        bk.box(m, 'renforts', f'chanfrein_{side}', x1, 18.5, 3.5, x2, 31.5, 12.5, 'metal')
        x = mx(-4, -2.5)[0] if side == 'gauche' else mx(-4, -2.5)[1]
        face_plate(m, 'tete/runes', f'embleme_frappe_{side}', x - 0.1 if side == 'gauche' else x + 0.1, 22.5, 27.5, 5.5, 10.5,
                   'west' if side == 'gauche' else 'east', deco='embleme')
        x1, x2 = mx(-3.6, -2.9)
        front_back_plates(m, 'tete/runes', f'runes_{side}', x1, 21, x2, 29, 2.5, 13.5)
    both_heads(head)
    for i, c in enumerate(((2, 31.0, 4), (14, 31.0, 12), (-6.5, 22, 8), (22.5, 22, 8), (2, 16.5, 12), (14, 16.5, 4))):
        bk.frag(m, 'fragments', f'fragment_{i + 1}', c, 1.0, 'z' if i % 2 == 0 else 'x')
    return m, (8, -2.0)


def marteau_100():
    m = bk.Model('marteau_niv_100', 100, 'Marteau mythique')
    bk.col(m, 'manche', 'manche', -9, 17.0, 2.25, 'bois')
    for i, (y1, y2) in enumerate(((8.0, 11.0), (12.0, 15.0))):
        bk.col(m, 'manche', f'fourreau_{i + 1}', y1, y2, 3.0, 'metal')
        bk.col(m, 'renforts', f'fourreau_{i + 1}_bord', y1, y1 + 0.5, 3.25, 'argent')
        bk.col_plates(m, 'manche/runes', f'runes_fourreau_{i + 1}', y1 + 0.7, y2 - 0.3, 3.0, 0.5, 'rune')
    for y1, y2 in ((7.4, 8.0), (11.0, 12.0), (15.0, 16.5)):
        bk.col_plates(m, 'manche/runes', 'canal_energie', y1, y2, 2.25, 0.75, 'energie', sides='NSEW')
    # longue poignee a deux mains
    bk.col(m, 'poignee', 'cuir', -9, 7.4, 2.75, 'cuir_tresse')
    for i, y in enumerate((-9.5, -1.3, 7.4)):
        bk.col(m, 'poignee', f'bague_{i + 1}', y, y + 0.7, 3.4, 'argent')
    bk.col(m, 'pommeau', 'pommeau', -12.5, -9.5, 4.0, 'metal')
    bk.col(m, 'pommeau', 'pommeau_couronne', -11.75, -10.25, 4.75, 'argent')
    bk.col(m, 'pommeau', 'pommeau_bout', -13.0, -12.5, 3.0, 'argent')
    bk.col_plates(m, 'cristaux', 'cristal_pommeau', -11.6, -10.4, 4.75, 1.3, 'cristal', gap=0.05)
    bk.crystal(m, 'cristaux', 'cristal_pointe', 8, -14.4, 8, 2.0, 3.0)
    # tete monumentale : deux masses, ouverture centrale avec cristal suspendu
    bk.box(m, 'tete', 'poutre_haute', -1, 30, 5.5, 17, 32, 10.5, 'metal')
    bk.box(m, 'tete', 'poutre_basse', -1, 16.5, 5.5, 17, 18.5, 10.5, 'metal')
    bk.box(m, 'renforts', 'liseret_haut', 0, 29.6, 5.2, 16, 30.0, 10.8, 'argent')
    bk.box(m, 'renforts', 'liseret_bas', 0, 18.5, 5.2, 16, 18.9, 10.8, 'argent')
    for side, (x1, x2) in (('gauche', (2.0, 3.5)), ('droite', (12.5, 14.0))):
        bk.box(m, 'renforts', f'pilier_{side}', x1, 18.5, 6.0, x2, 30.0, 10.0, 'argent')
    bk.crystal(m, 'cristaux', 'cristal_suspendu', 8, 24.25, 8, 4.4, 8.0)
    for i, (cx, cy, ang) in enumerate(((4.7, 19.9, 45), (11.3, 19.9, -45), (4.7, 28.6, -45), (11.3, 28.6, 45))):
        bk.rbox(m, 'renforts', f'armature_{i + 1}', (cx, cy, 8.0), (2.4, 0.6, 1.2), ang, 'argent')

    def head(side, mx):
        x1, x2 = mx(-7, -1)
        bk.box(m, 'tete', f'masse_{side}', x1, 17.5, 2.0, x2, 31.0, 14.0, 'metal')
        bk.box(m, 'tete', f'masse_chanfrein_{side}', x1, 16.5, 3.0, x2, 32.0, 13.0, 'metal')
        x1, x2 = mx(-7.75, -7)
        bk.box(m, 'renforts', f'face_de_frappe_{side}', x1, 18.5, 3.0, x2, 30.0, 13.0, 'argent')
        x1, x2 = mx(-1.75, -1)
        bk.box(m, 'renforts', f'bordure_{side}', x1, 16.25, 1.75, x2, 32.0, 14.25, 'argent')
        x1, x2 = mx(-5.0, -4.25)
        bk.box(m, 'renforts', f'cerclage_{side}', x1, 16.25, 1.75, x2, 32.0, 14.25, 'argent')
        x = mx(-7.75, -7)[0] if side == 'gauche' else mx(-7.75, -7)[1]
        face_plate(m, 'tete/runes', f'embleme_frappe_{side}', x - 0.1 if side == 'gauche' else x + 0.1, 21.5, 27.0, 5.5, 10.5,
                   'west' if side == 'gauche' else 'east', deco='embleme')
        x1, x2 = mx(-3.9, -2.1)
        front_back_plates(m, 'tete/runes', f'runes_{side}', x1, 19.5, x2, 29.0, 2.0, 14.0)
        x1, x2 = mx(-6.6, -5.4)
        front_back_plates(m, 'tete/runes', f'energie_{side}', x1, 19.0, x2, 29.5, 2.0, 14.0, mat='energie')
    both_heads(head)
    for i, c in enumerate(((-10.5, 24.5, 8), (26.5, 24.5, 8), (-4, 13.8, 4), (20, 13.8, 12), (2.5, 14.5, 12.5), (13.5, 14.5, 3.5),
                           (-9.0, 30.5, 8), (25.0, 30.5, 8))):
        bk.frag(m, 'fragments', f'fragment_{i + 1}', c, 1.1 if i < 2 else 0.85, 'z' if i % 2 == 0 else 'x')
    return m, (8, -1.0)


MARTEAUX = [marteau_01, marteau_20, marteau_40, marteau_60, marteau_80, marteau_100]


def main():
    mc = os.path.join(ICI, 'minecraft', 'assets', NS)
    for sub in ('textures', 'apercu', os.path.join(mc, 'models', 'item'), os.path.join(mc, 'textures', 'item'), os.path.join(mc, 'items')):
        os.makedirs(os.path.join(ICI, sub), exist_ok=True)
    bk.write_json(os.path.join(ICI, 'minecraft', 'pack.mcmeta'),
                  {'pack': {'description': 'Marteaux Eldoria (6 evolutions)', 'min_format': 97, 'max_format': 97}}, indent=2)
    done = []
    for fn in MARTEAUX:
        m, grip = fn()
        atlas = bk.build_atlas([m], m.key)
        display = bk.handheld_display(m, grip, ref_height=34)
        atlas.save(os.path.join(ICI, 'textures', f'{m.key}.png'))
        atlas.save(os.path.join(mc, 'textures', 'item', f'{m.key}.png'))
        bk.write_json(os.path.join(ICI, f'{m.key}.bbmodel'), bk.bbmodel(m, atlas, m.key, NS, display, COLORS))
        bk.write_json(os.path.join(mc, 'models', 'item', f'{m.key}.json'),
                      bk.java_model(m, atlas, f'{NS}:item/{m.key}', display, COLORS, 'Eldoria MMORPG - generer_marteaux.py'))
        bk.write_json(os.path.join(mc, 'items', f'{m.key}.json'), {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{m.key}'}}, indent=2)
        bk.views(m, atlas, os.path.join(ICI, 'apercu', f'{m.key}_vues.png'), f'{m.titre} — niveau {m.level}  ({len(m.parts)} cubes)')
        x1, x2, y1, y2 = bk.bbox(m)
        print(f'{m.key:16s} niveau {m.level:3d}  {len(m.parts):3d} cubes  texture {atlas.width}x{atlas.height}  '
              f'taille {x2 - x1:.1f} x {y2 - y1:.1f}  icone x{display["gui"]["scale"][0]}')
        done.append((m, atlas, f'NIV. {m.level}', m.titre))
    bk.lineup(done, os.path.join(ICI, 'apercu', 'evolution.png'), 'MARTEAUX — ÉVOLUTION (BLOCKBENCH, JAVA BLOCK/ITEM)', min_w=260)
    print('Apercu : apercu/evolution.png')


if __name__ == '__main__':
    main()
