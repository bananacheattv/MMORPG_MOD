package com.mmorpg.world;

import java.util.Random;

/**
 * Zones de niveau des mines et des plaines : le sol est decoupe en cellules irregulieres (grille aux centres decales, cellule la plus
 * proche) et chaque cellule porte un des {@link #TIERS} paliers de niveau de sa region. Les monstres d'une cellule ont tous a peu pres le
 * meme niveau, quelle que soit la direction : on peut franchir une frontiere et tomber sur des monstres bien plus forts, ou plus faibles.
 */
public final class LevelZones {
    private LevelZones() {
    }

    public static final int TIERS = 4;

    /** Une cellule : son centre et son palier (0 = niveau le plus bas de la region, TIERS - 1 = le plus haut). */
    public record Cell(int gx, int gz, int cx, int cz, int tier) {
    }

    /** Cellule qui contient la colonne (x, z) ; {@code size} est le pas de la grille en blocs. */
    public static Cell cell(long seed, int x, int z, int size) {
        int gx = Math.floorDiv(x, size), gz = Math.floorDiv(z, size);
        Cell best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Cell c = at(seed, gx + dx, gz + dz, size);
                double d = (double) (c.cx() - x) * (c.cx() - x) + (double) (c.cz() - z) * (c.cz() - z);
                if (d < bestD) {
                    bestD = d;
                    best = c;
                }
            }
        }
        return best;
    }

    /** Cellule de la grille (gx, gz) : centre decale de maniere stable, palier tire au hasard. */
    public static Cell at(long seed, int gx, int gz, int size) {
        int cx = gx * size + (int) (size * (0.15 + 0.7 * hash(seed, gx, gz, 1)));
        int cz = gz * size + (int) (size * (0.15 + 0.7 * hash(seed, gx, gz, 2)));
        return new Cell(gx, gz, cx, cz, Math.min(TIERS - 1, (int) (hash(seed, gx, gz, 3) * TIERS)));
    }

    /** Niveau de base d'un palier dans une region de niveaux [min, max]. */
    public static int tierLevel(int tier, int min, int max) {
        return min + Math.round((max - min) * (float) tier / (TIERS - 1));
    }

    /** Niveau d'un monstre qui apparait en (x, z) : celui de la cellule, a un niveau pres. */
    public static int level(long seed, int x, int z, int size, int min, int max, Random r) {
        int base = tierLevel(cell(seed, x, z, size).tier(), min, max);
        return Math.max(min, Math.min(max, base + r.nextInt(3) - 1));
    }

    private static double hash(long seed, int x, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L ^ seed;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) / (double) (1L << 53);
    }
}
