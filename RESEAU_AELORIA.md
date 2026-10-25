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
  "plains": "plaines",
  "dungeons": "donjons",
  "dungeonMaxInstancesPerPool": 8,
  "dungeonBuildBudgetMs": 8
}
```

| Dossier | Rôle | Port local | Contenu construit automatiquement |
| --- | --- | --- | --- |
| hub | HUB | 25571 | Citadelle, essais des classes et spawn |
| serveur | MAIN | 25570 | Spawn, commerces et quêtes |
| donjons | DUNGEONS | 25574 | Accueil sécurisé et instances de donjons |
| mines | MINES | 25572 | Mines uniquement |
| plaines | PLAINS | 25573 | Plaines uniquement |

Les cinq fichiers doivent désigner **exactement le même dossier partagé sur le disque local**. Ce mécanisme utilise les verrous du système d'exploitation ; il ne constitue pas une configuration multi-machine ou un stockage MariaDB. MariaDB n'est pas nécessaire à ce système.

Les profils transportent données RPG, classe, XP, or, recettes apprises, quêtes, familiers/montures possédés, inventaire, équipement, coffre de l'Ender, banque et contenu de ses trois pages. Les délais de compétences et effets temporaires sont recalés sur l'horloge de destination. Les positions ordinaires et les permissions restent propres à chaque monde. La composition des groupes est maintenant commune au réseau et persiste dans `shared/parties.json`, avec une copie `.bak` et un verrou système pour chaque modification. Un départ, changement de chef ou exclusion relit l'état commun avant de l'enregistrer : une ancienne copie de serveur ne peut pas ressusciter un membre qui a quitté le groupe. Les invitations et le chat restent locaux ; les annonces de l'hôtel des ventes, les zones et les téléporteurs personnels aussi. Utiliser le MAIN pour le commerce.

À la première connexion au réseau, en l'absence de profil partagé, **le personnage local du serveur d'arrivée est importé**. Pour un personnage existant réparti sur plusieurs anciens mondes, sauvegarder les mondes et choisir le serveur qui détient sa progression à conserver avant cette première connexion. Ne jamais fusionner les inventaires de copies anciennes.

Chaque joueur connecté conserve un verrou exclusif jusqu'à sa déconnexion. Un serveur de destination attend au maximum deux secondes que la source libère le verrou ; sinon il refuse la connexion. Un profil invalide est refusé et n'est jamais remplacé par un personnage vide. Sauvegarde toutes les 30 secondes, avant le transfert et à la déconnexion ; écriture atomique avec copie précédente `.dat.bak`. Un arrêt brutal peut perdre les changements depuis la dernière sauvegarde. Sauvegarder également `shared/players` avec les mondes. Pour une restauration manuelle, arrêter les serveurs avant de remettre une copie saine.

## Velocity et déplacements

Utiliser une version de Velocity qui prend en charge Minecraft 26.3 : l'installation locale a été mise à jour vers **Velocity 4.3.0, build 44** depuis l'API officielle PaperMC, avec vérification SHA-256. L'ancien Velocity 3.5.1 annonçait seulement la prise en charge jusqu'à 26.2 (protocole 776), alors que les backends 26.3 utilisent le protocole 777.

Velocity : `online-mode = true`, `bungee-plugin-message-channel = true`, entrée sur `hub` et correspondance des quatre noms de serveur. Les backends restent en `online-mode=false` et liés à `127.0.0.1`. Seul le proxy est exposé aux joueurs.

La configuration existante conserve `player-info-forwarding-mode = "none"` : l'authentification officielle est faite par Velocity, tandis que les backends conservent leurs UUID hors ligne existants. Ce changement ne prétend pas installer le forwarding moderne ni rétablir les skins officiels. Pour activer ultérieurement le forwarding moderne, installer une intégration compatible avec cette version de NeoForge, configurer le secret et migrer explicitement les UUID des profils et des mondes.

Les portails hub/spawn, les choix du maître des mines/plaines et l'entrée de donjon dirigent vers le serveur qui héberge le contenu. Une instruction d'arrivée sauvegardée avec le personnage conserve la région ou le donjon choisi. Le protocole de connexion est le canal standard `bungeecord:main` de Velocity. Si le proxy refuse la destination, le joueur reste à la source ; le verrou ne lui est pas retiré. Un délai de dix secondes permet de réessayer. Les transferts sont refusés pendant un essai, une cinématique ou un combat PvP.

Pour un donjon, les membres du groupe connectés sur le même serveur, dans la même dimension et à moins de 24 blocs de l'initiateur partent ensemble. Tous doivent avoir le niveau et être disponibles ; sinon le départ est refusé avant l'envoi des demandes de connexion. Chaque arrivée réserve avec l'UUID du même chef, même si les joueurs arrivent dans un ordre différent. Un membre éloigné peut rejoindre ensuite par le portail du même donjon et de la même difficulté. Les déconnexions et changements de serveur ne dissolvent plus les groupes en mode réseau ; `/groupe quitter` reste explicite.

Chaque personnage emporte un ticket de retour (serveur, dimension, coordonnées et orientation d'entrée) dans son profil partagé. Le Passeur, la sortie de victoire et `/retourdonjon` ramènent à ce point ; après une mort, le retour est demandé après la réapparition. Si le point a été obstrué ou si son sol a disparu, le spawn sert de repli. Une petite salle protégée sur le serveur donjons évite toute chute dans le vide pendant le chargement. Une instance pleine ou une génération interrompue refuse l'entrée et demande un retour au serveur source.

## Installation du serveur de donjons

Arrêter les serveurs de jeu et le proxy, puis exécuter :

```powershell
.\tools\Install-AeloriaNetwork.ps1 -PanelRoot 'C:\Users\Grand\Desktop\serveur manager' -AddDungeons
```

L'installateur sauvegarde les mondes, mods, configurations, données partagées et configuration du proxy. Il crée `servers/donjons`, ajoute le service au panel et l'adresse `127.0.0.1:25574` à Velocity, avec un démarrage avant le serveur principal et le proxy. La nouvelle JVM utilise initialement `-Xms1G -Xmx4G`, une distance de vue de 6 et une simulation de 4. Sa pause à vide est désactivée pour permettre la libération des réservations.

La migration copie les régions de la dimension des donjons et les données nécessaires aux instances, zones et épreuves. Elle conserve l'original sur le MAIN et **ne duplique ni les fichiers joueurs, ni les banques, ni l'hôtel des ventes, ni la ville**. Le MAIN ne crée, n'alloue et ne réinitialise plus d'instances en mode réseau. Recharger la configuration du panel puis démarrer tout ; installer le même JAR sur les cinq backends.

Un seul processus Java héberge plusieurs instances. Par défaut, le nombre est limité à huit par couple donjon/difficulté. Les emplacements libres sont réutilisés ; une réservation abandonnée est libérée après environ trois minutes. La génération et le préchauffage sont répartis entre les groupes avec un budget souple de 8 ms par tick ; une étape déjà commencée peut dépasser ce budget. Ces limites sont configurables dans `network.json` et ne constituent pas une garantie de TPS : CPU, RAM et disque restent partagés sur le PC.

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

La suite utilise de faux joueurs dans le monde de développement, puis arrête le serveur. **Ne jamais activer `networkChecks` sur un monde de production.** Les résultats attendus incluent `[CODEX CHECKS] NETWORK_FORGE PASS`, `[CODEX CHECKS] PORTALS PASS`, `[CODEX CHECKS] DEDICATED_DUNGEONS PASS` et `build/reports/network-checks.txt` contenant `PASS`. Elle vérifie aussi l'isolation du rôle, les groupes durables, le propriétaire commun des instances, la capacité et la réutilisation, les caches périmés, la salle d'attente protégée et la persistance/sélection du point de retour. Les téléportations sont enregistrées par un faux joueur de test, sans connexion réseau réelle. Une connexion réelle avec des comptes officiels reste nécessaire pour vérifier de bout en bout le départ et le retour d'un groupe via Velocity.

## Cles de chat et identifiants internes

Le proxy reste en `online-mode=true` et `force-key-authentication=true`. Les backends lies exclusivement a `127.0.0.1` conservent les UUID hors ligne pour les sauvegardes existantes. Une cle de chat signee pour le compte officiel ne correspond pas a cet UUID interne : le mod ignore donc la mise a jour de session de chat sur ces seuls backends, lorsque le reseau est active, l authentification locale et `enforce-secure-profile` sont desactives. Le decodeur de chat non signe reste actif. Le chat entre backends ne beneficie pas de signatures officielles ; les joueurs sont toujours authentifies par Velocity. Aucune modification pour les mondes solo, les serveurs autonomes ou ceux lies a une adresse publique. Ne jamais exposer directement les ports des backends.

Le chargement initial affiche le logo et un message unique, sans statistiques ni barres NeoForge. F1 masque les calques personnalises Aeloria.
