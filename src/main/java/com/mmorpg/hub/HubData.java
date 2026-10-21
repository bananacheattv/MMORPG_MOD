package com.mmorpg.hub;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/** Emplacement du hub d'entree dans le monde (construit par /mmorpg hub) ; tout le reste se deduit de {@link HubLayout}. */
public class HubData extends SavedData {
    private static final Codec<HubData> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.optionalFieldOf("origin").forGetter(d -> java.util.Optional.ofNullable(d.origin)),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension", Level.OVERWORLD).forGetter(d -> d.dimension)
    ).apply(i, (o, dim) -> new HubData(o.orElse(null), dim)));
    public static final SavedDataType<HubData> TYPE = new SavedDataType<>(MMORPG.id("hub"), HubData::new, CODEC);

    private @Nullable BlockPos origin;
    private ResourceKey<Level> dimension = Level.OVERWORLD;

    public HubData() {
    }

    private HubData(@Nullable BlockPos origin, ResourceKey<Level> dimension) {
        this.origin = origin;
        this.dimension = dimension;
    }

    public static HubData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean exists() {
        return origin != null;
    }

    public @Nullable BlockPos origin() {
        return origin;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public @Nullable ServerLevel level(MinecraftServer server) {
        return origin == null ? null : server.getLevel(dimension);
    }

    public void set(ResourceKey<Level> dim, BlockPos origin) {
        this.dimension = dim;
        this.origin = origin.immutable();
        setDirty();
    }

    public void clear() {
        origin = null;
        setDirty();
    }
}
