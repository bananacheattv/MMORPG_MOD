"""Genere les 20 cosmetiques 3D « lot 2 » (coiffes, visage, epaules, dos) : JSON + texture palette.
Usage : python3 tools/gen_cosmetics_3d.py [cle ...]   (sans argument : tous)
Convention : voir tools/cosmlib.py. Repere coiffe = dessus de la tete a y=0 ; visage = centre du visage, z=0 contre le front ;
epaules = haut du torse ; dos = surface du dos, 4 px sous la nuque.
"""
import json
import math
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from cosmlib import Model, mix, shade, rgb, model, BUILD, CATS  # noqa: E402

TAU = 2 * math.pi
def sine(period_amp, phase=0, base=0.0):
    return lambda p: base + period_amp * math.sin(TAU * (p + phase))


# ============================================================ COIFFES ============================================================
@model('couronne_solaire', 'coiffe')
def couronne_solaire():
    m = Model('couronne_solaire')
    gold, dark, light, ruby = '#F0B830', '#A8701A', '#FFE680', '#E02828'
    root = m.group(None, (0, 0, 0), name='root')
    # bandeau : anneau octogonal evide (4 barres + 4 diagonales)
    for y0, h, col in ((0, 0.5, dark), (0.5, 1.5, gold), (2.0, 0.5, light)):
        m.cube(root, (-5, y0, -5), (5, y0 + h, -4.3), col)
        m.cube(root, (-5, y0, 4.3), (5, y0 + h, 5), col)
        m.cube(root, (-5, y0, -4.3), (-4.3, y0 + h, 4.3), col)
        m.cube(root, (4.3, y0, -4.3), (5, y0 + h, 4.3), col)
    for sx in (-1, 1):
        for sz in (-1, 1):  # angles chanfreines
            m.cube(root, (sx * 5.2 - .5, 0, sz * 5.2 - .5), (sx * 5.2 + .5, 2.5, sz * 5.2 + .5), dark)
    # studs
    for i in range(-3, 4, 2):
        m.cube(root, (i - .35, 0.9, -5.15), (i + .35, 1.6, -5), light)
    # rayons : 8 directions, longs aux 4 points cardinaux
    rays = []
    for k in range(8):
        ang = k * 45
        long = (k % 2 == 0)
        g = m.group(root, (0, 2.4, 0), (0, ang, 0), name=f'ray{k}')
        tilt = m.group(g, (0, 2.4, -4.9), (-10, 0, 0), name=f'tilt{k}')
        hh = (5.0, 4.0, 3.0) if long else (3.2, 2.4, 1.6)
        ww = (2.3, 1.6, .9) if long else (1.8, 1.2, .7)
        y = 2.4
        for i in range(3):
            c = mix(gold, light, i / 2)
            m.cube(tilt, (-ww[i] / 2, y, -4.9 - ww[i] / 3), (ww[i] / 2, y + hh[i] * .5, -4.9 + ww[i] / 3), c)
            y += hh[i] * .5
        # pointe claire
        m.cube(tilt, (-.25, y, -4.9 - .2), (.25, y + .6, -4.9 + .2), '#FFFFFF')
        rays.append((g, k))
    # soleil central : losange dore + rubis
    m.cube(root, (-1.5, 0.3, -5.5), (1.5, 3.3, -5.1), gold, rot=(0, 0, 45))
    gem = m.group(root, (0, 1.8, -5.45), name='gem')
    m.cube(gem, (-.85, 0.95, -5.7), (.85, 2.65, -5.3), ruby, rot=(0, 0, 45))
    m.cube(gem, (-.35, 1.4, -5.75), (.35, 2.1, -5.55), '#FFB0A0')
    L = 3
    tr = []
    for g, k in rays:
        ph = 0 if k % 2 == 0 else .5
        tr.append((g, 's', m.wave(L, lambda p, ph=ph: (1, 1 + .12 * math.sin(TAU * (p + ph)), 1))))
    tr.append((gem, 's', m.wave(L, lambda p: (1 + .14 * math.sin(TAU * p), 1 + .14 * math.sin(TAU * p), 1))))
    m.anim('idle', L, tr)
    return m


@model('casque_valkyrie', 'coiffe')
def casque_valkyrie():
    m = Model('casque_valkyrie')
    steel, dark, light, gold = '#B8C4D4', '#6C7A90', '#E8F0FA', '#E8B030'
    root = m.group(None, name='root')
    radii = [(0, 1.6, 5.0), (1.6, 1.4, 4.9), (3.0, 1.2, 4.4), (4.2, 1.0, 3.6), (5.2, .9, 2.6), (6.1, .6, 1.5)]
    for y0, h, r in radii:
        m.octagon(root, 0, y0, 0, r, h, mix(steel, light, y0 / 8), bottom=dark)
    m.cube(root, (-5.15, 0, -5.15), (5.15, .55, 5.15), gold)                      # liseré dore
    m.cube(root, (-.5, 0.55, -5.3), (.5, 6.4, -4.2), gold)                          # arete frontale
    m.cube(root, (-.35, 6.5, -.35), (.35, 7.6, .35), gold)                          # pointe
    m.cube(root, (-.45, 0.55, -4.45), (.45, 3.2, -5.1), dark)                       # nasal
    # protege-joues
    for s in (-1, 1):
        m.cube(root, (s * 4.7 - .5, -3.4, -2.8), (s * 4.7 + .5, 0, 2.8), steel)
        m.cube(root, (s * 4.75 - .55, -3.5, -3), (s * 4.75 + .55, -2.8, 3), gold)
    # ailes
    wings = []
    for s in (-1, 1):
        w = m.group(root, (s * 5.1, 4.0, 0), (0, 0, 0), name=f'wing{s}')
        wings.append((w, s))
        for i, (ln, ang, col) in enumerate(((7.0, 60, '#FFFFFF'), (6.2, 38, '#F2F6FF'), (5.3, 16, '#E4ECF8'), (4.4, -6, '#D2DDF0'))):
            a = ang * s
            m.cube(w, (s * 5.1, 3.55 - i * .1, -.5), (s * 5.1 + s * ln, 4.45 - i * .1, .5), col, rot=(0, 0, a), pivot=(s * 5.1, 4.0, 0))
            m.cube(w, (s * 5.1 + s * ln * .55, 3.65 - i * .1, -.45), (s * 5.1 + s * ln, 4.35 - i * .1, .45), mix(col, '#9FB4E0', .45), rot=(0, 0, a), pivot=(s * 5.1, 4.0, 0))
        m.cube(w, (s * 4.9, 3.5, -.7), (s * 5.7, 5.2, .7), gold)
    L = 2.4
    m.anim('idle', L, [(w, 'r', m.wave(L, lambda p, s=s: (0, 0, s * 4 * math.sin(TAU * p)))) for w, s in wings])
    return m


@model('tricorne_corsaire', 'coiffe')
def tricorne():
    m = Model('tricorne_corsaire')
    felt, fold, gold, bone, red = '#2A2430', '#18141E', '#E0A830', '#F0E8D8', '#C02030'
    root = m.group(None, name='root')
    # calotte
    m.octagon(root, 0, 0, 0, 4.7, 1.0, felt, bottom=fold)
    m.octagon(root, 0, 1.0, 0, 4.7, 1.2, felt)
    m.octagon(root, 0, 2.2, 0, 4.4, 1.0, felt)
    m.octagon(root, 0, 3.2, 0, 3.8, .8, '#352E3E')
    m.cube(root, (-4.85, 1.6, -4.85), (4.85, 2.2, 4.85), '#5A4026')                  # ruban de cuir
    m.cube(root, (-1, 1.5, -5.05), (1, 2.3, -4.8), gold)                              # boucle
    # trois pans releves (gauche, droit, arriere)
    for key, (axis, s) in {'l': ('x', -1), 'r': ('x', 1), 'b': ('z', 1)}.items():
        up = m.group(root, (0, 2.2, 0), name='pan' + key)
        if axis == 'x':
            piv = (s * 4.6, 2.0, 0)
            m.cube(up, (s * 4.6, 1.8, -5.6), (s * 10.0, 2.5, 5.6), felt, rot=(0, 0, s * 32), pivot=piv, bottom=fold)
            m.cube(up, (s * 9.6, 1.6, -5.7), (s * 10.0, 2.7, 5.7), gold, rot=(0, 0, s * 32), pivot=piv)
        else:
            piv = (0, 2.0, 4.6)
            m.cube(up, (-5.6, 1.8, 4.6), (5.6, 2.5, 9.6), felt, rot=(-32, 0, 0), pivot=piv, bottom=fold)
            m.cube(up, (-5.7, 1.6, 9.2), (5.7, 2.7, 9.6), gold, rot=(-32, 0, 0), pivot=piv)
    # pan avant (visiere courte)
    m.cube(root, (-5.2, 0.2, -8.2), (5.2, 0.9, -4.6), felt, rot=(14, 0, 0), pivot=(0, 0.6, -4.6), bottom=fold)
    m.cube(root, (-5.25, 0.15, -8.25), (5.25, .3, -4.6), gold, rot=(14, 0, 0), pivot=(0, 0.6, -4.6))
    # crane + os croises sur la calotte avant
    m.cube(root, (-1.2, 1.9, -5.35), (1.2, 4.0, -5.05), bone)
    m.cube(root, (-.9, 1.5, -5.35), (.9, 2.0, -5.05), bone)
    m.cube(root, (-.9, 3.0, -5.4), (-.2, 3.6, -5.2), '#121018')
    m.cube(root, (.2, 3.0, -5.4), (.9, 3.6, -5.2), '#121018')
    m.cube(root, (-.15, 2.3, -5.4), (.15, 2.8, -5.2), '#121018')
    # plume rouge
    plume = m.group(root, (4.6, 3.0, -1.5), name='plume')
    cur = plume
    segs = []
    for i in range(5):
        g = m.group(cur, (4.8 + i * .5, 3.0 + i * 1.2, -1.5 + i * .35), (0, 0, -6), name=f'p{i}')
        w = 1.9 - i * .27
        m.cube(g, (4.8 + i * .5 - w / 2, 3.0 + i * 1.2, -1.5 + i * .35 - .35), (4.8 + i * .5 + w / 2, 4.3 + i * 1.2, -1.5 + i * .35 + .35), mix(red, '#FFD0A0', i / 6))
        segs.append(g)
        cur = g
    L = 2.6
    m.anim('idle', L, [(g, 'r', m.wave(L, lambda p, i=i: (0, 0, -6 + 5 * math.sin(TAU * (p - i * .08))))) for i, g in enumerate(segs)])
    return m


@model('couronne_lauriers', 'coiffe')
def couronne_lauriers():
    m = Model('couronne_lauriers')
    leaf1, leaf2, gold, berry = '#4FA83A', '#7CC84A', '#F2C040', '#D8402A'
    root = m.group(None, name='root')
    m.cube(root, (-4.9, 0.3, -4.9), (4.9, 1.0, 4.9), '#6A4A24')                    # fil/branche
    leaves = []
    n = 16
    for k in range(n):
        a = k * 360 / n
        g = m.group(root, (0, 0.8, 0), (0, a, 0), name=f'leafgrp{k}')
        for side, (dy, col) in enumerate(((0.55, leaf1), (-0.1, leaf2))):
            sx = 1 if side == 0 else -1
            m.cube(g, (-0.9, .45 + side * .4, -5.2 - 1.1), (0.9, 1.25 + side * .4, -5.2 + .15), mix(col, '#FFFFFF', .05 * (k % 3)), rot=(0, sx * 28, 0), pivot=(0, .8, -5.0))
            m.cube(g, (-.35, 1.0 + side * .4, -6.0), (.35, 1.5 + side * .4, -5.3), mix(col, '#E8F8C0', .35), rot=(0, sx * 28, 0), pivot=(0, .8, -5.0))
        leaves.append(g)
    # baies dorees + ruban nouee derriere
    for k in range(0, n, 4):
        a = math.radians(k * 360 / n + 11)
        m.cube(root, (math.sin(a) * 5.2 - .45, .5, -math.cos(a) * 5.2 - .45), (math.sin(a) * 5.2 + .45, 1.4, -math.cos(a) * 5.2 + .45), gold)
    rib = []
    for s in (-1, 1):
        g = m.group(root, (s * .4, .6, 5.1), (0, 0, 0), name=f'rib{s}')
        m.cube(g, (s * .4 - .5, -6.5, 5.1), (s * .4 + .5, .6, 5.5), '#C02030', rot=(0, 0, s * 8), pivot=(s * .4, .6, 5.1))
        m.cube(g, (s * .4 - .5, -7.2, 5.1), (s * .4 + .5, -6.5, 5.5), '#8C1422', rot=(0, 0, s * 8), pivot=(s * .4, .6, 5.1))
        rib.append((g, s))
    m.cube(root, (-.9, -.2, 4.9), (.9, 1.6, 5.7), '#E8B030')
    # baie front brillante
    star = m.group(root, (0, 1, -5.3), name='star')
    m.cube(star, (-.6, .4, -5.8), (.6, 1.6, -5.2), '#FFE680', rot=(0, 0, 45))
    L = 3
    m.anim('idle', L, [(g, 'r', m.wave(L, lambda p, k=k: (0, 0, 0))) for k, g in enumerate(leaves[:1])] +
           [(g, 'r', m.wave(L, lambda p, s=s: (4 * math.sin(TAU * p), 0, s * 6 + s * 3 * math.sin(TAU * p)))) for g, s in rib] +
           [(star, 's', m.wave(L, lambda p: (1 + .25 * math.sin(TAU * p),) * 3))])
    return m


@model('chapeau_sorciere', 'coiffe')
def chapeau_sorciere():
    m = Model('chapeau_sorciere')
    cloth, dark, band, gold = '#5A2E96', '#3C1E68', '#2A1648', '#F2C040'
    root = m.group(None, name='root')
    # bord large legerement ondule
    m.octagon(root, 0, -0.4, 0, 7.4, .9, dark, bottom=shade(dark, .6))
    m.octagon(root, 0, 0.5, 0, 6.2, .5, cloth)
    # cone : empilement qui s'amincit et se penche
    stack = m.group(root, (0, 1, 0), name='tip0')
    cur = stack
    pieces = []
    r = 4.4
    y = 1.0
    for i in range(6):
        g = m.group(cur, (0, y, 0), (0, 0, -3 if i > 1 else 0), name=f'cone{i}')
        h = 2.0
        m.octagon(g, i * 0.0, y, 0, r, h, mix(cloth, '#8A5AD0', i / 8))
        pieces.append(g)
        cur = g
        y += h
        r = max(.7, r * .76)
    # bout du chapeau : petite etoile / pointe
    m.cube(cur, (-.45, y, -.45), (.45, y + 1.4, .45), '#B58AF0')
    # ruban + boucle doree
    m.octagon(root, 0, 1.0, 0, 4.55, 1.3, band)
    m.cube(root, (-1.3, 1.0, -4.9), (1.3, 2.4, -4.55), gold)
    m.cube(root, (-.7, 1.4, -5.0), (.7, 2.0, -4.85), '#6A3A10')
    # croissant argente sur le cone
    m.cube(root, (-.5, 4.3, -3.8), (.5, 5.6, -3.4), '#E6EEFF')
    m.cube(root, (-.1, 4.5, -3.82), (.9, 5.4, -3.45), '#5A2E96')
    # etoiles flottantes
    stars = []
    for k, (sx, sy, sz) in enumerate(((5.2, 6, 3.5), (-4.8, 8.5, -2), (2.5, 11, -4))):
        g = m.group(root, (sx, sy, sz), name=f'st{k}')
        m.cube(g, (sx - .5, sy - .5, sz - .5), (sx + .5, sy + .5, sz + .5), '#FFE680', rot=(45, 45, 0))
        stars.append(g)
    L = 3.2
    m.anim('idle', L, [(g, 'r', m.wave(L, lambda p, i=i: (0, 0, (-3 if i > 1 else 0) + 3.5 * math.sin(TAU * (p - i * .07))))) for i, g in enumerate(pieces)] +
           [(g, 'p', m.wave(L, lambda p, k=k: (0, .8 * math.sin(TAU * (p + k / 3)), 0))) for k, g in enumerate(stars)] +
           [(g, 'r', m.wave(L, lambda p: (0, 360 * p, 0))) for g in stars])
    return m


@model('kabuto_samourai', 'coiffe')
def kabuto():
    m = Model('kabuto_samourai')
    red, dark, gold, black, lace = '#B02828', '#701818', '#E8B030', '#201820', '#E8E0C8'
    root = m.group(None, name='root')
    for y0, h, r, col in ((0, 1.3, 5.0, red), (1.3, 1.3, 4.9, red), (2.6, 1.2, 4.3, '#C03030'), (3.8, 1.0, 3.4, '#C83A3A'), (4.8, .8, 2.2, '#D04444')):
        m.octagon(root, 0, y0, 0, r, h, col, bottom=dark)
    m.cube(root, (-5.1, 0, -5.1), (5.1, .5, 5.1), gold)                              # rebord dore
    m.cube(root, (-5.1, 1.2, -5.1), (5.1, 1.5, 5.1), black)
    # rivets
    for i in range(8):
        a = math.radians(i * 45)
        m.cube(root, (math.sin(a) * 5.0 - .3, 2.0, -math.cos(a) * 5.0 - .3), (math.sin(a) * 5.0 + .3, 2.6, -math.cos(a) * 5.0 + .3), gold)
    # visiere
    m.cube(root, (-5.2, -.4, -7.0), (5.2, .5, -4.8), black, rot=(12, 0, 0), pivot=(0, .4, -4.8))
    m.cube(root, (-5.25, .4, -7.05), (5.25, .6, -4.8), gold, rot=(12, 0, 0), pivot=(0, .4, -4.8))
    # nuquiere (jupe a lamelles) : 3 bandes etagees
    for i, (y, d) in enumerate(((-.5, 6.4), (-2.3, 7.4), (-4.1, 8.2))):
        c = mix(red, dark, i / 3)
        m.cube(root, (-5.6 - i * .3, y - 1.8, 4.3), (5.6 + i * .3, y, d), c, rot=(-10 - i * 4, 0, 0), pivot=(0, y, 4.3))
        m.cube(root, (-5.6 - i * .3, y - 1.8, 4.3), (5.6 + i * .3, y - 1.6, d + .1), lace, rot=(-10 - i * 4, 0, 0), pivot=(0, y, 4.3))
    for s in (-1, 1):
        for i, (y, d) in enumerate(((-.2, 3.4), (-1.9, 3.9))):                          # cotes (fukigaeshi)
            m.cube(root, (s * 5.0, y - 1.8, -3.5), (s * (5.6 + i * .5 + d * .2), y, 4.2), mix(red, dark, i / 2), rot=(0, 0, s * (-20 - i * 6)), pivot=(s * 5.0, y, 0))
    # maedate : croissant dore (corne en lune)
    cres = m.group(root, (0, 5.4, -1.5), name='crest')
    m.cube(cres, (-.45, 5.4, -2.2), (.45, 7.0, -1.2), gold)
    for s in (-1, 1):
        m.cube(cres, (s * 1.2 - .4, 6.1, -2.2), (s * 1.2 + .4, 8.9, -1.2), gold, rot=(0, 0, -s * 22), pivot=(s * .6, 6.1, -1.7))
        m.cube(cres, (s * 2.6 - .4, 8.4, -2.2), (s * 2.6 + .4, 11.0, -1.2), '#FFE680', rot=(0, 0, -s * 40), pivot=(s * .6, 6.1, -1.7))
    m.cube(cres, (-.9, 5.0, -2.3), (.9, 6.2, -1.1), gold)
    m.cube(cres, (-.4, 5.3, -2.5), (.4, 6.0, -2.2), '#FFFFFF')
    L = 3
    m.anim('idle', L, [(cres, 'r', m.wave(L, lambda p: (0, 0, 1.6 * math.sin(TAU * p)))),
                       (cres, 's', m.wave(L, lambda p: (1, 1 + .03 * math.sin(TAU * p), 1)))])
    return m


@model('diademe_lune', 'coiffe')
def diademe_lune():
    m = Model('diademe_lune')
    silver, light, blue, gold = '#C8D4E8', '#F4F8FF', '#6AA8FF', '#FFE070'
    root = m.group(None, name='root')
    for sx, c in ((-1, 0), (1, 1)):
        pass
    m.cube(root, (-4.9, .3, -4.9), (4.9, 1.1, -4.4), silver)
    m.cube(root, (-4.9, .3, 4.4), (4.9, 1.1, 4.9), silver)
    m.cube(root, (-4.9, .3, -4.4), (-4.4, 1.1, 4.4), silver)
    m.cube(root, (4.4, .3, -4.4), (4.9, 1.1, 4.4), silver)
    for a_, b_ in (((-4.95, -4.95), (4.95, -4.4)), ((-4.95, 4.4), (4.95, 4.95)), ((-4.95, -4.4), (-4.4, 4.4)), ((4.4, -4.4), (4.95, 4.4))):
        m.cube(root, (a_[0], .75, a_[1]), (b_[0], 1.0, b_[1]), light)
    # montee en pointe vers l'avant : croissant + gemme
    for i, (w, h) in enumerate(((3.6, .8), (2.8, .8), (2.0, .8))):
        m.cube(root, (-w / 2, 1.1 + i * .8, -5.0), (w / 2, 1.9 + i * .8, -4.5), mix(silver, light, i / 2))
    moon = m.group(root, (0, 5.0, -4.8), name='moon')
    # croissant : arc de cubes (pointes vers le haut)
    for i in range(9):
        t = (i - 4) / 4 * 1.25
        x, y = math.sin(t) * 3.2, 5.0 - math.cos(t) * 3.2 + 1.9
        m.cube(moon, (x - .55, y - .55, -5.5), (x + .55, y + .55, -4.9), light if abs(i - 4) < 3 else '#FFFFFF')
    m.cube(moon, (-.5, 5.2, -5.3), (.5, 6.2, -4.95), '#9CC4FF')
    m.cube(root, (-.6, 1.4, -5.4), (.6, 2.6, -4.9), blue, rot=(0, 0, 45))
    # perles pendantes lateral
    for s in (-1, 1):
        for i in range(3):
            m.cube(root, (s * 5.0 - .35, -.2 - i * 1.0, -1.5 + i * .2 - .35), (s * 5.0 + .35, .5 - i * 1.0, -1.5 + i * .2 + .35), mix(light, blue, i / 3))
    # etoiles en orbite
    orb = m.group(root, (0, 4.5, 0), name='orbit')
    for k in range(4):
        a = math.radians(k * 90)
        m.cube(orb, (math.sin(a) * 6.8 - .5, 3.8 + (k % 2) * 1.2, -math.cos(a) * 6.8 - .5), (math.sin(a) * 6.8 + .5, 4.8 + (k % 2) * 1.2, -math.cos(a) * 6.8 + .5), gold if k % 2 else '#FFFFFF', rot=(45, 45, 0))
    L = 6
    m.anim('idle', L, [(orb, 'r', m.wave(L, lambda p: (0, 360 * p, 0), 24)), (orb, 'p', m.wave(L, lambda p: (0, .6 * math.sin(TAU * 2 * p), 0), 24)),
                       (moon, 's', m.wave(L, lambda p: (1,) + (1 + .05 * math.sin(TAU * 2 * p),) * 2, 24))])
    return m


import cosm_models_b  # noqa: E402,F401  (visage, epaules, dos)

if __name__ == '__main__':
    want = sys.argv[1:] or list(BUILD)
    for k in want:
        BUILD[k]().save()
        print('ok', k)
    (pathlib.Path(__file__).parent / 'cosm_cats.json').write_text(json.dumps(CATS))
