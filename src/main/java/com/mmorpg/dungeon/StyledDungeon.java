package com.mmorpg.dungeon;

import com.mmorpg.hub.HubCtx;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.world.DungeonBuilder;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import com.mmorpg.world.WaypointData;
import com.mmorpg.zone.ZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Donjons de boss stylises, d'environ vingt minutes : hall d'entree a coupole, six salles d'epreuve rondes (vagues, enigmes, cibles,
 * lac de plateformes, gardien d'elite), deux couloirs a fosse (parcours de saut), quatre couloirs d'embuscade, la galerie des gardiens,
 * une arene de boss monumentale et une alcove de tresor. Chaque ambiance (voir {@link Theme}) fournit sa coque, ses motifs de sol,
 * ses lumieres et son decor ; ici on assemble le plan commun ({@link Layout}) et on pose les elements de jeu (teleporteur,
 * epreuves, coffres, autel d'invocation, zones d'annonce).
 */
public final class StyledDungeon {
    private StyledDungeon() {
    }

    private static Theme theme(HubCtx c, DungeonType t) {
        return switch (t) {
            case CRYPTE_GOBELINE -> new GoblinTheme(c, t);
            case CATACOMBES -> new CryptTheme(c, t);
            case FORTERESSE_INFERNALE -> new InfernalTheme(c, t);
            case SANCTUAIRE_GLACE -> new FrostTheme(c, t);
            case CITADELLE_NEANT -> new VoidTheme(c, t);
        };
    }

    /** @return la position du teleporteur d'entree */
    public static BlockPos build(ServerLevel level, BlockPos origin, DungeonType t) {
        return build(level, origin, t, 0);
    }

    /** Construction decoupee en etapes : on peut les executer d'un coup ({@link #build}) ou une par tick (donjons instancies, avec progression). */
    public static final class Job {
        final ServerLevel level;
        final BlockPos o;
        final HubCtx c;
        final Theme th;
        final DungeonType t;
        final int bonus;
        Carve v;
        public BlockPos tp;
        final java.util.List<Runnable> steps = new java.util.ArrayList<>();

        Job(ServerLevel level, BlockPos origin, DungeonType t, int bonus) {
            this.level = level;
            this.o = origin.below();
            this.c = new HubCtx(level, o);
            this.th = theme(c, t);
            this.t = t;
            this.bonus = bonus;
            this.tp = o.offset(0, 1, Theme.HALL_Z);
            plan();
        }

        public int size() {
            return steps.size();
        }

        public void run(int i) {
            steps.get(i).run();
        }

        private void plan() {
            String key = t.id + "_" + o.getX() + "_" + o.getZ();
            Trials.Plan plan = th.plan();
            Trials.Palette pal = th.palette();
            steps.add(this::shapes);
            int zFirst = -4;
            int zLast = Layout.LENGTH + 2;
            for (int za = zFirst; za <= zLast; za += 80) {
                int a = za;
                steps.add(() -> v.prepareShellSlice(a, Math.min(zLast, a + 79)));
            }
            int slices = (zLast - zFirst) / 18 + 1;
            int span = (zLast - zFirst + slices) / slices;
            for (int k = 0; k < slices; k++) {
                int za = zFirst + k * span;
                int zb = Math.min(zLast, za + span - 1);
                steps.add(() -> v.shellSlice(c, (x, y, z, kind, back) -> {
                    if (!back && kind != Carve.Kind.FLOOR && th.rnd(x, y, z, 77) < th.lightChance()) return th.light(x, y, z);
                    return th.shell(x, y, z, kind, back);
                }, za, zb));
            }
            for (int za = zFirst; za <= zLast; za += 120) {
                int a = za;
                steps.add(() -> foundation(a, Math.min(zLast, a + 119)));
            }
            // decor : hall, salles (signature de l'ambiance pour les deux premieres, decors communs ensuite), couloirs
            steps.add(th::hall);
            for (int i = 0; i < Layout.ROOMS.length; i++) {
                int idx = i;
                Layout.Room rm = Layout.ROOMS[i];
                steps.add(() -> decorRoom(idx, rm));
            }
            steps.add(() -> th.pit(Layout.P1.z0(), Layout.P1.z1()));
            steps.add(() -> th.pit(Layout.P2.z0(), Layout.P2.z1()));
            for (Layout.Corridor cr : new Layout.Corridor[]{Layout.C0, Layout.C2, Layout.C4, Layout.C5, Layout.C6}) {
                steps.add(() -> Rooms.corridor(th, cr));
            }
            steps.add(th::gallery);
            steps.add(th::arena);
            steps.add(th::treasure);
            // epreuves : une par salle ou couloir a fosse, puis les embuscades
            TrialData trials = TrialData.get(level.getServer());
            long seed = o.asLong() * 31 + t.level;
            int lvl = t.level + 4 * bonus;
            for (int i = 0; i < Layout.ROOMS.length; i++) {
                int idx = i;
                steps.add(() -> roomTrial(trials, pal, plan, key, idx, lvl, seed + idx));
            }
            for (int j = 0; j < Layout.PITS.length; j++) {
                int idx = j;
                steps.add(() -> pitTrial(trials, pal, plan, key, idx, lvl, seed + 10 + idx));
            }
            for (int k = 0; k < Layout.AMBUSHES.length; k++) {
                Layout.Corridor cr = Layout.AMBUSHES[k];
                String id = key + "_x" + (k + 1);
                int diff = Math.min(3, plan.room(k + 1).diff() + bonus);
                steps.add(() -> Trials.ambush(c, level, trials, pal, id, cr, diff, lvl));
            }
            steps.add(() -> gameElements(plan));
            steps.add(this::registerZones);
        }

        private void decorRoom(int i, Layout.Room rm) {
            if (th.jumpRoom(i)) {
                Trials.roomLamps(c, th.palette(), rm);
                return;
            }
            switch (i) {
                case 0 -> th.roomA(rm);
                case 1 -> th.roomB(rm);
                case 2 -> Rooms.pillared(th, rm);
                case 3 -> Rooms.shrine(th, rm);
                case 4 -> Rooms.coliseum(th, rm);
                default -> Rooms.throne(th, rm);
            }
        }

        private void shapes() {
            // reconstruction au meme endroit (meme un autre donjon) : on efface zones et epreuves de l'ancien
            var oldZones = ZoneData.get(level.getServer());
            var oldTrials = TrialData.get(level.getServer());
            int zEnd = o.getZ() + Layout.LENGTH + 10;
            oldTrials.removeInside(level.dimension(), o.getX() - 40, o.getZ() - 10, o.getX() + 40, zEnd);
            for (DungeonType other : DungeonType.values()) {
                oldZones.removeOverlapping(level.dimension(), other.id + "_", o.getX() - 40, o.getZ() - 10, o.getX() + 40, zEnd);
            }
            v = new Carve(-Layout.HALF_WIDTH, Layout.HALF_WIDTH, -11, 36, -4, Layout.LENGTH + 2);
            Layout.Room hall = Layout.HALL;
            v.cyl(0, hall.z(), hall.r(), 1, hall.h());
            v.dome(0, hall.z(), hall.r(), hall.r(), hall.h(), 11);
            // couloirs voutes
            for (Layout.Corridor cr : new Layout.Corridor[]{Layout.C0, Layout.P1, Layout.C2, Layout.P2, Layout.C4, Layout.C5, Layout.C6}) {
                v.vault(0, cr.z0() - 1, cr.z1() + 1, Layout.COR_HALF, 1, 6, 4, false);
            }
            // fosses sous les ponts
            for (Layout.Corridor cr : Layout.PITS) {
                v.box(-Theme.PIT_HALF, -Theme.PIT_DEPTH, cr.z0(), Theme.PIT_HALF, -2, cr.z1());
                v.box(-Theme.PIT_HALF, -1, cr.z0(), -Theme.DECK - 1, 0, cr.z1());
                v.box(Theme.DECK + 1, -1, cr.z0(), Theme.PIT_HALF, 0, cr.z1());
            }
            // salles d'epreuve
            Trials.Plan plan0 = th.plan();
            for (int i = 0; i < Layout.ROOMS.length; i++) {
                Layout.Room rm = Layout.ROOMS[i];
                boolean elite = rm == Layout.ELITE;
                v.cyl(0, rm.z(), rm.r(), 1, rm.h());
                v.dome(0, rm.z(), rm.r(), rm.r(), rm.h(), elite ? 11 : 9);
                if (i == 1 || elite) {                                   // alcoves laterales
                    for (int side : new int[]{-1, 1}) {
                        int ax = side * (rm.r() + (elite ? 4 : 4));
                        v.cyl(ax, rm.z(), elite ? 5 : 6, 1, 9);
                        v.dome(ax, rm.z(), elite ? 5 : 6, elite ? 5 : 6, 9, 5);
                    }
                }
                // lac de plateformes : le sol est creuse de mur a mur, aucun rebord ne permet de contourner
                if (plan0.roomKind(i).equals(Trials.JUMPROOM)) v.cyl(0, rm.z(), rm.r() + 0.4, -8, 0);
            }
            v.vault(0, Theme.GAL_Z0 - 2, Theme.GAL_Z1 + 1, 4, 1, 7, 4, false);
            v.cyl(0, Theme.ARENA_Z, Theme.ARENA_R, 1, Theme.ARENA_H);
            v.dome(0, Theme.ARENA_Z, Theme.ARENA_R, Theme.ARENA_R, Theme.ARENA_H, 15);
            v.vault(0, Theme.ARENA_Z + Theme.ARENA_R - 2, Theme.TREASURE_Z - 4, 2, 1, 5, 2, false);
            v.cyl(0, Theme.TREASURE_Z, Theme.TREASURE_R, 1, 9);
            v.dome(0, Theme.TREASURE_Z, Theme.TREASURE_R, Theme.TREASURE_R, 9, 5);
            th.shape(v);
            v.beginShell();
            TrialData.get(level.getServer()).removePrefix(t.id + "_" + o.getX() + "_" + o.getZ());
        }

        /** Fondations : le sol ne doit jamais flotter, meme pose en plein ciel. */
        private void foundation(int za, int zb) {
            for (int x = v.x0; x <= v.x1; x++) {
                for (int z = Math.max(v.z0, za); z <= Math.min(v.z1, zb); z++) {
                    if (!v.inside(x, 1, z) || v.inside(x, -1, z)) continue;
                    for (int y = -1; y >= -5; y--) {
                        if (c.air(x, y, z)) c.set(x, y, z, th.shell(x, y, z, Carve.Kind.WALL, true));
                    }
                }
            }
        }

        private void roomTrial(TrialData trials, Trials.Palette pal, Trials.Plan plan, String key, int i, int lvl, long seed) {
            Layout.Room rm = Layout.ROOMS[i];
            Trials.Stage st = plan.room(i);
            int diff = Math.min(3, st.diff() + bonus);
            String id = key + "_t" + (Trials.Plan.ROOM_STAGE[i] + 1);
            int g0 = rm.z1() + 2;
            int g1 = rm.z1() + 3;
            switch (st.kind()) {
                case TrialData.WAVES -> Trials.waves(c, level, trials, pal, id, rm.z(), g0, g1, diff, lvl);
                case TrialData.SIMON -> Trials.simon(c, level, trials, pal, id, rm.z(), g0, g1, diff, lvl);
                case TrialData.TARGETS -> Trials.targets(c, level, trials, pal, id, rm.z(), g0, g1, diff, lvl);
                case TrialData.ORDER -> Trials.order(c, level, trials, pal, id, rm.z(), rm.z0() - 10, g0, g1, diff, lvl, seed);
                case TrialData.ELITE -> Trials.elite(c, level, trials, pal, id, rm, th.eliteKey(), g0, g1, diff, lvl);
                case Trials.JUMPROOM -> Trials.jumpRoom(c, level, trials, pal, id, rm, diff, lvl, seed);
                default -> {
                }
            }
        }

        private void pitTrial(TrialData trials, Trials.Palette pal, Trials.Plan plan, String key, int j, int lvl, long seed) {
            Layout.Corridor cr = Layout.PITS[j];
            Trials.Stage st = plan.pit(j);
            if (!st.kind().equals(TrialData.JUMP)) return;
            int diff = Math.min(3, st.diff() + bonus);
            Trials.jump(c, level, trials, pal, key + "_t" + (Trials.Plan.PIT_STAGE[j] + 1), cr.z0() + 4, cr.z1(), diff, lvl, seed);
        }

        private void gameElements(Trials.Plan plan) {
            RandomSource r = level.getRandom();
            var mobs = t.mobs.get();
            c.set(0, 1, Theme.HALL_Z, ModBlocks.TELEPORTEUR.get());
            for (int i = 0; i < Layout.ROOMS.length; i++) {
                Layout.Room rm = Layout.ROOMS[i];
                // sans epreuve : deux generateurs de monstres comme un donjon classique
                if (plan.roomKind(i).equals(Trials.NONE)) {
                    DungeonBuilder.spawner(level, o.offset(-7, 1, rm.z() - 3), mobs.get(0), r);
                    DungeonBuilder.spawner(level, o.offset(7, 1, rm.z() + 4), mobs.get(mobs.size() > 1 ? 1 : 0), r);
                    DungeonBuilder.spawner(level, o.offset(-7, 1, rm.z() + 6), mobs.get(0), r);
                    DungeonBuilder.spawner(level, o.offset(7, 1, rm.z() - 5), mobs.get(mobs.size() > 1 ? 1 : 0), r);
                }
                // un coffre par salle : dans la salle, ou derriere le lac de plateformes
                BlockPos at = th.jumpRoom(i) ? o.offset(3, 1, rm.z1() + 6) : o.offset(rm.r() - 3, 1, rm.z() + (i % 2 == 0 ? 7 : -7));
                DungeonBuilder.chest(level, at, DungeonBuilder.roomLoot(t, r, false), r);
            }
            c.set(0, 1, Theme.ARENA_Z, ModBlocks.AUTEL_INVOCATION.get());
            var bossLoot = DungeonBuilder.roomLoot(t, r, true);
            var summonKey = com.mmorpg.registry.ModItems.get(t.bossKey);
            bossLoot.removeIf(s -> s.is(summonKey));
            for (int i = 0; i < bonus; i++) bossLoot.addAll(DungeonBuilder.roomLoot(t, r, false));
            DungeonBuilder.chest(level, o.offset(0, 1, Theme.TREASURE_Z + 3), bossLoot.subList(0, Math.min(27, bossLoot.size())), r);
        }

        private void registerZones() {
            if (!level.dimension().equals(InstanceManager.DIM)) {         // un exemplaire instancie n'est pas un point de passage : on y entre par la porte du donjon
                WaypointData data = WaypointData.get(level.getServer());
                data.removeAt(level.dimension(), tp);
                data.create(level.dimension(), tp, t.label, WaypointData.DUNGEON, t.level, new UUID(0, 0));
            }
            // annonces d'entree : le donjon, puis l'antre du boss (animation dediee)
            ZoneData zones = ZoneData.get(level.getServer());
            int ox = o.getX();
            int oz = o.getZ();
            String key = t.id + "_" + ox + "_" + oz;
            zones.add(new ZoneData.Zone(key, t.label, level.dimension(), ox - 33, oz - 2, ox + 33, oz + Layout.LENGTH,
                    true, true, false, ZoneData.Zone.STYLE_DUNGEON, th.lore() + "|Donjon · niveau " + t.level + (bonus == 1 ? " · héroïque" : bonus >= 2 ? " · mythique" : "")));
            zones.add(new ZoneData.Zone(key + "_boss", th.arenaName(), level.dimension(), ox - 23, oz + Theme.ARENA_Z - 23, ox + 23, oz + Theme.ARENA_Z + 23,
                    true, true, false, ZoneData.Zone.STYLE_BOSS, "Boss · niveau " + t.level));
        }

        /** Remplit de nouveau les coffres (remise a zero d'un exemplaire reutilise). */
        void refillChests() {
            RandomSource r = level.getRandom();
            for (int i = 0; i < Layout.ROOMS.length; i++) {
                Layout.Room rm = Layout.ROOMS[i];
                BlockPos at = th.jumpRoom(i) ? o.offset(3, 1, rm.z1() + 6) : o.offset(rm.r() - 3, 1, rm.z() + (i % 2 == 0 ? 7 : -7));
                DungeonBuilder.chest(level, at, DungeonBuilder.roomLoot(t, r, false), r);
            }
            var bossLoot = DungeonBuilder.roomLoot(t, r, true);
            var summonKey = com.mmorpg.registry.ModItems.get(t.bossKey);
            bossLoot.removeIf(s -> s.is(summonKey));
            for (int i = 0; i < bonus; i++) bossLoot.addAll(DungeonBuilder.roomLoot(t, r, false));
            DungeonBuilder.chest(level, o.offset(0, 1, Theme.TREASURE_Z + 3), bossLoot.subList(0, Math.min(27, bossLoot.size())), r);
        }
    }

    public static Job prepare(ServerLevel level, BlockPos origin, DungeonType t, int bonus) {
        return new Job(level, origin, t, bonus);
    }

    /** Remet des objets neufs dans les coffres d'un exemplaire (appele par la remise a zero). */
    public static void refill(ServerLevel level, BlockPos origin, DungeonType t, int bonus) {
        new Job(level, origin, t, bonus).refillChests();
    }

    /** Phrase d'ambiance d'un donjon (ecran de chargement, entree). */
    public static String loreOf(DungeonType t) {
        return theme(null, t).lore();
    }

    /** Couleur d'accent d'un donjon. */
    public static int colorOf(DungeonType t) {
        return switch (t) {
            case CRYPTE_GOBELINE -> 0xA8D060;
            case CATACOMBES -> 0xB890F0;
            case FORTERESSE_INFERNALE -> 0xFF8040;
            case SANCTUAIRE_GLACE -> 0x90E0FF;
            case CITADELLE_NEANT -> 0xC870FF;
        };
    }

    /** @param bonus difficulte : 0 normal, 1 heroique, 2 mythique (epreuves plus dures, monstres plus forts, meilleur butin) */
    public static BlockPos build(ServerLevel level, BlockPos origin, DungeonType t, int bonus) {
        Job job = prepare(level, origin, t, bonus);
        for (int i = 0; i < job.size(); i++) job.run(i);
        return job.tp;
    }

    // ------------------------------------------------------------------ utilitaires pour les ambiances

    static BlockState state(Object o) {
        if (o instanceof BlockState s) return s;
        if (o instanceof net.minecraft.world.level.block.Block b) return b.defaultBlockState();
        return Blocks.STONE.defaultBlockState();
    }
}
