package com.mmorpg.item;

import com.mmorpg.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/** Objet d'equipement portant des statistiques RPG. */
public interface RpgEquipment {
    int MAX_UPGRADE = 20;
    double UPGRADE_BONUS = 0.06;

    RpgItemDef rpgDef();

    static int upgrade(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.UPGRADE.get(), 0);
    }

    static double upgradeMult(ItemStack stack) {
        return 1.0 + UPGRADE_BONUS * upgrade(stack);
    }
}
