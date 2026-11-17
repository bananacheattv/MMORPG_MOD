# Aéloria — Les Cinq Horizons (800 × 800)

Demande : créer une vraie carte Minecraft Java 26.3, très détaillée, villages et monuments emblématiques, réunissant les cinq régions des mobs.

Le mod a six familles : Forêt ancienne, Marais, Cryptes, Terres gelées, Désert volcanique, Néant astral. Choix confirmé : réunir Forêt et Marais pour conserver tous les habitats dans cinq régions.

## Contraintes et livraison

- Coordonnées X/Z : -400 à 399, arrivée au centre 0.5 / 84 / 0.5.
- Construction dans un monde indépendant. Instruction du 17 novembre : installer sur Openworld puis supprimer l'ancienne carte, sans la conserver.
- Terrain naturel sculpté, transitions organiques, itinéraires praticables, rivières, falaises, grottes et cinq silhouettes monumentales.
- Villages avec maisons différentes, intérieurs, marchés, puits, jardins, quais et éclairage.
- Export Sponge v3, DataVersion 5023 vérifiée dans le projet ; 100 secteurs de 80×80, CSV compatible avec l'importeur natif existant.
- Palette limitée à 256 états par secteur pour l'importeur Aeloria ; remappage local autorisé. Biomes encodés réellement, pas seulement une couleur de sol.
- Import dans un serveur de construction isolé puis sauvegarde native Minecraft 26.3 ; vérifier import, biomes, entrée, chemins et rendu.
- Captures réelles si le client est disponible. Toute vue géométrique externe sera identifiée comme telle.
- Config de cette carte : réduire le rayon de sécurité de 200 à environ 60 blocs, adapter la progression radiale de 2500 à 400, villages protégés. Préserver les valeurs par défaut des autres cartes.

## Composition

1. Sylveronce / Brumevert (ouest et sud-ouest) : prairies vallonnées, chênes anciens, saules et marais ; village sylvestre, hameau sur pilotis, arbre-sanctuaire et cercle druidique.
2. Val des Cryptes (sud) : lande brumeuse, ravins, arbres morts, ruines ; bourg des Veilleurs, abbaye gothique, cimetière et ossuaire explorable.
3. Hauts de Boréalis (nord-ouest) : pics, glacier et lac gelé ; village de charpentiers nordiques, citadelle de glace, pont de pierre et mine.
4. Dunes d'Ignis (nord-est) : dunes, mesas stratifiées, oasis et caldeira ; caravansérail, temple solaire et forge de lave.
5. Éclats d'Astréon (est/sud-est) : falaises sombres, cristaux, îlots suspendus ; refuge astral, observatoire monumental, passerelles et archipel accessible.

La place centrale d'Aéloria appartient à la première région. Cinq routes la relient aux villages, une route secondaire forme une boucle d'exploration. Les monuments et les camps sauvages sont en dehors des zones sûres.
