package com.mmorpg.network;

import com.mmorpg.MMORPG;
import com.mmorpg.perf.Perf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Sauvegarde automatique asynchrone des profils partages (reseau). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class ProfileAutosave {
    private ProfileAutosave() {
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 20 == 0) Perf.time("autosave_profils", () -> NetworkPlayers.autosaveTick(e.getServer()));
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent e) {
        NetworkPlayers.drainSaves();
    }
}
