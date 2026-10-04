# -*- coding: utf-8 -*-
"""Golem de poche : petit golem de pierre grise couverte de mousse (environ 0,9 bloc), tete cubique aux yeux ambres
lumineux, noyau ambre enchasse dans la poitrine, gros bras de pierre, jambes courtes, spirales gravees."""
import entity_kit as ek
from entity_kit import pal, Mat, stamp, blotches

KEY = 'golem_poche'
TITLE = 'Golem de poche'
ENTITY_CLASS = 'GolemPocheEntity'
VISIBLE_BOX = (0.8, 1.15, 0)
VIEW_SCALE = 22.0
SHEET_SCALE = 13.0
GIF_SCALE = 11.0

P_PIERRE = pal('#77726a', '#8f8a80', '#a6a095', '#bab4a8', '#cbc6b9', '#ddd8cc')
P_MOUSSE = pal('#4b5f1a', '#627a20', '#7b9627', '#93ad31', '#a9c13d')
P_AMBRE = pal('#8a4206', '#c76a0c', '#f09a1c', '#ffc54a', '#ffe9a0')
NOIR = (30, 28, 26)
ORBITE = (52, 46, 40)

MOSS = blotches(1.6, [P_MOUSSE[1], P_MOUSSE[2]], (0.6, 1.2))


def moss_cap(f):
    """Mousse : dessus couvert, coulures irregulieres sur le haut des faces laterales."""
    if f.name == 'up':                                       # plaques de mousse (pas un tapis uniforme)
        blotches(9, [P_MOUSSE[2], P_MOUSSE[3], P_MOUSSE[4]], (1.0, 2.0))(f)
    elif f.side:                                             # coulures depuis le bord haut
        x = f.rng.randrange(max(1, f.w))
        while x < f.w:
            d = f.rng.choice((1, 2, 2, 3, 4))
            for xx in range(x, min(f.w, x + f.rng.choice((1, 2, 2)))):
                for y in range(d - (xx - x)):
                    f.px(xx, y, P_MOUSSE[3] if y == 0 else P_MOUSSE[2])
            x += f.rng.choice((2, 3, 4))


def cracks_dark(f):
    for _ in range(max(1, f.w * f.h // 45)):
        x, y = f.rng.randrange(f.w), f.rng.randrange(f.h)
        for k in range(f.rng.randint(2, 3)):
            f.px(x + k * f.rng.choice((-1, 0, 1)), y + k, P_PIERRE[0])


def spiral(f):
    """Spirale gravee (motif en G) sur les flancs des blocs assez grands."""
    if not f.side:
        return
    if f.w >= 5 and f.h >= 5:
        stamp(['ddddd', 'd....', 'd.ddd', 'd...d', 'ddddd'], {'d': P_PIERRE[1]})(f)
    elif f.w >= 4 and f.h >= 4:
        stamp(['dddd', 'd...', 'd.dd', 'dddd'], {'d': P_PIERRE[1]})(f)


def bricks(f):
    """Joints de pierre taillee (blocs de 4 texels decales)."""
    if f.name == 'down':
        return
    for y in range(f.h):
        for x in range(f.w):
            if y % 4 == 3 or (x + (2 if (y // 4) % 2 else 0)) % 5 == 4:
                if f.rng.random() < 0.55:
                    f.px(x, y, P_PIERRE[1])


STONE = Mat(P_PIERRE, level=0.55, noise=0.28, scale=2.0, grad=0.15, up=0.12, decos=[cracks_dark, MOSS, moss_cap])
STONE_SPIRAL = Mat(P_PIERRE, level=0.55, noise=0.28, scale=2.0, grad=0.15, up=0.12, decos=[cracks_dark, spiral, MOSS, moss_cap])
STONE_PLAIN = Mat(P_PIERRE, level=0.5, noise=0.25, scale=2.0, grad=0.12, decos=[cracks_dark])
AMBER = Mat(P_AMBRE, level=0.6, noise=0.2, scale=1, grad=0.3, glow=True,
            decos=[lambda f: f.px(f.w // 2, f.h // 2, P_AMBRE[4], glow=True) if f.w > 1 and f.h > 1 else None])
C = {'A': P_AMBRE[3], 'a': P_AMBRE[2], 'W': P_AMBRE[4], 'd': P_PIERRE[0], 'D': P_PIERRE[1], 'g': P_MOUSSE[2], 'G': P_MOUSSE[3],
     'K': ORBITE}


class Decal:
    def __init__(self, *decos):
        self.decos = decos

    def paint(self, f):
        for y in range(f.h):
            for x in range(f.w):
                f.clear(x, y)
        for d in self.decos:
            d(f)


# visage (face avant de la tete 8x7) : arcade sombre, deux yeux ambres lumineux, bouche gravee
FACE = stamp([
    '........',
    'KKKddKKK',
    'KAWddWAK',
    'KaAddAaK',
    'dKKddKKd',
    '..dDDd..',
    '........',
], C, glow='AaW', ay='t')
LIDS = Decal(lambda f: stamp(['DDD..DDD', 'ddd..ddd'], C, ay='t')(f) if f.name == 'north' else None)
NOYAU_SERTI = {'north': [stamp(['dDDDd', 'D___D', 'D___D', 'D___D', 'dDDDd'], C)]}
GEMME = {'north': [stamp(['_A_', 'AWA', '_a_'], C, glow='AaW')]}


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))
    for side, sx in (('droite', 1), ('gauche', -1)):
        lg = m.bone(f'jambe_{side}', (2 * sx, 4, 0), 'racine')
        m.cube(lg, f'jambe_{side}_bloc', (2 * sx - 1.5, 1, -1.5), (3, 3, 3), STONE_PLAIN, share='jambe', mirror=sx < 0)
        m.cube(lg, f'pied_{side}', (2 * sx - 2, 0, -2.5), (4, 2, 4), STONE, share='pied', mirror=sx < 0)

    co = m.bone('corps', (0, 4, 0), 'racine')
    m.cube(co, 'torse', (-4, 4, -2.5), (8, 6, 5), STONE)
    m.cube(co, 'epaules', (-4.5, 8, -3), (9, 2, 6), STONE)
    nb = m.bone('noyau', (0, 7, -3), co)
    m.cube(nb, 'noyau_serti', (-2.5, 4.5, -3), (5, 5, 1), STONE_PLAIN, deco=NOYAU_SERTI)
    m.cube(nb, 'noyau_cristal', (-1.5, 5.5, -3.3), (3, 3, 1), AMBER, deco=GEMME)

    h = m.bone('tete', (0, 10, 0), co)
    m.cube(h, 'tete_cube', (-4, 10, -3.5), (8, 7, 7), STONE, deco={'north': [FACE]})
    m.cube(h, 'arcade', (-4.5, 15, -4.5), (9, 1, 2), STONE_PLAIN, deco={'up': [moss_cap]})
    pp = m.bone('paupieres', (0, 15, -3.55), h, rot=(-90, 0, 0))
    m.cube(pp, 'paupieres_plan', (-4, 13, -3.55), (8, 2, 0), LIDS)

    for side, sx in (('droit', 1), ('gauche', -1)):
        b = m.bone(f'bras_{side}', (5.5 * sx, 9, 0), co, rot=(0, 0, 5 * sx))
        x0 = 4.5 if sx > 0 else -8.5
        m.cube(b, f'bras_{side}_epaule', (x0, 6, -2), (4, 4, 4), STONE_SPIRAL, share='bras_epaule', mirror=sx < 0)
        ab = m.bone(f'avant_bras_{side}', (6.5 * sx, 6.5, 0), b)
        m.cube(ab, f'avant_bras_{side}_bloc', (x0 + 0.5, 4.5, -1.5), (3, 2, 3), STONE_PLAIN, share='avant_bras', mirror=sx < 0)
        m.cube(ab, f'poing_{side}', (x0 if sx > 0 else x0 - 1, 0.5, -2.5), (5, 5, 5), STONE_SPIRAL, share='poing', mirror=sx < 0)
    return m


COLORS = {'racine': 0, 'corps': 1, 'tete': 2, 'noyau': 5, 'bras_droit': 3, 'bras_gauche': 3, 'jambe_droite': 4, 'jambe_gauche': 4}


def animations(m):
    P, M = ek.key_poses, ek.M
    L = 'linear'
    FERMES = {'paupieres': (90, 0, 0)}
    NOYAU = ['noyau']

    # --- repos (3 s) : respiration lourde, noyau qui pulse, bras qui pendent
    a = m.anim('repos', 3.0, 'loop')
    P(a, [(0, {}), (1.5, {'corps': (2, 0, 0), 'tete': (-3, 0, 0), 'bras_droit': (0, 0, 3), 'bras_gauche': (0, 0, -3)}), (3.0, {})])
    P(a, [(0, {}), (1.5, {'corps': (0, -0.3, 0)}), (3.0, {})], 'position', bones=['corps'])
    a.scale('noyau', [(0, 1, 1, 1), (1.5, 1.25, 1.25, 1.25), (3.0, 1, 1, 1)])

    # --- marche (1,2 s) : pas lourds et courts, balancement du corps, bras opposes aux jambes
    a = m.anim('marche', 1.2, 'loop')
    pas_d = {'jambe_droite': (-22, 0, 0), 'jambe_gauche': (22, 0, 0), 'bras_droit': (18, 0, 0), 'bras_gauche': (-18, 0, 0), 'corps': (0, 0, -4)}
    pas_g = {'jambe_droite': (22, 0, 0), 'jambe_gauche': (-22, 0, 0), 'bras_droit': (-18, 0, 0), 'bras_gauche': (18, 0, 0), 'corps': (0, 0, 4)}
    P(a, [(0, pas_d), (0.6, pas_g), (1.2, pas_d)])
    P(a, [(0, {'racine': (0, 0.7, 0)}), (0.3, {'racine': (0, 1.1, 0)}), (0.6, {'racine': (0, 0.7, 0)}), (0.9, {'racine': (0, 1.1, 0)}),
           (1.2, {'racine': (0, 0.7, 0)})], 'position', bones=['racine'])

    # --- joie (1,4 s) : leve les bras, petit saut, noyau qui brille
    a = m.anim('joie', 1.4, 'once')
    bras_leves = {'bras_droit': (0, 0, 150), 'bras_gauche': (0, 0, -150), 'avant_bras_droit': (0, 0, 10), 'avant_bras_gauche': (0, 0, -10),
                  'tete': (12, 0, 0)}
    P(a, [(0, {}), (0.3, bras_leves), (0.55, M(bras_leves, {'corps': (6, 0, 0)})), (0.8, bras_leves), (1.4, {})])
    P(a, [(0, {}), (0.3, {'racine': (0, 0, 0)}), (0.5, {'racine': (0, 3, 0)}), (0.7, {}), (1.4, {})], 'position', bones=['racine'])
    P(a, [(0, {}), (0.4, {'noyau': (1.5, 1.5, 1.5)}), (1.0, {'noyau': (1.2, 1.2, 1.2)}), (1.4, {})], 'scale', bones=NOYAU)

    # --- frappe au sol (1,2 s) : leve les deux poings, les abat, recule
    a = m.anim('frappe_sol', 1.2, 'once')
    leve = {'bras_droit': (150, 0, 0), 'bras_gauche': (150, 0, 0), 'corps': (10, 0, 0), 'tete': (-6, 0, 0)}
    abat = {'bras_droit': (55, 0, 0), 'bras_gauche': (55, 0, 0), 'corps': (-22, 0, 0), 'tete': (12, 0, 0), 'jambe_droite': (5, 0, 0),
            'jambe_gauche': (5, 0, 0)}
    P(a, [(0, {}), (0.45, leve), (0.6, abat, L), (0.8, abat), (1.2, {})])
    P(a, [(0, {}), (0.45, {'corps': (0, 0.4, 0)}), (0.6, {'corps': (0, -0.1, 0)}, L), (0.8, {'corps': (0, -0.1, 0)}), (1.2, {})],
      'position', bones=['corps'])

    # --- sommeil (4 s, en boucle) : s'assoit (jambes en avant), tete basse, yeux eteints
    a = m.anim('sommeil', 4.0, 'loop')
    dort = M(FERMES, {'jambe_droite': (80, 0, 8), 'jambe_gauche': (80, 0, -8), 'corps': (6, 0, 0), 'tete': (-20, 0, 0),
                      'bras_droit': (SOMMEIL_BRAS, 0, 10), 'bras_gauche': (SOMMEIL_BRAS, 0, -10)})
    P(a, [(0, dort), (2.0, M(dort, {'tete': (-24, 0, 0)})), (4.0, dort)])
    P(a, [(0, {'racine': SOMMEIL_Y}), (2.0, {'racine': (0, SOMMEIL_Y[1], 0)}), (4.0, {'racine': SOMMEIL_Y})], 'position', bones=['racine'])
    a.scale('noyau', [(0, 0.8, 0.8, 0.8), (2.0, 0.9, 0.9, 0.9), (4.0, 0.8, 0.8, 0.8)])


SOMMEIL_Y = (0, -1.55, 0)
SOMMEIL_BRAS = 46
