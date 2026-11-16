"""Cosmetiques 3D lot 2 (suite) : visage, epaules, dos. Voir gen_cosmetics_3d.py."""
import math

from cosmlib import Model, mix, shade, model

TAU = 2 * math.pi


def disc(m, parent, cx, cy, z0, z1, r, color, **kw):
    """Disque octogonal dans le plan xy (axe z)."""
    m.cube(parent, (cx - r, cy - r, z0), (cx + r, cy + r, z1), color, **kw)
    m.cube(parent, (cx - r, cy - r, z0 + .005), (cx + r, cy + r, z1 - .005), color, rot=(0, 0, 45), pivot=(cx, cy, (z0 + z1) / 2), **kw)


# ============================================================ VISAGE ============================================================
@model('masque_venitien', 'visage')
def masque_venitien():
    m = Model('masque_venitien')
    por, gold, deep, blue = '#F4EEE4', '#E8B838', '#2A1840', '#4A7CE0'
    root = m.group(None, name='root')
    z0, z1 = -1.1, .2
    m.cube(root, (-4.4, .6, z0), (4.4, 2.6, z1), por)                                        # arcade sourciliere
    m.cube(root, (-4.4, .2, z0), (-3.2, .6, z1), por); m.cube(root, (3.2, .2, z0), (4.4, .6, z1), por)
    for s in (-1, 1):
        m.cube(root, (s * 3.3 - .55, -1.5, z0), (s * 3.3 + .55, .6, z1), por)                # bords exterieurs
        m.cube(root, (s * 4.0 - .4, -2.4, z0), (s * 4.0 + .4, -1.5, z1), por)
        m.cube(root, (s * 1.2 - .5, -2.6, z0), (s * 3.4 + .5, -1.5, z1), por, rot=(0, 0, s * -8), pivot=(s * 2.3, -2.0, 0))
        # cadres dores des yeux
        m.cube(root, (s * .9 - .15, -1.5, z0 - .12), (s * 3.2 + .15, -1.3, z0), gold)
        m.cube(root, (s * .9 - .15, .4, z0 - .12), (s * 3.2 + .15, .6, z0), gold)
        m.cube(root, (s * 3.2 - .15, -1.5, z0 - .12), (s * 3.2 + .15, .6, z0), gold)
        m.cube(root, (s * .9 - .15, -1.5, z0 - .12), (s * .9 + .15, .6, z0), gold)
        # volutes dorees sur l'arcade
        m.cube(root, (s * 1.0, 1.4, z0 - .12), (s * 3.9, 1.7, z0), gold)
        m.cube(root, (s * 3.6, 1.0, z0 - .12), (s * 3.9, 2.4, z0), gold)
        m.cube(root, (s * 1.0, 1.4, z0 - .12), (s * 1.3, 2.3, z0), gold)
    m.cube(root, (-.9, -1.4, z0), (.9, .6, z1), por)                                          # arete du nez
    m.cube(root, (-.7, -2.7, z0 - .5), (.7, -1.3, z1), por)
    m.cube(root, (-.5, -2.9, z0 - .6), (.5, -2.5, z0 + .2), gold)
    for i, (w) in enumerate((4.4, 3.4)):
        m.cube(root, (-w, 2.6 + i * .0, z0), (w, 2.9, z1), gold)                              # liseré haut
    g = m.group(root, (0, 1.6, z0 - .1), name='gem')
    m.cube(g, (-.6, 1.0, z0 - .5), (.6, 2.2, z0 - .1), '#D82838', rot=(0, 0, 45))
    # plume du coté droit
    pl = []
    cur = m.group(root, (4.2, 2.4, -.5), name='plume')
    cols = ['#F8F8FF', '#BBD0FF', '#8AA8F0', '#6A88E0', '#4A68C8']
    for i in range(5):
        g2 = m.group(cur, (4.2 + i * .75, 2.4 + i * 1.3, -.6), (0, 0, -9), name=f'pf{i}')
        m.cube(g2, (4.2 + i * .75 - .9 + (i * .1), 2.4 + i * 1.3, -.9), (4.2 + i * .75 + .9 - (i * .12), 3.8 + i * 1.3, -.2), cols[i])
        pl.append(g2)
        cur = g2
    # petits pendentifs
    for s in (-1, 1):
        m.cube(root, (s * 4.4 - .3, -3.6, -.9), (s * 4.4 + .3, -2.4, -.3), gold)
        m.cube(root, (s * 4.4 - .45, -4.2, -.95), (s * 4.4 + .45, -3.5, -.25), blue, rot=(0, 0, 45))
    L = 3
    m.anim('idle', L, [(g2, 'r', m.wave(L, lambda p, i=i: (0, 0, -9 + 6 * math.sin(TAU * (p - i * .09))))) for i, g2 in enumerate(pl)] +
           [(g, 's', m.wave(L, lambda p: (1 + .15 * math.sin(TAU * p),) * 3))])
    return m


@model('masque_oni', 'visage')
def masque_oni():
    m = Model('masque_oni')
    red, dark, cream, black, yel = '#C82A2A', '#7A1418', '#F0E4C8', '#140A10', '#FFD030'
    root = m.group(None, name='root')
    z0, z1 = -1.3, .2
    m.cube(root, (-4.5, -4.3, z0), (4.5, 4.3, z1), red)
    m.cube(root, (-4.7, -4.3, z0 + .5), (-4.4, 4.3, z1), dark); m.cube(root, (4.4, -4.3, z0 + .5), (4.7, 4.3, z1), dark)
    # arcades sourcilieres en colere
    for s in (-1, 1):
        m.cube(root, (s * .3, 1.4, z0 - 1.0), (s * 4.5, 2.5, z0 + .2), dark, rot=(0, 0, s * -20), pivot=(s * 2.4, 1.9, z0))
        m.cube(root, (s * .9, -.9, z0 - .08), (s * 3.3, .8, z0 + .1), black, rot=(0, 0, s * -12), pivot=(s * 2.1, 0, z0))
        m.cube(root, (s * 1.6, -.6, z0 - .12), (s * 2.6, .4, z0 - .05), yel, rot=(0, 0, s * -12), pivot=(s * 2.1, 0, z0))
        # joues + crocs lateraux
        m.cube(root, (s * 2.8, -3.4, z0 - .5), (s * 4.5, -1.2, z0 + .3), shade(red, .88))
        m.cube(root, (s * 3.0 - .4, -3.9, z0 - .6), (s * 3.0 + .4, -2.9, z0 - .1), cream)
    m.cube(root, (-.8, -2.0, z0 - 1.5), (.8, -.2, z0 + .3), shade(red, .9))               # nez
    m.cube(root, (-.5, -2.2, z0 - 1.7), (.5, -1.5, z0 - 1.0), dark)
    # bouche beante + dents
    m.cube(root, (-3.0, -3.8, z0 - .12), (3.0, -2.3, z0 + .05), black)
    for i in range(5):
        x = -2.5 + i * 1.25
        m.cube(root, (x - .35, -3.0, z0 - .25), (x + .35, -2.3, z0 - .1), '#FFFFFF')
        m.cube(root, (x - .35, -3.8, z0 - .25), (x + .35, -3.25, z0 - .1), '#FFFFFF')
    for s in (-1, 1):
        m.cube(root, (s * 2.4 - .45, -3.9, z0 - .35), (s * 2.4 + .45, -2.2, z0 - .05), cream)      # grands crocs
    # cornes
    for s in (-1, 1):
        base = (s * 2.6, 4.2, z0 + .6)
        for i, (w, h, ang) in enumerate(((2.0, 2.4, 14), (1.6, 2.2, 22), (1.2, 2.0, 30), (.7, 1.8, 38))):
            y = 4.0 + sum(hh for _, hh, _ in ((0, 2.4, 0), (0, 2.2, 0), (0, 2.0, 0))[:i])
            m.cube(root, (s * 2.6 - w / 2 + s * i * .45, y, base[2] - w / 2), (s * 2.6 + w / 2 + s * i * .45, y + h, base[2] + w / 2), mix(cream, '#D8A860', i / 3), rot=(0, 0, -s * ang * .3), pivot=(s * 2.6, 4.0, base[2]))
    # crinière rouge
    m.cube(root, (-4.7, 4.0, z0 + .3), (4.7, 5.0, z1 + 4.5), dark)
    g = m.group(root, (0, 2.0, z0), name='brow')
    L = 2.5
    m.anim('idle', L, [(g, 's', m.wave(L, lambda p: (1, 1 + .06 * math.sin(TAU * p), 1)))])
    return m


@model('masque_docteur_peste', 'visage')
def masque_peste():
    m = Model('masque_docteur_peste')
    lea, dk, brass, glass = '#CDB48A', '#6A5030', '#C89838', '#FFB040'
    root = m.group(None, name='root')
    z0 = -1.0
    m.cube(root, (-4.4, -4.3, z0), (4.4, -.6, .2), lea)                                     # cagoule du bas du visage
    m.cube(root, (-4.4, -.6, z0), (-3.8, 1.8, .2), lea); m.cube(root, (3.8, -.6, z0), (4.4, 1.8, .2), lea)
    m.cube(root, (-4.4, 1.8, z0), (4.4, 2.8, .2), dk)                                          # bandeau de front
    # bec courbe : 6 segments vers l'avant et le bas
    for i in range(6):
        w = 2.8 - i * .38
        h = 3.2 - i * .45
        z = z0 - i * 1.6
        y = -1.0 - i * .35
        m.cube(root, (-w / 2, y - h / 2, z - 1.6), (w / 2, y + h / 2, z), mix(lea, dk, i / 9), top=mix(lea, '#FFFFFF', .12))
        if i % 2 == 0:
            m.cube(root, (-w / 2 - .06, y - h / 2 - .06, z - 1.65), (w / 2 + .06, y - h / 2 + .4, z - 1.2), dk)
    m.cube(root, (-.25, -1.0, -10.8), (.25, -.5, -10.3), dk)
    # lunettes rondes
    gl = []
    for s in (-1, 1):
        disc(m, root, s * 2.0, -.2, z0 - .8, z0, 1.9, brass)
        gg = m.group(root, (s * 2.0, -.2, z0 - .9), name=f'lens{s}')
        disc(m, gg, s * 2.0, -.2, z0 - .95, z0 - .75, 1.4, glass, alpha=200)
        m.cube(gg, (s * 2.0 - .9, .2, z0 - 1.05), (s * 2.0 - .2, .8, z0 - .95), '#FFFFFF', alpha=210)
        gl.append(gg)
    # sangle de tete (autour du crane) - posée en arrière des tempes
    m.cube(root, (-4.5, 1.8, .2), (-4.1, 2.8, 8.9), dk); m.cube(root, (4.1, 1.8, .2), (4.5, 2.8, 8.9), dk)
    m.cube(root, (-4.5, 1.8, 8.5), (4.5, 2.8, 8.9), dk)
    L = 4
    m.anim('idle', L, [(g, 'p', m.wave(L, lambda p: (.35 * s * math.sin(TAU * p) * 0, .15 * math.sin(TAU * p), 0))) for g, s in zip(gl, (-1, 1))])
    return m


@model('lunettes_steampunk', 'visage')
def lunettes_steampunk():
    m = Model('lunettes_steampunk')
    brass, cop, lea, glass = '#D0A040', '#B8683A', '#5A3A20', '#58E0E8'
    root = m.group(None, name='root')
    z0 = -.9
    for s in (-1, 1):
        for rr, col in ((2.2, brass), (1.8, cop)):
            pass
        disc(m, root, s * 2.1, -.4, z0 - .5, .3, 2.3, brass)
        disc(m, root, s * 2.1, -.4, z0 - .6, z0 - .3, 1.7, glass, alpha=185)
        m.cube(root, (s * 2.1 - 1.1, .4, z0 - .7), (s * 2.1 - .3, .9, z0 - .6), '#FFFFFF', alpha=200)
        for k in range(8):                                                                  # rivets
            a = math.radians(k * 45)
            m.cube(root, (s * 2.1 + math.sin(a) * 2.3 - .22, -.4 + math.cos(a) * 2.3 - .22, z0 - .65), (s * 2.1 + math.sin(a) * 2.3 + .22, -.4 + math.cos(a) * 2.3 + .22, z0 - .45), '#FFE090')
    m.cube(root, (-.5, -.9, z0 - .4), (.5, .1, .1), brass)                                    # pont
    m.cube(root, (-4.5, -.9, 0), (-4.2, .2, 9.0), lea); m.cube(root, (4.2, -.9, 0), (4.5, .2, 9.0), lea)   # sangle
    m.cube(root, (-4.5, -.9, 8.7), (4.5, .2, 9.0), lea)
    # engrenage sur la tempe
    gear = m.group(root, (4.7, .3, -1.0), name='gear')
    disc(m, gear, 4.7, .3, -1.2, -.7, 1.0, cop)
    for k in range(4):
        gear_t = m.cube(gear, (4.7 - .25, .3 - 1.6, -1.2), (4.7 + .25, .3 + 1.6, -.7), cop, rot=(0, 0, k * 45), pivot=(4.7, .3, -1))
    m.cube(gear, (4.45, .05, -1.3), (4.95, .55, -.6), brass)
    # monocle relevable sur le front
    mono = m.group(root, (-2.1, 2.6, -1.0), (-60, 0, 0), name='mono')
    disc(m, mono, -2.1, 3.9, -1.3, -.8, 1.5, brass)
    disc(m, mono, -2.1, 3.9, -1.35, -1.05, 1.1, '#FFB040', alpha=190)
    m.cube(mono, (-2.3, 2.4, -1.15), (-1.9, 2.8, -.95), brass)
    L = 6
    m.anim('idle', L, [(gear, 'r', m.wave(L, lambda p: (0, 0, 360 * p), 24)),
                       (mono, 'r', [(0, -60, 0, 0), (2.0, -60, 0, 0), (2.6, 8, 0, 0), (3.8, 8, 0, 0), (4.4, -60, 0, 0), (6, -60, 0, 0)])])
    return m


# ============================================================ EPAULES ============================================================
@model('epaulieres_phenix', 'epaules')
def epaulieres_phenix():
    m = Model('epaulieres_phenix')
    crim, gold, dark = '#B82020', '#F0B830', '#701010'
    root = m.group(None, name='root')
    plumes = []
    for s in (-1, 1):
        cx = s * 6.0
        m.octagon(root, cx, -1.2, 0, 4.3, 1.2, shade(crim, .8), bottom=dark)
        m.octagon(root, cx, 0.0, 0, 4.0, 1.3, crim)
        m.octagon(root, cx, 1.3, 0, 3.2, 1.2, mix(crim, '#FF6030', .3))
        m.octagon(root, cx, 2.5, 0, 2.0, .9, '#FF8838')
        m.cube(root, (cx - 4.4, -1.3, -4.4), (cx + 4.4, -.8, 4.4), gold, rot=(0, 0, 0))
        for k in range(6):
            a = math.radians(k * 60)
            m.cube(root, (cx + math.sin(a) * 4.1 - .4, -.3, -math.cos(a) * 4.1 - .4), (cx + math.sin(a) * 4.1 + .4, .5, -math.cos(a) * 4.1 + .4), gold)
        m.cube(root, (cx - .8, 2.3, -3.7), (cx + .8, 3.9, -3.3), '#FFE040', rot=(0, 0, 45))
        # panache de flammes (3 plumes)
        for i, (ang, ln, ph) in enumerate(((32, 9.0, 0), (6, 11.0, .15), (-20, 8.0, .3))):
            a = ang * s
            g = m.group(root, (cx, 3.2, 1.0), (0, 0, 0), name=f'pl{s}{i}')
            for j, (a0, a1, col) in enumerate(((0, .4, '#B81818'), (.4, .7, '#F05A18'), (.7, 1.0, '#FFD040'))):
                w = 1.7 - j * .5
                m.cube(g, (cx - w / 2, 3.2 + ln * a0, .4), (cx + w / 2, 3.2 + ln * a1, 1.6), col, rot=(-14, 0, a), pivot=(cx, 3.2, 1.0))
            plumes.append((g, ph, s, a))
    L = 2.4
    m.anim('idle', L, [(g, 'r', m.wave(L, lambda p, ph=ph, s=s, a=a: (-3 * math.sin(TAU * (p + ph)), 0, 4 * s * math.sin(TAU * (p + ph + .25))))) for g, ph, s, a in plumes])
    return m


@model('epaulieres_chevalier_or', 'epaules')
def epaulieres_or():
    m = Model('epaulieres_chevalier_or')
    gold, dark, light, gem = '#E8B838', '#A87820', '#FFE890', '#3090F0'
    root = m.group(None, name='root')
    gems = []
    for s in (-1, 1):
        cx = s * 6.2
        for i, (y0, h, r, col) in enumerate(((-.2, 1.2, 4.9, gold), (1.0, 1.1, 4.4, mix(gold, light, .15)), (2.1, 1.0, 3.5, mix(gold, light, .3)), (3.1, .9, 2.4, mix(gold, light, .5)))):
            m.octagon(root, cx, y0, 0, r, h, col, bottom=dark)
        m.cube(root, (cx - 5.0, -.3, -5.0), (cx + 5.0, .1, 5.0), dark)
        m.cube(root, (cx - 4.5, 1.0, -4.5), (cx + 4.5, 1.2, 4.5), dark)
        # lames qui descendent sur le bras
        for i, (y, w) in enumerate(((-1.0, 4.9), (-2.4, 4.7), (-3.8, 4.5))):
            m.cube(root, (cx - w, y - 1.4, -w), (cx + w, y, w), mix(gold, dark, i * .25), bottom=shade(dark, .8))
            m.cube(root, (cx - w - .05, y - 1.45, -w - .05), (cx + w + .05, y - 1.25, w + .05), dark)
        # pointe
        for i, (w, h) in enumerate(((1.1, 1.2), (.8, 1.1), (.45, 1.0))):
            m.cube(root, (cx - w, 3.9 + i * 1.1, -w), (cx + w, 3.9 + i * 1.1 + h, w), light if i == 2 else gold)
        g = m.group(root, (cx, 1.8, -4.4), name=f'gem{s}')
        m.cube(g, (cx - 1.0, 1.0, -4.9), (cx + 1.0, 2.8, -4.5), gem, rot=(0, 0, 45))
        m.cube(g, (cx - .35, 1.5, -5.0), (cx + .35, 2.1, -4.75), '#C0E4FF')
        gems.append(g)
        for k in range(8):
            a = math.radians(k * 45 + 22)
            m.cube(root, (cx + math.sin(a) * 4.7 - .35, 1.4, -math.cos(a) * 4.7 - .35), (cx + math.sin(a) * 4.7 + .35, 2.1, -math.cos(a) * 4.7 + .35), light)
    L = 3
    m.anim('idle', L, [(g, 's', m.wave(L, lambda p: (1 + .18 * math.sin(TAU * p),) * 3)) for g in gems])
    return m


@model('hibou_epaule', 'epaules')
def hibou_epaule():
    m = Model('hibou_epaule')
    br, dr, cream, yel, org = '#7A5634', '#4E3520', '#EADAB8', '#FFD030', '#E88A20'
    root = m.group(None, name='root')
    m.cube(root, (-2.3, 0, -1.8), (2.3, 4.6, 1.8), br)                                      # corps
    m.cube(root, (-1.8, .3, -2.2), (1.8, 4.2, -1.7), cream)                                   # ventre
    for i in range(3):
        m.cube(root, (-1.4, .8 + i * 1.1, -2.3), (1.4, 1.2 + i * 1.1, -2.2), br)             # plumetis
    m.cube(root, (-2.9, .4, -1.2), (-2.3, 4.2, 1.6), dr); m.cube(root, (2.3, .4, -1.2), (2.9, 4.2, 1.6), dr)   # ailes
    m.cube(root, (-1.6, -2.0, 1.4), (1.6, .6, 2.6), dr, rot=(-24, 0, 0), pivot=(0, .6, 1.4))      # queue
    for s in (-1, 1):
        m.cube(root, (s * 1.1 - .5, -.6, -1.5), (s * 1.1 + .5, .2, -.5), org)               # serres
    head = m.group(root, (0, 4.6, 0), name='head')
    m.cube(head, (-2.6, 4.4, -2.2), (2.6, 8.2, 2.0), br)
    m.cube(head, (-2.0, 5.0, -2.35), (2.0, 7.4, -2.1), cream)
    for s in (-1, 1):
        m.cube(head, (s * 1.0 - .85, 5.3, -2.6), (s * 1.0 + .85, 7.1, -2.3), cream)       # disque facial
        eye = m.group(head, (s * 1.0, 6.2, -2.7), name=f'eye{s}')
        m.cube(eye, (s * 1.0 - .75, 5.5, -3.0), (s * 1.0 + .75, 6.9, -2.6), yel)
        m.cube(eye, (s * 1.0 - .35, 5.9, -3.1), (s * 1.0 + .35, 6.5, -3.0), '#101010')
        m.cube(head, (s * 2.0 - .45, 8.0, -1.4), (s * 2.0 + .45, 9.4, -.4), dr, rot=(0, 0, s * -14), pivot=(s * 2.0, 8.0, -.9))   # aigrettes
    m.cube(head, (-.4, 5.2, -3.1), (.4, 6.1, -2.55), org)                                    # bec
    eyes = [h for h in head['children'] if h.get('_name', '').startswith('eye')]
    L = 6
    turn = [(0, 0, 0, 0), (1.5, 0, 0, 0), (2, 0, 38, 0), (3.4, 0, 38, 0), (3.9, 0, -30, 0), (5.2, 0, -30, 0), (5.7, 0, 0, 0), (6, 0, 0, 0)]
    blink = [(0, 1, 1, 1), (4.5, 1, 1, 1), (4.62, 1, .1, 1), (4.78, 1, 1, 1), (6, 1, 1, 1)]
    m.anim('idle', L, [(head, 'r', turn)] + [(e, 's', blink) for e in eyes] + [(root, 'p', m.wave(L, lambda p: (0, .15 * math.sin(TAU * 2 * p), 0), 24))])
    return m


# ============================================================ DOS ============================================================
def feather(m, g, s, piv, ang, ln, w, z0, z1, cols):
    """Plume a 3 segments partant de piv dans la direction ang (deg, 0 = vers l'exterieur, 90 = vers le haut)."""
    a = ang * s
    sx = s
    cuts = (0, .5, .8, 1.0)
    for i in range(3):
        ww = w * (1.0 - .22 * i)
        m.cube(g, (piv[0] + sx * ln * cuts[i], piv[1] - ww / 2, z0), (piv[0] + sx * ln * cuts[i + 1], piv[1] + ww / 2, z1), cols[i], rot=(0, 0, a), pivot=piv)


@model('ailes_phenix', 'dos')
def ailes_phenix():
    m = Model('ailes_phenix')
    root = m.group(None, name='root')
    wings = []
    for s in (-1, 1):
        piv = (s * 2.4, 2.0, 1.0)
        w = m.group(root, piv, name=f'w{s}')
        m.cube(w, (s * 2.4 - 1.2, 0.8, .3), (s * 2.4 + 1.2, 3.4, 1.9), '#8C1414')                 # epaule
        # rangee principale (longues rémiges) puis couvertures
        n = 8
        for i in range(n):
            t = i / (n - 1)
            ang = 64 - 104 * t
            ln = 11 + 6 * math.sin(math.pi * (t * .75 + .1))
            cols = (mix('#C01818', '#E83020', t), mix('#F05A18', '#FF8A20', t), mix('#FFB030', '#FFE070', t))
            feather(m, w, s, piv, ang, ln, 4.0, 1.0, 1.8, cols)
        for i in range(7):
            t = i / 6
            ang = 58 - 92 * t
            ln = 7 + 3 * math.sin(math.pi * (t * .75 + .1))
            cols = (mix('#901010', '#B81818', t), mix('#D83A18', '#F05A18', t), mix('#FF8A20', '#FFC040', t))
            feather(m, w, s, piv, ang, ln, 3.6, .2, 1.0, cols)
        for i in range(6):
            t = i / 5
            feather(m, w, s, piv, 52 - 80 * t, 5.0, 3.2, -.6, .2, ('#6A0C0C', '#901010', '#C02818'))
        wings.append((w, s))
    # plumes de queue ardentes
    tails = []
    for k, (x, ang) in enumerate(((-2, 10), (0, 0), (2, -10))):
        g = m.group(root, (x, -8, 1), name=f'tail{k}')
        for j, col in enumerate(('#B01818', '#F05A18', '#FFD040')):
            m.cube(g, (x - .9 + j * .2, -8 - 5 * (j + 1) / 3 * 1.1, .6), (x + .9 - j * .2, -8 - 5 * j / 3 * 1.1, 1.6), col, rot=(0, 0, ang), pivot=(x, -8, 1))
        tails.append((g, k))
    L = 2.6
    m.anim('idle', L, [(w, 'r', m.wave(L, lambda p, s=s: (0, s * 24 * math.sin(TAU * p), s * 6 * math.sin(TAU * p)))) for w, s in wings] +
           [(g, 'r', m.wave(L, lambda p, k=k: (6 * math.sin(TAU * (p + k * .1)), 0, 0))) for g, k in tails])
    return m


@model('ailes_libellule', 'dos')
def ailes_libellule():
    m = Model('ailes_libellule')
    root = m.group(None, name='root')
    glass, vein, hi = '#9AF0FF', '#2A9CB8', '#E8FFFF'
    wings = []
    for pair, (y, ang, ln, zoff, ph) in enumerate(((2.6, 24, 17, 0, 0), (.6, -10, 13, .55, .35))):
        for s in (-1, 1):
            piv = (s * 1.6, y, 1.0 + zoff)
            g = m.group(root, piv, name=f'w{pair}{s}')
            a = ang * s
            # membrane : lentille a 4 segments
            for i, (c0, c1, hh) in enumerate(((0, .2, 1.8), (.2, .5, 3.0), (.5, .8, 3.0), (.8, 1.0, 1.8))):
                col = mix(glass, hi, i * .1)
                m.cube(g, (piv[0] + s * ln * c0, piv[1] - hh / 2, piv[2] - .1), (piv[0] + s * ln * c1, piv[1] + hh / 2, piv[2] + .15), col, alpha=140, rot=(0, 0, a), pivot=piv, flat=True)
            m.cube(g, (piv[0], piv[1] - .2, piv[2] + .15), (piv[0] + s * ln, piv[1] + .2, piv[2] + .4), vein, rot=(0, 0, a), pivot=piv)
            for k in range(1, 5):
                m.cube(g, (piv[0] + s * ln * (.15 + k * .17) - .15, piv[1] - 1.4, piv[2] + .15), (piv[0] + s * ln * (.15 + k * .17) + .15, piv[1] + 1.4, piv[2] + .3), vein, rot=(0, 0, a), pivot=piv, alpha=220)
            m.cube(g, (piv[0], piv[1] + 1.3, piv[2] + .15), (piv[0] + s * ln * .85, piv[1] + 1.6, piv[2] + .35), '#1E7C94', rot=(0, 0, a), pivot=piv)
            wings.append((g, s, ph))
    m.cube(root, (-1.2, -.6, .3), (1.2, 3.6, 1.4), '#2A9CB8')                                  # attache
    m.cube(root, (-.6, 0, 1.3), (.6, 3.0, 1.9), '#F0FFFF')
    L = 1.2
    m.anim('idle', L, [(g, 'r', m.wave(L, lambda p, s=s, ph=ph: (0, s * 16 * math.sin(TAU * (p + ph)), s * 3 * math.sin(TAU * (p + ph))), 12)) for g, s, ph in wings])
    return m


@model('cape_vampire', 'dos')
def cape_vampire():
    m = Model('cape_vampire')
    out, inn, trim = '#201428', '#B01830', '#E0B040'
    root = m.group(None, name='root')
    # haut col dresse
    for s in (-1, 1):
        m.cube(root, (s * 3.2 - 1.6 + (0 if s > 0 else 0), 1.0, .4), (s * 6.6, 9.0, 1.6), out, faces={'north': inn}, rot=(0, 0, s * -14), pivot=(s * 3.4, 1.0, 1.0))
        m.cube(root, (s * 5.8 - .2, 8.0, .35), (s * 7.0, 9.4, 1.65), trim, rot=(0, 0, s * -14), pivot=(s * 3.4, 1.0, 1.0))
    m.cube(root, (-6.6, 2.2, .35), (6.6, 3.2, 1.7), trim)                                      # chaine doree
    for s in (-1, 1):
        m.cube(root, (s * 6.4 - .7, 1.6, -.9), (s * 6.4 + .7, 3.8, 1.8), trim)               # agrafes d'épaule
        m.cube(root, (s * 6.4 - .45, 2.0, -1.2), (s * 6.4 + .45, 3.4, -.8), '#D82838', rot=(0, 0, 45))
    strips = []
    n = 6
    wd = 13.2 / n
    for i in range(n):
        x0 = -6.6 + i * wd
        g = m.group(root, (x0 + wd / 2, 2.2, .9), name=f's{i}')
        cur = g
        chain = [g]
        y = 2.2
        for j in range(3):
            seg = m.group(cur, (x0 + wd / 2, y, .9), name=f's{i}_{j}')
            h = 6.2
            c = mix(out, '#3A2040', j * .15)
            m.cube(seg, (x0, y - h, .5), (x0 + wd - .02, y, 1.3), c, faces={'north': mix(inn, '#601020', j * .3)}, bottom=shade(inn, .8))
            if j == 2:                                                                         # bord bas en pointes
                m.cube(seg, (x0 + .2, y - h - 1.6, .6), (x0 + wd - .22, y - h + .1, 1.2), c, faces={'north': inn}, flat=True)
                m.cube(seg, (x0 + .2, y - h, .55), (x0 + wd - .22, y - h + .6, 1.35), trim)
            chain.append(seg)
            cur = seg
            y -= h
        strips.append((i, chain[1:]))
    L = 3.4
    tracks = []
    for i, segs in strips:
        for j, sg in enumerate(segs):
            tracks.append((sg, 'r', m.wave(L, lambda p, i=i, j=j: (4 + (j + 1) * 3.2 * math.sin(TAU * (p - i * .07 - j * .1)) , 0, 1.2 * math.sin(TAU * (p - i * .05))))))
    m.anim('idle', L, tracks)
    return m


@model('sac_aventurier', 'dos')
def sac_aventurier():
    m = Model('sac_aventurier')
    lea, dk, flap, gold, red, cream = '#8A5A30', '#5E3A1C', '#6E4624', '#E8B838', '#B8362E', '#EADAB8'
    root = m.group(None, name='root')
    m.cube(root, (-4.2, -8.2, .3), (4.2, 2.4, 5.4), lea)                                     # sac
    m.cube(root, (-4.4, -.4, .2), (4.4, 3.4, 5.6), flap, bottom=dk)                         # rabat
    m.cube(root, (-4.5, -.5, 5.3), (4.5, 3.5, 5.7), dk)
    m.cube(root, (-.8, -1.8, 5.5), (.8, .2, 5.9), gold)                                     # boucle
    m.cube(root, (-.35, -1.2, 5.85), (.35, -.5, 6.0), '#6A4A10')
    for x in (-2.8, 2.8):
        m.cube(root, (x - .5, -8.0, 5.3), (x + .5, -.4, 5.65), dk)                          # sangles verticales
    # poches
    for s in (-1, 1):
        m.cube(root, (s * 4.2, -7.2, 1.0), (s * 5.7, -2.0, 4.6), shade(lea, .9))
        m.cube(root, (s * 4.2, -3.0, .9), (s * 5.8, -1.6, 4.7), flap)
    m.cube(root, (-3.2, -7.6, 5.4), (3.2, -3.0, 6.8), lea)                                  # poche avant
    m.cube(root, (-3.3, -4.2, 5.35), (3.3, -2.6, 6.9), flap)
    m.cube(root, (-.5, -3.6, 6.8), (.5, -3.0, 7.0), gold)
    # rouleau de couchage
    m.cube(root, (-5.2, -10.8, .2), (5.2, -8.1, 4.6), red, bottom=shade(red, .6))
    m.cube(root, (-5.2, -9.7, .15), (5.2, -9.4, 4.65), '#6A1818'); m.cube(root, (-5.2, -9.9, .15), (5.2, -9.8, 4.65), cream)
    m.cube(root, (-5.7, -10.5, .5), (-5.2, -8.4, 4.3), cream); m.cube(root, (5.2, -10.5, .5), (5.7, -8.4, 4.3), cream)
    for x in (-3.4, 3.4):
        m.cube(root, (x - .3, -10.9, .1), (x + .3, -8.0, 4.7), dk)
    # tube a cartes
    m.cube(root, (-4.9, 2.4, .2), (4.9, 4.2, 1.9), '#C8A060', rot=(0, 0, 0))
    m.cube(root, (-5.2, 2.2, .1), (-4.7, 4.4, 2.0), gold); m.cube(root, (4.7, 2.2, .1), (5.2, 4.4, 2.0), gold)
    # casserole
    pot = m.group(root, (6.4, -2.4, 2.6), name='pot')
    m.cube(pot, (6.2, -2.6, 2.4), (6.6, -.6, 2.8), dk)
    m.cube(pot, (6.0, -6.0, 1.2), (8.8, -2.6, 4.0), '#6C7480', top='#9AA2B0')
    m.cube(pot, (5.9, -3.0, 1.1), (8.9, -2.5, 4.1), '#8A92A0')
    m.cube(pot, (5.7, -3.6, 2.2), (6.0, -3.2, 3.0), dk)
    # lanterne
    lan = m.group(root, (-6.4, -2.4, 2.6), name='lan')
    m.cube(lan, (-6.6, -2.6, 2.4), (-6.2, -.6, 2.8), dk)
    m.cube(lan, (-8.2, -3.0, 1.4), (-4.8, -2.6, 3.8), '#B08830')
    m.cube(lan, (-8.2, -7.2, 1.4), (-4.8, -6.8, 3.8), '#B08830')
    flame = m.group(lan, (-6.5, -5.0, 2.6), name='flame')
    m.cube(flame, (-7.4, -6.6, 1.7), (-5.6, -3.2, 3.5), '#FFD860', alpha=230, flat=True)
    m.cube(flame, (-7.0, -6.2, 2.1), (-6.0, -4.2, 3.1), '#FFFFFF', flat=True)
    for dx in (-8.2, -5.2):
        for dz in (1.4, 3.5):
            m.cube(lan, (dx, -6.9, dz), (dx + .6, -2.7, dz + .3), '#B08830')
    L = 3.2
    m.anim('idle', L, [(pot, 'r', m.wave(L, lambda p: (0, 0, 7 * math.sin(TAU * p)))), (lan, 'r', m.wave(L, lambda p: (0, 0, -9 * math.sin(TAU * (p + .3))))),
                       (flame, 's', m.wave(L, lambda p: (1 + .12 * math.sin(TAU * 3 * p), 1 + .22 * math.sin(TAU * 4 * p + 1), 1), 24))])
    return m


@model('bouclier_croise', 'dos')
def bouclier_croise():
    m = Model('bouclier_croise')
    steel, dk, blue, gold, light = '#B8C0CC', '#6C7684', '#2850B8', '#F0C040', '#FFFFFF'
    root = m.group(None, name='root')
    # epee en travers, derriere le bouclier
    sword = m.group(root, (0, -2.0, .5), (0, 0, -32), name='sword')
    m.cube(sword, (-.7, -4, .1), (.7, 11.5, .9), '#D8DEE8', rot=(0, 0, -32), pivot=(0, -2, .5))
    m.cube(sword, (-.15, -3, .9), (.15, 11.5, 1.0), '#8090A8', rot=(0, 0, -32), pivot=(0, -2, .5))
    m.cube(sword, (-.5, 11.5, .1), (.5, 12.6, .9), '#D8DEE8', rot=(0, 0, -32), pivot=(0, -2, .5))
    m.cube(sword, (-2.8, 12.6, -.2), (2.8, 13.7, 1.4), gold, rot=(0, 0, -32), pivot=(0, -2, .5))
    m.cube(sword, (-.55, 13.7, .0), (.55, 17.0, 1.2), '#5E3A1C', rot=(0, 0, -32), pivot=(0, -2, .5))
    m.cube(sword, (-.9, 17.0, -.2), (.9, 18.4, 1.4), gold, rot=(0, 0, -32), pivot=(0, -2, .5))
    # bouclier
    sh = m.group(root, (0, -1.5, 1.0), name='shield')
    disc(m, sh, 0, -1.5, 1.4, 2.8, 6.3, steel, bottom=dk)
    disc(m, sh, 0, -1.5, 2.8, 3.2, 5.5, blue)
    m.cube(sh, (-1.0, -6.1, 3.2), (1.0, 3.1, 3.6), gold); m.cube(sh, (-4.9, -2.5, 3.2), (4.9, -.5, 3.6), gold)
    m.cube(sh, (-1.0, -6.1, 3.55), (-.7, 3.1, 3.65), '#FFF0A0'); m.cube(sh, (-4.9, -.8, 3.55), (4.9, -.5, 3.65), '#FFF0A0')
    disc(m, sh, 0, -1.5, 3.6, 4.4, 1.6, gold)
    disc(m, sh, 0, -1.5, 4.4, 4.7, .7, '#E83030')
    for k in range(8):
        a = math.radians(k * 45 + 22.5)
        m.cube(sh, (math.sin(a) * 5.9 - .4, -1.5 + math.cos(a) * 5.9 - .4, 2.8), (math.sin(a) * 5.9 + .4, -1.5 + math.cos(a) * 5.9 + .4, 3.5), '#E8EEF8')
    glint = m.group(sh, (0, -1.5, 3.7), name='glint')
    m.cube(glint, (-.5, -6, 3.66), (.5, 3, 3.78), '#FFFFFF', alpha=170, rot=(0, 0, 35), pivot=(0, -1.5, 3.7), flat=True)
    L = 4
    m.anim('idle', L, [(glint, 'p', [(0, -5, 0, 0), (1.2, 5, 0, 0), (4, 5, 0, 0)]),
                       (glint, 's', [(0, 0, 0, 0), (.3, 1, 1, 1), (.9, 1, 1, 1), (1.2, 0, 0, 0), (4, 0, 0, 0)]),
                       (sh, 'r', m.wave(L, lambda p: (1.5 * math.sin(TAU * p), 0, 0)))])
    return m


@model('reacteur_vapeur', 'dos')
def reacteur_vapeur():
    m = Model('reacteur_vapeur')
    cop, brass, steel, dk, org = '#B8683A', '#D8A840', '#7C8694', '#3A3E48', '#FF9A30'
    root = m.group(None, name='root')
    # plaque et manifold central
    m.cube(root, (-4.2, -5.6, .3), (4.2, 3.0, 1.2), steel)
    m.cube(root, (-1.4, -5.0, 1.2), (1.4, 1.6, 4.4), mix(steel, '#FFFFFF', .1))
    # reservoirs
    for s in (-1, 1):
        cx = s * 3.2
        for i, (y0, h, col) in enumerate(((-9.0, 1.2, brass), (-7.8, 3.6, cop), (-4.2, .8, brass), (-3.4, 3.6, cop), (.2, .8, brass), (1.0, 2.2, cop), (3.2, .6, brass))):
            r = 1.9 if i < 6 else 1.2
            if i == 5: r = 1.7
            m.octagon(root, cx, y0, 2.9, r + (.15 if col == brass else 0), h, col)
        m.cube(root, (cx - .5, 3.8, 2.4), (cx + .5, 5.2, 3.4), brass)                         # soupape
    wheel = m.group(root, (3.2, 5.4, 2.9), name='wheel')
    m.cube(wheel, (2.0, 5.2, 2.75), (4.4, 5.7, 3.05), '#D83030'); m.cube(wheel, (2.95, 5.2, 1.7), (3.45, 5.7, 4.1), '#D83030')
    # cadran
    disc(m, root, 0, .6, 4.4, 4.9, 1.7, brass)
    disc(m, root, 0, .6, 4.9, 5.1, 1.35, '#F4EEDC')
    for k in range(6):
        a = math.radians(-100 + k * 40)
        m.cube(root, (math.sin(a) * 1.0 - .12, .6 + math.cos(a) * 1.0 - .12, 5.1), (math.sin(a) * 1.0 + .12, .6 + math.cos(a) * 1.0 + .12, 5.2), '#3A2A20')
    needle = m.group(root, (0, .6, 5.15), name='needle')
    m.cube(needle, (-.12, .6, 5.1), (.12, 1.8, 5.35), '#D02828')
    # engrenage
    gear = m.group(root, (0, -2.7, 4.4), name='gear')
    disc(m, gear, 0, -2.7, 4.4, 5.0, 1.2, cop)
    for k in range(4):
        m.cube(gear, (-.3, -2.7 - 1.7, 4.4), (.3, -2.7 + 1.7, 5.0), cop, rot=(0, 0, k * 45), pivot=(0, -2.7, 4.7))
    m.cube(gear, (-.35, -3.05, 4.9), (.35, -2.35, 5.3), brass)
    # tuyaux vers les tuyeres
    for s in (-1, 1):
        m.cube(root, (s * 1.4, -4.6, 2.6), (s * 3.2, -3.9, 3.2), dk)
        # tuyere
        for i, (y0, h, r) in enumerate(((-9.8, 1.0, 1.6), (-11.0, 1.2, 2.0), (-12.2, 1.0, 2.5))):
            m.octagon(root, s * 3.2, y0, 2.9, r, h, mix(steel, dk, i / 3), bottom=org)
        m.octagon(root, s * 3.2, -12.35, 2.9, 1.7, .25, org)
    # vapeur : bouffees
    puffs = []
    for s in (-1, 1):
        for k in range(3):
            g = m.group(root, (s * 3.2, -12.4, 2.9), name=f'puff{s}{k}')
            m.cube(g, (s * 3.2 - 1.0, -13.4, 1.9), (s * 3.2 + 1.0, -11.4, 3.9), '#F0F4F8', alpha=190, flat=True)
            puffs.append((g, k, s))
    for k in range(2):
        g = m.group(root, (3.2, 5.0, 2.9), name=f'top{k}')
        m.cube(g, (2.4, 5.4, 2.3), (4.0, 7.0, 3.5), '#FFFFFF', alpha=170, flat=True)
        puffs.append((g, k + 3, 0))
    L = 2.4
    # bouffees decalees : boucles fermees via echantillonnage
    tr = []
    for g, k, s in puffs:
        ph = (k % 3) / 3 if s != 0 else k * .5
        sgn = 1 if s == 0 else -1
        frames_p, frames_s = [], []
        for i in range(13):
            t = i / 12
            q = (t + ph) % 1
            frames_p.append((t * L, 0, sgn * 9 * q, 0))
            sc = .25 + 1.45 * math.sin(math.pi * q) ** .6
            frames_s.append((t * L, sc, sc, sc))
        tr += [(g, 'p', frames_p), (g, 's', frames_s)]
    tr += [(gear, 'r', m.wave(L, lambda p: (0, 0, -360 * p), 24)), (wheel, 'r', m.wave(L, lambda p: (0, 360 * p * 0, 0))),
           (needle, 'r', m.wave(L, lambda p: (0, 0, 35 * math.sin(TAU * p) + 10 * math.sin(TAU * 3 * p))))]
    m.anim('idle', L, tr)
    return m
