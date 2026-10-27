package com.mmorpg.item;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public final class ArmorStandEditorItem extends Item {
    public ArmorStandEditorItem(Properties p){super(p.stacksTo(1));}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer p)com.mmorpg.server.ArmorStandEditor.aim(p);return InteractionResult.SUCCESS;
    }
}
