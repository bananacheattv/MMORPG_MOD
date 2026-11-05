package com.mmorpg.registry;

import java.util.List;

/** Genere par tools/gen_decor_bb.py : 30 decors 3D de la citadelle (1 bloc de citadelle = 3 blocs Minecraft). */
public final class DecorCitadelList {
    private DecorCitadelList() {
    }

    /** id = cle du modele (bbmodels/props/<id>.json), boite de collision en pixels de bloc, niveau de lumiere. */
    public record Entry(String id, double x0, double y0, double z0, double x1, double y1, double z1, int light) {
    }

    public static final List<Entry> ALL = List.of(
            new Entry("deco_lampadaire", 5, 0, 5, 11, 32, 11, 15),
            new Entry("deco_brasero", 3, 0, 3, 13, 20, 13, 15),
            new Entry("deco_fontaine", 1, 0, 1, 15, 14, 15, 6),
            new Entry("deco_statue_guerrier", 3, 0, 3, 13, 32, 13, 0),
            new Entry("deco_statue_mage", 3, 0, 3, 13, 32, 13, 0),
            new Entry("deco_colonne_ornee", 3, 0, 3, 13, 32, 13, 0),
            new Entry("deco_arche", 0, 0, 5, 16, 32, 11, 0),
            new Entry("deco_banc", 1, 0, 4, 15, 9, 12, 0),
            new Entry("deco_etendard", 6, 0, 6, 10, 32, 10, 0),
            new Entry("deco_obelisque", 4, 0, 4, 12, 32, 12, 8),
            new Entry("deco_vase_geant", 3, 0, 3, 13, 20, 13, 0),
            new Entry("deco_lanterne_potence", 5, 0, 5, 11, 32, 11, 14),
            new Entry("deco_grille_forgee", 0, 0, 6, 16, 28, 10, 0),
            new Entry("deco_tonneau_geant", 2, 0, 2, 14, 16, 14, 0),
            new Entry("deco_caisses_empilees", 0, 0, 0, 16, 32, 16, 0),
            new Entry("deco_table_banquet", 0, 0, 2, 16, 14, 14, 0),
            new Entry("deco_trone", 2, 0, 2, 14, 32, 14, 0),
            new Entry("deco_chandelier", 4, 0, 4, 12, 28, 12, 14),
            new Entry("deco_cristal_flottant", 3, 0, 3, 13, 12, 13, 15),
            new Entry("deco_ratelier_armes", 1, 0, 5, 15, 24, 11, 0),
            new Entry("deco_enclume_geante", 2, 0, 4, 14, 16, 12, 0),
            new Entry("deco_puits", 1, 0, 1, 15, 16, 15, 0),
            new Entry("deco_horloge", 3, 0, 3, 13, 32, 13, 5),
            new Entry("deco_balise_runique", 3, 0, 3, 13, 20, 13, 15),
            new Entry("deco_etal_marche", 0, 0, 2, 16, 14, 14, 0),
            new Entry("deco_sablier_geant", 3, 0, 3, 13, 28, 13, 4),
            new Entry("deco_globe_arcane", 3, 0, 3, 13, 28, 13, 12),
            new Entry("deco_bibliotheque", 0, 0, 4, 16, 32, 12, 0),
            new Entry("deco_gargouille", 2, 0, 2, 14, 24, 14, 0),
            new Entry("deco_bassin", 0, 0, 0, 16, 10, 16, 7));
}
