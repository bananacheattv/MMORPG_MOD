# Instructions de reprise pour Claude — interfaces illustrées Aeloria

## Demande de l’utilisateur

Utiliser les images de ce pack pour moderniser les interfaces évoquées : Métiers, Donjons, choix de classe, Quêtes, Montures/Familiers, Boutique et Chargements. Conserver l’identité **Minecraft MMORPG violet/or**, les angles arrondis, la transparence et une police lissée. L’inventaire et les chiffres doivent rester sobres et lisibles.

Tu travailles déjà sur le mod. Ce pack est préparé dans un checkout distinct : il n’altère ni ta branche de travail ni le launcher. **Récupère uniquement le commit de ce pack par cherry-pick depuis ta branche actuelle ; ne fusionne pas son ancienne base de gameplay par défaut.** Lis ensuite les instructions locales de ta branche et garde tes correctifs déjà réalisés. Aucun fichier Java, aucune configuration serveur ni aucun fichier du launcher n’est modifié par ce commit.

## Sources

- `art/ui-pack/images/` : dix PNG originaux, conservés sans retouche destructrice.
- `art/ui-pack/manifest.json` : dimensions réelles, alpha, SHA256, usage, zones UV des quatre classes.
- `art/ui-pack/prompts.json` : prompts exacts des sept nouvelles générations, outil intégré image_gen. Les trois images métier proviennent des générations précédentes approuvées dans cette conversation.
- [Galerie du pack](README.md).

Copier les seules images utilisées vers `src/main/resources/assets/mmorpg/textures/gui/illustrations/` dans ton implémentation, ou dériver des versions optimisées sans écraser les originaux. Les identifiants de ressources doivent rester en minuscules ASCII (`mmorpg:textures/gui/illustrations/...png`). Ne charger aucune image par URL réseau pendant le rendu.

## Règles communes de présentation

1. Les images sont de véritables assets, pas des captures de menus à afficher à la place de l’interface. Les boutons, titres, états sélectionnés, données et barres de progression restent dessinés et interactifs en code.
2. Le fond reste visible : ne pas étaler un énorme panneau flouté opaque sur tout le décor. Ajouter seulement un voile local sombre derrière les textes. Préserver l’architecture et les visages hors des grandes zones de texte.
3. Respecter les proportions. Utiliser un recadrage cover maîtrisé pour les bannières, un contain pour le portrait transparent et des UV explicites pour l’atlas des classes. Ne jamais étirer un personnage.
4. Réserver aux vrais textes du jeu les noms, niveaux, prix, soldes, récompenses, difficulté, temps et descriptions. Ils doivent rester localisables, sélectionnables ou accessibles selon les capacités de l’UI. Ne pas graver ces données dans de nouvelles images.
5. Les cadres arrondis et indicateurs de survol restent discrets. L’état sélectionné doit être identifiable autrement que par une légère variation de couleur : contour, marqueur et texte lisible.
6. Les sources ne sont pas toutes des puissances de deux. Lire leur largeur/hauteur depuis le manifeste ; ne pas supposer1024 ou2048 et utiliser les paramètres de texture adéquats. Vérifier la compatibilité du moteur avant optimisation. Conserver l’alpha du portrait et un mélange correct pour éviter les halos noirs.
7. Les images enrichissent les menus ; elles ne remplacent ni les modèles3D réels des objets ni les règles serveur. Ne pas modifier les statistiques, les restrictions de classe ou les permissions en intégrant l’habillage.

## 1 — Métiers

**Assets :** `professions_background`, `miner_card`, `fisher_card`, `farmer_card`.

- Présenter trois grandes cartes à image, clairement cliquables et alignées, avec Mineur, Pêcheur et Fermier ; niveau, XP et progression sont en code sous chaque carte.
- Le fond panoramique montre la mine, le lac et la ferme ; le bas calme permet de poser les cartes. Ne pas mettre un bloc opaque qui annule l’intérêt du décor.
- Les trois cartes existantes contiennent déjà leur titre doré. Soit afficher ce titre une seule fois, soit recadrer la partie haute `[u0=0,v0=0,u1=1,v1=.71]` pour conserver uniquement l’illustration et afficher le libellé en code. Éviter tout double titre. Le manifeste signale ce texte embarqué.
- **Vérifier l’existence du système de métiers sur ta branche.** Le pack ne crée pas de progression métier ni de récompenses. Si le système n’existe pas encore, ne pas fabriquer de niveaux ou de boutons sans action ; signaler le travail fonctionnel restant et brancher l’UI sur le système réel lorsqu’il est disponible.

## 2 — Donjons

**Asset :** `dungeon_crypt_cover`.

- Cible indicative : `client/screen/DungeonMasterScreen.java`, à adapter à l’état actuel de ta branche.
- Utiliser la scène en carte ou en en-tête, avec le niveau, la difficulté, les variantes et la mention de groupe provenant des données existantes.
- C’est une illustration conceptuelle de crypte : ne pas présenter la silhouette comme un boss nommé ou comme une capture fidèle d’un donjon existant.
- Une seule illustration de donjon est livrée. Ne pas la dupliquer sous plusieurs noms pour prétendre que chaque donjon a son visuel distinct ; l’utiliser comme illustration générique ou pour une crypte appropriée et conserver les visuels existants ailleurs.

## 3 — Choix de classe

**Asset :** `classes_atlas`.

- Cibles indicatives : `ClassSelectScreen.java` et `ClassMasterScreen.java`.
- Il y a quatre portraits côte à côte. Ordre **Guerrier, Mage, Archer, Paladin**. L’identifiant interne du Paladin reste `tank`/`TANK`.
- Lire `regions` du manifeste pour les rectangles pixels et UV normalisées : éviter les divisions entières qui perdent un pixel. Chaque carte doit cadrer son propre personnage, jamais afficher la planche entière compressée dans une carte.
- Le Paladin porte bien un **marteau et un bouclier**, pas une épée. Les silhouettes et équipements sont des concepts d’illustration ; ils ne remplacent pas les modèles équipés en jeu.
- Afficher le rôle, les attributs et la confirmation avec les vraies données de classe. Garder les conditions serveur et le parcours de changement de classe existants.

## 4 — Quêtes et dialogues

**Asset :** `quest_guide_portrait`, avec alpha réel.

- Cibles indicatives : `QuestGiverScreen.java`, `QuestLogScreen.java`, `QuestPanel.java`.
- Placer le portrait à gauche ; à droite, le dialogue propre au PNJ, les objectifs, récompenses et actions du système actuel. Laisser assez de place à la lecture et au défilement.
- Le personnage est une **guide conceptuelle non nommée**. L’associer seulement à une guide appropriée ou à une introduction neutre, avec une table de correspondance explicite. Ne pas le réutiliser automatiquement comme identité de tous les PNJ et ne pas renommer ni modifier leurs skins.
- Préserver l’éditeur de dialogues, les affectations de quêtes et le déplacement des PNJ réalisés sur ta branche. Aucun dialogue ou objectif ne doit devenir commun à tous les guides à cause de cet habillage.

## 5 — Montures et familiers

**Asset :** `mounts_pets_header`.

- Cibles indicatives : `MountsScreen.java`, `PetsScreen.java`.
- Employer cette image comme bannière d’accueil ou décoration du panneau. La galerie, l’état possédé/verrouillé et l’aperçu du modèle3D réel restent liés aux collections actuelles du joueur.
- Le cheval, le renard et le feu follet sont illustratifs. **Ne pas inventer leurs identifiants, une nouvelle monture ou un nouveau familier** pour justifier l’image, ni vendre une apparence qui ne correspond pas au modèle existant.
- Les cartes de collection doivent montrer leurs vrais modèles ou icônes spécifiques, pas des recadrages trompeurs de cette bannière pour chaque créature.

## 6 — Boutique

**Asset :** `shop_header`.

- Cibles indicatives : `PremiumStoreScreen.java` ; conserver la distinction avec `PvpStoreScreen.java` et `ShopScreen.java`.
- Utiliser la nature morte comme en-tête. Garder une grille de produits lisible avec les véritables icônes, prix, devises, quantités et descriptions du serveur.
- Les coffres, clés, charmes, potions et symbole de Lucky Block illustrent des catégories ; ce ne sont pas des modèles commerciaux exacts et ils ne doivent pas promettre un avantage ou un contenu absent.
- **Ne pas activer les paiements réels ni inventer d’offres ou de probabilités.** Conserver l’état et la configuration actuels du site et des achats. L’intégration graphique ne modifie pas les débits, contrôles et livraisons.

## 7 — Chargements et portails

**Asset :** `portal_loading_background`.

- Cibles indicatives : `PortalLoadingScreenMixin.java`, `AeloriaLoadingOverlayMixin.java`, helpers graphiques de chargement de ta branche.
- Conserver le château dans la partie supérieure droite et le portail à gauche. Poser le vrai logo et les textes de chargement dans la zone centrale/basse calme, avec un voile sombre léger si nécessaire.
- Utiliser du texte en code tel que « Passage dans le portail » / « Téléportation en cours… ». La progression doit refléter les étapes réelles ou rester indéterminée ; ne pas fabriquer une progression serveur.
- Conserver les demandes précédentes : pas de bouton Annuler réintroduit et pas d’écran rouge superposé par erreur. Ne pas retirer les messages d’erreur ni masquer une connexion échouée.
- Pour une variation par destination, le fond Métiers peut accompagner le spawn et la crypte un donjon adapté. Il n’y a pas de nouvelle image dédiée aux mines seules : ne pas annoncer un pack de destinations plus complet que ce qui est livré.

## Vérification avant livraison

- Vérifier les sept interfaces concernées dans les résolutions et échelles GUI effectivement supportées, dont1280x720 et1920x1080, ainsi qu’une petite fenêtre. Les images ne doivent masquer ni boutons ni données.
- Tester le recadrage des quatre classes, les libellés uniques des métiers, l’alpha du portrait et le passage du chargement au monde.
- Les listes, défilements, info-bulles et zones de clic doivent rester cohérents avec ce qui est dessiné. Préserver clavier, Échap, focus, droits OP et fonctionnement hors connexion des assets.
- Ne pas tester les générateurs Java/Gradle en recompilant pendant un client de QA lancé : un précédent essai a supprimé temporairement des classes chargées tardivement.
- Joindre des captures réelles des interfaces intégrées. Une image d’illustration seule ne prouve pas qu’une interface fonctionne.
- Ne pas redémarrer les serveurs s’il y a des joueurs connectés. Ce pack n’a pas été déployé et ne demande aucun changement de launcher.
