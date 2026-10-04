// Genere automatiquement par art/boss_blockbench/generer_boss.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.entity.boss.anim;

/** Animations de Roi gobelin : indices, durees et instants de frappe (en ticks). */
public final class RoiGobelinAnims {
    public static final int REPOS = 0;
    public static final int MARCHE = 1;
    public static final int COUP_HORIZONTAL = 2;
    public static final int FRAPPE_VERTICALE = 3;
    public static final int CRI_RALLIEMENT = 4;
    public static final int ETOURDI = 5;
    public static final int MORT = 6;
    public static final int RAGE_REPOS = 7;
    public static final int RAGE_MARCHE = 8;
    public static final int COUNT = 9;
    public static final int[] LENGTH_TICKS = {48, 24, 30, 34, 44, 40, 52, 24, 18};
    public static final int[] STRIKE_TICKS = {0, 0, 14, 16, 17, 0, 0, 0, 0};
    public static final boolean[] LOOPING = {true, true, false, false, false, true, false, true, true};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false, false, false, false, true, false, false};

    private RoiGobelinAnims() {
    }
}
