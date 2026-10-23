#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Cultures d'Aeloria : textures (quatre stades de pousse par plante, graines, recoltes, aliments), blockstates, modeles, definitions
d'objets, tables de butin et langue. La liste CROPS doit rester identique a com.mmorpg.farm.FarmCrops (Java).
Usage : python tools/gen_farm.py
"""
import json
import os
import random
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_mine  # noqa: E402  (merge_lang)

MODID = 'mmorpg'
RES = os.path.join(HERE, '..', 'src', 'main', 'resources')
ASSETS = os.path.join(RES, 'assets', MODID)
DATA = os.path.join(RES, 'data')

# id, nom, nom en, graine, graine en, recolte, recolte en, couleur principale, couleur sombre, couleur claire, feuillage
CROPS = [
    ('lueur', 'Racine de Lueur', 'Gleamroot', 'Graine de Lueur', 'Gleamroot Seed', 'racine_lueur', (80, 230, 170), (30, 120, 90), (200, 255, 235), (40, 130, 80)),
    ('argent', "Blé d'Argent", 'Silverwheat', "Graine d'Argent", 'Silverwheat Seed', 'ble_argent', (205, 214, 232), (110, 120, 150), (255, 255, 255), (110, 150, 70)),
    ('ambre', 'Baie Ambrée', 'Amberberry', 'Graine Ambrée', 'Amberberry Seed', 'baie_ambree', (255, 150, 50), (160, 70, 20), (255, 225, 140), (50, 120, 50)),
    ('givre', 'Fleur de Givre', 'Frostbloom', 'Graine de Givre', 'Frostbloom Seed', 'fleur_givre', (110, 220, 255), (40, 120, 190), (235, 252, 255), (60, 140, 130)),
    ('neant', 'Orchidée du Néant', 'Void Orchid', 'Graine du Néant', 'Void Orchid Seed', 'orchidee_neant', (170, 100, 255), (90, 40, 170), (240, 210, 255), (70, 50, 110)),
]
FOODS = [
    ('pain_argent', 'Pain d\'argent', 'Silver Bread', (205, 214, 232), (170, 120, 70)),
    ('confiture_ambree', 'Confiture ambrée', 'Amber Jam', (255, 150, 50), (160, 70, 20)),
    ('elixir_givre', 'Élixir de givre', 'Frost Elixir', (110, 220, 255), (40, 120, 190)),
    ('nectar_neant', 'Nectar du Néant', 'Void Nectar', (170, 100, 255), (90, 40, 170)),
]
PRODUCT_NAMES = {c[5]: (c[1], c[2]) for c in CROPS}


def img():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def px(im, x, y, c, a=255):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), tuple(c) + (a,))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def dots(im, pts, c):
    for x, y in pts:
        px(im, x, y, c)


def crop_stage(ci, stage):
    cid, _, _, _, _, _, main, dark, light, leaf = CROPS[ci]
    im = img()
    r = random.Random(ci * 31 + stage)
    heights = [4, 8, 11, 14]
    for sx in (2, 6, 10, 14):
        h = heights[stage] + r.randint(-1, 1) * (stage > 0)
        h = max(3, min(15, h))
        for k in range(h):
            y = 15 - k
            px(im, sx, y, shade(leaf, 0.75 + 0.25 * (k % 2)))
        # feuilles
        for k in range(2, h, 3):
            y = 15 - k
            d = 1 if (k // 3 + sx) % 2 else -1
            px(im, sx + d, y, leaf)
            if stage >= 2:
                px(im, sx + 2 * d, y + 1, shade(leaf, 1.15))
        top = 15 - h
        if stage == 3:
            if cid == 'lueur':                                   # bulbes lumineux au pied et lueur au sommet
                for dx in (-1, 0, 1):
                    px(im, sx + dx, 14, main)
                    px(im, sx + dx, 13, shade(main, 0.8))
                px(im, sx, 15, light)
                px(im, sx, top, light)
                px(im, sx, top - 1 if top > 0 else top, main)
            elif cid == 'argent':                                # epi argente
                for k in range(5):
                    px(im, sx, top + k, main if k % 2 == 0 else shade(main, 0.8))
                    px(im, sx - 1 + (k % 2) * 2, top + k + 1, light)
                px(im, sx, top - 1 if top > 0 else top, light)
            elif cid == 'ambre':                                 # grappes de baies
                for k in range(0, 8, 3):
                    px(im, sx - 1, top + k + 1, main)
                    px(im, sx + 1, top + k + 2, main)
                    px(im, sx - 1, top + k + 2, dark)
                    px(im, sx + 1, top + k + 1, light)
            elif cid == 'givre':                                 # fleur a cinq petales
                cy = max(1, top)
                dots(im, [(sx - 1, cy), (sx + 1, cy), (sx, cy - 1), (sx, cy + 1)], main)
                px(im, sx, cy, light)
                dots(im, [(sx - 1, cy - 1), (sx + 1, cy - 1), (sx - 1, cy + 1), (sx + 1, cy + 1)], shade(main, 0.75))
            else:                                                # orchidee : petales courbes et coeur lumineux
                cy = max(2, top + 1)
                dots(im, [(sx - 2, cy - 1), (sx + 2, cy - 1), (sx - 1, cy), (sx + 1, cy), (sx, cy + 1), (sx - 2, cy), (sx + 2, cy)], main)
                dots(im, [(sx - 1, cy - 1), (sx + 1, cy - 1), (sx, cy - 2)], light)
                px(im, sx, cy, dark)
        elif stage == 2:
            col = main if cid != 'argent' else shade(main, 0.9)
            px(im, sx, top, col)
            px(im, sx + (1 if sx % 8 == 2 else -1), top + 1, shade(col, 0.8))
    return im


def seed_icon(ci):
    _, _, _, _, _, _, main, dark, light, leaf = CROPS[ci]
    im = img()
    # petit sachet de toile noue au col, graines dans la couleur de la plante
    cloth, cloth_d = (176, 142, 96), (122, 92, 58)
    d = ImageDraw.Draw(im)
    d.ellipse((3, 6, 12, 14), fill=cloth + (255,), outline=cloth_d + (255,))
    d.rectangle((6, 4, 9, 6), fill=cloth_d + (255,))
    px(im, 5, 3, cloth)
    px(im, 10, 3, cloth)
    px(im, 7, 3, cloth_d)
    px(im, 8, 3, cloth_d)
    for x, y in ((5, 9), (7, 8), (9, 10), (6, 11), (8, 12), (10, 8)):
        px(im, x, y, main)
        px(im, x + 1, y, dark)
    px(im, 7, 7, light)
    return im


def product_icon(ci):
    cid, _, _, _, _, _, main, dark, light, leaf = CROPS[ci]
    im = img()
    d = ImageDraw.Draw(im)
    if cid == 'lueur':                                            # racine renflee qui luit
        d.ellipse((4, 5, 11, 13), fill=main + (255,), outline=dark + (255,))
        for x, y in ((6, 7), (7, 6)):
            px(im, x, y, light)
        d.line((7, 5, 6, 2), fill=leaf + (255,))
        d.line((8, 5, 10, 2), fill=leaf + (255,))
        px(im, 7, 14, dark)
        px(im, 8, 15, dark)
    elif cid == 'argent':                                         # gerbe de trois epis
        for ox, tilt in ((4, -1), (7, 0), (10, 1)):
            for k in range(8):
                px(im, ox + (k * tilt) // 4, 13 - k, main if k % 2 == 0 else shade(main, 0.82))
                if k > 3:
                    px(im, ox + (k * tilt) // 4 - 1, 13 - k, light)
        d.line((4, 14, 11, 14), fill=(176, 142, 96, 255))
        d.line((5, 15, 10, 15), fill=(122, 92, 58, 255))
    elif cid == 'ambre':                                          # grappe de baies
        for x, y in ((5, 8), (9, 8), (7, 11)):
            d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=main + (255,), outline=dark + (255,))
            px(im, x - 1, y - 1, light)
        d.line((7, 6, 7, 3), fill=leaf + (255,))
        d.polygon([(8, 4), (12, 3), (9, 6)], fill=leaf + (255,))
    elif cid == 'givre':                                          # fleur de glace
        d.line((7, 15, 7, 8), fill=leaf + (255,))
        for x, y in ((7, 3), (4, 5), (10, 5), (5, 8), (9, 8)):
            d.ellipse((x - 2, y - 2, x + 1, y + 1), fill=main + (255,), outline=dark + (255,))
        px(im, 7, 6, light)
        px(im, 6, 6, light)
        px(im, 7, 5, light)
    else:                                                         # orchidee du neant
        d.line((7, 15, 7, 9), fill=leaf + (255,))
        d.polygon([(7, 10), (1, 5), (4, 9)], fill=main + (255,), outline=dark + (255,))
        d.polygon([(8, 10), (14, 5), (11, 9)], fill=main + (255,), outline=dark + (255,))
        d.polygon([(7, 9), (4, 2), (11, 2), (8, 9)], fill=shade(main, 1.1) + (255,), outline=dark + (255,))
        d.ellipse((6, 5, 8, 7), fill=light + (255,))
    return im


def food_icon(fid, main, dark):
    im = img()
    d = ImageDraw.Draw(im)
    if fid == 'pain_argent':
        crust, crumb = (170, 120, 70), (232, 200, 150)
        d.ellipse((2, 6, 13, 13), fill=crust + (255,), outline=shade(crust, 0.6) + (255,))
        d.line((5, 7, 6, 10), fill=crumb + (255,))
        d.line((8, 7, 9, 10), fill=crumb + (255,))
        for x, y in ((4, 5), (7, 4), (10, 5)):
            px(im, x, y, main)
    elif fid == 'confiture_ambree':
        d.rectangle((4, 6, 11, 14), fill=(60, 40, 30, 255), outline=(30, 20, 15, 255))
        d.rectangle((5, 8, 10, 13), fill=main + (255,))
        d.rectangle((4, 4, 11, 6), fill=(190, 150, 90, 255), outline=(110, 80, 40, 255))
        px(im, 6, 9, (255, 225, 140))
        px(im, 6, 10, (255, 225, 140))
    elif fid == 'elixir_givre':
        d.rectangle((6, 2, 9, 5), fill=(190, 150, 90, 255), outline=(110, 80, 40, 255))
        d.polygon([(6, 5), (9, 5), (12, 9), (12, 13), (3, 13), (3, 9)], fill=(200, 230, 245, 120), outline=(90, 150, 200, 255))
        d.polygon([(4, 9), (11, 9), (11, 12), (4, 12)], fill=main + (255,))
        px(im, 5, 10, (235, 252, 255))
        px(im, 9, 7, (235, 252, 255))
    else:
        d.rectangle((6, 1, 9, 4), fill=(110, 60, 190, 255), outline=(60, 30, 110, 255))
        d.polygon([(6, 4), (9, 4), (13, 8), (13, 13), (2, 13), (2, 8)], fill=(210, 190, 245, 120), outline=(90, 40, 170, 255))
        d.polygon([(3, 8), (12, 8), (12, 12), (3, 12)], fill=main + (255,))
        px(im, 5, 9, (240, 210, 255))
        px(im, 10, 6, (240, 210, 255))
        px(im, 8, 10, (255, 255, 255))
    return im


def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(json.dumps(obj, ensure_ascii=False, indent=2))


def item_files(iid, tex_img):
    tex_img.save(os.path.join(ASSETS, 'textures', 'item', iid + '.png'))
    wj(os.path.join(ASSETS, 'models', 'item', iid + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MODID}:item/{iid}'}})
    wj(os.path.join(ASSETS, 'items', iid + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{MODID}:item/{iid}'}})


def generate():
    fr, en = {}, {}
    sheet = Image.new('RGBA', (16 * 14, 16 * 5), (40, 40, 52, 255))
    for ci, (cid, nfr, nen, sfr, sen, prod, main, dark, light, leaf) in enumerate(CROPS):
        block = f'culture_{cid}'
        for st in range(4):
            tex = crop_stage(ci, st)
            tex.save(os.path.join(ASSETS, 'textures', 'block', f'{block}_s{st}.png'))
            wj(os.path.join(ASSETS, 'models', 'block', f'{block}_s{st}.json'), {'parent': 'minecraft:block/crop', 'render_type': 'minecraft:cutout',
                                                                              'textures': {'crop': f'{MODID}:block/{block}_s{st}'}})
            sheet.paste(tex, (st * 16, ci * 16), tex)
        stage_of = [0, 0, 1, 1, 2, 2, 2, 3]
        wj(os.path.join(ASSETS, 'blockstates', block + '.json'),
           {'variants': {f'age={a}': {'model': f'{MODID}:block/{block}_s{stage_of[a]}'} for a in range(8)}})
        seed = seed_icon(ci)
        item_files('graine_' + cid, seed)
        sheet.paste(seed, (4 * 16, ci * 16), seed)
        pr = product_icon(ci)
        item_files(prod, pr)
        sheet.paste(pr, (5 * 16, ci * 16), pr)
        fr[f'block.{MODID}.{block}'] = nfr
        en[f'block.{MODID}.{block}'] = nen
        fr[f'item.{MODID}.graine_{cid}'] = sfr
        en[f'item.{MODID}.graine_{cid}'] = sen
        fr[f'item.{MODID}.{prod}'] = nfr
        en[f'item.{MODID}.{prod}'] = nen
        # butin : plante mure = recolte + 1 a 3 graines ; sinon une graine
        mature = {'type': 'minecraft:match_block', 'blocks': f'{MODID}:{block}', 'state': {'age': '7'}}
        wj(os.path.join(DATA, MODID, 'loot_table', 'blocks', block + '.json'), {
            'type': 'minecraft:block',
            'modifier': {'type': 'minecraft:explosion_decay'},
            'pools': [
                {'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
                    {'type': 'minecraft:item', 'condition': mature, 'name': f'{MODID}:{prod}'},
                    {'type': 'minecraft:item', 'name': f'{MODID}:graine_{cid}'}]}]},
                {'rolls': 1, 'condition': mature, 'entries': [{'type': 'minecraft:item', 'name': f'{MODID}:graine_{cid}',
                                                                'modifier': {'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}}}]},
            ],
            'random_sequence': f'{MODID}:blocks/{block}'})
    for i, (fid, nfr, nen, main, dark) in enumerate(FOODS):
        ic = food_icon(fid, main, dark)
        item_files(fid, ic)
        sheet.paste(ic, ((6 + i) * 16, 0), ic)
        fr[f'item.{MODID}.{fid}'] = nfr
        en[f'item.{MODID}.{fid}'] = nen
    gen_mine.merge_lang(os.path.join(ASSETS, 'lang', 'fr_fr.json'), fr)
    gen_mine.merge_lang(os.path.join(ASSETS, 'lang', 'en_us.json'), en)
    sheet.resize((sheet.width * 5, sheet.height * 5), Image.NEAREST).save(os.path.join(HERE, 'apercu_cultures.png'))
    print('cultures : ressources generees')


if __name__ == '__main__':
    generate()
