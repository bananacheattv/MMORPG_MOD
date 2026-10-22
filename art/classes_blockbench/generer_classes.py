#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Maitres de classe du hub d'Eldoria (Guerrier, Mage, Archer, Tank) et Gardien de l'epreuve : modeles Blockbench "Modded Entity"
generes par script, avec animations idle / present, apercus et GIF, et export Java pour le mod.

  python generer_classes.py            tous les maitres, avec apercus et GIF
  python generer_classes.py --rapide   sans GIF
  python generer_classes.py guerrier   un seul maitre
"""
import importlib
import os
import sys

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
sys.path.insert(0, os.path.join(ICI, '..'))
import entity_kit as ek  # noqa: E402

SRC = os.path.join(ICI, '..', '..', 'src', 'main', 'java', 'com', 'mmorpg')
MAITRES = {'guerrier': 'MaitreGuerrier', 'mage': 'MaitreMage', 'archer': 'MaitreArcher', 'tank': 'MaitreTank', 'epreuve': 'GardienEpreuve'}


def generer(nom, gifs=True):
    mod = importlib.import_module(nom)
    cls = MAITRES[nom]
    m = mod.build()
    mod.animations(m)
    tex, emi = ek.build_texture(m)
    ek.check(m, tex)
    out = os.path.join(ICI, nom)
    ap = os.path.join(out, 'apercu')
    os.makedirs(ap, exist_ok=True)
    key = mod.KEY
    ek.write_json(os.path.join(out, f'{key}.bbmodel'), ek.bbmodel(m, tex, mod.ENTITY_CLASS, mod.COLORS, mod.VISIBLE_BOX))
    tex.save(os.path.join(out, f'{key}.png'))
    if emi.getbbox():
        emi.save(os.path.join(out, f'{key}_emissive.png'))
    sub = f'{len(m.order)} os · {len(m.cubes())} cubes · texture {tex.width}x{tex.height} · {len(m.animations)} animations'
    ek.views_sheet(m, tex, emi, os.path.join(ap, f'{key}_vues.png'), mod.TITLE, sub, scale=11.0)
    ek.anim_sheet(m, tex, emi, os.path.join(ap, f'{key}_animations.png'), f'{mod.TITLE} — animations', scale=8.0, yaw=35)
    if gifs:
        for a in m.animations:
            ek.anim_gif(m, tex, emi, a, os.path.join(ap, f'{key}_{a.name}.gif'), scale=7.0, yaw=35)
    if os.path.isdir(SRC):
        client, common = ek.java_export(m, tex, cls, key, {}, 'art/classes_blockbench/generer_classes.py',
                                        client_pkg='com.mmorpg.client.model.hub', common_pkg='com.mmorpg.entity.hub.anim',
                                        art_dir='art/classes_blockbench')
        for rel, code in ((('client', 'model', 'hub'), client), (('entity', 'hub', 'anim'), common)):
            d = os.path.join(SRC, *rel)
            os.makedirs(d, exist_ok=True)
            fname = cls + ('Model' if rel[0] == 'client' else 'Anims') + '.java'
            with open(os.path.join(d, fname), 'w', encoding='utf-8', newline=chr(10)) as f:
                f.write(code)
    print(f'{key}: {sub}')


if __name__ == '__main__':
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    for nom in args or MAITRES:
        generer(nom, gifs='--rapide' not in sys.argv)
