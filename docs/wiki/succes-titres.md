# Succès et titres

Onglet **Succès** du menu principal (`Succès`) : 7 catégories (Progression, Combat, Donjons, PvP, Métiers, Quêtes, Richesse), barre de progression par succès, et page **Titres**.

- **Déblocage** : niveaux, monstres et boss vaincus, donjons terminés, victoires et éliminations en arène (hors entraînement), niveaux de métier, quêtes terminées, or gagné.
- **Titres** : certains succès débloquent un titre. Le titre choisi (page Titres) s'affiche après le pseudo dans le chat : `Grade Pseudo « Titre » : message`. Les titres PvP du marchand restent équipables depuis la même page quand ils sont déjà équipés ou via le marchand.
- **Chat de guilde / autres chats** : utiliser `Achievements.suffix(joueur)` pour ajouter le titre après le pseudo.
- **Stockage** : dans le profil du joueur (voyage avec lui entre serveurs) et, si la base est activée (`config/mmorpg/database.json`), dans MariaDB : tables `player_achievements`, `player_stats`, `player_titles` (créées automatiquement). À la connexion, la base et le profil sont fusionnés (union des succès, maximum des compteurs).
- **Ajouter un succès** : une ligne dans `achievement/Achievement.java`.
