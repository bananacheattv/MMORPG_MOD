package com.mmorpg.mount;

import com.mmorpg.rpg.Rarity;

/**
 * Montures 3D (modeles assets/mmorpg/bbmodels/mounts, generes par tools/gen_mounts.py).
 * Obtenues uniquement dans les caisses et Lucky Blocks (sceau de monture). L'Hippogriffe vole.
 */
public enum MountType {
    SANGLIER("sanglier", "Sanglier de guerre", Rarity.PEU_COMMUN, .25, 1.0f, 1.4f, 1.26f, false),
    LOUP_GIVRE("loup_givre", "Loup de givre", Rarity.RARE, .30, .9f, 1.5f, 1.39f, false),
    CERF_SYLVESTRE("cerf_sylvestre", "Cerf sylvestre", Rarity.RARE, .30, .9f, 2.0f, 1.76f, false),
    TORTUE_CRISTAL("tortue_cristal", "Tortue de cristal", Rarity.RARE, .21, 1.4f, 1.2f, 1.2f, false),
    CHEVRE_CELESTE("chevre_celeste", "Chèvre céleste", Rarity.RARE, .28, .9f, 1.5f, 1.45f, false),
    LEZARD_LAVE("lezard_lave", "Lézard de lave", Rarity.EPIQUE, .31, 1.1f, 1.0f, .95f, false),
    RAPTOR_SABLES("raptor_sables", "Raptor des sables", Rarity.EPIQUE, .34, .9f, 1.6f, 1.39f, false),
    OURS_RUNIQUE("ours_runique", "Ours runique", Rarity.EPIQUE, .28, 1.2f, 1.6f, 1.45f, false),
    SCORPION_DUNES("scorpion_dunes", "Scorpion des dunes", Rarity.EPIQUE, .30, 1.4f, 1.0f, .9f, false),
    FELIN_VIDE("felin_vide", "Félin du vide", Rarity.LEGENDAIRE, .35, .9f, 1.4f, 1.33f, false),
    ARAIGNEE_CAVERNES("araignee_cavernes", "Araignée des cavernes", Rarity.LEGENDAIRE, .32, 1.4f, 1.1f, 1.2f, false),
    HIPPOGRIFFE("hippogriffe", "Hippogriffe", Rarity.MYTHIQUE, .32, 1.0f, 1.6f, 1.6f, true);

    public final String id, label;
    public final Rarity rarity;
    public final double speed;
    public final float width, height, seat;
    public final boolean flies;

    MountType(String id, String label, Rarity rarity, double speed, float width, float height, float seat, boolean flies) {
        this.id = id; this.label = label; this.rarity = rarity; this.speed = speed;
        this.width = width; this.height = height; this.seat = seat; this.flies = flies;
    }

    public static MountType byId(String id) {
        for (var t : values()) if (t.id.equals(id)) return t;
        return null;
    }

    /** Anciennes montures (chevaux) remplacees par les modeles 3D. */
    public static String migrate(String id) {
        return switch (id) {
            case "voyageur" -> SANGLIER.id;
            case "aube" -> CERF_SYLVESTRE.id;
            case "ombre" -> FELIN_VIDE.id;
            default -> id;
        };
    }
}
