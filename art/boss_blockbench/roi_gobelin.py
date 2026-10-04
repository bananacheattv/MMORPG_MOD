# -*- coding: utf-8 -*-
"""Roi gobelin : boss trapu d'environ 3 blocs (49 px avec la couronne), couperet ebreche a la main droite."""
import math

import entity_kit as ek
from entity_kit import pal, Mat, stamp, rivets, border, scratches, blotches, tatter, only

KEY = 'roi_gobelin'
TITLE = 'Roi gobelin'
ENTITY_CLASS = 'RoiGobelinEntity'
VISIBLE_BOX = (2.5, 3.2, 0)

# ---------------------------------------------------------------------------------------------- palettes
P_PEAU = pal('#3c5620', '#4b6a28', '#5b7d31', '#6c903a', '#7fa347', '#96b95a')
P_IVOIRE = pal('#857a62', '#aa9f84', '#cbc1a3', '#e8dfc3')
P_OR = pal('#6b470d', '#916213', '#b9841e', '#d9a832', '#f0cd58', '#fde88c')
P_RUBIS = pal('#4c0810', '#7a0e18', '#a91824', '#d6323a', '#ff7272')
P_ROUGE = pal('#45100f', '#5e1615', '#781d1b', '#932621', '#ad322a', '#c64535')
P_FER = pal('#2c2e33', '#3b3e44', '#4b4f56', '#5e626a', '#767a83', '#969aa3')
P_FER2 = pal('#33302c', '#433f39', '#555048', '#69635a', '#817a6e', '#9c9384')
P_CUIR = pal('#2b1a0f', '#3b2515', '#4e321d', '#634127', '#7a5332', '#93673f')
P_TISSU = pal('#1f1d1a', '#2a2723', '#36322d', '#433e37', '#524c43')
P_ACIER = pal('#46484e', '#5e6168', '#787c84', '#959aa2', '#b4b8bf', '#d7dade')
ROUILLE = [(110, 62, 34), (134, 78, 40), (90, 50, 30)]
K = (22, 18, 16)

SKIN = Mat(P_PEAU, level=0.55, noise=0.32, scale=2.5, decos=[blotches(1.2, [P_PEAU[1], P_PEAU[4]], (0.6, 1.2))])
SKIN_DARK = Mat(P_PEAU, level=0.36, noise=0.25, scale=2.5)
IVORY = Mat(P_IVOIRE, level=0.6, noise=0.25, scale=2, grad=0.3)
GOLD = Mat(P_OR, level=0.55, noise=0.2, scale=2, rim=0.25)
RUBY = Mat(P_RUBIS, level=0.6, noise=0.15, scale=1.5, grad=0.4)
RED = Mat(P_ROUGE, level=0.5, noise=0.3, scale=(1.2, 4), grad=0.18)
IRON = Mat(P_FER, level=0.5, noise=0.25, scale=3, rim=0.22,
           decos=[scratches(2, P_FER[5]), blotches(0.8, ROUILLE, (0.5, 1.4), sides_only=True)])
IRON2 = Mat(P_FER2, level=0.5, noise=0.28, scale=3, rim=0.22,
            decos=[scratches(2, P_FER2[5]), blotches(1.2, ROUILLE, (0.6, 1.5))])
LEATHER = Mat(P_CUIR, level=0.5, noise=0.3, scale=(4, 1.5), grad=0.1)
CLOTH = Mat(P_TISSU, level=0.5, noise=0.3, scale=(1.5, 3), grad=0.1)
STEEL = Mat(P_ACIER, level=0.52, noise=0.22, scale=(5, 2), grad=0.2,
            decos=[scratches(3, P_ACIER[1]), blotches(0.5, ROUILLE, (0.5, 1.0))])


def mail(f):
    """Cotte de mailles : anneaux en quinconce."""
    for y in range(f.h):
        for x in range(f.w):
            k = (x + (y // 2)) % 2
            if y % 2 == 0:
                f.px(x, y, P_FER[3] if k == 0 else P_FER[1])
            else:
                f.px(x, y, P_FER[2] if k == 0 else P_FER[0])


MAIL = Mat(P_FER, level=0.4, noise=0.0, decos=[])
MAIL.paint = lambda f: (mail(f), border(P_FER[0], 'b')(f))


def chain_and_trim(f):
    """Plastron : chaine doree en V vers le pendentif."""
    if f.name != 'north':
        return
    cx = f.w / 2.0
    for i in range(f.w):
        d = abs(i + 0.5 - cx)
        y = int(round((cx - d) * 0.7))
        if 0 <= y < f.h - 2 and 1 <= i < f.w - 1:
            f.px(i, y, P_OR[4] if i % 2 else P_OR[2])
            f.px(i, y + 1, P_OR[1] if i % 2 else P_FER[0])


def belt_studs(f):
    if not f.side:
        return
    y = f.h // 2
    for x in range(1, f.w - 1, 3):
        f.px(x, y, P_OR[4])
        f.px(x, y + 1, P_OR[1])


def fingers(f):
    """Poing : jointures et doigts replies."""
    if f.name in ('north',):
        for x in range(f.w):
            for y in (2, 4):
                if x % 2 == 1:
                    f.px(x, y, P_PEAU[1])
        for x in range(0, f.w, 2):
            f.px(x, 0, P_PEAU[4])
    if f.name == 'down':
        for x in range(1, f.w, 2):
            for y in range(f.h):
                f.px(x, y, P_PEAU[1])


def blade_edge(f):
    """Couperet : fil clair et ebreche en bas, dos sombre en haut, trou de suspension pres de la pointe."""
    if f.name in ('east', 'west'):
        for i in range(f.w):
            f.px(i, 0, P_ACIER[1])
            f.px(i, f.h - 1, P_ACIER[5])
            f.px(i, f.h - 2, P_ACIER[4])
        for i in range(1, f.w - 1):
            wz = f.world(i, 0)[2]
            if int(abs(wz) * 7) % 5 == 0:
                f.clear(i, f.h - 1)
                f.px(i, f.h - 2, P_ACIER[2])
        tip_col = 2 if f.name == 'east' else f.w - 3
        f.px(tip_col, 2, K)
        f.px(tip_col + (1 if f.name == 'east' else -1), 2, K)
        f.px(tip_col, 3, (40, 40, 44))
    if f.name == 'down':
        for i in range(f.w):
            for j in range(f.h):
                if int(abs(f.world(i, j)[2]) * 7) % 5 == 0:
                    f.clear(i, j)


def crown_points(f):
    if f.side:
        f.px(f.w // 2, f.h - 1, P_OR[1])


def velvet(f):
    if f.name == 'up':
        for y in range(f.h):
            for x in range(f.w):
                edge = x == 0 or y == 0 or x == f.w - 1 or y == f.h - 1
                f.px(x, y, P_OR[3] if edge else (P_ROUGE[2] if (x + y) % 3 else P_ROUGE[1]))
        cx, cy = f.w // 2, f.h // 2
        for k in range(-2, 3):
            f.px(cx + k, cy, P_OR[4])
            f.px(cx, cy + k, P_OR[4])


def crown_band(f):
    if f.side:
        for x in range(f.w):
            f.px(x, 1, P_OR[2] if x % 2 else P_OR[1])
        for x in range(2, f.w - 1, 4):
            f.px(x, 1, P_RUBIS[3])
            f.px(x, 0, P_RUBIS[4])


def ear_inner(f):
    if f.name == 'north' and f.h >= 3:
        for y in range(1, f.h - 1):
            for x in range(f.w):
                f.px(x, y, (186, 120, 104) if y < f.h - 2 else (150, 92, 82))
        for x in range(0, f.w, 2):
            f.px(x, f.h - 2, (128, 76, 68))


FACE = stamp([
    '................',
    '....d......d....',
    '................',
    '.....dd..dd.....',
    '................',
    '................',
    '.dRRRd....dRRRd.',
    '..dKRd....dRKd..',
    '...dd......dd...',
    '................',
    '..mmmmmmmmmmmm..',
    '................',
], {'d': P_PEAU[0], 'R': (214, 40, 36), 'K': K, 'm': (42, 30, 22)}, ay='t')

SKULL = stamp([
    '.......',
    '.KK.KK.',
    '.KK.KK.',
    '...K...',
    '.K.K.K.',
], {'K': (58, 46, 36)}, ay='t')

BANNER = stamp([
    'Y.Y.Y',
    'YYYYY',
    'YrYrY',
    'YYYYY',
], {'Y': P_OR[4], 'r': P_RUBIS[3]}, ay='t', dy=2)

TEETH = stamp(['.w.ww.ww.w.'], {'w': P_IVOIRE[3]}, ay='t')


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))
    m.bone('bassin', (0, 14, 0), 'racine')
    m.cube('bassin', 'hanches', (-10, 11, -6), (20, 6, 12), CLOTH)
    m.cube('bassin', 'ceinture', (-11, 15, -7), (22, 4, 14), LEATHER, deco={'*': [belt_studs]})
    m.cube('bassin', 'boucle_crane', (-3.5, 14, -9.5), (7, 5, 2), IVORY, deco={'north': [SKULL]})
    m.cube('bassin', 'sacoche', (9.5, 11, -3), (3, 4, 5), LEATHER, deco={'*': [border(P_CUIR[0], 't')]})
    m.bone('etendard', (0, 15, -8), 'bassin')
    m.cube('etendard', 'etendard_tissu', (-4, 3, -8.5), (8, 12, 1), RED,
           deco={'sides': [border(P_OR[3], 'lr'), BANNER, tatter(3, seed=1.3)]})

    m.bone('torse', (0, 17, 0), 'bassin')
    m.cube('torse', 'ventre', (-10, 17, -6), (20, 14, 12), MAIL)
    m.cube('torse', 'plastron', (-9, 22, -7.5), (18, 9, 2), IRON, deco={'*': [rivets(P_FER[5], P_FER[0])], 'north': [chain_and_trim]})
    m.cube('torse', 'plaque_ventre', (-8, 17.5, -7), (16, 5, 1), IRON2, deco={'*': [rivets(P_FER2[5], P_FER2[0], every=5, rows=('t',))]})
    m.cube('torse', 'pendentif', (-1.5, 23, -9), (3, 3, 1), GOLD, deco={'north': [stamp(['.r.', 'rRr', '.r.'], {'r': P_RUBIS[2], 'R': P_RUBIS[4]})]})
    m.cube('torse', 'mantelet', (-12, 29, -6.5), (24, 4, 13), RED, deco={'sides': [border(P_OR[3], 'b')]})
    m.cube('torse', 'cou', (-4, 31, -5), (8, 3, 7), SKIN_DARK)

    m.bone('tete', (0, 32, -3), 'torse')
    m.cube('tete', 'crane', (-8, 33, -10.5), (16, 12, 13), SKIN, deco={'north': [FACE]})
    m.cube('tete', 'arcade', (-8, 39, -11.5), (16, 2, 1), SKIN_DARK, deco={'north': [border(P_PEAU[0], 'b')]})
    m.cube('tete', 'nez', (-2, 35, -12.5), (4, 4, 2), SKIN, deco={'north': [stamp(['....', '....', '....', 'K..K'], {'K': P_PEAU[0]})]})
    m.bone('machoire', (0, 34, -4), 'tete')
    m.cube('machoire', 'machoire_bas', (-6.5, 30, -11.5), (13, 4, 10), SKIN_DARK, deco={'north': [TEETH]})
    m.cube('machoire', 'defense_droite', (3.5, 33, -12.5), (2, 4, 2), IVORY)
    m.cube('machoire', 'defense_gauche', (-5.5, 33, -12.5), (2, 4, 2), IVORY)
    for side, sx in (('droite', 1), ('gauche', -1)):
        b = m.bone(f'oreille_{side}', (8 * sx, 40, -5), 'tete', rot=(0, -18 * sx, 22 * sx))
        for part, x_in, w, y0, h in (('base', 8, 5, 37, 6), ('milieu', 13, 4, 38, 4), ('pointe', 17, 4, 39, 2)):
            x0 = x_in if sx > 0 else -x_in - w
            m.cube(b, f'oreille_{side}_{part}', (x0, y0, -5.5), (w, h, 1), SKIN,
                   deco={'north': [ear_inner]} if part != 'pointe' else None, share=f'oreille_{part}', mirror=sx < 0)
    m.bone('couronne', (0, 45, -4), 'tete', rot=(-4, 0, 7))
    m.cube('couronne', 'couronne_bandeau', (-6.5, 45, -10.5), (13, 3, 13), GOLD, deco={'*': [crown_band], 'up': [velvet]})
    m.cube('couronne', 'gemme', (-1.5, 45.5, -11), (3, 2, 1), RUBY)
    for i, (x, h, z) in enumerate(((-1, 4, -11), (-6, 3, -11), (4, 3, -11), (-6.5, 3, 1.5), (4.5, 2, 1.5))):
        m.cube('couronne', f'fleuron_{i + 1}', (x, 48, z), (2, h, 1), GOLD, deco={'*': [crown_points]})

    # bras
    for side, sx in (('droit', 1), ('gauche', -1)):
        b = m.bone(f'bras_{side}', (12 * sx, 30, -2), 'torse', rot=(0, 0, (12 if sx > 0 else 6) * sx))
        m.cube(b, f'bras_{side}_peau', (10 if sx > 0 else -17, 21, -5.5), (7, 10, 7), SKIN, share='bras_haut', mirror=sx < 0)
        fa = m.bone(f'avant_bras_{side}', (13.5 * sx, 22, -2), b, rot=(40, 0, 0) if sx > 0 else (18, 0, 0))
        m.cube(fa, f'avant_bras_{side}_peau', (10.5 if sx > 0 else -16.5, 14, -5), (6, 8, 6), SKIN, share='avant_bras', mirror=sx < 0)
        m.cube(fa, f'brassard_{side}', (10 if sx > 0 else -17, 15, -5.5), (7, 6, 7), IRON, inflate=0.3,
               deco={'*': [rivets(P_FER[5], P_FER[0], every=3, rows=('t', 'b'))]}, share='brassard', mirror=sx < 0)
        h = m.bone('main_droite' if sx > 0 else 'main_gauche', (13.5 * sx, 14, -2), fa)
        m.cube(h, f'poing_{side}', (10 if sx > 0 else -17, 8, -6), (7, 6, 8), SKIN, deco={'*': [fingers]}, share='poing', mirror=sx < 0)
    # epaulieres
    m.cube('bras_droit', 'epauliere_droite', (9, 28, -7), (9, 4, 10), IRON2, deco={'*': [border(P_ROUGE[2], 'b')]})
    m.cube('bras_gauche', 'epauliere_gauche', (-22, 26, -9), (13, 7, 14), IRON, deco={'*': [rivets(P_FER[5], P_FER[0], every=4, rows=('b',))]})
    m.cube('bras_gauche', 'epauliere_gauche_lame', (-21, 23, -8), (11, 3, 12), IRON2)
    pt = m.bone('pointes_epauliere', (-16, 33, -2), 'bras_gauche', rot=(0, 0, 18))
    for i, z in enumerate((-7, -2.5, 2)):
        m.cube(pt, f'pointe_{i + 1}', (-17.5 + 1.2 * i, 33, z), (2, 4, 2), STEEL, share='pointe')

    # couperet (manche selon -Z dans le poing ; l'avant-bras plie le fait pointer vers le haut)
    cp = m.bone('couperet', (13.5, 10.5, -2), 'main_droite', rot=(35, 0, 0))
    m.cube(cp, 'manche', (12.5, 9.5, -12), (2, 2, 15), LEATHER, deco={'*': [lambda f: [f.px(x, y, P_CUIR[1]) for y in range(f.h) for x in range(f.w) if (x + y) % 3 == 0]]})
    m.cube(cp, 'pommeau', (12, 9, 3), (3, 3, 2), IRON)
    m.cube(cp, 'lame', (12.5, 0.5, -28), (2, 11, 16), STEEL, deco={'*': [blade_edge]})
    m.cube(cp, 'garde', (12, 8.5, -13), (3, 4, 1), IRON)

    # jambes
    for side, sx in (('droite', 1), ('gauche', -1)):
        foot = 'droit' if sx > 0 else 'gauche'
        lg = m.bone(f'jambe_{side}', (5 * sx, 14, 0), 'bassin')
        m.cube(lg, f'cuisse_{side}', (1.5 if sx > 0 else -8.5, 8, -3.5), (7, 7, 7), CLOTH, share='cuisse', mirror=sx < 0)
        m.cube(lg, f'jambiere_{side}', (1 if sx > 0 else -9, 3, -4), (8, 6, 8), IRON, share='jambiere', mirror=sx < 0,
               deco={'*': [rivets(P_FER[5], P_FER[0])]})
        m.cube(lg, f'genouillere_{side}', (2.5 if sx > 0 else -7.5, 7, -5), (5, 3, 1), IRON2, share='genou', mirror=sx < 0)
        ft = m.bone(f'pied_{foot}', (5 * sx, 3, 0), lg)
        m.cube(ft, f'botte_{side}', (0.5 if sx > 0 else -9.5, 0, -7), (9, 4, 10), LEATHER, share='botte', mirror=sx < 0,
               deco={'sides': [border(P_FER[3], 't')]})
        m.cube(ft, f'coque_{side}', (1 if sx > 0 else -9, 0.5, -7.5), (8, 3, 3), IRON2, share='coque', mirror=sx < 0)

    # cape segmentee
    m.bone('cape_haut', (0, 32, 7), 'torse')
    m.cube('cape_haut', 'cape_haut_tissu', (-11, 21, 6.5), (22, 11, 1), RED, deco={'sides': [border(P_OR[3], 'lr')]})
    m.bone('cape_milieu', (0, 21, 7), 'cape_haut')
    m.cube('cape_milieu', 'cape_milieu_tissu', (-11.5, 11, 6.5), (23, 10, 1), RED, deco={'sides': [border(P_OR[3], 'lr')]})
    m.bone('cape_bas', (0, 11, 7), 'cape_milieu')
    m.cube('cape_bas', 'cape_bas_tissu', (-12, 2, 6.5), (24, 9, 1), RED,
           deco={'sides': [border(P_OR[3], 'lr'), tatter(4, seed=0.7, holes=0.05)]})
    return m


# instant de l'impact de chaque attaque (s) : les degats sont appliques a ce moment en jeu
STRIKES = {'coup_horizontal': 0.68, 'frappe_verticale': 0.8, 'cri_ralliement': 0.85}

COLORS = {'racine': 0, 'bassin': 3, 'torse': 1, 'tete': 2, 'couronne': 5, 'bras_droit': 4, 'bras_gauche': 4,
          'jambe_droite': 6, 'jambe_gauche': 6, 'cape_haut': 0, 'couperet': 7}


# ============================================================================================== animations
# Valeurs en degres / pixels, convention du kit : +X bras pendant vers l'avant (os vertical vers l'arriere),
# +Y tourne vers la gauche de la creature, +Z leve le bras droit sur le cote (penche le torse vers sa gauche).
def animations(m):
    P = ek.key_poses
    L = 'linear'

    # --- repos : respiration lente (2,4 s)
    a = m.anim('repos', 2.4, 'loop')
    P(a, [(0, {}), (1.2, {'torse': (1.5, 0, 0), 'tete': (-2, 3, 0), 'machoire': (-3, 0, 0), 'bras_droit': (0, 0, 2),
                          'bras_gauche': (0, 0, -2), 'avant_bras_droit': (3, 0, 0), 'avant_bras_gauche': (4, 0, 0),
                          'cape_haut': (-2, 0, 0), 'cape_milieu': (-1.5, 0, 0), 'cape_bas': (-2, 0, 0)}), (2.4, {})])
    P(a, [(0, {}), (1.2, {'torse': (0, 0.35, 0), 'tete': (0, 0.15, 0)}), (2.4, {})], 'position')
    a.rot('oreille_droite', [(0, 0, 0, 0), (1.0, 0, 0, 0, L), (1.1, 0, 0, 8, L), (1.25, 0, 0, 0, L), (2.4, 0, 0, 0)])
    a.rot('oreille_gauche', [(0, 0, 0, 0), (1.9, 0, 0, 0, L), (2.0, 0, 0, -8, L), (2.15, 0, 0, 0, L), (2.4, 0, 0, 0)])

    # --- marche lourde (1,2 s) : jambes raides, bassin qui s'enfonce a chaque appui, epaules en opposition
    def walk(name, T, leg, arm, lean, bob, extra=None):
        a = m.anim(name, T, 'loop')
        h, q = T / 2, T / 4
        base = extra or {}

        def pose(sgn, contact):
            p = {'jambe_droite': (leg * sgn, 0, 0), 'jambe_gauche': (-leg * sgn, 0, 0),
                 'pied_droit': (-leg * sgn * 0.9, 0, 0), 'pied_gauche': (leg * sgn * 0.9, 0, 0),
                 'torse': (lean, -4 * sgn, 3 * sgn), 'tete': (3 if contact else -2, 3 * sgn, -2 * sgn),
                 'bras_gauche': (arm * sgn, 0, 0), 'bras_droit': (-arm * 0.5 * sgn, 0, 0),
                 'etendard': (15, 0, 0) if contact else (5, 0, 0),
                 'cape_haut': (-8, 0, 0) if contact else (-4, 0, 0), 'cape_milieu': (-4, 0, 0) if contact else (-10, 0, 0),
                 'cape_bas': (-6, 0, 0) if contact else (-12, 0, 0), 'machoire': (0, 0, 0) if contact else (-4, 0, 0),
                 'couronne': (0, 0, 2 * sgn)}
            for k, v in base.items():
                p[k] = tuple(p.get(k, (0, 0, 0))[i] + v[i] for i in range(3))
            return p
        P(a, [(0, pose(1, True)), (q, pose(0.05, False)), (h, pose(-1, True)), (h + q, pose(-0.05, False)), (T, pose(1, True))])
        P(a, [(0, {'bassin': (0, -bob, 0)}), (q, {'bassin': (0, bob * 0.5, 0)}), (h, {'bassin': (0, -bob, 0)}),
              (h + q, {'bassin': (0, bob * 0.5, 0)}), (T, {'bassin': (0, -bob, 0)})], 'position')
        return a
    walk('marche', 1.2, 22, 18, -4, 1.0)

    # --- coup de couperet horizontal (1,5 s) : armement a droite, balayage de droite a gauche, retour
    a = m.anim('coup_horizontal', 1.5, 'once')
    arme = {'bassin': (0, -12, 0), 'torse': (5, -30, 0), 'tete': (0, 22, 0), 'bras_droit': (75, -95, 0),
            'avant_bras_droit': (-20, 0, 0), 'couperet': (-110, -90, 0), 'bras_gauche': (40, 0, -30),
            'jambe_droite': (-8, 0, 0), 'jambe_gauche': (8, 0, 0), 'cape_haut': (-4, 0, -8), 'machoire': (-6, 0, 0)}
    frappe = {'bassin': (0, 12, 0), 'torse': (-8, 28, 0), 'tete': (-4, -20, 0), 'bras_droit': (90, 40, 0),
              'avant_bras_droit': (-40, 0, 0), 'couperet': (-125, -90, 0), 'bras_gauche': (-20, 0, -20),
              'jambe_droite': (8, 0, 0), 'jambe_gauche': (-6, 0, 0), 'cape_haut': (-12, 0, 14), 'cape_milieu': (-6, 0, 6),
              'machoire': (-16, 0, 0)}
    suite = dict(frappe, torse=(-10, 36, 0), bras_droit=(85, 62, 0), bassin=(0, 14, 0), cape_haut=(-8, 0, 18))
    P(a, [(0, {}), (0.45, arme), (0.55, arme, L), (0.68, frappe, L), (0.8, suite), (1.05, dict(suite, machoire=(-4, 0, 0))), (1.5, {})])
    P(a, [(0, {}), (0.45, {'racine': (0, 1, 0)}), (0.8, {'racine': (0, 1, 0)}), (1.5, {})], 'position')

    # --- frappe verticale (1,7 s) : couperet leve au-dessus de la tete, abattu devant lui
    a = m.anim('frappe_verticale', 1.7, 'once')
    leve = {'torse': (12, 0, 0), 'tete': (8, 0, 0), 'bras_droit': (160, 0, 8), 'avant_bras_droit': (-10, 0, 0),
            'couperet': (-25, 0, 0), 'bras_gauche': (20, 0, -55), 'jambe_droite': (-6, 0, 0), 'jambe_gauche': (6, 0, 0),
            'cape_haut': (-6, 0, 0), 'machoire': (-10, 0, 0), 'oreille_droite': (0, 0, 10), 'oreille_gauche': (0, 0, -10)}
    impact = {'torse': (-26, 0, 0), 'tete': (14, 0, 0), 'bras_droit': (62, 0, 4), 'avant_bras_droit': (-40, 0, 0),
              'couperet': (-72, 0, 0), 'bras_gauche': (35, 0, -25), 'jambe_droite': (10, 0, 6), 'jambe_gauche': (-12, 0, -6), 'pied_droit': (-10, 0, -6), 'pied_gauche': (12, 0, 6),
              'cape_haut': (-16, 0, 0), 'cape_milieu': (-8, 0, 0), 'machoire': (-18, 0, 0), 'etendard': (12, 0, 0)}
    P(a, [(0, {}), (0.55, leve), (0.68, leve, L), (0.8, impact, L), (0.86, dict(impact, torse=(-24, 0, 2))),
          (1.05, impact), (1.7, {})])
    P(a, [(0, {}), (0.55, {'racine': (0, 1, 0)}), (0.68, {'racine': (0, 1, 0)}, L), (0.8, {'torse': (0, -2.5, -2)}, L),
          (1.05, {'torse': (0, -2.5, -2)}), (1.7, {})], 'position')

    # --- cri de ralliement (2,2 s) : se ramasse, puis se redresse bras ecartes, machoire grande ouverte
    a = m.anim('cri_ralliement', 2.2, 'once')
    ramasse = {'torse': (-14, 0, 0), 'tete': (-10, 0, 0), 'bras_droit': (10, 0, -4), 'bras_gauche': (10, 0, 4),
               'avant_bras_droit': (10, 0, 0), 'avant_bras_gauche': (20, 0, 0), 'cape_haut': (-3, 0, 0)}
    cri = {'torse': (14, 0, 0), 'tete': (22, 0, 0), 'machoire': (-28, 0, 0), 'bras_droit': (150, 0, 25),
           'avant_bras_droit': (-40, 0, 0), 'couperet': (-95, 0, 0), 'bras_gauche': (20, 0, -75), 'avant_bras_gauche': (15, 0, 0),
           'oreille_droite': (0, 10, 14), 'oreille_gauche': (0, -10, -14), 'cape_haut': (-14, 0, 0), 'cape_milieu': (-10, 0, 0),
           'cape_bas': (-8, 0, 0), 'couronne': (0, 0, 4), 'etendard': (6, 0, 0)}
    vib = [(0.95 + 0.1 * i, dict(cri, tete=(22, 0, 2.5 if i % 2 else -2.5), torse=(14, 0, 1 if i % 2 else -1)), L) for i in range(6)]
    P(a, [(0, {}), (0.5, ramasse), (0.8, cri)] + vib + [(1.65, cri), (2.2, {})])
    P(a, [(0, {}), (0.5, {'torse': (0, -1.5, 0)}), (0.8, {'racine': (0, 0.5, 0)}), (1.65, {'racine': (0, 0.5, 0)}), (2.2, {})], 'position')

    # --- etourdissement (2 s, en boucle) : tete qui tourne en rond, bras ballants, couperet qui pend
    a = m.anim('etourdi', 2.0, 'loop')
    base = {'bras_droit': (-6, 0, -6), 'bras_gauche': (-4, 0, 6), 'avant_bras_droit': (-30, 0, 0), 'avant_bras_gauche': (-15, 0, 0),
            'couperet': (-28, 0, 0), 'machoire': (-14, 0, 0), 'oreille_droite': (0, 0, -26), 'oreille_gauche': (0, 0, 26),
            'couronne': (0, 0, 10)}

    def dz(t, hx, hz, tz):
        return (t, dict(base, tete=(hx, 0, hz), torse=(-6, 0, tz), cape_haut=(-2, 0, -tz)))
    P(a, [dz(0, 0, 9, 4), dz(0.5, 9, 0, 0), dz(1.0, 0, -9, -4), dz(1.5, -5, 0, 0), dz(2.0, 0, 9, 4)])
    P(a, [(0, {'torse': (0, -0.5, 0)}), (1.0, {'torse': (0, -0.8, 0)}), (2.0, {'torse': (0, -0.5, 0)})], 'position')

    # --- mort (2,6 s, figee sur la derniere pose) : recul, affaissement, chute en avant, couperet et couronne lachés
    a = m.anim('mort', 2.6, 'hold')
    recul = {'torse': (14, 0, 0), 'tete': (22, 0, 0), 'machoire': (-22, 0, 0), 'bras_droit': (-25, 0, 30),
             'bras_gauche': (-15, 0, -40), 'cape_haut': (-6, 0, 0)}
    affaisse = {'torse': (-28, 0, 4), 'tete': (-22, 0, 8), 'machoire': (-10, 0, 0), 'bras_droit': (25, 0, -2),
                'bras_gauche': (20, 0, 2), 'avant_bras_droit': (-30, 0, 0), 'couperet': (-30, 0, 20),
                'jambe_droite': (8, 0, 0), 'jambe_gauche': (-6, 0, 0), 'pied_droit': (-8, 0, 0), 'pied_gauche': (6, 0, 0), 'oreille_droite': (0, 0, -20), 'oreille_gauche': (0, 0, 20)}
    sol = dict(affaisse, racine=(-86, 0, 0), torse=(-6, 0, 3), tete=(38, 0, 8), bras_droit=(170, -20, 30),
               bras_gauche=(165, 20, -30), avant_bras_droit=(-40, 0, 0), avant_bras_gauche=(-10, 0, 0),
               jambe_droite=(-6, 0, 4), jambe_gauche=(-8, 0, -4), cape_haut=(10, 0, 0), cape_milieu=(14, 0, 0),
               couperet=(30, 90, 0), couronne=(0, 0, 70))
    rebond = dict(sol, racine=(-82, 0, 0), tete=(30, 0, 6))
    P(a, [(0, {}), (0.35, recul), (0.9, affaisse), (1.35, sol, L), (1.5, rebond), (1.7, sol), (2.6, sol)])
    P(a, [(0, {}), (0.35, {'racine': (0, 0, 2)}), (0.9, {'racine': (0, -1, 1)}), (1.35, {'racine': (0, 9.5, -2), 'couronne': (0, 0, -3)}, L),
          (1.5, {'racine': (0, 11, -2), 'couronne': (0, 2, -2)}), (1.7, {'racine': (0, 9.5, -2), 'couronne': (6, 5, -2.5)}),
          (2.6, {'racine': (0, 9.5, -2), 'couronne': (6, 5, -2.5)})], 'position')

    # --- rage : posture plus basse et plus large, souffle court, couperet brandi
    a = m.anim('rage_repos', 1.2, 'loop')
    rage = {'torse': (-12, 0, 0), 'tete': (10, 0, 0), 'bras_droit': (25, 0, 22), 'avant_bras_droit': (20, 0, 0),
            'bras_gauche': (10, 0, -28), 'avant_bras_gauche': (25, 0, 0), 'jambe_droite': (0, 0, 6), 'jambe_gauche': (0, 0, -6),
            'pied_droit': (0, 0, -6), 'pied_gauche': (0, 0, 6), 'machoire': (-10, 0, 0), 'oreille_droite': (0, 16, -6),
            'oreille_gauche': (0, -16, 6), 'cape_haut': (-6, 0, 0), 'etendard': (4, 0, 0)}
    souffle = dict(rage, torse=(-9, 0, 0), machoire=(-16, 0, 0), bras_droit=(28, 0, 25), bras_gauche=(12, 0, -31))
    P(a, [(0, rage), (0.6, souffle), (1.2, rage)])
    P(a, [(0, {'torse': (0, -1.2, 0)}), (0.6, {'torse': (0, -0.4, 0)}), (1.2, {'torse': (0, -1.2, 0)})], 'position')
    walk('rage_marche', 0.9, 28, 26, -14, 1.4,
         extra={'bras_droit': (20, 0, 18), 'avant_bras_droit': (20, 0, 0), 'bras_gauche': (0, 0, -18), 'tete': (10, 0, 0),
                'machoire': (-10, 0, 0), 'oreille_droite': (0, 16, -6), 'oreille_gauche': (0, -16, 6)})
