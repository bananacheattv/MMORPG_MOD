"""Mannequin duelliste : texture d'entite (mannequin blinde teinte de rouge) et icone d'objet.

Appele par gen_hub.generate(ga) ; peut aussi etre lance seul pour ecrire les textures et le modele d'objet.
"""
import os

from PIL import Image, ImageEnhance

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'mmorpg')
ART = os.path.join(ROOT, 'art', 'mannequin_blockbench', 'blinde')
KEY = 'mannequin_entrainement_blinde'
ITEM = 'mannequin_duelliste'
NAME_FR, NAME_EN = 'Mannequin duelliste', 'Dueling Dummy'


def tint(img):
    """Rougit l'image en gardant les ombres : les gris de l'acier deviennent un fer rougi, la paille garde sa chaleur."""
    img = img.convert('RGBA')
    r, g, b, a = img.split()
    r = r.point(lambda v: min(255, int(v * 1.25 + 30)))
    g = g.point(lambda v: int(v * 0.62))
    b = b.point(lambda v: int(v * 0.58))
    out = Image.merge('RGBA', (r, g, b, a))
    return ImageEnhance.Contrast(out).enhance(1.08)


def generate(ga=None):
    tex = os.path.join(ASSETS, 'textures', 'entity', 'dummy', KEY + '.png')
    icon = os.path.join(ART, KEY + '_icone.png')
    src = Image.open(tex)
    out_tex = os.path.join(ASSETS, 'textures', 'entity', 'dummy', 'mannequin_entrainement_duelliste.png')
    tint(src).save(out_tex)
    if os.path.isfile(icon):
        item = tint(Image.open(icon))
    else:
        item = tint(Image.open(os.path.join(ASSETS, 'textures', 'item', 'mannequin_blinde.png')))
    if ga is not None:
        ga.simple_item(ITEM, NAME_FR, item)
        ga.LANG_EN[f'item.{ga.MODID}.{ITEM}'] = NAME_EN
    else:
        import json
        item.save(os.path.join(ASSETS, 'textures', 'item', ITEM + '.png'))
        with open(os.path.join(ASSETS, 'models', 'item', ITEM + '.json'), 'w', encoding='utf-8', newline='\n') as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"mmorpg:item/{ITEM}"}}, f, indent=2)
        with open(os.path.join(ASSETS, 'items', ITEM + '.json'), 'w', encoding='utf-8', newline='\n') as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"mmorpg:item/{ITEM}"}}, f, indent=2)
    print('mannequin duelliste : textures ecrites')


if __name__ == '__main__':
    generate()
