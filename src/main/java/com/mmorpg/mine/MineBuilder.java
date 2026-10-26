package com.mmorpg.mine;

import com.mmorpg.mine.MineLayout.Sector;
import com.mmorpg.registry.DecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import java.util.HashSet;
import java.util.Set;

/**
 * Construit les Mines d'Aeloria rangee par rangee (une rangee = une ligne de blocs d'ouest en est), en un budget de temps par tick :
 * la caverne ({@link MineLayout}) puis sa decoration (stalactites, plantes, lumieres, poteaux, charpentes, campements), les minerais,
 * les echelles des gouffres et la place d'entree.
 */
public final class MineBuilder {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final int SLAB = 32;

    /** Etiquette des textes flottants des mines (retires avant une reconstruction). */
    public static final String TAG = "mmorpg_mines";
    private static final net.minecraft.world.item.DyeColor[] VEIL = {net.minecraft.world.item.DyeColor.LIGHT_GRAY, net.minecraft.world.item.DyeColor.ORANGE,
            net.minecraft.world.item.DyeColor.LIGHT_BLUE, net.minecraft.world.item.DyeColor.PURPLE, net.minecraft.world.item.DyeColor.PURPLE};
    private static final String[] VEIL_COLOR = {"#E0E0E0", "#FFA040", "#8CD8FF", "#C080FF", "#C080FF"};
    private static final String[] TIER_COLORS = {"#8CE68C", "#FFE070", "#FFA050", "#FF6A5A"};

    private final ServerLevel level;
    private final int region;
    private final MineLayout L;
    private final int ox;
    private final List<com.mmorpg.world.LevelZones.Cell> totems = new ArrayList<>();
    private int totemsDone = 0;
    private boolean exitDone = false;
    private final Set<Long> forced = new HashSet<>();
    private final java.util.ArrayDeque<Long> pending = new java.util.ArrayDeque<>();
    private int row = 0;
    private int slabEnd = 0;
    private boolean plazaDone = false;
    private int laddersDone = 0;
    private long placed = 0;

    public MineBuilder(ServerLevel level, int region) {
        this.level = level;
        this.region = region;
        this.L = MineLayout.get(region);
        this.ox = L.x0();
        for (int gx = -1; gx <= MineLayout.SIZE / MineLayout.ZONE_SIZE + 1; gx++) {
            for (int gz = -1; gz <= MineLayout.SIZE / MineLayout.ZONE_SIZE + 1; gz++) {
                var c = com.mmorpg.world.LevelZones.at(MineLayout.SEED + region, gx, gz, MineLayout.ZONE_SIZE);
                int x = c.cx(), z = c.cz();
                if (!L.in(x, z) || Math.hypot(x - MineLayout.PLAZA_X, z - MineLayout.PLAZA_Z) < 40 || Math.hypot(x - L.exitX, z - L.exitZ) < 22) continue;
                boolean flat = true;
                for (int dx = -2; dx <= 2 && flat; dx++) {
                    for (int dz = -2; dz <= 2 && flat; dz++) flat = free(x + dx, z + dz) && Math.abs(L.floor(x + dx, z + dz) - L.floor(x, z)) <= 1;
                }
                if (flat) totems.add(c);
            }
        }
        totems.sort(Comparator.comparingInt(com.mmorpg.world.LevelZones.Cell::cz));
    }

    public int region() {
        return region;
    }

    public int percent() {
        return Math.min(100, row * 100 / MineLayout.SIZE);
    }

    public long blocksPlaced() {
        return placed;
    }

    /** Avance la construction pendant au plus {@code nanos} nanosecondes ; renvoie vrai quand tout est construit. */
    public boolean step(long nanos) {
        long start = System.nanoTime();
        if (row == 0 && slabEnd == 0) clearTexts();
        while (row < MineLayout.SIZE) {
            if (row >= slabEnd) nextSlab();
            if (!drainChunks(start, nanos)) return false;
            for (int lx = 0; lx < MineLayout.SIZE; lx++) column(lx, row);
            row++;
            if (!plazaDone && row >= MineLayout.PLAZA_Z + 22) {
                plaza();
                plazaDone = true;
            }
            while (laddersDone < L.chasms.size() && row > L.chasms.get(laddersDone).z() + L.chasms.get(laddersDone).r() + 3) ladders(L.chasms.get(laddersDone++));
            while (totemsDone < totems.size() && row > totems.get(totemsDone).cz() + 5) totem(totems.get(totemsDone++));
            if (!exitDone && row > L.exitZ + 14) {
                exitPortal();
                exitDone = true;
            }
            if (System.nanoTime() - start > nanos) return false;
        }
        while (laddersDone < L.chasms.size()) ladders(L.chasms.get(laddersDone++));
        if (!plazaDone) {
            plaza();
            plazaDone = true;
        }
        release();
        return true;
    }

    private void nextSlab() {
        int slabStart = row;
        slabEnd = Math.min(MineLayout.SIZE, row + SLAB);
        int cx0 = (ox - 16) >> 4, cx1 = (ox + MineLayout.SIZE + 16) >> 4;
        int cz0 = (MineLayout.Z0 + slabStart - 16) >> 4, cz1 = (MineLayout.Z0 + slabEnd + 16) >> 4;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (forced.add(ChunkKey.of(cx, cz))) pending.add(ChunkKey.of(cx, cz));
            }
        }
        // on libere ce qui est derriere nous (les echelles et la place utilisent le chargement a la demande)
        int keep = (MineLayout.Z0 + slabStart - 48) >> 4;
        forced.removeIf(k -> {
            int cz = ChunkKey.z(k);
            if (cz >= keep) return false;
            level.setChunkForced(ChunkKey.x(k), cz, false);
            return true;
        });
    }

    /**
     * Charge de force les chunks de la tranche, quelques-uns par passage : attendre des centaines de chunks d'un coup bloquerait le tick
     * (et le chien de garde du serveur le prendrait pour un plantage au premier demarrage). @return vrai quand tout est charge
     */
    private boolean drainChunks(long start, long nanos) {
        while (!pending.isEmpty()) {
            long k = pending.pollFirst();
            level.setChunkForced(ChunkKey.x(k), ChunkKey.z(k), true);
            level.getChunk(ChunkKey.x(k), ChunkKey.z(k));
            if (System.nanoTime() - start > nanos) break;
        }
        return pending.isEmpty();
    }

    /** Libere les chunks charges de force (fin de construction ou abandon). */
    public void release() {
        for (long k : forced) level.setChunkForced(ChunkKey.x(k), ChunkKey.z(k), false);
        forced.clear();
    }

    private static final class ChunkKey {
        static long of(int x, int z) {
            return ((long) x << 32) | (z & 0xFFFFFFFFL);
        }

        static int x(long k) {
            return (int) (k >> 32);
        }

        static int z(long k) {
            return (int) k;
        }
    }

    // ------------------------------------------------------------------------------------------------ ecriture

    private void put(int lx, int y, int lz, BlockState s) {
        BlockPos at = new BlockPos(ox + lx, y, MineLayout.Z0 + lz);
        if (s.getBlock() instanceof MineOreBlock && !MineDensity.keep(at)) return;
        level.setBlock(at, s, FLAGS);
        placed++;
    }

    private void put(int lx, int y, int lz, Block b) {
        put(lx, y, lz, b.defaultBlockState());
    }

    private BlockState get(int lx, int y, int lz) {
        return level.getBlockState(new BlockPos(ox + lx, y, MineLayout.Z0 + lz));
    }

    private static Block pick(Block[] arr, int x, int y, int z, int salt) {
        return arr[(int) (MineLayout.rnd(x * 31 + y * 7, z + y * 13, salt) * arr.length) % arr.length];
    }

    private static BlockState hanging(Block b) {
        BlockState s = b.defaultBlockState();
        if (b instanceof PointedDripstoneBlock) return s.setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN);
        if (s.hasProperty(BlockStateProperties.FACING)) return s.setValue(BlockStateProperties.FACING, Direction.DOWN);
        return s;
    }

    private static boolean soft(Block b) {
        return b == Blocks.AIR;
    }

    private boolean free(int lx, int lz) {
        return L.kind(lx, lz) == 1 && !L.isPillar(lx, lz) && L.liquid(lx, lz) <= L.floor(lx, lz);
    }

    // ------------------------------------------------------------------------------------------------ colonne

    private void column(int lx, int lz) {
        int k = L.kind(lx, lz);
        if (k == 0) return;
        Sector s = L.style(lx, lz);
        int f = L.floor(lx, lz), c = L.ceil(lx, lz);
        int minF = f, maxC = c;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!L.in(lx + dx, lz + dz) || L.kind(lx + dx, lz + dz) == 0) continue;
                minF = Math.min(minF, L.floor(lx + dx, lz + dz));
                maxC = Math.max(maxC, L.ceil(lx + dx, lz + dz));
            }
        }
        if (k == 2) {                                              // paroi : pleine de bas en haut
            for (int y = minF - 3; y <= maxC + 3; y++) put(lx, y, lz, pick(s.wall(), lx, y, lz, 3));
            return;
        }
        // sol
        for (int y = minF - 3; y < f - 2; y++) put(lx, y, lz, pick(s.wall(), lx, y, lz, 4));
        put(lx, f - 2, lz, pick(s.floorSub(), lx, f - 2, lz, 5));
        put(lx, f - 1, lz, pick(s.floorSub(), lx, f - 1, lz, 6));
        // plafond
        for (int y = c; y <= maxC + 3; y++) put(lx, y, lz, pick(s.ceiling(), lx, y, lz, 7));

        if (L.isPillar(lx, lz)) {
            boolean edge = !L.isPillar(lx + 1, lz) || !L.isPillar(lx - 1, lz) || !L.isPillar(lx, lz + 1) || !L.isPillar(lx, lz - 1);
            put(lx, f, lz, pick(s.wall(), lx, f, lz, 8));
            MineBlocks.Ore ore = MineBlocks.ore(s.ore());
            for (int y = f + 1; y < c; y++) {
                if (edge && y - f <= 4 && MineLayout.rnd(lx * 5 + y, lz, 9) < 0.07) put(lx, y, lz, MineBlocks.block(ore));
                else put(lx, y, lz, pick(s.wall(), lx, y, lz, 10));
            }
            return;
        }
        put(lx, f, lz, pick(s.floorTop(), lx, f, lz, 11));
        int liquid = L.liquid(lx, lz);
        if (liquid > f && s.liquid() != Blocks.AIR) {
            for (int y = f + 1; y <= liquid; y++) put(lx, y, lz, s.liquid());
            return;
        }
        if (liquid > f) return;                                    // bassin sans liquide a cet etage : on laisse la cuvette
        decor(lx, lz, f, c, s);
    }

    // ------------------------------------------------------------------------------------------------ decoration

    private void decor(int lx, int lz, int f, int c, Sector s) {
        double r = MineLayout.rnd(lx, lz, 20);
        int nearPlaza = (int) Math.hypot(lx - MineLayout.PLAZA_X, lz - MineLayout.PLAZA_Z);
        // lumieres noyees dans le sol (une pour ~150 cases)
        if (r < 0.0065 && nearPlaza > 20) {
            put(lx, f, lz, s.light());
            return;
        }
        // stalactites de roche
        if (r > 0.9935 && c - f > 14 && nearPlaza > 12) {
            int len = 3 + (int) (MineLayout.rnd(lx, lz, 21) * Math.min(14, (c - f - 8) / 2.0));
            for (int i = 0; i < len; i++) put(lx, c - 1 - i, lz, pick(s.ceiling(), lx, c - i, lz, 22));
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                if (L.kind(lx + d[0], lz + d[1]) == 1 && !L.isPillar(lx + d[0], lz + d[1]) && L.ceil(lx + d[0], lz + d[1]) == c) {
                    put(lx + d[0], c - 1, lz + d[1], pick(s.ceiling(), lx, c, lz, 23));
                    if (len > 6) put(lx + d[0], c - 2, lz + d[1], pick(s.ceiling(), lx, c - 1, lz, 24));
                }
            }
            return;
        }
        double p = MineLayout.rnd(lx, lz, 25);
        if (p < 0.035) {
            Block plant = pick(s.plants(), lx, f, lz, 26);
            if (plant != Blocks.AIR) put(lx, f + 1, lz, plant);
        } else if (p < 0.0425 && c - f > 8) {
            put(lx, c - 1, lz, hanging(pick(s.hanging(), lx, c, lz, 27)));
        }
        double q = MineLayout.rnd(lx, lz, 30);
        if (q < 0.0028 && nearPlaza > 18) post(lx, lz, f, s);
        else if (q > 0.9988 && s.index() <= 2 && nearPlaza > 24) arch(lx, lz, f);
        else if (q > 0.9962 && q <= 0.9966 && s.index() <= 3 && nearPlaza > 30) camp(lx, lz, f);
        ores(lx, lz, f, s, q);
    }

    private void post(int lx, int lz, int f, Sector s) {
        if (s.index() == 4) {
            put(lx, f + 1, lz, Blocks.OBSIDIAN);
            put(lx, f + 2, lz, Blocks.END_ROD);
            return;
        }
        put(lx, f + 1, lz, Blocks.OAK_FENCE);
        put(lx, f + 2, lz, Blocks.OAK_FENCE);
        put(lx, f + 3, lz, s.index() == 1 ? Blocks.SOUL_LANTERN : Blocks.LANTERN);
    }

    /** Charpente de mine : deux poteaux de bois et une poutre, une lanterne suspendue au milieu. */
    private void arch(int lx, int lz, int f) {
        for (int dx = -2; dx <= 2; dx++) {
            if (!free(lx + dx, lz) || Math.abs(L.floor(lx + dx, lz) - f) > 1) return;
        }
        int top = f + 5;
        for (int sx : new int[]{-2, 2}) {
            int gf = L.floor(lx + sx, lz);
            for (int y = gf + 1; y < top; y++) put(lx + sx, y, lz, Blocks.STRIPPED_OAK_LOG);
        }
        for (int dx = -2; dx <= 2; dx++) put(lx + dx, top, lz, Blocks.OAK_PLANKS);
        put(lx - 1, top, lz, Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.X));
        put(lx + 1, top, lz, Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.X));
        put(lx, top - 1, lz, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
    }

    /** Campement de mineurs : plancher de planches, table d'etabli, tonneaux, four et feu de camp. */
    private void camp(int lx, int lz, int f) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (!free(lx + dx, lz + dz) || Math.abs(L.floor(lx + dx, lz + dz) - f) > 1) return;
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int gf = L.floor(lx + dx, lz + dz);
                for (int y = gf + 1; y <= f; y++) put(lx + dx, y, lz + dz, Blocks.SPRUCE_PLANKS);
                put(lx + dx, f, lz + dz, ((dx + dz) & 1) == 0 ? Blocks.SPRUCE_PLANKS : Blocks.DARK_OAK_PLANKS);
                for (int y = f + 1; y <= f + 4; y++) put(lx + dx, y, lz + dz, Blocks.AIR);
            }
        }
        for (int[] corner : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            put(lx + corner[0], f + 1, lz + corner[1], Blocks.OAK_FENCE);
            put(lx + corner[0], f + 2, lz + corner[1], Blocks.OAK_FENCE);
            put(lx + corner[0], f + 3, lz + corner[1], Blocks.LANTERN);
        }
        put(lx, f + 1, lz, Blocks.CAMPFIRE);
        put(lx - 2, f + 1, lz - 2, Blocks.CRAFTING_TABLE);
        put(lx + 2, f + 1, lz - 2, Blocks.BARREL);
        put(lx + 2, f + 1, lz + 2, Blocks.BARREL);
        put(lx - 2, f + 1, lz + 2, Blocks.FURNACE);
    }

    /** Amas de minerai poses sur le sol (et parfois empiles) ; l'etage donne sa propre veine, les precedents quelques restes. */
    private void ores(int lx, int lz, int f, Sector s, double q) {
        if (Math.hypot(lx - MineLayout.PLAZA_X, lz - MineLayout.PLAZA_Z) < 26) return;
        double v = MineLayout.rnd(lx, lz, 40);
        int size;
        if (v < 0.0105) size = 3 + (int) (MineLayout.rnd(lx, lz, 41) * 4);
        else if (v > 0.9993) size = 14 + (int) (MineLayout.rnd(lx, lz, 41) * 8);       // gros filon
        else return;
        int tier = s.index();
        for (int i = 0; i < size; i++) {
            int dx = (int) Math.round((MineLayout.rnd(lx + i, lz, 42) - 0.5) * (size > 10 ? 6 : 3));
            int dz = (int) Math.round((MineLayout.rnd(lx, lz + i, 43) - 0.5) * (size > 10 ? 6 : 3));
            int x = lx + dx, z = lz + dz;
            if (!free(x, z)) continue;
            int t = tier > 0 && MineLayout.rnd(x, z, 44) < 0.12 ? tier - 1 : tier;
            Block ore = MineBlocks.block(MineBlocks.ORES.get(t));
            int fy = L.floor(x, z);
            put(x, fy + 1, z, ore);
            if (MineLayout.rnd(x, z, 45) < 0.4) put(x, fy + 2, z, ore);
        }
    }

    // ------------------------------------------------------------------------------------------------ echelles et place d'entree

    private void ladders(MineLayout.Feature ch) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        Direction[] face = {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
        for (int i = 0; i < 4; i++) {
            int x = ch.x(), z = ch.z();
            for (int d = 0; d < ch.r() + 8; d++) {
                int nx = x + dirs[i][0], nz = z + dirs[i][1];
                if (L.floor(nx, nz) - L.floor(x, z) >= 3) {      // le bord de la falaise : (x, z) est contre la paroi
                    int low = L.floor(x, z), high = L.floor(nx, nz);
                    for (int y = low + 1; y <= high; y++) put(x, y, z, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, face[i]));
                    break;
                }
                x = nx;
                z = nz;
            }
        }
    }

    /** Place d'entree : disque de marbre, anneau de dalles runiques, quatre colonnes et un grand portique face a la descente. */
    private void plaza() {
        int cx = MineLayout.PLAZA_X, cz = MineLayout.PLAZA_Z;
        int f = L.floor(cx, cz);
        for (int dx = -14; dx <= 14; dx++) {
            for (int dz = -14; dz <= 14; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > 14) continue;
                Block b = d < 3.5 ? DecorBlocks.get("marbre_dore") : d < 6 ? DecorBlocks.get("marbre_poli")
                        : d >= 10.5 && d < 12 ? DecorBlocks.get("dalle_runique") : d >= 13 ? DecorBlocks.get("fer_noirci") : DecorBlocks.get("marbre");
                put(cx + dx, f, cz + dz, b);
                for (int y = f + 1; y <= f + 10; y++) {
                    BlockState cur = get(cx + dx, y, cz + dz);
                    if (!cur.isAir()) put(cx + dx, y, cz + dz, Blocks.AIR);
                }
            }
        }
        for (int[] p : new int[][]{{-9, -9}, {9, -9}, {-9, 9}, {9, 9}}) {
            for (int y = 1; y <= 6; y++) put(cx + p[0], f + y, cz + p[1], DecorBlocks.get("colonne_marbre"));
            put(cx + p[0], f + 7, cz + p[1], DecorBlocks.get("cristal_ambre"));
        }
        // portique vers la descente (au sud de la place)
        for (int sx : new int[]{-5, 5}) {
            for (int y = 1; y <= 9; y++) put(cx + sx, f + y, cz + 12, DecorBlocks.get("colonne_marbre"));
            put(cx + sx, f + 10, cz + 12, DecorBlocks.get("cristal_azur"));
        }
        for (int dx = -5; dx <= 5; dx++) {
            put(cx + dx, f + 10, cz + 12, DecorBlocks.get(dx == 0 ? "cristal_arcane" : "marbre_dore"));
            put(cx + dx, f + 11, cz + 12, DecorBlocks.get("marbre_poli"));
        }
        // arche du retour vers le Spawn, au nord de la place
        for (int sx : new int[]{-5, 5}) {
            for (int y = 1; y <= 9; y++) put(cx + sx, f + y, cz - 12, DecorBlocks.get("colonne_marbre"));
            put(cx + sx, f + 10, cz - 12, DecorBlocks.get("cristal_azur"));
        }
        for (int dx = -5; dx <= 5; dx++) {
            put(cx + dx, f + 10, cz - 12, DecorBlocks.get(dx == 0 ? "cristal_arcane" : "marbre_dore"));
            put(cx + dx, f + 11, cz - 12, DecorBlocks.get("marbre_poli"));
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int y = 1; y <= 8; y++) put(cx + dx, f + y, cz - 12, Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.LIME));
        }
        text(cx, f + 14.0, cz - 9, "RETOUR AU SPAWN", "#E0B0FF", 4F, "retour");
        text(cx, f + 17.0, cz - 9, L.style.name().toUpperCase(Locale.ROOT), "#FFD060", 5.6F, "titre");
        text(cx, f + 12.2, cz - 9, "Niveaux " + L.style.minLevel() + " à " + L.style.maxLevel() + " · une mine par palier", "#F0E8D8", 2.2F, "sous-titre");
        text(cx, f + 5.0, cz + 6, hint(), "#FFE6A0", 1.6F, "rumeur");
        // brasero central
        put(cx, f + 1, cz, Blocks.CAMPFIRE);
        for (int[] p : new int[][]{{-4, 0}, {4, 0}, {0, -4}, {0, 4}}) {
            put(cx + p[0], f + 1, cz + p[1], DecorBlocks.get("cristal_ambre"));
        }
    }

    // ------------------------------------------------------------------------------------------------ portail cache et totems

    private String hint() {
        if (region >= MineLayout.SECTORS - 1) return "Rumeur : la dernière mine garde ses secrets… pour l'instant.";
        int dx = L.exitX - MineLayout.PLAZA_X;
        String where = "tout au fond" + (dx > 40 ? ", vers l'est" : dx < -40 ? ", vers l'ouest" : "");
        return "Rumeur : le portail vers « " + MineLayout.STYLES.get(region + 1).name() + " » se cache " + where + " de la caverne.";
    }

    /** Recoin au fond de la caverne : une arche de pierre dont le voile mene a la mine suivante (ou l'autel de la derniere). */
    private void exitPortal() {
        int ex = L.exitX, ez = L.exitZ, f = L.floor(ex, ez);
        Sector s = L.style;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                double r = Math.hypot(dx, dz);
                if (r > 10.4) continue;
                int x = ex + dx, z = ez + dz;
                if (!L.in(x, z) || L.kind(x, z) != 1) continue;
                put(x, f, z, r < 7.5 ? (MineLayout.rnd(x, z, 60) < 0.5 ? Blocks.STONE_BRICKS : MineLayout.rnd(x, z, 61) < 0.5 ? Blocks.CRACKED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS) : pick(s.floorTop(), x, f, z, 62));
                for (int y = f + 1; y <= f + 12; y++) put(x, y, z, Blocks.AIR);
            }
        }
        for (int i = 0; i < 7; i++) {                                                         // colonnes en ruine tout autour
            double a = Math.PI * 2 * i / 7 + 0.3;
            int x = ex + (int) Math.round(Math.cos(a) * 8), z = ez + (int) Math.round(Math.sin(a) * 8);
            if (!free(x, z)) continue;
            int hh = 2 + (int) (MineLayout.rnd(x, z, 63) * 5);
            for (int y = 1; y <= hh; y++) put(x, f + y, z, Blocks.DEEPSLATE_BRICKS);
        }
        for (int sx : new int[]{-4, 4}) {
            for (int y = 1; y <= 9; y++) put(ex + sx, f + y, ez, y % 4 == 0 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS);
            put(ex + sx, f + 10, ez, Blocks.LANTERN);
        }
        for (int dx = -4; dx <= 4; dx++) put(ex + dx, f + 9, ez, Blocks.STONE_BRICK_SLAB);
        if (region >= MineLayout.SECTORS - 1) {
            put(ex, f + 1, ez, Blocks.CRYING_OBSIDIAN);
            put(ex, f + 2, ez, Blocks.END_ROD);
            text(ex, f + 6, ez + 2, "Autel du Néant", "#C090FF", 3.2F, "sortie");
            text(ex, f + 4.4, ez + 2, "Fin de la dernière mine", "#F0E8D8", 1.8F, "sortie2");
            return;
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int y = 1; y <= 8; y++) put(ex + dx, f + y, ez, Blocks.STAINED_GLASS.pick(VEIL[region]));
        }
        put(ex, f, ez - 1, Blocks.SEA_LANTERN);
        for (int y = 11; y <= 18; y++) put(ex, f + y, ez, Blocks.END_ROD);
        text(ex, f + 12.4, ez - 2, "Portail vers " + MineLayout.STYLES.get(region + 1).name(), VEIL_COLOR[region], 3.4F, "sortie");
    }

    /** Totem au centre d'une zone de niveau : il annonce les niveaux des monstres de la zone. */
    private void totem(com.mmorpg.world.LevelZones.Cell c) {
        int x = c.cx(), z = c.cz(), f = L.floor(x, z);
        Sector s = L.style;
        for (int y = 1; y <= 4; y++) put(x, f + y, z, pick(s.wall(), x, f + y, z, 70));
        put(x, f + 5, z, s.light());
        int base = com.mmorpg.world.LevelZones.tierLevel(c.tier(), s.minLevel(), s.maxLevel());
        int lo = Math.max(s.minLevel(), base - 1), hi = Math.min(s.maxLevel(), base + 1);
        text(x, f + 7.2, z, "Zone niveau " + lo + (hi > lo ? " à " + hi : ""), TIER_COLORS[c.tier()], 2.6F, "totem" + c.gx() + "_" + c.gz());
    }

    private void text(double lx, double y, double lz, String text, String color, float scale, String key) {
        UUID id = UUID.nameUUIDFromBytes(("mmorpg:mines:" + region + ":" + key).getBytes(StandardCharsets.UTF_8));
        String snbt = String.format(Locale.ROOT, "{text:{text:\"%s\",font:\"mmorpg:aeloria\",color:\"%s\"},billboard:\"center\",background:0,shadow:true,"
                        + "see_through:false,line_width:600,Tags:[\"%s\"],UUID:[I;%d,%d,%d,%d],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                        + "translation:[0f,0f,0f],scale:[%.2ff,%.2ff,%.2ff]}}", text.replace("\"", "\\\""), color, TAG,
                (int) (id.getMostSignificantBits() >> 32), (int) id.getMostSignificantBits(), (int) (id.getLeastSignificantBits() >> 32),
                (int) id.getLeastSignificantBits(), scale, scale, scale);
        var src = level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(src, String.format(Locale.ROOT, "summon minecraft:text_display %.2f %.2f %.2f %s", ox + lx + 0.5, y, MineLayout.Z0 + lz + 0.5, snbt));
    }

    private void clearTexts() {
        var box = new net.minecraft.world.phys.AABB(ox - 8, 0, MineLayout.Z0 - 8, ox + MineLayout.SIZE + 8, 256, MineLayout.Z0 + MineLayout.SIZE + 8);
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, box, x -> x instanceof net.minecraft.world.entity.Display.TextDisplay && x.entityTags().contains(TAG))) e.discard();
    }
}
