# Inventaire des montures et blocs

Copie du depot : commit `892a216`. Mesures automatiques (`outils/inventaire.py`).

**Legende.** Densite : texels par unite de modele (mediane, min-max). « Scintille » : paires de faces coplanaires qui se recouvrent et restent visibles. « Trous » : faces sans texture mais exposees.

## Montures (22)

| Monture | Id | Rarete | Sieges | Hauteur selle attendue | Cubes | Os | Texture | Densite | Scintille | Trous | Animations | Etat |
|---|---|---|---|---|---:|---:|---|---|---:|---:|---|---|
| Sanglier de guerre | `sanglier` | PEU_COMMUN | 1 | 21.16 u | 89 | 7 | 512x512 | 3.2 (1.8-84.0) | 5 | 0 | idle 3s, walk 0.9s | PILOTE LIVRE |
| Loup de givre | `loup_givre` | RARE | 1 | 23.24 u | 92 | 7 | 512x512 | 3.1 (1.0-55.6) | 0 | 0 | idle 3s, walk 0.75s | PILOTE LIVRE |
| Cerf sylvestre | `cerf_sylvestre` | RARE | 1 | 29.16 u | 97 | 7 | 512x512 | 3.1 (1.2-55.6) | 2 | 0 | idle 3s, walk 0.85s | a refaire |
| Tortue de cristal | `tortue_cristal` | RARE | 1 | 20.20 u | 75 | 7 | 512x512 | 3.3 (1.5-94.4) | 6 | 0 | idle 4s, walk 1.4s | a refaire |
| Chèvre céleste | `chevre_celeste` | RARE | 1 | 24.20 u | 83 | 7 | 512x512 | 3.3 (2.2-61.1) | 0 | 0 | idle 3s, walk 0.75s | a refaire |
| Lézard de lave | `lezard_lave` | EPIQUE | 1 | 16.20 u | 94 | 7 | 512x512 | 3.1 (0.9-66.7) | 2 | 0 | idle 3s, walk 0.7s | a refaire |
| Raptor des sables | `raptor_sables` | EPIQUE | 1 | 23.24 u | 88 | 7 | 512x512 | 3.1 (0.9-61.1) | 5 | 0 | idle 3s, walk 0.6s | a refaire |
| Ours runique | `ours_runique` | EPIQUE | 1 | 24.20 u | 82 | 7 | 512x512 | 3.3 (2.2-94.4) | 0 | 0 | idle 3s, walk 1s | a refaire |
| Scorpion des dunes | `scorpion_dunes` | EPIQUE | 1 | 15.40 u | 78 | 12 | 512x512 | 3.3 (2.5-168.0) | 0 | 0 | idle 3s, walk 0.6s | a refaire |
| Félin du vide | `felin_vide` | LEGENDAIRE | 1 | 22.28 u | 86 | 7 | 512x512 | 3.2 (1.6-61.1) | 0 | 0 | idle 3s, walk 0.65s | a refaire |
| Araignée des cavernes | `araignee_cavernes` | LEGENDAIRE | 1 | 20.20 u | 90 | 10 | 512x512 | 3.2 (2.5-166.7) | 68 | 0 | idle 3s, walk 0.5s | a refaire |
| Hippogriffe | `hippogriffe` | MYTHIQUE | 1 | 26.60 u | 106 | 9 | 512x512 | 6.7 (2.2-61.1) | 16 | 0 | idle 3s, walk 0.8s, fly 0.7s | a refaire |
| Chimère des arènes | `chimere_arene` | MYTHIQUE | 1 | 24.20 u | 72 | 7 | 512x512 | 3.0 (2.2-4.0) | 4 | 0 | idle 3s, walk 0.75s | a refaire |
| Taureau de guerre | `taureau_guerre` | PEU_COMMUN | 2 | 28.04 u | 53 (local non pousse : 528 cubes, 1024 px) | 7 | 512x512 | 3.0 (2.5-3.3) | 10 | 0 | idle 3s, walk 0.8s | a refaire |
| Lion doré | `lion_dore` | RARE | 2 | 27.08 u | 56 (local non pousse : 738 cubes, 1024 px) | 7 | 512x512 | 3.0 (2.5-3.3) | 12 | 0 | idle 3s, walk 0.7s | a refaire |
| Rhinocéros cuirassé | `rhinoceros_cuirasse` | RARE | 3 | 27.08 u | 56 (local non pousse : 573 cubes, 1024 px) | 7 | 512x512 | 2.4 (1.7-3.3) | 11 | 0 | idle 3s, walk 0.95s | a refaire |
| Bison géant | `bison_geant` | EPIQUE | 3 | 32.60 u | 52 (local non pousse : 726 cubes, 1024 px) | 7 | 512x512 | 2.4 (2.0-3.3) | 10 | 0 | idle 3s, walk 0.9s | a refaire |
| Mammouth laineux | `mammouth` | EPIQUE | 4 | 40.04 u | 61 (local non pousse : 960 cubes, 1024 px) | 7 | 512x512 | 2.0 (1.7-3.3) | 10 | 0 | idle 3s, walk 1s | a refaire |
| Éléphant de guerre | `elephant_guerre` | LEGENDAIRE | 4 | 42.12 u | 73 (local non pousse : 938 cubes, 1024 px) | 7 | 512x512 | 2.0 (1.7-3.3) | 11 | 0 | idle 3s, walk 1s | a refaire |
| Tortue forteresse | `tortue_forteresse` | LEGENDAIRE | 5 | 28.04 u | 51 (local non pousse : 545 cubes, 1024 px) | 7 | 512x512 | 2.0 (1.7-3.3) | 12 | 0 | idle 3s, walk 1.1s | a refaire |
| Drake de caravane | `drake_caravane` | MYTHIQUE | 5 | 33.00 u | 68 (local non pousse : 706 cubes, 1024 px) | 7 | 512x512 | 2.0 (1.7-3.3) | 10 | 0 | idle 3s, walk 0.85s | a refaire |
| Drake ascendant | `drake_ascendant` | MYTHIQUE | 1 | 26.60 u | 106 | 9 | 512x512 **PNG ABSENT** | 6.7 (2.2-61.1) | 16 | 0 | idle 3s, walk 0.8s, fly 0.7s | a refaire |

Geometries strictement identiques : [['hippogriffe', 'drake_ascendant']].

## Blocs 3D : decor anime (2)

| Id | Cubes | Emprise (blocs) | Hauteur | Texture | Densite | Hors grille | Scintille | Trous | Etat |
|---|---:|---|---:|---|---|---:|---:|---:|---|
| `camp` | 60 | 5.6 x 4.0 | 1.4 | 64x352 | 1.0 (0.2-2.8) | 70% | 22 | 0 | a refaire |
| `drapeau` | 7 | 0.1 x 1.2 | 1.7 | 64x352 | 1.0 (1.0-4.0) | 0% | 0 | 0 | a refaire |

## Blocs 3D : citadelle (deco_) (30)

| Id | Cubes | Emprise (blocs) | Hauteur | Texture | Densite | Hors grille | Scintille | Trous | Etat |
|---|---:|---|---:|---|---|---:|---:|---:|---|
| `deco_arche` | 71 | 8.5 x 1.8 | 12.8 | 2048x1792 | 5.3 (4.9-5.6) | 89% | 28 | 0 | a refaire |
| `deco_balise_runique` | 39 | 2.2 x 2.0 | 6.2 | 2048x1792 | 5.3 (5.0-5.5) | 43% | 1 | 0 | a refaire |
| `deco_banc` | 32 | 3.0 x 1.1 | 2.9 | 2048x1792 | 5.3 (5.0-6.2) | 66% | 74 | 0 | a refaire |
| `deco_bassin` | 29 | 5.0 x 5.0 | 3.4 | 2048x1792 | 5.3 (5.2-5.5) | 18% | 4 | 0 | a refaire |
| `deco_bibliotheque` | 142 | 4.2 x 1.4 | 7.8 | 2048x1792 | 5.3 (5.0-5.8) | 43% | 66 | 0 | a refaire |
| `deco_brasero` | 37 | 2.4 x 2.4 | 4.4 | 2048x1792 | 5.3 (5.1-5.6) | 54% | 1 | 0 | a refaire |
| `deco_caisses_empilees` | 87 | 3.0 x 3.1 | 2.6 | 2048x1792 | 5.3 (5.0-6.3) | 69% | 114 | 0 | a refaire |
| `deco_chandelier` | 72 | 1.6 x 1.7 | 4.8 | 2048x1792 | 5.4 (5.0-5.6) | 59% | 30 | 0 | a refaire |
| `deco_colonne_ornee` | 44 | 1.5 x 1.5 | 6.2 | 2048x1792 | 5.3 (5.0-5.5) | 58% | 0 | 0 | a refaire |
| `deco_cristal_flottant` | 17 | 2.3 x 2.3 | 5.6 | 2048x1792 | 5.3 (5.0-5.6) | 20% | 1 | 0 | a refaire |
| `deco_enclume_geante` | 40 | 4.0 x 1.5 | 2.9 | 2048x1792 | 5.3 (5.0-6.2) | 67% | 97 | 0 | a refaire |
| `deco_etal_marche` | 69 | 4.5 x 2.6 | 5.0 | 2048x1792 | 5.3 (5.0-6.2) | 43% | 53 | 0 | a refaire |
| `deco_etendard` | 29 | 2.4 x 0.6 | 7.5 | 2048x1792 | 5.3 (5.2-5.6) | 52% | 1 | 0 | a refaire |
| `deco_fontaine` | 66 | 4.0 x 4.0 | 5.1 | 2048x1792 | 5.3 (5.0-5.6) | 52% | 9 | 0 | a refaire |
| `deco_gargouille` | 41 | 4.2 x 1.9 | 5.2 | 2048x1792 | 5.3 (5.0-5.7) | 55% | 32 | 0 | a refaire |
| `deco_globe_arcane` | 61 | 2.9 x 2.9 | 3.9 | 2048x1792 | 5.4 (5.0-5.5) | 83% | 37 | 0 | a refaire |
| `deco_grille_forgee` | 81 | 6.3 x 0.9 | 10.4 | 2048x1792 | 5.3 (5.0-5.8) | 85% | 0 | 0 | a refaire |
| `deco_horloge` | 57 | 2.0 x 2.7 | 9.9 | 2048x1792 | 5.3 (5.0-5.8) | 73% | 18 | 0 | a refaire |
| `deco_lampadaire` | 40 | 1.1 x 1.1 | 6.2 | 2048x1792 | 5.3 (5.0-5.7) | 65% | 0 | 0 | a refaire |
| `deco_lanterne_potence` | 23 | 2.6 x 0.8 | 5.1 | 2048x1792 | 5.3 (5.0-5.6) | 46% | 3 | 0 | a refaire |
| `deco_obelisque` | 24 | 2.2 x 2.2 | 8.4 | 2048x1792 | 5.3 (5.1-5.6) | 61% | 0 | 0 | a refaire |
| `deco_puits` | 67 | 2.5 x 2.5 | 7.0 | 2048x1792 | 5.3 (5.0-5.5) | 67% | 68 | 0 | PILOTE LIVRE |
| `deco_ratelier_armes` | 40 | 3.9 x 1.2 | 5.6 | 2048x1792 | 5.3 (4.6-5.8) | 43% | 2 | 0 | a refaire |
| `deco_sablier_geant` | 80 | 2.0 x 2.0 | 6.0 | 2048x1792 | 5.3 (5.1-5.6) | 70% | 4 | 0 | a refaire |
| `deco_statue_guerrier` | 32 | 2.1 x 1.7 | 5.9 | 2048x1792 | 5.3 (5.0-6.3) | 59% | 28 | 0 | a refaire |
| `deco_statue_mage` | 45 | 1.9 x 1.7 | 6.9 | 2048x1792 | 5.3 (5.0-6.3) | 63% | 40 | 0 | a refaire |
| `deco_table_banquet` | 24 | 4.6 x 1.6 | 2.5 | 2048x1792 | 5.3 (5.0-6.0) | 42% | 4 | 0 | a refaire |
| `deco_tonneau_geant` | 75 | 2.5 x 2.5 | 3.0 | 2048x1792 | 5.3 (5.0-6.2) | 69% | 62 | 0 | a refaire |
| `deco_trone` | 43 | 2.7 x 2.7 | 8.1 | 2048x1792 | 5.3 (5.0-6.3) | 40% | 26 | 0 | a refaire |
| `deco_vase_geant` | 56 | 1.9 x 1.9 | 4.1 | 2048x1792 | 5.3 (5.0-5.8) | 57% | 66 | 0 | a refaire |

## Blocs 3D : cathedrale (deco_cat_) (40)

| Id | Cubes | Emprise (blocs) | Hauteur | Texture | Densite | Hors grille | Scintille | Trous | Etat |
|---|---:|---|---:|---|---|---:|---:|---:|---|
| `deco_cat_ange` | 81 | 4.0 x 1.9 | 7.0 | 2048x1792 | 5.3 (5.0-5.6) | 70% | 3 | 0 | a refaire |
| `deco_cat_applique` | 61 | 2.9 x 1.3 | 4.4 | 2048x1792 | 5.3 (5.0-6.3) | 72% | 9 | 0 | a refaire |
| `deco_cat_arc_brise` | 352 | 6.9 x 1.9 | 12.8 | 2048x1792 | 5.3 (5.1-5.8) | 92% | 12 | 0 | a refaire |
| `deco_cat_arcade_cloitre` | 337 | 10.5 x 1.4 | 11.8 | 2048x1792 | 5.3 (5.1-5.6) | 92% | 12 | 0 | a refaire |
| `deco_cat_autel_majeur` | 298 | 5.5 x 2.8 | 7.2 | 2048x1792 | 5.3 (4.8-6.7) | 70% | 33 | 0 | a refaire |
| `deco_cat_baldaquin` | 693 | 5.0 x 5.0 | 12.1 | 2048x1792 | 5.3 (4.8-6.3) | 57% | 644 | 0 | a refaire |
| `deco_cat_balustrade_choeur` | 392 | 9.7 x 1.0 | 7.1 | 2048x1792 | 5.4 (4.9-5.6) | 93% | 3 | 0 | a refaire |
| `deco_cat_banc_nef` | 50 | 6.0 x 1.9 | 4.8 | 2048x1792 | 5.3 (5.0-5.8) | 30% | 2 | 0 | a refaire |
| `deco_cat_benitier` | 60 | 1.8 x 1.8 | 2.8 | 2048x1792 | 5.3 (5.0-5.6) | 62% | 1 | 0 | a refaire |
| `deco_cat_candelabre` | 205 | 4.1 x 1.6 | 6.6 | 2048x1792 | 5.4 (4.3-6.7) | 78% | 259 | 0 | a refaire |
| `deco_cat_candelabre_geant` | 240 | 3.1 x 2.8 | 9.4 | 2048x1792 | 5.3 (4.3-6.7) | 67% | 49 | 0 | a refaire |
| `deco_cat_chaire` | 83 | 2.5 x 2.5 | 9.4 | 2048x1792 | 5.3 (5.2-5.5) | 54% | 3 | 0 | a refaire |
| `deco_cat_cierge` | 56 | 1.6 x 1.6 | 7.5 | 2048x1792 | 5.3 (5.0-5.6) | 57% | 6 | 0 | a refaire |
| `deco_cat_clocheton` | 156 | 1.7 x 1.7 | 8.8 | 2048x1792 | 5.3 (4.7-6.0) | 81% | 20 | 0 | a refaire |
| `deco_cat_colonne_torsadee` | 125 | 1.6 x 1.6 | 9.1 | 2048x1792 | 5.2 (5.2-5.5) | 78% | 0 | 0 | a refaire |
| `deco_cat_confessionnal` | 73 | 4.0 x 2.6 | 7.5 | 2048x1792 | 5.3 (4.8-6.0) | 47% | 12 | 0 | a refaire |
| `deco_cat_contrefort` | 57 | 7.1 x 1.7 | 12.6 | 2048x1792 | 5.3 (5.0-5.6) | 74% | 36 | 0 | a refaire |
| `deco_cat_croix` | 29 | 4.9 x 2.8 | 10.0 | 2048x1792 | 5.3 (5.0-5.5) | 25% | 0 | 0 | a refaire |
| `deco_cat_encensoir` | 86 | 2.4 x 1.4 | 6.1 | 2048x1792 | 5.3 (5.0-6.0) | 63% | 13 | 0 | a refaire |
| `deco_cat_escalier_helicoidal` | 86 | 3.5 x 3.4 | 9.0 | 2048x1792 | 5.3 (5.0-5.6) | 62% | 8 | 0 | a refaire |
| `deco_cat_fleche` | 96 | 2.5 x 2.5 | 11.5 | 2048x1792 | 5.3 (5.0-5.8) | 56% | 6 | 0 | a refaire |
| `deco_cat_fonts_baptismaux` | 101 | 2.8 x 2.8 | 8.4 | 2048x1792 | 5.3 (5.2-5.6) | 57% | 3 | 0 | a refaire |
| `deco_cat_gargouille` | 1220 | 10.8 x 6.8 | 12.4 | 2048x1792 | 5.3 (4.8-5.8) | 55% | 50 | 0 | a refaire |
| `deco_cat_gisant` | 124 | 5.5 x 2.0 | 3.3 | 2048x1792 | 5.2 (5.0-5.8) | 66% | 17 | 0 | a refaire |
| `deco_cat_horloge` | 287 | 3.7 x 2.7 | 11.0 | 2048x1792 | 5.3 (5.0-6.0) | 69% | 1 | 0 | a refaire |
| `deco_cat_lutrin_aigle` | 60 | 3.1 x 2.0 | 6.0 | 2048x1792 | 5.3 (5.0-5.6) | 54% | 0 | 0 | a refaire |
| `deco_cat_orgue` | 530 | 6.4 x 2.0 | 10.0 | 2048x1792 | 5.3 (5.0-6.2) | 67% | 43 | 0 | a refaire |
| `deco_cat_pilier_faisceau` | 123 | 2.7 x 2.7 | 9.4 | 2048x1792 | 5.3 (5.0-5.5) | 69% | 41 | 0 | a refaire |
| `deco_cat_porche` | 535 | 7.5 x 4.4 | 12.8 | 2048x1792 | 5.3 (5.1-5.6) | 89% | 44 | 0 | a refaire |
| `deco_cat_portail` | 443 | 8.1 x 2.0 | 12.8 | 2048x1792 | 5.3 (4.9-5.8) | 97% | 28 | 0 | a refaire |
| `deco_cat_puits_lumiere` | 149 | 2.8 x 2.8 | 8.4 | 2048x1792 | 5.3 (5.2-5.6) | 48% | 12 | 0 | a refaire |
| `deco_cat_reliquaire` | 82 | 3.3 x 2.8 | 5.1 | 2048x1792 | 5.3 (5.0-6.0) | 54% | 16 | 0 | a refaire |
| `deco_cat_reliquaire_cristal` | 64 | 1.7 x 1.7 | 5.8 | 2048x1792 | 5.3 (5.0-5.6) | 58% | 2 | 0 | a refaire |
| `deco_cat_rosace` | 226 | 5.8 x 1.0 | 7.9 | 2048x1792 | 5.3 (4.8-6.0) | 65% | 2 | 0 | a refaire |
| `deco_cat_stalle` | 193 | 3.5 x 1.8 | 7.9 | 2048x1792 | 5.3 (4.8-6.0) | 75% | 2 | 0 | a refaire |
| `deco_cat_statue_saint` | 158 | 2.8 x 1.2 | 6.7 | 2048x1792 | 5.4 (5.0-6.0) | 56% | 6 | 0 | a refaire |
| `deco_cat_tapisserie` | 66 | 4.9 x 0.4 | 8.0 | 2048x1792 | 5.3 (5.0-5.9) | 58% | 6 | 0 | a refaire |
| `deco_cat_triptyque` | 655 | 7.6 x 0.8 | 9.2 | 2048x1792 | 5.3 (4.6-10.0) | 61% | 34 | 0 | a refaire |
| `deco_cat_trone_eveque` | 212 | 3.8 x 2.5 | 10.6 | 2048x1792 | 5.3 (5.0-6.7) | 68% | 10 | 0 | a refaire |
| `deco_cat_vitrail` | 425 | 4.9 x 1.1 | 10.4 | 2048x1792 | 5.3 (4.8-6.0) | 56% | 16 | 0 | a refaire |

## Blocs 3D : citadelle Blockbench (decob_) (51)

| Id | Cubes | Emprise (blocs) | Hauteur | Texture | Densite | Hors grille | Scintille | Trous | Etat |
|---|---:|---|---:|---|---|---:|---:|---:|---|
| `decob_arbre_cristal` | 12 | 4.0 x 3.2 | 4.8 | 2048x1792 | 5.3 (5.3-5.5) | 76% | 0 | 0 | a refaire |
| `decob_arbre_nain` | 7 | 3.6 x 2.4 | 4.0 | 2048x1792 | 5.3 (5.2-5.4) | 83% | 0 | 0 | a refaire |
| `decob_armoire_geante` | 66 | 3.3 x 2.3 | 5.0 | 2048x1792 | 5.3 (5.0-6.2) | 66% | 54 | 0 | a refaire |
| `decob_armure_exposition` | 35 | 3.3 x 2.0 | 6.0 | 2048x1792 | 5.3 (5.0-6.2) | 84% | 72 | 0 | a refaire |
| `decob_autel_lumiere` | 10 | 3.2 x 3.2 | 5.6 | 2048x1792 | 5.3 (5.3-5.6) | 63% | 0 | 0 | a refaire |
| `decob_autel_runique` | 31 | 4.4 x 4.4 | 5.0 | 2048x1792 | 5.3 (5.0-6.2) | 72% | 48 | 0 | a refaire |
| `decob_baliste` | 12 | 4.8 x 4.6 | 2.8 | 2048x1792 | 5.3 (5.0-5.5) | 81% | 0 | 0 | a refaire |
| `decob_balustrade` | 60 | 8.9 x 2.9 | 8.3 | 2048x1792 | 5.3 (4.9-5.6) | 79% | 58 | 0 | a refaire |
| `decob_brasero_double` | 27 | 4.8 x 1.6 | 5.0 | 2048x1792 | 5.3 (5.0-5.8) | 73% | 48 | 0 | a refaire |
| `decob_buisson_fleuri` | 18 | 4.0 x 4.0 | 3.6 | 2048x1792 | 5.3 (5.3-5.4) | 62% | 0 | 0 | a refaire |
| `decob_bureau_scribe` | 47 | 4.1 x 2.1 | 4.0 | 2048x1792 | 5.3 (5.0-6.2) | 79% | 52 | 0 | a refaire |
| `decob_canon_decoratif` | 50 | 4.2 x 4.0 | 2.6 | 2048x1792 | 5.3 (5.0-6.3) | 83% | 98 | 0 | a refaire |
| `decob_cercle_invocation` | 19 | 4.4 x 4.4 | 0.4 | 2048x1792 | 5.3 (5.3-5.6) | 67% | 10 | 0 | a refaire |
| `decob_chariot_marchand` | 71 | 5.7 x 2.5 | 3.0 | 2048x1792 | 5.3 (5.0-6.2) | 84% | 126 | 0 | a refaire |
| `decob_cheminee` | 76 | 4.4 x 1.7 | 6.0 | 2048x1792 | 5.3 (5.0-6.2) | 83% | 122 | 0 | a refaire |
| `decob_coffre_tresor` | 33 | 3.4 x 2.3 | 3.6 | 2048x1792 | 5.3 (5.0-6.2) | 78% | 41 | 0 | a refaire |
| `decob_colonne_lumineuse` | 36 | 2.0 x 2.0 | 5.8 | 2048x1792 | 5.3 (5.0-5.8) | 77% | 28 | 0 | a refaire |
| `decob_comptoir_taverne` | 56 | 5.0 x 1.9 | 4.1 | 2048x1792 | 5.3 (5.0-6.2) | 79% | 82 | 0 | a refaire |
| `decob_creneau` | 124 | 9.0 x 4.1 | 6.1 | 2048x1792 | 5.3 (4.9-5.6) | 82% | 191 | 0 | a refaire |
| `decob_dais` | 17 | 4.8 x 4.8 | 1.9 | 2048x1792 | 5.3 (5.0-5.4) | 65% | 0 | 0 | a refaire |
| `decob_dragon_pierre` | 77 | 5.2 x 4.4 | 5.4 | 2048x1792 | 5.3 (5.0-6.2) | 79% | 112 | 0 | a refaire |
| `decob_escalier_monumental` | 61 | 8.2 x 8.2 | 6.1 | 2048x1792 | 5.3 (5.3-5.5) | 53% | 4 | 0 | a refaire |
| `decob_fauteuil_royal` | 34 | 2.5 x 2.0 | 5.0 | 2048x1792 | 5.3 (5.0-6.2) | 66% | 56 | 0 | a refaire |
| `decob_fenetre_gothique` | 35 | 7.1 x 2.0 | 9.9 | 2048x1792 | 5.3 (5.1-5.5) | 79% | 14 | 0 | a refaire |
| `decob_fontaine_murale` | 34 | 3.6 x 2.8 | 5.0 | 2048x1792 | 5.3 (5.0-5.8) | 72% | 31 | 0 | a refaire |
| `decob_geode` | 64 | 4.2 x 3.8 | 4.8 | 2048x1792 | 5.3 (5.0-6.2) | 69% | 84 | 0 | a refaire |
| `decob_haie_taillee` | 13 | 8.6 x 2.2 | 6.1 | 2048x1792 | 5.3 (5.3-5.4) | 79% | 0 | 0 | a refaire |
| `decob_herse` | 28 | 6.8 x 1.4 | 8.8 | 2048x1792 | 5.3 (5.1-5.4) | 76% | 0 | 0 | a refaire |
| `decob_jardiniere_geante` | 121 | 4.2 x 1.8 | 2.8 | 2048x1792 | 5.3 (5.0-6.2) | 74% | 56 | 0 | a refaire |
| `decob_lanterne_sol` | 10 | 1.6 x 1.6 | 4.0 | 2048x1792 | 5.3 (5.3-5.4) | 87% | 0 | 0 | a refaire |
| `decob_lit_royal` | 81 | 4.4 x 5.2 | 5.1 | 2048x1792 | 5.3 (5.0-6.2) | 63% | 70 | 0 | a refaire |
| `decob_lustre_cristal` | 17 | 4.0 x 3.3 | 4.8 | 2048x1792 | 5.3 (5.3-5.6) | 74% | 0 | 0 | a refaire |
| `decob_lutrin` | 8 | 2.8 x 1.6 | 4.8 | 2048x1792 | 5.3 (5.2-5.4) | 85% | 0 | 0 | a refaire |
| `decob_mur_orne` | 110 | 9.2 x 2.3 | 8.3 | 2048x1792 | 5.3 (4.9-5.6) | 74% | 60 | 0 | a refaire |
| `decob_parterre_fleurs` | 18 | 4.8 x 4.8 | 2.3 | 2048x1792 | 5.3 (5.3-5.5) | 67% | 0 | 0 | a refaire |
| `decob_passerelle` | 62 | 8.2 x 3.1 | 6.3 | 2048x1792 | 5.3 (5.1-5.5) | 90% | 56 | 0 | a refaire |
| `decob_phare_arcane` | 68 | 3.2 x 3.2 | 6.0 | 2048x1792 | 5.3 (5.0-6.2) | 67% | 57 | 0 | a refaire |
| `decob_pilier_court` | 27 | 2.5 x 2.5 | 4.2 | 2048x1792 | 5.3 (5.0-5.6) | 83% | 28 | 0 | a refaire |
| `decob_pilier_cristal_trio` | 13 | 3.2 x 3.2 | 6.2 | 2048x1792 | 5.3 (5.3-5.4) | 60% | 0 | 0 | a refaire |
| `decob_portail_monumental` | 124 | 8.5 x 2.7 | 10.9 | 2048x1792 | 5.3 (5.1-5.5) | 81% | 86 | 0 | a refaire |
| `decob_portail_runique` | 31 | 7.5 x 2.0 | 10.2 | 2048x1792 | 5.3 (5.1-5.5) | 86% | 4 | 0 | a refaire |
| `decob_pylone_arcane` | 47 | 2.4 x 2.4 | 6.0 | 2048x1792 | 5.3 (5.0-6.2) | 82% | 72 | 0 | a refaire |
| `decob_rocher_moussu` | 95 | 4.9 x 3.8 | 4.8 | 2048x1792 | 5.3 (5.0-6.2) | 73% | 110 | 0 | a refaire |
| `decob_saule_arcane` | 18 | 4.4 x 4.4 | 4.6 | 2048x1792 | 5.3 (5.0-5.5) | 67% | 1 | 0 | a refaire |
| `decob_sphere_cristal` | 9 | 3.2 x 3.2 | 4.2 | 2048x1792 | 5.3 (5.3-5.5) | 76% | 0 | 0 | a refaire |
| `decob_stele_gravee` | 16 | 1.6 x 1.6 | 4.6 | 2048x1792 | 5.3 (5.0-5.8) | 91% | 24 | 0 | a refaire |
| `decob_tabouret_geant` | 8 | 2.0 x 2.0 | 3.5 | 2048x1792 | 5.3 (5.3-5.4) | 56% | 0 | 0 | a refaire |
| `decob_tente_commandement` | 21 | 4.4 x 4.4 | 5.6 | 2048x1792 | 5.3 (5.3-5.4) | 54% | 0 | 0 | a refaire |
| `decob_torche_geante` | 7 | 1.6 x 1.6 | 5.8 | 2048x1792 | 5.3 (5.2-5.4) | 93% | 0 | 0 | a refaire |
| `decob_totem_arcane` | 47 | 3.2 x 1.6 | 5.2 | 2048x1792 | 5.3 (5.0-5.8) | 82% | 54 | 0 | a refaire |
| `decob_tourelle` | 38 | 3.6 x 3.6 | 5.2 | 2048x1792 | 5.3 (5.0-6.2) | 75% | 30 | 0 | a refaire |

## Structures fonctionnelles animees (Modded Entity, modele Java + StructureRenderer) (8)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `autel_invocation` | 1 | 3 | a refaire |
| `banque_aeloria` | 1 | 0 | a refaire |
| `boite_aux_lettres` | 1 | 0 | a refaire |
| `forge_arcanique` | 1 | 0 | a refaire |
| `hdv_aeloria` | 1 | 0 | a refaire |
| `presse_compression` | 1 | 0 | a refaire |
| `quetes_hebdo` | 1 | 0 | a refaire |
| `teleporteur` | 1 | 4 | a refaire |

## Caisses (JSON Java + CrateRenderer) (7)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `caisse_commune_bloc` | 2 | 58 | a refaire |
| `caisse_epique_bloc` | 2 | 60 | a refaire |
| `caisse_legendaire_bloc` | 2 | 60 | a refaire |
| `caisse_mythique_bloc` | 2 | 62 | a refaire |
| `caisse_quete_bloc` | 2 | 58 | a refaire |
| `caisse_rare_bloc` | 2 | 58 | a refaire |
| `caisse_vote_bloc` | 2 | 58 | a refaire |

## Lucky Blocks (JSON Java) (6)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `lucky_block` | 1 | 0 | a refaire |
| `lucky_celeste` | 1 | 65 | a refaire |
| `lucky_givre` | 1 | 67 | a refaire |
| `lucky_infernal` | 1 | 65 | a refaire |
| `lucky_neant` | 1 | 64 | a refaire |
| `lucky_sylvestre` | 1 | 69 | a refaire |

## Classements (JSON Java + rendu) (10)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `classement_arene` | 1 | 4 | a refaire |
| `classement_boss` | 1 | 4 | a refaire |
| `classement_donjons` | 1 | 4 | a refaire |
| `classement_fortune` | 1 | 4 | a refaire |
| `classement_guilde_banque` | 1 | 4 | a refaire |
| `classement_guilde_gardiens` | 1 | 4 | a refaire |
| `classement_guilde_niveau` | 1 | 4 | a refaire |
| `classement_monstres` | 1 | 4 | a refaire |
| `classement_niveau` | 1 | 4 | a refaire |
| `classement_quetes` | 1 | 4 | a refaire |

## Accessoires de decor (JSON Java Block) (11)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `armoire_aeloria` | 1 | 10 | a refaire |
| `buche_camp` | 1 | 2 | a refaire |
| `chaise_aeloria` | 1 | 14 | a refaire |
| `feu_de_camp` | 1 | 19 | a refaire |
| `jarre_ancienne` | 1 | 3 | a refaire |
| `panneau_feuille` | 1 | 6 | a refaire |
| `panneau_quetes` | 1 | 26 | a refaire |
| `pot_decore` | 1 | 6 | a refaire |
| `potion_etoilee` | 1 | 29 | a refaire |
| `table_aeloria` | 1 | 6 | a refaire |
| `viande_deco` | 1 | 7 | a refaire |

## Cultures (croix) (5)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `culture_ambre` | 4 | 0 | a refaire |
| `culture_argent` | 4 | 0 | a refaire |
| `culture_givre` | 4 | 0 | a refaire |
| `culture_lueur` | 4 | 0 | a refaire |
| `culture_neant` | 4 | 0 | a refaire |

## Minerais et materiaux (cube, dalle, escalier) (51)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `bois_ancien` | 1 | 0 | a refaire |
| `bois_ancien_slab` | 3 | 0 | a refaire |
| `bois_ancien_stairs` | 3 | 0 | a refaire |
| `colonne_marbre` | 2 | 0 | a refaire |
| `cristal_ambre` | 1 | 0 | a refaire |
| `cristal_arcane` | 1 | 0 | a refaire |
| `cristal_azur` | 1 | 0 | a refaire |
| `dalle_runique` | 1 | 0 | a refaire |
| `fer_noirci` | 1 | 0 | a refaire |
| `fer_noirci_slab` | 3 | 0 | a refaire |
| `fer_noirci_stairs` | 3 | 0 | a refaire |
| `marbre` | 1 | 0 | a refaire |
| `marbre_dore` | 1 | 0 | a refaire |
| `marbre_poli` | 1 | 0 | a refaire |
| `marbre_poli_slab` | 3 | 0 | a refaire |
| `marbre_poli_stairs` | 3 | 0 | a refaire |
| `marbre_slab` | 3 | 0 | a refaire |
| `marbre_stairs` | 3 | 0 | a refaire |
| `minerai_ambrasite` | 1 | 0 | a refaire |
| `minerai_argentite` | 1 | 0 | a refaire |
| `minerai_glacium` | 1 | 0 | a refaire |
| `minerai_neantite` | 1 | 0 | a refaire |
| `minerai_verdite` | 1 | 0 | a refaire |
| `or_ouvrage` | 1 | 0 | a refaire |
| `or_ouvrage_slab` | 3 | 0 | a refaire |
| `or_ouvrage_stairs` | 3 | 0 | a refaire |
| `pierre_azur` | 1 | 0 | a refaire |
| `pierre_azur_slab` | 3 | 0 | a refaire |
| `pierre_azur_stairs` | 3 | 0 | a refaire |
| `pierre_solaire` | 1 | 0 | a refaire |
| `pierre_solaire_slab` | 3 | 0 | a refaire |
| `pierre_solaire_stairs` | 3 | 0 | a refaire |
| `roche_celeste` | 1 | 0 | a refaire |
| `roche_celeste_slab` | 3 | 0 | a refaire |
| `roche_celeste_stairs` | 3 | 0 | a refaire |
| `tuile_azur` | 1 | 0 | a refaire |
| `tuile_azur_slab` | 3 | 0 | a refaire |
| `tuile_azur_stairs` | 3 | 0 | a refaire |
| `tuile_ecarlate` | 1 | 0 | a refaire |
| `tuile_ecarlate_slab` | 3 | 0 | a refaire |
| `tuile_ecarlate_stairs` | 3 | 0 | a refaire |
| `tuile_givre` | 1 | 0 | a refaire |
| `tuile_givre_slab` | 3 | 0 | a refaire |
| `tuile_givre_stairs` | 3 | 0 | a refaire |
| `tuile_pourpre` | 1 | 0 | a refaire |
| `tuile_pourpre_slab` | 3 | 0 | a refaire |
| `tuile_pourpre_stairs` | 3 | 0 | a refaire |
| `tuile_sylve` | 1 | 0 | a refaire |
| `tuile_sylve_slab` | 3 | 0 | a refaire |
| `tuile_sylve_stairs` | 3 | 0 | a refaire |
| `veine_epuisee` | 1 | 0 | a refaire |

## Autres blocs (7)

| Bloc | Modeles | Elements max | Etat |
|---|---:|---:|---|
| `bloc_lumiere` | 1 | 0 | a refaire |
| `camp_aeloria` | 1 | 0 | a refaire |
| `coffre_donjon` | 1 | 0 | a refaire |
| `decor_part` | 1 | 0 | a refaire |
| `drapeau_aeloria` | 1 | 0 | a refaire |
| `relais_lumiere` | 1 | 0 | a refaire |
| `structure_part` | 1 | 0 | a refaire |

## Totaux

- Montures : 22
- Blocs 3D en ImportedModel : 123 (13048 cubes rendus par bloc-entite)
- Autres blocs (blockstates) : 105
- Total a suivre : 250
