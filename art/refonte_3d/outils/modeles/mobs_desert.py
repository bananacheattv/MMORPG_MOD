#!/usr/bin/env python3
"""Desert volcanique : scorpion de cuivre, momie des dunes, hyene cendree, djinn des braises, tortue volcanique. En texels de jeu."""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from clips_mob import bipede, octo_mob, quadrupede_mob
from kit_mob import CF, CR, FR, M, RE, losange, triangle
from materiaux import stamp

CU = ('segments', 'cuivre')
CUP = ('plaque', 'cuivre')
CS = ('uni', 'cuivre_sombre')
BA = ('bandelette', 'bandelette')
BS = ('bandelette', 'bandelette_sale')
K = {'k': '#0b0a0f', 'p': '#c98cff', 'P': '#f4e0ff', 'r': '#e0342a', 'y': '#ffe16a', 'o': '#fb8f22', 'n': '#20120b'}


def scorpion_cuivre():
    """Scorpion de cuivre : plaques de cuivre chaud, bords vert-de-gris, grandes pinces, queue arquee par-dessus le dos, dard sombre, petits yeux turquoise."""
    m = M('scorpion_cuivre', 241, 2.544, description='Scorpion de cuivre Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.bone('body', (0, 9, 0), 'root')
    m.b = 'body'
    m.c('thorax', (-10, 6, -12), (10, 14, 6), CU, pas=6)
    m.c('thorax_h', (-8, 14, -10), (8, 16, 5), CUP, base=3.4)
    m.c('abdomen_a', (-9, 6, 6), (9, 13, 12), CU, pas=5, ton=.2)
    m.c('abdomen_b', (-7, 7, 12), (7, 12, 17), CU, pas=5, ton=.4)
    m.c('dessous', (-8, 5, -11), (8, 6, 15), ('nacre', 'cuivre_sombre'), base=2.4)
    p.bone('head', (0, 9, -12), 'body')
    m.b = 'head'
    m.c('tete', (-8, 5, -21), (8, 13, -11), CUP, base=3)
    m.c('vert_de_gris', (-7, 13, -19), (7, 14, -12), ('uni', 'cuivre_vert'), base=3.4)
    m.yeux('head', 2, 9, -21, 2, 2, ramp='rune', base=3.4)
    m.yeux('head', 5, 10, -21, 1, 1, ramp='rune', base=3.4, nom='oeil2_lueur')
    m.s('mandibule', (1, 5, -25), (3, 8, -21), CS, base=3.6)
    # pinces
    for bone, s in (('claw_g', 1), ('claw_d', -1)):
        p.bone(bone, (s * 8, 8, -14), 'body')
    m.sb('claw_g|claw_d', 'bras', (7, 6, -22), (10, 10, -13), CU, pas=4, ton=.2, rot=(0, -32, 0), pivot=(8, 8, -14))
    m.sb('claw_g|claw_d', 'avantbras', (13, 6, -30), (16, 10, -20), CU, pas=4, ton=.3, rot=(0, 10, 0), pivot=(14, 8, -21))
    m.sb('claw_g|claw_d', 'paume', (10, 5, -37), (18, 11, -29), CUP, base=3.2)
    m.sb('claw_g|claw_d', 'doigt_haut', (11, 8, -46), (16, 11, -37), CUP, base=3.6, rot=(-12, 0, 0), pivot=(13, 10, -37))
    m.sb('claw_g|claw_d', 'doigt_bas', (11, 5, -44), (16, 8, -37), CUP, base=3, rot=(10, 0, 0), pivot=(13, 6, -37))
    # queue : chaine de segments jusqu'au dard
    pts = [(0, 10, 16), (0, 12, 25), (0, 20, 31), (0, 30, 31), (0, 38, 25), (0, 42, 17)]
    lar = [5, 5, 4, 4, 3]

    def cote(m_, os_, i, P, W, H):
        x, y, z = P
        wx, wz = W
        m_.cb(os_, f'bague{i}', (x - wx - 1, y + H - 4, z - wz - 1), (x + wx + 1, y + H - 2, z + wz + 1), ('uni', 'cuivre_vert'), base=3.2)
    ch = m.courbe(pts, lar, CU, prefixe='queue', parent='body', cote=cote, pas=5, ton=.3, axe='y')
    dernier = ch[-1][0]
    p.bone('dard', (0, 42, 17), dernier)
    m.b = 'dard'
    m.c('bulbe', (-4, 36, 9), (4, 44, 19), CUP, base=2.2)
    m.c('bulbe_venin', (-2, 35, 10), (2, 36, 17), ('uni', 'venin'), base=3, hide={'up'})
    m.c('aiguillon', (-2, 30, 6), (2, 37, 10), ('griffe', 'nuit'), base=2.8, rot=(-34, 0, 0), pivot=(0, 37, 9))
    m.c('aiguillon_b', (-1, 24, 6), (1, 31, 8), ('griffe', 'nuit'), base=3.8, rot=(-34, 0, 0), pivot=(0, 37, 9))
    # huit pattes
    legs = []
    for i, (z, fan) in enumerate(((-9, 26), (-4, 8), (1, -8), (5, -26))):
        g, d = f'leg{i}l', f'leg{i}r'
        p.bone(g, (9, 9, z), 'body')
        p.bone(d, (-9, 9, z), 'body')
        L = 10
        xk = round(9 + L * math.cos(math.radians(fan)) * .9)
        zk = round(z - L * math.sin(math.radians(fan)) * .9)
        p.bone(g + '_bas', (xk, 13, zk), g)
        p.bone(d + '_bas', (-xk, 13, zk), d)
        m.sb(f'{g}|{d}', f'hanche{i}', (8, 8, z - 1), (8 + L, 11, z + 1), CU, pas=3, axe='x', ton=.3, rot=(0, fan, 22), pivot=(9, 9, z))
        m.sb(f'{g}_bas|{d}_bas', f'tibia{i}', (xk - 1, 2, zk - 1), (xk + 1, 14, zk + 1), CS, base=3.6, rot=(0, 0, 14), pivot=(xk, 13, zk))
        m.sb(f'{g}_bas|{d}_bas', f'griffe{i}', (xk + 2, 0, zk - 1), (xk + 4, 3, zk + 1), CS, base=2)
        legs += [(g, g + '_bas', 1), (d, d + '_bas', -1)]
    octo_mob(p, legs, head='head', claws=(('claw_g', 1), ('claw_d', -1)), chaine=[c[0] for c in ch], stinger='dard')
    return p


def momie_dunes():
    """Momie des dunes : bandelettes sales sur corps maigre, orbites violettes, echarpe turquoise a franges, lanieres qui pendent, pieds nus et sandales de cuivre."""
    m = M('momie_dunes', 242, 2.690, description='Momie des dunes Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=27, epaule=46, cou=48, x_bras=8, x_jambe=4, coude=35, genou=13)
    m.sb('leg_-1|leg_1', 'cuisse', (2, 13, -3), (7, 28, 3), BA, ton=.1)
    m.sb('tibia_-1|tibia_1', 'tibia', (2, 3, -3), (6, 14, 2), BA, ton=-.2)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -6), (7, 3, 3), BS, ton=.2)
    m.sb('tibia_-1|tibia_1', 'sandale', (1, 1, -5), (7, 2, 2), CS, base=4) if False else None
    m.sb('leg_-1|leg_1', 'laniere_c', (7, 14, -1), (8, 24, 1), FR('bandelette'), base=2.6, mini=.5, pas=1, **CR)
    m.b = 'body'
    m.c('bassin', (-6, 26, -4), (6, 33, 4), BS)
    m.c('taille', (-5, 33, -3), (5, 39, 3), BA, ton=.1)
    m.c('poitrine', (-7, 39, -4), (7, 47, 4), BA, ton=.3)
    m.c('pagne', (-5, 15, -5), (5, 30, -4), FR('tissu_bleu'), base=2.8, mini=.6, **CF) if False else None
    m.c('pagne', (-5, 17, -5), (5, 30, -4), FR('cuivre_vert'), base=3.6, mini=.6, **CF)
    m.c('pagne_dos', (-5, 18, 4), (5, 30, 5), FR('bandelette_sale'), base=2.8, mini=.5, **CF)
    m.c('ceinture', (-7, 29, -5), (7, 32, 5), ('plaque', 'cuivre'), base=3.2, rivets=4)
    # echarpe en travers, pan qui pend
    TQ = ('peau_tissu', 'cuivre_vert')
    m.c('echarpe_col', (-7, 44, -5), (7, 48, 5), TQ, base=4)
    m.c('echarpe_pan', (-5, 27, -6), (-1, 45, -5), TQ, base=4.4, pli=3, hide={'south'})
    m.c('echarpe_bout', (-5, 22, -6), (-1, 28, -5), FR('cuivre_vert'), base=4.2, mini=.5, pas=1, **CF)
    m.c('echarpe_dos', (-4, 26, 4), (2, 46, 5), FR('cuivre_vert'), base=3.6, mini=.7, pas=1, **CF)
    # tete : bandelettes croisees, orbites, un oeil brille plus fort
    m.b = 'head'
    m.c('crane', (-5, 48, -6), (5, 60, 5), BA, ton=.5, pas=3)
    m.c('fente', (-4, 53, -7), (4, 57, -6), ('uni', 'nuit'), base=.5, no_ao=True, hide={'south'})
    m.yeux('head', 1, 54, -8, 3, 2, ramp='amethyste', base=4.4, dz=2)
    m.c('bande_front', (-6, 57, -7), (6, 60, 4), BS, ton=.4, pas=3, rot=(0, 0, -6), pivot=(0, 58, 0))
    m.c('bande_menton', (-5, 47, -7), (5, 51, 3), BS, ton=.2, pas=3)
    m.c('bande_defaite', (3, 45, 3), (6, 56, 4), FR('bandelette'), base=3.2, mini=.6, pas=1, **CF)
    # bras maigres, lanieres
    m.sb('arm_-1|arm_1', 'bras', (6, 34, -2), (10, 47, 2), BA, ton=.2)
    m.sb('avbras_-1|avbras_1', 'avantbras', (6, 23, -2), (10, 35, 2), BA)
    m.sb('avbras_-1|avbras_1', 'main', (6, 19, -3), (10, 24, 2), BS, ton=.6)
    m.sb('avbras_-1|avbras_1', 'laniere_bras', (10, 14, -1), (11, 30, 1), FR('bandelette'), base=2.8, mini=.4, pas=1, **CR)
    bipede(p, style='griffes', amp=20, genou=30, bras=14, jambe=27, posture=-5, haleine=.3, cape=())
    return p


def hyene_cendree():
    """Hyene cendree : garrot haut, croupe basse, grosse tete, pelage de cendre tachete, criniere de pics incandescents le long du dos et des flancs."""
    m = M('hyene_cendree', 243, 3.113, description='Hyene cendree Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (6, 36)
    FU = ('fourrure', 'cendre')
    PK = ('cristal', 'braise')
    p.bone('body', (0, 19, 0), 'root')
    m.b = 'body'
    m.c('poitrail', (-8, 13, -16), (8, 33, -3), FU, ton=.3)
    m.c('garrot', (-6, 33, -15), (6, 36, -5), FU, ton=.6)
    m.c('ventre', (-7, 12, -3), (7, 29, 8), FU)
    m.c('croupe', (-7, 12, 8), (7, 26, 18), FU, ton=-.2)
    m.s('epaule', (8, 16, -15), (9, 30, -5), FU, ton=.5)
    # pics incandescents : grande criniere le long de l'echine
    pics = ((-14, 36, 13, 3, -8), (-9, 36, 15, 3, -2), (-4, 33, 14, 3, 6), (1, 29, 13, 3, 10), (6, 28, 12, 2, 14), (11, 26, 10, 2, 22), (16, 25, 9, 2, 30))
    for i, (z, y, h, r, rx) in enumerate(pics):
        m.pointe('body', 'pic_lueur', 0, y - 1, z, h, r, PK, inc=(rx, 0), base=2.4)
    for i, (z, y, h, r, rx, rz) in enumerate(((-12, 28, 12, 2, -6, -30), (-5, 27, 12, 2, 2, -34), (3, 24, 11, 2, 10, -36), (11, 22, 9, 2, 18, -38))):
        m.pointe('body', 'pic_g_lueur', 7, y, z, h, r, PK, inc=(rx, rz), base=2.2)
        m.pointe('body', 'pic_d_lueur', -7, y, z, h, r, PK, inc=(rx, -rz), base=2.2)
    # tete forte, machoire puissante
    p.bone('head', (0, 27, -15), 'body')
    m.b = 'head'
    m.c('cou', (-6, 20, -22), (6, 33, -12), FU, ton=.5)
    m.c('crane', (-7, 21, -30), (7, 33, -20), FU, ton=.7)
    m.c('museau', (-4, 21, -38), (4, 28, -30), ('uni', 'cendre'), base=1.8)
    m.c('truffe', (-2, 26, -39), (2, 29, -38), ('uni', 'nuit'), base=1, hide={'south'})
    m.yeux('head', 3, 28, -30, 3, 2, ramp='braise', base=4.6)
    m.c('arcade', (-7, 30, -31), (7, 32, -29), FU, ton=-.2)
    p.bone('machoire', (0, 21, -22), 'head')
    m.b = 'machoire'
    m.c('mandibule', (-4, 16, -37), (4, 21, -22), ('uni', 'cendre'), base=1.4)
    m.s('croc_haut', (2, 21, -37), (4, 23, -35), ('griffe', 'ivoire'), base=4)
    p.bone('ear_g', (5, 33, -24), 'head')
    p.bone('ear_d', (-5, 33, -24), 'head')
    OR_ = ['..##..', '.####.', '######', '######', '.####.']
    m.sb('ear_g|ear_d', 'oreille', (2, 32, -25), (8, 39, -24), ('plan', 'cendre'), base=2.6, masque=OR_, rot=(-10, 0, -10), pivot=(5, 33, -24), **CF)
    # queue courte en touffe
    p.bone('tail', (0, 24, 18), 'body')
    m.cb('tail', 'queue', (-2, 13, 18), (2, 25, 21), ('poils_longs', 'cendre'), rot=(-16, 0, 0), pivot=(0, 25, 19))
    m.pointe('tail', 'queue_lueur', 0, 14, 21, 9, 2, PK, inc=(164, 0), base=2.4)
    # pattes : avant longues, arriere courtes et flechies
    legs = []
    for nom, x, z, y, ph, yk in (('fl', 5, -10, 20, 0.0, 10), ('fr', -5, -10, 20, .5, 10), ('bl', 5, 13, 20, .56, 9), ('br', -5, 13, 20, .06, 9)):
        p.bone('leg_' + nom, (x, y, z), 'root')
        p.bone('leg_' + nom + '_bas', (x, yk, z), 'leg_' + nom)
        legs.append(('leg_' + nom, 'leg_' + nom + '_bas', ph))
    m.sb('leg_fl|leg_fr', 'bras', (3, 9, -14), (8, 21, -6), FU, y=(0, 36), ton=-.1, hide={'up'})
    m.sb('leg_fl_bas|leg_fr_bas', 'canon_av', (3, 2, -13), (7, 10, -7), FU, y=(0, 36), ton=-.5)
    m.sb('leg_fl_bas|leg_fr_bas', 'patte_av', (3, 0, -16), (8, 2, -7), ('uni', 'nuit'), base=4)
    m.sb('leg_bl|leg_br', 'cuisse', (3, 8, 9), (8, 21, 17), FU, y=(0, 36), ton=-.1, hide={'up'})
    m.sb('leg_bl_bas|leg_br_bas', 'canon_ar', (3, 2, 11), (7, 9, 16), FU, y=(0, 36), ton=-.5)
    m.sb('leg_bl_bas|leg_br_bas', 'patte_ar', (3, 0, 8), (8, 2, 15), ('uni', 'nuit'), base=4)
    quadrupede_mob(p, legs, jaw='machoire', ears=('ear_g', 'ear_d'), amp=28, knee=44, leg_len=20, poids=1.2, gr=1.5)
    return p


def djinn_braises():
    """Djinn des braises : buste d'or, masque rouge, cornes et bracelets, mains de feu, le bas du corps est une spirale de flammes qui s'effile."""
    m = M('djinn_braises', 244, 2.643, description='Djinn des braises Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    OC = ('plaque', 'or_corne')
    OU = ('uni', 'or_corne')
    RG = ('plaque', 'rouge')
    FL = ('flamme', 'flamme')
    p.bone('body', (0, 42, 0), 'root')
    p.bone('head', (0, 57, -1), 'body')
    p.bone('arm_-1', (12, 53, 0), 'body')
    p.bone('arm_1', (-12, 53, 0), 'body')
    p.bone('avbras_-1', (17, 42, 0), 'arm_-1')
    p.bone('avbras_1', (-17, 42, 0), 'arm_1')
    p.bone('queue1', (0, 40, 0), 'body')
    p.bone('queue2', (1, 29, 1), 'queue1')
    p.bone('queue3', (-1, 19, 2), 'queue2')
    # buste nu dore, ceinture de chaines
    m.b = 'body'
    m.c('torse', (-11, 46, -6), (11, 58, 6), OU, base=3.4)
    m.c('pectoraux', (-9, 48, -7), (9, 57, -6), OU, base=3.8, hide={'south'})
    m.c('ventre', (-8, 38, -5), (8, 46, 5), OU, base=3)
    m.c('ceinture', (-9, 37, -6), (9, 41, 6), RG, base=3)
    m.c('boucle', (-3, 36, -7), (3, 42, -6), OC, base=3.4, hide={'south'})
    m.lueur('body', 'gemme', (-2, 50, -8), (2, 54, -7), ('cristal', 'braise'), base=3, hide={'south'})
    # queue de flammes : trois volumes qui s'effilent, plans de flamme croises
    m.lueur('queue1', 'braise_a', (-9, 28, -6), (9, 39, 6), ('flamme_pleine', 'flamme'), base=3.4)
    m.lueur('queue1', 'flamme_a', (-13, 22, 0), (13, 40, 1), FL, **CF)
    m.lueur('queue1', 'flamme_a2', (0, 22, -11), (1, 40, 11), FL, **CR)
    m.lueur('queue2', 'braise_b', (-6, 18, -4), (8, 30, 6), ('flamme_pleine', 'flamme'), base=3.2, rot=(0, 18, 0), pivot=(1, 24, 1))
    m.lueur('queue2', 'flamme_b', (-9, 13, 1), (11, 31, 2), FL, **CF)
    m.lueur('queue2', 'flamme_b2', (1, 13, -8), (2, 31, 10), FL, **CR)
    m.lueur('queue3', 'braise_c', (-4, 11, -1), (3, 20, 5), ('flamme_pleine', 'flamme'), base=3, rot=(0, 36, 0), pivot=(-1, 15, 2))
    m.lueur('queue3', 'flamme_c', (-7, 6, 2), (6, 20, 3), FL, **CF)
    m.lueur('queue3', 'flamme_c2', (-1, 6, -3), (0, 20, 8), FL, **CR)
    # tete : masque rouge, yeux, machoire d'or, cornes, flamme sur le crane
    m.b = 'head'
    m.c('crane', (-7, 57, -8), (7, 70, 5), RG, base=2.6)
    m.c('masque_front', (-7, 66, -9), (7, 70, -8), OC, base=3.4, hide={'south'})
    m.c('joues', (-8, 57, -9), (8, 62, -2), OC, base=2.8)
    m.yeux('head', 2, 63, -9, 3, 2, ramp='flamme', base=5)
    m.c('bouche', (-4, 58, -10), (4, 60, -9), ('uni', 'nuit'), base=.6, hide={'south'})
    for sg, n in ((1, 'g'), (-1, 'd')):
        a0, a1 = sorted((sg * 5, sg * 9))
        m.c(f'corne_{n}', (a0, 69, -3), (a1, 78, 1), ('corne', 'or_corne'), rot=(-8, 0, -sg * 18), pivot=((a0 + a1) / 2, 69, -1))
        m.c(f'corne2_{n}', (a0 + 1, 77, -2), (a1 - 1, 84, 0), ('corne', 'or_corne'), base=3.4, rot=(-8, 0, -sg * 18), pivot=((a0 + a1) / 2, 69, -1))
    m.lueur('head', 'flamme_tete', (-5, 70, 0), (5, 84, 1), FL, **CF)
    # bras musculeux, bracelets, mains ouvertes, boules de feu
    m.sb('arm_-1|arm_1', 'epaule', (10, 49, -5), (20, 59, 5), OU, base=3.8)
    m.sb('arm_-1|arm_1', 'epauliere', (10, 56, -6), (21, 60, 6), RG, base=3.2)
    m.sb('arm_-1|arm_1', 'bras', (12, 40, -4), (19, 50, 4), OU, base=3.2)
    m.sb('avbras_-1|avbras_1', 'avantbras', (13, 30, -3), (20, 42, 4), OU, base=3.4)
    m.sb('avbras_-1|avbras_1', 'bracelet', (12, 31, -4), (21, 36, 5), OC, base=3.4, rivets=3)
    m.sb('avbras_-1|avbras_1', 'main', (14, 25, -3), (19, 31, 4), OU, base=3.6)
    m.lueur_sym('avbras_-1|avbras_1', 'feu_main', (12, 12, 0), (21, 28, 1), FL, **CF)
    m.lueur_sym('avbras_-1|avbras_1', 'feu_main2', (16, 12, -5), (17, 28, 5), FL, **CR)
    bipede(p, style='griffes', marche='fly', amp=0, genou=0, bras=10, jambe=0, flotte=1.8, gr=1.2, queue=('queue1', 'queue2', 'queue3'))
    return p


def tortue_volcanique():
    """Tortue volcanique : carapace de basalte en gradins, plaques fendues de lave, cratere fumant au sommet, pattes-piliers, tete basse a bec."""
    m = M('tortue_volcanique', 245, 2.765, description='Tortue volcanique Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    B = ('lave', 'basalte')
    PN = ('plaque', 'pierre_noire')
    PE = ('peau_plissee', 'cendre')
    p.bone('body', (0, 16, 0), 'root')
    m.b = 'body'
    m.c('plastron', (-19, 8, -22), (19, 12, 20), PN, base=2.2)
    m.c('jupe', (-22, 11, -24), (22, 19, 21), B, base=1.2, large=.02)
    m.c('dome', (-19, 19, -21), (19, 29, 18), B, base=1.4, large=.02)
    m.c('dome_h', (-14, 29, -16), (14, 36, 13), B, base=1.6, large=.02)
    m.c('sommet', (-8, 36, -10), (8, 40, 7), B, base=1.8, large=.02)
    # plaques de lave en saillie : fissures qui brillent
    LV = ('lave', 'basalte')
    for i, (x0, x1, y0, y1, z0, z1) in enumerate(((22, 23, 12, 18, -19, -7), (22, 23, 13, 18, -4, 8), (22, 23, 12, 17, 11, 18), (19, 20, 21, 28, -15, -3), (19, 20, 21, 27, 2, 13), (14, 15, 30, 35, -11, 8))):
        m.lueur_sym('body', f'lave{i}', (x0, y0, z0), (x1, y1, z1), LV, large=.09, hide={'west'})
    for i, (x0, x1, y0, y1, z) in enumerate(((-16, -6, 12, 18, -25), (-4, 4, 12, 18, -25), (6, 16, 12, 18, -25), (-12, -3, 21, 28, -22), (3, 12, 21, 28, -22), (-8, 8, 30, 35, -17))):
        m.lueur('body', f'lave_av{i}', (x0, y0, z), (x1, y1, z + 1), LV, large=.09, hide={'south'})
    for i, (x0, x1, y0, y1, z) in enumerate(((-16, -3, 12, 18, 21), (3, 16, 12, 18, 21), (-10, 10, 21, 28, 18))):
        m.lueur('body', f'lave_ar{i}', (x0, y0, z), (x1, y1, z + 1), LV, large=.09, hide={'north'})
    # cratere : bord de roche, lave dedans, deux pierres
    m.c('bord_av', (-8, 40, -10), (8, 43, -6), PN, base=2.6)
    m.c('bord_ar', (-8, 40, 3), (8, 43, 7), PN, base=2.6)
    m.s('bord_c', (5, 40, -6), (8, 43, 3), PN, base=2.8)
    m.lueur('body', 'lave_cratere', (-5, 40, -6), (5, 41, 3), ('uni', 'braise'), base=4.6, hide={'down'})
    m.c('pierre', (-3, 41, -3), (1, 44, 1), PN, base=3.2, rot=(10, 20, 8))
    # tete
    p.bone('head', (0, 14, -22), 'body')
    m.b = 'head'
    m.c('cou', (-8, 7, -33), (8, 20, -20), PE, y=(0, 30))
    m.c('crane', (-8, 8, -44), (8, 22, -31), PE, ton=.5, y=(0, 30))
    m.c('front', (-7, 22, -42), (7, 24, -32), PN, base=2.8)
    m.c('bec', (-5, 9, -48), (5, 15, -44), ('griffe', 'nuit'), base=3.6)
    m.yeux('head', 4, 15, -45, 3, 3, ramp='braise', base=4.8)
    p.bone('machoire', (0, 9, -33), 'head')
    m.cb('machoire', 'mandibule', (-6, 3, -46), (6, 9, -32), PE, ton=-.2, y=(0, 30))
    # queue courte
    p.bone('tail', (0, 13, 20), 'body')
    m.cb('tail', 'queue', (-5, 7, 20), (5, 14, 28), PE, rot=(-18, 0, 0), pivot=(0, 13, 21), y=(0, 30))
    # pattes-piliers
    legs = []
    for nom, x, z, ph in (('fl', 18, -15, 0.0), ('fr', -18, -15, .5), ('bl', 18, 13, .5), ('br', -18, 13, 0.0)):
        p.bone('leg_' + nom, (x, 14, z), 'root')
        p.bone('leg_' + nom + '_bas', (x, 7, z), 'leg_' + nom)
        legs.append(('leg_' + nom, 'leg_' + nom + '_bas', ph))
    for av, z in (('f', -15), ('b', 13)):
        m.sb(f'leg_{av}l|leg_{av}r', f'cuisse_{av}', (13, 6, z - 7), (24, 15, z + 7), PE, y=(0, 30))
        m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'pilier_{av}', (13, 0, z - 8), (25, 7, z + 8), PE, y=(0, 30), ton=-.2)
        m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'ongles_{av}', (14, 0, z - 10), (24, 3, z - 8), ('griffe', 'nuit'), base=3.2)
    quadrupede_mob(p, legs, jaw='machoire', amp=16, knee=20, leg_len=14, poids=2.2, gr=1.4)
    return p


DESERT = {'scorpion_cuivre': scorpion_cuivre, 'momie_dunes': momie_dunes, 'hyene_cendree': hyene_cendree, 'djinn_braises': djinn_braises, 'tortue_volcanique': tortue_volcanique}
