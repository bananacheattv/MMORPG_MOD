package com.mmorpg.dungeon;

import com.mmorpg.hub.HubCtx;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Forteresse Infernale : bastion de briques du Nether et de pierre noire doree, ponts au-dessus de la lave, tours a creneaux ;
 * l'arene est une plateforme d'obsidienne cernee d'un fosse de lave que franchissent quatre chaussees.
 */
final class InfernalTheme extends Theme {
    InfernalTheme(HubCtx c, DungeonType t) {
        super(c, t);
    }

    @Override
    BlockState shell(int x, int y, int z, Carve.Kind kind, boolean back) {
        if (back) return c.mix(x, y, z, 1, Blocks.NETHERRACK, 45, Blocks.BLACKSTONE, 35, Blocks.BASALT, 20);
        return switch (kind) {
            case FLOOR -> floor(x, y, z);
            case CEILING -> c.mix(x, y, z, 3, Blocks.NETHER_BRICKS, 40, Blocks.BLACKSTONE, 25, Blocks.CRACKED_NETHER_BRICKS, 15, Blocks.RED_NETHER_BRICKS, 20);
            case WALL -> wall(x, y, z);
        };
    }

    private BlockState wall(int x, int y, int z) {
        if (y == 3 || y == 9) return c.mix(x, y, z, 5, Blocks.POLISHED_BLACKSTONE_BRICKS, 55, Blocks.GILDED_BLACKSTONE, 15, Blocks.CHISELED_POLISHED_BLACKSTONE, 30);
        return c.mix(x, y, z, 2, Blocks.NETHER_BRICKS, 38, Blocks.RED_NETHER_BRICKS, 22, Blocks.BLACKSTONE, 14, Blocks.CRACKED_NETHER_BRICKS, 14, Blocks.POLISHED_BLACKSTONE_BRICKS, 12);
    }

    private BlockState floor(int x, int y, int z) {
        if (arenaRegion(z) && z < ARENA_Z + ARENA_R + 2) {
            double d = arenaDist(x, z);
            double a = Carve.angle(x, z, 0, ARENA_Z);
            if (d >= 13.5 && d <= 17.5) {
                double m = Math.min(a % 90, 90 - a % 90);          // chaussees aux quatre points cardinaux
                if (m > 11) return d < 14.5 || d > 16.5 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.LAVA.defaultBlockState();
                return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            }
            if (d <= 13.5) {
                if (d <= 5.2) return d > 4.0 ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
                if (d >= 8.8 && d <= 9.8) return Blocks.GILDED_BLACKSTONE.defaultBlockState();
                if (a % 45 < 4 || a % 45 > 41) return Blocks.MAGMA_BLOCK.defaultBlockState();
                return c.mix(x, y, z, 6, Blocks.OBSIDIAN, 60, Blocks.POLISHED_BLACKSTONE, 40);
            }
        }
        return c.mix(x, y, z, 4, Blocks.POLISHED_BLACKSTONE_BRICKS, 45, Blocks.NETHER_BRICKS, 30, Blocks.BLACKSTONE, 15, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, 10);
    }

    @Override
    BlockState light(int x, int y, int z) {
        return (x + y + z) % 3 == 0 ? Blocks.SHROOMLIGHT.defaultBlockState() : Blocks.GLOWSTONE.defaultBlockState();
    }

    private void tower(int cx, int cz, int h) {
        c.wall(cx, cz, 2.6, 1, h, 1.2, (x, y, z) -> c.mix(x, y, z, 21, Blocks.NETHER_BRICKS, 60, Blocks.POLISHED_BLACKSTONE_BRICKS, 40));
        c.disc(cx, h, cz, 3.2, (x, y, z) -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = cx + (int) Math.round(Math.cos(a) * 3.1);
            int z = cz + (int) Math.round(Math.sin(a) * 3.1);
            c.set(x, h + 1, z, i % 2 == 0 ? Blocks.NETHER_BRICK_WALL : Blocks.NETHER_BRICK_FENCE);
            if (i % 2 == 0) c.set(x, h + 2, z, Blocks.LANTERN);
        }
    }

    @Override
    void hall() {
        Kit.ribs(c, 0, HALL_Z, HALL_R, HALL_R, HALL_H, 11, 8, Blocks.POLISHED_BLACKSTONE_BRICKS);
        Kit.band(c, 0, HALL_Z, HALL_R, 5, Blocks.GILDED_BLACKSTONE);
        Kit.colonnade(c, 0, HALL_Z, 9.0, 6, 0.0, 1, HALL_H, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_STAIRS);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            Kit.brazier(c, (int) Math.round(Math.cos(a) * 7.2), 1, HALL_Z + (int) Math.round(Math.sin(a) * 7.2), Blocks.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.CAMPFIRE);
        }
        Kit.chandelier(c, HALL_H + 7, 0, HALL_Z, 5, Blocks.LANTERN);
        // portes de fer a l'entree du couloir
        for (int y = 1; y <= 7; y++) {
            c.set(-3, y, 22, Blocks.POLISHED_BLACKSTONE_BRICKS);
            c.set(3, y, 22, Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
    }

    @Override
    void roomA() {
        double cz = A_Z;
        Kit.ribs(c, 0, cz, A_R, A_R, A_H, 9, 10, Blocks.POLISHED_BLACKSTONE_BRICKS);
        Kit.band(c, 0, cz, A_R, 4, Blocks.NETHER_BRICKS);
        // fosse de lave centrale avec ilot-brasero, chaussees en croix
        c.disc(0, 0, (int) cz, 7.0, (x, y, z) -> {
            double d = Math.hypot(x, z - cz);
            boolean cross = Math.abs(x) <= 1 || Math.abs(z - cz) <= 1;
            if (d <= 2.2 || cross) return c.state(Blocks.POLISHED_BLACKSTONE_BRICKS);
            return d > 6.0 ? c.state(Blocks.OBSIDIAN) : c.state(Blocks.LAVA);
        });
        c.set(0, 1, (int) cz, Blocks.NETHER_BRICK_FENCE);
        c.set(0, 2, (int) cz, Blocks.NETHER_BRICK_FENCE);
        c.set(0, 3, (int) cz, Blocks.CAMPFIRE);
        Kit.colonnade(c, 0, cz, 12.5, 8, Math.PI / 8, 1, A_H, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_STAIRS);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI * 2 * i / 4 + Math.PI / 4;
            Kit.chandelier(c, A_H + 6, (int) Math.round(Math.cos(a) * 9), (int) Math.round(cz + Math.sin(a) * 9), 4, Blocks.LANTERN);
        }
    }

    @Override
    void pit() {
        // lac de lave sous le pont, poutres de basalte, braseros sur les piles
        for (int z = PIT_Z0; z <= PIT_Z1; z++) {
            for (int x = -PIT_HALF; x <= PIT_HALF; x++) {
                if (Math.abs(x) <= DECK) continue;
                c.set(x, -PIT_DEPTH, z, Blocks.OBSIDIAN);
                c.set(x, -PIT_DEPTH + 1, z, Blocks.LAVA);
                c.set(x, -PIT_DEPTH + 2, z, Blocks.LAVA);
            }
            for (int x = -DECK; x <= DECK; x++) c.set(x, 0, z, c.mix(x, 0, z, 13, Blocks.POLISHED_BLACKSTONE_BRICKS, 60, Blocks.NETHER_BRICKS, 40));
        }
        for (int z = PIT_Z0; z <= PIT_Z1; z += 4) {
            for (int side : new int[]{-2, 2}) {
                c.set(side, 1, z, Blocks.NETHER_BRICK_WALL);
                c.set(side, 2, z, Blocks.NETHER_BRICK_WALL);
                c.set(side, 3, z, Blocks.LANTERN);
            }
            // contreforts sous le pont
            for (int y = -1; y >= -3; y--) {
                c.set(-1, y, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
                c.set(1, y, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
                c.set(0, y, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
            }
        }
        for (int z = PIT_Z0; z <= PIT_Z1; z++) {
            if (z % 4 == 0) continue;
            c.set(-2, 1, z, Blocks.NETHER_BRICK_FENCE);
            c.set(2, 1, z, Blocks.NETHER_BRICK_FENCE);
        }
    }

    @Override
    void roomB() {
        double cz = B_Z;
        Kit.ribs(c, 0, cz, B_R, B_R, B_H, 9, 10, Blocks.POLISHED_BLACKSTONE_BRICKS);
        // salle des forges : enclumes, fournaises et chaudrons de lave
        for (int[] p : new int[][]{{-9, -4}, {-9, 4}, {9, -4}, {9, 4}}) {
            c.fill(p[0] - 1, 1, (int) cz + p[1] - 1, p[0] + 1, 1, (int) cz + p[1] + 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
            c.set(p[0], 2, (int) cz + p[1], Blocks.LAVA_CAULDRON);
            c.set(p[0] + (p[0] > 0 ? -2 : 2), 1, (int) cz + p[1], Blocks.ANVIL);
        }
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = (int) Math.round(Math.cos(a) * 12.3);
            int z = (int) Math.round(cz + Math.sin(a) * 12.3);
            for (int y = 1; y <= B_H; y++) c.set(x, y, z, y % 4 == 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
        // grande chaine de forge et seaux de lave suspendus
        for (int[] p : new int[][]{{-4, 0}, {4, 0}}) {
            for (int y = B_H + 6; y > 6; y--) c.set(p[0], y, (int) cz + p[1], Blocks.IRON_CHAIN);
            c.set(p[0], 5, (int) cz + p[1], Blocks.LAVA_CAULDRON);
        }
        for (int side : new int[]{-1, 1}) {
            c.fill(side * 20, 1, (int) cz - 2, side * 22, 1, (int) cz + 2, Blocks.MAGMA_BLOCK);
            Kit.brazier(c, side * 16, 1, (int) cz - 3, Blocks.NETHER_BRICK_WALL, Blocks.CAMPFIRE);
            Kit.brazier(c, side * 16, 1, (int) cz + 3, Blocks.NETHER_BRICK_WALL, Blocks.CAMPFIRE);
        }
    }

    @Override
    void gallery() {
        for (int z = GAL_Z0; z <= GAL_Z1 - 2; z += 4) {
            for (int side : new int[]{-1, 1}) {
                for (int y = 1; y <= 7; y++) c.set(side * 3, y, z, y == 7 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS);
                c.set(side * 2, 1, z, Blocks.NETHER_BRICK_WALL);
                c.set(side * 2, 2, z, Blocks.CAMPFIRE);
            }
            c.fill(-3, 8, z, 3, 8, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
        for (int z = GAL_Z0; z <= GAL_Z1 - 2; z++) {
            for (int x = -1; x <= 1; x++) c.set(x, 0, z, x == 0 ? Blocks.GILDED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
    }

    @Override
    void arena() {
        double cz = ARENA_Z;
        // tours d'angle et remparts a creneaux
        for (int[] d : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) tower(d[0] * 15, (int) cz + d[1] * 15, 13);
        c.ring(0, 1, (int) cz, 18.4, 19.8, (x, y, z) -> {
            double a = Carve.angle(x, z, 0, cz);
            if (Math.abs(x) < 5) return null;
            return (int) (a / 6) % 2 == 0 ? c.state(Blocks.POLISHED_BLACKSTONE_BRICKS) : c.state(Blocks.NETHER_BRICKS);
        });
        for (int y = 2; y <= 3; y++) {
            final int fy = y;
            c.ring(0, fy, (int) cz, 18.4, 19.8, (x, yy, z) -> {
                double a = Carve.angle(x, z, 0, cz);
                if (Math.abs(x) < 5) return null;
                return ((int) (a / 6) % 2 == 0 && fy == 3) ? Blocks.AIR.defaultBlockState() : c.state(Blocks.NETHER_BRICKS);
            });
        }
        // trone d'Ignis : bloc de basalte, crocs de blackstone et chevelure de flammes
        int tz = (int) cz + 15;
        c.fill(-4, 1, tz, 4, 2, tz + 3, Blocks.POLISHED_BLACKSTONE_BRICKS);
        c.fill(-3, 3, tz + 1, 3, 4, tz + 3, Blocks.GILDED_BLACKSTONE);
        for (int y = 5; y <= 11; y++) {
            int w = y < 8 ? 3 : y < 10 ? 2 : 1;
            for (int x = -w; x <= w; x++) c.set(x, y, tz + 3, (x + y) % 3 == 0 ? Blocks.GILDED_BLACKSTONE : Blocks.BLACKSTONE);
        }
        for (int x = -4; x <= 4; x += 2) c.set(x, 12, tz + 3, Blocks.CAMPFIRE);
        c.set(0, 12, tz + 3, Blocks.CAMPFIRE);
        // braseros sur la plateforme
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 + Math.PI / 8;
            Kit.brazier(c, (int) Math.round(Math.cos(a) * 11), 1, (int) Math.round(cz + Math.sin(a) * 11), Blocks.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.CAMPFIRE);
        }
        Kit.ribs(c, 0, cz, ARENA_R, ARENA_R, ARENA_H, 15, 16, Blocks.POLISHED_BLACKSTONE_BRICKS);
        Kit.band(c, 0, cz, ARENA_R, 13, Blocks.GILDED_BLACKSTONE);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + 0.5;
            Kit.chandelier(c, ARENA_H + 9, (int) Math.round(Math.cos(a) * 9.5), (int) Math.round(cz + Math.sin(a) * 9.5), 6, Blocks.LANTERN);
        }
    }

    @Override
    void treasure() {
        double cz = TREASURE_Z;
        c.disc(0, 0, (int) cz, 4.0, (x, y, z) -> Math.hypot(x, z - cz) > 2.6 ? c.state(Blocks.MAGMA_BLOCK) : c.state(Blocks.GILDED_BLACKSTONE));
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            Kit.brazier(c, (int) Math.round(Math.cos(a) * 5.4), 1, (int) Math.round(cz + Math.sin(a) * 5.4), Blocks.NETHER_BRICK_WALL, Blocks.CAMPFIRE);
        }
        c.set(0, 1, (int) cz - 2, c.stair(Blocks.NETHER_BRICK_STAIRS, Direction.SOUTH, false));
    }

    @Override
    Trials.Palette palette() {
        return new Trials.Palette(Blocks.NETHER_BRICK_FENCE, Blocks.GLOWSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.BLACKSTONE, Blocks.LANTERN,
                java.util.List.of("elementaire_feu", "orc_guerrier"), java.util.Map.of(TrialData.WAVES, "FOURNAISE DES DÉMONS", TrialData.JUMP, "TRAVERSÉE DE LAVE", TrialData.SIMON, "ÉNIGME DES FORGES"));
    }

    @Override
    Trials.Plan plan() {
        return new Trials.Plan(TrialData.WAVES, TrialData.JUMP, TrialData.SIMON, 1, 1, 2);
    }

    @Override
    String arenaName() {
        return "Trône du Seigneur Ignis";
    }
}
