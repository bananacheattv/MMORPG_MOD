package com.mmorpg.guild;

import com.google.gson.Gson;
import com.mmorpg.MMORPG;
import com.mmorpg.network.SharedLedger;
import com.mmorpg.server.FeatureFlags;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.Files;
import java.util.*;

/** Chat de guilde partage entre tous les serveurs du reseau (registre de coordination « guild-chat »). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class GuildChat {
    public static final class Message { public long id, time; public String guild, name, rank, text, title, rankLabel;
        public int rankColor; }
    public static final class State { public long next = 1; public List<Message> messages = new ArrayList<>(); }
    private static final int MAX_LENGTH = 200, KEPT = 300;
    private static final long COOLDOWN_MS = 1000;
    private static final Gson JSON = new Gson();
    private static final Map<UUID, Long> LAST_SEND = new HashMap<>();
    private static long seen = -1;
    private static final com.mmorpg.db.SharedDocs.Poller POLLER = new com.mmorpg.db.SharedDocs.Poller("ledger-guild-chat");

    private GuildChat() {}

    public static void send(ServerPlayer p, String raw) {
        if (!FeatureFlags.allow(p, "guildes") || !FeatureFlags.allow(p, "chat_guilde")) return;
        String text = raw.replaceAll("\\p{Cntrl}", " ").replace('§', ' ').trim();
        if (text.isEmpty()) return;
        if (text.length() > MAX_LENGTH) text = text.substring(0, MAX_LENGTH);
        long now = System.currentTimeMillis();
        if (now - LAST_SEND.getOrDefault(p.getUUID(), 0L) < COOLDOWN_MS) {
            p.sendSystemMessage(Component.literal("Doucement : un message par seconde.").withColor(0xFFAA77));
            return;
        }
        MinecraftServer server = p.level().getServer();
        try {
            String[] info = GuildStore.access(server, false, s -> {
                var g = GuildStore.of(s, p.getUUID());
                return g == null ? null : new String[]{g.id.toString(), g.members.get(p.getUUID()).rank};
            });
            if (info == null) {
                p.sendSystemMessage(Component.literal("Tu n’as pas de guilde.").withColor(0xFFAA77));
                return;
            }
            LAST_SEND.put(p.getUUID(), now);
            String message = text;
            SharedLedger.access(server, "guild-chat", State.class, State::new, st -> {
                var m = new Message();
                m.id = st.next++;
                m.time = System.currentTimeMillis();
                m.guild = info[0];
                m.rank = info[1];
                m.name = p.getName().getString();
                var pd = com.mmorpg.server.RpgPlayers.get(p);
                m.title = pd.pvpTitle;
                m.rankLabel = pd.rankLabel;
                m.rankColor = pd.rankColor;
                m.text = message;
                st.messages.add(m);
                while (st.messages.size() > KEPT) st.messages.removeFirst();
                return null;
            });
        } catch (Exception e) {
            p.sendSystemMessage(Component.literal("Chat de guilde indisponible : " + e.getMessage()).withColor(0xFFAA77));
        }
    }

    /** Etat du chat ; {@code null} tant que la lecture en base (en arriere-plan, jamais d'attente) n'est pas arrivee. */
    private static State read(MinecraftServer server) throws java.io.IOException {
        if (com.mmorpg.db.SharedDocs.useDb()) {
            // send() ecrit dans la base (registre partage) : le fichier n'est plus que l'ancienne copie a importer
            var doc = POLLER.poll();
            if (doc == null) return null;
            if (doc.data() != null) {
                State s = JSON.fromJson(doc.data(), State.class);
                return s == null ? new State() : s;
            }
        }
        var file = GuildStore.directory(server).resolveSibling("coordination").resolve("guild-chat.json");
        if (!Files.exists(file)) return new State();
        State s = JSON.fromJson(Files.readString(file), State.class);
        return s == null ? new State() : s;
    }

    /** Distribue les nouveaux messages aux membres connectes ici ; rend leur nombre, -1 si l'etat n'est pas encore lisible. */
    public static int poll(MinecraftServer server) throws java.io.IOException {
        State state = read(server);
        if (state == null) return -1;
        if (seen < 0) { seen = state.next - 1; return 0; }
        long from = seen;
        var fresh = state.messages.stream().filter(m -> m.id > from).toList();
        seen = Math.max(seen, state.next - 1);
        if (fresh.isEmpty()) return 0;
        Map<UUID, String> guildOf = GuildStore.access(server, false, s -> {
            var out = new HashMap<UUID, String>();
            for (var g : s.guilds) for (UUID id : g.members.keySet()) out.put(id, g.id.toString());
            return out;
        });
        for (var m : fresh) {
            Component line = Component.literal("[Guilde] ").withColor(0x4FD1A5)
                    .append(Component.literal("CHEF".equals(m.rank) ? "★ " : "OFFICIER".equals(m.rank) ? "✦ " : "").withColor(0xE8D9FF))
                    .append(m.rankLabel == null ? Component.literal(m.name).withColor(0xE8D9FF)
                            : com.mmorpg.achievement.Achievements.chatName(m.rankLabel, m.rankColor, m.name, m.title))
                    .append(Component.literal(" : ").withColor(0xFFFFFF))
                    .append(com.mmorpg.achievement.Achievements.body(m.rankLabel, m.text, 0xFFFFFF));
            for (ServerPlayer p : server.getPlayerList().getPlayers())
                if (m.guild.equals(guildOf.get(p.getUUID()))) p.sendSystemMessage(line);
        }
        return fresh.size();
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 10 != 0 || server.getPlayerList().getPlayers().isEmpty()) return;
        try {
            poll(server);
        } catch (Exception ex) {
            MMORPG.LOGGER.warn("Guild chat unavailable: {}", ex.getMessage());
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        var message = Commands.argument("message", StringArgumentType.greedyString())
                .executes(c -> { send(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "message")); return 1; });
        e.getDispatcher().register(Commands.literal("g").then(message));
        e.getDispatcher().register(Commands.literal("guilde").then(Commands.literal("chat").then(
                Commands.argument("message", StringArgumentType.greedyString())
                        .executes(c -> { send(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "message")); return 1; }))));
    }
}
