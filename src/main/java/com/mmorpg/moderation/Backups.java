package com.mmorpg.moderation;

import com.mmorpg.MMORPG;
import com.mmorpg.db.InventorySync;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.RpgPlayers;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Sauvegardes de profil : copie complete (donnees RPG, sac, coffre de l'Ender) d'un joueur dans moderation/profils/&lt;uuid&gt;/. Automatiques toutes les
 * 5 minutes, a la connexion/deconnexion et avant chaque modification d'un administrateur ; manuelles a la demande. Servent a restaurer un profil
 * (/profil restaurer) et a annuler les dernieres minutes d'un joueur (/annuler) ; toute restauration prend d'abord une sauvegarde de securite.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Backups {
    private Backups() {
    }

    private static final int KEEP = 40;

    public record Info(long time, String tag, String file) {
    }

    private static Path dir(UUID id) {
        return ModUtil.dir().resolve("profils").resolve(id.toString());
    }

    private static String clean(String tag) {
        String t = tag.replaceAll("[^A-Za-z0-9_-]", "-");
        return t.length() > 24 ? t.substring(0, 24) : t;
    }

    public static boolean snapshot(ServerPlayer p, String tag) {
        try {
            CompoundTag t = new CompoundTag();
            t.putLong("time", System.currentTimeMillis());
            t.putString("tag", tag);
            t.putString("name", p.getGameProfile().name());
            t.put("data", RpgPlayers.get(p).save());
            t.putString("inv", InventorySync.encode(p));
            Path d = dir(p.getUUID());
            Files.createDirectories(d);
            Files.writeString(d.resolve(System.currentTimeMillis() + "_" + clean(tag) + ".snbt"), t.toString(), StandardCharsets.UTF_8);
            prune(d);
            return true;
        } catch (Exception e) {
            MMORPG.LOGGER.warn("[MMORPG] SAUVEGARDE de {} impossible : {}", p.getGameProfile().name(), e.getMessage());
            return false;
        }
    }

    private static void prune(Path d) throws IOException {
        List<Path> files;
        try (var s = Files.list(d)) {
            files = s.sorted(Comparator.comparing((Path f) -> f.getFileName().toString())).toList();
        }
        int extra = files.size() - KEEP;
        for (int i = 0; i < files.size() && extra > 0; i++) {
            String n = files.get(i).getFileName().toString();
            if (n.contains("_auto") || n.contains("_connexion") || n.contains("_deconnexion") || n.contains("_avant-")) {
                Files.deleteIfExists(files.get(i));
                extra--;
            }
        }
    }

    /** Sauvegardes d'un joueur, de la plus recente a la plus ancienne. */
    public static List<Info> list(UUID id) {
        List<Info> out = new ArrayList<>();
        Path d = dir(id);
        if (!Files.isDirectory(d)) return out;
        try (var s = Files.list(d)) {
            s.forEach(f -> {
                String n = f.getFileName().toString();
                int us = n.indexOf('_');
                if (us < 0 || !n.endsWith(".snbt")) return;
                try {
                    out.add(new Info(Long.parseLong(n.substring(0, us)), n.substring(us + 1, n.length() - 5), n));
                } catch (NumberFormatException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
        out.sort(Comparator.comparingLong(Info::time).reversed());
        return out;
    }

    private static CompoundTag read(UUID id, String file) throws Exception {
        if (!file.matches("[0-9]+_[A-Za-z0-9_-]+\\.snbt")) throw new IllegalArgumentException("Sauvegarde invalide.");
        return TagParser.parseCompoundFully(Files.readString(dir(id).resolve(file), StandardCharsets.UTF_8));
    }

    /** Restaure la sauvegarde {@code file} sur le joueur connecte. Renvoie un message (commence par « ! » si echec). */
    public static String restore(String by, ServerPlayer p, String file) {
        try {
            CompoundTag t = read(p.getUUID(), file);
            snapshot(p, "avant-restauration");
            PlayerData live = RpgPlayers.get(p);
            live.load(t.getCompoundOrEmpty("data"));
            if (live.playerClass.isPlayable()) live.unlockSkills();
            RpgPlayers.recomputeAndSync(p);
            boolean inv = InventorySync.apply(p, t.getStringOr("inv", ""));
            ModHistory.record(by, p.getUUID(), p.getGameProfile().name(), "restauration", file + (inv ? "" : " (inventaire non restauré)"));
            ModUtil.tell(p, "Votre profil a été restauré par un administrateur.", 0xFFD27F);
            return "Profil de " + p.getGameProfile().name() + " restauré (" + file + ")." + (inv ? "" : " Inventaire illisible : non modifié.");
        } catch (IllegalArgumentException e) {
            return "!" + e.getMessage();
        } catch (Exception e) {
            return "!Restauration impossible : " + e.getClass().getSimpleName();
        }
    }

    /** Annule les {@code minutes} dernieres minutes : restaure la plus recente sauvegarde anterieure. */
    public static String undo(String by, ServerPlayer p, int minutes) {
        long limit = System.currentTimeMillis() - minutes * 60_000L;
        for (Info i : list(p.getUUID())) {
            if (i.time() <= limit && !i.tag().startsWith("avant-restauration") && !i.tag().startsWith("avant-annulation")) {
                String r = restore(by, p, i.file());
                if (!r.startsWith("!")) {
                    ModHistory.record(by, p.getUUID(), p.getGameProfile().name(), "annulation", minutes + " min annulées");
                    return "Dernières " + minutes + " min de " + p.getGameProfile().name() + " annulées (retour à " + java.time.Instant.ofEpochMilli(i.time()) + ").";
                }
                return r;
            }
        }
        return "!Aucune sauvegarde assez ancienne (" + minutes + " min) pour ce joueur.";
    }

    // ------------------------------------------------------------------ evenements

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 6000 != 3000) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) snapshot(p, "auto");
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) snapshot(p, "connexion");
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) snapshot(p, "deconnexion");
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        var d = event.getDispatcher();
        var profil = Commands.literal("profil").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        profil.then(Commands.literal("sauvegarder").then(Commands.argument("pseudo", StringArgumentType.string()).executes(c -> {
            ServerPlayer p = target(c.getSource(), StringArgumentType.getString(c, "pseudo"));
            if (p == null) return 0;
            return reply(c.getSource(), snapshot(p, "manuel") ? "Profil de " + p.getGameProfile().name() + " sauvegardé." : "!Sauvegarde impossible.");
        })));
        profil.then(Commands.literal("liste").then(Commands.argument("pseudo", StringArgumentType.string()).executes(c -> {
            UUID id = Jail.idOf(c.getSource().getServer(), StringArgumentType.getString(c, "pseudo"));
            if (id == null) return reply(c.getSource(), "!Joueur inconnu.");
            List<Info> l = list(id);
            for (int i = 0; i < Math.min(15, l.size()); i++) {
                Info x = l.get(i);
                c.getSource().sendSuccess(() -> Component.literal(x.file() + "  (" + java.time.Instant.ofEpochMilli(x.time()) + ")"), false);
            }
            return l.size();
        })));
        profil.then(Commands.literal("restaurer").then(Commands.argument("pseudo", StringArgumentType.string()).then(Commands.argument("fichier", StringArgumentType.string()).executes(c -> {
            ServerPlayer p = target(c.getSource(), StringArgumentType.getString(c, "pseudo"));
            if (p == null) return 0;
            return reply(c.getSource(), restore(c.getSource().getTextName(), p, StringArgumentType.getString(c, "fichier")));
        }))));
        d.register(profil);
        d.register(Commands.literal("annuler").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("pseudo", StringArgumentType.string()).then(Commands.argument("minutes", IntegerArgumentType.integer(1, 1440)).executes(c -> {
                    ServerPlayer p = target(c.getSource(), StringArgumentType.getString(c, "pseudo"));
                    if (p == null) return 0;
                    return reply(c.getSource(), undo(c.getSource().getTextName(), p, IntegerArgumentType.getInteger(c, "minutes")));
                }))));
    }

    private static ServerPlayer target(CommandSourceStack src, String name) {
        ServerPlayer p = ModUtil.online(src.getServer(), name);
        if (p == null) src.sendFailure(Component.literal("Le joueur doit être connecté."));
        return p;
    }

    private static int reply(CommandSourceStack src, String msg) {
        if (msg.startsWith("!")) {
            src.sendFailure(Component.literal(msg.substring(1)));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }
}
