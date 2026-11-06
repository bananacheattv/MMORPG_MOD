#!/usr/bin/env python3
"""Bison geant : enorme bosse hirsute devant, tete tres basse, barbe, cornes courtes et sombres, croupe lisse et mince, nacelle a trois places. Siege 1,975, places en z = -9, 0,5, +10."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede
from kit import CF, CR, FER, OR, PAL, nacelle, oeil, patte, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('bison_geant', density=2, kind='mounts', seed=171, description='Bison geant Aeloria : refonte.')
    p.meta['y_corps'] = (12, 36)
    FUR = ('fourrure', 'bison')
    F = dict(base=3.4)
    SH = ('poils_longs', 'bison')
    S = dict(base=3.2, y=(6, 40))
    SEAT = selle_y(1.975)                                 # 32,5
    DOS = SEAT - 3                                        # 29,5
    p.bone('body', (0, 22, 0))
    # avant-train hirsute, bosse dressee devant la nacelle
    p.cube('body', 'poitrail', (-10, 13, -16), (10, DOS, -6), SH, **S)
    p.cube('body', 'bosse', (-9, 25, -27), (9, 34, -15.5), SH, ton=.5, **S)
    p.cube('body', 'bosse_haut', (-6.5, 34, -24.5), (6.5, 36.5, -17), SH, ton=.8, **S)
    p.cube('body', 'cou_bas', (-9, 13, -26), (9, 25, -16), SH, ton=-.1, **S)
    p.sym('body', 'jupe', (10, 7, -16), (10.5, 21, -6), ('crete', 'bison'), base=1.6, ecart=2.2, bas=True, pas=4, dent=6, **CR)
    # arriere-train lisse et plus mince
    p.cube('body', 'ventre', (-8.5, 15, -6), (8.5, DOS, 10), FUR, **F)
    p.cube('body', 'croupe', (-8, 16, 10), (8, DOS, 17.5), FUR, ton=.2, **F)
    p.cube('body', 'croupe_bout', (-6, 17.5, 17.5), (6, 28, 19), FUR, **F)
    # tete tres basse
    p.bone('head', (0, 20, -26), 'body')
    p.cube('head', 'cou', (-6.5, 11.5, -30), (6.5, 25, -25.5), SH, **S)
    p.cube('head', 'crane', (-6, 10, -37), (6, 23, -29.5), SH, ton=.2, **S)
    p.cube('head', 'toupet', (-4.5, 23, -35.5), (4.5, 25.5, -29.5), SH, ton=.8, **S)
    p.cube('head', 'museau', (-4, 9.5, -41.5), (4, 16, -37), ('peau', 'museau_gris'), base=3.2)
    p.decal('museau', 'north', stamp(['................', '..kk........kk..', '..kk........kk..'], PAL, 4, 15.5))
    p.bone('machoire', (0, 9.5, -37), 'head')
    p.cube('machoire', 'mandibule', (-3, 7.5, -41), (3, 9.5, -33), ('peau', 'museau_gris'), base=2.2)
    p.cube('machoire', 'barbe', (-2.5, 3, -39), (2.5, 7.5, -35.5), SH, **S)
    p.cube('machoire', 'barbe_bout', (-1.5, 1, -38.5), (1.5, 3, -36.5), SH, ton=-.2, **S)
    oeil(p, 'crane', -31.5, y=19, couleur='R', clair='a')
    # cornes courtes, sombres, pointes claires
    C = ('corne', 'sabot')
    p.sym('head', 'corne_a', (6, 18.5, -34), (10, 21.5, -31), C, base=1.6, rot=(0, 0, 12), pivot=(6, 20, -32.5))
    p.sym('head', 'corne_b', (8.5, 20.5, -34), (11, 25.5, -31.5), C, base=2.2, rot=(0, 0, 8), pivot=(9.75, 20.5, -32.75))
    p.sym('head', 'corne_c', (7.5, 25, -33.5), (9, 29, -32), ('corne', 'ivoire'), base=2, rot=(0, 0, 28), pivot=(8.25, 25.5, -32.75))
    p.bone('ear_l', (6, 17, -30.5), 'head')
    p.bone('ear_r', (-6, 17, -30.5), 'head')
    p.sym('ear_l|ear_r', 'oreille', (6, 15.5, -30.5), (9.5, 18, -30), ('plan', 'bison'), base=3, masque=['.######.', '########', '.#####..'], rot=(0, -10, -20), pivot=(6, 17, -30.5), **CF)
    # queue courte, touffe
    p.bone('tail', (0, 27.5, 19), 'body')
    p.cube('tail', 'queue', (-1, 18, 18.5), (1, 28, 20.5), FUR, rot=(-6, 0, 0), pivot=(0, 27.5, 19.5), **F)
    p.bone('tail_bout', (0, 18.5, 20), 'tail')
    p.cube('tail_bout', 'queue_touffe', (-1.5, 13, 19), (1.5, 18.5, 22), SH, ton=.3, **S)
    # pattes : culottes de poils devant, fines derriere
    K = ('uni', 'sabot')
    patte(p, 'leg_fl', 'leg_fr', 'av', 4, 9, -14.5, -8, 18, 8.5, SH, pied=K, pied_h=2.5, avance=.5, ton=0, ton_bas=-.3, base=2.6, y=(0, 36))
    patte(p, 'leg_bl', 'leg_br', 'ar', 3.5, 7.5, 11, 16.5, 19, 9, FUR, pied=K, pied_h=2.5, avance=.5, ton=-.1, ton_bas=-.5, base=3.2)
    nacelle(p, [(0, -9), (0, .5), (0, 10)], SEAT, DOS, 7, -13.5, 14, 'epique', bas=19, ventre=15, tapis_l=8.5, flanc=(-6, 10), tapis_z=(-14.5, 15), sangles=(-3, 6))
    rig = tuiles_rig(jaw='machoire', tail_tip='tail_bout', ears=('ear_l', 'ear_r'), swing=(), idle=3.4, walk=.95, walk_amp=21, walk_knee=38, run=.54, leg_len=18.0)
    add_all(p, quadrupede(rig, poids=1.4))
    return p
