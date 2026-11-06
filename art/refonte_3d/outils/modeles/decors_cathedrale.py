#!/usr/bin/env python3
"""Decors de la cathedrale (deco_cat_) : refonte gothique. Marbre clair, or, bois sombre, ardoise, vitraux. Enveloppes des originaux conservees, face avant vers -z."""
import math

from kit import EMBLEME, MEDAILLON, PAL, feston
from kit_decor import AME, ARDOISE, BOIS, BOIS_C, BOIS_N, BRAISES, CF, CH, CIRE, CR, CRISTAL, D, DALLE, EAU, FEUILLE, FER, FER_N, MARBRE, MARBRE_N, OR, PIERRE, PIERRE_C, PIERRE_N, TUILE
from materiaux import stamp

AILE = ['................####', '.............#######', '...........#########', '.........###########', '.......#############', '......##############', '.....###############', '....################', '...#################', '..##################', '..#################.', '.##################.', '.#################..', '.################...', '###.###.####.####...', '##..##...###..###...', '#...#.....#....##...', '#.........#.....#...']


def croix(d, nom, cx, cz, y0, h=10, mat=OR):
    d.c(nom, (cx - 1, y0, cz - 1), (cx + 1, y0 + h, cz + 1), mat)
    d.c(nom + '_t', (cx - 3, y0 + h - 4, cz - 1), (cx - 1, y0 + h - 2, cz + 1), mat)
    d.c(nom + '_t', (cx + 1, y0 + h - 4, cz - 1), (cx + 3, y0 + h - 2, cz + 1), mat)


def ange():
    d = D('deco_cat_ange', 401)
    y = d.socle('socle', 12, 12, [(6, 0), (5, 2), (4, 1)], PIERRE_N)
    d.boite('bague', 9, y, y + 2, 9, OR)
    y += 2
    R = ('peau_tissu', 'marbre')
    d.c('robe', (-9, y, -7), (9, y + 26, 7), R, base=3.6)
    d.c('robe', (-7, y + 26, -6), (7, y + 44, 6), R, base=3.8)
    d.c('ceinture', (-8, y + 44, -7), (8, y + 46, 7), OR)
    d.c('torse', (-7, y + 46, -5), (7, y + 62, 5), R, base=4)
    d.c('mains', (-4, y + 50, -9), (4, y + 56, -5), MARBRE, ton=.4)
    with d.miroir():
        d.c('bras', (7, y + 44, -7), (10, y + 62, -1), R, base=3.8, rot=(14, 0, 0), pivot=(8.5, y + 62, -3))
    d.c('cou', (-2, y + 62, -2), (2, y + 65, 2), MARBRE)
    d.c('tete', (-5, y + 65, -5), (5, y + 75, 5), MARBRE, ton=.5)
    d.c('cheveux', (-6, y + 69, 2), (6, y + 77, 6), MARBRE, ton=.1)
    d.c('aureole', (-7, y + 80, 5), (7, y + 82, 6), OR, ton=.5)
    d.c('aureole', (-7, y + 72, 5), (-5, y + 80, 6), OR, ton=.5)
    d.c('aureole', (5, y + 72, 5), (7, y + 80, 6), OR, ton=.5)
    with d.miroir():
        d.c('aile', (8, y + 22, 3), (32, y + 90, 5), ('plan', 'marbre'), base=4, masque=AILE, rot=(0, -16, 0), pivot=(8, y + 50, 4), **CF)
    return d.fin()


def applique():
    d = D('deco_cat_applique', 402)
    d.c('plaque', (-10, 20, -5), (10, 70, -2), OR, ton=-.5)
    d.c('fond', (-7, 24, -6), (7, 60, -5), ('tissu', 'pourpre'), bord=1, hide={'east', 'west', 'up', 'down', 'south'})
    d.ogive('arc', 58, 8, 12, 2, -6, -2, OR, n=3)
    d.c('console', (-10, 30, -2), (10, 33, 4), OR)
    with d.miroir():
        d.c('bras', (10, 36, -1), (20, 38, 1), OR)
        d.c('bras_m', (19, 38, -1), (21, 44, 1), OR)
        d.c('coupe', (17, 44, -3), (23, 46, 3), OR, ton=.4)
    d.c('bras_c', (-1, 33, 4), (1, 36, 14), OR)
    d.c('coupe', (-3, 36, 9), (3, 38, 15), OR, ton=.4)
    d.bougie('c', 0, 12, 38, 18, r=2)
    d.bougie('a', 20, 0, 46, 13, r=2)
    d.bougie('b', -20, 0, 46, 13, r=2)
    return d.fin()


def arc_brise():
    d = D('deco_cat_arc_brise', 403)
    with d.miroir():
        d.boite('socle', 12, 0, 6, 15, PIERRE_N, cx=43)
        d.colonne('pied', 43, 0, 6, 110, 8, base=PIERRE)
    d.ogive('arc', 110, 43, 74, 16, -12, 12, ('maconnerie', 'pierre'), n=6, assise=8, long=14)
    d.ogive('filet', 110, 35, 60, 2, -7, 7, OR, n=6)
    d.c('cle', (-6, 180, -13), (6, 192, 13), DALLE, ton=.5)
    d.cristal('cle_c', 0, 0, 192, 9, 4)
    return d.fin()


def arcade_cloitre():
    d = D('deco_cat_arcade_cloitre', 404)
    d.c('soubassement', (-83, 0, -11), (85, 8, 11), PIERRE_N)
    for cx in (-72, -24, 24, 74):
        d.colonne('pile', cx, 0, 8, 96, 6, base=PIERRE)
    for cx in (-48, 0, 49):
        d.ogive('arc', 96, 21, 36, 6, -8, 8, MARBRE, n=4, cx=cx)
    d.c('mur', (-83, 136, -9), (85, 170, 9), PIERRE, assise=8, long=16)
    d.c('frise', (-83, 133, -11), (85, 136, 11), OR)
    d.c('corniche', (-83, 170, -11), (85, 174, 11), DALLE, ton=.4)
    for i, x in enumerate(range(-78, 74, 22)):
        d.c('merlon', (x, 174, -9), (x + 12, 189, 9), PIERRE, assise=6, long=12, ton=.2)
    for cx in (-48, 0, 49):
        d.vitrail('oculus', cx - 8, cx + 8, 145, 161, -1, forme='rosace', epais=2)
    return d.fin()


def autel_majeur():
    d = D('deco_cat_autel_majeur', 405)
    y = d.socle('marches', 44, 22, [(5, 0), (5, 3)], PIERRE_N)
    n = d.c('autel', (-34, y, -14), (34, 60, 14), MARBRE)
    d.c('bande', (-35, 30, -15), (35, 36, 15), OR)
    d.rune('bande', 'north', ['oo.VV.oo.VV.oo.VV.oo'], 10, 34)
    d.c('nappe', (-36, 60, -16), (36, 63, 16), ('tissu', 'pourpre'), bord=2)
    d.c('retable', (-30, 63, 4), (30, 100, 14), MARBRE, ton=.3)
    d.c('retable_or', (-32, 100, 3), (32, 104, 15), OR)
    for cx in (-18, 0, 18):
        d.ogive('niche', 72, 6, 10, 2, 3, 5, OR, n=2, cx=cx)
        d.c('niche', (cx - 5, 66, 3), (cx + 5, 80, 4), ('tissu', 'pourpre'), bord=0, **CF)
    with d.miroir():
        d.pinacle('pin', 28, 9, 104, 12, 3, mat=MARBRE)
    croix(d, 'croix', 0, 9, 104, h=12)
    for cx in (-28, -14, 14, 28):
        d.boite('chandelier', 2, 63, 66, 2, OR, cx=cx, cz=-6)
        d.bougie('b', cx, -6, 66, 12, r=1)
    return d.fin()


def baldaquin():
    d = D('deco_cat_baldaquin', 406)
    y = d.socle('socle', 40, 40, [(4, 0), (4, 3)], PIERRE_N)
    d.c('dalle', (-33, y, -33), (33, y + 1, 33), DALLE, ton=.3)
    y += 1
    with d.miroir():
        for z in (-26, 26):
            d.colonne('col', 26, z, y, 104, 5, base=PIERRE)
    for z0, z1 in ((-30, -22), (22, 30)):
        d.ogive('arc', 104, 26, 30, 6, z0, z1, MARBRE, n=4)
    d.ogive_z('arc_l', 104, 26, 30, 6, 22, 30, MARBRE, n=4)
    d.ogive_z('arc_l', 104, 26, 30, 6, -30, -22, MARBRE, n=4)
    # arcs lateraux : memes ogives, cote x, en cubes tournes de 90 degres
    d.c('entablement', (-34, 138, -34), (34, 142, 34), OR)
    d.pyramide('toit', 142, 40, 36, 36, ARDOISE, n=8)
    with d.miroir():
        for z in (-31, 31):
            d.pinacle('pin', 31, z, 142, 19, 3, mat=MARBRE)
    d.boite('epi', 3, 182, 186, 3, OR)
    croix(d, 'croix', 0, 0, 186, h=8)
    d.boite('autel', 11, y, y + 22, 11, MARBRE)
    d.boite('autel_or', 12, y + 22, y + 24, 12, OR)
    d.cristal('relique', 0, 0, y + 24, 16, 4)
    return d.fin()


def balustrade_choeur():
    d = D('deco_cat_balustrade_choeur', 407)
    d.c('soubassement', (-85, 0, -8), (70, 12, 8), PIERRE_N)
    d.c('plinthe', (-85, 12, -7), (70, 16, 7), MARBRE)
    for x0, x1 in ((-75, -39), (-27, 9), (21, 57)):
        d.grille('balustres', x0, x1, 16, 66, 0, pas=9, large=5, traverses=((16, 19), (40, 43)), mat=('marbre', 'marbre'))
    d.c('main', (-85, 66, -7), (70, 72, 7), MARBRE, ton=.4)
    d.c('main_or', (-85, 72, -6), (70, 74, 6), OR)
    for cx in (-79, -33, 15, 64):
        d.boite('pilier', 6, 16, 80, 8, PIERRE, cx=cx)
        d.boite('chap', 6, 80, 84, 7, OR, cx=cx)
        d.pinacle('pin', cx, 0, 84, 29, 5, mat=OR)
    return d.fin()


def banc_nef():
    d = D('deco_cat_banc_nef', 408)
    d.c('marche', (-48, 0, -21), (48, 6, 9), PIERRE_N)
    d.c('assise', (-44, 22, -17), (44, 26, 5), BOIS_C, sens='x')
    d.c('devant', (-44, 6, -15), (44, 22, -11), BOIS_N, sens='x')
    d.c('dossier', (-44, 26, 3), (44, 60, 7), BOIS, sens='x', planche=8)
    d.c('rail', (-44, 60, 2), (44, 64, 8), BOIS_N, sens='x')
    with d.miroir():
        d.c('joue', (44, 6, -19), (48, 64, 9), BOIS_N, sens='y')
        d.c('joue_or', (43, 64, -19), (49, 66, 9), OR)
        d.pinacle('pin', 46, -5, 66, 10, 2, mat=OR)
    d.c('prie_dieu', (-44, 8, -21), (44, 12, -17), ('tissu', 'pourpre'), bord=1)
    return d.fin()


def benitier():
    d = D('deco_cat_benitier', 409)
    y = d.socle('socle', 12, 12, [(3, 0), (3, 2)], PIERRE_N)
    d.fut('pile', 0, 0, y, 26, 5, MARBRE)
    d.boite('bague', 6, 15, 17, 6, OR)
    d.rond('cuve', 0, 0, 26, 32, 11, MARBRE, ton=.2)
    d.margelle('rebord', 0, 0, 32, 41, 15, 4, MARBRE, ton=.3)
    d.margelle('rebord_or', 0, 0, 41, 45, 15, 4, OR)
    d.rond('eau', 0, 0, 32, 37, 11, EAU, hide={'down', 'north', 'south', 'east', 'west'}, no_ao=True)
    return d.fin()


def candelabre():
    d = D('deco_cat_candelabre', 410)
    y = d.socle('pied', 13, 13, [(3, 0), (3, 3), (3, 3)], OR)
    d.fut('tige', 0, 0, y, 72, 2, OR)
    d.boite('noeud', 4, 26, 30, 4, OR, ton=.4)
    d.boite('noeud', 4, 48, 52, 4, OR, ton=.4)
    for i, (x, yb) in enumerate(((10, 66), (20, 58), (30, 50))):
        with d.miroir():
            d.c('bras', (2, yb, -1), (x, yb + 2, 1), OR)
            d.c('bras_m', (x - 1, yb, -1), (x + 1, 80, 1), OR)
            d.boite('coupe', 3, 80, 82, 3, OR, cx=x, ton=.4)
    d.fut('tige_h', 0, 0, 72, 82, 1, OR, chanfrein=0)
    d.boite('coupe', 3, 82, 84, 3, OR, ton=.4)
    d.bougie('c', 0, 0, 84, 14, r=2)
    for x in (10, 20, 30):
        d.bougie('g', x, 0, 82, 15, r=1)
        d.bougie('d', -x, 0, 82, 15, r=1)
    return d.fin()


def candelabre_geant():
    d = D('deco_cat_candelabre_geant', 411)
    y = d.socle('pied', 16, 16, [(4, 0), (3, 3), (3, 3)], OR)
    d.fut('tige', 0, 0, y, 120, 3, OR)
    for yb in (36, 62, 88):
        d.boite('noeud', 5, yb, yb + 5, 5, OR, ton=.4)
    for x, z, yb in ((22, 0, 108), (-22, 0, 108), (0, 20, 108), (0, -20, 108), (12, 0, 92), (-12, 0, 92), (0, 12, 92), (0, -12, 92)):
        ax0, ax1 = sorted((0, x)) if x else (-1, 1)
        az0, az1 = sorted((0, z)) if z else (-1, 1)
        d.c('bras', (ax0, yb, az0), (ax1, yb + 2, az1), OR)
        d.c('bras_m', (x - 1, yb, z - 1), (x + 1, yb + 14, z + 1), OR)
        d.boite('coupe', 3, yb + 14, yb + 16, 3, OR, cx=x, cz=z, ton=.4)
        d.bougie('b', x, z, yb + 16, 12, r=1)
    d.boite('coupe', 4, 120, 122, 4, OR, ton=.4)
    d.bougie('c', 0, 0, 122, 20, r=2)
    return d.fin()


def chaire():
    d = D('deco_cat_chaire', 412)
    y = d.socle('socle', 13, 13, [(4, 0), (4, 2)], PIERRE_N)
    d.fut('pile', 0, 0, y, 40, 6, MARBRE)
    d.boite('console', 11, 40, 46, 11, BOIS_N, sens='x')
    d.rond('plancher', 0, 0, 46, 49, 16, BOIS_N, sens='x')
    d.margelle('cuve', 0, 0, 49, 72, 18, 3, BOIS, sens='y', planche=5)
    d.margelle('cuve_or', 0, 0, 72, 75, 19, 4, OR)
    d.margelle('cuve_or', 0, 0, 58, 60, 19, 2, OR)
    with d.miroir():
        for z in (-14, 14):
            d.boite('colonnette', 1, 75, 108, 1, OR, cx=14, cz=z)
    d.rond('abat_voix', 0, 0, 108, 112, 19, BOIS_N, sens='x')
    d.pyramide('dome', 112, 28, 17, 17, TUILE, n=7)
    d.boite('epi', 2, 140, 143, 2, OR)
    croix(d, 'croix', 0, 0, 143, h=7)
    return d.fin()


def cierge():
    d = D('deco_cat_cierge', 413)
    y = d.socle('pied', 13, 13, [(3, 0), (3, 3), (3, 3)], OR)
    d.fut('tige', 0, 0, y, 26, 3, OR)
    d.boite('noeud', 5, 20, 24, 5, OR, ton=.4)
    d.boite('coupe', 7, 26, 28, 7, OR)
    d.boite('coupe', 6, 28, 32, 6, OR, ton=.4)
    d.fut('cierge', 0, 0, 32, 100, 5, CIRE, base=4.2)
    d.c('cire_haut', (-4, 100, -4), (4, 101, 4), CIRE, base=4.6)
    d.c('coulure', (4, 80, -4), (6, 100, -1), CIRE, base=4.6, hide={'up', 'down'})
    d.c('coulure', (-3, 88, -7), (0, 100, -5), CIRE, base=4.6, hide={'up', 'down'})
    d.c('meche', (-1, 101, -1), (1, 103, 1), ('uni', 'sabot'), base=1)
    d.flamme('c', 0, 0, 103, 5, 17)
    return d.fin()


def clocheton():
    d = D('deco_cat_clocheton', 414)
    y = d.socle('socle', 13, 13, [(4, 0), (4, 2)], PIERRE_N)
    with d.miroir():
        for z in (-8, 8):
            d.boite('col', 2, y, 70, 2, MARBRE, cx=8, cz=z)
    d.fut('coeur', 0, 0, y, 70, 5, PIERRE, chanfrein=1)
    d.boite('entablement', 12, 70, 74, 12, MARBRE, ton=.4)
    d.boite('frise', 11, 74, 76, 11, OR)
    d.c('tambour', (-8, 76, -8), (8, 96, 8), PIERRE, assise=6, long=10)
    with d.miroir():
        for z in (-9, 9):
            d.pinacle('pin', 10, z, 76, 26, 3, mat=MARBRE)
    d.pyramide('fleche', 96, 38, 9, 9, ARDOISE, n=9)
    d.boite('epi', 1, 134, 140, 1, OR)
    return d.fin()


def colonne_torsadee():
    d = D('deco_cat_colonne_torsadee', 415)
    y = d.socle('base', 13, 13, [(4, 0), (3, 2), (3, 2)], PIERRE_N)
    d.boite('bague', 8, y, y + 2, 8, OR)
    y0 = y + 2
    n = 16
    H = 7
    def filet(ctx, face):
        face[:, :2, :3] = (232, 190, 84)
        face[:, 2, :3] = (140, 96, 34)
    for i in range(n):
        a = i * 14 - 105
        yi = y0 + i * H
        t = d.c('tresse', (-6, yi, -6), (6, yi + H, 6), ('marbre', 'marbre'), rot=(0, a, 0), pivot=(0, yi, 0), hide={'up', 'down'})
        for f in ('north', 'east', 'south', 'west'):
            d.p.decal(t, f, filet)
    yt = y0 + n * H
    d.fut('coeur', 0, 0, y0, yt, 4, MARBRE, chanfrein=0)
    d.boite('chap', 8, yt, yt + 3, 8, OR)
    d.boite('chap', 12, yt + 3, yt + 9, 12, OR, ton=.3)
    return d.fin()


def confessionnal():
    d = D('deco_cat_confessionnal', 416)
    d.c('plinthe', (-32, 0, -20), (32, 4, 22), BOIS_N, sens='x')
    d.c('caisse', (-30, 4, -17), (30, 82, 20), BOIS, sens='y', planche=8)
    d.c('porte', (-10, 6, -18), (10, 66, -17), BOIS_C, sens='y', planche=5)
    d.ogive('arc_porte', 66, 11, 14, 2, -19, -17, OR, n=3)
    with d.miroir():
        d.c('volet', (16, 8, -18), (28, 66, -17), BOIS_N, sens='y', planche=4)
        d.c('fente', (19, 44, -19), (25, 56, -18), FER_N, base=1.4, **CF)
    d.c('corniche', (-32, 82, -20), (32, 86, 22), BOIS_N, sens='x')
    d.pyramide('toit', 86, 28, 28, 18, BOIS_N, n=7, ton=.4, sens='x')
    d.c('epi', (-1, 114, 0), (1, 120, 2), OR)
    return d.fin()


def contrefort():
    d = D('deco_cat_contrefort', 417)
    # pile principale a gauche, arc-boutant, culee a droite
    d.c('pile_socle', (-60, 0, -14), (-20, 8, 14), PIERRE_N)
    d.c('pile', (-56, 8, -11), (-24, 130, 11), PIERRE, assise=10, long=16)
    d.c('pile_bande', (-58, 88, -13), (-22, 92, 13), DALLE, ton=.4)
    d.c('pile_haut', (-52, 130, -9), (-28, 160, 9), PIERRE, assise=8, long=14)
    d.pinacle('pin', -40, 0, 160, 41, 9, mat=MARBRE)
    d.c('culee_socle', (24, 0, -13), (54, 6, 13), PIERRE_N)
    d.c('culee', (28, 6, -10), (50, 78, 10), PIERRE, assise=8, long=14)
    d.c('culee_haut', (31, 78, -8), (47, 90, 8), DALLE, ton=.3)
    d.pinacle('pin_b', 39, 0, 90, 26, 6, mat=MARBRE)
    # arc-boutant : voussoirs tournes de la culee a la pile
    for i in range(6):
        t = (i + .5) / 6
        px = int(round(-24 + 52 * t))
        py = int(round(150 - 70 * t))
        d.c('volee', (px - 8, py - 4, -6), (px + 8, py + 4, 6), ('dalle', 'pierre'), pas=8, rot=(0, 0, -math.degrees(math.atan2(70, 52))), pivot=(px, py, 0))
    return d.fin()


def croix_():
    d = D('deco_cat_croix', 418)
    y = d.socle('socle', 16, 22, [(4, 0), (4, 3), (4, 3)], PIERRE_N)
    d.c('fut', (-6, y, -6), (6, 150, 6), OR, ton=-.2)
    d.c('fut_or', (-7, 90, -7), (7, 94, 7), OR, ton=.4)
    d.c('bras', (-36, 100, -6), (-6, 124, 6), OR, ton=-.1)
    d.c('bras', (6, 100, -6), (36, 124, 6), OR, ton=-.1)
    d.c('bout', (-39, 98, -8), (-36, 126, 8), OR, ton=.3)
    d.c('bout', (36, 98, -8), (39, 126, 8), OR, ton=.3)
    d.c('haut', (-8, 150, -8), (8, 153, 8), OR, ton=.3)
    d.boite('pointe', 5, 153, 157, 5, OR, ton=.5)
    d.boite('pointe', 2, 157, 160, 2, OR, ton=.7)
    d.cristal('coeur', 0, -7, 104, 16, 7, angle=0, pointe=False)
    d.cristal('coeur_s', 0, 7, 104, 16, 7, angle=0, pointe=False)
    return d.fin()


def encensoir():
    d = D('deco_cat_encensoir', 419)
    y = d.socle('socle', 11, 11, [(3, 0), (3, 3)], PIERRE_N)
    d.fut('mat', 0, 0, y, 92, 2, FER_N)
    d.boite('bague', 3, 40, 42, 3, OR)
    d.c('potence', (-3, 92, -2), (27, 98, 2), FER_N)
    d.boite('crochet', 2, 88, 92, 2, OR, cx=20)
    o = d.os('encensoir', (20, 88, 0))
    d.c('chaine', (19, 56, -1), (21, 88, 1), FER_N, base=2, ton=.6)
    d.c('couvercle', (15, 52, -5), (25, 56, 5), OR, ton=.3)
    d.c('couvercle', (17, 56, -3), (23, 58, 3), OR, ton=.5)
    d.c('panse', (13, 40, -7), (27, 52, 7), OR)
    d.c('pied', (16, 36, -4), (24, 40, 4), OR, ton=.4)
    d.balance(o, 7, axe='z')
    return d.fin()


def escalier_helicoidal():
    d = D('deco_cat_escalier_helicoidal', 420)
    d.boite('socle', 8, 0, 4, 8, PIERRE_N)
    d.fut('noyau', 0, 0, 4, 126, 5, MARBRE)
    n = 22
    for i in range(n):
        a = ((i * 360 / 12 + 180) % 360) - 180
        y0 = 4 + i * 5
        d.c('marche', (4, y0, -6), (27, y0 + 3, 6), BOIS_N, sens='x', rot=(0, a, 0), pivot=(0, y0, 0))
        if i % 2 == 0:
            d.c('balustre', (24, y0 + 3, -1), (26, y0 + 18, 1), OR, rot=(0, a, 0), pivot=(0, y0, 0))
    d.boite('chap', 7, 126, 130, 7, OR)
    d.pyramide('dome', 130, 12, 8, 8, ARDOISE, n=4)
    return d.fin()


def fleche():
    d = D('deco_cat_fleche', 421)
    y = d.socle('socle', 20, 20, [(5, 0), (4, 3)], PIERRE_N)
    r = 15
    for i, h in enumerate((30, 26, 24, 22, 20, 18, 14)):
        d.boite('etage', r, y, y + h, r, ARDOISE if i % 2 == 0 else PIERRE, ton=i * .08)
        d.boite('frise', r + 1, y + h, y + h + 2, r + 1, OR)
        y += h + 2
        r -= 2
    d.boite('pointe', 1, y, 174, 1, OR)
    croix(d, 'croix', 0, 0, 174, h=10)
    return d.fin()


def fonts_baptismaux():
    d = D('deco_cat_fonts_baptismaux', 422)
    y = d.socle('socle', 17, 17, [(4, 0), (3, 2)], PIERRE_N)
    d.fut('pile', 0, 0, y, 34, 7, MARBRE)
    d.rond('cuve', 0, 0, 34, 40, 15, MARBRE, ton=.2)
    d.margelle('rebord', 0, 0, 40, 58, 18, 4, MARBRE, ton=.3)
    d.margelle('rebord_or', 0, 0, 58, 61, 19, 5, OR)
    d.rond('eau', 0, 0, 40, 50, 14, EAU, hide={'down', 'north', 'south', 'east', 'west'}, no_ao=True)
    with d.miroir():
        for z in (-14, 14):
            d.boite('colonnette', 1, 61, 98, 1, OR, cx=14, cz=z)
    d.rond('dais', 0, 0, 98, 102, 19, BOIS_N, sens='x')
    d.pyramide('dome', 102, 28, 19, 19, BOIS, n=7, sens='x')
    d.boite('epi', 2, 130, 134, 2, OR)
    return d.fin()


def gargouille_geante():
    d = D('deco_cat_gargouille', 423)
    d.p.meta['y_corps'] = (0, 150)
    S = ('marbre', 'cuivre_vert')
    B = dict(base=3)
    y = d.socle('socle', 44, 44, [(7, 0), (6, 6), (5, 6)], PIERRE_N)
    # pattes arriere repliees
    with d.miroir():
        d.c('cuisse', (14, y, 6), (32, y + 42, 36), S, ton=.2, **B)
        d.c('jambe', (16, y, -6), (30, y + 14, 8), S, **B)
        d.c('griffes_ar', (16, y, -12), (30, y + 4, -6), PIERRE, ton=.4)
        # pattes avant
        d.c('bras', (20, y, -32), (31, y + 58, -14), S, ton=.1, rot=(-10, 0, 0), pivot=(25, y + 58, -22), **B)
        d.c('main', (19, y, -42), (32, y + 8, -26), S, **B)
        d.c('griffes_av', (19, y, -46), (32, y + 3, -42), PIERRE, ton=.4)
    d.c('corps', (-20, y + 24, -26), (20, y + 74, 34), S, ton=.1, rot=(-24, 0, 0), pivot=(0, y + 40, 0), **B)
    d.c('poitrail', (-16, y + 34, -34), (16, y + 76, -18), S, ton=.3, **B)
    d.c('ventre', (-12, y + 26, -30), (12, y + 40, -18), S, ton=.3, **B)
    # tete sur un cou court, cornes, gueule
    d.os('head', (0, y + 80, -22))
    d.c('cou', (-10, y + 72, -32), (10, y + 92, -14), S, ton=.2, **B)
    d.c('crane', (-13, y + 86, -48), (13, y + 110, -22), S, ton=.4, **B)
    d.c('arcade', (-14, y + 104, -49), (14, y + 111, -44), S, ton=-.3, **B)
    d.c('museau', (-9, y + 88, -50), (9, y + 98, -48), S, ton=.3, **B)
    d.c('machoire', (-9, y + 78, -50), (9, y + 86, -30), S, **B)
    d.rune('crane', 'north', ['.kkkk......kkkk.', '.kRRk......kRRk.', '.kkkk......kkkk.'], 8, y + 104)
    d.c('dents', (-8, y + 85, -50), (8, y + 88, -49), ('plan', 'ivoire'), base=4, masque=['#.##.##.##.##.#.', '#..#..#..#..#..#'], **CF)
    with d.miroir():
        d.c('corne_a', (8, y + 108, -36), (14, y + 122, -30), S, ton=.5, rot=(18, 0, -14), pivot=(11, y + 108, -33), **B)
        d.c('corne_b', (11, y + 120, -32), (15, y + 134, -28), S, ton=.7, rot=(34, 0, -8), pivot=(13, y + 120, -30), **B)
        d.c('corne_c', (13, y + 130, -26), (15, y + 142, -24), PIERRE, ton=.9, rot=(50, 0, -4), pivot=(14, y + 130, -25))
    d.racine()
    # grandes ailes de pierre : bras, doigts, membrane decoupee
    with d.miroir():
        d.c('aile_bras', (16, y + 66, 8), (30, y + 80, 22), S, ton=.3, rot=(0, 0, 38), pivot=(20, y + 70, 14), **B)
        d.c('aile_avant', (24, y + 84, 10), (90, y + 94, 18), S, ton=.2, rot=(0, -14, 46), pivot=(26, y + 88, 14), **B)
        d.c('aile_plan', (24, y + 16, 14), (86, y + 164, 16), ('plan', 'cuivre_vert'), base=2.6, masque=AILE, rot=(0, -14, 0), pivot=(24, y + 60, 15), **CF)
    d.c('queue', (-7, y, 34), (7, y + 12, 50), S, ton=.1, **B)
    d.c('queue', (-4, y, 50), (4, y + 8, 59), S, ton=.3, **B)
    return d.fin()


def gisant():
    d = D('deco_cat_gisant', 424)
    d.c('socle', (-43, 0, -16), (45, 6, 16), PIERRE_N)
    n = d.c('tombeau', (-40, 6, -13), (42, 32, 13), MARBRE)
    d.c('bande', (-41, 28, -14), (43, 32, 14), OR, ton=-.2)
    d.c('dalle', (-42, 32, -15), (44, 35, 15), MARBRE, ton=.5)
    d.c('drap', (-30, 35, -10), (30, 37, 10), ('tissu', 'pourpre'), bord=2)
    # effigie couchee, tete vers +x
    d.c('coussin', (28, 37, -8), (38, 40, 8), ('tissu', 'rouge'), bord=1)
    d.c('corps', (-22, 37, -6), (26, 43, 6), ('peau_tissu', 'marbre'), base=3.8)
    d.c('pieds', (-28, 37, -5), (-22, 41, 5), MARBRE, ton=.2)
    d.c('tete', (26, 40, -4), (35, 47, 4), MARBRE, ton=.5)
    d.c('mains', (-2, 43, -3), (4, 46, 3), MARBRE, ton=.5)
    d.c('epee', (-20, 43, -1), (14, 44, 1), FER, ton=.4)
    with d.miroir():
        for z in (-12, 12):
            d.boite('flambeau', 2, 35, 40, 2, OR, cx=40, cz=z)
    for x, z in ((40, -12), (40, 12), (-38, -12), (-38, 12)):
        d.flamme('f', x, z, 40, 2, 8)
    return d.fin()


def horloge_cat():
    d = D('deco_cat_horloge', 425)
    y = d.socle('socle', 30, 21, [(6, 0), (5, 3)], PIERRE_N)
    d.c('tour', (-24, y, -15), (24, 134, 15), PIERRE, assise=8, long=14)
    d.c('frise', (-26, 80, -17), (26, 84, 17), DALLE, ton=.4)
    d.c('cadran_or', (-20, 88, -17), (20, 128, -15), OR)
    d.vitrail('cadran', -18, 18, 90, 126, -18, forme='rosace', palette=('#1f2f6b', '#2a55c0', '#e8d9b0', '#16224f'), epais=1)
    o = d.os('aig_h', (0, 108, -19))
    d.c('aiguille_h', (-1, 107, -20), (1, 118, -19), OR, ton=.5, hide={'south'})
    d.mouvants.append(('aiguille', o, 1))
    d.racine()
    o = d.os('aig_m', (0, 108, -20))
    d.c('aiguille_m', (-1, 107, -21), (0, 124, -20), OR, ton=.5, hide={'south'})
    d.mouvants.append(('aiguille', o, 12))
    d.racine()
    d.vitrail('baie', -8, 8, 28, 72, -16, palette=('#c02a4a', '#2a55c0', '#e0a820'))
    d.ogive('arc_baie', 58, 10, 16, 3, -17, -15, MARBRE, n=3)
    d.c('corniche', (-27, 134, -18), (27, 138, 18), DALLE, ton=.4)
    d.pyramide('toit', 138, 30, 24, 16, ARDOISE, n=7)
    with d.miroir():
        for z in (-14, 14):
            d.pinacle('pin', 23, z, 138, 22, 4, mat=PIERRE)
    d.boite('epi', 2, 168, 176, 2, OR)
    return d.fin(T=60.0)


def lutrin_aigle():
    d = D('deco_cat_lutrin_aigle', 426)
    y = d.socle('pied', 13, 13, [(3, 0), (3, 3)], OR)
    d.fut('tige', 0, 0, y, 58, 3, OR)
    d.boite('noeud', 5, 28, 32, 5, OR, ton=.4)
    # aigle : corps, tete crochue, ailes ouvertes qui portent le livre
    d.c('corps', (-7, 58, -8), (7, 76, 8), OR, ton=-.2)
    d.c('cou', (-3, 76, -10), (3, 84, -3), OR, ton=.2)
    d.c('tete', (-4, 84, -16), (4, 92, -6), OR, ton=.4)
    d.c('bec', (-2, 82, -20), (2, 88, -16), OR, ton=.6)
    d.rune('tete', 'east', ['kk'], -10, 90)
    with d.miroir():
        d.c('aile', (7, 62, -14), (25, 66, 12), ('plumes', 'or'), base=3, rot=(28, 0, 8), pivot=(7, 64, 0))
    d.c('livre', (-16, 72, 0), (16, 74, 12), ('tissu', 'pourpre'), bord=1, rot=(28, 0, 0), pivot=(0, 70, 0))
    d.c('pages', (-14, 74, 1), (14, 76, 11), CIRE, base=4.4, rot=(28, 0, 0), pivot=(0, 70, 0))
    return d.fin()


def orgue():
    d = D('deco_cat_orgue', 427)
    d.c('socle', (-51, 0, -14), (51, 8, 12), BOIS_N, sens='x')
    d.c('buffet', (-48, 8, -6), (48, 70, 10), BOIS, sens='y', planche=8)
    d.c('frise', (-51, 70, -8), (51, 76, 12), OR)
    d.c('fond', (-46, 76, 2), (46, 144, 10), BOIS_N, sens='y')
    for i, (x, h) in enumerate(((-40, 44), (-30, 56), (-20, 66), (-10, 74), (0, 80), (10, 74), (20, 66), (30, 56), (40, 44))):
        d.c('tuyau', (x - 4, 76, -4), (x + 4, 76 + h, 2), ('tuyaux', 'fer'), pas=8, ton=.5)
        d.c('bouche', (x - 2, 84, -5), (x + 2, 88, -4), FER_N, base=1, **CF)
        d.c('tuyau_or', (x - 4, 76 + h, -3), (x + 4, 78 + h, 1), OR)
    with d.miroir():
        d.c('tour', (46, 8, -9), (51, 138, 13), BOIS_N, sens='y')
        d.pinacle('pin', 48, 2, 138, 22, 3, mat=OR)
    # console : clavier et pupitre
    d.c('console', (-24, 8, -20), (24, 30, -6), BOIS_N, sens='x')
    n = d.c('clavier', (-22, 30, -18), (22, 32, -10), ('uni', 'cire'), base=4.2)
    d.c('pupitre', (-14, 32, -9), (14, 44, -7), BOIS_N, sens='x')
    return d.fin()


def pilier_faisceau():
    d = D('deco_cat_pilier_faisceau', 428)
    y = d.socle('socle', 21, 21, [(5, 0), (5, 3)], PIERRE_N)
    d.c('coeur', (-11, y, -11), (11, 130, 11), PIERRE, assise=10, long=18, hide={'up', 'down'})
    for x, z in ((14, 0), (-14, 0), (0, 14), (0, -14)):
        d.fut('colonnette', x, z, y, 130, 3, MARBRE, chanfrein=1)
    for yb in (50, 92):
        d.boite('bague', 13, yb, yb + 3, 13, OR, hide={'up', 'down'})
    d.boite('chap', 18, 130, 135, 18, OR)
    d.boite('chap', 20, 135, 140, 20, MARBRE, ton=.4)
    d.cristal('c', 0, 0, 140, 6, 6)
    return d.fin()


def porche():
    d = D('deco_cat_porche', 429)
    d.c('dalle', (-60, 0, -39), (60, 6, 31), PIERRE_N)
    with d.miroir():
        for z in (-30, 22):
            d.colonne('col', 48, z, 6, 112, 7, base=PIERRE)
    d.ogive('arc_av', 112, 44, 56, 10, -36, -24, MARBRE, n=5)
    d.ogive('arc_ar', 112, 44, 56, 10, 16, 28, MARBRE, n=5)
    d.c('poutre', (-54, 110, -36), (-44, 118, 28), MARBRE, ton=.2)
    d.c('poutre', (44, 110, -36), (54, 118, 28), MARBRE, ton=.2)
    d.c('entablement', (-56, 118, -38), (-42, 122, 30), OR)
    d.c('entablement', (42, 118, -38), (56, 122, 30), OR)
    # toit a deux pans sur l'axe z
    A = ARDOISE
    d.s('pan', (0, 164, -40), (60, 168, 31), A, rot=(0, 0, -36), pivot=(0, 166, 0))
    d.c('faitage', (-3, 164, -40), (3, 172, 31), OR, ton=-.2)
    d.c('pignon_av', (-46, 122, -32), (46, 160, -28), PIERRE, assise=8, long=14, hide={'up'})
    d.vitrail('oculus', -14, 14, 128, 156, -33, forme='rosace')
    d.pinacle('pin', 0, -31, 172, 33, 6, mat=MARBRE)
    return d.fin()


def portail():
    d = D('deco_cat_portail', 430)
    d.c('marche', (-65, 0, -17), (65, 6, 15), PIERRE_N)
    d.c('mur_g', (-60, 6, -10), (-32, 160, 10), PIERRE, assise=8, long=16)
    d.c('mur_d', (32, 6, -10), (60, 160, 10), PIERRE, assise=8, long=16)
    d.c('linteau', (-32, 128, -10), (32, 160, 10), PIERRE, assise=8, long=16)
    with d.miroir():
        d.colonne('col', 36, -12, 6, 84, 3, base=PIERRE)
        d.c('contre', (56, 6, -14), (65, 120, 14), PIERRE, assise=8, long=12, ton=.2)
        d.pinacle('pin', 60, 0, 120, 40, 5, mat=MARBRE)
    d.ogive('arc', 84, 36, 62, 8, -14, -6, MARBRE, n=5)
    d.ogive('arc_or', 84, 30, 52, 2, -11, -7, OR, n=5)
    # vantaux et tympan de vitrail
    d.c('vantail', (-28, 6, -5), (0, 84, -3), BOIS, sens='y', planche=6)
    d.c('vantail', (0, 6, -5), (28, 84, -3), BOIS, sens='y', planche=6)
    for y in (24, 60):
        d.c('penture', (-27, y, -6), (-3, y + 3, -5), FER_N)
        d.c('penture', (3, y, -6), (27, y + 3, -5), FER_N)
    d.c('anneau', (-5, 40, -7), (-2, 46, -5), OR)
    d.c('anneau', (2, 40, -7), (5, 46, -5), OR)
    d.vitrail('tympan', -28, 28, 84, 133, -4)
    d.c('fronton', (-60, 160, -11), (60, 164, 11), DALLE, ton=.4)
    d.pyramide('gable', 164, 26, 40, 6, PIERRE, n=6, assise=6, long=12)
    d.pinacle('pin_c', 0, 0, 190, 15, 4, mat=OR)
    return d.fin()


def puits_lumiere():
    d = D('deco_cat_puits_lumiere', 431)
    y = d.socle('socle', 18, 18, [(4, 0), (4, 2)], PIERRE_N)
    d.rond('plinthe', 0, 0, y, y + 4, 16, MARBRE)
    d.margelle('cuve', 0, 0, y + 4, y + 14, 16, 4, MARBRE, ton=.3)
    d.margelle('cuve_or', 0, 0, y + 14, y + 16, 17, 3, OR)
    o = d.os('rayon', (0, y + 4, 0))
    d.fut('lumiere', 0, 0, y + 4, 92, 7, ('uni', 'rar_mythique'), base=4.4, no_ao=True)
    d.mouvants.append(('flotte', o, 0.0, .8))
    d.racine()
    with d.miroir():
        for z in (-12, 12):
            d.boite('colonnette', 1, y + 16, 94, 1, OR, cx=12, cz=z)
    d.rond('dais', 0, 0, 94, 98, 16, ARDOISE)
    d.pyramide('dome', 98, 22, 15, 15, ARDOISE, n=6)
    d.boite('epi', 1, 120, 134, 1, OR)
    return d.fin()


def reliquaire():
    d = D('deco_cat_reliquaire', 432)
    y = d.socle('socle', 26, 22, [(4, 0), (3, 2)], PIERRE_N)
    n = d.c('coffre', (-22, y, -17), (22, 42, 17), ('tissu', 'or'), base=2.6, bord=2)
    with d.miroir():
        for z in (-17, 17):
            d.boite('angle', 2, y, 42, 2, OR, cx=23, cz=z, ton=.3)
    d.c('corniche', (-27, 42, -22), (27, 46, 22), OR, ton=.3)
    d.pyramide('toit', 46, 18, 24, 18, ('tissu', 'or'), n=5, bord=1, base=3)
    with d.miroir():
        for z in (-16, 16):
            d.pinacle('pin', 22, z, 46, 10, 2, mat=OR)
    d.boite('crete', 3, 64, 68, 3, OR)
    croix(d, 'croix', 0, 0, 68, h=14)
    d.rune(n, 'north', EMBLEME, 5, 36)
    return d.fin()


def reliquaire_cristal():
    d = D('deco_cat_reliquaire_cristal', 433)
    y = d.socle('pied', 13, 13, [(3, 0), (3, 2), (3, 3)], OR)
    d.boite('coupe', 9, y, y + 3, 9, OR, ton=.3)
    y += 3
    o = d.os('noyau', (0, y + 24, 0))
    d.c('cristal', (-5, y + 3, -5), (5, y + 45, 5), AME, rot=(0, 45, 0), pivot=(0, y + 24, 0))
    d.c('cristal_p', (-2, y + 45, -2), (2, y + 50, 2), AME, rot=(0, 45, 0), pivot=(0, y + 24, 0), ton=.6)
    d.flottant(o, 1)
    d.racine()
    with d.miroir():
        for z in (-10, 10):
            d.boite('barreau', 1, y, y + 54, 1, OR, cx=10, cz=z, ton=-.2)
    d.boite('toit', 12, y + 54, y + 57, 12, OR, ton=.2)
    d.boite('toit', 8, y + 57, y + 60, 8, OR, ton=.4)
    d.boite('toit', 4, y + 60, y + 63, 4, OR, ton=.6)
    croix(d, 'croix', 0, 0, y + 63, h=9)
    return d.fin()


def rosace():
    d = D('deco_cat_rosace', 434)
    d.c('soubassement', (-46, 0, -10), (46, 8, 6), PIERRE_N)
    d.c('mur_bas', (-40, 8, -6), (40, 34, 3), PIERRE, assise=8, long=16)
    d.mur_troue('mur', -40, 40, 34, 110, -6, 3, 0, 71, 35, PIERRE, assise=8, long=16)
    d.c('mur_haut', (-40, 110, -6), (40, 112, 3), DALLE, ton=.3)
    d.vitrail('rose', -36, 36, 35, 107, -2, forme='rosace', epais=2)
    with d.miroir():
        d.c('contre', (40, 8, -10), (46, 112, 6), PIERRE, assise=8, long=10, ton=.2)
        d.pinacle('pin', 43, -2, 112, 14, 3, mat=PIERRE)
    d.c('couronne', (-40, 112, -7), (40, 116, 4), OR)
    return d.fin()


def stalle():
    d = D('deco_cat_stalle', 435)
    d.c('marche', (-28, 0, -14), (28, 6, 15), PIERRE_N)
    d.c('assise', (-22, 26, -12), (22, 30, 6), BOIS_C, sens='x')
    d.c('devant', (-22, 6, -8), (22, 26, -4), BOIS_N, sens='x', planche=8)
    d.c('dossier', (-24, 6, 6), (24, 100, 12), BOIS, sens='y', planche=7)
    with d.miroir():
        d.c('joue', (22, 6, -14), (28, 44, 8), BOIS_N, sens='y')
        d.c('accoudoir', (21, 44, -14), (29, 47, 8), OR)
        d.colonne('col', 25, -10, 47, 90, 2, fut=BOIS_N, base=BOIS_N)
    d.ogive('dais_arc', 90, 22, 22, 4, -14, -6, BOIS_N, n=3)
    d.c('dais_ciel', (-24, 100, -14), (24, 110, 12), BOIS_N, sens='x')
    with d.miroir():
        d.pinacle('pin', 22, 0, 110, 16, 3, mat=OR)
    d.pinacle('pin_c', 0, 0, 110, 16, 3, mat=OR)
    return d.fin()


def statue_saint():
    d = D('deco_cat_statue_saint', 436)
    d.c('socle', (-22, 0, -10), (22, 6, 10), PIERRE_N)
    # niche : fond, montants, ogive
    d.c('fond', (-16, 6, 4), (16, 76, 8), ('tissu', 'pourpre'), bord=2)
    with d.miroir():
        d.c('montant', (16, 6, -6), (22, 74, 8), PIERRE, assise=8, long=10)
    d.ogive('ogive', 74, 19, 30, 6, -6, 8, PIERRE, n=4, assise=6, long=8)
    d.boite('pointe', 3, 100, 107, 3, OR)
    # saint : robe, mains, tete, aureole
    R = ('peau_tissu', 'pourpre')
    d.c('robe', (-9, 6, -6), (9, 36, 4), R)
    d.c('torse', (-7, 36, -5), (7, 58, 4), R, ton=.3)
    d.c('etole', (-2, 16, -7), (2, 58, -6), OR, **CF)
    d.c('mains', (-4, 42, -8), (4, 48, -5), MARBRE, ton=.4)
    d.c('tete', (-5, 58, -5), (5, 68, 4), MARBRE, ton=.5)
    d.c('aureole', (-8, 56, 3), (8, 72, 4), OR, ton=.4, **CF)
    return d.fin()


def tapisserie():
    d = D('deco_cat_tapisserie', 437)
    d.c('tringle', (-39, 122, -1), (39, 125, 2), OR)
    with d.miroir():
        d.c('pomme', (37, 120, -3), (39, 128, 3), OR, ton=.4)
    o = d.os('tissu', (0, 122, 0))
    n = d.banniere('toile', -34, 34, 10, 122, 0, ramp='pourpre', embleme=False, pointe=False)
    d.rune(n, 'north', EMBLEME, 5, 86)
    d.rune(n, 'south', EMBLEME, -5, 86, flip=True)
    d.c('frange', (-34, 4, 0), (34, 10, 1), ('plan', 'or'), base=4, masque=['#.' * 34, '#.' * 34, '#...' * 17], **CF)
    d.balance(o, 1.2)
    return d.fin()


def triptyque():
    d = D('deco_cat_triptyque', 438)
    d.c('soubassement', (-61, 0, -7), (61, 8, 7), PIERRE_N)
    d.fenetre('baie_c', 0, 8, 17, 74, -5, 5, n=4)
    d.fenetre('baie_g', -38, 8, 12, 62, -4, 4, n=3)
    d.fenetre('baie_d', 38, 8, 12, 62, -4, 4, n=3)
    with d.miroir():
        d.c('contre', (54, 8, -7), (61, 110, 7), MARBRE, ton=.2)
        d.pinacle('pin', 57, 0, 110, 30, 4, mat=PIERRE)
    d.pinacle('pin_c', 0, 0, 108, 40, 4, mat=PIERRE)
    return d.fin()


def trone_eveque():
    d = D('deco_cat_trone_eveque', 439)
    y = d.socle('marches', 30, 22, [(5, 0), (5, 3), (5, 3)], PIERRE_N)
    d.c('assise', (-16, y, -14), (16, y + 16, 8), BOIS_N, sens='x')
    d.c('coussin', (-14, y + 16, -14), (14, y + 20, 6), ('tissu', 'rouge'), bord=2)
    with d.miroir():
        d.c('accoudoir', (14, y + 16, -14), (19, y + 32, 8), BOIS, sens='z')
        d.c('accoudoir_or', (13, y + 32, -15), (20, y + 35, 9), OR)
    d.c('dossier', (-19, y, 8), (19, 132, 16), BOIS, sens='y', planche=7)
    d.c('panneau', (-14, y + 22, 7), (14, 122, 8), ('tissu', 'pourpre'), bord=2, **CF)
    d.ogive('arc', 122, 16, 26, 4, 4, 12, BOIS_N, n=3)
    d.c('frise', (-20, 132, 7), (20, 136, 17), OR)
    with d.miroir():
        d.pinacle('pin', 17, 12, 136, 22, 3, mat=OR)
    d.pinacle('pin_c', 0, 8, 148, 22, 3, mat=OR)
    return d.fin()


def vitrail_cat():
    d = D('deco_cat_vitrail', 440)
    d.c('socle', (-39, 0, -9), (39, 8, 9), PIERRE_N)
    d.c('allege', (-34, 8, -6), (34, 24, 6), MARBRE)
    d.c('allege_or', (-35, 24, -7), (35, 26, 7), OR)
    d.fenetre('baie', 0, 26, 24, 70, -6, 6, n=5, e=6)
    with d.miroir():
        d.c('contre', (34, 8, -8), (39, 120, 8), PIERRE, assise=8, long=10)
        d.pinacle('pin', 36, 0, 120, 30, 3, mat=PIERRE)
    d.boite('fleuron', 3, 142, 150, 3, OR)
    croix(d, 'croix', 0, 0, 150, h=16)
    return d.fin()


DECORS = {'deco_cat_ange': ange, 'deco_cat_applique': applique, 'deco_cat_arc_brise': arc_brise, 'deco_cat_arcade_cloitre': arcade_cloitre, 'deco_cat_autel_majeur': autel_majeur, 'deco_cat_baldaquin': baldaquin,
          'deco_cat_balustrade_choeur': balustrade_choeur, 'deco_cat_banc_nef': banc_nef, 'deco_cat_benitier': benitier, 'deco_cat_candelabre': candelabre, 'deco_cat_candelabre_geant': candelabre_geant,
          'deco_cat_chaire': chaire, 'deco_cat_cierge': cierge, 'deco_cat_clocheton': clocheton, 'deco_cat_colonne_torsadee': colonne_torsadee, 'deco_cat_confessionnal': confessionnal,
          'deco_cat_contrefort': contrefort, 'deco_cat_croix': croix_, 'deco_cat_encensoir': encensoir, 'deco_cat_escalier_helicoidal': escalier_helicoidal, 'deco_cat_fleche': fleche,
          'deco_cat_fonts_baptismaux': fonts_baptismaux, 'deco_cat_gargouille': gargouille_geante, 'deco_cat_gisant': gisant, 'deco_cat_horloge': horloge_cat, 'deco_cat_lutrin_aigle': lutrin_aigle,
          'deco_cat_orgue': orgue, 'deco_cat_pilier_faisceau': pilier_faisceau, 'deco_cat_porche': porche, 'deco_cat_portail': portail, 'deco_cat_puits_lumiere': puits_lumiere,
          'deco_cat_reliquaire': reliquaire, 'deco_cat_reliquaire_cristal': reliquaire_cristal, 'deco_cat_rosace': rosace, 'deco_cat_stalle': stalle, 'deco_cat_statue_saint': statue_saint,
          'deco_cat_tapisserie': tapisserie, 'deco_cat_triptyque': triptyque, 'deco_cat_trone_eveque': trone_eveque, 'deco_cat_vitrail': vitrail_cat}
