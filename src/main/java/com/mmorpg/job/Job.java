package com.mmorpg.job;

import java.util.List;

/** Les trois metiers de recolte : niveaux 1 a 50, paliers a 10 / 25 / 50. */
public enum Job {
    PECHEUR("pecheur", "Pêcheur", 0xFF4FB3E8, "Pêchez : chaque prise rapporte de l'expérience.",
            List.of(new Milestone(10, "Pêcheur de l'Aube", "Recette : Filet de poisson grillé", "Cosmétique : Cannes à pêche argentée", "Poissons rares plus fréquents, ingrédients marins"),
                    new Milestone(25, "Maître des Eaux", "Recette : Ragoût des profondeurs", "Cosmétique : Lueur des marées", "Double prise, poissons très rares"),
                    new Milestone(50, "Légende des Abysses", "Recette : Festin du Léviathan", "Cosmétique : Aura des abysses", "Prises doublées plus souvent, trésors marins"))),
    FERMIER("fermier", "Fermier", 0xFF7BCB4A, "Récoltez les cultures mûres d'Aeloria.",
            List.of(new Milestone(10, "Fermier Diligent", "Recette : Soupe de racine", "Cosmétique : Chapeau de paille", "Chance de double récolte, plats à effets"),
                    new Milestone(25, "Maître Cultivateur", "Recette : Tourte aux baies", "Cosmétique : Épouvantail doré", "Double récolte accrue, plats plus puissants"),
                    new Milestone(50, "Gardien des Moissons", "Recette : Banquet des moissons", "Cosmétique : Couronne d'épis", "Double récolte maximale, plats de grand banquet"))),
    MINEUR("mineur", "Mineur", 0xFFE0A24A, "Extrayez les minerais des mines d'Aeloria.",
            List.of(new Milestone(10, "Piocheur Tenace", "Recette : Lingot raffiné", "Cosmétique : Casque de mineur", "Chance de double minerai, minage plus rapide"),
                    new Milestone(25, "Maître des Filons", "Recette : Alliage renforcé", "Cosmétique : Lanterne de filon", "Double minerai accru, minage encore plus rapide"),
                    new Milestone(50, "Seigneur des Profondeurs", "Recette : Alliage du Seigneur", "Cosmétique : Aura de gemmes", "Double minerai maximal, vitesse de minage maximale")));

    public static final int MAX_LEVEL = 50;

    public record Milestone(int level, String title, String recipe, String cosmetic, String bonus) {
    }

    public final String id;
    public final String label;
    public final int color;
    public final String description;
    public final List<Milestone> milestones;

    Job(String id, String label, int color, String description, List<Milestone> milestones) {
        this.id = id;
        this.label = label;
        this.color = color;
        this.description = description;
        this.milestones = milestones;
    }

    public static Job byId(String id) {
        for (Job j : values()) if (j.id.equals(id)) return j;
        return null;
    }

    /** Experience necessaire pour passer du niveau {@code level} au suivant. */
    public static long xpToNext(int level) {
        return 40L + 14L * level + (long) level * level * 2L;
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

    /** Nombre de paliers atteints (0 a 3) au niveau donne. */
    public static int tier(int level) {
        return level >= 50 ? 3 : level >= 25 ? 2 : level >= 10 ? 1 : 0;
    }

    /** Chance (0 a 0,5) de doubler la prise / recolte / extraction. */
    public static double doubleChance(int level) {
        return level * 0.004 + (level >= 10 ? 0.05 : 0) + (level >= 25 ? 0.10 : 0) + (level >= 50 ? 0.15 : 0);
    }

    /** Multiplicateur de vitesse de minage. */
    public static float miningSpeed(int level) {
        return 1f + level * 0.01f + (level >= 10 ? 0.10f : 0) + (level >= 25 ? 0.15f : 0) + (level >= 50 ? 0.25f : 0);
    }
}
