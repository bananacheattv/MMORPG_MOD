package com.mmorpg.hub;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;

/**
 * Bâtiments du Marché d'Aeloria : maisons et halles à colombages (soubassement de pierre appareillée, pilastres, étages de plâtre cerclés
 * de chêne noir, fenêtres de verre azur à contrevents et appuis, corniches, toits de tuiles violettes ou azur à fort pignon) aménagées
 * à l'intérieur (taverne, guilde, atelier, salle des ventes, chambres, escaliers, lustres), et tours rondes à créneaux, échelle intérieure,
 * toit conique, girouette et bannière.
 * Un bâtiment se décrit dans son propre repère (u : largeur, v : profondeur, l'avant regarde la place) ; (fx, fz) est le sens de la façade.
 */
final class HubTown {
    private HubTown() {
    }

    /** Aménagement des pièces d'un bâtiment. */
    enum Style {
        TAVERNE, GUILDE, ATELIER, VENTES, MAISON
    }

    private static int[] at(int cx, int cz, int fx, int fz, int u, int v) {
        return new int[]{cx - u * fz + v * fx, cz + u * fx + v * fz};
    }

    private static void put(HubCtx c, int cx, int cz, int fx, int fz, int u, int y, int v, Object block) {
        int[] p = at(cx, cz, fx, fz, u, v);
        c.set(p[0], y, p[1], c.state(block));
    }

    private static Direction dir(int dx, int dz) {
        return dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Direction du monde d'un pas local (du, dv). */
    private static Direction ld(int fx, int fz, int du, int dv) {
        return dir(-du * fz + dv * fx, du * fx + dv * fz);
    }

    private static BlockState facing(BlockState s, Direction d) {
        if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return s.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
        if (s.hasProperty(StairBlock.FACING)) return s.setValue(StairBlock.FACING, d);
        return s;
    }

    private static BlockState stone(HubCtx c, int x, int y, int z) {
        return c.mix(x, y, z, 301, Blocks.STONE_BRICKS, 40, Blocks.COBBLESTONE, 18, Blocks.ANDESITE, 12, Blocks.MOSSY_STONE_BRICKS, 8,
                Blocks.POLISHED_ANDESITE, 10, Blocks.CRACKED_STONE_BRICKS, 8, Blocks.CHISELED_STONE_BRICKS, 4);
    }

    /** Plâtre vivant : blanc cassé, teintes de chaux et de pierre claire pour casser l'aplat. */
    private static BlockState plaster(HubCtx c, int x, int y, int z) {
        return c.mix(x, y, z, 302, "marbre", 62, Blocks.DIORITE, 14, Blocks.POLISHED_DIORITE, 10, "pierre_solaire", 8, Blocks.CALCITE, 6);
    }

    private static BlockState glass() {
        return Blocks.STAINED_GLASS.pick(DyeColor.LIGHT_BLUE).defaultBlockState();
    }

    private static BlockState stair(Object block, Direction f, boolean top) {
        BlockState s = ((Block) block).defaultBlockState();
        return s.setValue(StairBlock.FACING, f).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState trapdoor(Direction f) {
        return Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, f).setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.HALF, Half.BOTTOM);
    }

    private static BlockState log(HubCtx c, Direction.Axis axis) {
        return c.axis(Blocks.DARK_OAK_LOG, axis);
    }

    // ================================================================== maison ou halle

    /** Maison ou halle. w et d impairs ; floors etages de 4 blocs ; roof : decor des tuiles (tuile_pourpre, tuile_azur...). */
    static void house(HubCtx c, int cx, int cz, int fx, int fz, int w, int d, int floors, String roof, Style style) {
        int hw = w / 2;
        int hd = d / 2;
        int top = floors * 4;
        Direction front = ld(fx, fz, 0, 1);
        for (int u = -hw - 2; u <= hw + 2; u++) {                                              // degagement
            for (int v = -hd - 2; v <= hd + 2; v++) {
                for (int y = 0; y <= top + hd + 7; y++) put(c, cx, cz, fx, fz, u, y, v, Blocks.AIR);
            }
        }
        // fondations appareillees et plancher du rez-de-chaussee
        for (int u = -hw - 1; u <= hw + 1; u++) {
            for (int v = -hd - 1; v <= hd + 1; v++) {
                int[] p = at(cx, cz, fx, fz, u, v);
                boolean inside = Math.abs(u) < hw && Math.abs(v) < hd;
                c.set(p[0], -1, p[1], inside ? c.state(style == Style.TAVERNE || style == Style.VENTES ? Blocks.SPRUCE_PLANKS : Blocks.DARK_OAK_PLANKS) : stone(c, p[0], -1, p[1]));
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
                        if (f > 0 && !stairHole(u, v, hw, hd, f)) c.set(p[0], y0, p[1], c.mix(p[0], y0, p[1], 303, Blocks.DARK_OAK_PLANKS, 70, Blocks.SPRUCE_PLANKS, 30));
                        continue;
                    }
                    boolean corner = wallU && wallV;
                    int along = wallV ? u : v;
                    int phase = Math.floorMod(along + (wallV ? hw : hd), 4);
                    boolean post = corner || phase == 0;
                    for (int dy = 0; dy < 4; dy++) {
                        int y = y0 + dy;
                        BlockState s;
                        if (post) s = log(c, Direction.Axis.Y);
                        else if (dy == 0) s = c.state(Blocks.DARK_OAK_PLANKS);
                        else if (dy == 3) s = c.state(f == floors - 1 ? Blocks.DARK_OAK_PLANKS : Blocks.STRIPPED_DARK_OAK_LOG);
                        else if (f == 0) s = stone(c, p[0], y, p[1]);
                        else s = plaster(c, p[0], y, p[1]);
                        boolean door = f == 0 && wallV && v == hd && Math.abs(u) <= 1 && dy <= 2;
                        if (!post && (dy == 1 || dy == 2) && phase == 2 && !door) s = glass();
                        if (door) s = Blocks.AIR.defaultBlockState();
                        c.set(p[0], y, p[1], s);
                    }
                    // ornements exterieurs des fenetres : appui, contrevents ; pilastres sous les poteaux du rez-de-chaussee
                    Direction out = wallV ? ld(fx, fz, 0, v > 0 ? 1 : -1) : ld(fx, fz, u > 0 ? 1 : -1, 0);
                    int[] q = {p[0] + out.getStepX(), p[1] + out.getStepZ()};
                    if (!post && phase == 2 && !(f == 0 && wallV && v == hd && Math.abs(u) <= 1)) {
                        c.set(q[0], y0 + 1, q[1], stair(Blocks.DARK_OAK_STAIRS, out, true));            // appui de fenetre
                        Direction side = wallV ? ld(fx, fz, 1, 0) : ld(fx, fz, 0, 1);
                        for (int sgn : new int[]{-1, 1}) {
                            int sx = p[0] + side.getStepX() * sgn + out.getStepX();
                            int sz = p[1] + side.getStepZ() * sgn + out.getStepZ();
                            c.set(sx, y0 + 1, sz, trapdoor(out));
                            c.set(sx, y0 + 2, sz, trapdoor(out));
                        }
                        if (f > 0) c.set(q[0], y0 + 2, q[1], Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState());
                    }
                    if (post && !corner && f == 0) {                                                // pilastre contre le mur
                        for (int dy = 0; dy < 4; dy++) c.set(q[0], dy, q[1], dy == 3 ? stair(Blocks.DARK_OAK_STAIRS, out, true) : log(c, Direction.Axis.Y));
                    }
                }
            }
            // encorbellement : corbeaux et poutres sous l'etage suivant, sur la facade
            if (f < floors - 1) {
                for (int u = -hw - 1; u <= hw + 1; u++) put(c, cx, cz, fx, fz, u, y0 + 4, hd + 1, Blocks.DARK_OAK_SLAB.defaultBlockState());
                for (int u = -hw; u <= hw; u += 4) put(c, cx, cz, fx, fz, u, y0 + 3, hd + 1, stair(Blocks.DARK_OAK_STAIRS, front, true));
            }
        }
        // corniche sous le toit
        for (int u = -hw - 1; u <= hw + 1; u++) {
            put(c, cx, cz, fx, fz, u, top - 1, hd + 1, stair(Blocks.STONE_BRICK_STAIRS, front, true));
            put(c, cx, cz, fx, fz, u, top - 1, -hd - 1, stair(Blocks.STONE_BRICK_STAIRS, front.getOpposite(), true));
        }
        // porte : linteau, lanternes et enseigne
        for (int u = -2; u <= 2; u++) put(c, cx, cz, fx, fz, u, 3, hd, Blocks.DARK_OAK_PLANKS);
        put(c, cx, cz, fx, fz, -2, 2, hd + 1, c.hanging(Blocks.LANTERN));
        put(c, cx, cz, fx, fz, 2, 2, hd + 1, c.hanging(Blocks.LANTERN));
        put(c, cx, cz, fx, fz, -2, 3, hd + 1, Blocks.DARK_OAK_FENCE);
        put(c, cx, cz, fx, fz, 2, 3, hd + 1, Blocks.DARK_OAK_FENCE);
        // plafond de l'etage sous les combles
        for (int u = -hw + 1; u < hw; u++) for (int v = -hd + 1; v < hd; v++) put(c, cx, cz, fx, fz, u, top, v, Blocks.DARK_OAK_PLANKS);
        roof(c, cx, cz, fx, fz, hw, hd, top, roof, front);
        // amenagement des pieces
        for (int f = 0; f < floors; f++) furnish(c, cx, cz, fx, fz, hw, hd, f, floors, style);
        staircase(c, cx, cz, fx, fz, hw, hd, floors);
    }

    private static boolean stairHole(int u, int v, int hw, int hd, int f) {
        return v >= -hd + 1 && v <= -hd + 2 && u >= -hw + 1 && u <= -hw + 5;
    }

    /** Escalier droit le long du mur du fond : quatre marches par etage, trappe percee dans le plancher du dessus. */
    private static void staircase(HubCtx c, int cx, int cz, int fx, int fz, int hw, int hd, int floors) {
        Direction up = ld(fx, fz, 1, 0);                                  // l'escalier monte vers +u
        for (int f = 0; f < floors - 1; f++) {
            for (int k = 0; k < 4; k++) {
                put(c, cx, cz, fx, fz, -hw + 1 + k, f * 4 + 1 + k - 0, -hd + 1, stair(Blocks.DARK_OAK_STAIRS, up, false));
                for (int y = f * 4; y < f * 4 + 1 + k; y++) put(c, cx, cz, fx, fz, -hw + 1 + k, y, -hd + 1, Blocks.DARK_OAK_PLANKS);
            }
        }
    }

    /** Toit a deux pans (faitage le long de u) a debord d'un bloc, pignons de platre, lucarnes, epis et cheminee. */
    private static void roof(HubCtx c, int cx, int cz, int fx, int fz, int hw, int hd, int top, String roof, Direction front) {
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
        for (int k = 0; k <= hd; k++) {                                                       // pignons
            int y = top + k;
            for (int vv = -(hd - k); vv <= hd - k; vv++) {
                for (int su : new int[]{-hw, hw}) {
                    boolean edge = Math.abs(vv) == hd - k;
                    BlockState s = edge ? log(c, Direction.Axis.Y) : (k == 1 && Math.abs(vv) <= 1 ? glass() : plaster(c, vv, y, su));
                    put(c, cx, cz, fx, fz, su, y, vv, s);
                }
            }
        }
        for (int u = -hw + 2; u <= hw - 2; u += 6) {                                          // lucarnes sur le pan avant
            int base = top + 1;
            put(c, cx, cz, fx, fz, u, base, hd - 1, plaster(c, u, base, hd));
            put(c, cx, cz, fx, fz, u, base + 1, hd - 1, glass());
            put(c, cx, cz, fx, fz, u - 1, base, hd - 1, stair(Blocks.DARK_OAK_STAIRS, front, false));
            put(c, cx, cz, fx, fz, u + 1, base, hd - 1, stair(Blocks.DARK_OAK_STAIRS, front, false));
            put(c, cx, cz, fx, fz, u, base + 2, hd - 1, c.slab(roof, false));
        }
        put(c, cx, cz, fx, fz, -hw - 1, top + hd + 2, 0, Blocks.END_ROD);
        put(c, cx, cz, fx, fz, hw + 1, top + hd + 2, 0, Blocks.END_ROD);
        for (int y = top; y <= top + hd + 3; y++) {                                           // cheminee en briques
            put(c, cx, cz, fx, fz, hw - 2, y, -hd + 2, Blocks.BRICKS);
            put(c, cx, cz, fx, fz, hw - 3, y, -hd + 2, Blocks.BRICKS);
        }
        put(c, cx, cz, fx, fz, hw - 2, top + hd + 4, -hd + 2, Blocks.BRICK_SLAB);
        put(c, cx, cz, fx, fz, hw - 3, top + hd + 4, -hd + 2, Blocks.BRICK_SLAB);
    }

    // ================================================================== amenagement interieur

    private static void table(HubCtx c, int cx, int cz, int fx, int fz, int u, int y, int v) {
        put(c, cx, cz, fx, fz, u, y, v, Blocks.SPRUCE_FENCE);
        put(c, cx, cz, fx, fz, u, y + 1, v, Blocks.SPRUCE_PRESSURE_PLATE);
        for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            Direction face = ld(fx, fz, -o[0], -o[1]);
            put(c, cx, cz, fx, fz, u + o[0] * 2, y, v + o[1] * 2, stair(Blocks.SPRUCE_STAIRS, face, false));
        }
    }

    private static void furnish(HubCtx c, int cx, int cz, int fx, int fz, int hw, int hd, int f, int floors, Style style) {
        int y = f * 4 + 1;                                                    // sol de la piece (au-dessus du plancher)
        int fy = f == 0 ? 0 : y;                                              // hauteur de pose des meubles
        if (f == 0) fy = 0;
        int base = f == 0 ? 0 : f * 4 + 1;
        Direction toBack = ld(fx, fz, 0, -1);
        Direction toFront = ld(fx, fz, 0, 1);
        // lustres et lanternes : une par quart de piece, suspendues au plafond
        for (int u = -hw + 3; u <= hw - 3; u += 4) {
            for (int v = -hd + 3; v <= hd - 3; v += 5) {
                int ceil = f * 4 + 3;
                put(c, cx, cz, fx, fz, u, ceil, v, Blocks.IRON_CHAIN);
                put(c, cx, cz, fx, fz, u, ceil - 1, v, c.hanging(Blocks.LANTERN));
            }
        }
        if (f == 0) {
            switch (style) {
                case TAVERNE -> {
                    for (int u = -hw + 1; u <= hw - 1; u++) {                                        // comptoir et etagere au fond
                        put(c, cx, cz, fx, fz, u, 0, -hd + 3, Blocks.DARK_OAK_PLANKS);
                        put(c, cx, cz, fx, fz, u, 1, -hd + 3, Blocks.DARK_OAK_SLAB.defaultBlockState());
                        put(c, cx, cz, fx, fz, u, 0, -hd + 1, u % 2 == 0 ? Blocks.BARREL.defaultBlockState() : Blocks.BOOKSHELF.defaultBlockState());
                        put(c, cx, cz, fx, fz, u, 1, -hd + 1, u % 2 == 0 ? Blocks.FLOWER_POT.defaultBlockState() : Blocks.BOOKSHELF.defaultBlockState());
                        put(c, cx, cz, fx, fz, u, 2, -hd + 1, Blocks.BOOKSHELF.defaultBlockState());
                    }
                    for (int u = -hw + 3; u <= hw - 3; u += 5) for (int v = -1; v <= 5; v += 5) if (v < hd - 2) table(c, cx, cz, fx, fz, u, 0, v);
                    for (int v = -1; v <= 1; v++) put(c, cx, cz, fx, fz, hw - 1, 0, v, Blocks.BRICKS);          // cheminee
                    put(c, cx, cz, fx, fz, hw - 2, 0, 0, Blocks.CAMPFIRE.defaultBlockState());
                    for (int v = -hd + 1; v <= hd - 1; v++) put(c, cx, cz, fx, fz, 0, 0, v, Blocks.CARPET.pick(DyeColor.RED).defaultBlockState());
                }
                case GUILDE -> {
                    for (int v = -hd + 1; v <= hd - 1; v++) {
                        put(c, cx, cz, fx, fz, -hw + 1, 0, v, Blocks.BOOKSHELF.defaultBlockState());
                        put(c, cx, cz, fx, fz, -hw + 1, 1, v, Blocks.BOOKSHELF.defaultBlockState());
                        put(c, cx, cz, fx, fz, -hw + 1, 2, v, Blocks.BOOKSHELF.defaultBlockState());
                        put(c, cx, cz, fx, fz, hw - 1, 0, v, Blocks.BOOKSHELF.defaultBlockState());
                        put(c, cx, cz, fx, fz, hw - 1, 1, v, Blocks.BOOKSHELF.defaultBlockState());
                        put(c, cx, cz, fx, fz, hw - 1, 2, v, Blocks.BOOKSHELF.defaultBlockState());
                    }
                    for (int v = -hd + 2; v <= hd - 2; v++) {
                        put(c, cx, cz, fx, fz, 0, 0, v, Blocks.DARK_OAK_PLANKS);
                        put(c, cx, cz, fx, fz, 0, 1, v, Blocks.DARK_OAK_PRESSURE_PLATE);
                        if (v % 2 == 0) {
                            put(c, cx, cz, fx, fz, -1, 0, v, stair(Blocks.DARK_OAK_STAIRS, ld(fx, fz, 1, 0), false));
                            put(c, cx, cz, fx, fz, 1, 0, v, stair(Blocks.DARK_OAK_STAIRS, ld(fx, fz, -1, 0), false));
                        }
                    }
                    for (int u = -3; u <= 3; u++) put(c, cx, cz, fx, fz, u, 0, -hd + 1, Blocks.CARPET.pick(DyeColor.PURPLE).defaultBlockState());
                    for (int u = -2; u <= 2; u += 4) for (int yy = 1; yy <= 3; yy++) put(c, cx, cz, fx, fz, u, yy, -hd, c.state("tuile_pourpre"));
                    put(c, cx, cz, fx, fz, 0, 1, -hd + 2, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, toFront));
                }
                case ATELIER -> {
                    for (int u = -hw + 1; u <= hw - 1; u += 3) {
                        put(c, cx, cz, fx, fz, u, 0, -hd + 1, u % 2 == 0 ? facing(Blocks.FURNACE.defaultBlockState(), toFront) : facing(Blocks.BLAST_FURNACE.defaultBlockState(), toFront));
                        put(c, cx, cz, fx, fz, u + 1, 0, -hd + 1, Blocks.ANVIL.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, ld(fx, fz, 1, 0)));
                    }
                    for (int u = -hw + 2; u <= hw - 2; u += 4) put(c, cx, cz, fx, fz, u, 0, 1, Blocks.SMITHING_TABLE);
                    put(c, cx, cz, fx, fz, hw - 1, 0, 3, Blocks.GRINDSTONE.defaultBlockState());
                    put(c, cx, cz, fx, fz, -hw + 1, 0, 3, Blocks.CAULDRON.defaultBlockState());
                    for (int v = -hd + 3; v <= hd - 2; v += 2) put(c, cx, cz, fx, fz, hw - 1, 0, v, Blocks.BARREL.defaultBlockState());
                }
                case VENTES -> {
                    for (int v = -hd + 5; v <= hd - 2; v += 2) {                                      // gradins de la salle des ventes
                        for (int u = -hw + 2; u <= hw - 2; u++) {
                            if (Math.abs(u) <= 1) continue;
                            put(c, cx, cz, fx, fz, u, 0, v, stair(Blocks.SPRUCE_STAIRS, toBack, false));
                        }
                    }
                    for (int u = -3; u <= 3; u++) {                                                   // estrade du commissaire-priseur
                        put(c, cx, cz, fx, fz, u, 0, -hd + 1, Blocks.SPRUCE_PLANKS);
                        put(c, cx, cz, fx, fz, u, 0, -hd + 2, Blocks.SPRUCE_PLANKS);
                    }
                    put(c, cx, cz, fx, fz, 0, 1, -hd + 2, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, toFront));
                    for (int v = -hd + 3; v <= hd - 1; v++) put(c, cx, cz, fx, fz, 0, 0, v, Blocks.CARPET.pick(DyeColor.RED).defaultBlockState());
                    for (int u : new int[]{-hw + 1, hw - 1}) for (int v = -hd + 1; v <= hd - 1; v += 3) put(c, cx, cz, fx, fz, u, 0, v, Blocks.BARREL.defaultBlockState());
                }
                default -> {
                    for (int v = -hd + 1; v <= hd - 1; v += 3) {
                        put(c, cx, cz, fx, fz, hw - 1, 0, v, Blocks.BOOKSHELF.defaultBlockState());
                        put(c, cx, cz, fx, fz, hw - 1, 1, v, Blocks.BOOKSHELF.defaultBlockState());
                    }
                    table(c, cx, cz, fx, fz, 0, 0, 0);
                    for (int u = -hw + 1; u <= hw - 1; u++) put(c, cx, cz, fx, fz, u, 0, hd - 1, Blocks.BARREL.defaultBlockState());
                    put(c, cx, cz, fx, fz, -hw + 1, 0, -hd + 1, Blocks.CRAFTING_TABLE);
                    put(c, cx, cz, fx, fz, -hw + 2, 0, -hd + 1, Blocks.FURNACE.defaultBlockState());
                }
            }
            return;
        }
        // etages : chambres (lits, coffres, tapis) ou reserves
        boolean bedroom = style != Style.ATELIER || f % 2 == 1;
        for (int u = -hw + 2; u <= hw - 2; u += 3) {
            if (stairHole(u, -hd + 1, hw, hd, f + 1) && f < floors - 1) continue;
            if (bedroom) {
                bed(c, cx, cz, fx, fz, u, base, hd - 2, toBack);
                put(c, cx, cz, fx, fz, u + 1, base, hd - 1, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, toBack));
            } else {
                put(c, cx, cz, fx, fz, u, base, hd - 1, Blocks.BARREL.defaultBlockState());
                put(c, cx, cz, fx, fz, u + 1, base, hd - 1, Blocks.BARREL.defaultBlockState());
            }
        }
        for (int v = -hd + 3; v <= hd - 4; v += 2) put(c, cx, cz, fx, fz, hw - 1, base, v, Blocks.BOOKSHELF.defaultBlockState());
        put(c, cx, cz, fx, fz, 0, base, 0, Blocks.CARPET.pick(DyeColor.PURPLE).defaultBlockState());
        put(c, cx, cz, fx, fz, 1, base, 0, Blocks.CARPET.pick(DyeColor.PURPLE).defaultBlockState());
        put(c, cx, cz, fx, fz, -1, base, 0, Blocks.CARPET.pick(DyeColor.PURPLE).defaultBlockState());
        put(c, cx, cz, fx, fz, -hw + 1, base, 0, Blocks.FLOWER_POT);
    }

    private static void bed(HubCtx c, int cx, int cz, int fx, int fz, int u, int y, int v, Direction headDir) {
        BlockState foot = Blocks.BED.pick(DyeColor.RED).defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, headDir)
                .setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT);
        BlockState head = foot.setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD);
        int[] p = at(cx, cz, fx, fz, u, v);
        c.set(p[0], y, p[1], foot);
        c.set(p[0] + headDir.getStepX(), y, p[1] + headDir.getStepZ(), head);
    }

    // ================================================================== tour ronde

    /** Tour ronde : fut appareille, contreforts, meurtrieres, echelle interieure, chemin de ronde a creneaux, toit conique, bannieres. */
    static void tower(HubCtx c, int cx, int cz, int radius, int height, String roof, boolean gold) {
        for (int y = 0; y <= height; y++) {
            final int yy = y;
            c.ring(cx, y, cz, radius - 1.2, radius, (x, yv, z) -> {
                boolean slit = yy % 7 == 4 && (Math.abs(x - cx) <= 0 || Math.abs(z - cz) <= 0);
                if (slit && yy > 3) return glass();
                if (yy % 7 == 0) return c.state(Blocks.POLISHED_ANDESITE);
                if (yy % 7 == 6) return c.state(Blocks.CHISELED_STONE_BRICKS);
                return stone(c, x, yv, z);
            });
        }
        c.disc(cx, -1, cz, radius, (x, y, z) -> c.state(Blocks.DARK_OAK_PLANKS));
        for (int a = 0; a < 360; a += 45) {                                                // contreforts
            int bx = cx + (int) Math.round(Math.cos(Math.toRadians(a)) * (radius + 0.8));
            int bz = cz + (int) Math.round(Math.sin(Math.toRadians(a)) * (radius + 0.8));
            for (int y = 0; y < Math.min(height, 12); y++) c.set(bx, y, bz, y < 10 ? stone(c, bx, y, bz) : c.state(Blocks.STONE_BRICK_SLAB));
        }
        for (int y = 6; y < height; y += 7) {                                              // planchers intermediaires
            final int yy = y;
            c.disc(cx, yy, cz, radius - 1.2, (x, yv, z) -> (x == cx + radius - 2 && z == cz) ? null : c.state(Blocks.DARK_OAK_PLANKS));
            c.set(cx, y + 1, cz, Blocks.DARK_OAK_FENCE);
            c.set(cx, y + 2, cz, c.hanging(Blocks.LANTERN));
        }
        for (int y = 0; y <= height; y++) {                                                // echelle interieure
            c.set(cx + radius - 1, y, cz, Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        }
        c.disc(cx, height + 1, cz, radius + 1.4, (x, y, z) -> c.state(Blocks.DARK_OAK_PLANKS));
        c.set(cx + radius - 1, height + 1, cz, Blocks.AIR);
        c.ring(cx, height + 2, cz, radius + 0.6, radius + 1.5, (x, y, z) -> ((x + z) & 1) == 0 ? c.state(Blocks.STONE_BRICK_WALL) : c.state(Blocks.DARK_OAK_FENCE));
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
        for (int i = 0; i < 4; i++) {
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
