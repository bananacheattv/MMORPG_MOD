package com.mmorpg.moderation;

import com.mmorpg.MMORPG;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Journal local d'incident (fichiers moderation/journal/AAAA-MM-JJ.log), independant de la base : evenements de jeu (via {@code Audit}), messages,
 * commandes, sanctions et positions. Sert au rejeu d'un incident (/rejeu) et a l'onglet « Journal » du panneau. Conserve 14 jours.
 * Format d'une ligne : {@code epoch_ms|uuid|pseudo|type|montant|detail} (barres et retours a la ligne retires du detail).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Journal {
    private Journal() {
    }

    public record Line(long time, String id, String name, String kind, long amount, String detail) {
    }

    private static final Object LOCK = new Object();
    private static final Map<UUID, double[]> LAST_POS = new HashMap<>();

    private static Path dir() {
        return ModUtil.dir().resolve("journal");
    }

    public static void add(UUID id, String name, String kind, long amount, String detail) {
        add(System.currentTimeMillis(), id, name, kind, amount, detail);
    }

    public static void add(long time, UUID id, String name, String kind, long amount, String detail) {
        String d = detail == null ? "" : detail.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
        if (d.length() > 400) d = d.substring(0, 400);
        String line = time + "|" + (id == null ? "" : id) + "|" + (name == null ? "" : name.replace('|', '/')) + "|" + kind + "|" + amount + "|" + d + System.lineSeparator();
        synchronized (LOCK) {
            try {
                Files.createDirectories(dir());
                Files.writeString(dir().resolve(LocalDate.now() + ".log"), line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                MMORPG.LOGGER.warn("[MMORPG] journal local non ecrit : {}", e.getMessage());
            }
        }
    }

    /** Lignes d'un joueur (par UUID) depuis {@code sinceMs}, de la plus ancienne a la plus recente, limitees aux {@code max} dernieres. */
    public static List<Line> replay(String id, long sinceMs, int max) {
        List<Line> out = new ArrayList<>();
        synchronized (LOCK) {
            LocalDate d = LocalDate.now();
            LocalDate first = java.time.Instant.ofEpochMilli(sinceMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            List<LocalDate> days = new ArrayList<>();
            for (LocalDate x = first; !x.isAfter(d); x = x.plusDays(1)) days.add(x);
            for (LocalDate day : days) {
                Path f = dir().resolve(day + ".log");
                if (!Files.exists(f)) continue;
                try {
                    for (String s : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                        String[] p = s.split("\\|", 6);
                        if (p.length < 6 || !(id == null || id.isEmpty() || id.equals(p[1]))) continue;
                        try {
                            long t = Long.parseLong(p[0]);
                            if (t >= sinceMs) out.add(new Line(t, p[1], p[2], p[3], Long.parseLong(p[4]), p[5]));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                } catch (IOException e) {
                    MMORPG.LOGGER.warn("[MMORPG] journal {} illisible : {}", f.getFileName(), e.getMessage());
                }
            }
        }
        return out.size() > max ? new ArrayList<>(out.subList(out.size() - max, out.size())) : out;
    }

    private static void prune() {
        synchronized (LOCK) {
            try (var files = Files.list(dir())) {
                long limit = System.currentTimeMillis() - 14L * 86_400_000L;
                for (Path f : (Iterable<Path>) files::iterator) {
                    if (Files.getLastModifiedTime(f).toMillis() < limit) Files.deleteIfExists(f);
                }
            } catch (IOException ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ collecte

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer p = event.getPlayer();
        add(p.getUUID(), p.getGameProfile().name(), "chat", 0, event.getRawText());
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer p) {
            add(p.getUUID(), p.getGameProfile().name(), "commande", 0, "/" + event.getParseResults().getReader().getString());
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        int tick = event.getServer().getTickCount();
        if (tick % 400 == 0) {
            for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
                double[] last = LAST_POS.get(p.getUUID());
                if (last != null && last[0] == p.level().dimension().identifier().hashCode() && Math.abs(last[1] - p.getX()) + Math.abs(last[3] - p.getZ()) < 8) continue;
                LAST_POS.put(p.getUUID(), new double[]{p.level().dimension().identifier().hashCode(), p.getX(), p.getY(), p.getZ()});
                add(p.getUUID(), p.getGameProfile().name(), "position", 0, p.level().dimension().identifier() + " " + (int) p.getX() + " " + (int) p.getY() + " " + (int) p.getZ());
            }
        }
        if (tick % 72000 == 1) prune();
    }

    @SubscribeEvent
    public static void onStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        LAST_POS.clear();
        ModHistory.reset();
    }
}
