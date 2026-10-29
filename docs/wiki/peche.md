# Pêche

[Accueil](README.md) · [Métiers](metiers.md)

Code : `fishing/` (catalogue, règles, mini-jeu, événements), `item/AeloriaRodItem.java`, `item/TreasureChestItem.java`. Un lancer ramène un poisson, un déchet ou un coffre selon le lieu, l’heure, la météo, le niveau de Pêcheur et la canne. Une prise rare demande de réussir le mini-jeu de la jauge (clic droit quand l’aiguille est dans la zone).

## Cannes

| Rang | Canne | Niveau de Pêcheur | Durabilité |
|---:|---|---:|---:|
| 1 | Canne d'apprenti | 1 | 250 |
| 2 | Canne du pêcheur | 10 | 500 |
| 3 | Canne du marin | 20 | 800 |
| 4 | Canne du harponneur | 30 | 1200 |
| 5 | Canne des abysses | 40 | 1800 |
| 6 | Canne du Léviathan | 50 | 2600 |

## Poissons

| Poisson | Rareté | Niveau | Moment |
|---|---|---:|---|
| Gardon argenté | Commun | 1 | Toujours |
| Carpe dorée | Commun | 1 | Jour |
| Sardine lunaire | Commun | 1 | Nuit |
| Maquereau bleu | Commun | 1 | Toujours |
| Goujon des marais | Commun | 1 | Toujours |
| Omble des glaces | Commun | 1 | Toujours |
| Tilapia solaire | Commun | 1 | Toujours |
| Anchois des abysses | Commun | 1 | Toujours |
| Truite arc-en-ciel | Peu commun | 6 | Pluie |
| Brochet doré | Peu commun | 8 | Nuit |
| Bar écarlate | Peu commun | 8 | Jour |
| Anguille voltaïque | Peu commun | 10 | Nuit |
| Piranha pourpre | Peu commun | 10 | Toujours |
| Poisson-lanterne | Peu commun | 10 | Nuit |
| Saumon royal | Rare | 18 | Toujours |
| Esturgeon ambré | Rare | 20 | Jour |
| Raie cristalline | Rare | 22 | Toujours |
| Silure géant | Rare | 24 | Nuit |
| Perche de givre | Rare | 24 | Toujours |
| Thon de l'aurore | Épique | 30 | Jour |
| Baudroie abyssale | Épique | 34 | Nuit |
| Dragon des lacs | Épique | 36 | Pluie |
| Requin de corail | Épique | 38 | Toujours |
| Poisson-étoile astral | Légendaire | 42 | Nuit |
| Kraken juvénile | Légendaire | 45 | Pluie |
| Léviathan nain | Légendaire | 48 | Toujours |
| Carpe du Néant | Mythique | 50 | Nuit |

Les coffres (rouillé, de corail, du capitaine, abyssal) donnent de l’or et des objets selon leur rang ; ils se pêchent plus souvent sous la pluie et avec une meilleure canne.
