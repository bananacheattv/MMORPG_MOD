# Modèles Blockbench — Eldoria MMORPG

Tous les modèles sont **générés par script**, de façon reproductible : il n'y a aucune modélisation à la main. Python 3 et Pillow suffisent (`pip install pillow`).

| Dossier | Contenu | Script | Format Blockbench |
|---|---|---|---|
| `batons_blockbench/` | 6 bâtons de mage (niv. 1 → 100), **intégrés au mod** | `generer_batons.py` | Java Block/Item |
| `arcs_blockbench/` | 6 arcs + 3 étapes de traction chacun, **intégrés au mod** | `generer_arcs.py` | Java Block/Item |
| `marteaux_blockbench/` | 6 marteaux de guerre `marteau_niv_01` → `marteau_niv_100`, **intégrés au mod** | `generer_marteaux.py` | Java Block/Item |
| `epees_blockbench/` | 6 épées `epee_niv_01` → `epee_niv_100`, 5 **intégrées au mod** | `generer_epees.py` | Java Block/Item |
| `armures_blockbench/` | 20 panoplies `panoplie_01_niv_001` → `panoplie_20_niv_100`, 17 **intégrées au mod** | `generer_armures.py` + `exporter_java.py` | Modded Entity |
| `structures_blockbench/` | 3 structures multiblocs animées : autel d'invocation, téléporteur, forge arcanique, **intégrées au mod** | `generer_structures.py` | Modded Entity (états) |
| `boss_blockbench/` | 5 boss animés : Roi gobelin, Liche ancienne, Ignis, Titan de glace, Avatar du néant, **intégrés au mod** | `generer_boss.py` | Modded Entity (animé) |
| `familiers_blockbench/` | 3 familiers animés : Feu follet, Bébé slime, Chouette sage | `generer_familiers.py` | Modded Entity (animé) |

`blockbench_kit.py` regroupe le code commun : textures pixel art, rangement des UV, export, aperçus et réglages d'affichage.

`entity_kit.py` fait de même pour les entités animées (boss, familiers) :
- os et pivots, box UV conforme à Blockbench, peinture pixel art ;
- animations par poses-clés, export `.bbmodel` ;
- rendu d'aperçu avec z-buffer, planches et GIF ;
- contrôle du contact avec le sol.

Voir `boss_blockbench/LISEZMOI.md` et `familiers_blockbench/LISEZMOI.md`.

## Intégration dans le mod (Minecraft 26.3, NeoForge, sans dépendance)

Tout se régénère par script ; ne modifiez pas à la main les fichiers marqués « Généré automatiquement ».

| Quoi | Comment | Où |
|---|---|---|
| **Armes 3D** (bâtons, arcs, marteaux, épées) | modèles d'item copiés par `tools/generate_assets.py` (table `WEAPONS_3D`) | `assets/mmorpg/models/item/`, `items/` |
| **Armures 3D** | `armures_blockbench/exporter_java.py` écrit `ArmorLayers.java` (un modèle `HumanoidModel` par panoplie et par pièce) ; `ArmorModels` remplace le modèle vanilla via `IClientItemExtensions.getHumanoidArmorModel` | `client/model/armor/`, textures `textures/entity/armor/` |
| **Structures multiblocs** | `structures_blockbench/generer_structures.py` écrit `XxxModel.java`, `XxxAnims.java` et `StructureShapes.java` (collisions) ; le bloc fonctionnel dessine la structure (`StructureRenderer`) et pose des blocs de structure invisibles sur l'emprise | `block/structure/`, `client/model/structure/`, textures `textures/entity/structure/` |
| **Boss animés** | `boss_blockbench/generer_boss.py` écrit `XxxModel.java` (géométrie + animations) et `XxxAnims.java` (indices, durées, instants d'impact) ; `AnimatedBossRenderer` les affiche avec la couche lumineuse | `client/model/boss/`, `entity/boss/anim/`, textures `textures/entity/boss/` |

Après une modification d'un modèle : relancer le script du dossier concerné, puis `python tools/generate_assets.py` (textures et modèles d'item), puis recompiler.

**Armes.** Épées : `epee_recrue` → niv. 1, `lame_runique` → niv. 40 (épée runique), `epee_seigneur_guerre` → niv. 60, `lame_demoniaque` → niv. 80, `excalibur_celeste` → niv. 100 (`epee_niv_20` reste libre). La **Hache du Berserker** garde son sprite 2D (pas de hache 3D). Marteaux et arcs : dans l'ordre des niveaux. Les arcs utilisent leurs 3 modèles de traction.

**Armures.** Association set → panoplie dans `SET_PANOPLY` (`exporter_java.py`) :

| Classe | Niv. 5 | Niv. 25 | Niv. 50 | Niv. 85 |
|---|---|---|---|---|
| Guerrier | Acier du Soldat → 05 Armure de fer | Berserker → 06 Fer renforcé | Seigneur de Guerre → 12 Épaulières angulaires | Dieu de la Guerre → 19 Armure souveraine |
| Mage | Apprenti → 01 Cuir simple | Sorcier → 08 Acier et tissu sombre | Archimage → 11 Armure runique sombre | Avatar Arcanique → 14 Armure runique élaborée |
| Archer | Chasseur → 02 Cuir renforcé | Rôdeur → 03 Cuir clouté | Tireur d'Élite → 09 Acier noir bordé d'argent | Sylvestre Divine → 17 Acier noir cristallin |
| Tank | Garde → 04 Mailles et cuir | Gardien → 07 Acier poli | Paladin → 13 Chevalier cristallin | Titan Immortel → 18 Armure ancienne massive |

Néant Primordial (niv. 100) → 20 Armure mythique. Panoplies libres : 10, 15, 16.
- Chaque pièce suit le corps du joueur (marche, accroupi, bras). Le côté de chaque groupe est déduit de sa position : le rendu en jeu est celui de Blockbench.
- Les parties lumineuses des armures ne brillent pas dans le noir (pas de couche émissive pour l'équipement en 26.3 sans rendu personnalisé) ; leurs couleurs vives restent dans la texture.

**Boss.** Le repos et la marche sont pilotés par le mouvement (la vitesse de marche compense la foulée du modèle), les attaques par des événements serveur → client, la mort par `deathTime` (le corps reste le temps de l'animation, sans bascule vanilla). Les dégâts et effets des capacités sont appliqués **à l'instant d'impact** de l'animation :

| Boss | Capacité → animation |
|---|---|
| Roi gobelin | corps à corps → `coup_horizontal` ; onde de choc → `frappe_verticale` ; appel des gobelins → `cri_ralliement` ; furie → `rage_repos` / `rage_marche` |
| Liche ancienne | projectiles d'ombre → `projectile` ; squelettes → `invocation` ; drain de vie → `canalisation` (2 s) ; téléportation → `recul` |
| Ignis | corps à corps → `balayage` ; salve de feu → `souffle_feu` ; anneau de flammes → `frappe_sol` ; météores → `rugissement` |
| Titan de glace | apparition → `reveil` (immobile) ; corps à corps → `frappe_poing` ; frappe gelante → `double_frappe_sol` ; éclats → `pietinement` ; changement de phase → `exposition_noyau` (immobile 4 s) |
| Avatar du néant | orbes → `charge_energie` ; clignement → `teleportation_preparation` puis `reformation` ; trou noir → `ouverture_torse` |

Pendant une action animée, les autres capacités attendent. Hitbox agrandies : Roi gobelin 1,8 × 3, Liche 1,2 × 3,2, Ignis 1,8 × 4, Titan 3 × 6, Avatar 1,4 × 4 blocs. `etourdi` (Roi gobelin) n'est pas encore utilisé : le mod n'a pas d'étourdissement.

## 1. Armes (arcs, marteaux, épées) — format « Java Block/Item »

C'est le seul format Blockbench qui produit directement un **modèle d'item Minecraft Java**. Un projet « Generic Model » n'est pas exportable tel quel, et « Bedrock » ou « Modded Entity » servent aux entités.

**Contraintes respectées et vérifiées à chaque cube** (Minecraft 26.3, validées en ouvrant les fichiers dans Blockbench 5.2.1) :
- uniquement des cubes, avec des coordonnées comprises entre **-16 et 32** sur chaque axe ;
- **une rotation par cube, sur un seul axe**, à -45, -22,5, 0, 22,5 ou 45° pour tous les modèles au repos ;
- **UV par face** et une seule texture carrée (64×64 ou 128×128) ;
- pas de rotation de groupe : les groupes servent seulement à organiser le projet ;
- `light_emission` de 0 à 15 sur les cristaux, les runes et les lignes d'énergie.

**Exception : la corde tendue et les branches fléchies des arcs (`traction/*.bbmodel`).** Elles utilisent des **angles libres**. Minecraft 26.3 et Blockbench 5 les acceptent si la version cible du projet est 26.3, et c'est déjà réglé dans les fichiers (`java_block_version: 26.3`). Sur une version plus ancienne, il faudrait refaire ces trois modèles avec des angles par pas de 22,5°.

**Arcs : repos et traction.** Chaque arc a un modèle au repos et trois modèles de traction : `_pulling_0`, `_pulling_1` et `_pulling_2`.
- Les deux branches fléchissent de 7, 18 puis 31°.
- La corde garde sa longueur et rejoint le point d'encoche (allonge de 5, 8 puis 11 px).
- La flèche encochée passe par la fenêtre de tir de la poignée.
- `minecraft/assets/mmorpg/items/arc_niv_XX.json` choisit le modèle selon la durée de traction, comme l'arc vanilla (`using_item`, puis `use_duration` aux seuils 0,65 et 0,9).

**Tenue en main.** Les réglages sont dérivés de ceux de l'arc et de l'épée vanilla. Ils ont été vérifiés en jeu en 1re personne, en 3e personne, pendant la traction et dans l'inventaire. Les très grandes armes sont légèrement réduites en 1re personne.

**Groupes.**

| Famille | Groupes |
|---|---|
| Arcs | `poignee`, `branche_superieure`, `branche_inferieure`, `corde`, `cristaux` (et `fragments_flottants`), `ornements`, `fleche` (en traction uniquement) |
| Marteaux | `manche`, `poignee`, `pommeau`, `tete`, `renforts`, `cristaux`, `fragments` |
| Épées | `lame`, `tranchants`, `garde`, `poignee`, `pommeau`, `cristaux`, `fragments` |

**Exporter après une modification.** Fichier › Exporter › *Modèle Block/Item*. Chaque dossier contient aussi `minecraft/`, un pack de ressources prêt à l'emploi : modèles JSON, textures, définitions d'item et `pack.mcmeta` au format 97 (Minecraft 26.3).

## 2. Armures — format « Modded Entity »

Une armure 3D n'est **pas** un modèle d'item : elle s'affiche sur le corps du joueur. Le bon format est donc **Modded Entity**, avec export Java en Mojang mappings 1.17 et plus.

- **Os calés sur le `HumanoidModel` du joueur**, avec les mêmes pivots que vanilla : tête (0, 24, 0), torse (0, 24, 0), bras (±5, 22, 0), jambes (±1,9, 12, 0).
- **Quatre pièces séparées**, chacune dans son groupe :

  | Pièce | Os |
  |---|---|
  | `casque` | `casque_tete` |
  | `plastron` | `plastron_torse`, `plastron_bras_droit`, `plastron_bras_gauche` (épaulières comprises) |
  | `jambieres` | `jambieres_taille`, `jambieres_jambe_droite`, `jambieres_jambe_gauche` |
  | `bottes` | `bottes_pied_droit`, `bottes_pied_gauche` |

- **Box UV obligatoire.** C'est ce qu'utilise le code Java (`texOffs`). Les cubes ont des dimensions entières, et l'épaisseur fine passe par le *gonflement* (`inflate`), comme vanilla. Il y a 1 texel par pixel, sur une texture de 64×64 ou 128×128.
- **Rotations uniquement sur les os.** Les pièces inclinées (couronnes, ailerons, pointes, épaulières angulaires) sont des sous-os tournés.
- **Articulations.**
  - Les épaulières sont attachées aux bras, le reste du plastron au torse.
  - Les jambières couvrent le haut des jambes et les bottes le bas.
  - Les mains restent dégagées : les canons d'avant-bras s'arrêtent 2 px au-dessus.
- **Gabarit de référence.** Chaque projet contient le groupe `reference_joueur` (mannequin du joueur), **masqué et non exporté**.
- **Vérification.** Ouvert dans Blockbench 5.2.1 : format Modded Entity, aucune erreur, box UV identique à celle calculée par Blockbench.

### Fichiers de travail et fichiers d'intégration

| Fichier | Rôle |
|---|---|
| `panoplie_XX_niv_YYY.bbmodel` | **travail** : projet éditable (texture intégrée) |
| `panoplie_XX_niv_YYY.png` | **intégration** : texture de l'armure (box UV) |
| `panoplie_XX_niv_YYY_emissive.png` | **intégration** : masque des pixels lumineux, pour une couche émissive |
| `panoplie_XX_niv_YYY_vues.png` | **contrôle** : vues de face, de profil, de dos et 3/4 sur le mannequin |
| `apercu/lot_1..4.png`, `apercu/panoplies.png` | **contrôle** : planches par lots de 5 et planche des 20 |

**Un `.bbmodel` seul ne rend pas une armure équipable.** Pour Minecraft 26.3 avec NeoForge, il faut :
1. **Exporter** chaque projet depuis Blockbench (*Export Java Entity*, Mojang mappings) pour obtenir le `LayerDefinition`, ou le générer depuis ces fichiers.
2. **Brancher le modèle sur l'objet** avec `IClientItemExtensions.getHumanoidArmorModel(stack, layerType, original)`, qui renvoie un `HumanoidModel` construit à partir de ce `LayerDefinition`.
3. **Déclarer la texture** dans l'*equipment asset* (`assets/<ns>/equipment/<nom>.json`) et la placer dans `textures/entity/equipment/humanoid/`.
4. **Facultatif : rendre les parties lumineuses brillantes.** Il faut une seconde passe de rendu avec `_emissive.png` (type de rendu « eyes »).

Pour le mod Eldoria (Minecraft 26.3 + NeoForge), cette intégration peut être faite directement, sans dépendance supplémentaire. Il faut d'abord décider comment répartir les 20 panoplies entre les 17 armures existantes et les 4 classes. Avec GeckoLib, il faudrait au contraire convertir les projets au format « GeckoLib Animated Model ».

## 3. Effets lumineux : ce qui marche tel quel et ce qui demande du code

| Effet | Armes (Java Block/Item) | Armures (Modded Entity) |
|---|---|---|
| Couleurs vives des cristaux et des runes | texture, fonctionne partout | texture, fonctionne partout |
| Parties qui restent éclairées dans le noir | `light_emission`, natif en 26.3 | **à coder** : couche émissive avec `_emissive.png` |
| Halo, bloom, rayonnement | **shader** (Iris…) ou rendu personnalisé | idem |
| Fragments qui lévitent ou tournent, énergie qui pulse | **rendu personnalisé** ; les modèles sont statiques | **à coder** dans `setupAnim` ou avec GeckoLib |
| Traction de l'arc | **native** (définition d'item `use_duration`) | — |
| Liens d'énergie semi-transparents | natif (texture translucide) | natif (texture découpée) |

Tous les modèles sont pensés pour rester lisibles **sans shader** : les cristaux et les runes sont d'abord des formes et des couleurs, et la lumière ne vient qu'en complément.
