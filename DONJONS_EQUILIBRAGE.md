# Équilibrage des donjons en solo

Modèle : tools/balance/final.py (formules du code). Colonnes : durée du combat de boss, PV perdus face au boss, pire vague (% des PV, sans potions ni esquive active). Avant -> après, difficulté normale.

```
TABLE (donjon, classe) : avant -> apres  [TTK boss s | PV perdus boss % | pire vague %] (normal)
catacombes             archer      42s  129%   325%  ->    35s   40%    71%
catacombes             guerrier    45s  132%   290%  ->    38s   51%    66%
catacombes             mage        39s  224%   625%  ->    33s   34%    67%
catacombes             tank       200s  220%   581%  ->    67s   32%    46%
citadelle_neant        archer      27s   63%   156%  ->    23s   20%    33%
citadelle_neant        guerrier    48s  131%   359%  ->    40s   50%    78%
citadelle_neant        mage        59s  322%  1203%  ->    49s   49%   124%
citadelle_neant        tank       327s  380%  1011%  ->   109s   41%    76%
crypte_gobeline        archer      37s  107%   284%  ->    31s   33%    61%
crypte_gobeline        guerrier    41s  110%   296%  ->    34s   42%    66%
crypte_gobeline        mage        36s  164%   540%  ->    30s   25%    56%
crypte_gobeline        tank       153s  169%   501%  ->    51s   26%    39%
forteresse_infernale   archer      38s  122%   270%  ->    31s   38%    57%
forteresse_infernale   guerrier    51s  154%   339%  ->    43s   59%    74%
forteresse_infernale   mage        49s  306%   892%  ->    41s   47%    91%
forteresse_infernale   tank       265s  341%   816%  ->    88s   42%    61%
sanctuaire_glace       archer      42s  123%   331%  ->    35s   38%    71%
sanctuaire_glace       guerrier    52s  164%   442%  ->    43s   63%    99%
sanctuaire_glace       mage        57s  353%  1282%  ->    47s   54%   135%
sanctuaire_glace       tank       303s  399%  1083%  ->   101s   45%    84%
```

Changements : vagues 2+2n+2d monstres (avant 6+3n+3d), embuscades 2+2d, escorte 4+2d ; boss baseDamage 1.5->1.25, robustesse 1.8->1.5 ; DungeonBalance : dégâts subis x0.55 Guerrier/Paladin, x0.22 Mage, x0.45 Archer, dégâts du Paladin x2.5, mana x1.5 Mage/Archer ; ces bonus s'estompent avec la taille du groupe (100 % solo, 2/3 à deux, 40 % à quatre). Non testé en jeu : le modèle est approximatif, à ajuster dans DungeonBalance.
