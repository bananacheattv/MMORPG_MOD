package com.mmorpg.economy;

import com.mmorpg.config.ShopConfig;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.item.*;
import net.minecraft.world.item.Item;
import java.util.Comparator;

/** Shared stable ordering: shop indices must agree between client display and server purchase. */
public final class ShopSorting {
    public static int category(Item item){
        if(item instanceof RpgEquipment e)return e.rpgDef().slot()==RpgItemDef.Slot.WEAPON?0:1;
        if(item instanceof com.mmorpg.mine.MineTools.Pickaxe||item instanceof com.mmorpg.mine.MineTools.Tool)return 2;
        if(item instanceof RpgConsumableItem)return 3;
        if(item instanceof RpgMaterialItem)return 4;
        return 5;
    }
    public static int level(Item item){return item instanceof RpgEquipment e?e.rpgDef().level():1;}
    public static void sort(ShopConfig cfg){
        cfg.buy.sort(Comparator.comparingInt((ShopConfig.Offer o)->category(ForgeRecipes.resolveItem(o.item)))
            .thenComparingInt(o->Math.max(o.minLevel,level(ForgeRecipes.resolveItem(o.item))))
            .thenComparing(o->o.item));
        var ordered=new java.util.LinkedHashMap<String,Integer>();cfg.sellPrices.entrySet().stream()
            .sorted(Comparator.comparingInt((java.util.Map.Entry<String,Integer> e)->category(ForgeRecipes.resolveItem(e.getKey())))
                .thenComparingInt(e->level(ForgeRecipes.resolveItem(e.getKey()))).thenComparing(java.util.Map.Entry::getKey))
            .forEach(e->ordered.put(e.getKey(),e.getValue()));cfg.sellPrices=ordered;
    }
}
