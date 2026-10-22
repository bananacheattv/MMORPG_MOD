package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.cinematic.Ease;
import com.mmorpg.network.Payloads;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Animation d'entree dans une zone (style 0 : filet dore qui s'ouvre, titre qui se pose, sous-titre) ou dans une salle de boss
 * (style 1 : bandes de cinema, flash, vignette rouge qui pulse, titre qui s'abat avec tremblement d'ecran).
 * Tout est calcule avec le temps reel : la fluidite ne depend pas du nombre d'images par seconde.
 */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class AreaEntryClient {
    public static final Identifier LAYER = MMORPG.id("area_entry");
    private static final float ZONE_SECONDS = 4.4F;
    private static final float BOSS_SECONDS = 6.4F;
    private static final float SHAKE_SECONDS = 1.6F;

    private static String title = "";
    private static String subtitle = "";
    private static int style;
    private static int color = 0xFFD060;
    private static long startNanos;
    private static boolean active;

    private AreaEntryClient() {
    }

    public static void start(Payloads.AreaEntry p) {
        title = p.title().toUpperCase(java.util.Locale.ROOT);
        subtitle = p.subtitle();
        style = p.style();
        color = p.color() & 0xFFFFFF;
        startNanos = System.nanoTime();
        active = true;
    }

    /** Interrompt l'animation en cours (outils de verification). */
    public static void cancel() {
        active = false;
    }

    private static float seconds() {
        return (System.nanoTime() - startNanos) / 1.0E9F;
    }

    private static float total() {
        return style == 1 ? BOSS_SECONDS : ZONE_SECONDS;
    }

    // ================================================================== tremblement (salle de boss)

    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active || style != 1) return;
        float t = seconds();
        if (t > SHAKE_SECONDS) return;
        float amp = (1F - Ease.OUT_CUBIC.apply(t / SHAKE_SECONDS)) * 1.6F;
        float time = t * 38F;
        event.setPitch(event.getPitch() + Mth.sin(time) * amp * 0.55F);
        event.setYaw(event.getYaw() + Mth.sin(time * 0.83F + 1.7F) * amp * 0.45F);
        event.setRoll(event.getRoll() + Mth.sin(time * 1.21F + 0.4F) * amp * 0.6F);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        active = false;
    }

    // ================================================================== affichage

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!active) return;
        float t = seconds();
        if (t >= total()) {
            active = false;
            return;
        }
        if (style == 1) drawBoss(g, t);
        else drawZone(g, t);
    }

    private static int alpha(float a) {
        return Mth.clamp((int) (a * 255F), 0, 255) << 24;
    }

    private static float window(float t, float in0, float in1, float out0, float out1) {
        float in = Ease.OUT_CUBIC.apply((t - in0) / Math.max(0.001F, in1 - in0));
        float out = 1F - Ease.IN_OUT_CUBIC.apply((t - out0) / Math.max(0.001F, out1 - out0));
        return Math.min(in, out);
    }

    private static void drawZone(GuiGraphicsExtractor g, float t) {
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;
        int cy = Math.round(h * 0.25F);
        float out = ZONE_SECONDS;
        float band = window(t, 0F, 0.5F, out - 0.9F, out);
        // voile sombre en bande : degrade vertical qui s'etire avec le filet
        int bh = Math.round(34F * band);
        g.fillGradient(0, cy - bh - 14, w, cy, alpha(0F), alpha(0.42F * band));
        g.fillGradient(0, cy, w, cy + bh + 26, alpha(0.42F * band), alpha(0F));
        // filet dore qui s'ouvre depuis le centre, losanges aux extremites
        float open = Ease.OUT_CUBIC.apply(t / 0.7F) * (1F - Ease.IN_OUT_CUBIC.apply((t - (out - 0.9F)) / 0.9F));
        int half = Math.round(w * 0.2F * open);
        int col = color | alpha(0.9F * band);
        g.fill(cx - half, cy - 1, cx + half, cy, col);
        int glow = color | alpha(0.25F * band);
        g.fill(cx - half, cy - 2, cx + half, cy - 1, glow);
        g.fill(cx - half, cy, cx + half, cy + 1, glow);
        if (open > 0.05F) {
            diamond(g, cx - half, cy, 3, col);
            diamond(g, cx + half, cy, 3, col);
        }
        // titre : se pose de haut en bas en rapetissant legerement
        float a = window(t, 0.25F, 1.05F, out - 0.9F, out);
        if (a > 0.01F) {
            float settle = Ease.OUT_CUBIC.apply((t - 0.25F) / 0.9F);
            var text = EldoriaFont.of(title);
            int tw = mc.font.width(text);
            float fit = Math.min(1F, w * 0.84F / Math.max(1, tw * 2.3F));
            float scale = (2.3F + (1F - settle) * 0.45F) * fit;
            int ty = cy - 8 - Math.round((1F - settle) * 8F) - Math.round(9F * scale) / 2 - 4;
            g.pose().pushMatrix();
            g.pose().translate(cx, ty);
            g.pose().scale(scale, scale);
            g.text(mc.font, text, -tw / 2, 0, color | alpha(a), true);
            g.pose().popMatrix();
        }
        float sa = window(t, 0.7F, 1.4F, out - 0.9F, out);
        if (sa > 0.01F && !subtitle.isEmpty()) {
            g.pose().pushMatrix();
            g.pose().translate(cx, cy + 8 + (1F - sa) * 4F);
            g.pose().scale(1.15F, 1.15F);
            g.centeredText(mc.font, net.minecraft.network.chat.Component.literal(subtitle), 0, 0, 0xE8E0D0 | alpha(sa));
            g.pose().popMatrix();
        }
    }

    private static void drawBoss(GuiGraphicsExtractor g, float t) {
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;
        float out = BOSS_SECONDS;
        float k = 1F - Ease.IN_OUT_CUBIC.apply((t - (out - 1F)) / 1F);
        // bandes de cinema
        int bar = Math.round(h * 0.11F * Ease.OUT_CUBIC.apply(t / 0.6F) * k);
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        // vignette rouge qui pulse (quatre degrades sur les bords)
        float pulse = 0.55F + 0.45F * Mth.sin(t * 5.2F);
        float va = (0.22F + 0.18F * pulse) * Ease.OUT_CUBIC.apply(t / 0.8F) * k;
        int edge = Math.round(Math.min(w, h) * 0.22F);
        int hot = alpha(va) | 0x700000;
        g.fillGradient(0, 0, w, edge, hot, alpha(0F));
        g.fillGradient(0, h - edge, w, h, alpha(0F), hot);
        g.fillGradient(0, 0, edge, h, hot, alpha(0F));
        g.fillGradient(w - edge, 0, w, h, alpha(0F), hot);
        // eclair a l'arrivee
        float flash = 1F - Mth.clamp(t / 0.45F, 0F, 1F);
        if (flash > 0.01F) g.fill(0, 0, w, h, 0xFFE0D0 & 0xFFFFFF | alpha(flash * 0.38F));
        // titre : tombe du zoom avec tremblement qui s'eteint
        float a = window(t, 0.3F, 0.9F, out - 1F, out);
        if (a > 0.01F) {
            float settle = Ease.OUT_CUBIC.apply((t - 0.3F) / 0.8F);
            var text = EldoriaFont.of(title);
            int tw = mc.font.width(text);
            float fit = Math.min(1F, w * 0.84F / Math.max(1, tw * 3.4F));
            float scale = (3.4F + (1F - settle) * 1.8F) * fit;
            float shake = Math.max(0F, 1F - t / SHAKE_SECONDS) * 5F;
            float sx = Mth.sin(t * 61F) * shake;
            float sy = Mth.sin(t * 47F + 1F) * shake;
            int cy = Math.round(h * 0.42F);
            g.pose().pushMatrix();
            g.pose().translate(cx + sx, cy + sy);
            g.pose().scale(scale, scale);
            g.text(mc.font, text, -tw / 2, -4, color | alpha(a), true);
            g.pose().popMatrix();
            // surtitre et filets
            float ka = window(t, 0.6F, 1.2F, out - 1F, out);
            int half = Math.round(Math.min(w * 0.3F, mc.font.width(text) * scale * 0.5F + 40F) * Ease.OUT_CUBIC.apply((t - 0.5F) / 0.9F));
            int ly = cy + Math.round(9F * scale / 2F) + 6;
            g.fill(cx - half, ly, cx + half, ly + 1, color | alpha(0.8F * ka));
            g.pose().pushMatrix();
            g.pose().translate(cx, cy - Math.round(9F * scale / 2F) - 16);
            g.pose().scale(1.3F, 1.3F);
            g.centeredText(mc.font, EldoriaFont.of("— ANTRE DU BOSS —"), 0, 0, 0xFF5040 | alpha(ka));
            g.pose().popMatrix();
            float sa = window(t, 1.0F, 1.7F, out - 1F, out);
            if (!subtitle.isEmpty()) {
                g.pose().pushMatrix();
                g.pose().translate(cx, ly + 10);
                g.pose().scale(1.35F, 1.35F);
                g.centeredText(mc.font, net.minecraft.network.chat.Component.literal(subtitle), 0, 0, 0xF0E4D0 | alpha(sa));
                g.pose().popMatrix();
            }
        }
    }

    private static void diamond(GuiGraphicsExtractor g, int x, int y, int r, int col) {
        for (int i = -r; i <= r; i++) {
            int span = r - Math.abs(i);
            g.fill(x - span, y + i, x + span + 1, y + i + 1, col);
        }
    }
}
