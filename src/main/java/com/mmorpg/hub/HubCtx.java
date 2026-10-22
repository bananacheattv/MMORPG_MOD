package com.mmorpg.hub;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Contexte de construction du hub : monde, origine, masques de l'ile et primitives de forme (disques, anneaux, murs cylindriques,
 * domes, ellipsoides, lignes) avec melanges de materiaux deterministes. Toutes les coordonnees sont relatives a l'origine.
 */
final class HubCtx {
    /** Choisit le bloc d'une cellule d'une forme ; {@code null} laisse la cellule intacte. */
    interface Pick {
        BlockState at(int x, int y, int z);
    }

    /** Idem pour un dome : {@code elev} (degres, 0 = base, 90 = sommet) et {@code az} (degres, 0..360). */
    interface DomePick {
        BlockState at(int x, int y, int z, double elev, double az);
    }

    final ServerLevel L;
    final BlockPos O;
    static final int X0 = -56, X1 = 80, Z0 = -112, Z1 = 70;
    boolean[][] foot;
    boolean[][] isle;
    float[][] edge;

    HubCtx(ServerLevel level, BlockPos origin) {
        this.L = level;
        this.O = origin;
    }

    // ------------------------------------------------------------------ blocs

    void set(int x, int y, int z, BlockState s) {
        Block b = s.getBlock();
        boolean shaped = b instanceof WallBlock || b instanceof CrossCollisionBlock || b instanceof IronBarsBlock;
        L.setBlock(O.offset(x, y, z), s, shaped ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS);
    }

    void set(int x, int y, int z, Block b) {
        set(x, y, z, b.defaultBlockState());
    }

    void set(int x, int y, int z, String decor) {
        set(x, y, z, com.mmorpg.registry.DecorBlocks.state(decor));
    }

    BlockState state(Object o) {
        if (o instanceof BlockState s) return s;
        if (o instanceof Block b) return b.defaultBlockState();
        return com.mmorpg.registry.DecorBlocks.state((String) o);
    }

    boolean air(int x, int y, int z) {
        return L.getBlockState(O.offset(x, y, z)).isAir();
    }

    BlockState get(int x, int y, int z) {
        return L.getBlockState(O.offset(x, y, z));
    }

    void setIfAir(int x, int y, int z, BlockState s) {
        if (air(x, y, z)) set(x, y, z, s);
    }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, Object block) {
        BlockState s = state(block);
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    set(x, y, z, s);
    }

    private Object variant(Object block, String suffix) {
        if (!(block instanceof String id)) return block;
        return com.mmorpg.registry.DecorBlocks.has(id + suffix) ? id + suffix : "marbre" + suffix;
    }

    BlockState stair(Object block, Direction facing, boolean top) {
        return state(variant(block, "_stairs")).setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    BlockState slab(Object block, boolean top) {
        return state(variant(block, "_slab")).setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    BlockState axis(Object block, Direction.Axis a) {
        BlockState st = state(block);
        return st.hasProperty(RotatedPillarBlock.AXIS) ? st.setValue(RotatedPillarBlock.AXIS, a) : st;
    }

    BlockState leaves(Block b) {
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    BlockState hanging(Block lantern) {
        return lantern.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    // ------------------------------------------------------------------ hasard et melanges

    float rnd(int x, int y, int z, int salt) {
        long v = x * 374761393L + y * 668265263L + z * 2147483647L + salt * 1274126177L;
        v = (v ^ (v >>> 13)) * 1274126177L;
        v ^= v >>> 16;
        return (v & 0xFFFFL) / 65535F;
    }

    float noise(float x, float z, float scale, int salt) {
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

    /** Melange pondere : {@code mix(x, y, z, salt, "marbre", 50, Blocks.QUARTZ_BRICKS, 20, ...)} (bloc ou identifiant de decor, poids). */
    BlockState mix(int x, int y, int z, int salt, Object... pairs) {
        int total = 0;
        for (int i = 1; i < pairs.length; i += 2) total += (Integer) pairs[i];
        float r = rnd(x, y, z, salt) * total;
        int acc = 0;
        for (int i = 0; i < pairs.length; i += 2) {
            acc += (Integer) pairs[i + 1];
            if (r < acc) return state(pairs[i]);
        }
        return state(pairs[0]);
    }

    // ------------------------------------------------------------------ formes

    void disc(int cx, int y, int cz, double r, Pick p) {
        int n = (int) Math.ceil(r) + 1;
        for (int x = cx - n; x <= cx + n; x++) {
            for (int z = cz - n; z <= cz + n; z++) {
                if (Math.hypot(x - cx, z - cz) > r + 0.35) continue;
                BlockState s = p.at(x, y, z);
                if (s != null) set(x, y, z, s);
            }
        }
    }

    void ring(int cx, int y, int cz, double r0, double r1, Pick p) {
        int n = (int) Math.ceil(r1) + 1;
        for (int x = cx - n; x <= cx + n; x++) {
            for (int z = cz - n; z <= cz + n; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d <= r0 + 0.35 || d > r1 + 0.35) continue;
                BlockState s = p.at(x, y, z);
                if (s != null) set(x, y, z, s);
            }
        }
    }

    /** Mur cylindrique d'epaisseur {@code thick} dont la face externe est a {@code r}. */
    void wall(int cx, int cz, double r, int y0, int y1, double thick, Pick p) {
        for (int y = y0; y <= y1; y++) ring(cx, y, cz, r - thick, r, p);
    }

    /** Coque de dome (hemisphere superieur) de rayon {@code R} et d'epaisseur {@code thick}, centree en (cx, cy, cz). */
    void dome(int cx, int cy, int cz, double R, double thick, DomePick p) {
        int n = (int) Math.ceil(R) + 1;
        for (int x = cx - n; x <= cx + n; x++) {
            for (int y = cy; y <= cy + n; y++) {
                for (int z = cz - n; z <= cz + n; z++) {
                    double dx = x - cx;
                    double dy = y - cy;
                    double dz = z - cz;
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d > R + 0.3 || d <= R - thick + 0.3) continue;
                    double elev = Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
                    double az = (Math.toDegrees(Math.atan2(dz, dx)) + 360) % 360;
                    BlockState s = p.at(x, y, z, elev, az);
                    if (s != null) set(x, y, z, s);
                }
            }
        }
    }

    void ball(int cx, int cy, int cz, double rx, double ry, double rz, boolean onlyAir, Pick p) {
        for (int x = (int) Math.floor(cx - rx); x <= Math.ceil(cx + rx); x++) {
            for (int y = (int) Math.floor(cy - ry); y <= Math.ceil(cy + ry); y++) {
                for (int z = (int) Math.floor(cz - rz); z <= Math.ceil(cz + rz); z++) {
                    double d = Math.pow((x - cx) / rx, 2) + Math.pow((y - cy) / ry, 2) + Math.pow((z - cz) / rz, 2);
                    if (d > 1.0) continue;
                    if (onlyAir && !air(x, y, z)) continue;
                    BlockState s = p.at(x, y, z);
                    if (s != null) set(x, y, z, s);
                }
            }
        }
    }

    void line(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        int n = Math.max(Math.abs(x1 - x0), Math.max(Math.abs(y1 - y0), Math.abs(z1 - z0)));
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : (double) i / n;
            set((int) Math.round(Mth.lerp(t, x0, x1)), (int) Math.round(Mth.lerp(t, y0, y1)), (int) Math.round(Mth.lerp(t, z0, z1)), s);
        }
    }

    // ------------------------------------------------------------------ masques de l'ile

    boolean in(boolean[][] m, int x, int z) {
        int i = x - X0;
        int j = z - Z0;
        return i >= 0 && j >= 0 && i < m.length && j < m[0].length && m[i][j];
    }
}
