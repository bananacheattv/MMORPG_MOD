// Genere automatiquement par art/boss_blockbench/generer_boss.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.entity.boss.anim;

/** Animations de Avatar du neant : indices, durees et instants de frappe (en ticks). */
public final class AvatarNeantAnims {
    public static final int LEVITATION = 0;
    public static final int DEPLACEMENT = 1;
    public static final int CHARGE_ENERGIE = 2;
    public static final int OUVERTURE_TORSE = 3;
    public static final int TELEPORTATION_PREPARATION = 4;
    public static final int REFORMATION = 5;
    public static final int MORT = 6;
    public static final int COUNT = 7;
    public static final int[] LENGTH_TICKS = {60, 40, 48, 52, 28, 32, 68};
    public static final int[] STRIKE_TICKS = {0, 0, 39, 24, 22, 9, 0};
    public static final boolean[] LOOPING = {true, true, false, false, false, false, false};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false, false, true, false, true};

    private AvatarNeantAnims() {
    }
}
