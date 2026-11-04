package com.mmorpg.client;

import com.mmorpg.MMORPG;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Mode photo : compte à rebours, interface masquée, caméra de face, capture puis retour à l'état précédent. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class PhotoMode {
    private static int ticks = -1;
    private static boolean hadGui;
    private static CameraType camera = CameraType.FIRST_PERSON;
    private PhotoMode() {}

    public static void start(int seconds) {
        var mc = Minecraft.getInstance();
        if (ticks >= 0) return;
        mc.gui.setScreen(null);
        hadGui = mc.gui.hud.isHidden();
        camera = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        ticks = Math.max(2, seconds) * 20;
    }

    public static boolean active() { return ticks >= 0; }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (ticks < 0) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) { ticks = -1; return; }
        if (ticks > 40 && ticks % 20 == 0) mc.gui.hud.setOverlayMessage(Component.literal("Photo dans " + ticks / 20 + " s · souriez !"), false);
        if (ticks == 20 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (ticks == 0) {
            Screenshot.grab(mc.gameDirectory, "photo_" + System.currentTimeMillis() + ".png", mc.gameRenderer.mainRenderTarget(), 1,
                msg -> mc.execute(() -> { if (mc.player != null) mc.player.sendSystemMessage(Component.literal("Photo enregistrée dans le dossier screenshots.")); }));
        }
        if (--ticks < -5) { ticks = -1; if (mc.gui.hud.isHidden() != hadGui) mc.gui.hud.toggle(); mc.options.setCameraType(camera); }
    }
}
