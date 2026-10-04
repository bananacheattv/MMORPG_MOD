#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Boss d'Eldoria pour Blockbench (format "Modded Entity"), generes entierement par script.

Usage :
  python generer_boss.py                      tous les boss, avec apercus et GIF
  python generer_boss.py roi_gobelin ignis    seulement ces boss
  python generer_boss.py --rapide             sans GIF (planches seulement)

Sorties, un dossier par boss (<nom>/) :
  <nom>.bbmodel                 projet de travail Blockbench (texture integree, os, animations)
  <nom>.png                     texture pixel art (box UV, 1 texel par pixel de modele)
  <nom>_emissive.png            masque des pixels lumineux (couche emissive optionnelle en jeu), si le boss en a
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

BOSS = ['roi_gobelin', 'liche_ancienne', 'ignis', 'titan_glace', 'avatar_neant']
# classes Java generees dans le mod (modele client + constantes communes)
JAVA = {'roi_gobelin': 'RoiGobelin', 'liche_ancienne': 'LicheAncienne', 'ignis': 'Ignis', 'titan_glace': 'TitanGlace',
        'avatar_neant': 'AvatarNeant'}
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
    if nom in JAVA and os.path.isdir(SRC):
        client, common = ek.java_export(m, tex, JAVA[nom], nom, getattr(mod, 'STRIKES', {}), 'art/boss_blockbench/generer_boss.py')
        for rel, code in ((('client', 'model', 'boss'), client), (('entity', 'boss', 'anim'), common)):
            d = os.path.join(SRC, *rel)
            os.makedirs(d, exist_ok=True)
            fname = JAVA[nom] + ('Model' if rel[0] == 'client' else 'Anims') + '.java'
            with open(os.path.join(d, fname), 'w', encoding='utf-8', newline='\n') as f:
                f.write(code)
    print(f'{nom}: {sub}')
    return m, tex, emi


if __name__ == '__main__':
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    for nom in args or BOSS:
        generer(nom, gifs='--rapide' not in sys.argv, anims='--sans-anim' not in sys.argv)
