package com.mmorpg.leaderboard;

import java.util.Locale;

/** Un classement : identifiant (colonne {@code board}), titre du panneau, couleur d'accent et mise en forme de la valeur. */
public enum Board {
    NIVEAU("niveau", "Niveau", 0x3FA9F5, "Niv. ", ""),
    FORTUNE("fortune", "Fortune", 0xF2C94C, "", " po"),
    MONSTRES("monstres", "Chasseurs", 0xE5533D, "", " tués"),
    BOSS("boss", "Tueurs de boss", 0x9B51E0, "", " boss"),
    DONJONS("donjons", "Donjons", 0x3DBE7A, "", " victoires"),
    QUETES("quetes", "Quêtes", 0xF2994A, "", " quêtes"),
    PVP_VICTOIRES("pvp_victoires", "Arène : victoires", 0xE0457B, "", " victoires"),
    PVP_ELIMS("pvp_elims", "Arène : éliminations", 0xB02A2A, "", " élims"),
    GUILDES("guildes", "Guildes", 0x2DC4B2, "Niv. ", "");

    public static final int ROWS = 10;
    public final String id, title, prefix, suffix;
    public final int color;

    Board(String id, String title, int color, String prefix, String suffix) {
        this.id = id;
        this.title = title;
        this.color = color;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /** Identifiant du bloc (et de l'objet) du panneau. */
    public String blockId() {
        return "classement_" + id;
    }

    public String format(long value) {
        return prefix + String.format(Locale.ROOT, "%,d", value).replace(',', ' ') + suffix;
    }

    public static Board byId(String id) {
        for (Board b : values()) if (b.id.equals(id)) return b;
        return null;
    }
}
