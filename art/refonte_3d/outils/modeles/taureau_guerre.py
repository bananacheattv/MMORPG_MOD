#!/usr/bin/env python3
"""Taureau de guerre : encolure enorme en bosse, tete basse, chanfrein de fer, cornes en lyre, anneau d'or, plaque de poitrail, double selle. Siege 1,69, deux places en z = -5 et z = +6."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede
from kit import CF, CR, FER, OR, PAL, deux_faces, oeil, patte, selle_longue, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('taureau_guerre', density=2, kind='mounts', seed=141, description='Taureau de guerre Aeloria : refonte.')
    p.meta['y_corps'] = (10, 34)
    FUR = ('fourrure', 'taureau')
    F = dict(base=3.4)
    SEAT = selle_y(1.69)                                  # 28
    p.bone('body', (0, 20, 0))
    p.cube('body', 'poitrail', (-8, 13, -12), (8, 26.5, -3), FUR, **F)
    p.cube('body', 'ventre', (-7, 14.5, -3), (7, 26.5, 11), FUR, **F)
    p.cube('body', 'croupe', (-6.5, 14.5, 11), (6.5, 26.5, 18), FUR, **F)
    p.cube('body', 'croupe_bout', (-5, 16, 18), (5, 25, 19.5), FUR, ton=-.2, **F)
    p.sym('body', 'hanche', (6.5, 16, 12), (7, 25, 17.5), FUR, ton=.4, **F)
    # bosse d'encolure : la masse du taureau, devant le premier cavalier
    p.cube('body', 'bosse', (-7.5, 14, -19.5), (7.5, 30, -12), FUR, ton=.3, **F)
    p.cube('body', 'bosse_haut', (-5, 30, -18.5), (5, 33, -13), ('poils_longs', 'taureau'), base=3.8, y=(10, 34))
    # plaques d'encolure, rivetees
    p.sym('body', 'plaque', (7.5, 17, -19), (8.5, 29, -12.5), ('cuirasse', 'fer'), base=3)
    p.decal('plaque_g', 'east', stamp(['.t...t...t.'], PAL, -13, 28))
    p.decal('plaque_g', 'east', stamp(['.oVVo', 'oVPPV', '.oVVo'], PAL, -14.5, 24))
    # tete basse
    p.bone('head', (0, 20, -19.5), 'body')
    p.cube('head', 'cou', (-5.5, 16, -25), (5.5, 27, -19.5), FUR, ton=.2, **F)
    p.cube('head', 'crane', (-5, 13.5, -31), (5, 24, -24.5), FUR, ton=.4, **F)
    p.cube('head', 'toupet', (-3.5, 24, -30), (3.5, 26, -25), ('poils_longs', 'taureau'), base=4, y=(10, 34))
    p.cube('head', 'museau', (-3.5, 13, -35.5), (3.5, 19, -31), ('uni', 'museau_bovin'), base=2.2)
    p.cube('head', 'nez', (-3, 15.5, -36), (3, 19, -35.5), ('uni', 'museau_bovin'), base=2.8)
    p.decal('nez', 'north', stamp(['............', '.kk......kk.', '.kk......kk.', '............', '............', '............'], PAL, 3, 18.5))
    # chanfrein de fer
    p.cube('head', 'chanfrein', (-3, 19, -31.5), (3, 24.5, -31), ('cuirasse', 'fer'), base=2.8)
    p.decal('chanfrein', 'north', stamp(['t..........t', '............', '....oVVo....', '...oVPPVo...', '....oVVo....', '.....oo.....'], PAL, 3, 24))
    # anneau d'or
    p.cube('head', 'anneau_bas', (-1.5, 11.5, -36.5), (1.5, 12.5, -36), OR, ton=.4)
    p.sym('head', 'anneau_cote', (1.5, 11.5, -36.5), (2, 16, -36), OR, ton=.4)
    p.bone('machoire', (0, 14, -31), 'head')
    p.cube('machoire', 'mandibule', (-2.5, 11, -35), (2.5, 13, -29), FUR, ton=-.6, **F)
    oeil(p, 'crane', -26.5, y=22, couleur='R', clair='a')
    # cornes en lyre : sortent sur les cotes, montent, pointes d'or rentrantes
    C = ('corne', 'ivoire')
    p.sym('head', 'corne_a', (5, 21, -28.5), (10.5, 24, -25.5), C, base=1.8, rot=(0, 0, 12), pivot=(5, 22.5, -27))
    p.sym('head', 'corne_b', (9, 23.5, -28.5), (11.5, 31, -26), C, base=2.6, rot=(0, 0, 8), pivot=(10.25, 23.5, -27.25))
    p.sym('head', 'corne_c', (8, 30, -28), (9.5, 35, -26.5), ('metal', 'or'), rot=(0, 0, 24), pivot=(8.75, 30.5, -27.25))
    p.bone('ear_l', (5, 21, -24.5), 'head')
    p.bone('ear_r', (-5, 21, -24.5), 'head')
    p.sym('ear_l|ear_r', 'oreille', (5, 18.5, -25), (9, 21, -24.5), ('plan', 'taureau'), base=3.4, masque=['.######.', '########', '.#####..'], rot=(0, -10, -18), pivot=(5, 20, -24.5), **CF)
    # queue longue a touffe
    p.bone('tail', (0, 24, 19.5), 'body')
    p.cube('tail', 'queue', (-1, 15, 19), (1, 24.5, 21), FUR, rot=(-8, 0, 0), pivot=(0, 24, 20), **F)
    p.bone('tail_bout', (0, 15.5, 21), 'tail')
    p.cube('tail_bout', 'queue_touffe', (-1.5, 9.5, 20), (1.5, 16, 23), ('poils_longs', 'taureau'), base=3.6, y=(8, 32))
    # pattes
    S = ('uni', 'sabot')
    patte(p, 'leg_fl', 'leg_fr', 'av', 3, 7.5, -11, -5.5, 16, 7.5, FUR, pied=S, pied_h=2.5, avance=1, ton=-.4, ton_bas=-.8, bande=FER, base=3.4)
    patte(p, 'leg_bl', 'leg_br', 'ar', 2.5, 6, 11.5, 16.5, 17, 8.5, FUR, pied=S, pied_h=2.5, avance=1, ton=-.4, ton_bas=-.8, bande=FER, base=3.4)
    selle_longue(p, [(0, -5), (0, 6)], SEAT, 26.5, 7, 14.5, 'peu_commun', -11, 13, bas=18, flanc=(-3, 11), sangles=(-2, 8), etrier_z=-1)
    rig = tuiles_rig(jaw='machoire', tail_tip='tail_bout', ears=('ear_l', 'ear_r'), idle=3.2, walk=.8, walk_amp=23, walk_knee=42, run=.5, leg_len=17.0)
    add_all(p, quadrupede(rig, poids=1.2))
    return p
