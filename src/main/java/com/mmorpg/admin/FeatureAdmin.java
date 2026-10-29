package com.mmorpg.admin;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.db.Audit;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.server.FeatureFlags;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** Ecran d'administration des fonctionnalites (/fonctionnalites) : permission d'operateur reverifiee a chaque action, changements journalises. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class FeatureAdmin {
    private FeatureAdmin() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fonctionnalites").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
            open(c.getSource().getPlayerOrException());
            return 1;
        }));
    }

    public static void open(ServerPlayer p) {
        if (PlayerAdmin.allowed(p)) reply(p, state(p, "", true));
    }

    public static void action(ServerPlayer admin, CompoundTag d) {
        if (!PlayerAdmin.allowed(admin)) {
            MMORPG.LOGGER.warn("[MMORPG] ADMIN-FONCTIONNALITES : requete refusee de {} (pas operateur)", admin.getGameProfile().name());
            return;
        }
        reply(admin, state(admin, d.getStringOr("action", "").equals("toggle") ? toggle(admin, d) : "", true));
    }

    /** Applique un changement et renvoie le message d'etat ; sans effet de bord si la cle est inconnue. */
    public static String toggle(ServerPlayer admin, CompoundTag d) {
        String key = d.getStringOr("key", "");
        if (!FeatureFlags.KEYS.contains(key)) return "Fonctionnalité inconnue.";
        boolean value = d.getBooleanOr("value", true);
        var server = admin.level().getServer();
        if (FeatureFlags.enabled(server, key) == value) return "";
        FeatureFlags.set(server, key, value);
        String line = Instant.now() + " " + admin.getGameProfile().name() + " : fonctionnalité " + key + " -> " + (value ? "activée" : "désactivée");
        MMORPG.LOGGER.info("[MMORPG] ADMIN-FONCTIONNALITES : {}", line);
        try {
            Files.createDirectories(ConfigManager.dir());
            Files.writeString(ConfigManager.dir().resolve("admin-actions.log"), line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            MMORPG.LOGGER.warn("[MMORPG] admin-actions.log non ecrit : {}", e.getMessage());
        }
        Audit.log(admin, Audit.ADMIN, 0, "fonctionnalité " + key + " " + (value ? "activée" : "désactivée"));
        return key + (value ? " activée" : " désactivée");
    }

    public static CompoundTag state(ServerPlayer p, String status, boolean ok) {
        CompoundTag t = new CompoundTag();
        t.putString("status", status);
        t.putBoolean("ok", ok);
        CompoundTag flags = new CompoundTag();
        for (String key : FeatureFlags.KEYS) flags.putBoolean(key, FeatureFlags.enabled(p.level().getServer(), key));
        t.put("flags", flags);
        return t;
    }

    private static void reply(ServerPlayer p, CompoundTag t) {
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.ADMIN_FEATURES, t));
    }
}
