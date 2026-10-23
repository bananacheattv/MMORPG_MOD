package com.mmorpg.hub;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * La Cite d'Aeloria : le grand Spawn. Le Marche (HubMarket) reste le coeur de la ville ; tout autour s'etendent des avenues, deux rues
 * circulaires bordees de maisons a colombages, un chateau au nord, un port et son phare au sud, un moulin et des champs a l'ouest, de
 * grandes halles a l'est, et un rempart a creneaux, ses tours et ses portes. Les coordonnees sont relatives a l'origine du spawn.
 */
final class SpawnCity {
    static final int CX = HubMarket.CX;
    /** Rayon de la cite, rempart compris (l'ile deborde de quelques blocs). */
    static final int R = 124;

    static boolean footprint(int x, int z) {
        return Math.hypot(x - CX, z) <= R + 1.4;
    }

    private static final int W = HubCtx.X1 - HubCtx.X0 + 1, D = HubCtx.Z1 - HubCtx.Z0 + 1;
    private static final Block[] ROOFS = {null};
    private static final String[] TILES = {"tuile_pourpre", "tuile_azur"};

    private final HubCtx c;
    private final boolean[][] occ = new boolean[W][D];

    private SpawnCity(HubCtx c) {
        this.c = c;
    }

    // ------------------------------------------------------------------ occupation

    private boolean busy(int x, int z) {
        int i = x - HubCtx.X0, j = z - HubCtx.Z0;
        return i < 0 || j < 0 || i >= W || j >= D || occ[i][j];
    }

    private void mark(int x0, int z0, int x1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                int i = x - HubCtx.X0, j = z - HubCtx.Z0;
                if (i >= 0 && j >= 0 && i < W && j < D) occ[i][j] = true;
            }
        }
    }

    private void markDisc(int cx, int cz, double r) {
        for (int x = (int) (cx - r - 1); x <= cx + r + 1; x++) for (int z = (int) (cz - r - 1); z <= cz + r + 1; z++) if (Math.hypot(x - cx, z - cz) <= r) mark(x, z, x, z);
    }

    private boolean free(int x0, int z0, int x1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) if (busy(x, z)) return false;
        return true;
    }

    private static double polar(int x, int z) {
        return Math.hypot(x - CX, z);
    }

    // ------------------------------------------------------------------ sol de la cite

    /** Pelouse de toute l'ile de la cite (le terrain du hub y a laisse du marbre) ; l'ile reste plate pour les batiments. */
    static void ground(HubCtx c) {
        for (int x = CX - R - 6; x <= CX + R + 6; x++) {
            for (int z = -R - 6; z <= R + 6; z++) {
                if (!c.in(c.isle, x, z)) continue;
                c.set(x, -1, z, c.mix(x, -1, z, 80, Blocks.GRASS_BLOCK, 84, Blocks.COARSE_DIRT, 5, Blocks.PODZOL, 4, Blocks.MOSS_BLOCK, 7));
                c.set(x, -2, z, Blocks.DIRT);
            }
        }
    }

    static void build(HubCtx c) {
        new SpawnCity(c).run();
    }

    private void run() {
        markDisc(CX, 0, 54);                                                              // le Marche existant
        mark(CX - R, -8, CX + R, 8);                                                       // avenues est-ouest et nord-sud
        mark(CX - 8, -R, CX + 8, R);
        markDisc(CX, 0, R + 8 + 0);                                                        // (le rempart est reserve plus bas)
        for (int x = HubCtx.X0; x <= HubCtx.X1; x++) for (int z = HubCtx.Z0; z <= HubCtx.Z1; z++) if (polar(x, z) <= R - 12 && polar(x, z) > 54) occ[x - HubCtx.X0][z - HubCtx.Z0] = false;
        mark(CX - 8, -R, CX + 8, R);
        mark(CX - R, -8, CX + R, 8);
        // zones reservees : chateau (nord), port (sud), moulin et champs (ouest), halles (est)
        mark(CX - 36, -118, CX + 36, -62);
        markDisc(CX, 96, 31);
        mark(CX + 28, 86, CX + 44, 106);
        mark(CX - 116, -50, CX - 56, 50);
        mark(CX + 56, -50, CX + 122, 50);
        roads();
        wall();
        castle();
        harbor();
        farm();
        east();
        houses();
        scenery();
    }

    // ------------------------------------------------------------------ routes

    private BlockState pave(int x, int z) {
        return c.mix(x, -1, z, 81, Blocks.COBBLESTONE, 36, Blocks.STONE_BRICKS, 22, Blocks.ANDESITE, 18, Blocks.GRAVEL, 8, Blocks.MOSSY_COBBLESTONE, 8, Blocks.POLISHED_ANDESITE, 8);
    }

    private void paveCell(int x, int z) {
        c.set(x, -1, z, pave(x, z));
        for (int y = 0; y <= 2; y++) if (!c.air(x, y, z)) c.set(x, y, z, Blocks.AIR);
    }

    private void roads() {
        for (int x = CX - R + 6; x <= CX + R - 6; x++) {                                    // avenue est-ouest
            for (int z = -5; z <= 5; z++) {
                if (polar(x, z) <= 52 || polar(x, z) > R - 4 || (x > CX - 40 && x < CX + 40 && false)) continue;
                paveCell(x, z);
                if (Math.abs(z) <= 1 && (x + CX) % 6 == 0) c.set(x, -1, z, "dalle_runique");
            }
        }
        for (int z = -R + 6; z <= R - 6; z++) {                                             // avenue nord-sud (jusqu'au chateau et au port)
            for (int x = CX - 5; x <= CX + 5; x++) {
                if (polar(x, z) <= 52 || (z < -66) || (z > 66)) continue;
                paveCell(x, z);
                if (Math.abs(x - CX) <= 1 && z % 6 == 0) c.set(x, -1, z, "dalle_runique");
            }
        }
        for (int ring : new int[]{66, 94}) {                                                // rues circulaires
            for (int x = CX - ring - 4; x <= CX + ring + 4; x++) {
                for (int z = -ring - 4; z <= ring + 4; z++) {
                    double r = polar(x, z);
                    if (Math.abs(r - ring) > 2.6) continue;
                    if (busyReserved(x, z)) continue;
                    paveCell(x, z);
                    mark(x, z, x, z);
                }
            }
        }
        for (int a = 0; a < 360; a += 4) {                                                  // lampadaires aux bords des rues
            for (int ring : new int[]{66, 94}) {
                double rad = Math.toRadians(a);
                int x = CX + (int) Math.round(Math.cos(rad) * (ring + 3.6)), z = (int) Math.round(Math.sin(rad) * (ring + 3.6));
                if (a % 12 == 0 && !busyReserved(x, z) && c.air(x, 0, z) && !roadCell(x, z)) HubBuilder.lamp(c, x, z);
            }
        }
        for (int x = CX - R + 10; x <= CX + R - 10; x += 12) {                              // lampadaires le long des avenues
            for (int s : new int[]{-6, 6}) {
                if (polar(x, s) > 54 && polar(x, s) < R - 6) HubBuilder.lamp(c, x, s);
            }
        }
        for (int z = -62; z <= 62; z += 12) {
            for (int s : new int[]{-6, 6}) {
                if (polar(CX + s, z) > 54 && !(z > 66)) HubBuilder.lamp(c, CX + s, z);
            }
        }
    }

    private boolean roadCell(int x, int z) {
        return Math.abs(z) <= 5 || Math.abs(x - CX) <= 5;
    }

    /** Zones reservees aux grands ensembles : on n'y trace pas les rues circulaires. */
    private boolean busyReserved(int x, int z) {
        return (x >= CX - 36 && x <= CX + 36 && z >= -118 && z <= -62) || Math.hypot(x - CX, z - 96) <= 31 || (x >= CX + 28 && x <= CX + 44 && z >= 86 && z <= 106)
                || (x >= CX - 116 && x <= CX - 56 && z >= -50 && z <= 50) || (x >= CX + 56 && x <= CX + 122 && z >= -50 && z <= 50);
    }

    // ------------------------------------------------------------------ rempart

    private BlockState stone(int x, int y, int z) {
        return c.mix(x, y, z, 82, Blocks.STONE_BRICKS, 44, Blocks.COBBLESTONE, 16, Blocks.ANDESITE, 12, Blocks.MOSSY_STONE_BRICKS, 10, Blocks.POLISHED_ANDESITE, 8, Blocks.CRACKED_STONE_BRICKS, 10);
    }

    private void wall() {
        for (int x = CX - R - 2; x <= CX + R + 2; x++) {
            for (int z = -R - 2; z <= R + 2; z++) {
                double r = polar(x, z);
                if (r <= R - 3 || r > R + 0.4) continue;
                boolean gate = Math.abs(z) <= 5 && Math.abs(x - CX) > R - 8;
                boolean water = Math.abs(x - CX) <= 6 && z > 100;
                for (int y = water ? -3 : -2; y <= 10; y++) {
                    if (gate && y >= 0 && y <= 7) continue;
                    if (water && y >= -1 && y <= 7) continue;
                    if (y == 10 && !(r > R - 1.3 && ((x + z) & 1) == 0)) continue;
                    c.set(x, y, z, y == 9 ? c.state(Blocks.POLISHED_ANDESITE) : stone(x, y, z));
                }
                if (gate) {
                    for (int y = 8; y <= 9; y++) c.set(x, y, z, stone(x, y, z));
                    c.set(x, 7, z, c.state(Blocks.STONE_BRICK_SLAB));
                }
                if (water) {
                    for (int y = -1; y <= -1; y++) c.set(x, y, z, Blocks.WATER);
                    if (r > R - 2) for (int y = 0; y <= 6; y++) if (Math.abs(x - CX) % 2 == 0) c.set(x, y, z, Blocks.IRON_BARS);
                }
            }
        }
        // tours du rempart et des portes
        for (int k = 0; k < 12; k++) {
            if (k == 0 || k == 3 || k == 6 || k == 9) continue;                          // portes, chateau et port
            double a = Math.toRadians(k * 30 + 15);
            int x = CX + (int) Math.round(Math.cos(a) * (R - 2)), z = (int) Math.round(Math.sin(a) * (R - 2));
            HubTown.tower(c, x, z, 5, 24, TILES[k & 1], true);
        }
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) HubTown.tower(c, CX + sx * (R - 2), sz * 10, 4, 24, "tuile_pourpre", true);
        }
        for (int sx : new int[]{-1, 1}) {
            HubTown.tower(c, CX + sx * 10, R - 3, 4, 22, "tuile_azur", true);                // flancs de l'arche du port
            gateDecor(sx);
        }
    }

    private void gateDecor(int side) {
        int gx = CX + side * (R - 2);
        for (int z : new int[]{-5, 5}) {
            for (int y = 0; y <= 9; y++) c.set(gx, y, z, y % 4 == 3 ? c.state("or_ouvrage") : c.axis("colonne_marbre", Direction.Axis.Y));
        }
        for (int z = -4; z <= 4; z++) {
            for (int y = 1; y <= 7; y++) c.set(gx + side * 2, y, z, y == 7 ? c.state("or_ouvrage") : c.state(Blocks.IRON_BARS));
        }
        for (int y = 0; y <= 3; y++) for (int s : new int[]{-3, 3}) c.set(gx - side * 3, y, s, Blocks.SPRUCE_FENCE);
        HubBuilder.text(c, gx - side * 4, 12.5, 0, side > 0 ? "PORTE DE L'EST" : "PORTE DE L'OUEST", "#FFD060", 4F);
    }

    // ------------------------------------------------------------------ chateau

    private void castle() {
        int x0 = CX - 34, x1 = CX + 34, z0 = -116, z1 = -66;
        for (int x = x0; x <= x1; x++) {                                                    // cour pavee
            for (int z = z0; z <= z1; z++) {
                c.set(x, -1, z, (x + z) % 5 == 0 ? c.state("dalle_runique") : pave(x, z));
                for (int y = 0; y <= 3; y++) if (!c.air(x, y, z)) c.set(x, y, z, Blocks.AIR);
            }
        }
        for (int x = x0; x <= x1; x++) {                                                    // courtines : trois blocs d'epaisseur
            for (int z = z0; z <= z1; z++) {
                boolean edge = x - x0 < 3 || x1 - x < 3 || z - z0 < 3 || z1 - z < 3;
                if (!edge) continue;
                boolean gate = z1 - z < 3 && Math.abs(x - CX) <= 4;
                for (int y = 0; y <= 10; y++) {
                    if (gate && y <= 7) continue;
                    if (y == 10 && ((x + z) & 1) != 0) continue;
                    c.set(x, y, z, y == 9 ? c.state(Blocks.POLISHED_ANDESITE) : stone(x, y, z));
                }
                if (gate) {
                    for (int y = 8; y <= 9; y++) c.set(x, y, z, stone(x, y, z));
                    if (Math.abs(x - CX) == 4) for (int y = 0; y <= 7; y++) c.set(x, y, z, c.axis("colonne_marbre", Direction.Axis.Y));
                }
            }
        }
        for (int sx : new int[]{-1, 1}) {
            HubTown.tower(c, CX + sx * 34, z0, 6, 32, "tuile_pourpre", true);
            HubTown.tower(c, CX + sx * 34, z1, 6, 32, "tuile_azur", true);
            HubTown.tower(c, CX + sx * 10, z1, 5, 26, "tuile_pourpre", true);               // tours de la porte
            c.fill(CX + sx * 22, -1, z0 + 3, CX + sx * 22, -1, z1 - 3, Blocks.AIR);
        }
        HubTown.tower(c, CX, z0 + 3, 7, 46, "tuile_pourpre", true);                          // grand donjon au fond
        HubTown.house(c, CX, -97, 0, 1, 27, 15, 5, "tuile_pourpre", HubTown.Style.GUILDE);   // le logis
        HubTown.house(c, CX - 24, -80, 1, 0, 11, 9, 3, "tuile_azur", HubTown.Style.MAISON);
        HubTown.house(c, CX + 24, -80, -1, 0, 11, 9, 3, "tuile_azur", HubTown.Style.VENTES);
        for (int z = -88; z <= z1 - 3; z++) {                                               // allee centrale et massifs fleuris
            for (int x = CX - 3; x <= CX + 3; x++) c.set(x, -1, z, (x + z) % 4 == 0 ? c.state("marbre_dore") : c.state("marbre_poli"));
        }
        for (int s : new int[]{-1, 1}) {
            for (int z = -86; z <= -70; z += 4) HubBuilder.lamp(c, CX + s * 5, z);
            for (int x = CX + s * 8; Math.abs(x - CX) <= 18; x += s) {
                for (int z = -76; z <= -70; z++) {
                    c.set(x, -1, z, Blocks.GRASS_BLOCK);
                    if ((x + z) % 3 != 0) c.set(x, 0, z, HubBuilder.FLOWERS[Math.floorMod(x * 7 + z * 3, HubBuilder.FLOWERS.length)]);
                }
            }
        }
        HubBuilder.text(c, CX, 24, -66, "CHÂTEAU D'AELORIA", "#E0B0FF", 6.5F);
        HubBuilder.text(c, CX, 20.2, -66, "Siège du royaume · salle du trône", "#F0E8D8", 2.6F);
    }

    // ------------------------------------------------------------------ port et phare

    private void harbor() {
        int lx = CX, lz = 96;
        double r0 = 25;
        for (int x = lx - 30; x <= lx + 30; x++) {
            for (int z = lz - 30; z <= lz + 30; z++) {
                double d = Math.hypot(x - lx, z - lz);
                if (d > 29.5 || polar(x, z) > R - 3) continue;
                if (d <= r0) {
                    for (int y = -1; y >= -3; y--) c.set(x, y, z, Blocks.WATER);
                    c.set(x, -4, z, c.mix(x, -4, z, 83, Blocks.GRAVEL, 60, Blocks.SAND, 30, Blocks.CLAY, 10));
                    for (int y = 0; y <= 3; y++) if (!c.air(x, y, z)) c.set(x, y, z, Blocks.AIR);
                } else {                                                                   // plage et quais
                    c.set(x, -1, z, d < r0 + 2.2 ? c.mix(x, -1, z, 84, Blocks.SAND, 70, Blocks.GRAVEL, 30) : c.mix(x, -1, z, 85, Blocks.GRASS_BLOCK, 60, Blocks.COARSE_DIRT, 20, Blocks.SAND, 20));
                }
            }
        }
        for (int s : new int[]{-1, 1}) pier(CX + s * 12, 72, 22);
        ship(CX - 22, 98, false);
        ship(CX + 6, 108, true);
        lighthouse(CX + 36, 96);
        HubTown.house(c, CX - 40, 86, 1, 0, 15, 11, 2, "tuile_azur", HubTown.Style.ATELIER);       // entrepots du port
        HubTown.house(c, CX - 40, 106, 1, 0, 13, 11, 2, "tuile_pourpre", HubTown.Style.MAISON);
        HubTown.house(c, CX - 14, 66, 0, 1, 11, 9, 3, "tuile_pourpre", HubTown.Style.TAVERNE);       // la capitainerie
        HubBuilder.text(c, CX, 16, 70, "PORT D'AELORIA", "#8CD8FF", 6F);
        HubBuilder.text(c, CX + 36, 46, 96, "PHARE", "#FFE6A0", 3F);
    }

    private void pier(int x, int z0, int len) {
        for (int z = z0; z < z0 + len; z++) {
            for (int dx = -1; dx <= 1; dx++) {
                c.set(x + dx, -1, z, c.mix(x + dx, -1, z, 86, Blocks.SPRUCE_PLANKS, 70, Blocks.DARK_OAK_PLANKS, 30));
                if (polar(x + dx, z) < R) for (int y = 0; y <= 2; y++) if (!c.air(x + dx, y, z)) c.set(x + dx, y, z, Blocks.AIR);
            }
            if ((z - z0) % 4 == 0) {
                for (int y = -2; y >= -4; y--) for (int dx : new int[]{-1, 1}) c.set(x + dx, y, z, c.axis(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
            }
            for (int dx : new int[]{-2, 2}) {
                if ((z - z0) % 6 == 0) {
                    c.set(x + dx, 0, z, Blocks.DARK_OAK_FENCE);
                    c.set(x + dx, 1, z, Blocks.DARK_OAK_FENCE);
                    c.set(x + dx, 2, z, c.state(Blocks.LANTERN));
                }
            }
        }
        c.set(x, 0, z0 + len - 1, Blocks.BARREL);
    }

    private void ship(int cx, int cz, boolean alongX) {
        for (int u = -6; u <= 6; u++) {
            int hw = Math.abs(u) > 3 ? 2 - (Math.abs(u) - 3) / 2 : 2;
            for (int v = -hw; v <= hw; v++) {
                int x = alongX ? cx + u : cx + v, z = alongX ? cz + v : cz + u;
                c.set(x, -2, z, Blocks.SPRUCE_PLANKS);
                c.set(x, -1, z, Math.abs(v) == hw ? c.state(Blocks.DARK_OAK_PLANKS) : c.state(Blocks.SPRUCE_PLANKS));
                if (Math.abs(v) == hw) c.set(x, 0, z, Blocks.SPRUCE_FENCE);
            }
        }
        for (int y = 0; y <= 10; y++) c.set(cx, y, cz, Blocks.SPRUCE_LOG);
        for (int y = 3; y <= 8; y++) {
            for (int s = -2; s <= 2; s++) {
                int x = alongX ? cx : cx + s, z = alongX ? cz + s : cz;
                c.set(alongX ? x + 1 : x, y, alongX ? z : z + 1, Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
            }
        }
        int sx = alongX ? cx - 6 : cx, sz = alongX ? cz : cz - 6;
        c.set(sx, 0, sz, Blocks.SPRUCE_FENCE);
        c.set(sx, 1, sz, Blocks.SPRUCE_FENCE);
        c.set(sx, 2, sz, c.state(Blocks.LANTERN));
    }

    private void lighthouse(int cx, int cz) {
        c.disc(cx, -1, cz, 7, (x, y, z) -> c.state(Blocks.STONE_BRICKS));
        for (int y = 0; y <= 36; y++) {
            final int yy = y;
            double r = 4.6 - y * 0.035;
            c.ring(cx, y, cz, r - 1.4, r, (x, yv, z) -> (yy / 6) % 2 == 0 ? c.state(Blocks.CONCRETE.pick(DyeColor.WHITE)) : c.state(Blocks.CONCRETE.pick(DyeColor.RED)));
            if (y % 9 == 5) c.ring(cx, y, cz, r - 0.5, r, (x, yv, z) -> Math.abs(x - cx) <= 0 ? glass() : null);
        }
        for (int y = 0; y <= 36; y++) c.set(cx + 2, y, cz, Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        for (int dy = 0; dy < 3; dy++) c.set(cx, dy, cz - 5, Blocks.AIR);
        c.disc(cx, 37, cz, 5.5, (x, y, z) -> c.state(Blocks.POLISHED_ANDESITE));
        c.ring(cx, 38, cz, 4.2, 5.4, (x, y, z) -> c.state(Blocks.IRON_BARS));
        c.ring(cx, 38, cz, 0, 1.6, (x, y, z) -> c.state(Blocks.SEA_LANTERN));
        for (int y = 38; y <= 41; y++) c.ring(cx, y, cz, 3.0, 4.2, (x, yv, z) -> glass());
        c.disc(cx, 42, cz, 5.2, (x, y, z) -> c.state(Blocks.CONCRETE.pick(DyeColor.RED)));
        c.disc(cx, 43, cz, 3.4, (x, y, z) -> c.state(Blocks.CONCRETE.pick(DyeColor.RED)));
        c.disc(cx, 44, cz, 1.6, (x, y, z) -> c.state(Blocks.CONCRETE.pick(DyeColor.RED)));
        for (int y = 45; y <= 52; y++) c.set(cx, y, cz, Blocks.END_ROD);
    }

    private BlockState glass() {
        return Blocks.STAINED_GLASS.pick(DyeColor.LIGHT_BLUE).defaultBlockState();
    }

    // ------------------------------------------------------------------ moulin et champs

    private void farm() {
        int mx = CX - 84, mz = -30;
        c.disc(mx, -1, mz, 8, (x, y, z) -> c.state(Blocks.COBBLESTONE));
        for (int y = 0; y <= 18; y++) {
            final int yy = y;
            double r = 5.5 - y * 0.12;
            c.ring(mx, y, mz, r - 1.3, r, (x, yv, z) -> (yy % 6 == 5) ? c.state(Blocks.DARK_OAK_PLANKS) : c.mix(x, yv, z, 87, "marbre", 60, Blocks.DIORITE, 20, Blocks.CALCITE, 20));
        }
        for (int k = 0; k < 9; k++) {
            double r = 4.4 - k * 0.55;
            if (r < 0.5) break;
            final int kk = k;
            c.disc(mx, 19 + k, mz, r, (x, y, z) -> kk % 3 == 2 ? c.state(Blocks.DARK_OAK_PLANKS) : c.state(Blocks.SPRUCE_PLANKS));
        }
        for (int dy = 0; dy < 3; dy++) c.set(mx, dy, mz + 5, Blocks.AIR);
        for (int y = 0; y <= 17; y++) c.set(mx - 1, y, mz, Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        int hx = mx + 7, hy = 14;                                                           // moyeu et quatre ailes tournees vers l'est
        c.set(hx - 1, hy, mz, c.axis(Blocks.DARK_OAK_LOG, Direction.Axis.X));
        c.set(hx, hy, mz, c.axis(Blocks.DARK_OAK_LOG, Direction.Axis.X));
        for (int k = 1; k <= 9; k++) {
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                c.set(hx, hy + d[0] * k, mz + d[1] * k, Blocks.DARK_OAK_FENCE);
                if (k >= 3) {
                    int sx = d[0] == 0 ? 1 : 0, sz2 = d[1] == 0 ? 1 : 0;
                    c.set(hx, hy + d[0] * k + (d[0] != 0 ? 0 : 1), mz + d[1] * k + (d[1] != 0 ? 0 : 0), Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
                    c.set(hx, hy + d[0] * k + (d[0] != 0 ? 0 : -1), mz + d[1] * k, Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
                    if (sx + sz2 > 5) break;
                }
            }
        }
        HubBuilder.text(c, mx, 26, mz, "MOULIN D'AELORIA", "#FFE08A", 4.4F);
        crops(CX - 104, -26, 17, 11, Blocks.WHEAT);
        crops(CX - 104, 26, 17, 11, Blocks.CARROTS);
        crops(CX - 80, 30, 13, 11, Blocks.POTATOES);
        HubTown.house(c, CX - 72, -4, -1, 0, 11, 9, 2, "tuile_azur", HubTown.Style.ATELIER);       // grange, face a l'avenue (ouest)
        for (int s : new int[]{-1, 1}) {                                                    // enclos de paille
            int px = CX - 100, pz = 44 * s;
            for (int dx = -7; dx <= 7; dx++) for (int dz = -4; dz <= 4; dz++) {
                boolean edge = Math.abs(dx) == 7 || Math.abs(dz) == 4;
                if (Math.abs(pz) + dz < 6 || polar(px + dx, pz + dz) > R - 6) continue;
                if (edge) c.set(px + dx, 0, pz + dz, Blocks.OAK_FENCE);
                else if ((dx + dz) % 4 == 0) c.set(px + dx, 0, pz + dz, Blocks.HAY_BLOCK);
            }
        }
    }

    private void crops(int cx, int cz, int w, int d, Block crop) {
        int hw = w / 2, hd = d / 2;
        for (int dx = -hw - 1; dx <= hw + 1; dx++) {
            for (int dz = -hd - 1; dz <= hd + 1; dz++) {
                int x = cx + dx, z = cz + dz;
                boolean border = Math.abs(dx) > hw || Math.abs(dz) > hd;
                for (int y = 0; y <= 2; y++) if (!c.air(x, y, z)) c.set(x, y, z, Blocks.AIR);
                if (border) {
                    c.set(x, -1, z, Blocks.COARSE_DIRT);
                    boolean gap = dz == hd + 1 && dx == 0 || dz == -hd - 1 && dx == 0;
                    if (!gap) c.set(x, 0, z, Blocks.OAK_FENCE.defaultBlockState()
                            .setValue(FenceBlock.EAST, Math.abs(dx + 1) <= hw + 1 && (Math.abs(dz) > hd || dx + 1 > hw))
                            .setValue(FenceBlock.WEST, Math.abs(dx - 1) <= hw + 1 && (Math.abs(dz) > hd || dx - 1 < -hw))
                            .setValue(FenceBlock.NORTH, Math.abs(dz - 1) <= hd + 1 && (Math.abs(dx) > hw || dz - 1 < -hd))
                            .setValue(FenceBlock.SOUTH, Math.abs(dz + 1) <= hd + 1 && (Math.abs(dx) > hw || dz + 1 > hd)));
                } else if (dz == 0) {
                    c.set(x, -1, z, Blocks.WATER);
                } else {
                    c.set(x, -1, z, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
                    c.set(x, 0, z, crop.defaultBlockState().setValue(CropBlock.AGE, 7));
                }
            }
        }
        mark(cx - hw - 3, cz - hd - 3, cx + hw + 3, cz + hd + 3);
    }

    // ------------------------------------------------------------------ halles de l'est

    private void east() {
        HubTown.house(c, CX + 92, -24, 0, 1, 33, 15, 4, "tuile_pourpre", HubTown.Style.GUILDE);      // grande halle des guildes (face a l'avenue)
        HubTown.house(c, CX + 92, 24, 0, -1, 29, 15, 3, "tuile_azur", HubTown.Style.TAVERNE);         // l'auberge
        HubBuilder.text(c, CX + 92, 24, -9, "HALLE DES GUILDES", "#FFC080", 4.4F);
        HubBuilder.text(c, CX + 92, 21, 9, "AUBERGE D'AELORIA", "#FFC080", 4.4F);
        HubTown.house(c, CX + 66, -24, 0, 1, 13, 11, 3, "tuile_pourpre", HubTown.Style.MAISON);
        HubTown.house(c, CX + 66, 24, 0, -1, 13, 11, 3, "tuile_azur", HubTown.Style.ATELIER);
    }

    // ------------------------------------------------------------------ maisons le long des rues circulaires

    private void houses() {
        int[][] sizes = {{9, 9, 2}, {9, 11, 2}, {11, 9, 3}, {9, 9, 3}, {13, 9, 3}, {11, 11, 2}};
        HubTown.Style[] styles = {HubTown.Style.MAISON, HubTown.Style.MAISON, HubTown.Style.MAISON, HubTown.Style.ATELIER, HubTown.Style.TAVERNE, HubTown.Style.VENTES, HubTown.Style.MAISON};
        int n = 0;
        for (int ring : new int[]{66, 94}) {
            for (int side : new int[]{-1, 1}) {
                double off = side * 11.5;
                double step = 13.5 / (ring + off);
                for (double a = 0.12; a < Math.PI * 2; a += step) {
                    n++;
                    if (c.rnd(n, 3, ring, 90) < 0.16) continue;                              // trouées : places, jardins
                    int x = CX + (int) Math.round(Math.cos(a) * (ring + off)), z = (int) Math.round(Math.sin(a) * (ring + off));
                    Direction face = Math.abs(Math.cos(a)) > Math.abs(Math.sin(a)) ? (Math.cos(a) * side > 0 ? Direction.WEST : Direction.EAST)
                            : (Math.sin(a) * side > 0 ? Direction.NORTH : Direction.SOUTH);
                    int[] sz = sizes[(int) (c.rnd(n, 4, ring, 91) * sizes.length) % sizes.length];
                    int w = sz[0], d = sz[1];
                    int ex = face.getAxis() == Direction.Axis.X ? d : w, ez = face.getAxis() == Direction.Axis.X ? w : d;
                    int hx = ex / 2 + 3, hz = ez / 2 + 3;
                    if (polar(x, z) + Math.max(hx, hz) > R - 9 || polar(x, z) - Math.max(hx, hz) < 53) continue;
                    if (!free(x - hx, z - hz, x + hx, z + hz)) continue;
                    HubTown.Style st = styles[(int) (c.rnd(n, 5, ring, 92) * styles.length) % styles.length];
                    HubTown.house(c, x, z, face.getStepX(), face.getStepZ(), w, d, sz[2], TILES[(n + (side > 0 ? 1 : 0)) & 1], st);
                    mark(x - hx, z - hz, x + hx, z + hz);
                }
            }
        }
    }

    // ------------------------------------------------------------------ arbres, puits et detail

    private void scenery() {
        for (int a = 0; a < 360; a += 90) {                                                // puits aux carrefours des avenues et de la rue exterieure
            double rad = Math.toRadians(a);
            int x = CX + (int) Math.round(Math.cos(rad) * 80), z = (int) Math.round(Math.sin(rad) * 80);
            if (busyReserved(x, z) && a != 0 && a != 180) continue;
            well(x + (a == 0 || a == 180 ? 0 : 8), z + (a == 0 || a == 180 ? 8 : 0));
        }
        int n = 0;
        for (int x = CX - R + 8; x <= CX + R - 8; x += 3) {
            for (int z = -R + 8; z <= R - 8; z += 3) {
                n++;
                if (busy(x, z) || polar(x, z) > R - 10 || polar(x, z) < 56) continue;
                if (c.rnd(x, 7, z, 93) > 0.18F) continue;
                int dx = x + (int) (c.rnd(x, 8, z, 94) * 3) - 1, dz = z + (int) (c.rnd(x, 9, z, 95) * 3) - 1;
                if (!free(dx - 3, dz - 3, dx + 3, dz + 3)) continue;
                tree(dx, dz, c.rnd(x, 10, z, 96));
                mark(dx - 2, dz - 2, dx + 2, dz + 2);
            }
        }
    }

    private void well(int x, int z) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            c.set(x + dx, -1, z + dz, c.state(Blocks.STONE_BRICKS));
            for (int y = 0; y <= 5; y++) c.set(x + dx, y, z + dz, Blocks.AIR);
            if (Math.abs(dx) == 2 || Math.abs(dz) == 2) c.set(x + dx, 0, z + dz, c.state(Blocks.STONE_BRICK_WALL));
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) c.set(x + dx, -1, z + dz, Blocks.WATER);
        for (int s : new int[]{-2, 2}) for (int t : new int[]{-2, 2}) for (int y = 1; y <= 3; y++) c.set(x + s, y, z + t, Blocks.DARK_OAK_FENCE);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) c.set(x + dx, 4, z + dz, Blocks.DARK_OAK_SLAB);
        c.set(x, 3, z, c.hanging(Blocks.LANTERN));
        mark(x - 4, z - 4, x + 4, z + 4);
    }

    private void tree(int x, int z, float v) {
        int th = 4 + (int) (v * 3);
        boolean birch = v > 0.7F;
        Block log = birch ? Blocks.BIRCH_LOG : Blocks.OAK_LOG;
        BlockState lv = c.leaves(birch ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES);
        for (int y = 0; y <= th; y++) c.set(x, y, z, log);
        for (int dy = th - 2; dy <= th + 1; dy++) {
            int rad = dy >= th ? 1 : 2;
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (Math.abs(dx) == rad && Math.abs(dz) == rad && (rad == 2 || dy == th + 1 || c.rnd(x + dx, dy, z + dz, 97) < 0.5F)) continue;
                    if (c.air(x + dx, dy, z + dz)) c.set(x + dx, dy, z + dz, lv);
                }
            }
        }
    }

    // ------------------------------------------------------------------ textes

    static void texts(HubCtx c) {
        HubBuilder.text(c, CX, 30, 0, "CITÉ D'AELORIA", "#FFD060", 11F);
    }
}
