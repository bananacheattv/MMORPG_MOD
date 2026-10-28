package com.mmorpg.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Minecraft.class)
public abstract class AeloriaWindowTitleMixin {
 @Inject(method="createTitle",at=@At("HEAD"),cancellable=true)
 private void aeloria$title(CallbackInfoReturnable<String> ci){ci.setReturnValue("Aeloria 0.0 — "+((Minecraft)(Object)this).getUser().getName());}
}
