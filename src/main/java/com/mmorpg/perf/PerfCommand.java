package com.mmorpg.perf;

import com.mmorpg.MMORPG;
import com.mmorpg.db.Database;
import com.mmorpg.network.NetworkPlayers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** /perf : etat de sante du serveur (tick, memoire, entites, base, sauvegardes, paquets) ; /perf reset ; /perf nettoyer. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class PerfCommand {
    private PerfCommand() {
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("perf").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> show(c.getSource()))
                .then(Commands.literal("reset").executes(c -> {
                    Perf.reset();
                    c.getSource().sendSuccess(() -> Component.literal("§7Compteurs remis à zéro."), false);
                    return 1;
                }))
                .then(Commands.literal("nettoyer").executes(c -> {
                    int n = OrphanSweeper.sweep(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("§7" + n + " entité(s) orpheline(s) retirée(s)."), false);
                    return n;
                })));
    }

    private static int show(CommandSourceStack src) {
        var server = src.getServer();
        double mspt = server.getCurrentSmoothedTickTime();
        double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 50.0));
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) >> 20, max = rt.maxMemory() >> 20;
        StringBuilder sb = new StringBuilder("§6=== /perf ===\n");
        sb.append(String.format(Locale.ROOT, "§7Tick : §f%.1f TPS §7· §f%.1f ms §7· mémoire §f%d/%d Mo §7· joueurs §f%d\n",
                tps, mspt, used, max, server.getPlayerList().getPlayerCount()));
        int total = 0;
        Map<String, Integer> byType = new HashMap<>();
        for (ServerLevel l : server.getAllLevels()) {
            int n = 0;
            for (Entity e : l.getAllEntities()) {
                n++;
                byType.merge(e.getType().builtInRegistryHolder().key().identifier().toString(), 1, Integer::sum);
            }
            total += n;
            if (n > 0) sb.append("§7  ").append(l.dimension().identifier()).append(" : §f").append(n).append(" entités\n");
        }
        sb.append("§7Entités chargées : §f").append(total).append(" §7· top : §f");
        byType.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(5)
                .forEach(e -> sb.append(e.getKey().replace("mmorpg:", "")).append(' ').append(e.getValue()).append("  "));
        sb.append('\n');
        sb.append("§7Base : ").append(Database.enabled() ? (Database.ready() ? "§aprête" : "§cindisponible") : "§7désactivée")
                .append(" §7· file §f").append(Database.pending()).append(" §7· perdues §f").append(Database.dropped()).append('\n');
        sb.append("§7Sauvegardes asynchrones : §f").append(Perf.ASYNC_SAVES.get()).append(" §7(remplacées ")
                .append(Perf.ASYNC_SAVES_SKIPPED.get()).append(", en attente ").append(NetworkPlayers.pendingSaves()).append(")\n");
        long q = Perf.PACKETS_QUEUED.get(), co = Perf.PACKETS_COALESCED.get(), bu = Perf.BUNDLES_SENT.get();
        sb.append("§7Paquets regroupés : §f").append(q).append(" §7· fusionnés §f").append(co).append(" §7· groupes envoyés §f").append(bu).append('\n');
        sb.append("§7Orphelins retirés : §f").append(Perf.ORPHANS_REMOVED.get()).append('\n');
        if (!Perf.sections().isEmpty()) {
            sb.append("§7Sections (moy. / max, ms) :\n");
            for (var e : new TreeMap<>(Perf.sections()).entrySet()) {
                sb.append(String.format(Locale.ROOT, "§7  %s : §f%.3f §7/ §f%.3f §7(%d)\n", e.getKey(), e.getValue().avgMs, e.getValue().maxMs, e.getValue().calls.get()));
            }
        }
        String out = sb.toString().stripTrailing();
        src.sendSuccess(() -> Component.literal(out), false);
        return 1;
    }
}
