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

## Caisses et Lucky Blocks

Fabriquer caisse et cle a la Forge (niveau 5), puis utiliser la caisse avec une cle dans l’inventaire. Le Lucky Block se fabrique au niveau 10 : le poser, faire un clic droit puis choisir « Ouvrir ». Trois rouleaux affichent le tirage ; « Passer l’animation » affiche directement le resultat. Le serveur donne la recompense des l’ouverture, meme si la fenetre est fermee. Casser un bloc non ouvert en survie le rend sans tirer de butin. En creatif, casser le bloc ne donne rien. Les pistons ne peuvent pas le deplacer.

Chacun a sa propre table de butin, modifiable en jeu par un operateur avec `/mmorpg butins` : onglet **Caisse d'aventure** ou **Lucky Block**, ajout d'un objet (recherche, objet tenu en main ou pieces d'or), quantites min/max, poids (la chance affichee = poids / somme des poids ; Maj + clic : pas de 10), suppression, puis **Enregistrer**. Les tables sont stockees dans `config/mmorpg/butins.json` (aussi modifiable a la main, puis `/mmorpg reload`). L'ecran du Lucky Block affiche les butins possibles et leurs chances.

## Affichage

Maintenir TAB pour la liste Eldoria (classe, niveau, latence ; jusqu’a 80 joueurs affiches comme la liste native). Le tableau Eldoria en haut a droite affiche l’or et les quetes. Il se deplace, se redimensionne et se masque dans l’editeur du HUD existant. Les niveaux et classes sont calcules par le serveur.

## Montures

Dans le menu **Familiers**, ouvrir **Collection de montures**. Selectionner un cheval, puis confirmer son achat avec l'or du jeu :

| Monture | Niveau requis | Prix unique | Vitesse par rapport au cheval standard |
| --- | ---: | ---: | ---: |
| Destrier du Voyageur | 10 | 250 or | 107 % |
| Courser de l'Aube | 40 | 1 500 or | 129 % |
| Etalon de l'Ombre | 75 | 6 000 or | 151 % |

La collection reste acquise apres mort et deconnexion. Invoquer sur la terre ferme, avec de la place autour du joueur. Clic droit pour monter, controles habituels du cheval pour avancer/sauter, Maj pour descendre. La selle est fournie et ne se retire pas. Le bouton **Renvoyer** retire la monture ; attendre cinq secondes entre deux invocations.

Seul le proprietaire peut monter. La monture invoquee disparait si son proprietaire meurt, se deconnecte, change de dimension ou s'eloigne de plus de 64 blocs. Il suffit de la reinvoquer depuis la collection. Les montures utilisent les modeles de chevaux du jeu, avec trois robes et vitesses distinctes.
