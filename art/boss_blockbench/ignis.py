# -*- coding: utf-8 -*-
"""Ignis, seigneur demon : armure d'obsidienne fissuree de lave, environ 4 blocs (66 px cornes comprises),
grande epee noire au coeur en fusion dans la main droite. Pas d'ailes."""
import math

import entity_kit as ek
from entity_kit import pal, Mat, stamp, rivets, border, blotches, tatter, cracks

KEY = 'ignis'
TITLE = 'Ignis, seigneur demon'
ENTITY_CLASS = 'IgnisEntity'
VISIBLE_BOX = (3, 4.2, 0)
VIEW_SCALE = 5.0
SHEET_SCALE = 2.6
GIF_SCALE = 2.4

P_OBS = pal('#0c0a0f', '#141118', '#1d1923', '#27222f', '#332c3d', '#41384d')
P_LAVE = pal('#5a1004', '#8f1f05', '#c83a08', '#ef6a10', '#ff9a22', '#ffd25a', '#fff2a8')
P_BRONZE = pal('#2e200f', '#4a3518', '#6b4e24', '#8e6a33', '#b38844', '#d2aa5e')
P_ROUGE = pal('#2f0806', '#4b0e09', '#6c160d', '#8f2112', '#b03117', '#c9481f')
LAVA = [P_LAVE[2], P_LAVE[3]]

OBS = Mat(P_OBS, level=0.5, noise=0.3, scale=3, rim=0.3, decos=[cracks(LAVA, n=1.6, length=(3, 8), core=P_LAVE[5])])
OBS_PLAIN = Mat(P_OBS, level=0.45, noise=0.3, scale=3, rim=0.25, decos=[cracks(LAVA, n=0.6, length=(2, 5), core=P_LAVE[4])])
BRONZE = Mat(P_BRONZE, level=0.55, noise=0.25, scale=2, rim=0.3, decos=[blotches(0.4, [P_BRONZE[1]], (0.5, 1))])
LAVA_MAT = Mat(P_LAVE, level=0.62, noise=0.25, scale=1.5, grad=0.35, glow=True)
CLOTH = Mat(P_ROUGE, level=0.5, noise=0.3, scale=(1.2, 5), grad=0.15)

E = {'K': (6, 4, 8), 'E': P_LAVE[6], 'e': P_LAVE[5], 'o': P_LAVE[4], 'O': P_LAVE[3], 'b': P_BRONZE[4], 'B': P_BRONZE[2]}

VISOR = stamp([
    '..........',
    '..........',
    '.KK....KK.',
    '.eEK..KEe.',
    '..eE..Ee..',
    '...K..K...',
    '..K.KK.K..',
    '.KoKooKoK.',
    '.KoKooKoK.',
    '..KKKKKK..',
], E, glow='Eeo')
CHIN = stamp(['KoKooKoK', 'KoKooKoK', '.KKKKKK.', '........'], E, glow='o', ay='t')


def glow_tip(rows=2):
    """Extremite incandescente (griffes, cornes) : les 'rows' dernieres lignes des faces laterales, toute la face du bout."""
    def d(f):
        if f.side:
            for y in range(max(0, f.h - rows), f.h):
                for x in range(f.w):
                    f.px(x, y, P_LAVE[5] if y == f.h - 1 else P_LAVE[3], glow=True)
        elif f.name == 'down':
            for y in range(f.h):
                for x in range(f.w):
                    f.px(x, y, P_LAVE[6], glow=True)
    return d


def tip_x(f):
    """Bout de corne : face exterieure et faces laterales vers l'extremite en lave."""
    if f.name in ('east', 'west'):
        for y in range(f.h):
            for x in range(f.w):
                f.px(x, y, P_LAVE[4] if (x + y) % 3 else P_LAVE[5], glow=True)
    elif f.w > 1:
        for y in range(f.h):
            for x in range(f.w // 2):
                xx = x if f.name in ('north', 'up', 'down') else f.w - 1 - x
                f.px(xx, y, P_LAVE[3], glow=True)


def molten_core(f):
    """Lame : coeur en fusion au centre des faces plates, tranchants gris."""
    if f.name in ('east', 'west'):
        c = f.h // 2
        for x in range(f.w):
            f.px(x, 0, P_OBS[5])
            f.px(x, f.h - 1, P_OBS[5])
            f.px(x, c - 1, P_LAVE[3], glow=True)
            f.px(x, c, P_LAVE[5] if x % 5 else P_LAVE[6], glow=True)
            if f.rng.random() < 0.18:
                yy = c - 2 if f.rng.random() < 0.5 else c + 1
                f.px(x, yy, P_LAVE[2], glow=True)


def core_glow(f):
    """Noyau : croix blanche-jaune sur fond de lave."""
    cx, cy = f.w // 2, f.h // 2
    for y in range(f.h):
        for x in range(f.w):
            d = abs(x + 0.5 - f.w / 2) + abs(y + 0.5 - f.h / 2)
            col = P_LAVE[6] if d < 1.5 else P_LAVE[5] if d < 2.6 else P_LAVE[4] if d < 3.6 else P_LAVE[3]
            f.px(x, y, col, glow=True)


def chest_lines(f):
    """Plastron : arete centrale et lignes de lave qui convergent vers le noyau."""
    if f.name == 'north':
        for y in range(f.h):
            f.px(0 if f.cube.frm[0] > 0 else f.w - 1, y, P_LAVE[3], glow=True)
        for x in range(f.w):
            f.px(x, f.h - 1, P_BRONZE[3])


def abs_lines(f):
    if f.side:
        for y in range(1, f.h, 2):
            for x in range(f.w):
                f.px(x, y, P_OBS[0])


def tabard_rune(f):
    if f.name == 'north':
        stamp(['..b..', '.bOb.', 'bOoOb', '.bOb.', '..b..', '..b..', '..b..'], E, ay='t', dy=3, glow='Oo')(f)


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))
    bs = m.bone('bassin', (0, 30, 0), 'racine')
    m.cube(bs, 'bassin_armure', (-11, 26, -7), (22, 7, 14), OBS)
    m.cube(bs, 'ceinture', (-12, 31, -8), (24, 3, 16), BRONZE, deco={'*': [rivets(P_BRONZE[5], P_BRONZE[0], every=4, rows=('t',))]})
    m.cube(bs, 'boucle', (-3, 29.5, -9), (6, 5, 1), OBS_PLAIN, deco={'north': [stamp(['.oo.', 'oEEo', 'oEEo', '.oo.'], E, glow='oE')]})
    pg = m.bone('pagne', (0, 31, -8.5), bs)
    m.cube(pg, 'pagne_tissu', (-5, 12, -9.5), (10, 19, 1), CLOTH,
           deco={'sides': [border(P_BRONZE[3], 'lr'), tabard_rune, tatter(3, seed=0.8)]})
    pa = m.bone('pagne_arriere', (0, 31, 8.5), bs)
    m.cube(pa, 'pagne_arriere_tissu', (-6, 14, 8), (12, 17, 1), CLOTH, deco={'sides': [border(P_BRONZE[2], 'lr'), tatter(3, seed=2.1)]})

    t = m.bone('torse', (0, 33, 0), bs)
    m.cube(t, 'taille', (-10, 33, -6), (20, 6, 12), OBS_PLAIN, deco={'*': [abs_lines]})
    m.cube(t, 'poitrine', (-13, 39, -8), (26, 12, 16), OBS)
    m.cube(t, 'pectoral_droit', (2, 41, -9), (9, 8, 1), OBS_PLAIN, deco={'*': [chest_lines]})
    m.cube(t, 'pectoral_gauche', (-11, 41, -9), (9, 8, 1), OBS_PLAIN, deco={'*': [chest_lines]})
    m.cube(t, 'gorgerin', (-6, 49, -6), (12, 3, 10), OBS_PLAIN, deco={'sides': [border(P_BRONZE[3], 'b')]})
    m.cube(t, 'cadre_noyau', (-3, 40, -9), (6, 1, 1), BRONZE)
    for i, y in enumerate((41, 45)):
        m.cube(t, f'pointe_dos_{i + 1}', (-1.5, y, 7.5), (3, 3, 4), OBS_PLAIN, rot=(-30, 0, 0), origin=(0, y, 8))
    n = m.bone('noyau', (0, 45, -8.5), t)
    m.cube(n, 'noyau_coeur', (-2, 41, -9.5), (4, 8, 2), LAVA_MAT, deco={'*': [core_glow]})
    m.cube(n, 'noyau_branches', (-4, 44, -9.2), (8, 2, 1), LAVA_MAT)

    # tete et cornes (chaine d'os : la courbure vient des rotations de repos)
    h = m.bone('tete', (0, 52, -1), t)
    m.cube(h, 'casque', (-5, 52, -6.5), (10, 11, 10), OBS, deco={'north': [VISOR]})
    m.cube(h, 'mentonniere', (-4, 50, -7.5), (8, 4, 2), OBS_PLAIN, deco={'north': [CHIN]})
    m.cube(h, 'crete', (-1, 63, -6), (2, 2, 9), OBS_PLAIN)
    m.cube(h, 'arcade', (-5, 60, -7.5), (10, 1, 1), OBS_PLAIN)
    cs = m.bone('cornes', (0, 59, -2), h)
    for side, sx in (('droite', 1), ('gauche', -1)):
        segs = [(6, 5, 5, 10), (5, 4, 4, 32), (4, 4, 3, 32), (4, 3, 2, 28)]
        parent, x = cs, 4.5
        for k, (ln, hh, dd, rz) in enumerate(segs):
            b = m.bone(f'corne_{side}' + ('' if k == 0 else f'_{k + 1}'), (x * sx, 59, -2), parent, rot=(-6 if k == 3 else 0, 0, rz * sx))
            x0 = x if sx > 0 else -x - ln
            m.cube(b, f'corne_{side}_{k + 1}', (x0, 59 - hh / 2, -2 - dd / 2), (ln, hh, dd), OBS_PLAIN,
                   deco={'*': [tip_x]} if k == 3 else None, share=f'corne_{k}', mirror=sx < 0)
            parent, x = b, x + ln - 0.5

    # bras, epaulieres (os separes, accroches au bras : l'epaule reste libre), mains griffues
    for side, sx in (('droit', 1), ('gauche', -1)):
        hand = 'main_droite' if sx > 0 else 'main_gauche'
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        b = m.bone(f'bras_{side}', (14 * sx, 47, 0), t, rot=(0, 0, 6 * sx))
        m.cube(b, f'bras_{side}_armure', (xo(11, 8), 36, -4), (8, 12, 8), OBS, share='bras_haut', mirror=sx < 0)
        ep = m.bone(f'epauliere_{"droite" if sx > 0 else "gauche"}', (16 * sx, 50, 0), b)
        m.cube(ep, f'epauliere_{side}_plaque', (xo(9.5, 14), 46, -8.5), (14, 7, 17), OBS, share='epaul', mirror=sx < 0,
               deco={'sides': [border(P_BRONZE[3], 'b')]})
        m.cube(ep, f'epauliere_{side}_lame', (xo(11, 12), 43, -7.5), (12, 3, 15), OBS_PLAIN, share='epaul_lame', mirror=sx < 0,
               deco={'sides': [border(P_BRONZE[2], 'b')]})
        for k, (z, hh) in enumerate(((-5, 6), (1, 5))):
            pk = m.bone(f'pique_{side}_{k + 1}', (16.5 * sx, 53, z + 1.5), ep, rot=(0, 0, -20 * sx))
            m.cube(pk, f'pique_{side}_{k + 1}_base', (xo(15, 3), 53, z), (3, hh, 3), OBS_PLAIN, share=f'pique_{k}', mirror=sx < 0)
            m.cube(pk, f'pique_{side}_{k + 1}_bout', (xo(15.5, 2), 53 + hh, z + 0.5), (2, 3, 2), OBS_PLAIN, share='pique_bout', mirror=sx < 0,
                   deco={'*': [glow_tip(1)]})
        fa = m.bone(f'avant_bras_{side}', (16 * sx, 37, 0), b, rot=(22, 0, 0) if sx > 0 else (12, 0, 4))
        m.cube(fa, f'gantelet_{side}', (xo(11.5, 9), 26, -4.5), (9, 12, 9), OBS, share='gantelet', mirror=sx < 0,
               deco={'sides': [border(P_BRONZE[3], 't')]})
        m.cube(fa, f'aileron_{side}', (xo(20, 2), 28, -1), (2, 8, 3), OBS_PLAIN, share='aileron', mirror=sx < 0)
        hd = m.bone(hand, (16 * sx, 27, 0), fa)
        m.cube(hd, f'poing_{side}', (xo(12, 8), 20, -4), (8, 7, 8), OBS_PLAIN, share='poing', mirror=sx < 0)
        for k in range(3):
            m.cube(hd, f'griffe_{side}_{k + 1}', (xo(12.5 + 2.5 * k, 2), 16, -4.5), (2, 5, 2), OBS_PLAIN, share='griffe', mirror=sx < 0,
                   deco={'*': [glow_tip(2)]}, rot=(-25, 0, 0) if sx > 0 else (-10, 0, (k - 1) * 12 * sx), origin=(xo(12.5 + 2.5 * k, 2) + 1, 20, -3.5))

    # epee (manche selon -Z dans le poing)
    ep = m.bone('epee', (16, 23.5, -0.5), 'main_droite', rot=(-48, 0, 0))
    m.cube(ep, 'poignee', (15, 22.5, -6), (2, 2, 12), Mat(P_BRONZE, level=0.35, noise=0.2, scale=1),
           deco={'*': [lambda f: [f.px(x, y, P_BRONZE[1]) for y in range(f.h) for x in range(f.w) if (x + y) % 2 == 0]]})
    m.cube(ep, 'pommeau', (14.5, 22, 6), (3, 3, 3), OBS_PLAIN, deco={'*': [glow_tip(1)]})
    m.cube(ep, 'garde', (14, 16.5, -8.5), (4, 14, 2), OBS, deco={'sides': [border(P_BRONZE[3], 'tb')]})
    m.cube(ep, 'garde_pointes', (14.5, 15, -9), (3, 17, 1), OBS_PLAIN, deco={'*': [glow_tip(1)]})
    m.cube(ep, 'lame', (15, 19.5, -48.5), (2, 8, 40), OBS_PLAIN, deco={'*': [molten_core]})
    m.cube(ep, 'pointe', (15, 21, -52.5), (2, 5, 4), OBS_PLAIN, deco={'*': [molten_core]})

    # jambes : cuisse, tibia (genou articule), pied griffu
    for side, sx in (('droite', 1), ('gauche', -1)):
        foot = 'droit' if sx > 0 else 'gauche'
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        lg = m.bone(f'jambe_{side}', (6.5 * sx, 28, 0), bs)
        m.cube(lg, f'cuissard_{side}', (xo(2, 9), 18, -4.5), (9, 11, 9), OBS, share='cuissard', mirror=sx < 0,
               deco={'sides': [border(P_BRONZE[3], 't')]})
        tb = m.bone(f'tibia_{foot}', (6.5 * sx, 18, 0), lg)
        m.cube(tb, f'jambiere_{side}', (xo(2.5, 8), 5, -4), (8, 13, 8), OBS, share='jambiere', mirror=sx < 0)
        m.cube(tb, f'genouillere_{side}', (xo(3.5, 6), 14, -5.5), (6, 5, 2), BRONZE, share='genou', mirror=sx < 0,
               deco={'north': [stamp(['..o...', '.oOo..'], E, glow='oO', ay='b', dy=-1)]})
        pd = m.bone(f'pied_{foot}', (6.5 * sx, 5, 0), tb)
        m.cube(pd, f'soleret_{side}', (xo(1.5, 10), 0, -8), (10, 5, 12), OBS, share='soleret', mirror=sx < 0)
        for k in range(3):
            m.cube(pd, f'orteil_{side}_{k + 1}', (xo(2 + 3.2 * k, 2), 0, -11), (2, 3, 3), OBS_PLAIN, share='orteil', mirror=sx < 0,
                   deco={'north': [glow_tip(1)]})
        m.cube(pd, f'eperon_{side}', (xo(5.5, 2), 1, 4), (2, 2, 3), OBS_PLAIN, share='eperon', mirror=sx < 0)
    return m


# instant de l'impact de chaque attaque (s) : les degats sont appliques a ce moment en jeu
STRIKES = {'balayage': 0.86, 'frappe_sol': 1.0, 'souffle_feu': 1.0, 'rugissement': 0.85}

COLORS = {'racine': 0, 'bassin': 3, 'torse': 1, 'tete': 2, 'cornes': 5, 'bras_droit': 4, 'bras_gauche': 4,
          'jambe_droite': 6, 'jambe_gauche': 6, 'epee': 7, 'noyau': 8}


# ============================================================================================== animations
def legs(r, l):
    """r, l : (cuisse, tibia, pied) en degres autour de X pour la jambe droite et la gauche."""
    return {'jambe_droite': (r[0], 0, 0), 'tibia_droit': (r[1], 0, 0), 'pied_droit': (r[2], 0, 0),
            'jambe_gauche': (l[0], 0, 0), 'tibia_gauche': (l[1], 0, 0), 'pied_gauche': (l[2], 0, 0)}


def crouch(drop, spread=0.0, thigh=10.0, shin=13.0):
    """Flexion des genoux qui abaisse la hanche de 'drop' px en gardant le pied sous la hanche et a plat
    (a placer avec un deplacement du bassin de -drop). spread : jambes ecartees (degres)."""
    best = (0.0, 0.0)
    for i in range(0, 900):
        a = math.radians(i / 10.0)
        sb = thigh * math.sin(a) / shin
        if sb >= 1:
            break
        b = math.asin(sb)
        d = thigh * (1 - math.cos(a)) + shin * (1 - math.cos(b))
        if d >= drop:
            best = (math.degrees(a), math.degrees(b))
            break
    a, b = best
    p = legs((a, -(a + b), b), (a, -(a + b), b))
    if spread:
        p['jambe_droite'] = (a, 0, spread)
        p['jambe_gauche'] = (a, 0, -spread)
        p['pied_droit'] = (b, 0, -spread)
        p['pied_gauche'] = (b, 0, spread)
    return p


def M(*dicts):
    """Somme de poses (os absents = 0)."""
    out = {}
    for d in dicts:
        for k, v in d.items():
            out[k] = tuple(out.get(k, (0, 0, 0))[i] + v[i] for i in range(3))
    return out


def SC(v):
    return (v, v, v)


def animations(m):
    P = ek.key_poses
    L = 'linear'

    # --- repos menacant (2,4 s) : respiration lourde, tete qui balaie la zone, griffes qui se crispent
    a = m.anim('repos', 2.4, 'loop')
    P(a, [(0, {}), (0.6, {'tete': (2, 8, 0), 'main_gauche': (8, 0, 0)}),
          (1.2, {'torse': (2.5, 0, 0), 'tete': (-2, 0, 0), 'bras_droit': (0, 0, 2), 'bras_gauche': (0, 0, -3)}),
          (1.8, {'tete': (2, -8, 0), 'main_gauche': (-6, 0, 0)}), (2.4, {})])
    P(a, [(0, {}), (1.2, {'torse': (0, 0.6, 0), 'epauliere_droite': (0, 0.4, 0), 'epauliere_gauche': (0, 0.4, 0)}), (2.4, {})], 'position')
    P(a, [(0, {}), (0.6, {'noyau': SC(1.15)}), (1.2, {}), (1.8, {'noyau': SC(1.15)}), (2.4, {})], 'scale')

    # --- marche lourde (1,6 s) : genoux plies, pas ecrases, epaules en opposition
    a = m.anim('marche', 1.6, 'loop')
    cyc = [(25, -5, -20), (0, 0, 0), (-22, -10, 17), (10, -45, 20)]       # contact, appui, poussee, passage

    def wpose(i):
        r, l = cyc[i % 4], cyc[(i + 2) % 4]
        side = {0: 1, 2: -1}.get(i % 4, 0)
        contact = i % 2 == 0
        p = legs(r, l)
        p.update({'torse': (-6, -5 * side, 3 * side), 'tete': (4 if contact else -2, 0, 0),
                  'bras_gauche': (18 * side, 0, 0), 'bras_droit': (-8 * side, 0, 0),
                  'pagne': (16 if contact else 6, 0, 0), 'pagne_arriere': (-10 if contact else -4, 0, 0),
                  'avant_bras_droit': (12, 0, 0), 'epee': (30, 0, 0)})
        return p
    P(a, [(0.4 * i, wpose(i)) for i in range(5)])
    P(a, [(0.4 * i, {'bassin': (0, -1.2 if i % 2 == 0 else 0.6, 0)}) for i in range(5)], 'position')
    P(a, [(0, {}), (0.4, {'noyau': SC(1.1)}), (0.8, {}), (1.2, {'noyau': SC(1.1)}), (1.6, {})], 'scale')

    # --- balayage a l'epee (1,9 s) : armement a droite, balayage horizontal, recuperation
    a = m.anim('balayage', 1.9, 'once')
    base = crouch(1.6, spread=7)
    arme = M(base, {'torse': (5, -35, 0), 'tete': (0, 26, 0), 'bras_droit': (70, -100, 0), 'avant_bras_droit': (-10, 0, 0),
                    'epee': (-30, -90, 0), 'bras_gauche': (35, 0, -25), 'pagne': (6, 0, 0)})
    coup = M(base, {'bassin': (0, 10, 0), 'torse': (-8, 24, 0), 'tete': (-4, -20, 0), 'bras_droit': (90, 50, 0),
                    'avant_bras_droit': (-22, 0, 0), 'epee': (-42, -90, 0), 'bras_gauche': (-15, 0, -25), 'pagne': (10, 0, 6)})
    suite = M(coup, {'torse': (0, 8, 0), 'bras_droit': (-4, 22, 0)})
    P(a, [(0, {}), (0.6, arme), (0.72, arme, L), (0.86, coup, L), (1.05, suite), (1.3, suite), (1.9, {})])
    P(a, [(0, {}), (0.6, {'bassin': (0, -1.6, 0)}), (1.3, {'bassin': (0, -1.6, 0)}), (1.9, {})], 'position')
    P(a, [(0, {}), (0.72, {'noyau': SC(1.2)}), (0.86, {'noyau': SC(1.4)}, L), (1.3, {'noyau': SC(1.1)}), (1.9, {})], 'scale')

    # --- frappe au sol (2,3 s) : epee levee a deux mains, abattue dans le sol, genoux plies a l'impact
    a = m.anim('frappe_sol', 2.3, 'once')
    leve = {'torse': (12, 0, 0), 'tete': (10, 0, 0), 'bras_droit': (165, 0, -14), 'avant_bras_droit': (10, 0, 0), 'epee': (51, 0, 0),
            'bras_gauche': (165, 0, 16), 'avant_bras_gauche': (20, 0, 0), 'pagne': (-4, 0, 0)}
    impact = M(crouch(5, spread=6), {'torse': (-30, 0, 0), 'tete': (18, 0, 0), 'bras_droit': (58, 0, -10), 'avant_bras_droit': (-22, 0, 0),
                                      'epee': (-4, 0, 0), 'bras_gauche': (58, 0, 12), 'avant_bras_gauche': (-12, 0, 0), 'pagne': (24, 0, 0),
                                      'pagne_arriere': (-14, 0, 0)})
    tremble = [(1.08 + 0.08 * i, M(impact, {'torse': (0, 0, 1.5 if i % 2 else -1.5)}), L) for i in range(4)]
    P(a, [(0, {}), (0.7, leve), (0.85, leve, L), (1.0, impact, L)] + tremble + [(1.5, impact), (2.3, {})])
    P(a, [(0, {}), (0.7, {'racine': (0, 1, 0)}), (0.85, {'racine': (0, 1, 0)}, L), (1.0, {'bassin': (0, -5, -2)}, L),
          (1.5, {'bassin': (0, -5, -2)}), (2.3, {})], 'position')
    P(a, [(0, {}), (0.85, {'noyau': SC(1.3)}), (1.0, {'noyau': SC(1.6)}, L), (1.5, {'noyau': SC(1.2)}), (2.3, {})], 'scale')

    # --- souffle de feu (2,8 s) : inspiration (le noyau gonfle), puis souffle tete projetee en avant (flammes de 1,0 a 2,0 s)
    a = m.anim('souffle_feu', 2.8, 'once')
    inspire = {'torse': (14, 0, 0), 'tete': (24, 0, 0), 'bras_droit': (-20, 0, 30), 'bras_gauche': (-20, 0, -34),
               'avant_bras_gauche': (20, 0, 0), 'pagne': (-6, 0, 0)}
    souffle = M(crouch(1), {'torse': (-16, 0, 0), 'tete': (-20, 0, 0), 'bras_droit': (18, 0, 24), 'bras_gauche': (24, 0, -28),
                             'avant_bras_gauche': (25, 0, 0), 'main_gauche': (-20, 0, 0), 'pagne': (12, 0, 0)})
    vib = [(1.1 + 0.15 * i, M(souffle, {'tete': (0, 3 if i % 2 else -3, 1 if i % 2 else -1)}), L) for i in range(6)]
    P(a, [(0, {}), (0.8, inspire), (0.9, inspire, L), (1.0, souffle, L)] + vib + [(2.0, souffle), (2.8, {})])
    P(a, [(0, {}), (0.8, {'epauliere_droite': (0, 1, 0), 'epauliere_gauche': (0, 1, 0)}), (1.0, {'bassin': (0, -1, 0)}, L), (2.0, {'bassin': (0, -1, 0)}), (2.8, {})], 'position')
    P(a, [(0, {}), (0.8, {'noyau': SC(1.45)}), (1.0, {'noyau': SC(1.2)}, L), (1.5, {'noyau': SC(1.35)}), (2.0, {'noyau': SC(1.1)}), (2.8, {})], 'scale')

    # --- rugissement de rage (2,4 s) : se ramasse, puis se deploie bras ecartes, tete levee, noyau au maximum
    a = m.anim('rugissement', 2.4, 'once')
    ramasse = M(crouch(2), {'torse': (-18, 0, 0), 'tete': (-15, 0, 0), 'bras_droit': (30, 0, -8), 'avant_bras_droit': (30, 0, 0),
                             'bras_gauche': (30, 0, 8), 'avant_bras_gauche': (40, 0, 0)})
    rugit = M(crouch(0.8, spread=6), {'torse': (16, 0, 0), 'tete': (26, 0, 0), 'bras_droit': (40, 0, 68), 'avant_bras_droit': (-10, 0, 0),
                                    'bras_gauche': (40, 0, -72), 'avant_bras_gauche': (-5, 0, 0), 'main_gauche': (-30, 0, 0),
                                    'pagne': (-8, 0, 0), 'pagne_arriere': (8, 0, 0)})
    shake = [(0.95 + 0.1 * i, M(rugit, {'tete': (0, 0, 2.5 if i % 2 else -2.5), 'torse': (0, 0, 1 if i % 2 else -1)}), L) for i in range(9)]
    P(a, [(0, {}), (0.5, ramasse), (0.85, rugit, L)] + shake + [(1.85, rugit), (2.4, {})])
    up = {'epauliere_droite': (0, 1.5, 0), 'epauliere_gauche': (0, 1.5, 0)}
    P(a, [(0, {}), (0.5, {'bassin': (0, -2, 0)}), (0.85, M(up, {'bassin': (0, -0.8, 0)})), (1.85, M(up, {'bassin': (0, -0.8, 0)})), (2.4, {})], 'position')
    P(a, [(0, {}), (0.5, {'noyau': SC(0.9)}), (0.85, {'noyau': SC(1.6)}, L), (1.85, {'noyau': SC(1.5)}), (2.4, {})], 'scale')

    # --- mort (4 s, figee) : touche, tombe a genoux, vacille, puis s'effondre en avant
    a = m.anim('mort', 4.0, 'hold')
    touche = {'torse': (14, 0, -4), 'tete': (18, 0, 6), 'bras_droit': (-10, 0, 26), 'bras_gauche': (-15, 0, -22)}
    genoux = M(legs((0, -90, -80), (0, -90, -80)), {'torse': (-14, 0, 3), 'tete': (-22, 0, 6), 'bras_droit': (-4, 0, 4),
                                                 'avant_bras_droit': (-15, 0, 0), 'epee': (74, 0, 0), 'bras_gauche': (-2, 0, -2),
                                                 'pagne': (60, 0, 0), 'pagne_arriere': (-70, 0, 0)})
    vacille = M(genoux, {'torse': (-6, 0, -5), 'tete': (-6, 0, -4)})
    sol = M(genoux, legs((-6, 75, 60), (6, 80, 60)), {'racine': (-84, 0, 0), 'torse': (8, 0, 0), 'tete': (42, 0, 0), 'bras_droit': (168, 0, 20), 'bras_gauche': (158, 0, -20),
                     'avant_bras_droit': (-10, 0, 0), 'epee': (46, 75, 90), 'pagne': (-60, 0, 0), 'pagne_arriere': (70, 0, 0)})
    rebond = M(sol, {'racine': (4, 0, 0)})
    P(a, [(0, {}), (0.35, touche), (0.75, M(crouch(7), {'torse': (-4, 0, 0), 'tete': (-6, 0, 3), 'bras_droit': (-6, 0, 12), 'epee': (45, 0, 0), 'bras_gauche': (-8, 0, -10), 'pagne': (25, 0, 0), 'pagne_arriere': (-20, 0, 0)})), (0.93, M(genoux, legs((10, 15, 10), (10, 15, 10)), {'epee': (-10, 0, 0)})), (1.1, genoux), (1.3, M(genoux, {'torse': (4, 0, 0)})), (1.8, vacille), (2.1, vacille),
          (2.6, sol, L), (2.75, rebond), (2.95, sol), (4.0, sol)])
    a.rot('epee', [(2.35, 105, 0, 90, L)])        # l'epee pivote vers l'exterieur pendant la chute (evite le sol)
    a.rot('pagne', [(2.35, -65, 0, 0, L)])        # le pagne se replie sous le corps pendant la chute
    kneel = {'bassin': (0, -14, 0)}
    P(a, [(0, {}), (0.35, {'racine': (0, 0, 1.5)}), (0.75, {'bassin': (0, -7, 0), 'racine': (0, 0, 0.8)}), (0.93, {'bassin': (0, -11.5, 0), 'racine': (0, 0, 0.4)}), (1.1, kneel), (2.1, kneel),
          (2.6, M(kneel, {'racine': (0, 10, 0), 'epee': (0, 0.6, 2.6)}), L), (2.75, M(kneel, {'racine': (0, 11, 0), 'epee': (0, 0.6, 2.6)})),
          (2.95, M(kneel, {'racine': (0, 10, 0), 'epee': (0, 0.6, 2.6)})), (4.0, M(kneel, {'racine': (0, 10, 0), 'epee': (0, 0.6, 2.6)}))], 'position')
    P(a, [(0, {}), (0.35, {'noyau': SC(1.5)}), (0.5, {'noyau': SC(0.8)}), (0.7, {'noyau': SC(1.2)}), (1.1, {'noyau': SC(0.8)}),
          (2.6, {'noyau': SC(0.6)}), (4.0, {'noyau': SC(0.5)})], 'scale')
