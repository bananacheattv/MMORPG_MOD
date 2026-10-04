package com.mmorpg.economy;

import com.mmorpg.config.ShopConfig;
import com.mmorpg.item.RpgEquipment;
import com.mmorpg.item.RpgMaterialItem;
import com.mmorpg.rpg.Rarity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Prix de rachat des objets par les marchands (code partage client/serveur). */
public final class ShopPricing {
    private ShopPricing() {
    }

    private static int rarityPrice(ShopConfig cfg, Rarity rarity) {
        Integer p = cfg.materialPriceByRarity.get(rarity.name());
        return p == null ? 1 : p;
    }

    /** Prix d'une unite de l'objet, ou 0 si le marchand ne l'achete pas. */
    public static int sellPrice(ItemStack stack, ShopConfig cfg) {
        if (stack.isEmpty()) return 0;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Integer explicit = cfg.sellPrices.get(id);
        if (explicit != null) return Math.max(0, explicit);
        if (stack.getItem() instanceof RpgEquipment eq) {
            double base = rarityPrice(cfg, eq.rpgDef().rarity()) * cfg.equipmentMultiplier;
            return (int) Math.max(1, Math.round(base * (1 + 0.25 * RpgEquipment.upgrade(stack))));
        }
        if (stack.getItem() instanceof RpgMaterialItem m) {
            return rarityPrice(cfg, m.rarity());
        }
        return 0;
    }
}
