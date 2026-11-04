package com.mmorpg.security;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Journal d'audit des actions d'administration : fichier {@code config/mmorpg/admin_audit.log}, toujours actif, ecrit hors du thread serveur. */
public final class AdminJournal {
    private AdminJournal() {
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "aeloria-audit");
        t.setDaemon(true);
        return t;
    });

    public static Path file() {
        return ConfigManager.dir().resolve("admin_audit.log");
    }

    /** Ajoute une ligne : {@code date  acteur  type  montant  detail}. */
    public static void write(String who, String kind, long amount, String detail) {
        final String line = Instant.now() + "\t" + clean(who) + "\t" + clean(kind) + "\t" + amount + "\t" + clean(detail);
        IO.execute(() -> {
            try {
                Files.createDirectories(ConfigManager.dir());
                Files.writeString(file(), line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {
                MMORPG.LOGGER.warn("[MMORPG] admin_audit.log non écrit : {}", e.getMessage());
            }
        });
    }

    /** Attend la fin des ecritures en cours (tests, arret). */
    public static void flush() {
        try {
            IO.submit(() -> { }).get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    /** Les {@code n} dernieres lignes du journal. */
    public static List<String> tail(int n) {
        flush();
        try {
            List<String> all = Files.readAllLines(file(), StandardCharsets.UTF_8);
            return all.subList(Math.max(0, all.size() - Math.max(1, n)), all.size());
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String clean(String s) {
        if (s == null) return "";
        String t = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
        return t.length() > 400 ? t.substring(0, 400) : t;
    }
}
