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


def generate(ga):
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
