#!/usr/bin/env python3
"""Decors de la citadelle, serie Blockbench (decob_) et decors animes : refonte. Enveloppes des originaux conservees, face avant vers -z."""
import math

import numpy as np

from kit import EMBLEME, MEDAILLON, PAL, feston, losange
from kit_decor import AME, ARDOISE, BOIS, BOIS_C, BOIS_N, BRAISES, CF, CH, CIRE, CR, CRISTAL, D, DALLE, EAU, FEUILLE, FEUILLE_N, FER, FER_N, MARBRE, MARBRE_N, OR, PIERRE, PIERRE_C, PIERRE_N, TUILE
from materiaux import stamp

def _roue(n):
    r = n / 2
    lignes = []
    for j in range(n):
        l = ''
        for i in range(n):
            x, y = i + .5 - r, j + .5 - r
            d = math.hypot(x, y)
            a = (math.atan2(y, x) + math.pi) / (math.pi / 3)
            l += '#' if (r - 2.2 <= d < r) or (d < r and abs(a - round(a)) * d * (math.pi / 3) < 1) or d < 2.5 else '.'
        lignes.append(l)
    return lignes


ROUE_X = _roue(16)
FLEURS = ('#e0406a', '#f2c230', '#e8e4f0', '#8a5ad8', '#e8732a')
PIERRE_L = ('marbre', 'pierre')
VERRE = ('uni', 'cire')


def touffe(d, nom, cx, cz, y0, r, h, mat=FEUILLE, **kw):
    """Masse de feuillage bosselee : trois volumes decales, pas de cube parfait."""
    d.c(nom, (cx - r, y0, cz - r + 2), (cx + r, y0 + h - 2, cz + r - 2), mat, **kw)
    d.c(nom, (cx - r + 2, y0 + 1, cz - r), (cx + r - 2, y0 + h - 3, cz + r), mat, **kw)
    d.c(nom, (cx - r + 3, y0 + h - 2, cz - r + 3), (cx + r - 3, y0 + h, cz + r - 3), mat, **kw)


def fleurs_sur(d, nom_cube, faces=('up', 'north', 'south', 'east', 'west'), densite=.05, seed=5):
    """Petites fleurs semees au hasard : coeur jaune, quatre petales, couleurs tirees par face."""
    pal = [tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) for c in FLEURS]

    def fn(ctx, face):
        H, W = face.shape[:2]
        rng = ctx.rng
        n = int(H * W * densite / 4)
        for _ in range(n):
            y, x = int(rng.integers(1, max(2, H - 1))), int(rng.integers(1, max(2, W - 1)))
            c = pal[int(rng.integers(0, len(pal)))]
            face[y - 1, x, :3] = c
            face[y + 1 if y + 1 < H else y, x, :3] = c
            face[y, x - 1, :3] = c
            face[y, x + 1 if x + 1 < W else x, :3] = c
            face[y, x, :3] = (250, 226, 110)
    for f in faces:
        d.p.decal(nom_cube, f, fn)


def arbre_cristal():
    d = D('decob_arbre_cristal', 501)
    d.c('souche', (-8, 0, -8), (8, 4, 8), BOIS_N, sens='y')
    d.fut('tronc', 0, 0, 4, 40, 5, BOIS_N, sens='y')
    with d.miroir():
        d.c('branche', (4, 32, -2), (18, 36, 2), BOIS_N, sens='x', rot=(0, 0, 18), pivot=(4, 34, 0))
    touffe(d, 'feuillage', 0, 0, 36, 26, 18)
    touffe(d, 'feuillage', -3, 2, 54, 18, 14, ton=.3)
    touffe(d, 'feuillage', 2, -2, 68, 10, 9, ton=.6)
    d.cristal('c_a', 27, 8, 40, 11, 3)
    d.cristal('c_b', -28, -6, 42, 10, 3)
    d.cristal('c_c', 10, -19, 52, 9, 3, incl=(0, -18))
    d.cristal('c_d', -6, 15, 64, 9, 2)
    d.cristal('c_e', 5, 5, 16, 8, 2, incl=(-14, 0))
    return d.fin()


def arbre_nain():
    d = D('decob_arbre_nain', 502)
    d.c('souche', (-10, 0, -10), (10, 3, 10), BOIS_N, sens='y')
    d.fut('tronc', 0, 0, 3, 24, 4, BOIS_N, sens='y')
    d.c('tronc_haut', (-3, 24, -3), (7, 38, 5), BOIS_N, sens='y', rot=(0, 0, -14), pivot=(2, 24, 1))
    touffe(d, 'feuillage', 2, -2, 14, 22, 12, FEUILLE_N)
    touffe(d, 'feuillage', 6, -3, 36, 26, 16)
    touffe(d, 'feuillage', 10, -6, 52, 16, 12, ton=.4)
    return d.fin()


def armoire_geante():
    d = D('decob_armoire_geante', 503)
    d.c('plinthe', (-27, 0, -19), (27, 5, 17), BOIS_N, sens='x')
    d.c('caisse', (-24, 5, -16), (24, 70, 15), BOIS, sens='y', planche=8)
    with d.miroir():
        d.c('vantail', (1, 8, -17), (22, 66, -16), BOIS_C, sens='y', planche=5)
        d.c('moulure', (3, 10, -18), (20, 13, -17), BOIS_N, **CF)
        d.c('moulure', (3, 60, -18), (20, 63, -17), BOIS_N, **CF)
        d.c('poignee', (2, 34, -19), (4, 42, -17), OR)
        d.c('penture', (19, 18, -18), (22, 21, -16), FER_N)
        d.c('penture', (19, 52, -18), (22, 55, -16), FER_N)
    d.c('corniche', (-27, 70, -19), (27, 74, 17), BOIS_N, sens='x')
    d.c('corniche_or', (-25, 74, -17), (25, 77, 15), OR)
    d.c('fronton', (-10, 77, -16), (10, 81, 14), OR, ton=.4)
    return d.fin()


def armure_exposition():
    d = D('decob_armure_exposition', 504)
    A = ('metal', 'fer_noir')
    y = d.socle('socle', 16, 16, [(4, 0), (3, 2)], BOIS_N, sens='x')
    with d.miroir():
        d.c('jambe', (2, y, -4), (8, y + 26, 4), A, ton=.6)
        d.c('genouillere', (1, y + 12, -5), (9, y + 16, 5), A, ton=1.2)
        d.c('epauliere', (9, y + 54, -6), (16, y + 62, 6), A, ton=1.4)
    d.c('taille', (-8, y + 26, -4), (8, y + 32, 4), A, ton=.4)
    d.c('plastron', (-10, y + 32, -6), (10, y + 56, 5), A, ton=1)
    d.c('liseret', (-1, y + 32, -7), (1, y + 56, -6), OR, **CF)
    d.c('bras_d', (10, y + 28, -3), (15, y + 54, 3), A, ton=.6)
    d.c('bras_g', (-15, y + 28, -3), (-10, y + 54, 3), A, ton=.6)
    d.c('cou', (-3, y + 56, -3), (3, y + 60, 3), A)
    d.c('heaume', (-5, y + 60, -6), (5, y + 73, 5), A, ton=1.2)
    d.c('fente', (-4, y + 66, -7), (4, y + 68, -6), ('uni', 'sabot'), base=0, **CF)
    d.c('cimier', (-1, y + 73, -6), (1, y + 79, 5), ('tissu', 'rouge'), bord=0)
    # epee posee, pointe au sol
    d.c('lame', (20, y, -2), (23, y + 44, 2), FER, ton=.6)
    d.c('garde', (15, y + 44, -2), (29, y + 47, 2), OR)
    d.c('fusee', (20, y + 47, -1), (23, y + 55, 1), BOIS_N)
    d.c('pommeau', (19, y + 55, -2), (24, y + 58, 2), OR, ton=.4)
    return d.fin()


def autel_lumiere():
    d = D('decob_autel_lumiere', 505)
    y = d.socle('socle', 26, 26, [(4, 0), (3, 3)], ('tissu', 'or'), bord=1)
    n = d.c('autel', (-16, y, -16), (16, 46, 16), ('tissu', 'or'), base=2.6, bord=2)
    d.rune(n, 'north', EMBLEME, 5, 36)
    d.c('table', (-20, 46, -20), (20, 50, 20), ('tissu', 'or'), base=3.4, bord=2)
    d.c('nappe', (-14, 50, -14), (14, 52, 14), MARBRE, ton=.6)
    d.fut('tige', 0, 0, 52, 72, 2, MARBRE, chanfrein=0, ton=.7)
    d.c('traverse', (-10, 74, -2), (10, 78, 2), MARBRE, ton=.7)
    d.c('haut', (-2, 78, -2), (2, 90, 2), MARBRE, ton=.7)
    d.c('milieu', (-2, 72, -2), (2, 74, 2), OR)
    return d.fin()


def autel_runique():
    d = D('decob_autel_runique', 506)
    y = d.socle('marches', 35, 35, [(5, 0), (5, 5), (5, 5)], PIERRE_N)
    with d.miroir():
        for z in (-16, 16):
            d.boite('stele', 4, y, 52, 4, PIERRE, cx=16, cz=z, assise=8, long=8)
            d.boite('stele_h', 5, 52, 55, 5, DALLE, cx=16, cz=z, ton=.3)
    d.boite('podium', 9, y, y + 8, 9, DALLE, ton=.2)
    d.cristal('statue', 0, 0, y + 8, 40, 6)
    d.cristal('statue_a', -5, 3, y + 8, 22, 3, incl=(10, 0))
    d.cristal('statue_b', 5, -3, y + 8, 18, 3, incl=(-12, 0))
    return d.fin()


def baliste():
    d = D('decob_baliste', 507)
    d.c('socle', (-20, 0, -18), (20, 8, 18), BOIS_N, sens='x', planche=8)
    d.fut('pivot', 0, 0, 8, 24, 5, BOIS_N, sens='y')
    d.boite('couronne', 7, 22, 25, 7, FER_N)
    d.c('fut', (-7, 25, -42), (7, 34, 32), BOIS, sens='z', planche=7)
    d.c('rainure', (-2, 34, -42), (2, 35, 30), BOIS_N, sens='z')
    d.c('trait', (-1, 35, -40), (1, 37, 8), BOIS_C, sens='z')
    d.c('fer', (-2, 35, -42), (2, 38, -40), FER, ton=.5)
    d.c('arc_c', (-10, 28, -30), (10, 38, -22), FER_N)
    with d.miroir():
        d.c('bras', (10, 30, -30), (38, 36, -24), BOIS, sens='x', rot=(0, 16, 0), pivot=(10, 33, -27))
    d.c('corde', (-30, 32, -12), (30, 34, -11), ('cordage', 'corde'))
    d.c('treuil', (-11, 26, 22), (11, 33, 30), OR)
    with d.miroir():
        d.c('manivelle', (11, 28, 25), (15, 31, 27), FER_N)
    return d.fin()


def balustrade():
    d = D('decob_balustrade', 508)
    d.c('soubassement', (-71, 0, -23), (71, 8, 23), PIERRE_N)
    with d.miroir():
        d.c('tour', (51, 8, -22), (71, 112, 22), MARBRE, ton=-.3)
        d.boite('tour_cap', 10, 112, 118, 22, DALLE, cx=61, ton=.4)
        d.boite('merlon', 6, 118, 132, 6, MARBRE, cx=61, cz=-14, ton=-.1)
        d.boite('merlon', 6, 118, 132, 6, MARBRE, cx=61, cz=14, ton=-.1)
    d.grille('balustres', -51, 51, 8, 80, 0, pas=17, large=9, traverses=((8, 12),), mat=('marbre', 'marbre'), epais=6)
    d.c('main', (-52, 80, -6), (52, 90, 7), MARBRE, ton=.3)
    d.c('main_or', (-50, 90, -4), (50, 93, 5), OR)
    return d.fin()


def brasero_double():
    d = D('decob_brasero_double', 509)
    y = d.socle('socle', 38, 13, [(4, 0), (3, 2)], PIERRE_N)
    d.c('traverse', (-22, y, -8), (22, y + 8, 8), FER_N, ton=.4)
    for cx, nm in ((27, 'a'), (-27, 'b')):
        d.boite('pied', 8, y, 26, 8, FER_N, cx=cx, ton=.4)
        d.boite('vasque', 11, 26, 36, 11, FER_N, cx=cx, ton=.8)
        d.boite('rebord', 12, 36, 39, 12, OR, cx=cx)
        d.boite('braises', 9, 39, 40, 9, BRAISES, cx=cx, hide={'down', 'north', 'south', 'east', 'west'})
        d.flamme(nm, cx, 0, 40, 9, 40)
    return d.fin()


def buisson_fleuri():
    d = D('decob_buisson_fleuri', 510)
    for i, (x, z, y0, r, h) in enumerate(((0, 0, 0, 32, 26), (-12, 8, 12, 20, 30), (12, -6, 14, 18, 30), (0, 2, 30, 18, 24), (-2, -2, 46, 10, 12))):
        d.c('masse', (x - r + 2, y0 + 1, z - r), (x + r - 2, y0 + h - 2, z + r), FEUILLE, ton=i * .15)
        n = d.c('masse', (x - r, y0, z - r + 3), (x + r, y0 + h - 3, z + r - 3), FEUILLE, ton=i * .15)
        fleurs_sur(d, n, densite=.05, seed=i)
        n = d.c('masse_h', (x - r + 3, y0 + h - 2, z - r + 3), (x + r - 3, y0 + h, z + r - 3), FEUILLE, ton=.2 + i * .15)
        fleurs_sur(d, n, ('up',), densite=.06, seed=i + 5)
    return d.fin()


def bureau_scribe():
    d = D('decob_bureau_scribe', 511)
    with d.miroir():
        d.c('caisson', (17, 0, -15), (31, 32, 15), BOIS, sens='y')
        for y in (6, 18):
            d.c('tiroir', (19, y, -16), (29, y + 9, -15), BOIS_C, sens='x', **CF)
            d.c('bouton', (23, y + 3, -17), (25, y + 5, -16), OR, **CF)
    d.c('plateau', (-33, 32, -17), (33, 36, 17), BOIS_N, sens='x', planche=6)
    d.c('liseret', (-34, 33, -18), (34, 35, 18), OR, hide={'up', 'down'})
    d.c('sous_main', (-14, 36, -14), (14, 37, 4), ('tissu', 'rar_rare'), bord=1)
    d.c('etagere', (-30, 36, 8), (30, 48, 16), BOIS, sens='x')
    d.rayonnage('livres', -28, 28, 48, 62, 9, 15, etage=14)
    d.c('etagere_h', (-30, 62, 8), (30, 64, 16), BOIS_N, sens='x')
    d.c('parchemin', (-8, 37, -12), (8, 38, 0), VERRE, base=4.6)
    d.boite('encrier', 2, 37, 41, 2, OR, cx=18, cz=-4)
    d.c('plume', (17, 41, -5), (18, 54, -3), ('uni', 'plume_blanche'), base=5, rot=(0, 0, -14), pivot=(18, 41, -4), **CF)
    return d.fin()


def canon_decoratif():
    d = D('decob_canon_decoratif', 512)
    d.c('affut', (-14, 8, -20), (14, 16, 20), BOIS_N, sens='z', planche=7)
    d.c('affut_h', (-11, 16, -8), (11, 24, 20), BOIS, sens='z', planche=7)
    for cz in (-14, 14):
        for x0, x1 in ((14, 17), (-17, -14)):
            d.c('roue', (x0, 0, cz - 8), (x1, 16, cz + 8), ('plan', 'bois_sombre'), base=3, masque=ROUE_X, **CR)
            d.c('moyeu', (x0 - 1 if x0 < 0 else x0, 6, cz - 2), (x1 + 1 if x0 > 0 else x1, 10, cz + 2), FER_N)
    F = dict(ton=.5)
    d.c('fut', (-8, 22, -32), (8, 37, 16), FER_N, sens='z', **F)
    d.c('fut_b', (-10, 22, 16), (10, 39, 22), FER_N, ton=.8)
    d.c('bouche', (-10, 21, -42), (10, 39, -32), FER_N, ton=.9)
    d.c('frette', (-9, 21, -14), (9, 38, -11), OR)
    d.c('frette', (-9, 21, 6), (9, 38, 9), OR)
    d.c('ame', (-5, 25, -43), (5, 35, -42), ('uni', 'sabot'), base=0, hide={'east', 'west', 'up', 'down', 'south'})
    B = ('metal', 'fer_noir')
    for x, z, y0 in ((26, -2, 0), (34, -2, 0), (30, 6, 0), (30, 1, 8)):
        d.boite('boulet', 4, y0, y0 + 8, 4, B, cx=x, cz=z, ton=.6)
    return d.fin()


def cercle_invocation():
    d = D('decob_cercle_invocation', 513)
    d.c('dalle', (-35, 0, -35), (35, 3, 35), MARBRE_N, hide={'down'})
    d.c('cadre_n', (-35, 3, -35), (35, 5, -31), OR)
    d.c('cadre_s', (-35, 3, 31), (35, 5, 35), OR)
    d.s('cadre_e', (31, 3, -31), (35, 5, 31), OR)
    d.c('anneau_n', (-20, 3, -21), (20, 4, -18), AME)
    d.c('anneau_s', (-20, 3, 18), (20, 4, 21), AME)
    d.s('anneau_e', (18, 3, -18), (21, 4, 18), AME)
    d.c('croix_x', (-16, 3, -2), (16, 4, 2), OR, ton=.3)
    d.c('croix_z', (-2, 3, -16), (2, 4, -2), OR, ton=.3)
    d.c('croix_z', (-2, 3, 2), (2, 4, 16), OR, ton=.3)
    with d.miroir():
        for z in (-26, 26):
            d.boite('rune', 3, 3, 6, 3, AME, cx=26, cz=z, ton=.6)
    return d.fin()


def chariot_marchand():
    d = D('decob_chariot_marchand', 514)
    d.c('plancher', (-33, 12, -18), (34, 16, 18), BOIS, sens='x', planche=6)
    d.c('ridelle_n', (-33, 16, -20), (34, 30, -18), BOIS_C, sens='x', planche=5)
    d.c('ridelle_s', (-33, 16, 18), (34, 30, 20), BOIS_C, sens='x', planche=5)
    d.c('ridelle_ar', (-33, 16, -18), (-31, 30, 18), BOIS_C, sens='z', planche=5)
    d.c('ridelle_av', (32, 16, -18), (34, 30, 18), BOIS_C, sens='z', planche=5)
    for x in (-30, 0, 30):
        d.c('piquet', (x - 1, 14, -21), (x + 1, 32, -20), FER_N, **CF)
        d.c('piquet', (x - 1, 14, 20), (x + 1, 32, 21), FER_N, **CF)
    for cx in (-20, 20):
        for z0, z1 in ((-23, -21), (21, 23)):
            d.roue('roue', cx, 11, z0, z1, 11)
    d.c('essieu', (-22, 8, -21), (-18, 12, 21), BOIS_N, sens='z')
    d.c('essieu', (18, 8, -21), (22, 12, 21), BOIS_N, sens='z')
    d.c('brancard', (34, 14, -12), (58, 17, -9), BOIS_N, sens='x')
    d.c('brancard', (34, 14, 9), (58, 17, 12), BOIS_N, sens='x')
    d.caisse('caisse', (-28, 16, -15), (-4, 38, 8), ton=.2)
    d.caisse('caisse', (0, 16, -12), (20, 32, 8), ton=-.2)
    d.c('sac', (6, 16, 10), (26, 24, 18), ('peau_tissu', 'sable'), base=4)
    d.c('tonnelet', (-26, 16, 10), (-12, 28, 18), BOIS, sens='y')
    d.c('cercle', (-27, 21, 9), (-11, 23, 19), FER_N, hide={'up', 'down'})
    return d.fin()


def cheminee():
    d = D('decob_cheminee', 515)
    d.c('foyer_sol', (-35, 0, -13), (35, 5, 14), PIERRE_N)
    with d.miroir():
        d.c('jambage', (16, 5, -10), (32, 52, 12), PIERRE, assise=8, long=14)
    d.c('fond', (-16, 5, 4), (16, 52, 12), PIERRE_N, assise=6, long=10)
    d.c('linteau', (-33, 52, -12), (33, 60, 13), DALLE, ton=.4)
    d.c('manteau', (-35, 60, -13), (35, 63, 14), BOIS_N, sens='x')
    d.c('hotte', (-24, 63, -6), (24, 82, 12), PIERRE, assise=8, long=14)
    d.c('conduit', (-14, 82, -2), (14, 96, 12), PIERRE, assise=8, long=14)
    d.c('braises', (-12, 5, -6), (12, 7, 4), BRAISES)
    d.c('buche', (-10, 7, -4), (10, 11, 0), BOIS_N, sens='x', rot=(0, 8, 0), pivot=(0, 9, -2))
    d.flamme('a', 0, -1, 9, 11, 30)
    d.flamme('b', -7, -3, 9, 6, 20)
    d.flamme('c', 7, -3, 9, 6, 22)
    d.boite('blason', 5, 66, 76, 1, OR, cz=-7)
    return d.fin()


def coffre_tresor():
    d = D('decob_coffre_tresor', 516)
    d.c('caisse', (-24, 0, -16), (24, 28, 16), BOIS, sens='x', planche=7)
    with d.miroir():
        d.c('coin', (21, 0, -18), (27, 30, -13), OR, ton=-.1)
        d.c('coin', (21, 0, 13), (27, 30, 18), OR, ton=-.1)
        d.c('cerclage', (9, 0, -17), (13, 29, 17), FER_N, ton=.4)
    d.c('serrure', (-4, 16, -18), (4, 28, -16), OR, ton=.3)
    # couvercle ouvert et tresor
    d.c('couvercle', (-25, 30, 12), (25, 56, 18), BOIS_N, sens='x', rot=(14, 0, 0), pivot=(0, 30, 15))
    O = ('peau', 'or')
    d.c('or', (-22, 28, -14), (22, 34, 12), ('ecailles', 'or'), base=3.4, bas=0, haut=0, l=3, h=3)
    d.c('or', (-16, 34, -10), (14, 40, 8), ('ecailles', 'or'), base=3.8, bas=0, haut=0, l=3, h=3)
    d.c('or', (-8, 40, -6), (8, 44, 4), ('ecailles', 'or'), base=4.2, bas=0, haut=0, l=3, h=3)
    d.cristal('joyau', 12, -6, 38, 7, 3)
    d.boite('coupe', 3, 44, 46, 3, FER, cx=-2, cz=-2, ton=.6)
    return d.fin()


def colonne_lumineuse():
    d = D('decob_colonne_lumineuse', 517)
    y = d.socle('socle', 13, 13, [(4, 0), (4, 2)], PIERRE_N)
    d.c('cristal', (-8, y, -8), (8, 66, 8), AME)
    with d.miroir():
        d.c('montant', (8, y, -8), (11, 66, -5), PIERRE_N, ton=.5)
        d.c('montant', (8, y, 5), (11, 66, 8), PIERRE_N, ton=.5)
    d.boite('chap', 12, 66, 70, 12, ('tissu', 'pourpre'), bord=1)
    d.boite('chap', 16, 70, 74, 16, ('tissu', 'pourpre'), bord=1)
    d.boite('toit', 9, 74, 82, 9, ('tissu', 'pourpre'), bord=1, ton=.2)
    d.boite('toit', 5, 82, 88, 5, ('tissu', 'pourpre'), bord=1, ton=.4)
    d.cristal('c', 0, 0, 88, 3, 2, pointe=True)
    return d.fin()


def comptoir_taverne():
    d = D('decob_comptoir_taverne', 518)
    d.c('comptoir', (-38, 0, -13), (38, 32, 10), BOIS, sens='y', planche=8)
    d.c('plateau', (-40, 32, -17), (40, 36, 13), BOIS_N, sens='x')
    d.c('frise', (-38, 28, -14), (38, 32, -13), OR, **CF)
    # etagere arriere et bouteilles
    d.c('etagere', (-30, 36, 4), (30, 40, 13), BOIS_N, sens='x')
    d.c('fond', (-30, 40, 8), (30, 62, 13), BOIS, sens='x')
    d.c('etagere_h', (-32, 62, 4), (32, 66, 13), BOIS_N, sens='x')
    cols = ('uni', 'rar_peu_commun'), ('uni', 'rar_rare'), ('uni', 'rouge'), ('uni', 'rar_legendaire')
    for i, x in enumerate(range(-26, 28, 9)):
        m = cols[i % 4]
        d.boite('bouteille', 2, 40, 50, 2, m, cx=x, cz=6, base=2.6)
        d.boite('goulot', 1, 50, 54, 1, m, cx=x, cz=6, base=3)
    # chopes sur le plateau
    for x, z in ((-24, -8), (-14, -5), (20, -7)):
        d.boite('chope', 3, 36, 43, 3, BOIS_C, cx=x, cz=z, sens='y')
        d.c('mousse', (x - 2, 43, z - 2), (x + 2, 44, z + 2), VERRE, base=5)
    return d.fin()


def creneau():
    d = D('decob_creneau', 519)
    S = ('maconnerie', 'pierre')
    M = dict(assise=8, long=16)
    d.c('mur', (-72, 0, -20), (72, 60, 8), S, **M)
    d.c('chemin', (-72, 44, 8), (72, 60, 46), DALLE, ton=-.1)
    d.c('corniche', (-72, 60, -20), (72, 64, 8), DALLE, ton=.5)
    for x in (-72, -40, -8, 24, 56):
        d.c('merlon', (x, 64, -20), (x + 16, 90, -6), S, assise=6, long=12, ton=.2)
        d.c('coiffe', (x - 1, 90, -21), (x + 17, 94, -5), DALLE, ton=.4)
        d.c('archere', (x + 7, 72, -21), (x + 9, 84, -20), ('uni', 'sabot'), base=0, **CF)
    return d.fin()


def dais():
    d = D('decob_dais', 520)
    d.c('marche_1', (-38, 0, -38), (38, 8, 38), MARBRE)
    d.c('marche_2', (-30, 8, -30), (30, 16, 30), MARBRE, ton=.2)
    d.c('marche_3', (-22, 16, -22), (22, 24, 22), MARBRE, ton=.4)
    d.c('tapis', (-16, 24, -16), (16, 25, 16), ('tissu', 'rar_legendaire'), bord=2)
    with d.miroir():
        for z in (-34, 34):
            d.boite('borne', 3, 8, 30, 3, DALLE, cx=34, cz=z, ton=.6)
    return d.fin()


def dragon_pierre():
    d = D('decob_dragon_pierre', 521)
    S = ('marbre', 'pierre')
    B = dict(base=2.8)
    y = d.socle('socle', 28, 30, [(6, 0), (4, 3)], PIERRE_N)
    with d.miroir():
        d.c('cuisse', (6, y, 4), (16, y + 22, 22), S, ton=.2, **B)
        d.c('patte_ar', (7, y, -2), (15, y + 6, 4), S, **B)
        d.c('patte_av', (8, y, -26), (15, y + 26, -16), S, **B)
        d.c('griffes', (8, y, -29), (15, y + 3, -26), PIERRE, ton=.5)
    d.c('corps', (-11, y + 14, -20), (11, y + 36, 22), S, ton=.2, **B)
    d.c('cou', (-6, y + 26, -32), (6, y + 56, -18), S, ton=.3, rot=(-12, 0, 0), pivot=(0, y + 30, -24), **B)
    d.c('tete', (-7, y + 50, -35), (7, y + 62, -19), S, ton=.5, **B)
    d.rune('tete', 'north', ['.kk......kk.', '.kR......Rk.', '............', 'kkkkkkkkkkkk'], 6, y + 60)
    with d.miroir():
        d.c('corne', (3, y + 62, -26), (6, y + 74, -22), S, ton=.8, rot=(22, 0, -10), pivot=(4.5, y + 62, -24), **B)
        d.c('aile', (10, y + 22, 6), (42, y + 76, 8), ('plan', 'pierre'), base=2.8, masque=AILE_D, rot=(0, -18, 0), pivot=(10, y + 30, 7), **CF)
    d.c('crete', (-1, y + 36, -14), (1, y + 42, 20), ('pics', 'pierre'), base=2.6, pas=8, dent=5, **CR)
    d.c('queue', (-6, y, 22), (6, y + 12, 34), S, ton=.1, **B)
    d.c('queue', (-3, y, 30), (3, y + 8, 35), S, ton=.3, **B)
    return d.fin()


AILE_D = ['..............##', '............####', '..........######', '........########', '......##########', '....############', '..##############', '.###############', '##.####.####.###', '#...##...##...##', '#....#....#....#']


def escalier_monumental():
    d = D('decob_escalier_monumental', 522)
    S = ('dalle', 'pierre')
    n = 14
    pas_z = 118 // n
    h = 96 // n
    for i in range(n):
        z0 = -65 + i * pas_z
        d.c('marche', (-55, 0, z0), (55, (i + 1) * h, z0 + pas_z), MARBRE, ton=i * .02, hide={'down'} if i else set())
    d.c('palier', (-55, 0, -65 + n * pas_z), (55, n * h, 65), MARBRE, ton=.4)
    with d.miroir():
        d.c('limon', (55, 0, -65), (65, 98, 65), PIERRE, assise=10, long=16)
    return d.fin()


def fauteuil_royal():
    d = D('decob_fauteuil_royal', 523)
    with d.miroir():
        d.c('pied_av', (11, 0, -15), (17, 26, -9), BOIS_N, sens='y')
        d.c('pied_ar', (11, 0, 8), (17, 26, 14), BOIS_N, sens='y')
        d.c('accoudoir', (14, 34, -16), (20, 38, 12), BOIS, sens='z')
        d.c('accoudoir_m', (15, 26, -14), (19, 34, -10), BOIS_N, sens='y')
    d.c('assise', (-16, 24, -16), (16, 30, 14), ('tissu', 'rouge'), bord=2, motif=True)
    d.c('coussin', (-13, 30, -14), (13, 33, 10), ('tissu', 'rouge'), bord=1, ton=.4)
    d.c('dossier', (-14, 26, 11), (14, 66, 17), BOIS_N, sens='y')
    d.c('dossier_tissu', (-11, 34, 10), (11, 62, 11), ('tissu', 'rouge'), bord=2, **CF)
    d.c('couronne', (-10, 66, 12), (10, 70, 16), OR)
    d.boite('couronne', 6, 70, 76, 2, OR, cz=14, ton=.3)
    d.boite('couronne', 3, 76, 80, 1, OR, cz=14, ton=.5)
    return d.fin()


def fenetre_gothique():
    d = D('decob_fenetre_gothique', 524)
    M = dict(assise=8, long=16)
    d.c('socle', (-57, 0, -16), (57, 8, 16), PIERRE_N)
    with d.miroir():
        d.c('pile', (34, 8, -12), (57, 122, 12), PIERRE, **M)
        d.c('bande', (33, 62, -14), (57, 66, 14), DALLE, ton=.4)
        d.c('couv', (34, 122, -14), (57, 126, 14), DALLE, ton=.4)
        d.pinacle('pin', 46, 0, 126, 32, 7, mat=PIERRE)
    d.c('allege', (-34, 8, -10), (34, 26, 10), PIERRE, **M)
    d.ogive('arc', 86, 30, 52, 8, -10, 10, ('marbre', 'marbre'), n=5)
    d.colonne('meneau', 0, 0, 26, 86, 2, base=MARBRE, chap=MARBRE)
    with d.miroir():
        d.c('jamb', (26, 26, -10), (34, 86, 10), MARBRE)
    PV = ('#d7e7ea', '#b6d3da', '#eef6f6', '#9fc2cc')
    d.vitrail('vitre', -26, 26, 26, 131, 0, palette=PV, cell=8)
    return d.fin()


def fontaine_murale():
    d = D('decob_fontaine_murale', 525)
    d.c('dosseret', (-29, 0, 2), (29, 80, 13), MARBRE)
    d.c('panneau', (-20, 30, 1), (20, 70, 2), DALLE, ton=.2, **CF)
    d.c('corniche', (-29, 74, 0), (29, 80, 13), DALLE, ton=.5)
    # tete de lion, cuve
    d.c('tete', (-6, 44, -3), (6, 58, 2), OR)
    d.c('gueule', (-3, 46, -6), (3, 51, -3), OR, ton=.3)
    d.c('filet', (-2, 18, -5), (2, 46, -3), EAU, no_ao=True, hide={'up', 'down'})
    d.c('cuve_sol', (-26, 0, -32), (26, 6, 2), MARBRE, ton=-.2)
    d.c('cuve_n', (-29, 6, -32), (29, 20, -26), MARBRE, ton=.3)
    d.s('cuve_e', (24, 6, -26), (29, 20, 2), MARBRE, ton=.3)
    d.c('eau', (-24, 6, -26), (24, 16, 2), EAU, hide={'down', 'north', 'south', 'east', 'west'}, no_ao=True)
    return d.fin()


def geode():
    d = D('decob_geode', 526)
    R = ('marbre', 'pierre_noire')
    y = d.socle('roche', 33, 30, [(8, 0), (10, 6), (6, 6)], R)
    d.c('roche_h', (-13, y, -12), (13, y + 6, 12), R, ton=.3)
    d.cristal('c_a', 0, 0, y + 6, 34, 7)
    d.cristal('c_b', -13, 6, y - 3, 22, 5, incl=(12, 0))
    d.cristal('c_c', 13, -6, y - 3, 20, 5, incl=(-14, 0))
    d.cristal('c_d', 24, 12, 8, 16, 4, incl=(-10, 0))
    d.cristal('c_e', -24, -14, 8, 14, 4, incl=(14, 0))
    d.cristal('c_f', 6, 20, 14, 12, 3)
    return d.fin()


def haie_taillee():
    d = D('decob_haie_taillee', 527)
    d.c('base', (-69, 0, -17), (69, 4, 17), ('uni', 'terre'), base=2)
    d.c('haie', (-68, 4, -15), (68, 60, 15), FEUILLE)
    for i, x in enumerate((-68, -32, 4, 40)):
        d.c('creneau', (x, 60, -15), (x + 28, 98 if i % 2 else 84, 15), FEUILLE, ton=.3)
    return d.fin()


def herse():
    d = D('decob_herse', 528)
    with d.miroir():
        d.c('pied', (40, 0, -11), (54, 10, 11), PIERRE_N)
        d.c('jambage', (41, 10, -9), (53, 126, 9), PIERRE, assise=8, long=12)
    d.c('linteau', (-54, 126, -11), (54, 141, 11), PIERRE, assise=8, long=16)
    d.c('rainure', (-41, 122, -3), (41, 126, 3), FER_N)
    d.grille('herse', -41, 41, 6, 122, 0, pas=14, large=4, traverses=((30, 34), (64, 68), (98, 102)), mat=('metal', 'fer_noir'), epais=2)
    for x in range(-34, 41, 14):
        d.c('pointe', (x - 2, 0, 0), (x + 2, 6, 2), FER, ton=.5)
    with d.miroir():
        d.c('chaine', (30, 122, -6), (32, 126, -4), FER)
    return d.fin()


def jardiniere_geante():
    d = D('decob_jardiniere_geante', 529)
    d.c('caisse', (-34, 0, -14), (34, 24, 14), BOIS, sens='x', planche=6)
    d.c('liseret', (-35, 22, -15), (35, 25, 15), OR, hide={'up', 'down'})
    with d.miroir():
        d.c('angle', (30, 0, -15), (35, 22, -10), BOIS_N, sens='y')
        d.c('angle', (30, 0, 10), (35, 22, 15), BOIS_N, sens='y')
    d.c('terre', (-31, 24, -11), (31, 25, 11), ('uni', 'terre'), base=2)
    n = d.c('plante', (-30, 25, -10), (-10, 40, 10), FEUILLE)
    fleurs_sur(d, n, densite=.05)
    n = d.c('plante', (-8, 25, -9), (14, 45, 9), FEUILLE, ton=.3)
    fleurs_sur(d, n, densite=.05, seed=8)
    n = d.c('plante', (16, 25, -10), (30, 36, 10), FEUILLE, ton=-.2)
    fleurs_sur(d, n, densite=.05, seed=2)
    return d.fin()


def lanterne_sol():
    d = D('decob_lanterne_sol', 530)
    y = d.socle('socle', 13, 13, [(3, 0), (3, 2)], OR)
    d.c('verre', (-8, y, -8), (8, 44, 8), VERRE, base=4.4, no_ao=True)
    with d.miroir():
        d.boite('montant', 2, y, 44, 2, OR, cx=9, cz=-9)
        d.boite('montant', 2, y, 44, 2, OR, cx=9, cz=9)
    d.boite('toit', 12, 44, 47, 12, OR)
    d.boite('toit', 8, 47, 52, 8, OR, ton=.3)
    d.boite('toit', 4, 52, 58, 4, OR, ton=.5)
    d.boite('epi', 2, 58, 64, 2, OR, ton=.7)
    return d.fin()


def lit_royal():
    d = D('decob_lit_royal', 531)
    d.c('cadre', (-33, 8, -40), (33, 20, 40), BOIS_N, sens='z')
    with d.miroir():
        for z in (-37, 37):
            d.boite('colonne', 3, 0, 78, 3, BOIS_N, cx=31, cz=z, sens='y')
    d.c('matelas', (-30, 20, -38), (30, 28, 36), ('peau_tissu', 'marbre'), base=4.2)
    d.c('couverture', (-31, 22, -39), (31, 30, 12), ('tissu', 'rouge'), bord=2)
    d.c('oreiller', (-24, 28, 20), (-3, 33, 34), ('peau_tissu', 'marbre'), base=4.6)
    d.c('oreiller', (3, 28, 20), (24, 33, 34), ('peau_tissu', 'marbre'), base=4.6)
    d.c('tete', (-30, 20, 36), (30, 66, 40), BOIS, sens='x', planche=8)
    d.c('tete_or', (-30, 66, 35), (30, 70, 41), OR)
    d.c('ciel', (-35, 76, -42), (35, 82, 42), BOIS_N, sens='z')
    v = d.c('lambrequin', (-34, 66, -42), (34, 76, -41), ('tissu', 'rouge'), bord=0, **CF)
    d._masque_alpha(v, feston(68, 8))
    with d.miroir():
        v = d.c('rideau', (34, 26, -41), (35, 76, -22), ('peau_tissu', 'rouge'), **CR)
        v = d.c('rideau_h', (34, 66, -22), (35, 76, 41), ('tissu', 'rouge'), bord=0, **CR)
    return d.fin()


def lustre_cristal():
    d = D('decob_lustre_cristal', 532)
    d.fut('chaine', 0, 0, 56, 77, 1, OR, chanfrein=0)
    d.boite('plafonnier', 32, 46, 50, 27, ('tissu', 'or'), base=3, bord=2)
    d.boite('plafonnier', 22, 50, 56, 18, ('tissu', 'or'), base=3.4, bord=2)
    for x, z, h, r in ((-22, -14, 34, 6), (22, -14, 30, 6), (-22, 14, 30, 6), (22, 14, 34, 6), (0, 0, 49, 8), (0, -20, 22, 4), (0, 20, 22, 4)):
        y0 = 46 - h
        d.c('pendeloque', (x - r, y0, z - r), (x + r, 46, z + r), AME, rot=(0, 45, 0), pivot=(x, 46, z), ton=(abs(x) % 3) * .2)
    return d.fin()


def lutrin():
    d = D('decob_lutrin', 533)
    y = d.socle('pied', 12, 12, [(3, 0), (2, 3)], OR)
    d.fut('tige', 0, 0, y, 42, 2, OR)
    d.boite('noeud', 4, 26, 30, 4, OR, ton=.4)
    d.c('traverse', (-22, 42, -5), (22, 48, 5), OR, ton=.2)
    d.c('pupitre', (-18, 48, -12), (18, 52, 12), OR, rot=(24, 0, 0), pivot=(0, 50, 0), ton=.3)
    d.c('livre', (-14, 53, -9), (14, 55, 9), ('tissu', 'pourpre'), bord=1, rot=(24, 0, 0), pivot=(0, 50, 0))
    d.c('croix', (-2, 48, 6), (2, 77, 10), OR, ton=.5)
    d.c('croix', (-7, 68, 6), (-2, 72, 10), OR, ton=.5)
    d.c('croix', (2, 68, 6), (7, 72, 10), OR, ton=.5)
    return d.fin()


def mur_orne():
    d = D('decob_mur_orne', 534)
    d.c('soubassement', (-74, 0, -20), (74, 10, 16), PIERRE_N)
    d.c('mur', (-68, 10, -13), (68, 120, 10), MARBRE)
    d.c('panneau', (-58, 22, -14), (58, 108, -13), DALLE, ton=.2, **CF)
    d.c('cadre', (-60, 20, -15), (60, 23, -13), OR)
    d.c('cadre', (-60, 107, -15), (60, 110, -13), OR)
    with d.miroir():
        d.c('cadre_v', (57, 23, -15), (60, 107, -13), OR)
        d.c('pilastre', (68, 10, -18), (74, 120, 14), MARBRE, ton=.3)
    d.c('corniche', (-74, 120, -20), (74, 126, 16), DALLE, ton=.5)
    d.c('corniche_or', (-72, 126, -16), (72, 134, 12), OR)
    return d.fin()


def parterre_fleurs():
    d = D('decob_parterre_fleurs', 535)
    d.c('terre', (-38, 0, -38), (38, 5, 38), ('uni', 'terre'), base=2.4)
    d.c('bordure_n', (-38, 5, -38), (38, 8, -35), PIERRE, assise=3, long=8)
    d.c('bordure_s', (-38, 5, 35), (38, 8, 38), PIERRE, assise=3, long=8)
    d.s('bordure_e', (35, 5, -35), (38, 8, 35), PIERRE, assise=3, long=8)
    for i, (x, z, r, h) in enumerate(((-22, -22, 8, 28), (0, -24, 7, 22), (22, -20, 8, 26), (-24, 2, 7, 20), (0, 2, 9, 32), (24, 4, 7, 24), (-20, 24, 8, 26), (4, 24, 7, 22), (24, 24, 7, 18))):
        n = d.c('touffe', (x - r, 5, z - r), (x + r, 5 + h, z + r), FEUILLE, ton=(i % 3) * .2)
        fleurs_sur(d, n, densite=.09, seed=i)
    return d.fin()


def passerelle():
    d = D('decob_passerelle', 536)
    with d.miroir():
        d.c('culee', (46, 0, -22), (65, 56, 22), PIERRE, assise=8, long=14)
        d.c('pile_garde', (46, 56, -25), (65, 100, -19), MARBRE, ton=.2)
        d.c('pile_garde', (46, 56, 19), (65, 100, 25), MARBRE, ton=.2)
    d.arc('arche', 16, 46, 16, -20, 20, ('maconnerie', 'pierre'), n=5, assise=8, long=12)
    d.c('tablier', (-46, 56, -22), (46, 62, 22), DALLE, ton=.3)
    for z in (-24, 18):
        n = d.c('garde', (-46, 62, z), (46, 86, z + 6), ('tissu', 'or'), base=2.8, bord=2)
        d.c('main', (-46, 86, z - 1), (46, 92, z + 7), MARBRE, ton=.4)
    return d.fin()


def phare_arcane():
    d = D('decob_phare_arcane', 537)
    S = ('maconnerie', 'pierre_noire')
    M = dict(assise=8, long=14)
    d.c('etage', (-26, 0, -26), (26, 14, 26), S, **M)
    d.c('frise', (-24, 14, -24), (24, 16, 24), OR)
    d.c('etage', (-20, 16, -20), (20, 34, 20), S, **M, ton=.2)
    d.c('frise', (-18, 34, -18), (18, 36, 18), OR)
    d.c('etage', (-14, 36, -14), (14, 54, 14), S, **M, ton=.4)
    d.c('frise', (-13, 54, -13), (13, 56, 13), OR)
    o = d.os('foyer', (0, 70, 0))
    d.cristal('foyer_c', 0, 0, 58, 22, 6)
    d.flottant(o, 1.2)
    d.racine()
    with d.miroir():
        for z in (-10, 10):
            d.boite('montant', 1, 56, 84, 1, PIERRE_N, cx=10, cz=z)
    d.boite('toit', 12, 84, 87, 12, S, **M)
    d.boite('toit', 7, 87, 92, 7, S, **M)
    d.boite('toit', 3, 92, 96, 3, OR)
    return d.fin()


def pilier_court():
    d = D('decob_pilier_court', 538)
    y = d.socle('socle', 20, 20, [(4, 0), (4, 3)], MARBRE)
    d.fut('fut', 0, 0, y, 50, 11, MARBRE, chanfrein=2)
    d.boite('bague', 12, 16, 18, 12, OR, hide={'up', 'down'})
    d.boite('chap', 15, 50, 54, 15, MARBRE, ton=.4)
    d.boite('chap', 20, 54, 60, 20, DALLE, ton=.5)
    d.boite('plaque', 18, 60, 63, 18, ('tissu', 'or'), base=3, bord=2)
    d.boite('plaque', 13, 63, 68, 13, ('tissu', 'or'), base=3.4, bord=2)
    return d.fin()


def pilier_cristal_trio():
    d = D('decob_pilier_cristal_trio', 539)
    y = d.socle('socle', 25, 25, [(5, 0), (4, 4)], PIERRE_N)
    d.cristal('a', -5, 3, y, 74, 7)
    d.cristal('b', 8, 5, y, 52, 6, incl=(-6, 0))
    d.cristal('c', 2, -9, y, 36, 5, incl=(0, 8))
    return d.fin()


def portail_monumental():
    d = D('decob_portail_monumental', 540)
    M = dict(assise=10, long=16)
    d.c('marche', (-68, 0, -22), (68, 6, 22), PIERRE_N)
    with d.miroir():
        d.c('pile', (30, 6, -18), (62, 150, 18), PIERRE, **M)
        d.c('contrefort', (62, 6, -14), (68, 120, 14), PIERRE, **M, ton=.2)
        d.c('bande', (29, 80, -20), (62, 84, 20), DALLE, ton=.4)
        d.pinacle('pin', 48, 0, 150, 24, 9, mat=PIERRE)
    d.arc('voute', 110, 34, 14, -18, 18, ('dalle', 'pierre'), n=4, pas=8)
    d.c('fronton', (-30, 140, -16), (30, 158, 16), PIERRE, **M)
    d.c('corniche', (-32, 158, -18), (32, 162, 18), DALLE, ton=.5)
    d.c('cle', (-7, 136, -20), (7, 158, -16), OR)
    d.boite('cimier', 7, 162, 174, 7, OR, ton=.3)
    d.c('porte', (-30, 6, 1), (30, 114, 5), BOIS_N, sens='y', planche=8)
    for y in (30, 70):
        d.c('penture', (-30, y, 0), (30, y + 4, 1), FER_N, **CF)
    return d.fin()


def portail_runique():
    d = D('decob_portail_runique', 541)
    S = ('maconnerie', 'pierre_noire')
    M = dict(assise=8, long=10)
    with d.miroir():
        d.c('socle', (34, 0, -16), (60, 8, 16), S, **M)
        d.c('pile', (38, 8, -12), (56, 96, 12), S, **M)
        for y in (30, 60):
            d.c('rune', (37, y, -13), (57, y + 3, 13), OR)
    d.c('seuil', (-34, 0, -14), (34, 4, 14), S, **M)
    d.arc('voute', 96, 46, 18, -12, 12, AME, n=5)
    d.c('cle', (-8, 134, -14), (8, 156, 14), OR, ton=.2)
    d.cristal('cle_c', 0, 0, 156, 5, 3, pointe=True)
    o = d.os('voile_os', (0, 4, 0))
    n = d.c('voile', (-38, 4, 0), (38, 134, 1), ('uni', 'rar_epique'), base=5, no_ao=True, **CF)

    def voile(ctx, face):
        X, Y = ctx.Plocal[..., 0], ctx.Plocal[..., 1]
        ht = np.where(np.abs(X) < 37, 96 + np.sqrt(np.clip(37 * 37 - X * X, 0, None)), 0)
        face[..., 3] = np.where(Y > ht, 0, face[..., 3])
        t = ((Y + X * .4) % 22 < 2)
        face[..., :3] = np.where(t[..., None], np.array((246, 236, 255)), face[..., :3])
    d.p.decal(n, 'north', voile)
    d.p.decal(n, 'south', voile)
    d.racine()
    return d.fin()


def pylone_arcane():
    d = D('decob_pylone_arcane', 542)
    S = ('maconnerie', 'pierre_noire')
    M = dict(assise=6, long=10)
    y = d.socle('socle', 19, 19, [(5, 0), (5, 3)], S, **M)
    d.c('etage', (-12, y, -12), (12, 30, 12), S, **M)
    d.boite('frise', 14, 30, 33, 14, OR)
    d.c('etage', (-9, 33, -9), (9, 52, 9), S, **M, ton=.3)
    d.boite('frise', 11, 52, 55, 11, OR)
    d.c('etage', (-6, 55, -6), (6, 68, 6), S, **M, ton=.5)
    d.boite('frise', 8, 68, 70, 8, OR)
    d.cristal('c', 0, 0, 70, 20, 4)
    return d.fin()


def rocher_moussu():
    d = D('decob_rocher_moussu', 543)
    R = ('marbre', 'pierre')
    B = dict(base=2.6)
    n = d.c('roc_a', (-39, 0, -28), (10, 44, 26), R, **B)
    n2 = d.c('roc_b', (-6, 0, -30), (38, 30, 22), R, base=2.3)
    n3 = d.c('roc_c', (-26, 44, -18), (6, 68, 16), R, base=2.9)
    n4 = d.c('roc_d', (-16, 68, -10), (2, 77, 8), R, base=3.2)
    n5 = d.c('roc_e', (8, 30, -22), (28, 42, 12), R, base=2.6)
    mousse = ('feuilles', 'feuillage')
    d.c('mousse', (-26, 68, -18), (-16, 70, 16), mousse, hide={'down'})
    d.c('mousse', (-39, 44, -28), (-26, 46, 26), mousse, hide={'down'})
    d.c('mousse', (-6, 30, 12), (28, 32, 22), mousse, hide={'down'})
    d.c('mousse', (6, 44, -18), (10, 46, 16), mousse, hide={'down'})
    d.c('fougere', (20, 42, -16), (26, 50, -10), ('crete', 'feuillage'), base=2, ecart=3, pas=2, dent=4, hide={'north', 'south', 'up', 'down'}, no_shadow=True)
    return d.fin()


def saule_arcane():
    d = D('decob_saule_arcane', 544)
    d.fut('tronc', 0, 0, 0, 36, 7, BOIS_N, sens='y')
    touffe(d, 'couronne', 0, 0, 36, 32, 22)
    touffe(d, 'couronne', 2, -2, 58, 22, 16, ton=.3)
    # rideaux de branches retombantes, decoupes en bas
    PL = ('crete', 'feuillage')
    K = dict(base=2, ecart=2.4, bas=True, pas=4, dent=8)
    with d.miroir():
        d.c('rideau', (32, 8, -28), (35, 46, 28), PL, **K, **CR)
    d.c('rideau_n', (-30, 10, -35), (30, 44, -34), ('plan', 'feuillage'), base=3, masque=PLUIE, **CF)
    d.c('rideau_s', (-30, 10, 34), (30, 44, 35), ('plan', 'feuillage'), base=3, masque=PLUIE, **CF)
    d.cristal('a', 16, -12, 58, 8, 2)
    d.cristal('b', -18, 10, 56, 8, 2)
    return d.fin()


PLUIE = ['#' * 30] * 8 + ['##.###.####.##.###.##.####.###', '##.##..###..##.##..##.###..###', '#..##..##...#..##..#..##...##.', '#...#..#....#...#..#...#...#..', '....#.......#...#......#......']


def sphere_cristal():
    d = D('decob_sphere_cristal', 545)
    y = d.socle('socle', 14, 14, [(4, 0), (3, 2), (3, 2)], PIERRE_N)
    d.fut('tige', 0, 0, y, 18, 4, FER_N)
    d.boite('coupe', 9, 18, 21, 9, OR)
    o = d.os('sphere', (0, 44, 0))
    A = AME
    d.boite('s', 12, 21, 26, 12, A, ton=-.2)
    d.boite('s', 19, 26, 33, 19, A)
    d.boite('s', 25, 33, 55, 25, A, ton=.3)
    d.boite('s', 19, 55, 62, 19, A, ton=.5)
    d.boite('s', 12, 62, 67, 12, A, ton=.7)
    d.flottant(o, .8)
    return d.fin()


def stele_gravee():
    d = D('decob_stele_gravee', 546)
    y = d.socle('socle', 13, 13, [(4, 0), (3, 3)], PIERRE_N)
    n = d.c('stele', (-7, y, -5), (7, 58, 5), PIERRE_L, base=3)
    d.c('haut', (-5, 58, -4), (5, 66, 4), PIERRE_L, base=3.2)
    d.c('pointe', (-3, 66, -2), (3, 74, 2), PIERRE_L, base=3.4)
    R = ['VVVV', 'V..V', 'VVVV', '.VV.', '....', 'V..V', '.VV.', 'V..V', '....', '.VVV', 'V...', '.VV.', '...V', 'VVV.']
    d.rune(n, 'north', R, 2, 48)
    d.rune(n, 'south', R, -2, 48, flip=True)
    return d.fin()


def tabouret_geant():
    d = D('decob_tabouret_geant', 547)
    with d.miroir():
        for z in (-11, 11):
            d.c('pied', (8, 0, z - 3), (14, 48, z + 3), BOIS_N, sens='y', rot=(z * .6, 0, 4), pivot=(11, 48, z))
    d.c('barreau', (-9, 16, -1), (9, 20, 1), BOIS, sens='x')
    d.c('barreau', (-1, 16, -9), (1, 20, -1), BOIS, sens='z')
    d.c('barreau', (-1, 16, 1), (1, 20, 9), BOIS, sens='z')
    d.boite('assise', 16, 48, 52, 16, BOIS, sens='x', planche=6)
    d.boite('assise', 13, 52, 56, 13, BOIS_C, sens='x', planche=6)
    return d.fin()


def tente_commandement():
    d = D('decob_tente_commandement', 548)
    T = ('tissu', 'rouge')
    d.c('parois', (-35, 0, -35), (35, 36, 35), T, base=2.8, bord=2)
    d.c('entree', (-10, 0, -36), (10, 30, -35), ('uni', 'sabot'), base=.5, **CF)
    with d.miroir():
        d.c('pan_entree', (6, 0, -37), (12, 30, -35), ('peau_tissu', 'rouge'), rot=(0, 18, 0), pivot=(12, 0, -36))
    d.pyramide('toit', 36, 36, 37, 37, T, n=7, base=3.2, bord=1)
    d.c('frise', (-37, 34, -37), (37, 37, 37), OR, hide={'up', 'down'})
    d.fut('mat', 0, 0, 72, 90, 1, BOIS_N, chanfrein=0, sens='y')
    o = d.os('fanion', (1, 88, 0))
    n = d.c('fanion_p', (1, 80, 0), (16, 90, 1), ('uni', 'rouge'), base=4, **CF)
    d._masque_alpha(n, ['###############', '.##############', '..#############', '...############', '..#############', '.##############', '###############'])
    d.ondule(o, 8)
    return d.fin()


def torche_geante():
    d = D('decob_torche_geante', 549)
    y = d.socle('pied', 12, 12, [(3, 0), (2, 3)], OR)
    d.fut('manche', 0, 0, y, 58, 3, OR)
    d.boite('noeud', 5, 26, 30, 5, OR, ton=.4)
    d.boite('coupe', 6, 58, 61, 6, OR, ton=.2)
    d.boite('coupe', 9, 61, 66, 9, OR, ton=.4)
    d.boite('braises', 7, 66, 67, 7, BRAISES, hide={'down', 'north', 'south', 'east', 'west'})
    d.flamme('c', 0, 0, 67, 12, 26)
    return d.fin()


def totem_arcane():
    d = D('decob_totem_arcane', 550)
    S = ('maconnerie', 'pierre_noire')
    M = dict(assise=6, long=10)
    y = d.socle('socle', 13, 13, [(5, 0), (5, 2)], S, **M)
    d.c('fut', (-7, y, -7), (7, 56, 7), S, **M)
    n = d.c('masque', (-6, 26, -8), (6, 50, -7), ('uni', 'pierre'), base=3, **CF)
    d.rune(n, 'north', ['.EE....EE.', '.EE....EE.', '..........', '....EE....', '...E..E...', '...E..E...', '....EE....', '....EE....', '...EEEE...'], 5, 46)
    d.c('bras', (-26, 56, -6), (26, 64, 6), ('marbre', 'pierre'), base=3)
    d.c('fut_h', (-5, 64, -5), (5, 72, 5), S, **M, ton=.4)
    d.cristal('c', 0, 0, 72, 9, 4)
    return d.fin()


def tourelle():
    d = D('decob_tourelle', 551)
    S = ('maconnerie', 'pierre')
    M = dict(assise=8, long=14)
    d.c('socle', (-29, 0, -29), (29, 6, 29), PIERRE_N)
    d.c('corps', (-24, 6, -24), (24, 60, 24), S, **M)
    d.c('corniche', (-29, 60, -29), (29, 66, 29), DALLE, ton=.4)
    d.c('plancher', (-24, 66, -24), (24, 67, 24), DALLE, ton=.1, hide={'down', 'north', 'south', 'east', 'west'})
    for x, z in ((-22, -22), (22, -22), (-22, 22), (22, 22), (0, -22), (0, 22), (-22, 0), (22, 0)):
        d.boite('merlon', 6, 66, 83, 6, S, cx=x, cz=z, assise=6, long=10, ton=.2)
    d.c('archere', (-1, 28, -25), (1, 44, -24), ('uni', 'sabot'), base=0, **CF)
    d.c('archere', (-1, 28, 24), (1, 44, 25), ('uni', 'sabot'), base=0, **CF)
    return d.fin()


def camp():
    """Campement : foyer de pierres, buches croisees et flammes, quatre bancs de rondins, caisse et lanterne, souche et hache, pile de bois."""
    d = D('camp', 601)
    # foyer
    d.margelle('pierres', 0, 0, 0, 3, 11, 4, ('marbre', 'pierre'), base=2.6)
    d.c('braises', (-7, 0, -7), (7, 1, 7), BRAISES, hide={'down', 'north', 'south', 'east', 'west'})
    d.c('buche', (-8, 1, -2), (8, 4, 1), BOIS_N, sens='x', rot=(0, 30, 0), pivot=(0, 2, 0))
    d.c('buche', (-8, 1, -1), (8, 4, 2), BOIS_N, sens='x', rot=(0, -40, 0), pivot=(0, 2, 0))
    d.flamme('a', 0, 0, 3, 7, 19)
    d.flamme('b', -3, 2, 3, 4, 11)
    d.flamme('c', 3, -2, 3, 4, 13)
    # bancs de rondins
    with d.miroir():
        d.c('rondin', (22, 0, -13), (30, 7, 14), BOIS, sens='z', planche=4)
    d.c('rondin', (-13, 0, -30), (14, 7, -22), BOIS, sens='x', planche=4)
    d.c('rondin', (-13, 0, 22), (14, 7, 30), BOIS, sens='x', planche=4)
    # caisse et lanterne
    d.caisse('caisse', (-32, 0, 20), (-20, 10, 32), ton=.2)
    d.c('socle_lanterne', (-30, 10, 24), (-24, 12, 30), FER_N)
    d.c('verre', (-29, 12, 25), (-25, 18, 29), ('uni', 'cire'), base=5, no_ao=True)
    d.c('toit', (-30, 18, 24), (-24, 20, 30), FER_N, ton=.4)
    d.c('anse', (-28, 20, 26), (-26, 22, 28), FER_N)
    # souche et hache
    d.c('souche', (-16, 0, -14), (-8, 6, -6), BOIS_N, sens='y')
    d.c('manche', (-13, 6, -11), (-11, 16, -9), BOIS_C, sens='y', rot=(0, 0, 28), pivot=(-12, 6, -10))
    d.c('fer', (-18, 12, -10), (-13, 17, -9), FER, ton=.5, rot=(0, 0, 28), pivot=(-12, 6, -10))
    # pile de bois
    for i, (x, y, z) in enumerate(((26, 0, 20), (26, 0, 27), (26, 7, 23))):
        d.c('tas', (x, y, z), (x + 26, y + 7, z + 7), BOIS_C, sens='x', planche=4, ton=-.3 + i * .1)
    return d.fin()


def drapeau():
    """Drapeau d'Aeloria : hampe, tissu en trois segments qui ondulent l'un apres l'autre, fleche de pierre."""
    d = D('drapeau', 602)
    d.c('pied', (-3, 0, -3), (3, 2, 3), PIERRE)
    d.c('hampe', (-1, 2, -1), (1, 27, 1), BOIS_N, sens='y')
    T = ('peau_tissu', 'rar_legendaire')
    o1 = d.os('tissu1', (0, 22, -1))
    d.c('seg1', (0, 17, -7), (1, 27, -1), T, base=3.4, **CR)
    o2 = d.os('tissu2', (0, 22, -7), o1)
    n = d.c('seg2', (0, 17, -13), (1, 27, -7), T, base=3.4, **CR)
    d.p.decal(n, 'east', stamp(['..oo..', '.oVVo.', 'oVPPVo', '.oVVo.', '..oo..'], PAL, -7, 25))
    d.p.decal(n, 'west', stamp(['..oo..', '.oVVo.', 'oVPPVo', '.oVVo.', '..oo..'], PAL, -13, 25, flip_on=('west',)))
    o3 = d.os('tissu3', (0, 22, -13), o2)
    n = d.c('seg3', (0, 17, -18), (1, 27, -13), T, base=3.4, **CR)
    d.ondule(o1, 12, 0)
    d.ondule(o2, 16, .12)
    d.ondule(o3, 20, .24)
    d.racine()
    return d.fin(T=5.0)


DECORS = {'camp': camp, 'drapeau': drapeau, 'decob_arbre_cristal': arbre_cristal, 'decob_arbre_nain': arbre_nain, 'decob_armoire_geante': armoire_geante, 'decob_armure_exposition': armure_exposition, 'decob_autel_lumiere': autel_lumiere,
          'decob_autel_runique': autel_runique, 'decob_baliste': baliste, 'decob_balustrade': balustrade, 'decob_brasero_double': brasero_double, 'decob_buisson_fleuri': buisson_fleuri,
          'decob_bureau_scribe': bureau_scribe, 'decob_canon_decoratif': canon_decoratif, 'decob_cercle_invocation': cercle_invocation, 'decob_chariot_marchand': chariot_marchand, 'decob_cheminee': cheminee,
          'decob_coffre_tresor': coffre_tresor, 'decob_colonne_lumineuse': colonne_lumineuse, 'decob_comptoir_taverne': comptoir_taverne, 'decob_creneau': creneau, 'decob_dais': dais,
          'decob_dragon_pierre': dragon_pierre, 'decob_escalier_monumental': escalier_monumental, 'decob_fauteuil_royal': fauteuil_royal, 'decob_fenetre_gothique': fenetre_gothique,
          'decob_fontaine_murale': fontaine_murale, 'decob_geode': geode, 'decob_haie_taillee': haie_taillee, 'decob_herse': herse, 'decob_jardiniere_geante': jardiniere_geante, 'decob_lanterne_sol': lanterne_sol,
          'decob_lit_royal': lit_royal, 'decob_lustre_cristal': lustre_cristal, 'decob_lutrin': lutrin, 'decob_mur_orne': mur_orne, 'decob_parterre_fleurs': parterre_fleurs, 'decob_passerelle': passerelle,
          'decob_phare_arcane': phare_arcane, 'decob_pilier_court': pilier_court, 'decob_pilier_cristal_trio': pilier_cristal_trio, 'decob_portail_monumental': portail_monumental,
          'decob_portail_runique': portail_runique, 'decob_pylone_arcane': pylone_arcane, 'decob_rocher_moussu': rocher_moussu, 'decob_saule_arcane': saule_arcane, 'decob_sphere_cristal': sphere_cristal,
          'decob_stele_gravee': stele_gravee, 'decob_tabouret_geant': tabouret_geant, 'decob_tente_commandement': tente_commandement, 'decob_torche_geante': torche_geante, 'decob_totem_arcane': totem_arcane,
          'decob_tourelle': tourelle}
