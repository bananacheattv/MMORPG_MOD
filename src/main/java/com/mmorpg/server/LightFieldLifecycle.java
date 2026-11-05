package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Retrouve les sources déjà posées au chargement des chunks et reconstruit leurs relais. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class LightFieldLifecycle {
    private static final java.util.Queue<LevelChunk> QUEUE = new java.util.concurrent.ConcurrentLinkedQueue<>();
    @SubscribeEvent public static void loaded(ChunkEvent.Load e) {
        if (e.getChunk() instanceof LevelChunk c && c.getLevel() instanceof ServerLevel) QUEUE.add(c);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { QUEUE.clear(); }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        var c = QUEUE.poll(); if (c == null) return;
        var l = (ServerLevel)c.getLevel();
        if (!l.hasChunk(c.getPos().x(), c.getPos().z())) return;
        for (int i = 0; i < c.getSections().length; i++) {
            var section = c.getSections()[i];
            if (!section.maybeHas(s -> s.is(ModBlocks.BLOC_LUMIERE.get()))) continue;
            int y0 = c.getMinY() + i * 16;
            for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                var s = section.getBlockState(x, y, z);
                if (s.is(ModBlocks.BLOC_LUMIERE.get()))
                    LightNetwork.changed(l, new BlockPos(c.getPos().getMinBlockX() + x, y0 + y, c.getPos().getMinBlockZ() + z));
            }
        }
    }
}
