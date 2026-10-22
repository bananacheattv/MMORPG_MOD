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
    static final String JUMPROOM = "jumproom";

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
    record Palette(Block gate, Block padOn, Object platform, Object edge, Block lamp, List<String> mobs, Map<String, String> titles,
                   Block hazard, Block lantern) {
        String title(String kind) {
            return titles.getOrDefault(kind, "ÉPREUVE");
        }
    }

    static String id(Block b) {
        return BuiltInRegistries.BLOCK.getKey(b).toString();
    }

    private static List<Integer> box(BlockPos o, int x0, int y0, int z0, int x1, int y1, int z1) {
        return List.of(o.getX() + x0, o.getY() + y0, o.getZ() + z0, o.getX() + x1, o.getY() + y1, o.getZ() + z1);
    }

    /** Pose une herse sur toutes les cellules libres du couloir entre z0 et z1 et renvoie la boite occupee (relative). */
    static int[] gate(HubCtx c, int z0, int z1, Block block) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
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
                pal.title(TrialData.WAVES), "Survivez à " + waves + " vagues : la herse s'ouvrira", false));
    }

    // ================================================================== enigme de sequence

    /** Quatre dalles colorees sur un cercle autour du centre de la salle ; remplit {@code pads} et {@code states}. */
    private static void buildPads(HubCtx c, Palette pal, int cz, List<Integer> pads, List<String> states) {
        BlockPos o = c.O;
        DyeColor[] colors = {DyeColor.RED, DyeColor.BLUE, DyeColor.LIME, DyeColor.YELLOW};
        int[][] spots = {{-6, -6}, {6, -6}, {-6, 6}, {6, 6}};
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
    }

    static void simon(HubCtx c, ServerLevel level, TrialData data, Palette pal, String id, int cz, int gateZ0, int gateZ1, int diff, int lvl) {
        int[] g = gate(c, gateZ0, gateZ1, pal.gate());
        BlockPos o = c.O;
        List<Integer> pads = new ArrayList<>();
        List<String> states = new ArrayList<>();
        buildPads(c, pal, cz, pads, states);
        // lutrin d'activation a l'entree de la salle
        c.set(0, 0, cz - 11, Blocks.CONCRETE.pick(DyeColor.BLACK));
        c.set(0, 1, cz - 11, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, net.minecraft.core.Direction.SOUTH));
        data.add(new Trial(id, TrialData.SIMON, level.dimension(), box(o, -14, 1, cz - 14, 14, 9, cz + 14), abs(o, g), id(pal.gate()),
                List.of(o.getX(), o.getY() + 1, o.getZ() + cz - 11), pads, states, lvl, diff, List.of(),
                pal.title(TrialData.SIMON), "Activez le lutrin, observez les dalles puis répétez la séquence", false));
    }

    // ================================================================== cibles

    /** Quatre cibles sur des piliers au fond de la salle : un coup (arme, sort ou fleche) sur chacune les allume ; toutes allumees = herse ouverte. */
    static void targets(HubCtx c, ServerLevel level, TrialData data, Palette pal, String id, int cz, int gateZ0, int gateZ1, int diff, int lvl) {
        int[] g = gate(c, gateZ0, gateZ1, pal.gate());
        BlockPos o = c.O;
        List<Integer> pads = new ArrayList<>();
        List<String> states = new ArrayList<>();
        int[] xs = {-9, -3, 3, 9};
        for (int k = 0; k < 4; k++) {
            int x = xs[k];
            int z = cz + 9 - Math.abs(x) / 3;
            for (int y = 1; y <= 2; y++) c.set(x, y, z, c.state(pal.edge()));
            c.set(x, 3, z, Blocks.TARGET);
            c.set(x, 0, z, c.state(pal.edge()));
            pads.add(o.getX() + x);
            pads.add(o.getY() + 3);
            pads.add(o.getZ() + z);
            states.add(id(Blocks.TARGET));
            states.add(id(pal.padOn()));
        }
        data.add(new Trial(id, TrialData.TARGETS, level.dimension(), box(o, -14, 1, cz - 14, 14, 9, cz + 14), abs(o, g), id(pal.gate()),
                List.of(o.getX(), o.getY() + 1, o.getZ() + cz), pads, states, lvl, diff, List.of(),
                pal.title(TrialData.TARGETS), "Touchez les 4 cibles (arme, sort ou flèche) pour ouvrir la herse", false));
    }

    // ================================================================== enigme d'ordre

    /** Les couleurs de la sequence sont gravees dans le mur du couloir d'entree (z = 26..32) ; les dalles se touchent dans cet ordre. */
    static void order(HubCtx c, ServerLevel level, TrialData data, Palette pal, String id, int cz, int gateZ0, int gateZ1, int diff, int lvl, long seed) {
        int[] g = gate(c, gateZ0, gateZ1, pal.gate());
        BlockPos o = c.O;
        List<Integer> pads = new ArrayList<>();
        List<String> states = new ArrayList<>();
        buildPads(c, pal, cz, pads, states);
        List<Integer> seq = new ArrayList<>(List.of(0, 1, 2, 3));
        java.util.Collections.shuffle(seq, new Random(seed));
        DyeColor[] colors = {DyeColor.RED, DyeColor.BLUE, DyeColor.LIME, DyeColor.YELLOW};
        for (int i = 0; i < 4; i++) {
            int z = 26 + i * 2;
            for (int side : new int[]{-4, 4}) {
                c.set(side, 2, z, Blocks.CONCRETE.pick(DyeColor.BLACK));
                c.set(side, 4, z, Blocks.CONCRETE.pick(DyeColor.BLACK));
                c.set(side, 3, z, Blocks.CONCRETE.pick(colors[seq.get(i)]));
            }
            c.set(-3, 1, z, pal.lantern());
        }
        List<String> order = new ArrayList<>();
        for (int k : seq) order.add(String.valueOf(k));
        data.add(new Trial(id, TrialData.ORDER, level.dimension(), box(o, -14, 1, cz - 14, 14, 9, cz + 14), abs(o, g), id(pal.gate()),
                List.of(o.getX(), o.getY() + 1, o.getZ() + cz), pads, states, lvl, diff, order,
                pal.title(TrialData.ORDER), "Les couleurs gravées dans le couloir d'entrée donnent l'ordre des dalles", false));
    }

    // ================================================================== parcours de saut dans une salle ronde

    /** Lampes sur le pourtour et lustres, pour une salle dont le decor habituel est remplace par un lac de danger. */
    static void roomLamps(HubCtx c, Palette pal, int cz, int height, int r) {
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2 * i / 10 + 0.3;
            int x = (int) Math.round(Math.cos(a) * (r - 1.2));
            int z = cz + (int) Math.round(Math.sin(a) * (r - 1.2));
            if (c.air(x, 1, z)) c.set(x, 1, z, pal.lantern());
        }
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * 2 * i / 3 + 0.5;
            Kit.chandelier(c, height + 5, (int) Math.round(Math.cos(a) * 6), cz + (int) Math.round(Math.sin(a) * 6), 3, pal.lantern());
        }
    }

    /** Le sol central de la salle est creuse (voir StyledDungeon) : on y met le danger et un chemin de plateformes, de l'entree a la sortie. */
    static void jumpRoom(HubCtx c, ServerLevel level, TrialData data, Palette pal, String id, int cz, int r, int diff, int lvl, long seed) {
        BlockPos o = c.O;
        int pr = r - 3;
        c.disc(0, -8, cz, pr - 0.5, (x, y, z) -> c.state(pal.hazard() == Blocks.MUD ? Blocks.MUD : pal.hazard()));
        if (pal.hazard() == Blocks.LAVA || pal.hazard() == Blocks.WATER) {
            for (int y = -7; y <= -5; y++) c.disc(0, y, cz, pr - 0.5, (x, yy, z) -> c.state(pal.hazard()));
        }
        Random rnd = new Random(seed);
        if (pal.hazard() == Blocks.MUD) {
            for (int i = 0; i < 70; i++) {
                int x = rnd.nextInt(2 * pr + 1) - pr;
                int z = cz + rnd.nextInt(2 * pr + 1) - pr;
                if (Math.hypot(x, z - cz) < pr - 1 && c.air(x, -7, z)) c.set(x, -7, z, Blocks.POINTED_DRIPSTONE);
            }
        }
        int cursor = cz - pr;
        int prevX = 0;
        int count = 0;
        int end = cz + pr;
        while (true) {
            int gap = diff <= 0 ? 2 : 3;
            int len = diff <= 0 ? 3 : diff == 1 ? 2 : 1 + rnd.nextInt(2);
            int wid = diff <= 0 ? 3 : 2;
            int zs = cursor + gap + 1;
            if (zs + len - 1 > end - 1) break;
            double zc = zs + len / 2.0 - cz;
            int lim = (int) Math.max(0, Math.sqrt(Math.max(0, (pr - 1.5) * (pr - 1.5) - zc * zc)) - wid / 2.0);
            int cx = Math.max(-lim, Math.min(lim, prevX + rnd.nextInt(7) - 3));
            for (int dx = 0; dx < wid; dx++) {
                for (int dz = 0; dz < len; dz++) {
                    int x = cx + dx - wid / 2;
                    c.set(x, 0, zs + dz, c.state(pal.platform()));
                    c.set(x, -1, zs + dz, c.state(pal.edge()));
                }
            }
            if (count % 3 == 2) c.set(cx, 1, zs, pal.lantern());
            cursor = zs + len - 1;
            prevX = cx;
            count++;
            if (cursor >= end - 3) break;
        }
        data.add(new Trial(id, TrialData.JUMP, level.dimension(), box(o, -pr, -3, cz - pr, pr, 8, cz + pr), box(o, -6, 1, cz + pr, 6, 6, cz + r),
                "", List.of(o.getX(), o.getY() + 1, o.getZ() + cz - pr - 1), List.of(), List.of(), lvl, diff, List.of(),
                pal.title(TrialData.JUMP), "Traversez le lac de plateforme en plateforme : une chute vous ramène au départ", false));
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
                pal.title(TrialData.JUMP), "Traversez sans tomber : une chute vous ramène au départ", false));
    }
}
