package com.mmorpg.hub;

import com.mmorpg.entity.dummy.TrainingDummyEntity;
import com.mmorpg.entity.hub.ClassMasterEntity;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.world.WaypointData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;

import java.util.Locale;
import java.util.UUID;

/**
 * Construction du hub d'entree (/mmorpg hub) : la « Citadelle d'Eldoria », une ile celeste rocheuse (face inferieure effilee,
 * cascades, cristaux, lianes) portant une place centrale a anneau d'eau, quatre pavillons de classe, un portail monumental,
 * un teleporteur, une salle d'epreuve et une aire d'entrainement, entoures d'ilots flottants. Les titres en lettres geantes
 * sont des entites d'affichage de texte avec la police « Eldoria ». Reconstruire le hub au meme endroit remplace ses PNJ,
 * mannequins et textes sans les dupliquer.
 */
public final class HubBuilder {
    private HubBuilder() {
    }

    /** Etiquette des entites decoratives du hub (textes), retirees a chaque reconstruction. */
    public static final String TAG = "mmorpg_hub";
    private static final int X0 = -34, X1 = 54, Z0 = -66, Z1 = 40;
    private static final Block[] FLOWERS = {Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.ALLIUM,
            Blocks.OXEYE_DAISY, Blocks.PINK_TULIP, Blocks.LILY_OF_THE_VALLEY};
    private static final PlayerClass[] CLASSES = {PlayerClass.GUERRIER, PlayerClass.MAGE, PlayerClass.ARCHER, PlayerClass.TANK};

    private static BlockPos O;
    private static ServerLevel L;
    private static boolean[][] foot;
    private static boolean[][] isle;
    private static float[][] edge;

    // ================================================================== primitives

    private static void set(int x, int y, int z, Block b) {
        set(x, y, z, b.defaultBlockState());
    }

    private static void set(int x, int y, int z, BlockState s) {
        Block b = s.getBlock();
        boolean shaped = b instanceof WallBlock || b instanceof CrossCollisionBlock || b instanceof IronBarsBlock;
        L.setBlock(O.offset(x, y, z), s, shaped ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS);
    }

    private static boolean air(int x, int y, int z) {
        return L.getBlockState(O.offset(x, y, z)).isAir();
    }

    private static void setIfAir(int x, int y, int z, BlockState s) {
        if (air(x, y, z)) set(x, y, z, s);
    }

    private static void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        BlockState s = b.defaultBlockState();
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    set(x, y, z, s);
    }

    private static BlockState stair(Block b, Direction facing, boolean top) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState slab(Block b, boolean top) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    private static BlockState axis(Block b, Direction.Axis a) {
        BlockState st = b.defaultBlockState();
        return st.hasProperty(RotatedPillarBlock.AXIS) ? st.setValue(RotatedPillarBlock.AXIS, a) : st;
    }

    private static BlockState leaves(Block b) {
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static BlockState hanging(Block lantern) {
        return lantern.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    private static BlockState cluster(Direction d) {
        return Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(BlockStateProperties.FACING, d);
    }

    /** Bruit pseudo-aleatoire deterministe 0..1 (le meme hub est reconstruit a l'identique). */
    private static float rnd(int x, int y, int z, int salt) {
        long v = x * 374761393L + y * 668265263L + z * 2147483647L + salt * 1274126177L;
        v = (v ^ (v >>> 13)) * 1274126177L;
        v ^= v >>> 16;
        return (v & 0xFFFFL) / 65535F;
    }

    /** Bruit de valeur lisse (interpolation bilineaire) : relief doux de la face inferieure. */
    private static float noise(float x, float z, float scale, int salt) {
        float fx = x / scale;
        float fz = z / scale;
        int ix = Mth.floor(fx);
        int iz = Mth.floor(fz);
        float tx = fx - ix;
        float tz = fz - iz;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        float a = rnd(ix, 0, iz, salt);
        float b = rnd(ix + 1, 0, iz, salt);
        float c = rnd(ix, 0, iz + 1, salt);
        float d = rnd(ix + 1, 0, iz + 1, salt);
        return Mth.lerp(tz, Mth.lerp(tx, a, b), Mth.lerp(tx, c, d));
    }

    // ================================================================== point d'entree

    /** Construit le hub avec sa place centrale au niveau {@code feet} (sol en dessous) et renvoie le centre. */
    public static BlockPos build(ServerLevel level, BlockPos feet) {
        L = level;
        O = feet.immutable();
        try {
            removeOld();
            computeMasks();
            terrain();
            plaza();
            for (PlayerClass c : CLASSES) pavilion(c);
            centerCanopy();
            gate();
            teleporter();
            trialRoom();
            yard();
            gardens();
            islets();
            texts();
            entities();
            BlockPos spawn = HubLayout.pos(O, HubLayout.SPAWN);
            level.setRespawnData(new LevelData.RespawnData(GlobalPos.of(level.dimension(), spawn), 180F, 0F));
            HubData.get(level.getServer()).set(level.dimension(), O);
            return O;
        } finally {
            L = null;
            O = null;
            foot = null;
            isle = null;
            edge = null;
        }
    }

    // ================================================================== emprise et face inferieure de l'ile

    private static boolean footprint(int x, int z) {
        if (x * x + z * z <= 19.6 * 19.6) return true;                                                  // place
        if (Math.abs(x) <= 7 && z >= 16 && z <= 28) return true;                                        // portail et parvis sud
        if (Math.abs(x) <= HubLayout.ROOM_HALF + 1 && z >= HubLayout.ROOM_Z0 - 1 && z <= HubLayout.ROOM_Z1 + 1) return true;   // salle
        if (Math.abs(x) <= 4 && z >= HubLayout.ROOM_Z1 && z <= -14) return true;                        // couloir de la salle
        if (x >= 14 && x <= 22 && Math.abs(z) <= 4) return true;                                        // allee de l'aire
        if (x >= 18 && x <= HubLayout.YARD_X1 + 2 && Math.abs(z) <= HubLayout.YARD_HALF + 2) return true;   // aire d'entrainement
        for (PlayerClass c : CLASSES) {
            int[] s = HubLayout.classSpot(c);
            if (Math.abs(x - s[0]) <= 6 && Math.abs(z - s[2]) <= 6) return true;
        }
        return false;
    }

    private static boolean in(boolean[][] m, int x, int z) {
        int i = x - X0;
        int j = z - Z0;
        return i >= 0 && j >= 0 && i < m.length && j < m[0].length && m[i][j];
    }

    private static void computeMasks() {
        int w = X1 - X0 + 1;
        int d = Z1 - Z0 + 1;
        foot = new boolean[w][d];
        isle = new boolean[w][d];
        edge = new float[w][d];
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) foot[x - X0][z - Z0] = footprint(x, z);
        for (int x = X0; x <= X1; x++) {                     // ile = emprise dilatee d'une bordure de pelouse (3 blocs)
            for (int z = Z0; z <= Z1; z++) {
                if (!foot[x - X0][z - Z0]) continue;
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        if (dx * dx + dz * dz > 11) continue;
                        int i = x + dx - X0;
                        int j = z + dz - Z0;
                        if (i >= 0 && j >= 0 && i < w && j < d) isle[i][j] = true;
                    }
                }
            }
        }
        float inf = 1e6F;                                    // distance au bord de l'ile (chanfrein, 2 passes)
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) edge[i][j] = isle[i][j] ? inf : 0;
        for (int i = 1; i < w - 1; i++) {
            for (int j = 1; j < d - 1; j++) {
                float v = edge[i][j];
                v = Math.min(v, edge[i - 1][j] + 1);
                v = Math.min(v, edge[i][j - 1] + 1);
                v = Math.min(v, edge[i - 1][j - 1] + 1.41F);
                v = Math.min(v, edge[i + 1][j - 1] + 1.41F);
                edge[i][j] = v;
            }
        }
        for (int i = w - 2; i >= 1; i--) {
            for (int j = d - 2; j >= 1; j--) {
                float v = edge[i][j];
                v = Math.min(v, edge[i + 1][j] + 1);
                v = Math.min(v, edge[i][j + 1] + 1);
                v = Math.min(v, edge[i + 1][j + 1] + 1.41F);
                v = Math.min(v, edge[i - 1][j + 1] + 1.41F);
                edge[i][j] = v;
            }
        }
    }

    private static BlockState rock(int x, int y, int z, float depthFrac) {
        float h = rnd(x, y, z, 7);
        if (depthFrac > 0.62F) {
            return (h < 0.5F ? Blocks.DEEPSLATE : h < 0.75F ? Blocks.TUFF : h < 0.9F ? Blocks.COBBLED_DEEPSLATE : Blocks.BLACKSTONE).defaultBlockState();
        }
        return (h < 0.5F ? Blocks.STONE : h < 0.68F ? Blocks.ANDESITE : h < 0.8F ? Blocks.TUFF : h < 0.9F ? Blocks.COBBLESTONE
                : depthFrac < 0.3F ? Blocks.MOSS_BLOCK : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
    }

    private static void terrain() {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                if (!in(isle, x, z)) continue;
                float e = edge[x - X0][z - Z0];
                int depth = Mth.clamp(Math.round(2.4F + 3.3F * (float) Math.sqrt(e) + 4.5F * (noise(x, z, 5F, 1) - 0.5F)), 3, 28);
                for (int y = 0; y <= 28; y++) set(x, y, z, air);
                boolean f = in(foot, x, z);
                set(x, -1, z, f ? Blocks.POLISHED_ANDESITE : Blocks.GRASS_BLOCK);
                set(x, -2, z, f ? Blocks.STONE_BRICKS : Blocks.DIRT);
                for (int y = -3; y >= -depth; y--) set(x, y, z, rock(x, y, z, (float) (-y - 3) / Math.max(1, depth - 3)));
                int by = -depth - 1;                          // cristaux et lianes lumineuses sous l'ile
                float h = rnd(x, 5, z, 11);
                if (e > 2F && h < 0.1F) {
                    set(x, by, z, cluster(Direction.DOWN));
                } else if (e < 6F && h > 0.55F) {
                    int len = 2 + (int) (rnd(x, 9, z, 13) * (e < 2F ? 6 : 3));
                    for (int k = 0; k < len; k++) {
                        BlockState s = (k == len - 1 ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
                                .setValue(BlockStateProperties.BERRIES, rnd(x, k, z, 17) < 0.6F);
                        set(x, by - k, z, s);
                    }
                }
                if (!f) {                                     // pelouse du bord : herbes, fleurs, arbustes
                    float g = rnd(x, 0, z, 21);
                    if (g < 0.16F) set(x, 0, z, Blocks.SHORT_GRASS);
                    else if (g < 0.24F) set(x, 0, z, FLOWERS[(int) (rnd(x, 1, z, 22) * FLOWERS.length) % FLOWERS.length]);
                    else if (g < 0.265F && e > 1.5F) {
                        set(x, 0, z, leaves(Blocks.AZALEA_LEAVES));
                        if (rnd(x, 2, z, 23) < 0.5F) set(x, 1, z, leaves(Blocks.FLOWERING_AZALEA_LEAVES));
                    }
                }
            }
        }
        int[][] falls = {{-23, 0}, {0, 26}, {24, -14}, {-18, -38}, {42, 14}};     // cascades : sources sur le rebord
        for (int[] f : falls) {
            int bx = f[0];
            int bz = f[1];
            float best = 1e9F;
            int fx = bx;
            int fz = bz;
            for (int x = X0 + 1; x < X1; x++) {
                for (int z = Z0 + 1; z < Z1; z++) {
                    if (!in(isle, x, z) || in(foot, x, z) || edge[x - X0][z - Z0] > 1.2F) continue;
                    float dd = (x - bx) * (x - bx) + (z - bz) * (z - bz);
                    if (dd < best) {
                        best = dd;
                        fx = x;
                        fz = z;
                    }
                }
            }
            if (best < 400F) {
                set(fx, -1, fz, Blocks.WATER);
                set(fx, 0, fz, Blocks.AIR);
            }
        }
    }

    // ================================================================== place

    private static boolean avenue(int x, int z) {
        return Math.abs(x) <= 1 || Math.abs(z) <= 1 || Math.abs(Math.abs(x) - Math.abs(z)) <= 1;
    }

    private static void plaza() {
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r > 18.6) continue;
                boolean spoke = x == 0 || z == 0 || Math.abs(x) == Math.abs(z);
                Block top;
                Block under = Blocks.STONE_BRICKS;
                if (r <= 1.6) top = Blocks.CHISELED_QUARTZ_BLOCK;
                else if (r <= 2.7) top = Blocks.GOLD_BLOCK;
                else if (r <= 4.2) top = spoke ? Blocks.GOLD_BLOCK : Blocks.SMOOTH_QUARTZ;
                else if (r <= 5.4) top = Blocks.QUARTZ_BRICKS;
                else if (r <= 7.9) {
                    if (Math.abs(x) <= 1 || Math.abs(z) <= 1) {
                        top = Blocks.POLISHED_ANDESITE;      // pont
                    } else {
                        top = Blocks.WATER;
                        under = Blocks.PRISMARINE_BRICKS;
                    }
                } else if (r <= 8.7) top = Blocks.QUARTZ_BRICKS;
                else if (r > 17.6) top = Blocks.POLISHED_BLACKSTONE_BRICKS;
                else if (r > 15.8) top = (x + z) % 2 == 0 ? Blocks.STONE_BRICKS : Blocks.CRACKED_STONE_BRICKS;
                else if (avenue(x, z)) top = x == 0 || z == 0 || x == z || x == -z ? Blocks.SMOOTH_QUARTZ : Blocks.POLISHED_ANDESITE;
                else {
                    top = Blocks.GRASS_BLOCK;
                    under = Blocks.DIRT;
                }
                set(x, -2, z, under);
                set(x, -1, z, top);
                if (top == Blocks.GRASS_BLOCK) {
                    float g = rnd(x, 0, z, 31);
                    if (g < 0.18F) set(x, 0, z, Blocks.SHORT_GRASS);
                    else if (g < 0.3F) set(x, 0, z, FLOWERS[(int) (rnd(x, 1, z, 32) * FLOWERS.length) % FLOWERS.length]);
                }
            }
        }
        for (int x = -10; x <= 10; x++) {                    // rebord de la fontaine : levres basses de quartz, sauf aux ponts
            for (int z = -10; z <= 10; z++) {
                if (Math.abs(x) <= 1 || Math.abs(z) <= 1) continue;
                double r = Math.sqrt(x * x + z * z);
                if ((r > 5.0 && r <= 5.4) || (r > 7.9 && r <= 8.3)) set(x, 0, z, slab(Blocks.QUARTZ_SLAB, false));
            }
        }
        for (int a = 0; a < 360; a += 2) {                   // balustrade du pourtour, pilastres de lanternes, ouvertures aux portes
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 18);
            int z = (int) Math.round(Math.sin(Math.toRadians(a)) * 18);
            if (Math.abs(x) <= 4 && z >= 13) continue;
            if (Math.abs(x) <= 4 && z <= -13) continue;
            if (x >= 13 && Math.abs(z) <= 4) continue;
            set(x, 0, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
            if (a % 12 == 0) {
                for (int y = 0; y <= 2; y++) set(x, y, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
                set(x, 3, z, Blocks.LANTERN);
            }
        }
        for (int s : new int[]{-1, 1}) {                     // lampadaires le long des quatre avenues
            for (int d = 10; d <= 16; d += 3) {
                lamp(s * 2, d);
                lamp(s * 2, -d);
                lamp(d, s * 2);
                lamp(-d, s * 2);
            }
        }
        flag(-7, -7, Blocks.BANNER.pick(DyeColor.RED));
        flag(7, -7, Blocks.BANNER.pick(DyeColor.PURPLE));
        flag(-7, 7, Blocks.BANNER.pick(DyeColor.GREEN));
        flag(7, 7, Blocks.BANNER.pick(DyeColor.BLUE));
    }

    private static void lamp(int x, int z) {
        for (int y = 0; y <= 2; y++) set(x, y, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        set(x, 3, z, Blocks.LANTERN);
    }

    /** Mat de fer et grande banniere aux couleurs d'une classe. */
    private static void flag(int x, int z, Block banner) {
        for (int y = 0; y <= 4; y++) set(x, y, z, Blocks.DARK_OAK_FENCE);
        set(x, 5, z, banner.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 0));
    }

    /** Baldaquin central : quatre piliers, poutres en croix et lustre de cristal au-dessus du Maitre des classes. */
    private static void centerCanopy() {
        for (int[] c : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) {
            for (int y = 0; y <= 8; y++) set(c[0], y, c[1], y == 0 ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : axis(Blocks.QUARTZ_PILLAR, Direction.Axis.Y));
            set(c[0], 9, c[1], Blocks.CHISELED_QUARTZ_BLOCK);
            set(c[0], 10, c[1], Blocks.SEA_LANTERN);
            set(c[0], 11, c[1], Blocks.END_ROD);
        }
        for (int i = -2; i <= 2; i++) {
            set(i, 9, 3, axis(Blocks.QUARTZ_PILLAR, Direction.Axis.X));
            set(i, 9, -3, axis(Blocks.QUARTZ_PILLAR, Direction.Axis.X));
            set(3, 9, i, axis(Blocks.QUARTZ_PILLAR, Direction.Axis.Z));
            set(-3, 9, i, axis(Blocks.QUARTZ_PILLAR, Direction.Axis.Z));
            set(i, 9, 0, axis(Blocks.QUARTZ_PILLAR, Direction.Axis.X));
            set(0, 9, i, axis(Blocks.QUARTZ_PILLAR, Direction.Axis.Z));
        }
        set(0, 9, 0, Blocks.CHISELED_QUARTZ_BLOCK);
        set(0, 10, 0, Blocks.GOLD_BLOCK);
        set(0, 11, 0, Blocks.END_ROD);
        set(0, 8, 0, Blocks.IRON_CHAIN);
        set(0, 7, 0, Blocks.IRON_CHAIN);
        set(0, 6, 0, Blocks.SEA_LANTERN);
        set(0, 5, 0, cluster(Direction.DOWN));
        for (int[] c : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) set(c[0], 8, c[1], hanging(Blocks.LANTERN));
    }

    // ================================================================== pavillons de classe

    private static final String[] EMB_GUERRIER = {"S.....S", ".S...S.", "..S.S..", "...H...", "..S.S..", ".G...G.", "P.....P"};
    private static final String[] EMB_MAGE = {"...A...", "..AGA..", ".AGWGA.", "AGWWWGA", ".AGWGA.", "..AGA..", "...A..."};
    private static final String[] EMB_ARCHER = {"..WWW..", ".WRRRW.", "WRRYRRW", "WRYYYRW", "WRRYRRW", ".WRRRW.", "..WWW.."};
    private static final String[] EMB_TANK = {"WWWWWWW", "WBBBBBW", "WBBWBBW", "WBWWWBW", ".WBWBW.", "..WBW..", "...W..."};

    private static void pavilion(PlayerClass cls) {
        int[] s = HubLayout.classSpot(cls);
        int cx = s[0];
        int cz = s[2];
        int sx = cx > 0 ? 1 : -1;
        int sz = cz > 0 ? 1 : -1;
        Block base, base2, accent, pillar, back, trim, light;
        String[] emblem;
        switch (cls) {
            case GUERRIER -> {
                base = Blocks.POLISHED_BLACKSTONE_BRICKS;
                base2 = Blocks.BLACKSTONE;
                accent = Blocks.RED_NETHER_BRICKS;
                pillar = Blocks.STRIPPED_CRIMSON_STEM;
                back = Blocks.POLISHED_BLACKSTONE_BRICKS;
                trim = Blocks.GILDED_BLACKSTONE;
                light = Blocks.SHROOMLIGHT;
                emblem = EMB_GUERRIER;
            }
            case MAGE -> {
                base = Blocks.PURPUR_BLOCK;
                base2 = Blocks.END_STONE_BRICKS;
                accent = Blocks.AMETHYST_BLOCK;
                pillar = Blocks.PURPUR_PILLAR;
                back = Blocks.BOOKSHELF;
                trim = Blocks.QUARTZ_BRICKS;
                light = Blocks.SEA_LANTERN;
                emblem = EMB_MAGE;
            }
            case ARCHER -> {
                base = Blocks.MOSSY_STONE_BRICKS;
                base2 = Blocks.SPRUCE_PLANKS;
                accent = Blocks.MOSS_BLOCK;
                pillar = Blocks.STRIPPED_SPRUCE_LOG;
                back = Blocks.DARK_OAK_PLANKS;
                trim = Blocks.SPRUCE_LOG;
                light = Blocks.LANTERN;
                emblem = EMB_ARCHER;
            }
            default -> {
                base = Blocks.POLISHED_DEEPSLATE;
                base2 = Blocks.DEEPSLATE_TILES;
                accent = Blocks.LAPIS_BLOCK;
                pillar = Blocks.QUARTZ_PILLAR;
                back = Blocks.IRON_BLOCK;
                trim = Blocks.CHISELED_QUARTZ_BLOCK;
                light = Blocks.SEA_LANTERN;
                emblem = EMB_TANK;
            }
        }
        for (int x = -5; x <= 5; x++) {                                          // sol et degagement
            for (int z = -5; z <= 5; z++) {
                boolean border = Math.abs(x) >= 4 || Math.abs(z) >= 4;
                set(cx + x, -2, cz + z, Blocks.STONE_BRICKS);
                set(cx + x, -1, cz + z, border ? accent : ((x + z) % 2 == 0 ? base : base2));
                for (int y = 0; y <= 14; y++) set(cx + x, y, cz + z, Blocks.AIR);
            }
        }
        for (int x = -2; x <= 2; x++) {                                          // marches vers le piedestal du PNJ (il se tient a y = 1)
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 && Math.abs(z) == 2) set(cx + x, 0, cz + z, accent);
                else if (Math.abs(x) == 2) set(cx + x, 0, cz + z, stair(Blocks.STONE_BRICK_STAIRS, x > 0 ? Direction.EAST : Direction.WEST, false));
                else if (Math.abs(z) == 2) set(cx + x, 0, cz + z, stair(Blocks.STONE_BRICK_STAIRS, z > 0 ? Direction.SOUTH : Direction.NORTH, false));
                else set(cx + x, 0, cz + z, x == 0 && z == 0 ? accent : base);
            }
        }
        int wx = cx + sx * 4;
        int wz = cz + sz * 4;
        for (int i = -4; i <= 4; i++) {                                          // fond de scene : deux murs sur les cotes exterieurs
            for (int y = 0; y <= 9; y++) {
                Block b = y <= 1 || y == 9 ? base : back;
                set(cx + i, y, wz, b);
                set(wx, y, cz + i, b);
                set(cx + i, y, wz + sz, base2);
                set(wx + sx, y, cz + i, base2);
            }
            set(cx + i, 10, wz, slab(Blocks.STONE_BRICK_SLAB, false));
            set(wx, 10, cz + i, slab(Blocks.STONE_BRICK_SLAB, false));
            set(cx + i, 1, wz, trim);
            set(wx, 1, cz + i, trim);
        }
        for (int row = 0; row < 7; row++) {                                      // embleme 7x7 incruste dans la face interieure
            for (int col = 0; col < 7; col++) {
                Block b = emblemBlock(cls, emblem[row].charAt(col));
                if (b == null) continue;
                int y = 8 - row;
                set(cx - 3 + col, y, wz, b);
                set(wx, y, cz - 3 + col, b);
            }
        }
        for (int[] c : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {        // piliers d'angle (les exterieurs plus hauts) et couronnes
            boolean outerX = c[0] == sx * 4;
            boolean outerZ = c[1] == sz * 4;
            int h = outerX && outerZ ? 13 : outerX || outerZ ? 11 : 7;
            for (int y = 0; y <= h; y++) set(cx + c[0], y, cz + c[1], y == 0 || y == h ? trim.defaultBlockState() : axis(pillar, Direction.Axis.Y));
            set(cx + c[0], h + 1, cz + c[1], cls == PlayerClass.GUERRIER ? Blocks.CAMPFIRE.defaultBlockState()
                    : cls == PlayerClass.MAGE ? Blocks.END_ROD.defaultBlockState() : light.defaultBlockState());
        }
        extras(cls, cx, cz, sx, sz);
    }

    private static Block emblemBlock(PlayerClass cls, char c) {
        if (c == '.') return null;
        return switch (cls) {
            case GUERRIER -> c == 'S' ? Blocks.IRON_BLOCK : Blocks.GOLD_BLOCK;
            case MAGE -> c == 'A' ? Blocks.AMETHYST_BLOCK : c == 'G' ? Blocks.CONCRETE.pick(DyeColor.PURPLE) : Blocks.SEA_LANTERN;
            case ARCHER -> c == 'W' ? Blocks.CONCRETE.pick(DyeColor.WHITE) : c == 'R' ? Blocks.CONCRETE.pick(DyeColor.RED) : Blocks.GOLD_BLOCK;
            default -> c == 'W' ? Blocks.CONCRETE.pick(DyeColor.WHITE) : Blocks.CONCRETE.pick(DyeColor.BLUE);
        };
    }

    /** Mobilier propre a chaque classe, au sol (y = 0) sur les flancs du piedestal. */
    private static void extras(PlayerClass cls, int cx, int cz, int sx, int sz) {
        switch (cls) {
            case GUERRIER -> {
                set(cx - sx * 3, 0, cz + sz * 2, Blocks.ANVIL);
                set(cx + sx * 3, 0, cz - sz * 2, Blocks.GRINDSTONE);
                set(cx + sx * 3, 0, cz + sz * 3, Blocks.BLAST_FURNACE);
                set(cx - sx * 3, 0, cz - sz * 3, Blocks.SMITHING_TABLE);
                set(cx + sx * 3, 0, cz, Blocks.LAVA_CAULDRON);
                set(cx, 0, cz + sz * 3, Blocks.LAVA_CAULDRON);
                set(cx - sx * 3, 0, cz, Blocks.IRON_BARS);
                set(cx - sx * 3, 1, cz, Blocks.IRON_BARS);
                set(cx - sx * 3, 2, cz, hanging(Blocks.LANTERN));
                for (int i = -3; i <= 3; i += 3) {
                    set(cx + i, 8, cz + sz * 3, Blocks.IRON_CHAIN);
                    set(cx + i, 7, cz + sz * 3, hanging(Blocks.LANTERN));
                }
            }
            case MAGE -> {
                set(cx - sx * 3, 0, cz + sz * 2, Blocks.ENCHANTING_TABLE);
                set(cx + sx * 3, 0, cz - sz * 2, Blocks.ENCHANTING_TABLE);
                set(cx - sx * 3, 0, cz - sz * 3, Blocks.LECTERN);
                set(cx, 0, cz + sz * 3, Blocks.BREWING_STAND);
                set(cx + sx * 3, 0, cz, Blocks.AMETHYST_BLOCK);
                set(cx + sx * 3, 1, cz, cluster(Direction.UP));
                set(cx - sx * 3, 0, cz, Blocks.AMETHYST_BLOCK);
                set(cx - sx * 3, 1, cz, cluster(Direction.UP));
                for (int[] p : new int[][]{{-2, 3}, {2, 3}, {3, -2}, {3, 2}}) {
                    set(cx + p[0] * sx, 0, cz + p[1] * sz, Blocks.DYED_CANDLE.pick(DyeColor.PURPLE).defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
                }
                set(cx, 6, cz, Blocks.AMETHYST_BLOCK);                           // cristal flottant au-dessus du Maitre
                set(cx, 7, cz, Blocks.AMETHYST_BLOCK);
                set(cx, 8, cz, cluster(Direction.UP));
                set(cx, 5, cz, cluster(Direction.DOWN));
                for (Direction d : Direction.Plane.HORIZONTAL) set(cx + d.getStepX(), 7, cz + d.getStepZ(), cluster(d));
            }
            case ARCHER -> {
                set(cx - sx * 3, 0, cz + sz * 2, Blocks.FLETCHING_TABLE);
                set(cx + sx * 3, 0, cz - sz * 2, Blocks.BARREL);
                set(cx + sx * 3, 0, cz + sz * 3, Blocks.HAY_BLOCK);
                set(cx + sx * 3, 1, cz + sz * 3, Blocks.TARGET);
                set(cx - sx * 3, 0, cz - sz * 3, Blocks.HAY_BLOCK);
                set(cx - sx * 3, 1, cz - sz * 3, Blocks.TARGET);
                set(cx, 0, cz + sz * 3, Blocks.POTTED_FLOWERING_AZALEA);
                set(cx + sx * 3, 0, cz, leaves(Blocks.AZALEA_LEAVES));
                for (int i = -4; i <= 4; i++) {                                  // haie vivante sur le faite des murs
                    set(cx + i, 11, cz + sz * 4, leaves(Blocks.AZALEA_LEAVES));
                    set(cx + sx * 4, 11, cz + i, leaves(Blocks.FLOWERING_AZALEA_LEAVES));
                }
                for (int i = -3; i <= 3; i += 2) {
                    set(cx + i, 8, cz + sz * 3, hanging(Blocks.LANTERN));
                    set(cx + sx * 3, 8, cz + i, hanging(Blocks.LANTERN));
                }
            }
            default -> {
                set(cx - sx * 3, 0, cz + sz * 2, Blocks.SMITHING_TABLE);
                set(cx + sx * 3, 0, cz - sz * 2, Blocks.ANVIL);
                set(cx + sx * 3, 0, cz + sz * 3, Blocks.IRON_BLOCK);
                set(cx + sx * 3, 1, cz + sz * 3, Blocks.IRON_BARS);
                set(cx - sx * 3, 0, cz - sz * 3, Blocks.IRON_BLOCK);
                set(cx - sx * 3, 1, cz - sz * 3, Blocks.IRON_BARS);
                set(cx, 0, cz + sz * 3, Blocks.CHISELED_QUARTZ_BLOCK);
                for (int i = -3; i <= 3; i += 3) {
                    set(cx + i, 8, cz + sz * 3, Blocks.IRON_CHAIN);
                    set(cx + i, 7, cz + sz * 3, hanging(Blocks.SOUL_LANTERN));
                }
            }
        }
    }

    // ================================================================== portail sud et teleporteur

    private static void gate() {
        for (int x = -7; x <= 7; x++) {                                          // parvis : allee, bordures et pelouses
            for (int z = 20; z <= 28; z++) {
                int ax = Math.abs(x);
                Block top = ax == 0 ? Blocks.CHISELED_STONE_BRICKS : ax <= 1 ? Blocks.POLISHED_ANDESITE : ax == 2 ? Blocks.SMOOTH_QUARTZ : Blocks.GRASS_BLOCK;
                set(x, -2, z, ax >= 3 ? Blocks.DIRT : Blocks.STONE_BRICKS);
                set(x, -1, z, top);
                if (ax >= 3 && rnd(x, 0, z, 41) < 0.3F) {
                    set(x, 0, z, rnd(x, 1, z, 42) < 0.5F ? Blocks.SHORT_GRASS : FLOWERS[(int) (rnd(x, 3, z, 43) * FLOWERS.length) % FLOWERS.length]);
                }
            }
        }
        for (int z = 22; z <= 26; z += 4) for (int s : new int[]{-1, 1}) lamp(s * 3, z);
        flag(-5, 24, Blocks.BANNER.pick(DyeColor.RED));
        flag(5, 24, Blocks.BANNER.pick(DyeColor.BLUE));
        for (int s : new int[]{-1, 1}) {                                         // tours a toit de tuiles sombres
            int a = s > 0 ? 4 : -7;
            int b = s > 0 ? 7 : -4;
            for (int x = a; x <= b; x++) {
                for (int z = 16; z <= 19; z++) {
                    for (int y = 0; y <= 12; y++) set(x, y, z, y == 4 || y == 8 ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.STONE_BRICKS);
                }
            }
            int slit = s > 0 ? 5 : -6;
            for (int y = 5; y <= 6; y++) set(slit, y, 19, Blocks.IRON_BARS);
            for (int y = 9; y <= 10; y++) set(slit, y, 19, Blocks.IRON_BARS);
            for (int x = a - 1; x <= b + 1; x++) for (int z = 15; z <= 20; z++) set(x, 13, z, Blocks.DEEPSLATE_TILES);
            for (int x = a; x <= b; x++) for (int z = 16; z <= 19; z++) set(x, 14, z, Blocks.DEEPSLATE_TILES);
            for (int x = a + 1; x <= b - 1; x++) for (int z = 17; z <= 18; z++) set(x, 15, z, Blocks.DEEPSLATE_TILES);
            set(slit, 16, 17, Blocks.DEEPSLATE_TILES);
            set(slit, 17, 17, Blocks.END_ROD);
            set(slit, 18, 17, Blocks.END_ROD);
            set(s > 0 ? 3 : -3, 4, 20, hanging(Blocks.LANTERN));
        }
        for (int x = -3; x <= 3; x++) {                                          // courtine et arche, frise doree au-dessus de la porte
            for (int z = 17; z <= 19; z++) {
                for (int y = 0; y <= 10; y++) {
                    boolean open = Math.abs(x) <= 2 && y <= 4 || Math.abs(x) <= 1 && y == 5;
                    set(x, y, z, open ? Blocks.AIR : (y == 6 ? Blocks.GOLD_BLOCK : Blocks.STONE_BRICKS));
                }
                if (x % 2 == 0) set(x, 11, z, Blocks.STONE_BRICK_WALL);
            }
        }
        set(0, 4, 18, hanging(Blocks.LANTERN));
    }

    private static void teleporter() {
        BlockPos tp = HubLayout.pos(O, HubLayout.TELEPORTER);
        for (int x = -3; x <= 3; x++) {
            for (int z = 10; z <= 16; z++) {
                double r = Math.hypot(x, z - 13);
                Block top = r <= 0.5 ? Blocks.CHISELED_QUARTZ_BLOCK : r <= 1.6 ? Blocks.GOLD_BLOCK : r <= 2.6 ? Blocks.SMOOTH_QUARTZ : r <= 3.5 ? Blocks.PRISMARINE_BRICKS : Blocks.POLISHED_ANDESITE;
                set(x, -1, z, top);
            }
        }
        for (int[] c : new int[][]{{-3, 10}, {3, 10}, {-3, 16}, {3, 16}}) {
            for (int y = 0; y <= 4; y++) set(c[0], y, c[1], axis(Blocks.QUARTZ_PILLAR, Direction.Axis.Y));
            set(c[0], 5, c[1], Blocks.SEA_LANTERN);
            set(c[0], 6, c[1], Blocks.END_ROD);
        }
        L.setBlock(tp, ModBlocks.TELEPORTEUR.get().defaultBlockState(), Block.UPDATE_ALL);
        WaypointData data = WaypointData.get(L.getServer());
        data.removeAt(L.dimension(), tp);
        data.create(L.dimension(), tp, "Hub d'Eldoria", WaypointData.CITY, 1, new UUID(0, 0));
    }

    // ================================================================== salle d'epreuve

    private static void trialRoom() {
        int h = HubLayout.ROOM_HEIGHT;
        int z0 = HubLayout.ROOM_Z0;
        int z1 = HubLayout.ROOM_Z1;
        int hw = HubLayout.ROOM_HALF;
        double zc = (z0 + z1) / 2.0;
        Block[] banners = {Blocks.WALL_BANNER.pick(DyeColor.RED), Blocks.WALL_BANNER.pick(DyeColor.PURPLE), Blocks.WALL_BANNER.pick(DyeColor.GREEN), Blocks.WALL_BANNER.pick(DyeColor.BLUE)};
        for (int x = -hw; x <= hw; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean wall = Math.abs(x) == hw || z == z0 || z == z1;
                double r = Math.hypot(x, z - zc);
                Block floor;
                if (r <= 1.6) floor = Blocks.CHISELED_POLISHED_BLACKSTONE;
                else if (r <= 2.6) floor = Blocks.GOLD_BLOCK;
                else if (r > 7.4 && r <= 8.2) floor = Blocks.GOLD_BLOCK;
                else if (r > 8.2 && r <= 9.0) floor = Blocks.POLISHED_BLACKSTONE;
                else floor = ((x + z) & 1) == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES;
                set(x, -2, z, Blocks.DEEPSLATE_BRICKS);
                set(x, -1, z, floor);
                for (int y = 0; y < h; y++) set(x, y, z, wall ? (y % 4 == 3 ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.STONE_BRICKS) : Blocks.AIR);
                set(x, h, z, Blocks.DEEPSLATE_BRICKS);
                if (!wall && Math.abs(x) <= 1 && z > zc + 3) set(x, 0, z, Blocks.CARPET.pick(DyeColor.RED));
            }
        }
        for (int z = z0 + 5; z <= z1 - 5; z += 5) {                              // pilastres, bannieres, poutres et lustres
            for (int s : new int[]{-1, 1}) {
                int x = s * (hw - 1);
                for (int y = 0; y < h - 1; y++) set(x, y, z, y == 0 || y == h - 2 ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState() : axis(Blocks.POLISHED_BLACKSTONE_BRICKS, Direction.Axis.Y));
                int bz = z + 2;
                if (bz < z1 - 1) {
                    Block b = banners[((z - z0) / 5) % 4];
                    set(x, 5, bz, b.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, s < 0 ? Direction.EAST : Direction.WEST));
                }
            }
            for (int x = -hw + 1; x <= hw - 1; x++) set(x, h - 1, z, axis(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X));
            for (int x = -10; x <= 10; x += 10) {
                set(x, h - 2, z, Blocks.IRON_CHAIN);
                set(x, h - 3, z, Blocks.IRON_CHAIN);
                set(x, h - 4, z, hanging(Blocks.SOUL_LANTERN));
            }
        }
        for (int x = -2; x <= 2; x++) for (int y = 0; y <= 4; y++) set(x, y, z1, Blocks.AIR);       // porte et couloir
        for (int x = -3; x <= 3; x++) set(x, 5, z1, x == -3 || x == 3 ? Blocks.CHISELED_STONE_BRICKS : Blocks.GOLD_BLOCK);
        for (int x : new int[]{-3, 3}) set(x, 4, z1, Blocks.LANTERN);
        for (int z = z1 + 1; z <= -14; z++) {
            for (int x = -4; x <= 4; x++) {
                set(x, -2, z, Blocks.STONE_BRICKS);
                set(x, -1, z, Math.abs(x) <= 1 ? Blocks.POLISHED_ANDESITE : Math.abs(x) <= 3 ? Blocks.SMOOTH_QUARTZ : Blocks.GRASS_BLOCK);
                for (int y = 0; y <= 6; y++) set(x, y, z, Math.abs(x) == 3 && y <= 3 ? Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            if ((z - z1) % 4 == 0) for (int s : new int[]{-3, 3}) set(s, 4, z, Blocks.LANTERN);
        }
    }

    // ================================================================== aire d'entrainement

    private static void yard() {
        int x0 = HubLayout.YARD_X0;
        int x1 = HubLayout.YARD_X1;
        int hz = HubLayout.YARD_HALF;
        for (int x = 14; x <= x1 + 2; x++) {                                     // sol : allee depuis la place, terre battue dans l'aire
            for (int z = -hz - 2; z <= hz + 2; z++) {
                if (!in(foot, x, z)) continue;
                boolean inside = x >= x0 && x <= x1 && Math.abs(z) <= hz;
                Block top;
                if (x < x0) top = Math.abs(z) <= 1 ? Blocks.POLISHED_ANDESITE : Math.abs(z) == 2 ? Blocks.SMOOTH_QUARTZ : Blocks.GRASS_BLOCK;
                else if (inside) top = rnd(x, 0, z, 51) < 0.3F ? Blocks.COARSE_DIRT : rnd(x, 1, z, 52) < 0.3F ? Blocks.GRAVEL : Blocks.PACKED_MUD;
                else top = Blocks.GRASS_BLOCK;
                set(x, -2, z, Blocks.DIRT);
                set(x, -1, z, top);
            }
        }
        for (int x = x0; x <= x1; x++) {                                         // palissade de rondins, pointes et lanternes
            for (int z : new int[]{-hz, hz}) palisade(x, z, (x - x0) % 6 == 0);
        }
        for (int z = -hz; z <= hz; z++) {
            if (Math.abs(z) <= 2) continue;
            palisade(x0, z, false);
            palisade(x1, z, (z + hz) % 6 == 0);
        }
        for (int[] c : new int[][]{{x0, -hz}, {x0, hz}, {x1, -hz}, {x1, hz}}) {   // tours d'angle
            for (int y = 0; y <= 5; y++) set(c[0], y, c[1], axis(Blocks.SPRUCE_LOG, Direction.Axis.Y));
            set(c[0], 6, c[1], Blocks.LANTERN);
        }
        for (int s : new int[]{-1, 1}) {                                         // portique d'entree a l'ouest
            for (int y = 0; y <= 5; y++) set(x0, y, s * 3, axis(Blocks.SPRUCE_LOG, Direction.Axis.Y));
            set(x0, 6, s * 3, Blocks.LANTERN);
        }
        for (int z = -3; z <= 3; z++) set(x0, 5, z, axis(Blocks.DARK_OAK_LOG, Direction.Axis.Z));
        set(x0, 4, 0, hanging(Blocks.LANTERN));
        for (int z : new int[]{-hz + 2, hz - 2}) {                               // bottes de paille, cibles et tonneaux
            set(x0 + 2, 0, z, Blocks.HAY_BLOCK);
            set(x0 + 3, 0, z, Blocks.HAY_BLOCK);
            set(x0 + 2, 1, z, Blocks.TARGET);
            set(x1 - 2, 0, z, Blocks.BARREL);
            set(x1 - 3, 0, z, Blocks.BARREL);
        }
        for (int z = -hz + 1; z <= hz - 1; z += 4) {                             // pylones d'entrainement le long du fond
            set(x1 - 1, 0, z, Blocks.DARK_OAK_FENCE);
            set(x1 - 1, 1, z, Blocks.DARK_OAK_FENCE);
            set(x1 - 1, 2, z, Blocks.TARGET);
        }
    }

    private static void palisade(int x, int z, boolean lantern) {
        for (int y = 0; y <= 2; y++) set(x, y, z, axis(Blocks.SPRUCE_LOG, Direction.Axis.Y));
        set(x, 3, z, Blocks.SPRUCE_FENCE);
        if (lantern) set(x, 4, z, Blocks.LANTERN);
    }

    // ================================================================== jardins : cerisiers, bancs, massifs

    private static void gardens() {
        int[][] trees = {{-14, -4, 6}, {-14, 4, 5}, {11, -4, 6}, {11, 4, 5}, {-5, -11, 6}, {5, -11, 5}, {-5, 9, 5}, {5, 9, 6}, {14, -6, 5}, {14, 6, 6}};
        for (int[] t : trees) cherry(t[0], t[1], t[2]);
        bench(-12, 0, Direction.EAST);
        bench(-12, -2, Direction.EAST);
        bench(-12, 2, Direction.EAST);
        bench(0, -12, Direction.SOUTH);
        bench(2, 9, Direction.NORTH);
        bench(-2, 9, Direction.NORTH);
    }

    private static void cherry(int x, int z, int h) {
        if (L.getBlockState(O.offset(x, -1, z)).getBlock() != Blocks.GRASS_BLOCK) return;
        for (int y = 0; y < h; y++) set(x, y, z, axis(Blocks.CHERRY_LOG, Direction.Axis.Y));
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    double d = dx * dx + dz * dz + dy * dy * 1.6;
                    if (d > 9.6 || rnd(x + dx, h + dy, z + dz, 61) < 0.14F) continue;
                    setIfAir(x + dx, h - 1 + dy, z + dz, leaves(Blocks.CHERRY_LEAVES));
                }
            }
        }
        for (int[] p : new int[][]{{1, 0}, {-1, 1}, {0, -1}}) setIfAir(x + p[0], 0, z + p[1], Blocks.PINK_PETALS.defaultBlockState());
    }

    private static void bench(int x, int z, Direction facing) {
        if (!air(x, 0, z)) return;
        set(x, 0, z, stair(Blocks.SPRUCE_STAIRS, facing, false));
    }

    // ================================================================== ilots flottants

    private static void islets() {
        int[][] spots = {{-46, 8, -8, 6}, {-40, 16, 32, 5}, {32, 10, 36, 6}, {64, 6, 18, 5}, {-8, 24, -78, 7}, {56, 14, -34, 5}, {-44, 4, -48, 5}};
        for (int[] s : spots) {
            int cx = s[0];
            int cy = s[1];
            int cz = s[2];
            int r = s[3];
            if (!air(cx, cy, cz) || !air(cx, cy + 6, cz)) continue;                  // jamais dans du relief existant
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d = Math.hypot(dx, dz) + (noise(cx + dx, cz + dz, 3F, 71) - 0.5) * 2.0;
                    if (d > r) continue;
                    int depth = Math.max(1, (int) Math.round((r - d) * 1.7));
                    set(cx + dx, cy, cz + dz, Blocks.GRASS_BLOCK);
                    for (int k = 1; k <= depth; k++) set(cx + dx, cy - k, cz + dz, k == 1 ? Blocks.DIRT.defaultBlockState() : rock(cx + dx, cy - k, cz + dz, 0.2F));
                    if (d < r - 1 && rnd(cx + dx, cy, cz + dz, 72) < 0.2F) set(cx + dx, cy + 1, cz + dz, rnd(cx + dx, 1, cz + dz, 73) < 0.5F ? Blocks.SHORT_GRASS.defaultBlockState() : FLOWERS[(int) (rnd(cx + dx, 2, cz + dz, 74) * FLOWERS.length) % FLOWERS.length].defaultBlockState());
                    if (depth >= 3 && rnd(cx + dx, 3, cz + dz, 75) < 0.12F) set(cx + dx, cy - depth - 1, cz + dz, cluster(Direction.DOWN));
                }
            }
            for (int y = 1; y <= 4; y++) set(cx, cy + y, cz, axis(Blocks.CHERRY_LOG, Direction.Axis.Y));
            for (int dx = -2; dx <= 2; dx++) for (int dy = 0; dy <= 3; dy++) for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz + dy * dy * 1.5 <= 6 && rnd(cx + dx, cy + dy, cz + dz, 76) > 0.12F) setIfAir(cx + dx, cy + 3 + dy, cz + dz, leaves(Blocks.CHERRY_LEAVES));
            }
            set(cx + 1, cy + 1, cz, Blocks.LANTERN);
        }
    }

    // ================================================================== textes en police Eldoria

    private static void text(double x, double y, double z, String text, String color, float scale) {
        String snbt = String.format(Locale.ROOT, "{text:{text:\"%s\",font:\"mmorpg:eldoria\",color:\"%s\"},billboard:\"center\",background:0,shadow:true,"
                + "see_through:false,line_width:600,Tags:[\"%s\"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                + "translation:[0f,0f,0f],scale:[%.2ff,%.2ff,%.2ff]}}", text.replace("\"", "\\\""), color, TAG, scale, scale, scale);
        CommandSourceStack src = L.getServer().createCommandSourceStack().withLevel(L).withSuppressedOutput();
        L.getServer().getCommands().performPrefixedCommand(src,
                String.format(Locale.ROOT, "summon minecraft:text_display %.2f %.2f %.2f %s", O.getX() + x + 0.5, O.getY() + y, O.getZ() + z + 0.5, snbt));
    }

    private static void texts() {
        text(0, 15.5, 19.4, "ELDORIA", "#FFD060", 9F);
        text(0, 12.6, 19.4, "Citadelle des héros", "#E8E0D0", 2.6F);
        for (PlayerClass c : CLASSES) {
            int[] s = HubLayout.classSpot(c);
            String[] t = switch (c) {
                case GUERRIER -> new String[]{"LE GUERRIER", "#FF6A50"};
                case MAGE -> new String[]{"LE MAGE", "#B07CFF"};
                case ARCHER -> new String[]{"L'ARCHER", "#6CE06C"};
                default -> new String[]{"LE TANK", "#58A8FF"};
            };
            text(s[0], 17.5, s[2], t[0], t[1], 5F);
        }
        text(0, 8, 14, "TÉLÉPORTEUR", "#9FD8FF", 3F);
        text(0, 12.5, HubLayout.ROOM_Z1 + 0.5, "ÉPREUVE DES HÉROS", "#FFC060", 4F);
        text(HubLayout.YARD_X0 - 0.5, 9, 0, "AIRE D'ENTRAÎNEMENT", "#FFB070", 3.6F);
    }

    // ================================================================== PNJ et mannequins

    private static void removeOld() {
        AABB box = HubLayout.hubBounds(O);
        for (Entity e : L.getEntities((Entity) null, box, x -> x instanceof ClassMasterEntity || x instanceof TrainingDummyEntity
                || x instanceof Display.TextDisplay
                || (x instanceof com.mmorpg.entity.NpcEntity n && "maitre_classes".equals(n.group())))) {
            if (e instanceof Display.TextDisplay && !e.entityTags().contains(TAG)) continue;
            e.discard();
        }
    }

    private static void entities() {
        var master = com.mmorpg.world.DungeonBuilder.spawnNpc(L, HubLayout.pos(O, HubLayout.CLASS_MASTER), com.mmorpg.entity.NpcEntity.Role.QUETES,
                "Maître des classes", 2, 0F);
        if (master != null) master.setup(com.mmorpg.entity.NpcEntity.Role.QUETES, "Maître des classes", 2, "maitre_classes");
        for (int k = 0; k < 4; k++) {
            PlayerClass cls = CLASSES[k];
            int[] s = HubLayout.classSpot(cls);
            spawnMaster(k, s[0], 1, s[2], HubLayout.classYaw(cls));
        }
        spawnMaster(ClassMasterEntity.EPREUVE, HubLayout.TRIAL_MASTER[0], 0, HubLayout.TRIAL_MASTER[2], 90F);
        for (int[] d : HubLayout.DUMMIES_ROOM) spawnDummy(d);
        for (int[] d : HubLayout.DUMMIES_YARD) spawnDummy(d);
    }

    private static void spawnMaster(int kind, int x, int y, int z, float yaw) {
        ClassMasterEntity npc = ModEntities.CLASS_MASTER.get().create(L, EntitySpawnReason.COMMAND);
        if (npc == null) return;
        npc.snapTo(O.getX() + x + 0.5, O.getY() + y, O.getZ() + z + 0.5, yaw, 0F);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setup(kind);
        L.addFreshEntity(npc);
    }

    private static void spawnDummy(int[] d) {
        TrainingDummyEntity e = ModEntities.TRAINING_DUMMY.get().create(L, EntitySpawnReason.COMMAND);
        if (e == null) return;
        e.snapTo(O.getX() + d[0] + 0.5, O.getY(), O.getZ() + d[1] + 0.5, d[3], 0F);
        e.setup(d[2]);
        L.addFreshEntity(e);
    }
}
