// Genere automatiquement par art/boss_blockbench/generer_boss.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.entity.boss.anim;

/** Animations de Titan de glace : indices, durees et instants de frappe (en ticks). */
public final class TitanGlaceAnims {
    public static final int REVEIL = 0;
    public static final int REPOS = 1;
    public static final int MARCHE = 2;
    public static final int FRAPPE_POING = 3;
    public static final int DOUBLE_FRAPPE_SOL = 4;
    public static final int PIETINEMENT = 5;
    public static final int EXPOSITION_NOYAU = 6;
    public static final int MORT = 7;
    public static final int COUNT = 8;
    public static final int[] LENGTH_TICKS = {80, 80, 48, 44, 56, 44, 80, 92};
    public static final int[] STRIKE_TICKS = {0, 0, 0, 20, 26, 20, 20, 0};
    public static final boolean[] LOOPING = {false, true, true, false, false, false, false, false};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false, false, false, false, false, true};

    private TitanGlaceAnims() {
    }
}
