#!/usr/bin/env python3
"""Pieces communes a toutes les montures : tapis a la couleur de la rarete, selle, etriers, nacelle multi-places, embleme.
Regle de hauteur (verifiee sur le code du jeu) : dessous des cuisses du cavalier a 16 x siege + 1 unites. Le dessus de la selle est place a cette hauteur, arrondi a 0,5.
Sieges du jeu {x, z} en blocs : x modele = 16 x, z modele = -16 z.
"""
from aeloria3d import Project
from materiaux import stamp

CUIR = ('cuir', 'cuir')
FER = ('metal', 'fer')
OR = ('metal', 'or')
PAL = {'k': '#120b08', 'd': '#2a1710', 'a': '#e8a324', 'b': '#ffdf6e', 'w': '#fff6d8', 'g': '#f6d566', 'o': '#ad781d', 's': '#3b2a20', 'n': '#1a0e0c',
       'v': '#54297e', 'V': '#9354c8', 'l': '#f6d566', 't': '#fff3b6', 'i': '#a0abb9', 'j': '#2c303b', 'B': '#58c8f0', 'C': '#d8f6ff', 'R': '#e0342a', 'Y': '#ffe16a', 'E': '#3ddbcf', 'P': '#c98cff', 'W': '#ffffff'}
EMBLEME = ['....oo....',
           '...ovvo...',
           '..ovVVvo..',
           '.ovVllVvo.',
           'ovVllllVvo',
           'ovllVVllvo',
           'ovlVvvVlvo',
           '.ovvvvvvo.',
           '..ovVVvo..',
           '...ovvo...',
           '....oo....']
MEDAILLON = ['...oo...',
             '..ovvo..',
             '.ovVVvo.',
             'ovVllVvo',
             'ovlVVlvo',
             'ovlvvlvo',
             '.ovVVvo.',
             '..ovvo..',
             '...oo...']
CR = dict(hide={'north', 'south', 'up', 'down'}, no_shadow=True)      # plan fin decoupe, vu de cote
CF = dict(hide={'east', 'west', 'up', 'down'}, no_shadow=True)        # plan fin decoupe, vu de face


def losange(w, h, puissance=1.0):
    """Masque d'un fer de lance vu de dessus : pointe vers l'arriere (derniere ligne), w colonnes, h lignes."""
    rows = []
    for y in range(h):
        t = (y + .5) / h
        large = (t / .35) if t < .35 else ((1 - t) / .65)
        large = max(0.0, min(1.0, large)) ** puissance
        rows.append(''.join('#' if abs(2 * (x + .5) / w - 1) <= large else '.' for x in range(w)))
    return rows


def feston(n, pas=6):
    """Masque d'un bord festonne de n texels de large : plein en haut, demi-cercles en bas."""
    def r(f):
        return ''.join('#' if f(x % pas) else '.' for x in range(n))
    return [r(lambda c: True), r(lambda c: True), r(lambda c: c != 0), r(lambda c: 1 < c < pas - 1)]


def demi(v):
    return round(v * 2) / 2


def selle_y(siege):
    return demi(16 * siege + 1 - .001)


def tapis(p, rarete, dos, demi_l, av, ar, bas, parent='body', bone='selle', emblem_z=None, franges=True, pivot=None, flanc=None):
    """Tapis de selle : dessus + deux flancs a liseret d'or, embleme sur les flancs."""
    if bone not in p.bones:
        p.bone(bone, pivot or (0, dos, (av + ar) / 2), parent)
    t = ('tissu', 'rar_' + rarete)
    p.cube(bone, 'tapis_dessus', (-demi_l - .5, dos, av), (demi_l + .5, dos + .5, ar), t, bord=2)
    if bas is not None:
        f0, f1 = flanc or (av, ar)
        p.sym(bone, 'tapis_flanc', (demi_l, bas, f0), (demi_l + .5, dos, f1), t, bord=2, franges=franges)
        if emblem_z is not None:
            p.decal('tapis_flanc_g', 'east', stamp(EMBLEME, PAL, emblem_z, dos - 1.5))
    return bone


def selle(p, seat, dos, demi_l, ventre, rarete, av=-6.0, ar=7.5, z0=0.0, bas=None, parent='body', strap=True, etriers=True, emblem=True, bone='selle'):
    """Selle complete d'une monture a un siege : tapis, quartier, siege, pommeau et troussequin a liseret d'or, sangle, etriers. seat = dessus du siege."""
    if bas is None:
        bas = max(ventre + 1.5, dos - 8.5)
    em = (z0 + ar - 1) if emblem else None
    tapis(p, rarete, dos, demi_l, z0 + av, z0 + ar, bas, parent, bone, em)
    y1 = dos + .5
    y2 = y1 + .5
    if seat - y2 < .5:
        y2 = seat - .5
    p.cube(bone, 'quartier', (-min(demi_l, 5.5), y1, z0 - 4.5), (min(demi_l, 5.5), y2, z0 + 4), CUIR, ton=-.4, couture=False)
    p.cube(bone, 'siege', (-3, y2, z0 - 4), (3, seat, z0 + 2), CUIR, ton=.6)
    P = dict(rot=(-10, 0, 0), pivot=(0, y2, z0 - 4.75))
    p.cube(bone, 'pommeau', (-1, y2, z0 - 5.5), (1, seat + 2, z0 - 4), CUIR, ton=.2, **P)
    p.cube(bone, 'pommeau_or', (-1.5, seat + 2, z0 - 6), (1.5, seat + 2.5, z0 - 3.5), OR, **P)
    T = dict(rot=(12, 0, 0), pivot=(0, y2, z0 + 2.25))
    p.cube(bone, 'troussequin', (-3.5, y2, z0 + 1.5), (3.5, seat + 2.5, z0 + 3), CUIR, **T)
    p.cube(bone, 'troussequin_or', (-4, seat + 2.5, z0 + 1), (4, seat + 3, z0 + 3.5), OR, **T)
    if strap:
        p.cube(bone, 'sangle', (-demi_l - 1, ventre - .5, z0 - 3), (demi_l + 1, ventre, z0 - 1), CUIR, ton=-.5, couture=False)
        p.sym(bone, 'sangle_flanc', (demi_l + .5, ventre, z0 - 3), (demi_l + 1, dos + .5, z0 - 1), CUIR, ton=-.2, couture=False)
    if etriers:
        xe = demi_l + 1
        p.bone('etrier_g', (xe + .25, dos + .5, z0 - 2), bone)
        p.bone('etrier_d', (-xe - .25, dos + .5, z0 - 2), bone)
        ybas = max(ventre - 1.5, 2.0)
        p.sym('etrier_g|etrier_d', 'etriviere', (xe, ybas + 3, z0 - 2.5), (xe + .5, dos + .5, z0 - 1.5), CUIR, ton=-.3, couture=False)
        p.sym('etrier_g|etrier_d', 'etrier_fer', (xe, ybas, z0 - 3.5), (xe + .5, ybas + 3, z0 - .5), ('plan', 'fer'), base=3.4, masque=['.####.', '##..##', '##..##', '##..##', '######'], **CR)
    return bone


def nacelle(p, sieges, seat, dos, demi_l, av, ar, rarete, parent='body', bas=None, ventre=None, dais=None, bois=('bois', 'bois'), emblem=True, tapis_l=None, flanc=None, tapis_z=None, sangles=None):
    """Nacelle multi-places : plancher, rambardes basses (les cuisses du cavalier passent au-dessus), coussins sous chaque siege, rive d'or, dais a 4 montants en option.
    sieges = [(x, z)] en unites modele ; seat = dessus des coussins ; dos = dessus du dos de la bete."""
    bone = 'nacelle'
    if seat - dos < 3:
        raise ValueError(f'nacelle : il faut au moins 3 unites entre le dos ({dos}) et le dessus des coussins ({seat})')
    p.bone(bone, (0, dos, (av + ar) / 2), parent)
    t = ('tissu', 'rar_' + rarete)
    tl = demi_l if tapis_l is None else tapis_l
    # tapis sous la nacelle
    t0, t1 = tapis_z or (av - 1, ar + 1)
    p.cube(bone, 'tapis_dessus', (-tl - .5, dos, t0), (tl + .5, dos + .5, t1), t, bord=2)
    f0, f1 = flanc or (t0, t1)
    if bas is not None:
        p.sym(bone, 'tapis_flanc', (tl, bas, f0), (tl + .5, dos, f1), t, bord=2, franges=True)
    y0 = dos + .5
    yp = seat - 1.5                                                       # plancher
    p.cube(bone, 'plancher', (-demi_l, y0, av), (demi_l, yp, ar), bois, sens='z', planche=6, ton=-.3)
    rail = seat + 1
    p.sym(bone, 'rambarde', (demi_l - 1, yp, av), (demi_l, rail, ar), bois, sens='z', planche=4)
    p.cube(bone, 'rambarde_av', (-demi_l + 1, yp, av), (demi_l - 1, rail - 1, av + 1), bois, sens='x', planche=4)
    p.cube(bone, 'rambarde_ar', (-demi_l + 1, yp, ar - 1), (demi_l - 1, rail + 1, ar), bois, sens='x', planche=4)
    p.sym(bone, 'rive', (demi_l - 1.5, rail, av - .5), (demi_l + .5, rail + .5, ar + .5), OR)
    p.cube(bone, 'rive_av', (-demi_l + 1.5, rail - 1, av - .5), (demi_l - 1.5, rail - .5, av + 1.5), OR)
    p.cube(bone, 'rive_ar', (-demi_l + 1.5, rail + 1, ar - 1.5), (demi_l - 1.5, rail + 1.5, ar + .5), OR)
    for i, (x, z) in enumerate(sieges):
        p.cube(bone, f'coussin{i}', (x - 2.5, yp, z - 3.5), (x + 2.5, seat, z + 1.5), t, bord=1, motif=False, ton=.4)
        p.cube(bone, f'dossier{i}', (x - 2.5, yp, z + 1.5), (x + 2.5, seat + 3, z + 2.5), CUIR, ton=.2, couture=False)
    if bas is not None and emblem:
        p.decal('tapis_flanc_g', 'east', stamp(EMBLEME, PAL, (f0 + f1) / 2 + 2.5, dos - 1.5))
    if ventre is not None:
        z1, z2 = (sangles[0], sangles[0] + 2) if sangles else (av + 2, av + 4)
        p.cube(bone, 'sangle', (-tl - 1, ventre - .5, z1), (tl + 1, ventre, z2), CUIR, ton=-.5, couture=False)
        p.sym(bone, 'sangle_flanc', (tl + .5, ventre, z1), (tl + 1, dos + .5, z2), CUIR, ton=-.2, couture=False)
        z3, z4 = (sangles[1], sangles[1] + 2) if sangles else (ar - 4, ar - 2)
        p.cube(bone, 'sangle2', (-tl - 1, ventre - .5, z3), (tl + 1, ventre, z4), CUIR, ton=-.5, couture=False)
        p.sym(bone, 'sangle2_flanc', (tl + .5, ventre, z3), (tl + 1, dos + .5, z4), CUIR, ton=-.2, couture=False)
    if dais:
        h, cloth = dais                                                   # hauteur du toit, couleur (rampe)
        for t2, zz in (('av', av + .5), ('ar', ar - 1.5)):
            p.sym(bone, f'montant_{t2}', (demi_l - 1, rail, zz), (demi_l, h, zz + 1), OR)
        c = ('tissu', cloth)
        p.cube(bone, 'dais', (-demi_l - 1.5, h, av - 1.5), (demi_l + 1.5, h + 1, ar + 1.5), c, bord=2, motif=True, base=2.2)
        p.cube(bone, 'dais_haut', (-demi_l + 1, h + 1, av + 1), (demi_l - 1, h + 2, ar - 1), c, bord=1, motif=True, base=2)
        # lambrequin festonne tout autour du toit
        lg = int((ar - av + 3) * p.density)
        lf = int((2 * demi_l + 2) * p.density)
        p.sym(bone, 'lambrequin', (demi_l + 1, h - 2, av - 1.5), (demi_l + 1.5, h, ar + 1.5), ('plan', cloth), base=2.8, masque=feston(lg), **CR)
        p.cube(bone, 'lambrequin_av', (-demi_l - 1, h - 2, av - 1.5), (demi_l + 1, h, av - 1), ('plan', cloth), base=2.8, masque=feston(lf), **CF)
        p.cube(bone, 'lambrequin_ar', (-demi_l - 1, h - 2, ar + 1), (demi_l + 1, h, ar + 1.5), ('plan', cloth), base=2.8, masque=feston(lf), **CF)
        zc = demi((av + ar) / 2)
        p.cube(bone, 'dais_or', (-1.5, h + 2, zc - 1.5), (1.5, h + 3, zc + 1.5), OR)
    return bone


def deux_faces(p, cube, rows, z_e, y, largeur=None):
    """Meme decalque sur les deux flancs d'un cube, a la meme position : z_e est le bord arriere du decalque (colonne 0 cote +x), l'autre flanc est le miroir exact."""
    d = p.density
    w = (largeur if largeur is not None else len(rows[0]) / d)
    p.decal(cube, 'east', stamp(rows, PAL, z_e, y))
    p.decal(cube, 'west', stamp(rows, PAL, z_e - w, y, flip_on=('west',)))


def oeil(p, cube, z_e, z_o=None, y=0, couleur='a', clair='b'):
    """Oeil peint a la meme place sur les deux flancs d'un cube de crane. z_e : bord arriere de l'oeil ; il s'etend de 2 unites vers l'avant. z_o n'est plus utilise."""
    deux_faces(p, cube, ['dddd', f'd{clair}{couleur}k', '.dkk'], z_e, y)


def medaillon(p, nom, bone, z, y0, cote=2):
    p.cube(bone, nom, (-cote, y0, z - .5), (cote, y0 + 4.5, z), OR)
    p.decal(nom, 'north', stamp(MEDAILLON, PAL, cote, y0 + 4.5))


def selle_longue(p, sieges, seat, dos, demi_l, ventre, rarete, av, ar, bas=None, parent='body', emblem=True, flanc=None, sangles=None, etrier_z=None):
    """Selle a plusieurs places en file : long tapis, longue selle, un siege et un troussequin par place, pommeau devant, deux sangles, etriers a la premiere place."""
    if bas is None:
        bas = max(ventre + 1.5, dos - 8.5)
    f0, f1 = flanc or (av, ar)
    em = ((f0 + f1) / 2 + 2.5) if emblem else None
    tapis(p, rarete, dos, demi_l, av, ar, bas, parent, 'selle', em, flanc=flanc)
    y1 = dos + .5
    y2 = y1 + .5
    p.cube('selle', 'quartier', (-min(demi_l, 5), y1, av + 1.5), (min(demi_l, 5), y2, ar - 2), CUIR, ton=-.4, couture=False)
    for i, (x, z) in enumerate(sieges):
        p.cube('selle', f'siege{i}', (x - 3, y2, z - 4), (x + 3, seat, z + 2), CUIR, ton=.6)
        T = dict(rot=(12, 0, 0), pivot=(x, y2, z + 2.25))
        p.cube('selle', f'troussequin{i}', (x - 3.5, y2, z + 1.5), (x + 3.5, seat + 2.5, z + 3), CUIR, **T)
        p.cube('selle', f'troussequin_or{i}', (x - 4, seat + 2.5, z + 1), (x + 4, seat + 3, z + 3.5), OR, **T)
    x0, z0 = sieges[0]
    P = dict(rot=(-10, 0, 0), pivot=(x0, y2, z0 - 4.75))
    p.cube('selle', 'pommeau', (x0 - 1, y2, z0 - 5.5), (x0 + 1, seat + 2, z0 - 4), CUIR, ton=.2, **P)
    p.cube('selle', 'pommeau_or', (x0 - 1.5, seat + 2, z0 - 6), (x0 + 1.5, seat + 2.5, z0 - 3.5), OR, **P)
    for i, zz in enumerate(sangles or (av + 2, ar - 4)):
        p.cube('selle', f'sangle{i}', (-demi_l - 1, ventre - .5, zz), (demi_l + 1, ventre, zz + 2), CUIR, ton=-.5, couture=False)
        p.sym('selle', f'sangle_flanc{i}', (demi_l + .5, ventre, zz), (demi_l + 1, dos + .5, zz + 2), CUIR, ton=-.2, couture=False)
    xe = demi_l + 1
    ze = z0 - 2 if etrier_z is None else etrier_z
    p.bone('etrier_g', (xe + .25, dos + .5, ze), 'selle')
    p.bone('etrier_d', (-xe - .25, dos + .5, ze), 'selle')
    yb = max(ventre - 1.5, 2.0)
    p.sym('etrier_g|etrier_d', 'etriviere', (xe, yb + 3, ze - .5), (xe + .5, dos + .5, ze + .5), CUIR, ton=-.3, couture=False)
    p.sym('etrier_g|etrier_d', 'etrier_fer', (xe, yb, ze - 1.5), (xe + .5, yb + 3, ze + 1.5), ('plan', 'fer'), base=3.4, masque=['.####.', '##..##', '##..##', '##..##', '######'], **CR)


def patte(p, g, d, nom, x0, x1, z0, z1, h, genou, mat, pied=None, pied_h=2.0, avance=1.0, ton=0.0, ton_bas=-.5, bande=None, griffes=None, large_bas=.5, retrait=0.0, **matk):
    """Patte en deux segments articules : haut (attache a y = h), bas plus etroit, pied. Paire +x (os g) / -x (os d). retrait : le bas part vers l'arriere (jarret)."""
    xc, zc = (x0 + x1) / 2, (z0 + z1) / 2
    p.bone(g, (xc, h, zc))
    p.bone(d, (-xc, h, zc))
    p.bone(g + '_bas', (xc, genou, zc), g)
    p.bone(d + '_bas', (-xc, genou, zc), d)
    p.sym(f'{g}|{d}', nom + '_haut', (x0, genou - .5, z0), (x1, h + 1, z1), mat, ton=ton, hide={'up'}, **matk)
    L = large_bas
    pied = pied or mat
    p.sym(f'{g}_bas|{d}_bas', nom + '_bas', (x0 + L, pied_h - .5, z0 + L + retrait), (x1 - L, genou + .5, z1 - L + retrait), mat, ton=ton_bas, hide={'up', 'down'}, **matk)
    p.sym(f'{g}_bas|{d}_bas', nom + '_pied', (x0, 0, z0 - avance + retrait), (x1, pied_h, z1 + retrait), pied, hide={'down'})
    if bande:
        yb = demi(pied_h + (genou - pied_h) * .55)
        p.sym(f'{g}_bas|{d}_bas', nom + '_bande', (x0, yb, z0 + retrait), (x1, yb + 1, z1 + retrait), bande)
    if griffes:
        w = (x1 - x0)
        n = 3 if w >= 4 else 2
        for i in range(n):
            xa = demi(x0 + w * (i + .5) / n - .5)
            p.sym(f'{g}_bas|{d}_bas', f'{nom}_griffe{i}', (xa, 0, z0 - avance - 1 + retrait), (xa + 1, 1, z0 - avance + retrait), griffes, hide={'down', 'south'})


def tuiles_rig(body='body', head='head', tail='tail', jaw=None, ears=(), tail_tip=None, legs=(), swing=('etrier_g', 'etrier_d'), **kw):
    """Rig standard pour animations.quadrupede. legs : [('leg_fl', 'leg_fl_bas', phase, amp, genou), ...]"""
    r = {'body': body, 'head': head, 'tail': tail, 'ears': ears, 'swing': swing,
         'legs': list(legs) or [('leg_fl', 'leg_fl_bas', 0.0), ('leg_fr', 'leg_fr_bas', .5), ('leg_bl', 'leg_bl_bas', .56, .95, .8), ('leg_br', 'leg_br_bas', .06, .95, .8)]}
    if jaw:
        r['jaw'] = jaw
    if tail_tip:
        r['tail_tip'] = tail_tip
    r.update(kw)
    return r
