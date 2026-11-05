"""30 decors 3D de la citadelle en .bbmodel (sources dans bbmodels/deco_*.bbmodel), puis integration au mod.

    python tools/gen_decor_bb.py [id ...] [--preview DOSSIER]

ECHELLE : la citadelle est surdimensionnee, 1 bloc de citadelle = 3 blocs Minecraft = 48 unites de modele
(16 unites = 1 bloc Minecraft). Un lampadaire fait donc ~5 blocs, une statue ~7, une colonne ~8.
Repere Blockbench : x largeur, y hauteur (0 = sol), z profondeur, face avant vers -z. Origine au centre du bloc.

Ecrit : bbmodels/<id>.bbmodel (editable dans Blockbench, texture integree)
        assets/mmorpg/bbmodels/props/<id>.json + textures/imported/props/<id>.png   (lus par ImportedModel)
        assets/mmorpg/{items,models/item,textures/item,blockstates}, data/.../loot_table/blocks, lang fr/en
        src/main/java/com/mmorpg/registry/DecorCitadelList.java (liste des blocs, collisions, lumiere)
"""
import json
import math
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import bbmount_lib as L
from bbmount_lib import rgb, vnoise, fbm, lerp

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/mmorpg'
G = 'root'
CITADEL_BLOCK = 48          # unites de modele pour 1 bloc de citadelle


# ------------------------------------------------------------------ materiaux supplementaires
class DShader(L.Shader):
    FLAT = {'flame', 'glowc', 'water'}

    def color(self, mat, P, N, w, h, face):
        fn = getattr(self, 's_' + mat[0], None)
        if fn is None:
            return super().color(mat, P, N, w, h, face)
        return fn(P, *mat[1:]), mat[0] in self.FLAT

    def s_marble(self, P, color='#e8e4da'):
        c = rgb(color)
        n = fbm(P, (.07, .07, .07), self.seed)
        vein = np.abs(np.sin((P[..., 0] * .5 + P[..., 2] * .35 + P[..., 1] * .2) + n * 7)) < .07
        img = c * (.9 + .16 * n[..., None])
        return np.where(vein[..., None], img * .72, img)

    def s_stone(self, P, color='#8c8f96'):
        c = rgb(color)
        row = np.floor(P[..., 1] / 6.0)
        col = np.floor((P[..., 0] + P[..., 2] + (row % 2) * 6) / 12.0)
        fy = P[..., 1] / 6.0 - row
        fx = ((P[..., 0] + P[..., 2] + (row % 2) * 6) / 12.0) - col
        mortar = (fy < .07) | (fx < .04)
        k = np.stack([row, col], -1)
        tone = .85 + .3 * (((row * 7 + col * 13) % 5) / 5.0)
        img = c * tone[..., None] * (.88 + .24 * fbm(P, (.3, .3, .3), self.seed)[..., None])
        return np.where(mortar[..., None], c * .55, img)

    def s_wood(self, P, color='#6b4528'):
        c = rgb(color)
        g = np.sin((P[..., 0] + P[..., 2]) * 2.2 + vnoise(P, (.3, .05, .3), self.seed) * 6)
        plank = (np.abs(np.sin(P[..., 1] * .5)) < .05)[..., None]
        img = c * (.82 + .16 * g[..., None]) * (.9 + .2 * fbm(P, (.4, .4, .4), self.seed + 2)[..., None])
        return np.where(plank, img * .6, img)

    def s_crystal(self, P, color='#6ab0ff'):
        c = rgb(color)
        n = fbm(P, (.22, .22, .22), self.seed)
        facet = .78 + .4 * np.abs(np.sin(P[..., 1] * .35 + P[..., 0] * .2 + n * 4))
        return np.clip(c * facet[..., None] * 1.1 + 22, 0, 255)

    def s_glowc(self, P, color='#9ad0ff'):
        c = rgb(color)
        n = fbm(P, (.3, .3, .3), self.seed + 4)
        return np.clip(c * (.85 + .3 * n[..., None]) + 24, 0, 255)

    def s_flame(self, P, color='#ff9a28'):
        c = rgb(color)
        t = np.clip((P[..., 1] % 40) / 14.0 + (fbm(P, (.5, .5, .5), self.seed) - .5), 0, 1)
        return np.clip(lerp(c, rgb('#fff0a0'), t) * 1.05, 0, 255)

    def s_water(self, P, color='#3f8fe0'):
        c = rgb(color)
        w = np.sin(P[..., 0] * .8 + P[..., 2] * .6 + fbm(P, (.2, .2, .2), self.seed) * 5)
        return np.clip(c * (.9 + .18 * w[..., None]) + 14, 0, 255)

    def s_banner(self, P, color='#8a1e1e', trim='#e0b040'):
        c = rgb(color); t = rgb(trim)
        band = (np.abs(np.sin(P[..., 1] * .22)) > .93)[..., None]
        diam = (np.abs(P[..., 0]) + np.abs(((P[..., 1] % 24) - 12) * .6) < 3.4)[..., None]
        base = c * (.85 + .2 * fbm(P, (.3, .3, .3), self.seed)[..., None])
        return np.where(band | diam, t * .95, base)


L.Shader = DShader

MARB = ('marble', '#ece7dc')
MARB2 = ('marble', '#c9c2b4')
AZUR = ('marble', '#9a6fd0')
STONE = ('stone', '#8f9298')
STONE2 = ('stone', '#6f737b')
GOLD = ('gold', '#d9a830')
IRON = ('metal', '#4b4e57')
IRON2 = ('metal', '#2e3037')
WOOD = ('wood', '#6b4528')
WOOD2 = ('wood', '#4a2e1a')
CRYS = ('crystal', '#b070f0')
CRYS2 = ('crystal', '#e070d0')
FLAME = ('flame', '#ff9a28')
GLOW = ('glowc', '#e0b8ff')
WATER = ('water', '#3f8fe0')
RED = ('banner', '#8a1e1e', '#e0b040')
BLUE = ('banner', '#5a2a8a', '#e0e0f0')
LEATH = ('leather', '#6b4024')
CLOTH = ('cloth_plain', '#a82a2a')
BONE = ('bone', '#e8dfc4')


# ------------------------------------------------------------------ primitives
def new(key, seed):
    m = L.Model(key, seed)
    m.group(G, (0, 0, 0))
    return m


def B(m, a, b, mat, rot=None, pivot=None, hide=()):
    m.box(G, a, b, mat, rot, pivot, False, hide)


def Bc(m, cx, cz, y0, y1, hw, hd, mat, hide=()):
    """Boite centree sur (cx, cz)."""
    m.box(G, (cx - hw, y0, cz - hd), (cx + hw, y1, cz + hd), mat, None, None, False, hide)


def cyl(m, cx, cz, y0, y1, r, mat, small=False):
    """Cylindre a 8 pans (4 boites a 0/45/90/135 degres) ; r = apotheme."""
    if small or r < 1.6:
        Bc(m, cx, cz, y0, y1, r, r, mat)
        return
    k = r * math.tan(math.radians(22.5))
    m.box(G, (cx - r, y0, cz - k), (cx + r, y1, cz + k), mat)
    m.box(G, (cx - k, y0, cz - r), (cx + k, y1, cz + r), mat)
    for a in (45, -45):
        m.box(G, (cx - r, y0, cz - k), (cx + r, y1, cz + k), mat, (0, a, 0), (cx, y0, cz))


def taper(m, cx, cz, y0, y1, r0, r1, mat, n=3):
    for i in range(n):
        a = y0 + (y1 - y0) * i / n
        b = y0 + (y1 - y0) * (i + 1) / n
        cyl(m, cx, cz, a, b, r0 + (r1 - r0) * (i + .5) / n, mat)


def pyramid(m, cx, cz, y0, y1, r0, r1, mat, n=4):
    for i in range(n):
        a = y0 + (y1 - y0) * i / n
        b = y0 + (y1 - y0) * (i + 1) / n
        r = r0 + (r1 - r0) * (i + .5) / n
        Bc(m, cx, cz, a, b, r, r, mat)


MODELS = {}


def model(key, fr, en, shape, light=0):
    """shape = boite de collision (x0,y0,z0,x1,y1,z1) en pixels de bloc (0-16), hauteur jusqu'a 32."""
    def deco(f):
        MODELS[key] = (f, fr, en, shape, light)
        return f
    return deco


# ============================================================================== les 30 decors
@model('deco_lampadaire', 'Lampadaire de la citadelle', 'Citadel Street Lamp', (5, 0, 5, 11, 32, 11), 15)
def lampadaire():
    m = new('deco_lampadaire', 1)
    pyramid(m, 0, 0, 0, 8, 8, 5, STONE)
    cyl(m, 0, 0, 8, 12, 5.5, MARB)
    cyl(m, 0, 0, 12, 60, 2.4, IRON)
    for y in (16, 34, 50):
        cyl(m, 0, 0, y, y + 2.2, 3.4, GOLD)
    for s in (-1, 1):                                    # volutes
        Bc(m, s * 4.5, 0, 52, 53.4, 3, .9, GOLD)
        Bc(m, s * 7, 0, 53.4, 58, .9, .9, GOLD)
    Bc(m, 0, 0, 60, 62, 8, 8, GOLD)
    for sx in (-1, 1):
        for sz in (-1, 1):
            Bc(m, sx * 6.6, sz * 6.6, 62, 82, 1, 1, GOLD)
    Bc(m, 0, 0, 62, 80, 5.6, 5.6, GLOW)
    Bc(m, 0, 0, 64, 70, 2.5, 2.5, FLAME)
    pyramid(m, 0, 0, 80, 92, 9.5, 2, GOLD, 4)
    cyl(m, 0, 0, 92, 100, 1.2, GOLD)
    return m


@model('deco_brasero', 'Brasero monumental', 'Monumental Brazier', (3, 0, 3, 13, 20, 13), 15)
def brasero():
    m = new('deco_brasero', 2)
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.box(G, (sx * 9 - 1.6, 0, sz * 9 - 1.6), (sx * 9 + 1.6, 22, sz * 9 + 1.6), IRON2, (sz * 12, 0, -sx * 12), (sx * 9, 22, sz * 9))
    cyl(m, 0, 0, 22, 26, 13, IRON)
    taper(m, 0, 0, 26, 40, 13, 19, IRON, 4)
    cyl(m, 0, 0, 40, 43, 19.5, GOLD)
    cyl(m, 0, 0, 36, 41, 15, ('flame', '#c83010'))
    for i, (x, z, h) in enumerate([(0, 0, 30), (6, 3, 22), (-6, -2, 24), (2, -7, 20), (-3, 7, 18)]):
        Bc(m, x, z, 41, 41 + h, 4 - i * .3, 4 - i * .3, FLAME)
    return m


@model('deco_fontaine', 'Fontaine de la citadelle', 'Citadel Fountain', (1, 0, 1, 15, 14, 15), 6)
def fontaine():
    m = new('deco_fontaine', 3)
    cyl(m, 0, 0, 0, 10, 30, MARB2)
    cyl(m, 0, 0, 10, 14, 32, MARB)
    cyl(m, 0, 0, 10, 12, 27, WATER)
    taper(m, 0, 0, 12, 34, 5, 3.5, MARB, 3)
    cyl(m, 0, 0, 34, 38, 13, MARB)
    cyl(m, 0, 0, 38, 40, 11, WATER)
    taper(m, 0, 0, 40, 60, 2.4, 1.8, MARB, 2)
    cyl(m, 0, 0, 60, 63, 6, GOLD)
    Bc(m, 0, 0, 63, 78, 1.4, 1.4, WATER)
    Bc(m, 0, 0, 78, 82, 3, 3, GLOW)
    for a in range(0, 360, 45):                          # jets retombants
        x, z = math.cos(math.radians(a)) * 6, math.sin(math.radians(a)) * 6
        Bc(m, x, z, 40, 62, .8, .8, WATER)
    return m


def statue(key, seed, armor, accent, weapon):
    m = new(key, seed)
    pyramid(m, 0, 0, 0, 10, 14, 11, STONE, 2)
    Bc(m, 0, 0, 10, 16, 12, 12, MARB2)
    for s in (-1, 1):                                    # jambes
        Bc(m, s * 3.4, 0, 16, 40, 2.6, 2.8, armor)
        Bc(m, s * 3.4, -.8, 14, 17, 3, 4.2, armor)
    Bc(m, 0, 0, 40, 46, 7.5, 4, accent)                  # ceinture
    Bc(m, 0, 0, 46, 66, 7, 4.4, armor)                   # torse
    for s in (-1, 1):
        Bc(m, s * 10, 0, 62, 68, 4.4, 4.4, accent)       # epaulieres
        Bc(m, s * 10.4, 0, 46, 62, 2.2, 2.4, armor)
    Bc(m, 0, 0, 66, 70, 2.5, 2.5, armor)
    Bc(m, 0, 0, 70, 82, 4.2, 4.2, MARB)                  # tete
    return m


@model('deco_statue_guerrier', 'Statue du guerrier', 'Warrior Statue', (3, 0, 3, 13, 32, 13))
def statue_guerrier():
    m = statue('deco_statue_guerrier', 4, MARB, MARB2, None)
    Bc(m, 0, 0, 82, 86, 4.8, 4.8, IRON)                  # casque
    Bc(m, 0, 0, 86, 90, 1, 4.6, IRON)
    Bc(m, 0, 0, 90, 94, .8, 1.5, ('banner', '#8a1e1e', '#e0b040'))
    Bc(m, 10.4, -1, 38, 46, 2.4, 2.4, MARB)              # main sur l'epee
    cyl(m, 10.4, -1, 22, 38, 1.3, IRON)                  # lame plantee
    Bc(m, 10.4, -1, 38, 40, 5.6, 1.4, GOLD)
    Bc(m, 10.4, -1, 14, 38, .9, 3.4, ('metal', '#aab0bc'))
    B(m, (-18, 42, -6), (-13, 66, 6), IRON)              # bouclier
    B(m, (-18.4, 46, -3), (-12.6, 62, 3), GOLD)
    return m


@model('deco_statue_mage', 'Statue de l’archimage', 'Archmage Statue', (3, 0, 3, 13, 32, 13))
def statue_mage():
    m = statue('deco_statue_mage', 5, AZUR, MARB, None)
    Bc(m, 0, 0, 82, 86, 5, 5, AZUR)                      # chapeau pointu
    pyramid(m, 0, 0, 86, 110, 4, .6, AZUR, 6)
    Bc(m, 0, 0, 83, 84.4, 8, 8, AZUR)
    cyl(m, 12, -2, 14, 100, 1.3, WOOD)                   # baton
    Bc(m, 12, -2, 100, 108, 3.2, 3.2, CRYS)
    Bc(m, 12, -2, 96, 100, 3, 3, GOLD)
    Bc(m, 0, 0, 44, 62, 8.2, 5.4, ('marble', '#6a3fa8'))  # robe
    return m


@model('deco_colonne_ornee', 'Colonne ornée', 'Ornate Column', (3, 0, 3, 13, 32, 13))
def colonne():
    m = new('deco_colonne_ornee', 6)
    Bc(m, 0, 0, 0, 6, 11, 11, STONE)
    Bc(m, 0, 0, 6, 10, 9.5, 9.5, MARB)
    cyl(m, 0, 0, 10, 14, 8.4, GOLD)
    cyl(m, 0, 0, 14, 82, 6.2, MARB)
    for a in range(0, 360, 45):                          # cannelures
        x, z = math.cos(math.radians(a)) * 6.4, math.sin(math.radians(a)) * 6.4
        Bc(m, x, z, 16, 80, .6, .6, MARB2)
    cyl(m, 0, 0, 82, 86, 8.4, GOLD)
    taper(m, 0, 0, 86, 94, 7, 10, MARB, 2)
    Bc(m, 0, 0, 94, 98, 11.5, 11.5, MARB)
    Bc(m, 0, 0, 98, 100, 12, 12, GOLD)
    return m


@model('deco_arche', 'Arche triomphale', 'Triumphal Arch', (0, 0, 5, 16, 32, 11))
def arche():
    m = new('deco_arche', 7)
    for s in (-1, 1):
        Bc(m, s * 30, 0, 0, 8, 12, 9, STONE)
        Bc(m, s * 30, 0, 8, 88, 8.5, 6.5, MARB)
        Bc(m, s * 30, 0, 88, 92, 11, 8.5, GOLD)
        for y in (20, 44, 68):
            Bc(m, s * 30, -6.7, y, y + 2, 7, .5, MARB2)
    n = 14                                                # voute en briques
    for i in range(n + 1):
        a = math.pi * i / n
        x, y = math.cos(a) * 22, 92 + math.sin(a) * 22
        m.box(G, (x - 3.2, y - 3.2, -5.5), (x + 3.2, y + 3.2, 5.5), MARB if i % 2 else MARB2, (0, 0, math.degrees(a) - 90), (x, y, 0))
    Bc(m, 0, 0, 114, 120, 5, 7, GOLD)                     # clef de voute
    Bc(m, 0, 0, 120, 126, 3, 5, CRYS)
    Bc(m, 0, 0, 120, 128, 40, 6.6, ('stone', '#b8b2a4'), hide=())
    return m


@model('deco_banc', 'Banc de pierre', 'Stone Bench', (1, 0, 4, 15, 9, 12))
def banc():
    m = new('deco_banc', 8)
    for s in (-1, 1):
        Bc(m, s * 17, 0, 0, 16, 4, 8, MARB)
        Bc(m, s * 17, 0, 16, 20, 5, 9, GOLD)
    B(m, (-24, 20, -9), (24, 25, 9), WOOD)
    B(m, (-24, 25, 6), (24, 44, 8.5), WOOD)
    for s in (-1, 1):
        Bc(m, s * 20, 7.2, 25, 46, 1.2, 1.2, GOLD)
    return m


@model('deco_etendard', 'Étendard de la citadelle', 'Citadel Standard', (6, 0, 6, 10, 32, 10))
def etendard():
    m = new('deco_etendard', 9)
    cyl(m, 0, 0, 0, 6, 5, STONE)
    cyl(m, 0, 0, 6, 112, 1.6, WOOD2)
    Bc(m, 0, 0, 110, 114, 3.4, 3.4, GOLD)
    Bc(m, 0, 0, 114, 120, 2.4, 2.4, GOLD)
    Bc(m, 0, 0, 100, 102, 17, .9, GOLD)                  # traverse
    for s in (-1, 1):
        Bc(m, s * 17.5, 0, 99, 103, 1.6, 1.6, GOLD)
    B(m, (-15, 44, -.9), (15, 100, .9), RED)
    for i in range(5):                                    # queue en pointes
        w = 15 - i * 3
        B(m, (-w, 40 - i * 0, -.9), (w - 3, 44 - 0, .9), RED) if False else None
    pyramid(m, 0, 0, 36, 44, 15, 1, RED, 4) if False else None
    B(m, (-15, 36, -.9), (-5, 44, .9), RED)
    B(m, (5, 36, -.9), (15, 44, .9), RED)
    B(m, (-4, 30, -.9), (4, 44, .9), RED)
    Bc(m, 0, 0, 72, 90, 6, 1.1, GOLD)                    # embleme
    Bc(m, 0, 0, 78, 84, 10, 1.1, GOLD)
    return m


@model('deco_obelisque', 'Obélisque d’Aeloria', 'Aeloria Obelisk', (4, 0, 4, 12, 32, 12), 8)
def obelisque():
    m = new('deco_obelisque', 10)
    pyramid(m, 0, 0, 0, 12, 18, 14, STONE, 3)
    pyramid(m, 0, 0, 12, 120, 10, 5.2, MARB, 9)
    pyramid(m, 0, 0, 120, 134, 5.2, .6, GOLD, 4)
    for y in (30, 52, 74, 96):                            # runes
        Bc(m, 0, 0, y, y + 6, 8.6 - y * .04, 8.6 - y * .04 + .6, GLOW) if False else None
        B(m, (-1.2, y, -9.5 + y * .04), (1.2, y + 8, -8.6 + y * .04), GLOW)
        B(m, (-1.2, y, 8.6 - y * .04), (1.2, y + 8, 9.5 - y * .04), GLOW)
    return m


@model('deco_vase_geant', 'Vase monumental', 'Giant Vase', (3, 0, 3, 13, 20, 13))
def vase():
    m = new('deco_vase_geant', 11)
    cyl(m, 0, 0, 0, 4, 7, GOLD)
    cyl(m, 0, 0, 4, 10, 6, ('marble', '#6a3fa8'))
    for y0, y1, r in [(10, 20, 11), (20, 34, 15), (34, 46, 13), (46, 54, 8), (54, 62, 6)]:
        cyl(m, 0, 0, y0, y1, r, ('marble', '#6a3fa8'))
    cyl(m, 0, 0, 27, 29, 15.6, GOLD)
    cyl(m, 0, 0, 62, 66, 10, GOLD)
    for s in (-1, 1):                                    # anses
        Bc(m, s * 8.5, 0, 52, 60, 1.2, 1.6, GOLD)
        Bc(m, s * 11, 0, 38, 54, 1.2, 1.6, GOLD)
    return m


@model('deco_lanterne_potence', 'Lanterne à potence', 'Bracket Lantern', (5, 0, 5, 11, 32, 11), 14)
def lanterne():
    m = new('deco_lanterne_potence', 12)
    Bc(m, 0, 0, 0, 5, 6, 6, STONE)
    cyl(m, 0, 0, 5, 76, 2.2, WOOD2)
    B(m, (-1, 66, -1), (1.4, 80, 1.4), IRON)
    B(m, (-1, 78, -1), (30, 80.4, 1.4), IRON)
    B(m, (0, 60, -1), (24, 62, 1), IRON, rot=(0, 0, -32), pivot=(2, 62, 0))
    for sx in (-1, 1):
        for sz in (-1, 1):
            Bc(m, 30 + sx * 3.8, sz * 3.8, 54, 72, .8, .8, IRON)
    Bc(m, 30, 0, 52, 54, 5, 5, IRON)
    Bc(m, 30, 0, 54, 72, 3.6, 3.6, GLOW)
    Bc(m, 30, 0, 56, 64, 1.8, 1.8, FLAME)
    pyramid(m, 30, 0, 72, 80, 6, 1, IRON, 3)
    B(m, (29, 79, -.5), (31, 82, .5), IRON)
    return m


@model('deco_grille_forgee', 'Grille forgée', 'Wrought Gate', (0, 0, 6, 16, 28, 10))
def grille():
    m = new('deco_grille_forgee', 13)
    for s in (-1, 1):
        Bc(m, s * 23, 0, 0, 74, 2.8, 2.8, IRON2)
        Bc(m, s * 23, 0, 74, 80, 3.6, 3.6, GOLD)
        pyramid(m, s * 23, 0, 80, 88, 2.4, .4, GOLD, 3)
    B(m, (-23, 6, -1.2), (23, 9, 1.2), IRON2)
    B(m, (-23, 60, -1.2), (23, 63, 1.2), IRON2)
    for i in range(-5, 6):
        x = i * 3.6
        B(m, (x - .7, 9, -.7), (x + .7, 66, .7), IRON)
        pyramid(m, x, 0, 63, 68, 1.2, .2, GOLD, 2)
    for i in range(-4, 5):                                # ornements en losange
        Bc(m, i * 3.6 + 1.8, 0, 30, 36, 1.8, 1.0, GOLD) if i < 4 else None
    return m


@model('deco_tonneau_geant', 'Tonneau géant', 'Giant Barrel', (2, 0, 2, 14, 16, 14))
def tonneau():
    m = new('deco_tonneau_geant', 14)
    for y0, y1, r in [(0, 5, 14), (5, 12, 17), (12, 36, 19), (36, 43, 17), (43, 48, 14)]:
        cyl(m, 0, 0, y0, y1, r, WOOD)
    for y in (3, 16, 32, 45):
        cyl(m, 0, 0, y, y + 2.2, 19.3 if 16 <= y <= 32 else 15.5, IRON)
    B(m, (-4, 22, -20.2), (4, 28, -19.2), IRON2)         # bonde
    return m


@model('deco_caisses_empilees', 'Caisses empilées', 'Stacked Crates', (0, 0, 0, 16, 32, 16))
def caisses():
    m = new('deco_caisses_empilees', 15)

    def crate(x, y, z, s, rot=0):
        r = (0, rot, 0) if rot else None
        p = (x, y, z) if rot else None
        m.box(G, (x - s, y, z - s), (x + s, y + 2 * s, z + s), WOOD, r, p)
        for dx in (-1, 1):
            for dz in (-1, 1):
                m.box(G, (x + dx * (s - 1.2) - 1.2, y, z + dz * (s - 1.2) - 1.2), (x + dx * (s - 1.2) + 1.2, y + 2 * s + .3, z + dz * (s - 1.2) + 1.2), WOOD2, r, p)
        m.box(G, (x - s - .3, y + s - 1, z - s - .3), (x + s + .3, y + s + 1, z + s + .3), WOOD2, r, p)
    crate(-10, 0, -8, 12)
    crate(12, 0, -2, 11)
    crate(-4, 0, 17, 10)
    crate(-8, 24, -8, 9, 18)
    crate(12, 22, -2, 7, -10)
    return m


@model('deco_table_banquet', 'Table de banquet', 'Banquet Table', (0, 0, 2, 16, 14, 14))
def table():
    m = new('deco_table_banquet', 16)
    B(m, (-36, 26, -12), (36, 30, 12), WOOD)
    B(m, (-37, 29, -13), (37, 31, 13), WOOD2)
    for sx in (-1, 1):
        for sz in (-1, 1):
            Bc(m, sx * 31, sz * 8, 0, 26, 2.6, 2.6, WOOD2)
    B(m, (-33, 14, -.8), (33, 17, .8), WOOD2)
    B(m, (-20, 31, -4), (-6, 32, 8), ('cloth_plain', '#a82a2a'))
    B(m, (6, 31, -8), (22, 32, 4), ('cloth_plain', '#a82a2a'))
    cyl(m, -24, -4, 31, 35, 2.4, GOLD)                    # gobelets / plats
    cyl(m, 14, 6, 31, 34, 5, ('metal', '#c0c4cc'))
    cyl(m, 0, 0, 31, 40, 1.6, GOLD)
    return m


@model('deco_trone', 'Trône de la citadelle', 'Citadel Throne', (2, 0, 2, 14, 32, 14))
def trone():
    m = new('deco_trone', 17)
    pyramid(m, 0, 0, 0, 12, 22, 18, STONE, 3)
    B(m, (-14, 12, -12), (14, 30, 12), RED)
    B(m, (-16, 30, -14), (16, 34, 14), GOLD)
    B(m, (-16, 34, 8), (16, 110, 14), MARB)
    B(m, (-12, 38, 6.5), (12, 100, 8.5), RED)
    for s in (-1, 1):
        B(m, (s * 14 - 2.4 + (0 if s > 0 else 0), 30, -14), (s * 14 + 2.4, 48, 12), GOLD)
        B(m, (s * 17 - 2.6, 34, 6), (s * 17 + 2.6, 118, 14), GOLD)
        pyramid(m, s * 17, 10, 118, 130, 2.6, .4, GOLD, 3)
    Bc(m, 0, 11, 110, 122, 9, 2, GOLD)
    Bc(m, 0, 11, 122, 130, 4, 2, CRYS)
    return m


@model('deco_chandelier', 'Chandelier d’or', 'Golden Candelabrum', (4, 0, 4, 12, 28, 12), 14)
def chandelier():
    m = new('deco_chandelier', 18)
    pyramid(m, 0, 0, 0, 6, 9, 6, GOLD, 2)
    cyl(m, 0, 0, 6, 62, 2, GOLD)
    for y in (16, 30, 44):
        cyl(m, 0, 0, y, y + 3, 3.6, GOLD)
    for i, a in enumerate([0, 72, 144, 216, 288]):        # cinq bras
        dx, dz = math.cos(math.radians(a)), math.sin(math.radians(a))
        for t in (1, 2, 3):
            x, z = dx * 4 * t, dz * 4 * t
            Bc(m, x, z, 46 + t * 2.5 - 3, 48 + t * 2.5 - 3, 1, 1, GOLD)
        x, z = dx * 12, dz * 12
        Bc(m, x, z, 46 + 7.5 - 3, 62, 1, 1, GOLD)
        cyl(m, x, z, 63, 70, 1.8, ('bone', '#f4efe0'))
        Bc(m, x, z, 70, 75, 1.1, 1.1, FLAME)
    cyl(m, 0, 0, 62, 70, 1.8, ('bone', '#f4efe0'))
    Bc(m, 0, 0, 70, 77, 1.3, 1.3, FLAME)
    return m


@model('deco_cristal_flottant', 'Cristal flottant', 'Floating Crystal', (3, 0, 3, 13, 12, 13), 15)
def cristal():
    m = new('deco_cristal_flottant', 19)
    pyramid(m, 0, 0, 0, 5, 20, 14, STONE2, 2)
    cyl(m, 0, 0, 5, 9, 12, IRON)
    for i in range(3):
        a = math.radians(i * 120 + 20)
        B(m, (math.cos(a) * 12 - 1, 9, math.sin(a) * 12 - 1), (math.cos(a) * 12 + 1, 30, math.sin(a) * 12 + 1), GOLD)
    for i in range(6):                                    # cristal en losange empile
        r = [3, 8, 12, 12, 8, 3][i]
        Bc(m, 0, 0, 36 + i * 9, 45 + i * 9, r, r, CRYS if i % 2 == 0 else CRYS2)
    m.box(G, (-9, 36, -9), (9, 90, 9), CRYS, (0, 45, 0), (0, 36, 0))
    return m


@model('deco_ratelier_armes', 'Râtelier d’armes', 'Weapon Rack', (1, 0, 5, 15, 24, 11))
def ratelier():
    m = new('deco_ratelier_armes', 20)
    for s in (-1, 1):
        Bc(m, s * 26, 0, 0, 70, 3, 5, WOOD2)
        Bc(m, s * 26, 0, 70, 74, 4, 6, GOLD)
        Bc(m, s * 26, 0, 0, 3, 5, 10, WOOD2)
    B(m, (-26, 24, -3), (26, 28, 3), WOOD)
    B(m, (-26, 50, -3), (26, 54, 3), WOOD)
    for i, x in enumerate([-18, -6, 6, 18]):
        cyl(m, x, 0, 20, 62 + (i % 2) * 8, 1.1, ('metal', '#c0c4cc'))      # lames
        Bc(m, x, 0, 12, 20, 2.6, .8, GOLD)
        Bc(m, x, 0, 4, 12, 1, 1, LEATH)
        Bc(m, x, 0, 62 + (i % 2) * 8, 66 + (i % 2) * 8, .6, .6, ('metal', '#e0e4ec'))
    for x in (-12, 12):
        cyl(m, x, 4, 30, 80, .9, WOOD)                    # lances
        pyramid(m, x, 4, 80, 90, 1.6, .2, ('metal', '#d0d4dc'), 3)
    return m


@model('deco_enclume_geante', 'Enclume géante', 'Giant Anvil', (2, 0, 4, 14, 16, 12))
def enclume():
    m = new('deco_enclume_geante', 21)
    Bc(m, 0, 0, 0, 16, 11, 11, WOOD2)
    Bc(m, 0, 0, 16, 20, 13, 9, IRON2)
    Bc(m, 0, 0, 20, 26, 7, 6, IRON2)
    B(m, (-22, 26, -8), (22, 40, 8), IRON)
    B(m, (-34, 32, -5), (-22, 40, 5), IRON)
    B(m, (22, 33, -4), (30, 40, 4), IRON, rot=(0, 0, -14), pivot=(22, 36, 0))
    B(m, (-22, 39.6, -6), (22, 41, 6), ('metal', '#8a8f9c'))
    B(m, (-10, 41, -1), (6, 46, 2), IRON2, rot=(0, 24, 0), pivot=(0, 41, 0))
    return m


@model('deco_puits', 'Puits de la citadelle', 'Citadel Well', (1, 0, 1, 15, 16, 15))
def puits():
    m = new('deco_puits', 22)
    cyl(m, 0, 0, 0, 24, 18, STONE)
    cyl(m, 0, 0, 24, 28, 19.5, MARB2)
    cyl(m, 0, 0, 20, 24.5, 13, WATER)
    for s in (-1, 1):
        Bc(m, s * 18, 0, 28, 84, 2.4, 2.4, WOOD2)
    B(m, (-20, 84, -3), (20, 88, 3), WOOD2)
    cyl(m, 0, 0, 70, 80, 0.0 + 1.8, WOOD)
    m.box(G, (-18, 68, -2), (18, 70, 2), WOOD)
    pyramid(m, 0, 0, 88, 112, 22, 2, ('tuile', '#6a3fa8') if False else ('stone', '#6a3fa8'), 5)
    B(m, (-.5, 40, -.5), (.5, 69, .5), LEATH)
    Bc(m, 0, 0, 30, 40, 4, 4, WOOD)
    return m


@model('deco_horloge', 'Horloge de la citadelle', 'Citadel Clock', (3, 0, 3, 13, 32, 13), 5)
def horloge():
    m = new('deco_horloge', 23)
    pyramid(m, 0, 0, 0, 16, 16, 14, STONE, 3)
    pyramid(m, 0, 0, 16, 120, 12, 11, MARB, 8)
    cyl(m, 0, -12, 90, 118, 15, GOLD)                    # cadran sur la face avant
    cyl(m, 0, -13.3, 92, 116, 13, ('marble', '#f4efe0'))
    B(m, (-.8, 104, -15.4), (.8, 114, -13.5), IRON2)     # aiguilles
    B(m, (-.8, 103, -15.4), (8, 105, -13.5), IRON2)
    for a in range(0, 360, 30):
        x = math.cos(math.radians(a)) * 10.5
        y = 104 + math.sin(math.radians(a)) * 10.5
        B(m, (x - .8, y - .8, -15.2), (x + .8, y + .8, -13.5), GOLD)
    Bc(m, 0, 0, 120, 124, 14, 14, GOLD)
    pyramid(m, 0, 0, 124, 152, 12, .8, ('stone', '#6a3fa8'), 6)
    Bc(m, 0, 0, 152, 158, 1, 1, GOLD)
    return m


@model('deco_balise_runique', 'Balise runique', 'Rune Beacon', (3, 0, 3, 13, 20, 13), 15)
def balise():
    m = new('deco_balise_runique', 24)
    cyl(m, 0, 0, 0, 6, 16, STONE2)
    cyl(m, 0, 0, 6, 9, 13, ('dark', '#1a2a4a'))
    cyl(m, 0, 0, 9, 10, 11, GLOW)
    for a in range(0, 360, 60):
        x, z = math.cos(math.radians(a)) * 15, math.sin(math.radians(a)) * 15
        m.box(G, (x - 2, 6, z - 2), (x + 2, 30, z + 2), STONE, (0, 0, 0))
        Bc(m, x, z, 30, 34, 2.6, 2.6, GLOW)
    taper(m, 0, 0, 10, 40, 4, 2, CRYS, 3)
    Bc(m, 0, 0, 40, 60, 5, 5, CRYS)
    m.box(G, (-5, 40, -5), (5, 60, 5), CRYS2, (0, 45, 0), (0, 40, 0))
    Bc(m, 0, 0, 60, 100, 1.6, 1.6, GLOW)
    return m


@model('deco_etal_marche', 'Étal de marché', 'Market Stall', (0, 0, 2, 16, 14, 14))
def etal():
    m = new('deco_etal_marche', 25)
    for sx in (-1, 1):
        for sz in (-1, 1):
            Bc(m, sx * 30, sz * 14, 0, 78, 1.8, 1.8, WOOD2)
    B(m, (-34, 26, -16), (34, 30, 16), WOOD)
    B(m, (-34, 0, -16), (34, 26, -14), WOOD2)
    B(m, (-34, 0, 14), (34, 26, 16), WOOD2)
    for i in range(7):                                    # toit a rayures
        c = ('cloth_plain', '#a82a2a') if i % 2 == 0 else ('cloth_plain', '#f0e8d0')
        x0 = -36 + i * 10.3
        B(m, (x0, 76, -20), (x0 + 10.3, 80, 20), c)
        B(m, (x0, 72, 19), (x0 + 10.3, 78, 21), c)
        B(m, (x0, 72, -21), (x0 + 10.3, 78, -19), c)
    for x, c in [(-22, ('banner', '#c03020', '#f0d060')), (-6, ('banner', '#e0b040', '#f0d060')), (10, ('banner', '#5a9a3a', '#f0d060'))]:
        Bc(m, x, 0, 30, 36, 6, 6, WOOD)
        Bc(m, x, 0, 36, 42, 5, 5, c)
    cyl(m, 24, 4, 30, 40, 4, LEATH)
    return m


@model('deco_sablier_geant', 'Sablier géant', 'Giant Hourglass', (3, 0, 3, 13, 28, 13), 4)
def sablier():
    m = new('deco_sablier_geant', 26)
    cyl(m, 0, 0, 0, 5, 16, GOLD)
    cyl(m, 0, 0, 5, 8, 14, WOOD2)
    for a in range(0, 360, 90):
        x, z = math.cos(math.radians(a + 45)) * 13, math.sin(math.radians(a + 45)) * 13
        Bc(m, x, z, 8, 88, 1.6, 1.6, GOLD)
    taper(m, 0, 0, 8, 46, 11, 1.4, GLOW, 5)
    taper(m, 0, 0, 46, 84, 1.4, 11, GLOW, 5)
    taper(m, 0, 0, 10, 28, 8, 3, ('stone', '#d8b060') if False else ('flame', '#e8c070'), 3)
    cyl(m, 0, 0, 80, 83, 11, ('flame', '#e8c070'))
    cyl(m, 0, 0, 88, 91, 14, WOOD2)
    cyl(m, 0, 0, 91, 96, 16, GOLD)
    return m


@model('deco_globe_arcane', 'Globe arcanique', 'Arcane Globe', (3, 0, 3, 13, 28, 13), 12)
def globe():
    m = new('deco_globe_arcane', 27)
    pyramid(m, 0, 0, 0, 8, 14, 10, STONE, 2)
    cyl(m, 0, 0, 8, 28, 3, GOLD)
    cyl(m, 0, 0, 28, 31, 7, GOLD)
    Bc(m, 0, 0, 32, 56, 12, 12, CRYS)
    Bc(m, 0, 0, 35, 53, 14, 6, CRYS2)
    Bc(m, 0, 0, 35, 53, 6, 14, CRYS2)
    for k in range(3):                                    # anneaux en laiton
        r = 22 - k * 2
        n = 16
        for i in range(n):
            a = 2 * math.pi * i / n
            x, z = math.cos(a) * r, math.sin(a) * r
            m.box(G, (x - 1.6, 43 - 1, z - 1.6), (x + 1.6, 43 + 1, z + 1.6), GOLD, (k * 30 - 30, 0, k * 30), (0, 43, 0)) if False else None
    for i in range(16):
        a = 2 * math.pi * i / 16
        for k, tilt in enumerate((0, 50, -50)):
            y = 44 + math.sin(a) * 22 * math.sin(math.radians(tilt))
            x = math.cos(a) * 22
            z = math.sin(a) * 22 * math.cos(math.radians(tilt))
            B(m, (x - 1.4, y - 1.4, z - 1.4), (x + 1.4, y + 1.4, z + 1.4), GOLD)
    return m


@model('deco_bibliotheque', 'Grande bibliothèque', 'Grand Bookshelf', (0, 0, 4, 16, 32, 12))
def bibliotheque():
    m = new('deco_bibliotheque', 28)
    B(m, (-30, 0, -9), (30, 4, 9), WOOD2)
    for s in (-1, 1):
        B(m, (s * 30 - (0 if s > 0 else 3), 0, -9), (s * 30 + (3 if s > 0 else 0), 120, 9), WOOD2)
    B(m, (-30, 4, 7), (30, 118, 9), WOOD2)
    for y in (4, 34, 64, 94, 118):
        B(m, (-30, y, -9), (30, y + 3, 9), WOOD)
    B(m, (-33, 118, -11), (33, 124, 11), GOLD)
    cols = ['#8a1e1e', '#5a2a8a', '#2e6a3a', '#c09030', '#5a2a7a', '#6b4024', '#2a6a8a']
    for row, y in enumerate((7, 37, 67, 97)):
        x = -27.0
        i = row
        while x < 26:
            w = 2.2 + ((i * 7) % 4) * .7
            h = 18 + ((i * 5) % 4) * 2.4
            B(m, (x, y, -6.5), (x + w, y + h, 5.5), ('leather', cols[(i * 3 + row) % len(cols)]))
            x += w + .15
            i += 1
    return m


@model('deco_gargouille', 'Gargouille de pierre', 'Stone Gargoyle', (2, 0, 2, 14, 24, 14))
def gargouille():
    m = new('deco_gargouille', 29)
    Bc(m, 0, 0, 0, 10, 14, 14, STONE)
    Bc(m, 0, 0, 10, 14, 15, 15, STONE2)
    for s in (-1, 1):
        Bc(m, s * 7, 2, 14, 30, 3.6, 4.4, STONE2)
        Bc(m, s * 7, -4, 14, 18, 3.6, 3, STONE2)
    Bc(m, 0, 2, 28, 56, 9, 7, STONE2)
    for s in (-1, 1):                                    # ailes
        B(m, (s * 9, 38, 3), (s * 34, 62, 5.5), STONE, rot=(0, 0, -s * 14), pivot=(s * 9, 40, 4))
        B(m, (s * 12, 56, 2), (s * 30, 74, 4.5), STONE2, rot=(0, 0, -s * 30), pivot=(s * 9, 56, 3))
        Bc(m, s * 8, -5, 44, 54, 2.4, 3.2, STONE2)       # bras
    Bc(m, 0, -3, 56, 74, 6.2, 6, STONE)                  # tete
    Bc(m, 0, -9, 58, 66, 4, 3.6, STONE)                  # museau
    for s in (-1, 1):
        B(m, (s * 5 - 1, 74, -3), (s * 5 + 1, 84, 1), BONE, rot=(0, 0, -s * 10), pivot=(s * 5, 74, -1))
        Bc(m, s * 3.2, -9.2, 66, 69, 1.2, .7, ('flame', '#ff5030'))
    return m


@model('deco_bassin', 'Bassin de cristal', 'Crystal Basin', (0, 0, 0, 16, 10, 16), 7)
def bassin():
    m = new('deco_bassin', 30)
    B(m, (-36, 0, -36), (36, 12, 36), MARB2)
    B(m, (-38, 12, -38), (38, 16, 38), MARB)
    B(m, (-32, 12, -32), (32, 15, 32), WATER)
    for sx in (-1, 1):
        for sz in (-1, 1):
            Bc(m, sx * 36, sz * 36, 0, 28, 3.6, 3.6, MARB)
            Bc(m, sx * 36, sz * 36, 28, 30, 4.4, 4.4, GOLD)
            Bc(m, sx * 36, sz * 36, 30, 36, 2, 2, CRYS)
    for x, z, h in [(-8, 4, 22), (6, -8, 30), (10, 10, 18), (-12, -10, 14), (0, 0, 40)]:
        m.box(G, (x - 3, 14, z - 3), (x + 3, 14 + h, z + 3), CRYS if (x + z) % 2 else CRYS2, (0, 30 + x * 3, 0), (x, 14, z))
    return m


# ============================================================================== sortie
def jstr(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')


def lang_update(entries):
    for code, idx in (('fr_fr', 0), ('en_us', 1)):
        path = RES / f'lang/{code}.json'
        data = json.loads(path.read_text(encoding='utf-8'))
        for k, names in entries.items():
            data[k] = names[idx]
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def main():
    args = sys.argv[1:]
    prev = None
    if '--preview' in args:
        i = args.index('--preview'); prev = Path(args[i + 1]); args = args[:i] + args[i + 2:]
    only = set(args)
    (ROOT / 'bbmodels').mkdir(exist_ok=True)
    (RES / 'bbmodels/props').mkdir(parents=True, exist_ok=True)
    (RES / 'textures/imported/props').mkdir(parents=True, exist_ok=True)
    (RES / 'textures/item').mkdir(parents=True, exist_ok=True)
    langs = {}
    rows = []
    for key, (f, fr, en, shape, light) in MODELS.items():
        langs[f'block.mmorpg.{key}'] = (fr, en)
        rows.append((key, shape, light))
        if only and key not in only:
            continue
        m = f()
        tex, _ = m.build([])
        data = L.to_bbmodel(m, tex, [])
        (ROOT / 'bbmodels' / f'{key}.bbmodel').write_text(json.dumps(data, separators=(',', ':')), encoding='utf-8')
        model_json, png = L.bbmodel_to_runtime(data)
        (RES / f'bbmodels/props/{key}.json').write_text(json.dumps(model_json, separators=(',', ':')), encoding='utf-8')
        (RES / f'textures/imported/props/{key}.png').write_bytes(png)
        hts = max(b['to'][1] for b in m.boxes)
        wid = max(max(abs(b['from'][0]), abs(b['to'][0]), abs(b['from'][2]), abs(b['to'][2])) for b in m.boxes) * 2
        print(f'{key}: {len(m.boxes)} cubes, {wid:.0f} x {hts:.0f} u ({wid / CITADEL_BLOCK:.1f} x {hts / CITADEL_BLOCK:.1f} blocs citadelle), densite {m.density:.2f}')
        icon = L.render(m, tex, -35, 18, 64)
        icon.save(RES / f'textures/item/{key}.png')
        (RES / f'models/item/{key}.json').write_text(json.dumps({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mmorpg:item/{key}'}}, indent=2), encoding='utf-8')
        (RES / f'items/{key}.json').write_text(json.dumps({'model': {'type': 'minecraft:model', 'model': f'mmorpg:item/{key}'}}, indent=2), encoding='utf-8')
        (RES / f'blockstates/{key}.json').write_text(json.dumps({'variants': {f'facing={d}': {'model': 'mmorpg:block/coffre_donjon'} for d in ('north', 'east', 'south', 'west')}}, separators=(',', ':')), encoding='utf-8')
        loot = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'mmorpg:{key}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}], 'random_sequence': f'mmorpg:blocks/{key}'}
        lp = ROOT / f'src/main/resources/data/mmorpg/loot_table/blocks/{key}.json'
        lp.write_text(json.dumps(loot, separators=(',', ':')), encoding='utf-8')
        if prev:
            prev.mkdir(parents=True, exist_ok=True)
            for name, (yaw, pit) in {'a': (-35, 18), 'b': (180, 12)}.items():
                L.render(m, tex, yaw, pit).save(prev / f'{key}_{name}.png')
    lang_update(langs)
    out = ['package com.mmorpg.registry;', '', 'import java.util.List;', '',
           '/** Genere par tools/gen_decor_bb.py : 30 decors 3D de la citadelle (1 bloc de citadelle = 3 blocs Minecraft). */',
           'public final class DecorCitadelList {', '    private DecorCitadelList() {', '    }', '',
           '    /** id = cle du modele (bbmodels/props/<id>.json), boite de collision en pixels de bloc, niveau de lumiere. */',
           '    public record Entry(String id, double x0, double y0, double z0, double x1, double y1, double z1, int light) {', '    }', '',
           '    public static final List<Entry> ALL = List.of(']
    for i, (key, s, light) in enumerate(rows):
        out.append(f'            new Entry("{key}", {s[0]}, {s[1]}, {s[2]}, {s[3]}, {s[4]}, {s[5]}, {light})' + (',' if i < len(rows) - 1 else ');'))
    out.append('}')
    (ROOT / 'src/main/java/com/mmorpg/registry/DecorCitadelList.java').write_text('\n'.join(out) + '\n', encoding='utf-8')
    print(len(rows), 'decors')


if __name__ == '__main__':
    main()
