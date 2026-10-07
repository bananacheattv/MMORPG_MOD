# Eldoria MMORPG — mod Minecraft 26.3 (NeoForge)

Mod MMORPG complet : 4 classes qui évoluent (niveaux 1 à 100), 28 compétences, un système de statistiques, 24 armes et 17 sets d'armure, des monstres et boss entièrement configurables, de l'artisanat, des familiers, des téléporteurs, des donjons, des quêtes et des PNJ, des marchands et une monnaie, des groupes, des cosmétiques animés et des interfaces sur mesure.

## Installation

1. Installer **NeoForge 26.3.0.39-beta** (ou plus récent) pour **Minecraft 26.3**. Minecraft 26.3 utilise Java 25.
2. Copier `build/libs/mmorpg-1.0.0.jar` dans le dossier `mods`.
3. Le mod doit être installé **sur le serveur et sur chaque client**.

## Touches (modifiables dans Options > Commandes > MMORPG)

| Touche | Action |
|---|---|
| **M** | Menu MMORPG (Personnage, Compétences, Quêtes, Groupe, Familiers, Cosmétiques, Bestiaire) |
| **R, B, G, H, J, V** | Compétences 1 à 6 de la barre de compétences |
| **U** | Modifier l'interface (déplacer / redimensionner / masquer les éléments affichés) |

**Inventaire** : l'inventaire de Minecraft est désactivé. La touche **E** ouvre l'onglet **Personnage** du menu, qui regroupe tout : équipement, artisanat 2×2, sac et barre rapide, portrait, attributs (répartition des points), statistiques et évolution de la classe.

**Ouvrir un menu** (PNJ, Forge Arcanique, Autel d'Invocation) : **clic gauche**. Le clic droit fonctionne aussi. Le **Téléporteur** s'utilise en **passant sous son arche**. Pour **casser** un de ces blocs, il faut faire **Maj + clic gauche**.

## Classes et évolutions

À la première connexion, un écran propose de choisir une classe. La classe évolue aux niveaux 25, 50, 75 et 100 : elle change de nom, gagne +5 % de PV, d'Attaque, de Puissance magique et de Défense, et débloque de nouvelles compétences.

| Classe | Niv. 1 | Niv. 25 | Niv. 50 | Niv. 75 | Niv. 100 |
|---|---|---|---|---|---|
| Guerrier | Guerrier | Berserker | Seigneur de Guerre | Champion Légendaire | Dieu de la Guerre |
| Mage | Mage | Sorcier | Archimage | Arcaniste Suprême | Avatar des Arcanes |
| Archer | Archer | Rôdeur | Tireur d'Élite | Sentinelle Sylvestre | Archer Divin |
| Tank | Tank | Gardien | Paladin | Rempart Sacré | Titan Immortel |

### Compétences (7 par classe, rangs 1 à 5)

Une compétence se débloque au rang 1 dès que son niveau est atteint. On gagne 1 point de compétence tous les 4 niveaux, et chaque rang ajoute +15 % d'efficacité.

- **Guerrier** : Frappe Puissante, Cri de Guerre, Maîtrise des Armes (passif), Tourbillon d'Acier (25), Charge Brutale (50), Rage Sanguinaire (75), Fureur du Dieu de la Guerre (100, ultime).
- **Mage** : Boule de Feu, Nova de Givre, Flux Arcanique (passif), Éclair en Chaîne (25), Transfert Arcanique (50), Bouclier de Mana (75), Météore Céleste (100, ultime).
- **Archer** : Tir Perçant, Pluie de Flèches, Œil de Lynx (passif), Saut Acrobatique (25), Flèche Explosive (50), Volée de Flèches (75), Tempête Divine (100, ultime).
- **Tank** : Coup de Bouclier, Provocation, Peau de Fer (passif), Forteresse (25), Onde de Choc (50), Aura Sacrée (75), Rempart du Titan (100, ultime).

## Statistiques

- **8 attributs**, deux par classe. On gagne 3 points à répartir par niveau, en plus de la croissance propre à la classe ; l'équipement d'une classe donne aussi son second attribut.

  | Classe | Attributs | Effets principaux |
  |---|---|---|
  | Guerrier | Force, **Férocité** | Attaque, PV ; dégâts critiques, vol de vie |
  | Archer | Agilité, **Dextérité** | Attaque, esquive, vitesse ; critique, vitesse d'attaque |
  | Mage | Intelligence, Esprit | Puissance magique, mana ; régénération, recharges |
  | Tank | Vitalité, **Endurance** | PV, défense ; défense, régénération de vie |
- **14 statistiques dérivées** : PV, Mana, Attaque, Puissance magique, Défense, Critique, Dégâts critiques, Esquive, Vitesse, Vitesse d'attaque, Régénération de vie et de mana, Vol de vie, Réduction de recharge.
- **Ordre de calcul** : attributs → formules → bonus d'équipement, de familier et de sets → multiplicateur d'évolution → bonus en % → plafonds (critique 75 %, esquive 45 %, recharge 40 %…).
- **Réduction de dégâts** : `DEF / (DEF + 200 + 10 × niveau de l'attaquant)`, plafonnée à 80 %.
- **Points de vie** : ils sont gérés par le mod. Ils ne sont donc pas limités à 1024 comme la santé de Minecraft, et les cœurs vanilla sont remplacés par la barre de vie du HUD.

## Équipement

- **24 armes** (6 par classe : épées et haches, bâtons, arcs, marteaux), du rang Commun à Mythique. Le clic droit avec un bâton lance un projectile magique. Les arcs ne consomment pas de flèches.
- **Modèles 3D Blockbench** pour les bâtons, les arcs (avec 3 étapes de traction), les marteaux et les épées. Seule la Hache du Berserker garde un sprite 2D. Les projets éditables et leurs scripts sont dans `art/` (voir `art/LISEZMOI.md`).
- **17 sets d'armure** (68 pièces) : 4 par classe et 1 universel, le « Néant Primordial » (niv. 100). Bonus quand on porte 2 et 4 pièces d'un même set. Chaque set s'affiche sur le joueur avec un modèle 3D (épaulières, couronnes, cristaux…) issu de `art/armures_blockbench/`.
- **Prérequis** : chaque objet a une classe et un niveau minimum. Sans les remplir, l'objet ne donne aucune stat.
- **Amélioration +1 à +20** à la Forge Arcanique : +6 % de stats par niveau. La chance de réussite baisse de 100 % (+1) à 5 % (+20) ; à partir de +5, un échec fait perdre un niveau. Pierre d'Amélioration pour +1 à +8, Pierre Supérieure pour +9 à +20.
- **Trèfles d'amélioration** : chacun **garantit la réussite**, mais seulement dans son palier de 4 niveaux. Il est consommé à l'utilisation, et l'interface le propose automatiquement quand on en possède un.

  | Palier | Trèfle |
  |---|---|
  | +1 → +4 | Trèfle de Chance |
  | +5 → +8 | Trèfle à Quatre Feuilles |
  | +9 → +12 | Trèfle Doré |
  | +13 → +16 | Trèfle Céleste |
  | +17 → +20 | Trèfle Divin |

  On les trouve sur les monstres (palier selon leur niveau) et les boss (le Trèfle Divin sur l'Avatar du Néant), on les fabrique à la Forge, et le marchand vend les deux premiers.

## Monstres et boss

| Monstre | Niveaux | | Boss | Niveau |
|---|---|---|---|---|
| Gobelin | 1-12 | | Roi Gobelin | 20 |
| Loup Sombre | 5-18 | | Liche Ancienne | 45 |
| Squelette Maudit | 15-32 | | Seigneur Démon Ignis | 65 |
| Orc Guerrier | 25-45 | | Titan de Glace | 80 |
| Élémentaire de Feu | 40-60 | | Avatar du Néant | 100 |
| Spectre de Givre | 50-70 | | | |
| Golem de Cristal | 60-80 | | | |
| Chevalier du Néant | 75-95 | | | |

Chaque boss reste dans sa **base d'invocation** (12 blocs autour de son point d'apparition, réglable avec `abilities.leashRadius`) : il ne poursuit pas un joueur au-delà et y revient s'il en est sorti. Les boss ont 3 phases et des capacités spéciales : frappes au sol, invocations, projectiles à tête chercheuse, météores, trou noir… Ils ont des modèles 3D animés (`art/boss_blockbench/`) : chaque capacité a son animation et ses dégâts tombent à l'instant de l'impact ; la mort est animée. On les invoque en utilisant un **objet d'invocation** (fabriqué à la Forge) sur un **Autel d'Invocation**. Les monstres vanilla reçoivent aussi un niveau et des stats RPG.

### Tout est personnalisable : `config/mmorpg/mobs.json`

Pour chaque monstre ou boss, on peut régler :
- le nom affiché et les niveaux min/max ;
- les PV, l'attaque et la défense (valeur de base + gain par niveau) ;
- la vitesse, la taille, le multiplicateur d'XP ;
- l'apparition naturelle : biomes ou tags de biomes, dimensions, obscurité, altitude, poids, taille des groupes ;
- le butin : objet, chance, quantité ;
- les capacités des boss : recharges et multiplicateurs.

`config/mmorpg/general.json` contient les réglages généraux : multiplicateurs d'XP et de butin, mise à l'échelle des monstres vanilla, perte d'XP à la mort, PvP des compétences, fréquence d'apparition, annonces…

**PvP** : `skillsHitPlayers` (activé par défaut) permet aux compétences de blesser les autres joueurs, si le PvP du serveur est actif (règle `pvp`). Les membres d'un même groupe ne peuvent jamais se blesser.

Après une modification, `/mmorpg reload` recharge la configuration sans redémarrer.

## Artisanat — Forge Arcanique

La Forge propose 123 recettes en 6 catégories : Armes, Armures, Matériaux, Consommables (potions de soin et de mana, élixir d'XP, parchemins, orbe de renaissance, coffre cosmétique), Invocations et Familiers. Elles utilisent les matériaux lâchés par les monstres et les boss, plus des pièces d'or. L'onglet **Amélioration** sert à améliorer l'équipement.

Recettes à l'établi vanilla :
- **Forge Arcanique** : 5 lingots de fer, 1 fourneau, 3 pierres taillées.
- **Autel d'Invocation** : diamant, or, obsidienne, pierres taillées.

## Familiers

8 familiers, de Commun à Mythique, dont le Phénix Doré et le Dragonnet du Néant. On les obtient avec des œufs, lâchés par les boss ou fabriqués à la Forge. Le familier invoqué accompagne le joueur et donne des bonus de stats. Il gagne de l'expérience à chaque victoire, jusqu'au niveau 10 (bonus ×2,35).

Les familiers sont des **modèles 3D animés** (Blockbench, `art/familiers_blockbench`) qui se comportent selon leur espèce :
- **en vol permanent** à hauteur d'épaule : Feu follet, Fée lumineuse, Dragonnet du néant (battements d'ailes sur place, vol penché en déplacement) ;
- **au sol** à côté du joueur, en suivant le relief : Bébé slime (bonds), Loup spectral (marche puis course), Golem de poche ;
- **se posent** quand le joueur s'arrête et s'envolent dès qu'il repart : Chouette sage, Phénix doré.

Immobiles un moment, ils se reposent : le loup s'assoit puis s'endort, le golem et la chouette s'endorment, le slime s'aplatit, le feu follet entre en canalisation. À l'invocation, ils jouent une animation de joie.

## Téléporteurs, villes et donjons

Les téléporteurs sont un **outil d'administration** pour installer les villes et les donjons. Ils n'ont pas de recette : on les prend dans l'onglet créatif du mod ou avec `/give`.

- **Opérateurs** : poser un Téléporteur ouvre sa configuration (nom, catégorie Ville / Donjon / Point de passage, niveau minimum). Accroupi + clic droit (ou traverser l'arche accroupi) permet de le reconfigurer, et Maj + clic gauche de le retirer.
- **Joueurs** : **passer sous l'arche** d'un téléporteur découvre le lieu et ouvre la liste des destinations déjà découvertes (une fois par passage ; à l'arrivée, on est déposé devant l'arche). Un clic sur le téléporteur rappelle simplement de passer sous l'arche. Ils ne peuvent ni le poser, ni le casser, ni le configurer. Il est aussi insensible aux pistons et aux explosions.
- **Parchemin de Téléportation** : voyager depuis n'importe où (il est consommé).
- `/mmorpg ville <nom>` crée une place de ville avec son téléporteur, un Maître des quêtes et une Marchande.
- `/mmorpg donjon <type>` construit un donjon complet : hall avec téléporteur, salles avec spawners et coffres, salle de boss avec autel. Les 5 types sont `crypte_gobeline`, `catacombes`, `forteresse_infernale`, `sanctuaire_glace` et `citadelle_neant`.

## Quêtes et PNJ

- Les **Maîtres des quêtes** (« ! » jaune au-dessus de la tête, « ? » quand une quête est à rendre) proposent 20 quêtes : chasse, collecte d'objets, paliers de niveau, chaînes de quêtes, et 3 quêtes **journalières**.
- Récompenses : XP, or, objets, et parfois un familier ou un cosmétique.
- Jusqu'à 10 quêtes actives. Le suivi s'affiche en haut à droite de l'écran, et le journal complet est dans l'onglet **Quêtes** du menu (M).
- Tout est modifiable dans `config/mmorpg/quests.json` : objectifs (`kill`, `collect`, `level`), niveau requis, quêtes prérequises, PNJ qui la propose, récompenses.

## Marchands et or

- L'or est rangé dans une **bourse** : les pièces d'or ramassées y sont versées automatiquement. Le solde s'affiche sous la barre de vie.
- Les **Marchands** vendent potions, parchemins, pierres d'amélioration, œufs de familier, coffres cosmétiques et fournitures. Certains articles demandent un niveau minimum. Ils rachètent les matériaux de monstres et l'équipement : le prix dépend de la rareté et du niveau d'amélioration. Un bouton vend d'un coup tous les exemplaires d'un même objet.
- Articles et prix : `config/mmorpg/shop.json`.
- La Forge Arcanique utilise aussi l'or de la bourse.

## Groupes

- Jusqu'à **5 joueurs** par groupe, depuis l'onglet **Groupe** du menu ou avec `/groupe`.
- L'XP des monstres est partagée entre les membres proches (48 blocs), avec un bonus de +10 % par membre supplémentaire. Les quêtes de chasse avancent pour tout le groupe.
- Les membres ne peuvent pas se blesser entre eux. Leurs PV et mana s'affichent à gauche de l'écran.

## Cosmétiques animés

15 cosmétiques en 4 catégories :
- **Auras** : flammes, givre, arcanes, vortex du Néant ;
- **Ailes battantes** : angéliques, démoniaques, de fée, de dragon ;
- **Halos** : doré, couronne de givre, couronne infernale ;
- **Traînées** : cœurs, étoiles, notes, braises.

Certains se débloquent avec le niveau, les autres dans les Coffres Cosmétiques. Ils sont visibles par tous les joueurs.

## Commandes d'administration (`/mmorpg`, opérateur)

| Commande | Effet |
|---|---|
| `niveau <joueur> <niv>` | fixe le niveau |
| `xp <joueur> <montant>` | donne de l'expérience |
| `classe <joueur> <classe>` | change la classe (`aucune` rouvre l'écran de choix) |
| `points <joueur> <n>` | donne des points d'attribut |
| `reset <joueur>` | réinitialise toute la progression |
| `soin <joueur>` | restaure PV, mana et recharges |
| `info <joueur>` | affiche toutes les statistiques |
| `mob <type> [niveau]` | fait apparaître un monstre |
| `boss <type>` | fait apparaître un boss |
| `donjon <type>` | construit un donjon |
| `ville <nom>` | construit une place de ville |
| `teleporteurs` | liste les téléporteurs du monde |
| `familier <joueur> <id\|tous>` | débloque un ou tous les familiers |
| `cosmetique <joueur> <id\|tous>` | débloque un ou tous les cosmétiques |
| `pnj quetes <nom>` / `pnj marchand <nom>` | fait apparaître un PNJ (`@0`…`@3` avant le nom pour choisir son apparence) |
| `pnj supprimer` | supprime les PNJ dans un rayon de 4 blocs |
| `or <joueur> <montant>` | donne de l'or |
| `quetes <joueur> reset` | réinitialise les quêtes d'un joueur |
| `reload` | recharge la configuration |

## Commandes des joueurs

| Commande | Effet |
|---|---|
| `/groupe inviter <joueur>` | invite un joueur dans son groupe |
| `/groupe accepter` / `/groupe refuser` | répond à une invitation |
| `/groupe quitter` | quitte le groupe |
| `/groupe exclure <joueur>` / `/groupe chef <joueur>` | gestion du groupe (chef seulement) |
| `/g <message>` | message au groupe |
| `/or` | affiche le contenu de la bourse |
| `/payer <joueur> <montant>` | donne de l'or à un autre joueur |

## Options d'affichage (client)

### Interface modifiable (touche **U**, ou bouton en haut à droite du menu **M**)

Tous les éléments affichés à l'écran se déplacent et se règlent :
- **du mod** : cadre du joueur, effets du personnage, cadre de la cible, membres du groupe, suivi des quêtes, barre de boss, barre de sorts, annonces ;
- **de Minecraft** : barre d'objets (avec le nom de l'objet), barre d'expérience, faim / air / monture, effets de potion, barres de boss vanilla, tableau des scores, message d'action, messages du chat.

Dans l'éditeur : **glisser** pour déplacer (aimantation aux bords, au centre et aux autres éléments ; **Maj** pour la désactiver), **molette** pour la taille (50 % à 200 %), **clic droit** pour masquer / afficher, **clic molette** ou **R** pour remettre un élément à sa place, **flèches** pour ajuster au pixel (Maj : 10 px). Les éléments vides (cible, groupe, boss, annonces…) affichent un aperçu pour pouvoir être placés. **Échap** ou **Terminé** enregistre, **Annuler** rétablit la disposition d'avant, **Tout réinitialiser** revient à l'interface d'origine.

La disposition est enregistrée par joueur dans `config/mmorpg-hud.json` ; chaque position est accrochée au bord ou au centre le plus proche, elle reste donc correcte si la fenêtre change de taille. Le chat ouvert pour écrire reste à sa place habituelle (seuls les messages reçus se déplacent).

### Options

Dans Mods > Eldoria MMORPG > Configurer, on peut activer ou désactiver :
- les dégâts flottants ;
- les barres de vie au-dessus des monstres ;
- l'affichage de ses propres cosmétiques en vue à la première personne.

## Développement

- `./gradlew build` génère le mod dans `build/libs/`.
- `python tools/generate_assets.py` régénère toutes les ressources : textures procédurales, modèles, traductions, recettes et tags.
- `./gradlew runClient -PquickWorld=<monde>` ouvre directement une sauvegarde de `run/saves`.
- `./gradlew runClient -PquickWorld=<monde> -Pshowcase` lance la vitrine automatique : elle ouvre chaque interface et enregistre des captures dans `run/screenshots`. Variantes : `-Pshowcase=quetes` (quêtes et marchand), `-Pshowcase=clic` (menus au clic gauche). Réservée au développement, elle est sans effet en jeu normal.
- Test à deux joueurs : `./gradlew runServer`, puis `./gradlew runClient -PmpServer=localhost -Pusername=Alice -Pshowcase=mpA` et la même commande avec `-Pusername=Bob -Pshowcase=mpB`.

## Launcher auto-mis à jour

À chaque push sur `main`, le workflow `Release launcher` compile le mod et met à jour la release GitHub
[`latest`](https://github.com/bananacheattv/MMORPG_MOD/releases/tag/latest).

Joueurs : télécharger `EldoriaLauncher-Windows.zip` (Java inclus), dézipper, lancer `EldoriaLauncher.exe`
(ou `EldoriaLauncher.jar` avec Java 21+). Le launcher :
- se met à jour tout seul (son cœur `launcher-core.jar` est retéléchargé quand il change) ;
- synchronise les mods dans `%APPDATA%\.eldoria\mods` (les mods ajoutés à la main sont gardés) ;
- installe NeoForge si besoin et crée le profil « Eldoria MMORPG » dans le launcher Minecraft officiel ;
- ouvre le launcher Minecraft (connexion Microsoft gérée par Mojang).

Code : `launcher/` (amorce `bootstrap/`, cœur `core/`, scripts de build `scripts/`).
