#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Familiers d'Eldoria pour Blockbench (format "Modded Entity"), generes entierement par script.

Usage :
  python generer_familiers.py              tous les familiers, avec apercus et GIF
  python generer_familiers.py feu_follet  seulement ce familier
  python generer_familiers.py --rapide             sans GIF (planches seulement)

Sorties, un dossier par familier (<nom>/) :
  <nom>.bbmodel                 projet de travail Blockbench (texture integree, os, animations)
  <nom>.png                     texture pixel art (box UV, 1 texel par pixel de modele)
  <nom>_emissive.png            masque des pixels lumineux (couche emissive optionnelle en jeu), si le familier en a
  apercu/<nom>_vues.png         face, 3/4, profil, dos + texture
  apercu/<nom>_animations.png   6 poses par animation
  apercu/<nom>_<anim>.gif       chaque animation en boucle
Necessite Python 3.8+, Pillow et numpy.
"""
import importlib
import os
import sys

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
sys.path.insert(0, os.path.join(ICI, '..'))
import entity_kit as ek  # noqa: E402

BOSS = ['feu_follet', 'bebe_slime', 'chouette_sage', 'loup_spectral', 'golem_poche', 'fee_lumineuse', 'phenix_dore', 'dragonnet_neant']
# classes Java generees pour le mod (src/main/java/com/mmorpg/client/model/pet et pet/anim)
JAVA = {'feu_follet': 'FeuFollet', 'bebe_slime': 'BebeSlime', 'chouette_sage': 'ChouetteSage', 'loup_spectral': 'LoupSpectral',
        'golem_poche': 'GolemPoche', 'fee_lumineuse': 'FeeLumineuse', 'phenix_dore': 'PhenixDore', 'dragonnet_neant': 'DragonnetNeant'}
SRC = os.path.join(ICI, '..', '..', 'src', 'main', 'java', 'com', 'mmorpg')


def generer(nom, gifs=True, anims=True):
    mod = importlib.import_module(nom)
    m = mod.build()
    if anims and hasattr(mod, 'animations'):
        mod.animations(m)
    tex, emi = ek.build_texture(m)
    ek.check(m, tex)
    out = os.path.join(ICI, nom)
    ap = os.path.join(out, 'apercu')
    os.makedirs(ap, exist_ok=True)
    ek.write_json(os.path.join(out, f'{nom}.bbmodel'), ek.bbmodel(m, tex, mod.ENTITY_CLASS, mod.COLORS, mod.VISIBLE_BOX))
    tex.save(os.path.join(out, f'{nom}.png'))
    if emi.getbbox():
        emi.save(os.path.join(out, f'{nom}_emissive.png'))
    n_cubes = len(m.cubes())
    sub = f'{len(m.order)} os · {n_cubes} cubes · texture {tex.width}x{tex.height} · {len(m.animations)} animations'
    ek.views_sheet(m, tex, emi, os.path.join(ap, f'{nom}_vues.png'), mod.TITLE, sub, scale=getattr(mod, 'VIEW_SCALE', 6.0))
    if m.animations:
        ek.anim_sheet(m, tex, emi, os.path.join(ap, f'{nom}_animations.png'), f'{mod.TITLE} — animations',
                      scale=getattr(mod, 'SHEET_SCALE', 3.0), yaw=getattr(mod, 'SHEET_YAW', 35))
        if gifs:
            for a in m.animations:
                ek.anim_gif(m, tex, emi, a, os.path.join(ap, f'{nom}_{a.name}.gif'),
                            scale=getattr(mod, 'GIF_SCALE', 3.0), yaw=getattr(mod, 'SHEET_YAW', 35))
    for var in getattr(mod, 'VARIANTES', []):          # variantes de texture (ex. slime sans translucidite)
        mv = mod.build(var)
        if anims and hasattr(mod, 'animations'):
            mod.animations(mv)
        tv, ev = ek.build_texture(mv)
        ek.check(mv, tv)
        ek.write_json(os.path.join(out, f'{nom}_{var}.bbmodel'), ek.bbmodel(mv, tv, mod.ENTITY_CLASS, mod.COLORS, mod.VISIBLE_BOX))
        tv.save(os.path.join(out, f'{nom}_{var}.png'))
        ek.views_sheet(mv, tv, ev, os.path.join(ap, f'{nom}_{var}_vues.png'), f'{mod.TITLE} ({var})', sub, scale=getattr(mod, 'VIEW_SCALE', 6.0))
    if nom in JAVA and os.path.isdir(SRC):
        client, common = ek.java_export(m, tex, JAVA[nom], nom, {}, 'art/familiers_blockbench/generer_familiers.py',
                                        client_pkg='com.mmorpg.client.model.pet', common_pkg='com.mmorpg.pet.anim',
                                        art_dir='art/familiers_blockbench')
        for rel, code in ((('client', 'model', 'pet'), client), (('pet', 'anim'), common)):
            d = os.path.join(SRC, *rel)
            os.makedirs(d, exist_ok=True)
            fname = JAVA[nom] + ('Model' if rel[0] == 'client' else 'Anims') + '.java'
            with open(os.path.join(d, fname), 'w', encoding='utf-8', newline=chr(10)) as f:
                f.write(code)
    print(f'{nom}: {sub}')
    return m, tex, emi


if __name__ == '__main__':
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    for nom in args or BOSS:
        generer(nom, gifs='--rapide' not in sys.argv, anims='--sans-anim' not in sys.argv)
