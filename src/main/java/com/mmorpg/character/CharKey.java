package com.mmorpg.character;

import com.mmorpg.server.RpgPlayers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Cle d'identite d'un personnage (compte + emplacement) pour les registres partages (guildes, succes...).
 * L'emplacement 0 garde l'UUID du compte : les donnees existantes sont donc deja celles du personnage 0, sans migration.
 */
public final class CharKey {
    private CharKey() {
    }

    public static UUID key(UUID account, int slot) {
        return slot <= 0 ? account : UUID.nameUUIDFromBytes(("aeloria-char:" + account + ":" + slot).getBytes(StandardCharsets.UTF_8));
    }

    /** Cle du personnage actuellement joue par ce joueur. */
    public static UUID of(ServerPlayer p) {
        return key(p.getUUID(), RpgPlayers.get(p).charSlot);
    }

    /** Nom du personnage actif (nom du compte si non renomme). */
    public static String name(ServerPlayer p) {
        String n = RpgPlayers.get(p).charName;
        return n == null || n.isBlank() ? p.getName().getString() : n;
    }

    /** Joueur connecte qui joue actuellement le personnage de cette cle, ou null. */
    public static ServerPlayer online(MinecraftServer server, UUID key) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (of(p).equals(key)) return p;
        return null;
    }
}
