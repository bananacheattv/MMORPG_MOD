package com.mmorpg.db;

import com.mmorpg.MMORPG;
import com.mmorpg.economy.BankData;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Profil des joueurs (classe, niveau, experience, or, quetes, montures, deblocages...), banque, inventaire et coffre de l'Ender dans la base
 * du reseau, pour qu'un joueur qui passe d'un serveur a l'autre (hub, mines, plaines, serveur principal) retrouve la meme progression et
 * les memes objets.
 *
 * <p>Regles : a l'arrivee, la base fait foi si elle a une version plus recente que celle que ce serveur connait ; le joueur est alors
 * « reserve » pour ce serveur (colonne {@code online}), et si un autre serveur le tenait encore, on attend jusqu'a 3 s qu'il ait fini de
 * sauvegarder avant de lire. A la sortie et toutes les {@link DbConfig#autosaveSeconds} secondes, le profil est ecrit (seulement s'il a change) ;
 * un serveur qui ne tient plus le joueur ne peut plus l'ecraser. Non synchronises : la position et l'etat du monde.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class PlayerSync {
    private static final long PULL_WAIT_MS = 3000;
    /** Empreinte de ce qui a ete ecrit en dernier pour chaque joueur : on n'ecrit pas deux fois la meme chose. */
    private static final Map<UUID, Integer> LAST_HASH = new java.util.concurrent.ConcurrentHashMap<>();
    private static int ticks;

    private PlayerSync() {
    }

    private static boolean active() {
        return Database.enabled() && Database.config().syncPlayers && Database.ready();
    }

    /** Ligne lue dans la base a l'arrivee d'un joueur. */
    private record Row(String data, long version, String bank, String inventory, String arrivalKind, int arrivalArg) {
    }

    // ================================================================== arrivee

    /** Avant tous les autres traitements de connexion : le profil doit etre a jour avant qu'on s'en serve. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Audit.log(p, Audit.LOGIN, 0, p.level().dimension().identifier().toString());
        if (!active()) return;
        final UUID id = p.getUUID();
        final String me = Database.config().serverId;
        Row row = Database.await("lecture du profil", PULL_WAIT_MS + 2000, c -> {
            long deadline = System.currentTimeMillis() + PULL_WAIT_MS;
            Row r;
            while (true) {
                boolean busyElsewhere = false;
                r = null;
                try (PreparedStatement ps = c.prepareStatement("SELECT p.data, p.version, p.online, p.server_id, TIMESTAMPDIFF(SECOND, p.last_seen, NOW()), b.data, p.inventory "
                        + "FROM players p LEFT JOIN bank_accounts b ON b.uuid = p.uuid WHERE p.uuid = ?")) {
                    ps.setString(1, id.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            r = new Row(rs.getString(1), rs.getLong(2), rs.getString(6), rs.getString(7), null, 0);
                            busyElsewhere = rs.getInt(3) == 1 && !me.equals(rs.getString(4)) && rs.getLong(5) <= 180;
                        }
                    }
                }
                if (r == null || !busyElsewhere || System.currentTimeMillis() >= deadline) {
                    if (busyElsewhere) MMORPG.LOGGER.warn("[MMORPG] Base de donnees : {} est encore en ligne sur un autre serveur, on reprend sans attendre", id);
                    break;
                }
                try {
                    Thread.sleep(200);                                // l'autre serveur finit de sauvegarder
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (r != null) {                                          // on reserve le joueur pour ce serveur
                try (PreparedStatement ps = c.prepareStatement("UPDATE players SET online = 1, server_id = ?, last_seen = NOW() WHERE uuid = ?")) {
                    ps.setString(1, me);
                    ps.setString(2, id.toString());
                    ps.executeUpdate();
                }
            }
            // arrivee notee par le serveur d'ou vient le joueur (recente, adressee a ce serveur) : lue puis effacee
            String kind = null;
            int arg = 0;
            try (PreparedStatement ps = c.prepareStatement("SELECT kind, arg FROM transfers WHERE uuid = ? AND dest_server = ? AND created > NOW() - INTERVAL 120 SECOND")) {
                ps.setString(1, id.toString());
                ps.setString(2, me);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        kind = rs.getString(1);
                        arg = rs.getInt(2);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM transfers WHERE uuid = ?")) {
                ps.setString(1, id.toString());
                ps.executeUpdate();
            }
            if (kind != null) r = r == null ? new Row(null, 0, null, null, kind, arg) : new Row(r.data(), r.version(), r.bank(), r.inventory(), kind, arg);
            return r;
        });
        PlayerData d = RpgPlayers.get(p);
        MinecraftServer server = p.level().getServer();
        if (row != null) {
            if (com.mmorpg.network.NetworkConfig.current.enabled) {
                // en reseau, le profil partage (ProfileStore) fait foi : la base le refletera a la prochaine ecriture
            } else if (row.data() != null && row.version() > d.dbVersion) {
                try {
                    CompoundTag t = TagParser.parseCompoundFully(row.data());
                    d.load(t);
                    d.dbVersion = row.version();
                    if (row.bank() != null) BankData.get(server).replace(id, row.bank(), server.registryAccess());
                    if (row.inventory() != null) InventorySync.apply(p, row.inventory());
                    MMORPG.LOGGER.info("[MMORPG] Profil de {} lu dans la base (version {}, niveau {})", p.getGameProfile().name(), row.version(), d.level);
                } catch (Exception e) {
                    MMORPG.LOGGER.error("[MMORPG] Profil de {} illisible dans la base, on garde celui du serveur", p.getGameProfile().name(), e);
                }
            } else if (row.data() != null && row.version() < d.dbVersion) {
                MMORPG.LOGGER.warn("[MMORPG] La base a un profil plus ancien (v{}) que ce serveur (v{}) pour {} : on garde celui du serveur",
                        row.version(), d.dbVersion, p.getGameProfile().name());
            }
        }
        LAST_HASH.remove(id);                                         // premiere ecriture de la session : toujours (elle reserve aussi le joueur)
    }

    // ================================================================== ecriture

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Audit.log(p, Audit.LOGOUT, 0, null);
        push(p, true);
        LAST_HASH.remove(p.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!Database.enabled() || !Database.config().syncPlayers) return;
        ticks++;
        if (ticks % 20 != 0) return;                                  // une fois par seconde
        boolean full = ticks % (Database.config().autosaveSeconds * 20) == 0;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            if (full || !LAST_HASH.containsKey(p.getUUID())) push(p, false);      // les nouveaux joueurs sont ecrits tout de suite
        }
    }

    /** Ecrit tous les joueurs connectes maintenant (commande /db sauvegarder). @return le nombre de joueurs envoyes */
    public static int pushAll(MinecraftServer server) {
        int n = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            LAST_HASH.remove(p.getUUID());
            push(p, false);
            n++;
        }
        return n;
    }

    /**
     * Serialise le profil, la banque et l'inventaire sur le thread du serveur, puis les envoie a la base sans attendre.
     *
     * @return l'operation en cours (vrai quand tout est ecrit), ou {@code null} si rien n'a ete envoye
     */
    public static Future<Boolean> push(ServerPlayer p, boolean logout) {
        if (!Database.enabled() || !Database.config().syncPlayers) return null;
        PlayerData d = RpgPlayers.get(p);
        MinecraftServer server = p.level().getServer();
        final UUID id = p.getUUID();
        final String name = p.getGameProfile().name();
        CompoundTag tag = d.save();
        tag.remove("dbVersion");                                      // la version est ecrite par la base : l'inclure ferait changer l'empreinte a chaque ecriture
        final String profile = tag.toString();
        BankData.Account acc = BankData.get(server).peek(id);
        final String bank = acc == null ? null : BankData.encode(acc, server.registryAccess());
        final String inventory = InventorySync.encode(p);
        final long bankGold = acc == null ? 0 : acc.gold;
        final int hash = (profile.hashCode() * 31 + (bank == null ? 0 : bank.hashCode())) * 31 + inventory.hashCode();
        final String me = Database.config().serverId;
        final String cls = d.playerClass.name();
        final int level = d.level, minesU = d.minesUnlocked, plainsU = d.plainsUnlocked, evo = d.evolution;
        final long xp = d.xp, gold = d.gold;
        Integer before = LAST_HASH.get(id);
        if (!logout && before != null && before == hash) {     // rien n'a change : on signale seulement que le joueur est toujours la
            return Database.async("presence", c -> {
                try (PreparedStatement ps = c.prepareStatement("UPDATE players SET last_seen = NOW() WHERE uuid = ? AND server_id = ?")) {
                    ps.setString(1, id.toString());
                    ps.setString(2, me);
                    ps.executeUpdate();
                }
                return true;
            });
        }
        LAST_HASH.put(id, hash);
        final int online = logout ? 0 : 1;
        return Database.async("ecriture du profil", c -> {
            long version;
            c.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = c.prepareStatement("UPDATE players SET name = ?, class = ?, level = ?, xp = ?, gold = ?, mines_unlocked = ?, plains_unlocked = ?, "
                        + "evolution = ?, data = ?, inventory = ?, version = version + 1, server_id = ?, online = ?, last_seen = NOW() WHERE uuid = ? AND (server_id = ? OR online = 0)")) {
                    int i = 1;
                    ps.setString(i++, name);
                    ps.setString(i++, cls);
                    ps.setInt(i++, level);
                    ps.setLong(i++, xp);
                    ps.setLong(i++, gold);
                    ps.setInt(i++, minesU);
                    ps.setInt(i++, plainsU);
                    ps.setInt(i++, evo);
                    ps.setString(i++, profile);
                    ps.setString(i++, inventory);
                    ps.setString(i++, me);
                    ps.setInt(i++, online);
                    ps.setString(i++, id.toString());
                    ps.setString(i, me);
                    rows = ps.executeUpdate();
                }
                if (rows == 0) {                                      // pas encore de ligne : premiere ecriture de ce joueur
                    try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO players (uuid, name, class, level, xp, gold, mines_unlocked, plains_unlocked, evolution, data, inventory, "
                            + "version, server_id, online) VALUES (?,?,?,?,?,?,?,?,?,?,?,1,?,?)")) {
                        int i = 1;
                        ps.setString(i++, id.toString());
                        ps.setString(i++, name);
                        ps.setString(i++, cls);
                        ps.setInt(i++, level);
                        ps.setLong(i++, xp);
                        ps.setLong(i++, gold);
                        ps.setInt(i++, minesU);
                        ps.setInt(i++, plainsU);
                        ps.setInt(i++, evo);
                        ps.setString(i++, profile);
                        ps.setString(i++, inventory);
                        ps.setString(i++, me);
                        ps.setInt(i, online);
                        rows = ps.executeUpdate();
                    }
                }
                if (rows == 0) {                                      // un autre serveur tient ce joueur : on ne l'ecrase pas
                    c.rollback();
                    LAST_HASH.remove(id);
                    MMORPG.LOGGER.warn("[MMORPG] Base de donnees : {} est tenu par un autre serveur, ecriture ignoree", name);
                    return false;
                }
                try (PreparedStatement ps = c.prepareStatement("SELECT version FROM players WHERE uuid = ?")) {
                    ps.setString(1, id.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        version = rs.getLong(1);
                    }
                }
                if (bank != null) {
                    try (PreparedStatement ps = c.prepareStatement("INSERT INTO bank_accounts (uuid, gold, data, version) VALUES (?,?,?,?) "
                            + "ON DUPLICATE KEY UPDATE gold = VALUES(gold), data = VALUES(data), version = VALUES(version), updated = NOW()")) {
                        ps.setString(1, id.toString());
                        ps.setLong(2, bankGold);
                        ps.setString(3, bank);
                        ps.setLong(4, version);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (java.sql.SQLException e) {
                c.rollback();
                LAST_HASH.remove(id);
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
            final long v = version;
            server.execute(() -> d.dbVersion = Math.max(d.dbVersion, v));
            return true;
        });
    }
}
