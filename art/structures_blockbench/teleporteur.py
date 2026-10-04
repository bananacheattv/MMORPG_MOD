# -*- coding: utf-8 -*-
"""Teleporteur : grande arche rectangulaire en pierre sombre aux angles en escalier, renforts argentes, runes violettes
sur les deux montants, medaillons a gemme ; grand cristal au-dessus du passage ; plateforme a deux marches parcourue
de lignes d'energie qui rejoignent l'arche ; a droite (vu de face), un pupitre d'activation a tablette inclinee et
petit cristal. Passage libre de 2,25 blocs de large sur 3 blocs de haut. La surface du portail (groupe 'portail')
est separee du cadre : le jeu ne l'affiche que lorsque le teleporteur est actif.
Le bloc fonctionnel est le sol du passage, sous le portail."""
import entity_kit as ek
from structure_common import (PIERRE, ARGENT, RUNE, CRISTAL_B, CRISTAL_V, PORTAIL, Masonry, silver, crystal, rune_panel,
                              trim, floor_lines, stripes_x, decal, base_panels)

KEY = 'teleporteur'
TITLE = 'Téléporteur'
ENTITY_CLASS = 'Teleporteur'
JAVA = 'Teleporteur'
VISIBLE_BOX = (6, 5, 0)
VIEW_SCALE = 5.0
SHEET_SCALE = 3.2
GIF_SCALE = 3.0
SHEET_YAW = 30
NO_COLLISION = ('cristal', 'portail')
STATES = {'inactif': 'inactif', 'activation': 'activation', 'actif': 'actif'}

STONE = Masonry(PIERRE, 0.42, decos=[base_panels()])
STONE_TOP = Masonry(PIERRE, 0.5, seed=1)
DARK = Masonry(PIERRE, 0.33, bw=6, bh=3, seed=2)
SILVER = silver()
SILVER_RIV = silver(rivet=True)
CRYS = crystal(CRISTAL_B, level=0.6)
CRYS_V = crystal(CRISTAL_V)

# lignes d'energie : seuil du portail, liaisons vers l'avant et vers le pupitre
SEGMENTS = [(-15, -9, 15, -9), (-15, -9, -15, -16), (15, -9, 15, -16), (-15, 12, 15, 12),
            (-15, 12, -15, -9), (15, 12, 15, -9), (-36, -12, -40, -12), (-18, 2, -40, 2)]


def swirl(f):
    """Surface du portail : volutes bleu-violet lumineuses, semi-transparentes."""
    if f.name not in ('north', 'south'):
        return
    import math
    for y in range(f.h):
        for x in range(f.w):
            wx, wy, _ = f.world(x, y)
            v = (math.sin(wx * 0.35 + wy * 0.18) + math.sin(wy * 0.27 - wx * 0.12 + 1.7) + math.sin((wx + wy) * 0.09)) / 3
            k = int(max(0, min(len(PORTAIL) - 1, (v + 1) / 2 * (len(PORTAIL) - 1) + f.rng.random() * 0.6)))
            f.px(x, y, PORTAIL[k], glow=True, a=205)
    for _ in range(int(f.w * f.h / 60)):
        x, y = f.rng.randrange(f.w), f.rng.randrange(f.h)
        f.px(x, y, PORTAIL[-1], glow=True, a=235)


def montant(m, b, nom, x0, sens):
    """Montant de l'arche : base, fut avec runes et medaillon, chapiteau. x0 = bord exterieur, sens = +1 / -1."""
    def X(a, w):     # cube de largeur w commencant a 'a' px du bord exterieur, vers l'interieur
        return x0 - a - w if sens > 0 else x0 + a
    m.cube(b, f'{nom}_base', (X(0, 20), 6, -8), (20, 10, 18), STONE_TOP, share='montant_base',
           deco={'sides': [trim('t')]})
    m.cube(b, f'{nom}_fut', (X(2, 16), 16, -6), (16, 34, 14), DARK, share='montant_fut',
           deco={'north': [rune_panel(3, margin=2), trim('lr')], 'south': [rune_panel(3, margin=2), trim('lr')],
                 'east': [trim('lr')], 'west': [trim('lr')]})
    m.cube(b, f'{nom}_bague_basse', (X(1, 18), 22, -7), (18, 2, 16), SILVER, share='montant_bague')
    m.cube(b, f'{nom}_bague_haute', (X(1, 18), 43, -7), (18, 2, 16), SILVER, share='montant_bague')
    m.cube(b, f'{nom}_medaillon', (X(5, 10), 29, -8), (10, 10, 2), SILVER_RIV, share='medaillon',
           deco={'north': [lambda f: [f.px(x, y, PIERRE[0]) for y in range(2, 8) for x in range(2, 8)]]})
    m.cube(b, f'{nom}_gemme', (X(7, 6), 31, -9), (6, 6, 1), CRYS_V, share='gemme')
    m.cube(b, f'{nom}_chapiteau', (X(0, 20), 50, -8), (20, 6, 18), STONE_TOP, share='montant_chapiteau',
           deco={'sides': [trim('tb')]})


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))

    p = m.bone('plateforme', (0, 0, 0), 'racine')
    m.cube(p, 'dalle', (-40, 0, -16), (80, 6, 40), STONE,
           deco={'up': [floor_lines(SEGMENTS)], 'north': [stripes_x([-17, 15, -31, 29], faces=('north',))]})
    m.cube(p, 'marche_1', (-36, 0, -24), (72, 2, 4), STONE_TOP, deco={'*': [stripes_x([-17, 15, -31, 29])]})
    m.cube(p, 'marche_2', (-38, 0, -20), (76, 4, 4), STONE_TOP, deco={'*': [stripes_x([-17, 15, -31, 29])]})
    m.cube(p, 'extension_pupitre', (-56, 0, -14), (16, 4, 24), STONE_TOP, deco={'up': [floor_lines(SEGMENTS)]})
    m.cube(p, 'bordure_gauche', (38, 6, -16), (2, 2, 40), SILVER, share='bordure')
    m.cube(p, 'bordure_droite', (-40, 6, -16), (2, 2, 40), SILVER, share='bordure')

    a = m.bone('arche', (0, 6, 0), 'racine')
    mg = m.bone('montant_gauche', (27, 6, 0), a)
    montant(m, mg, 'montant_gauche', 37, +1)
    md = m.bone('montant_droit', (-27, 6, 0), a)
    montant(m, md, 'montant_droit', -37, -1)
    li = m.bone('linteau', (0, 56, 0), a)
    m.cube(li, 'linteau_bande', (-37, 54, -9), (74, 2, 20), SILVER_RIV)
    m.cube(li, 'linteau_bloc', (-36, 56, -8), (72, 8, 18), DARK, deco={'north': [trim('tb')], 'south': [trim('tb')]})
    m.cube(li, 'linteau_rune_gauche', (14, 57, -8.5), (14, 6, 1), DARK, share='linteau_rune',
           deco={'north': [rune_panel(3, margin=0, frame=False)]})
    m.cube(li, 'linteau_rune_droite', (-28, 57, -8.5), (14, 6, 1), DARK, share='linteau_rune',
           deco={'north': [rune_panel(3, margin=0, frame=False)]})
    for nom, x in (('gradin_gauche', 22), ('gradin_droit', -36)):
        m.cube(li, f'{nom}_1', (x, 64, -7), (14, 6, 16), STONE_TOP, share='gradin_1', deco={'sides': [trim('t')]})
    for nom, x in (('gradin_gauche', 25), ('gradin_droit', -35)):
        m.cube(li, f'{nom}_2', (x, 70, -5), (10, 4, 12), SILVER_RIV, share='gradin_2')
    m.cube(li, 'clef_de_voute', (-10, 64, -6), (20, 4, 14), STONE_TOP, deco={'sides': [trim('t')]})

    c = m.bone('cristal', (0, 66, -10), 'racine')
    m.cube(c, 'cadre_bas', (-7, 55, -12), (14, 3, 3), SILVER)
    m.cube(c, 'cadre_gauche', (6, 57, -12), (3, 16, 3), SILVER, share='cadre_cote')
    m.cube(c, 'cadre_droit', (-9, 57, -12), (3, 16, 3), SILVER, share='cadre_cote')
    m.cube(c, 'cadre_haut', (-6, 72, -12), (12, 3, 3), SILVER)
    m.cube(c, 'cadre_pointe', (-2, 75, -12), (4, 3, 3), SILVER)
    cc = m.bone('cristal_arche', (0, 65, -13), c)
    m.cube(cc, 'cristal_arche_coeur', (-4, 59, -15), (8, 12, 4), CRYS)
    m.cube(cc, 'cristal_arche_haut', (-2, 71, -14), (4, 3, 2), CRYS, share='cristal_arche_bout')
    m.cube(cc, 'cristal_arche_bas', (-2, 56, -14), (4, 3, 2), CRYS, share='cristal_arche_bout')

    pt = m.bone('portail', (0, 30, 1), 'racine')
    m.cube(pt, 'portail_surface', (-18, 6, 1), (36, 48, 0), decal(swirl))

    pu = m.bone('pupitre', (-48, 4, -4), 'racine')
    m.cube(pu, 'pupitre_socle', (-54, 4, -10), (12, 13, 12), DARK, deco={'north': [stripes_x([-49], faces=('north',))],
                                                                          'sides': [trim('lr')]})
    m.cube(pu, 'pupitre_ceinture', (-55, 15, -11), (14, 2, 14), SILVER)
    m.cube(pu, 'pupitre_tablette', (-55, 17, -11), (14, 2, 12), SILVER_RIV, rot=(-25, 0, 0), origin=(-48, 17, -5),
           deco={'up': [lambda f: rune_panel(1, faces=('up',), margin=2)(f)]})
    pc = m.bone('cristal_pupitre', (-48, 21, 1), pu)
    m.cube(pc, 'cristal_pupitre_socle', (-50, 17, -1), (4, 3, 4), SILVER)
    m.cube(pc, 'cristal_pupitre_coeur', (-49, 20, 0), (2, 5, 2), CRYS)
    m.cube(pc, 'cristal_pupitre_pointe', (-48.5, 25, 0.5), (1, 2, 1), CRYS)
    return m


COLORS = {'racine': 0, 'plateforme': 1, 'arche': 2, 'montant_gauche': 3, 'montant_droit': 3, 'linteau': 4,
          'cristal': 6, 'portail': 5, 'pupitre': 7}


def animations(m):
    P = ek.key_poses
    L = 'linear'
    cristaux = ['cristal_arche', 'cristal_pupitre']

    # --- inactif (3 s) : portail ferme (ecrase), cristaux qui respirent
    a = m.anim('inactif', 3.0, 'loop')
    P(a, [(0, {}), (1.5, {'cristal_arche': (0, 0.5, 0), 'cristal_pupitre': (0, 0.3, 0)}), (3.0, {})], 'position', bones=cristaux)
    a.scale('portail', [(0, 1, 0.02, 1, L), (3.0, 1, 0.02, 1, L)])

    # --- activation (1,2 s) : le portail s'ouvre du centre vers les bords, le cristal s'avance
    a = m.anim('activation', 1.2, 'once')
    P(a, [(0, {'portail': (1, 0.02, 1)}), (0.3, {'portail': (0.3, 0.1, 1)}), (1.2, {'portail': (1, 1, 1)})], 'scale',
      interp='ease', bones=['portail'])
    P(a, [(0, {}), (1.2, {'cristal_arche': (0, 1, -1), 'cristal_pupitre': (0, 1.5, 0)})], 'position', interp='ease', bones=cristaux)
    P(a, [(0, {}), (1.2, {'cristal_pupitre': (0, 180, 0)})], 'rotation', interp='ease', bones=['cristal_pupitre'])

    # --- actif (2 s, en boucle) : le portail ondule, le grand cristal flotte, le petit tourne
    a = m.anim('actif', 2.0, 'loop')
    a.scale('portail', [(0, 1, 1, 1), (0.5, 1.02, 0.99, 1), (1.0, 1, 1, 1), (1.5, 0.99, 1.01, 1), (2.0, 1, 1, 1)])
    P(a, [(0, {'cristal_arche': (0, 1, -1), 'cristal_pupitre': (0, 1.5, 0)}),
          (1.0, {'cristal_arche': (0, 2, -1), 'cristal_pupitre': (0, 2.2, 0)}),
          (2.0, {'cristal_arche': (0, 1, -1), 'cristal_pupitre': (0, 1.5, 0)})], 'position', bones=cristaux)
    a.rot('cristal_pupitre', [(0, 0, 180, 0, L), (2.0, 0, 540, 0, L)])
