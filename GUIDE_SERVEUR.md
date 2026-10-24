# Guide de mise en production d'un serveur Aeloria

Le mod n'utilise **aucune base de données** : tout est dans le dossier du serveur. Ce guide liste ce qu'il faut régler et surveiller avant d'ouvrir au public. Les points marqués *(non mesuré)* n'ont pas été vérifiés en charge réelle.

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

- Plusieurs serveurs reliés (économie, banque ou progression partagées) : non, il faudrait une vraie base de données.
- Sauvegarde automatique intégrée au mod : non, utiliser les scripts ci-dessus.
