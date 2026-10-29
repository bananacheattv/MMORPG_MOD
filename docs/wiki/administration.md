# Administration et personnalisation

[Accueil du wiki](README.md) · [Commandes et aide](commandes-aide.md)

Cette page s’adresse aux OP. Le grade affiché dans le chat n’accorde pas le statut opérateur.

`/amenagement outilpnj` donne l’outil de PNJ. Au spawn : Maj + clic droit sélectionne un guide, puis clic droit sur un sol solide le déplace. L’éditeur permet aussi de tourner le PNJ, de modifier son dialogue et ses informations. Les quêtes et dialogues des guides doivent rester affectés au bon PNJ.

`/amenagement outilarmure` donne l’outil de porte-armure. Le panneau Skin / 3D permet de choisir un modèle ou de saisir un pseudo Minecraft pour son skin. Les joueurs ordinaires ne peuvent ni prendre l’équipement des stands ni retirer ou tourner les objets des cadres au spawn.

`/arene victoire rouge` ou `/arene victoire bleu` termine le match avec l’équipe choisie ; un joueur cible peut être ajouté. Les entraînements donnent un bilan mais aucun point.

`/fonctionnalite` liste les interrupteurs. `/fonctionnalite donjons desactiver` puis `activer` modifie une activité sans redémarrage. Les états sont persistants, partagés entre les serveurs sous une seconde et conservés dans `coordination/features.json`. Une activité désactivée ferme ses nouvelles interactions ; les parties engagées peuvent se terminer. Les interrupteurs couvrent notamment arènes, donjons, drones, boutiques, quêtes, sacs, HDV, guildes, compétences, bouclier, coffres, Lucky Blocks, montures, familiers, mines et plaines.

Pour les commandes de construction, configuration, sécurité et réseau, utilise les guides de référence : [Administration](../../GUIDE_ADMIN.md), [Serveur](../../GUIDE_SERVEUR.md), [Réseau](../../RESEAU_AELORIA.md), [Portails et annulation](../../PORTAILS_ET_UNDO.md).
