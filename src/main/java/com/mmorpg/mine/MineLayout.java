package com.mmorpg.mine;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plan des Mines d'Aeloria : une immense caverne d'environ 430 x 430 blocs de large et 100 de haut, dont le sol descend de pres de
 * 60 blocs depuis la place d'entree (au nord) jusqu'a l'abime (au sud). Tout est calcule a partir d'une graine : le constructeur
 * ({@link MineBuilder}) et le generateur de monstres ({@link MineManager}) interrogent les memes tableaux, sans rien stocker dans le monde.
 *
 * <p>Cinq etages se suivent du nord au sud, chacun avec sa roche, son minerai, sa lumiere et ses monstres.
 */
public final class MineLayout {
    public static final int X0 = 22000, Z0 = 22000, STEP = 1000;      // coin nord-ouest de la premiere mine ; la suivante est STEP blocs plus a l'est
    public static final int Y0 = 12;                      // fond de la mine
    public static final int SIZE = 320;                   // largeur et longueur du cadre
    public static final int HEIGHT = 100;
    public static final int PLAZA_X = SIZE / 2, PLAZA_Z = 34;
    public static final int WALL = 4;                     // epaisseur de la paroi au bord de la caverne
    public static final long SEED = 0xAE10A1AL;

    /** Un etage de la mine. */
    public record Sector(int index, String id, String name, String lore, int minLevel, int maxLevel, String ore, List<String> mobs,
                         Block[] floorTop, Block[] floorSub, Block[] wall, Block[] ceiling, Block light, Block liquid, Block[] hanging, Block[] plants) {
    }

    public static final int SECTORS = 5;

    public static final List<Sector> STYLES = List.of(
            new Sector(0, "mine_verdelune", "Veines de Verdelune", "Une mousse tiède recouvre la roche… quelque chose brille dans l'ombre.", 1, 20, "verdite",
                    List.of("gobelin", "loup_sombre", "zombie_des_cryptes"),
                    new Block[]{Blocks.MOSS_BLOCK, Blocks.MOSS_BLOCK, Blocks.STONE, Blocks.MOSSY_COBBLESTONE, Blocks.COARSE_DIRT, Blocks.ANDESITE},
                    new Block[]{Blocks.STONE, Blocks.ANDESITE, Blocks.COBBLESTONE}, new Block[]{Blocks.STONE, Blocks.STONE, Blocks.MOSSY_COBBLESTONE, Blocks.ANDESITE, Blocks.MOSS_BLOCK},
                    new Block[]{Blocks.STONE, Blocks.TUFF, Blocks.MOSS_BLOCK, Blocks.ANDESITE}, Blocks.SHROOMLIGHT, Blocks.WATER,
                    new Block[]{Blocks.SPORE_BLOSSOM, Blocks.SPORE_BLOSSOM}, new Block[]{Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.AZALEA, Blocks.FLOWERING_AZALEA, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM, Blocks.FERN}),
            new Sector(1, "mine_argentite", "Galeries d'Argentite", "Les parois d'ardoise reflètent une lueur d'argent.", 18, 38, "argentite",
                    List.of("squelette_maudit", "araignee_venimeuse", "zombie_des_cryptes"),
                    new Block[]{Blocks.DEEPSLATE, Blocks.DEEPSLATE, Blocks.TUFF, Blocks.COBBLED_DEEPSLATE, Blocks.SMOOTH_BASALT},
                    new Block[]{Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE}, new Block[]{Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.TUFF, Blocks.CALCITE, Blocks.DEEPSLATE_BRICKS},
                    new Block[]{Blocks.DEEPSLATE, Blocks.TUFF, Blocks.COBBLED_DEEPSLATE}, Blocks.SEA_LANTERN, Blocks.WATER,
                    new Block[]{Blocks.POINTED_DRIPSTONE}, new Block[]{Blocks.AMETHYST_CLUSTER, Blocks.SMALL_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD}),
            new Sector(2, "mine_ambrasite", "Forges d'Ambrasite", "L'air est lourd : la roche elle-même semble couver une braise.", 35, 58, "ambrasite",
                    List.of("orc_guerrier", "bandit_arbaletrier", "elementaire_feu"),
                    new Block[]{Blocks.BLACKSTONE, Blocks.BLACKSTONE, Blocks.BASALT, Blocks.MAGMA_BLOCK, Blocks.POLISHED_BLACKSTONE, Blocks.NETHERRACK},
                    new Block[]{Blocks.BLACKSTONE, Blocks.BASALT}, new Block[]{Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.BASALT, Blocks.GILDED_BLACKSTONE, Blocks.NETHER_BRICKS},
                    new Block[]{Blocks.BLACKSTONE, Blocks.BASALT, Blocks.NETHERRACK}, Blocks.SHROOMLIGHT, Blocks.LAVA,
                    new Block[]{Blocks.POINTED_DRIPSTONE}, new Block[]{Blocks.CRIMSON_ROOTS, Blocks.NETHER_SPROUTS}),
            new Sector(3, "mine_glacium", "Abysses de Glacium", "Un froid mordant fige la roche : la glace chante sous vos pas.", 55, 75, "glacium",
                    List.of("spectre_givre", "golem_cristal", "squelette_maudit"),
                    new Block[]{Blocks.SNOW_BLOCK, Blocks.SNOW_BLOCK, Blocks.CALCITE, Blocks.STONE, Blocks.PACKED_ICE},
                    new Block[]{Blocks.STONE, Blocks.PACKED_ICE, Blocks.CALCITE}, new Block[]{Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.SNOW_BLOCK, Blocks.STONE, Blocks.CALCITE},
                    new Block[]{Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.STONE}, Blocks.SEA_LANTERN, Blocks.WATER,
                    new Block[]{Blocks.PACKED_ICE, Blocks.BLUE_ICE}, new Block[]{Blocks.SNOW, Blocks.SMALL_AMETHYST_BUD}),
            new Sector(4, "mine_neantite", "Gouffre de Néantite", "Le silence est total. Le Néant respire entre les cristaux.", 72, 100, "neantite",
                    List.of("chevalier_neant", "golem_cristal"),
                    new Block[]{Blocks.OBSIDIAN, Blocks.END_STONE_BRICKS, Blocks.END_STONE, Blocks.CRYING_OBSIDIAN, Blocks.PURPUR_BLOCK, Blocks.BLACKSTONE},
                    new Block[]{Blocks.OBSIDIAN, Blocks.END_STONE}, new Block[]{Blocks.OBSIDIAN, Blocks.END_STONE_BRICKS, Blocks.PURPUR_PILLAR, Blocks.AMETHYST_BLOCK, Blocks.CRYING_OBSIDIAN},
                    new Block[]{Blocks.OBSIDIAN, Blocks.END_STONE, Blocks.AMETHYST_BLOCK}, Blocks.CRYING_OBSIDIAN, Blocks.AIR,
                    new Block[]{Blocks.AMETHYST_CLUSTER, Blocks.END_ROD}, new Block[]{Blocks.AMETHYST_CLUSTER, Blocks.LARGE_AMETHYST_BUD, Blocks.END_ROD}));

    /** Pilier naturel, bassin ou gouffre. */
    public record Feature(int x, int z, int r, int depth) {
    }

    private static final MineLayout[] INSTANCES = new MineLayout[SECTORS];

    /** Plan de la mine numero {@code region} (0 a 4) : chaque palier de niveau a sa propre mine. */
    public static synchronized MineLayout get(int region) {
        if (INSTANCES[region] == null) INSTANCES[region] = new MineLayout(region);
        return INSTANCES[region];
    }

    public final int region;
    public final Sector style;
    /** Position (colonne locale) du portail cache vers la mine suivante, dans un recoin au fond de la caverne. */
    public int exitX, exitZ;
    private final int regionSalt;

    public int x0() {
        return X0 + region * STEP;
    }

    /** Mine qui contient la colonne du monde (x, z), ou -1. */
    public static int regionAt(double x, double z) {
        if (z < Z0 - 8 || z > Z0 + SIZE + 8 || x < X0 - 8) return -1;
        int r = (int) Math.floor((x - X0 + 8) / STEP);
        if (r < 0 || r >= SECTORS || x > X0 + r * STEP + SIZE + 8) return -1;
        return r;
    }

    /** 0 = vide, 1 = interieur de la caverne, 2 = paroi. */
    private final byte[][] kind = new byte[SIZE][SIZE];
    private final int[][] floor = new int[SIZE][SIZE];
    private final int[][] ceil = new int[SIZE][SIZE];
    /** Niveau du liquide d'un bassin (-1 = pas de liquide). */
    private final int[][] liquid = new int[SIZE][SIZE];
    private final boolean[][] pillar = new boolean[SIZE][SIZE];
    public final List<Feature> pillars = new ArrayList<>();
    public final List<Feature> pools = new ArrayList<>();
    public final List<Feature> chasms = new ArrayList<>();

    private MineLayout(int region) {
        this.region = region;
        this.style = STYLES.get(region);
        this.regionSalt = region * 1013;
        Random r = new Random(SEED + region * 7919L);
        // ---- emprise : une ellipse irreguliere, un peu plus de 420 blocs dans les deux sens
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double dx = (x - SIZE / 2.0) / (SIZE / 2.0 - WALL - 3), dz = (z - SIZE / 2.0) / (SIZE / 2.0 - WALL - 3);
                double d = Math.hypot(dx, dz) + 0.06 * fbm(x / 38.0, z / 38.0, 11, 3) + 0.03 * fbm(x / 12.0, z / 12.0, 12, 2);
                boolean border = x < WALL + 2 || z < WALL + 2 || x >= SIZE - WALL - 2 || z >= SIZE - WALL - 2;      // la paroi doit tenir dans le cadre
                kind[x][z] = (byte) (d < 1.0 && !border ? 1 : 0);
            }
        }
        // l'entree : un couloir large prolonge la caverne vers le bord nord
        for (int x = PLAZA_X - 20; x <= PLAZA_X + 20; x++) {
            for (int z = WALL + 2; z < 60; z++) kind[x][z] = 1;
        }
        // parois : toute cellule vide a moins de WALL cellules d'une cellule interieure
        byte[][] walls = new byte[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                if (kind[x][z] == 1) continue;
                boolean near = false;
                for (int dx = -WALL; dx <= WALL && !near; dx++) {
                    for (int dz = -WALL; dz <= WALL && !near; dz++) {
                        int nx = x + dx, nz = z + dz;
                        if (nx >= 0 && nz >= 0 && nx < SIZE && nz < SIZE && kind[nx][nz] == 1 && dx * dx + dz * dz <= WALL * WALL) near = true;
                    }
                }
                if (near) walls[x][z] = 2;
            }
        }
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                if (walls[x][z] == 2) kind[x][z] = 2;
            }
        }

        // ---- sol de base : il descend du nord au sud
        double[][] h = new double[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double t = Mth.clamp((z - 20) / (double) (SIZE - 70), 0, 1);
                double slope = 1 - smooth(smooth(t));                       // plateau a l'entree, pente reguliere, fond plat
                double base = 12 + 62 * slope;
                double hills = 15.0 * fbm(x / 72.0, z / 72.0, 21, 3) + 7.0 * fbm(x / 27.0, z / 27.0, 22, 3) + 2.4 * fbm(x / 9.0, z / 9.0, 23, 2);
                double ridge = 1 - 2 * Math.abs(fbm(x / 58.0, z / 58.0, 24, 2));                 // cretes (valeurs hautes) et vallees (basses)
                double valley = Math.max(0, 1 - 5 * Math.abs(fbm(x / 46.0, z / 46.0, 25, 2)));  // couloirs creuses
                double calm = smooth(Mth.clamp((z - 30) / 70.0, 0, 1));    // pres de l'entree le relief s'attenue
                h[x][z] = base + (hills + 6.5 * ridge - 7.0 * valley) * calm;
            }
        }

        // pente maximale d'un bloc par case dans les quatre directions : trois passes aller-retour
        for (int pass = 0; pass < 3; pass++) {
            for (int x = 1; x < SIZE; x++) for (int z = 1; z < SIZE; z++) h[x][z] = Mth.clamp(h[x][z], Math.max(h[x - 1][z], h[x][z - 1]) - 1.0, Math.min(h[x - 1][z], h[x][z - 1]) + 1.0);
            for (int x = SIZE - 2; x >= 0; x--) for (int z = SIZE - 2; z >= 0; z--) h[x][z] = Mth.clamp(h[x][z], Math.max(h[x + 1][z], h[x][z + 1]) - 1.0, Math.min(h[x + 1][z], h[x][z + 1]) + 1.0);
        }

        // ---- bassins (de l'eau ou de la lave selon l'etage) : le sol est aplati puis creuse en cuvette
        for (int i = 0; i < 14; i++) {
            Feature f = place(r, 8 + r.nextInt(8), 5, 0);
            if (f == null) continue;
            if (style.index() == 4 && r.nextInt(3) != 0) continue;
            pools.add(f);
        }
        for (Feature f : pools) {
            double level = Math.round(h[f.x()][f.z()]);
            for (int x = Math.max(0, f.x() - f.r() - 10); x < Math.min(SIZE, f.x() + f.r() + 10); x++) {
                for (int z = Math.max(0, f.z() - f.r() - 10); z < Math.min(SIZE, f.z() + f.r() + 10); z++) {
                    double d = Math.hypot(x - f.x(), z - f.z());
                    double w = 1 - smooth(Mth.clamp((d - f.r()) / 9.0, 0, 1));
                    h[x][z] = Mth.lerp(w, h[x][z], level);
                    if (d < f.r()) {
                        double bowl = 4.5 * (1 - (d * d) / (f.r() * (double) f.r()));
                        h[x][z] = level - bowl;
                        liquid[x][z] = (int) level - 1;
                    }
                }
            }
        }
        // ---- plateforme de l'entree
        double plazaY = Math.round(h[PLAZA_X][PLAZA_Z]);
        for (int x = PLAZA_X - 40; x <= PLAZA_X + 40; x++) {
            for (int z = PLAZA_Z - 40; z <= PLAZA_Z + 40; z++) {
                if (x < 0 || z < 0 || x >= SIZE || z >= SIZE) continue;
                double d = Math.hypot(x - PLAZA_X, z - PLAZA_Z);
                double w = 1 - smooth(Mth.clamp((d - 16) / 24.0, 0, 1));
                h[x][z] = Mth.lerp(w, h[x][z], plazaY);
                liquid[x][z] = w > 0.01 ? 0 : liquid[x][z];
            }
        }
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                if (liquid[x][z] == 0 && Math.hypot(x - PLAZA_X, z - PLAZA_Z) < 40) liquid[x][z] = 0;
            }
        }
        // ---- gouffres : falaises verticales a descendre par des echelles
        for (int i = 0; i < 4; i++) {
            Feature f = place(r, 9 + r.nextInt(8), 8, 16 + r.nextInt(14));
            if (f == null || f.z() < 110) continue;
            chasms.add(f);
            for (int x = f.x() - f.r() - 1; x <= f.x() + f.r() + 1; x++) {
                for (int z = f.z() - f.r() - 1; z <= f.z() + f.r() + 1; z++) {
                    if (x < 0 || z < 0 || x >= SIZE || z >= SIZE) continue;
                    double d = Math.hypot(x - f.x(), z - f.z());
                    if (d < f.r() + 0.3 * fbm(x / 6.0, z / 6.0, 31, 2)) h[x][z] -= f.depth() - 2.2 * (1 - d / f.r());
                }
            }
        }
        // ---- recoin du portail cache : un replat sec dans la moitie sud, loin de la place, des bassins et des gouffres
        pickExit(r, h);
        double exitY = Math.round(h[exitX][exitZ]);
        for (int x = exitX - 11; x <= exitX + 11; x++) {
            for (int z = exitZ - 11; z <= exitZ + 11; z++) {
                double d = Math.hypot(x - exitX, z - exitZ);
                double w = 1 - smooth(Mth.clamp((d - 7) / 4.0, 0, 1));
                h[x][z] = Mth.lerp(w, h[x][z], exitY);
                if (d < 12) liquid[x][z] = 0;
            }
        }
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                floor[x][z] = Y0 + (int) Math.round(h[x][z]);
                double top = Y0 + 96 + 3.0 * fbm(x / 90.0, z / 90.0, 41, 2) - 3.5 * Math.abs(fbm(x / 33.0, z / 33.0, 42, 2));
                ceil[x][z] = (int) Math.max(Math.min(top, Y0 + HEIGHT - 4), floor[x][z] + 20);
            }
        }
        // ---- piliers naturels (du sol au plafond), hors entree, bassins et gouffres
        for (int i = 0; i < 300; i++) {
            Feature f = place(r, 2 + r.nextInt(4), 0, 0);
            if (f == null || kind[f.x()][f.z()] != 1) continue;
            if (Math.hypot(f.x() - PLAZA_X, f.z() - PLAZA_Z) < 38 || Math.hypot(f.x() - exitX, f.z() - exitZ) < 14) continue;
            boolean clash = false;
            for (Feature p : pools) clash |= Math.hypot(f.x() - p.x(), f.z() - p.z()) < p.r() + f.r() + 3;
            for (Feature p : chasms) clash |= Math.hypot(f.x() - p.x(), f.z() - p.z()) < p.r() + f.r() + 4;
            for (Feature p : pillars) clash |= Math.hypot(f.x() - p.x(), f.z() - p.z()) < p.r() + f.r() + 5;
            if (clash) continue;
            pillars.add(f);
            for (int x = f.x() - f.r() - 1; x <= f.x() + f.r() + 1; x++) {
                for (int z = f.z() - f.r() - 1; z <= f.z() + f.r() + 1; z++) {
                    if (x < 0 || z < 0 || x >= SIZE || z >= SIZE) continue;
                    if (Math.hypot(x - f.x(), z - f.z()) <= f.r() + 0.35 * fbm(x / 3.0, z / 3.0, 51, 2)) pillar[x][z] = true;
                }
            }
        }
    }

    /** Nombre de cases de caverne qui touchent le vide ou le bord du cadre (doit rester nul : sinon un trou laisse voir l'exterieur). */
    public int leaks() {
        int n = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                if (kind[x][z] != 1) continue;
                boolean leak = false;
                for (int dx = -1; dx <= 1 && !leak; dx++) {
                    for (int dz = -1; dz <= 1 && !leak; dz++) {
                        int nx = x + dx, nz = z + dz;
                        leak = nx < 0 || nz < 0 || nx >= SIZE || nz >= SIZE || kind[nx][nz] == 0;
                    }
                }
                if (leak && n++ < 6) com.mmorpg.MMORPG.LOGGER.info("[MMORPG] fuite de la caverne en ({}, {})", x, z);
            }
        }
        return n;
    }

    /** Position au hasard dans la caverne, a plus de {@code margin} de la paroi. */
    private Feature place(Random r, int radius, int margin, int depth) {
        for (int tries = 0; tries < 40; tries++) {
            int x = WALL + radius + margin + r.nextInt(SIZE - 2 * (WALL + radius + margin));
            int z = 70 + r.nextInt(SIZE - 70 - WALL - radius - margin);
            if (kind[x][z] != 1) continue;
            boolean ok = true;
            for (int a = 0; a < 8 && ok; a++) {
                int px = x + (int) Math.round(Math.cos(a * Math.PI / 4) * (radius + margin)), pz = z + (int) Math.round(Math.sin(a * Math.PI / 4) * (radius + margin));
                ok = px >= 0 && pz >= 0 && px < SIZE && pz < SIZE && kind[px][pz] == 1;
            }
            if (!ok) continue;
            if (pools.stream().anyMatch(p -> Math.hypot(p.x() - x, p.z() - z) < p.r() + radius + 8)) continue;
            return new Feature(x, z, radius, depth);
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------ acces

    public boolean in(int lx, int lz) {
        return lx >= 0 && lz >= 0 && lx < SIZE && lz < SIZE;
    }

    /** 0 vide, 1 interieur, 2 paroi. */
    public int kind(int lx, int lz) {
        return in(lx, lz) ? kind[lx][lz] : 0;
    }

    /** Hauteur (y absolu) du dernier bloc solide du sol. */
    public int floor(int lx, int lz) {
        return in(lx, lz) ? floor[lx][lz] : Y0;
    }

    /** Hauteur du premier bloc solide du plafond. */
    public int ceil(int lx, int lz) {
        return in(lx, lz) ? ceil[lx][lz] : Y0 + HEIGHT;
    }

    public int liquid(int lx, int lz) {
        return in(lx, lz) ? liquid[lx][lz] : -1;
    }

    public boolean isPillar(int lx, int lz) {
        return in(lx, lz) && pillar[lx][lz];
    }

    public Sector style(int lx, int lz) {
        return style;
    }

    /** Choisit le recoin du portail : un carre de 25 x 25 cases libres, de sol presque plat, dans le tiers sud-ouest ou sud-est. */
    private void pickExit(Random r, double[][] h) {
        for (int tries = 0; tries < 4000; tries++) {
            int x = 40 + r.nextInt(SIZE - 80), z = (int) (SIZE * 0.58) + r.nextInt((int) (SIZE * 0.34));
            if (Math.abs(x - PLAZA_X) < 50 && tries < 3000) continue;
            if (okExit(x, z, h)) {
                exitX = x;
                exitZ = z;
                return;
            }
        }
        for (int x = 40; x < SIZE - 40; x += 4) {                    // repli : premier endroit convenable en balayant
            for (int z = SIZE - 50; z > SIZE / 2; z -= 4) {
                if (okExit(x, z, h)) {
                    exitX = x;
                    exitZ = z;
                    return;
                }
            }
        }
        exitX = PLAZA_X;
        exitZ = SIZE - 60;
    }

    private boolean okExit(int x, int z, double[][] h) {
        for (int dx = -12; dx <= 12; dx += 3) {
            for (int dz = -12; dz <= 12; dz += 3) {
                int nx = x + dx, nz = z + dz;
                if (nx < 0 || nz < 0 || nx >= SIZE || nz >= SIZE || kind[nx][nz] != 1 || liquid[nx][nz] > 0 || Math.abs(h[nx][nz] - h[x][z]) > 3) return false;
            }
        }
        for (Feature f : pools) if (Math.hypot(f.x() - x, f.z() - z) < f.r() + 16) return false;
        for (Feature f : chasms) if (Math.hypot(f.x() - x, f.z() - z) < f.r() + 16) return false;
        return true;
    }

    /** Pas de la grille des zones de niveau (voir {@link com.mmorpg.world.LevelZones}). */
    public static final int ZONE_SIZE = 46;

    /** Niveau des monstres a cet endroit : l'etage donne la fourchette, la zone (cellule irreguliere) donne le palier dans cette fourchette. */
    public int mobLevel(int lx, int lz, Random r) {
        return Mth.clamp(com.mmorpg.world.LevelZones.level(SEED + region, lx, lz, ZONE_SIZE, style.minLevel(), style.maxLevel(), r), 1, 100);
    }

    // ------------------------------------------------------------------------------------------------ bruit

    static double hash(int x, int z, int salt) {
        long h = x * 374761393L + z * 668265263L + salt * 1442695040888963407L + SEED;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        h *= 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        return ((h & 0xFFFFFFL) / (double) 0xFFFFFFL) * 2 - 1;
    }

    /** Valeur aleatoire stable dans [0, 1) pour une cellule et une graine locale. */
    public static double rnd(int x, int z, int salt) {
        return (hash(x, z, salt) + 1) / 2;
    }

    static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    double value(double x, double z, int salt) {
        salt += regionSalt;
        int xi = (int) Math.floor(x), zi = (int) Math.floor(z);
        double fx = smooth(x - xi), fz = smooth(z - zi);
        double a = hash(xi, zi, salt), b = hash(xi + 1, zi, salt), c = hash(xi, zi + 1, salt), d = hash(xi + 1, zi + 1, salt);
        return Mth.lerp(fz, Mth.lerp(fx, a, b), Mth.lerp(fx, c, d));
    }

    double fbm(double x, double z, int salt, int octaves) {
        double sum = 0, amp = 1, norm = 0, f = 1;
        for (int i = 0; i < octaves; i++) {
            sum += amp * value(x * f, z * f, salt + i * 17);
            norm += amp;
            amp *= 0.5;
            f *= 2;
        }
        return sum / norm;
    }
}
