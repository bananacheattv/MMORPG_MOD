// Genere automatiquement par art/familiers_blockbench/generer_familiers.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.pet.anim;

/** Animations de Golem de poche : indices, durees et instants de frappe (en ticks). */
public final class GolemPocheAnims {
    public static final int REPOS = 0;
    public static final int MARCHE = 1;
    public static final int JOIE = 2;
    public static final int FRAPPE_SOL = 3;
    public static final int SOMMEIL = 4;
    public static final int COUNT = 5;
    public static final int[] LENGTH_TICKS = {60, 24, 28, 24, 80};
    public static final int[] STRIKE_TICKS = {0, 0, 0, 0, 0};
    public static final boolean[] LOOPING = {true, true, false, false, true};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false, false, false};

    private GolemPocheAnims() {
    }
}
