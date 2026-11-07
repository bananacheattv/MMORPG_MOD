#!/usr/bin/env python3
"""Gobelin : petit charognard voute. Grosse tete en avant du buste, arcade lourde, nez crochu, crocs d'en bas, oreilles entaillees, longs bras a griffes, surin rouille, gilet de peau, epauliere de recup, gri-gri d'os.
Echelle du jeu 0,82 : dessine en texels de jeu, 25 de haut. Il trottine plie en deux, surine en se fendant, et lance sa poudre de la main gauche."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, keys, pulse, sin
from kit_mob import CF, CR, FR, M
from materiaux import stamp
from poses import LANCER, P, R, appui, clip, cycle, jambe

G = ('peau', 'gobelin')
CU = ('cuir', 'cuir')
TB = ('peau_tissu', 'tissu_brun')
IV = ('griffe', 'ivoire')
RO = ('metal', 'rouille')
P_ = {'k': '#0d0a08', 'y': '#ffd84a', 'o': '#e89a22', 'd': '#2c4215', 'D': '#1b2a0d', 'l': '#9dbf58', 'm': '#79a03c', 'c': '#b8672d', 'n': '#2a120b', 'w': '#e9e1c4', 'r': '#8a2a1e'}


def build():
    m = M('gobelin', 271, .82, kind='classic', description='Gobelin Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=8, epaule=14, cou=15, x_bras=6, x_jambe=3, coude=9, genou=4, z_bras=-1, z_cou=-2)

    # --- jambes courtes et arquees, grands pieds nus
    m.sb('leg_-1|leg_1', 'cuisse', (1, 4, -2), (5, 9, 2), TB, base=2.3, rot=(8, 0, 0), pivot=(3, 8, 0))
    m.sb('tibia_-1|tibia_1', 'mollet', (2, 2, -2), (5, 5, 1), G, base=2.8, rot=(-8, 0, 0), pivot=(3, 4, 0))
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -5), (5, 2, 2), G, base=3.3)
    m.sb('tibia_-1|tibia_1', 'griffe_a', (1, 0, -6), (2, 1, -5), IV, base=3)
    m.sb('tibia_-1|tibia_1', 'griffe_b', (3, 0, -6), (4, 1, -5), IV, base=3)

    # --- bassin, pagne court, ceinture, gri-gri, bourse
    m.b = 'body'
    m.c('bassin', (-4, 7, -3), (4, 10, 3), TB, base=2.2)
    m.c('ceinture', (-5, 9, -4), (5, 10, 4), CU, ton=-1.4, couture=False)
    m.c('pagne', (-3, 4, -4), (3, 9, -3), FR('tissu_brun'), base=3.2, mini=.6, **CF)
    m.c('pagne_dos', (-3, 4, 3), (3, 9, 4), FR('tissu_brun'), base=2.8, mini=.6, **CF)
    n = m.c('gri_gri', (-1, 7, -5), (1, 10, -4), ('os', 'os_vieux'), base=3.8)
    p.decal(n, 'north', stamp(['kk'], P_, 1, 9))
    m.c('bourse', (5, 6, -1), (7, 9, 2), CU, ton=.4, couture=False)

    # --- buste voute : le gilet de cuir ouvre sur la peau
    p.bone('buste', (0, 9, 0), 'body', rot=(-20, 0, 0))
    m.b = 'buste'
    m.c('taille', (-3, 9, -2), (3, 11, 2), G, base=3.3)
    n = m.c('gilet', (-4, 11, -3), (4, 16, 2), CU, ton=.2, couture=False)
    p.decal(n, 'north', stamp(['..mmmm..', '..lmmm..', '...lm...', '...mm...', '...m....'], P_, 4, 16))
    m.c('bosse', (-3, 13, 2), (3, 17, 4), ('fourrure', 'fourrure_brune'), base=2)
    m.c('col_fourrure', (-4, 16, -2), (4, 17, 3), ('fourrure', 'fourrure_brune'), base=1.8)

    # --- tete : large, basse, loin devant le buste. La bouche ouverte sur les dents est le premier signe qu'on lit
    m.b = 'head'
    n = m.c('crane', (-5, 18, -8), (5, 24, 0), G, base=3.5)
    p.decal(n, 'north', stamp(['..........', '..........', '..........', '..........', 'dd......dd', 'dkwkkkkwkd'], P_, 5, 24))
    m.c('crane_haut', (-4, 24, -7), (4, 25, -1), G, base=4)
    m.c('nuque', (-2, 16, -3), (2, 19, 0), G, base=2.6)
    m.c('arcade', (-5, 22, -9), (5, 23, -8), G, base=2.2, hide={'south'})
    m.yeux('head', 2, 20, -9, 2, 2, ramp='ambre', base=4.2)
    p.decal('oeil_lueur_g', 'north', stamp(['yy', 'yk'], P_, 4, 22))
    m.c('nez', (-1, 19, -10), (1, 22, -8), G, base=3)
    m.c('bout_nez', (-1, 19, -11), (1, 20, -10), G, base=2.4)
    m.s('joue', (5, 18, -7), (6, 21, -3), G, base=3)
    m.c('verrue', (-3, 25, -5), (-2, 26, -4), G, base=4.6)
    m.c('verrue', (2, 25, -3), (3, 26, -2), G, base=4.6)

    m.c('gorge', (-3, 16, -7), (3, 19, -2), ('uni', 'soie_noire'), base=.4)
    p.bone('machoire', (0, 18, -2), 'head')
    n = m.cb('machoire', 'mandibule', (-4, 15, -8), (4, 18, -1), G, base=2.9)
    p.decal(n, 'north', stamp(['kkkkkkkk'], P_, 4, 18))
    m.sb('machoire', 'croc', (3, 17, -9), (4, 19, -8), IV, base=4.2)

    p.bone('oreille_g', (5, 21, -4), 'head')
    p.bone('oreille_d', (-5, 21, -4), 'head')
    O = ['.....#####', '..########', '##########', '....######', '.......###']
    n = m.sb('oreille_g|oreille_d', 'oreille', (5, 19, -5), (15, 24, -4), ('plan', 'gobelin'), base=3.9, masque=O, rot=(0, -22, 7), pivot=(5, 21, -4), **CF)
    m.cb('oreille_g', 'anneau', (9, 18, -5), (10, 20, -4), ('metal', 'or'), base=3.6, rot=(0, -22, 7), pivot=(5, 21, -4))

    # --- bras maigres ecartes du buste, gros avant-bras, mains griffues ; epauliere de ferraille a gauche
    m.sb('arm_-1|arm_1', 'bras', (5, 9, -2), (7, 15, 0), G, base=2.8)
    m.sb('avbras_-1|avbras_1', 'avantbras', (5, 5, -3), (8, 10, 0), G, base=3.2)
    m.sb('avbras_-1|avbras_1', 'main', (5, 3, -4), (8, 5, 0), G, base=3.7)
    m.sb('avbras_-1|avbras_1', 'griffe_main', (5, 2, -4), (6, 3, -3), IV, base=3.4)
    m.sb('avbras_-1|avbras_1', 'griffe_main_b', (7, 2, -4), (8, 3, -3), IV, base=3.4)
    m.cb('arm_1', 'epauliere', (-8, 14, -3), (-4, 17, 1), ('plaque', 'fer'), base=3.4, rivets=2)
    m.cb('arm_1', 'pique', (-7, 17, -2), (-6, 19, -1), ('metal', 'acier'), base=4)

    # --- surin ebreche, tenu lame en avant
    p.bone('dague', (6.5, 3.5, -2), 'avbras_-1', rot=(-72, 0, 0))
    m.cb('dague', 'manche', (6, 1, -3), (7, 5, -2), CU, ton=-.8, couture=False)
    m.cb('dague', 'garde', (5, 5, -3), (8, 6, -2), ('metal', 'fer'), base=2.4)
    L = ['.#.', '###', '##.', '###', '###', '##.', '###', '###']
    m.cb('dague', 'lame', (6, 6, -4), (7, 14, -1), ('plan', 'acier'), base=4.4, masque=L, **CR)

    # ------------------------------------------------------------------ repos : accroupi, la tete vire par a-coups, le surin tourne dans la main, l'oreille tressaille
    T = 2.8
    cycle(p, 'idle', T, {
        'body': {'position': keys(T, lambda q: (0, -.4 + sin(.35, 2, .1)(q), 0), 24), 'rotation': keys(T, lambda q: (sin(1.2, 2, .1)(q), 0, 0), 12)},
        'head': {'rotation': [(0, -3, 0, 0), (.45, -3, 0, 0), (.6, -2, 24, 0), (1.15, -1, 24, 0), (1.3, -3, 0, 0), (1.6, -3, 0, 0), (1.75, -2, -22, 2), (2.3, -1, -22, 2), (2.45, -3, 0, 0), (T, -3, 0, 0)]},
        'machoire': R(T, x=sin(2.5, 2, .1, bias=-2.5)),
        'oreille_g': R(T, y=pulse(-16, .3, .04), z=pulse(10, .3, .04), n=28),
        'oreille_d': R(T, y=pulse(16, .78, .04), z=pulse(-10, .78, .04), n=28),
        'arm_-1': R(T, x=sin(3, 1, .1, bias=12), z=sin(1.5, 1, .3, bias=3)),
        'avbras_-1': R(T, x=sin(5, 2, .2, bias=22)),
        'dague': R(T, x=sin(9, 2, 0), z=sin(5, 2, .25)),
        'arm_1': R(T, x=sin(-3, 1, .1, bias=6), z=sin(-1.5, 1, .3, bias=-3)),
        'avbras_1': R(T, x=sin(-3, 1, .2, bias=14)),
        'leg_-1': R(T, x=sin(1, 2, .1, bias=6)),
        'leg_1': R(T, x=sin(1, 2, .1, bias=6)),
        'tibia_-1': R(T, x=sin(-1.5, 2, .1, bias=-10)),
        'tibia_1': R(T, x=sin(-1.5, 2, .1, bias=-10)),
    })

    # ------------------------------------------------------------------ deplacement : trot presse, plie en deux, bras qui balancent bas et le surin devant
    T = .62
    amp = 36
    u0, l0 = jambe(T, amp, 0, 50)
    u1, l1 = jambe(T, amp, .5, 50)
    cycle(p, 'walk', T, {
        'root': P(T, y=combine(appui(T, amp, 8), cos(.5, 2, .05))),
        'body': R(T, x=sin(1.5, 2, .1, bias=-8), y=sin(10, 1, 0), z=sin(3.5, 1, .25)),
        'head': R(T, x=cos(3, 2, .1, bias=5), y=sin(-11, 1, 0), z=sin(-3, 1, .25)),
        'machoire': R(T, x=sin(3, 2, .3, bias=-3)),
        'oreille_g': R(T, y=sin(10, 2, .3, bias=-8), z=sin(6, 2, .4)),
        'oreille_d': R(T, y=sin(-10, 2, .3, bias=8), z=sin(-6, 2, .4)),
        'leg_-1': u0, 'tibia_-1': l0, 'leg_1': u1, 'tibia_1': l1,
        'arm_-1': R(T, x=sin(-16, 1, 0, bias=34)),
        'avbras_-1': R(T, x=sin(8, 1, .2, bias=30)),
        'dague': R(T, x=sin(5, 2, .1, bias=-35)),
        'arm_1': R(T, x=sin(30, 1, 0, bias=8), z=sin(-3, 1, .25, bias=-6)),
        'avbras_1': R(T, x=sin(10, 1, .35, bias=22)),
    })

    # ------------------------------------------------------------------ attaque : se fend et surine bas, de bas en haut, en montrant les crocs. Le clip dure tout le geste du jeu (swing).
    clip(p, 'attack', 1.0, [
        (.2, {'root+': (0, -.8, 1.2), 'body': (4, -26, -3), 'head': (-5, 20, 0), 'arm_-1': (-42, 0, 10), 'avbras_-1': (82, 0, 0), 'dague': (-14, 0, 0), 'arm_1': (32, 0, -16), 'avbras_1': (34, 0, 0),
              'leg_-1': (-12, 0, 0), 'tibia_-1': (-14, 0, 0), 'leg_1': (22, 0, 0), 'tibia_1': (-36, 0, 0), 'machoire': (-6, 0, 0), 'oreille_g': (0, -22, 0), 'oreille_d': (0, 22, 0)}),
        (.4, {'root+': (0, -1.3, -3.4), 'body': (-16, 28, 3), 'head': (10, -22, 0), 'arm_-1': (86, 0, -4), 'avbras_-1': (12, 0, 0), 'dague': (-112, 0, 0), 'arm_1': (-36, 0, -22), 'avbras_1': (10, 0, 0),
              'leg_-1': (38, 0, 0), 'tibia_-1': (-34, 0, 0), 'leg_1': (-26, 0, 0), 'tibia_1': (-14, 0, 0), 'machoire': (-26, 0, 0), 'oreille_g': (0, -30, -6), 'oreille_d': (0, 30, 6)}),
        (.55, {'root+': (0, -1.1, -3), 'body': (-12, 22, 2), 'head': (8, -16, 0), 'arm_-1': (78, 0, -2), 'avbras_-1': (18, 0, 0), 'dague': (-104, 0, 0), 'arm_1': (-26, 0, -18), 'leg_-1': (32, 0, 0), 'tibia_-1': (-30, 0, 0), 'leg_1': (-20, 0, 0), 'machoire': (-22, 0, 0)}),
        (.8, {'root+': (0, -.3, -.6), 'body': (-3, 5, 0), 'arm_-1': (22, 0, 0), 'avbras_-1': (16, 0, 0), 'dague': (-30, 0, 0), 'arm_1': (-6, 0, -4), 'leg_-1': (8, 0, 0), 'tibia_-1': (-8, 0, 0), 'leg_1': (-5, 0, 0), 'machoire': (-5, 0, 0)}),
    ])

    # ------------------------------------------------------------------ sort : plonge la main gauche dans sa poudre, la jette vers le haut en ricanant, puis saute d'un pied sur l'autre
    clip(p, 'cast', 1.3, [
        (.16, {'root+': (0, -1, .8), 'body': (6, 24, 4), 'head': (-4, -16, 0), 'arm_1': (-52, 0, -30), 'avbras_1': (84, 0, 0), 'arm_-1': (10, 0, 12), 'avbras_-1': (26, 0, 0), 'oreille_g': (0, -18, -8), 'oreille_d': (0, 18, 8)}),
        (LANCER, {'root+': (0, .8, -1), 'body': (-14, -22, -3), 'head': (14, 14, 0), 'arm_1': (150, 0, -10), 'avbras_1': (4, 0, 0), 'arm_-1': (-18, 0, 16), 'avbras_-1': (30, 0, 0), 'machoire': (-30, 0, 0), 'oreille_g': (0, 12, 14), 'oreille_d': (0, -12, -14)}),
        (.46, {'root+': (0, .3, -.6), 'body': (-8, -14, -2), 'head': (12, 8, 0), 'arm_1': (128, 0, -8), 'avbras_1': (10, 0, 0), 'machoire': (-26, 0, 0)}),
        (.68, {'root+': (0, 1, 0), 'body': (-4, 0, 5), 'head': (8, 0, -5), 'arm_1': (60, 0, -12), 'avbras_1': (34, 0, 0), 'leg_-1': (24, 0, 0), 'tibia_-1': (-44, 0, 0), 'machoire': (-16, 0, 0)}),
        (.9, {'root+': (0, 1, 0), 'body': (-4, 0, -5), 'head': (8, 0, 5), 'arm_1': (24, 0, -8), 'leg_1': (24, 0, 0), 'tibia_1': (-44, 0, 0), 'machoire': (-20, 0, 0)}),
        (1.1, {'root+': (0, 0, 0), 'body': (-1, 0, 0), 'head': (2, 0, 0), 'machoire': (-4, 0, 0)}),
    ])

    # ------------------------------------------------------------------ mort : recule sous le coup, le surin s'echappe, les genoux lachent, les oreilles retombent
    clip(p, 'death', 1.0, [
        (.22, {'body': (16, 0, 7), 'head': (20, 0, -8), 'arm_-1': (24, 0, 42), 'avbras_-1': (30, 0, 0), 'dague': (70, 0, 0), 'arm_1': (46, 0, -48), 'avbras_1': (20, 0, 0), 'machoire': (-28, 0, 0), 'oreille_g': (0, 10, 14), 'oreille_d': (0, -10, -14)}),
        (.6, {'root+': (0, -3, 0), 'body': (-20, 0, -6), 'head': (-26, 0, 10), 'arm_-1': (14, 0, 14), 'avbras_-1': (12, 0, 0), 'dague': (120, 0, 0), 'arm_1': (18, 0, -12), 'avbras_1': (8, 0, 0), 'leg_-1': (52, 0, 0), 'tibia_-1': (-72, 0, 0), 'leg_1': (44, 0, 0), 'tibia_1': (-82, 0, 0), 'machoire': (-20, 0, 0), 'oreille_g': (0, -6, -22), 'oreille_d': (0, 6, 22)}),
        (1.0, {'root+': (0, -4, 0), 'body': (-28, 0, -8), 'head': (-34, 0, 12), 'arm_-1': (8, 0, 6), 'dague': (130, 0, 0), 'arm_1': (10, 0, -6), 'leg_-1': (60, 0, 0), 'tibia_-1': (-86, 0, 0), 'leg_1': (50, 0, 0), 'tibia_1': (-92, 0, 0), 'machoire': (-14, 0, 0), 'oreille_g': (0, -8, -30), 'oreille_d': (0, 8, 30)}),
    ], retour=False)
    return p
