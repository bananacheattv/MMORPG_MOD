#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
20 panoplies d'armure (niveaux 1 a 100) pour Blockbench, format "Modded Entity" (Java, Mojang mappings 1.17+).
Chaque panoplie = un projet .bbmodel avec quatre pieces separees : casque, plastron (torse + bras avec epaulieres),
jambieres (taille + jambes) et bottes, calees sur le gabarit du joueur (os et pivots du HumanoidModel de Minecraft).

Usage :  python generer_armures.py        (Python 3.8+, Pillow ; utilise ../blockbench_kit.py)

Sorties (un dossier par panoplie : panoplie_01_niv_001 ... panoplie_20_niv_100) :
  <nom>.bbmodel            fichier de travail Blockbench (texture integree, box UV, groupes par piece et par membre)
  <nom>.png                texture de l'armure (box UV, 1 texel par pixel)
  <nom>_emissive.png       masque des parties lumineuses (cristaux, runes, energie) pour une couche emissive en jeu
  <nom>_vues.png           vues de face, de profil, de dos et 3/4 sur le mannequin de reference
Planches : apercu/lot_1.png ... lot_4.png (5 panoplies chacune) et apercu/panoplies.png (les 20).
"""
import base64
import io
import json
import math
import os
import random
import sys
import uuid

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
import blockbench_kit as bk  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

ICI = os.path.dirname(os.path.abspath(__file__))
NS = 'mmorpg'
COLORS = {'casque': 4, 'plastron': 1, 'jambieres': 2, 'bottes': 3, 'reference_joueur': 0}

bk.PAL.update({
    'cuir_clair': [(126, 86, 52), (96, 64, 38), (150, 108, 68), (70, 46, 26)],
    'cuir_sombre': [(78, 52, 36), (58, 38, 26), (98, 68, 48), (42, 28, 18)],
    'acier': [(176, 184, 198), (128, 136, 152), (212, 218, 230), (240, 244, 252)],
    'tissu': [(44, 56, 104), (32, 40, 78), (60, 74, 128), (24, 30, 60)],
    'tissu_violet': [(66, 42, 106), (48, 30, 80), (88, 58, 136), (34, 20, 58)],
    'maille': [(150, 154, 162), (98, 102, 112), (190, 194, 202), (70, 72, 80)],
    'mannequin': [(150, 148, 156), (120, 118, 126), (170, 168, 176), (100, 98, 106)],
})
GLOW_MATS = {'cristal', 'gemme'}


# ============================================================================================== modele
class Bone:
    def __init__(self, name, pivot, rot=(0, 0, 0), parent=None, export=True, visible=True):
        self.name, self.pivot, self.rot, self.parent = name, list(pivot), list(rot), parent
        self.cubes, self.children = [], []
        self.export, self.visible = export, visible
        if parent is not None:
            parent.children.append(self)

    def transform(self, v):
        """Point dans le repere de l'os -> monde (rotations des os, Z puis Y puis X, autour du pivot)."""
        x, y, z = v[0] - self.pivot[0], v[1] - self.pivot[1], v[2] - self.pivot[2]
        rx, ry, rz = (math.radians(a) for a in self.rot)
        if rz:
            x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
        if ry:
            x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
        if rx:
            y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
        p = (x + self.pivot[0], y + self.pivot[1], z + self.pivot[2])
        return self.parent.transform(p) if self.parent is not None else p


class Cube:
    def __init__(self, name, frm, size, mat, infl=0.0, deco=None):
        assert all(abs(s - round(s)) < 1e-9 and s >= 0 for s in size), (name, size)   # box UV : tailles entieres
        self.name, self.frm, self.size, self.mat, self.infl = name, list(frm), [int(round(s)) for s in size], mat, infl
        self.deco = deco or {}
        self.uv_offset = None


class Armor:
    def __init__(self, num, level, titre):
        self.num, self.level, self.titre = num, level, titre
        self.key = f'panoplie_{num:02d}_niv_{level:03d}'
        self.roots, self.bones = [], {}
        for piece in ('casque', 'plastron', 'jambieres', 'bottes'):
            self.roots.append(Bone(piece, (0, 0, 0)))
        r = {b.name: b for b in self.roots}
        self.casque = self._bone('casque_tete', (0, 24, 0), r['casque'])
        self.torse = self._bone('plastron_torse', (0, 24, 0), r['plastron'])
        self.bras = {'droit': self._bone('plastron_bras_droit', (-5, 22, 0), r['plastron']),
                     'gauche': self._bone('plastron_bras_gauche', (5, 22, 0), r['plastron'])}
        self.taille = self._bone('jambieres_taille', (0, 24, 0), r['jambieres'])
        self.jambe = {'droit': self._bone('jambieres_jambe_droite', (-1.9, 12, 0), r['jambieres']),
                      'gauche': self._bone('jambieres_jambe_gauche', (1.9, 12, 0), r['jambieres'])}
        self.pied = {'droit': self._bone('bottes_pied_droit', (-1.9, 12, 0), r['bottes']),
                     'gauche': self._bone('bottes_pied_gauche', (1.9, 12, 0), r['bottes'])}
        # mannequin de reference (non exporte, masque dans Blockbench)
        ref = Bone('reference_joueur', (0, 0, 0), export=False, visible=False)
        self.ref = ref
        for name, frm, size, piv in (('ref_tete', (-4, 24, -4), (8, 8, 8), (0, 24, 0)), ('ref_torse', (-4, 12, -2), (8, 12, 4), (0, 24, 0)),
                                     ('ref_bras_droit', (-8, 12, -2), (4, 12, 4), (-5, 22, 0)), ('ref_bras_gauche', (4, 12, -2), (4, 12, 4), (5, 22, 0)),
                                     ('ref_jambe_droite', (-3.9, 0, -2), (4, 12, 4), (-1.9, 12, 0)), ('ref_jambe_gauche', (-0.1, 0, -2), (4, 12, 4), (1.9, 12, 0))):
            b = Bone(name.replace('ref_', 'reference_'), piv, parent=ref, export=False, visible=False)
            b.cubes.append(Cube(name, frm, size, 'mannequin'))

    def _bone(self, name, pivot, parent):
        b = Bone(name, pivot, parent=parent)
        self.bones[name] = b
        return b

    def sub(self, parent, name, pivot, rot):
        """Sous-os tourne (seuls les os peuvent tourner dans un modele d'entite Java)."""
        b = Bone(name, pivot, rot, parent=parent)
        self.bones[name] = b
        return b

    def cube(self, bone, name, frm, size, mat, infl=0.0, deco=None):
        c = Cube(name, frm, size, mat, infl, deco)
        bone.cubes.append(c)
        for v in c.frm:
            assert -64 < v < 64
        return c

    def all_cubes(self, include_ref=False):
        out = []

        def walk(b):
            for c in b.cubes:
                out.append((b, c))
            for ch in b.children:
                walk(ch)
        for r in self.roots:
            walk(r)
        if include_ref:
            walk(self.ref)
        return out


SIDES = (('droit', -1), ('gauche', 1))


def arm_x(sx):
    return -8.0 if sx < 0 else 4.0


def leg_x(sx):
    return -3.9 if sx < 0 else -0.1


def deco_all(*decos):
    return {f: list(decos) for f in ('north', 'south', 'east', 'west', 'up', 'down')}


def deco_front(front, others=()):
    d = deco_all(*others)
    d['north'] = list(others) + list(front)
    return d


# ============================================================================================== briques de construction
def helm(A, mat, infl, front, others=(), name='calotte'):
    A.cube(A.casque, name, (-4, 24, -4), (8, 8, 8), mat, infl, deco_front(front, others))


def torso(A, mat, infl, front=(), back=(), others=(), name='cuirasse', y1=12, h=12):
    d = deco_all(*others)
    d['north'] = list(others) + list(front)
    d['south'] = list(others) + list(back)
    A.cube(A.torse, name, (-4, y1, -2), (8, h, 4), mat, infl, d)


def arms(A, mat, infl, y1=14, h=10, others=(), name='manche'):
    for side, sx in SIDES:
        A.cube(A.bras[side], f'{name}_{side}', (arm_x(sx), y1, -2), (4, h, 4), mat, infl, deco_all(*others))


def pads(A, layers, mat_cycle, others=(), y=22, extra=None):
    """Epaulieres empilees : layers = [(largeur x, hauteur, profondeur, decalage y, inflate)]."""
    for side, sx in SIDES:
        for i, (w, h, d, dy, infl) in enumerate(layers):
            cx = 6 * sx + sx * 0.6
            mat = mat_cycle[i % len(mat_cycle)]
            A.cube(A.bras[side], f'epauliere_{side}_{i + 1}', (cx - w / 2, y + dy, -d / 2), (w, h, d), mat, infl, deco_all(*others))
        if extra:
            extra(side, sx)


def legs(A, mat, infl, y1=4, h=8, others=(), name='cuissard'):
    for side, sx in SIDES:
        A.cube(A.jambe[side], f'{name}_{side}', (leg_x(sx), y1, -2), (4, h, 4), mat, infl, deco_all(*others))


def waist(A, mat, infl, h=3, belt=None, buckle=None, others=()):
    A.cube(A.taille, 'ceinture_basse', (-4, 12, -2), (8, h, 4), mat, infl, deco_all(*others))
    if belt:
        A.cube(A.taille, 'ceinturon', (-4, 13, -2), (8, 2, 4), belt, infl + 0.35, deco_all('bordure_argent') if belt == 'metal' else None)
    if buckle:
        A.cube(A.taille, 'boucle', (-1, 13, -3), (2, 2, 1), buckle, infl + 0.4,
               deco_all('embleme_glow') if buckle in GLOW_MATS else None)


def boots(A, mat, infl, h=5, toe=None, cuff=None, others=()):
    for side, sx in SIDES:
        x = leg_x(sx)
        A.cube(A.pied[side], f'botte_{side}', (x, 0, -2), (4, h, 4), mat, infl, deco_all(*others))
        if toe:
            A.cube(A.pied[side], f'coque_{side}', (x, 0, -3), (4, 2, 1), toe, infl + 0.1)
        if cuff:
            A.cube(A.pied[side], f'revers_{side}', (x, h - 1, -2), (4, 1, 4), cuff, infl + 0.3)


def knees(A, mat, infl=0.6, size=(3, 3), y=5, deco=None):
    for side, sx in SIDES:
        x = leg_x(sx) + (4 - size[0]) / 2
        A.cube(A.jambe[side], f'genouillere_{side}', (x, y, -3), (size[0], size[1], 1), mat, infl, deco)


def chest_plate(A, mat, w, h, y, infl=0.6, deco=None, name='plastron_avant', z=-3):
    A.cube(A.torse, name, (-w / 2, y, z), (w, h, 1), mat, infl, deco)


def diamond(A, bone, name, center, size, mat, infl=0.0, z=-3.6, depth=1, deco=None):
    """Losange (carre tourne de 45 deg) : sous-os tourne autour de Z."""
    cx, cy = center
    b = A.sub(bone, f'{name}_rot', (cx, cy, z + depth / 2), (0, 0, 45))
    A.cube(b, name, (cx - size / 2, cy - size / 2, z), (size, size, depth), mat, infl, deco)


def crystal_core(A, bone, name, center, size, frame='argent', z=-3.9, frame_w=1):
    """Cristal en losange serti dans un cadre carre (emblème devenu cristal)."""
    diamond(A, bone, f'{name}_cadre', center, size + 2, frame, 0.0, z=z + 0.5, depth=1)
    diamond(A, bone, name, center, size, 'cristal', 0.25, z=z, depth=1, deco=deco_all('cristal_eclat'))


def fragments(A, bone, name, positions, size=1):
    for i, (x, y, z, rz) in enumerate(positions):
        b = A.sub(bone, f'{name}_{i + 1}_rot', (x, y, z), (rz, 0, 45))
        A.cube(b, f'{name}_{i + 1}', (x - size / 2, y - size / 2, z - size / 2), (size, size, size), 'cristal', 0.0, deco_all('cristal_eclat'))


def spikes(A, bone, name, base, n, length, rz, mat='argent', size=1, spacing=1.5, axis='z', infl=0.0):
    """Pointes inclinees (sous-os tournes)."""
    bx, by, bz = base
    for i in range(n):
        x = bx + (i - (n - 1) / 2) * spacing if axis == 'z' else bx
        z = bz + (i - (n - 1) / 2) * spacing if axis == 'x' else bz
        b = A.sub(bone, f'{name}_{i + 1}_rot', (x, by, z), (0, 0, rz) if axis == 'z' else (rz, 0, 0))
        A.cube(b, f'{name}_{i + 1}', (x - size / 2, by, z - size / 2), (size, length, size), mat, infl)


def crown(A, n_points, height, mat='argent', y=32.5, gem=None, tall=False):
    """Couronne : bandeau + pointes reparties sur le pourtour du casque."""
    A.cube(A.casque, 'couronne_bandeau', (-4, y - 1.5, -4), (8, 2, 8), mat, 1.15, deco_all('bordure_argent'))
    pts = [(-4.6, -4.6), (0, -4.9), (4.6, -4.6), (4.9, 0), (4.6, 4.6), (0, 4.9), (-4.6, 4.6), (-4.9, 0)][:n_points]
    for i, (x, z) in enumerate(pts):
        h = height + (2 if (tall and i in (1,)) else 0)
        tilt = 12 if x < -1 else (-12 if x > 1 else 0)
        b = A.sub(A.casque, f'couronne_pointe_{i + 1}_rot', (x, y, z), (0, 0, tilt))
        A.cube(b, f'couronne_pointe_{i + 1}', (x - 0.5, y, z - 0.5), (1, h, 1), mat, 0.1)
    if gem:
        diamond(A, A.casque, 'couronne_joyau', (0, y + 0.5), 2, gem, 0.1, z=-5.6, depth=1)


# ============================================================================================== les 20 panoplies
def p01():
    A = Armor(1, 1, 'Cuir simple')
    A.cube(A.casque, 'bonnet', (-4, 28, -4), (8, 4, 8), 'cuir_clair', 0.6, deco_all('couture'))
    A.cube(A.casque, 'bord_bonnet', (-4, 27, -4), (8, 1, 8), 'cuir_clair', 0.85)
    for side, sx in SIDES:
        A.cube(A.casque, f'rabat_{side}', (-4 if sx < 0 else 3, 24.5, -1), (1, 3, 3), 'cuir_clair', 0.65)
    torso(A, 'cuir_clair', 0.6, front=('lacage', 'embleme_sombre'), others=('couture',))
    arms(A, 'cuir_clair', 0.5, y1=19, h=5, others=('couture',))
    waist(A, 'cuir_clair', 0.45, belt='cuir_sombre')
    legs(A, 'cuir_clair', 0.4, y1=5, h=7, others=('couture',))
    boots(A, 'cuir_clair', 0.75, h=4, cuff='cuir_sombre')
    return A


def p02():
    A = Armor(2, 5, 'Cuir renforcé')
    helm(A, 'cuir_sombre', 0.8, front=('visage',), others=('couture',))
    torso(A, 'cuir_sombre', 0.6, front=('embleme_sombre',), others=('couture',))
    chest_plate(A, 'cuir_clair', 6, 7, 15, infl=0.5, deco=deco_all('couture'))
    pads(A, [(5, 2, 5, 0.5, 0.3)], ['cuir_clair'], others=('couture',))
    arms(A, 'cuir_sombre', 0.5, y1=14, h=5, others=('couture',), name='brassard')
    waist(A, 'cuir_sombre', 0.45, belt='cuir_clair')
    legs(A, 'cuir_sombre', 0.4, y1=5, h=7, others=('couture',))
    knees(A, 'cuir_clair', 0.55, deco=deco_all('couture'))
    boots(A, 'cuir_sombre', 0.8, h=5, cuff='cuir_clair')
    return A


def p03():
    A = Armor(3, 10, 'Cuir clouté')
    helm(A, 'cuir_clous', 0.8, front=('visage',))
    A.cube(A.casque, 'bandeau_fer', (-4, 29, -4), (8, 1, 8), 'fer', 0.95)
    torso(A, 'cuir_clous', 0.6, front=('embleme_sombre',))
    chest_plate(A, 'fer', 4, 4, 18, infl=0.4, deco=deco_all('clous'))
    pads(A, [(5, 2, 5, 0.5, 0.35), (4, 1, 4, 2.5, 0.3)], ['cuir_clous', 'fer'])
    arms(A, 'cuir_clous', 0.5, y1=14, h=6, name='brassard')
    for side, sx in SIDES:
        A.cube(A.bras[side], f'plaque_avant_bras_{side}', (arm_x(sx), 15, -3), (4, 3, 1), 'fer', 0.35)
    waist(A, 'cuir_sombre', 0.45, belt='cuir_clair', buckle='fer')
    legs(A, 'cuir_clous', 0.4, y1=5, h=7)
    knees(A, 'fer', 0.55, deco=deco_all('clous'))
    boots(A, 'cuir_sombre', 0.8, h=5, toe='fer', cuff='cuir_clous')
    return A


def p04():
    A = Armor(4, 15, 'Mailles et cuir')
    helm(A, 'fer', 0.9, front=('visage',))
    A.cube(A.casque, 'nasal', (-0.5, 26, -5), (1, 3, 1), 'fer', 0.2)
    A.cube(A.casque, 'camail', (-4, 24, -4), (8, 2, 8), 'maille', 1.2, deco_front(('visage_bas',)))
    torso(A, 'maille', 0.65)
    torso(A, 'cuir_clair', 0.8, front=('embleme_sombre',), others=('couture',), name='gilet', y1=15, h=9)
    arms(A, 'maille', 0.55, y1=15, h=9, name='manche_mailles')
    pads(A, [(5, 2, 5, 0.5, 0.25)], ['maille'])
    waist(A, 'maille', 0.5, h=3, belt='cuir_sombre', buckle='fer')
    A.cube(A.taille, 'jupe_mailles', (-4, 9, -2), (8, 4, 4), 'maille', 0.75)
    legs(A, 'maille', 0.4, y1=4, h=8)
    boots(A, 'cuir_sombre', 0.8, h=5, toe='fer', cuff='cuir_clair')
    return A


def p05():
    A = Armor(5, 20, 'Armure de fer')
    helm(A, 'fer', 1.0, front=('visage_t',))
    A.cube(A.casque, 'crete', (-0.5, 32, -4), (1, 1, 8), 'fer', 0.4)
    torso(A, 'fer', 0.75)
    chest_plate(A, 'fer', 6, 6, 16, infl=0.4, deco=deco_all('embleme_relief'))
    pads(A, [(6, 3, 6, 0.0, 0.3), (5, 2, 5, 2.0, 0.3)], ['fer'])
    arms(A, 'fer', 0.6, y1=14, h=7, name='canon')
    waist(A, 'fer', 0.5, belt='cuir_sombre', buckle='fer')
    legs(A, 'fer', 0.45, y1=4, h=8)
    knees(A, 'fer', 0.6)
    boots(A, 'fer', 0.9, h=5, toe='fer')
    return A


def p06():
    A = Armor(6, 25, 'Fer renforcé')
    helm(A, 'fer', 1.0, front=('visage_t',), others=('rivets',))
    for side, sx in SIDES:
        A.cube(A.casque, f'protege_joue_{side}', (-4 if sx < 0 else 2, 24, -5), (2, 4, 1), 'fer', 0.3)
    A.cube(A.casque, 'crete', (-0.5, 32, -4), (1, 2, 8), 'fer', 0.4)
    torso(A, 'fer', 0.75, others=('rivets',))
    chest_plate(A, 'fer', 6, 7, 15, infl=0.5, deco=deco_all('embleme_relief'))
    A.cube(A.torse, 'gorgerin', (-4, 23, -2), (8, 2, 4), 'fer', 1.05)
    pads(A, [(7, 3, 7, 0.0, 0.3), (6, 1, 6, 3.0, 0.2)], ['fer'], others=('rivets',))
    arms(A, 'fer', 0.6, y1=14, h=7, name='canon')
    A.cube(A.taille, 'ceinture_blindee', (-4, 12, -2), (8, 3, 4), 'fer', 0.95, deco_all('rivets'))
    A.cube(A.taille, 'boucle', (-1, 12.5, -3), (2, 2, 1), 'fer', 1.0)
    legs(A, 'fer', 0.45, y1=4, h=8)
    knees(A, 'fer', 0.65, size=(4, 3))
    boots(A, 'fer', 0.9, h=5, toe='fer', cuff='fer')
    return A


def p07():
    A = Armor(7, 30, 'Acier poli')
    helm(A, 'acier', 1.0, front=('visage',))
    A.cube(A.casque, 'visiere', (-4, 25, -5), (8, 5, 1), 'acier', 0.5, deco_front(('fentes',)))
    A.cube(A.casque, 'crete', (-0.5, 32, -4), (1, 2, 8), 'acier', 0.4)
    torso(A, 'acier', 0.75)
    for i, y in enumerate((20, 17.5, 15)):
        chest_plate(A, 'acier', 7, 2, y, infl=0.4 + i * 0.05, name=f'lame_plastron_{i + 1}')
    pads(A, [(6, 3, 6, 0.0, 0.3), (5, 2, 5, 2.0, 0.25), (4, 1, 4, 3.5, 0.2)], ['acier'])
    arms(A, 'acier', 0.6, y1=14, h=7, name='canon')
    waist(A, 'acier', 0.5, belt='cuir_sombre', buckle='acier')
    for side, sx in SIDES:
        x = leg_x(sx)
        for i, (y, h) in enumerate(((9, 3), (6, 2), (4, 2))):
            A.cube(A.jambe[side], f'segment_{side}_{i + 1}', (x, y, -2), (4, h, 4), 'acier', 0.45 + 0.1 * (i == 1))
    knees(A, 'acier', 0.7, size=(4, 2), y=6)
    boots(A, 'acier', 0.9, h=4, toe='acier', cuff='acier')
    return A


def p08():
    A = Armor(8, 35, 'Acier et tissu sombre')
    helm(A, 'acier', 1.0, front=('visage_t',))
    A.cube(A.casque, 'crete', (-0.5, 32, -4), (1, 2, 8), 'acier', 0.4)
    torso(A, 'tissu', 0.6)
    chest_plate(A, 'acier', 7, 8, 15, infl=0.5, deco=deco_all('bordure_claire'))
    crystal_core(A, A.torse, 'petit_cristal', (0, 20.5), 2, frame='acier', z=-4.4)
    A.cube(A.torse, 'tabard', (-3, 8, -3), (6, 4, 1), 'tissu', 0.2, deco_all('bordure_claire'))
    pads(A, [(6, 3, 6, 0.0, 0.3), (5, 2, 5, 2.0, 0.25)], ['acier'])
    arms(A, 'tissu', 0.5, y1=14, h=10, name='manche_tissu')
    for side, sx in SIDES:
        A.cube(A.bras[side], f'canon_{side}', (arm_x(sx), 14, -2), (4, 4, 4), 'acier', 0.75)
    waist(A, 'tissu', 0.5, belt='cuir_sombre', buckle='acier')
    legs(A, 'tissu', 0.4, y1=4, h=8)
    knees(A, 'acier', 0.65, size=(4, 3))
    boots(A, 'acier', 0.9, h=5, toe='acier')
    return A


def p09():
    A = Armor(9, 40, 'Acier noir bordé d\'argent')
    helm(A, 'metal', 1.0, front=('visage_t',), others=('bordure_argent',))
    A.cube(A.casque, 'crete', (-0.5, 32, -4), (1, 2, 8), 'argent', 0.4)
    torso(A, 'metal', 0.75, others=('bordure_argent',))
    diamond(A, A.torse, 'embleme_argent', (0, 20), 3, 'argent', 0.0, z=-4)
    # epaulieres asymetriques : droite massive, gauche legere
    A.cube(A.bras['droit'], 'epauliere_droit_1', (-10.4, 22, -3.5), (7, 3, 7), 'metal', 0.3, deco_all('bordure_argent'))
    A.cube(A.bras['droit'], 'epauliere_droit_2', (-9.9, 25, -3), (6, 2, 6), 'metal', 0.25, deco_all('bordure_argent'))
    spikes(A, A.bras['droit'], 'pointe_epaule', (-8.0, 26.5, 0), 1, 2, 22.5, mat='argent')
    A.cube(A.bras['gauche'], 'epauliere_gauche_1', (4.1, 22.5, -2.5), (5, 2, 5), 'metal', 0.3, deco_all('bordure_argent'))
    arms(A, 'metal', 0.6, y1=14, h=7, others=('bordure_argent',), name='canon')
    waist(A, 'metal', 0.5, belt='argent', buckle='argent')
    legs(A, 'metal', 0.45, y1=4, h=8, others=('bordure_argent',))
    knees(A, 'argent', 0.65, size=(3, 3))
    boots(A, 'metal', 0.9, h=5, toe='argent', others=('bordure_argent',))
    return A


def p10():
    A = Armor(10, 45, 'Chevalier lourd')
    helm(A, 'acier', 1.0, front=('visage_t',), others=('rivets',))
    A.cube(A.casque, 'crete', (-0.5, 32, -4), (1, 3, 8), 'acier', 0.4)
    diamond(A, A.casque, 'cristal_front', (0, 30.5), 1, 'cristal', 0.2, z=-5.4)
    torso(A, 'acier', 0.8)
    chest_plate(A, 'acier', 8, 9, 14, infl=0.6, deco=deco_all('ornements'))
    chest_plate(A, 'argent', 4, 3, 22, infl=0.4, name='col_orne', z=-3.2)
    crystal_core(A, A.torse, 'cristal_central', (0, 18.5), 2, frame='argent', z=-4.6)
    pads(A, [(7, 3, 7, 0.0, 0.3), (6, 2, 6, 2.5, 0.25), (5, 1, 5, 4.0, 0.2)], ['acier', 'argent'],
         extra=lambda side, sx: diamond(A, A.bras[side], f'cristal_epaule_{side}', (6.6 * sx, 24.0), 1, 'cristal', 0.2, z=-4.4))
    arms(A, 'acier', 0.65, y1=14, h=7, name='canon')
    waist(A, 'acier', 0.55, belt='argent', buckle='cristal')
    legs(A, 'acier', 0.5, y1=4, h=8, others=('ornements',))
    knees(A, 'argent', 0.7, size=(4, 3))
    for side, sx in SIDES:
        diamond(A, A.jambe[side], f'cristal_genou_{side}', (leg_x(sx) + 2, 6.5), 1, 'cristal', 0.2, z=-4.2)
    boots(A, 'acier', 0.95, h=5, toe='argent', cuff='argent')
    return A


def p11():
    A = Armor(11, 50, 'Armure runique sombre')
    helm(A, 'metal', 1.0, front=('visage_t',), others=('runes_laterales',))
    A.cube(A.casque, 'crete', (-0.5, 32, -4), (1, 2, 8), 'metal', 0.4, deco_all('runes'))
    torso(A, 'metal', 0.75, front=('runes',), back=('runes',))
    chest_plate(A, 'metal', 6, 8, 14.5, infl=0.5, deco=deco_all('runes', 'bordure_argent'))
    crystal_core(A, A.torse, 'gemme_runique', (0, 20.5), 2, frame='metal', z=-4.4)
    pads(A, [(6, 3, 6, 0.0, 0.3), (5, 2, 5, 2.0, 0.25)], ['metal'], others=('runes',))
    arms(A, 'metal', 0.6, y1=14, h=8, others=('runes',), name='canon')
    waist(A, 'metal', 0.5, belt='cuir_sombre', buckle='gemme')
    legs(A, 'metal', 0.45, y1=4, h=8, others=('runes',))
    knees(A, 'metal', 0.65, size=(3, 3), deco=deco_all('runes'))
    boots(A, 'metal', 0.9, h=5, toe='metal')
    return A


def p12():
    A = Armor(12, 55, 'Épaulières angulaires')
    helm(A, 'metal', 1.0, front=('visage_t',), others=('bordure_argent',))
    for side, sx in SIDES:
        b = A.sub(A.casque, f'aileron_{side}_rot', (4.6 * sx, 29, 0), (0, 0, -20 * sx))
        A.cube(b, f'aileron_{side}', (4.6 * sx - 0.5, 29, -3), (1, 4, 6), 'argent', 0.0)
    torso(A, 'metal', 0.75, others=('bordure_argent',))
    for i, y in enumerate((21, 18.5, 16, 13.5)):
        chest_plate(A, 'metal' if i % 2 else 'argent', 7 - (i % 2), 2, y, infl=0.45, name=f'segment_plastron_{i + 1}', deco=deco_all('runes'))
    for side, sx in SIDES:
        b = A.sub(A.bras[side], f'epauliere_{side}_rot', (6 * sx, 24, 0), (0, 0, 22.5 * -sx))
        A.cube(b, f'epauliere_{side}_1', (6 * sx - 4.5, 22, -4), (9, 3, 8), 'metal', 0.2, deco_all('bordure_argent', 'runes'))
        A.cube(b, f'epauliere_{side}_2', (6 * sx - 4, 25, -3.5), (8, 1, 7), 'argent', 0.2)
        A.cube(b, f'epauliere_{side}_3', (6 * sx + (2.5 if sx > 0 else -4.5), 21, -4), (2, 1, 8), 'argent', 0.2)
    arms(A, 'metal', 0.6, y1=14, h=7, others=('bordure_argent',), name='canon')
    waist(A, 'metal', 0.5, belt='argent', buckle='gemme')
    legs(A, 'metal', 0.45, y1=4, h=8, others=('bordure_argent',))
    knees(A, 'argent', 0.7, size=(4, 3))
    boots(A, 'metal', 0.9, h=5, toe='argent', cuff='argent')
    return A


def p13():
    A = Armor(13, 60, 'Chevalier cristallin')
    helm(A, 'acier', 1.0, front=('visage_t',), others=('bordure_sombre',))
    for i, (z, h) in enumerate(((-2.5, 2), (0, 3), (2.5, 2))):
        b = A.sub(A.casque, f'cristal_crete_{i + 1}_rot', (0, 32.5, z), (0, 45, 0))
        A.cube(b, f'cristal_crete_{i + 1}', (-0.5, 32.5, z - 0.5), (1, h, 1), 'cristal', 0.15, deco_all('cristal_eclat'))
    torso(A, 'acier', 0.75, others=('bordure_sombre',))
    chest_plate(A, 'metal', 7, 8, 14.5, infl=0.5, deco=deco_all('bordure_argent'))
    crystal_core(A, A.torse, 'cristal_central', (0, 19.5), 2, frame='argent', z=-4.5)

    def shoulder_crystals(side, sx):
        for i, (dx, h, rz) in enumerate(((-1.6, 2, 25), (0, 3, 0), (1.6, 2, -25))):
            x = 6.6 * sx + dx
            b = A.sub(A.bras[side], f'cristal_epaule_{side}_{i + 1}_rot', (x, 25.5, 0), (0, 0, rz))
            A.cube(b, f'cristal_epaule_{side}_{i + 1}', (x - 0.5, 25.5, -0.5), (1, h, 1), 'cristal', 0.2, deco_all('cristal_eclat'))
    pads(A, [(7, 3, 7, 0.0, 0.3), (6, 1, 6, 3.0, 0.25)], ['acier', 'metal'], others=('bordure_sombre',), extra=shoulder_crystals)
    arms(A, 'acier', 0.6, y1=14, h=7, others=('bordure_sombre',), name='canon')
    for side, sx in SIDES:
        diamond(A, A.bras[side], f'cristal_avant_bras_{side}', (6 * sx, 17.5), 1, 'cristal', 0.2, z=-3.9)
    waist(A, 'metal', 0.5, belt='argent', buckle='cristal')
    legs(A, 'acier', 0.45, y1=4, h=8, others=('bordure_sombre',))
    for side, sx in SIDES:
        diamond(A, A.jambe[side], f'cristal_genou_{side}', (leg_x(sx) + 2, 6.5), 1, 'cristal', 0.25, z=-3.4)
    boots(A, 'acier', 0.9, h=5, toe='metal', cuff='metal')
    return A


def p14():
    A = Armor(14, 65, 'Armure runique élaborée')
    helm(A, 'metal', 1.0, front=('visage_t',), others=('energie_lignes', 'bordure_argent'))
    for i, (z, h) in enumerate(((-3, 2), (-1, 3), (1, 4), (3, 3))):
        A.cube(A.casque, f'crete_{i + 1}', (-0.5, 33, z - 1), (1, h, 2), 'argent', 0.0, deco_all('runes'))
    torso(A, 'metal', 0.8, front=('energie_lignes',), back=('runes',), others=('bordure_argent',))
    chest_plate(A, 'metal', 6, 8, 14.5, infl=0.55, deco=deco_all('runes', 'bordure_argent'))
    crystal_core(A, A.torse, 'gemme_runique', (0, 20.5), 2, frame='argent', z=-4.6)
    pads(A, [(7, 3, 7, 0.0, 0.3), (6, 2, 6, 2.5, 0.25)], ['metal', 'argent'], others=('runes',))
    arms(A, 'metal', 0.6, y1=14, h=8, others=('energie_lignes',), name='canon')
    waist(A, 'metal', 0.5, belt='argent', buckle='gemme')
    legs(A, 'metal', 0.45, y1=4, h=8, others=('energie_lignes', 'bordure_argent'))
    knees(A, 'argent', 0.7, size=(4, 3), deco=deco_all('runes'))
    boots(A, 'metal', 0.9, h=5, toe='argent', cuff='argent', others=('energie_lignes',))
    return A


def p15():
    A = Armor(15, 70, "Gardien d'élite")
    helm(A, 'argent', 1.0, front=('visage_t',), others=('bordure_sombre',))
    for side, sx in SIDES:
        A.cube(A.casque, f'protege_joue_{side}', (-4 if sx < 0 else 2, 24, -5.2), (2, 5, 1), 'metal', 0.35, deco_all('bordure_argent'))
    for i, (z, h) in enumerate(((-2, 2), (0, 3), (2, 2))):
        A.cube(A.casque, f'crete_{i + 1}', (-1, 33, z - 1), (2, h, 2), 'argent', 0.0)
    torso(A, 'metal', 0.85, others=('bordure_argent',))
    chest_plate(A, 'argent', 8, 9, 14, infl=0.6, deco=deco_all('ornements'))
    A.cube(A.torse, 'gorgerin', (-4, 23, -2), (8, 2, 4), 'argent', 1.15)
    crystal_core(A, A.torse, 'cristal_central', (0, 18.5), 2, frame='metal', z=-4.7)
    pads(A, [(9, 3, 8, 0.0, 0.3), (8, 2, 7, 3.0, 0.25), (6, 1, 5, 5.0, 0.2)], ['metal', 'argent'], others=('bordure_argent',),
         extra=lambda side, sx: diamond(A, A.bras[side], f'cristal_epaule_{side}', (7.0 * sx, 24.0), 1, 'cristal', 0.25, z=-4.9))
    arms(A, 'metal', 0.65, y1=14, h=8, others=('bordure_argent',), name='canon')
    A.cube(A.taille, 'ceinture_blindee', (-4, 12, -2), (8, 3, 4), 'argent', 1.0, deco_all('ornements'))
    A.cube(A.taille, 'boucle', (-1, 12.5, -3), (2, 2, 1), 'cristal', 1.05, deco_all('cristal_eclat'))
    legs(A, 'metal', 0.5, y1=4, h=8, others=('bordure_argent',))
    knees(A, 'argent', 0.75, size=(4, 4), deco=deco_all('ornements'))
    boots(A, 'metal', 0.95, h=5, toe='argent', cuff='argent')
    return A


def p16():
    A = Armor(16, 75, 'Armure légendaire')
    helm(A, 'metal', 1.0, front=('visage_t',), others=('bordure_argent', 'runes_laterales'))
    diamond(A, A.casque, 'cristal_front', (0, 30.0), 2, 'cristal', 0.1, z=-5.6)
    A.cube(A.casque, 'crete', (-1, 32, -4), (2, 3, 8), 'argent', 0.0)
    torso(A, 'metal', 0.8, others=('bordure_argent',))
    for i, (w, h, y, z) in enumerate(((8, 10, 13, -3), (6, 8, 14, -3.5), (4, 6, 15, -4))):
        chest_plate(A, 'argent' if i % 2 == 0 else 'metal', w, h, y, infl=0.4, name=f'plaque_superposee_{i + 1}', z=z, deco=deco_all('bordure_sombre' if i % 2 == 0 else 'runes'))
    crystal_core(A, A.torse, 'cristal_lumineux', (0, 18.0), 3, frame='argent', z=-5.6)
    pads(A, [(8, 3, 8, 0.0, 0.3), (7, 2, 7, 2.5, 0.3), (6, 2, 6, 4.0, 0.25)], ['metal', 'argent'], others=('bordure_argent',),
         extra=lambda side, sx: diamond(A, A.bras[side], f'cristal_epaule_{side}', (6.8 * sx, 24.5), 2, 'cristal', 0.1, z=-5.3))
    arms(A, 'metal', 0.65, y1=14, h=8, others=('bordure_argent', 'energie_lignes'), name='canon')
    waist(A, 'metal', 0.5, belt='argent', buckle='cristal')
    legs(A, 'metal', 0.5, y1=4, h=8, others=('bordure_argent', 'energie_lignes'))
    for side, sx in SIDES:
        diamond(A, A.jambe[side], f'cristal_genou_{side}', (leg_x(sx) + 2, 6.5), 2, 'cristal', 0.1, z=-4.0)
    boots(A, 'metal', 0.95, h=5, toe='argent', cuff='argent')
    return A


def p17():
    A = Armor(17, 80, 'Acier noir cristallin')
    helm(A, 'metal', 1.0, front=('visage_t',), others=('bordure_argent',))
    spikes(A, A.casque, 'pointe_casque', (0, 32.5, 0), 3, 3, 0, mat='argent', spacing=2.5, axis='x')
    for side, sx in SIDES:
        b = A.sub(A.casque, f'corne_{side}_rot', (4.5 * sx, 30, 0), (0, 0, -35 * sx))
        A.cube(b, f'corne_{side}', (4.5 * sx - 0.5, 30, -0.5), (1, 4, 1), 'argent', 0.1)
    torso(A, 'metal', 0.8, others=('bordure_argent',))
    chest_plate(A, 'metal', 7, 9, 14, infl=0.55, deco=deco_all('runes', 'bordure_argent'))
    crystal_core(A, A.torse, 'cristal_central', (0, 19.0), 3, frame='argent', z=-5.4)

    def crystal_pads(side, sx):
        for i, (dx, h, rz) in enumerate(((-2.4, 3, 30), (-0.8, 5, 12), (0.8, 4, -12), (2.4, 3, -30))):
            x = 6.8 * sx + dx
            b = A.sub(A.bras[side], f'cristal_epaule_{side}_{i + 1}_rot', (x, 25.5, 0), (0, 0, rz))
            A.cube(b, f'cristal_epaule_{side}_{i + 1}', (x - 0.5, 25.5, -0.5), (1, h, 1), 'cristal', 0.25, deco_all('cristal_eclat'))
    pads(A, [(8, 3, 8, 0.0, 0.3), (7, 1, 7, 3.0, 0.25)], ['metal', 'argent'], others=('bordure_argent',), extra=crystal_pads)
    arms(A, 'metal', 0.65, y1=14, h=8, others=('bordure_argent',), name='canon')
    for side, sx in SIDES:
        b = A.sub(A.bras[side], f'lame_bras_{side}_rot', (8.5 * sx, 16, 0), (0, 0, 20 * sx))
        A.cube(b, f'lame_bras_{side}', (8.5 * sx - 0.5, 16, -0.5), (1, 5, 1), 'argent', 0.1)
    waist(A, 'metal', 0.5, belt='argent', buckle='cristal')
    legs(A, 'metal', 0.5, y1=4, h=8, others=('bordure_argent', 'runes'))
    knees(A, 'argent', 0.75, size=(4, 3))
    for side, sx in SIDES:
        spikes(A, A.jambe[side], f'pointe_genou_{side}', (leg_x(sx) + 2, 6.5, -3.5), 1, 2, 0, mat='argent')
    boots(A, 'metal', 0.95, h=5, toe='argent', cuff='argent')
    return A


def p18():
    A = Armor(18, 85, 'Armure ancienne massive')
    helm(A, 'metal', 1.1, front=('visage_t',), others=('bordure_argent', 'ancien'))
    A.cube(A.casque, 'arcade', (-4, 29, -5.5), (8, 2, 2), 'argent', 0.3)
    A.cube(A.casque, 'crete', (-1.5, 32.5, -4), (3, 2, 8), 'metal', 0.2, deco_all('bordure_argent'))
    torso(A, 'metal', 0.9, others=('bordure_argent', 'ancien'))
    # noyau encadre
    A.cube(A.torse, 'cadre_noyau', (-3, 16, -4), (6, 7, 1), 'argent', 0.3, deco_all('ornements'))
    A.cube(A.torse, 'noyau', (-2, 17, -5), (4, 5, 1), 'cristal', 0.1, deco_all('cristal_eclat'))
    for i, (x, y, w, h) in enumerate(((-3.5, 23, 7, 1), (-3.5, 15, 7, 1), (-3.5, 16, 1, 7), (2.5, 16, 1, 7))):
        A.cube(A.torse, f'barre_cadre_{i + 1}', (x, y, -5.2), (w, h, 1), 'metal', 0.2)
    # plaques d'epaules suspendues (detachees de l'epaule)
    for side, sx in SIDES:
        A.cube(A.bras[side], f'epaule_{side}', (arm_x(sx), 21, -2), (4, 3, 4), 'metal', 1.1, deco_all('bordure_argent'))
        A.cube(A.bras[side], f'plaque_suspendue_{side}_1', (6.8 * sx - 4.5, 26, -4.5), (9, 2, 9), 'metal', 0.0, deco_all('bordure_argent', 'ancien'))
        A.cube(A.bras[side], f'plaque_suspendue_{side}_2', (6.8 * sx - 3.5, 28.5, -3.5), (7, 1, 7), 'argent', 0.0)
        diamond(A, A.bras[side], f'cristal_suspendu_{side}', (6.8 * sx, 27.0), 2, 'cristal', 0.0, z=-5.1)
    arms(A, 'metal', 0.7, y1=14, h=7, others=('bordure_argent',), name='canon')
    A.cube(A.taille, 'ceinture_blindee', (-4, 12, -2), (8, 3, 4), 'metal', 1.05, deco_all('bordure_argent', 'ancien'))
    A.cube(A.taille, 'boucle', (-1.5, 12, -3.3), (3, 3, 1), 'cristal', 1.0, deco_all('cristal_eclat'))
    legs(A, 'metal', 0.55, y1=4, h=8, others=('bordure_argent', 'ancien'))
    knees(A, 'argent', 0.8, size=(4, 4), deco=deco_all('ornements'))
    boots(A, 'metal', 1.0, h=6, toe='argent', cuff='argent', others=('ancien',))
    return A


def p19():
    A = Armor(19, 90, 'Armure souveraine')
    helm(A, 'metal', 1.0, front=('visage_t',), others=('bordure_argent',))
    crown(A, 8, 2, mat='argent', y=33.5, gem='cristal')
    torso(A, 'metal', 0.85, others=('bordure_argent', 'runes'))
    chest_plate(A, 'argent', 8, 9, 14, infl=0.6, deco=deco_all('ornements'))
    crystal_core(A, A.torse, 'cristal_royal', (0, 19.0), 3, frame='metal', z=-5.6)
    pads(A, [(8, 3, 8, 0.0, 0.3), (7, 2, 7, 2.5, 0.3), (5, 1, 5, 4.5, 0.25)], ['metal', 'argent'], others=('bordure_argent',),
         extra=lambda side, sx: diamond(A, A.bras[side], f'cristal_epaule_{side}', (6.8 * sx, 24.5), 2, 'cristal', 0.1, z=-5.3))
    arms(A, 'metal', 0.65, y1=14, h=8, others=('bordure_argent', 'energie_lignes'), name='canon')
    fragments(A, A.torse, 'fragment_flottant', ((-11, 27, 2, 20), (11, 27, 2, -20), (-10, 18, 4, 0), (10, 18, 4, 0), (0, 29, 6, 0)), size=1)
    waist(A, 'metal', 0.5, belt='argent', buckle='cristal')
    A.cube(A.taille, 'pan_tissu', (-2, 8, -3), (4, 5, 1), 'tissu_violet', 0.2, deco_all('bordure_argent'))
    legs(A, 'metal', 0.5, y1=4, h=8, others=('bordure_argent', 'energie_lignes'))
    for side, sx in SIDES:
        diamond(A, A.jambe[side], f'cristal_genou_{side}', (leg_x(sx) + 2, 6.5), 2, 'cristal', 0.1, z=-4.0)
    boots(A, 'metal', 0.95, h=5, toe='argent', cuff='argent')
    return A


def p20():
    A = Armor(20, 100, 'Armure mythique')
    helm(A, 'metal', 1.05, front=('visage_t',), others=('bordure_argent', 'energie_lignes'))
    crown(A, 8, 3, mat='argent', y=33.5, gem='cristal', tall=True)
    for side, sx in SIDES:
        b = A.sub(A.casque, f'aile_couronne_{side}_rot', (4.8 * sx, 31, 0), (0, 0, -30 * sx))
        A.cube(b, f'aile_couronne_{side}', (4.8 * sx - 0.5, 31, -2), (1, 5, 4), 'argent', 0.0, deco_all('bordure_sombre'))
    torso(A, 'metal', 0.9, others=('bordure_argent', 'energie_lignes'))
    for i, (w, h, y, z) in enumerate(((8, 10, 13, -3), (6, 8, 14, -3.5))):
        chest_plate(A, 'argent' if i == 0 else 'metal', w, h, y, infl=0.45, name=f'plaque_superposee_{i + 1}', z=z,
                    deco=deco_all('ornements' if i == 0 else 'runes'))
    # noyau bleu-violet imposant
    crystal_core(A, A.torse, 'noyau', (0, 18.5), 4, frame='argent', z=-6.3)
    for i, (x, y, rz) in enumerate(((-3.8, 22.2, 45), (3.8, 22.2, -45), (-3.8, 14.8, -45), (3.8, 14.8, 45))):
        b = A.sub(A.torse, f'griffe_noyau_{i + 1}_rot', (x, y, -5.2), (0, 0, rz))
        A.cube(b, f'griffe_noyau_{i + 1}', (x - 0.5, y - 1.5, -5.7), (1, 3, 1), 'metal', 0.1)

    def mythic_pads(side, sx):
        b = A.sub(A.bras[side], f'aile_epaule_{side}_rot', (8.5 * sx, 25, 0), (0, 0, -30 * sx))
        A.cube(b, f'aile_epaule_{side}', (8.5 * sx - 0.5, 25, -3.5), (1, 5, 7), 'argent', 0.1, deco_all('bordure_sombre'))
        for i, (dx, h, rz) in enumerate(((-1.8, 3, 25), (0, 4, 0), (1.8, 3, -25))):
            x = 6.8 * sx + dx
            bb = A.sub(A.bras[side], f'cristal_epaule_{side}_{i + 1}_rot', (x, 26.5, 0), (0, 0, rz))
            A.cube(bb, f'cristal_epaule_{side}_{i + 1}', (x - 0.5, 26.5, -0.5), (1, h, 1), 'cristal', 0.25, deco_all('cristal_eclat'))
    pads(A, [(9, 3, 8, 0.0, 0.3), (8, 2, 7, 2.5, 0.3), (6, 1, 6, 4.5, 0.25)], ['metal', 'argent'], others=('bordure_argent', 'runes'),
         extra=mythic_pads)
    arms(A, 'metal', 0.7, y1=14, h=8, others=('bordure_argent', 'energie_lignes'), name='canon')
    for side, sx in SIDES:
        diamond(A, A.bras[side], f'cristal_avant_bras_{side}', (6 * sx, 17.5), 2, 'cristal', 0.0, z=-4.2)
    fragments(A, A.torse, 'fragment_flottant', ((-12, 28, 2, 20), (12, 28, 2, -20), (-11, 19, 4, 0), (11, 19, 4, 0),
                                                (-6, 31, 5, 0), (6, 31, 5, 0), (0, 10, 6, 0)), size=1)
    A.cube(A.taille, 'ceinture_blindee', (-4, 12, -2), (8, 3, 4), 'argent', 1.05, deco_all('ornements'))
    A.cube(A.taille, 'boucle', (-1.5, 12, -3.3), (3, 3, 1), 'cristal', 1.05, deco_all('cristal_eclat'))
    A.cube(A.taille, 'pan_tissu', (-2, 7, -3), (4, 6, 1), 'tissu_violet', 0.2, deco_all('bordure_argent', 'energie_lignes'))
    legs(A, 'metal', 0.55, y1=4, h=8, others=('bordure_argent', 'energie_lignes'))
    knees(A, 'argent', 0.8, size=(4, 4), deco=deco_all('ornements'))
    for side, sx in SIDES:
        diamond(A, A.jambe[side], f'cristal_genou_{side}', (leg_x(sx) + 2, 6.5), 2, 'cristal', 0.1, z=-4.6)
        spikes(A, A.pied[side], f'pointe_botte_{side}', (leg_x(sx) + 2 + 2.2 * sx, 3, 0), 1, 3, -30 * sx, mat='argent')
    boots(A, 'metal', 1.0, h=6, toe='argent', cuff='argent', others=('energie_lignes',))
    return A


PANOPLIES = [p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20]

# ============================================================================================== textures (box UV)
EMISSIVE_DECOS = {'runes', 'runes_laterales', 'energie_lignes', 'embleme_glow'}


def paint_face(mat, w, h, decos, r, face):
    """Peint une face (w x h texels) ; renvoie l'image et le masque des pixels lumineux."""
    if w <= 0 or h <= 0:
        return None, None
    base_mat = {'cuir_clair': 'cuir', 'cuir_sombre': 'cuir', 'cuir_clous': 'cuir', 'maille': 'fer', 'acier': 'fer',
                'tissu': 'fer', 'tissu_violet': 'fer', 'mannequin': 'fer'}.get(mat, mat)
    if mat in ('cuir_clair', 'cuir_sombre', 'cuir_clous'):
        pal = bk.PAL['cuir_sombre' if mat == 'cuir_clous' else mat]
        img = Image.new('RGBA', (w, h))
        px = img.load()
        for x in range(w):
            for y in range(h):
                c = bk.mul(pal[0], 1 + r.uniform(-0.07, 0.07))
                if x in (0, w - 1) or y in (0, h - 1):
                    c = pal[1]
                px[x, y] = c + (255,)
        if mat == 'cuir_clous':
            for x in range(1, w - 1, 3):
                for y in range(1 + (x // 3) % 2, h - 1, 3):
                    px[x, y] = bk.PAL['argent'][2] + (255,)
    elif mat == 'maille':
        pal = bk.PAL['maille']
        img = Image.new('RGBA', (w, h))
        px = img.load()
        for x in range(w):
            for y in range(h):
                k = (x + (y % 2)) % 2
                px[x, y] = (pal[2] if k == 0 else pal[1]) + (255,)
                if y % 2 == 1 and x % 2 == 0:
                    px[x, y] = pal[3] + (255,)
    elif mat in ('tissu', 'tissu_violet'):
        pal = bk.PAL[mat]
        img = Image.new('RGBA', (w, h))
        px = img.load()
        for x in range(w):
            for y in range(h):
                c = pal[0] if (x + y) % 2 == 0 else pal[1]
                if y % 4 == 3:
                    c = pal[3]
                px[x, y] = c + (255,)
    elif mat in ('acier', 'mannequin'):
        saved = bk.PAL['fer']
        bk.PAL['fer'] = bk.PAL[mat]
        img = bk.paint_base('fer', w, h, face not in ('up', 'down'), r)
        bk.PAL['fer'] = saved
    else:
        img = bk.paint_base(base_mat, w, h, face not in ('up', 'down'), r)
    if mat in GLOW_MATS and w * h <= 4:     # petites faces de cristal : couleur franche (pas de reflet blanc)
        pal = bk.PAL[mat]
        img = Image.new('RGBA', (w, h), pal[0] + (255,))
        if w * h > 1:
            img.putpixel((0, 0), pal[2] + (255,))
    mask = Image.new('L', (w, h), 255 if mat in GLOW_MATS else 0)
    px, mk = img.load(), mask.load()
    glow = bk.PAL['glow']

    def gl(x, y):
        if 0 <= x < w and 0 <= y < h:
            px[x, y] = bk.lerp(glow[0], glow[2], r.uniform(0.15, 0.6)) + (255,)
            mk[x, y] = 255

    for deco in decos:
        if deco == 'visage' and face == 'north':
            for x in range(1, w - 1):
                for y in range(2, h):
                    px[x, y] = (0, 0, 0, 0)
        elif deco == 'visage_bas' and face == 'north':
            for x in range(1, w - 1):
                for y in range(0, h):
                    px[x, y] = (0, 0, 0, 0)
        elif deco == 'visage_t' and face == 'north':
            for x in range(1, w - 1):
                for y in (3, 4):
                    if y < h:
                        px[x, y] = (0, 0, 0, 0)
            for x in range(w // 2 - 1, w // 2 + 1):
                for y in range(4, h):
                    px[x, y] = (0, 0, 0, 0)
        elif deco == 'fentes' and face == 'north':
            for y in (1, 3):
                for x in range(1, w - 1):
                    if y < h:
                        px[x, y] = (12, 12, 16, 255)
        elif deco in ('embleme_sombre', 'embleme_argent', 'embleme_relief', 'embleme_glow') and w >= 5 and h >= 5:
            x0, y0 = (w - 5) // 2, max(0, (h - 5) // 2 - (1 if h > 7 else 0))
            for j, row in enumerate(bk.EMBLEME):
                for i, ch in enumerate(row):
                    if ch == '#':
                        if deco == 'embleme_glow':
                            gl(x0 + i, y0 + j)
                        else:
                            col = {'embleme_sombre': bk.mul(px[x0 + i, y0 + j][:3], 0.55), 'embleme_argent': bk.PAL['argent'][2],
                                   'embleme_relief': bk.mul(px[x0 + i, y0 + j][:3], 1.25)}[deco]
                            px[x0 + i, y0 + j] = col + (255,)
        elif deco == 'lacage' and face == 'north':
            cx = w // 2
            for y in range(1, h - 1):
                px[cx - 1 + (y % 2), y] = (40, 26, 16, 255)
        elif deco == 'couture':
            for x in range(1, w - 1, 2):
                if h > 2:
                    px[x, 1] = bk.mul(px[x, 1][:3], 1.35) + (255,)
                    px[x, h - 2] = bk.mul(px[x, h - 2][:3], 1.35) + (255,)
        elif deco == 'clous':
            for x in range(1, w - 1, 2):
                for y in (1, h - 2):
                    if 0 <= y < h:
                        px[x, y] = bk.PAL['argent'][3] + (255,)
        elif deco == 'rivets' and w >= 4 and h >= 4:
            for x, y in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
                px[x, y] = bk.PAL['argent'][3] + (255,)
        elif deco in ('bordure_argent', 'bordure_claire', 'bordure_sombre'):
            if w < 6 or h < 5:      # bordure seulement sur les grandes faces (sinon la piece entiere change de couleur)
                continue
            col = {'bordure_argent': bk.PAL['argent'][2], 'bordure_claire': bk.PAL['acier'][2], 'bordure_sombre': bk.PAL['metal'][1]}[deco]
            for x in range(w):
                px[x, 0] = col + (255,)
                px[x, h - 1] = bk.mul(col, 0.8) + (255,)
            for y in range(h):
                px[0, y] = col + (255,)
                px[w - 1, y] = bk.mul(col, 0.8) + (255,)
        elif deco == 'ornements' and w >= 4 and h >= 4:
            for x in range(1, w - 1):
                if (x % 3) != 1:
                    px[x, 1] = bk.PAL['argent'][3] + (255,)
            cx = w // 2
            for y in range(2, h - 1):
                if y % 2 == 0:
                    px[cx, y] = bk.PAL['argent'][2] + (255,)
        elif deco == 'ancien':
            for _ in range(max(1, w * h // 14)):
                x, y = r.randrange(w), r.randrange(h)
                px[x, y] = bk.mul(px[x, y][:3], 0.7) + (255,)
        elif deco in ('runes', 'runes_laterales'):
            if deco == 'runes_laterales' and face not in ('east', 'west'):
                continue
            if face in ('up', 'down') or w < 3 or h < 5:
                continue
            g = bk.GLYPHES[r.randrange(len(bk.GLYPHES))]
            x0, y0 = (w - 3) // 2, (h - 5) // 2
            for j, row in enumerate(g):
                for i, ch in enumerate(row):
                    if ch == '#':
                        gl(x0 + i, y0 + j)
        elif deco == 'energie_lignes':
            if face in ('up', 'down') or h < 4:
                continue
            for x in ([w // 2] if w < 6 else [w // 4, w - 1 - w // 4]):
                for y in range(1, h - 1):
                    gl(x, y)
        elif deco == 'cristal_eclat':
            if w >= 3 and h >= 3:
                px[0, 0] = bk.PAL['cristal'][3] + (255,)
    return img, mask


def box_regions(c):
    w, h, d = c.size
    u, v = c.uv_offset
    return {'north': (u + d, v + d, w, h), 'south': (u + 2 * d + w, v + d, w, h), 'east': (u, v + d, d, h),
            'west': (u + d + w, v + d, d, h), 'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d)}


def build_texture(A):
    cubes = [c for _, c in A.all_cubes()]
    cache, regions = {}, []
    for c in cubes:
        w, h, d = c.size
        key = (c.mat, json.dumps(c.deco, sort_keys=True), w, h, d)
        if key not in cache:
            cache[key] = len(regions)
            regions.append((key, c, (2 * (w + d), h + d)))
        c._region = cache[key]
    order = sorted(range(len(regions)), key=lambda i: (-regions[i][2][1], -regions[i][2][0]))
    for size in (64, 128, 256):
        x = y = shelf = 0
        pos, ok = {}, True
        for i in order:
            rw, rh = regions[i][2]
            rw, rh = max(rw, 1), max(rh, 1)
            if x + rw > size:
                x, y, shelf = 0, y + shelf, 0
            if y + rh > size or rw > size:
                ok = False
                break
            pos[i] = (x, y)
            x += rw
            shelf = max(shelf, rh)
        if ok:
            break
    tex = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    emi = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for i, (key, c, _) in enumerate(regions):
        c.uv_offset = pos[i]
        r = random.Random(bk.crc(A.key, key))
        for f, (u, v, fw, fh) in box_regions(c).items():
            decos = c.deco.get(f, [])
            img, mask = paint_face(c.mat, fw, fh, decos, r, f)
            if img is None:
                continue
            tex.paste(img, (u, v))
            emi.paste(img, (u, v), mask)
    for c in cubes:
        c.uv_offset = pos[c._region]
    return tex, emi


# ============================================================================================== export .bbmodel (Modded Entity)
def uid(*key):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'eldoria-armure/' + '/'.join(map(str, key))))


def bbmodel(A, tex):
    size = tex.width
    buf = io.BytesIO()
    tex.save(buf, 'PNG')
    elements = []

    def outline(b, color):
        node = {'name': b.name, 'origin': [round(v, 4) for v in b.pivot], 'rotation': [round(v, 4) for v in b.rot],
                'color': color, 'uuid': uid(A.key, 'bone', b.name), 'export': b.export, 'mirror_uv': False,
                'isOpen': b.parent is None, 'locked': False, 'visibility': b.visible, 'autouv': 0, 'children': []}
        for c in b.cubes:
            faces = {}
            if c.uv_offset is None:
                c.uv_offset = (0, 0)
            for f, (u, v, fw, fh) in box_regions(c).items():
                faces[f] = {'uv': [u, v, u + fw, v + fh], 'texture': 0}
            el = {'name': c.name, 'box_uv': True, 'rescale': False, 'locked': False, 'render_order': 'default',
                  'allow_mirror_modeling': True, 'from': [round(v, 4) for v in c.frm],
                  'to': [round(c.frm[i] + c.size[i], 4) for i in range(3)], 'autouv': 0, 'color': color,
                  'inflate': c.infl, 'origin': [round(v, 4) for v in b.pivot], 'uv_offset': list(c.uv_offset),
                  'faces': faces, 'type': 'cube', 'uuid': uid(A.key, 'cube', b.name, c.name), 'visibility': b.visible}
            if not b.export:
                el['export'] = False
            elements.append(el)
            node['children'].append(el['uuid'])
        for ch in b.children:
            node['children'].append(outline(ch, color))
        return node

    out = [outline(r, COLORS.get(r.name, 0)) for r in A.roots] + [outline(A.ref, 0)]
    return {
        'meta': {'format_version': '4.10', 'model_format': 'modded_entity', 'box_uv': True},
        'name': A.key, 'model_identifier': A.key, 'modded_entity_version': '1.17', 'modded_entity_flip_y': True,
        'visible_box': [1, 1, 0], 'variable_placeholders': '', 'variable_placeholder_buttons': [],
        'timeline_setups': [], 'unhandled_root_fields': {},
        'resolution': {'width': size, 'height': size},
        'elements': elements, 'outliner': out,
        'textures': [{'path': '', 'name': f'{A.key}.png', 'folder': '', 'namespace': '', 'id': '0', 'group': '',
                      'width': size, 'height': size, 'uv_width': size, 'uv_height': size, 'particle': False,
                      'use_as_default': False, 'layers_enabled': False, 'sync_to_project': '', 'render_mode': 'default',
                      'render_sides': 'auto', 'pbr_channel': 'color', 'frame_time': 1, 'frame_order_type': 'loop',
                      'frame_order': '', 'frame_interpolate': False, 'visible': True, 'internal': True, 'saved': False,
                      'uuid': uid(A.key, 'tex'), 'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode('ascii')}],
    }


# ============================================================================================== apercus
class RPart:
    """Adaptateur vers le moteur de rendu du kit (cube gonfle + transformation des os)."""

    def __init__(self, bone, c):
        e = c.infl
        self.frm = [c.frm[i] - e for i in range(3)]
        self.to = [c.frm[i] + c.size[i] + e for i in range(3)]
        self.rot, self.mat, self.light, self.bone = None, c.mat, 0, bone
        self.uv = {}
        for f, (u, v, fw, fh) in box_regions(c).items():
            if fw > 0 and fh > 0:
                self.uv[f] = (u, v, fw, fh)
        self.xf = bone.transform


class RModel:
    def __init__(self, parts):
        self.parts = parts


def preview_model(A, tex):
    """Modele de rendu : armure + mannequin (zone grise ajoutee sous la texture)."""
    W = tex.width
    big = Image.new('RGBA', (W, W + 40), (0, 0, 0, 0))
    big.paste(tex, (0, 0))
    ImageDraw.Draw(big).rectangle([0, W, 39, W + 39], fill=(132, 128, 138, 255))
    parts = []
    for b, c in A.all_cubes(include_ref=True):
        p = RPart(b, c)
        if c.mat == 'mannequin':
            p.frm = [c.frm[i] + 0.05 for i in range(3)]
            p.to = [c.frm[i] + c.size[i] - 0.05 for i in range(3)]
            p.uv = {f: (0, W, max(1, int(fw)), max(1, int(fh))) for f, (u, v, fw, fh) in box_regions(c).items()}
        parts.append(p)
    return RModel(parts), big


def armor_views(A, tex, path):
    m, atlas = preview_model(A, tex)
    specs = [('Face', 180, 0), ('Profil', 270, 0), ('Dos', 0, 0), ('3/4', 215, 12)]
    imgs = [bk.render(m, atlas, yw, pt, 9, glow=False, bounds=(-15, 15, -40, 2)) for _, yw, pt in specs]
    W = sum(i.width for i in imgs) + 30 * (len(imgs) + 1)
    H = imgs[0].height + 120
    bg = bk.background(W, H)
    d = ImageDraw.Draw(bg)
    d.text((24, 16), f'{A.num:02d} · NIV. {A.level} — {A.titre}', font=bk.font(28), fill=(226, 222, 240))
    x = 30
    for (lab, _, _), im in zip(specs, imgs):
        bg.alpha_composite(im, (x, 64))
        d.text((x + (im.width - d.textlength(lab, font=bk.font(20))) / 2, 70 + im.height), lab, font=bk.font(20), fill=(190, 184, 214))
        x += im.width + 30
    bg.convert('RGB').save(path)
    return imgs[3]


def sheet(entries, path, title):
    cw, ch = entries[0][1].size
    cols = 5
    rows = (len(entries) + cols - 1) // cols
    W, H = cols * (cw + 24) + 24, rows * (ch + 60) + 90
    bg = bk.background(W, H)
    d = ImageDraw.Draw(bg)
    d.text(((W - d.textlength(title, font=bk.font(36))) / 2, 22), title, font=bk.font(36), fill=(226, 222, 240))
    for k, (A, im) in enumerate(entries):
        x = 24 + (k % cols) * (cw + 24)
        y = 80 + (k // cols) * (ch + 60)
        d.rectangle([x - 4, y - 4, x + cw + 4, y + ch + 40], outline=(70, 66, 86), width=2)
        bg.alpha_composite(im, (x, y))
        lab = f'{A.num:02d} · NIV. {A.level}'
        d.text((x + (cw - d.textlength(lab, font=bk.font(20))) / 2, y + ch + 8), lab, font=bk.font(20), fill=(232, 228, 246))
    bg.convert('RGB').save(path)


# ============================================================================================== main
def main():
    os.makedirs(os.path.join(ICI, 'apercu'), exist_ok=True)
    thumbs = []
    for fn in PANOPLIES:
        A = fn()
        if A.level >= 50:   # armures sombres : le bas du corps reste noir, l'argent est reserve aux accents
            for _, c in A.all_cubes():
                if c.mat == 'argent' and (c.name.startswith('revers_') or (c.name.startswith('genouillere_') and A.num not in (15, 18))):
                    c.mat = 'metal'
        folder = os.path.join(ICI, A.key)
        os.makedirs(folder, exist_ok=True)
        tex, emi = build_texture(A)
        tex.save(os.path.join(folder, f'{A.key}.png'))
        emi.save(os.path.join(folder, f'{A.key}_emissive.png'))
        with open(os.path.join(folder, f'{A.key}.bbmodel'), 'w', encoding='utf-8') as f:
            json.dump(bbmodel(A, tex), f, ensure_ascii=False, indent=1)
        thumb = armor_views(A, tex, os.path.join(folder, f'{A.key}_vues.png'))
        m, atlas = preview_model(A, tex)
        thumbs.append((A, bk.render(m, atlas, 205, 10, 7, glow=False, bounds=(-15, 15, -40, 2))))
        n = len(A.all_cubes())
        print(f'{A.key}  {A.titre:30s} {n:3d} cubes  texture {tex.width}x{tex.height}')
    for i in range(4):
        sheet(thumbs[i * 5:(i + 1) * 5], os.path.join(ICI, 'apercu', f'lot_{i + 1}.png'), f'ARMURES — LOT {i + 1} (PANOPLIES {i * 5 + 1:02d} À {i * 5 + 5:02d})')
    sheet(thumbs, os.path.join(ICI, 'apercu', 'panoplies.png'), 'ARMURES — 20 PANOPLIES (BLOCKBENCH, MODDED ENTITY)')
    print('Planches : apercu/lot_1..4.png, apercu/panoplies.png')


if __name__ == '__main__':
    main()
