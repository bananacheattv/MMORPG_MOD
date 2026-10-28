# Aeloria — lien de travail des interfaces illustrées

Ce dossier est le **point d’entrée unique pour Claude**. Il regroupe toutes les images, les consignes et les contrôles d’affichage. La branche reste `art/ui-assets-for-claude` ; ce lien sera conservé pour les prochains lots.

## État des lots

- Lot1 : Métiers, Donjons, Classes, Quêtes, Montures/Familiers, Boutique, Chargements — livré.
- Lot2 : Guildes, Forge, Bestiaire, Succès, Résultats d’arène — livré.
- Lot3 : HDV, Banque/Sacs, Compétences, Fin de donjon, Fiches d’équipement — en préparation.
- **Carte du monde retirée à la demande de l’utilisateur. Aucun fichier de carte ni demande de création de ce menu n’est livré.**

**15 PNG disponibles**, dont les trois métiers de la première série. Les images sont des assets, pas des interfaces déjà fonctionnelles.

➡️ [Instructions d’intégration pour Claude](BRIEF_CLAUDE.md) · [Contrôles d’affichage obligatoires](DISPLAY_QA.md) · [Manifeste technique](manifest.json) · [Prompts](prompts.json)

## Récupération sans toucher au travail en cours

Récupérer les commits d’assets uniquement par cherry-pick depuis la branche de travail actuelle de Claude. Ne pas fusionner l’ancienne base de gameplay de cette branche par défaut. Les commits de ce pack modifient seulement `art/ui-pack/`, aucun Java ni launcher.

## Fichiers

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
