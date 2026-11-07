#!/usr/bin/env python3
"""Loup sombre : poitrail profond et flanc rentre, criniere brune en collier, crete sur l'echine, long museau, crocs, oeil bleu froid, pattes digitigrades, queue touffue.
Trot net par diagonales, langue pendante au repos, bondit gueule ouverte et referme en l'air, hurle pour son sort."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, foot_drop, keys, pulse, sin
from kit_mob import CF, CR, FR, M
from materiaux import stamp
from poses import LANCER, P, R, clip, cycle, jambe

FU = ('fourrure', 'nuit')
CRI = ('poils_longs', 'fourrure_brune')
NU = ('uni', 'nuit')
IV = ('griffe', 'ivoire')
P_ = {'k': '#07060c', 'g': '#4b4566', 'l': '#6b6585', 'r': '#7a1712', 'R': '#b0281e'}
JAMBES = ('leg_-10', 'leg_10', 'leg_-11', 'leg_11')


def build():
    m = M('loup_sombre', 280, 1.0, kind='classic', description='Loup sombre Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (4, 20)
    p.bone('body', (0, 13, 0), 'root')
    m.b = 'body'
    # --- corps : poitrail haut, taille rentree, croupe ; criniere en collier
    m.c('poitrail', (-4, 7, -10), (4, 17, -2), FU, ton=.4)
    m.c('taille', (-3, 9, -2), (3, 16, 6), FU, ton=.1)
    m.c('croupe', (-4, 8, 6), (4, 16, 14), FU, ton=.3)
    m.c('ventre_clair', (-2, 8, -3), (2, 9, 7), ('uni', 'museau_gris'), base=2.4)
    m.c('criniere', (-5, 7, -12), (5, 19, -6), CRI, ton=-.2, y=(4, 20))
    m.c('crete', (-1, 17, -6), (1, 19, 3), CRI, ton=-.5, y=(4, 20))
    m.c('crete_b', (-1, 16, 3), (1, 17, 12), CRI, ton=-.8, y=(4, 20))

    # --- tete portee bas, museau long
    p.bone('head', (0, 15, -11), 'body')
    m.b = 'head'
    n = m.c('crane', (-4, 14, -19), (4, 20, -11), FU, ton=.9)
    m.c('joue', (-5, 14, -17), (5, 17, -13), CRI, ton=-.2, y=(4, 20))
    m.c('front', (-3, 20, -18), (3, 21, -13), FU, ton=1.2)
    n = m.c('museau', (-2, 14, -25), (2, 18, -19), ('uni', 'nuit'), base=3.8)
    p.decal(n, 'up', stamp(['....', 'k..k', '....', '....', '....', '....'], P_, -2, -25))
    m.c('truffe', (-1, 16, -26), (1, 18, -25), ('uni', 'nuit'), base=.6, hide={'south'})
    m.yeux('head', 2, 17, -20, 2, 1, ramp='ame', base=4.4, dz=1)
    m.c('gorge', (-1, 12, -20), (1, 15, -14), ('uni', 'venin'), base=1)
    p.bone('machoire', (0, 14, -14), 'head')
    m.cb('machoire', 'mandibule', (-2, 12, -24), (2, 14, -13), ('uni', 'nuit'), base=2.8)
    m.sb('machoire', 'croc', (1, 14, -24), (2, 15, -23), IV, base=4)
    p.bone('langue', (0, 13, -18), 'machoire')
    m.cb('langue', 'langue_cube', (-1, 13, -23), (1, 14, -17), ('uni', 'venin'), base=3.2)
    p.bone('ear_g', (3, 20, -13), 'head')
    p.bone('ear_d', (-3, 20, -13), 'head')
    OR_ = ['..##', '..##', '.###', '.###', '####', '####', '####']
    m.sb('ear_g|ear_d', 'oreille', (1, 19, -14), (5, 26, -13), ('plan', 'nuit'), base=4, masque=OR_, rot=(-8, 0, -8), pivot=(3, 20, -13), **CF)

    # --- queue touffue en trois morceaux
    p.bone('tail', (0, 15, 14), 'body')
    m.cb('tail', 'queue', (-2, 11, 13), (2, 16, 21), CRI, ton=-.5, y=(4, 20), rot=(16, 0, 0), pivot=(0, 15, 14))
    p.bone('queue_b', (0, 13, 20), 'tail')
    m.cb('queue_b', 'queue_milieu', (-3, 8, 19), (3, 14, 26), CRI, ton=-.2, y=(4, 20), rot=(30, 0, 0), pivot=(0, 13, 20))
    p.bone('queue_c', (0, 10, 25), 'queue_b')
    m.cb('queue_c', 'queue_bout', (-2, 4, 24), (2, 10, 29), FU, ton=.9, rot=(44, 0, 0), pivot=(0, 10, 25))

    # --- pattes : epaule et canon, cuisse et jarret en angle, griffes
    for nom, x, z in (('leg_-10', 3, -7), ('leg_10', -3, -7), ('leg_-11', 3, 10), ('leg_11', -3, 10)):
        p.bone(nom, (x, 13, z), 'root')
        p.bone(nom + '_bas', (x, 7, z), nom)
    m.sb('leg_-10|leg_10', 'epaule', (2, 6, -10), (5, 15, -4), FU, y=(0, 20), hide={'up'})
    m.sb('leg_-10_bas|leg_10_bas', 'canon_av', (2, 2, -9), (4, 7, -6), FU, y=(0, 20), ton=-.2)
    m.sb('leg_-10_bas|leg_10_bas', 'patte_av', (2, 0, -11), (5, 2, -6), ('uni', 'nuit'), base=3)
    m.sb('leg_-10_bas|leg_10_bas', 'griffe_av', (2, 0, -12), (3, 1, -11), IV, base=3)
    m.sb('leg_-10_bas|leg_10_bas', 'griffe_av_b', (4, 0, -12), (5, 1, -11), IV, base=3)
    m.sb('leg_-11|leg_11', 'cuisse', (2, 6, 7), (5, 15, 14), FU, y=(0, 20), hide={'up'})
    m.sb('leg_-11_bas|leg_11_bas', 'jarret', (2, 3, 11), (4, 8, 14), FU, y=(0, 20), ton=-.1, rot=(-16, 0, 0), pivot=(3, 7, 12))
    m.sb('leg_-11_bas|leg_11_bas', 'canon_ar', (2, 2, 11), (4, 4, 13), FU, y=(0, 20), ton=-.4)
    m.sb('leg_-11_bas|leg_11_bas', 'patte_ar', (2, 0, 8), (5, 2, 13), ('uni', 'nuit'), base=3)
    m.sb('leg_-11_bas|leg_11_bas', 'griffe_ar', (2, 0, 7), (3, 1, 8), IV, base=3)
    m.sb('leg_-11_bas|leg_11_bas', 'griffe_ar_b', (4, 0, 7), (5, 1, 8), IV, base=3)

    # ------------------------------------------------------------------ repos : halete langue dehors, tete qui scrute, oreille qui pivote, queue qui balaie lentement
    T = 2.8
    cycle(p, 'idle', T, {
        'body': {'position': keys(T, lambda q: (0, sin(.25, 6, 0)(q), 0), 24), 'rotation': keys(T, lambda q: (sin(.6, 6, 0)(q), 0, 0), 24)},
        'head': {'rotation': [(0, 4, 0, 0), (.6, 4, 0, 0), (.8, 6, 22, 0), (1.4, 6, 22, 0), (1.65, 5, -18, 0), (2.3, 5, -18, 0), (2.55, 4, 0, 0), (T, 4, 0, 0)]},
        'machoire': R(T, x=sin(5, 8, 0, bias=-12), n=32),
        'langue': R(T, x=sin(6, 8, .1, bias=-16), n=32),
        'ear_g': R(T, x=pulse(-14, .3, .04), y=pulse(-20, .3, .04), n=28),
        'ear_d': R(T, x=pulse(-14, .72, .04), y=pulse(20, .72, .04), n=28),
        'tail': R(T, x=sin(1.5, 1, .1), y=sin(9, 1, 0)),
        'queue_b': R(T, y=sin(9, 1, .1)),
        'queue_c': R(T, y=sin(11, 1, .2)),
    })

    # ------------------------------------------------------------------ deplacement : trot en diagonales, echine qui ondule, tete basse et stable, queue qui suit
    T = .6
    amp = 34
    tr = {
        'body': {'position': keys(T, lambda q: (0, cos(.45, 2, .05)(q), 0), 16), 'rotation': keys(T, lambda q: (sin(1.2, 2, .1)(q), sin(2.5, 1, 0)(q), sin(2, 1, .25)(q)), 16)},
        'head': R(T, x=cos(-2.5, 2, .1, bias=-5), y=sin(-2.5, 1, 0), z=sin(-2, 1, .25)),
        'machoire': R(T, x=sin(3, 2, .3, bias=-7)),
        'langue': R(T, x=sin(6, 2, .4, bias=-12)),
        'ear_g': R(T, x=sin(5, 2, .3, bias=-12)),
        'ear_d': R(T, x=sin(5, 2, .3, bias=-12)),
        'tail': R(T, x=sin(3, 2, .15, bias=-18), y=sin(8, 1, .2)),
        'queue_b': R(T, x=sin(4, 2, .25, bias=-10), y=sin(10, 1, .3)),
        'queue_c': R(T, x=sin(5, 2, .35, bias=-8), y=sin(12, 1, .4)),
    }
    for nom, ph, a, k in (('leg_-10', 0, amp, 56), ('leg_10', .5, amp, 56), ('leg_-11', .5, amp * .92, 44), ('leg_11', 0, amp * .92, 44)):
        u, l = jambe(T, a, ph, k, reach=.3)
        u['position'] = foot_drop(T, a, ph, 13, n=16)
        tr[nom], tr[nom + '_bas'] = u, l
    cycle(p, 'walk', T, tr)

    # ------------------------------------------------------------------ attaque : se tasse, oreilles couchees, bondit pattes en avant, gueule ouverte, et claque des machoires en l'air
    clip(p, 'attack', 1.0, [
        (.2, {'root+': (0, -2.4, 2), 'body': (6, 0, 0), 'head': (-10, 0, 0), 'machoire': (-8, 0, 0), 'ear_g': (-30, 0, 0), 'ear_d': (-30, 0, 0), 'leg_-10': (-20, 0, 0), 'leg_10': (-20, 0, 0), 'leg_-10_bas': (-22, 0, 0), 'leg_10_bas': (-22, 0, 0), 'leg_-11': (30, 0, 0), 'leg_11': (30, 0, 0), 'leg_-11_bas': (-46, 0, 0), 'leg_11_bas': (-46, 0, 0), 'tail': (-26, 0, 0)}),
        (.42, {'root+': (0, 4, -9), 'body': (12, 0, 0), 'head': (4, 0, 0), 'machoire': (-44, 0, 0), 'ear_g': (-36, 0, 0), 'ear_d': (-36, 0, 0), 'leg_-10': (62, 0, 0), 'leg_10': (70, 0, 0), 'leg_-10_bas': (-8, 0, 0), 'leg_10_bas': (-8, 0, 0), 'leg_-11': (-44, 0, 0), 'leg_11': (-52, 0, 0), 'leg_-11_bas': (-12, 0, 0), 'leg_11_bas': (-12, 0, 0), 'tail': (-34, 0, 0), 'queue_b': (-16, 0, 0)}),
        (.54, {'root+': (0, 2.6, -10), 'body': (0, 0, 0), 'head': (-16, 0, 0), 'machoire': (0, 0, 0), 'ear_g': (-30, 0, 0), 'ear_d': (-30, 0, 0), 'leg_-10': (40, 0, 0), 'leg_10': (44, 0, 0), 'leg_-10_bas': (-10, 0, 0), 'leg_10_bas': (-10, 0, 0), 'leg_-11': (-24, 0, 0), 'leg_11': (-30, 0, 0), 'leg_-11_bas': (-24, 0, 0), 'leg_11_bas': (-24, 0, 0), 'tail': (-14, 0, 0)}),
        (.8, {'root+': (0, -.6, -2.6), 'body': (-5, 0, 0), 'head': (2, 0, 0), 'machoire': (-14, 0, 0), 'ear_g': (-8, 0, 0), 'ear_d': (-8, 0, 0), 'leg_-10': (10, 0, 0), 'leg_10': (6, 0, 0), 'leg_-10_bas': (-14, 0, 0), 'leg_10_bas': (-14, 0, 0), 'leg_-11': (6, 0, 0), 'leg_11': (6, 0, 0), 'leg_-11_bas': (-14, 0, 0), 'leg_11_bas': (-14, 0, 0)}),
    ])

    # ------------------------------------------------------------------ sort : s'assoit sur l'arriere-train, leve le museau et hurle, en modulant ; les oreilles se couchent
    clip(p, 'cast', 1.8, [
        (.18, {'root+': (0, -1.5, 1), 'body': (14, 0, 0), 'head': (24, 0, 0), 'machoire': (-14, 0, 0), 'ear_g': (-20, 0, 0), 'ear_d': (-20, 0, 0), 'leg_-10': (-14, 0, 0), 'leg_10': (-14, 0, 0), 'leg_-11': (40, 0, 0), 'leg_11': (40, 0, 0), 'leg_-11_bas': (-70, 0, 0), 'leg_11_bas': (-70, 0, 0), 'tail': (10, 0, 0)}),
        (LANCER, {'root+': (0, -2.5, 1.5), 'body': (24, 0, 0), 'head': (54, 0, 0), 'machoire': (-34, 0, 0), 'ear_g': (-46, 0, 0), 'ear_d': (-46, 0, 0), 'leg_-10': (-24, 0, 0), 'leg_10': (-24, 0, 0), 'leg_-11': (54, 0, 0), 'leg_11': (54, 0, 0), 'leg_-11_bas': (-88, 0, 0), 'leg_11_bas': (-88, 0, 0), 'tail': (24, 0, 0)}),
        (.8, {'root+': (0, -2.5, 1.5), 'body': (25, 0, 0), 'head': (62, 3, 0), 'machoire': (-28, 0, 0), 'ear_g': (-46, 0, 0), 'ear_d': (-46, 0, 0), 'leg_-10': (-24, 0, 0), 'leg_10': (-24, 0, 0), 'leg_-11': (54, 0, 0), 'leg_11': (54, 0, 0), 'leg_-11_bas': (-88, 0, 0), 'leg_11_bas': (-88, 0, 0), 'tail': (24, 0, 0)}),
        (1.25, {'root+': (0, -2.3, 1.3), 'body': (23, 0, 0), 'head': (58, -3, 0), 'machoire': (-36, 0, 0), 'ear_g': (-40, 0, 0), 'ear_d': (-40, 0, 0), 'leg_-10': (-22, 0, 0), 'leg_10': (-22, 0, 0), 'leg_-11': (50, 0, 0), 'leg_11': (50, 0, 0), 'leg_-11_bas': (-84, 0, 0), 'leg_11_bas': (-84, 0, 0), 'tail': (20, 0, 0)}),
        (1.55, {'root+': (0, -.5, .3), 'body': (4, 0, 0), 'head': (10, 0, 0), 'machoire': (-6, 0, 0), 'leg_-11': (8, 0, 0), 'leg_11': (8, 0, 0), 'leg_-11_bas': (-14, 0, 0), 'leg_11_bas': (-14, 0, 0)}),
    ])

    # ------------------------------------------------------------------ mort : glapit tete en l'air, l'arriere lache d'abord, puis l'avant ; la langue pend
    clip(p, 'death', 1.0, [
        (.2, {'body': (-8, 0, 0), 'head': (24, 0, 0), 'machoire': (-34, 0, 0), 'ear_g': (-30, 0, 0), 'ear_d': (-30, 0, 0), 'tail': (-20, 0, 0), 'leg_-11': (18, 0, 0), 'leg_11': (18, 0, 0), 'leg_-11_bas': (-30, 0, 0), 'leg_11_bas': (-30, 0, 0)}),
        (.6, {'root+': (0, -4, 0), 'body': (6, 0, 0), 'head': (-14, 0, 0), 'machoire': (-22, 0, 0), 'langue': (-14, 0, 0), 'tail': (22, 0, 0), 'leg_-10': (-18, 0, 0), 'leg_10': (-18, 0, 0), 'leg_-10_bas': (-30, 0, 0), 'leg_10_bas': (-30, 0, 0), 'leg_-11': (46, 0, 0), 'leg_11': (46, 0, 0), 'leg_-11_bas': (-76, 0, 0), 'leg_11_bas': (-76, 0, 0)}),
        (1.0, {'root+': (0, -6.5, 0), 'body': (0, 0, 0), 'head': (-32, 0, 0), 'machoire': (-16, 0, 0), 'langue': (-20, 0, 0), 'ear_g': (10, 0, -20), 'ear_d': (10, 0, 20), 'tail': (30, 0, 0), 'queue_b': (10, 0, 0), 'leg_-10': (-36, 0, 0), 'leg_10': (-36, 0, 0), 'leg_-10_bas': (-54, 0, 0), 'leg_10_bas': (-54, 0, 0), 'leg_-11': (58, 0, 0), 'leg_11': (58, 0, 0), 'leg_-11_bas': (-96, 0, 0), 'leg_11_bas': (-96, 0, 0)}),
    ], retour=False)
    return p
