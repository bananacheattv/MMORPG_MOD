#!/usr/bin/env python3
"""Decors de la citadelle (deco_) : refonte. Enveloppe et emprise des originaux conservees, face avant vers -z (nord). Le puits est valide et fige : il n'est pas ici."""
from kit import EMBLEME, MEDAILLON, PAL, feston
from kit_decor import AME, ARDOISE, BOIS, BOIS_C, BOIS_N, BRAISES, CF, CH, CIRE, CR, CRISTAL, D, DALLE, EAU, FEUILLE, FER, FER_N, MARBRE, MARBRE_N, OR, PIERRE, PIERRE_C, PIERRE_N, TUILE
from materiaux import stamp


def arche():
    d = D('deco_arche', 301)
    with d.miroir():
        d.boite('socle', 14, 0, 4, 15, PIERRE_N, cx=54)
        d.colonne('pied', 54, 0, 4, 120, 9, base=PIERRE)
    d.arc('arc', 120, 54, 20, -13, 13, ('maconnerie', 'pierre'), n=6, assise=8, long=14)
    d.arc('filet', 120, 43, 2, -8, 8, OR, n=6)
    d.c('cle', (-7, 170, -14), (7, 188, 14), DALLE, ton=.5)
    d.cristal('cle_cristal', 0, 0, 188, 12, 4)
    return d.fin()


def balise_runique():
    d = D('deco_balise_runique', 302)
    y = d.socle('socle', 16, 16, [(3, 0), (3, 2), (3, 2)], PIERRE_N)
    d.fut('tige', 0, 0, y, 38, 3, FER_N)
    for sx in (-1, 1):
        for sz in (-1, 1):
            d.boite('borne', 2, y, 24, 2, PIERRE_N, cx=sx * 9, cz=sz * 9, ton=.4)
            d.boite('borne_rune', 2, 24, 27, 2, AME, cx=sx * 9, cz=sz * 9)
    d.boite('collier', 5, 38, 41, 5, OR)
    o = d.os('noyau', (0, 56, 0))
    n = d.c('cristal', (-5, 43, -5), (5, 69, 5), AME, rot=(0, 45, 0), pivot=(0, 56, 0))
    d.c('cristal_haut', (-3, 69, -3), (3, 73, 3), AME, rot=(0, 45, 0), pivot=(0, 56, 0), ton=.6)
    d.flottant(o, 1.5)
    d.racine()
    d.boite('couronne', 4, 75, 77, 4, FER)
    d.fut('pointe', 0, 0, 77, 96, 1, FER, chanfrein=0)
    d.boite('pointe_or', 1, 96, 100, 1, OR)
    return d.fin()


def banc():
    d = D('deco_banc', 303)
    with d.miroir():
        d.boite('pied', 7, 0, 3, 8, PIERRE, cx=16)
        d.boite('pied', 6, 3, 18, 7, MARBRE, cx=16)
        d.boite('pied_or', 7, 18, 20, 8, OR, cx=16)
        d.c('montant', (20, 24, 3), (24, 44, 8), BOIS_N, sens='y')
    d.c('assise', (-24, 20, -9), (24, 24, 8), BOIS_C, sens='x', planche=5)
    d.c('dossier', (-20, 24, 4), (20, 42, 7), BOIS, sens='x', planche=6)
    d.c('dossier_haut', (-24, 44, 2), (24, 46, 9), OR)
    d.c('dossier_rail', (-20, 42, 3), (20, 44, 8), BOIS_N, sens='x')
    return d.fin()


def bassin():
    d = D('deco_bassin', 304)
    d.boite('plinthe', 40, 0, 3, 40, PIERRE_N)
    d.c('fond', (-33, 3, -33), (33, 11, 33), PIERRE, hide={'down'})
    M = dict(ton=.2)
    d.c('rebord_n', (-40, 3, -40), (40, 16, -33), MARBRE, **M)
    d.c('rebord_s', (-40, 3, 33), (40, 16, 40), MARBRE, **M)
    d.s('rebord_e', (33, 3, -33), (40, 16, 33), MARBRE, **M)
    d.c('eau', (-33, 11, -33), (33, 13, 33), EAU, hide={'down', 'north', 'south', 'east', 'west'}, no_ao=True)
    with d.miroir():
        for z in (-36, 36):
            d.boite('angle', 6, 16, 20, 6, DALLE, cx=36, cz=z)
            d.boite('borne', 4, 20, 30, 4, MARBRE, cx=36, cz=z)
            d.boite('borne_or', 5, 30, 32, 5, OR, cx=36, cz=z)
            d.cristal('borne_cristal', 36, z, 32, 6, 2)
    d.boite('ilot', 13, 13, 19, 13, DALLE)
    d.cristal('c_a', 0, 0, 19, 28, 6)
    d.cristal('c_b', -7, 3, 19, 18, 4, incl=(10, 0))
    d.cristal('c_c', 7, -4, 19, 15, 4, incl=(-12, 0))
    d.cristal('c_d', 3, 8, 19, 12, 3, incl=(0, 12))
    return d.fin()


def bibliotheque():
    d = D('deco_bibliotheque', 305)
    d.c('plinthe', (-34, 0, -11), (34, 6, 11), BOIS_N, sens='x')
    d.rayonnage('livres', -30, 30, 6, 116, -8, 8, etage=22, faces=('north', 'south'))
    d.c('montant_mi', (-2, 6, -10), (2, 116, 10), BOIS_N, sens='y')
    for i, y in enumerate((27, 49, 71, 93)):
        d.c('rayon', (-30, y, -10), (-2, y + 2, 10), BOIS, sens='x')
        d.c('rayon', (2, y, -10), (30, y + 2, 10), BOIS, sens='x')
    d.s('montant', (30, 6, -11), (34, 116, 11), BOIS, sens='y')
    d.c('corniche', (-34, 116, -11), (34, 120, 11), BOIS_N, sens='x')
    d.c('fronton', (-30, 120, -9), (30, 124, 9), OR)
    d.c('plaque', (-5, 120, -10), (5, 124, -9), OR, ton=.4)
    return d.fin()


def brasero():
    d = D('deco_brasero', 306)
    with d.miroir():
        for z in (-1, 1):
            d.c('patte', (9, 0, z * 9 - 2), (13, 30, z * 9 + 2), FER_N, rot=(z * 14, 0, 12), pivot=(11, 30, z * 9))
    d.rond('vasque', 0, 0, 28, 33, 10, PIERRE_N)
    d.rond('vasque', 0, 0, 33, 38, 15, FER_N, y=None)
    d.margelle('rebord', 0, 0, 38, 44, 19, 5, FER_N)
    d.margelle('rebord_or', 0, 0, 44, 46, 20, 6, OR)
    d.rond('braises', 0, 0, 38, 42, 15, BRAISES, hide={'down'})
    d.flamme('c', 0, 0, 42, 11, 29)
    d.flamme('a', -7, 4, 42, 6, 17)
    d.flamme('b', 7, -4, 42, 6, 20)
    return d.fin()


def caisses_empilees():
    d = D('deco_caisses_empilees', 307)
    d.caisse('caisse_a', (-23, 0, -21), (5, 22, 7))
    d.caisse('caisse_b', (5, 0, -14), (24, 18, 5), ton=-.4)
    d.caisse('caisse_c', (-12, 0, 7), (14, 20, 28), ton=.2)
    d.caisse('caisse_d', (-18, 22, -18), (2, 42, 2), ton=-.2)
    d.caisse('caisse_e', (6, 18, -11), (20, 30, 3), ton=.3)
    d.c('corde', (-19, 30, -19), (3, 32, 3), ('cordage', 'corde'))
    return d.fin()


def chandelier():
    d = D('deco_chandelier', 308)
    y = d.socle('pied', 10, 10, [(2, 0), (2, 2), (2, 2)], OR)
    d.fut('tige', 0, 0, y, 56, 2, OR)
    d.boite('noeud', 3, 26, 30, 3, OR, ton=.4)
    d.boite('noeud', 3, 44, 47, 3, OR, ton=.4)
    d.c('bras_x', (-11, 56, -1), (11, 58, 1), OR)
    d.c('bras_z', (-1, 56, -11), (1, 58, -1), OR)
    d.c('bras_z', (-1, 56, 1), (1, 58, 11), OR)
    d.boite('coupe', 3, 58, 60, 3, OR, ton=.4)
    d.bougie('c', 0, 0, 60, 11)
    for cx, cz in ((-10, 0), (10, 0), (0, -10), (0, 10)):
        d.boite('coupe', 2, 58, 60, 2, OR, cx=cx, cz=cz, ton=.4)
        d.bougie('b', cx, cz, 60, 8, r=1)
    return d.fin()


def colonne_ornee():
    d = D('deco_colonne_ornee', 309)
    d.boite('plinthe', 12, 0, 4, 12, PIERRE_N)
    d.colonne('col', 0, 0, 4, 94, 6, bague=OR)
    d.boite('abaque', 12, 94, 100, 12, MARBRE, ton=.5)
    with d.miroir():
        d.c('acanthe', (6, 83, -6), (9, 91, -2), OR, ton=.2)
        d.c('acanthe', (6, 83, 2), (9, 91, 6), OR, ton=.2)
    return d.fin()


def cristal_flottant():
    d = D('deco_cristal_flottant', 310)
    y = d.socle('socle', 18, 18, [(3, 0), (3, 3), (2, 3)], PIERRE_N)
    with d.miroir():
        d.c('griffe', (9, y, -2), (12, 30, 2), OR)
        d.c('griffe_haut', (6, 30, -2), (12, 33, 2), OR, ton=.3)
    o = d.os('noyau', (0, 60, 0))
    P = dict(rot=(0, 45, 0), pivot=(0, 60, 0))
    d.c('bas', (-4, 36, -4), (4, 44, 4), AME, **P)
    d.c('corps', (-10, 44, -10), (10, 74, 10), AME, **P)
    d.c('haut', (-6, 74, -6), (6, 84, 6), AME, ton=.5, **P)
    d.c('pointe', (-2, 84, -2), (2, 90, 2), AME, ton=1, **P)
    d.flottant(o, 2)
    return d.fin()


def enclume_geante():
    d = D('deco_enclume_geante', 311)
    d.c('souche', (-14, 0, -11), (14, 20, 11), BOIS_N, sens='y')
    d.c('cerclage', (-15, 15, -12), (15, 17, 12), FER_N)
    d.c('pied', (-12, 20, -9), (12, 24, 9), FER_N)
    d.c('taille', (-8, 24, -6), (8, 31, 6), FER_N, ton=.3)
    d.c('table', (-20, 31, -9), (22, 41, 9), FER_N, ton=.6)
    d.c('dessus', (-18, 41, -8), (20, 42, 8), FER)
    d.c('bec', (-28, 33, -6), (-20, 40, 6), FER_N, ton=.5)
    d.c('bec', (-34, 35, -3), (-28, 39, 3), FER_N, ton=.4)
    d.c('talon', (22, 35, -5), (30, 41, 5), FER_N, ton=.5)
    d.c('marteau_tete', (-6, 42, -4), (6, 46, 2), FER, rot=(0, 20, 0), pivot=(0, 42, 0))
    return d.fin()


def etal_marche():
    d = D('deco_etal_marche', 312)
    d.c('comptoir', (-34, 0, -19), (34, 28, 17), BOIS, sens='x', planche=7)
    d.c('plateau', (-36, 28, -21), (36, 31, 19), BOIS_C, sens='x', planche=6)
    with d.miroir():
        d.c('poteau', (30, 31, -18), (34, 66, -14), BOIS_N, sens='y')
        d.c('poteau', (30, 31, 12), (34, 78, 16), BOIS_N, sens='y')
    n = d.c('auvent', (-36, 70, -22), (36, 73, 22), ('uni', 'rouge'), rot=(-14, 0, 0), pivot=(0, 71, 0))
    d.rayures_tissu(n, ('up', 'down', 'north', 'south'), pas=9)
    v = d.c('lambrequin', (-36, 58, -22), (36, 66, -21), ('uni', 'rouge'), **CF)
    d.rayures_tissu(v, ('north', 'south'), pas=9)
    d._masque_alpha(v, feston(72, 18))
    d.caisse('caisse', (-30, 31, -14), (-14, 41, 2), ton=.3)
    d.c('pommes', (-28, 41, -12), (-16, 44, 0), ('feuilles', 'rouge'), base=3.4)
    d.caisse('caisse', (-8, 31, -12), (8, 39, 2), ton=-.2)
    d.c('poires', (-6, 39, -10), (6, 42, 0), ('feuilles', 'feuillage'), base=3.8)
    d.c('etoffe', (14, 31, -14), (30, 37, 4), ('tissu', 'rar_rare'), bord=1)
    d.c('etoffe', (16, 37, -12), (28, 41, 2), ('tissu', 'rar_mythique'), bord=1)
    return d.fin()


def etendard():
    d = D('deco_etendard', 313)
    y = d.socle('socle', 5, 5, [(3, 0), (2, 1)], PIERRE_N)
    d.fut('hampe', 0, 0, y, 112, 1, BOIS_N, chanfrein=0, sens='y')
    d.c('traverse', (-19, 106, -2), (19, 109, 2), OR)
    d.boite('pomme', 3, 112, 115, 3, OR, ton=.3)
    d.c('pointe', (-1, 115, -1), (1, 120, 1), OR, ton=.6)
    o = d.os('tissu', (0, 106, -3))
    d.banniere('banniere', -14, 14, 28, 106, -3, ramp='rouge')
    d.balance(o, 2.5)
    return d.fin()


def fontaine():
    d = D('deco_fontaine', 314)
    d.rond('fond', 0, 0, 0, 6, 28, PIERRE, hide={'down'})
    d.margelle('rebord', 0, 0, 0, 12, 32, 6, MARBRE)
    d.rond('eau', 0, 0, 6, 9, 26, EAU, hide={'down', 'north', 'south', 'east', 'west'}, no_ao=True)
    d.fut('pile', 0, 0, 9, 42, 4, MARBRE)
    d.rond('coupe', 0, 0, 42, 46, 8, MARBRE, ton=.3)
    d.margelle('coupe_rebord', 0, 0, 46, 50, 13, 3, MARBRE, ton=.5)
    d.rond('coupe_eau', 0, 0, 46, 47, 10, EAU, hide={'down', 'north', 'south', 'east', 'west'}, no_ao=True)
    d.fut('pile_h', 0, 0, 47, 66, 2, MARBRE)
    d.boite('tete', 4, 66, 70, 4, OR)
    d.c('jet', (-1, 70, -1), (1, 82, 1), EAU, no_ao=True)
    with d.miroir():
        d.c('filet', (5, 47, -1), (7, 70, 1), EAU, no_ao=True, rot=(0, 0, -6), pivot=(6, 70, 0))
    d.c('filet_n', (-1, 47, -7), (1, 70, -5), EAU, no_ao=True, rot=(-6, 0, 0), pivot=(0, 70, -6))
    d.c('filet_s', (-1, 47, 5), (1, 70, 7), EAU, no_ao=True, rot=(6, 0, 0), pivot=(0, 70, 6))
    return d.fin()


AILE_PIERRE = ['..............##', '............####', '.........#######', '.......#########', '.....###########', '...#############', '..##############', '.###############', '####.###.####.##', '##...##...##...#', '#....#.....#....']


def gargouille():
    d = D('deco_gargouille', 315)
    S = ('marbre', 'pierre')
    y = d.socle('socle', 15, 15, [(5, 0), (4, 2), (3, 2)], PIERRE_N)
    with d.miroir():
        d.c('patte_ar', (4, y, -2), (9, y + 18, 8), S, base=2.6)
        d.c('patte_av', (5, y, -11), (9, y + 8, -6), S, base=2.6)
        d.c('griffe', (5, y, -13), (9, y + 3, -11), PIERRE, ton=.5)
        d.c('bras', (8, y + 8, -9), (12, y + 32, -4), S, base=2.8, rot=(-8, 0, -8), pivot=(10, y + 32, -6))
    d.c('corps', (-8, y + 14, -7), (8, y + 40, 8), S, base=3, rot=(-14, 0, 0), pivot=(0, y + 14, 0))
    d.os('head', (0, y + 44, -8))
    d.c('tete', (-6, y + 38, -16), (6, y + 50, -5), S, base=3.3)
    d.c('gueule', (-4, y + 38, -21), (4, y + 44, -16), S, base=3)
    d.rune('tete', 'north', ['.kk....kk.', '.kR....Rk.'], 5, y + 48)
    d.s('corne', (3, y + 50, -11), (6, y + 58, -7), PIERRE, rot=(16, 0, -12), pivot=(4.5, y + 50, -9), ton=.6)
    d.racine()
    with d.miroir():
        n = d.c('aile', (8, y + 22, 3), (34, y + 60, 4), ('plan', 'pierre'), base=3.2, masque=AILE_PIERRE, rot=(0, -22, 0), pivot=(8, y + 30, 3), **CF)
    return d.fin()


def globe_arcane():
    d = D('deco_globe_arcane', 316)
    y = d.socle('socle', 13, 13, [(3, 0), (3, 3)], PIERRE_N)
    d.fut('tige', 0, 0, y, 22, 2, OR)
    d.boite('coupe', 6, 22, 24, 6, OR, ton=.3)
    o = d.os('globe', (0, 42, 0))
    A = ('cristal', 'amethyste')
    d.boite('g', 9, 24, 27, 9, A)
    d.boite('g', 14, 27, 31, 14, A, ton=.2)
    d.boite('g', 17, 31, 53, 17, A, ton=.4)
    d.boite('g', 14, 53, 57, 14, A, ton=.6)
    d.boite('g', 9, 57, 60, 9, A, ton=.8)
    d.racine()
    o2 = d.os('orbite', (0, 42, 0))
    for i, (x, z) in enumerate(((22, 0), (-22, 0), (0, 22), (0, -22), (15, 15), (-15, 15), (15, -15), (-15, -15))):
        yy = 41 + (i % 2) * 4 - 2
        d.boite('bille', 1, yy, yy + 3, 1, OR, cx=x, cz=z, ton=.4)
    d.tourne(o2, 1)
    return d.fin(T=6.0)


def grille_forgee():
    d = D('deco_grille_forgee', 317)
    with d.miroir():
        d.boite('socle', 7, 0, 6, 7, PIERRE_N, cx=44)
        d.fut('pilier', 44, 0, 6, 140, 5, FER_N)
        d.boite('chap', 6, 140, 143, 6, OR, cx=44)
        d.pinacle('pinacle', 44, 0, 143, 24, 5, mat=FER_N)
    d.grille('grille', -39, 39, 6, 136, 0, pas=8, large=2, traverses=((16, 19), (70, 80), (124, 127)), lances=9)
    d.c('plaque', (-30, 68, -1), (30, 82, 2), OR, ton=-.2)
    d.rune('plaque', 'north', ['.oo..VV..oo.', 'o..oVPPVo..o', '.oo..VV..oo.'], 6, 77)
    return d.fin()


def horloge():
    d = D('deco_horloge', 318)
    y = d.socle('socle', 16, 16, [(5, 0), (3, 2)], PIERRE_N)
    d.c('tour', (-12, y, -12), (12, 118, 12), ('marbre', 'marbre'), ton=-.4)
    d.boite('corniche', 15, 118, 122, 15, DALLE, ton=.4)
    d.pyramide('toit', 122, 30, 15, 15, TUILE, n=6)
    d.boite('epi', 1, 152, 158, 1, OR)
    d.c('boitier', (-11, 92, -24), (11, 116, -12), BOIS_N, sens='z')
    d.c('cadran', (-10, 93, -26), (10, 115, -24), OR)
    d.c('fond', (-8, 95, -27), (8, 113, -26), ('uni', 'cire'), base=4.4)
    d.rune('fond', 'north', ['.......kk.......', '..k..........k..', '................', 'k..............k', '................', '..k..........k..', '.......kk.......'], 8, 110)
    o = d.os('aiguille_h', (0, 104, -27))
    d.c('aig_h', (-1, 103, -28), (1, 110, -27), FER_N, base=1, hide={'south'})
    d.mouvants.append(('aiguille', o, 1))
    d.racine()
    o = d.os('aiguille_m', (0, 104, -28))
    d.c('aig_m', (-1, 103, -29), (0, 113, -28), FER_N, base=1, hide={'south'})
    d.mouvants.append(('aiguille', o, 12))
    d.racine()
    return d.fin(T=60.0)


def lampadaire():
    d = D('deco_lampadaire', 319)
    y = d.socle('socle', 8, 8, [(2, 0), (2, 1), (2, 1), (2, 1)], PIERRE)
    d.fut('mat', 0, 0, y, 62, 2, FER_N)
    d.boite('bague', 3, 16, 18, 3, OR)
    d.boite('bague', 3, 36, 38, 3, OR)
    d.boite('porte', 6, 62, 65, 6, OR)
    d.fut('verre', 0, 0, 65, 86, 5, ('uni', 'cire'), chanfrein=0, base=4.4, no_ao=True)
    with d.miroir():
        d.c('montant', (5, 65, -6), (6, 86, -5), OR)
        d.c('montant', (5, 65, 5), (6, 86, 6), OR)
    d.boite('chapeau', 7, 86, 89, 7, OR)
    d.boite('chapeau', 5, 89, 92, 5, OR, ton=.3)
    d.boite('chapeau', 3, 92, 95, 3, OR, ton=.5)
    d.boite('epi', 1, 95, 100, 1, OR, ton=.7)
    return d.fin()


def lanterne_potence():
    d = D('deco_lanterne_potence', 320)
    y = d.socle('socle', 6, 6, [(3, 0), (3, 2)], PIERRE)
    d.fut('poteau', 0, 0, y, 82, 2, BOIS_N, chanfrein=0, sens='y')
    d.boite('tete', 3, 78, 82, 3, FER_N, base=2)
    d.c('bras', (2, 76, -2), (30, 80, 2), BOIS_N, sens='x')
    d.c('jambe', (2, 56, -1), (4, 76, 1), FER_N, rot=(0, 0, -40), pivot=(3, 56, 0))
    o = d.os('lanterne', (27, 76, 0))
    d.c('chaine', (26, 66, -1), (28, 76, 1), FER_N, base=2)
    d.c('toit', (21, 62, -6), (33, 66, 6), FER_N, ton=.4)
    d.fut('verre', 27, 0, 46, 62, 4, ('uni', 'cire'), chanfrein=0, base=4.4, no_ao=True)
    d.c('fond', (22, 43, -5), (32, 46, 5), FER_N)
    for x, z in ((22, -5), (31, -5), (22, 4), (31, 4)):
        d.c('barreau', (x, 46, z), (x + 1, 62, z + 1), FER_N)
    d.balance(o, 3, axe='z')
    return d.fin()


def obelisque():
    d = D('deco_obelisque', 321)
    y = d.socle('socle', 17, 17, [(3, 0), (3, 3), (3, 3)], PIERRE)
    n = d.c('fut_1', (-9, y, -9), (9, 38, 9), MARBRE)
    n2 = d.c('fut_2', (-8, 38, -8), (8, 68, 8), MARBRE, ton=.1)
    d.c('fut_3', (-7, 68, -7), (7, 98, 7), MARBRE, ton=.2)
    d.c('fut_4', (-6, 98, -6), (6, 122, 6), MARBRE, ton=.3)
    d.boite('pyr', 5, 122, 126, 5, OR)
    d.boite('pyr', 3, 126, 130, 3, OR, ton=.3)
    d.boite('pyr', 1, 130, 134, 1, OR, ton=.6)
    RUNE = ['.VV.', 'V..V', '.VV.', '.V..', '.VVV']
    for f, fl in (('north', False), ('south', True)):
        d.rune(n, f, RUNE, 2 if f == 'north' else -2, 32, flip=fl)
        d.rune(n2, f, RUNE, 2 if f == 'north' else -2, 60, flip=fl)
    return d.fin()


def ratelier_armes():
    d = D('deco_ratelier_armes', 322)
    with d.miroir():
        d.boite('pied', 8, 0, 4, 10, BOIS_N, cx=25, sens='z')
        d.c('poteau', (21, 4, -4), (29, 86, 4), BOIS, sens='y')
        d.c('tete', (20, 86, -5), (30, 90, 5), OR)
    d.c('traverse', (-21, 70, -3), (21, 75, 3), BOIS_N, sens='x')
    d.c('traverse', (-21, 22, -3), (21, 27, 3), BOIS_N, sens='x')
    # epees et lances suspendues
    for i, x in enumerate((-16, 0, 16)):
        d.c('lame', (x - 2, 12, 3), (x + 2, 66, 4), FER, ton=.5)
        d.c('garde', (x - 6, 66, 2), (x + 6, 69, 5), OR)
        d.c('fusee', (x - 1, 69, 3), (x + 1, 79, 4), BOIS_N, sens='y')
        d.c('pommeau', (x - 2, 79, 2), (x + 2, 82, 5), OR, ton=.4)
    for x in (-8, 8):
        d.c('hampe', (x - 1, 4, -5), (x + 1, 84, -4), BOIS_C, sens='y')
        d.c('fer', (x - 2, 84, -5), (x + 2, 90, -4), FER, ton=.5)
    return d.fin()


def sablier_geant():
    d = D('deco_sablier_geant', 323)
    d.boite('plateau', 16, 0, 4, 16, OR)
    d.boite('plateau', 13, 4, 7, 13, BOIS_N, sens='x')
    d.boite('plateau', 13, 89, 92, 13, BOIS_N, sens='x')
    d.boite('plateau', 16, 92, 96, 16, OR)
    with d.miroir():
        for z in (-12, 12):
            d.boite('colonne', 2, 7, 89, 2, OR, cx=12, cz=z, ton=-.2)
    G = ('uni', 'toison')
    SA = ('uni', 'sable')
    d.boite('sable_b', 9, 7, 20, 9, SA, base=4.4)
    d.boite('verre_b', 9, 20, 28, 9, G, base=4, no_ao=True)
    d.boite('verre_b', 6, 28, 40, 6, G, base=3.6, no_ao=True)
    d.boite('col', 2, 40, 56, 2, G, base=3, no_ao=True)
    d.boite('sable_h', 5, 56, 68, 5, SA, base=4)
    d.boite('sable_h', 9, 68, 77, 9, SA, base=4.4)
    d.boite('verre_h', 9, 77, 89, 9, G, base=4, no_ao=True)
    return d.fin()


def statue_guerrier():
    d = D('deco_statue_guerrier', 324)
    S = ('marbre', 'marbre')
    y = d.socle('socle', 13, 13, [(4, 0), (4, 2), (3, 2), (6, 0)], PIERRE_N)
    with d.miroir():
        d.c('jambe', (2, y, -4), (8, y + 28, 4), S, ton=-.2)
        d.c('pied', (2, y, -7), (8, y + 3, -4), S, ton=-.4)
    d.c('taille', (-8, y + 28, -4), (8, y + 34, 4), S, ton=-.3)
    d.c('torse', (-9, y + 34, -5), (9, y + 56, 5), S)
    with d.miroir():
        d.c('epauliere', (9, y + 50, -6), (14, y + 58, 6), S, ton=.3)
    d.c('bras_d', (9, y + 26, -3), (14, y + 50, 3), S, ton=-.1)
    d.c('bras_g', (-14, y + 26, -3), (-9, y + 50, 3), S, ton=-.1)
    d.c('cou', (-3, y + 56, -3), (3, y + 59, 3), S, ton=-.4)
    d.c('heaume', (-5, y + 59, -6), (5, y + 71, 5), S, ton=.3)
    d.c('visiere', (-4, y + 63, -7), (4, y + 65, -6), PIERRE_N)
    d.c('cimier', (-1, y + 71, -6), (1, y + 77, 6), S, ton=.4)
    d.c('bouclier', (-18, y + 22, -7), (-14, y + 54, 7), PIERRE_N, ton=.5)
    d.c('bouclier_or', (-19, y + 34, -3), (-18, y + 42, 3), OR)
    d.c('lame', (14, y, -2), (16, y + 28, 2), FER, ton=.5)
    d.c('garde', (13, y + 28, -5), (17, y + 30, 5), OR)
    return d.fin()


def statue_mage():
    d = D('deco_statue_mage', 325)
    R = ('peau_tissu', 'pourpre')
    y = d.socle('socle', 13, 13, [(4, 0), (3, 2)], PIERRE_N)
    d.c('robe_bas', (-10, y, -9), (10, y + 20, 9), R)
    d.c('robe_mi', (-8, y + 20, -7), (8, y + 40, 7), R, ton=.2)
    d.c('liseret', (-11, y + 2, -10), (11, y + 4, 10), OR)
    d.c('ceinture', (-9, y + 40, -8), (9, y + 43, 8), OR)
    d.c('torse', (-8, y + 43, -6), (8, y + 60, 6), R, ton=.4)
    d.c('bras_d', (8, y + 38, -4), (13, y + 60, 3), R, ton=.1)
    d.c('bras_g', (-13, y + 42, -4), (-8, y + 60, 3), R, ton=.1)
    d.c('visage', (-5, y + 60, -5), (5, y + 70, 5), MARBRE)
    d.c('barbe', (-4, y + 56, -7), (4, y + 66, -5), MARBRE, ton=.5)
    d.c('rebord', (-9, y + 70, -9), (9, y + 72, 9), ('peau_tissu', 'pourpre'), ton=-.2)
    d.c('chapeau', (-6, y + 72, -6), (6, y + 80, 6), R, ton=.3)
    d.c('chapeau', (-4, y + 80, -3), (4, y + 88, 5), R, ton=.5)
    d.c('chapeau', (-2, y + 88, 0), (2, y + 96, 7), R, ton=.7)
    d.c('baton', (13, y, -2), (15, y + 90, 0), BOIS_N, sens='y')
    d.c('baton_or', (12, y + 88, -3), (16, y + 91, 1), OR)
    d.cristal('baton_cristal', 14, -1, y + 91, 6, 2)
    return d.fin()


def table_banquet():
    d = D('deco_table_banquet', 326)
    d.c('plateau', (-37, 32, -13), (37, 36, 13), BOIS, sens='x', planche=6)
    with d.miroir():
        for x, z in ((31, -9), (31, 9)):
            d.boite('pied', 3, 0, 32, 3, BOIS_N, cx=x, cz=z, sens='y')
    d.c('entretoise', (-28, 12, -2), (28, 16, 2), BOIS_N, sens='x')
    with d.miroir():
        d.c('sabot', (27, 8, -8), (35, 12, 8), BOIS_N, sens='z')
    d.c('chemin', (-30, 36, -7), (30, 37, 7), ('tissu', 'rouge'), bord=1)
    for x, z in ((-22, 0), (22, 0)):
        d.boite('assiette', 4, 37, 38, 4, FER, cx=x, cz=z, ton=.6)
    d.boite('coupe', 2, 37, 38, 2, OR, cx=-10, cz=-3)
    d.boite('coupe', 1, 38, 40, 1, OR, cx=-10, cz=-3, ton=.3)
    d.boite('pain', 3, 37, 39, 2, ('uni', 'bois_clair'), cx=8, cz=2, base=4.4)
    d.boite('fruits', 2, 37, 40, 2, ('feuilles', 'rouge'), cx=-1, cz=0, base=3.4)
    return d.fin()


def tonneau_geant():
    d = D('deco_tonneau_geant', 327)
    B = dict(sens='y', planche=5)
    def tranche(y0, y1, r, a, mat=BOIS, **kw):
        d.c('douve', (-r, y0, -a), (r, y1, a), mat, hide={'up', 'down'}, **kw)
        d.c('douve', (-a, y0, -r), (a, y1, r), mat, hide={'up', 'down'}, **kw)
    d.rond('fond', 0, 0, 0, 2, 15, BOIS_N, sens='x')
    tranche(2, 10, 17, 11, **B)
    tranche(10, 38, 20, 13, **B, ton=.2)
    tranche(38, 46, 17, 11, **B)
    FI = dict(ton=.3)
    d.c('cercle', (-18, 7, -12), (18, 10, 12), FER_N, hide={'up', 'down'}, **FI)
    d.c('cercle', (-12, 7, -18), (12, 10, 18), FER_N, hide={'up', 'down'}, **FI)
    d.c('cercle', (-21, 17, -14), (21, 20, 14), FER_N, hide={'up', 'down'}, **FI)
    d.c('cercle', (-14, 17, -21), (14, 20, 21), FER_N, hide={'up', 'down'}, **FI)
    d.c('cercle', (-21, 29, -14), (21, 32, 14), FER_N, hide={'up', 'down'}, **FI)
    d.c('cercle', (-14, 29, -21), (14, 32, 21), FER_N, hide={'up', 'down'}, **FI)
    d.c('cercle', (-18, 38, -12), (18, 41, 12), FER_N, hide={'up', 'down'}, **FI)
    d.c('cercle', (-12, 38, -18), (12, 41, 18), FER_N, hide={'up', 'down'}, **FI)
    d.rond('couvercle', 0, 0, 46, 48, 15, BOIS_C, sens='x', planche=5)
    d.c('robinet', (-2, 12, -24), (2, 15, -20), OR)
    d.c('robinet', (-1, 15, -23), (1, 18, -21), OR, ton=.4)
    return d.fin()


def trone():
    d = D('deco_trone', 328)
    y = d.socle('marches', 21, 21, [(5, 0), (5, 3), (5, 3)], PIERRE_N)
    d.c('assise', (-13, y, -13), (13, y + 12, 7), BOIS_N, sens='x')
    d.c('coussin', (-11, y + 12, -13), (11, y + 16, 5), ('tissu', 'rouge'), bord=2)
    with d.miroir():
        d.c('accoudoir', (11, y + 12, -13), (15, y + 22, 7), OR, ton=-.2)
        d.c('accoudoir_h', (10, y + 22, -14), (16, y + 25, 8), OR)
    d.c('dossier', (-13, y, 7), (13, 118, 13), OR, ton=-.3)
    d.c('panneau', (-10, y + 18, 6), (10, 112, 7), ('tissu', 'pourpre'), bord=2, **CF)
    d.c('fronton', (-15, 118, 6), (15, 122, 14), OR)
    with d.miroir():
        d.pinacle('pinacle', 13, 10, 122, 8, 2, mat=OR)
    d.cristal('joyau', 0, 10, 122, 6, 3)
    return d.fin()


def vase_geant():
    d = D('deco_vase_geant', 329)
    V = ('marbre', 'pourpre')
    def tranche(y0, y1, r, a, mat=V, **kw):
        n = d.c('panse', (-r, y0, -a), (r, y1, a), mat, hide={'up', 'down'}, **kw)
        d.veines_or(n, ('north', 'south', 'east', 'west'))
        n = d.c('panse', (-a, y0, -r), (a, y1, r), mat, hide={'up', 'down'}, **kw)
        d.veines_or(n, ('north', 'south', 'east', 'west'))
    d.boite('pied', 12, 0, 3, 12, OR)
    d.boite('pied', 9, 3, 6, 9, OR, ton=.3)
    tranche(6, 14, 10, 7)
    d.boite('panse_bas', 13, 14, 20, 10, V, hide={'up', 'down'})
    d.boite('panse_bas', 10, 14, 20, 13, V, hide={'up', 'down'})
    tranche(20, 38, 15, 11, ton=.3)
    d.c('bague', (-16, 28, -12), (16, 30, 12), OR, hide={'up', 'down'})
    d.c('bague', (-12, 28, -16), (12, 30, 16), OR, hide={'up', 'down'})
    tranche(38, 46, 12, 8, ton=.2)
    tranche(46, 56, 7, 5, ton=.1)
    d.boite('levre', 8, 56, 58, 8, OR)
    d.boite('levre', 11, 58, 62, 11, OR, ton=.3)
    d.c('fond', (-9, 62, -9), (9, 63, 9), ('uni', 'pourpre'), base=.5, hide={'down', 'north', 'south', 'east', 'west'})
    return d.fin()


DECORS = {'deco_arche': arche, 'deco_balise_runique': balise_runique, 'deco_banc': banc, 'deco_bassin': bassin, 'deco_bibliotheque': bibliotheque, 'deco_brasero': brasero,
          'deco_caisses_empilees': caisses_empilees, 'deco_chandelier': chandelier, 'deco_colonne_ornee': colonne_ornee, 'deco_cristal_flottant': cristal_flottant,
          'deco_enclume_geante': enclume_geante, 'deco_etal_marche': etal_marche, 'deco_etendard': etendard, 'deco_fontaine': fontaine, 'deco_gargouille': gargouille,
          'deco_globe_arcane': globe_arcane, 'deco_grille_forgee': grille_forgee, 'deco_horloge': horloge, 'deco_lampadaire': lampadaire, 'deco_lanterne_potence': lanterne_potence,
          'deco_obelisque': obelisque, 'deco_ratelier_armes': ratelier_armes, 'deco_sablier_geant': sablier_geant, 'deco_statue_guerrier': statue_guerrier, 'deco_statue_mage': statue_mage,
          'deco_table_banquet': table_banquet, 'deco_tonneau_geant': tonneau_geant, 'deco_trone': trone, 'deco_vase_geant': vase_geant}
