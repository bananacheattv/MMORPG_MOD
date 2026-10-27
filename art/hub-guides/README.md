# Guides et Maîtres de la Citadelle — bêta 0.0

Dix modèles articulés originaux et cubiques, avec projets Blockbench dans `models/`, animations et UV par face : cinq guides (archiviste, éclaireur, artisan, gardienne des guildes et capitaine), quatre Maîtres de classe et le Gardien de l’épreuve. Les silhouettes, coiffures, outils et vêtements diffèrent selon le métier. Le rapport `quality-report.json` décrit chaque modèle.

`material_atlas.png` a été généré avec le skill imagegen et l’outil intégré, puis copié sans retouche raster dans les ressources du mod. Prompt : atlas carré opaque de seize matériaux Minecraft, grille exacte 4 × 4 sans bordures ni texte ; peaux claire, foncée et âgée, cheveux châtains ; lin ivoire, tissu indigo brodé, cuir sarcelle, tissu bordeaux ; cuir brun, fer sombre, laiton, parchemin ; argent, barbe blonde, cheveux bruns, cristal turquoise. Texture albedo plate, pixel art de fantasy, sans rendu 3D.

Recréer les fichiers de modèles : `node tools/generate-hub-guides.cjs`. Contrôler les UV, les animations et les cubes : `node --test tools/bestiary.test.cjs`.

L’installation des guides ne reconstruit pas le hub : seul le bloc téléporteur à son emplacement connu est retiré. La plateforme, les arches, les portails physiques et le spawn manuel restent en place. Les guides apparaissent uniquement après chargement effectif des entités de leur chunk, avec UUID stables. Leur progression est sauvegardée dans les données de hub du personnage. `/tuto` indique le prochain guide ; les conversations restent consultables.

Les anciens Maîtres sont conservés dans Git (`9bc3b87`) et dans l’archive extérieure `backups/maitres-avant-refonte-2026-10-27/maitres-9bc3b87.zip`. Pour rétablir leurs rendus, restaurer l’ancien enregistrement de renderer et ses classes/textures, puis recompiler. Aucun monde à remplacer.
