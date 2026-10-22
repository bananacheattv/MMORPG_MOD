package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.cinematic.Ease;
import com.mmorpg.network.Payloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Cinematique d'apparition d'un boss invoque a l'autel d'un donjon : la camera tourne autour de l'autel pendant le rituel
 * (cristaux, feu d'ames), puis s'approche quand le boss surgit et leve les yeux vers lui, avant de rendre la main au joueur.
 * Pilotee a chaque image avec le temps reel ; le HUD est masque, les bandes de cinema encadrent la scene, ESPACE la saute.
 */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class BossRevealClient {
    public static final Identifier LAYER = MMORPG.id("boss_reveal");
    /** Instant (secondes) ou le boss surgit : le rituel de l'autel dure 3 s. */
    private static final float RITUAL = 3.0F;
    private static final float OUTRO = 0.7F;

    private static boolean active;
    private static boolean skipping;
    private static ArmorStand camera;
    private static Vec3 center = Vec3.ZERO;
    private static float azimuth0;
    private static float duration;
    private static long startNanos;
    private static long skipNanos;
    private static float fov = 70F;
    private static float userFov = 70F;

    private BossRevealClient() {
    }

    public static boolean active() {
        return active;
    }

    public static void start(Payloads.BossReveal p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || active) return;
        center = new Vec3(p.x() + 0.5, p.y() + 1.0, p.z() + 0.5);
        duration = p.ticks() / 20F;
        azimuth0 = (float) Math.atan2(mc.player.getZ() - center.z, mc.player.getX() - center.x);
        camera = new ArmorStand(mc.level, center.x, center.y, center.z);
        camera.setInvisible(true);
        camera.setNoGravity(true);
        userFov = mc.options.fov().get();
        fov = userFov;
        startNanos = System.nanoTime();
        skipping = false;
        active = true;
        mc.setCameraEntity(camera);
        mc.gui.setScreen(null);
    }

    private static void end() {
        Minecraft mc = Minecraft.getInstance();
        if (active && mc.player != null) mc.setCameraEntity(mc.player);
        active = false;
        camera = null;
    }

    private static float seconds() {
        return (System.nanoTime() - startNanos) / 1.0E9F;
    }

    // ================================================================== camera

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || camera == null) {
            end();
            return;
        }
        float t = seconds();
        if (skipping && (System.nanoTime() - skipNanos) / 1.0E9F > 0.35F) {
            end();
            return;
        }
        if (t >= duration) {
            end();
            return;
        }
        float a = Mth.clamp(t / RITUAL, 0F, 1F);
        float b = Mth.clamp((t - RITUAL) / Math.max(0.5F, duration - RITUAL - OUTRO), 0F, 1F);
        // rituel : large plan qui tourne ; apparition : la camera s'approche et leve les yeux
        double radius = t < RITUAL ? Mth.lerp(Ease.IN_OUT_CUBIC.apply(a), 13.5F, 9.5F) : Mth.lerp(Ease.IN_OUT_CUBIC.apply(b), 9.5F, 6.8F);
        double height = t < RITUAL ? Mth.lerp(Ease.IN_OUT_CUBIC.apply(a), 5.2F, 3.4F) : Mth.lerp(Ease.IN_OUT_CUBIC.apply(b), 3.4F, 1.6F);
        double az = azimuth0 + Math.toRadians(95) * Ease.IN_OUT_CUBIC.apply(Mth.clamp(t / (duration - OUTRO), 0F, 1F));
        double shake = t > RITUAL && t < RITUAL + 0.9F ? (1F - (t - RITUAL) / 0.9F) * 0.18F : 0;
        Vec3 target = center.add(0, Mth.lerp(Ease.IN_OUT_CUBIC.apply(b), 0.6F, 2.6F), 0);
        Vec3 pos = center.add(Math.cos(az) * radius + Math.sin(t * 70F) * shake, height + Math.sin(t * 53F) * shake, Math.sin(az) * radius + Math.cos(t * 61F) * shake);
        // la camera ne traverse jamais un mur : elle s'arrete devant
        var hit = mc.level.clip(new ClipContext(target, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, camera));
        if (hit.getType() != HitResult.Type.MISS) pos = hit.getLocation().subtract(pos.subtract(target).normalize().scale(0.4));
        // fin : on rejoint les yeux du joueur en douceur
        float o = t > duration - OUTRO ? Ease.IN_OUT_CUBIC.apply((t - (duration - OUTRO)) / OUTRO) : 0F;
        Vec3 eye = mc.player.getEyePosition();
        pos = pos.lerp(eye, o);
        Vec3 look = target.lerp(eye.add(mc.player.getLookAngle().scale(4)), o);
        Vec3 d = look.subtract(pos);
        float yaw = (float) (Math.toDegrees(Math.atan2(-d.x, d.z)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        double y = pos.y - camera.getEyeHeight();
        camera.setPos(pos.x, y, pos.z);
        camera.xo = camera.xOld = pos.x;
        camera.yo = camera.yOld = y;
        camera.zo = camera.zOld = pos.z;
        camera.setYRot(yaw);
        camera.yRotO = yaw;
        camera.setXRot(pitch);
        camera.xRotO = pitch;
        camera.setYHeadRot(yaw);
        camera.yHeadRotO = yaw;
        fov = userFov * (t < RITUAL ? Mth.lerp(a, 1F, 0.92F) : Mth.lerp(Ease.IN_OUT_CUBIC.apply(b), 0.92F, 0.76F)) * (1F - o) + userFov * o;
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (active) event.setFOV(fov);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLayer(RenderGuiLayerEvent.Pre event) {
        if (active && !event.getName().equals(LAYER) && !event.getName().equals(AreaEntryClient.LAYER)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (active && !skipping && event.getAction() == 1 && Keys.SKIP.matches(event.getKeyEvent())) {
            skipping = true;
            skipNanos = System.nanoTime();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        active = false;
        camera = null;
    }

    // ================================================================== bandes de cinema

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        if (!active) return;
        float t = seconds();
        int w = g.guiWidth();
        int h = g.guiHeight();
        float in = Ease.OUT_CUBIC.apply(t / 0.6F);
        float out = 1F - Ease.IN_OUT_CUBIC.apply((t - (duration - OUTRO)) / OUTRO);
        int bar = Math.round(h * 0.115F * in * out);
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        // montee du danger : vignette rouge qui s'epaissit jusqu'a l'apparition
        float danger = Mth.clamp(t / RITUAL, 0F, 1F) * (t < RITUAL ? 1F : Math.max(0F, 1F - (t - RITUAL) / 1.2F));
        int edge = Math.round(Math.min(w, h) * 0.22F);
        int hot = (Mth.clamp((int) (danger * 0.38F * 255F), 0, 255) << 24) | 0x600000;
        g.fillGradient(0, 0, w, edge, hot, 0);
        g.fillGradient(0, h - edge, w, h, 0, hot);
        g.fillGradient(0, 0, edge, h, hot, 0);
        g.fillGradient(w - edge, 0, w, h, 0, hot);
        float flash = t > RITUAL && t < RITUAL + 0.4F ? 1F - (t - RITUAL) / 0.4F : 0F;
        if (flash > 0.01F) g.fill(0, 0, w, h, (Mth.clamp((int) (flash * 0.55F * 255F), 0, 255) << 24) | 0xFFE8D0);
        if (!skipping && t > 0.8F && t < duration - OUTRO) {
            String hint = "[" + Keys.SKIP.getTranslatedKeyMessage().getString() + "] Passer";
            g.text(Minecraft.getInstance().font, hint, w - 8 - Minecraft.getInstance().font.width(hint), h - bar + 6, 0x88FFFFFF, true);
        }
    }
}
