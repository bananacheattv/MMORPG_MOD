package com.mmorpg.admin;

import com.mmorpg.MMORPG;
import com.mmorpg.character.CharKey;
import com.mmorpg.db.Database;
import com.mmorpg.moderation.ModUtil;
import com.mmorpg.network.NetworkConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.*;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * Suppression de TOUS les personnages d'un joueur (par pseudo exact, joueur hors ligne).
 * Sauvegarde d'abord tout dans config/.../moderation/suppressions/, puis efface : fiche des personnages,
 * donnees du joueur, profil reseau, lignes MariaDB (profil et banque), appartenance aux guildes.
 * Sans {@code confirmer}, ne fait qu'afficher ce qui serait supprime.
 */
public final class PurgePlayer {
    private PurgePlayer() {
    }

    public static String run(MinecraftServer server, String name, boolean confirm) throws Exception {
        UUID id = resolve(server, name);
        if (id == null) return "Joueur introuvable (pseudo exact requis) : " + name;
        if (server.getPlayerList().getPlayer(id) != null) return name + " est connecté : expulse-le (/kick) puis relance la commande.";
        for (var p : server.getPlayerList().getPlayers()) if (CharKey.of(p).equals(id)) return name + " est connecté : expulse-le d'abord.";

        List<Path> files = new ArrayList<>();
        Path chars = com.mmorpg.character.CharacterStore.directory(server);
        addWithBak(files, chars.resolve(id + ".dat"));
        addWithBak(files, server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id + ".dat"));
        if (NetworkConfig.current.enabled) {
            Path shared = Path.of(NetworkConfig.current.sharedDirectory).toAbsolutePath();
            addWithBak(files, shared.resolve(id + ".dat"));
        }
        List<UUID> keys = new ArrayList<>();
        for (int s = 0; s < 12; s++) keys.add(CharKey.key(id, s));
        boolean db = Database.enabled() && Database.ready();

        // 1) verification des guildes AVANT toute suppression (refus si chef d'une guilde qui a d'autres membres)
        if (confirm) {
            for (UUID k : keys) {
                try {
                    com.mmorpg.guild.GuildService.leaveForDelete(server, k);
                } catch (IllegalArgumentException e) {
                    return "Suppression refusée (aucune donnée effacée en dehors des guildes déjà quittées) : " + e.getMessage();
                }
            }
        }

        String summary = "fichiers=" + files.size() + (db ? " + lignes MariaDB (profil, banque)" : " (base de données non active)") + ", guildes quittées pour 12 emplacements";
        if (!confirm) return "Simulation pour " + name + " (" + id + ") : " + summary + ". Relance avec « confirmer » pour supprimer (sauvegarde automatique avant).";

        // 2) sauvegarde
        Path backup = ModUtil.dir().resolve("suppressions").resolve(name + "_" + System.currentTimeMillis());
        Files.createDirectories(backup);
        for (Path f : files) Files.copy(f, backup.resolve(f.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
        if (db) {
            String dump = Database.await("sauvegarde avant suppression", 8000, c -> {
                StringBuilder sb = new StringBuilder();
                for (String t : new String[]{"players", "bank_accounts"}) {
                    try (PreparedStatement ps = c.prepareStatement("SELECT * FROM " + t + " WHERE uuid IN (" + marks(keys.size()) + ")")) {
                        for (int i = 0; i < keys.size(); i++) ps.setString(i + 1, keys.get(i).toString());
                        try (var rs = ps.executeQuery()) {
                            var md = rs.getMetaData();
                            while (rs.next()) {
                                sb.append(t).append(':');
                                for (int i = 1; i <= md.getColumnCount(); i++) sb.append(md.getColumnName(i)).append('=').append(rs.getString(i)).append(';');
                                sb.append('\n');
                            }
                        }
                    }
                }
                return sb.toString();
            });
            if (dump == null) return "Sauvegarde MariaDB impossible : suppression annulée (rien d'effacé hors guildes).";
            Files.writeString(backup.resolve("mariadb.txt"), dump);
        }

        // 3) suppression
        for (Path f : files) Files.deleteIfExists(f);
        if (db) {
            Integer n = Database.await("suppression des profils", 8000, c -> {
                int total = 0;
                for (String t : new String[]{"players", "bank_accounts"}) {
                    try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + t + " WHERE uuid IN (" + marks(keys.size()) + ")")) {
                        for (int i = 0; i < keys.size(); i++) ps.setString(i + 1, keys.get(i).toString());
                        total += ps.executeUpdate();
                    }
                }
                return total;
            });
            if (n == null) return "Fichiers supprimés mais la base n'a pas répondu : relance la commande. Sauvegarde : " + backup;
        }
        MMORPG.LOGGER.warn("[MMORPG] ADMIN : tous les personnages de {} ({}) supprimes. Sauvegarde : {}", name, id, backup);
        return "Personnages de " + name + " supprimés (" + summary + "). Sauvegarde : " + backup;
    }

    private static String marks(int n) {
        return String.join(",", Collections.nCopies(n, "?"));
    }

    private static void addWithBak(List<Path> files, Path f) {
        if (Files.exists(f)) files.add(f);
        Path bak = f.resolveSibling(f.getFileName() + ".bak");
        if (Files.exists(bak)) files.add(bak);
    }

    /** UUID du compte pour ce pseudo exact : cache de profils du serveur, puis base MariaDB. */
    static UUID resolve(MinecraftServer server, String name) {
        try {
            var cached = server.services().nameToIdCache().get(name);
            if (cached.isPresent()) return cached.get().id();
        } catch (Throwable ignored) {
        }
        if (Database.enabled() && Database.ready()) {
            String id = Database.await("recherche du pseudo", 4000, c -> {
                try (PreparedStatement ps = c.prepareStatement("SELECT uuid FROM players WHERE name = ? ORDER BY last_seen DESC LIMIT 1")) {
                    ps.setString(1, name);
                    try (var rs = ps.executeQuery()) {
                        return rs.next() ? rs.getString(1) : null;
                    }
                }
            });
            if (id != null) return UUID.fromString(id);
        }
        return null;
    }
}
