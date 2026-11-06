#!/usr/bin/env python3
"""Construit les boss : python construire_boss.py [id...]. Meme squelette et memes animations que les modules d'origine (art/boss_blockbench/<id>.py), memes instants de frappe, memes indices d'animation.
Sorties dans livraison : .bbmodel Modded Entity, PNG et PNG emissive, classes Java <Boss>Model et <Boss>Anims regenerees."""
import importlib
import json
import os
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE / 'modeles'))
from boss_kit import coplanaires, ek, prepare

ROOT = HERE.parent
LIV = ROOT / 'livraison'
RES = LIV / 'src/main/resources/assets/mmorpg'
JAVA = LIV / 'src/main/java/com/mmorpg'
BOSS = {'roi_gobelin': ('RoiGobelin', 'boss_roi'), 'liche_ancienne': ('LicheAncienne', 'boss_liche'), 'ignis': ('Ignis', 'boss_ignis'), 'titan_glace': ('TitanGlace', 'boss_titan'), 'avatar_neant': ('AvatarNeant', 'boss_avatar')}


def build(key):
    t0 = time.time()
    cls, modnom = BOSS[key]
    orig = importlib.import_module(key)
    mod = importlib.import_module(modnom)
    m = mod.build()
    orig.animations(m)
    prepare(m, seed=sum(map(ord, key)), y_corps=getattr(mod, 'Y_CORPS', (0, 60)))
    tex, emi = ek.build_texture(m)
    ek.check(m, tex)
    (LIV / 'bbmodels').mkdir(parents=True, exist_ok=True)
    ek.write_json(LIV / 'bbmodels' / f'{key}.bbmodel', ek.bbmodel(m, tex, orig.ENTITY_CLASS, orig.COLORS, getattr(mod, 'VISIBLE_BOX', orig.VISIBLE_BOX)))
    (RES / 'textures/entity/boss').mkdir(parents=True, exist_ok=True)
    tex.save(RES / f'textures/entity/boss/{key}.png', optimize=True)
    if emi.getbbox():
        emi.save(RES / f'textures/entity/boss/{key}_emissive.png', optimize=True)
    client, common = ek.java_export(m, tex, cls, key, getattr(orig, 'STRIKES', {}), 'art/refonte_3d/outils/construire_boss.py', art_dir='art/refonte_3d/livraison/bbmodels')
    (JAVA / 'client/model/boss').mkdir(parents=True, exist_ok=True)
    (JAVA / 'entity/boss/anim').mkdir(parents=True, exist_ok=True)
    (JAVA / f'client/model/boss/{cls}Model.java').write_text(client, encoding='utf-8', newline='\n')
    (JAVA / f'entity/boss/anim/{cls}Anims.java').write_text(common, encoding='utf-8', newline='\n')
    ap = ROOT / 'apercus/travail/boss'
    ap.mkdir(parents=True, exist_ok=True)
    ek.views_sheet(m, tex, emi, str(ap / f'{key}_vues.png'), orig.TITLE, f'{len(m.order)} os, {len(m.cubes())} cubes, texture {tex.width}x{tex.height}, {len(m.animations)} animations', scale=getattr(mod, 'VIEW_SCALE', getattr(orig, 'VIEW_SCALE', 6.0)))
    print(f'{key}: {len(m.order)} os, {len(m.cubes())} cubes, texture {tex.width}x{tex.height}, {len(m.animations)} animations, {time.time() - t0:.1f}s')
    cop = coplanaires(m)
    if cop:
        print('  COPLANAIRES', len(cop), cop[:8])
    return m, tex, emi


if __name__ == '__main__':
    a = sys.argv[1:] or list(BOSS)
    for k in a:
        build(k)
