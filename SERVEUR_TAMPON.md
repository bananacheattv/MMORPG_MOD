# Serveur lobby (choix de personnage) — à transmettre à l'administrateur machine

Périmètre actuel : **uniquement le serveur lobby et le dossier partagé**. Proxy anti-DDoS, pare-feu et le reste de l'infrastructure viendront plus tard.

## Rôle

Un serveur du mod en rôle `LOBBY`, toujours en ligne, où arrive toute nouvelle connexion. Il affiche l'écran de choix de personnage ; « C'est parti ! » envoie le joueur vers le serveur de jeu (Spawn s'il a une classe, sinon hub). Il ne construit ni hub ni donjons.

## Installation du lobby

- Même JAR du mod que les autres serveurs (Java 25, NeoForge 26.3), dossier `servers/lobby`, port **25575**, `online-mode=false`, `server-ip=127.0.0.1`.
- `server.properties` conseillé : `view-distance=2`, `simulation-distance=2`, `spawn-protection=0`, `gamemode=adventure`, `spawn-monsters=false`, `level-type=flat`, JVM `-Xms512M -Xmx1G`.
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

- Velocity : ajouter `lobby = "127.0.0.1:25575"` dans `[servers]` et mettre `try = ["lobby", "hub"]` (nom identique à `network.json`). **Toujours garder `hub` en repli** : avec `try = ["lobby"]` seul et un lobby arrêté ou absent, Velocity affiche « Unable to connect you to lobby. Please try again later. » et personne ne peut entrer. Si le lobby n'est pas installé, utiliser `try = ["hub"]`.

## Dossier partagé

- `sharedDirectory` doit désigner **exactement le même dossier** sur le lobby et sur les cinq autres serveurs.
- Le mod crée à côté de `players` le dossier **`characters`** (fiches des personnages). Il doit être sur le même disque/partage que `players`, accessible à tous les serveurs en lecture/écriture.
- À sauvegarder **ensemble** : `players` (profils `.dat`, `.lock`, `.bak`) et `characters`. Ne jamais restaurer l'un sans l'autre : le personnage actif est dans `players`, les autres dans `characters`.
- Autres dossiers partagés existants à inclure dans la sauvegarde : `guilds`, `commerce`, `coordination`.

## Ordre

Même JAR installé en même temps sur tous les serveurs et les clients. Démarrer les serveurs de jeu et le lobby, puis Velocity.

## Vérifier

Connexion → écran de choix sur le lobby → « C'est parti ! » arrive au Spawn. Non testé de bout en bout derrière Velocity ; `Install-AeloriaNetwork.ps1` ne crée pas encore le service lobby (à créer à la main).
