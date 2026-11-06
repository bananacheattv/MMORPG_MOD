#!/usr/bin/env python3
"""Chevre celeste : corps trapu dans une toison de nuage, cornes d'or enroulees, barbiche, pattes fines et sabots dores. Siege 1,45. Saut plus fort en jeu (inchange)."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede
from kit import CF, CR, FER, OR, PAL, oeil, patte, selle, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('chevre_celeste', density=2, kind='mounts', seed=41, description='Chevre celeste Aeloria : refonte.')
    p.meta['y_corps'] = (10, 32)
    W = ('laine', 'toison')
    FUR = ('fourrure', 'toison')
    SEAT = selle_y(1.45)                                  # 24
    p.bone('body', (0, 18, 0))
    p.cube('body', 'toison', (-5, 13, -8), (5, 22.5, 8), W)
    p.cube('body', 'toison_av', (-4, 14.5, -10.5), (4, 23, -8), W, ton=.4)
    p.cube('body', 'toison_ar', (-4, 14, 8), (4, 21.5, 10.5), W, ton=.2)
    p.cube('body', 'toison_ventre', (-4, 12, -7), (4, 13, 7), W, ton=-.6)
    p.sym('body', 'bouffant_a', (5, 15, -7.5), (6, 21, -3.5), W, ton=.3)
    p.sym('body', 'bouffant_b', (5, 14, 3), (6, 20, 7.5), W, ton=.2)
    p.cube('body', 'bouffant_c', (-3, 22.5, 7), (3, 23.5, 10), W, ton=.6)
    # petites etoiles d'or dans la toison
    ETO = ['.t.', 'ttt', '.t.']
    p.decal('toison', 'east', stamp(ETO, PAL, 6.5, 16))
    p.decal('toison', 'east', stamp(ETO, PAL, -5.5, 15))
    p.decal('toison', 'west', stamp(ETO, PAL, -6.5, 16, flip_on=()))
    p.decal('toison', 'west', stamp(ETO, PAL, 4.5, 15, flip_on=()))

    # cou et tete hauts
    p.bone('head', (0, 21, -9), 'body')
    N = dict(rot=(-18, 0, 0), pivot=(0, 21, -9))
    p.cube('head', 'cou', (-2.5, 19, -12), (2.5, 29, -7.5), ('fourrure', 'toison'), base=2.6, **N)
    p.cube('head', 'crane', (-3, 27, -17.5), (3, 32, -12), ('fourrure', 'toison'), base=3.2)
    p.cube('head', 'museau', (-2, 27, -21.5), (2, 29.5, -17.5), ('fourrure', 'toison'), base=2.8)
    p.cube('head', 'nez', (-1.5, 28, -22), (1.5, 29.5, -21.5), ('uni', 'gris_peau'), base=1.4)
    p.cube('head', 'barbiche', (-.5, 22.5, -21), (.5, 27, -18), ('crete', 'toison'), base=3, ecart=1.2, bas=True, pas=3, dent=3, hide={'north', 'south', 'up', 'down'}, no_shadow=True)
    p.decal('crane', 'east', stamp(['dddd', 'dbak', 'dddd'], PAL, -13.5, 30.5))
    p.decal('crane', 'west', stamp(['dddd', 'dbak', 'dddd'], PAL, -16.5, 30.5, flip_on=('west',)))
    p.bone('ear_l', (3, 30, -13.5), 'head')
    p.bone('ear_r', (-3, 30, -13.5), 'head')
    p.sym('ear_l|ear_r', 'oreille', (3, 29.5, -14), (8, 31, -13.5), ('plan', 'toison'), base=2.6, masque=['.######.', '########', '.#####..'], rot=(0, -10, -14), pivot=(3, 30, -13.5), **CF)
    # cornes d'or enroulees : quatre segments qui tournent vers l'arriere puis le dehors
    C = ('corne', 'or_corne')
    p.sym('head', 'corne_a', (1.5, 31.5, -15), (3.5, 36, -13), C, base=1.6, rot=(24, 0, -10), pivot=(2.5, 31.5, -14))
    p.sym('head', 'corne_b', (2.5, 35, -12.5), (4, 39.5, -11), C, base=2.2, rot=(60, 0, -14), pivot=(3, 35, -12))
    p.sym('head', 'corne_c', (3, 37, -9), (4.5, 41, -7.5), C, base=2.8, rot=(110, 0, -22), pivot=(3.5, 37, -8.5))
    p.sym('head', 'corne_d', (3.5, 35.5, -5.5), (4.5, 38.5, -4.5), C, base=3.4, rot=(165, 0, -30), pivot=(4, 35.5, -5))
    # queue courte
    p.bone('tail', (0, 21, 10.5), 'body')
    p.cube('tail', 'queue', (-1.5, 20, 10.5), (1.5, 24, 12.5), W, ton=.6, rot=(26, 0, 0), pivot=(0, 21, 11))
    # pattes fines, sabots d'or
    SAB = OR
    PA = ('fourrure', 'toison')
    patte(p, 'leg_fl', 'leg_fr', 'av', 1.5, 3.5, -7.5, -4, 15, 7.5, PA, pied=SAB, pied_h=2, avance=.5, ton=-1.2, ton_bas=-1.6, large_bas=.5)
    patte(p, 'leg_bl', 'leg_br', 'ar', 1.5, 3.5, 4, 7.5, 15, 8, PA, pied=SAB, pied_h=2, avance=.5, ton=-1.2, ton_bas=-1.6, large_bas=.5)
    selle(p, SEAT, 22.5, 5, 13, 'rare', av=-5.5, ar=6.5, bas=14.5)
    rig = tuiles_rig(ears=('ear_l', 'ear_r'), walk=.72, walk_amp=26, walk_knee=48, run=.44, idle=3.0, leg_len=15.0)
    add_all(p, quadrupede(rig, poids=.9))
    return p
