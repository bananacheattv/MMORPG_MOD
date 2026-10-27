package com.mmorpg.network;

import com.google.gson.Gson;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;

/** Small atomic cross-backend ledgers. A failed read never silently resets existing data. */
public final class SharedLedger {
    private static final Gson JSON = new Gson();
    public static synchronized <S,T> T access(MinecraftServer server, String name, Class<S> type, Supplier<S> empty, Function<S,T> action) {
        Path directory = com.mmorpg.guild.GuildStore.directory(server).resolveSibling("coordination");
        try {
            Files.createDirectories(directory);
            try (var channel = FileChannel.open(directory.resolve(name + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 var lock = channel.lock()) {
                Path file = directory.resolve(name + ".json");
                S state = Files.exists(file) ? JSON.fromJson(Files.readString(file), type) : empty.get();
                if (state == null) throw new IllegalStateException("Registre vide : " + name);
                T result = action.apply(state);
                Path temp = Files.createTempFile(directory, name, ".tmp");
                try {
                    Files.writeString(temp, JSON.toJson(state));
                    Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(temp); }
                return result;
            }
        } catch (java.io.IOException e) { throw new IllegalStateException("Coordination indisponible : " + name, e); }
    }
}
