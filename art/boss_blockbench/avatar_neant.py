# -*- coding: utf-8 -*-
"""Avatar du neant : entite en levitation faite de fragments d'armure noire et violette (environ 4 blocs couronne
comprise). Masque sans visage fendu de violet, couronne brisee flottante, epaulieres detachees, cage thoracique
ouverte autour d'un noyau suspendu, bas du corps en fragments effiles, quelques eclats en orbite. Sans arme."""
import math

import numpy as np

import entity_kit as ek
from entity_kit import pal, Mat, stamp, border, cracks

KEY = 'avatar_neant'
TITLE = 'Avatar du neant'
ENTITY_CLASS = 'AvatarNeantEntity'
VISIBLE_BOX = (2.4, 4.2, 0)
VIEW_SCALE = 5.0
SHEET_SCALE = 2.6
GIF_SCALE = 2.4

P_ARM = pal('#0d0b12', '#15121c', '#1e1a28', '#282334', '#332d42', '#423a54')
P_BORD = pal('#3a3549', '#4b4560', '#5f5875', '#756d8c')
P_VIO = pal('#2e0f5c', '#4b1a92', '#6a2bc9', '#8f4cf0', '#b98bff', '#e6d4ff')
GLOW = [P_VIO[2], P_VIO[3]]


def edge(f):
    """Liseres gris-violet sur les aretes des plaques d'armure."""
    border(P_BORD[1], 'tl')(f)
    border(P_ARM[0], 'br')(f)


def slit(width=1, core=True):
    """Fente lumineuse verticale au centre de la face avant (masque, avant-bras, plaques)."""
    def d(f):
        if f.name != 'north':
            return
        c = f.w // 2
        for y in range(1, f.h - 1):
            for k in range(width):
                f.px(c - width // 2 + k, y, P_VIO[4] if (core and 2 < y < f.h - 3) else P_VIO[3], glow=True)
    return d


def vline(f):
    """Ligne d'energie le long des faces laterales (membres segmentes)."""
    if f.side:
        c = f.w // 2
        for y in range(1, f.h - 1):
            f.px(c, y, P_VIO[3], glow=True)


def rune_ring(f):
    """Rune circulaire des epaulieres (face exterieure)."""
    if f.name in ('east', 'west') and f.w >= 7 and f.h >= 5:
        stamp(['.vvv.', 'v...v', 'v.V.v', 'v...v', '.vvv.'], {'v': P_VIO[3], 'V': P_VIO[5]}, glow='vV')(f)


ARM = Mat(P_ARM, level=0.5, noise=0.3, scale=3, rim=0.2, decos=[edge, cracks([P_VIO[1], P_VIO[2]], n=0.5, length=(2, 5), glow=True)])
ARM_PLAIN = Mat(P_ARM, level=0.45, noise=0.3, scale=3, rim=0.2, decos=[edge])
VIOLET = Mat(P_VIO, level=0.6, noise=0.2, scale=1.5, grad=0.4, glow=True)


def core_face(f):
    cx, cy = (f.w - 1) / 2, (f.h - 1) / 2
    for y in range(f.h):
        for x in range(f.w):
            d = max(abs(x - cx), abs(y - cy)) / max(1, f.w / 2)
            col = P_VIO[5] if d < 0.3 else P_VIO[4] if d < 0.6 else P_VIO[3]
            f.px(x, y, col, glow=True)


# fragments du bas du corps : (nom, centre, taille, rotation de repos)
BAS = [('bas_1', (0, 23, 0), (10, 6, 8), (0, 0, 0)), ('bas_2', (0, 15.5, 0), (7, 7, 6), (0, 12, 0)),
       ('bas_3', (0, 9, 0), (4, 5, 4), (0, -20, 6)), ('bas_4', (0, 4.5, 0), (2, 3, 2), (0, 30, 0))]
# eclats en orbite : (nom, centre, taille)
ECLATS = [('eclat_1', (-17, 44, -6), 3), ('eclat_2', (18, 47, -3), 3), ('eclat_3', (-19, 30, 4), 2),
          ('eclat_4', (17, 28, 6), 2), ('eclat_5', (-8, 22, -12), 2), ('eclat_6', (9, 56, 8), 2)]
# segments de couronne : (nom, centre, taille, rotation)
COURONNE = [('couronne_centre', (0, 66, -2.5), (2, 7, 1), (0, 0, 0)), ('couronne_droite', (5, 64.5, -1.5), (2, 5, 1), (0, -25, -16)),
            ('couronne_gauche', (-5, 64, -1.5), (2, 5, 1), (0, 25, 12)), ('couronne_arriere_droite', (4, 63.5, 3), (1, 4, 1), (0, 0, -20)),
            ('couronne_arriere_gauche', (-3.5, 64, 3.5), (1, 4, 1), (0, 0, 24)), ('couronne_anneau', (0, 62, 0), (8, 1, 7), (0, 0, 6))]


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))
    t = m.bone('torse', (0, 30, 0), 'racine')
    m.cube(t, 'colonne', (-2.5, 26, 3), (5, 18, 3), ARM, deco={'south': [slit(1, False)]})
    m.cube(t, 'taille', (-4, 26, -3), (8, 4, 6), ARM_PLAIN)
    m.cube(t, 'clavicule', (-11, 41, -4.5), (22, 3, 9), ARM, deco={'sides': [vline]})
    m.cube(t, 'gorgerin', (-3.5, 43, -3), (7, 2, 6), ARM_PLAIN)
    for side, sx in (('droite', 1), ('gauche', -1)):
        cg = m.bone(f'cage_{side}', (9 * sx, 36, 2), t)
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        m.cube(cg, f'cage_{side}_flanc', (xo(7, 3), 30, -5), (3, 11, 9), ARM, share='cage_flanc', mirror=sx < 0)
        for k, y in enumerate((38, 34, 30.5)):
            m.cube(cg, f'cote_{side}_{k + 1}', (xo(3, 5), y, -6), (5, 2, 2), ARM_PLAIN, share=f'cote_{k}', mirror=sx < 0,
                   deco={'north': [lambda f: f.px(0 if f.cube.frm[0] > 0 else f.w - 1, 0, P_VIO[3], glow=True)]})
        m.cube(cg, f'cage_{side}_dos', (xo(3, 5), 29, 2), (5, 12, 2), ARM_PLAIN, share='cage_dos', mirror=sx < 0)
    n = m.bone('noyau', (0, 35.5, -1), t)
    m.cube(n, 'noyau_cristal', (-2.5, 33, -3.5), (5, 5, 5), VIOLET, deco={'*': [core_face]}, rot=(35, 45, 0), origin=(0, 35.5, -1))
    m.cube(n, 'noyau_eclat_haut', (-1, 39, -2), (2, 2, 2), VIOLET, rot=(45, 0, 45), origin=(0, 40, -1))
    m.cube(n, 'noyau_eclat_bas', (-1, 30, -2), (2, 2, 2), VIOLET, rot=(45, 0, 45), origin=(0, 31, -1))

    # masque sans visage et couronne brisee
    h = m.bone('masque', (0, 45, 0), t)
    m.cube(h, 'masque_plaque', (-4, 45, -4.5), (8, 12, 8), ARM_PLAIN, deco={'north': [slit(1)]})
    m.cube(h, 'masque_arete', (-1, 56, -5), (2, 2, 9), ARM_PLAIN)
    m.cube(h, 'masque_joue_droite', (4, 47, -3.5), (1, 7, 5), ARM_PLAIN)
    m.cube(h, 'masque_joue_gauche', (-5, 47, -3.5), (1, 7, 5), ARM_PLAIN)
    cr = m.bone('couronne', (0, 63, 0), h)
    for name, c, s, r in COURONNE:
        b = m.bone(name, c, cr, rot=r)
        mat = VIOLET if name == 'couronne_centre' else ARM_PLAIN
        m.cube(b, f'{name}_piece', tuple(c[i] - s[i] / 2 for i in range(3)), s, mat,
               deco={'*': [vline]} if mat is ARM_PLAIN and s[1] > 3 else None)

    # epaulieres detachees (os propres, flottent a cote des epaules)
    for side, sx in (('droite', 1), ('gauche', -1)):
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        ep = m.bone(f'epauliere_{side}', (15 * sx, 42, 0), t, rot=(0, 0, -12 * sx))
        m.cube(ep, f'epauliere_{side}_plaque', (xo(11, 9), 38, -6.5), (9, 7, 13), ARM, share='epaul', mirror=sx < 0, deco={'*': [rune_ring]})
        m.cube(ep, f'epauliere_{side}_crete', (xo(12, 6), 45, -5), (6, 2, 10), ARM_PLAIN, share='epaul_crete', mirror=sx < 0)
        m.cube(ep, f'epauliere_{side}_lame', (xo(13, 7), 36, -5.5), (7, 2, 11), ARM_PLAIN, share='epaul_lame', mirror=sx < 0,
               deco={'sides': [lambda f: border(P_VIO[2], 'b', glow=True)(f)]})

    # bras segmentes, mains et doigts angulaires
    for side, sx in (('droit', 1), ('gauche', -1)):
        cote = 'droite' if sx > 0 else 'gauche'
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        b = m.bone(f'bras_{side}', (10 * sx, 40, 0), t, rot=(0, 0, 15 * sx))
        m.cube(b, f'bras_{side}_segment', (xo(8, 4), 31, -2), (4, 9, 4), ARM, share='bras', mirror=sx < 0, deco={'*': [vline]})
        fa = m.bone(f'avant_bras_{side}', (10 * sx, 30, 0), b, rot=(14, 0, 0))
        m.cube(fa, f'avant_bras_{side}_segment', (xo(7.5, 5), 20, -3), (5, 10, 6), ARM, share='avant_bras', mirror=sx < 0, deco={'north': [slit(1)]})
        m.cube(fa, f'coude_{side}', (xo(8.5, 3), 27, 2), (3, 3, 3), ARM_PLAIN, share='coude', mirror=sx < 0, rot=(-35, 0, 0), origin=(10 * sx, 28.5, 3.5))
        hd = m.bone(f'main_{cote}', (10 * sx, 19, 0), fa)
        m.cube(hd, f'paume_{side}', (xo(8, 4), 15, -2.5), (4, 4, 5), ARM_PLAIN, share='paume', mirror=sx < 0)
        dg = m.bone(f'doigts_{cote}', (10 * sx, 15, 0), hd, rot=(-12, 0, 0))
        for k, z in enumerate((-2.5, -0.5, 1.5)):
            m.cube(dg, f'doigt_{side}_{k + 1}', (xo(8.5 + 1.2 * (k == 1), 1), 9, z), (1, 6, 1), ARM_PLAIN, share='doigt', mirror=sx < 0,
                   deco={'*': [lambda f: f.px(0, f.h - 1, P_VIO[4], glow=True) if f.side else None]}, rot=(0, 0, 8 * sx * (k - 1)),
                   origin=(xo(8.5 + 1.2 * (k == 1), 1) + 0.5, 15, z + 0.5))
        m.cube(dg, f'pouce_{side}', (xo(11.5, 1), 11, -2), (1, 4, 1), ARM_PLAIN, share='pouce', mirror=sx < 0, rot=(0, 0, -20 * sx),
               origin=(xo(11.5, 1) + 0.5, 15, -1.5))

    # bas du corps : fragments effiles, et eclats en orbite
    for name, c, s, r in BAS:
        b = m.bone(name, c, t, rot=r)
        m.cube(b, f'{name}_piece', tuple(c[i] - s[i] / 2 for i in range(3)), s, ARM, deco={'north': [slit(1, False)]} if s[1] >= 5 else None)
    fr = m.bone('fragments', (0, 36, 0), 'racine')
    for name, c, s in ECLATS:
        b = m.bone(name, c, fr, rot=(35, 45, 0))
        m.cube(b, f'{name}_piece', tuple(c[i] - s / 2 for i in range(3)), (s, s, s), ARM_PLAIN if s > 2 else VIOLET,
               deco={'*': [vline]} if s > 2 else None)
    return m


# instant de l'impact de chaque attaque (s) : les degats sont appliques a ce moment en jeu
STRIKES = {'charge_energie': 1.95, 'ouverture_torse': 1.2, 'teleportation_preparation': 1.1, 'reformation': 0.45}

COLORS = {'racine': 0, 'torse': 1, 'masque': 2, 'couronne': 5, 'bras_droit': 4, 'bras_gauche': 4, 'noyau': 8,
          'epauliere_droite': 3, 'epauliere_gauche': 3, 'fragments': 6, 'bas_1': 7}


# ============================================================================================== animations
NOYAU = np.array([0.0, 35.5, -1.0])
CROWN = [c[0] for c in COURONNE]
LOWER = [b[0] for b in BAS]
CHIPS = [e[0] for e in ECLATS]
PIECES = (['masque', 'epauliere_droite', 'epauliere_gauche', 'bras_droit', 'bras_gauche', 'cage_droite', 'cage_gauche']
          + CROWN + LOWER + CHIPS)


def toward_core(m, names, k):
    """Deplacement (repere du parent) qui rapproche chaque os du noyau d'une fraction k (k < 0 : l'eloigne)."""
    out = {}
    for n in names:
        b = m.bones[n]
        p = np.array(b.pivot)
        if b.parent and b.parent.name == 'fragments':       # les eclats orbitent autour du centre du groupe
            d = (np.array([0.0, 36.0, 0.0]) - p) * k
        else:
            d = (NOYAU - p) * k
        out[n] = tuple(float(v) for v in d)
    return out


def animations(m):
    P = ek.key_poses
    L = 'linear'
    M, SC = ek.M, ek.SC

    def float_layer(a, T, amp=1.0, phase=0.0):
        """Flottement individuel : couronne, epaulieres, fragments du bas, eclats (dephases)."""
        names = CROWN + ['epauliere_droite', 'epauliere_gauche'] + LOWER + CHIPS
        for i, n in enumerate(names):
            ph = (i * 0.37 + phase) * 2 * math.pi
            keys = []
            for k in range(9):
                tt = T * k / 8
                s = math.sin(2 * math.pi * k / 8 + ph)
                keys.append((tt, 0, amp * (0.6 if n in CROWN else 0.9) * s, 0))
            a.pos(n, keys)

    # --- levitation (3 s) : flottement, couronne et eclats dephases, orbite des eclats, noyau qui tourne
    a = m.anim('levitation', 3.0, 'loop')
    P(a, [(0, {}), (1.5, {'racine': (0, 1.6, 0)}), (3.0, {})], 'position', bones=['racine'])
    float_layer(a, 3.0)
    a.rot('fragments', [(0, 0, 0, 0, L), (3.0, 0, 360, 0, L)])
    a.rot('noyau', [(0, 0, 0, 0, L), (3.0, 0, 360, 0, L)])
    a.rot('couronne', [(0, 0, 0, 0), (1.5, 0, 12, 0), (3.0, 0, 0, 0)])
    ek.sway(a, LOWER, 3.0, 5, axis=2, phase_step=0.2)
    P(a, [(0, {}), (0.75, {'bras_droit': (4, 0, 3), 'bras_gauche': (-3, 0, -3), 'doigts_droite': (6, 0, 0)}),
          (1.5, {'torse': (1.5, 0, 0), 'masque': (-2, 0, 0)}), (2.25, {'bras_droit': (-3, 0, -2), 'bras_gauche': (4, 0, 2), 'doigts_gauche': (6, 0, 0)}),
          (3.0, {})])
    P(a, [(0, {}), (0.75, {'noyau': SC(1.12)}), (1.5, {}), (2.25, {'noyau': SC(1.12)}), (3.0, {})], 'scale')

    # --- deplacement flottant (2 s) : buste incline, bras et fragments qui trainent
    a = m.anim('deplacement', 2.0, 'loop')
    P(a, [(0, {}), (1.0, {'racine': (0, 1.2, 0)}), (2.0, {})], 'position', bones=['racine'])
    float_layer(a, 2.0, amp=0.6)
    a.rot('fragments', [(0, 0, 0, 0, L), (2.0, 0, 360, 0, L)])
    a.rot('noyau', [(0, 0, 0, 0, L), (2.0, 0, 360, 0, L)])
    trail = {'torse': (-14, 0, 0), 'masque': (10, 0, 0), 'bras_droit': (-22, 0, 4), 'bras_gauche': (-22, 0, -4),
             'avant_bras_droit': (-8, 0, 0), 'avant_bras_gauche': (-8, 0, 0), 'couronne': (-8, 0, 0),
             'bas_1': (-10, 0, 0), 'bas_2': (-18, 0, 0), 'bas_3': (-26, 0, 0), 'bas_4': (-32, 0, 0)}
    P(a, [(0, trail), (1.0, M(trail, {'torse': (-2, 0, 0), 'bas_3': (-6, 0, 0), 'bas_4': (-8, 0, 0)})), (2.0, trail)])

    # --- charge d'energie entre les mains (2,4 s) : mains rapprochees devant le noyau, puis projetees (tir a 1,95 s)
    a = m.anim('charge_energie', 2.4, 'once')
    joint = {'torse': (-4, 0, 0), 'masque': (-6, 0, 0), 'bras_droit': (62, 0, -27), 'bras_gauche': (62, 0, 27),
             'avant_bras_droit': (38, 0, 0), 'avant_bras_gauche': (38, 0, 0), 'doigts_droite': (30, 0, 0), 'doigts_gauche': (30, 0, 0)}
    jit = [(0.95 + 0.12 * i, M(joint, {'bras_droit': (1.5 if i % 2 else -1.5, 0, 0), 'bras_gauche': (-1.5 if i % 2 else 1.5, 0, 0),
                                       'masque': (0, 0, 1.5 if i % 2 else -1.5)}), L) for i in range(7)]
    tir = {'torse': (-10, 0, 0), 'masque': (-4, 0, 0), 'bras_droit': (85, 0, -14), 'bras_gauche': (85, 0, 14),
           'avant_bras_droit': (-14, 0, 0), 'avant_bras_gauche': (-14, 0, 0), 'doigts_droite': (40, 0, 0), 'doigts_gauche': (40, 0, 0)}
    P(a, [(0, {}), (0.8, joint)] + jit + [(1.8, joint), (1.95, tir, L), (2.1, tir), (2.4, {})])
    P(a, [(0, {}), (0.8, M(toward_core(m, CHIPS, 0.35), {'couronne': (0, 2, 0)})), (1.8, M(toward_core(m, CHIPS, 0.45), {'couronne': (0, 2.5, 0)})),
          (1.95, M(toward_core(m, CHIPS, -0.25), {'couronne': (0, 1, 0)}), L), (2.4, {})], 'position')
    P(a, [(0, {}), (0.8, {'noyau': SC(1.3)}), (1.8, {'noyau': SC(1.5)}), (1.95, {'noyau': SC(0.9)}, L), (2.4, {})], 'scale')

    # --- ouverture du torse avant une decharge (2,6 s) : la cage s'ouvre, le noyau s'avance et grossit (decharge 1,2 -> 1,8 s)
    a = m.anim('ouverture_torse', 2.6, 'once')
    ouvert = {'cage_droite': (0, -75, 0), 'cage_gauche': (0, 75, 0), 'torse': (8, 0, 0), 'masque': (18, 0, 0),
              'bras_droit': (-25, 0, 25), 'bras_gauche': (-25, 0, -25), 'doigts_droite': (35, 0, 0), 'doigts_gauche': (35, 0, 0),
              'epauliere_droite': (0, 0, -10), 'epauliere_gauche': (0, 0, 10), 'couronne': (-10, 0, 0)}
    trem = [(1.2 + 0.1 * i, M(ouvert, {'masque': (0, 0, 2 if i % 2 else -2)}), L) for i in range(7)]
    P(a, [(0, {}), (0.3, {'torse': (-6, 0, 0), 'cage_droite': (0, 8, 0), 'cage_gauche': (0, -8, 0)}), (0.9, ouvert)] + trem + [(2.0, ouvert), (2.6, {})])
    push = M(toward_core(m, CHIPS, -0.35), {'noyau': (0, 0, -4), 'epauliere_droite': (3, 1, 0), 'epauliere_gauche': (-3, 1, 0), 'couronne': (0, 3, 0)})
    P(a, [(0, {}), (0.9, push), (2.0, push), (2.6, {})], 'position')
    P(a, [(0, {}), (0.9, {'noyau': SC(1.6)}), (1.2, {'noyau': SC(1.9)}), (1.8, {'noyau': SC(1.9)}), (2.0, {'noyau': SC(1.4)}), (2.6, {})], 'scale')

    # --- preparation de teleportation (1,4 s, figee) : tout se resserre autour du noyau
    contract_pos = M(toward_core(m, CHIPS, 0.8), toward_core(m, LOWER, 0.55), toward_core(m, ['epauliere_droite', 'epauliere_gauche'], 0.4),
                     {'couronne': (0, -4, 0), 'racine': (0, 2, 0)})
    contract_rot = {'torse': (-12, 0, 0), 'masque': (-14, 0, 0), 'bras_droit': (40, 0, -30), 'bras_gauche': (40, 0, 30),
                    'avant_bras_droit': (60, 0, 0), 'avant_bras_gauche': (60, 0, 0), 'doigts_droite': (-30, 0, 0), 'doigts_gauche': (-30, 0, 0),
                    'cage_droite': (0, 12, 0), 'cage_gauche': (0, -12, 0)}
    contract_scl = dict({n: SC(0.5) for n in CHIPS}, **{n: SC(0.7) for n in LOWER}, noyau=SC(1.7))
    a = m.anim('teleportation_preparation', 1.4, 'hold')
    P(a, [(0, {}), (0.5, M(contract_rot, {'torse': (6, 0, 0)})), (1.1, contract_rot), (1.4, contract_rot)])
    P(a, [(0, {}), (1.1, contract_pos), (1.4, contract_pos)], 'position')
    P(a, [(0, {}), (1.1, contract_scl), (1.4, contract_scl)], 'scale')
    a.rot('fragments', [(0, 0, 0, 0, L), (1.4, 0, 540, 0, L)])

    # --- reformation (1,6 s) : part de la pose resserree, eclate vers l'exterieur puis se remet en place
    a = m.anim('reformation', 1.6, 'once')
    burst_pos = M(toward_core(m, CHIPS, -0.3), toward_core(m, LOWER, -0.15), toward_core(m, ['epauliere_droite', 'epauliere_gauche'], -0.2),
                  {'couronne': (0, 2, 0)})
    burst_rot = {'torse': (6, 0, 0), 'masque': (8, 0, 0), 'bras_droit': (-15, 0, 20), 'bras_gauche': (-15, 0, -20), 'doigts_droite': (30, 0, 0),
                 'doigts_gauche': (30, 0, 0), 'cage_droite': (0, -20, 0), 'cage_gauche': (0, 20, 0)}
    P(a, [(0, contract_rot), (0.45, burst_rot, L), (1.0, M(burst_rot, {k: tuple(-0.3 * x for x in v) for k, v in burst_rot.items()})), (1.6, {})])
    P(a, [(0, contract_pos), (0.45, M(burst_pos, {'racine': (0, 2, 0)}), L), (1.0, {k: tuple(-0.2 * x for x in v) for k, v in burst_pos.items()}), (1.6, {})], 'position')
    P(a, [(0, contract_scl), (0.45, dict({n: SC(1.15) for n in CHIPS}, noyau=SC(1.3)), L), (1.6, {})], 'scale')
    a.rot('fragments', [(0, 0, 0, 0, L), (1.6, 0, 360, 0, L)])

    # --- mort par dispersion autour du noyau (3,4 s, figee) : spasme, les pieces s'ecartent du noyau en tournoyant,
    #     puis retombent au sol ; le noyau reste suspendu un instant, puis s'eteint et tombe a son tour
    rng = __import__('random').Random(7)
    tumble = {n: (rng.uniform(-90, 90), rng.uniform(-120, 120), rng.uniform(-90, 90)) for n in PIECES}
    lying = {n: (rng.choice((-90, 90)) + rng.uniform(-15, 15), rng.uniform(-60, 60), rng.uniform(-20, 20)) for n in PIECES}
    spread = toward_core(m, PIECES, -0.9)
    spread = {k: (v[0], v[1] * 0.4 + 3, v[2]) for k, v in spread.items()}
    far = {k: (v[0] * 1.5, 0.0, v[2] * 1.5) for k, v in spread.items()}

    def death(final_pos):
        a = m.anim('mort', 3.4, 'hold')
        jit = {n: (rng2.uniform(-12, 12), rng2.uniform(-12, 12), rng2.uniform(-12, 12)) for n in PIECES}
        P(a, [(0, {}), (0.35, jit), (1.2, tumble), (2.4, lying, L), (3.4, lying)], bones=PIECES + ['torse'])
        P(a, [(0, {}), (0.35, {'noyau': (0, 0, -1)}), (1.2, M(spread, {'racine': (0, 2, 0)})),
              (2.4, M(final_pos, {'racine': (0, 2, 0)}), L), (3.4, M(final_pos, {'racine': (0, 2, 0)}))],
          'position', bones=PIECES + ['racine', 'torse', 'noyau'])
        P(a, [(0, {}), (0.35, {'noyau': SC(1.6)}), (1.2, {'noyau': SC(1.3)}), (2.4, {'noyau': SC(1.0)}), (2.8, {'noyau': SC(0.6)}),
              (3.4, {'noyau': SC(0.5)})], 'scale', bones=['noyau'])
        return a
    rng2 = __import__('random').Random(11)
    # 1re passe : position de chute provisoire, puis calcul des positions qui posent chaque piece au sol a 2,4 s
    tmp = death(M(far, {'torse': (0, -26, 0), 'noyau': (0, 0, 0)}))
    final = ek.settle(m, tmp, 2.4, ['torse'] + PIECES + ['noyau'], ground=0.2)
    m.animations.remove(tmp)
    # 2e passe : animation definitive avec ces positions (memes tirages aleatoires)
    rng2 = __import__('random').Random(11)
    death({k: tuple(v) for k, v in final.items()})
