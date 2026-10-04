package com.mmorpg.world;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reseau de teleporteurs du monde (sauvegarde dans les donnees du monde). */
public class WaypointData extends SavedData {
    public static final int CITY = 0;
    public static final int DUNGEON = 1;
    public static final int WAYPOINT = 2;
    public static final String[] CATEGORY_NAMES = {"Ville", "Donjon", "Point de passage"};

    public static final class Waypoint {
        public final UUID id;
        public String name;
        public int category;
        public final ResourceKey<Level> dimension;
        public final BlockPos pos;
        public int minLevel;
        public UUID owner;

        public Waypoint(UUID id, String name, int category, ResourceKey<Level> dimension, BlockPos pos, int minLevel, UUID owner) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.dimension = dimension;
            this.pos = pos;
            this.minLevel = minLevel;
            this.owner = owner;
        }

        static final Codec<Waypoint> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(w -> w.id),
                Codec.STRING.fieldOf("name").forGetter(w -> w.name),
                Codec.INT.fieldOf("category").forGetter(w -> w.category),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(w -> w.dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(w -> w.pos),
                Codec.INT.fieldOf("minLevel").forGetter(w -> w.minLevel),
                UUIDUtil.CODEC.optionalFieldOf("owner", new UUID(0, 0)).forGetter(w -> w.owner)
        ).apply(i, Waypoint::new));
    }

    private static final Codec<WaypointData> CODEC = Waypoint.CODEC.listOf().xmap(WaypointData::new, d -> new ArrayList<>(d.waypoints.values()));
    public static final SavedDataType<WaypointData> TYPE = new SavedDataType<>(MMORPG.id("waypoints"), WaypointData::new, CODEC);

    private final Map<UUID, Waypoint> waypoints = new LinkedHashMap<>();

    public WaypointData() {
    }

    private WaypointData(List<Waypoint> list) {
        for (Waypoint w : list) waypoints.put(w.id, w);
    }

    public static WaypointData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Collection<Waypoint> all() {
        return waypoints.values();
    }

    public Waypoint get(UUID id) {
        return waypoints.get(id);
    }

    public Waypoint at(ResourceKey<Level> dim, BlockPos pos) {
        for (Waypoint w : waypoints.values()) {
            if (w.dimension.equals(dim) && w.pos.equals(pos)) return w;
        }
        return null;
    }

    public Waypoint create(ResourceKey<Level> dim, BlockPos pos, String name, int category, int minLevel, UUID owner) {
        Waypoint existing = at(dim, pos);
        if (existing != null) return existing;
        Waypoint w = new Waypoint(UUID.randomUUID(), name, category, dim, pos.immutable(), minLevel, owner);
        waypoints.put(w.id, w);
        setDirty();
        return w;
    }

    public void removeAt(ResourceKey<Level> dim, BlockPos pos) {
        Waypoint w = at(dim, pos);
        if (w != null) {
            waypoints.remove(w.id);
            setDirty();
        }
    }

    public void remove(UUID id) {
        if (waypoints.remove(id) != null) setDirty();
    }

    public static String dimensionName(ResourceKey<Level> dim) {
        Identifier id = dim.identifier();
        return switch (id.toString()) {
            case "minecraft:overworld" -> "Surface";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> id.getPath();
        };
    }
}
