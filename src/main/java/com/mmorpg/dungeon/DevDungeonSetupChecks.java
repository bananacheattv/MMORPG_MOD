package com.mmorpg.dungeon;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.LootTables;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Couverture serveur du donjon de setup : modele enregistre, copie dans un exemplaire, coffres aleatoires issus des tables. */
public final class DevDungeonSetupChecks {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError("dungeon setup: " + message); }

    private static int chestItems(net.minecraft.server.level.ServerLevel level, BlockPos at) {
        if (!(level.getBlockEntity(at) instanceof ChestBlockEntity c)) return -1;
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) if (!c.getItem(i).isEmpty()) n++;
        return n;
    }

    public static void run(ServerPlayer p) throws Exception {
        var server = p.level().getServer();
        var level = server.getLevel(InstanceManager.DIM);
        check(level != null, "dungeon dimension");
        DungeonType t = DungeonType.CRYPTE_GOBELINE;
        var rooms = LootTables.dungeonRooms(t.id);
        check(LootTables.isDungeonTable(rooms) && !ConfigManager.loot().table(rooms).isEmpty() && !ConfigManager.loot().table(LootTables.dungeonTreasure(t.id)).isEmpty(), "default chest tables exist");
        check(InstanceManager.validPool("crypte_gobeline:3") && InstanceManager.isSetup("crypte_gobeline:3") && !InstanceManager.isSetup("crypte_gobeline:2"), "setup pool");
        check(!DungeonLoot.room(t, level.getRandom()).isEmpty() && !DungeonLoot.treasure(t, level.getRandom(), 1).isEmpty(), "random loot rolled from tables");
        boolean had = DungeonTemplate.exists(t);
        check(!had, "no template before the check");
        BlockPos setup = new BlockPos(3000, 101, InstanceManager.Z_BASE + 40 * InstanceManager.Z_STEP);
        BlockPos copy = new BlockPos(3400, 101, InstanceManager.Z_BASE + 40 * InstanceManager.Z_STEP);
        try {
            var job = StyledDungeon.prepareSetup(level, setup, t);
            for (int i = 0; i < job.size(); i++) job.run(i);
            BlockPos o = setup.below();
            BlockPos mark = o.offset(0, 2, 20), room0 = o.offset(Layout.ROOMS[0].r() - 3, 1, Layout.ROOMS[0].z() + 7), treasure = o.offset(0, 1, Theme.TREASURE_Z + 3);
            check(chestItems(level, room0) > 0 && chestItems(level, treasure) > 0, "setup chests filled from tables");
            level.setBlockAndUpdate(mark, Blocks.GOLD_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(room0, Blocks.AIR.defaultBlockState());                          // un coffre supprime dans le setup
            int saved = DungeonTemplate.capture(level, o, t);
            check(DungeonTemplate.exists(t) && saved > 0, "template saved (" + saved + " chests/spawners)");
            // le modele est copie dans un exemplaire construit ensuite
            var inst = StyledDungeon.prepare(level, copy, t, 0);
            for (int i = 0; i < inst.size(); i++) inst.run(i);
            BlockPos co = copy.below();
            check(level.getBlockState(co.offset(0, 2, 20)).is(Blocks.GOLD_BLOCK), "edited block copied into new instance");
            check(level.getBlockState(co.offset(Layout.ROOMS[0].r() - 3, 1, Layout.ROOMS[0].z() + 7)).isAir(), "removed chest stays removed");
            BlockPos ctreasure = co.offset(0, 1, Theme.TREASURE_Z + 3);
            check(chestItems(level, ctreasure) > 0, "instance chest filled randomly");
            // butin configure : table vide de poids -> coffre du tresor sans objet aleatoire autre que la table
            var saveTable = new java.util.ArrayList<>(ConfigManager.loot().table(LootTables.dungeonTreasure(t.id)));
            ConfigManager.saveLootTable(LootTables.dungeonTreasure(t.id), java.util.List.of(new LootTables.Entry("mmorpg:piece_or", 7, 7, 1)));
            StyledDungeon.refill(level, copy, t, 0);
            var chest = (ChestBlockEntity) level.getBlockEntity(ctreasure);
            int total = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) total += chest.getItem(i).getCount();
            check(total == 7 * DungeonLoot.TREASURE_DRAWS, "configured table drives chest content (" + total + ")");
            ConfigManager.saveLootTable(LootTables.dungeonTreasure(t.id), saveTable);
            check(DungeonTemplate.delete(t) && !DungeonTemplate.exists(t), "template deleted");
            MMORPG.LOGGER.info("[CODEX CHECKS] DUNGEON_SETUP PASS: setup pool, template save/copy, removed chest, random configurable chest tables");
        } finally {
            DungeonTemplate.delete(t);
            TrialData.get(server).removeInside(level.dimension(), 2900, InstanceManager.Z_BASE, 3600, InstanceManager.Z_BASE + 60 * InstanceManager.Z_STEP);
            com.mmorpg.zone.ZoneData.get(server).removeOverlapping(level.dimension(), t.id + "_", 2900, InstanceManager.Z_BASE, 3600, InstanceManager.Z_BASE + 60 * InstanceManager.Z_STEP);
        }
    }
}
