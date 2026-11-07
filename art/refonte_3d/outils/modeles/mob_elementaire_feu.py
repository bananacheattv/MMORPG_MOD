#!/usr/bin/env python3
"""Elementaire de feu : un brasier tenu par des roches. Noyau de lave fendu, masque de basalte aux yeux blancs, grande couronne de flammes, epaules de roche, poings de pierre qui flottent sans bras, tourbillon de feu a la place des jambes.
Il derive penche en avant, frappe en lancant ses poings l'un apres l'autre, se contracte puis explose pour son sort, et s'eteint en laissant tomber ses pierres."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, keys, pulse, sin
from kit_mob import CF, CR, FL, M, langue
from materiaux import stamp
from poses import LANCER, P, R, clip, cycle

LA = ('lave', 'basalte')
RO = ('basalte', 'basalte')
FP = ('flamme_pleine', 'flamme')
P_ = {'k': '#120806', 'w': '#fff9d8', 'y': '#fee56f', 'o': '#f97c14', 'r': '#a22408'}


def build():
    m = M('elementaire_feu', 278, 1.0, kind='classic', description='Elementaire de feu Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.bone('body', (0, 16, 0), 'root')
    p.bone('head', (0, 22, -1), 'body')

    # --- tourbillon a la place des jambes : trois etages qui tournent
    p.bone('queue0', (0, 14, 0), 'body')
    m.lueur('queue0', 'tourbillon', (-3, 10, -3), (3, 15, 3), FP, base=3.6)
    p.bone('queue1', (0, 10, 0), 'queue0')
    m.lueur('queue1', 'tourbillon', (-2, 7, -2), (2, 11, 2), FP, base=4.4)
    p.bone('queue2', (0, 7, 0), 'queue1')
    m.lueur('queue2', 'tourbillon', (-1, 4, -1), (1, 8, 1), FP, base=5.2)
    m.lueur('queue0', 'langue_tourbillon', (-5, 7, 0), (5, 15, 1), FL, masque=langue(10, 8, 3, bas=True), **CF)
    m.lueur('queue0', 'langue_tourbillon', (0, 7, -5), (1, 15, 5), FL, masque=langue(10, 8, 4, bas=True), **CR)

    # --- noyau de lave, plaques de roche, epaules
    m.b = 'body'
    m.c('noyau', (-5, 14, -4), (5, 22, 4), LA, base=1.6, large=.09)
    m.lueur('body', 'coeur', (-2, 16, -5), (2, 20, -4), FP, base=5, hide={'south'})
    m.c('plaque', (-6, 18, -3), (-4, 22, 3), RO, base=1.8)
    m.c('plaque', (3, 14, -5), (5, 17, -4), RO, base=1.6, hide={'south'})
    m.c('plaque', (-4, 19, -5), (-2, 22, -4), RO, base=1.8, hide={'south'})
    m.c('plaque_dos', (-4, 16, 4), (4, 22, 5), RO, base=1.5, hide={'north'})
    m.s('epaule', (5, 18, -3), (9, 23, 3), LA, base=1.4, large=.05)
    m.s('pointe_epaule', (6, 23, -1), (8, 26, 1), RO, base=2.2)

    # --- masque de basalte, yeux blancs, bouche en feu ; couronne de flammes
    m.b = 'head'
    n = m.c('masque', (-4, 22, -5), (4, 29, 3), RO, base=2)
    p.decal(n, 'north', stamp(['........', '........', '........', '........', '.k....k.', '.kkkkkk.', '..kkkk..'], P_, 4, 29))
    m.c('front', (-4, 27, -6), (4, 28, -5), RO, base=2.8, hide={'south'})
    m.yeux('head', 1, 25, -6, 2, 2, ramp='flamme', base=6, dz=1)
    m.lueur('head', 'gueule', (-3, 22, -6), (3, 24, -5), FP, base=5.4, hide={'south'})
    m.s('joue', (4, 22, -4), (5, 26, 1), RO, base=1.6)
    for i, (x, z, w, h, g) in enumerate(((0, -1, 11, 9, 11), (0, 3, 9, 7, 12))):
        p.bone(f'flamme_{i}', (x, 29, z), 'head')
        m.lueur(f'flamme_{i}', 'couronne', (x - w // 2, 29, z), (x + w // 2, 29 + h, z + 1), FL, masque=langue(w, h, g), **CF)
    p.bone('flamme_2', (0, 29, 0), 'head')
    m.lueur('flamme_2', 'couronne', (0, 29, -5), (1, 37, 5), FL, masque=langue(10, 8, 13), **CR)

    # --- poings de pierre qui flottent, relies au corps par une langue de feu
    p.bone('arm_-1', (11, 18, -2), 'body')
    p.bone('arm_1', (-11, 18, -2), 'body')
    m.sb('arm_-1|arm_1', 'poing', (9, 11, -5), (14, 17, 1), LA, base=1.6, large=.06)
    m.sb('arm_-1|arm_1', 'jointure', (9, 11, -6), (14, 14, -5), RO, base=2.8, hide={'south'})
    m.sb('arm_-1|arm_1', 'pic_poing', (10, 17, -3), (13, 19, -1), RO, base=2.2)
    n = m.lueur('arm_-1', 'bras_feu', (10, 17, -2), (13, 24, -1), FL, masque=langue(3, 7, 15), **CF)
    n = m.lueur('arm_1', 'bras_feu', (-13, 17, -2), (-10, 24, -1), FL, masque=langue(3, 7, 16), **CF)

    # ------------------------------------------------------------------ repos : les flammes lechent et se tordent vite, le corps respire comme une forge, les poings dansent
    T = 1.6
    flam = lambda k, ph: {'rotation': keys(T, lambda q: (sin(5, k, ph)(q), 0, sin(8, k + 1, ph + .2)(q)), 16), 'scale': keys(T, lambda q: (1 + sin(.08, k, ph)(q), 1 + sin(.2, k + 1, ph + .1)(q), 1), 16)}
    cycle(p, 'idle', T, {
        'root': P(T, y=sin(.8, 1, 0, bias=.4)),
        'body': R(T, x=sin(1.5, 1, .15), z=sin(1.5, 2, .3)),
        'head': R(T, x=sin(1.5, 1, .3), y=sin(4, 1, 0)),
        'flamme_0': flam(2, 0), 'flamme_1': flam(2, .35), 'flamme_2': flam(3, .6),
        'queue0': {'rotation': keys(T, lambda q: (0, 360 * q, 0), 16)},
        'queue1': {'rotation': keys(T, lambda q: (sin(6, 1, 0)(q), 360 * q, sin(6, 1, .25)(q)), 16)},
        'queue2': {'rotation': keys(T, lambda q: (sin(10, 1, .2)(q), 360 * q, sin(10, 1, .45)(q)), 16)},
        'arm_-1': {'position': keys(T, lambda q: (0, sin(1.4, 1, .1)(q), sin(.8, 1, .35)(q)), 16), 'rotation': keys(T, lambda q: (sin(6, 1, .2)(q), 0, sin(5, 1, .4)(q)), 16)},
        'arm_1': {'position': keys(T, lambda q: (0, sin(1.4, 1, .6)(q), sin(.8, 1, .85)(q)), 16), 'rotation': keys(T, lambda q: (sin(6, 1, .7)(q), 0, sin(5, 1, .9)(q)), 16)},
    })

    # ------------------------------------------------------------------ deplacement : derive sans pieds, buste couche, tourbillon et flammes tires vers l'arriere, poings en avant
    T = 1.2
    fv = lambda ph: {'rotation': keys(T, lambda q: (sin(5, 2, ph, bias=26)(q), 0, sin(7, 3, ph)(q)), 16), 'scale': keys(T, lambda q: (1, 1 + sin(.14, 3, ph)(q), 1), 16)}
    cycle(p, 'walk', T, {
        'root': P(T, y=sin(1, 1, 0, bias=1)),
        'body': R(T, x=sin(2, 1, .1, bias=-20), y=sin(5, 1, 0), z=sin(3, 1, .25)),
        'head': R(T, x=sin(2, 1, .2, bias=14), y=sin(-4, 1, 0)),
        'flamme_0': fv(0), 'flamme_1': fv(.33), 'flamme_2': fv(.66),
        'queue0': {'rotation': keys(T, lambda q: (-22, 360 * q, 0), 16)},
        'queue1': {'rotation': keys(T, lambda q: (sin(6, 1, 0, bias=-12)(q), 360 * q, sin(6, 1, .25)(q)), 16)},
        'queue2': {'rotation': keys(T, lambda q: (sin(9, 1, .2, bias=-12)(q), 360 * q, sin(9, 1, .45)(q)), 16)},
        'arm_-1': {'position': keys(T, lambda q: (0, sin(1.2, 1, .1)(q) + 2, sin(1.6, 1, .35)(q) - 3), 16), 'rotation': keys(T, lambda q: (sin(8, 1, .2, bias=-12)(q), 0, 0), 16)},
        'arm_1': {'position': keys(T, lambda q: (0, sin(1.2, 1, .6)(q) + 2, sin(1.6, 1, .85)(q) - 3), 16), 'rotation': keys(T, lambda q: (sin(8, 1, .7, bias=-12)(q), 0, 0), 16)},
    })

    # ------------------------------------------------------------------ attaque : le poing droit part comme un boulet, puis le gauche, le corps tourne a chaque coup
    clip(p, 'attack', 1.0, [
        (.15, {'body': (4, -22, 0), 'head': (0, 14, 0), 'arm_-1+': (2, 3, 5), 'arm_-1': (24, 0, 0), 'arm_1+': (-1, -1, -3)}),
        (.32, {'root+': (0, -.6, -2), 'body': (-12, 28, 0), 'head': (4, -18, 0), 'arm_-1+': (-6, 3, -15), 'arm_-1': (-48, 0, 0), 'arm_1+': (0, 1, 3), 'flamme_0': (-20, 0, 0), 'flamme_1': (-20, 0, 0)}),
        (.5, {'root+': (0, -.2, -1), 'body': (2, -22, 0), 'head': (0, 14, 0), 'arm_-1+': (0, 2, 4), 'arm_-1': (10, 0, 0), 'arm_1+': (1, 3, 4), 'arm_1': (24, 0, 0)}),
        (.68, {'root+': (0, -.8, -2.4), 'body': (-14, -30, 0), 'head': (4, 20, 0), 'arm_-1+': (1, 1, 3), 'arm_1+': (6, 3, -15), 'arm_1': (-48, 0, 0), 'flamme_0': (-20, 0, 0), 'flamme_1': (-20, 0, 0)}),
        (.86, {'root+': (0, -.2, -.5), 'body': (-3, -4, 0), 'arm_1+': (1, 1, -2), 'arm_1': (-6, 0, 0)}),
    ])

    # ------------------------------------------------------------------ sort : tout se contracte sur le noyau, puis explose : poings projetes, couronne doublee, masque renverse
    clip(p, 'cast', 1.5, [
        (.17, {'root+': (0, -2, 0), 'body': (-10, 0, 0), 'body*': (.84, .84, .84), 'head': (-14, 0, 0), 'arm_-1+': (-6, 3, -1), 'arm_1+': (6, 3, -1), 'flamme_0*': (.7, .5, 1), 'flamme_1*': (.7, .5, 1), 'flamme_2*': (.7, .5, 1)}),
        (LANCER, {'root+': (0, 3, 0), 'body': (10, 0, 0), 'body*': (1.22, 1.22, 1.22), 'head': (22, 0, 0), 'arm_-1+': (9, 10, 2), 'arm_1+': (-9, 10, 2), 'arm_-1': (0, 0, 40), 'arm_1': (0, 0, -40), 'flamme_0*': (1.2, 1.9, 1), 'flamme_1*': (1.2, 1.7, 1), 'flamme_2*': (1.2, 1.8, 1)}),
        (.7, {'root+': (0, 2.4, 0), 'body': (7, 0, 0), 'body*': (1.12, 1.12, 1.12), 'head': (16, 0, 0), 'arm_-1+': (7, 8, 2), 'arm_1+': (-7, 8, 2), 'arm_-1': (0, 0, 30), 'arm_1': (0, 0, -30), 'flamme_0*': (1.1, 1.5, 1), 'flamme_1*': (1.1, 1.4, 1), 'flamme_2*': (1.1, 1.4, 1)}),
        (1.2, {'root+': (0, .3, 0), 'body*': (1, 1, 1), 'flamme_0*': (1, 1.05, 1), 'flamme_1*': (1, 1.05, 1), 'flamme_2*': (1, 1.05, 1)}),
    ])

    # ------------------------------------------------------------------ mort : les flammes s'etouffent, le feu s'eteint de haut en bas, et les pierres tombent au sol
    clip(p, 'death', 1.0, [
        (.25, {'root+': (0, 2, 0), 'body': (8, 0, 0), 'head': (18, 0, 0), 'flamme_0*': (.8, .5, 1), 'flamme_1*': (.8, .5, 1), 'flamme_2*': (.8, .5, 1), 'queue0*': (.8, .8, .8)}),
        (.6, {'root+': (0, -3, 0), 'body': (-8, 0, 6), 'head': (-20, 0, 10), 'flamme_0*': (.3, .15, 1), 'flamme_1*': (.3, .15, 1), 'flamme_2*': (.3, .15, 1), 'queue0*': (.4, .4, .4), 'arm_-1+': (1, -6, 0), 'arm_-1': (30, 0, 20), 'arm_1+': (-1, -7, 0), 'arm_1': (30, 0, -20)}),
        (1.0, {'root+': (0, -8, 0), 'body': (-20, 0, 12), 'head': (-44, 0, 22), 'flamme_0*': (.05, .05, 1), 'flamme_1*': (.05, .05, 1), 'flamme_2*': (.05, .05, 1), 'queue0*': (.05, .05, .05), 'arm_-1+': (3, -8, 1), 'arm_-1': (60, 0, 50), 'arm_1+': (-3, -8, 1), 'arm_1': (60, 0, -50)}),
    ], retour=False)
    return p
