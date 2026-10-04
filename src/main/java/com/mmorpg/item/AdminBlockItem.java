package com.mmorpg.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** Bloc reserve aux operateurs (teleporteur) : l'info-bulle l'indique. */
public class AdminBlockItem extends BlockItem {
    public AdminBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        out.accept(Component.literal("Outil d'administration (opérateurs)").withColor(0xFF8060));
        out.accept(Component.literal("Poser : crée une ville, un donjon ou un point de passage").withColor(0xA0A0A0));
        out.accept(Component.literal("Accroupi + clic droit : configurer").withColor(0xA0A0A0));
        out.accept(Component.literal("Maj + clic gauche : retirer").withColor(0xA0A0A0));
    }
}
