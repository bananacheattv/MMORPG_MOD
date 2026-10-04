// Genere automatiquement par art/familiers_blockbench/generer_familiers.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.pet.anim;

/** Animations de Loup spectral : indices, durees et instants de frappe (en ticks). */
public final class LoupSpectralAnims {
    public static final int REPOS = 0;
    public static final int MARCHE = 1;
    public static final int COURSE = 2;
    public static final int ASSIS = 3;
    public static final int SOMMEIL = 4;
    public static final int QUEUE_JOYEUSE = 5;
    public static final int HURLEMENT = 6;
    public static final int MORSURE = 7;
    public static final int COUNT = 8;
    public static final int[] LENGTH_TICKS = {60, 20, 10, 60, 80, 10, 52, 16};
    public static final int[] STRIKE_TICKS = {0, 0, 0, 0, 0, 0, 0, 0};
    public static final boolean[] LOOPING = {true, true, true, true, true, true, false, false};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false, false, false, false, false, false};

    private LoupSpectralAnims() {
    }
}
