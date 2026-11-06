# Refonte 3D Aeloria : livraison à intégrer

État : **pilotes uniquement** (`sanglier`, `deco_puits`), en attente de validation artistique. Rien n'a été intégré au mod ni poussé sur `main`. Base : commit `892a216`.

Ce dossier reprend l'arborescence du mod : copier `src/` par-dessus le dépôt suffit. Aucune classe Java n'a besoin de changer pour ces deux modèles.

## Contenu

| Fichier | Rôle |
|---|---|
| `bbmodels/<id>.bbmodel` | Source éditable, Blockbench 5 natif (format Generic Model, texture intégrée, animations). |
| `src/main/resources/assets/mmorpg/bbmodels/mounts/sanglier.json` + `textures/imported/mounts/sanglier.png` | Export lu par `ImportedModel("mounts", "sanglier")`. Remplace les fichiers de même nom. |
| `src/main/resources/assets/mmorpg/bbmodels/props/deco_puits.json` + `textures/imported/props/deco_puits.png` | Export lu par `ImportedModel("props", "deco_puits")`. Le JSON n'a plus de clé `texture` : le modèle utilise son propre PNG, plus l'atlas. |
| `src/main/resources/assets/mmorpg/textures/item/deco_puits.png` | Icône d'inventaire, fond transparent (les 121 anciennes ont un fond bleu opaque). |
| `decor_shapes_patch.json` | Entrée `deco_puits` à reporter dans `data/mmorpg/decor_shapes.json`. Mêmes 45 cellules qu'avant, boîtes recalculées par la logique de `tools/gen_decor_shapes.py`. |

## Installation

1. Copier `livraison/src` sur la racine du dépôt (écrase les JSON et PNG listés ci-dessus).
2. Copier `livraison/bbmodels/*.bbmodel` dans `bbmodels/`.
3. Reporter la clé `deco_puits` de `decor_shapes_patch.json` dans `src/main/resources/data/mmorpg/decor_shapes.json`, ou relancer `tools/gen_decor_shapes.py`, qui produit la même entrée à partir du nouveau JSON.
4. Compiler, puis tester en jeu (liste ci-dessous).

## Trois pièges

- **Ne pas relancer les générateurs sur ces modèles.** `tools/gen_mounts.py` réécrit `sanglier`, et `tools/gen_decor_bb.py` réécrit `deco_puits` : ils écraseraient la refonte. Exclure ces identifiants, ou ne plus utiliser ces scripts pour eux.
- **`tools/bbmount_lib.py:bbmodel_to_runtime` ne lit pas les fichiers Blockbench 5** : sur un `.bbmodel` re-sauvé, il plante (`KeyError: 'name'`), car les os sont dans `groups`. Pour retoucher un modèle : `outils/aeloria3d.py:export_runtime` lit l'ancien et le nouveau format.
- **Sens des animations.** Blockbench 5 applique les clés telles quelles pour un fichier 5.0, et inverse X (et Y en rotation) à l'ouverture d'un fichier plus ancien. Les fichiers livrés sont en 5.0. L'export donne `x_jeu = -x`, `y_jeu = -y` en rotation, et `x_jeu = -x` en position. `to_bbmodel` écrit encore du 4.10 avec cette même inversion, donc son aperçu Blockbench est à l'envers du jeu.

## Animations

| Modèle | Clip | Joué par le code actuel |
|---|---|---|
| `sanglier` | `idle`, `walk` | Oui (`ImportedModel.setupAnim`, mélange selon `walkAnimationSpeed`). |
| `sanglier` | `run`, `jump` | Non : fournis, à raccorder si souhaité. Sans effet tant qu'ils ne sont pas appelés. |
| `deco_puits` | `chest` | Oui : boucle continue de `DecorModelRenderer`. |

Os du sanglier conservés : `body`, `head`, `tail`, `leg_fl`, `leg_fr`, `leg_bl`, `leg_br`. Os ajoutés : `machoire`, `ear_l`, `ear_r`, `tail_bout`, `leg_*_bas`, `selle`, `etrier_g`, `etrier_d`. Ajouter un os ne demande aucun code : `ImportedModel` les parcourt tous.

`MountType.SANGLIER` reste inchangé (siège 1,26, largeur 1,4, hauteur 1,4). Le dessus de la selle est à y = 21 pour un dessous de cuisses calculé à 21,16 (`16 × siège + 1`).

## Contrôles

**Faits, sans lancer le jeu**

- Ouverture dans Blockbench 5.2.2, `validate_project` sans diagnostic.
- Aller-retour : fichier re-sauvé par Blockbench, export identique (géométrie, texture, animations) ; `outils/controle_roundtrip.py`.
- Conditions de chargement rejouées : pistes sur des os existants, noms uniques, UV dans la texture, clés triées.
- Grille : densité unique (2 texels/unité pour la monture, 1 pour le bloc), 0 % hors grille, 0 paire de faces coplanaires visibles.
- Cavalier : mannequin de contrôle positionné par le calcul du jeu, dans Blockbench.

**À faire en jeu : non testé à ce jour**

- [ ] Les deux modèles se chargent, sans erreur de log.
- [ ] Orientation des textures (œil, emblème, groin), comparée aux captures.
- [ ] Sens et cadence de `idle` et `walk`, sabots au sol.
- [ ] Cavalier assis sur la selle, à l'arrêt et en marche ; aperçu dans `MountsScreen`.
- [ ] Pas de scintillement à distance ni en mouvement.
- [ ] Puits : collisions (socle, poteaux, toit), lumière, 4 orientations, balancement du seau, icône.

## Refaire ou retoucher

Depuis `refonte_3d/outils` : `python construire.py sanglier deco_puits` (Python 3, numpy, Pillow). Les modèles sont décrits dans `outils/modeles/`, les matières et palettes dans `outils/materiaux.py`, les cycles dans `outils/animations.py`. Le contrôle de toute la collection : `python inventaire.py`.

## Etat au 2026-11-06

Valides par le proprietaire : `sanglier` et `deco_puits`, ne plus les modifier. `loup_givre` : premiere version, non validee, non controlee dans Blockbench. Les 20 autres montures et les autres blocs restent a faire (voir `INVENTAIRE.md`). Rien n'est integre en jeu : ces fichiers vivent dans `art/refonte_3d` et le mod ne les lit pas.
