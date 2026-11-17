# Zones de chasse et configuration des quêtes

Les instances **Mines** et **Plaines** (dimension construite, portails, menus, Maître des mines, `/setmine`) n'existent plus.
On pose ses propres lieux dans l'openworld et on les déclare comme *zones de chasse*.

## Ce qui reste (et marche partout)
- Minerais du mod (`minerai_*`) : extraction à la pioche, lingot direct, repousse (toute dimension).
- Cultures du mod : récolte « à la veine » + repousse **dans les zones avec `recolte: true`**.
- Métiers, quêtes de minage (`job:mineur`), butin direct à l'inventaire dans les zones de chasse.

## Zones de chasse : `config/mmorpg/zones_chasse.json`
Créé au premier démarrage. Rechargement à chaud : `/zone chasse recharger`.
```json
{ "zones": [ {
  "id": "prairies", "nom": "Prairies d'Aeloria", "dimension": "minecraft:overworld",
  "x1": 0, "y1": -64, "z1": 0, "x2": 400, "y2": 320, "z2": 400,
  "niveauMin": 1, "niveauMax": 18, "cellule": 48,
  "monstres": ["gobelin","mousseux","sanglier_ronce","archer_sylvestre","loup_sombre"],
  "max": 24, "surRayon": 40, "recolte": true, "actif": true } ] }
```
- `monstres` : clés de monstres du mod. Niveaux par cellules de `cellule` blocs entre `niveauMin` et `niveauMax`.
- `max` : monstres RPG maximum autour d'un joueur ; `surRayon` : zone sûre autour du centre.
- Les quêtes « tuer X » fonctionnent partout ; le guidage (boussole) pointe vers la zone qui contient le monstre voulu.

### En jeu (opérateurs)
Baguette de zone (2 coins), puis :
`/zone chasse creer <id> <niveauMin> <niveauMax> <monstres…>`, `liste`, `supprimer <id>`, `monstres <id> a,b,c`,
`niveaux <id> min max`, `max <id> n`, `sure <id> rayon`, `recolte <id> true|false`, `actif <id> true|false`, `nom <id> …`, `tp <id>`.

### Anciennes régions, prêtes à copier (monstres / niveaux)
| id | niveaux | monstres |
|---|---|---|
| prairies | 1-18 | gobelin, mousseux, sanglier_ronce, archer_sylvestre, loup_sombre |
| marais | 17-34 | crapaud_venimeux, limace_acide, luciole_geante, sorcier_roseaux, araignee_venimeuse |
| os | 33-50 | osselet, araignee_os, garde_funeraire, spectre_enchaine, bandit_arbaletrier |
| givre | 49-66 | renard_givre, scarabee_polaire, harpie_neiges, golem_iceberg, spectre_givre |
| cendres | 65-82 | scorpion_cuivre, momie_dunes, hyene_cendree, djinn_braises, tortue_volcanique |
| astre | 81-98 | mite_astrale, rodeur_vide, oeil_chaos, chevalier_fracture, devoreur_etoiles |
| mine_verdelune | 1-20 | gobelin, loup_sombre, zombie_des_cryptes |
| mine_argentite | 18-38 | squelette_maudit, araignee_venimeuse, zombie_des_cryptes |
| mine_ambrasite | 35-58 | orc_guerrier, bandit_arbaletrier, elementaire_feu |
| mine_glacium | 55-75 | spectre_givre, golem_cristal, squelette_maudit |
| mine_neantite | 72-100 | chevalier_neant, golem_cristal |

## Quêtes
- Fichier : `config/mmorpg/quests.json` (format : `docs/lore/FORMAT_JSON.md`). Rechargement : `/quetes recharger` (ou `/mmorpg reload`).
- Éditeur en jeu (admin) : créer/modifier quête, objectifs, récompenses, PNJ donneur.
- Une quête modifiée en jeu (ou avec `"custom": true` dans le JSON) n'est **jamais** écrasée par une mise à jour des quêtes par défaut.
- Les 777 quêtes du lore, le tutoriel (35 quêtes) et les PNJ ne dépendent plus d'aucun lieu : seuls les textes de 95 quêtes du lore citent « mines » ou « plaines » (aucun objectif lié à un lieu).
- Tutoriel : l'étape « Maître des mines » est devenue « Parler au marchand de minerais ».

## À placer par vous
Minerais (blocs `minerai_*`), champs de cultures, et vos zones de chasse (JSON ci-dessus). L'ancien PNJ « Maître des mines » déjà posé affiche seulement un message : à supprimer.
Données joueur `minesUnlocked`/`plainsUnlocked` : conservées, inutilisées.
