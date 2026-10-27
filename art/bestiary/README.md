# Bestiaire cubique — bêta 0.0

Les modèles restent composés de pavés Minecraft : silhouettes articulées, reliefs d’armure, selles, boucles, griffes et visages. Les 30 créatures importées gardent leurs textures et animations originales ; les 12 montures et 5 anciens boss conservent également leurs textures d’origine.

- `classic/` : projets Blockbench des onze créatures classiques et nouvel atlas matériel.
- `mounts/`, `mobs/` : projets éditables des modèles existants enrichis.
- `textures/` : copies des textures nécessaires à l’édition.
- `refinement-report.json` : nombres de cubes avant/après.
- `TEXTURES.md` : provenance de l’atlas des créatures classiques.

Génération : `python tools/refine-bestiary.py`, puis `node tools/generate-classic-bestiary.cjs`. Le premier script relit la référence Git `c92f062` ; il ne remplace pas les textures originales. Tests : `node --test tools/bestiary.test.cjs tools/yggdrasil/models.test.cjs`.

Les modèles des gardiens et des cristaux, leurs projets et leurs générateurs se trouvent dans `art/yggdrasil` et `tools/yggdrasil`.

## Retour aux anciens modèles

Une archive antérieure à la refonte est conservée hors dépôt dans le dossier `backups/modeles-avant-refonte-2026-10-26` du workspace, avec sa procédure de restauration et son SHA-256. La référence Git `c92f062` conserve aussi les sources initiales. Restaurer les ressources et les classes de rendu ensemble, puis recompiler le JAR ; ne pas remplacer les mondes ni les profils joueurs.

## Boucliers du Paladin

Les cinq boucliers utilisent les transformations des mains du bouclier vanilla 26.3. Leur géométrie a une origine centrée identique au modèle spécial vanilla (axe vertical descendant avant la transformation `[1,-1,-1]`). La poignée se trouve derrière la plaque. Les textures proviennent des blocs personnalisés existants, avec des dimensions compatibles avec les mipmaps.
