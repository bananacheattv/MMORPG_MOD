# -*- coding: utf-8 -*-
"""Phenix dore : oiseau de feu (environ 1,4 bloc de haut avec la crete), plumage or, poitrail creme,
pointes de plumes orange et rouges lumineuses, crete de trois plumes, bec crochu orange, oeil noir a iris de braise,
grandes ailes a remiges separees (deux segments), longue queue de plumes tombantes, pattes brunes a serres orange."""
import entity_kit as ek
from entity_kit import pal, Mat, stamp

KEY = 'phenix_dore'
TITLE = 'Phénix doré'
ENTITY_CLASS = 'PhenixDoreEntity'
VISIBLE_BOX = (1.0, 1.5, 0)
VIEW_SCALE = 15.0
SHEET_SCALE = 9.0
GIF_SCALE = 8.0
SHEET_YAW = 50

P_OR = pal('#b86e0a', '#dc9214', '#f2b21e', '#fbcd36', '#fde25e', '#fff09a')
P_ORANGE = pal('#9c2a08', '#c8420c', '#e86214', '#f8861e', '#ffa830')
P_CREME = pal('#dcb878', '#ecd09a', '#f6e2b6', '#fdf0d4')
P_BRUN = pal('#4a2a14', '#62391c', '#7c4a24', '#94602e')
NOIR = (26, 16, 10)
BRAISE = (255, 120, 20)
BRAISE_C = (255, 196, 70)
C = {'K': NOIR, 'O': BRAISE, 'o': BRAISE_C, 'c': P_CREME[2], 'C': P_CREME[3]}

PLUME = Mat(P_OR, level=0.55, noise=0.25, scale=1.5, grad=0.2, up=0.15)
CREME = Mat(P_CREME, level=0.55, noise=0.2, scale=1.5, grad=0.15)
BEC = Mat(P_ORANGE, level=0.75, noise=0.15, scale=1, grad=0.25)
PATTE = Mat(P_BRUN, level=0.5, noise=0.2, scale=1, grad=0.2)


def feather(axis, start, length, sign=1, hot=0.62):
    """Plume coloree selon la distance a sa racine : or -> orange -> rouge braise (pointe lumineuse)."""
    idx = {'x': 0, 'y': 1, 'z': 2}[axis]

    def d(f):
        for j in range(f.h):
            for i in range(f.w):
                p = f.world(i, j)[idx]
                t = (p - start) * sign / max(0.1, length)
                if t < hot:
                    continue
                if t < hot + 0.16:
                    f.px(i, j, P_ORANGE[4] if (i + j) % 2 else P_ORANGE[3], glow=True)
                elif t < hot + 0.3:
                    f.px(i, j, P_ORANGE[3] if (i + j) % 2 else P_ORANGE[2], glow=True)
                else:
                    f.px(i, j, P_ORANGE[2] if (i + j) % 2 else P_ORANGE[1], glow=True)
    return d


def plume_mat(axis, start, length, sign=1, hot=0.62, level=0.6):
    return Mat(P_OR, level=level, noise=0.2, scale=1.5, grad=0.15, decos=[feather(axis, start, length, sign, hot)])


def eye(f):
    if f.name == 'east':
        stamp(['KKK', 'KOo', 'KKK'], C, ax='r', dx=-1, dy=-1, glow='Oo')(f)
    elif f.name == 'west':
        stamp(['KKK', 'oOK', 'KKK'], C, ax='l', dx=1, dy=-1, glow='Oo')(f)


def belly(f):
    """Poitrail : ecailles de plumes creme et petites pointes or."""
    if f.name == 'north':
        for y in range(f.h):
            for x in range(f.w):
                if (x + 2 * y) % 4 == 0:
                    f.px(x, y, P_CREME[1])
                if y == 0 and x % 2:
                    f.px(x, y, P_OR[3])


def coverts(f):
    """Couvertures des ailes : creme avec taches orange."""
    if f.name in ('up', 'down'):
        for y in range(f.h):
            for x in range(f.w):
                if (x * 3 + y * 5) % 7 == 0:
                    f.px(x, y, P_ORANGE[3], glow=f.name == 'up')


def talons(f):
    if f.name == 'up':
        for x in range(f.w):
            f.px(x, f.h - 1, P_ORANGE[1])


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))

    # ---------------------------------------------------------------- pattes
    for side, sx in (('droite', 1), ('gauche', -1)):
        p = m.bone(f'patte_{side}', (1.6 * sx, 6, 1), 'racine')
        m.cube(p, f'cuisse_{side}', (1.6 * sx - 1, 4, 0), (2, 3, 2), PLUME, share='cuisse', mirror=sx < 0)
        m.cube(p, f'tarse_{side}', (1.6 * sx - 0.5, 1, 0.5), (1, 3, 1), PATTE, share='tarse', mirror=sx < 0)
        m.cube(p, f'pied_{side}', (1.6 * sx - 1.5, 0, -1), (3, 1, 3), Mat(P_ORANGE, level=0.7, noise=0.15, decos=[talons]),
               share='pied', mirror=sx < 0)
        m.cube(p, f'serres_{side}', (1.6 * sx - 1.5, 0, -2), (3, 1, 1), Mat(P_ORANGE, level=0.35, noise=0.1), inflate=-0.1,
               share='serres', mirror=sx < 0)

    # ---------------------------------------------------------------- corps
    co = m.bone('corps', (0, 7, 1), 'racine')
    m.cube(co, 'torse', (-3, 6, -2), (6, 7, 6), PLUME)
    m.cube(co, 'poitrail', (-2.5, 6.5, -2.6), (5, 6, 1), Mat(P_CREME, level=0.6, noise=0.2, decos=[belly]))
    m.cube(co, 'croupion', (-2.5, 6, 3.5), (5, 4, 2), PLUME)

    # ---------------------------------------------------------------- cou et tete
    cou = m.bone('cou', (0, 12, 0), co)
    m.cube(cou, 'cou_plumes', (-2, 11.5, -2.5), (4, 5, 4), PLUME)
    m.cube(cou, 'gorge', (-1.5, 12, -2.9), (3, 4, 1), CREME)
    t = m.bone('tete', (0, 16, -0.5), cou)
    m.cube(t, 'tete_crane', (-2.5, 15.5, -4), (5, 5, 6), Mat(P_OR, level=0.62, noise=0.2, scale=1.5, grad=0.15, up=0.15),
           deco={'east': [eye], 'west': [eye]})
    m.cube(t, 'joue', (-2.5, 15.5, -4.4), (5, 2, 1), CREME)
    m.cube(t, 'bec_haut', (-1, 17, -7), (2, 2, 3), BEC)
    m.cube(t, 'bec_crochet', (-1, 16, -7), (2, 1, 1), Mat(P_ORANGE, level=0.45, noise=0.1))
    bb = m.bone('bec_bas', (0, 16.8, -4), t)
    m.cube(bb, 'bec_mandibule', (-0.5, 16, -6.2), (1, 1, 2), Mat(P_ORANGE, level=0.55, noise=0.1))
    cr = m.bone('crete', (0, 20.5, 0), t)
    for i, (dx, rz, ln) in enumerate(((0, 0, 6), (-1.4, 14, 5), (1.4, -14, 5))):
        m.cube(cr, f'crete_plume_{i + 1}', (dx - 0.5, 20, -0.5), (1, ln, 1), plume_mat('y', 20, ln, 1, hot=0.45, level=0.65),
               rot=(32, 0, rz), origin=(dx, 20.5, 0))
    for side, sx in (('droite', 1), ('gauche', -1)):
        m.cube(t, f'aigrette_{side}', (2.2 * sx - 0.5, 17.5, 0.5), (1, 1, 4), plume_mat('z', 0.5, 4, 1, hot=0.5),
               rot=(-15, 20 * sx, 0), origin=(2.2 * sx, 18, 0.5), share='aigrette', mirror=sx < 0)

    # ---------------------------------------------------------------- ailes (modelees deployees, repliees par la pose de repos)
    for side, sx in (('droite', 1), ('gauche', -1)):
        a = m.bone(f'aile_{side}', (3 * sx, 12, 0), co, rot=REPLIE[side])
        x0 = 3 if sx > 0 else -9
        m.cube(a, f'aile_{side}_bras', (x0, 11, -1), (6, 2, 2), PLUME, share='aile_bras', mirror=sx < 0)
        m.cube(a, f'aile_{side}_couvertures', (x0, 11.5, 1), (6, 1, 4), Mat(P_CREME, level=0.55, noise=0.2, decos=[coverts]),
               share='aile_couv', mirror=sx < 0)
        for k in range(3):                                      # remiges secondaires (vers l'arriere)
            fx = (x0 + 0.5 + 2 * k) if sx > 0 else (x0 + 4.5 - 2 * k)
            m.cube(a, f'aile_{side}_secondaire_{k + 1}', (fx, 11.6, 3), (2, 1, 6), plume_mat('z', 3, 6, 1, hot=0.6),
                   inflate=-0.05, rot=(0, -8 * k * sx, 0), origin=(fx + 1, 12, 3), share=f'aile_sec_{k}', mirror=sx < 0)
        b = m.bone(f'aile_{side}_bout', (9 * sx, 12, 0), a)
        x1 = 9 if sx > 0 else -14
        m.cube(b, f'aile_{side}_main', (x1, 11, -1), (5, 2, 2), PLUME, share='aile_main', mirror=sx < 0)
        for k in range(4):                                      # remiges primaires en eventail
            ang = (-4 - 12 * k) * sx
            fx = 12 if sx > 0 else -21
            m.cube(b, f'aile_{side}_primaire_{k + 1}', (fx if sx > 0 else fx + 2, 11.5 - 0.02 * k, -0.5 + 0.6 * k), (7, 1, 2),
                   plume_mat('x', 12 * sx, 7, sx, hot=0.5), inflate=-0.03, rot=(0, ang, 0), origin=(12 * sx, 12, 0.5 + 0.6 * k),
                   share=f'aile_prim_{k}', mirror=sx < 0)

    # ---------------------------------------------------------------- queue (deux segments)
    q = m.bone('queue', (0, 8, 5), co, rot=(18, 0, 0))
    for k, (dx, ry) in enumerate(((0, 0), (-1.8, -10), (1.8, 10), (-3.2, -20), (3.2, 20))):
        ln = 9 if k == 0 else 8 if k < 3 else 6
        m.cube(q, f'queue_plume_{k + 1}', (dx - 1, 7.6 + 0.05 * k, 5), (2, 1, ln), plume_mat('z', 5, ln + 8, 1, hot=0.9),
               rot=(0, ry, 0), origin=(dx, 8, 5), share=f'queue_{k}')
    qb = m.bone('queue_bout', (0, 8, 13), q, rot=(16, 0, 0))
    for k, (dx, ry) in enumerate(((0, 0), (-3.4, -14), (3.4, 14))):
        m.cube(qb, f'queue_pointe_{k + 1}', (dx - 1, 7.6, 13 if k == 0 else 12), (2, 1, 8), plume_mat('z', 13, 8, 1, hot=0.25),
               rot=(0, ry, 0), origin=(dx, 8, 13), share=f'queue_pointe_{k}')
    return m


REPLIE = {'droite': (80, -80, 0), 'gauche': (80, 80, 0)}      # ailes rabattues le long du corps
COLORS = {'racine': 0, 'corps': 1, 'cou': 1, 'tete': 2, 'crete': 5, 'bec_bas': 2, 'aile_droite': 3, 'aile_gauche': 3,
          'aile_droite_bout': 3, 'aile_gauche_bout': 3, 'queue': 6, 'queue_bout': 6, 'patte_droite': 4, 'patte_gauche': 4}


def deploie(z_up=0.0, bout=0.0):
    """Pose ailes deployees (annule le repli) ; z_up : battement (+ = vers le haut), bout : flexion de la main."""
    return {'aile_droite': (-80, 80, z_up), 'aile_gauche': (-80, -80, -z_up),
            'aile_droite_bout': (0, 0, bout), 'aile_gauche_bout': (0, 0, -bout)}


def animations(m):
    P, M = ek.key_poses, ek.M
    L = 'linear'

    # --- repos (3 s) : respiration, tete qui regarde autour, queue qui ondule, crete qui frissonne
    a = m.anim('repos', 3.0, 'loop')
    P(a, [(0, {}),
          (0.8, {'tete': (0, 18, 0), 'cou': (4, 0, 0), 'crete': (6, 0, 0), 'queue': (0, 4, 0), 'queue_bout': (4, 0, 0)}),
          (1.6, {'tete': (8, -10, 4), 'cou': (-2, 0, 0), 'queue': (0, -4, 0), 'queue_bout': (-3, 0, 0), 'corps': (2, 0, 0)}),
          (2.3, {'tete': (0, -18, 0), 'crete': (-4, 0, 0), 'queue': (0, 3, 0)}),
          (3.0, {})])
    a.scale('corps', [(0, 1, 1, 1), (1.5, 1.04, 1.03, 1.04), (3.0, 1, 1, 1)])

    # --- vol (0,8 s) : corps penche, grands battements, pattes repliees, queue en traine
    a = m.anim('vol', 0.8, 'loop')
    pose = {'corps': (-48, 0, 0), 'cou': (26, 0, 0), 'tete': (20, 0, 0), 'patte_droite': (-70, 0, 0), 'patte_gauche': (-70, 0, 0),
            'queue': (34, 0, 0), 'queue_bout': (-6, 0, 0)}
    P(a, [(0, M(pose, deploie(42, -18))), (0.2, M(pose, deploie(0, 10))), (0.4, M(pose, deploie(-38, 22), {'queue_bout': (6, 0, 0)})),
          (0.6, M(pose, deploie(0, -6))), (0.8, M(pose, deploie(42, -18)))])
    P(a, [(0, {'racine': (0, 6, 0)}), (0.4, {'racine': (0, 7.2, 0)}), (0.8, {'racine': (0, 6, 0)})], 'position', bones=['racine'])

    # --- plane (2 s) : ailes tendues, virage doux, queue qui gouverne
    a = m.anim('plane', 2.0, 'loop')
    pl = {'corps': (-42, 0, 0), 'cou': (24, 0, 0), 'tete': (16, 0, 0), 'patte_droite': (-70, 0, 0), 'patte_gauche': (-70, 0, 0),
          'queue': (30, 0, 0)}
    P(a, [(0, M(pl, deploie(6, 4), {'racine': (0, 0, -8)})), (1.0, M(pl, deploie(-2, -4), {'racine': (0, 0, 8), 'queue': (0, 8, 0)})),
          (2.0, M(pl, deploie(6, 4), {'racine': (0, 0, -8)}))])
    P(a, [(0, {'racine': (0, 6.5, 0)}), (1.0, {'racine': (0, 7.5, 0)}), (2.0, {'racine': (0, 6.5, 0)})], 'position', bones=['racine'])

    # --- joie (1,8 s) : deploie les ailes vers le ciel, crie (bec ouvert), petit bond, crete dressee
    a = m.anim('joie', 1.8, 'once')
    haut = M(deploie(55, 25), {'cou': (14, 0, 0), 'tete': (22, 0, 0), 'crete': (-18, 0, 0), 'bec_bas': (-35, 0, 0), 'queue': (-10, 0, 0),
                               'corps': (6, 0, 0)})
    P(a, [(0, {}), (0.35, haut), (0.6, M(haut, deploie(-10, -10))), (0.85, haut), (1.1, M(haut, deploie(-10, -10))), (1.3, haut), (1.8, {})])
    P(a, [(0, {}), (0.5, {'racine': (0, 3, 0)}), (0.9, {'racine': (0, 1, 0)}), (1.3, {'racine': (0, 2, 0)}), (1.8, {})], 'position',
      bones=['racine'])
