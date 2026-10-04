// Genere automatiquement par art/boss_blockbench/generer_boss.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.entity.boss.anim;

/** Animations de Ignis, seigneur demon : indices, durees et instants de frappe (en ticks). */
public final class IgnisAnims {
    public static final int REPOS = 0;
    public static final int MARCHE = 1;
    public static final int BALAYAGE = 2;
    public static final int FRAPPE_SOL = 3;
    public static final int SOUFFLE_FEU = 4;
    public static final int RUGISSEMENT = 5;
    public static final int MORT = 6;
    public static final int COUNT = 7;
    public static final int[] LENGTH_TICKS = {48, 32, 38, 46, 56, 48, 80};
    public static final int[] STRIKE_TICKS = {0, 0, 17, 20, 20, 17, 0};
    public static final boolean[] LOOPING = {true, true, false, false, false, false, false};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false, false, false, false, true};

    private IgnisAnims() {
    }
}
