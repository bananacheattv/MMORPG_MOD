// Genere automatiquement par art/structures_blockbench/generer_structures.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.client.model.structure;

/** Animations de Téléporteur : indices, durees et instants de frappe (en ticks). */
public final class TeleporteurAnims {
    public static final int INACTIF = 0;
    public static final int ACTIVATION = 1;
    public static final int ACTIF = 2;
    public static final int COUNT = 3;
    public static final int[] LENGTH_TICKS = {60, 24, 40};
    public static final int[] STRIKE_TICKS = {0, 0, 0};
    public static final boolean[] LOOPING = {true, false, true};
    /** Animation qui reste figee sur sa derniere pose jusqu'a l'animation suivante. */
    public static final boolean[] HOLD = {false, false, false};

    private TeleporteurAnims() {
    }
}
