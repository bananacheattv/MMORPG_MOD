package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Invincibilite pendant les chargements (arrivee en plaine, donjon, mine, monde ouvert, spawn, changement de serveur).
 * Le bouclier se leve quand le client a fini de charger (paquet « joueur charge » vanilla), que les chunks autour sont prets,
 * puis encore {@link #MARGIN} ticks. Un plafond {@link #MAX} le retire de toute facon (jamais d'invulnerabilite permanente) ;
 * il disparait aussi a la deconnexion, a la mort et au changement de joueur. Les degats et le ciblage sont bloques via
 * {@link SafeSpawn#safe}, la faim, la noyade et la chute sont neutralisees ici.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class LoadingShield {
    private LoadingShield() { }

    /** Marge apres la fin du chargement : 3 secondes. */
    static final int MARGIN = 60;
    /** Duree maximale absolue : 45 secondes. */
    static final int MAX = 900;

    private static final class State { long start; long readyAt = -1; }
    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    /** (Re)arme le bouclier : a appeler a chaque teleportation vers une zone chargee. */
    public static void grant(ServerPlayer p) {
        var s = new State();
        s.start = p.level().getServer().getTickCount();
        ACTIVE.put(p.getUUID(), s);
    }

    public static boolean active(net.minecraft.world.entity.Entity e) { return e instanceof ServerPlayer p && ACTIVE.containsKey(p.getUUID()); }
    public static void clear(ServerPlayer p) { ACTIVE.remove(p.getUUID()); }
    public static void clearAll() { ACTIVE.clear(); }

    /** Ecran de chargement du mod ou transfert en cours : l'horloge de marge ne demarre pas. */
    private static boolean busy(ServerPlayer p) {
        return com.mmorpg.network.NetworkPlayers.isArriving(p) || com.mmorpg.network.NetworkPlayers.inTransit(p)
                || com.mmorpg.mine.MineManager.transitioning(p) || com.mmorpg.plains.PlainsManager.transitioning(p);
    }

    private static boolean chunksReady(ServerPlayer p) {
        if (!(p.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        int cx = p.blockPosition().getX() >> 4, cz = p.blockPosition().getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (!level.hasChunk(cx + dx, cz + dz)) return false;
        return true;
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        if (ACTIVE.isEmpty()) return;
        var server = e.getServer();
        long now = server.getTickCount();
        for (Iterator<Map.Entry<UUID, State>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            State s = en.getValue();
            if (p == null || p.isRemoved() || !p.isAlive() || now - s.start >= MAX) { it.remove(); continue; }
            p.resetFallDistance();
            p.setAirSupply(p.getMaxAirSupply());
            if (busy(p) || !p.connection.hasClientLoaded() || !chunksReady(p)) { s.readyAt = -1; continue; }
            if (s.readyAt < 0) s.readyAt = now;
            if (now - s.readyAt >= MARGIN) it.remove();
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) clear(p); }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) clear(p); }

    /** Les monstres vanilla lâchent aussi leur cible tant que le joueur est protégé. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void target(LivingChangeTargetEvent e) {
        if (active(e.getNewAboutToBeSetTarget())) e.setCanceled(true);
    }
}
