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
 * Classements des panneaux. Les statistiques sont ecrites dans la table {@code leaderboard_stats} de la base (partagee par tous les serveurs
 * du reseau) et recopiees dans le monde ({@link LeaderboardData}) ; sans base, le classement vient de cette copie locale.
 * Valeurs mesurees (niveau, or, monstres, quetes) : relevees toutes les 20 secondes pour les joueurs connectes ; compteurs (boss, donjons,
 * arene) : incrementes sur l'evenement. Le classement affiche est rafraichi toutes les 10 secondes.
 */
public final class Leaderboards {
    public static final int KEPT = 100;
    private static final int SAMPLE_TICKS = 400, REFRESH_TICKS = 200, GUILD_EVERY = 6;

    public record Row(String name, long value) {
    }

    private static final Map<Board, List<Row>> TOP = new EnumMap<>(Board.class);
    private static final Map<String, long[]> LAST = new HashMap<>();
    private static volatile int revision;
    private static int refreshes, pushed = -1;

    private Leaderboards() {
    }

    /** Change a chaque rafraichissement : les panneaux comparent pour savoir s'ils doivent se mettre a jour. */
    public static int revision() {
        return revision;
    }

    public static synchronized List<Row> top(Board board) {
        return TOP.getOrDefault(board, List.of());
    }

    /** Rang (a partir de 1) d'un joueur dans le top conserve, ou 0. */
    public static int rankOf(Board board, String name) {
        List<Row> rows = top(board);
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).name().equalsIgnoreCase(name)) return i + 1;
        return 0;
    }

    // ================================================================== ecriture

    private static boolean useDb() {
        return Database.enabled() && Database.ready();
    }

    private static String clean(String name) {
        String n = name == null ? "?" : name.replace('\t', ' ').replace('\n', ' ').trim();
        return n.length() > 32 ? n.substring(0, 32) : n;
    }

    /** Compteur : ajoute {@code delta} a la valeur du joueur. */
    public static void add(ServerPlayer p, Board board, long delta) {
        if (delta <= 0 || p.level().getServer() == null) return;
        add(p.level().getServer(), p.getUUID(), p.getGameProfile().name(), board, delta);
    }

    public static void add(MinecraftServer server, UUID id, String name, Board board, long delta) {
        if (delta <= 0) return;
        final String who = clean(name);
        LeaderboardData.get(server).add(board, id, who, delta);
        if (!Database.enabled()) return;
        Database.fire("classement " + board.id, c -> {
            try (var ps = c.prepareStatement("INSERT INTO leaderboard_stats (board, uuid, name, value, extra) VALUES (?,?,?,?,0) "
                    + "ON DUPLICATE KEY UPDATE name = VALUES(name), value = value + VALUES(value)")) {
                ps.setString(1, board.id);
                ps.setString(2, id.toString());
                ps.setString(3, who);
                ps.setLong(4, delta);
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

    /** Fin d'un match d'arene (hors entrainement) : victoires et eliminations des joueurs presents. */
    public static void arena(MinecraftServer server, com.mmorpg.competition.ArenaRules.Match match) {
        int winner = match.winner();
        for (var team : match.teams.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(team.getKey());
            if (p == null) continue;
            var contribution = match.contributions.get(team.getKey());
            if (contribution != null) add(p, Board.PVP_ELIMS, contribution.kills);
            if (winner >= 0 && team.getValue() == winner) add(p, Board.PVP_VICTOIRES, 1);
        }
    }

    // ================================================================== cycle serveur

    public static void tick(MinecraftServer server) {
        int t = server.getTickCount();
        if (t % SAMPLE_TICKS == 0) sample(server);
        if (t % REFRESH_TICKS == 7) refresh(server);
        if (t % 40 == 11 && pushed != revision) {
            pushed = revision;
            LeaderboardBlockEntity.refreshAll();
        }
    }

    private static void sample(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (com.mmorpg.network.NetworkPlayers.inTransit(p)) continue;
            var d = RpgPlayers.get(p);
            if (!d.playerClass.isPlayable()) continue;
            UUID id = p.getUUID();
            String name = p.getGameProfile().name();
            set(server, id, name, Board.NIVEAU, d.level, d.xp);
            set(server, id, name, Board.FORTUNE, d.gold, 0);
            long kills = 0;
            for (int k : d.kills.values()) kills += k;
            set(server, id, name, Board.MONSTRES, kills, 0);
            set(server, id, name, Board.QUETES, d.completedQuests.size(), 0);
        }
    }

    private static void refresh(MinecraftServer server) {
        boolean db = useDb();
        for (Board board : Board.values()) {
            if (board == Board.GUILDES) continue;
            if (db) {
                Database.fire("classement " + board.id, c -> {
                    List<Row> rows = new ArrayList<>();
                    try (var ps = c.prepareStatement("SELECT name, value FROM leaderboard_stats WHERE board = ? AND value > 0 "
                            + "ORDER BY value DESC, extra DESC, name LIMIT " + KEPT)) {
                        ps.setString(1, board.id);
                        try (var rs = ps.executeQuery()) {
                            while (rs.next()) rows.add(new Row(rs.getString(1), rs.getLong(2)));
                        }
                    }
                    server.execute(() -> publish(board, rows));
                    return null;
                });
            } else {
                publish(board, LeaderboardData.get(server).top(board, KEPT).stream().map(e -> new Row(e.name(), e.value())).toList());
            }
        }
        if (refreshes++ % GUILD_EVERY == 0) guilds(server);
    }

    private static void guilds(MinecraftServer server) {
        try {
            List<com.mmorpg.guild.GuildStore.Guild> all = com.mmorpg.guild.GuildStore.access(server, false, s -> new ArrayList<>(s.guilds));
            publish(Board.GUILDES, all.stream()
                    .sorted((a, b) -> a.level != b.level ? Integer.compare(b.level, a.level) : Long.compare(b.xp, a.xp))
                    .limit(KEPT).map(g -> new Row(clean(g.name), g.level)).toList());
        } catch (RuntimeException e) {
            MMORPG.LOGGER.warn("[MMORPG] Classement des guildes indisponible : {}", e.getMessage());
        }
    }

    private static synchronized void publish(Board board, List<Row> rows) {
        List<Row> old = TOP.get(board);
        if (rows.equals(old)) return;
        TOP.put(board, List.copyOf(rows));
        revision++;
    }
}
