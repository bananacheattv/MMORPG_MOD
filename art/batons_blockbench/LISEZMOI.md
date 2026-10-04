# Bâtons de mage — 6 évolutions pour Blockbench

Ce dossier contient six bâtons de mage, des niveaux 1, 20, 40, 60, 80 et 100. Ce sont de vrais modèles 3D en cubes, éditables dans Blockbench et utilisables comme modèles d'item Minecraft Java.

| Fichier | Niveau | Nom | Cubes |
|---|---|---|---|
| `baton_niv001.bbmodel` | 1 | Bâton d'apprenti | 24 |
| `baton_niv020.bbmodel` | 20 | Bâton d'initié | 26 |
| `baton_niv040.bbmodel` | 40 | Bâton de mage | 54 |
| `baton_niv060.bbmodel` | 60 | Bâton d'archimage | 69 |
| `baton_niv080.bbmodel` | 80 | Bâton légendaire | 95 |
| `baton_niv100.bbmodel` | 100 | Bâton mythique | 111 |

Contenu du dossier :
- `textures/` : les textures pixel art (64×64). Elles sont aussi intégrées dans chaque `.bbmodel`.
- `minecraft/` : un pack de ressources prêt à l'emploi, avec les modèles d'item JSON déjà exportés, les textures et les définitions d'item.
- `apercu/` :
  - `evolution.png` : les six bâtons côte à côte, à la même échelle ;
  - `*_vues.png` : face, profil, 3/4, dos et texture de chaque bâton ;
  - `en_jeu.png` : captures dans Minecraft 26.3.
- `generer_batons.py` : le script qui génère tout (Python 3 et Pillow).

## Format de projet Blockbench

Les projets utilisent le format **« Java Block/Item »** (`model_format: java_block`). Les fichiers `.bbmodel` sont en version 4.10.

Ils ont été ouverts dans **Blockbench 5.2.1**. Le validateur ne signale aucune erreur ni aucun avertissement. L'export Java (Fichier › Exporter › Modèle Block/Item) produit le même nombre d'éléments, sans coordonnée hors limites ni angle interdit.

C'est le seul format Blockbench qui donne directement un modèle d'item Minecraft Java. Les autres formats ne sont pas compatibles tels quels :
- **Generic Model** : il autorise les maillages libres, les rotations de groupes et des tailles quelconques, que le format Java refuse.
- **Bedrock Model** et **Modded Entity** : ce sont des modèles d'entité, pas des modèles d'item.

## Contraintes respectées (Minecraft Java, vérifiées dans le code de la 26.3)

1. **Cubes uniquement.** Pas de maillage libre. Les formes « sculptées » (cristaux facettés, croissants, lames, anneaux) sont faites de cubes orientés.
2. **Limites de -16 à 32 sur chaque axe**, soit 48 unités au maximum. Les bâtons mesurent de 39 à 46,5 unités de long. La taille en jeu se règle dans l'onglet *Affichage*.
3. **Une rotation par cube, sur un seul axe, de -45°, -22,5°, 0°, 22,5° ou 45°.** Minecraft 26.3 accepte aussi des angles libres et les rotations sur plusieurs axes. Je m'en suis tenu à la règle classique pour rester compatible avec toutes les versions et avec n'importe quel réglage de version dans Blockbench.
4. **Pas de rotation ni de pivot de groupe à l'export.** Les groupes servent uniquement à organiser le projet. Blockbench les exporte dans un champ `groups` que Minecraft ignore.
5. **UV par face** (box UV désactivé), avec une **seule texture carrée de 64×64** (puissance de deux, densité de 2 pixels de texture par unité). Les faces identiques partagent la même zone de texture.
6. **Plaques de runes plates** (épaisseur 0, une seule face visible), posées 0,1 unité devant la surface. Leurs pixels transparents sont gérés par le rendu des items.
7. **`light_emission` de 0 à 15 par cube** (cristaux, runes, lignes d'énergie). Ces cubes restent lumineux même dans le noir. Il n'y a pas de halo : il faudrait un shader.
8. **Les modèles d'item sont statiques.** Les éléments « flottants » sont détachés du bâton mais ne bougent pas. Une lévitation animée demanderait un rendu spécial (GeckoLib ou un rendu d'item personnalisé).
9. **Réglages d'affichage** dans les limites de Minecraft (translation ±80, échelle ≤ 4). Ils sont identiques pour les six bâtons :
   - 3e personne : bâton vertical, la main sur la poignée en cuir ;
   - 1re personne : bâton à droite, tête visible en haut de l'écran ;
   - inventaire : bâton en diagonale, éclairé de face (`gui_light: front`).

## Organisation des groupes

| Groupe | Contenu |
|---|---|
| `manche` | bois, fourreaux métalliques, anneaux sculptés |
| `poignee` | cuir (usé ou tressé), bagues, lanière qui pend (présente sur les six) |
| `embout` | pointe inférieure, pommeau, cristal inférieur |
| `tete/structure`, `tete/lames`, `tete/couronne_basse`, `tete/couronne_haute`, `tete/anneaux_orbitaux` | structure de la tête |
| `tete/cristal` | cristal ou pierre (lumineux) |
| `runes` | runes, lignes d'énergie et liens lumineux |
| `ornements` | plaques en losange avec gemme |
| `elements_flottants` | fragments de cristal et segments détachés |

**Fil conducteur de la famille :**
- la même poignée en cuir, de y=0 à y=8, avec sa lanière ;
- le même manche de 2 unités ;
- la rune d'Eldoria (un losange évidé) :
  - niveau 1 : gravée dans un nœud du bois ;
  - niveau 20 : incrustée en bleu dans le collier ;
  - niveau 40 et suivants : en argent ou lumineuse.

## Utiliser les modèles dans Minecraft

**Déjà intégré au mod Eldoria.** `tools/generate_assets.py` (fonction `staff_models_3d`) copie les modèles et associe chaque bâton à son modèle :

| Objet du mod | Niveau requis | Modèle |
|---|---|---|
| Bâton de l'Apprenti | 1 | `baton_niv001` |
| Sceptre de Givre | 10 | `baton_niv020` |
| Bâton des Flammes Infernales | 25 | `baton_niv040` |
| Bâton Arcanique | 45 | `baton_niv060` |
| Sceptre du Néant | 70 | `baton_niv080` |
| Bâton de l'Archimage Éternel | 95 | `baton_niv100` |

Après une modification dans Blockbench, il faut relancer `python generer_batons.py`, puis `python tools/generate_assets.py`.

- **Sans rien exporter** : copier le contenu de `minecraft/assets/mmorpg/` dans les ressources du mod, ou utiliser le dossier `minecraft/` comme pack de ressources. Son `pack.mcmeta` est au format 97, celui de Minecraft 26.3.
- **Depuis Blockbench, après une modification** :
  1. Fichier › Exporter › *Exporter le modèle Block/Item*, puis placer le JSON dans `assets/<ns>/models/item/`.
  2. Placer la texture dans `assets/<ns>/textures/item/` (le chemin enregistré est `mmorpg:item/baton_nivXXX`).
- **Associer un modèle à un objet** (depuis la 1.21.4) : créer `assets/<ns>/items/<id>.json` avec le contenu `{"model": {"type": "minecraft:model", "model": "mmorpg:item/baton_niv100"}}`.
- **Garder une icône 2D dans l'inventaire** : utiliser un modèle `minecraft:select` sur la propriété `minecraft:display_context`.

## Régénérer

```bash
pip install pillow
python generer_batons.py
```

Le script est déterministe : il reproduit exactement les mêmes fichiers. La géométrie de chaque niveau est décrite dans les fonctions `baton_1()` à `baton_100()`, avec des outils de construction (`col`, `poly`, `arc`, `crystal`, `frag`, `plate`…). Les règles du format Java (limites et angles) sont vérifiées à chaque cube créé.
