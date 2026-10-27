package com.mmorpg.farm;

import com.mmorpg.mine.MineTools;
import com.mmorpg.registry.ModItems;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Plante d'Aeloria : pousse comme une culture normale mais plus lentement selon son rang. A la recolte d'une plante mure, une houe de
 * palier superieur donne des recoltes supplementaires (20 % par palier) et un peu d'experience.
 */
public class AeloriaCropBlock extends CropBlock {
    private final FarmCrops.Crop crop;

    public AeloriaCropBlock(Properties properties, FarmCrops.Crop crop) {
        super(properties);
        this.crop = crop;
    }

    public FarmCrops.Crop crop() {
        return crop;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return FarmCrops.SEEDS.get(crop.id()).get();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (com.mmorpg.plains.PlainsManager.inPlains(level, pos)) return;                 // dans les plaines, les champs repoussent a heure fixe
        if (crop.slow() > 1 && random.nextInt(crop.slow()) != 0) return;
        super.randomTick(state, level, pos, random);
    }

    @Override
    public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!isMaxAge(state)) return;
        int hoe = MineTools.hoeTier(tool);
        int rank = FarmCrops.CROPS.indexOf(crop);
        RpgPlayers.giveGatherXp(player, 2 + rank * 3L);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.0);
        if (hoe <= 0) return;
        bonus(level, pos, hoe,com.mmorpg.mine.ToolRunes.rank(tool,0));
        if (hoe < 2) return;
        // houe d'Argentite et au-dela : recolte en zone (rayon 1, ou 2 des le Glacium), replante la plante cassee et ses voisines
        level.setBlock(pos, getStateForAge(0), 3);
        int r = Math.min(4,(hoe >= 4 ? 2 : 1)+com.mmorpg.mine.ToolRunes.rank(player.getMainHandItem(),2)/2);
        int n = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos at = pos.offset(dx, 0, dz);
                BlockState st = level.getBlockState(at);
                if (!(st.getBlock() instanceof AeloriaCropBlock cb) || !cb.isMaxAge(st)) continue;
                net.minecraft.world.level.block.Block.dropResources(st, level, at, null, player, tool);
                cb.bonus(level, at, hoe,com.mmorpg.mine.ToolRunes.rank(tool,0));
                level.setBlock(at, cb.getStateForAge(0), 3);
                RpgPlayers.giveGatherXp(player, 2 + FarmCrops.CROPS.indexOf(cb.crop) * 3L);
                n++;
            }
        }
        if (n > 0) player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Récolte en zone : +" + n + " plante" + (n > 1 ? "s" : "")).withColor(crop.color()));
    }

    /** Recoltes supplementaires de la houe : deux tirages de 10 % par palier. */
    private void bonus(ServerLevel level, BlockPos pos, int hoe,int rune) {
        int extra = 0;
        for (int i = 0; i < 2; i++) {
            if (level.getRandom().nextInt(100) < hoe * 10+rune*10) extra++;
        }
        if (extra > 0) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, new ItemStack(ModItems.get(crop.product()), extra)));
        }
    }
}
