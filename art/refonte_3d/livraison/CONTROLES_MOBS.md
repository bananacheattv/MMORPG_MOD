# Contrôles des mobs

Script `outils/controle_mobs.py`, sans lancer le jeu. Tailles en texels de jeu (16 = 1 bloc).

| Mob | Cubes | Texture | Lueurs | Clips | Hauteur avant | Hauteur après | Remarque |
|---|---:|---:|---:|---|---:|---:|---|
| `mousseux` | 60 | 256 | 2 | attack, death, idle, walk | 41 | 43 |  |
| `sanglier_ronce` | 89 | 256 | 2 | attack, death, idle, walk | 43 | 44 |  |
| `archer_sylvestre` | 54 | 128 | 2 | attack, death, idle, walk | 62 | 70 |  |
| `veilleur_champignon` | 56 | 256 | 2 | attack, death, idle, walk | 59 | 59 |  |
| `colosse_racine` | 94 | 512 | 4 | attack, death, idle, walk | 174 | 173 |  |
| `crapaud_venimeux` | 51 | 256 | 18 | attack, death, idle, walk | 35 | 35 |  |
| `limace_acide` | 27 | 256 | 4 | attack, death, idle, slither | 27 | 29 |  |
| `luciole_geante` | 32 | 128 | 4 | attack, death, fly, idle | 26 | 30 |  |
| `sorcier_roseaux` | 49 | 256 | 4 | attack, death, idle, walk | 64 | 75 |  |
| `hydre_marais` | 104 | 512 | 6 | attack, death, idle, walk | 154 | 159 |  |
| `osselet` | 53 | 128 | 2 | attack, death, idle, walk | 43 | 46 |  |
| `araignee_os` | 60 | 128 | 3 | attack, death, idle, walk | 29 | 27 |  |
| `garde_funeraire` | 81 | 256 | 3 | attack, death, idle, walk | 67 | 78 |  |
| `spectre_enchaine` | 66 | 256 | 3 | attack, death, fly, idle | 58 | 64 |  |
| `roi_cryptes` | 107 | 512 | 7 | attack, death, idle, walk | 156 | 163 |  |
| `renard_givre` | 47 | 128 | 2 | attack, death, idle, walk | 42 | 46 |  |
| `scarabee_polaire` | 45 | 256 | 4 | attack, death, idle, walk | 37 | 38 |  |
| `harpie_neiges` | 46 | 128 | 12 | attack, death, fly, idle | 51 | 58 |  |
| `golem_iceberg` | 63 | 512 | 4 | attack, death, idle, walk | 154 | 156 |  |
| `wyrm_boreal` | 71 | 512 | 2 | attack, death, idle, slither | 157 | 168 |  |
| `scorpion_cuivre` | 61 | 256 | 4 | attack, death, idle, walk | 41 | 46 |  |
| `momie_dunes` | 33 | 128 | 2 | attack, death, idle, walk | 62 | 61 |  |
| `hyene_cendree` | 63 | 256 | 34 | attack, death, idle, walk | 42 | 50 |  |
| `djinn_braises` | 42 | 256 | 17 | attack, death, fly, idle | 64 | 78 |  |
| `tortue_volcanique` | 52 | 256 | 24 | attack, death, idle, walk | 45 | 44 |  |
| `mite_astrale` | 21 | 128 | 4 | attack, death, fly, idle | 57 | 57 |  |
| `rodeur_vide` | 63 | 256 | 16 | attack, death, idle, walk | 48 | 45 |  |
| `oeil_chaos` | 50 | 256 | 15 | attack, death, fly, idle | 50 | 67 | hauteur très différente, à regarder |
| `chevalier_fracture` | 65 | 256 | 16 | attack, death, fly, idle | 71 | 77 |  |
| `devoreur_etoiles` | 105 | 1024 | 8 | attack, death, fly, idle | 262 | 257 |  |
| `fernling` | 22 | 128 | 2 | attack, death, idle, walk | 18 | 22 |  |
| `champion_gobelin` | 39 | 256 | 2 | attack, death, idle, walk | 45 | 50 |  |
| `voyageur_squelette` | 49 | 128 | 2 | attack, death, idle, walk | 34 | 38 |  |
| `sentinelle_hibou` | 38 | 256 | 3 | attack, death, idle, walk | 58 | 66 |  |
| `minotaure` | 54 | 256 | 3 | attack, death, idle, walk | 70 | 76 |  |
| `morghul` | 108 | 512 | 16 | attack, death, idle, walk | 100 | 93 |  |
| `gardien_citadelle` | 105 | 512 | 3 | attack, death, idle, walk | 106 | 107 |  |
| `seigneur_squelette` | 84 | 256 | 25 | attack, death, idle, walk | 88 | 98 |  |
| `gobelin` | 54 | 128 | 2 | attack, cast, death, idle, walk | 27 | 26 |  |
| `orc_guerrier` | 55 | 128 | 2 | attack, cast, death, idle, walk | 33 | 40 |  |
| `bandit_arbaletrier` | 47 | 128 | 2 | attack, cast, death, idle, walk | 32 | 36 |  |
| `squelette_maudit` | 56 | 128 | 3 | attack, cast, death, idle, walk | 33 | 37 |  |
| `zombie_des_cryptes` | 34 | 128 | 1 | attack, cast, death, idle, walk | 33 | 35 |  |
| `chevalier_neant` | 54 | 128 | 8 | attack, cast, death, idle, walk | 38 | 46 |  |
| `spectre_givre` | 56 | 128 | 5 | attack, cast, death, idle, walk | 36 | 44 |  |
| `elementaire_feu` | 33 | 128 | 14 | attack, cast, death, idle, walk | 22 | 34 | hauteur très différente, à regarder |
| `golem_cristal` | 77 | 256 | 3 | attack, cast, death, idle, walk | 44 | 51 |  |
| `loup_sombre` | 46 | 128 | 2 | attack, cast, death, idle, walk | 21 | 26 |  |
| `araignee_venimeuse` | 54 | 128 | 9 | attack, cast, death, idle, walk | 8 | 15 | hauteur très différente, à regarder |
| `scarabee_racines` | 83 | 512 | 10 | attack, cast, death, idle, walk | 88 | 111 |  |
| `mante_epines` | 67 | 256 | 2 | attack, cast, death, idle, walk | 145 | 147 |  |
| `raie_celeste` | 41 | 512 | 14 | attack, cast, death, idle, walk | 55 | 50 |  |
| `hydre_corail` | 135 | 512 | 6 | attack, cast, death, idle, walk | 156 | 172 |  |
| `millepattes_ambre` | 141 | 512 | 2 | attack, cast, death, idle, walk | 66 | 69 |  |
| `meduse_oracles` | 146 | 512 | 78 | attack, cast, death, idle, walk | 151 | 166 |  |
| `crabe_bastion` | 74 | 512 | 5 | attack, cast, death, idle, walk | 84 | 94 |  |
| `papillon_eclipse` | 39 | 256 | 10 | attack, cast, death, idle, walk | 151 | 151 |  |
| `serpent_couronnes` | 118 | 512 | 14 | attack, cast, death, idle, walk | 216 | 209 |  |
| `oeil_yggdrasil` | 145 | 512 | 32 | attack, cast, death, idle, walk | 220 | 226 |  |
| `crystal_neutral` | 45 | 256 | 15 | attack, cast, death, idle, walk | 65 | 70 |  |
| `crystal_red` | 45 | 256 | 15 | attack, cast, death, idle, walk | 65 | 70 |  |
| `crystal_blue` | 45 | 256 | 15 | attack, cast, death, idle, walk | 65 | 70 |  |
| `arena_drone` | 11 | 64 | 1 | attack, cast, death, idle, walk | 14 | 13 |  |

Modèles : 63. Problèmes : 0.
