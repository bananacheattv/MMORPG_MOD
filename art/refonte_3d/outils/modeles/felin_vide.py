#!/usr/bin/env python3
"""Felin du vide : silhouette de panthere, longue et basse, pelage de nuit etoilee, epines d'amethyste sur l'echine, queue en S a pointe de cristal, yeux violets, collier d'or. Siege 1,33."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede
from kit import CF, CR, FER, OR, PAL, oeil, patte, selle, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('felin_vide', density=2, kind='mounts', seed=91, description='Felin du vide Aeloria : refonte.')
    p.meta['y_corps'] = (8, 30)
    V = 'etoile'
    CRI = ('cristal', 'cristal')
    SEAT = selle_y(1.33)                                  # 22
    p.bone('body', (0, 17, 0))
    p.cube('body', 'poitrail', (-4.5, 12, -12), (4.5, 22, -5), V)
    p.cube('body', 'taille', (-3.5, 13.5, -5), (3.5, 20.5, 6), V)
    p.cube('body', 'croupe', (-4.5, 13, 6), (4.5, 21, 13), V)
    p.sym('body', 'epaule', (4.5, 14, -11), (5, 21, -6), V, base=2.6)
    p.sym('body', 'hanche', (4.5, 14.5, 7), (5, 20, 12), V, base=2.6)
    # epines d'amethyste
    for i, (z, y, h, a) in enumerate(((-10.5, 22, 5, 16), (-7.5, 22, 4, 22), (8, 21, 4, 22), (11, 20.5, 5, 26))):
        p.cube('body', f'epine{i}', (-1, y - .5, z - 1), (1, y + h, z + 1), CRI, rot=(a, 45, 0), pivot=(0, y, z))
    p.sym('body', 'epine_epaule', (4.5, 20, -10), (6, 24, -8.5), CRI, rot=(12, 0, -26), pivot=(5, 20, -9))
    p.sym('body', 'epine_hanche', (4.5, 19, 8.5), (6, 22.5, 10), CRI, rot=(20, 0, -30), pivot=(5, 19, 9))
    # tete petite et plate
    p.bone('head', (0, 19, -12), 'body')
    p.cube('head', 'cou', (-3, 15.5, -17), (3, 22, -11), V, rot=(-8, 0, 0), pivot=(0, 19, -12))
    p.cube('head', 'crane', (-3.5, 17.5, -22), (3.5, 23.5, -16), V, base=3.2)
    p.cube('head', 'museau', (-2, 17.5, -26), (2, 20.5, -22), V, base=3.4)
    p.cube('head', 'truffe', (-1, 19.5, -26.5), (1, 20.5, -26), ('uni', 'cristal'), base=3)
    p.bone('machoire', (0, 18, -21), 'head')
    p.cube('machoire', 'mandibule', (-1.5, 16, -25.5), (1.5, 17.5, -20.5), V, base=1.6)
    p.sym('machoire', 'croc', (1, 17.5, -25.5), (1.5, 18.5, -25), ('uni', 'ivoire'), base=4)
    p.decal('crane', 'east', stamp(['PPPP', 'WPPk', '.PPk'], PAL, -17.5, 22))
    p.decal('crane', 'west', stamp(['PPPP', 'WPPk', '.PPk'], PAL, -21, 22, flip_on=('west',)))
    p.cube('head', 'collier', (-3.5, 15.5, -14.5), (3.5, 17, -12.5), ('metal', 'or'))
    p.bone('ear_l', (2.5, 23, -19), 'head')
    p.bone('ear_r', (-2.5, 23, -19), 'head')
    O = ['......##', '....####', '...#####', '..######', '.#######', '########']
    p.sym('ear_l|ear_r', 'oreille', (1, 22.5, -19.5), (5, 26, -19), ('plan', 'vide'), base=3, masque=O, rot=(-8, 0, -8), pivot=(2.5, 23, -19), **CF)
    # queue en S, trois os, pointe de cristal
    p.bone('tail', (0, 19, 13), 'body')
    p.cube('tail', 'queue', (-1, 18, 12.5), (1, 20, 21), V, base=2.6, rot=(20, 0, 0), pivot=(0, 19, 13))
    p.bone('tail_bout', (0, 16.5, 20.5), 'tail')
    p.cube('tail_bout', 'queue_haut', (-1, 16, 19.5), (1, 24, 21.5), V, base=2.6, rot=(20, 0, 0), pivot=(0, 16.5, 20.5))
    p.cube('tail_bout', 'queue_cristal', (-1, 23.5, 22), (1, 29, 24), CRI, rot=(10, 45, 0), pivot=(0, 23.5, 23))
    # pattes fines, griffes
    G = ('griffe', 'cristal')
    patte(p, 'leg_fl', 'leg_fr', 'av', 1.5, 4, -10.5, -6, 17, 8, V, pied=V, pied_h=2, avance=1.5, ton=-.3, ton_bas=-.6, griffes=None)
    patte(p, 'leg_bl', 'leg_br', 'ar', 1, 4, 8, 12.5, 17, 8.5, V, pied=V, pied_h=2, avance=1.5, ton=-.3, ton_bas=-.6, griffes=None)
    selle(p, SEAT, 20.5, 3.5, 13.5, 'legendaire', av=-5, ar=6, bas=14)
    rig = tuiles_rig(jaw='machoire', tail_tip='tail_bout', ears=('ear_l', 'ear_r'), idle=3.0, walk=.65, walk_amp=28, walk_knee=52, run=.4, idle2=0, leg_len=17.0)
    add_all(p, quadrupede(rig, poids=.8))
    return p
