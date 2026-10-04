package com.mmorpg.entity;

/** Animations du personnage pendant le lancement d'une competence (appliquees au rendu du joueur). */
public enum CastAnim {
    /** Bras leve au-dessus de la tete (projectile lance, cri, rage). */
    RAISE,
    /** Deux bras tendus vers l'avant (canalisation). */
    CHANNEL,
    /** Posture de tir a l'arc. */
    AIM,
    /** Rotation complete du corps (tourbillon). */
    SPIN,
    /** Bras projete vers l'avant (coup de bouclier, charge). */
    BASH,
    /** Arme levee puis abattue sur le sol. */
    SLAM,
    /** Deux bras leves (bond, aura). */
    LEAP;

    public static CastAnim byId(int id) {
        CastAnim[] v = values();
        return id >= 0 && id < v.length ? v[id] : RAISE;
    }
}
