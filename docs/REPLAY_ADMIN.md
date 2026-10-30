# Atelier Replay Aeloria — première version sur serveur

Branche : `feature/admin-replay`, base `0e486f1` de `main`. Code `db825a2` intégré dans `main`, publié et installé le 30 octobre 2026 : **build 87-db825a2**, sur serveur / hub / mines / plaines / donjons. Empreinte du jar publié et vérifié sur les cinq cibles : `e9fa35a9a1cdacdb06f67ad06f07040d445e2f9e81593c65559c3e7e9f12e27b`. Le launcher pointe vers cette même release ; les joueurs doivent mettre leur client à jour et relancer Minecraft.

## Utilisation

1. Installer la même version du mod côté serveur et côté client. Seuls les opérateurs Minecraft ayant la permission `COMMANDS_GAMEMASTER` peuvent utiliser l'atelier ; un grade cosmétique ne donne aucun droit.
2. Aller sur le lieu du tournage et exécuter `/replay record`, ou `/replay` puis **Enregistrer**. Jouer la scène normalement. Les prises suivent les acteurs vivants visibles dans un rayon de 96 blocs, à 20 échantillons/s, jusqu'à 3 minutes et 128 acteurs.
3. Appuyer sur **F8** pour ouvrir l'atelier, puis **Arrêter / quitter**, ou `/replay stop`. La prise est sauvegardée localement dans `<dossier du jeu>/aeloria-replays/*.aereplay`.
4. Revenir près du tournage, sur le même serveur et dans la même dimension, sélectionner la prise, puis **Revoir la prise**. L'OP passe temporairement en spectateur. Les acteurs de relecture sont locaux et ne remplacent jamais les entités du serveur.
5. Utiliser pause, curseur, ±5 s et vitesse ×0,25 / ×0,5 / ×1 / ×2. Fermer l'atelier pour voler avec les commandes spectateur habituelles ; **F8** le rouvre.
6. Placer le curseur à un instant, cadrer, puis **Point caméra**. Répéter aux instants suivants ; **Trajectoire : oui** anime la caméra entre ces points. **Sauver les points** les conserve dans la prise. Le FOV des points est celui réglé dans les options au moment de leur création.
7. Choisir 16:9, 9:16 ou carré, puis **Exporter les 10 prochaines secondes — MP4 / 60 fps**. Si la prise se termine avant, seul le reste est exporté. Les clips partent de la position actuelle du curseur et respectent la vitesse choisie.
8. Le fichier final est `<dossier du jeu>/aeloria-replays/exports/<date numérique>/clip.mp4`. On peut aussi choisir l'export PNG pour un montage plus poussé.
9. **Arrêter / quitter** restaure le mode de jeu, la position et l'orientation d'avant la lecture. Si l'OP a été retiré entre-temps, un ancien mode créatif/spectateur revient en survie. Un marqueur persistant permet également de récupérer cet état après une reconnexion interrompue.

## Export MP4

L'encodage utilise FFmpeg installé sur la machine du vidéaste, jamais sur les serveurs Minecraft. Le mod ne télécharge et ne redistribue aucun exécutable.

Recherche dans cet ordre : propriété Java `aeloria.replay.ffmpeg`, chemin enregistré via **Configurer FFmpeg**, fichier local `aeloria-replays/ffmpeg.exe` (ou `ffmpeg` sur Linux/macOS), puis `ffmpeg` dans le PATH.

Le bouton de configuration écrit uniquement `aeloria-replays/ffmpeg-path.txt`. Choisir le chemin de l'exécutable réel, sans ajouter d'arguments. Les processus sont lancés avec `ProcessBuilder` et une liste d'arguments ; aucune chaîne n'est exécutée dans un shell.

Le rendu écrit chaque PNG avant d'avancer la scène, puis encode en H.264, 60 images/s, CRF 18, YUV420p, MP4 faststart. Le recadrage central est réellement appliqué aux images pour le vertical/carré. La résolution est limitée à celle de la fenêtre de rendu ; aucune promesse de 1080×1920 depuis une petite fenêtre. Les dimensions impaires sont complétées d'un pixel lors de l'encodage.

Les PNG restent disponibles si FFmpeg échoue. Le journal est `encoding.log`. Une estimation prudente vérifie l'espace disque avant la capture. Le délai d'encodage maximal est cinq minutes. Les clips de dix secondes limitent les volumes intermédiaires ; monter plusieurs clips pour une vidéo longue.

## Périmètre réel

- Replay visuel sur le **décor actuel chargé**, pas une sauvegarde des blocs, du monde ou de ses anciennes versions. Rester à proximité du lieu pour garder les chunks chargés.
- Joueurs, mobs et porte-armures : positions/orientations interpolées, équipement, données synchronisées d'entité, marche, animation de frappe standard, blessure et mort.
- Les skins des joueurs utilisent les informations encore disponibles dans la connexion courante ; ils ne sont pas archivés comme un pack de skins indépendant.
- Les projectiles, objets au sol, particules, sons et certaines animations propres aux systèmes Aeloria ne sont pas encore archivés. Le MP4 est **muet**. Ajouter la musique/les sons au montage. Ne pas présenter cette version comme une reproduction intégrale des sorts ou des combats.
- Les autres joueurs continuent leur partie ; leur combat n'est ni ralenti ni modifié. Les prises n'enregistrent que ce que le client a reçu, pas les acteurs dans des chunks déchargés.
- La lecture nécessite une autorisation serveur renouvelée ; révocation OP, déconnexion, dimension différente ou fin de session interrompent l'atelier. La session spectateur est limitée à trente minutes.
- Le verrou OP protège cet outil et ses commandes. Il ne peut pas empêcher un joueur d'utiliser un logiciel externe d'enregistrement d'écran.

## Intégration pour Claude

Les nouvelles classes sont dans `replay/` et `client/replay/`. Les seuls fichiers existants modifiés sont les traductions du raccourci. Les enregistrements réseau et événements sont indépendants de `Net` et du menu général afin de réduire les conflits.

Récupérer le commit de cette branche sur la branche actuelle de Claude. Ne pas déployer directement une ancienne base de cette branche si `main` a avancé ; rebaser/cherry-pick et compiler d'abord. Ne pas déployer un serveur avec les propriétés de test.

## Vérifications reproductibles

- Tests unitaires : `gradlew test --tests "com.mmorpg.replay.*"` : aller-retour des fichiers, écriture atomique, interpolation à ±180°, fichiers tronqués, références/valeurs invalides, limites de collections et encodage réel lorsque FFmpeg est présent dans `run/aeloria-replays/ffmpeg.exe`.
- Serveur isolé : propriété Java `mmorpg.replayChecks=true`, jamais en production. `DevReplayChecks` vérifie les permissions, le refus des paquets forgés, la récupération et le mode de jeu. Le test utilise un FakePlayer, dont les téléportations réseau sont intentionnellement inactives ; le retour de position doit également être contrôlé avec un vrai joueur.
- Client isolé : propriété Java `mmorpg.replayClientChecks=true` des deux côtés et pseudo `ReplayChecks`. `DevReplayClientChecks` enregistre, sauvegarde, recharge, lance la lecture et l'export MP4, puis quitte. La propriété autorise un joueur de test dans le serveur de développement : ne jamais la mettre dans le launcher public.
- Rapports locaux : `build/reports/replay-server-checks.txt`, `build/reports/replay-client-checks.txt` et rapports JUnit. Une compilation seule ne valide ni le cadrage ni la restitution visuelle d'un boss.

Avant publication : tester un combat complet, les armures/modèles custom, le bouclier, une révocation OP pendant lecture, une reconnexion, les trois cadrages et une petite fenêtre. Les limitations ci-dessus restent explicites tant que ces fonctionnalités ne sont pas prises en charge.

## Validation effectuée le 30 octobre 2026

- `gradlew build` réussi ; 42 tests unitaires passent, dont huit tests du replay/encodeur, aucun ignoré dans cet environnement.
- Contrôle dédié : refus non-OP/paquet forgé, accès OP, spectateur temporaire, marqueur de récupération, restauration du mode et repli en survie après révocation : PASS.
- Vrai client Minecraft 26.3 / NeoForge 26.3.0.39-beta, fenêtre 960×600 : enregistrement, sauvegarde/rechargement, acteurs locaux, curseur, trajectoire, export MP4, nettoyage après retrait OP, retour à la position et mode survie : PASS.
- Export vertical de contrôle : H.264 YUV420p, 338×600, 87 images, 1,45 seconde à 60 images/s. Décodage complet FFmpeg réussi. Première image vérifiée : pas de notification Minecraft incrustée.
- Interface divisée en page Caméra et page Prises pour éviter le chevauchement constaté sur petite fenêtre.
- Aucun test complet de restitution de tous les boss/sorts, ni promesse de prise en charge des éléments explicitement exclus ci-dessus. Après ces essais, déploiement du build 87 sur les cinq serveurs, tous en ligne ; commande `help replay` vérifiée sur le principal.
