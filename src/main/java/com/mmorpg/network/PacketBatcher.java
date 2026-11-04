package com.mmorpg.network;

import com.mmorpg.MMORPG;
import com.mmorpg.perf.Perf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Regroupement des paquets frequents : pendant un tick, les paquets d'etat (SyncPlayer, Vitals) ne gardent que le dernier, et
 * les paquets d'evenement (textes de combat) partent ensemble. Tout part en fin de tick dans un seul envoi groupe, sans
 * changer le contenu recu par le client (au pire le dernier etat, au plus tard a la fin du meme tick).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class PacketBatcher {
    private static final class Box {
        final ServerPlayer player;
        final Map<Class<?>, CustomPacketPayload> state = new LinkedHashMap<>();
        final List<CustomPacketPayload> events = new ArrayList<>();

        Box(ServerPlayer player) {
            this.player = player;
        }
    }

    private static final Map<UUID, Box> PENDING = new LinkedHashMap<>();   // thread du serveur uniquement

    private PacketBatcher() {
    }

    /** Vrai si ce paquet est pris en charge ici (a utiliser depuis le thread du serveur). */
    static boolean offer(ServerPlayer player, CustomPacketPayload payload) {
        boolean isState = payload instanceof Payloads.SyncPlayer || payload instanceof Payloads.Vitals;
        boolean isEvent = payload instanceof Payloads.CombatText;
        if (!isState && !isEvent) return false;
        if (player.level().getServer() == null || !player.level().getServer().isSameThread()) return false;
        Box box = PENDING.computeIfAbsent(player.getUUID(), k -> new Box(player));
        Perf.PACKETS_QUEUED.incrementAndGet();
        if (isState) {
            if (box.state.put(payload.getClass(), payload) != null) Perf.PACKETS_COALESCED.incrementAndGet();
        } else {
            box.events.add(payload);
        }
        return true;
    }

    @SubscribeEvent
    public static void onTickEnd(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        Perf.time("paquets", PacketBatcher::flush);
    }

    private static void flush() {
        var boxes = new ArrayList<>(PENDING.values());
        PENDING.clear();
        for (Box box : boxes) {
            ServerPlayer p = box.player;
            if (p.hasDisconnected() || p.connection == null) continue;
            List<CustomPacketPayload> all = new ArrayList<>(box.state.values());
            all.addAll(box.events);
            if (all.isEmpty()) continue;
            if (all.size() == 1) {
                PacketDistributor.sendToPlayer(p, all.get(0));
            } else {
                PacketDistributor.sendToPlayer(p, all.get(0), all.subList(1, all.size()).toArray(new CustomPacketPayload[0]));
                Perf.BUNDLES_SENT.incrementAndGet();
            }
        }
    }

    /** Envoie tout de suite ce qui attend pour ce joueur (avant un changement de serveur ou une deconnexion). */
    public static void flushNow(ServerPlayer p) {
        Box box = PENDING.remove(p.getUUID());
        if (box == null || p.hasDisconnected() || p.connection == null) return;
        for (var pl : box.state.values()) PacketDistributor.sendToPlayer(p, pl);
        for (var pl : box.events) PacketDistributor.sendToPlayer(p, pl);
    }
}
