package com.mmorpg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

/** Dev : crée un monde solo neuf depuis le menu titre (-Dmmorpg.freshWorld=true), pour les captures sans sauvegarde. */
final class DevFreshWorld {
    private static final boolean ENABLED = Boolean.getBoolean("mmorpg.freshWorld");
    private static int wait;
    private static boolean done;

    private DevFreshWorld() {
    }

    static void tick(Minecraft mc) {
        if (!ENABLED || done || mc.level != null) return;
        if ((mc.gui.screen() instanceof TitleScreen || mc.gui.screen() instanceof com.mmorpg.client.screen.AeloriaTitleScreen) && ++wait == 60) CreateWorldScreen.openFresh(mc, () -> mc.gui.setScreen(new TitleScreen()));
        if (mc.gui.screen() instanceof CreateWorldScreen s && ++wait > 160) {
            try {
                var m = CreateWorldScreen.class.getDeclaredMethod("onCreate");
                m.setAccessible(true);
                m.invoke(s);
                done = true;
            } catch (Throwable t) {
                com.mmorpg.MMORPG.LOGGER.error("[MMORPG] freshWorld : {}", t.toString());
                done = true;
            }
        }
    }
}
