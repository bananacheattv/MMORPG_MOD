#!/usr/bin/env python3
"""Loup de givre : long et sec, poitrail profond, ventre rentre, collerette de fourrure, eclats de glace sur l'echine, oeil bleu. Siege 1,39."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede
from kit import CF, CR, FER, OR, PAL, medaillon, selle, selle_y
from materiaux import stamp


def build():
    p = Project('loup_givre', density=2, kind='mounts', seed=21, description='Loup de givre Aeloria : refonte.')
    p.meta['y_corps'] = (9, 30)
    FUR = ('fourrure', 'fourrure_givre')
    FO = dict(nom='fourrure_givre')
    SEAT = selle_y(1.39)                                  # 23
    GL = ('glace', 'glace')

    # corps : poitrail profond, taille fine, croupe nerveuse
    p.bone('body', (0, 18, 0))
    p.cube('body', 'poitrail', (-5, 13, -12), (5, 24, -4), FUR)
    p.cube('body', 'taille', (-4, 15, -4), (4, 21.5, 6), FUR)
    p.cube('body', 'croupe', (-4.5, 14.5, 6), (4.5, 22, 13), FUR, ton=.2)
    p.cube('body', 'croupe_bout', (-3, 15.5, 13), (3, 21, 14.5), FUR)
    p.sym('body', 'epaule', (5, 15, -11), (5.5, 22.5, -5), FUR, ton=.4)
    p.sym('body', 'hanche', (4.5, 15.5, 7), (5, 21, 12), FUR, ton=.4)
    # collerette : masse de fourrure autour du cou, plans decoupes
    p.cube('body', 'collerette', (-5.5, 15, -15), (5.5, 26, -11), ('poils_longs', 'fourrure_givre'), ton=.8, y=(9, 30))
    p.sym('body', 'collerette_cote', (5.5, 12, -16), (6, 25, -8), ('crete', 'fourrure_givre'), base=3.4, ecart=2.2, bas=True, pas=5, dent=5, **CR)
    p.cube('body', 'collerette_devant', (-5, 10, -15.5), (5, 16, -15), ('plan', 'fourrure_givre'), base=4.2, masque=['##########', '##########', '#########.', '.########.', '.#######..', '..######..', '...####...', '....##....'], **CF)

    # echine : eclats de glace, du garrot a la queue
    for i, (z, y, h, w, a) in enumerate(((-9.5, 24, 5, 2, 14), (-6.5, 23.5, 4, 2, 22))):
        p.cube('body', f'eclat_av{i}', (-w / 2, y - .5, z - 1), (w / 2, y + h, z + 1), GL, rot=(a, 45, 0), pivot=(0, y, z))
    for i, (z, y, h, w, a) in enumerate(((8.5, 21.5, 4, 2, 24), (11.5, 21, 3, 2, 30))):
        p.cube('body', f'eclat_ar{i}', (-w / 2, y - .5, z - 1), (w / 2, y + h, z + 1), GL, rot=(a, 45, 0), pivot=(0, y, z))
    p.sym('body', 'eclat_epaule', (5, 21, -10), (6, 25, -8), GL, rot=(10, 0, -24), pivot=(5.5, 21, -9))
    p.sym('body', 'eclat_epaule2', (5, 20, -7.5), (6, 23, -6), GL, rot=(18, 0, -30), pivot=(5.5, 20, -7))

    # tete : cou leve, crane, long museau, machoire, oreilles pointues
    p.bone('head', (0, 24, -13), 'body')
    p.cube('head', 'cou', (-3.5, 20, -19), (3.5, 28, -13), FUR, rot=(16, 0, 0), pivot=(0, 24, -13), ton=.4)
    p.cube('head', 'crane', (-4, 25, -24), (4, 31, -18), FUR, ton=.6)
    p.cube('head', 'museau', (-2, 25, -30.5), (2, 28.5, -24), FUR, ton=.8)
    p.cube('head', 'truffe', (-1, 27, -31), (1, 29, -30.5), ('uni', 'gris_peau'), base=.6)
    p.sym('head', 'joue', (4, 22, -24), (4.5, 28.5, -18), ('crete', 'fourrure_givre'), base=3.6, ecart=2, bas=True, pas=4, dent=4, **CR)
    p.bone('machoire', (0, 25.5, -23), 'head')
    p.cube('machoire', 'mandibule', (-1.5, 23.5, -29.5), (1.5, 25, -22.5), FUR, ton=.1)
    p.sym('machoire', 'croc', (1, 25, -29.5), (1.5, 26, -29), ('uni', 'ivoire'), base=4.2)
    p.bone('ear_l', (2.5, 31, -20), 'head')
    p.bone('ear_r', (-2.5, 31, -20), 'head')
    OREILLE = ['......##', '.....###', '....####', '...#####', '..######', '.#######', '########', '########']
    p.sym('ear_l|ear_r', 'oreille', (1, 30.5, -20.5), (5, 35, -20), ('plan', 'fourrure_givre'), base=3.6, masque=OREILLE, rot=(-8, 0, -6), pivot=(2.5, 31, -20), **CF)
    p.cube('head', 'collier', (-4, 21, -17), (4, 22.5, -15), FER, rot=(16, 0, 0), pivot=(0, 24, -13))
    p.decal('crane', 'east', stamp(['dddd', 'dCBk', '.dkk'], PAL, -20, 29))
    p.decal('crane', 'west', stamp(['dddd', 'dCBk', '.dkk'], PAL, -22.5, 29, flip_on=('west',)))
    p.decal('museau', 'east', stamp(['kkkkkkkkkkkk'], PAL, -24.5, 25.5))
    p.decal('museau', 'west', stamp(['kkkkkkkkkkkk'], PAL, -30.5, 25.5, flip_on=()))
    p.decal('oreille_g', 'north', stamp(['...CC', '..CCC', '.CCCC'], PAL, 4, 33.5))

    # queue touffue en deux os
    p.bone('tail', (0, 20, 14), 'body')
    p.cube('tail', 'queue', (-2, 17.5, 13.5), (2, 22, 21), ('poils_longs', 'fourrure_givre'), rot=(35, 0, 0), pivot=(0, 20, 14), y=(9, 30))
    p.bone('tail_bout', (0, 16, 19.5), 'tail')
    p.cube('tail_bout', 'queue_blanche', (-1.5, 14, 19), (1.5, 18, 25.5), ('poils_longs', 'fourrure_givre'), rot=(55, 0, 0), pivot=(0, 16, 19.5), ton=1.4, y=(9, 30))

    # pattes hautes et fines
    p.bone('leg_fl', (3.5, 18, -8))
    p.bone('leg_fr', (-3.5, 18, -8))
    p.bone('leg_fl_bas', (3.5, 10, -8), 'leg_fl')
    p.bone('leg_fr_bas', (-3.5, 10, -8), 'leg_fr')
    p.bone('leg_bl', (3.5, 19, 9.5))
    p.bone('leg_br', (-3.5, 19, 9.5))
    p.bone('leg_bl_bas', (3.5, 11, 11), 'leg_bl')
    p.bone('leg_br_bas', (-3.5, 11, 11), 'leg_br')
    p.sym('leg_fl|leg_fr', 'avantbras', (2, 9.5, -10), (4.5, 19, -6), FUR, ton=.1, hide={'up'})
    p.sym('leg_fl_bas|leg_fr_bas', 'canon', (2.5, 1.5, -9.5), (4, 10, -6.5), FUR, ton=-.2, hide={'up'})
    p.sym('leg_fl_bas|leg_fr_bas', 'patte_av', (2, 0, -11), (5, 2, -6), ('uni', 'gris_peau'), base=2)
    p.sym('leg_bl|leg_br', 'cuisse', (1.5, 10.5, 7.5), (4, 20, 12.5), FUR, ton=.1, hide={'up'})
    p.sym('leg_bl_bas|leg_br_bas', 'jarret', (2.5, 6, 10), (4.5, 11, 12.5), FUR, ton=-.2, hide={'up'}, rot=(14, 0, 0), pivot=(3.5, 11, 11))
    p.sym('leg_bl_bas|leg_br_bas', 'canon_ar', (2.5, 1.5, 8), (4.5, 7, 10.5), FUR, ton=-.2)
    p.sym('leg_bl_bas|leg_br_bas', 'patte_ar', (2, 0, 6.5), (5, 2, 11), ('uni', 'gris_peau'), base=2)

    # harnais
    selle(p, SEAT, 21.5, 4, 15, 'rare', av=-5, ar=6, bas=15.5)

    rig = {'body': 'body', 'head': 'head', 'tail': 'tail', 'tail_tip': 'tail_bout', 'jaw': 'machoire', 'ears': ('ear_l', 'ear_r'), 'swing': ('etrier_g', 'etrier_d'),
           'legs': [('leg_fl', 'leg_fl_bas', 0.0), ('leg_fr', 'leg_fr_bas', .5), ('leg_bl', 'leg_bl_bas', .56), ('leg_br', 'leg_br_bas', .06)],
           'walk': .62, 'walk_amp': 30, 'walk_knee': 52, 'run': .42, 'idle': 3.0, 'leg_len': 18.0}
    add_all(p, quadrupede(rig, poids=.9))
    return p
