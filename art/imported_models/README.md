# Modèles fournis par l'utilisateur

40 projets Blockbench originaux, conservés sans modification : 30 monstres et 10 cosmétiques.
Sources : dossiers `Bestiaire_Mmorpg_30_Mobs_Animes` et `Cosmetiques_Mmorpg_Animes_10_BBModels` fournis dans la conversation.

Régénération : `python tools/import_bbmodels.py` (aucun dossier externe nécessaire).
Le convertisseur conserve les cubes, pivots, textures intégrées, UV par face, hiérarchie et clés d'animation.
Les os sont adressés par UUID pour préserver les groupes portant le même nom.
Les ressources JSON sont chargées lors de la création des renderers, y compris lors du rechargement des ressources.

Les 30 monstres utilisent le combat au corps à corps du mod ; les animations idle, marche/vol/reptation et attaque sont raccordées.
Les ailes des créatures sont animées mais leur déplacement reste celui d'un monstre terrestre.
Les niveaux, apparitions par biome et butins sont modifiables dans `config/mmorpg/mobs.json`.

Les 10 cosmétiques sont obtenus dans les coffres cosmétiques ou par `/mmorpg cosmetique <joueur> <identifiant>`.
Ils occupent les catégories Coiffes 3D, Dos 3D, Visage 3D et Épaules 3D ; les effets de particules restent disponibles.

## Cosmétiques 3D, lot 2 (20 modèles)

Générés par `python tools/gen_cosmetics_3d.py` (bibliothèque `tools/cosmlib.py`, modèles dans `tools/gen_cosmetics_3d.py` et `tools/cosm_models_b.py`) :
modèles JSON + texture palette 64x64 écrits directement dans `src/main/resources/assets/mmorpg/`. Aperçu logiciel : `python tools/preview_cosmetic.py cle -o apercu.png`.
Coiffes : Couronne Solaire, Casque de Valkyrie, Tricorne de Corsaire, Couronne de Lauriers, Chapeau de Sorcière, Kabuto du Samouraï, Diadème de Lune.
Visage : Masque Vénitien, Masque d'Oni, Masque du Docteur de la Peste, Lunettes Steampunk.
Épaules : Épaulières du Phénix, Épaulières du Chevalier d'Or, Hibou d'Épaule.
Dos : Ailes de Phénix, Ailes de Libellule, Cape de Vampire, Sac d'Aventurier, Bouclier du Croisé, Réacteur à Vapeur.
Tous s'obtiennent dans les caisses / Lucky Blocks (`/mmorpg cosmetique <joueur> <id>`). La liste de l'onglet Cosmétiques défile à la molette.
