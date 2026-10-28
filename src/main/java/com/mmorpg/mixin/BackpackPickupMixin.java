package com.mmorpg.mixin;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Inventory.class)
public abstract class BackpackPickupMixin {
 @Shadow @Final public Player player;
 @Inject(method="add(Lnet/minecraft/world/item/ItemStack;)Z",at=@At("RETURN"),cancellable=true)
 private void aeloria$overflow(ItemStack stack,CallbackInfoReturnable<Boolean> ci){if(!stack.isEmpty()&&player instanceof net.minecraft.server.level.ServerPlayer p&&com.mmorpg.server.Backpacks.insert(p,stack))ci.setReturnValue(true);}
}
