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


def demi(v):
    return round(v * 2) / 2


def selle_y(siege):
    return demi(16 * siege + 1 - .001)


def tapis(p, rarete, dos, demi_l, av, ar, bas, parent='body', bone='selle', emblem_z=None, franges=True, pivot=None):
    """Tapis de selle : dessus + deux flancs a liseret d'or, embleme sur les flancs."""
    if bone not in p.bones:
        p.bone(bone, pivot or (0, dos, (av + ar) / 2), parent)
    t = ('tissu', 'rar_' + rarete)
    p.cube(bone, 'tapis_dessus', (-demi_l - .5, dos, av), (demi_l + .5, dos + .5, ar), t, bord=2)
    if bas is not None:
        p.sym(bone, 'tapis_flanc', (demi_l, bas, av), (demi_l + .5, dos, ar), t, bord=2, franges=franges)
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
        p.cube(bone, 'sangle', (-demi_l, ventre - .5, z0 - 3), (demi_l, ventre, z0 - 1), CUIR, ton=-.5, couture=False)
        p.sym(bone, 'sangle_flanc', (demi_l + .5, ventre, z0 - 3), (demi_l + 1, dos + .5, z0 - 1), CUIR, ton=-.2, couture=False)
    if etriers:
        xe = demi_l + 1
        p.bone('etrier_g', (xe + .25, dos + .5, z0 - 2), bone)
        p.bone('etrier_d', (-xe - .25, dos + .5, z0 - 2), bone)
        ybas = max(ventre - 1.5, 2.0)
        p.sym('etrier_g|etrier_d', 'etriviere', (xe, ybas + 3, z0 - 2.5), (xe + .5, dos + .5, z0 - 1.5), CUIR, ton=-.3, couture=False)
        p.sym('etrier_g|etrier_d', 'etrier_fer', (xe, ybas, z0 - 3.5), (xe + .5, ybas + 3, z0 - .5), ('plan', 'fer'), base=3.4, masque=['.####.', '##..##', '##..##', '##..##', '######'], **CR)
    return bone


def nacelle(p, sieges, seat, dos, demi_l, av, ar, rarete, parent='body', bas=None, ventre=None, dais=None, bois=('bois', 'bois'), emblem=True):
    """Nacelle multi-places : plancher, rambardes basses (les cuisses du cavalier passent au-dessus), coussins sous chaque siege, rive d'or, dais a 4 montants en option.
    sieges = [(x, z)] en unites modele ; seat = dessus des coussins ; dos = dessus du dos de la bete."""
    bone = 'nacelle'
    p.bone(bone, (0, dos, (av + ar) / 2), parent)
    t = ('tissu', 'rar_' + rarete)
    # tapis sous la nacelle
    p.cube(bone, 'tapis_dessus', (-demi_l - .5, dos, av - 1), (demi_l + .5, dos + .5, ar + 1), t, bord=2)
    if bas is not None:
        p.sym(bone, 'tapis_flanc', (demi_l, bas, av - 1), (demi_l + .5, dos, ar + 1), t, bord=2, franges=True)
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
        p.decal('tapis_flanc_g', 'east', stamp(EMBLEME, PAL, (av + ar) / 2 + 2.5, dos - 1.5))
    if ventre is not None:
        z1, z2 = av + 2, av + 4
        p.cube(bone, 'sangle', (-demi_l - .5, ventre - .5, z1), (demi_l + .5, ventre, z2), CUIR, ton=-.5, couture=False)
        p.sym(bone, 'sangle_flanc', (demi_l + .5, ventre, z1), (demi_l + 1, dos + .5, z2), CUIR, ton=-.2, couture=False)
        z3, z4 = ar - 4, ar - 2
        p.cube(bone, 'sangle2', (-demi_l - .5, ventre - .5, z3), (demi_l + .5, ventre, z4), CUIR, ton=-.5, couture=False)
        p.sym(bone, 'sangle2_flanc', (demi_l + .5, ventre, z3), (demi_l + 1, dos + .5, z4), CUIR, ton=-.2, couture=False)
    if dais:
        h, cloth = dais                                                   # hauteur du toit, couleur (rampe)
        for t2, zz in (('av', av + .5), ('ar', ar - 1.5)):
            p.sym(bone, f'montant_{t2}', (demi_l - 1, rail, zz), (demi_l, h, zz + 1), OR)
        c = ('tissu', cloth)
        p.cube(bone, 'dais', (-demi_l - 1.5, h, av - 1.5), (demi_l + 1.5, h + 1, ar + 1.5), c, bord=2, motif=True)
        p.cube(bone, 'dais_haut', (-demi_l + 1, h + 1, av + 1), (demi_l - 1, h + 2, ar - 1), c, bord=0, motif=False, ton=.4)
        p.cube(bone, 'dais_or', (-1.5, h + 2, (av + ar) / 2 - 1.5), (1.5, h + 3, (av + ar) / 2 + 1.5), OR)
    return bone


def oeil(p, cube, z_e, z_o, y, couleur='a', clair='b'):
    """Oeil peint sur les deux flancs d'un cube de crane. z_e : colonne de depart cote +x (face east), z_o : cote -x (face west)."""
    rows = ['dddd', f'd{clair}{couleur}k', '.dkk']
    p.decal(cube, 'east', stamp(rows, PAL, z_e, y))
    p.decal(cube, 'west', stamp(rows, PAL, z_o, y, flip_on=('west',)))


def medaillon(p, nom, bone, z, y0, cote=2):
    p.cube(bone, nom, (-cote, y0, z - .5), (cote, y0 + 4.5, z), OR)
    p.decal(nom, 'north', stamp(MEDAILLON, PAL, cote, y0 + 4.5))
