# Aéloria — Les Cinq Horizons

Carte Minecraft Java **26.3** de **800 × 800 blocs**, X/Z **−400 à 399**, arrivée **0.5 / 84 / 0.5**. Cinq régions, en réunissant la forêt et le marais à la demande du propriétaire.

| Région | Paysage et habitats | Lieux majeurs |
| --- | --- | --- |
| Sylveronce et Brumevert | Forêt, prairie, gorge, marais | Clairbois, village sur pilotis, arbre-sanctuaire |
| Val des Cryptes | Lande sombre, arbres morts, sépultures | Veillepierre, abbaye avec crypte, ossuaire |
| Hauts de Boréalis | Montagnes, conifères, lac gelé | Boréalis, citadelle, mine du Givre |
| Dunes d’Ignis | Désert, reliefs rouges, volcan | Sahr-Éclat, temple solaire, caldeira |
| Éclats d’Astréon | Terres astrales, cristaux, îlots suspendus | Astréon, observatoire, géode et passerelles |

La place centrale relie les cinq régions. La carte comprend 7 lieux habités, 71 bâtiments avec mobilier, 21 étals, 5 monuments principaux, 4 galeries, 6 jardins cultivés, 15 haltes/ruines secondaires et 2 ponts de pierre. Les villages sont des constructions ; aucun marchand, quête ou récompense supplémentaire n’est créé par cette carte.

## Fichiers et génération

- `schematics/` : 100 secteurs de 80 × 320 × 80 blocs, y = −64 à 255, biomes inclus ; `importation.csv` donne les positions et SHA-256.
- `map.json` : inventaire, palette, repères, panneaux, coordonnées des villages.
- `general-openworld-fragment.json` : champ `openWorld` à fusionner dans le `general.json` du serveur Openworld seulement.
- `carte-openworld.png` : cartographie calculée depuis les blocs produits, pas une capture du client.
- Sources éditables : `tools/openworld_map/` ; Python 3.12, numpy, Pillow, nbtlib.

```powershell
python -m pip install numpy pillow nbtlib
python tools/openworld_map/generate.py --out CHEMIN_SORTIE
python tools/openworld_map/package_metadata.py CHEMIN_SORTIE
python tools/openworld_map/preview.py --out CHEMIN_SORTIE --jar CHEMIN_MINECRAFT_26_3_CLIENT_JAR
```

Le générateur utilise la graine 161126. Le terrain, les constructions et la végétation sont reproductibles. Les exports utilisent le [format officiel Sponge v3](https://github.com/SpongePowered/Schematic-Specification/blob/master/versions/schematic-3.md), DataVersion 5023 validée par le moteur Minecraft 26.3. Ils contiennent au maximum 85 états de blocs par secteur, sous la limite de 256 de l’importeur du mod.

## Installation

La livraison native est un dossier de monde `openworld` produit et sauvegardé par NeoForge **26.3.0.39-beta**, avec Java **25**. Arrêter le serveur concerné avant de changer son monde. Copier le monde natif dans le dossier du serveur et régler `level-name=openworld`. La mémoire demandée reste **4 Gio minimum / 16 Gio maximum**.

Si l’on reconstruit depuis les schémas : utiliser impérativement un **nouveau monde vide**, car `/aeloriaimport` ignore l’air. Ne pas coller ces secteurs sur un monde déjà construit. Le script `native_import.cjs` attend un serveur de construction séparé, port local 25686, role `OPENWORLD`, stockage partagé isolé, avec les secteurs dans `config/worldedit/schematics` ; il importe, vérifie, pose les textes des panneaux, fixe l’arrivée et la bordure, puis sauvegarde et arrête le serveur. Le correctif de l’importeur livré dans cette branche évite que la pose décorative d’une citrouille déclenche une recherche de golem dans des chunks non chargés.

Les vrais biomes pilotent les familles de mobs existantes dans `mobs.json`. La configuration de cette carte réduit le rayon central sans apparition à 58 blocs, ajoute les périmètres des villages et adapte la progression à un rayon de 400 blocs. Les autres serveurs gardent les valeurs historiques de 200 / 2500. La bordure est de 800 blocs ; le générateur hors carte reste vide. Les réglages de la carte désactivent l’évolution aléatoire des blocs et la propagation du feu afin de conserver le décor.

## Vérification

`export_map.py` vérifie les 100 fichiers, dimensions, palettes et empreintes. `/aeloriaimport verify` compare ensuite tous les blocs non vides et les biomes à la carte chargée par Minecraft. Le contrôle opt-in `-Dmmorpg.openworldMapChecks=CHEMIN/native-checks.json` vérifie après redémarrage la bordure, le sol et la hauteur libre à l’arrivée, 111 échantillons de bâtiments/monuments, 7 habitats et les 110 panneaux français. Son résultat est écrit dans `openworld-map-checks.txt`.

Les vues Blender sont des vérifications de géométrie à matériaux simplifiés ; elles ne remplacent pas une visite dans le client Minecraft. Le rapport de livraison distingue l’import/lecture native du contrôle visuel côté client.
