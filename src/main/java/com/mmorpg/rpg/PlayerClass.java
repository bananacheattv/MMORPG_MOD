package com.mmorpg.rpg;

/**
 * Les quatre classes jouables. Chaque classe evolue tous les 25 niveaux (25, 50, 75 et 100)
 * et change alors de nom, gagne des bonus de statistiques et debloque de nouvelles competences.
 */
public enum PlayerClass {
    NONE("Aventurier", 0xFFB0B0B0, "Sans classe",
            new String[]{"Aventurier", "Aventurier", "Aventurier", "Aventurier", "Aventurier"},
            "Choisissez une classe pour commencer votre aventure.",
            new double[]{4, 4, 4, 4, 4, 4, 4, 4}, new double[]{0, 0, 0, 0, 0, 0, 0, 0},
            80, 6, 2, new int[]{1, 1, 1, 1, 1}),
    GUERRIER("Guerrier", 0xFFD04030, "Combattant de mêlée — dégâts physiques",
            new String[]{"Guerrier", "Berserker", "Seigneur de Guerre", "Champion Légendaire", "Dieu de la Guerre"},
            "Maître des armes lourdes, le Guerrier frappe fort et encaisse bien. Sa Force alimente des attaques dévastatrices, ses cris galvanisent ses alliés et sa rage le rend presque inarrêtable.",
            new double[]{8, 5, 2, 6, 3, 6, 3, 4}, new double[]{2.0, 1.0, 0.2, 1.2, 0.3, 1.6, 0.4, 0.6},
            110, 13, 2, new int[]{5, 3, 1, 3, 2}),
    MAGE("Mage", 0xFF8A5CF0, "Lanceur de sorts — dégâts magiques à distance",
            new String[]{"Mage", "Sorcier", "Archimage", "Arcaniste Suprême", "Avatar des Arcanes"},
            "Le Mage manie le feu, la glace et la foudre. Fragile mais redoutable, son Intelligence nourrit des sorts de zone capables d'anéantir des groupes entiers d'ennemis.",
            new double[]{2, 4, 10, 4, 7, 1, 3, 2}, new double[]{0.2, 0.6, 2.4, 0.8, 1.4, 0.1, 0.4, 0.4},
            75, 7, 6, new int[]{2, 1, 5, 2, 4}),
    ARCHER("Archer", 0xFF4CC04C, "Tireur agile — dégâts physiques à distance",
            new String[]{"Archer", "Rôdeur", "Tireur d'Élite", "Sentinelle Sylvestre", "Archer Divin"},
            "Rapide et précis, l'Archer élimine ses cibles avant qu'elles ne l'atteignent. Son Agilité augmente ses coups critiques et son esquive, ses flèches pleuvent sur le champ de bataille.",
            new double[]{4, 10, 3, 4, 3, 3, 8, 3}, new double[]{0.8, 2.2, 0.4, 1.0, 0.6, 0.5, 1.8, 0.5},
            85, 9, 3, new int[]{4, 2, 1, 5, 3}),
    TANK("Tank", 0xFF3D8BD8, "Protecteur — défense et contrôle",
            new String[]{"Tank", "Gardien", "Paladin", "Rempart Sacré", "Titan Immortel"},
            "Bouclier vivant du groupe, le Tank attire l'attention des monstres et résiste aux pires assauts. Sa Vitalité colossale et ses auras sacrées protègent ses compagnons.",
            new double[]{6, 2, 2, 10, 4, 3, 2, 9}, new double[]{1.2, 0.3, 0.2, 2.5, 0.8, 0.5, 0.3, 2.0},
            150, 19, 2, new int[]{2, 5, 1, 1, 2});

    public static final int[] EVOLUTION_LEVELS = {1, 25, 50, 75, 100};

    /** Les deux attributs principaux de la classe (affiches en avant, donnes par son equipement). */
    public Stat[] mainAttributes() {
        return switch (this) {
            case GUERRIER -> new Stat[]{Stat.FORCE, Stat.FEROCITE};
            case MAGE -> new Stat[]{Stat.INTELLIGENCE, Stat.ESPRIT};
            case ARCHER -> new Stat[]{Stat.AGILITE, Stat.DEXTERITE};
            case TANK -> new Stat[]{Stat.VITALITE, Stat.ENDURANCE};
            default -> new Stat[0];
        };
    }

    public final String label;
    public final int color;
    public final String role;
    public final String[] evolutions;
    public final String description;
    /** Attributs de base au niveau 1 (FOR, AGI, INT, VIT, ESP). */
    public final double[] baseAttributes;
    /** Gain automatique d'attributs par niveau. */
    public final double[] growth;
    public final double baseHp;
    public final double hpPerLevel;
    public final double manaPerLevel;
    /** Notes affichees dans l'ecran de choix : attaque, defense, magie, mobilite, difficulte. */
    public final int[] ratings;

    PlayerClass(String label, int color, String role, String[] evolutions, String description, double[] baseAttributes,
                double[] growth, double baseHp, double hpPerLevel, double manaPerLevel, int[] ratings) {
        this.label = label;
        this.color = color;
        this.role = role;
        this.evolutions = evolutions;
        this.description = description;
        this.baseAttributes = baseAttributes;
        this.growth = growth;
        this.baseHp = baseHp;
        this.hpPerLevel = hpPerLevel;
        this.manaPerLevel = manaPerLevel;
        this.ratings = ratings;
    }

    /** Palier d'evolution : 0 (niv. 1-24), 1 (25-49), 2 (50-74), 3 (75-99), 4 (100). */
    public static int tier(int level) {
        return Math.max(0, Math.min(4, level / 25));
    }

    public String title(int level) {
        return evolutions[tier(level)];
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static PlayerClass byId(int ordinal) {
        PlayerClass[] v = values();
        return ordinal >= 0 && ordinal < v.length ? v[ordinal] : NONE;
    }

    public static PlayerClass byName(String name) {
        for (PlayerClass c : values()) {
            if (c.name().equalsIgnoreCase(name) || c.label.equalsIgnoreCase(name)) return c;
        }
        return NONE;
    }

    public boolean isPlayable() {
        return this != NONE;
    }
}
