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
        // lecture abandonnee (delai depasse) mais finie plus tard : le verrou pris doit etre rendu, par la tache ou par l'appelant
        final java.util.concurrent.atomic.AtomicBoolean claimed = new java.util.concurrent.atomic.AtomicBoolean();
        Row row = Database.await("lecture de " + name, WAIT_MS + 1000, c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT GET_LOCK(?, 3)")) {
                ps.setString(1, lock);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) != 1) throw new SQLException("document « " + name + " » occupe");
                }
            }
            Row r;
            try (PreparedStatement ps = c.prepareStatement("SELECT data, revision FROM shared_docs WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    r = rs.next() ? new Row(rs.getString(1), rs.getLong(2)) : new Row(null, 0);
                }
            }
            if (!claimed.compareAndSet(false, true)) release(c, lock);    // l'appelant a deja abandonne
            return r;
        });
        if (row == null) {
            if (!claimed.compareAndSet(false, true)) Database.fire("liberation de " + name, c -> {   // lecture finie juste apres le delai
                release(c, lock);
                return null;
            });
            throw new IllegalStateException("Base de donnees indisponible : " + name);
        }
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
                release(c, lock);
                return null;
            });
        }
    }

    private static void release(java.sql.Connection c, String lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            ps.setString(1, lock);
            ps.executeQuery().close();
        }
    }

    /** Lit un document sans verrou ; {@code null} s'il est absent ou si la base est indisponible. */
    public static String get(String name) {
        Doc d = read(name);
        return d == null ? null : d.data();
    }

    /** Contenu ({@code null} si absent) et revision (0 si absent) d'un document. */
    public record Doc(String data, long revision) {
    }

    /** Lit un document et sa revision sans verrou ; {@code null} si la base est indisponible. */
    public static Doc read(String name) {
        Row r = Database.await("lecture de " + name, WAIT_MS, c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT data, revision FROM shared_docs WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new Row(rs.getString(1), rs.getLong(2)) : new Row(null, 0);
                }
            }
        });
        return r == null ? null : new Doc(r.data(), r.revision());
    }

    /** Revision d'un document (0 s'il est absent), -1 si la base ne repond pas : dit si une copie en memoire est perimee. */
    public static long revision(String name) {
        Long r = Database.await("revision de " + name, WAIT_MS, c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT revision FROM shared_docs WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
        return r == null ? -1 : r;
    }

    /**
     * Sondage periodique d'un document sans jamais attendre la base sur le thread du serveur : chaque appel a {@link #poll()}
     * rend la lecture arrivee depuis l'appel precedent (ou {@code null}) et en relance une si aucune n'est en cours.
     */
    public static final class Poller {
        private final String name;
        private volatile Doc fetched;
        private volatile boolean fetching;
        private volatile long epoch;

        public Poller(String name) {
            this.name = name;
        }

        /** Derniere lecture arrivee ({@code data} nul si le document est absent), {@code null} si rien de neuf. */
        public Doc poll() {
            Doc d = fetched;
            fetched = null;
            if (!fetching) {
                fetching = true;
                final long launched = epoch;
                var task = Database.async("lecture de " + name, c -> {
                    try (PreparedStatement ps = c.prepareStatement("SELECT data, revision FROM shared_docs WHERE name = ?")) {
                        ps.setString(1, name);
                        try (ResultSet rs = ps.executeQuery()) {
                            Doc doc = rs.next() ? new Doc(rs.getString(1), rs.getLong(2)) : new Doc(null, 0);
                            if (epoch == launched) fetched = doc;
                        }
                        return null;
                    } finally {
                        fetching = false;
                    }
                });
                if (task == null) fetching = false;
            }
            return d;
        }

        /** Ignore les lectures en cours ou arrivees : le document vient d'etre modifie ici, elles sont perimees. */
        public void discard() {
            epoch++;
            fetched = null;
        }
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
