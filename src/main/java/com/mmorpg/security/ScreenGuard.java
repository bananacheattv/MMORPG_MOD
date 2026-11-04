package com.mmorpg.security;

import com.mmorpg.anticheat.AntiCheat;
import com.mmorpg.network.S2COpenScreen;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validation serveur des ouvertures d'interface : le serveur retient quels ecrans il a lui-meme ouverts pour chaque joueur
 * ({@link com.mmorpg.network.Net#toPlayer}) et refuse les actions d'une interface qu'il n'a jamais ouverte (client modifie).
 */
public final class ScreenGuard {
    private ScreenGuard() {
    }

    /** Duree pendant laquelle une ouverture reste valable. */
    static final long VALID_MS = 30 * 60_000L;
    /** Paquet -> ecrans dont il peut venir. */
    private static final Map<String, Set<Integer>> NEEDS = Map.of(
            "forge_action", Set.of(S2COpenScreen.FORGE, S2COpenScreen.COMPRESSOR),
            "shop_action", Set.of(S2COpenScreen.SHOP),
            "teleport", Set.of(S2COpenScreen.TELEPORTER),
            "crate_open", Set.of(S2COpenScreen.CRATE),
            "lucky_roll", Set.of(S2COpenScreen.LUCKY_BLOCK));
    private static final Map<UUID, Map<Integer, Long>> OPENED = new ConcurrentHashMap<>();

    public static void opened(ServerPlayer p, int screen) {
        OPENED.computeIfAbsent(p.getUUID(), k -> new ConcurrentHashMap<>()).put(screen, System.currentTimeMillis());
    }

    public static boolean wasOpened(UUID id, String packet, long now) {
        Set<Integer> screens = NEEDS.get(packet);
        if (screens == null) return true;
        Map<Integer, Long> m = OPENED.get(id);
        if (m == null) return false;
        for (int s : screens) {
            Long t = m.get(s);
            if (t != null && now - t <= VALID_MS) return true;
        }
        return false;
    }

    public static boolean allowed(ServerPlayer p, String packet) {
        if (wasOpened(p.getUUID(), packet, System.currentTimeMillis())) return true;
        AntiCheat.flag(p, AntiCheat.Check.SCREEN, packet);
        return false;
    }

    public static void forget(UUID id) {
        OPENED.remove(id);
    }
}
