package com.mmorpg.hub;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/** Emplacement et version du Spawn d'Aeloria (le marche medieval, dans sa propre dimension mmorpg:spawn). */
public class SpawnData extends SavedData {
    public static final Codec<SpawnData> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.optionalFieldOf("origin").forGetter(d -> java.util.Optional.ofNullable(d.origin)),
            Codec.INT.optionalFieldOf("layout", 0).forGetter(d -> d.layout),
            net.minecraft.world.phys.Vec3.CODEC.optionalFieldOf("arrival").forGetter(d -> java.util.Optional.ofNullable(d.arrival)),
            Codec.FLOAT.optionalFieldOf("yaw", 90F).forGetter(d -> d.yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0F).forGetter(d -> d.pitch)
    ).apply(i, (o, lay, at, yaw, pitch) -> {
        var data=new SpawnData(o.orElse(null),lay);data.arrival=at.orElse(null);data.yaw=yaw;data.pitch=pitch;return data;
    }));
    public static final SavedDataType<SpawnData> TYPE = new SavedDataType<>(MMORPG.id("spawn"), SpawnData::new, CODEC);

    private @Nullable BlockPos origin;
    private int layout;
    private @Nullable Vec3 arrival;
    private float yaw=90F, pitch=0F;
    public net.minecraft.world.phys.Vec3 arrival() {return arrival==null?net.minecraft.world.phys.Vec3.atBottomCenterOf(origin):arrival;}
    public float yaw() {return yaw;}
    public float pitch() {return pitch;}
    public void setArrival(net.minecraft.world.phys.Vec3 position,float yaw,float pitch) {
        this.arrival=position;this.yaw=yaw;this.pitch=pitch;setDirty();
    }

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
