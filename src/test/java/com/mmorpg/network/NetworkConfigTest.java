package com.mmorpg.network;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkConfigTest {
    private final NetworkConfig previous = NetworkConfig.current;

    @AfterEach
    void restore() { NetworkConfig.current = previous; }

    @Test
    void openworldDoesNotHostTownOrInstances() {
        var config = new NetworkConfig();
        config.enabled = true;
        config.role = NetworkConfig.Role.OPENWORLD;
        NetworkConfig.current = config;
        assertFalse(NetworkConfig.town());
        for (var role : NetworkConfig.Role.values()) {
            assertEquals(role == NetworkConfig.Role.OPENWORLD, NetworkConfig.hosts(role));
        }
        assertEquals("openworld", NetworkConfig.serverFor(NetworkConfig.Role.OPENWORLD));
        config.openworld = "exploration-2";
        assertEquals("exploration-2", NetworkConfig.serverFor(NetworkConfig.Role.OPENWORLD));
    }

    @Test
    void existingConfigurationsKeepTheirRoutesWithoutTheNewField() {
        NetworkConfig.current = new Gson().fromJson(
                "{\"enabled\":true,\"role\":\"LOBBY\",\"main\":\"spawn\"}", NetworkConfig.class);
        assertTrue(NetworkConfig.hosts(NetworkConfig.Role.LOBBY));
        assertFalse(NetworkConfig.hosts(NetworkConfig.Role.OPENWORLD));
        assertEquals("spawn", NetworkConfig.serverFor(NetworkConfig.Role.MAIN));
        assertEquals("openworld", NetworkConfig.serverFor(NetworkConfig.Role.OPENWORLD));
    }

    @Test
    void singlePlayerStillHostsAllContent() {
        NetworkConfig.current = new NetworkConfig();
        assertTrue(NetworkConfig.town());
        for (var role : NetworkConfig.Role.values()) assertTrue(NetworkConfig.hosts(role));
    }
}
