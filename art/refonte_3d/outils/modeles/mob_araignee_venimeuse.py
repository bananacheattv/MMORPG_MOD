#!/usr/bin/env python3
"""Araignee venimeuse : gros abdomen noir marque d'un sablier rouge, thorax brun, huit yeux rouges, pedipalpes, crochets a goutte verte, hautes pattes coudees et annelees.
Elle court en vagues alternees par a-coups, se cabre pour montrer ses crochets, retourne l'abdomen au-dessus d'elle pour cracher sa toile, et meurt les pattes repliees sous le ventre."""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, keys, pas_patte, pas_patte_levee, pattes_alternees, pulse, sin
from kit_mob import CF, CR, M
from materiaux import stamp
from poses import LANCER, P, R, clip, cycle

AB = ('plaque', 'nuit')
TH = ('segments', 'chitine')
P_ = {'k': '#07060c', 'r': '#d6322a', 'R': '#6a1414', 'y': '#ffb08a', 'g': '#4b4566', 'w': '#b9bfa8'}


def build():
    m = M('araignee_venimeuse', 281, 1.0, kind='classic', description='Araignee venimeuse Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.bone('body', (0, 6, 0), 'root')

    # --- abdomen sur son propre os : il ballotte et se retourne
    p.bone('abdomen', (0, 7, 2), 'body')
    m.cb('abdomen', 'ventre', (-6, 3, 3), (6, 11, 14), AB, base=3)
    m.cb('abdomen', 'dessus', (-5, 11, 4), (5, 13, 13), AB, base=3.6, bord=0)
    n = m.cb('abdomen', 'dessus_haut', (-3, 13, 6), (3, 14, 11), AB, base=4, bord=0)
    p.decal(n, 'up', stamp(['r....r', '.rrrr.', '..rr..', '.rrrr.', 'r....r'], P_, -3, 6))
    n = m.cb('abdomen', 'croupion', (-4, 4, 14), (4, 10, 16), AB, base=2.6, bord=0)
    p.decal(n, 'south', stamp(['........', '...RR...', '..RrrR..', '...RR...', '........', '........'], P_, 4, 10))
    m.cb('abdomen', 'filiere', (-1, 4, 16), (1, 6, 18), ('uni', 'chitine'), base=2.6)
    for i, (x, z) in enumerate(((5, 5), (6, 9), (4, 12))):
        m.sb('abdomen', 'poil', (x, 11, z), (x + 1, 13, z + 1), ('uni', 'fourrure_brune'), base=2.2)

    # --- thorax, couronne de huit yeux, pedipalpes, crochets
    p.bone('head', (0, 6, -2), 'body')
    m.b = 'head'
    n = m.c('thorax', (-4, 3, -7), (4, 9, 3), TH, pas=3, base=2.4)
    m.c('dos_thorax', (-3, 9, -6), (3, 10, 2), TH, pas=3, base=3)
    m.yeux('head', 1, 7, -7, 1, 2, ramp='venin', base=4.6)
    m.yeux('head', 3, 8, -7, 1, 1, ramp='venin', base=4.2, nom='oeil_b_lueur')
    m.yeux('head', 0, 5, -7, 1, 1, ramp='venin', base=3.8, nom='oeil_c_lueur')
    m.yeux('head', 2, 5, -7, 1, 1, ramp='venin', base=3.8, nom='oeil_d_lueur')
    p.bone('croc_g', (2, 4, -7), 'head')
    p.bone('croc_d', (-2, 4, -7), 'head')
    m.sb('croc_g|croc_d', 'chelicere', (1, 2, -9), (3, 5, -7), ('uni', 'chitine'), base=2.2)
    m.sb('croc_g|croc_d', 'crochet', (1, 0, -9), (2, 2, -8), ('griffe', 'ivoire'), base=3)
    m.lueur('croc_g', 'goutte', (1, -1, -9), (2, 0, -8), ('uni', 'venin_vert'), base=4)
    p.bone('palpe_g', (3, 6, -7), 'head')
    p.bone('palpe_d', (-3, 6, -7), 'head')
    m.sb('palpe_g|palpe_d', 'palpe', (2, 2, -11), (4, 3, -7), ('segments', 'nuit'), pas=2, base=3.6)
    m.sb('palpe_g|palpe_d', 'palpe_bout', (2, 1, -12), (4, 3, -11), ('uni', 'chitine'), base=3)

    # --- huit pattes : femur qui monte, tibia qui redescend
    legs = []
    for i, (nom, z, fan) in enumerate((('0', -5, 40), ('1', -2, 14), ('2', 1, -12), ('3', 3, -36))):
        g, d = f'leg_-1{nom}', f'leg_1{nom}'
        p.bone(g, (3, 6, z), 'body')
        p.bone(d, (-3, 6, z), 'body')
        L = 9
        a = math.radians(fan)
        xk = round(3 + L * math.cos(math.radians(32)) * math.cos(a))
        yk = 6 + L * math.sin(math.radians(32))
        zk = round(z - L * math.cos(math.radians(32)) * math.sin(a))
        p.bone(g + '_bas', (xk, yk, zk), g)
        p.bone(d + '_bas', (-xk, yk, zk), d)
        m.sb(f'{g}|{d}', f'cuisse{i}', (3, 5, z), (12, 7, z + 2), ('segments', 'chitine'), pas=3, axe='x', base=2.8, rot=(0, fan, 32), pivot=(3, 6, z))
        h = int(math.ceil(yk))
        m.sb(f'{g}_bas|{d}_bas', f'tibia{i}', (xk, 0, zk), (xk + 2, h + 1, zk + 1), ('segments', 'nuit'), pas=3, axe='y', base=3.4, rot=(0, 0, 16), pivot=(xk, yk, zk))
        m.sb(f'{g}_bas|{d}_bas', f'griffe{i}', (xk, -1, zk), (xk + 1, 1, zk + 1), ('griffe', 'ivoire'), base=2.6, rot=(0, 0, 16), pivot=(xk, yk, zk))
        legs += [(g, g + '_bas', 1), (d, d + '_bas', -1)]

    # ------------------------------------------------------------------ repos : immobile puis un a-coup ; les palpes tatonnent, l'abdomen pulse, une patte de devant tapote
    T = 3.4
    tr = {
        'body': {'position': keys(T, lambda q: (0, sin(.25, 1, .1)(q) - .2, 0), 16)},
        'abdomen': {'rotation': keys(T, lambda q: (sin(1.6, 2, .1)(q), 0, 0), 16), 'scale': keys(T, lambda q: (1 + sin(.03, 2, 0)(q), 1 + sin(.05, 2, 0)(q), 1 + sin(.03, 2, 0)(q)), 16)},
        'head': R(T, y=combine(pulse(14, .25, .05), pulse(-14, .7, .05)), x=sin(1, 1, .3), n=32),
        'palpe_g': R(T, x=combine(sin(6, 3, 0), pulse(-16, .3, .03)), n=32),
        'palpe_d': R(T, x=combine(sin(6, 3, .3), pulse(-16, .55, .03)), n=32),
        'croc_g': R(T, y=sin(3, 2, .1)),
        'croc_d': R(T, y=sin(-3, 2, .1)),
        'leg_-10': R(T, z=combine(pulse(16, .42, .03), pulse(16, .5, .03)), y=combine(pulse(8, .42, .03), pulse(8, .5, .03)), n=40),
    }
    cycle(p, 'idle', T, tr)

    # ------------------------------------------------------------------ deplacement : course nerveuse, tetrapode alterne a grande vitesse, l'abdomen suit en retard
    T = .56
    tr = {
        'body': {'position': keys(T, lambda q: (sin(.25, 1, .25)(q), cos(.25, 2)(q) + .3, 0), 16), 'rotation': keys(T, lambda q: (sin(1, 2, 0)(q), sin(2, 1, 0)(q), sin(2, 1, .25)(q)), 16)},
        'abdomen': R(T, x=sin(3, 2, .2, bias=-3), y=sin(-5, 1, .1), z=sin(-3, 1, .3)),
        'head': R(T, x=sin(1, 2, .1), y=sin(-2, 1, .1)),
        'palpe_g': R(T, x=sin(10, 1, 0)),
        'palpe_d': R(T, x=sin(-10, 1, 0)),
    }
    for up, low, ph, s in pattes_alternees(legs):
        tr[up] = {'rotation': keys(T, lambda q, ph=ph, s=s: pas_patte(q + ph, s, 18, 20), 16)}
        tr[low] = {'rotation': keys(T, lambda q, ph=ph, s=s: (0, 0, -s * 22 * pas_patte_levee(q + ph)), 16)}
    cycle(p, 'walk', T, tr)

    # ------------------------------------------------------------------ attaque : se cabre, pattes de devant ouvertes et crochets ecartes, puis plonge et referme
    def avant(a, b, c, d):
        return {'leg_-10': (0, a, b), 'leg_10': (0, -a, -b), 'leg_-11': (0, c, d), 'leg_11': (0, -c, -d)}
    clip(p, 'attack', 1.0, [
        (.25, {'root+': (0, 2, 1), 'body': (26, 0, 0), 'abdomen': (-22, 0, 0), 'head': (8, 0, 0), 'croc_g': (0, -28, 0), 'croc_d': (0, 28, 0), 'palpe_g': (40, 0, 0), 'palpe_d': (40, 0, 0), **avant(16, 40, 8, 24), 'leg_-13': (0, 0, -12), 'leg_13': (0, 0, 12)}),
        (.45, {'root+': (0, -1.2, -5), 'body': (-16, 0, 0), 'abdomen': (22, 0, 0), 'head': (-14, 0, 0), 'croc_g': (0, 24, 0), 'croc_d': (0, -24, 0), 'palpe_g': (-20, 0, 0), 'palpe_d': (-20, 0, 0), **avant(20, -6, 8, -4), 'leg_-13': (0, 0, 6), 'leg_13': (0, 0, -6)}),
        (.6, {'root+': (0, -1, -4.4), 'body': (-12, 0, 0), 'abdomen': (16, 0, 0), 'head': (-12, 0, 0), 'croc_g': (0, 26, 0), 'croc_d': (0, -26, 0), **avant(18, -4, 6, -2)}),
        (.85, {'root+': (0, -.2, -.8), 'body': (-2, 0, 0), 'abdomen': (3, 0, 0), 'head': (-2, 0, 0), 'croc_g': (0, 4, 0), 'croc_d': (0, -4, 0)}),
    ])

    # ------------------------------------------------------------------ sort : plante ses pattes, leve l'abdomen au-dessus de la tete et crache sa toile par saccades
    clip(p, 'cast', 1.6, [
        (.18, {'root+': (0, 1, 0), 'body': (-8, 0, 0), 'abdomen': (-40, 0, 0), 'head': (6, 0, 0), 'palpe_g': (24, 0, 0), 'palpe_d': (24, 0, 0)}),
        (LANCER, {'root+': (0, 1.6, 0), 'body': (-14, 0, 0), 'abdomen': (-96, 0, 0), 'head': (12, 0, 0), 'palpe_g': (34, 0, 0), 'palpe_d': (34, 0, 0), 'croc_g': (0, -18, 0), 'croc_d': (0, 18, 0)}),
        (.46, {'root+': (0, 1.2, .4), 'body': (-12, 0, 0), 'abdomen': (-84, 0, 0), 'head': (10, 0, 0)}),
        (.62, {'root+': (0, 1.6, 0), 'body': (-14, 0, 0), 'abdomen': (-98, 0, 0), 'head': (12, 0, 0)}),
        (.8, {'root+': (0, 1.2, .4), 'body': (-12, 0, 0), 'abdomen': (-84, 0, 0), 'head': (10, 0, 0)}),
        (1.2, {'root+': (0, .3, 0), 'body': (-3, 0, 0), 'abdomen': (-14, 0, 0), 'head': (2, 0, 0)}),
    ])

    # ------------------------------------------------------------------ mort : tombe a plat ventre et replie toutes ses pattes sous elle, les unes apres les autres
    poses = {}
    for k, (up, low, s) in enumerate(legs):
        poses.setdefault(.3 + .02 * k, {})[up] = (0, 0, -s * 22)
        poses.setdefault(.3 + .02 * k, {})[low] = (0, 0, s * 30)
    fin = {}
    for up, low, s in legs:
        fin[up] = (0, 0, -s * 34)
        fin[low] = (0, 0, s * 70)
    clip(p, 'death', 1.0, [
        (.15, {'root+': (0, 1, 0), 'body': (12, 0, 0), 'abdomen': (-14, 0, 0), 'croc_g': (0, -24, 0), 'croc_d': (0, 24, 0)}),
        *sorted(poses.items()),
        (.55, {'root+': (0, -3.6, 0), 'body': (-4, 0, 0), 'abdomen': (6, 0, 0), 'head': (-8, 0, 0)}),
        (1.0, {'root+': (0, -4, 0), 'body': (-4, 0, 0), 'abdomen': (10, 0, 0), 'head': (-12, 0, 0), 'croc_g': (0, 10, 0), 'croc_d': (0, -10, 0), 'palpe_g': (-24, 0, 0), 'palpe_d': (-24, 0, 0), **fin}),
    ], retour=False)
    return p
