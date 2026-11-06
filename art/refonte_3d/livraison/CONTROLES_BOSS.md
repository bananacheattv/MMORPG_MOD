# Contrôles des boss

Script `outils/controle_boss.py`, sans lancer le jeu. Le squelette et les animations viennent des modules d'origine (`art/boss_blockbench/<id>.py`) : seuls les volumes et les textures changent.

| Boss | Os avant | Os après | Cubes avant | Cubes après | Texture avant | Texture après | Animations | Remarque |
|---|---:|---:|---:|---:|---|---|---|---|
| `roi_gobelin` | 24 | 24 | 61 | 72 | 128x256 | 128x256 | repos, marche, coup_horizontal, frappe_verticale, cri_ralliement, etourdi, mort, rage_repos, rage_marche |  |
| `liche_ancienne` | 23 | 23 | 66 | 77 | 128x256 | 128x256 | repos, deplacement, projectile, invocation, canalisation, recul, mort |  |
| `ignis` | 35 | 37 | 75 | 87 | 128x256 | 256x256 | repos, marche, balayage, frappe_sol, souffle_feu, rugissement, mort | cornes et piques un peu plus longues : pivots des segments décalés, sans effet sur les clips |
| `titan_glace` | 25 | 25 | 56 | 73 | 512x128 | 256x512 | reveil, repos, marche, frappe_poing, double_frappe_sol, pietinement, exposition_noyau, mort |  |
| `avatar_neant` | 34 | 34 | 59 | 68 | 128x128 | 128x128 | levitation, deplacement, charge_energie, ouverture_torse, teleportation_preparation, reformation, mort |  |

Boss : 5. Problèmes : 0.

`<Boss>Anims.java` est identique à l'original, commentaire d'en-tête mis à part : indices, durées et instants de frappe ne changent pas, le code serveur reste valable.
