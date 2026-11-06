#!/usr/bin/env python3
"""Drake de caravane : un vrai dragon de bat, massif, cou en S, tete cornue, crete dorsale, ailes de cuir repliees le long des flancs, grande queue articulee a fer de lance, palanquin a dais sur le dos. Siege 2,0, places (0, -13), (+-5,5, -3), (+-5,5, 8). Ne vole pas."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, ajoute_chaine, quadrupede, rot, sin
from kit import CF, CR, FER, OR, PAL, deux_faces, losange, nacelle, oeil, patte, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('drake_caravane', density=2, kind='mounts', seed=211, description='Drake de caravane Aeloria : refonte en dragon.')
    p.meta['y_corps'] = (13, 40)
    S = ('ecailles', 'dragon_pourpre')
    B = dict(base=3.2)
    BG = dict(base=3.2, l=6, h=6)
    NP = dict(base=4, axe='y', h=6)
    N = ('nacre', 'dragon_ventre')
    CREST = ('pics', 'dragon_or')
    SEAT = selle_y(2.0)                                   # 33
    DOS = SEAT - 3                                        # 30
    p.bone('body', (0, 22, 0))
    p.cube('body', 'poitrail', (-9.5, 13.5, -24), (9.5, DOS, -14), S, **BG)
    p.cube('body', 'ventre', (-10.5, 13, -14), (10.5, DOS, 8), S, **BG)
    p.cube('body', 'hanches', (-10, 13.5, 8), (10, DOS, 19), S, **BG)
    p.cube('body', 'croupe', (-7.5, 15, 19), (7.5, 28, 22), S, **BG)
    p.cube('body', 'ventre_nacre', (-7, 12, -23), (7, 12.5, 18), N, base=3.6, h=6)
    p.cube('body', 'plastron', (-6.5, 14, -24.5), (6.5, 27, -24), N, **NP)
    # cou en S : deux os, plaques de gorge, crete
    p.bone('cou1', (0, 25, -22), 'body')
    C1 = dict(rot=(-54, 0, 0), pivot=(0, 25, -22))
    p.cube('cou1', 'cou_a', (-5, 24, -26), (5, 39, -18), S, ton=.1, **B, **C1)
    p.cube('cou1', 'gorge_a', (-3.5, 25, -26.5), (3.5, 38, -26), N, **NP, **C1)
    p.cube('cou1', 'crete_a', (-.5, 25, -18), (.5, 38, -15.5), CREST, base=2.6, axe='y', pas=8, dent=5, **CR, **C1)
    p.bone('cou2', (0, 33, -32.5), 'cou1')
    C2 = dict(rot=(14, 0, 0), pivot=(0, 33, -32.5))
    p.cube('cou2', 'cou_b', (-4, 31.5, -36), (4, 46, -29), S, ton=.2, **B, **C2)
    p.cube('cou2', 'gorge_b', (-3, 33, -36.5), (3, 45, -36), N, **NP, **C2)
    p.cube('cou2', 'crete_b', (-.5, 33, -29), (.5, 45, -26.5), CREST, base=2.6, axe='y', pas=8, dent=5, **CR, **C2)
    # tete cornue
    p.bone('head', (0, 45, -30), 'cou2')
    p.cube('head', 'nuque', (-3.5, 43.5, -31.5), (3.5, 49.5, -28), S, ton=.2, **B)
    p.cube('head', 'crane', (-4.5, 44, -39), (4.5, 51, -31), S, ton=.3, **B)
    p.cube('head', 'museau', (-3.5, 44.5, -47), (3.5, 48.5, -39), S, ton=.4, **B)
    p.cube('head', 'museau_bout', (-3, 44.5, -48.5), (3, 47.5, -47), S, ton=.3, **B)
    p.cube('head', 'naseaux', (-2.5, 48.5, -46.5), (2.5, 49.5, -43.5), S, ton=.6, **B)
    p.sym('head', 'arcade', (2.5, 50, -40), (5, 52, -35.5), S, ton=.1, rot=(-8, 0, 0), pivot=(3.75, 51, -37.5), **B)
    oeil(p, 'crane', -35.5, y=49.5, couleur='a', clair='Y')
    deux_faces(p, 'museau', ['kk', 'kk'], -45, 48)
    deux_faces(p, 'museau', ['k' * 16], -39, 45, largeur=8)
    p.sym('head', 'dents', (3.5, 44, -46.5), (4, 45, -39), ('plan', 'ivoire'), base=3.6, masque=['#.#.#.#.#.#.#.#', '#...#...#...#..'], **CR)
    p.bone('machoire', (0, 45, -35), 'head')
    p.cube('machoire', 'mandibule', (-3, 42, -46), (3, 44.5, -34.5), S, ton=-.1, **B)
    p.cube('machoire', 'menton_nacre', (-2, 41.5, -45.5), (2, 42, -36), N, base=3)
    # cornes : balayees vers l'arriere, pointes d'or
    K = ('corne', 'ivoire')
    p.sym('head', 'corne_a', (1.5, 50, -33), (4, 52.5, -26.5), K, base=1.8, rot=(-10, 10, 0), pivot=(2.75, 51.25, -33))
    p.sym('head', 'corne_b', (2.5, 51.5, -27.5), (4.5, 53.5, -21.5), K, base=2.8, rot=(-20, 10, 0), pivot=(3.5, 52.5, -27))
    p.sym('head', 'corne_c', (3.5, 53.5, -22.5), (5, 55, -18), ('metal', 'or'), rot=(-32, 10, 0), pivot=(4.25, 54.25, -22))
    p.sym('head', 'corne_joue', (4, 45, -34), (5.5, 46.5, -27.5), K, base=2.4, rot=(8, 14, 0), pivot=(4.75, 45.75, -34))
    p.sym('head', 'collerette', (4.5, 43, -32), (5, 50, -26), ('crete_arriere', 'membrane'), base=2.2, pas=5, dent=6, ecart=2, **CR)
    p.cube('head', 'crete_tete', (-.5, 51, -39), (.5, 53.5, -29), CREST, base=2.6, pas=6, dent=4, **CR)
    # ailes de cuir repliees le long des flancs (os historiques wing_l, wing_r)
    p.bone('wing_l', (10.5, 29, -13), 'body')
    p.bone('wing_r', (-10.5, 29, -13), 'body')
    p.sym('wing_l|wing_r', 'aile_epaule', (10.5, 25.5, -16), (13, 30, -9), S, ton=.4, **B)
    p.sym('wing_l|wing_r', 'aile_bras', (11, 26.5, -10), (12.5, 29, 9), S, ton=.3, rot=(4, 0, 0), pivot=(11.75, 27.75, -10), **B)
    p.sym('wing_l|wing_r', 'aile_griffe', (11, 29.5, -11.5), (12.5, 32.5, -10), ('griffe', 'dragon_or'), base=3.6)
    p.sym('wing_l|wing_r', 'aile_plan', (11.5, 12, -10), (12, 27.5, 27), ('membrane_pliee', 'membrane'), base=3, doigts=4, **CR)
    # grande queue en trois os, crete et fer de lance
    p.bone('tail', (0, 23, 21), 'body')
    T1 = dict(rot=(6, 0, 0), pivot=(0, 23, 21))
    p.cube('tail', 'queue_a', (-5.5, 18, 20), (5.5, 27.5, 32.5), S, ton=.1, **B, **T1)
    p.cube('tail', 'crete_qa', (-.5, 27.5, 21), (.5, 31, 32), CREST, base=2.6, pas=8, dent=6, **CR, **T1)
    p.bone('tail_bout', (0, 21.5, 32), 'tail')
    T2 = dict(rot=(6, 0, 0), pivot=(0, 21.5, 32))
    p.cube('tail_bout', 'queue_b', (-3.5, 17.5, 31.5), (3.5, 24.5, 43), S, ton=.2, **B, **T2)
    p.cube('tail_bout', 'crete_qb', (-.5, 24.5, 32), (.5, 27.5, 43), CREST, base=2.6, pas=7, dent=5, **CR, **T2)
    p.bone('queue3', (0, 20, 43), 'tail_bout')
    p.cube('queue3', 'queue_c', (-2, 17.5, 42.5), (2, 22, 52), S, ton=.3, **B)
    p.cube('queue3', 'queue_fer', (-4.5, 19.5, 51.5), (4.5, 20, 58.5), ('plan', 'dragon_or'), base=3.6, masque=losange(18, 14), hide={'north', 'south', 'east', 'west'}, no_shadow=True)
    # pattes griffues en deux segments
    G = ('griffe', 'ivoire')
    patte(p, 'leg_fl', 'leg_fr', 'av', 5.5, 9, -20, -14, 18, 8.5, S, pied_h=3, avance=2, ton=-.1, ton_bas=-.2, griffes=G, base=3.2, bas=-.4)
    patte(p, 'leg_bl', 'leg_br', 'ar', 5.5, 9.5, 10, 16, 19, 9, S, pied_h=3, avance=2, retrait=1.5, ton=-.1, ton_bas=-.2, griffes=G, base=3.2, bas=-.4)
    # epaules et cuisses de bete : les pattes ne sont plus des colonnes
    p.sym('leg_fl|leg_fr', 'epaule', (5, 12.5, -21.5), (10, 25, -13), S, ton=.2, **BG)
    p.sym('leg_bl|leg_br', 'cuisse', (5, 11.5, 8.5), (10.5, 25, 18.5), S, ton=.2, **BG)
    nacelle(p, [(0, -13), (5.5, -3), (-5.5, -3), (5.5, 8), (-5.5, 8)], SEAT, DOS, 9.5, -17.5, 12, 'mythique', tapis_l=10, tapis_z=(-18.5, 13), dais=(47, 'rar_mythique'))
    rig = tuiles_rig(jaw='machoire', tail_tip='tail_bout', swing=(), idle=3.4, walk=1.0, walk_amp=20, walk_knee=36, run=.6, run_amp=32, run_knee=48, leg_len=18.0)
    clips = quadrupede(rig, poids=1.4)
    ajoute_chaine(clips, ['cou1', 'cou2'], ax=(1.4, 2.2, 3.5), az=(1.5, 2.5, 3), retard=.1)
    ajoute_chaine(clips, ['queue3'], ax=(1.5, 3, 5), az=(10, 16, 20), retard=.5)
    for name, (T, loop, tr) in clips.items():
        tr['wing_l'] = {'rotation': rot(T, z=sin(1.5, 1, 0))}
        tr['wing_r'] = {'rotation': rot(T, z=sin(-1.5, 1, 0))}
    add_all(p, clips)
    return p
