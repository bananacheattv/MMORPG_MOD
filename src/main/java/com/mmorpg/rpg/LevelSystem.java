package com.mmorpg.rpg;

/** Courbe d'experience et points gagnes par niveau. */
public final class LevelSystem {
    public static final int MAX_LEVEL = 100;
    public static final int ATTRIBUTE_POINTS_PER_LEVEL = 3;

    private LevelSystem() {
    }

    /** Experience necessaire pour passer du niveau {@code level} au suivant. */
    public static long xpToNext(int level) {
        if (level >= MAX_LEVEL) return 0;
        return Math.round(100 + 40 * Math.pow(level, 1.9));
    }

    /** Experience totale cumulee pour atteindre un niveau. */
    public static long totalXpFor(int level) {
        long total = 0;
        for (int l = 1; l < level; l++) total += xpToNext(l);
        return total;
    }

    public static int attributePointsAt(int level) {
        return (level - 1) * ATTRIBUTE_POINTS_PER_LEVEL;
    }

    /** Points de competence cumules : 1 tous les 4 niveaux. */
    public static int skillPointsAt(int level) {
        return level / 4;
    }

    /**
     * Experience accordee pour avoir vaincu un monstre.
     *
     * @param mobLevel   niveau du monstre
     * @param multiplier multiplicateur du type de monstre (boss, elite...)
     * @param playerLevel niveau du joueur (penalite si l'ecart est trop grand)
     */
    public static long xpForKill(int mobLevel, double multiplier, int playerLevel) {
        double base = 10 + 0.6 * mobLevel * mobLevel + 5 * mobLevel;
        int diff = playerLevel - mobLevel;
        double mod;
        if (diff > 15) mod = 0.1;
        else if (diff > 10) mod = 0.3;
        else if (diff > 5) mod = 0.7;
        else if (diff < -5) mod = 1.2;
        else mod = 1.0;
        return Math.max(1, Math.round(base * multiplier * mod));
    }
}
