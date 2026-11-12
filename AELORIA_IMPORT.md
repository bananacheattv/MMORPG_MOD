# Importer la carte Aéloria avec `/aeloriaimport`

Module du mod (`com.mmorpg.aeloria`), autonome : ni WorldEdit ni plugin de gameplay. Une seule commande enchaîne les 100 secteurs (Sponge v3, DataVersion 5023, Minecraft 26.3, Java 25) sans téléportation ni intervention.

## Installation

1. Déposer le JAR du mod dans `mods/` (comme d'habitude).
2. Copier les 100 fichiers `.schem` dans `config/worldedit/schematics/` (le même dossier que `/schem`). Le CSV d'ordre, d'origines et d'empreintes de l'édition **Relief** est intégré au mod. Si vos `.schem` viennent d'une autre édition, copiez leur `importation.csv` **à côté des `.schem`** (ou dans `config/mmorpg/aeloria/`) : il est utilisé en priorité. Sans cela, les tailles et SHA-256 ne correspondent pas et l'import refuse de démarrer.
3. Le monde doit couvrir Y −64 à 319 (Overworld). Prévoir `-Xmx4G` ou plus (un secteur occupe environ 200 Mo) et de la place disque.
4. Sauvegarder le monde. Pour un premier essai, utiliser un monde de test.

## Commande exacte

```text
/aeloriaimport
```

À taper en jeu (permission opérateur niveau 2) **dans l'Overworld** (`minecraft:overworld`) : toute autre dimension (hub, spawn, mines…) est refusée pour ne rien écraser. Depuis la console, elle vise l'Overworld. La dimension est annoncée au démarrage. Le joueur ne sert qu'à choisir la dimension : l'import continue s'il se déconnecte, et la pause « serveur vide » est suspendue pendant l'import.

Autres usages : `/aeloriaimport status` (progression), `/aeloriaimport stop` (arrêt propre), `/aeloriaimport verify` (relit les secteurs déjà importés et compte les blocs différents), `/aeloriaimport reset` (efface la reprise), `/aeloriaimport budget <ms>` (temps d'écriture par tick, 20 par défaut).

## Ce que fait l'import

- Avant la première modification : présence, taille, SHA-256, en-tête Sponge v3, DataVersion 5023, dimensions, palettes et biomes des 100 fichiers. Toute erreur est listée et le monde n'est pas touché.
- Ordre et origines absolues du CSV : premier secteur en `-2500 -64 -2500`, dernier en `2000 -64 2000`.
- Équivalent de `//paste -a -o -b` : l'air est ignoré, l'eau est collée, les biomes sont appliqués (cellules de 4 blocs).
- Un secteur à la fois : lecture hors du thread serveur, écriture chunk par chunk par tranches de temps (le serveur reste réactif), puis sauvegarde du monde.
- Reprise : après un arrêt, un crash ou une erreur, relancer `/aeloriaimport` dans la même dimension reprend au dernier secteur intégralement enregistré (`config/mmorpg/aeloria/progress_*.properties`). Deux imports simultanés sont refusés.
- Progression dans la console et pour l'opérateur connecté toutes les 15 s, avec une estimation du temps restant.

## À savoir

- Sur un monde **déjà généré**, l'air n'étant pas collé, des blocs existants peuvent rester dans les grottes et sous les îles. Utiliser un monde vide (générateur plat ou vide) pour un résultat identique au schéma.
- Les blocs-entités éventuels (coffres…) ne sont pas importés (un avertissement les signale).
- Les propriétés finales des fluides dépendent des mises à jour de blocs du serveur : l'import n'en déclenche aucune.
- La durée dépend de la machine : voir les mesures du fichier de livraison. Augmenter `budget` accélère l'import, au prix de ticks plus longs.
- Essai restreint : un fichier `config/mmorpg/aeloria/only.txt` (un ordre ou un nom de fichier par ligne) limite l'import à ces secteurs ; le contrôle des 100 fichiers reste complet. Le supprimer pour l'import complet.
