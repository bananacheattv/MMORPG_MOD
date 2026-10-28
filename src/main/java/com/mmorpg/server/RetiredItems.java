package com.mmorpg.server;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/** Kept as registry tombstones so old worlds load; retired items have no playable source or use. */
public final class RetiredItems {
    public static boolean id(String value){
        if(value==null)return false;String id=value.startsWith("mmorpg:")?value.substring(7):value;
        return id.equals("lucky_block")||id.equals("cle_aventure")||id.equals("caisse_aventure")||id.startsWith("hache_")||id.startsWith("pelle_")||id.startsWith("parchemin_plan_hache_")||id.startsWith("parchemin_plan_pelle_");
    }
    public static boolean item(net.minecraft.world.item.Item item){var id=BuiltInRegistries.ITEM.getKey(item);return id.getNamespace().equals("mmorpg")&&id(id.getPath());}
    public static void clear(net.minecraft.world.Container inventory){LuckyMigration.inventory(inventory);for(int i=0;i<inventory.getContainerSize();i++)if(item(inventory.getItem(i).getItem()))inventory.setItem(i,ItemStack.EMPTY);}
}
