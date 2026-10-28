package com.mmorpg.mixin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({ConnectScreen.class,LevelLoadingScreen.class})
public abstract class PortalLoadingScreenMixin extends Screen {
 protected PortalLoadingScreenMixin(Component title){super(title);}
 private long aeloria$lastNarration;
 @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
 private void aeloria$portal(GuiGraphicsExtractor g,int mx,int my,float a,CallbackInfo ci){
  com.mmorpg.client.ui.PortalLoadingView.draw(g,width,height);
  for(var child:java.util.List.copyOf(children()))if(child instanceof net.minecraft.client.gui.components.Button button)removeWidget(button);
  for(var widget:renderables)widget.extractRenderState(g,mx,my,a);
  long now=System.nanoTime();if(now-aeloria$lastNarration>3_000_000_000L){aeloria$lastNarration=now;minecraft.getNarrator().saySystemNow(Component.literal("Passage dans le portail. Téléportation en cours."));}
  ci.cancel();
 }
}
