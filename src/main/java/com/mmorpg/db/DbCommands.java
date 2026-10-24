package com.mmorpg.db;

import com.mmorpg.MMORPG;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Commandes d'exploitation de la base (operateurs) : /db etat, /db sauvegarder, /db joueur &lt;pseudo&gt;, /db evenements [n]. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DbCommands {
    private DbCommands() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("db").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("etat").executes(c -> status(c.getSource())))
                .then(Commands.literal("sauvegarder").executes(c -> {
                    if (!Database.enabled() || !Database.ready()) return fail(c.getSource(), "La base n'est pas disponible (/db etat).");
                    int n = PlayerSync.pushAll(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal(n + " joueur(s) envoyé(s) à la base."), true);
                    return n;
                }))
                .then(Commands.literal("joueur").then(Commands.argument("pseudo", StringArgumentType.word()).executes(c -> player(c.getSource(), StringArgumentType.getString(c, "pseudo")))))
                .then(Commands.literal("evenements").executes(c -> events(c.getSource(), 10))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 30)).executes(c -> events(c.getSource(), IntegerArgumentType.getInteger(c, "n"))))));
    }

    private static int fail(CommandSourceStack src, String msg) {
        src.sendFailure(Component.literal(msg));
        return 0;
    }

    private static void reply(CommandSourceStack src, List<String> lines) {
        src.getServer().execute(() -> {
            for (String l : lines) src.sendSystemMessage(Component.literal(l));
        });
    }

    private static int status(CommandSourceStack src) {
        DbConfig cfg = Database.config();
        if (!cfg.enabled) {
            src.sendSuccess(() -> Component.literal("Base de données : désactivée (config/mmorpg/database.json, « enabled »)."), false);
            return 1;
        }
        src.sendSuccess(() -> Component.literal("Base de données : " + (Database.ready() ? "connectée" : "NON connectée") + " — " + cfg.host + ":" + cfg.port + "/" + cfg.database
                + " — serveur « " + cfg.serverId + " » — file : " + Database.pending() + " — abandonnées : " + Database.dropped()
                + (Database.lastError().isEmpty() ? "" : " — dernière erreur : " + Database.lastError())), false);
        Database.fire("etat", conn -> {
            List<String> out = new ArrayList<>();
            try (var st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT (SELECT COUNT(*) FROM players), (SELECT COUNT(*) FROM players WHERE online = 1), "
                    + "(SELECT COUNT(*) FROM bank_accounts), (SELECT COUNT(*) FROM events)")) {
                rs.next();
                out.add("Profils : " + rs.getLong(1) + " (" + rs.getLong(2) + " en ligne sur le réseau) — comptes bancaires : " + rs.getLong(3) + " — événements : " + rs.getLong(4));
            }
            reply(src, out);
            return null;
        });
        return 1;
    }

    private static int player(CommandSourceStack src, String name) {
        if (!Database.enabled() || !Database.ready()) return fail(src, "La base n'est pas disponible (/db etat).");
        Database.fire("joueur", conn -> {
            List<String> out = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT name, class, level, xp, gold, version, server_id, online, last_seen, first_seen FROM players WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        out.add(rs.getString(1) + " : " + rs.getString(2) + " niveau " + rs.getInt(3) + " (" + rs.getLong(4) + " XP), " + rs.getLong(5) + " or — version " + rs.getLong(6)
                                + " — " + (rs.getInt(8) == 1 ? "en ligne sur « " + rs.getString(7) + " »" : "dernier serveur « " + rs.getString(7) + " »")
                                + " — vu le " + rs.getString(9) + " (premier passage " + rs.getString(10) + ")");
                    } else {
                        out.add("Aucun profil nommé « " + name + " » dans la base.");
                    }
                }
            }
            reply(src, out);
            return null;
        });
        return 1;
    }

    private static int events(CommandSourceStack src, int n) {
        if (!Database.enabled() || !Database.ready()) return fail(src, "La base n'est pas disponible (/db etat).");
        Database.fire("evenements", conn -> {
            List<String> out = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT ts, server_id, name, kind, amount, detail FROM events ORDER BY id DESC LIMIT ?")) {
                ps.setInt(1, n);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String detail = rs.getString(6);
                        out.add(rs.getString(1) + " [" + rs.getString(2) + "] " + (rs.getString(3) == null ? "-" : rs.getString(3)) + " " + rs.getString(4)
                                + (rs.getObject(5) == null || rs.getLong(5) == 0 ? "" : " " + rs.getLong(5)) + (detail == null ? "" : " (" + detail + ")"));
                    }
                }
            }
            if (out.isEmpty()) out.add("Aucun événement.");
            reply(src, out);
            return null;
        });
        return 1;
    }
}
