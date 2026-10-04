# -*- coding: utf-8 -*-
"""Forge arcanique : atelier multibloc (5 x 4 blocs). A gauche (vu de face), un four massif en pierre noire avec une
ouverture renforcee de metal, une cheminee carree et un foyer bleu-violet aux braises orangees ; a l'avant, une grande
enclume sur un socle grave de runes ; a l'arriere, un cristal source d'energie relie au four et au socle par des
conduits lumineux ; a droite, un etabli en bois sombre avec marteau, pinces, lingots et rangements.
Le bloc fonctionnel (ouvre l'interface de la forge) est le socle de l'enclume."""
import entity_kit as ek
from structure_common import (PIERRE, ARGENT, RUNE, CRISTAL_B, BOIS, FER, FLAMME, BRAISE, Masonry, silver, crystal,
                              rune_panel, trim, floor_lines, decal, base_panels)
from entity_kit import Mat

KEY = 'forge_arcanique'
TITLE = 'Forge arcanique'
ENTITY_CLASS = 'ForgeArcanique'
JAVA = 'ForgeArcanique'
VISIBLE_BOX = (5, 4.5, 0)
VIEW_SCALE = 5.0
SHEET_SCALE = 3.2
GIF_SCALE = 3.0
SHEET_YAW = 35
NO_COLLISION = ('cristal_source', 'flammes', 'braises', 'outils', 'conduits')
STATES = {'inactif': 'eteinte', 'activation': 'allumage', 'actif': 'active'}

STONE = Masonry(PIERRE, 0.42, decos=[base_panels()])
STONE_TOP = Masonry(PIERRE, 0.5, seed=1)
WALL = Masonry(PIERRE, 0.4, seed=4)
DARK = Masonry(PIERRE, 0.3, bw=6, bh=3, seed=2)
SOOT = Masonry(PIERRE, 0.16, var=0.08, seed=5)
SILVER = silver()
SILVER_RIV = silver(rivet=True)
IRON = Mat(FER, level=0.5, noise=0.18, scale=1.5, grad=0.3, up=0.3, down=-0.2, rim=0.15)
WOOD = Mat(BOIS, level=0.5, noise=0.3, scale=(4, 1), grad=0.1, decos=[lambda f: [f.px(x, y, BOIS[0]) for y in range(f.h)
                                                                                  for x in range(f.w) if f.side and x % 5 == 4]])
CRYS = crystal(CRISTAL_B, level=0.6)
CONDUIT = Mat(RUNE, level=0.55, noise=0.25, scale=1, grad=0.2, glow=True)

SOL = [(-4, 8, -4, 26), (4, 33, 8, 33), (-12, 30, -40, 30)]


def flame(f):
    """Langues de flamme bleu-violet (decoupees), plus claires au coeur."""
    import math
    if f.name not in ('north', 'south', 'east', 'west'):
        return
    for x in range(f.w):
        top = int(f.h * (0.55 + 0.45 * abs(math.sin(x * 1.3 + f.w))))
        for y in range(f.h):
            hgt = f.h - 1 - y
            if hgt < top:
                k = min(len(FLAMME) - 1, int((1 - hgt / max(1, top)) * (len(FLAMME) - 1) + 0.6))
                if hgt < 2:
                    k = max(k, 3)
                f.px(x, y, FLAMME[k], glow=True)


def coals(f):
    for y in range(f.h):
        for x in range(f.w):
            r = f.rng.random()
            col = BRAISE[3] if r < 0.12 else BRAISE[2] if r < 0.3 else BRAISE[1] if r < 0.5 else PIERRE[1]
            f.px(x, y, col, glow=r < 0.5)


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))

    p = m.bone('plateforme', (0, 0, 0), 'racine')
    m.cube(p, 'dalle', (-40, 0, -16), (80, 4, 56), STONE, deco={'up': [floor_lines(SOL)]})
    m.cube(p, 'marche', (-22, 0, -24), (44, 2, 8), STONE_TOP)
    m.cube(p, 'borne_avant_gauche', (32, 0, -18), (8, 10, 8), DARK, share='borne', deco={'sides': [rune_panel(1, margin=1), trim('lrt')]})
    m.cube(p, 'borne_avant_droite', (-40, 0, -18), (8, 10, 8), DARK, share='borne', deco={'sides': [rune_panel(1, margin=1), trim('lrt')]})

    f4 = m.bone('four', (24, 4, 24), 'racine')
    m.cube(f4, 'four_soubassement', (8, 4, 8), (32, 6, 30), STONE_TOP, deco={'sides': [trim('t')]})
    m.cube(f4, 'four_mur_fond', (8, 10, 30), (32, 30, 8), WALL)
    m.cube(f4, 'four_mur_gauche', (32, 10, 8), (8, 30, 22), WALL,
           deco={'north': [lambda f: [f.px(x, y, FER[3]) for x in (1, 3, 5) for y in range(3, 12)]]})
    m.cube(f4, 'four_mur_droit', (8, 10, 8), (6, 30, 22), WALL)
    m.cube(f4, 'four_linteau', (14, 34, 8), (18, 6, 4), WALL)
    m.cube(f4, 'four_dessus', (8, 40, 8), (32, 6, 30), STONE_TOP, deco={'sides': [trim('tb')]})
    m.cube(f4, 'four_fond_suie', (14, 10, 29), (18, 24, 1), SOOT)
    m.cube(f4, 'cadre_gauche', (31, 10, 6), (3, 26, 3), SILVER_RIV, share='cadre_four')
    m.cube(f4, 'cadre_droit', (12, 10, 6), (3, 26, 3), SILVER_RIV, share='cadre_four')
    m.cube(f4, 'cadre_haut', (12, 33, 6), (22, 4, 3), SILVER_RIV)
    m.cube(f4, 'cadre_coin_gauche', (26, 29, 6.5), (5, 2, 2), SILVER, rot=(0, 0, -45), origin=(31, 33, 7.5), share='cadre_coin')
    m.cube(f4, 'cadre_coin_droit', (15, 29, 6.5), (5, 2, 2), SILVER, rot=(0, 0, 45), origin=(15, 33, 7.5), share='cadre_coin')
    m.cube(f4, 'four_grille', (14, 10, 12), (18, 2, 14), IRON)
    m.cube(f4, 'coffre', (25, 4, -9), (12, 10, 10), WOOD, deco={'sides': [trim('tb', FER[4])]})
    m.cube(f4, 'coffre_ferrure', (30, 8, -9.5), (2, 3, 1), IRON)

    ch = m.bone('cheminee', (25, 46, 23), 'racine')
    m.cube(ch, 'cheminee_conduit', (18, 46, 16), (14, 22, 14), WALL, deco={'north': [rune_panel(1, margin=3)], 'sides': [trim('lr')]})
    m.cube(ch, 'cheminee_couronne', (17, 68, 15), (16, 3, 16), SILVER_RIV,
           deco={'up': [lambda f: [f.px(x, y, PIERRE[0]) for y in range(3, f.h - 3) for x in range(3, f.w - 3)]]})

    fo = m.bone('foyer', (23, 12, 19), 'racine')
    br = m.bone('braises', (23, 12, 19), fo)
    m.cube(br, 'braises_lit', (15, 12, 13), (16, 2, 12), Mat(BRAISE, level=0.5, glow=True, decos=[coals]))
    for i, (x, z) in enumerate(((17, 15), (22, 19), (27, 16), (19, 22), (26, 22))):
        m.cube(br, f'braise_{i + 1}', (x, 13.5, z), (2, 2, 2), Mat(BRAISE, level=0.6, glow=True, decos=[coals]), share='braise')
    fl = m.bone('flammes', (23, 14, 19), fo)
    m.cube(fl, 'flammes_face', (15, 14, 19), (16, 16, 0), decal(flame))
    m.cube(fl, 'flammes_travers', (23, 14, 13), (0, 14, 12), decal(flame))

    en = m.bone('enclume', (0, 4, 0), 'racine')
    m.cube(en, 'socle_enclume', (-10, 4, -8), (20, 12, 16), DARK,
           deco={'north': [rune_panel(1, margin=2)], 'south': [rune_panel(1, margin=2)], 'sides': [trim('lr')]})
    m.cube(en, 'socle_couvercle', (-11, 16, -9), (22, 2, 18), SILVER_RIV)
    m.cube(en, 'enclume_pied', (-6, 18, -4), (12, 3, 8), IRON)
    m.cube(en, 'enclume_taille', (-3, 21, -2), (6, 3, 4), IRON)
    m.cube(en, 'enclume_table', (-8, 24, -4), (16, 5, 8), IRON)
    m.cube(en, 'enclume_bigorne', (8, 25, -3), (5, 3, 6), IRON)
    m.cube(en, 'enclume_bigorne_pointe', (13, 26, -2), (3, 1, 4), IRON)
    m.cube(en, 'enclume_talon', (-10, 25, -3), (2, 4, 6), IRON)

    cs = m.bone('cristal', (-4, 4, 33), 'racine')
    m.cube(cs, 'cristal_socle', (-12, 4, 26), (16, 6, 14), STONE_TOP, deco={'sides': [rune_panel(0, margin=1)]})
    for nom, (x, z) in {'montant_av_gauche': (1, 26), 'montant_av_droit': (-12, 26), 'montant_ar_gauche': (1, 37),
                        'montant_ar_droit': (-12, 37)}.items():
        m.cube(cs, f'cristal_{nom}', (x, 10, z), (3, 30, 3), SILVER_RIV, share='cristal_montant')
    m.cube(cs, 'cristal_chapeau', (-12, 40, 26), (16, 3, 14), SILVER_RIV)
    cc = m.bone('cristal_source', (-4, 22, 33), cs)
    m.cube(cc, 'cristal_source_coeur', (-7, 12, 30), (6, 20, 6), CRYS)
    m.cube(cc, 'cristal_source_pointe', (-6, 32, 31), (4, 5, 4), CRYS)
    m.cube(cc, 'cristal_source_base', (-6, 10, 31), (4, 2, 4), CRYS)

    co = m.bone('conduits', (0, 6, 20), 'racine')
    m.cube(co, 'conduit_four', (4, 26, 31), (4, 4, 4), CONDUIT)
    m.cube(co, 'conduit_four_bague', (6, 25, 30), (2, 6, 6), SILVER)
    m.cube(co, 'conduit_enclume', (-6, 4, 8), (4, 2, 18), CONDUIT)
    m.cube(co, 'conduit_enclume_regard', (-8, 4, 14), (8, 3, 4), SILVER_RIV)
    m.cube(co, 'conduit_etabli', (-14, 26, 31), (2, 3, 3), CONDUIT)

    et = m.bone('etabli', (-27, 4, 13), 'racine')
    m.cube(et, 'etabli_plateau', (-40, 18, 4), (26, 3, 18), WOOD)
    for nom, (x, z) in {'pied_av_gauche': (-17, 5), 'pied_av_droit': (-39, 5), 'pied_ar_gauche': (-17, 18),
                        'pied_ar_droit': (-39, 18)}.items():
        m.cube(et, f'etabli_{nom}', (x, 4, z), (3, 14, 3), WOOD, share='etabli_pied')
    m.cube(et, 'etabli_tablette', (-38, 8, 6), (22, 2, 14), WOOD)
    m.cube(et, 'rangement_1', (-36, 10, 9), (9, 8, 10), WOOD, share='rangement', deco={'sides': [trim('tb', FER[4])]})
    m.cube(et, 'rangement_2', (-26, 10, 9), (9, 8, 10), WOOD, share='rangement', deco={'sides': [trim('tb', FER[4])]})
    m.cube(et, 'mur_arriere', (-40, 4, 32), (26, 32, 8), WALL, deco={'north': [rune_panel(0, margin=3)], 'sides': [trim('lr')]})
    m.cube(et, 'mur_arriere_corniche', (-40, 36, 31), (27, 3, 9), SILVER_RIV)

    ou = m.bone('outils', (-27, 21, 13), 'racine')
    m.cube(ou, 'marteau_tete', (-36, 21, 8), (6, 3, 3), IRON)
    m.cube(ou, 'marteau_manche', (-30, 22, 9), (9, 1, 1), WOOD)
    m.cube(ou, 'pinces_branche_1', (-27, 21, 14), (8, 1, 1), IRON, rot=(0, 12, 0), origin=(-27, 21, 14.5), share='pince')
    m.cube(ou, 'pinces_branche_2', (-27, 21, 15), (8, 1, 1), IRON, rot=(0, -12, 0), origin=(-27, 21, 15.5), share='pince')
    m.cube(ou, 'lingot_argent_1', (-38, 21, 15), (5, 2, 3), SILVER, share='lingot')
    m.cube(ou, 'lingot_argent_2', (-37.5, 23, 15.5), (5, 2, 3), SILVER, share='lingot')
    m.cube(ou, 'lingot_arcanique', (-20, 21, 9), (5, 2, 3), CONDUIT)
    return m


COLORS = {'racine': 0, 'plateforme': 1, 'four': 2, 'cheminee': 2, 'foyer': 4, 'enclume': 5, 'cristal': 6,
          'conduits': 6, 'etabli': 3, 'outils': 7}


def animations(m):
    P = ek.key_poses
    L = 'linear'

    # --- eteinte (4 s) : braises qui couvent, flammes reduites, cristal qui respire
    a = m.anim('eteinte', 4.0, 'loop')
    a.scale('flammes', [(0, 0.6, 0.15, 0.6, L), (4.0, 0.6, 0.15, 0.6, L)])
    P(a, [(0, {}), (2.0, {'cristal_source': (0, 0.6, 0)}), (4.0, {})], 'position', bones=['cristal_source'])

    # --- allumage (1 s) : les flammes montent, le cristal s'eleve et s'illumine
    a = m.anim('allumage', 1.0, 'once')
    P(a, [(0, {'flammes': (0.6, 0.15, 0.6)}), (0.4, {'flammes': (1.1, 1.3, 1.1)}), (1.0, {'flammes': (1, 1, 1)})], 'scale',
      interp='ease', bones=['flammes'])
    P(a, [(0, {}), (1.0, {'cristal_source': (0, 2, 0)})], 'position', interp='ease', bones=['cristal_source'])

    # --- active (1,2 s, en boucle) : flammes vacillantes, cristal flottant qui tourne lentement
    a = m.anim('active', 1.2, 'loop')
    a.scale('flammes', [(0, 1, 1, 1), (0.3, 1.06, 1.18, 1.06), (0.6, 0.96, 0.92, 0.96), (0.9, 1.04, 1.12, 1.04), (1.2, 1, 1, 1)])
    a.rot('flammes', [(0, 0, 0, 0), (0.4, 0, 6, 0), (0.8, 0, -6, 0), (1.2, 0, 0, 0)])
    P(a, [(0, {'cristal_source': (0, 2, 0)}), (0.6, {'cristal_source': (0, 2.8, 0)}), (1.2, {'cristal_source': (0, 2, 0)})],
      'position', bones=['cristal_source'])
    a.rot('cristal_source', [(0, 0, 0, 0, L), (1.2, 0, 90, 0, L)])
