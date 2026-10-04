#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Six epees (niveaux 1, 20, 40, 60, 80, 100) pour Blockbench, format "Java Block/Item".
Fichiers : epee_niv_01 ... epee_niv_100 (.bbmodel + texture PNG + modele d'item Java exporte).

Usage :  python generer_epees.py      (Python 3.8+, Pillow ; utilise ../blockbench_kit.py)

Construction (vue de face) : lame verticale (pointe vers +Y) centree en x = z = 8. La lame a une vraie epaisseur :
ame centrale plus epaisse, deux tranchants argentes plus fins de chaque cote, pointe formee par les deux tranchants
qui se rejoignent (angles classiques 67,5 ou 45 deg) et par des paliers de l'ame. Detail fin : textures.
"""
import math
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
import blockbench_kit as bk  # noqa: E402

ICI = os.path.dirname(os.path.abspath(__file__))
NS = 'mmorpg'
COLORS = {'lame': 4, 'tranchants': 6, 'garde': 1, 'poignee': 2, 'pommeau': 3, 'cristaux': 5, 'fragments': 7}


def blade(m, y0, y_edge, hw, ew, core_t, edge_t, core_mat, fuller=False, angle=67.5, edge_steps=None):
    """Ame (lame) + deux tranchants + pointe. y_edge : hauteur ou les tranchants commencent a converger."""
    deco = {'north': 'rainure', 'south': 'rainure'} if fuller else None
    bk.box(m, 'lame', 'ame', 8 - hw, y0, 8 - core_t / 2, 8 + hw, y_edge + 0.5, 8 + core_t / 2, core_mat, deco=deco)
    # tranchants droits (eventuellement dentes : largeur alternee par troncons)
    steps = edge_steps or [(y0, y_edge, ew)]
    for i, (a, b, w) in enumerate(steps):
        bk.box(m, 'tranchants', f'tranchant_gauche_{i + 1}', 8 - hw - w, a, 8 - edge_t / 2, 8 - hw, b, 8 + edge_t / 2, 'argent')
        bk.box(m, 'tranchants', f'tranchant_droit_{i + 1}', 8 + hw, a, 8 - edge_t / 2, 8 + hw + w, b, 8 + edge_t / 2, 'argent')
    # pointe : les deux tranchants se rejoignent sur l'axe
    dx = hw + ew / 2
    length = dx / math.cos(math.radians(angle))
    tip = y_edge + dx * math.tan(math.radians(angle))
    bk.seg(m, 'tranchants', 'pointe_gauche', (8 - dx, y_edge), angle, length, ew, edge_t, 'argent', ext=ew * 0.5)
    bk.seg(m, 'tranchants', 'pointe_droite', (8 + dx, y_edge), 180 - angle, length, ew, edge_t, 'argent', ext=ew * 0.5)
    # ame de la pointe : paliers qui suivent les tranchants
    h = tip - (y_edge + 0.5)
    n = 3 if h > 3 else 2
    for k in range(n):
        ya = y_edge + 0.5 + h * k / (n + 0.6)
        yb = y_edge + 0.5 + h * (k + 1) / (n + 0.6)
        frac = 1 - (k + 1) / (n + 0.6)
        w = max(0.5, (hw - 0.1) * frac * 2)
        bk.box(m, 'lame', f'pointe_ame_{k + 1}', 8 - w / 2, ya, 8 - core_t / 2 * 0.85, 8 + w / 2, yb, 8 + core_t / 2 * 0.85, core_mat)
    bk.cbox(m, 'tranchants', 'pointe', (8, tip - 0.35, 8), (0.8, 0.8, edge_t), 'argent', rot=('z', 45, [8, tip - 0.35, 8]))
    return tip


def blade_plates(m, name, x1, y1, x2, y2, core_t, mat='rune', deco=None):
    bk.plate(m, 'lame/runes', f'{name}_avant', 'north', x1, y1, x2, y2, 8 - core_t / 2 - 0.1, mat, deco=deco)
    bk.plate(m, 'lame/runes', f'{name}_arriere', 'south', x1, y1, x2, y2, 8 + core_t / 2 + 0.1, mat, deco=deco)


# ============================================================================================== niveaux
def epee_01():
    m = bk.Model('epee_niv_01', 1, 'Épée de la recrue')
    bk.col(m, 'pommeau', 'pommeau', -6.5, -5, 2.25, 'fer')
    bk.col(m, 'pommeau', 'pommeau_bout', -7, -6.5, 1.75, 'fer')
    bk.col(m, 'poignee', 'cuir', -5, 1, 1.75, 'cuir')
    bk.col(m, 'poignee', 'bourrelet_bas', -5, -4.6, 2.0, 'cuir')
    bk.col(m, 'poignee', 'bourrelet_haut', 0.6, 1, 2.0, 'cuir')
    bk.box(m, 'garde', 'garde', 3.5, 1, 7, 12.5, 2.5, 9, 'fer')
    bk.col(m, 'garde', 'garde_centre', 0.75, 2.75, 2.5, 'fer')
    blade(m, 2.5, 19.5, 1.0, 0.75, 1.2, 0.8, 'fer')
    return m, (8, -2.0)


def epee_20():
    m = bk.Model('epee_niv_20', 20, 'Épée renforcée')
    bk.col(m, 'pommeau', 'pommeau', -8, -6.25, 2.5, 'metal')
    bk.col(m, 'pommeau', 'pommeau_bout', -8.5, -8, 2.0, 'argent')
    bk.diamond_plate(m, 'cristaux', 'pierre_pommeau', (8, -7.1), 1.2, 0.6, 6.75, 9.25, gem_mat='cristal', gem_light=10)
    bk.col(m, 'poignee', 'cuir', -6.25, 0.75, 2.0, 'cuir_tresse')
    bk.col(m, 'poignee', 'bague_bas', -6.5, -6.0, 2.3, 'argent')
    bk.col(m, 'poignee', 'bague_haut', 0.5, 1.0, 2.3, 'argent')
    bk.box(m, 'garde', 'garde', 3, 0.75, 6.9, 13, 2.25, 9.1, 'metal')
    for side, (a, b) in (('gauche', (2.25, 3)), ('droite', (13, 13.75))):
        bk.box(m, 'garde', f'embout_{side}', a, 0.5, 6.6, b, 2.5, 9.4, 'argent')
    bk.col(m, 'garde', 'garde_centre', 0.25, 2.75, 3.0, 'metal')
    bk.diamond_plate(m, 'cristaux', 'pierre_bleue', (8, 1.5), 1.8, 0.9, 6.5, 9.5, gem_mat='cristal', gem_light=12)
    blade(m, 2.75, 22.5, 1.25, 0.75, 1.3, 0.8, 'fer', fuller=True)
    return m, (8, -2.75)


def epee_40():
    m = bk.Model('epee_niv_40', 40, 'Épée runique')
    bk.col(m, 'pommeau', 'pommeau', -9, -7, 2.75, 'metal')
    bk.col(m, 'pommeau', 'pommeau_bout', -9.5, -9, 2.1, 'argent')
    bk.diamond_plate(m, 'cristaux', 'gemme_pommeau', (8, -8.0), 1.5, 0.8, 6.6, 9.4, gem_mat='gemme', gem_light=12)
    bk.col(m, 'poignee', 'cuir', -7, 1, 2.0, 'cuir_tresse')
    for i, y in enumerate((-7.25, -3.25, 0.6)):
        bk.col(m, 'poignee', f'bague_{i + 1}', y, y + 0.5, 2.3, 'argent')
    bk.cbox(m, 'garde', 'garde_centre', (8, 2.0, 8), (3.2, 3.2, 2.4), 'metal', rot=('z', 45, [8, 2.0, 8]))
    bk.diamond_plate(m, 'cristaux', 'cristal_central', (8, 2.0), 2.2, 1.2, 6.8, 9.2, gem_mat='cristal', gem_light=15)
    for mirror in (False, True):
        bk.poly(m, 'garde', 'bras_' + ('droit' if mirror else 'gauche'), (6.6, 1.6),
                [(3.6, 157.5, 1.2), (1.6, 135, 0.9, 'argent'), (1.2, 112.5, 0.6, 'argent')], 1.6, 'metal', mirror=mirror)
    blade(m, 3.25, 25.5, 1.5, 0.8, 1.4, 0.9, 'metal', fuller=True)
    blade_plates(m, 'runes_rainure', 7.55, 5.0, 8.45, 24.5, 1.4)
    return m, (8, -3.0)


def epee_60():
    m = bk.Model('epee_niv_60', 60, 'Épée du conquérant')
    bk.col(m, 'pommeau', 'pommeau', -10, -7.5, 3.0, 'metal')
    bk.col(m, 'pommeau', 'pommeau_bout', -10.5, -10, 2.2, 'argent')
    bk.col(m, 'pommeau', 'sertissure_bas', -9.9, -9.5, 3.3, 'argent')
    bk.col(m, 'pommeau', 'sertissure_haut', -8.0, -7.6, 3.3, 'argent')
    bk.col_plates(m, 'cristaux', 'cristal_serti', -9.4, -8.1, 3.0, 0.55, 'cristal', gap=0.05)
    bk.col(m, 'poignee', 'cuir', -7.5, 1, 2.0, 'cuir_tresse')
    for i, y in enumerate((-7.5, -3.5, 0.6)):
        bk.col(m, 'poignee', f'bague_{i + 1}', y, y + 0.5, 2.35, 'argent')
    bk.col(m, 'garde', 'garde_centre', 0.5, 3.5, 3.2, 'metal')
    bk.diamond_plate(m, 'cristaux', 'gemme_garde', (8, 2.0), 2.4, 1.3, 6.4, 9.6, gem_mat='gemme', gem_light=13)
    for mirror in (False, True):
        sd = 'droit' if mirror else 'gauche'
        bk.poly(m, 'garde', f'crochet_{sd}', (6.4, 2.0), [(3.2, 180, 1.4), (1.8, 135, 1.1), (1.6, 90, 0.8, 'argent')], 1.8, 'metal', mirror=mirror)
        bk.poly(m, 'garde', f'ergot_{sd}', (4.2, 1.4), [(1.4, 247.5, 0.6, 'argent')], 1.2, 'argent', mirror=mirror)
    bk.box(m, 'lame', 'talon', 6.0, 3.5, 7.0, 10.0, 5.0, 9.0, 'metal')
    bk.box(m, 'tranchants', 'talon_liseret', 5.75, 4.75, 6.9, 10.25, 5.25, 9.1, 'argent')
    blade(m, 3.5, 27.5, 1.75, 1.0, 1.5, 1.0, 'metal', fuller=True, angle=45)
    blade_plates(m, 'canal_energie', 7.55, 5.5, 8.45, 27.0, 1.5, mat='energie')
    return m, (8, -3.25)


def epee_80():
    m = bk.Model('epee_niv_80', 80, 'Épée légendaire')
    bk.col(m, 'pommeau', 'pommeau', -10.5, -8, 3.4, 'metal')
    bk.col(m, 'pommeau', 'pommeau_couronne', -9.6, -8.6, 3.9, 'argent')
    bk.col_plates(m, 'cristaux', 'cristal_pommeau', -10.3, -8.8, 3.4, 0.6, 'cristal', gap=0.05)
    bk.crystal(m, 'cristaux', 'cristal_pointe_pommeau', 8, -11.6, 8, 1.6, 2.4)
    bk.col(m, 'poignee', 'cuir', -8, 1, 2.0, 'cuir_tresse')
    for i, y in enumerate((-8.25, -3.75, 0.6)):
        bk.col(m, 'poignee', f'bague_{i + 1}', y, y + 0.5, 2.4, 'argent')
    bk.col(m, 'garde', 'garde_centre', 0.5, 3.75, 3.6, 'metal')
    bk.diamond_plate(m, 'cristaux', 'cristal_garde', (8, 2.1), 2.8, 1.6, 6.2, 9.8, gem_mat='cristal', gem_light=15)
    for mirror in (False, True):
        sd = 'droit' if mirror else 'gauche'
        end = bk.poly(m, 'garde', f'bras_{sd}', (6.2, 2.2), [(4.0, 180, 1.5), (1.4, 157.5, 1.2, 'argent')], 2.0, 'metal', mirror=mirror)
        bk.poly(m, 'garde', f'couche_{sd}', (6.2, 3.1), [(3.2, 157.5, 0.7, 'argent')], 1.4, 'argent', mirror=mirror)
        bk.rbox(m, 'cristaux', f'eclat_garde_{sd}', (end[0], end[1] + 1.0, 8), (0.9, 2.2, 0.9), 22.5 if not mirror else -22.5, 'cristal', light=15)
    hw, core_t = 2.25, 1.6
    blade(m, 3.75, 27.5, hw, 1.0, core_t, 1.1, 'metal', fuller=False, angle=45)
    # structure a plusieurs couches : nervure centrale epaisse bordee d'argent, cristaux integres
    bk.box(m, 'lame', 'nervure', 7.25, 4.0, 6.9, 8.75, 26.5, 9.1, 'metal')
    for x1, x2 in ((6.95, 7.25), (8.75, 9.05)):
        bk.box(m, 'lame', 'liseret_nervure', x1, 4.0, 7.0, x2, 26.5, 9.0, 'argent')
    for i, y in enumerate((9.0, 15.0, 21.0)):
        bk.cbox(m, 'cristaux', f'cristal_integre_{i + 1}', (8, y, 8), (1.3, 1.3, 2.6), 'cristal', rot=('z', 45, [8, y, 8]), light=15)
    for i, (a, b) in enumerate(((10.2, 13.8), (16.2, 19.8), (22.2, 26.0))):
        bk.plate(m, 'lame/runes', f'energie_{i + 1}_avant', 'north', 7.8, a, 8.2, b, 6.8, 'energie')
        bk.plate(m, 'lame/runes', f'energie_{i + 1}_arriere', 'south', 7.8, a, 8.2, b, 9.2, 'energie')
    for i, c in enumerate(((2.2, 6.5, 8), (13.8, 6.5, 8), (3.0, 9.0, 6), (13.0, 9.0, 10), (1.5, 3.8, 9), (14.5, 3.8, 7))):
        bk.frag(m, 'fragments', f'fragment_{i + 1}', c, 0.8 if i < 2 else 0.6, 'z' if i % 2 == 0 else 'x')
    return m, (8, -3.5)


def epee_100():
    m = bk.Model('epee_niv_100', 100, 'Épée mythique')
    bk.col(m, 'pommeau', 'pommeau', -11, -8.25, 3.6, 'metal')
    bk.col(m, 'pommeau', 'pommeau_couronne', -10.2, -9.0, 4.2, 'argent')
    bk.col_plates(m, 'cristaux', 'cristal_pommeau', -10.8, -8.5, 3.6, 0.6, 'cristal', gap=0.05)
    bk.crystal(m, 'cristaux', 'cristal_pointe_pommeau', 8, -12.7, 8, 1.8, 2.8)
    bk.col(m, 'poignee', 'cuir', -8.25, 0.75, 2.2, 'cuir_tresse')
    for i, y in enumerate((-8.5, -4.0, 0.35)):
        bk.col(m, 'poignee', f'bague_{i + 1}', y, y + 0.5, 2.6, 'argent')
    bk.col(m, 'garde', 'garde_centre', 0.25, 4.25, 4.0, 'metal')
    bk.diamond_plate(m, 'cristaux', 'cristal_garde', (8, 2.3), 3.2, 1.8, 6.0, 10.0, gem_mat='cristal', gem_light=15)
    bk.box(m, 'garde', 'barre_basse', 2, 0.75, 6.75, 14, 2.25, 9.25, 'metal')
    for mirror in (False, True):
        sd = 'droite' if mirror else 'gauche'
        end = bk.poly(m, 'garde', f'aile_{sd}', (5.8, 2.6), [(3.6, 157.5, 1.4), (2.0, 135, 1.0, 'argent'), (1.4, 112.5, 0.7, 'argent')],
                      1.8, 'metal', mirror=mirror)
        bk.poly(m, 'garde', f'crochet_{sd}', (2.6, 1.5), [(1.6, 225, 0.7, 'argent'), (0.9, 270, 0.5, 'argent')], 1.2, 'argent', mirror=mirror)
        bk.rbox(m, 'cristaux', f'eclat_aile_{sd}', (end[0] + (0.6 if mirror else -0.6), end[1] - 0.6, 8), (0.8, 2.0, 0.8),
                -22.5 if mirror else 22.5, 'cristal', light=15)
    hw, core_t = 2.75, 1.7
    # tranchants dentes (largeur alternee) : silhouette acérée
    steps, y = [], 4.25
    k = 0
    while y < 27.0:
        b = min(27.0, y + 3.0)
        steps.append((y, b, 1.25 if k % 2 == 0 else 1.0))
        y, k = b, k + 1
    tip = blade(m, 4.25, 27.0, hw, 1.25, core_t, 1.15, 'metal', fuller=True, angle=45, edge_steps=steps)
    # armature noire autour d'un grand cristal dans la partie basse de la lame (fenetre ouverte)
    m.parts = [p for p in m.parts if p.name != 'ame']
    m.names.discard('ame')
    bk.box(m, 'lame', 'ame_base', 8 - hw, 4.25, 8 - core_t / 2, 8 + hw, 5.5, 8 + core_t / 2, 'metal')
    bk.box(m, 'lame', 'ame', 8 - hw, 14.75, 8 - core_t / 2, 8 + hw, 27.5, 8 + core_t / 2, 'metal', deco={'north': 'rainure', 'south': 'rainure'})
    for name, x1, x2 in (('armature_gauche', 8 - hw, 6.5), ('armature_droite', 9.5, 8 + hw)):
        bk.box(m, 'lame', name, x1, 5.5, 8 - core_t / 2 - 0.15, x2, 14.75, 8 + core_t / 2 + 0.15, 'metal')
    bk.box(m, 'lame', 'armature_haut', 6.0, 13.5, 8 - core_t / 2 - 0.2, 10.0, 14.75, 8 + core_t / 2 + 0.2, 'metal')
    bk.crystal(m, 'cristaux', 'grand_cristal', 8, 9.6, 8, 2.4, 6.6)
    for i, (cx, cy, ang) in enumerate(((6.7, 6.0, 45), (9.3, 6.0, -45), (6.7, 13.2, -45), (9.3, 13.2, 45))):
        bk.rbox(m, 'lame', f'griffe_armature_{i + 1}', (cx, cy, 8.0), (1.2, 0.5, core_t + 0.3), ang, 'argent')
    blade_plates(m, 'canal_energie', 7.6, 15.5, 8.4, 26.5, core_t, mat='energie')
    blade_plates(m, 'runes_base', 6.6, 14.9, 9.4, 15.0, core_t)
    for i, c in enumerate(((1.4, 7, 8), (14.6, 7, 8), (2.4, 11.5, 6), (13.6, 11.5, 10), (0.8, 3.0, 9), (15.2, 3.0, 7),
                           (2.6, 16.5, 9), (13.4, 16.5, 7))):
        bk.frag(m, 'fragments', f'fragment_{i + 1}', c, 0.9 if i < 2 else 0.65, 'z' if i % 2 == 0 else 'x')
    return m, (8, -3.75)


EPEES = [epee_01, epee_20, epee_40, epee_60, epee_80, epee_100]


def main():
    mc = os.path.join(ICI, 'minecraft', 'assets', NS)
    for sub in ('textures', 'apercu', os.path.join(mc, 'models', 'item'), os.path.join(mc, 'textures', 'item'), os.path.join(mc, 'items')):
        os.makedirs(os.path.join(ICI, sub), exist_ok=True)
    bk.write_json(os.path.join(ICI, 'minecraft', 'pack.mcmeta'),
                  {'pack': {'description': 'Epees Eldoria (6 evolutions)', 'min_format': 97, 'max_format': 97}}, indent=2)
    done = []
    for fn in EPEES:
        m, grip = fn()
        atlas = bk.build_atlas([m], m.key)
        display = bk.handheld_display(m, grip, third=0.6, first=0.5, ref_height=34)
        atlas.save(os.path.join(ICI, 'textures', f'{m.key}.png'))
        atlas.save(os.path.join(mc, 'textures', 'item', f'{m.key}.png'))
        bk.write_json(os.path.join(ICI, f'{m.key}.bbmodel'), bk.bbmodel(m, atlas, m.key, NS, display, COLORS))
        bk.write_json(os.path.join(mc, 'models', 'item', f'{m.key}.json'),
                      bk.java_model(m, atlas, f'{NS}:item/{m.key}', display, COLORS, 'Eldoria MMORPG - generer_epees.py'))
        bk.write_json(os.path.join(mc, 'items', f'{m.key}.json'), {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{m.key}'}}, indent=2)
        bk.views(m, atlas, os.path.join(ICI, 'apercu', f'{m.key}_vues.png'), f'{m.titre} — niveau {m.level}  ({len(m.parts)} cubes)')
        x1, x2, y1, y2 = bk.bbox(m)
        print(f'{m.key:14s} niveau {m.level:3d}  {len(m.parts):3d} cubes  texture {atlas.width}x{atlas.height}  '
              f'taille {x2 - x1:.1f} x {y2 - y1:.1f}  icone x{display["gui"]["scale"][0]}')
        done.append((m, atlas, f'NIV. {m.level}', m.titre))
    bk.lineup(done, os.path.join(ICI, 'apercu', 'evolution.png'), 'ÉPÉES — ÉVOLUTION (BLOCKBENCH, JAVA BLOCK/ITEM)', min_w=230)
    print('Apercu : apercu/evolution.png')


if __name__ == '__main__':
    main()
