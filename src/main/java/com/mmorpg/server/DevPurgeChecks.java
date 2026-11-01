package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Verification serveur (opt-in, -Dmmorpg.checkPurge=true) : /amenagement purgepnj. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevPurgeChecks {
    @SubscribeEvent public static void started(ServerStartedEvent e) {
        if (!Boolean.getBoolean("mmorpg.checkPurge")) return;
        var server = e.getServer();
        try {
            var level = server.overworld();
            level.getChunkAt(new BlockPos(80, 100, 80));
            var a = com.mmorpg.world.DungeonBuilder.spawnNpc(level, new BlockPos(81, 100, 80), NpcEntity.Role.MARCHAND, "Purge A", 0, 0);
            var b = com.mmorpg.world.DungeonBuilder.spawnNpc(level, new BlockPos(82, 100, 80), NpcEntity.Role.QUETES, "Purge B", 0, 0);
            var dir = com.mmorpg.world.NpcDirectory.get(server);
            if (dir.entry(a.getUUID().toString()) == null) throw new AssertionError("annuaire sans PNJ avant purge");
            int n = NpcPurge.purge(server);
            if (n < 2) throw new AssertionError("purge a retire " + n);
            if (level.getEntity(a.getUUID()) != null || level.getEntity(b.getUUID()) != null) throw new AssertionError("entites encore presentes");
            if (dir.entry(a.getUUID().toString()) != null || dir.snapshot().size() != 0) throw new AssertionError("annuaire non vide");
            if (!NpcPurge.active(server)) throw new AssertionError("drapeau absent");
            NpcPurge.allow(server);
            if (NpcPurge.active(server)) throw new AssertionError("drapeau persistant");
            MMORPG.LOGGER.info("[PURGE CHECKS] PASS ({} PNJ)", n);
        } catch (Throwable t) { MMORPG.LOGGER.error("[PURGE CHECKS] FAIL", t); }
        server.halt(false);
    }
}
