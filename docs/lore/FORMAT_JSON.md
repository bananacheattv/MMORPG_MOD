# Format des JSON de quêtes et de butins

## `src/main/resources/data/mmorpg/lore/lore_quests.json`
Liste de 777 quêtes (principale → secondaires → événements), indentée (2 espaces), une propriété par ligne.
Ordre des clés : identité (`id`, `code`, `kind`, `name`, `donor`, `level`, `requires`) → classement (`act`, `chapter`, `num`, `category`, `region`) → événement/conditions (`event`, `from`, `to`, `repeat`, `condText`, `cond`, `classes`, `scene`) → objectif (`type`, `obj`, `objs`, `ask`) → textes (`desc`, `gameplay`, `companion`) → `rewards`.
Ne pas réordonner le tableau (le générateur de PNJ en dépend). Après modification : bumper `DefaultQuests.REVISION`.
Fichier produit par `tools/quetes/build_lore_quests.py` (écrit via `tools/quetes/fmt_json.py`) ; `python3 tools/quetes/fmt_json.py` le reformate en place.

## `config/mmorpg/butins.json` (serveur)
Une table par caisse / Lucky Block / mystère / donjon, une ligne par objet `{ item, min, max, weight }`, tables dans l'ordre de l'éditeur `/mmorpg butins`. La clé `_doc` est ignorée à la lecture. Réécrit ainsi à chaque démarrage et à chaque sauvegarde en jeu.
