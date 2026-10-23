package com.mmorpg.hub;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bâtiments du Marché d'Aeloria : maisons et halles à colombages (soubassement de pierre, étages de plâtre cerclés de chêne noir,
 * fenêtres de verre azur, toits de tuiles violettes ou azur à fort pignon) et tours rondes à toit conique, girouette et bannière.
 * Un bâtiment se décrit dans son propre repère (u : largeur, v : profondeur, l'avant regarde la place) ; (fx, fz) est le sens de la façade.
 */
final class HubTown {
    private HubTown() {
    }

    private static int[] at(int cx, int cz, int fx, int fz, int u, int v) {
        return new int[]{cx - u * fz + v * fx, cz + u * fx + v * fz};
    }

    private static void put(HubCtx c, int cx, int cz, int fx, int fz, int u, int y, int v, Object block) {
        int[] p = at(cx, cz, fx, fz, u, v);
        c.set(p[0], y, p[1], c.state(block));
    }

    private static Direction dir(int fx, int fz) {
        return fx > 0 ? Direction.EAST : fx < 0 ? Direction.WEST : fz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static BlockState stone(HubCtx c, int x, int y, int z) {
        return c.mix(x, y, z, 301, Blocks.STONE_BRICKS, 42, Blocks.COBBLESTONE, 22, Blocks.ANDESITE, 14, Blocks.MOSSY_STONE_BRICKS, 10, Blocks.POLISHED_ANDESITE, 12);
    }

    private static BlockState glass(HubCtx c) {
        return Blocks.STAINED_GLASS.pick(DyeColor.LIGHT_BLUE).defaultBlockState();
    }

    /**
     * Maison ou halle. w et d impairs ; floors etages de 4 blocs ; roof : decor des tuiles (tuile_pourpre, tuile_azur...).
     */
    static void house(HubCtx c, int cx, int cz, int fx, int fz, int w, int d, int floors, String roof) {
        int hw = w / 2;
        int hd = d / 2;
        int top = floors * 4;
        // degagement et sol
        for (int u = -hw - 2; u <= hw + 2; u++) {
            for (int v = -hd - 2; v <= hd + 2; v++) {
                for (int y = 0; y <= top + hd + 6; y++) put(c, cx, cz, fx, fz, u, y, v, Blocks.AIR);
                if (Math.abs(u) <= hw && Math.abs(v) <= hd) {
                    int[] p = at(cx, cz, fx, fz, u, v);
                    c.set(p[0], -1, p[1], stone(c, p[0], -1, p[1]));
                }
            }
        }
        for (int f = 0; f < floors; f++) {
            int y0 = f * 4;
            for (int u = -hw; u <= hw; u++) {
                for (int v = -hd; v <= hd; v++) {
                    boolean wallU = Math.abs(u) == hw;
                    boolean wallV = Math.abs(v) == hd;
                    int[] p = at(cx, cz, fx, fz, u, v);
                    if (!wallU && !wallV) {
                        if (f > 0) c.set(p[0], y0, p[1], Blocks.DARK_OAK_PLANKS);        // plancher de l'etage
                        continue;
                    }
                    boolean corner = wallU && wallV;
                    int along = wallV ? u : v;                                                // position le long de la facade
                    boolean post = corner || Math.floorMod(along + (wallV ? hw : hd), 4) == 0;
                    for (int dy = 0; dy < 4; dy++) {
                        int y = y0 + dy;
                        BlockState s;
                        if (post) s = c.axis(Blocks.DARK_OAK_LOG, Direction.Axis.Y);
                        else if (dy == 0 || dy == 3) s = Blocks.DARK_OAK_PLANKS.defaultBlockState();
                        else if (f == 0) s = stone(c, p[0], y, p[1]);
                        else s = c.state("marbre");
                        // fenetre au milieu de chaque panneau
                        if (!post && (dy == 1 || dy == 2) && Math.floorMod(along + (wallV ? hw : hd), 4) == 2 && !(f == 0 && wallV && v == hd && Math.abs(u) <= 1)) s = glass(c);
                        // porte voutee au centre de la facade
                        if (f == 0 && wallV && v == hd && Math.abs(u) <= 1 && dy <= 2) s = Blocks.AIR.defaultBlockState();
                        c.set(p[0], y0 + dy, p[1], s);
                    }
                }
            }
            // balcon en encorbellement sous l'etage suivant (facade)
            if (f < floors - 1) for (int u = -hw - 1; u <= hw + 1; u++) put(c, cx, cz, fx, fz, u, y0 + 4, hd + 1, Blocks.DARK_OAK_SLAB.defaultBlockState());
        }
        // lanternes et enseigne
        put(c, cx, cz, fx, fz, -2, 3, hd + 1, c.hanging(Blocks.LANTERN));
        put(c, cx, cz, fx, fz, 2, 3, hd + 1, c.hanging(Blocks.LANTERN));
        put(c, cx, cz, fx, fz, -2, 4, hd + 1, Blocks.DARK_OAK_FENCE);
        put(c, cx, cz, fx, fz, 2, 4, hd + 1, Blocks.DARK_OAK_FENCE);
        // toit a deux pans (faitage le long de u), debord d'un bloc
        Direction front = dir(fx, fz);
        for (int k = 0; k <= hd + 1; k++) {
            int y = top + k;
            int v1 = hd + 1 - k;
            for (int u = -hw - 1; u <= hw + 1; u++) {
                if (v1 > 0) {
                    put(c, cx, cz, fx, fz, u, y, v1, c.stair(roof, front, false));
                    put(c, cx, cz, fx, fz, u, y, -v1, c.stair(roof, front.getOpposite(), false));
                } else {
                    put(c, cx, cz, fx, fz, u, y, 0, c.state(roof));
                }
            }
        }
        for (int k = 0; k <= hd; k++) {                                                       // pignons de platre sous le toit
            int y = top + k;
            for (int vv = -(hd - k); vv <= hd - k; vv++) {
                for (int su : new int[]{-hw, hw}) {
                    boolean edge = Math.abs(vv) == hd - k;
                    put(c, cx, cz, fx, fz, su, y, vv, edge ? c.axis(Blocks.DARK_OAK_LOG, Direction.Axis.Y) : c.state("marbre"));
                }
            }
        }
        // epi de faitage et cheminee
        put(c, cx, cz, fx, fz, -hw - 1, top + hd + 2, 0, Blocks.END_ROD);
        put(c, cx, cz, fx, fz, hw + 1, top + hd + 2, 0, Blocks.END_ROD);
        for (int y = top; y <= top + hd + 3; y++) {
            put(c, cx, cz, fx, fz, hw - 2, y, -hd + 2, Blocks.BRICKS);
            put(c, cx, cz, fx, fz, hw - 3, y, -hd + 2, Blocks.BRICKS);
        }
        put(c, cx, cz, fx, fz, hw - 2, top + hd + 4, -hd + 2, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
    }

    /** Tour ronde : fut de pierre a meurtrieres, couronne de chene, toit conique de tuiles, hampe et bannieres. */
    static void tower(HubCtx c, int cx, int cz, int radius, int height, String roof, boolean gold) {
        for (int y = 0; y <= height; y++) {
            final int yy = y;
            c.ring(cx, y, cz, radius - 1.2, radius, (x, yv, z) -> {
                boolean slit = yy % 7 == 4 && (Math.abs(x - cx) <= 0 || Math.abs(z - cz) <= 0);
                if (slit && yy > 3) return glass(c);
                if (yy % 7 == 0) return c.state(Blocks.POLISHED_ANDESITE);
                return stone(c, x, yv, z);
            });
        }
        c.disc(cx, -1, cz, radius, (x, y, z) -> stone(c, x, y, z));
        c.disc(cx, height + 1, cz, radius + 1.4, (x, y, z) -> c.state(Blocks.DARK_OAK_PLANKS));
        c.ring(cx, height + 2, cz, radius + 0.6, radius + 1.5, (x, y, z) -> ((x + z) & 1) == 0 ? c.state(Blocks.DARK_OAK_FENCE) : null);
        // porte au pied de la tour
        for (int dy = 0; dy < 3; dy++) c.set(cx, dy, cz + radius, Blocks.AIR);
        int layers = radius * 2 + 4;
        for (int k = 0; k < layers; k++) {
            double r = radius + 1.4 - k * ((radius + 1.2) / layers);
            if (r < 0.6) break;
            final int kk = k;
            c.disc(cx, height + 2 + k, cz, r, (x, y, z) -> kk % 5 == 4 ? c.state("or_ouvrage") : c.state(roof));
        }
        int tip = height + 2 + layers;
        for (int y = tip; y < tip + 6; y++) c.set(cx, y, cz, y == tip + 5 ? Blocks.END_ROD.defaultBlockState() : Blocks.DARK_OAK_FENCE.defaultBlockState());
        for (int i = 0; i < 4; i++) {                                                      // bannieres
            c.set(cx + 1 + i, tip + 4, cz, Blocks.WOOL.pick(DyeColor.PURPLE).defaultBlockState());
            if (i < 3) c.set(cx + 1 + i, tip + 3, cz, gold ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
        }
        for (int a = 0; a < 360; a += 90) {
            int lx = cx + (int) Math.round(Math.cos(Math.toRadians(a)) * (radius + 1));
            int lz = cz + (int) Math.round(Math.sin(Math.toRadians(a)) * (radius + 1));
            c.set(lx, height, lz, c.hanging(Blocks.LANTERN));
        }
    }
}
