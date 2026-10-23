package com.mmorpg.plains;

import com.mmorpg.plains.PlainsLayout.Sector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static com.mmorpg.plains.PlainsLayout.*;

/**
 * Construit les Plaines d'Aeloria rangee par rangee (du nord au sud) avec un budget de temps par tick : sol, lacs, falaise, flore,
 * arbres et rochers, puis la place d'entree, les campements et les portiques qui separent les regions.
 */
public final class PlainsBuilder {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final int SLAB = 32;
    /** Etiquette des textes flottants des plaines (ils sont retires avant une reconstruction). */
    public static final String TAG = "mmorpg_plains";
    private static final String[] COLORS = {"#8CE68C", "#70C8A0", "#D8D0B8", "#A8E0FF", "#FF9A60", "#C090FF"};

    private final ServerLevel level;
    private final PlainsLayout lay = PlainsLayout.get();
    private final Set<Long> forced = new HashSet<>();
    private int row = 0;
    private int slabEnd = 0;
    private boolean plazaDone = false;
    private int campsDone = 0;
    private int gatesDone = 1;
    private long placed = 0;

    public PlainsBuilder(ServerLevel level) {
        this.level = level;
    }

    public int percent() {
        return Math.min(100, row * 100 / L);
    }

    public long blocksPlaced() {
        return placed;
    }

    /** Avance la construction pendant au plus {@code nanos} nanosecondes ; renvoie vrai quand tout est construit. */
    public boolean step(long nanos) {
        long start = System.nanoTime();
        if (row == 0 && slabEnd == 0) clearTexts();
        while (row < L) {
            if (row >= slabEnd) nextSlab();
            for (int lx = 0; lx < W; lx++) column(lx, row);
            row++;
            if (!plazaDone && row >= PLAZA_Z + 16) {
                plaza();
                plazaDone = true;
            }
            while (campsDone < SECTORS && row > campZ(campsDone) + 8) camp(campsDone++);
            while (gatesDone < SECTORS && row > sectorStart(gatesDone) + 4) gate(gatesDone++);
            if (System.nanoTime() - start > nanos) return false;
        }
        release();
        return true;
    }

    private void nextSlab() {
        int slabStart = row;
        slabEnd = Math.min(L, row + SLAB);
        int cx0 = (X0 - 16) >> 4, cx1 = (X0 + W + 16) >> 4;
        int cz0 = (Z0 + slabStart - 16) >> 4, cz1 = (Z0 + slabEnd + 16) >> 4;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (forced.add(key(cx, cz))) {
                    level.setChunkForced(cx, cz, true);
                    level.getChunk(cx, cz);
                }
            }
        }
        int keep = (Z0 + slabStart - 48) >> 4;
        forced.removeIf(k -> {
            int cz = (int) (long) k;
            if (cz >= keep) return false;
            level.setChunkForced((int) (k >> 32), cz, false);
            return true;
        });
    }

    /** Libere les chunks charges de force (fin de construction ou abandon). */
    public void release() {
        for (long k : forced) level.setChunkForced((int) (k >> 32), (int) k, false);
        forced.clear();
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    // ------------------------------------------------------------------------------------------------ ecriture

    private void put(int lx, int y, int lz, BlockState s) {
        level.setBlock(new BlockPos(X0 + lx, y, Z0 + lz), s, FLAGS);
        placed++;
    }

    private void put(int lx, int y, int lz, Block b) {
        put(lx, y, lz, b.defaultBlockState());
    }

    private void putFree(int lx, int y, int lz, BlockState s) {
        if (!inside(lx, lz)) return;
        if (level.getBlockState(new BlockPos(X0 + lx, y, Z0 + lz)).isAir()) put(lx, y, lz, s);
    }

    private void putFree(int lx, int y, int lz, Block b) {
        putFree(lx, y, lz, b.defaultBlockState());
    }

    private static BlockState leaves(Block b) {
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static double rnd(int lx, int lz, int salt) {
        return hash(lx, lz, salt);
    }

    // ------------------------------------------------------------------------------------------------ colonne

    private void column(int lx, int lz) {
        int h = lay.height(lx, lz);
        Sector s = lay.sector(lx, lz);
        if (cliff(lx, lz)) {
            for (int y = BASE - 6; y <= h; y++) put(lx, y, lz, s.rock()[(int) (rnd(lx + y * 7, lz, 3) * s.rock().length)]);
            return;
        }
        boolean wet = h < BASE;
        BlockState top;
        if (wet) top = (s.index() == 2 || s.index() == 3 ? Blocks.GRAVEL : s.index() == 5 ? Blocks.END_STONE : Blocks.SAND).defaultBlockState();
        else if (onPath(lx, lz)) top = pathBlock(s).defaultBlockState();
        else top = s.top()[(int) (rnd(lx, lz, 4) * s.top().length)].defaultBlockState();
        for (int y = h - 5; y <= h - 3; y++) put(lx, y, lz, Blocks.STONE);
        for (int y = h - 2; y <= h - 1; y++) put(lx, y, lz, wet ? top : s.sub().defaultBlockState());
        put(lx, h, lz, top);
        if (wet) {
            for (int y = h + 1; y < BASE; y++) put(lx, y, lz, Blocks.WATER);
            put(lx, BASE, lz, s.water() == Blocks.ICE ? Blocks.ICE : Blocks.WATER);
            if (s.index() == 1 && rnd(lx, lz, 6) < 0.05) put(lx, BASE + 1, lz, Blocks.LILY_PAD);
            return;
        }
        double d = Math.hypot(lx - PLAZA_X, lz - PLAZA_Z);
        if (d < 15 || onPath(lx, lz) || edge(lx, lz) < WALL + 3) return;
        double r = rnd(lx, lz, 7);
        if (rnd(lx, lz, 8) < treeDensity(s)) {
            tree(lx, h, lz, s);
        } else if (rnd(lx, lz, 9) < 0.003) {
            boulder(lx, h, lz, s);
        } else {
            flora(lx, h, lz, s, r);
        }
    }

    private static Block pathBlock(Sector s) {
        return switch (s.index()) {
            case 3 -> Blocks.GRAVEL;
            case 4 -> Blocks.SMOOTH_RED_SANDSTONE;
            case 5 -> Blocks.STONE_BRICKS;
            default -> Blocks.DIRT_PATH;
        };
    }

    private static double treeDensity(Sector s) {
        return switch (s.index()) {
            case 0 -> 0.011;
            case 1 -> 0.013;
            case 2 -> 0.006;
            case 3 -> 0.015;
            case 5 -> 0.004;
            default -> 0;
        };
    }

    // ------------------------------------------------------------------------------------------------ flore

    private void flora(int lx, int h, int lz, Sector s, double r) {
        Block b = null;
        switch (s.index()) {
            case 0 -> b = r < 0.55 ? Blocks.SHORT_GRASS : r < 0.60 ? Blocks.FERN : r < 0.63 ? Blocks.DANDELION : r < 0.66 ? Blocks.POPPY : r < 0.68 ? Blocks.CORNFLOWER
                    : r < 0.70 ? Blocks.OXEYE_DAISY : r < 0.72 ? Blocks.AZURE_BLUET : null;
            case 1 -> b = r < 0.40 ? Blocks.FERN : r < 0.62 ? Blocks.SHORT_GRASS : r < 0.65 ? Blocks.BLUE_ORCHID : r < 0.68 ? Blocks.BROWN_MUSHROOM : r < 0.69 ? Blocks.RED_MUSHROOM : null;
            case 2 -> b = r < 0.12 ? Blocks.DEAD_BUSH : r < 0.135 ? Blocks.BONE_BLOCK : r < 0.14 ? Blocks.COBWEB : null;
            case 4 -> b = r < 0.10 ? Blocks.DEAD_BUSH : null;
            case 5 -> b = r < 0.05 ? Blocks.SMALL_AMETHYST_BUD : r < 0.07 ? Blocks.MEDIUM_AMETHYST_BUD : r < 0.08 ? Blocks.AMETHYST_CLUSTER : null;
            default -> { }
        }
        if (s.index() == 4 && r > 0.985) {                                                       // cactus de 1 a 3 blocs
            int hh = 1 + (int) (rnd(lx, lz, 12) * 3);
            for (int i = 1; i <= hh; i++) put(lx, h + i, lz, Blocks.CACTUS);
        } else if (b != null) {
            putFree(lx, h + 1, lz, b);
        }
    }

    private void boulder(int lx, int h, int lz, Sector s) {
        int r = 1 + (int) (rnd(lx, lz, 13) * 2);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = 0; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy * 1.6 + dz * dz > r * r + 0.8) continue;
                    putFree(lx + dx, h + 1 + dy, lz + dz, s.rock()[(int) (rnd(lx + dx, lz + dz + dy, 14) * s.rock().length)]);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ arbres

    private void tree(int lx, int h, int lz, Sector s) {
        int th = 4 + (int) (rnd(lx, lz, 15) * 3);
        switch (s.index()) {
            case 0, 1 -> {
                boolean birch = s.index() == 0 && rnd(lx, lz, 16) < 0.3;
                boolean dark = s.index() == 1;
                Block log = birch ? Blocks.BIRCH_LOG : dark ? Blocks.DARK_OAK_LOG : Blocks.OAK_LOG;
                BlockState lv = leaves(birch ? Blocks.BIRCH_LEAVES : dark ? Blocks.DARK_OAK_LEAVES : Blocks.OAK_LEAVES);
                for (int y = 1; y <= th; y++) put(lx, h + y, lz, log);
                for (int dy = th - 2; dy <= th + 1; dy++) {
                    int rad = dy >= th ? 1 : 2;
                    for (int dx = -rad; dx <= rad; dx++) {
                        for (int dz = -rad; dz <= rad; dz++) {
                            if (Math.abs(dx) == rad && Math.abs(dz) == rad && (rad == 2 || dy == th + 1 || rnd(lx + dx, lz + dz, 17 + dy) < 0.5)) continue;
                            putFree(lx + dx, h + dy, lz + dz, lv);
                        }
                    }
                }
            }
            case 2 -> {                                                                       // arbre mort : tronc blanchi et moignons
                for (int y = 1; y <= th; y++) put(lx, h + y, lz, Blocks.STRIPPED_BIRCH_LOG);
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (rnd(lx + d.getStepX(), lz + d.getStepZ(), 18) < 0.5) {
                        int by = h + th - 1 - (int) (rnd(lx, lz, 19 + d.ordinal()) * 2);
                        put(lx + d.getStepX(), by, lz + d.getStepZ(), Blocks.STRIPPED_BIRCH_LOG);
                        if (rnd(lx, lz, 25 + d.ordinal()) < 0.5) put(lx + d.getStepX(), by + 1, lz + d.getStepZ(), Blocks.STRIPPED_BIRCH_LOG);
                    }
                }
            }
            case 3 -> {                                                                       // epicea enneige
                int sh = th + 3;
                BlockState lv = leaves(Blocks.SPRUCE_LEAVES);
                for (int y = 1; y <= sh; y++) put(lx, h + y, lz, Blocks.SPRUCE_LOG);
                for (int dy = 2; dy <= sh + 1; dy++) {
                    int rad = Math.min(3, (sh + 1 - dy) / 2 + ((sh - dy) % 2 == 0 ? 1 : 0));
                    if (dy == sh + 1) rad = 0;
                    for (int dx = -rad; dx <= rad; dx++) {
                        for (int dz = -rad; dz <= rad; dz++) {
                            if (Math.abs(dx) + Math.abs(dz) > rad + (rad > 1 ? 1 : 0) || (dx == 0 && dz == 0 && dy <= sh)) continue;
                            putFree(lx + dx, h + dy, lz + dz, lv);
                        }
                    }
                }
                putFree(lx, h + sh + 2, lz, Blocks.SNOW);
            }
            default -> {                                                                       // fleche de cristal
                int sh = 4 + (int) (rnd(lx, lz, 20) * 5);
                for (int y = 1; y <= sh; y++) put(lx, h + y, lz, y % 3 == 0 ? Blocks.CALCITE : Blocks.AMETHYST_BLOCK);
                put(lx, h + sh + 1, lz, Blocks.END_ROD);
                for (Direction d : Direction.Plane.HORIZONTAL) putFree(lx + d.getStepX(), h + 1, lz + d.getStepZ(), Blocks.AMETHYST_CLUSTER);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ place d'entree

    private void plaza() {
        int px = PLAZA_X, pz = PLAZA_Z, y = BASE;
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                double r = Math.hypot(dx, dz);
                if (r > 10.6) continue;
                BlockState top = r < 2.6 ? Blocks.SMOOTH_QUARTZ.defaultBlockState()
                        : r < 3.6 ? Blocks.GOLD_BLOCK.defaultBlockState()
                        : r < 6.5 ? (Math.abs(dx) <= 1 || Math.abs(dz) <= 1 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS).defaultBlockState()
                        : r < 9.5 ? (rnd(px + dx, pz + dz, 30) < 0.5 ? Blocks.STONE_BRICKS : rnd(px + dx, pz + dz, 31) < 0.5 ? Blocks.MOSSY_STONE_BRICKS : Blocks.COBBLESTONE).defaultBlockState()
                        : (rnd(px + dx, pz + dz, 32) < 0.55 ? Blocks.COBBLESTONE : Blocks.GRAVEL).defaultBlockState();
                put(px + dx, y, pz + dz, top);
                for (int up = 1; up <= 12; up++) put(px + dx, y + up, pz + dz, Blocks.AIR);
            }
        }
        // lampadaires et parterres
        for (int sx : new int[]{-8, 8}) {
            for (int sz : new int[]{-5, 6}) {
                for (int up = 1; up <= 3; up++) put(px + sx, y + up, pz + sz, Blocks.COBBLESTONE_WALL);
                put(px + sx, y + 4, pz + sz, Blocks.LANTERN);
            }
        }
        for (int dx : new int[]{-5, 5}) {
            put(px + dx, y + 1, pz + 8, Blocks.POPPY);
            put(px + dx + 1, y + 1, pz + 8, Blocks.CORNFLOWER);
            put(px + dx - 1, y + 1, pz + 8, Blocks.DANDELION);
        }
        // arche du retour vers le Spawn, au nord : piliers de quartz, linteau dore et voile vert
        int az = ARCH_Z;
        for (int sx : new int[]{-5, 5}) {
            for (int up = 1; up <= 9; up++) put(px + sx, y + up, az, up % 4 == 0 ? Blocks.CHISELED_QUARTZ_BLOCK : Blocks.QUARTZ_PILLAR);
            put(px + sx, y + 10, az, Blocks.GOLD_BLOCK);
            put(px + sx, y + 11, az, Blocks.END_ROD);
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) put(px + sx + dx, y, az + dz, Blocks.SMOOTH_QUARTZ);
        }
        for (int dx = -5; dx <= 5; dx++) {
            put(px + dx, y + 9, az, Blocks.SMOOTH_QUARTZ);
            put(px + dx, y + 10, az, Math.abs(dx) == 5 ? Blocks.GOLD_BLOCK : Blocks.POLISHED_ANDESITE);
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int up = 1; up <= 8; up++) {
                if (Math.abs(dx) <= 4 - Math.max(0, up - 5)) put(px + dx, y + up, az, Blocks.STAINED_GLASS.pick(DyeColor.LIME));
            }
        }
        for (int dx = -4; dx <= 4; dx += 2) put(px + dx, y + 8, az + 1, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        text(px, y + 14.2, az + 3, "RETOUR AU SPAWN", "#E0B0FF", 4F, "retour");
        text(px, y + 17.5, az + 3, "PLAINES D'AELORIA", "#8CE68C", 7F, "titre");
        text(px, y + 12.4, az + 3, "Farm en plein air · niveaux 1 à 98", "#F0E8D8", 2.4F, "sous-titre");
    }

    /** Position de l'arche du retour (colonne locale z) : le portail de la place. */
    public static final int ARCH_Z = 6;

    // ------------------------------------------------------------------------------------------------ campements et portiques

    private static int campZ(int k) {
        return sectorStart(k) + 6;
    }

    private void camp(int k) {
        int cz = campZ(k);
        int px = pathX(cz);
        int cx = px + 7, gh = lay.height(cx, cz);
        for (int off : new int[]{7, -7, 9, -9, 12, -12}) {                                      // un terrain sec et a peu pres plat, pres du chemin
            int x = px + off;
            boolean ok = true;
            int c = lay.height(x, cz);
            for (int dx = -4; dx <= 4 && ok; dx += 2) {
                for (int dz = -4; dz <= 4 && ok; dz += 2) ok = lay.height(x + dx, cz + dz) >= BASE + 1 && Math.abs(lay.height(x + dx, cz + dz) - c) <= 3;
            }
            if (ok) {
                cx = x;
                gh = c;
                break;
            }
        }
        gh = Math.max(gh, BASE + 1);
        Sector s = STYLES.get(k);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double r = Math.hypot(dx, dz);
                if (r > 4.3) continue;
                int x = cx + dx, z = cz + dz;
                int ground = lay.height(x, z);
                for (int y = Math.min(ground, gh) - 1; y < gh; y++) if (y >= BASE - 4) put(x, y, z, s.sub());
                for (int y = gh + 1; y <= gh + 7; y++) put(x, y, z, Blocks.AIR);
                put(x, gh, z, r < 3.5 ? (rnd(x, z, 40) < 0.5 ? Blocks.COBBLESTONE : Blocks.GRAVEL) : s.top()[0]);
            }
        }
        put(cx, gh + 1, cz, Blocks.CAMPFIRE);
        put(cx - 2, gh + 1, cz + 1, Blocks.HAY_BLOCK);
        put(cx - 2, gh + 1, cz - 1, Blocks.HAY_BLOCK);
        put(cx - 2, gh + 2, cz, Blocks.HAY_BLOCK);
        put(cx + 2, gh + 1, cz + 2, Blocks.BARREL);
        for (int up = 1; up <= 3; up++) put(cx + 3, gh + up, cz - 2, Blocks.OAK_FENCE);
        put(cx + 3, gh + 4, cz - 2, Blocks.LANTERN);
        for (int up = 1; up <= 3; up++) put(cx - 3, gh + up, cz + 3, Blocks.OAK_FENCE);
        put(cx - 3, gh + 4, cz + 3, Blocks.LANTERN);
        text(cx, gh + 3.6, cz, "Campement", "#FFD9A0", 2.2F, "camp" + k);
    }

    private void gate(int k) {
        int z = sectorStart(k);
        int px = pathX(z);
        Sector s = STYLES.get(k);
        int top = BASE;
        for (int side : new int[]{-6, 6}) {
            int x = px + side;
            int g = Math.max(lay.height(x, z), BASE + 1);
            top = Math.max(top, g);
        }
        for (int side : new int[]{-6, 6}) {
            int x = px + side;
            for (int y = Math.max(BASE - 2, lay.height(x, z) - 2); y <= top + 7; y++) {
                put(x, y, z, y > top + 6 ? Blocks.SMOOTH_STONE_SLAB : s.rock()[(int) (rnd(x, y, 50) * s.rock().length)]);
            }
            put(x, top + 8, z, Blocks.LANTERN);
            for (int dz = -1; dz <= 1; dz += 2) for (int up = 1; up <= 2; up++) put(x, top + up, z + dz, s.rock()[0]);
        }
        for (int x = px - 5; x <= px + 5; x++) put(x, top + 7, z, Blocks.STONE_BRICK_SLAB);
        text(px, top + 11, z, s.name(), COLORS[k], 4.4F, "gate" + k);
        text(px, top + 9, z, "Niveaux " + s.minLevel() + " à " + s.maxLevel(), "#F0E8D8", 2.2F, "gatelvl" + k);
    }

    // ------------------------------------------------------------------------------------------------ textes flottants

    private void text(double lx, double y, double lz, String text, String color, float scale, String key) {
        UUID id = UUID.nameUUIDFromBytes(("mmorpg:plaines:" + key).getBytes(StandardCharsets.UTF_8));
        String snbt = String.format(Locale.ROOT, "{text:{text:\"%s\",font:\"mmorpg:aeloria\",color:\"%s\"},billboard:\"center\",background:0,shadow:true,"
                        + "see_through:false,line_width:600,Tags:[\"%s\"],UUID:[I;%d,%d,%d,%d],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                        + "translation:[0f,0f,0f],scale:[%.2ff,%.2ff,%.2ff]}}", text.replace("\"", "\\\""), color, TAG,
                (int) (id.getMostSignificantBits() >> 32), (int) id.getMostSignificantBits(), (int) (id.getLeastSignificantBits() >> 32),
                (int) id.getLeastSignificantBits(), scale, scale, scale);
        var src = level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(src, String.format(Locale.ROOT, "summon minecraft:text_display %.2f %.2f %.2f %s", X0 + lx + 0.5, y, Z0 + lz + 0.5, snbt));
    }

    private void clearTexts() {
        var box = new net.minecraft.world.phys.AABB(X0 - 8, 0, Z0 - 8, X0 + W + 8, 256, Z0 + L + 8);
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, box, x -> x instanceof net.minecraft.world.entity.Display.TextDisplay && x.entityTags().contains(TAG))) e.discard();
    }
}
