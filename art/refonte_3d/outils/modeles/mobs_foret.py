#!/usr/bin/env python3
"""Foret ancienne : mousseux, sanglier des ronces, archer sylvestre, veilleur champignon, colosse des racines. En texels de jeu."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from clips_mob import bipede, quadrupede_mob
from kit_mob import CF, CR, FR, M, feuille, losange, triangle
from materiaux import stamp

E = ('ecorce', 'ecorce')
EC = ('ecorce', 'bois')
EB = ('ecorce', 'bois_clair')
EN = ('ecorce', 'bois_sombre')
MO = ('feuilles', 'mousse')
FE = ('feuilles', 'feuillage')
FN = ('feuilles', 'feuillage_sombre')
AMB = {'k': '#120b08', 'a': '#f2b23a', 'b': '#ffe07a', 'd': '#3a1e0a', 'n': '#20120b'}


def mousseux():
    """Souche vivante : tronc large et court, sommet brise couvert de mousse, deux yeux d'ambre au fond d'une fente, bouche d'echardes, bras de racines detaches du corps, pieds en griffes."""
    m = M('mousseux', 201, 2.759, description='Mousseux Aeloria : souche qui marche, refonte (16 texels par bloc en jeu).')
    m.humanoide(hanche=13, epaule=26, cou=28, x_bras=15, x_jambe=6, coude=18, genou=6)
    m.sb('leg_-1|leg_1', 'cuisse', (3, 5, -4), (10, 14, 4), EC, ton=.2, hide={'up'})
    m.sb('tibia_-1|tibia_1', 'mollet', (3, 2, -4), (9, 6, 4), EC, ton=.4)
    m.sb('tibia_-1|tibia_1', 'pied', (2, 0, -7), (10, 3, 5), E, ton=.2)
    m.sb('tibia_-1|tibia_1', 'griffe', (2, 0, -10), (4, 2, -7), EB, ton=-.3)
    m.sb('tibia_-1|tibia_1', 'griffe', (7, 0, -9), (9, 2, -7), EB, ton=-.3)
    # tronc : section a pans coupes, ceinture de racines, bouche creusee
    m.b = 'body'
    m.c('tronc', (-11, 12, -7), (11, 27, 7), E, ton=.5)
    m.c('tronc_avant', (-8, 12, -9), (8, 26, 9), E, ton=.3)
    m.c('bas', (-10, 10, -8), (10, 13, 8), EN, ton=1.2)
    m.s('racine_ventre', (6, 9, -11), (9, 17, -9), EN, ton=1.6)
    m.c('racine_ventre2', (-9, 9, -11), (-6, 15, -9), EN, ton=1.6)
    m.c('bouche', (-5, 17, -10), (5, 22, -9), ('uni', 'soie_noire'), base=.5, no_ao=True, hide={'south'})
    for i, (x, y, h) in enumerate(((-4, 19, 3), (0, 19, 3), (3, 20, 2))):
        m.c(f'echarde{i}', (x, y, -11), (x + 1, y + h, -10), EB, ton=.8, no_ao=True, hide={'south'})
    for i, (x, h) in enumerate(((-3, 3), (1, 2))):
        m.c(f'echarde_b{i}', (x, 17, -11), (x + 2, 17 + h, -10), EB, ton=.6, no_ao=True, hide={'south'})
    # tete : sommet du tronc, fente d'yeux, arcade, mousse et eclats brises
    m.b = 'head'
    m.c('souche', (-10, 26, -7), (10, 34, 7), E, ton=.7)
    m.c('souche_avant', (-7, 26, -8), (7, 33, 8), E, ton=.5)
    m.c('fente', (-8, 27, -9), (8, 32, -8), ('uni', 'soie_noire'), base=.5, no_ao=True, hide={'south'})
    m.c('arcade', (-9, 32, -10), (9, 34, -8), EC, ton=.8)
    m.yeux('head', 2, 28, -10, 4, 4, ramp='ambre', base=3.2, dz=2)
    m.p.decal('oeil_lueur_g', 'north', stamp(['bbab', 'bkka', 'bkka', 'aaaa'], AMB, 6, 32))
    m.c('eclat', (-10, 33, 3), (-7, 39, 6), EB, ton=.2, rot=(4, 0, 8), pivot=(-8, 33, 4))
    m.c('eclat', (7, 33, 2), (10, 37, 5), EB, rot=(-5, 0, -8), pivot=(8, 33, 4))
    m.c('eclat', (-3, 33, 5), (1, 40, 8), EB, ton=-.2, rot=(10, 0, 0), pivot=(0, 33, 6))
    m.c('mousse', (-12, 34, -9), (12, 36, 9), MO, ton=.1)
    m.c('mousse_haut', (-9, 36, -7), (7, 38, 4), MO, ton=.6)
    m.c('mousse_haut2', (-5, 38, -5), (3, 39, 1), MO, ton=1)
    m.c('mousse_face', (-11, 31, -11), (-5, 35, -10), FR('mousse'), base=2.2, **CF)
    m.c('mousse_face', (4, 30, -11), (11, 35, -10), FR('mousse'), base=2.4, **CF)
    m.s('mousse_flanc', (12, 27, -8), (13, 35, 8), FR('mousse'), base=2.2, mini=.3, **CR)
    m.c('mousse_dos', (-11, 25, 9), (11, 35, 10), FR('mousse'), base=2.2, mini=.25, **CF)
    m.touffe('head', 'pousse', 5, 38, -2, 5, 2, 'feuillage', 3.0)
    m.touffe('head', 'pousse', -6, 38, 1, 4, 2, 'feuillage', 3.4)
    m.champi('head', 'champi', 8, 36, 5, 2, 2)
    # bras : racines noueuses, plus claires que le tronc, mains griffues, mousse sur l'epaule
    m.sb('arm_-1|arm_1', 'epaule', (13, 21, -4), (19, 28, 4), EC, ton=.5)
    m.sb('arm_-1|arm_1', 'bras', (14, 17, -3), (18, 22, 3), EC, ton=.3)
    m.sb('arm_-1|arm_1', 'mousse_epaule', (12, 28, -5), (20, 30, 5), MO, ton=.3)
    m.sb('avbras_-1|avbras_1', 'avantbras', (13, 10, -3), (18, 18, 3), EC, ton=.5)
    m.sb('avbras_-1|avbras_1', 'doigt', (12, 5, -3), (14, 11, -1), EB, ton=-.2, rot=(0, 0, -8), pivot=(13, 11, -2))
    m.sb('avbras_-1|avbras_1', 'doigt', (17, 5, -3), (19, 11, -1), EB, ton=-.2, rot=(0, 0, 8), pivot=(18, 11, -2))
    m.sb('avbras_-1|avbras_1', 'doigt', (14, 6, 0), (16, 11, 2), EB, ton=-.3)
    bipede(m.p, style='griffes', amp=24, genou=34, bras=20, jambe=9, haleine=.5)
    return m.p


def sanglier_ronce():
    """Sanglier des ronces : garrot massif, echine qui descend vers la croupe, chanfrein en pente, grandes defenses, ronces epineuses et feuilles sur tout le dos, yeux d'ambre."""
    m = M('sanglier_ronce', 202, 2.936, description='Sanglier des ronces Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (8, 34)
    FUR = ('fourrure', 'fourrure_brune')
    SO = ('fourrure', 'soie_noire')
    UM = ('uni', 'museau_gris')
    p.bone('body', (0, 20, 0), 'root')
    m.b = 'body'
    m.c('poitrail', (-11, 13, -20), (11, 30, -8), FUR, ton=.1)
    m.c('poitrail_avant', (-8, 15, -22), (8, 28, -20), FUR)
    m.c('sternum', (-8, 11, -19), (8, 13, -8), SO, ton=.2)
    m.c('garrot', (-9, 30, -19), (9, 33, -9), FUR, ton=.3)
    m.c('ventre', (-10, 14, -8), (10, 28, 11), FUR)
    m.c('croupe', (-9, 15, 11), (9, 27, 18), FUR, ton=.1)
    m.c('croupe_haut', (-7, 27, 11), (7, 29, 17), FUR, ton=.2)
    m.c('fesse', (-7, 16, 18), (7, 25, 21), FUR, ton=-.2)
    m.s('epaule', (11, 16, -19), (12, 28, -9), FUR, ton=.4)
    m.s('hanche', (9, 17, 11), (10, 26, 18), FUR, ton=.4)
    # toison de ronces : coussins de feuilles irreguliers, branches epineuses, retombes sur les flancs
    m.c('feuilles_a', (-8, 33, -18), (5, 37, -9), FE, ton=.2)
    m.c('feuilles_a2', (-3, 37, -16), (4, 39, -11), FE, ton=.8)
    m.c('feuilles_b', (-7, 28, -8), (8, 32, 2), FN, ton=.9)
    m.c('feuilles_c', (-8, 28, 2), (3, 31, 11), FE, ton=-.1)
    m.c('feuilles_d', (-5, 29, 11), (6, 32, 18), FN, ton=.7)
    m.c('feuilles_e', (4, 30, -18), (10, 34, -12), FN, ton=.8)
    m.s('retombe', (10, 19, -19), (11, 33, -8), FR('feuillage_sombre'), base=2.8, mini=.3, **CR) if False else None
    m.s('retombe', (12, 21, -19), (13, 31, -8), FR('feuillage_sombre'), base=2.8, mini=.2, **CR)
    m.s('retombe2', (11, 20, -7), (12, 29, 11), FR('feuillage'), base=2.2, mini=.2, **CR)
    RO = ('ecorce', 'bois_sombre')
    for i, (x, z, h, rx, rz) in enumerate(((-6, -14, 10, -18, 16), (7, -15, 11, -8, -22), (-1, -8, 9, 14, 8), (7, -2, 11, 20, -24), (-7, 2, 10, 8, 26), (2, 8, 9, -14, -12), (-5, 14, 8, 22, 18), (5, 15, 8, 26, -20))):
        ya = 31 if z < -9 else 29
        n = m.c(f'ronce{i}', (x - 1, ya, z - 1), (x + 1, ya + h, z + 1), RO, ton=1.2, pas=2, rot=(rx, 0, rz), pivot=(x, ya, z))
        m.c(f'epine{i}', (x + 1, ya + h - 4, z), (x + 3, ya + h - 3, z + 1), ('griffe', 'ivoire'), base=2.2, rot=(rx, 0, rz), pivot=(x, ya, z))
        m.c(f'epine_b{i}', (x - 3, ya + h - 7, z), (x - 1, ya + h - 6, z + 1), ('griffe', 'ivoire'), base=2.2, rot=(rx, 0, rz), pivot=(x, ya, z))
    m.touffe('body', 'pousse', -3, 39, -14, 5, 3, 'feuillage', 3)
    m.touffe('body', 'pousse', 5, 32, 3, 7, 3, 'feuillage', 3.4)
    m.touffe('body', 'pousse', -4, 31, 8, 6, 3, 'feuillage_sombre', 3.4)
    # tete basse : cou, crane, chanfrein en pente, groin, defenses, oreilles
    p.bone('head', (0, 22, -20), 'body')
    m.b = 'head'
    m.c('cou', (-8, 15, -27), (8, 29, -17), SO, ton=.9)
    m.c('crane', (-7, 14, -35), (7, 23, -26), FUR, ton=.3)
    m.c('crane_haut', (-6, 23, -30), (6, 27, -26), FUR, ton=.5)
    m.c('face', (-6, 14, -39), (6, 21, -35), FUR, ton=-.1)
    m.c('nez', (-5, 23, -45), (5, 27, -26), FUR, ton=.3, rot=(-25, 0, 0), pivot=(0, 27, -27))
    m.c('chanfrein', (-1, 27, -42), (1, 28, -28), ('uni', 'soie_noire'), base=3.2, rot=(-25, 0, 0), pivot=(0, 27, -27))
    m.c('museau', (-4, 12, -43), (4, 18, -39), UM, base=3)
    m.c('groin', (-5, 12, -44), (5, 20, -43), ('uni', 'museau_rose'), base=2.4, no_ao=True)
    p.decal('groin', 'north', stamp(['..........', '..kk..kk..', '..kk..kk..', '..........', '..........'], {'k': '#20120b'}, 5, 18))
    m.s('favori', (7, 10, -34), (8, 19, -26), ('crete', 'soie_noire'), base=3, ecart=2, bas=True, pas=3, dent=4, **CR)
    m.c('sourcil_g', (5, 23, -36), (8, 25, -29), SO, ton=1, rot=(-13, 0, 0), pivot=(6, 24, -32))
    m.c('sourcil_d', (-8, 23, -36), (-5, 25, -29), SO, ton=1, rot=(-13, 0, 0), pivot=(-6, 24, -32))
    m.yeux('head', 4, 19, -36, 3, 3, ramp='ambre', base=3.4)
    p.decal('oeil_lueur_g', 'north', stamp(['aaa', 'kka', 'kaa'], AMB, 7, 22))
    p.bone('machoire', (0, 14, -30), 'head')
    m.b = 'machoire'
    m.c('mandibule', (-5, 10, -41), (5, 14, -30), UM, base=2)
    D_ = ('corne', 'ivoire')
    m.s('defense', (5, 11, -42), (8, 17, -39), D_, base=3, rot=(14, 0, -13), pivot=(6, 11, -40))
    m.s('defense_pointe', (6, 16, -43), (8, 22, -41), D_, base=3.6, rot=(-16, 0, -22), pivot=(7, 16, -42))
    m.s('defense_bout', (9, 21, -44), (10, 25, -43), D_, base=4.2, rot=(-26, 0, -32), pivot=(9, 21, -43))
    m.s('bague', (4, 11, -43), (9, 14, -39), ('metal', 'bois_sombre'), base=2.4) if False else None
    p.bone('ear_g', (6, 27, -28), 'head')
    p.bone('ear_d', (-6, 27, -28), 'head')
    OR_ = ['....##', '...###', '..####', '.#####', '######', '.####.']
    m.sb('ear_g|ear_d', 'oreille', (6, 26, -29), (12, 32, -28), ('plan', 'soie_noire'), base=3, masque=OR_, rot=(8, -24, -20), pivot=(6, 27, -28), **CF)
    m.c('crete_cou', (-1, 26, -28), (1, 36, -16), ('crete_arriere', 'soie_noire'), base=1.4, rot=(-10, 0, 0), pivot=(0, 28, -22), **CR) if False else None
    # queue
    p.bone('tail', (0, 24, 21), 'body')
    m.cb('tail', 'queue', (-1, 16, 21), (1, 25, 23), ('uni', 'soie_noire'), base=2.2, rot=(-24, 0, 0), pivot=(0, 25, 22))
    p.bone('tail_bout', (0, 16, 25), 'tail')
    m.cb('tail_bout', 'queue_touffe', (-2, 10, 24), (2, 17, 27), FN, ton=.4)
    # pattes courtes et fortes, sabots
    jambes = []
    for nom, x, z, y, ph, zk in (('fl', 7, -14, 20, 0.0, -14), ('fr', -7, -14, 20, .5, -14), ('bl', 7, 14, 20, .56, 15), ('br', -7, 14, 20, .06, 15)):
        p.bone('leg_' + nom, (x, y, z), 'root')
        p.bone('leg_' + nom + '_bas', (x, 10, zk), 'leg_' + nom)
        jambes.append(('leg_' + nom, 'leg_' + nom + '_bas', ph))
    m.sb('leg_fl|leg_fr', 'avantbras', (4, 9, -18), (10, 21, -11), FUR, ton=-.2, y=(0, 30), hide={'up'})
    m.sb('leg_fl_bas|leg_fr_bas', 'canon_av', (5, 3, -17), (9, 10, -12), SO, ton=.5, y=(0, 30))
    m.sb('leg_fl_bas|leg_fr_bas', 'sabot_av', (4, 0, -18), (10, 3, -11), ('sabot', 'sabot'), base=2)
    m.sb('leg_bl|leg_br', 'cuisse', (4, 10, 11), (10, 21, 18), FUR, ton=-.1, y=(0, 30), hide={'up'})
    m.sb('leg_bl_bas|leg_br_bas', 'jarret', (5, 3, 12), (9, 11, 17), SO, ton=.5, y=(0, 30))
    m.sb('leg_bl_bas|leg_br_bas', 'sabot_ar', (4, 0, 11), (10, 3, 18), ('sabot', 'sabot'), base=2)
    quadrupede_mob(p, jambes, jaw='machoire', ears=('ear_g', 'ear_d'), tail_tip='tail_bout', amp=26, knee=44, leg_len=20, poids=1.4, gr=1.4)
    return p


def archer_sylvestre():
    """Archer sylvestre : corps de bois fin, capuche de feuilles, visage creux et yeux d'ambre, arc noue a la main gauche, carquois de fleches."""
    m = M('archer_sylvestre', 203, 2.484, description='Archer sylvestre Aeloria : refonte (16 texels par bloc en jeu).')
    m.humanoide(hanche=28, epaule=46, cou=48, x_bras=8, x_jambe=4, coude=36, genou=14)
    # jambes fines, tibia noueux, pied en racine
    m.sb('leg_-1|leg_1', 'cuisse', (2, 14, -3), (6, 29, 3), EC, ton=.3)
    m.sb('leg_-1|leg_1', 'lierre_cuisse', (1, 18, -4), (7, 22, 4), FN, ton=.6)
    m.sb('tibia_-1|tibia_1', 'tibia', (2, 3, -2), (6, 15, 2), E, ton=.4)
    m.sb('tibia_-1|tibia_1', 'genou', (2, 12, -3), (6, 15, -2), EB, ton=.2)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -6), (7, 3, 3), E)
    # tronc : bassin, taille fine, buste, plastron de feuilles
    m.b = 'body'
    m.c('bassin', (-6, 26, -4), (6, 32, 4), EC, ton=.2)
    m.c('taille', (-5, 32, -3), (5, 38, 3), E, ton=.5)
    m.c('buste', (-7, 38, -4), (7, 47, 4), EC, ton=.4)
    m.c('plastron', (-6, 37, -5), (6, 46, -4), FE, ton=.2)
    m.c('tablier', (-5, 22, -5), (5, 31, -4), FR('feuillage'), base=2.4, mini=.45, **CF)
    m.c('tablier_dos', (-5, 22, 4), (5, 31, 5), FR('feuillage_sombre'), base=2.4, mini=.45, **CF)
    m.c('sangle', (-7, 40, -6), (-5, 41, 6), ('cuir', 'cuir'), ton=-.4, couture=False, rot=(0, 0, 0))
    # carquois dans le dos, fleches
    m.c('carquois', (2, 32, 5), (7, 47, 9), ('cuir', 'cuir'), rot=(0, 0, -16), pivot=(4, 40, 7))
    for i, (x, y) in enumerate(((3, 47), (5, 48), (6, 47))):
        m.c(f'fleche{i}', (x, y, 6), (x + 1, y + 6, 7), ('bois', 'bois_clair'), rot=(0, 0, -16), pivot=(4, 40, 7))
        m.c(f'empenne{i}', (x - 1, y + 5, 6), (x + 2, y + 8, 7), ('plan', 'feuillage'), base=3.4, masque=['.#.', '###', '#.#'], rot=(0, 0, -16), pivot=(4, 40, 7), **CF)
    # tete : capuche de feuilles, visage creux
    m.b = 'head'
    m.c('visage', (-4, 49, -4), (4, 57, 4), EC, ton=.6)
    m.c('masque_creux', (-3, 50, -5), (3, 56, -4), ('uni', 'soie_noire'), base=.5, no_ao=True, hide={'south'})
    m.yeux('head', 1, 52, -5, 2, 3, ramp='ambre', base=3.6)
    m.c('capuche', (-6, 56, -6), (6, 61, 6), FE, ton=.3)
    m.c('capuche_haut', (-4, 61, -3), (4, 64, 5), FE, ton=.8)
    m.c('capuche_bord', (-7, 47, -6), (7, 55, -5), FR('feuillage'), base=2.4, mini=.3, ajoure=True, **CF) if False else None
    m.s('capuche_flanc', (6, 47, -5), (7, 57, 6), FR('feuillage'), base=2.2, mini=.35, **CR)
    m.c('capuche_dos', (-6, 44, 6), (6, 57, 7), FR('feuillage_sombre'), base=2.2, mini=.3, **CF)
    m.c('capuche_cou', (-5, 47, -5), (5, 50, 5), FN, ton=.4)
    m.touffe('head', 'pousse', 2, 64, 1, 6, 3, 'feuillage', 3.2)
    # bras fins
    m.sb('arm_-1|arm_1', 'epaule', (6, 43, -3), (11, 48, 3), FE, ton=.5)
    m.sb('arm_-1|arm_1', 'bras', (7, 36, -2), (10, 44, 2), EC, ton=.3)
    m.sb('avbras_-1|avbras_1', 'avantbras', (7, 26, -2), (10, 37, 2), E, ton=.4)
    m.sb('avbras_-1|avbras_1', 'bracelet', (6, 30, -3), (11, 33, 3), FN, ton=.6)
    m.sb('avbras_-1|avbras_1', 'main', (7, 22, -2), (10, 26, 2), EB, ton=.1)
    # arc en bois noue, tenu main gauche (cote -x)
    p = m.p
    p.bone('arc', (-8.5, 25, 0), 'avbras_1')
    B = ('bois', 'bois_clair')
    m.b = 'arc'
    m.c('poignee', (-10, 21, -6), (-7, 29, -3), ('cuir', 'cuir'), couture=False)
    m.c('branche_h', (-10, 28, -6), (-8, 41, -4), B, rot=(16, 0, 0), pivot=(-9, 28, -5))
    m.c('branche_b', (-10, 9, -6), (-8, 22, -4), B, rot=(-16, 0, 0), pivot=(-9, 22, -5))
    m.c('bout_h', (-10, 40, -3), (-8, 46, -1), EN, ton=1.4, rot=(36, 0, 0), pivot=(-9, 40, -2))
    m.c('bout_b', (-10, 4, -3), (-8, 10, -1), EN, ton=1.4, rot=(-36, 0, 0), pivot=(-9, 10, -2))
    m.c('corde', (-9, 6, 2), (-8, 44, 3), ('uni', 'chair_pale'), base=3.6, no_ao=True, hide={'up', 'down'})
    m.touffe('arc', 'feuille_arc', -9, 33, -6, 4, 2, 'feuillage', 3.2)
    bipede(p, style='arc', amp=24, genou=44, bras=20, jambe=28, haleine=.4)
    return p


def veilleur_champignon():
    """Veilleur champignon : pied pale et trapu, grand chapeau rouge a taches blanches et lamelles dessous, visage endormi, bras courts, mousse au pied."""
    m = M('veilleur_champignon', 204, 2.867, description='Veilleur champignon Aeloria : refonte (16 texels par bloc en jeu).')
    m.humanoide(hanche=13, epaule=31, cou=33, x_bras=11, x_jambe=5, coude=24, genou=6)
    PA = ('uni', 'chair_pale')
    m.sb('leg_-1|leg_1', 'cuisse', (2, 6, -4), (9, 14, 4), EC, ton=.3, hide={'up'})
    m.sb('tibia_-1|tibia_1', 'mollet', (2, 2, -4), (8, 7, 4), E, ton=.3)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -7), (9, 3, 5), EC)
    # pied du champignon : renfle en bas, resserre en haut
    m.b = 'body'
    m.c('pied_bas', (-10, 11, -8), (10, 22, 8), PA, base=3.2)
    m.c('pied_avant', (-8, 12, -9), (8, 21, 9), PA, base=3.6)
    m.c('pied_haut', (-8, 22, -7), (8, 35, 7), PA, base=3.6)
    m.c('pied_collier', (-10, 31, -8), (10, 34, 8), ('uni', 'lamelles'), base=2.4)
    # jupe de mousse et racines
    m.c('mousse_pied', (-11, 11, -10), (11, 15, 10), MO, ton=.3)
    m.c('mousse_face', (-11, 15, -11), (-2, 19, -10), FR('mousse'), base=2.6, mini=.4, **CF) if False else None
    m.s('mousse_cote', (11, 12, -8), (12, 19, 8), FR('mousse'), base=2.6, mini=.25, **CR)
    m.touffe('body', 'herbe', 8, 15, -7, 5, 2, 'feuillage', 3)
    m.touffe('body', 'herbe', -7, 15, 8, 5, 2, 'feuillage', 3.4)
    m.champi('body', 'petit', -9, 15, -6, 2, 2)
    m.champi('body', 'petit', 10, 15, 3, 1, 1)
    # visage endormi : sourcils, yeux d'ambre fins, petite bouche
    m.b = 'head'
    m.c('front', (-8, 34, -8), (8, 42, 8), PA, base=3.8)
    m.c('barbe', (-7, 30, -9), (7, 36, -8), FR('lamelles'), base=3, mini=.6, **CF) if False else None
    m.c('orbites', (-6, 35, -9), (6, 39, -8), ('uni', 'soie_noire'), base=1.2, no_ao=True, hide={'south'})
    m.yeux('head', 2, 35, -10, 3, 3, ramp='ambre', base=3.4, dz=2)
    m.p.decal('oeil_lueur_g', 'north', stamp(['bba', 'bka', 'aaa'], AMB, 5, 38))
    m.c('sourcils', (-7, 39, -10), (7, 40, -8), ('uni', 'bois_sombre'), base=3, no_ao=True, hide={'south'})
    m.c('bouche_v', (-3, 29, -8), (3, 30, -7), ('uni', 'soie_noire'), base=1, no_ao=True, hide={'south'})
    # chapeau : trois etages, bord tombant, lamelles dessous, taches blanches en cubes plats
    p = m.p
    p.bone('chapeau', (0, 41, 0), 'head')
    C = ('chapeau', 'champignon')
    m.b = 'chapeau'
    m.c('lamelles_dessous', (-15, 42, -14), (15, 43, 14), ('rayures', 'lamelles'), base=3, pas=3, large=1, hide={'up'})
    m.c('bord', (-17, 43, -15), (17, 47, 15), C, ton=-.2)
    m.c('bord_avant', (-14, 43, -17), (14, 47, 17), C, ton=-.3)
    m.c('dome', (-14, 47, -13), (14, 53, 13), C, ton=.4)
    m.c('dome_haut', (-10, 53, -9), (10, 57, 9), C, ton=.8)
    m.c('sommet', (-5, 57, -5), (5, 59, 5), C, ton=1.2)
    W = ('uni', 'chair_pale')
    for i, (x, y, z, w, d, ou) in enumerate(((-11, 47, -15, 5, 1, 'av'), (4, 45, -18, 6, 1, 'av'), (-3, 50, -14, 5, 1, 'av'))):
        m.c(f'tache_av{i}', (x, y, z), (x + w, y + 3 + i % 2, z + d), W, base=4.4, no_ao=True, hide={'south'})
    for i, (x, z, w, d, y) in enumerate(((-10, -7, 6, 5, 53), (3, 2, 6, 5, 53), (-3, -4, 5, 4, 57), (-12, 3, 5, 6, 53), (6, -9, 5, 4, 53))):
        m.c(f'tache_d{i}', (x, y, z), (x + w, y + 1, z + d), W, base=4.4, no_ao=True, hide={'down'})
    for i, (y, z, d) in enumerate(((43, -6, 6), (44, 4, 5), (47, -2, 6))):
        m.s(f'tache_c{i}', (17 if y < 47 else 14, y, z), (18 if y < 47 else 15, y + 3, z + d), W, base=4.4, no_ao=True, hide={'west'})
    # bras courts et ronds
    m.sb('arm_-1|arm_1', 'epaule', (8, 25, -3), (13, 33, 3), PA, base=3.4)
    m.sb('avbras_-1|avbras_1', 'avantbras', (9, 18, -3), (13, 25, 3), PA, base=3.2)
    m.sb('avbras_-1|avbras_1', 'main', (8, 14, -3), (14, 18, 3), EC, ton=.4)
    m.sb('avbras_-1|avbras_1', 'mousse_bras', (8, 22, -4), (14, 25, 4), MO, ton=.4)
    bipede(p, style='griffes', amp=22, genou=34, bras=16, jambe=9, haleine=.5, cape=('chapeau',))
    return p


def colosse_racine():
    """Colosse des racines : golem-arbre haut de presque onze blocs. Epaules massives, bras jusqu'aux genoux, bois de cerf en branches, gemme d'ambre au coeur, mousse et lianes."""
    m = M('colosse_racine', 205, 4.174, description='Colosse des racines Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=50, epaule=102, cou=108, x_bras=38, x_jambe=17, coude=68, genou=25, z_cou=-3)
    # jambes : troncs enracines, faisceaux de racines au sol
    for av in ('', '2'):
        pass
    m.sb('leg_-1|leg_1', 'cuisse', (7, 24, -13), (28, 53, 13), E, ton=.3, pas=6, hide={'up'})
    m.sb('leg_-1|leg_1', 'cuisse_avant', (10, 26, -15), (25, 50, -13), EC, ton=.2, pas=6)
    m.sb('leg_-1|leg_1', 'lierre', (6, 40, -16), (29, 48, 15), FR('feuillage_sombre'), base=2.4, mini=.3, **CR) if False else None
    m.sb('tibia_-1|tibia_1', 'tibia', (8, 8, -12), (27, 26, 12), EC, ton=.3, pas=6)
    m.sb('tibia_-1|tibia_1', 'tibia_avant', (11, 10, -14), (24, 24, -12), E, ton=.3, pas=6)
    m.sb('tibia_-1|tibia_1', 'pied', (6, 0, -17), (29, 9, 15), E, pas=6)
    for i, (x, z0, l, r) in enumerate(((5, -30, 14, -12), (14, -33, 17, 0), (24, -30, 14, 12))):
        m.sb('tibia_-1|tibia_1', f'orteil{i}', (x, 0, z0), (x + 7, 7, z0 + l), EN, ton=1, pas=4, rot=(0, r, 0), pivot=(x + 3, 3, z0 + l))
    m.sb('tibia_-1|tibia_1', 'talon', (11, 0, 15), (24, 6, 23), EN, ton=.8, pas=4)
    # tronc : bassin, ventre, poitrail tres large, dos couvert de mousse
    m.b = 'body'
    m.c('bassin', (-26, 44, -16), (26, 62, 16), E, ton=.2, pas=6)
    m.c('ventre', (-24, 62, -17), (24, 82, 17), E, ton=.4, pas=6)
    m.c('poitrail', (-32, 82, -20), (32, 108, 20), E, ton=.5, pas=6)
    m.c('pectoral', (-27, 86, -23), (27, 106, -20), EC, ton=.6, pas=6)
    m.s('flanc_racine', (24, 60, -6), (29, 84, 8), EN, ton=1, pas=4, rot=(0, 0, 4), pivot=(26, 72, 0))
    m.c('ceinture_lianes', (-27, 60, -18), (27, 65, 18), EN, ton=1.2, pas=4)
    m.c('mousse_dos', (-30, 92, 20), (30, 108, 22), MO, ton=.2)
    m.c('mousse_dos_bas', (-29, 64, 21), (29, 92, 22), FR('mousse'), base=2.4, mini=.25, **CF)
    m.c('mousse_ventre', (-22, 78, -19), (-6, 84, -18), MO, ton=.4)
    # coeur d'ambre : gemme a facettes dans un cadre de racines
    m.c('cadre', (-11, 66, -19), (11, 82, -17), EN, ton=1, pas=4)
    m.lueur('body', 'gemme', (-7, 69, -21), (7, 80, -19), ('cristal', 'ambre'), base=2.6)
    m.lueur('body', 'gemme_c', (-4, 71, -23), (4, 78, -21), ('cristal', 'ambre'), base=3.6)
    # tete : souche sculptee, machoire de bois, yeux d'ambre, bois de cerf
    m.b = 'head'
    m.c('crane', (-16, 106, -22), (16, 130, 8), E, ton=.6, pas=6)
    m.c('front', (-14, 124, -24), (14, 132, 6), EC, ton=.8, pas=6)
    m.c('joues', (-18, 104, -18), (18, 118, 4), E, ton=.3, pas=6)
    m.c('fente', (-11, 114, -23), (11, 119, -22), ('uni', 'soie_noire'), base=.6, no_ao=True, hide={'south'})
    m.yeux('head', 3, 114, -23, 7, 4, ramp='ambre', base=3.4)
    m.c('sourcil', (-14, 119, -26), (14, 124, -22), EN, ton=1.6, pas=4)
    m.c('nez', (-3, 110, -25), (3, 119, -22), EC, ton=.5, pas=3)
    p.bone('machoire', (0, 108, -8), 'head')
    m.b = 'machoire'
    m.c('mandibule', (-14, 97, -23), (14, 108, -2), EC, ton=.2, pas=6)
    for i, x in enumerate((-10, -3, 4)):
        m.c(f'dent{i}', (x, 108, -24), (x + 5, 112, -19), EB, ton=.5)
    m.c('barbe_mousse', (-13, 86, -24), (13, 98, -23), FR('mousse'), base=2.4, mini=.3, **CF)
    # bois de cerf : perches, andouillers
    B = ('ecorce', 'bois_clair')
    for s, os_ in ((1, 'bois_g'), (-1, 'bois_d')):
        p.bone(os_, (s * 12, 130, -8), 'head')
    m.sb('bois_g|bois_d', 'perche', (9, 129, -11), (15, 156, -5), B, ton=.2, pas=3, rot=(-6, 0, -18), pivot=(12, 130, -8))
    m.sb('bois_g|bois_d', 'perche2', (19, 152, -9), (24, 172, -4), B, ton=.4, pas=3, rot=(-6, 0, -30), pivot=(21, 153, -7))
    m.sb('bois_g|bois_d', 'andouiller', (17, 142, -8), (21, 160, -4), B, ton=.4, pas=3, rot=(10, 0, 32), pivot=(19, 142, -6))
    m.sb('bois_g|bois_d', 'andouiller2', (26, 158, -10), (30, 172, -6), B, ton=.6, pas=3, rot=(-10, 0, 8), pivot=(28, 158, -8))
    m.sb('bois_g|bois_d', 'andouiller3', (34, 162, -8), (37, 173, -5), B, ton=.6, pas=3, rot=(12, 0, -42), pivot=(35, 162, -6))
    m.sb('bois_g|bois_d', 'andouiller4', (22, 148, -2), (25, 162, 1), B, ton=.5, pas=3, rot=(26, 0, -8), pivot=(23, 148, 0))
    for i, (x, y, z, w, h) in enumerate(((23, 164, -7, 8, 9), (32, 168, -7, 6, 5), (17, 154, 0, 7, 6), (9, 136, -13, 7, 5))):
        m.sb('bois_g|bois_d', f'feuille_bois{i}', (x, y, z), (x + w, y + h, z + 6), FE, ton=.3)
    m.c('mousse_tete', (-17, 130, -22), (17, 134, 8), MO, ton=.5) if False else None
    m.cb('head', 'mousse_tete', (-16, 132, -23), (16, 136, 7), MO, ton=.5)
    m.cb('head', 'mousse_tete2', (-10, 136, -16), (11, 139, 3), MO, ton=1)
    m.touffe('head', 'pousse', 0, 139, -7, 10, 5, 'feuillage', 3)
    # bras immenses, epaulieres de souche, mains-griffes de racines
    m.sb('arm_-1|arm_1', 'epaule', (28, 88, -18), (52, 112, 18), E, ton=.6, pas=6)
    m.sb('arm_-1|arm_1', 'mousse_epaule', (27, 112, -19), (53, 117, 19), MO, ton=.5)
    m.sb('arm_-1|arm_1', 'touffe_epaule', (34, 117, -12), (47, 120, 8), FE, ton=.5)
    m.sb('arm_-1|arm_1', 'mousse_epaule_pend', (52, 96, -18), (53, 112, 18), FR('mousse'), base=2.4, mini=.3, **CR)
    m.sb('arm_-1|arm_1', 'bras', (30, 66, -13), (48, 90, 13), EC, ton=.3, pas=6)
    m.sb('avbras_-1|avbras_1', 'avantbras', (28, 36, -14), (51, 68, 14), E, ton=.5, pas=6)
    m.sb('avbras_-1|avbras_1', 'racine_avbras', (51, 42, -4), (54, 64, 6), EN, ton=1.2, pas=3)
    m.sb('avbras_-1|avbras_1', 'poignet', (31, 30, -12), (48, 36, 12), EN, ton=1.2, pas=4)
    m.sb('avbras_-1|avbras_1', 'main', (28, 20, -14), (51, 30, 14), EC, ton=.3, pas=6)
    for i, (x, z, r) in enumerate(((28, -13, -10), (37, -14, 0), (45, -13, 10), (37, 8, 0))):
        m.sb('avbras_-1|avbras_1', f'griffe{i}', (x, 2, z), (x + 6, 20, z + 6), EB, ton=.1, pas=3, rot=(10 if z < 0 else -10, 0, r * -1), pivot=(x + 3, 20, z + 3))
    bipede(p, style='poing', amp=20, genou=26, bras=15, jambe=50, gr=4, jaw='machoire', haleine=1.2)
    return p


FORET = {'mousseux': mousseux, 'sanglier_ronce': sanglier_ronce, 'archer_sylvestre': archer_sylvestre, 'veilleur_champignon': veilleur_champignon, 'colosse_racine': colosse_racine}
