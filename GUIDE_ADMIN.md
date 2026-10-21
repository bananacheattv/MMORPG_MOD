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

## Caisses (blocs 3D)

- 7 caisses dans l'onglet créatif « Objets » : vote, quête, commune, rare, épique, légendaire, mythique.
- Posez-les en créatif : incassables en survie, réutilisables à l'infini.
- Clic droit avec la clé correspondante (`mmorpg:cle_vote`, `cle_quete`, `cle_commune`, `cle_rare`, `cle_epique`, `cle_legendaire`, `cle_mythique`) : le couvercle s'ouvre et le butin est tiré au sort.
- Accroupi + clic droit : aperçu du contenu et des chances.
- Butins modifiables en jeu : `/mmorpg butins` (un onglet par caisse + Lucky Block).
- Cosmétiques (coffre cosmétique) et familiers (œufs) ne s'obtiennent **que** dans les caisses et les Lucky Blocks.
- Clé de vote : à donner par votre plugin de vote, par ex. `/give <joueur> mmorpg:cle_vote 1`.

## Évolutions de classe

- Les évolutions (niv. 25, 50, 75, 100) se débloquent auprès du **Maître des classes** via des quêtes.
- Créer le PNJ : `/mmorpg pnj maitre_classes` (groupe `maitre_classes`).
- Quêtes par défaut : `evolution_1` à `evolution_4`, modifiables dans l'éditeur de quêtes (onglet récompenses → « Évolution de classe »).
- Les joueurs déjà évolués gardent leur palier.

## Montures 3D

- 12 montures (Sanglier, Loup de givre, Cerf sylvestre, Tortue de cristal, Chèvre céleste, Lézard de lave, Raptor des sables, Ours runique, Scorpion des dunes, Félin du vide, Araignée des cavernes, Hippogriffe).
- Obtenues uniquement via les sceaux `mmorpg:sceau_<monture>` (caisses / Lucky Blocks), sans niveau minimum.
- L'Hippogriffe vole : avancer en regardant vers le haut = monter, vers le bas = descendre, saut = battement d'ailes.
- Modèles régénérables : `python3 tools/gen_mounts.py`.

## Mannequins d'entraînement

- 3 variantes : **standard** (paille, aucune défense), **blindé** (forte défense physique), **magique** (forte résistance magique).
- Objets : `mmorpg:mannequin_entrainement`, `mannequin_blinde`, `mannequin_magique` (onglet créatif « Objets » ; recettes à la Forge Arcanique, catégorie Consommables). Clic droit sur un bloc pour le poser.
- Commandes : `/mmorpg mannequin <standard|blinde|magique>` pose un mannequin à ta position ; `/mmorpg mannequin supprimer` retire ceux dans un rayon de 10 blocs. Un joueur créatif accroupi qui le frappe le retire aussi.
- Il est immortel, immobile, non poussable et ne riposte jamais. Il subit les dégâts du mod comme un monstre (défense, critiques, menace du Paladin, dégâts flottants) sans perdre de vie. Les dégâts d'environnement (chute, feu, vide) ne comptent pas.
- Panneau au-dessus de lui : DPS (fenêtre glissante), total, plus gros coup, critiques. **Accroupi + clic droit** : remise à zéro.
- Réglages : `config/mmorpg/mannequin.json` (niveau affiché, fenêtre du DPS, portée du panneau, défense physique / magique de chaque variante). `/mmorpg reload` les recharge.
- Modèles et animations (Blockbench) : `art/mannequin_blockbench/` (`python generer_mannequin.py`).

## Hub d'entrée, cinématique et essai des classes

- `/hub set` (ou `/mmorpg hub`) construit le hub dans la **dimension dédiée `mmorpg:hub`** (origine fixe (0, 96, 0), peu importe ta position ; la dimension est définie par `data/mmorpg/dimension/hub.json` et fonctionne sur un monde existant) : place de 31 blocs de rayon avec rotonde et bassin, 4 pavillons de classe, portail, Téléporteur, arène d'épreuve, aire d'entraînement, Maître des classes. Il **fixe le point d'apparition du monde dessus**. La construction bloque brièvement le serveur ; tant que le hub n'existe pas, il n'y a pas de cinématique et le jeu reste inchangé. Le **point d'apparition du monde** est déplacé dans le hub ; l'ancien point est conservé comme destination « Monde principal » du Téléporteur du hub (créée une seule fois, éditable comme un point de passage). `/hub` y ramène les joueurs. Reconstruire au même endroit remplace PNJ, mannequins et textes sans doublon.
- `/hub` (tous les joueurs) ramène au point d'apparition du hub (met fin à un essai en cours). `/hub reset <joueur>` (opérateur) remet un joueur à zéro (cinématique, essais, classe). `/class set <joueur> <classe>` (opérateur) change la classe d'un joueur (identique à `/mmorpg classe`).
- **Réglages** dans `config/mmorpg/general.json` : `hubCinematic` (cinématique à la première arrivée ; `false` = pas de cinématique, rejouable seulement si activée), `hubTrialZone` (zone d'essai des classes ; `false` = choix de classe direct auprès des Maîtres, sans essai), `hubProtection` (pas de casse/pose de blocs, de PvP ni de monstres hostiles dans le hub).
- **Première arrivée dans le hub** (par défaut, réglage `hubCinematic`) : la cinématique démarre toute seule la première fois qu'un joueur entre dans le hub, par n'importe quel chemin (nouvelle connexion sans classe, `/hub`, Téléporteur, reconnexion dans le hub). Un seul plan de caméra continu (≈ 66 s) : survol de la citadelle, passage devant chacun des quatre pavillons de classe, puis retour exactement au regard du joueur. Sous écran de chargement, le rendu du parcours est pré-chauffé pour éviter toute saccade. Espace ou Échap la passe.
- `/mmorpg cinematique [joueur]` la rejoue (les Maîtres de classe proposent aussi de la revoir).
- **Essai d'une classe** : un Maître de classe permet d'essayer sa classe au niveau 1, 25, 50, 75 ou 100 dans la salle d'épreuve, avec arme, panoplie et compétences correspondantes. Rien ne se gagne pendant l'essai (ni XP, ni or, ni butin, ni progression de quête). À la fin (sortie de la salle, PNJ, déconnexion, changement de dimension, plantage puis reconnexion), tout le personnage est restauré à l'identique (classe, niveau, XP, attributs, sac, équipement, barre de sorts, familier et monture actifs).
- **Choix définitif** : seulement auprès d'un Maître de classe et seulement après avoir essayé la classe (les opérateurs peuvent choisir directement). Le Téléporteur du hub reste bloqué tant qu'aucune classe n'est choisie.
- `/mmorpg essai fin [joueur]` met fin de force à un essai (restauration complète).
- Blocs décoratifs : l'onglet créatif « Eldoria : bâtisseur » (19 matériaux + marches/dalles, ex. `/give @s mmorpg:marbre_dore`). Régénération des textures : `python tools/generate_assets.py` (inclut `gen_decor.py` et `gen_font.py`).
- Tests automatiques du hub (client de développement) : `./gradlew runClient -Pshowcase=hub` (essai, restauration, accès, protection, `/hub`), `-Pshowcase=cinematique` (caméra, saut, verrou), `-Pshowcase=hubdesign` (revue visuelle sous 16 angles + vues de nuit, captures `showcase_15xx_*`).
- Réglages : `config/mmorpg/cinematique.json` (format version 2 : un **parcours** global d'images clés horodatées `{t, camera, cible, fov}` en secondes, interpolées par courbes d'Hermite pour un mouvement continu ; les plans ne portent que les titres, sons, particules et la mise en scène des Maîtres ; coordonnées relatives à la place centrale du hub). Un fichier d'un ancien format est sauvegardé en `cinematique.json.ancienne` puis remplacé. Supprimer le fichier pour revenir au parcours par défaut.

