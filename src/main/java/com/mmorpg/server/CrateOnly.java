package com.mmorpg.server;

import com.mmorpg.item.PetEggItem;
import com.mmorpg.registry.ModItems;
import net.minecraft.world.item.Item;

/** Cosmetiques (coffre cosmetique) et familiers (oeufs) ne s'obtiennent que dans les caisses et les Lucky Blocks. */
public final class CrateOnly {
    private CrateOnly() {
    }

    public static boolean is(Item item) {
        return item instanceof PetEggItem || item == ModItems.COFFRE_COSMETIQUE.get();
    }
}
