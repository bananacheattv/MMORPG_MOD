package com.mmorpg.ranking;

import com.mmorpg.MMORPG;
import com.mmorpg.guild.GuildStore;
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
 * Classements affichés sur les panneaux et par /classement. Statistiques des joueurs : table MariaDB {@code leaderboard_stats}
 * (copie locale sans base), voir {@link com.mmorpg.leaderboard.Leaderboards}. Guildes : registre des guildes, relu toutes les 10 s.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Rankings {
    public static final int TOP = 10;
    public static final class Row { public String name = ""; public long level, xp, bank, bosses, members; }
    /** value : valeur principale ; tie : départage ; text : valeur affichée. */
    public record Entry(String id, String name, long value, long tie, String text) {}

    /** Classements de guildes (les classements de joueurs sont lus a la demande dans Leaderboards). */
    private static volatile Map<RankingKind, List<Entry>> cache = Map.of();

    private Rankings() {}

    public static List<Entry> top(RankingKind kind) {
        var board = kind.board();
        if (board == null) return cache.getOrDefault(kind, List.of());
        var out = new ArrayList<Entry>();
        for (var r : com.mmorpg.leaderboard.Leaderboards.top(board)) out.add(new Entry(r.id(), r.name(), r.value(), r.extra(), text(kind, r.value(), r.extra())));
        return out;
    }

    private static String text(RankingKind kind, long value, long tie) {
        return switch (kind) {
            case NIVEAU -> "Niv. " + value;
            case ARENE -> value + " V · " + tie + " frags";
            case FORTUNE -> grouped(value) + " or";
            case BOSS -> value + " boss";
            case DONJONS -> value + (value > 1 ? " victoires" : " victoire");
            case QUETES -> value + (value > 1 ? " quêtes" : " quête");
            default -> grouped(value);
        };
    }

    private static String grouped(long v) {
        return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
    }

    public static List<String> lines(RankingKind kind) {
        var out = new ArrayList<String>();
        for (var e : top(kind)) out.add(e.name() + "\t" + e.text());
        return out;
    }

    private static void sample(MinecraftServer server) {
        com.mmorpg.leaderboard.Leaderboards.sample(server);
        com.mmorpg.leaderboard.Leaderboards.refresh(server);
        var guilds = new ArrayList<Row>();
        var ids = new ArrayList<String>();
        try {
            GuildStore.access(server, false, s -> {
                for (var g : s.guilds) {
                    var r = new Row();
                    r.name = g.name; r.level = g.level; r.xp = g.xp; r.bank = g.gold;
                    r.bosses = Integer.bitCount(g.bosses); r.members = g.members.size();
                    guilds.add(r);
                    ids.add(g.id.toString());
                }
                return null;
            });
        } catch (Exception e) {
            MMORPG.LOGGER.warn("Rankings: guilds unavailable: {}", e.getMessage());
            return;
        }
        var out = new EnumMap<RankingKind, List<Entry>>(RankingKind.class);
        for (var kind : RankingKind.values()) {
            if (!kind.guild) continue;
            var list = new ArrayList<Entry>();
            for (int i = 0; i < guilds.size(); i++) {
                var r = guilds.get(i);
                String id = ids.get(i);
                Entry e = switch (kind) {
                    case GUILDE_NIVEAU -> new Entry(id, r.name, r.level, r.xp, "Niv. " + r.level);
                    case GUILDE_BANQUE -> new Entry(id, r.name, r.bank, 0, r.bank + " or");
                    default -> new Entry(id, r.name, r.bosses, r.level, r.bosses + " / 10");
                };
                if (e.value() > 0 || kind == RankingKind.GUILDE_NIVEAU) list.add(e);
            }
            list.sort(Comparator.comparingLong(Entry::value).thenComparingLong(Entry::tie).reversed().thenComparing(Entry::name));
            out.put(kind, List.copyOf(list));
        }
        cache = out;
    }

    /** Fin d'un match d'arène classé (appelé une seule fois par joueur et par match). */
    public static void arena(MinecraftServer server, UUID player, String name, int kills, boolean won) {
        com.mmorpg.leaderboard.Leaderboards.add(server, player, name, com.mmorpg.leaderboard.Board.ARENE, won ? 1 : 0, kills);
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
