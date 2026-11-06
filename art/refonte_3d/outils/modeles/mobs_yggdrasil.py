#!/usr/bin/env python3
"""Gardiens d'Yggdrasil (boss de guilde), cristal d'arene et drone. Lus par ImportedModel("yggdrasil", id) : meme format que les mobs, echelle 1, donc 1 unite = 1 texel = 1/16 de bloc en jeu.
Le code n'attend que les clips idle, walk, attack, cast, death et aucun os par son nom."""
import math
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, keys, pulse, rot, sin
from clips_mob import P_ATT, _fin, _r, ev, octo_mob, quadrupede_mob, serpent, volant
from kit_mob import CF, CR, FR, M, RE, anneau, feuille, losange, rond, triangle
from materiaux import stamp

K = {'k': '#0b0a0f', 'n': '#16120e'}
OR_ = ('plaque', 'or')
ORM = ('metal', 'or')
GEMME_A = ('cristal', 'ambre')
CF0 = dict(hide={'east', 'west', 'up', 'down'}, no_shadow=True)


def ajoute_cast(p, corps, tete=None, gr=1.0, T=2.4, extra=None):
    PH = (0, .3, .75, 1)
    tr = {corps: {'position': ev(T, PH, y=tuple(v * gr for v in (0, 3, 3, 0))), 'rotation': ev(T, PH, x=(0, 6, 6, 0))}}
    if tete:
        tr[tete] = _r(T, PH, x=(0, 12, 12, 0))
    for b, ch in (extra or {}).items():
        tr[b] = ch
    _fin(p, 'cast', T, tr, False)


# ------------------------------------------------------------------ 1. Kharvoss
def scarabee_racines():
    """Kharvoss, Devoreur des Racines : enorme scarabee a carapace vert-noir filetee d'or, gerbes de racines sur le dos, grandes mandibules, six pattes, antennes a boules."""
    m = M('scarabee_racines', 301, kind='yggdrasil', description='Kharvoss, Devoreur des Racines, gardien d\'Yggdrasil : refonte (16 texels par bloc en jeu).')
    p = m.p
    C = ('plaque', 'carapace')
    N = ('plaque', 'nuit')
    p.bone('body', (0, 30, -4), 'root')
    m.b = 'body'
    m.c('dessous', (-32, 16, -40), (32, 28, 34), ('segments', 'nuit'), base=3, pas=8)
    m.c('elytres', (-38, 26, -42), (38, 40, 36), C, base=1.8, bord=1)
    m.c('elytres_b', (-42, 32, -36), (42, 40, 30), C, base=2, bord=1)
    m.c('elytres_c', (-36, 40, -40), (36, 52, 34), C, base=2.2, bord=1)
    m.c('elytres_d', (-40, 40, -32), (40, 50, 26), C, base=2.4, bord=1)
    m.c('elytres_e', (-32, 52, -34), (32, 62, 28), C, base=2.6, bord=1)
    m.c('elytres_f', (-26, 62, -28), (26, 70, 22), C, base=3, bord=1)
    m.c('elytres_g', (-17, 70, -20), (17, 76, 14), C, base=3.4, bord=1)
    m.c('couture', (-2, 26, 36), (2, 53, 37), OR_, base=3, bord=0, hide={'north'})
    m.c('couture_b', (-2, 53, 28), (2, 62, 29), OR_, base=3, bord=0, hide={'north'})
    m.c('couture_h', (-2, 76, -20), (2, 77, 14), OR_, base=3.2, bord=0, hide={'down'})
    m.s('filet', (42, 34, -34), (43, 37, 28), OR_, base=3, bord=0, hide={'west'})
    m.s('filet_h', (32, 55, -32), (33, 58, 26), OR_, base=3.2, bord=0, hide={'west'})
    for i, (y, z) in enumerate(((42, -30), (42, -8), (42, 14))):
        m.lueur_sym('body', f'rune{i}', (40, y + 1, z + 2), (41, y + 7, z + 12), ('uni', 'rar_peu_commun'), base=4.4, hide={'west'})
    # racines du dos : gerbes de branches noueuses
    RA = ('ecorce', 'bois_sombre')
    for i, (x, z, h, rx, rz) in enumerate(((0, -6, 32, 6, 0), (-12, -14, 26, -8, 16), (12, -14, 26, -8, -16), (-15, 6, 28, 12, 18), (15, 6, 28, 12, -18), (0, 14, 24, 20, 2), (-6, -22, 20, -22, 10), (7, -22, 20, -22, -10))):
        m.c(f'racine{i}', (x - 3, 72, z - 3), (x + 3, 74 + h // 2, z + 3), RA, ton=1.2, rot=(rx, 0, rz), pivot=(x, 74, z))
        m.c(f'racine_b{i}', (x - 2, 74 + h // 2, z - 2), (x + 2, 74 + h, z + 2), RA, ton=1.6, rot=(rx, 0, rz), pivot=(x, 74, z))
        m.c(f'racine_v{i}', (x - 1, 74 + h, z - 1), (x + 1, 79 + h, z + 1), ('feuilles', 'feuillage'), ton=.4, rot=(rx, 0, rz), pivot=(x, 74, z))
    # tete, mandibules, yeux, antennes
    p.bone('head', (0, 36, -40), 'body')
    m.b = 'head'
    m.c('tete', (-24, 22, -62), (24, 52, -40), C, base=2.6, bord=2)
    m.c('chaperon', (-18, 40, -68), (18, 52, -62), C, base=3, bord=1)
    m.c('corne', (-5, 50, -72), (5, 66, -62), ('corne', 'carapace'), base=2.4, rot=(-18, 0, 0), pivot=(0, 50, -64))
    m.yeux('head', 12, 36, -62, 9, 7, ramp='rar_peu_commun', base=4.4)
    m.c('bouche', (-12, 22, -63), (12, 30, -62), ('uni', 'nuit'), base=1, no_ao=True, hide={'south'})
    for s, n in ((1, 'g'), (-1, 'd')):
        p.bone(f'mandibule_{n}', (s * 14, 26, -60), 'head')
        p.bone(f'antenne_{n}', (s * 18, 50, -58), 'head')
    m.sb('mandibule_g|mandibule_d', 'mandibule', (10, 21, -84), (19, 31, -58), N, base=3.2, rot=(0, -10, 0), pivot=(14, 26, -60))
    m.sb('mandibule_g|mandibule_d', 'mandibule_b', (7, 22, -98), (15, 30, -82), N, base=3.6, rot=(0, 26, 0), pivot=(13, 26, -83))
    m.sb('mandibule_g|mandibule_d', 'mandibule_p', (3, 23, -106), (9, 29, -95), ('griffe', 'ivoire'), base=2.6, rot=(0, 44, 0), pivot=(9, 26, -96))
    m.sb('mandibule_g|mandibule_d', 'dent', (7, 22, -74), (11, 29, -68), ('griffe', 'ivoire'), base=2.6)
    m.sb('antenne_g|antenne_d', 'antenne', (17, 50, -74), (19, 52, -58), N, base=4, rot=(-22, -28, 0), pivot=(18, 51, -58))
    m.sb('antenne_g|antenne_d', 'antenne_b', (27, 66, -74), (29, 68, -60), N, base=4, rot=(-10, -10, 0), pivot=(28, 67, -60))
    m.lueur_sym('antenne_g|antenne_d', 'boule', (26, 65, -80), (31, 70, -74), ('cristal', 'rar_peu_commun'), base=3)
    # six pattes : hanche, tibia, griffe
    legs = []
    for i, (z, fan) in enumerate(((-28, 30), (-4, 6), (20, -24))):
        g, d = f'leg{i}l', f'leg{i}r'
        p.bone(g, (30, 28, z), 'body')
        p.bone(d, (-30, 28, z), 'body')
        L = 24
        xk = round(30 + L * math.cos(math.radians(fan)) * .86)
        zk = round(z - L * math.sin(math.radians(fan)) * .86)
        p.bone(g + '_bas', (xk, 40, zk), g)
        p.bone(d + '_bas', (-xk, 40, zk), d)
        m.sb(f'{g}|{d}', f'hanche{i}', (28, 23, z - 5), (28 + L + 4, 33, z + 5), N, base=2.8, rot=(0, fan, 26), pivot=(30, 28, z))
        m.sb(f'{g}_bas|{d}_bas', f'tibia{i}', (xk - 4, 8, zk - 4), (xk + 4, 42, zk + 4), ('segments', 'carapace'), pas=7, axe='y', base=2.2, rot=(0, 0, 10), pivot=(xk, 40, zk))
        m.sb(f'{g}_bas|{d}_bas', f'griffe{i}', (xk + 6, 0, zk - 4), (xk + 12, 10, zk + 4), ('griffe', 'ivoire'), base=2.2)
        legs += [(g, g + '_bas', 1), (d, d + '_bas', -1)]
    octo_mob(p, legs, head='head', jaw=(('mandibule_g', 1), ('mandibule_d', -1)), walk=.9, attack=1.0, cast=True, gr=3)
    return p


# ------------------------------------------------------------------ 2. Sylkaris
def mante_epines():
    """Sylkaris, Faux des Ronces : mante dressee, abdomen lourd, thorax vert, tete triangulaire aux yeux enormes, quatre faux epineuses tenues devant, manteau de feuilles, quatre pattes hautes."""
    m = M('mante_epines', 302, kind='yggdrasil', description='Sylkaris, Faux des Ronces, gardien d\'Yggdrasil : refonte (16 texels par bloc en jeu).')
    p = m.p
    V = ('plaque', 'feuillage')
    VS = ('segments', 'feuillage_sombre')
    p.bone('body', (0, 62, 0), 'root')
    m.b = 'body'
    m.c('abdo_a', (-16, 46, 4), (16, 76, 26), VS, pas=7, base=3, axe='z')
    m.c('abdo_b', (-13, 42, 26), (13, 70, 44), VS, pas=7, base=2.6, axe='z')
    m.c('abdo_c', (-8, 40, 44), (8, 60, 54), VS, pas=6, base=2.2, axe='z')
    m.c('bassin', (-14, 56, -14), (14, 78, 6), V, base=3)
    m.c('thorax', (-12, 78, -16), (12, 104, 4), V, base=3.4)
    m.c('poitrail', (-9, 80, -18), (9, 102, -16), ('nacre', 'hydre_ventre'), base=3, axe='y', h=5, hide={'south'})
    m.c('col', (-8, 104, -14), (8, 112, 0), V, base=2.8)
    m.c('manteau', (-20, 54, 6), (20, 104, 7), FR('feuillage_sombre'), base=3.2, mini=.6, pas=3, **CF)
    m.c('manteau_h', (-17, 70, 26), (17, 92, 27), FR('feuillage'), base=3, mini=.5, pas=3, **CF)
    m.s('manteau_c', (16, 50, 2), (17, 92, 28), FR('feuillage'), base=2.8, mini=.55, pas=3, **CR)
    p.bone('head', (0, 112, -8), 'body')
    m.b = 'head'
    m.c('tete', (-16, 114, -24), (16, 128, -6), V, base=3.8)
    m.c('tete_b', (-9, 104, -26), (9, 115, -10), V, base=3.4)
    m.c('labre', (-5, 98, -28), (5, 105, -20), ('griffe', 'bois_sombre'), base=3)
    m.c('sommet', (-7, 128, -20), (7, 131, -9), V, base=4.2)
    m.lueur_sym('head', 'oeil', (13, 116, -26), (20, 128, -12), ('cristal', 'rar_peu_commun'), base=2.2)
    for s, n in ((1, 'g'), (-1, 'd')):
        p.bone(f'antenne_{n}', (s * 5, 130, -14), 'head')
    m.sb('antenne_g|antenne_d', 'antenne', (4, 129, -15), (6, 148, -13), ('uni', 'bois_sombre'), base=4, rot=(-14, 0, -20), pivot=(5, 130, -14))
    # quatre faux : bras porte en avant, lame epineuse rabattue vers le bas, plan tourne pour rester lisible de face
    for rang, (y, L, h, ep) in enumerate(((102, 26, 46, 4), (84, 20, 34, 3))):
        for s, n in ((1, 'g'), (-1, 'd')):
            p.bone(f'faux{rang}_{n}', (s * 12, y, -10), 'body', rot=(62 - rang * 8, s * 18, 0))
            p.bone(f'faux{rang}_{n}_b', (s * 16, y - L, -10), f'faux{rang}_{n}', rot=(-92 + rang * 6, s * 18, 0))
        g, d = f'faux{rang}_g', f'faux{rang}_d'
        m.sb(f'{g}|{d}', f'bras_faux{rang}', (12, y - L - 2, -14), (20, y + 3, -6), V, base=3.2)
        m.sb(f'{g}_b|{d}_b', f'dos_faux{rang}', (13, y - L - h // 2, -13), (19, y - L + 3, -7), V, base=2.6)
        I_ = ('os', 'ivoire')
        ya, yb, yc = y - L - h * 11 // 20, y - L - h * 17 // 20, y - L - h
        m.sb(f'{g}_b|{d}_b', f'lame_faux{rang}', (14, ya, -15), (18, y - L + 2, -8), I_, base=3.2)
        m.sb(f'{g}_b|{d}_b', f'lame_b_faux{rang}', (15, yb, -14), (17, ya, -9), I_, base=3.6)
        m.sb(f'{g}_b|{d}_b', f'lame_c_faux{rang}', (15, yc, -13), (17, yb, -10), I_, base=4)
        for k in range(3):
            yd = y - L - 4 - k * h // 5
            m.sb(f'{g}_b|{d}_b', f'dent_faux{rang}_{k}', (15, yd - 3, -19), (17, yd, -15), I_, base=3.8)
    # quatre pattes marcheuses, cuisse forte, genou haut, griffe
    legs = []
    for i, (z, zb) in enumerate(((-6, -26), (18, 40))):
        g, d = f'leg{i}l', f'leg{i}r'
        p.bone(g, (12, 62, z), 'body')
        p.bone(d, (-12, 62, z), 'body')
        xk, zk = 42, zb
        p.bone(g + '_bas', (xk, 78, zk), g)
        p.bone(d + '_bas', (-xk, 78, zk), d)
        L = int(math.hypot(xk - 12, zk - z, 16))
        ang = math.degrees(math.atan2(-(zk - z), xk - 12))
        m.sb(f'{g}|{d}', f'cuisse{i}', (10, 57, z - 4), (12 + L, 67, z + 4), VS, pas=7, axe='x', base=3, rot=(0, ang, 26), pivot=(12, 62, z))
        m.sb(f'{g}_bas|{d}_bas', f'genou{i}', (xk - 5, 73, zk - 5), (xk + 5, 83, zk + 5), V, base=3.4)
        m.sb(f'{g}_bas|{d}_bas', f'tibia{i}', (xk - 3, 8, zk - 3), (xk + 3, 74, zk + 3), VS, pas=8, axe='y', base=2.4, rot=(0, 0, 5), pivot=(xk, 76, zk))
        m.sb(f'{g}_bas|{d}_bas', f'griffe{i}', (xk + 3, 0, zk - 3), (xk + 9, 10, zk + 3), ('griffe', 'bois_sombre'), base=3)
        legs += [(g, g + '_bas', 1), (d, d + '_bas', -1)]
    octo_mob(p, legs, head='head', claws=(('faux0_g', 1), ('faux0_d', -1), ('faux1_g', 1), ('faux1_d', -1)), walk=.9, attack=1.0, cast=True, gr=3)
    return p


# ------------------------------------------------------------------ 3. Ormea
def raie_celeste():
    """Ormea, Voile des Cieux : raie gigantesque au ventre clair, dos de nuit etoilee, ailes en trois segments, lignes de constellation lumineuses, longue queue de comete."""
    m = M('raie_celeste', 303, kind='yggdrasil', description='Ormea, Voile des Cieux, gardien d\'Yggdrasil : refonte (16 texels par bloc en jeu).')
    p = m.p
    ET = ('etoile', 'tissu_bleu')
    p.bone('body', (0, 68, 10), 'root')
    m.b = 'body'
    m.c('dos', (-18, 62, -30), (18, 80, 44), ET, base=2.2)
    m.c('dos_h', (-11, 80, -18), (11, 86, 36), ET, base=2.8)
    m.c('ventre', (-16, 56, -26), (16, 62, 40), ('nacre', 'toison'), base=3.4, h=6)
    m.c('tete', (-14, 58, -44), (14, 76, -30), ET, base=2.6)
    m.s('lobe', (8, 58, -56), (15, 66, -44), ET, base=2.4)
    m.c('bouche', (-9, 58, -45), (9, 62, -44), ('uni', 'nuit'), base=1, no_ao=True, hide={'south'})
    m.yeux('head' if False else 'body', 8, 68, -44, 5, 4, ramp='ame', base=3.6)
    for i, (z, h) in enumerate(((-10, 16), (2, 20), (14, 18), (26, 13))):
        m.c(f'aileron{i}', (-1, 84, z - 4), (1, 84 + h, z + 8), ('plan', 'ame'), base=3, masque=triangle(6, 8), rot=(-18, 0, 0), pivot=(0, 86, z), **CR)
    m.lueur('body', 'ligne_dos', (-1, 86, -14), (1, 87, 32), ('uni', 'ame'), base=4.2, hide={'down'})
    # ailes en trois segments, plats et effiles
    for s, n in ((1, 'g'), (-1, 'd')):
        p.bone(f'aile1_{n}', (s * 16, 68, 4), 'body')
        p.bone(f'aile2_{n}', (s * 46, 68, 4), f'aile1_{n}')
        p.bone(f'aile3_{n}', (s * 72, 68, 4), f'aile2_{n}')
    m.sb('aile1_g|aile1_d', 'aile_a', (15, 63, -30), (47, 74, 38), ET, base=2.2)
    m.sb('aile1_g|aile1_d', 'aile_a_v', (16, 62, -28), (47, 63, 36), ('nacre', 'toison'), base=3, h=6, hide={'up'})
    m.sb('aile2_g|aile2_d', 'aile_b', (46, 64, -22), (73, 71, 28), ET, base=2.4)
    m.sb('aile2_g|aile2_d', 'aile_b_v', (47, 63, -20), (73, 64, 26), ('nacre', 'toison'), base=2.8, h=6, hide={'up'})
    m.sb('aile3_g|aile3_d', 'aile_c', (72, 65, -12), (92, 69, 16), ET, base=2.6)
    m.sb('aile3_g|aile3_d', 'aile_d', (92, 66, -4), (104, 68, 8), ET, base=2.8)
    for i, (x0, x1, z0) in enumerate(((16, 46, -8), (47, 72, 2), (73, 92, 2))):
        os_ = ('aile1_g|aile1_d', 'aile2_g|aile2_d', 'aile3_g|aile3_d')[i]
        y = (74, 71, 69)[i]
        m.lueur_sym(os_, f'constellation{i}', (x0, y, z0), (x1, y + 1, z0 + 2), ('uni', 'ame'), base=4, hide={'down'})
    # queue de comete
    pts = [(0, 68, 44), (0, 68, 62), (0, 70, 80), (0, 73, 96), (0, 77, 110)]

    def cote(m_, os_, i, P, W, H):
        x, y, z = P
        if i >= 1:
            m_.lueur(os_, f'etincelle{i}', (x - 1, y + H // 2, z - W[1] - 1), (x + 1, y + H // 2 + 2, z - W[1]), ('uni', 'ame'), base=4.4, hide={'south'})
    ch = m.courbe(pts, [6, 4, 3, 2], ET, prefixe='queue', parent='body', cote=cote, base=2.2)
    x_, y_, z_ = ch[-1][1]
    H_ = ch[-1][4]
    m.lueur(ch[-1][0] + '_r', 'comete', (x_ - 3, y_ + H_ - 2, z_ - 3), (x_ + 3, y_ + H_ + 5, z_ + 3), ('cristal', 'ame'), base=2.6)
    m.lueur(ch[-1][0] + '_r', 'comete_p', (x_ - 2, y_ + H_ + 5, z_ - 2), (x_ + 2, y_ + H_ + 12, z_ + 2), ('cristal', 'ame'), base=3.4)
    volant(p, [('aile1_g', 'aile1_d', 0.0), ('aile2_g', 'aile2_d', .1), ('aile3_g', 'aile3_d', .2)], queue=[c[0] for c in ch], head='body', idle=3.0, vol=1.2, attack=1.0, battement=11, battement_idle=6, cadence=1, hover=3, nom_vol='walk')
    ajoute_cast(p, 'body', gr=2)
    return p


# ------------------------------------------------------------------ 4. Thalassyr
def _tete_hydre(m, nom, tete, x, y, z, sc, EC, VE, COR):
    """Tete de serpent de mer : crane, museau, levre, crocs, yeux, cornes de corail. Renvoie l'os de machoire."""
    p = m.p
    hw, ml, hh = round(13 * sc), round(24 * sc), round(14 * sc)
    m.cb(tete, f'crane_{nom}', (x - hw, y - 3, z - 18), (x + hw, y + hh, z + 6), EC, l=5, h=5, y=(20, 160), ton=.5)
    m.cb(tete, f'museau_{nom}', (x - hw + 3, y - 2, z - 18 - ml), (x + hw - 3, y + hh - 5, z - 18), EC, l=4, h=4, y=(20, 160), ton=.7)
    m.cb(tete, f'levre_{nom}', (x - hw + 3, y - 3, z - 18 - ml), (x + hw - 3, y - 2, z - 18), ('uni', 'corail'), base=2.2)
    for i in range(3):
        xd = hw - 5 - i * 3
        for sg, c in ((1, 'g'), (-1, 'd')):
            a0, a1 = sorted((x + sg * xd, x + sg * (xd + 2)))
            m.cb(tete, f'croc_{nom}_{c}{i}', (a0, y - 7, z - 17 - ml + i * 4), (a1, y - 2, z - 15 - ml + i * 4), ('griffe', 'ivoire'), base=4)
    m.lueur(tete, f'oeil_{nom}_a', (x + hw, y + hh // 2 - 1, z - 15), (x + hw + 1, y + hh // 2 + 4, z - 8), ('uni', 'ame'), base=4, hide={'west'})
    m.lueur(tete, f'oeil_{nom}_b', (x - hw - 1, y + hh // 2 - 1, z - 15), (x - hw, y + hh // 2 + 4, z - 8), ('uni', 'ame'), base=4, hide={'east'})
    for sg, c in ((1, 'g'), (-1, 'd')):
        a0, a1 = sorted((x + sg * (hw - 6), x + sg * (hw - 1)))
        ax = (a0 + a1) / 2
        m.cb(tete, f'corail_{nom}_{c}', (a0, y + hh - 2, z - 7), (a1, y + hh + 15, z - 2), COR, base=3, rot=(-22, 0, -sg * 14), pivot=(ax, y + hh, z - 4))
        m.cb(tete, f'corail_{nom}_{c}2', (a0 - 6 if sg < 0 else a1, y + hh + 6, z - 6), (a0 if sg < 0 else a1 + 6, y + hh + 9, z - 3), COR, base=3.6, rot=(-22, 0, -sg * 14), pivot=(ax, y + hh, z - 4))
        m.cb(tete, f'corail_{nom}_{c}3', (a0, y + hh + 15, z - 6), (a1 - 1, y + hh + 23, z - 3), COR, base=4.2, rot=(-22, 0, -sg * 14), pivot=(ax, y + hh, z - 4))
    mach = f'machoire_{nom}'
    p.bone(mach, (x, y - 3, z - 9), tete)
    m.cb(mach, f'mandibule_{nom}', (x - hw + 4, y - 11, z - 16 - ml), (x + hw - 4, y - 3, z - 5), VE, h=5, base=3.2)
    return mach


def hydre_corail():
    """Thalassyr, Couronne des Profondeurs : hydre turquoise au corps massif, trois cous en plaques, tetes a cornes de corail orange, recif sur le dos, longue queue."""
    m = M('hydre_corail', 304, kind='yggdrasil', description='Thalassyr, Couronne des Profondeurs, gardien d\'Yggdrasil : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (10, 100)
    EC = ('ecailles', 'turquoise')
    VE = ('nacre', 'chair_pale')
    COR = ('corne', 'corail')
    p.bone('body', (0, 40, 8), 'root')
    m.b = 'body'
    m.c('poitrail', (-34, 22, -36), (34, 72, -4), EC, l=7, h=7, y=(10, 100))
    m.c('ventre', (-38, 20, -4), (38, 66, 30), EC, l=7, h=7, y=(10, 100))
    m.c('croupe', (-32, 24, 30), (32, 60, 54), EC, l=7, h=7, y=(10, 100))
    m.c('sous_ventre', (-28, 16, -32), (28, 22, 50), VE, h=8, base=3.4)
    m.c('devant', (-26, 24, -37), (26, 66, -36), VE, h=7, axe='y', base=3.4, hide={'south'})
    # recif : blocs de corail et d'algues sur le dos
    for i, (x, z, h, r, rx, rz) in enumerate(((0, -18, 20, 5, -6, 0), (-16, -8, 15, 4, -2, 16), (16, -6, 17, 4, 0, -16), (-6, 10, 18, 4, 8, 8), (10, 14, 16, 4, 10, -10), (0, 30, 15, 4, 18, 0), (-20, 22, 11, 3, 12, 22), (22, 24, 10, 3, 12, -24))):
        m.pointe('body', 'recif', x, 62, z, h, r, COR, inc=(rx, rz), base=3.4, tourne=0)
    m.c('algues', (-22, 66, -14), (22, 69, 24), ('feuilles', 'feuillage_sombre'), ton=.8)
    # queue
    pts = [(0, 44, 54), (0, 38, 70), (0, 32, 84), (0, 26, 96), (0, 22, 104)]

    def cote_q(m_, os_, i, P, W, H):
        x, y, z = P
        m_.cb(os_, f'nageoire{i}', (x - 1, y, z + W[1] - 1), (x + 1, y + H, z + W[1] + 10 - i), ('plan', 'ame'), base=2.6, masque=[''.join('#' if (a + b) % 5 < 4 else '.' for a in range(5)) for b in range(7)], **CR)
    ch_q = m.courbe(pts, [18, 13, 9, 6], EC, prefixe='queue', parent='body', cote=cote_q, l=6, h=6, y=(10, 100))
    # trois cous
    cous, jaws, heads = [], [], []
    for nom, xb, zb, yb, H_, sc, d in (('c', 0, -30, 70, 1.0, 1.0, (0, 0)), ('g', 22, -26, 62, .82, .86, (14, 0)), ('d', -22, -26, 62, .82, .86, (-14, 0))):
        h1, h2, h3 = round(23 * H_), round(23 * H_), round(20 * H_)
        pts = [(xb, yb, zb), (xb + d[0], yb + h1, zb - 8), (xb + d[0], yb + h1 + h2, zb - 2), (xb + d[0] // 2, yb + h1 + h2 + h3, zb - 10)]
        w = round(12 * sc)

        def cote_c(m_, os_, i, P, W, H, nom=nom):
            x, y, z = P
            m_.cb(os_, f'gorge_{nom}{i}', (x - W[0] + 3, y, z - W[1] - 1), (x + W[0] - 3, y + H - 2, z - W[1]), VE, h=6, axe='y', base=3.4, hide={'south'})
            m_.cb(os_, f'crete_{nom}{i}', (x - 1, y, z + W[1] - 1), (x + 1, y + H, z + W[1] + 8), ('plan', 'ame'), base=2.6, masque=[''.join('#' if (a + b) % 5 < 4 else '.' for a in range(5)) for b in range(7)], **CR)
        ch = m.courbe(pts, [w, w - 2, w - 3], EC, prefixe=f'cou_{nom}', parent='body', cote=cote_c, l=5, h=5, y=(10, 160), ton=.2)
        cous += [c[0] for c in ch]
        x4, y4, z4 = pts[-1]
        tete = 'head' if nom == 'c' else f'head_{nom}'
        p.bone(tete, (x4, y4, z4), ch[-1][0])
        heads.append(tete)
        jaws.append(_tete_hydre(m, nom, tete, x4, y4, z4, sc, EC, VE, COR))
    # pattes
    legs = []
    for nom, x, z, ph in (('fl', 32, -18, 0.0), ('fr', -32, -18, .5), ('bl', 32, 32, .56), ('br', -32, 32, .06)):
        s = 1 if x > 0 else -1
        p.bone('leg_' + nom, (x, 38, z), 'root')
        p.bone('leg_' + nom + '_bas', (x + s * 8, 16, z), 'leg_' + nom)
        legs.append(('leg_' + nom, 'leg_' + nom + '_bas', ph))
    for av, z in (('f', -18), ('b', 32)):
        m.sb(f'leg_{av}l|leg_{av}r', f'cuisse_{av}', (26, 14, z - 13), (52, 44, z + 13), EC, l=7, h=7, y=(0, 100), ton=.1)
        m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'tibia_{av}', (31, 6, z - 10), (49, 16, z + 10), EC, l=6, h=6, y=(0, 100), ton=-.1)
        m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'pied_{av}', (29, 0, z - 16), (51, 6, z + 10), EC, l=6, h=6, y=(0, 100), ton=-.3)
        for i, (xg, r) in enumerate(((29, 16), (37, 0), (45, -16))):
            m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'griffe_{av}{i}', (xg, 0, z - 24), (xg + 5, 4, z - 15), ('griffe', 'ivoire'), base=2.6, rot=(0, r, 0), pivot=(xg + 2, 2, z - 16))
    quadrupede_mob(p, legs, jaw='machoire_c', ears=tuple(j for j in jaws if j != 'machoire_c'), tail='queue0', amp=16, knee=26, leg_len=34, poids=3, gr=4, walk=.9, attack=1.0, cou=tuple(cous) + tuple(h for h in heads if h != 'head') + tuple(c[0] for c in ch_q[1:]), cast=True)
    return p


# ------------------------------------------------------------------ 5. Varkhun
def millepattes_ambre():
    """Varkhun, Anneaux d'Ambre : mille-pattes colossal, quatorze anneaux d'ambre cernes de noir, epines dorsales, vingt-huit pattes, tete a forcipules."""
    m = M('millepattes_ambre', 305, kind='yggdrasil', description='Varkhun, Anneaux d\'Ambre, gardien d\'Yggdrasil : refonte (16 texels par bloc en jeu).')
    p = m.p
    A = ('plaque', 'ambre')
    N = ('plaque', 'nuit')
    n = 14
    pas = 13
    z0 = -82
    p.bone('body', (0, 26, z0), 'root')
    pere = 'body'
    segs, gauches, droites = [], [], []
    for i in range(n):
        z = z0 + i * pas
        k = 1 - abs(i - 5) / 12
        w = round(16 + 8 * k)
        h = round(10 + 8 * k)
        os_ = f'seg{i}'
        p.bone(os_, (0, 26, z), pere)
        segs.append(os_)
        m.cb(os_, f'anneau{i}', (-w, 26 - h, z), (w, 26 + h, z + pas + 1), N, base=2.6, bord=1)
        m.cb(os_, f'plaque{i}', (-w + 2, 26 + h, z + 1), (w - 2, 26 + h + 4, z + pas - 1), A, base=3 - (i % 2) * .5, bord=1)
        if i % 2 == 0:
            m.sb(os_, f'plaque_c{i}', (w, 26 - h + 4, z + 1), (w + 1, 26 + h, z + pas - 1), A, base=2.6, bord=1, hide={'west'})
        hs = round(8 + 16 * k) - (i % 2) * 4
        m.pointe(os_, f'epine{i}', 0, 26 + h + 3, z + pas // 2, hs, 2, ('griffe', 'ambre'), inc=(14, 0), base=2.6)
        # une paire de pattes par anneau
        g, d = f'patte{i}_g', f'patte{i}_d'
        p.bone(g, (w - 2, 18, z + pas // 2), os_)
        p.bone(d, (-w + 2, 18, z + pas // 2), os_)
        m.sb(f'{g}|{d}', f'femur{i}', (w - 3, 15, z + pas // 2 - 2), (w + 18, 20, z + pas // 2 + 2), N, base=3.2, rot=(0, 0, -30), pivot=(w - 2, 18, z + pas // 2))
        m.sb(f'{g}|{d}', f'griffe{i}', (w + 14, 0, z + pas // 2 - 2), (w + 18, 9, z + pas // 2 + 2), ('griffe', 'ambre'), base=2.6)
        gauches.append(g)
        droites.append(d)
        pere = os_
    # tete, forcipules, antennes
    p.bone('head', (0, 26, z0), 'body')
    m.b = 'head'
    m.c('tete', (-20, 12, -106), (20, 42, z0), N, base=2.8, bord=2)
    m.c('tete_plaque', (-16, 42, -104), (16, 46, z0 - 2), A, base=3.4, bord=1)
    m.c('chaperon', (-12, 24, -112), (12, 40, -106), A, base=2.8, bord=1)
    m.yeux('head', 9, 30, -106, 8, 6, ramp='flamme', base=4.8)
    for s, c in ((1, 'g'), (-1, 'd')):
        p.bone(f'forcipule_{c}', (s * 12, 16, -102), 'head')
        p.bone(f'antenne_{c}', (s * 12, 40, -104), 'head')
    m.sb('forcipule_g|forcipule_d', 'forcipule', (6, 12, -124), (18, 22, -100), N, base=3, rot=(0, -12, 0), pivot=(12, 17, -102))
    m.sb('forcipule_g|forcipule_d', 'crochet', (1, 13, -130), (9, 21, -120), ('griffe', 'ambre'), base=3)
    m.sb('antenne_g|antenne_d', 'antenne_a', (11, 39, -128), (13, 41, -104), N, base=4, rot=(-24, -18, 0), pivot=(12, 40, -104))
    m.sb('antenne_g|antenne_d', 'antenne_b', (17, 49, -146), (19, 51, -125), N, base=4, rot=(-40, -18, 0), pivot=(18, 50, -125))
    # dernier anneau : pinces de queue
    z = z0 + n * pas
    m.sb(segs[-1], 'pince_queue', (6, 14, z - 2), (14, 24, z + 18), A, base=2.8, rot=(0, -12, 0), pivot=(10, 19, z))

    def jambes(T, amp):
        tr = {}
        for i, (g, d) in enumerate(zip(gauches, droites)):
            ph = i * .13
            tr[g] = {'rotation': rot(T, y=sin(amp, 1, ph), z=lambda q, ph=ph: 9 * max(0.0, math.cos(math.tau * (q + ph))))}
            tr[d] = {'rotation': rot(T, y=sin(-amp, 1, ph + .5), z=lambda q, ph=ph: -9 * max(0.0, math.cos(math.tau * (q + ph + .5))))}
        return tr
    T = 3.0
    tr = jambes(T, 2)
    for i, s_ in enumerate(segs):
        tr[s_] = {'rotation': rot(T, y=sin(.6, 1, .08 * i), x=sin(.4, 1, .08 * i + .25))}
    tr['head'] = {'rotation': rot(T, y=sin(5, 1, 0), x=sin(2, 1, .3))}
    tr['forcipule_g'] = {'rotation': rot(T, y=sin(4, 2, 0))}
    tr['forcipule_d'] = {'rotation': rot(T, y=sin(-4, 2, 0))}
    _fin(p, 'idle', T, tr)
    T = .9
    tr = jambes(T, 16)
    for i, s_ in enumerate(segs):
        tr[s_] = {'rotation': rot(T, y=sin(1.8, 1, .09 * i))}
    tr['head'] = {'rotation': rot(T, y=sin(-5, 1, 0))}
    _fin(p, 'walk', T, tr)
    T = 1.0
    PH = P_ATT
    tr = {'head': _r(T, PH, x=(0, 12, -18, -10, -2, 0)), 'forcipule_g': _r(T, PH, y=(0, 26, -24, -8, 0, 0)), 'forcipule_d': _r(T, PH, y=(0, -26, 24, 8, 0, 0))}
    for i, s_ in enumerate(segs[:5]):
        tr[s_] = _r(T, PH, x=(0, 7 - i, -9 + i, -5 + i * .5, -1, 0))
    tr['body'] = {'position': ev(T, PH, z=(0, 4, -10, -8, -2, 0), y=(0, 3, 0, 0, 0, 0))}
    _fin(p, 'attack', T, tr, False)
    PH = (0, .3, .75, 1)
    T = 2.4
    tr = {'body': {'position': ev(T, PH, y=(0, 6, 6, 0))}, 'head': _r(T, PH, x=(0, 16, 16, 0))}
    for i, s_ in enumerate(segs[:6]):
        tr[s_] = _r(T, PH, x=(0, 8 - i, 8 - i, 0))
    _fin(p, 'cast', T, tr, False)
    T = 1.0
    PH = (0, .5, 1)
    tr = {'head': _r(T, PH, x=(0, -14, -24))}
    for i, (g, d) in enumerate(zip(gauches, droites)):
        tr[g] = _r(T, PH, z=(0, 22, 40))
        tr[d] = _r(T, PH, z=(0, -22, -40))
    for i, s_ in enumerate(segs):
        tr[s_] = _r(T, PH, y=(0, (1 if i % 2 else -1) * 3, (1 if i % 2 else -1) * 5))
    _fin(p, 'death', T, tr, False)
    return p


# ------------------------------------------------------------------ 6. Nymethis
def meduse_oracles():
    """Nymethis, Oracle Suspendu : grande cloche violette en gradins, oeil d'oracle et bandeau de runes lumineux, couronne de pointes, douze longs tentacules runiques."""
    m = M('meduse_oracles', 306, kind='yggdrasil', description='Nymethis, Oracle Suspendu, gardien d\'Yggdrasil : refonte (16 texels par bloc en jeu).')
    p = m.p
    V = ('voile', 'pourpre')
    p.bone('body', (0, 120, 0), 'root')
    m.b = 'body'
    m.c('cloche_haut', (-16, 150, -16), (16, 160, 16), V, base=3.6, pli=7)
    m.c('cloche_b', (-27, 140, -27), (27, 150, 27), V, base=3.2, pli=8)
    m.c('cloche_c', (-36, 124, -36), (36, 140, 36), V, base=2.8, pli=9)
    m.c('cloche_d', (-41, 114, -41), (41, 124, 41), V, base=2.3, pli=9)
    m.c('sous_cloche', (-30, 110, -30), (30, 114, 30), ('uni', 'nuit'), base=2.4)
    m.lueur('body', 'coeur', (-14, 96, -14), (14, 110, 14), ('cristal', 'amethyste'), base=2.6)
    for s, n in ((1, 'g'), (-1, 'd')):
        pass
    # bandeau de runes sur les quatre cotes, oeil d'oracle devant
    def runes(ctx, face):
        H, W = face.shape[:2]
        for x in range(2, W - 2, 6):
            for k, dy in enumerate(((0, 0), (1, 1), (0, 2), (2, 2), (1, 3))):
                yy, xx = dy[1] + 1, x + dy[0]
                if 0 <= yy < H and 0 <= xx < W:
                    face[yy, xx, :3] = (230, 210, 255)
    for nom_, a, b, h_ in (('runes_n', (-36, 128, -37), (36, 134, -36), {'south'}), ('runes_s', (-36, 128, 36), (36, 134, 37), {'north'}), ('runes_o', (-37, 128, -36), (-36, 134, 36), {'east'}), ('runes_e', (36, 128, -36), (37, 134, 36), {'west'})):
        c = m.lueur('body', nom_, a, b, ('uni', 'amethyste'), base=3, hide=h_)
    m.c('paupiere', (-12, 134, -38), (12, 138, -36), ('plaque', 'or'), base=3, hide={'south'})
    m.c('paupiere_b', (-12, 122, -38), (12, 126, -36), ('plaque', 'or'), base=2.6, hide={'south'})
    m.lueur('body', 'oeil', (-10, 126, -39), (10, 134, -37), ('uni', 'rose_chaos'), base=4.2, hide={'south'})
    m.c('pupille', (-2, 126, -40), (2, 134, -39), ('uni', 'nuit'), base=.6, no_ao=True, hide={'south'})
    # couronne de pointes
    for i in range(12):
        a = i * math.tau / 12
        x, z = round(30 * math.cos(a)), round(30 * math.sin(a))
        h = 16 + (i % 2) * 8
        rx, rz = -z * .5, -x * .5
        m.pointe('body', 'couronne', x, 148, z, h, 3, ('corne', 'or'), inc=(z * .6, -x * .6), base=2.8, tourne=0)
    # douze tentacules : trois os chacun, ecartes puis retombants
    tent = []
    for i in range(12):
        a = (i + .5) * math.tau / 12
        ca, sa = math.cos(a), math.sin(a)
        R0 = 26 if i % 2 else 33
        pts = [(R0 * ca, 112, R0 * sa), ((R0 + 6) * ca, 76, (R0 + 6) * sa), ((R0 + 4) * ca, 42, (R0 + 4) * sa), ((R0 - 2) * ca, 8 + (i % 3) * 4, (R0 - 2) * sa)]

        def cote(m_, os_, j, P, W, H, i=i):
            x, y, z = P
            for k in range(2):
                yy = y + H // 3 + k * H // 3
                m_.lueur(os_, f'rune_t{i}_{j}_{k}', (x - W[0] - 1, yy, z - 2), (x + W[0] + 1, yy + 3, z + 2), ('uni', 'amethyste'), base=5)
        ch = m.courbe(pts, [4, 3, 2], ('voile', 'amethyste'), prefixe=f't{i}_', parent='body', cote=cote, base=2.6, pli=4)
        tent.append([c[0] for c in ch])
    T = 3.0
    tr = {'body': {'position': rot(T, y=sin(3, 1, .2)), 'rotation': rot(T, y=sin(2, 1, 0))}}
    for i, t in enumerate(tent):
        a = (i + .5) * math.tau / 12
        for j, b in enumerate(t):
            tr[b] = {'rotation': rot(T, x=sin(3 + j, 1, i * .08 + j * .1), z=sin(3 + j, 1, i * .08 + j * .1 + .25))}
    _fin(p, 'idle', T, tr)
    T = 1.4
    tr = {'body': {'position': rot(T, y=sin(5, 1, .2)), 'rotation': rot(T, x=sin(2, 1, .1, bias=-7))}}
    for i, t in enumerate(tent):
        for j, b in enumerate(t):
            tr[b] = {'rotation': rot(T, x=sin(5 + j * 2, 1, j * .12, bias=-4 - j), z=sin(3 + j, 1, i * .08))}
    _fin(p, 'walk', T, tr)
    T = 1.0
    PH = P_ATT
    tr = {'body': {'position': ev(T, PH, y=(0, 8, -4, -2, 0, 0)), 'rotation': ev(T, PH, x=(0, 10, -14, -8, -2, 0))}}
    for i, t in enumerate(tent):
        a = (i + .5) * math.tau / 12
        ca, sa = math.cos(a), math.sin(a)
        tr[t[0]] = _r(T, PH, x=tuple(round(sa * v, 2) for v in (0, 20, -18, -8, 0, 0)), z=tuple(round(-ca * v, 2) for v in (0, 20, -18, -8, 0, 0)))
    _fin(p, 'attack', T, tr, False)
    T = 2.4
    PH = (0, .3, .75, 1)
    tr = {'body': {'position': ev(T, PH, y=(0, 10, 10, 0))}}
    for i, t in enumerate(tent):
        a = (i + .5) * math.tau / 12
        ca, sa = math.cos(a), math.sin(a)
        tr[t[0]] = _r(T, PH, x=tuple(round(sa * v, 2) for v in (0, 28, 28, 0)), z=tuple(round(-ca * v, 2) for v in (0, 28, 28, 0)))
    _fin(p, 'cast', T, tr, False)
    T = 1.0
    PH = (0, .5, 1)
    tr = {'body': {'position': ev(T, PH, y=(0, -6, -20)), 'rotation': ev(T, PH, x=(0, -6, -14))}}
    for i, t in enumerate(tent):
        tr[t[1]] = _r(T, PH, x=(0, 8, 16))
    _fin(p, 'death', T, tr, False)
    return p


# ------------------------------------------------------------------ 7. Ghorzakk
def crabe_bastion():
    """Ghorzakk, Bastion Vivant : crabe forteresse, carapace de plaques rivetees et de pierre, creneaux, donjon sur le dos, yeux sur pedoncules, deux enormes pinces, huit pattes."""
    m = M('crabe_bastion', 307, kind='yggdrasil', description='Ghorzakk, Bastion Vivant, gardien d\'Yggdrasil : refonte (16 texels par bloc en jeu).')
    p = m.p
    FE = ('plaque', 'fer_noir')
    FR_ = ('plaque', 'fer')
    PI = ('maconnerie', 'pierre')
    OR2 = ('plaque', 'cuivre')
    p.bone('body', (0, 34, -4), 'root')
    m.b = 'body'
    m.c('ventre', (-46, 22, -40), (46, 32, 30), ('segments', 'cuivre_sombre'), pas=8, base=3)
    m.c('carapace', (-56, 30, -48), (56, 56, 34), FE, base=2.6, bord=2, rivets=8)
    m.c('filet', (-57, 44, -49), (57, 48, 35), OR2, base=2.6, bord=0)
    m.c('rempart', (-44, 56, -40), (44, 68, 26), PI, assise=6, long=12, base=2.8)
    for s_, (xa, xb) in enumerate(((-44, -30), (-22, -8), (8, 22), (30, 44))):
        m.c(f'creneau_av{s_}', (xa, 68, -40), (xb, 74, -34), PI, assise=6, long=12, base=3.2)
        m.c(f'creneau_ar{s_}', (xa, 68, 20), (xb, 74, 26), PI, assise=6, long=12, base=3)
    for i, z in enumerate((-20, 0)):
        m.s(f'creneau_c{i}', (38, 68, z), (44, 74, z + 12), PI, assise=6, long=12, base=3)
    # donjon central
    m.c('donjon', (-16, 68, -16), (16, 84, 12), PI, assise=6, long=12, base=3.2)
    m.c('donjon_toit', (-18, 84, -18), (18, 87, 14), FE, base=3, rivets=6)
    m.lueur('body', 'meurtriere', (-3, 72, -17), (3, 80, -16), ('uni', 'braise'), base=4.2, hide={'south'})
    m.lueur_sym('body', 'meurtriere_c', (16, 72, -6), (17, 80, 0), ('uni', 'braise'), base=4.2, hide={'west'})
    for i, x in enumerate((-13, -3, 7)):
        m.pointe('body', 'pique_toit', x + 3, 87, -2, 7, 1, ('metal', 'fer'), base=3.4, tourne=0)
    # devant : bouche grillagee, yeux sur pedoncules
    p.bone('head', (0, 40, -48), 'body')
    m.b = 'head'
    m.c('front', (-26, 30, -56), (26, 54, -46), FR_, base=2.8, rivets=6)
    m.c('bouche', (-16, 32, -57), (16, 42, -56), ('uni', 'nuit'), base=.8, no_ao=True, hide={'south'})
    for i, x in enumerate((-13, -7, -1, 5, 11)):
        m.c(f'herse{i}', (x, 30, -58), (x + 2, 44, -57), FR_, base=3.2, bord=0, hide={'south'})
    for s, n in ((1, 'g'), (-1, 'd')):
        p.bone(f'pedoncule_{n}', (s * 16, 54, -50), 'head')
    m.sb('pedoncule_g|pedoncule_d', 'pedoncule', (14, 53, -52), (18, 76, -48), FE, base=3, bord=0, rot=(-8, 0, -10), pivot=(16, 54, -50))
    m.lueur_sym('pedoncule_g|pedoncule_d', 'oeil', (16, 74, -56), (26, 83, -47), ('cristal', 'braise'), base=3)
    # deux grandes pinces, la droite plus forte
    for s, n, os_ in ((1, 'g', 'pince_g'), (-1, 'd', 'pince_d')):
        p.bone(os_, (s * 44, 36, -40), 'body')
        p.bone(os_ + '_doigt', (s * 62, 46, -78), os_)
    m.sb('pince_g|pince_d', 'bras_pince', (40, 30, -66), (52, 44, -38), FE, base=2.8, rot=(0, -22, 0), pivot=(44, 36, -40))
    m.sb('pince_g|pince_d', 'poignet', (50, 28, -80), (68, 48, -62), FR_, base=2.8, rivets=4)
    m.sb('pince_g|pince_d', 'mors_fixe', (50, 24, -106), (70, 38, -80), FE, base=3, rivets=4)
    m.sb('pince_g|pince_d', 'mors_bout', (54, 24, -120), (68, 34, -106), ('griffe', 'fer'), base=3.2)
    m.sb('pince_g_doigt|pince_d_doigt', 'mors_mobile', (52, 44, -106), (70, 56, -78), OR2, base=2.4, rivets=4)
    m.sb('pince_g_doigt|pince_d_doigt', 'mors_mobile_bout', (56, 46, -118), (68, 54, -106), ('griffe', 'fer'), base=3.4)
    # huit pattes
    legs = []
    for i, (z, fan) in enumerate(((-30, 26), (-12, 10), (6, -8), (22, -24))):
        g, d = f'leg{i}l', f'leg{i}r'
        p.bone(g, (44, 32, z), 'body')
        p.bone(d, (-44, 32, z), 'body')
        L = 24
        xk = round(44 + L * math.cos(math.radians(fan)) * .86)
        zk = round(z - L * math.sin(math.radians(fan)) * .86)
        p.bone(g + '_bas', (xk, 44, zk), g)
        p.bone(d + '_bas', (-xk, 44, zk), d)
        m.sb(f'{g}|{d}', f'cuisse{i}', (42, 27, z - 4), (44 + L + 4, 37, z + 4), FE, base=2.8, bord=1, rot=(0, fan, 26), pivot=(44, 32, z))
        m.sb(f'{g}_bas|{d}_bas', f'tibia{i}', (xk - 3, 10, zk - 3), (xk + 4, 46, zk + 3), FR_, base=2.6, bord=1, rot=(0, 0, 10), pivot=(xk, 44, zk))
        m.sb(f'{g}_bas|{d}_bas', f'griffe{i}', (xk + 6, 0, zk - 2), (xk + 12, 12, zk + 2), ('griffe', 'fer'), base=2.6)
        legs += [(g, g + '_bas', 1), (d, d + '_bas', -1)]
    octo_mob(p, legs, head='head', claws=(('pince_g', 1), ('pince_d', -1)), jaw=(('pince_g_doigt', 1), ('pince_d_doigt', -1)), walk=.9, attack=1.0, cast=True, gr=3)
    return p


# ------------------------------------------------------------------ 8. Vespera
def papillon_eclipse():
    """Vespera, Mangeuse de Soleils : quatre ailes immenses bleu-nuit bordees d'or, ocelles d'eclipse lumineux, corps velu, antennes plumeuses, griffes."""
    m = M('papillon_eclipse', 308, kind='yggdrasil', description='Vespera, Mangeuse de Soleils, gardien d\'Yggdrasil : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.bone('body', (0, 86, 0), 'root')
    m.b = 'body'
    m.c('thorax', (-8, 72, -7), (8, 108, 8), ('poils_longs', 'pourpre'), base=2.8, y=(60, 120))
    m.c('collier', (-10, 100, -9), (10, 108, 10), ('poils_longs', 'or'), base=3, y=(60, 120))
    p.bone('abdomen', (0, 72, 0), 'body')
    m.cb('abdomen', 'abdo_a', (-6, 52, -5), (6, 73, 6), ('segments', 'pourpre'), pas=5, axe='y', base=2.6)
    m.cb('abdomen', 'abdo_b', (-4, 36, -3), (4, 53, 4), ('segments', 'nuit'), pas=5, axe='y', base=3)
    m.cb('abdomen', 'dard', (-1, 26, -1), (1, 37, 1), ('griffe', 'or'), base=2.4)
    p.bone('head', (0, 108, -3), 'body')
    m.b = 'head'
    m.c('crane', (-7, 108, -11), (7, 120, 3), ('plaque', 'nuit'), base=3)
    m.lueur_sym('head', 'oeil', (5, 110, -13), (10, 118, -4), ('cristal', 'rar_legendaire'), base=3)
    m.c('trompe', (-1, 100, -14), (1, 110, -11), ('uni', 'or'), base=2)
    for s, n in ((1, 'g'), (-1, 'd')):
        p.bone(f'antenne_{n}', (s * 3, 120, -8), 'head')
    AN = ['.#.', '###', '###', '.#.', '###', '###', '.#.', '###', '###', '.#.', '###', '.#.', '.#.', '.#.']
    m.sb('antenne_g|antenne_d', 'antenne', (1, 119, -9), (7, 160, -8), ('plan', 'or'), base=3.4, masque=AN, rot=(-10, 0, -22), pivot=(3, 120, -8), **CF0)
    # ailes en plans decoupes, ocelles d'eclipse sur leur propre plan
    def masque_avant():
        rows = []
        W, H = 44, 36
        for j in range(H):
            t = j / (H - 1)
            xmax = W * (.45 + .55 * min(1, t * 1.8)) if t < .55 else W * (1 - (t - .55) * 1.1)
            xmin = 0
            rows.append(''.join('#' if xmin <= i <= xmax - abs(math.sin(j * .9)) * 2 else '.' for i in range(W)))
        return rows

    def masque_arriere():
        rows = []
        W, H = 34, 40
        for j in range(H):
            t = j / (H - 1)
            xmax = W * (1 - t ** 1.6 * .75) - (2 if j % 7 < 2 else 0)
            rows.append(''.join('#' if i <= xmax else '.' for i in range(W)))
        return rows
    MA, MR = masque_avant(), masque_arriere()
    AI = dict(hide={'east', 'west', 'up', 'down'}, no_shadow=True)
    p.bone('aile_av_g', (6, 94, 2), 'body')
    p.bone('aile_av_d', (-6, 94, 2), 'body')
    p.bone('aile_ar_g', (6, 78, 2), 'body')
    p.bone('aile_ar_d', (-6, 78, 2), 'body')
    m.sb('aile_av_g|aile_av_d', 'aile_avant', (6, 76, 2), (94, 161, 3), ('aile_insecte', 'tissu_bleu'), masque=MA, base=2.4, alpha=255, nervure=2.6, n=7, **AI)
    m.sb('aile_ar_g|aile_ar_d', 'aile_arriere', (6, 10, 3), (72, 82, 4), ('aile_insecte', 'pourpre'), masque=MR, base=2.8, alpha=255, nervure=2.4, n=6, **AI)
    for nom_, os_, x, y, r in (('ocelle_av', 'aile_av_g|aile_av_d', 54, 122, 13), ('ocelle_ar', 'aile_ar_g|aile_ar_d', 32, 50, 11)):
        m.sb(os_, nom_ + '_or_lueur', (x - r, y - r, 1), (x + r, y + r, 2), ('plan', 'rar_legendaire'), base=4.2, masque=rond(16), **CF0)
        m.sb(os_, nom_ + '_noir', (x - r + 3, y - r + 3, 0), (x + r - 3, y + r - 3, 1), ('plan', 'nuit'), base=.8, masque=rond(12), **CF0)
        m.sb(os_, nom_ + '_c_lueur', (x - 3, y - 3, -1), (x + 3, y + 3, 0), ('plan', 'rar_legendaire'), base=4.6, masque=rond(6), **CF0)
    # griffes : trois paires de pattes repliees devant
    pattes = []
    for i, (y, r) in enumerate(((98, 8), (86, 0), (76, -8))):
        g, d = f'griffe{i}_g', f'griffe{i}_d'
        p.bone(g, (7, y, -5), 'body')
        p.bone(d, (-7, y, -5), 'body')
        pattes += [g, d]
        m.sb(f'{g}|{d}', f'bras{i}', (6, y - 26, -7), (9, y + 1, -4), ('plaque', 'nuit'), base=3.4, bord=0, rot=(30, 0, -24), pivot=(7, y, -5))
        m.sb(f'{g}|{d}', f'griffe{i}', (6, y - 35, -7), (9, y - 26, -4), ('griffe', 'or'), base=2.6, rot=(30, 0, -24), pivot=(7, y, -5))
    volant(p, [('aile_av_g', 'aile_av_d', 0.0), ('aile_ar_g', 'aile_ar_d', .1)], queue=('abdomen',), pattes=pattes, idle=3.0, vol=.9, attack=1.0, battement=20, battement_idle=9, cadence=1, hover=4, nom_vol='walk', axe='y')
    ajoute_cast(p, 'body', 'head', gr=2.5)
    return p


# ------------------------------------------------------------------ 9. Azheryn
def serpent_couronnes():
    """Azheryn, Spirale des Mondes : serpent cramoisi dresse en spirale, ventre pale, trois couronnes d'or en orbite, tete a grandes cornes, gueule ouverte."""
    m = M('serpent_couronnes', 309, kind='yggdrasil', description='Azheryn, Spirale des Mondes, gardien d\'Yggdrasil : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (0, 210)
    EC = ('ecailles', 'rouge')
    VE = ('nacre', 'chair_pale')
    n = 15
    pts, lar = [], []
    for i in range(n + 1):
        t = i / n
        a = t * math.tau * 2.1 + .6
        r = 25 - t * 12
        pts.append((round(r * math.cos(a)), round(11 + t * 150), round(r * math.sin(a))))
        lar.append(round(13 - t * 4))

    def cote(m_, os_, i, P, W, H):
        x, y, z = P
        m_.cb(os_, f'ventre{i}', (x - W[0] + 2, y, z - W[1] - 1), (x + W[0] - 2, y + H - 1, z - W[1]), VE, h=4, axe='y', base=3.2, hide={'south'})
        m_.pointe(os_, f'pic{i}', x, y + H // 2, z + W[1] - 1, 7 + (i % 2) * 4, 2, ('griffe', 'or'), inc=(64, 0), base=2.6, tourne=0)
        if i % 3 == 1:
            m_.lueur_sym(os_, f'rune{i}', (W[0] + x, y + H // 2 - 2, z - 2), (W[0] + x + 1, y + H // 2 + 2, z + 2), ('uni', 'rose_chaos'), base=4.2, hide={'west'}) if abs(x) < 1e6 and x == 0 else None
    ch = m.courbe(pts, lar, EC, prefixe='spire', parent='root', cote=cote, l=5, h=5, y=(0, 210), base=2.6)
    x, y, z = pts[-1]
    p.bone('head', (x, y, z), ch[-1][0])
    m.b = 'head'
    m.c('crane', (x - 16, y - 2, z - 24), (x + 16, y + 18, z + 8), EC, l=5, h=5, y=(0, 210), base=3)
    m.c('museau', (x - 11, y - 1, z - 44), (x + 11, y + 12, z - 24), EC, l=4, h=4, y=(0, 210), base=3.2)
    m.c('front', (x - 12, y + 17, z - 22), (x + 12, y + 21, z + 4), OR_, base=2.8)
    m.c('levre', (x - 11, y - 2, z - 44), (x + 11, y - 1, z - 24), ('uni', 'rose_chaos'), base=2.4)
    m.lueur('head', 'oeil_a', (x + 16, y + 6, z - 22), (x + 17, y + 13, z - 12), ('uni', 'rose_chaos'), base=4.4, hide={'west'})
    m.lueur('head', 'oeil_b', (x - 17, y + 6, z - 22), (x - 16, y + 13, z - 12), ('uni', 'rose_chaos'), base=4.4, hide={'east'})
    for i in range(4):
        xd = x + 7 - i * 4
        m.c(f'croc{i}', (xd - 1, y - 9, z - 43), (xd + 1, y - 2, z - 41), ('griffe', 'ivoire'), base=4.2)
    for sg, c in ((1, 'g'), (-1, 'd')):
        a0, a1 = sorted((x + sg * 8, x + sg * 14))
        m.c(f'corne_{c}', (a0, y + 16, z - 6), (a1, y + 34, z), ('corne', 'or'), base=2.6, rot=(-28, 0, -sg * 16), pivot=((a0 + a1) / 2, y + 16, z - 3))
        m.c(f'corne2_{c}', (a0 + 1, y + 33, z - 5), (a1 - 1, y + 48, z - 1), ('corne', 'or'), base=3.4, rot=(-28, 0, -sg * 16), pivot=((a0 + a1) / 2, y + 16, z - 3))
    p.bone('machoire', (x, y - 2, z - 10), 'head')
    m.cb('machoire', 'mandibule', (x - 10, y - 12, z - 42), (x + 10, y - 2, z - 6), EC, l=4, h=4, y=(0, 210), base=2.2)
    m.cb('machoire', 'gueule', (x - 7, y - 2, z - 40), (x + 7, y - 1, z - 10), ('uni', 'rose_chaos'), base=2.6, hide={'down'})
    # trois couronnes en orbite, plan horizontal, gemmes lumineuses
    couronnes = []
    for i, (yc, R) in enumerate(((46, 37), (92, 32), (132, 27))):
        os_ = f'couronne{i}'
        p.bone(os_, (0, yc, 0), 'root')
        couronnes.append(os_)
        anneau(m, os_, 0, yc, 0, R, 4, 3, 12 - i * 2, OR_, plan='xz', base=3)
        for k in range(4):
            a = k * math.tau / 4 + .4
            xg, zg = round(R * math.cos(a)), round(R * math.sin(a))
            m.lueur(os_, f'gemme{i}_{k}', (xg - 3, yc - 3, zg - 3), (xg + 3, yc + 5, zg + 3), ('cristal', 'rose_chaos'), base=2.8, rot=(0, 45, 0), pivot=(xg, yc, zg))
    chaine = [c[0] for c in ch]
    serpent(p, chaine[2:], head='head', jaw='machoire', nom_move='walk', move=.9, oscille=1.2, oscille_idle=.6, leve=chaine[-3:], retard=.07)
    for a in p.anims:
        if a['name'] in ('idle', 'walk'):
            T = a['length']
            for i, c in enumerate(couronnes):
                s = 1 if i % 2 else -1
                a['tracks'][c] = {'rotation': keys(T, lambda q, s=s: (0, s * 360 * q, 0), 16)}
    PH = (0, .3, .75, 1)
    extra = {c: {'rotation': keys(2.4, lambda q, s=(1 if i % 2 else -1): (0, s * 720 * q, 0), 16)} for i, c in enumerate(couronnes)}
    extra['machoire'] = _r(2.4, PH, x=(0, -22, -22, 0))
    for b in chaine[-4:]:
        extra[b] = _r(2.4, PH, x=(0, 6, 6, 0))
    ajoute_cast(p, 'head', None, gr=2, extra=extra)
    return p


# ------------------------------------------------------------------ 10. Elyndraxx
def oeil_yggdrasil():
    """Elyndraxx, Coeur de l'Arbre-Monde : grand oeil d'or vertical dans un cadre de bois, trois anneaux celestes qui tournent en sens contraires, douze branches fractales couronnees de gemmes."""
    m = M('oeil_yggdrasil', 310, kind='yggdrasil', description='Elyndraxx, Coeur de l\'Arbre-Monde, gardien d\'Yggdrasil : refonte (16 texels par bloc en jeu).')
    p = m.p
    CY = 107
    B = ('ecorce', 'bois')
    p.bone('body', (0, CY, 0), 'root')
    p.bone('head', (0, CY, -6), 'body')
    p.bone('paupiere_h', (0, CY + 30, -10), 'head')
    p.bone('paupiere_b', (0, CY - 30, -10), 'head')
    m.b = 'body'
    m.c('coeur', (-30, CY - 32, -8), (30, CY + 32, 10), B, ton=.4, pas=6)
    m.c('coeur_v', (-22, CY - 40, -8), (22, CY + 40, 10), B, ton=.2, pas=6)
    m.s('coeur_c', (30, CY - 22, -7), (36, CY + 22, 9), B, ton=.1, pas=6)
    m.b = 'head'
    m.lueur('head', 'blanc', (-20, CY - 26, -11), (20, CY + 26, -8), ('uni', 'or'), base=4.6, hide={'south'})
    m.lueur('head', 'iris', (-13, CY - 22, -13), (13, CY + 22, -11), ('cristal', 'ambre'), base=3, hide={'south'})
    m.c('pupille', (-4, CY - 20, -14), (4, CY + 20, -13), ('uni', 'nuit'), base=.6, no_ao=True, hide={'south'})
    for sx, sy in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        x0, x1 = sorted((sx * 21, sx * 10))
        y0, y1 = sorted((CY + sy * 27, CY + sy * 15))
        m.c('coin', (x0, y0, -14), (x1, y1, -9), B, ton=.9, pas=3, hide={'south'})
    m.cb('paupiere_h', 'paupiere_haut', (-20, CY + 25, -15), (20, CY + 34, -8), OR_, base=2.8, rivets=6)
    m.cb('paupiere_b', 'paupiere_bas', (-20, CY - 34, -15), (20, CY - 25, -8), OR_, base=2.4, rivets=6)
    # trois anneaux celestes, chacun sur son os
    anneaux = []
    for i, (R, e, n, z) in enumerate(((46, 7, 14, -4), (62, 6, 18, 0), (78, 7, 22, -2))):
        os_ = f'anneau{i}'
        p.bone(os_, (0, CY, 0), 'body')
        anneaux.append(os_)
        anneau(m, os_, 0, CY, z, R, e, 6, n, OR_ if i != 1 else ('plaque', 'cuivre'), plan='xy', base=2.8)
        for k in range(6):
            a = k * math.tau / 6 + i * .3
            xg, yg = round(R * math.cos(a)), round(CY + R * math.sin(a))
            m.lueur(os_, f'gemme{i}_{k}', (xg - 4, yg - 4, z - 5), (xg + 4, yg + 4, z + 3), ('cristal', 'ame'), base=2.8)
    # douze branches fractales, base pres de l'anneau exterieur
    for i in range(12):
        a = i * math.tau / 12
        ca, sa = math.cos(a), math.sin(a)
        deg = math.degrees(a) - 90
        os_ = f'branche{i}'
        R0 = 81
        x0, y0 = round(R0 * ca), round(CY + R0 * sa)
        p.bone(os_, (x0, y0, 2), 'body', rot=(0, 0, deg))
        m.cb(os_, f'tronc{i}', (x0 - 4, y0 - 2, -2), (x0 + 4, y0 + 16, 6), B, ton=.6, pas=3)
        m.cb(os_, f'tronc_b{i}', (x0 - 3, y0 + 16, -1), (x0 + 3, y0 + 26, 5), B, ton=.9, pas=3)
        m.cb(os_, f'rameau_a{i}', (x0 + 2, y0 + 10, 0), (x0 + 5, y0 + 26, 4), B, ton=1.1, pas=2, rot=(0, 0, -34), pivot=(x0 + 3, y0 + 10, 2))
        m.lueur(os_, f'fruit{i}', (x0 - 3, y0 + 26, -1), (x0 + 3, y0 + 32, 5), ('cristal', 'ame'), base=2.8)
        m.cb(os_, f'feuilles{i}', (x0 - 7, y0 + 18, 0), (x0 + 7, y0 + 25, 4), ('feuilles', 'feuillage'), ton=.2)
    T = 3.0
    tr = {'body': {'position': rot(T, y=sin(2, 1, .2)), 'rotation': rot(T, y=sin(3, 1, 0))},
          'head': {'rotation': rot(T, y=sin(10, 1, .1), x=sin(5, 2, .2))},
          'paupiere_h': {'position': rot(T, y=pulse(-14, .62, .05), n=24)},
          'paupiere_b': {'position': rot(T, y=pulse(10, .62, .05), n=24)}}
    for i, a in enumerate(anneaux):
        s = 1 if i % 2 else -1
        tr[a] = {'rotation': keys(T, lambda q, s=s, i=i: (0, 0, s * 360 * q), 18)}
    _fin(p, 'idle', T, tr)
    T = .9
    tr = {'body': {'position': rot(T, y=sin(2, 1, .2)), 'rotation': rot(T, x=sin(2, 1, 0, bias=-4))}}
    for i, a in enumerate(anneaux):
        s = 1 if i % 2 else -1
        tr[a] = {'rotation': keys(T, lambda q, s=s: (0, 0, s * 360 * q), 18)}
    _fin(p, 'walk', T, tr)
    T = 1.0
    PH = P_ATT
    tr = {'body': {'rotation': ev(T, PH, x=(0, 10, -14, -9, -2, 0)), 'position': ev(T, PH, z=(0, 6, -12, -9, -2, 0))},
          'paupiere_h': {'position': ev(T, PH, y=(0, -6, 5, 5, 1, 0))}, 'paupiere_b': {'position': ev(T, PH, y=(0, 5, -4, -4, -1, 0))}}
    for i, a in enumerate(anneaux):
        s = 1 if i % 2 else -1
        tr[a] = _r(T, PH, z=tuple(s * v for v in (0, 90, 300, 330, 350, 360)))
    _fin(p, 'attack', T, tr, False)
    T = 2.4
    PH = (0, .3, .75, 1)
    tr = {'body': {'position': ev(T, PH, y=(0, 6, 6, 0))}, 'paupiere_h': {'position': ev(T, PH, y=(0, 5, 5, 0))}, 'paupiere_b': {'position': ev(T, PH, y=(0, -5, -5, 0))}}
    for i, a in enumerate(anneaux):
        s = 1 if i % 2 else -1
        tr[a] = {'rotation': keys(T, lambda q, s=s: (0, 0, s * 720 * q), 24)}
    _fin(p, 'cast', T, tr, False)
    T = 1.0
    PH = (0, .5, 1)
    tr = {'body': {'position': ev(T, PH, y=(0, -10, -30))}, 'paupiere_h': {'position': ev(T, PH, y=(0, -14, -22))}, 'paupiere_b': {'position': ev(T, PH, y=(0, 10, 16))}}
    _fin(p, 'death', T, tr, False)
    return p


# ------------------------------------------------------------------ cristal d'arene
def _cristal(nom, seed, ramp, ramp_rune):
    m = M(nom, seed, kind='yggdrasil', description="Cristal d'arene Aeloria : refonte (16 texels par bloc en jeu).")
    p = m.p
    C = ('cristal', ramp)
    CAGE = ('plaque', 'fer_noir')
    ORB = ('metal', 'or')
    p.bone('body', (0, 34, 0), 'root')
    m.b = 'body'
    # socle et pointe, cage a quatre montants
    m.c('socle', (-14, 3, -14), (14, 9, 14), CAGE, base=2.8, rivets=4)
    m.c('socle_b', (-10, 9, -10), (10, 13, 10), ('plaque', 'or'), base=2.6)
    m.c('chapeau', (-10, 56, -10), (10, 60, 10), ('plaque', 'or'), base=2.6)
    m.c('chapeau_b', (-6, 60, -6), (6, 63, 6), CAGE, base=3)
    m.pointe('body', 'pointe', 0, 63, 0, 10, 2, ORB, base=3.4, tourne=0)
    for sx, sz in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        x0, x1 = sorted((sx * 12, sx * 16))
        z0, z1 = sorted((sz * 12, sz * 16))
        m.c('montant', (x0, 9, z0), (x1, 60, z1), CAGE, base=2.6, bord=0)
    p.bone('Crystal_heart', (0, 34, 0), 'body')
    m.lueur('Crystal_heart', 'coeur', (-9, 20, -9), (9, 48, 9), C, base=2.2, rot=(0, 45, 0), pivot=(0, 34, 0))
    m.lueur('Crystal_heart', 'coeur_h', (-5, 48, -5), (5, 55, 5), C, base=3, rot=(0, 45, 0), pivot=(0, 34, 0))
    m.lueur('Crystal_heart', 'coeur_b', (-5, 13, -5), (5, 20, 5), C, base=1.6, rot=(0, 45, 0), pivot=(0, 34, 0))
    # gyroscopes
    for i, (y, R) in enumerate(((20, 24), (46, 22))):
        os_ = f'Gyroscope_{i}'
        p.bone(os_, (0, y, 0), 'body')
        anneau(m, os_, 0, y, 0, R, 3, 4, 10, ('plaque', 'or'), plan='xz', base=2.8)
    # eclats en orbite
    eclats = []
    for i in range(6):
        os_ = f'Orbiting_shard_{i}'
        a = i * math.tau / 6
        x, z = round(26 * math.cos(a)), round(26 * math.sin(a))
        y = 28 + (i % 2) * 12
        p.bone(os_, (0, 34, 0), 'body')
        eclats.append(os_)
        m.lueur(os_, f'eclat{i}', (x - 2, y, z - 2), (x + 2, y + 10, z + 2), C, base=2.4, rot=(0, 45, 0), pivot=(x, y, z))
        m.lueur(os_, f'eclat_p{i}', (x - 1, y + 10, z - 1), (x + 1, y + 13, z + 1), C, base=3.2, rot=(0, 45, 0), pivot=(x, y, z))
    T = 3.0
    tr = {'Crystal_heart': {'rotation': keys(T, lambda q: (0, 360 * q, 0), 16), 'position': rot(T, y=sin(1.2, 1, .2))},
          'Gyroscope_0': {'rotation': keys(T, lambda q: (0, -360 * q, 0), 16)}, 'Gyroscope_1': {'rotation': keys(T, lambda q: (0, 360 * q, 0), 16)}}
    for i, e in enumerate(eclats):
        tr[e] = {'rotation': keys(T, lambda q: (0, 360 * q, 0), 16), 'position': rot(T, y=sin(1.5, 2, i * .16))}
    _fin(p, 'idle', T, tr)
    T = .9
    _fin(p, 'walk', T, {'body': {'position': rot(T, y=sin(.6, 1, .2))}})
    T = 1.0
    PH = P_ATT
    _fin(p, 'attack', T, {'body': {'rotation': ev(T, PH, y=(0, 40, 200, 280, 340, 360))}}, False)
    T = 2.4
    PH = (0, .3, .75, 1)
    _fin(p, 'cast', T, {'body': {'position': ev(T, PH, y=(0, 3, 3, 0))}}, False)
    T = 1.0
    PH = (0, .5, 1)
    _fin(p, 'death', T, {'Crystal_heart': {'position': ev(T, PH, y=(0, -4, -12))}}, False)
    return p


def crystal_neutral():
    return _cristal('crystal_neutral', 311, 'toison', 'toison')


def crystal_red():
    return _cristal('crystal_red', 311, 'rouge', 'rouge')


def crystal_blue():
    return _cristal('crystal_blue', 311, 'ame', 'ame')


def arena_drone():
    """Drone d'arene : petite coque de fer et d'or, lentille d'ambre, ailerons, rotor au-dessus. Flotte."""
    m = M('arena_drone', 312, kind='yggdrasil', description="Drone d'arene Aeloria : refonte (16 texels par bloc en jeu).")
    p = m.p
    p.bone('body', (0, 8, 0), 'root')
    m.b = 'body'
    m.c('coque', (-6, 4, -6), (6, 12, 5), ('plaque', 'fer_noir'), base=2.8, bord=0)
    m.c('coque_h', (-5, 12, -5), (5, 13, 4), ('plaque', 'or'), base=2.8, bord=0)
    m.c('coque_b', (-4, 3, -4), (4, 4, 3), ('plaque', 'fer'), base=2.4, bord=0)
    p.bone('head', (0, 8, -6), 'body')
    m.cb('head', 'bague', (-4, 5, -8), (4, 11, -6), ('plaque', 'or'), base=2.6, bord=0)
    m.lueur('head', 'lentille', (-3, 6, -9), (3, 10, -8), ('uni', 'ambre'), base=4, hide={'south'})
    m.cb('head', 'pupille', (-1, 7, -10), (1, 9, -9), ('uni', 'nuit'), base=.8, no_ao=True, hide={'south'})
    m.s('aileron', (6, 6, -3), (9, 10, 3), ('plaque', 'fer'), base=2.8, bord=0)
    m.c('queue', (-1, 7, 5), (1, 11, 8), ('plaque', 'fer'), base=2.8, bord=0)
    p.bone('rotor', (0, 13, 0), 'body')
    m.cb('rotor', 'axe', (-1, 13, -1), (1, 15, 1), ('metal', 'fer'), base=3)
    m.cb('rotor', 'pale', (-8, 15, -1), (8, 16, 1), ('metal', 'acier'), base=3.6)
    T = 3.0
    _fin(p, 'idle', T, {'body': {'position': rot(T, y=sin(.8, 1, .2)), 'rotation': rot(T, z=sin(2, 1, .3))}, 'head': {'rotation': rot(T, y=sin(8, 1, 0))}, 'rotor': {'rotation': keys(T, lambda q: (0, 360 * 12 * q, 0), 24)}})
    T = .9
    _fin(p, 'walk', T, {'body': {'rotation': rot(T, x=sin(2, 1, 0, bias=-9)), 'position': rot(T, y=sin(.6, 1, .2))}, 'rotor': {'rotation': keys(T, lambda q: (0, 360 * 6 * q, 0), 24)}})
    T = 1.0
    PH = P_ATT
    _fin(p, 'attack', T, {'body': {'rotation': ev(T, PH, x=(0, 8, -14, -8, -2, 0)), 'position': ev(T, PH, z=(0, 1, -2, -2, 0, 0))}}, False)
    T = 2.4
    PH = (0, .3, .75, 1)
    _fin(p, 'cast', T, {'body': {'position': ev(T, PH, y=(0, 2, 2, 0))}}, False)
    T = 1.0
    PH = (0, .5, 1)
    _fin(p, 'death', T, {'body': {'rotation': ev(T, PH, z=(0, 20, 50)), 'position': ev(T, PH, y=(0, -2, -4))}}, False)
    return p


YGGDRASIL = {'scarabee_racines': scarabee_racines, 'mante_epines': mante_epines, 'raie_celeste': raie_celeste, 'hydre_corail': hydre_corail, 'millepattes_ambre': millepattes_ambre, 'meduse_oracles': meduse_oracles, 'crabe_bastion': crabe_bastion, 'papillon_eclipse': papillon_eclipse, 'serpent_couronnes': serpent_couronnes, 'oeil_yggdrasil': oeil_yggdrasil, 'crystal_neutral': crystal_neutral, 'crystal_red': crystal_red, 'crystal_blue': crystal_blue, 'arena_drone': arena_drone}
