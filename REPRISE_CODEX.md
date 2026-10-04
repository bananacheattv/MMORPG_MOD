# Reprise Eldoria - 4 octobre 2026

Branche : codex/eldoria-reprise. Sauvegarder chaque fonctionnalite dans un commit distinct et pousser sur GitHub avant la suivante.

## Etat

- Potions : correctif implemente (PV 150/600/1500 ; mana 100/350/900), plafonne aux maxima existants, aucun objet consomme si la jauge est pleine. Verification statique effectuee. Compilation NON VALIDEE : Gradle echoue avant compilation avec Unable to establish loopback connection / UnixDomainSockets Invalid argument: connect, sous Java 21 et Java 25. Java 25 installe dans C:/Program Files/Java/jdk-25.0.4. Aucun test en jeu effectue.
- Les 16 autres demandes restent a implementer/verifier.
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