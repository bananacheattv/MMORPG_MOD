# Wiki animé, guide et éclairage — bêta 0.0

- **Wiki** : chaque page possède une démonstration 3D en trois étapes. Lecture/pause, étapes précédente/suivante, rejouer, glissement pour tourner et molette pour zoomer. Le bouton « Fiche détaillée » conserve les explications écrites. Les scènes utilisent les modèles et textures du jeu dans un rendu isolé, sans modifier le monde.
- **Lumières invisibles** : chaque bloc génère jusqu'à 26 relais autour de lui. La lumière reste au maximum Minecraft de 15, mais sa couverture théorique passe de 4 089 à 90 045 blocs en espace ouvert (environ ×22). Murs, limites de monde, hauteur et chunks non chargés réduisent cette couverture. Les relais ne remplacent ni constructions, ni eau, ni lumières manuelles ; ils disparaissent après suppression de la dernière source qui les alimente. La mise à jour des relais prend jusqu'à 10 secondes en zone chargée. Aucun chunk n'est forcé.
- **Montures posées** : correction de la vraie méthode de sauvegarde des entités. Les décorations conservent position/orientation après rechargement. Les montures invoquées pour voyager restent temporaires. Les anciennes montures déjà perdues doivent être reposées.
- **Mascotte** : modèle ×3, gravité et marche au sol, suivi à 3–5 blocs avec recherche de chemin. En cas de très grande séparation, retour à un emplacement sûr ; elle ne traverse pas les murs pour suivre. Les rappels présentent les missions et réactivent leur suivi quand il n'a pas été masqué. Une mascotte forcée pour un ancien joueur présente aussi le serveur, sans réinitialiser sa progression.
- **Dialogue** : portrait 3D du PNJ ou de la mascotte, pagination automatique des longs textes. Dans **U**, déplacer/agrandir/masquer « Dialogue et portrait du PNJ ». F1 masque également le dialogue.
- **Création de quêtes** : dans Objectifs, choisir « Parler à un autre PNJ », puis sélectionner le personnage par nom et zone. La flèche suit sa position actuelle, y compris après déplacement. Un véritable clic sur ce PNJ valide l’objectif une fois ; parler au donneur, à un autre personnage ou envoyer une action cliente ne suffit pas. Le donneur de quête et le PNJ à rencontrer sont indépendants.
- **Armes gratuites** : voir [la sélection avec liens et licences](ARMES_GRATUITES.md). Le site demande une connexion pour télécharger ; aucun modèle tiers n'est annoncé installé sans réception de ses fichiers.

## Validation

Tests serveur natifs `runServer -PlightChecks` : niveaux/clamp/permissions, absence de collision, 26 relais, murs, conservation du terrain, recouvrement de deux lumières et nettoyage, sauvegarde complète/rechargement d'une monture posée, monture temporaire non sauvegardée, sol et distance minimale de la mascotte.

L'objectif « Parler à un autre PNJ » est testé par une interaction native : bon/mauvais personnage, distance, refus de simulation par une action cliente, progression plafonnée, sauvegarde/rechargement et guidage. La compilation complète et la suite `runServer -PnetworkChecks` passent également (quêtes, forge, portails, guildes, arènes, transferts et inventaires).

Les captures privées `-Pshowcase=guide_polish` couvrent le wiki, les étapes, le dialogue, le menu U et F1. La version publique reste **bêta 0.0**.
