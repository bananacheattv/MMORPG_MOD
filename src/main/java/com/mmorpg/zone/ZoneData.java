package com.mmorpg.zone;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Zones nommees d'Eldoria (colonnes rectangulaires sur toute la hauteur) avec leurs regles : construction, PvP, monstres hostiles. */
public class ZoneData extends SavedData {
    public record Zone(String id, String name, ResourceKey<Level> dimension, int minX, int minZ, int maxX, int maxZ,
                       boolean noBuild, boolean noPvp, boolean noMobs, String style, String subtitle) {
        public static final String STYLE_ZONE = "zone";
        public static final String STYLE_BOSS = "boss";

        static final Codec<Zone> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Zone::id),
                Codec.STRING.fieldOf("name").forGetter(Zone::name),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Zone::dimension),
                Codec.INT.fieldOf("minX").forGetter(Zone::minX),
                Codec.INT.fieldOf("minZ").forGetter(Zone::minZ),
                Codec.INT.fieldOf("maxX").forGetter(Zone::maxX),
                Codec.INT.fieldOf("maxZ").forGetter(Zone::maxZ),
                Codec.BOOL.optionalFieldOf("noBuild", true).forGetter(Zone::noBuild),
                Codec.BOOL.optionalFieldOf("noPvp", true).forGetter(Zone::noPvp),
                Codec.BOOL.optionalFieldOf("noMobs", true).forGetter(Zone::noMobs),
                Codec.STRING.optionalFieldOf("style", STYLE_ZONE).forGetter(Zone::style),
                Codec.STRING.optionalFieldOf("subtitle", "").forGetter(Zone::subtitle)
        ).apply(i, Zone::new));

        public boolean contains(ResourceKey<Level> dim, BlockPos pos) {
            return dim.equals(dimension) && pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }

        public long area() {
            return (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        }

        public Zone with(boolean noBuild, boolean noPvp, boolean noMobs) {
            return new Zone(id, name, dimension, minX, minZ, maxX, maxZ, noBuild, noPvp, noMobs, style, subtitle);
        }

        public Zone withStyle(String style, String subtitle) {
            return new Zone(id, name, dimension, minX, minZ, maxX, maxZ, noBuild, noPvp, noMobs, style, subtitle);
        }

        public boolean boss() {
            return STYLE_BOSS.equals(style);
        }
    }

    private static final Codec<ZoneData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Zone.CODEC.listOf().optionalFieldOf("zones", List.of()).forGetter(d -> d.zones)
    ).apply(i, ZoneData::new));
    public static final SavedDataType<ZoneData> TYPE = new SavedDataType<>(MMORPG.id("zones"), ZoneData::new, CODEC);

    private final List<Zone> zones;

    public ZoneData() {
        this.zones = new ArrayList<>();
    }

    private ZoneData(List<Zone> zones) {
        this.zones = new ArrayList<>(zones);
    }

    public static ZoneData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Zone> all() {
        return List.copyOf(zones);
    }

    /** Zone la plus petite qui contient la position (une zone imbriquee prime sur celle qui l'entoure). */
    public @Nullable Zone at(ResourceKey<Level> dim, BlockPos pos) {
        Zone best = null;
        for (Zone z : zones) {
            if (z.contains(dim, pos) && (best == null || z.area() < best.area())) best = z;
        }
        return best;
    }

    public @Nullable Zone byId(String id) {
        for (Zone z : zones) {
            if (z.id().equals(id)) return z;
        }
        return null;
    }

    /** Identifiant derive du nom : minuscules, sans accents, mots separes par _. */
    public static String slug(String name) {
        String s = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return s.length() > 40 ? s.substring(0, 40) : s;
    }

    public void add(Zone zone) {
        zones.removeIf(z -> z.id().equals(zone.id()));
        zones.add(zone);
        setDirty();
    }

    public boolean remove(String id) {
        boolean removed = zones.removeIf(z -> z.id().equals(id));
        if (removed) setDirty();
        return removed;
    }
}
