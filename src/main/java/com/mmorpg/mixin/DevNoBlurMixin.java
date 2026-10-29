package com.mmorpg.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** QA uniquement (-Dmmorpg.noblur=true) : le flou est trop lent en rendu logiciel. Sans effet en jeu normal. */
@Mixin(GameRenderer.class)
public abstract class DevNoBlurMixin {
    private static final boolean NO_BLUR = Boolean.getBoolean("mmorpg.noblur");

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void aeloria$noBlur(CallbackInfo ci) {
        if (NO_BLUR) ci.cancel();
    }
}
