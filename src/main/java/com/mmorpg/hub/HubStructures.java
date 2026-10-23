package com.mmorpg.hub;

import com.mmorpg.registry.ModBlocks;
import com.mmorpg.world.WaypointData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Structures monumentales du hub : portail a tours rondes et grande arche, plateforme du teleporteur, voie a arches vers l'arene,
 * arene d'epreuve a dome et aire d'entrainement a palissade circulaire.
 */
final class HubStructures {
    private HubStructures() {
    }

    private static BlockState glass() {
        return Blocks.STAINED_GLASS.pick(DyeColor.LIGHT_BLUE).defaultBlockState();
    }

    // ================================================================== portail sud

    static void gate(HubCtx c) {
        c.disc(0, -2, 46, 13.4, (x, y, z) -> c.state(Blocks.STONE_BRICKS));
        c.disc(0, -1, 46, 13.2, (x, y, z) -> {                    // parvis : mosaique, allee centrale, pelouses
            double r = Math.hypot(x, z - 46);
            if (r <= 1.8) return c.state("marbre_dore");
            if (r <= 2.9) return c.state("or_ouvrage");
            if (r > 11.8) return c.state("pierre_solaire");
            if (Math.abs(x) <= 2) return c.mix(x, y, z, 61, "marbre_poli", 70, "marbre", 30);
            if (r <= 5) return c.mix(x, y, z, 62, "marbre_poli", 50, "pierre_solaire", 50);
            return Blocks.GRASS_BLOCK.defaultBlockState();
        });
        for (int x = -2; x <= 2; x++) for (int z = 30; z <= 46; z++) {
            c.set(x, -2, z, Blocks.STONE_BRICKS);
            c.set(x, -1, z, c.mix(x, 0, z, 63, "marbre_poli", 70, "marbre", 30));
        }
        for (int x = -11; x <= 11; x++) for (int z = 31; z <= 46; z++) {
            if (c.get(x, -1, z).getBlock() == Blocks.GRASS_BLOCK) {
                float g = c.rnd(x, 0, z, 64);
                if (g < 0.2F) c.set(x, 0, z, Blocks.SHORT_GRASS);
                else if (g < 0.33F) c.set(x, 0, z, HubBuilder.FLOWERS[(int) (c.rnd(x, 1, z, 65) * HubBuilder.FLOWERS.length) % HubBuilder.FLOWERS.length]);
            }
        }
        for (int z = 38; z <= 54; z += 5) for (int s : new int[]{-4, 4}) HubBuilder.lamp(c, s, z);
        for (int s : new int[]{-1, 1}) tower(c, s * 12, 36);
        // grande arche entre les tours
        for (int x = -9; x <= 9; x++) {
            for (int z = 34; z <= 37; z++) {
                for (int y = 0; y <= 16; y++) {
                    double d = Math.hypot(x, y - 6.0);
                    BlockState s;
                    if (d <= 6.2) {
                        s = Blocks.AIR.defaultBlockState();
                        if (y < 6 && Math.abs(x) <= 6) s = Blocks.AIR.defaultBlockState();
                    } else if (d <= 7.6 && y >= 6) s = c.state(y == 13 && x == 0 ? "or_ouvrage" : "marbre_dore");
                    else if (y >= 12 && y <= 13) s = c.state(y == 12 && Math.abs(x) % 2 == 0 ? "cristal_ambre" : "or_ouvrage");
                    else s = c.mix(x, y, z, 66, "pierre_solaire", 50, "marbre", 30, "marbre_poli", 20);
                    c.set(x, y, z, s);
                }
                if (x % 2 == 0) c.set(x, 17, z, "pierre_solaire");
            }
        }
        for (int x = -6; x <= 6; x++) for (int z = 34; z <= 37; z++) c.set(x, -1, z, c.state("marbre_poli"));
        for (int s : new int[]{-1, 1}) {                          // banieres de l'arche
            for (int y = 7; y <= 11; y++) c.set(s * 8, y, 33, Blocks.WALL_BANNER.pick(s < 0 ? DyeColor.RED : DyeColor.BLUE).defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        }
        for (int x = -4; x <= 4; x += 4) c.set(x, 5, 35, c.hanging(Blocks.LANTERN));
    }

    private static void tower(HubCtx c, int tx, int tz) {
        for (int y = 0; y <= 22; y++) {
            final int yy = y;
            c.ring(tx, y, tz, 3.6, 5.2, (x, yq, z) -> {
                if (yy == 12) return (x + z) % 2 == 0 ? c.state("cristal_ambre") : c.state("or_ouvrage");
                if (yy == 0 || yy == 22) return c.state("or_ouvrage");
                if (yy % 6 == 5) return c.state("marbre_dore");
                return c.mix(x, yy, z, 67, "pierre_solaire", 50, "marbre", 30, "marbre_poli", 20);
            });
        }
        for (int[] o : new int[][]{{0, 5}, {0, -5}, {5, 0}, {-5, 0}, {4, 4}, {-4, 4}}) {      // meurtrieres et vitraux
            for (int y = 7; y <= 9; y++) c.set(tx + o[0], y, tz + o[1], glass());
            for (int y = 16; y <= 18; y++) c.set(tx + o[0], y, tz + o[1], glass());
        }
        c.disc(tx, 22, tz, 5.2, (x, y, z) -> c.state("marbre"));
        for (int k = 0; k < 13; k++) {                          // toit conique : couches de rayon decroissant
            double r = 6.6 - k * 0.52;
            if (r < 0.6) r = 0.6;
            final int kk = k;
            c.disc(tx, 23 + k, tz, r, (x, y, z) -> kk == 0 ? c.state("marbre_dore") : c.state("tuile_azur"));
        }
        for (int y = 36; y <= 39; y++) c.set(tx, y, tz, y == 36 ? c.state("or_ouvrage") : Blocks.END_ROD.defaultBlockState());
        for (int k = 0; k < 8; k++) {                           // corniche de lanternes
            double a = Math.toRadians(k * 45);
            c.set(tx + (int) Math.round(Math.cos(a) * 5.9), 21, tz + (int) Math.round(Math.sin(a) * 5.9), c.hanging(Blocks.LANTERN));
        }
    }

    // ================================================================== teleporteur

    static void teleporter(HubCtx c) {
        int tz = 24;
        c.disc(0, -2, tz, 7, (x, y, z) -> c.state(Blocks.STONE_BRICKS));
        c.disc(0, -1, tz, 6.8, (x, y, z) -> {
            double r = Math.hypot(x, z - tz);
            if (r <= 0.9) return c.state("marbre_dore");
            if (r <= 2.2) return c.state("or_ouvrage");
            if (r <= 3.2) return c.state("marbre_poli");
            if (r <= 5.6) return r > 4.2 && r <= 5.0 ? c.state("dalle_runique") : c.mix(x, y, z, 71, "marbre_poli", 60, "pierre_azur", 40);
            return c.state("marbre");
        });
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + k * 45);
            int x = (int) Math.round(Math.cos(a) * 5.6);
            int z = tz + (int) Math.round(Math.sin(a) * 5.6);
            c.set(x, 0, z, "marbre_dore");
            for (int y = 1; y <= 7; y++) c.set(x, y, z, "colonne_marbre");
            c.set(x, 8, z, "marbre_dore");
            c.set(x, 9, z, "cristal_azur");
            c.set(x, 10, z, Blocks.END_ROD);
        }
        c.ring(0, 8, tz, 4.6, 6.4, (x, y, z) -> c.state("marbre"));
        c.ring(0, 9, tz, 5.0, 6.0, (x, y, z) -> c.slab("or_ouvrage", false));
        BlockPos tp = HubLayout.pos(c.O, HubLayout.TELEPORTER);
        c.L.setBlock(tp, ModBlocks.TELEPORTEUR.get().defaultBlockState(), Block.UPDATE_ALL);
        WaypointData data = WaypointData.get(c.L.getServer());
        data.removeAt(c.L.dimension(), tp);
        data.create(c.L.dimension(), tp, HubBuilder.instance() == 0 ? "Hub d'Aeloria" : "Hub d'Aeloria " + (HubBuilder.instance() + 1), WaypointData.CITY, 1, new UUID(0, 0));
    }

    // ================================================================== voie de l'arene : arches de marbre

    static void causeway(HubCtx c) {
        for (int z = -54; z <= -28; z++) {
            for (int x = -7; x <= 7; x++) {
                int ax = Math.abs(x);
                BlockState top = ax <= 1 ? c.state("marbre_dore") : ax <= 3 ? c.mix(x, 0, z, 81, "marbre_poli", 65, "marbre", 35) : ax == 4 ? c.state("pierre_solaire") : Blocks.GRASS_BLOCK.defaultBlockState();
                c.set(x, -2, z, ax <= 4 ? c.state(Blocks.STONE_BRICKS) : c.state(Blocks.DIRT));
                c.set(x, -1, z, top);
                for (int y = 0; y <= 14; y++) c.set(x, y, z, Blocks.AIR);
                if (ax >= 5 && c.rnd(x, 0, z, 82) < 0.22F) c.set(x, 0, z, c.rnd(x, 1, z, 83) < 0.6F ? Blocks.SHORT_GRASS.defaultBlockState()
                        : HubBuilder.FLOWERS[(int) (c.rnd(x, 2, z, 84) * HubBuilder.FLOWERS.length) % HubBuilder.FLOWERS.length].defaultBlockState());
            }
        }
        for (int z = -31; z >= -49; z -= 6) for (int s : new int[]{-6, 6}) HubBuilder.lamp(c, s, z);
        for (int z = -34; z >= -50; z -= 6) {                    // arches en plein cintre
            for (int s : new int[]{-1, 1}) {
                for (int y = 0; y <= 5; y++) c.set(s * 5, y, z, y == 0 ? c.state("marbre_dore") : c.state("colonne_marbre"));
            }
            for (int x = -6; x <= 6; x++) {
                for (int y = 5; y <= 12; y++) {
                    double d = Math.hypot(x, y - 5.0);
                    if (d > 4.2 && d <= 6.0) c.set(x, y, z, d > 5.2 && y > 9 ? c.state("or_ouvrage") : c.state("marbre"));
                }
            }
            c.set(0, 11, z, "cristal_azur");
            c.set(0, 6, z, c.hanging(Blocks.LANTERN));
        }
    }

    // ================================================================== arene d'epreuve

    static void arena(HubCtx c) {
        final int zc = -74;
        final int R = HubLayout.ROOM_HALF;
        c.disc(0, -2, zc, R + 1.4, (x, y, z) -> c.state(Blocks.DEEPSLATE_BRICKS));
        c.disc(0, -1, zc, R + 0.6, (x, y, z) -> {                  // sol : etoile centrale, anneaux, dalles sombres
            double dx = x;
            double dz = z - zc;
            double r = Math.hypot(dx, dz);
            if (r <= 2.6) return c.state("marbre_dore");
            if (r <= 4.2) return c.state("or_ouvrage");
            if (r > 4.2 && r <= 14 && (Math.abs(((Math.toDegrees(Math.atan2(dz, dx)) % 45) + 45) % 45 - 22.5) > 20.2)) return c.state("dalle_runique");
            if (r > 8.4 && r <= 9.2) return c.state("or_ouvrage");
            if (r > 13.2 && r <= 14.0) return c.state("or_ouvrage");
            if (r > 17.2) return c.state("pierre_solaire");
            return c.mix(x, y, z, 91, Blocks.POLISHED_DEEPSLATE, 40, Blocks.DEEPSLATE_TILES, 30, "fer_noirci", 18, Blocks.POLISHED_BLACKSTONE, 12);
        });
        for (int y = 0; y <= 40; y++) c.disc(0, y, zc, R + 0.6, (x, yy, z) -> Blocks.AIR.defaultBlockState());
        c.ring(0, 0, zc, 17.4, 19.2, (x, y, z) -> seat(c, x, z, zc, 0, 0));       // gradins en anneaux
        c.ring(0, 0, zc, 19.2, 20.8, (x, y, z) -> c.state("marbre_poli"));
        c.ring(0, 1, zc, 19.2, 20.8, (x, y, z) -> seat(c, x, z, zc, 1, 1));
        c.wall(0, zc, R + 0.4, 0, 14, 2.0, (x, y, z) -> {
            double a = ((Math.toDegrees(Math.atan2(z - zc, x)) % 360) + 360) % 360;
            double m = a % 22.5;
            if (y == 7 || y == 14) return c.state("or_ouvrage");
            if (y >= 4 && y <= 11 && m > 6.5 && m < 16 && Math.hypot(x, z - zc) > R - 0.6) return glass();
            if (m < 2.4 || m > 20.1) return y == 3 || y == 10 ? c.state("cristal_ambre") : c.state("marbre_dore");
            return c.mix(x, y, z, 92, "pierre_solaire", 45, "marbre", 30, "marbre_poli", 25);
        });
        for (int x = -7; x <= 7; x++) {                              // entree au sud : grande arche
            for (int z = zc + R - 3; z <= zc + R + 2; z++) {
                for (int y = 0; y <= 12; y++) {
                    double d = Math.hypot(x, y - 5.0);
                    boolean open = (y <= 5 && Math.abs(x) <= 5) || d <= 5.2;
                    if (open) c.set(x, y, z, Blocks.AIR);
                    else if (d <= 6.4 && y >= 5) c.set(x, y, z, c.state(d > 5.8 && y > 9 ? "or_ouvrage" : "marbre_dore"));
                }
            }
        }
        c.dome(0, 14, zc, R + 1.6, 1.5, (x, y, z, el, az) -> {
            if (el > 80) return null;                                   // oculus
            double m = az % 22.5;
            if (el < 6) return c.state("or_ouvrage");
            if (el > 72) return c.state("or_ouvrage");
            if (m < 2.2 || m > 20.3) return c.state("marbre_dore");
            if (el > 14 && el < 62 && m > 7 && m < 15.5) return glass();
            return c.state("tuile_pourpre");
        });
        for (int k = 0; k < 8; k++) {                                // braseros colonnes au bord de la piste
            double a = Math.toRadians(k * 45 + 22.5);
            int x = (int) Math.round(Math.cos(a) * 15.5);
            int z = zc + (int) Math.round(Math.sin(a) * 15.5);
            for (int y = 0; y <= 3; y++) c.set(x, y, z, "fer_noirci");
            c.set(x, 4, z, Blocks.CAMPFIRE);
        }
    }

    private static BlockState seat(HubCtx c, int x, int z, int zc, int y, int salt) {
        double dx = -x;
        double dz = zc - z;
        Direction f = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        return c.stair(salt == 0 ? "marbre_poli" : "marbre", f, false);
    }

    // ================================================================== aire d'entrainement

    static void yard(HubCtx c) {
        final int cx = HubLayout.YARD_CENTER_X;
        for (int x = 28; x <= 46; x++) {                              // allee depuis la place
            for (int z = -7; z <= 7; z++) {
                int az = Math.abs(z);
                c.set(x, -2, z, az <= 3 ? c.state(Blocks.STONE_BRICKS) : c.state(Blocks.DIRT));
                c.set(x, -1, z, az <= 1 ? c.state("marbre_dore") : az <= 3 ? c.mix(x, 0, z, 101, "marbre_poli", 60, "pierre_solaire", 40) : Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = 0; y <= 8; y++) c.set(x, y, z, Blocks.AIR);
                if (az >= 4 && c.rnd(x, 0, z, 102) < 0.25F) c.set(x, 0, z, Blocks.SHORT_GRASS);
            }
        }
        for (int x = 34; x <= 44; x += 5) for (int s : new int[]{-4, 4}) HubBuilder.lamp(c, x, s);
        c.disc(cx, -2, 0, 16.4, (x, y, z) -> c.state(Blocks.DIRT));
        c.disc(cx, -1, 0, 16, (x, y, z) -> {
            double r = Math.hypot(x - cx, z);
            if (r <= 2.6) return c.state(Blocks.BLACKSTONE);
            if (r > 14.4) return c.state("roche_celeste");
            return c.mix(x, y, z, 103, Blocks.COARSE_DIRT, 30, Blocks.PACKED_MUD, 30, Blocks.GRAVEL, 20, Blocks.DIRT_PATH, 20);
        });
        for (int y = 0; y <= 14; y++) c.disc(cx, y, 0, 16, (x, yy, z) -> Blocks.AIR.defaultBlockState());
        c.wall(cx, 0, 15.6, 0, 3, 1.4, (x, y, z) -> {                  // palissade de rondins
            if (Math.abs(z) <= 3 && x < cx) return Blocks.AIR.defaultBlockState();
            return c.axis(Blocks.SPRUCE_LOG, Direction.Axis.Y);
        });
        c.wall(cx, 0, 15.6, 4, 4, 1.4, (x, y, z) -> {
            if (Math.abs(z) <= 3 && x < cx) return null;
            return ((x + z) & 1) == 0 ? c.state(Blocks.SPRUCE_FENCE) : null;
        });
        for (int k = 0; k < 24; k += 2) {                              // lanternes sur la palissade
            double a = Math.toRadians(k * 15);
            int lx = cx + (int) Math.round(Math.cos(a) * 15.1);
            int lz = (int) Math.round(Math.sin(a) * 15.1);
            if (c.get(lx, 4, lz).getBlock() == Blocks.SPRUCE_FENCE) c.set(lx, 5, lz, Blocks.LANTERN);
            else if (Math.abs(lz) > 3 || lx > cx) c.set(lx, 4, lz, Blocks.LANTERN);
        }
        for (int k = 0; k < 4; k++) {                                 // tours de guet rondes
            double a = Math.toRadians(k * 90 + 45);
            int tx = cx + (int) Math.round(Math.cos(a) * 15.6);
            int tz = (int) Math.round(Math.sin(a) * 15.6);
            for (int y = 0; y <= 9; y++) c.ring(tx, y, tz, 1.3, 2.9, (x, yq, z) -> c.state(Blocks.SPRUCE_LOG));
            c.disc(tx, 10, tz, 3.4, (x, y, z) -> c.state("bois_ancien"));
            for (int j = 0; j < 9; j++) {
                final int jj = j;
                c.disc(tx, 11 + j, tz, Math.max(0.6, 3.8 - j * 0.38), (x, y, z) -> jj == 0 ? c.state("tuile_ecarlate") : c.state("tuile_ecarlate"));
            }
            c.set(tx, 20, tz, "or_ouvrage");
            c.set(tx, 21, tz, Blocks.END_ROD);
        }
        for (int s : new int[]{-1, 1}) {                              // portique d'entree
            for (int y = 0; y <= 6; y++) c.set(cx - 16, y, s * 4, y == 0 ? c.state("or_ouvrage") : c.axis(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
            c.set(cx - 16, 7, s * 4, Blocks.LANTERN);
        }
        for (int z = -4; z <= 4; z++) c.set(cx - 16, 6, z, c.axis(Blocks.DARK_OAK_LOG, Direction.Axis.Z));
        c.set(cx - 16, 5, 0, c.hanging(Blocks.LANTERN));
        for (int k = 0; k < 6; k++) {                                 // feux de camp autour du foyer central
            double a = Math.toRadians(k * 60);
            c.set(cx + (int) Math.round(Math.cos(a) * 3.4), 0, (int) Math.round(Math.sin(a) * 3.4), Blocks.HAY_BLOCK);
        }
        c.set(cx, 0, 0, Blocks.CAMPFIRE);
        for (int k = 0; k < 7; k++) {                                 // pylones et cibles sur l'arc du fond
            double a = Math.toRadians(-60 + k * 20);
            int x = cx + (int) Math.round(Math.cos(a) * 13);
            int z = (int) Math.round(Math.sin(a) * 13);
            c.set(x, 0, z, Blocks.DARK_OAK_FENCE);
            c.set(x, 1, z, Blocks.DARK_OAK_FENCE);
            c.set(x, 2, z, Blocks.TARGET);
        }
    }
}
