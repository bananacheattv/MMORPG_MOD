package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.screen.RpgDeathScreen;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.screens.DeathScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Ecran de mort personnalise : le serveur envoie la cause et l'experience perdue juste avant que Minecraft ouvre son ecran de mort,
 * qu'on remplace par {@link RpgDeathScreen}.
 */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class DeathClient {
    private static Payloads.DeathInfo last = new Payloads.DeathInfo(0, 1, 0, 0, "");
    private static long receivedAt;

    private DeathClient() {
    }

    public static void info(Payloads.DeathInfo p) {
        last = p;
        receivedAt = System.nanoTime();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreen(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof DeathScreen && !(event.getCurrentScreen() instanceof RpgDeathScreen)) {
            // les informations datent de cette mort si elles viennent d'arriver ; sinon (mort sans classe, ancienne version) on n'affiche rien de plus
            boolean fresh = System.nanoTime() - receivedAt < 15_000_000_000L;
            event.setNewScreen(new RpgDeathScreen(fresh ? last : new Payloads.DeathInfo(0, 1, 0, 0, "")));
        }
    }
}
