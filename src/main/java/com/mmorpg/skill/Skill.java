package com.mmorpg.skill;

import com.mmorpg.rpg.PlayerClass;

/** Definition d'une competence (donnees partagees client/serveur). */
public final class Skill {
    public static final int MAX_RANK = 5;

    public interface Describer {
        String describe(Skill skill, int rank);
    }

    public final String id;
    public final String name;
    public final PlayerClass playerClass;
    public final int unlockLevel;
    public final boolean passive;
    public final boolean ultimate;
    public final int manaCost;
    /** Temps de recharge de base en ticks. */
    public final int cooldown;
    /** Coefficient principal (1.8 = 180 % de la statistique de reference). */
    public final double power;
    /** Coefficient secondaire (selon la competence). */
    public final double power2;
    public final double radius;
    /** Duree en ticks des effets (buffs, controles...). */
    public final int duration;
    private final Describer describer;

    private Skill(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.playerClass = b.playerClass;
        this.unlockLevel = b.unlockLevel;
        this.passive = b.passive;
        this.ultimate = b.ultimate;
        this.manaCost = b.manaCost;
        this.cooldown = b.cooldown;
        this.power = b.power;
        this.power2 = b.power2;
        this.radius = b.radius;
        this.duration = b.duration;
        this.describer = b.describer;
    }

    /** Multiplicateur d'efficacite selon le rang (+15 % par rang au-dela du premier). */
    public static double rankMult(int rank) {
        return 1.0 + 0.15 * (Math.max(1, rank) - 1);
    }

    public double scaled(int rank) {
        return power * rankMult(rank);
    }

    public double scaled2(int rank) {
        return power2 * rankMult(rank);
    }

    public String describe(int rank) {
        return describer.describe(this, Math.max(1, rank));
    }

    public String typeLabel() {
        if (passive) return "Passif";
        if (ultimate) return "Ultime";
        return "Actif";
    }

    public static String pct(double coefficient) {
        return Math.round(coefficient * 100) + " %";
    }

    public static String sec(long ticks) {
        double s = ticks / 20.0;
        return (Math.abs(s - Math.rint(s)) < 0.01 ? String.valueOf(Math.round(s)) : String.format(java.util.Locale.FRANCE, "%.1f", s)) + " s";
    }

    public static Builder builder(String id, String name, PlayerClass cls, int unlockLevel) {
        return new Builder(id, name, cls, unlockLevel);
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private final PlayerClass playerClass;
        private final int unlockLevel;
        private boolean passive;
        private boolean ultimate;
        private int manaCost;
        private int cooldown;
        private double power;
        private double power2;
        private double radius;
        private int duration;
        private Describer describer = (s, r) -> "";

        private Builder(String id, String name, PlayerClass cls, int unlockLevel) {
            this.id = id;
            this.name = name;
            this.playerClass = cls;
            this.unlockLevel = unlockLevel;
        }

        public Builder passive() {
            this.passive = true;
            return this;
        }

        public Builder ultimate() {
            this.ultimate = true;
            return this;
        }

        public Builder cost(int mana, double cooldownSeconds) {
            this.manaCost = mana;
            this.cooldown = (int) Math.round(cooldownSeconds * 20);
            return this;
        }

        public Builder power(double power) {
            this.power = power;
            return this;
        }

        public Builder power2(double power2) {
            this.power2 = power2;
            return this;
        }

        public Builder radius(double radius) {
            this.radius = radius;
            return this;
        }

        public Builder duration(double seconds) {
            this.duration = (int) Math.round(seconds * 20);
            return this;
        }

        public Builder desc(Describer describer) {
            this.describer = describer;
            return this;
        }

        public Skill build() {
            return new Skill(this);
        }
    }
}
