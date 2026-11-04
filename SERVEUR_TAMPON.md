# Serveur tampon (proxy + lobby de connexion) — à transmettre à l'administrateur machine

## Principe

Les joueurs ne voient qu'**une adresse publique** : le proxy **Velocity**. Il est placé derrière une protection anti-DDoS. Derrière lui, un petit serveur **lobby** (rôle `LOBBY` du mod) accueille la connexion et affiche l'écran de choix de personnage ; « C'est parti ! » envoie le joueur vers le serveur de jeu (Spawn s'il a une classe, sinon hub). Tout le reste est privé.

```
Internet ──► [anti-DDoS] ──► Velocity :25565 ──► lobby   :25575  (choix du perso)
                                   │──► hub     :25571
                                   │──► serveur :25570
                                   │──► mines   :25572   plaines :25573   donjons :25574
Dossier partagé : players/ characters/ guilds/ commerce/ coordination/ (tous les backends)
```

Une attaque touche l'adresse publique (anti-DDoS + proxy), jamais les serveurs de jeu. Si les serveurs de jeu sont arrêtés, le proxy et le lobby restent joignables.

## Réseau et pare-feu

- Ouvert à Internet : **uniquement Velocity (25565/TCP)**, idéalement sur une machine ou une IP séparée avec protection anti-DDoS (OVH Game, TCPShield, Cloudflare Spectrum ou équivalent). L'IP réelle du proxy/de la machine de jeu ne doit pas être divulguée.
- Tous les backends (lobby 25575, hub 25571, serveur 25570, mines 25572, plaines 25573, donjons 25574) : écoute sur `127.0.0.1` ou réseau privé/VPN, pare-feu limité à l'IP du proxy. **Obligatoire** : les backends sont en `online-mode=false`, donc un accès direct permettrait d'usurper n'importe quel compte.
- Si le proxy est sur une autre machine que les backends : les ports ci-dessus passent par le réseau privé/VPN, et le dossier partagé doit être monté sur toutes les machines de jeu (pas sur le proxy).

## Velocity (`velocity.toml`)

```toml
bind = "0.0.0.0:25565"
online-mode = true
force-key-authentication = true
player-info-forwarding-mode = "none"
bungee-plugin-message-channel = true   # requis : les transferts du mod passent par bungeecord:main

[servers]
lobby   = "127.0.0.1:25575"
hub     = "127.0.0.1:25571"
serveur = "127.0.0.1:25570"
mines   = "127.0.0.1:25572"
plaines = "127.0.0.1:25573"
donjons = "127.0.0.1:25574"
try = ["lobby"]                        # toute nouvelle connexion arrive au lobby

[advanced]
login-ratelimit = 3000                 # ms entre deux connexions depuis la même IP
connection-timeout = 5000
read-timeout = 30000
```

Remplacer `127.0.0.1` par les IP privées si les machines sont séparées. Les noms de `[servers]` doivent être exactement ceux de `network.json` (`lobby`, `hub`, `serveur`, `mines`, `plaines`, `donjons`).

## Serveur lobby

- Même JAR du mod que les autres (Java 25, NeoForge 26.3), dossier `servers/lobby`, port 25575, `online-mode=false`, `server-ip=127.0.0.1`.
- `server.properties` conseillé : `view-distance=2`, `simulation-distance=2`, `spawn-protection=0`, `gamemode=adventure`, `spawn-monsters=false`, `level-type=flat` (monde vide), `max-players` large, JVM `-Xms512M -Xmx1G`.
- `config/mmorpg/network.json` :

```json
{
  "enabled": true,
  "role": "LOBBY",
  "sharedDirectory": "C:/chemin/vers/shared/players",
  "hub": "hub", "main": "serveur", "mines": "mines", "plains": "plaines", "dungeons": "donjons",
  "lobby": "lobby"
}
```

`sharedDirectory` doit désigner **exactement le même dossier** que sur les cinq autres serveurs. Ajouter aussi `"lobby": "lobby"` dans le `network.json` des autres serveurs (valeur par défaut, donc facultatif).
- Le lobby ne construit ni hub ni donjons ; il ne sert qu'à choisir le personnage.

## Dossier partagé et sauvegardes

À sauvegarder ensemble, serveurs arrêtés ou via instantané du disque : `shared/players` (profils `.dat`, `.lock`, `.bak`), `shared/characters` (fiches de personnages, créée à côté de `players`), `shared/guilds`, `shared/commerce` (boutique, crédits), `shared/coordination` (interrupteurs), plus les mondes de chaque serveur et `velocity.toml`. Ne jamais restaurer `players` sans `characters` (le personnage actif est dans le profil, les autres dans `characters`).

## Mise à jour et ordre de démarrage

Même JAR partout (lobby compris), à installer en même temps sur les serveurs et les clients. Démarrer : serveurs de jeu et lobby, puis Velocity en dernier. Arrêt : Velocity d'abord.

## Vérifier

Se connecter : écran de choix visible sur le lobby ; « C'est parti ! » arrive au Spawn ; arrêter `serveur` et vérifier qu'on reste sur le lobby avec un message d'échec du transfert au lieu d'être déconnecté.

## Limites actuelles (à savoir)

Rôle `LOBBY` ajouté et testé côté configuration ; le transfert réel lobby → Spawn derrière Velocity n'a pas été testé de bout en bout. Les scripts `tools/Install-AeloriaNetwork.ps1` ne créent pas encore le service lobby : à créer à la main selon ce document.
