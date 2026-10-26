# Gardiens d’Yggdrasil et cristal — sources artistiques

Cette refonte est limitée aux dix gardiens d’Yggdrasil et au cristal d’arène. Les cinq anciens boss, les monstres existants, les pets et les montures ne sont pas remplacés.

Les fichiers `models/*.bbmodel` sont des projets **Blockbench Free Model** éditables : véritables maillages polygonaux, faces triangulaires et quadrilatères, articulations nommées, UV continus et animations. Ouvrir le projet dans Blockbench en conservant `material_atlas.png` dans le dossier parent. Si Blockbench demande la texture, sélectionner ce fichier.

Chaque gardien possède une silhouette, un matériau principal et une armature propres. Le cristal possède trois états de capture utilisant la même géométrie, les mêmes articulations et des surfaces colorées différentes. Les yeux, les cœurs et les runes ont un passage de rendu lumineux séparé ; les carapaces restent éclairées par le monde.

## Projets

1. `models/scarabee_racines.bbmodel` — carapace segmentée, six pattes articulées, mandibules et antennes.
2. `models/mante_epines.bbmodel` — thorax effilé, quatre faux, manteau végétal et yeux composés.
3. `models/raie_celeste.bbmodel` — ailes nervurées, queue souple, branchies et couronne dorsale.
4. `models/hydre_corail.bbmodel` — trois cous, trois têtes, mâchoires, crocs et cornes de corail.
5. `models/millepattes_ambre.bbmodel` — quatorze segments, vingt-huit pattes, carapaces et pinces buccales.
6. `models/meduse_oracles.bbmodel` — cloche sculptée, douze tentacules, cœur lumineux et couronne.
7. `models/crabe_bastion.bbmodel` — armure rivetée, huit pattes, deux pinces mobiles et yeux sur pédoncules.
8. `models/papillon_eclipse.bbmodel` — quatre ailes nervurées, ocelles, antennes plumeuses et griffes.
9. `models/serpent_couronnes.bbmodel` — corps spiralé, couronnes orbitales, grande mâchoire et crocs.
10. `models/oeil_yggdrasil.bbmodel` — œil central, trois anneaux sculptés et branches fractales.
11. `models/crystal_neutral.bbmodel`, `crystal_red.bbmodel`, `crystal_blue.bbmodel` — cœur facetté, cage, gyroscopes gravés, runes et éclats orbitaux.

## Production et contrôle

`node tools/generate-yggdrasil.cjs` produit les projets et les ressources Minecraft depuis les sources de géométrie et d’animation de `tools/yggdrasil/`. `node --test tools/yggdrasil/models.test.cjs` contrôle la validité des surfaces, UV, articulations, animations et références des projets. `quality-report.json` donne les mesures et les comptes de géométrie par créature.

Le diffuse original a été créé avec le tool intégré **image_gen**. Son prompt exact et sa provenance sont consignés dans [TEXTURES.md](TEXTURES.md). Le fichier maître est [material_atlas.png](material_atlas.png) ; la copie utilisée en jeu est `src/main/resources/assets/mmorpg/textures/entity/yggdrasil/material_atlas.png`. Aucun modèle 3D existant n’a été repris pour ces gardiens.
