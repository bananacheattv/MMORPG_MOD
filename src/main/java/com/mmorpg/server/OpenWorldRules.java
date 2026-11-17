package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/** Monde ouvert (Overworld importe, spawn 0 84 0) : plus de mobs vanilla, et aucun de nos mobs pres du spawn. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class OpenWorldRules {
    private OpenWorldRules() { }

    public static boolean nearSpawn(double x, double z) {
        return com.mmorpg.config.ConfigManager.general().openWorld().isSafe(x, z);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpawn(FinalizeSpawnEvent e) {
        Mob mob = e.getEntity();
        if (!mob.level().dimension().equals(Level.OVERWORLD)) return;
        EntitySpawnReason r = e.getSpawnType();
        if (r == EntitySpawnReason.COMMAND || r == EntitySpawnReason.SPAWN_ITEM_USE) return;
        if (mob instanceof com.mmorpg.entity.RpgMob) {
            if (nearSpawn(mob.getX(), mob.getZ())) e.setSpawnCancelled(true);
        } else if (r == EntitySpawnReason.NATURAL || r == EntitySpawnReason.CHUNK_GENERATION || r == EntitySpawnReason.PATROL
                || r == EntitySpawnReason.REINFORCEMENT || r == EntitySpawnReason.JOCKEY) {
            e.setSpawnCancelled(true);
        }
    }
}
