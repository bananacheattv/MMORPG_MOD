#!/usr/bin/env python3
"""Squelette maudit : os jaunis, cage ouverte sur une ame bleue, crane fele aux orbites froides, carcan de fer a la gorge, menotte a chaine cassee, lambeaux de cape bleue, vieille epee ebrechee.
Marche saccadee et boiteuse ou la tete tressaute, taille horizontale, priere muette l'epee au ciel, et a sa mort il s'effondre sur place, le crane roule."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, keys, pulse, sin
from kit_mob import CF, CR, FR, M
from materiaux import stamp
from mobs_cryptes import cage
from poses import LANCER, P, R, appui, clip, cycle, jambe

OS = ('os', 'os')
OSV = ('os', 'os_vieux')
FE = ('metal', 'fer')
NO = ('plaque', 'fer_noir')
P_ = {'k': '#0b0a0f', 'n': '#1a1820', 'b': '#3ac6d4', 'c': '#dcffff', 'd': '#6a5f45', 'w': '#f2ecd2'}


def build():
    m = M('squelette_maudit', 274, 1.0, kind='classic', description='Squelette maudit Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=14, epaule=25, cou=26, x_bras=6, x_jambe=3, coude=17, genou=7, z_cou=-1)

    # --- jambes : femur, rotule, tibia, pied a orteils
    m.sb('leg_-1|leg_1', 'femur', (2, 7, -1), (4, 15, 1), OS, ton=-.1)
    m.sb('tibia_-1|tibia_1', 'rotule', (2, 6, -2), (5, 8, 1), OS, ton=.7)
    m.sb('tibia_-1|tibia_1', 'tibia', (2, 2, -1), (4, 7, 1), OS, ton=-.3)
    n = m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -4), (5, 2, 1), OS, ton=.2)
    p.decal(n, 'north', stamp(['....', 'k.k.'], P_, 5, 2))

    # --- bassin, colonne, cage ouverte : l'ame brille derriere les cotes, la cape fait un fond sombre
    m.b = 'body'
    n = m.c('bassin', (-4, 13, -2), (4, 16, 2), OS, ton=.1)
    p.decal(n, 'north', stamp(['........', '.kk..kk.', '.kk..kk.'], P_, 4, 16))
    m.c('colonne', (-1, 15, 0), (1, 26, 2), OS, ton=-.4)
    cage(m, 18, 4, 4, 3, pas=2, h=1, zf=0)
    m.c('clavicule', (-6, 25, -2), (6, 26, 1), OS, ton=.5)
    m.lueur('body', 'ame', (-2, 19, -2), (2, 23, 0), ('uni', 'ame'), base=3.8)
    m.c('cape', (-5, 8, 2), (5, 26, 3), FR('tissu_bleu'), base=1.6, mini=.45, **CF)
    m.c('pagne', (-3, 7, -3), (3, 14, -2), FR('tissu_bleu'), base=2.4, mini=.5, **CF)
    m.c('sangle', (-4, 15, -3), (4, 16, 3), ('cuir', 'cuir'), ton=-1.5, couture=False)
    # carcan
    m.c('carcan', (-3, 25, -3), (3, 27, 2), NO, base=2.8, rivets=2)
    p.bone('chaine_cou', (2, 25, -3), 'body')
    m.cb('chaine_cou', 'maillon', (1, 21, -4), (2, 25, -3), ('mailles', 'fer'), base=3)

    # --- crane fele, orbites profondes, machoire a part
    m.b = 'head'
    n = m.c('crane', (-4, 29, -5), (4, 37, 3), OS, ton=.3)
    p.decal(n, 'north', stamp(['...dd...', '..d.....', '..d.....', 'kkk..kkk', 'kkk..kkk', 'kkk..kkk', '...kk...', '...kk...'], P_, 4, 37))
    p.decal(n, 'east', stamp(['........', '........', '.......k', '......kk'], P_, 3, 36))
    m.yeux('head', 2, 32, -5, 1, 1, ramp='ame', base=4.6, dz=1)
    n = m.c('face', (-3, 27, -5), (3, 29, 0), OS, ton=.1)
    p.decal(n, 'north', stamp(['kwkwkw', 'wkwkwk'], P_, 3, 29))
    m.s('tempe', (4, 31, -3), (5, 34, 1), OS, ton=-.3)
    m.c('gorge_noire', (-2, 26, -4), (2, 28, -1), ('uni', 'nuit'), base=.5)
    p.bone('machoire', (0, 28, -1), 'head')
    n = m.cb('machoire', 'mandibule', (-3, 25, -5), (3, 27, 0), OS, ton=-.2)
    p.decal(n, 'north', stamp(['wkwkwk'], P_, 3, 27))

    # --- bras : humerus, tete d'os, radius, main a doigts, menotte a gauche
    m.sb('arm_-1|arm_1', 'epaule', (5, 24, -2), (8, 27, 1), OS, ton=.6)
    m.sb('arm_-1|arm_1', 'humerus', (5, 18, -1), (7, 25, 1), OS)
    m.sb('avbras_-1|avbras_1', 'radius', (5, 11, -1), (7, 18, 1), OS, ton=-.2)
    m.sb('avbras_-1|avbras_1', 'main', (5, 8, -2), (8, 11, 1), OS, ton=.3)
    m.sb('avbras_-1|avbras_1', 'doigt', (5, 6, -2), (6, 8, -1), OS, ton=.2)
    m.sb('avbras_-1|avbras_1', 'doigt_b', (7, 6, -2), (8, 8, -1), OS, ton=.2)
    m.cb('avbras_1', 'menotte', (-8, 12, -2), (-4, 14, 2), NO, base=3)
    p.bone('chaine_main', (-6, 12, 0), 'avbras_1')
    m.cb('chaine_main', 'chainon', (-7, 5, -1), (-6, 12, 0), ('mailles', 'fer'), base=3.2)

    # --- vieille epee ebrechee
    p.bone('epee', (6.5, 9, -.5), 'avbras_-1', rot=(-50, 0, 0))
    m.cb('epee', 'fusee', (6, 7, -1), (7, 14, 0), ('cuir', 'cuir'), ton=-.8, couture=False)
    m.cb('epee', 'pommeau', (5, 5, -2), (8, 7, 1), FE, base=2.6)
    m.cb('epee', 'garde', (5, 14, -4), (8, 15, 3), FE, base=2.8)
    L = ['.#.', '###', '##.', '###', '###', '.##', '###', '###', '###', '##.', '###', '###', '###', '###', '###', '###']
    m.cb('epee', 'lame', (6, 15, -2), (7, 31, 1), ('plan', 'acier'), base=3.4, masque=L, fil=1, **CR)

    # ------------------------------------------------------------------ repos : tient a peine debout, tete penchee qui tressaute, la chaine se balance
    T = 3.0
    cycle(p, 'idle', T, {
        'body': R(T, x=sin(1, 1, .1, bias=-3), z=sin(1.5, 1, .3, bias=2)),
        'head': R(T, x=sin(1, 1, .3, bias=2), y=combine(sin(5, 1, 0), pulse(12, .3, .03)), z=combine(sin(1, 1, .2, bias=-7), pulse(-12, .7, .03)), n=40),
        'machoire': R(T, x=combine(sin(2, 1, .2, bias=-4), pulse(-14, .3, .03), pulse(-14, .36, .03)), n=40),
        'arm_-1': R(T, x=sin(1.5, 1, .1, bias=10), z=sin(1, 1, .3, bias=2)),
        'avbras_-1': R(T, x=sin(2, 1, .15, bias=12)),
        'arm_1': R(T, x=sin(-2, 1, .1, bias=-2), z=sin(-1.5, 1, .3, bias=-4)),
        'avbras_1': R(T, x=sin(-2, 1, .15, bias=6)),
        'chaine_main': R(T, x=sin(5, 1, .2), z=sin(4, 1, .45)),
        'chaine_cou': R(T, x=sin(5, 1, .35)),
    })

    # ------------------------------------------------------------------ deplacement : boiterie saccadee, jambe droite raide et jambe gauche trainante, bassin qui bascule, bras gauche ballant
    T = 1.15
    a0, a1 = 18, 30
    u0, l0 = jambe(T, a0, 0, 22, reach=.1)
    u1, l1 = jambe(T, a1, .5, 40, reach=.2)
    cycle(p, 'walk', T, {
        'root': {'position': keys(T, lambda q: (sin(.7, 1, .25)(q), appui(T, a1, 13)(q) - .8 * pulse(1, .55, .08)(q), 0), 24)},
        'body': R(T, x=sin(1.5, 1, .1, bias=-5), y=sin(9, 1, 0), z=sin(6, 1, .3)),
        'head': R(T, x=sin(2, 2, .1, bias=2), y=sin(-7, 1, 0), z=combine(sin(-6, 1, .3, bias=-3), pulse(16, .05, .04), pulse(-12, .56, .04)), n=40),
        'machoire': R(T, x=combine(sin(3, 2, .2, bias=-5), pulse(-10, .06, .05), pulse(-10, .57, .05)), n=40),
        'leg_-1': u0, 'tibia_-1': l0, 'leg_1': u1, 'tibia_1': l1,
        'arm_-1': R(T, x=sin(-6, 1, 0, bias=22)),
        'avbras_-1': R(T, x=sin(4, 1, .2, bias=18)),
        'epee': R(T, x=sin(3, 2, .1, bias=-18)),
        'arm_1': R(T, x=sin(30, 1, .08, bias=4), z=sin(-5, 1, .3, bias=-6)),
        'avbras_1': R(T, x=sin(22, 1, .2, bias=14)),
        'chaine_main': R(T, x=sin(14, 1, .3), z=sin(8, 1, .5)),
        'chaine_cou': R(T, x=sin(9, 2, .3)),
    })

    # ------------------------------------------------------------------ attaque : large taille de droite a gauche, tout le buste tourne, la tete suit en retard
    clip(p, 'attack', 1.0, [
        (.25, {'root+': (0, -.4, .6), 'body': (2, -40, 0), 'head': (-2, 26, 0), 'arm_-1': (72, 0, 58), 'avbras_-1': (24, 0, 0), 'epee': (-90, 0, 0), 'arm_1': (24, 0, -26), 'avbras_1': (20, 0, 0), 'leg_-1': (-10, 0, 0), 'leg_1': (10, 0, 0), 'machoire': (-8, 0, 0)}),
        (.44, {'root+': (0, -1.2, -2), 'body': (-8, 44, 0), 'head': (4, -34, 0), 'arm_-1': (84, 0, -30), 'avbras_-1': (6, 0, 0), 'epee': (-124, 0, 0), 'arm_1': (-22, 0, -30), 'avbras_1': (12, 0, 0), 'leg_-1': (28, 0, 0), 'tibia_-1': (-26, 0, 0), 'leg_1': (-20, 0, 0), 'machoire': (-34, 0, 0)}),
        (.6, {'root+': (0, -1, -1.6), 'body': (-6, 50, 0), 'head': (3, -38, 0), 'arm_-1': (72, 0, -44), 'avbras_-1': (10, 0, 0), 'epee': (-110, 0, 0), 'arm_1': (-14, 0, -24), 'leg_-1': (24, 0, 0), 'tibia_-1': (-22, 0, 0), 'leg_1': (-16, 0, 0), 'machoire': (-26, 0, 0)}),
        (.85, {'root+': (0, -.2, -.3), 'body': (-1, 8, 0), 'head': (1, -6, 0), 'arm_-1': (18, 0, -6), 'avbras_-1': (12, 0, 0), 'epee': (-20, 0, 0), 'leg_-1': (4, 0, 0), 'machoire': (-6, 0, 0)}),
    ])

    # ------------------------------------------------------------------ sort : priere muette. Epee pointee au ciel, la tete bascule, la machoire tombe, tout le corps tremble
    clip(p, 'cast', 1.7, [
        (.18, {'root+': (0, -1, 0), 'body': (-10, 0, 0), 'head': (-14, 0, 0), 'arm_-1': (60, 0, 6), 'avbras_-1': (40, 0, 0), 'epee': (-50, 0, 0), 'arm_1': (20, 0, -20), 'avbras_1': (40, 0, 0)}),
        (LANCER, {'root+': (0, 1, 0), 'body': (10, 0, 0), 'head': (28, 0, 0), 'machoire': (-40, 0, 0), 'arm_-1': (176, 0, 6), 'avbras_-1': (2, 0, 0), 'epee': (-128, 0, 0), 'arm_1': (96, 0, -40), 'avbras_1': (30, 0, 0), 'chaine_main': (-30, 0, 0)}),
        (.7, {'root+': (0, 1, 0), 'body': (9, 2, 1), 'head': (26, 3, 2), 'machoire': (-38, 0, 0), 'arm_-1': (174, 0, 8), 'avbras_-1': (4, 0, 0), 'epee': (-128, 0, 0), 'arm_1': (92, 0, -38), 'avbras_1': (32, 0, 0)}),
        (.82, {'root+': (0, 1.2, 0), 'body': (11, -2, -1), 'head': (28, -3, -2), 'machoire': (-40, 0, 0), 'arm_-1': (178, 0, 4), 'avbras_-1': (0, 0, 0), 'epee': (-128, 0, 0), 'arm_1': (96, 0, -42), 'avbras_1': (28, 0, 0)}),
        (1.0, {'root+': (0, 1, 0), 'body': (9, 2, 1), 'head': (26, 3, 2), 'machoire': (-38, 0, 0), 'arm_-1': (174, 0, 8), 'avbras_-1': (4, 0, 0), 'epee': (-128, 0, 0), 'arm_1': (92, 0, -38), 'avbras_1': (32, 0, 0)}),
        (1.4, {'root+': (0, 0, 0), 'body': (0, 0, 0), 'head': (4, 0, 0), 'machoire': (-8, 0, 0), 'arm_-1': (34, 0, 2), 'avbras_-1': (16, 0, 0), 'epee': (-24, 0, 0), 'arm_1': (10, 0, -8), 'avbras_1': (10, 0, 0)}),
    ])

    # ------------------------------------------------------------------ mort : tout se defait. Les jambes plient, le buste descend, le crane se detache et tombe, l'epee lache
    clip(p, 'death', 1.0, [
        (.18, {'body': (8, 0, 8), 'head': (12, 0, -14), 'machoire': (-30, 0, 0), 'arm_-1': (40, 0, 38), 'avbras_-1': (28, 0, 0), 'arm_1': (44, 0, -42), 'avbras_1': (20, 0, 0)}),
        (.55, {'root+': (0, -7, 0), 'body': (-20, 0, 4), 'head': (-30, 10, 16), 'head+': (0, -3, -2), 'machoire': (-34, 0, 0), 'arm_-1': (8, 0, 26), 'avbras_-1': (6, 0, 0), 'epee': (70, 0, 0), 'arm_1': (6, 0, -26), 'leg_-1': (78, 0, -8), 'tibia_-1': (-120, 0, 0), 'leg_1': (70, 0, 8), 'tibia_1': (-118, 0, 0)}),
        (1.0, {'root+': (0, -9, 0), 'body': (-44, 0, 6), 'head': (-74, 24, 46), 'head+': (0, -13, -5), 'machoire': (-26, 0, 0), 'arm_-1': (-8, 0, 38), 'avbras_-1': (4, 0, 0), 'epee': (110, 0, 0), 'arm_1': (-10, 0, -40), 'leg_-1': (86, 0, -12), 'tibia_-1': (-132, 0, 0), 'leg_1': (78, 0, 12), 'tibia_1': (-130, 0, 0)}),
    ], retour=False)
    return p
