#!/usr/bin/env python3
"""Araignee des cavernes : huit longues pattes a genoux leves, gros abdomen marque d'une tache d'amethyste, thorax porte-selle, huit yeux, cheliceres. Siege 1,2. Os historiques : body, head, leg0l a leg3r."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, octopode
from kit import CF, CR, FER, OR, PAL, selle, selle_y
from materiaux import stamp


def build():
    p = Project('araignee_cavernes', density=2, kind='mounts', seed=111, description='Araignee des cavernes Aeloria : refonte.')
    p.meta['y_corps'] = (8, 24)
    PL = ('segments', 'chitine_sombre')
    POIL = ('fourrure', 'chitine_sombre')
    U = ('uni', 'chitine_sombre')
    SEAT = selle_y(1.2)                                   # 20
    p.bone('body', (0, 15, 0))
    p.cube('body', 'thorax', (-6, 11, -9), (6, 18.5, 6), PL, pas=8, ton=.4)
    p.cube('body', 'taille', (-3, 12.5, 6), (3, 16.5, 7.5), U, base=1.6)
    p.cube('body', 'abdomen', (-7, 9.5, 7.5), (7, 22, 21), POIL, base=2.4, bas=-.6, haut=.8)
    p.sym('body', 'abdomen_flanc', (7, 11.5, 9.5), (8.5, 20, 19), POIL, base=2.6, bas=-.6, haut=.8)
    p.cube('body', 'abdomen_haut', (-5.5, 22, 9.5), (5.5, 24, 19), POIL, base=2.8, bas=-.3, haut=.8)
    p.cube('body', 'abdomen_bout', (-5, 11, 21), (5, 20.5, 23), POIL, base=2.2, bas=-.6, haut=.8)
    p.cube('body', 'abdomen_dessous', (-5, 8.5, 9.5), (5, 9.5, 19), POIL, base=1.8)
    # marque d'amethyste sur le dos et l'arriere
    MARQUE = ['...VV...', '..VPPV..', '.VPPPPV.', 'VPPVVPPV', '.VPPPPV.', '..VPPV..', '...VV...']
    p.decal('abdomen_haut', 'up', stamp(MARQUE, PAL, -2, 11))
    p.decal('abdomen_bout', 'south', stamp(MARQUE, PAL, -2, 18))
    p.sym('body', 'poils_abdomen', (8.5, 11, 9.5), (9, 21, 19), ('crete', 'chitine_sombre'), base=1.6, ecart=2.4, bas=True, pas=3, dent=3, **CR)
    # tete et cheliceres
    p.bone('head', (0, 14, -9), 'body')
    p.cube('head', 'crane', (-4.5, 10.5, -15), (4.5, 17, -8.5), PL, pas=6, ton=.6)
    p.decal('crane', 'north', stamp(['..R......R..', '.RYR....RYR.', '..R......R..', '............', '....R..R....', '.R........R.'], PAL, 4.5, 16.5))
    p.bone('machoire', (0, 11, -15), 'head')
    p.sym('machoire', 'chelicere', (.5, 7, -18), (3.5, 12, -14.5), U, base=2.4, rot=(-8, 0, 0), pivot=(2, 12, -15))
    p.sym('machoire', 'croc', (1.5, 4, -18), (3, 7.5, -16.5), ('griffe', 'venin'), base=2.6, rot=(16, 0, -6), pivot=(2.25, 7.5, -17.25))
    # huit pattes : femur qui monte, tibia qui redescend vers le sol
    jambes = []
    for i, (z, fan) in enumerate(((-7, 34), (-3, 12), (1, -10), (5, -32))):
        g, d = f'leg{i}l', f'leg{i}r'
        p.bone(g, (5.5, 14.5, z))
        p.bone(d, (-5.5, 14.5, z))
        p.sym(f'{g}|{d}', f'femur{i}', (5, 13.5, z - 1), (17, 16, z + 1), ('fourrure', 'chitine_sombre'), base=2.4, bas=-.3, haut=.4, rot=(0, fan, 38), pivot=(5.5, 14.5, z))
        import math
        L = 11.5
        a = math.radians(fan)
        ex = 5.5 + L * math.cos(math.radians(38)) * math.cos(a)
        ey = 14.5 + L * math.sin(math.radians(38))
        ez = z - L * math.cos(math.radians(38)) * math.sin(a)
        ex2, ez2 = round(ex * 2) / 2, round(ez * 2) / 2
        p.bone(g + '_bas', (ex2, ey, ez2), g)
        p.bone(d + '_bas', (-ex2, ey, ez2), d)
        hy = round(ey * 2) / 2 + .5
        p.sym(f'{g}_bas|{d}_bas', f'tibia{i}', (ex2 - 1, 0, ez2 - 1), (ex2 + 1, hy, ez2 + 1), ('fourrure', 'chitine_sombre'), base=1.8, bas=-.4, haut=.6, rot=(0, 0, 22), pivot=(ex2, ey, ez2), hide={'down'})
        jambes += [(g, g + '_bas', 1), (d, d + '_bas', -1)]
    selle(p, SEAT, 18.5, 6, 11, 'legendaire', av=-5.5, ar=5, bas=13.5, strap=False, etriers=False)
    rig = {'body': 'body', 'head': 'head', 'legs': jambes, 'walk': .5, 'idle': 3.0}
    add_all(p, octopode(rig))
    return p
