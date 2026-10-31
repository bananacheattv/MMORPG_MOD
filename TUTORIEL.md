# Tutoriel de la Mascotte

Le joueur ne reçoit **aucun objet** au départ (ni outils, ni arme, ni armure) : tout passe par les quêtes du groupe `tutoriel`, proposées par la **Mascotte** (pet dragon Dragonnet, 1,4× la taille d’un familier) qui parle dans une bulle au-dessus de sa tête et dans le chat.

- **Position** : `SpawnDiscovery.installMascot` la pose à l'arrivée du spawn + (5, 0, −2) (est +5, sud −2, comme les autres décalages de `QuestNpcPlan`), sur le sol le plus proche. Entité `mascotte` (`MascotEntity`, UUID fixe `QuestNpcPlan.MASCOT_ID`).
- **Quêtes** : `DefaultQuests.tutorial()` — 35 quêtes chaînées (`tuto_bienvenue` … `tuto_fin`). Partie 1 : forge, pioche, houe, minage, presse, première arme, première armure (matériaux remis par les quêtes, jetons `@mat_arme` / `@mat_armure` selon la classe). Partie 2 : une quête par fonctionnalité (compétences, bestiaire, métiers, pêche, ferme, marchand, banque, HDV, téléporteurs, groupe, donjons, guilde, arène, échange, succès, hebdo, familiers, montures, cosmétiques, sac, boutique, wiki).
- **Objectif `action`** : cible `cle|libellé`. Clés : `screen:<NomEcran>` (interface ouverte, envoyée par `TutorialScreens`), `craft:<objet>`, `stat:<compteur>` (compteurs de succès), `job:<métier>`. Les jetons `@arme`, `@casque`, `@plastron`, `@jambieres`, `@bottes` désignent la pièce de la classe (`TutorialKit`).
- **Recettes novices** : première arme et première armure de chaque classe, ainsi que `pioche_debutant` / `houe_debutant`, se fabriquent sans minerai compressé (`ForgeRecipes.NOVICE`).
- **Anciens joueurs** : au premier chargement après la mise à jour, ils ont tout le tutoriel validé (`MascotGuide.skipTutorial`).
- **La chaîne de découverte du spawn** (Maëlys) et la campagne démarrent après `tuto_fin`.
- **Test** : `tools/screens/mp.sh` (cherche `TUTORIAL PASS` dans `/tmp/mp-server.log`) ou `runServer -PtutorialChecks` avec un joueur.
