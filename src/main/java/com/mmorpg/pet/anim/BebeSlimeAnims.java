// Genere automatiquement par art/familiers_blockbench/generer_familiers.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.pet.anim;

/** Animations de Bebe slime : indices, durees et instants de frappe (en ticks). */
public final class BebeSlimeAnims {
    public static final int REPOS = 0;
    public static final int PREPARATION_SAUT = 1;
    public static final int SAUT = 2;
    public static final int RECEPTION = 3;
    public static final int DOUBLE_REBOND = 4;
    public static final int REPOS_APLATI = 5;
    public static final int COUNT = 6;
    public static final int[] LENGTH_TICKS = {40, 8, 14, 12, 28, 48};
    public static final int[] STRIKE_TICKS = {0, 0, 0, 0, 0, 0};
    public static final boolean[] LOOPING = {true, false, false, false, false, true};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, true, false, false, false, false};

    private BebeSlimeAnims() {
    }
}
