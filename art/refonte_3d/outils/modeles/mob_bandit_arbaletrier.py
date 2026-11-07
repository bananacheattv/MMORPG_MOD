#!/usr/bin/env python3
"""Bandit arbaletrier : silhouette fine et aux aguets. Capuche bordeaux a pointe, foulard jusqu'au nez, seuls les yeux depassent, long manteau de cuir fendu, bottes hautes, carquois de carreaux a la hanche, arbalete tenue en joue a deux mains.
Il avance plie, l'arme en joue sans la bouger, tire avec un recul d'epaules puis retend la corde de la main gauche. Sort : fiole lancee de la main gauche."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, keys, pulse, sin
from kit_mob import CF, CR, FR, M
from materiaux import stamp
from poses import LANCER, P, R, appui, clip, cycle, jambe

CU = ('cuir', 'cuir')
TB = ('peau_tissu', 'tissu_brun')
BD = ('peau_tissu', 'rouge')
H = ('peau', 'humain')
P_ = {'k': '#0d0a08', 'y': '#e8e0c0', 'p': '#c28f6a', 's': '#8a5f46', 'd': '#4a0f12', 'o': '#b8672d', 'n': '#1e140c'}


def build():
    m = M('bandit_arbaletrier', 273, 1.0, kind='classic', description='Bandit arbaletrier Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=14, epaule=25, cou=26, x_bras=5, x_jambe=2, coude=18, genou=7, z_bras=-1, z_cou=-1)
    # pose de repos : l'arme en joue. Les clips s'ajoutent a cette pose.
    p.bones['arm_-1'].rot = (24, 0, 0)
    p.bones['avbras_-1'].rot = (66, 0, 0)
    p.bones['arm_1'].rot = (60, -14, 0)
    p.bones['avbras_1'].rot = (22, 0, 0)

    # --- jambes fines, bottes hautes a revers
    m.sb('leg_-1|leg_1', 'cuisse', (1, 7, -2), (4, 15, 2), TB, base=3.6)
    m.sb('tibia_-1|tibia_1', 'botte', (1, 2, -2), (4, 7, 2), CU, ton=-1.6, couture=False)
    m.sb('tibia_-1|tibia_1', 'revers', (1, 7, -3), (4, 8, -2), CU, ton=-.6, couture=False, hide={'south'})
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -4), (4, 2, 2), CU, ton=-2, couture=False)

    # --- long manteau fendu, ceinture, carquois, fiole
    m.b = 'body'
    m.c('bassin', (-4, 13, -2), (4, 16, 2), TB, base=2.2)
    m.c('ceinture', (-4, 15, -3), (4, 16, 3), CU, ton=-1.5, couture=False)
    m.c('boucle', (-1, 14, -4), (1, 17, -3), ('metal', 'fer'), base=3.8, hide={'south'})
    m.s('pan_devant', (1, 10, -3), (5, 15, -2), FR('cuir'), base=1.4, mini=.6, **CF)
    m.c('pan_dos', (-4, 8, 2), (4, 15, 3), FR('cuir'), base=1.2, mini=.55, **CF)
    p.bone('carquois', (5, 13, 2), 'body', rot=(16, 0, -8))
    m.cb('carquois', 'etui', (4, 6, 1), (7, 14, 4), ('plaque', 'cuir'), base=2.2)
    for i, (x, z, h) in enumerate(((4, 1, 3), (6, 1, 2), (5, 3, 3))):
        m.cb('carquois', 'carreau_tige', (x, 14, z), (x + 1, 14 + h, z + 1), ('bois', 'bois_clair'), base=3.4)
    m.c('fiole', (-6, 10, -2), (-4, 14, 1), ('cristal', 'venin'), base=2.6)
    m.c('bouchon', (-6, 14, -1), (-4, 15, 0), CU, ton=.4, couture=False)

    # --- buste etroit, manteau lace, baudrier, mantelet effiloche
    n = m.c('veste', (-4, 16, -2), (4, 24, 2), CU, ton=-1.1, couture=False)
    p.decal(n, 'north', stamp(['...nn...', '...nn...', '...nn...', '...nn...', '...nn...', '...nn...'], P_, 4, 22))
    m.c('baudrier', (-2, 15, -3), (0, 26, -2), CU, ton=.5, couture=False, rot=(0, 0, -30), pivot=(-1, 20, -3), hide={'south'})
    m.c('mantelet', (-5, 22, -3), (5, 26, 3), BD, base=1.6)
    m.c('cape_courte', (-5, 15, 3), (5, 26, 4), FR('rouge'), base=1.2, mini=.55, **CF)

    # --- capuche a pointe : tout le visage dans l'ombre, foulard jusqu'au nez
    m.b = 'head'
    n = m.c('capuche', (-4, 26, -5), (4, 34, 3), BD, base=1.9)
    p.decal(n, 'north', stamp(['........', '.dddddd.', '.dpppsd.', '.dppppd.', '.dddddd.'], P_, 4, 33))
    p.decal(n, 'north', stamp(['.kyyk.yk'[:8]], P_, 4, 31))
    m.yeux('head', 1, 30, -5, 1, 1, ramp='ivoire', base=3.6, dz=1)
    m.c('foulard', (-3, 25, -6), (3, 29, -5), BD, base=2.5, hide={'south'})
    m.c('visiere', (-4, 33, -6), (4, 34, -5), BD, base=1.3, hide={'south'})
    m.c('pointe', (-3, 34, -2), (3, 36, 3), BD, base=1.7)
    m.c('pointe_b', (-2, 33, 3), (2, 35, 6), BD, base=1.5)
    m.c('pointe_c', (-1, 31, 6), (1, 34, 8), BD, base=1.3)

    # --- bras fins, canon de cuir, gantelet
    m.sb('arm_-1|arm_1', 'bras', (4, 18, -2), (7, 26, 1), CU, ton=-1.1, couture=False)
    m.sb('avbras_-1|avbras_1', 'avantbras', (4, 11, -2), (7, 19, 1), CU, ton=-.5, couture=False)
    m.sb('avbras_-1|avbras_1', 'main', (4, 9, -3), (7, 11, 1), H, base=3.2)

    # --- arbalete : crosse le long de l'avant-bras, arc de fer, corde en V, carreau engage. En joue : -y local vers l'avant, -z local vers le haut.
    p.bone('arbalete', (6, 10, -1), 'avbras_-1')
    m.cb('arbalete', 'crosse', (5, -6, -5), (7, 13, -3), ('bois', 'bois_sombre'), base=3.2, planche=2)
    m.cb('arbalete', 'talon', (5, 13, -6), (7, 16, -2), ('bois', 'bois_sombre'), base=2.6, planche=2)
    m.cb('arbalete', 'etrier', (4, -9, -6), (8, -6, -3), ('metal', 'fer'), base=3.2)
    m.cb('arbalete', 'arc', (7, -8, -5), (15, -6, -3), ('metal', 'acier'), base=3.8, rot=(0, 0, 14), pivot=(7, -7, -4))
    m.cb('arbalete', 'arc', (-3, -8, -5), (5, -6, -3), ('metal', 'acier'), base=3.8, rot=(0, 0, -14), pivot=(5, -7, -4))
    m.cb('arbalete', 'corde', (6, 3, -5), (16, 4, -4), ('uni', 'corde'), base=3.4, rot=(0, 0, -46), pivot=(6, 3.5, -4.5))
    m.cb('arbalete', 'corde', (-4, 3, -5), (6, 4, -4), ('uni', 'corde'), base=3.4, rot=(0, 0, 46), pivot=(6, 3.5, -4.5))
    m.cb('arbalete', 'carreau', (5, -9, -6), (6, 4, -5), ('bois', 'bois_clair'), base=3.6)
    m.cb('arbalete', 'pointe_carreau', (5, -12, -6), (6, -9, -5), ('metal', 'acier'), base=5)

    # ------------------------------------------------------------------ repos : aux aguets, regarde a droite puis a gauche, ne baisse pas l'arme, transfere son poids
    T = 3.2
    cycle(p, 'idle', T, {
        'root': {'position': keys(T, lambda q: (sin(.5, 1, 0)(q), -.4 + sin(.2, 2, .1)(q), 0), 16)},
        'body': R(T, x=sin(1, 2, .2, bias=-3), z=sin(1.4, 1, 0)),
        'head': {'rotation': [(0, -2, 0, 0), (.7, -2, 0, 0), (1, 0, 26, 0), (1.5, 0, 26, 0), (1.8, -2, -24, 0), (2.4, -1, -24, 0), (2.8, -2, 0, 0), (T, -2, 0, 0)]},
        'arm_-1': R(T, x=sin(1.2, 2, .1)),
        'arm_1': R(T, x=sin(1.2, 2, .15)),
        'leg_-1': R(T, x=sin(.8, 1, .2, bias=5)),
        'leg_1': R(T, x=sin(.8, 1, .7, bias=-3)),
        'tibia_-1': R(T, x=sin(1, 1, .2, bias=-8)),
        'tibia_1': R(T, x=sin(1, 1, .7, bias=-3)),
        'carquois': R(T, x=sin(1, 1, .3)),
    })

    # ------------------------------------------------------------------ deplacement : pas de chasseur, genoux plies, buste bas et stable ; l'arme reste pointee, les pans du manteau battent
    T = .98
    amp = 24
    u0, l0 = jambe(T, amp, 0, 40, bias=4)
    u1, l1 = jambe(T, amp, .5, 40, bias=4)
    cycle(p, 'walk', T, {
        'root': P(T, y=combine(appui(T, amp, 14), cos(.3, 2, .05))),
        'body': R(T, x=sin(.8, 2, .1, bias=-9), y=sin(3.5, 1, 0), z=sin(1.2, 1, .25)),
        'head': R(T, x=sin(.8, 2, .1, bias=6), y=sin(-3, 1, 0), z=sin(-1, 1, .25)),
        'leg_-1': u0, 'tibia_-1': l0, 'leg_1': u1, 'tibia_1': l1,
        'arm_-1': R(T, x=sin(1.5, 2, .1, bias=6)),
        'avbras_-1': R(T, x=sin(1, 2, .2)),
        'arm_1': R(T, x=sin(1.5, 2, .1, bias=4)),
        'carquois': R(T, x=sin(4, 2, .1), z=sin(3, 1, .25)),
    })

    # ------------------------------------------------------------------ attaque : epaule, tir, recul, l'arme baisse, la main gauche retend la corde
    clip(p, 'attack', 1.0, [
        (.12, {'root+': (0, -.5, -.6), 'body': (-6, 0, 0), 'head': (-4, 0, 0), 'arm_-1': (2, 0, 0), 'arm_1': (2, 0, 0)}),
        (.2, {'root+': (0, 0, 2), 'body': (10, 0, 0), 'head': (8, 0, 0), 'arm_-1': (12, 0, 0), 'avbras_-1': (4, 0, 0), 'arm_1': (10, 0, 0), 'avbras_1': (4, 0, 0), 'leg_-1': (-8, 0, 0), 'leg_1': (8, 0, 0)}),
        (.42, {'root+': (0, -.4, .6), 'body': (-3, 0, 0), 'arm_-1': (-22, 0, 0), 'avbras_-1': (-6, 0, 0), 'arm_1': (-46, 18, -8), 'avbras_1': (46, 0, 0)}),
        (.66, {'root+': (0, -.6, .4), 'body': (-6, -8, 0), 'head': (-10, 6, 0), 'arm_-1': (-30, 0, 0), 'avbras_-1': (-6, 0, 0), 'arm_1': (-84, 30, -4), 'avbras_1': (76, 0, 0)}),
        (.82, {'body': (-3, -3, 0), 'head': (-3, 2, 0), 'arm_-1': (-10, 0, 0), 'arm_1': (-30, 10, 0), 'avbras_1': (22, 0, 0)}),
    ])

    # ------------------------------------------------------------------ sort : lache l'arme de la main gauche, saisit sa fiole a la hanche et la jette par-dessus l'epaule
    clip(p, 'cast', 1.2, [
        (.15, {'root+': (0, -.8, .5), 'body': (2, 26, 3), 'head': (0, -22, 0), 'arm_-1': (-44, 0, 8), 'avbras_-1': (-20, 0, 0), 'arm_1': (-130, 30, -12), 'avbras_1': (50, 0, 0)}),
        (LANCER, {'root+': (0, -.2, -1.5), 'body': (-10, -26, -2), 'head': (6, 22, 0), 'arm_-1': (-60, 0, 14), 'avbras_-1': (-30, 0, 0), 'arm_1': (70, 18, -10), 'avbras_1': (-20, 0, 0), 'leg_1': (18, 0, 0), 'tibia_1': (-14, 0, 0), 'leg_-1': (-12, 0, 0)}),
        (.5, {'root+': (0, -.3, -1.2), 'body': (-8, -18, -2), 'head': (5, 15, 0), 'arm_-1': (-50, 0, 10), 'avbras_-1': (-24, 0, 0), 'arm_1': (36, 10, -8), 'avbras_1': (-6, 0, 0), 'leg_1': (14, 0, 0), 'tibia_1': (-12, 0, 0), 'leg_-1': (-10, 0, 0)}),
        (.95, {'body': (-2, -2, 0), 'arm_-1': (-6, 0, 0), 'arm_1': (-6, 0, 0)}),
    ])

    # ------------------------------------------------------------------ mort : le tir part en l'air, l'arbalete tombe, il glisse sur un genou et s'affaisse de cote
    clip(p, 'death', 1.0, [
        (.2, {'body': (12, 0, -5), 'head': (16, 0, 6), 'arm_-1': (30, 0, 12), 'avbras_-1': (-24, 0, 0), 'arbalete': (22, 0, 0), 'arm_1': (-30, 10, -30), 'avbras_1': (-8, 0, 0)}),
        (.6, {'root+': (0, -4, 0), 'body': (-12, 0, 8), 'head': (-20, 0, 8), 'arm_-1': (-48, 0, 6), 'avbras_-1': (-40, 0, 0), 'arbalete': (50, 0, 0), 'arm_1': (-54, 12, -14), 'avbras_1': (-18, 0, 0), 'leg_-1': (52, 0, 0), 'tibia_-1': (-88, 0, 0), 'leg_1': (28, 0, 0), 'tibia_1': (-58, 0, 0)}),
        (1.0, {'root+': (0, -5, 0), 'body': (-26, 0, 16), 'head': (-30, 0, 16), 'arm_-1': (-56, 0, 2), 'avbras_-1': (-50, 0, 0), 'arbalete': (64, 0, 0), 'arm_1': (-60, 12, -6), 'avbras_1': (-22, 0, 0), 'leg_-1': (60, 0, 0), 'tibia_-1': (-98, 0, 0), 'leg_1': (34, 0, 0), 'tibia_1': (-68, 0, 0)}),
    ], retour=False)
    return p
