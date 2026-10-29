# Anticheat

Interrupteur : `anticheat` dans `/fonctionnalites` (ou `/fonctionnalite anticheat activer|desactiver`). Commandes : `/anticheat`, `/anticheat joueurs`, `/anticheat reinitialiser <joueur>`.

- **Paquets** : chaque paquet client→serveur passe par `AntiCheat.gate` (limite de débit par joueur et par type, permission opérateur obligatoire pour les paquets d'édition).
- **Détections** : vitesse, vol stationnaire, portée de frappe, cadence de frappe, portée et cadence de casse.
- **Sanctions** : score par joueur qui décroît (0,5/s). 10 alerte aux opérateurs, 25 retour à la dernière position sûre, 50 expulsion (3 expulsions : bannissement conseillé, jamais automatique). Les opérateurs sont journalisés, jamais sanctionnés.
- **Journal** : `config/mmorpg/anticheat.log` + table `events` (kind `anticheat`).
- **Pare-feu** : voir `PARE_FEU.md`.
- **Hors du mod** : anti-xray (à régler au niveau du serveur/monde), pare-feu et limites de connexion (Velocity), mots de passe de base de données.
