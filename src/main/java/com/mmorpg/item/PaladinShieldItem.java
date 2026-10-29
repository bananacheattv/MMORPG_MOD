package com.mmorpg.item;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
/** Bound shield; its regenerating guard is controlled by the RPG server, never by the main-hand weapon. */
public final class PaladinShieldItem extends Item {
 public PaladinShieldItem(Properties p){super(p.stacksTo(1));}
 @Override public InteractionResult use(Level level,Player player,InteractionHand hand){if(hand!=InteractionHand.OFF_HAND)return InteractionResult.PASS;if(player instanceof net.minecraft.server.level.ServerPlayer p&&!com.mmorpg.combat.ShieldGuard.canStart(p))return InteractionResult.FAIL;player.startUsingItem(hand);return InteractionResult.SUCCESS;}
 @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return ItemUseAnimation.NONE;}
 @Override public int getUseDuration(ItemStack stack,LivingEntity user){return 72000;}
}
