#!/usr/bin/env python3
"""Roi gobelin : trapu, gros ventre sous la maille, tete large a nez fort et grandes oreilles, couronne de guingois, cape rouge a galon d'or, couperet ebreche. Meme squelette que art/boss_blockbench/roi_gobelin.py.
Le rendu de ce boss n'a pas de couche emissive dans AnimatedModels : aucun pixel lumineux ici."""
import roi_gobelin as O
from boss_kit import B, P, ek
from entity_kit import border, rivets, stamp, tatter

SKIN = P('peau', 'gobelin', base=3.4)
SKIN_D = P('peau', 'gobelin', base=2.5)
SKIN_C = P('peau', 'gobelin', base=4.2)
MAIL = P('mailles', 'fer', base=2.6)
FER = P('plaque', 'fer', base=2.6)
FER_R = P('plaque', 'fer', base=2.8, rivets=4)
FER_N = P('plaque', 'fer_noir', base=3)
OR = P('plaque', 'or', base=3)
OR_M = P('metal', 'or', base=3.2)
CUIR = P('cuir', 'cuir', ton=-.8, couture=False)
CUIR_S = P('cuir', 'cuir', ton=-1.4, couture=False)
ROUGE = P('peau_tissu', 'rouge', base=3, pli=7)
ROUGE_S = P('peau_tissu', 'rouge', base=2.2, pli=7)
ETOFFE = P('peau_tissu', 'tissu_brun', base=2.2, pli=5)
FOUR = P('poils_longs', 'soie_noire', base=2.6, y=(20, 40))
OS = P('os', 'os', base=3.6)
IVOIRE = P('corne', 'ivoire', base=3)
ACIER = P('metal', 'acier', base=3.2)
RUBIS = P('cristal', 'rouge', base=2.6, no_ao=True)
K = (22, 18, 16)
Y_CORPS = (0, 50)

VISAGE = stamp([
    '................',
    '................',
    '................',
    '................',
    '................',
    '.RRRR......RRRR.',
    '.RkkR......RkkR.',
    '..RRd......dRR..',
    '................',
    '................',
    '................',
    '................',
], {'R': (232, 52, 40), 'k': K, 'd': (40, 58, 18)}, ay='t')
NARINES = stamp(['......', '......', '......', '.kk.kk'], {'k': (22, 30, 12)})


def build():
    m = ek.Model(O.KEY, O.TITLE)
    b = B(m)
    m.bone('racine', (0, 0, 0))
    m.bone('bassin', (0, 14, 0), 'racine')
    # bassin, ceinture cloutee, boucle-crane, sacoches
    b.c('bassin', 'hanches', (-10, 10, -6), (10, 16, 6), ETOFFE)
    b.c('bassin', 'ceinture', (-11, 15, -7), (11, 19, 7), CUIR, deco={'*': [O.belt_studs]})
    b.c('bassin', 'boucle_crane', (-4, 13, -10), (4, 20, -7), OS, deco={'north': [O.SKULL]})
    b.c('bassin', 'sacoche_d', (10, 10, -4), (14, 15, 3), CUIR, deco={'*': [border((43, 26, 15), 't')]})
    b.c('bassin', 'sacoche_g', (-14, 11, 0), (-10, 15, 5), CUIR_S)
    m.bone('etendard', (0, 15, -8), 'bassin')
    b.c('etendard', 'etendard_tissu', (-4, 2, -8.5), (4, 15, -7.5), ROUGE, deco={'sides': [border((217, 168, 50), 'lr'), O.BANNER, tatter(3, seed=1.3)]})
    # ventre rond sous la maille, plastron, grand mantelet de fourrure
    m.bone('torse', (0, 17, 0), 'bassin')
    b.c('torse', 'ventre', (-10, 17, -6), (10, 31, 6), MAIL)
    b.c('torse', 'bedaine', (-8, 18, -8), (8, 27, -6), MAIL, deco={'*': [border((30, 32, 38), 'b')]})
    b.c('torse', 'plastron', (-9, 25, -9), (9, 32, -7), FER_R, deco={'north': [O.chain_and_trim]})
    b.c('torse', 'pendentif', (-2, 24, -10), (2, 28, -9), OR, deco={'north': [stamp(['.rr.', 'rRRr', 'rRRr', '.rr.'], {'r': (169, 24, 36), 'R': (255, 114, 114)})]})
    b.c('torse', 'mantelet', (-13, 29, -7), (13, 33, 7), FOUR)
    b.c('torse', 'col_fourrure', (-11, 33, -6), (11, 35, 6), FOUR, deco={'*': [border((26, 18, 14), 'b')]})
    # tete : grand crane, front bas, nez fort, machoire, oreilles
    m.bone('tete', (0, 32, -3), 'torse')
    b.c('tete', 'crane', (-8, 33, -11), (8, 45, 2), SKIN, deco={'north': [VISAGE]})
    b.c('tete', 'arcade', (-9, 40, -13), (9, 43, -11), SKIN_D, deco={'north': [border((40, 58, 18), 'b')]})
    b.c('tete', 'nez', (-3, 34, -14), (3, 39, -11), SKIN_C, deco={'north': [NARINES]})
    b.c('tete', 'joue_d', (6, 34, -12), (9, 37, -6), SKIN)
    b.c('tete', 'joue_g', (-9, 34, -12), (-6, 37, -6), SKIN)
    m.bone('machoire', (0, 34, -4), 'tete')
    b.c('machoire', 'machoire_bas', (-7, 29, -12), (7, 34, -2), SKIN_D, deco={'north': [O.TEETH]})
    b.paire('machoire', 'machoire', 'defense', (5, 32, -14), (8, 36, -11), IVOIRE)
    b.paire('machoire', 'machoire', 'defense_bout', (6, 36, -14), (8, 39, -12), IVOIRE)
    for side, sx in (('droite', 1), ('gauche', -1)):
        os_ = m.bone(f'oreille_{side}', (8 * sx, 40, -5), 'tete', rot=(0, -18 * sx, 22 * sx))
        for part, x_in, w, y0, h in (('base', 8, 6, 36, 7), ('milieu', 14, 5, 37, 5), ('pointe', 19, 5, 38, 3)):
            x0 = x_in if sx > 0 else -x_in - w
            m.cube(os_, f'oreille_{side}_{part}', (x0, y0, -5.5), (w, h, 1), SKIN, share=f'oreille_{part}', mirror=sx < 0, deco={'north': [O.ear_inner]} if part != 'pointe' else None)
    # couronne de guingois
    m.bone('couronne', (0, 45, -4), 'tete', rot=(-4, 0, 7))
    b.c('couronne', 'couronne_bandeau', (-7, 45, -11), (7, 48, 3), OR, deco={'*': [O.crown_band], 'up': [O.velvet]})
    b.c('couronne', 'gemme', (-2, 45, -12), (2, 48, -11), RUBIS)
    for i, (x, h, z) in enumerate(((-1, 5, -12), (-7, 4, -12), (5, 4, -12), (-7, 3, 2), (5, 3, 2), (-1, 3, 2))):
        m.cube('couronne', f'fleuron_{i + 1}', (x, 48, z), (2, h, 1), OR_M, deco={'*': [O.crown_points]})
    # bras
    for side, sx in (('droit', 1), ('gauche', -1)):
        os_ = m.bone(f'bras_{side}', (12 * sx, 30, -2), 'torse', rot=(0, 0, (12 if sx > 0 else 6) * sx))
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        m.cube(os_, f'bras_{side}_peau', (xo(10, 7), 21, -5.5), (7, 10, 7), SKIN, share='bras_haut', mirror=sx < 0)
        fa = m.bone(f'avant_bras_{side}', (13.5 * sx, 22, -2), os_, rot=(40, 0, 0) if sx > 0 else (18, 0, 0))
        m.cube(fa, f'avant_bras_{side}_peau', (xo(10.5, 6), 14, -5), (6, 8, 6), SKIN, share='avant_bras', mirror=sx < 0)
        m.cube(fa, f'brassard_{side}', (xo(10, 7), 15, -5.5), (7, 6, 7), FER, inflate=.3, share='brassard', mirror=sx < 0, deco={'*': [rivets((150, 154, 163), (44, 46, 51), every=3, rows=('t', 'b'))]})
        h = m.bone('main_droite' if sx > 0 else 'main_gauche', (13.5 * sx, 14, -2), fa)
        m.cube(h, f'poing_{side}', (xo(10, 7), 8, -6), (7, 6, 8), SKIN, share='poing', mirror=sx < 0, deco={'*': [O.fingers]})
    # epaulieres : droite legere a bande rouge, gauche enorme a pointes
    b.c('bras_droit', 'epauliere_droite', (9, 28, -7), (18, 33, 3), FER_N, deco={'*': [border((147, 38, 33), 'b')]})
    b.c('bras_droit', 'sangle_droite', (9.5, 25, -6.5), (17.5, 28, 2.5), CUIR_S)
    b.c('bras_gauche', 'epauliere_gauche', (-22, 26, -9), (-9, 33, 5), FER_R)
    b.c('bras_gauche', 'epauliere_gauche_lame', (-21, 22, -8), (-9, 26, 4), FER)
    m.bone('pointes_epauliere', (-16, 33, -2), 'bras_gauche', rot=(0, 0, 18))
    for i, z in enumerate((-8, -3, 2)):
        m.cube('pointes_epauliere', f'pointe_{i + 1}', (-18 + 1.2 * i, 33, z), (3, 5, 3), ACIER, share='pointe')
        m.cube('pointes_epauliere', f'pointe_b_{i + 1}', (-17.5 + 1.2 * i, 38, z + .5), (2, 3, 2), ACIER, share='pointe_b')
    # couperet ebreche : long manche, lame large, dos renforce
    m.bone('couperet', (13.5, 10.5, -2), 'main_droite', rot=(35, 0, 0))
    b.c('couperet', 'manche', (12.5, 9.5, -12), (14.5, 11.5, 3), CUIR, deco={'*': [lambda f: [f.px(x, y, (59, 37, 21)) for y in range(f.h) for x in range(f.w) if (x + y) % 3 == 0]]})
    b.c('couperet', 'pommeau', (12, 9, 3), (15, 12, 5), FER_N)
    b.c('couperet', 'lame', (12.5, -2, -30), (14.5, 10, -12), ACIER, deco={'*': [O.blade_edge]})
    b.c('couperet', 'dos_lame', (12, 10, -31), (15, 12, -12), FER)
    b.c('couperet', 'garde', (11.5, 8.5, -13), (15.5, 12.5, -11), FER_N)
    # jambes courtes, jambieres, bottes a coque
    for side, sx in (('droite', 1), ('gauche', -1)):
        foot = 'droit' if sx > 0 else 'gauche'
        xo = (lambda x0, w: x0 if sx > 0 else -x0 - w)
        lg = m.bone(f'jambe_{side}', (5 * sx, 14, 0), 'bassin')
        m.cube(lg, f'cuisse_{side}', (xo(1.5, 7), 8, -3.5), (7, 7, 7), ETOFFE, share='cuisse', mirror=sx < 0)
        m.cube(lg, f'jambiere_{side}', (xo(1, 8), 3, -4), (8, 6, 8), FER_R, share='jambiere', mirror=sx < 0)
        m.cube(lg, f'genouillere_{side}', (xo(2.5, 5), 7, -5), (5, 3, 1), FER_N, share='genou', mirror=sx < 0)
        ft = m.bone(f'pied_{foot}', (5 * sx, 3, 0), lg)
        m.cube(ft, f'botte_{side}', (xo(.5, 9), 0, -7), (9, 4, 10), CUIR, share='botte', mirror=sx < 0)
        m.cube(ft, f'coque_{side}', (xo(1, 8), .5, -8), (8, 3, 3), FER_N, share='coque', mirror=sx < 0)
    # cape en trois pans
    GAL = border((217, 168, 50), 'lr')
    m.bone('cape_haut', (0, 32, 7), 'torse')
    b.c('cape_haut', 'cape_haut_tissu', (-12, 21, 6.5), (12, 32, 7.5), ROUGE, deco={'sides': [GAL]})
    m.bone('cape_milieu', (0, 21, 7), 'cape_haut')
    b.c('cape_milieu', 'cape_milieu_tissu', (-12.5, 11, 6.5), (12.5, 21, 7.5), ROUGE, deco={'sides': [GAL]})
    m.bone('cape_bas', (0, 11, 7), 'cape_milieu')
    b.c('cape_bas', 'cape_bas_tissu', (-13, 2, 6.5), (13, 11, 7.5), ROUGE_S, deco={'sides': [GAL, tatter(4, seed=.7, holes=.05)]})
    return m
