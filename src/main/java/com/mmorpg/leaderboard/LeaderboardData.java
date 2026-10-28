package com.mmorpg.leaderboard;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Copie locale (enregistree avec le monde) des statistiques de classement : sert quand la base de donnees est desactivee ou indisponible. */
public class LeaderboardData extends SavedData {
    public record Entry(String board, UUID id, String name, long value, long extra) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("board").forGetter(Entry::board),
                UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(Entry::id),
                Codec.STRING.fieldOf("name").forGetter(Entry::name),
                Codec.LONG.fieldOf("value").forGetter(Entry::value),
                Codec.LONG.optionalFieldOf("extra", 0L).forGetter(Entry::extra)
        ).apply(i, Entry::new));
    }

    private static final Codec<LeaderboardData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(d -> d.all())
    ).apply(i, LeaderboardData::new));
    public static final SavedDataType<LeaderboardData> TYPE = new SavedDataType<>(MMORPG.id("classements"), LeaderboardData::new, CODEC);

    private final Map<String, Map<UUID, Entry>> boards = new HashMap<>();

    public LeaderboardData() {
    }

    private LeaderboardData(List<Entry> entries) {
        for (Entry e : entries) boards.computeIfAbsent(e.board(), k -> new HashMap<>()).put(e.id(), e);
    }

    public static LeaderboardData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    private List<Entry> all() {
        List<Entry> out = new ArrayList<>();
        boards.values().forEach(m -> out.addAll(m.values()));
        return out;
    }

    public void set(Board board, UUID id, String name, long value, long extra) {
        boards.computeIfAbsent(board.id, k -> new HashMap<>()).put(id, new Entry(board.id, id, name, value, extra));
        setDirty();
    }

    public void add(Board board, UUID id, String name, long delta) {
        Entry old = boards.computeIfAbsent(board.id, k -> new HashMap<>()).get(id);
        set(board, id, name, (old == null ? 0 : old.value()) + delta, old == null ? 0 : old.extra());
    }

    /** Les {@code limit} meilleures entrees (valeur puis bonus decroissants). */
    public List<Entry> top(Board board, int limit) {
        Map<UUID, Entry> m = boards.get(board.id);
        if (m == null) return List.of();
        return m.values().stream().filter(e -> e.value() > 0)
                .sorted((a, b) -> a.value() != b.value() ? Long.compare(b.value(), a.value())
                        : a.extra() != b.extra() ? Long.compare(b.extra(), a.extra()) : a.name().compareToIgnoreCase(b.name()))
                .limit(limit).toList();
    }
}
