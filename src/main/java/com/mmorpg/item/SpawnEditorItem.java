package com.mmorpg.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class SpawnEditorItem extends Item {
    public SpawnEditorItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer p) com.mmorpg.server.SpawnEditor.open(p, null);
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer p)
            com.mmorpg.server.SpawnEditor.open(p, context.getClickedPos().relative(context.getClickedFace()));
        return InteractionResult.SUCCESS;
    }
}
