package com.mmorpg.block;

import com.mmorpg.server.AdventureLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Right click opens the reels. Unopened blocks can be picked back up. */
public class LuckyBlock extends Block {
    public LuckyBlock(Properties properties) { super(properties); }
    @Override public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, BlockEntity entity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, entity, tool);
        if (player.isAlive() && !player.isSpectator() && !player.hasInfiniteMaterials())
            com.mmorpg.server.RpgPlayers.give(player, com.mmorpg.registry.ModItems.LUCKY_BLOCK.get(), 1);
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level,
            BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) com.mmorpg.server.LuckyBlockManager.open(sp, pos);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
