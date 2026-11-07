#!/usr/bin/env python3
"""Zombie des cryptes : cadavre voute, chair grise, machoire pendante, un oeil laiteux et une orbite vide, bandage de linceul, cotes a nu au flanc, chemise en lambeaux, un bras tendu devant lui et l'autre casse.
Il traine la jambe gauche en tanguant, saisit a deux mains et mord, vomit son miasme courbe en deux, puis s'ecroule en avant."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, keys, pulse, sin
from kit_mob import CF, CR, FR, M
from materiaux import stamp
from poses import LANCER, P, R, appui, clip, cycle, jambe

Z = ('peau', 'zombie')
TB = ('peau_tissu', 'tissu_brun')
OS = ('os', 'os_vieux')
BA = ('bandelette', 'bandelette_sale')
P_ = {'k': '#0b0a0f', 'w': '#d8e0c8', 'p': '#39402a', 'r': '#6b1a18', 'R': '#3c0e10', 'o': '#c9c2a4', 'd': '#2c3322', 'e': '#ff5a3c'}


def build():
    m = M('zombie_des_cryptes', 275, 1.0, kind='classic', description='Zombie des cryptes Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=13, epaule=23, cou=24, x_bras=6, x_jambe=3, coude=16, genou=6, z_bras=-1, z_cou=-3)
    # pose : bras droit tendu, bras gauche pendant au coude casse, tete tombee sur l'epaule
    p.bones['arm_-1'].rot = (74, 0, 4)
    p.bones['avbras_-1'].rot = (8, 0, 0)
    p.bones['arm_1'].rot = (34, 0, -10)
    p.bones['avbras_1'].rot = (-30, 16, 0)
    p.bones['head'].rot = (0, 0, -9)

    # --- jambes : pantalon dechire a droite, tibia a nu a gauche
    m.sb('leg_-1|leg_1', 'cuisse', (1, 6, -2), (5, 14, 2), TB, base=2.6)
    m.cb('tibia_-1', 'mollet', (1, 2, -2), (5, 7, 2), TB, base=2.2)
    m.cb('tibia_1', 'mollet_chair', (-5, 3, -2), (-1, 7, 1), Z, base=2.6)
    m.cb('tibia_1', 'tibia_nu', (-4, 1, -1), (-2, 4, 1), OS, base=3.8)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -4), (5, 2, 2), Z, base=2.3)

    # --- bassin, corde, chemise en lambeaux, ventre ouvert, cotes a nu
    m.b = 'body'
    m.c('bassin', (-5, 12, -3), (5, 15, 3), TB, base=2)
    m.c('corde', (-5, 14, -4), (5, 15, 4), ('cordage', 'corde'), base=2)
    m.c('pan', (-3, 7, -4), (3, 14, -3), FR('tissu_brun'), base=3.2, mini=.5, **CF)
    m.c('pan_dos', (-4, 6, 3), (4, 14, 4), FR('tissu_brun'), base=2.6, mini=.45, **CF)
    p.bone('buste', (0, 14, 0), 'body', rot=(-14, 0, 0))
    m.b = 'buste'
    n = m.c('chemise', (-5, 15, -3), (5, 24, 3), TB, base=3)
    p.decal(n, 'north', stamp(['..........', '..........', '...ppp....', '..prrrp...', '..prRRrp..', '...prRrp..', '....ppp...', '..........', '..........'], P_, 5, 24))
    p.decal(n, 'west', stamp(['......', '.Ro.R.', '.Ro.R.', '.Ro.R.', '.Ro.R.', '......'], P_, 3, 22))
    m.c('epaule_haut', (-6, 22, -3), (6, 25, 3), Z, base=3)
    m.c('dos_rond', (-4, 18, 3), (4, 25, 5), TB, base=2.4)

    # --- tete penchee : crane cireux, bandage, oeil laiteux, orbite vide, machoire qui pend
    m.b = 'head'
    n = m.c('crane', (-4, 26, -8), (4, 33, 0), Z, base=3.2)
    p.decal(n, 'north', stamp(['........', '........', 'kkk..ppp', 'kek.pwwp', 'kkk.pwkp', '........', 'kkkkkkkk'][1:], P_, 4, 32))
    p.decal(n, 'east', stamp(['..oo....', '.oooo...', '..oo....'], P_, 0, 32))
    m.lueur('head', 'oeil_rouge', (2, 29, -9), (3, 30, -8), ('uni', 'venin'), base=4.6, hide={'south'})
    m.c('nuque', (-2, 24, -4), (2, 27, 0), Z, base=2.4)
    m.c('nez', (-1, 28, -9), (1, 30, -8), Z, base=2.4, hide={'south'})
    m.c('bandage', (-5, 31, -9), (5, 33, 1), BA, base=3.6)
    p.bone('bout_bandage', (4, 32, 1), 'head')
    m.cb('bout_bandage', 'bandage_pend', (3, 26, 1), (5, 32, 2), FR('bandelette_sale'), base=3.2, mini=.6, **CF)
    n = m.c('calotte', (-3, 33, -7), (3, 35, -1), Z, base=3.6)
    p.decal(n, 'up', stamp(['d..dd.', '.dd...', 'd...d.', '..d..d', 'd....d', '.dd.d.'], P_, -3, -7))
    m.c('gorge', (-3, 24, -7), (3, 27, -2), ('uni', 'nuit'), base=.5)
    p.bone('machoire', (0, 26, -2), 'head', rot=(-16, 0, 0))
    n = m.cb('machoire', 'mandibule', (-3, 23, -8), (3, 26, -1), Z, base=2.7)
    p.decal(n, 'north', stamp(['o.oo.o'], P_, 3, 26))
    p.decal(n, 'up', stamp(['RRRRRR', 'RRRRRR', 'RRRRRR', 'RRRRRR', 'RRRRRR', 'RRRRRR', 'RRRRRR'], P_, -3, -8))

    # --- bras decharnes : main droite griffue, avant-bras gauche ronge jusqu'a l'os
    m.sb('arm_-1|arm_1', 'bras', (4, 16, -3), (8, 25, 1), Z, base=2.9)
    m.sb('arm_-1|arm_1', 'manche', (4, 19, -3), (8, 25, 1), TB, base=2.6, inflate=.01)
    m.cb('avbras_-1', 'avantbras', (4, 9, -3), (8, 17, 1), Z, base=3.1)
    m.cb('avbras_-1', 'main', (4, 6, -4), (8, 9, 2), Z, base=3.4)
    m.cb('avbras_-1', 'griffe', (4, 4, -4), (5, 6, -3), ('griffe', 'ivoire'), base=2.4)
    m.cb('avbras_-1', 'griffe', (6, 4, -4), (7, 6, -3), ('griffe', 'ivoire'), base=2.4)
    m.cb('avbras_1', 'chair_bras', (-8, 12, -3), (-4, 17, 1), Z, base=2.8)
    m.cb('avbras_1', 'os_bras', (-7, 8, -2), (-5, 13, 0), OS, base=3.8)
    m.cb('avbras_1', 'main_morte', (-8, 5, -3), (-4, 8, 1), Z, base=2.4)

    # ------------------------------------------------------------------ repos : balance a peine, la tete roule lentement, la machoire ballotte
    T = 4.2
    cycle(p, 'idle', T, {
        'root': {'position': keys(T, lambda q: (sin(.8, 1, 0)(q), 0, sin(.3, 1, .25)(q)), 16)},
        'body': R(T, x=sin(1.5, 1, .25, bias=-2), z=sin(2.5, 1, 0)),
        'head': R(T, x=sin(5, 1, .3), y=sin(8, 1, .05), z=sin(6, 1, .2)),
        'machoire': R(T, x=combine(sin(4, 2, .1, bias=-2), pulse(-12, .55, .05)), n=32),
        'arm_-1': R(T, x=sin(4, 1, .15), z=sin(2, 1, .4)),
        'avbras_-1': R(T, x=sin(4, 1, .2)),
        'arm_1': R(T, x=sin(3, 1, .35), z=sin(2.5, 1, .1)),
        'avbras_1': R(T, x=sin(5, 1, .45)),
        'bout_bandage': R(T, x=sin(5, 1, .2), z=sin(4, 1, .5)),
    })

    # ------------------------------------------------------------------ deplacement : un grand pas de la jambe droite, puis la gauche qu'il traine ; tout le corps tangue vers la jambe d'appui
    T = 1.9
    u0, l0 = jambe(T, 30, 0, 46, reach=.2)
    u1, l1 = jambe(T, 13, .5, 6, reach=.05, bias=-8)
    cycle(p, 'walk', T, {
        'root': {'position': keys(T, lambda q: (sin(1.6, 1, .25)(q), appui(T, 30, 13)(q) + cos(.6, 2, .08)(q), 0), 24)},
        'body': R(T, x=sin(2.5, 2, .05, bias=-3), y=sin(9, 1, 0), z=sin(9, 1, .25)),
        'head': R(T, x=sin(4, 2, .25), y=sin(-6, 1, .05), z=sin(10, 1, .35)),
        'machoire': R(T, x=sin(7, 2, .15, bias=-3)),
        'leg_-1': u0, 'tibia_-1': l0, 'leg_1': u1, 'tibia_1': l1,
        'arm_-1': R(T, x=sin(8, 2, .1), z=sin(4, 1, .3)),
        'avbras_-1': R(T, x=sin(6, 2, .2)),
        'arm_1': R(T, x=sin(14, 1, .1), z=sin(5, 1, .3)),
        'avbras_1': R(T, x=sin(16, 1, .25)),
        'bout_bandage': R(T, x=sin(12, 2, .2, bias=8), z=sin(8, 1, .4)),
    })

    # ------------------------------------------------------------------ attaque : saisit a deux mains, tire vers lui et mord
    clip(p, 'attack', 1.0, [
        (.22, {'root+': (0, 0, .8), 'body': (10, 0, 0), 'head': (14, 0, 0), 'machoire': (-34, 0, 0), 'arm_-1': (24, 0, 26), 'avbras_-1': (10, 0, 0), 'arm_1': (84, 0, -30), 'avbras_1': (30, -16, 0)}),
        (.42, {'root+': (0, -1, -3), 'body': (-14, 0, 0), 'head': (-6, 0, 0), 'machoire': (-40, 0, 0), 'arm_-1': (6, -30, -14), 'avbras_-1': (-4, 0, 0), 'arm_1': (56, 26, 10), 'avbras_1': (28, -16, 0), 'leg_-1': (28, 0, 0), 'tibia_-1': (-30, 0, 0), 'leg_1': (-16, 0, 0)}),
        (.58, {'root+': (0, -1.4, -3.4), 'body': (-20, 0, 0), 'head': (-18, 0, 0), 'head+': (0, -1, -1.6), 'machoire': (10, 0, 0), 'arm_-1': (-12, -36, -18), 'avbras_-1': (24, 0, 0), 'arm_1': (44, 32, 14), 'avbras_1': (44, -16, 0), 'leg_-1': (30, 0, 0), 'tibia_-1': (-32, 0, 0), 'leg_1': (-18, 0, 0)}),
        (.82, {'root+': (0, -.3, -.8), 'body': (-4, 0, 0), 'head': (-3, 0, 0), 'machoire': (-8, 0, 0), 'arm_-1': (-2, -6, -2), 'avbras_-1': (4, 0, 0), 'arm_1': (8, 4, 0), 'leg_-1': (6, 0, 0), 'leg_1': (-4, 0, 0)}),
    ])

    # ------------------------------------------------------------------ sort : se cabre, se casse en deux et rend son miasme, gueule ouverte vers le sol
    clip(p, 'cast', 1.6, [
        (.16, {'root+': (0, .4, .8), 'body': (16, 0, 0), 'head': (24, 0, 0), 'machoire': (-16, 0, 0), 'arm_-1': (-40, 0, 16), 'avbras_-1': (16, 0, 0), 'arm_1': (-6, 0, -18), 'avbras_1': (20, 0, 0)}),
        (LANCER, {'root+': (0, -2, -2), 'body': (-36, 0, 0), 'head': (-4, 0, 0), 'machoire': (-44, 0, 0), 'arm_-1': (-70, 0, 12), 'avbras_-1': (10, 0, 0), 'arm_1': (-24, 0, -14), 'avbras_1': (10, 0, 0), 'leg_-1': (26, 0, 0), 'tibia_-1': (-34, 0, 0), 'leg_1': (6, 0, 0), 'tibia_1': (-18, 0, 0)}),
        (.6, {'root+': (0, -1.6, -1.6), 'body': (-30, 3, 3), 'head': (-10, 0, 3), 'machoire': (-36, 0, 0), 'arm_-1': (-62, 0, 10), 'avbras_-1': (14, 0, 0), 'arm_1': (-20, 0, -12), 'avbras_1': (12, 0, 0), 'leg_-1': (22, 0, 0), 'tibia_-1': (-30, 0, 0), 'tibia_1': (-14, 0, 0)}),
        (.85, {'root+': (0, -2, -2), 'body': (-38, -3, -3), 'head': (-2, 0, -3), 'machoire': (-46, 0, 0), 'arm_-1': (-70, 0, 12), 'avbras_-1': (10, 0, 0), 'arm_1': (-24, 0, -14), 'avbras_1': (10, 0, 0), 'leg_-1': (26, 0, 0), 'tibia_-1': (-34, 0, 0), 'tibia_1': (-18, 0, 0)}),
        (1.3, {'root+': (0, -.4, -.3), 'body': (-5, 0, 0), 'head': (2, 0, 0), 'machoire': (-8, 0, 0), 'arm_-1': (-10, 0, 2), 'arm_1': (-4, 0, -2), 'leg_-1': (4, 0, 0)}),
    ])

    # ------------------------------------------------------------------ mort : les bras retombent les premiers, puis les genoux, il part en avant comme un sac
    clip(p, 'death', 1.0, [
        (.25, {'body': (8, 0, 5), 'head': (22, 0, 16), 'machoire': (-22, 0, 0), 'arm_-1': (-44, 0, 8), 'avbras_-1': (6, 0, 0), 'arm_1': (-20, 0, -8), 'avbras_1': (20, 0, 0)}),
        (.65, {'root+': (0, -5, -1), 'body': (-22, 0, 4), 'head': (-14, 0, 22), 'machoire': (-30, 0, 0), 'arm_-1': (-82, 0, 12), 'avbras_-1': (-8, 0, 0), 'arm_1': (-46, 0, -6), 'avbras_1': (30, 0, 0), 'leg_-1': (64, 0, 0), 'tibia_-1': (-100, 0, 0), 'leg_1': (52, 0, 0), 'tibia_1': (-92, 0, 0)}),
        (1.0, {'root+': (0, -6, -2), 'body': (-44, 0, 6), 'head': (-30, 0, 26), 'machoire': (-22, 0, 0), 'arm_-1': (-96, 0, 18), 'avbras_-1': (-8, 0, 0), 'arm_1': (-58, 0, -10), 'avbras_1': (34, 0, 0), 'leg_-1': (72, 0, 0), 'tibia_-1': (-112, 0, 0), 'leg_1': (60, 0, 0), 'tibia_1': (-104, 0, 0)}),
    ], retour=False)
    return p
