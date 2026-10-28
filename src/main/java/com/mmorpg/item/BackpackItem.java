package com.mmorpg.item;
import net.minecraft.world.item.Item;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public final class BackpackItem extends Item {
 public final int tier;
 public BackpackItem(Properties p,int tier){super(p.stacksTo(1));this.tier=tier;}
 public int capacity(){return new int[]{6,12,18,24}[tier];}
 @Override public InteractionResult use(Level l,Player p,InteractionHand hand){if(p instanceof net.minecraft.server.level.ServerPlayer sp)com.mmorpg.server.Backpacks.equipAndOpen(sp,hand);return InteractionResult.SUCCESS;}
}
