# Choix de personnage

À la connexion, un écran de sélection s'ouvre : liste des personnages à droite (portrait, nom, classe, niveau, lieu), personnage 3D au centre, boutons **Créer Personnage**, **Effacer personnage**, **Retour** et **C'est parti !**.

## Règles

- **3 emplacements gratuits**, jusqu'à **12** en achetant des emplacements avec des **crédits de boutique** (prix : `slotPrice` dans `boutique.json`, 400 par défaut ; achat en deux clics, idempotent, débité du compte).
- Chaque personnage a ses propres données : classe, niveau, progression, quêtes, compétences, succès, familiers/montures, inventaire, équipement, coffre de l'Ender, **banque** et **guilde**.
- Restent au niveau du **compte** : crédits, grade et badge staff, annonces de l'hôtel des ventes, groupes, permissions.
- Un personnage qu'on rejoint depuis un autre apparaît au Spawn (ou au hub s'il n'a pas de classe) ; celui qu'on reprend sans changer reste où il s'était déconnecté.
- Supprimer un personnage demande d'écrire son nom ; s'il est chef d'une guilde qui a d'autres membres, ou si la banque de guilde n'est pas vide, la suppression est refusée.
- Phase 1 : le choix se fait **à la connexion** uniquement (pas de changement en cours de partie).

## Données et migration

- Le personnage joué vit dans les données du joueur comme avant ; les autres sont rangés dans `data/aeloria-characters/<UUID du compte>.dat` (ou `<dossier partagé>/../characters/` en réseau), avec une copie `.bak` et une écriture atomique. Une fiche illisible n'est jamais remplacée par une fiche vide.
- **Aucune migration à lancer** : l'emplacement 0 reprend l'UUID du compte, donc les profils, guildes, succès et banques existants sont déjà ceux du personnage 0. Les emplacements suivants utilisent un UUID dérivé (`CharKey`) pour les registres partagés (guildes, succès).
- Chaque joueur n'a qu'un personnage chargé : en réseau, le profil partagé contient le personnage actif, comme avant.

## Interrupteur

Fonctionnalité **personnages** (`/fonctionnalite desactiver personnages` ou écran `/fonctionnalites`, catégorie Social) : désactivée, la connexion redevient celle d'avant, sans écran.

## Serveur tampon (réseau)

Aucun serveur supplémentaire n'est nécessaire : l'écran s'affiche sur le serveur où le proxy fait entrer le joueur (le **hub**, déjà configuré dans Velocity), et le dossier `characters` suit le dossier partagé (`sharedDirectory`). À vérifier côté machine : que tous les backends partagent le même dossier `sharedDirectory`, et que `characters` (frère de `players`) soit sauvegardé avec lui.

## Vérification

`./gradlew runServer -PnetworkChecks` : marqueur `[CODEX CHECKS] CHARACTERS PASS`. Capture : `tools/screens/capture.sh personnages`.
