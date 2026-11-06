#!/usr/bin/env python3
"""Tortue forteresse : carapace en dome a gradins portant une citadelle, chemin de ronde a merlons, tour arriere et grande banniere orange, heaume de pierre. Siege 1,69, places (0, -13), (+-6, -3), (+-6, 8)."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede, rot, sin
from kit import CF, CR, EMBLEME, FER, OR, PAL, deux_faces, nacelle, oeil, patte, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('tortue_forteresse', density=2, kind='mounts', seed=201, description='Tortue forteresse Aeloria : refonte.')
    p.meta['y_corps'] = (4, 28)
    SH = ('scutes', 'carapace')
    PE = ('peau_plissee', 'peau_tortue')
    PK = dict(base=3.5, y=(0, 26))
    M = ('maconnerie', 'pierre')
    CP = ('creneau_pierre', 'pierre')
    SEAT = selle_y(1.69)                                  # 28
    DOS = SEAT - 3                                        # 25
    p.bone('body', (0, 14, 0))
    # carapace en gradins
    p.cube('body', 'plastron', (-12.5, 6, -19), (12.5, 8, 23), ('nacre', 'dragon_ventre'), base=2.6)
    p.cube('body', 'bord', (-15, 8, -22), (15, 11, 26), ('segments', 'carapace'), base=2.8, pas=10, axe='z')
    p.cube('body', 'carapace_a', (-13.5, 11, -20), (13.5, 19, 24), SH, base=3.1, l=20, h=14)
    p.cube('body', 'carapace_b', (-12, 19, -18.5), (12, DOS, 22.5), SH, base=3.5, l=18, h=12)
    # chemin de ronde : merlons sur la rambarde
    for i, z in enumerate((-16, -9, -2, 5)):
        p.sym('body', f'merlon{i}', (10, 30, z), (11.5, 32, z + 2.5), CP, ton=.2)
    # tour arriere
    p.bone('tour', (0, 25, 18), 'body')
    p.cube('tour', 'tour_corps', (-6.5, 25.5, 13.5), (6.5, 40, 22.5), M, assise=6, long=12, varie=True)
    p.cube('tour', 'tour_couronne', (-8, 40, 12.5), (8, 42, 24), M, assise=4, long=8, ton=.3)
    for n, (x0, x1, z0, z1) in enumerate(((5.5, 8, 12.5, 15), (5.5, 8, 21.5, 24), (5.5, 8, 17, 19.5))):
        p.sym('tour', f'merlon_t{n}', (x0, 42, z0), (x1, 44.5, z1), CP, ton=.5)
    p.cube('tour', 'merlon_av', (-1.5, 42, 12.5), (1.5, 44.5, 15), CP, ton=.5)
    p.cube('tour', 'merlon_ar', (-1.5, 42, 21.5), (1.5, 44.5, 24), CP, ton=.5)
    p.cube('tour', 'mat', (-.5, 42, 17.5), (.5, 54, 18.5), OR)
    deux_faces(p, 'tour_corps', ['kkk', 'kkk', 'kkk', 'kkk', 'kkk', 'kkk', '.k.'], 19, 37)
    # grande banniere orange sur la tour, oriflamme en tete de mat
    p.cube('tour', 'banniere', (-3.5, 28.5, 13), (3.5, 39, 13.5), ('tissu', 'rar_legendaire'), bord=1, masque=None, **CF)
    p.decal('banniere', 'north', stamp(EMBLEME, PAL, 2.5, 36.5))
    p.bone('oriflamme', (0, 53, 18.5), 'tour')
    p.cube('oriflamme', 'oriflamme_plan', (-.5, 47.5, 18.5), (.5, 54, 30), ('plan', 'rar_legendaire'), base=4, masque=['###########', '.##########', '..#########', '...########', '..#########', '.##########', '###########'], **CR)
    # tete et heaume de pierre
    p.bone('head', (0, 13, -21), 'body')
    p.cube('head', 'cou', (-5, 8.5, -29), (5, 17, -20.5), PE, **PK)
    p.cube('head', 'crane', (-5.5, 9, -38), (5.5, 19, -29), PE, ton=.3, **PK)
    p.cube('head', 'bec_haut', (-4, 9.5, -41), (4, 15, -38), ('uni', 'sabot'), base=3)
    p.bone('machoire', (0, 10, -33), 'head')
    p.cube('machoire', 'bec_bas', (-3.5, 7, -40.5), (3.5, 9, -32), ('uni', 'sabot'), base=2.2)
    p.cube('head', 'heaume', (-6, 15.5, -36.5), (6, 20, -30), ('cuirasse', 'pierre'), base=3.4)
    p.cube('head', 'heaume_or', (-1, 20, -36), (1, 21, -30.5), OR)
    p.cube('head', 'heaume_front', (-3, 15, -37), (3, 19.5, -36.5), OR, ton=-.2)
    p.decal('heaume_front', 'north', stamp(['.oVVVVo.', 'oVPPPPVo', '.oVVVVo.'], PAL, 2, 18.5))
    oeil(p, 'crane', -33, y=15, couleur='a', clair='Y')
    # queue
    p.bone('tail', (0, 10, 25.5), 'body')
    p.cube('tail', 'queue', (-2, 8, 25), (2, 11, 33), PE, rot=(14, 0, 0), pivot=(0, 10, 25.5), **PK)
    # grosses pattes, griffes
    G = ('griffe', 'ivoire')
    patte(p, 'leg_fl', 'leg_fr', 'av', 8, 14.5, -18.5, -11, 12, 5, PE, pied_h=3, avance=1.5, ton=.2, griffes=G, ton_bas=-.1, **PK)
    patte(p, 'leg_bl', 'leg_br', 'ar', 8, 14.5, 13, 21, 12, 5, PE, pied_h=3, avance=1.5, ton=.2, griffes=G, ton_bas=-.1, **PK)
    nacelle(p, [(0, -13), (6, -3), (-6, -3), (6, 8), (-6, 8)], SEAT, DOS, 11.5, -17.5, 12.5, 'legendaire', bois=('dalle', 'pierre'), tapis_z=(-18, 13.5))
    rig = tuiles_rig(jaw='machoire', swing=(), idle=3.8, walk=1.25, walk_amp=17, walk_knee=26, run=.8, run_amp=24, run_knee=34, leg_len=12.0)
    clips = quadrupede(rig, poids=1.6)
    for name, (T, loop, tr) in clips.items():
        tr['oriflamme'] = {'rotation': rot(T, y=sin(10, 2, 0), z=sin(2, 1, .2))}
    add_all(p, clips)
    return p
