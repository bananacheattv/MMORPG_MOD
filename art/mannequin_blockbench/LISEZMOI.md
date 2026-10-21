# Mannequin d'entraînement — modèle Blockbench animé

`python generer_mannequin.py` (Python 3, Pillow, numpy ; mêmes outils que `../entity_kit.py`) génère, pour chaque variante
(`standard/`, `blinde/`, `magique/`) : `.bbmodel` (projet Blockbench « Modded Entity »), texture, couche lumineuse (magique),
icône d'objet 32×32, aperçus et GIF, et écrit le Java du mod (`client/model/dummy`, `entity/dummy/anim`).

- Os communs : `racine` › `socle` ; `racine` › `mannequin` › `corps`, `bras`, `tete` (+ `cristal` pour le magique).
- Animations : `idle` (boucle), `hit` (oscillation amortie vers l'avant : dans le jeu, l'inclinaison est simulée par un ressort
  orienté selon la direction et la force du coup, voir `DummyRenderer`), `reset` (tour complet et petit rebond).
