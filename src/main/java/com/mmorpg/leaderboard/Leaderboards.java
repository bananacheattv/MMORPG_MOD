package com.mmorpg.leaderboard;

import com.mmorpg.MMORPG;
import com.mmorpg.db.Database;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Statistiques de classement des joueurs. Ecrites dans la table {@code leaderboard_stats} de la base (partagee par tous les serveurs du
 * reseau) et recopiees dans le monde ({@link LeaderboardData}) ; sans base, le classement vient de cette copie locale.
 * Valeurs mesurees (niveau, or, monstres, quetes) : relevees pour les joueurs connectes ; compteurs (boss, donjons, arene) : incrementes
 * sur l'evenement. Les panneaux et commandes lisent le top en cache ({@link com.mmorpg.ranking.Rankings}).
 */
public final class Leaderboards {
    public static final int KEPT = 100;

    /** Une ligne du classement : joueur, valeur principale et bonus de depart. */
    public record Row(String id, String name, long value, long extra) {
    }

    private static final Map<Board, List<Row>> TOP = new EnumMap<>(Board.class);
    private static final Map<String, long[]> LAST = new HashMap<>();

    private Leaderboards() {
    }

    public static synchronized List<Row> top(Board board) {
        return TOP.getOrDefault(board, List.of());
    }

    private static String clean(String name) {
        String n = name == null ? "?" : name.replace('\t', ' ').replace('\n', ' ').trim();
        return n.length() > 32 ? n.substring(0, 32) : n;
    }

    // ================================================================== ecriture

    /** Compteur : ajoute {@code delta} (et {@code extraDelta}) aux valeurs du joueur. */
    public static void add(ServerPlayer p, Board board, long delta) {
        if (p.level().getServer() != null) add(p.level().getServer(), p.getUUID(), p.getGameProfile().name(), board, delta, 0);
    }

    public static void add(MinecraftServer server, UUID id, String name, Board board, long delta, long extraDelta) {
        if (delta <= 0 && extraDelta <= 0) return;
        final String who = clean(name);
        LeaderboardData.get(server).add(board, id, who, delta, extraDelta);
        if (!Database.enabled()) return;
        Database.fire("classement " + board.id, c -> {
            try (var ps = c.prepareStatement("INSERT INTO leaderboard_stats (board, uuid, name, value, extra) VALUES (?,?,?,?,?) "
                    + "ON DUPLICATE KEY UPDATE name = VALUES(name), value = value + VALUES(value), extra = extra + VALUES(extra)")) {
                ps.setString(1, board.id);
                ps.setString(2, id.toString());
                ps.setString(3, who);
                ps.setLong(4, delta);
                ps.setLong(5, extraDelta);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** Valeur mesuree : remplace la valeur du joueur (ignore si rien n'a change depuis le dernier releve). */
    public static void set(MinecraftServer server, UUID id, String name, Board board, long value, long extra) {
        String key = board.id + id;
        long[] last = LAST.get(key);
        if (last != null && last[0] == value && last[1] == extra) return;
        LAST.put(key, new long[]{value, extra});
        final String who = clean(name);
        LeaderboardData.get(server).set(board, id, who, value, extra);
        if (!Database.enabled()) return;
        Database.fire("classement " + board.id, c -> {
            try (var ps = c.prepareStatement("INSERT INTO leaderboard_stats (board, uuid, name, value, extra) VALUES (?,?,?,?,?) "
                    + "ON DUPLICATE KEY UPDATE name = VALUES(name), value = VALUES(value), extra = VALUES(extra)")) {
                ps.setString(1, board.id);
                ps.setString(2, id.toString());
                ps.setString(3, who);
                ps.setLong(4, value);
                ps.setLong(5, extra);
                ps.executeUpdate();
            }
            return null;
        });
    }

    // ================================================================== cycle serveur

    /** Releve les valeurs mesurees des joueurs connectes. */
    public static void sample(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sample(server, p);
    }

    public static void sample(MinecraftServer server, ServerPlayer p) {
        if (com.mmorpg.network.NetworkPlayers.inTransit(p)) return;
        var d = RpgPlayers.get(p);
        if (!d.playerClass.isPlayable()) return;
        UUID id = p.getUUID();
        String name = p.getGameProfile().name();
        set(server, id, name, Board.NIVEAU, d.level, d.xp);
        set(server, id, name, Board.FORTUNE, d.gold, 0);
        long kills = 0;
        for (int k : d.kills.values()) kills += k;
        set(server, id, name, Board.MONSTRES, kills, 0);
        set(server, id, name, Board.QUETES, d.completedQuests.size(), 0);
    }

    /** Relit les classements : depuis la base si elle est prete (resultat publie sur le thread serveur), sinon depuis la copie locale. */
    public static void refresh(MinecraftServer server) {
        boolean db = Database.enabled() && Database.ready();
        for (Board board : Board.values()) {
            if (db) {
                Database.fire("classement " + board.id, c -> {
                    List<Row> rows = query(c, board);
                    server.execute(() -> publish(board, rows));
                    return null;
                });
            } else {
                publish(board, LeaderboardData.get(server).top(board, KEPT).stream()
                        .map(e -> new Row(e.id().toString(), e.name(), e.value(), e.extra())).toList());
            }
        }
    }

    /** Comme {@link #refresh} mais attend la base (verifications de developpement uniquement). */
    public static void refreshAndWait(MinecraftServer server) {
        if (!(Database.enabled() && Database.ready())) {
            refresh(server);
            return;
        }
        for (Board board : Board.values()) {
            List<Row> rows = Database.await("classement " + board.id, 5000, c -> query(c, board));
            if (rows != null) publish(board, rows);
        }
    }

    private static List<Row> query(java.sql.Connection c, Board board) throws java.sql.SQLException {
        List<Row> rows = new ArrayList<>();
        try (var ps = c.prepareStatement("SELECT uuid, name, value, extra FROM leaderboard_stats WHERE board = ? AND value > 0 "
                + "ORDER BY value DESC, extra DESC, name LIMIT " + KEPT)) {
            ps.setString(1, board.id);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) rows.add(new Row(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4)));
            }
        }
        return rows;
    }

    private static synchronized void publish(Board board, List<Row> rows) {
        TOP.put(board, List.copyOf(rows));
    }
}
