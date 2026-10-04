#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Structures multiblocs d'Eldoria pour Blockbench (format "Modded Entity"), generees entierement par script :
autel d'invocation, teleporteur, forge arcanique.

Usage :
  python generer_structures.py                    toutes les structures, avec apercus et GIF
  python generer_structures.py teleporteur        seulement celle-ci
  python generer_structures.py --rapide           sans GIF

Sorties, un dossier par structure (<nom>/) :
  <nom>.bbmodel                 projet de travail Blockbench (texture integree, groupes, animations d'etat)
  <nom>.png / <nom>_emissive.png texture pixel art (box UV) et masque des parties lumineuses
  apercu/                       vues, planche des animations, GIF
Et dans le mod (src/main/java/com/mmorpg) :
  client/model/structure/<Nom>Model.java, <Nom>Anims.java   geometrie + animations (Java vanilla 26.3)
  block/structure/StructureShapes.java                      collisions de chaque bloc de l'emprise
Necessite Python 3.8+, Pillow et numpy.
"""
import importlib
import math
import os
import sys

import numpy as np

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
sys.path.insert(0, os.path.join(ICI, '..'))
import entity_kit as ek  # noqa: E402

STRUCTURES = ['autel_invocation', 'teleporteur', 'forge_arcanique']
SRC = os.path.join(ICI, '..', '..', 'src', 'main', 'java', 'com', 'mmorpg')
HEADER = 'art/structures_blockbench/generer_structures.py'


def world_boxes(m, exclude):
    """Boites englobantes (repere Blockbench, pixels) des cubes portant une collision, pose de repos."""
    M = ek.pose(m)
    out = []
    for b in m.order:
        chain, p = [], b
        while p is not None:
            chain.append(p.name)
            p = p.parent
        if any(any(k in n for k in exclude) for n in chain):
            continue
        for c in b.cubes:
            if min(c.size) <= 0:
                continue
            C, off = np.eye(4), b.pivot
            if c.rot:
                org = c.origin or b.pivot
                C, off = ek.mat4(ek.euler_zyx(c.rot), [org[i] - b.pivot[i] for i in range(3)]), org
            pts = []
            for x in (c.frm[0], c.to[0]):
                for y in (c.frm[1], c.to[1]):
                    for z in (c.frm[2], c.to[2]):
                        pts.append((M[b.name] @ C @ np.array([x - off[0], y - off[1], z - off[2], 1.0]))[:3])
            pts = np.array(pts)
            out.append((pts.min(0), pts.max(0)))
    return out


def cell_boxes(boxes):
    """Decoupe par bloc (bloc fonctionnel = cellule 0,0,0 ; x et z decales de 8 px) : {(cx, cy, cz): [boites 0..16]}."""
    cells = {}
    for lo, hi in boxes:
        lo = [lo[0] + 8, max(0.0, lo[1]), lo[2] + 8]
        hi = [hi[0] + 8, hi[1], hi[2] + 8]
        rng = [range(math.floor(lo[i] / 16), math.ceil(hi[i] / 16)) for i in range(3)]
        for cx in rng[0]:
            for cy in rng[1]:
                for cz in rng[2]:
                    c0 = (cx * 16, cy * 16, cz * 16)
                    b = [max(lo[i], c0[i]) - c0[i] for i in range(3)] + [min(hi[i], c0[i] + 16) - c0[i] for i in range(3)]
                    if all(b[i + 3] - b[i] >= 0.5 for i in range(3)):
                        cells.setdefault((cx, cy, cz), []).append([round(v * 2) / 2 for v in b])
    return cells


def shapes_java(all_cells):
    lines = []
    for key, cells in all_cells.items():
        parts = []
        for (cx, cy, cz), bs in sorted(cells.items()):
            parts.append(f'{cx},{cy},{cz}:' + '|'.join(' '.join(ek.fnum(v) for v in b) for b in bs))
        lines.append(f'        DATA.put("{key}", "{";".join(parts)}");')
    return f"""// Genere automatiquement par {HEADER} - ne pas modifier a la main (relancer le script).
package com.mmorpg.block.structure;

import java.util.HashMap;
import java.util.Map;

/**
 * Collisions des structures multiblocs, calculees depuis les cubes des modeles Blockbench (hors cristaux, flammes,
 * portail, outils). Par structure : "cx,cy,cz:x0 y0 z0 x1 y1 z1|...;..." ; cellule (0,0,0) = bloc fonctionnel,
 * structure tournee vers le nord, coordonnees en pixels (0..16) dans chaque bloc.
 */
public final class StructureShapes {{
    public static final Map<String, String> DATA = new HashMap<>();

    static {{
{chr(10).join(lines)}
    }}

    private StructureShapes() {{
    }}
}}
"""


def generer(nom, gifs=True):
    mod = importlib.import_module(nom)
    m = mod.build()
    mod.animations(m)
    tex, emi = ek.build_texture(m)
    ek.check(m, tex)
    warn = ek.interp_warnings(m)
    if warn:
        print('  interpolations a revoir :', warn[:4])
    out = os.path.join(ICI, nom)
    ap = os.path.join(out, 'apercu')
    os.makedirs(ap, exist_ok=True)
    ek.write_json(os.path.join(out, f'{nom}.bbmodel'), ek.bbmodel(m, tex, mod.ENTITY_CLASS, mod.COLORS, mod.VISIBLE_BOX))
    tex.save(os.path.join(out, f'{nom}.png'))
    if emi.getbbox():
        emi.save(os.path.join(out, f'{nom}_emissive.png'))
    sub = f'{len(m.order)} os · {len(m.cubes())} cubes · texture {tex.width}x{tex.height} · {len(m.animations)} animations'
    ek.views_sheet(m, tex, emi, os.path.join(ap, f'{nom}_vues.png'), mod.TITLE, sub, scale=mod.VIEW_SCALE)
    ek.anim_sheet(m, tex, emi, os.path.join(ap, f'{nom}_animations.png'), f'{mod.TITLE} — états', scale=mod.SHEET_SCALE,
                  yaw=mod.SHEET_YAW)
    if gifs:
        for a in m.animations:
            ek.anim_gif(m, tex, emi, a, os.path.join(ap, f'{nom}_{a.name}.gif'), scale=mod.GIF_SCALE, yaw=mod.SHEET_YAW)
    cells = cell_boxes(world_boxes(m, mod.NO_COLLISION))
    if os.path.isdir(SRC):
        client, common = ek.java_export(m, tex, mod.JAVA, nom, {}, HEADER, client_pkg='com.mmorpg.client.model.structure',
                                        common_pkg='com.mmorpg.client.model.structure', art_dir='art/structures_blockbench')
        d = os.path.join(SRC, 'client', 'model', 'structure')
        os.makedirs(d, exist_ok=True)
        for fname, code in ((mod.JAVA + 'Model.java', client), (mod.JAVA + 'Anims.java', common)):
            with open(os.path.join(d, fname), 'w', encoding='utf-8', newline='\n') as f:
                f.write(code)
    span = [sorted({k[i] for k in cells}) for i in range(3)]
    print(f'{nom}: {sub} · emprise x {span[0][0]}..{span[0][-1]} y {span[1][0]}..{span[1][-1]} z {span[2][0]}..{span[2][-1]}'
          f' ({len(cells)} blocs avec collision)')
    return cells


if __name__ == '__main__':
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    todo = args or STRUCTURES
    all_cells = {}
    for nom in STRUCTURES:
        if nom in todo:
            all_cells[nom] = generer(nom, gifs='--rapide' not in sys.argv)
        else:   # collisions des autres structures recalculees sans regenerer leurs fichiers
            mod = importlib.import_module(nom)
            m = mod.build()
            all_cells[nom] = cell_boxes(world_boxes(m, mod.NO_COLLISION))
    if os.path.isdir(SRC):
        d = os.path.join(SRC, 'block', 'structure')
        os.makedirs(d, exist_ok=True)
        with open(os.path.join(d, 'StructureShapes.java'), 'w', encoding='utf-8', newline='\n') as f:
            f.write(shapes_java(all_cells))
