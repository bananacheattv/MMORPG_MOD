# Controles des 22 montures

Controles faits par script sur les fichiers livres, sans lancer le jeu. Les tests en jeu restent a faire.

| Monture | Cubes | Os | Texture | Clips | Os historiques | Selle (haut / attendu) | Bilan |
|---|---|---|---|---|---|---|---|
| `araignee_cavernes` | 40 | 20 | 256x256 | idle, walk | 10 ok | 20 / 20 | OK |
| `bison_geant` | 62 | 16 | 512x512 | idle, jump, run, walk | 7 ok | 32.5 / 32.5 | OK |
| `cerf_sylvestre` | 66 | 16 | 256x256 | idle, jump, run, walk | 7 ok | 29 / 29 | OK |
| `chevre_celeste` | 53 | 16 | 256x256 | idle, jump, run, walk | 7 ok | 24 / 24 | OK |
| `chimere_arene` | 80 | 20 | 256x256 | idle, jump, run, walk | 7 ok | 24 / 24 | OK |
| `drake_ascendant` | 90 | 23 | 256x256 | fly, idle, jump, run, walk | 9 ok | 26.5 / 26.5 | OK |
| `drake_caravane` | 105 | 19 | 512x512 | idle, jump, run, walk | 7 ok | 33 / 33 | OK |
| `elephant_guerre` | 91 | 18 | 512x512 | idle, jump, run, walk | 7 ok | 42 / 42 | OK |
| `felin_vide` | 56 | 18 | 256x256 | idle, jump, run, walk | 7 ok | 22.5 / 22.5 | OK |
| `hippogriffe` | 56 | 21 | 256x256 | fly, idle, jump, run, walk | 9 ok | 26.5 / 26.5 | OK |
| `lezard_lave` | 54 | 14 | 256x256 | idle, jump, run, walk | 7 ok | 16 / 16 | OK |
| `lion_dore` | 75 | 18 | 512x512 | idle, jump, run, walk | 7 ok | 27 / 27 | OK |
| `loup_givre` | 64 | 18 | 256x256 | idle, jump, run, walk | 7 ok | 23 / 23 | OK |
| `mammouth` | 80 | 17 | 512x512 | idle, jump, run, walk | 7 ok | 40 / 40 | OK |
| `ours_runique` | 64 | 17 | 256x256 | idle, jump, run, walk | 7 ok | 24 / 24 | OK |
| `raptor_sables` | 49 | 14 | 256x256 | idle, jump, run, walk | 7 ok | 23 / 23 | OK |
| `rhinoceros_cuirasse` | 74 | 15 | 512x512 | idle, jump, run, walk | 7 ok | 27 / 27 | OK |
| `sanglier` | 87 | 18 | 256x256 | idle, jump, run, walk | 7 ok | 21 / 21 | OK |
| `scorpion_dunes` | 60 | 25 | 256x256 | idle, walk | 11 ok | 15.5 / 15.5 | OK |
| `taureau_guerre` | 68 | 18 | 256x256 | idle, jump, run, walk | 7 ok | 28 / 28 | OK |
| `tortue_cristal` | 52 | 8 | 512x512 | idle, jump, run, walk | 6 ok | 20 / 20 | OK |
| `tortue_forteresse` | 77 | 15 | 512x512 | idle, jump, run, walk | 7 ok | 28 / 28 | OK |

Bilan : 0 monture(s) avec un probleme.

## Controle dans Blockbench 5.2.2

Trois vitrines contenant les 22 modeles ouvertes dans Blockbench : 701 + 436 + 366 cubes charges, validation Blockbench sans diagnostic, captures dans `apercus/apres`.
Aller-retour complet (ouvrir, re-sauver, comparer) : fait pour le sanglier et le puits ; a refaire pour les 20 autres quand Blockbench autorise de nouveau l'ecriture dans le dossier.

## Reste a tester en jeu

- Position du cavalier sur chaque monture, a toutes les places.
- Enchainement idle, walk, et fly pour l'hippogriffe et le drake ascendant.
- Clips run et jump : fournis, non joues par le code actuel.
- Scorpion et araignee : pattes au sol pendant la marche.
