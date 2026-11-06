# Refonte 3D Aeloria : livraison à intégrer

État au 2026-11-06 : **22 montures livrées** (`sanglier` validé par le propriétaire, ne plus le modifier ; les 21 autres à valider) et **1 bloc** (`deco_puits`, validé). Les autres blocs, les mobs et les boss sont en cours. Rien n'est intégré au mod : ces fichiers sont dans `art/refonte_3d`, le jeu ne les lit pas. Base : commit `892a216`.

Ce dossier reprend l'arborescence du mod : copier `src/` par-dessus le dépôt suffit. Aucune classe Java n'a besoin de changer pour ces modèles.

## Contenu

| Fichier | Rôle |
|---|---|
| `bbmodels/<id>.bbmodel` | Source éditable, Blockbench 5 natif (format Generic Model, texture intégrée, animations). |
| `src/main/resources/assets/mmorpg/bbmodels/mounts/<id>.json` + `textures/imported/mounts/<id>.png` | Export lu par `ImportedModel("mounts", "<id>")`. Remplace les fichiers de même nom. Les 22 montures ont leur propre PNG, `drake_ascendant` compris. |
| `src/main/resources/assets/mmorpg/bbmodels/props/deco_puits.json` + `textures/imported/props/deco_puits.png` | Export lu par `ImportedModel("props", "deco_puits")`. Le JSON n'a plus de clé `texture` : le modèle utilise son propre PNG, plus l'atlas. |
| `src/main/resources/assets/mmorpg/textures/item/deco_puits.png` | Icône d'inventaire, fond transparent (les 121 anciennes ont un fond bleu opaque). |
| `decor_shapes_patch.json` | Entrée `deco_puits` à reporter dans `data/mmorpg/decor_shapes.json`. Mêmes 45 cellules qu'avant, boîtes recalculées par la logique de `tools/gen_decor_shapes.py`. |
| `CONTROLES_MONTURES.md` | Résultat des contrôles automatiques, monture par monture. |

## Installation

1. Copier `livraison/src` sur la racine du dépôt (écrase les JSON et PNG de même nom).
2. Copier `livraison/bbmodels/*.bbmodel` dans `bbmodels/`.
3. Reporter les clés de `decor_shapes_patch.json` dans `src/main/resources/data/mmorpg/decor_shapes.json`, ou relancer `tools/gen_decor_shapes.py`, qui produit la même entrée à partir du nouveau JSON.
4. Compiler, puis tester en jeu (liste ci-dessous).

## Trois pièges

- **Ne pas relancer les générateurs sur ces modèles.** `tools/gen_mounts.py` réécrit les montures, et `tools/gen_decor_bb.py` réécrit les décors : ils écraseraient la refonte. Exclure ces identifiants, ou ne plus utiliser ces scripts pour eux.
- **`tools/bbmount_lib.py:bbmodel_to_runtime` ne lit pas les fichiers Blockbench 5** : sur un `.bbmodel` re-sauvé, il plante (`KeyError: 'name'`), car les os sont dans `groups`. Pour retoucher un modèle : `outils/aeloria3d.py:export_runtime` lit l'ancien et le nouveau format.
- **Sens des animations.** Blockbench 5 applique les clés telles quelles pour un fichier 5.0, et inverse X (et Y en rotation) à l'ouverture d'un fichier plus ancien. Les fichiers livrés sont en 5.0. L'export donne `x_jeu = -x`, `y_jeu = -y` en rotation, et `x_jeu = -x` en position. `to_bbmodel` écrit encore du 4.10 avec cette même inversion, donc son aperçu Blockbench est à l'envers du jeu.

## Animations

| Modèles | Clip | Joué par le code actuel |
|---|---|---|
| Toutes les montures | `idle`, `walk` | Oui (`ImportedModel.setupAnim`, mélange selon `walkAnimationSpeed`). |
| `hippogriffe`, `drake_ascendant` | `fly` | Oui, pour les montures qui volent. Ailes construites ouvertes, repliées par les clips au sol. |
| Toutes sauf `scorpion_dunes` et `araignee_cavernes` | `run`, `jump` | Non : fournis, à raccorder si souhaité. Sans effet tant qu'ils ne sont pas appelés. |
| `deco_puits` | `chest` | Oui : boucle continue de `DecorModelRenderer`. |

Tous les os animés des originaux existent encore (`body`, `head`, `tail`, `leg_*`, `leg0l` à `leg3r`, `wing_l`, `wing_r`, `claw_l`, `claw_r`). Les os ajoutés (`machoire`, `ear_l`, `tail_bout`, `leg_*_bas`, `selle`, `nacelle`, `cou1`, `cou2`, `trompe1` à `trompe3`, `tour`, `oriflamme`, etc.) ne demandent aucun code : `ImportedModel` les parcourt tous.

`MountType` reste inchangé pour les 22 montures. Le dessus de chaque selle est calé sur le calcul du jeu (`16 × siège + 1`, arrondi à 0,5) : voir `CONTROLES_MONTURES.md`.

## Contrôles

**Faits, sans lancer le jeu**

- Les 22 montures ouvertes dans Blockbench 5.2.2 (trois vitrines, 1503 cubes), `validate_project` sans diagnostic, captures réelles dans `apercus/apres/montures`. Une vitrine réduit et range les modèles côte à côte : mêmes cubes, mêmes UV, mêmes PNG (`outils/vitrine.py`).
- Script `outils/controle_montures.py`, sans problème sur les 22 : clips et os d'origine présents, pistes sur des os existants, PNG présent, résolution exacte, hauteur de selle, moins de 150 cubes.
- Script de construction : grille respectée, aucune surface coplanaire qui scintille.
- Cavaliers : mannequins de contrôle placés par le calcul du jeu, sur les planches de travail.
- Aller-retour complet (fichier re-sauvé par Blockbench, export identique) : fait pour `sanglier` et `deco_puits` seulement. À refaire pour les 21 autres, Blockbench doit d'abord réautoriser l'écriture dans le dossier.

**À faire en jeu : non testé à ce jour**

- [ ] Tous les modèles se chargent, sans erreur de log.
- [ ] Orientation des textures (œil, emblème, groin), comparée aux captures.
- [ ] Sens et cadence de `idle`, `walk` et `fly`, pieds au sol.
- [ ] Cavalier assis sur la selle, à l'arrêt et en marche, à toutes les places ; aperçu dans `MountsScreen`.
- [ ] Pas de scintillement à distance ni en mouvement.
- [ ] Puits : collisions (socle, poteaux, toit), lumière, 4 orientations, balancement du seau, icône.

## Refaire ou retoucher

Depuis `refonte_3d/outils` : `python construire.py <id>` (Python 3, numpy, Pillow). Les modèles sont décrits dans `outils/modeles/`, les matières et palettes dans `outils/materiaux.py`, les pièces communes dans `outils/kit.py`, les cycles dans `outils/animations.py`. `sanglier` et `deco_puits` sont figés : leur référence est `bbmodels/`. Contrôles : `python controle_montures.py`, `python inventaire.py`.
