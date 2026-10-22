package com.mmorpg.dungeon;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Instances de donjon : chaque donjon est construit en plusieurs exemplaires dans la dimension des donjons, un par joueur seul ou par
 * groupe. {@link Slot} = un exemplaire (sa position, son proprietaire) ; {@code gates} relie un point de passage du reseau de
 * teleportation (« Crypte Gobeline ») au type de donjon et a la difficulte qu'il ouvre.
 */
public class InstanceData extends SavedData {
    public static final class Slot {
        public final String pool;
        public final int index;
        public final int x, y, z;
        /** Proprietaire : identifiant du chef de groupe (ou du joueur seul) ; vide = libre. */
        public String owner;
        public long lastBusy;
        /** Faux tant que le donjon n'est pas entierement construit (construction en cours ou interrompue). */
        public boolean ready = true;

        public Slot(String pool, int index, int x, int y, int z, String owner, long lastBusy, boolean ready) {
            this(pool, index, x, y, z, owner, lastBusy);
            this.ready = ready;
        }

        public Slot(String pool, int index, int x, int y, int z, String owner, long lastBusy) {
            this.pool = pool;
            this.index = index;
            this.x = x;
            this.y = y;
            this.z = z;
            this.owner = owner;
            this.lastBusy = lastBusy;
        }

        static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("pool").forGetter(s -> s.pool),
                Codec.INT.fieldOf("index").forGetter(s -> s.index),
                Codec.INT.fieldOf("x").forGetter(s -> s.x),
                Codec.INT.fieldOf("y").forGetter(s -> s.y),
                Codec.INT.fieldOf("z").forGetter(s -> s.z),
                Codec.STRING.optionalFieldOf("owner", "").forGetter(s -> s.owner),
                Codec.LONG.optionalFieldOf("lastBusy", 0L).forGetter(s -> s.lastBusy),
                Codec.BOOL.optionalFieldOf("ready", true).forGetter(s -> s.ready)
        ).apply(i, (pool, index, x, y, z, owner, lastBusy, ready) -> new Slot(pool, index, x, y, z, owner, lastBusy, ready)));
    }

    private static final Codec<InstanceData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Slot.CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(d -> d.slots),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("gates", Map.of()).forGetter(d -> d.gateStrings())
    ).apply(i, InstanceData::new));
    public static final SavedDataType<InstanceData> TYPE = new SavedDataType<>(MMORPG.id("instances"), InstanceData::new, CODEC);

    final List<Slot> slots;
    private final Map<UUID, String> gates = new HashMap<>();

    public InstanceData() {
        this.slots = new ArrayList<>();
    }

    private InstanceData(List<Slot> slots, Map<String, String> gates) {
        this.slots = new ArrayList<>(slots);
        gates.forEach((k, v) -> this.gates.put(UUID.fromString(k), v));
    }

    private Map<String, String> gateStrings() {
        Map<String, String> out = new HashMap<>();
        gates.forEach((k, v) -> out.put(k.toString(), v));
        return out;
    }

    public static InstanceData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Slot> slotsOf(String pool) {
        List<Slot> out = new ArrayList<>();
        for (Slot s : slots) {
            if (s.pool.equals(pool)) out.add(s);
        }
        return out;
    }

    public @Nullable String gate(UUID waypoint) {
        return gates.get(waypoint);
    }

    public void putGate(UUID waypoint, String pool) {
        gates.put(waypoint, pool);
        setDirty();
    }

    public void dirty() {
        setDirty();
    }
}
