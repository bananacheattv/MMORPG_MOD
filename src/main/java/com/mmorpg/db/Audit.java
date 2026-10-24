package com.mmorpg.db;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Journal des evenements du jeu dans la table {@code events} (qui a fait quoi, quand, sur quel serveur) : connexions, niveaux, morts,
 * boss, banque, ventes, deblocages... Les appels ne bloquent jamais le serveur et ne font rien sans base ou si {@link DbConfig#audit} est faux.
 */
public final class Audit {
    private Audit() {
    }

    /** Types d'evenements (colonne {@code kind}). */
    public static final String LOGIN = "login", LOGOUT = "logout", LEVEL_UP = "level_up", DEATH = "death", BOSS_KILL = "boss_kill",
            DUNGEON_WIN = "dungeon_win", BANK_DEPOSIT = "bank_deposit", BANK_WITHDRAW = "bank_withdraw", AUCTION_LIST = "auction_list",
            AUCTION_BUY = "auction_buy", AUCTION_SALE = "auction_sale", SHOP_BUY = "shop_buy", SHOP_SELL = "shop_sell", QUEST_DONE = "quest_done",
            UNLOCK = "unlock", MOUNT = "mount", GOLD_SPENT = "gold_spent", ADMIN = "admin";

    public static void log(ServerPlayer p, String kind, long amount, String detail) {
        log(p == null ? null : p.getUUID(), p == null ? null : p.getGameProfile().name(), kind, amount, detail);
    }

    public static void log(UUID uuid, String name, String kind, long amount, String detail) {
        if (!Database.enabled() || !Database.config().audit) return;
        final String id = uuid == null ? null : uuid.toString();
        final String who = name == null ? null : (name.length() > 32 ? name.substring(0, 32) : name);
        final String what = detail == null ? null : (detail.length() > 500 ? detail.substring(0, 500) : detail);
        final String server = Database.config().serverId;
        Database.fire("journal " + kind, c -> {
            try (var ps = c.prepareStatement("INSERT INTO events (server_id, uuid, name, kind, amount, detail) VALUES (?,?,?,?,?,?)")) {
                ps.setString(1, server);
                ps.setString(2, id);
                ps.setString(3, who);
                ps.setString(4, kind);
                ps.setLong(5, amount);
                ps.setString(6, what);
                ps.executeUpdate();
            }
            return null;
        });
    }
}
