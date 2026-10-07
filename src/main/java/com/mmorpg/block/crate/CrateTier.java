package com.mmorpg.block.crate;

/** Types de caisses (bloc pose par un admin) : chacune s'ouvre avec sa propre cle et a sa table de butin (config/mmorpg/butins.json). */
public enum CrateTier {
    VOTE("vote", "Caisse de vote", "Clé de vote", 0x50FF80),
    QUETE("quete", "Caisse de quête", "Clé de quête", 0xFFD040),
    COMMUNE("commune", "Caisse commune", "Clé commune", 0xC8C8C8),
    RARE("rare", "Caisse rare", "Clé rare", 0x4FA3FF),
    EPIQUE("epique", "Caisse épique", "Clé épique", 0xB66CFF),
    LEGENDAIRE("legendaire", "Caisse légendaire", "Clé légendaire", 0xFFA030),
    MYTHIQUE("mythique", "Caisse mythique", "Clé mythique", 0xFF3050);

    public final String id, label, keyLabel;
    public final int color;

    CrateTier(String id, String label, String keyLabel, int color) {
        this.id = id;
        this.label = label;
        this.keyLabel = keyLabel;
        this.color = color;
    }

    /** Identifiant de la table de butin. La caisse commune reprend la table historique « caisse_aventure ». */
    public String table() {
        return this == COMMUNE ? "caisse_aventure" : "caisse_" + id;
    }

    public String keyItem() {
        return "cle_" + id;
    }

    public String blockId() {
        return "caisse_" + id + "_bloc";
    }
}
