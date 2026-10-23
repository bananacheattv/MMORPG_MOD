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

/**
 * Etat des Plaines d'Aeloria enregistre avec le monde : version du plan de chaque plaine (0 = pas construite) et
 * cultures recoltees qui repoussent (comme les veines de la mine).
 */
public class PlainsData extends SavedData {
    /** Une culture recoltee : position et temps de jeu (tick du monde) auquel elle redevient mure. */
    public record Respawn(int x, int y, int z, long due) {
        static final Codec<Respawn> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Respawn::x),
                Codec.INT.fieldOf("y").forGetter(Respawn::y),
                Codec.INT.fieldOf("z").forGetter(Respawn::z),
                Codec.LONG.fieldOf("due").forGetter(Respawn::due)
        ).apply(i, Respawn::new));

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private static final Codec<PlainsData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.listOf().optionalFieldOf("versions", List.of()).forGetter(d -> d.versions),
            Respawn.CODEC.listOf().optionalFieldOf("respawns", List.of()).forGetter(d -> d.respawns)
    ).apply(i, PlainsData::new));
    public static final SavedDataType<PlainsData> TYPE = new SavedDataType<>(MMORPG.id("plaines"), PlainsData::new, CODEC);

    /** Version du plan avec laquelle chaque plaine a ete construite (0 = pas construite). */
    private final List<Integer> versions;
    private final List<Respawn> respawns;

    public PlainsData() {
        this(List.of(), List.of());
    }

    private PlainsData(List<Integer> versions, List<Respawn> respawns) {
        this.versions = new ArrayList<>(versions);
        while (this.versions.size() < PlainsLayout.REGIONS) this.versions.add(0);
        this.respawns = new ArrayList<>(respawns);
    }

    public static PlainsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int version(int region) {
        return versions.get(region);
    }

    public void setVersion(int region, int v) {
        versions.set(region, v);
        setDirty();
    }

    public List<Respawn> respawns() {
        return respawns;
    }

    public void add(Respawn r) {
        respawns.removeIf(o -> o.x() == r.x() && o.y() == r.y() && o.z() == r.z());
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
