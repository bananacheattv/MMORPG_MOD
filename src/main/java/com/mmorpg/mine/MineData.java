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

/** Etat des Mines d'Aeloria enregistre avec le monde : la mine est-elle construite, et quels minerais epuises repoussent quand. */
public class MineData extends SavedData {
    /** Un minerai epuise : position, palier du minerai et temps de jeu (tick du monde) de sa repousse. */
    public record Respawn(int x, int y, int z, int tier, long due) {
        static final Codec<Respawn> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Respawn::x),
                Codec.INT.fieldOf("y").forGetter(Respawn::y),
                Codec.INT.fieldOf("z").forGetter(Respawn::z),
                Codec.INT.fieldOf("tier").forGetter(Respawn::tier),
                Codec.LONG.fieldOf("due").forGetter(Respawn::due)
        ).apply(i, Respawn::new));

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private static final Codec<MineData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.listOf().optionalFieldOf("versions", List.of()).forGetter(d -> d.versions),
            Respawn.CODEC.listOf().optionalFieldOf("respawns", List.of()).forGetter(d -> d.respawns)
    ).apply(i, MineData::new));
    public static final SavedDataType<MineData> TYPE = new SavedDataType<>(MMORPG.id("mines"), MineData::new, CODEC);

    /** Version du plan avec laquelle chaque mine a ete construite (0 = pas construite) ; si le plan change, la mine est reconstruite. */
    private final List<Integer> versions;
    private final List<Respawn> respawns;

    public MineData() {
        this(List.of(), List.of());
    }

    private MineData(List<Integer> versions, List<Respawn> respawns) {
        this.versions = new ArrayList<>(versions);
        while (this.versions.size() < MineLayout.SECTORS) this.versions.add(0);
        this.respawns = new ArrayList<>(respawns);
    }

    public static MineData get(MinecraftServer server) {
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
