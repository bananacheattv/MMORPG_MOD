// Genere automatiquement par art/familiers_blockbench/generer_familiers.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.pet.anim;

/** Animations de Chouette sage : indices, durees et instants de frappe (en ticks). */
public final class ChouetteSageAnims {
    public static final int REPOS = 0;
    public static final int CLIGNEMENT = 1;
    public static final int PETITS_PAS = 2;
    public static final int VOL = 3;
    public static final int DECOLLAGE = 4;
    public static final int ATTERRISSAGE = 5;
    public static final int SOMMEIL = 6;
    public static final int PERCHE = 7;
    public static final int COUNT = 8;
    public static final int[] LENGTH_TICKS = {60, 7, 16, 12, 18, 16, 60, 60};
    public static final int[] STRIKE_TICKS = {0, 0, 0, 0, 0, 0, 0, 0};
    public static final boolean[] LOOPING = {true, false, true, true, false, false, true, true};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false, false, false, false, false, false};

    private ChouetteSageAnims() {
    }
}
