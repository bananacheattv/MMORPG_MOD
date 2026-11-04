package com.mmorpg.moderation;

import com.google.gson.reflect.TypeToken;
import com.mmorpg.MMORPG;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Mode maintenance : {@code /maintenance <temps> [raison]} lance un compte a rebours annonce a tous, puis expulse les joueurs non operateurs
 * et refuse leur connexion jusqu'a {@code /maintenance off}. L'etat est conserve au redemarrage (moderation/maintenance.json).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Maintenance {
    private Maintenance() {
    }

    private static final class State {
        /** Heure (epoch ms) de debut de la maintenance, 0 = aucune. */
        long startAt;
        boolean active;
        String reason = "";
    }

    private static State state;
    private static long lastAnnounce = Long.MAX_VALUE;
    private static final long[] MARKS = {3600, 1800, 600, 300, 120, 60, 30, 10, 5, 4, 3, 2, 1};

    private static synchronized State state() {
        if (state == null) state = ModUtil.read("maintenance.json", new TypeToken<State>() {
        }.getType(), new State());
        return state;
    }

    private static void save() {
        ModUtil.write("maintenance.json", state());
    }

    public static synchronized boolean active() {
        return state().active;
    }

    public static synchronized boolean counting() {
        return !state().active && state().startAt > 0;
    }

    public static synchronized long remainingMs() {
        return Math.max(0, state().startAt - System.currentTimeMillis());
    }

    public static synchronized String reason() {
        return state().reason;
    }

    public static synchronized String schedule(String by, long ms, String reason) {
        State s = state();
        s.startAt = System.currentTimeMillis() + ms;
        s.active = false;
        s.reason = reason == null ? "" : reason;
        lastAnnounce = Long.MAX_VALUE;
        save();
        ModHistory.record(by, null, "-", "maintenance", "dans " + ModUtil.formatDuration(ms) + (s.reason.isEmpty() ? "" : " : " + s.reason));
        return "Maintenance dans " + ModUtil.formatDuration(ms) + ".";
    }

    public static synchronized String cancel(String by) {
        State s = state();
        boolean was = s.active || s.startAt > 0;
        s.active = false;
        s.startAt = 0;
        s.reason = "";
        lastAnnounce = Long.MAX_VALUE;
        save();
        if (was) ModHistory.record(by, null, "-", "maintenance", "terminée / annulée");
        return was ? "Maintenance terminée : les connexions sont rouvertes." : "!Aucune maintenance en cours.";
    }

    private static String kickMessage() {
        String r = reason();
        return "Serveur en maintenance" + (r.isEmpty() ? "." : " : " + r);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 10 != 0) return;
        long rem;
        boolean act;
        synchronized (Maintenance.class) {
            State s = state();
            if (s.active || s.startAt <= 0) return;
            rem = s.startAt - System.currentTimeMillis();
            act = rem <= 0;
            if (act) {
                s.active = true;
                save();
            }
        }
        if (act) {
            for (ServerPlayer p : new java.util.ArrayList<>(server.getPlayerList().getPlayers())) {
                if (!ModUtil.staff(p)) p.connection.disconnect(Component.literal(kickMessage()));
            }
            ModUtil.broadcast(server, "Maintenance en cours : seuls les opérateurs peuvent se connecter.", 0xFFB347);
            return;
        }
        long secs = (rem + 999) / 1000;
        for (long m : MARKS) {
            if (secs <= m && lastAnnounce > m) {
                lastAnnounce = m;
                String t = ModUtil.formatDuration(m * 1000);
                String r = reason();
                ModUtil.broadcast(server, "⚠ Maintenance dans " + t + (r.isEmpty() ? "" : " : " + r) + ". Terminez ce que vous faites.", 0xFFB347);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && active() && !ModUtil.staff(p)) p.connection.disconnect(Component.literal(kickMessage()));
    }

    @SubscribeEvent
    public static void onStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        state = null;
        lastAnnounce = Long.MAX_VALUE;
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("maintenance").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("off").executes(c -> reply(c.getSource(), cancel(c.getSource().getTextName()))))
                .then(Commands.literal("statut").executes(c -> reply(c.getSource(), active() ? "Maintenance active." : counting()
                        ? "Maintenance dans " + ModUtil.formatDuration(remainingMs()) + "." : "Aucune maintenance.")))
                .then(Commands.argument("temps", StringArgumentType.string())
                        .executes(c -> start(c.getSource(), StringArgumentType.getString(c, "temps"), ""))
                        .then(Commands.argument("raison", StringArgumentType.greedyString())
                                .executes(c -> start(c.getSource(), StringArgumentType.getString(c, "temps"), StringArgumentType.getString(c, "raison"))))));
    }

    private static int start(CommandSourceStack src, String time, String reason) {
        long ms = time.equalsIgnoreCase("now") || time.equalsIgnoreCase("maintenant") ? 1000 : ModUtil.parseDuration(time);
        return reply(src, ms < 0 ? "!Durée invalide (exemples : 10m, 1h, maintenant)." : schedule(src.getTextName(), ms, reason));
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
