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
        } else if (event.getNewScreen() instanceof net.minecraft.client.gui.screens.DisconnectedScreen d) {
            // deconnexion (kick, coupure, arret, timeout) : retour au menu Aeloria avec la raison
            event.setNewScreen(new AeloriaTitleScreen(reason(d)));
        } else if (event.getNewScreen() instanceof net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
                || event.getNewScreen() instanceof net.minecraft.client.gui.screens.multiplayer.SafetyScreen) {
            // la liste vanilla des serveurs ne s'affiche jamais
            event.setNewScreen(new AeloriaTitleScreen());
        }
    }

    private static net.minecraft.network.chat.Component reason(net.minecraft.client.gui.screens.DisconnectedScreen screen) {
        try {
            var f = net.minecraft.client.gui.screens.DisconnectedScreen.class.getDeclaredField("details");
            f.setAccessible(true);
            return ((net.minecraft.network.DisconnectionDetails) f.get(screen)).reason();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return net.minecraft.network.chat.Component.literal("Connexion perdue.");
        }
    }
}
