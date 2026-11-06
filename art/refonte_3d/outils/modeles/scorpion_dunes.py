#!/usr/bin/env python3
"""Scorpion des dunes : carapace en plaques de sable, huit pattes articulees, grandes pinces, queue arquee au-dessus du cavalier, dard rouge. Siege 0,9. Os historiques : claw_l, claw_r, tail, leg0l a leg3r."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, octopode
from kit import CF, CR, FER, OR, PAL, selle, selle_y
from materiaux import stamp


def build():
    p = Project('scorpion_dunes', density=2, kind='mounts', seed=101, description='Scorpion des dunes Aeloria : refonte.')
    p.meta['y_corps'] = (4, 18)
    PL = ('segments', 'chitine')
    U = ('uni', 'chitine')
    SEAT = selle_y(.9)                                    # 15,5
    p.bone('body', (0, 9, 0))
    p.cube('body', 'thorax', (-7, 6.5, -9), (7, 14, 8), PL, pas=8)
    p.cube('body', 'tete_plaque', (-5.5, 6, -15), (5.5, 11.5, -9), PL, pas=6, ton=.3)
    p.cube('body', 'abdomen_a', (-6, 7, 8), (6, 13, 12.5), PL, pas=6, ton=.1)
    p.cube('body', 'abdomen_b', (-5, 7.5, 12.5), (5, 12.5, 16), PL, pas=7, ton=.3)
    p.cube('body', 'dessous', (-5.5, 5.5, -8), (5.5, 6.5, 12), ('nacre', 'chitine'), base=2)
    # yeux et mandibules
    p.decal('tete_plaque', 'north', stamp(['..kk....kk..', '..kR....Rk..'], PAL, 5.5, 11))
    p.sym('body', 'mandibule', (1, 6, -18), (3, 8, -15), U, base=2.2, rot=(0, -10, 0), pivot=(2, 7, -15))
    # pinces : bras en deux segments puis deux doigts
    for bone, s, c in (('claw_l', 1, 'g'), ('claw_r', -1, 'd')):
        p.bone(bone, (s * 5.5, 8.5, -12), 'body')
    p.sym('claw_l|claw_r', 'bras', (5, 7.5, -21), (7.5, 10, -11), PL, pas=6, axe='z', ton=.2, rot=(0, -30, 0), pivot=(5.5, 8.5, -12))
    p.sym('claw_l|claw_r', 'avantbras', (8.5, 7.5, -29), (11, 10, -19.5), PL, pas=6, ton=.3, rot=(0, 6, 0), pivot=(9.75, 8.5, -20))
    p.sym('claw_l|claw_r', 'paume', (7, 7, -34), (12, 11, -28), PL, pas=6, ton=.5)
    p.sym('claw_l|claw_r', 'doigt_haut', (8, 9.5, -41), (11, 11, -34), U, base=3.4, rot=(-14, 0, 0), pivot=(9.5, 10.5, -34))
    p.sym('claw_l|claw_r', 'doigt_bas', (8, 7, -40), (11, 8.5, -34), U, base=3, rot=(12, 0, 0), pivot=(9.5, 7.5, -34))
    p.sym('claw_l|claw_r', 'pique_haut', (9, 7, -44.5), (10, 9, -41), ('griffe', 'chitine'), base=3, rot=(-40, 0, 0), pivot=(9.5, 8.5, -41))
    # queue : segments enchaines, chacun sur son os, jusqu'au dard
    p.bone('tail', (0, 10, 16), 'body')
    p.bone('queue2', (0, 13, 21), 'tail')
    p.bone('queue3', (0, 19, 24), 'queue2')
    p.bone('queue4', (0, 26, 24), 'queue3')
    p.bone('dard', (0, 31, 21), 'queue4')
    p.cube('tail', 'seg1', (-3, 8.5, 15.5), (3, 13, 22), PL, pas=6, ton=.3, rot=(-30, 0, 0), pivot=(0, 10, 16))
    p.cube('queue2', 'seg2', (-2.5, 12, 19.5), (2.5, 20, 24), PL, pas=6, axe='y', ton=.3, rot=(22, 0, 0), pivot=(0, 13, 21))
    p.cube('queue3', 'seg3', (-2, 18, 22), (2, 27, 26), PL, pas=6, axe='y', ton=.4, rot=(-4, 0, 0), pivot=(0, 19, 24))
    p.cube('queue4', 'seg4', (-2, 25.5, 22), (2, 32.5, 26), PL, pas=6, axe='y', ton=.4, rot=(-26, 0, 0), pivot=(0, 26, 24))
    p.cube('dard', 'bulbe', (-2.5, 28.5, 16.5), (2.5, 32.5, 22), ('segments', 'chitine'), pas=4, ton=-.6, rot=(-8, 0, 0), pivot=(0, 31, 21))
    p.cube('dard', 'bulbe_venin', (-1.5, 28, 17), (1.5, 28.5, 21), ('uni', 'venin'), base=2.6, rot=(-8, 0, 0), pivot=(0, 31, 21))
    p.cube('dard', 'aiguillon', (-1, 27, 14.5), (1, 31, 17), ('griffe', 'chitine_sombre'), base=3, rot=(48, 0, 0), pivot=(0, 31, 17))
    p.cube('dard', 'aiguillon_bout', (-.5, 25, 13), (.5, 28.5, 14.5), ('griffe', 'chitine_sombre'), base=1.6, rot=(25, 0, 0), pivot=(0, 28.5, 14.25))
    # huit pattes : hanche levee, tibia vers le sol
    jambes = []
    for i, (z, fan) in enumerate(((-7, 22), (-2.5, 6), (2, -8), (6.5, -24))):
        g, d = f'leg{i}l', f'leg{i}r'
        p.bone(g, (6.5, 10, z))
        p.bone(d, (-6.5, 10, z))
        zb = round((z - fan * .1) * 2) / 2
        p.bone(g + '_bas', (13.5, 13, zb), g)
        p.bone(d + '_bas', (-13.5, 13, zb), d)
        p.sym(f'{g}|{d}', f'hanche{i}', (6, 9, z - 1), (14.5, 11.5, z + 1), PL, pas=6, axe='x', ton=.2, rot=(0, fan, 22), pivot=(6.5, 10, z))
        p.sym(f'{g}_bas|{d}_bas', f'tibia{i}', (12.5, 1.5, zb - 1), (14.5, 13.5, zb + 1), U, base=3.2, rot=(0, 0, 12), pivot=(13.5, 13, zb))
        p.sym(f'{g}_bas|{d}_bas', f'griffe{i}', (15, 0, zb - 1), (16.5, 2, zb + 1), ('griffe', 'chitine'), base=2.2, hide={'down'})
        jambes += [(g, g + '_bas', 1), (d, d + '_bas', -1)]
    selle(p, SEAT, 14, 7, 6.5, 'epique', av=-6, ar=6, bas=None, strap=False, etriers=False)
    rig = {'body': 'body', 'tail': 'tail', 'stinger': 'dard', 'claws': (('claw_l', 1), ('claw_r', -1)), 'legs': jambes, 'walk': .6, 'idle': 3.0}
    add_all(p, octopode(rig))
    return p
