#!/usr/bin/env python3
"""Cerf sylvestre : haut sur pattes, poitrail etroit, long cou, grands bois feuillus, ecorce et mousse, fanion blanc. Siege 1,76."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede
from kit import CF, CR, FER, OR, PAL, oeil, patte, selle, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('cerf_sylvestre', density=2, kind='mounts', seed=31, description='Cerf sylvestre Aeloria : refonte.')
    p.meta['y_corps'] = (16, 36)
    FUR = ('fourrure', 'daim')
    ECORCE = ('poils_longs', 'ecorce')
    SEAT = selle_y(1.76)                                  # 29
    p.bone('body', (0, 24, 0))
    p.cube('body', 'poitrail', (-4, 18.5, -11), (4, 29, -5), FUR)
    p.cube('body', 'ventre', (-3.5, 20, -5), (3.5, 27.5, 7), FUR)
    p.cube('body', 'croupe', (-4, 19.5, 7), (4, 28.5, 12.5), FUR, ton=.2)
    p.cube('body', 'croupe_bout', (-3, 21, 12.5), (3, 27.5, 14), ('fourrure', 'creme'), ton=.4)
    p.cube('body', 'gorge', (-3, 17.5, -10), (3, 18.5, -5), ('fourrure', 'creme'), ton=.5)
    p.sym('body', 'epaule', (4, 20.5, -10), (4.5, 28, -5), ECORCE, base=3.4)
    p.sym('body', 'hanche', (4, 21, 7.5), (4.5, 27.5, 12), FUR, ton=.4)
    # criniere de feuilles le long du cou et du garrot
    p.cube('body', 'feuilles_garrot', (-.5, 28, -11), (.5, 32, -5), ('crete', 'feuillage'), base=1.5, ecart=3, pas=4, dent=3, **CR)

    # cou et tete
    p.bone('head', (0, 27, -9), 'body')
    N = dict(rot=(-26, 0, 0), pivot=(0, 27, -9))
    p.cube('head', 'cou', (-2.5, 25, -12), (2.5, 39, -7), FUR, ton=.1, **N)
    p.cube('head', 'cou_ecorce', (-3, 26, -12.5), (3, 33, -6.5), ECORCE, base=3.2, **N)
    p.cube('head', 'cou_feuilles', (-.5, 33, -7), (.5, 40, -6), ('crete', 'feuillage'), base=1.5, ecart=3, pas=4, dent=3, rot=(-26, 0, 0), pivot=(0, 27, -9), hide={'north', 'south', 'up', 'down'}, no_shadow=True)
    p.cube('head', 'crane', (-3, 36, -21), (3, 41, -15), FUR, ton=.5)
    p.cube('head', 'museau', (-2, 36, -26), (2, 38.5, -21), FUR, ton=.3)
    p.cube('head', 'truffe', (-1.5, 37, -26.5), (1.5, 38.5, -26), ('uni', 'gris_peau'), base=.8)
    p.cube('head', 'menton', (-1.5, 35, -25), (1.5, 36, -19), ('fourrure', 'creme'), ton=.6)
    oeil(p, 'crane', -16.5, -20.5, 39.5, couleur='s', clair='a')
    p.bone('ear_l', (3, 39.5, -16), 'head')
    p.bone('ear_r', (-3, 39.5, -16), 'head')
    O = ['..####..', '.######.', '########', '########', '.######.', '...##...']
    p.sym('ear_l|ear_r', 'oreille', (3, 38, -16.5), (7, 41, -16), ('plan', 'daim'), base=3.4, masque=O, rot=(0, -15, 15), pivot=(3, 39.5, -16), **CF)
    # grands bois : une perche, trois andouillers, des feuilles
    B = ('corne', 'ecorce')
    p.sym('head', 'bois_base', (2, 41, -17.5), (3, 47, -16.5), B, base=1.6, rot=(8, 0, -14), pivot=(2.5, 41, -17))
    p.sym('head', 'bois_perche', (3, 46, -17), (4, 53, -16), B, base=2.2, rot=(16, 0, -24), pivot=(3.5, 46, -16.5))
    p.sym('head', 'bois_sommet', (5.5, 52, -15), (6.5, 58, -14), B, base=2.6, rot=(-6, 0, 16), pivot=(6, 52, -14.5))
    p.sym('head', 'andouiller_a', (3, 44, -18), (4, 49, -17), B, base=2.0, rot=(-46, 0, -20), pivot=(3.5, 44, -17.5))
    p.sym('head', 'andouiller_b', (5, 49, -17), (6, 54, -16), B, base=2.2, rot=(-34, 0, -48), pivot=(5.5, 49, -16.5))
    p.sym('head', 'andouiller_c', (5.5, 53, -15), (6.5, 57, -14), B, base=2.6, rot=(30, 0, -52), pivot=(6, 53, -14.5))
    F = ('uni', 'feuillage')
    L = ['...##...', '..####..', '.######.', '########', '.######.', '..####..', '...##...']
    p.sym('head', 'feuilles_a', (7, 54, -14.5), (11, 58, -14), ('plan', 'feuillage'), base=3.2, masque=L, rot=(20, 40, 0), pivot=(9, 56, -14), **CF)
    p.sym('head', 'feuilles_b', (4, 57, -15.5), (8, 61, -15), ('plan', 'feuillage'), base=3.8, masque=L, rot=(-10, -30, 12), pivot=(6, 59, -15), **CF)
    p.sym('head', 'feuilles_c', (7, 49, -21), (11, 53, -20.5), ('plan', 'feuillage'), base=2.6, masque=L, rot=(-14, 26, 0), pivot=(9, 51, -20.5), **CF)

    # queue : petit fanion blanc
    p.bone('tail', (0, 26.5, 14), 'body')
    p.cube('tail', 'queue', (-1.5, 25, 14), (1.5, 29, 15.5), ('fourrure', 'creme'), ton=1.2, rot=(30, 0, 0), pivot=(0, 26.5, 14.5))

    # pattes longues et fines, sabots sombres
    S = ('uni', 'sabot')
    patte(p, 'leg_fl', 'leg_fr', 'av', 1.5, 3.5, -9.5, -6, 20, 10.5, FUR, pied=S, pied_h=2, avance=.5, ton=-.1, ton_bas=-.3)
    patte(p, 'leg_bl', 'leg_br', 'ar', 1.5, 3.5, 7.5, 11.5, 21, 11.5, FUR, pied=S, pied_h=2, avance=.5, ton=-.1, ton_bas=-.3)
    selle(p, SEAT, 27.5, 3.5, 20, 'rare', av=-5, ar=6, bas=20.5)
    rig = tuiles_rig(ears=('ear_l', 'ear_r'), walk=.85, walk_amp=24, walk_knee=48, run=.5, idle=3.2, leg_len=20.0)
    add_all(p, quadrupede(rig, poids=.8))
    return p
