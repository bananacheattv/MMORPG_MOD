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
    private static String lore = "";
    private static int style;
    private static int color = 0xFFD060;
    private static long startNanos;
    private static boolean active;

    private AreaEntryClient() {
    }

    public static void start(Payloads.AreaEntry p) {
        title = p.title().toUpperCase(java.util.Locale.ROOT);
        subtitle = p.subtitle();
        lore = p.lore();
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
        return switch (style) {
            case Payloads.AreaEntry.BOSS_ROOM -> BOSS_SECONDS;
            case Payloads.AreaEntry.WAVE -> 3.4F;
            case Payloads.AreaEntry.PHASE -> 4.4F;
            case Payloads.AreaEntry.VICTORY -> 7.8F;
            case Payloads.AreaEntry.DUNGEON -> 7.2F;
            default -> ZONE_SECONDS;
        };
    }

    // ================================================================== tremblement (salle de boss)

    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active || (style != Payloads.AreaEntry.BOSS_ROOM && style != Payloads.AreaEntry.PHASE && style != Payloads.AreaEntry.WAVE)) return;
        float t = seconds();
        float length = style == Payloads.AreaEntry.BOSS_ROOM ? SHAKE_SECONDS : style == Payloads.AreaEntry.PHASE ? 1.2F : 0.5F;
        if (t > length) return;
        float amp = (1F - Ease.OUT_CUBIC.apply(t / length)) * (style == Payloads.AreaEntry.BOSS_ROOM ? 1.6F : style == Payloads.AreaEntry.PHASE ? 2.4F : 0.7F);
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
        switch (style) {
            case Payloads.AreaEntry.BOSS_ROOM -> drawBoss(g, t);
            case Payloads.AreaEntry.WAVE -> drawWave(g, t);
            case Payloads.AreaEntry.PHASE -> drawPhase(g, t);
            case Payloads.AreaEntry.VICTORY -> drawVictory(g, t);
            case Payloads.AreaEntry.DUNGEON -> drawDungeon(g, t);
            default -> drawZone(g, t);
        }
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
            var text = AeloriaFont.of(title);
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
            var text = AeloriaFont.of(title);
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
            g.centeredText(mc.font, AeloriaFont.of("— ANTRE DU BOSS —"), 0, 0, 0xFF5040 | alpha(ka));
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

    /** Titre centre qui s'ajuste a la largeur de l'ecran. */
    private static void bigTitle(GuiGraphicsExtractor g, String text, int cx, int cy, float scale, int col, float a) {
        Minecraft mc = Minecraft.getInstance();
        var comp = AeloriaFont.of(text);
        int tw = mc.font.width(comp);
        float fit = Math.min(1F, g.guiWidth() * 0.86F / Math.max(1, tw * scale));
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(scale * fit, scale * fit);
        g.text(mc.font, comp, -tw / 2, -4, col | alpha(a), true);
        g.pose().popMatrix();
    }

    private static void smallLine(GuiGraphicsExtractor g, String text, int cx, int y, float scale, int col, float a) {
        if (text.isEmpty() || a <= 0.01F) return;
        Minecraft mc = Minecraft.getInstance();
        int tw = mc.font.width(text);
        float fit = Math.min(1F, g.guiWidth() * 0.86F / Math.max(1, tw * scale));
        g.pose().pushMatrix();
        g.pose().translate(cx, y);
        g.pose().scale(scale * fit, scale * fit);
        g.text(mc.font, text, -tw / 2, 0, col | alpha(a), true);
        g.pose().popMatrix();
    }

    private static void vignette(GuiGraphicsExtractor g, int rgb, float a) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int edge = Math.round(Math.min(w, h) * 0.24F);
        int hot = alpha(a) | rgb;
        g.fillGradient(0, 0, w, edge, hot, alpha(0F));
        g.fillGradient(0, h - edge, w, h, alpha(0F), hot);
        g.fillGradient(0, 0, edge, h, hot, alpha(0F));
        g.fillGradient(w - edge, 0, w, h, alpha(0F), hot);
    }

    // ------------------------------------------------------------------ vague : le numero s'abat au centre

    private static void drawWave(GuiGraphicsExtractor g, float t) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;
        int cy = Math.round(h * 0.3F);
        float out = total();
        boolean finale = title.contains("FINALE");
        float k = window(t, 0F, 0.3F, out - 0.7F, out);
        int bh = Math.round(30F * k);
        g.fillGradient(0, cy - bh - 22, w, cy, alpha(0F), alpha(0.5F * k));
        g.fillGradient(0, cy, w, cy + bh + 34, alpha(0.5F * k), alpha(0F));
        // deux lames qui s'ecartent du centre
        int half = Math.round(w * (finale ? 0.34F : 0.26F) * Ease.OUT_CUBIC.apply(t / 0.5F) * (1F - Ease.IN_OUT_CUBIC.apply((t - (out - 0.7F)) / 0.7F)));
        int blade = color | alpha(0.9F * k);
        g.fill(cx - half, cy + 22, cx + half, cy + 23, blade);
        diamond(g, cx - half, cy + 22, 3, blade);
        diamond(g, cx + half, cy + 22, 3, blade);
        // le titre tombe de haut en bas avec un leger rebond
        float settle = Ease.OUT_CUBIC.apply(t / 0.35F);
        float shake = Math.max(0F, 1F - t / 0.45F) * 4F;
        float scale = (finale ? 3.8F : 3.2F) + (1F - settle) * 2.2F;
        float pulse = finale ? 0.85F + 0.15F * Mth.sin(t * 14F) : 1F;
        bigTitle(g, title, cx + Math.round(Mth.sin(t * 70F) * shake), cy - 6 + Math.round(Mth.sin(t * 53F) * shake), scale, color, k * pulse);
        smallLine(g, lore.isEmpty() ? "" : lore.replace("/", " / "), cx, cy + 28, 1.05F, 0xE8D8B8, window(t, 0.25F, 0.7F, out - 0.7F, out));
        smallLine(g, subtitle, cx, cy + 40, 1.2F, 0xF0E4D0, window(t, 0.4F, 0.9F, out - 0.7F, out));
    }

    // ------------------------------------------------------------------ phase de boss : chiffre romain, flash, vignette

    private static void drawPhase(GuiGraphicsExtractor g, float t) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;
        float out = total();
        float k = 1F - Ease.IN_OUT_CUBIC.apply((t - (out - 0.9F)) / 0.9F);
        int bar = Math.round(h * 0.09F * Ease.OUT_CUBIC.apply(t / 0.4F) * k);
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        float pulse = 0.5F + 0.5F * Mth.sin(t * 7F);
        vignette(g, color, (0.28F + 0.2F * pulse) * Ease.OUT_CUBIC.apply(t / 0.5F) * k);
        float flash = 1F - Mth.clamp(t / 0.35F, 0F, 1F);
        if (flash > 0.01F) g.fill(0, 0, w, h, 0xFFFFFF & 0xFFE0C0 | alpha(flash * 0.45F));
        String roman = switch (lore) {
            case "1" -> "I";
            case "2" -> "II";
            case "3" -> "III";
            default -> lore;
        };
        int cy = Math.round(h * 0.4F);
        float a = window(t, 0.1F, 0.5F, out - 0.9F, out);
        smallLine(g, title, cx, cy - 46, 1.7F, 0xF0D8C0, a);
        float settle = Ease.OUT_CUBIC.apply((t - 0.1F) / 0.5F);
        float shake = Math.max(0F, 1F - t / 0.9F) * 5F;
        bigTitle(g, "PHASE " + roman, cx + Math.round(Mth.sin(t * 61F) * shake), cy + Math.round(Mth.sin(t * 47F) * shake),
                4.4F + (1F - settle) * 2.4F, color, a);
        int half = Math.round(Math.min(w * 0.3F, 160F) * Ease.OUT_CUBIC.apply((t - 0.2F) / 0.7F));
        g.fill(cx - half, cy + 28, cx + half, cy + 29, color | alpha(0.85F * a));
        smallLine(g, subtitle, cx, cy + 38, 1.4F, 0xF8EEDC, window(t, 0.7F, 1.3F, out - 0.9F, out));
    }

    // ------------------------------------------------------------------ victoire : rayons d'or qui tournent

    private static void drawVictory(GuiGraphicsExtractor g, float t) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;
        int cy = Math.round(h * 0.4F);
        float out = total();
        float k = Ease.OUT_CUBIC.apply(t / 0.8F) * (1F - Ease.IN_OUT_CUBIC.apply((t - (out - 1.3F)) / 1.3F));
        int bar = Math.round(h * 0.1F * Ease.OUT_CUBIC.apply(t / 0.7F) * (1F - Ease.IN_OUT_CUBIC.apply((t - (out - 1.0F)) / 1.0F)));
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        vignette(g, 0x806000, 0.30F * k);
        g.fillGradient(0, 0, w, h, alpha(0.10F * k) | 0x201000, alpha(0.22F * k) | 0x402800);
        float flash = 1F - Mth.clamp(t / 0.6F, 0F, 1F);
        if (flash > 0.01F) g.fill(0, 0, w, h, 0xFFFFE8 | alpha(flash * 0.6F));
        // rayons : fins traits dores qui tournent lentement autour du titre
        int rays = 16;
        float len = Math.max(w, h) * 0.75F;
        for (int i = 0; i < rays; i++) {
            g.pose().pushMatrix();
            g.pose().translate(cx, cy);
            g.pose().rotate((float) (Math.PI * 2 * i / rays) + t * 0.18F);
            float wob = 0.55F + 0.45F * Mth.sin(t * 2.2F + i * 1.7F);
            g.fillGradient(40, -(i % 2 == 0 ? 3 : 1), Math.round(40 + len), (i % 2 == 0 ? 3 : 1), color | alpha(0.34F * k * wob), color | alpha(0F));
            g.pose().popMatrix();
        }
        float a = window(t, 0.2F, 0.9F, out - 1.3F, out);
        float settle = Ease.OUT_CUBIC.apply((t - 0.2F) / 0.7F);
        bigTitle(g, title, cx, cy + 2, 5.0F + (1F - settle) * 3F, 0xFFF4C0, a * 0.35F);
        bigTitle(g, title, cx, cy, 5.0F + (1F - settle) * 3F, color, a);
        int half = Math.round(Math.min(w * 0.34F, 190F) * Ease.OUT_CUBIC.apply((t - 0.6F) / 0.9F));
        int y = cy + 32;
        g.fill(cx - half, y, cx + half, y + 1, color | alpha(0.9F * a));
        diamond(g, cx - half, y, 3, color | alpha(a));
        diamond(g, cx + half, y, 3, color | alpha(a));
        smallLine(g, subtitle, cx, y + 10, 1.7F, 0xFFF0D0, window(t, 1.0F, 1.7F, out - 1.3F, out));
        smallLine(g, lore, cx, y + 28, 1.2F, 0xE8D8B0, window(t, 1.6F, 2.3F, out - 1.3F, out));
    }

    // ------------------------------------------------------------------ entree de donjon : le texte s'ecrit lettre a lettre

    private static void drawDungeon(GuiGraphicsExtractor g, float t) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;
        int cy = Math.round(h * 0.38F);
        float out = total();
        float k = 1F - Ease.IN_OUT_CUBIC.apply((t - (out - 1.2F)) / 1.2F);
        int bar = Math.round(h * 0.11F * Ease.OUT_CUBIC.apply(t / 0.8F) * k);
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        g.fillGradient(0, 0, w, h, alpha(0.30F * k), alpha(0.45F * k));
        vignette(g, 0x000000, 0.45F * k);
        float a = window(t, 0.3F, 1.3F, out - 1.2F, out);
        float settle = Ease.OUT_CUBIC.apply((t - 0.3F) / 1.0F);
        bigTitle(g, title, cx, cy + Math.round((1F - settle) * -10F), 3.4F + (1F - settle) * 0.6F, color, a);
        int half = Math.round(Math.min(w * 0.3F, 170F) * Ease.OUT_CUBIC.apply((t - 0.5F) / 1.0F));
        g.fill(cx - half, cy + 20, cx + half, cy + 21, color | alpha(0.85F * a));
        diamond(g, cx - half, cy + 20, 3, color | alpha(a));
        diamond(g, cx + half, cy + 20, 3, color | alpha(a));
        // lettres qui s'ecrivent
        if (!lore.isEmpty()) {
            int chars = Mth.clamp(Math.round((t - 1.4F) * 30F), 0, lore.length());
            smallLine(g, lore.substring(0, chars) + (chars < lore.length() && ((int) (t * 8) & 1) == 0 ? "_" : ""), cx, cy + 32, 1.25F, 0xE6DCC8,
                    window(t, 1.4F, 1.6F, out - 1.2F, out));
        }
        smallLine(g, subtitle, cx, cy + 52, 1.1F, 0xB8B0A0, window(t, 3.2F, 3.9F, out - 1.2F, out));
    }

    private static void diamond(GuiGraphicsExtractor g, int x, int y, int r, int col) {
        for (int i = -r; i <= r; i++) {
            int span = r - Math.abs(i);
            g.fill(x - span, y + i, x + span + 1, y + i + 1, col);
        }
    }
}
