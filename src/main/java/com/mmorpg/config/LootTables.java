package com.mmorpg.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Butins des caisses d'aventure et des Lucky Blocks (config/mmorpg/butins.json), modifiables en jeu
 * avec /mmorpg butins (operateurs). Chaque entree est tiree au sort selon son poids.
 */
public class LootTables {
    public static final String CRATE = "caisse_aventure";
    public static final String LUCKY = "lucky_block";
    public static final String VOTE = "caisse_vote", QUETE = "caisse_quete", RARE = "caisse_rare", EPIQUE = "caisse_epique",
            LEGENDAIRE = "caisse_legendaire", MYTHIQUE = "caisse_mythique";
    /** Toutes les tables modifiables en jeu (ordre des onglets de l'editeur). */
    public static final List<String> ALL = List.of(VOTE, QUETE, CRATE, RARE, EPIQUE, LEGENDAIRE, MYTHIQUE, LUCKY);
    public static final int MAX_ENTRIES = 48;

    public Map<String, List<Entry>> tables = new LinkedHashMap<>();

    public static class Entry {
        /** Identifiant d'objet, ou "or" pour des pieces d'or versees dans la bourse. */
        public String item = "mmorpg:piece_or";
        public int min = 1;
        public int max = 1;
        public int weight = 10;

        public Entry() {
        }

        public Entry(String item, int min, int max, int weight) {
            this.item = item;
            this.min = min;
            this.max = max;
            this.weight = weight;
        }

        public boolean isGold() {
            return "or".equals(item);
        }
    }

    public List<Entry> table(String id) {
        return tables.getOrDefault(id, List.of());
    }

    public static String label(String id) {
        return switch (id) {
            case LUCKY -> "Lucky Block";
            case VOTE -> "Vote";
            case QUETE -> "Quête";
            case CRATE -> "Commune";
            case RARE -> "Rare";
            case EPIQUE -> "Épique";
            case LEGENDAIRE -> "Légendaire";
            case MYTHIQUE -> "Mythique";
            default -> id;
        };
    }

    private static Entry e(String item, int min, int max, int weight) {
        return new Entry(item.equals("or") || item.contains(":") ? item : "mmorpg:" + item, min, max, weight);
    }

    public static LootTables defaults() {
        LootTables t = new LootTables();
        t.tables.put(CRATE, new ArrayList<>(List.of(
                e("potion_soin", 2, 4, 22), e("potion_mana", 2, 4, 18), e("pierre_amelioration", 1, 3, 16),
                e("or", 60, 200, 18), e("pierre_amelioration_sup", 1, 1, 5), e("charme_experience", 1, 1, 4),
                e("charme_chance", 1, 1, 4), e("trefle_chance", 1, 2, 6), e("nourriture_familier", 2, 4, 5),
                e("coffre_cosmetique", 1, 1, 2), e("oeuf_feu_follet", 1, 1, 2), e("oeuf_slime", 1, 1, 2), e("cle_rare", 1, 1, 3))));
        // cosmetiques (coffre cosmetique), familiers (oeufs) et montures (sceaux) ne s'obtiennent que dans les caisses et les Lucky Blocks
        t.tables.put(VOTE, new ArrayList<>(List.of(
                e("or", 50, 250, 25), e("potion_soin", 2, 5, 15), e("potion_mana", 2, 5, 12), e("pierre_amelioration", 1, 3, 14),
                e("trefle_chance", 1, 2, 8), e("cle_commune", 1, 2, 10), e("cle_rare", 1, 1, 4), e("lucky_block", 1, 1, 5),
                e("coffre_cosmetique", 1, 1, 3), e("oeuf_feu_follet", 1, 1, 2), e("oeuf_slime", 1, 1, 2))));
        t.tables.put(QUETE, new ArrayList<>(List.of(
                e("or", 150, 500, 20), e("potion_soin_majeure", 1, 3, 12), e("potion_mana_majeure", 1, 3, 10),
                e("pierre_amelioration_sup", 1, 2, 12), e("charme_experience", 1, 1, 8), e("charme_chance", 1, 1, 8),
                e("cle_rare", 1, 1, 8), e("cle_epique", 1, 1, 3), e("coffre_cosmetique", 1, 1, 5),
                e("oeuf_chouette", 1, 1, 3), e("oeuf_loup_spectral", 1, 1, 3), e("oeuf_golem", 1, 1, 3))));
        t.tables.put(RARE, new ArrayList<>(List.of(
                e("or", 200, 600, 20), e("pierre_amelioration_sup", 1, 3, 15), e("elixir_experience", 1, 2, 10),
                e("charme_chance", 1, 2, 8), e("trefle_quatre_feuilles", 1, 1, 8), e("cle_epique", 1, 1, 6),
                e("coffre_cosmetique", 1, 1, 8), e("oeuf_chouette", 1, 1, 5), e("oeuf_loup_spectral", 1, 1, 5), e("oeuf_golem", 1, 1, 5), e("monture_voyageur", 1, 1, 4))));
        t.tables.put(EPIQUE, new ArrayList<>(List.of(
                e("or", 500, 1500, 18), e("pierre_amelioration_sup", 2, 4, 14), e("fragment_divin", 1, 1, 6),
                e("trefle_dore", 1, 1, 6), e("cle_legendaire", 1, 1, 5), e("coffre_cosmetique", 1, 2, 12),
                e("oeuf_fee", 1, 1, 8), e("oeuf_loup_spectral", 1, 1, 6), e("oeuf_golem", 1, 1, 6), e("monture_voyageur", 1, 1, 6), e("monture_aube", 1, 1, 3))));
        t.tables.put(LEGENDAIRE, new ArrayList<>(List.of(
                e("or", 1500, 4000, 16), e("fragment_divin", 1, 2, 12), e("trefle_celeste", 1, 1, 6),
                e("cle_mythique", 1, 1, 4), e("coffre_cosmetique", 2, 3, 14), e("oeuf_fee", 1, 1, 8),
                e("oeuf_phenix", 1, 1, 8), e("oeuf_dragonnet", 1, 1, 2), e("monture_aube", 1, 1, 6), e("monture_ombre", 1, 1, 3))));
        t.tables.put(MYTHIQUE, new ArrayList<>(List.of(
                e("or", 5000, 12000, 14), e("fragment_divin", 2, 4, 14), e("trefle_divin", 1, 1, 8),
                e("coffre_cosmetique", 3, 5, 18), e("oeuf_phenix", 1, 1, 10), e("oeuf_dragonnet", 1, 1, 10), e("monture_ombre", 1, 1, 8))));
        t.tables.put(LUCKY, new ArrayList<>(List.of(
                e("or", 30, 300, 25), e("potion_soin_majeure", 1, 2, 12), e("potion_mana_majeure", 1, 2, 10),
                e("cle_aventure", 1, 1, 10), e("trefle_quatre_feuilles", 1, 1, 8), e("pierre_amelioration_sup", 1, 2, 8),
                e("charme_chance", 1, 1, 7), e("elixir_experience", 1, 1, 7), e("lucky_block", 1, 1, 5),
                e("oeuf_feu_follet", 1, 1, 3), e("coffre_cosmetique", 1, 1, 3), e("fragment_divin", 1, 1, 2), e("monture_voyageur", 1, 1, 1))));
        return t;
    }

    /** Corrige les valeurs invalides et complete les tables manquantes. */
    public LootTables sanitize() {
        if (tables == null) tables = new LinkedHashMap<>();
        LootTables d = defaults();
        for (String id : ALL) {
            List<Entry> list = tables.get(id);
            if (list == null) tables.put(id, d.tables.get(id));
        }
        for (List<Entry> list : tables.values()) {
            list.removeIf(x -> x == null || x.item == null || x.item.isBlank());
            for (Entry x : list) {
                x.min = Math.max(1, x.min);
                x.max = Math.max(x.min, x.max);
                x.weight = Math.max(0, x.weight);
            }
            while (list.size() > MAX_ENTRIES) list.remove(list.size() - 1);
        }
        return this;
    }
}
