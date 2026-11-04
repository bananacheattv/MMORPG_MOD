package com.mmorpg.client;

import com.mmorpg.MMORPG;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Zoom x4 (maintenir la touche) avec sensibilite souris reduite. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class ZoomClient {
    private static final double FACTOR = 4.0;
    private static boolean zooming;
    /** Force le zoom (tests uniquement). */
    public static boolean forced;
    private static double savedSensitivity;

    private ZoomClient() {
    }

    public static boolean isZooming() {
        return zooming;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean want = mc.player != null && mc.level != null && mc.gui.screen() == null && (Keys.ZOOM.isDown() || forced);
        if (want && !zooming) {
            zooming = true;
            savedSensitivity = mc.options.sensitivity().get();
            mc.options.sensitivity().set(savedSensitivity / FACTOR);
        } else if (!want && zooming) {
            zooming = false;
            mc.options.sensitivity().set(savedSensitivity);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (!zooming) return;
        double tan = Math.tan(Math.toRadians(event.getFOV()) / 2.0) / FACTOR;
        event.setFOV((float) (Math.toDegrees(Math.atan(tan)) * 2.0));
    }
}
