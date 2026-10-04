# -*- coding: utf-8 -*-
"""Liche ancienne : squelette en levitation (environ 3 blocs, couronne comprise), robe violette en panneaux,
cristal d'ame cyan dans la cage thoracique, baton a cristal dans la main droite."""
import entity_kit as ek
from entity_kit import pal, Mat, stamp, rivets, border, scratches, blotches, tatter, cracks

KEY = 'liche_ancienne'
TITLE = 'Liche ancienne'
ENTITY_CLASS = 'LicheAncienneEntity'
VISIBLE_BOX = (2, 3.4, 0)

P_OS = pal('#4c473b', '#6f6857', '#968d77', '#bbb298', '#dad2ba', '#f0eada')
P_ROBE = pal('#160b1f', '#211130', '#2e1843', '#3d2057', '#4f2a6f', '#643789', '#7b47a3')
P_ARGENT = pal('#2c2b32', '#44424b', '#5d5b65', '#7a7782', '#9a97a1', '#bdbac2', '#dcd9df')
P_CYAN = pal('#0a4752', '#10697a', '#1898ab', '#2fc4d6', '#79eef7', '#d4feff')
P_BOIS = pal('#1c1715', '#2a2420', '#3a322b', '#4c4239', '#5f5347')
SOMBRE = (14, 8, 20)
TERNI = [(70, 82, 78), (58, 66, 66)]

BONE = Mat(P_OS, level=0.6, noise=0.25, scale=2, grad=0.25, decos=[blotches(0.6, [P_OS[2]], (0.5, 1))])
ROBE = Mat(P_ROBE, level=0.5, noise=0.35, scale=(1.2, 6), grad=0.2)
ROBE_ALT = Mat(P_ROBE, level=0.42, noise=0.35, scale=(1.2, 6), grad=0.2)
ROBE_DARK = Mat(P_ROBE, level=0.22, noise=0.25, scale=(1.5, 5), grad=0.1)
SILVER = Mat(P_ARGENT, level=0.55, noise=0.25, scale=2.5, rim=0.25, decos=[blotches(0.8, TERNI, (0.5, 1.2))])
CRYSTAL = Mat(P_CYAN, level=0.62, noise=0.2, scale=1.5, grad=0.45, glow=True)
WOOD = Mat(P_BOIS, level=0.5, noise=0.3, scale=(1, 5), grad=0.1)
VOID = Mat(pal('#0c0612', '#140a1c', '#1c0f27'), level=0.4, noise=0.4, scale=2)

C = {'B': P_OS[4], 'b': P_OS[3], 'd': P_OS[1], 'K': SOMBRE, 'c': P_CYAN[3], 'C': P_CYAN[5], 'S': P_ARGENT[5], 's': P_ARGENT[3]}

SKULL = stamp([
    'dbBBBBbd',
    'bBBBBBBb',
    'BKKBBKKB',
    'KCcKKcCK',
    'BKKbbKKB',
    'bBBKKBBb',
    '.bBdBdB.',
    '.BdBdBd.',
], C, glow='Cc')

JAW = stamp(['BdBdBd', 'bbbbbb'], C, ay='t')

RIBS = stamp([
    '.BBBBBB.',
    'B...B..B',
    '.bBBBBb.',
    'B...B..B',
    '.bBBBBb.',
    'B...B..B',
    '.bBBBBb.',
    '...BB...',
    '...BB...',
    '..BddB..',
    '.B....B.',
], C)

RIBS_SIDE = stamp([
    'BBBBBB',
    '......',
    'bBBBBb',
    '......',
    'bBBBBb',
    '......',
    'bBBBBb',
    '......',
    '......',
    '......',
    '......',
], C)


def cross(f):
    """Croix cyan du panneau avant (motif de la robe)."""
    if f.name not in ('north', 'south'):
        return
    cx = f.w // 2
    for y in range(3, min(f.h - 5, 14)):
        f.px(cx - 1, y, P_CYAN[3], glow=True)
        f.px(cx, y, P_CYAN[4], glow=True)
    for x in range(1, f.w - 1):
        f.px(x, 6, P_CYAN[3], glow=True)
    f.px(cx - 1, 6, P_CYAN[5], glow=True)
    f.px(cx, 6, P_CYAN[5], glow=True)


def trim(color=P_ARGENT[4], sides='lr'):
    return border(color, sides)


def sigil(f):
    """Sceau argent dans le dos de la robe."""
    if f.name == 'south':
        stamp(['..s..', '.sSs.', 'sS.Ss', '.sSs.', '..s..'], C, ay='t', dy=4)(f)


def runes_band(f):
    """Bande argent avec petites gemmes (couronne, ceinture)."""
    if not f.side:
        return
    for x in range(f.w):
        if x % 4 == 2:
            f.px(x, f.h // 2, P_CYAN[4], glow=True)


def fingers(f):
    if f.name == 'north' or f.name == 'south':
        for x in range(0, f.w, 2):
            f.px(x, f.h - 1, P_OS[1])


def chain(f):
    if f.side:
        for y in range(f.h):
            f.px(0, y, P_ARGENT[4] if y % 2 == 0 else P_ARGENT[1])
        f.px(0, f.h - 1, P_CYAN[4], glow=True)


def shaft(f):
    if f.side:
        for y in range(0, f.h, 9):
            for x in range(f.w):
                f.px(x, y, P_ARGENT[4])
                f.px(x, y + 1, P_ARGENT[2])


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))
    t = m.bone('torse', (0, 22, 0), 'racine')
    m.cube(t, 'robe_dos', (-8, 22, 1), (16, 14, 4), ROBE, deco={'sides': [trim(P_ROBE[1], 'b')]})
    m.cube(t, 'robe_flanc_droit', (4, 22, -5), (4, 14, 6), ROBE, deco={'west': [trim(P_ARGENT[4], 'l')]})
    m.cube(t, 'robe_flanc_gauche', (-8, 22, -5), (4, 14, 6), ROBE, deco={'east': [trim(P_ARGENT[4], 'r')]})
    m.cube(t, 'revers_droit', (3, 23, -5.5), (1, 12, 1), SILVER)
    m.cube(t, 'revers_gauche', (-4, 23, -5.5), (1, 12, 1), SILVER)
    m.cube(t, 'cotes', (-4, 24, -4.5), (8, 11, 6), VOID, deco={'north': [RIBS], 'east': [RIBS_SIDE], 'west': [RIBS_SIDE]})
    m.cube(t, 'colonne', (-1, 21, -0.5), (2, 4, 2), BONE)
    m.cube(t, 'ceinture', (-8.5, 20, -5.5), (17, 3, 11), ROBE_DARK, deco={'sides': [trim(P_ARGENT[3], 't'), runes_band]})
    m.cube(t, 'boucle', (-2, 19.5, -6.5), (4, 4, 1), SILVER, deco={'north': [stamp(['.cc.', 'cCCc', 'cCCc', '.cc.'], C, glow='cC')]})
    m.cube(t, 'mantelet', (-9, 35, -5.5), (18, 3, 11), ROBE, deco={'sides': [trim(P_ARGENT[4], 'b')]})
    ri = m.bone('robe_interieure', (0, 22, 0), t)
    m.cube(ri, 'jupon', (-7, 6, -5), (14, 16, 10), ROBE_DARK, deco={'*': [tatter(3, seed=2.2)]})

    ca = m.bone('cristal_ame', (0, 30, -5), t)
    m.cube(ca, 'cristal', (-1.5, 28, -6.5), (3, 5, 3), CRYSTAL, rot=(0, 45, 0), origin=(0, 30.5, -5))
    m.cube(ca, 'cristal_pointe', (-1, 33, -6), (2, 2, 2), CRYSTAL, rot=(0, 45, 0), origin=(0, 34, -5))

    # tete, capuche et couronne
    h = m.bone('tete', (0, 37, -1), t)
    m.cube(h, 'crane', (-4, 38, -5), (8, 8, 8), BONE, deco={'north': [SKULL]})
    jw = m.bone('machoire', (0, 38, 1), h)
    m.cube(jw, 'machoire_bas', (-3, 36, -5.5), (6, 2, 6), BONE, deco={'north': [JAW]})
    m.cube(h, 'capuche_haut', (-6, 46, -7), (12, 2, 12), ROBE, deco={'north': [trim(P_ROBE[1], 'b')]})
    m.cube(h, 'capuche_droite', (4, 35, -7), (2, 11, 12), ROBE, deco={'north': [trim(P_ROBE[1], 'l')], 'west': [trim(P_ROBE[0], 'tblr')]})
    m.cube(h, 'capuche_gauche', (-6, 35, -7), (2, 11, 12), ROBE, deco={'north': [trim(P_ROBE[1], 'r')], 'east': [trim(P_ROBE[0], 'tblr')]})
    m.cube(h, 'capuche_dos', (-4, 35, 3), (8, 11, 2), ROBE)
    m.cube(h, 'capuche_pointe', (-3, 37, 5), (6, 9, 1), ROBE, deco={'*': [tatter(2, seed=0.4)]})
    cr = m.bone('couronne', (0, 48, -1), h, rot=(0, 0, -4))
    m.cube(cr, 'couronne_bandeau', (-6.5, 47.5, -7.5), (13, 2, 13), SILVER, deco={'*': [runes_band]})
    for i, (x, y, z, w, hh, rz) in enumerate(((-1, 49.5, -8, 2, 5, 0), (-5.5, 49.5, -8, 2, 4, 12), (3.5, 49.5, -8, 2, 4, -12),
                                               (-6.5, 49.5, -3, 1, 4, 14), (5.5, 49.5, -3, 1, 4, -14), (-3, 49.5, 5, 1, 3, 0),
                                               (2, 49.5, 5, 1, 4, 0))):
        m.cube(cr, f'pointe_{i + 1}', (x, y, z), (w, hh, 1), SILVER, rot=(0, 0, rz) if rz else None,
               origin=(x + w / 2, y, z + 0.5) if rz else None)
    m.cube(cr, 'gemme_couronne', (-1, 49.5, -8.5), (2, 2, 1), CRYSTAL)

    # bras : manches evasees, mains squelettiques
    for side, sx in (('droit', 1), ('gauche', -1)):
        hand = 'main_droite' if sx > 0 else 'main_gauche'
        b = m.bone(f'bras_{side}', (8 * sx, 35, -1), t, rot=(0, 0, 4) if sx > 0 else (10, 0, -24))
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        m.cube(b, f'epauliere_{side}', (xo(6, 6), 34, -5.5), (6, 4, 9), SILVER, share='epauliere', mirror=sx < 0,
               deco={'*': [rivets(P_ARGENT[6], P_ARGENT[1], every=3, rows=('b',))]})
        m.cube(b, f'epauliere_{side}_haut', (xo(8, 4), 38, -4.5), (4, 2, 7), SILVER, share='epauliere_haut', mirror=sx < 0)
        m.cube(b, f'chaine_{side}', (xo(10.5, 1), 27, -5.5), (1, 7, 1), SILVER, deco={'*': [chain]}, share='chaine', mirror=sx < 0)
        m.cube(b, f'manche_haut_{side}', (xo(6.5, 5), 26, -4), (5, 9, 6), ROBE, share='manche_haut', mirror=sx < 0)
        fa = m.bone(f'avant_bras_{side}', (9 * sx, 27, -1), b, rot=(35, 0, 0) if sx > 0 else (45, 0, 10))
        m.cube(fa, f'manche_bas_{side}', (xo(5.5, 7), 17, -5), (7, 10, 8), ROBE, share='manche_bas', mirror=sx < 0,
               deco={'sides': [trim(P_ARGENT[4], 'b'), tatter(2, seed=1.1)]})
        hd = m.bone(hand, (9 * sx, 18, -1), fa)
        m.cube(hd, f'paume_{side}', (xo(7.5, 3), 14.5, -2.5), (3, 4, 3), BONE, share='paume', mirror=sx < 0)
        for k in range(3):
            spread = (0, 0, (k - 1) * 18 * sx) if sx < 0 else None
            m.cube(hd, f'doigt_{side}_{k + 1}', (xo(7.5 + k, 1), 11.5 + (k % 2), -2.5), (1, 3, 1), BONE, share=f'doigt_{k % 2}', mirror=sx < 0,
                   deco={'*': [fingers]}, rot=spread, origin=(xo(7.5 + k, 1) + 0.5, 14.5, -2) if spread else None)
        m.cube(hd, f'pouce_{side}', (xo(7.5, 1), 13, -3.5), (1, 2, 1), BONE, share='pouce', mirror=sx < 0)

    # baton (contre-rotation de l'avant-bras : il reste vertical au repos)
    bt = m.bone('baton', (9, 16, -1), 'main_droite', rot=(-35, 0, 0))
    m.cube(bt, 'hampe', (8, 1, -2), (2, 38, 2), WOOD, deco={'*': [shaft]})
    m.cube(bt, 'ferrure', (7.5, -1, -2.5), (3, 3, 3), SILVER)
    m.cube(bt, 'collier', (7, 38, -3), (4, 2, 4), SILVER, deco={'*': [runes_band]})
    for i, (dx, dz) in enumerate(((-1.5, -1.5), (1.5, -1.5), (-1.5, 1.5), (1.5, 1.5))):
        m.cube(bt, f'griffe_{i + 1}', (9 + dx - 0.5, 40, -1 + dz - 0.5), (1, 6, 1), SILVER,
               rot=(dz * 8, 0, -dx * 8), origin=(9 + dx, 40, -1 + dz))
    cs = m.bone('cristal_baton', (9, 44, -1), bt)
    m.cube(cs, 'cristal_baton_corps', (7.5, 41, -2.5), (3, 7, 3), CRYSTAL, rot=(0, 45, 0), origin=(9, 44.5, -1))
    m.cube(cs, 'cristal_baton_pointe', (8, 48, -2), (2, 3, 2), CRYSTAL, rot=(0, 45, 0), origin=(9, 49.5, -1))

    # panneaux de robe (un os par panneau, pivot a la taille)
    panels = [
        ('robe_avant', (0, 22, -6), (6, 0, 0), (-4, 3, -7), (8, 19, 1), [trim(P_ARGENT[4], 'lr'), cross, tatter(3, seed=0.3)]),
        ('robe_avant_droite', (6, 22, -5.5), (5, 0, 7), (3, 5, -6.5), (6, 17, 1), [trim(P_ARGENT[3], 'l'), tatter(4, seed=1.7)]),
        ('robe_avant_gauche', (-6, 22, -5.5), (5, 0, -7), (-9, 4, -6.5), (6, 18, 1), [trim(P_ARGENT[3], 'r'), tatter(4, seed=2.9)]),
        ('robe_droite', (7.5, 22, 0), (0, 0, 9), (7, 3, -5), (1, 19, 10), [tatter(4, seed=3.3)]),
        ('robe_gauche', (-7.5, 22, 0), (0, 0, -9), (-8, 4, -5), (1, 18, 10), [tatter(4, seed=4.1)]),
        ('robe_arriere', (0, 22, 5.5), (-6, 0, 0), (-7, 2, 5), (14, 20, 1), [trim(P_ARGENT[3], 'lr'), sigil, tatter(4, seed=5.6)]),
        ('robe_arriere_droite', (6, 22, 5), (-5, 0, 8), (4, 4, 5.5), (5, 18, 1), [tatter(3, seed=6.2)]),
        ('robe_arriere_gauche', (-6, 22, 5), (-5, 0, -8), (-9, 5, 5.5), (5, 17, 1), [tatter(3, seed=7.7)]),
    ]
    for i, (name, piv, rot, frm, size, decos) in enumerate(panels):
        b = m.bone(name, piv, t, rot=rot)
        mat = ROBE if i % 2 == 0 else ROBE_ALT
        decos = [border(P_ROBE[1], 'lr')] + decos[:-1] + [tatter(6, seed=1.3 * i + 0.5, holes=0.06)]
        m.cube(b, f'{name}_tissu', frm, size, mat, deco={'sides': decos, 'down': [tatter(1)]})
    return m


# instant de l'impact de chaque attaque (s) : les degats sont appliques a ce moment en jeu
STRIKES = {'projectile': 0.57, 'invocation': 1.1, 'canalisation': 0.5, 'recul': 0.0}

COLORS = {'racine': 0, 'torse': 1, 'tete': 2, 'couronne': 5, 'bras_droit': 4, 'bras_gauche': 4, 'baton': 7,
          'cristal_ame': 3, 'robe_avant': 6, 'robe_arriere': 6}

PANNEAUX = ['robe_avant', 'robe_avant_droite', 'robe_droite', 'robe_arriere_droite', 'robe_arriere',
            'robe_arriere_gauche', 'robe_gauche', 'robe_avant_gauche']
# sens "vers l'exterieur" de chaque panneau : (x, z) a appliquer en rotation pour l'ecarter du corps
EVASE = {'robe_avant': (1, 0), 'robe_avant_droite': (0.7, 0.7), 'robe_droite': (0, 1), 'robe_arriere_droite': (-0.7, 0.7),
         'robe_arriere': (-1, 0), 'robe_arriere_gauche': (-0.7, -0.7), 'robe_gauche': (0, -1), 'robe_avant_gauche': (0.7, -0.7)}


def flare(deg, back=0.0):
    """Rotation des panneaux : 'deg' vers l'exterieur, 'back' en plus vers l'arriere (tissu qui traine)."""
    return {p: (EVASE[p][0] * deg - back, 0, EVASE[p][1] * deg) for p in PANNEAUX}


# ============================================================================================== animations
def animations(m):
    P = ek.key_poses
    L = 'linear'

    # --- levitation au repos (3 s) : flottement, robe ondulante, cristal qui pulse
    a = m.anim('repos', 3.0, 'loop')
    P(a, [(0, {}), (1.5, {'racine': (0, 1.4, 0), 'cristal_baton': (0, 0.4, 0)}), (3.0, {})], 'position')
    P(a, [(0, {}), (0.75, {'torse': (1.5, 0, 0), 'tete': (-2, 4, 2), 'machoire': (-3, 0, 0), 'bras_droit': (2, 0, 0),
                           'bras_gauche': (-3, 0, -3), 'avant_bras_gauche': (6, 0, 0), 'cristal_baton': (0, 45, 0)}),
          (2.25, {'torse': (-1.5, 0, 0), 'tete': (2, -4, -2), 'bras_droit': (-2, 0, 0), 'bras_gauche': (3, 0, 3),
                  'avant_bras_gauche': (-4, 0, 0), 'cristal_baton': (0, 135, 0)}),
          (3.0, {'cristal_baton': (0, 180, 0)})])
    P(a, [(0, {}), (0.75, {'cristal_ame': (1.12, 1.12, 1.12)}), (1.5, {}), (2.25, {'cristal_ame': (1.12, 1.12, 1.12)}), (3.0, {})], 'scale')
    ek.sway(a, PANNEAUX, 3.0, 4, axis=0, phase_step=0.125)

    # --- deplacement flottant (1,6 s) : buste penche, robe qui traine vers l'arriere
    a = m.anim('deplacement', 1.6, 'loop')
    P(a, [(0, {}), (0.8, {'racine': (0, 1.5, 0)}), (1.6, {})], 'position')
    P(a, [(0, {'torse': (-12, 0, 2), 'tete': (8, 0, -2), 'bras_droit': (-8, 0, 0), 'bras_gauche': (-14, 0, 6), 'avant_bras_gauche': (-15, 0, 0)}),
          (0.8, {'torse': (-10, 0, -2), 'tete': (6, 0, 2), 'bras_droit': (-12, 0, 0), 'bras_gauche': (-18, 0, 4), 'avant_bras_gauche': (-18, 0, 0)}),
          (1.6, {'torse': (-12, 0, 2), 'tete': (8, 0, -2), 'bras_droit': (-8, 0, 0), 'bras_gauche': (-14, 0, 6), 'avant_bras_gauche': (-15, 0, 0)})])
    ek.sway(a, PANNEAUX, 0.8, 6, axis=0, phase_step=0.125, base=flare(4, back=22))

    # --- lancement de projectile (1,3 s) : main gauche ramenee, puis projetee vers l'avant (tir a 0,55 s)
    a = m.anim('projectile', 1.3, 'once')
    arme = {'torse': (4, -22, 0), 'tete': (0, 18, 0), 'bras_gauche': (-35, 0, -25), 'avant_bras_gauche': (25, 0, 0),
            'machoire': (-6, 0, 0), 'bras_droit': (-6, 0, 6)}
    tir = {'torse': (-8, 20, 0), 'tete': (-4, -16, 0), 'bras_gauche': (72, -10, 18), 'avant_bras_gauche': (-45, 0, -10),
           'main_gauche': (-25, 0, 0), 'machoire': (-18, 0, 0), 'bras_droit': (4, 0, 0)}
    P(a, [(0, {}), (0.4, arme), (0.47, arme, L), (0.57, tir, L), (0.8, tir), (1.3, {})])
    P(a, [(0, {}), (0.4, flare(2, back=-6)), (0.57, flare(4, back=12), L), (0.8, flare(2, back=6)), (1.3, {})], bones=PANNEAUX)
    P(a, [(0, {}), (0.47, {'cristal_ame': (1.1, 1.1, 1.1)}, L), (0.57, {'cristal_ame': (1.35, 1.35, 1.35)}, L), (0.9, {}), (1.3, {})], 'scale')

    # --- invocation (2,6 s) : les deux bras leves, baton dresse, robe qui s'evase, la liche s'eleve
    a = m.anim('invocation', 2.6, 'once')
    leve = {'torse': (10, 0, 0), 'tete': (18, 0, 0), 'machoire': (-22, 0, 0), 'bras_droit': (125, 0, 12),
            'avant_bras_droit': (-35, 0, 0), 'baton': (-90, 0, 0), 'bras_gauche': (135, 0, -22), 'avant_bras_gauche': (-40, 0, 0),
            'main_gauche': (-20, 0, 0), 'couronne': (0, 0, 3)}
    mi = {k: tuple(v * 0.45 for v in vals) for k, vals in leve.items()}
    trem = [(1.1 + 0.15 * i, dict(leve, tete=(18, 0, 2 if i % 2 else -2), bras_gauche=(135, 0, -20 if i % 2 else -24)), L) for i in range(6)]
    P(a, [(0, {}), (0.6, mi), (1.1, leve)] + trem + [(2.0, leve), (2.6, {})])
    P(a, [(0, {}), (0.6, flare(6)), (1.1, flare(18))] + [(1.1 + 0.15 * i, flare(18 + (3 if i % 2 else -3)), L) for i in range(6)]
      + [(2.0, flare(16)), (2.6, {})], bones=PANNEAUX)
    P(a, [(0, {}), (1.1, {'racine': (0, 4, 0)}), (2.0, {'racine': (0, 4.5, 0)}), (2.6, {})], 'position')
    P(a, [(0, {}), (1.1, {'cristal_ame': (1.4, 1.4, 1.4), 'cristal_baton': (1.3, 1.3, 1.3)}),
          (2.0, {'cristal_ame': (1.5, 1.5, 1.5), 'cristal_baton': (1.4, 1.4, 1.4)}), (2.6, {})], 'scale')

    # --- canalisation (2 s, en boucle) : baton pointe vers la cible, tenu a deux mains, tremblement
    a = m.anim('canalisation', 2.0, 'loop')
    can = {'torse': (-6, 8, 0), 'tete': (6, -6, 0), 'bras_droit': (68, 0, -6), 'avant_bras_droit': (-20, 0, 0),
           'baton': (-93, 0, 0), 'bras_gauche': (52, -20, 30), 'avant_bras_gauche': (-25, 0, 0), 'machoire': (-10, 0, 0)}
    keys = []
    for i in range(9):
        j = (1 if i % 2 else -1) * (1.0 if i % 4 < 2 else 0.6)
        keys.append((0.25 * i, dict(can, baton=(-93 + 1.5 * j, 0, j), tete=(6, -6, j), torse=(-6, 8, 0.5 * j))))
    keys[-1] = (2.0, keys[0][1])
    P(a, keys)
    ek.sway(a, PANNEAUX, 0.5, 5, axis=0, phase_step=0.2, base=flare(6, back=-4))
    P(a, [(0, {'cristal_baton': (1.2, 1.2, 1.2), 'cristal_ame': (1.15, 1.15, 1.15)}), (0.5, {'cristal_baton': (1.4, 1.4, 1.4), 'cristal_ame': (1.25, 1.25, 1.25)}),
          (1.0, {'cristal_baton': (1.2, 1.2, 1.2), 'cristal_ame': (1.15, 1.15, 1.15)}), (1.5, {'cristal_baton': (1.4, 1.4, 1.4), 'cristal_ame': (1.25, 1.25, 1.25)}),
          (2.0, {'cristal_baton': (1.2, 1.2, 1.2), 'cristal_ame': (1.15, 1.15, 1.15)})], 'scale')
    P(a, [(0, {'racine': (0, 1, 0)}), (1.0, {'racine': (0, 1.6, 0)}), (2.0, {'racine': (0, 1, 0)})], 'position')

    # --- recul a l'impact (0,7 s) : rejete en arriere, la robe fouette vers l'avant
    a = m.anim('recul', 0.7, 'once')
    choc = {'torse': (16, 0, -4), 'tete': (20, 0, 6), 'machoire': (-12, 0, 0), 'bras_droit': (-10, 0, 14), 'bras_gauche': (-20, 0, -16)}
    P(a, [(0, {}), (0.1, choc, L), (0.3, dict(choc, torse=(6, 0, -2), tete=(8, 0, 2))), (0.7, {})])
    P(a, [(0, {}), (0.1, flare(4, back=-16), L), (0.3, flare(2, back=6)), (0.7, {})], bones=PANNEAUX)
    P(a, [(0, {}), (0.1, {'racine': (0, 0.5, 3)}, L), (0.3, {'racine': (0, 0, 1)}), (0.7, {})], 'position')

    # --- mort (3,2 s, figee) : spasme, la levitation cesse, le corps s'affaisse sur sa robe, le baton tombe
    a = m.anim('mort', 3.2, 'hold')
    spasme = {'torse': (16, 0, 0), 'tete': (26, 0, 0), 'machoire': (-26, 0, 0), 'bras_droit': (10, 0, 30), 'bras_gauche': (-10, 0, -20),
              'avant_bras_gauche': (-30, 0, 0)}
    affaisse = {'torse': (-38, 0, 6), 'tete': (-30, 0, 10), 'machoire': (-14, 0, 0), 'bras_droit': (-20, 0, 6), 'avant_bras_droit': (-20, 0, 0),
                'bras_gauche': (-30, 0, 10), 'avant_bras_gauche': (-40, 0, 0), 'couronne': (-10, 0, 25)}
    sol = dict(affaisse, baton=(0, 0, -80))
    P(a, [(0, {}), (0.45, spasme), (0.6, dict(spasme, tete=(22, 0, 4))), (1.3, dict(affaisse, baton=(0, 0, -20))),
          (1.75, sol, L), (1.9, dict(sol, baton=(0, 0, -74))), (2.05, sol), (3.2, sol)])
    def tas(deg, comp):
        f = flare(deg)
        for p in ('robe_avant', 'robe_avant_droite', 'robe_avant_gauche'):     # compense le buste penche en avant
            f[p] = (f[p][0] + comp * (1.0 if p == 'robe_avant' else 1.3), 0, f[p][2])
        return f
    P(a, [(0, {}), (0.6, flare(8)), (1.0, tas(38, 28)), (1.3, tas(62, 40)), (1.75, tas(74, 42)), (3.2, tas(74, 42))], bones=PANNEAUX)
    P(a, [(0, {}), (0.45, {'racine': (0, 2, 0)}), (1.3, {'racine': (0, -8, 0)}), (1.75, {'racine': (0, -11, 0), 'couronne': (0, -1, -2), 'baton': (0, -6, 0)}, L),
          (3.2, {'racine': (0, -11, 0), 'couronne': (2, -3, -4), 'baton': (0, -6, 0)})], 'position')
    P(a, [(0, {}), (0.45, {'cristal_ame': (1.4, 1.4, 1.4)}), (0.6, {'cristal_ame': (0.9, 0.9, 0.9)}), (0.75, {'cristal_ame': (1.2, 1.2, 1.2)}),
          (1.3, {'cristal_ame': (0.7, 0.7, 0.7), 'cristal_baton': (0.8, 0.8, 0.8), 'robe_interieure': (1, 0.6, 1)}),
          (1.75, {'cristal_ame': (0.65, 0.65, 0.65), 'cristal_baton': (0.7, 0.7, 0.7), 'robe_interieure': (1.1, 0.35, 1.1)}),
          (3.2, {'cristal_ame': (0.6, 0.6, 0.6), 'cristal_baton': (0.7, 0.7, 0.7), 'robe_interieure': (1.1, 0.35, 1.1)})], 'scale')
