package com.mmorpg.network;

import com.google.gson.GsonBuilder;
import com.mmorpg.config.ConfigManager;
import java.nio.file.Files;
import java.nio.file.Path;

/** Optional local multi-server network; disabled for existing single-player worlds. */
public final class NetworkConfig {
    public enum Role { STANDALONE, HUB, MAIN, MINES, PLAINS }
    public boolean enabled = false;
    public Role role = Role.STANDALONE;
    public String sharedDirectory = "../../shared/players";
    public String hub = "hub", main = "serveur", mines = "mines", plains = "plaines";
    public static NetworkConfig current = new NetworkConfig();

    public static void load() throws java.io.IOException {
        var gson = new GsonBuilder().setPrettyPrinting().create();
        Path file = ConfigManager.dir().resolve("network.json");
        Files.createDirectories(file.getParent());
        current = Files.exists(file) ? gson.fromJson(Files.readString(file), NetworkConfig.class) : new NetworkConfig();
        if (current == null || current.role == null) throw new java.io.IOException("Invalid network.json");
        if (current.enabled && current.role == Role.STANDALONE) throw new java.io.IOException("Network role must be explicit");
        for (String name : new String[]{current.hub, current.main, current.mines, current.plains}) {
            if (name == null || !name.matches("[a-zA-Z0-9_-]{1,64}")) throw new java.io.IOException("Invalid proxy server name");
        }
        if (!Files.exists(file)) Files.writeString(file, gson.toJson(current));
    }
    public static boolean hosts(Role role) { return !current.enabled || current.role == role; }
    public static boolean town() { return !current.enabled || current.role == Role.HUB || current.role == Role.MAIN; }
}
