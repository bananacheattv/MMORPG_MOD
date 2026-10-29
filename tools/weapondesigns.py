"""Dessins des armes d'Aeloria : chaque arme a sa silhouette, ses materiaux et sa palette."""
import math
import numpy as np
from weaponart import Art, hexc, mix, shade

C = {k: hexc(v) for k, v in dict(
    iron='#9aa1ae', steel='#c2ccd9', bright='#e6eef7', dark='#5b6274', night='#3a3d52', obsidian='#2b2736',
    blue='#7fa6d8', ice='#bfe7f5', gold='#e3b653', gold2='#b98a2c', bronze='#b07a46', brass='#c9a24c',
    leather='#7a4a2c', leather2='#5a3420', wood='#8b6038', wood2='#5e3f26', bone='#e2d8bd', crimson='#c8353f',
    red='#e0434d', ember='#ff8a3a', yellow='#ffd95a', green='#58c878', leaf='#6fae4a', cyan='#4fe3e0', azure='#4b8df0',
    violet='#a65be0', magenta='#e45ad0', white='#f4f7ff', rust='#7d4a3a', teal='#2f8f94', silver='#d7dfe8', void='#4b2a7a').items()}


def hx(h):
    return hexc(h)


# --------------------------------------------------------------------- briques communes

def pommel(a, kind, y, mat, gem=None, r=1.35):
    if kind == 'ball':
        a.part(a.ellipse(8, y, r), mat, t0=.8, t1=2.0, sigma=r * .8, spec=.5)
    elif kind == 'disc':
        a.part(a.ellipse(8, y, r * 1.1, r * .75), mat, t0=.8, t1=1.6, sigma=r * .6, spec=.5)
    elif kind == 'spike':
        a.part(a.poly([(8 - r * .8, y + .6), (8, y - r * 1.9), (8 + r * .8, y + .6)]), mat, t0=.6, t1=1.9, sigma=.45, spec=.5)
        a.part(a.ellipse(8, y + .6, r * .85, r * .6), mat, t0=.8, t1=1.8, sigma=.5, spec=.5)
    elif kind == 'gem':
        a.part(a.ellipse(8, y, r * 1.05), mat, t0=.8, t1=1.7, sigma=r * .6, spec=.5)
        a.gem(8, y, r * .72, gem if gem is not None else C['red'])
    elif kind == 'wing':
        for s in (-1, 1):
            a.part(a.poly([(8, y + .6), (8 + s * r * 2, y + .2), (8 + s * r * 1.6, y - .5), (8 + s * r * .7, y - .9), (8, y - .6)]), mat, t0=.6, t1=1.2, sigma=.35, spec=.5)
        a.part(a.ellipse(8, y, r * .8), mat, t0=.8, t1=1.8, sigma=.5, spec=.5)


def grip(a, y0, y1, col, wrap=None, w=1.5, bands=True):
    a.part(a.rect(8, (y0 + y1) / 2, w, y1 - y0), col, t0=.8, t1=1.7, sigma=w * .38, spec=.12, grain=.07, vgrain=True)
    if bands:
        n = int((y1 - y0) / .9)
        for i in range(n):
            y = y0 + .5 + i * (y1 - y0 - .5) / max(1, n)
            a.cut(a.line(8 - w / 2, y, 8 + w / 2, y + .35, .28), .2, wrap if wrap is not None else shade(col, .6), .5)


def guard(a, kind, y, mat, w=8, gem=None, accent=None):
    accent = mat if accent is None else accent
    if kind == 'bar':
        a.part(a.rect(8, y, w, 1.25), mat, t0=.9, t1=1.9, sigma=.5, spec=.55)
        for s in (-1, 1):
            a.part(a.ellipse(8 + s * w / 2, y, .8, .9), mat, t0=.9, t1=1.9, sigma=.4, spec=.55)
    elif kind == 'curved':
        for s in (-1, 1):
            pts = [(8 + s * .8, y + .15)]
            for i in range(1, 8):
                u = i / 7
                pts.append((8 + s * (.8 + (w / 2 - .8) * u), y + .15 + 1.9 * u * u))
            a.part(a.stroke(pts, [1.3 - .6 * i / 7 for i in range(8)]), mat, t0=.8, t1=1.8, sigma=.45, spec=.55)
        a.part(a.rect(8, y, 2.2, 1.7), mat, t0=.9, t1=2.1, sigma=.6, spec=.55)
    elif kind == 'cross':
        a.part(a.rect(8, y, w, 1.35), mat, t0=.9, t1=1.9, sigma=.5, spec=.55)
        for s in (-1, 1):
            a.part(a.rect(8 + s * w / 2, y, 1.1, 2.7), mat, t0=.8, t1=1.7, sigma=.4, spec=.55)
        a.part(a.rect(8, y, 2, 2), accent, t0=1, t1=2.0, sigma=.5, spec=.6)
    elif kind == 'wings':
        for s in (-1, 1):
            for i in range(4):
                x0 = 8 + s * (1.0 + i * w / 8.2)
                yy = y - .2 + i * .55
                a.part(a.poly([(x0, yy + .5), (x0 + s * w / 5.2, yy + 1.6 + i * .4), (x0 + s * w / 5.2, yy - .2), (x0, yy - .6)]), shade(mat, 1.08 - .07 * i), t0=.6, t1=1.5, sigma=.35, spec=.6)
        a.part(a.rect(8, y, 2.4, 1.8), mat, t0=1, t1=2.1, sigma=.6, spec=.6)
    elif kind == 'horn':
        for s in (-1, 1):
            pts = [(8 + s * .9, y - .2)]
            for i in range(1, 9):
                u = i / 8
                pts.append((8 + s * (.9 + (w / 2 - .9) * math.sin(u * 1.3) / math.sin(1.3)), y - .2 + 2.6 * u ** 2.2 - .6 * u))
            a.part(a.stroke(pts, [1.7 * (1 - i / 9) + .25 for i in range(9)]), mat, t0=.7, t1=1.9, sigma=.5, spec=.5)
        a.part(a.rect(8, y, 2.4, 1.6), mat, t0=1, t1=2.1, sigma=.6, spec=.5)
    elif kind == 'disc':
        a.part(a.ellipse(8, y, w / 2, 1.1), mat, t0=.8, t1=1.9, sigma=.6, spec=.6)
        a.part(a.ellipse(8, y, w / 2 - .8, .55), shade(mat, 1.15), t0=1.2, t1=1.2, flat=True, spec=.5)
    elif kind == 'spike':
        a.part(a.rect(8, y, w, 1.2), mat, t0=.9, t1=1.9, sigma=.5, spec=.55)
        for s in (-1, 1):
            a.part(a.poly([(8 + s * (w / 2 - 1.4), y + .5), (8 + s * (w / 2 + .6), y + 2.8), (8 + s * (w / 2 + .2), y - .2)]), mat, t0=.6, t1=1.6, sigma=.35, spec=.55)
            a.part(a.poly([(8 + s * (w / 2 - 1.4), y - .5), (8 + s * (w / 2 - .4), y - 2.5), (8 + s * (w / 2 - .0), y + .3)]), mat, t0=.6, t1=1.5, sigma=.3, spec=.55)
        a.part(a.rect(8, y, 2.2, 1.9), mat, t0=1, t1=2.1, sigma=.6, spec=.55)
    elif kind == 'claw':
        for s in (-1, 1):
            pts = [(8 + s * 1.0, y + .2), (8 + s * (w / 2 - 1), y + 1.3), (8 + s * (w / 2 + .3), y + 2.7)]
            a.part(a.stroke(pts, [1.5, 1.0, .3]), mat, t0=.7, t1=1.8, sigma=.5, spec=.55)
            pts = [(8 + s * 1.0, y - .2), (8 + s * (w / 2 - 1), y - 1.0), (8 + s * (w / 2 - .5), y - 2.4)]
            a.part(a.stroke(pts, [1.2, .9, .3]), mat, t0=.7, t1=1.6, sigma=.4, spec=.55)
        a.part(a.rect(8, y, 2.4, 1.9), mat, t0=1, t1=2.1, sigma=.6, spec=.55)
    if gem is not None:
        a.gem(8, y, .95, gem)


def blade(a, y0, length, w0, w1, col, curve=0, tip=.14, serr=0, wave=0, fuller=True, edge=None, tipcut=None,
          fcol=None, sig=None, t1=1.25, rune=None, double=True):
    """Lame : profil de largeur lineaire, courbure, dents, ondulation, gouttiere, reflets de fil."""
    N = 44
    left, right = [], []
    for i in range(N + 1):
        u = i / N
        y = y0 + u * length
        xc = 8 + curve * u * u + wave * math.sin(u * math.pi * 3.2) * (.3 + u)
        hw = (w0 + (w1 - w0) * u) / 2
        if u > 1 - tip:
            k = (u - (1 - tip)) / tip
            hw *= max(0, 1 - k) ** (1.0 if tipcut is None else tipcut) if tipcut is not None or True else 1
        off = 0
        if serr and 0.25 < u < 1 - tip:
            off = serr * (1 if (i % 2) else -.2)
        left.append((xc - hw - (off if double else 0), y))
        right.append((xc + hw + off, y))
    m = a.poly(left + right[::-1])
    sig = (min(w0, w1) / 2.4) if sig is None else sig
    a.part(m, col, t0=.55, t1=t1, sigma=sig, spec=.85, grain=.035, vgrain=True)
    if fuller:
        pts = [(8 + curve * u * u + wave * math.sin(u * math.pi * 3.2) * (.3 + u), y0 + .6 + u * (length * .72)) for u in np.linspace(0, 1, 18)]
        fm = a.stroke(pts, [max(.35, (w0 + (w1 - w0) * u) * .16) for u in np.linspace(0, 1, 18)])
        a.cut(fm & m, .28, fcol if fcol is not None else shade(col, .62), .65)
    if rune is not None:
        for i in range(5):
            u = .16 + i * .125
            xc = 8 + curve * u * u + wave * math.sin(u * math.pi * 3.2) * (.3 + u)
            yy = y0 + u * length
            a.paint(a.rect(xc, yy, .55, .8), rune, .95, glow=1)
            a.paint(a.rect(xc, yy + .45, 1.0 if i % 2 else .4, .25), rune, .95, glow=1)
    if edge is not None:  # fil colore (feu, givre...)
        e = a.poly([(l[0] - .05, l[1]) for l in left] + [(l[0] + .45, l[1]) for l in left[::-1]])
        a.paint(e & m, edge, .75, glow=1)
    return m


# --------------------------------------------------------------------- epees

def sword(a, v, pal):
    S = pal
    y_g = 9.0
    if v == 0:      # epee de recrue : lame courte, garde droite simple
        blade(a, 9.6, 14.5, 2.3, 1.9, C['iron'], tip=.2, fuller=True)
        guard(a, 'bar', y_g, C['bronze'], 6)
        grip(a, 3, 8.6, C['leather'])
        pommel(a, 'ball', 2.1, C['bronze'])
    elif v == 1:    # epee de milicien : longue lame droite, garde cuivree, pommeau lourd
        blade(a, 10, 17.5, 2.6, 2.1, C['steel'], tip=.16)
        guard(a, 'bar', y_g, C['brass'], 7.5)
        a.part(a.rect(8, 9.9, 2.0, .7), C['brass'], t0=1, t1=1.6, sigma=.3, spec=.5)
        grip(a, 3, 8.5, C['leather2'], C['gold2'])
        pommel(a, 'disc', 2.1, C['brass'], r=1.5)
    elif v == 2:    # lame runique : runes lumineuses, garde en croissant, gemme turquoise
        blade(a, 10.2, 18.6, 3.0, 2.3, C['blue'], tip=.14, rune=C['cyan'], fuller=False)
        guard(a, 'curved', y_g, C['silver'], 8.6, gem=C['cyan'])
        grip(a, 3, 8.4, C['wood2'], C['teal'])
        pommel(a, 'gem', 2.0, C['silver'], C['cyan'])
    elif v == 3:    # lame d'acier trempe : sabre courbe, ligne de trempe, petite garde ronde
        blade(a, 9.8, 19.5, 2.1, 1.7, C['bright'], curve=2.6, tip=.13, tipcut=.9, fuller=False, t1=1.0)
        a.paint(a.stroke([(8 + 2.6 * u * u + .55, 10.2 + u * 18.2) for u in np.linspace(0, .95, 20)], .5) & (a.a > 0), C['steel'] * .62, .6)
        guard(a, 'disc', y_g, C['dark'], 4.4)
        grip(a, 3.2, 8.4, C['night'], C['red'])
        pommel(a, 'ball', 2.2, C['dark'], r=1.2)
    elif v == 4:    # epee de berserker : coupe-chou ebreche, pointes, cuir rouge
        blade(a, 9.8, 15.5, 4.0, 4.7, C['iron'] * .78, tip=.12, serr=.35, fuller=True, fcol=C['night'], t1=1.5, tipcut=.5)
        a.paint(a.rect(9.6, 22, .5, 1.5), C['ember'], .5)
        guard(a, 'spike', y_g, C['dark'], 6.4)
        grip(a, 3, 8.4, C['crimson'], C['night'])
        pommel(a, 'spike', 2.0, C['dark'])
    elif v == 5:    # epee croisee : lame droite elancee, grande croix doree, gemme sainte
        blade(a, 10.6, 18.6, 2.5, 2.0, C['bright'], tip=.12, fuller=True, fcol=C['gold2'])
        guard(a, 'cross', y_g, C['gold'], 9, accent=C['gold2'])
        a.gem(8, y_g, .8, C['azure'])
        grip(a, 3, 8.2, C['leather2'], C['gold2'])
        pommel(a, 'ball', 2.0, C['gold'])
    elif v == 6:    # epee du seigneur de guerre : lame large dentelee, garde a piques, metal sombre
        blade(a, 9.8, 18.4, 4.4, 3.4, C['dark'], tip=.14, serr=.28, fcol=C['violet'], t1=1.7, edge=C['violet'] * .8)
        guard(a, 'spike', y_g, C['night'], 8.6)
        a.gem(8, y_g, .9, C['violet'])
        grip(a, 3, 8.2, C['obsidian'], C['violet'])
        pommel(a, 'spike', 2.0, C['night'], r=1.5)
    elif v == 7:    # lame des tempetes : lame en eclair
        blade(a, 10, 19.5, 3.0, 2.4, C['azure'], wave=1.35, tip=.14, fuller=False, edge=C['white'], rune=None)
        a.paint(a.stroke([(8 + 1.35 * math.sin(u * math.pi * 3.2) * (.3 + u), 10.6 + u * 17.5) for u in np.linspace(0, 1, 40)], .45) & (a.a > 0), C['white'], .9, glow=1)
        guard(a, 'wings', y_g, C['silver'], 8.4)
        grip(a, 3, 8.4, C['night'], C['azure'])
        pommel(a, 'gem', 2.0, C['silver'], C['cyan'])
    elif v == 8:    # lame demoniaque : lame dentee tordue, cornes, braise
        blade(a, 10.2, 19, 3.6, 2.6, C['obsidian'], curve=-1.4, tip=.13, serr=.4, fcol=C['ember'], edge=C['ember'], t1=1.5)
        a.paint(a.stroke([(8 - 1.4 * u * u, 10.8 + u * 16) for u in np.linspace(0, 1, 18)], .38) & (a.a > 0), C['ember'], .9, glow=1)
        guard(a, 'horn', y_g, C['night'], 9)
        a.gem(8, y_g, .95, C['red'])
        grip(a, 3, 8.2, C['crimson'], C['obsidian'])
        pommel(a, 'spike', 2.0, C['night'])
    elif v == 9:    # epee du roi dragon : ailes de dragon, lame ecaillee or/pourpre
        blade(a, 10.2, 19.4, 3.4, 2.5, C['crimson'], tip=.12, fuller=False, t1=1.5, edge=C['gold'])
        for u in np.linspace(.08, .85, 8):
            yy = 10.6 + u * 18.4
            a.cut(a.ellipse(8, yy, 1.05 - u * .3, .55), .15, C['gold2'], .45)
        a.paint(a.stroke([(8, 10.8 + u * 17) for u in np.linspace(0, 1, 12)], .35) & (a.a > 0), C['gold'], .9)
        guard(a, 'wings', y_g, C['gold'], 10.4)
        a.gem(8, y_g, 1.05, C['ember'])
        grip(a, 3, 8.2, C['leather2'], C['gold'])
        pommel(a, 'wing', 2.1, C['gold'])
    else:           # excalibur celeste : lame lumineuse, ailes d'ange, gemme celeste
        blade(a, 10.8, 20.4, 3.0, 2.4, C['white'], tip=.1, fuller=True, fcol=C['ice'], t1=1.35, edge=C['ice'])
        a.paint(a.stroke([(8, 11.5 + u * 18) for u in np.linspace(0, 1, 12)], .3) & (a.a > 0), C['azure'], .55, glow=1)
        guard(a, 'wings', y_g, C['gold'], 11.4)
        a.gem(8, y_g, 1.25, C['azure'])
        a.gem(8, y_g, .6, C['white'], facets=False)
        grip(a, 3, 8.2, C['white'] * .86, C['gold'])
        pommel(a, 'gem', 2.0, C['gold'], C['azure'], r=1.6)


# --------------------------------------------------------------------- batons

def shaft(a, y0, y1, col, w=1.3, grip_col=None, collars=()):
    a.part(a.rect(8, (y0 + y1) / 2, w, y1 - y0), col, t0=.8, t1=1.6, sigma=w * .4, spec=.2, grain=.08, vgrain=True)
    if grip_col is not None:
        a.part(a.rect(8, 6.2, w + .35, 6.2), grip_col, t0=.9, t1=1.8, sigma=w * .45, spec=.12, grain=.07, vgrain=True)
        for i in range(7):
            y = 3.4 + i * .85
            a.cut(a.line(8 - w / 2 - .2, y, 8 + w / 2 + .2, y + .3, .24), .2, shade(grip_col, .55), .6)
    for y, mat in collars:
        a.part(a.rect(8, y, w + 1.2, .75), mat, t0=.9, t1=2.0, sigma=.3, spec=.6)


def prongs(a, y, mat, n=2, spread=3.0, h=4.5, w=.85):
    for s in (-1, 1):
        pts = [(8 + s * .7, y)] + [(8 + s * (.7 + spread * math.sin(u * 1.35) / math.sin(1.35)), y + h * u) for u in np.linspace(.15, 1, 8)]
        a.part(a.stroke(pts, [w * 1.4] + [w * (1.15 - .6 * u) for u in np.linspace(.15, 1, 8)]), mat, t0=.7, t1=1.6, sigma=.35, spec=.6)


def staff(a, v, pal):
    wood, dkw = C['wood'], C['wood2']
    if v == 0:      # baton d'apprenti : branche noueuse, petite bille de verre
        shaft(a, -.5, 25, wood, 1.3, C['leather'])
        a.part(a.ellipse(8.35, 14.5, .9, .7), dkw, t0=.8, t1=1.5, sigma=.4)
        prongs(a, 25.5, dkw, spread=2.2, h=3.2)
        a.gem(8, 27.6, 1.25, C['azure'], facets=False)
    elif v == 1:    # baton de saule : bois sinueux, feuilles, perle verte
        pts = [(8 + .55 * math.sin(u * 7), -.5 + u * 25.5) for u in np.linspace(0, 1, 30)]
        a.part(a.stroke(pts, 1.25), wood, t0=.8, t1=1.6, sigma=.5, spec=.2, vgrain=True)
        for i, (x, y) in enumerate([(8.6, 12), (7.5, 16.4), (8.5, 20.5)]):
            s = 1 if i % 2 == 0 else -1
            a.part(a.poly([(x, y), (x + s * 2.6, y + 1.2), (x + s * 3.6, y - .1), (x + s * 2.2, y - .9)]), C['leaf'], t0=.5, t1=1.1, sigma=.5, spec=.3)
        prongs(a, 25.2, dkw, spread=2.6, h=4.2)
        a.gem(8, 27.8, 1.5, C['green'])
    elif v == 2:    # sceptre de givre : hampe argentee, cristal de glace
        shaft(a, -.5, 24, C['silver'], 1.1, C['night'], collars=[(10, C['steel']), (21, C['steel'])])
        for s in (-1, 1):
            a.part(a.poly([(8 + s * .5, 24), (8 + s * 3.4, 28.5), (8 + s * 2.3, 29.5), (8 + s * .5, 26.6)]), C['ice'], t0=.6, t1=1.4, sigma=.5, spec=.9)
        a.part(a.poly([(8, 23.5), (6.4, 27), (8, 32), (9.6, 27)]), C['ice'], t0=.8, t1=2.0, sigma=.9, spec=.95)
        a.cut(a.poly([(8, 25), (7.4, 27), (8, 30.2), (8.5, 27)]), -.3, C['white'], .5)
    elif v == 3:    # sceptre des marees : coquillage nacre et vague
        shaft(a, -.5, 23, C['teal'], 1.15, C['leather2'], collars=[(9, C['silver'])])
        for i, ang in enumerate(np.linspace(-65, 65, 7)):
            r = math.radians(ang)
            a.part(a.poly([(8, 25.2), (8 + 4.4 * math.sin(r - .17), 25.2 + 4.2 * math.cos(r - .17) + 1.2), (8 + 4.4 * math.sin(r + .17), 25.2 + 4.2 * math.cos(r + .17) + 1.2)]), mix(C['white'], C['azure'], .15 + .1 * (i % 2)), t0=.7, t1=1.7, sigma=.4, spec=.7)
        a.part(a.ellipse(8, 24, 1.5, 1.0), C['silver'], t0=.9, t1=1.9, sigma=.5, spec=.7)
        a.gem(8, 26.6, .95, C['cyan'])
    elif v == 4:    # baton de flammes : bois sombre, couronne de flammes
        shaft(a, -.5, 23, C['wood2'], 1.3, C['crimson'], collars=[(9, C['bronze']), (22.5, C['bronze'])])
        prongs(a, 22.8, C['bronze'], spread=3.0, h=3.4)
        flame = [(8, 24), (5.8, 26.5), (6.6, 28.2), (7.0, 27), (7.8, 30), (8, 32), (8.4, 30), (9.2, 27.2), (9.7, 28.6), (10.4, 26.4)]
        a.part(a.poly(flame), C['ember'], t0=.7, t1=1.7, sigma=.7, spec=.5, glow=1)
        a.part(a.poly([(8, 24.8), (7, 26.5), (8, 29.6), (9, 26.5)]), C['yellow'], t0=1.2, t1=1.9, sigma=.5, spec=.5, glow=1)
    elif v == 5:    # baton de foudre : fourche et eclairs
        shaft(a, -.5, 22, C['night'], 1.2, C['azure'], collars=[(10, C['silver']), (21, C['silver'])])
        prongs(a, 22.3, C['silver'], spread=3.6, h=5.8, w=.9)
        bolt = [(8, 23.6), (9.4, 26.2), (8.4, 26.5), (9.6, 29.6), (8.0, 28.0), (8.6, 27.8), (6.6, 26), (7.6, 25.7)]
        a.part(a.poly(bolt), C['yellow'], t0=.8, t1=1.8, sigma=.4, spec=.8, glow=1)
        a.gem(8, 24.6, .75, C['azure'], facets=False)
    elif v == 6:    # baton arcanique : cage d'anneaux autour d'un orbe
        shaft(a, -.5, 22, C['violet'] * .6, 1.3, C['night'], collars=[(8.4, C['gold']), (20.5, C['gold'])])
        a.part(a.stroke([(8 + 3.6 * math.cos(t), 26 + 3.6 * math.sin(t)) for t in np.linspace(0, 2 * math.pi, 40)], .85), C['gold'], t0=.7, t1=1.6, sigma=.35, spec=.7)
        a.part(a.stroke([(8 + 2.2 * math.cos(t) * .5 + 0, 26 + 3.6 * math.sin(t)) for t in np.linspace(0, 2 * math.pi, 40)], .6), C['gold2'], t0=.7, t1=1.4, sigma=.3, spec=.7)
        a.gem(8, 26, 2.0, C['violet'])
        a.part(a.poly([(8, 29.4), (7.2, 31.4), (8, 32), (8.8, 31.4)]), C['gold'], t0=.8, t1=1.6, sigma=.3, spec=.7)
    elif v == 7:    # sceptre de l'ombre : serpent noir enroule et oeil violet
        pts = [(8 + .8 * math.sin(u * 14), -.5 + u * 23.5) for u in np.linspace(0, 1, 60)]
        a.part(a.stroke(pts, 1.5), C['obsidian'], t0=.8, t1=1.7, sigma=.55, spec=.5)
        a.part(a.stroke([(8 + .8 * math.sin(u * 14 + 1.2), 3 + u * 19) for u in np.linspace(0, 1, 40)], .45), C['violet'], t0=.8, t1=1.3, sigma=.2, spec=.5, glow=.5)
        for s in (-1, 1):
            a.part(a.poly([(8 + s * .6, 22), (8 + s * 3.4, 25.5), (8 + s * 2.6, 29), (8 + s * 1.2, 26.8)]), C['night'], t0=.6, t1=1.5, sigma=.4, spec=.5)
        a.gem(8, 26, 1.7, C['magenta'], ry=1.15)
        a.part(a.ellipse(8, 26, .32, .95), C['obsidian'], t0=1.6, t1=1.6, flat=True)
    elif v == 8:    # sceptre du neant : orbe de vide entoure d'eclats
        shaft(a, -.5, 22, C['night'], 1.25, C['obsidian'], collars=[(9, C['void']), (21, C['void'])])
        for ang in (30, 90, 150, 210, 270, 330):
            r = math.radians(ang)
            a.part(a.poly([(8 + 2.9 * math.cos(r - .22), 26 + 2.9 * math.sin(r - .22)), (8 + 5.0 * math.cos(r), 26 + 5.0 * math.sin(r)), (8 + 2.9 * math.cos(r + .22), 26 + 2.9 * math.sin(r + .22))]), C['void'], t0=.7, t1=1.7, sigma=.4, spec=.6)
        a.gem(8, 26, 2.2, C['obsidian'], facets=False)
        a.gem(8, 26, 1.4, C['violet'], facets=True)
    elif v == 9:    # baton du phenix : plumes de feu en eventail, orbe ardent
        shaft(a, -.5, 22, C['crimson'], 1.3, C['wood2'], collars=[(9, C['gold']), (21.5, C['gold'])])
        for ang, col in [(-70, C['red']), (-40, C['ember']), (-12, C['yellow']), (12, C['yellow']), (40, C['ember']), (70, C['red'])]:
            r = math.radians(ang)
            L = 7.5 - abs(ang) * .03
            a.part(a.stroke([(8 + L * u * math.sin(r), 24 + L * u * math.cos(r)) for u in np.linspace(.2, 1, 6)], [.5, 1.3, 1.5, 1.3, .9, .3]), col, t0=.6, t1=1.4, sigma=.4, spec=.4, glow=1)
        a.part(a.stroke([(8, 22), (8, 24.8)], 1.4), C['gold'], t0=.9, t1=1.8, sigma=.4, spec=.6)
        a.gem(8, 26.2, 1.5, C['ember'])
    else:           # baton d'archimage : couronne etoilee, orbes en orbite
        shaft(a, -.5, 22, C['white'] * .92, 1.3, C['azure'], collars=[(8.4, C['gold']), (12, C['gold']), (20.5, C['gold'])])
        prongs(a, 22, C['gold'], spread=3.4, h=6.2, w=.95)
        a.gem(8, 26.2, 2.0, C['azure'])
        a.gem(8, 26.2, 1.05, C['white'], facets=False)
        for (x, y, r) in [(4.0, 29.4, .8), (12.0, 29.4, .8), (8, 30.8, .8)]:
            a.gem(x, y, r, C['cyan'], facets=False)
        a.part(a.poly([(8, 29), (7.4, 30.8), (8, 32), (8.6, 30.8)]), C['gold'], t0=.7, t1=1.6, sigma=.3, spec=.7)


# --------------------------------------------------------------------- masses et marteaux

def haft(a, y0, y1, col, w=1.5, wrap=None, collars=()):
    a.part(a.rect(8, (y0 + y1) / 2, w, y1 - y0), col, t0=.8, t1=1.7, sigma=w * .4, spec=.25, grain=.08, vgrain=True)
    if wrap is not None:
        a.part(a.rect(8, 6, w + .3, 8), wrap, t0=.9, t1=1.9, sigma=w * .45, spec=.12, grain=.07, vgrain=True)
        for i in range(8):
            y = 2.5 + i * .95
            a.cut(a.line(8 - w / 2 - .2, y, 8 + w / 2 + .2, y + .35, .26), .2, shade(wrap, .55), .6)
    for y, mat in collars:
        a.part(a.rect(8, y, w + 1.0, .8), mat, t0=.9, t1=2.1, sigma=.3, spec=.6)


def flanged_head(a, cy, mat, n=6, r=2.4, fl=2.7, h=6.0, spikes=False):
    a.part(a.rect(8, cy, r * 1.15, h), mat, t0=1, t1=2.3, sigma=.9, spec=.6)
    for i in range(n):
        s = -1 if i % 2 == 0 else 1
        k = (i // 2 + .5) / (n / 2)
        y = cy - h / 2 + k * h
        if spikes:
            a.part(a.poly([(8 + s * r * .5, y - .9), (8 + s * (r + 1.7), y), (8 + s * r * .5, y + .9)]), mat, t0=.7, t1=1.9, sigma=.4, spec=.6)
        else:
            a.part(a.poly([(8 + s * r * .5, y - h / (n / 2) * .45), (8 + s * (r + fl * .6), y - h / (n / 2) * .3), (8 + s * (r + fl * .6), y + h / (n / 2) * .3), (8 + s * r * .5, y + h / (n / 2) * .45)]), shade(mat, 1.05), t0=.7, t1=1.9, sigma=.4, spec=.6)
    a.part(a.ellipse(8, cy + h / 2 + .3, r * .6, .9), mat, t0=1, t1=2.0, sigma=.4, spec=.6)


def hammer(a, v, pal):
    wood, steel = C['wood'], C['steel']
    if v == 0:      # masse de la garde : tete a ailettes en fer
        haft(a, -.5, 17, wood, 1.5, C['leather'])
        a.part(a.ellipse(8, 0, 1.5, 1.1), C['iron'], t0=.9, t1=1.8, sigma=.5, spec=.5)
        flanged_head(a, 21.3, C['iron'], n=6, r=1.9, fl=2.0, h=6.6)
    elif v == 1:    # masse cloutee : tete ronde hérissee de pointes
        haft(a, -.5, 17.5, C['wood2'], 1.5, C['leather2'])
        a.part(a.ellipse(8, 22.3, 3.6), C['dark'], t0=1, t1=2.3, sigma=1.8, spec=.6)
        for ang in range(0, 360, 40):
            r = math.radians(ang)
            a.part(a.poly([(8 + 3.0 * math.cos(r - .26), 22.3 + 3.0 * math.sin(r - .26)), (8 + 5.2 * math.cos(r), 22.3 + 5.2 * math.sin(r)), (8 + 3.0 * math.cos(r + .26), 22.3 + 3.0 * math.sin(r + .26))]), C['iron'], t0=.6, t1=1.8, sigma=.4, spec=.6)
        a.part(a.ellipse(8, 22.3, 1.1), C['steel'], t0=1.2, t1=2.2, sigma=.4, spec=.8)
    elif v == 2:    # marteau du bastion : tete massive rectangulaire a bandes
        haft(a, -.5, 19, wood, 1.6, C['leather'])
        a.part(a.rect(8, 23, 10.4, 5.8), C['steel'], t0=1.2, t1=2.4, sigma=1.5, spec=.7)
        a.part(a.rect(8, 23, 2.4, 6.4), C['dark'], t0=1.2, t1=2.4, sigma=.7, spec=.5)
        for s in (-1, 1):
            a.part(a.rect(8 + s * 4.6, 23, 1.0, 6.4), C['iron'], t0=1.2, t1=2.4, sigma=.4, spec=.6)
        for x in (4.0, 12.0):
            for y in (21.4, 24.6):
                a.part(a.ellipse(x, y, .45), C['gold'], t0=1.4, t1=2.4, sigma=.2, spec=.8)
    elif v == 3:    # marteau de forgeron : tete d'enclume, panne
        haft(a, -.5, 19.5, wood, 1.6, C['leather'], collars=[(18.5, C['bronze'])])
        a.part(a.poly([(1.5, 22.2), (5, 25.8), (13.5, 25.8), (14.5, 22.2), (13, 20.3), (5, 20.3)]), C['iron'], t0=1.1, t1=2.4, sigma=1.4, spec=.7)
        a.part(a.rect(8, 23, 3.0, 6.4), C['dark'], t0=1.1, t1=2.3, sigma=.7, spec=.5)
        a.part(a.rect(13.8, 23, 1.8, 4.4), C['steel'], t0=1.1, t1=2.3, sigma=.5, spec=.8)
        a.paint(a.rect(8, 23, 1.0, 1.0), C['ember'], .8, glow=.6)
    elif v == 4:    # marteau du colosse : maillet enorme, anneaux
        haft(a, -.5, 17, C['wood2'], 1.8, C['leather2'])
        a.part(a.rect(8, 22.8, 12.6, 7.4), C['bronze'], t0=1.2, t1=2.4, sigma=1.8, spec=.6)
        for s in (-1, 1):
            a.part(a.rect(8 + s * 5.2, 22.8, 2.2, 7.8), C['dark'], t0=1.3, t1=2.4, sigma=.8, spec=.6)
        a.part(a.rect(8, 22.8, 3.2, 8), C['iron'], t0=1.3, t1=2.4, sigma=.9, spec=.6)
        a.gem(8, 22.8, 1.0, C['ember'])
    elif v == 5:    # masse du templier : croix sainte, cuir blanc
        haft(a, -.5, 17.5, C['white'] * .85, 1.5, C['azure'] * .7, collars=[(16.5, C['gold'])])
        flanged_head(a, 21.4, C['silver'], n=6, r=2.1, fl=2.2, h=6.8)
        a.part(a.rect(8, 22.8, 1.5, 5), C['gold'], t0=1.3, t1=2.4, sigma=.4, spec=.8)
        a.part(a.rect(8, 23.4, 4.4, 1.5), C['gold'], t0=1.3, t1=2.4, sigma=.4, spec=.8)
        a.part(a.poly([(8, 25.2), (7.2, 27.2), (8, 28.6), (8.8, 27.2)]), C['silver'], t0=.8, t1=1.9, sigma=.4, spec=.7)
    elif v == 6:    # masse du gardien : etoile a six pointes dorees
        haft(a, -.5, 17, C['white'] * .85, 1.5, C['teal'], collars=[(16.5, C['gold'])])
        a.part(a.ellipse(8, 22.6, 3.0), C['gold'], t0=1.1, t1=2.4, sigma=1.4, spec=.8)
        for ang in range(0, 360, 60):
            r = math.radians(ang + 30)
            a.part(a.poly([(8 + 2.6 * math.cos(r - .3), 22.6 + 2.6 * math.sin(r - .3)), (8 + 6.2 * math.cos(r), 22.6 + 6.2 * math.sin(r)), (8 + 2.6 * math.cos(r + .3), 22.6 + 2.6 * math.sin(r + .3))]), C['silver'], t0=.6, t1=1.8, sigma=.4, spec=.8)
        a.gem(8, 22.6, 1.3, C['azure'])
    elif v == 7:    # marteau runique : tete gravee de runes lumineuses
        haft(a, -.5, 18, C['night'], 1.6, C['leather2'], collars=[(17.2, C['gold2'])])
        a.part(a.rect(8, 22.6, 11.2, 6.4), C['dark'], t0=1.2, t1=2.4, sigma=1.4, spec=.7)
        a.part(a.rect(8, 22.6, 11.2, 1.0), C['gold2'], t0=1.2, t1=2.4, sigma=.3, spec=.7)
        for x in (4.3, 6.2, 8.0, 9.8, 11.7):
            a.paint(a.rect(x, 24.5 if int(x) % 2 else 20.7, .5, 1.1), C['cyan'], .95, glow=1)
        a.paint(a.rect(8, 22.6, .9, 2), C['cyan'], .9, glow=1)
    elif v == 8:    # marteau du titan : roche volcanique, fissures de lave
        haft(a, -.5, 18, C['obsidian'], 1.8, C['rust'])
        a.part(a.poly([(1.2, 21), (3, 26.2), (7, 27.2), (11.5, 26.6), (14.8, 22.6), (13, 19.6), (5, 19.2)]), C['night'], t0=1.2, t1=2.4, sigma=1.5, spec=.35, grain=.1)
        for pts in ([(3.2, 21.4), (6, 23), (5.4, 25.4)], [(8, 20), (9.4, 22.6), (12, 23.4)], [(7, 26.4), (8.6, 24.4)]):
            a.paint(a.stroke(pts, .45) & (a.a > 0), C['ember'], .95, glow=1)
        a.part(a.rect(8, 18.4, 3.4, 1.0), C['bronze'], t0=1, t1=2.0, sigma=.3, spec=.6)
    elif v == 9:    # marteau des rois anciens : or, couronne et griffes
        haft(a, -.5, 17, C['crimson'], 1.6, C['leather2'], collars=[(16.2, C['gold'])])
        a.part(a.rect(8, 22.2, 11.6, 6.0), C['gold'], t0=1.2, t1=2.4, sigma=1.5, spec=.85)
        for x in (3.2, 5.6, 8, 10.4, 12.8):
            a.part(a.poly([(x - .9, 25), (x, 27.6 if x == 8 else 26.6), (x + .9, 25)]), C['gold'], t0=.7, t1=1.9, sigma=.4, spec=.85)
        a.gem(8, 22.2, 1.5, C['red'])
        for x in (4, 12):
            a.gem(x, 22.2, .6, C['azure'], facets=False)
    else:           # marteau de l'egide : bouclier-marteau cathedrale, ailes
        haft(a, -.5, 17, C['white'] * .85, 1.6, C['azure'] * .7, collars=[(16.2, C['gold'])])
        a.part(a.poly([(8, 18.4), (3, 20.4), (3, 26), (8, 28.6), (13, 26), (13, 20.4)]), C['silver'], t0=1.2, t1=2.4, sigma=1.5, spec=.85)
        a.part(a.poly([(8, 19.6), (4.3, 21.2), (4.3, 25.4), (8, 27.4), (11.7, 25.4), (11.7, 21.2)]), C['white'], t0=1.4, t1=2.4, sigma=1.0, spec=.8)
        a.part(a.rect(8, 23.4, 1.3, 6), C['gold'], t0=1.5, t1=2.4, sigma=.4, spec=.8)
        a.part(a.rect(8, 23.8, 6, 1.3), C['gold'], t0=1.5, t1=2.4, sigma=.4, spec=.8)
        for s in (-1, 1):
            for i in range(3):
                a.part(a.poly([(8 + s * 5.4, 22.4 + i * 1.5), (8 + s * 7.8, 23.6 + i * 1.3 + 1), (8 + s * 5.4, 24.6 + i * 1.5)]), C['gold'], t0=.6, t1=1.5, sigma=.3, spec=.8)
        a.gem(8, 23.6, .8, C['azure'])


# --------------------------------------------------------------------- arcs (plan 16x16 comme l'arc vanilla)

def bow(a, v, pal, pull=0):
    # arc : bezier B -> C -> T, bombee vers le coin haut-gauche, corde en bas-droite
    palette = [
        (C['wood'], C['wood2'], C['leather'], None),
        (C['wood'], C['brass'], C['leather2'], None),
        (C['leaf'], C['wood'], C['gold'], C['green']),
        (C['bone'], C['wood2'], C['bronze'], None),
        (C['azure'], C['night'], C['silver'], C['yellow']),
        (C['wood2'], C['leaf'], C['leather'], C['green']),
        (C['rust'], C['gold'], C['night'], C['red']),
        (C['silver'], C['ice'], C['night'], C['azure']),
        (C['void'], C['cyan'], C['obsidian'], C['cyan']),
        (C['crimson'], C['gold'], C['night'], C['ember']),
        (C['white'], C['gold'], C['azure'], C['yellow']),
    ][v]
    limb, trim, handle, jewel = palette
    # (bulge, span, largeur, enroulement du bout)
    bulge, span, wsc, curl = [(.85, 1.0, .75, 0), (.55, .86, .85, 0), (.95, 1.0, .7, 1), (.7, .92, 1.05, 0), (1.0, 1.0, .8, 1),
                              (.8, 1.0, .9, 0), (.6, .95, 1.1, 0), (1.05, 1.0, .65, 1), (.9, 1.0, .85, 1), (.75, 1.0, 1.0, 0), (1.1, 1.0, .75, 1)][v]
    lo = 1.5 + (1 - span) * 5
    hi = 15.1 - (1 - span) * 3
    B, T = np.array([lo, lo * .6 + .3]), np.array([hi, hi - .2])
    Cc = np.array([-1.4 + (1 - bulge) * 6, 16.2 - (1 - bulge) * 5.5 - (1 - span) * 4])
    ts = np.linspace(0, 1, 40)
    pts = [tuple((1 - t) ** 2 * B + 2 * (1 - t) * t * Cc + t * t * T) for t in ts]
    widths = [(.55 + 1.5 * math.sin(t * math.pi) ** .8) * wsc + (.5 if 17 < i < 23 else 0) for i, t in enumerate(ts)]
    if curl:   # extremites recourbees vers l'exterieur (arc reflexe)
        pts[0] = (pts[0][0] + .5, pts[0][1] - .15)
        pts[-1] = (pts[-1][0] + .5, pts[-1][1] - .5)
    # pieces decoratives de bout de branche (autour de l'arc)
    if v in (2, 5):      # feuilles / lianes
        for t in (.2, .33, .62, .74, .85):
            x, y = pts[int(t * 39)]
            a.part(a.poly([(x, y), (x - 2.6, y + 1.0), (x - 3.2, y - .2), (x - 1.8, y - 1.0)]), C['leaf'], t0=.5, t1=1.1, sigma=.4, spec=.3)
    if v in (4, 7, 10, 9):  # ailes
        for s, t in ((0, .12), (1, .88)):
            x, y = pts[int(t * 39)]
            d = -1 if s == 0 else 1
            for i in range(3):
                a.part(a.poly([(x, y + i * .2 * d), (x - 2.4 - i * .7, y + 1.6 + i * 1.0) if s else (x - .6 - i * .8, y + 2.4 + i * 1.0), (x - 1.0 - i * .6, y + .4 + i * .7) if s else (x + 1.6, y + 1.2 + i * 1.0)]), jewel if i == 2 and jewel is not None else C['white'] * (.95 - i * .08), t0=.5, t1=1.2, sigma=.3, spec=.7)
    a.part(a.stroke(pts, widths), limb, t0=.6, t1=1.6, sigma=.7, spec=.45, grain=.06, vgrain=False)
    a.paint(a.stroke(pts[4:36], [w * .18 for w in widths[4:36]]) & (a.a > 0), shade(limb, 1.25), .45)
    # enroulements
    for t in (.16, .30, .70, .84):
        i = int(t * 39)
        x, y = pts[i]
        x2, y2 = pts[i + 1]
        dx, dy = y2 - y, -(x2 - x)
        n = math.hypot(dx, dy) or 1
        w = widths[i] / 2 + .2
        a.part(a.line(x - dx / n * w, y - dy / n * w, x + dx / n * w, y + dy / n * w, .55), trim, t0=1.2, t1=1.9, flat=True, spec=.7)
    # poignee et gemme centrale
    mid = pts[20]
    a.part(a.stroke(pts[15:26], [2.5] * 11), handle, t0=1, t1=2.2, sigma=.8, spec=.15, grain=.07)
    for i in range(16, 25, 2):
        x, y = pts[i]
        x2, y2 = pts[i + 1]
        dx, dy = y2 - y, -(x2 - x)
        n = math.hypot(dx, dy) or 1
        a.cut(a.line(x - dx / n * 1.2, y - dy / n * 1.2, x + dx / n * 1.2, y + dy / n * 1.2, .26), .25, shade(handle, .5), .6)
    if jewel is not None:
        a.gem(mid[0] - .15, mid[1] + .1, .85, jewel)
    # extremites (nocks)
    for p in (pts[0], pts[-1]):
        a.part(a.ellipse(p[0], p[1], .75), trim, t0=1, t1=1.8, sigma=.3, spec=.8)
    # corde et fleche
    nock = {0: (8.9, 7.0), 1: (10.4, 5.5), 2: (11.4, 4.6), 3: (12.4, 3.6)}[pull]
    scol = C['white'] * .8
    a.part(a.stroke([pts[0], nock, pts[-1]], .42), scol, t0=.45, t1=.45, flat=True, spec=.2, grain=0)
    if pull:
        tip = np.array([1.6, 14.4])
        nk = np.array(nock)
        d = (tip - nk) / np.linalg.norm(tip - nk)
        tail = nk - d * 2.2
        a.part(a.line(tail[0], tail[1], tip[0], tip[1], .6), C['wood'], t0=.9, t1=.9, flat=True, spec=.3)
        n = np.array([-d[1], d[0]])
        a.part(a.poly([tuple(tip + d * 2.2), tuple(tip + n * .95), tuple(tip - n * .95)]), C['steel'], t0=1.0, t1=1.8, sigma=.3, spec=.9)
        for off in (0, .7):
            base = tail + d * off
            a.part(a.poly([tuple(base + n * .2), tuple(base + d * 1.3 + n * 1.3), tuple(base + d * 1.6), tuple(base - n * .2)]), C['red'] if v % 2 == 0 else C['white'], t0=.5, t1=.9, sigma=.2)
            a.part(a.poly([tuple(base - n * .2), tuple(base + d * 1.3 - n * 1.3), tuple(base + d * 1.6), tuple(base + n * .2)]), C['red'] if v % 2 == 0 else C['white'], t0=.5, t1=.9, sigma=.2)
