package com.mmorpg.mixin;

import com.mmorpg.server.InventoryRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class InventoryClickMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void mmorpg$restrict(int index, int button, ContainerInput input, Player player, CallbackInfo ci) {
        if (!InventoryRules.restricted(player)) return;
        if (input == ContainerInput.SWAP && button == 40
                || (Object) this instanceof InventoryMenu && InventoryRules.blockedSlot(index)) ci.cancel();
    }
}
