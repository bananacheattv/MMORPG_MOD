package com.mmorpg.mixin;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ScreenEffectRenderer.class)
public abstract class HideFireOverlayMixin {
 @Inject(method="submitFire",at=@At("HEAD"),cancellable=true)
 private static void aeloria$clearView(PoseStack pose,SubmitNodeCollector collector,TextureAtlasSprite sprite,CallbackInfo ci){ci.cancel();}
}
