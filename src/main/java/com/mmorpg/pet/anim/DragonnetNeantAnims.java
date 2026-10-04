// Genere automatiquement par art/familiers_blockbench/generer_familiers.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.pet.anim;

/** Animations de Dragonnet du néant : indices, durees et instants de frappe (en ticks). */
public final class DragonnetNeantAnims {
    public static final int REPOS = 0;
    public static final int MARCHE = 1;
    public static final int VOL = 2;
    public static final int JOIE = 3;
    public static final int SOUFFLE = 4;
    public static final int COUNT = 5;
    public static final int[] LENGTH_TICKS = {60, 18, 14, 32, 32};
    public static final int[] STRIKE_TICKS = {0, 0, 0, 0, 0};
    public static final boolean[] LOOPING = {true, true, true, false, false};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false, false, false};

    private DragonnetNeantAnims() {
    }
}
