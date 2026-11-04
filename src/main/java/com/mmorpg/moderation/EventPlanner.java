package com.mmorpg.moderation;

import com.google.gson.reflect.TypeToken;
import com.mmorpg.MMORPG;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Planificateur d'evenements : {@code /planifier annonce|commande <delai> [repetition] <texte>}. Une annonce est diffusee a tous (prevenue 5 min et
 * 1 min avant), une commande est executee par la console. Une tache peut se repeter (« repete=2h »). Sauvegarde dans moderation/planning.json.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class EventPlanner {
    private EventPlanner() {
    }

    public static final class Task {
        public int id;
        public String type = "annonce";
        public String text = "";
        public long at;
        public long repeat;
        public String by = "";
        public boolean warned5, warned1;
    }

    private static List<Task> tasks;
    private static int nextId = 1;

    private static synchronized List<Task> tasks() {
        if (tasks == null) {
            tasks = ModUtil.read("planning.json", new TypeToken<ArrayList<Task>>() {
            }.getType(), new ArrayList<Task>());
            for (Task t : tasks) nextId = Math.max(nextId, t.id + 1);
        }
        return tasks;
    }

    private static void save() {
        ModUtil.write("planning.json", tasks());
    }

    public static synchronized List<Task> list() {
        return new ArrayList<>(tasks());
    }

    public static synchronized String add(String by, String type, long delayMs, long repeatMs, String text) {
        if (!type.equals("annonce") && !type.equals("commande")) return "!Type inconnu (annonce ou commande).";
        if (delayMs <= 0) return "!Délai invalide (exemples : 10m, 2h, 1d).";
        String t = text.strip();
        if (t.isEmpty() || t.length() > 200) return "!Texte vide ou trop long (200 caractères).";
        if (type.equals("commande") && t.startsWith("/")) t = t.substring(1);
        Task k = new Task();
        k.id = nextId++;
        k.type = type;
        k.text = t;
        k.at = System.currentTimeMillis() + delayMs;
        k.repeat = Math.max(0, repeatMs);
        k.by = by;
        tasks().add(k);
        save();
        ModHistory.record(by, null, "-", "planifié", type + " #" + k.id + " dans " + ModUtil.formatDuration(delayMs) + " : " + t);
        return "Tâche #" + k.id + " planifiée dans " + ModUtil.formatDuration(delayMs) + ".";
    }

    public static synchronized String remove(String by, int id) {
        boolean ok = tasks().removeIf(t -> t.id == id);
        if (ok) {
            save();
            ModHistory.record(by, null, "-", "planifié", "tâche #" + id + " supprimée");
        }
        return ok ? "Tâche #" + id + " supprimée." : "!Tâche introuvable.";
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        List<Task> due = new ArrayList<>();
        List<String> warns = new ArrayList<>();
        long now = System.currentTimeMillis();
        synchronized (EventPlanner.class) {
            boolean dirty = false;
            for (Task t : new ArrayList<>(tasks())) {
                long rem = t.at - now;
                if (rem <= 0) {
                    due.add(t);
                    if (t.repeat > 0) {
                        t.at = now + t.repeat;
                        t.warned5 = t.warned1 = false;
                    } else tasks().remove(t);
                    dirty = true;
                } else if (t.type.equals("annonce")) {
                    if (rem <= 60_000 && !t.warned1) {
                        t.warned1 = t.warned5 = true;
                        warns.add("Dans 1 minute : " + t.text);
                    } else if (rem <= 300_000 && !t.warned5) {
                        t.warned5 = true;
                        warns.add("Dans 5 minutes : " + t.text);
                    }
                }
            }
            if (dirty) save();
        }
        for (String w : warns) ModUtil.broadcast(server, "📅 " + w, 0xFFD27F);
        for (Task t : due) {
            if (t.type.equals("annonce")) ModUtil.broadcast(server, "📣 " + t.text, 0xFFE080);
            else {
                try {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), t.text);
                } catch (Exception e) {
                    MMORPG.LOGGER.warn("[MMORPG] PLANIFICATEUR : commande « {} » en echec : {}", t.text, e.getMessage());
                }
            }
            Journal.add(null, "planificateur", "planifié", 0, t.type + " #" + t.id + " : " + t.text);
        }
    }

    @SubscribeEvent
    public static void onStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        tasks = null;
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        var root = Commands.literal("planifier").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (String type : new String[]{"annonce", "commande"}) {
            root.then(Commands.literal(type).then(Commands.argument("delai", StringArgumentType.string())
                    .then(Commands.argument("texte", StringArgumentType.greedyString()).executes(c -> reply(c.getSource(),
                            add(c.getSource().getTextName(), type, ModUtil.parseDuration(StringArgumentType.getString(c, "delai")), 0, StringArgumentType.getString(c, "texte")))))
                    .then(Commands.literal("repete").then(Commands.argument("intervalle", StringArgumentType.string())
                            .then(Commands.argument("texte", StringArgumentType.greedyString()).executes(c -> {
                                long every = ModUtil.parseDuration(StringArgumentType.getString(c, "intervalle"));
                                return reply(c.getSource(), every < 60_000 ? "!Intervalle minimum : 1m." : add(c.getSource().getTextName(), type,
                                        ModUtil.parseDuration(StringArgumentType.getString(c, "delai")), every, StringArgumentType.getString(c, "texte")));
                            }))))));
        }
        root.then(Commands.literal("liste").executes(c -> {
            List<Task> l = list();
            if (l.isEmpty()) c.getSource().sendSuccess(() -> Component.literal("Aucune tâche planifiée."), false);
            for (Task t : l) c.getSource().sendSuccess(() -> Component.literal("#" + t.id + " " + t.type + " dans " + ModUtil.formatDuration(t.at - System.currentTimeMillis())
                    + (t.repeat > 0 ? " (tous les " + ModUtil.formatDuration(t.repeat) + ")" : "") + " : " + t.text), false);
            return l.size();
        }));
        root.then(Commands.literal("supprimer").then(Commands.argument("numero", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                .executes(c -> reply(c.getSource(), remove(c.getSource().getTextName(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "numero"))))));
        event.getDispatcher().register(root);
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
