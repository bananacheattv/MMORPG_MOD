package com.mmorpg.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;

/** Lecture du lieu et du moment d'un lancer : habitat selon le biome et la profondeur, nuit, pluie. */
public final class FishingWorld {
    private FishingWorld() {
    }

    public static FishCatalog.Habitat habitat(ServerLevel level, BlockPos pos) {
        int depth = 0;
        BlockPos.MutableBlockPos p = pos.mutable();
        while (depth < 16 && level.getFluidState(p).is(FluidTags.WATER)) {
            depth++;
            p.move(0, -1, 0);
        }
        Holder<Biome> biome = level.getBiome(pos);
        return FishingRules.habitat(biome.is(BiomeTags.IS_DEEP_OCEAN), biome.is(BiomeTags.IS_OCEAN), biome.is(BiomeTags.IS_RIVER), biome.is(Tags.Biomes.IS_SWAMP),
                biome.is(Tags.Biomes.IS_JUNGLE), biome.is(Tags.Biomes.IS_COLD), depth);
    }

    public static boolean night(ServerLevel level) {
        return !level.isBrightOutside() && !level.isRaining();
    }

    public static boolean rain(ServerLevel level, BlockPos pos) {
        return level.isRainingAt(pos.above());
    }
}
