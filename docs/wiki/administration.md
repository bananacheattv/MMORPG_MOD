# Administration et personnalisation

[Accueil du wiki](README.md) · [Commandes et aide](commandes-aide.md)

Cette page s’adresse aux OP. Le grade affiché dans le chat n’accorde pas le statut opérateur.

`/amenagement outilpnj` donne l’outil de PNJ. Au spawn : Maj + clic droit sélectionne un guide, puis clic droit sur un sol solide le déplace. L’éditeur permet aussi de tourner le PNJ, de modifier son dialogue et ses informations. Les quêtes et dialogues des guides doivent rester affectés au bon PNJ.

`/amenagement outilarmure` donne l’outil de porte-armure. Le panneau Skin / 3D permet de choisir un modèle ou de saisir un pseudo Minecraft pour son skin. Les joueurs ordinaires ne peuvent ni prendre l’équipement des stands ni retirer ou tourner les objets des cadres au spawn.

`/arene victoire rouge` ou `/arene victoire bleu` termine le match avec l’équipe choisie ; un joueur cible peut être ajouté. Les entraînements donnent un bilan mais aucun point.

**Écran des fonctionnalités** : `/fonctionnalites` (opérateurs, aussi dans le Menu Aeloria) ouvre les 35 interrupteurs groupés par catégorie (Combat, Économie, Progression, Social, Collection, Monde), avec l'état activée/désactivée et une bascule au clic. Permission revérifiée par le serveur à chaque clic, effet immédiat sur le réseau, changement écrit dans `admin-actions.log` et le journal de la base ; les onglets du menu apparaissent ou disparaissent chez les joueurs. `/fonctionnalite` liste les interrupteurs. `/fonctionnalite donjons desactiver` puis `activer` modifie une activité sans redémarrage. Les états sont persistants, partagés entre les serveurs sous une seconde et conservés dans `coordination/features.json`. Une activité désactivée ferme ses nouvelles interactions ; les parties engagées peuvent se terminer. Les interrupteurs couvrent notamment arènes, donjons, drones, boutiques, quêtes, sacs, HDV, guildes, compétences, bouclier, coffres, Lucky Blocks, montures, familiers, mines et plaines.

## Gestion des joueurs

`/adminjoueurs` (OP) ouvre le menu de gestion complète d'un joueur : liste à gauche (en ligne, et hors ligne si la base du réseau est activée), puis quatre pages.

- **Résumé** : niveau, expérience, classe, palier d'évolution, points d'attributs, or, or en banque, PV, mana, titre, rang, emplacements HDV, mines et plaines ouvertes, familier actif, quête suivie, plus des raccourcis vers chaque catégorie.
- **Données** : arbre de toutes les données du joueur (compétences, métiers, succès, quêtes, familiers, montures, cosmétiques, bestiaire, sacs à dos, recettes de forge...). On modifie une valeur, on ajoute ou supprime une entrée, on vide un groupe. Les valeurs sont bornées au chargement (niveau 1 à 100, etc.).
- **Objets** : inventaire, Ender, banque (retirer un objet), annonces de l'hôtel des ventes (annuler, l'objet est rendu au vendeur), donner un objet.
- **Actions** : soigner, tuer, expulser, bannir/gracier, réduire au silence, téléporter (vers ou depuis le joueur), mode de jeu, quitter groupe ou guilde, réinitialiser attributs, classe, quêtes, métiers ou tout, vider inventaire, Ender ou banque. Les actions destructrices demandent une seconde pression pour confirmer.

Le serveur revérifie la permission d'OP à chaque requête et journalise chaque modification : console, `config/mmorpg/admin-actions.log` et journal de la base (type `admin`). Un joueur hors ligne n'est modifiable que si la base du réseau est active et s'il n'est connecté nulle part ; inventaire, Ender, soin, téléportation et expulsion exigent qu'il soit sur ce serveur. La sourdine est enregistrée dans `config/mmorpg/muted.txt`.

Pour les commandes de construction, configuration, sécurité et réseau, utilise les guides de référence : [Administration](../../GUIDE_ADMIN.md), [Serveur](../../GUIDE_SERVEUR.md), [Réseau](../../RESEAU_AELORIA.md), [Portails et annulation](../../PORTAILS_ET_UNDO.md).
