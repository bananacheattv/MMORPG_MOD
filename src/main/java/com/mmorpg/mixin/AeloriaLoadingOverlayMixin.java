package com.mmorpg.mixin;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LoadingOverlay.class)
public abstract class AeloriaLoadingOverlayMixin {
 @Inject(method="registerTextures",at=@At("TAIL")) private static void aeloria$brand(TextureManager textures,CallbackInfo ci){com.mmorpg.client.ui.LoadingBrand.register(textures);}
 @Redirect(method="extractRenderState",at=@At(value="INVOKE",target="Ljava/util/function/IntSupplier;getAsInt()I")) private int aeloria$violet(java.util.function.IntSupplier supplier){return 0xFF160D26;}
 @Redirect(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"))
 private void aeloria$logo(GuiGraphicsExtractor g,RenderPipeline pipeline,Identifier old,int x,int y,float u,float v,int width,int height,int regionW,int regionH,int texW,int texH,int color){
  if(u>=0)return;int w=Math.min(330,(int)(g.guiWidth()*.64)),h=(int)(w/com.mmorpg.client.ui.Logo.FULL_RATIO);
  g.blit(RenderPipelines.GUI_TEXTURED,com.mmorpg.client.ui.LoadingBrand.LOGO,(g.guiWidth()-w)/2,(g.guiHeight()-h)/2,0,0,w,h,512,228,512,228,color);
 }
}
