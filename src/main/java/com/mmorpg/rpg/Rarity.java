package com.mmorpg.rpg;

/** Rarete des objets RPG (couleur du nom, cadre des icones). */
public enum Rarity {
    COMMUN("Commun", 0xFFC8C8C8),
    PEU_COMMUN("Peu commun", 0xFF5CE65C),
    RARE("Rare", 0xFF4FA3FF),
    EPIQUE("Épique", 0xFFB66CFF),
    LEGENDAIRE("Légendaire", 0xFFFF9C2A),
    MYTHIQUE("Mythique", 0xFFFF4D7A);

    public final String label;
    public final int color;

    Rarity(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public int rgb() {
        return color & 0xFFFFFF;
    }
}
