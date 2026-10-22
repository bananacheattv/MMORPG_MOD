package com.mmorpg.dungeon;

import com.mmorpg.MMORPG;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.zone.ZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cinematique d'apparition d'un boss dans l'antre d'un donjon : les joueurs proches sont figes et invulnerables pendant la scene
 * (le client pilote la camera, voir {@code BossRevealClient}) ; le rituel de l'autel dure 3 s, puis le boss surgit.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class BossReveal {
    /** Duree totale en ticks : 3 s de rituel + 3,2 s d'apparition. */
    public static final int TICKS = 124;
    private record Lock(long until, Vec3 pos) {
    }

    private static final Map<UUID, Lock> LOCKED = new HashMap<>();

    private BossReveal() {
    }

    public static boolean isLocked(Player p) {
        return LOCKED.containsKey(p.getUUID());
    }

    /** Lance la scene pour les joueurs proches de l'autel si celui-ci se trouve dans l'antre d'un donjon. */
    public static void play(ServerLevel level, BlockPos altar) {
        var zone = ZoneManager.zoneAt(level, altar);
        if (zone == null || !zone.boss()) return;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.distanceToSqr(altar.getX() + 0.5, altar.getY(), altar.getZ() + 0.5) > 40 * 40) continue;
            p.stopRiding();
            LOCKED.put(p.getUUID(), new Lock(level.getGameTime() + TICKS + 10, p.position()));
            Net.toPlayer(p, new Payloads.BossReveal(altar.getX(), altar.getY(), altar.getZ(), TICKS));
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Lock lock = LOCKED.get(p.getUUID());
        if (lock == null) return;
        if (p.level().getGameTime() > lock.until) {
            LOCKED.remove(p.getUUID());
            return;
        }
        if (p.position().distanceToSqr(lock.pos) > 1.5) {
            p.teleportTo(p.level(), lock.pos.x, lock.pos.y, lock.pos.z, Set.of(), p.getYRot(), p.getXRot(), true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LOCKED.remove(event.getEntity().getUUID());
    }
}
