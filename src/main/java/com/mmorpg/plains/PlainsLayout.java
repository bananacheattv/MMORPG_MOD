package com.mmorpg.plains;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Random;

/**
 * Plan des Plaines d'Aeloria : une vaste etendue a ciel ouvert (360 x 480 blocs, dans la dimension des donjons) faite de collines douces,
 * de lacs et de bosquets, ceinte d'une falaise. On y entre par la place du nord ; six regions se suivent vers le sud, chacune avec son
 * decor et ses monstres, de plus en plus forts. Tout vient d'une graine : le constructeur et le generateur de monstres interrogent les
 * memes tableaux.
 */
public final class PlainsLayout {
    public static final int X0 = 30000, Z0 = 30000;       // coin nord-ouest (dimension des donjons)
    public static final int W = 360, L = 480;
    public static final int BASE = 70;                     // hauteur de la place d'entree et niveau de l'eau
    public static final int ENTRY = 26;                    // longueur de la place d'entree (les regions commencent apres)
    public static final int SECTORS = 6;
    public static final int SECTOR_LEN = (L - ENTRY) / SECTORS;
    public static final int PLAZA_X = W / 2, PLAZA_Z = 13;
    public static final int WALL = 3;                      // epaisseur de la falaise
    public static final int CLIFF = 15;                    // hauteur de la falaise au-dessus du niveau de base

    /** Une region : decor et monstres. */
    public record Sector(int index, String id, String name, String lore, int minLevel, int maxLevel, List<String> mobs,
                         Block[] top, Block sub, Block water, Block[] rock, int trees) {
    }

    public static final List<Sector> STYLES = List.of(
            new Sector(0, "plaines_prairies", "Prairies d'Aeloria", "Herbe haute et fleurs sauvages : gobelins et loups rôdent entre les chênes.", 1, 18,
                    List.of("gobelin", "mousseux", "sanglier_ronce", "archer_sylvestre", "loup_sombre"),
                    new Block[]{Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT},
                    Blocks.DIRT, Blocks.WATER, new Block[]{Blocks.STONE, Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE, Blocks.ANDESITE}, 0),
            new Sector(1, "plaines_marais", "Marais de Brumevert", "L'eau stagne entre les saules : tout ici mord, pique ou empoisonne.", 17, 34,
                    List.of("crapaud_venimeux", "limace_acide", "luciole_geante", "sorcier_roseaux", "araignee_venimeuse"),
                    new Block[]{Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.MUD, Blocks.MOSS_BLOCK, Blocks.PODZOL},
                    Blocks.DIRT, Blocks.WATER, new Block[]{Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_STONE_BRICKS, Blocks.STONE}, 1),
            new Sector(2, "plaines_os", "Landes des Ossements", "Des arbres morts et des os blanchis : ce champ de bataille n'a jamais été nettoyé.", 33, 50,
                    List.of("osselet", "araignee_os", "garde_funeraire", "spectre_enchaine", "bandit_arbaletrier"),
                    new Block[]{Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.GRAVEL},
                    Blocks.DIRT, Blocks.WATER, new Block[]{Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.TUFF, Blocks.BONE_BLOCK}, 2),
            new Sector(3, "plaines_givre", "Toundra de Givre", "La neige crisse sous les bottes et le vent porte des cris de harpies.", 49, 66,
                    List.of("renard_givre", "scarabee_polaire", "harpie_neiges", "golem_iceberg", "spectre_givre"),
                    new Block[]{Blocks.SNOW_BLOCK, Blocks.SNOW_BLOCK, Blocks.SNOW_BLOCK, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.STONE},
                    Blocks.DIRT, Blocks.ICE, new Block[]{Blocks.STONE, Blocks.PACKED_ICE, Blocks.CALCITE, Blocks.BLUE_ICE}, 3),
            new Sector(4, "plaines_cendres", "Terres Cendrées", "Le sable rouge brûle les semelles ; scorpions et djinns guettent derrière les dunes.", 65, 82,
                    List.of("scorpion_cuivre", "momie_dunes", "hyene_cendree", "djinn_braises", "tortue_volcanique"),
                    new Block[]{Blocks.RED_SAND, Blocks.RED_SAND, Blocks.RED_SAND, Blocks.COARSE_DIRT, Blocks.TERRACOTTA},
                    Blocks.RED_SANDSTONE, Blocks.WATER, new Block[]{Blocks.TERRACOTTA, Blocks.RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE, Blocks.BLACKSTONE}, 4),
            new Sector(5, "plaines_astre", "Étendue Astrale", "Le ciel semble tout proche : des cristaux chantent et le Néant rôde entre eux.", 81, 98,
                    List.of("mite_astrale", "rodeur_vide", "oeil_chaos", "chevalier_fracture", "devoreur_etoiles"),
                    new Block[]{Blocks.END_STONE, Blocks.END_STONE, Blocks.END_STONE, Blocks.CALCITE, Blocks.AMETHYST_BLOCK},
                    Blocks.END_STONE, Blocks.WATER, new Block[]{Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.PURPUR_BLOCK, Blocks.END_STONE_BRICKS}, 5));

    private static PlainsLayout instance;

    public static synchronized PlainsLayout get() {
        if (instance == null) instance = new PlainsLayout();
        return instance;
    }

    private final int[][] height = new int[W][L];

    private PlainsLayout() {
        for (int lx = 0; lx < W; lx++) {
            for (int lz = 0; lz < L; lz++) height[lx][lz] = compute(lx, lz);
        }
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

    /** Axe du chemin de terre qui descend de la place vers le sud (colonne locale x). */
    public static int pathX(int lz) {
        double g = Math.max(0, Math.min(1, (lz - PLAZA_Z - 8) / 50.0));              // le chemin part du centre de la place
        return (int) Math.round(PLAZA_X + g * (26 * Math.sin(lz / 41.0) + 11 * Math.sin(lz / 15.0 + 1.3)));
    }

    public static boolean onPath(int lx, int lz) {
        return lz >= PLAZA_Z + 9 && Math.abs(lx - pathX(lz)) <= 1;
    }

    private int compute(int lx, int lz) {
        if (cliff(lx, lz)) return BASE + CLIFF + (int) (hash(lx, lz, 9) * 3);
        double n = 0.62 * (noise(lx / 52.0, lz / 52.0, 1) * 2 - 1) + 0.28 * (noise(lx / 21.0, lz / 21.0, 2) * 2 - 1) + 0.10 * (noise(lx / 8.0, lz / 8.0, 3) * 2 - 1);
        int h = BASE + (int) Math.round(n * 8 + 2.4);
        double d = Math.hypot(lx - PLAZA_X, lz - PLAZA_Z);
        double f = Math.max(0, Math.min(1, (d - 17) / 24));                        // la place et ses abords sont plats
        h = BASE + (int) Math.round((h - BASE) * f);
        if (Math.abs(lx - pathX(lz)) <= 4 && lz > ENTRY) h = Math.max(h, BASE + 1);   // le chemin passe toujours a sec
        int e = edge(lx, lz);
        if (e < WALL + 5) h = Math.max(h, BASE + 1 + (int) ((WALL + 5 - e) * 0.6));  // le terrain remonte doucement vers la falaise
        return h;
    }

    // ------------------------------------------------------------------ regions

    /** Region de la colonne (les limites sont dentelees pour ne pas former de ligne droite). */
    public Sector sector(int lx, int lz) {
        int jz = lz + (int) (hash(lx, lz, 5) * 9) - 4;
        return STYLES.get(Math.max(0, Math.min(SECTORS - 1, (jz - ENTRY) / SECTOR_LEN)));
    }

    public static int sectorStart(int index) {
        return ENTRY + index * SECTOR_LEN;
    }

    public static int sectorEnd(int index) {
        return index == SECTORS - 1 ? L - 1 : ENTRY + (index + 1) * SECTOR_LEN - 1;
    }

    /** Niveau d'un monstre : monte regulierement du nord au sud de la region. */
    public int mobLevel(int lz, Random r) {
        int k = Math.max(0, Math.min(SECTORS - 1, (lz - ENTRY) / SECTOR_LEN));
        Sector s = STYLES.get(k);
        double t = Math.max(0, Math.min(1, (lz - sectorStart(k)) / (double) SECTOR_LEN));
        int lvl = (int) Math.round(s.minLevel() + (s.maxLevel() - s.minLevel()) * t) + r.nextInt(3) - 1;
        return Math.max(s.minLevel(), Math.min(s.maxLevel(), lvl));
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

    private static double noise(double x, double z, int salt) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = x - x0, fz = z - z0;
        double sx = fx * fx * (3 - 2 * fx), sz = fz * fz * (3 - 2 * fz);
        double a = hash(x0, z0, salt), b = hash(x0 + 1, z0, salt), c = hash(x0, z0 + 1, salt), d = hash(x0 + 1, z0 + 1, salt);
        return a + (b - a) * sx + (c - a) * sz + (a - b - c + d) * sx * sz;
    }
}
