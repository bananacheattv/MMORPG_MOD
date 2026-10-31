package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.network.Payloads;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Signale au serveur chaque interface du mod ouverte par le joueur : le tutoriel de la Mascotte s'en sert pour valider la decouverte. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class TutorialScreens {
    private TutorialScreens() {
    }

    @SubscribeEvent
    public static void opened(ScreenEvent.Opening event) {
        var screen = event.getNewScreen();
        if (screen == null || Minecraft.getInstance().player == null || !screen.getClass().getName().startsWith("com.mmorpg.client.screen.")) return;
        CompoundTag t = new CompoundTag();
        t.putString("screen", screen.getClass().getSimpleName());
        ClientNet.send(new Payloads.SocialAction("tuto", t));
    }
}
