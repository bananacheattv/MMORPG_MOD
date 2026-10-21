#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Mannequin d'entrainement d'Eldoria (format Blockbench "Modded Entity"), genere par script.

  python generer_mannequin.py            les 3 variantes, avec apercus et GIF
  python generer_mannequin.py --rapide   sans GIF

Sorties dans <variante>/ : .bbmodel, .png, _emissive.png, _icone.png (icone d'objet), apercu/*.
Ecrit aussi le Java du mod (src/main/java/com/mmorpg/client/model/dummy et entity/dummy/anim).
"""
import os
import sys

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
sys.path.insert(0, os.path.join(ICI, '..'))
import entity_kit as ek  # noqa: E402
import mannequin_entrainement as mod  # noqa: E402
from PIL import Image  # noqa: E402

SRC = os.path.join(ICI, '..', '..', 'src', 'main', 'java', 'com', 'mmorpg')
VARIANTES = [('standard', 'Standard'), ('blinde', 'Blinde'), ('magique', 'Magique')]


def generer(variant, cls, gifs=True):
    key = mod.KEY if variant == 'standard' else f'{mod.KEY}_{variant}'
    m = mod.build(variant)
    mod.animations(m)
    tex, emi = ek.build_texture(m)
    ek.check(m, tex)
    out = os.path.join(ICI, variant)
    ap = os.path.join(out, 'apercu')
    os.makedirs(ap, exist_ok=True)
    ek.write_json(os.path.join(out, f'{key}.bbmodel'), ek.bbmodel(m, tex, mod.ENTITY_CLASS, mod.COLORS, mod.VISIBLE_BOX))
    tex.save(os.path.join(out, f'{key}.png'))
    if emi.getbbox():
        emi.save(os.path.join(out, f'{key}_emissive.png'))
    sub = f'{len(m.order)} os · {len(m.cubes())} cubes · texture {tex.width}x{tex.height} · {len(m.animations)} animations'
    ek.views_sheet(m, tex, emi, os.path.join(ap, f'{key}_vues.png'), f'{mod.TITLE} ({variant})', sub, scale=mod.VIEW_SCALE)
    ek.anim_sheet(m, tex, emi, os.path.join(ap, f'{key}_animations.png'), f'{mod.TITLE} ({variant}) — animations',
                  scale=mod.SHEET_SCALE, yaw=35)
    if gifs:
        for a in m.animations:
            ek.anim_gif(m, tex, emi, a, os.path.join(ap, f'{key}_{a.name}.gif'), scale=mod.GIF_SCALE, yaw=35)
    icon = ek.render(m, tex, emi, yaw=30, pitch=12, scale=4.0)             # icone d'objet 32x32 depuis le rendu
    icon = icon.crop(icon.getbbox())
    side = max(icon.size)
    sq = Image.new('RGBA', (side, side), (0, 0, 0, 0))
    sq.alpha_composite(icon, ((side - icon.width) // 2, side - icon.height))
    sq.resize((32, 32), Image.LANCZOS).save(os.path.join(out, f'{key}_icone.png'))
    if os.path.isdir(SRC):
        client, common = ek.java_export(m, tex, cls, key, {}, 'art/mannequin_blockbench/generer_mannequin.py',
                                        client_pkg='com.mmorpg.client.model.dummy', common_pkg='com.mmorpg.entity.dummy.anim',
                                        art_dir='art/mannequin_blockbench')
        for rel, code in ((('client', 'model', 'dummy'), client), (('entity', 'dummy', 'anim'), common)):
            d = os.path.join(SRC, *rel)
            os.makedirs(d, exist_ok=True)
            fname = cls + ('Model' if rel[0] == 'client' else 'Anims') + '.java'
            with open(os.path.join(d, fname), 'w', encoding='utf-8', newline=chr(10)) as f:
                f.write(code)
    print(f'{key}: {sub}')


if __name__ == '__main__':
    for variant, cls in VARIANTES:
        generer(variant, 'Mannequin' + cls, gifs='--rapide' not in sys.argv)
