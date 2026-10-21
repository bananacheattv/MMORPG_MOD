# -*- coding: utf-8 -*-
"""Mannequin d'entrainement : poteau de bois plante dans un socle, torse de toile bourre de paille, traverse portant
deux bottes de paille, tete-sac cousue. Trois variantes de meme squelette :
  standard : paille et bois ;
  blinde   : plastron, epaulieres et casque d'acier (forte resistance physique) ;
  magique  : tunique violette, runes et cristal flottant lumineux (forte resistance magique).
Hauteur 32 px (2 blocs). Os communs a toutes les variantes : racine > socle, mannequin > corps, bras, tete (+ cristal)."""
import entity_kit as ek
from entity_kit import pal, Mat, stamp, scratches, rivets

KEY = 'mannequin_entrainement'
TITLE = "Mannequin d'entraînement"
ENTITY_CLASS = 'MannequinEntrainementEntity'
VISIBLE_BOX = (1.0, 2.0, 0)
VIEW_SCALE = 12.0
SHEET_SCALE = 9.0
GIF_SCALE = 8.0

P_TOILE = pal('#6b5236', '#85683f', '#9c7e4e', '#b39a64', '#c9b27c')
P_PAILLE = pal('#a47b1f', '#c79a2a', '#e0b83f', '#f0d466', '#fbe99a')
P_BOIS = pal('#3a2514', '#4e3320', '#684528', '#82592f', '#9b703e')
P_ACIER = pal('#2a3038', '#3e4650', '#566270', '#7a8898', '#a8b6c6', '#d6e0ec')
P_VIOLET = pal('#241338', '#35205a', '#4a2c82', '#6a3fb0', '#8c5ed8')
P_RUNE = pal('#1a6c9c', '#2aa0d8', '#58d0f8', '#a8f0ff', '#e8ffff')
P_CORDE = pal('#7a5f33', '#96774a', '#b2925a', '#c9aa70')
FIL = (46, 30, 20)


def planks(f):
    """Joints de planches : traits sombres espaces."""
    if f.name in ('up', 'down'):
        for x in range(0, f.w, 4):
            for y in range(f.h):
                f.px(x, y, P_BOIS[0])
    else:
        for y in range(3, f.h, 4):
            for x in range(f.w):
                f.px(x, y, P_BOIS[0])


def weave(f):
    """Trame de toile de jute : points plus sombres en quinconce."""
    for y in range(0, f.h, 2):
        for x in range(f.w):
            if (x + y) % 4 == 0:
                c = f.get(x, y)
                f.px(x, y, (max(0, c[0] - 18), max(0, c[1] - 18), max(0, c[2] - 18)))


def straw_strands(f):
    """Brins de paille : petits traits obliques clairs et sombres."""
    for _ in range(max(2, f.w * f.h // 5)):
        x, y = f.rng.randrange(f.w), f.rng.randrange(max(1, f.h - 1))
        col = f.rng.choice((P_PAILLE[1], P_PAILLE[4], P_PAILLE[0]))
        f.px(x, y, col)
        f.px(x + 1, y + 1, col)


def stitches(color):
    def d(f):
        if f.side:
            for y in range(1, f.h, 3):
                f.px(0, y, color)
                f.px(f.w - 1, y, color)
    return d


class Decal:
    """Matiere 'decalque' : tout transparent sauf les motifs."""

    def __init__(self, *decos):
        self.decos = decos

    def paint(self, f):
        for y in range(f.h):
            for x in range(f.w):
                f.clear(x, y)
        for d in self.decos:
            d(f)


def face(color, glow=False):
    rows = ['.......', 'K.K.K.K', '.K...K.', 'K.K.K.K', '.......', '.KKKKK.', '.K.K.K.']
    return stamp(rows, {'K': color}, glow='K' if glow else '')


def runes(color, n=3, glow=True):
    """Runes lumineuses : petits glyphes aleatoires."""
    glyphs = [['V.', 'V.', '.V'], ['VV.', '.V.'], ['V..', '.V.', 'V..'], ['.V', 'V.', 'V.'], ['V.V', '.V.']]

    def d(f):
        if f.w < 4 or f.h < 4:
            return
        for _ in range(n):
            g = f.rng.choice(glyphs)
            w, h = len(g[0]), len(g)
            if f.w < w + 1 or f.h < h + 1:
                continue
            x0, y0 = f.rng.randrange(0, f.w - w + 1), f.rng.randrange(0, f.h - h + 1)
            for j, row in enumerate(g):
                for i, ch in enumerate(row):
                    if ch == 'V':
                        f.px(x0 + i, y0 + j, color[(i + j) % 2 + 2], glow=glow)
    return d


def circle(f):
    """Cercle runique sur le dessus du socle (variante magique)."""
    if f.name != 'up':
        return
    cx, cy, r = f.w / 2 - 0.5, f.h / 2 - 0.5, min(f.w, f.h) / 2 - 1.2
    for y in range(f.h):
        for x in range(f.w):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if abs(d - r) < 0.7 or (d < r - 2 and abs(d - (r - 2.4)) < 0.5):
                f.px(x, y, P_RUNE[3], glow=True)


def build(variant='standard'):
    blinde = variant == 'blinde'
    magique = variant == 'magique'
    toile = P_VIOLET if magique else P_TOILE
    BOIS = Mat(P_BOIS, level=0.5, noise=0.3, scale=(3, 1), grad=0.15, decos=[planks, scratches(2, P_BOIS[0])])
    BOIS_SOCLE = Mat(P_BOIS, level=0.5, noise=0.3, scale=(3, 1), grad=0.15,
                     decos=[planks, scratches(3, P_BOIS[0]), circle] if magique else [planks, scratches(3, P_BOIS[0])])
    TOILE = Mat(toile, level=0.55, noise=0.25, scale=1.5, grad=0.2, decos=[weave, stitches(FIL)])
    PAILLE = Mat(P_PAILLE, level=0.55, noise=0.3, scale=1.5, grad=0.15, decos=[straw_strands])
    CORDE = Mat(P_CORDE, level=0.5, noise=0.3, scale=1)
    ACIER = Mat(P_ACIER, level=0.55, noise=0.2, scale=2, grad=0.25, decos=[rivets(P_ACIER[5], P_ACIER[0], inset=1, every=4), scratches(2, P_ACIER[1])])

    m = ek.Model(KEY if variant == 'standard' else f'{KEY}_{variant}', TITLE)
    m.bone('racine', (0, 0, 0))
    # ------------------------------------------------------------ socle (immobile)
    s = m.bone('socle', (0, 0, 0), 'racine')
    m.cube(s, 'socle_base', (-8, 0, -8), (16, 2, 16), BOIS_SOCLE)
    m.cube(s, 'socle_anneau', (-6, 2, -6), (12, 1, 12), BOIS_SOCLE)
    for i, (x, z) in enumerate(((-8, -8), (4, -8), (-8, 4), (4, 4))):      # quatre sacs de sable aux coins
        m.cube(s, f'sac_{i + 1}', (x, 2, z), (4, 3, 4), TOILE)
    # ------------------------------------------------------------ mannequin (pivote autour de la base du poteau)
    mq = m.bone('mannequin', (0, 3, 0), 'racine')
    m.cube(mq, 'poteau', (-1.5, 3, -1.5), (3, 14, 3), BOIS)
    co = m.bone('corps', (0, 17, 0), mq)
    m.cube(co, 'torse', (-4, 13, -2.5), (8, 10, 5), TOILE)
    m.cube(co, 'ceinture', (-4.5, 16, -3), (9, 1, 6), CORDE)
    m.cube(co, 'bourrage_bas', (-3.5, 12, -2), (7, 1, 4), PAILLE)
    if blinde:
        m.cube(co, 'plastron', (-5, 14, -3.5), (10, 8, 7), ACIER)
        m.cube(co, 'plastron_bord', (-5, 13, -3.5), (10, 1, 7), Mat(P_ACIER, level=0.3, noise=0.1))
    if magique:
        m.cube(co, 'runes_torse', (-4, 14, -2.55), (8, 7, 0), Decal(runes(P_RUNE, 3)))
        m.cube(co, 'echarpe', (-4.5, 21, -3), (9, 2, 6), Mat(P_VIOLET, level=0.8, noise=0.2))
    br = m.bone('bras', (0, 21, 0), mq)
    m.cube(br, 'traverse', (-10, 20, -1), (20, 2, 2), BOIS)
    for side, x in (('droit', -13), ('gauche', 10)):
        m.cube(br, f'botte_{side}', (x, 18, -1.5), (3, 5, 3), PAILLE)
        m.cube(br, f'lien_{side}', (x + (2 if side == 'droit' else -1), 19, -1.5), (2, 1, 3), CORDE)
    if blinde:
        for side, x in (('droit', -8), ('gauche', 4)):
            m.cube(br, f'epauliere_{side}', (x, 21, -2.5), (4, 3, 5), ACIER)
    t = m.bone('tete', (0, 23, 0), mq)
    m.cube(t, 'sac_tete', (-3.5, 23, -3.5), (7, 7, 7), TOILE,
           deco={'north': [face((120, 240, 255) if magique else FIL, glow=magique)]})
    m.cube(t, 'touffe_haut', (-2, 30, -2), (4, 2, 4), PAILLE)
    m.cube(t, 'touffe_gauche', (3.5, 25, -1), (1, 3, 2), PAILLE)
    m.cube(t, 'touffe_droite', (-4.5, 25, -1), (1, 3, 2), PAILLE)
    m.cube(t, 'nouet', (-1.5, 22, -1.5), (3, 1, 3), CORDE)
    if blinde:
        m.cube(t, 'casque', (-4, 25, -4), (8, 5, 8), ACIER)
        m.cube(t, 'casque_cimier', (-0.5, 30, -4), (1, 2, 8), ACIER)
        m.cube(t, 'visiere', (-3, 26, -4.6), (6, 1, 1), Mat(P_ACIER, level=0.05, noise=0))
    if magique:
        cr = m.bone('cristal', (0, 35, 0), mq)
        m.cube(cr, 'cristal_corps', (-1.5, 33, -1.5), (3, 5, 3), Mat(P_RUNE, level=0.7, noise=0.2, grad=0.3, glow=True),
               rot=(0, 45, 0), origin=(0, 35, 0))
        m.cube(cr, 'cristal_pointe', (-0.5, 38, -0.5), (1, 2, 1), Mat(P_RUNE, level=0.9, noise=0.1, glow=True),
               rot=(0, 45, 0), origin=(0, 35, 0))
    return m


COLORS = {'racine': 0, 'socle': 1, 'mannequin': 2, 'corps': 3, 'bras': 4, 'tete': 5, 'cristal': 6}


def animations(m):
    P = ek.key_poses
    has_cristal = 'cristal' in m.bones

    # --- idle (3 s) : oscillation tres lente du mannequin, tete et bras qui pendent un peu
    a = m.anim('idle', 3.0, 'loop')
    P(a, [(0, {}), (0.75, {'mannequin': (0.5, 0, 0.7), 'tete': (0.8, 1.5, 0), 'bras': (0, 0, 0.6)}),
          (1.5, {'mannequin': (0, 0, 0)}),
          (2.25, {'mannequin': (-0.5, 0, -0.7), 'tete': (-0.8, -1.5, 0), 'bras': (0, 0, -0.6)}),
          (3.0, {})])
    if has_cristal:
        a.pos('cristal', [(0, 0, 0, 0), (0.75, 0, 0.8, 0), (1.5, 0, 0, 0), (2.25, 0, -0.8, 0), (3.0, 0, 0, 0)])
        a.rot('cristal', [(0, 0, 0, 0), (1.0, 0, 120, 0), (2.0, 0, 240, 0), (3.0, 0, 360, 0)], 'linear')

    # --- hit (1 s) : coup recu de face, oscillation amortie (le jeu l'oriente selon la direction et la force du coup)
    a = m.anim('hit', 1.0, 'once')
    amp = [(0.0, 0), (0.08, 9), (0.2, -5.5), (0.32, 3.2), (0.46, -1.8), (0.62, 0.9), (0.8, -0.3), (1.0, 0)]
    P(a, [(t, {'mannequin': (v, 0, 0), 'tete': (-v * 0.6, 0, 0), 'bras': (0, 0, v * 0.5)}) for t, v in amp], interp='catmullrom')

    # --- reset (1,2 s) : tour complet du mannequin avec petit rebond, la paille se remet en place
    a = m.anim('reset', 1.2, 'once')
    a.rot('mannequin', [(0, 0, 0, 0), (0.5, 0, 200, 0), (1.0, 0, 360, 0), (1.2, 0, 360, 0)], 'catmullrom')
    a.scale('corps', [(0, 1, 1, 1), (0.3, 0.96, 1.05, 0.96), (0.6, 1.03, 0.97, 1.03), (0.9, 0.99, 1.01, 0.99), (1.2, 1, 1, 1)])
