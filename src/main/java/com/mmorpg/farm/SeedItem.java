package com.mmorpg.farm;

import com.mmorpg.item.ItemTooltips;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Graine d'Aeloria : se seme sur une terre labouree, a partir du niveau de la plante. */
public class SeedItem extends BlockItem {
    private final FarmCrops.Crop crop;

    public SeedItem(Properties properties, Supplier<? extends Block> block, FarmCrops.Crop crop) {
        super(block.get(), properties);
        this.crop = crop;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer sp) {
            PlayerData d = RpgPlayers.get(sp);
            if (d.level < crop.minLevel()) {
                sp.sendOverlayMessage(Component.literal("Niveau " + crop.minLevel() + " requis pour semer : " + crop.name() + ".").withColor(0xFF7060));
                return InteractionResult.FAIL;
            }
        }
        return super.useOn(context);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId()).withColor(crop.rarity().rgb());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        String grow = crop.slow() <= 1 ? "pousse vite" : crop.slow() == 2 ? "pousse lentement" : crop.slow() == 3 ? "pousse très lentement" : "pousse extrêmement lentement";
        ItemTooltips.simple(crop.rarity(), "Graine", "Se sème sur une terre labourée (houe) à partir du niveau " + crop.minLevel() + ".\n"
                + "Cette plante " + grow + " ; une houe de palier élevé donne des récoltes en plus.", out);
    }
}
