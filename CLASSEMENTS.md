# Classements en panneaux

Neuf panneaux 3D (onglet créatif « Objets », réservés aux admins, incassables en survie) : `classement_niveau`, `_fortune`, `_monstres`, `_boss`, `_donjons`, `_quetes`, `_pvp_victoires`, `_pvp_elims`, `_guildes`. Chaque classement a son modèle (bandeau de couleur) et affiche son top 10 sur la face avant ; clic droit = votre rang. Pas de récompenses ni de saisons.

| Classement | Valeur | Mise à jour |
| --- | --- | --- |
| Niveau | niveau (départage : XP) | relevé toutes les 20 s (joueurs connectés) |
| Fortune | or du porte-monnaie | relevé |
| Chasseurs | monstres tués | relevé |
| Quêtes | quêtes terminées | relevé |
| Tueurs de boss | boss tués (hors essai de classe) | à chaque mort de boss |
| Donjons | donjons vaincus (joueurs à 64 blocs du boss final) | à chaque victoire |
| Arène : victoires / éliminations | matchs gagnés / éliminations (hors entraînement) | fin de match |
| Guildes | niveau de guilde (départage : XP) | lu dans le registre des guildes |

**Stockage** : table `leaderboard_stats` (MariaDB, `config/mmorpg/database.json` avec `enabled: true`, schéma v3 créé automatiquement) ; partagée entre tous les serveurs du réseau. Sans base, copie locale enregistrée avec le monde (`classements`). Les panneaux se rafraîchissent toutes les 10 s.

Les modèles, textures, états de bloc, objets et noms sont générés par `python3 tools/gen-leaderboards.py`.
