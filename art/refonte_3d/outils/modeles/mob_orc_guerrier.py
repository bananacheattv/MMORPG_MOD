#!/usr/bin/env python3
"""Orc guerrier : une masse. Tete basse enfoncee entre d'enormes epaules, machoire en avant et defenses, peinture de guerre rouge, chignon et tresse, spaliere a pointes du cote de l'arme, greves de fer, grande hache en croissant.
Marche lourde qui roule des epaules, frappe a deux mains par-dessus la tete, cri de guerre qui fait trembler le sol."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, keys, pulse, sin
from kit_mob import CF, CR, FR, M
from materiaux import stamp
from poses import LANCER, P, R, appui, clip, cycle, jambe

G = ('peau', 'orc')
CU = ('cuir', 'cuir')
TB = ('peau_tissu', 'tissu_brun')
IV = ('griffe', 'ivoire')
FE = ('plaque', 'fer')
P_ = {'k': '#0d0a08', 'r': '#b0281e', 'R': '#7a1712', 'y': '#ffb43a', 'd': '#2c3a18', 'l': '#8aa35a', 'w': '#e9e1c4', 'g': '#5f7a33'}


def build():
    m = M('orc_guerrier', 272, 1.0, kind='classic', description='Orc guerrier Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=14, epaule=27, cou=28, x_bras=11, x_jambe=5, coude=18, genou=7, z_cou=-3)

    # --- jambes en colonnes bien ecartees, greves de fer, bottes
    m.sb('leg_-1|leg_1', 'cuisse', (2, 7, -3), (8, 15, 3), TB, base=2.6)
    m.sb('tibia_-1|tibia_1', 'greve', (2, 2, -3), (8, 8, 3), FE, base=3, bord=1)
    m.sb('tibia_-1|tibia_1', 'genouillere', (3, 6, -4), (7, 9, -3), ('metal', 'fer'), base=4, hide={'south'})
    m.sb('tibia_-1|tibia_1', 'pied', (2, 0, -6), (8, 2, 3), CU, ton=-.8, couture=False)

    # --- bassin, ceinture a boucle d'os, pagne dechire
    m.b = 'body'
    m.c('bassin', (-8, 13, -4), (8, 17, 4), TB, base=2.2)
    m.c('ceinture', (-8, 16, -5), (8, 19, 5), CU, ton=-1.2, couture=False)
    n = m.c('boucle', (-2, 15, -6), (2, 20, -5), ('os', 'os'), base=3.8, hide={'south'})
    p.decal(n, 'north', stamp(['....', 'k..k', 'k..k', '....', '.kk.'], P_, 2, 20))
    m.c('pagne', (-3, 7, -5), (3, 16, -4), FR('tissu_brun'), base=3.4, mini=.55, **CF)
    m.c('pagne_dos', (-5, 7, 4), (5, 16, 5), FR('tissu_brun'), base=2.8, mini=.5, **CF)

    # --- buste en barrique : ventre, gros pectoraux, trapezes, bandouliere
    m.c('ventre', (-7, 19, -4), (7, 23, 4), G, base=3.2)
    n = m.c('poitrine', (-8, 23, -5), (8, 29, 5), G, base=3.6)
    p.decal(n, 'north', stamp(['................', 'd......dd......d', '.dd...d..d...dd.', '...ddd....ddd...'], P_, 8, 28))
    m.c('trapeze', (-6, 29, -4), (6, 31, 4), G, base=2.9)
    m.c('dos_large', (-7, 21, 4), (7, 29, 5), G, base=2.6, hide={'north'})
    m.c('bandouliere', (-2, 17, -6), (1, 32, -5), CU, ton=-.6, couture=False, rot=(0, 0, -27), pivot=(0, 24, -5), hide={'south'})
    m.c('bandouliere_dos', (-2, 17, 5), (1, 32, 6), CU, ton=-.6, couture=False, rot=(0, 0, -27), pivot=(0, 24, 5), hide={'north'})

    # --- tete basse et machoire en avant ; peinture de guerre en travers des yeux
    m.b = 'head'
    n = m.c('crane', (-5, 29, -8), (5, 37, 0), G, base=3.6)
    p.decal(n, 'north', stamp(['rr......rr', 'Rrr....rrR', '..........', '..........', '..........', '..........', '..........', 'k........k'], P_, 5, 37))
    m.c('crane_haut', (-4, 37, -7), (4, 38, -1), G, base=4)
    m.c('arcade', (-5, 34, -9), (5, 35, -8), G, base=2.1, hide={'south'})
    m.yeux('head', 2, 33, -9, 2, 1, ramp='flamme', base=4)
    m.c('nez', (-1, 30, -10), (1, 34, -8), G, base=2.8)
    m.s('oreille', (5, 33, -5), (8, 36, -3), G, base=3.3)
    m.c('gorge', (-4, 27, -7), (4, 31, -2), ('uni', 'soie_noire'), base=.4)
    # chignon et tresse
    m.c('chignon', (-2, 38, -4), (2, 40, 0), ('soies', 'soie_noire'), base=2.6)
    p.bone('tresse', (0, 38, 0), 'head')
    m.cb('tresse', 'tresse_haut', (-1, 33, 0), (1, 39, 2), ('cordage', 'soie_noire'), base=2.8)
    m.cb('tresse', 'anneau_tresse', (-2, 31, 0), (2, 33, 3), ('metal', 'or'), base=3)
    m.cb('tresse', 'tresse_bas', (-1, 27, 1), (1, 31, 3), ('cordage', 'soie_noire'), base=2.4)

    p.bone('machoire', (0, 30, -2), 'head')
    n = m.cb('machoire', 'mandibule', (-6, 26, -10), (6, 30, -1), G, base=3)
    p.decal(n, 'north', stamp(['kkkkkkkkkkkk'], P_, 6, 30))
    m.sb('machoire', 'defense', (4, 29, -11), (6, 33, -10), IV, base=4.2)

    # --- bras en troncs : grande epaule, avant-bras, canon de cuir clouté, poing
    m.sb('arm_-1|arm_1', 'epaule', (9, 18, -4), (14, 29, 4), G, base=3.4)
    m.sb('avbras_-1|avbras_1', 'avantbras', (9, 13, -3), (14, 19, 3), G, base=3.1)
    m.sb('avbras_-1|avbras_1', 'canon', (9, 8, -3), (14, 13, 3), ('plaque', 'cuir'), base=2.6, rivets=2)
    m.sb('avbras_-1|avbras_1', 'poing', (9, 4, -4), (14, 8, 3), G, base=3.7)
    # spaliere a pointes du cote de l'arme
    m.cb('arm_-1', 'spaliere', (8, 28, -6), (17, 32, 6), FE, base=3.2, rivets=3)
    m.cb('arm_-1', 'spaliere_bas', (13, 25, -5), (17, 29, 5), FE, base=2.5)
    m.pointe('arm_-1', 'pique', 13, 32, -3, 7, 1, ('metal', 'fer'), inc=(0, -12), base=4)
    m.pointe('arm_-1', 'pique', 13, 32, 3, 6, 1, ('metal', 'fer'), inc=(0, -12), base=4)
    # epaule gauche : une peau de bete
    m.cb('arm_1', 'peau_bete', (-15, 28, -5), (-8, 31, 5), ('fourrure', 'fourrure_brune'), base=2.2)
    m.cb('arm_1', 'peau_bete_b', (-15, 25, -5), (-13, 28, 5), ('fourrure', 'fourrure_brune'), base=1.9)

    # --- grande hache en croissant : fer clair, fil net
    p.bone('hache', (11.5, 6, -.5), 'avbras_-1', rot=(-24, 0, 0))
    m.cb('hache', 'manche', (11, 0, -2), (13, 32, 0), ('bois', 'bois_sombre'), base=3.2, planche=2)
    m.cb('hache', 'douille', (10, 23, -3), (14, 30, 1), ('metal', 'fer'), base=3)
    LAME = ['........##', '......####', '....######', '..########', '##########', '#########.', '########..', '#########.', '##########', '..########', '....######', '......####', '........##']
    m.cb('hache', 'lame', (11, 20, -13), (13, 33, -3), ('plan', 'acier'), base=3.8, masque=LAME, fil=2, **CR)
    m.cb('hache', 'contre_pique', (11, 25, 1), (13, 28, 6), ('metal', 'fer'), base=3.6)

    # ------------------------------------------------------------------ repos : souffle de bete, poitrine qui se gonfle, mufle qui renifle, hache qui pese
    T = 3.6
    cycle(p, 'idle', T, {
        'body': {'position': keys(T, lambda q: (0, sin(.45, 1, .1)(q) - .2, 0), 16), 'rotation': keys(T, lambda q: (sin(1.6, 1, .2)(q) - 2, 0, 0), 16)},
        'head': R(T, x=sin(-1.6, 1, .25, bias=-2), y=combine(sin(9, 1, 0), sin(3, 3, .1))),
        'machoire': R(T, x=combine(sin(2, 1, .1, bias=-3), pulse(-9, .62, .06)), n=32),
        'tresse': R(T, x=sin(2.5, 1, .3), z=sin(2, 1, .1)),
        'arm_-1': R(T, x=sin(1.5, 1, .15, bias=8), z=sin(1.8, 1, .3, bias=2)),
        'avbras_-1': R(T, x=sin(2, 1, .2, bias=14)),
        'hache': R(T, x=sin(2, 1, .25)),
        'arm_1': R(T, x=sin(-1.5, 1, .15, bias=2), z=sin(-1.8, 1, .3, bias=-5)),
        'avbras_1': R(T, x=sin(-2, 1, .2, bias=10)),
    })

    # ------------------------------------------------------------------ deplacement : lourd, en canard, les epaules roulent, le poids passe d'une jambe a l'autre
    T = 1.15
    amp = 25
    u0, l0 = jambe(T, amp, 0, 34, reach=.2)
    u1, l1 = jambe(T, amp, .5, 34, reach=.2)
    cycle(p, 'walk', T, {
        'root': {'position': keys(T, lambda q: (sin(1.2, 1, .25)(q), appui(T, amp, 14)(q) + cos(.5, 2, .05)(q), 0), 16)},
        'body': R(T, x=sin(1.2, 2, .1, bias=-5), y=sin(12, 1, 0), z=sin(5, 1, .25)),
        'head': R(T, x=cos(2, 2, .15, bias=3), y=sin(-9, 1, 0), z=sin(-4, 1, .25)),
        'machoire': R(T, x=sin(2, 2, .2, bias=-3)),
        'tresse': R(T, x=sin(5, 2, .2, bias=6), z=sin(6, 1, .35)),
        'leg_-1': u0, 'tibia_-1': l0, 'leg_1': u1, 'tibia_1': l1,
        'arm_-1': R(T, x=sin(-10, 1, 0, bias=14), z=sin(2, 2, .1, bias=4)),
        'avbras_-1': R(T, x=sin(5, 1, .2, bias=18)),
        'hache': R(T, x=sin(3, 2, .3)),
        'arm_1': R(T, x=sin(24, 1, 0, bias=2), z=sin(-3, 2, .1, bias=-7)),
        'avbras_1': R(T, x=sin(10, 1, .3, bias=16)),
    })

    # ------------------------------------------------------------------ attaque : hache levee a deux mains derriere la tete, abattue de tout le buste
    clip(p, 'attack', 1.0, [
        (.28, {'root+': (0, .6, 1.5), 'body': (12, -8, 0), 'head': (-2, 6, 0), 'arm_-1': (168, 0, 2), 'avbras_-1': (34, 0, 0), 'hache': (-60, 0, 0), 'arm_1': (146, 0, 18), 'avbras_1': (46, 0, 0),
               'leg_-1': (-8, 0, 0), 'leg_1': (10, 0, 0), 'tibia_1': (-12, 0, 0), 'machoire': (-14, 0, 0), 'tresse': (-14, 0, 0)}),
        (.46, {'root+': (0, -3, -3), 'body': (-28, 8, 0), 'head': (16, -6, 0), 'arm_-1': (62, 0, -8), 'avbras_-1': (6, 0, 0), 'hache': (-144, 0, 0), 'arm_1': (56, 0, 26), 'avbras_1': (20, 0, 0),
               'leg_-1': (30, 0, 0), 'tibia_-1': (-34, 0, 0), 'leg_1': (-20, 0, 0), 'tibia_1': (-14, 0, 0), 'machoire': (-30, 0, 0), 'tresse': (22, 0, 0)}),
        (.62, {'root+': (0, -2.6, -2.6), 'body': (-24, 6, 0), 'head': (14, -4, 0), 'arm_-1': (58, 0, -6), 'avbras_-1': (8, 0, 0), 'hache': (-142, 0, 0), 'arm_1': (50, 0, 22), 'avbras_1': (22, 0, 0), 'leg_-1': (26, 0, 0), 'tibia_-1': (-30, 0, 0), 'leg_1': (-16, 0, 0), 'machoire': (-22, 0, 0)}),
        (.85, {'root+': (0, -.5, -.4), 'body': (-5, 1, 0), 'head': (3, 0, 0), 'arm_-1': (14, 0, 0), 'avbras_-1': (10, 0, 0), 'hache': (-30, 0, 0), 'arm_1': (10, 0, 4), 'avbras_1': (10, 0, 0), 'leg_-1': (5, 0, 0), 'leg_1': (-4, 0, 0), 'machoire': (-5, 0, 0)}),
    ])

    # ------------------------------------------------------------------ sort : cri de guerre. S'accroupit, explose vers le haut bras ouverts, hache brandie, la gueule grande ouverte
    clip(p, 'cast', 1.5, [
        (.18, {'root+': (0, -2.4, 0), 'body': (-16, 0, 0), 'head': (-10, 0, 0), 'arm_-1': (18, 0, 34), 'avbras_-1': (40, 0, 0), 'arm_1': (18, 0, -34), 'avbras_1': (40, 0, 0), 'leg_-1': (28, 0, 0), 'tibia_-1': (-44, 0, 0), 'leg_1': (28, 0, 0), 'tibia_1': (-44, 0, 0)}),
        (LANCER, {'root+': (0, 1.5, 0), 'body': (14, 0, 0), 'head': (26, 0, 0), 'machoire': (-36, 0, 0), 'arm_-1': (156, 0, 26), 'avbras_-1': (6, 0, 0), 'hache': (-10, 0, 0), 'arm_1': (130, 0, -48), 'avbras_1': (22, 0, 0), 'tresse': (-30, 0, 0)}),
        (.7, {'root+': (0, 1, 0), 'body': (10, 2, 1), 'head': (22, 3, 0), 'machoire': (-34, 0, 0), 'arm_-1': (150, 0, 24), 'avbras_-1': (10, 0, 0), 'hache': (-8, 0, 0), 'arm_1': (124, 0, -46), 'avbras_1': (26, 0, 0), 'tresse': (-20, 0, 0)}),
        (.8, {'root+': (0, 1.2, 0), 'body': (11, -2, -1), 'head': (24, -3, 0), 'machoire': (-36, 0, 0), 'arm_-1': (152, 0, 26), 'avbras_-1': (8, 0, 0), 'arm_1': (127, 0, -48), 'avbras_1': (24, 0, 0)}),
        (1.2, {'root+': (0, 0, 0), 'body': (0, 0, 0), 'head': (4, 0, 0), 'machoire': (-6, 0, 0), 'arm_-1': (24, 0, 6), 'avbras_-1': (14, 0, 0), 'arm_1': (16, 0, -8), 'avbras_1': (14, 0, 0)}),
    ])

    # ------------------------------------------------------------------ mort : tombe d'abord a genoux, la hache plante au sol et lui echappe, la tete pend
    clip(p, 'death', 1.0, [
        (.22, {'body': (10, 0, 4), 'head': (20, 0, -5), 'machoire': (-26, 0, 0), 'arm_-1': (34, 0, 20), 'avbras_-1': (26, 0, 0), 'arm_1': (26, 0, -26), 'avbras_1': (22, 0, 0)}),
        (.6, {'root+': (0, -5, -1), 'body': (-14, 0, 2), 'head': (-16, 0, -4), 'machoire': (-18, 0, 0), 'arm_-1': (26, 0, 8), 'avbras_-1': (6, 0, 0), 'hache': (-60, 0, 0), 'arm_1': (14, 0, -8), 'avbras_1': (10, 0, 0),
              'leg_-1': (62, 0, 0), 'tibia_-1': (-98, 0, 0), 'leg_1': (58, 0, 0), 'tibia_1': (-102, 0, 0)}),
        (1.0, {'root+': (0, -5.6, -1.4), 'body': (-30, 0, 3), 'head': (-30, 0, -8), 'machoire': (-12, 0, 0), 'arm_-1': (12, 0, 4), 'avbras_-1': (2, 0, 0), 'hache': (-92, 0, 0), 'arm_1': (6, 0, -4), 'avbras_1': (4, 0, 0),
               'leg_-1': (66, 0, 0), 'tibia_-1': (-104, 0, 0), 'leg_1': (62, 0, 0), 'tibia_1': (-108, 0, 0)}),
    ], retour=False)
    return p
