package com.mmorpg.crafting;

import com.mmorpg.rpg.PlayerData;

/** Individual permanent equipment recipes, shared across all network backends. */
public final class ForgeProgression {
    public static final int TIERS = 5;
    public static String scroll(int tier) { return blueprint(recipes(tier).stream().filter(r->r.category()==ForgeRecipes.Category.ARMURES).findFirst().orElseGet(()->recipes(tier).getFirst())); }
    public static int tier(int level) { return Math.clamp((Math.max(1, level) - 1) / 20 + 1, 1, TIERS); }
    public static String blueprint(ForgeRecipes.Recipe recipe) { return "parchemin_plan_" + recipe.id(); }
    public static java.util.List<ForgeRecipes.Recipe> recipes(int tier) { return ForgeRecipes.all().stream().filter(r -> required(r) == tier).toList(); }
    public static String reward(PlayerData data, int tier, java.util.Random random) {
        var choices=recipes(tier);var unknown=choices.stream().filter(r->!unlocked(data,r)).toList();if(!unknown.isEmpty())choices=unknown;
        var ownClass=choices.stream().filter(r->r.playerClass()==com.mmorpg.rpg.PlayerClass.NONE||r.playerClass()==data.playerClass).toList();
        if(!ownClass.isEmpty())choices=ownClass;
        return blueprint(choices.get(random.nextInt(choices.size())));
    }
    public static int required(ForgeRecipes.Recipe recipe) {
        return switch (recipe.category()) {
            case ARMES, ARMURES -> recipe.level() <= 1 ? 0 : tier(recipe.level());
            case OUTILS -> tier(recipe.level());
            default -> 0;
        };
    }
    public static boolean unlocked(PlayerData data, ForgeRecipes.Recipe recipe) {
        int tier = required(recipe);
        return tier == 0 || data.forgeRecipes.contains(recipe.id());
    }
    public static boolean learn(PlayerData data, String recipe) {
        var r=ForgeRecipes.get(recipe);if(r==null||required(r)==0||!data.forgeRecipes.add(recipe))return false;
        data.dirty=true;return true;
    }
}
