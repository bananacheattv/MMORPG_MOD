package com.mmorpg.server;

import com.mmorpg.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** All consumption and rolls happen on the server. No client-selected reward. */
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
        reward(p);
        return true;
    }
    static String category(int roll) {
        if (roll < 0 || roll >= 100) throw new IllegalArgumentException("roll");
        return roll < 45 ? "potions" : roll < 70 ? "materiaux" : roll < 90 ? "or" : roll < 98 ? "charme" : "cosmetique";
    }
    public static void reward(ServerPlayer p) {
        grant(p, roll(p));
    }
    public record Loot(String item, int count, long gold, String category) {
        public net.minecraft.nbt.CompoundTag tag() {
            var t = new net.minecraft.nbt.CompoundTag();
            t.putString("item", item); t.putInt("count", count); t.putLong("gold", gold); t.putString("category", category);
            return t;
        }
    }
    public static Loot roll(ServerPlayer p) {
        var random = p.getRandom();
        String category = category(random.nextInt(100));
        int level = RpgPlayers.get(p).level;
        return switch (category) {
            case "potions" -> {
                boolean heal = random.nextBoolean();
                var item = heal ? (level >= 50 ? ModItems.POTION_SOIN_MAJEURE : level >= 20 ? ModItems.POTION_SOIN : ModItems.POTION_SOIN_MINEURE)
                    : (level >= 50 ? ModItems.POTION_MANA_MAJEURE : level >= 20 ? ModItems.POTION_MANA : ModItems.POTION_MANA_MINEURE);
                yield new Loot(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.get()).toString(), 3, 0, category);
            }
            case "materiaux" -> new Loot(level >= 50 ? "mmorpg:pierre_amelioration_sup" : "mmorpg:pierre_amelioration", 2, 0, category);
            case "or" -> new Loot("mmorpg:piece_or", 0, 40 + Math.max(1, level) * 3L, category);
            case "charme" -> new Loot(random.nextBoolean() ? "mmorpg:charme_chance" : "mmorpg:charme_experience", 1, 0, category);
            default -> new Loot("mmorpg:coffre_cosmetique", 1, 0, category);
        };
    }
    public static void grant(ServerPlayer p, Loot loot) {
        if (loot.gold > 0) RpgPlayers.addGold(p, loot.gold, true);
        if (loot.count > 0) RpgPlayers.give(p, com.mmorpg.crafting.ForgeRecipes.resolveItem(loot.item), loot.count);
        p.sendSystemMessage(Component.literal("✦ Butin d’aventure : " + loot.category).withColor(0xFFD040));
        p.level().playSound(null, p.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1, 1.2f);
        p.level().sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 1, p.getZ(), 25, .5, .5, .5, .1);
    }
}
