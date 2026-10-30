package com.mmorpg.dungeon;

import com.mmorpg.hub.HubCtx;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crypte Gobeline : un terrier de guerre creuse dans la roche moussue, etaye de poutres sombres, eclaire de lanternes et de
 * feux de camp ; l'arene est une fosse a gradins dominee par un trone de crane et de ferraille.
 */
class GoblinTheme extends Theme {
    GoblinTheme(HubCtx c, DungeonType t) {
        super(c, t);
    }

    @Override
    BlockState shell(int x, int y, int z, Carve.Kind kind, boolean back) {
        if (back) return c.mix(x, y, z, 1, Blocks.COBBLESTONE, 35, Blocks.MOSSY_COBBLESTONE, 25, Blocks.DIRT, 15, Blocks.STONE, 25);
        return switch (kind) {
            case FLOOR -> floor(x, y, z);
            case CEILING -> c.mix(x, y, z, 3, Blocks.MOSSY_COBBLESTONE, 38, Blocks.COBBLESTONE, 24, Blocks.MOSS_BLOCK, 10, Blocks.DARK_OAK_PLANKS, 16, Blocks.ROOTED_DIRT, 12);
            case WALL -> c.mix(x, y, z, 2, Blocks.MOSSY_COBBLESTONE, 38, Blocks.COBBLESTONE, 22, Blocks.MUD_BRICKS, 16, Blocks.MOSSY_STONE_BRICKS, 14, Blocks.MOSS_BLOCK, 10);
        };
    }

    private BlockState floor(int x, int y, int z) {
        if (arenaRegion(z) && z < ARENA_Z + ARENA_R + 2) {
            double d = arenaDist(x, z);
            double a = Carve.angle(x, z, 0, ARENA_Z);
            if (d >= 5.0 && d <= 6.2) return Blocks.DARK_OAK_PLANKS.defaultBlockState();
            if (d >= 12.0 && d <= 13.2) return Blocks.SPRUCE_PLANKS.defaultBlockState();
            if (d > 6.2 && d < 12.0 && (a % 45) < 7 || (a % 45) > 38) return Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        }
        return c.mix(x, y, z, 4, Blocks.PACKED_MUD, 36, Blocks.MUD_BRICKS, 22, Blocks.COARSE_DIRT, 14, Blocks.COBBLESTONE, 16, Blocks.MOSSY_COBBLESTONE, 12);
    }

    @Override
    BlockState light(int x, int y, int z) {
        return Blocks.SHROOMLIGHT.defaultBlockState();
    }

    @Override
    void shape(Carve v) {
        // terriers : petites cavites aux flancs des salles de combat (jamais le long d'un lac de saut : on ne contourne rien)
        for (int i = 0; i < Layout.ROOMS.length; i++) {
            Layout.Room rm = Layout.ROOMS[i];
            if (jumpRoom(i)) continue;
            for (int[] p : new int[][]{{-1, -8}, {1, -4}, {-1, 7}, {1, 9}}) {
                int x = p[0] * (rm.r() + 1);
                int z = rm.z() + p[1];
                v.cyl(x, z, 3.2, 1, 5, 0.6, x + z);
                v.dome(x, z, 3.2, 3.2, 5, 2.5);
            }
        }
    }

    // ------------------------------------------------------------------ espaces

    private void lantern(int x, int y, int z) {
        c.set(x, y, z, Blocks.SPRUCE_FENCE);
        c.set(x, y + 1, z, Blocks.SPRUCE_FENCE);
        c.set(x, y + 2, z, Blocks.LANTERN);
    }

    private void totem(int x, int z, int h) {
        for (int y = 1; y <= h; y++) {
            BlockState s = switch (y % 4) {
                case 0 -> Blocks.CARVED_PUMPKIN.defaultBlockState();
                case 1 -> Blocks.DARK_OAK_LOG.defaultBlockState();
                case 2 -> Blocks.BONE_BLOCK.defaultBlockState();
                default -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            };
            c.set(x, y, z, s);
        }
        c.set(x, h + 1, z, Blocks.LANTERN);
        for (int[] o : new int[][]{{1, 0}, {-1, 0}}) c.set(x + o[0], 3, z + o[1], Blocks.OAK_FENCE);
    }

    @Override
    void hall() {
        Kit.ribs(c, 0, HALL_Z, HALL_R, HALL_R, HALL_H, 11, 12, Blocks.DARK_OAK_LOG);
        Kit.band(c, 0, HALL_Z, HALL_R, 6, Blocks.DARK_OAK_PLANKS);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12 + Math.PI / 12;
            lantern((int) Math.round(Math.cos(a) * 9), 1, HALL_Z + (int) Math.round(Math.sin(a) * 9));
        }
        totem(-7, HALL_Z - 5, 6);
        totem(7, HALL_Z - 5, 6);
        totem(-7, HALL_Z + 6, 5);
        totem(7, HALL_Z + 6, 5);
        Kit.chandelier(c, HALL_H + 6, 0, HALL_Z, 5, Blocks.LANTERN);
        // sacs, tonneaux et foin pres de l'entree
        for (int[] p : new int[][]{{-9, HALL_Z + 1}, {9, HALL_Z - 1}, {-8, HALL_Z - 4}}) {
            c.set(p[0], 1, p[1], Blocks.BARREL);
            c.set(p[0] + 1, 1, p[1], Blocks.HAY_BLOCK);
        }
    }

    @Override
    void roomA(Layout.Room rm) {
        double cz = rm.z();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 + Math.PI / 8;
            int x = (int) Math.round(Math.cos(a) * 11.5);
            int z = (int) Math.round(cz + Math.sin(a) * 11.5);
            for (int y = 1; y <= rm.h(); y++) c.set(x, y, z, c.axis(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
            for (Direction d : Direction.Plane.HORIZONTAL) c.set(x + d.getStepX(), rm.h(), z + d.getStepZ(), c.stair(Blocks.DARK_OAK_STAIRS, d, true));
            c.set(x, 1, z, Blocks.SPRUCE_PLANKS);
        }
        // roue de chariot suspendue : anneau de planches porte par quatre chaines, lanternes sur le pourtour
        int wy = rm.h() - 2;
        c.ring(0, wy, (int) cz, 3.0, 4.6, (x, y, z) -> Blocks.SPRUCE_PLANKS.defaultBlockState());
        c.disc(0, wy, (int) cz, 1.4, (x, y, z) -> Blocks.DARK_OAK_PLANKS.defaultBlockState());
        for (int[] o : new int[][]{{4, 0}, {-4, 0}, {0, 4}, {0, -4}}) {
            for (int y = wy + 1; y < wy + 6; y++) c.set(o[0], y, (int) cz + o[1], Blocks.IRON_CHAIN);
            c.set(o[0], wy - 1, (int) cz + o[1], c.hanging(Blocks.LANTERN));
        }
        // foyer central
        c.ring(0, 1, (int) cz, 1.6, 2.6, (x, y, z) -> Blocks.COBBLESTONE.defaultBlockState());
        c.set(0, 1, (int) cz, Blocks.CAMPFIRE);
        // butin accumule
        for (int[] p : new int[][]{{-12, -4}, {-12, 5}, {13, -6}}) {
            c.set(p[0], 1, (int) cz + p[1], Blocks.HAY_BLOCK);
            c.set(p[0], 2, (int) cz + p[1], Blocks.BARREL);
        }
    }

    @Override
    void pit(int z0, int z1) {
        // fosse : boue et pieux de pierre, pont de planches avec cordes et lanternes
        for (int z = z0; z <= z1; z++) {
            for (int x = -PIT_HALF; x <= PIT_HALF; x++) {
                if (Math.abs(x) <= DECK) continue;
                c.set(x, -PIT_DEPTH, z, Blocks.MUD);
                if (c.rnd(x, 0, z, 9) < 0.55F) c.set(x, -PIT_DEPTH + 1, z, Blocks.POINTED_DRIPSTONE);
            }
            for (int x = -DECK; x <= DECK; x++) c.set(x, 0, z, (z % 3 == 0 ? Blocks.DARK_OAK_PLANKS : Blocks.SPRUCE_PLANKS));
        }
        for (int z = z0; z <= z1; z += 4) {
            for (int side : new int[]{-2, 2}) {
                c.set(side, 1, z, Blocks.SPRUCE_FENCE);
                c.set(side, 2, z, Blocks.SPRUCE_FENCE);
                c.set(side, 3, z, Blocks.LANTERN);
            }
        }
        for (int z = z0; z <= z1; z++) {
            for (int side : new int[]{-2, 2}) {
                if (z % 4 != 0) c.set(side, 1, z, Blocks.SPRUCE_FENCE);
            }
        }
    }

    @Override
    void roomB(Layout.Room rm) {
        double cz = rm.z();
        // camp de guerre : cercle de pierre, chaudron, enclumes et ratelier
        c.ring(0, 1, (int) cz, 5.0, 6.2, (x, y, z) -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
        c.set(0, 1, (int) cz, Blocks.CAULDRON);
        c.set(2, 1, (int) cz + 2, Blocks.CAMPFIRE);
        c.set(-2, 1, (int) cz - 2, Blocks.CAMPFIRE);
        for (int[] p : new int[][]{{-9, -8}, {9, -8}, {-9, 8}, {9, 8}}) {
            c.set(p[0], 1, (int) cz + p[1], Blocks.ANVIL);
            c.set(p[0] + (p[0] > 0 ? -1 : 1), 1, (int) cz + p[1], Blocks.GRINDSTONE);
            lantern(p[0], 2, (int) cz + p[1]);
        }
        // cages suspendues
        for (int[] p : new int[][]{{-7, 4}, {7, -4}}) {
            for (int y = rm.h() + 5; y > 6; y--) c.set(p[0], y, (int) cz + p[1], Blocks.IRON_CHAIN);
            c.fill(p[0] - 1, 3, (int) cz + p[1] - 1, p[0] + 1, 5, (int) cz + p[1] + 1, Blocks.IRON_BARS);
            c.fill(p[0], 4, (int) cz + p[1], p[0], 4, (int) cz + p[1], Blocks.AIR);
            c.set(p[0], 6, (int) cz + p[1], Blocks.IRON_BARS);
            c.set(p[0], 3, (int) cz + p[1], Blocks.IRON_BARS);
        }
        Kit.ribs(c, 0, cz, rm.r(), rm.r(), rm.h(), 9, 8, Blocks.SPRUCE_LOG);
        // alcoves : litieres de foin et totems
        for (int side : new int[]{-1, 1}) {
            c.fill(side * 20, 1, (int) cz - 2, side * 22, 1, (int) cz + 2, Blocks.HAY_BLOCK);
            lantern(side * 16, 1, (int) cz - 3);
            lantern(side * 16, 1, (int) cz + 3);
        }
    }

    @Override
    void gallery() {
        for (int z = GAL_Z0; z <= GAL_Z1 - 2; z += 4) {
            for (int side : new int[]{-1, 1}) {
                int x = side * 3;
                for (int y = 1; y <= 6; y++) c.set(x, y, z, c.axis(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
                c.set(x, 7, z, Blocks.DARK_OAK_PLANKS);
                c.set(x - side, 4, z, c.hanging(Blocks.LANTERN));
                c.set(x, 1, z + 2, Kit.skull(Blocks.ZOMBIE_HEAD, z));
            }
            c.fill(-3, 8, z, 3, 8, z, Blocks.DARK_OAK_PLANKS);
        }
    }

    @Override
    void arena() {
        double cz = ARENA_Z;
        // gradins : trois anneaux de marches ou les gobelins regardent le combat
        c.ring(0, 1, (int) cz, 16.0, 17.5, (x, y, z) -> Math.abs(x) < 5 ? null : c.mix(x, y, z, 6, Blocks.MOSSY_COBBLESTONE, 50, Blocks.COBBLESTONE, 30, Blocks.MUD_BRICKS, 20));
        c.ring(0, 2, (int) cz, 17.0, 18.5, (x, y, z) -> Math.abs(x) < 5 ? null : c.mix(x, y, z, 7, Blocks.MOSSY_COBBLESTONE, 50, Blocks.COBBLESTONE, 30, Blocks.MUD_BRICKS, 20));
        c.ring(0, 3, (int) cz, 18.0, 19.6, (x, y, z) -> Math.abs(x) < 5 ? null : c.mix(x, y, z, 8, Blocks.MOSSY_COBBLESTONE, 50, Blocks.COBBLESTONE, 30, Blocks.MUD_BRICKS, 20));
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + 0.3;
            int x = (int) Math.round(Math.cos(a) * 17);
            int z = (int) Math.round(cz + Math.sin(a) * 17);
            if (z > cz + 12) continue;                                   // pas devant le passage du tresor
            c.set(x, 4, z, Kit.skull(Blocks.SKELETON_SKULL, i * 3));
            c.set(x, 5, z, Blocks.LANTERN);
        }
        // tours de guet aux quatre diagonales
        for (int[] d : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            int tx = d[0] * 14;
            int tz = (int) cz + d[1] * 14;
            for (int y = 1; y <= 12; y++) {
                for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) {
                    c.set(tx + dx, y, tz + dz, c.axis(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
                }
            }
            c.fill(tx - 1, 12, tz - 1, tx + 2, 12, tz + 2, Blocks.SPRUCE_PLANKS);
            for (int dx = -1; dx <= 2; dx++) for (int dz = -1; dz <= 2; dz++) {
                if (dx == -1 || dx == 2 || dz == -1 || dz == 2) c.set(tx + dx, 13, tz + dz, Blocks.SPRUCE_FENCE);
            }
            c.set(tx, 13, tz, Blocks.LANTERN);
            c.set(tx + 1, 13, tz + 1, Blocks.CAMPFIRE);
        }
        // braseros du cercle de combat
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 + Math.PI / 8;
            Kit.brazier(c, (int) Math.round(Math.cos(a) * 9.5), 1, (int) Math.round(cz + Math.sin(a) * 9.5), Blocks.COBBLESTONE_WALL, Blocks.CAMPFIRE);
        }
        // estrade de l'autel
        c.disc(0, 1, (int) cz, 4.0, (x, y, z) -> c.mix(x, y, z, 10, Blocks.MOSSY_STONE_BRICKS, 60, Blocks.STONE_BRICKS, 40));
        c.set(0, 1, (int) cz, Blocks.AIR);
        // trone de crane et de ferraille devant l'alcove du tresor
        int tz = (int) cz + 15;
        c.fill(-3, 1, tz, 3, 2, tz + 2, Blocks.BONE_BLOCK);
        c.fill(-2, 3, tz + 1, 2, 6, tz + 2, Blocks.MUD_BRICKS);
        c.fill(-1, 3, tz, 1, 3, tz, Blocks.DARK_OAK_PLANKS);
        for (int x = -3; x <= 3; x += 2) {
            c.set(x, 7, tz + 2, Blocks.BONE_BLOCK);
            c.set(x, 8, tz + 2, Kit.skull(Blocks.SKELETON_SKULL, 8));
        }
        c.set(-3, 3, tz + 1, Blocks.IRON_BARS);
        c.set(3, 3, tz + 1, Blocks.IRON_BARS);
        // nervures et lanternes suspendues
        Kit.ribs(c, 0, cz, ARENA_R, ARENA_R, ARENA_H, 15, 16, Blocks.DARK_OAK_LOG);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            int x = (int) Math.round(Math.cos(a) * 11);
            int z = (int) Math.round(cz + Math.sin(a) * 11);
            Kit.chandelier(c, ARENA_H + 7, x, z, 3, Blocks.LANTERN);
        }
    }

    @Override
    void treasure() {
        double cz = TREASURE_Z;
        for (int[] p : new int[][]{{-4, -2}, {4, -2}, {-4, 2}, {4, 2}, {0, 5}}) {
            c.set(p[0], 1, (int) cz + p[1], Blocks.GOLD_BLOCK);
            c.set(p[0], 2, (int) cz + p[1], Blocks.GOLD_BLOCK);
            c.set(p[0] > 0 ? p[0] - 1 : p[0] + 1, 1, (int) cz + p[1], Blocks.GOLD_BLOCK);
        }
        c.set(-2, 1, (int) cz, Blocks.HAY_BLOCK);
        c.set(2, 1, (int) cz, Blocks.HAY_BLOCK);
        Kit.chandelier(c, 14, 0, (int) cz, 4, Blocks.LANTERN);
    }

    @Override
    Trials.Palette palette() {
        return new Trials.Palette(Blocks.SPRUCE_FENCE, Blocks.SHROOMLIGHT, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.LANTERN,
                java.util.List.of("gobelin", "loup_sombre"), java.util.Map.of(TrialData.WAVES, "ASSAUT DU CAMP", TrialData.JUMP, "PASSERELLE BRISÉE", TrialData.SIMON, "ÉNIGME DES TOTEMS", TrialData.ORDER, "LANGAGE DES TOTEMS", TrialData.TARGETS, "TIR AUX TOTEMS", TrialData.ELITE, "GARDIEN DU TERRIER", TrialData.AMBUSH, "EMBUSCADE GOBELINE"), Blocks.MUD, Blocks.LANTERN);
    }

    @Override
    Trials.Plan plan() {
        // salle 1, couloir 1, salle 2, salle 3, couloir 2, salle 4, salle 5, gardien d'elite
        return Trials.plan(TrialData.WAVES, 0, TrialData.JUMP, 0, TrialData.TARGETS, 0, TrialData.ORDER, 0, TrialData.JUMP, 1, TrialData.WAVES, 1, Trials.JUMPROOM, 1, TrialData.ELITE, 1);
    }

    @Override
    String eliteKey() {
        return "colosse_racine";
    }

    @Override
    Look look() {
        return new Look(Blocks.DARK_OAK_LOG, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_STAIRS, Blocks.LANTERN, Blocks.CAMPFIRE, Blocks.MUD_BRICKS, Blocks.COBBLESTONE, Blocks.BONE_BLOCK);
    }

    @Override
    String lore() {
        return "Les tambours de guerre résonnent sous la terre… Le Roi Gobelin vous attend.";
    }

    @Override
    String arenaName() {
        return "Antre du Roi Gobelin";
    }
}
