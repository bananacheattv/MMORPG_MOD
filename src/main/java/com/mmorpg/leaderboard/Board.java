package com.mmorpg.leaderboard;

/** Statistique de joueur classee (colonne {@code board} de la table {@code leaderboard_stats}). */
public enum Board {
    NIVEAU("niveau"), FORTUNE("fortune"), MONSTRES("monstres"), BOSS("boss"), DONJONS("donjons"), QUETES("quetes"),
    /** Valeur : victoires ; bonus : eliminations. */
    ARENE("arene");

    public static Board byId(String id) {
        for (Board b : values()) if (b.id.equals(id)) return b;
        return null;
    }

    public final String id;

    Board(String id) {
        this.id = id;
    }
}
