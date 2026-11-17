package com.mmorpg.plains;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Cultures recoltees enregistrees avec le monde : elles redeviennent mures a l'heure indiquee (comme les veines de minerai). */
public class PlainsData extends SavedData {
    /** Une culture recoltee : position, temps de jeu (tick du monde) auquel elle redevient mure, et dimension ("" = ancienne plaine, ignoree). */
    public record Respawn(int x, int y, int z, long due, String dim) {
        static final Codec<Respawn> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Respawn::x),
                Codec.INT.fieldOf("y").forGetter(Respawn::y),
                Codec.INT.fieldOf("z").forGetter(Respawn::z),
                Codec.LONG.fieldOf("due").forGetter(Respawn::due),
                Codec.STRING.optionalFieldOf("dim", "").forGetter(Respawn::dim)
        ).apply(i, Respawn::new));

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private static final Codec<PlainsData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Respawn.CODEC.listOf().optionalFieldOf("respawns", List.of()).forGetter(d -> d.respawns)
    ).apply(i, PlainsData::new));
    public static final SavedDataType<PlainsData> TYPE = new SavedDataType<>(MMORPG.id("plaines"), PlainsData::new, CODEC);

    private final List<Respawn> respawns;

    public PlainsData() {
        this(List.of());
    }

    private PlainsData(List<Respawn> respawns) {
        this.respawns = new ArrayList<>(respawns);
        this.respawns.removeIf(r -> r.dim().isEmpty());
    }

    public static PlainsData get(MinecraftServer server) {
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
