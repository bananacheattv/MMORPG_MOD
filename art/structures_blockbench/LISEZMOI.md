# Structures multiblocs d'Eldoria — autel, téléporteur, forge

Trois structures **générées par script** d'après vos images : `python generer_structures.py` (Python 3.8+, Pillow, numpy ; `--rapide` sans GIF).
Code commun : `structure_common.py` (pierre maçonnée, renforts argentés, runes, cristaux) et `../entity_kit.py`.

## Format Blockbench : « Modded Entity »

Les structures dépassent la limite d'un modèle de bloc Java (3 blocs maximum par axe) et leurs cristaux sont animés. Elles sont donc au format **Modded Entity** et dessinées en jeu par le bloc fonctionnel, comme les boss :
- box UV, une texture par structure, 1 texel par pixel de modèle (16 texels par bloc, comme Minecraft) ;
- un masque `_emissive.png` pour les parties lumineuses (runes, cristaux, flammes, portail) ;
- trois **animations d'état** : repos, activation (jouée une fois), actif (en boucle).

| Fichier | Rôle |
|---|---|
| `<nom>/<nom>.bbmodel` | **travail** : projet éditable (texture intégrée, groupes, animations d'état) |
| `<nom>/<nom>.png`, `<nom>_emissive.png` | **jeu** : texture et parties lumineuses |
| `<nom>/apercu/` | **contrôle** : vues (face, 3/4, profil, dos), planche des états, GIF |
| `src/.../client/model/structure/<Nom>Model.java`, `<Nom>Anims.java` | **jeu, généré** : géométrie et animations |
| `src/.../block/structure/StructureShapes.java` | **jeu, généré** : collisions de chaque bloc de l'emprise |

Convention : la structure regarde vers −Z (escaliers, entrée) ; « gauche » et « droite » sont vus de face. Le bloc fonctionnel est à l'origine (x −8..8, z −8..8, y 0..16).

## Les trois structures

| Structure | Emprise | Bloc fonctionnel | Groupes | États |
|---|---|---|---|---|
| **Autel d'invocation** | 5 × 5 blocs, 4,5 de haut | autel central | `plateforme`, `marches`, `autel` › `cristal_autel`, `pilier_avant_gauche/droit`, `pilier_arriere_gauche/droit` › `cristal_*` | `inactif`, `activation`, `actif` |
| **Téléporteur** | 6 × 3 blocs, 5 de haut | sol du passage | `plateforme`, `arche` › `montant_gauche`, `montant_droit`, `linteau` ; `cristal` › `cristal_arche` ; `pupitre` › `cristal_pupitre` ; `portail` | `inactif`, `activation`, `actif` |
| **Forge arcanique** | 5 × 4 blocs, 4,5 de haut | socle de l'enclume | `plateforme`, `four`, `cheminee`, `foyer` › `braises`, `flammes` ; `enclume`, `cristal` › `cristal_source`, `conduits`, `etabli`, `outils` | `eteinte`, `allumage`, `active` |

- **Autel** : plateforme à 3 marches, quatre piliers runiques (ceux de l'arrière plus hauts), cristaux violets, autel à cavité avec un cristal bleu enchâssé, gravures lumineuses reliant les piliers au centre. Actif : les cristaux s'élèvent et tournent, le cristal central sort de sa cavité et pulse.
- **Téléporteur** : arche aux angles en escalier, runes violettes et médaillons à gemme sur les montants, grand cristal au-dessus du passage, deux marches, lignes d'énergie, pupitre d'activation à tablette inclinée. Passage libre de 2,25 × 3 blocs. Le **portail est un groupe séparé** : le jeu ne l'affiche que quand le téléporteur est actif ; l'activation l'ouvre du centre vers les bords.
- **Forge** : four en pierre noire avec ouverture renforcée de métal, cheminée, foyer bleu-violet aux braises orangées ; enclume sur un socle runique ; cristal source à l'arrière relié au four et au socle par des conduits lumineux ; établi en bois sombre avec marteau, pinces, lingots et rangements. Éteinte : braises seules ; allumage : les flammes montent ; active : elles vacillent, le cristal flotte et tourne.

## Ce qui est dans le modèle, ce qui est programmé en jeu

| Élément | Où |
|---|---|
| Géométrie, textures, cristaux animés, flammes, ouverture du portail | **modèle** (animations d'état) |
| Lueur des runes et des cristaux (plus forte et pulsée quand actif) | **jeu** : couche `_emissive.png`, toujours éclairée |
| Portail translucide visible seulement quand actif | **jeu** |
| Particules : flammes, fumée de cheminée, énergie du cristal (forge) ; portail et étincelles (téléporteur) ; convergence vers le centre pendant l'invocation (autel) | **jeu** (`animateTick` des blocs) |
| Quand une structure est active | **jeu** : autel = pendant une invocation ; forge et téléporteur = un joueur à moins de 7 blocs |

## Intégration dans le mod

- Le bloc fonctionnel garde son rôle : ouvrir la forge, invoquer un boss. Le téléporteur s'utilise en **passant sous l'arche** : la liste des destinations s'ouvre une fois par passage, et l'arrivée se fait devant l'arche de destination.
- À la pose, la structure **regarde vers le joueur**. Elle n'est posée que si **toute l'emprise est libre**, sinon un message l'indique.
- Les autres blocs de l'emprise reçoivent un **bloc de structure invisible** qui porte la collision calculée depuis le modèle. On marche sur la plateforme, on monte les marches, on passe sous l'arche, on bute contre les piliers et le four. Ces blocs renvoient clics et casse vers le bloc fonctionnel. Casser n'importe quelle partie casse la structure, qui redonne son objet ; le téléporteur reste réservé aux administrateurs.
- **Mondes existants** : au chargement, les anciens blocs reçoivent leur entité de bloc et leurs parties, aux endroits libres.
- Les cristaux, flammes, outils et le portail n'ont pas de collision.
