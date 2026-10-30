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
        if(event.getNewScreen()==null)return;
        // titre traduisible envoye par le serveur (un coffre renomme « Sacs à dos » a l'enclume a un titre litteral) et 6 rangees
        if(event.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen chest&&chest.getMenu().getRowCount()==6&&chest.getTitle().getContents().equals(com.mmorpg.server.Backpacks.TITLE.getContents())&&mc.player!=null){event.setNewScreen(new com.mmorpg.client.screen.BackpackScreen(chest.getMenu(),mc.player.getInventory(),chest.getTitle()));return;}

        if ((event.getNewScreen() instanceof InventoryScreen || event.getNewScreen().getClass()==net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class) && mc.player != null) {
            com.mmorpg.client.screen.MenuScreen.lastTab = com.mmorpg.client.screen.MenuScreen.Tab.PERSONNAGE;
            event.setNewScreen(new CharacterScreen(mc.player));
        }
    }
}
