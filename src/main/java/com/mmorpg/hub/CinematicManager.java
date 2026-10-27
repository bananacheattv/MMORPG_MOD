package com.mmorpg.hub;

import com.mmorpg.MMORPG;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
 * Cote serveur de la cinematique d'arrivee : amene le joueur sur la place d'apparition du hub (ses chunks sont ainsi charges),
 * le bloque pendant la scene (aucun degat, aucun deplacement), envoie la definition au client qui joue la camera, puis le libere
 * quand le client termine ou saute la scene. Une echeance de securite libere le joueur si le client ne repond jamais.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class CinematicManager {
    private record Lock(long until, Vec3 pos) {
    }

    private static final Map<UUID, Lock> LOCKED = new HashMap<>();

    private CinematicManager() {
    }

    public static boolean isLocked(Player p) {
        return LOCKED.containsKey(p.getUUID());
    }

    /** Lance la cinematique ; {@code first} : premiere arrivee (marque la cinematique comme vue a la fin). Faux si le hub n'existe pas. */
    public static boolean start(ServerPlayer p, boolean first) {
        HubData hub = HubData.get(p.level().getServer());
        ServerLevel level = hub.level(p.level().getServer());
        if (!hub.exists() || level == null) return false;
        if (ClassTrial.active(p)) ClassTrial.end(p, false, null);
        if (isLocked(p)) return true;
        BlockPos hubOrigin = hub.origin(HubManager.assignHub(p));
        Vec3 spawn = HubLayout.center(hubOrigin, HubLayout.SPAWN);
        p.stopRiding();
        p.teleportTo(level, spawn.x, spawn.y, spawn.z, Set.of(), 180F, 6F, true);
        String json = CinematicConfig.shortened(CinematicConfig.load());
        double seconds = CinematicConfig.totalSeconds(json);
        LOCKED.put(p.getUUID(), new Lock(p.level().getGameTime() + (long) (seconds * 20) + 400, spawn));
        CompoundTag t = new CompoundTag();
        BlockPos o = hubOrigin;
        t.putLong("origin", o.asLong());
        t.putString("json", json);
        t.putBoolean("first", first);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.CINEMATIC, t));
        return true;
    }

    /** Fin de la scene (terminee ou sautee). */
    public static void finish(ServerPlayer p) {
        Lock lock = LOCKED.remove(p.getUUID());
        if (lock == null) return;
        MMORPG.LOGGER.info("[MMORPG] Cinematique : fin cote serveur pour {}", p.getName().getString());
        HubPlayerData hd = ClassTrial.data(p);
        boolean wasFirst = !hd.cinematicSeen;
        hd.cinematicSeen = true;
        p.teleportTo(p.level(), lock.pos.x, lock.pos.y, lock.pos.z, Set.of(), 180F, 6F, true);
        if (wasFirst && !RpgPlayers.get(p).playerClass.isPlayable()) {
            p.sendOverlayMessage(Component.literal("Parlez aux Maîtres de classe : essayez, puis choisissez votre voie.").withColor(0xFFE6A0));
            Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.HUB_TUTORIAL,new CompoundTag()));
        }
    }

    /** Joueurs qui viennent d'arriver dans le hub : la cinematique demarre au prochain controle si elle n'a jamais ete vue. */
    private static final Set<UUID> ARRIVED = new java.util.HashSet<>();

    public static void arrivedAtHub(ServerPlayer p) {
        ARRIVED.add(p.getUUID());
        HubManager.healOnce(p.level(), p.getX());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getTo().equals(HubManager.HUB_DIM)) arrivedAtHub(p);
    }

    @SubscribeEvent
    public static void onLoginInHub(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && p.level().dimension().equals(HubManager.HUB_DIM)) arrivedAtHub(p);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 5 != 0) return;
        if (!ARRIVED.isEmpty() && ARRIVED.remove(p.getUUID())) {
            HubPlayerData hd = ClassTrial.data(p);
            if (!hd.cinematicSeen && !hd.inTrial && !LOCKED.containsKey(p.getUUID()) && com.mmorpg.config.ConfigManager.general().hubCinematic) start(p, true);
        }
        Lock lock = LOCKED.get(p.getUUID());
        if (lock == null) return;
        if (p.level().getGameTime() > lock.until) {
            finish(p);
            return;
        }
        if (p.position().distanceToSqr(lock.pos) > 2.25) {
            p.teleportTo(p.level(), lock.pos.x, lock.pos.y, lock.pos.z, Set.of(), 180F, 6F, true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LOCKED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        LOCKED.remove(event.getEntity().getUUID());
    }
}
