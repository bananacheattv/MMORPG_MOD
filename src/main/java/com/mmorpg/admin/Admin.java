package com.mmorpg.admin;

import com.mmorpg.MMORPG;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Menu d'administration : /adminmenu "mot de passe" ouvre un editeur de toutes les configurations du serveur (config/mmorpg/*.json),
 * reserve aux joueurs qui connaissent le mot de passe (voir {@link AdminAuth}). Toute action du menu est reverifiee cote serveur.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Admin {
    private Admin() {
    }

    public static final int CHUNK = 12000;
    private static final Map<UUID, StringBuilder> UPLOADS = new HashMap<>();

    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        AdminAuth.load();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        AdminAuth.clear();
        UPLOADS.clear();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AdminAuth.logout(event.getEntity().getUUID());
        UPLOADS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static String clean(String arg) {
        String s = arg.strip();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
        return s;
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("adminmenu")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!AdminAuth.authorized(p)) {
                        c.getSource().sendFailure(Component.literal("Utilisation : /adminmenu \"mot de passe\""));
                        return 0;
                    }
                    open(p);
                    return 1;
                })
                .then(Commands.argument("mdp", StringArgumentType.greedyString()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String err = AdminAuth.attempt(p, clean(StringArgumentType.getString(c, "mdp")));
                    if (err != null) {
                        c.getSource().sendFailure(Component.literal(err));
                        return 0;
                    }
                    open(p);
                    return 1;
                })));
        d.register(Commands.literal("adminpass")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                .then(Commands.argument("nouveau", StringArgumentType.greedyString()).executes(c -> {
                    String pw = clean(StringArgumentType.getString(c, "nouveau"));
                    if (pw.length() < 8) {
                        c.getSource().sendFailure(Component.literal("Le mot de passe doit faire au moins 8 caractères."));
                        return 0;
                    }
                    try {
                        AdminAuth.setPassword(pw);
                    } catch (Exception e) {
                        c.getSource().sendFailure(Component.literal("Impossible d'enregistrer le mot de passe : " + e.getMessage()));
                        return 0;
                    }
                    c.getSource().sendSuccess(() -> Component.literal("Mot de passe du menu d'administration modifié (les administrateurs connectés sont déconnectés).").withColor(0x80E080), true);
                    return 1;
                })));
    }

    private static void send(ServerPlayer p, int kind, String file, int index, int count, String text) {
        Net.toPlayer(p, new Payloads.AdminReply(kind, file, index, count, text));
    }

    public static void open(ServerPlayer p) {
        send(p, Payloads.AdminReply.OPEN, "", 0, 1, String.join("\n", AdminConfigs.FILES));
    }

    private static void message(ServerPlayer p, boolean ok, String text) {
        send(p, ok ? Payloads.AdminReply.OK : Payloads.AdminReply.ERROR, "", 0, 1, text);
    }

    public static void handle(ServerPlayer p, Payloads.AdminRequest req) {
        if (!AdminAuth.authorized(p)) {
            MMORPG.LOGGER.warn("[MMORPG] ADMIN : requete refusee de {} (non authentifie)", p.getGameProfile().name());
            return;
        }
        switch (req.action()) {
            case Payloads.AdminRequest.GET -> {
                if (!AdminConfigs.known(req.file())) return;
                try {
                    String text = AdminConfigs.read(req.file());
                    int count = Math.max(1, (text.length() + CHUNK - 1) / CHUNK);
                    for (int i = 0; i < count; i++) send(p, Payloads.AdminReply.FILE, req.file(), i, count, text.substring(i * CHUNK, Math.min(text.length(), (i + 1) * CHUNK)));
                } catch (IOException e) {
                    message(p, false, "Lecture impossible : " + e.getMessage());
                }
            }
            case Payloads.AdminRequest.SAVE -> {
                if (!AdminConfigs.known(req.file())) return;
                StringBuilder sb = req.index() == 0 ? new StringBuilder() : UPLOADS.get(p.getUUID());
                if (sb == null || req.count() < 1 || req.count() > 400) {
                    message(p, false, "Envoi incomplet : recommencez l'enregistrement.");
                    return;
                }
                if (req.index() == 0) UPLOADS.put(p.getUUID(), sb);
                sb.append(req.text());
                if (sb.length() > AdminConfigs.MAX_BYTES) {
                    UPLOADS.remove(p.getUUID());
                    message(p, false, "Fichier trop volumineux.");
                    return;
                }
                if (req.index() + 1 < req.count()) return;
                UPLOADS.remove(p.getUUID());
                String err = AdminConfigs.save(req.file(), sb.toString());
                if (err == null) {
                    MMORPG.LOGGER.info("[MMORPG] ADMIN : {} a modifié {}.json", p.getGameProfile().name(), req.file());
                    message(p, true, req.file() + ".json enregistré et rechargé (ancienne version dans config/mmorpg/sauvegardes).");
                } else {
                    message(p, false, "Refusé : " + err);
                }
            }
            case Payloads.AdminRequest.LOGOUT -> AdminAuth.logout(p.getUUID());
            default -> {
            }
        }
    }
}
