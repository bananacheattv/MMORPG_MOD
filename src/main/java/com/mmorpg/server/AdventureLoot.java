package com.mmorpg.server;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.LootTables;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** All consumption and rolls happen on the server. No client-selected reward. Tables : config/mmorpg/butins.json. */
public final class AdventureLoot {
    public static boolean openCrate(ServerPlayer p, ItemStack crate) {
        if (!p.isAlive() || p.isSpectator() || !crate.is(ModItems.CAISSE_AVENTURE.get())) return false;
        ItemStack key = ItemStack.EMPTY;
        for (ItemStack s : p.getInventory()) if (s.is(ModItems.CLE_AVENTURE.get())) { key = s; break; }
        if (key.isEmpty()) {
            p.sendOverlayMessage(Component.literal("Il vous faut une clé d’aventure."));
            return false;
        }
        if (!p.hasInfiniteMaterials()) { key.shrink(1); crate.shrink(1); }
        grant(p, roll(p, LootTables.CRATE));
        return true;
    }

    public record Loot(String item, int count, long gold, String category) {
        public CompoundTag tag() {
            var t = new CompoundTag();
            t.putString("item", item); t.putInt("count", count); t.putLong("gold", gold); t.putString("category", category);
            return t;
        }
    }

    /** Tirage pondere dans une table ; une table vide ou sans poids donne quelques pieces d'or. */
    public static Loot roll(ServerPlayer p, String table) {
        List<LootTables.Entry> entries = ConfigManager.loot().table(table);
        int total = 0;
        for (LootTables.Entry e : entries) total += e.weight;
        if (total <= 0) return new Loot("mmorpg:piece_or", 0, 25, "Pièces d’or");
        int r = p.getRandom().nextInt(total);
        LootTables.Entry pick = entries.get(entries.size() - 1);
        for (LootTables.Entry e : entries) {
            r -= e.weight;
            if (r < 0) { pick = e; break; }
        }
        int amount = pick.min + (pick.max > pick.min ? p.getRandom().nextInt(pick.max - pick.min + 1) : 0);
        if (pick.isGold()) return new Loot("mmorpg:piece_or", 0, amount, "Pièces d’or");
        Item item = ForgeRecipes.resolveItem(pick.item);
        if (item == null || item == Items.AIR) return new Loot("mmorpg:piece_or", 0, 25, "Pièces d’or");
        return new Loot(pick.item, amount, 0, new ItemStack(item).getHoverName().getString());
    }

    /** Contenu d'une table pour l'affichage (objet, quantites, chance en pour mille). */
    public static ListTag preview(String table) {
        List<LootTables.Entry> entries = ConfigManager.loot().table(table);
        int total = 0;
        for (LootTables.Entry e : entries) total += e.weight;
        ListTag list = new ListTag();
        for (LootTables.Entry e : entries) {
            if (e.weight <= 0) continue;
            CompoundTag t = new CompoundTag();
            t.putString("item", e.isGold() ? "mmorpg:piece_or" : e.item);
            t.putBoolean("gold", e.isGold());
            t.putInt("min", e.min);
            t.putInt("max", e.max);
            t.putInt("permille", total <= 0 ? 0 : Math.round(e.weight * 1000f / total));
            list.add(t);
        }
        return list;
    }

    public static void grant(ServerPlayer p, Loot loot) {
        grant(p, loot, "Butin d’aventure", 0xFFD040);
    }

    public static void grant(ServerPlayer p, Loot loot, String source, int color) {
        if (loot.gold > 0) RpgPlayers.addGold(p, loot.gold, true);
        if (loot.count > 0) RpgPlayers.give(p, ForgeRecipes.resolveItem(loot.item), loot.count);
        String what = loot.gold > 0 ? loot.gold + " pièces d’or" : loot.count + " × " + loot.category;
        p.sendSystemMessage(Component.literal("✦ " + source + " : " + what).withColor(color));
        p.level().playSound(null, p.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1, 1.2f);
        p.level().sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 1, p.getZ(), 25, .5, .5, .5, .1);
    }
}
