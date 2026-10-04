package com.mmorpg.item;

import com.mmorpg.rpg.Rarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Materiau d'artisanat (butin de monstres et de boss) avec rarete et description. */
public class RpgMaterialItem extends Item {
    protected final Rarity rarity;
    protected final String kind;
    protected final String lore;

    public RpgMaterialItem(Properties properties, Rarity rarity, String kind, String lore) {
        super(properties);
        this.rarity = rarity;
        this.kind = kind;
        this.lore = lore;
    }

    public Rarity rarity() {
        return rarity;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId()).withColor(rarity.rgb());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        ItemTooltips.simple(rarity, kind, lore, out);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return rarity.ordinal() >= Rarity.LEGENDAIRE.ordinal();
    }
}
