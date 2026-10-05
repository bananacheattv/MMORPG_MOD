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
        var random = p.getRandom();
        String category = category(random.nextInt(100));
        int level = RpgPlayers.get(p).level;
        switch (category) {
            case "potions" -> {
                boolean heal = random.nextBoolean();
                var item = heal ? (level >= 50 ? ModItems.POTION_SOIN_MAJEURE : level >= 20 ? ModItems.POTION_SOIN : ModItems.POTION_SOIN_MINEURE)
                    : (level >= 50 ? ModItems.POTION_MANA_MAJEURE : level >= 20 ? ModItems.POTION_MANA : ModItems.POTION_MANA_MINEURE);
                RpgPlayers.give(p, item.get(), 3);
            }
            case "materiaux" -> RpgPlayers.give(p, level >= 50 ? ModItems.PIERRE_AMELIORATION_SUP.get() : ModItems.PIERRE_AMELIORATION.get(), 2);
            case "or" -> RpgPlayers.addGold(p, 40 + Math.max(1, level) * 3L, true);
            case "charme" -> RpgPlayers.give(p, random.nextBoolean() ? ModItems.CHARME_CHANCE.get() : ModItems.CHARME_EXPERIENCE.get(), 1);
            case "cosmetique" -> RpgPlayers.give(p, ModItems.COFFRE_COSMETIQUE.get(), 1);
        }
        p.sendSystemMessage(Component.literal("✦ Butin d’aventure : " + category).withColor(0xFFD040));
        p.level().playSound(null, p.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1, 1.2f);
        p.level().sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 1, p.getZ(), 25, .5, .5, .5, .1);
    }
}
