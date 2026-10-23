package com.mmorpg.plains;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Etat des Plaines d'Aeloria enregistre avec le monde : version du plan avec lequel elles ont ete construites (0 = pas construites). */
public class PlainsData extends SavedData {
    private static final Codec<PlainsData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("version", 0).forGetter(d -> d.version)
    ).apply(i, PlainsData::new));
    public static final SavedDataType<PlainsData> TYPE = new SavedDataType<>(MMORPG.id("plaines"), PlainsData::new, CODEC);

    private int version;

    public PlainsData() {
        this(0);
    }

    private PlainsData(int version) {
        this.version = version;
    }

    public static PlainsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int version() {
        return version;
    }

    public void setVersion(int v) {
        version = v;
        setDirty();
    }
}
