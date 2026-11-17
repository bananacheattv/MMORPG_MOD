# Validation du 17 novembre 2026

- Minecraft Java 26.3, NeoForge 26.3.0.39-beta, Java 25.
- 100 fichiers Sponge v3, DataVersion 5023, empreintes SHA-256, dimensions et palettes contrôlées.
- Import natif terminé : **100/100 secteurs**. Comparaison exhaustive des blocs non vides et des cellules de biome : **0 différence**.
- Monde sauvegardé puis redémarré avec le mod intégrant les derniers changements de `main` : **PASS** pour l'arrivée libre et solide, la bordure de 800, 111 échantillons de bâtiments/monuments, 7 échantillons de biomes et les **110 panneaux en français**.
- Construction Gradle et **64 tests unitaires** réussis, dont les réglages de la petite carte et les zones sans apparition dans les villages.
- Vues géométriques calculées depuis les blocs inspectées avec Blender : place centrale, forêt, abbaye, citadelle, temple et observatoire. Les matériaux et les blocs fins y sont simplifiés.

Le premier essai a révélé une recherche de golem déclenchée par une citrouille décorative à la limite d'un chunk. L'importeur utilise maintenant le drapeau natif `Block.UPDATE_SKIP_ON_PLACE` pour éviter les effets d'une pose interactive pendant un collage. Le secteur concerné a été recollé, puis l'intégralité de la carte comparée. La vérification isolée utilise 4 Gio et désactive le préchargement général pour éviter la concurrence avec l'import.

**Limite du contrôle :** aucun client Minecraft connecté au bridge n'était disponible. La visite visuelle dans le jeu, le combat et la circulation de plusieurs joueurs restent à vérifier dans le client ; les contrôles ci-dessus ont réellement exécuté le serveur Minecraft et relu sa sauvegarde native.
