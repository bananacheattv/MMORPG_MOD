#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Presse de compression : ressources du bloc (textures 16x16 du cube d'inventaire, blockstate, modele, item, butin, recette, langue)
et copie des textures du modele 3D (art/structures_blockbench/presse_compression).
Le modele 3D lui-meme est genere par art/structures_blockbench/generer_structures.py.
Usage : python tools/gen_presse.py
"""
import json
import os
import random
import shutil
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_mine  # noqa: E402  (merge_lang et conventions de fin de ligne)

MODID = 'mmorpg'
RES = os.path.join(HERE, '..', 'src', 'main', 'resources')
ASSETS = os.path.join(RES, 'assets', MODID)
DATA = os.path.join(RES, 'data')
STRUCT = os.path.join(HERE, '..', 'art', 'structures_blockbench', 'presse_compression')

STONE = [(20, 19, 25), (28, 27, 35), (37, 36, 45), (47, 46, 57)]
SILVER = [(87, 90, 100), (118, 122, 133), (149, 154, 164), (182, 186, 194)]
TEAL = [(20, 112, 106), (37, 184, 168), (110, 240, 216), (210, 255, 244)]


def base(seed):
    r = random.Random(seed)
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), STONE[r.choice((0, 1, 1, 2, 2, 3))] + (255,))
    for y in (0, 8):
        for x in range(16):
            im.putpixel((x, y), STONE[0] + (255,))
    for y in range(16):
        im.putpixel((0 if y < 8 else 8, y), STONE[0] + (255,))
    return im


def frame(im):
    for i in range(16):
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            im.putpixel(p, SILVER[1] + (255,))
    for p in ((0, 0), (15, 0), (0, 15), (15, 15)):
        im.putpixel(p, SILVER[3] + (255,))


def textures():
    out = os.path.join(ASSETS, 'textures', 'block')
    top = base(11)
    frame(top)
    for y in range(4, 12):
        for x in range(4, 12):
            d = max(abs(x - 7.5), abs(y - 7.5))
            top.putpixel((x, y), (SILVER[2] if d > 3 else TEAL[1] if d > 1.5 else TEAL[2]) + (255,))
    front = base(12)
    frame(front)
    for y in range(4, 12):
        for x in range(3, 13):
            edge = y in (4, 11) or x in (3, 12)
            front.putpixel((x, y), (SILVER[2] if edge else (10, 10, 14)) + (255,))
    for x in range(5, 11):
        for y in (6, 7, 8, 9):
            front.putpixel((x, y), TEAL[2 if y in (7, 8) else 1] + (255,))
    side = base(13)
    frame(side)
    for y in range(3, 13):
        side.putpixel((7, y), TEAL[0] + (255,))
        side.putpixel((8, y), TEAL[1] + (255,))
    top.save(os.path.join(out, 'presse_compression_top.png'))
    front.save(os.path.join(out, 'presse_compression_front.png'))
    side.save(os.path.join(out, 'presse_compression_side.png'))
    ent = os.path.join(ASSETS, 'textures', 'entity', 'structure')
    for f in ('presse_compression.png', 'presse_compression_emissive.png'):
        shutil.copyfile(os.path.join(STRUCT, f), os.path.join(ent, f))


def wj(path, obj, eol='\n'):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(json.dumps(obj, ensure_ascii=False, indent=2).replace('\n', eol))


def generate():
    textures()
    key = 'presse_compression'
    variants = {f'facing={d}': dict(model=f'{MODID}:block/{key}', **({'y': y} if y else {}))
                for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}
    wj(os.path.join(ASSETS, 'blockstates', key + '.json'), {'variants': variants})
    wj(os.path.join(ASSETS, 'models', 'block', key + '.json'), {
        'parent': 'minecraft:block/orientable',
        'textures': {'top': f'{MODID}:block/{key}_top', 'front': f'{MODID}:block/{key}_front', 'side': f'{MODID}:block/{key}_side'}})
    wj(os.path.join(ASSETS, 'items', key + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{MODID}:block/{key}'}})
    wj(os.path.join(DATA, MODID, 'loot_table', 'blocks', key + '.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{MODID}:{key}'}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': f'{MODID}:blocks/{key}'})
    wj(os.path.join(DATA, MODID, 'recipe', key + '.json'), {
        'type': 'minecraft:crafting_shaped',
        'pattern': ['SIS', 'PFP', 'BBB'],
        'key': {'S': 'minecraft:stone_bricks', 'I': 'minecraft:iron_block', 'P': 'minecraft:piston', 'F': f'{MODID}:forge_arcanique', 'B': 'minecraft:smooth_stone'},
        'result': {'id': f'{MODID}:{key}'}})
    fr = {f'block.{MODID}.{key}': 'Presse de compression'}
    en = {f'block.{MODID}.{key}': 'Compression Press'}
    gen_mine.merge_lang(os.path.join(ASSETS, 'lang', 'fr_fr.json'), fr)
    gen_mine.merge_lang(os.path.join(ASSETS, 'lang', 'en_us.json'), en)
    print('presse de compression : ressources generees')


if __name__ == '__main__':
    generate()
