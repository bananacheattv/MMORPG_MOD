#!/usr/bin/env python3
"""Raptor des sables : port bipede, echine horizontale, longue queue rigide, petits bras griffus, crete de plumes turquoise, robe rayee. Siege 1,39."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede
from kit import CF, CR, FER, OR, PAL, oeil, selle, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('raptor_sables', density=2, kind='mounts', seed=81, description='Raptor des sables Aeloria : refonte.')
    p.meta['y_corps'] = (10, 32)
    R = ('rayures', 'sable')
    SEAT = selle_y(1.39)                                  # 23
    PL = ('crete', 'turquoise')
    CD = CR
    p.bone('body', (0, 18, 0))
    p.cube('body', 'poitrail', (-3.5, 14, -11), (3.5, 22, -5), R, y0=18)
    p.cube('body', 'ventre', (-4, 14.5, -5), (4, 21.5, 7.5), R, y0=18)
    p.cube('body', 'croupe', (-3.5, 15, 7.5), (3.5, 21, 11), R, y0=18)
    p.cube('body', 'gorge', (-2.5, 13, -10), (2.5, 14, -4), ('fourrure', 'creme'), ton=.6)
    # cou et tete
    p.bone('head', (0, 20, -10), 'body')
    N = dict(rot=(-34, 0, 0), pivot=(0, 20, -10))
    p.cube('head', 'cou', (-2.5, 17, -13), (2.5, 28, -8.5), R, y0=0, **N)
    p.cube('head', 'crane', (-3, 25.5, -22), (3, 31, -15.5), R, y0=0, pas=6, large=2)
    p.cube('head', 'museau', (-2, 26, -30), (2, 29.5, -22), R, y0=0, pas=6, large=2, ton=.3)
    p.bone('machoire', (0, 26.5, -21), 'head')
    p.cube('machoire', 'mandibule', (-1.5, 24.5, -29), (1.5, 26, -20), ('fourrure', 'creme'), ton=.2)
    p.decal('crane', 'east', stamp(['dddd', 'dYak', '.dkk'], PAL, -17, 30))
    p.decal('crane', 'west', stamp(['dddd', 'dYak', '.dkk'], PAL, -20.5, 30, flip_on=('west',)))
    p.decal('museau', 'east', stamp(['k.k.k.k.k.k.k.k.'], PAL, -22, 26.5))
    p.decal('museau', 'west', stamp(['k.k.k.k.k.k.k.k.'], PAL, -30, 26.5, flip_on=()))
    # crete de plumes de la tete au garrot
    p.cube('head', 'crete_tete', (-.5, 30.5, -21), (.5, 36, -13), PL, base=2, ecart=3, pas=5, dent=4, rot=(16, 0, 0), pivot=(0, 31, -15), **CD)
    p.cube('head', 'crete_cou', (-.5, 24, -10), (.5, 30, -3), PL, base=2, ecart=3, pas=5, dent=4, rot=(-34, 0, 0), pivot=(0, 20, -10), **CD)
    # petits bras griffus (os historiques leg_fl, leg_fr)
    p.bone('leg_fl', (3.5, 16.5, -7), 'body')
    p.bone('leg_fr', (-3.5, 16.5, -7), 'body')
    p.sym('leg_fl|leg_fr', 'bras', (3.5, 11, -8), (5, 17, -6.5), R, y0=0, ton=.2, rot=(24, 0, 0), pivot=(4.25, 16.5, -7.25))
    p.sym('leg_fl|leg_fr', 'griffes', (3.5, 9.5, -10.5), (5, 11.5, -9), ('griffe', 'ivoire'), rot=(40, 0, 0), pivot=(4.25, 11.5, -9.75))
    # queue rigide en deux os, fin de plumes
    p.bone('tail', (0, 19, 11), 'body')
    p.cube('tail', 'queue', (-2.5, 16.5, 11), (2.5, 21, 21), R, y0=0, ton=.1)
    p.bone('tail_bout', (0, 19, 21), 'tail')
    p.cube('tail_bout', 'queue_bout', (-1.5, 17.5, 21), (1.5, 20.5, 30), R, y0=0, ton=.3)
    p.sym('tail_bout', 'plumes_queue', (1.5, 18.5, 26), (2, 19.5, 34), ('uni', 'turquoise'), base=3.2, rot=(0, 16, 0), pivot=(1.75, 19, 26))
    p.cube('tail_bout', 'plume_haut', (-.5, 20.5, 24), (.5, 24, 33), PL, base=2, ecart=3, pas=5, dent=3, **CD)
    # grandes pattes (os historiques leg_bl, leg_br) : cuisse, jarret en arriere, canon en avant, pied griffu
    p.bone('leg_bl', (4.5, 19, 4))
    p.bone('leg_br', (-4.5, 19, 4))
    p.bone('leg_bl_bas', (4.5, 11, 6), 'leg_bl')
    p.bone('leg_br_bas', (-4.5, 11, 6), 'leg_br')
    p.sym('leg_bl|leg_br', 'cuisse', (3, 10.5, 1), (6.5, 20, 7), R, y0=0, hide={'up'})
    p.sym('leg_bl_bas|leg_br_bas', 'jarret', (3.5, 5, 5.5), (6, 11, 8.5), R, y0=0, ton=-.6, hide={'up'}, rot=(14, 0, 0), pivot=(4.75, 11, 6))
    p.sym('leg_bl_bas|leg_br_bas', 'canon', (3.5, 1.5, 3), (6, 6.5, 5.5), ('uni', 'sable_sombre'), base=1.8)
    p.sym('leg_bl_bas|leg_br_bas', 'pied', (3, 0, -.5), (6.5, 2, 6), ('uni', 'sable_sombre'), base=1.4, hide={'down'})
    for i, xx in enumerate((3, 4.5, 6)):
        p.sym('leg_bl_bas|leg_br_bas', f'griffe{i}', (xx, 0, -2), (xx + .5, 1.5, -.5), ('griffe', 'ivoire'), hide={'down', 'south'})
    selle(p, SEAT, 21.5, 4, 14.5, 'epique', av=-5, ar=6.5, bas=15, etriers=True)
    rig = tuiles_rig(jaw='machoire', tail_tip='tail_bout', ears=(), legs=[('leg_fl', None, 0.0, .25), ('leg_fr', None, .5, .25), ('leg_bl', 'leg_bl_bas', .0, 1.1, 1.0), ('leg_br', 'leg_br_bas', .5, 1.1, 1.0)],
                     idle=3.0, walk=.6, walk_amp=30, walk_knee=56, run=.4, run_amp=44, run_knee=70, leg_len=19.0)
    add_all(p, quadrupede(rig, poids=.9))
    return p
