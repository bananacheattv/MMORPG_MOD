"""Additional resources; also called by the main resource generator."""
from pathlib import Path
import json
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/mmorpg'
ITEMS = {
    'cle_aventure': ('Clé d’aventure', 'tripwire_hook'),
    'caisse_aventure': ('Caisse d’aventure', 'chest'),
    'alliage_celeste': ('Alliage Céleste', 'netherite_ingot'),
    'charme_experience': ("Charme d'expérience", 'experience_bottle'),
    'charme_chance': ('Charme de chance', 'emerald'),
    'nourriture_familier': ('Nourriture pour familier', 'golden_carrot'),
}
MOBS = {
    'zombie_des_cryptes': ('Zombie des Cryptes', '#345530', '#809040'),
    'araignee_venimeuse': ('Araignée Venimeuse', '#192638', '#70e868'),
    'bandit_arbaletrier': ('Bandit Arbalétrier', '#705443', '#a0a0a0'),
}
for key, (name, _, _) in MOBS.items():
    ITEMS[key + '_spawn_egg'] = ("Œuf d'apparition : " + name, key + '_spawn_egg')

def generate():
    import generate_assets as g
    g.ARMOR_SETS = [
        ('valkyrie', 'de la Valkyrie Céleste', 'plate', '#eee8d6', '#d7a530', '#ef5350'),
        ('eternel_arcanes', 'de l’Éternel des Arcanes', 'robe', '#352060', '#d4c0ff', '#50e8ff'),
        ('sentinelle_astrale', 'de la Sentinelle Astrale', 'leather', '#235a50', '#d4ca7a', '#8affbb'),
        ('egide_divine', 'de l’Égide Divine', 'plate', '#213451', '#f0c75b', '#9bcaff'),
    ]
    g.armors()
    from PIL import Image, ImageDraw
    texture = Image.new('RGBA', (16, 16), '#bd790e')
    draw = ImageDraw.Draw(texture)
    draw.rectangle((1, 1, 14, 14), fill='#efbf38', outline='#ffe38a')
    draw.rectangle((3, 3, 12, 12), fill='#d99c18')
    for box in [(5,4,9,5),(9,5,10,7),(7,7,9,8),(7,8,8,9),(7,11,8,12)]: draw.rectangle(box, fill='#fff7d0')
    texture.save(ROOT / 'textures/block/lucky_block.png')
    g.write_json(str(ROOT / 'blockstates/lucky_block.json'), {'variants': {'': {'model': 'mmorpg:block/lucky_block'}}})
    g.write_json(str(ROOT / 'models/block/lucky_block.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'mmorpg:block/lucky_block'}})
    g.write_json(str(ROOT / 'items/lucky_block.json'), {'model': {'type': 'minecraft:model', 'model': 'mmorpg:block/lucky_block'}})
    g.LANG['block.mmorpg.lucky_block'] = 'Lucky Block'
    import gen_items
    for key, (_, base, spots) in MOBS.items():
        p = ROOT / 'textures/item' / (key + '_spawn_egg.png')
        p.parent.mkdir(parents=True, exist_ok=True)
        gen_items.spawn_egg(base, spots).save(p)
    for key, (name, texture) in ITEMS.items():
        for directory, obj in [
            ('items', {'model': {'type': 'minecraft:model', 'model': 'mmorpg:item/' + key}}),
            ('models/item', {'parent': 'minecraft:item/generated', 'textures': {'layer0': ('mmorpg:item/' if key.endswith('_spawn_egg') else 'minecraft:block/' if texture == 'chest' else 'minecraft:item/') + ('oak_planks' if texture == 'chest' else texture)}})
        ]:
            p = ROOT / directory / (key + '.json'); p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    key = Image.new('RGBA', (16, 16))
    ink = ImageDraw.Draw(key)
    ink.ellipse((2, 2, 9, 9), fill='#d9a332', outline='#fff0a0')
    ink.ellipse((4, 4, 7, 7), fill=(0, 0, 0, 0))
    ink.line((8, 8, 13, 13), fill='#ffe386', width=3)
    ink.rectangle((10, 12, 11, 14), fill='#d9a332')
    key.save(ROOT / 'textures/item/cle_aventure.png')
    g.write_json(str(ROOT / 'models/item/cle_aventure.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mmorpg:item/cle_aventure'}})
    g.write_json(str(ROOT / 'models/item/caisse_aventure.json'), {'parent': 'minecraft:block/barrel'})
    for locale in ['fr_fr', 'en_us']:
        p = ROOT / 'lang' / (locale + '.json')
        data = json.loads(p.read_text(encoding='utf-8'))
        data.update(g.LANG)
        data.update({'entity.mmorpg.' + key: value[0] for key, value in MOBS.items()})
        data.update({'item.mmorpg.' + key: value[0] for key, value in ITEMS.items()})
        p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
if __name__ == '__main__': generate()
