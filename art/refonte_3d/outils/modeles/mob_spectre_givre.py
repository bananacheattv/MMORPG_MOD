#!/usr/bin/env python3
"""Spectre de givre : robe vide qui flotte au ras du sol, capuche sur un visage noir et deux yeux de glace, couronne d'eclats, coeur gele dans la poitrine, longues griffes de glace, deux eclats qui orbitent.
Il glisse penche en avant sans poser les pieds, griffe en X en se jetant sur la cible, lance une onde en ouvrant les bras, et a sa mort la robe se vide et s'affaisse."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, keys, pulse, sin
from kit_mob import CF, CR, FR, M
from materiaux import stamp
from poses import LANCER, P, R, clip, cycle

V = ('voile', 'glace')
VS = ('voile', 'spectre')
GL = ('glace', 'glace')
P_ = {'k': '#05070d', 'n': '#0a1a28', 'c': '#dff8ff'}


def build():
    m = M('spectre_givre', 277, 1.0, kind='classic', description='Spectre de givre Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=16, epaule=28, cou=29, x_bras=7, x_jambe=3, coude=20, genou=9, z_cou=-2)
    p.bones['arm_-1'].rot = (16, 0, 10)
    p.bones['arm_1'].rot = (16, 0, -10)
    p.bones['avbras_-1'].rot = (24, 0, 0)
    p.bones['avbras_1'].rot = (24, 0, 0)

    # --- pas de jambes : deux trainees de voile, sous une robe effilochee
    m.sb('leg_-1|leg_1', 'trainee', (1, 9, -3), (6, 17, 3), VS, base=3, alpha=225)
    m.sb('tibia_-1|tibia_1', 'trainee_b', (2, 5, -2), (5, 10, 2), VS, base=2.4, alpha=205)
    m.sb('tibia_-1|tibia_1', 'trainee_c', (3, 2, -1), (4, 6, 1), VS, base=1.8, alpha=170)
    m.b = 'body'
    m.c('pan', (-6, 7, -4), (6, 17, -3), FR('glace'), base=2.8, mini=.4, **CF)
    m.c('pan_dos', (-6, 5, 3), (6, 17, 4), FR('glace'), base=2.2, mini=.4, **CF)
    m.s('pan_flanc', (6, 7, -4), (7, 17, 4), FR('glace'), base=2.4, mini=.4, **CR)

    # --- buste creux : taille pincee, coeur de glace, mantelet
    m.c('taille', (-4, 16, -3), (4, 20, 3), V, base=2.8)
    m.c('poitrine', (-5, 20, -3), (5, 27, 3), V, base=3.2)
    m.lueur('body', 'coeur', (-2, 21, -4), (2, 26, -3), ('cristal', 'glace'), base=3.4, hide={'south'})
    m.c('mantelet', (-7, 25, -4), (7, 29, 4), V, base=3.8)
    m.c('mantelet_bas', (-7, 21, -5), (7, 25, -4), FR('glace'), base=3.2, mini=.4, **CF)
    m.c('mantelet_dos', (-7, 19, 4), (7, 29, 5), FR('glace'), base=3, mini=.4, **CF)

    # --- capuche profonde, visage noir, couronne d'eclats
    m.b = 'head'
    n = m.c('capuche', (-4, 29, -7), (4, 38, 3), V, base=3.6)
    p.decal(n, 'north', stamp(['........', '.kkkkkk.', '.kkkkkk.', '.kkkkkk.', '.kkkkkk.', '.kkkkkk.', '.kkkkkk.', '..kkkk..', '........'], P_, 4, 38))
    m.yeux('head', 1, 33, -7, 2, 1, ramp='glace', base=5, dz=1)
    m.c('rebord', (-4, 37, -8), (4, 38, -7), V, base=4.6, hide={'south'})
    m.s('rebord_flanc', (4, 30, -8), (5, 38, -6), V, base=4.2)
    m.pointe('head', 'eclat', 0, 38, -3, 8, 1, GL, inc=(-6, 0), tourne=45, base=3.4)
    m.pointe('head', 'eclat', 3, 38, -2, 6, 1, GL, inc=(0, -14), tourne=45, base=3)
    m.pointe('head', 'eclat', -3, 38, -2, 6, 1, GL, inc=(0, 14), tourne=45, base=3)
    m.pointe('head', 'eclat', 2, 37, 1, 5, 1, GL, inc=(12, -8), tourne=45, base=2.6)
    m.pointe('head', 'eclat', -2, 37, 1, 5, 1, GL, inc=(12, 8), tourne=45, base=2.6)

    # --- epaules de glace, manches larges, griffes
    m.sb('arm_-1|arm_1', 'bras', (6, 20, -2), (9, 28, 1), V, base=3)
    m.sb('arm_-1|arm_1', 'epaule_glace', (5, 27, -3), (10, 30, 2), GL, base=2.6)
    m.pointe('arm_-1', 'pique', 8, 30, -1, 5, 1, GL, inc=(0, -16), tourne=45, base=3.2)
    m.pointe('arm_1', 'pique', -8, 30, -1, 5, 1, GL, inc=(0, 16), tourne=45, base=3.2)
    m.sb('avbras_-1|avbras_1', 'manche', (5, 14, -3), (10, 20, 2), V, base=2.7)
    m.sb('avbras_-1|avbras_1', 'manche_bas', (5, 9, -3), (10, 14, -2), FR('glace'), base=2.2, mini=.4, **CF)
    m.sb('avbras_-1|avbras_1', 'manche_bas_dos', (5, 9, 1), (10, 14, 2), FR('glace'), base=2, mini=.4, **CF)
    m.sb('avbras_-1|avbras_1', 'main', (6, 11, -2), (9, 14, 1), ('uni', 'spectre'), base=2)
    m.sb('avbras_-1|avbras_1', 'griffe', (6, 6, -3), (7, 11, -2), ('cristal', 'glace'), base=3.2)
    m.sb('avbras_-1|avbras_1', 'griffe_b', (8, 7, -3), (9, 11, -2), ('cristal', 'glace'), base=3.2)
    m.sb('avbras_-1|avbras_1', 'griffe_c', (7, 7, 0), (8, 11, 1), ('cristal', 'glace'), base=2.8)

    # --- deux eclats en orbite
    p.bone('eclat_a', (11, 22, -6), 'root')
    m.lueur('eclat_a', 'eclat_orbite', (10, 19, -7), (12, 25, -5), ('cristal', 'glace'), base=3.4, rot=(0, 45, 0), pivot=(11, 22, -6))
    p.bone('eclat_b', (-12, 30, 5), 'root')
    m.lueur('eclat_b', 'eclat_orbite', (-13, 28, 4), (-11, 32, 6), ('cristal', 'glace'), base=3.4, rot=(0, 45, 0), pivot=(-12, 30, 5))

    # ------------------------------------------------------------------ repos : flotte, tout ondule lentement et hors phase, les eclats tournent autour de lui
    T = 4.0
    cycle(p, 'idle', T, {
        'root': P(T, y=sin(1.2, 1, 0, bias=.6)),
        'body': R(T, x=sin(2, 1, .15, bias=-3), z=sin(1.5, 1, .4)),
        'head': R(T, x=sin(2.5, 1, .3), y=sin(8, 1, 0), z=sin(2, 1, .15)),
        'arm_-1': R(T, x=sin(5, 1, .1), z=sin(3, 1, .3)),
        'avbras_-1': R(T, x=sin(7, 1, .2)),
        'arm_1': R(T, x=sin(5, 1, .35), z=sin(-3, 1, .55)),
        'avbras_1': R(T, x=sin(7, 1, .45)),
        'leg_-1': R(T, x=sin(5, 1, .1), z=sin(3, 1, .35)),
        'tibia_-1': R(T, x=sin(9, 1, .2)),
        'leg_1': R(T, x=sin(5, 1, .4), z=sin(-3, 1, .65)),
        'tibia_1': R(T, x=sin(9, 1, .5)),
        'eclat_a': {'position': keys(T, lambda q: (sin(4, 1, .25)(q) - 4, sin(2.5, 2, 0)(q), sin(9, 1, 0)(q) + 8), 16), 'rotation': keys(T, lambda q: (0, 360 * q, 0), 16)},
        'eclat_b': {'position': keys(T, lambda q: (sin(-4, 1, .25)(q) + 4, sin(-2, 2, .1)(q), sin(-8, 1, 0)(q) - 7), 16), 'rotation': keys(T, lambda q: (0, -360 * q, 0), 16)},
    })

    # ------------------------------------------------------------------ deplacement : glisse sans un pas, buste couche vers l'avant, bras et voiles qui trainent en arriere, le tout en vagues
    T = 2.2
    cycle(p, 'walk', T, {
        'root': P(T, y=sin(1.2, 1, 0, bias=1)),
        'body': R(T, x=sin(2, 1, .2, bias=-20), y=sin(3, 1, 0), z=sin(2, 1, .25)),
        'head': R(T, x=sin(2, 1, .3, bias=13), y=sin(-3, 1, 0)),
        'arm_-1': R(T, x=sin(7, 1, .1, bias=-30), z=sin(4, 1, .3, bias=6)),
        'avbras_-1': R(T, x=sin(8, 1, .2, bias=-6)),
        'arm_1': R(T, x=sin(7, 1, .35, bias=-30), z=sin(-4, 1, .55, bias=-6)),
        'avbras_1': R(T, x=sin(8, 1, .45, bias=-6)),
        'leg_-1': R(T, x=sin(8, 1, .1, bias=-26)),
        'tibia_-1': R(T, x=sin(12, 1, .22, bias=-16)),
        'leg_1': R(T, x=sin(8, 1, .55, bias=-26)),
        'tibia_1': R(T, x=sin(12, 1, .67, bias=-16)),
        'eclat_a': {'position': keys(T, lambda q: (sin(4, 1, .25)(q) - 4, sin(2.5, 2, 0)(q), sin(9, 1, 0)(q) + 8), 16), 'rotation': keys(T, lambda q: (0, 360 * q, 0), 16)},
        'eclat_b': {'position': keys(T, lambda q: (sin(-4, 1, .25)(q) + 4, sin(-2, 2, .1)(q), sin(-8, 1, 0)(q) - 7), 16), 'rotation': keys(T, lambda q: (0, -360 * q, 0), 16)},
    })

    # ------------------------------------------------------------------ attaque : se dresse, bras croises haut, puis fond sur la cible et griffe en X
    clip(p, 'attack', 1.0, [
        (.25, {'root+': (0, 3, 2), 'body': (14, 0, 0), 'head': (14, 0, 0), 'arm_-1': (138, 0, 24), 'avbras_-1': (26, 0, 0), 'arm_1': (138, 0, -24), 'avbras_1': (26, 0, 0), 'leg_-1': (14, 0, 0), 'leg_1': (14, 0, 0)}),
        (.44, {'root+': (0, -1, -6), 'body': (-28, 0, 0), 'head': (16, 0, 0), 'arm_-1': (44, -26, -26), 'avbras_-1': (-6, 0, 0), 'arm_1': (32, 26, 26), 'avbras_1': (-6, 0, 0), 'leg_-1': (-36, 0, 0), 'tibia_-1': (-20, 0, 0), 'leg_1': (-36, 0, 0), 'tibia_1': (-20, 0, 0)}),
        (.6, {'root+': (0, -1, -5.4), 'body': (-24, 0, 0), 'head': (14, 0, 0), 'arm_-1': (24, -40, -30), 'avbras_-1': (-2, 0, 0), 'arm_1': (14, 40, 30), 'avbras_1': (-2, 0, 0), 'leg_-1': (-30, 0, 0), 'tibia_-1': (-20, 0, 0), 'leg_1': (-30, 0, 0), 'tibia_1': (-20, 0, 0)}),
        (.85, {'root+': (0, 0, -1), 'body': (-4, 0, 0), 'head': (2, 0, 0), 'arm_-1': (6, -6, -4), 'arm_1': (6, 6, 4), 'leg_-1': (-6, 0, 0), 'leg_1': (-6, 0, 0)}),
    ])

    # ------------------------------------------------------------------ sort : se replie sur lui-meme, puis tout s'ouvre d'un coup, bras en croix, tete en arriere ; les eclats filent vers l'exterieur
    clip(p, 'cast', 1.6, [
        (.16, {'root+': (0, -1, 0), 'body': (-16, 0, 0), 'head': (-12, 0, 0), 'arm_-1': (42, -22, -26), 'avbras_-1': (60, 0, 0), 'arm_1': (42, 22, 26), 'avbras_1': (60, 0, 0), 'eclat_a+': (-8, 0, 5), 'eclat_b+': (9, 0, -5)}),
        (LANCER, {'root+': (0, 4, 0), 'body': (10, 0, 0), 'head': (26, 0, 0), 'arm_-1': (56, 0, 72), 'avbras_-1': (-20, 0, 0), 'arm_1': (56, 0, -72), 'avbras_1': (-20, 0, 0), 'leg_-1': (-10, 0, 16), 'leg_1': (-10, 0, -16), 'eclat_a+': (10, 3, -10), 'eclat_b+': (-10, 3, 10)}),
        (.8, {'root+': (0, 3.6, 0), 'body': (8, 0, 0), 'head': (22, 0, 0), 'arm_-1': (52, 0, 66), 'avbras_-1': (-16, 0, 0), 'arm_1': (52, 0, -66), 'avbras_1': (-16, 0, 0), 'leg_-1': (-8, 0, 12), 'leg_1': (-8, 0, -12), 'eclat_a+': (6, 2, -6), 'eclat_b+': (-6, 2, 6)}),
        (1.3, {'root+': (0, .5, 0), 'body': (0, 0, 0), 'head': (3, 0, 0), 'arm_-1': (8, 0, 8), 'avbras_-1': (-2, 0, 0), 'arm_1': (8, 0, -8), 'avbras_1': (-2, 0, 0)}),
    ])

    # ------------------------------------------------------------------ mort : la tete retombe, les manches tombent, la robe se vide et s'ecrase au sol, les eclats tombent aussi
    clip(p, 'death', 1.0, [
        (.25, {'root+': (0, 2, 0), 'body': (10, 0, 0), 'head': (30, 0, 0), 'arm_-1': (40, 0, 30), 'arm_1': (40, 0, -30), 'eclat_a+': (0, -4, 0), 'eclat_b+': (0, -6, 0)}),
        (.65, {'root+': (0, -5, 0), 'body*': (1, .75, 1), 'body': (-6, 0, 0), 'head': (-16, 0, 0), 'arm_-1': (-6, 0, 6), 'arm_1': (-6, 0, -6), 'leg_-1': (0, 0, 22), 'leg_1': (0, 0, -22), 'eclat_a+': (0, -16, 0), 'eclat_b+': (0, -24, 0)}),
        (1.0, {'root+': (0, -11, 0), 'body*': (1.1, .35, 1.1), 'body': (-10, 0, 0), 'head': (-40, 0, 0), 'head+': (0, -8, 0), 'arm_-1': (-16, 0, 20), 'arm_1': (-16, 0, -20), 'leg_-1': (0, 0, 40), 'leg_1': (0, 0, -40), 'eclat_a+': (0, -20, 0), 'eclat_b+': (0, -28, 0)}),
    ], retour=False)
    return p
