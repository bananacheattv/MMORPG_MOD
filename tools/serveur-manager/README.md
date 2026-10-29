# Aeloria Panel (serveur-manager)

Panel web (Node.js >= 18, aucun `npm install`) pour gérer le réseau Aeloria : base de données, proxy Velocity, serveurs serveur / hub / mines / plaines.

## Installation
1. Copier ce dossier (`tools/serveur-manager/`) sur la machine du serveur, par exemple dans `Desktop\serveur manager`.
2. Copier `config.example.json` en `config.json` et adapter dossiers, commandes et ports. Ne jamais committer `config.json`, `data/` ni `servers/`.
3. Lancer `start.bat` (relance le panel s'il plante), ouvrir http://127.0.0.1:8090 et choisir le mot de passe admin à la première visite.

## Mise à jour du panel
Si le panel est déjà installé : **ne remplacer que `server.js` et le dossier `public/`**. Ne pas écraser `config.json` ni `data/` (mot de passe, état, sessions). Fermer `start.bat`, copier les deux, relancer `start.bat`.

## Mise à jour du mod (page « Mise à jour du mod »)
- Télécharge le jar de la release `latest` de `bananacheattv/MMORPG_MOD`, vérifie taille, format et SHA-256.
- Sauvegarde l'ancien jar dans `servers/<id>/mods_backup/<date>/`, puis le remplace dans `mods/` de chaque serveur cible (`mmorpg-*.jar` et `aeloria-*.jar` sont remplacés ; les autres mods ne sont pas touchés).
- Si des serveurs tournent : confirmation, annonce en jeu 15 s avant, arrêt propre, remplacement, redémarrage. Mondes, base et configs ne sont pas touchés.
- Vérifie les nouvelles versions au démarrage puis toutes les 6 h (badge « ! » dans le menu).
- Dépôt public : aucun token requis. Dépôt privé : token GitHub dans les réglages de la page, ou variable d'environnement `AELORIA_GITHUB_TOKEN`.
- Penser à copier le même jar côté client.

Voir aussi `tools/serveur/` pour la base (`schema.sql`) et les sauvegardes.
