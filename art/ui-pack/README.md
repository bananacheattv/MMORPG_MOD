# Aeloria — lien de travail des interfaces illustrées

Ce dossier est le **point d’entrée unique pour Claude**. Il regroupe toutes les images, les consignes et les contrôles d’affichage. La branche reste `art/ui-assets-for-claude` ; ce lien sera conservé pour les prochains lots.

Complément demandé : [les dix interfaces et Yggdrasil](COMPLEMENT_INTERFACES.md). Prioritaire pour Claude : ce document précise les éléments à coder en plus des images.

## État des lots

- Lot1 : Métiers, Donjons, Classes, Quêtes, Montures/Familiers, Boutique, Chargements — livré.
- Lot2 : Guildes, Forge, Bestiaire, Succès, Résultats d’arène — livré.
- Médailles : six PNG transparents individuels livrés, un fichier par icône.
- Lot3 : HDV, Banque/Sacs, Compétences, Fin de donjon, Fiches d’équipement — livré.
- **Carte du monde retirée à la demande de l’utilisateur. Aucun fichier de carte ni demande de création de ce menu n’est livré.**

**34 PNG disponibles**, dont les trois métiers de la première série. Les images sont des assets, pas des interfaces déjà fonctionnelles.

➡️ [Instructions d’intégration pour Claude](BRIEF_CLAUDE.md) · [Contrôles d’affichage obligatoires](DISPLAY_QA.md) · [Manifeste technique](manifest.json) · [Prompts](prompts.json)

## Récupération sans toucher au travail en cours

Récupérer les commits d’assets uniquement par cherry-pick depuis la branche de travail actuelle de Claude. Ne pas fusionner l’ancienne base de gameplay de cette branche par défaut. Les commits de ce pack modifient seulement `art/ui-pack/`, aucun Java ni launcher.

## Fichiers

Les six médailles individuelles `medal_*.png` sont les fichiers à intégrer. La planche d’ensemble reste une référence, pas un bouton regroupant six icônes.

| Asset | Dimensions | Usage |
|---|---:|---|
| [Fond Métiers](images/professions_background.png) | 1672×941 | background |
| [Mineur](images/miner_card.png) | 1254×1254 | profession_card_with_title |
| [Pêcheur](images/fisher_card.png) | 1254×1254 | profession_card_with_title |
| [Fermier](images/farmer_card.png) | 1254×1254 | profession_card_with_title |
| [Donjons — crypte](images/dungeon_crypt_cover.png) | 1672×941 | landscape_card |
| [Guerrier, Mage, Archer, Paladin](images/classes_atlas.png) | 1983×793 | four_portrait_atlas |
| [Portrait guide de quêtes](images/quest_guide_portrait.png) | 1254×1254 | transparent_portrait |
| [Montures et familiers](images/mounts_pets_header.png) | 1672×941 | collection_header |
| [Boutique](images/shop_header.png) | 1672×941 | shop_header |
| [Chargement / portail](images/portal_loading_background.png) | 1672×941 | background |
| [Hall de guilde](images/guild_hall_header.png) | 1672×941 | guild_header |
| [Forge](images/forge_header.png) | 1672×941 | crafting_header |
| [Bestiaire](images/bestiary_header.png) | 1672×941 | bestiary_header |
| [Médailles de succès](images/achievement_medals_atlas.png) | 1536×1024 | six_medal_atlas |
| [Résultats d’arène](images/arena_results_background.png) | 1672×941 | results_background |
| [Médaille Mine](images/medal_mining.png) | 1254×1254 | achievement_icon |
| [Médaille Exploration](images/medal_exploration.png) | 1254×1254 | achievement_icon |
| [Médaille Forge](images/medal_crafting.png) | 1254×1254 | achievement_icon |
| [Médaille Boss](images/medal_boss.png) | 1254×1254 | achievement_icon |
| [Médaille Arène](images/medal_arena.png) | 1254×1254 | achievement_icon |
| [Médaille Récolte](images/medal_harvest.png) | 1254×1254 | achievement_icon |
| [Hôtel des ventes](images/auction_header.png) | 1672×941 | auction_header |
| [Banque et sacs](images/storage_header.png) | 1672×941 | storage_header |
| [Compétences](images/skills_background.png) | 1672×941 | skills_background |
| [Fin de donjon](images/dungeon_results_background.png) | 1672×941 | results_background |
| [Fiche d’équipement](images/equipment_inspection_background.png) | 1672×941 | item_inspection_background |

## Galerie

### Fond Métiers

![Fond Métiers](images/professions_background.png)

### Mineur

![Mineur](images/miner_card.png)

### Pêcheur

![Pêcheur](images/fisher_card.png)

### Fermier

![Fermier](images/farmer_card.png)

### Donjons — crypte

![Donjons — crypte](images/dungeon_crypt_cover.png)

### Guerrier, Mage, Archer, Paladin

![Guerrier, Mage, Archer, Paladin](images/classes_atlas.png)

### Portrait guide de quêtes

![Portrait guide de quêtes](images/quest_guide_portrait.png)

### Montures et familiers

![Montures et familiers](images/mounts_pets_header.png)

### Boutique

![Boutique](images/shop_header.png)

### Chargement / portail

![Chargement / portail](images/portal_loading_background.png)

### Hall de guilde

![Hall de guilde](images/guild_hall_header.png)

### Forge

![Forge](images/forge_header.png)

### Bestiaire

![Bestiaire](images/bestiary_header.png)

### Médailles de succès

![Médailles de succès](images/achievement_medals_atlas.png)

### Résultats d’arène

![Résultats d’arène](images/arena_results_background.png)

### Médaille Mine

![Médaille Mine](images/medal_mining.png)

### Médaille Exploration

![Médaille Exploration](images/medal_exploration.png)

### Médaille Forge

![Médaille Forge](images/medal_crafting.png)

### Médaille Boss

![Médaille Boss](images/medal_boss.png)

### Médaille Arène

![Médaille Arène](images/medal_arena.png)

### Médaille Récolte

![Médaille Récolte](images/medal_harvest.png)

### Hôtel des ventes

![Hôtel des ventes](images/auction_header.png)

### Banque et sacs

![Banque et sacs](images/storage_header.png)

### Compétences

![Compétences](images/skills_background.png)

### Fin de donjon

![Fin de donjon](images/dungeon_results_background.png)

### Fiche d’équipement

![Fiche d’équipement](images/equipment_inspection_background.png)

## Complément : interfaces manquantes et Yggdrasil

Huit nouveaux assets générés avec image_gen intégré ; prompts dans prompts.json.

### Yggdrasil — arbre sacré

![Yggdrasil — arbre sacré](images/yggdrasil_tree.png)

1254 × 1254 — alpha [0, 255].

### Maître des Quêtes — état vide

![Maître des Quêtes — état vide](images/quest_master.png)

1254 × 1254 — alpha [0, 255].

### Parchemin de quête

![Parchemin de quête](images/quest_parchment.png)

1024 × 1536 — alpha [0, 254].

### Bannière de groupe

![Bannière de groupe](images/group_banner.png)

1672 × 941 — alpha [255, 255].

### Arène de feu

![Arène de feu](images/arena_fire_cover.png)

1672 × 941 — alpha [255, 255].

### Arène des dunes

![Arène des dunes](images/arena_dunes_cover.png)

1672 × 941 — alpha [255, 255].

### Coffre de récompense hebdomadaire

![Coffre de récompense hebdomadaire](images/weekly_reward_chest.png)

1254 × 1254 — alpha [0, 255].

### Aperçu des cosmétiques

![Aperçu des cosmétiques](images/cosmetics_stage.png)

1672 × 941 — alpha [255, 255].
