# Portails d'aventure et annulation

Commandes réservées aux opérateurs (permission maître du jeu). L'arche apparaît **quatre blocs devant le joueur**, tournée selon son regard. Prévoir un sol plat, avec un espace vide de **5 blocs de large sur 5 de haut**. La commande ne remplace ni terrain, ni coffre : si la place manque, elle refuse sans modifier le monde.

| Commande | Résultat |
| --- | --- |
| `/portail mine` | Arche ouvrant le choix des mines |
| `/portail mine 1` | Accès direct à la première mine (numéros 1 à 5) |
| `/portail plaines` | Arche ouvrant le choix des plaines |
| `/portail plaines 1` | Accès direct à la première plaine (numéros 1 à 6) |
| `/portail donjon crypte_gobeline` | Instance de la Crypte Gobeline en difficulté normale |
| `/portail donjon crypte_gobeline heroique` | Même donjon en héroïque |
| `/portail donjon crypte_gobeline mythique` | Même donjon en mythique |
| `/portail liste` | Liste des portails enregistrés, identifiants, dimensions et coordonnées |
| `/portail supprimer` | Supprime le portail situé à moins de 8 blocs le plus proche |

Utiliser les suggestions de commande pour les autres identifiants de donjon. Sans difficulté précisée, le donjon est normal. Les arches utilisent les blocs de marbre et cristaux du mod ; leurs particules sont orange pour les mines, vertes pour les plaines et violettes pour les donjons. Le nom de la destination s'affiche en s'approchant.

Traverser l'ouverture active le portail une seule fois ; il faut ressortir pour le réactiver. Revenir d'une instance à l'intérieur de son portail d'entrée ne relance pas immédiatement une nouvelle instance. Une arche dont le cadre a été cassé devient inactive ; elle peut toujours être supprimée par commande.

Les niveaux, difficultés, choix de classe et découvertes des mines/plaines sont contrôlés côté serveur. Un portail vers une région avancée ne la débloque pas. Les essais, cinématiques et combats PvP empêchent l'entrée. Les voyages utilisent les mécanismes existants (écran de chargement, instance personnelle/de groupe et, en réseau, transfert vers le serveur approprié). Les arches et leurs destinations sont sauvegardées dans les données du monde et conservées au redémarrage.

## Annuler

- `/undoportail` annule la dernière pose **ou suppression** de portail effectuée par cet administrateur.
- `/undoportail 2` annule ses deux dernières modifications. La limite est de 20 opérations.
- `/undo` et `/undo 2` sont des alias si aucune autre extension n'a déjà enregistré `/undo`. Avec WorldEdit, utiliser `/undoportail` pour ces portails.

Annuler une pose retire le cadre et son activation, et remet les blocs touchés dans leur état exact d'avant la commande. Le terrain voisin et le sol ne sont pas modifiés par la pose. Annuler une suppression rétablit le cadre et la destination. Si un bloc a été changé depuis par un bâtisseur, l'annulation refuse de l'écraser et conserve l'opération dans l'historique : remettre ce bloc dans l'état attendu permet de réessayer.

L'historique est propre à chaque administrateur et reste en mémoire jusqu'à l'arrêt du serveur ; **les anciens portails restent sauvegardés, mais l'historique d'annulation n'est pas conservé après un redémarrage**. Cet historique couvre uniquement les poses et suppressions de portails réalisées par ces commandes, pas les autres constructions du mod ou les opérations WorldEdit.

## Tests

La suite `gradlew.bat runServer -PnetworkChecks build` comprend désormais les vérifications `[CODEX CHECKS] PORTALS PASS` : deux orientations, commandes et alias, destinations persistantes, refus des chevauchements/coffres, annulation de pose et de suppression, retour exact du terrain, protection des modifications ultérieures, contrôle des accès et prévention du rebond au retour d'un donjon. Utiliser exclusivement le monde de développement pour cette suite, comme expliqué dans `RESEAU_AELORIA.md`.
