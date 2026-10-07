package com.mmorpg.server;

import com.mmorpg.item.PetEggItem;
import com.mmorpg.registry.ModItems;
import net.minecraft.world.item.Item;

/** Cosmetiques (coffre cosmetique), familiers (oeufs) et montures (sceaux) ne s'obtiennent que dans les caisses et les Lucky Blocks. */
public final class CrateOnly {
    private CrateOnly() {
    }

    public static boolean is(Item item) {
        return item instanceof PetEggItem || item instanceof com.mmorpg.item.MysteryItem || item instanceof com.mmorpg.item.MountSealItem || item == ModItems.COFFRE_COSMETIQUE.get();
    }
}
