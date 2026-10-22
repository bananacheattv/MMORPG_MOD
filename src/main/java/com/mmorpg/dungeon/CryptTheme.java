package com.mmorpg.dungeon;

import com.mmorpg.hub.HubCtx;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Catacombes de la Liche : une cathedrale funeraire d'ardoise noire, a nervures d'os, ossuaires, sarcophages et cierges
 * violets ; l'arene est un cercle rituel de feu d'ames sous une coupole d'ossements.
 */
final class CryptTheme extends Theme {
    private static final Object BONE = Blocks.BONE_BLOCK;

    CryptTheme(HubCtx c, DungeonType t) {
        super(c, t);
    }

    @Override
    BlockState shell(int x, int y, int z, Carve.Kind kind, boolean back) {
        if (back) return c.mix(x, y, z, 1, Blocks.DEEPSLATE, 50, Blocks.COBBLED_DEEPSLATE, 30, Blocks.TUFF, 20);
        return switch (kind) {
            case FLOOR -> floor(x, y, z);
            case CEILING -> c.mix(x, y, z, 3, Blocks.DEEPSLATE_BRICKS, 45, Blocks.DEEPSLATE_TILES, 30, Blocks.CRACKED_DEEPSLATE_BRICKS, 15, Blocks.CHISELED_DEEPSLATE, 10);
            case WALL -> wall(x, y, z);
        };
    }

    private BlockState wall(int x, int y, int z) {
        // ossuaires : deux frises d'os et de crane sculpte le long des murs
        if (y == 4 || y == 8) return c.mix(x, y, z, 5, Blocks.BONE_BLOCK, 55, Blocks.CHISELED_DEEPSLATE, 25, Blocks.DEEPSLATE_BRICKS, 20);
        if (y == 5 || y == 9) return c.mix(x, y, z, 6, Blocks.DEEPSLATE_BRICKS, 60, Blocks.POLISHED_DEEPSLATE, 40);
        return c.mix(x, y, z, 2, Blocks.DEEPSLATE_BRICKS, 40, Blocks.DEEPSLATE_TILES, 25, Blocks.CRACKED_DEEPSLATE_BRICKS, 20, Blocks.POLISHED_DEEPSLATE, 15);
    }

    private BlockState floor(int x, int y, int z) {
        if (arenaRegion(z) && z < ARENA_Z + ARENA_R + 2) {
            double d = arenaDist(x, z);
            double a = Carve.angle(x, z, 0, ARENA_Z);
            if (d <= 4.6) return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            if (d >= 8.6 && d <= 9.6) return Blocks.AMETHYST_BLOCK.defaultBlockState();
            if (d >= 14.0 && d <= 14.9) return Blocks.AMETHYST_BLOCK.defaultBlockState();
            if (d > 4.6 && d < 14.0 && (a % 40 < 3.2 || a % 40 > 36.8)) return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        // damier ardoise / pierre noire
        return ((x + z) & 1) == 0
                ? c.mix(x, y, z, 4, Blocks.POLISHED_DEEPSLATE, 70, Blocks.DEEPSLATE_TILES, 30)
                : c.mix(x, y, z, 7, Blocks.DEEPSLATE_TILES, 55, Blocks.BLACKSTONE, 25, Blocks.POLISHED_BLACKSTONE, 20);
    }

    @Override
    BlockState light(int x, int y, int z) {
        return Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
    }

    @Override
    float lightChance() {
        return 0.055F;
    }

    // ------------------------------------------------------------------ decor

    private void candleCluster(int x, int y, int z, int salt) {
        c.set(x, y, z, Kit.candles(Blocks.DYED_CANDLE.pick(DyeColor.PURPLE), 2 + (salt & 1) * 2));
    }

    private void sarcophagus(int x, int z, boolean alongZ) {
        for (int i = -2; i <= 2; i++) {
            int px = alongZ ? x : x + i;
            int pz = alongZ ? z + i : z;
            c.set(px, 1, pz, Blocks.POLISHED_DEEPSLATE);
            c.set(px, 2, pz, Blocks.DEEPSLATE_TILE_SLAB);
        }
        c.set(alongZ ? x : x + 3, 1, alongZ ? z + 3 : z, c.stair(Blocks.POLISHED_DEEPSLATE_STAIRS, alongZ ? Direction.NORTH : Direction.WEST, false));
        c.set(alongZ ? x : x - 3, 1, alongZ ? z - 3 : z, c.stair(Blocks.POLISHED_DEEPSLATE_STAIRS, alongZ ? Direction.SOUTH : Direction.EAST, false));
        candleCluster(alongZ ? x : x + 1, 3, alongZ ? z + 1 : z, x + z);
    }

    private void spire(int x, int z, int h) {
        for (int y = 1; y <= h; y++) c.set(x, y, z, y == h ? Blocks.SOUL_LANTERN : y % 5 == 0 ? Blocks.CHISELED_DEEPSLATE : Blocks.DEEPSLATE_BRICK_WALL);
    }

    @Override
    void hall() {
        Kit.ribs(c, 0, HALL_Z, HALL_R, HALL_R, HALL_H, 11, 10, Blocks.POLISHED_DEEPSLATE);
        Kit.band(c, 0, HALL_Z, HALL_R, 7, Blocks.DEEPSLATE_TILES);
        Kit.colonnade(c, 0, HALL_Z, 9.2, 8, Math.PI / 8, 1, HALL_H, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICK_STAIRS);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 + Math.PI / 8;
            candleCluster((int) Math.round(Math.cos(a) * 7.4), 1, HALL_Z + (int) Math.round(Math.sin(a) * 7.4), i);
        }
        sarcophagus(-8, HALL_Z + 2, true);
        sarcophagus(8, HALL_Z + 2, true);
        Kit.chandelier(c, HALL_H + 7, 0, HALL_Z, 5, Blocks.SOUL_LANTERN);
    }

    @Override
    void roomA(Layout.Room rm) {
        double cz = rm.z();
        Kit.ribs(c, 0, cz, rm.r(), rm.r(), rm.h(), 9, 12, Blocks.POLISHED_DEEPSLATE);
        Kit.band(c, 0, cz, rm.r(), 3, Blocks.CHISELED_DEEPSLATE);
        Kit.colonnade(c, 0, cz, 12.5, 6, 0.0, 1, rm.h(), Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICK_STAIRS);
        // autel d'ossements : estrade ronde, piliers d'os et crane central
        Kit.rings(c, 0, 1, (int) cz, 5, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_BLACKSTONE_BRICKS);
        c.set(0, 2, (int) cz, Blocks.BONE_BLOCK);
        c.set(0, 3, (int) cz, Blocks.BONE_BLOCK);
        c.set(0, 4, (int) cz, Kit.skull(Blocks.WITHER_SKELETON_SKULL, 0));
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            int x = (int) Math.round(Math.cos(a) * 3.4);
            int z = (int) Math.round(cz + Math.sin(a) * 3.4);
            c.set(x, 2, z, Blocks.BONE_BLOCK);
            candleCluster(x, 3, z, i);
        }
        sarcophagus(-11, (int) cz + 3, true);
        sarcophagus(11, (int) cz - 3, true);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI * 2 * i / 4 + 0.6;
            Kit.chandelier(c, rm.h() + 6, (int) Math.round(Math.cos(a) * 8), (int) Math.round(cz + Math.sin(a) * 8), 3, Blocks.SOUL_LANTERN);
        }
    }

    @Override
    void pit(int z0, int z1) {
        for (int z = z0; z <= z1; z++) {
            for (int x = -PIT_HALF; x <= PIT_HALF; x++) {
                if (Math.abs(x) <= DECK) continue;
                c.set(x, -PIT_DEPTH, z, Blocks.SOUL_SOIL);
                if (c.rnd(x, 0, z, 11) < 0.7F) c.set(x, -PIT_DEPTH + 1, z, Blocks.SOUL_FIRE);
                if (c.rnd(x, 1, z, 12) < 0.12F) c.set(x, -PIT_DEPTH + 2, z, Blocks.COBWEB);
            }
            for (int x = -DECK; x <= DECK; x++) c.set(x, 0, z, c.mix(x, 0, z, 13, Blocks.POLISHED_DEEPSLATE, 50, Blocks.DEEPSLATE_TILES, 30, Blocks.CRACKED_DEEPSLATE_BRICKS, 20));
        }
        for (int z = z0 + 1; z <= z1 - 1; z += 4) {
            for (int side : new int[]{-2, 2}) {
                c.set(side, 1, z, Blocks.DEEPSLATE_BRICK_WALL);
                c.set(side, 2, z, Blocks.DEEPSLATE_BRICK_WALL);
                c.set(side, 3, z, Blocks.SOUL_LANTERN);
            }
        }
        // arches de l'ossuaire au-dessus du pont
        for (int z = z0 + 3; z <= z1 - 3; z += 6) Kit.doorway(c, 0, z, 2, 5, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICK_STAIRS);
    }

    @Override
    void roomB(Layout.Room rm) {
        double cz = rm.z();
        Kit.ribs(c, 0, cz, rm.r(), rm.r(), rm.h(), 9, 12, Blocks.POLISHED_DEEPSLATE);
        // chapelle : rangees de bancs de part et d'autre d'une allee, autel au fond
        for (int row = 0; row < 4; row++) {
            int z = (int) cz - 7 + row * 3;
            for (int side : new int[]{-1, 1}) {
                for (int x = 2; x <= 6; x++) {
                    c.set(side * x, 1, z, c.stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
                }
            }
        }
        for (int z = (int) cz - 8; z <= (int) cz + 6; z++) {
            c.set(0, 0, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
            c.set(-1, 0, z, Blocks.DEEPSLATE_TILES);
            c.set(1, 0, z, Blocks.DEEPSLATE_TILES);
        }
        // croix d'ossements et d'amethyste derriere l'autel
        int az = (int) cz + 8;
        c.fill(-2, 1, az, 2, 1, az, Blocks.POLISHED_DEEPSLATE);
        for (int y = 2; y <= 7; y++) c.set(0, y, az, y == 5 ? Blocks.AMETHYST_BLOCK : Blocks.BONE_BLOCK);
        c.fill(-2, 5, az, 2, 5, az, Blocks.AMETHYST_BLOCK);
        c.set(0, 8, az, Blocks.SOUL_LANTERN);
        for (int[] p : new int[][]{{-3, 1}, {3, 1}}) candleCluster(p[0], 2, az + p[1], p[0]);
        // alcoves : sarcophages
        sarcophagus(-21, (int) cz, false);
        sarcophagus(21, (int) cz, false);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI * 2 * i / 4 + 0.78;
            Kit.chandelier(c, rm.h() + 6, (int) Math.round(Math.cos(a) * 8), (int) Math.round(cz + Math.sin(a) * 8), 4, Blocks.SOUL_LANTERN);
        }
    }

    @Override
    void gallery() {
        for (int z = GAL_Z0; z <= GAL_Z1 - 2; z++) {
            for (int x = -1; x <= 1; x++) c.set(x, 1, z, Blocks.CARPET.pick(x == 0 ? DyeColor.PURPLE : DyeColor.BLACK));
        }
        int k = 0;
        for (int z = GAL_Z0 + 2; z <= GAL_Z1 - 4; z += 6) {
            Kit.statue(c, -3, 1, z, Direction.EAST, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICK_WALL,
                    Kit.skull(Blocks.SKELETON_SKULL, 4), Blocks.DEEPSLATE_BRICK_STAIRS);
            Kit.statue(c, 3, 1, z + 3, Direction.WEST, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICK_WALL,
                    Kit.skull(Blocks.SKELETON_SKULL, 12), Blocks.DEEPSLATE_BRICK_STAIRS);
            candleCluster(-2, 1, z + 3, k++);
            candleCluster(2, 1, z, k++);
        }
        for (int z = GAL_Z0 + 3; z <= GAL_Z1 - 3; z += 6) Kit.doorway(c, 0, z, 3, 7, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICK_STAIRS);
    }

    @Override
    void arena() {
        double cz = ARENA_Z;
        // cercle rituel : estrade centrale a degres, anneaux d'amethyste et neuf piliers de cierges (l'autel garde son espace)
        c.disc(0, 1, (int) cz, 5.4, (x, y, z) -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        c.disc(0, 1, (int) cz, 3.2, (x, y, z) -> Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        c.set(0, 1, (int) cz, Blocks.AIR);
        for (int i = 0; i < 9; i++) {
            double a = Math.PI * 2 * i / 9;
            int x = (int) Math.round(Math.cos(a) * 11.8);
            int z = (int) Math.round(cz + Math.sin(a) * 11.8);
            for (int y = 1; y <= 4; y++) c.set(x, y, z, y == 4 ? Blocks.CHISELED_DEEPSLATE : Blocks.DEEPSLATE_BRICK_WALL);
            candleCluster(x, 5, z, i);
            c.set(x, 6, z, Blocks.SOUL_LANTERN);
        }
        // braseros de feu d'ames
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 + Math.PI / 8;
            Kit.brazier(c, (int) Math.round(Math.cos(a) * 16.5), 1, (int) Math.round(cz + Math.sin(a) * 16.5), Blocks.DEEPSLATE_BRICK_WALL, Blocks.SOUL_CAMPFIRE);
        }
        // trone de la liche : dossier gothique en ogive, amethyste et cranes
        int tz = (int) cz + 16;
        c.fill(-3, 1, tz, 3, 1, tz + 3, Blocks.POLISHED_BLACKSTONE_BRICKS);
        c.fill(-2, 2, tz + 1, 2, 2, tz + 3, Blocks.POLISHED_DEEPSLATE);
        for (int y = 3; y <= 9; y++) {
            int w = y < 6 ? 2 : y < 8 ? 1 : 0;
            for (int x = -w; x <= w; x++) c.set(x, y, tz + 3, y == 5 && x == 0 ? Blocks.AMETHYST_BLOCK : Blocks.DEEPSLATE_BRICKS);
        }
        c.set(0, 10, tz + 3, Kit.skull(Blocks.WITHER_SKELETON_SKULL, 8));
        for (int side : new int[]{-3, 3}) {
            for (int y = 2; y <= 6; y++) c.set(side, y, tz + 3, Blocks.DEEPSLATE_BRICK_WALL);
            c.set(side, 7, tz + 3, Blocks.SOUL_LANTERN);
        }
        // coupole d'ossements et lustres d'ames
        Kit.ribs(c, 0, cz, ARENA_R, ARENA_R, ARENA_H, 15, 18, Blocks.BONE_BLOCK);
        Kit.band(c, 0, cz, ARENA_R, 9, Blocks.POLISHED_DEEPSLATE);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + 0.5;
            Kit.chandelier(c, ARENA_H + 9, (int) Math.round(Math.cos(a) * 8.5), (int) Math.round(cz + Math.sin(a) * 8.5), 6, Blocks.SOUL_LANTERN);
        }
    }

    @Override
    void treasure() {
        double cz = TREASURE_Z;
        for (int i = 0; i < 7; i++) {
            double a = Math.PI * 2 * i / 7;
            int x = (int) Math.round(Math.cos(a) * 4.5);
            int z = (int) Math.round(cz + Math.sin(a) * 4.5);
            for (int y = 1; y <= 3; y++) c.set(x, y, z, y == 3 ? Blocks.AMETHYST_CLUSTER : Blocks.AMETHYST_BLOCK);
        }
        sarcophagus(0, (int) cz - 1, false);
        spire(-3, (int) cz + 3, 4);
        spire(3, (int) cz + 3, 4);
    }

    @Override
    Trials.Palette palette() {
        return new Trials.Palette(Blocks.IRON_BARS, Blocks.PEARLESCENT_FROGLIGHT, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.SOUL_LANTERN,
                java.util.List.of("squelette_maudit", "orc_guerrier"), java.util.Map.of(TrialData.WAVES, "RÉVEIL DES TOMBES", TrialData.JUMP, "PONT D'OSSEMENTS", TrialData.SIMON, "ÉNIGME DES CIERGES", TrialData.ORDER, "SCEAUX DES DÉFUNTS", TrialData.TARGETS, "TIR AUX CRÂNES", TrialData.ELITE, "ROI DES CRYPTES", TrialData.AMBUSH, "RÔDEURS DES TOMBES"), Blocks.SOUL_SAND, Blocks.SOUL_LANTERN);
    }

    @Override
    Trials.Plan plan() {
        // salle 1, couloir 1, salle 2, salle 3, couloir 2, salle 4, salle 5, gardien d'elite
        return Trials.plan(TrialData.SIMON, 1, TrialData.JUMP, 0, TrialData.WAVES, 1, TrialData.ORDER, 1, TrialData.JUMP, 1, TrialData.TARGETS, 1, TrialData.WAVES, 1, TrialData.ELITE, 1);
    }

    @Override
    String eliteKey() {
        return "roi_cryptes";
    }

    @Override
    Look look() {
        return new Look(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.SOUL_LANTERN, Blocks.SOUL_CAMPFIRE, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE, Blocks.CHISELED_DEEPSLATE);
    }

    @Override
    String lore() {
        return "Les morts murmurent dans le noir. La Liche veille sur ses reliques.";
    }

    @Override
    String arenaName() {
        return "Sanctum de la Liche Ancienne";
    }
}
