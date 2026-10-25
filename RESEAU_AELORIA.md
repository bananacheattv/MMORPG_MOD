# Réseau Aeloria sur un même PC

Java 25 et NeoForge 26.3.0.39-beta. Installer **le même nouveau JAR sur les quatre serveurs et sur les clients**. Les nouveaux objets sont des entrées de registre : un ancien client doit être mis à jour.

## Rôles et stockage partagé

Le fonctionnement solo habituel reste actif tant que `config/mmorpg/network.json` contient `"enabled": false`. Pour un réseau local, utiliser un fichier par serveur :

```json
{
  "enabled": true,
  "role": "HUB",
  "sharedDirectory": "C:/Users/Grand/Desktop/serveur manager/shared/players",
  "hub": "hub",
  "main": "serveur",
  "mines": "mines",
  "plains": "plaines"
}
```

| Dossier | Rôle | Port local | Contenu construit automatiquement |
| --- | --- | --- | --- |
| hub | HUB | 25571 | Citadelle, essais des classes et spawn |
| serveur | MAIN | 25570 | Spawn et instances de donjons |
| mines | MINES | 25572 | Mines uniquement |
| plaines | PLAINS | 25573 | Plaines uniquement |

Les quatre fichiers doivent désigner **exactement le même dossier partagé sur le disque local**. Ce mécanisme utilise les verrous du système d'exploitation ; il ne constitue pas une configuration multi-machine ou un stockage MariaDB. MariaDB n'est pas nécessaire à ce système.

Les profils transportent données RPG, classe, XP, or, recettes apprises, quêtes, familiers/montures possédés, inventaire, équipement, coffre de l'Ender, banque et contenu de ses trois pages. Les délais de compétences et effets temporaires sont recalés sur l'horloge de destination. Les positions et les permissions restent propres à chaque monde. Les groupes, les annonces de l'hôtel des ventes, les zones et les téléporteurs personnels restent propres au serveur ; utiliser le MAIN pour le commerce et reformer son groupe sur le serveur de donjon.

À la première connexion au réseau, en l'absence de profil partagé, **le personnage local du serveur d'arrivée est importé**. Pour un personnage existant réparti sur plusieurs anciens mondes, sauvegarder les mondes et choisir le serveur qui détient sa progression à conserver avant cette première connexion. Ne jamais fusionner les inventaires de copies anciennes.

Chaque joueur connecté conserve un verrou exclusif jusqu'à sa déconnexion. Un serveur de destination attend au maximum deux secondes que la source libère le verrou ; sinon il refuse la connexion. Un profil invalide est refusé et n'est jamais remplacé par un personnage vide. Sauvegarde toutes les 30 secondes, avant le transfert et à la déconnexion ; écriture atomique avec copie précédente `.dat.bak`. Un arrêt brutal peut perdre les changements depuis la dernière sauvegarde. Sauvegarder également `shared/players` avec les mondes. Pour une restauration manuelle, arrêter les serveurs avant de remettre une copie saine.

## Velocity et déplacements

Utiliser une version de Velocity qui prend en charge Minecraft 26.3 : l'installation locale a été mise à jour vers **Velocity 4.3.0, build 44** depuis l'API officielle PaperMC, avec vérification SHA-256. L'ancien Velocity 3.5.1 annonçait seulement la prise en charge jusqu'à 26.2 (protocole 776), alors que les backends 26.3 utilisent le protocole 777.

Velocity : `online-mode = true`, `bungee-plugin-message-channel = true`, entrée sur `hub` et correspondance des quatre noms de serveur. Les backends restent en `online-mode=false` et liés à `127.0.0.1`. Seul le proxy est exposé aux joueurs.

La configuration existante conserve `player-info-forwarding-mode = "none"` : l'authentification officielle est faite par Velocity, tandis que les backends conservent leurs UUID hors ligne existants. Ce changement ne prétend pas installer le forwarding moderne ni rétablir les skins officiels. Pour activer ultérieurement le forwarding moderne, installer une intégration compatible avec cette version de NeoForge, configurer le secret et migrer explicitement les UUID des profils et des mondes.

Les portails hub/spawn, les choix du maître des mines/plaines et l'entrée de donjon dirigent vers le serveur qui héberge le contenu. Une instruction d'arrivée sauvegardée avec le personnage conserve la région ou le donjon choisi. Le protocole de connexion est le canal standard `bungeecord:main` de Velocity. Si le proxy refuse la destination, le joueur reste à la source ; le verrou ne lui est pas retiré. Un délai de dix secondes permet de réessayer. Les transferts sont refusés pendant un essai, une cinématique ou un combat PvP.

Commandes supplémentaires : `/reseau hub`, `/reseau spawn`, `/reseau mines`, `/reseau plaines`. Les commandes et portails habituels restent disponibles. Les nouveaux personnages doivent d'abord choisir leur classe au hub.

## Forge, parchemins et départ

- Première arrivée : pioche, hache, pelle et houe du débutant, ainsi que les graines de Lueur.
- Premier choix de classe : arme et quatre pièces d'armure de niveau 1, potions et provisions habituelles. Le kit ne peut pas être réclamé deux fois.
- Armes et armures de départ restent fabricables. Tous les outils avancés et les autres armes/armures demandent un parchemin appris.
- Cinq parchemins : I (niveaux 1–20), II (21–40), III (41–60), IV (61–80), V (81–100). Chaque parchemin débloque les trois catégories de son palier, sans débloquer les autres paliers ni supprimer les exigences de niveau et de matériaux.
- Clic droit pour apprendre ; l'objet est consommé seulement si le palier est nouveau. Un doublon reste échangeable. L'apprentissage est sauvegardé et synchronisé entre serveurs.
- Chaque boss remet un parchemin de son palier à chaque bénéficiaire admissible de la récompense. Les essais de classe ne donnent pas de parchemins.
- Les cinq quêtes « Le savoir du forgeron » donnent les mêmes objets ; elles sont ajoutées aux configurations existantes sans remplacer les quêtes personnalisées. Les objectifs portent sur des monstres du palier et les quêtes se suivent.
- Toutes les recettes de la forge emploient les ressources du mod. La fiole runique se fabrique avec de la Verdite et de la poussière d'âme. Les ingrédients identiques sont additionnés avant le contrôle serveur.
- La catégorie et les cinq recettes d'invocation ont été retirées de la forge. Les objets d'invocation restent disponibles aux autres systèmes existants.
- Les montures utilisent les effets d'Aeloria et les sons de pas du sol. Les effets hérités des chevaux, y compris leurs variantes bébé, sont remplacés.

## Vérification reproductible

Dans un checkout de développement, créer `run/eula.txt` après avoir accepté l'EULA Minecraft et configurer un port de test local libre dans `run/server.properties`. Exécuter :

```powershell
.\gradlew.bat runServer -PnetworkChecks build --console=plain
```

La suite utilise un faux joueur dans le monde de développement, puis arrête le serveur. **Ne jamais activer `networkChecks` sur un monde de production.** Le résultat attendu est `[CODEX CHECKS] NETWORK_FORGE PASS` et `build/reports/network-checks.txt` contenant `PASS`. Vérifie verrous et sauvegardes, format du paquet proxy, recettes, quatre kits de classe, parchemins et doublons, refus/fabrication côté serveur, récompenses de boss, sauvegarde partagée de l'inventaire et de la banque, refus des profils incomplets et transmission d'une session. Une connexion réelle avec un compte officiel reste nécessaire pour vérifier de bout en bout le changement de serveur via Velocity.
