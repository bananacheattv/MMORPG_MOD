#!/usr/bin/env python3
"""Tortue de cristal : tres basse, carapace en plaques turquoise et amas de cristaux sur le pourtour, cou long, bec, pattes-colonnes. Siege 1,2."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from aeloria3d import Project
from animations import add_all, quadrupede
from kit import CF, CR, FER, OR, PAL, oeil, selle, selle_y, tuiles_rig
from materiaux import stamp


def build():
    p = Project('tortue_cristal', density=2, kind='mounts', seed=61, description='Tortue de cristal Aeloria : refonte.')
    p.meta['y_corps'] = (2, 20)
    SHELL = ('scutes', 'carapace')
    PEAU = ('uni', 'peau_tortue')
    GL = ('glace', 'glace')
    SEAT = selle_y(1.2)                                   # 20
    p.bone('body', (0, 10, 0))
    p.cube('body', 'plastron', (-9.5, 4, -12), (9.5, 6.5, 12), ('nacre', 'dragon_ventre'), base=2.4)
    p.cube('body', 'bord', (-11.5, 6.5, -14), (11.5, 9.5, 14), ('segments', 'carapace'), base=3.2, pas=8, axe='z')
    p.cube('body', 'dome_bas', (-10, 9.5, -12), (10, 14.5, 12), SHELL, l=16, h=12, ton=.1)
    p.cube('body', 'dome_haut', (-7.5, 14.5, -9.5), (7.5, 18, 9.5), SHELL, l=16, h=12, ton=.4)
    p.cube('body', 'bord_av', (-6, 9.5, -14), (6, 11, -12), ('segments', 'carapace'), base=3.4, pas=8, axe='x')
    # amas de cristaux sur le pourtour, jamais sous le cavalier
    K = (('a', (8, 14, 8), 2, 7, (14, 0, -26)), ('b', (5.5, 14.5, 10), 2, 5, (26, 0, -14)), ('c', (9, 11, 4), 2, 5, (6, 0, -40)), ('d', (9.5, 10, -5), 2, 6, (-10, 0, -38)), ('e', (8, 12, -9), 2, 5, (-26, 0, -24)))
    for n, (x, y, z), w, h, r in K:
        p.sym('body', f'cristal_{n}', (x, y, z), (x + w, y + h, z + w), GL, rot=r, pivot=(x + 1, y, z + 1))
    for n, (x, y, z), w, h, r in (('f', (-1, 9.5, 11), 2, 6, (34, 0, 0)), ('g', (-1, 16, 8.5), 2, 6, (22, 45, 0))):
        p.cube('body', f'cristal_{n}', (x, y, z), (x + w, y + h, z + w), GL, rot=r, pivot=(x + 1, y, z + 1))
    # tete
    p.bone('head', (0, 9, -13), 'body')
    N = dict(rot=(14, 0, 0), pivot=(0, 9, -13))
    p.cube('head', 'cou', (-2.5, 6, -22), (2.5, 11, -12), PEAU, base=2.2, **N)
    p.cube('head', 'crane', (-3.5, 9, -28), (3.5, 14.5, -21), PEAU, base=3)
    p.cube('head', 'bec', (-2, 9, -30.5), (2, 12, -28), ('uni', 'sabot'), base=2.4)
    p.cube('head', 'mandibule', (-2.5, 8, -29.5), (2.5, 9, -22), PEAU, base=1.6)
    oeil(p, 'crane', -23.5, -27, 13, couleur='B', clair='C')
    p.cube('head', 'cristal_front', (-1, 14.5, -26), (1, 18, -24), GL, rot=(-16, 45, 0), pivot=(0, 14.5, -25))
    # queue
    p.bone('tail', (0, 7, 14), 'body')
    p.cube('tail', 'queue', (-1.5, 4.5, 13.5), (1.5, 7.5, 20), PEAU, base=2, rot=(-14, 0, 0), pivot=(0, 7, 14))
    # pattes-colonnes, un seul segment : la tortue glisse, elle ne plie pas les genoux
    E = ('ecailles', 'peau_tortue')
    for g, d, n, x0, x1, z0, z1 in (('leg_fl', 'leg_fr', 'av', 5.5, 10.5, -13, -7), ('leg_bl', 'leg_br', 'ar', 5.5, 10.5, 7, 13)):
        p.bone(g, ((x0 + x1) / 2, 7, (z0 + z1) / 2))
        p.bone(d, (-(x0 + x1) / 2, 7, (z0 + z1) / 2))
        p.sym(f'{g}|{d}', n + '_patte', (x0, 1.5, z0), (x1, 7.5, z1), E, base=2.4, l=4, h=3, y=(0, 8), hide={'up'})
        p.sym(f'{g}|{d}', n + '_pied', (x0 - .5, 0, z0 - 1), (x1 + .5, 1.5, z1), E, base=1.8, l=4, h=3, y=(0, 8), hide={'down'})
        for i, xx in enumerate((x0 + .5, x0 + 2, x0 + 3.5)):
            p.sym(f'{g}|{d}', f'{n}_griffe{i}', (xx, 0, z0 - 1.5), (xx + 1, 1, z0 - 1), ('griffe', 'ivoire'), hide={'down', 'south'})
    selle(p, SEAT, 18, 6, 6.5, 'rare', av=-5.5, ar=6.5, bas=14.5, strap=False, etriers=False)
    rig = tuiles_rig(ears=(), swing=(), legs=[('leg_fl', None, 0.0), ('leg_fr', None, .5), ('leg_bl', None, .5), ('leg_br', None, 0.0)], idle=4.0, walk=1.4, walk_amp=15, walk_knee=0, run=.9, run_amp=20, run_knee=0, leg_len=7.0)
    add_all(p, quadrupede(rig, poids=.7))
    return p
