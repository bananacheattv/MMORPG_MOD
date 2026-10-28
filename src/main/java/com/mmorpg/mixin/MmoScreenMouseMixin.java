package com.mmorpg.mixin;

import com.mmorpg.client.ui.MmoScreen;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Les ecrans du mod sont mis a l'echelle (cadre identique a l'inventaire) : la souris est convertie dans leur repere. */
@Mixin(MouseHandler.class)
public abstract class MmoScreenMouseMixin {
    @Inject(method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;D)D", at = @At("RETURN"), cancellable = true)
    private static void aeloria$scaleX(Window window, double x, CallbackInfoReturnable<Double> ci) {
        if (Minecraft.getInstance().gui.screen() instanceof MmoScreen s) ci.setReturnValue(ci.getReturnValue() / s.uiScale());
    }

    @Inject(method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;D)D", at = @At("RETURN"), cancellable = true)
    private static void aeloria$scaleY(Window window, double y, CallbackInfoReturnable<Double> ci) {
        if (Minecraft.getInstance().gui.screen() instanceof MmoScreen s) ci.setReturnValue(ci.getReturnValue() / s.uiScale());
    }
}
