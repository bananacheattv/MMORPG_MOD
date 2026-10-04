package com.mmorpg.rpg;

/** Effets temporaires appliques par les competences. */
public enum BuffType {
    WAR_CRY("Cri de Guerre", "cri_de_guerre", 0xFFFF8040),
    RAGE("Rage Sanguinaire", "rage_sanguinaire", 0xFFE01818),
    ARCANE_GUARD("Garde Arcanique", "teleportation_arcanique", 0xFFC080FF),
    MANA_SHIELD("Bouclier de Mana", "bouclier_de_mana", 0xFF60A0FF),
    AGILITY("Élan Acrobatique", "saut_arriere", 0xFF80FF80),
    TAUNT("Provocation", "provocation", 0xFFFF6040),
    FORTRESS("Forteresse", "forteresse", 0xFFB0B8C8),
    SACRED_AURA("Aura Sacrée", "aura_sacree", 0xFFFFE080),
    TITAN("Rempart du Titan", "rempart_du_titan", 0xFFFFD040);

    public final String label;
    public final String icon;
    public final int color;

    BuffType(String label, String icon, int color) {
        this.label = label;
        this.icon = icon;
        this.color = color;
    }
}
