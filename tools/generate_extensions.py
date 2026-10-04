"""Additional resources; also called by the main resource generator."""
from pathlib import Path
import json
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/mmorpg'
ITEMS = {
    'charme_experience': ("Charme d'expérience", 'experience_bottle'),
    'charme_chance': ('Charme de chance', 'emerald'),
    'nourriture_familier': ('Nourriture pour familier', 'golden_carrot'),
}
def generate():
    for key, (name, texture) in ITEMS.items():
        for directory, obj in [
            ('items', {'model': {'type': 'minecraft:model', 'model': 'mmorpg:item/' + key}}),
            ('models/item', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'minecraft:item/' + texture}})
        ]:
            p = ROOT / directory / (key + '.json'); p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    for locale in ['fr_fr', 'en_us']:
        p = ROOT / 'lang' / (locale + '.json')
        data = json.loads(p.read_text(encoding='utf-8'))
        data.update({'item.mmorpg.' + key: value[0] for key, value in ITEMS.items()})
        p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
if __name__ == '__main__': generate()
