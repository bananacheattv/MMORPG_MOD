package com.mmorpg.network;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mmorpg.MMORPG;
import com.mmorpg.server.PartyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.*;

/** Authoritative membership shared by all local JVMs; mutations are read/modify/write under one OS lock. */
public final class SharedParties {
    private static final com.google.gson.Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final java.lang.reflect.Type TYPE = new TypeToken<List<PartyManager.Party>>() {}.getType();
    private static boolean changing;
    private static long lastWarning;
    private static Path file() { return Path.of(NetworkConfig.current.sharedDirectory).toAbsolutePath().resolveSibling("parties.json"); }
    private static volatile String fetched;
    private static boolean fetching;
    private static boolean db() { return com.mmorpg.db.SharedDocs.useDb(); }
    private static List<PartyManager.Party> read() throws IOException {
        if (db()) { String json = com.mmorpg.db.SharedDocs.get("parties"); if (json == null) return readFile(); return parse(json); }
        return readFile();
    }
    private static List<PartyManager.Party> readFile() throws IOException {
        if (!Files.exists(file())) return new ArrayList<>();
        return parse(Files.readString(file()));
    }
    private static List<PartyManager.Party> parse(String json) throws IOException {
        try {
            List<PartyManager.Party> groups = JSON.fromJson(json, TYPE);
            if (groups == null) throw new IllegalArgumentException("Null party list");
            validate(groups);
            return groups;
        } catch (RuntimeException e) { throw new IOException("Invalid shared parties.json", e); }
    }
    private static void validate(List<PartyManager.Party> groups) {
        if (groups == null) throw new IllegalArgumentException("Null party list");
        Set<UUID> seen = new HashSet<>();
        for (var group : groups) {
            if (group == null || group.leader == null || group.members == null || group.members.size() < 2 || group.members.size() > PartyManager.MAX_SIZE || !group.members.contains(group.leader)) throw new IllegalArgumentException("Invalid group");
            for (UUID id : group.members) if (id == null || !seen.add(id)) throw new IllegalArgumentException("Duplicate/null member");
        }
    }
    public static void refresh() throws IOException {
        if (NetworkConfig.current.enabled && !changing) PartyManager.installShared(read());
    }
    public static void poll() {
        if (db() && NetworkConfig.current.enabled && !changing) {
            // base : la lecture part en arriere-plan, le resultat est installe au passage suivant (jamais d'attente sur le thread du serveur)
            String json = fetched; fetched = null;
            try { if (json != null) PartyManager.installShared(parse(json)); }
            catch (IOException e) { MMORPG.LOGGER.error("[Aeloria] Shared group membership invalid; retaining last good state", e); }
            if (!fetching) {
                fetching = true;
                var task = com.mmorpg.db.Database.async("lecture des groupes", c -> {
                    try (var ps = c.prepareStatement("SELECT data FROM shared_docs WHERE name = 'parties'"); var rs = ps.executeQuery()) { if (rs.next()) fetched = rs.getString(1); return null; }
                    finally { fetching = false; }
                });
                if (task == null) fetching = false;
            }
            return;
        }
        try { refresh(); }
        catch (IOException e) {
            if (System.currentTimeMillis() - lastWarning > 10_000) {
                lastWarning = System.currentTimeMillis(); MMORPG.LOGGER.error("[Aeloria] Shared group membership unavailable; retaining last good state", e);
            }
        }
    }
    public static void mutate(ServerPlayer player, Runnable action) {
        if (!NetworkConfig.current.enabled || changing) { action.run(); return; }
        List<PartyManager.Party> original = null;
        if (db()) {
            try {
                final List<PartyManager.Party>[] saved = new List[1];
                try {
                    com.mmorpg.db.SharedDocs.access("parties", TYPE, ArrayList<PartyManager.Party>::new, file(), true, (List<PartyManager.Party> groups) -> {
                        validate(groups); PartyManager.installShared(groups); saved[0] = PartyManager.sharedSnapshot();
                        changing = true;
                        try { action.run(); } finally { changing = false; }
                        groups.clear(); groups.addAll(PartyManager.sharedSnapshot());
                        return null;
                    });
                } catch (RuntimeException e) { if (saved[0] != null) PartyManager.installShared(saved[0]); throw e; }
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("Le groupe n'a pas pu être modifié. Réessayez ; contactez un administrateur si le problème persiste.").withColor(0xFF6060));
                MMORPG.LOGGER.error("[Aeloria] Shared group update refused", e);
            }
            return;
        }
        try {
            Path file = file(); Files.createDirectories(file.getParent());
            try (FileChannel channel = FileChannel.open(file.resolveSibling("parties.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                FileLock lock = null;
                long deadline = System.nanoTime() + 1_000_000_000L;
                do { lock = channel.tryLock(); if (lock != null) break; Thread.sleep(10); } while (System.nanoTime() < deadline);
                if (lock == null) throw new IOException("Group store is busy");
                try {
                    PartyManager.installShared(read()); original = PartyManager.sharedSnapshot();
                    changing = true;
                    action.run();
                    Path temp = Files.createTempFile(file.getParent(), "parties-", ".tmp");
                    try {
                        Files.writeString(temp, JSON.toJson(PartyManager.sharedSnapshot(), TYPE));
                        try (FileChannel output = FileChannel.open(temp, StandardOpenOption.WRITE)) { output.force(true); }
                        if (Files.exists(file)) Files.copy(file, file.resolveSibling("parties.json.bak"), StandardCopyOption.REPLACE_EXISTING);
                        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } finally { Files.deleteIfExists(temp); }
                } finally { changing = false; lock.release(); }
            }
        } catch (Exception e) {
            if (original != null) PartyManager.installShared(original);
            player.sendSystemMessage(Component.literal("Le groupe n'a pas pu être modifié. Réessayez ; contactez un administrateur si le problème persiste.").withColor(0xFF6060));
            MMORPG.LOGGER.error("[Aeloria] Shared group update refused", e);
        }
    }
}
