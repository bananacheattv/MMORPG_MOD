#!/usr/bin/env python3
"""Golem de cristal : montagne de roche noire, petite tete enfoncee, poitrail ferme par deux battants de pierre sur un noyau de glace, epaules et dos herisses de cristaux, bras immenses qui touchent presque le sol.
Marche de gorille, double coup de poings qui ecrase, sort unique : les battants de la poitrine s'ouvrent sur le noyau. A sa mort il s'effondre en eboulis."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, keys, pulse, sin
from kit_mob import CF, CR, M
from materiaux import stamp
from poses import LANCER, P, R, appui, clip, cycle, jambe

RO = ('roche', 'pierre_noire')
PL = ('plaque', 'pierre_noire')
CG = ('cristal', 'glace')
P_ = {'k': '#07080c', 'c': '#b4eef7', 'b': '#38a0cc', 'w': '#effdff'}


def amas(m, os_, x, y, z, tailles, cote=1, base=2):
    """Grappe de cristaux qui rayonnent depuis un point : (dx, dz, h, r, bascule x, bascule z)."""
    for dx, dz, h, r, rx, rz in tailles:
        m.pointe(os_, 'cristal', x + dx * cote, y, z + dz, h, r, CG, inc=(rx, rz * cote), tourne=45, base=base)


def build():
    m = M('golem_cristal', 279, 1.0, kind='classic', description='Golem de cristal Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=16, epaule=33, cou=34, x_bras=15, x_jambe=6, coude=21, genou=7, z_cou=-4)
    p.bones['arm_-1'].rot = (8, 0, 4)
    p.bones['arm_1'].rot = (8, 0, -4)
    p.bones['avbras_-1'].rot = (6, 0, 0)
    p.bones['avbras_1'].rot = (6, 0, 0)

    # --- jambes courtes en piliers
    m.sb('leg_-1|leg_1', 'cuisse', (2, 7, -5), (10, 18, 5), RO, base=3.2)
    m.sb('tibia_-1|tibia_1', 'tibia', (2, 3, -5), (10, 8, 5), PL, base=3.2, bord=1)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -8), (11, 4, 5), RO, base=3.7)
    m.pointe('tibia_-1', 'eclat_genou', 7, 8, -5, 5, 1, CG, inc=(-20, -10), base=2)
    m.pointe('tibia_1', 'eclat_genou', -5, 8, -5, 4, 1, CG, inc=(-20, 10), base=2)

    # --- bassin, ventre, grand poitrail ; deux battants de pierre devant le noyau
    m.b = 'body'
    m.c('bassin', (-9, 14, -5), (9, 20, 5), RO, base=2.8)
    m.c('ventre', (-8, 20, -5), (8, 26, 5), RO, base=2.8)
    m.c('poitrail', (-12, 26, -6), (12, 39, 6), RO, base=3.4)
    m.c('creux', (-6, 27, -7), (6, 38, -6), ('uni', 'nuit'), base=.6, hide={'south'})
    m.lueur('body', 'noyau', (-4, 29, -8), (4, 37, -7), CG, base=4, hide={'south'})
    m.lueur('body', 'noyau_coeur', (-2, 31, -9), (2, 35, -8), ('uni', 'glace'), base=5.2, hide={'south'})
    p.bone('battant_g', (12, 32, -9), 'body')
    p.bone('battant_d', (-12, 32, -9), 'body')
    n = m.sb('battant_g|battant_d', 'battant', (3, 26, -10), (12, 39, -8), PL, base=4, rivets=3)
    # epaules et cristaux
    m.c('nuque_roc', (-7, 37, -5), (7, 41, 6), RO, base=3)
    amas(m, 'body', 0, 40, 5, ((0, 0, 11, 2, 22, 0), (4, 1, 8, 1, 26, -10), (-4, 1, 8, 1, 26, 10)))
    amas(m, 'body', 0, 32, 6, ((0, 0, 9, 2, 66, 0), (5, 0, 7, 1, 62, -16), (-5, 0, 7, 1, 62, 16)))
    amas(m, 'body', 0, 24, 5, ((3, 0, 6, 1, 74, -8), (-3, 0, 6, 1, 74, 8)))

    # --- petite tete enfoncee, arcade, fente de glace
    m.b = 'head'
    n = m.c('tete', (-5, 33, -12), (5, 41, -3), RO, base=4)
    m.c('arcade', (-6, 38, -13), (6, 40, -11), PL, base=3.4, bord=0)
    m.lueur('head', 'fente', (-4, 36, -13), (4, 38, -12), CG, base=4.6, hide={'south'})
    m.c('machoire', (-4, 32, -13), (4, 35, -10), RO, base=3.2)
    m.pointe('head', 'corne', 3, 41, -8, 7, 1, CG, inc=(-10, -14), base=2.2)
    m.pointe('head', 'corne', -3, 41, -8, 5, 1, CG, inc=(-10, 14), base=2)

    # --- bras immenses
    m.sb('arm_-1|arm_1', 'epaule', (11, 28, -7), (20, 39, 7), PL, base=3.8, rivets=4)
    m.sb('arm_-1|arm_1', 'bras', (12, 21, -5), (19, 29, 5), RO, base=3.4)
    amas(m, 'arm_-1', 15, 39, 0, ((0, 0, 12, 2, 0, -10), (3, -4, 8, 1, -16, -22), (-3, 3, 9, 1, 14, 4), (3, 4, 6, 1, 18, -24)))
    amas(m, 'arm_1', -15, 39, 0, ((0, 0, 9, 2, 0, 10), (-3, -4, 7, 1, -16, 22), (3, 3, 7, 1, 14, -4)))
    m.sb('avbras_-1|avbras_1', 'avantbras', (11, 8, -5), (19, 22, 5), RO, base=3.6)
    m.sb('avbras_-1|avbras_1', 'canon', (10, 12, -6), (20, 18, 6), PL, base=3.2, bord=1)
    amas(m, 'avbras_-1', 20, 15, 0, ((0, -3, 7, 1, 0, -62), (0, 3, 6, 1, 0, -70)))
    amas(m, 'avbras_1', -20, 15, 0, ((0, -3, 7, 1, 0, 62), (0, 3, 6, 1, 0, 70)))
    m.sb('avbras_-1|avbras_1', 'poing', (10, 1, -7), (20, 9, 6), RO, base=4)
    m.sb('avbras_-1|avbras_1', 'phalanges', (10, 2, -8), (20, 6, -7), PL, base=4.4, bord=0, hide={'south'})

    # ------------------------------------------------------------------ repos : immobile comme une falaise, la poitrine respire a peine, la tete grince d'un cote a l'autre
    T = 5.0
    cycle(p, 'idle', T, {
        'body': {'rotation': keys(T, lambda q: (sin(.8, 1, .1)(q), sin(1, 1, 0)(q), 0), 16), 'position': keys(T, lambda q: (0, sin(.25, 1, .1)(q), 0), 16)},
        'head': {'rotation': [(0, 0, 0, 0), (1.2, 0, 0, 0), (1.8, -1, 14, 0), (2.6, -1, 14, 0), (3.2, 0, -10, 0), (4.2, 0, -10, 0), (T, 0, 0, 0)]},
        'battant_g': R(T, y=sin(-1.2, 1, .1, bias=-1.2)),
        'battant_d': R(T, y=sin(1.2, 1, .1, bias=1.2)),
        'arm_-1': R(T, x=sin(.8, 1, .15), z=sin(.6, 1, .35)),
        'arm_1': R(T, x=sin(.8, 1, .15), z=sin(-.6, 1, .35)),
    })

    # ------------------------------------------------------------------ deplacement : gorille : buste qui tourne, grands bras qui balancent et presque au sol, chaque pas fait sursauter la tete
    T = 1.9
    amp = 18
    u0, l0 = jambe(T, amp, 0, 26, reach=.2)
    u1, l1 = jambe(T, amp, .5, 26, reach=.2)
    choc = combine(pulse(1, .02, .05), pulse(1, .52, .05))
    cycle(p, 'walk', T, {
        'root': {'position': keys(T, lambda q: (sin(2, 1, .25)(q), appui(T, amp, 16)(q) - .7 * choc(q), 0), 28)},
        'body': R(T, x=sin(1.2, 2, .1, bias=-7), y=sin(14, 1, 0), z=sin(5, 1, .25)),
        'head': R(T, x=combine(sin(1, 2, .1, bias=5), lambda q: -5 * choc(q)), y=sin(-10, 1, 0), z=sin(-4, 1, .25), n=28),
        'leg_-1': u0, 'tibia_-1': l0, 'leg_1': u1, 'tibia_1': l1,
        'arm_-1': R(T, x=sin(-22, 1, 0, bias=8), z=sin(2, 2, .1, bias=2)),
        'avbras_-1': R(T, x=sin(12, 1, .2, bias=10)),
        'arm_1': R(T, x=sin(22, 1, 0, bias=8), z=sin(-2, 2, .1, bias=-2)),
        'avbras_1': R(T, x=sin(-12, 1, .2, bias=10)),
    })

    # ------------------------------------------------------------------ attaque : les deux poings montent ensemble au-dessus de la tete et s'abattent, tout le corps tombe avec eux
    clip(p, 'attack', 1.0, [
        (.3, {'root+': (0, 2, 2), 'body': (14, 0, 0), 'head': (10, 0, 0), 'arm_-1': (164, 0, -10), 'avbras_-1': (30, 0, 0), 'arm_1': (164, 0, 10), 'avbras_1': (30, 0, 0)}),
        (.48, {'root+': (0, -5, -5), 'body': (-34, 0, 0), 'head': (22, 0, 0), 'arm_-1': (54, 0, -8), 'avbras_-1': (6, 0, 0), 'arm_1': (54, 0, 8), 'avbras_1': (6, 0, 0), 'leg_-1': (30, 0, 0), 'tibia_-1': (-30, 0, 0), 'leg_1': (10, 0, 0), 'tibia_1': (-28, 0, 0)}),
        (.64, {'root+': (0, -4.6, -4.6), 'body': (-30, 0, 0), 'head': (20, 0, 0), 'arm_-1': (46, 0, -6), 'avbras_-1': (8, 0, 0), 'arm_1': (46, 0, 6), 'avbras_1': (8, 0, 0), 'leg_-1': (26, 0, 0), 'tibia_-1': (-28, 0, 0), 'leg_1': (8, 0, 0), 'tibia_1': (-24, 0, 0)}),
        (.86, {'root+': (0, -.8, -.8), 'body': (-6, 0, 0), 'head': (4, 0, 0), 'arm_-1': (10, 0, -2), 'avbras_-1': (4, 0, 0), 'arm_1': (10, 0, 2), 'avbras_1': (4, 0, 0), 'leg_-1': (5, 0, 0), 'tibia_1': (-4, 0, 0)}),
    ])

    # ------------------------------------------------------------------ sort : les battants claquent et s'ouvrent, les bras s'ecartent, le noyau rayonne et tout le corps vibre ; puis le poitrail se referme
    clip(p, 'cast', 1.8, [
        (.16, {'root+': (0, -1.5, 0), 'body': (-8, 0, 0), 'head': (-10, 0, 0), 'arm_-1': (30, -18, -8), 'avbras_-1': (34, 0, 0), 'arm_1': (30, 18, 8), 'avbras_1': (34, 0, 0)}),
        (LANCER, {'root+': (0, 1, 0), 'body': (10, 0, 0), 'head': (20, 0, 0), 'battant_g': (0, -96, 0), 'battant_d': (0, 96, 0), 'arm_-1': (30, 0, 56), 'avbras_-1': (-6, 0, 0), 'arm_1': (30, 0, -56), 'avbras_1': (-6, 0, 0)}),
        (.5, {'root+': (0, 1.3, 0), 'body': (11, 1, 1), 'head': (22, 2, 1), 'battant_g': (0, -90, 0), 'battant_d': (0, 90, 0), 'arm_-1': (32, 0, 58), 'avbras_-1': (-4, 0, 0), 'arm_1': (32, 0, -58), 'avbras_1': (-4, 0, 0)}),
        (.7, {'root+': (0, 1, 0), 'body': (10, -1, -1), 'head': (20, -2, -1), 'battant_g': (0, -94, 0), 'battant_d': (0, 94, 0), 'arm_-1': (30, 0, 56), 'avbras_-1': (-6, 0, 0), 'arm_1': (30, 0, -56), 'avbras_1': (-6, 0, 0)}),
        (1.0, {'root+': (0, 1.2, 0), 'body': (11, 1, 1), 'head': (22, 2, 1), 'battant_g': (0, -90, 0), 'battant_d': (0, 90, 0), 'arm_-1': (32, 0, 58), 'avbras_-1': (-4, 0, 0), 'arm_1': (32, 0, -58), 'avbras_1': (-4, 0, 0)}),
        (1.25, {'root+': (0, 0, 0), 'body': (2, 0, 0), 'head': (4, 0, 0), 'battant_g': (0, -18, 0), 'battant_d': (0, 18, 0), 'arm_-1': (10, 0, 12), 'arm_1': (10, 0, -12)}),
        (1.4, {'battant_g': (0, 0, 0), 'battant_d': (0, 0, 0)}),
    ])

    # ------------------------------------------------------------------ mort : le noyau lache : les battants tombent, les bras se detachent et s'ecrasent, le reste s'affaisse en tas
    clip(p, 'death', 1.0, [
        (.2, {'body': (6, 0, 4), 'head': (12, 0, -6), 'battant_g': (0, -30, 0), 'battant_d': (0, 40, 0), 'arm_-1': (10, 0, 10), 'arm_1': (10, 0, -10)}),
        (.6, {'root+': (0, -8, 0), 'body': (-18, 0, 6), 'head': (-20, 0, 10), 'battant_g': (60, -60, 0), 'battant_g+': (0, -14, -3), 'battant_d': (60, 70, 0), 'battant_d+': (0, -14, -3), 'arm_-1': (-10, 0, 34), 'arm_-1+': (3, -8, 0), 'arm_1': (-10, 0, -34), 'arm_1+': (-3, -8, 0), 'leg_-1': (40, 0, -14), 'tibia_-1': (-70, 0, 0), 'leg_1': (36, 0, 14), 'tibia_1': (-66, 0, 0)}),
        (1.0, {'root+': (0, -14, 0), 'body': (-34, 0, 10), 'head': (-38, 0, 22), 'head+': (0, -6, -2), 'battant_g': (88, -60, 0), 'battant_g+': (0, -20, -5), 'battant_d': (88, 70, 0), 'battant_d+': (0, -20, -5), 'arm_-1': (-26, 0, 60), 'arm_-1+': (6, -8, 0), 'arm_1': (-26, 0, -60), 'arm_1+': (-6, -8, 0), 'leg_-1': (60, 0, -22), 'tibia_-1': (-100, 0, 0), 'leg_1': (56, 0, 22), 'tibia_1': (-96, 0, 0)}),
    ], retour=False)
    return p
