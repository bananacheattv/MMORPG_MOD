# Aeloria — Commandes et fonctionnalités

Référence unique du mod (NeoForge, Minecraft 26.3), tirée du code. Détails : [wiki](wiki/README.md), [GUIDE_ADMIN](../GUIDE_ADMIN.md), [RESEAU_AELORIA](../RESEAU_AELORIA.md), [PORTAILS_ET_UNDO](../PORTAILS_ET_UNDO.md), [GUIDE_SERVEUR](../GUIDE_SERVEUR.md).

**Permissions** : *Tous* = aucun prérequis · *OP* = opérateur niveau 2 (gamemaster) · *Propriétaire* = niveau 4. Le grade affiché (Fondateur, Modérateur…) n'accorde **pas** le statut OP. Les arguments entre `<>` sont obligatoires, entre `[]` facultatifs.

## Sommaire
1. [Touches](#1-touches)
2. [Commandes joueur](#2-commandes-joueur)
3. [Commandes opérateur](#3-commandes-opérateur-op)
4. [Commandes propriétaire](#4-commandes-propriétaire)
5. [Interfaces et menus](#5-interfaces-et-menus)
6. [Fonctionnalités par catégorie](#6-fonctionnalités-par-catégorie)
7. [Interrupteurs de fonctionnalités](#7-interrupteurs-de-fonctionnalités)
8. [Outils, items et blocs d'administration](#8-outils-items-et-blocs-dadministration)
9. [Configuration et fichiers](#9-configuration-et-fichiers)
10. [Réseau, base de données, sécurité](#10-réseau-base-de-données-sécurité)

---

## 1. Touches
Modifiables dans Options > Commandes > MMORPG.

| Touche | Action |
|---|---|
| **E** | Personnage + inventaire (l'inventaire vanilla est remplacé) |
| **M** | Menu MMORPG (onglets) |
| **K** | Carte du monde |
| **X** | Marquer un point pour le groupe (ping) |
| **N** | Monture : invoquer / descendre |
| **R B G H J V** | Compétences 1 à 6 (6 emplacements) |
| **U** | Éditeur de l'interface (HUD) |
| **C** (maintenir) | Zoom ×4 |
| **Page préc. / suiv.** | Rapprocher / éloigner la caméra 3e personne (aussi à cheval) |
| **Espace** | Passer la cinématique |
| **F8** | Atelier Replay (OP) |
| **F1** | Masque la flèche de guidage 3D |
| **T**, puis Tab / Maj+Tab / Alt+flèches | Chat : changer de canal (Général / Guilde / Groupe) |
| **Tab** (maintenir) | Liste des joueurs Aeloria (classe, niveau, latence, jusqu'à 80) |
| **Échap** | Menu de pause Aeloria (aventure, boutique, options, tous les menus, WIKI) |
| *(non assignée)* | Mode streamer (masque les infos sensibles) |
| **Maj + clic** (inventaire) | Transfère une pile ; maintenir Maj + glisser transfère plusieurs piles |
| **Maj + clic gauche** | Casser un bloc d'interface (PNJ, forge, autel, téléporteur…) |
| **Clic gauche / droit** | Ouvrir un PNJ, une forge, un autel, un coffre ; passer sous l'arche d'un téléporteur |

Dans l'éditeur HUD (U) : glisser = déplacer (Maj = sans aimantation), molette = taille (50–300 %), clic droit = masquer/afficher, clic molette ou R = replacer, flèches = ajuster au pixel (Maj : 10 px), Échap/Terminé = enregistrer.

---

## 2. Commandes joueur
Disponibles pour tous. Les commandes marquées *(OP)* sont détaillées en §3.

### Groupe, chat, social
| Commande | Effet |
|---|---|
| `/groupe` | Liste du groupe |
| `/groupe inviter <joueur>` | Inviter (groupes de 5 max) |
| `/groupe accepter` / `refuser` | Répondre à une invitation |
| `/groupe quitter` | Quitter le groupe |
| `/groupe exclure <joueur>` / `chef <joueur>` | Gestion (chef) |
| `/p <message>` ou `/gr <message>` | Chat de groupe |
| `/g <message>` ou `/guilde chat <message>` | Chat de guilde (tous serveurs, 200 car., 1 msg/s) |
| `/amis` | Liste d'amis |
| `/photo` / `/photo groupe` | Mode photo (solo / tout le groupe proche) |
| `/report <joueur> <raison>` | Signaler un joueur aux modérateurs |

### Économie
| Commande | Effet |
|---|---|
| `/or` | Solde de la bourse |
| `/payer <joueur> <montant>` | Donner de l'or |
| `/trade <joueur>` (alias `/echange`) | Échange sécurisé (48 blocs max, 60 s d'expiration) |
| `/trade accepter` / `refuser` / `annuler` | Répondre / annuler |
| `/boutique` | Boutique premium (crédits) |
| `/credits` | Ouvre la boutique (solde de crédits) |
| `/outils` | Outils du débutant (cooldown) |
| `/forge relancer` | Relancer les affixes de l'objet tenu (près d'une Forge Arcanique) |
| `/forge reforger <de> <vers>` / `annuler` | Reforger un affixe (près d'une Forge) |

### Combat, PvP, guilde
| Commande | Effet |
|---|---|
| `/duel <joueur> [mise]` | Défier un joueur (mise en or facultative) |
| `/duel accepter` / `refuser` / `abandon` | Répondre / abandonner |
| `/arene` | Menu des arènes |
| `/arene quitter` | Quitter la file ou le match |
| `/arene bilan` | Revoir le dernier bilan |
| `/pvp` | Marchand de points d'arène |
| `/saison` | Saison PvP : récompenses et classement |
| `/guilde` | Menu de guilde |
| `/guilde sortir` | Quitter une épreuve de guilde (Yggdrasil) |
| `/classement [type]` | Top 10 : `niveau`, `monstres`, `arene`, `fortune`, `boss`, `donjons`, `quetes`, `guilde_niveau`, `guilde_banque`, `guilde_gardiens` |

### Monde et déplacements
| Commande | Effet |
|---|---|
| `/hub` | Retour au hub (met fin à un essai) ; `/hub <n>` : hub n°n |
| `/spawn` (alias `/marche`) | Aller au Spawn d'Aeloria |
| `/reseau hub` / `spawn` / `mines` / `plaines` | Changer de serveur du réseau |
| `/mine` | Menu du Maître des mines (5 étages) |
| `/plaines` | Menu des plaines (6 régions) |
| `/donjon carte` | Carte du donjon en cours |
| `/donjon classement` | Records de vitesse des donjons |
| `/retourdonjon` | Retour au portail d'entrée du donjon |
| `/tuto` | Indique le prochain guide du tutoriel |

---

## 3. Commandes opérateur (OP)

### 3.1 `/mmorpg …` (OP)
| Sous-commande | Effet |
|---|---|
| `niveau <joueur> <1-100>` | Fixe le niveau |
| `xp <joueur> <montant>` | Donne de l'XP |
| `points <joueur> <1-1000>` | Points d'attributs |
| `classe <joueur> <classe>` | Change la classe (`aucune` rouvre le choix) |
| `renommer <joueur> <1-12> <nom>` | Renomme un emplacement de personnage |
| `reset <joueur>` | Réinitialise la progression |
| `soin <joueur>` | PV, mana, recharges |
| `info <joueur>` | Statistiques complètes |
| `or <joueur> <montant>` | Donne de l'or |
| `familier <joueur> <id\|tous>` | Débloque un/tous les familiers |
| `cosmetique <joueur> <id\|tous>` | Débloque un/tous les cosmétiques |
| `quetes <joueur> reset` | Réinitialise les quêtes |
| `mob <type> [niveau]` | Fait apparaître un monstre |
| `boss <roi_gobelin\|liche_ancienne\|seigneur_ignis\|titan_glace\|avatar_neant>` | Fait apparaître un boss |
| `mannequin <standard\|blinde\|magique>` / `mannequin supprimer` | Pose / retire des mannequins (rayon 10) |
| `pnj quetes <nom>` · `pnj marchand <nom>` | PNJ (`@0`…`@3` avant le nom = apparence) |
| `pnj quetes_groupe <groupe> <0-3> <nom>` | PNJ d'un groupe de quêtes |
| `pnj maitre_classes` | Maître des classes |
| `pnj supprimer` | Supprime les PNJ dans 4 blocs |
| `ville <nom>` | Construit une place de ville |
| `teleporteurs` | Liste les téléporteurs |
| `hub` | Construit le hub |
| `cinematique [joueur]` | Rejoue la cinématique |
| `essai fin [joueur]` | Termine un essai de classe |
| `donjon construire <type> [normal\|heroique\|mythique]` | Construit un donjon |
| `donjon reinitialiser` | Remet à zéro le donjon où l'on se trouve |
| `donjon prechauffer <type> <1-20>` | Pré-génère des instances |
| `donjonsetup menu` / `donjonsetup <donjon>` | Éditeur de donjon |
| `editeurquetes` | Atelier de quêtes |
| `butins` | Éditeur des tables de butin (caisses, Lucky Blocks) |
| `reload` | Recharge la configuration |

Types de donjon : `crypte_gobeline`, `catacombes`, `forteresse_infernale`, `sanctuaire_glace`, `citadelle_neant`.

### 3.2 Construction, monde, zones
| Commande | Effet |
|---|---|
| `/hub set` · `/hub reset <joueur>` | Construire le hub · remettre un joueur à zéro |
| `/spawn set` · `pnj` · `stations` · `reconstruire` | Gestion du Spawn (arrivée, PNJ, stations, reconstruction) |
| `/class set <joueur> <classe>` | Change la classe |
| `/setdonjon [type]` | Pose le Maître des donjons |
| `/setmine` | Pose le Maître des mines |
| `/mine etat` · `reconstruire <n>` · `aller <n>` · `debloquer` | Gestion des mines |
| `/plaines etat` · `reconstruire <n>` · `aller <n>` · `debloquer` | Gestion des plaines |
| `/portail hub\|spawn\|mine [région]\|plaines [région]\|donjon [type] [normal\|heroique\|mythique]` | Pose un portail d'aventure |
| `/portail bloc …` · `/portail zone …` | Mêmes cibles, en bloc ou en zone |
| `/portail liste` · `/portail supprimer` | Liste · retire le portail à 8 blocs |
| `/undoportail [n]` · `/undo` | Annule les derniers aménagements |
| `/zone creer <nom>` · `liste` · `ici` · `supprimer <id>` | Zones (baguette de zone) |
| `/zone style <id> <zone\|boss\|dungeon> [sous-titre]` | Style d'annonce d'entrée |
| `/zone regle <id> <construction\|pvp\|monstres> <interdit\|autorisé…>` | Règles de zone |
| `/amenagement` | Éditeur d'aménagement du Spawn |
| `/amenagement outil` · `outilarmure` · `outilpnj` | Donne les outils |
| `/amenagement poser <élément> <orientation>` | Pose un élément du catalogue |
| `/amenagement texte <taille> <hauteur> <texte>` · `texteoriente …` · `supprimertexte` | Textes 3D |
| `/amenagement annuler` · `pointspawn` | Annule · point d'arrivée |
| `/amenagement purgepnj [confirm\|recreer]` | Supprime / recrée tous les PNJ |
| `/outilarmure` | Outil de porte-armure |
| `/monture poser <type>` · `retirer` | Monture décor immobile |
| `/mascotte [joueur]` · `poser` · `retirer` · `suit tout\|<n>` | Mascotte du tutoriel |
| `/schem list\|paste <nom> <pos> [air]\|status\|resume\|stop\|tp\|rate <blocs>\|verify <nom> <pos>` | Collage de schematics progressif |
| `/flyspeed <0-10>` | Vitesse de vol |

### 3.3 Tests, événements, PvP
| Commande | Effet |
|---|---|
| `/aeloriatest boss <1-10>` | Affronter un gardien de guilde (sans gain) |
| `/aeloriatest arene feu\|dunes` | Instance d'arène solo |
| `/aeloriatest equipe rouge\|bleu` | Changer d'équipe de test |
| `/aeloriatest stop` | Quitter le test |
| `/arene victoire rouge\|bleu [joueur]` (ou `bleue`) | Termine le match avec le vainqueur choisi |
| `/saison terminer` | Clôt la saison PvP |
| `/atp <joueur>` (alias `/tpreseau`) | Téléportation entre dimensions et serveurs |

### 3.4 Modération
| Commande | Effet |
|---|---|
| `/moderation` (alias `/mod`) | Panneau : Joueurs, Sanctions, Signalements, Historique, Journal/rejeu, Sauvegardes, Connectés, Outils, Donjons |
| `/adminjoueurs` | Gestion complète d'un joueur (Résumé, Données, Objets, Actions) |
| `/jail "pseudo" "temps" [raison]` · `/jail liste` · `/unjail "pseudo"` | Prison (ex. 30m, 2h, 1d ; même hors ligne) |
| `/jailzone set` · `rayon <n>` · `info` | Zone de prison |
| `/maintenance <temps\|now> [raison]` · `off` · `statut` | Maintenance avec compte à rebours |
| `/planifier annonce\|commande <délai> [repete <intervalle>] <texte>` · `liste` · `supprimer <n>` | Planificateur |
| `/profil sauvegarder\|liste\|restaurer "pseudo" [fichier]` | Sauvegardes de profil |
| `/annuler "pseudo" <minutes>` | Annule les dernières minutes d'un profil |
| `/signalements` · `tous` · `joueur <nom>` · `traiter <id>` · `rejeter <id>` | Signalements |
| `/anticheat` · `joueurs` · `reinitialiser <joueur>` | Anticheat |
| `/audit [n]` | Journal d'audit (n ≤ 50) |
| `/replay` · `record` · `play` · `stop` | Atelier Replay |

### 3.5 Serveur, fonctionnalités, base
| Commande | Effet |
|---|---|
| `/fonctionnalites` | Écran des interrupteurs |
| `/fonctionnalite` · `<fonction> activer\|desactiver` | Liste / bascule d'une fonctionnalité (sans redémarrage) |
| `/tps` | TPS/MSPT du serveur et du réseau |
| `/perf` · `reset` · `nettoyer` | Performances · compteurs · entités orphelines |
| `/db etat` · `sauvegarder` · `joueur <pseudo>` · `evenements [n]` | Base de données |
| `/adminmenu "mot de passe"` | Éditeur de toutes les configs (mot de passe requis, essayable par tous) |

## 4. Commandes propriétaire
| Commande | Effet |
|---|---|
| `/adminpass <nouveau>` | Change le mot de passe du menu admin (8 car. min.) |
| `/grade <joueur> <fondateur\|administrateur\|moderateur\|builder\|aucun\|aelorien\|eclaireur\|gardien\|arcaniste\|elu>` | Badge staff ou grade joueur |
| `/credits ajouter <UUID> <montant> <référence>` | Crédite un compte (référence unique, idempotent) |

---

## 5. Interfaces et menus
Cadre commun (`MmoScreen`), onglets du menu **M** (activables par interrupteur) :

| Onglet / écran | Contenu |
|---|---|
| Personnage (E) | Équipement, portrait, sacs, inventaire, barre, attributs, stats, évolution, Personnaliser |
| Compétences | Arbre des 11 compétences, rangs 1-5, barre de 6 emplacements |
| Talents | Arbre secondaire et prestige |
| Quêtes / Journal de quêtes | Toutes / Campagne / Journalières / À rendre, suivi prioritaire |
| Groupe, Guilde, Amis | Gestion sociale |
| Arène | Files par tranche de niveau |
| Familiers, Montures, Cosmétiques | Collections |
| Bestiaire | Monstres vaincus |
| Métiers | Mineur, Fermier, Pêcheur (aperçu, paliers, sources d'XP) |
| Succès | 7 catégories + page Titres |
| Journal | Statistiques détaillées |
| Boutique | Boutique premium |
| Wiki | Wiki intégré (aussi dans Échap) |
| Créatif | Catalogue de construction (OP en créatif) |
| Carte du monde (K) | Carte, points d'intérêt |

Écrans liés aux blocs/PNJ : Forge Arcanique, Presse de compression, Banque, HDV, Boîte aux lettres, Marchand, Marchand PvP, Quêtes hebdomadaires, Maître des classes, Maître des donjons, Maître des mines, Téléporteur, Coffres/Caisses, Lucky Block, Échange, Sacs à dos, Classement.

Écrans d'administration : Menu admin, Fonctionnalités, Joueurs, Modération, Éditeur de quêtes, Éditeur de butins, Éditeur de marchand, Éditeur de PNJ, Éditeur d'aménagement, Éditeur de porte-armure, Configuration de téléporteur, Configuration de donjon, Éditeur HUD.

---

## 6. Fonctionnalités par catégorie

### Classes et progression
- 4 classes : **Guerrier, Mage, Archer, Paladin** ; niveaux 1-100 ; évolutions aux niveaux 25 / 50 / 75 / 100 (+5 % PV, Attaque, Magie, Défense chacune) : Dieu de la Guerre, Avatar des Arcanes, Archer Divin, Titan Immortel.
- Choix de classe au hub : cinématique d'arrivée, Maîtres de classe, **essai** à un niveau (1/25/50/75/100) sans gain, choix définitif. Rune/orbe pour changer de classe.
- **8 attributs** (2 par classe) et **14 statistiques** dérivées ; 3 points par niveau.
- **44 points de compétences** à 100 ; 11 compétences par classe (10 actives, 1 passive), rangs 1-5, 6 emplacements. Catalogue : [wiki](wiki/catalogue-competences.md).
- Talents (arbre secondaire/prestige), succès et titres, bestiaire, journal de statistiques, classements.
- Régénération passive (1 % PV/5 s), écran de mort avec perte d'XP configurable, réapparition au spawn.
- Plusieurs personnages par compte (sélection de personnage).

### Combat
- Classes à distance : **autolock PVE** arc/bâton (jamais sur un joueur) ; arcs sans flèches ; bâton = projectile magique.
- Paladin : bouclier lié en main secondaire, jauge de garde 100 (coût par coup : 25/20/15/10/2 selon palier), rupture, récupération.
- Dégâts flottants, barres de vie, buffs/débuffs affichés (Défense réduite, Mana drainé, Soins réduits), contrôle de foule, immunité de projection après 3 projections.
- **24+ armes PvE** (44 au catalogue) et **17 sets d'armure** ; prérequis classe + niveau vérifiés côté serveur ; affixes à la Forge ; amélioration +1 à +20 (trèfles garantis par palier).
- Mannequins d'entraînement (standard, blindé, magique, duelliste) avec DPS.
- **Monstres** : Gobelin, Loup Sombre, Squelette Maudit, Orc, Élémentaire de Feu, Spectre de Givre, Golem de Cristal, Chevalier du Néant, + 30 monstres 3D importés par zone ; apparition configurable.
- **5 boss** (Roi Gobelin 20, Liche Ancienne 45, Seigneur Ignis 65, Titan de Glace 80, Avatar du Néant 100) : 4 phases, terrain modifié, base d'invocation (laisse), invocation par objet sur Autel.

### Donjons
- 5 donjons × 3 difficultés (normal/héroïque/mythique), instanciés (solo ou groupe) dans `mmorpg:donjons`.
- 8 épreuves + embuscades, variantes à modificateurs (renforts, brume, surcharge), secrets runiques et puzzles, clés d'étage, carte (`/donjon carte`), chrono et records de vitesse, coffre bonus sans mort, Passeur de sortie, coffre de boss.
- Pas de monture en donjon ; musique propre par donjon.

### PvP et arènes
- Arène de feu (5v5) et des dunes (3v3), 5 tranches de niveau (1-20 … 81-100), cristal mobile (50 coups), 500 points, drones, zones sûres, bilan de fin.
- Duels avec mise (`/duel`), saisons PvP, points d'arène, marchand PvP (20 panoplies + 20 armes + Chimère d'arène + titres + potions).

### Guilde
- Niveaux 1-10 (50 membres), rangs chef/officier/membre, banque d'or, journal (60 ops), **Yggdrasil** (10 gardiens), buffs temporaires (1 000 or/h), identité (couleur, emblème, hall `mmorpg:guildes`, 3 améliorations), chat de guilde, invitation à un boss de guilde, classement.

### Quêtes et PNJ
- **Tutoriel** guidé par la Mascotte (35 quêtes `tuto_*`), flèche 3D de guidage, bulles de PNJ ; découverte du spawn ; évolutions de classe ; 3 quêtes **hebdomadaires** par joueur (reset lundi 00:00) ; journalières ; chaînes de quêtes ; objectifs `kill`, `collect`, `level`, `visit`, `action`.
- Jusqu'à 10 quêtes actives, suivi à l'écran, offres de quête, quêtes refusées (onglet Important).

### Économie
- **Or** (bourse, ramassage direct même inventaire plein, drop d'objets interdit), **Marchands** (achat/vente), **Banque** (or et objets), **HDV** (annonces limitées par grade, objets livrés par courrier), **Boîte aux lettres** (message 140 car., or, 4 objets, 60 lettres), **Échange** sécurisé (8 objets + or), `/payer`, **Presse de compression**, boutique premium (crédits, grades, clés, charmes, runes, Lucky Blocks, potions).
- Grades joueur : Aelorien, Éclaireur, Gardien, Arcaniste, Élu.
- Caisses (vote, quête, commune, rare, épique, légendaire, mythique) et 5 Lucky Blocks (Céleste, Sylvestre, Infernal, Givre, Néant) avec tirage animé.

### Artisanat et métiers
- **Forge Arcanique** : 6 catégories (Armes, Armures, Matériaux, Consommables, Invocations, Familiers), onglet Amélioration, parchemins de forge, runes d'outils (Abondance, Prospection, Célérité, Ténacité, rangs 1-5), sacs à dos (6/12/18/24 cases, 2 emplacements).
- **Métiers** (niv. 1-50, paliers 10/20/30/40/50 : titre, plat, cosmétique) : **Mineur** (5 mines, minerais Verdite→Néantite, veines), **Fermier** (5 cultures, 6 plaines), **Pêcheur** (6 cannes, 27 poissons, mini-jeu de jauge, coffres, combat de poisson).
- 9 plats exclusifs.

### Montures, familiers, cosmétiques
- **Montures** : 23 types (Sanglier … Hippogriffe, Drake ascendant en vol ; montures partagées à plusieurs sièges ; Chimère d'arène), sceaux, vitesse selon rareté, caméra adaptative.
- **Familiers** : 8 (Feu Follet, Bébé Slime, Chouette Sage, Loup Spectral, Golem de Poche, Fée Lumineuse, Phénix Doré, Dragonnet du Néant), niveau 10 max, bonus de stats, modèles 3D animés ; œufs.
- **Cosmétiques** : auras, ailes, halos, traînées, coiffes/dos/visage/épaules 3D (26), visibles par tous.

### Social et confort
- Groupes (5 max, XP partagée +10 %/membre), amis, ping de groupe, mode photo, chat à 3 canaux, mode streamer, carte et minimap, compteur DPS, Discord Rich Presence, zoom, wiki intégré, menu principal et pause Aeloria, écran de chargement Aeloria.

### Monde
- **Hub** (Citadelle, dimension `mmorpg:hub`), **Spawn / Cité d'Aeloria** (marché, banque, HDV, forges), 5 **mines**, 6 **plaines**, dimensions donjons / compétitions / guildes, portails d'aventure, téléporteurs (villes, donjons, points de passage), zones (règles pvp/construction/monstres), respawn de zone partagé.

---

## 7. Interrupteurs de fonctionnalités
`/fonctionnalite <nom> activer|desactiver` ou écran `/fonctionnalites` (effet immédiat sur le réseau, journalisé) :

`wiki`, `arenes`, `donjons`, `drones`, `boutique_pvp`, `boutique`, `quetes`, `sacs`, `hdv`, `courrier`, `guildes`, `boss_guilde`, `cinematique`, `epreuves_classes`, `apparition_mobs`, `competences`, `bouclier`, `luckyblocks`, `coffres`, `montures`, `familiers`, `mines`, `plaines`, `forge`, `banque`, `marchands`, `cosmetiques`, `echange`, `metiers`, `peche`, `succes`, `classements`, `groupes`, `bestiaire`, `portails`, `quetes_hebdo`, `chat_guilde`, `anticheat`, `drop_objets`, `autolock`, `personnages`.

---

## 8. Outils, items et blocs d'administration
| Objet | Usage |
|---|---|
| Outil d'aménagement (`outil_spawn`) | Éditer le Spawn |
| Outil PNJ (`outil_pnj`) | Maj+clic droit = sélectionner, clic droit sol = déplacer ; dialogue, rotation |
| Outil porte-armure (`outil_armure`) | Poses, position, skin/modèle 3D |
| Sceptre des quêtes (`sceptre_quetes`) | Atelier de quêtes / fiche PNJ |
| Baguette de zone (`baguette_zone`) | Coins A/B d'une zone |
| Téléporteur (`teleporteur`) | Arche de téléportation configurable |
| Bloc de lumière / relais | Éclairage décoratif |
| Blocs : Forge Arcanique, Autel d'invocation, Banque, HDV, Boîte aux lettres, Quêtes hebdo, Presse, Lucky Blocks, Caisses, Coffre de donjon, Classements, Structures | Interactifs |
| Onglets créatifs : *Aeloria : bâtisseur* (19 matériaux), *Objets*, *Équipement* | Construction (OP) |

---

## 9. Configuration et fichiers
Dossier `config/mmorpg/` ; `/mmorpg reload` recharge.

| Fichier | Contenu |
|---|---|
| `general.json` | `xpMultiplier`, `dropMultiplier`, mise à l'échelle des monstres vanilla, `deathXpLossPercent`, `skillsHitPlayers`, apparition, drops, annonces, `hubCinematic`, `hubTrialZone`, `hubProtection`, `openClassSelectionOnJoin` |
| `mobs.json` | Monstres et boss (PV, attaque, défense, apparition, butin, capacités) |
| `quests.json` | Quêtes |
| `shop.json`, `butins.json`, `mannequin.json`, `spawn.json`, `cinematique.json` | Marchands, tables de butin, mannequins, spawn, cinématique |
| `database.json`, `network.json` | MariaDB et réseau |
| `admin-actions.log`, `muted.txt`, `anticheat.log`, `moderation/journal/` | Journaux et état de modération |
| `coordination/features.json`, `shared/…` | État partagé entre serveurs |
| `config/mmorpg-hud.json` (client) | Disposition du HUD |

Options client (Mods > Aeloria > Configurer) : dégâts flottants, barres de vie, ses cosmétiques en 1re personne, flèche de quête, mode streamer, minimap, DPS, volume de la musique d'ambiance, serveur du menu principal, **mode shader** (OFF/SOFT/AMBIANCE/CINEMA), Discord (activé / ID), distance de rendu des PNJ, particules adaptatives.

---

## 10. Réseau, base de données, sécurité
- **Réseau** : Velocity, serveurs hub / principal / mines / plaines / donjons, profils partagés, transfert de groupe, `/reseau`, `/atp`, `/tps`.
- **Base MariaDB** (facultative, désactivée par défaut) : profils, banque, journal d'audit, `/db`.
- **Anticheat** : vérifie débit de paquets, paquets interdits, vitesse, vol, portée, cadence de frappe et de casse, débit de commandes, interface non ouverte, signature invalide, objet dupliqué ; score de suspicion par joueur.
- **Sécurité** : permissions revérifiées côté serveur à chaque requête admin, journal d'audit, menu admin protégé par mot de passe, achats idempotents, sauvegardes de profil automatiques (5 min, connexion, déconnexion).
- **Boutique** : crédits uniquement via `/credits ajouter` (propriétaire) ; passerelle de paiement désactivée par défaut.
