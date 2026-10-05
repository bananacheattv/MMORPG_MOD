# Administration Eldoria

## PNJ et quetes

Commandes reservees aux operateurs :

- `/mmorpg pnj quetes Aldric le Sage` : PNJ generaliste.
- `/mmorpg pnj quetes_groupe cryptes 2 Gardien des Cryptes` : PNJ du groupe `cryptes`, apparence 2 (0 a 3).
- `/mmorpg pnj marchand Brunhilde` : marchand.
- `/mmorpg pnj supprimer` : supprime tous les PNJ dans un rayon de 4 blocs.

Dans `config/mmorpg/quests.json`, donner la valeur `cryptes` au champ `giver` pour reserver une quete a ce groupe. Un champ vide autorise tous les maitres des quetes. Le joueur doit etre a moins de 8 blocs du bon PNJ pour accepter ou rendre la quete. Les PNJ sont sauvegardes avec le monde.

Chaque quete possede un identifiant unique, `name`, `description`, `minLevel`, `giver`, une liste `requires` de quetes prealables, `daily`, `objectives` et `rewards`. Objectifs : `kill` (cle du monstre, `*` ou `boss`), `collect` (identifiant objet) et `level`. `count` indique la quantite ou le niveau cible. Les objectifs de collecte consomment les objets a la remise.

Utiliser `/mmorpg reload` apres modification. Les nouvelles quetes par defaut sont ajoutees sans ecraser les definitions personnalisees existantes. Les journalieres redeviennent disponibles a minuit selon la date du serveur.

Le journal du joueur propose Toutes / Campagne / Journalieres / A rendre. « Suivre en priorite » place la quete en tete du suivi a l'ecran. Dix quetes peuvent etre actives simultanement.
