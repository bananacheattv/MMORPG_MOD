"""Montures partagees 2/3/4/5 places (2 modeles par taille) : meme format voxel que gen_mounts.py.

Sortie : bbmodels/mounts/<id>.json + textures/imported/mounts/<id>.png (uniquement les 8 nouvelles montures).
Affiche aussi les positions de siege (x, z en blocs, z>0 = vers l'avant) a recopier dans MountType.
"""
import json
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_mounts as gm
from gen_mounts import Model, quadruped, quad_anims, wave

METAL = ('metal', '#9aa0a8')


def howdah(m, seats, top, w, cz, length, wood='#5a3420', cloth='#8a1e1e', trim='#e0b040', emblem='chevron',
           canopy=False, grp='body'):
    """Plateforme a rambardes + un coussin par siege ; seats = [(x, z)] en unites modele. Retourne la hauteur d'assise."""
    wd = ('leather', wood)
    hl = length / 2
    m.box(grp, (-w / 2, top, cz - hl), (w / 2, top + 1.5, cz + hl), wd)
    for sx in (-1, 1):                                    # rambardes laterales + montants
        x0, x1 = sorted((sx * w / 2, sx * (w / 2 - 1)))
        m.box(grp, (x0, top + 1.5, cz - hl), (x1, top + 4.5, cz + hl), wd)
        m.box(grp, (x0 - .2, top + 4.5, cz - hl), (x1 + .2, top + 5.4, cz + hl), ('gold', trim))
    m.box(grp, (-w / 2, top + 1.5, cz + hl - 1), (w / 2, top + 6.5, cz + hl), wd)                # dossier arriere
    m.box(grp, (-w / 2, top + 1.5, cz - hl), (w / 2, top + 4.5, cz - hl + 1), wd)               # garde-corps avant
    for sx in (-1, 1):
        x0, x1 = sorted((sx * w / 2 - .8, sx * w / 2 + .8))
        m.box(grp, (x0, top - 2, cz - hl - .5), (x1, top + 1.5, cz - hl + 1), ('gold', trim))
        m.box(grp, (x0, top - 2, cz + hl - 1), (x1, top + 1.5, cz + hl + .5), ('gold', trim))
    seat_y = top + 1.5
    for (x, z) in seats:
        m.box(grp, (x - 2.6, seat_y, z - 2.6), (x + 2.6, seat_y + 1.6, z + 2.6), ('cloth', cloth, trim, emblem))
        m.box(grp, (x - 2.6, seat_y + 1.6, z + 1.6), (x + 2.6, seat_y + 3.6, z + 2.6), ('cloth', cloth, trim, emblem))
    if canopy:
        for sx in (-1, 1):
            for z in (cz - hl + 1, cz + hl - 2):
                x0, x1 = sorted((sx * (w / 2 - 1), sx * (w / 2 - 2)))
                m.box(grp, (x0, top + 1.5, z), (x1, top + 17, z + 1), ('gold', trim))
        m.box(grp, (-w / 2 - 1, top + 17, cz - hl - 1.5), (w / 2 + 1, top + 18.5, cz + hl + 1.5), ('cloth', cloth, trim, emblem))
        m.box(grp, (-w / 2 + 1, top + 18.5, cz - hl + .5), (w / 2 - 1, top + 20, cz + hl - .5), ('cloth', cloth, trim, emblem))
    return seat_y + 1.6


def banner(m, top, cz, hl, w, cloth, trim, emblem, grp='body'):
    for sx in (-1, 1):
        x0, x1 = sorted((sx * (w / 2 + .3), sx * (w / 2 + .6)))
        m.box(grp, (x0, top - 8, cz - hl + 2), (x1, top - .5, cz + hl - 2), ('cloth', cloth, trim, emblem))


def report(key, seats, top, extra=''):
    pts = ', '.join(f'({x / 16:.3f}, {-z / 16:.3f})' for x, z in seats)
    print(f'  sieges {key}: top={top / 16:.3f} bloc  [{pts}] {extra}')


# ------------------------------------------------------------------ 2 places

def taureau_guerre():
    m = Model('taureau_guerre', 41, density=3)
    hide, dark = ('fur', '#4a2e22', '#2a1810'), ('dark', '#1e1410')
    info = quadruped(m, dict(bw=14, bh=13, bl=26, leg=11, lt=4.2, body_mat=hide, leg_mat=hide, paw_mat=dark, neck=4, neck_w=10,
                             neck_y=.7, neck_up=8, hw=10, hh=9, hl=8, snout=(7, 5, 4), snout_mat=('hide', '#8a6a58'),
                             nose=('metal', '#c8c8d0'), eye=('eye', '#f04020'), ears=(3, 2.5, hide), tail=(1.8, 10, hide), tail_up=-30))
    m.box('body', (-6.5, 22, -13), (6.5, 27, -3), hide)                                  # garrot massif
    hy, hz = info['head_y'], info['head_z'] - info['hl']
    bone = ('bone', '#efe6cc')
    for sx in (1, -1):                                                                  # cornes recourbees
        m.box('head', (sx * 5 - .9, hy + 2.5, hz + 3.5), (sx * 5 + .9, hy + 4.2, hz + 5.5), bone)
        m.box('head', (sx * 7.4 - .8, hy + 3.5, hz + 3.4), (sx * 7.4 + .8, hy + 7, hz + 5.2), bone, rot=(0, 0, -sx * 20), pivot=(sx * 7.4, hy + 3.5, hz + 4))
        m.box('head', (sx * 8.4 - .6, hy + 6.5, hz + 2.6), (sx * 8.4 + .6, hy + 9, hz + 4.4), bone, rot=(-28, 0, -sx * 25), pivot=(sx * 8.4, hy + 6.5, hz + 4))
    m.box('head', (-3.2, hy + 4.2, hz + 3), (3.2, hy + 5.6, hz + 7.5), ('metal', '#8a9098'))   # plaque frontale
    for leg in ('leg_fl', 'leg_fr', 'leg_bl', 'leg_br'):
        g = m.groups[leg]['origin']
        m.box(leg, (g[0] - 2.8, 3, g[2] - 2.8), (g[0] + 2.8, 4.6, g[2] + 2.8), METAL)
    m.box('body', (-7.4, 16, -13), (7.4, 19, -11), ('metal', '#8a9098'))                  # harnais
    m.box('body', (-7.4, 16, 3), (7.4, 19, 5), ('metal', '#8a9098'))
    seats = [(0, -5), (0, 6)]
    sy = howdah(m, seats, 24, 12, .5, 17, cloth='#7a1818', trim='#e0b040', emblem='chevron')
    banner(m, 24, .5, 8.5, 12, '#7a1818', '#e0b040', 'chevron')
    report('taureau_guerre', seats, sy, f'hauteur={max(b["to"][1] for b in m.boxes) / 16:.2f}')
    return m, quad_anims(swing=26, speed=.8), seats, sy


def lion_dore():
    m = Model('lion_dore', 42, density=3)
    fur = ('fur', '#d9a64a', '#9a6a28')
    info = quadruped(m, dict(bw=12, bh=11, bl=28, leg=12, lt=3.8, body_mat=fur, leg_mat=fur, paw_mat=('hide', '#c08a3a'), neck=4, neck_w=10,
                             neck_y=.75, neck_up=22, hw=9, hh=8, hl=7, snout=(6, 4, 4), snout_mat=('hide', '#e8c888'), nose=('dark', '#3a2418'),
                             eye=('eye', '#40c0ff'), ears=(2.5, 2.5, fur), tail=(1.6, 16, fur), tail_up=-10))
    hy, hz = info['head_y'], info['head_z']
    mane = ('fur', '#7a4a18', '#4a2a08')
    for j in range(9):                                                                  # criniere en couronne
        a = j * math.pi / 8
        x, y = math.cos(a) * 7.5, hy + 1 + math.sin(a) * 6.5
        m.box('head', (x - 1.8, y - 2, hz - 1.5), (x + 1.8, y + 2, hz + 4), mane, rot=(0, 0, math.degrees(a) - 90), pivot=(0, hy + 1, hz))
    m.box('body', (-5, 15, -12), (5, 23, -4), mane)                                     # criniere sur les epaules
    m.box('tail', (-2.2, 25 - 2, 22), (2.2, 25 + 2.2, 28), mane, rot=(-10, 0, 0), pivot=(0, 25, 14))   # touffe de queue
    for leg in ('leg_fl', 'leg_fr', 'leg_bl', 'leg_br'):
        g = m.groups[leg]['origin']
        m.box(leg, (g[0] - 2.5, 3.5, g[2] - 2.5), (g[0] + 2.5, 5, g[2] + 2.5), ('gold', '#e8c040'))
    for z in (-6, 6):
        m.box('body', (-6.4, 12, z), (6.4, 14, z + 2), ('gold', '#e8c040'))
    seats = [(0, -5.5), (0, 5.5)]
    sy = howdah(m, seats, 23, 11, 0, 17, wood='#6a3a1a', cloth='#c8a020', trim='#fff0b0', emblem='sun' if 'sun' in gm.EMBLEMS else 'chevron')
    banner(m, 23, 0, 8.5, 11, '#c8a020', '#fff0b0', 'sun' if 'sun' in gm.EMBLEMS else 'chevron')
    report('lion_dore', seats, sy, f'hauteur={max(b["to"][1] for b in m.boxes) / 16:.2f}')
    return m, quad_anims(swing=32, speed=.7), seats, sy


# ------------------------------------------------------------------ 3 places

def rhinoceros_cuirasse():
    m = Model('rhinoceros_cuirasse', 43, density=2.4)
    hide = ('hide', '#7a7a80')
    info = quadruped(m, dict(bw=18, bh=15, bl=34, leg=10, lt=6, body_mat=hide, leg_mat=hide, paw_mat=('dark', '#3a3a40'), neck=4, neck_w=13,
                             neck_y=.55, neck_up=0, hw=12, hh=11, hl=10, snout=(8, 6, 6), snout_mat=hide, nose=('dark', '#2a2a30'),
                             eye=('eye', '#f0c020'), ears=(3, 3.5, hide), tail=(1.8, 8, hide), tail_up=-40, leg_in=.2))
    hy, hz = info['head_y'], info['head_z'] - info['hl']
    bone = ('bone', '#e6dcc0')
    m.box('head', (-2.6, hy - 1, hz - 6), (2.6, hy + 4, hz - 1.5), bone, rot=(35, 0, 0), pivot=(0, hy, hz - 4))           # grande corne
    m.box('head', (-1.8, hy + 1.5, hz - 1), (1.8, hy + 6, hz + 3), bone, rot=(25, 0, 0), pivot=(0, hy + 2, hz))           # petite corne
    plate = ('metal', '#7a8088')
    for z in (-12, -4, 4, 12):                                                          # plaques d'armure segmentees
        m.box('body', (-9.6, 20, z), (9.6, 23, z + 6), plate)
        m.box('body', (-9.8, 11, z), (-9.2, 20, z + 6), plate, mirror=True)
    m.box('head', (-5.5, hy + 4.5, hz + 2), (5.5, hy + 6.5, hz + 9), plate)
    seats = [(0, -10), (0, 0), (0, 10)]
    sy = howdah(m, seats, 23, 15, 0, 32, cloth='#2a3a6a', trim='#d9b040', emblem='chevron')
    banner(m, 23, 0, 16, 15, '#2a3a6a', '#d9b040', 'chevron')
    report('rhinoceros_cuirasse', seats, sy, f'hauteur={max(b["to"][1] for b in m.boxes) / 16:.2f}')
    return m, quad_anims(swing=20, speed=.95), seats, sy


def bison_geant():
    m = Model('bison_geant', 44, density=2.4)
    fur = ('fur', '#5a3a24', '#2e1c10')
    info = quadruped(m, dict(bw=18, bh=17, bl=34, leg=11, lt=5, body_mat=fur, leg_mat=fur, paw_mat=('dark', '#1e140c'), neck=4, neck_w=14,
                             neck_y=.7, neck_up=-6, head_up=-2, hw=11, hh=11, hl=9, snout=(8, 6, 4), snout_mat=('hide', '#3a2a20'), nose=('dark', '#201610'),
                             eye=('eye', '#e8b020'), ears=(3, 2.5, fur), tail=(2, 12, fur), tail_up=-20, leg_in=.3))
    m.box('body', (-8, 28, -17), (8, 36, -1), fur)                                      # bosse
    for z in range(-15, 2, 4):                                                          # poils longs
        m.box('body', (-8.4, 24, z), (8.4, 30, z + 3), ('fur', '#6a4630', '#2e1c10'), rot=(0, 0, 0))
    hy, hz = info['head_y'], info['head_z'] - info['hl']
    bone = ('bone', '#d8ccb0')
    for sx in (1, -1):
        m.box('head', (sx * 6 - .9, hy + 3, hz + 3), (sx * 6 + .9, hy + 5, hz + 6), bone)
        m.box('head', (sx * 8 - .8, hy + 4, hz + 3.5), (sx * 8 + .8, hy + 8.5, hz + 5.3), bone, rot=(0, 0, -sx * 18), pivot=(sx * 8, hy + 4, hz + 4))
    m.box('head', (-4, hy - 8, hz + 2), (4, hy - 2, hz + 4), ('fur', '#3a2414', '#1e1008'))   # barbe
    seats = [(0, -9), (0, 0.5), (0, 10)]
    sy = howdah(m, seats, 28.5, 15, 1, 31, wood='#4a2c18', cloth='#5a2a1a', trim='#e8c060', emblem='leaf')
    banner(m, 28.5, 1, 15, 15, '#5a2a1a', '#e8c060', 'leaf')
    report('bison_geant', seats, sy, f'hauteur={max(b["to"][1] for b in m.boxes) / 16:.2f}')
    return m, quad_anims(swing=22, speed=.9), seats, sy


# ------------------------------------------------------------------ 4 places

def _elephantine(m, skin, tusk_len, ear_scale, armor):
    info = quadruped(m, dict(bw=22, bh=20, bl=38, leg=16, lt=7, body_mat=skin, leg_mat=skin, paw_mat=('hide', '#5a5048'), neck=3, neck_w=16,
                             neck_y=.6, neck_up=0, hw=14, hh=15, hl=10, snout=(6, 6, 2), snout_mat=skin, eye=('eye', '#1a1210'),
                             tail=(1.6, 12, skin), tail_up=-45, leg_in=.4, leg_edge=2))
    hy, hz = info['head_y'], info['head_z'] - info['hl']
    ear = ('hide', '#7a6a60')
    for sx in (1, -1):                                                                  # grandes oreilles
        m.box('head', (sx * 7.5 - .4, hy - 7 * ear_scale, hz + 2), (sx * 7.5 + .4, hy + 7 * ear_scale, hz + 12), ear, rot=(0, -sx * 20, 0), pivot=(sx * 7.5, hy, hz + 2))
    for k in range(5):                                                                  # trompe segmentee
        t = 7.5 - k * 1.1
        m.box('head', (-t / 2, hy - 4 - k * 3.6, hz - 1.6 - k * .6), (t / 2, hy - .6 - k * 3.6, hz + 3.2 - k * .6), skin)
    m.box('head', (-3, hy - 22, hz - 5), (3, hy - 17, hz - 1.2), skin, rot=(-25, 0, 0), pivot=(0, hy - 17, hz - 1))
    bone = ('bone', '#f0e8d0')
    for sx in (1, -1):
        m.box('head', (sx * 4.5 - 1.2, hy - 6, hz - 1), (sx * 4.5 + 1.2, hy - 2, hz + 2), bone)
        m.box('head', (sx * 5 - 1, hy - 8.5, hz - tusk_len), (sx * 5 + 1, hy - 5, hz - 1), bone, rot=(-30, 0, 0), pivot=(sx * 5, hy - 6, hz - 1))
    if armor:
        plate, gold = ('metal', '#8a9098'), ('gold', '#d9a830')
        m.box('head', (-5.5, hy + 3, hz + 1), (5.5, hy + 7, hz + 7), plate)
        m.box('head', (-1, hy + 7, hz + 2), (1, hy + 11, hz + 6), gold)
        for z in (-14, -5, 4, 13):
            m.box('body', (-11.8, 18, z), (11.8, 21, z + 3), plate)
            m.box('body', (-12, 12, z), (-11.4, 18, z + 3), gold, mirror=True)
    return info


def mammouth():
    m = Model('mammouth', 45, density=2)
    fur = ('fur', '#6a4a30', '#3a2414')
    info = _elephantine(m, fur, 12, 1.0, False)
    for z in range(-17, 18, 5):                                                         # franges de poils
        m.box('body', (-11.8, 12, z), (11.8, 17, z + 4), ('fur', '#7a5a3c', '#3a2414'))
    m.box('body', (-8, 36, -19), (8, 41, -2), fur)                                      # bosse de crane
    m.box('tail', (-3, 28, 18), (3, 33, 24), ('fur', '#4a3220', '#2a1a0c'))
    seats = [(-5.5, -8), (5.5, -8), (-5.5, 8), (5.5, 8)]
    sy = howdah(m, seats, 36, 22, 0, 32, wood='#4a3220', cloth='#3a5a8a', trim='#e0e8f0', emblem='snow' if 'snow' in gm.EMBLEMS else 'chevron')
    banner(m, 36, 0, 16, 22, '#3a5a8a', '#e0e8f0', 'snow' if 'snow' in gm.EMBLEMS else 'chevron')
    report('mammouth', seats, sy, f'hauteur={max(b["to"][1] for b in m.boxes) / 16:.2f}')
    return m, quad_anims(swing=18, speed=1.0, extra=[(wave('head', 'r', 'x', 6, 1.0, .25), 'walk')]), seats, sy


def elephant_guerre():
    m = Model('elephant_guerre', 46, density=2)
    skin = ('hide', '#8a8478')
    info = _elephantine(m, skin, 9, 1.15, True)
    m.box('body', (-9, 36, -16), (9, 38, 16), ('cloth', '#7a1a1a', '#e0b040', 'chevron'))     # tapis de selle
    seats = [(-5.5, -8), (5.5, -8), (-5.5, 8), (5.5, 8)]
    sy = howdah(m, seats, 38, 22, 0, 32, wood='#3a2418', cloth='#7a1a1a', trim='#e0b040', emblem='chevron', canopy=True)
    banner(m, 38, 0, 16, 22, '#7a1a1a', '#e0b040', 'chevron')
    report('elephant_guerre', seats, sy, f'hauteur={max(b["to"][1] for b in m.boxes) / 16:.2f}')
    return m, quad_anims(swing=18, speed=1.0, extra=[(wave('head', 'r', 'x', 6, 1.0, .25), 'walk')]), seats, sy


# ------------------------------------------------------------------ 5 places

def tortue_forteresse():
    m = Model('tortue_forteresse', 47, density=2)
    skin, shell = ('scale', '#4a7a52', '#2a4a30'), ('chitin', '#5a4a38', '#8a7a58')
    info = quadruped(m, dict(bw=26, bh=9, bl=40, leg=7, lt=8, body_mat=skin, leg_mat=skin, paw_mat=('dark', '#2a3a2c'), neck=7, neck_w=8,
                             neck_y=.7, neck_up=5, hw=9, hh=8, hl=9, snout=(6, 4, 4), snout_mat=skin, eye=('eye', '#f0d020'),
                             tail=(3, 7, skin), tail_up=-30, leg_in=.2, leg_edge=3))
    m.box('body', (-14, 16, -19), (14, 20, 19), shell)                                  # carapace en gradins
    m.box('body', (-12, 20, -16), (12, 24, 16), ('chitin', '#6a5a42', '#9a8a62'))
    m.box('body', (-14.4, 13, -19.4), (14.4, 16, 19.4), ('metal', '#8a9098'))             # cerclage metallique
    seats = [(0, -13), (-6, -3), (6, -3), (-6, 8), (6, 8)]
    sy = howdah(m, seats, 24, 24, -1.5, 34, wood='#3a2a1c', cloth='#1a6a6a', trim='#e8d070', emblem='leaf', canopy=True)
    banner(m, 24, -1.5, 17, 24, '#1a6a6a', '#e8d070', 'leaf')
    report('tortue_forteresse', seats, sy, f'hauteur={max(b["to"][1] for b in m.boxes) / 16:.2f}')
    return m, quad_anims(swing=18, speed=1.1), seats, sy


def drake_caravane():
    m = Model('drake_caravane', 48, density=2)
    scale = ('scale', '#5a2a6a', '#3a1a48')
    info = quadruped(m, dict(bw=20, bh=16, bl=42, leg=13, lt=6, body_mat=scale, leg_mat=scale, paw_mat=('dark', '#1e1024'), neck=9, neck_w=10,
                             neck_y=.8, neck_up=35, hw=11, hh=10, hl=12, snout=(8, 5, 5), snout_mat=scale, nose=('dark', '#140a18'),
                             eye=('glow', '#ffd040'), ears=(2, 4, scale), tail=(8, 26, scale), tail_up=-12, leg_in=.3))
    hy, hz = info['head_y'], info['head_z'] - info['hl']
    bone = ('bone', '#efe2c8')
    for sx in (1, -1):                                                                  # cornes + crete dorsale
        m.box('head', (sx * 4 - 1, hy + 3, hz + 4), (sx * 4 + 1, hy + 12, hz + 7), bone, rot=(-35, 0, 0), pivot=(sx * 4, hy + 3, hz + 5))
    for i, z in enumerate(range(-18, 18, 5)):
        m.box('body', (-1.4, 29, z), (1.4, 29 + 6 - abs(i - 3) * .6, z + 3), ('glow', '#ff9a30') if i % 2 else bone, rot=(-20, 0, 0), pivot=(0, 29, z + 1))
    for i in range(5):                                                                  # pointes de queue
        m.box('tail', (-1, 21, 22 + i * 5), (1, 24, 24 + i * 5), bone, rot=(-18, 0, 0), pivot=(0, 21, 23 + i * 5))
    for sx in (1, -1):                                                                  # ailerons repliés
        m.box('body', (sx * 10 - .5, 22, -6), (sx * 10 + .5, 29, 8), ('feather', '#7a3a8a', '#d090e8'), rot=(0, 0, -sx * 12), pivot=(sx * 10, 22, 0))
    seats = [(0, -13), (-5.5, -3), (5.5, -3), (-5.5, 8), (5.5, 8)]
    sy = howdah(m, seats, 29, 22, -1.5, 34, wood='#2a1a30', cloth='#4a1a6a', trim='#ffd040', emblem='flame' if 'flame' in gm.EMBLEMS else 'chevron', canopy=True)
    banner(m, 29, -1.5, 17, 22, '#4a1a6a', '#ffd040', 'flame' if 'flame' in gm.EMBLEMS else 'chevron')
    report('drake_caravane', seats, sy, f'hauteur={max(b["to"][1] for b in m.boxes) / 16:.2f}')
    return m, quad_anims(swing=24, speed=.85, extra=[(wave('tail', 'r', 'y', 18, .85), 'walk')]), seats, sy


MOUNTS = [taureau_guerre, lion_dore, rhinoceros_cuirasse, bison_geant, mammouth, elephant_guerre, tortue_forteresse, drake_caravane]


def main():
    out_models = gm.ROOT / 'bbmodels/mounts'
    out_tex = gm.ROOT / 'textures/imported/mounts'
    for f in MOUNTS:
        m, anims, seats, sy = f()
        model, tex = m.build(anims)
        (out_models / (m.key + '.json')).write_text(json.dumps(model, separators=(',', ':')), encoding='utf-8')
        tex.save(out_tex / (m.key + '.png'))
        print(f'{m.key}: {len(m.boxes)} cubes')


if __name__ == '__main__':
    main()
