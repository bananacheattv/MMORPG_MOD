#!/usr/bin/env python3
"""Ours runique : masse voutee, bosse d'epaules, grosse tete a museau court, griffes d'ivoire, brassards de pierre gravee et runes turquoise. Siege 1,45."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede
from kit import CF, CR, FER, OR, PAL, oeil, patte, selle, selle_y, tuiles_rig
from materiaux import stamp

RUNE_A = ['EE..EE',
          'E.EE.E',
          '..EE..',
          '.EEEE.',
          'E.EE.E',
          '..EE..',
          '.E..E.',
          'E....E']
RUNE_B = ['..EE..',
          '.E..E.',
          'E.EE.E',
          'E.EE.E',
          '.E..E.',
          '..EE..',
          '..EE..',
          '.EEEE.']


def build():
    p = Project('ours_runique', density=2, kind='mounts', seed=51, description='Ours runique Aeloria : refonte.')
    p.meta['y_corps'] = (6, 28)
    FUR = ('fourrure', 'ours')
    SEAT = selle_y(1.45)                                  # 24
    PIERRE = ('maconnerie', 'pierre')
    p.bone('body', (0, 16, 0))
    p.cube('body', 'epaules', (-7.5, 9.5, -13), (7.5, 24.5, -4), FUR)
    p.cube('body', 'bosse', (-6, 24.5, -12), (6, 27.5, -5), FUR, ton=.2)
    p.cube('body', 'ventre', (-7, 10, -4), (7, 22.5, 8), FUR)
    p.cube('body', 'croupe', (-6.5, 10.5, 8), (6.5, 22, 14), FUR)
    p.cube('body', 'croupe_haut', (-5, 22, 8), (5, 23, 12.5), FUR, ton=.2)
    p.cube('body', 'croupe_bout', (-5, 12, 14), (5, 20.5, 15.5), FUR, ton=-.2)
    p.sym('body', 'epaule', (7.5, 12, -12), (8.5, 23, -5), FUR, ton=.3)
    p.sym('body', 'hanche', (6.5, 12.5, 8.5), (7.5, 21, 13.5), FUR, ton=.3)
    p.decal('epaule_g', 'east', stamp(RUNE_A, PAL, -6.5, 21))
    p.decal('hanche_g', 'east', stamp(RUNE_B, PAL, 12.5, 20))
    # epaulieres de pierre runique
    p.cube('body', 'pierre_garrot', (-4, 27.5, -11), (4, 28.5, -6), PIERRE, assise=4, long=8, ton=.3)
    p.decal('pierre_garrot', 'up', stamp(['EE..EE..EE', '..EE..EE..'], PAL, -2.5, -9.5))
    # tete
    p.bone('head', (0, 16.5, -13), 'body')
    p.cube('head', 'cou', (-6, 11.5, -19), (6, 23.5, -12), FUR, ton=.1)
    p.cube('head', 'crane', (-5.5, 12.5, -25), (5.5, 23, -18), FUR, ton=.2)
    p.cube('head', 'front', (-4, 23, -24), (4, 24.5, -19), FUR, ton=.4)
    p.cube('head', 'museau', (-3, 12.5, -30), (3, 18, -25), ('fourrure', 'daim'), ton=.2)
    p.cube('head', 'truffe', (-1.5, 16, -30.5), (1.5, 18, -30), ('uni', 'gris_peau'), base=.6)
    p.bone('machoire', (0, 13.5, -24), 'head')
    p.cube('machoire', 'mandibule', (-2.5, 10.5, -29), (2.5, 12.5, -23), ('fourrure', 'daim'), ton=-.5)
    p.sym('machoire', 'croc', (1.5, 12.5, -29), (2.5, 13.5, -28.5), ('uni', 'ivoire'), base=4)
    p.sym('head', 'crane_cote', (5.5, 12.5, -24.5), (6, 20, -19), ('crete', 'ours'), base=2.4, ecart=2, bas=True, pas=4, dent=3, **CR)
    oeil(p, 'crane', -20, -24, 21)
    p.decal('crane', 'north', stamp(['..E..E..', '...EE...', '...EE...', '..E..E..'], PAL, 4, 22.5))
    p.decal('museau', 'north', stamp(['kkkkkkkkkkkk'], PAL, 3, 13.5))
    p.bone('ear_l', (4, 23, -20), 'head')
    p.bone('ear_r', (-4, 23, -20), 'head')
    p.sym('ear_l|ear_r', 'oreille', (2.5, 22.5, -20.5), (6, 26.5, -19.5), FUR, ton=-.3, rot=(0, 0, -12), pivot=(4, 23, -20))
    # queue minuscule
    p.bone('tail', (0, 18, 15.5), 'body')
    p.cube('tail', 'queue', (-1.5, 16, 15.5), (1.5, 19, 17), FUR, ton=.2)
    # pattes-colonnes, griffes, brassards de pierre
    G = ('griffe', 'ivoire')
    patte(p, 'leg_fl', 'leg_fr', 'av', 3, 7, -12, -6, 15, 7, FUR, pied=FUR, pied_h=3, avance=1, ton=-.2, ton_bas=-.5, griffes=G, bande=PIERRE)
    patte(p, 'leg_bl', 'leg_br', 'ar', 2.5, 6, 8.5, 13.5, 15, 7.5, FUR, pied=FUR, pied_h=3, avance=1, ton=-.2, ton_bas=-.5, griffes=G)
    selle(p, SEAT, 22.5, 7, 10, 'epique', av=-4, ar=7.5, bas=13)
    rig = tuiles_rig(jaw='machoire', ears=('ear_l', 'ear_r'), walk=1.0, walk_amp=20, walk_knee=38, run=.56, idle=3.4, leg_len=15.0)
    add_all(p, quadrupede(rig, poids=1.3))
    return p
