package com.mmorpg.client;

import com.mmorpg.crafting.ForgeRecipes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Formulation des objectifs et recompenses de quetes cote client. */
public final class QuestText {
    private QuestText() {
    }

    public static String targetName(String type, String target) {
        if ("collect".equals(type)) {
            Item item = ForgeRecipes.resolveItem(target);
            return item == null || item == Items.AIR ? target : new ItemStack(item).getHoverName().getString();
        }
        if ("*".equals(target)) return "monstres";
        if ("boss".equals(target)) return "boss";
        String key = target.contains(":") ? "entity." + target.replace(':', '.') : "entity.mmorpg." + target;
        return Component.translatable(key).getString();
    }

    public static String objective(CompoundTag o) {
        String type = o.getStringOr("type", "kill");
        int count = o.getIntOr("count", 1);
        String target = o.getStringOr("target", "*");
        return switch (type) {
            case "collect" -> "Rapporter " + count + " × " + targetName(type, target);
            case "level" -> "Atteindre le niveau " + count;
            default -> count == 1 ? "Vaincre : " + targetName(type, target) : "Vaincre " + count + " × " + targetName(type, target);
        };
    }

    public static String progress(CompoundTag o) {
        return o.getIntOr("progress", 0) + "/" + o.getIntOr("count", 1);
    }

    public static boolean done(CompoundTag o) {
        return o.getIntOr("progress", 0) >= o.getIntOr("count", 1);
    }
}
