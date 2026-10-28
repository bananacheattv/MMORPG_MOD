package com.mmorpg.achievement;

import com.mmorpg.db.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Succes, statistiques et titre choisi dans MariaDB (inerte quand la base est desactivee). */
public final class AchievementStore {
    public record Loaded(Map<String, Long> achievements, Map<String, Long> stats, String title) {
    }

    private static volatile boolean schema;

    private AchievementStore() {
    }

    private static void ensureSchema(Connection c) throws SQLException {
        if (schema) return;
        try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS player_achievements (uuid CHAR(36) NOT NULL, achievement VARCHAR(48) NOT NULL, unlocked_at BIGINT NOT NULL, "
                    + "PRIMARY KEY (uuid, achievement), KEY idx_achievement (achievement)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.execute("CREATE TABLE IF NOT EXISTS player_stats (uuid CHAR(36) NOT NULL, stat VARCHAR(48) NOT NULL, value BIGINT NOT NULL, "
                    + "PRIMARY KEY (uuid, stat)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.execute("CREATE TABLE IF NOT EXISTS player_titles (uuid CHAR(36) NOT NULL PRIMARY KEY, title VARCHAR(64) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
        schema = true;
    }

    public static boolean enabled() {
        return Database.enabled();
    }

    /** Lit tout ce que la base connait du joueur (thread de la base, le rappel est appele sur ce meme thread). */
    public static void load(UUID id, java.util.function.Consumer<Loaded> then) {
        Database.fire("succes (lecture)", c -> {
            ensureSchema(c);
            Map<String, Long> ach = new HashMap<>(), stats = new HashMap<>();
            String title = "";
            try (var ps = c.prepareStatement("SELECT achievement, unlocked_at FROM player_achievements WHERE uuid = ?")) {
                ps.setString(1, id.toString());
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) ach.put(rs.getString(1), rs.getLong(2));
                }
            }
            try (var ps = c.prepareStatement("SELECT stat, value FROM player_stats WHERE uuid = ?")) {
                ps.setString(1, id.toString());
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) stats.put(rs.getString(1), rs.getLong(2));
                }
            }
            try (var ps = c.prepareStatement("SELECT title FROM player_titles WHERE uuid = ?")) {
                ps.setString(1, id.toString());
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) title = rs.getString(1);
                }
            }
            then.accept(new Loaded(ach, stats, title));
            return null;
        });
    }

    /** Ecrit succes, compteurs et titre (les compteurs ne diminuent jamais). */
    public static void save(UUID id, Map<String, Long> achievements, Map<String, Long> stats, String title) {
        Map<String, Long> ach = Map.copyOf(achievements), st = Map.copyOf(stats);
        Database.fire("succes (ecriture)", c -> {
            ensureSchema(c);
            try (var ps = c.prepareStatement("INSERT INTO player_achievements (uuid, achievement, unlocked_at) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE achievement = achievement")) {
                for (var e : ach.entrySet()) {
                    ps.setString(1, id.toString());
                    ps.setString(2, e.getKey());
                    ps.setLong(3, e.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            try (var ps = c.prepareStatement("INSERT INTO player_stats (uuid, stat, value) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE value = GREATEST(value, VALUES(value))")) {
                for (var e : st.entrySet()) {
                    ps.setString(1, id.toString());
                    ps.setString(2, e.getKey());
                    ps.setLong(3, e.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            try (var ps = c.prepareStatement("REPLACE INTO player_titles (uuid, title) VALUES (?, ?)")) {
                ps.setString(1, id.toString());
                ps.setString(2, title == null ? "" : title);
                ps.executeUpdate();
            }
            return null;
        });
    }
}
