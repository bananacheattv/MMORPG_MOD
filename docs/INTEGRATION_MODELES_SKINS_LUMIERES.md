# Intégration des modèles, skins et lumières — 7 novembre 2026

## Modèles livrés par Claude

La livraison du commit `f7ccfb65` était publiée dans `art/refonte_3d/livraison`, mais absente des ressources chargées par Minecraft. Les 673 fichiers de `livraison/src` sont intégrés aux mêmes chemins dans `src`, et les 252 sources éditables sont copiées dans `bbmodels`. Les 121 entrées de collision livrées sont fusionnées dans `data/mmorpg/decor_shapes.json`, sans supprimer les autres entrées.

Cela comprend les 22 montures, 123 décors, blocs Java et textures, 49 mobs, cinq boss, dix gardiens d'Yggdrasil, les états du cristal et le drone. Les géométries, textures, clips, identifiants et réglages de gameplay livrés sont conservés. Les sources de livraison restent présentes ; les versions précédentes des ressources sont conservées dans l'historique Git au commit ci-dessus. Les prototypes de l'autre branche artistique ne sont pas inclus.

Le sceau du Drake ascendant reçoit les deux références de modèle d'objet qui lui manquaient. Il réutilise l'icône du sceau de monture mythique volante.

Les clips `run` et `jump` fournis restent disponibles dans les sources mais ne sont pas raccordés à de nouveaux états de gameplay dans ce correctif.

## Skins

Le skin choisi est fourni par `AbstractClientPlayer.getSkin`, avant la sélection du modèle et du renderer. Les bras fins, le rendu en vue subjective et les aperçus utilisent ainsi le même skin. Le skin du compte reste utilisé pendant le téléchargement et après `/skin reset` ; les capes du compte sont conservées.

Les requêtes simultanées sont regroupées. Un échec autorise une nouvelle tentative après 30 secondes ; un skin chargé est réactualisé après dix minutes sans disparaître pendant le téléchargement. Les erreurs sont signalées au joueur. Le téléchargement PNG est limité à 1 Mo, avec délais réseau et vérification des dimensions, puis confié au décodeur de Minecraft, qui prend aussi en charge le format historique 64×32. Les profils par pseudo conservent leur variante slim/wide.

## Lumières

Le réglage « 100 » désigne une portée de 100 blocs. L'ancien calcul de `LightNetwork.step` écrasait le niveau des relais avec celui de la dernière source, et supprimait les relais hors de sa sphère même s'ils appartenaient aussi à une autre source. Les sources à portée 100 pouvaient ainsi s'éteindre mutuellement.

Chaque cellule est maintenant recalculée avec les sources qui contribuent réellement à sa grille : niveau maximum, distance et obstacles respectés. Éteindre, réduire ou retirer une source conserve les contributions voisines. Une source temporairement déchargée ne provoque pas l'effacement de ses relais. Le budget de travail tient compte du nombre de sources et le marquage de reconstruction n'est plus perdu quand un travail est déjà en cours.

## Vérifications réalisées

- `gradlew build` : compilation et 55 tests unitaires, dont l'intégrité des pistes d'animation et les reprises/rafraîchissements du cache des skins.
- Serveur NeoForge 26.3 isolé, `-PlightChecks` : reproduction avec le code original, qui échoue avec deux sources à portée 100 ; correction validée sur monde neuf. Vérifications de superposition, intensité différente, réduction de portée, extinction, suppression, obstacles et nettoyage de la dernière source.
- Client Minecraft 26.3 / NeoForge 26.3.0.39-beta, `-Pshowcase=integration_checks` : commandes `/skin` réelles, synchronisation publique, skin rendu, pseudo slim via serveur HTTP de test, PNG 64×64 et 64×32, HTTP 503 suivi d'une reprise automatique, puis reset. Cinq étapes validées.
- Même client : chargement des renderers et affichage natif animé des 22 montures sur quatre pages. Captures dans `build/qa-client/screenshots` pendant le contrôle. Les classes de vérification sont désactivées sans option de développement.

Les essais réseau des skins utilisent des réponses HTTP locales contrôlées ; ils ne garantissent pas la disponibilité de Mojang ou d'un hébergeur externe. Le client testé ici utilise OpenGL, sans Iris/Sodium. Les collisions montées en déplacement, tous les sièges et le vol ne font pas partie de cette passe. Les sources et textures livrées n'ont pas été remodélisées.

## Installation

Installer le même JAR sur les six serveurs de jeu et sur les clients, puis redémarrer les serveurs. Fermer Minecraft et relancer le launcher pour récupérer le JAR publié. Les blocs de lumière déjà posés sont retrouvés au chargement de leurs chunks ; leurs relais sont reconstruits progressivement.
