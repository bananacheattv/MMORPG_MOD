package com.mmorpg.db;

import com.mmorpg.MMORPG;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base de donnees du reseau Aeloria (MariaDB / MySQL). Toutes les requetes passent par un seul thread dedie avec une seule connexion
 * (reconnectee au besoin) : le thread du serveur ne fait jamais d'entree-sortie reseau, sauf les rares attentes bornees (arrivee d'un
 * joueur). Sans base configuree ({@link DbConfig#enabled}), tout cela est inerte.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Database {
    /** Une requete executee sur le thread de la base, avec sa connexion. */
    @FunctionalInterface
    public interface Task<T> {
        T run(Connection c) throws SQLException;
    }

    private static final int QUEUE_LIMIT = 20_000;
    private static final long SCHEMA_VERSION = 3;

    private static volatile DbConfig config = new DbConfig();
    private static volatile boolean ready;
    private static ThreadPoolExecutor exec;
    private static Connection conn;                                  // uniquement sur le thread de la base
    private static final AtomicLong DROPPED = new AtomicLong();
    private static volatile String lastError = "";

    private Database() {
    }

    public static DbConfig config() {
        return config;
    }

    public static boolean enabled() {
        return config.enabled;
    }

    /** Vrai quand la connexion est etablie et le schema a jour. */
    public static boolean ready() {
        return ready;
    }

    public static String lastError() {
        return lastError;
    }

    public static long dropped() {
        return DROPPED.get();
    }

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent event) {
        config = DbConfig.load();
        if (!config.enabled) {
            MMORPG.LOGGER.info("[MMORPG] Base de donnees : desactivee (config/mmorpg/database.json)");
            return;
        }
        exec = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(QUEUE_LIMIT), r -> {
            Thread t = new Thread(r, "aeloria-db");
            t.setDaemon(true);
            return t;
        });
        try {
            // au demarrage : on attend la connexion (quelques secondes au plus) pour que le premier joueur trouve la base prete
            exec.submit(Database::connectAndMigrate).get(8, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            fail("demarrage", e instanceof ExecutionException ee && ee.getCause() != null ? ee.getCause() : e);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        if (exec == null) return;
        exec.shutdown();                                              // les sauvegardes de fin de partie (deconnexions) sont deja dans la file
        try {
            if (!exec.awaitTermination(20, TimeUnit.SECONDS)) {
                MMORPG.LOGGER.warn("[MMORPG] Base de donnees : {} operation(s) abandonnee(s) a l'arret", exec.getQueue().size());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            if (conn != null) conn.close();
        } catch (SQLException ignored) {
        }
        ready = false;
        exec = null;
    }

    // ================================================================== connexion et schema

    private static Void connectAndMigrate() throws SQLException {
        Connection c = connection();
        try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS aeloria_meta (k VARCHAR(64) NOT NULL PRIMARY KEY, v VARCHAR(255) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            long have = 0;
            try (var rs = s.executeQuery("SELECT v FROM aeloria_meta WHERE k = 'schema_version'")) {
                if (rs.next()) have = Long.parseLong(rs.getString(1));
            }
            if (have < 1) {
                s.execute("CREATE TABLE IF NOT EXISTS players ("
                        + "uuid CHAR(36) NOT NULL PRIMARY KEY, name VARCHAR(32) NOT NULL, class VARCHAR(32) NOT NULL, level INT NOT NULL, xp BIGINT NOT NULL, "
                        + "gold BIGINT NOT NULL, mines_unlocked INT NOT NULL, plains_unlocked INT NOT NULL, evolution INT NOT NULL, data MEDIUMTEXT NOT NULL, "
                        + "version BIGINT NOT NULL DEFAULT 1, server_id VARCHAR(32) NOT NULL, online TINYINT(1) NOT NULL DEFAULT 0, "
                        + "first_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, last_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "KEY idx_players_name (name), KEY idx_players_level (level), KEY idx_players_server (server_id, online)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                s.execute("CREATE TABLE IF NOT EXISTS bank_accounts ("
                        + "uuid CHAR(36) NOT NULL PRIMARY KEY, gold BIGINT NOT NULL, data MEDIUMTEXT NOT NULL, version BIGINT NOT NULL DEFAULT 1, "
                        + "updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                s.execute("CREATE TABLE IF NOT EXISTS events ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, ts TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), server_id VARCHAR(32) NOT NULL, "
                        + "uuid CHAR(36) NULL, name VARCHAR(32) NULL, kind VARCHAR(32) NOT NULL, amount BIGINT NULL, detail VARCHAR(512) NULL, "
                        + "KEY idx_events_player (uuid, ts), KEY idx_events_kind (kind, ts)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            }
            if (have < 2) {                                           // v2 : inventaire dans le profil et transferts entre serveurs
                s.execute("ALTER TABLE players ADD COLUMN IF NOT EXISTS inventory MEDIUMTEXT NULL AFTER data");
                s.execute("CREATE TABLE IF NOT EXISTS transfers (uuid CHAR(36) NOT NULL PRIMARY KEY, dest_server VARCHAR(32) NOT NULL, kind VARCHAR(16) NOT NULL, "
                        + "arg INT NOT NULL DEFAULT 0, created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            }
            if (have < 3) {                                           // v3 : classements (panneaux)
                s.execute("CREATE TABLE IF NOT EXISTS leaderboard_stats (board VARCHAR(32) NOT NULL, uuid CHAR(36) NOT NULL, name VARCHAR(64) NOT NULL, "
                        + "value BIGINT NOT NULL DEFAULT 0, extra BIGINT NOT NULL DEFAULT 0, updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                        + "PRIMARY KEY (board, uuid), KEY idx_lb_rank (board, value, extra)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            }
            // les prochaines evolutions du schema s'ajoutent ici : if (have < 4) { ... }
            s.execute("REPLACE INTO aeloria_meta (k, v) VALUES ('schema_version', '" + SCHEMA_VERSION + "')");
            // reprise apres plantage : les joueurs que CE serveur avait en ligne ne le sont plus
            s.executeUpdate("UPDATE players SET online = 0 WHERE server_id = '" + config.serverId.replace("'", "''") + "' AND online = 1");
        }
        ready = true;
        lastError = "";
        MMORPG.LOGGER.info("[MMORPG] Base de donnees : connectee ({} / {}), serveur « {} », schema v{}", config.host + ":" + config.port, config.database, config.serverId, SCHEMA_VERSION);
        return null;
    }

    /** Sur le thread de la base : la connexion en cours, rouverte si elle est tombee. */
    private static Connection connection() throws SQLException {
        try {
            if (conn != null && !conn.isClosed() && conn.isValid(2)) return conn;
        } catch (SQLException ignored) {
        }
        ready = false;
        Properties p = new Properties();
        p.setProperty("user", config.user);
        p.setProperty("password", config.password);
        p.setProperty("connectTimeout", "3000");
        p.setProperty("socketTimeout", "15000");
        // le pilote est embarque dans le jar du mod : on l'instancie directement (le registre DriverManager ne le voit pas depuis un module de jeu)
        conn = new org.mariadb.jdbc.Driver().connect(config.url(), p);
        if (conn == null) throw new SQLException("pilote MariaDB : URL refusee " + config.url());
        conn.setAutoCommit(true);
        ready = true;
        return conn;
    }

    private static void fail(String what, Throwable e) {
        lastError = what + " : " + e.getClass().getSimpleName() + " " + e.getMessage();
        ready = false;
        MMORPG.LOGGER.error("[MMORPG] Base de donnees : {} en echec ({})", what, lastError);
    }

    // ================================================================== execution

    /** Execute sur le thread de la base sans attendre ; renvoie {@code null} si la base est desactivee ou la file pleine. */
    public static <T> Future<T> async(String what, Task<T> task) {
        ThreadPoolExecutor e = exec;
        if (e == null || e.isShutdown()) return null;
        try {
            return e.submit((Callable<T>) () -> {
                try {
                    T r = task.run(connection());
                    if (!ready) ready = true;
                    return r;
                } catch (SQLException ex) {
                    fail(what, ex);
                    try {
                        if (conn != null) conn.close();
                    } catch (SQLException ignored) {
                    }
                    conn = null;
                    throw ex;
                }
            });
        } catch (RejectedExecutionException full) {
            DROPPED.incrementAndGet();
            return null;
        }
    }

    /** Idem, sans resultat : la requete part et on n'attend rien. */
    public static void fire(String what, Task<?> task) {
        async(what, task);
    }

    /** Attend le resultat au plus {@code millis} ms ; {@code null} en cas d'echec, de depassement ou de base indisponible. */
    public static <T> T await(String what, long millis, Task<T> task) {
        Future<T> f = async(what, task);
        if (f == null) return null;
        try {
            return f.get(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            return null;                                              // deja journalise par async()
        } catch (TimeoutException e) {
            MMORPG.LOGGER.warn("[MMORPG] Base de donnees : {} trop long (> {} ms), on continue sans", what, millis);
            return null;
        }
    }

    /** Nombre d'operations en attente (diagnostic). */
    public static int pending() {
        ThreadPoolExecutor e = exec;
        return e == null ? 0 : e.getQueue().size();
    }
}
