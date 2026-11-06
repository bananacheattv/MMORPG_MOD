#!/usr/bin/env python3
"""Drake ascendant : un vrai dragon elance d'or et d'amethyste, cou en S, couronne de cornes, grandes ailes de membrane articulees, queue a aileron. Siege 1,6. Vole : clip fly, ailes repliees au sol."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, ajoute_chaine, ajoute_vol, quadrupede, rot, sin
from kit import CF, CR, FER, OR, PAL, deux_faces, oeil, patte, selle, selle_y, tuiles_rig
from materiaux import stamp


def fer(w, h):
    """Masque d'un fer de lance pointe vers l'arriere : w colonnes, h lignes, lu depuis l'est (colonne 0 = arriere)."""
    rows = []
    for y in range(h):
        d = abs(2 * y + 1 - h) / h
        rows.append(''.join('#' if x / w >= d ** .9 else '.' for x in range(w)))
    return rows


def build():
    p = Project('drake_ascendant', density=2, kind='mounts', seed=221, description='Drake ascendant Aeloria : refonte en dragon.')
    p.meta['y_corps'] = (12, 34)
    S = ('ecailles', 'dragon_or')
    B = dict(base=3.4, bas=-.4)
    NP = dict(base=4, axe='y', h=4)
    A = ('ecailles', 'dragon_pourpre')
    N = ('nacre', 'dragon_ventre')
    SEAT = selle_y(1.6)                                   # 26,5
    p.bone('body', (0, 19, 0))
    p.cube('body', 'poitrail', (-6, 14.5, -13), (6, 25, -5), S, **B)
    p.cube('body', 'ventre', (-5, 15.5, -5), (5, 25, 8), S, **B)
    p.cube('body', 'hanches', (-5.5, 15, 8), (5.5, 25, 15), S, **B)
    p.cube('body', 'croupe', (-4, 16.5, 15), (4, 24, 17), S, **B)
    p.cube('body', 'ventre_nacre', (-3.5, 14, -12), (3.5, 14.5, 14), N, base=3.6, h=6)
    p.cube('body', 'plastron', (-4, 15, -13.5), (4, 23.5, -13), N, **NP)
    # cou en S
    p.bone('cou1', (0, 22, -11), 'body')
    C1 = dict(rot=(-50, 0, 0), pivot=(0, 22, -11))
    p.cube('cou1', 'cou_a', (-3.5, 21, -14), (3.5, 32.5, -8), S, ton=.1, **B, **C1)
    p.cube('cou1', 'gorge_a', (-2.5, 22, -14.5), (2.5, 31.5, -14), N, **NP, **C1)
    p.cube('cou1', 'crete_a', (-.5, 22, -8), (.5, 31.5, -5.5), ('pics', 'dragon_pourpre'), base=2.4, axe='y', pas=7, dent=5, **CR, **C1)
    p.bone('cou2', (0, 28, -18.5), 'cou1')
    C2 = dict(rot=(12, 0, 0), pivot=(0, 28, -18.5))
    p.cube('cou2', 'cou_b', (-3, 27, -21), (3, 38.5, -16), S, ton=.2, **B, **C2)
    p.cube('cou2', 'gorge_b', (-2, 28, -21.5), (2, 38, -21), N, **NP, **C2)
    p.cube('cou2', 'crete_b', (-.5, 28, -16), (.5, 38, -14), ('pics', 'dragon_pourpre'), base=2.4, axe='y', pas=7, dent=4, **CR, **C2)
    # tete fine et couronne de cornes
    p.bone('head', (0, 37, -17.5), 'cou2')
    p.cube('head', 'crane', (-3.5, 36, -25.5), (3.5, 41.5, -18), S, ton=.3, **B)
    p.cube('head', 'nuque', (-2.5, 35.5, -18.5), (2.5, 40.5, -15.5), S, ton=.2, **B)
    p.cube('head', 'museau', (-2.5, 36.5, -33), (2.5, 39.5, -25.5), S, ton=.4, **B)
    p.cube('head', 'museau_bout', (-2, 36.5, -34.5), (2, 39, -33), S, ton=.3, **B)
    p.cube('head', 'naseaux', (-1.5, 39.5, -32.5), (1.5, 40, -30), S, ton=.6, **B)
    p.sym('head', 'arcade', (2, 40.5, -26), (4, 42, -22), A, base=3, rot=(-8, 0, 0), pivot=(3, 41.25, -24))
    oeil(p, 'crane', -22.5, y=40.5, couleur='P', clair='W')
    deux_faces(p, 'museau', ['kk'], -31.5, 39)
    deux_faces(p, 'museau', ['k' * 14], -26, 37, largeur=7)
    p.sym('head', 'dents', (2.5, 36, -32.5), (3, 37, -26), ('plan', 'ivoire'), base=3.6, masque=['#.#.#.#.#.#.#', '#...#...#...#'], **CR)
    p.bone('machoire', (0, 37, -22), 'head')
    p.cube('machoire', 'mandibule', (-2, 34.5, -32), (2, 36.5, -21.5), S, ton=-.1, **B)
    p.cube('machoire', 'menton_nacre', (-1.5, 34, -31.5), (1.5, 34.5, -23), N, base=3)
    K = ('corne', 'dragon_pourpre')
    p.sym('head', 'corne_a', (1.5, 40.5, -20), (3.5, 43, -14), K, base=2.6, rot=(-16, 10, 0), pivot=(2.5, 41.75, -20))
    p.sym('head', 'corne_b', (2.5, 42, -15), (4, 43.5, -9), K, base=3.4, rot=(-28, 12, 0), pivot=(3.25, 42.75, -15))
    OR2 = ('metal', 'or')
    p.sym('head', 'couronne_a', (2.5, 40, -24), (4, 44.5, -22.5), OR2, ton=.3, rot=(0, 0, -14), pivot=(3.25, 40.5, -23.25))
    p.sym('head', 'couronne_b', (2.5, 40, -21), (4, 45.5, -19.5), OR2, ton=.3, rot=(0, 0, -14), pivot=(3.25, 40.5, -20.25))
    p.cube('head', 'couronne_c', (-1, 41.5, -25), (1, 46.5, -23), OR2, ton=.5)
    p.decal('couronne_c', 'north', stamp(['oooo', 'oVVo', 'VPPV', 'oVVo', 'oooo'], PAL, 1, 45))
    # ailes de membrane articulees (os historiques wing_l, wing_r)
    AP = ('aile_membrane', 'membrane')
    for w, t, s in (('wing_l', 'wing_l_bout', 1), ('wing_r', 'wing_r_bout', -1)):
        p.bone(w, (s * 5, 25, -7), 'body')
        p.bone(t, (s * 17, 25.5, -7), w)
    p.sym('wing_l|wing_r', 'aile_bras', (5, 24.5, -9), (17, 26, -5.5), A, base=3.2)
    p.sym('wing_l|wing_r', 'aile_griffe', (15.5, 26, -8.5), (16.5, 28, -7.5), ('griffe', 'dragon_or'), base=2.4)
    p.sym('wing_l|wing_r', 'aile_plan', (5, 25.5, -5), (17, 26, 15), AP, base=3, doigts=2, hide={'north', 'south', 'east', 'west'}, no_shadow=True)
    p.sym('wing_l_bout|wing_r_bout', 'bout_bras', (17, 24.5, -8), (34, 26, -5.5), A, base=3.4)
    p.sym('wing_l_bout|wing_r_bout', 'bout_griffe', (34, 25, -8), (36.5, 25.5, -6.5), ('griffe', 'dragon_or'), base=2.6)
    p.sym('wing_l_bout|wing_r_bout', 'bout_plan', (17, 25.5, -5.5), (37, 26, 14), AP, base=3.2, doigts=4, hide={'north', 'south', 'east', 'west'}, no_shadow=True)
    # queue longue en trois os, aileron
    p.bone('tail', (0, 21, 16.5), 'body')
    T1 = dict(rot=(8, 0, 0), pivot=(0, 21, 16.5))
    p.cube('tail', 'queue_a', (-3, 18, 16), (3, 24, 27), S, **B, **T1)
    p.cube('tail', 'crete_qa', (-.5, 24, 17), (.5, 26.5, 27), ('pics', 'dragon_pourpre'), base=2.4, pas=7, dent=5, **CR, **T1)
    p.bone('tail_bout', (0, 19.5, 27), 'tail')
    T2 = dict(rot=(4, 0, 0), pivot=(0, 19.5, 27))
    p.cube('tail_bout', 'queue_b', (-2, 18, 26.5), (2, 22, 36), S, ton=.1, **B, **T2)
    p.cube('tail_bout', 'crete_qb', (-.5, 22, 27), (.5, 24, 36), ('pics', 'dragon_pourpre'), base=2.4, pas=6, dent=4, **CR, **T2)
    p.bone('queue3', (0, 19, 36), 'tail_bout')
    p.cube('queue3', 'queue_c', (-1, 18, 35.5), (1, 20.5, 44), S, ton=.2, **B)
    p.cube('queue3', 'aileron', (-.5, 13.5, 40), (.5, 25.5, 50), ('plan', 'membrane'), base=3.4, masque=fer(20, 24), **CR)
    # pattes griffues
    G = ('griffe', 'ivoire')
    patte(p, 'leg_fl', 'leg_fr', 'av', 2.5, 5, -11.5, -7.5, 19, 10, S, pied_h=2.5, avance=1.5, ton=-.1, ton_bas=-.2, griffes=G, base=3.3, bas=-.3)
    patte(p, 'leg_bl', 'leg_br', 'ar', 2, 5, 9, 13, 20, 10.5, S, pied_h=2.5, avance=1.5, retrait=1, ton=-.1, ton_bas=-.2, griffes=G, base=3.3, bas=-.3)
    p.sym('leg_fl|leg_fr', 'epaule', (2, 13.5, -12.5), (6.5, 22, -6.5), S, ton=.2, **B)
    p.sym('leg_bl|leg_br', 'cuisse', (1.5, 13, 8.5), (6, 22, 14.5), S, ton=.2, **B)
    selle(p, SEAT, 25, 5, 15.5, 'mythique', av=-5, ar=7, bas=18)
    rig = tuiles_rig(jaw='machoire', tail_tip='tail_bout', wings=('wing_l', 'wing_r'), wing_tips=('wing_l_bout', 'wing_r_bout'), wing_fold=(-78, -4, 66), tip_fold=(-6, 0),
                     idle=3.0, walk=.8, walk_amp=26, walk_knee=48, run=.46, leg_len=19.0)
    clips = quadrupede(rig, poids=.9)
    ajoute_vol(clips, rig, .7)
    # en vol le cou s'allonge vers l'avant
    T = clips['fly'][0]
    clips['fly'][2]['cou1'] = {'rotation': rot(T, x=sin(2, 1, .1, bias=-16))}
    clips['fly'][2]['cou2'] = {'rotation': rot(T, x=sin(2, 1, .2, bias=-18))}
    clips['fly'][2]['head'] = {'rotation': rot(T, x=sin(2, 1, .3, bias=20))}
    ajoute_chaine(clips, ['cou1', 'cou2'], ax=(1.4, 2.5, 3.5), az=(1.5, 2.5, 3), retard=.1)
    ajoute_chaine(clips, ['queue3'], ax=(1.5, 3, 5), az=(10, 15, 18), retard=.5)
    add_all(p, clips)
    return p
