package com.mmorpg.dungeon;

import com.mmorpg.hub.HubCtx;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sanctuaire Glace : une cathedrale de glace et de calcite aux cascades gelees, stalactites et aiguilles de cristal azur ;
 * l'arene est un lac gele orne d'un flocon de glace bleue, au pied du trone du Titan.
 */
final class FrostTheme extends Theme {
    FrostTheme(HubCtx c, DungeonType t) {
        super(c, t);
    }

    @Override
    BlockState shell(int x, int y, int z, Carve.Kind kind, boolean back) {
        if (back) return c.mix(x, y, z, 1, Blocks.PACKED_ICE, 40, "marbre_poli", 35, Blocks.STONE, 25);
        return switch (kind) {
            case FLOOR -> floor(x, y, z);
            case CEILING -> c.mix(x, y, z, 3, Blocks.PACKED_ICE, 40, Blocks.BLUE_ICE, 25, "marbre_poli", 20, "tuile_givre", 15);
            case WALL -> wall(x, y, z);
        };
    }

    private BlockState wall(int x, int y, int z) {
        // cascades gelees : bandes verticales de glace bleue
        if ((Math.abs(x) * 7 + z * 3) % 11 < 2) return Blocks.BLUE_ICE.defaultBlockState();
        return c.mix(x, y, z, 2, Blocks.PACKED_ICE, 34, "marbre_poli", 24, "tuile_givre", 20, Blocks.BLUE_ICE, 14, Blocks.CONCRETE.pick(DyeColor.WHITE), 8);
    }

    private BlockState floor(int x, int y, int z) {
        if (arenaRegion(z) && z < ARENA_Z + ARENA_R + 2) {
            double d = arenaDist(x, z);
            double a = Carve.angle(x, z, 0, ARENA_Z);
            if (d <= 4.4) return d > 3.2 ? Blocks.BLUE_ICE.defaultBlockState() : c.state("tuile_givre");
            // flocon : six rayons de glace bleue, anneaux fins
            double m = Math.min(a % 60, 60 - a % 60);
            if (d < 17 && m < 3.0) return Blocks.BLUE_ICE.defaultBlockState();
            if (d >= 12.5 && d <= 13.3) return Blocks.BLUE_ICE.defaultBlockState();
            if (d >= 8.0 && d <= 8.8) return c.state("tuile_givre");
            return c.mix(x, y, z, 6, Blocks.ICE, 45, Blocks.PACKED_ICE, 35, Blocks.BLUE_ICE, 20);
        }
        return c.mix(x, y, z, 4, Blocks.PACKED_ICE, 40, "marbre_poli", 25, Blocks.ICE, 20, Blocks.BLUE_ICE, 15);
    }

    @Override
    BlockState light(int x, int y, int z) {
        return Blocks.SEA_LANTERN.defaultBlockState();
    }

    @Override
    float lightChance() {
        return 0.03F;
    }

    // ------------------------------------------------------------------ decor

    /** Aiguille de cristal : colonne qui s'affine vers le haut. */
    private void crystal(int x, int z, int h) {
        for (int y = 1; y <= h; y++) {
            c.set(x, y, z, "cristal_azur");
            if (y <= h / 2) {
                c.set(x + 1, y, z, y > 1 ? c.state("cristal_azur") : c.state(Blocks.BLUE_ICE));
                c.set(x - 1, y, z, y > 1 ? c.state("cristal_azur") : c.state(Blocks.BLUE_ICE));
            }
            if (y <= h / 3) {
                c.set(x, y, z + 1, "cristal_azur");
                c.set(x, y, z - 1, "cristal_azur");
            }
        }
    }

    /** Stalactite : cone de glace qui pend du plafond. */
    private void stalactite(int x, int topY, int z, int len) {
        for (int i = 0; i < len; i++) {
            if (c.air(x, topY - i, z)) c.set(x, topY - i, z, i < len - 1 ? Blocks.PACKED_ICE : Blocks.BLUE_ICE);
        }
    }

    private void icicles(double cx, double cz, double r, int topY, int count, int salt) {
        for (int i = 0; i < count; i++) {
            double a = c.rnd(i, salt, 3, 5) * Math.PI * 2;
            double rr = c.rnd(i, salt, 4, 6) * r;
            stalactite((int) Math.round(cx + Math.cos(a) * rr), topY, (int) Math.round(cz + Math.sin(a) * rr), 2 + (int) (c.rnd(i, salt, 5, 7) * 4));
        }
    }

    @Override
    void hall() {
        Kit.ribs(c, 0, HALL_Z, HALL_R, HALL_R, HALL_H, 11, 12, Blocks.BLUE_ICE);
        Kit.band(c, 0, HALL_Z, HALL_R, 6, "tuile_givre");
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + 0.3;
            crystal((int) Math.round(Math.cos(a) * 8.6), HALL_Z + (int) Math.round(Math.sin(a) * 8.6), 5 + i % 3);
        }
        icicles(0, HALL_Z, 9, HALL_H + 5, 26, 1);
        Kit.chandelier(c, HALL_H + 8, 0, HALL_Z, 6, Blocks.SOUL_LANTERN);
    }

    @Override
    void roomA() {
        double cz = A_Z;
        Kit.ribs(c, 0, cz, A_R, A_R, A_H, 9, 12, Blocks.BLUE_ICE);
        // bassin gele central, tertres de neige et aiguilles
        c.disc(0, 0, (int) cz, 6.0, (x, y, z) -> Math.hypot(x, z - cz) > 4.4 ? c.state(Blocks.PACKED_ICE) : c.state(Blocks.BLUE_ICE));
        crystal(0, (int) cz, 9);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 + 0.2;
            int x = (int) Math.round(Math.cos(a) * 12.2);
            int z = (int) Math.round(cz + Math.sin(a) * 12.2);
            for (int y = 1; y <= A_H; y++) c.set(x, y, z, c.state(y == A_H ? "tuile_givre" : y % 4 == 0 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE));
            if (i % 2 == 0) crystal((int) Math.round(Math.cos(a) * 9.2), (int) Math.round(cz + Math.sin(a) * 9.2), 4);
        }
        icicles(0, cz, 13, A_H + 8, 36, 2);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI * 2 * i / 4 + 0.78;
            Kit.chandelier(c, A_H + 7, (int) Math.round(Math.cos(a) * 8), (int) Math.round(cz + Math.sin(a) * 8), 4, Blocks.SOUL_LANTERN);
        }
    }

    @Override
    void pit() {
        // crevasse : abime de glace bleue eclairee par des lanternes marines, pont de calcite
        for (int z = PIT_Z0; z <= PIT_Z1; z++) {
            for (int x = -PIT_HALF; x <= PIT_HALF; x++) {
                if (Math.abs(x) <= DECK) continue;
                c.set(x, -PIT_DEPTH, z, c.rnd(x, 0, z, 9) < 0.18F ? c.state(Blocks.SEA_LANTERN) : c.state(Blocks.BLUE_ICE));
                if (c.rnd(x, 1, z, 10) < 0.25F) {
                    c.set(x, -PIT_DEPTH + 1, z, Blocks.POWDER_SNOW);
                }
            }
            for (int x = -DECK; x <= DECK; x++) c.set(x, 0, z, c.mix(x, 0, z, 13, "tuile_givre", 45, Blocks.PACKED_ICE, 35, "marbre_poli", 20));
        }
        for (int z = PIT_Z0; z <= PIT_Z1; z += 4) {
            for (int side : new int[]{-2, 2}) {
                for (int y = 1; y <= 3; y++) c.set(side, y, z, Blocks.BLUE_ICE);
                c.set(side, 4, z, Blocks.SEA_LANTERN);
            }
        }
        for (int z = PIT_Z0 + 2; z <= PIT_Z1 - 1; z += 4) {
            for (int side : new int[]{-2, 2}) c.set(side, 1, z, Blocks.SNOW);
        }
        icicles(0, (PIT_Z0 + PIT_Z1) / 2.0, 9, 11, 22, 3);
    }

    @Override
    void roomB() {
        double cz = B_Z;
        Kit.ribs(c, 0, cz, B_R, B_R, B_H, 9, 12, Blocks.BLUE_ICE);
        // sapins givres autour d'un cercle de menhirs
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            int x = (int) Math.round(Math.cos(a) * 6);
            int z = (int) Math.round(cz + Math.sin(a) * 6);
            for (int y = 1; y <= 5; y++) c.set(x, y, z, y == 5 ? Blocks.SEA_LANTERN : Blocks.PACKED_ICE);
        }
        for (int[] p : new int[][]{{-10, -6}, {10, -6}, {-10, 7}, {10, 7}}) {
            for (int y = 1; y <= 3; y++) c.set(p[0], y, (int) cz + p[1], Blocks.SPRUCE_LOG);
            for (int layer = 0; layer < 3; layer++) {
                final int ly = 3 + layer;
                int rad = 3 - layer;
                c.disc(p[0], ly, (int) cz + p[1], rad, (x, y, z) -> (x == p[0] && z == (int) cz + p[1]) ? c.state(Blocks.SPRUCE_LOG) : c.state(Blocks.SPRUCE_LEAVES.defaultBlockState()));
            }
            c.set(p[0], 6, (int) cz + p[1], "marbre_poli");
        }
        c.disc(0, 0, (int) cz, 4.4, (x, y, z) -> c.state(Blocks.BLUE_ICE));
        icicles(0, cz, 12, B_H + 8, 36, 4);
        for (int side : new int[]{-1, 1}) {
            for (int i = -2; i <= 2; i++) {
                c.set(side * 20 + i, 1, (int) cz - 3, Blocks.SNOW);
                crystal(side * 22, (int) cz + (i > 0 ? 3 : -3), 4);
            }
        }
        for (int i = 0; i < 4; i++) {
            double a = Math.PI * 2 * i / 4 + 0.78;
            Kit.chandelier(c, B_H + 7, (int) Math.round(Math.cos(a) * 8), (int) Math.round(cz + Math.sin(a) * 8), 4, Blocks.SOUL_LANTERN);
        }
    }

    @Override
    void gallery() {
        for (int z = GAL_Z0; z <= GAL_Z1 - 2; z += 4) {
            for (int side : new int[]{-1, 1}) {
                for (int y = 1; y <= 6; y++) c.set(side * 3, y, z, c.state(y == 6 ? "tuile_givre" : Blocks.PACKED_ICE));
                crystal(side * 3, z + 2, 3);
            }
            c.fill(-3, 7, z, 3, 7, z, Blocks.BLUE_ICE);
        }
        for (int z = GAL_Z0; z <= GAL_Z1 - 2; z++) {
            for (int x = -1; x <= 1; x++) c.set(x, 1, z, Blocks.CARPET.pick(x == 0 ? DyeColor.LIGHT_BLUE : DyeColor.WHITE));
        }
        icicles(0, (GAL_Z0 + GAL_Z1) / 2.0, 8, 12, 20, 5);
    }

    @Override
    void arena() {
        double cz = ARENA_Z;
        // huit piliers de glace couronnes de calcite, aiguilles de cristal le long des parois
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 + Math.PI / 8;
            int x = (int) Math.round(Math.cos(a) * 14.5);
            int z = (int) Math.round(cz + Math.sin(a) * 14.5);
            Kit.column(c, x, z, 1, ARENA_H - 1, Blocks.BLUE_ICE, Blocks.PACKED_ICE, null);
            c.set(x, ARENA_H - 1, z, "tuile_givre");
            c.set(x, 2, z, Blocks.BLUE_ICE);
            for (Direction d : Direction.Plane.HORIZONTAL) c.set(x + d.getStepX(), 1, z + d.getStepZ(), Blocks.SNOW);
        }
        for (int i = 0; i < 9; i++) {
            double a = Math.PI * 2 * i / 9 + 0.35;
            int z = (int) Math.round(cz + Math.sin(a) * 18.3);
            if (z > cz + 12 && Math.abs(Math.cos(a) * 18.3) < 5) continue;
            crystal((int) Math.round(Math.cos(a) * 18.3), z, 6 + i % 4);
        }
        // trone du Titan : siege de glace bleue massif, dossier hérisse de cristaux
        int tz = (int) cz + 15;
        c.fill(-4, 1, tz, 4, 2, tz + 3, Blocks.PACKED_ICE);
        c.fill(-3, 3, tz + 1, 3, 4, tz + 3, Blocks.BLUE_ICE);
        for (int y = 5; y <= 13; y++) {
            int w = y < 8 ? 4 : y < 11 ? 3 : 2;
            for (int x = -w; x <= w; x++) c.set(x, y, tz + 3, c.state((x + y) % 4 == 0 ? "tuile_givre" : Blocks.BLUE_ICE));
        }
        for (int x = -4; x <= 4; x += 2) {
            for (int y = 14; y <= 14 + (x % 4 == 0 ? 3 : 1); y++) c.set(x, y, tz + 3, "cristal_azur");
        }
        c.set(-4, 5, tz + 2, Blocks.SEA_LANTERN);
        c.set(4, 5, tz + 2, Blocks.SEA_LANTERN);
        // estrade et aurore
        Kit.rings(c, 0, 1, (int) cz, 5, "tuile_givre", Blocks.BLUE_ICE, Blocks.PACKED_ICE);
        c.set(0, 1, (int) cz, Blocks.AIR);
        Kit.ribs(c, 0, cz, ARENA_R, ARENA_R, ARENA_H, 15, 14, Blocks.BLUE_ICE);
        Kit.band(c, 0, cz, ARENA_R, 8, "tuile_givre");
        icicles(0, cz, 17, ARENA_H + 9, 70, 6);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + 0.5;
            Kit.chandelier(c, ARENA_H + 9, (int) Math.round(Math.cos(a) * 9), (int) Math.round(cz + Math.sin(a) * 9), 6, Blocks.SOUL_LANTERN);
        }
    }

    @Override
    void treasure() {
        double cz = TREASURE_Z;
        c.disc(0, 0, (int) cz, 5.0, (x, y, z) -> Math.hypot(x, z - cz) > 3.0 ? c.state(Blocks.PACKED_ICE) : c.state(Blocks.BLUE_ICE));
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5;
            crystal((int) Math.round(Math.cos(a) * 4.6), (int) Math.round(cz + Math.sin(a) * 4.6), 4 + i % 2 * 2);
        }
        icicles(0, cz, 6, 14, 14, 7);
    }

    @Override
    Trials.Palette palette() {
        return new Trials.Palette(Blocks.BLUE_ICE, Blocks.SEA_LANTERN, Blocks.BLUE_ICE, Blocks.PACKED_ICE, Blocks.SOUL_LANTERN,
                java.util.List.of("spectre_givre", "golem_cristal"), java.util.Map.of(TrialData.WAVES, "TEMPÊTE DE GIVRE", TrialData.JUMP, "BANQUISE BRISÉE", TrialData.SIMON, "ÉNIGME DES CRISTAUX"), Blocks.WATER, Blocks.SOUL_LANTERN);
    }

    @Override
    Trials.Plan plan() {
        return new Trials.Plan(TrialData.SIMON, TrialData.JUMP, Trials.JUMPROOM, 1, 1, 1);
    }

    @Override
    String arenaName() {
        return "Antre du Titan de Glace";
    }
}
