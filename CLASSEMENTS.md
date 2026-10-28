# Classements en panneaux

Dix panneaux 3D (onglet créatif « Objets », admins) et la commande `/classement <type>` : `niveau`, `monstres`, `arene`, `fortune`, `boss`, `donjons`, `quetes`, `guilde_niveau`, `guilde_banque`, `guilde_gardiens`. Un modèle par classement, top 10 sur la face avant, clic droit = top et votre rang. Pas de récompenses ni de saisons.

| Classement | Valeur (départage) | Mise à jour |
| --- | --- | --- |
| niveau | niveau (XP) | relevé toutes les 10 s, joueurs connectés |
| monstres | monstres tués | relevé |
| fortune | or du porte-monnaie | relevé |
| quetes | quêtes terminées | relevé |
| boss | boss tués (hors essai de classe) | à chaque mort de boss |
| donjons | donjons vaincus (joueurs à 64 blocs du boss final) | à chaque victoire |
| arene | victoires (éliminations), hors entraînement | fin de match |
| guilde_* | niveau, fortune de la banque, gardiens vaincus | registre des guildes |

**Stockage des joueurs** : table MariaDB `leaderboard_stats(board, uuid, name, value, extra)`, partagée par tous les serveurs (`config/mmorpg/database.json`, `enabled: true`, schéma v3 créé automatiquement). Sans base : copie locale enregistrée avec le monde. Code : `leaderboard/Leaderboards.java`, affichage : `ranking/Rankings.java`.

Ajouter un classement : `leaderboard/Board.java`, `ranking/RankingKind.java` (+ texte dans `Rankings.text`), `tools/generate_rankings.py`, puis `python3 tools/generate_rankings.py`.
