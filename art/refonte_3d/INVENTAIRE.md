# Inventaire des montures, blocs, mobs et boss

Copie du depot : commit `892a216`. Mesures automatiques (`outils/inventaire.py`).

**Legende.** Densite : texels par unite de modele (mediane, min-max). « Scintille » : paires de faces coplanaires qui se recouvrent et restent visibles. « Trous » : faces sans texture mais exposees.

## Montures (22)

| Monture | Id | Rarete | Sieges | Hauteur selle attendue | Cubes | Os | Texture | Densite | Scintille | Trous | Animations | Etat |
|---|---|---|---|---|---:|---:|---|---|---:|---:|---|---|
| Sanglier de guerre | `sanglier` | PEU_COMMUN | 1 | 21.16 u | 89 | 7 | 512x512 | 3.2 (1.8-84.0) | 5 | 0 | idle 3s, walk 0.9s | VALIDE |
| Loup de givre | `loup_givre` | RARE | 1 | 23.24 u | 92 | 7 | 512x512 | 3.1 (1.0-55.6) | 0 | 0 | idle 3s, walk 0.75s | REFAIT, a valider |
| Cerf sylvestre | `cerf_sylvestre` | RARE | 1 | 29.16 u | 97 | 7 | 512x512 | 3.1 (1.2-55.6) | 2 | 0 | idle 3s, walk 0.85s | REFAIT, a valider |
| Tortue de cristal | `tortue_cristal` | RARE | 1 | 20.20 u | 75 | 7 | 512x512 | 3.3 (1.5-94.4) | 6 | 0 | idle 4s, walk 1.4s | REFAIT, a valider |
| Chèvre céleste | `chevre_celeste` | RARE | 1 | 24.20 u | 83 | 7 | 512x512 | 3.3 (2.2-61.1) | 0 | 0 | idle 3s, walk 0.75s | REFAIT, a valider |
| Lézard de lave | `lezard_lave` | EPIQUE | 1 | 16.20 u | 94 | 7 | 512x512 | 3.1 (0.9-66.7) | 2 | 0 | idle 3s, walk 0.7s | REFAIT, a valider |
| Raptor des sables | `raptor_sables` | EPIQUE | 1 | 23.24 u | 88 | 7 | 512x512 | 3.1 (0.9-61.1) | 5 | 0 | idle 3s, walk 0.6s | REFAIT, a valider |
| Ours runique | `ours_runique` | EPIQUE | 1 | 24.20 u | 82 | 7 | 512x512 | 3.3 (2.2-94.4) | 0 | 0 | idle 3s, walk 1s | REFAIT, a valider |
| Scorpion des dunes | `scorpion_dunes` | EPIQUE | 1 | 15.40 u | 78 | 12 | 512x512 | 3.3 (2.5-168.0) | 0 | 0 | idle 3s, walk 0.6s | REFAIT, a valider |
| Félin du vide | `felin_vide` | LEGENDAIRE | 1 | 22.28 u | 86 | 7 | 512x512 | 3.2 (1.6-61.1) | 0 | 0 | idle 3s, walk 0.65s | REFAIT, a valider |
| Araignée des cavernes | `araignee_cavernes` | LEGENDAIRE | 1 | 20.20 u | 90 | 10 | 512x512 | 3.2 (2.5-166.7) | 68 | 0 | idle 3s, walk 0.5s | REFAIT, a valider |
| Hippogriffe | `hippogriffe` | MYTHIQUE | 1 | 26.60 u | 106 | 9 | 512x512 | 6.7 (2.2-61.1) | 16 | 0 | idle 3s, walk 0.8s, fly 0.7s | REFAIT, a valider |
| Chimère des arènes | `chimere_arene` | MYTHIQUE | 1 | 24.20 u | 72 | 7 | 512x512 | 3.0 (2.2-4.0) | 4 | 0 | idle 3s, walk 0.75s | REFAIT, a valider |
| Taureau de guerre | `taureau_guerre` | PEU_COMMUN | 2 | 28.04 u | 53 (local non pousse : 528 cubes, 1024 px) | 7 | 512x512 | 3.0 (2.5-3.3) | 10 | 0 | idle 3s, walk 0.8s | REFAIT, a valider |
| Lion doré | `lion_dore` | RARE | 2 | 27.08 u | 56 (local non pousse : 738 cubes, 1024 px) | 7 | 512x512 | 3.0 (2.5-3.3) | 12 | 0 | idle 3s, walk 0.7s | REFAIT, a valider |
| Rhinocéros cuirassé | `rhinoceros_cuirasse` | RARE | 3 | 27.08 u | 56 (local non pousse : 573 cubes, 1024 px) | 7 | 512x512 | 2.4 (1.7-3.3) | 11 | 0 | idle 3s, walk 0.95s | REFAIT, a valider |
| Bison géant | `bison_geant` | EPIQUE | 3 | 32.60 u | 52 (local non pousse : 726 cubes, 1024 px) | 7 | 512x512 | 2.4 (2.0-3.3) | 10 | 0 | idle 3s, walk 0.9s | REFAIT, a valider |
| Mammouth laineux | `mammouth` | EPIQUE | 4 | 40.04 u | 61 (local non pousse : 960 cubes, 1024 px) | 7 | 512x512 | 2.0 (1.7-3.3) | 10 | 0 | idle 3s, walk 1s | REFAIT, a valider |
| Éléphant de guerre | `elephant_guerre` | LEGENDAIRE | 4 | 42.12 u | 73 (local non pousse : 938 cubes, 1024 px) | 7 | 512x512 | 2.0 (1.7-3.3) | 11 | 0 | idle 3s, walk 1s | REFAIT, a valider |
| Tortue forteresse | `tortue_forteresse` | LEGENDAIRE | 5 | 28.04 u | 51 (local non pousse : 545 cubes, 1024 px) | 7 | 512x512 | 2.0 (1.7-3.3) | 12 | 0 | idle 3s, walk 1.1s | REFAIT, a valider |
| Drake de caravane | `drake_caravane` | MYTHIQUE | 5 | 33.00 u | 68 (local non pousse : 706 cubes, 1024 px) | 7 | 512x512 | 2.0 (1.7-3.3) | 10 | 0 | idle 3s, walk 0.85s | REFAIT, a valider |
| Drake ascendant | `drake_ascendant` | MYTHIQUE | 1 | 26.60 u | 106 | 9 | 512x512 **PNG ABSENT** | 6.7 (2.2-61.1) | 16 | 0 | idle 3s, walk 0.8s, fly 0.7s | REFAIT, a valider |

Geometries strictement identiques : [['hippogriffe', 'drake_ascendant']].

## Blocs 3D : decor anime (2)

| Id | Cubes | Emprise (blocs) | Hauteur | Texture | Densite | Hors grille | Scintille | Trous | Etat |
|---|---:|---|---:|---|---|---:|---:|---:|---|
| `camp` | 60 | 5.6 x 4.0 | 1.4 | 64x352 | 1.0 (0.2-2.8) | 70% | 22 | 0 | REFAIT, a valider |
| `drapeau` | 7 | 0.1 x 1.2 | 1.7 | 64x352 | 1.0 (1.0-4.0) | 0% | 0 | 0 | REFAIT, a valider |

## Blocs 3D : citadelle (deco_) (30)

| Id | Cubes | Emprise (blocs) | Hauteur | Texture | Densite | Hors grille | Scintille | Trous | Etat |
|---|---:|---|---:|---|---|---:|---:|---:|---|
| `deco_arche` | 71 | 8.5 x 1.8 | 12.8 | 2048x1792 | 5.3 (4.9-5.6) | 89% | 28 | 0 | REFAIT, a valider |
| `deco_balise_runique` | 39 | 2.2 x 2.0 | 6.2 | 2048x1792 | 5.3 (5.0-5.5) | 43% | 1 | 0 | REFAIT, a valider |
| `deco_banc` | 32 | 3.0 x 1.1 | 2.9 | 2048x1792 | 5.3 (5.0-6.2) | 66% | 74 | 0 | REFAIT, a valider |
| `deco_bassin` | 29 | 5.0 x 5.0 | 3.4 | 2048x1792 | 5.3 (5.2-5.5) | 18% | 4 | 0 | REFAIT, a valider |
| `deco_bibliotheque` | 142 | 4.2 x 1.4 | 7.8 | 2048x1792 | 5.3 (5.0-5.8) | 43% | 66 | 0 | REFAIT, a valider |
| `deco_brasero` | 37 | 2.4 x 2.4 | 4.4 | 2048x1792 | 5.3 (5.1-5.6) | 54% | 1 | 0 | REFAIT, a valider |
| `deco_caisses_empilees` | 87 | 3.0 x 3.1 | 2.6 | 2048x1792 | 5.3 (5.0-6.3) | 69% | 114 | 0 | REFAIT, a valider |
| `deco_chandelier` | 72 | 1.6 x 1.7 | 4.8 | 2048x1792 | 5.4 (5.0-5.6) | 59% | 30 | 0 | REFAIT, a valider |
| `deco_colonne_ornee` | 44 | 1.5 x 1.5 | 6.2 | 2048x1792 | 5.3 (5.0-5.5) | 58% | 0 | 0 | REFAIT, a valider |
| `deco_cristal_flottant` | 17 | 2.3 x 2.3 | 5.6 | 2048x1792 | 5.3 (5.0-5.6) | 20% | 1 | 0 | REFAIT, a valider |
| `deco_enclume_geante` | 40 | 4.0 x 1.5 | 2.9 | 2048x1792 | 5.3 (5.0-6.2) | 67% | 97 | 0 | REFAIT, a valider |
| `deco_etal_marche` | 69 | 4.5 x 2.6 | 5.0 | 2048x1792 | 5.3 (5.0-6.2) | 43% | 53 | 0 | REFAIT, a valider |
| `deco_etendard` | 29 | 2.4 x 0.6 | 7.5 | 2048x1792 | 5.3 (5.2-5.6) | 52% | 1 | 0 | REFAIT, a valider |
| `deco_fontaine` | 66 | 4.0 x 4.0 | 5.1 | 2048x1792 | 5.3 (5.0-5.6) | 52% | 9 | 0 | REFAIT, a valider |
| `deco_gargouille` | 41 | 4.2 x 1.9 | 5.2 | 2048x1792 | 5.3 (5.0-5.7) | 55% | 32 | 0 | REFAIT, a valider |
| `deco_globe_arcane` | 61 | 2.9 x 2.9 | 3.9 | 2048x1792 | 5.4 (5.0-5.5) | 83% | 37 | 0 | REFAIT, a valider |
| `deco_grille_forgee` | 81 | 6.3 x 0.9 | 10.4 | 2048x1792 | 5.3 (5.0-5.8) | 85% | 0 | 0 | REFAIT, a valider |
| `deco_horloge` | 57 | 2.0 x 2.7 | 9.9 | 2048x1792 | 5.3 (5.0-5.8) | 73% | 18 | 0 | REFAIT, a valider |
| `deco_lampadaire` | 40 | 1.1 x 1.1 | 6.2 | 2048x1792 | 5.3 (5.0-5.7) | 65% | 0 | 0 | REFAIT, a valider |
| `deco_lanterne_potence` | 23 | 2.6 x 0.8 | 5.1 | 2048x1792 | 5.3 (5.0-5.6) | 46% | 3 | 0 | REFAIT, a valider |
| `deco_obelisque` | 24 | 2.2 x 2.2 | 8.4 | 2048x1792 | 5.3 (5.1-5.6) | 61% | 0 | 0 | REFAIT, a valider |
| `deco_puits` | 67 | 2.5 x 2.5 | 7.0 | 2048x1792 | 5.3 (5.0-5.5) | 67% | 68 | 0 | VALIDE |
| `deco_ratelier_armes` | 40 | 3.9 x 1.2 | 5.6 | 2048x1792 | 5.3 (4.6-5.8) | 43% | 2 | 0 | REFAIT, a valider |
| `deco_sablier_geant` | 80 | 2.0 x 2.0 | 6.0 | 2048x1792 | 5.3 (5.1-5.6) | 70% | 4 | 0 | REFAIT, a valider |
| `deco_statue_guerrier` | 32 | 2.1 x 1.7 | 5.9 | 2048x1792 | 5.3 (5.0-6.3) | 59% | 28 | 0 | REFAIT, a valider |
| `deco_statue_mage` | 45 | 1.9 x 1.7 | 6.9 | 2048x1792 | 5.3 (5.0-6.3) | 63% | 40 | 0 | REFAIT, a valider |
| `deco_table_banquet` | 24 | 4.6 x 1.6 | 2.5 | 2048x1792 | 5.3 (5.0-6.0) | 42% | 4 | 0 | REFAIT, a valider |
| `deco_tonneau_geant` | 75 | 2.5 x 2.5 | 3.0 | 2048x1792 | 5.3 (5.0-6.2) | 69% | 62 | 0 | REFAIT, a valider |
| `deco_trone` | 43 | 2.7 x 2.7 | 8.1 | 2048x1792 | 5.3 (5.0-6.3) | 40% | 26 | 0 | REFAIT, a valider |
| `deco_vase_geant` | 56 | 1.9 x 1.9 | 4.1 | 2048x1792 | 5.3 (5.0-5.8) | 57% | 66 | 0 | REFAIT, a valider |

## Blocs 3D : cathedrale (deco_cat_) (40)

| Id | Cubes | Emprise (blocs) | Hauteur | Texture | Densite | Hors grille | Scintille | Trous | Etat |
|---|---:|---|---:|---|---|---:|---:|---:|---|
| `deco_cat_ange` | 81 | 4.0 x 1.9 | 7.0 | 2048x1792 | 5.3 (5.0-5.6) | 70% | 3 | 0 | REFAIT, a valider |
| `deco_cat_applique` | 61 | 2.9 x 1.3 | 4.4 | 2048x1792 | 5.3 (5.0-6.3) | 72% | 9 | 0 | REFAIT, a valider |
| `deco_cat_arc_brise` | 352 | 6.9 x 1.9 | 12.8 | 2048x1792 | 5.3 (5.1-5.8) | 92% | 12 | 0 | REFAIT, a valider |
| `deco_cat_arcade_cloitre` | 337 | 10.5 x 1.4 | 11.8 | 2048x1792 | 5.3 (5.1-5.6) | 92% | 12 | 0 | REFAIT, a valider |
| `deco_cat_autel_majeur` | 298 | 5.5 x 2.8 | 7.2 | 2048x1792 | 5.3 (4.8-6.7) | 70% | 33 | 0 | REFAIT, a valider |
| `deco_cat_baldaquin` | 693 | 5.0 x 5.0 | 12.1 | 2048x1792 | 5.3 (4.8-6.3) | 57% | 644 | 0 | REFAIT, a valider |
| `deco_cat_balustrade_choeur` | 392 | 9.7 x 1.0 | 7.1 | 2048x1792 | 5.4 (4.9-5.6) | 93% | 3 | 0 | REFAIT, a valider |
| `deco_cat_banc_nef` | 50 | 6.0 x 1.9 | 4.8 | 2048x1792 | 5.3 (5.0-5.8) | 30% | 2 | 0 | REFAIT, a valider |
| `deco_cat_benitier` | 60 | 1.8 x 1.8 | 2.8 | 2048x1792 | 5.3 (5.0-5.6) | 62% | 1 | 0 | REFAIT, a valider |
| `deco_cat_candelabre` | 205 | 4.1 x 1.6 | 6.6 | 2048x1792 | 5.4 (4.3-6.7) | 78% | 259 | 0 | REFAIT, a valider |
| `deco_cat_candelabre_geant` | 240 | 3.1 x 2.8 | 9.4 | 2048x1792 | 5.3 (4.3-6.7) | 67% | 49 | 0 | REFAIT, a valider |
| `deco_cat_chaire` | 83 | 2.5 x 2.5 | 9.4 | 2048x1792 | 5.3 (5.2-5.5) | 54% | 3 | 0 | REFAIT, a valider |
| `deco_cat_cierge` | 56 | 1.6 x 1.6 | 7.5 | 2048x1792 | 5.3 (5.0-5.6) | 57% | 6 | 0 | REFAIT, a valider |
| `deco_cat_clocheton` | 156 | 1.7 x 1.7 | 8.8 | 2048x1792 | 5.3 (4.7-6.0) | 81% | 20 | 0 | REFAIT, a valider |
| `deco_cat_colonne_torsadee` | 125 | 1.6 x 1.6 | 9.1 | 2048x1792 | 5.2 (5.2-5.5) | 78% | 0 | 0 | REFAIT, a valider |
| `deco_cat_confessionnal` | 73 | 4.0 x 2.6 | 7.5 | 2048x1792 | 5.3 (4.8-6.0) | 47% | 12 | 0 | REFAIT, a valider |
| `deco_cat_contrefort` | 57 | 7.1 x 1.7 | 12.6 | 2048x1792 | 5.3 (5.0-5.6) | 74% | 36 | 0 | REFAIT, a valider |
| `deco_cat_croix` | 29 | 4.9 x 2.8 | 10.0 | 2048x1792 | 5.3 (5.0-5.5) | 25% | 0 | 0 | REFAIT, a valider |
| `deco_cat_encensoir` | 86 | 2.4 x 1.4 | 6.1 | 2048x1792 | 5.3 (5.0-6.0) | 63% | 13 | 0 | REFAIT, a valider |
| `deco_cat_escalier_helicoidal` | 86 | 3.5 x 3.4 | 9.0 | 2048x1792 | 5.3 (5.0-5.6) | 62% | 8 | 0 | REFAIT, a valider |
| `deco_cat_fleche` | 96 | 2.5 x 2.5 | 11.5 | 2048x1792 | 5.3 (5.0-5.8) | 56% | 6 | 0 | REFAIT, a valider |
| `deco_cat_fonts_baptismaux` | 101 | 2.8 x 2.8 | 8.4 | 2048x1792 | 5.3 (5.2-5.6) | 57% | 3 | 0 | REFAIT, a valider |
| `deco_cat_gargouille` | 1220 | 10.8 x 6.8 | 12.4 | 2048x1792 | 5.3 (4.8-5.8) | 55% | 50 | 0 | REFAIT, a valider |
| `deco_cat_gisant` | 124 | 5.5 x 2.0 | 3.3 | 2048x1792 | 5.2 (5.0-5.8) | 66% | 17 | 0 | REFAIT, a valider |
| `deco_cat_horloge` | 287 | 3.7 x 2.7 | 11.0 | 2048x1792 | 5.3 (5.0-6.0) | 69% | 1 | 0 | REFAIT, a valider |
| `deco_cat_lutrin_aigle` | 60 | 3.1 x 2.0 | 6.0 | 2048x1792 | 5.3 (5.0-5.6) | 54% | 0 | 0 | REFAIT, a valider |
| `deco_cat_orgue` | 530 | 6.4 x 2.0 | 10.0 | 2048x1792 | 5.3 (5.0-6.2) | 67% | 43 | 0 | REFAIT, a valider |
| `deco_cat_pilier_faisceau` | 123 | 2.7 x 2.7 | 9.4 | 2048x1792 | 5.3 (5.0-5.5) | 69% | 41 | 0 | REFAIT, a valider |
| `deco_cat_porche` | 535 | 7.5 x 4.4 | 12.8 | 2048x1792 | 5.3 (5.1-5.6) | 89% | 44 | 0 | REFAIT, a valider |
| `deco_cat_portail` | 443 | 8.1 x 2.0 | 12.8 | 2048x1792 | 5.3 (4.9-5.8) | 97% | 28 | 0 | REFAIT, a valider |
| `deco_cat_puits_lumiere` | 149 | 2.8 x 2.8 | 8.4 | 2048x1792 | 5.3 (5.2-5.6) | 48% | 12 | 0 | REFAIT, a valider |
| `deco_cat_reliquaire` | 82 | 3.3 x 2.8 | 5.1 | 2048x1792 | 5.3 (5.0-6.0) | 54% | 16 | 0 | REFAIT, a valider |
| `deco_cat_reliquaire_cristal` | 64 | 1.7 x 1.7 | 5.8 | 2048x1792 | 5.3 (5.0-5.6) | 58% | 2 | 0 | REFAIT, a valider |
| `deco_cat_rosace` | 226 | 5.8 x 1.0 | 7.9 | 2048x1792 | 5.3 (4.8-6.0) | 65% | 2 | 0 | REFAIT, a valider |
| `deco_cat_stalle` | 193 | 3.5 x 1.8 | 7.9 | 2048x1792 | 5.3 (4.8-6.0) | 75% | 2 | 0 | REFAIT, a valider |
| `deco_cat_statue_saint` | 158 | 2.8 x 1.2 | 6.7 | 2048x1792 | 5.4 (5.0-6.0) | 56% | 6 | 0 | REFAIT, a valider |
| `deco_cat_tapisserie` | 66 | 4.9 x 0.4 | 8.0 | 2048x1792 | 5.3 (5.0-5.9) | 58% | 6 | 0 | REFAIT, a valider |
| `deco_cat_triptyque` | 655 | 7.6 x 0.8 | 9.2 | 2048x1792 | 5.3 (4.6-10.0) | 61% | 34 | 0 | REFAIT, a valider |
| `deco_cat_trone_eveque` | 212 | 3.8 x 2.5 | 10.6 | 2048x1792 | 5.3 (5.0-6.7) | 68% | 10 | 0 | REFAIT, a valider |
| `deco_cat_vitrail` | 425 | 4.9 x 1.1 | 10.4 | 2048x1792 | 5.3 (4.8-6.0) | 56% | 16 | 0 | REFAIT, a valider |

## Blocs 3D : citadelle Blockbench (decob_) (51)

| Id | Cubes | Emprise (blocs) | Hauteur | Texture | Densite | Hors grille | Scintille | Trous | Etat |
|---|---:|---|---:|---|---|---:|---:|---:|---|
| `decob_arbre_cristal` | 12 | 4.0 x 3.2 | 4.8 | 2048x1792 | 5.3 (5.3-5.5) | 76% | 0 | 0 | REFAIT, a valider |
| `decob_arbre_nain` | 7 | 3.6 x 2.4 | 4.0 | 2048x1792 | 5.3 (5.2-5.4) | 83% | 0 | 0 | REFAIT, a valider |
| `decob_armoire_geante` | 66 | 3.3 x 2.3 | 5.0 | 2048x1792 | 5.3 (5.0-6.2) | 66% | 54 | 0 | REFAIT, a valider |
| `decob_armure_exposition` | 35 | 3.3 x 2.0 | 6.0 | 2048x1792 | 5.3 (5.0-6.2) | 84% | 72 | 0 | REFAIT, a valider |
| `decob_autel_lumiere` | 10 | 3.2 x 3.2 | 5.6 | 2048x1792 | 5.3 (5.3-5.6) | 63% | 0 | 0 | REFAIT, a valider |
| `decob_autel_runique` | 31 | 4.4 x 4.4 | 5.0 | 2048x1792 | 5.3 (5.0-6.2) | 72% | 48 | 0 | REFAIT, a valider |
| `decob_baliste` | 12 | 4.8 x 4.6 | 2.8 | 2048x1792 | 5.3 (5.0-5.5) | 81% | 0 | 0 | REFAIT, a valider |
| `decob_balustrade` | 60 | 8.9 x 2.9 | 8.3 | 2048x1792 | 5.3 (4.9-5.6) | 79% | 58 | 0 | REFAIT, a valider |
| `decob_brasero_double` | 27 | 4.8 x 1.6 | 5.0 | 2048x1792 | 5.3 (5.0-5.8) | 73% | 48 | 0 | REFAIT, a valider |
| `decob_buisson_fleuri` | 18 | 4.0 x 4.0 | 3.6 | 2048x1792 | 5.3 (5.3-5.4) | 62% | 0 | 0 | REFAIT, a valider |
| `decob_bureau_scribe` | 47 | 4.1 x 2.1 | 4.0 | 2048x1792 | 5.3 (5.0-6.2) | 79% | 52 | 0 | REFAIT, a valider |
| `decob_canon_decoratif` | 50 | 4.2 x 4.0 | 2.6 | 2048x1792 | 5.3 (5.0-6.3) | 83% | 98 | 0 | REFAIT, a valider |
| `decob_cercle_invocation` | 19 | 4.4 x 4.4 | 0.4 | 2048x1792 | 5.3 (5.3-5.6) | 67% | 10 | 0 | REFAIT, a valider |
| `decob_chariot_marchand` | 71 | 5.7 x 2.5 | 3.0 | 2048x1792 | 5.3 (5.0-6.2) | 84% | 126 | 0 | REFAIT, a valider |
| `decob_cheminee` | 76 | 4.4 x 1.7 | 6.0 | 2048x1792 | 5.3 (5.0-6.2) | 83% | 122 | 0 | REFAIT, a valider |
| `decob_coffre_tresor` | 33 | 3.4 x 2.3 | 3.6 | 2048x1792 | 5.3 (5.0-6.2) | 78% | 41 | 0 | REFAIT, a valider |
| `decob_colonne_lumineuse` | 36 | 2.0 x 2.0 | 5.8 | 2048x1792 | 5.3 (5.0-5.8) | 77% | 28 | 0 | REFAIT, a valider |
| `decob_comptoir_taverne` | 56 | 5.0 x 1.9 | 4.1 | 2048x1792 | 5.3 (5.0-6.2) | 79% | 82 | 0 | REFAIT, a valider |
| `decob_creneau` | 124 | 9.0 x 4.1 | 6.1 | 2048x1792 | 5.3 (4.9-5.6) | 82% | 191 | 0 | REFAIT, a valider |
| `decob_dais` | 17 | 4.8 x 4.8 | 1.9 | 2048x1792 | 5.3 (5.0-5.4) | 65% | 0 | 0 | REFAIT, a valider |
| `decob_dragon_pierre` | 77 | 5.2 x 4.4 | 5.4 | 2048x1792 | 5.3 (5.0-6.2) | 79% | 112 | 0 | REFAIT, a valider |
| `decob_escalier_monumental` | 61 | 8.2 x 8.2 | 6.1 | 2048x1792 | 5.3 (5.3-5.5) | 53% | 4 | 0 | REFAIT, a valider |
| `decob_fauteuil_royal` | 34 | 2.5 x 2.0 | 5.0 | 2048x1792 | 5.3 (5.0-6.2) | 66% | 56 | 0 | REFAIT, a valider |
| `decob_fenetre_gothique` | 35 | 7.1 x 2.0 | 9.9 | 2048x1792 | 5.3 (5.1-5.5) | 79% | 14 | 0 | REFAIT, a valider |
| `decob_fontaine_murale` | 34 | 3.6 x 2.8 | 5.0 | 2048x1792 | 5.3 (5.0-5.8) | 72% | 31 | 0 | REFAIT, a valider |
| `decob_geode` | 64 | 4.2 x 3.8 | 4.8 | 2048x1792 | 5.3 (5.0-6.2) | 69% | 84 | 0 | REFAIT, a valider |
| `decob_haie_taillee` | 13 | 8.6 x 2.2 | 6.1 | 2048x1792 | 5.3 (5.3-5.4) | 79% | 0 | 0 | REFAIT, a valider |
| `decob_herse` | 28 | 6.8 x 1.4 | 8.8 | 2048x1792 | 5.3 (5.1-5.4) | 76% | 0 | 0 | REFAIT, a valider |
| `decob_jardiniere_geante` | 121 | 4.2 x 1.8 | 2.8 | 2048x1792 | 5.3 (5.0-6.2) | 74% | 56 | 0 | REFAIT, a valider |
| `decob_lanterne_sol` | 10 | 1.6 x 1.6 | 4.0 | 2048x1792 | 5.3 (5.3-5.4) | 87% | 0 | 0 | REFAIT, a valider |
| `decob_lit_royal` | 81 | 4.4 x 5.2 | 5.1 | 2048x1792 | 5.3 (5.0-6.2) | 63% | 70 | 0 | REFAIT, a valider |
| `decob_lustre_cristal` | 17 | 4.0 x 3.3 | 4.8 | 2048x1792 | 5.3 (5.3-5.6) | 74% | 0 | 0 | REFAIT, a valider |
| `decob_lutrin` | 8 | 2.8 x 1.6 | 4.8 | 2048x1792 | 5.3 (5.2-5.4) | 85% | 0 | 0 | REFAIT, a valider |
| `decob_mur_orne` | 110 | 9.2 x 2.3 | 8.3 | 2048x1792 | 5.3 (4.9-5.6) | 74% | 60 | 0 | REFAIT, a valider |
| `decob_parterre_fleurs` | 18 | 4.8 x 4.8 | 2.3 | 2048x1792 | 5.3 (5.3-5.5) | 67% | 0 | 0 | REFAIT, a valider |
| `decob_passerelle` | 62 | 8.2 x 3.1 | 6.3 | 2048x1792 | 5.3 (5.1-5.5) | 90% | 56 | 0 | REFAIT, a valider |
| `decob_phare_arcane` | 68 | 3.2 x 3.2 | 6.0 | 2048x1792 | 5.3 (5.0-6.2) | 67% | 57 | 0 | REFAIT, a valider |
| `decob_pilier_court` | 27 | 2.5 x 2.5 | 4.2 | 2048x1792 | 5.3 (5.0-5.6) | 83% | 28 | 0 | REFAIT, a valider |
| `decob_pilier_cristal_trio` | 13 | 3.2 x 3.2 | 6.2 | 2048x1792 | 5.3 (5.3-5.4) | 60% | 0 | 0 | REFAIT, a valider |
| `decob_portail_monumental` | 124 | 8.5 x 2.7 | 10.9 | 2048x1792 | 5.3 (5.1-5.5) | 81% | 86 | 0 | REFAIT, a valider |
| `decob_portail_runique` | 31 | 7.5 x 2.0 | 10.2 | 2048x1792 | 5.3 (5.1-5.5) | 86% | 4 | 0 | REFAIT, a valider |
| `decob_pylone_arcane` | 47 | 2.4 x 2.4 | 6.0 | 2048x1792 | 5.3 (5.0-6.2) | 82% | 72 | 0 | REFAIT, a valider |
| `decob_rocher_moussu` | 95 | 4.9 x 3.8 | 4.8 | 2048x1792 | 5.3 (5.0-6.2) | 73% | 110 | 0 | REFAIT, a valider |
| `decob_saule_arcane` | 18 | 4.4 x 4.4 | 4.6 | 2048x1792 | 5.3 (5.0-5.5) | 67% | 1 | 0 | REFAIT, a valider |
| `decob_sphere_cristal` | 9 | 3.2 x 3.2 | 4.2 | 2048x1792 | 5.3 (5.3-5.5) | 76% | 0 | 0 | REFAIT, a valider |
| `decob_stele_gravee` | 16 | 1.6 x 1.6 | 4.6 | 2048x1792 | 5.3 (5.0-5.8) | 91% | 24 | 0 | REFAIT, a valider |
| `decob_tabouret_geant` | 8 | 2.0 x 2.0 | 3.5 | 2048x1792 | 5.3 (5.3-5.4) | 56% | 0 | 0 | REFAIT, a valider |
| `decob_tente_commandement` | 21 | 4.4 x 4.4 | 5.6 | 2048x1792 | 5.3 (5.3-5.4) | 54% | 0 | 0 | REFAIT, a valider |
| `decob_torche_geante` | 7 | 1.6 x 1.6 | 5.8 | 2048x1792 | 5.3 (5.2-5.4) | 93% | 0 | 0 | REFAIT, a valider |
| `decob_totem_arcane` | 47 | 3.2 x 1.6 | 5.2 | 2048x1792 | 5.3 (5.0-5.8) | 82% | 54 | 0 | REFAIT, a valider |
| `decob_tourelle` | 38 | 3.6 x 3.6 | 5.2 | 2048x1792 | 5.3 (5.0-6.2) | 75% | 30 | 0 | REFAIT, a valider |

## Structures fonctionnelles animees (Modded Entity, modele Java + StructureRenderer) (8)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `autel_invocation` | 1 | 3 | garde (deja 16 texels par bloc, fait d apres les images du proprietaire) |
| `banque_aeloria` | 1 | 0 | garde (deja 16 texels par bloc, fait d apres les images du proprietaire) |
| `boite_aux_lettres` | 1 | 0 | garde (deja 16 texels par bloc, fait d apres les images du proprietaire) |
| `forge_arcanique` | 1 | 0 | garde (deja 16 texels par bloc, fait d apres les images du proprietaire) |
| `hdv_aeloria` | 1 | 0 | garde (deja 16 texels par bloc, fait d apres les images du proprietaire) |
| `presse_compression` | 1 | 0 | garde (deja 16 texels par bloc, fait d apres les images du proprietaire) |
| `quetes_hebdo` | 1 | 0 | garde (deja 16 texels par bloc, fait d apres les images du proprietaire) |
| `teleporteur` | 1 | 4 | garde (deja 16 texels par bloc, fait d apres les images du proprietaire) |

## Caisses (JSON Java + CrateRenderer) (7)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `caisse_commune_bloc` | 2 | 58 | REFAIT, a valider |
| `caisse_epique_bloc` | 2 | 60 | REFAIT, a valider |
| `caisse_legendaire_bloc` | 2 | 60 | REFAIT, a valider |
| `caisse_mythique_bloc` | 2 | 62 | REFAIT, a valider |
| `caisse_quete_bloc` | 2 | 58 | REFAIT, a valider |
| `caisse_rare_bloc` | 2 | 58 | REFAIT, a valider |
| `caisse_vote_bloc` | 2 | 58 | REFAIT, a valider |

## Lucky Blocks (JSON Java) (6)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `lucky_block` | 1 | 0 | REFAIT, a valider |
| `lucky_celeste` | 1 | 65 | REFAIT, a valider |
| `lucky_givre` | 1 | 67 | REFAIT, a valider |
| `lucky_infernal` | 1 | 65 | REFAIT, a valider |
| `lucky_neant` | 1 | 64 | REFAIT, a valider |
| `lucky_sylvestre` | 1 | 69 | REFAIT, a valider |

## Classements (JSON Java + rendu) (10)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `classement_arene` | 1 | 4 | REFAIT, a valider |
| `classement_boss` | 1 | 4 | REFAIT, a valider |
| `classement_donjons` | 1 | 4 | REFAIT, a valider |
| `classement_fortune` | 1 | 4 | REFAIT, a valider |
| `classement_guilde_banque` | 1 | 4 | REFAIT, a valider |
| `classement_guilde_gardiens` | 1 | 4 | REFAIT, a valider |
| `classement_guilde_niveau` | 1 | 4 | REFAIT, a valider |
| `classement_monstres` | 1 | 4 | REFAIT, a valider |
| `classement_niveau` | 1 | 4 | REFAIT, a valider |
| `classement_quetes` | 1 | 4 | REFAIT, a valider |

## Accessoires de decor (JSON Java Block) (11)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `armoire_aeloria` | 1 | 10 | REFAIT, a valider |
| `buche_camp` | 1 | 2 | REFAIT, a valider |
| `chaise_aeloria` | 1 | 14 | REFAIT, a valider |
| `feu_de_camp` | 1 | 19 | REFAIT, a valider |
| `jarre_ancienne` | 1 | 3 | REFAIT, a valider |
| `panneau_feuille` | 1 | 6 | REFAIT, a valider |
| `panneau_quetes` | 1 | 26 | modele d origine garde, a la demande du proprietaire ; face sans texture corrigee |
| `pot_decore` | 1 | 6 | REFAIT, a valider |
| `potion_etoilee` | 1 | 29 | REFAIT, a valider |
| `table_aeloria` | 1 | 6 | REFAIT, a valider |
| `viande_deco` | 1 | 7 | REFAIT, a valider |

## Cultures (croix) (5)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `culture_ambre` | 4 | 0 | REFAIT, a valider |
| `culture_argent` | 4 | 0 | REFAIT, a valider |
| `culture_givre` | 4 | 0 | REFAIT, a valider |
| `culture_lueur` | 4 | 0 | REFAIT, a valider |
| `culture_neant` | 4 | 0 | REFAIT, a valider |

## Minerais et materiaux (cube, dalle, escalier) (51)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `bois_ancien` | 1 | 0 | REFAIT, a valider |
| `bois_ancien_slab` | 3 | 0 | REFAIT, a valider |
| `bois_ancien_stairs` | 3 | 0 | REFAIT, a valider |
| `colonne_marbre` | 2 | 0 | REFAIT, a valider |
| `cristal_ambre` | 1 | 0 | REFAIT, a valider |
| `cristal_arcane` | 1 | 0 | REFAIT, a valider |
| `cristal_azur` | 1 | 0 | REFAIT, a valider |
| `dalle_runique` | 1 | 0 | REFAIT, a valider |
| `fer_noirci` | 1 | 0 | REFAIT, a valider |
| `fer_noirci_slab` | 3 | 0 | REFAIT, a valider |
| `fer_noirci_stairs` | 3 | 0 | REFAIT, a valider |
| `marbre` | 1 | 0 | REFAIT, a valider |
| `marbre_dore` | 1 | 0 | REFAIT, a valider |
| `marbre_poli` | 1 | 0 | REFAIT, a valider |
| `marbre_poli_slab` | 3 | 0 | REFAIT, a valider |
| `marbre_poli_stairs` | 3 | 0 | REFAIT, a valider |
| `marbre_slab` | 3 | 0 | REFAIT, a valider |
| `marbre_stairs` | 3 | 0 | REFAIT, a valider |
| `minerai_ambrasite` | 1 | 0 | REFAIT, a valider |
| `minerai_argentite` | 1 | 0 | REFAIT, a valider |
| `minerai_glacium` | 1 | 0 | REFAIT, a valider |
| `minerai_neantite` | 1 | 0 | REFAIT, a valider |
| `minerai_verdite` | 1 | 0 | REFAIT, a valider |
| `or_ouvrage` | 1 | 0 | REFAIT, a valider |
| `or_ouvrage_slab` | 3 | 0 | REFAIT, a valider |
| `or_ouvrage_stairs` | 3 | 0 | REFAIT, a valider |
| `pierre_azur` | 1 | 0 | REFAIT, a valider |
| `pierre_azur_slab` | 3 | 0 | REFAIT, a valider |
| `pierre_azur_stairs` | 3 | 0 | REFAIT, a valider |
| `pierre_solaire` | 1 | 0 | REFAIT, a valider |
| `pierre_solaire_slab` | 3 | 0 | REFAIT, a valider |
| `pierre_solaire_stairs` | 3 | 0 | REFAIT, a valider |
| `roche_celeste` | 1 | 0 | REFAIT, a valider |
| `roche_celeste_slab` | 3 | 0 | REFAIT, a valider |
| `roche_celeste_stairs` | 3 | 0 | REFAIT, a valider |
| `tuile_azur` | 1 | 0 | REFAIT, a valider |
| `tuile_azur_slab` | 3 | 0 | REFAIT, a valider |
| `tuile_azur_stairs` | 3 | 0 | REFAIT, a valider |
| `tuile_ecarlate` | 1 | 0 | REFAIT, a valider |
| `tuile_ecarlate_slab` | 3 | 0 | REFAIT, a valider |
| `tuile_ecarlate_stairs` | 3 | 0 | REFAIT, a valider |
| `tuile_givre` | 1 | 0 | REFAIT, a valider |
| `tuile_givre_slab` | 3 | 0 | REFAIT, a valider |
| `tuile_givre_stairs` | 3 | 0 | REFAIT, a valider |
| `tuile_pourpre` | 1 | 0 | REFAIT, a valider |
| `tuile_pourpre_slab` | 3 | 0 | REFAIT, a valider |
| `tuile_pourpre_stairs` | 3 | 0 | REFAIT, a valider |
| `tuile_sylve` | 1 | 0 | REFAIT, a valider |
| `tuile_sylve_slab` | 3 | 0 | REFAIT, a valider |
| `tuile_sylve_stairs` | 3 | 0 | REFAIT, a valider |
| `veine_epuisee` | 1 | 0 | REFAIT, a valider |

## Autres blocs (7)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `bloc_lumiere` | 1 | 0 | particule seule, rien a refaire |
| `camp_aeloria` | 1 | 0 | particule seule, rien a refaire |
| `coffre_donjon` | 1 | 0 | particule seule, rien a refaire |
| `decor_part` | 1 | 0 | particule seule, rien a refaire |
| `drapeau_aeloria` | 1 | 0 | particule seule, rien a refaire |
| `relais_lumiere` | 1 | 0 | particule seule, rien a refaire |
| `structure_part` | 1 | 0 | particule seule, rien a refaire |

## Mobs de zones et importes (38)

| Id | Cubes avant | Cubes apres | Os | Texture avant | Texture apres | Lueurs | Clips | Etat |
|---|---:|---:|---:|---|---|---:|---|---|
| `mousseux` | 278 | 60 | 11 | 256x256 | 256x256 | 2 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `sanglier_ronce` | 399 | 89 | 16 | 256x256 | 256x256 | 2 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `archer_sylvestre` | 275 | 54 | 12 | 256x256 | 128x128 | 2 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `veilleur_champignon` | 166 | 56 | 12 | 256x256 | 256x256 | 2 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `colosse_racine` | 690 | 94 | 14 | 256x256 | 512x512 | 4 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `crapaud_venimeux` | 301 | 51 | 12 | 256x256 | 256x256 | 18 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `limace_acide` | 114 | 27 | 9 | 256x256 | 256x256 | 4 | idle 3s, slither 1.5s, attack 1.2s, death 1s | REFAIT, a valider |
| `luciole_geante` | 163 | 32 | 14 | 256x256 | 128x128 | 4 | idle 3s, fly 1.5s, attack 1.2s, death 1s | REFAIT, a valider |
| `sorcier_roseaux` | 290 | 49 | 13 | 256x256 | 256x256 | 4 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `hydre_marais` | 705 | 104 | 28 | 256x256 | 512x512 | 6 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `osselet` | 111 | 53 | 12 | 256x256 | 128x128 | 2 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `araignee_os` | 94 | 60 | 21 | 256x256 | 128x128 | 3 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `garde_funeraire` | 201 | 81 | 13 | 256x256 | 256x256 | 3 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `spectre_enchaine` | 240 | 66 | 8 | 256x256 | 256x256 | 3 | idle 3s, fly 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `roi_cryptes` | 351 | 107 | 14 | 256x256 | 512x512 | 7 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `renard_givre` | 351 | 47 | 17 | 256x256 | 128x128 | 2 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `scarabee_polaire` | 147 | 45 | 17 | 256x256 | 256x256 | 4 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `harpie_neiges` | 275 | 46 | 9 | 256x256 | 128x128 | 12 | idle 3s, fly 1.5s, attack 1.2s, death 1s | REFAIT, a valider |
| `golem_iceberg` | 316 | 63 | 11 | 256x256 | 512x512 | 4 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `wyrm_boreal` | 387 | 71 | 23 | 256x256 | 512x512 | 2 | idle 3s, slither 1.5s, attack 1.2s, death 1s | REFAIT, a valider |
| `scorpion_cuivre` | 219 | 61 | 32 | 256x256 | 256x256 | 4 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `momie_dunes` | 127 | 33 | 11 | 256x256 | 128x128 | 2 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `hyene_cendree` | 270 | 63 | 15 | 256x256 | 256x256 | 34 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `djinn_braises` | 217 | 42 | 10 | 256x256 | 256x256 | 17 | idle 3s, fly 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `tortue_volcanique` | 386 | 52 | 13 | 256x256 | 256x256 | 24 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `mite_astrale` | 328 | 21 | 14 | 256x256 | 128x128 | 4 | idle 3s, fly 1.5s, attack 1.2s, death 1s | REFAIT, a valider |
| `rodeur_vide` | 350 | 63 | 17 | 256x256 | 256x256 | 16 | idle 3s, walk 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `oeil_chaos` | 179 | 50 | 7 | 256x256 | 256x256 | 15 | idle 3s, fly 1.5s, attack 1.2s, death 1s | REFAIT, a valider |
| `chevalier_fracture` | 245 | 65 | 13 | 256x256 | 256x256 | 16 | idle 3s, fly 1.2s, attack 1.2s, death 1s | REFAIT, a valider |
| `devoreur_etoiles` | 559 | 105 | 37 | 256x256 | 1024x1024 | 8 | idle 3s, fly 1.5s, attack 1.2s, death 1s | REFAIT, a valider |
| `fernling` | 40 | 22 | 13 | 64x64 | 128x128 | 2 | idle 4s, walk 0.8s, attack 1.8s, death 1s | REFAIT, a valider |
| `champion_gobelin` | 41 | 39 | 15 | 128x128 | 256x256 | 2 | idle 2.5s, walk 1s, attack 1s, death 1s | REFAIT, a valider |
| `voyageur_squelette` | 22 | 49 | 13 | 128x128 | 128x128 | 2 | idle 2.5s, walk 1s, attack 1.25s, death 1s | REFAIT, a valider |
| `sentinelle_hibou` | 50 | 38 | 11 | 128x128 | 256x256 | 3 | idle 4s, walk 1.7s, attack 1.4s, death 1s | REFAIT, a valider |
| `minotaure` | 29 | 54 | 15 | 256x256 | 256x256 | 3 | idle 2s, walk 1.2s, attack 0.75s, death 1s | REFAIT, a valider |
| `morghul` | 868 | 108 | 23 | 128x1024 | 512x512 | 16 | idle 4s, walk 1.5s, attack 3s, death 1s | REFAIT, a valider |
| `gardien_citadelle` | 1328 | 105 | 37 | 1254x2765 | 512x512 | 3 | idle 4s, walk 1.8s, attack 1.2s, death 1s | REFAIT, a valider |
| `seigneur_squelette` | 286 | 84 | 12 | 256x1024 | 256x256 | 25 | idle 3s, walk 2s, attack 0.96s, death 1s | REFAIT, a valider |

## Bestiaire classique (11)

| Id | Cubes avant | Cubes apres | Os | Texture avant | Texture apres | Lueurs | Clips | Etat |
|---|---:|---:|---:|---|---|---:|---|---|
| `gobelin` | 55 | 27 | 15 | 1254x1254 | 128x128 | 2 | idle 3s, walk 0.9s, attack 1s, death 1s, cast 2.4s | REFAIT, a valider |
| `orc_guerrier` | 55 | 38 | 13 | 1254x1254 | 128x128 | 2 | idle 3s, walk 0.9s, attack 1s, death 1s, cast 2.4s | REFAIT, a valider |
| `bandit_arbaletrier` | 51 | 35 | 12 | 1254x1254 | 128x128 | 2 | idle 3s, walk 0.9s, attack 1s, death 1s, cast 2.4s | REFAIT, a valider |
| `squelette_maudit` | 53 | 47 | 13 | 1254x1254 | 128x128 | 3 | idle 3s, walk 0.9s, attack 1s, death 1s, cast 2.4s | REFAIT, a valider |
| `zombie_des_cryptes` | 49 | 28 | 12 | 1254x1254 | 128x128 | 2 | idle 3s, walk 0.9s, attack 1s, death 1s, cast 2.4s | REFAIT, a valider |
| `chevalier_neant` | 60 | 36 | 12 | 1254x1254 | 128x128 | 2 | idle 3s, walk 0.9s, attack 1s, death 1s, cast 2.4s | REFAIT, a valider |
| `spectre_givre` | 56 | 32 | 11 | 1254x1254 | 128x128 | 11 | idle 3s, walk 0.9s, attack 1s, death 1s, cast 2.4s | REFAIT, a valider |
| `elementaire_feu` | 46 | 26 | 11 | 1254x1254 | 128x128 | 14 | idle 3s, walk 0.9s, attack 1s, cast 2.4s, death 1s | REFAIT, a valider |
| `golem_cristal` | 45 | 36 | 11 | 1254x1254 | 256x256 | 15 | idle 3s, walk 0.9s, attack 1s, death 1s, cast 2.4s | REFAIT, a valider |
| `loup_sombre` | 58 | 29 | 16 | 1254x1254 | 128x128 | 2 | idle 3s, walk 0.9s, attack 1s, death 1s, cast 2.4s | REFAIT, a valider |
| `araignee_venimeuse` | 67 | 27 | 21 | 1254x1254 | 64x64 | 5 | idle 3s, walk 0.9s, attack 1s, death 1s, cast 2.4s | REFAIT, a valider |

## Boss (5)

Format Modded Entity, box UV, classes Java `<Boss>Model` et `<Boss>Anims`.

| Id | Etat |
|---|---|
| `roi_gobelin` | REFAIT, a valider |
| `liche_ancienne` | REFAIT, a valider |
| `ignis` | REFAIT, a valider |
| `titan_glace` | REFAIT, a valider |
| `avatar_neant` | REFAIT, a valider |

## Dossiers non traites a ce jour

PNJ et divers, hors de la demande (montures, blocs, mobs, boss) : `bbmodels/guides` (15), `bbmodels/yggdrasil` (14), `bbmodels/chests` (2), `bbmodels/cosmetics` (11), `art/familiers_blockbench`.

## Totaux

- Montures : 22
- Blocs 3D en ImportedModel : 123 (13048 cubes rendus par bloc-entite)
- Autres blocs (blockstates) : 105
- Total a suivre : 250
