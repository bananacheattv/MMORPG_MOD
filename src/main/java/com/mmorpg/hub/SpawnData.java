package com.mmorpg.hub;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/** Emplacement et version du Spawn d'Aeloria (le marche medieval, dans sa propre dimension mmorpg:spawn). */
public class SpawnData extends SavedData {
    private static final Codec<SpawnData> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.optionalFieldOf("origin").forGetter(d -> java.util.Optional.ofNullable(d.origin)),
            Codec.INT.optionalFieldOf("layout", 0).forGetter(d -> d.layout)
    ).apply(i, (o, lay) -> new SpawnData(o.orElse(null), lay)));
    public static final SavedDataType<SpawnData> TYPE = new SavedDataType<>(MMORPG.id("spawn"), SpawnData::new, CODEC);

    private @Nullable BlockPos origin;
    private int layout;

    public SpawnData() {
    }

    private SpawnData(@Nullable BlockPos origin, int layout) {
        this.origin = origin;
        this.layout = layout;
    }

    public static SpawnData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean exists() {
        return origin != null;
    }

    public @Nullable BlockPos origin() {
        return origin;
    }

    public int layout() {
        return layout;
    }

    public void set(BlockPos origin, int layout) {
        this.origin = origin.immutable();
        this.layout = layout;
        setDirty();
    }
}
