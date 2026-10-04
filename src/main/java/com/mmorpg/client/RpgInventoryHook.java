package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.screen.CharacterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Desactive l'inventaire de Minecraft : la touche E ouvre l'onglet Personnage (inventaire, equipement, attributs). */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class RpgInventoryHook {
    private RpgInventoryHook() {
    }

    @SubscribeEvent
    public static void onOpen(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getNewScreen() instanceof InventoryScreen && mc.player != null && !mc.player.hasInfiniteMaterials()) {
            com.mmorpg.client.screen.MenuScreen.lastTab = com.mmorpg.client.screen.MenuScreen.Tab.PERSONNAGE;
            event.setNewScreen(new CharacterScreen(mc.player));
        }
    }
}
