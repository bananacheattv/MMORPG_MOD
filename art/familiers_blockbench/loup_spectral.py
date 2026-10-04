# -*- coding: utf-8 -*-
"""Loup spectral : petit loup quadrupede (environ 0,94 bloc au garrot, 1,4 bloc du museau a la croupe, hors queue),
pelage gris bleute tres clair, yeux cyan, oreilles angulaires, museau cubique, collerette de fourrure,
queue touffue en 3 segments, marques runiques cyan peintes dans la texture. Corps entierement opaque."""
import entity_kit as ek
from entity_kit import pal, Mat, stamp, border

KEY = 'loup_spectral'
TITLE = 'Loup spectral'
ENTITY_CLASS = 'LoupSpectralEntity'
VISIBLE_BOX = (0.6, 1.0, 0)
VIEW_SCALE = 18.0
SHEET_SCALE = 10.0
GIF_SCALE = 9.0

P_FOUR = pal('#7a8599', '#96a1b4', '#b2bccb', '#ccd4e0', '#e2e8f0', '#f6f8fb')
P_SOMBRE = pal('#323846', '#444b5b', '#596172', '#6d7688')
P_RUNE = pal('#0f7f9c', '#19a9c9', '#3fd4ec', '#9ef4ff', '#e2fdff')
NOIR = (22, 24, 30)

FUR = Mat(P_FOUR, level=0.64, noise=0.32, scale=(1, 2.5), grad=0.2, up=0.15, down=-0.35)
FUR_CLAIR = Mat(P_FOUR, level=0.78, noise=0.28, scale=(1, 2), grad=0.15, up=0.1, down=-0.25)
SOMBRE = Mat(P_SOMBRE, level=0.5, noise=0.25, scale=1.5, grad=0.2)

R = {'r': P_RUNE[2], 'R': P_RUNE[3], 'C': P_RUNE[4], 'c': P_RUNE[2], 'K': NOIR, 'k': P_SOMBRE[1], 'w': P_FOUR[5]}
G = 'rRCc'                     # caracteres lumineux (masque emissif)

RUNE_ZIGZAG = ['.r.', 'rr.', '.r.', '.rr', '.r.']
RUNE_SPIRALE = ['rrrr', 'r...', 'r.rr', 'r..r', 'rrrr']
RUNE_PATTE = ['r.', 'rr', 'r.', 'r.', '.r']


def rune(rows, faces, ax='c', ay='c', dx=0, dy=0):
    """Rune cyan (lumineuse) sur certaines faces."""
    def d(f):
        if f.name in faces:
            stamp(rows, R, ax=ax, ay=ay, dx=dx, dy=dy, glow=G)(f)
    return d


FACE = stamp([
    '...r...',
    '..rRr..',
    '...r...',
    'CR...RC',
    'kk...kk',
    '.......',
], R, glow=G, ay='t')
SNOUT = stamp(['kKk', '.k.', '...'], R, ay='t')


def ear_shape(f):
    """Oreille angulaire : coins du haut decoupes, interieur cyan."""
    if f.name in ('north', 'south'):
        f.clear(0, 0)
        f.clear(f.w - 1, 0)
        if f.name == 'north':
            for y in range(1, f.h):
                f.px(f.w // 2, y, P_RUNE[2] if y > 1 else P_RUNE[3], glow=True)
    elif f.name in ('east', 'west'):
        f.clear(0, 0)
    elif f.name == 'up':
        f.clear(0, 0)
        f.clear(f.w - 1, 0)


def fluff(f):
    """Collerette : meches plus claires et bord inferieur dechiquete (pixels decoupes)."""
    if f.side:
        for x in range(f.w):
            if (x * 7) % 3 == 0:
                f.clear(x, f.h - 1)
            if x % 2 == 0:
                f.px(x, f.h // 2, P_FOUR[5])


def paw(f):
    if f.name == 'north':
        for x in range(f.w):
            f.px(x, 0, NOIR if x % 2 == 0 else P_SOMBRE[2])


def tail_tip(f):
    """Bout de queue spectral : degrade vers le cyan."""
    for y in range(f.h):
        for x in range(f.w):
            if f.name == 'south' or (f.side and x >= f.w - 2 and f.name in ('east',)) or (f.side and x <= 1 and f.name == 'west'):
                f.px(x, y, P_RUNE[3] if (x + y) % 2 else P_RUNE[2], glow=True)
            elif f.side and f.rng.random() < 0.25:
                f.px(x, y, P_RUNE[1], glow=True)


class Decal:
    def __init__(self, *decos):
        self.decos = decos

    def paint(self, f):
        for y in range(f.h):
            for x in range(f.w):
                f.clear(x, y)
        for d in self.decos:
            d(f)


LIDS = Decal(lambda f: stamp(['kk...kk'], R, ay='t')(f) if f.name == 'north' else None)


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))
    co = m.bone('corps', (0, 9, 4), 'racine')
    m.cube(co, 'poitrail', (-4.5, 7, -6), (9, 7, 7), FUR, deco={'east': [rune(RUNE_ZIGZAG, ('east', 'west'))], 'west': [rune(RUNE_ZIGZAG, ('east', 'west'))]})
    m.cube(co, 'arriere_train', (-4, 7.5, 1), (8, 6, 6), FUR, deco={'sides': [rune(RUNE_SPIRALE, ('east', 'west'), dy=-1)]})
    m.cube(co, 'criniere', (-5, 13, -6), (10, 2, 7), FUR_CLAIR)
    m.cube(co, 'collerette', (-5.5, 7, -7.5), (11, 8, 4), FUR_CLAIR, deco={'sides': [fluff]})
    m.cube(co, 'jabot', (-3, 5.5, -7.5), (6, 2, 3), FUR_CLAIR, deco={'sides': [fluff]})

    cu = m.bone('cou', (0, 12, -6), co)
    m.cube(cu, 'cou_fourrure', (-2.5, 10, -9), (5, 5, 3), FUR_CLAIR)
    h = m.bone('tete', (0, 13, -8.5), cu)
    m.cube(h, 'crane', (-3.5, 11, -12), (7, 6, 5), FUR, deco={'north': [FACE]})
    m.cube(h, 'museau', (-1.5, 11, -15), (3, 3, 3), FUR_CLAIR, deco={'north': [SNOUT], 'up': [border(P_FOUR[2], 'b')]})
    mc = m.bone('machoire', (0, 11, -12), h)
    m.cube(mc, 'machoire_bas', (-1.5, 10, -14.5), (3, 1, 3), FUR_CLAIR, deco={'north': [stamp(['w.w'], R)]})
    pp = m.bone('paupieres', (0, 14, -12.05), h, rot=(-90, 0, 0))
    m.cube(pp, 'paupieres_plan', (-3.5, 13, -12.05), (7, 1, 0), LIDS)
    for side, sx in (('droite', 1), ('gauche', -1)):
        ob = m.bone(f'oreille_{side}', (2 * sx, 17, -9.5), h, rot=(0, 0, -12 * sx))
        m.cube(ob, f'oreille_{side}_cube', (0.5 if sx > 0 else -3.5, 17, -10), (3, 4, 1), FUR, deco={'*': [ear_shape]},
               share='oreille', mirror=sx < 0)

    # pattes (un os chacune, pivot a l'epaule / a la hanche)
    for nom, (px, pz, top, z0) in {'avant_droite': (2.5, -3.5, 8, -5), 'avant_gauche': (-2.5, -3.5, 8, -5),
                                   'arriere_droite': (2.5, 4.5, 9, 3), 'arriere_gauche': (-2.5, 4.5, 9, 3)}.items():
        b = m.bone(f'patte_{nom}', (px, top - 0.5 if 'arriere' in nom else top, pz), co)
        avant = 'avant' in nom
        x0 = px - 1.5
        m.cube(b, f'patte_{nom}_jambe', (x0, 1, z0), (3, top - 1, 3), FUR,
               deco={'north': [rune(RUNE_PATTE, ('north',), dy=-1)]} if avant else {'sides': [rune(RUNE_PATTE, ('east', 'west'), dy=-1)]},
               share='jambe_avant' if avant else 'jambe_arriere', mirror=px < 0)
        m.cube(b, f'patte_{nom}_pied', (x0, 0, z0 - 1), (3, 1, 4), SOMBRE, deco={'*': [paw]}, share='pied', mirror=px < 0)

    # queue touffue en trois segments, relevee au repos
    q1 = m.bone('queue_1', (0, 12, 7), co, rot=(-30, 0, 0))
    m.cube(q1, 'queue_1_fourrure', (-1.5, 10.5, 7), (3, 3, 4), FUR)
    q2 = m.bone('queue_2', (0, 12, 11), q1, rot=(-15, 0, 0))
    m.cube(q2, 'queue_2_fourrure', (-2.5, 9.5, 11), (5, 5, 4), FUR_CLAIR)
    q3 = m.bone('queue_3', (0, 12, 15), q2, rot=(-10, 0, 0))
    m.cube(q3, 'queue_3_pointe', (-1.5, 10.5, 15), (3, 3, 3), FUR_CLAIR, deco={'*': [tail_tip]})
    return m


COLORS = {'racine': 0, 'corps': 1, 'cou': 2, 'tete': 2, 'machoire': 5, 'queue_1': 6, 'patte_avant_droite': 3,
          'patte_avant_gauche': 3, 'patte_arriere_droite': 4, 'patte_arriere_gauche': 4}


# ============================================================================================== animations
PATTES = ['patte_avant_droite', 'patte_avant_gauche', 'patte_arriere_droite', 'patte_arriere_gauche']
QUEUE = ['queue_1', 'queue_2', 'queue_3']
LONG = 8.0                     # epaule / hanche -> sol


def gait(a, T, amp, phases, stance=0.6, lift=1.2, bob=True, samples=10):
    """Allure a appuis coherents : chaque patte recule a vitesse constante pendant l'appui (pied pose au sol pendant
    que le corps avance), puis revient vers l'avant patte levee. phases : decalage (0..1) de chaque patte.
    Le corps descend juste ce qu'il faut pour que la patte d'appui la plus verticale touche le sol."""
    import math

    def angle(p, t):
        u = ((t / T) - p) % 1.0
        if u < stance:
            return amp - 2 * amp * u / stance, u, True
        s = (u - stance) / (1 - stance)
        return -amp + 2 * amp * s, u, False

    for b, p in phases.items():
        ts = sorted({0.0, T} | {round(((p + k) % 1.0) * T, 4) for k in (0.0, stance, stance + (1 - stance) / 2)})
        a.key(b, 'rotation', [(t, angle(p, t)[0], 0, 0, 'linear') for t in ts])
        pk = []
        for t in ts:
            ang, u, st = angle(p, t)
            mid = (not st) and abs((u - stance) / (1 - stance) - 0.5) < 1e-3
            pk.append((t, 0, lift if mid else 0, 0, 'linear'))
        a.key(b, 'position', pk)
    if bob:
        keys = []
        for k in range(samples + 1):
            t = T * k / samples
            best = None
            for b, p in phases.items():
                ang, u, st = angle(p, t)
                if st:
                    c = math.cos(math.radians(ang))
                    best = c if best is None else max(best, c)
            keys.append((t, 0, -LONG * (1 - (best if best else 1.0)), 0, 'linear'))
        a.key('racine', 'position', keys)


def animations(m):
    P, M, SC = ek.key_poses, ek.M, ek.SC
    L = 'linear'
    FERMES = {'paupieres': (90, 0, 0)}

    # --- repos (3 s) : respiration, regard qui balaie, oreilles qui bougent, queue lente
    a = m.anim('repos', 3.0, 'loop')
    P(a, [(0, {}), (0.75, {'tete': (2, 10, 0), 'queue_1': (0, 8, 0), 'oreille_droite': (-6, 0, 0)}),
          (1.5, {'corps': (-1, 0, 0), 'cou': (2, 0, 0), 'queue_1': (0, 0, 0)}),
          (2.25, {'tete': (0, -10, 3), 'queue_1': (0, -8, 0), 'oreille_gauche': (-6, 0, 0)}), (3.0, {})])
    P(a, [(0, {}), (1.5, {'corps': (0, 0.25, 0)}), (3.0, {})], 'position', bones=['corps'])

    # --- marche (1 s) : pas diagonal (avant droite + arriere gauche ensemble), appuis coherents
    a = m.anim('marche', 1.0, 'loop')
    gait(a, 1.0, 20, {'patte_avant_droite': 0.0, 'patte_arriere_gauche': 0.0, 'patte_avant_gauche': 0.5, 'patte_arriere_droite': 0.5})
    P(a, [(0, {'tete': (2, 0, 0), 'queue_1': (0, 10, 0)}), (0.25, {'tete': (-2, 0, 0)}), (0.5, {'tete': (2, 0, 0), 'queue_1': (0, -10, 0)}),
          (0.75, {'tete': (-2, 0, 0)}), (1.0, {'tete': (2, 0, 0), 'queue_1': (0, 10, 0)})])

    # --- course (0,5 s) : galop (pattes avant ensemble, pattes arriere ensemble), corps qui s'etire, oreilles couchees
    a = m.anim('course', 0.5, 'loop')
    gait(a, 0.5, 42, {'patte_avant_droite': 0.0, 'patte_avant_gauche': 0.07, 'patte_arriere_droite': 0.5, 'patte_arriere_gauche': 0.57},
         stance=0.45, lift=2.0, bob=False)
    course = {'cou': (-12, 0, 0), 'tete': (4, 0, 0), 'oreille_droite': (35, 0, 0), 'oreille_gauche': (35, 0, 0), 'machoire': (-10, 0, 0),
              'queue_1': (28, 0, 0), 'queue_2': (8, 0, 0), 'queue_3': (6, 0, 0)}
    P(a, [(0, M(course, {'corps': (4, 0, 0)})), (0.125, M(course, {'corps': (0, 0, 0), 'queue_2': (6, 0, 0)})),
          (0.25, M(course, {'corps': (-5, 0, 0)})), (0.375, M(course, {'corps': (0, 0, 0), 'queue_2': (-6, 0, 0)})), (0.5, M(course, {'corps': (4, 0, 0)}))])
    P(a, [(0, {'racine': (0, 1.6, 0)}), (0.125, {'racine': (0, 0.4, 0)}), (0.25, {'racine': (0, 0.4, 0)}), (0.375, {'racine': (0, 0.8, 0)}),
          (0.5, {'racine': (0, 1.6, 0)})], 'position', bones=['racine'])

    # --- position assise (3 s, en boucle) : arriere-train au sol, pattes avant droites, tete droite
    a = m.anim('assis', 3.0, 'loop')
    assis = {'corps': (46, 0, 0), 'cou': (-40, 0, 0), 'tete': (-4, 0, 0), 'patte_avant_droite': ASSIS_AVANT, 'patte_avant_gauche': ASSIS_AVANT,
             'patte_arriere_droite': ASSIS_ARRIERE, 'patte_arriere_gauche': ASSIS_ARRIERE, 'queue_1': ASSIS_QUEUE, 'queue_2': (8, 0, 0)}
    P(a, [(0, assis), (1.5, M(assis, {'corps': (1, 0, 0), 'tete': (0, 8, 4)})), (3.0, assis)])
    P(a, [(0, {'racine': ASSIS_Y}), (1.5, {'racine': (ASSIS_Y[0], ASSIS_Y[1] + 0.2, ASSIS_Y[2])}), (3.0, {'racine': ASSIS_Y})], 'position', bones=['racine'])

    # --- sommeil couche (4 s, en boucle) : pattes repliees, tete posee, yeux fermes, queue enroulee
    a = m.anim('sommeil', 4.0, 'loop')
    dort = M(FERMES, {'patte_avant_droite': (83, 0, 0), 'patte_avant_gauche': (83, 0, 0), 'patte_arriere_droite': (82, 0, -18),
                      'patte_arriere_gauche': (82, 0, 18), 'cou': COU_DORT, 'tete': (-8, 0, 6), 'oreille_droite': (20, 0, 10),
                      'oreille_gauche': (20, 0, -10), 'queue_1': (40, 45, 0), 'queue_2': (5, 35, 0), 'queue_3': (5, 30, 0)})
    P(a, [(0, dort), (2.0, M(dort, {'corps': (-1, 0, 0), 'cou': (1, 0, 0)})), (4.0, dort)])
    P(a, [(0, {'racine': DORT_Y}), (2.0, {'racine': (0, DORT_Y[1] + 0.25, 0)}), (4.0, {'racine': DORT_Y})], 'position', bones=['racine'])

    # --- mouvement joyeux de la queue (0,5 s, en boucle) : queue haute qui fouette, petit rebond, gueule entrouverte
    a = m.anim('queue_joyeuse', 0.5, 'loop')
    joie = {'queue_1': (-12, 0, 0), 'oreille_droite': (-8, 0, 0), 'oreille_gauche': (-8, 0, 0), 'machoire': (-12, 0, 0)}
    P(a, [(0, M(joie, {'queue_1': (0, 35, 0), 'queue_2': (0, 15, 0), 'queue_3': (0, 10, 0), 'corps': (0, -3, 0), 'tete': (0, 0, 4)})),
          (0.25, M(joie, {'queue_1': (0, -35, 0), 'queue_2': (0, -15, 0), 'queue_3': (0, -10, 0), 'corps': (0, 3, 0), 'tete': (0, 0, -4)})),
          (0.5, M(joie, {'queue_1': (0, 35, 0), 'queue_2': (0, 15, 0), 'queue_3': (0, 10, 0), 'corps': (0, -3, 0), 'tete': (0, 0, 4)}))])
    P(a, [(0, {}), (0.125, {'corps': (0, 0.5, 0)}), (0.25, {}), (0.375, {'corps': (0, 0.5, 0)}), (0.5, {})], 'position', bones=['corps'])

    # --- hurlement (2,6 s) : se campe, leve le museau, gueule ouverte, oreilles en arriere (son et particules : jeu)
    a = m.anim('hurlement', 2.6, 'once')
    campe = {'corps': (6, 0, 0), 'patte_avant_droite': (-6, 0, 0), 'patte_avant_gauche': (-6, 0, 0), 'queue_1': (18, 0, 0)}
    hurle = M(campe, {'cou': (34, 0, 0), 'tete': (28, 0, 0), 'machoire': (-28, 0, 0), 'oreille_droite': (22, 0, -6), 'oreille_gauche': (22, 0, 6)})
    vib = [(0.8 + 0.12 * i, M(hurle, {'tete': (0, 0, 1.5 if i % 2 else -1.5)}), L) for i in range(10)]
    P(a, [(0, {}), (0.4, M(campe, {'cou': (-6, 0, 0)})), (0.75, hurle)] + vib + [(2.0, hurle), (2.6, {})])

    # --- morsure courte (0,8 s) : recul, bond de la tete gueule ouverte, claquement, retour
    a = m.anim('morsure', 0.8, 'once')
    recul = {'corps': (4, 0, 0), 'cou': (10, 0, 0), 'tete': (6, 0, 0), 'oreille_droite': (15, 0, 0), 'oreille_gauche': (15, 0, 0),
             'patte_avant_droite': (6, 0, 0), 'patte_avant_gauche': (6, 0, 0), 'patte_arriere_droite': (6, 0, 0), 'patte_arriere_gauche': (6, 0, 0)}
    bond = {'corps': (-6, 0, 0), 'cou': (-16, 0, 0), 'tete': (-4, 0, 0), 'machoire': (-32, 0, 0), 'oreille_droite': (-10, 0, 0), 'oreille_gauche': (-10, 0, 0),
            'patte_avant_droite': (-18, 0, 0), 'patte_avant_gauche': (-18, 0, 0), 'patte_arriere_droite': (-18, 0, 0), 'patte_arriere_gauche': (-18, 0, 0)}
    claque = dict(bond, machoire=(2, 0, 0))
    P(a, [(0, {}), (0.18, recul), (0.3, bond, L), (0.36, claque, L), (0.48, claque), (0.8, {})])
    P(a, [(0, {}), (0.18, {'corps': (0, 0, 1)}), (0.3, {'corps': (0, 1.1, -2.5)}, L), (0.48, {'corps': (0, 1.1, -2.5)}), (0.8, {})], 'position', bones=['corps'])


# poses calculees pour poser le loup au sol (arriere-train, pattes et queue au contact ; reglees avec ek.subtree_min_y)
ASSIS_AVANT = (-21.5, 0, 0)
ASSIS_ARRIERE = (37.6, 0, 0)
ASSIS_QUEUE = (-13, 0, 0)
ASSIS_Y = (0, -5.8, 0)
COU_DORT = (-28, 0, 0)
DORT_Y = (0, -5.5, 0)
