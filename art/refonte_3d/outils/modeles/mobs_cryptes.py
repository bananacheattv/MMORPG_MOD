#!/usr/bin/env python3
"""Cryptes : osselet, araignee d'os, garde funeraire, spectre enchaine, roi des cryptes. En texels de jeu."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from clips_mob import bipede, octo_mob
from kit_mob import CF, CR, FR, M, losange, triangle
from materiaux import stamp

OS = ('os', 'os')
OSV = ('os', 'os_vieux')
FN = ('plaque', 'fer_noir')
ORM = ('plaque', 'or')
ACI = ('metal', 'acier')
GL = ('glace', 'glace')
K = {'k': '#0b0a0f', 'n': '#1a1820', 'b': '#3ac6d4', 'c': '#dcffff', 'd': '#2a2520'}
CRANE = ['.kk.kk.', '.kk.kk.', '...k...', '.......']


def cage(m, y0, n, w, d, pas=3, h=2, mat=OS, nom='cote', zf=0, sternum=True):
    """Cage thoracique : n cotes (avant + deux cotes), colonne derriere, sternum devant. Rien n'est plein : on voit a travers."""
    y = y0
    for i in range(n):
        w_i = max(2, w - (0 if i >= n - 2 else 1 if i == n - 3 else 1 + (n - 3 - i)))
        m.c(nom + '_av', (-w_i, y, zf - d), (w_i, y + h, zf - d + 1), mat, ton=.2, hide={'south'})
        m.s(nom + '_flanc', (w_i - 1, y, zf - d + 1), (w_i, y + h, zf + d - 1), mat, ton=-.1)
        y += pas
    if sternum:
        m.c(nom + '_sternum', (-1, y0, zf - d - 1), (1, y - pas + h, zf - d), mat, ton=.6, hide={'south'})


def crane(m, os_, x, y0, y1, z0, z1, mat=OS, oeil='ame', ton=.2, yeux=True, dents=True, oeil_w=None, oeil_h=None, oeil_dz=2):
    """Crane : boite principale, pommettes, mandibule a part (machoire), orbites creuses, nez, dents."""
    m.cb(os_, 'crane', (-x, y0 + 3, z0), (x, y1, z1), mat, ton=ton)
    m.cb(os_, 'face', (-x + 1, y0, z0), (x - 1, y0 + 3, z0 + 5), mat, ton=ton - .2)
    h = y1 - y0
    oh = max(2, h // 4)
    ow = max(2, x * 2 // 5)
    oy = y0 + 3 + (h - 3) // 3
    m.cb(os_, 'orbites', (-x + 1, oy, z0 - 1), (x - 1, oy + oh + 1, z0), ('uni', 'nuit'), base=.6, no_ao=True, hide={'south'})
    m.cb(os_, 'nez', (-1, oy - 2, z0 - 1), (1, oy, z0), ('uni', 'nuit'), base=.6, no_ao=True, hide={'south'})
    if yeux:
        m.yeux(os_, max(1, x // 3), oy, z0 - 1, oeil_w or ow, oeil_h or oh, ramp=oeil, base=3.4, dz=oeil_dz)
    if dents:
        n = (2 * x - 2) // 2
        for i in range(n):
            xd = -x + 1 + i * 2
            m.cb(os_, 'dent', (xd, y0 - 1, z0 - 1), (xd + 1, y0 + 1, z0), mat, ton=.8, no_ao=True, hide={'south'})


def epee(m, os_, cx, y0, L, l=2, ep=1, cz=0, lame=ACI, garde=('metal', 'or'), pommeau=('metal', 'or'), large=None, lueur=False):
    """Epee le long de +y depuis la poignee : pommeau, fusee, garde, lame et pointe. L'os porte la bascule qui l'incline."""
    hw = l + 1 if large is None else large
    m.cb(os_, 'pommeau', (cx - 1, y0 - 2, cz - 1), (cx + 1, y0, cz + 1), pommeau)
    m.cb(os_, 'fusee', (cx - 1, y0, cz - 1), (cx + 1, y0 + 5, cz + 1), ('cuir', 'cuir'), couture=False)
    m.cb(os_, 'garde', (cx - hw - 1, y0 + 5, cz - 2), (cx + hw + 1, y0 + 7, cz + 2), garde)
    (m.lueur if lueur else m.cb)(os_, 'lame', (cx - l, y0 + 7, cz - ep), (cx + l, y0 + 7 + L - l * 2, cz + ep), lame)
    (m.lueur if lueur else m.cb)(os_, 'pointe', (cx - max(1, l // 2), y0 + 7 + L - l * 2, cz - ep), (cx + max(1, l // 2), y0 + 7 + L, cz + ep), lame, ton=.5)


def osselet():
    """Osselet : petit squelette a grosse tete et casque sombre, cage ouverte, epee courte rouillee a la main droite."""
    m = M('osselet', 221, 2.483, description='Osselet Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=17, epaule=29, cou=31, x_bras=8, x_jambe=3, coude=20, genou=9)
    m.sb('leg_-1|leg_1', 'femur', (2, 9, -1), (4, 18, 1), OS)
    m.sb('leg_-1|leg_1', 'genou', (1, 8, -2), (5, 11, 2), OS, ton=.6)
    m.sb('tibia_-1|tibia_1', 'tibia', (2, 2, -1), (4, 9, 1), OS, ton=-.2)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -4), (5, 2, 2), OS, ton=.2)
    m.b = 'body'
    m.c('colonne', (-1, 16, 0), (1, 30, 2), OS, ton=-.3)
    m.c('bassin', (-5, 15, -2), (5, 19, 2), OS, ton=.2)
    m.c('pagne', (-4, 8, -3), (4, 17, -2), FR('tissu_brun'), base=2.4, mini=.5, **CF)
    m.c('pagne_dos', (-4, 9, 2), (4, 17, 3), FR('tissu_brun'), base=2, mini=.5, **CF)
    m.b = 'body'
    cage(m, 21, 3, 5, 3)
    m.c('clavicule', (-7, 28, -2), (7, 30, 2), OS, ton=.4)
    m.b = 'head'
    crane(m, 'head', 6, 31, 42, -6, 5)
    m.c('casque', (-7, 39, -7), (7, 44, 6), FN, base=2.4)
    m.c('casque_nuque', (-7, 33, 3), (7, 39, 6), FN, base=2.2)
    m.s('casque_joue', (6, 33, -5), (7, 39, 3), FN, base=2)
    m.c('casque_nasal', (-1, 36, -8), (1, 39, -7), FN, base=3, hide={'south'})
    m.c('casque_crete', (-1, 44, -6), (1, 46, 5), ('metal', 'fer'), base=2.6)
    m.sb('arm_-1|arm_1', 'humerus', (7, 20, -1), (9, 29, 1), OS, ton=.1)
    m.sb('arm_-1|arm_1', 'epaule', (6, 27, -2), (10, 31, 2), OS, ton=.5)
    m.sb('avbras_-1|avbras_1', 'radius', (7, 11, -1), (9, 20, 1), OS, ton=-.2)
    m.sb('avbras_-1|avbras_1', 'main', (6, 8, -2), (10, 11, 2), OS, ton=.2)
    p.bone('epee', (8, 9, 0), 'avbras_-1', rot=(-62, 0, 0))
    epee(m, 'epee', 8, 9, 18, l=2, lame=('metal', 'fer'), garde=('metal', 'fer'), pommeau=('metal', 'fer'))
    bipede(p, style='epee', amp=30, genou=46, bras=26, jambe=17, jaw=None, arme='epee')
    return p


def araignee_os():
    """Araignee d'os : grand crane de face, yeux d'ame, abdomen de cotes et de vertebres, huit longues pattes articulees en phalanges."""
    m = M('araignee_os', 222, 3.556, description="Araignee d'os Aeloria : refonte (16 texels par bloc en jeu).")
    p = m.p
    p.bone('body', (0, 13, 0), 'root')
    m.b = 'body'
    m.c('thorax', (-6, 9, -6), (6, 18, 5), OS, ton=.1)
    # abdomen : vertebre centrale, cotes en arceaux, ombre dedans
    m.c('vertebres', (-2, 16, 3), (2, 21, 21), OS, ton=.5)
    m.c('coeur_ombre', (-6, 9, 5), (6, 17, 19), ('uni', 'nuit'), base=1, no_ao=True)
    for i, z in enumerate((5, 9, 13, 17)):
        w = (9, 10, 9, 7)[i]
        m.c(f'arc_h{i}', (-w, 18, z), (w, 20, z + 2), OS, ton=.3)
        m.s(f'arc_c{i}', (w - 2, 9, z), (w, 18, z + 2), OS, ton=0)
        m.c(f'arc_b{i}', (-w, 7, z), (w, 9, z + 2), OS, ton=-.3)
    m.lueur('body', 'ame', (-3, 11, 8), (3, 15, 15), ('uni', 'ame'), base=3.4)
    # tete : gros crane plein de face
    p.bone('head', (0, 14, -6), 'body')
    m.b = 'head'
    m.c('crane', (-10, 8, -17), (10, 25, -5), OS, ton=.4)
    m.c('crane_haut', (-8, 25, -15), (8, 27, -6), OS, ton=.8)
    m.c('orbites', (-8, 14, -18), (8, 21, -17), ('uni', 'nuit'), base=.6, no_ao=True, hide={'south'})
    m.yeux('head', 3, 15, -18, 4, 5, ramp='ame', base=3.2, dz=2)
    p.decal('oeil_lueur_g', 'north', stamp(['bbcb', 'bccb', 'bccb', 'bbbb', 'bbbb'], K, 7, 20))
    m.c('nez', (-1, 11, -18), (1, 14, -17), ('uni', 'nuit'), base=.6, no_ao=True, hide={'south'})
    p.bone('crochet_g', (5, 9, -14), 'head')
    p.bone('crochet_d', (-5, 9, -14), 'head')
    m.sb('crochet_g|crochet_d', 'crochet', (3, 2, -16), (7, 10, -12), OS, ton=.7, rot=(14, 0, -8), pivot=(5, 9, -14))
    m.sb('crochet_g|crochet_d', 'croc', (4, -1, -19), (6, 4, -17), ('griffe', 'ivoire'), base=3.4, rot=(18, 0, -8), pivot=(5, 9, -14))
    for i, x in enumerate((-8, -5, -2, 1, 4, 7)):
        m.cb('head', 'dent', (x, 6, -18), (x + 1, 10, -17), OS, ton=1, no_ao=True, hide={'south'})
    # huit pattes en phalanges : femur leve, tibia vers le sol, griffe
    legs = []
    for i, (z, fan, L) in enumerate(((-5, 34, 20), (-1, 12, 22), (3, -10, 22), (7, -32, 20))):
        g, d = f'leg{i}l', f'leg{i}r'
        p.bone(g, (7, 14, z), 'body')
        p.bone(d, (-7, 14, z), 'body')
        import math
        xk = round(7 + L * math.cos(math.radians(fan)) * .85)
        zk = round(z - L * math.sin(math.radians(fan)) * .85)
        p.bone(g + '_bas', (xk, 23, zk), g)
        p.bone(d + '_bas', (-xk, 23, zk), d)
        m.sb(f'{g}|{d}', f'femur{i}', (6, 13, z - 1), (6 + L, 15, z + 1), OS, ton=.2, rot=(0, fan, 24), pivot=(7, 14, z))
        m.sb(f'{g}_bas|{d}_bas', f'tibia{i}', (xk - 1, 3, zk - 1), (xk + 1, 24, zk + 1), OS, ton=.1, rot=(0, 0, 13), pivot=(xk, 23, zk))
        m.sb(f'{g}_bas|{d}_bas', f'griffe{i}', (xk + 4, 1, zk - 1), (xk + 6, 5, zk + 1), ('griffe', 'ivoire'), base=2.8, rot=(0, 0, 4), pivot=(xk + 5, 5, zk))
        legs += [(g, g + '_bas', 1), (d, d + '_bas', -1)]
    octo_mob(p, legs, head='head', jaw=(('crochet_g', 1), ('crochet_d', -1)), gr=1.2)
    return p


def garde_funeraire():
    """Garde funeraire : squelette solennel en armure d'or sur noir, casque a cimier, longue lance a la main droite, grand bouclier de tombe a la gauche."""
    m = M('garde_funeraire', 223, 2.227, description='Garde funeraire Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=28, epaule=47, cou=50, x_bras=10, x_jambe=4, coude=36, genou=14)
    # jambes : os, jambieres d'or, genouilleres, solerets
    m.sb('leg_-1|leg_1', 'femur', (2, 14, -2), (6, 29, 2), OS, ton=-.2)
    m.sb('leg_-1|leg_1', 'cuissard', (1, 18, -3), (7, 27, -1), ORM, base=2.8)
    m.sb('tibia_-1|tibia_1', 'tibia', (2, 3, -2), (6, 14, 2), OS, ton=-.3)
    m.sb('tibia_-1|tibia_1', 'jambiere', (1, 3, -3), (7, 12, -1), ORM, base=2.8)
    m.sb('tibia_-1|tibia_1', 'genouillere', (1, 11, -4), (7, 16, -2), ORM, base=3.4)
    m.sb('tibia_-1|tibia_1', 'soleret', (1, 0, -6), (7, 3, 3), ORM, base=2.6)
    # buste : cage visible sous une cuirasse d'or ouverte, jupe de lames
    m.b = 'body'
    m.c('bassin', (-7, 26, -3), (7, 33, 3), FN, base=2)
    m.c('colonne', (-1, 32, 0), (1, 47, 3), OS, ton=-.3)
    cage(m, 34, 4, 7, 4)
    m.c('cuirasse_dos', (-8, 33, 3), (8, 49, 5), ORM, base=2.6)
    m.s('cuirasse_flanc', (7, 33, -3), (9, 47, 4), ORM, base=2.2)
    m.c('col', (-7, 46, -5), (7, 49, 5), ORM, base=3.4, rivets=3)
    m.c('ceinture', (-8, 31, -5), (8, 34, 5), FN, base=2.6)
    m.c('boucle', (-3, 30, -6), (3, 35, -5), ORM, base=3.8, hide={'south'})
    for i, (x0, x1) in enumerate(((-8, -4), (-3, 3), (4, 8))):
        m.c(f'lame_av{i}', (x0, 18, -6), (x1, 31, -5), ORM, base=2.6 + (i == 1) * .8, hide={'south'})
    m.c('lame_dos', (-7, 19, 4), (7, 31, 5), FN, base=2.4)
    m.s('lame_flanc', (7, 20, -3), (8, 31, 3), FN, base=2.4)
    # tete : crane, casque d'or a cimier de crin
    crane(m, 'head', 6, 50, 62, -6, 5, oeil='ame')
    m.c2 = None
    m.cb('head', 'casque', (-7, 58, -7), (7, 63, 6), ORM, base=3)
    m.cb('head', 'visiere', (-7, 57, -8), (7, 59, -7), ORM, base=3.6, hide={'south'})
    m.cb('head', 'casque_nuque', (-7, 51, 3), (7, 58, 6), ORM, base=2.6)
    m.sb('head', 'casque_joue', (6, 51, -6), (7, 58, 3), ORM, base=2.4)
    m.cb('head', 'cimier', (-1, 63, -8), (1, 70, 10), ('crete', 'rouge'), base=2, ecart=2, pas=3, dent=3, **CR)
    # bras : os, spalieres d'or
    m.sb('arm_-1|arm_1', 'humerus', (9, 37, -2), (12, 47, 2), OS, ton=-.1)
    m.sb('arm_-1|arm_1', 'spaliere', (8, 44, -4), (14, 50, 4), ORM, base=3.2, rivets=3)
    m.sb('arm_-1|arm_1', 'spaliere_b', (10, 41, -3), (14, 44, 3), ORM, base=2.6)
    m.sb('avbras_-1|avbras_1', 'radius', (9, 26, -2), (12, 37, 2), OS, ton=-.2)
    m.sb('avbras_-1|avbras_1', 'canon', (8, 27, -3), (13, 35, 3), ORM, base=2.8)
    m.sb('avbras_-1|avbras_1', 'gant', (8, 22, -3), (13, 27, 3), FN, base=3)
    # lance dans la main droite (+x), devant le corps
    p.bone('lance', (10, 24, -6), 'avbras_-1')
    m.b = 'lance'
    m.c('hampe', (9, 2, -7), (11, 66, -5), ('bois', 'bois_sombre'), base=3, planche=2)
    m.c('bague', (8, 62, -8), (12, 66, -4), ORM, base=3.4)
    m.c('fer', (8, 66, -7), (12, 78, -5), ('plan', 'acier'), base=3.4, masque=['.##.'] * 3 + ['####'] * 3 + ['.##.'] * 2 + ['.##.'] * 1, hide={'east', 'west', 'up', 'down'}, no_shadow=True)
    m.c('fer_arete', (9, 66, -8), (11, 76, -4), ('metal', 'acier'), base=3)
    m.c('poing', (8, 21, -9), (12, 27, -3), FN, base=3)
    # bouclier de tombe dans la main gauche
    p.bone('bouclier', (-13, 27, 0), 'avbras_1')
    m.b = 'bouclier'
    m.c('ecu', (-16, 10, -9), (-14, 40, 9), FN, base=2.2, hide=None)
    m.c('ecu_bord', (-17, 10, -9), (-16, 12, 9), ORM, base=3)
    m.c('ecu_bord', (-17, 38, -9), (-16, 40, 9), ORM, base=3)
    m.c('ecu_bord_l', (-17, 12, -9), (-16, 38, -7), ORM, base=3)
    m.c('ecu_bord_l', (-17, 12, 7), (-16, 38, 9), ORM, base=3)
    m.c('croix_v', (-17, 15, -1), (-16, 35, 1), ORM, base=3.8)
    m.c('croix_h', (-17, 26, -5), (-16, 28, 5), ORM, base=3.8)
    m.lueur('bouclier', 'gemme', (-18, 25, -2), (-17, 29, 2), ('cristal', 'ame'), base=2.8, hide={'east'})
    bipede(p, style='estoc', amp=20, genou=34, bras=8, jambe=28, arme='lance')
    return p


def _chaine(m, os_, x, y1, n, z=0):
    """Chaine : maillons alternes, de face puis de cote, chacun sur 4 texels."""
    y = y1
    for i in range(n):
        if i % 2 == 0:
            m.cb(os_, 'maillon', (x - 2, y - 4, z), (x + 2, y, z + 1), ('plan', 'fer'), base=3.4, masque=['.##.', '#..#', '#..#', '.##.'], **CF)
        else:
            m.cb(os_, 'maillon', (x, y - 4, z - 2), (x + 1, y, z + 2), ('plan', 'fer'), base=2.6, masque=['.##.', '#..#', '#..#', '.##.'], **CR)
        y -= 3
    return y


def spectre_enchaine():
    """Spectre enchaine : grande capuche, visage d'ombre, deux yeux d'ame, manteau qui s'effile en glacons, bras sans mains cercles de fers, chaines qui pendent."""
    m = M('spectre_enchaine', 224, 3.137, description='Spectre enchaine Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    V = ('voile', 'spectre')
    m.p.bone('body', (0, 30, 0), 'root')
    p.bone('head', (0, 46, -1), 'body')
    p.bone('arm_-1', (12, 42, 0), 'body')
    p.bone('arm_1', (-12, 42, 0), 'body')
    p.bone('avbras_-1', (14, 30, 0), 'arm_-1')
    p.bone('avbras_1', (-14, 30, 0), 'arm_1')
    p.bone('bas', (0, 28, 0), 'body')
    # manteau : epaules, buste, jupe qui se resserre vers le bas
    m.b = 'body'
    m.c('epaules', (-13, 40, -6), (13, 46, 6), V, base=2.4, ton=.5)
    m.c('buste', (-11, 28, -6), (11, 40, 6), V, base=2.6)
    m.c('poitrine', (-8, 30, -7), (8, 40, -6), V, base=2.8, hide={'south'})
    m.c('fer_cou', (-8, 44, -7), (8, 47, 6), ('plaque', 'fer'), base=2.6, rivets=3)
    m.lueur('body', 'coeur', (-2, 33, -8), (2, 38, -7), ('uni', 'ame'), base=3.6, hide={'south'})
    _chaine(m, 'body', -4, 44, 4, z=-8)
    m.b = 'bas'
    m.c('jupe_a', (-10, 19, -6), (10, 28, 6), V, base=2.2)
    m.c('jupe_b', (-8, 13, -5), (8, 19, 5), V, base=1.8)
    m.c('frange_av', (-10, 8, -7), (10, 20, -6), FR('spectre'), base=2.2, mini=.35, **CF)
    m.c('frange_ar', (-10, 8, 6), (10, 20, 7), FR('spectre'), base=2, mini=.35, **CF)
    m.s('frange_c', (10, 8, -6), (11, 20, 6), FR('spectre'), base=2, mini=.35, **CR)
    # glacons sous l'ourlet
    for i, (x, z, h, r) in enumerate(((-6, -3, 9, 2), (0, -3, 12, 2), (6, -2, 10, 2), (-3, 2, 11, 2), (4, 2, 9, 2), (-8, 1, 7, 1), (8, 1, 7, 1))):
        m.c(f'glacon{i}', (x - r, 13 - h * 3 // 5, z - r), (x + r, 13, z + r), GL, ton=.3, rot=(0, 45, 0), pivot=(x, 13, z))
        m.c(f'glacon_p{i}', (x - 1, 13 - h, z - 1), (x + 1, 13 - h * 3 // 5, z + 1), GL, ton=1, rot=(0, 45, 0), pivot=(x, 13, z))
    # capuche : ouverture sombre, pointe en arriere
    m.b = 'head'
    m.c('capuche', (-9, 44, -8), (9, 60, 7), V, base=2.8, ton=.5)
    m.c('capuche_haut', (-7, 60, -6), (7, 63, 5), V, base=3.2)
    m.c('capuche_pointe', (-4, 58, 7), (4, 64, 12), V, base=2.6, rot=(-20, 0, 0), pivot=(0, 58, 7))
    m.c('capuche_bord', (-9, 58, -10), (9, 61, -8), V, base=3.4, hide={'south'})
    m.s('capuche_bord_c', (7, 47, -10), (9, 58, -8), V, base=3.2, hide={'south'})
    m.c('ombre', (-7, 46, -9), (7, 58, -8), ('uni', 'nuit'), base=.4, no_ao=True, hide={'south'})
    m.yeux('head', 2, 51, -10, 3, 4, ramp='ame', base=3.4, dz=2)
    p.decal('oeil_lueur_g', 'north', stamp(['bbb', 'cbb', 'ccb', 'bcb'], K, 5, 55))
    # bras : manches effilees, fers, chaines
    m.sb('arm_-1|arm_1', 'manche', (11, 30, -5), (18, 45, 5), V, base=2.6, ton=.2)
    m.sb('avbras_-1|avbras_1', 'manche_bas', (11, 21, -5), (18, 31, 5), V, base=2.2)
    m.sb('avbras_-1|avbras_1', 'frange_m', (11, 15, -6), (18, 22, -5), FR('spectre'), base=2, mini=.4, **CF)
    m.sb('avbras_-1|avbras_1', 'frange_m2', (11, 15, 5), (18, 22, 6), FR('spectre'), base=2, mini=.4, **CF)
    m.sb('avbras_-1|avbras_1', 'frange_m3', (18, 15, -5), (19, 22, 5), FR('spectre'), base=2, mini=.4, **CR)
    m.sb('avbras_-1|avbras_1', 'fers', (10, 24, -6), (19, 28, 6), ('plaque', 'fer'), base=2.8, rivets=3)
    for s, os_ in ((1, 'avbras_-1'), (-1, 'avbras_1')):
        _chaine(m, os_, s * 15, 24, 5, z=-7)
        _chaine(m, os_, s * 12, 24, 3, z=6)
    bipede(p, style='griffes', marche='fly', amp=0, genou=0, bras=10, jambe=0, cape=('bas',), flotte=1.5, gr=1.4)
    return p


def roi_cryptes():
    """Roi des cryptes : squelette immense, couronne d'or, armure noire filetee d'or et de bleu, cape pourpre, grande epee de glace bleue."""
    m = M('roi_cryptes', 225, 4.369, description='Roi des cryptes Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=62, epaule=112, cou=118, x_bras=32, x_jambe=14, coude=82, genou=32)
    NO = ('plaque', 'fer_noir')
    OR_ = ('plaque', 'or')
    BL = ('plaque', 'tissu_bleu')
    # jambes d'armure
    m.sb('leg_-1|leg_1', 'cuisse', (5, 30, -9), (24, 64, 9), NO, base=2.6, bord=2)
    m.sb('leg_-1|leg_1', 'cuisse_or', (11, 34, -10), (18, 62, -9), OR_, base=3, hide={'south'})
    m.sb('tibia_-1|tibia_1', 'greve', (6, 8, -9), (23, 32, 9), NO, base=2.4, bord=2)
    m.sb('tibia_-1|tibia_1', 'greve_or', (11, 10, -10), (18, 28, -9), BL, base=3, hide={'south'})
    m.sb('tibia_-1|tibia_1', 'genouillere', (6, 26, -12), (23, 40, -8), OR_, base=3, bord=2, rivets=4)
    m.sb('tibia_-1|tibia_1', 'soleret', (4, 0, -16), (25, 8, 11), NO, base=2.6, bord=2)
    m.sb('tibia_-1|tibia_1', 'soleret_or', (11, 2, -17), (18, 7, -16), OR_, base=3.4, hide={'south'})
    # bassin, jupe de lames, ceinture a crane
    m.b = 'body'
    m.c('bassin', (-24, 56, -13), (24, 74, 13), NO, base=2.2, bord=2)
    m.c('ceinture', (-26, 72, -15), (26, 80, 15), OR_, base=2.8, bord=2, rivets=6)
    m.c('boucle_crane', (-8, 68, -18), (8, 84, -15), OS, ton=.4, hide={'south'})
    m.c('boucle_orbites', (-5, 76, -19), (5, 80, -18), ('uni', 'nuit'), base=.6, no_ao=True, hide={'south'})
    for i, (x0, x1) in enumerate(((-24, -14), (-13, -3), (3, 13), (14, 24))):
        m.c(f'tasse{i}', (x0, 40, -16), (x1, 70, -14), NO, base=2.6, bord=2, hide={'south'})
        m.c(f'tasse_or{i}', (x0 + 3, 44, -17), (x1 - 3, 68, -16), BL, base=3, hide={'south'})
    # poitrine ouverte : cotes d'os entre des plaques noires, coeur d'ame
    m.c('ventre_ombre', (-20, 80, -10), (20, 112, 12), ('uni', 'nuit'), base=1)
    m.b = 'body'
    cage(m, 86, 4, 22, 13, pas=6, h=3)
    m.lueur('body', 'coeur', (-6, 92, -8), (6, 106, 4), ('cristal', 'ame'), base=3)
    m.c('dos_plaque', (-24, 80, 11), (24, 116, 16), NO, base=2.4, bord=2)
    m.s('flanc_plaque', (20, 80, -8), (26, 110, 13), NO, base=2.2, bord=2)
    m.c('col', (-22, 110, -15), (22, 120, 14), NO, base=2.8, bord=2)
    m.c('col_or', (-23, 116, -16), (23, 120, -15), OR_, base=3.6, hide={'south'})
    # cape pourpre en deux pans
    p.bone('cape', (0, 116, 16), 'body')
    p.bone('cape_bas', (0, 62, 19), 'cape')
    m.cb('cape', 'cape_haut', (-27, 62, 16), (27, 118, 19), ('peau_tissu', 'pourpre'), base=2.6, pli=11)
    m.cb('cape', 'cape_col', (-28, 113, 16), (28, 120, 20), OR_, base=2.8)
    m.cb('cape_bas', 'cape_pan', (-27, 16, 18), (27, 62, 20), FR('pourpre'), base=2.4, mini=.7, pas=4, hide={'east', 'west', 'up', 'down'}, no_shadow=True)
    # tete : crane couronne
    crane(m, 'head', 13, 118, 146, -14, 11, oeil='ame', dents=True)
    m.c2 = None
    m.cb('head', 'couronne', (-15, 141, -16), (15, 148, 13), OR_, base=3, bord=1, rivets=4)
    for i, (x, h) in enumerate(((-13, 9), (-7, 12), (-1, 15), (5, 12), (11, 9))):
        m.cb('head', f'fleuron{i}', (x, 148, -16), (x + 3, 148 + h, -14), ('metal', 'or'), base=3.4, hide=None)
        m.cb('head', f'fleuron_ar{i}', (x, 148, 11), (x + 3, 148 + h - 3, 13), ('metal', 'or'), base=3)
    m.sb('head', 'fleuron_c', (13, 148, -9), (15, 158, -6), ('metal', 'or'), base=3.2)
    m.sb('head', 'fleuron_c', (13, 148, 3), (15, 158, 6), ('metal', 'or'), base=3.2)
    m.lueur('head', 'gemme', (-3, 142, -18), (3, 147, -16), ('cristal', 'ame'), base=3, hide={'south'})
    # bras : spalieres a pointes d'or
    m.sb('arm_-1|arm_1', 'bras', (26, 80, -9), (40, 110, 9), NO, base=2.4, bord=2)
    m.sb('arm_-1|arm_1', 'spaliere', (22, 106, -15), (47, 124, 15), NO, base=2.8, bord=2)
    m.sb('arm_-1|arm_1', 'spaliere_or', (22, 104, -16), (48, 108, 16), OR_, base=3.2)
    m.sb('arm_-1|arm_1', 'spaliere_b', (30, 94, -13), (46, 106, 13), NO, base=2.4, bord=2)
    m.sb('arm_-1|arm_1', 'pique', (34, 124, -3), (41, 140, 3), ('metal', 'or'), base=3.2, rot=(0, 0, -12), pivot=(37, 124, 0))
    m.sb('arm_-1|arm_1', 'pique_b', (36, 139, -2), (40, 150, 2), ('metal', 'or'), base=3.8, rot=(0, 0, -12), pivot=(37, 124, 0))
    m.sb('avbras_-1|avbras_1', 'avantbras', (27, 52, -8), (39, 82, 8), NO, base=2.4, bord=2)
    m.sb('avbras_-1|avbras_1', 'canon_or', (25, 60, -10), (41, 74, 10), OR_, base=3, bord=2, rivets=4)
    m.sb('avbras_-1|avbras_1', 'gantelet', (25, 42, -10), (41, 54, 10), NO, base=2.8, bord=2)
    # grande epee de glace, main droite
    p.bone('epee', (33, 46, -9), 'avbras_-1', rot=(-26, 0, 0))
    GLA = ('cristal', 'ame')
    m.cb('epee', 'fusee', (31, 30, -11), (35, 60, -7), ('cuir', 'cuir'), couture=False)
    m.cb('epee', 'garde', (22, 58, -13), (44, 63, -5), ('plaque', 'or'), base=3.2)
    m.lueur('epee', 'lame', (28, 63, -11), (38, 130, -7), GLA, base=2.6)
    m.lueur('epee', 'pointe', (30, 130, -11), (36, 144, -7), GLA, base=3.4)
    m.lueur('epee', 'pointe_b', (32, 144, -10), (34, 150, -8), GLA, base=4)
    m.cb('epee', 'pommeau', (29, 24, -13), (37, 30, -5), ('plaque', 'or'), base=3.4)
    bipede(p, style='epee', amp=18, genou=28, bras=10, jambe=62, cape=('cape', 'cape_bas'), gr=4, arme='epee', haleine=1)
    return p


CRYPTES = {'osselet': osselet, 'araignee_os': araignee_os, 'garde_funeraire': garde_funeraire, 'spectre_enchaine': spectre_enchaine, 'roi_cryptes': roi_cryptes}
