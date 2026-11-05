package com.mmorpg.mixin;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Supprime les popups de tutoriel et de recettes de Minecraft (nos messages restent). */
@Mixin(ToastManager.class)
public abstract class NoTutorialToastMixin {
    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void aeloria$noVanillaTutorial(Toast toast, CallbackInfo ci) {
        if (toast instanceof TutorialToast || toast instanceof RecipeToast) {
            ci.cancel();
        }
    }
}
