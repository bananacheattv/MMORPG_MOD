# Reprise Eldoria - 4 octobre 2026

Branche : codex/eldoria-reprise. Sauvegarder chaque fonctionnalite dans un commit distinct et pousser sur GitHub avant la suivante.

## Etat

- Potions : correctif implemente (PV 150/600/1500 ; mana 100/350/900), plafonne aux maxima existants, aucun objet consomme si la jauge est pleine. Verification statique effectuee. Compilation Gradle reussie sous Java 25 : utiliser JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/Users/Utilisateur/Documents/GitHub/MMORPG_MOD/build dans cet environnement Windows. Java 25 installe dans C:/Program Files/Java/jdk-25.0.4. Aucun test en jeu effectue.
- Familiers adaptes a la classe : bonus offensifs et attributs adaptes au proprietaire, avec les memes valeurs dans le calcul serveur et dans le menu. Compilation reussie ; verification en jeu restante.
- Nourriture et bonheur : bonheur persistant par familier, baisse de 1/minute invoque, nourriture fabricable (+25), bonus de 50 a 100 %, jauge dans le menu. Compilation reussie ; essai en jeu restant.
- Charmes : XP et probabilites de butin +25 %, 30 minutes de jeu, timers sauvegardes, non cumulables, recettes de Forge.
- Menace / aggro : degats cumules par joueur, multiplicateur x2 pour le tank, seuil de changement de cible de 10 %, provocation prioritaire pendant sa duree, nettoyage hors portee/deconnexion/mort et respect de la base des boss. Compilation reussie ; validation multijoueur en jeu restante.
- Bestiaire : filtres Monstres/Boss puis familles, compteurs et tri par niveau ; famille configurable dans mobs.json, compatible avec les anciens fichiers. Compilation reussie ; essai visuel en jeu restant.
- Inventaire : artisanat 2x2 et seconde main bloques en survie (menus, serveur, raccourci F), objets existants restitues ; mode creatif conserve. Tests executes dans une copie du monde : recette impossible, restitution sans perte, slots bloques, raccourcis refuses et retour creatif reussis. Capture visuelle verifiee.
- Competences : 16 nouvelles actives (44 competences au total), paliers 10/40/65/90 en plus des paliers existants, six emplacements conserves, liste avec defilement et pagination. Compilation et test automatique des debloquages 1-100 reussis ; ecrans verifies en jeu. Equilibrage et chaque effet de combat a tester en multijoueur.
- Icones : 44 visuels uniques, cadres par classe, marqueur passif et symboles secondaires (soin, poison, ralentissement, traction, protection...). Verification des fichiers 32x32 et planche visuelle effectuees.
- Images des familiers : modeles 3D reels du jeu dans la collection et la fiche, apercus animes et noms au survol. Demarrage du jeu et capture des huit familiers verifies.
- Cosmetiques : rendu local raccorde aux donnees privees synchronisees, reprise des debloquages au login, bouton de vue en jeu et option premiere personne dans la garde-robe. Test equipement/synchronisation reussi ; auras/ailes visibles en jeu sous forme de particules, comme dans le systeme initial.
- Monstres : Zombie des Cryptes (8-22), Araignee Venimeuse (18-35), Bandit Arbaletrier (30-50), IA native melee/poison/tir, apparitions et butins configures, oeufs et bestiaire raccordes. Compilation et apparition/rendu des trois types verifies en jeu. Sauvegarde : 9abd4d3.
- Equipement : cinq sets par classe dont quatre nouveaux mythiques de niveau 100 (Valkyrie, Eternel des Arcanes, Sentinelle Astrale, Egide Divine), 16 pieces avec textures portees, alliage celeste et recettes. Epee du Berserker ajoutee au niveau 25 ; les six armes existantes par classe sont conservees. 149 recettes resolues verifiees en jeu et apercus des quatre sets captures.
- Les 4 autres demandes restent a implementer/verifier.
- Aucun code des agents cloud de Claude n'a ete recupere. Base initiale : f8c07de.

## Demandes utilisateur

1. Competences : plus de skills, unlock 1-100, 6 a equiper.
2. Familier adapte a la classe du joueur.
3. Faire fonctionner les cosmetiques.
4. Bestiaire avec categories et sous-categories.
5. Plus de types de mobs.
6. Bonnes images dans l'onglet familiers.
7. Bloquer artisanat inventaire et deuxieme main.
8. Revoir les icones de competences.
9. Systeme de quetes revu et creation de PNJ de quetes.
10. Equipement : materiaux, 5 armes et 5 sets par classe, epee guerrier, sets mythiques.
11. Charmes d'experience et de chance.
12. Menace / aggro des mobs.
13. Potions : soin plat PV/mana.
14. Nourriture et bonheur des familiers.
15. Caisses + cles, lucky blocks.
16. TAB et scoreboard custom.
17. Montures deblocables.