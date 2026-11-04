package com.mmorpg.perf;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.SkillFxEntity;
import com.mmorpg.registry.ModEntities;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Controle serveur (-PperfChecks) : la regle d'orphelin vise un effet vieilli, jamais une entite normale ; la passe complete s'execute sans erreur. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevPerfChecks {
    private DevPerfChecks() {
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent e) {
        if (!Boolean.getBoolean("mmorpg.checkPerf")) return;
        var server = e.getServer();
        try {
            var level = server.overworld();
            var stand = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
            stand.tickCount = 100000;
            SkillFxEntity fx = ModEntities.SKILL_FX.get().create(level, EntitySpawnReason.COMMAND);
            fx.tickCount = fx.life() + 1000;
            SkillFxEntity young = ModEntities.SKILL_FX.get().create(level, EntitySpawnReason.COMMAND);
            young.tickCount = 1;
            if (OrphanSweeper.orphan(stand)) throw new AssertionError("perf: entite normale visee");
            if (!OrphanSweeper.orphan(fx)) throw new AssertionError("perf: effet vieilli non vise");
            if (OrphanSweeper.orphan(young)) throw new AssertionError("perf: effet recent vise");
            OrphanSweeper.sweep(server);
            Perf.record("test", 1_000_000);
            if (Perf.sections().get("test").calls.get() != 1) throw new AssertionError("perf: sections");
            MMORPG.LOGGER.info("PERF_PASS");
        } catch (Throwable t) {
            MMORPG.LOGGER.error("PERF_FAIL", t);
        }
        server.execute(() -> server.halt(false));
    }
}
