# -*- coding: utf-8 -*-
"""Presse de compression : machine multibloc (5 x 5 blocs) des Mines d'Aeloria. Au centre, une enclume de presse
sur un socle grave de runes, surmontee d'un piston hydraulique dans un portique d'argent ; a gauche (vu de face),
une tremie qui deverse le minerai brut dans la chambre ; a droite, une palette ou s'empilent les blocs compresses ;
a l'arriere, un reacteur de cristal relie aux verins par des conduits lumineux, entoure de deux engrenages et d'un
manometre. Quand un joueur approche, le piston pompe, les engrenages tournent et le cristal s'illumine.
Le bloc fonctionnel (ouvre l'interface de compression) est le socle de la presse."""
import entity_kit as ek
from structure_common import (PIERRE, ARGENT, RUNE, CRISTAL_B, BOIS, FER, BRAISE, Masonry, silver, crystal,
                              rune_panel, trim, floor_lines, decal, base_panels)
from entity_kit import Mat, pal

KEY = 'presse_compression'
TITLE = 'Presse de compression'
ENTITY_CLASS = 'PresseCompression'
JAVA = 'PresseCompression'
VISIBLE_BOX = (5, 5, 0)
VIEW_SCALE = 4.6
SHEET_SCALE = 3.0
GIF_SCALE = 2.8
SHEET_YAW = 35
NO_COLLISION = ('cristal_source', 'conduits', 'minerais', 'aiguille', 'blocs_compresses')
STATES = {'inactif': 'arret', 'activation': 'demarrage', 'actif': 'active'}

MINERAI = pal('#0b3b3a', '#14706a', '#25b8a8', '#6ef0d8', '#d2fff4')
ORE_LUEUR = pal('#3a1a08', '#87420e', '#e0821c', '#ffc060', '#fff0c0')

STONE = Masonry(PIERRE, 0.42, decos=[base_panels()])
STONE_TOP = Masonry(PIERRE, 0.5, seed=1)
WALL = Masonry(PIERRE, 0.4, seed=4)
DARK = Masonry(PIERRE, 0.3, bw=6, bh=3, seed=2)
SILVER = silver()
SILVER_RIV = silver(rivet=True)
IRON = Mat(FER, level=0.5, noise=0.18, scale=1.5, grad=0.3, up=0.3, down=-0.2, rim=0.15)
IRON_DARK = Mat(FER, level=0.32, noise=0.18, scale=1.5, grad=0.25, up=0.2, down=-0.2, rim=0.1)
WOOD = Mat(BOIS, level=0.5, noise=0.3, scale=(4, 1), grad=0.1, decos=[lambda f: [f.px(x, y, BOIS[0]) for y in range(f.h)
                                                                                  for x in range(f.w) if f.side and x % 5 == 4]])
CRYS = crystal(CRISTAL_B, level=0.6)
CONDUIT = Mat(RUNE, level=0.55, noise=0.25, scale=1, grad=0.2, glow=True)
ORE = Mat(MINERAI, level=0.6, noise=0.3, scale=1.3, grad=0.3, up=0.2, down=-0.15, glow=True)
ORE_CHAUD = Mat(ORE_LUEUR, level=0.55, noise=0.3, scale=1.3, grad=0.3, glow=True)
COMPRESSE = Mat(MINERAI, level=0.7, noise=0.12, scale=1.0, grad=0.3, rim=0.2, glow=True,
                decos=[ek.border(MINERAI[4], 'tblr', 1, glow=True)])

SOL = [(-4, 8, -4, 26), (4, 30, 8, 30), (-12, 28, -34, 28), (34, -4, 34, 18)]


def gear(m, parent, name, cx, cy, z, r, depth, mat):
    """Engrenage dans le plan XY (axe Z) : croix, diagonales et moyeu ; l'os tourne autour de son centre."""
    b = m.bone(name, (cx, cy, z + depth / 2), parent)
    m.cube(b, f'{name}_barre_h', (cx - r, cy - 2, z), (2 * r, 4, depth), mat, share=f'{name}_bh')
    m.cube(b, f'{name}_barre_v', (cx - 2, cy - r, z), (4, 2 * r, depth), mat, share=f'{name}_bv')
    d = round(r * 1.4)
    m.cube(b, f'{name}_diag_1', (cx - d, cy - 2, z), (2 * d, 4, depth), mat, rot=(0, 0, 45), origin=(cx, cy, z + depth / 2), share=f'{name}_d')
    m.cube(b, f'{name}_diag_2', (cx - d, cy - 2, z), (2 * d, 4, depth), mat, rot=(0, 0, -45), origin=(cx, cy, z + depth / 2), share=f'{name}_d')
    m.cube(b, f'{name}_moyeu', (cx - 3, cy - 3, z - 1), (6, 6, depth + 2), SILVER_RIV, share=f'{name}_m')
    return b


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))

    # ------------------------------------------------------------ plateforme
    p = m.bone('plateforme', (0, 0, 0), 'racine')
    m.cube(p, 'dalle', (-40, 0, -20), (80, 4, 64), STONE, deco={'up': [floor_lines(SOL)]})
    m.cube(p, 'marche', (-20, 0, -28), (40, 2, 8), STONE_TOP)
    m.cube(p, 'borne_avant_gauche', (30, 0, -22), (8, 10, 8), DARK, share='borne', deco={'sides': [rune_panel(1, margin=1), trim('lrt')]})
    m.cube(p, 'borne_avant_droite', (-38, 0, -22), (8, 10, 8), DARK, share='borne', deco={'sides': [rune_panel(1, margin=1), trim('lrt')]})

    # ------------------------------------------------------------ socle et chambre de compression (bloc fonctionnel)
    sc = m.bone('socle_presse', (0, 4, 0), 'racine')
    m.cube(sc, 'socle', (-14, 4, -12), (28, 12, 24), DARK,
           deco={'north': [rune_panel(2, margin=2)], 'south': [rune_panel(2, margin=2)], 'sides': [trim('lr')]})
    m.cube(sc, 'socle_plaque', (-16, 16, -14), (32, 3, 28), SILVER_RIV)
    m.cube(sc, 'chambre_cuve', (-9, 19, -9), (18, 2, 18), IRON_DARK)
    m.cube(sc, 'chambre_bord_n', (-10, 19, -11), (20, 3, 2), SILVER, share='bord_h')
    m.cube(sc, 'chambre_bord_s', (-10, 19, 9), (20, 3, 2), SILVER, share='bord_h')
    m.cube(sc, 'chambre_bord_e', (8, 19, -9), (2, 3, 18), SILVER, share='bord_v')
    m.cube(sc, 'chambre_bord_o', (-10, 19, -9), (2, 3, 18), SILVER, share='bord_v')

    # ------------------------------------------------------------ portique (quatre montants d'argent et traverse)
    po = m.bone('portique', (0, 19, 0), 'racine')
    for nom, (x, z) in {'montant_av_g': (14, -14), 'montant_av_d': (-18, -14), 'montant_ar_g': (14, 12), 'montant_ar_d': (-18, 12)}.items():
        m.cube(po, f'portique_{nom}', (x, 19, z), (4, 41, 4), SILVER_RIV, share='portique_montant')
    m.cube(po, 'portique_traverse', (-20, 60, -16), (40, 5, 32), SILVER_RIV)
    m.cube(po, 'portique_dalle', (-17, 65, -13), (34, 3, 26), DARK, deco={'sides': [trim('tb')]})
    m.cube(po, 'verin_corps', (-8, 68, -8), (16, 9, 16), IRON, deco={'sides': [trim('tb', FER[4])]})
    m.cube(po, 'verin_bague_1', (-9, 70, -9), (18, 2, 18), SILVER, share='verin_bague')
    m.cube(po, 'verin_bague_2', (-9, 74, -9), (18, 2, 18), SILVER, share='verin_bague')
    m.cube(po, 'verin_tete', (-5, 77, -5), (10, 2, 10), SILVER_RIV)

    # ------------------------------------------------------------ piston (descend sur la chambre)
    pi = m.bone('piston', (0, 36, 0), 'racine')
    m.cube(pi, 'piston_tige', (-3, 43, -3), (6, 24, 6), IRON)
    m.cube(pi, 'piston_manchon', (-5, 52, -5), (10, 6, 10), SILVER_RIV)
    m.cube(pi, 'piston_tete', (-8, 36, -8), (16, 7, 16), IRON_DARK, deco={'sides': [trim('tb', FER[5])]})
    m.cube(pi, 'piston_patin', (-10, 33, -10), (20, 3, 20), SILVER_RIV)
    m.cube(pi, 'piston_pointe_1', (-7, 31, -7), (4, 2, 4), IRON, share='piston_dent')
    m.cube(pi, 'piston_pointe_2', (3, 31, -7), (4, 2, 4), IRON, share='piston_dent')
    m.cube(pi, 'piston_pointe_3', (-7, 31, 3), (4, 2, 4), IRON, share='piston_dent')
    m.cube(pi, 'piston_pointe_4', (3, 31, 3), (4, 2, 4), IRON, share='piston_dent')

    # ------------------------------------------------------------ tremie (a gauche, +X) et minerai brut
    tr = m.bone('tremie', (28, 4, 0), 'racine')
    for nom, (x, z) in {'av': (30, -8), 'ar': (30, 6)}.items():
        m.cube(tr, f'tremie_pied_{nom}', (x, 4, z), (3, 24, 3), WOOD, share='tremie_pied')
        m.cube(tr, f'tremie_pied_{nom}_b', (x + 8, 4, z), (3, 24, 3), WOOD, share='tremie_pied')
    m.cube(tr, 'tremie_entretoise', (30, 14, -8), (11, 2, 17), WOOD)
    m.cube(tr, 'tremie_haut', (26, 28, -10), (19, 8, 21), IRON, deco={'sides': [trim('tb', FER[5])]})
    m.cube(tr, 'tremie_bas', (29, 24, -7), (13, 4, 15), IRON_DARK)
    m.cube(tr, 'tremie_col', (32, 20, -4), (7, 4, 9), IRON_DARK)
    m.cube(tr, 'goulotte', (15, 19, -3), (18, 3, 7), IRON, rot=(0, 0, 18), origin=(32, 22, 0.5))
    m.cube(tr, 'tremie_cerclage', (25, 31, -11), (21, 2, 23), SILVER_RIV)
    mi = m.bone('minerais', (35, 36, 0), tr)
    m.cube(mi, 'minerai_tas', (28, 36, -8), (15, 4, 17), ORE)
    for i, (x, y, z, s) in enumerate(((30, 40, -5, 4), (36, 40, 1, 5), (32, 41, 4, 3), (39, 40, -4, 3), (34, 43, -1, 3))):
        m.cube(mi, f'minerai_{i + 1}', (x, y, z), (s, s, s), ORE, share=f'minerai_{s}', rot=(0, 20 + i * 15, 0), origin=(x + s / 2, y, z + s / 2))

    # ------------------------------------------------------------ palette de sortie (a droite, -X) et blocs compresses
    sa = m.bone('sortie', (-28, 4, 0), 'racine')
    m.cube(sa, 'glissiere', (-30, 19, -4), (20, 3, 8), IRON, rot=(0, 0, -16), origin=(-12, 21, 0), deco={'sides': [trim('lr', FER[5])]})
    m.cube(sa, 'palette_dalle', (-38, 4, -12), (22, 3, 24), WOOD)
    m.cube(sa, 'palette_lattes', (-38, 7, -12), (22, 2, 24), WOOD, deco={'up': [lambda f: [f.px(x, y, BOIS[0]) for y in range(f.h) for x in range(f.w) if y % 6 == 5]]})
    m.cube(sa, 'palette_garde_ar', (-38, 9, 10), (22, 12, 2), WOOD)
    m.cube(sa, 'palette_garde_g', (-38, 9, -12), (2, 12, 22), WOOD)
    bc = m.bone('blocs_compresses', (-28, 9, 0), sa)
    for i, (x, y, z) in enumerate(((-34, 9, -8), (-27, 9, -8), (-34, 9, -1), (-27, 9, -1), (-34, 9, 5), (-27, 9, 5), (-31, 16.5, -5), (-31, 16.5, 2))):
        m.cube(bc, f'bloc_compresse_{i + 1}', (x, y, z), (6, 7, 6), COMPRESSE, share='bloc_compresse')

    # ------------------------------------------------------------ reacteur de cristal (au fond) et conduits
    re = m.bone('reacteur', (0, 4, 30), 'racine')
    m.cube(re, 'reacteur_mur', (-24, 4, 24), (48, 30, 8), WALL, deco={'north': [rune_panel(0, margin=3)], 'sides': [trim('lr')]})
    m.cube(re, 'reacteur_corniche', (-26, 34, 23), (52, 3, 10), SILVER_RIV)
    m.cube(re, 'reacteur_socle', (-8, 4, 20), (16, 6, 12), STONE_TOP, deco={'sides': [rune_panel(0, margin=1)]})
    for nom, (x, z) in {'g': (6, 20), 'd': (-9, 20)}.items():
        m.cube(re, f'reacteur_montant_{nom}', (x, 10, z), (3, 20, 3), SILVER_RIV, share='reacteur_montant')
    m.cube(re, 'reacteur_chapeau', (-9, 30, 19), (18, 3, 14), SILVER_RIV)
    cc = m.bone('cristal_source', (0, 20, 26), re)
    m.cube(cc, 'cristal_source_coeur', (-3, 12, 23), (6, 17, 6), CRYS)
    m.cube(cc, 'cristal_source_pointe', (-2, 29, 24), (4, 4, 4), CRYS)
    m.cube(cc, 'cristal_source_base', (-2, 10, 24), (4, 2, 4), CRYS)
    co = m.bone('conduits', (0, 20, 20), 'racine')
    m.cube(co, 'conduit_haut', (-2, 33, 18), (4, 4, 4), CONDUIT)
    m.cube(co, 'conduit_vertical', (-2, 37, 18), (4, 30, 4), CONDUIT, deco={'sides': [trim('tb', RUNE[3])]})
    m.cube(co, 'conduit_vers_verin', (-2, 64, 9), (4, 4, 13), CONDUIT)
    m.cube(co, 'conduit_bague_a', (-3, 45, 17), (6, 3, 6), SILVER, share='conduit_bague')
    m.cube(co, 'conduit_bague_b', (-3, 56, 17), (6, 3, 6), SILVER, share='conduit_bague')
    m.cube(co, 'conduit_socle', (-2, 4, 12), (4, 2, 10), CONDUIT)
    m.cube(co, 'conduit_socle_regard', (-4, 4, 10), (8, 3, 3), SILVER_RIV)

    # ------------------------------------------------------------ engrenages et manometre sur le mur du fond
    gear(m, 'racine', 'engrenage_g', 16, 52, 18, 8, 3, IRON)
    gear(m, 'racine', 'engrenage_d', -16, 44, 18, 6, 3, IRON_DARK)
    ma = m.bone('manometre', (0, 40, 22), 'racine')
    m.cube(ma, 'manometre_cadran', (-16, 56, 21), (9, 9, 3), SILVER_RIV, share='manometre')
    m.cube(ma, 'manometre_fond', (-15, 57, 23.5), (7, 7, 1), Mat(RUNE, level=0.25, noise=0.15, scale=1))
    ai = m.bone('aiguille', (-11.5, 60.5, 23), ma)
    m.cube(ai, 'aiguille_tige', (-12, 60, 23.5), (1, 4, 1), Mat(BRAISE, level=0.8, glow=True))
    return m


COLORS = {'racine': 0, 'plateforme': 1, 'socle_presse': 5, 'portique': 2, 'piston': 3, 'tremie': 4, 'minerais': 6,
          'sortie': 7, 'blocs_compresses': 6, 'reacteur': 2, 'cristal_source': 6, 'conduits': 6, 'engrenage_g': 3,
          'engrenage_d': 3, 'manometre': 1, 'aiguille': 7}


def animations(m):
    P = ek.key_poses
    L = 'linear'

    # --- arret (4 s) : aiguille au repos, cristal qui respire, engrenages immobiles
    a = m.anim('arret', 4.0, 'loop')
    P(a, [(0, {}), (2.0, {'cristal_source': (0, 0.6, 0)}), (4.0, {})], 'position', bones=['cristal_source'])
    a.rot('aiguille', [(0, 0, 0, -30, L), (4.0, 0, 0, -30, L)])

    # --- demarrage (1 s) : le piston se leve de quelques pixels, le cristal monte, l'aiguille grimpe
    a = m.anim('demarrage', 1.0, 'once')
    P(a, [(0, {}), (1.0, {'cristal_source': (0, 2, 0), 'piston': (0, 4, 0)})], 'position', interp='ease', bones=['cristal_source', 'piston'])
    a.rot('aiguille', [(0, 0, 0, -30, L), (1.0, 0, 0, 40, L)])
    a.rot('engrenage_g', [(0, 0, 0, 0, L), (1.0, 0, 0, 90, L)])
    a.rot('engrenage_d', [(0, 0, 0, 0, L), (1.0, 0, 0, -120, L)])

    # --- active (1,6 s, en boucle) : le piston s'abat sur la chambre puis remonte, engrenages et cristal en mouvement
    a = m.anim('active', 1.6, 'loop')
    P(a, [(0, {'piston': (0, 4, 0)}), (0.5, {'piston': (0, 4, 0)}), (0.8, {'piston': (0, -11, 0)}), (1.0, {'piston': (0, -11, 0)}),
          (1.6, {'piston': (0, 4, 0)})], 'position', bones=['piston'])
    a.rot('engrenage_g', [(0, 0, 0, 90, L), (1.6, 0, 0, 450, L)])
    a.rot('engrenage_d', [(0, 0, 0, -120, L), (1.6, 0, 0, -480, L)])
    a.rot('aiguille', [(0, 0, 0, 40, L), (0.8, 0, 0, 70, L), (1.6, 0, 0, 40, L)])
    P(a, [(0, {'cristal_source': (0, 2, 0)}), (0.8, {'cristal_source': (0, 2.8, 0)}), (1.6, {'cristal_source': (0, 2, 0)})],
      'position', bones=['cristal_source'])
    a.rot('cristal_source', [(0, 0, 0, 0, L), (1.6, 0, 90, 0, L)])
    a.scale('blocs_compresses', [(0, 1, 1, 1), (0.9, 1, 1.06, 1), (1.6, 1, 1, 1)])
