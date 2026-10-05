"""Additional resources; also called by the main resource generator."""
from pathlib import Path
import json
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/mmorpg'
ITEMS = {
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
    g.simple_item('epee_berserker', 'Épée du Berserker', g.gi.sword('#d3d9df', '#a04430', '#443033', gem='#ff6030'), 'minecraft:item/handheld')
    g.write_json(str(ROOT / 'items/epee_berserker.json'), {'model': {'type': 'minecraft:model', 'model': 'mmorpg:item/epee_niv_40'}})
    import gen_items
    for key, (_, base, spots) in MOBS.items():
        p = ROOT / 'textures/item' / (key + '_spawn_egg.png')
        p.parent.mkdir(parents=True, exist_ok=True)
        gen_items.spawn_egg(base, spots).save(p)
    for key, (name, texture) in ITEMS.items():
        for directory, obj in [
            ('items', {'model': {'type': 'minecraft:model', 'model': 'mmorpg:item/' + key}}),
            ('models/item', {'parent': 'minecraft:item/generated', 'textures': {'layer0': ('mmorpg:item/' if key.endswith('_spawn_egg') else 'minecraft:item/') + texture}})
        ]:
            p = ROOT / directory / (key + '.json'); p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    for locale in ['fr_fr', 'en_us']:
        p = ROOT / 'lang' / (locale + '.json')
        data = json.loads(p.read_text(encoding='utf-8'))
        data.update(g.LANG)
        data.update({'entity.mmorpg.' + key: value[0] for key, value in MOBS.items()})
        data.update({'item.mmorpg.' + key: value[0] for key, value in ITEMS.items()})
        p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
if __name__ == '__main__': generate()
