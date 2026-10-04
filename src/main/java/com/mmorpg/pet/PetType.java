package com.mmorpg.pet;

import com.mmorpg.rpg.Rarity;
import com.mmorpg.rpg.Stat;
import com.mmorpg.rpg.StatBlock;

/** Les familiers : ils suivent le joueur et lui conferent des bonus de statistiques qui grandissent avec leur niveau. */
public enum PetType {
    FEU_FOLLET("feu_follet", "Feu Follet", Rarity.COMMUN, 0xFF80E8FF,
            "Une petite flamme spirituelle qui nourrit votre réserve de mana.",
            StatBlock.of(Stat.MAX_MANA, 30, Stat.MANA_REGEN, 0.6, Stat.ESPRIT, 2)),
    SLIME("slime", "Bébé Slime", Rarity.COMMUN, 0xFF80E060,
            "Un compagnon gluant et robuste qui renforce votre constitution.",
            StatBlock.of(Stat.MAX_HP, 60, Stat.HP_REGEN, 0.6, Stat.VITALITE, 2)),
    CHOUETTE("chouette", "Chouette Sage", Rarity.RARE, 0xFFC09060,
            "Gardienne du savoir, elle affûte votre esprit et vos sortilèges.",
            StatBlock.of(Stat.MAG, 12, Stat.CDR, 3, Stat.INTELLIGENCE, 3)),
    LOUP_SPECTRAL("loup_spectral", "Loup Spectral", Rarity.RARE, 0xFFA0C8FF,
            "Un loup fantôme rapide et féroce qui chasse à vos côtés.",
            StatBlock.of(Stat.ATK, 12, Stat.SPEED, 4, Stat.AGILITE, 3)),
    GOLEM("golem", "Golem de Poche", Rarity.RARE, 0xFFA8A498,
            "Un minuscule golem de pierre qui vous rend plus résistant.",
            StatBlock.of(Stat.DEF, 15, Stat.MAX_HP, 100, Stat.VITALITE, 3)),
    FEE("fee", "Fée Lumineuse", Rarity.EPIQUE, 0xFFFF90D0,
            "Une fée bienveillante qui soigne vos blessures et guide vos pas.",
            StatBlock.of(Stat.HP_REGEN, 2, Stat.ESQ, 3, Stat.ESPRIT, 5, Stat.MAX_HP, 80)),
    PHENIX("phenix", "Phénix Doré", Rarity.LEGENDAIRE, 0xFFFFB030,
            "Oiseau de feu immortel, il embrase votre puissance de combat.",
            StatBlock.of(Stat.ATK, 25, Stat.MAG, 25, Stat.CRIT, 4, Stat.MAX_HP, 150)),
    DRAGONNET("dragonnet", "Dragonnet du Néant", Rarity.MYTHIQUE, 0xFFB060FF,
            "Rejeton de l'Avatar du Néant, il confère une puissance colossale.",
            StatBlock.of(Stat.ATK, 40, Stat.MAG, 40, Stat.DEF, 30, Stat.MAX_HP, 300, Stat.CRIT, 5, Stat.CRIT_DMG, 15));

    public static final int MAX_LEVEL = 10;
    private static final int[] XP_THRESHOLDS = {0, 150, 400, 900, 1800, 3200, 5500, 9000, 14000, 21000};

    public final String id;
    public final String label;
    public final Rarity rarity;
    public final int color;
    public final String description;
    public final StatBlock bonus;

    PetType(String id, String label, Rarity rarity, int color, String description, StatBlock bonus) {
        this.id = id;
        this.label = label;
        this.rarity = rarity;
        this.color = color;
        this.description = description;
        this.bonus = bonus;
    }

    /** Deplacement : en vol permanent, au sol, posé quand le maitre s'arrete, ou au sol mais capable de voler. */
    public enum Move { HOVER, GROUND, PERCH, GROUND_FLY }

    public Move move() {
        return switch (this) {
            case FEU_FOLLET, FEE, DRAGONNET -> Move.HOVER;          // le dragonnet vole en permanence
            case CHOUETTE, PHENIX -> Move.PERCH;
            default -> Move.GROUND;
        };
    }

    public static PetType byId(String id) {
        for (PetType p : values()) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    public static int levelForXp(int xp) {
        int level = 1;
        for (int i = 0; i < XP_THRESHOLDS.length; i++) {
            if (xp >= XP_THRESHOLDS[i]) level = i + 1;
        }
        return Math.min(MAX_LEVEL, level);
    }

    public static int xpForLevel(int level) {
        return XP_THRESHOLDS[Math.max(0, Math.min(MAX_LEVEL - 1, level - 1))];
    }

    /** Multiplicateur des bonus selon le niveau du familier (x1 au niveau 1, x2,35 au niveau 10). */
    public static double levelMult(int level) {
        return 1.0 + 0.15 * (Math.max(1, level) - 1);
    }

    public StatBlock bonusAt(int level) {
        return bonus.scaled(levelMult(level));
    }
}
