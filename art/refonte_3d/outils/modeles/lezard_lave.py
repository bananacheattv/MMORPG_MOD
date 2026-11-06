#!/usr/bin/env python3
"""Lezard de lave : corps rampant, pattes ecartees, basalte fendu de lave, crete dorsale, tete plate a gueule rougeoyante, longue queue en deux os. Siege 0,95."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede
from kit import CF, CR, FER, OR, PAL, oeil, selle, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('lezard_lave', density=2, kind='mounts', seed=71, description='Lezard de lave Aeloria : refonte.')
    p.meta['y_corps'] = (4, 18)
    B = 'basalte'
    SEAT = selle_y(.95)                                   # 16
    p.bone('body', (0, 10, 0))
    p.cube('body', 'epaules', (-6.5, 5.5, -12), (6.5, 14.5, -5), B)
    p.cube('body', 'dos', (-6, 6, -5), (6, 14.5, 7), B)
    p.cube('body', 'hanches', (-6.5, 6, 7), (6.5, 13.5, 13), B)
    p.cube('body', 'ventre', (-5, 4.5, -11), (5, 5.5, 12), ('nacre', 'basalte'), base=2.2, h=5)
    # crete dorsale en plans decoupes, pointes incandescentes
    CD = dict(hide={'north', 'south', 'up', 'down'}, no_shadow=True)
    p.cube('body', 'crete_epaules', (-.5, 14.5, -12), (.5, 18.5, -6), ('crete', 'basalte'), base=1, ecart=3.6, pas=4, dent=3, **CD)
    p.cube('body', 'crete_hanches', (-.5, 13.5, 7), (.5, 17.5, 13), ('crete', 'basalte'), base=1, ecart=3.6, pas=4, dent=3, **CD)
    # tete
    p.bone('head', (0, 9.5, -12), 'body')
    p.cube('head', 'cou', (-4.5, 6, -16), (4.5, 12.5, -11), B)
    p.cube('head', 'crane', (-5, 6.5, -22), (5, 12, -15.5), B)
    p.cube('head', 'museau', (-3.5, 6.5, -28), (3.5, 10, -22), B)
    p.bone('machoire', (0, 7, -18), 'head')
    p.cube('machoire', 'mandibule', (-3.5, 4.5, -27.5), (3.5, 6.5, -17), B)
    p.cube('head', 'gueule', (-3, 6, -27), (3, 6.5, -18), ('uni', 'basalte'), base=5.2)
    p.decal('crane', 'east', stamp(['dddd', 'dYRk', '.ddd'], PAL, -17, 11))
    p.decal('crane', 'west', stamp(['dddd', 'dYRk', '.ddd'], PAL, -21, 11, flip_on=('west',)))
    p.sym('head', 'corne', (3.5, 11, -18), (5, 15, -16.5), B, rot=(30, 0, -16), pivot=(4.25, 11, -17.25))
    p.cube('head', 'crete_tete', (-.5, 12, -22), (.5, 15, -15), ('crete', 'basalte'), base=1, ecart=3.6, pas=4, dent=2, **CD)
    # queue en deux os
    p.bone('tail', (0, 10, 13), 'body')
    p.cube('tail', 'queue', (-4, 7, 13), (4, 12.5, 22), B)
    p.cube('tail', 'crete_queue', (-.5, 12.5, 13), (.5, 15, 22), ('crete', 'basalte'), base=1, ecart=3.6, pas=4, dent=2, **CD)
    p.bone('tail_bout', (0, 9.5, 22), 'tail')
    p.cube('tail_bout', 'queue_bout', (-2.5, 7.5, 22), (2.5, 11, 31), B, ton=.4)
    p.cube('tail_bout', 'queue_pointe', (-1, 8, 31), (1, 10, 37), ('uni', 'basalte'), base=4.6)
    # pattes ecartees : bras horizontal, avant-bras vertical, griffes
    for g, d, n, z0, z1, a in (('leg_fl', 'leg_fr', 'av', -11, -6, 16), ('leg_bl', 'leg_br', 'ar', 7.5, 12.5, -16)):
        zc = (z0 + z1) / 2
        p.bone(g, (6, 9, zc))
        p.bone(d, (-6, 9, zc))
        p.bone(g + '_bas', (11, 8, zc), g)
        p.bone(d + '_bas', (-11, 8, zc), d)
        p.sym(f'{g}|{d}', n + '_bras', (5.5, 6.5, z0), (12, 10.5, z1), B, hide={'west'})
        p.sym(f'{g}_bas|{d}_bas', n + '_avantbras', (9, 1.5, z0 + .5), (12.5, 8, z1 - .5), B, ton=.2)
        p.sym(f'{g}_bas|{d}_bas', n + '_pied', (8.5, 0, z0 - 1.5), (13, 1.5, z1), B, ton=.4, hide={'down'})
        for i, xx in enumerate((8.5, 10, 11.5)):
            p.sym(f'{g}_bas|{d}_bas', f'{n}_griffe{i}', (xx, 0, z0 - 2.5), (xx + 1, 1, z0 - 1.5), ('griffe', 'basalte'), base=4, hide={'down', 'south'})
    selle(p, SEAT, 14.5, 6, 6, 'epique', av=-5, ar=6.5, bas=7.5, etriers=False)
    rig = tuiles_rig(jaw='machoire', tail_tip='tail_bout', ears=(), swing=(), idle=3.4, walk=.7, walk_amp=18, walk_knee=26, run=.42, run_amp=26, run_knee=34, leg_len=8.0)
    clips = quadrupede(rig, poids=.6)
    add_all(p, clips)
    return p
