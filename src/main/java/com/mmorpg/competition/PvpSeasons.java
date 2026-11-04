package com.mmorpg.competition;

import com.mmorpg.MMORPG;
import com.mmorpg.network.SharedLedger;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/** Saisons classées de l'arène : cote Elo, paliers, fin de saison automatique (28 jours), récompenses, archive. Registre partagé entre serveurs. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class PvpSeasons {
    public static final long LENGTH_MS = 28L * 24 * 3600 * 1000;
    public static final int START = 1000, MIN_GAMES = 5;
    private static final String[] TIERS = {"Bronze", "Argent", "Or", "Platine", "Diamant"};
    private static final int[] LIMITS = {0, 1100, 1300, 1500, 1700};

    public static final class Entry { public String name = ""; public int rating = START, wins, losses; }
    public static final class Archived { public int season; public List<String> top = new ArrayList<>(); }
    public static final class State {
        public int season = 1;
        public long start, end;
        public Map<String, Entry> ratings = new HashMap<>();
        public Map<String, Long> gold = new HashMap<>();
        public List<Archived> history = new ArrayList<>();
    }

    private PvpSeasons() {}

    private static <T> T access(MinecraftServer s, java.util.function.Function<State, T> f) {
        return SharedLedger.access(s, "arena-seasons", State.class, State::new, st -> {
            if (st.end == 0) { st.start = System.currentTimeMillis(); st.end = st.start + LENGTH_MS; }
            return f.apply(st);
        });
    }

    public static String tier(int rating) {
        int t = 0;
        for (int i = 0; i < LIMITS.length; i++) if (rating >= LIMITS[i]) t = i;
        return TIERS[t];
    }

    /** Enregistre un match classé terminé : un seul appel par match (garde-fou de PvpProgression.finish). */
    public static void record(MinecraftServer server, PvpProgression.Report report) {
        if (report.training || report.unfinished || report.winner < 0) return;
        access(server, st -> {
            double[] avg = new double[2];
            int[] n = new int[2];
            for (var row : report.rows) {
                var e = st.ratings.computeIfAbsent(row.id, k -> new Entry());
                avg[row.team & 1] += e.rating;
                n[row.team & 1]++;
            }
            for (int i = 0; i < 2; i++) avg[i] = n[i] == 0 ? START : avg[i] / n[i];
            for (var row : report.rows) {
                var e = st.ratings.get(row.id);
                e.name = row.name;
                boolean won = row.team == report.winner;
                double expected = 1.0 / (1 + Math.pow(10, (avg[1 - (row.team & 1)] - e.rating) / 400.0));
                int delta = (int) Math.round(32 * ((won ? 1 : 0) - expected));
                e.rating = Math.max(0, e.rating + delta);
                if (won) e.wins++; else e.losses++;
            }
            return null;
        });
    }

    /** Clôture la saison si sa date est passée : classement archivé, récompenses créditées, cotes ramenées vers 1000. */
    public static boolean endIfDue(MinecraftServer server, boolean force) {
        return access(server, st -> {
            if (!force && System.currentTimeMillis() < st.end) return false;
            var ranked = new ArrayList<Map.Entry<String, Entry>>();
            for (var e : st.ratings.entrySet()) if (e.getValue().wins + e.getValue().losses >= MIN_GAMES) ranked.add(e);
            ranked.sort((a, b) -> Integer.compare(b.getValue().rating, a.getValue().rating));
            var archive = new Archived();
            archive.season = st.season;
            int rank = 0;
            for (var e : ranked) {
                rank++;
                if (rank <= 10) archive.top.add(rank + ". " + e.getValue().name + " " + e.getValue().rating + " (" + tier(e.getValue().rating) + ")");
                long gold = rank == 1 ? 5000 : rank <= 3 ? 2500 : rank <= 10 ? 1000 : 300;
                long points = rank == 1 ? 1500 : rank <= 3 ? 800 : rank <= 10 ? 400 : 100;
                String title = rank == 1 ? "Champion de la saison " + st.season : rank <= 3 ? "Élite de la saison " + st.season : "";
                st.gold.merge(e.getKey(), gold, Long::sum);
                final String key = e.getKey();
                PvpProgression.access(server, ps -> {
                    var a = PvpProgression.account(ps, UUID.fromString(key));
                    a.points += points;
                    if (!title.isEmpty()) a.unlocks.add("title:" + title);
                    return null;
                });
            }
            st.history.add(archive);
            if (st.history.size() > 24) st.history.remove(0);
            for (var e : st.ratings.values()) { e.rating = START + (e.rating - START) / 2; e.wins = 0; e.losses = 0; }
            st.season++;
            st.start = System.currentTimeMillis();
            st.end = st.start + LENGTH_MS;
            return true;
        });
    }

    /** Verse l'or de fin de saison en attente. */
    public static void claim(ServerPlayer p) {
        long gold = access(p.level().getServer(), st -> { Long g = st.gold.remove(p.getUUID().toString()); return g == null ? 0L : g; });
        if (gold > 0) {
            RpgPlayers.addGold(p, gold, true);
            p.sendSystemMessage(Component.literal("Récompense de saison : " + gold + " or."));
        }
    }

    private static void show(ServerPlayer p) {
        var st = access(p.level().getServer(), s -> s);
        var mine = st.ratings.get(p.getUUID().toString());
        int rating = mine == null ? START : mine.rating;
        long days = Math.max(0, (st.end - System.currentTimeMillis()) / 86_400_000L);
        p.sendSystemMessage(Component.literal("Saison " + st.season + " · fin dans " + days + " j · ta cote : " + rating + " (" + tier(rating) + ")"
                + (mine == null ? "" : " · " + mine.wins + "V " + mine.losses + "D")));
        var top = new ArrayList<>(st.ratings.entrySet());
        top.sort((a, b) -> Integer.compare(b.getValue().rating, a.getValue().rating));
        int i = 0;
        for (var e : top) {
            if (++i > 10 || e.getValue().wins + e.getValue().losses == 0) break;
            p.sendSystemMessage(Component.literal(i + ". " + e.getValue().name + " · " + e.getValue().rating + " (" + tier(e.getValue().rating) + ")"));
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 1200 != 0) return;
        try {
            if (endIfDue(e.getServer(), false)) {
                for (var p : e.getServer().getPlayerList().getPlayers()) { p.sendSystemMessage(Component.literal("Nouvelle saison d'arène ! Récompenses : /saison")); claim(p); }
            }
        } catch (RuntimeException ex) {
            MMORPG.LOGGER.warn("Saisons : {}", ex.getMessage());
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) com.mmorpg.server.Scheduler.later(p.level(), 60, () -> { try { claim(p); } catch (RuntimeException ex) { MMORPG.LOGGER.warn("Saisons : {}", ex.getMessage()); } });
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("saison")
                .executes(c -> { var p = c.getSource().getPlayerOrException(); claim(p); show(p); return 1; })
                .then(Commands.literal("terminer").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(c -> { endIfDue(c.getSource().getServer(), true); c.getSource().sendSuccess(() -> Component.literal("Saison terminée, nouvelle saison lancée."), true); return 1; })));
    }
}
