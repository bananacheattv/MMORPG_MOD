#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Six batons de mage (niveaux 1, 20, 40, 60, 80, 100) pour Blockbench, format "Java Block/Item".

Usage :  python generer_batons.py            (Python 3.8+ et Pillow : pip install pillow)

Produit dans ce dossier :
  baton_niv001.bbmodel ... baton_niv100.bbmodel   projets Blockbench editables (texture integree)
  textures/baton_nivXXX.png                       textures pixel art (meme contenu que dans les .bbmodel)
  minecraft/                                      pack de ressources : modeles d'item Java prets a l'emploi (export deja fait)
  apercu/                                         rendus de controle (evolution + 4 vues par baton)

Toute la geometrie respecte les contraintes du format Java Block/Item :
  - uniquement des cuboides, coordonnees comprises entre -16 et 32 sur chaque axe ;
  - une rotation par cube, sur un seul axe, d'angle -45 / -22.5 / 0 / 22.5 / 45 ;
  - UV par face (pas de box UV), une seule texture carree (puissance de deux) ;
  - "light_emission" (0-15) pour les cristaux et les runes lumineuses ;
  - pas de rotation de groupe (les groupes ne servent qu'a organiser le projet).
"""
import base64
import io
import json
import math
import os
import random
import uuid
import zlib

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ICI = os.path.dirname(os.path.abspath(__file__))
NS = 'mmorpg'          # espace de noms des textures/modeles exportes
D = 2                  # texels par pixel de modele (densite de texture)
S2 = math.sqrt(2.0)
ANGLES_OK = (-45.0, -22.5, 0.0, 22.5, 45.0)

# =============================================================================================== palette
PAL = {
    'bois_clair': [(112, 74, 42), (84, 54, 30), (140, 98, 58), (62, 40, 22)],
    'bois':       [(78, 52, 38), (56, 37, 28), (102, 70, 50), (40, 26, 20)],
    'cuir':       [(104, 64, 38), (72, 42, 26), (136, 88, 54), (52, 30, 18)],
    'metal':      [(56, 56, 68), (36, 36, 46), (84, 86, 102), (112, 116, 134)],
    'argent':     [(170, 174, 188), (122, 126, 142), (206, 210, 222), (240, 242, 250)],
    'cristal':    [(92, 96, 236), (58, 44, 168), (146, 156, 255), (222, 230, 255)],
    'gemme':      [(140, 70, 230), (86, 36, 160), (186, 128, 255), (236, 214, 255)],
    'pierre':     [(86, 102, 150), (58, 68, 108), (122, 142, 196), (170, 196, 255)],
    'glow':       [(150, 126, 255), (112, 170, 255), (226, 232, 255)],
}
GLYPHES = [  # runes 3x5 inspirees des futharks (motif commun de la famille)
    ['#.#', '##.', '#.#', '##.', '#..'],
    ['#.#', '#.#', '.#.', '.#.', '.#.'],
    ['#..', '##.', '#.#', '##.', '#..'],
    ['.#.', '#.#', '.#.', '.#.', '.#.'],
    ['.#.', '#.#', '.#.', '#.#', '#.#'],
    ['##.', '#.#', '##.', '#.#', '#.#'],
    ['#.#', '.#.', '#.#', '.#.', '#.#'],
]
EMBLEME = ['..#..', '.###.', '##.##', '.###.', '..#..']   # rune d'Eldoria (losange evide), presente sur les six batons


def crc(*key):
    return zlib.crc32(repr(key).encode('utf-8'))


def lerp(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a + (b - a) * t)) for a, b in zip(c1, c2))


def mul(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


# =============================================================================================== geometrie
class Part:
    def __init__(self, name, group, frm, to, mat, rot=None, light=0, faces=None, deco=None):
        self.name, self.group, self.mat = name, group, mat
        self.frm = [round(v, 4) for v in frm]
        self.to = [round(v, 4) for v in to]
        self.rot = rot            # (axe, angle, origine) ou None
        self.light = light
        self.faces = faces        # ensemble de faces a garder (None = toutes)
        self.deco = deco or {}    # face -> decoration peinte par-dessus la matiere
        self.uv = {}              # face -> (u, v, w, h) dans la texture


class Staff:
    def __init__(self, key, level, titre):
        self.key, self.level, self.titre = key, level, titre
        self.parts = []
        self.names = set()

    def add(self, p):
        base, n = p.name, 2
        while p.name in self.names:
            p.name = f'{base}_{n}'
            n += 1
        self.names.add(p.name)
        if p.rot is not None:
            ax, ang, org = p.rot
            assert ax in 'xyz' and any(abs(ang - a) < 1e-6 for a in ANGLES_OK), (p.name, p.rot)
            p.rot = (ax, float(ang), [round(v, 4) for v in org])
        for v in p.frm + p.to:
            assert -16 <= v <= 32, (self.key, p.name, p.frm, p.to)
        self.parts.append(p)
        return p


def box(st, group, name, x1, y1, z1, x2, y2, z2, mat, **kw):
    return st.add(Part(name, group, [min(x1, x2), min(y1, y2), min(z1, z2)], [max(x1, x2), max(y1, y2), max(z1, z2)], mat, **kw))


def cbox(st, group, name, c, size, mat, **kw):
    return box(st, group, name, c[0] - size[0] / 2, c[1] - size[1] / 2, c[2] - size[2] / 2,
               c[0] + size[0] / 2, c[1] + size[1] / 2, c[2] + size[2] / 2, mat, **kw)


def col(st, group, name, y1, y2, w, mat, cx=8.0, cz=8.0, **kw):
    return box(st, group, name, cx - w / 2, y1, cz - w / 2, cx + w / 2, y2, cz + w / 2, mat, **kw)


def oriented(st, group, name, c, angle, length, width, depth, mat, plane='xy', **kw):
    """Cube allonge oriente selon un angle (multiple de 22,5 deg) dans le plan XY (face) ou XZ (horizontal)."""
    a = angle % 360.0
    assert abs(a / 22.5 - round(a / 22.5)) < 1e-6, angle
    theta = ((a + 90.0) % 180.0) - 90.0          # direction ramenee dans ]-90, 90]
    if abs(theta) <= 45.0 + 1e-9:
        long_axis, r = 'u', theta
    else:
        long_axis, r = 'v', (theta - 90.0 if theta > 0 else theta + 90.0)
    cx, cy, cz = c
    if plane == 'xy':
        size = (length, width, depth) if long_axis == 'u' else (width, length, depth)
        rot = ('z', r, [cx, cy, cz]) if abs(r) > 1e-9 else None
    else:  # plan horizontal XZ : u = x, v = -z ; rotation autour de Y
        size = (length, depth, width) if long_axis == 'u' else (width, depth, length)
        rot = ('y', r, [cx, cy, cz]) if abs(r) > 1e-9 else None
    return cbox(st, group, name, c, size, mat, rot=rot, **kw)


def seg(st, group, name, p0, angle, length, width, depth, mat, cz=8.0, ext=None, **kw):
    """Segment partant de p0 (x, y) dans la direction angle (plan de face). Renvoie l'extremite."""
    rad = math.radians(angle)
    p1 = (p0[0] + length * math.cos(rad), p0[1] + length * math.sin(rad))
    e = width * 0.6 if ext is None else ext
    c = ((p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2, cz)
    oriented(st, group, name, c, angle, length + e, width, depth, mat, **kw)
    return p1


def poly(st, group, name, p0, steps, depth, mat, cz=8.0, mirror=False, **kw):
    """Ligne brisee : steps = [(longueur, angle, largeur[, matiere]), ...]. mirror = symetrique gauche/droite."""
    p = (16 - p0[0], p0[1]) if mirror else p0
    for i, step in enumerate(steps):
        length, ang, width = step[0], step[1], step[2]
        m = step[3] if len(step) > 3 else mat
        a = (180.0 - ang) if mirror else ang
        p = seg(st, group, f'{name}_{i + 1}', p, a, length, width, depth, m, cz=cz, **kw)
    return p


def arc(st, group, name, cx, cy, cz, radius, angles, width, depth, mat, plane='xy', **kw):
    """Anneau ou croissant : un segment tangent par angle (multiples de 22,5 deg)."""
    chord = 2 * radius * math.sin(math.radians(11.25))
    for i, a in enumerate(angles):
        w = width[i] if isinstance(width, (list, tuple)) else width
        rad = math.radians(a)
        if plane == 'xy':
            c = (cx + radius * math.cos(rad), cy + radius * math.sin(rad), cz)
        else:
            c = (cx + radius * math.cos(rad), cy, cz - radius * math.sin(rad))
        oriented(st, group, f'{name}_{i + 1}', c, a + 90.0, chord + w * 0.7, w, depth, mat, plane=plane, **kw)


def crystal(st, group, name, cx, cy, cz, w, h, mat='cristal', light=15, side=True):
    """Cristal facette : silhouette hexagonale de face (losanges a 45 deg) et de profil."""
    a = w / S2
    hc = max(0.0, h - w)
    t = w * 0.55
    for suffix, yc in (('haut', cy + hc / 2), ('bas', cy - hc / 2)):
        cbox(st, group, f'{name}_{suffix}', (cx, yc, cz), (a, a, t), mat, rot=('z', 45, [cx, yc, cz]), light=light)
    if hc > 0:
        cbox(st, group, f'{name}_corps', (cx, cy, cz), (w, hc, t), mat, light=light)
    if side:
        w2 = w * 0.8
        a2 = w2 / S2
        hc2 = max(0.0, h - w2)
        t2 = w * 0.5
        for suffix, yc in (('profil_haut', cy + hc2 / 2), ('profil_bas', cy - hc2 / 2)):
            cbox(st, group, f'{name}_{suffix}', (cx, yc, cz), (t2, a2, a2), mat, rot=('x', 45, [cx, yc, cz]), light=light)
        if hc2 > 0:
            cbox(st, group, f'{name}_profil_corps', (cx, cy, cz), (t2, hc2, w2), mat, light=light)


def frag(st, group, name, c, s, axis='z', mat='cristal', light=15):
    """Fragment de cristal flottant (cube pose sur la pointe)."""
    cbox(st, group, name, c, (s, s, s), mat, rot=(axis, 45, list(c)), light=light)


def plate(st, group, name, side, x1, y1, x2, y2, depth_at, mat, light=15, deco=None):
    """Plaque plate (epaisseur nulle) posee devant une face : runes, lignes d'energie, emblemes.
    side = north/south (rectangle x1..x2 / y1..y2, a la profondeur z = depth_at) ou east/west (z1..z2 / y1..y2, x = depth_at)."""
    d = {side: deco} if deco else None
    if side in ('north', 'south'):
        return box(st, group, name, x1, y1, depth_at, x2, y2, depth_at, mat, light=light, faces={side}, deco=d)
    return box(st, group, name, depth_at, y1, x1, depth_at, y2, x2, mat, light=light, faces={side}, deco=d)


def col_plates(st, group, name, y1, y2, wcol, inset, mat, sides='NSEW', light=15, deco=None, gap=0.1):
    """Plaques sur les 4 cotes d'une colonne centree de largeur wcol."""
    h = wcol / 2
    lo, hi = 8 - h + inset, 8 + h - inset
    if 'N' in sides:
        plate(st, group, f'{name}_avant', 'north', lo, y1, hi, y2, 8 - h - gap, mat, light, deco)
    if 'S' in sides:
        plate(st, group, f'{name}_arriere', 'south', lo, y1, hi, y2, 8 + h + gap, mat, light, deco)
    if 'E' in sides:
        plate(st, group, f'{name}_droite', 'east', lo, y1, hi, y2, 8 + h + gap, mat, light, deco)
    if 'W' in sides:
        plate(st, group, f'{name}_gauche', 'west', lo, y1, hi, y2, 8 - h - gap, mat, light, deco)


def diamond_plate(st, group, name, cy, size, gem, cx=8.0, front_z=7.0, back_z=9.0, frame='metal'):
    """Plaque en losange (motif de rune commun) devant et derriere, avec une gemme lumineuse."""
    for side, z0, sgn in (('avant', front_z, -1), ('arriere', back_z, 1)):
        zc = z0 + sgn * 0.3
        cbox(st, group, f'{name}_{side}', (cx, cy, zc), (size, size, 0.6), frame, rot=('z', 45, [cx, cy, zc]),
             deco={'north': 'embleme_argent', 'south': 'embleme_argent'})
        zg = z0 + sgn * 0.75
        cbox(st, group, f'{name}_gemme_{side}', (cx, cy, zg), (gem, gem, 0.3), 'gemme', rot=('z', 45, [cx, cy, zg]), light=12)


def grip(st, mat='cuir', rings=None, strap_tip=None):
    """Poignee commune a toute la famille : cuir de y=0 a y=8 et laniere qui pend sur le cote."""
    col(st, 'poignee', 'cuir', 0, 8, 2.5, mat)
    for i, (y1, y2, w, m) in enumerate(rings or []):
        col(st, 'poignee', f'bague_{i + 1}', y1, y2, w, m)
    end = poly(st, 'poignee', 'laniere', (9.4, 6.6), [(2.2, -67.5, 0.7), (1.8, -90, 0.7)], 0.5, 'cuir', cz=8.0)
    if strap_tip:
        cbox(st, 'poignee', 'laniere_embout', (end[0], end[1] - 0.4, 8.0), (0.9, 0.9, 0.9), strap_tip,
             rot=('y', 45, [end[0], end[1] - 0.4, 8.0]), light=12 if strap_tip == 'cristal' else 0)


# =============================================================================================== les 6 batons
def baton_1():
    st = Staff('baton_niv001', 1, "Bâton d'apprenti")
    m = 'bois_clair'
    col(st, 'manche', 'manche_bas', -12.5, 0, 2, m)
    box(st, 'manche', 'manche_milieu', 6.5, 8, 7, 8.5, 15, 9, m)
    box(st, 'manche', 'noeud_rune', 6.25, 10.5, 6.75, 8.75, 13, 9.25, m, deco={'north': 'grave', 'south': 'grave'})
    box(st, 'manche', 'manche_haut', 7, 15, 7.5, 9, 22, 9.5, m)
    box(st, 'manche', 'bosse', 9, 17.5, 8, 9.5, 19, 9, m)
    grip(st, 'cuir', rings=[(0.5, 1.5, 3.0, 'cuir'), (6.5, 7.5, 3.0, 'cuir')])
    col(st, 'embout', 'bout_use', -13.5, -12.5, 1.5, m)
    box(st, 'tete', 'lien_cuir', 6.75, 21, 7.25, 9.25, 22.5, 9.75, 'cuir')
    poly(st, 'tete/structure', 'branche_gauche', (7.4, 22.0), [(3.0, 112.5, 1.25), (1.8, 90, 1.0), (1.0, 67.5, 0.6)], 1.25, m, cz=8.5)
    poly(st, 'tete/structure', 'branche_droite', (7.4, 22.0), [(3.0, 112.5, 1.25), (1.8, 90, 1.0), (1.0, 67.5, 0.6)], 1.25, m, cz=8.5,
         mirror=True)
    crystal(st, 'tete/cristal', 'pierre', 8, 25.2, 8.5, 2.6, 3.4, mat='pierre', light=9, side=True)
    return st


def baton_20():
    st = Staff('baton_niv020', 20, "Bâton d'initié")
    m = 'bois'
    col(st, 'manche', 'manche_bas', -12, 0, 2, m)
    col(st, 'manche', 'manche_runes', 8, 14, 2, m, deco={'north': 'runes_gravees', 'south': 'runes_gravees'})
    col(st, 'manche', 'manche_haut', 14, 21, 2, m)
    col(st, 'manche', 'renfort_1', 14, 15, 2.5, 'metal')
    col(st, 'manche', 'renfort_2', 18, 18.5, 2.5, 'metal')
    grip(st, 'cuir', rings=[(-0.5, 0, 3.0, 'metal'), (8, 8.5, 3.0, 'metal')])
    col(st, 'embout', 'ferrule', -14, -12, 3, 'metal')
    col(st, 'embout', 'pointe', -15, -14, 1.5, 'argent')
    col(st, 'tete/structure', 'collier', 21, 23.5, 3.5, 'metal', deco={'north': 'grave_bleu', 'south': 'grave_bleu'})
    col(st, 'tete/structure', 'collier_bague', 23.5, 24, 4, 'argent')
    for mirror in (False, True):
        side = 'droite' if mirror else 'gauche'
        poly(st, 'tete/structure', f'griffe_{side}', (6.6, 23.6), [(2.6, 112.5, 1.0), (1.6, 90, 1.0), (1.4, 45, 0.75, 'argent')],
             1.5, 'metal', mirror=mirror)
    crystal(st, 'tete/cristal', 'cristal', 8, 27.2, 8, 3.0, 5.0, light=13)
    return st


def baton_40():
    st = Staff('baton_niv040', 40, 'Bâton de mage')
    m = 'bois'
    col(st, 'manche', 'manche_bas', -11.5, 0, 2, m)
    col(st, 'manche', 'manche_haut', 8, 20, 2, m)
    for i, y in enumerate((9.0, 12.5, 16.0)):
        col(st, 'manche', f'incrustation_{i + 1}', y, y + 0.5, 2.5, 'argent')
        col(st, 'manche', f'sculpture_{i + 1}', y + 0.5, y + 1.5, 2.5, m)
    col_plates(st, 'runes', 'runes_bas', 10.5, 12.5, 2, 0.25, 'rune', sides='NS')
    col_plates(st, 'runes', 'runes_haut', 14.0, 16.0, 2, 0.25, 'rune', sides='NS')
    col_plates(st, 'runes', 'ligne_energie', 8.5, 19.5, 2, 0.75, 'energie', sides='EW')
    diamond_plate(st, 'ornements', 'plaque_rune', 18.3, 2.4, 1.1)
    grip(st, 'cuir_tresse', rings=[(-0.5, 0, 3.0, 'argent'), (3.75, 4.25, 2.75, 'argent'), (8, 8.5, 3.0, 'argent')])
    col(st, 'embout', 'ferrule', -12.5, -10.5, 3, 'metal')
    diamond_plate(st, 'embout', 'plaque_bas', -7.5, 1.8, 0.8)
    col(st, 'embout', 'pointe', -13.5, -12.5, 2, 'argent')
    col(st, 'embout', 'pointe_fine', -15, -13.5, 1, 'argent')
    col(st, 'tete/structure', 'base', 20, 22, 3, 'metal')
    box(st, 'tete/structure', 'evasement', 5.5, 22, 7, 10.5, 23, 9, 'argent')
    box(st, 'tete/structure', 'evasement_profil', 7, 22, 6, 9, 23, 10, 'argent')
    for mirror in (False, True):
        side = 'droite' if mirror else 'gauche'
        poly(st, 'tete/structure', f'branche_{side}', (6.2, 22.6),
             [(3.2, 112.5, 1.25), (2.5, 90, 1.25), (1.8, 67.5, 1.0), (1.4, 67.5, 0.6, 'argent')], 1.25, 'metal', mirror=mirror)
    # runes lumineuses sur la partie verticale des branches
    for x in (16 - 4.98, 4.98):
        lo, hi = x - 0.4, x + 0.4
        plate(st, 'runes', 'rune_branche', 'north', lo, 25.9, hi, 28.0, 7.27, 'rune')
        plate(st, 'runes', 'rune_branche', 'south', lo, 25.9, hi, 28.0, 8.73, 'rune')
    oriented(st, 'tete/structure', 'branche_arriere', (8, 25.0, 10.3), 90, 5.0, 1.0, 1.0, 'metal')
    st.parts[-1].rot = ('x', 22.5, [8, 22.5, 10.3])
    cbox(st, 'tete/structure', 'branche_arriere_pointe', (8, 28.6, 11.6), (0.6, 1.6, 0.6), 'argent', rot=('x', 22.5, [8, 28.6, 11.6]))
    crystal(st, 'tete/cristal', 'cristal', 8, 26.6, 8, 3.6, 6.8)
    return st


def baton_60():
    st = Staff('baton_niv060', 60, "Bâton d'archimage")
    m = 'bois'
    col(st, 'manche', 'manche_bas', -10.5, 0, 2, m)
    col(st, 'manche', 'manche_haut', 8, 20, 2, m)
    for i, (y1, y2) in enumerate(((9.0, 13.0), (14.5, 18.5))):
        col(st, 'manche', f'fourreau_{i + 1}', y1, y2, 3, 'metal')
        col(st, 'manche', f'fourreau_{i + 1}_bord_bas', y1, y1 + 0.5, 3.25, 'argent')
        col(st, 'manche', f'fourreau_{i + 1}_bord_haut', y2 - 0.5, y2, 3.25, 'argent')
        col_plates(st, 'runes', f'runes_fourreau_{i + 1}', y1 + 0.75, y2 - 0.75, 3, 0.5, 'rune',
                   deco='embleme' if i == 1 else None)
    grip(st, 'cuir_tresse', rings=[(-0.75, 0, 3.0, 'metal'), (-0.25, 0.25, 3.25, 'argent'), (8, 8.75, 3.0, 'metal'),
                                   (8.25, 8.75, 3.25, 'argent')], strap_tip='argent')
    col(st, 'embout', 'ferrule', -12, -10, 3, 'metal')
    col(st, 'embout', 'ferrule_bague', -10.5, -10, 3.25, 'argent')
    col(st, 'embout', 'pointe_1', -13, -12, 2, 'argent')
    col(st, 'embout', 'pointe_2', -14, -13, 1.4, 'argent')
    col(st, 'embout', 'pointe_3', -15, -14, 0.7, 'argent')
    col(st, 'tete/structure', 'collier', 19, 21, 4, 'metal')
    col(st, 'tete/structure', 'collier_bague', 20.5, 21, 4.5, 'argent')
    col(st, 'tete/structure', 'cou', 21, 22.5, 3, 'metal')
    # croissant brise : epais en bas a gauche, fin aux pointes, ouvert en haut a droite
    angles = [67.5, 90, 112.5, 135, 157.5, 180, 202.5, 225, 247.5]
    widths = [0.8, 1.1, 1.4, 1.7, 2.0, 2.0, 1.8, 1.4, 1.0]
    arc(st, 'tete/structure', 'croissant', 8, 26.2, 8, 5.0, angles, widths, 1.5, 'argent')
    arc(st, 'tete/structure', 'croissant_interieur', 8, 26.2, 8, 3.8, [135, 157.5, 180, 202.5], 0.6, 1.0, 'metal')
    arc(st, 'runes', 'croissant_energie', 8, 26.2, 7.15, 5.0, [135, 157.5, 180, 202.5], 0.5, 0.2, 'energie', light=15)
    seg(st, 'tete/structure', 'croissant_pointe', (8 + 5.0 * math.cos(math.radians(67.5)), 26.2 + 5.0 * math.sin(math.radians(67.5))),
        -22.5, 1.6, 0.6, 1.0, 'argent')
    arc(st, 'tete/structure', 'croissant_brise', 8, 26.2, 8, 5.0, [292.5, 315], [0.9, 0.7], 1.2, 'argent')
    cbox(st, 'elements_flottants', 'eclat_croissant', (8 + 6.3 * math.cos(math.radians(342)), 26.2 + 6.3 * math.sin(math.radians(342)), 8),
         (0.8, 1.6, 0.8), 'argent', rot=('z', -22.5, [8 + 6.3 * math.cos(math.radians(342)), 26.2 + 6.3 * math.sin(math.radians(342)), 8]))
    for mirror in (False, True):
        poly(st, 'tete/structure', 'griffe_' + ('droite' if mirror else 'gauche'), (6.8, 22.4), [(1.6, 112.5, 0.8), (1.2, 90, 0.6)],
             1.0, 'metal', mirror=mirror)
    crystal(st, 'tete/cristal', 'cristal', 8, 26.6, 8, 3.2, 8.2)
    for i, (c, s, ax) in enumerate((((1.8, 24.0, 8), 1.0, 'z'), ((14.6, 29.2, 8), 1.0, 'x'), ((13.8, 22.6, 8), 0.8, 'z'),
                                    ((3.0, 31.0, 8), 0.8, 'x'), ((8, 29.5, 3.2), 0.8, 'z'), ((8, 24.5, 12.8), 0.8, 'x'))):
        frag(st, 'elements_flottants', f'fragment_{i + 1}', c, s, ax)
    return st


def baton_80():
    st = Staff('baton_niv080', 80, 'Bâton légendaire')
    m = 'bois'
    col(st, 'manche', 'manche_bas', -10.5, 0, 2, m)
    col(st, 'manche', 'manche_haut', 8, 19, 2, m)
    for i, (y1, y2) in enumerate(((9.0, 11.5), (12.5, 15.0), (16.0, 18.5))):
        col(st, 'manche', f'fourreau_{i + 1}', y1, y2, 3, 'metal')
        col(st, 'manche', f'fourreau_{i + 1}_bord', y2 - 0.5, y2, 3.25, 'argent')
        col_plates(st, 'runes', f'runes_fourreau_{i + 1}', y1 + 0.4, y2 - 0.9, 3, 0.5, 'rune',
                   deco='embleme' if i == 1 else None)
    for y1, y2 in ((8.5, 9.0), (11.5, 12.5), (15.0, 16.0), (18.5, 19.0)):
        col_plates(st, 'runes', 'canal', y1, y2, 2, 0.75, 'energie', sides='NSEW')
    grip(st, 'cuir_tresse', rings=[(-0.75, 0, 3.0, 'metal'), (-0.25, 0.25, 3.25, 'argent'), (3.75, 4.25, 2.75, 'argent'),
                                   (8, 8.75, 3.0, 'metal'), (8.25, 8.75, 3.25, 'argent')], strap_tip='argent')
    col(st, 'embout', 'griffes_base', -10.5, -9, 3.5, 'metal')
    col(st, 'embout', 'griffes_bague', -9.5, -9, 3.75, 'argent')
    for mirror in (False, True):
        poly(st, 'embout', 'griffe_bas_' + ('droite' if mirror else 'gauche'), (6.6, -10.2), [(1.8, 247.5, 0.7), (1.0, 270, 0.6, 'argent')],
             1.0, 'metal', mirror=mirror)
    crystal(st, 'embout', 'cristal_pointe', 8, -12.6, 8, 2.0, 4.2, side=True)
    col(st, 'tete/structure', 'collier', 19, 21, 4, 'metal')
    col(st, 'tete/structure', 'collier_bague', 20.5, 21, 4.5, 'argent')
    col(st, 'tete/structure', 'cou', 21, 22, 2.5, 'metal')
    # couronne basse asymetrique
    poly(st, 'tete/couronne_basse', 'dent_gauche', (6.2, 21.6), [(3.0, 112.5, 1.25), (3.0, 90, 1.1), (1.6, 67.5, 0.9), (1.2, 45, 0.6, 'argent')],
         1.25, 'metal')
    poly(st, 'tete/couronne_basse', 'dent_droite', (9.8, 21.6), [(2.4, 67.5, 1.1), (2.0, 90, 1.0), (1.4, 112.5, 0.6, 'argent')], 1.25, 'metal')
    oriented(st, 'tete/couronne_basse', 'dent_arriere', (8, 24.0, 10.2), 90, 3.6, 1.0, 1.0, 'metal')
    st.parts[-1].rot = ('x', 22.5, [8, 22.2, 10.2])
    oriented(st, 'tete/couronne_basse', 'dent_avant', (8, 23.4, 5.9), 90, 2.4, 0.9, 0.9, 'metal')
    st.parts[-1].rot = ('x', -22.5, [8, 22.2, 5.9])
    # couronne haute (flottante, decalee)
    arc(st, 'tete/couronne_haute', 'arc_droit', 8, 26.6, 8, 5.2, [22.5, 45, 67.5], [0.7, 0.9, 1.1], 1.2, 'argent')
    arc(st, 'tete/couronne_haute', 'arc_gauche', 8, 26.6, 8, 5.2, [112.5, 135], [1.0, 0.7], 1.2, 'argent')
    cbox(st, 'tete/couronne_haute', 'pointe_haute', (8.9, 31.0, 8), (0.7, 1.6, 0.7), 'argent', rot=('z', -22.5, [8.9, 31.0, 8]))
    crystal(st, 'tete/cristal', 'noyau', 8, 26.4, 8, 3.8, 7.6)
    # segments metalliques flottants qui prolongent la structure + liens d'energie
    poly(st, 'elements_flottants', 'segment_gauche', (2.3, 22.4), [(2.6, 90, 1.0), (1.6, 67.5, 0.8), (1.0, 67.5, 0.5, 'argent')], 1.0, 'metal')
    poly(st, 'elements_flottants', 'segment_droit', (13.9, 23.2), [(2.2, 90, 1.0), (1.4, 112.5, 0.8), (0.9, 112.5, 0.5, 'argent')], 1.0, 'metal')
    box(st, 'runes', 'lien_gauche', 2.8, 24.2, 7.75, 4.95, 24.6, 8.25, 'lien', light=15)
    box(st, 'runes', 'lien_droit', 11.2, 24.6, 7.75, 13.4, 25.0, 8.25, 'lien', light=15)
    box(st, 'runes', 'lien_haut', 7.75, 30.2, 7.75, 8.25, 31.0, 8.25, 'lien', light=15)
    for i, (c, s, ax) in enumerate((((1.2, 28.8, 8), 0.9, 'z'), ((15.0, 28.4, 8), 0.9, 'x'), ((12.6, 31.0, 8), 0.7, 'z'),
                                    ((8, 27.5, 2.8), 0.8, 'x'), ((8, 25.0, 13.2), 0.8, 'z'))):
        frag(st, 'elements_flottants', f'fragment_{i + 1}', c, s, ax)
    return st


def baton_100():
    st = Staff('baton_niv100', 100, 'Bâton mythique')
    m = 'bois'
    col(st, 'manche', 'manche_bas', -3.5, 0, 2, m)
    col(st, 'manche', 'manche_haut', 8, 18.5, 2, m)
    for i, (y1, y2) in enumerate(((9.0, 11.0), (12.0, 14.5))):
        col(st, 'manche', f'fourreau_{i + 1}', y1, y2, 3, 'metal')
        col(st, 'manche', f'fourreau_{i + 1}_bord', y1, y1 + 0.5, 3.25, 'argent')
        col_plates(st, 'runes', f'runes_fourreau_{i + 1}', y1 + 0.6, y2 - 0.2, 3, 0.5, 'rune')
    for y1, y2 in ((8.5, 9.0), (11.0, 12.0), (14.5, 18.5)):
        col_plates(st, 'runes', 'canal_energie', y1, y2, 2, 0.75, 'energie', sides='NSEW')
    diamond_plate(st, 'ornements', 'embleme_central', 16.6, 3.0, 1.5, frame='argent')
    grip(st, 'cuir_tresse', rings=[(-0.75, 0, 3.0, 'metal'), (-0.25, 0.25, 3.25, 'argent'), (2.5, 3, 2.75, 'argent'),
                                   (5, 5.5, 2.75, 'argent'), (8, 8.75, 3.0, 'metal'), (8.25, 8.75, 3.25, 'argent')], strap_tip='cristal')
    # pommeau et pointe inferieure
    col(st, 'embout', 'pommeau', -7, -3.5, 4, 'metal')
    col(st, 'embout', 'pommeau_bague_haut', -4, -3.5, 4.5, 'argent')
    col(st, 'embout', 'pommeau_bague_bas', -7.5, -7, 4.5, 'argent')
    col_plates(st, 'runes', 'pommeau_embleme', -6.3, -4.2, 4, 0.9, 'rune', deco='embleme')
    for mirror in (False, True):
        poly(st, 'embout', 'eperon_' + ('droit' if mirror else 'gauche'), (6.2, -5.3), [(1.8, 202.5, 0.8), (1.0, 247.5, 0.5, 'argent')],
             0.8, 'metal', mirror=mirror)
        poly(st, 'embout', 'griffe_bas_' + ('droite' if mirror else 'gauche'), (6.8, -7.8), [(1.6, 247.5, 0.7), (1.0, 270, 0.5, 'argent')],
             0.9, 'metal', mirror=mirror)
    col(st, 'embout', 'cou_bas', -8, -7.5, 2.5, 'metal')
    crystal(st, 'embout', 'cristal_pointe', 8, -11.4, 8, 2.4, 5.0)
    # berceau sculpte
    col(st, 'tete/structure', 'berceau_1', 18.5, 20, 3.5, 'metal')
    box(st, 'tete/structure', 'berceau_2', 5.5, 20, 6.25, 10.5, 21, 9.75, 'metal')
    box(st, 'tete/structure', 'berceau_3', 4.0, 21, 7.0, 12.0, 22, 9.0, 'argent')
    box(st, 'tete/structure', 'berceau_profil', 7.0, 21, 5.0, 9.0, 22, 11.0, 'argent')
    # lames ornementales
    for mirror in (False, True):
        side = 'droite' if mirror else 'gauche'
        poly(st, 'tete/lames', f'lame_{side}', (5.0, 21.6),
             [(3.0, 135, 1.5), (3.0, 112.5, 1.5), (2.4, 90, 1.3), (2.4, 67.5, 1.0), (1.2, 45, 0.6)], 1.2, 'argent', mirror=mirror)
        poly(st, 'tete/lames', f'contrefort_{side}', (6.4, 22.0), [(2.0, 112.5, 0.9), (2.4, 90, 0.8)], 1.0, 'metal', mirror=mirror)
    for x in (1.73, 16 - 1.73):
        plate(st, 'runes', 'rune_lame', 'north', x - 0.4, 26.6, x + 0.4, 28.9, 7.3, 'rune')
        plate(st, 'runes', 'rune_lame', 'south', x - 0.4, 26.6, x + 0.4, 28.9, 8.7, 'rune')
    crystal(st, 'tete/cristal', 'grand_cristal', 8, 27.0, 8, 4.6, 9.0)
    # anneaux orbitaux brises
    arc(st, 'tete/anneaux_orbitaux', 'orbite', 8, 26.2, 8, 6.6, [0, 22.5, 45, 135, 157.5, 180, 270, 292.5, 315], 0.7, 0.7, 'argent',
        plane='xz')
    arc(st, 'tete/anneaux_orbitaux', 'orbite_energie', 8, 26.6, 8, 6.0, [22.5, 45, 202.5, 225], 0.45, 0.45, 'energie', light=15)
    for i, (c, s, ax) in enumerate((((0.6, 24.2, 8), 1.0, 'z'), ((15.4, 30.0, 8), 1.0, 'x'), ((1.4, 31.0, 8), 0.8, 'x'),
                                    ((14.6, 22.4, 8), 0.8, 'z'), ((8, 30.6, 2.0), 0.9, 'z'), ((8, 23.4, 14.2), 0.9, 'x'),
                                    ((4.0, 30.8, 12.0), 0.6, 'z'), ((12.2, 23.8, 3.6), 0.6, 'x'))):
        frag(st, 'elements_flottants', f'fragment_{i + 1}', c, s, ax)
    return st


BATONS = [baton_1, baton_20, baton_40, baton_60, baton_80, baton_100]

# =============================================================================================== textures
FACES = ('north', 'east', 'south', 'west', 'up', 'down')


def face_dims(p, f):
    sx, sy, sz = (p.to[i] - p.frm[i] for i in range(3))
    return {'north': (sx, sy), 'south': (sx, sy), 'east': (sz, sy), 'west': (sz, sy), 'up': (sx, sz), 'down': (sx, sz)}[f]


def tex_size(v):
    return max(1, int(round(v * D)))


def put(px, x, y, c, a=255):
    px[x, y] = (c[0], c[1], c[2], a)


def paint_base(mat, w, h, side, r, smooth=False):
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = img.load()
    if mat in ('bois', 'bois_clair'):
        base, dark, light, deep = PAL[mat]
        if side and smooth:
            for x in range(w):
                for y in range(h):
                    put(px, x, y, mul(base, 1 + r.uniform(-0.04, 0.04)))
        elif side:
            vertical = h >= w
            for i in range(w if vertical else h):
                tone = r.choice([base, base, base, dark, light])
                for j in range(h if vertical else w):
                    if r.random() < 0.14:
                        tone = r.choice([base, base, dark, light])
                    put(px, *((i, j) if vertical else (j, i)), tone)
            if w * h >= 30 and r.random() < 0.7:
                kx, ky = r.randrange(w), r.randrange(h)
                put(px, kx, ky, deep)
        else:
            cx, cy = (w - 1) / 2, (h - 1) / 2
            for x in range(w):
                for y in range(h):
                    d = math.hypot(x - cx, y - cy)
                    put(px, x, y, light if int(d * 1.3) % 2 == 0 else base)
    elif mat in ('cuir', 'cuir_tresse'):
        base, dark, light, deep = PAL['cuir']
        for x in range(w):
            for y in range(h):
                if mat == 'cuir':
                    k = (2 * y + x) % 6
                else:
                    k = (2 * y + x) % 6 if (y // 3) % 2 == 0 else (2 * y - x) % 6
                c = deep if k == 0 else dark if k == 1 else light if k == 3 else base
                if r.random() < 0.06:
                    c = mul(c, 1.1)
                put(px, x, y, c)
    elif mat in ('metal', 'argent'):
        base, dark, light, hi = PAL[mat]
        for x in range(w):
            for y in range(h):
                put(px, x, y, mul(base, 1 + r.uniform(-0.05, 0.05)))
        if w >= 3:
            for y in range(h):
                put(px, 0, y, light)
                put(px, w - 1, y, dark)
        if h >= 3:
            for x in range(w):
                put(px, x, 0, hi if mat == 'argent' else light)
                put(px, x, h - 1, dark)
        if w >= 6 and h >= 6:
            for x, y in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
                put(px, x, y, hi)
        if mat == 'argent' and w * h > 8:
            put(px, r.randrange(w), r.randrange(h), hi)
    elif mat in ('cristal', 'gemme', 'pierre'):
        base, dark, light, hi = PAL[mat]
        n = max(1, w + h - 2)
        for x in range(w):
            for y in range(h):
                t = (x + y) / n
                c = lerp(light, base, t * 1.4) if t < 0.5 else lerp(base, dark, (t - 0.5) * 2)
                if mat == 'pierre':
                    c = mul(c, 1 + r.uniform(-0.12, 0.12))
                put(px, x, y, c)
        if mat != 'pierre':
            if w >= 3:
                for y in range(h):
                    put(px, w // 2, y, lerp(px[w // 2, y][:3], hi, 0.35))
            put(px, 0, 0, hi)
            if w >= 3 and h >= 3:
                put(px, 1, 1, hi)
        else:
            put(px, r.randrange(w), r.randrange(h), hi)
    elif mat in ('rune',):
        pass   # fond transparent : les glyphes sont ajoutes par la decoration
    elif mat in ('energie', 'lien'):
        main, alt, core = PAL['glow']
        alpha = 255 if mat == 'energie' else 170
        long_v = h >= w
        for x in range(w):
            for y in range(h):
                i = y if long_v else x
                k = 0.5 + 0.5 * math.sin(i * 0.9)
                c = lerp(main, alt, k)
                across = (x if long_v else y)
                span = w if long_v else h
                if span >= 3 and across == span // 2:
                    c = lerp(c, core, 0.6)
                put(px, x, y, c, alpha)
    else:
        raise ValueError(mat)
    return img


def draw_pattern(img, pattern, x0, y0, color_fn):
    px = img.load()
    for j, row in enumerate(pattern):
        for i, ch in enumerate(row):
            x, y = x0 + i, y0 + j
            if ch == '#' and 0 <= x < img.width and 0 <= y < img.height:
                px[x, y] = color_fn(x, y, px[x, y])


def glow_color(r):
    main, alt, core = PAL['glow']
    return lambda x, y, old: lerp(main, core, r.uniform(0.15, 0.6)) + (255,)


def apply_deco(img, deco, r, mat):
    w, h = img.size
    if not deco:
        return img
    if deco in ('glyphes', 'embleme'):
        if deco == 'embleme' and w >= 5 and h >= 5:
            draw_pattern(img, EMBLEME, (w - 5) // 2, (h - 5) // 2, glow_color(r))
            return img
        if h >= w:
            n = max(1, (h + 1) // 6)
            used = n * 6 - 1
            y = (h - used) // 2
            for k in range(n):
                g = GLYPHES[r.randrange(len(GLYPHES))]
                if w >= 3:
                    draw_pattern(img, g, (w - 3) // 2, y, glow_color(r))
                else:
                    draw_pattern(img, ['#'] * 5, (w - 1) // 2, y, glow_color(r))
                y += 6
        else:
            n = max(1, (w + 1) // 4)
            x = (w - (n * 4 - 1)) // 2
            for k in range(n):
                g = GLYPHES[r.randrange(len(GLYPHES))]
                draw_pattern(img, [row for row in g[:min(5, h)]], x, max(0, (h - 5) // 2), glow_color(r))
                x += 4
        return img
    if deco in ('grave', 'grave_bleu', 'embleme_argent'):
        if w < 5 or h < 5:
            return img
        x0, y0 = (w - 5) // 2, (h - 5) // 2
        if deco == 'grave':
            fn = lambda x, y, old: PAL['bois_clair'][3] + (255,)
        elif deco == 'grave_bleu':
            fn = lambda x, y, old: (110, 130, 225, 255)
        else:
            fn = lambda x, y, old: PAL['argent'][2] + (255,)
        draw_pattern(img, EMBLEME, x0, y0, fn)
        return img
    if deco == 'runes_gravees':
        y = 1
        while y + 5 <= h:
            g = GLYPHES[r.randrange(len(GLYPHES))]
            draw_pattern(img, g, (w - 3) // 2, y, lambda x, yy, old: lerp(mul(old[:3], 0.55), (80, 100, 200), 0.5) + (255,))
            y += 7
        return img
    raise ValueError(deco)


def build_texture(st):
    """Peint chaque face et range les motifs dans une texture carree (les faces identiques partagent leurs UV)."""
    cache = {}
    regions = []
    for p in st.parts:
        for f in FACES:
            if p.faces is not None and f not in p.faces:
                continue
            fw, fh = face_dims(p, f)
            if fw <= 1e-6 or fh <= 1e-6:
                continue
            w, h = tex_size(fw), tex_size(fh)
            side = f not in ('up', 'down')
            deco = p.deco.get(f)
            if p.mat == 'rune' and not deco:
                deco = 'glyphes'
            variant = crc(p.name) % 3 if p.mat == 'rune' else 0
            key = (p.mat, deco, side, w, h, variant)
            if key not in cache:
                r = random.Random(crc(st.key, key))
                img = apply_deco(paint_base(p.mat, w, h, side, r, smooth=deco in ('grave', 'grave_bleu')), deco, r, p.mat)
                cache[key] = len(regions)
                regions.append([img, None])
            p.uv[f] = cache[key]
    # rangement par etageres
    order = sorted(range(len(regions)), key=lambda i: (-regions[i][0].height, -regions[i][0].width))
    for size in (32, 64, 128, 256, 512):
        x = y = shelf = 0
        ok = True
        pos = {}
        for i in order:
            w, h = regions[i][0].size
            if x + w > size:
                x, y, shelf = 0, y + shelf, 0
            if y + h > size or w > size:
                ok = False
                break
            pos[i] = (x, y)
            x += w
            shelf = max(shelf, h)
        if ok:
            break
    atlas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for i, (img, _) in enumerate(regions):
        atlas.paste(img, pos[i])
        regions[i][1] = pos[i]
    for p in st.parts:
        for f, i in list(p.uv.items()):
            (x, y), (w, h) = regions[i][1], regions[i][0].size
            p.uv[f] = (x, y, w, h)
    return atlas


# =============================================================================================== export
GROUP_COLORS = {'manche': 1, 'poignee': 2, 'embout': 3, 'tete': 4, 'runes': 5, 'ornements': 6, 'elements_flottants': 7}

DISPLAY = {
    'thirdperson_righthand': {'rotation': [90, 0, 0], 'translation': [0, 1.5, 2.6], 'scale': [0.62, 0.62, 0.62]},
    'thirdperson_lefthand': {'rotation': [90, 0, 0], 'translation': [0, 1.5, 2.6], 'scale': [0.62, 0.62, 0.62]},
    'firstperson_righthand': {'rotation': [0, -20, 10], 'translation': [1.5, 2.5, 0.0], 'scale': [0.34, 0.34, 0.34]},
    'firstperson_lefthand': {'rotation': [0, -20, 10], 'translation': [1.5, 2.5, 0.0], 'scale': [0.34, 0.34, 0.34]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.3, 0.3, 0.3]},
    'gui': {'rotation': [0, 0, -45], 'translation': [-0.5, -0.5, 0], 'scale': [0.42, 0.42, 0.42]},
    'head': {'rotation': [0, 0, 0], 'translation': [0, 6, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 0, -45], 'translation': [-0.5, -0.5, 0], 'scale': [0.45, 0.45, 0.45]},
}


def uid(*key):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'eldoria/' + '/'.join(map(str, key))))


def rot_vec(p):
    if p.rot is None:
        return [0, 0, 0]
    ax, ang, _ = p.rot
    return [ang if ax == 'x' else 0, ang if ax == 'y' else 0, ang if ax == 'z' else 0]


def origin(p):
    if p.rot is not None:
        return p.rot[2]
    return [round((a + b) / 2, 4) for a, b in zip(p.frm, p.to)]


def group_tree(st):
    """Arborescence des groupes a partir des chemins 'tete/cristal'."""
    root = []
    index = {}
    for i, p in enumerate(st.parts):
        path = p.group.split('/')
        siblings, prefix = root, ''
        for name in path:
            prefix = f'{prefix}/{name}' if prefix else name
            if prefix not in index:
                node = {'name': name, 'path': prefix, 'children': []}
                index[prefix] = node
                siblings.append(node)
            siblings = index[prefix]['children']
        siblings.append(i)
    return root


def bbmodel(st, atlas):
    size = atlas.width
    buf = io.BytesIO()
    atlas.save(buf, 'PNG')
    elements = []
    for i, p in enumerate(st.parts):
        faces = {}
        for f in FACES:
            if f in p.uv:
                x, y, w, h = p.uv[f]
                faces[f] = {'uv': [x, y, x + w, y + h], 'texture': 0}
            else:
                faces[f] = {'uv': [0, 0, 0, 0], 'texture': None}
        top = p.group.split('/')[0]
        el = {
            'name': p.name, 'box_uv': False, 'rescale': False, 'locked': False, 'light_emission': p.light,
            'render_order': 'default', 'allow_mirror_modeling': True,
            'from': p.frm, 'to': p.to, 'autouv': 0, 'color': GROUP_COLORS.get(top, 0),
            'origin': origin(p), 'faces': faces, 'type': 'cube', 'uuid': uid(st.key, 'el', i, p.name),
        }
        if p.rot is not None:
            el['rotation'] = rot_vec(p)
        elements.append(el)

    def outline(nodes):
        out = []
        for n in nodes:
            if isinstance(n, int):
                out.append(elements[n]['uuid'])
            else:
                top = n['path'].split('/')[0]
                out.append({'name': n['name'], 'origin': [8, 8, 8], 'color': GROUP_COLORS.get(top, 0),
                            'uuid': uid(st.key, 'grp', n['path']), 'export': True, 'mirror_uv': False,
                            'isOpen': n['path'].count('/') == 0, 'locked': False, 'visibility': True, 'autouv': 0,
                            'children': outline(n['children'])})
        return out

    return {
        'meta': {'format_version': '4.10', 'model_format': 'java_block', 'box_uv': False},
        'name': st.key,
        'parent': '',
        'ambientocclusion': True,
        'front_gui_light': True,
        'visible_box': [1, 1, 0],
        'variable_placeholders': '',
        'variable_placeholder_buttons': [],
        'timeline_setups': [],
        'unhandled_root_fields': {},
        'resolution': {'width': size, 'height': size},
        'elements': elements,
        'outliner': outline(group_tree(st)),
        'textures': [{
            'path': '', 'name': f'{st.key}.png', 'folder': 'item', 'namespace': NS, 'id': '0', 'group': '',
            'width': size, 'height': size, 'uv_width': size, 'uv_height': size,
            'particle': True, 'use_as_default': False, 'layers_enabled': False, 'sync_to_project': '',
            'render_mode': 'default', 'render_sides': 'auto', 'pbr_channel': 'color',
            'frame_time': 1, 'frame_order_type': 'loop', 'frame_order': '', 'frame_interpolate': False,
            'visible': True, 'internal': True, 'saved': False, 'uuid': uid(st.key, 'tex'),
            'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode('ascii'),
        }],
        'display': DISPLAY,
    }


def java_model(st, atlas):
    size = atlas.width
    k = 16.0 / size
    elements = []
    for p in st.parts:
        faces = {}
        for f in FACES:
            if f in p.uv:
                x, y, w, h = p.uv[f]
                faces[f] = {'uv': [round(x * k, 4), round(y * k, 4), round((x + w) * k, 4), round((y + h) * k, 4)], 'texture': '#0'}
        el = {'name': p.name, 'from': p.frm, 'to': p.to}
        if p.rot is not None:
            el['rotation'] = {'angle': p.rot[1], 'axis': p.rot[0], 'origin': p.rot[2]}
        if p.light:
            el['light_emission'] = p.light
        el['faces'] = faces
        elements.append(el)

    def groups(nodes):
        out = []
        for n in nodes:
            if isinstance(n, int):
                out.append(n)
            else:
                out.append({'name': n['name'], 'origin': [8, 8, 8], 'color': GROUP_COLORS.get(n['path'].split('/')[0], 0),
                            'children': groups(n['children'])})
        return out

    tex = f'{NS}:item/{st.key}'
    return {
        'credit': 'Eldoria MMORPG - genere par generer_batons.py (Blockbench, format Java Block/Item)',
        'texture_size': [size, size],
        'textures': {'0': tex, 'particle': tex},
        'elements': elements,
        'gui_light': 'front',
        'display': DISPLAY,
        'groups': groups(group_tree(st)),
    }


# =============================================================================================== apercu (rendu logiciel)
def rot_point(v, ax, ang, org):
    a = math.radians(ang)
    c, s = math.cos(a), math.sin(a)
    x, y, z = v[0] - org[0], v[1] - org[1], v[2] - org[2]
    if ax == 'x':
        y, z = y * c - z * s, y * s + z * c
    elif ax == 'y':
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + org[0], y + org[1], z + org[2])


def face_corners(p, f):
    (x1, y1, z1), (x2, y2, z2) = p.frm, p.to
    return {  # (haut-gauche, haut-droit, bas-gauche) vus de l'exterieur = conventions UV de Minecraft
        'north': ((x2, y2, z1), (x1, y2, z1), (x2, y1, z1)),
        'south': ((x1, y2, z2), (x2, y2, z2), (x1, y1, z2)),
        'east': ((x2, y2, z2), (x2, y2, z1), (x2, y1, z2)),
        'west': ((x1, y2, z1), (x1, y2, z2), (x1, y1, z1)),
        'up': ((x1, y2, z1), (x2, y2, z1), (x1, y2, z2)),
        'down': ((x1, y1, z2), (x2, y1, z2), (x1, y1, z1)),
    }[f]


def render(st, atlas, yaw, pitch, scale, glow=True):
    cy, sy_ = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    cam = (sy_ * cp, sp, cy * cp)                       # direction vers la camera
    right = (cy, 0.0, -sy_)
    up = (-sy_ * sp, cp, -cy * sp)
    light_dir = (0.35, 0.85, 0.55)
    ln = math.sqrt(sum(v * v for v in light_dir))
    light_dir = tuple(v / ln for v in light_dir)
    tex = atlas.load()
    quads = []

    def tr(v):
        return v if p.rot is None else rot_point(v, *p.rot)

    for p in st.parts:
        for f, (u0, v0, w, h) in p.uv.items():
            tl, trr, bl = (tr(c) for c in face_corners(p, f))
            du = [(trr[i] - tl[i]) / w for i in range(3)]
            dv = [(bl[i] - tl[i]) / h for i in range(3)]
            n = (du[1] * dv[2] - du[2] * dv[1], du[2] * dv[0] - du[0] * dv[2], du[0] * dv[1] - du[1] * dv[0])
            nl = math.sqrt(sum(v * v for v in n)) or 1
            n = tuple(-v / nl for v in n)
            if sum(n[i] * cam[i] for i in range(3)) <= 1e-6:
                continue
            shade = 1.0 if p.light >= 10 else 0.55 + 0.45 * max(0.0, sum(n[i] * light_dir[i] for i in range(3)))
            for j in range(h):
                for i in range(w):
                    c = tex[u0 + i, v0 + j]
                    if c[3] == 0:
                        continue
                    pts = []
                    for a, b in ((i, j), (i + 1, j), (i + 1, j + 1), (i, j + 1)):
                        P = (tl[0] + a * du[0] + b * dv[0], tl[1] + a * du[1] + b * dv[1], tl[2] + a * du[2] + b * dv[2])
                        pts.append((sum(P[k] * right[k] for k in range(3)), -sum(P[k] * up[k] for k in range(3)),
                                    sum(P[k] * cam[k] for k in range(3))))
                    depth = sum(q[2] for q in pts) / 4
                    col_ = mul(c[:3], shade)
                    quads.append((depth, [(q[0], q[1]) for q in pts], col_ + (c[3],), p.light >= 10))
    xs = [q[0] for qd in quads for q in qd[1]]
    ys = [q[1] for qd in quads for q in qd[1]]
    pad = 1.5
    minx, maxx, miny, maxy = min(xs) - pad, max(xs) + pad, min(ys) - pad, max(ys) + pad
    W, H = int((maxx - minx) * scale) + 1, int((maxy - miny) * scale) + 1
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    gl = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d, dg = ImageDraw.Draw(img, 'RGBA'), ImageDraw.Draw(gl, 'RGBA')
    quads.sort(key=lambda q: q[0])
    for depth, pts, c, emissive in quads:
        poly_ = [((x - minx) * scale, (y - miny) * scale) for x, y in pts]
        d.polygon(poly_, fill=c)
        if emissive:
            dg.polygon(poly_, fill=c)
    if glow:
        halo = gl.filter(ImageFilter.GaussianBlur(scale * 0.9))
        out = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        out.alpha_composite(halo)
        out.alpha_composite(halo)
        out.alpha_composite(img)
        img = out
    return img


def font(size, bold=True):
    for name in (('segoeuib.ttf', 'arialbd.ttf', 'DejaVuSans-Bold.ttf') if bold else ('segoeui.ttf', 'arial.ttf', 'DejaVuSans.ttf')):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def background(W, H):
    bg = Image.new('RGBA', (W, H), (26, 24, 32, 255))
    d = ImageDraw.Draw(bg)
    for y in range(H):
        t = y / H
        d.line([(0, y), (W, y)], fill=lerp((34, 32, 44), (18, 16, 22), t) + (255,))
    return bg


def lineup(staffs, path):
    scale = 15
    imgs = [render(st, at, 32, 14, scale) for st, at in staffs]
    Hm = max(i.height for i in imgs)
    W = sum(max(i.width, 230) for i in imgs) + 40 * (len(imgs) + 1)
    H = Hm + 220
    bg = background(W, H)
    d = ImageDraw.Draw(bg)
    ft, fl, fs = font(44), font(30), font(18, bold=False)
    title = 'BÂTONS DE MAGE — ÉVOLUTION (BLOCKBENCH, JAVA BLOCK/ITEM)'
    d.text(((W - d.textlength(title, font=ft)) / 2, 28), title, font=ft, fill=(226, 222, 240))
    x = 40
    base_y = 110 + Hm
    for (st, at), im in zip(staffs, imgs):
        cw = max(im.width, 230)
        bg.alpha_composite(im, (x + (cw - im.width) // 2, base_y - im.height))
        lab = f'NIV. {st.level}'
        d.text((x + (cw - d.textlength(lab, font=fl)) / 2, base_y + 22), lab, font=fl, fill=(232, 228, 246))
        sub = st.titre
        d.text((x + (cw - d.textlength(sub, font=fs)) / 2, base_y + 62), sub, font=fs, fill=(170, 164, 196))
        x += cw + 40
    bg.convert('RGB').save(path)


def views(st, atlas, path):
    scale = 12
    specs = [('Face', 0, 0), ('Profil', 90, 0), ('3/4', 35, 16), ('Dos 3/4', 215, 16)]
    imgs = [render(st, atlas, yw, pt, scale) for _, yw, pt in specs]
    Hm = max(i.height for i in imgs)
    W = sum(max(i.width, 200) for i in imgs) + 40 * 5 + 160
    H = Hm + 140
    bg = background(W, H)
    d = ImageDraw.Draw(bg)
    d.text((24, 18), f'{st.titre} — niveau {st.level}  ({len(st.parts)} cubes)', font=font(28), fill=(226, 222, 240))
    x = 40
    for (lab, _, _), im in zip(specs, imgs):
        cw = max(im.width, 200)
        bg.alpha_composite(im, (x + (cw - im.width) // 2, 70 + Hm - im.height))
        d.text((x + (cw - d.textlength(lab, font=font(20))) / 2, 86 + Hm), lab, font=font(20), fill=(190, 184, 214))
        x += cw + 40
    tx = atlas.resize((atlas.width * 2, atlas.height * 2) if atlas.width <= 64 else atlas.size, Image.NEAREST)
    if tx.height > Hm:
        tx = tx.resize((Hm * tx.width // tx.height, Hm), Image.NEAREST)
    bg.alpha_composite(tx, (x, 70))
    d.text((x, 86 + Hm), f'texture {atlas.width}x{atlas.height}', font=font(18, False), fill=(190, 184, 214))
    bg.convert('RGB').save(path)


# =============================================================================================== main
def main():
    os.makedirs(os.path.join(ICI, 'textures'), exist_ok=True)
    os.makedirs(os.path.join(ICI, 'apercu'), exist_ok=True)
    mc = os.path.join(ICI, 'minecraft', 'assets', NS)
    for sub in ('models/item', 'textures/item', 'items'):
        os.makedirs(os.path.join(mc, *sub.split('/')), exist_ok=True)
    with open(os.path.join(ICI, 'minecraft', 'pack.mcmeta'), 'w', encoding='utf-8') as f:
        # format 97 = Minecraft 26.3 (a ajuster pour une autre version)
        json.dump({'pack': {'description': 'Batons de mage Eldoria (6 evolutions)', 'min_format': 97, 'max_format': 97}}, f, indent=2)
    done = []
    for fn in BATONS:
        st = fn()
        atlas = build_texture(st)
        with open(os.path.join(ICI, f'{st.key}.bbmodel'), 'w', encoding='utf-8') as f:
            json.dump(bbmodel(st, atlas), f, ensure_ascii=False, indent=1)
        atlas.save(os.path.join(ICI, 'textures', f'{st.key}.png'))
        atlas.save(os.path.join(mc, 'textures', 'item', f'{st.key}.png'))
        with open(os.path.join(mc, 'models', 'item', f'{st.key}.json'), 'w', encoding='utf-8') as f:
            json.dump(java_model(st, atlas), f, ensure_ascii=False, indent=1)
        with open(os.path.join(mc, 'items', f'{st.key}.json'), 'w', encoding='utf-8') as f:
            json.dump({'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{st.key}'}}, f, indent=2)
        views(st, atlas, os.path.join(ICI, 'apercu', f'{st.key}_vues.png'))
        print(f'{st.key:14s} niveau {st.level:3d}  {len(st.parts):3d} cubes  texture {atlas.width}x{atlas.height}')
        done.append((st, atlas))
    lineup(done, os.path.join(ICI, 'apercu', 'evolution.png'))
    print('Apercu : apercu/evolution.png')


if __name__ == '__main__':
    main()
