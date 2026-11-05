"""Decors « cathedrale d'Aeloria » : gothique violet et or, tres detailles (arcs brises, meneaux, vitraux, nervures, crochets...).

Echelle : 1 bloc de citadelle = 48 unites. Enregistre ses modeles dans gen_decor_bb.MODELS (python tools/gen_decor_bb.py).
Repere : x largeur, y hauteur (0 = sol), z profondeur, face avant vers -z, centre du bloc en (0, 0).
"""
import math

import gen_decor_bb as D
from gen_decor_bb import B, Bc, cyl, taper, pyramid, new, model

ST, ST2, MA, MA2, GO = D.STONE, D.STONE2, D.MARB, D.MARB2, D.GOLD
WD, WD2, IR, IR2 = D.WOOD, D.WOOD2, D.IRON, D.IRON2
CR, CR2, FL, WA = D.CRYS, D.CRYS2, D.FLAME, D.WATER
GL = ('glowc', '#b78aff')
GL2 = ('glowc', '#ffd86a')
GL3 = ('glowc', '#8a5ae0')
GLASS = (GL, GL3, GL2)
BONE = ('bone', '#e8dfc4')
CAN = ('bone', '#f4efe0')
VIO = ('banner', '#5a2a8a', '#e0b040')
VIO2 = ('cloth_plain', '#6a3fa8')


# ============================================================================ bibliotheque de details
def arch(m, cx, z, span, y0, t, dep, mat, step=2.0):
    """Arc brise (lancette) en marches ; retourne la hauteur du sommet."""
    R, half = span, span / 2
    apex = math.sqrt(R * R - half * half)
    y = 0.0
    while y < apex - step * .4:
        xl = cx + half - math.sqrt(R * R - y * y)
        B(m, (xl, y0 + y, z - dep / 2), (xl + t, y0 + y + step, z + dep / 2), mat)
        B(m, (2 * cx - xl - t, y0 + y, z - dep / 2), (2 * cx - xl, y0 + y + step, z + dep / 2), mat)
        y += step
    B(m, (cx - t * .6, y0 + apex - step, z - dep / 2 - .6), (cx + t * .6, y0 + apex + t * .5, z + dep / 2 + .6), GO)   # clef
    return y0 + apex


def glass_arch(m, cx, z, span, y0, t, dep, step=2.0, mullion=1.4):
    """Vitrail en lancette : trois bandes de verre colore separees par des meneaux."""
    R, half = span, span / 2
    apex = math.sqrt(R * R - half * half)
    y, band = 0.0, 0
    gd = dep * .3
    while y < apex - step * .6:
        xl = cx + half - math.sqrt(R * R - y * y) + t
        xr = 2 * cx - xl
        w = xr - xl
        if w > 2:
            third = (w - 2 * mullion) / 3
            for k in range(3):
                x0 = xl + k * (third + mullion)
                B(m, (x0, y0 + y, z - gd / 2), (x0 + third, y0 + y + step, z + gd / 2), GLASS[(k + band // 3) % 3])
        y += step
        band += 1
    return y0 + apex


def window(m, cx, z, span, straight, dep, y0=0.0, t=4.0):
    """Fenetre gothique complete : pieds-droits, allege, vitrail, meneaux, arc brise et gable."""
    hw = span / 2
    for s in (-1, 1):
        B(m, (cx + s * hw - (t if s < 0 else 0) - (0 if s < 0 else 0) + (0 if s < 0 else -t) + (-0), y0, z - dep / 2),
          (cx + s * hw + (0 if s < 0 else 0), y0 + straight, z + dep / 2), ST) if False else None
    B(m, (cx - hw, y0, z - dep / 2), (cx - hw + t, y0 + straight, z + dep / 2), ST)
    B(m, (cx + hw - t, y0, z - dep / 2), (cx + hw, y0 + straight, z + dep / 2), ST)
    B(m, (cx - hw - 2, y0, z - dep / 2 - 2), (cx + hw + 2, y0 + 4, z + dep / 2 + 2), MA)      # seuil
    gd = dep * .3
    inner = span - 2 * t
    third = (inner - 2 * 1.4) / 3
    for k in range(3):                                       # verre droit en trois lancettes
        x0 = cx - hw + t + k * (third + 1.4)
        for j in range(int((straight - 6) // 6)):
            B(m, (x0, y0 + 4 + j * 6, z - gd / 2), (x0 + third, y0 + 4 + j * 6 + 6, z + gd / 2), GLASS[(k + j) % 3])
    for k in (1, 2):                                         # meneaux
        x = cx - hw + t + k * third + (k - 1) * 1.4
        B(m, (x, y0 + 4, z - dep / 2 + 1), (x + 1.4, y0 + straight, z + dep / 2 - 1), ST)
    for yy in (y0 + straight * .45, y0 + straight * .8):     # traverses
        B(m, (cx - hw + t, yy, z - dep / 2 + .5), (cx + hw - t, yy + 1.6, z + dep / 2 - .5), ST)
    top = arch(m, cx, z, span, y0 + straight, t, dep, ST)
    glass_arch(m, cx, z, span, y0 + straight, t, dep)
    ytr = y0 + straight + span * .55                         # oculus trilobe
    ring(m, cx, ytr, z, 5.5, 8, 1.6, ST, dep * .8)
    B(m, (cx - 2, ytr - 2, z - gd / 2), (cx + 2, ytr + 2, z + gd / 2), GL2)
    top2 = arch(m, cx, z, span + 8, y0 + straight - 2, 2.5, dep + 3, MA)       # larmier
    return top2


def ring(m, cx, cy, z, R, n, s, mat, dep):
    """Anneau de n barres tangentes dans le plan XY."""
    L_ = 2 * math.pi * R / n * 1.08
    for i in range(n):
        a = 2 * math.pi * i / n
        x, y = cx + R * math.cos(a), cy + R * math.sin(a)
        B(m, (x - L_ / 2, y - s / 2, z - dep / 2), (x + L_ / 2, y + s / 2, z + dep / 2), mat, (0, 0, math.degrees(a) + 90), (x, y, z))


def spoke(m, cx, cy, z, r0, r1, deg, w, dep, mat):
    B(m, (cx + r0, cy - w / 2, z - dep / 2), (cx + r1, cy + w / 2, z + dep / 2), mat, (0, 0, deg), (cx, cy, z))


def candle(m, cx, cz, y0, h, r=1.6, flame=True):
    cyl(m, cx, cz, y0, y0 + h, r, CAN, small=True)
    Bc(m, cx + r * .9, cz, y0 + h * .5, y0 + h * .9, .35, .5, CAN)            # coulure
    Bc(m, cx, cz, y0 + h, y0 + h + .8, .3, .3, IR2)                           # meche
    if flame:
        Bc(m, cx, cz, y0 + h + .8, y0 + h + 4.2, .9, .9, FL)


def pinnacle(m, cx, cz, y0, h, w, mat=ST, crockets=True):
    """Clocheton : fut, corniche, pyramide a crochets et fleuron."""
    Bc(m, cx, cz, y0, y0 + h * .55, w, w, mat)
    Bc(m, cx, cz, y0 + h * .55, y0 + h * .6, w + .8, w + .8, GO)
    n = 6
    for i in range(n):
        a = y0 + h * .6 + h * .32 * i / n
        r = w * (1 - .82 * (i + .5) / n)
        Bc(m, cx, cz, a, a + h * .32 / n, r, r, mat)
        if crockets and i % 2 == 0:
            for sx, sz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                Bc(m, cx + sx * (r + .5), cz + sz * (r + .5), a, a + h * .32 / n * .9, .5 + abs(sz) * .3, .5 + abs(sx) * .3, GO)
    Bc(m, cx, cz, y0 + h * .92, y0 + h, .8, .8, GO)


def cluster(m, cx, cz, y0, y1, r, mat=MA):
    """Pilier fasciculé : fut central + 4 colonnettes, bases et chapiteau dores."""
    cyl(m, cx, cz, y0 + 6, y1 - 8, r, mat)
    for sx, sz in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        cyl(m, cx + sx * r * 1.05, cz + sz * r * 1.05, y0 + 6, y1 - 8, r * .42, mat)
    Bc(m, cx, cz, y0, y0 + 3, r * 1.9, r * 1.9, ST2)
    Bc(m, cx, cz, y0 + 3, y0 + 6, r * 1.6, r * 1.6, ST)
    cyl(m, cx, cz, y0 + 6, y0 + 8, r * 1.35, GO)
    cyl(m, cx, cz, y1 - 10, y1 - 8, r * 1.35, GO)
    taper(m, cx, cz, y1 - 8, y1 - 3, r * 1.2, r * 1.9, mat, 3)
    Bc(m, cx, cz, y1 - 3, y1, r * 2.1, r * 2.1, GO)


def quatrefoil(m, cx, cy, z, R, dep, mat=ST, glass=GL):
    for a in (0, 90, 180, 270):
        x, y = cx + R * .5 * math.cos(math.radians(a)), cy + R * .5 * math.sin(math.radians(a))
        ring(m, x, y, z, R * .5, 8, 1.2, mat, dep)
    B(m, (cx - R * .22, cy - R * .22, z - dep * .2), (cx + R * .22, cy + R * .22, z + dep * .2), glass)


def corbel(m, cx, cz, y0, w, h, mat=ST, facing=-1):
    for i in range(4):
        Bc(m, cx, cz + facing * (i * w * .25), y0 + h - (i + 1) * h / 4, y0 + h - i * h / 4 + 0, w / 2 * (1 - i * .12), w * .12 + .6, mat)


# ============================================================================ les decors
@model('deco_cat_vitrail', 'Vitrail de la cathédrale', 'Cathedral Stained Window', (0, 0, 5, 16, 32, 11), 7)
def vitrail():
    m = new('deco_cat_vitrail', 101)
    B(m, (-36, 0, -7), (36, 6, 7), ST2)
    B(m, (-34, 6, -6), (34, 9, 6), ST)
    top = window(m, 0, 0, 36, 70, 10, y0=9)
    for s in (-1, 1):
        cluster(m, s * 30, 0, 0, 130, 4.2)
        pinnacle(m, s * 30, 0, 130, 36, 4)
        B(m, (s * 30 - 1.6, 36, -9), (s * 30 + 1.6, 96, -7), ST)                      # contrefort plat
    ring(m, 0, top + 12, 0, 9, 12, 2.2, GO, 5)
    quatrefoil(m, 0, top + 12, 0, 10, 4, ST)
    return m


@model('deco_cat_rosace', 'Rosace de la cathédrale', 'Cathedral Rose Window', (0, 0, 5, 16, 32, 11), 8)
def rosace():
    m = new('deco_cat_rosace', 102)
    B(m, (-46, 0, -6), (46, 6, 6), ST2)
    B(m, (-44, 6, -5), (44, 100, 5), ST)
    cy = 54
    for R, s, mat in ((38, 4, GO), (33, 3, MA), (24, 3, MA), (13, 3, GO)):
        ring(m, 0, cy, -6, R, 36 if R > 20 else 24, s, mat, 8)
    for i in range(16):                                    # petales de verre
        deg = i * 22.5
        spoke(m, 0, cy, -5.6, 7, 31.5, deg, 7.5, 1, GLASS[i % 3])
    for i in range(16):                                    # meneaux entre les petales
        spoke(m, 0, cy, -6, 7, 33, i * 22.5 + 11.25, 1.6, 2.4, MA)
    for i in range(8):                                     # petites lancettes de la couronne externe
        a = math.radians(i * 45 + 22.5)
        x, y = 28.5 * math.cos(a), cy + 28.5 * math.sin(a)
        B(m, (x - 2.2, y - 2.2, -6.4), (x + 2.2, y + 2.2, -5.2), GL2)
    B(m, (-6, cy - 6, -7.5), (6, cy + 6, -5), GO)
    B(m, (-3.5, cy - 3.5, -8.5), (3.5, cy + 3.5, -7), CR)
    for s in (-1, 1):
        pinnacle(m, s * 40, 0, 100, 26, 3.4)
    B(m, (-44, 98, -6), (44, 104, 6), MA)
    return m


@model('deco_cat_arc_brise', 'Arc brisé de la nef', 'Nave Pointed Arch', (0, 0, 4, 16, 32, 12), 0)
def arc_brise():
    m = new('deco_cat_arc_brise', 103)
    for s in (-1, 1):
        cluster(m, s * 26, 0, 0, 74, 4.6)
    top = arch(m, 0, 0, 52, 74, 5, 11, MA)
    arch(m, 0, 0, 44, 74, 2.5, 7, ST)
    arch(m, 0, 0, 60, 70, 2.5, 13, GO)
    B(m, (-4, top, -8), (4, top + 9, 8), GO)
    for s in (-1, 1):
        pinnacle(m, s * 26, 0, 74, 40, 3)
    cyl(m, 0, 0, top + 9, top + 14, 3, CR)
    return m


@model('deco_cat_contrefort', 'Arc-boutant', 'Flying Buttress', (0, 0, 2, 16, 32, 14), 0)
def contrefort():
    m = new('deco_cat_contrefort', 104)
    B(m, (-40, 0, -8), (-16, 10, 8), ST2)
    for i, (w, h) in enumerate(((11, 24), (10, 22), (9, 20), (8, 18))):               # contrefort en retraits
        y0 = 10 + sum(x[1] for x in ((11, 24), (10, 22), (9, 20), (8, 18))[:i])
        B(m, (-28 - w, y0, -w * .7), (-28 + w, y0 + h, w * .7), ST)
        B(m, (-28 - w - 1, y0 + h - 1.6, -w * .7 - 1), (-28 + w + 1, y0 + h + .4, w * .7 + 1), MA)
    pinnacle(m, -28, 0, 94, 40, 6)
    n = 16                                              # volee de l'arc-boutant
    for i in range(n):
        a = math.pi * .5 * i / n
        x = -20 + (44) * math.sin(a) * .95
        y = 86 - (24) * (1 - math.cos(a)) * 1.0
        B(m, (x - 3, y - 3, -3.5), (x + 3, y + 3, 3.5), MA if i % 2 else ST)
    for i in range(n):                                   # ajours (tympan)
        a = math.pi * .5 * (i + .5) / n
        x = -20 + 44 * math.sin(a) * .95
        y = 86 - 24 * (1 - math.cos(a))
        if i % 3 == 1:
            B(m, (x - 1.2, y - 12, -.8), (x + 1.2, y - 4, .8), ST)
    B(m, (18, 0, -8), (34, 64, 8), ST)                   # pile du mur
    B(m, (16, 64, -9), (36, 70, 9), MA)
    for y in (16, 34, 52):
        B(m, (17, y, -9), (35, y + 1.6, 9), ST2)
    return m


@model('deco_cat_pilier_faisceau', 'Pilier à nervures', 'Ribbed Pier', (3, 0, 3, 13, 32, 13), 0)
def pilier():
    m = new('deco_cat_pilier_faisceau', 105)
    cluster(m, 0, 0, 0, 138, 7)
    for a in range(0, 360, 90):                           # nervures de voute en depart
        for k in range(6):
            r = math.radians(a)
            x, z = math.cos(r) * (9 + k * 2.2), math.sin(r) * (9 + k * 2.2)
            B(m, (x - 1.4, 134 + k * 1.8, z - 1.4), (x + 1.4, 138 + k * 2.4, z + 1.4), MA if k % 2 else ST)
    for y in (30, 60, 96):
        cyl(m, 0, 0, y, y + 2.4, 9.6, GO)
    cyl(m, 0, 0, 138, 142, 5, CR)
    return m


@model('deco_cat_chaire', 'Chaire de la cathédrale', 'Cathedral Pulpit', (2, 0, 2, 14, 32, 14), 0)
def chaire():
    m = new('deco_cat_chaire', 106)
    cyl(m, 0, 0, 0, 5, 16, ST2)
    taper(m, 0, 0, 5, 40, 7, 5, MA, 4)
    cyl(m, 0, 0, 20, 23, 8, GO)
    cyl(m, 0, 0, 40, 46, 16, WD2)
    cyl(m, 0, 0, 46, 62, 17, WD)
    for a in range(0, 360, 45):                               # panneaux a arcatures
        r = math.radians(a + 22.5)
        x, z = math.cos(r) * 17.4, math.sin(r) * 17.4
        B(m, (x - 3.6, 49, z - 3.6), (x + 3.6, 59, z + 3.6), GO, (0, -a - 22.5 + 90, 0), (x, 49, z))
    cyl(m, 0, 0, 62, 65, 18.5, GO)
    cyl(m, 0, 0, 65, 68, 18, WD2)
    for a in range(0, 360, 90):                               # colonnettes du dais
        r = math.radians(a + 45)
        cyl(m, math.cos(r) * 15, math.sin(r) * 15, 68, 108, 1.5, GO)
    cyl(m, 0, 0, 108, 112, 18, WD2)
    taper(m, 0, 0, 112, 136, 17, 3, WD, 6)
    cyl(m, 0, 0, 136, 144, 1.5, GO)
    Bc(m, 0, 0, 144, 150, 2.2, 2.2, GO)
    B(m, (-1, 148, -9), (1, 168, 9), GO) if False else None
    B(m, (-1.4, 66, -12), (1.4, 74, -3), IR2)
    for i in range(7):                                        # escalier d'acces
        B(m, (14 + i * 0, i * 6, 8 + i * 4), (30, i * 6 + 6, 12 + i * 4), ST) if False else None
    return m


@model('deco_cat_orgue', 'Grand orgue', 'Grand Organ', (0, 0, 3, 16, 32, 13), 0)
def orgue():
    m = new('deco_cat_orgue', 107)
    B(m, (-40, 0, -12), (40, 22, 10), WD2)
    B(m, (-42, 22, -14), (42, 26, 12), GO)
    for i in range(7):                                        # panneaux du buffet
        x = -34 + i * 11.4
        B(m, (x, 4, -13), (x + 8, 18, -12), GO)
        B(m, (x + 1.4, 5.4, -13.8), (x + 6.6, 16.6, -13), WD)
    pipes = [(-34, 72), (-28, 60), (-22, 82), (-16, 92), (-10, 78), (-4, 100), (0, 118), (4, 100), (10, 78), (16, 92), (22, 82), (28, 60), (34, 72)]
    for i, (x, h) in enumerate(pipes):
        r = 2.4 if h > 90 else 1.9
        cyl(m, x, -2, 26, 26 + h, r, ('metal', '#d8d0f0') if i % 2 else ('gold', '#e0b040'))
        Bc(m, x, -3.6, 26 + h * .18, 26 + h * .18 + 4, r * .8, .8, ST2)               # bouche
        pyramid(m, x, -2, 26 + h, 26 + h + 6, r, .4, GO, 2)
    for i in range(13):                                       # rang arriere
        x = -36 + i * 6
        cyl(m, x, 5, 26, 26 + 46 + (i % 3) * 10, 1.6, ('metal', '#c8c0e8'))
    for s in (-1, 1):
        cluster(m, s * 42, 0, 0, 128, 4.4, WD)
        pinnacle(m, s * 42, 0, 128, 32, 3.4, GO)
    B(m, (-44, 124, -8), (44, 130, 8), GO)
    for i in range(11):                                       # dentelle du couronnement
        x = -40 + i * 8
        B(m, (x, 130, -2), (x + 3, 144 - abs(i - 5) * 3, 2), GO)
    B(m, (-30, 8, -20), (30, 14, -12), WD)                    # tirasse / clavier
    for i in range(18):
        B(m, (-28 + i * 3.2, 14, -20), (-26 + i * 3.2, 15, -12), ('bone', '#f0ead8'))
    return m


@model('deco_cat_banc_nef', 'Banc de la nef', 'Nave Pew', (0, 0, 3, 16, 14, 13))
def banc():
    m = new('deco_cat_banc_nef', 108)
    B(m, (-44, 14, -6), (44, 19, 6), WD)                       # assise
    B(m, (-44, 19, 4), (44, 26, 6.4), WD)
    B(m, (-44, 26, 4.6), (44, 56, 6.4), WD)                    # dossier
    for i in range(11):                                        # ajours en lancettes
        x = -40 + i * 8
        B(m, (x, 34, 4), (x + 1.2, 52, 6.6), WD2)
        B(m, (x + 2, 52, 4), (x + 5, 54, 6.6), GO)
    B(m, (-44, 56, 3.4), (44, 60, 7), WD2)
    for s in (-1, 1):                                          # flancs sculptes
        x0, x1 = (s * 44 - 3, s * 44 + 3) if s > 0 else (s * 44 - 3, s * 44 + 3)
        B(m, (x0, 0, -8), (x1, 62, 8), WD2)
        B(m, (x0 - .4, 62, -8.4), (x1 + .4, 66, 8.4), GO)
        pyramid(m, s * 44, 0, 66, 76, 2.8, .4, GO, 3)
        B(m, (x0 - .6, 20, -3), (x1 + .6, 44, 3), GO) if False else None
        Bc(m, s * 44, 0, 28, 34, 3.6, 3.2, WD)
    B(m, (-44, 0, -9), (44, 5, 9), WD2)
    B(m, (-40, 4, -20), (40, 10, -12), WD2)                    # prie-dieu
    B(m, (-40, 10, -21), (40, 12, -11), VIO)
    return m


@model('deco_cat_autel_majeur', 'Autel majeur', 'High Altar', (0, 0, 2, 16, 32, 14), 11)
def autel():
    m = new('deco_cat_autel_majeur', 109)
    for i in range(3):
        B(m, (-44 + i * 3, i * 4, -22 + i * 3), (44 - i * 3, i * 4 + 4, 22 - i * 3), ST if i % 2 else MA)
    B(m, (-34, 12, -14), (34, 40, 8), MA)                      # mensa
    B(m, (-37, 40, -17), (37, 45, 11), GO)
    for i in range(5):                                         # arcature de face
        x = -28 + i * 14
        B(m, (x - 5, 14, -15), (x + 5, 36, -14), MA2)
        arch(m, x, -15.2, 10, 28, 1.5, 1.6, GO, 1.6)
        B(m, (x - 3.4, 15.4, -15.6), (x + 3.4, 30, -15), GL3)
        cyl(m, x - 7, -15, 14, 36, 1.2, GO)
    B(m, (-34, 45, 0), (34, 100, 8), MA)                       # retable
    for i in range(5):
        x = -28 + i * 14
        arch(m, x, -1, 10, 60, 2, 3, GO, 1.8)
        B(m, (x - 3, 50, -1), (x + 3, 64, 1), GL if i % 2 else GL2)
        pinnacle(m, x, 4, 90, 26, 2.6)
    B(m, (-4, 70, -2), (4, 96, 1), GO)                         # croix
    B(m, (-10, 84, -2), (10, 90, 1), GO)
    B(m, (-2, 78, -2.6), (2, 82, -2), CR)
    for s in (-1, 1):
        for k, x in enumerate((s * 38, s * 28)):
            candle(m, x, -6, 45, 16 - k * 4, 2.2)
            cyl(m, x, -6, 45, 47, 3.4, GO)
    B(m, (-44, 100, -2), (44, 106, 10), GO)
    return m


@model('deco_cat_candelabre', 'Candélabre à sept branches', 'Seven-Branch Candelabrum', (4, 0, 4, 12, 32, 12), 15)
def candelabre():
    m = new('deco_cat_candelabre', 110)
    pyramid(m, 0, 0, 0, 10, 14, 7, GO, 4)
    taper(m, 0, 0, 10, 24, 5, 2.4, GO, 3)
    for y in (22, 40, 58):
        cyl(m, 0, 0, y, y + 4, 4.2, GO)
        for k in range(3):
            cyl(m, 0, 0, y + 4 + k * 5, y + 8 + k * 5, 2.4 + (k == 1) * 1, GO)
    cyl(m, 0, 0, 14, 90, 2, GO)
    for i in range(-3, 4):                                    # sept branches en arcs
        x = i * 10
        for t in range(10):
            a = t / 9
            yy = 66 + (6 - abs(i)) * 0 + math.sin(a * math.pi / 2) * 10 * (abs(i) / 3 + .1)
            xx = x * a
            B(m, (xx - 1.3, 60 + a * (10 + abs(i) * 6), -1.3), (xx + 1.3, 62.6 + a * (10 + abs(i) * 6), 1.3), GO)
        B(m, (x - 1.3, 60 + 10 + abs(i) * 6, -1.3), (x + 1.3, 72 + abs(i) * 6, 1.3), GO)
        cyl(m, x, 0, 72 + abs(i) * 6, 74.6 + abs(i) * 6, 3, GO)
        candle(m, x, 0, 74.6 + abs(i) * 6, 9, 1.7)
    return m


@model('deco_cat_benitier', 'Bénitier', 'Holy Water Font', (4, 0, 4, 12, 24, 12), 4)
def benitier():
    m = new('deco_cat_benitier', 111)
    pyramid(m, 0, 0, 0, 6, 10, 8, ST, 2)
    taper(m, 0, 0, 6, 28, 5, 3.2, MA, 4)
    for a in range(0, 360, 45):
        r = math.radians(a)
        B(m, (math.cos(r) * 4.4 - .7, 8, math.sin(r) * 4.4 - .7), (math.cos(r) * 4.4 + .7, 26, math.sin(r) * 4.4 + .7), MA2)
    cyl(m, 0, 0, 26, 30, 9, GO)
    taper(m, 0, 0, 30, 42, 9, 14, MA, 4)
    cyl(m, 0, 0, 41, 45, 14.6, GO)
    cyl(m, 0, 0, 40, 43, 12.6, WA)
    for a in range(0, 360, 60):
        r = math.radians(a)
        Bc(m, math.cos(r) * 13.4, math.sin(r) * 13.4, 32, 40, .9, .9, GO)
    return m


@model('deco_cat_confessionnal', 'Confessionnal', 'Confessional', (1, 0, 3, 15, 32, 13))
def confessionnal():
    m = new('deco_cat_confessionnal', 112)
    B(m, (-30, 0, -10), (30, 8, 12), WD2)
    B(m, (-30, 8, -10), (30, 80, 12), WD)
    for i, (x0, x1) in enumerate(((-30, -12), (-6, 6), (12, 30))):
        B(m, (x0 + 1, 10, -11), (x1 - 1, 76, -10), WD2)
        if i != 1:
            B(m, (x0 + 4, 14, -12), (x1 - 4, 58, -11), WD)
            arch(m, (x0 + x1) / 2, -11.8, x1 - x0 - 8, 58, 1.4, 1.2, GO, 1.6)
            B(m, (x1 - 7, 32, -13), (x1 - 5.4, 38, -11.8), GO)
        else:
            B(m, (x0 + 2, 36, -12), (x1 - 2, 62, -11), IR2)                   # grille du guichet
            for k in range(6):
                B(m, (x0 + 2.6 + k * 1.7, 36, -12.4), (x0 + 3.4 + k * 1.7, 62, -11.4), GO)
    B(m, (-32, 80, -12), (32, 86, 14), GO)
    for i in range(9):                                        # frise a jours
        x = -28 + i * 7
        B(m, (x, 86, -10), (x + 3, 92 + (i % 2) * 4, 10), WD2)
    pyramid(m, 0, 1, 86, 112, 22, 1, WD2, 8)
    Bc(m, 0, 1, 112, 120, .8, .8, GO)
    for s in (-1, 1):
        B(m, (s * 30 - 2, 0, -12), (s * 30 + 2, 80, -10), GO)
    return m


@model('deco_cat_fonts_baptismaux', 'Fonts baptismaux', 'Baptismal Font', (2, 0, 2, 14, 32, 14), 5)
def fonts():
    m = new('deco_cat_fonts_baptismaux', 113)
    for i in range(3):
        cyl(m, 0, 0, i * 4, i * 4 + 4, 22 - i * 3, ST if i % 2 else MA)
    taper(m, 0, 0, 12, 36, 8, 6, MA, 4)
    for a in range(0, 360, 45):
        r = math.radians(a)
        cyl(m, math.cos(r) * 11, math.sin(r) * 11, 12, 34, 1.5, MA2)
    cyl(m, 0, 0, 36, 40, 14, GO)
    taper(m, 0, 0, 40, 56, 14, 20, MA, 4)
    cyl(m, 0, 0, 55, 59, 20.6, GO)
    cyl(m, 0, 0, 52, 56, 17.5, WA)
    for a in range(0, 360, 45):                               # quatrefeuilles de la cuve
        r = math.radians(a + 22.5)
        x, z = math.cos(r) * 17.4, math.sin(r) * 17.4
        B(m, (x - 3, 44, z - 3), (x + 3, 52, z + 3), GO, (0, -a - 22.5 + 90, 0), (x, 44, z))
    for a in range(0, 360, 90):                               # couvercle suspendu
        r = math.radians(a + 45)
        cyl(m, math.cos(r) * 13, math.sin(r) * 13, 59, 100, .9, GO)
    taper(m, 0, 0, 96, 126, 14, 1.6, WD, 6)
    cyl(m, 0, 0, 126, 134, 1.2, GO)
    return m


@model('deco_cat_gisant', 'Tombeau à gisant', 'Tomb with Effigy', (0, 0, 3, 16, 14, 13))
def gisant():
    m = new('deco_cat_gisant', 114)
    B(m, (-42, 0, -16), (42, 6, 16), ST2)
    B(m, (-40, 6, -14), (40, 26, 14), MA)
    for i in range(7):                                        # arcatures du coffre
        x = -36 + i * 12
        B(m, (x, 9, -15), (x + 9, 22, -14), MA2)
        arch(m, x + 4.5, -15.2, 9, 20, 1.2, 1.2, GO, 1.6)
        B(m, (x + 1.5, 10.6, -15.6), (x + 7.5, 20, -15), GL3)
    B(m, (-42, 26, -16), (42, 31, 16), GO)
    B(m, (-38, 31, -10), (38, 35, 10), ST)                    # dalle
    B(m, (-32, 35, -6), (14, 42, 6), MA)                      # corps du gisant
    B(m, (-32, 42, -4), (14, 47, 4), VIO)                     # linceul
    B(m, (14, 35, -5), (30, 46, 5), MA)                       # tete et coussin
    B(m, (16, 46, -4), (26, 53, 4), MA)
    B(m, (28, 35, -6), (36, 41, 6), VIO2)
    B(m, (-8, 47, -3), (6, 49, 3), MA2)                       # mains jointes
    B(m, (-30, 47, -1), (-4, 49, 1), ('metal', '#d0c8f0'))    # epee
    B(m, (-6, 47, -4), (-4, 49, 4), GO)
    B(m, (-36, 35, -5), (-32, 41, 5), MA2)                    # pieds
    for s in (-1, 1):
        for z in (-12, 12):
            B(m, (s * 40 - 3, 35, z - 3), (s * 40 + 3, 41, z + 3), GO)
            pyramid(m, s * 40, z, 41, 52, 2.8, .4, GO, 3)
    return m


@model('deco_cat_statue_saint', 'Saint dans sa niche', 'Saint in Niche', (2, 0, 3, 14, 32, 13), 0)
def statue_saint():
    m = new('deco_cat_statue_saint', 115)
    B(m, (-22, 0, -10), (22, 6, 10), ST2)
    B(m, (-20, 6, -8), (20, 12, 8), ST)
    for s in (-1, 1):
        B(m, (s * 18 - 3, 12, -8), (s * 18 + 3, 70, 8), MA)
        pinnacle(m, s * 18, 0, 70, 30, 3.6)
    arch(m, 0, -6, 40, 70, 4, 6, MA)
    B(m, (-15, 12, 4), (15, 70, 8), ('marble', '#5a3f8a'))   # fond de niche
    arch(m, 0, 3, 30, 70, 2, 4, ST2)
    cyl(m, 0, 0, 12, 18, 8, GO)                               # socle
    taper(m, 0, 0, 18, 56, 8, 5, VIO2, 5)                     # robe
    B(m, (-9, 38, -3), (9, 44, 3), GO)                        # ceinture
    B(m, (-11, 50, -4), (11, 56, 4), MA)                      # epaules
    B(m, (-14, 40, -2), (-9, 56, 2), MA); B(m, (9, 40, -2), (14, 56, 2), MA)
    B(m, (-3.4, 56, -3), (3.4, 60, 3), MA)
    cyl(m, 0, 0, 60, 70, 4, MA)                               # tete
    ring(m, 0, 70, -4, 6.5, 12, 1.6, GO, 1.6)                 # auréole
    B(m, (-7, 44, -7), (-3, 48, -4), GO)                      # livre
    B(m, (-1, 14, -13), (1, 40, -10), GO) if False else None
    return m


@model('deco_cat_gargouille', 'Gargouille monumentale', 'Monumental Gargoyle', (0, 0, 0, 16, 16, 16), 0)
def gargouille():
    """Gargouille accroupie (d'apres reference) : ailes de chauve-souris, genoux ecartes, mains griffues posees sur le socle."""
    m = new('deco_cat_gargouille', 116)
    SK, SK2, MB, CL = ('tile', 'gargouille_bleue'), ('tile', 'gargouille_sombre'), ('tile', 'gargouille_membrane'), ('tile', 'gargouille_sombre')

    def seg(p, q, r, mat, r2=None):
        p, q = [float(v) for v in p], [float(v) for v in q]
        d = [q[i] - p[i] for i in range(3)]
        L_ = math.sqrt(sum(v * v for v in d))
        if L_ < 1e-3:
            return
        a = -math.degrees(math.asin(max(-1, min(1, d[1] / L_))))
        b = math.degrees(math.atan2(d[0], d[2]))
        B(m, (p[0] - r, p[1] - r, p[2] - .3), (p[0] + r, p[1] + r, p[2] + L_ + .3), mat, (a, b, 0), tuple(p))

    def chain(pts, r0, r1, mat):
        n = len(pts) - 1
        for i in range(n):
            seg(pts[i], pts[i + 1], r0 + (r1 - r0) * (i + .5) / n, mat)

    def ell(c, r, mat, step=3.0):
        cx, cy, cz = c
        rx, ry, rz = r
        n = max(3, int(2 * ry / step))
        for k in range(n):
            y0, y1 = cy - ry + 2 * ry * k / n, cy - ry + 2 * ry * (k + 1) / n
            t = ((y0 + y1) / 2 - cy) / ry
            f = math.sqrt(max(.04, 1 - t * t))
            B(m, (cx - rx * f, y0, cz - rz * f * .72), (cx + rx * f, y1, cz + rz * f * .72), mat)
            B(m, (cx - rx * f * .72, y0, cz - rz * f), (cx + rx * f * .72, y1, cz + rz * f), mat)

    def bez(p0, p1, p2, n=6):
        return [tuple((1 - t) ** 2 * p0[i] + 2 * (1 - t) * t * p1[i] + t * t * p2[i] for i in range(3)) for t in (k / n for k in range(n + 1))]

    # socle
    Bc(m, 0, -4, 0, 8, 46, 46, ST2)
    Bc(m, 0, -4, 8, 13, 41, 41, ST)
    Bc(m, 0, -4, 13, 15, 42, 42, ST2)
    ell((0, 70, 8), (20, 16, 16), SK)                                  # bassin
    ell((0, 90, 3), (17, 18, 12), SK)                                  # ventre
    for row in range(4):                                               # abdominaux
        for s in (-1, 1):
            ell((s * 5.2, 77 + row * 7.2, -8.6), (4.6, 3.3, 2.6), SK, 1.6)
    ell((0, 116, -1), (25, 19, 16), SK)                                # poitrine
    B(m, (-1, 102, -16), (1, 128, -12), SK2)
    for s in (-1, 1):
        ell((s * 12, 118, -14), (12, 10, 5), SK)                       # pectoraux
        ell((s * 29, 124, 2), (13, 12, 12), SK)                        # epaules
        for k in range(4):                                             # cotes
            B(m, (s * 13 - 4, 94 + k * 4.4, -11 + k * .4), (s * 13 + 4, 95.4 + k * 4.4, -9 + k * .4), SK2) if False else None
        # bras : du haut de l'epaule au sol, entre les genoux
        chain(bez((s * 32, 120, -2), (s * 42, 98, -12), (s * 33, 80, -22)), 8.5, 6.6, SK)
        ell((s * 35, 100, -10), (7.5, 11, 7.5), SK)                    # biceps
        chain(bez((s * 33, 80, -22), (s * 22, 52, -32), (s * 13, 25, -38)), 6.6, 4.2, SK)
        ell((s * 13, 21, -39), (7, 5, 8), SK)                          # main
        for t, ox in enumerate((-5, 0, 5)):                            # griffes de la main
            chain([(s * 13 + ox, 19, -43), (s * 13 + ox * 1.1, 17, -49), (s * 13 + ox * 1.1, 15.4, -54)], 1.7, .6, CL)
        # cuisses ecartees, genou haut, tibia vers le pied
        chain(bez((s * 15, 66, 8), (s * 34, 74, 6), (s * 56, 72, -20)), 13.5, 9.5, SK)
        ell((s * 37, 72, -4), (15, 12, 15), SK)                        # cuisse
        ell((s * 57, 70, -22), (9.5, 10, 9.5), SK)                     # genou
        chain(bez((s * 57, 68, -24), (s * 55, 44, -30), (s * 34, 24, -24)), 9, 5.6, SK)
        ell((s * 50, 48, -24), (9, 12, 9), SK)                         # mollet
        ell((s * 32, 21, -24), (11, 6, 14), SK)                        # pied
        for t, ox in enumerate((-7, -1, 5)):
            chain([(s * 32 + ox, 19, -36), (s * 32 + ox * 1.1, 17.5, -43), (s * 32 + ox * 1.15, 15.4, -49)], 2, .7, CL)
        chain([(s * 32 + s * 10, 19, -14), (s * 32 + s * 13, 16, -8)], 2, .7, CL)   # ergot
    # tete
    ell((0, 134, -6), (11, 9, 11), SK)                                  # cou
    ell((0, 152, -14), (15, 15, 15), SK)
    ell((0, 142, -29), (9.5, 6.5, 7), SK)                               # museau
    ell((0, 134, -26), (9, 4.2, 8), SK2)                                # machoire
    B(m, (-14, 155, -29), (14, 159, -24), SK)                           # arcade
    B(m, (-8, 159, -28), (8, 163, -18), SK)
    for s in (-1, 1):
        B(m, (s * 6.4 - 2.2, 151, -28.5), (s * 6.4 + 2.2, 154, -27.4), ('flame', '#ff5030'))     # yeux
        B(m, (s * 3.2 - 1.4, 143, -36.2), (s * 3.2 + 1.4, 145.4, -35.4), SK2)                    # narines
        chain(bez((s * 12, 160, -10), (s * 24, 170, -6), (s * 22, 186, 8)), 4.2, .7, SK2)        # cornes
        B(m, (s * 14 - 1, 150, -10), (s * 14 + 8 * s + 1, 158, -4), SK) if False else None
        ell((s * 17, 152, -6), (4.5, 7, 2.6), SK)                                                # oreilles
        for t in range(3):                                                                       # crocs
            B(m, (s * (2.2 + t * 2.6) - .7, 134.6, -35 + t * 1.4), (s * (2.2 + t * 2.6) + .7, 140 - t, -33.8 + t * 1.4), ('bone', '#e8dfc4'))
    B(m, (-7, 137, -33), (7, 138.6, -22), SK2)                          # bouche
    # ailes
    sx = .86
    for s in (-1, 1):
        S, E, W = (18, 122, 14), (46, 154, 20), (64, 188, 26)
        chain([(s * S[0], S[1], S[2]), (s * E[0], E[1], E[2]), (s * W[0], W[1], W[2])], 5.5, 3.6, SK)
        ell((s * E[0], E[1], E[2]), (6, 6, 6), SK)
        ell((s * W[0], W[1], W[2]), (4.6, 4.6, 4.6), SK)
        chain([(s * W[0], W[1], W[2]), (s * (W[0] - 2), W[1] + 10, W[2] - 1), (s * (W[0] - 8), W[1] + 17, W[2] - 2)], 2.6, .6, CL)   # griffe de pouce
        tips = [(96 * sx, 150), (100 * sx, 100), (86 * sx, 56), (54, 56)]
        for k, (tx, ty) in enumerate(tips):                             # doigts de l'aile
            mid = ((W[0] + tx) / 2 + 4 - k * 2, (W[1] + ty) / 2 + 6 - k * 3)
            chain(bez((s * W[0], W[1], W[2]), (s * mid[0], mid[1], 30 - k), (s * tx, ty, 30 - k * 2), 7), 2.6 - k * .15, .9, SK2)
        poly = [(S[0], S[1] - 4), (E[0], E[1]), (W[0], W[1]), tips[0], (74, 128), tips[1], (70, 84), tips[2], (62, 72), tips[3], (40, 76), (20, 96)]

        def inside(x, y):
            c = False
            for i in range(len(poly)):
                x0, y0 = poly[i]
                x1, y1 = poly[(i + 1) % len(poly)]
                if (y0 > y) != (y1 > y) and x < (x1 - x0) * (y - y0) / (y1 - y0) + x0:
                    c = not c
            return c
        cell = 4.0
        y = 50.0
        while y < 196:
            x = 14.0
            while x < 92:
                if inside(x + cell / 2, y + cell / 2):
                    z = 14 + (x / 90) * 15
                    a, b = sorted((s * x, s * (x + cell)))
                    B(m, (a, y, z), (b, y + cell, z + 1.4), MB)
                x += cell
            y += cell
    return m


@model('deco_cat_clocheton', 'Clocheton à crochets', 'Crocketed Pinnacle', (5, 0, 5, 11, 32, 11), 0)
def clocheton():
    m = new('deco_cat_clocheton', 117)
    pyramid(m, 0, 0, 0, 12, 14, 11, ST2, 3)
    cluster(m, 0, 0, 12, 60, 4)
    for s in (-1, 1):
        for t in (-1, 1):
            cyl(m, s * 9, t * 9, 12, 52, 1.4, MA)
            pinnacle(m, s * 9, t * 9, 52, 20, 2.0)
    B(m, (-10, 56, -10), (10, 60, 10), GO)
    pinnacle(m, 0, 0, 60, 80, 7)
    return m


@model('deco_cat_fleche', 'Flèche de la cathédrale', 'Cathedral Spire', (4, 0, 4, 12, 32, 12), 6)
def fleche():
    m = new('deco_cat_fleche', 118)
    Bc(m, 0, 0, 0, 10, 20, 20, ST2)
    Bc(m, 0, 0, 10, 14, 18, 18, GO)
    for i in range(14):
        a = 14 + i * 9
        r = 17 * (1 - i / 15.5)
        Bc(m, 0, 0, a, a + 9, r, r, MA if i % 2 else ST)
        for sx, sz in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
            Bc(m, sx * r, sz * r, a, a + 9, 1.2, 1.2, GO if i % 3 == 0 else ST)
        if i % 3 == 1:
            for k in (-1, 1):
                B(m, (-3, a + 2, k * (r + .4) - .6), (3, a + 7, k * (r + .4) + .6), GL)       # lucarne
                B(m, (k * (r + .4) - .6, a + 2, -3), (k * (r + .4) + .6, a + 7, 3), GL)
    Bc(m, 0, 0, 140, 160, 1.6, 1.6, GO)
    Bc(m, 0, 0, 160, 164, 4, 4, GO)
    B(m, (-1, 164, -1), (1, 184, 1), GO)
    B(m, (-6, 176, -1), (6, 179, 1), GO)
    return m


@model('deco_cat_portail', 'Portail de la cathédrale', 'Cathedral Portal', (0, 0, 3, 16, 32, 13), 5)
def portail():
    m = new('deco_cat_portail', 119)
    B(m, (-46, 0, -10), (46, 8, 10), ST2)
    for s in (-1, 1):
        for k, (x, w) in enumerate(((s * 40, 8), (s * 30, 6), (s * 22, 5))):
            B(m, (x - w / 2, 8, -9 + k), (x + w / 2, 70 + k * 3, 9 - k), MA if k % 2 else ST)
        cluster(m, s * 14, 0, 8, 68, 2.6, MA)
    for k in range(4):                                       # archivoltes
        arch(m, 0, 0, 56 - k * 6, 70, 4.5, 18 - k * 3, MA if k % 2 else ST)
    arch(m, 0, 0, 62, 66, 2, 20, GO)
    B(m, (-22, 8, 3), (22, 68, 6), WD2)                       # vantaux
    B(m, (-1, 8, 2), (1, 68, 6.4), GO)
    for s in (-1, 1):
        for y in (18, 40, 60):
            B(m, (s * 20 - 1, y, 1.6), (s * 2, y + 3, 6.4), IR2)
    glass_arch(m, 0, 4, 50, 70, 5, 8)                         # tympan vitre
    B(m, (-3, 8, -6), (3, 92, -2), MA2) if False else None
    for s in (-1, 1):
        Bc(m, s * 36, -9, 24, 52, 4, 2.4, MA2)                # statues-colonnes
        Bc(m, s * 36, -9, 52, 60, 3, 2.4, MA)
        pyramid(m, s * 36, -9, 60, 70, 3.6, .8, GO, 3)
    B(m, (-3, 100, -8), (3, 120, 8), GO) if False else None
    top = 70 + 26
    B(m, (-3, top + 2, -9), (3, top + 20, 9), MA)
    pinnacle(m, 0, 0, top + 20, 30, 4)
    for s in (-1, 1):
        for i in range(8):                                    # crochets du gable
            B(m, (s * (6 + i * 5.2) - 1.6, top + 2 + i * 1.5, -9.5), (s * (6 + i * 5.2) + 1.6, top + 6 + i * 1.5, 9.5), GO)
    B(m, (-46, 82, -10), (46, 87, 10), GO) if False else None
    return m


@model('deco_cat_lutrin_aigle', 'Lutrin à l’aigle', 'Eagle Lectern', (4, 0, 4, 12, 32, 12), 0)
def lutrin():
    m = new('deco_cat_lutrin_aigle', 120)
    pyramid(m, 0, 0, 0, 8, 12, 8, GO, 3)
    taper(m, 0, 0, 8, 50, 3.2, 2.4, GO, 5)
    for y in (16, 30, 44):
        cyl(m, 0, 0, y, y + 4, 4.2, GO)
    cyl(m, 0, 0, 50, 56, 5, GO)
    B(m, (-4, 56, -6), (4, 76, 6), GO)                        # corps de l'aigle
    B(m, (-3, 74, -9), (3, 90, -3), GO)                       # cou
    B(m, (-3.4, 88, -14), (3.4, 96, -4), GO)                  # tete
    B(m, (-1.2, 90, -20), (1.2, 94, -13), ('gold', '#a88020'))   # bec
    for s in (-1, 1):
        B(m, (s * 4, 62, -4), (s * 24, 84, 3), GO, rot=(0, 0, -s * 8), pivot=(s * 4, 62, 0))
        for k in range(5):                                    # plumes
            B(m, (s * (6 + k * 4), 60 - k * 1.5, -3), (s * (9 + k * 4), 82 - k * 3, 2.4), ('gold', '#e8c050'))
        B(m, (s * 12 - 1, 80, 0), (s * 16 + 1, 82, 4), GO)
    B(m, (-1, 52, 4), (1, 62, 8), GO)
    B(m, (-14, 78, -6), (14, 82, 12), WD2, rot=(-24, 0, 0), pivot=(0, 78, 0))   # pupitre
    B(m, (-11, 82, -4), (11, 84, 10), VIO, rot=(-24, 0, 0), pivot=(0, 82, 0))
    return m


@model('deco_cat_encensoir', 'Encensoir géant', 'Giant Censer', (4, 0, 4, 12, 32, 12), 10)
def encensoir():
    m = new('deco_cat_encensoir', 121)
    pyramid(m, 0, 0, 0, 6, 12, 8, ST, 2)
    cyl(m, 0, 0, 6, 96, 2.4, IR2)
    for y in (24, 52, 80):
        cyl(m, 0, 0, y, y + 3, 4, GO)
    B(m, (-1.4, 94, -1.4), (22, 98, 1.4), IR2)                # potence
    B(m, (18, 78, -1), (22, 98, 1), IR2)
    for k in range(4):                                        # chaines
        B(m, (19 - (k % 2) * 3 + 1, 70 - k * 2, -.6), (20.6, 80 + 0, .6), GO) if False else None
    for dx, dz in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        for t in range(6):
            B(m, (20 + dx * 2.2 - .5, 80 - t * 2.4 - 2.4, dz * 2.2 - .5), (20 + dx * 2.2 + .5, 80 - t * 2.4, dz * 2.2 + .5), GO)
    cyl(m, 20, 0, 60, 64, 5, GO)
    taper(m, 20, 0, 48, 60, 3, 7.4, GO, 4)
    for a in range(0, 360, 60):
        r = math.radians(a)
        B(m, (20 + math.cos(r) * 5.2 - 1, 50, math.sin(r) * 5.2 - 1), (20 + math.cos(r) * 5.2 + 1, 60, math.sin(r) * 5.2 + 1), GL2)
    cyl(m, 20, 0, 44, 48, 2.4, GO)
    for i in range(8):                                        # fumee
        B(m, (20 + math.sin(i) * 3 - 2, 66 + i * 4, math.cos(i) * 3 - 2), (20 + math.sin(i) * 3 + 2, 70 + i * 4, math.cos(i) * 3 + 2), ('glowc', '#e8e0f8'))
    return m


@model('deco_cat_reliquaire', 'Reliquaire doré', 'Golden Reliquary', (2, 0, 4, 14, 24, 12), 6)
def reliquaire():
    m = new('deco_cat_reliquaire', 122)
    B(m, (-26, 0, -10), (26, 6, 10), ST2)
    B(m, (-22, 6, -8), (22, 14, 8), MA)
    for s in (-1, 1):
        B(m, (s * 20 - 1.6, 14, -7), (s * 20 + 1.6, 40, 7), GO)
    B(m, (-22, 14, -8), (22, 42, 8), GO)
    for i in range(5):                                        # reliquaire en forme de chasse
        x = -18 + i * 9
        B(m, (x, 16, -9), (x + 7, 38, -8), ('gold', '#a88020'))
        arch(m, x + 3.5, -9.2, 7, 32, 1, 1, GO, 1.4)
        B(m, (x + 1, 17, -9.6), (x + 6, 34, -9), GLASS[i % 3])
    pyramid(m, 0, 0, 42, 60, 24, 4, GO, 6)                     # toit
    for i in range(8):
        B(m, (-22 + i * 6, 56 - abs(i - 3.5) * 2, -9.2), (-20 + i * 6, 62 - abs(i - 3.5) * 2, 9.2), GO) if False else None
    B(m, (-24, 58, -2), (24, 62, 2), GO)
    for i in range(9):
        B(m, (-24 + i * 6, 62, -1), (-21 + i * 6, 66 + (i % 2) * 2, 1), GO)                # crete
    for x in (-14, 0, 14):
        B(m, (x - 2, 28, -10), (x + 2, 34, -8.6), CR if x else CR2)
    B(m, (-1, 66, -1), (1, 82, 1), GO); B(m, (-5, 74, -1), (5, 77, 1), GO)
    return m


@model('deco_cat_applique', 'Applique à cierges', 'Candle Sconce', (4, 0, 0, 12, 24, 8), 14)
def applique():
    m = new('deco_cat_applique', 123)
    B(m, (-10, 20, 8), (10, 70, 12), GO)
    B(m, (-8, 24, 12), (8, 66, 13), ('metal', '#2a2040'))
    arch(m, 0, 13.6, 14, 56, 1.2, 1.2, GO, 1.6)
    for s in (-1, 1):
        for t in range(8):
            a = t / 7
            B(m, (s * a * 20 - 1.2, 38 + math.sin(a * 1.4) * 8, 4 - a * 4), (s * a * 20 + 1.2, 41 + math.sin(a * 1.4) * 8, 8 - a * 4), GO)
        cyl(m, s * 20, -2, 46, 49, 3.4, GO)
        candle(m, s * 20, -2, 49, 12, 1.8)
    cyl(m, 0, 0, 40, 43, 3.4, GO)
    candle(m, 0, 0, 43, 14, 1.8)
    B(m, (-3, 20, 6), (3, 34, 10), GO)
    return m


@model('deco_cat_cierge', 'Cierge pascal', 'Great Candle', (4, 0, 4, 12, 28, 12), 15)
def cierge():
    m = new('deco_cat_cierge', 124)
    pyramid(m, 0, 0, 0, 8, 14, 9, GO, 3)
    taper(m, 0, 0, 8, 28, 5, 3, GO, 3)
    cyl(m, 0, 0, 28, 34, 9, GO)
    cyl(m, 0, 0, 34, 36, 10, GO)
    for a in range(0, 360, 45):
        r = math.radians(a)
        Bc(m, math.cos(r) * 8.6, math.sin(r) * 8.6, 36, 44, .9, .9, GO)
    cyl(m, 0, 0, 34, 100, 6.4, CAN)
    for a, h in ((20, 22), (110, 14), (200, 28), (290, 18), (70, 10)):   # coulures
        r = math.radians(a)
        Bc(m, math.cos(r) * 6.6, math.sin(r) * 6.6, 100 - h, 100, 1, 1.4, ('bone', '#e8dcc0'))
    B(m, (-4, 54, -6.8), (4, 62, -6), GO)                     # croix et lettres
    B(m, (-1, 50, -6.8), (1, 66, -6), GO)
    for y in (70, 78, 86):
        B(m, (-3, y, -6.8), (3, y + 4, -6), VIO)
    Bc(m, 0, 0, 100, 102, .5, .5, IR2)
    Bc(m, 0, 0, 102, 114, 2.8, 2.8, FL)
    Bc(m, 0, 0, 114, 120, 1.4, 1.4, FL)
    return m


@model('deco_cat_tapisserie', 'Tapisserie de la cathédrale', 'Cathedral Tapestry', (0, 0, 6, 16, 32, 10))
def tapisserie():
    m = new('deco_cat_tapisserie', 125)
    B(m, (-34, 110, -2), (34, 114, 2), GO)
    for s in (-1, 1):
        Bc(m, s * 36, 0, 106, 118, 3, 3, GO)
        pyramid(m, s * 36, 0, 118, 128, 2.6, .5, GO, 3)
    B(m, (-32, 24, -.8), (32, 110, .8), VIO)
    for i in range(7):                                        # pointes du bas
        x = -28 + i * 9.3
        B(m, (x, 14 + (i % 2) * 5, -.8), (x + 9, 24, .8), VIO)
    B(m, (-32, 100, -1.2), (32, 106, 1.2), GO)
    B(m, (-32, 36, -1.2), (32, 38, 1.2), GO)
    B(m, (-3, 54, -1.4), (3, 94, 1.4), GO)                    # croix centrale
    B(m, (-14, 78, -1.4), (14, 84, 1.4), GO)
    ring(m, 0, 66, 0, 12, 16, 2, GO, 1.6)
    B(m, (-5, 62, -1.8), (5, 70, 1.8), CR)
    for s in (-1, 1):
        for k in range(3):
            B(m, (s * (24 - k * 2) - 2, 46 + k * 16, -1.4), (s * (24 - k * 2) + 2, 52 + k * 16, 1.4), GO)
    for i in range(7):                                        # glands
        x = -28 + i * 9.3 + 4.5
        B(m, (x - .8, 8 + (i % 2) * 5, -.6), (x + .8, 16 + (i % 2) * 5, .6), GO)
        Bc(m, x, 0, 4 + (i % 2) * 5, 9 + (i % 2) * 5, 1.5, 1.2, GO)
    return m


@model('deco_cat_balustrade_choeur', 'Clôture du chœur', 'Choir Screen', (0, 0, 5, 16, 24, 11))
def cloture():
    m = new('deco_cat_balustrade_choeur', 126)
    B(m, (-46, 0, -6), (46, 8, 6), ST2)
    B(m, (-46, 8, -5), (46, 14, 5), ST)
    B(m, (-46, 56, -5), (46, 62, 5), MA)
    B(m, (-48, 62, -6), (48, 66, 6), GO)
    for i in range(4):
        x = -34 + i * 23
        B(m, (x - 8, 14, -3), (x + 8, 56, 3), ST)
        quatrefoil(m, x, 36, -3.2, 14, 6, MA, GLASS[i % 3])
        arch(m, x, -3.4, 16, 16, 1.4, 1.2, GO, 1.6) if False else None
    for s in range(-2, 3):
        cluster(m, s * 23 - 11.5 if s != 2 else 46, 0, 8, 62, 2.6, MA) if s < 2 else None
    for x in (-46, -23, 0, 23, 46):
        cyl(m, x, 0, 8, 66, 2.6, MA)
        pyramid(m, x, 0, 66, 84, 3.2, .4, GO, 4)
        cyl(m, x, 0, 30, 33, 3.6, GO)
    return m


@model('deco_cat_stalle', 'Stalle du chœur', 'Choir Stall', (2, 0, 2, 14, 32, 14))
def stalle():
    m = new('deco_cat_stalle', 127)
    B(m, (-26, 0, -14), (26, 6, 14), WD2)
    B(m, (-24, 6, -12), (24, 30, 12), WD)
    B(m, (-22, 30, -12), (22, 36, 8), WD2)                    # siege relevable
    B(m, (-22, 36, 4), (22, 76, 12), WD)                      # dossier
    for i in range(5):
        x = -18 + i * 9
        arch(m, x, 12.2, 8, 56, 1.2, 1.2, GO, 1.6)
        B(m, (x - 3.4, 38, 12), (x + 3.4, 60, 13), WD2)
    for s in (-1, 1):
        B(m, (s * 25 - 3, 6, -14), (s * 25 + 3, 52, 14), WD2)                 # jouées
        B(m, (s * 25 - 3.4, 52, -14.4), (s * 25 + 3.4, 56, 14.4), GO)
        Bc(m, s * 25, -11, 56, 60, 2.4, 2.4, GO)
        cluster(m, s * 22, 10, 36, 100, 2.2, WD)
        pinnacle(m, s * 22, 10, 100, 26, 2.6, GO)
    B(m, (-26, 76, 2), (26, 82, 14), GO)
    for i in range(9):
        B(m, (-24 + i * 6, 82, 5), (-21 + i * 6, 90 + (i % 2) * 6, 11), WD2)
    B(m, (-20, 24, -12.6), (20, 27, -11.6), VIO)
    return m


@model('deco_cat_trone_eveque', 'Trône de l’évêque', 'Bishop’s Throne', (2, 0, 2, 14, 32, 14), 3)
def trone_eveque():
    m = new('deco_cat_trone_eveque', 128)
    for i in range(3):
        B(m, (-30 + i * 3, i * 5, -22 + i * 4), (30 - i * 3, i * 5 + 5, 18 - i * 2), ST if i % 2 else MA)
    B(m, (-22, 15, -8), (22, 30, 14), WD)
    B(m, (-20, 30, -8), (20, 36, 12), VIO)
    B(m, (-22, 30, 8), (22, 120, 14), WD)
    for s in (-1, 1):
        B(m, (s * 22 - 3, 15, -12), (s * 22 + 3, 54, 14), WD2)
        B(m, (s * 22 - 3.4, 54, -12.4), (s * 22 + 3.4, 58, 14.4), GO)
        cluster(m, s * 24, 10, 58, 128, 2.6, WD)
    arch(m, 0, 10, 44, 100, 3, 5, GO, 2)
    B(m, (-18, 38, 7.4), (18, 100, 8), VIO)
    ring(m, 0, 84, 7, 8, 14, 1.8, GO, 1.4)
    B(m, (-4, 78, 6.6), (4, 90, 7.4), CR)
    pinnacle(m, 0, 10, 120, 50, 5, GO)
    for s in (-1, 1):
        pinnacle(m, s * 24, 10, 128, 36, 3, GO)
    return m


@model('deco_cat_croix', 'Croix monumentale', 'Monumental Cross', (4, 0, 4, 12, 32, 12), 6)
def croix():
    m = new('deco_cat_croix', 129)
    for i in range(3):
        Bc(m, 0, 0, i * 5, i * 5 + 5, 22 - i * 5, 22 - i * 5, ST if i % 2 else ST2)
    B(m, (-6, 15, -6), (6, 130, 6), GO)
    B(m, (-34, 90, -5), (34, 108, 5), GO)
    for sx in (-1, 1):                                        # extremites trilobees
        for dy in (-1, 1):
            B(m, (sx * 34 - 4, 99 + dy * 12 - 4, -5), (sx * 34 + 4, 99 + dy * 12 + 4, 5), GO) if False else None
        B(m, (sx * 34 - 5, 88, -6), (sx * 34 + 5, 110, 6), GO)
        for dy in (-8, 8):
            B(m, (sx * 38 - 3, 99 + dy - 3, -4), (sx * 38 + 3, 99 + dy + 3, 4), GO) if False else None
    B(m, (-6, 130, -6), (6, 144, 6), GO)
    pyramid(m, 0, 0, 144, 160, 6, 1, GO, 4)
    for y in (40, 70):
        cyl(m, 0, 0, y, y + 3, 9, GO)
    B(m, (-9, 94, -6.6), (9, 104, 6.6), CR)
    B(m, (-3, 98, -7.4), (3, 102, 7.4), CR2)
    for a in range(0, 360, 45):                               # rayons
        r = math.radians(a)
        spoke(m, 0, 99, 0, 16, 28, a, 3, 1.2, GL2) if a % 90 else None
    return m


@model('deco_cat_colonne_torsadee', 'Colonne torsadée', 'Twisted Column', (4, 0, 4, 12, 32, 12))
def torsadee():
    m = new('deco_cat_colonne_torsadee', 130)
    Bc(m, 0, 0, 0, 5, 12, 12, ST2)
    Bc(m, 0, 0, 5, 9, 10, 10, ST)
    cyl(m, 0, 0, 9, 12, 8, GO)
    for i in range(30):                                       # torsade : barres decalees en spirale
        y = 12 + i * 3.8
        a = i * 0.62
        for k in range(3):
            aa = a + k * 2.094
            x, z = math.cos(aa) * 4, math.sin(aa) * 4
            B(m, (x - 2.2, y, z - 2.2), (x + 2.2, y + 4.4, z + 2.2), MA if k else GO)
    cyl(m, 0, 0, 12, 126, 4, MA2)
    cyl(m, 0, 0, 124, 128, 8, GO)
    taper(m, 0, 0, 128, 140, 8, 12, MA, 3)
    Bc(m, 0, 0, 140, 146, 13, 13, ST)
    return m


@model('deco_cat_puits_lumiere', 'Puits de lumière', 'Light Well', (3, 0, 3, 13, 32, 13), 15)
def puits_lumiere():
    m = new('deco_cat_puits_lumiere', 131)
    cyl(m, 0, 0, 0, 6, 22, ST2)
    cyl(m, 0, 0, 6, 10, 19, MA)
    for a in range(0, 360, 45):
        r = math.radians(a)
        x, z = math.cos(r) * 17, math.sin(r) * 17
        cyl(m, x, z, 10, 74, 2, MA)
        cyl(m, x, z, 74, 77, 3, GO)
        arch(m, 0, 0, 12, 0, 0, 0, GO) if False else None
    for a in range(0, 360, 90):
        r = math.radians(a + 45)
        B(m, (math.cos(r) * 8 - 1, 77, math.sin(r) * 8 - 1), (math.cos(r) * 17 + 1, 81, math.sin(r) * 17 + 1), GO) if False else None
    ring_y = 80
    cyl(m, 0, 0, 77, 81, 20, GO)
    taper(m, 0, 0, 81, 96, 19, 6, MA, 5)
    cyl(m, 0, 0, 10, 14, 7, GO)
    cyl(m, 0, 0, 14, 60, 5, CR)
    m.box('root', (-6, 14, -6), (6, 60, 6), CR2, (0, 45, 0), (0, 14, 0))
    cyl(m, 0, 0, 60, 66, 3, GL)
    cyl(m, 0, 0, 96, 130, 1.8, GL)
    cyl(m, 0, 0, 130, 134, 3, GL2)
    return m


@model('deco_cat_reliquaire_cristal', 'Fiole sainte sous dôme', 'Holy Vial under Dome', (4, 0, 4, 12, 24, 12), 12)
def fiole():
    m = new('deco_cat_reliquaire_cristal', 132)
    pyramid(m, 0, 0, 0, 10, 14, 10, MA, 3)
    taper(m, 0, 0, 10, 20, 8, 12, GO, 3)
    cyl(m, 0, 0, 20, 23, 13, GO)
    cyl(m, 0, 0, 23, 29, 6, WD2)
    cyl(m, 0, 0, 29, 52, 4, CR)
    cyl(m, 0, 0, 52, 56, 2.4, GO)
    for a in range(0, 360, 60):
        r = math.radians(a)
        cyl(m, math.cos(r) * 11, math.sin(r) * 11, 23, 66, .9, GO)
    taper(m, 0, 0, 56, 62, 2.4, 1, CR2, 2)
    taper(m, 0, 0, 62, 76, 12, 3, GL, 4)
    ring(m, 0, 66, 0, 11, 16, 1.4, GO, 1.4) if False else None
    cyl(m, 0, 0, 76, 80, 3, GO)
    B(m, (-1, 80, -1), (1, 92, 1), GO)
    B(m, (-5, 86, -1), (5, 89, 1), GO)
    return m


@model('deco_cat_porche', 'Porche de la cathédrale', 'Cathedral Porch', (0, 0, 0, 16, 32, 16))
def porche():
    m = new('deco_cat_porche', 133)
    B(m, (-44, 0, -28), (44, 6, 20), ST2)
    for s in (-1, 1):
        for z in (-22, 14):
            cluster(m, s * 38, z, 6, 82, 4.2)
    for z in (-22, 14):
        arch(m, 0, z, 64, 82, 4.5, 8, MA)
        arch(m, 0, z, 56, 82, 2, 5, ST)
    for s in (-1, 1):
        arch(m, s * 38, -4, 28, 82, 3.5, 6, ST) if False else None
        B(m, (s * 38 - 2, 82, -22), (s * 38 + 2, 130, 14), ST) if False else None
    for i in range(10):                                       # toit a deux pans en dalles
        y = 118 + i * 4
        w = 44 - i * 4.2
        B(m, (-w, y, -30), (w, y + 4, 24), ST2 if i % 2 else ST)
    B(m, (-4, 156, -28), (4, 160, 22), GO)
    B(m, (-46, 112, -30), (46, 118, 24), GO)
    for i in range(10):
        B(m, (-42 + i * 9, 118, -30), (-38 + i * 9, 124, -29), GO)
    B(m, (-3, 128, -29), (3, 150, -27), GL2)
    return m


@model('deco_cat_arcade_cloitre', 'Arcade du cloître', 'Cloister Arcade', (0, 0, 4, 16, 28, 12))
def cloitre():
    m = new('deco_cat_arcade_cloitre', 134)
    B(m, (-46, 0, -6), (46, 8, 6), ST2)
    for i in range(4):
        x = -42 + i * 28
        cyl(m, x, -2, 8, 48, 1.8, MA)
        cyl(m, x, 2, 8, 48, 1.8, MA)
        cyl(m, x, -2, 8, 11, 2.6, GO); cyl(m, x, 2, 8, 11, 2.6, GO)
        B(m, (x - 3, 46, -4.4), (x + 3, 52, 4.4), GO)
    for i in range(3):
        cx = -28 + i * 28
        arch(m, cx, 0, 24, 52, 3, 8, MA)
        arch(m, cx, 0, 18, 52, 1.6, 4.4, ST)
        glass_arch(m, cx, 0, 20, 52, 4, 3) if False else None
        quatrefoil(m, cx, 52 + 18, 0, 8, 3, ST, GLASS[i % 3])
    B(m, (-46, 82, -6.4), (46, 90, 6.4), GO)
    B(m, (-46, 90, -6), (46, 96, 6), MA)
    for i in range(12):
        B(m, (-44 + i * 8, 96, -5), (-41 + i * 8, 102 + (i % 2) * 3, 5), ST)
    return m


@model('deco_cat_escalier_helicoidal', 'Escalier à vis', 'Spiral Staircase', (2, 0, 2, 14, 32, 14))
def vis():
    m = new('deco_cat_escalier_helicoidal', 135)
    cyl(m, 0, 0, 0, 120, 4, MA)
    cyl(m, 0, 0, 0, 4, 8, GO)
    for i in range(20):                                       # marches en eventail
        a = i * 22
        y = 4 + i * 5.6
        m.box('root', (0, y, -4), (28, y + 4, 4), WD if i % 2 else WD2, (0, a, 0), (0, y, 0))
        r = math.radians(a)
        x, z = math.cos(r) * 26, -math.sin(r) * 26
        Bc(m, x, z, y + 4, y + 18, .9, .9, GO)                 # barreau
        B(m, (x - 1.4, y + 17, z - 1.4), (x + 1.4, y + 20, z + 1.4), GO)
    cyl(m, 0, 0, 120, 126, 7, GO)
    pyramid(m, 0, 0, 126, 144, 12, 1.4, ST, 6)
    return m


@model('deco_cat_horloge', 'Horloge astronomique', 'Astronomical Clock', (3, 0, 3, 13, 32, 13), 6)
def horloge():
    m = new('deco_cat_horloge', 136)
    pyramid(m, 0, 0, 0, 14, 22, 18, ST2, 3)
    B(m, (-20, 14, -8), (20, 130, 8), ST)
    for s in (-1, 1):
        cluster(m, s * 22, 0, 14, 120, 3.6, MA)
        pinnacle(m, s * 22, 0, 120, 30, 3)
    cy = 76
    ring(m, 0, cy, -8, 17, 28, 3, GO, 3)
    ring(m, 0, cy, -8, 13, 24, 2, MA, 2.4)
    cyl_front = B(m, (-12, cy - 12, -9), (12, cy + 12, -7.6), ('marble', '#2a1a50'))
    for a in range(0, 360, 30):
        r = math.radians(a)
        B(m, (math.cos(r) * 12 - 1.2, cy + math.sin(r) * 12 - 1.2, -10), (math.cos(r) * 12 + 1.2, cy + math.sin(r) * 12 + 1.2, -8.8), GO)
    B(m, (-1, cy, -10.4), (1, cy + 10, -9), GO)
    spoke(m, 0, cy, -9.6, 0, 8, 30, 1.6, 1, GO)
    B(m, (-2, cy - 2, -10.6), (2, cy + 2, -9.2), CR)
    arch(m, 0, -6, 34, 98, 3, 6, MA)                           # dais
    B(m, (-5, 112, -8), (5, 122, -6), GL2)                     # soleil
    ring(m, 0, 46, -8, 8, 16, 2, GO, 3)
    B(m, (-3, 43, -9), (3, 49, -8), GL3)                       # lune
    B(m, (-14, 20, -9), (14, 36, -8), ('marble', '#2a1a50'))
    for k in range(5):
        B(m, (-12 + k * 5.4, 22, -9.4), (-9 + k * 5.4, 34, -8.6), GL2 if k % 2 else GL)
    B(m, (-22, 130, -9), (22, 136, 9), GO)
    pinnacle(m, 0, 0, 136, 40, 6)
    return m


@model('deco_cat_candelabre_geant', 'Candélabre monumental', 'Monumental Candelabrum', (4, 0, 4, 12, 32, 12), 15)
def candelabre_geant():
    m = new('deco_cat_candelabre_geant', 137)
    for i in range(3):
        cyl(m, 0, 0, i * 3, i * 3 + 3, 14 - i * 2, GO if i % 2 else ST2)
    taper(m, 0, 0, 9, 30, 6, 3, GO, 4)
    cyl(m, 0, 0, 30, 130, 2.4, GO)
    for y in (36, 60, 84, 108):
        cyl(m, 0, 0, y, y + 5, 5, GO)
        for a in range(0, 360, 90):
            r = math.radians(a)
            B(m, (math.cos(r) * 5 - 1, y + 5, math.sin(r) * 5 - 1), (math.cos(r) * 7 + 1, y + 10, math.sin(r) * 7 + 1), GO)
    for lvl, (y, R, n) in enumerate(((100, 22, 6), (124, 12, 4))):
        for i in range(n):
            a = 2 * math.pi * i / n
            for t in range(8):
                u = t / 7
                x, z = math.cos(a) * R * u, math.sin(a) * R * u
                B(m, (x - 1.2, y + math.sin(u * 1.5) * 10 - 1.2, z - 1.2), (x + 1.2, y + math.sin(u * 1.5) * 10 + 1.2, z + 1.2), GO)
            x, z = math.cos(a) * R, math.sin(a) * R
            cyl(m, x, z, y + 10, y + 13, 3, GO)
            candle(m, x, z, y + 13, 10, 1.8)
    cyl(m, 0, 0, 130, 133, 3.4, GO)
    candle(m, 0, 0, 133, 14, 2)
    return m


@model('deco_cat_ange', 'Ange de pierre', 'Stone Angel', (3, 0, 3, 13, 32, 13), 0)
def ange():
    m = new('deco_cat_ange', 138)
    pyramid(m, 0, 0, 0, 12, 16, 12, ST2, 3)
    cyl(m, 0, 0, 12, 18, 10, MA2)
    taper(m, 0, 0, 18, 70, 9, 5.4, MA, 7)                      # robe evasée
    for k in range(6):                                         # plis
        a = k * 60
        r = math.radians(a)
        B(m, (math.cos(r) * 7 - .8, 18, math.sin(r) * 7 - .8), (math.cos(r) * 7 + .8, 56, math.sin(r) * 7 + .8), MA2)
    B(m, (-6, 44, -3.4), (6, 48, 3.4), GO)
    B(m, (-8, 62, -4), (8, 72, 4), MA)
    B(m, (-12, 52, -2.4), (-7, 70, 2.4), MA); B(m, (7, 52, -2.4), (12, 70, 2.4), MA)
    B(m, (-12, 46, -5), (-7, 54, -3), MA2); B(m, (7, 46, -5), (12, 54, -3), MA2)    # mains jointes
    B(m, (-3.4, 72, -3), (3.4, 76, 3), MA)
    cyl(m, 0, 0, 76, 88, 4.4, MA)
    B(m, (-4.6, 82, -5), (4.6, 88, 4), MA2)                    # chevelure
    ring(m, 0, 84, -5, 7, 14, 1.6, GO, 1.4)
    for s in (-1, 1):                                          # ailes en plumes
        for k in range(7):
            B(m, (s * (6 + k * 3.6), 70 - k * 4.4, 3), (s * (10 + k * 3.6), 112 - k * 8, 6), MA if k % 2 else MA2,
              rot=(0, 0, -s * (10 + k * 3)), pivot=(s * 6, 70, 4.5))
    return m


@model('deco_cat_triptyque', 'Triptyque de vitraux', 'Stained Glass Triptych', (0, 0, 5, 16, 32, 11), 7)
def triptyque():
    m = new('deco_cat_triptyque', 139)
    B(m, (-54, 0, -6), (54, 6, 6), ST2)
    for i, (cx, span, st) in enumerate(((-36, 22, 52), (0, 30, 74), (36, 22, 52))):
        window(m, cx, 0, span, st, 8, y0=6, t=3)
    for x in (-54, -18, 18, 54):
        cluster(m, x, 0, 6, 122 if x in (-18, 18) else 106, 3.2)
        pinnacle(m, x, 0, 122 if x in (-18, 18) else 106, 26, 2.6)
    B(m, (-58, 100, -6.4), (58, 106, 6.4), GO) if False else None
    return m


@model('deco_cat_baldaquin', 'Baldaquin de l’autel', 'Altar Ciborium', (0, 0, 0, 16, 32, 16), 8)
def baldaquin():
    m = new('deco_cat_baldaquin', 140)
    B(m, (-40, 0, -40), (40, 6, 40), ST2)
    B(m, (-36, 6, -36), (36, 10, 36), ST)
    for sx in (-1, 1):
        for sz in (-1, 1):
            cluster(m, sx * 30, sz * 30, 10, 100, 4.4)
            pinnacle(m, sx * 30, sz * 30, 100, 30, 3.6)
    for s in (-1, 1):
        for z in (-30, 30):
            arch(m, 0, z, 44, 100, 4, 7, MA)
            arch(m, 0, z, 36, 100, 1.8, 4, ST)
        for x in (-30, 30):
            m_arch = arch(m, 0, 0, 44, 100, 4, 7, MA) if False else None
    for sx in (-30, 30):                                         # arcs lateraux (tournes)
        for i in range(12):
            a = math.pi * i / 11
            z = sx * 0 + (-22 + 44 * i / 11)
            h = math.sin(a) * 26 + 100
            B(m, (sx - 3.2, h - 2.4, z - 2.4), (sx + 3.2, h + 2.4, z + 2.4), MA if i % 2 else ST)
    B(m, (-30, 126, -30), (30, 132, 30), GO)
    for i in range(10):
        pyramid(m, 0, 0, 132 + i * 0, 132, 0, 0, GO, 1) if False else None
    pyramid(m, 0, 0, 132, 170, 28, 2, ST, 10)
    for a in range(0, 360, 90):
        r = math.radians(a)
        B(m, (math.cos(r) * 14 - 1.4, 138, math.sin(r) * 14 - 1.4), (math.cos(r) * 14 + 1.4, 150, math.sin(r) * 14 + 1.4), GO)
    cyl(m, 0, 0, 170, 180, 1.4, GO)
    B(m, (-1, 176, -1), (1, 194, 1), GO); B(m, (-6, 188, -1), (6, 191, 1), GO)
    for s in (-1, 1):
        B(m, (-14, 14, -14), (14, 34, 14), MA) if False else None
    B(m, (-18, 10, -14), (18, 34, 14), MA)
    B(m, (-20, 34, -16), (20, 38, 16), GO)
    candle(m, -14, 0, 38, 14, 2.2); candle(m, 14, 0, 38, 14, 2.2)
    return m
