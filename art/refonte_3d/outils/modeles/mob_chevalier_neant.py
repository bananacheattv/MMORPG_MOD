#!/usr/bin/env python3
"""Chevalier du neant : armure noire aux joints violets, heaume a fente en T et cornes vers l'arriere, epaulieres etagees, cape de nuit etoilee, grande epee a deux mains au fil violet, tenue en garde basse.
Pas lourd et regulier, taille montante du bas a droite vers le haut a gauche, sort : plante l'epee dans le sol en mettant un genou a terre, et meurt agenouille sur sa lame."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, keys, pulse, sin
from kit_mob import CF, CR, FR, M
from materiaux import stamp
from poses import LANCER, P, R, appui, clip, cycle, jambe

NO = ('plaque', 'fer_noir')
MN = ('metal', 'fer_noir')
VI = ('uni', 'nuit_violet')
P_ = {'k': '#07060c', 'v': '#8f4cf0', 'V': '#e6d4ff', 'n': '#272338', 'g': '#555a67'}


def build():
    m = M('chevalier_neant', 276, 1.0, kind='classic', description='Chevalier du neant Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=15, epaule=28, cou=29, x_bras=8, x_jambe=3, coude=20, genou=8, z_cou=-1)
    # garde basse a deux mains : pose de repos, tous les clips s'y ajoutent
    p.bones['arm_-1'].rot = (30, 20, 0)
    p.bones['avbras_-1'].rot = (26, 0, 0)
    p.bones['arm_1'].rot = (52, -62, 0)
    p.bones['avbras_1'].rot = (6, 0, 0)

    # --- jambes : cuissards, genouilleres a joint violet, greves, solerets pointus
    m.sb('leg_-1|leg_1', 'cuissard', (1, 8, -3), (6, 16, 3), NO, base=2.8, bord=1)
    m.sb('leg_-1|leg_1', 'tassette', (5, 11, -3), (7, 17, 3), NO, base=2.2, bord=1)
    m.sb('tibia_-1|tibia_1', 'greve', (1, 3, -3), (6, 9, 2), NO, base=2.6, bord=1)
    m.sb('tibia_-1|tibia_1', 'genouillere', (2, 7, -4), (5, 10, -3), MN, base=4.4, hide={'south'})
    m.sb('tibia_-1|tibia_1', 'soleret', (1, 0, -5), (6, 3, 3), NO, base=3, bord=0)
    m.sb('tibia_-1|tibia_1', 'pointe', (2, 0, -7), (5, 2, -5), MN, base=3.6)

    # --- bassin, braconniere, plastron a arete, gorgerin
    m.b = 'body'
    m.c('bassin', (-5, 14, -3), (5, 18, 3), NO, base=2.2, bord=0)
    m.c('braconniere', (-3, 9, -4), (3, 16, -3), NO, base=2.6, hide={'south'})
    m.lueur('body', 'joint', (-5, 17, -4), (5, 18, 3), VI, base=4.6)
    m.c('ventriere', (-6, 18, -4), (6, 23, 4), NO, base=2.5)
    n = m.c('plastron', (-7, 23, -5), (7, 29, 4), NO, base=3.2)
    m.c('arete', (-1, 19, -6), (1, 29, -5), MN, base=4.6, hide={'south'})
    m.lueur('body', 'rune', (-3, 24, -6), (-2, 28, -5), VI, base=5, hide={'south'})
    m.lueur('body', 'rune', (2, 24, -6), (3, 28, -5), VI, base=5, hide={'south'})
    m.c('gorgerin', (-4, 28, -4), (4, 31, 3), NO, base=2.3, bord=0)

    # --- cape de nuit en deux pans articules
    p.bone('cape_a', (0, 29, 4), 'body')
    m.cb('cape_a', 'cape_haut', (-7, 16, 4), (7, 30, 5), ('etoile', 'vide'), base=1.8, **CF)
    p.bone('cape_b', (0, 16, 4), 'cape_a')
    m.cb('cape_b', 'cape_bas', (-7, 3, 4), (7, 17, 5), FR('vide'), base=1.6, mini=.5, **CF)

    # --- heaume clos, fente en T, cornes vers l'arriere
    m.b = 'head'
    n = m.c('heaume', (-4, 30, -5), (4, 38, 3), NO, base=3, bord=0)
    p.decal(n, 'north', stamp(['...gg...', '...gg...', '...gg...', '........', '........', '...gg...', '.g.gg.g.', '.g.gg.g.'], P_, 4, 38))
    m.c('nasal', (-1, 31, -6), (1, 37, -5), MN, base=4.4, hide={'south'})
    m.lueur('head', 'fente', (-3, 34, -6), (3, 35, -5), VI, base=5.4, hide={'south'})
    m.c('sommet', (-3, 38, -4), (3, 39, 2), NO, base=3.4, bord=0)
    p.bone('corne_g', (4, 36, -1), 'head', rot=(38, 0, -28))
    p.bone('corne_d', (-4, 36, -1), 'head', rot=(38, 0, 28))
    m.sb('corne_g|corne_d', 'corne', (3, 35, -2), (6, 41, 1), MN, base=3.2)
    m.sb('corne_g|corne_d', 'corne_b', (4, 41, -1), (6, 45, 1), MN, base=3.8)
    m.sb('corne_g|corne_d', 'corne_c', (4, 45, 0), (5, 48, 1), MN, base=4.6)

    # --- bras : epauliere etagee, brassard, canon, gantelet
    m.sb('arm_-1|arm_1', 'bras', (6, 20, -3), (10, 28, 2), NO, base=2.3, bord=0)
    m.sb('arm_-1|arm_1', 'epauliere', (5, 27, -5), (12, 31, 4), NO, base=3.4)
    m.sb('arm_-1|arm_1', 'epauliere_b', (8, 24, -5), (12, 27, 4), NO, base=2.7)
    m.sb('arm_-1|arm_1', 'epauliere_haut', (6, 31, -3), (11, 32, 2), MN, base=4.2)
    m.lueur_sym('arm_-1|arm_1', 'joint_coude', (6, 19, -3), (10, 20, 2), VI, base=4.4)
    m.sb('avbras_-1|avbras_1', 'canon', (6, 13, -3), (10, 19, 2), NO, base=2.9)
    m.sb('avbras_-1|avbras_1', 'gantelet', (6, 10, -4), (10, 13, 2), NO, base=3.5, bord=0)

    # --- grande epee a deux mains
    p.bone('epee', (8, 11, -1), 'avbras_-1', rot=(174, 0, 0))
    m.cb('epee', 'pommeau', (7, 3, -2), (9, 5, 0), MN, base=4.6)
    m.cb('epee', 'fusee', (7, 5, -2), (9, 15, 0), ('cuir', 'cuir'), ton=-1.6, couture=False)
    m.cb('epee', 'garde', (3, 15, -3), (13, 17, 1), NO, base=3.6, bord=0)
    m.cb('epee', 'lame', (6, 17, -2), (10, 37, 0), ('metal', 'fer_noir'), base=2.6)
    m.lueur('epee', 'fil', (5, 17, -2), (6, 36, -1), VI, base=4.6)
    m.lueur('epee', 'fil', (10, 17, -2), (11, 36, -1), VI, base=4.6)
    m.cb('epee', 'pointe_lame', (7, 37, -2), (9, 40, 0), ('metal', 'fer_noir'), base=3.2)

    # ------------------------------------------------------------------ repos : souffle lent sous l'armure, tete qui pivote a peine, cape qui ondule sans vent
    T = 4.4
    cycle(p, 'idle', T, {
        'body': {'position': keys(T, lambda q: (0, sin(.3, 1, .1)(q), 0), 16), 'rotation': keys(T, lambda q: (sin(1, 1, .2)(q), 0, 0), 16)},
        'head': R(T, x=sin(1.2, 1, .3), y=sin(6, 1, 0)),
        'arm_-1': R(T, x=sin(1.2, 1, .15)),
        'arm_1': R(T, x=sin(1, 1, .15)),
        'cape_a': R(T, x=sin(3, 1, .1, bias=-3), z=sin(1.5, 1, .35)),
        'cape_b': R(T, x=sin(7, 1, .25, bias=-3), z=sin(3, 1, .5)),
    })

    # ------------------------------------------------------------------ deplacement : pas lourd, metronome ; le buste ne penche pas, l'epee reste en garde, la cape claque en retard
    T = 1.3
    amp = 24
    u0, l0 = jambe(T, amp, 0, 36, reach=.2)
    u1, l1 = jambe(T, amp, .5, 36, reach=.2)
    cycle(p, 'walk', T, {
        'root': P(T, y=combine(appui(T, amp, 15), cos(.45, 2, .08))),
        'body': R(T, x=sin(.8, 2, .1, bias=-3), y=sin(6, 1, 0), z=sin(2, 1, .25)),
        'head': R(T, x=sin(.8, 2, .1, bias=2), y=sin(-5, 1, 0), z=sin(-1.5, 1, .25)),
        'leg_-1': u0, 'tibia_-1': l0, 'leg_1': u1, 'tibia_1': l1,
        'arm_-1': R(T, x=sin(1.5, 2, .1), y=sin(-2, 1, 0)),
        'arm_1': R(T, x=sin(1.5, 2, .1), y=sin(-2, 1, 0)),
        'cape_a': R(T, x=sin(5, 2, .15, bias=-12), z=sin(3, 1, .3)),
        'cape_b': R(T, x=sin(10, 2, .3, bias=-14), z=sin(6, 1, .45)),
    })

    # ------------------------------------------------------------------ attaque : la lame part du bas a droite et remonte en diagonale jusqu'au-dessus de l'epaule gauche
    clip(p, 'attack', 1.0, [
        (.24, {'root+': (0, -1, 1), 'body': (4, -34, 0), 'head': (-2, 24, 0), 'arm_-1': (-36, -14, 26), 'avbras_-1': (16, 0, 0), 'epee': (6, 0, 0), 'arm_1': (-20, 34, 4), 'leg_-1': (-14, 0, 0), 'tibia_-1': (-10, 0, 0), 'leg_1': (18, 0, 0), 'tibia_1': (-26, 0, 0), 'cape_a': (-8, 0, 6)}),
        (.44, {'root+': (0, -1.4, -3), 'body': (-10, 38, 4), 'head': (6, -28, 0), 'arm_-1': (96, 8, -22), 'avbras_-1': (-14, 0, 0), 'epee': (-8, 0, 0), 'arm_1': (62, 18, -2), 'avbras_1': (10, 0, 0), 'leg_-1': (34, 0, 0), 'tibia_-1': (-32, 0, 0), 'leg_1': (-24, 0, 0), 'tibia_1': (-12, 0, 0), 'cape_a': (-22, 0, -6), 'cape_b': (-20, 0, 0)}),
        (.6, {'root+': (0, -1, -2.6), 'body': (-6, 44, 5), 'head': (6, -32, 0), 'arm_-1': (116, 12, -26), 'avbras_-1': (-14, 0, 0), 'epee': (-14, 0, 0), 'arm_1': (78, 20, 0), 'avbras_1': (8, 0, 0), 'leg_-1': (28, 0, 0), 'tibia_-1': (-28, 0, 0), 'leg_1': (-18, 0, 0), 'cape_a': (-16, 0, -4), 'cape_b': (-24, 0, 0)}),
        (.84, {'root+': (0, -.3, -.5), 'body': (-1, 8, 1), 'head': (1, -6, 0), 'arm_-1': (20, 2, -4), 'avbras_-1': (-2, 0, 0), 'arm_1': (14, 4, 0), 'leg_-1': (5, 0, 0), 'leg_1': (-4, 0, 0), 'cape_a': (-4, 0, 0), 'cape_b': (-6, 0, 0)}),
    ])

    # ------------------------------------------------------------------ sort : leve l'epee a deux mains et la plante dans le sol, un genou a terre, la main gauche ouverte vers le ciel
    clip(p, 'cast', 1.7, [
        (.16, {'root+': (0, 1, .5), 'body': (8, 0, 0), 'head': (12, 0, 0), 'arm_-1': (96, -14, 4), 'avbras_-1': (-20, 0, 0), 'epee': (-10, 0, 0), 'arm_1': (74, 44, 8), 'cape_a': (-6, 0, 0)}),
        (LANCER, {'root+': (0, -6, -1), 'body': (-16, 0, 0), 'head': (-2, 0, 0), 'arm_-1': (8, -18, 2), 'avbras_-1': (-22, 0, 0), 'epee': (-36, 0, 0), 'arm_1': (88, 66, -34), 'avbras_1': (20, 0, 0), 'leg_-1': (66, 0, 0), 'tibia_-1': (-104, 0, 0), 'leg_1': (-6, 0, 0), 'tibia_1': (-70, 0, 0), 'cape_a': (-30, 0, 0), 'cape_b': (-26, 0, 0)}),
        (.95, {'root+': (0, -6, -1), 'body': (-12, 0, 0), 'head': (6, 0, 0), 'arm_-1': (6, -18, 2), 'avbras_-1': (-22, 0, 0), 'epee': (-34, 0, 0), 'arm_1': (104, 66, -30), 'avbras_1': (10, 0, 0), 'leg_-1': (66, 0, 0), 'tibia_-1': (-104, 0, 0), 'leg_1': (-6, 0, 0), 'tibia_1': (-70, 0, 0), 'cape_a': (-6, 0, 0), 'cape_b': (-10, 0, 0)}),
        (1.4, {'root+': (0, -1.5, 0), 'body': (-3, 0, 0), 'arm_-1': (6, -4, 0), 'avbras_-1': (-6, 0, 0), 'epee': (-10, 0, 0), 'arm_1': (14, 10, -4), 'leg_-1': (14, 0, 0), 'tibia_-1': (-24, 0, 0), 'tibia_1': (-14, 0, 0)}),
    ])

    # ------------------------------------------------------------------ mort : recule d'un pas, plante sa lame, tombe a genoux et reste la, tete baissee sur le pommeau
    clip(p, 'death', 1.0, [
        (.2, {'root+': (0, 0, 1.5), 'body': (12, 0, 0), 'head': (16, 0, 0), 'arm_-1': (10, 0, 8), 'arm_1': (-14, 16, 0), 'leg_1': (16, 0, 0), 'tibia_1': (-20, 0, 0)}),
        (.6, {'root+': (0, -7, 0), 'body': (-10, 0, 0), 'head': (-8, 0, 0), 'arm_-1': (10, -18, 2), 'avbras_-1': (-22, 0, 0), 'epee': (-36, 0, 0), 'arm_1': (-6, 10, 0), 'leg_-1': (66, 0, 0), 'tibia_-1': (-106, 0, 0), 'leg_1': (58, 0, 0), 'tibia_1': (-108, 0, 0), 'cape_a': (-14, 0, 0)}),
        (1.0, {'root+': (0, -8, -.5), 'body': (-24, 0, 0), 'head': (-28, 0, 4), 'arm_-1': (10, -18, 2), 'avbras_-1': (-22, 0, 0), 'epee': (-36, 0, 0), 'arm_1': (-22, 10, 0), 'avbras_1': (-6, 0, 0), 'leg_-1': (68, 0, 0), 'tibia_-1': (-110, 0, 0), 'leg_1': (60, 0, 0), 'tibia_1': (-112, 0, 0)}),
    ], retour=False)
    return p
