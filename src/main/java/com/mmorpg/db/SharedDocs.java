package com.mmorpg.db;

import com.google.gson.Gson;
import com.mmorpg.MMORPG;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Documents JSON partages entre les serveurs, dans la table {@code shared_docs} (guildes, registres de coordination, groupes, hotel des ventes).
 * Chaque acces est une lecture-modification-ecriture sous verrou nomme MariaDB ({@code GET_LOCK}, tenu par l'unique connexion du serveur) et controle
 * de revision. A la premiere lecture d'un document absent de la base, l'ancien fichier JSON est importe puis renomme en {@code .migre}.
 */
public final class SharedDocs {
    private static final Gson GSON = new Gson();
    private static final long WAIT_MS = 3000;

    private SharedDocs() {
    }

    /** Vrai quand les documents partages vivent dans la base (activee et {@code syncShared}). */
    public static boolean active() {
        return Database.enabled() && Database.config().syncShared;
    }

    /** Vrai quand les documents doivent passer par la base : en reseau toujours (une base injoignable refuse l'operation), sinon seulement si elle repond. */
    public static boolean useDb() {
        return active() && (Database.ready() || com.mmorpg.network.NetworkConfig.current.enabled);
    }

    private record Row(String data, long revision) {
    }

    /**
     * @param legacy fichier JSON d'avant la base (importe si la base n'a pas encore le document), ou {@code null}
     * @throws IllegalStateException si la base est injoignable ou occupee (rien n'est ecrit)
     */
    public static <S, T> T access(String name, Type type, Supplier<S> empty, Path legacy, boolean write, Function<S, T> action) {
        final String lock = "aeloria:" + name;
        Row row = Database.await("lecture de " + name, WAIT_MS + 1000, c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT GET_LOCK(?, 3)")) {
                ps.setString(1, lock);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) != 1) throw new SQLException("document « " + name + " » occupe");
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT data, revision FROM shared_docs WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new Row(rs.getString(1), rs.getLong(2)) : new Row(null, 0);
                }
            }
        });
        if (row == null) throw new IllegalStateException("Base de donnees indisponible : " + name);
        boolean released = false;
        try {
            String json = row.data();
            boolean migrated = false;
            if (json == null && legacy != null && Files.exists(legacy)) {
                try {
                    json = Files.readString(legacy);
                    migrated = true;
                } catch (java.io.IOException e) {
                    throw new IllegalStateException("Ancien fichier illisible : " + legacy, e);
                }
            }
            S state = json == null ? empty.get() : GSON.fromJson(json, type);
            if (state == null) throw new IllegalStateException("Document vide : " + name);
            T result = action.apply(state);
            if (write || migrated) {
                final String out = GSON.toJson(state, type);
                final long rev = row.revision();
                final boolean existed = row.data() != null;
                Boolean ok = Database.await("ecriture de " + name, WAIT_MS, c -> {
                    int n;
                    if (existed) {
                        try (PreparedStatement ps = c.prepareStatement("UPDATE shared_docs SET data = ?, revision = revision + 1 WHERE name = ? AND revision = ?")) {
                            ps.setString(1, out);
                            ps.setString(2, name);
                            ps.setLong(3, rev);
                            n = ps.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement ps = c.prepareStatement("INSERT INTO shared_docs (name, data, revision) VALUES (?,?,1)")) {
                            ps.setString(1, name);
                            ps.setString(2, out);
                            n = ps.executeUpdate();
                        }
                    }
                    try (PreparedStatement ps = c.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                        ps.setString(1, lock);
                        ps.executeQuery().close();
                    }
                    return n == 1;
                });
                released = true;
                if (ok == null || !ok) throw new IllegalStateException("Ecriture refusee dans la base : " + name);
                if (migrated) {
                    try {
                        Files.move(legacy, legacy.resolveSibling(legacy.getFileName() + ".migre"), StandardCopyOption.REPLACE_EXISTING);
                        MMORPG.LOGGER.info("[MMORPG] Base de donnees : {} importe depuis {}", name, legacy);
                    } catch (java.io.IOException e) {
                        MMORPG.LOGGER.warn("[MMORPG] Base de donnees : {} importe, ancien fichier non renomme ({})", name, e.getMessage());
                    }
                }
            }
            return result;
        } finally {
            if (!released) Database.await("liberation de " + name, WAIT_MS, c -> {
                try (PreparedStatement ps = c.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                    ps.setString(1, lock);
                    ps.executeQuery().close();
                }
                return null;
            });
        }
    }

    /** Lit un document sans verrou ; {@code null} s'il est absent ou si la base est indisponible. */
    public static String get(String name) {
        Row r = Database.await("lecture de " + name, WAIT_MS, c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT data, revision FROM shared_docs WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new Row(rs.getString(1), rs.getLong(2)) : new Row(null, 0);
                }
            }
        });
        return r == null ? null : r.data();
    }

    /** Ecrit (remplace) un document sans attendre : pour les donnees tenues par un seul serveur (hotel des ventes). */
    public static void put(String name, String json) {
        Database.fire("ecriture de " + name, c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO shared_docs (name, data, revision) VALUES (?,?,1) "
                    + "ON DUPLICATE KEY UPDATE data = VALUES(data), revision = revision + 1")) {
                ps.setString(1, name);
                ps.setString(2, json);
                ps.executeUpdate();
            }
            return null;
        });
    }
}
