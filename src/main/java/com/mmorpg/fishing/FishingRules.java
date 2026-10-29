package com.mmorpg.fishing;

import com.mmorpg.job.Job;
import com.mmorpg.rpg.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Regles de la peche : table de butin (habitat, heure, meteo, niveau de metier, canne), bonus de metier et tailles des prises.
 * Sans dependance Minecraft : tout est teste dans FishingChecks.
 */
public final class FishingRules {
    private FishingRules() {
    }

    /** Situation d'un lancer : niveau de metier, rang de canne (0 = vanilla, 1 a 6), lieu et moment. */
    public record Context(int jobLevel, int rodTier, FishCatalog.Habitat habitat, boolean night, boolean rain) {
        public int luck() {
            FishCatalog.Rod r = FishCatalog.rod(rodTier);
            return (r == null ? 0 : r.luck()) + Job.tier(jobLevel);
        }
    }

    /** Profondeur d'eau a partir de laquelle on peche dans les abysses, quel que soit le biome. */
    public static final int ABYSS_DEPTH = 10;

    /** Habitat d'un lancer selon le biome (ocean profond, ocean, riviere, marais, jungle, froid) et la profondeur d'eau sous le bouchon. */
    public static FishCatalog.Habitat habitat(boolean deepOcean, boolean ocean, boolean river, boolean swamp, boolean jungle, boolean cold, int waterDepth) {
        if (deepOcean || waterDepth >= ABYSS_DEPTH) return FishCatalog.Habitat.ABYSSES;
        if (cold) return FishCatalog.Habitat.GLACE;
        if (ocean) return FishCatalog.Habitat.MER;
        if (river) return FishCatalog.Habitat.RIVIERE;
        if (swamp) return FishCatalog.Habitat.MARAIS;
        if (jungle) return FishCatalog.Habitat.JUNGLE;
        return FishCatalog.Habitat.LAC;
    }

    // ------------------------------------------------------------------ bonus du metier de pecheur

    /** Part de dechets : 14 % au debut, 2 % au minimum (baisse avec le niveau et la canne). */
    public static double junkChance(int level, int rodTier) {
        return Math.max(0.02, 0.14 - 0.0016 * level - 0.008 * rodTier);
    }

    /** Chance qu'un lancer ramene un coffre : progresse avec le niveau, la canne, les paliers et sous la pluie. */
    public static double treasureChance(int level, int rodTier, boolean rain) {
        return 0.025 + 0.0006 * level + 0.004 * rodTier + 0.004 * Job.tier(level) + (rain ? 0.012 : 0);
    }

    /** Agrandissement de la zone du mini-jeu apporte par les paliers du metier (+4 % par palier). */
    public static double jobZoneBonus(int level) {
        return 1.0 + 0.04 * Job.tier(level);
    }

    /** Niveaux d'« appat » supplementaires donnes par le metier (1 au palier 20, 2 au palier 40), pour une touche plus rapide. */
    public static int jobLure(int level) {
        return Job.tier(level) / 2;
    }

    /** Multiplicateur d'experience du metier sur une prise reussie (+2 % par palier). */
    public static double jobXpBonus(int level) {
        return 1.0 + 0.02 * Job.tier(level);
    }

    // ------------------------------------------------------------------ tirage

    private static boolean momentOk(FishCatalog.Moment m, Context c) {
        return switch (m) {
            case TOUJOURS -> true;
            case JOUR -> !c.night();
            case NUIT -> c.night();
            case PLUIE -> c.rain();
        };
    }

    /** Prises (poissons) possibles dans ces conditions. */
    public static List<FishCatalog.Entry> fishPool(Context c, Rarity minimum) {
        List<FishCatalog.Entry> out = new ArrayList<>();
        for (FishCatalog.Entry e : FishCatalog.ALL) {
            if (!e.fish()) continue;
            if (e.rarity().ordinal() < minimum.ordinal()) continue;
            if (e.minLevel() > c.jobLevel() || c.rodTier() < FishCatalog.rodTierFor(e.rarity())) continue;
            if (!e.habitats().contains(c.habitat()) || !momentOk(e.moment(), c)) continue;
            out.add(e);
        }
        return out;
    }

    private static double weight(FishCatalog.Entry e, Context c) {
        // la chance gonfle le poids des raretes elevees, sans jamais retirer les communs
        double w = e.weight() * (1.0 + 0.10 * c.luck() * e.rarity().ordinal());
        if (e.moment() != FishCatalog.Moment.TOUJOURS) w *= 1.6; // les poissons de passage sont plus nombreux quand ils sont la
        return w;
    }

    private static FishCatalog.Entry pick(List<FishCatalog.Entry> pool, Context c, Random rnd) {
        double total = 0;
        for (FishCatalog.Entry e : pool) total += weight(e, c);
        double r = rnd.nextDouble() * total;
        for (FishCatalog.Entry e : pool) {
            r -= weight(e, c);
            if (r <= 0) return e;
        }
        return pool.get(pool.size() - 1);
    }

    /** Tire une prise : coffre, dechet ou poisson. Ne renvoie jamais {@code null}. */
    public static FishCatalog.Entry roll(Context c, Random rnd) {
        if (rnd.nextDouble() < treasureChance(c.jobLevel(), c.rodTier(), c.rain())) {
            FishCatalog.Entry chest = rollChest(c, rnd);
            if (chest != null) return chest;
        }
        if (rnd.nextDouble() < junkChance(c.jobLevel(), c.rodTier())) {
            List<FishCatalog.Entry> junk = FishCatalog.of(FishCatalog.Kind.DECHET);
            return pick(junk, c, rnd);
        }
        List<FishCatalog.Entry> pool = fishPool(c, Rarity.COMMUN);
        if (pool.isEmpty()) return FishCatalog.get("gardon_argente");
        return pick(pool, c, rnd);
    }

    /** Coffre tire selon le niveau et la canne (rouille : toujours ; corail : canne 2 ; capitaine : canne 3 ; abyssal : canne 4). */
    public static FishCatalog.Entry rollChest(Context c, Random rnd) {
        List<FishCatalog.Entry> pool = new ArrayList<>();
        int[] rodNeeded = {0, 2, 3, 4};
        List<FishCatalog.Entry> chests = FishCatalog.of(FishCatalog.Kind.TRESOR);
        for (int i = 0; i < chests.size(); i++) {
            FishCatalog.Entry e = chests.get(i);
            if (e.minLevel() <= c.jobLevel() && c.rodTier() >= rodNeeded[i] && e.habitats().contains(c.habitat())) pool.add(e);
        }
        return pool.isEmpty() ? null : pick(pool, c, rnd);
    }

    /** Prise bonus (« prise rare » du metier) : poisson rare ou mieux si l'habitat en offre, sinon le meilleur disponible. */
    public static FishCatalog.Entry rollRare(Context c, Random rnd) {
        List<FishCatalog.Entry> pool = fishPool(c, Rarity.RARE);
        if (pool.isEmpty()) pool = fishPool(c, Rarity.PEU_COMMUN);
        if (pool.isEmpty()) pool = fishPool(c, Rarity.COMMUN);
        return pool.isEmpty() ? FishCatalog.get("gardon_argente") : pick(pool, c, rnd);
    }

    /** Ingredient marin d'une prise bonus, selon le palier du metier. */
    public static FishCatalog.Ingredient rollIngredient(int jobLevel, Random rnd) {
        int tier = Job.tier(jobLevel);
        int max = Math.min(FishCatalog.INGREDIENTS.size() - 1, tier);
        int idx = 0;
        // pondere vers les ingredients bas, mais les plus precieux restent possibles a leur palier
        double r = rnd.nextDouble();
        if (max >= 1 && r < 0.45) idx = 1;
        if (max >= 2 && r < 0.22) idx = 2;
        if (max >= 3 && r < 0.12) idx = 3;
        if (max >= 4 && r < 0.06) idx = 4;
        if (max >= 5 && r < 0.025) idx = 5;
        return FishCatalog.INGREDIENTS.get(Math.min(idx, max));
    }

    /** Taille d'un poisson en cm : biaisee vers le bas ; une touche parfaite augmente les chances d'un gros specimen. */
    public static int sizeCm(FishCatalog.Entry e, boolean perfect, Random rnd) {
        if (e.maxCm() <= 0) return 0;
        double u = rnd.nextDouble();
        double f = u * u;
        if (perfect) f = Math.min(1.0, f + 0.25 + 0.2 * rnd.nextDouble());
        return (int) Math.round(e.minCm() + f * (e.maxCm() - e.minCm()));
    }

    /** Experience d'une prise reussie (touche parfaite : +25 %, bonus de palier inclus). */
    public static long xp(FishCatalog.Entry e, boolean perfect, int jobLevel) {
        double v = e.xp() * jobXpBonus(jobLevel);
        if (perfect) v *= 1.25;
        return Math.round(v);
    }
}
