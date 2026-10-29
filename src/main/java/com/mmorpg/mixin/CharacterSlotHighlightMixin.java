package com.mmorpg.mixin;
import com.mmorpg.client.screen.CharacterScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class CharacterSlotHighlightMixin {
    @Inject(method="extractSlotHighlightBack",at=@At("HEAD"),cancellable=true)
    private void aeloria$back(GuiGraphicsExtractor graphics,CallbackInfo ci){if((Object)this instanceof CharacterScreen screen){screen.slotHighlight(graphics,false);ci.cancel();}else if((Object)this instanceof com.mmorpg.client.screen.BackpackScreen bag){bag.slotHighlight(graphics,false);ci.cancel();}}
    @Inject(method="extractSlotHighlightFront",at=@At("HEAD"),cancellable=true)
    private void aeloria$front(GuiGraphicsExtractor graphics,CallbackInfo ci){if((Object)this instanceof CharacterScreen screen){screen.slotHighlight(graphics,true);ci.cancel();}else if((Object)this instanceof com.mmorpg.client.screen.BackpackScreen bag){bag.slotHighlight(graphics,true);ci.cancel();}}
}
