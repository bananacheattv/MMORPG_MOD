#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Outils des mines d'Aeloria (format Blockbench "Java Block/Item", modeles 3D en cuboides) : pioches, haches, pelles et houes
en six paliers (debutant, Verdite, Argentite, Ambrasite, Glacium, Neantite).

Usage :  python generer_outils.py      (Python 3.8+, Pillow ; utilise ../blockbench_kit.py)
Sorties : <cle>.bbmodel, textures/<cle>.png, apercu/, et dans src/main/resources/assets/mmorpg : models/item, textures/item, items.
La liste TIERS doit rester identique a com.mmorpg.mine.MineTools (Java) ; les noms francais sont dans tools/gen_mine.py (langue).
"""
import json
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
import blockbench_kit as bk  # noqa: E402

ICI = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(ICI, '..', '..', 'src', 'main', 'resources', 'assets', 'mmorpg')
NS = 'mmorpg'
COLORS = {'manche': 2, 'tete': 4, 'gemmes': 5, 'ornements': 6}

# palier : (id, nom, matiere de la tete, palette des gemmes (base, sombre, clair, eclat))
TIERS = [
    ('debutant', 'du débutant', 'fer', None),
    ('verdite', 'de Verdite', 'metal', ((70, 200, 90), (28, 120, 56), (150, 245, 140), (225, 255, 210))),
    ('argentite', "d'Argentite", 'argent', ((165, 185, 225), (96, 116, 168), (235, 242, 255), (255, 255, 255))),
    ('ambrasite', "d'Ambrasite", 'metal', ((255, 130, 40), (176, 70, 20), (255, 205, 90), (255, 240, 170))),
    ('glacium', 'de Glacium', 'argent', ((96, 215, 245), (40, 140, 210), (200, 255, 255), (240, 255, 255))),
    ('neantite', 'de Néantite', 'metal', ((160, 90, 245), (92, 40, 180), (226, 170, 255), (250, 220, 255))),
]

_paint = bk.paint_base
TIER_PAL = {f'gemme_{t[0]}': t[3] for t in TIERS if t[3]}


def paint(mat, w, h, side, r, smooth=False):
    """Matieres de palier : gemme facettee a la palette du minerai (la matiere 'gemme' du kit, recoloree)."""
    if mat in TIER_PAL:
        saved = bk.PAL['gemme']
        bk.PAL['gemme'] = list(TIER_PAL[mat])
        try:
            return _paint('gemme', w, h, side, r, smooth)
        finally:
            bk.PAL['gemme'] = saved
    return _paint(mat, w, h, side, r, smooth)


bk.paint_base = paint
bk.GLOWING = set(bk.GLOWING) | set(TIER_PAL)


def handle(m, tier, y0=0.5, y1=13.5, w=1.6):
    """Manche : bois (cuir tresse au palier 3+), bagues metalliques et pommeau."""
    bk.col(m, 'manche', 'manche', y0, y1, w, 'bois' if tier < 3 else 'cuir_tresse')
    bk.col(m, 'manche', 'pommeau', y0 - 0.5, y0 + 0.6, w + 0.7, 'fer' if tier == 0 else 'argent')
    for i, y in enumerate((y0 + 2.4, y0 + 6.0)):
        bk.col(m, 'manche', f'bague_{i + 1}', y, y + 0.6, w + 0.5, 'fer' if tier == 0 else 'argent')
    if tier >= 2:
        bk.col(m, 'ornements', 'bague_haut', y1 - 1.4, y1 - 0.8, w + 0.55, 'argent')


def gems(m, tier, centre, spread=0.0):
    """Gemmes du palier au centre de la tete ; les paliers hauts ajoutent des fragments flottants."""
    if tier == 0:
        return
    mat = f'gemme_{TIERS[tier][0]}'
    cx, cy = centre
    bk.cbox(m, 'gemmes', 'gemme_centre', (cx, cy, 8), (2.4, 2.4, 1.0 + (tier >= 3) * 0.4), mat,
            rot=('z', 45, [cx, cy, 8]), light=8 + tier)
    if tier >= 2:
        for i, d in enumerate((-1, 1)):
            bk.cbox(m, 'gemmes', f'gemme_flanc_{i + 1}', (cx + d * (2.6 + spread), cy - 0.2, 8), (1.2, 1.2, 0.8), mat,
                    rot=('z', 45, [cx + d * (2.6 + spread), cy - 0.2, 8]), light=7 + tier)
    if tier >= 4:
        for i, (dx, dy) in enumerate(((-4.4, 3.2), (4.4, 3.2), (0, 4.6))):
            bk.frag(m, 'gemmes', f'fragment_{i + 1}', (cx + dx, cy + dy, 8), 0.7 + 0.1 * (i == 2), 'z', mat, 12)


# ============================================================================================ outils
def pioche(tier):
    t = TIERS[tier]
    m = bk.Model(f'pioche_{t[0]}', tier, f'Pioche {t[1]}')
    handle(m, tier, 0.5, 14.0)
    head = t[2]
    cy = 14.2
    bk.box(m, 'tete', 'collier', 6.6, cy - 1.0, 6.7, 9.4, cy + 1.2, 9.3, head)
    for side, mirror in (('gauche', False), ('droite', True)):
        bk.poly(m, 'tete', f'bras_{side}', (9.2, cy + 0.2), [(2.8, 0, 1.9), (2.6, -22.5, 1.6), (2.2, -22.5, 1.3)], 1.4, head, mirror=mirror)
    # pointes (en gemme au palier 1+, en fer au debutant)
    tip = 'fer' if tier == 0 else f'gemme_{t[0]}'
    for side, sx in (('gauche', 1), ('droite', -1)):
        x = 8 + sx * 7.3
        bk.cbox(m, 'tete', f'pointe_{side}', (x, cy - 2.6, 8), (1.1, 2.2, 0.9), tip, rot=('z', -sx * 22.5, [x, cy - 1.3, 8]), light=0 if tier == 0 else 6 + tier)
    gems(m, tier, (8, cy + 0.1))
    return m, (8, 0.0)


def hache(tier):
    t = TIERS[tier]
    m = bk.Model(f'hache_{t[0]}', tier, f'Hache {t[1]}')
    handle(m, tier, 0.5, 14.5)
    head = t[2]
    bk.box(m, 'tete', 'talon', 6.8, 11.4, 6.7, 9.4, 14.6, 9.3, head)
    # lame en croissant d'un seul cote (vers +x)
    bk.box(m, 'tete', 'lame_dos', 9.4, 12.0, 7.1, 11.0, 14.2, 8.9, head)
    bk.poly(m, 'tete', 'lame_haut', (10.6, 14.2), [(1.6, 22.5, 1.3), (1.6, 0, 1.2), (1.4, -22.5, 1.1)], 1.5, head)
    bk.poly(m, 'tete', 'lame_bas', (10.6, 12.0), [(1.4, -22.5, 1.2), (1.4, 0, 1.1), (1.2, 22.5, 1.0)], 1.5, head)
    tip = 'argent' if tier == 0 else f'gemme_{t[0]}'
    bk.box(m, 'tete', 'tranchant', 13.4, 11.6, 7.4, 14.0, 14.4, 8.6, tip, light=0 if tier == 0 else 5 + tier)
    bk.box(m, 'tete', 'barbe', 7.4, 10.4, 7.2, 9.0, 11.6, 8.8, head)
    gems(m, tier, (8.2, 13.0), 0.0)
    return m, (8, 0.0)


def pelle(tier):
    t = TIERS[tier]
    m = bk.Model(f'pelle_{t[0]}', tier, f'Pelle {t[1]}')
    handle(m, tier, 0.5, 12.5)
    head = t[2]
    bk.box(m, 'tete', 'collet', 7.0, 12.4, 7.0, 9.0, 13.6, 9.0, head)
    bk.box(m, 'tete', 'lame', 6.0, 13.6, 7.3, 10.0, 17.0, 8.7, head)
    bk.box(m, 'tete', 'lame_haut', 6.5, 17.0, 7.3, 9.5, 18.2, 8.7, head)
    bk.box(m, 'tete', 'pointe', 7.1, 18.2, 7.4, 8.9, 19.0, 8.6, head)
    if tier:
        bk.box(m, 'tete', 'nervure', 7.7, 13.6, 8.7, 8.3, 17.6, 9.0, f'gemme_{t[0]}', light=5 + tier)
    gems(m, tier, (8, 15.2))
    return m, (8, 0.0)


def houe(tier):
    t = TIERS[tier]
    m = bk.Model(f'houe_{t[0]}', tier, f'Houe {t[1]}')
    handle(m, tier, 0.5, 14.0)
    head = t[2]
    bk.box(m, 'tete', 'collet', 6.8, 13.0, 6.8, 9.2, 15.0, 9.2, head)
    bk.box(m, 'tete', 'barre', 8.6, 14.0, 7.2, 13.0, 15.2, 8.8, head)
    bk.box(m, 'tete', 'lame', 12.4, 11.4, 7.1, 13.8, 15.2, 8.9, head)
    bk.box(m, 'tete', 'lame_bas', 12.8, 10.4, 7.3, 13.6, 11.4, 8.7, head)
    bk.cbox(m, 'tete', 'pointe_haute', (11.0, 16.0, 8), (1.2, 1.2, 1.0), head, rot=('z', 45, [11.0, 16.0, 8]))
    if tier:
        bk.box(m, 'tete', 'fil', 13.8, 11.0, 7.5, 14.2, 14.6, 8.5, f'gemme_{t[0]}', light=5 + tier)
    gems(m, tier, (9.8, 14.6))
    return m, (8, 0.0)


BUILDERS = {'pioche': (pioche, range(6)), 'hache': (hache, range(1, 6)), 'pelle': (pelle, range(1, 6)), 'houe': (houe, range(6))}


def main():
    mc = os.path.join(RES)
    for sub in ('textures', 'apercu'):
        os.makedirs(os.path.join(ICI, sub), exist_ok=True)
    for sub in ('models/item', 'textures/item', 'items'):
        os.makedirs(os.path.join(mc, sub), exist_ok=True)
    done = []
    for kind, (fn, tiers) in BUILDERS.items():
        for tier in tiers:
            m, grip = fn(tier)
            atlas = bk.build_atlas([m], m.key)
            display = bk.handheld_display(m, grip, third=0.55, first=0.5, ref_height=22)
            atlas.save(os.path.join(ICI, 'textures', f'{m.key}.png'))
            atlas.save(os.path.join(mc, 'textures', 'item', f'{m.key}.png'))
            bk.write_json(os.path.join(ICI, f'{m.key}.bbmodel'), bk.bbmodel(m, atlas, m.key, NS, display, COLORS))
            bk.write_json(os.path.join(mc, 'models', 'item', f'{m.key}.json'),
                          bk.java_model(m, atlas, f'{NS}:item/{m.key}', display, COLORS, 'Aeloria MMORPG - generer_outils.py'))
            bk.write_json(os.path.join(mc, 'items', f'{m.key}.json'), {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{m.key}'}}, indent=2)
            bk.views(m, atlas, os.path.join(ICI, 'apercu', f'{m.key}_vues.png'), f'{m.titre}  ({len(m.parts)} cubes)')
            done.append((m, atlas, kind.upper(), m.titre))
    for kind in BUILDERS:
        bk.lineup([d for d in done if d[2] == kind.upper()], os.path.join(ICI, 'apercu', f'{kind}s.png'), f'{kind.upper()}S DES MINES', min_w=150)
    print(len(done), 'outils generes')


if __name__ == '__main__':
    main()
