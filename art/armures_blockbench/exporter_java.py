#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Export des 20 panoplies (.bbmodel, format Modded Entity) vers le mod : une classe Java ArmorLayers qui construit,
pour chaque panoplie et chaque emplacement (tete, torse, jambes, pieds), un LayerDefinition compatible HumanoidModel.

Usage :  python exporter_java.py      (apres generer_armures.py ; aucune dependance)

- Conversion identique a l'export Java de Blockbench : X et Y inverses, +24 sur les parties racines,
  rotations (-x, -y, z), box UV (texOffs), gonflement (CubeDeformation).
- Chaque groupe principal est rattache a la partie du HumanoidModel situee au meme endroit
  (tete, corps, bras droit / gauche, jambe droite / gauche) : la piece suit l'animation du joueur.
  Le cote est deduit de la position, pas du nom du groupe : le rendu en jeu est celui qu'affiche Blockbench.
- Le groupe 'reference_joueur' (non exporte) est ignore.
- SET_PANOPLY : quelle panoplie habille chacun des 17 sets d'armure du mod (a modifier ici, puis relancer).
"""
import glob
import json
import math
import os

ICI = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(ICI, '..', '..', 'src', 'main', 'java', 'com', 'mmorpg', 'client', 'model', 'armor')

# set du mod -> panoplie (classe, niveau et style du set ; 3 panoplies restent libres : 10, 15, 16)
SET_PANOPLY = {
    # Guerrier (plaques)
    'acier_soldat': 'panoplie_05_niv_020',      # Armure de fer
    'berserker': 'panoplie_06_niv_025',         # Fer renforce
    'seigneur_guerre': 'panoplie_12_niv_055',   # Epaulieres angulaires
    'dieu_guerre': 'panoplie_19_niv_090',       # Armure souveraine
    # Mage (runes, tissu)
    'apprenti': 'panoplie_01_niv_001',          # Cuir simple
    'sorcier': 'panoplie_08_niv_035',           # Acier et tissu sombre
    'archimage': 'panoplie_11_niv_050',         # Armure runique sombre
    'avatar_arcanique': 'panoplie_14_niv_065',  # Armure runique elaboree
    # Archer (cuir, armures legeres)
    'chasseur': 'panoplie_02_niv_005',          # Cuir renforce
    'rodeur': 'panoplie_03_niv_010',            # Cuir cloute
    'tireur_elite': 'panoplie_09_niv_040',      # Acier noir borde d'argent
    'sylvestre': 'panoplie_17_niv_080',         # Acier noir cristallin
    # Tank (armures lourdes)
    'garde': 'panoplie_04_niv_015',             # Mailles et cuir
    'gardien': 'panoplie_07_niv_030',           # Acier poli
    'paladin': 'panoplie_13_niv_060',           # Chevalier cristallin
    'titan': 'panoplie_18_niv_085',             # Armure ancienne massive
}

SLOTS = {'casque': 'head', 'plastron': 'chest', 'jambieres': 'legs', 'bottes': 'feet'}
# parties du HumanoidModel et leur pose vanilla (repere Java)
PARTS = [('head', (0, 0, 0)), ('body', (0, 0, 0)), ('right_arm', (-5, 2, 0)), ('left_arm', (5, 2, 0)),
         ('right_leg', (-1.9, 12, 0)), ('left_leg', (1.9, 12, 0))]


def jf(v):
    v = round(float(v), 4)
    if abs(v) < 1e-9:
        v = 0.0
    s = ('%.4f' % v).rstrip('0').rstrip('.')
    return (s if s not in ('', '-0') else '0') + 'F'


def target_part(group):
    """Partie du HumanoidModel portant ce groupe, d'apres son nom et la position de son pivot (repere Blockbench)."""
    n, x = group['name'], group['origin'][0]
    if 'tete' in n:
        return 'head'
    if 'torse' in n or 'taille' in n:
        return 'body'
    side = 'left' if x < 0 else 'right'         # Blockbench -X = cote gauche de la creature (X inverse a l'export)
    if 'bras' in n:
        return side + '_arm'
    if 'jambe' in n or 'pied' in n:
        return side + '_leg'
    raise ValueError('groupe non reconnu : ' + n)


def box(e, pivot):
    f, t = e['from'], e['to']
    size = [t[i] - f[i] for i in range(3)]
    x = pivot[0] - t[0]
    y = -f[1] - size[1] + pivot[1]
    z = f[2] - pivot[2]
    u, v = e.get('uv_offset', [0, 0])
    s = '.texOffs(%d, %d)' % (u, v)
    mirror = e.get('mirror_uv')
    if mirror:
        s += '.mirror()'
    s += '.addBox(%s, %s, %s, %s, %s, %s, new CubeDeformation(%s))' % (
        jf(x), jf(y), jf(z), jf(size[0]), jf(size[1]), jf(size[2]), jf(e.get('inflate', 0)))
    if mirror:
        s += '.mirror(false)'
    return s


def pose(origin, parent, rot):
    c = [-(origin[0] - parent[0]), -(origin[1] - parent[1]), origin[2] - parent[2]]
    if rot and any(abs(r) > 1e-9 for r in rot):
        rr = [math.radians(-rot[0]), math.radians(-rot[1]), math.radians(rot[2])]
        return 'PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)' % tuple(jf(v) for v in c + rr)
    return 'PartPose.offset(%s, %s, %s)' % tuple(jf(v) for v in c)


def slot_method(name, groups, els, width, height):
    """Corps d'une methode qui construit le maillage d'un emplacement."""
    lines = ['MeshDefinition mesh = new MeshDefinition();', 'PartDefinition root = mesh.getRoot();']
    by_part = {}
    for g in groups:
        by_part.setdefault(target_part(g), []).append(g)
    counter = [0]

    def children(var, group, pivot):
        for ch in group['children']:
            if isinstance(ch, str):
                continue
            if not ch.get('export', True):
                continue
            counter[0] += 1
            v = f'c{counter[0]}'
            cubes = ''.join(box(els[c], ch['origin']) for c in ch['children'] if isinstance(c, str) and els[c].get('export', True))
            lines.append('PartDefinition %s = %s.addOrReplaceChild("%s", CubeListBuilder.create()%s, %s);' % (
                v, var, ch['name'], cubes, pose(ch['origin'], pivot, ch.get('rotation'))))
            children(v, ch, ch['origin'])

    for part, (px, py, pz) in PARTS:
        gs = by_part.get(part, [])
        cubes = ''
        for g in gs:
            bb_pivot = g['origin']
            expected = (-px, 24 - py, pz)                  # pose vanilla exprimee dans le repere Blockbench
            if any(abs(bb_pivot[i] - expected[i]) > 1e-6 for i in range(3)):
                raise ValueError(f'{name} : pivot de {g["name"]} {bb_pivot} different de {part} {expected}')
            cubes += ''.join(box(els[c], bb_pivot) for c in g['children'] if isinstance(c, str) and els[c].get('export', True))
        lines.append('PartDefinition %s = root.addOrReplaceChild("%s", CubeListBuilder.create()%s, PartPose.offset(%s, %s, %s));' % (
            part, part, cubes, jf(px), jf(py), jf(pz)))
        if part == 'head':
            lines.append('head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);')
        for g in gs:
            children(part, g, g['origin'])
    lines.append('return LayerDefinition.create(mesh, %d, %d);' % (width, height))
    return lines


def main():
    methods, cases, keys = [], [], []
    for path in sorted(glob.glob(os.path.join(ICI, 'panoplie_*', 'panoplie_*.bbmodel'))):
        d = json.load(open(path, encoding='utf-8'))
        key = os.path.splitext(os.path.basename(path))[0]
        keys.append(key)
        els = {e['uuid']: e for e in d['elements']}
        w, h = d['resolution']['width'], d['resolution']['height']
        for top in d['outliner']:
            if isinstance(top, str) or top['name'] not in SLOTS or not top.get('export', True):
                continue
            slot = SLOTS[top['name']]
            groups = [g for g in top['children'] if not isinstance(g, str) and g.get('export', True)]
            mname = 'p' + key.split('_')[1] + slot.capitalize()
            body = slot_method(key, groups, els, w, h)
            methods.append('    private static LayerDefinition %s() {\n        %s\n    }\n' % (mname, '\n        '.join(body)))
            cases.append(f'            case "{key}/{slot}" -> {mname}();')
    missing = sorted(set(SET_PANOPLY.values()) - set(keys))
    if missing:
        raise SystemExit('panoplies introuvables : ' + ', '.join(missing))
    entries = ',\n            '.join(f'Map.entry("{s}", "{p}")' for s, p in SET_PANOPLY.items())
    code = f'''// Genere automatiquement par art/armures_blockbench/exporter_java.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.client.model.armor;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.List;
import java.util.Map;

/** Modeles 3D des panoplies (art/armures_blockbench), un LayerDefinition HumanoidModel par panoplie et par emplacement. */
public final class ArmorLayers {{
    /** Set d'armure du mod -> panoplie utilisee. */
    public static final Map<String, String> SET_PANOPLY = Map.ofEntries(
            {entries});
    public static final List<String> PANOPLIES = List.of({', '.join(f'"{k}"' for k in keys)});

    private ArmorLayers() {{
    }}

    public static LayerDefinition create(String panoply, EquipmentSlot slot) {{
        return switch (panoply + "/" + slot.getName()) {{
{chr(10).join(cases)}
            default -> throw new IllegalArgumentException("Panoplie inconnue : " + panoply + "/" + slot.getName());
        }};
    }}

{chr(10).join(methods)}}}
'''
    os.makedirs(SRC, exist_ok=True)
    with open(os.path.join(SRC, 'ArmorLayers.java'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(code)
    print(f'ArmorLayers.java : {len(keys)} panoplies, {len(cases)} emplacements, {len(SET_PANOPLY)} sets associes')


if __name__ == '__main__':
    main()
