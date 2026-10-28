# Guide de mise en production d'un serveur Aeloria

Le mod fonctionne **sans base de données** (tout est dans le dossier du serveur) et peut **en plus** envoyer les profils des joueurs, la banque et un journal d'événements dans une base MariaDB/MySQL (voir §10, désactivée par défaut). Ce guide liste ce qu'il faut régler et surveiller avant d'ouvrir au public. Les points marqués *(non mesuré)* n'ont pas été vérifiés en charge réelle.

## 1. Où sont les données

| Quoi | Où | Sauvegarder ? |
| --- | --- | --- |
| Monde, banque, hôtel des ventes, mines, plaines, hub, spawn, donjons, zones, PNJ, données des joueurs | `world/` | **oui, c'est l'essentiel** |
| Réglages du mod (monstres, quêtes, boutique, butins, spawn, cinématique, mot de passe admin haché) | `config/mmorpg/` | **oui** |
| Réglages du serveur, opérateurs, liste blanche, bannis | `server.properties`, `ops.json`, `whitelist.json`, `banned-*.json` | oui |
| Journaux | `logs/` | non (mais à protéger, voir §6) |

Perdre `world/` fait perdre toute la progression. Rien n'est partagé entre deux serveurs : chaque serveur a sa propre économie et ses propres joueurs.

## 2. Installation

1. **Java 25** sur la machine.
2. **NeoForge 26.3.0.39-beta** (Minecraft 26.3) : lancer l'installeur de cette version en mode serveur dans un dossier vide. Il crée les scripts de lancement (`run.sh` / `run.bat`) et `user_jvm_args.txt`.
3. Déposer le jar du mod dans `mods/`. La dernière version est dans la release `latest` du dépôt (fichier `mmorpg-*.jar`, la même que celle que le launcher installe chez les joueurs). **Le mod doit être de la même version sur le serveur et chez les clients** : après chaque fusion sur `main`, mettre à jour le jar du serveur.
4. Lire et accepter l'EULA de Minecraft (`eula.txt` après un premier lancement à vide), puis démarrer.

## 3. Réglages recommandés

`server.properties` :

```properties
online-mode=true            # indispensable en public : sinon n'importe qui peut prendre le pseudo d'un opérateur
enable-rcon=false           # (ou un mot de passe fort et le port jamais ouvert au public)
enforce-whitelist=true      # seulement si l'accès est limité ; sinon false
max-players=40              # à ajuster après mesure (non mesuré)
motd=Aeloria                # le nom affiché dans la liste des serveurs
view-distance=10
simulation-distance=8
```

`user_jvm_args.txt` : commencer par `-Xms6G -Xmx6G` et ajuster selon la consommation observée *(non mesuré)* ; ne pas donner toute la mémoire de la machine.

Pare-feu : n'ouvrir que le port **25565 TCP**. Jamais le port RCON (25575) ni le port de gestion.

Compte : lancer le serveur avec un utilisateur système dédié, sans droits administrateur.

## 4. Premier démarrage

- Le serveur construit tout seul le hub, la Cité d'Aeloria, les 5 mines et les 6 plaines, par tranches de temps (quelques secondes à quelques minutes au total). **Attendre la fin avant d'ouvrir** : les journaux affichent la construction de chaque carte. `/spawn reconstruire` force le spawn.
- Un **mot de passe admin aléatoire** est affiché **une seule fois dans la console**. Notez-le ; `/adminpass <nouveau>` (console) le change. Le menu s'ouvre en jeu avec `/adminmenu "mot de passe"`.
- Donnez le statut d'opérateur depuis la console : `op <pseudo>`.
- Le hub accueille 20 joueurs par copie ; une nouvelle copie est construite quand toutes sont pleines (le serveur se fige alors une trentaine de secondes, c'est annoncé à tous).

## 5. Sauvegardes et redémarrage quotidien

Copier un monde pendant que le serveur écrit peut donner une sauvegarde inutilisable. Deux méthodes sûres :

- **Recommandé** : redémarrage quotidien (à l'heure creuse) : arrêt (`stop`), sauvegarde, démarrage.
- **À chaud** : `save-off`, `save-all flush`, sauvegarde, puis `save-on` (depuis la console ou RCON local).

Scripts fournis (`tools/serveur/`) : une archive datée de `world/`, `config/mmorpg/` et des listes, vérifiée à la relecture, avec rotation (14 par défaut). Une sauvegarde qui échoue ne laisse pas d'archive partielle.

```bash
# Linux
tools/serveur/backup.sh /srv/aeloria /srv/sauvegardes 14
```

```powershell
# Windows
.\tools\serveur\backup.ps1 -Serveur C:\aeloria -Destination D:\sauvegardes -Garder 14
```

Planifier avec `cron` ou le Planificateur de tâches, **copier les archives hors de la machine** (autre disque ou stockage distant) et **tester une restauration** (dézipper dans un dossier vide, lancer le serveur dessus) avant l'ouverture.

Restaurer : serveur arrêté, remplacer `world/` et `config/mmorpg/` par ceux de l'archive.

## 6. Sécurité

- Les commandes des joueurs sont écrites dans `logs/latest.log` : **le mot de passe tapé avec `/adminmenu` y apparaît**. Restreindre l'accès aux journaux et changer le mot de passe régulièrement (`/adminpass`).
- `config/mmorpg/admin.json` (empreinte du mot de passe) et `config/mmorpg/sauvegardes/` ne se partagent pas.
- Cinq essais ratés de mot de passe bloquent le joueur 10 minutes.
- Peu d'opérateurs : un opérateur contourne la plupart des règles.

## 7. Mises à jour du mod

1. Prévenir les joueurs, arrêter le serveur, **sauvegarder**.
2. Remplacer le jar dans `mods/` par celui de la release `latest`.
3. Redémarrer. Un plan de hub, de spawn, de mine ou de plaine plus récent est reconstruit automatiquement au démarrage (versions de plan dans `GUIDE_ADMIN.md`) ; un monde existant est conservé.
4. Les joueurs se mettent à jour par le launcher.

## 8. Surveillance

- `logs/latest.log` : chercher `ERROR` et `Can't keep up` (tick trop lent).
- `/tick query` (commande de Minecraft) donne la durée moyenne d'un tick : au-dessus de 50 ms le serveur est en retard.
- Surveiller l'espace disque : les mines, plaines et donjons agrandissent le monde.

## 9. Ce qui n'est pas prévu

- Sauvegarde automatique intégrée au mod : non, utiliser les scripts ci-dessus.
- Voir §10 pour ce que la base partage entre serveurs et ce qui reste à faire pour un réseau complet.

## 10. Base de données (réseau de serveurs)

Le panel (`Desktop\serveur manager`) installe MariaDB (base `aeloria`, utilisateur `aeloria`, écoute sur `127.0.0.1:3306`). Le mod sait s'y connecter : le pilote est embarqué dans son jar, rien à installer.

### Activer

1. Démarrer la base depuis le panel.
2. Sur **chaque serveur de jeu**, éditer `config/mmorpg/database.json` (créé au premier démarrage, désactivé) :

```json
{
  "enabled": true,
  "host": "127.0.0.1", "port": 3306, "database": "aeloria", "user": "aeloria",
  "password": "",
  "serverId": "serveur",
  "autosaveSeconds": 120, "syncPlayers": true, "audit": true
}
```

   - `serverId` : un nom **différent sur chaque serveur** (`serveur`, `hub`, `mines`, `plaines`). Il sert à savoir quel serveur tient un joueur.
   - Mot de passe : de préférence dans la variable d'environnement `AELORIA_DB_PASSWORD` du service (elle prime sur le fichier), sinon dans `password`. Le fichier n'est pas dans le menu admin, mais restez prudent : ne le partagez pas.
3. Redémarrer. Le journal affiche « Base de données : connectée » et les tables sont créées toutes seules (`tools/serveur/schema.sql` les décrit). `/db etat` (opérateurs) confirme.

Si la base est arrêtée ou injoignable, le jeu continue normalement avec les données locales ; les écritures sont mises en file (20 000 au plus) et le mod se reconnecte tout seul.

### Ce qui est enregistré

| Table | Contenu |
| --- | --- |
| `players` | un profil par joueur : classe, niveau, XP, or, compétences, quêtes, montures, familiers, cosmétiques, mines/plaines débloquées, kills… (colonnes de recherche + profil complet) |
| `bank_accounts` | or en banque et coffre de 3 pages |
| `players.inventory` | sac, armure, main secondaire et coffre de l'Ender (suivent le joueur d'un serveur à l'autre) |
| `transfers` | arrivée en attente d'un joueur envoyé vers un autre serveur (effacée à son arrivée) |
| `events` | journal : connexion, sortie, niveau, mort, boss tué, donjon vaincu, dépôt/retrait de banque, ventes et achats (hôtel des ventes, marchands), quêtes, déblocages, montures |

Écriture : à chaque sortie, puis toutes les `autosaveSeconds` secondes, seulement si le profil a changé. Lecture : à l'arrivée d'un joueur, **la base fait foi si sa version est plus récente** que celle du serveur ; si un autre serveur tient encore le joueur (il vient de partir), on attend jusqu'à 3 s qu'il ait fini de sauvegarder. Un serveur qui ne tient plus un joueur ne peut plus l'écraser.

Commandes (opérateurs) : `/db etat`, `/db sauvegarder` (écrit tout de suite), `/db joueur <pseudo>`, `/db evenements [n]`.

Statistiques, classement, or en circulation, historique d'un joueur : voir les requêtes en bas de `tools/serveur/schema.sql`. Purgez le journal de temps en temps (`DELETE FROM events WHERE ts < NOW() - INTERVAL 90 DAY`). La base fait partie des **sauvegardes** : `mariadb-dump -u aeloria -p aeloria > aeloria.sql` (ou copie du dossier `servers/db/data` base arrêtée).

### Ce qui n'est pas (encore) partagé

- **Position** : chaque serveur place le joueur lui-même (voir §11).
- **Hôtel des ventes, mines/plaines épuisées, donjons, zones, PNJ** : restent dans le monde de chaque serveur (seule la banque, qui est personnelle, suit le joueur).
- **Un jeu en plusieurs serveurs** : voir §11 ; sans `network.json`, un serveur construit et sert tout lui-même, comme avant.

### Sécurité du réseau (à régler dans le panel avant l'ouverture)

- Le proxy Velocity du panel est en `online-mode = false` et `player-info-forwarding-mode = "none"` : **n'importe qui peut se connecter avec le pseudo d'un autre**, donc prendre son profil, son or et sa banque (les profils sont rangés par identifiant de joueur, qui en mode hors-ligne se déduit du pseudo). Pour un serveur public : `online-mode = true` sur le proxy, transfert d'identité sécurisé (`modern`) avec le secret partagé, et serveurs de jeu en `online-mode=false` **uniquement** joignables depuis la machine du proxy (pare-feu : seul le port 25565 du proxy est ouvert).
- Le mot de passe de la base est en clair dans `servers/db/IDENTIFIANTS.txt`, `servers/db/init.sql` et `servers/db/my.ini` (compte root) : gardez ces fichiers hors de tout dépôt et de toute capture, et changez les mots de passe avant la mise en ligne.
- La base n'écoute que sur `127.0.0.1` : laissez-la ainsi.

## 11. Plusieurs serveurs derrière un proxy (hub, spawn, mines, plaines)

Le réseau multi-serveurs (rôles `HUB`/`MAIN`/`MINES`/`PLAINS`/`DUNGEONS`, `config/mmorpg/network.json`, profils partagés, transferts Velocity) est décrit dans `RESEAU_AELORIA.md`. La base MariaDB (§10) y est facultative : elle reflète profils et banque (le profil partagé fait foi en réseau), journalise les actions et stocke les données partagées (guildes, hôtel des ventes).

