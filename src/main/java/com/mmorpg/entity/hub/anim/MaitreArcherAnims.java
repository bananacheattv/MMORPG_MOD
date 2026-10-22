// Genere automatiquement par art/classes_blockbench/generer_classes.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.entity.hub.anim;

/** Animations de Maître Archer : indices, durees et instants de frappe (en ticks). */
public final class MaitreArcherAnims {
    public static final int IDLE = 0;
    public static final int PRESENT = 1;
    public static final int COUNT = 2;
    public static final int[] LENGTH_TICKS = {72, 48};
    public static final int[] STRIKE_TICKS = {0, 0};
    public static final boolean[] LOOPING = {true, false};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false};

    private MaitreArcherAnims() {
    }
}
