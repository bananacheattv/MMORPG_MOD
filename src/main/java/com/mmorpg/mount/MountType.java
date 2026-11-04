package com.mmorpg.mount;

import com.mmorpg.rpg.Rarity;

/**
 * Montures 3D (modeles assets/mmorpg/bbmodels/mounts, generes par tools/gen_mounts.py).
 * Obtenues uniquement dans les caisses et Lucky Blocks (sceau de monture). L'Hippogriffe vole.
 */
public enum MountType {
    SANGLIER("sanglier", "Sanglier de guerre", Rarity.PEU_COMMUN, 1.0f, 1.4f, 1.26f, false),
    LOUP_GIVRE("loup_givre", "Loup de givre", Rarity.RARE, .9f, 1.5f, 1.39f, false),
    CERF_SYLVESTRE("cerf_sylvestre", "Cerf sylvestre", Rarity.RARE, .9f, 2.0f, 1.76f, false),
    TORTUE_CRISTAL("tortue_cristal", "Tortue de cristal", Rarity.RARE, 1.4f, 1.2f, 1.2f, false),
    CHEVRE_CELESTE("chevre_celeste", "Chèvre céleste", Rarity.RARE, .9f, 1.5f, 1.45f, false),
    LEZARD_LAVE("lezard_lave", "Lézard de lave", Rarity.EPIQUE, 1.1f, 1.0f, .95f, false),
    RAPTOR_SABLES("raptor_sables", "Raptor des sables", Rarity.EPIQUE, .9f, 1.6f, 1.39f, false),
    OURS_RUNIQUE("ours_runique", "Ours runique", Rarity.EPIQUE, 1.2f, 1.6f, 1.45f, false),
    SCORPION_DUNES("scorpion_dunes", "Scorpion des dunes", Rarity.EPIQUE, 1.4f, 1.0f, .9f, false),
    FELIN_VIDE("felin_vide", "Félin du vide", Rarity.LEGENDAIRE, .9f, 1.4f, 1.33f, false),
    ARAIGNEE_CAVERNES("araignee_cavernes", "Araignée des cavernes", Rarity.LEGENDAIRE, 1.4f, 1.1f, 1.2f, false),
    HIPPOGRIFFE("hippogriffe", "Hippogriffe", Rarity.MYTHIQUE, 1.0f, 1.6f, 1.6f, true),
    CHIMERE_ARENE("chimere_arene","Chimère des arènes",Rarity.MYTHIQUE,1.4f,2.6f,1.45f,false),
    // montures partagees : sieges {x, z} en blocs (z > 0 = vers l'avant), le premier siege est celui du proprietaire
    TAUREAU_GUERRE("taureau_guerre", "Taureau de guerre", Rarity.PEU_COMMUN, 1.0f, 1.9f, 1.69f, false, 0, .312f, 0, -.375f),
    LION_DORE("lion_dore", "Lion doré", Rarity.RARE, 1.0f, 1.95f, 1.63f, false, 0, .344f, 0, -.344f),
    RHINOCEROS_CUIRASSE("rhinoceros_cuirasse", "Rhinocéros cuirassé", Rarity.RARE, 1.3f, 1.85f, 1.63f, false, 0, .625f, 0, 0, 0, -.625f),
    BISON_GEANT("bison_geant", "Bison géant", Rarity.EPIQUE, 1.3f, 2.25f, 1.975f, false, 0, .562f, 0, -.031f, 0, -.625f),
    MAMMOUTH("mammouth", "Mammouth laineux", Rarity.EPIQUE, 1.6f, 2.66f, 2.44f, false, -.344f, .5f, .344f, .5f, -.344f, -.5f, .344f, -.5f),
    ELEPHANT_GUERRE("elephant_guerre", "Éléphant de guerre", Rarity.LEGENDAIRE, 1.6f, 3.4f, 2.57f, false, -.344f, .5f, .344f, .5f, -.344f, -.5f, .344f, -.5f),
    TORTUE_FORTERESSE("tortue_forteresse", "Tortue forteresse", Rarity.LEGENDAIRE, 1.9f, 2.75f, 1.69f, false, 0, .812f, -.375f, .188f, .375f, .188f, -.375f, -.5f, .375f, -.5f),
    DRAKE_CARAVANE("drake_caravane", "Drake de caravane", Rarity.MYTHIQUE, 1.6f, 3.06f, 2.0f, false, 0, .812f, -.344f, .188f, .344f, .188f, -.344f, -.5f, .344f, -.5f),
    DRAKE_ASCENDANT("drake_ascendant","Drake ascendant",Rarity.MYTHIQUE,1.0f,1.6f,1.6f,true);

    public final String id, label;
    public final Rarity rarity;
    public final double speed;
    public final float width, height, seat;
    public final boolean flies;
    /** Sieges {x, z} en blocs (avant mise a l'echelle) ; un seul siege pour les montures classiques. */
    public final float[][] seats;

    MountType(String id, String label, Rarity rarity, float width, float height, float seat, boolean flies, float... xz) {
        this.id = id; this.label = label; this.rarity = rarity;
        this.seats = new float[Math.max(1, xz.length / 2)][];
        if (xz.length < 2) this.seats[0] = new float[]{0, 0};
        for (int i = 0; i + 1 < xz.length; i += 2) this.seats[i / 2] = new float[]{xz[i], xz[i + 1]};
        this.speed = speedOf(rarity) * (1 - .03 * (seats.length - 1));      // plus la monture est chargee, plus elle est lente
        this.width = width; this.height = height; this.seat = seat; this.flies = flies;
    }

    /** Vitesse de deplacement : plus la monture est rare, plus elle va vite (chaque palier ajoute environ 18 % de la vitesse d'un joueur au sol). */
    public static double speedOf(Rarity rarity) {
        return switch (rarity) {
            case COMMUN -> .22;
            case PEU_COMMUN -> .26;
            case RARE -> .30;
            case EPIQUE -> .34;
            case LEGENDAIRE -> .38;
            case MYTHIQUE -> .42;
        };
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
