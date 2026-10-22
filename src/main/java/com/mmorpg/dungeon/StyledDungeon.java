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
 * Donjons de boss stylises : salle d'entree a coupole, deux salles de combat rondes, un couloir a fosse et pont, la galerie des
 * gardiens, une arene de boss monumentale et une alcove de tresor. Chaque ambiance (voir {@link Theme}) fournit sa coque,
 * ses motifs de sol, ses lumieres et ses elements de decor ; ici on assemble le plan et on pose les elements de jeu
 * (teleporteur, generateurs de monstres, coffres, autel d'invocation, zones d'annonce).
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
        BlockPos o = origin.below();
        HubCtx c = new HubCtx(level, o);
        Theme th = theme(c, t);

        Carve v = new Carve(-30, 30, -11, 36, -4, 194);
        v.cyl(0, Theme.HALL_Z, Theme.HALL_R, 1, Theme.HALL_H);
        v.dome(0, Theme.HALL_Z, Theme.HALL_R, Theme.HALL_R, Theme.HALL_H, 11);
        v.vault(0, 21, 38, 3, 1, 5, 3, false);
        v.cyl(0, Theme.A_Z, Theme.A_R, 1, Theme.A_H);
        v.dome(0, Theme.A_Z, Theme.A_R, Theme.A_R, Theme.A_H, 9);
        v.vault(0, Theme.PIT_Z0 - 1, Theme.PIT_Z1 + 1, Theme.PIT_HALF, 1, 6, 4, false);
        v.box(-Theme.PIT_HALF, -Theme.PIT_DEPTH, Theme.PIT_Z0, Theme.PIT_HALF, -2, Theme.PIT_Z1);          // fosse sous le pont
        v.box(-Theme.PIT_HALF, -1, Theme.PIT_Z0, -2, 0, Theme.PIT_Z1);
        v.box(2, -1, Theme.PIT_Z0, Theme.PIT_HALF, 0, Theme.PIT_Z1);
        v.cyl(0, Theme.B_Z, Theme.B_R, 1, Theme.B_H);
        v.dome(0, Theme.B_Z, Theme.B_R, Theme.B_R, Theme.B_H, 9);
        for (int side : new int[]{-1, 1}) {
            v.cyl(side * 18, Theme.B_Z, 6, 1, 9);
            v.dome(side * 18, Theme.B_Z, 6, 6, 9, 5);
        }
        v.vault(0, Theme.GAL_Z0 - 2, Theme.GAL_Z1 + 1, 4, 1, 7, 4, false);
        v.cyl(0, Theme.ARENA_Z, Theme.ARENA_R, 1, Theme.ARENA_H);
        v.dome(0, Theme.ARENA_Z, Theme.ARENA_R, Theme.ARENA_R, Theme.ARENA_H, 15);
        v.vault(0, Theme.ARENA_Z + Theme.ARENA_R - 2, Theme.TREASURE_Z - 4, 2, 1, 5, 2, false);
        v.cyl(0, Theme.TREASURE_Z, Theme.TREASURE_R, 1, 9);
        v.dome(0, Theme.TREASURE_Z, Theme.TREASURE_R, Theme.TREASURE_R, 9, 5);
        th.shape(v);
        v.shell(c, (x, y, z, k, back) -> {
            if (!back && k != Carve.Kind.FLOOR && th.rnd(x, y, z, 77) < th.lightChance()) return th.light(x, y, z);
            return th.shell(x, y, z, k, back);
        });

        // fondations : le sol ne doit jamais flotter, meme pose en plein ciel
        for (int x = v.x0; x <= v.x1; x++) {
            for (int z = v.z0; z <= v.z1; z++) {
                if (!v.inside(x, 1, z) || v.inside(x, -1, z)) continue;
                for (int y = -1; y >= -5; y--) {
                    if (c.air(x, y, z)) c.set(x, y, z, th.shell(x, y, z, Carve.Kind.WALL, true));
                }
            }
        }

        th.hall();
        th.roomA();
        th.pit();
        th.roomB();
        th.gallery();
        th.arena();
        th.treasure();

        // elements de jeu
        RandomSource r = level.getRandom();
        var mobs = t.mobs.get();
        BlockPos tp = o.offset(0, 1, Theme.HALL_Z);
        c.set(0, 1, Theme.HALL_Z, ModBlocks.TELEPORTEUR.get());
        DungeonBuilder.spawner(level, o.offset(-7, 1, Theme.A_Z - 3), mobs.get(0), r);
        DungeonBuilder.spawner(level, o.offset(7, 1, Theme.A_Z + 4), mobs.get(mobs.size() > 1 ? 1 : 0), r);
        DungeonBuilder.chest(level, o.offset(11, 1, Theme.A_Z + 9), DungeonBuilder.roomLoot(t, r, false), r);
        DungeonBuilder.spawner(level, o.offset(-5, 1, Theme.B_Z - 6), mobs.get(0), r);
        DungeonBuilder.spawner(level, o.offset(5, 1, Theme.B_Z + 6), mobs.get(mobs.size() > 1 ? 1 : 0), r);
        DungeonBuilder.spawner(level, o.offset(-18, 1, Theme.B_Z), mobs.get(0), r);
        DungeonBuilder.chest(level, o.offset(18, 1, Theme.B_Z), DungeonBuilder.roomLoot(t, r, false), r);
        c.set(0, 1, Theme.ARENA_Z, ModBlocks.AUTEL_INVOCATION.get());
        DungeonBuilder.chest(level, o.offset(0, 1, Theme.TREASURE_Z + 3), DungeonBuilder.roomLoot(t, r, true), r);

        WaypointData data = WaypointData.get(level.getServer());
        data.removeAt(level.dimension(), tp);
        data.create(level.dimension(), tp, t.label, WaypointData.DUNGEON, t.level, new UUID(0, 0));

        // annonces d'entree : le donjon, puis l'antre du boss (animation dediee)
        ZoneData zones = ZoneData.get(level.getServer());
        int ox = o.getX();
        int oz = o.getZ();
        String key = t.id + "_" + ox + "_" + oz;
        zones.add(new ZoneData.Zone(key, t.label, level.dimension(), ox - 27, oz - 2, ox + 27, oz + 192,
                true, true, false, ZoneData.Zone.STYLE_ZONE, "Donjon · niveau " + t.level));
        zones.add(new ZoneData.Zone(key + "_boss", th.arenaName(), level.dimension(), ox - 23, oz + Theme.ARENA_Z - 23, ox + 23, oz + Theme.ARENA_Z + 23,
                true, true, false, ZoneData.Zone.STYLE_BOSS, "Boss · niveau " + t.level));
        return tp;
    }

    // ------------------------------------------------------------------ utilitaires pour les ambiances

    static BlockState state(Object o) {
        if (o instanceof BlockState s) return s;
        if (o instanceof net.minecraft.world.level.block.Block b) return b.defaultBlockState();
        return Blocks.STONE.defaultBlockState();
    }
}
