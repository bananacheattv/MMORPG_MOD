package com.mmorpg.admin;

import com.mmorpg.MMORPG;
import com.mmorpg.network.SharedLedger;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.*;

/** /report <joueur> <raison> : signalements stockés dans le registre partagé, consultés par les modérateurs avec /signalements. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Reports {
    private static final int KEPT = 500, COOLDOWN_MS = 60_000, MAX_REASON = 200;
    private static final Map<UUID, Long> LAST = new HashMap<>();

    public static final class Item { public int id; public String reporter = "", reporterId = "", target = "", reason = "", status = "ouvert", handledBy = ""; public long time; }
    public static final class State { public int next = 1; public List<Item> items = new ArrayList<>(); }

    private Reports() {}

    private static <T> T access(net.minecraft.server.MinecraftServer s, java.util.function.Function<State, T> f) {
        return SharedLedger.access(s, "reports", State.class, State::new, f);
    }

    public static int file(ServerPlayer p, String target, String reason) {
        reason = reason.trim();
        if (reason.isEmpty()) throw new IllegalArgumentException("Indique une raison.");
        if (reason.length() > MAX_REASON) reason = reason.substring(0, MAX_REASON);
        long now = System.currentTimeMillis();
        if (now - LAST.getOrDefault(p.getUUID(), 0L) < COOLDOWN_MS) throw new IllegalArgumentException("Attends une minute entre deux signalements.");
        if (target.equalsIgnoreCase(p.getGameProfile().name())) throw new IllegalArgumentException("Tu ne peux pas te signaler toi-même.");
        LAST.put(p.getUUID(), now);
        final String reasonText = reason.replace('\n', ' ');
        int id = access(p.level().getServer(), st -> {
            var it = new Item();
            it.id = st.next++;
            it.reporter = p.getGameProfile().name(); it.reporterId = p.getUUID().toString();
            it.target = target; it.reason = reasonText; it.time = now;
            st.items.add(it);
            while (st.items.size() > KEPT) st.items.remove(0);
            return it.id;
        });
        MMORPG.LOGGER.info("Signalement #{} : {} -> {} : {}", id, p.getGameProfile().name(), target, reasonText);
        for (var op : p.level().getServer().getPlayerList().getPlayers())
            if (op.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                op.sendSystemMessage(Component.literal("[Signalement #" + id + "] " + p.getGameProfile().name() + " → " + target + " : " + reasonText));
        return id;
    }

    private static String format(Item it) {
        String date = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(java.time.ZoneId.of("Europe/Brussels")).format(java.time.Instant.ofEpochMilli(it.time));
        return "#" + it.id + " [" + it.status + "] " + date + " " + it.reporter + " → " + it.target + " : " + it.reason + (it.handledBy.isEmpty() ? "" : " (par " + it.handledBy + ")");
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("report")
                .executes(c -> { c.getSource().sendSuccess(() -> Component.literal("/report <joueur> <raison>"), false); return 1; })
                .then(Commands.argument("joueur", StringArgumentType.word())
                        .then(Commands.argument("raison", StringArgumentType.greedyString()).executes(c -> {
                            var p = c.getSource().getPlayerOrException();
                            try {
                                int id = file(p, StringArgumentType.getString(c, "joueur"), StringArgumentType.getString(c, "raison"));
                                c.getSource().sendSuccess(() -> Component.literal("Signalement #" + id + " envoyé aux modérateurs. Merci."), false);
                                return 1;
                            } catch (IllegalArgumentException ex) { c.getSource().sendFailure(Component.literal(ex.getMessage())); return 0; }
                        }))));
        var admin = Commands.literal("signalements").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> list(c.getSource(), "ouvert", null))
                .then(Commands.literal("tous").executes(c -> list(c.getSource(), null, null)))
                .then(Commands.literal("joueur").then(Commands.argument("nom", StringArgumentType.word()).executes(c -> list(c.getSource(), null, StringArgumentType.getString(c, "nom")))))
                .then(Commands.literal("traiter").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> close(c.getSource(), IntegerArgumentType.getInteger(c, "id"), "traité")))
                )
                .then(Commands.literal("rejeter").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> close(c.getSource(), IntegerArgumentType.getInteger(c, "id"), "rejeté"))));
        e.getDispatcher().register(admin);
    }

    private static int list(net.minecraft.commands.CommandSourceStack src, String status, String who) {
        var items = access(src.getServer(), st -> new ArrayList<>(st.items));
        int shown = 0;
        for (int i = items.size() - 1; i >= 0 && shown < 15; i--) {
            var it = items.get(i);
            if (status != null && !it.status.equals(status)) continue;
            if (who != null && !it.target.equalsIgnoreCase(who) && !it.reporter.equalsIgnoreCase(who)) continue;
            shown++;
            String line = format(it);
            src.sendSuccess(() -> Component.literal(line), false);
        }
        if (shown == 0) src.sendSuccess(() -> Component.literal("Aucun signalement."), false);
        return shown;
    }

    private static int close(net.minecraft.commands.CommandSourceStack src, int id, String status) {
        String by = src.getTextName();
        boolean ok = access(src.getServer(), st -> {
            for (var it : st.items) if (it.id == id) { it.status = status; it.handledBy = by; return true; }
            return false;
        });
        if (!ok) { src.sendFailure(Component.literal("Signalement introuvable.")); return 0; }
        src.sendSuccess(() -> Component.literal("Signalement #" + id + " " + status + "."), true);
        return 1;
    }
}
