# -*- coding: utf-8 -*-
"""Boite aux lettres d'Aeloria (1 x 1 bloc, 3 de haut) : un poteau de bois sombre cercle d'or porte une boite bleu nuit a toit
voute, bordee d'argent, avec sa fente, son battant et un petit drapeau rouge (leve quand du courrier attend). Un cristal ambre
brille au sommet. Le bloc fonctionnel (ouvre le courrier) est le pied du poteau."""
import entity_kit as ek
from structure_common import (PIERRE, ARGENT, BOIS, FER, Masonry, silver, trim)
from entity_kit import Mat, pal

KEY = 'boite_aux_lettres'
TITLE = "Boîte aux lettres d'Aeloria"
ENTITY_CLASS = 'BoiteAuxLettres'
JAVA = 'BoiteAuxLettres'
VISIBLE_BOX = (2, 3, 0)
VIEW_SCALE = 8.0
SHEET_SCALE = 5.0
GIF_SCALE = 5.0
SHEET_YAW = 35
NO_COLLISION = ('drapeau', 'cristal', 'battant', 'lettre')
STATES = {'inactif': 'repos', 'activation': 'ouverture', 'actif': 'active'}

OR = pal('#5a3a06', '#9a6a0c', '#d9a21a', '#ffd34a', '#fff1a0')
BLEU = pal('#0c1430', '#16254f', '#22397a', '#3358b0', '#5a82e0')
ROUGE = pal('#2a0a0a', '#561212', '#8a1c1c', '#c02c2c', '#f06a6a')
AMBRE = pal('#5a2b05', '#a85a0a', '#e8901c', '#ffc255', '#fff0c8')
PAPIER = pal('#6a5a3a', '#a08c5c', '#d8c890', '#efe3b8', '#fff8dc')
STONE = Masonry(PIERRE, 0.42, seed=3)
SILVER = silver()
SILVER_RIV = silver(rivet=True)
GOLD = Mat(OR, level=0.6, noise=0.2, scale=1.3, grad=0.3, up=0.25, down=-0.2, rim=0.2)
WOOD_DARK = Mat(BOIS, level=0.35, noise=0.3, scale=(4, 1), grad=0.1)
BLUE = Mat(BLEU, level=0.5, noise=0.2, scale=1.5, grad=0.25, up=0.2, down=-0.2, rim=0.12)
BLUE_DARK = Mat(BLEU, level=0.3, noise=0.2, scale=1.5, grad=0.2, up=0.1, down=-0.2)
IRON_DARK = Mat(FER, level=0.3, noise=0.18, scale=1.5, grad=0.25, up=0.2, down=-0.2, rim=0.1)
RED = Mat(ROUGE, level=0.55, noise=0.2, scale=1.5, grad=0.2)
PAPER = Mat(PAPIER, level=0.65, noise=0.15, scale=1.5, grad=0.15)
AMBER = Mat(AMBRE, level=0.6, noise=0.22, scale=1.2, grad=0.35, up=0.25, down=-0.15, glow=True)


def build():
    m = ek.Model(KEY, TITLE)
    m.bone('racine', (0, 0, 0))

    so = m.bone('socle', (0, 0, 0), 'racine')
    m.cube(so, 'socle_base', (-7, 0, -7), (14, 3, 14), STONE)
    m.cube(so, 'socle_haut', (-5, 3, -5), (10, 2, 10), SILVER_RIV)

    po = m.bone('poteau', (0, 5, 0), 'racine')
    m.cube(po, 'poteau_fut', (-3, 5, -3), (6, 22, 6), WOOD_DARK, deco={'sides': [trim('tb', OR[3])]})
    m.cube(po, 'poteau_bague_b', (-4, 8, -4), (8, 2, 8), GOLD, share='bague')
    m.cube(po, 'poteau_bague_h', (-4, 22, -4), (8, 2, 8), GOLD, share='bague')

    bo = m.bone('boite', (0, 27, 0), 'racine')
    m.cube(bo, 'boite_corps', (-8, 27, -6), (16, 10, 12), BLUE, deco={'sides': [trim('tb', OR[3])]})
    m.cube(bo, 'boite_dessous', (-8, 26, -7), (16, 1, 14), SILVER_RIV)
    m.cube(bo, 'boite_toit_1', (-8, 37, -7), (16, 2, 14), SILVER_RIV)
    m.cube(bo, 'boite_toit_2', (-7, 39, -6), (14, 2, 12), BLUE_DARK)
    m.cube(bo, 'boite_toit_3', (-5, 41, -5), (10, 2, 10), SILVER_RIV)
    m.cube(bo, 'boite_fente', (-6, 33, -7), (12, 2, 1), IRON_DARK)
    m.cube(bo, 'boite_plaque', (-3, 29, -7), (6, 3, 1), GOLD)
    m.cube(bo, 'boite_charniere_g', (5, 28, -7), (2, 7, 1), GOLD, share='charniere')
    m.cube(bo, 'boite_charniere_d', (-7, 28, -7), (2, 7, 1), GOLD, share='charniere')

    ba = m.bone('battant', (0, 27, -7), 'racine')
    m.cube(ba, 'battant_porte', (-5, 27, -8), (10, 1, 1), SILVER_RIV)
    ba2 = m.bone('lettres', (0, 34, -5), 'racine')
    m.cube(ba2, 'lettre_1', (-3, 34, -6), (5, 3, 1), PAPER, rot=(0, 0, 8), origin=(0, 35, -6), share='lettre')
    m.cube(ba2, 'lettre_2', (0, 34, -5), (5, 3, 1), PAPER, rot=(0, 0, -6), origin=(2, 35, -5), share='lettre')

    dr = m.bone('drapeau', (9, 31, 0), 'racine')
    m.cube(dr, 'drapeau_mat', (9, 29, -1), (1, 12, 2), GOLD)
    m.cube(dr, 'drapeau_toile', (10, 36, -1), (7, 4, 1), RED)
    m.cube(dr, 'drapeau_pointe', (17, 37, -1), (2, 2, 1), RED)

    ca = m.bone('cristal', (0, 44, 0), 'racine')
    m.cube(ca, 'cristal_base', (-2, 43, -2), (4, 1, 4), SILVER_RIV)
    m.cube(ca, 'cristal_coeur', (-1.5, 44, -1.5), (3, 6, 3), AMBER)
    m.cube(ca, 'cristal_pointe', (-1, 50, -1), (2, 2, 2), AMBER)
    return m


COLORS = {'racine': 0, 'socle': 1, 'poteau': 5, 'boite': 2, 'battant': 4, 'lettres': 4, 'drapeau': 3, 'cristal': 6}


def animations(m):
    P = ek.key_poses
    L = 'linear'

    a = m.anim('repos', 4.0, 'loop')
    a.rot('drapeau', [(0, 0, 0, -80, L), (2.0, 0, 0, -78, L), (4.0, 0, 0, -80, L)])
    P(a, [(0, {}), (2.0, {'cristal': (0, 0.5, 0)}), (4.0, {})], 'position', bones=['cristal'])

    a = m.anim('ouverture', 1.0, 'once')
    a.rot('drapeau', [(0, 0, 0, -80, L), (0.6, 0, 0, 10, L), (1.0, 0, 0, 0, L)])
    a.rot('battant', [(0, 0, 0, 0, L), (0.5, -60, 0, 0, L), (1.0, -60, 0, 0, L)])

    a = m.anim('active', 2.0, 'loop')
    a.rot('drapeau', [(0, 0, 0, 0, L), (1.0, 0, 0, 12, L), (2.0, 0, 0, 0, L)])
    a.rot('cristal', [(0, 0, 0, 0, L), (2.0, 0, 360, 0, L)])
    P(a, [(0, {'cristal': (0, 1, 0)}), (1.0, {'cristal': (0, 1.8, 0)}), (2.0, {'cristal': (0, 1, 0)})], 'position', bones=['cristal'])
