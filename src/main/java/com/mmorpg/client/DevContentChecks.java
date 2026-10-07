package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.rpg.PlayerClass;
import net.minecraft.world.item.Items;

/** Invariants de contenu executes uniquement par la vitrine de developpement. */
final class DevContentChecks {
    static void verify() {
        for (PlayerClass cls : PlayerClass.values()) {
            if (!cls.isPlayable()) continue;
            if (ItemDefs.SETS.values().stream().filter(s -> s.cls() == cls).count() != 5)
                throw new IllegalStateException("Five armor sets required: " + cls);
            if (ItemDefs.WEAPONS.stream().filter(w -> w.cls() == cls).count() < 6)
                throw new IllegalStateException("Weapon progression incomplete: " + cls);
        }
        for (var recipe : ForgeRecipes.all()) {
            if (recipe.resultItem() == null || recipe.resultItem() == Items.AIR) throw new IllegalStateException("Missing output " + recipe.id());
            for (var ingredient : recipe.ingredients()) {
                if (ingredient.resolve() == null || ingredient.resolve() == Items.AIR || ingredient.count() < 1)
                    throw new IllegalStateException("Invalid ingredient " + recipe.id() + ": " + ingredient.item());
            }
        }
        for (var weapon : ItemDefs.WEAPONS) {
            if (ForgeRecipes.get(weapon.id()) == null) throw new IllegalStateException("Missing weapon recipe " + weapon.id());
        }
        for (var set : ItemDefs.SETS.values()) {
            for (String piece : ItemDefs.PIECES) {
                if (ForgeRecipes.get(set.id() + "_" + piece) == null) throw new IllegalStateException("Missing armor recipe " + set.id());
            }
        }
        MMORPG.LOGGER.info("[CODEX CHECKS] CONTENT PASS: equipment progression and {} resolved recipes", ForgeRecipes.all().size());
    }
}
