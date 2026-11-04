package com.mmorpg.security;

import com.mmorpg.MMORPG;
import com.mmorpg.admin.PlayerAdmin;
import com.mmorpg.anticheat.AntiCheat;
import com.mmorpg.db.Audit;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Commandes : limite de debit par joueur (rafales refusees, signalees a l'anticheat) et journal d'audit de toutes les commandes
 * des operateurs et de la console. {@code /audit [n]} relit les dernieres lignes.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class CommandGuard {
    private CommandGuard() {
    }

    /** Rafale tolerée et recharge par seconde. */
    static final double CAPACITY = 8, REFILL = 2;
    private static final Map<UUID, double[]> BUCKETS = new ConcurrentHashMap<>();

    /** Consomme un jeton ; faux si le joueur depasse la limite. */
    public static boolean take(UUID id, long nowNanos) {
        double[] b = BUCKETS.computeIfAbsent(id, k -> new double[]{CAPACITY, nowNanos});
        synchronized (b) {
            b[0] = Math.min(CAPACITY, b[0] + (nowNanos - b[1]) / 1e9 * REFILL);
            b[1] = nowNanos;
            if (b[0] < 1) return false;
            b[0]--;
            return true;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void command(CommandEvent e) {
        var src = e.getParseResults().getContext().getSource();
        String line = e.getParseResults().getReader().getString();
        ServerPlayer p = src.getPlayer();
        boolean admin = p == null || PlayerAdmin.allowed(p);
        if (!admin) {
            if (AntiCheat.on(p) && !take(p.getUUID(), System.nanoTime())) {
                e.setCanceled(true);
                AntiCheat.flag(p, AntiCheat.Check.COMMAND_RATE, line.length() > 40 ? line.substring(0, 40) : line);
            }
            return;
        }
        String root = line.startsWith("/") ? line.substring(1) : line;
        String first = root.split(" ", 2)[0].toLowerCase();
        if (first.equals("audit")) return;                                          // la consultation n'est pas journalisee
        if (first.equals("login") || first.equals("register") || first.equals("l")) root = first + " ***";
        Audit.log(p == null ? null : p.getUUID(), p == null ? "console" : p.getGameProfile().name(), Audit.ADMIN, 0, "/" + root);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        BUCKETS.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("audit").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> show(c.getSource(), 10))
                .then(Commands.argument("n", IntegerArgumentType.integer(1, 50)).executes(c -> show(c.getSource(), IntegerArgumentType.getInteger(c, "n")))));
    }

    private static int show(net.minecraft.commands.CommandSourceStack src, int n) {
        var lines = AdminJournal.tail(n);
        src.sendSuccess(() -> Component.literal(lines.isEmpty() ? "Journal d'audit vide." : String.join("\n", lines)), false);
        return lines.size();
    }
}
