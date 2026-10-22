package com.mmorpg.hub;

import com.mmorpg.rpg.PlayerClass;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Jardins du hub (arbres arrondis, haies courbes, parterres circulaires) et ilots flottants alentour. */
final class HubFlora {
    private HubFlora() {
    }

    private static boolean nearPavilion(double x, double z, double margin) {
        for (PlayerClass cls : HubBuilder.CLASSES) {
            int[] s = HubLayout.classSpot(cls);
            if (Math.hypot(x - s[0], z - s[2]) < margin) return true;
        }
        return false;
    }

    static void gardens(HubCtx c) {
        for (int gx = -34; gx <= 56; gx += 4) {                    // arbres : semis regulier decale par le hasard, sur les pelouses seulement
            for (int gz = -56; gz <= 62; gz += 4) {
                int x = gx + (int) (c.rnd(gx, 0, gz, 301) * 3);
                int z = gz + (int) (c.rnd(gx, 1, gz, 302) * 3);
                double r = Math.hypot(x, z);
                boolean plazaLawn = r > 18.6 && r < 26.0 && !HubBuilder.avenue(x, z) && !(r > 21.0 && r < 23.4);
                boolean parvis = Math.hypot(x, z - 46) > 6 && Math.hypot(x, z - 46) < 12 && Math.abs(x) > 4 && z > 38;
                boolean rim = !c.in(c.foot, x, z) && c.in(c.isle, x, z) && c.edge[x - HubCtx.X0][z - HubCtx.Z0] > 2.5F;
                if (!(plazaLawn || parvis || rim)) continue;
                boolean onDiagonal = r < 34 && Math.abs(Math.abs(x) - Math.abs(z)) / 1.414 < 9.0;      // couloir de vue vers les pavillons
                boolean onAxis = r < 70 && (Math.abs(x) < 9 || Math.abs(z) < 9);                       // couloirs de la cinematique le long des axes
                if (nearPavilion(x, z, 13.5) || onDiagonal || onAxis || c.rnd(x, 2, z, 303) > 0.62F) continue;
                if (c.get(x, -1, z).getBlock() != Blocks.GRASS_BLOCK && !(rim && c.get(x, 0, z).isAir())) continue;
                int type = (int) (c.rnd(x, 4, z, 304) * 4);
                int h = 5 + (int) (c.rnd(x, 5, z, 305) * 4);
                tree(c, x, z, type, h);
            }
        }
        for (int ring = 0; ring < 2; ring++) {                       // haies basses en arcs, interrompues aux avenues
            double r0 = ring == 0 ? 17.8 : 23.0;
            double r1 = ring == 0 ? 18.6 : 23.8;
            c.ring(0, 0, 0, r0, r1, (x, y, z) -> {
                if (HubBuilder.avenue(x, z) || nearPavilion(x, z, 12.5)) return null;
                return c.rnd(x, 0, z, 311) < 0.28F ? c.leaves(Blocks.FLOWERING_AZALEA_LEAVES) : c.leaves(Blocks.AZALEA_LEAVES);
            });
        }
        for (int k = 0; k < 8; k++) {                                // parterres circulaires de fleurs
            double a = Math.toRadians(22.5 + k * 45);
            int bx = (int) Math.round(Math.cos(a) * 19.8);
            int bz = (int) Math.round(Math.sin(a) * 19.8);
            if (nearPavilion(bx, bz, 14.5)) continue;
            Block[] mix = k % 2 == 0 ? new Block[]{Blocks.PINK_TULIP, Blocks.ALLIUM, Blocks.LILAC} : new Block[]{Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY};
            c.disc(bx, -1, bz, 3.0, (x, y, z) -> Math.hypot(x - bx, z - bz) > 2.2 ? c.state("pierre_solaire") : Blocks.GRASS_BLOCK.defaultBlockState());
            c.disc(bx, 0, bz, 2.2, (x, y, z) -> c.rnd(x, 0, z, 321) < 0.82F ? mix[(int) (c.rnd(x, 1, z, 322) * mix.length) % mix.length].defaultBlockState()
                    : Blocks.SHORT_GRASS.defaultBlockState());
        }
        for (int k = 0; k < 4; k++) {                                // bancs en arc sous les arbres de l'allee circulaire
            double a = Math.toRadians(k * 90 + 45 + 17);
            int x = (int) Math.round(Math.cos(a) * 24.3);
            int z = (int) Math.round(Math.sin(a) * 24.3);
            if (nearPavilion(x, z, 13)) continue;
            Direction f = Math.abs(x) > Math.abs(z) ? (x > 0 ? Direction.WEST : Direction.EAST) : (z > 0 ? Direction.NORTH : Direction.SOUTH);
            c.set(x, 0, z, c.stair("bois_ancien", f, false));
        }
    }

    /** Arbre aux frondaisons arrondies : cerisier, chene, bouleau ou grand arbre a fleurs. */
    private static void tree(HubCtx c, int x, int z, int type, int h) {
        Block log = type == 0 || type == 3 ? Blocks.CHERRY_LOG : type == 1 ? Blocks.OAK_LOG : Blocks.BIRCH_LOG;
        for (int y = 0; y < h; y++) c.set(x, y, z, c.axis(log, Direction.Axis.Y));
        double rx = type == 3 ? 5.2 : 3.8 + c.rnd(x, 6, z, 331) * 1.2;
        double ry = type == 3 ? 4.0 : 3.0;
        HubCtx.Pick leafPick = switch (type) {
            case 0, 3 -> (xx, yy, zz) -> c.rnd(xx, yy, zz, 332) < 0.12F ? null : c.leaves(Blocks.CHERRY_LEAVES);
            case 1 -> (xx, yy, zz) -> c.rnd(xx, yy, zz, 333) < 0.1F ? null : c.rnd(xx, yy, zz, 334) < 0.18F ? c.leaves(Blocks.FLOWERING_AZALEA_LEAVES) : c.leaves(Blocks.OAK_LEAVES);
            default -> (xx, yy, zz) -> c.rnd(xx, yy, zz, 335) < 0.1F ? null : c.leaves(Blocks.BIRCH_LEAVES);
        };
        c.ball(x, h + 1, z, rx, ry, rx, true, leafPick);
        c.ball(x + 2, h - 1, z - 1, rx * 0.65, ry * 0.7, rx * 0.65, true, leafPick);
        c.ball(x - 2, h, z + 2, rx * 0.6, ry * 0.7, rx * 0.6, true, leafPick);
        if (type == 3) {                                             // branches charpentieres
            c.line(x, h - 2, z, x + 3, h + 1, z + 1, c.axis(log, Direction.Axis.X));
            c.line(x, h - 3, z, x - 3, h, z - 2, c.axis(log, Direction.Axis.X));
        }
        for (int[] o : new int[][]{{1, 0}, {-1, 1}, {0, -1}}) c.setIfAir(x + o[0], 0, z + o[1], Blocks.PINK_PETALS.defaultBlockState());
    }

    // ================================================================== ilots flottants

    static void islets(HubCtx c) {
        int[][] spots = {{-64, 8, -14, 8}, {-56, 20, 36, 6}, {-74, -2, 16, 5}, {46, 14, 62, 7}, {74, 8, 34, 6}, {86, 2, -26, 7},
                {-22, 28, -124, 10}, {40, 18, -104, 6}, {-54, 12, -86, 7}, {20, 34, 70, 5}, {-8, 6, 78, 6}};
        for (int i = 0; i < spots.length; i++) {
            int[] s = spots[i];
            int cx = s[0];
            int cy = s[1];
            int cz = s[2];
            int r = s[3];
            if (!c.air(cx, cy, cz) || !c.air(cx, cy + 7, cz)) continue;          // jamais dans du relief existant
            for (int dx = -r - 1; dx <= r + 1; dx++) {
                for (int dz = -r - 1; dz <= r + 1; dz++) {
                    double d = Math.hypot(dx, dz) + (c.noise(cx + dx, cz + dz, 3F, 71) - 0.5) * 2.4;
                    if (d > r) continue;
                    int depth = Math.max(2, (int) Math.round((r - d) * 1.9 + c.noise(cx + dx, cz + dz, 2.5F, 72) * 2));
                    c.set(cx + dx, cy, cz + dz, Blocks.GRASS_BLOCK);
                    c.set(cx + dx, cy - 1, cz + dz, Blocks.DIRT);
                    for (int k = 2; k <= depth; k++) {
                        c.set(cx + dx, cy - k, cz + dz, c.mix(cx + dx, cy - k, cz + dz, 73, "roche_celeste", 55, Blocks.STONE, 20, Blocks.TUFF, 12, Blocks.MOSS_BLOCK, 13));
                    }
                    if (depth >= 4 && c.rnd(cx + dx, 3, cz + dz, 75) < 0.14F) c.set(cx + dx, cy - depth - 1, cz + dz, c.rnd(dx, 1, dz, 76) < 0.6F ? "cristal_azur" : "cristal_arcane");
                    if (depth >= 3 && c.rnd(cx + dx, 4, cz + dz, 77) < 0.35F) {
                        int len = 2 + (int) (c.rnd(dx, 2, dz, 78) * 4);
                        for (int k = 0; k < len; k++) {
                            BlockState vine = (k == len - 1 ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
                                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BERRIES, true);
                            if (c.air(cx + dx, cy - depth - 1 - k, cz + dz)) c.set(cx + dx, cy - depth - 1 - k, cz + dz, vine);
                        }
                    }
                    if (d < r - 1 && c.rnd(cx + dx, cy, cz + dz, 79) < 0.22F) {
                        c.set(cx + dx, cy + 1, cz + dz, c.rnd(dx, 3, dz, 80) < 0.5F ? Blocks.SHORT_GRASS.defaultBlockState()
                                : HubBuilder.FLOWERS[(int) (c.rnd(dx, 4, dz, 81) * HubBuilder.FLOWERS.length) % HubBuilder.FLOWERS.length].defaultBlockState());
                    }
                }
            }
            tree(c, cx, cz, i % 4, 5 + i % 3);
            if (r >= 6) {
                tree(c, cx + r / 2, cz + 1, (i + 1) % 2 == 0 ? 0 : 2, 4);
                c.set(cx - r / 2, cy + 1, cz - 1, Blocks.LANTERN);
            }
            if (i % 2 == 0 && c.air(cx + r - 1, cy + 1, cz)) c.set(cx + r - 1, cy, cz, Blocks.WATER);        // petite cascade
        }
    }

}
