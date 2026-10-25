package com.mmorpg.world;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
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

/** Placed adventure arches, independent of procedural generation and personal waypoints. */
public final class AdventurePortalData extends SavedData {
    public record Portal(UUID id, ResourceKey<Level> dimension, BlockPos pos, boolean widthX,
                         String kind, int region, String dungeon, int difficulty) {
        public static final Codec<Portal> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Portal::id),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Portal::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Portal::pos),
                Codec.BOOL.fieldOf("widthX").forGetter(Portal::widthX),
                Codec.STRING.fieldOf("kind").forGetter(Portal::kind),
                Codec.INT.fieldOf("region").forGetter(Portal::region),
                Codec.STRING.fieldOf("dungeon").forGetter(Portal::dungeon),
                Codec.INT.fieldOf("difficulty").forGetter(Portal::difficulty)
        ).apply(i, Portal::new));
        public BlockPos at(int width, int height) { return pos.offset(widthX ? width : 0, height, widthX ? 0 : width); }
    }
    public static final Codec<AdventurePortalData> CODEC = Portal.CODEC.listOf().xmap(AdventurePortalData::new, d -> new ArrayList<>(d.portals.values()));
    public static final SavedDataType<AdventurePortalData> TYPE = new SavedDataType<>(MMORPG.id("adventure_portals"), AdventurePortalData::new, CODEC);
    private final Map<UUID, Portal> portals = new LinkedHashMap<>();
    public AdventurePortalData() { }
    private AdventurePortalData(List<Portal> list) { for (Portal portal : list) portals.put(portal.id(), portal); }
    public static AdventurePortalData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
    public Collection<Portal> all() { return List.copyOf(portals.values()); }
    public Portal find(UUID id) { return portals.get(id); }
    public void put(Portal portal) { portals.put(portal.id(), portal); setDirty(); }
    public void remove(UUID id) { if (portals.remove(id) != null) setDirty(); }
}
