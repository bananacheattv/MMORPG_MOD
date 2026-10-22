# -*- coding: utf-8 -*-
"""Ressources du hub d'entree : mannequins d'entrainement (textures d'entite, icones d'objet) et traductions fr / en.

Appele par generate_assets.main() ; les modeles viennent de art/mannequin_blockbench (generer_mannequin.py).
"""
import os
import shutil

from PIL import Image

# (id d'objet, dossier de variante, cle de texture, nom fr, nom en)
DUMMIES = [
    ('mannequin_entrainement', 'standard', 'mannequin_entrainement', "Mannequin d'entraînement", 'Training Dummy'),
    ('mannequin_blinde', 'blinde', 'mannequin_entrainement_blinde', 'Mannequin blindé', 'Armored Training Dummy'),
    ('mannequin_magique', 'magique', 'mannequin_entrainement_magique', 'Mannequin magique', 'Magical Training Dummy'),
]


MAITRES = [('guerrier', 'maitre_guerrier'), ('mage', 'maitre_mage'), ('archer', 'maitre_archer'), ('tank', 'maitre_tank'),
           ('epreuve', 'gardien_epreuve')]
NOMS = {'maitre_guerrier': ('Maître Guerrier', 'Warrior Master'), 'maitre_mage': ('Maître Mage', 'Mage Master'),
        'maitre_archer': ('Maître Archer', 'Archer Master'), 'maitre_tank': ('Maître Paladin', 'Paladin Master'),
        'gardien_epreuve': ("Gardien de l'épreuve", 'Trial Guardian')}


def hub_dimension(ga):
    """Dimension dediee du hub (vide, hauteur 256, cycle jour/nuit du monde principal, aucune apparition de monstres)."""
    ga.write_json(os.path.join(ga.DATA, ga.MODID, 'dimension_type', 'hub.json'), {
        'ambient_light': 0.05,
        'attributes': {
            'minecraft:visual/cloud_color': '#ccffffff',
            'minecraft:visual/cloud_height': 176.0,
            'minecraft:gameplay/bed_rule': {'can_set_spawn': 'never', 'can_sleep': 'never', 'explodes': False},
            'minecraft:gameplay/respawn_anchor_works': False,
        },
        'coordinate_scale': 1.0,
        'has_ceiling': False,
        'has_ender_dragon_fight': False,
        'has_fixed_time': False,
        'has_skylight': True,
        'height': 256,
        'infiniburn': '#minecraft:infiniburn_overworld',
        'logical_height': 256,
        'min_y': 0,
        'monster_spawn_block_light_limit': 0,
        'monster_spawn_light_level': 0,
        'skybox': 'overworld',
        'timelines': '#minecraft:in_overworld',
        'default_clock': 'minecraft:overworld',
    })
    ga.write_json(os.path.join(ga.DATA, ga.MODID, 'dimension', 'hub.json'), {
        'type': f'{ga.MODID}:hub',
        'generator': {'type': 'minecraft:flat', 'settings': {
            'biome': 'minecraft:the_void', 'features': False, 'lakes': False,
            'layers': [{'block': 'minecraft:air', 'height': 1}], 'structure_overrides': []}},
    })


def generate(ga):
    hub_dimension(ga)
    import gen_font
    gen_font.generate(ga.ASSETS)
    import gen_decor
    gen_decor.generate(ga)
    import gen_music
    gen_music.generate(ga.ASSETS)
    import gen_hub_music
    gen_hub_music.generate(ga.ASSETS)
    import build_sfx as gen_sfx
    gen_sfx.generate()
    import gen_quest_wand
    gen_quest_wand.generate(ga)
    import gen_duelliste
    gen_duelliste.generate(ga)
    classes = os.path.join(os.path.dirname(__file__), '..', 'art', 'classes_blockbench')
    for folder, key in MAITRES:
        for suffix in ('', '_emissive'):
            src = os.path.join(classes, folder, f'{key}{suffix}.png')
            if os.path.isfile(src):
                shutil.copyfile(src, ga.path(ga.ASSETS, 'textures', 'entity', 'hub', f'{key}{suffix}.png'))
            else:
                print(f'ATTENTION : texture du maitre {key}{suffix} absente (lancer art/classes_blockbench/generer_classes.py)')
        ga.LANG[f'entity.{ga.MODID}.maitre_classe'] = 'Maître de classe'
        ga.LANG_EN[f'entity.{ga.MODID}.maitre_classe'] = 'Class Master'
    art = os.path.join(os.path.dirname(__file__), '..', 'art', 'mannequin_blockbench')
    for item_id, variant, key, fr, en in DUMMIES:
        for suffix in ('', '_emissive'):
            src = os.path.join(art, variant, f'{key}{suffix}.png')
            if os.path.isfile(src):
                shutil.copyfile(src, ga.path(ga.ASSETS, 'textures', 'entity', 'dummy', f'{key}{suffix}.png'))
            elif not suffix:
                print(f'ATTENTION : texture du mannequin {key} absente (lancer art/mannequin_blockbench/generer_mannequin.py)')
        icon = os.path.join(art, variant, f'{key}_icone.png')
        img = Image.open(icon).convert('RGBA') if os.path.isfile(icon) else Image.new('RGBA', (32, 32), (160, 120, 60, 255))
        ga.simple_item(item_id, fr, img)
        ga.LANG_EN[f'item.{ga.MODID}.{item_id}'] = en
    ga.LANG_EN[f'entity.{ga.MODID}.mannequin_entrainement'] = 'Training Dummy'
    ga.LANG[f'entity.{ga.MODID}.mannequin_entrainement'] = "Mannequin d'entraînement"
