package com.mmorpg.dungeon;

import com.mmorpg.dungeon.TrialData.Trial;
import com.mmorpg.hub.HubCtx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Construction des epreuves d'un donjon : herses, dalles et lutrin de l'enigme, plateformes du parcours de saut, et enregistrement
 * dans {@link TrialData}. Chaque ambiance fournit sa {@link Palette} et son {@link Plan} (quelle epreuve dans quelle salle).
 */
final class Trials {
    private Trials() {
    }

    static final String NONE = "none";

    /** Quelle epreuve dans la salle de combat A, le couloir a fosse et la salle B (« waves », « simon », « jump » ou « none »). */
    record Plan(String a, String pit, String b, int diffA, int diffPit, int diffB) {
    }

    /**
     * @param gate     bloc de la herse
     * @param padOn    bloc lumineux d'une dalle allumee
     * @param platform bloc des plateformes de saut (identifiant de decor ou bloc)
     * @param edge     bloc de l'epaisseur des plateformes
     * @param lamp     petite lumiere posee sur certaines plateformes
     * @param mobs     monstres des vagues (cles de bestiaire)
     * @param titles   noms affiches pour chaque type d'epreuve
     */
    record Palette(Block gate, Block padOn, Object platform, Object edge, Block lamp, List<String> mobs, Map<String, String> titles) {
    }

    static String id(Block b) {
        return BuiltInRegistries.BLOCK.getKey(b).toString();
    }

    private static List<Integer> box(BlockPos o, int x0, int y0, int z0, int x1, int y1, int z1) {
        return List.of(o.getX() + x0, o.getY() + y0, o.getZ() + z0, o.getX() + x1, o.getY() + y1, o.getZ() + z1);
    }

    /** Pose une herse sur toutes les cellules libres du couloir entre z0 et z1 et renvoie la boite occupee (relative). */
    static int[] gate(HubCtx c, int z0, int z1, Block block) {
        int minX = 99, maxX = -99, minY = 99, maxY = -99, minZ = 99, maxZ = -99;
        for (int z = z0; z <= z1; z++) {
            for (int x = -5; x <= 5; x++) {
                for (int y = 1; y <= 14; y++) {
                    if (!c.air(x, y, z)) continue;
                    c.set(x, y, z, block);
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                    minZ = Math.min(minZ, z);
                    maxZ = Math.max(maxZ, z);
                }
            }
        }
        return new int[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    private static List<Integer> abs(BlockPos o, int[] g) {
        return box(o, g[0], g[1], g[2], g[3], g[4], g[5]);
    }

    // ================================================================== vagues

    static void waves(HubCtx c, ServerLevel level, TrialData data, Palette pal, String id, int cz, int gateZ0, int gateZ1, int diff, int lvl) {
        int[] g = gate(c, gateZ0, gateZ1, pal.gate());
        BlockPos o = c.O;
        int waves = 3 + (diff >= 2 ? 1 : 0);
        data.add(new Trial(id, TrialData.WAVES, level.dimension(), box(o, -13, 1, cz - 13, 13, 9, cz + 13), abs(o, g), id(pal.gate()),
                List.of(o.getX(), o.getY() + 1, o.getZ() + cz), List.of(), List.of(), lvl, diff, pal.mobs(),
                pal.titles().get(TrialData.WAVES), "Survivez à " + waves + " vagues : la herse s'ouvrira", false));
    }

    // ================================================================== enigme de sequence

    static void simon(HubCtx c, ServerLevel level, TrialData data, Palette pal, String id, int cz, int gateZ0, int gateZ1, int diff, int lvl) {
        int[] g = gate(c, gateZ0, gateZ1, pal.gate());
        BlockPos o = c.O;
        DyeColor[] colors = {DyeColor.RED, DyeColor.BLUE, DyeColor.LIME, DyeColor.YELLOW};
        int[][] spots = {{-6, -6}, {6, -6}, {-6, 6}, {6, 6}};
        List<Integer> pads = new ArrayList<>();
        List<String> states = new ArrayList<>();
        for (int k = 0; k < 4; k++) {
            int px = spots[k][0];
            int pz = cz + spots[k][1];
            c.fill(px - 1, 0, pz - 1, px + 1, 0, pz + 1, Blocks.CONCRETE.pick(DyeColor.BLACK));
            c.set(px, 0, pz, Blocks.CONCRETE.pick(colors[k]));
            c.set(px, 1, pz, Blocks.CONCRETE.pick(colors[k]));
            for (int y = 2; y <= 6; y++) c.set(px, y, pz, Blocks.AIR);
            pads.add(o.getX() + px);
            pads.add(o.getY() + 1);
            pads.add(o.getZ() + pz);
            states.add(id(Blocks.CONCRETE.pick(colors[k])));
            states.add(id(pal.padOn()));
        }
        // lutrin d'activation a l'entree de la salle
        c.set(0, 0, cz - 11, Blocks.CONCRETE.pick(DyeColor.BLACK));
        c.set(0, 1, cz - 11, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, net.minecraft.core.Direction.SOUTH));
        data.add(new Trial(id, TrialData.SIMON, level.dimension(), box(o, -14, 1, cz - 14, 14, 9, cz + 14), abs(o, g), id(pal.gate()),
                List.of(o.getX(), o.getY() + 1, o.getZ() + cz - 11), pads, states, lvl, diff, List.of(),
                pal.titles().get(TrialData.SIMON), "Activez le lutrin, observez les dalles puis répétez la séquence", false));
    }

    // ================================================================== parcours de saut

    static void jump(HubCtx c, ServerLevel level, TrialData data, Palette pal, String id, int z0, int z1, int diff, int lvl, long seed) {
        BlockPos o = c.O;
        int from = z0 + 3;
        int to = z1 - 3;
        // le pont disparait : il ne reste que le depart et l'arrivee
        for (int z = from; z <= to; z++) {
            for (int x = -2; x <= 2; x++) {
                c.set(x, 0, z, Blocks.AIR);
                c.set(x, -1, z, Blocks.AIR);
                for (int y = 1; y <= 4; y++) c.set(x, y, z, Blocks.AIR);
            }
        }
        Random r = new Random(seed);
        int cursor = from - 1;
        int prevX = 0;
        int count = 0;
        int maxGap = diff <= 0 ? 2 : 3;
        while (true) {
            int gap = diff <= 0 ? 2 : (diff == 1 ? 3 : 2 + r.nextInt(2));
            int len = diff <= 0 ? 3 : diff == 1 ? 2 : 1 + r.nextInt(2);
            int wid = diff <= 0 ? 3 : diff == 1 ? 2 : 1 + r.nextInt(2);
            int zs = cursor + gap + 1;
            if (zs + len - 1 > to) break;
            int cx = Math.max(-2, Math.min(2, prevX + r.nextInt(5) - 2));
            for (int dx = 0; dx < wid; dx++) {
                for (int dz = 0; dz < len; dz++) {
                    int x = Math.max(-3, Math.min(3, cx + dx - wid / 2));
                    c.set(x, 0, zs + dz, c.state(pal.platform()));
                    c.set(x, -1, zs + dz, c.state(pal.edge()));
                }
            }
            if (count % 3 == 2) c.set(cx, 1, zs, pal.lamp());
            cursor = zs + len - 1;
            prevX = cx;
            count++;
            if (cursor >= to - maxGap) break;
        }
        data.add(new Trial(id, TrialData.JUMP, level.dimension(), box(o, -4, -3, from, 4, 8, to), box(o, -4, 1, z1 - 2, 4, 6, z1),
                "", List.of(o.getX(), o.getY() + 1, o.getZ() + z0 + 1), List.of(), List.of(), lvl, diff, List.of(),
                pal.titles().get(TrialData.JUMP), "Traversez sans tomber : une chute vous ramène au départ", false));
    }
}
