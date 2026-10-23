# -*- coding: utf-8 -*-
"""Minerais et lingots des Mines d'Aeloria : textures peintes par code, modeles, etats, objets, traductions.

Cinq minerais, un par etage de la mine (du plus facile au plus profond) :
    Verdite, Argentite, Ambrasite, Glacium, Neantite.
Chaque minerai est un bloc pose dans la mine (il ne se detruit pas : voir MineManager) qui donne directement un lingot, plus un bloc
« veine epuisee » commun qui prend sa place pendant la repousse.

La liste ORES doit rester identique a celle de com.mmorpg.mine.MineOres (Java).
Appele par tools/gen_hub.py ; lancer ce fichier seul met a jour les ressources et les fichiers de langue sans rien regenerer d'autre.
"""
import json
import os
import random

from PIL import Image

MODID = 'mmorpg'
S = 16

# id, nom fr, nom en, roche (clair, sombre), cristal (clair, moyen, sombre), eclat (point lumineux)
ORES = [
    ('verdite', 'Verdite', 'Verdite', ((112, 124, 108), (70, 82, 70)), ((150, 245, 140), (70, 200, 90), (28, 120, 56)), (225, 255, 210)),
    ('argentite', 'Argentite', 'Argentite', ((84, 88, 104), (50, 54, 68)), ((235, 242, 255), (165, 185, 225), (96, 116, 168)), (255, 255, 255)),
    ('ambrasite', 'Ambrasite', 'Ambrasite', ((70, 58, 62), (34, 28, 32)), ((255, 205, 90), (255, 130, 40), (176, 70, 20)), (255, 240, 170)),
    ('glacium', 'Glacium', 'Glacium', ((96, 118, 138), (58, 78, 100)), ((200, 255, 255), (96, 215, 245), (40, 140, 210)), (240, 255, 255)),
    ('neantite', 'Néantite', 'Voidite', ((46, 34, 64), (22, 14, 36)), ((226, 170, 255), (160, 90, 245), (92, 40, 180)), (250, 220, 255)),
]

LEVELS = [1, 18, 38, 55, 75]

# lingots rares (obtenus au hasard en minant, necessaires aux armures) : id, nom fr, nom en, (clair, moyen, sombre), eclat
TOOL_TIERS = [('debutant', 'du débutant', "Beginner's"), ('verdite', 'de Verdite', 'Verdite'), ('argentite', "d'Argentite", 'Argentite'), ('ambrasite', "d'Ambrasite", 'Ambrasite'), ('glacium', 'de Glacium', 'Glacium'), ('neantite', 'de Néantite', 'Neantite')]

RARES = [
    ('aelium', 'Aelium', 'Aelium', ((170, 255, 240), (60, 215, 200), (20, 120, 130)), (240, 255, 255)),
    ('orichalque', 'Orichalque', 'Orichalcum', ((255, 190, 120), (240, 90, 60), (140, 30, 50)), (255, 235, 190)),
    ('astralium', 'Astralium', 'Astralium', ((255, 250, 215), (255, 214, 96), (214, 140, 50)), (255, 255, 255)),
]


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def add(c, d):
    return tuple(max(0, min(255, c[i] + d)) for i in range(3))


def rock(rng, light, dark, deplete=False):
    img = Image.new('RGBA', (S, S))
    for y in range(S):
        for x in range(S):
            t = rng.random() * 0.8 + 0.1
            c = mix(dark, light, t)
            if (x + y) % 5 == 0 and rng.random() < 0.5:
                c = add(c, -10)
            img.putpixel((x, y), c + (255,))
    # fissures sombres
    for _ in range(3 if not deplete else 6):
        x, y = rng.randrange(S), rng.randrange(S)
        for _ in range(rng.randint(3, 6)):
            img.putpixel((x % S, y % S), add(dark, -22) + (255,))
            x += rng.choice((-1, 0, 1))
            y += rng.choice((0, 1))
    return img


def ore_texture(index):
    oid, _, _, (light, dark), (c1, c2, c3), spark = ORES[index]
    rng = random.Random(1000 + index * 77)
    img = rock(rng, light, dark)
    # amas de cristaux : quelques losanges de 2 a 4 pixels, plus clairs au centre
    for _ in range(5):
        cx, cy = rng.randint(2, S - 3), rng.randint(2, S - 3)
        r = rng.choice((1, 1, 2))
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if abs(dx) + abs(dy) > r:
                    continue
                shade = c1 if (dx + dy) <= 0 and abs(dx) + abs(dy) <= 1 else c2 if dx + dy <= 0 else c3
                img.putpixel(((cx + dx) % S, (cy + dy) % S), shade + (255,))
        img.putpixel((cx % S, (cy - r) % S), c1 + (255,))
    # eclats lumineux
    for _ in range(4):
        img.putpixel((rng.randrange(S), rng.randrange(S)), spark + (255,))
    # biseau leger
    for i in range(S):
        for (x, y, d) in ((i, 0, 14), (0, i, 10), (i, S - 1, -18), (S - 1, i, -14)):
            img.putpixel((x, y), add(img.getpixel((x, y))[:3], d) + (255,))
    return img


def depleted_texture():
    rng = random.Random(4242)
    img = rock(rng, (62, 60, 66), (30, 29, 34), deplete=True)
    # petites braises eteintes : points bruns epars
    for _ in range(7):
        img.putpixel((rng.randrange(S), rng.randrange(S)), (86, 60, 52, 255))
    for i in range(S):
        for (x, y, d) in ((i, 0, 8), (0, i, 6), (i, S - 1, -14), (S - 1, i, -10)):
            img.putpixel((x, y), add(img.getpixel((x, y))[:3], d) + (255,))
    return img


def ingot_texture(index):
    oid, _, _, _, (c1, c2, c3), spark = ORES[index]
    return ingot_texture_pal(c1, c2, c3, spark)


def ingot_texture_pal(c1, c2, c3, spark):
    img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    outline = add(c3, -70)
    # lingot vu en perspective : face du dessus (clair), face avant (moyen), flanc droit (sombre)
    top = [(5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (11, 4),
           (4, 5), (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (10, 5),
           (3, 6), (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6)]
    front = [(x, y) for y in (7, 8, 9, 10) for x in range(2, 10)]
    side = [(10, 7), (10, 8), (9, 6), (10, 5), (11, 5), (11, 6), (11, 7), (10, 9), (9, 10)]
    for x, y in top:
        img.putpixel((x, y), c1 + (255,))
    for x, y in front:
        img.putpixel((x, y), c2 + (255,))
    for x, y in side:
        img.putpixel((x, y), c3 + (255,))
    for x in range(2, 10):
        img.putpixel((x, 7), mix(c1, c2, 0.5) + (255,))
    # contour
    px = {(x, y) for x, y in top + front + side}
    for (x, y) in list(px):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y + dy)
            if n not in px and 0 <= n[0] < S and 0 <= n[1] < S:
                img.putpixel(n, outline + (255,))
    img.putpixel((4, 6), spark + (255,))
    img.putpixel((6, 8), c1 + (255,))
    img.putpixel((12, 3), spark + (255,))
    img.putpixel((13, 2), c1 + (255,))
    return img


def ingot_from(c1, c2, c3, spark, stars=0):
    """Lingot (meme gabarit que ingot_texture) a partir d'une palette ; `stars` ajoute des etincelles pour les metaux rares."""
    img = ingot_texture_pal(c1, c2, c3, spark)
    rng = random.Random(sum(c1) + stars)
    for _ in range(stars):
        x, y = rng.choice(((1, 3), (13, 5), (14, 9), (2, 11), (12, 12), (6, 2), (9, 12)))
        img.putpixel((x, y), spark + (255,))
        if rng.random() < 0.6:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < S and 0 <= y + dy < S and img.getpixel((x + dx, y + dy))[3] == 0:
                    img.putpixel((x + dx, y + dy), c1 + (150,))
    return img


def compressed_texture(index):
    """Minerai compresse : plaque dense a cadre sombre, coeur lumineux et quatre rivets."""
    oid, _, _, _, (c1, c2, c3), spark = ORES[index]
    img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    outline = add(c3, -80)
    for y in range(2, 14):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (2, 13)
            inner = x in (3, 12) or y in (3, 12)
            col = outline if edge else mix(c3, c2, 0.35) if inner else mix(c2, c1, 0.5 * (1 - (abs(x - 7.5) + abs(y - 7.5)) / 11))
            img.putpixel((x, y), col + (255,))
    for (x, y) in ((4, 4), (11, 4), (4, 11), (11, 11)):
        img.putpixel((x, y), c1 + (255,))
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        img.putpixel((7 + dx, 7 + dy), spark + (255,))
    for (x, y) in ((6, 7), (9, 7), (7, 6), (7, 9), (8, 9), (9, 8)):
        img.putpixel((x, y), mix(c1, spark, 0.5) + (255,))
    return img


def write_json(p, obj):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def _eol(path):
    with open(path, 'rb') as f:
        return '\r\n' if b'\r\n' in f.read(4096) else '\n'


def merge_lang(path, additions):
    eol = _eol(path)
    with open(path, encoding='utf-8') as f:
        cur = json.load(f)
    cur.update(additions)
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(json.dumps(cur, ensure_ascii=False, indent=2).replace('\n', eol))


def generate(ga=None, root=None):
    here = os.path.dirname(os.path.abspath(__file__))
    root = root or os.path.join(here, '..', 'src', 'main', 'resources')
    assets = os.path.join(root, 'assets', MODID)
    data = os.path.join(root, 'data')
    fr, en = {}, {}
    mineable = []
    sheet = Image.new('RGBA', (S * 8, S * 3), (24, 24, 32, 255))
    for i, (oid, nfr, nen, _, _, _) in enumerate(ORES):
        block_id = 'minerai_' + oid
        ingot_id = 'lingot_' + oid
        tex = ore_texture(i)
        ing = ingot_texture(i)
        os.makedirs(os.path.join(assets, 'textures', 'block'), exist_ok=True)
        os.makedirs(os.path.join(assets, 'textures', 'item'), exist_ok=True)
        tex.save(os.path.join(assets, 'textures', 'block', block_id + '.png'))
        ing.save(os.path.join(assets, 'textures', 'item', ingot_id + '.png'))
        sheet.paste(tex, (i * S, 0))
        sheet.paste(ing, (i * S, S), ing)
        write_json(os.path.join(assets, 'models', 'block', block_id + '.json'),
                   {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{MODID}:block/{block_id}'}})
        write_json(os.path.join(assets, 'blockstates', block_id + '.json'), {'variants': {'': {'model': f'{MODID}:block/{block_id}'}}})
        write_json(os.path.join(assets, 'items', block_id + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{MODID}:block/{block_id}'}})
        write_json(os.path.join(assets, 'models', 'item', ingot_id + '.json'),
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MODID}:item/{ingot_id}'}})
        write_json(os.path.join(assets, 'items', ingot_id + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{MODID}:item/{ingot_id}'}})
        fr[f'block.{MODID}.{block_id}'] = f'Minerai de {nfr}'
        en[f'block.{MODID}.{block_id}'] = f'{nen} Ore'
        fr[f'item.{MODID}.{ingot_id}'] = f'Lingot de {nfr}'
        en[f'item.{MODID}.{ingot_id}'] = f'{nen} Ingot'
        mineable.append(f'{MODID}:{block_id}')
    # lingots rares et minerais compresses
    for i, (rid, nfr, nen, (c1, c2, c3), spark) in enumerate(RARES):
        ing = ingot_from(c1, c2, c3, spark, stars=i * 3)
        iid = 'lingot_' + rid
        ing.save(os.path.join(assets, 'textures', 'item', iid + '.png'))
        write_json(os.path.join(assets, 'models', 'item', iid + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MODID}:item/{iid}'}})
        write_json(os.path.join(assets, 'items', iid + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{MODID}:item/{iid}'}})
        fr[f'item.{MODID}.{iid}'] = "Lingot d'" + nfr
        en[f'item.{MODID}.{iid}'] = f'{nen} Ingot'
        sheet.paste(ing, (i * S, 2 * S), ing)
    for tid, tfr, ten in TOOL_TIERS:
        for kind, kfr, ken in (('pioche', 'Pioche', 'Pickaxe'), ('hache', 'Hache', 'Axe'), ('pelle', 'Pelle', 'Shovel'), ('houe', 'Houe', 'Hoe')):
            if tid == 'debutant' and kind in ('hache', 'pelle'):
                continue
            fr[f'item.{MODID}.{kind}_{tid}'] = f'{kfr} {tfr}'
            en[f'item.{MODID}.{kind}_{tid}'] = f'{ten} {ken}'
    for i, (oid, nfr, nen, _, _, _) in enumerate(ORES):
        cid = 'compresse_' + oid
        tex_c = compressed_texture(i)
        tex_c.save(os.path.join(assets, 'textures', 'item', cid + '.png'))
        write_json(os.path.join(assets, 'models', 'item', cid + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MODID}:item/{cid}'}})
        write_json(os.path.join(assets, 'items', cid + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{MODID}:item/{cid}'}})
        fr[f'item.{MODID}.{cid}'] = f'{nfr} compressée'
        en[f'item.{MODID}.{cid}'] = f'Compressed {nen}'
        sheet.paste(tex_c, ((3 + i) * S, 2 * S), tex_c)
    dep = depleted_texture()
    dep.save(os.path.join(assets, 'textures', 'block', 'veine_epuisee.png'))
    sheet.paste(dep, (5 * S, 0))
    write_json(os.path.join(assets, 'models', 'block', 'veine_epuisee.json'),
               {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{MODID}:block/veine_epuisee'}})
    write_json(os.path.join(assets, 'blockstates', 'veine_epuisee.json'), {'variants': {'': {'model': f'{MODID}:block/veine_epuisee'}}})
    write_json(os.path.join(assets, 'items', 'veine_epuisee.json'), {'model': {'type': 'minecraft:model', 'model': f'{MODID}:block/veine_epuisee'}})
    fr[f'block.{MODID}.veine_epuisee'] = 'Veine épuisée'
    en[f'block.{MODID}.veine_epuisee'] = 'Depleted Vein'
    fr[f'item.{MODID}.pioche_mineur_info'] = 'Il faut une pioche pour extraire ce minerai.'
    # le bloc de la mine ne donne rien quand on le casse a la main d'un operateur : table de butin vide
    for bid in [f'minerai_{o[0]}' for o in ORES] + ['veine_epuisee']:
        write_json(os.path.join(data, MODID, 'loot_table', 'blocks', bid + '.json'),
                   {'type': 'minecraft:block', 'pools': [], 'random_sequence': f'{MODID}:blocks/{bid}'})
    tag = os.path.join(data, 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json')
    eol = _eol(tag)
    with open(tag, encoding='utf-8') as f:
        cur = json.load(f)
    cur['values'] = cur['values'] + [m for m in mineable + [f'{MODID}:veine_epuisee'] if m not in cur['values']]
    with open(tag, 'w', encoding='utf-8', newline='') as f:
        f.write(json.dumps(cur, ensure_ascii=False, indent=2).replace('\n', eol))
    if ga is not None:
        ga.LANG.update(fr)
        ga.LANG_EN.update(en)
    else:
        merge_lang(os.path.join(assets, 'lang', 'fr_fr.json'), fr)
        merge_lang(os.path.join(assets, 'lang', 'en_us.json'), en)
    sheet.resize((sheet.width * 4, sheet.height * 4), Image.NEAREST).save(os.path.join(here, 'apercu_mine.png'))


if __name__ == '__main__':
    generate()
    print('minerais, lingots et veine epuisee generes')
