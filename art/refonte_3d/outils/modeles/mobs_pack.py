#!/usr/bin/env python3
"""Mobs du second pack : fernling, champion gobelin, voyageur squelette, sentinelle hibou, minotaure, morghul, gardien de citadelle, seigneur squelette. En texels de jeu."""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import rot, sin
from clips_mob import _fin, _r, P_ATT, bipede, ev, octo_mob, quadrupede_mob
from kit_mob import CF, CR, FR, M, RE, feuille, losange, rond, triangle
from materiaux import stamp
from mobs_cryptes import OS, OSV, FN, ORM, ACI, cage, crane, epee

K = {'k': '#0b0a0f', 'g': '#4bbd6a', 'G': '#86e58e', 'b': '#3ac6d4', 'c': '#dcffff', 'y': '#ffe16a', 'r': '#e0342a', 'o': '#f4c060'}
GOB = ('peau', 'gobelin')
CU = ('cuir', 'cuir')
CB = ('plaque', 'tissu_brun')


def fernling():
    """Fernling : petite capsule blanche sur quatre pattes de crabe, bandeau sombre aux deux yeux verts, bouquet de frondes et de crosses sur le dessus."""
    m = M('fernling', 261, 1.295, description='Fernling Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    BL = ('plaque', 'marbre')
    p.bone('body', (0, 8, 0), 'root')
    m.b = 'body'
    m.c('capsule', (-6, 4, -5), (6, 11, 5), BL, base=3.6, bord=0)
    m.c('bandeau', (-6, 6, -6), (6, 9, -5), ('uni', 'nuit'), base=1, no_ao=True, hide={'south'})
    m.c('couvercle', (-5, 11, -4), (5, 12, 4), ('uni', 'terre'), base=2)
    p.bone('head', (0, 11, 0), 'body')
    m.yeux('body', 2, 7, -6, 2, 2, ramp='rar_peu_commun', base=4.4)
    # frondes : plans croises decoupes, deux crosses roulees
    F = ['...#....', '..###...', '.#####..', '#######.', '.#####..', '..###...', '...#....']
    m.cb('head', 'fronde', (-9, 11, -1), (9, 15, 0), ('feuilles', 'feuillage_sombre'), ton=.4, rot=(-16, 0, 0), pivot=(0, 11, 0), hide={'south', 'east', 'west', 'down'})
    m.touffe('head', 'fronde_h', 0, 11, 0, 7, 7, 'feuillage', 3.2, masque=feuille(14, 7))
    m.touffe('head', 'fronde_b', 4, 11, -2, 6, 5, 'feuillage_sombre', 3.6, masque=feuille(10, 6))
    m.touffe('head', 'fronde_c', -5, 11, 2, 6, 5, 'feuillage_sombre', 3.6, masque=feuille(10, 6))
    CROSSE = ['.####.', '#....#', '#.##.#', '#.#..#', '#.#...', '#.....', '#.....', '#.....', '#.....', '#.....']
    p.bone('crosse_g', (2, 12, 0), 'head')
    p.bone('crosse_d', (-2, 12, 1), 'head')
    m.cb('crosse_g', 'crosse_a', (1, 12, 0), (7, 22, 1), ('plan', 'feuillage'), base=4, masque=CROSSE, rot=(0, 0, -12), pivot=(2, 12, 0), **CF)
    m.cb('crosse_d', 'crosse_b', (-6, 12, 1), (0, 20, 2), ('plan', 'feuillage'), base=4.4, masque=CROSSE, rot=(0, 0, 10), pivot=(-2, 12, 1), **CF)
    # quatre pattes blanches arquees
    legs = []
    for i, (z, fan) in enumerate(((-3, 30), (3, -30))):
        g, d = f'leg{i}l', f'leg{i}r'
        p.bone(g, (5, 8, z), 'body')
        p.bone(d, (-5, 8, z), 'body')
        xk = round(5 + 8 * math.cos(math.radians(fan)))
        zk = round(z - 8 * math.sin(math.radians(fan)))
        p.bone(g + '_bas', (xk, 12, zk), g)
        p.bone(d + '_bas', (-xk, 12, zk), d)
        m.sb(f'{g}|{d}', f'cuisse{i}', (4, 7, z - 1), (13, 10, z + 2), BL, base=3.4, bord=0, rot=(0, fan, 26), pivot=(5, 8, z))
        m.sb(f'{g}_bas|{d}_bas', f'tibia{i}', (xk - 1, 0, zk - 1), (xk + 2, 13, zk + 2), BL, base=3.8, bord=0, rot=(0, 0, 16), pivot=(xk, 12, zk))
        legs += [(g, g + '_bas', 1), (d, d + '_bas', -1)]
    octo_mob(p, legs, head='head', idle=4.0, walk=.8, attack=1.8, gr=.6)
    return p


def champion_gobelin():
    """Champion gobelin : trapu, grande tete aux oreilles longues, defenses, cuirasse de cuir cloutee, epauliere taillee dans un crane, gros couperet."""
    m = M('champion_gobelin', 262, 1.318, description='Champion gobelin Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=16, epaule=30, cou=31, x_bras=11, x_jambe=5, coude=21, genou=7)
    m.sb('leg_-1|leg_1', 'cuisse', (2, 7, -4), (9, 17, 4), ('peau_tissu', 'tissu_vert'), base=2.4)
    m.sb('tibia_-1|tibia_1', 'botte', (2, 2, -4), (9, 8, 4), CU, ton=-1, couture=False)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -7), (9, 3, 4), CU, ton=-1.3, couture=False)
    m.b = 'body'
    m.c('bassin', (-9, 14, -5), (9, 20, 5), ('peau_tissu', 'tissu_vert'), base=2.2)
    m.c('ventre', (-10, 20, -6), (10, 28, 6), CU, ton=-.9)
    m.c('poitrine', (-10, 28, -6), (10, 33, 6), CU, ton=-.6)
    m.c('ceinture', (-10, 19, -7), (10, 22, 7), CU, ton=-1.4, couture=False)
    m.c('boucle', (-3, 18, -8), (3, 23, -7), ('metal', 'fer'), base=3, hide={'south'})
    m.c('plaque_ventre', (-6, 23, -7), (6, 32, -6), ('plaque', 'fer'), base=2.8, rivets=3, hide={'south'})
    m.c('pagne', (-5, 8, -7), (5, 19, -6), FR('tissu_brun'), base=2.8, mini=.6, **CF)
    m.b = 'head'
    m.c('crane', (-8, 31, -8), (8, 44, 6), GOB, base=3.4)
    m.c('front', (-8, 40, -9), (8, 43, -8), GOB, base=2.6, hide={'south'})
    m.c('nez', (-2, 35, -10), (2, 39, -8), GOB, base=4)
    m.yeux('head', 3, 37, -9, 3, 2, ramp='rouge', base=4.6)
    p.bone('machoire', (0, 34, -2), 'head')
    m.cb('machoire', 'mandibule', (-7, 29, -10), (7, 34, 2), GOB, base=2.8)
    m.sb('machoire', 'defense', (4, 33, -11), (6, 38, -9), ('griffe', 'ivoire'), base=3.6)
    p.bone('oreille_g', (8, 40, -1), 'head')
    p.bone('oreille_d', (-8, 40, -1), 'head')
    OR_ = ['#..........', '###........', '#####......', '#######....', '#########..', '###########', '#########..', '#####......']
    m.sb('oreille_g|oreille_d', 'oreille', (8, 36, -1), (19, 44, 0), ('plan', 'gobelin'), base=3.6, masque=[r[::-1] for r in OR_], rot=(0, -18, 8), pivot=(8, 40, -1), **CF)
    m.cb('head', 'crete_poils', (-1, 44, -7), (1, 50, 6), ('crete', 'soie_noire'), base=2, ecart=2, pas=2, dent=3, **CR)
    # bras
    m.sb('arm_-1|arm_1', 'bras', (9, 21, -4), (15, 31, 4), GOB, base=3.2)
    m.sb('avbras_-1|avbras_1', 'avantbras', (9, 11, -4), (15, 21, 4), GOB, base=3.4)
    m.sb('avbras_-1|avbras_1', 'brassard', (8, 13, -5), (16, 18, 5), CU, ton=-1, couture=False)
    m.sb('avbras_-1|avbras_1', 'poing', (9, 8, -4), (15, 12, 4), GOB, base=3)
    # epauliere-crane sur l'epaule droite, cuir et clous sur l'autre
    m.cb('arm_-1', 'crane_ep', (8, 30, -6), (18, 39, 5), OS, ton=.3)
    m.cb('arm_-1', 'orbites_ep', (9, 33, -7), (17, 36, -6), ('uni', 'nuit'), base=.6, no_ao=True, hide={'south'})
    m.cb('arm_-1', 'dents_ep', (10, 29, -7), (16, 31, -6), OS, ton=1, hide={'south'})
    m.cb('arm_1', 'cuir_ep', (-16, 29, -5), (-8, 33, 5), ('plaque', 'cuir'), base=2.2, rivets=3)
    # couperet
    p.bone('couperet', (12, 10, -6), 'avbras_-1', rot=(-24, 0, -22))
    m.cb('couperet', 'manche', (11, 4, -7), (13, 22, -5), CU, couture=False)
    m.cb('couperet', 'lame', (11, 20, -16), (13, 34, -5), ('metal', 'fer'), base=3)
    m.cb('couperet', 'dos_lame', (11, 34, -16), (13, 36, -4), ('metal', 'fer'), base=2)
    bipede(p, style='epee', idle=2.5, walk=1.0, attack=1.0, amp=24, genou=34, bras=20, jambe=16, jaw='machoire', oreilles=('oreille_g', 'oreille_d'), arme='couperet')
    return p


def voyageur_squelette():
    """Voyageur squelette : capuche de toile, crane sous l'ombre, cotes a nu, vieille epee et rondache de bois."""
    m = M('voyageur_squelette', 263, 1.050, description='Voyageur squelette Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=14, epaule=25, cou=26, x_bras=7, x_jambe=3, coude=17, genou=7)
    T = ('peau_tissu', 'tissu_brun')
    m.sb('leg_-1|leg_1', 'femur', (2, 7, -1), (4, 15, 1), OS, ton=-.1)
    m.sb('tibia_-1|tibia_1', 'tibia', (2, 2, -1), (4, 8, 1), OS, ton=-.2)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -3), (5, 2, 2), OS)
    m.b = 'body'
    m.c('bassin', (-4, 13, -2), (4, 16, 2), OS)
    m.c('colonne', (-1, 15, 0), (1, 25, 2), OS, ton=-.3)
    cage(m, 18, 3, 4, 3, pas=2, h=1)
    m.c('clavicule', (-6, 24, -2), (6, 26, 2), OS, ton=.3)
    m.c('haillon', (-4, 5, -3), (4, 15, -2), FR('tissu_brun'), base=2.6, mini=.5, **CF)
    m.c('cape_dos', (-6, 6, 2), (6, 26, 3), FR('tissu_brun'), base=2.4, mini=.6, **CF)
    m.b = 'head'
    crane(m, 'head', 4, 26, 34, -4, 4, oeil='ambre', dents=False, oeil_w=1, oeil_h=2, oeil_dz=1)
    m.cb('head', 'capuche', (-6, 31, -5), (6, 37, 6), T, base=2.6)
    m.cb('head', 'capuche_b', (-6, 27, 3), (6, 31, 6), T, base=2.2)
    m.sb('head', 'capuche_c', (5, 26, -4), (6, 32, 4), T, base=2.4)
    m.cb('head', 'pointe', (-3, 35, 5), (3, 37, 8), T, base=2.4)
    m.sb('arm_-1|arm_1', 'humerus', (6, 17, -1), (8, 25, 1), OS, ton=-.1)
    m.sb('arm_-1|arm_1', 'epaule', (5, 23, -2), (9, 27, 2), T, base=2.6)
    m.sb('avbras_-1|avbras_1', 'radius', (6, 10, -1), (8, 17, 1), OS, ton=-.2)
    m.sb('avbras_-1|avbras_1', 'main', (5, 7, -2), (9, 10, 2), OS)
    p.bone('epee', (7, 8, 0), 'avbras_-1', rot=(-58, 0, 0))
    epee(m, 'epee', 7, 8, 16, l=1, lame=('metal', 'fer'), garde=('metal', 'fer'), pommeau=('metal', 'fer'))
    p.bone('rondache', (-9, 12, 0), 'avbras_1')
    m.cb('rondache', 'disque', (-11, 4, -7), (-9, 18, 7), ('bois', 'bois'), base=3, planche=3, sens='y')
    m.cb('rondache', 'cercle_h', (-12, 17, -5), (-11, 18, 5), ('metal', 'fer'), base=2.2, hide={'east'})
    m.cb('rondache', 'cercle_b', (-12, 4, -5), (-11, 5, 5), ('metal', 'fer'), base=2.2, hide={'east'})
    m.cb('rondache', 'umbo', (-13, 9, -2), (-11, 13, 2), ('metal', 'fer'), base=3.2)
    bipede(p, style='epee', idle=2.5, walk=1.0, attack=1.25, amp=26, genou=38, bras=20, jambe=14, arme='epee')
    return p


def sentinelle_hibou():
    """Sentinelle hibou : grand hibou en armure de fer bleu, tete ronde aux aigrettes, immenses yeux bleus, ailes repliees en cape de plumes, serres de fer."""
    m = M('sentinelle_hibou', 264, 1.047, description='Sentinelle hibou Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=19, epaule=37, cou=38, x_bras=11, x_jambe=5, coude=26, genou=9)
    PB = ('plaque', 'chitine_bleue')
    PF = ('plaque', 'fer')
    PL = ('ecailles', 'fourrure_givre')
    m.sb('leg_-1|leg_1', 'cuisse', (2, 9, -4), (8, 20, 4), PB, base=2.8)
    m.sb('tibia_-1|tibia_1', 'greve', (3, 3, -3), (7, 10, 3), PF, base=2.6)
    m.sb('tibia_-1|tibia_1', 'pied', (2, 0, -6), (8, 3, 3), PF, base=2.2)
    for x, r in ((2, 12), (6, -12)):
        m.sb('tibia_-1|tibia_1', 'serre', (x, 0, -10), (x + 2, 2, -6), ('griffe', 'nuit'), base=3.4, rot=(0, r, 0), pivot=(x + 1, 1, -6))
    m.b = 'body'
    m.c('bassin', (-8, 17, -5), (8, 24, 5), PB, base=2.4)
    m.c('plastron', (-9, 24, -6), (9, 39, 6), PF, base=2.8, rivets=4)
    m.c('poitrail', (-6, 26, -7), (6, 37, -6), PL, base=3.4, hide={'south'})
    m.lueur('body', 'rune', (-2, 29, -8), (2, 34, -7), ('cristal', 'ame'), base=2.6, hide={'south'})
    m.c('ceinture', (-9, 22, -7), (9, 25, 7), ('plaque', 'or'), base=2.6)
    m.c('tasse', (-8, 10, -6), (8, 22, -5), PB, base=2.6, hide={'south'})
    # queue en eventail
    m.c('queue', (-7, 7, 6), (7, 27, 7), RE('fourrure_givre'), base=3.4, mini=.5, pas=3, **CF)
    # tete ronde
    m.b = 'head'
    m.c('crane', (-10, 38, -9), (10, 54, 8), PL, base=3.2)
    m.c('disque', (-9, 41, -10), (9, 53, -9), ('uni', 'fourrure_givre'), base=4.4, hide={'south'})
    m.c('casque', (-10, 50, -11), (10, 55, 9), PF, base=3, rivets=4)
    m.yeux('head', 2, 43, -11, 6, 6, ramp='ame', base=3, dz=1)
    p.decal('oeil_lueur_g', 'north', stamp(['cbbbbc', 'bbbbbb', 'bbkkbb', 'bbkkbb', 'bbbbbb', 'cbbbbc'], K, 8, 49))
    m.c('bec', (-2, 40, -13), (2, 44, -10), ('griffe', 'bec'), base=3.4)
    m.c('bec_b', (-1, 38, -13), (1, 41, -11), ('griffe', 'bec'), base=2.2)
    for sg, n in ((1, 'g'), (-1, 'd')):
        a0, a1 = sorted((sg * 5, sg * 10))
        m.c(f'aigrette_{n}', (a0, 54, -5), (a1, 62, 1), PF, base=2.8, rot=(-4, 0, -sg * 14), pivot=((a0 + a1) / 2, 54, -2))
        m.c(f'aigrette2_{n}', (a0 + 1, 61, -4), (a1 - 1, 66, -1), PF, base=3.4, rot=(-4, 0, -sg * 14), pivot=((a0 + a1) / 2, 54, -2))
    # ailes repliees sur les bras, cape de plumes
    m.sb('arm_-1|arm_1', 'epauliere', (8, 32, -6), (16, 41, 6), PF, base=3.2, rivets=3)
    m.sb('arm_-1|arm_1', 'bras', (9, 26, -3), (14, 33, 3), PB, base=2.8)
    m.sb('arm_-1|arm_1', 'plumes_b', (15, 14, -5), (16, 33, 5), RE('fourrure_givre'), base=4, mini=.55, pas=3, **CR)
    m.sb('avbras_-1|avbras_1', 'avantbras', (9, 16, -3), (14, 26, 3), PF, base=2.8)
    m.sb('avbras_-1|avbras_1', 'gant', (8, 11, -4), (15, 17, 4), PB, base=3)
    bipede(p, style='griffes', idle=4.0, walk=1.7, attack=1.4, amp=20, genou=30, bras=12, jambe=19)
    return p


def minotaure():
    """Minotaure : torse de taureau, tete bovine a grandes cornes, armure sombre filetee d'or, jupe de plaques, sabots, enorme marteau de guerre."""
    m = M('minotaure', 265, 1.084, description='Minotaure Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (0, 70)
    m.humanoide(hanche=25, epaule=47, cou=48, x_bras=17, x_jambe=8, coude=33, genou=11, z_cou=-3)
    FU = ('fourrure', 'taureau')
    NO = ('plaque', 'fer_noir')
    OR_ = ('plaque', 'or')
    m.sb('leg_-1|leg_1', 'cuisse', (2, 11, -6), (14, 27, 6), FU, y=(0, 40))
    m.sb('tibia_-1|tibia_1', 'tibia', (3, 4, -5), (13, 12, 5), FU, y=(0, 40), ton=-.3)
    m.sb('tibia_-1|tibia_1', 'greve', (2, 4, -6), (14, 11, -4), NO, base=2.6)
    m.sb('tibia_-1|tibia_1', 'sabot', (2, 0, -7), (14, 4, 6), ('sabot', 'sabot'), base=2.4)
    m.b = 'body'
    m.c('bassin', (-14, 22, -8), (14, 31, 8), NO, base=2.2)
    m.c('ventre', (-14, 31, -9), (14, 40, 9), FU, y=(0, 70), ton=.1)
    m.c('poitrail', (-18, 40, -11), (18, 54, 11), NO, base=2.6)
    m.c('poitrail_or', (-10, 42, -12), (10, 52, -11), OR_, base=2.8, hide={'south'})
    m.c('losange', (-4, 44, -13), (4, 50, -12), NO, base=2, hide={'south'})
    m.lueur('body', 'gemme', (-2, 45, -14), (2, 49, -13), ('cristal', 'rouge'), base=3, hide={'south'})
    m.c('garrot', (-13, 54, -7), (13, 58, 9), FU, y=(0, 70), ton=.3)
    m.c('ceinture', (-15, 29, -10), (15, 33, 10), OR_, base=2.6, rivets=4)
    for i, (x0, x1) in enumerate(((-14, -5), (-4, 4), (5, 14))):
        m.c(f'tasse{i}', (x0, 13, -10), (x1, 30, -9), NO, base=2.6, hide={'south'})
        m.c(f'tasse_or{i}', (x0 + 2, 15, -11), (x1 - 2, 28, -10), OR_, base=2.4, hide={'south'})
    m.c('tasse_dos', (-13, 15, 8), (13, 30, 9), NO, base=2.4)
    # tete de taureau, museau, anneau, grandes cornes
    m.b = 'head'
    m.c('crane', (-10, 48, -15), (10, 66, 3), FU, y=(0, 70), ton=.3)
    m.c('museau', (-7, 46, -22), (7, 56, -14), ('uni', 'museau_bovin'), base=2.8)
    m.c('narines', (-5, 51, -23), (5, 54, -22), ('uni', 'nuit'), base=1.2, no_ao=True, hide={'south'})
    m.c('frontal', (-8, 60, -16), (8, 66, -15), NO, base=2.8, hide={'south'})
    m.yeux('head', 4, 56, -16, 3, 2, ramp='rouge', base=4.6)
    m.c('anneau', (-2, 44, -24), (2, 48, -23), ('metal', 'or'), base=3, hide=None)
    p.bone('machoire', (0, 50, -8), 'head')
    m.cb('machoire', 'mandibule', (-6, 42, -21), (6, 47, -6), ('uni', 'taureau'), base=2)
    for sg, n in ((1, 'g'), (-1, 'd')):
        a0, a1 = sorted((sg * 8, sg * 20))
        m.c(f'corne_{n}', (a0, 60, -8), (a1, 65, -3), ('corne', 'ivoire'), base=2.6, rot=(0, 0, sg * 8), pivot=(sg * 10, 62, -5))
        b0, b1 = sorted((sg * 19, sg * 24))
        m.c(f'corne2_{n}', (b0, 62, -8), (b1, 76, -3), ('corne', 'ivoire'), base=3.2, rot=(0, 0, -sg * 8), pivot=(sg * 21, 62, -5))
    p.bone('oreille_g', (10, 60, -6), 'head')
    p.bone('oreille_d', (-10, 60, -6), 'head')
    m.sb('oreille_g|oreille_d', 'oreille', (9, 55, -7), (16, 59, -5), FU, y=(0, 70), rot=(0, 0, -18), pivot=(10, 58, -6))
    # bras enormes
    m.sb('arm_-1|arm_1', 'epaule', (13, 40, -9), (27, 58, 9), NO, base=2.8)
    m.sb('arm_-1|arm_1', 'epaule_or', (12, 37, -10), (28, 41, 10), OR_, base=2.8)
    m.sb('arm_-1|arm_1', 'bras', (14, 32, -7), (25, 41, 7), FU, y=(0, 70), ton=.2)
    m.sb('avbras_-1|avbras_1', 'avantbras', (14, 18, -7), (26, 34, 7), FU, y=(0, 70))
    m.sb('avbras_-1|avbras_1', 'canon', (13, 19, -8), (27, 30, 8), NO, base=2.6)
    m.sb('avbras_-1|avbras_1', 'poing', (14, 13, -7), (26, 20, 7), FU, y=(0, 70), ton=-.3)
    # marteau de guerre : long manche, enorme tete cerclee d'or
    p.bone('marteau', (20, 16, -3), 'avbras_-1', rot=(-16, 0, -36))
    W = ('bois', 'bois_sombre')
    m.cb('marteau', 'manche', (18, -4, -5), (22, 50, -1), W, ton=1, planche=2)
    m.cb('marteau', 'tete', (5, 48, -15), (35, 76, 9), NO, base=2.6, bord=2)
    m.cb('marteau', 'cercle', (4, 58, -16), (36, 66, 10), OR_, base=2.8, rivets=5)
    m.cb('marteau', 'pointe', (15, 76, -8), (25, 82, 2), NO, base=3.2)
    m.cb('marteau', 'pommeau', (17, -8, -6), (23, -4, 0), OR_, base=3)
    bipede(p, style='poing', idle=2.0, walk=1.2, attack=.75, amp=20, genou=26, bras=10, jambe=25, jaw='machoire', oreilles=('oreille_g', 'oreille_d'), arme='marteau', gr=1.6, haleine=.7)
    return p


def morghul():
    """Morghul, bete des abysses : enorme masse voutee, bras jusqu'au sol et poings de roche, plaques d'os, echine d'epines noires a pointe violette, gueule pleine de crocs, longue queue."""
    m = M('morghul', 266, .445, description='Morghul, bete des abysses Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (0, 90)
    PE = ('peau_plissee', 'cendre')
    NR = ('plaque', 'pierre_noire')
    AM = ('cristal', 'amethyste')
    m.humanoide(hanche=30, epaule=60, cou=58, x_bras=28, x_jambe=15, coude=36, genou=13, z_bras=-10, z_jambe=12, z_cou=-24)
    # jambes courtes et tordues
    m.sb('leg_-1|leg_1', 'cuisse', (7, 12, 3), (25, 34, 21), PE, y=(0, 90))
    m.sb('tibia_-1|tibia_1', 'mollet', (9, 4, 4), (23, 14, 18), PE, y=(0, 90), ton=-.2)
    m.sb('tibia_-1|tibia_1', 'pied', (6, 0, -1), (26, 5, 18), NR, base=2.4)
    for i, x in enumerate((7, 14, 21)):
        m.sb('tibia_-1|tibia_1', f'griffe{i}', (x, 0, -9), (x + 4, 5, -1), ('griffe', 'ivoire'), base=3, rot=(10, 0, 0), pivot=(x + 2, 2, -1))
    # corps vouté : bassin, dos, epaules tres larges
    m.b = 'body'
    m.c('bassin', (-22, 26, 4), (22, 48, 26), PE, y=(0, 90))
    m.c('dos', (-27, 36, -14), (27, 66, 8), PE, y=(0, 90), ton=.3)
    m.c('poitrail', (-30, 40, -28), (30, 72, -14), PE, y=(0, 90), ton=.5)
    m.c('ventre_os', (-16, 30, -26), (16, 44, -8), OS, ton=-.2)
    for i, y in enumerate((44, 49, 54)):
        m.c(f'cote_av{i}', (-22, y, -29), (22, y + 3, -28), OS, ton=.3, hide={'south'})
    m.lueur('body', 'fente_coeur', (-3, 46, -30), (3, 56, -29), ('uni', 'amethyste'), base=3.6, hide={'south'})
    # epines dorsales : base d'os annelee, pointe noire, bout violet lumineux
    for i, (x, y, z, h, rx, rz) in enumerate(((0, 72, -16, 20, -14, 0), (-15, 68, -12, 17, -10, 18), (15, 68, -12, 17, -10, -18), (0, 66, 0, 22, 8, 0), (-15, 62, 2, 16, 12, 18), (15, 62, 2, 16, 12, -18), (0, 48, 16, 20, 34, 0), (-11, 46, 18, 14, 36, 16), (11, 46, 18, 14, 36, -16))):
        n = m.cb('body', f'epine{i}', (x - 3, y, z - 3), (x + 3, y + h // 2, z + 3), ('corne', 'os'), base=3, rot=(rx, 45, rz), pivot=(x, y, z))
        m.cb('body', f'epine_n{i}', (x - 2, y + h // 2, z - 2), (x + 2, y + h - 4, z + 2), ('uni', 'nuit'), base=3, rot=(rx, 45, rz), pivot=(x, y, z))
        m.cb('body', f'epine_lueur{i}', (x - 1, y + h - 4, z - 1), (x + 1, y + h + 1, z + 1), AM, base=2.4, rot=(rx, 45, rz), pivot=(x, y, z))
    # tete basse, enfoncee, grande gueule
    m.b = 'head'
    m.c('crane', (-16, 52, -48), (16, 74, -22), PE, y=(0, 90), ton=.8)
    m.c('front_os', (-13, 72, -46), (13, 77, -26), OS, ton=-.2)
    m.c('museau', (-12, 50, -56), (12, 62, -46), PE, y=(0, 90), ton=.5)
    m.yeux('head', 5, 64, -49, 5, 3, ramp='amethyste', base=4.4)
    m.c('arcade', (-14, 67, -50), (14, 71, -47), OS, ton=.2)
    for i in range(6):
        xd = -11 + i * 4
        m.c(f'croc_h{i}', (xd, 44 + (i % 2) * 2, -56), (xd + 3, 51, -53), OS, ton=1)
    for sg, n in ((1, 'g'), (-1, 'd')):
        a0, a1 = sorted((sg * 10, sg * 16))
        m.c(f'corne_{n}', (a0, 72, -38), (a1, 90, -32), ('corne', 'os'), base=3, rot=(14, 0, -sg * 22), pivot=((a0 + a1) / 2, 72, -35))
    p.bone('machoire', (0, 50, -30), 'head')
    m.cb('machoire', 'mandibule', (-13, 38, -54), (13, 50, -28), PE, y=(0, 90), ton=.2)
    m.cb('machoire', 'gueule', (-10, 50, -52), (10, 51, -30), ('uni', 'rose_chaos'), base=1.6, hide={'down'})
    for i in range(5):
        xd = -9 + i * 4
        m.cb('machoire', f'croc_b{i}', (xd, 50, -54), (xd + 3, 55 + (i % 2), -51), OS, ton=1)
    # bras : epaules-rochers, avant-bras enormes, poings au sol
    m.sb('arm_-1|arm_1', 'epaule', (22, 52, -24), (44, 72, 0), PE, y=(0, 90), ton=.6)
    m.sb('arm_-1|arm_1', 'plaque_os', (23, 72, -22), (43, 76, -2), OS, ton=.1)
    m.sb('arm_-1|arm_1', 'plaque_os_b', (44, 58, -21), (47, 73, -3), OS, ton=-.2)
    m.sb('arm_-1|arm_1', 'plaque_os_c', (26, 64, -25), (42, 72, -24), OS, ton=.2, hide={'south'})
    m.sb('arm_-1|arm_1', 'bras', (24, 34, -20), (42, 52, -2), PE, y=(0, 90))
    m.pointe('arm_-1', 'pic_ep_lueur', 34, 76, -12, 16, 3, AM, inc=(0, -16), base=1.6)
    m.pointe('arm_1', 'pic_ep_lueur', -34, 76, -12, 16, 3, AM, inc=(0, 16), base=1.6)
    m.sb('avbras_-1|avbras_1', 'avantbras', (22, 14, -24), (46, 38, 2), PE, y=(0, 90), ton=.2)
    m.sb('avbras_-1|avbras_1', 'bague_os', (21, 20, -25), (47, 26, 3), OS, ton=-.2)
    m.sb('avbras_-1|avbras_1', 'poing', (20, 0, -30), (50, 16, 4), NR, base=2.4, bord=2)
    for i, x in enumerate((21, 28, 35, 42)):
        m.sb('avbras_-1|avbras_1', f'jointure{i}', (x, 2, -33), (x + 6, 12, -30), OS, ton=.2)
    # queue en quatre os, crochet au bout
    pts = [(0, 34, 25), (0, 32, 37), (0, 30, 49), (2, 32, 60), (6, 38, 70), (8, 48, 74)]
    lar = [11, 9, 7, 5, 4]

    def cote(m_, os_, i, P, W, H):
        x, y, z = P
        wx, wz = W
        m_.cb(os_, f'crete_q{i}', (x - 1, y, z + wz - 1), (x + 1, y + H, z + wz + 5), ('plan', 'os'), base=3, masque=[''.join('#' if (a + b) % 4 < 3 else '.' for a in range(4)) for b in range(6)], **CR)
    ch = m.courbe(pts, lar, PE, prefixe='queue', parent='body', cote=cote, y=(0, 90))
    x, y, z = pts[-1]
    p.bone('crochet', (x, y, z), ch[-1][0])
    m.cb('crochet', 'crochet_a', (x - 2, y - 1, z - 2), (x + 2, y + 10, z + 2), ('corne', 'os'), base=3.2, rot=(-34, 0, 0), pivot=(x, y, z))
    m.cb('crochet', 'crochet_b', (x - 1, y + 8, z - 2), (x + 1, y + 14, z), AM, base=2.2, rot=(-64, 0, 0), pivot=(x, y + 6, z - 3))
    bipede(p, style='poing', idle=4.0, walk=1.5, attack=3.0, amp=14, genou=20, bras=16, jambe=30, jaw='machoire', gr=3.5, haleine=1.5, queue=[c[0] for c in ch], posture=0)
    return p


def gardien_citadelle():
    """Gardien de la citadelle : grande bete de cristal bleu nuit sur quatre pattes, immenses cornes en lyre, echine herissee, longue queue dressee en crosse."""
    m = M('gardien_citadelle', 267, .815, description='Gardien de la citadelle Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (0, 70)
    C = ('cristal', 'glace')
    D = ('cristal', 'chitine_bleue')
    G = ('glace', 'glace')
    p.bone('body', (0, 34, 0), 'root')
    m.b = 'body'
    m.c('poitrail', (-17, 22, -26), (17, 54, -6), D, base=2.2)
    m.c('ventre', (-15, 24, -6), (15, 48, 14), D, base=2)
    m.c('croupe', (-16, 22, 14), (16, 48, 32), D, base=2.2)
    m.lueur('body', 'coeur', (-5, 30, -29), (5, 44, -26), ('cristal', 'ame'), base=2.8)
    for i, (x, z, h, r, rx, rz, y) in enumerate(((0, -18, 18, 4, -6, 0, 52), (-11, -14, 13, 3, -4, 18, 50), (11, -14, 13, 3, -4, -18, 50), (0, -4, 20, 4, 4, 0, 46), (-11, -2, 13, 3, 6, 18, 44), (11, -2, 13, 3, 6, -18, 44), (0, 10, 18, 4, 10, 0, 46), (-10, 12, 12, 3, 12, 18, 44), (10, 12, 12, 3, 12, -18, 44), (0, 24, 16, 3, 18, 0, 46))):
        m.pointe('body', 'cristal', x, y, z, h, r, C, inc=(rx, rz), base=1.2)
    for i, (y, z, h) in enumerate(((36, -16, 10), (28, -2, 9), (36, 8, 10), (28, 22, 9))):
        m.pointe('body', 'cristal_g', 15, y, z, h, 2, C, inc=(0, -62), base=1.4)
        m.pointe('body', 'cristal_d', -15, y, z, h, 2, C, inc=(0, 62), base=1.4)
    # cou et tete basse
    p.bone('cou', (0, 42, -24), 'body')
    p.bone('head', (0, 34, -40), 'cou')
    m.cb('cou', 'cou_a', (-10, 28, -42), (10, 50, -22), D, base=2.2)
    m.b = 'head'
    m.c('crane', (-10, 24, -56), (10, 42, -38), D, base=2.6)
    m.c('museau', (-7, 22, -66), (7, 34, -56), D, base=2.4)
    m.yeux('head', 4, 33, -57, 4, 3, ramp='ame', base=3.4)
    m.s('narine', (6, 30, -67), (7, 33, -64), G, ton=1, hide=None) if False else None
    p.bone('machoire', (0, 24, -46), 'head')
    m.cb('machoire', 'mandibule', (-6, 17, -64), (6, 24, -44), D, base=2)
    for i, x in enumerate((-5, -1, 3)):
        m.cb('head', f'croc{i}', (x, 18, -66), (x + 2, 23, -64), G, ton=1.4)
    # cornes en lyre : deux chaines d'os depuis le crane, un andouiller a chaque etage
    for sg, n in ((1, 'g'), (-1, 'd')):
        pts_c = [(sg * 8, 40, -44), (sg * 12, 62, -38), (sg * 15, 84, -35), (sg * 15, 104, -28)]

        def cote_c(m_, os_, i, P, W, H, sg=sg, n=n):
            x, y, z = P
            if i < 2:
                m_.pointe(os_, f'and_{n}{i}', x, y + H // 2, z - 2, 11 - 2 * i, 2, C, inc=(-46, -sg * 26), base=1.4)
        m.courbe(pts_c, [4, 3, 2], C, prefixe=f'corne_{n}', parent='head', cote=cote_c, base=1.5)
    # queue dressee en crosse
    pts = [(0, 40, 30), (0, 46, 44), (0, 56, 56), (0, 70, 62), (0, 84, 60), (0, 94, 50), (0, 94, 40)]
    lar = [10, 8, 7, 6, 5, 3]

    def cote(m_, os_, i, P, W, H):
        x, y, z = P
        wx, wz = W
        m_.pointe(os_, f'pic_q{i}', x, y + H // 2, z + wz - 1, 8 + wx, 2, C, inc=(58, 0), base=1.4)
    ch = m.courbe(pts, lar, D, prefixe='queue', parent='body', cote=cote, base=2.2)
    # pattes hautes, griffes de glace
    legs = []
    for nom, x, z, ph in (('fl', 13, -16, 0.0), ('fr', -13, -16, .5), ('bl', 13, 24, .56), ('br', -13, 24, .06)):
        p.bone('leg_' + nom, (x, 34, z), 'root')
        p.bone('leg_' + nom + '_bas', (x, 15, z), 'leg_' + nom)
        legs.append(('leg_' + nom, 'leg_' + nom + '_bas', ph))
    for av, z0, z1 in (('f', -24, -8), ('b', 16, 32)):
        m.sb(f'leg_{av}l|leg_{av}r', f'cuisse_{av}', (6, 14, z0), (20, 38, z1), D, base=2.2)
        m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'canon_{av}', (8, 5, z0 + 2), (18, 16, z1 - 2), D, base=2)
        m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'pied_{av}', (6, 0, z0 - 2), (20, 6, z1 - 1), D, base=1.8)
        for i, x in enumerate((7, 11, 15)):
            m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'griffe_{av}{i}', (x, 0, z0 - 7), (x + 3, 4, z0 - 2), G, ton=1)
    quadrupede_mob(p, legs, jaw='machoire', tail='queue0', amp=20, knee=34, leg_len=34, poids=2, gr=2.5, idle=4.0, walk=1.8, attack=1.2, cou=['cou'] + [c[0] for c in ch[1:]])
    return p


def seigneur_squelette():
    """Seigneur squelette : grand crane sous une capuche dechiree, robe vert-de-gris et pourpre, cristaux roses qui lui poussent sur l'epaule et le crane, long baton a cristal."""
    m = M('seigneur_squelette', 268, .846, description='Seigneur squelette Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=36, epaule=62, cou=65, x_bras=13, x_jambe=5, coude=46, genou=18)
    RG = ('peau_tissu', 'marais')
    RP = ('peau_tissu', 'pourpre')
    RC = ('cristal', 'rose_chaos')
    m.sb('leg_-1|leg_1', 'femur', (3, 18, -2), (6, 37, 2), OS, ton=-.2)
    m.sb('tibia_-1|tibia_1', 'tibia', (3, 3, -2), (6, 19, 2), OS, ton=-.2)
    m.sb('tibia_-1|tibia_1', 'pied', (2, 0, -6), (7, 3, 3), OS)
    m.b = 'body'
    m.c('bassin', (-8, 33, -4), (8, 40, 4), RP, base=2.4)
    m.c('colonne', (-2, 39, 0), (2, 63, 3), OS, ton=-.3)
    cage(m, 44, 4, 8, 5, pas=4, h=2)
    m.c('col', (-10, 60, -6), (10, 65, 6), RG, base=2.6)
    m.c('dos_robe', (-10, 40, 3), (10, 63, 6), RG, base=2.2)
    m.s('flanc_robe', (8, 40, -5), (10, 62, 3), RG, base=2)
    m.c('ceinture', (-9, 39, -5), (9, 43, 5), ('plaque', 'cuir'), base=2.4)
    m.c('sceau_lueur', (-3, 37, -7), (3, 44, -5), RC, base=2.6)
    m.c('robe_av', (-10, 8, -6), (-2, 40, -5), FR('marais'), base=3, mini=.7, pas=2, **CF)
    m.c('robe_av2', (2, 6, -6), (10, 40, -5), FR('marais'), base=3, mini=.7, pas=2, **CF)
    m.c('tablier', (-4, 12, -5), (4, 40, -4), FR('pourpre'), base=3.4, mini=.75, pas=2, **CF)
    m.c('robe_ar', (-11, 4, 5), (11, 40, 6), FR('marais'), base=2.6, mini=.75, pas=2, **CF)
    m.s('robe_c', (10, 6, -5), (11, 40, 5), FR('marais'), base=2.6, mini=.7, pas=2, **CR)
    # tete
    m.b = 'head'
    crane(m, 'head', 7, 65, 82, -8, 6, oeil='rose_chaos')
    m.cb('head', 'capuche', (-9, 77, -9), (9, 85, 8), RG, base=2.8)
    m.cb('head', 'capuche_b', (-9, 64, 5), (9, 77, 8), RG, base=2.4)
    m.sb('head', 'capuche_c', (8, 65, -6), (9, 78, 5), FR('marais'), base=3, mini=.6, **CR)
    m.pointe('head', 'cristal_tete_lueur', 5, 84, -2, 14, 3, RC, inc=(-8, -14), base=1.8)
    m.pointe('head', 'cristal_tete_lueur', -2, 84, 1, 10, 2, RC, inc=(10, 8), base=2)
    m.pointe('head', 'cristal_tete_lueur', -7, 80, 3, 9, 2, RC, inc=(4, 30), base=2)
    # bras d'os, manches larges, cristaux d'epaule
    m.sb('arm_-1|arm_1', 'humerus', (12, 46, -2), (15, 63, 2), OS, ton=-.2)
    m.sb('arm_-1|arm_1', 'manche_h', (10, 54, -6), (18, 66, 6), RP, base=2.6)
    m.sb('avbras_-1|avbras_1', 'radius', (12, 28, -2), (15, 47, 2), OS, ton=-.2)
    m.sb('avbras_-1|avbras_1', 'manche_b', (10, 36, -6), (18, 47, 6), FR('marais'), base=3, mini=.8, hide={'east', 'west', 'up', 'down', 'south'}, no_shadow=True)
    m.sb('avbras_-1|avbras_1', 'main', (11, 22, -3), (16, 29, 3), OS)
    for i, (x, y, z, h, r, rx, rz) in enumerate(((14, 66, -2, 16, 4, -4, -10), (10, 66, 3, 11, 3, 8, 8), (17, 64, 3, 12, 2, 8, -30), (15, 56, -7, 8, 2, -46, -6))):
        m.pointe('arm_-1', 'cristal_ep_lueur', x, y, z, h, r, RC, inc=(rx, rz), base=1.8)
    m.pointe('arm_1', 'cristal_ep_lueur', -14, 66, 0, 9, 2, RC, inc=(0, 12), base=2)
    # baton a cristal, main droite
    p.bone('baton', (14, 26, -7), 'avbras_-1')
    W = ('ecorce', 'bois_sombre')
    m.cb('baton', 'hampe', (13, 0, -8), (15, 72, -6), W, ton=1, pas=2)
    m.cb('baton', 'poing', (11, 24, -9), (16, 29, -5), OS, ton=.2)
    m.cb('baton', 'griffe_haut', (11, 70, -10), (17, 74, -4), OS, ton=.3)
    m.pointe('baton', 'cristal_lueur', 14, 74, -7, 22, 4, RC, inc=(0, 0), base=1.6)
    m.pointe('baton', 'cristal_lueur', 11, 74, -7, 10, 2, RC, inc=(0, 18), base=2)
    m.pointe('baton', 'cristal_lueur', 17, 74, -7, 10, 2, RC, inc=(0, -18), base=2)
    bipede(p, style='sort', idle=3.0, walk=2.0, attack=.96, amp=18, genou=28, bras=8, jambe=36, arme='baton', gr=1.6)
    return p


PACK = {'fernling': fernling, 'champion_gobelin': champion_gobelin, 'voyageur_squelette': voyageur_squelette, 'sentinelle_hibou': sentinelle_hibou, 'minotaure': minotaure, 'morghul': morghul, 'gardien_citadelle': gardien_citadelle, 'seigneur_squelette': seigneur_squelette}
