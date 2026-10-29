package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.job.Job;
import com.mmorpg.job.JobSystem;
import com.mmorpg.mine.MineOreBlock;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Metiers : vitesse de minage (la peche est geree par com.mmorpg.fishing.FishingEvents). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class JobEvents {
    private JobEvents() {
    }

    @SubscribeEvent
    public static void miningSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getState().getBlock() instanceof MineOreBlock)) return;
        event.setNewSpeed(event.getNewSpeed() * Job.miningSpeed(JobSystem.level(p, Job.MINEUR)));
    }
}
