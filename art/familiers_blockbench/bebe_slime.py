# -*- coding: utf-8 -*-
"""Bebe slime : petit cube gelatineux (8 px = 0,5 bloc ; echelle de rendu 0,8 pour 0,4 bloc), enveloppe translucide, noyau plus dense, visage
(grands yeux carres et petit sourire) visible a travers l'enveloppe. Aucun membre.
Variante 'opaque' : enveloppe sans pixels semi-transparents (aretes et coins pleins, faces decoupees),
pour un rendu en 'cutout' si la translucidite pose probleme en jeu."""
import entity_kit as ek
from entity_kit import pal, Mat, stamp

KEY = 'bebe_slime'
TITLE = 'Bebe slime'
ENTITY_CLASS = 'BebeSlimeEntity'
VISIBLE_BOX = (0.5, 0.5, 0)
VIEW_SCALE = 30.0
SHEET_SCALE = 18.0
GIF_SCALE = 16.0
VARIANTES = ['opaque']

P_ENV = pal('#5c9a1c', '#77bd27', '#93dc36', '#b2f055', '#d3ff8c', '#efffc8')
P_NOYAU = pal('#3a7614', '#4a9119', '#5cab20', '#70c22a', '#88d63a')
K = (24, 46, 14)
W = (214, 255, 160)


class ShellMat:
    """Enveloppe : faces translucides, aretes et coins plus clairs et plus opaques.
    opaque=True : aucun pixel semi-transparent (faces decoupees, aretes pleines)."""

    def __init__(self, opaque=False):
        self.opaque = opaque

    def paint(self, f):
        for y in range(f.h):
            for x in range(f.w):
                edge = x in (0, f.w - 1) or y in (0, f.h - 1)
                corner = (x in (0, 1, f.w - 2, f.w - 1)) and (y in (0, 1, f.h - 2, f.h - 1))
                if corner:
                    f.px(x, y, P_ENV[5] if (x in (0, f.w - 1) or y in (0, f.h - 1)) else P_ENV[4], a=255 if self.opaque else 235)
                elif edge:
                    f.px(x, y, P_ENV[3], a=255 if self.opaque else 190)
                elif self.opaque:
                    if (x + y) % 5 == 0 and f.rng.random() < 0.4:
                        f.px(x, y, P_ENV[4], a=255)          # quelques reflets pleins
                    else:
                        f.clear(x, y)
                else:
                    c = P_ENV[2] if (x * 3 + y * 5) % 7 else P_ENV[3]
                    f.px(x, y, c, a=120)


class Decal:
    def __init__(self, *decos):
        self.decos = decos

    def paint(self, f):
        for y in range(f.h):
            for x in range(f.w):
                f.clear(x, y)
        for d in self.decos:
            d(f)


CORE = Mat(P_NOYAU, level=0.55, noise=0.3, scale=1.5, grad=0.2)
FACE = Decal(lambda f: stamp(['......', 'KK..KK', 'Kw..Kw', '......', '.K..K.', '..KK..'], {'K': K, 'w': W})(f) if f.name == 'north' else None)


def build(variant=None):
    m = ek.Model(KEY if variant is None else f'{KEY}_{variant}', TITLE)
    m.bone('racine', (0, 0, 0))
    e = m.bone('enveloppe', (0, 0, 0), 'racine')
    m.cube(e, 'enveloppe_cube', (-4, 0, -4), (8, 8, 8), ShellMat(opaque=variant == 'opaque'))
    n = m.bone('noyau', (0, 0.5, 0), 'racine')
    m.cube(n, 'noyau_cube', (-3, 0.5, -3), (6, 6, 6), CORE)
    v = m.bone('visage', (0, 3.5, -3.1), n)
    m.cube(v, 'visage_plan', (-3, 0.5, -3.1), (6, 6, 0), FACE)
    return m


COLORS = {'racine': 0, 'enveloppe': 4, 'noyau': 1, 'visage': 2}


def animations(m):
    P = ek.key_poses
    S = lambda x, y: (x, y, x)
    BS = ['enveloppe', 'noyau']

    # --- repos (2 s) : leger tremblement gelatineux, le noyau suit avec un temps de retard
    a = m.anim('repos', 2.0, 'loop')
    P(a, [(0, {}), (0.5, {'enveloppe': S(1.04, 0.95), 'noyau': S(0.99, 1.02)}), (1.0, {'enveloppe': S(0.97, 1.04), 'noyau': S(1.03, 0.96)}),
          (1.5, {'enveloppe': S(1.03, 0.97), 'noyau': S(0.98, 1.03)}), (2.0, {})], 'scale', bones=BS)

    # --- preparation du saut (0,4 s, figee) : se tasse
    a = m.anim('preparation_saut', 0.4, 'hold')
    P(a, [(0, {}), (0.3, {'enveloppe': S(1.22, 0.7), 'noyau': S(1.16, 0.76)}), (0.4, {'enveloppe': S(1.2, 0.72), 'noyau': S(1.15, 0.77)})], 'scale', bones=BS)
    P(a, [(0, {}), (0.3, {'visage': (0, -0.6, 0)}), (0.4, {'visage': (0, -0.6, 0)})], 'position', bones=['visage'])

    # --- saut (0,7 s) : part tasse, s'etire en montant, retombe etire (bond visuel ; a retirer si le jeu deplace l'entite)
    a = m.anim('saut', 0.7, 'once')
    P(a, [(0, {'enveloppe': S(1.2, 0.72), 'noyau': S(1.15, 0.77)}), (0.1, {'enveloppe': S(0.84, 1.26), 'noyau': S(0.9, 1.15)}, 'linear'),
          (0.32, {'enveloppe': S(0.96, 1.06), 'noyau': S(0.94, 1.1)}), (0.6, {'enveloppe': S(0.9, 1.14), 'noyau': S(0.95, 1.08)}),
          (0.7, {'enveloppe': S(0.92, 1.1), 'noyau': S(0.95, 1.06)})], 'scale', bones=BS)
    P(a, [(0, {}), (0.1, {'racine': (0, 1.2, 0)}, 'linear'), (0.32, {'racine': (0, 5, 0)}), (0.6, {'racine': (0, 1, 0)}), (0.7, {})],
      'position', bones=['racine'])

    # --- reception (0,6 s) : s'ecrase a l'impact puis rebondit en s'amortissant
    a = m.anim('reception', 0.6, 'once')
    P(a, [(0, {'enveloppe': S(1.3, 0.6), 'noyau': S(1.2, 0.7)}), (0.15, {'enveloppe': S(0.9, 1.15), 'noyau': S(1.1, 0.85)}),
          (0.3, {'enveloppe': S(1.07, 0.93), 'noyau': S(0.94, 1.08)}), (0.45, {'enveloppe': S(0.98, 1.02), 'noyau': S(1.03, 0.97)}), (0.6, {})],
      'scale', bones=BS)

    # --- double rebond joyeux (1,4 s) : deux petits bonds, yeux plisses de joie
    a = m.anim('double_rebond', 1.4, 'once')
    P(a, [(0, {}), (0.12, {'enveloppe': S(1.18, 0.75), 'noyau': S(1.12, 0.8)}), (0.22, {'enveloppe': S(0.88, 1.18)}, 'linear'),
          (0.38, {'enveloppe': S(0.98, 1.04)}), (0.54, {'enveloppe': S(1.25, 0.68), 'noyau': S(1.15, 0.78)}),
          (0.64, {'enveloppe': S(0.9, 1.15)}, 'linear'), (0.78, {'enveloppe': S(0.98, 1.03)}), (0.94, {'enveloppe': S(1.22, 0.72), 'noyau': S(1.12, 0.8)}),
          (1.1, {'enveloppe': S(0.96, 1.05)}), (1.4, {})], 'scale', bones=BS)
    a.scale('visage', [(0, 1, 1, 1), (0.12, 1, 0.5, 1), (1.1, 1, 0.5, 1), (1.4, 1, 1, 1)])
    P(a, [(0, {}), (0.22, {'racine': (0, 1, 0)}, 'linear'), (0.38, {'racine': (0, 3.2, 0)}), (0.54, {}), (0.64, {'racine': (0, 0.8, 0)}, 'linear'),
          (0.78, {'racine': (0, 2.2, 0)}), (0.94, {}), (1.4, {})], 'position', bones=['racine'])

    # --- repos aplati (2,4 s) : etale au sol, respiration lente
    a = m.anim('repos_aplati', 2.4, 'loop')
    P(a, [(0, {'enveloppe': S(1.18, 0.72), 'noyau': S(1.12, 0.76)}), (1.2, {'enveloppe': S(1.15, 0.77), 'noyau': S(1.1, 0.8)}),
          (2.4, {'enveloppe': S(1.18, 0.72), 'noyau': S(1.12, 0.76)})], 'scale', bones=BS)
    a.scale('visage', [(0, 1, 0.8, 1), (1.2, 1, 0.85, 1), (2.4, 1, 0.8, 1)])
