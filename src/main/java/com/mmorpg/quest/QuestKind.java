package com.mmorpg.quest;

/** Categorie visible d'une quete : principale, secondaire, reputation, journaliere, evenement. Couleurs/libelles communs serveur et client. */
public enum QuestKind {
    PRINCIPALE("principale", "PRINCIPALE", "Quête principale", "★", 0xFFFFB830),
    SECONDAIRE("secondaire", "SECONDAIRE", "Quête secondaire", "●", 0xFF6CB8F0),
    REPUTATION("reputation", "RÉPUTATION", "Quête de réputation", "♦", 0xFFC27CFF),
    JOURNALIERE("journaliere", "JOURNALIÈRE", "Quête journalière", "↻", 0xFF4FD8C4),
    EVENEMENT("evenement", "ÉVÉNEMENT", "Quête d'événement", "✦", 0xFFFF7A9C);

    public final String id, badge, label, glyph;
    /** Couleur ARGB (0xFFRRGGBB). */
    public final int color;

    QuestKind(String id, String badge, String label, String glyph, int color) {
        this.id = id;
        this.badge = badge;
        this.label = label;
        this.glyph = glyph;
        this.color = color;
    }

    public static QuestKind byId(String id) {
        for (QuestKind k : values()) if (k.id.equals(id)) return k;
        return SECONDAIRE;
    }

    public static QuestKind of(String questId, QuestDef q) {
        if (q == null) return SECONDAIRE;
        if (q.lore != null) {
            if ("principale".equals(q.lore.kind)) return PRINCIPALE;
            if ("evenement".equals(q.lore.kind)) return EVENEMENT;
            for (String e : q.lore.extras) if (e.toLowerCase(java.util.Locale.ROOT).contains("réputation")) return REPUTATION;
        }
        if (q.daily || q.weekly || !q.repeat.isEmpty()) return JOURNALIERE;
        if (questId != null && questId.startsWith("tuto_")) return PRINCIPALE;
        return SECONDAIRE;
    }

    /** Titre de notification : « QUÊTE PRINCIPALE ACCEPTÉE ». */
    public String notice(String what) {
        return label.toUpperCase(java.util.Locale.ROOT) + " " + what;
    }

    public int rgb() {
        return color & 0xFFFFFF;
    }
}
