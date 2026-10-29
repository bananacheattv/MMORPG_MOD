# Complément prioritaire — dix interfaces et Yggdrasil

Demande utilisateur du 29 octobre 2026. Ces visuels complètent le pack ; ils ne constituent pas une implémentation Java. Appliquer sur la branche actuelle de Claude, sans fusionner l'ancienne base gameplay de la branche art. Ne toucher ni au launcher ni à la carte du monde (exclue).

## Travail demandé, écran par écran

| Interface | Images et rendu | Mise en page / comportement attendu |
|---|---|---|
| Quêtes | `quest_guide_portrait.png`, `quest_parchment.png`, `quest_master.png` | À droite : portrait du PNJ associé à la quête sélectionnée et parchemin avec titre, dialogue, objectifs et récompenses réels. Portrait guide seulement si cohérent avec ce PNJ ; ne pas attribuer le même visage à tous. Sans sélection/liste vide : Maître des Quêtes et message explicatif. Texte sombre sur parchemin clair, défilement dans sa zone intérieure. |
| Groupe | `group_banner.png` | Bannière en en-tête. Moitié basse : membres réels, têtes 3D issues de leur skin, classe, PV actuels/max et barre de vie. Prévoir états solo, plein, déconnecté et noms longs. Pas de faux membres décoratifs. |
| Guilde | `guild_hall_header.png` | Blason réel ou bannière configurable au centre, barre XP illustrée codée séparément avec valeurs serveur. Conserver liste/actions accessibles. Ne pas peindre une fausse progression dans le fond. |
| Arène | `arena_fire_cover.png`, `arena_dunes_cover.png` | Vignettes en haut des cartes feu/dunes. Noms, conditions et boutons sous l'image. Illustrations thématiques, pas captures exactes de la géométrie des cartes ; ne pas reconstruire les arènes à partir d'elles. Conserver cristal visible dans le recadrage. |
| Trade | vrais ItemStack, flèche dessinée dans le code | Grosses icônes, deux offres côte à côte et flèche d'échange au centre ; inventaire disponible sur toute la largeur en dessous. Dimensions visuelles, clics et glisser-déposer doivent partager le même calcul. Afficher quantité, tooltip, verrouillage/confirmation ; conserver les validations serveur. |
| Hebdo | `weekly_reward_chest.png` + rendu du monstre/objet réel | Trois cartes lisibles : une icône propre à l'objectif de chaque quête et un coffre récompense. Pas le même coffre à la place de tous les objectifs. Compteur, échéance et bouton restent en code. Si un objectif n'a pas de modèle disponible, icône explicite et tooltip plutôt qu'une cible inventée. |
| Succès | six PNG `medal_*.png` individuels | Corriger le mauvais écran/route portail : aucun fond de chargement dans les succès. Une médaille sur chaque carte, choisie selon sa catégorie ; états verrouillé/débloqué/progression. Les six fichiers sont des catégories réutilisables, pas une icône unique déjà produite pour chaque succès possible. |
| Cosmétiques | `cosmetics_stage.png` + vrais objets/modèles | Remplacer carrés de couleur par rendus reconnaissables de chaque cosmétique. À gauche, aperçu 3D du joueur avec skin et cosmétique sélectionné, rotation contrôlable ; pas seulement particules. Masquer temporairement les accessoires incompatibles et restituer l'état après fermeture. |
| Choix de classe | `classes_atlas.png`, régions du manifeste | Portrait/silhouette de la classe sur chaque carte ; Guerrier, Mage, Archer, Paladin. Respecter UV et proportions, description et sélection en code. Ne pas étirer les quatre portraits ensemble dans une carte. |
| Boutique | `shop_header.png` + rendu des articles réels | Grandes icônes à droite, nom, rareté, prix et description lisibles. Étiquette de rareté avec texte et couleur. Paiements restent désactivés tant que le site et le backend ne sont pas prêts. Aucun item promotionnel inventé à partir du décor. |
| Yggdrasil | `yggdrasil_tree.png` transparent | Arbre violet/émeraude avec cœur améthyste et veines dorées, présenté entier sur fond violet sombre discret. Réserver un panneau latéral lisible pour description/niveau/coût/actions existants. Si des nœuds de progression existent, les dessiner et les rendre cliquables séparément ; pas de mécanique nouvelle imposée par cette illustration. |

## Règles de composition

- Huit nouveaux fichiers séparés : pas de planche à découper. Les six médailles restent également individuelles.
- Transparence réelle conservée sur arbre, portraits, parchemin et coffre. Le parchemin possède une teinte extérieure semi-transparente : contrôler son rendu sur violet sombre, sans présumer un détourage parfaitement net.
- Arbre : mode contain et marges, jamais crop de la couronne ou des racines. Ne pas poser du petit texte directement sur son feuillage détaillé.
- Parchemin : contenu dans la zone normalisée approximative x=0.16..0.84, y=0.15..0.83 ; vérifier avec la police réelle. Défilement interne si le dialogue dépasse, aucune réduction illisible de la police.
- Cartes : préserver l'aspect, image séparée des boutons, dégradé discret sous les titres uniquement si nécessaire.
- Les têtes 3D, previews joueur/objets, barres XP/PV, flèche et étiquettes sont des rendus natifs ; aucune image statique ne doit simuler ces données.
- Sur petite fenêtre : colonnes adaptatives, défilement, taille minimale de police et zones de clic cohérentes. Éviter d'ajouter un grand décor pour simplement cacher une zone vide.

## Validation avant de dire terminé

Suivre [DISPLAY_QA.md](DISPLAY_QA.md). Captures réelles pour chacun de ces onze écrans, à 920×600, 1280×720 et 1920×1080 au minimum, et échelles GUI prises en charge. Tester états vides, longues descriptions, listes pleines, sélection, hover et clic après redimensionnement. Contrôler que Succès ouvre bien Succès, que les vignettes feu/dunes diffèrent et que les données de groupe/XP viennent du serveur. Les assets sont livrés et vérifiés ; toute intégration reste à réaliser et à tester dans Minecraft.
