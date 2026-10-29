package com.mmorpg.fishing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Recompenses des coffres repeches : or, lingots, ingredients marins, cles, pierres, cosmetiques et, tres rarement, oeuf ou sceau
 * mystere. Quatre rangs (rouille, corail, capitaine, abyssal). Sans dependance Minecraft ; les identifiants sont verifies par FishingChecks.
 */
public final class TreasureLoot {
    private TreasureLoot() {
    }

    public record Drop(String itemId, int min, int max, double chance) {
    }

    public record Reward(String itemId, int count) {
    }

    public static final long[] GOLD_MIN = {30, 150, 600, 2500};
    public static final long[] GOLD_MAX = {120, 500, 1800, 6000};

    private static final List<List<Drop>> TABLES = List.of(
            List.of(new Drop("lingot_verdite", 2, 5, 0.80), new Drop("algue_lumineuse", 1, 3, 0.60), new Drop("ecaille_argentee", 1, 3, 0.45),
                    new Drop("pierre_amelioration", 1, 2, 0.30), new Drop("potion_soin_majeure", 1, 1, 0.20), new Drop("perle_nacree", 1, 1, 0.08),
                    new Drop("cle_commune", 1, 1, 0.06)),
            List.of(new Drop("lingot_argentite", 3, 6, 0.80), new Drop("perle_nacree", 1, 2, 0.60), new Drop("corail_ancien", 1, 1, 0.30),
                    new Drop("pierre_amelioration", 2, 3, 0.50), new Drop("gemme_brute", 1, 1, 0.15), new Drop("cle_commune", 1, 1, 0.25),
                    new Drop("cle_rare", 1, 1, 0.10), new Drop("trefle_chance", 1, 1, 0.25)),
            List.of(new Drop("lingot_ambrasite", 3, 6, 0.80), new Drop("corail_ancien", 1, 3, 0.65), new Drop("larme_de_sirene", 1, 1, 0.35),
                    new Drop("cle_rare", 1, 1, 0.30), new Drop("pierre_amelioration_sup", 1, 1, 0.15), new Drop("trefle_quatre_feuilles", 1, 1, 0.18),
                    new Drop("coffre_cosmetique", 1, 1, 0.06), new Drop("gemme_brute", 1, 2, 0.30)),
            List.of(new Drop("lingot_glacium", 3, 6, 0.85), new Drop("larme_de_sirene", 1, 3, 0.70), new Drop("ecaille_de_leviathan", 1, 1, 0.40),
                    new Drop("cle_epique", 1, 1, 0.35), new Drop("pierre_amelioration_sup", 1, 2, 0.35), new Drop("trefle_dore", 1, 1, 0.25),
                    new Drop("coffre_cosmetique", 1, 1, 0.15), new Drop("cle_legendaire", 1, 1, 0.05), new Drop("oeuf_mystere", 1, 1, 0.04),
                    new Drop("sceau_mystere", 1, 1, 0.03)));

    public static List<Drop> table(int rank) {
        return TABLES.get(Math.clamp(rank, 0, TABLES.size() - 1));
    }

    public static long gold(int rank, Random rnd) {
        int r = Math.clamp(rank, 0, GOLD_MIN.length - 1);
        return GOLD_MIN[r] + (long) (rnd.nextDouble() * (GOLD_MAX[r] - GOLD_MIN[r] + 1));
    }

    /** Objets d'un coffre ; au moins un objet est toujours donne. */
    public static List<Reward> roll(int rank, Random rnd) {
        List<Reward> out = new ArrayList<>();
        List<Drop> table = table(rank);
        for (Drop d : table) {
            if (rnd.nextDouble() < d.chance()) out.add(new Reward(d.itemId(), d.min() + rnd.nextInt(d.max() - d.min() + 1)));
        }
        if (out.isEmpty()) {
            Drop d = table.get(0);
            out.add(new Reward(d.itemId(), d.min() + rnd.nextInt(d.max() - d.min() + 1)));
        }
        return out;
    }
}
