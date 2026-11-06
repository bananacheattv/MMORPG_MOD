#!/usr/bin/env python3
"""Neant astral : mite astrale, rodeur du vide, oeil du chaos, chevalier fracture, devoreur d'etoiles. En texels de jeu."""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import combine, cos, keys, pulse, rot, sin
from clips_mob import _fin, _r, P_ATT, bipede, ev, quadrupede_mob, serpent, volant
from kit_mob import CF, CR, FR, M, RE, losange, rond, triangle
from materiaux import stamp

AM = ('cristal', 'amethyste')
NO = ('plaque', 'nuit')
FNO = ('plaque', 'fer_noir')
K = {'k': '#0b0a0f', 'c': '#8ff5e6', 'C': '#e4fff9', 'r': '#fcc8ea', 'R': '#ef7cc6', 'p': '#621248', 'n': '#1a0b18'}


def mite_astrale():
    """Mite astrale : grandes ailes violettes translucides ouvertes de face, taches d'etoiles, corps fin et sombre, antennes plumeuses, yeux turquoise."""
    m = M('mite_astrale', 251, 2.583, description='Mite astrale Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.bone('body', (0, 34, 0), 'root')
    m.b = 'body'
    m.c('thorax', (-3, 30, -3), (3, 42, 3), NO, base=3.4)
    m.c('poils', (-4, 36, -3), (4, 41, 4), ('poils_longs', 'pourpre'), base=3)
    p.bone('abdomen', (0, 30, 0), 'body')
    m.cb('abdomen', 'abdo_a', (-3, 21, -2), (3, 31, 3), ('segments', 'nuit'), pas=3, axe='y', base=3.4)
    m.cb('abdomen', 'abdo_b', (-2, 14, -1), (2, 22, 2), ('segments', 'pourpre'), pas=3, axe='y', base=3)
    p.bone('head', (0, 42, -1), 'body')
    m.b = 'head'
    m.c('crane', (-3, 41, -5), (3, 47, 1), NO, base=3.6)
    m.lueur_sym('head', 'oeil', (3, 42, -5), (4, 46, -1), ('uni', 'rune'), base=4, hide={'west'})
    m.yeux('head', 1, 43, -5, 2, 2, ramp='rune', base=4, nom='oeil_f_lueur')
    # antennes plumeuses
    AN = ['.#.', '###', '.#.', '###', '.#.', '###', '.#.', '###', '.#.']
    m.s('antenne', (1, 46, -3), (5, 58, -2), ('plan', 'amethyste'), base=4.6, masque=AN, rot=(-14, 0, -18), pivot=(2, 46, -2), **CF)
    # ailes : plan face camera, un os par paire, masque d'aile de papillon
    H = ['................#######', '............###########', '.........##############', '......#################', '...####################', '.######################', '#######################', '#######################', '.######################', '..#####################', '...###################.', '.....################..', '.......#############...', '.........#########.....', '............####.......']
    B = ['################', '###############.', '###############.', '##############..', '#############...', '############....', '###########.....', '##########......', '#########.......', '.#######........', '..#####.........', '...###..........', '....#...........']
    p.bone('aile_g', (3, 40, 1), 'body')
    p.bone('aile_d', (-3, 40, 1), 'body')
    p.bone('aile_b_g', (3, 33, 1), 'body')
    p.bone('aile_b_d', (-3, 33, 1), 'body')
    AI = dict(hide={'east', 'west', 'up', 'down'}, no_shadow=True)
    m.sb('aile_g|aile_d', 'aile_haut', (3, 34, 1), (34, 64, 2), ('aile_insecte', 'amethyste'), masque=H, base=3.2, alpha=190, nervure=1.8, **AI)
    m.sb('aile_b_g|aile_b_d', 'aile_bas', (3, 7, 1), (25, 34, 2), ('aile_insecte', 'amethyste'), masque=B, base=3, alpha=190, nervure=1.8, **AI)

    def etoiles(ctx, face):
        rng = ctx.rng
        H_, W_ = face.shape[:2]
        for _ in range(max(6, H_ * W_ // 60)):
            x, y = int(rng.integers(0, W_)), int(rng.integers(0, H_))
            if face[y, x, 3] > 0:
                face[y, x, :3] = (250, 232, 255)
                face[y, x, 3] = 255
    p.decal('aile_haut_g', 'north', etoiles)
    p.decal('aile_haut_g', 'south', etoiles)
    p.decal('aile_bas_g', 'north', etoiles)
    p.decal('aile_bas_g', 'south', etoiles)
    pattes = []
    for i, (y, r) in enumerate(((39, 6), (35, 0), (31, -6))):
        g, d = f'patte{i}_g', f'patte{i}_d'
        p.bone(g, (3, y, -2), 'body')
        p.bone(d, (-3, y, -2), 'body')
        pattes += [g, d]
        m.sb(f'{g}|{d}', f'patte{i}', (2, y - 9, -3), (4, y, -1), ('griffe', 'nuit'), base=3, rot=(26, 0, -24), pivot=(3, y, -2))
    volant(p, [('aile_g', 'aile_d', 0.0), ('aile_b_g', 'aile_b_d', .1)], queue=('abdomen',), pattes=pattes, battement=26, cadence=2, hover=1.6, battement_idle=12, axe='y')
    return p


def rodeur_vide():
    """Rodeur du vide : felin long et bas, pelage de nuit a reflets violets, echine de grands cristaux d'amethyste, yeux turquoise, griffes, longue queue."""
    m = M('rodeur_vide', 252, 3.077, description='Rodeur du vide Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (6, 30)
    FU = ('fourrure', 'chitine_sombre')
    p.bone('body', (0, 19, 0), 'root')
    m.b = 'body'
    m.c('poitrail', (-7, 13, -19), (7, 28, -6), FU, ton=.3)
    m.c('taille', (-5, 15, -6), (5, 26, 8), FU, ton=-.1)
    m.c('croupe', (-7, 14, 8), (7, 28, 19), FU, ton=.2)
    m.s('epaule', (7, 16, -18), (8, 27, -8), FU, ton=.7)
    m.s('hanche', (7, 16, 9), (8, 26, 18), FU, ton=.7)
    for i, (z, y, h, r, rx) in enumerate(((-15, 27, 13, 3, -8), (-9, 27, 18, 4, -2), (-3, 25, 15, 3, 6), (3, 25, 20, 4, 10), (9, 27, 16, 3, 14), (15, 27, 11, 2, 22))):
        m.pointe('body', 'cristal_lueur', 0, y, z, h, r, AM, inc=(rx, 0), base=1.2)
    for i, (z, y, h, rx, rz) in enumerate(((-11, 25, 9, -4, -28), (0, 23, 10, 6, -30), (11, 25, 8, 14, -28))):
        m.pointe('body', 'cristal_g', 5, y, z, h, 2, AM, inc=(rx, rz), base=1.2)
        m.pointe('body', 'cristal_d', -5, y, z, h, 2, AM, inc=(rx, -rz), base=1.2)
    p.bone('head', (0, 23, -18), 'body')
    m.b = 'head'
    m.c('cou', (-5, 17, -25), (5, 29, -15), FU, ton=.6)
    m.c('crane', (-7, 18, -33), (7, 30, -23), FU, ton=.8)
    m.c('museau', (-4, 18, -39), (4, 24, -33), ('uni', 'chitine_sombre'), base=2.4)
    m.c('truffe', (-1, 22, -40), (1, 24, -39), ('uni', 'amethyste'), base=3.6, hide={'south'})
    m.yeux('head', 3, 25, -33, 3, 2, ramp='rune', base=3.8)
    p.decal('oeil_lueur_g', 'north', stamp(['ccC', 'ckk'], K, 6, 27))
    p.bone('machoire', (0, 18, -25), 'head')
    m.cb('machoire', 'mandibule', (-4, 14, -38), (4, 18, -25), ('uni', 'chitine_sombre'), base=1.6)
    m.sb('machoire', 'croc', (2, 18, -38), (3, 20, -37), ('griffe', 'ivoire'), base=4)
    p.bone('ear_g', (4, 30, -26), 'head')
    p.bone('ear_d', (-4, 30, -26), 'head')
    OR_ = ['....#', '...##', '..###', '.####', '#####', '#####']
    m.sb('ear_g|ear_d', 'oreille', (2, 29, -27), (7, 37, -26), ('plan', 'chitine_sombre'), base=3.4, masque=OR_, rot=(-6, 0, -8), pivot=(4, 30, -26), **CF)
    # queue longue en trois os, cristal au bout
    p.bone('tail', (0, 24, 19), 'body')
    p.bone('tail2', (0, 21, 29), 'tail')
    p.bone('tail3', (0, 19, 38), 'tail2')
    m.cb('tail', 'queue_a', (-2, 19, 18), (2, 25, 31), FU, ton=-.1, rot=(-14, 0, 0), pivot=(0, 24, 19))
    m.cb('tail2', 'queue_b', (-2, 19, 28), (2, 23, 39), FU, ton=-.1, rot=(10, 0, 0), pivot=(0, 21, 29))
    m.cb('tail3', 'queue_c', (-1, 17, 37), (1, 21, 47), FU, rot=(-38, 0, 0), pivot=(0, 19, 38))
    m.pointe('tail3', 'queue_lueur', 0, 23, 45, 10, 2, AM, inc=(38, 0), base=1.6)
    # pattes hautes et griffues
    legs = []
    for nom, x, z, ph, yk in (('fl', 5, -12, 0.0, 10), ('fr', -5, -12, .5, 10), ('bl', 5, 13, .56, 11), ('br', -5, 13, .06, 11)):
        p.bone('leg_' + nom, (x, 19, z), 'root')
        p.bone('leg_' + nom + '_bas', (x, yk, z), 'leg_' + nom)
        legs.append(('leg_' + nom, 'leg_' + nom + '_bas', ph))
    for av, z0, z1 in (('f', -16, -8), ('b', 9, 17)):
        m.sb(f'leg_{av}l|leg_{av}r', f'cuisse_{av}', (3, 9, z0), (8, 21, z1), FU, y=(0, 30), hide={'up'})
        m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'canon_{av}', (3, 2, z0 + 1), (7, 11, z1 - 2), FU, y=(0, 30), ton=-.5)
        m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'patte_{av}', (3, 0, z0 - 2), (8, 3, z1 - 1), FU, y=(0, 30), ton=-.8)
        m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'griffes_{av}', (3, 0, z0 - 4), (8, 1, z0 - 2), ('griffe', 'amethyste'), base=4)
    quadrupede_mob(p, legs, jaw='machoire', ears=('ear_g', 'ear_d'), tail='tail', tail_tip='tail3', amp=30, knee=52, leg_len=19, poids=1, cou=('tail2',), gr=1.6)
    return p


def oeil_chaos():
    """Oeil du chaos : masse flottante de cristaux violets autour d'un grand oeil rose a pupille verticale, paupieres de roche, eclats en orbite."""
    m = M('oeil_chaos', 253, 2.714, description='Oeil du chaos Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.bone('body', (0, 35, 0), 'root')
    p.bone('head', (0, 35, -3), 'body')
    p.bone('paupiere_h', (0, 50, -6), 'head')
    p.bone('paupiere_b', (0, 20, -6), 'head')
    p.bone('orbite_a', (0, 35, 0), 'body')
    p.bone('orbite_b', (0, 35, 0), 'body')
    RO = ('plaque', 'pourpre')
    # masse : blocs sombres et cristaux, irreguliere
    m.b = 'body'
    m.c('masse', (-17, 19, -6), (17, 51, 8), ('cristal', 'pourpre'), base=1.8)
    m.c('masse_h', (-12, 51, -4), (12, 58, 7), ('cristal', 'pourpre'), base=2.2)
    m.c('masse_b', (-11, 13, -4), (11, 19, 6), ('cristal', 'pourpre'), base=1.4)
    m.s('masse_c', (17, 24, -4), (23, 46, 7), ('cristal', 'pourpre'), base=1.8)
    for i, (x, y, z, h, r, rx, rz) in enumerate(((16, 49, 0, 12, 3, -6, -30), (-16, 48, 0, 14, 3, -8, 32), (6, 57, 1, 11, 2, 0, -10), (-5, 57, 0, 13, 3, 4, 8), (22, 34, 2, 11, 3, 2, -68), (-22, 34, 2, 12, 3, 2, 68), (14, 18, 1, 11, 3, 4, -128), (-13, 18, 1, 12, 3, 4, 128), (0, 15, 1, 10, 3, 172, 0))):
        m.pointe('body', 'cristal', x, y, z, h, r, AM, inc=(rx, rz), base=1.4)
    # oeil : blanc rose, iris, pupille verticale fendue
    m.b = 'head'
    m.lueur('head', 'blanc', (-11, 23, -9), (11, 47, -5), ('uni', 'rose_chaos'), base=3.4, hide={'south'})
    m.lueur('head', 'iris', (-7, 25, -10), (7, 45, -9), ('uni', 'rose_chaos'), base=2.4, hide={'south'})
    m.lueur('head', 'pupille', (-2, 26, -11), (2, 44, -10), ('uni', 'soie_noire'), base=.4, hide={'south'})
    p.decal('iris_lueur', 'north', stamp(['....rr....', '...rRRr...', '..rRRRRr..', '.rRRRRRRr.'], K, 5, 45))
    # paupieres de roche, elles clignent
    for sx, sy in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        x0, x1 = sorted((sx * 11, sx * 6))
        y0, y1 = sorted((35 + sy * 12, 35 + sy * 6))
        m.c('coin', (x0, y0, -11), (x1, y1, -9), RO, base=2.4, hide={'south'})
        x0, x1 = sorted((sx * 11, sx * 8))
        y0, y1 = sorted((35 + sy * 6, 35 + sy * 3))
        m.c('coin_b', (x0, y0, -11), (x1, y1, -9), RO, base=2.2, hide={'south'})
    m.c('rebord_g', (11, 22, -9), (14, 48, -5), RO, base=2.6)
    m.c('rebord_d', (-14, 22, -9), (-11, 48, -5), RO, base=2.6)
    m.cb('paupiere_h', 'paupiere_haut', (-14, 46, -10), (14, 52, -5), RO, base=3, bord=1, rivets=5)
    m.cb('paupiere_b', 'paupiere_bas', (-14, 18, -10), (14, 24, -5), RO, base=2.4, bord=1)
    # eclats en orbite
    for i, a in enumerate((20, 140, 260)):
        x, y = round(29 * math.cos(math.radians(a))), round(35 + 28 * math.sin(math.radians(a)))
        m.pointe('orbite_a', 'eclat_lueur', x, y - 4, 2, 9, 2, AM, inc=(0, 0), base=1.8)
    for i, a in enumerate((80, 200, 320)):
        x, y = round(25 * math.cos(math.radians(a))), round(35 + 30 * math.sin(math.radians(a)))
        m.pointe('orbite_b', 'eclat2_lueur', x, y - 3, 4, 7, 1, AM, inc=(0, 0), base=2)
    # clips : flotte, regarde, cligne ; orbites autour de l'axe z
    T = 3.0
    tr = {'body': {'position': rot(T, y=sin(1.8, 1, .2)), 'rotation': rot(T, y=sin(5, 1, 0), z=sin(2, 1, .3))},
          'head': {'rotation': rot(T, y=sin(10, 1, .1), x=sin(5, 2, .2))},
          'paupiere_h': {'rotation': rot(T, x=pulse(-22, .62, .05), n=24), 'position': rot(T, y=pulse(-6, .62, .05), n=24)},
          'paupiere_b': {'position': rot(T, y=pulse(4, .62, .05), n=24)},
          'orbite_a': {'rotation': keys(T, lambda q: (0, 0, 360 * q), 16)},
          'orbite_b': {'rotation': keys(T, lambda q: (0, 0, -360 * q), 16)}}
    _fin(p, 'idle', T, tr)
    T = 1.5
    tr = {'body': {'position': rot(T, y=sin(2.2, 1, .2)), 'rotation': rot(T, x=sin(3, 1, 0, bias=-8), z=sin(3, 1, .25))},
          'head': {'rotation': rot(T, y=sin(4, 1, .1))},
          'orbite_a': {'rotation': keys(T, lambda q: (0, 0, 360 * q), 16)},
          'orbite_b': {'rotation': keys(T, lambda q: (0, 0, -360 * q), 16)}}
    _fin(p, 'fly', T, tr)
    T = 1.2
    PH = P_ATT
    tr = {'body': {'rotation': ev(T, PH, x=(0, 12, -18, -14, -4, 0)), 'position': ev(T, PH, y=(0, 2, -1, -1, 0, 0), z=(0, 3, -6, -5, -1, 0))},
          'paupiere_h': {'position': ev(T, PH, y=(0, -4, 3, 3, 1, 0))},
          'paupiere_b': {'position': ev(T, PH, y=(0, 3, -3, -3, -1, 0))},
          'orbite_a': {'rotation': ev(T, PH, z=(0, 90, 300, 330, 350, 360))},
          'orbite_b': {'rotation': ev(T, PH, z=(0, -90, -300, -330, -350, -360))}}
    _fin(p, 'attack', T, tr, False)
    T = 1.0
    PH = (0, .4, 1)
    tr = {'body': _r(T, PH, x=(0, -14, -26)), 'paupiere_h': {'position': ev(T, PH, y=(0, -5, -8))}, 'paupiere_b': {'position': ev(T, PH, y=(0, 4, 6))}}
    _fin(p, 'death', T, tr, False)
    return p


def chevalier_fracture():
    """Chevalier fracture : armure noire fendue, eclats violets qui sortent de la plaque et flottent autour de lui, heaume etroit a fente lumineuse, grande lame d'amethyste."""
    m = M('chevalier_fracture', 254, 2.404, description='Chevalier fracture Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=30, epaule=50, cou=53, x_bras=12, x_jambe=5, coude=38, genou=15)
    # jambes
    m.sb('leg_-1|leg_1', 'cuissot', (2, 15, -4), (8, 31, 4), FNO, base=2.6)
    m.sb('leg_-1|leg_1', 'fente_c', (8, 18, -1), (9, 28, 1), ('uni', 'amethyste'), base=4, hide={'west'})
    m.sb('tibia_-1|tibia_1', 'greve', (2, 3, -4), (8, 16, 3), FNO, base=2.4)
    m.sb('tibia_-1|tibia_1', 'genouillere', (2, 12, -5), (8, 18, -3), FNO, base=3.2)
    m.sb('tibia_-1|tibia_1', 'soleret', (1, 0, -7), (9, 4, 4), FNO, base=2.6)
    m.pointe('tibia_-1', 'eclat_genou', 5, 16, -5, 7, 1, AM, inc=(-30, 0), base=1.4)
    m.pointe('tibia_1', 'eclat_genou', -5, 16, -5, 7, 1, AM, inc=(-30, 0), base=1.4)
    # tronc : bassin, jupe de lames, plastron fendu
    m.b = 'body'
    m.c('bassin', (-8, 27, -5), (8, 35, 5), FNO, base=2.2)
    m.c('tasse_av', (-8, 19, -6), (8, 30, -5), FNO, base=2.6, hide={'south'})
    m.c('tasse_ar', (-8, 19, 5), (8, 30, 6), FNO, base=2.2, hide={'north'})
    m.c('plastron', (-10, 35, -6), (10, 53, 6), FNO, base=2.6, rivets=4)
    m.lueur('body', 'fissure', (-1, 37, -7), (2, 50, -6), ('uni', 'amethyste'), base=4.4, hide={'south'})
    m.lueur('body', 'fissure_b', (-4, 42, -7), (-1, 44, -6), ('uni', 'amethyste'), base=4, hide={'south'})
    m.lueur('body', 'fissure_c', (2, 46, -7), (6, 48, -6), ('uni', 'amethyste'), base=4, hide={'south'})
    m.pointe('body', 'eclat_torse', 6, 40, -6, 9, 2, AM, inc=(-55, -12), base=1.4)
    m.pointe('body', 'eclat_torse', -6, 46, -6, 8, 2, AM, inc=(-48, 16), base=1.4)
    m.c('cape_dos', (-9, 22, 6), (9, 51, 7), FR('nuit'), base=2.4, mini=.6, pas=3, **CF)
    # heaume etroit
    m.b = 'head'
    m.c('heaume', (-6, 53, -7), (6, 67, 6), FNO, base=2.8)
    m.c('visiere', (-5, 58, -8), (5, 62, -7), ('uni', 'nuit'), base=.5, hide={'south'})
    m.lueur('head', 'fente', (-4, 59, -9), (4, 61, -8), ('uni', 'amethyste'), base=4.6, hide={'south'})
    m.c('nasal', (-1, 54, -8), (1, 67, -7), FNO, base=3.6, hide={'south'})
    for sg, n in ((1, 'g'), (-1, 'd')):
        m.pointe('head', f'corne_{n}', sg * 5, 65, 0, 12, 2, AM, inc=(-10, -sg * 18), base=1.4)
    # bras, spalieres fendues
    m.sb('arm_-1|arm_1', 'bras', (10, 38, -4), (15, 50, 4), FNO, base=2.2)
    m.sb('arm_-1|arm_1', 'spaliere', (9, 47, -6), (18, 55, 6), FNO, base=3, rivets=3)
    m.pointe('arm_-1', 'eclat_ep', 15, 54, 0, 12, 2, AM, inc=(0, -20), base=1.4)
    m.pointe('arm_1', 'eclat_ep', -15, 54, 0, 12, 2, AM, inc=(0, 20), base=1.4)
    m.pointe('arm_-1', 'eclat_ep', 12, 54, 3, 9, 1, AM, inc=(14, -8), base=1.4)
    m.pointe('arm_1', 'eclat_ep', -12, 54, 3, 9, 1, AM, inc=(14, 8), base=1.4)
    m.sb('avbras_-1|avbras_1', 'avantbras', (10, 26, -4), (15, 38, 4), FNO, base=2.4)
    m.sb('avbras_-1|avbras_1', 'gantelet', (9, 21, -5), (16, 28, 5), FNO, base=3)
    # lame d'amethyste en main droite
    p.bone('epee', (12, 24, -6), 'avbras_-1', rot=(-30, 0, 0))
    m.cb('epee', 'poignee', (11, 14, -7), (13, 29, -5), ('cuir', 'cuir'), couture=False)
    m.cb('epee', 'pommeau', (10, 11, -8), (14, 14, -4), FNO, base=3.2)
    m.cb('epee', 'garde', (6, 29, -8), (18, 32, -4), FNO, base=3.4)
    m.lueur('epee', 'lame', (9, 32, -7), (15, 62, -5), AM, base=1.6)
    m.lueur('epee', 'pointe', (10, 62, -7), (14, 69, -5), AM, base=2.2)
    # eclats flottants, os propres qui tournent lentement
    p.bone('eclats', (0, 42, 0), 'body')
    for i, (x, y, z, h) in enumerate(((22, 48, 3, 8), (-22, 44, 4, 9), (19, 30, -6, 6), (-18, 32, 6, 7), (0, 60, 12, 7))):
        m.pointe('eclats', 'flotte_lueur', x, y, z, h, 1, AM, inc=(0, 0), base=1.8)
    bipede(p, style='epee', marche='fly', amp=22, genou=32, bras=12, jambe=30, arme='epee', gr=1.4)
    for a in p.anims:
        if a['name'] in ('idle', 'fly'):
            T = a['length']
            a['tracks']['eclats'] = {'rotation': keys(T, lambda q: (0, 360 * q, 0), 16), 'position': rot(T, y=sin(1.5, 2, .1))}
    return p


def devoreur_etoiles():
    """Devoreur d'etoiles : immense serpent noir enroule en spirale verticale, anneaux constelles, pics violets sur le bord exterieur, gueule beante a dents, yeux violets."""
    m = M('devoreur_etoiles', 255, 6.163, description="Devoreur d'etoiles Aeloria : refonte (16 texels par bloc en jeu).")
    p = m.p
    p.meta['y_corps'] = (0, 240)
    ET = ('etoile', 'vide')
    # spirale dans le plan x-y, de la queue (centre) vers la tete (exterieur haut)
    cx, cy = 0, 113
    pts, lar = [], []
    n = 17
    for i in range(n + 1):
        t = i / n
        a = math.radians(215 - t * 520)
        r = 34 + 74 * t
        pts.append((round(cx + r * math.cos(a)), round(cy + r * math.sin(a)), round(14 - 18 * t)))
        lar.append(round(7 + 13 * t ** .8))

    def cote(m_, os_, i, P, W, H):
        x, y, z = P
        wx, wz = W
        # le bord exterieur du segment est du cote -x local (la spirale tourne dans le sens horaire vue de face)
        if i % 1 == 0:
            m_.pointe(os_, f'pic{i}', x - wx, y + H // 2, z, 10 + wx, max(2, wx // 4), AM, inc=(0, 70), tourne=0, base=2.2)
        m_.cb(os_, f'ecailles_av{i}', (x - wx + 3, y, z - wz - 1), (x + wx - 3, y + H - 2, z - wz), ('plaque', 'nuit'), base=2.6, hide={'south'})
        if i % 3 == 1:
            m_.lueur(os_, f'rune{i}', (x - 2, y + H // 2 - 2, z - wz - 2), (x + 2, y + H // 2 + 2, z - wz - 1), ('uni', 'rune'), base=3.6, hide={'south'})
    ch = m.courbe(pts, lar, ET, prefixe='seg', parent='root', section=(1, 1), cote=cote, base=2.4)
    x, y, z = pts[-1]
    # orientation de la tete : suit le dernier segment, puis regarde vers l'avant
    p.bone('head', (x, y, z), ch[-1][0])
    m.b = 'head'
    m.c('crane', (x - 26, y - 8, z - 30), (x + 26, y + 22, z + 16), ET, base=2.4, ton=.6)
    m.c('museau', (x - 20, y + 4, z - 56), (x + 20, y + 20, z - 30), ET, base=2.4, ton=.8)
    m.c('front', (x - 22, y + 21, z - 26), (x + 22, y + 26, z + 8), ('plaque', 'nuit'), base=3)
    m.c('gueule_h', (x - 18, y + 3, z - 55), (x + 18, y + 4, z - 20), ('uni', 'rose_chaos'), base=1.6, hide={'up'})
    for i in range(7):
        xd = x - 17 + i * 5
        m.c(f'dent_h{i}', (xd, y - 4 - (i % 2) * 2, z - 56), (xd + 3, y + 4, z - 53), ('griffe', 'ivoire'), base=4.2)
    for sg, n_ in ((1, 'g'), (-1, 'd')):
        m.c(f'croc_{n_}', (x + sg * 18 - 2, y - 12, z - 54), (x + sg * 18 + 2, y + 4, z - 49), ('griffe', 'ivoire'), base=4.4)
        m.lueur('head', f'oeil_{n_}', (x + (26 if sg > 0 else -27), y + 8, z - 26), (x + (27 if sg > 0 else -26), y + 16, z - 12), ('uni', 'amethyste'), base=4.6, hide={'west' if sg > 0 else 'east'})
        m.pointe('head', f'corne_{n_}', x + sg * 14, y + 24, z, 30, 5, AM, inc=(-34, -sg * 16), base=2.2)
        m.pointe('head', f'corne2_{n_}', x + sg * 22, y + 16, z + 6, 22, 4, AM, inc=(-30, -sg * 44), base=2.2)
    p.bone('machoire', (x, y - 6, z - 14), 'head')
    m.b = 'machoire'
    m.c('mandibule', (x - 18, y - 24, z - 54), (x + 18, y - 8, z - 8), ET, base=2.4, ton=.2)
    m.c('gueule_b', (x - 14, y - 8, z - 52), (x + 14, y - 7, z - 12), ('uni', 'rose_chaos'), base=2, hide={'down'})
    for i in range(6):
        xd = x - 14 + i * 5
        m.c(f'dent_b{i}', (xd, y - 7, z - 54), (xd + 3, y + (i % 2) * 2 - 1, z - 51), ('griffe', 'ivoire'), base=4)
    chaine = [c[0] for c in ch]
    serpent(p, chaine[1:], head='head', jaw='machoire', nom_move='fly', move=1.5, axe='z', oscille=1.4, oscille_idle=.7, leve=chaine[-3:], retard=.07)
    return p


NEANT = {'mite_astrale': mite_astrale, 'rodeur_vide': rodeur_vide, 'oeil_chaos': oeil_chaos, 'chevalier_fracture': chevalier_fracture, 'devoreur_etoiles': devoreur_etoiles}
