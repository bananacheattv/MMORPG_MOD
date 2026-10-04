// Genere automatiquement par art/familiers_blockbench/generer_familiers.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.pet.anim;

/** Animations de Phénix doré : indices, durees et instants de frappe (en ticks). */
public final class PhenixDoreAnims {
    public static final int REPOS = 0;
    public static final int VOL = 1;
    public static final int PLANE = 2;
    public static final int JOIE = 3;
    public static final int COUNT = 4;
    public static final int[] LENGTH_TICKS = {60, 16, 40, 36};
    public static final int[] STRIKE_TICKS = {0, 0, 0, 0};
    public static final boolean[] LOOPING = {true, true, true, false};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false, false};

    private PhenixDoreAnims() {
    }
}
