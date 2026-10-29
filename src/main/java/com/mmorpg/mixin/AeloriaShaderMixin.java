package com.mmorpg.mixin;
import com.mmorpg.client.ClientConfig;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class AeloriaShaderMixin {
 @Inject(method="update",at=@At("RETURN"))
 private void aeloria$postEffect(net.minecraft.client.DeltaTracker delta,CallbackInfo ci){
  ClientConfig.ShaderMode mode=ClientConfig.SHADER_MODE.get();
  if(mode.effect!=null)((GameRenderer)(Object)this).getRequestedPostEffects().add(Identifier.fromNamespaceAndPath("mmorpg",mode.effect));
 }
}
