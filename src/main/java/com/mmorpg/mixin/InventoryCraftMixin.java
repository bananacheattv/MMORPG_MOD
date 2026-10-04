package com.mmorpg.mixin;

import com.mmorpg.server.InventoryRules;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryCraftMixin {
    @Shadow @Final private Player owner;
    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void mmorpg$noInventoryCrafting(Container container, CallbackInfo ci) {
        if (InventoryRules.restricted(owner)) {
            ((InventoryMenu) (Object) this).getResultSlot().set(ItemStack.EMPTY);
            ci.cancel();
        }
    }
}
