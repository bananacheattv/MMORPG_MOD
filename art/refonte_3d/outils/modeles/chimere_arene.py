#!/usr/bin/env python3
"""Chimere des arenes : corps et criniere de lion, tete de chevre qui jaillit de la croupe derriere le cavalier, queue-serpent articulee, epaulieres a pointes de l'arene. Siege 1,45."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, ajoute_chaine, quadrupede
from kit import CF, CR, FER, OR, PAL, deux_faces, oeil, patte, selle, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('chimere_arene', density=2, kind='mounts', seed=131, description='Chimere des arenes Aeloria : refonte.')
    p.meta['y_corps'] = (8, 30)
    FUR = ('fourrure', 'fauve')
    F = dict(base=3.3)
    MANE = ('poils_longs', 'lion_criniere')
    M = dict(y=(6, 32), base=2.5)
    SEAT = selle_y(1.45)                                  # 24
    p.bone('body', (0, 17, 0))
    p.cube('body', 'poitrail', (-6.5, 11, -12), (6.5, 22.5, -5), FUR, **F)
    p.cube('body', 'ventre', (-5.5, 13, -5), (5.5, 22.5, 7), FUR, **F)
    p.cube('body', 'croupe', (-6, 12.5, 7), (6, 22.5, 14), FUR, ton=.2, **F)
    p.cube('body', 'croupe_bout', (-4.5, 14, 14), (4.5, 22, 15.5), FUR, **F)
    p.sym('body', 'hanche', (6, 14, 8), (6.5, 22, 13.5), FUR, ton=.4, **F)
    # tete de lion, anneaux de criniere
    p.bone('head', (0, 19, -13), 'body')
    p.cube('head', 'criniere', (-7.5, 11, -20), (7.5, 28, -12.5), MANE, **M)
    p.cube('head', 'criniere_avant', (-6, 13, -21.5), (6, 26.5, -20), MANE, ton=.4, **M)
    p.cube('head', 'criniere_haut', (-5, 28, -19), (5, 30, -13.5), MANE, ton=.6, **M)
    p.cube('head', 'criniere_cou', (-5, 8, -20.5), (5, 13, -13.5), MANE, ton=-.2, **M)
    p.sym('head', 'criniere_cote', (7.5, 7, -20), (8, 27, -12.5), ('crete', 'lion_criniere'), base=2, ecart=2.4, bas=True, pas=4, dent=5, **CR)
    p.cube('head', 'criniere_barbe', (-4.5, 4.5, -21), (4.5, 8, -20.5), ('plan', 'lion_criniere'), base=2.4, masque=['#########', '#########', '.#.###.#.', '...#.#...', '....#....'], **CF)
    p.cube('head', 'crane', (-4.5, 14.5, -27), (4.5, 24, -21.5), FUR, ton=.5, **F)
    p.cube('head', 'arcade', (-5, 21.5, -27.5), (5, 23, -26), FUR, ton=-.4, **F)
    p.cube('head', 'museau', (-3, 14.5, -31), (3, 19, -27), ('fourrure', 'creme'), ton=-.6)
    p.cube('head', 'truffe', (-1.5, 17.5, -31.5), (1.5, 19, -31), ('uni', 'gris_peau'), base=.6)
    p.sym('head', 'croc', (1.5, 12.5, -31), (2.5, 14.5, -30.5), ('griffe', 'ivoire'), base=3.2)
    p.bone('machoire', (0, 15, -25.5), 'head')
    p.cube('machoire', 'mandibule', (-2.5, 12.5, -30.5), (2.5, 14.5, -25), ('fourrure', 'creme'), ton=-1)
    oeil(p, 'crane', -23.5, y=21, couleur='R', clair='Y')
    p.decal('museau', 'north', stamp(['.....kk.....', '.....kk.....', 'kkkkkkkkkkkk'], PAL, 3, 17))
    p.bone('ear_l', (4, 24, -22), 'head')
    p.bone('ear_r', (-4, 24, -22), 'head')
    p.sym('ear_l|ear_r', 'oreille', (2.5, 23.5, -22), (6, 27, -21.5), ('plan', 'fauve'), base=3, masque=['..####..', '.######.', '########', '########', '.######.'], rot=(0, 0, -10), pivot=(4, 24, -22), **CF)
    # epaulieres de l'arene, a pointes
    p.sym('body', 'epauliere', (6.5, 18, -11), (7.5, 22.5, -6.5), ('cuirasse', 'fer'), base=3)
    P = ('metal', 'fer')
    p.sym('body', 'pique_a', (6.5, 22, -10.5), (8, 25.5, -9), P, ton=.6, rot=(0, 0, -22), pivot=(7.25, 22, -9.75))
    p.sym('body', 'pique_b', (6.5, 22, -8), (8, 24.5, -6.5), P, ton=.4, rot=(0, 0, -22), pivot=(7.25, 22, -7.25))
    # tete de chevre sur la croupe, derriere le cavalier
    C = ('fourrure', 'toison')
    p.bone('chevre', (0, 22, 11.5), 'body')
    p.cube('chevre', 'chevre_cou', (-2.5, 21, 9.5), (2.5, 31, 13.5), C, base=2.8, rot=(-8, 0, 0), pivot=(0, 22, 11.5))
    p.cube('chevre', 'chevre_crane', (-3, 29.5, 6.5), (3, 35, 12.5), C, base=3.2)
    p.cube('chevre', 'chevre_museau', (-2, 29.5, 3.5), (2, 32.5, 6.5), C, base=2.6)
    p.cube('chevre', 'chevre_barbe', (-.5, 25.5, 3.5), (.5, 29.5, 6.5), ('crete', 'toison'), base=2.6, ecart=1.2, bas=True, pas=3, dent=2, **CR)
    deux_faces(p, 'chevre_crane', ['dddd', 'dRRk', 'dddd'], 10, 33.5)
    p.decal('chevre_museau', 'north', stamp(['kk....kk'], PAL, 2, 32))
    K = ('corne', 'gris_peau')
    p.sym('chevre', 'chevre_corne_a', (1, 34.5, 8), (3, 39, 10), K, base=1.6, rot=(22, 0, -8), pivot=(2, 34.5, 9))
    p.sym('chevre', 'chevre_corne_b', (1.5, 38, 9.5), (3, 42, 11), K, base=2.4, rot=(58, 0, -10), pivot=(2.25, 38.5, 10.5))
    # queue-serpent : trois os, tete de serpent
    S = ('ecailles', 'peau_tortue')
    p.bone('tail', (0, 19, 15.5), 'body')
    p.cube('tail', 'queue', (-1.5, 17.5, 15), (1.5, 20.5, 24), S, base=2.6, l=3, h=2, y=(8, 32), rot=(14, 0, 0), pivot=(0, 19, 15.5))
    p.bone('tail_bout', (0, 17, 23.5), 'tail')
    p.cube('tail_bout', 'queue_mi', (-1.5, 16, 23), (1.5, 25, 26), S, base=2.8, l=3, h=2, y=(8, 32), rot=(26, 0, 0), pivot=(0, 17, 24.5))
    p.bone('serpent', (0, 24.5, 28), 'tail_bout')
    p.cube('serpent', 'serpent_cou', (-1.5, 24, 27), (1.5, 30, 30), S, base=3, l=3, h=2, y=(8, 32), rot=(-20, 0, 0), pivot=(0, 24.5, 28.5))
    p.cube('serpent', 'serpent_tete', (-2, 29, 22.5), (2, 32.5, 28), S, base=3.2, l=3, h=2, y=(8, 32))
    deux_faces(p, 'serpent_tete', ['Yk.', '.kk'], 25.5, 32)
    p.sym('serpent', 'serpent_croc', (.5, 27.5, 23), (1.5, 29.5, 23.5), ('griffe', 'ivoire'), base=3.2)
    # pattes de lion, griffes
    G = ('griffe', 'ivoire')
    patte(p, 'leg_fl', 'leg_fr', 'av', 2.5, 6, -11, -6, 15, 7.5, FUR, pied_h=2.5, avance=1, ton=-.2, ton_bas=-.4, griffes=G, bande=FER, **F)
    patte(p, 'leg_bl', 'leg_br', 'ar', 2, 5.5, 8.5, 13.5, 16, 8, FUR, pied_h=2.5, avance=1, ton=-.2, ton_bas=-.4, griffes=G, **F)
    selle(p, SEAT, 22.5, 5.5, 13, 'mythique', av=-4.5, ar=6.5, bas=14)
    rig = tuiles_rig(jaw='machoire', tail_tip='tail_bout', ears=('ear_l', 'ear_r'), idle=3.0, walk=.75, walk_amp=25, walk_knee=46, run=.46, leg_len=16.0)
    clips = quadrupede(rig, poids=1.0)
    ajoute_chaine(clips, ['chevre', 'serpent'], ax=(1.5, 3, 5), az=(3, 5, 8), retard=.3)
    add_all(p, clips)
    return p
