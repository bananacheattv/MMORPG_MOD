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
        return LUCKY.equals(id) ? "Lucky Block" : "Caisse d'aventure";
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
                e("coffre_cosmetique", 1, 1, 2))));
        t.tables.put(LUCKY, new ArrayList<>(List.of(
                e("or", 30, 300, 25), e("potion_soin_majeure", 1, 2, 12), e("potion_mana_majeure", 1, 2, 10),
                e("cle_aventure", 1, 1, 10), e("trefle_quatre_feuilles", 1, 1, 8), e("pierre_amelioration_sup", 1, 2, 8),
                e("charme_chance", 1, 1, 7), e("elixir_experience", 1, 1, 7), e("lucky_block", 1, 1, 5),
                e("oeuf_feu_follet", 1, 1, 3), e("coffre_cosmetique", 1, 1, 3), e("fragment_divin", 1, 1, 2))));
        return t;
    }

    /** Corrige les valeurs invalides et complete les tables manquantes. */
    public LootTables sanitize() {
        if (tables == null) tables = new LinkedHashMap<>();
        LootTables d = defaults();
        for (String id : List.of(CRATE, LUCKY)) {
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
