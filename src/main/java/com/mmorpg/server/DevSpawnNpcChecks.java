package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.hub.SpawnBuilder;
import com.mmorpg.quest.QuestNpcPlan;
import com.mmorpg.quest.SpawnDiscovery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

/** Verification en jeu (opt-in, -Dmmorpg.checkSpawnNpcs=true) : PNJ de quetes du spawn poses, espaces, sur le sol ; visite de chacun. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevSpawnNpcChecks {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkSpawnNpcs") || !(event.getEntity() instanceof ServerPlayer p)) return;
        var server = p.level().getServer();
        var level = server.getLevel(SpawnBuilder.DIM);
        var data = com.mmorpg.hub.SpawnData.get(server);
        if (level == null || !data.exists()) { MMORPG.LOGGER.error("[SPAWN NPC CHECKS] FAIL spawn absent"); return; }
        Scheduler.later(p.level(), 400, () -> MMORPG.LOGGER.info("[SPAWN NPC CHECKS] teleportation vers le spawn : {}", p.teleportTo(level, data.arrival().x, data.arrival().y, data.arrival().z, Set.of(), 0, 0, true)));
        Scheduler.later(p.level(), 1000, () -> run(p, level));
    }

    private static void run(ServerPlayer p, net.minecraft.server.level.ServerLevel level) {
        // pas d appel direct a install : on verifie le rattrapage automatique des donneurs lointains
        var server = level.getServer();
        // annuaire des PNJ (les PNJ lointains ne sont pas charges) : donneurs de quetes du spawn
        var entries = new ArrayList<net.minecraft.nbt.CompoundTag>();
        for (var t : com.mmorpg.world.NpcDirectory.get(server).snapshot())
            if (t instanceof net.minecraft.nbt.CompoundTag n && n.getIntOr("role", -1) == 0 && n.getStringOr("dimension", "").equals(SpawnBuilder.DIM.identifier().toString())) entries.add(n);
        var fails = new ArrayList<String>();
        var planIds = new HashSet<String>();
        for (var g : QuestNpcPlan.GIVERS) planIds.add(g.uuid().toString());
        entries.removeIf(n -> !planIds.contains(n.getStringOr("id", ""))); // ignore les PNJ de test laisses par d'autres verifications
        for (int[] c : new int[][]{{-40, 62}, {-40, 50}, {-20, 62}, {-60, 62}, {0, 64}}) {
            var sb = new StringBuilder();
            for (int y = 130; y >= 70; y--) { var b = level.getBlockState(new BlockPos(c[0], y, c[1])); if (!b.isAir()) { sb.append(y).append(':').append(b.getBlock().getDescriptionId().replace("block.minecraft.", "")).append(' '); if (sb.length() > 120) break; } }
            MMORPG.LOGGER.info("[SPAWN NPC CHECKS] colonne {} {} loaded={} : {}", c[0], c[1], level.hasChunkAt(new BlockPos(c[0], 96, c[1])), sb);
        }
        var plan = new HashSet<String>();
        for (var g : QuestNpcPlan.GIVERS) plan.add(g.uuid().toString());
        long found = entries.stream().filter(n -> plan.contains(n.getStringOr("id", ""))).count();
        if (found < plan.size()) fails.add("seulement " + found + " donneurs prevus sur " + plan.size());
        double min = 1e9;
        for (var a : entries) {
            boolean first = a.getStringOr("id", "").equals(QuestNpcPlan.uuid(0).toString());
            var pa = BlockPos.of(a.getLongOr("pos", 0));
            MMORPG.LOGGER.info("[SPAWN NPC CHECKS] {} @ {}", a.getStringOr("name", "?"), pa);
            if (!first) for (var b : entries) if (a != b && !b.getStringOr("id", "").equals(QuestNpcPlan.uuid(0).toString())) min = Math.min(min, Math.sqrt(pa.distSqr(BlockPos.of(b.getLongOr("pos", 0)))));
        }
        if (entries.size() > 1 && min < QuestNpcPlan.MIN_DISTANCE - 3) fails.add("distance minimale " + Math.round(min));
        MMORPG.LOGGER.info("[SPAWN NPC CHECKS] {} : {} donneurs, distance minimale {}", fails.isEmpty() ? "PASS" : "FAIL " + fails, entries.size(), Math.round(min));
        // visite : le joueur se place devant chaque PNJ (charge la zone), verifie sol et presence, puis une capture est prise cote client
        int[] i = {0};
        Runnable[] step = new Runnable[1];
        step[0] = () -> {
            if (i[0] >= entries.size()) { MMORPG.LOGGER.info("[SPAWN NPC CHECKS] VISITES TERMINEES"); return; }
            var n = entries.get(i[0]++);
            var at = BlockPos.of(n.getLongOr("pos", 0));
            if (!p.teleportTo(level, at.getX() + .5, at.getY(), at.getZ() + 3.5, Set.of(), 180, 0, true)) MMORPG.LOGGER.info("[SPAWN NPC CHECKS] teleportation refusee (passager={} vivant={} dim={})", p.isPassenger(), p.isAlive(), p.level().dimension().identifier());
            Scheduler.later(level, 50, () -> {
                var npc = level.getEntity(UUID.fromString(n.getStringOr("id", "")));
                boolean ok = npc instanceof NpcEntity && level.getBlockState(at.below()).isSolidRender();
                MMORPG.LOGGER.info("[SPAWN NPC CHECKS] VISITE {} {}", n.getStringOr("name", "?"), ok ? "OK (present, sur le sol)" : "ECHEC (absent ou dans le vide)");
            });
            Scheduler.later(level, 110, step[0]);
        };
        step[0].run();
    }
}
