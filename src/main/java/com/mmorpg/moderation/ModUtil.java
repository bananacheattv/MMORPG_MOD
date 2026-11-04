package com.mmorpg.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Outils communs de la moderation : dossier de donnees, JSON, durees, droits. */
public final class ModUtil {
    private ModUtil() {
    }

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path dir() {
        return ConfigManager.dir().resolve("moderation");
    }

    public static boolean staff(ServerPlayer p) {
        return p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static <T> T read(String file, Type type, T fallback) {
        Path f = dir().resolve(file);
        try {
            if (Files.exists(f)) {
                T v = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), type);
                if (v != null) return v;
            }
        } catch (Exception e) {
            MMORPG.LOGGER.warn("[MMORPG] MODERATION : {} illisible : {}", file, e.getMessage());
        }
        return fallback;
    }

    public static void write(String file, Object value) {
        try {
            Files.createDirectories(dir());
            Path tmp = Files.createTempFile(dir(), "mod-", ".tmp");
            Files.writeString(tmp, GSON.toJson(value), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, dir().resolve(file), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, dir().resolve(file), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            MMORPG.LOGGER.warn("[MMORPG] MODERATION : {} non enregistre : {}", file, e.getMessage());
        }
    }

    /** « 30 », « 30m », « 2h », « 1d », « 90s » (sans unite : minutes). Renvoie des millisecondes, ou -1 si invalide. */
    public static long parseDuration(String s) {
        if (s == null) return -1;
        s = s.strip().toLowerCase(java.util.Locale.ROOT);
        if (s.isEmpty()) return -1;
        long mult = 60_000L;
        char last = s.charAt(s.length() - 1);
        if (Character.isLetter(last)) {
            switch (last) {
                case 's' -> mult = 1000L;
                case 'm' -> mult = 60_000L;
                case 'h' -> mult = 3_600_000L;
                case 'j', 'd' -> mult = 86_400_000L;
                default -> {
                    return -1;
                }
            }
            s = s.substring(0, s.length() - 1);
        }
        try {
            long n = Long.parseLong(s.strip());
            if (n <= 0 || n > 365L * 24 * 60) return -1;
            long ms = n * mult;
            return ms > 365L * 86_400_000L ? -1 : ms;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String formatDuration(long ms) {
        long s = Math.max(0, ms / 1000);
        if (s >= 86400) return (s / 86400) + " j " + (s % 86400 / 3600) + " h";
        if (s >= 3600) return (s / 3600) + " h " + (s % 3600 / 60) + " min";
        if (s >= 60) return (s / 60) + " min " + (s % 60) + " s";
        return s + " s";
    }

    public static ServerPlayer online(MinecraftServer server, String name) {
        return server == null || name == null ? null : server.getPlayerList().getPlayerByName(name);
    }

    public static UUID uuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static void tell(ServerPlayer p, String text, int color) {
        p.sendSystemMessage(Component.literal(text).withColor(color));
    }

    public static void broadcast(MinecraftServer server, String text, int color) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(text).withColor(color), false);
    }
}
