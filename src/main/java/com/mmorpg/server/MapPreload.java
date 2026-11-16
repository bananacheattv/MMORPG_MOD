package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Pre-charge (et pre-genere) la carte autour du spawn de l'Overworld au demarrage : 1000 x 1000 blocs par defaut
 * (-Dmmorpg.preloadRadius=N en chunks, 0 = desactive). Les chunks sont charges un lot a la fois, sans bloquer le tick,
 * puis relaches : ils sont alors sur disque et se rechargent vite pour tous les joueurs.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class MapPreload {
    private static final int RADIUS = Integer.getInteger("mmorpg.preloadRadius", 32);
    private static final int IN_FLIGHT = 24, DELAY_TICKS = 1200;
    private static final ArrayDeque<ChunkPos> TODO = new ArrayDeque<>();
    private static final List<ChunkPos> LOADING = new ArrayList<>();
    private static ServerLevel level;
    private static int total;
    private static long started, tickStart;
    private static int waited;

    private MapPreload() {
    }

    @SubscribeEvent
    public static void start(ServerStartedEvent e) {
        TODO.clear();
        LOADING.clear();
        if (RADIUS <= 0) return;
        level = e.getServer().overworld();
        var sp = level.getRespawnData().pos();
        int cx = sp.getX() >> 4, cz = sp.getZ() >> 4;
        for (int r = 0; r <= RADIUS; r++)
            for (int x = -r; x <= r; x++)
                for (int z = -r; z <= r; z++)
                    if (Math.max(Math.abs(x), Math.abs(z)) == r) TODO.add(new ChunkPos(cx + x, cz + z));
        total = TODO.size();
        started = System.currentTimeMillis();
        waited = 0;
        MMORPG.LOGGER.info("[MMORPG] Pre-chargement de la carte du spawn : {} chunks ({} x {} blocs)", total, (RADIUS * 2 + 1) * 16, (RADIUS * 2 + 1) * 16);
    }

    @SubscribeEvent
    public static void pre(ServerTickEvent.Pre e) {
        tickStart = System.nanoTime();
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        if (level == null) return;
        long dt = System.nanoTime() - tickStart;
        if (++waited < DELAY_TICKS) return;
        LOADING.removeIf(p -> {
            if (level.getChunkSource().getChunkNow(p.x(), p.z()) == null) return false;
            level.setChunkForced(p.x(), p.z(), false);
            return true;
        });
        // ne charge rien de plus quand le serveur est deja en retard (construction des mines, joueurs...)
        int budget = dt < 40_000_000L ? IN_FLIGHT : 4;
        while (budget-- > 0 && LOADING.size() < IN_FLIGHT && !TODO.isEmpty()) {
            ChunkPos p = TODO.poll();
            level.setChunkForced(p.x(), p.z(), true);
            LOADING.add(p);
        }
        if (waited % 600 == 0) MMORPG.LOGGER.info("[MMORPG] Pre-chargement : {} / {} chunks", total - TODO.size() - LOADING.size(), total);
        if (TODO.isEmpty() && LOADING.isEmpty()) {
            MMORPG.LOGGER.info("[MMORPG] Carte du spawn pre-chargee : {} chunks en {} s", total, (System.currentTimeMillis() - started) / 1000);
            level = null;
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent e) {
        TODO.clear();
        LOADING.clear();
        level = null;
    }
}
