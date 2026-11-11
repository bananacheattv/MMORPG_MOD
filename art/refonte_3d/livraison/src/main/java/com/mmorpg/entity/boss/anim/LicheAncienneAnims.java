// Genere automatiquement par art/refonte_3d/outils/construire_boss.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.entity.boss.anim;

/** Animations de Liche ancienne : indices, durees et instants de frappe (en ticks). */
public final class LicheAncienneAnims {
    public static final int REPOS = 0;
    public static final int DEPLACEMENT = 1;
    public static final int PROJECTILE = 2;
    public static final int INVOCATION = 3;
    public static final int CANALISATION = 4;
    public static final int RECUL = 5;
    public static final int MORT = 6;
    public static final int FRAPPE_BATON = 7;
    public static final int PLUIE_AMES = 8;
    public static final int RAYON = 9;
    public static final int COUNT = 10;
    public static final int[] LENGTH_TICKS = {60, 32, 26, 52, 40, 14, 64, 40, 56, 52};
    public static final int[] STRIKE_TICKS = {0, 0, 11, 22, 10, 0, 0, 18, 20, 19};
    public static final boolean[] LOOPING = {true, true, false, false, true, false, false, false, false, false};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false, false, false, false, true, false, false, false};

    private LicheAncienneAnims() {
    }
}
