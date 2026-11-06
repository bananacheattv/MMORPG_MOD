# Refonte 3D Aeloria : livraison à intégrer

État au 2026-11-06 : **22 montures, 123 décors 3D, les blocs Java, 49 mobs, 5 boss et les 10 gardiens d'Yggdrasil livrés**, avec le cristal d'arène et le drone (`sanglier` et `deco_puits` validés par le propriétaire, ne plus les modifier ; les autres à valider). Blocs Java : 7 caisses (fermée et ouverte), 6 Lucky Blocks, 10 accessoires et le panneau de quêtes d'origine corrigé, textures des 10 classements, des 5 cultures et des matériaux. Les 8 structures animées n'ont pas été refaites : elles sont déjà en 16 texels par bloc, faites d'après les images du propriétaire. Rien n'est intégré au mod : ces fichiers sont dans `art/refonte_3d`, le jeu ne les lit pas. Base : commit `892a216`, plus `f8169d7` pour les originaux des mobs.

Ce dossier reprend l'arborescence du mod : copier `src/` par-dessus le dépôt suffit. Aucune classe Java écrite à la main n'a besoin de changer : seuls `<Boss>Model.java` et `<Boss>Anims.java`, générés, sont remplacés.

## Contenu

| Fichier | Rôle |
|---|---|
| `bbmodels/<id>.bbmodel` | Source éditable, Blockbench 5 natif (format Generic Model, texture intégrée, animations). |
| `src/main/resources/assets/mmorpg/bbmodels/mounts/<id>.json` + `textures/imported/mounts/<id>.png` | Export lu par `ImportedModel("mounts", "<id>")`. Remplace les fichiers de même nom. Les 22 montures ont leur propre PNG, `drake_ascendant` compris. |
| `src/main/resources/assets/mmorpg/bbmodels/props/<id>.json` + `textures/imported/props/<id>.png` | Export lu par `ImportedModel("props", "<id>")`, pour les 123 décors (`deco_`, `deco_cat_`, `decob_`, `camp`, `drapeau`). Le JSON n'a plus de clé `texture` : chaque décor utilise son propre PNG, plus l'atlas `decor_atlas.png`. |
| `src/main/resources/assets/mmorpg/textures/item/<id>.png` | Icône d'inventaire, fond transparent (les 121 anciennes ont un fond bleu opaque). |
| `decor_shapes_patch.json` | Collisions recalculées sur les nouvelles formes par la logique de `tools/gen_decor_shapes.py`, à reporter dans `data/mmorpg/decor_shapes.json`. |
| `src/main/resources/assets/mmorpg/models/block/<id>.json` + `textures/block/<id>.png` | Blocs Java : `caisse_*_bloc`, `caisse_*_bloc_ouverte`, `lucky_*`, accessoires. Chaque modèle a sa propre texture. Mêmes identifiants, mêmes `display`. Rotation sur un axe à 22,5 ou 45 degrés, coordonnées dans -16..32. |
| `src/main/resources/assets/mmorpg/models/block/panneau_quetes.json` | Modèle d'origine gardé, à la demande du propriétaire, avec ses textures d'origine. Seul changement : le côté intérieur du montant gauche, vu de face, n'avait pas de texture (`#missing`) et reçoit celle du cadre ; les 18 autres faces `#missing`, invisibles, sont retirées. |
| `src/main/resources/assets/mmorpg/textures/block/` | Matériaux, minerais, cristaux, cultures, classements : mêmes noms et mêmes tailles que les originaux. Les modèles de cube, dalle et escalier ne changent pas, les classements gardent leurs bandes aux mêmes hauteurs pour le texte du jeu. |
| `src/main/resources/assets/mmorpg/bbmodels/mobs/<id>.json` + `textures/imported/mobs/<id>.png` | Les 38 mobs lus par `ImportedMobRenderer`. Remplacent les fichiers de même nom. `ImportedMobs.java` ne change pas : même échelle, même boîte de collision. |
| `src/main/resources/assets/mmorpg/bbmodels/classic/<id>.json` + `textures/imported/classic/<id>.png` | Les 11 mobs lus par `ClassicMobRenderer`. Plus de clé `texture` : chaque mob a son PNG, l'atlas `textures/entity/bestiary/material_atlas.png` n'est plus lu par eux. Le dossier `textures/imported/classic` est nouveau. |
| `src/main/resources/assets/mmorpg/bbmodels/yggdrasil/<id>.json` + `textures/imported/yggdrasil/<id>.png` | Les 10 gardiens de guilde, les 3 états du cristal d'arène et le drone, lus par `YggdrasilRenderer`, `ArenaCrystalRenderer` et `ArenaDroneRenderer`. Plus de clé `texture` : chaque modèle a son PNG, l'atlas `textures/entity/yggdrasil/material_atlas.png` n'est plus lu par eux. Le dossier `textures/imported/yggdrasil` est nouveau. |
| `src/main/resources/assets/mmorpg/textures/entity/boss/<id>.png`, `<id>_emissive.png` | Textures des 5 boss (box UV). Le Roi gobelin n'a pas de couche émissive, comme aujourd'hui : `AnimatedModels` le rend sans. |
| `src/main/java/com/mmorpg/client/model/boss/<Boss>Model.java` | Géométrie et animations des boss, générées par `outils/construire_boss.py`, même structure que les classes actuelles. La taille de texture y est écrite (`LayerDefinition.create`) : **à copier en même temps que le PNG**, un modèle sans sa texture aurait de mauvais UV. |
| `src/main/java/com/mmorpg/entity/boss/anim/<Boss>Anims.java` | Identiques aux classes actuelles, commentaire d'en-tête mis à part : mêmes indices, mêmes durées, mêmes instants de frappe. |
| `CONTROLES_MONTURES.md`, `CONTROLES_DECORS.md`, `CONTROLES_MOBS.md`, `CONTROLES_BOSS.md` | Résultat des contrôles automatiques, modèle par modèle, avec les cellules de collision qui changent. |

## Installation

1. Copier `livraison/src` sur la racine du dépôt (écrase les JSON et PNG de même nom).
2. Copier `livraison/bbmodels/*.bbmodel` dans `bbmodels/`.
3. Reporter les clés de `decor_shapes_patch.json` dans `src/main/resources/data/mmorpg/decor_shapes.json`, ou relancer `tools/gen_decor_shapes.py`, qui produit la même entrée à partir du nouveau JSON.
4. Compiler, puis tester en jeu (liste ci-dessous).

## Trois pièges

- **Ne pas relancer les générateurs sur ces modèles.** `tools/gen_mounts.py` réécrit les montures, `tools/gen_decor_bb.py` réécrit les décors, `tools/import_pack2.py`, `tools/refine-bestiary.py`, `tools/generate-classic-bestiary.cjs` et `tools/generate-yggdrasil.cjs` réécrivent des mobs : ils écraseraient la refonte. Exclure ces identifiants, ou ne plus utiliser ces scripts pour eux.
- **`tools/bbmount_lib.py:bbmodel_to_runtime` ne lit pas les fichiers Blockbench 5** : sur un `.bbmodel` re-sauvé, il plante (`KeyError: 'name'`), car les os sont dans `groups`. Pour retoucher un modèle : `outils/aeloria3d.py:export_runtime` lit l'ancien et le nouveau format.
- **Sens des animations.** Blockbench 5 applique les clés telles quelles pour un fichier 5.0, et inverse X (et Y en rotation) à l'ouverture d'un fichier plus ancien. Les fichiers livrés sont en 5.0. L'export donne `x_jeu = -x`, `y_jeu = -y` en rotation, et `x_jeu = -x` en position. `to_bbmodel` écrit encore du 4.10 avec cette même inversion, donc son aperçu Blockbench est à l'envers du jeu.

## Animations

| Modèles | Clip | Joué par le code actuel |
|---|---|---|
| Toutes les montures | `idle`, `walk` | Oui (`ImportedModel.setupAnim`, mélange selon `walkAnimationSpeed`). |
| `hippogriffe`, `drake_ascendant` | `fly` | Oui, pour les montures qui volent. Ailes construites ouvertes, repliées par les clips au sol. |
| Toutes sauf `scorpion_dunes` et `araignee_cavernes` | `run`, `jump` | Non : fournis, à raccorder si souhaité. Sans effet tant qu'ils ne sont pas appelés. |
| Décors avec feu, cristal flottant, bannière, lanterne, encensoir, horloge, orbite | `chest` | Oui : boucle continue de `DecorModelRenderer`. Les flammes ondulent, les cristaux flottent, les bannières et les lanternes se balancent, les aiguilles tournent. |

Tous les os animés des originaux existent encore (`body`, `head`, `tail`, `leg_*`, `leg0l` à `leg3r`, `wing_l`, `wing_r`, `claw_l`, `claw_r`). Les os ajoutés (`machoire`, `ear_l`, `tail_bout`, `leg_*_bas`, `selle`, `nacelle`, `cou1`, `cou2`, `trompe1` à `trompe3`, `tour`, `oriflamme`, etc.) ne demandent aucun code : `ImportedModel` les parcourt tous.

`MountType` reste inchangé pour les 22 montures. Le dessus de chaque selle est calé sur le calcul du jeu (`16 × siège + 1`, arrondi à 0,5) : voir `CONTROLES_MONTURES.md`.

### Mobs

| Modèles | Clip | Joué par le code actuel |
|---|---|---|
| Tous les mobs | `idle`, `attack`, `death` | Oui (`ImportedModel.setupAnim`). `attack` dure ce qu'il durait, `death` dure 1 s : le jeu n'avance `deathTime` que jusqu'à 20 ticks et couche déjà le mob. |
| Les mobs qui marchent | `walk` | Oui. |
| `chevalier_fracture`, `devoreur_etoiles`, `djinn_braises`, `harpie_neiges`, `luciole_geante`, `mite_astrale`, `oeil_chaos`, `spectre_enchaine` | `fly` | Oui, à la place de `walk`, comme les originaux. |
| `limace_acide`, `wyrm_boreal` | `slither` | Oui, à la place de `walk`, comme les originaux. |
| Bestiaire classique | `cast` | Fourni comme à l'origine. Le code actuel ne le joue pas pour ces mobs. |

Comment les mobs tiennent à 16 texels par bloc sans changer le Java : `ImportedMobs` multiplie chaque modèle par son `scale` (2,759 pour le mousseux, 0,445 pour Morghul). Les modèles sont dessinés en texels de jeu, puis l'export divise la géométrie, les pivots et les positions animées par ce `scale`. Les UV ne bougent pas. Un `.bbmodel` de mob est donc à la même échelle que son JSON : ses coordonnées ne sont pas entières, c'est normal, et `outils/aeloria3d.py:export_runtime` redonne le même JSON.

Règle de nommage : un cube dont le nom contient `lueur` est écrit `emissive: true` dans le JSON, donc dessiné par `ImportedGlowLayer`. Les os d'un mob importé portent maintenant un nom lisible (`body`, `head`, `arm_-1`...) et plus un uuid : ni le Java ni rien d'autre ne les référence. Le bestiaire classique garde `root`, `body`, `head`, `arm_-1`, `arm_1`, `leg_-1`, `leg_1` (`leg_-10`... pour le loup et l'araignée), que `ImportedModel` cherche par leur nom pour le regard. `arm_-1` est le bras droit, côté +x dans Blockbench, celui qui tient l'arme.

### Gardiens d'Yggdrasil

Même format que les mobs, échelle 1 : 1 unité = 1 texel = 1/16 de bloc en jeu. Tous gardent `idle`, `walk`, `attack`, `cast` et `death`. `YggdrasilRenderer` joue `attack` et `cast`, et balance l'os racine en phase 2 et 3 : il ne cherche aucun autre os par son nom. Le renderer du cristal ne bascule jamais sur `walk`, d'où l'orbite posée dans `idle`.

### Boss

Les boss gardent leur format Modded Entity : le Java les construit par `texOffs` et `addBox`, chaque cube a donc son patron box UV complet et des tailles entières. `outils/construire_boss.py` prend le squelette et les animations dans les modules d'origine (`art/boss_blockbench/<id>.py`, repris tels quels) et seuls les volumes et les textures sont neufs (`outils/modeles/boss_*.py`). Mêmes os aux mêmes pivots, mêmes clips, mêmes instants de frappe (`STRIKES`) : le code serveur et les `Profile` d'`AnimatedModels` restent valables. Seule exception sans effet sur les clips : les cornes et les piques d'Ignis sont un peu plus longues, et chaque épaulière gagne une pique (`pique_droit_3`, `pique_gauche_3`).

Ne pas relancer `art/boss_blockbench/generer_boss.py` ni `tools/refine-bestiary.py` sur les boss : ils réécriraient les classes et les PNG.

## Contrôles

**Faits, sans lancer le jeu**

- Les 14 modèles d'Yggdrasil ouverts dans Blockbench 5.2.2 (planches de contact), `validate_project` sans diagnostic, captures réelles dans `apercus/apres/boss`. Script `outils/controle_mobs.py` : mêmes contrôles que pour les mobs.
- Les 5 boss ouverts dans Blockbench 5.2.2 (planche de contact, avec la hiérarchie des os), `validate_project` sans diagnostic, capture réelle dans `apercus/apres/boss`. `liche_ancienne.bbmodel` ouvert aussi tel quel, reconnu en Modded Entity, sans diagnostic.
- Script `outils/controle_boss.py`, sans problème sur les 5 : os, pivots et parents d'origine, animations dans le même ordre avec la même durée, PNG à la taille écrite dans `Model.java`, `Anims.java` identique à l'original, aucune face coplanaire entre cubes droits d'un même os.
- Les animations des boss rejouées sur les nouveaux volumes par le rendu logiciel d'`art/entity_kit.py` : planches de travail dans `apercus/travail/boss`, non livrées. Ce n'est pas un test en jeu.
- Les classes Java générées n'ont **pas été compilées** : elles sortent du même générateur que les classes actuelles.

- Les 49 mobs ouverts dans Blockbench 5.2.2 (cinq planches de contact, hiérarchie des os conservée), `validate_project` sans diagnostic, captures réelles dans `apercus/apres/mobs`. Sur ces planches, chaque mob est ramené à la même taille à l'écran pour qu'on le voie : ce ne sont pas les tailles relatives du jeu.
- Script `outils/controle_mobs.py`, sans problème sur les 49 : JSON et PNG présents, résolution égale au PNG, plus de clé `texture`, moins de 150 cubes, clips d'origine présents, pistes sur des os existants, os exigés par le code, lueur là où l'original en avait, chaque face à 16 texels par bloc en jeu, pieds au sol. Trois mobs sont nettement plus hauts que l'original (`oeil_chaos`, `elementaire_feu`, `araignee_venimeuse`) : voulu, noté dans `CONTROLES_MOBS.md`.

- Les 22 montures ouvertes dans Blockbench 5.2.2 (trois vitrines, 1503 cubes), `validate_project` sans diagnostic, captures réelles dans `apercus/apres/montures`. Une vitrine réduit et range les modèles côte à côte : mêmes cubes, mêmes UV, mêmes PNG (`outils/vitrine.py`).
- Script `outils/controle_montures.py`, sans problème sur les 22 : clips et os d'origine présents, pistes sur des os existants, PNG présent, résolution exacte, hauteur de selle, moins de 150 cubes.
- Les 123 décors ouverts dans Blockbench 5.2.2 (neuf planches de contact, tournés en 3/4 dans leur groupe), `validate_project` sans diagnostic, captures réelles dans `apercus/apres/blocs`. Script `outils/planche_bb.py`.
- Script `outils/controle_decors.py`, sans problème sur les 123 : PNG et icône présents, plus de clé `texture`, moins de 150 cubes, enveloppe d'origine à quelques unités près, collisions présentes.
- Script de construction : grille respectée, aucune surface coplanaire qui scintille (`resoudre_coplanaires` avance la plus petite des deux faces d'une unité).
- Cavaliers : mannequins de contrôle placés par le calcul du jeu, sur les planches de travail.
- Aller-retour complet (fichier re-sauvé par Blockbench, export identique) : fait pour `sanglier` et `deco_puits` seulement. À refaire pour les 21 autres, Blockbench doit d'abord réautoriser l'écriture dans le dossier.

**À faire en jeu : non testé à ce jour**

- [ ] Tous les modèles se chargent, sans erreur de log.
- [ ] Orientation des textures (œil, emblème, groin), comparée aux captures.
- [ ] Sens et cadence de `idle`, `walk` et `fly`, pieds au sol.
- [ ] Cavalier assis sur la selle, à l'arrêt et en marche, à toutes les places ; aperçu dans `MountsScreen`.
- [ ] Pas de scintillement à distance ni en mouvement.
- [ ] Décors : collisions, lumière, 4 orientations, animation `chest`, icônes ; les plus grands restent dans `getRenderBoundingBox` (8 blocs sur les côtés, 14 en hauteur).
- [ ] Gardiens : taille face à leur boîte de collision (`Yggdrasil.ALL`), ailes de `papillon_eclipse` vues de dos, anneaux de `oeil_yggdrasil` et de `serpent_couronnes`, cristal aux 3 couleurs d'équipe.
- [ ] Boss : compilation des classes régénérées, chargement de la couche, coups alignés sur `STRIKES`, couche émissive de la lave, des cristaux et du noyau, pose de mort au sol.
- [ ] Mobs : taille et hitbox, pieds au sol, sens des clips `walk`, `fly`, `slither`, moment du coup dans `attack`, regard du bestiaire classique, lueurs la nuit, ailes translucides de `luciole_geante` et `mite_astrale` vues de loin.

## Refaire ou retoucher

Depuis `refonte_3d/outils` : `python construire.py <id>` pour une monture ou un décor, `python construire_mob.py <id>` pour un mob, `python construire_boss.py <id>` pour un boss (Python 3, numpy, Pillow). Les modèles sont décrits dans `outils/modeles/` (`mobs_foret.py`, `mobs_marais.py`, `mobs_cryptes.py`, `mobs_givre.py`, `mobs_desert.py`, `mobs_neant.py`, `mobs_pack.py`, `mobs_classic.py`, `mobs_yggdrasil.py`, `boss_roi.py`, `boss_liche.py`, `boss_ignis.py`, `boss_titan.py`, `boss_avatar.py`), les matières et palettes dans `outils/materiaux.py`, les pièces communes dans `outils/kit.py`, les cycles dans `outils/animations.py`. `sanglier` et `deco_puits` sont figés : leur référence est `bbmodels/`. Contrôles : `python controle_montures.py`, `python controle_decors.py`, `python controle_mobs.py`, `python controle_boss.py`, `python inventaire.py`.
