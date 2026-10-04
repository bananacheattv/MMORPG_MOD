# -*- coding: utf-8 -*-
"""Titan de glace : golem colossal de roche sombre et de plaques de glace (environ 6 blocs, cristaux compris).
Petite tete enfoncee entre les epaules, enormes poings, pieds tres larges, noyau glaciaire derriere deux plaques
de poitrine montees sur charnieres. Combat a mains nues. Glace entierement opaque."""
import math

import entity_kit as ek
from entity_kit import pal, Mat, stamp, border, blotches, cracks

KEY = 'titan_glace'
TITLE = 'Titan de glace'
ENTITY_CLASS = 'TitanGlaceEntity'
VISIBLE_BOX = (5, 6.2, 0)
VIEW_SCALE = 3.4
SHEET_SCALE = 1.7
GIF_SCALE = 1.6

P_ROC = pal('#23262c', '#2e3239', '#3a3f48', '#484e58', '#575e6a', '#6a727f')
P_GLACE = pal('#4d77a8', '#6994c6', '#87b3e0', '#a7cdf2', '#c9e4fb', '#ecf6ff')
P_COEUR = pal('#13588f', '#1f78bd', '#3aa0e6', '#6fc8fa', '#b6e9ff', '#eefbff')
P_YEUX = pal('#1b9fc0', '#4fe0f5', '#c8fbff')


def frost_cap(depth=3, seed=0.0):
    """Calotte de givre : le haut des faces laterales est gele avec des coulures ; la face du dessus entierement."""
    def d(f):
        if f.name == 'down':
            return
        if f.name == 'up':
            for y in range(f.h):
                for x in range(f.w):
                    f.px(x, y, P_GLACE[4] if (x * 7 + y * 3) % 5 else P_GLACE[5])
            return
        for i in range(f.w):
            wx, _, wz = f.world(i, 0)
            drip = depth + int(round(2.2 * (math.sin(wx * 1.9 + seed) * math.sin(wz * 1.3 + seed * 2) + 0.4 * math.sin((wx + wz) * 3.1))))
            if (int(wx * 3 + wz * 5) % 7) == 0:
                drip += 2
            for y in range(min(f.h, max(1, drip))):
                c = P_GLACE[5] if y == 0 else P_GLACE[4] if y < drip - 1 else P_GLACE[3]
                f.px(i, y, c)
    return d


def ice_streaks(f):
    """Glace : reflets en diagonale et fissures bleu fonce."""
    for k in range(-f.h, f.w, 6):
        for j in range(f.h):
            x = k + j // 2
            if 0 <= x < f.w and f.rng.random() < 0.8:
                f.px(x, j, P_GLACE[5])
    cracks([P_GLACE[1], P_GLACE[0]], n=0.8, length=(3, 6), glow=False)(f)


ROC = Mat(P_ROC, level=0.5, noise=0.32, scale=4, rim=0.22, decos=[blotches(0.5, [P_ROC[1], P_ROC[4]], (0.7, 1.6)),
                                                                cracks([P_ROC[0]], n=0.6, length=(3, 7), glow=False)])
GLACE = Mat(P_GLACE, level=0.55, noise=0.25, scale=3, grad=0.3, rim=0.15, decos=[ice_streaks])
CRISTAL = Mat(P_GLACE, level=0.62, noise=0.15, scale=(1.5, 5), grad=0.45, decos=[ice_streaks])
COEUR = Mat(P_COEUR, level=0.6, noise=0.2, scale=2, grad=0.4, glow=True)
CREUX = Mat(pal('#0d1a2a', '#132539', '#1a3149'), level=0.5, noise=0.4, scale=2)

EYES = stamp([
    '..............',
    '..............',
    '..............',
    '..............',
    '.KKKK....KKKK.',
    '.KCCc....cCCK.',
    '..KKK....KKK..',
    '..............',
    '...K.K..K.K...',
    '....K.KK.K....',
    '..............',
    '..............',
], {'K': (16, 18, 22), 'C': P_YEUX[2], 'c': P_YEUX[1]}, glow='Cc')


def core_face(f):
    """Noyau : losange lumineux, coeur presque blanc."""
    cx, cy = (f.w - 1) / 2, (f.h - 1) / 2
    for y in range(f.h):
        for x in range(f.w):
            d = abs(x - cx) / max(1, f.w / 2) + abs(y - cy) / max(1, f.h / 2)
            col = P_COEUR[5] if d < 0.35 else P_COEUR[4] if d < 0.6 else P_COEUR[3] if d < 0.85 else P_COEUR[2]
            f.px(x, y, col, glow=True)


def plate_edges(f):
    """Plaque de glace : bord exterieur epais, givre en haut."""
    border(P_GLACE[1], 'lrb')(f)


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))
    bs = m.bone('bassin', (0, 42, 0), 'racine')
    m.cube(bs, 'bassin_roc', (-16, 36, -10), (32, 12, 20), ROC, deco={'sides': [frost_cap(2, 0.5)], 'up': [frost_cap()]})
    m.cube(bs, 'ceinture_glace', (-11, 38, -11.5), (22, 6, 2), GLACE, deco={'*': [plate_edges]})

    t = m.bone('torse', (0, 48, 0), bs)
    m.cube(t, 'ventre', (-18, 46, -12), (36, 13, 24), ROC, deco={'sides': [frost_cap(2, 1.1)]})
    m.cube(t, 'poitrine', (-24, 58, -14), (48, 22, 28), ROC, deco={'sides': [frost_cap(3, 2.3)], 'up': [frost_cap()]})
    m.cube(t, 'cavite_noyau', (-7, 58, -14.5), (14, 15, 1), CREUX)
    m.cube(t, 'plaque_dos', (-16, 60, 13), (32, 16, 3), GLACE, deco={'*': [plate_edges]})
    n = m.bone('noyau', (0, 66, -15), t)
    m.cube(n, 'noyau_cristal', (-4, 62, -17), (8, 8, 4), COEUR, deco={'*': [core_face]}, rot=(0, 0, 45), origin=(0, 66, -15))
    m.cube(n, 'noyau_eclats', (-1.5, 59, -16.5), (3, 14, 2), COEUR)
    for side, sx in (('droite', 1), ('gauche', -1)):
        pq = m.bone(f'plaque_{side}', (15 * sx, 66, -15), t)
        m.cube(pq, f'plaque_{side}_glace', (2 if sx > 0 else -15, 57, -17.5), (13, 17, 3), GLACE,
               deco={'*': [plate_edges], 'up': [frost_cap()]}, share='plaque_poitrine', mirror=sx < 0)
    pv = m.bone('plaque_ventre', (0, 57, -14), t)
    m.cube(pv, 'plaque_ventre_glace', (-10, 47, -16), (20, 10, 3), GLACE, deco={'*': [plate_edges]})

    # tete enfoncee entre les epaules
    h = m.bone('tete', (0, 78, -12), t)
    m.cube(h, 'tete_roc', (-7, 74, -21), (14, 12, 12), ROC, deco={'north': [EYES], 'sides': [frost_cap(2, 3.3)], 'up': [frost_cap()]})
    m.cube(h, 'arcade_glace', (-7.5, 82, -22.5), (15, 3, 4), GLACE, deco={'*': [plate_edges]})
    m.cube(h, 'machoire', (-5, 72, -20), (10, 3, 9), ROC)

    # cristaux du dos (legere asymetrie)
    cr = m.bone('cristaux', (0, 78, 12), t)
    for i, (x, y, z, w, hh, d, rx, rz) in enumerate(((-12, 74, 10, 5, 16, 5, -25, 18), (-4, 76, 12, 6, 20, 6, -30, 4), (6, 75, 11, 5, 14, 5, -22, -14),
                                                     (14, 72, 10, 4, 11, 4, -18, -30), (-17, 70, 9, 4, 9, 4, -14, 34))):
        m.cube(cr, f'cristal_dos_{i + 1}', (x, y, z), (w, hh, d), CRISTAL, rot=(rx, 0, rz), origin=(x + w / 2, y, z + d / 2))

    # epaules (blocs + plaques + cristaux), bras, avant-bras, poings
    for side, sx in (('droite', 1), ('gauche', -1)):
        bside = 'droit' if sx > 0 else 'gauche'
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        ep = m.bone(f'epaule_{side}', (24 * sx, 76, 0), t)
        m.cube(ep, f'epaule_{side}_roc', (xo(20, 18), 68, -13), (18, 16, 26), ROC, share='epaule', mirror=sx < 0,
               deco={'sides': [frost_cap(3, 4.4)], 'up': [frost_cap()]})
        m.cube(ep, f'epaule_{side}_glace', (xo(21, 17), 83, -12), (17, 3, 24), GLACE, share='epaule_glace', mirror=sx < 0,
               deco={'*': [plate_edges]})
        cc = m.bone(f'cristaux_epaule_{side}', (30 * sx, 86, 0), ep)
        big = sx > 0                     # amas plus fourni sur l'epaule droite
        specs = ((25, -8, 5, 14 if big else 11, 5, -10, -14), (31, -1, 6, 18 if big else 13, 6, 6, -6), (27, 6, 4, 11, 4, 18, -20))
        if big:
            specs += ((34, -10, 4, 9, 4, -16, -30),)
        for k, (x, z, w, hh, d, rx, rz) in enumerate(specs):
            m.cube(cc, f'cristal_epaule_{side}_{k + 1}', (xo(x, w), 85, z), (w, hh, d), CRISTAL,
                   rot=(rx, 0, rz * sx), origin=(xo(x, w) + w / 2, 85, z + d / 2))
        b = m.bone(f'bras_{bside}', (30 * sx, 74, 0), ep, rot=(0, 0, 4 * sx))
        m.cube(b, f'bras_{side}_roc', (xo(23, 14), 56, -7), (14, 20, 14), ROC, share='bras', mirror=sx < 0, deco={'sides': [frost_cap(2, 5.1)]})
        fa = m.bone(f'avant_bras_{bside}', (30 * sx, 57, 0), b, rot=(10, 0, 0))
        m.cube(fa, f'avant_bras_{side}_roc', (xo(22, 16), 32, -8), (16, 26, 16), ROC, share='avant_bras', mirror=sx < 0,
               deco={'sides': [frost_cap(2, 6.7)]})
        m.cube(fa, f'brassard_{side}_glace', (xo(21, 18), 40, -9), (18, 10, 18), GLACE, share='brassard', mirror=sx < 0,
               deco={'*': [plate_edges], 'up': [frost_cap()]})
        pg = m.bone(f'poing_{bside}', (30 * sx, 33, 0), fa)
        m.cube(pg, f'poing_{side}_roc', (xo(19, 22), 10, -11), (22, 22, 22), ROC, share='poing', mirror=sx < 0,
               deco={'sides': [frost_cap(4, 7.9)], 'up': [frost_cap()], 'north': [knuckles]})
        m.cube(pg, f'poing_{side}_glace', (xo(18, 24), 24, -12), (24, 6, 24), GLACE, share='poing_glace', mirror=sx < 0,
               deco={'*': [plate_edges], 'up': [frost_cap()]})
        m.cube(pg, f'poing_{side}_cristal', (xo(26, 6), 30, -4), (6, 9, 6), CRISTAL, share='poing_cristal', mirror=sx < 0,
               rot=(0, 0, -15 * sx), origin=(xo(26, 6) + 3, 30, -1))

    # jambes : cuisse, tibia, pied tres large
    for side, sx in (('droite', 1), ('gauche', -1)):
        bside = 'droit' if sx > 0 else 'gauche'
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        lg = m.bone(f'jambe_{side}', (10 * sx, 41, 0), bs)
        m.cube(lg, f'cuisse_{side}', (xo(3, 15), 27, -7.5), (15, 15, 15), ROC, share='cuisse', mirror=sx < 0, deco={'sides': [frost_cap(2, 8.3)]})
        tb = m.bone(f'tibia_{bside}', (10.5 * sx, 27, 0), lg)
        m.cube(tb, f'tibia_{side}_roc', (xo(3, 15), 8, -7.5), (15, 19, 15), ROC, share='tibia', mirror=sx < 0, deco={'sides': [frost_cap(2, 9.1)]})
        m.cube(tb, f'genou_{side}_glace', (xo(4, 13), 20, -9.5), (13, 9, 3), GLACE, share='genou', mirror=sx < 0, deco={'*': [plate_edges]})
        pd = m.bone(f'pied_{bside}', (10.5 * sx, 9, 0), tb)
        m.cube(pd, f'pied_{side}_roc', (xo(0, 21), 0, -13), (21, 9, 25), ROC, share='pied', mirror=sx < 0,
               deco={'sides': [frost_cap(3, 10.2)], 'up': [frost_cap()]})
        for k in range(3):
            m.cube(pd, f'orteil_{side}_{k + 1}', (xo(0.5 + 7 * k, 6), 0, -16), (6, 6, 4), GLACE, share='orteil', mirror=sx < 0,
                   deco={'*': [plate_edges]})
    return m


def knuckles(f):
    """Poing : phalanges marquees."""
    for x in range(0, f.w, 5):
        for y in range(f.h // 3, f.h):
            f.px(x, y, P_ROC[0])
    for y in (f.h // 3, 2 * f.h // 3):
        for x in range(f.w):
            if f.rng.random() < 0.8:
                f.px(x, y, P_ROC[1])


# instant de l'impact de chaque attaque (s) : les degats sont appliques a ce moment en jeu
STRIKES = {'frappe_poing': 1.02, 'double_frappe_sol': 1.3, 'pietinement': 1.0, 'exposition_noyau': 1.0}

COLORS = {'racine': 0, 'bassin': 3, 'torse': 1, 'tete': 2, 'noyau': 8, 'cristaux': 5, 'epaule_droite': 4, 'epaule_gauche': 4,
          'jambe_droite': 6, 'jambe_gauche': 6}


# ============================================================================================== animations
TH, SH = 14.0, 18.0          # longueurs cuisse / tibia


def crouch(drop, spread=0.0):
    return ek.crouch(drop, spread, TH, SH)


def animations(m):
    P = ek.key_poses
    L = 'linear'
    M, SC, legs = ek.M, ek.SC, ek.legs
    down = lambda d: {'bassin': (0, -d, 0)}

    # --- reveil (4 s) : pose de sommeil accroupie (premiere image = etat dormant), frisson, se redresse, s'ebroue
    a = m.anim('reveil', 4.0, 'once')
    dort = M(crouch(12), {'torse': (-34, 0, 0), 'tete': (-28, 0, 0), 'bras_droit': (7, 0, -6), 'bras_gauche': (7, 0, 6),
                          'avant_bras_droit': (-30, 0, 0), 'avant_bras_gauche': (-30, 0, 0), 'poing_droit': (20, 0, 0), 'poing_gauche': (20, 0, 0)})
    frisson = [(0.8 + 0.12 * i, M(dort, {'torse': (0, 0, 1.2 if i % 2 else -1.2), 'cristaux': (0, 0, 3 if i % 2 else -3)}), L) for i in range(5)]
    pousse = M(crouch(7), {'torse': (-20, 0, 0), 'tete': (-8, 0, 0), 'bras_droit': (10, 0, 8), 'bras_gauche': (10, 0, -8),
                           'avant_bras_droit': (-10, 0, 0), 'avant_bras_gauche': (-10, 0, 0)})
    debout = {'torse': (10, 0, 0), 'tete': (14, 0, 0), 'bras_droit': (-12, 0, 22), 'bras_gauche': (-12, 0, -22),
              'epaule_droite': (0, 0, 6), 'epaule_gauche': (0, 0, -6), 'cristaux': (-6, 0, 0)}
    ebroue = [(3.0 + 0.12 * i, M(debout, {'torse': (0, 3 if i % 2 else -3, 0), 'cristaux': (0, 0, 4 if i % 2 else -4),
                                          'cristaux_epaule_droite': (0, 0, 5 if i % 2 else -5), 'cristaux_epaule_gauche': (0, 0, -5 if i % 2 else 5)}), L)
              for i in range(4)]
    P(a, [(0, dort), (0.8, dort)] + frisson + [(1.6, dort), (2.3, pousse), (2.9, debout)] + ebroue + [(3.5, debout), (4.0, {})])
    P(a, [(0, down(12)), (1.6, down(12)), (2.3, down(7)), (2.9, {}), (4.0, {})], 'position')
    P(a, [(0, {'noyau': SC(0.5)}), (1.6, {'noyau': SC(0.7)}), (2.9, {'noyau': SC(1.3)}), (4.0, {})], 'scale')

    # --- repos lent (4 s) : respiration profonde, epaules qui montent, regard lent
    a = m.anim('repos', 4.0, 'loop')
    P(a, [(0, {}), (1.0, {'tete': (0, 6, 0)}), (2.0, {'torse': (2, 0, 0), 'tete': (-2, 0, 0), 'bras_droit': (2, 0, 2), 'bras_gauche': (2, 0, -2),
                                                   'cristaux': (-2, 0, 0)}), (3.0, {'tete': (0, -6, 0)}), (4.0, {})])
    P(a, [(0, {}), (2.0, {'torse': (0, 0.8, 0), 'epaule_droite': (0, 0.6, 0), 'epaule_gauche': (0, 0.6, 0)}), (4.0, {})], 'position')
    P(a, [(0, {}), (2.0, {'noyau': SC(1.12)}), (4.0, {})], 'scale')

    # --- marche pesante (2,4 s) : grand transfert de poids, poings qui balancent, bassin qui s'ecrase
    a = m.anim('marche', 2.4, 'loop')
    cyc = [(18, -4, -14), (0, 0, 0), (-16, -8, 12), (8, -32, 14)]

    def wpose(i):
        r, l = cyc[i % 4], cyc[(i + 2) % 4]
        sd = {0: 1, 2: -1}.get(i % 4, 0)
        p = legs(r, l)
        p.update({'torse': (-4, -6 * sd, 4 * sd), 'tete': (3 if i % 2 == 0 else -2, 4 * sd, -3 * sd),
                  'bras_gauche': (12 * sd, 0, 0), 'bras_droit': (-12 * sd, 0, 0), 'avant_bras_droit': (4, 0, 0), 'avant_bras_gauche': (4, 0, 0),
                  'cristaux': (0, 0, -2 * sd)})
        return p
    P(a, [(0.6 * i, wpose(i)) for i in range(5)])
    P(a, [(0.6 * i, {'bassin': (0, -0.8 if i % 2 == 0 else 1, 0)}) for i in range(5)], 'position')

    # --- frappe d'un poing (2,2 s) : bras droit arme en arriere, coup droit, recuperation
    a = m.anim('frappe_poing', 2.2, 'once')
    arme = M(crouch(3), {'torse': (4, -26, 0), 'tete': (0, 18, 0), 'bras_droit': (-40, 0, 10), 'avant_bras_droit': (55, 0, 0),
                         'bras_gauche': (25, 0, -12), 'epaule_droite': (0, 0, 8)})
    coup = M(crouch(3), {'torse': (-10, 22, 0), 'tete': (-2, -14, 0), 'bras_droit': (82, 12, 0), 'avant_bras_droit': (-10, 0, 0),
                         'poing_droit': (-15, 0, 0), 'bras_gauche': (-15, 0, -14), 'epaule_droite': (0, 0, -4)})
    P(a, [(0, {}), (0.75, arme), (0.85, arme, L), (1.02, coup, L), (1.35, coup), (2.2, {})])
    P(a, [(0, {}), (0.75, down(3)), (1.02, M(down(3), {'racine': (0, 0, -2)}), L), (1.35, M(down(3), {'racine': (0, 0, -2)})), (2.2, {})], 'position')

    # --- double frappe au sol (2,8 s) : deux poings leves puis abattus devant lui
    a = m.anim('double_frappe_sol', 2.8, 'once')
    leve = {'torse': (10, 0, 0), 'tete': (10, 0, 0), 'bras_droit': (165, 0, -12), 'bras_gauche': (165, 0, 12),
            'avant_bras_droit': (25, 0, 0), 'avant_bras_gauche': (25, 0, 0), 'cristaux': (-6, 0, 0)}
    impact = M(crouch(8, spread=5), {'torse': (-34, 0, 0), 'tete': (20, 0, 0), 'bras_droit': (58, 0, -8), 'bras_gauche': (58, 0, 8),
                                     'avant_bras_droit': (-5, 0, 0), 'avant_bras_gauche': (-5, 0, 0), 'cristaux': (6, 0, 0)})
    shake = [(1.38 + 0.08 * i, M(impact, {'torse': (0, 0, 1.4 if i % 2 else -1.4)}), L) for i in range(4)]
    P(a, [(0, {}), (0.9, leve), (1.05, leve, L), (1.3, impact, L)] + shake + [(1.8, impact), (2.8, {})])
    P(a, [(0, {}), (0.9, {'racine': (0, 1, 0)}), (1.05, {'racine': (0, 1, 0)}, L), (1.3, down(8), L), (1.8, down(8)), (2.8, {})], 'position')

    # --- pietinement (2,2 s) : jambe droite levee haut, ecrasement, onde (au sol : code du jeu)
    a = m.anim('pietinement', 2.2, 'once')
    leve = {'jambe_droite': (50, 0, 6), 'tibia_droit': (-55, 0, 0), 'pied_droit': (10, 0, -6), 'jambe_gauche': (-4, 0, 0), 'pied_gauche': (4, 0, 0),
            'torse': (6, 0, -8), 'tete': (6, 0, 6), 'bras_droit': (20, 0, 30), 'bras_gauche': (10, 0, -34), 'cristaux': (0, 0, 4)}
    ecrase = M(crouch(6, spread=6), {'torse': (-14, 0, 2), 'tete': (14, 0, 0), 'bras_droit': (-10, 0, 26), 'bras_gauche': (-10, 0, -26),
                                     'cristaux': (4, 0, 0)})
    shake = [(1.08 + 0.08 * i, M(ecrase, {'torse': (0, 0, 2 + (1.2 if i % 2 else -1.2))}), L) for i in range(4)]
    P(a, [(0, {}), (0.8, leve), (0.88, leve, L), (1.0, ecrase, L)] + shake + [(1.4, ecrase), (2.2, {})])
    P(a, [(0, {}), (0.8, {'racine': (-1.5, 1, 0)}), (0.88, {'racine': (-1.5, 1, 0)}, L), (1.0, down(6), L), (1.4, down(6)), (2.2, {})], 'position')

    # --- exposition du noyau (4 s) : les plaques s'ouvrent, le titan se cambre, le noyau grossit ; fenetre 1,0 -> 3,0 s
    a = m.anim('exposition_noyau', 4.0, 'once')
    ouvert = {'plaque_droite': (0, -105, 0), 'plaque_gauche': (0, 105, 0), 'plaque_ventre': (-80, 0, 0), 'torse': (12, 0, 0), 'tete': (16, 0, 0),
              'bras_droit': (-18, 0, 30), 'bras_gauche': (-18, 0, -30), 'epaule_droite': (0, 0, 6), 'epaule_gauche': (0, 0, -6), 'cristaux': (-8, 0, 0)}
    souffle = M(ouvert, {'torse': (2, 0, 0), 'plaque_droite': (0, 5, 0), 'plaque_gauche': (0, -5, 0)})
    P(a, [(0, {}), (0.4, {'torse': (-6, 0, 0), 'tete': (-6, 0, 0)}), (1.0, ouvert), (2.0, souffle), (3.0, ouvert), (3.6, {'torse': (-4, 0, 0)}), (4.0, {})])
    P(a, [(0, {}), (1.0, {'noyau': (0, 0, -2)}), (3.0, {'noyau': (0, 0, -2)}), (3.6, {}), (4.0, {})], 'position')
    P(a, [(0, {}), (1.0, {'noyau': SC(1.4)}), (1.5, {'noyau': SC(1.55)}), (2.0, {'noyau': SC(1.4)}), (2.5, {'noyau': SC(1.55)}), (3.0, {'noyau': SC(1.4)}),
          (3.6, {}), (4.0, {})], 'scale')

    # --- mort par effondrement (4,6 s, figee) : le noyau vacille, les genoux cedent, le titan s'ecroule en avant
    a = m.anim('mort', 4.6, 'hold')
    vacille = {'torse': (8, 0, -6), 'tete': (14, 0, 8), 'bras_droit': (-10, 0, 20), 'bras_gauche': (-6, 0, -12), 'plaque_droite': (0, -25, 0)}
    haut = {'torse': (-20, 0, 4), 'tete': (-24, 0, 6), 'bras_droit': (78, 0, 6), 'bras_gauche': (78, 0, -6),
            'avant_bras_droit': (-30, 0, 0), 'avant_bras_gauche': (-30, 0, 0), 'plaque_droite': (0, -60, 0), 'plaque_gauche': (0, 20, 0)}
    genoux = M(crouch(20), haut)
    sol = M(haut, legs((-6, -10, -10), (6, -6, -10)), {'racine': (-82, 0, 0), 'torse': (6, 0, -4), 'tete': (34, 0, 0),
                                                       'bras_droit': (102, 0, 30), 'bras_gauche': (102, 0, -30), 'avant_bras_droit': (20, 0, 0),
                                                       'avant_bras_gauche': (20, 0, 0), 'plaque_droite': (0, -40, 30),
                                                       'cristaux': (10, 0, 6), 'cristaux_epaule_droite': (0, 0, 14), 'cristaux_epaule_gauche': (0, 0, -14)})
    rebond = M(sol, {'racine': (3, 0, 0)})
    P(a, [(0, {}), (0.5, vacille), (1.1, M(crouch(9), {'torse': (-8, 0, 0), 'tete': (-8, 0, 4), 'bras_droit': (45, 0, 6), 'bras_gauche': (45, 0, -6), 'avant_bras_droit': (-15, 0, 0), 'avant_bras_gauche': (-15, 0, 0)})), (1.6, genoux), (2.4, M(genoux, {'torse': (-4, 0, -4)})),
          (3.1, sol, L), (3.3, rebond), (3.55, sol), (4.6, sol)])
    kneel = down(20)
    P(a, [(0, {}), (0.5, {}), (1.1, down(9)), (1.6, kneel), (2.4, kneel), (2.75, M(kneel, {'racine': (0, 18, 0)}), L), (3.1, M(kneel, {'racine': (0, 20.5, 0)}), L),
          (3.3, M(kneel, {'racine': (0, 21.5, 0)})), (3.55, M(kneel, {'racine': (0, 20.5, 0)})), (4.6, M(kneel, {'racine': (0, 20.5, 0)}))], 'position')
    P(a, [(0, {}), (0.3, {'noyau': SC(1.4)}), (0.5, {'noyau': SC(0.7)}), (0.7, {'noyau': SC(1.2)}), (1.1, {'noyau': SC(0.8)}),
          (1.6, {'noyau': SC(0.6)}), (3.1, {'noyau': SC(0.4)}), (4.6, {'noyau': SC(0.35)})], 'scale')
