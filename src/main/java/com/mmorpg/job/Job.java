package com.mmorpg.job;

import java.util.List;
import java.util.Map;

/**
 * Les trois metiers de recolte : niveaux 1 a 50, un bonus de metier tous les 5 niveaux (5 a 50) et une recompense (titre, plat, cosmetique) tous les 10.
 * Classe volontairement sans dependance Minecraft : elle est testee seule (voir JobChecks).
 */
public enum Job {
    PECHEUR("pecheur", "Pêcheur", 0xFF4FB3E8,
            "Lancez votre canne dans les lacs d'Aeloria. Chaque prise rapporte de l'expérience de pêche ; plus votre niveau monte, plus les poissons rares, les ingrédients marins et les doubles prises deviennent fréquents.",
            List.of(new Reward(10, "Pêcheur de l'Aube", "filet_grille", "Filet grillé", null, "potion_soin_majeure", 3, 0),
                    new Reward(20, "Marin des Lacs", null, null, "aura_givre", "charme_experience", 1, 2000),
                    new Reward(30, "Maître des Eaux", "ragout_des_profondeurs", "Ragoût des profondeurs", null, "elixir_experience", 1, 0),
                    new Reward(40, "Harponneur", null, null, "trainee_etoiles", "charme_chance", 1, 5000),
                    new Reward(50, "Légende des Abysses", "festin_du_leviathan", "Festin du Léviathan", "dragon_epaule", "coffre_cosmetique", 1, 15000))),
    FERMIER("fermier", "Fermier", 0xFF7BCB4A,
            "Cultivez et récoltez les plantes d'Aeloria dans les champs et les plaines. Chaque plante mûre rapporte de l'expérience, d'autant plus que la culture est rare ; le métier donne des doubles récoltes et des plats qui renforcent.",
            List.of(new Reward(10, "Semeur Diligent", "soupe_de_racine", "Soupe de racine", null, "potion_soin_majeure", 3, 0),
                    new Reward(20, "Moissonneur", null, null, "trainee_notes", "charme_experience", 1, 2000),
                    new Reward(30, "Maître Cultivateur", "tourte_aux_baies", "Tourte aux baies", null, "elixir_experience", 1, 0),
                    new Reward(40, "Intendant des Terres", null, null, "carquois_sylvestre", "charme_chance", 1, 5000),
                    new Reward(50, "Gardien des Moissons", "banquet_des_moissons", "Banquet des moissons", "ailes_fee", "coffre_cosmetique", 1, 15000))),
    MINEUR("mineur", "Mineur", 0xFFE0A24A,
            "Descendez dans les mines d'Aeloria et extrayez les minerais à la pioche. Chaque minerai rapporte de l'expérience, d'autant plus qu'il est profond ; le métier donne des doubles minerais et accélère le minage.",
            List.of(new Reward(10, "Piocheur Tenace", "tonique_du_mineur", "Tonique du mineur", null, "potion_soin_majeure", 3, 0),
                    new Reward(20, "Fouilleur de Filons", null, null, "aura_arcane", "charme_experience", 1, 2000),
                    new Reward(30, "Maître des Filons", "bouillon_de_roche", "Bouillon de roche", null, "elixir_experience", 1, 0),
                    new Reward(40, "Briseur de Roche", null, null, "couronne_givre", "charme_chance", 1, 5000),
                    new Reward(50, "Seigneur des Profondeurs", "elixir_du_seigneur", "Élixir du Seigneur", "couronne_infernale", "coffre_cosmetique", 1, 15000)));

    public static final int MAX_LEVEL = 50;
    public static final int STEP = 10;

    /** Recompense d'un palier : titre equipable, recette de forge (plat), cosmetique, objet et or, tous donnes une seule fois. */
    public record Reward(int level, String title, String recipeId, String recipeLabel, String cosmeticId, String itemId, int itemCount, int gold) {
    }

    /** Une source d'experience affichee dans l'onglet « Sources d'XP ». */
    public record Source(String label, int xp, String note) {
    }

    public final String id;
    public final String label;
    public final int color;
    public final String description;
    public final List<Reward> rewards;

    Job(String id, String label, int color, String description, List<Reward> rewards) {
        this.id = id;
        this.label = label;
        this.color = color;
        this.description = description;
        this.rewards = rewards;
    }

    public static Job byId(String id) {
        for (Job j : values()) if (j.id.equals(id)) return j;
        return null;
    }

    // ------------------------------------------------------------------ courbe d'experience (exponentielle)

    /** Experience necessaire pour passer du niveau {@code level} au suivant : 60 x 1,13^(niveau-1). */
    public static long xpToNext(int level) {
        return Math.round(60.0 * Math.pow(1.13, Math.max(0, level - 1)));
    }

    /** Experience cumulee necessaire pour atteindre {@code level}. */
    public static long totalFor(int level) {
        long t = 0;
        for (int l = 1; l < level; l++) t += xpToNext(l);
        return t;
    }

    public static int levelFor(long totalXp) {
        int l = 1;
        while (l < MAX_LEVEL && totalXp >= totalFor(l + 1)) l++;
        return l;
    }

    public static int levelOf(Map<String, Long> jobXp, Job job) {
        return levelFor(jobXp.getOrDefault(job.id, 0L));
    }

    /** Nombre de paliers de 10 niveaux atteints (0 a 5). */
    public static int tier(int level) {
        return Math.min(MAX_LEVEL, Math.max(0, level)) / STEP;
    }

    // ------------------------------------------------------------------ bonus passifs

    /** Paliers de bonus impairs atteints (5 / 15 / 25 / 35 / 45) : 0 a 5. */
    public static int oddSteps(int level) {
        return Math.min(5, Math.max(0, level + 5) / STEP);
    }

    /** Chance (0 a 0,30) de doubler la prise / recolte / extraction : +6 % a chaque palier (mineur : paliers pairs, autres : impairs). */
    public static double doubleChance(Job job, int level) {
        return (job == MINEUR ? tier(level) : oddSteps(level)) * 0.06;
    }

    /** Multiplicateur de vitesse de minage : +12 % aux niveaux 5 / 15 / 25 / 35 / 45 (x1,60 des le niveau 45). */
    public static float miningSpeed(int level) {
        return 1f + oddSteps(level) * 0.12f;
    }

    /** Chance qu'une prise de peche s'accompagne d'un poisson rare ou d'un tresor marin (des le palier 10). */
    public static double rareCatchChance(int level) {
        return tier(level) == 0 ? 0 : 0.03 + level * 0.003 + tier(level) * 0.01;
    }

    /** Chance qu'une prise s'accompagne d'ingredients marins (des le palier 10). */
    public static double ingredientChance(int level) {
        return tier(level) == 0 ? 0 : 0.10 + tier(level) * 0.04;
    }

    /** Chance qu'une recolte procure un « repas du fermier » (effets temporaires, des le palier 10). */
    public static double mealChance(int level) {
        return tier(level) == 0 ? 0 : 0.08 + tier(level) * 0.03;
    }

    /** Duree (secondes) des effets du repas du fermier. */
    public static int mealSeconds(int level) {
        return 30 + tier(level) * 30;
    }

    /** Un bonus fonctionnel debloque a un niveau (tous les 5 niveaux). */
    public record Perk(int level, String text) {
    }

    /** Les dix bonus d'un metier (niveaux 5, 10, ... 50), avec la valeur cumulee a ce niveau. */
    public static List<Perk> perks(Job job) {
        java.util.ArrayList<Perk> out = new java.util.ArrayList<>();
        for (int l = STEP / 2; l <= MAX_LEVEL; l += STEP / 2) {
            boolean odd = (l / (STEP / 2)) % 2 == 1;
            int dbl = (int) Math.round(doubleChance(job, l) * 100);
            String t = switch (job) {
                case MINEUR -> odd ? "Vitesse de minage +12 % (total +" + Math.round((miningSpeed(l) - 1) * 100) + " %)"
                        : "Double minerai +6 % (total " + dbl + " %)";
                case FERMIER -> odd ? "Double récolte +6 % (total " + dbl + " %)"
                        : l == STEP ? "Repas du fermier : " + Math.round(mealChance(l) * 100) + " % par récolte, " + mealSeconds(l) + " s"
                        : "Repas du fermier : " + Math.round(mealChance(l) * 100) + " % par récolte, " + mealSeconds(l) + " s d'effets";
                case PECHEUR -> odd ? "Double prise +6 % (total " + dbl + " %)"
                        : l == STEP ? "Poissons rares " + Math.round(rareCatchChance(l) * 100) + " % · ingrédients marins " + Math.round(ingredientChance(l) * 100) + " %"
                        : "Prise rare " + Math.round(rareCatchChance(l) * 100) + " % · ingrédients " + Math.round(ingredientChance(l) * 100) + " %";
            };
            out.add(new Perk(l, t));
        }
        return out;
    }

    /** Resume lisible des bonus passifs au niveau donne. */
    public static List<String> bonusLines(Job job, int level) {
        int pct = (int) Math.round(doubleChance(job, level) * 100);
        return switch (job) {
            case MINEUR -> List.of("Double minerai : " + pct + " % de chance",
                    "Vitesse de minage : +" + Math.round((miningSpeed(level) - 1) * 100) + " %");
            case FERMIER -> List.of("Double récolte : " + pct + " % de chance",
                    tier(level) == 0 ? "Repas du fermier : dès le niveau 10"
                            : "Repas du fermier : " + Math.round(mealChance(level) * 100) + " % par récolte, " + mealSeconds(level) + " s d'effets");
            case PECHEUR -> List.of("Double prise : " + pct + " % de chance",
                    tier(level) == 0 ? "Poissons rares et ingrédients : dès le niveau 10"
                            : "Prise rare : " + Math.round(rareCatchChance(level) * 100) + " % · ingrédients : " + Math.round(ingredientChance(level) * 100) + " %");
        };
    }

    // ------------------------------------------------------------------ sources d'experience

    public static final String[] ORE_NAMES = {"Verdite", "Argentite", "Ambrasite", "Glacium", "Néantite"};
    public static final int[] ORE_XP = {3, 7, 14, 24, 40};
    public static final String[] CROP_NAMES = {"Racine de Lueur", "Blé d'Argent", "Baie Ambrée", "Fleur de Givre", "Orchidée du Néant"};
    public static final int[] CROP_XP = {3, 6, 11, 18, 28};
    public static final int FISH_COMMON_XP = 12, FISH_RARE_XP = 20, FISH_TREASURE_XP = 45, FISH_JUNK_XP = 5;

    public static int oreXp(int tier) {
        return ORE_XP[Math.clamp(tier, 0, ORE_XP.length - 1)];
    }

    public static int cropXp(int rank) {
        return CROP_XP[Math.clamp(rank, 0, CROP_XP.length - 1)];
    }

    public static List<Source> sources(Job job) {
        return switch (job) {
            case MINEUR -> List.of(
                    new Source("Verdite", ORE_XP[0], "par minerai extrait (mine 1)"),
                    new Source("Argentite", ORE_XP[1], "par minerai extrait (mine 2)"),
                    new Source("Ambrasite", ORE_XP[2], "par minerai extrait (mine 3)"),
                    new Source("Glacium", ORE_XP[3], "par minerai extrait (mine 4)"),
                    new Source("Néantite", ORE_XP[4], "par minerai extrait (mine 5)"),
                    new Source("Veine entière", 0, "chaque minerai de la veine compte"));
            case FERMIER -> List.of(
                    new Source("Racine de Lueur", CROP_XP[0], "par plante mûre récoltée"),
                    new Source("Blé d'Argent", CROP_XP[1], "par plante mûre récoltée"),
                    new Source("Baie Ambrée", CROP_XP[2], "par plante mûre récoltée"),
                    new Source("Fleur de Givre", CROP_XP[3], "par plante mûre récoltée"),
                    new Source("Orchidée du Néant", CROP_XP[4], "par plante mûre récoltée"),
                    new Source("Récolte en zone", 0, "chaque plante du rayon compte"));
            case PECHEUR -> List.of(
                    new Source("Poisson commun", FISH_COMMON_XP, "morue, saumon"),
                    new Source("Poisson rare", FISH_RARE_XP, "poisson tropical, poisson-globe"),
                    new Source("Trésor", FISH_TREASURE_XP, "livre, selle, nautile, canne..."),
                    new Source("Déchet", FISH_JUNK_XP, "bottes, ficelle, os..."),
                    new Source("Prise bonus", 0, "poisson rare ou ingrédient : sans XP en plus"));
        };
    }

    // ------------------------------------------------------------------ verrouillage des recettes de plats

    /** Niveau de metier requis pour une recette de forge de plat, ou {@code null} si la recette n'est pas liee a un metier. */
    public record Gate(Job job, int level) {
    }

    public static Gate gateFor(String recipeId) {
        for (Job j : values()) {
            for (Reward r : j.rewards) {
                if (recipeId.equals(r.recipeId())) return new Gate(j, r.level());
            }
        }
        return null;
    }

    public static boolean recipeUnlocked(Map<String, Long> jobXp, String recipeId) {
        Gate g = gateFor(recipeId);
        return g == null || levelOf(jobXp, g.job()) >= g.level();
    }
}
