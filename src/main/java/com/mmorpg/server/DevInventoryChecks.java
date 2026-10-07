package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Verification dans un monde jetable : runClient -Pshowcase=ecrans -Dmmorpg.checkInventory=true. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevInventoryChecks {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkInventory") || !(event.getEntity() instanceof ServerPlayer p)) return;
        var inventory = p.getInventory();
        ItemStack[] saved = new ItemStack[inventory.getContainerSize()];
        for (int i = 0; i < saved.length; i++) saved[i] = inventory.getItem(i).copy();
        GameType mode = p.gameMode.getGameModeForPlayer();
        try {
            p.setGameMode(GameType.SURVIVAL);
            for (int i = 0; i < saved.length; i++) inventory.setItem(i, ItemStack.EMPTY);
            var menu = p.inventoryMenu;
            menu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS, 4));
            menu.getSlot(2).set(new ItemStack(Items.OAK_PLANKS, 4));
            check(menu.getSlot(0).getItem().isEmpty(), "recette 2x2 bloquee");
            menu.getSlot(45).set(new ItemStack(Items.TORCH, 3));
            InventoryRules.apply(p);
            check(inventory.countItem(Items.OAK_PLANKS) == 8 && inventory.countItem(Items.TORCH) == 3, "objets restitues sans perte");
            for (int i : new int[]{0, 1, 2, 3, 4, 45}) {
                check(!menu.getSlot(i).mayPlace(new ItemStack(Items.STONE)) && !menu.getSlot(i).isActive(), "emplacement ferme " + i);
            }
            menu.getSlot(9).set(new ItemStack(Items.STONE, 7));
            menu.clicked(9, 40, ContainerInput.SWAP, p);
            check(p.getOffhandItem().isEmpty() && menu.getSlot(9).getItem().getCount() == 7, "raccourci F inventaire bloque");
            var swap = new LivingSwapItemsEvent.Hands(p);
            NeoForge.EVENT_BUS.post(swap);
            check(swap.isCanceled(), "raccourci F en jeu bloque");
            p.setGameMode(GameType.CREATIVE);
            InventoryRules.apply(p);
            check(menu.getSlot(1).mayPlace(new ItemStack(Items.STONE)) && menu.getSlot(45).isActive(), "mode creatif restaure");
            MMORPG.LOGGER.info("[CODEX CHECKS] INVENTORY PASS: crafting, restoration, slots, swaps, creative");
        } finally {
            for (int i = 0; i < saved.length; i++) inventory.setItem(i, saved[i]);
            p.setGameMode(mode);
            InventoryRules.apply(p);
        }
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("Inventory check failed: " + label);
    }
}
