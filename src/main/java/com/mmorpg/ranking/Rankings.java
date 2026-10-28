package com.mmorpg.ranking;

import com.mmorpg.MMORPG;
import com.mmorpg.guild.GuildStore;
import com.mmorpg.network.SharedLedger;
import com.mmorpg.server.RpgPlayers;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/**
 * Classements persistants et partagés entre serveurs (registre « rankings »). Chaque serveur y écrit ses joueurs en ligne
 * et l'état des guildes toutes les 10 s ; les panneaux lisent le résultat mis en cache.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Rankings {
    public static final int TOP = 10;
    public static final class Row { public String name = ""; public long level, xp, monsters, wins, frags, bank, bosses, members; }
    public static final class State { public Map<String, Row> players = new HashMap<>(), guilds = new HashMap<>(); }
    /** value : valeur principale ; tie : départage ; text : valeur affichée. */
    public record Entry(String id, String name, long value, long tie, String text) {}

    private static volatile Map<RankingKind, List<Entry>> cache = Map.of();

    private Rankings() {}

    public static List<Entry> top(RankingKind kind) {
        return cache.getOrDefault(kind, List.of());
    }

    public static List<String> lines(RankingKind kind) {
        var out = new ArrayList<String>();
        for (var e : top(kind)) out.add(e.name() + "\t" + e.text());
        return out;
    }

    private static void sample(MinecraftServer server) {
        var guilds = new HashMap<String, Row>();
        try {
            GuildStore.access(server, false, s -> {
                for (var g : s.guilds) {
                    var r = new Row();
                    r.name = g.name; r.level = g.level; r.xp = g.xp; r.bank = g.gold;
                    r.bosses = Integer.bitCount(g.bosses); r.members = g.members.size();
                    guilds.put(g.id.toString(), r);
                }
                return null;
            });
        } catch (Exception e) {
            guilds.clear();
            MMORPG.LOGGER.warn("Rankings: guilds unavailable: {}", e.getMessage());
        }
        var online = new ArrayList<ServerPlayer>(server.getPlayerList().getPlayers());
        var result = SharedLedger.access(server, "rankings", State.class, State::new, st -> {
            for (var p : online) {
                var d = RpgPlayers.get(p);
                var r = st.players.computeIfAbsent(p.getUUID().toString(), k -> new Row());
                r.name = p.getName().getString();
                r.level = d.level;
                r.xp = d.xp;
                r.monsters = d.kills.values().stream().mapToLong(Integer::longValue).sum();
            }
            if (!guilds.isEmpty()) st.guilds = guilds;
            return build(st);
        });
        cache = result;
    }

    /** Fin d'un match d'arène classé (appelé une seule fois par joueur et par match). */
    public static void arena(MinecraftServer server, UUID player, String name, int kills, boolean won) {
        try {
            cache = SharedLedger.access(server, "rankings", State.class, State::new, st -> {
                var r = st.players.computeIfAbsent(player.toString(), k -> new Row());
                if (r.name.isEmpty()) r.name = name;
                r.frags += kills;
                if (won) r.wins++;
                return build(st);
            });
        } catch (Exception e) {
            MMORPG.LOGGER.warn("Rankings: arena result lost: {}", e.getMessage());
        }
    }

    private static Map<RankingKind, List<Entry>> build(State st) {
        var out = new EnumMap<RankingKind, List<Entry>>(RankingKind.class);
        for (var kind : RankingKind.values()) {
            var list = new ArrayList<Entry>();
            for (var en : (kind.guild ? st.guilds : st.players).entrySet()) {
                var r = en.getValue();
                Entry e = switch (kind) {
                    case NIVEAU -> new Entry(en.getKey(), r.name, r.level, r.xp, "Niv. " + r.level);
                    case MONSTRES -> new Entry(en.getKey(), r.name, r.monsters, 0, String.valueOf(r.monsters));
                    case ARENE -> new Entry(en.getKey(), r.name, r.wins, r.frags, r.wins + " V · " + r.frags + " frags");
                    case GUILDE_NIVEAU -> new Entry(en.getKey(), r.name, r.level, r.xp, "Niv. " + r.level);
                    case GUILDE_BANQUE -> new Entry(en.getKey(), r.name, r.bank, 0, r.bank + " or");
                    case GUILDE_GARDIENS -> new Entry(en.getKey(), r.name, r.bosses, r.level, r.bosses + " / 10");
                };
                if (e.value() > 0 || kind == RankingKind.NIVEAU || kind == RankingKind.GUILDE_NIVEAU) list.add(e);
            }
            list.sort(Comparator.comparingLong(Entry::value).thenComparingLong(Entry::tie).reversed().thenComparing(Entry::name));
            out.put(kind, List.copyOf(list));
        }
        return out;
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 200 != 0) return;
        try {
            sample(e.getServer());
        } catch (Exception ex) {
            MMORPG.LOGGER.warn("Rankings unavailable: {}", ex.getMessage());
        }
    }

    /** Affiche le top 10 et le rang du joueur dans le chat. */
    public static void show(ServerPlayer p, RankingKind kind) {
        var list = top(kind);
        p.sendSystemMessage(Component.literal("— " + kind.label + " —").withColor(kind.color));
        if (list.isEmpty()) p.sendSystemMessage(Component.literal("Aucune donnée pour l’instant.").withColor(0xB9AFCC));
        String own = kind.guild ? ownGuild(p) : p.getUUID().toString();
        for (int i = 0; i < list.size(); i++) {
            var e = list.get(i);
            boolean mine = e.id().equals(own);
            if (i >= TOP && !mine) continue;
            p.sendSystemMessage(Component.literal((i + 1) + ". " + e.name() + " · " + e.text()).withColor(mine ? 0x7FFFD4 : i == 0 ? 0xFFD040 : 0xFFFFFF));
        }
    }

    private static String ownGuild(ServerPlayer p) {
        try {
            String id = GuildStore.access(p.level().getServer(), false, s -> {
                var g = GuildStore.of(s, p.getUUID());
                return g == null ? "" : g.id.toString();
            });
            return id;
        } catch (Exception e) {
            return "";
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        var root = Commands.literal("classement").executes(c -> {
            var p = c.getSource().getPlayerOrException();
            p.sendSystemMessage(Component.literal("Classements : " + String.join(", ", Arrays.stream(RankingKind.values()).map(k -> k.id).toList()) + ". Exemple : /classement niveau").withColor(0xB9AFCC));
            return 1;
        });
        root.then(Commands.argument("type", StringArgumentType.word())
                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(Arrays.stream(RankingKind.values()).map(k -> k.id), b))
                .executes(c -> {
                    var kind = RankingKind.byId(StringArgumentType.getString(c, "type"));
                    if (kind == null) { c.getSource().sendFailure(Component.literal("Classement inconnu.")); return 0; }
                    show(c.getSource().getPlayerOrException(), kind);
                    return 1;
                }));
        e.getDispatcher().register(root);
    }
}
