#!/usr/bin/env python3
"""Rhinoceros cuirasse : fut en tonneau, garrot releve, peau plissee sous de grandes plaques rivetees, double corne effilee, tete basse, nacelle a trois places. Siege 1,63, places en z = -10, 0, +10."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede
from kit import CF, CR, FER, OR, PAL, nacelle, oeil, patte, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('rhinoceros_cuirasse', density=2, kind='mounts', seed=161, description='Rhinoceros cuirasse Aeloria : refonte.')
    p.meta['y_corps'] = (8, 30)
    H = ('peau_plissee', 'gris_peau')
    B = dict(base=2.8)
    PL = ('cuirasse', 'gris_peau')
    SEAT = selle_y(1.63)                                  # 27
    DOS = SEAT - 3                                        # 24
    p.bone('body', (0, 17, 0))
    p.cube('body', 'epaules', (-8.5, 11.5, -18), (8.5, DOS, -8), H, **B)
    p.cube('body', 'fut', (-9, 10.5, -8), (9, DOS, 9), H, **B)
    p.cube('body', 'hanches', (-8.5, 11.5, 9), (8.5, DOS, 18), H, **B)
    p.cube('body', 'croupe_bout', (-6.5, 13, 18), (6.5, 23, 20.5), H, base=2.6)
    p.cube('body', 'garrot', (-7.5, 12, -22), (7.5, 27.5, -18), H, base=3)
    # grandes plaques d'un seul tenant, rivets sur le bord haut
    p.sym('body', 'plaque_epaule', (8.5, 12.5, -17.5), (9.5, 23.5, -8.5), PL, base=3.4)
    p.sym('body', 'plaque_hanche', (8.5, 12.5, 9.5), (9.5, 23.5, 17.5), PL, base=3.4)
    RIV = ['..t...t...t...t...t...']
    p.decal('plaque_epaule_g', 'east', stamp(RIV[:1], PAL, -8.5, 22.5))
    p.decal('plaque_hanche_g', 'east', stamp(RIV[:1], PAL, 17.5, 22.5))
    p.cube('body', 'plaque_garrot', (-6, 22, -22.5), (6, 28, -22), PL, base=3.6)
    # tete basse et lourde
    p.bone('head', (0, 16, -22), 'body')
    p.cube('head', 'cou', (-6.5, 10.5, -26), (6.5, 22, -21.5), H, **B)
    p.cube('head', 'plaque_cou', (-7, 17, -25.5), (7, 23.5, -22.5), PL, base=3.2)
    p.cube('head', 'crane', (-5.5, 9.5, -33.5), (5.5, 20, -25.5), H, base=3)
    p.cube('head', 'museau', (-4.5, 8.5, -40), (4.5, 16, -33.5), H, base=2.9)
    p.cube('head', 'levre', (-3.5, 7.5, -41), (3.5, 10.5, -39.5), H, base=2.2)
    p.bone('machoire', (0, 9, -33), 'head')
    p.cube('machoire', 'mandibule', (-3.5, 6.5, -39.5), (3.5, 8.5, -31), H, base=2)
    # double corne effilee
    C = ('corne', 'ivoire')
    p.cube('head', 'corne_a', (-2, 15.5, -38.5), (2, 20, -34.5), C, base=1.8, rot=(10, 0, 0), pivot=(0, 15.5, -36.5))
    p.cube('head', 'corne_b', (-1.5, 19.5, -37), (1.5, 24, -34), C, base=2.6, rot=(18, 0, 0), pivot=(0, 19.5, -35.5))
    p.cube('head', 'corne_c', (-1, 23.5, -35), (1, 26.5, -33), C, base=3.2, rot=(24, 0, 0), pivot=(0, 23.5, -34))
    p.cube('head', 'corne_d', (-.5, 26, -33.5), (.5, 29.5, -32.5), C, base=3.6, rot=(32, 0, 0), pivot=(0, 26, -33))
    p.cube('head', 'corne_petite', (-1.5, 19.5, -32.5), (1.5, 23.5, -29.5), C, base=2.2, rot=(14, 0, 0), pivot=(0, 19.5, -31))
    p.cube('head', 'corne_petite_b', (-1, 23, -31.5), (1, 25.5, -29.5), C, base=3, rot=(24, 0, 0), pivot=(0, 23, -30.5))
    oeil(p, 'crane', -27, y=16.5)
    p.bone('ear_l', (4.5, 20, -26.5), 'head')
    p.bone('ear_r', (-4.5, 20, -26.5), 'head')
    p.sym('ear_l|ear_r', 'oreille', (3.5, 19.5, -27), (6, 24.5, -26), H, base=2.8, rot=(0, 0, -16), pivot=(4.5, 20, -26.5))
    # queue
    p.bone('tail', (0, 22, 20.5), 'body')
    p.cube('tail', 'queue', (-1, 14.5, 20), (1, 22.5, 22), H, base=2.4, rot=(-6, 0, 0), pivot=(0, 22, 21))
    p.cube('tail', 'queue_touffe', (-1.5, 11.5, 20.5), (1.5, 15, 23), ('poils_longs', 'gris_peau'), base=1.6, y=(8, 30))
    # pattes courtes et epaisses, ongles
    patte(p, 'leg_fl', 'leg_fr', 'av', 3.5, 8, -16, -9.5, 14, 6.5, H, pied_h=2.5, avance=.5, ton=-.1, ton_bas=-.3, griffes=('griffe', 'ivoire'), base=2.6)
    patte(p, 'leg_bl', 'leg_br', 'ar', 3.5, 8, 10, 16.5, 14, 6.5, H, pied_h=2.5, avance=.5, ton=-.1, ton_bas=-.3, griffes=('griffe', 'ivoire'), base=2.6)
    nacelle(p, [(0, -10), (0, 0), (0, 10)], SEAT, DOS, 6.5, -17, 16.5, 'rare', bas=15, ventre=10.5, tapis_l=9, flanc=(-8, 9), tapis_z=(-18, 18), sangles=(-6, 5))
    rig = tuiles_rig(jaw='machoire', ears=('ear_l', 'ear_r'), swing=(), idle=3.4, walk=.95, walk_amp=19, walk_knee=34, run=.56, run_amp=30, run_knee=44, leg_len=14.0)
    add_all(p, quadrupede(rig, poids=1.3))
    return p
