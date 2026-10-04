package com.mmorpg.item;

import com.mmorpg.MMORPG;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import java.util.function.Consumer;

/** Piece d'armure RPG. La protection est geree par la statistique Defense du systeme RPG. */
public class RpgArmorItem extends Item implements RpgEquipment {
    private final RpgItemDef def;
    private final String pieceLabel;

    public RpgArmorItem(Properties properties, RpgItemDef def, EquipmentSlot slot, String style, String pieceLabel) {
        super(properties.stacksTo(1).component(DataComponents.EQUIPPABLE,
                Equippable.builder(slot)
                        .setEquipSound(sound(style))
                        .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, MMORPG.id(def.setId())))
                        .build()));
        this.def = def;
        this.pieceLabel = pieceLabel;
    }

    private static Holder<SoundEvent> sound(String style) {
        return switch (style) {
            case "robe", "leather" -> SoundEvents.ARMOR_EQUIP_LEATHER;
            default -> SoundEvents.ARMOR_EQUIP_IRON;
        };
    }

    @Override
    public RpgItemDef rpgDef() {
        return def;
    }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent name = Component.translatable(this.getDescriptionId()).withColor(def.rarity().rgb());
        int up = RpgEquipment.upgrade(stack);
        if (up > 0) name.append(Component.literal(" +" + up).withColor(0xFFD040));
        return name;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        ItemTooltips.equipment(stack, def, pieceLabel, out);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return RpgEquipment.upgrade(stack) >= 7 || super.isFoil(stack);
    }
}
