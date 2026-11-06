#!/usr/bin/env python3
"""Mammouth : toison longue qui tombe en jupe, tete en dome, garrot releve, tres longues defenses recourbees, trompe articulee, petites oreilles, nacelle de quatre places. Siege 2,44, places x = +-5,5, z = -8 et +8."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, ajoute_chaine, quadrupede
from kit import CF, CR, FER, OR, PAL, nacelle, oeil, patte, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('mammouth', density=2, kind='mounts', seed=181, description='Mammouth Aeloria : refonte.')
    p.meta['y_corps'] = (14, 44)
    SH = ('poils_longs', 'laine_mammouth')
    S = dict(base=3.2, y=(4, 46))
    JUPE = ('crete', 'laine_mammouth')
    J = dict(base=2, ecart=2.4, bas=True, pas=4, dent=7)
    SEAT = selle_y(2.44)                                  # 40
    DOS = SEAT - 3                                        # 37
    p.bone('body', (0, 26, 0))
    p.cube('body', 'epaules', (-13, 17, -16), (13, DOS, -5), SH, **S)
    p.cube('body', 'ventre', (-12.5, 18, -5), (12.5, DOS, 14), SH, **S)
    p.cube('body', 'croupe', (-11, 18.5, 14), (11, 35, 20), SH, ton=-.2, **S)
    p.cube('body', 'croupe_bout', (-8, 20, 20), (8, 33, 22), SH, ton=-.2, **S)
    p.cube('body', 'bosse', (-10, 30, -24), (10, 43.5, -15), SH, ton=.5, **S)
    p.cube('body', 'bosse_bas', (-11, 16, -24), (11, 30, -16), SH, ton=-.1, **S)
    # la toison tombe en jupe sous le ventre
    p.sym('body', 'jupe_flanc', (12.5, 11, -5), (13, 27, 14), JUPE, **J, **CR)
    p.sym('body', 'jupe_epaule', (13, 10, -16), (13.5, 33, -5), JUPE, **J, **CR)
    p.sym('body', 'jupe_croupe', (11, 12, 14), (11.5, 31, 20), JUPE, **J, **CR)
    # tete en dome
    p.bone('head', (0, 30, -24), 'body')
    p.cube('head', 'crane', (-8.5, 20, -34), (8.5, 42, -23.5), SH, ton=.3, **S)
    p.cube('head', 'dome', (-6.5, 42, -33), (6.5, 46, -25), SH, ton=.8, **S)
    p.cube('head', 'front', (-6.5, 16, -37.5), (6.5, 34, -34), SH, ton=.1, **S)
    oeil(p, 'crane', -31, y=30)
    p.bone('ear_l', (8.5, 33, -29.5), 'head')
    p.bone('ear_r', (-8.5, 33, -29.5), 'head')
    p.sym('ear_l|ear_r', 'oreille', (8.5, 29.5, -30), (13, 36.5, -29.5), ('plan', 'laine_mammouth'), base=2.8, masque=['..######', '.#######', '########', '########', '.#######', '...#####'], rot=(0, -18, -8), pivot=(8.5, 33, -29.5), **CF)
    # trompe en trois os
    p.bone('trompe1', (0, 22, -36), 'head')
    p.cube('trompe1', 'trompe_a', (-3.5, 10, -39.5), (3.5, 23, -34.5), SH, ton=-.1, **S)
    p.bone('trompe2', (0, 10.5, -36.5), 'trompe1')
    p.cube('trompe2', 'trompe_b', (-2.5, 3, -39), (2.5, 11, -35), SH, ton=-.3, **S)
    p.bone('trompe3', (0, 3.5, -36.5), 'trompe2')
    p.cube('trompe3', 'trompe_c', (-2, .5, -43), (2, 4, -35.5), SH, ton=-.5, **S)
    p.cube('trompe3', 'trompe_bout', (-1.5, 4, -43.5), (1.5, 8, -40), SH, ton=-.5, **S)
    # tres longues defenses, en quatre troncons qui se relevent
    D = ('ivoire', 'ivoire')
    p.sym('head', 'defense_a', (3.5, 12, -46), (7, 16, -35.5), D, base=2.6, rot=(-20, 0, 0), pivot=(5.25, 14, -36))
    p.sym('head', 'defense_b', (3.5, 9, -54), (7, 12, -44.5), D, base=2.8, rot=(5, -10, 0), pivot=(5.25, 10.5, -45))
    p.sym('head', 'defense_c', (5.5, 10, -61), (8, 12.5, -53), D, base=3, rot=(28, -8, 0), pivot=(6.75, 11.25, -53.5))
    p.sym('head', 'defense_d', (7, 14, -65.5), (8.5, 15.5, -59.5), D, base=3.4, rot=(50, -5, 0), pivot=(7.75, 14.75, -59.75))
    # queue courte
    p.bone('tail', (0, 33, 22), 'body')
    p.cube('tail', 'queue', (-1, 22, 21.5), (1, 33.5, 23.5), SH, ton=-.2, rot=(-6, 0, 0), pivot=(0, 33, 22.5), **S)
    # pattes-colonnes, ongles d'ivoire
    G = ('griffe', 'ivoire')
    patte(p, 'leg_fl', 'leg_fr', 'av', 5, 12, -15, -7, 22, 10.5, SH, pied_h=3, avance=1, ton=-.1, ton_bas=-.3, griffes=G, **S)
    patte(p, 'leg_bl', 'leg_br', 'ar', 4.5, 10.5, 11, 18, 23, 11, SH, pied_h=3, avance=1, ton=-.2, ton_bas=-.4, griffes=G, **S)
    nacelle(p, [(5.5, -8), (-5.5, -8), (5.5, 8), (-5.5, 8)], SEAT, DOS, 10.5, -12.5, 12, 'epique', bas=27, ventre=18, tapis_l=12.5, flanc=(-5, 13.5), tapis_z=(-13.5, 13.5), sangles=(-2, 8))
    rig = tuiles_rig(ears=('ear_l', 'ear_r'), swing=(), idle=3.8, walk=1.15, walk_amp=16, walk_knee=26, run=.72, run_amp=26, run_knee=38, leg_len=22.0)
    clips = quadrupede(rig, poids=1.7)
    ajoute_chaine(clips, ['trompe1', 'trompe2', 'trompe3'], ax=(2, 4, 6), az=(3, 5, 7), retard=.12)
    add_all(p, clips)
    return p
