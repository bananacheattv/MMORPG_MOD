package com.mmorpg.rpg;

/**
 * Toutes les statistiques du systeme RPG.
 * Les huit premieres sont les attributs primaires (repartissables avec les points d'attribut), deux par classe :
 * Guerrier = Force + Ferocite, Archer = Agilite + Dexterite, Mage = Intelligence + Esprit, Tank = Vitalite + Endurance ;
 * les suivantes sont derivees des attributs, du niveau, de la classe, de l'equipement, du familier et des effets.
 */
public enum Stat {
    FORCE("Force", "FOR", Kind.PRIMARY, 0xFFE06040),
    AGILITE("Agilité", "AGI", Kind.PRIMARY, 0xFF60D060),
    INTELLIGENCE("Intelligence", "INT", Kind.PRIMARY, 0xFF8080FF),
    VITALITE("Vitalité", "VIT", Kind.PRIMARY, 0xFFFF7070),
    ESPRIT("Esprit", "ESP", Kind.PRIMARY, 0xFF60D0FF),
    FEROCITE("Férocité", "FER", Kind.PRIMARY, 0xFFFF4070),
    DEXTERITE("Dextérité", "DEX", Kind.PRIMARY, 0xFFD0E050),
    ENDURANCE("Endurance", "END", Kind.PRIMARY, 0xFFD0A060),

    MAX_HP("Points de vie", "PV", Kind.FLAT, 0xFFFF5050),
    MAX_MANA("Mana", "PM", Kind.FLAT, 0xFF5090FF),
    ATK("Attaque", "ATQ", Kind.FLAT, 0xFFFF9040),
    MAG("Puissance magique", "MAG", Kind.FLAT, 0xFFC070FF),
    DEF("Défense", "DEF", Kind.FLAT, 0xFFA0B0C0),
    CRIT("Chance de critique", "CRIT", Kind.PERCENT, 0xFFFFE040),
    CRIT_DMG("Dégâts critiques", "DCRIT", Kind.PERCENT, 0xFFFFB020),
    ESQ("Esquive", "ESQ", Kind.PERCENT, 0xFF70FFB0),
    SPEED("Vitesse de déplacement", "VIT", Kind.PERCENT, 0xFF80FFFF),
    ATK_SPEED("Vitesse d'attaque", "VATQ", Kind.PERCENT, 0xFFFFD080),
    HP_REGEN("Régénération de vie", "RPV", Kind.PER_SEC, 0xFFFF8080),
    MANA_REGEN("Régénération de mana", "RPM", Kind.PER_SEC, 0xFF80B0FF),
    LIFESTEAL("Vol de vie", "VDV", Kind.PERCENT, 0xFFD02040),
    CDR("Réduction de recharge", "RDR", Kind.PERCENT, 0xFF40E0D0);

    public enum Kind { PRIMARY, FLAT, PERCENT, PER_SEC }

    /** Attributs primaires, dans l'ordre des points investis (PlayerData.allocated). */
    public static final Stat[] PRIMARIES = {FORCE, AGILITE, INTELLIGENCE, VITALITE, ESPRIT, FEROCITE, DEXTERITE, ENDURANCE};
    /** Ordre d'affichage : les deux attributs de chaque classe cote a cote. */
    public static final Stat[] PRIMARIES_DISPLAY = {FORCE, FEROCITE, AGILITE, DEXTERITE, INTELLIGENCE, ESPRIT, VITALITE, ENDURANCE};

    /** Indice d'un attribut primaire dans PRIMARIES (points investis), -1 sinon. */
    public int primaryIndex() {
        for (int i = 0; i < PRIMARIES.length; i++) {
            if (PRIMARIES[i] == this) return i;
        }
        return -1;
    }

    public final String label;
    public final String shortName;
    public final Kind kind;
    public final int color;

    Stat(String label, String shortName, Kind kind, int color) {
        this.label = label;
        this.shortName = shortName;
        this.kind = kind;
        this.color = color;
    }

    public boolean isPrimary() {
        return kind == Kind.PRIMARY;
    }

    /** Valeur formatee pour l'affichage (ex : "+12", "+4,5 %", "+2,0 /s"). */
    public String format(double value, boolean signed) {
        String sign = signed && value >= 0 ? "+" : "";
        return switch (kind) {
            case PERCENT -> sign + fmt(value, 1) + " %";
            case PER_SEC -> sign + fmt(value, 1) + " /s";
            default -> sign + fmt(value, 0);
        };
    }

    public static String fmt(double v, int decimals) {
        if (decimals == 0 || Math.abs(v - Math.rint(v)) < 0.05) {
            return String.valueOf(Math.round(v));
        }
        return String.format(java.util.Locale.FRANCE, "%." + decimals + "f", v);
    }

    /** Plafonds de securite pour garder le systeme equilibre. */
    public double cap() {
        return switch (this) {
            case CRIT -> 75;
            case ESQ -> 45;
            case SPEED -> 60;
            case ATK_SPEED -> 60;
            case LIFESTEAL -> 25;
            case CDR -> 40;
            default -> Double.MAX_VALUE;
        };
    }
}
