// Genere automatiquement par art/familiers_blockbench/generer_familiers.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.pet.anim;

/** Animations de Feu follet : indices, durees et instants de frappe (en ticks). */
public final class FeuFolletAnims {
    public static final int FLOTTEMENT = 0;
    public static final int DEPLACEMENT = 1;
    public static final int JOIE = 2;
    public static final int CANALISATION = 3;
    public static final int COUNT = 4;
    public static final int[] LENGTH_TICKS = {40, 20, 24, 32};
    public static final int[] STRIKE_TICKS = {0, 0, 0, 0};
    public static final boolean[] LOOPING = {true, true, false, true};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false, false};

    private FeuFolletAnims() {
    }
}
