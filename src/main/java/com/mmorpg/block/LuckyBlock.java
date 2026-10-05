package com.mmorpg.block;

import com.mmorpg.server.AdventureLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Breaking in survival yields one reward; no block loot, silk touch or explosion duplication. */
public class LuckyBlock extends Block {
    public LuckyBlock(Properties properties) { super(properties); }
    @Override public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, BlockEntity entity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, entity, tool);
        if (player.isAlive() && !player.isSpectator() && !player.hasInfiniteMaterials()) AdventureLoot.reward(player);
    }
}
