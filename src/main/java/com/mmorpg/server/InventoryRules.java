package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = MMORPG.MODID)
public final class InventoryRules {
    private InventoryRules() {}
    public static boolean blockedSlot(int slot) { return slot >= 0 && slot < 5 || slot == InventoryMenu.SHIELD_SLOT; }
    public static boolean restricted(Player p) { return !p.isCreative(); }

    private static final class DisabledSlot extends Slot {
        final Slot original;
        DisabledSlot(Slot original) {
            super(original.container, original.getContainerSlot(), original.x, original.y);
            this.original = original;
            this.index = original.index;
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean isActive() { return false; }
    }

    public static void apply(Player p) {
        InventoryMenu menu = p.inventoryMenu;
        for (int i = 0; i < menu.slots.size(); i++) {
            if (!blockedSlot(i)) continue;
            Slot slot = menu.slots.get(i);
            if (!restricted(p)) {
                if (slot instanceof DisabledSlot disabled) menu.slots.set(i, disabled.original);
                continue;
            }
            if (p instanceof ServerPlayer && slot.hasItem()) {
                ItemStack saved = slot.getItem().copy();
                slot.set(ItemStack.EMPTY);
                // Le resultat est une previsualisation ; seuls les ingredients sont rendus.
                if (i != InventoryMenu.RESULT_SLOT && !p.getInventory().add(saved)) {
                    p.drop(saved, false, net.minecraft.util.Prediction.SERVER_ONLY);
                }
            }
            if (!(slot instanceof DisabledSlot)) menu.slots.set(i, new DisabledSlot(slot));
        }
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) { apply(event.getEntity()); }
    @SubscribeEvent public static void swap(LivingSwapItemsEvent.Hands event) {
        if (event.getEntity() instanceof Player p && restricted(p)) event.setCanceled(true);
    }
}
