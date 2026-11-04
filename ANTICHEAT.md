# Anticheat

Interrupteur : `anticheat` dans `/fonctionnalites` (ou `/fonctionnalite anticheat activer|desactiver`). Commandes : `/anticheat`, `/anticheat joueurs`, `/anticheat reinitialiser <joueur>`.

- **Paquets** : chaque paquet client→serveur passe par `AntiCheat.gate` (limite de débit par joueur et par type, permission opérateur obligatoire pour les paquets d'édition).
- **Détections** : vitesse, vol stationnaire, portée de frappe, cadence de frappe, portée et cadence de casse.
- **Exemptions** : monture (la mesure de vitesse repart de zéro à la descente), élytre, trident, coup reçu, compétences de déplacement (charge, bond, téléportation, saut arrière) et attraction du boss du Néant (`AntiCheat.grace`), glace et nage (Grâce du dauphin). La cadence de casse tolère le minage instantané légitime (1 bloc par tick).
- **Sanctions** : score par joueur qui décroît (0,5/s). 10 alerte aux opérateurs, 25 retour à la dernière position sûre, 50 expulsion (3 expulsions : bannissement conseillé, jamais automatique). Les opérateurs sont journalisés, jamais sanctionnés.
- **Journal** : `config/mmorpg/anticheat.log` + table `events` (kind `anticheat`).
- **Pare-feu** : voir `PARE_FEU.md`.
- **Hors du mod** : anti-xray (à régler au niveau du serveur/monde), pare-feu et limites de connexion (Velocity), mots de passe de base de données.

## Sécurité renforcée (interrupteur `anticheat`)

- **Commandes** : joueurs non opérateurs limités à 8 en rafale puis 2/s (excès refusé et signalé). `CommandGuard`.
- **Interfaces** : le serveur retient les écrans qu'il a ouverts (30 min) et refuse forge, boutique, téléporteur, caisse et bloc chanceux sans ouverture préalable. `ScreenGuard`.
- **Journal d'audit admin** : `config/mmorpg/admin_audit.log` (toujours actif) reçoit toutes les commandes des opérateurs/console et les actions d'admin ; `/audit [n]` relit les dernières lignes.
- **Duplication** : les équipements du mod reçoivent un identifiant `mmorpg_uid` ; le même identifiant chez deux joueurs connectés sur deux balayages (5 s) est signalé aux opérateurs et journalisé. Rien n'est supprimé ; copies d'opérateurs ignorées. `ItemIntegrity`.
- **Signature des paquets** : clé de session remise à la connexion, HMAC-SHA256 + numéro croissant avant chaque paquet sensible (admin, social, forge, boutique, éditeurs). Refuse le rejeu et l'injection, pas un client modifié qui connaît la clé. `PacketSigner`.
- **Session unique** (avec base de données) : si le compte est encore en ligne sur un autre serveur après l'attente de transfert (3 s), la nouvelle connexion est refusée ; un crash libère le compte après 3 min.
- Test : `./gradlew runServer -PsecurityChecks`.
