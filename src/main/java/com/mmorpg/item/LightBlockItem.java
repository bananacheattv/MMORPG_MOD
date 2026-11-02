package com.mmorpg.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** Bloc de lumiere invisible (operateurs) : l'info-bulle explique l'usage. */
public class LightBlockItem extends BlockItem {
    public LightBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        out.accept(Component.literal("Outil d'administration (opérateurs)").withColor(0xFF8060));
        out.accept(Component.literal("Bloc invisible qui émet de la lumière (0-15)").withColor(0xA0A0A0));
        out.accept(Component.literal("Visible seulement quand on le tient").withColor(0xA0A0A0));
        out.accept(Component.literal("Clic droit : régler la lumière").withColor(0xA0A0A0));
    }
}
