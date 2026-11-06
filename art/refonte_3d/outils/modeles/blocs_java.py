#!/usr/bin/env python3
"""Blocs Java : caisses (fermee et ouverte), Lucky Blocks, panneaux de classement, accessoires. Coordonnees centrees sur x et z, face avant vers -z."""
import numpy as np

from kit import EMBLEME, MEDAILLON, PAL
from kit_decor import AME, BOIS, BOIS_C, BOIS_N, BRAISES, CF, CH, CIRE, CR, D, DALLE, EAU, FEUILLE, FER, FER_N, MARBRE, OR, PIERRE, PIERRE_N
from materiaux import stamp

CADRE = ['#' * 22] * 2 + ['##' + '.' * 18 + '##'] * 6 + ['#' * 22] * 2


# --------------------------------------------------------------- caisses
def _caisse(cle, bois, metal, gemme, seed, ouverte, base_bois=2.8, base_metal=3.0):
    d = D(f'caisse_{cle}_bloc' + ('_ouverte' if ouverte else ''), seed)
    W = ('bois', bois)
    M = ('metal', metal)
    G = ('cristal', gemme)
    Bb = dict(base=base_bois)
    Bm = dict(base=base_metal)
    d.c('socle', (-15, 0, -14), (15, 2, 13), M, **Bm)
    d.c('socle_h', (-13, 2, -12), (13, 3, 11), M, base=base_metal - 1)
    if ouverte:
        d.c('fond', (-12, 3, -11), (12, 5, 10), W, sens='x', planche=5, base=base_bois - 1)
        d.c('paroi', (-12, 3, -11), (12, 15, -9), W, sens='x', planche=4, **Bb)
        d.c('paroi', (-12, 3, 8), (12, 15, 10), W, sens='x', planche=4, **Bb)
        d.s('paroi_e', (10, 3, -9), (12, 15, 8), W, sens='z', planche=4, **Bb)
        d.c('tresor', (-9, 5, -8), (9, 11, 7), ('ecailles', 'or'), base=3.4, bas=0, haut=0, l=3, h=3)
        d.c('tresor', (-6, 11, -5), (6, 13, 4), ('ecailles', 'or'), base=4, bas=0, haut=0, l=3, h=3)
        d.cristal('tresor_gemme', 3, -2, 13, 3, 2, mat=G, pointe=False)
    else:
        d.c('caisse', (-12, 3, -11), (12, 15, 10), W, sens='x', planche=4, **Bb)
    with d.miroir():
        d.c('pilier', (11, 2, -13), (14, 17, -10), M, **Bm)
        d.c('pilier', (11, 2, 9), (14, 17, 12), M, **Bm)
        d.c('rivet', (14, 4, -12), (15, 6, -11), M, base=base_metal + 1)
        d.c('rivet', (14, 12, -12), (15, 14, -11), M, base=base_metal + 1)
    if ouverte:
        d.c('ceinture', (-13, 14, -12), (13, 16, -9), M, base=base_metal + .3)
        d.c('ceinture', (-13, 14, 8), (13, 16, 11), M, base=base_metal + .3)
        d.s('ceinture_e', (10, 14, -9), (13, 16, 8), M, base=base_metal + .3)
    else:
        d.c('ceinture', (-13, 14, -12), (13, 16, 11), M, base=base_metal + .3)
    n = d.c('serrure', (-4, 6, -12), (4, 15, -11), M, base=base_metal + .6)
    d.c('joyau_serrure', (-2, 9, -14), (2, 13, -12), G, rot=(0, 0, 45), pivot=(0, 11, -13))
    d.p.decal(n, 'north', stamp(['kk', 'kk', 'kk'], PAL, 1, 8.5))
    d.s('anneau', (12, 5, -6), (13, 13, 5), ('plan', metal), base=base_metal, masque=CADRE, **CR)
    # couvercle : quatre gradins, deux sangles, sur un meme axe de charniere a l'arriere
    rot = dict(rot=(45, 0, 0), pivot=(0, 16, 11)) if ouverte else {}
    Lid = ('bois', bois)
    L = dict(sens='x', planche=4)
    d.c('couvercle', (-13, 16, -12), (13, 19, 11), Lid, ton=.2, base=base_bois, **L, **rot)
    d.c('couvercle', (-13, 19, -10), (13, 22, 9), Lid, ton=.3, base=base_bois, **L, **rot)
    d.c('couvercle', (-13, 22, -7), (13, 24, 6), Lid, ton=.4, base=base_bois, **L, **rot)
    d.c('couvercle', (-13, 24, -3), (13, 25, 2), Lid, ton=.5, base=base_bois, **L, **rot)
    with d.miroir():
        d.c('sangle', (6, 16, -13), (10, 20, 12), M, base=base_metal, **rot)
        d.c('sangle', (6, 20, -11), (10, 23, 10), M, base=base_metal, **rot)
        d.c('sangle', (6, 23, -8), (10, 25, 7), M, base=base_metal + .2, **rot)
        d.c('sangle', (6, 25, -4), (10, 26, 3), M, base=base_metal + .4, **rot)
    if not ouverte:
        d.c('joyau', (-2, 25, -2), (2, 27, 2), G, rot=(0, 45, 0), pivot=(0, 25, 0))
    else:
        d.c('joyau', (-2, 25, -2), (2, 27, 2), G, **rot)
    return d.fin()


TIERS = {'commune': ('bois', 'fer', 'fer', 501), 'rare': ('ardoise', 'fer', 'glace', 502), 'epique': ('pourpre', 'or', 'amethyste', 503), 'legendaire': ('bois_sombre', 'or', 'rar_legendaire', 504),
         'mythique': ('marbre_sombre', 'or', 'venin', 505), 'quete': ('bois_clair', 'cuivre', 'or_corne', 506), 'vote': ('rar_peu_commun', 'bois_clair', 'turquoise', 507)}


# --------------------------------------------------------------- lucky blocks
Q = ['..######..', '.##....##.', '##......##', '......###.', '....###...', '....##....', '..........', '....##....', '....##....']


def _lucky(cle, seed, panneau, cadre=('metal', 'or'), couleur='#6ad0f0', couronne=None):
    d = D(cle, seed)
    P = panneau
    d.c('coeur', (-7, 1, -7), (7, 15, 7), P)
    Q_pal = {'#': couleur, '@': '#ffffff'}
    for f, along in (('north', 5), ('east', 5), ('south', -5), ('west', -5)):
        d.p.decal('coeur', f, stamp(Q, Q_pal, along, 13, flip_on=()))
    # aretes du cadre
    with d.miroir():
        d.c('montant', (7, 0, -8), (8, 16, -7), cadre)
        d.c('montant', (7, 0, 7), (8, 16, 8), cadre)
        d.c('bord_e', (7, 0, -7), (8, 1, 7), cadre)
        d.c('bord_e', (7, 15, -7), (8, 16, 7), cadre)
    d.c('bord_n', (-7, 0, -8), (7, 1, -7), cadre)
    d.c('bord_n', (-7, 15, -8), (7, 16, -7), cadre)
    d.c('bord_s', (-7, 0, 7), (7, 1, 8), cadre)
    d.c('bord_s', (-7, 15, 7), (7, 16, 8), cadre)
    if couronne:
        couronne(d)
    return d.fin()


def _c_celeste(d):
    with d.miroir():
        d.c('aile', (3, 16, -5), (8, 18, 5), ('plumes', 'plume_blanche'), base=4, l=3, h=2, rot=(0, 0, 22.5), pivot=(3, 16, 0))
    d.c('socle_c', (-3, 16, -3), (3, 17, 3), ('metal', 'or'))
    d.cristal('c', 0, 0, 17, 2, 2, mat=('cristal', 'glace'), pointe=False)


def _c_givre(d):
    I = ('cristal', 'glace')
    for x, z, h in ((-5, -5, 3), (5, -5, 2), (-5, 5, 2), (5, 5, 3), (0, -6, 2), (0, 6, 2)):
        d.c('glacon', (x - 1, 16, z - 1), (x + 1, 16 + h, z + 1), I)
    d.c('c', (-2, 16, -2), (2, 19, 2), I, ton=.4, rot=(0, 45, 0), pivot=(0, 16, 0))


def _c_infernal(d):
    L = ('braises', 'basalte')
    with d.miroir():
        d.c('corne', (5, 16, -5), (7, 18, -3), ('corne', 'chitine_sombre'))
        d.c('corne', (5, 16, 3), (7, 18, 5), ('corne', 'chitine_sombre'))
    d.c('roche', (-3, 16, -3), (3, 17, 3), ('marbre', 'pierre_noire'))
    d.c('lave', (-2, 17, -2), (2, 19, 2), ('uni', 'flamme'), base=4)


def _c_neant(d):
    d.c('socle_c', (-4, 16, -4), (4, 17, 4), ('marbre', 'pierre_noire'))
    d.cristal('c', 0, 0, 17, 2, 2, mat=('cristal', 'amethyste'))
    with d.miroir():
        d.c('eclat', (5, 16, -2), (7, 18, 0), ('cristal', 'amethyste'))


def _c_sylve(d):
    d.c('mousse', (-7, 16, -7), (7, 17, 7), ('feuilles', 'feuillage'), hide={'down'})
    d.c('feuille', (-5, 17, -5), (-1, 19, -1), ('feuilles', 'feuillage'))
    d.c('feuille', (1, 17, 0), (6, 18, 5), ('feuilles', 'feuillage'))
    d.c('pousse', (-1, 17, 2), (1, 19, 4), ('feuilles', 'feuillage'), ton=.8)


LUCKY = {'lucky_celeste': (('peau', 'plume_blanche'), 4, '#3a78d0', _c_celeste, 511), 'lucky_givre': (('glace', 'glace'), 3, '#1d5fa8', _c_givre, 512), 'lucky_infernal': (('marbre', 'marbre_sombre'), 2, '#f2682a', _c_infernal, 513),
         'lucky_neant': (('marbre', 'marbre_sombre'), 1.4, '#a85ae0', _c_neant, 514), 'lucky_sylvestre': (('bois', 'bois'), 2.6, '#4bbd6a', _c_sylve, 515)}


# --------------------------------------------------------------- accessoires
def armoire():
    d = D('armoire_aeloria', 521)
    W = BOIS_C
    d.c('plinthe', (-13, 0, -6), (13, 2, 8), BOIS, sens='x')
    d.c('caisse', (-12, 2, -5), (12, 30, 7), BOIS, sens='y', planche=5, base=2.6)
    with d.miroir():
        d.c('vantail', (0, 8, -6), (11, 29, -5), W, sens='y', planche=3, **CF)
        d.c('tiroir', (0, 3, -6), (11, 7, -5), W, sens='x', **CF)
        d.c('poignee', (1, 18, -7), (2, 22, -6), OR)
        d.c('bouton', (5, 4, -7), (6, 6, -6), OR)
    d.c('corniche', (-13, 30, -6), (13, 32, 8), BOIS, sens='x')
    return d.fin()


def buche():
    d = D('buche_camp', 522)
    d.c('buche', (-12, 0, -4), (12, 6, 2), BOIS, sens='x', planche=3)
    d.c('moignon', (-4, 6, -3), (-1, 8, 0), BOIS, sens='y', rot=(0, 0, -22.5), pivot=(-2, 6, -1))
    d.c('moignon', (6, 1, 2), (9, 4, 4), BOIS, sens='z')
    d.c('mousse', (-8, 6, -4), (4, 7, 2), ('feuilles', 'feuillage'), hide={'down'})
    return d.fin()


def chaise():
    d = D('chaise_aeloria', 523)
    with d.miroir():
        d.c('pied', (5, 0, -7), (7, 8, -5), BOIS, sens='y')
        d.c('montant', (5, 0, 5), (7, 18, 7), BOIS, sens='y')
    d.c('assise', (-7, 8, -7), (7, 10, 7), BOIS, sens='x')
    d.c('coussin', (-6, 10, -6), (6, 11, 5), ('tissu', 'rouge'), bord=1)
    for y in (12, 15):
        d.c('barreau', (-5, y, 5), (5, y + 2, 6), BOIS_C, sens='x')
    d.c('haut', (-7, 18, 5), (7, 20, 7), BOIS, sens='x')
    return d.fin()


def feu_de_camp():
    d = D('feu_de_camp', 524)
    d.margelle('pierres', 0, 0, 0, 3, 10, 3, ('marbre', 'pierre'), base=2.6)
    d.c('braises', (-7, 0, -7), (7, 1, 7), BRAISES, hide={'down'})
    d.c('buche', (-7, 1, -1), (7, 3, 1), BOIS_N, sens='x', rot=(0, 45, 0), pivot=(0, 2, 0))
    d.c('buche', (-7, 2, -1), (7, 4, 1), BOIS_N, sens='x', rot=(0, -45, 0), pivot=(0, 3, 0))
    # flamme en plans croises, sans os : le JSON ne s'anime pas
    d.c('flamme_a', (-5, 3, 0), (5, 15, 1), ('flamme', 'flamme'), **CF)
    d.c('flamme_b', (0, 3, -5), (1, 15, 5), ('flamme', 'flamme'), **CR)
    # potence en A, cordes aux angles
    with d.miroir():
        d.c('montant', (11, 0, -5), (13, 18, -3), BOIS, sens='y', rot=(22.5, 0, 0), pivot=(12, 18, -4))
        d.c('montant', (11, 0, 3), (13, 18, 5), BOIS, sens='y', rot=(-22.5, 0, 0), pivot=(12, 18, 4))
        d.c('corde', (10, 15, -1), (14, 18, 1), ('cordage', 'corde'))
    d.c('traverse', (-14, 18, -1), (14, 20, 1), BOIS, sens='x')
    return d.fin()


def jarre():
    d = D('jarre_ancienne', 525)
    T = ('cuir', 'cuivre_sombre')
    d.c('pied', (-5, 0, -5), (5, 2, 5), T, couture=False)
    d.c('panse', (-7, 2, -5), (7, 12, 5), T, couture=False, hide={'up', 'down'})
    d.c('panse', (-5, 2, -7), (5, 12, 7), T, couture=False, hide={'up', 'down'})
    d.c('epaule', (-5, 12, -5), (5, 15, 5), T, couture=False, ton=.3)
    d.c('col', (-3, 15, -3), (3, 18, 3), T, couture=False, ton=.1)
    d.c('levre', (-4, 18, -4), (4, 20, 4), T, couture=False, ton=.4)
    d.c('frise', (-8, 7, -6), (8, 9, 6), ('metal', 'or'), hide={'up', 'down'})
    d.c('frise', (-6, 7, -8), (6, 9, 8), ('metal', 'or'), hide={'up', 'down'})
    return d.fin()


def panneau_feuille():
    d = D('panneau_feuille', 526)
    d.c('poteau', (-1, 0, -1), (1, 20, 1), BOIS_N, sens='y')
    F = ('plan', 'feuillage')
    M = ['..######..', '.########.', '##########', '.########.', '..######..', '....##....']
    d.c('feuille', (1, 10, 0), (7, 14, 1), F, base=3, masque=M, **CF)
    d.c('feuille', (-7, 14, 0), (-1, 18, 1), ('plan', 'feuillage'), base=4, masque=M, **CF)
    d.c('feuille', (1, 16, 0), (6, 20, 1), ('plan', 'bois_clair'), base=4, masque=M, **CF)
    return d.fin()


def panneau_quetes():
    d = D('panneau_quetes', 527)
    d.c('fond', (-16, 2, 0), (16, 26, 2), BOIS, sens='x', planche=5)
    M = FER_N
    d.c('cadre', (-17, 25, -1), (17, 27, 3), M, ton=.5)
    d.c('cadre', (-17, 1, -1), (17, 3, 3), M, ton=.5)
    with d.miroir():
        d.c('cadre_v', (15, 3, -1), (17, 25, 3), M, ton=.5)
        d.c('pied', (11, -8, 0), (13, 1, 2), BOIS_N, sens='y')
    for i, (x0, y0, x1, y1) in enumerate(((-13, 12, -5, 23), (-3, 15, 5, 23), (7, 8, 14, 22), (-12, 4, -4, 11), (-2, 5, 5, 13))):
        n = d.c('affiche', (x0, y0, -1), (x1, y1, 0), ('peau_tissu', 'creme'), base=5, pli=40, **CF)
        d.p.decal(n, 'north', stamp(['kkkkkk', '......', 'kkkkk.', '......', 'kkkkkk'], PAL, x1 - 1, y1 - 2))
        d.c('punaise', ((x0 + x1) // 2, y1 - 1, -2), ((x0 + x1) // 2 + 1, y1, -1), ('metal', 'or'))
    return d.fin()


def pot_decore():
    d = D('pot_decore', 528)
    T = ('cuir', 'cuivre_sombre')
    d.c('corps', (-6, 0, -6), (6, 8, 6), T, couture=False, base=3.4, ton=.2)
    d.c('frise', (-7, 4, -5), (7, 6, 5), ('metal', 'or'), hide={'up', 'down'})
    d.c('frise', (-5, 4, -7), (5, 6, 7), ('metal', 'or'), hide={'up', 'down'})
    d.c('epaule', (-4, 8, -4), (4, 9, 4), T, couture=False, base=3.6)
    d.c('levre', (-5, 9, -5), (5, 12, 5), T, couture=False, base=3.8)
    return d.fin()


def potion_etoilee():
    d = D('potion_etoilee', 529)
    V = ('uni', 'glace')
    d.c('liquide', (-4, 1, -4), (4, 7, 4), ('etoile', 'vide'), base=3.4, densite=.93)
    d.c('fond', (-4, 0, -4), (4, 1, 4), ('uni', 'glace'), base=3.4)
    d.c('air', (-4, 7, -4), (4, 8, 4), ('uni', 'glace'), base=4.4, no_ao=True)
    with d.miroir():
        for z in (-5, 4):
            d.c('arete', (4, 0, z), (5, 8, z + 1), ('uni', 'glace'), base=4.6, no_ao=True)
    d.c('epaule', (-3, 8, -3), (3, 9, 3), ('uni', 'glace'), base=4.4, no_ao=True)
    d.c('col', (-2, 9, -2), (2, 11, 2), ('uni', 'glace'), base=4.6, no_ao=True)
    d.c('bouchon', (-2, 11, -2), (2, 13, 2), BOIS_C, sens='y')
    d.c('cire', (-3, 10, -3), (3, 11, 3), ('metal', 'or'), hide={'up'})
    return d.fin()


def table():
    d = D('table_aeloria', 530)
    d.c('plateau', (-13, 9, -6), (13, 11, 6), BOIS, sens='x', planche=4)
    with d.miroir():
        d.c('pied', (9, 0, -5), (12, 9, -2), BOIS_N, sens='y')
        d.c('pied', (9, 0, 2), (12, 9, 5), BOIS_N, sens='y')
        d.c('clou', (11, 11, -5), (12, 12, -4), ('metal', 'fer'))
        d.c('clou', (11, 11, 4), (12, 12, 5), ('metal', 'fer'))
    d.c('entretoise', (-9, 4, -1), (9, 6, 1), BOIS_N, sens='x')
    return d.fin()


def viande():
    d = D('viande_deco', 531)
    d.c('jambon', (-5, 0, -7), (5, 7, 5), ('fourrure', 'fauve'), base=2.4, taches=1.2, traits=1.4)
    d.c('couenne', (-4, 7, -6), (4, 8, 4), ('fourrure', 'fauve'), base=3, taches=1.4, traits=1)
    d.c('tranche', (-4, 1, 5), (4, 6, 6), ('uni', 'museau_rose'), base=4.4, **CF)
    d.c('os', (-2, 2, 6), (2, 5, 11), ('ivoire', 'ivoire'), base=3.6)
    d.c('bout', (-3, 1, 11), (3, 6, 13), ('ivoire', 'ivoire'), base=4.2)
    return d.fin()


BLOCS = {}
for _c, (_b, _m, _g, _s) in TIERS.items():
    BLOCS[f'caisse_{_c}_bloc'] = (lambda c=_c, b=_b, m=_m, g=_g, s=_s: _caisse(c, b, m, g, s, False))
    BLOCS[f'caisse_{_c}_bloc_ouverte'] = (lambda c=_c, b=_b, m=_m, g=_g, s=_s: _caisse(c, b, m, g, s + 50, True))
for _k, (_p, _b, _q, _f, _s) in LUCKY.items():
    _pp = ('uni', _p[1]) if _p[0] in ('glace', 'peau') else _p
    BLOCS[_k] = (lambda k=_k, p=_pp, b=_b, q=_q, f=_f, s=_s: _lucky(k, s, (p[0], p[1]) if p[0] != 'uni' else p, couleur=q, couronne=f))
BLOCS.update({'armoire_aeloria': armoire, 'buche_camp': buche, 'chaise_aeloria': chaise, 'feu_de_camp': feu_de_camp, 'jarre_ancienne': jarre, 'panneau_feuille': panneau_feuille, 'panneau_quetes': panneau_quetes,
              'pot_decore': pot_decore, 'potion_etoilee': potion_etoilee, 'table_aeloria': table, 'viande_deco': viande})
