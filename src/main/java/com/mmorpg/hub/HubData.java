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
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension", Level.OVERWORLD).forGetter(d -> d.dimension),
            Codec.BOOL.optionalFieldOf("stable", false).forGetter(d -> d.stable),
            Codec.INT.optionalFieldOf("layout", 1).forGetter(d -> d.layout),
            Codec.INT.optionalFieldOf("count", 1).forGetter(d -> d.count)
    ).apply(i, (o, dim, st, lay, n) -> new HubData(o.orElse(null), dim, st, lay, n)));
    public static final SavedDataType<HubData> TYPE = new SavedDataType<>(MMORPG.id("hub"), HubData::new, CODEC);

    private @Nullable BlockPos origin;
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    /** Vrai quand le hub a ete construit avec des identifiants d'entites stables (les anciens exemplaires peuvent alors etre retires). */
    private boolean stable;
    /** Version du plan avec laquelle le hub a ete construit (voir HubBuilder.LAYOUT_VERSION). */
    private int layout = 1;
    /** Nombre de copies (« salons ») du hub : Hub 1, Hub 2... ; la copie i est a i x SPACING blocs a l'est de la premiere. */
    private int count = 1;
    public static final int SPACING = 320;
    /** Joueurs au plus par copie du hub. */
    public static final int CAPACITY = 20;

    public HubData() {
    }

    private HubData(@Nullable BlockPos origin, ResourceKey<Level> dimension, boolean stable, int layout, int count) {
        this.layout = layout;
        this.count = Math.max(1, count);
        this.origin = origin;
        this.dimension = dimension;
        this.stable = stable;
    }

    public int layout() {
        return layout;
    }

    public int count() {
        return count;
    }

    /** Origine de la copie i du hub. */
    public BlockPos origin(int index) {
        return origin.offset(index * SPACING, 0, 0);
    }

    /** Copie du hub qui contient cette abscisse (la plus proche). */
    public int indexAt(double x) {
        if (origin == null) return 0;
        return Math.max(0, Math.min(count - 1, (int) Math.round((x - origin.getX()) / SPACING)));
    }

    public BlockPos originAt(double x) {
        return origin(indexAt(x));
    }

    /** Ajoute une copie ; renvoie son index. */
    public int addHub() {
        count++;
        setDirty();
        return count - 1;
    }

    /** Fin d'une construction : le plan est a jour quand toutes les copies ont ete construites. */
    public void built(ResourceKey<Level> dim, BlockPos hubOrigin, int index) {
        if (index == 0) {
            this.dimension = dim;
            this.origin = hubOrigin.immutable();
            this.stable = true;
        }
        if (index + 1 >= count) this.layout = HubBuilder.LAYOUT_VERSION;
        setDirty();
    }

    public boolean stableBuild() {
        return stable;
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
        this.stable = true;
        this.layout = HubBuilder.LAYOUT_VERSION;
        setDirty();
    }

    public void clear() {
        origin = null;
        setDirty();
    }
}
