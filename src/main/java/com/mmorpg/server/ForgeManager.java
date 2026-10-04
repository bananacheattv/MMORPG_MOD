package com.mmorpg.server;

import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.item.RpgEquipment;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.registry.ModDataComponents;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Artisanat et amelioration a la Forge Arcanique (toujours verifies cote serveur). */
public final class ForgeManager {
    private ForgeManager() {
    }

    private static boolean nearForge(ServerPlayer player, BlockPos pos) {
        return player.level().getBlockState(pos).is(ModBlocks.FORGE_ARCANIQUE.get()) && player.distanceToSqr(Vec3.atCenterOf(pos)) < 64;
    }

    public static int count(Inventory inv, Item item) {
        int n = 0;
        for (ItemStack s : inv) {
            if (s.is(item) && RpgEquipment.upgrade(s) == 0) n += s.getCount();
        }
        return n;
    }

    private static void remove(Inventory inv, Item item, int amount) {
        for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item) && RpgEquipment.upgrade(s) == 0) {
                int take = Math.min(amount, s.getCount());
                s.shrink(take);
                amount -= take;
            }
        }
    }

    public static void craft(ServerPlayer player, BlockPos pos, String recipeId, int times) {
        if (!nearForge(player, pos)) return;
        ForgeRecipes.Recipe r = ForgeRecipes.get(recipeId);
        if (r == null) return;
        PlayerData d = RpgPlayers.get(player);
        if (d.level < r.level()) {
            player.sendOverlayMessage(Component.literal("Niveau " + r.level() + " requis.").withColor(0xFF5050));
            return;
        }
        times = Math.max(1, Math.min(times, 16));
        Inventory inv = player.getInventory();
        RpgPlayers.depositCoins(player);
        int crafted = 0;
        for (int t = 0; t < times; t++) {
            boolean ok = RpgPlayers.get(player).gold >= r.gold();
            for (ForgeRecipes.Ingredient ing : r.ingredients()) {
                if (count(inv, ing.resolve()) < ing.count()) ok = false;
            }
            if (!ok) break;
            for (ForgeRecipes.Ingredient ing : r.ingredients()) remove(inv, ing.resolve(), ing.count());
            RpgPlayers.takeGold(player, r.gold());
            ItemStack result = new ItemStack(r.resultItem(), r.count());
            if (!inv.add(result)) player.drop(result, false, net.minecraft.util.Prediction.SERVER_ONLY);
            crafted++;
        }
        ServerLevel level = player.level();
        if (crafted > 0) {
            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1f, 1f);
            level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 25, 0.4, 0.4, 0.4, 0.6);
            Net.toPlayer(player, new Payloads.Notify(Payloads.Notify.LOOT, "FABRICATION RÉUSSIE",
                    r.resultItem().getName(new ItemStack(r.resultItem())).getString() + (crafted * r.count() > 1 ? " x" + crafted * r.count() : ""), 0x60E060));
        } else {
            player.sendOverlayMessage(Component.literal("Matériaux insuffisants.").withColor(0xFF5050));
        }
    }

    /** @param clover identifiant du trefle a consommer ("" = aucun) */
    public static void upgrade(ServerPlayer player, BlockPos pos, int slot, String clover) {
        if (!nearForge(player, pos)) return;
        Inventory inv = player.getInventory();
        if (slot < 0 || slot >= inv.getContainerSize()) return;
        ItemStack stack = inv.getItem(slot);
        if (!(stack.getItem() instanceof RpgEquipment)) return;
        int current = RpgEquipment.upgrade(stack);
        if (current >= RpgEquipment.MAX_UPGRADE) return;
        int target = current + 1;
        Item stone = ForgeRecipes.upgradeNeedsSuperior(target) ? ModItems.PIERRE_AMELIORATION_SUP.get() : ModItems.PIERRE_AMELIORATION.get();
        int gold = ForgeRecipes.upgradeGold(target);
        RpgPlayers.depositCoins(player);
        if (count(inv, stone) < 1 || RpgPlayers.get(player).gold < gold) {
            player.sendOverlayMessage(Component.literal("Il vous manque une pierre ou de l'or.").withColor(0xFF5050));
            return;
        }
        Item cloverItem = null;
        if (!clover.isEmpty()) {
            int[] range = ForgeRecipes.cloverRange(clover);
            if (range == null) return;
            if (!clover.equals(ForgeRecipes.cloverFor(target))) {
                player.sendOverlayMessage(Component.literal("Ce trèfle ne fonctionne que de +" + range[0] + " à +" + range[1] + ".").withColor(0xFF5050));
                return;
            }
            cloverItem = ModItems.get(clover);
            if (count(inv, cloverItem) < 1) {
                player.sendOverlayMessage(Component.literal("Vous n'avez plus ce trèfle.").withColor(0xFF5050));
                return;
            }
        }
        remove(inv, stone, 1);
        RpgPlayers.takeGold(player, gold);
        if (cloverItem != null) remove(inv, cloverItem, 1);
        ServerLevel level = player.level();
        // le trefle du palier garantit la reussite
        int chance = cloverItem != null ? 100 : ForgeRecipes.UPGRADE_CHANCE[target - 1];
        if (player.getRandom().nextInt(100) < chance) {
            stack.set(ModDataComponents.UPGRADE.get(), target);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1f, 1.4f);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.8f);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.3, 0.4, 0.3, 0.3);
            Net.toPlayer(player, new Payloads.Notify(Payloads.Notify.LOOT, "AMÉLIORATION RÉUSSIE !",
                    stack.getHoverName().getString(), 0xFFD040));
        } else {
            String extra = "";
            if (current >= 5) {
                stack.set(ModDataComponents.UPGRADE.get(), current - 1);
                extra = " (l'objet perd un niveau)";
            }
            level.playSound(null, pos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 0.8f, 1f);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.02);
            Net.toPlayer(player, new Payloads.Notify(Payloads.Notify.INFO, "ÉCHEC DE L'AMÉLIORATION", "La pierre s'est brisée" + extra, 0xFF5050));
        }
        RpgPlayers.recomputeAndSync(player);
    }
}
