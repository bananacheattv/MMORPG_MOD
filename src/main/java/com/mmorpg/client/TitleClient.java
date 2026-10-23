package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.screen.AeloriaTitleScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Remplace le menu principal de Minecraft par celui d'Aeloria. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class TitleClient {
    private TitleClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreen(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen && !(event.getNewScreen() instanceof AeloriaTitleScreen)) {
            event.setNewScreen(new AeloriaTitleScreen());
        }
    }
}
