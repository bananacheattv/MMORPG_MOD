package com.mmorpg.item;

import com.mmorpg.entity.dummy.TrainingDummyEntity;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.rpg.Rarity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/** Objet de placement d'un mannequin d'entrainement (clic droit sur un bloc). */
public class TrainingDummyItem extends RpgMaterialItem {
    private final int variant;

    public TrainingDummyItem(Properties properties, Rarity rarity, int variant, String lore) {
        super(properties.stacksTo(16), rarity, "Mannequin d'entraînement", lore);
        this.variant = variant;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        Player player = ctx.getPlayer();
        BlockPos base = ctx.getClickedPos();
        BlockState clicked = level.getBlockState(base);
        BlockPos pos = clicked.canBeReplaced() ? base : base.relative(ctx.getClickedFace());
        Direction face = ctx.getClickedFace();
        if (face == Direction.DOWN) return InteractionResult.FAIL;
        TrainingDummyEntity dummy = ModEntities.TRAINING_DUMMY.get().create(level, null, pos, EntitySpawnReason.SPAWN_ITEM_USE, true, false);
        if (dummy == null) return InteractionResult.FAIL;
        dummy.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player == null ? 0F : Mth.wrapDegrees(player.getYRot() + 180F), 0F);
        if (!level.noCollision(dummy)) {
            if (player != null) player.sendOverlayMessage(Component.literal("Pas assez de place pour poser le mannequin ici.").withColor(0xFF8080));
            return InteractionResult.FAIL;
        }
        dummy.setup(variant);
        level.addFreshEntity(dummy);
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
        if (player == null || !player.hasInfiniteMaterials()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        out.accept(Component.literal("Clic droit sur un bloc pour le poser").withColor(0x80C0FF));
        out.accept(Component.literal("Accroupi + clic droit sur lui : remise à zéro").withColor(0x808080));
    }
}
