package com.mmorpg.perf;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.MagicProjectile;
import com.mmorpg.entity.MascotEntity;
import com.mmorpg.entity.PetEntity;
import com.mmorpg.entity.SkillFxEntity;
import com.mmorpg.entity.SpellProjectile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Nettoyage periodique des entites orphelines : familiers et mascottes sans maitre, effets de competence et projectiles
 * restes trop longtemps. Les entites normales ne sont jamais touchees.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class OrphanSweeper {
    /** Intervalle entre deux passes (5 minutes). */
    public static final int INTERVAL_TICKS = 6000;
    private static final int PROJECTILE_MAX_AGE = 1200;
    private static final int FX_GRACE = 200;

    private OrphanSweeper() {
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL_TICKS != INTERVAL_TICKS / 2) return;
        Perf.time("orphelins", () -> sweep(server));
    }

    /** @return le nombre d'entites retirees */
    public static int sweep(MinecraftServer server) {
        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> doomed = new ArrayList<>();
            for (Entity e : level.getAllEntities()) {
                if (orphan(e)) doomed.add(e);
            }
            for (Entity e : doomed) e.discard();
            removed += doomed.size();
        }
        if (removed > 0) {
            Perf.ORPHANS_REMOVED.addAndGet(removed);
            MMORPG.LOGGER.info("[MMORPG] Nettoyage : {} entite(s) orpheline(s) retiree(s)", removed);
        }
        return removed;
    }

    static boolean orphan(Entity e) {
        if (e.isRemoved()) return false;
        if (e instanceof PetEntity pet) return pet.tickCount > 100 && (pet.owner() == null || !pet.owner().isAlive());
        if (e instanceof MascotEntity m) return m.tickCount > 100 && (m.ownerId() == -1 || !(m.level().getEntity(m.ownerId()) instanceof net.minecraft.server.level.ServerPlayer));
        if (e instanceof SkillFxEntity fx) return fx.tickCount > fx.life() + FX_GRACE;
        if (e instanceof SpellProjectile || e instanceof MagicProjectile) return e.tickCount > PROJECTILE_MAX_AGE;
        return false;
    }
}
