package com.mmorpg.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Arme de melee RPG (epees, haches, marteaux). Les degats reels sont calcules par le systeme de combat RPG. */
public class RpgWeaponItem extends Item implements RpgEquipment {
    protected final RpgItemDef def;
    protected final ItemDefs.WeaponKind kind;

    public RpgWeaponItem(Properties properties, RpgItemDef def, ItemDefs.WeaponKind kind) {
        super(withAttributes(properties, kind));
        this.def = def;
        this.kind = kind;
    }

    static Properties withAttributes(Properties p, ItemDefs.WeaponKind kind) {
        double dmg;
        double speed;
        switch (kind) {
            case AXE -> { dmg = 7; speed = 1.1; }
            case HAMMER -> { dmg = 7; speed = 1.0; }
            case STAFF -> { dmg = 3; speed = 1.4; }
            case BOW -> { dmg = 1; speed = 1.6; }
            default -> { dmg = 5; speed = 1.6; }
        }
        ItemAttributeModifiers mods = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, dmg, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
        return p.stacksTo(1).attributes(mods);
    }

    @Override
    public RpgItemDef rpgDef() {
        return def;
    }

    public ItemDefs.WeaponKind kind() {
        return kind;
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
        String kindLabel = switch (kind) {
            case AXE -> "Hache";
            case HAMMER -> "Marteau";
            case STAFF -> "Bâton magique";
            case BOW -> "Arc";
            default -> "Épée";
        };
        ItemTooltips.equipment(stack, def, kindLabel, out);
        if (kind == ItemDefs.WeaponKind.STAFF) {
            out.accept(Component.literal("Clic droit : projectile magique (3 mana)").withColor(0x80C0FF));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return RpgEquipment.upgrade(stack) >= 7 || super.isFoil(stack);
    }
}
