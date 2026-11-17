package com.mmorpg.mixin;

import com.mmorpg.client.ui.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Les infobulles sont dessinees apres l'image, hors de la mise a l'echelle des ecrans du mod : leur position
 * (donnee dans le repere de l'ecran) est reconvertie en pixels d'interface pour rester collee au curseur
 * et ne jamais depasser de la fenetre.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class TooltipScaleMixin {
    private static final String TARGET = "setTooltipForNextFrameInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;ZZ)V";

    @ModifyVariable(method = TARGET, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int aeloria$tooltipX(int x) {
        return Math.round(x * Ui.renderScale);
    }

    @ModifyVariable(method = TARGET, at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int aeloria$tooltipY(int y) {
        return Math.round(y * Ui.renderScale);
    }
}
