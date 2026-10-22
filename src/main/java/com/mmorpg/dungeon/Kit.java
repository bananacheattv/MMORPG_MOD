package com.mmorpg.dungeon;

import com.mmorpg.hub.HubCtx;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Elements decoratifs reutilisables par tous les donjons : colonnes, arcs, lustres, braziers, statues, nervures de coupole. */
final class Kit {
    private Kit() {
    }

    /** Pose un bloc donne soit comme bloc, soit comme etat, soit comme identifiant de decor. */
    static void put(HubCtx c, int x, int y, int z, Object o) {
        c.set(x, y, z, c.state(o));
    }

    /** Colonne : socle, fut, chapiteau evase (escaliers retournes sur les quatre faces). */
    static void column(HubCtx c, int x, int z, int y0, int h, Object base, Object shaft, Object stair) {
        put(c, x, y0, z, base);
        for (int y = y0 + 1; y < y0 + h; y++) put(c, x, y, z, shaft);
        if (stair instanceof Block sb) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                c.set(x + d.getStepX(), y0 + h - 1, z + d.getStepZ(), c.stair(sb, d, true));
                c.set(x + d.getStepX(), y0, z + d.getStepZ(), c.stair(sb, d, false));
            }
        }
    }

    /** Colonnes reparties regulierement sur un cercle. */
    static void colonnade(HubCtx c, double cx, double cz, double r, int count, double phase, int y0, int h, Object base, Object shaft, Object stair) {
        for (int i = 0; i < count; i++) {
            double a = phase + Math.PI * 2 * i / count;
            column(c, (int) Math.round(cx + Math.cos(a) * r), (int) Math.round(cz + Math.sin(a) * r), y0, h, base, shaft, stair);
        }
    }

    /** Brasero : pied en pierre, vasque et feu. */
    static void brazier(HubCtx c, int x, int y, int z, Object foot, Block fire) {
        put(c, x, y, z, foot);
        put(c, x, y + 1, z, foot);
        c.set(x, y + 2, z, fire);
    }

    /** Lustre : chaine verticale et croix de chaines portant quatre lanternes. */
    static void chandelier(HubCtx c, int x, int topY, int z, int drop, Block lantern) {
        for (int y = topY; y > topY - drop; y--) c.set(x, y, z, Blocks.IRON_CHAIN);
        int by = topY - drop;
        for (int d = -2; d <= 2; d++) {
            c.set(x + d, by, z, c.axis(Blocks.IRON_CHAIN, Direction.Axis.X));
            c.set(x, by, z + d, c.axis(Blocks.IRON_CHAIN, Direction.Axis.Z));
        }
        c.set(x, by, z, Blocks.IRON_CHAIN);
        for (int[] o : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {0, 0}}) c.set(x + o[0], by - 1, z + o[1], c.hanging(lantern));
    }

    /** Statue de gardien : piedestal, jambes, torse, bras et tete (le bloc de tete est fourni par le theme). */
    static void statue(HubCtx c, int x, int y, int z, Direction facing, Object pedestal, Object body, Object limb, BlockState head, Block stair) {
        c.fill(x - 1, y, z - 1, x + 1, y, z + 1, pedestal);
        put(c, x, y + 1, z, body);
        put(c, x, y + 2, z, body);
        put(c, x, y + 3, z, body);
        Direction side = facing.getClockWise();
        put(c, x + side.getStepX(), y + 3, z + side.getStepZ(), limb);
        put(c, x - side.getStepX(), y + 3, z - side.getStepZ(), limb);
        put(c, x + side.getStepX(), y + 2, z + side.getStepZ(), limb);
        put(c, x - side.getStepX(), y + 2, z - side.getStepZ(), limb);
        c.set(x, y + 4, z, head);
        if (stair != null) {
            c.set(x + facing.getStepX(), y + 1, z + facing.getStepZ(), c.stair(stair, facing, false));
        }
    }

    static BlockState skull(Block skull, int rotation) {
        BlockState s = skull.defaultBlockState();
        return s.hasProperty(SkullBlock.ROTATION) ? s.setValue(SkullBlock.ROTATION, rotation & 15) : s;
    }

    static BlockState candles(Block candle, int count) {
        BlockState s = candle.defaultBlockState();
        if (s.hasProperty(CandleBlock.CANDLES)) s = s.setValue(CandleBlock.CANDLES, Math.max(1, Math.min(4, count)));
        if (s.hasProperty(CandleBlock.LIT)) s = s.setValue(CandleBlock.LIT, true);
        return s;
    }

    /** Nervures d'une coupole : arcs de cercle verticaux de la base au sommet, legerement en saillie sur la voute. */
    static void ribs(HubCtx c, double cx, double cz, double rx, double rz, int baseY, double height, int count, Object block) {
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            for (double t = 0; t <= Math.PI / 2 + 0.001; t += 0.03) {
                double rr = Math.cos(t);
                int x = (int) Math.round(cx + Math.cos(a) * (rx - 0.2) * rr);
                int z = (int) Math.round(cz + Math.sin(a) * (rz - 0.2) * rr);
                int y = baseY + (int) Math.round(Math.sin(t) * (height - 0.2));
                put(c, x, y, z, block);
            }
        }
    }

    /** Bande decorative qui fait le tour d'une salle ronde, en saillie contre le mur. */
    static void band(HubCtx c, double cx, double cz, double r, int y, Object block) {
        c.ring((int) cx, y, (int) cz, r - 1.2, r + 0.1, (x, yy, z) -> c.state(block));
    }

    /** Porte en arc : jambages, linteau et escaliers retournes qui arrondissent les angles du passage. */
    static void doorway(HubCtx c, int cx, int z, int halfW, int h, Object frame, Block stair) {
        for (int y = 1; y <= h; y++) {
            put(c, cx - halfW - 1, y, z, frame);
            put(c, cx + halfW + 1, y, z, frame);
        }
        for (int x = cx - halfW - 1; x <= cx + halfW + 1; x++) put(c, x, h + 1, z, frame);
        if (stair != null) {
            c.set(cx - halfW, h, z, c.stair(stair, Direction.EAST, true));
            c.set(cx + halfW, h, z, c.stair(stair, Direction.WEST, true));
        }
    }

    /** Disque a motifs concentriques, utilise pour les sols d'arene et les estrades. */
    static void rings(HubCtx c, int cx, int y, int cz, double rMax, Object a, Object b, Object edge) {
        c.disc(cx, y, cz, rMax, (x, yy, z) -> {
            double d = Math.hypot(x - cx, z - cz);
            if (d > rMax - 1.2) return c.state(edge);
            return c.state(((int) (d / 2.5)) % 2 == 0 ? a : b);
        });
    }
}
