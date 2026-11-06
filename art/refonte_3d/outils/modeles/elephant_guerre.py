#!/usr/bin/env python3
"""Elephant de guerre : peau plissee gris bleu, immenses oreilles articulees, trompe en trois os, defenses a bouts d'or, chanfrein blinde, caparacon et palanquin a dais de quatre places. Siege 2,57, places x = +-5,5, z = -8 et +8."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, ajoute_chaine, quadrupede, rot, sin
from kit import CF, CR, EMBLEME, FER, OR, PAL, nacelle, oeil, patte, selle_y, tuiles_rig
from materiaux import stamp

OREILLE = ['..########....',
           '.############.',
           '##############',
           '##############',
           '##############',
           '##############',
           '#############.',
           '#############.',
           '############..',
           '###########...',
           '##########....',
           '#########.....',
           '#######.......',
           '#####.........']


def build():
    p = Project('elephant_guerre', density=2, kind='mounts', seed=191, description='Elephant de guerre Aeloria : refonte.')
    p.meta['y_corps'] = (14, 46)
    H = ('peau_plissee', 'elephant')
    B = dict(base=3, y=(8, 46))
    SEAT = selle_y(2.57)                                  # 42
    DOS = SEAT - 3                                        # 39
    p.bone('body', (0, 28, 0))
    p.cube('body', 'epaules', (-13.5, 18, -17), (13.5, DOS, -6), H, **B)
    p.cube('body', 'ventre', (-13, 18.5, -6), (13, DOS, 13), H, **B)
    p.cube('body', 'croupe', (-12, 19, 13), (12, 38, 20), H, ton=.1, **B)
    p.cube('body', 'croupe_bout', (-9, 21, 20), (9, 35, 22), H, **B)
    p.cube('body', 'garrot', (-10, 30, -21), (10, 41.5, -17), H, ton=.3, **B)
    # tete haute et blindee
    p.bone('head', (0, 32, -21), 'body')
    p.cube('head', 'cou', (-8.5, 21, -26), (8.5, 40, -20.5), H, **B)
    p.cube('head', 'crane', (-8, 19, -35), (8, 41, -25.5), H, ton=.3, **B)
    p.cube('head', 'dome', (-5.5, 41, -34), (5.5, 43.5, -27), H, ton=.5, **B)
    p.cube('head', 'front', (-6, 15, -38), (6, 30, -35), H, ton=.1, **B)
    p.cube('head', 'chanfrein', (-5, 30, -35.5), (5, 42, -35), ('cuirasse', 'fer'), base=3)
    p.decal('chanfrein', 'north', stamp(['t..................t', '....................', '.......oooooo.......', '.....ooVVVVVVoo.....', '....oVVPPPPPPVVo....', '.....ooVVVVVVoo.....', '.......oooooo.......', '....................', '.........oo.........', '.........oo.........', '.........oo.........'], PAL, 5, 41))
    p.cube('head', 'pointe_front', (-1, 42, -36), (1, 46, -34), OR, ton=.3, rot=(16, 0, 0), pivot=(0, 42, -35))
    oeil(p, 'crane', -30, y=31)
    # immenses oreilles sur os
    p.bone('ear_l', (8, 35, -27), 'head')
    p.bone('ear_r', (-8, 35, -27), 'head')
    p.sym('ear_l|ear_r', 'oreille', (8, 16, -27.5), (22, 40, -27), ('plan', 'elephant'), base=3.2, masque=OREILLE, rot=(0, -22, -6), pivot=(8, 35, -27), **CF)
    # trompe en trois os, anneau d'or
    p.bone('trompe1', (0, 22, -37), 'head')
    p.cube('trompe1', 'trompe_a', (-4, 9, -41), (4, 23, -35.5), H, ton=.1, **B)
    p.cube('trompe1', 'trompe_anneau', (-4.5, 13, -41.5), (4.5, 14.5, -35), OR)
    p.bone('trompe2', (0, 9.5, -38), 'trompe1')
    p.cube('trompe2', 'trompe_b', (-3, 3, -40.5), (3, 10, -36), H, **B)
    p.bone('trompe3', (0, 3.5, -38), 'trompe2')
    p.cube('trompe3', 'trompe_c', (-2.5, .5, -44), (2.5, 4, -36.5), H, ton=-.2, **B)
    p.cube('trompe3', 'trompe_bout', (-2, 4, -44.5), (2, 8.5, -41), H, ton=-.2, **B)
    # defenses a bouts d'or
    D = ('ivoire', 'ivoire')
    p.sym('head', 'defense_a', (4, 12, -47), (7, 15.5, -37), D, base=2.8, rot=(-18, 0, 0), pivot=(5.5, 13.75, -37))
    p.sym('head', 'defense_b', (4.5, 9, -54), (6.5, 12.5, -46), D, base=3.2, rot=(22, -6, 0), pivot=(5.5, 10.75, -46.5))
    p.sym('head', 'defense_or', (5, 12, -59), (7, 14, -53), OR, ton=.3, rot=(48, -4, 0), pivot=(6, 13, -53.5))
    # queue
    p.bone('tail', (0, 34, 22), 'body')
    p.cube('tail', 'queue', (-1, 22, 21.5), (1, 34.5, 23.5), H, rot=(-6, 0, 0), pivot=(0, 34, 22.5), **B)
    p.bone('tail_bout', (0, 22.5, 23), 'tail')
    p.cube('tail_bout', 'queue_touffe', (-1.5, 17, 22), (1.5, 22.5, 24.5), ('poils_longs', 'gris_peau'), base=1.8, y=(8, 40))
    # pattes-colonnes, chevilleres d'or, ongles
    G = ('griffe', 'ivoire')
    patte(p, 'leg_fl', 'leg_fr', 'av', 5.5, 12.5, -15.5, -7.5, 23, 11, H, pied_h=3, avance=1, ton=-.1, ton_bas=-.3, bande=OR, griffes=G, **B)
    patte(p, 'leg_bl', 'leg_br', 'ar', 5, 11.5, 11.5, 18.5, 24, 11.5, H, pied_h=3, avance=1, ton=-.1, ton_bas=-.3, bande=OR, griffes=G, **B)
    nacelle(p, [(5.5, -8), (-5.5, -8), (5.5, 8), (-5.5, 8)], SEAT, DOS, 10.5, -12.5, 12, 'legendaire', bas=23, ventre=18.5, tapis_l=13, flanc=(-6, 13), tapis_z=(-13.5, 14), sangles=(-3, 9), dais=(58, 'rar_legendaire'))
    rig = tuiles_rig(ears=('ear_l', 'ear_r'), tail_tip='tail_bout', swing=(), idle=4.0, walk=1.2, walk_amp=15, walk_knee=24, run=.74, run_amp=24, run_knee=36, leg_len=23.0)
    clips = quadrupede(rig, poids=1.8)
    ajoute_chaine(clips, ['trompe1', 'trompe2', 'trompe3'], ax=(2, 4, 6), az=(3, 5, 7), retard=.12)
    # les oreilles battent lentement autour de leur charniere
    for name, (T, loop, tr) in clips.items():
        tr['ear_l'] = {'rotation': rot(T, y=sin(9, 1, 0, bias=-3), z=sin(2, 1, .2))}
        tr['ear_r'] = {'rotation': rot(T, y=sin(-9, 1, 0, bias=3), z=sin(-2, 1, .2))}
    add_all(p, clips)
    return p
