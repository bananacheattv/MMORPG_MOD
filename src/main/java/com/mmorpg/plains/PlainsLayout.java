package com.mmorpg.plains;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Random;

/**
 * Plan des Plaines d'Aeloria : six grandes plaines a ciel ouvert (une par palier de niveau), chacune une carte de 300 x 330 blocs dans la
 * dimension des donjons (la region i commence en x = 40000 + i x 1000, z = 40000). On y entre par la place du nord ; collines douces, lacs, bosquets,
 * champs de cultures et zones de niveau y sont semes, et un portail cache, quelque part dans la moitie sud, mene a la plaine suivante : il
 * faut le trouver en se promenant. Tout vient d'une graine par region : le constructeur et le generateur de monstres interrogent les
 * memes tableaux.
 */
public final class PlainsLayout {
    public static final int X0 = 40000, Z0 = 40000, STEP = 1000;
    public static final int W = 300, L = 330;
    public static final int BASE = 70;                     // hauteur de la place d'entree et niveau de l'eau
    public static final int ENTRY = 26;                    // longueur de la place d'entree
    public static final int REGIONS = 6;
    public static final int PLAZA_X = W / 2, PLAZA_Z = 13;
    public static final int WALL = 3;                      // epaisseur de la falaise
    public static final int CLIFF = 15;                    // hauteur de la falaise au-dessus du niveau de base
    public static final int ZONE_SIZE = 52;                // pas de la grille des zones de niveau

    /** Une plaine : decor, monstres et cultures. */
    public record Sector(int index, String id, String name, String lore, int minLevel, int maxLevel, List<String> mobs,
                         Block[] top, Block sub, Block water, Block[] rock, String cropA, String cropB) {
    }

    public static final List<Sector> STYLES = List.of(
            new Sector(0, "plaines_prairies", "Prairies d'Aeloria", "Herbe haute et fleurs sauvages : gobelins et loups rôdent entre les chênes.", 1, 18,
                    List.of("gobelin", "mousseux", "sanglier_ronce", "archer_sylvestre", "loup_sombre"),
                    new Block[]{Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT},
                    Blocks.DIRT, Blocks.WATER, new Block[]{Blocks.STONE, Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE, Blocks.ANDESITE}, "lueur", "lueur"),
            new Sector(1, "plaines_marais", "Marais de Brumevert", "L'eau stagne entre les saules : tout ici mord, pique ou empoisonne.", 17, 34,
                    List.of("crapaud_venimeux", "limace_acide", "luciole_geante", "sorcier_roseaux", "araignee_venimeuse"),
                    new Block[]{Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.MUD, Blocks.MOSS_BLOCK, Blocks.PODZOL},
                    Blocks.DIRT, Blocks.WATER, new Block[]{Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_STONE_BRICKS, Blocks.STONE}, "lueur", "argent"),
            new Sector(2, "plaines_os", "Landes des Ossements", "Des arbres morts et des os blanchis : ce champ de bataille n'a jamais été nettoyé.", 33, 50,
                    List.of("osselet", "araignee_os", "garde_funeraire", "spectre_enchaine", "bandit_arbaletrier"),
                    new Block[]{Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.GRAVEL},
                    Blocks.DIRT, Blocks.WATER, new Block[]{Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.TUFF, Blocks.BONE_BLOCK}, "argent", "ambre"),
            new Sector(3, "plaines_givre", "Toundra de Givre", "La neige crisse sous les bottes et le vent porte des cris de harpies.", 49, 66,
                    List.of("renard_givre", "scarabee_polaire", "harpie_neiges", "golem_iceberg", "spectre_givre"),
                    new Block[]{Blocks.SNOW_BLOCK, Blocks.SNOW_BLOCK, Blocks.SNOW_BLOCK, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.STONE},
                    Blocks.DIRT, Blocks.ICE, new Block[]{Blocks.STONE, Blocks.PACKED_ICE, Blocks.CALCITE, Blocks.BLUE_ICE}, "ambre", "givre"),
            new Sector(4, "plaines_cendres", "Terres Cendrées", "Le sable rouge brûle les semelles ; scorpions et djinns guettent derrière les dunes.", 65, 82,
                    List.of("scorpion_cuivre", "momie_dunes", "hyene_cendree", "djinn_braises", "tortue_volcanique"),
                    new Block[]{Blocks.RED_SAND, Blocks.RED_SAND, Blocks.RED_SAND, Blocks.COARSE_DIRT, Blocks.TERRACOTTA},
                    Blocks.RED_SANDSTONE, Blocks.WATER, new Block[]{Blocks.TERRACOTTA, Blocks.RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE, Blocks.BLACKSTONE}, "givre", "neant"),
            new Sector(5, "plaines_astre", "Étendue Astrale", "Le ciel semble tout proche : des cristaux chantent et le Néant rôde entre eux.", 81, 98,
                    List.of("mite_astrale", "rodeur_vide", "oeil_chaos", "chevalier_fracture", "devoreur_etoiles"),
                    new Block[]{Blocks.END_STONE, Blocks.END_STONE, Blocks.END_STONE, Blocks.CALCITE, Blocks.AMETHYST_BLOCK},
                    Blocks.END_STONE, Blocks.WATER, new Block[]{Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.PURPUR_BLOCK, Blocks.END_STONE_BRICKS}, "neant", "neant"));

    private static final PlainsLayout[] INSTANCES = new PlainsLayout[REGIONS];

    public static synchronized PlainsLayout get(int region) {
        if (INSTANCES[region] == null) INSTANCES[region] = new PlainsLayout(region);
        return INSTANCES[region];
    }

    public final int region;
    public final Sector style;
    /** Position (colonne locale) du portail cache vers la plaine suivante. */
    public final int exitX, exitZ;
    private final int[][] height = new int[W][L];

    private PlainsLayout(int region) {
        this.region = region;
        this.style = STYLES.get(region);
        Random r = new Random(0xAE10A1AL * 31 + region);
        this.exitX = 55 + r.nextInt(W - 110);
        this.exitZ = L - 95 + r.nextInt(40);
        for (int lx = 0; lx < W; lx++) {
            for (int lz = 0; lz < L; lz++) height[lx][lz] = compute(lx, lz);
        }
    }

    // ------------------------------------------------------------------ regions dans le monde

    public int x0() {
        return X0 + region * STEP;
    }

    /** Region qui contient la colonne du monde (x, z), ou -1. */
    public static int regionAt(double x, double z) {
        if (z < Z0 - 4 || z > Z0 + L + 4 || x < X0 - 4) return -1;
        int r = (int) Math.floor((x - X0 + 4) / STEP);
        if (r < 0 || r >= REGIONS || x > X0 + r * STEP + W + 4) return -1;
        return r;
    }

    // ------------------------------------------------------------------ terrain

    /** Hauteur du sol (premier bloc plein) a la colonne locale. */
    public int height(int lx, int lz) {
        return height[lx][lz];
    }

    public static boolean inside(int lx, int lz) {
        return lx >= 0 && lz >= 0 && lx < W && lz < L;
    }

    /** Distance au bord du cadre (0 = colonne du bord). */
    public static int edge(int lx, int lz) {
        return Math.min(Math.min(lx, W - 1 - lx), Math.min(lz, L - 1 - lz));
    }

    public static boolean cliff(int lx, int lz) {
        return edge(lx, lz) < WALL;
    }

    /** Axe du chemin de terre qui descend de la place vers le sud (colonne locale x) ; il s'arrete avant le portail cache. */
    public int pathX(int lz) {
        double g = Math.max(0, Math.min(1, (lz - PLAZA_Z - 8) / 50.0));              // le chemin part du centre de la place
        double s = region * 1.7;
        return (int) Math.round(PLAZA_X + g * (24 * Math.sin(lz / 41.0 + s) + 10 * Math.sin(lz / 15.0 + 1.3 + s)));
    }

    public boolean onPath(int lx, int lz) {
        return lz >= PLAZA_Z + 9 && lz < L - 120 && Math.abs(lx - pathX(lz)) <= 1;
    }

    private int compute(int lx, int lz) {
        if (cliff(lx, lz)) return BASE + CLIFF + (int) (hash(lx, lz, 9) * 3);
        double n = 0.62 * (noise(lx / 52.0, lz / 52.0, 1) * 2 - 1) + 0.28 * (noise(lx / 21.0, lz / 21.0, 2) * 2 - 1) + 0.10 * (noise(lx / 8.0, lz / 8.0, 3) * 2 - 1);
        int h = BASE + (int) Math.round(n * 8 + 2.4);
        double d = Math.hypot(lx - PLAZA_X, lz - PLAZA_Z);
        double f = Math.max(0, Math.min(1, (d - 17) / 24));                        // la place et ses abords sont plats
        h = BASE + (int) Math.round((h - BASE) * f);
        if (lz < L - 120 && Math.abs(lx - pathX(lz)) <= 4 && lz > ENTRY) h = Math.max(h, BASE + 1);   // le chemin passe toujours a sec
        double de = Math.hypot(lx - exitX, lz - exitZ);                            // le sanctuaire du portail est sur un replat sec
        if (de < 22) h = BASE + 2 + (int) Math.round((h - BASE - 2) * Math.max(0, (de - 11) / 11));
        int e = edge(lx, lz);
        if (e < WALL + 5) h = Math.max(h, BASE + 1 + (int) ((WALL + 5 - e) * 0.6));  // le terrain remonte doucement vers la falaise
        return h;
    }

    // ------------------------------------------------------------------ niveaux

    /** Niveau d'un monstre : la zone (cellule irreguliere) donne le palier dans la fourchette de la plaine. */
    public int mobLevel(int lx, int lz, Random r) {
        return com.mmorpg.world.LevelZones.level(0xAE10A1AL + region, lx, lz, ZONE_SIZE, style.minLevel(), style.maxLevel(), r);
    }

    /** Zone de niveau de cette colonne. */
    public com.mmorpg.world.LevelZones.Cell zone(int lx, int lz) {
        return com.mmorpg.world.LevelZones.cell(0xAE10A1AL + region, lx, lz, ZONE_SIZE);
    }

    public Sector sector(int lx, int lz) {
        return style;
    }

    // ------------------------------------------------------------------ bruit

    /** Nombre pseudo-aleatoire stable dans [0, 1) pour une cellule. */
    public static double hash(int x, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L ^ 0xAE10A1AL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) / (double) (1L << 53);
    }

    private double noise(double x, double z, int salt) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = x - x0, fz = z - z0;
        double sx = fx * fx * (3 - 2 * fx), sz = fz * fz * (3 - 2 * fz);
        int s = salt + region * 101;
        double a = hash(x0, z0, s), b = hash(x0 + 1, z0, s), c = hash(x0, z0 + 1, s), d = hash(x0 + 1, z0 + 1, s);
        return a + (b - a) * sx + (c - a) * sz + (a - b - c + d) * sx * sz;
    }
}
