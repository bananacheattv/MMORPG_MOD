package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/** Hors donjons : aucun spawn automatique (vanilla ou serveur) ; les mobs viennent des zones de chasse (/zone chasse). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class OpenWorldRules {
    private OpenWorldRules() { }

    /** Rayon sans aucun de nos mobs autour du spawn. */
    public static final int SAFE_RADIUS = 200;

    public static boolean nearSpawn(double x, double z) {
        return x * x + z * z <= (double) SAFE_RADIUS * SAFE_RADIUS;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpawn(FinalizeSpawnEvent e) {
        Mob mob = e.getEntity();
        if (mob.level().dimension().equals(com.mmorpg.dungeon.InstanceManager.DIM)) return;   // donjons : inchanges
        EntitySpawnReason r = e.getSpawnType();
        if (r == EntitySpawnReason.NATURAL || r == EntitySpawnReason.CHUNK_GENERATION || r == EntitySpawnReason.PATROL
                || r == EntitySpawnReason.REINFORCEMENT || r == EntitySpawnReason.JOCKEY || r == EntitySpawnReason.SPAWNER
                || r == EntitySpawnReason.TRIAL_SPAWNER) {
            e.setSpawnCancelled(true);   // plus aucun spawn automatique : seules les zones de chasse posees a la main (/zone chasse) font apparaitre des mobs
        }
    }
}
