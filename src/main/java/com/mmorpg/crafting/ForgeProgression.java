package com.mmorpg.crafting;

import com.mmorpg.rpg.PlayerData;

/** Five permanent recipe licenses, shared across classes and all equipment categories. */
public final class ForgeProgression {
    public static final int TIERS = 5;
    public static String scroll(int tier) { return "parchemin_forge_" + tier; }
    public static int tier(int level) { return Math.clamp((Math.max(1, level) - 1) / 20 + 1, 1, TIERS); }
    public static int required(ForgeRecipes.Recipe recipe) {
        return switch (recipe.category()) {
            case ARMES, ARMURES -> recipe.level() <= 1 ? 0 : tier(recipe.level());
            case OUTILS -> tier(recipe.level());
            default -> 0;
        };
    }
    public static boolean unlocked(PlayerData data, ForgeRecipes.Recipe recipe) {
        int tier = required(recipe);
        return tier == 0 || (data.forgeBlueprints & (1 << (tier - 1))) != 0;
    }
    public static boolean learn(PlayerData data, int tier) {
        if (tier < 1 || tier > TIERS || (data.forgeBlueprints & (1 << (tier - 1))) != 0) return false;
        data.forgeBlueprints |= 1 << (tier - 1);
        data.dirty = true;
        return true;
    }
}
