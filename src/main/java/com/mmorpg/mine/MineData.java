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
            Codec.INT.optionalFieldOf("version", 0).forGetter(d -> d.version),
            Respawn.CODEC.listOf().optionalFieldOf("respawns", List.of()).forGetter(d -> d.respawns)
    ).apply(i, MineData::new));
    public static final SavedDataType<MineData> TYPE = new SavedDataType<>(MMORPG.id("mines"), MineData::new, CODEC);

    /** Version du plan avec laquelle la mine a ete construite (0 = pas construite) ; si le plan change, la mine est reconstruite. */
    private int version;
    private final List<Respawn> respawns;

    public MineData() {
        this.version = 0;
        this.respawns = new ArrayList<>();
    }

    private MineData(int version, List<Respawn> respawns) {
        this.version = version;
        this.respawns = new ArrayList<>(respawns);
    }

    public static MineData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int version() {
        return version;
    }

    public void setVersion(int v) {
        version = v;
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
