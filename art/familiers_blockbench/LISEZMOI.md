# Familiers d'Eldoria — modèles Blockbench animés

Huit familiers **générés par script** : `python generer_familiers.py` (Python 3.8+, Pillow, numpy).
Même kit que les boss (`../entity_kit.py`) et mêmes règles : voir `../boss_blockbench/LISEZMOI.md`.

## Format Blockbench : « Modded Entity » (Java)

- **Box UV**, une texture, 1 texel par pixel de modèle.
- **Échelle** : les animations des familiers en utilisent. Java vanilla (`AnimationDefinition`, cible `SCALE`) et GeckoLib la gèrent toutes deux.

| Fichier | Rôle |
|---|---|
| `<familier>.bbmodel` | **travail** : projet éditable |
| `<familier>.png` | **jeu** : texture |
| `<familier>_emissive.png` | **jeu, facultatif** : couche lumineuse (feu follet, pendentif de la chouette) |
| `apercu/*` | **contrôle** : vues, planche d'animations, GIF |

## Tailles et échelle de rendu

Les modèles sont à la densité Minecraft standard (1 px = 1/16 de bloc), avec des visages lisibles. Pour obtenir exactement la taille demandée, il suffit d'une **échelle dans le renderer** :

| Familier | Hauteur du modèle | Taille demandée | Échelle de rendu |
|---|---|---|---|
| Feu follet | 11,5 px (0,72 bloc) | 0,5 bloc | 0,7 |
| Bébé slime | 8 px (0,5 bloc) | 0,4 bloc | 0,8 |
| Chouette sage | 14 px (0,87 bloc) | 0,7 bloc | 0,8 |

## Feu follet
- **Groupes** :
  - `racine` › `noyau` › `visage` (plan des yeux, pour cligner) ;
  - `flamme_1` (bas), `flamme_2` (côtés et dos, en escalier), `flamme_3` (sommet et volute).
- **Animations** : `flottement` (avec clignement), `deplacement` (incliné, flammes qui traînent), `joie`, `canalisation` (flammes resserrées qui se tordent autour du noyau).
- **Lisible sans shader** : les couleurs sont dans la texture.
- **À intégrer en jeu** : étincelles, halo, éclairage dynamique. Le rendu plein éclat se fait avec `_emissive.png` ou un type de rendu « eyes ».

## Bébé slime
- **Groupes** : `racine` › `enveloppe`, `racine` › `noyau` › `visage`. Le visage est visible à travers l'enveloppe.
- **Animations** : `repos` (gélatineux), `preparation_saut` (figée tassée), `saut`, `reception`, `double_rebond`, `repos_aplati`.
- **Enchaînement d'un saut** : `preparation_saut` › `saut` › `reception`. `saut` contient un petit bond visuel ; supprimez sa piste de position si le jeu fait déjà sauter l'entité.
- **Transparence** :
  - `bebe_slime.png` a une enveloppe translucide. Elle demande un type de rendu translucide en jeu, comme le slime vanilla.
  - Variante sans aucun pixel semi-transparent : `bebe_slime_opaque.png` / `.bbmodel`. Arêtes et coins pleins, faces découpées : elle fonctionne avec un rendu « cutout » classique.

## Chouette sage
- **Groupes** :
  - `racine` › `corps` (pendentif) › `tete` › `bec` et `paupieres` ;
  - `aile_*` › `aile_*_bout` : deux segments, repliés le long du corps sans le traverser ;
  - `queue`, `patte_*`.
- **Paupières** : un plan sur charnière, rangé dans la tête au repos, qui bascule devant les yeux pour cligner ou dormir.
- **Animations** : `repos` (tête inclinée, curieuse), `clignement`, `petits_pas`, `decollage`, `vol`, `atterrissage`, `sommeil`, `perche` (pose perchée).
- **Dos et dessous des ailes** : peints (dessous crème, plumes tachetées).

## Loup spectral
- **Groupes** : `racine` › `corps` › `cou` › `tete` › `machoire`, `oreilles` ; 4 pattes ; `queue_1` à `queue_3`.
- **Animations** : `repos`, `marche`, `course`, `assis`, `sommeil` (couché), `queue_joyeuse`, `hurlement`, `morsure`.
- Corps opaque ; les effets spectraux (runes, yeux) sont dans `_emissive.png`.

## Golem de poche
- **Référence** : pierre beige-gris moussue, orbites sombres aux yeux ambrés, gemme en losange dans la poitrine, spirales gravées sur les poings.
- **Groupes** : `racine` › `corps` › `tete` (`paupieres`), `noyau`, `bras_*` › `avant_bras_*` ; `jambe_*`.
- **Animations** : `repos`, `marche`, `joie`, `frappe_sol`, `sommeil` (assis, yeux fermés).

## Fée lumineuse
- **Référence** : cheveux blonds fleuris, oreilles pointues, robe verte à liserés dorés, jupon crème, bottes brunes, cristal lumineux tenu dans la main gauche.
- **Ailes** : 4 plans translucides (membrane semi-transparente, bords et nervures lumineux) : rendu translucide nécessaire.
- **Groupes** : `racine` › `corps` › `tete` (`paupieres`), `bras_droit`, `bras_gauche` › `cristal` (compense la rotation du bras pour rester droit), `aile_haut_*`, `aile_bas_*` ; `jambe_*`.
- **Animations** : `flottement`, `deplacement`, `joie` (pirouette), `sortilege`. Elle vole à 3 px du sol dans toutes ses animations.

## Phénix doré
- **Référence** : plumage or, poitrail crème, pointes orange et rouges lumineuses, crête de trois plumes, bec crochu, œil noir à iris de braise.
- **Ailes** : modélisées déployées (bras + main, rémiges séparées) et rabattues le long du corps par la pose de repos ; les animations de vol annulent ce repli.
- **Groupes** : `corps` › `cou` › `tete` (`bec_bas`, `crete`) ; `aile_*` › `aile_*_bout` ; `queue` › `queue_bout` ; `patte_*`.
- **Animations** : `repos`, `vol`, `plane`, `joie` (ailes au ciel, cri).

## Dragonnet du néant
- **Référence** : obsidienne noir-violet, grands yeux violets lumineux, cornes à pointe de cristal, runes luisantes, ailes de chauve-souris, queue en 4 segments terminée par un cristal.
- **Groupes** : `corps` › `cou` › `tete` (`machoire`, `paupieres`, `corne_*`) ; `aile_*` › `aile_*_bout` ; `queue_1` à `queue_4` ; 4 pattes.
- **Animations** : `repos`, `marche`, `vol`, `joie`, `souffle`. En jeu il **vole en permanence** à côté du joueur (demande de l'utilisateur).

## Rotations (rappel)
Convention Blockbench / Java : sur un membre qui pend, une rotation X **positive** le porte **vers l'avant** ; sur une partie dressée (tête, buste), une rotation X positive la penche **en arrière**.

## Intégration dans le mod
- `generer_familiers.py` écrit aussi le Java : `client/model/pet/<Nom>Model.java` (géométrie + animations) et `pet/anim/<Nom>Anims.java` (indices). `tools/generate_assets.py` copie les textures dans `textures/entity/pet/`.
- Rendu : `client/model/pet/PetRenderer` (translucide + couche lumineuse), `PetModel` mélange les boucles selon l'état (repos, marche/course, vol, assis puis endormi après 5 s / 15 s d'immobilité, joie à l'invocation).
- Comportement : `entity/PetEntity` + `PetType.move()` (vol permanent, au sol, posé à l'arrêt). Position calculée par chaque client (pas d'à-coups).
- Échelles de rendu : feu follet 0,7 · slime 0,8 · chouette 0,8 · loup 0,8 · golem 0,75 · fée 0,75 · phénix 0,7 · dragonnet 0,8.
- Test en jeu : `./gradlew runClient -PquickWorld=<monde> -Pshowcase=familiers` (vitrine des 8 familiers puis suivi du joueur, captures dans `run/screenshots`).

## Contrôles
Les 4 premiers projets (feu follet, slime, chouette, loup) ont été ouverts dans Blockbench 5.2.1 (variante opaque du slime comprise) ; golem, fée, phénix et dragonnet ont été vérifiés par les aperçus et en jeu, pas encore ouverts dans Blockbench :
- aucune erreur ;
- box UV identique à celui du fichier ;
- export Java du modèle et des animations sans erreur ;
- toutes les animations jouables.
