#!/usr/bin/env python3
"""Lion dore : pelage d'or, immense criniere rousse en anneaux de meches autour de la face, diademe, queue relevee a houppe, harnais royal a double selle. Siege 1,63, deux places en z = -5,5 et z = +5,5."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede
from kit import CF, CR, FER, OR, PAL, oeil, patte, selle_longue, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('lion_dore', density=2, kind='mounts', seed=151, description='Lion dore Aeloria : refonte.')
    p.meta['y_corps'] = (10, 32)
    FUR = ('fourrure', 'lion_or')
    F = dict(base=3.2)
    MANE = ('poils_longs', 'lion_criniere')
    M = dict(y=(8, 34))
    SEAT = selle_y(1.63)                                  # 27
    p.bone('body', (0, 20, 0))
    p.cube('body', 'poitrail', (-7.5, 13, -13), (7.5, 25.5, -4), FUR, **F)
    p.cube('body', 'ventre', (-6, 15, -4), (6, 25.5, 10), FUR, **F)
    p.cube('body', 'croupe', (-6.5, 14.5, 10), (6.5, 25.5, 17), FUR, ton=.2, **F)
    p.cube('body', 'croupe_bout', (-5, 16, 17), (5, 24.5, 18.5), FUR, **F)
    p.sym('body', 'hanche', (6.5, 16, 11), (7, 24.5, 16.5), FUR, ton=.4, **F)
    p.sym('body', 'epaule', (7.5, 15, -12), (8, 24.5, -6), FUR, ton=.4, **F)
    # criniere : anneaux de meches autour de la face, plus larges vers l'arriere
    p.bone('head', (0, 21, -13), 'body')
    p.cube('head', 'criniere', (-8.5, 12, -21), (8.5, 31, -13), MANE, **M)
    p.cube('head', 'criniere_avant', (-7, 14, -23), (7, 29.5, -21), MANE, ton=.4, **M)
    p.cube('head', 'criniere_haut', (-6, 31, -20), (6, 33, -14.5), MANE, ton=.6, **M)
    p.cube('head', 'criniere_arriere', (-7, 15, -13), (7, 29, -11.5), MANE, ton=-.3, **M)
    p.cube('head', 'criniere_cou', (-6, 8.5, -22), (6, 14, -14), MANE, ton=-.2, **M)
    p.sym('head', 'criniere_cote', (8.5, 8, -21), (9, 30, -13), ('crete', 'lion_criniere'), base=2.6, ecart=2.4, bas=True, pas=4, dent=6, **CR)
    p.cube('head', 'criniere_barbe', (-5.5, 5, -22.5), (5.5, 9, -22), ('plan', 'lion_criniere'), base=3, masque=['###########', '###########', '.#########.', '.#.#####.#.', '...#.#.#...', '.....#.....'], **CF)
    # face
    p.cube('head', 'crane', (-5, 16, -28.5), (5, 26.5, -23), FUR, ton=.5, **F)
    p.cube('head', 'chanfrein', (-2, 21, -32.5), (2, 23, -28.5), FUR, ton=.3, **F)
    p.cube('head', 'museau', (-3, 16, -33), (3, 21, -28.5), ('fourrure', 'creme'), ton=-.2)
    p.cube('head', 'truffe', (-1.5, 19.5, -33.5), (1.5, 21, -33), ('uni', 'gris_peau'), base=1)
    p.sym('head', 'croc', (1.5, 14.5, -33), (2.5, 16, -32.5), ('griffe', 'ivoire'), base=3.2)
    p.bone('machoire', (0, 16.5, -27), 'head')
    p.cube('machoire', 'mandibule', (-2.5, 14, -32.5), (2.5, 16, -26.5), ('fourrure', 'creme'), ton=-.6)
    oeil(p, 'crane', -24.5, y=24)
    p.decal('museau', 'north', stamp(['.....kk.....', '.....kk.....', 'kkkkkkkkkkkk'], PAL, 3, 19))
    # diademe royal
    p.cube('head', 'diademe', (-5.5, 24.5, -29), (5.5, 25.5, -28), OR)
    p.cube('head', 'diademe_pierre', (-1, 24, -29.5), (1, 26.5, -29), OR, ton=.3)
    p.decal('diademe_pierre', 'north', stamp(['oo.o', 'oVVo', 'VPPV', 'oVVo', 'o.oo'], PAL, 1, 26.5))
    p.bone('ear_l', (4.5, 27, -24), 'head')
    p.bone('ear_r', (-4.5, 27, -24), 'head')
    p.sym('ear_l|ear_r', 'oreille', (3, 26.5, -24), (6.5, 29.5, -23.5), ('plan', 'lion_or'), base=3.4, masque=['..####..', '.######.', '########', '########'], rot=(0, 0, -8), pivot=(4.5, 27, -24), **CF)
    # queue : descend puis se releve, houppe de meches
    p.bone('tail', (0, 23, 18.5), 'body')
    p.cube('tail', 'queue', (-1, 22, 18), (1, 24, 26.5), FUR, rot=(20, 0, 0), pivot=(0, 23, 18.5), **F)
    p.bone('tail_bout', (0, 20.5, 26.5), 'tail')
    p.cube('tail_bout', 'queue_mi', (-1, 19.5, 26), (1, 21.5, 33), FUR, rot=(-30, 0, 0), pivot=(0, 20.5, 26.5), **F)
    p.cube('tail_bout', 'queue_houppe', (-1.5, 22, 30.5), (1.5, 25.5, 34), MANE, ton=.3, rot=(-30, 0, 0), pivot=(0, 24, 32), **M)
    p.cube('tail_bout', 'queue_houppe_bout', (-1, 25.5, 31), (1, 28, 33.5), MANE, ton=.5, rot=(-30, 0, 0), pivot=(0, 24, 32), **M)
    # pattes, griffes
    G = ('griffe', 'ivoire')
    patte(p, 'leg_fl', 'leg_fr', 'av', 3, 7, -11.5, -6.5, 16, 7.5, FUR, pied_h=2.5, avance=1, ton=-.2, ton_bas=-.4, griffes=G, **F)
    patte(p, 'leg_bl', 'leg_br', 'ar', 2.5, 6, 11, 16, 17, 8.5, FUR, pied_h=2.5, avance=1, ton=-.2, ton_bas=-.4, griffes=G, **F)
    selle_longue(p, [(0, -5.5), (0, 5.5)], SEAT, 25.5, 6, 15, 'rare', -11, 12.5, bas=17, flanc=(-4, 10), sangles=(-3, 7), etrier_z=-1.5)
    rig = tuiles_rig(jaw='machoire', tail_tip='tail_bout', ears=('ear_l', 'ear_r'), idle=3.0, walk=.7, walk_amp=25, walk_knee=46, run=.44, leg_len=17.0)
    add_all(p, quadrupede(rig, poids=1.0))
    return p
