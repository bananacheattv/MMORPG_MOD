package com.mmorpg.cluster;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mmorpg.MMORPG;
import com.mmorpg.db.Audit;
import com.mmorpg.db.Database;
import com.mmorpg.db.PlayerSync;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Reseau de serveurs derriere un proxy Velocity (hub, spawn, mines, plaines...). Chaque serveur declare les lieux qu'il heberge
 * (config/mmorpg/network.json) : il ne construit que ceux-la, et quand un joueur doit aller dans un lieu qui est ailleurs
 * (portails, menus des mines et des plaines, /hub, /spawn, retour apres un essai ou une mort), il est <b>transfere</b> :
 * son profil, sa banque et son inventaire partent dans la base, une « arrivee » est notee, puis le proxy le connecte au bon serveur
 * (message « Connect » du canal BungeeCord, que Velocity traite quand {@code bungee-plugin-message-channel} est vrai). Le serveur
 * d'arrivee lit la base, retrouve l'arrivee et place le joueur. Sans reseau actif (par defaut), tout reste local comme avant.
 * Le reseau exige la base de donnees (profils synchronises).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Cluster {
    /** Les lieux du jeu qui peuvent vivre sur des serveurs differents. */
    public enum Place {
        HUB("hub"), SPAWN("spawn"), MINES("mines"), PLAINS("plains");

        public final String id;

        Place(String id) {
            this.id = id;
        }

        static Place byId(String id) {
            for (Place p : values()) if (p.id.equals(id)) return p;
            return null;
        }
    }

    /** Arrivee notee dans la base : ou placer le joueur sur le serveur d'arrivee. */
    public record Arrival(Place place, int arg) {
    }

    /** Reglages (config/mmorpg/network.json). */
    public static final class Config {
        public String _aide = "enabled=true active le reseau (la base de donnees doit etre active). hosts : lieux que CE serveur construit et sert (hub, spawn, mines, plains). "
                + "servers : nom Velocity du serveur qui heberge chaque lieu. Voir GUIDE_SERVEUR.md.";
        public boolean enabled = false;
        public List<String> hosts = List.of("hub", "spawn", "mines", "plains");
        public Map<String, String> servers = new LinkedHashMap<>(Map.of("hub", "hub", "spawn", "serveur", "mines", "mines", "plains", "plaines"));
        /** Secondes apres lesquelles un transfert sans effet (pas de proxy ?) est annule et le joueur repris par ce serveur. */
        public int giveUpSeconds = 8;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile Config config = new Config();
    private static final Map<UUID, Arrival> ARRIVALS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> IN_FLIGHT = new ConcurrentHashMap<>();

    private Cluster() {
    }

    public static Config config() {
        return config;
    }

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent event) {
        Path f = FMLPaths.CONFIGDIR.get().resolve("mmorpg").resolve("network.json");
        Config c = new Config();
        try {
            if (Files.exists(f)) {
                Config read = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Config.class);
                if (read != null) c = read;
            } else {
                Files.createDirectories(f.getParent());
                Files.writeString(f, GSON.toJson(c), StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException e) {
            MMORPG.LOGGER.error("[MMORPG] Reseau : network.json illisible, reseau desactive", e);
            c = new Config();
        }
        config = c;
        MMORPG.LOGGER.info("[MMORPG] Reseau : {}", c.enabled ? "actif, lieux heberges ici : " + c.hosts : "desactive (network.json)");
    }

    /** Vrai si le reseau est actif (reglage et base de donnees). */
    public static boolean active() {
        return config.enabled && Database.enabled() && Database.config().syncPlayers;
    }

    /** Ce serveur construit-il et sert-il ce lieu ? Toujours vrai hors reseau. */
    public static boolean hosts(Place p) {
        return !active() || config.hosts.contains(p.id);
    }

    // ================================================================== transfert

    /**
     * Envoie le joueur sur le serveur qui heberge {@code dest}, si ce n'est pas ce serveur.
     *
     * @return vrai si le transfert est lance (ou refuse : l'appelant ne doit rien faire de plus) ; faux si le lieu est ici (a traiter normalement)
     */
    public static boolean transfer(ServerPlayer p, Place dest, int arg) {
        if (hosts(dest)) return false;
        String target = config.servers.get(dest.id);
        if (target == null || target.isBlank() || target.equals(Database.config().serverId)) {
            MMORPG.LOGGER.error("[MMORPG] Reseau : aucun serveur n'heberge « {} » (network.json : servers / hosts)", dest.id);
            p.sendOverlayMessage(Component.literal("Ce lieu est indisponible pour le moment.").withColor(0xFF7060));
            return true;
        }
        long now = p.level().getServer().getTickCount();
        Long last = IN_FLIGHT.get(p.getUUID());
        if (last != null && now - last < config.giveUpSeconds * 20L + 40) return true;                 // transfert deja en cours
        if (!Database.ready()) {
            p.sendOverlayMessage(Component.literal("Réseau indisponible : réessayez dans un instant.").withColor(0xFF7060));
            return true;
        }
        IN_FLIGHT.put(p.getUUID(), now);
        Future<Boolean> written = PlayerSync.push(p, true, new PlayerSync.Move(target, dest.id, arg));
        Boolean ok = null;
        try {
            if (written != null) ok = written.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            MMORPG.LOGGER.warn("[MMORPG] Reseau : sauvegarde avant transfert impossible ({})", e.toString());
        }
        if (ok == null || !ok) {
            IN_FLIGHT.remove(p.getUUID());
            PlayerSync.reclaim(p);
            p.sendOverlayMessage(Component.literal("Transfert impossible : la sauvegarde a échoué, vous restez ici.").withColor(0xFF7060));
            return true;
        }
        Audit.log(p, Audit.TRANSFER, arg, dest.id + " -> " + target);
        p.sendOverlayMessage(Component.literal("Départ vers " + label(dest) + "…").withColor(0xFFD060));
        try {
            sendConnect(p, target);
        } catch (IOException | RuntimeException e) {
            MMORPG.LOGGER.error("[MMORPG] Reseau : message au proxy impossible", e);
        }
        final UUID id = p.getUUID();
        com.mmorpg.server.Scheduler.later(p.level(), config.giveUpSeconds * 20, () -> {      // le proxy n'a rien fait (connexion directe, proxy absent) : on reprend le joueur
            ServerPlayer still = p.level().getServer().getPlayerList().getPlayer(id);
            if (still != null && IN_FLIGHT.remove(id) != null) {
                MMORPG.LOGGER.warn("[MMORPG] Reseau : {} n'a pas ete transfere vers {} (proxy ?), il reste ici", still.getGameProfile().name(), target);
                PlayerSync.reclaim(still);
                still.sendOverlayMessage(Component.literal("Le transfert n'a pas abouti : vous restez ici.").withColor(0xFF7060));
            }
        });
        return true;
    }

    private static String label(Place p) {
        return switch (p) {
            case HUB -> "le Hub";
            case SPAWN -> "la Cité d'Aeloria";
            case MINES -> "les Mines";
            case PLAINS -> "les Plaines";
        };
    }

    /** Message « Connect » du canal BungeeCord : le proxy deplace le joueur vers le serveur nomme. */
    private static void sendConnect(ServerPlayer p, String server) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF("Connect");
            out.writeUTF(server);
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new ProxyConnect(bytes.toByteArray()));
    }

    // ================================================================== arrivee

    /** Appele a l'arrivee (avant le reste de la connexion) avec l'arrivee notee dans la base, s'il y en a une. */
    public static void noteArrival(UUID player, String kind, int arg) {
        Place place = Place.byId(kind);
        if (place != null && hosts(place)) ARRIVALS.put(player, new Arrival(place, arg));
    }

    /** Le joueur vient d'etre transfere ici : il sera place par {@link #onJoinedLate} ; la redirection habituelle de la connexion doit se taire. */
    public static boolean arrivalPending(ServerPlayer p) {
        return ARRIVALS.containsKey(p.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onJoinedLate(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        IN_FLIGHT.remove(p.getUUID());
        Arrival a = ARRIVALS.get(p.getUUID());
        if (a == null) return;
        final UUID id = p.getUUID();
        com.mmorpg.server.Scheduler.later(p.level(), 10, () -> {
            ARRIVALS.remove(id);
            ServerPlayer now = p.level().getServer().getPlayerList().getPlayer(id);
            if (now == null) return;
            MMORPG.LOGGER.info("[MMORPG] Reseau : arrivee de {} ({} {})", now.getGameProfile().name(), a.place().id, a.arg());
            switch (a.place()) {
                case HUB -> com.mmorpg.hub.HubManager.goToHub(now);
                case SPAWN -> com.mmorpg.hub.HubManager.goToSpawn(now);
                case MINES -> com.mmorpg.mine.MineManager.startTransition(now, a.arg());
                case PLAINS -> com.mmorpg.plains.PlainsManager.startTransition(now, a.arg());
            }
        });
    }

    /** Le joueur quitte ce serveur parce qu'il vient d'etre transfere (et deja sauvegarde) : vrai une seule fois. */
    public static boolean consumeLeaving(UUID player) {
        return IN_FLIGHT.remove(player) != null;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ARRIVALS.remove(event.getEntity().getUUID());
    }

    /** Resume pour /db etat. */
    public static String describe() {
        if (!config.enabled) return "réseau désactivé";
        return "réseau " + (active() ? "actif" : "INACTIF (base de données requise)") + " — héberge " + config.hosts + " — serveurs " + config.servers;
    }
}
