#!/usr/bin/env python3
"""Hippogriffe : tete d'aigle blanche et bec d'or, poitrail de plumes, serres devant, sabots et croupe de cheval derriere, grandes ailes articulees, queue de plumes. Siege 1,6. Vole : clip fly, ailes pliees au sol."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, ajoute_vol, quadrupede
from kit import CF, CR, FER, OR, PAL, patte, selle, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('hippogriffe', density=2, kind='mounts', seed=121, description='Hippogriffe Aeloria : refonte.')
    p.meta['y_corps'] = (10, 34)
    PB = ('plumes', 'plume_blanche')
    FUR = ('fourrure', 'daim')
    SEAT = selle_y(1.6)                                   # 26,5
    p.bone('body', (0, 20, 0))
    p.cube('body', 'poitrail', (-5, 15, -12), (5, 26, -5), PB, l=6, h=4, ton=.3)
    p.cube('body', 'ventre', (-4.5, 16.5, -5), (4.5, 25, 7), FUR)
    p.cube('body', 'croupe', (-5, 16, 7), (5, 25.5, 13.5), FUR, ton=.2)
    p.cube('body', 'croupe_bout', (-3.5, 17.5, 13.5), (3.5, 24.5, 15), FUR)
    p.sym('body', 'jabot', (5, 15.5, -11.5), (5.5, 24, -6), ('crete', 'plume_blanche'), base=3, ecart=1.6, bas=True, pas=5, dent=4, **CR)
    p.cube('body', 'jabot_devant', (-4, 11, -13), (4, 17, -12.5), ('plan', 'plume_blanche'), base=3.6, masque=['########', '########', '.######.', '.######.', '..####..', '...##...'], **CF)
    # cou de plumes et tete d'aigle
    p.bone('head', (0, 25, -10), 'body')
    N = dict(rot=(-22, 0, 0), pivot=(0, 25, -10))
    p.cube('head', 'cou', (-3.5, 23, -14), (3.5, 34, -8), PB, l=5, h=3, ton=.5, **N)
    p.cube('head', 'crane', (-3.5, 32, -21.5), (3.5, 38.5, -14.5), PB, l=5, h=3, ton=1)
    p.cube('head', 'bec_haut', (-2.5, 33.5, -25), (2.5, 37, -21.5), ('uni', 'bec'), base=3.2)
    p.cube('head', 'bec_crochet', (-1.5, 31.5, -26.5), (1.5, 36.5, -24.5), ('uni', 'bec'), base=2.4, rot=(-12, 0, 0), pivot=(0, 36.5, -25))
    p.cube('head', 'bec_bas', (-2, 32, -24.5), (2, 33.5, -21.5), ('uni', 'bec'), base=1.8)
    p.decal('crane', 'east', stamp(['dddd', 'dYak', '.dkk'], PAL, -16, 37))
    p.decal('crane', 'west', stamp(['dddd', 'dYak', '.dkk'], PAL, -20, 37, flip_on=('west',)))
    p.bone('ear_l', (2.5, 38, -16), 'head')
    p.bone('ear_r', (-2.5, 38, -16), 'head')
    p.sym('ear_l|ear_r', 'aigrette', (1, 37.5, -16.5), (4.5, 42, -16), ('plan', 'plume_blanche'), base=4, masque=['....##..', '...###..', '..####..', '.#####..', '.####...', '.###....'], rot=(26, 0, 0), pivot=(2.5, 38, -16), **CF)
    # ailes : couvertures et grandes plumes, os historiques wing_l, wing_r, bouts articules
    PA = ('aile_plumes', 'plume_blanche')
    for w, t, s in (('wing_l', 'wing_l_bout', 1), ('wing_r', 'wing_r_bout', -1)):
        p.bone(w, (s * 5, 25, -7), 'body')
        p.bone(t, (s * 17, 25.5, -7), w)
    p.sym('wing_l|wing_r', 'aile_bras', (5, 24.5, -8.5), (17, 26, -5), PB, l=5, h=3, ton=.6)
    p.sym('wing_l|wing_r', 'aile_plan', (5, 25.5, -5), (17, 26, 12), PA, pas=6, dent=7, hide={'north', 'south', 'east', 'west'}, no_shadow=True)
    p.sym('wing_l_bout|wing_r_bout', 'bout_bras', (17, 24.5, -8), (29, 26, -5.5), PB, l=5, h=3, ton=.8)
    p.sym('wing_l_bout|wing_r_bout', 'bout_plan', (17, 25.5, -5.5), (32, 26, 8), PA, pas=6, dent=9, ton=-.2, hide={'north', 'south', 'east', 'west'}, no_shadow=True)
    # queue de plumes
    p.bone('tail', (0, 23, 15), 'body')
    p.cube('tail', 'queue', (-1.5, 21.5, 15), (1.5, 24.5, 21), FUR, rot=(24, 0, 0), pivot=(0, 23, 15), ton=.2)
    p.bone('tail_bout', (0, 20.5, 20.5), 'tail')
    p.cube('tail_bout', 'queue_plan', (-3, 18, 19.5), (3, 18.5, 31), ('aile_plumes', 'plume_blanche'), pas=4, dent=6, rot=(40, 0, 0), pivot=(0, 20.5, 20.5), hide={'north', 'south', 'east', 'west'}, no_shadow=True)
    # serres devant, sabots derriere
    SE = ('uni', 'bec')
    patte(p, 'leg_fl', 'leg_fr', 'av', 2, 4.5, -11, -7, 19, 10, PB, pied=SE, pied_h=2, avance=1.5, ton=.2, ton_bas=-.2, griffes=('griffe', 'chitine_sombre'))
    patte(p, 'leg_bl', 'leg_br', 'ar', 1.5, 4.5, 8, 13, 20, 10.5, FUR, pied=('uni', 'sabot'), pied_h=2, avance=.5, ton=-.1, ton_bas=-.4)
    selle(p, SEAT, 25, 4.5, 16.5, 'mythique', av=-5, ar=6.5, bas=17.5)
    rig = tuiles_rig(ears=('ear_l', 'ear_r'), tail_tip='tail_bout', wings=('wing_l', 'wing_r'), wing_tips=('wing_l_bout', 'wing_r_bout'), wing_fold=(-78, -4, 66), tip_fold=(-6, 0),
                     idle=3.0, walk=.8, walk_amp=26, walk_knee=50, run=.46, leg_len=19.0)
    clips = quadrupede(rig, poids=.9)
    ajoute_vol(clips, rig, .7)
    add_all(p, clips)
    return p
