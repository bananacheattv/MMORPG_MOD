package com.mmorpg.ranking;

/** Types de classements : chacun a son panneau 3D (bloc « classement_<id> ») et sa colonne dans le registre partagé. */
public enum RankingKind {
    NIVEAU("niveau", "Niveaux des aventuriers", false, 0xFFD040),
    MONSTRES("monstres", "Chasseurs de monstres", false, 0xFF6A5A),
    ARENE("arene", "Gladiateurs de l’arène", false, 0x4FA3FF),
    FORTUNE("fortune", "Fortunes d’Aeloria", false, 0xF2C94C),
    BOSS("boss", "Tueurs de boss", false, 0xB66CFF),
    DONJONS("donjons", "Conquérants de donjons", false, 0x3DBE7A),
    QUETES("quetes", "Héros des quêtes", false, 0xF2994A),
    GUILDE_NIVEAU("guilde_niveau", "Guildes · niveau", true, 0xB66CFF),
    GUILDE_BANQUE("guilde_banque", "Guildes · fortune", true, 0xFFA030),
    GUILDE_GARDIENS("guilde_gardiens", "Guildes · gardiens vaincus", true, 0x50FF80);

    public final String id, label;
    public final boolean guild;
    public final int color;

    RankingKind(String id, String label, boolean guild, int color) {
        this.id = id;
        this.label = label;
        this.guild = guild;
        this.color = color;
    }

    /** Statistique de joueur correspondante (null pour les classements de guildes). */
    public com.mmorpg.leaderboard.Board board() {
        return guild ? null : com.mmorpg.leaderboard.Board.byId(id);
    }

    public String blockId() {
        return "classement_" + id;
    }

    public static RankingKind byId(String id) {
        for (var k : values()) if (k.id.equals(id)) return k;
        return null;
    }
}
