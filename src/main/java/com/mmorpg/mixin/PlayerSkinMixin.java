package com.mmorpg.mixin;

import com.mmorpg.client.model.imported.SkinOverride;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void aeloria$chosenSkin(CallbackInfoReturnable<PlayerSkin> callback) {
        callback.setReturnValue(SkinOverride.forPlayer((AbstractClientPlayer) (Object) this, callback.getReturnValue()));
    }
}
