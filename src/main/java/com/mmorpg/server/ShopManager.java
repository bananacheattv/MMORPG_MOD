package com.mmorpg.server;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.ShopConfig;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.economy.ShopPricing;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Marchands : achat d'articles et rachat du butin contre de l'or (porte-monnaie). */
public final class ShopManager {
    private ShopManager() {
    }

    public static void open(ServerPlayer p, NpcEntity npc) {
        CompoundTag t = new CompoundTag();
        t.putInt("npc", npc.getId());
        t.putString("name", npc.displayName());
        t.putString("shop", ConfigManager.shopJson());
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.SHOP, t));
        p.level().playSound(null, npc.blockPosition(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 0.8f, 1.0f);
    }

    private static boolean nearMerchant(ServerPlayer p, int npcId) {
        Entity e = p.level().getEntity(npcId);
        return e instanceof NpcEntity npc && npc.role() == NpcEntity.Role.MARCHAND && npc.distanceToSqr(p) < 64;
    }

    public static void buy(ServerPlayer p, int npcId, int index, int times) {
        if (!nearMerchant(p, npcId)) return;
        ShopConfig cfg = ConfigManager.shop();
        if (index < 0 || index >= cfg.buy.size()) return;
        ShopConfig.Offer offer = cfg.buy.get(index);
        PlayerData d = RpgPlayers.get(p);
        if (d.level < offer.minLevel) {
            p.sendOverlayMessage(Component.literal("Niveau " + offer.minLevel + " requis.").withColor(0xFF5050));
            return;
        }
        Item item = ForgeRecipes.resolveItem(offer.item);
        if (item == null || item == Items.AIR) return;
        times = Math.max(1, Math.min(64, times));
        long cost = (long) offer.price * times;
        if (!RpgPlayers.takeGold(p, cost)) {
            p.sendOverlayMessage(Component.literal("Or insuffisant (" + cost + " requis).").withColor(0xFF5050));
            return;
        }
        RpgPlayers.give(p, item, offer.count * times);
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.6f, 1.1f);
        p.sendOverlayMessage(Component.literal("Achat : " + new ItemStack(item).getHoverName().getString() + " x" + offer.count * times
                + " (-" + cost + " or)").withColor(0xFFD040));
    }

    public static void sell(ServerPlayer p, int npcId, int slot, int amount, boolean all) {
        if (!nearMerchant(p, npcId)) return;
        Inventory inv = p.getInventory();
        if (slot < 0 || slot >= inv.getContainerSize()) return;
        ItemStack stack = inv.getItem(slot);
        ShopConfig cfg = ConfigManager.shop();
        int price = ShopPricing.sellPrice(stack, cfg);
        if (price <= 0) {
            p.sendOverlayMessage(Component.literal("Le marchand n'achète pas cet objet.").withColor(0xFF5050));
            return;
        }
        long total = 0;
        int sold = 0;
        String name = stack.getHoverName().getString();
        if (all) {
            Item item = stack.getItem();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(item) && ShopPricing.sellPrice(s, cfg) == price) {
                    total += (long) price * s.getCount();
                    sold += s.getCount();
                    inv.setItem(i, ItemStack.EMPTY);
                }
            }
        } else {
            int n = Math.max(1, Math.min(amount, stack.getCount()));
            stack.shrink(n);
            total = (long) price * n;
            sold = n;
        }
        if (sold > 0) {
            RpgPlayers.addGold(p, total, true);
            p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.5f, 1.4f);
            p.sendOverlayMessage(Component.literal("Vente : " + name + " x" + sold + " (+" + total + " or)").withColor(0xFFD040));
        }
    }
}
