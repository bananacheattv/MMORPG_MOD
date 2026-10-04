package com.mmorpg.rpg;

import java.util.Arrays;

/** Vecteur de statistiques (une valeur par {@link Stat}). */
public final class StatBlock {
    private final double[] values = new double[Stat.values().length];

    public static StatBlock of(Object... pairs) {
        StatBlock b = new StatBlock();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            b.add((Stat) pairs[i], ((Number) pairs[i + 1]).doubleValue());
        }
        return b;
    }

    public double get(Stat stat) {
        return values[stat.ordinal()];
    }

    public StatBlock set(Stat stat, double value) {
        values[stat.ordinal()] = value;
        return this;
    }

    public StatBlock add(Stat stat, double value) {
        values[stat.ordinal()] += value;
        return this;
    }

    public StatBlock addAll(StatBlock other) {
        for (int i = 0; i < values.length; i++) {
            values[i] += other.values[i];
        }
        return this;
    }

    public StatBlock addScaled(StatBlock other, double factor) {
        for (int i = 0; i < values.length; i++) {
            values[i] += other.values[i] * factor;
        }
        return this;
    }

    public StatBlock scaled(double factor) {
        StatBlock b = copy();
        for (int i = 0; i < b.values.length; i++) {
            b.values[i] *= factor;
        }
        return b;
    }

    public StatBlock copy() {
        StatBlock b = new StatBlock();
        System.arraycopy(values, 0, b.values, 0, values.length);
        return b;
    }

    public boolean isEmpty() {
        for (double v : values) {
            if (Math.abs(v) > 1e-9) return false;
        }
        return true;
    }

    public double[] raw() {
        return values;
    }

    public static StatBlock fromRaw(double[] raw) {
        StatBlock b = new StatBlock();
        System.arraycopy(raw, 0, b.values, 0, Math.min(raw.length, b.values.length));
        return b;
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }
}
