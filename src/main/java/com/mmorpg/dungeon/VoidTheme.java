package com.mmorpg.dungeon;

import com.mmorpg.hub.HubCtx;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Citadelle du Neant : obsidienne, pierre d'End et purpur sous un ciel d'etoiles ; des obelisques de purpur flottent autour d'une
 * estrade a runes, et la voute etoilee de l'arene se reflete sur un sol de cristaux pleurants.
 */
final class VoidTheme extends Theme {
    VoidTheme(HubCtx c, DungeonType t) {
        super(c, t);
    }

    @Override
    BlockState shell(int x, int y, int z, Carve.Kind kind, boolean back) {
        if (back) return c.mix(x, y, z, 1, Blocks.OBSIDIAN, 55, Blocks.END_STONE, 30, Blocks.BLACKSTONE, 15);
        return switch (kind) {
            case FLOOR -> floor(x, y, z);
            case CEILING -> {
                // voute etoilee : noir profond piquete d'etoiles
                float r = c.rnd(x, y, z, 31);
                yield r < 0.025F ? Blocks.END_ROD.defaultBlockState().setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, Direction.DOWN)
                        : c.mix(x, y, z, 3, Blocks.CONCRETE.pick(DyeColor.BLACK), 55, Blocks.OBSIDIAN, 30, Blocks.CRYING_OBSIDIAN, 8, Blocks.PURPUR_BLOCK, 7);
            }
            case WALL -> wall(x, y, z);
        };
    }

    private BlockState wall(int x, int y, int z) {
        if (y == 4 || y == 9) return c.mix(x, y, z, 5, Blocks.PURPUR_PILLAR, 55, Blocks.END_STONE_BRICKS, 25, Blocks.CRYING_OBSIDIAN, 20);
        return c.mix(x, y, z, 2, Blocks.OBSIDIAN, 38, Blocks.END_STONE_BRICKS, 26, Blocks.PURPUR_BLOCK, 14, Blocks.CONCRETE.pick(DyeColor.BLACK), 14, Blocks.CRYING_OBSIDIAN, 8);
    }

    private BlockState floor(int x, int y, int z) {
        if (arenaRegion(z) && z < ARENA_Z + ARENA_R + 2) {
            double d = arenaDist(x, z);
            double a = Carve.angle(x, z, 0, ARENA_Z);
            if (d <= 5.2) return d > 4.0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : c.state("dalle_runique");
            if (d >= 8.6 && d <= 9.4) return c.state("cristal_arcane");
            if (d >= 14.0 && d <= 14.8) return Blocks.PURPUR_PILLAR.defaultBlockState();
            double m = Math.min(a % 72, 72 - a % 72);                  // pentagramme de runes
            if (d > 5.2 && d < 14.0 && m < 2.6) return c.state("dalle_runique");
            return c.mix(x, y, z, 6, Blocks.OBSIDIAN, 60, Blocks.CRYING_OBSIDIAN, 12, Blocks.CONCRETE.pick(DyeColor.BLACK), 28);
        }
        return c.mix(x, y, z, 4, Blocks.OBSIDIAN, 45, Blocks.END_STONE_BRICKS, 30, Blocks.PURPUR_BLOCK, 10, Blocks.CRYING_OBSIDIAN, 15);
    }

    @Override
    BlockState light(int x, int y, int z) {
        return c.state("cristal_arcane");
    }

    @Override
    float lightChance() {
        return 0.035F;
    }

    // ------------------------------------------------------------------ decor

    private void obelisk(int x, int z, int y0, int h) {
        for (int y = y0; y < y0 + h; y++) c.set(x, y, z, y == y0 + h - 1 ? Blocks.CRYING_OBSIDIAN : Blocks.PURPUR_PILLAR);
        c.set(x, y0 + h, z, Blocks.END_ROD);
    }

    /** Obelisque flottant : fragments de purpur separes par du vide, relies par une colonne d'etoiles. */
    private void floating(int x, int z, int y0) {
        c.set(x, y0, z, Blocks.CRYING_OBSIDIAN);
        c.set(x, y0 + 1, z, Blocks.PURPUR_PILLAR);
        c.set(x, y0 + 2, z, Blocks.PURPUR_PILLAR);
        c.set(x, y0 + 4, z, Blocks.PURPUR_PILLAR);
        c.set(x, y0 + 5, z, Blocks.END_ROD);
        c.set(x, y0 + 7, z, Blocks.AMETHYST_BLOCK);
        for (Direction d : Direction.Plane.HORIZONTAL) c.set(x + d.getStepX(), y0 + 1, z + d.getStepZ(), c.stair(Blocks.PURPUR_STAIRS, d, false));
    }

    @Override
    void hall() {
        Kit.ribs(c, 0, HALL_Z, HALL_R, HALL_R, HALL_H, 11, 8, Blocks.PURPUR_PILLAR);
        Kit.band(c, 0, HALL_Z, HALL_R, 5, Blocks.CRYING_OBSIDIAN);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 + Math.PI / 8;
            obelisk((int) Math.round(Math.cos(a) * 9), HALL_Z + (int) Math.round(Math.sin(a) * 9), 1, 6 + (i % 2) * 3);
        }
        for (int i = 0; i < 4; i++) floating((int) Math.round(Math.cos(i * Math.PI / 2 + 0.78) * 5.8), HALL_Z + (int) Math.round(Math.sin(i * Math.PI / 2 + 0.78) * 5.8), 4);
        Kit.chandelier(c, HALL_H + 8, 0, HALL_Z, 6, Blocks.SOUL_LANTERN);
    }

    @Override
    void roomA() {
        double cz = A_Z;
        Kit.ribs(c, 0, cz, A_R, A_R, A_H, 9, 12, Blocks.PURPUR_PILLAR);
        // anneaux concentriques d'obsidienne et de cristaux pleurants autour d'une pierre de runes
        c.ring(0, 0, (int) cz, 2.5, 3.5, (x, y, z) -> c.state("cristal_arcane"));
        c.ring(0, 0, (int) cz, 6.5, 7.5, (x, y, z) -> c.state("dalle_runique"));
        c.disc(0, 0, (int) cz, 2.5, (x, y, z) -> c.state(Blocks.CRYING_OBSIDIAN));
        obelisk(0, (int) cz, 1, 7);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            floating((int) Math.round(Math.cos(a) * 10.5), (int) Math.round(cz + Math.sin(a) * 10.5), 2 + (i % 3) * 2);
        }
        Kit.colonnade(c, 0, cz, 13.2, 8, 0.4, 1, A_H, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.PURPUR_STAIRS);
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * 2 * i / 3 + 0.5;
            Kit.chandelier(c, A_H + 7, (int) Math.round(Math.cos(a) * 8), (int) Math.round(cz + Math.sin(a) * 8), 4, Blocks.SOUL_LANTERN);
        }
    }

    @Override
    void pit() {
        // abime du neant : le sol du fosse est un champ d'etoiles ; le pont lui-meme est de purpur et de cristal
        for (int z = PIT_Z0; z <= PIT_Z1; z++) {
            for (int x = -PIT_HALF; x <= PIT_HALF; x++) {
                if (Math.abs(x) <= 1) continue;
                c.set(x, -PIT_DEPTH, z, Blocks.CONCRETE.pick(DyeColor.BLACK));
                if (c.rnd(x, 0, z, 9) < 0.2F) c.set(x, -PIT_DEPTH, z, Blocks.END_ROD.defaultBlockState().setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, Direction.UP));
                if (c.rnd(x, 1, z, 10) < 0.1F) c.set(x, -PIT_DEPTH + 1, z, Blocks.AMETHYST_CLUSTER);
            }
            for (int x = -1; x <= 1; x++) c.set(x, 0, z, x == 0 ? c.state("dalle_runique") : c.state(Blocks.PURPUR_PILLAR));
        }
        for (int z = PIT_Z0; z <= PIT_Z1; z += 4) {
            for (int side : new int[]{-2, 2}) {
                c.set(side, 1, z, Blocks.PURPUR_PILLAR);
                c.set(side, 2, z, Blocks.PURPUR_PILLAR);
                c.set(side, 3, z, Blocks.END_ROD);
            }
        }
    }

    @Override
    void roomB() {
        double cz = B_Z;
        Kit.ribs(c, 0, cz, B_R, B_R, B_H, 9, 12, Blocks.PURPUR_PILLAR);
        // sphere d'etoiles : anneaux d'obelisques flottants sur trois hauteurs autour d'un cristal
        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + ring * 0.5;
                floating((int) Math.round(Math.cos(a) * (6 + ring * 3)), (int) Math.round(cz + Math.sin(a) * (6 + ring * 3)), 1 + ring * 2);
            }
        }
        c.disc(0, 0, (int) cz, 3.5, (x, y, z) -> c.state("dalle_runique"));
        for (int y = 1; y <= 4; y++) c.set(0, y, (int) cz, y == 4 ? Blocks.AMETHYST_BLOCK : Blocks.CRYING_OBSIDIAN);
        c.set(0, 5, (int) cz, Blocks.END_ROD);
        for (int side : new int[]{-1, 1}) {
            for (int i = -3; i <= 3; i += 2) obelisk(side * 21, (int) cz + i, 1, 3 + Math.abs(i));
        }
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * 2 * i / 3;
            Kit.chandelier(c, B_H + 7, (int) Math.round(Math.cos(a) * 8), (int) Math.round(cz + Math.sin(a) * 8), 4, Blocks.SOUL_LANTERN);
        }
    }

    @Override
    void gallery() {
        for (int z = GAL_Z0; z <= GAL_Z1 - 2; z += 4) {
            for (int side : new int[]{-1, 1}) {
                obelisk(side * 3, z, 1, 5);
                c.set(side * 2, 1, z + 2, Blocks.AMETHYST_CLUSTER);
            }
            c.fill(-3, 8, z, 3, 8, z, Blocks.PURPUR_PILLAR);
        }
        for (int z = GAL_Z0; z <= GAL_Z1 - 2; z++) {
            for (int x = -1; x <= 1; x++) c.set(x, 0, z, x == 0 ? c.state("dalle_runique") : c.state(Blocks.CRYING_OBSIDIAN));
        }
    }

    @Override
    void arena() {
        double cz = ARENA_Z;
        // estrade a runes ; l'autel garde son espace libre
        c.disc(0, 1, (int) cz, 5.4, (x, y, z) -> Math.hypot(x, z - cz) > 4.2 ? c.state(Blocks.PURPUR_BLOCK) : c.state(Blocks.OBSIDIAN));
        c.set(0, 1, (int) cz, Blocks.AIR);
        // sept obelisques flottants autour de l'estrade
        for (int i = 0; i < 7; i++) {
            double a = Math.PI * 2 * i / 7;
            floating((int) Math.round(Math.cos(a) * 11.5), (int) Math.round(cz + Math.sin(a) * 11.5), 3 + (i % 3) * 2);
        }
        // piliers du pourtour, chacun couronne d'une etoile
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12 + Math.PI / 12;
            int x = (int) Math.round(Math.cos(a) * 18);
            int z = (int) Math.round(cz + Math.sin(a) * 18);
            if (z > cz + 12 && Math.abs(x) < 5) continue;
            Kit.column(c, x, z, 1, 14, Blocks.CRYING_OBSIDIAN, Blocks.PURPUR_PILLAR, Blocks.PURPUR_STAIRS);
            c.set(x, 15, z, Blocks.END_ROD);
        }
        // trone du neant : arche d'obsidienne enchassee de cristaux, deux serres de purpur
        int tz = (int) cz + 15;
        c.fill(-4, 1, tz, 4, 1, tz + 3, Blocks.OBSIDIAN);
        c.fill(-3, 2, tz + 1, 3, 2, tz + 3, Blocks.PURPUR_BLOCK);
        for (int y = 3; y <= 14; y++) {
            for (int x : new int[]{-4, -3, 3, 4}) c.set(x, y, tz + 3, y % 4 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN);
        }
        for (int x = -4; x <= 4; x++) c.set(x, 15, tz + 3, x == 0 ? c.state("cristal_arcane") : c.state(Blocks.PURPUR_PILLAR));
        for (int x = -2; x <= 2; x++) c.set(x, 16, tz + 3, c.state(Blocks.OBSIDIAN));
        c.set(0, 17, tz + 3, Blocks.END_ROD);
        for (int y = 3; y <= 6; y++) {
            c.set(-1, y, tz + 3, Blocks.OBSIDIAN);
            c.set(1, y, tz + 3, Blocks.OBSIDIAN);
        }
        c.set(0, 6, tz + 3, c.state("cristal_arcane"));
        Kit.ribs(c, 0, cz, ARENA_R, ARENA_R, ARENA_H, 15, 16, Blocks.PURPUR_PILLAR);
        Kit.band(c, 0, cz, ARENA_R, 10, Blocks.CRYING_OBSIDIAN);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + 0.5;
            Kit.chandelier(c, ARENA_H + 9, (int) Math.round(Math.cos(a) * 9.5), (int) Math.round(cz + Math.sin(a) * 9.5), 6, Blocks.SOUL_LANTERN);
        }
    }

    @Override
    void treasure() {
        double cz = TREASURE_Z;
        c.ring(0, 0, (int) cz, 2.8, 4.2, (x, y, z) -> c.state("cristal_arcane"));
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            obelisk((int) Math.round(Math.cos(a) * 5.4), (int) Math.round(cz + Math.sin(a) * 5.4), 1, 3 + i % 2);
        }
        for (int[] p : new int[][]{{-2, 3}, {2, 3}}) c.set(p[0], 1, (int) cz + p[1], Blocks.AMETHYST_CLUSTER);
    }

    @Override
    String arenaName() {
        return "Abîme de l'Avatar du Néant";
    }
}
