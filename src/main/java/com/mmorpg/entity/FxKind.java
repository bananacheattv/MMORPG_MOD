package com.mmorpg.entity;

/** Effets visuels des competences, rendus cote client par {@code SkillFxRenderer}. */
public enum FxKind {
    /** Anneau au sol qui s'elargit et s'efface. */
    RING,
    /** Onde de choc : anneau + mur d'energie qui s'ecrase. */
    SHOCKWAVE,
    /** Cercle runique tournant au sol (incantation, zone ciblee). */
    CIRCLE,
    /** Colonne de lumiere. */
    PILLAR,
    /** Arc de lame devant le lanceur. */
    SLASH,
    /** Lames tournoyantes autour du lanceur. */
    WHIRL,
    /** Flammes en spirale autour du lanceur. */
    AURA,
    /** Bouclier spherique hexagonal. */
    DOME,
    /** Eclair qui saute d'une cible a l'autre. */
    CHAIN,
    /** Rayon droit entre deux points. */
    BEAM,
    /** Explosion : boule de lumiere, etincelles projetees, anneau. */
    BURST,
    /** Nova de glace : anneau et pics de glace qui jaillissent du sol. */
    NOVA,
    /** Fissures lumineuses rayonnant au sol. */
    CRACKS,
    /** Halo dore tournant aux pieds et au-dessus de la tete. */
    HALO,
    /** Implosion d'etincelles (teleportation). */
    BLINK,
    /** Marque runique au-dessus d'une creature. */
    MARK,
    /** Trainee de vitesse derriere le lanceur. */
    TRAIL,
    /** Bouclier spectral projete devant le lanceur. */
    SHIELD,
    /** Nuage d'orage tournant au-dessus du lanceur. */
    STORM;

    public static FxKind byId(int id) {
        FxKind[] v = values();
        return id >= 0 && id < v.length ? v[id] : RING;
    }
}
