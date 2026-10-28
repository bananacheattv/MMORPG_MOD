# Reprise Aeloria — travail en cours, 28 octobre 2026

L’utilisateur a demandé l’arrêt du développement et le push de l’état actuel pour reprendre avec Claude. Cette branche est un instantané de travail : ne pas la considérer comme prête à publier. Ne pas fusionner ni déployer avant correction et validation.

## État livré auparavant

Production et main : commit76d4d4a, version41-76d4d4a. Le serveur a été redémarré après déconnexion de l’utilisateur ; six ports contrôlés OK, cinq JAR identiques, zéro erreur serveur. Launcher séparé gfrpg-launcher version1.3.7. Les changements suivants sont uniquement dans cette branche.

## Demandes et modifications en cours

- Cristal trop rapide et montant sur les rochers : CrystalRoute génère un circuit A* autour de leurs colonnes, vitesse0.035bloc/tick=0.7bloc/seconde, hauteur101.15 avec léger flottement. Waypoints ±32 évitent les marquages àx29. Test12000pas vitesse/hauteur/collision validé.
- Bilans absents au retour : PvpProgression conserve le dernier bilan et son accusé séparément ; entraînements et sorties anticipées ont un bilan sans points. ArenaResultDelivery attend que le monde soit visible. /arene bilan rouvre le dernier bilan.
- Roulette des coffres remontée ày3.3, nom4.6 et gain2.55.
- Inventaire : zone équipement/inventaire élargie46%, attributs réduits38%du reste, grille centrée via gridOffset. CharacterSlotHighlightMixin adapte le survol aux cases. Identity hauteur34 pour ne pas être cachée à1280x720. L’utilisateur demande une grille centrée ; vérifier visuellement que cela correspond à son intention.
- Revenir au jeu déplacé sous le logo dans la colonne centrale.
- Niveau100 : assez de points pour les onze compétences au rang maximum actuel5, confirmé explicitement par l’utilisateur. Budget44points calculé par classe ; pas de rang100.
- Icônes44 compétences refaites : symbole plus contrasté et pastille d’effet. HUD buffs/débuffs avec icône, durée, texte3mots maximum, ancrage sous cadre joueur. Effets vanilla inclus.
- Wiki17pages docs/wiki/README.md, catalogues44skills et44armes PvE générés par tools/gen-wiki-catalog.py ; liens relatifs validés.
- Armes : l’utilisateur jugeait textures/formes trop semblables. tools/sculpt-rpg-assets.py produit64atlas individuels512 et silhouettes nettement différentes par famille/palier (épée/sabre/couperet/lame à éclats, croissant/trident/orrery, arcs sylvestres/ailés, masses sacrées). Garder un style MMORPG et vérifier la qualité avec lui.
- SpawnDecorProtection interdit interactions/attaques des nonOP sur cadres et stands du spawn. SpawnDecorDamageMixin protège ArmorStand et ItemFrame contre dégâts horsOP, y compris environnement. Les OP doivent pouvoir éditer.
- Bouclier dernière demande : perte uniquement sur coup bloqué25/20/15/10/2 aux niveaux1/25/50/75/100 ; aucun coût en maintenant ; à0 récupération complète100avant nouvelle parade ; jauge sans texte. Implémenté côté serveur avec shieldGuardRecovering persisté, regen10/sec abaissé après2sec. Décalage des paliers25/50/75 corrigé via level/25 dans ShieldGuard et PaladinOffhand.

## Problèmes encore ouverts — PRIORITAIRES

1. ARC : l’utilisateur dit toujours inversé et demande explicitement la référence vanilla. Ne pas déclarer corrigé. La flèche encoche était orientée vers -X derrière la poignée : désormais pointe+X, mais orientation finale non validée. Les transformations vanilla ont été lues dans le JAR sources : thirdperson right[-80,260,-40],left[-80,-280,40],firstperson right[0,-90,25],left[0,90,-25]. Le modèle vanilla est une image diagonale, notre géométrie est verticale : adapter la géométrie et les transformations ensemble. Référence extraite localement .tools/vanilla-bow.png dans le workspace parent.
2. BOUCLIER première personne : l’utilisateur précise qu’il est trop haut et doit venir horizontalement devant le joueur. Dernière translation ajoutée Y+.5 dans CastAnimations est trop forte. CAUSE IDENTIFIÉE JUSTE AVANT ARRÊT : PaladinShieldItem hérite de Item au lieu de ShieldItem ; FirstPersonHandsAndItemsRenderer case BLOCK applique alors translate(invert*-.14142136,.08,.14142136), puis rotations X-102.25,Yinvert*13.365,Zinvert*78.05. Un véritable ShieldItem évite ces rotations. Corriger le type ou gérer une animation adaptée, préserver la parade serveur sans double réduction des dégâts vanilla. Cette correction n’a PAS été faite.
3. Modèle sculpted_baton_archimage : nouvelle pièce àtoY32.4 hors limites JSON[-16,32]. Le client rejette ce modèle. Corriger et auditer TOUS les éléments générés pour ces bornes ; régénérer.
4. Dernier essai natif échoué : compilation lancée en parallèle du client a temporairement retiré ClientNet.class, provoquant ClassNotFoundException. Ne pas compiler pendant un client de QA. Relancer proprement après corrections.
5. Vérifier les cadres également par test d’interaction et dégâts, pas seulement les stands.

## Validation déjà effectuée

arena-fixes-network5.log dans workspace parent : FULLPASS, incluant parcours réel12000pas, bilans/ACK/replay/training, budget compétences quatreclasses, coûts/récupération bouclier, protection réelle stand nonOP/accèsOP. Une sortie Gradle SUCCESS seule ne suffit pas : les erreurs dans événements serveur peuvent quand même sortir0. Chercher NETWORK_FORGE PASS et PVP_PASS, et absence AssertionError/Exception caught.

Captures natives dans run/screenshots/showcase_* : inventory_large_two_rows, menus_two_rows, pause_return_centered, buffs_debuffs_timers, bow_aim_first_person/third_person, shield_guard_draining, weapons_refined_again. Attention : captures de passages différents ; le dernier passage n’a pas terminé.

QA : gradlew runServer -PnetworkChecks ; natif serveur gradlew runServer -PvisualChecks -PguideVisual, client JAVA_TOOL_OPTIONS=-Dneoforge.disableBlaze3DValidation=true puis gradlew runClient -PmpServer=127.0.0.1:25590 -Pusername=VisualChecks -Pshowcase=pvp_polish. DevPvpVisual contrôle keyUse held pour vraie visée435..490 et bouclier575..630. Il restaure CharacterLayout normalement àtick930 ; un crash peut interrompre cette restauration dans le seul client de QA.

run/config/neoforge-client.toml a showLoadWarnings=false pour QA. Restaurer depuis le fichier parent .tools/neoforge-client-before-preview.toml après validation. Ne pas réessayer l’automatisation native du bureau refusée ; QA par captures Minecraft intégrées.

## Contraintes à préserver

L’utilisateur a validé le bâton tenu VERS L’AVANT en troisième personne : conserver rotations[0,-90,0] droite/[0,90,0] gauche ; ne pas le redresser verticalement. Créatif uniquement vraisOP, pas grade affiché. Ne pas arrêter la production si un joueur est connecté. Aucun nouvel arrêt ou déploiement autorisé par la seule demande de push. Le wiki premium indique achats argent réel désactivés tant que site pas prêt.

Le suivi historique complet est dans TRAVAIL_EN_COURS.md du workspace parent. Les scripts locaux manage-aeloria.cjs, deploy-aeloria-0-0.cjs et check-aeloria.cjs s’y trouvent ; ce sont des outils de production, ne pas les exécuter sans nécessité et respect des joueurs connectés.
