# Boss d'Eldoria — modèles Blockbench animés

Cinq boss **générés par script** (aucune modélisation à la main) : `python generer_boss.py` (Python 3.8+, Pillow, numpy).
Le code commun est dans `../entity_kit.py` ; chaque boss a son module (`roi_gobelin.py`, `liche_ancienne.py`, `ignis.py`, `titan_glace.py`, `avatar_neant.py`).

## Format Blockbench choisi : « Modded Entity » (Java)

- **Box UV** (le `texOffs` du code Java), tailles de cubes entières, une seule texture, 1 texel par pixel de modèle.
- **Os nommés en `snake_case` ASCII**, pivots placés aux articulations ; les pièces inclinées sont des cubes tournés (Blockbench crée lui-même les sous-parties `_r1` à l'export Java).
- **Animations limitées à ce que lisent tous les systèmes** : canaux rotation / position / échelle, interpolations linéaire et catmullrom.
  - Boucles : catmullrom.
  - Attaques et morts : linéaire, avec 3 clés intermédiaires qui adoucissent départ et arrivée.
  - Pourquoi : Blockbench, Java et GeckoLib n'interprètent pas tous le catmullrom de la même façon (Blockbench l'applique dès qu'une des deux clés l'est, Java et GeckoLib regardent la clé d'arrivée). Avec ce découpage, le mouvement est identique partout.
- **Fichiers au format 4.10** : ils s'ouvrent dans Blockbench 4.10 et plus, et dans Blockbench 5.x (qui les convertit à l'enregistrement).

Ce format convient aux trois cibles :
- Java vanilla : *Export Java Entity* et *Export Animations* sont natifs dans ce format.
- GeckoLib : *Fichier › Convertir le projet* vers « GeckoLib Animated Model ».
- Bedrock : conversion directe.

## Projet de travail et exports pour le jeu

| Fichier | Rôle |
|---|---|
| `<boss>/<boss>.bbmodel` | **travail** : projet éditable (texture intégrée, os, animations) |
| `<boss>/<boss>.png` | **jeu** : texture (box UV) |
| `<boss>/<boss>_emissive.png` | **jeu, facultatif** : pixels lumineux pour une couche émissive (tous les boss sauf le Roi gobelin) |
| `<boss>/apercu/*` | **contrôle** : vues (face, 3/4, profil, dos), planche des animations, un GIF par animation |
| `src/.../client/model/boss/<Boss>Model.java` | **jeu, généré** : géométrie (`LayerDefinition`) et animations (`AnimationDefinition`, Java vanilla 26.3) |
| `src/.../entity/boss/anim/<Boss>Anims.java` | **jeu, généré** : indices, durées et instants d'impact des animations (côté serveur) |

Ces classes sont écrites par `generer_boss.py` avec la même conversion que l'export Java de Blockbench, mais pour l'API 26.3. Le branchement dans le mod (déclenchement des animations, dégâts à l'impact) est décrit dans `../LISEZMOI.md`, section « Intégration dans le mod ».

## Les cinq boss

| Boss | Taille | Os | Cubes | Texture | Animations |
|---|---|---|---|---|---|
| Roi gobelin | 3,2 blocs (couronne comprise) | 24 | 61 | 128×256 | `repos`, `marche`, `coup_horizontal`, `frappe_verticale`, `cri_ralliement`, `etourdi`, `mort`, `rage_repos`, `rage_marche` |
| Liche ancienne | 3,3 blocs (bâton et couronne) | 23 | 66 | 128×256 | `repos` (lévitation), `deplacement`, `projectile`, `invocation`, `canalisation`, `recul`, `mort` |
| Ignis | 4 blocs (cornes) | 35 | 75 | 128×256 | `repos`, `marche`, `balayage`, `frappe_sol`, `souffle_feu`, `rugissement`, `mort` |
| Titan de glace | 6,2 blocs (cristaux) | 25 | 56 | 512×128 | `reveil`, `repos`, `marche`, `frappe_poing`, `double_frappe_sol`, `pietinement`, `exposition_noyau`, `mort` |
| Avatar du néant | 4,3 blocs (couronne) | 34 | 59 | 128×128 | `levitation`, `deplacement`, `charge_energie`, `ouverture_torse`, `teleportation_preparation`, `reformation`, `mort` |

Les morts sont en mode `hold` : la dernière pose reste affichée.

### Roi gobelin
- **Groupes** :
  - `racine` › `bassin` (avec `etendard`) › `torse` › `tete` (`machoire`, `oreille_*`, `couronne` légèrement tordue) ;
  - `bras_*` › `avant_bras_*` › `main_*` › `couperet` ;
  - `jambe_*` › `pied_*` ;
  - cape en trois segments `cape_haut` › `cape_milieu` › `cape_bas`.
- **Dos travaillé** : cape à liseré doré et ourlet déchiré (pixels découpés).
- **Armure dépareillée** : deux fers différents, maille, épaulière gauche surdimensionnée avec pointes.
- **Rage** : `rage_repos` et `rage_marche` (posture basse, bras écartés, souffle court, pas plus amples).
- **À programmer en jeu** : invocation des sbires pendant `cri_ralliement` (pic vers 0,9 s), dégâts.

### Liche ancienne
- **Groupes** :
  - `torse` › `cristal_ame`, `tete` › `machoire` / `couronne` ;
  - bras › avant-bras › mains (doigts écartés à gauche), `baton` › `cristal_baton` ;
  - 8 panneaux de robe (`robe_avant`, `robe_avant_droite`, … `robe_arriere_gauche`) et un jupon intérieur.
- **Mort** : le corps s'affaisse sur la robe étalée, le bâton tombe au sol.
- **Lisible sans émissif** : cyan clair dans la texture.
- **À programmer en jeu** : départ du projectile (main gauche, 0,55 s), sbires invoqués, particules, lumière du cristal, rayon de canalisation.

### Ignis
- **Groupes** :
  - `bassin` (`pagne`, `pagne_arriere`) ;
  - `torse` › `noyau`, `tete` › `cornes` (chaîne de 4 segments par corne) ;
  - `bras_*` › `epauliere_*` (os séparés : l'épaule reste mobile) et `avant_bras_*` › `main_*` › `epee` ;
  - `jambe_*` › `tibia_*` › `pied_*` : le genou est articulé, ce qui permet la mort à genoux.
- **Fissures de lave** : peintes dans la texture, pas modélisées.
- **Attaques en trois temps** (anticipation, frappe, récupération) :
  - balayage : frappe à 0,72-0,86 s ;
  - frappe au sol : impact à 1,0 s ;
  - souffle : flammes de 1,0 à 2,0 s.
- **À programmer en jeu** : flammes du souffle, fissures au sol, dégâts, rendu émissif.

### Titan de glace
- **Glace 100 % opaque** : bleu pâle à coulures, sans transparence.
- **Groupes** :
  - `torse` › `noyau`, plaques mobiles `plaque_droite` / `plaque_gauche` (charnières) et `plaque_ventre` ;
  - `epaule_*` › `cristaux_epaule_*` (amas plus fourni à droite) › `bras_*` › `avant_bras_*` › `poing_*` ;
  - `cristaux` dans le dos ; jambes à genou articulé.
- **`reveil`** : sa première image est la pose dormante.
- **`exposition_noyau`** : fenêtre de vulnérabilité de 1,0 à 3,0 s.
- **À programmer en jeu** : pics de glace, ralentissement, neige, vulnérabilité du noyau, onde du piétinement.

### Avatar du néant
- **Groupes** :
  - `torse` › `cage_droite` / `cage_gauche` (s'ouvrent), `noyau`, `masque` › `couronne` › 6 segments ;
  - `epauliere_*` détachées, `bras_*` › `avant_bras_*` › `main_*` › `doigts_*` ;
  - `bas_1` à `bas_4` (corps effilé) et `fragments` › `eclat_1` à `eclat_6`, en orbite.
- **Pivots** : chaque fragment pivote sur son propre centre.
- **Mort** : les pièces s'écartent du noyau en tournoyant, puis se posent au sol (positions calculées pièce par pièce).
- **À programmer en jeu** :
  - la téléportation réelle (`teleportation_preparation` reste figée sur la pose resserrée, `reformation` part de cette pose) ;
  - le tir à 1,95 s de `charge_energie` ;
  - le rayon d'`ouverture_torse` (1,2-1,8 s), les distorsions et les particules.

## Contrôles effectués

- Les 5 projets ont été **ouverts dans Blockbench 5.2.1** :
  - format Modded Entity, aucune erreur ;
  - box UV recalculé par Blockbench identique à celui du fichier ;
  - export Java du modèle et des animations sans erreur ;
  - chaque animation jouée.
- Pour le Roi gobelin, les positions des os animés dans Blockbench **correspondent au centième de pixel** à celles des aperçus.
- **Contact avec le sol** vérifié sur chaque animation. Restent des frottements brefs pendant les chutes : moins de 2 px, sauf les orteils d'Ignis et du Titan (6 à 9 px pendant environ 0,1 s).
