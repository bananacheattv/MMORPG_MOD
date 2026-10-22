// Genere automatiquement par art/mannequin_blockbench/generer_mannequin.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.entity.dummy.anim;

/** Animations de Mannequin d'entraînement : indices, durees et instants de frappe (en ticks). */
public final class MannequinBlindeAnims {
    public static final int IDLE = 0;
    public static final int HIT = 1;
    public static final int RESET = 2;
    public static final int COUNT = 3;
    public static final int[] LENGTH_TICKS = {60, 20, 24};
    public static final int[] STRIKE_TICKS = {0, 0, 0};
    public static final boolean[] LOOPING = {true, false, false};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false};

    private MannequinBlindeAnims() {
    }
}
