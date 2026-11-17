package com.mmorpg.mine;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Minerais epuises enregistres avec le monde : position, dimension et moment de leur repousse. */
public class MineData extends SavedData {
    /** Un minerai epuise : position, palier du minerai, temps de jeu (tick du monde) de sa repousse et dimension ("" = ancienne mine, ignore). */
    public record Respawn(int x, int y, int z, int tier, long due, String dim) {
        static final Codec<Respawn> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Respawn::x),
                Codec.INT.fieldOf("y").forGetter(Respawn::y),
                Codec.INT.fieldOf("z").forGetter(Respawn::z),
                Codec.INT.fieldOf("tier").forGetter(Respawn::tier),
                Codec.LONG.fieldOf("due").forGetter(Respawn::due),
                Codec.STRING.optionalFieldOf("dim", "").forGetter(Respawn::dim)
        ).apply(i, Respawn::new));

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private static final Codec<MineData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Respawn.CODEC.listOf().optionalFieldOf("respawns", List.of()).forGetter(d -> d.respawns)
    ).apply(i, MineData::new));
    public static final SavedDataType<MineData> TYPE = new SavedDataType<>(MMORPG.id("mines"), MineData::new, CODEC);

    private final List<Respawn> respawns;

    public MineData() {
        this(List.of());
    }

    private MineData(List<Respawn> respawns) {
        this.respawns = new ArrayList<>(respawns);
        this.respawns.removeIf(r -> r.dim().isEmpty());                    // restes de l'ancienne dimension des mines
    }

    public static MineData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Respawn> respawns() {
        return respawns;
    }

    public void add(Respawn r) {
        respawns.removeIf(o -> o.x() == r.x() && o.y() == r.y() && o.z() == r.z() && o.dim().equals(r.dim()));
        respawns.add(r);
        setDirty();
    }

    public void remove(Respawn r) {
        if (respawns.remove(r)) setDirty();
    }

    public void clear() {
        respawns.clear();
        setDirty();
    }
}
