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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Ecran de chargement d'un donjon instancie : fond sombre teinte de la couleur du donjon, nom, phrase d'ambiance, barre de
 * progression de la construction, conseils qui defilent et braises qui montent. Il recouvre tout, s'ouvre en fondu et se retire en
 * fondu quand le joueur est arrive ; une securite le ferme seul si la fin n'arrive jamais.
 */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class LoadingClient {
    public static final Identifier LAYER = MMORPG.id("loading");
    private static final float FADE_IN = 0.45F;
    private static final float FADE_OUT = 0.9F;
    private static final float TIMEOUT = 60F;
    private static final String[] TIPS = {
            "Toutes les épreuves se font en solo : un groupe ne fait que renforcer les vagues.",
            "Une chute dans un parcours de saut vous ramène simplement au départ.",
            "Les couleurs gravées dans le couloir d'entrée donnent l'ordre des dalles.",
            "Touchez les cibles avec une arme, un sort ou une flèche : tout compte.",
            "L'objet qui active l'autel apparaît quand la dernière épreuve est réussie.",
            "Chaque groupe a son propre donjon : personne d'autre n'y entrera.",
            "Le boss change de phase à 66 % et 33 % de vie : des renforts surgissent.",
            "Votre progression est gardée si vous revenez avant trois minutes.",
    };

    private static boolean active;
    private static boolean ending;
    private static boolean fading;
    private static long fadeNanos;
    private static String title = "";
    private static String lore = "";
    private static int color = 0xFFC868;
    private static float target;
    private static float shown;
    private static long startNanos;
    private static long endNanos;
    private static long lastNanos;

    private LoadingClient() {
    }

    public static boolean active() {
        return active;
    }

    public static void handle(Payloads.Loading p) {
        switch (p.mode()) {
            case Payloads.Loading.START -> {
                title = p.title().toUpperCase(java.util.Locale.ROOT);
                lore = p.lore();
                color = p.color() & 0xFFFFFF;
                target = p.percent();
                shown = Math.min(shown, target);
                if (!active) shown = 0F;
                active = true;
                ending = false;
                fading = false;
                startNanos = System.nanoTime();
                lastNanos = startNanos;
                Minecraft mc = Minecraft.getInstance();
                mc.gui.setScreen(null);
            }
            case Payloads.Loading.PROGRESS -> target = Math.max(target, p.percent());
            default -> {
                if (active && !ending) {
                    ending = true;
                    target = 100F;
                    endNanos = System.nanoTime();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        active = false;
    }

    /** Le « chargement du terrain » de Minecraft ne s'affiche pas par-dessus : notre ecran reste jusqu'a ce que le decor soit pret. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreen(ScreenEvent.Opening event) {
        if (active && event.getNewScreen() instanceof net.minecraft.client.gui.screens.LevelLoadingScreen) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLayer(RenderGuiLayerEvent.Pre event) {
        if (active && !event.getName().equals(LAYER)) event.setCanceled(true);
    }

    private static float seconds(long from) {
        return (System.nanoTime() - from) / 1.0E9F;
    }

    private static int alpha(float a) {
        return Mth.clamp((int) (a * 255F), 0, 255) << 24;
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!active) return;
        float t = seconds(startNanos);
        Minecraft mc = Minecraft.getInstance();
        // la fin demandee par le serveur attend que le decor autour du joueur soit rendu (au moins 0,6 s)
        if (ending && !fading && seconds(endNanos) > 0.6F && mc.levelRenderer.hasRenderedAllSections()) {
            fading = true;
            fadeNanos = System.nanoTime();
        }
        float out = fading ? seconds(fadeNanos) : 0F;
        if (out > FADE_OUT || t > TIMEOUT) {
            active = false;
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        float k = Ease.OUT_CUBIC.apply(t / FADE_IN) * (1F - Ease.IN_OUT_CUBIC.apply(out / FADE_OUT));
        long now = System.nanoTime();
        float dt = Math.min(0.1F, (now - lastNanos) / 1.0E9F);
        lastNanos = now;
        shown += (target - shown) * (1F - (float) Math.exp(-dt * 4F));
        if (ending) shown = Math.min(100F, shown + dt * 80F);
        // fond : nuit teintee, degrade qui s'eclaire vers le bas
        g.fill(0, 0, w, h, alpha(k) | 0x05060A);
        g.fillGradient(0, h / 2, w, h, alpha(0F), alpha(0.22F * k) | color);
        g.fillGradient(0, 0, w, h / 3, alpha(0.5F * k) | 0x000000, alpha(0F));
        // braises qui montent
        for (int i = 0; i < 36; i++) {
            float seed = i * 12.9898F;
            float speed = 8F + (i % 7) * 4F;
            float y = h - ((t * speed + (Mth.frac(seed * 0.37F) * h)) % h);
            float x = Mth.frac(seed * 0.618F) * w + Mth.sin(t * 0.8F + i) * 10F;
            int sz = 1 + (i % 3 == 0 ? 1 : 0);
            float flick = 0.4F + 0.6F * Mth.sin(t * 3F + i * 1.7F) * Mth.sin(t * 3F + i * 1.7F);
            g.fill(Math.round(x), Math.round(y), Math.round(x) + sz, Math.round(y) + sz, alpha(0.55F * k * flick) | color);
        }
        int cx = w / 2;
        int cy = Math.round(h * 0.34F);
        // titre et filet
        var comp = AeloriaFont.of(title);
        int tw = mc.font.width(comp);
        float scale = Math.min(3.4F, w * 0.82F / Math.max(1, tw));
        g.pose().pushMatrix();
        g.pose().translate(cx, cy + (1F - Ease.OUT_CUBIC.apply(t / 0.9F)) * -14F);
        g.pose().scale(scale, scale);
        g.text(mc.font, comp, -tw / 2, -4, color | alpha(k), true);
        g.pose().popMatrix();
        int half = Math.round(Math.min(w * 0.3F, 180F) * Ease.OUT_CUBIC.apply((t - 0.2F) / 0.9F));
        g.fill(cx - half, cy + 20, cx + half, cy + 21, color | alpha(0.85F * k));
        diamond(g, cx - half, cy + 20, 3, color | alpha(k));
        diamond(g, cx + half, cy + 20, 3, color | alpha(k));
        // phrase d'ambiance
        if (!lore.isEmpty()) {
            float la = Ease.OUT_CUBIC.apply((t - 0.5F) / 0.8F) * k;
            int lw = mc.font.width(lore);
            float ls = Math.min(1.3F, w * 0.84F / Math.max(1, lw));
            g.pose().pushMatrix();
            g.pose().translate(cx, cy + 32);
            g.pose().scale(ls, ls);
            g.text(mc.font, lore, -lw / 2, 0, 0xE6DCC8 | alpha(la), true);
            g.pose().popMatrix();
        }
        // barre de progression
        int bw = Math.min(w - 60, 260);
        int bx = cx - bw / 2;
        int by = Math.round(h * 0.72F);
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 7, alpha(0.7F * k) | 0x000000);
        g.fill(bx, by, bx + bw, by + 6, alpha(0.9F * k) | 0x14161C);
        int fill = Math.round(bw * Mth.clamp(shown / 100F, 0F, 1F));
        g.fill(bx, by, bx + fill, by + 6, alpha(k) | color);
        g.fillGradient(bx, by, bx + fill, by + 3, alpha(0.35F * k) | 0xFFFFFF, alpha(0F));
        String pct = Math.round(shown) + " %";
        g.text(mc.font, pct, cx - mc.font.width(pct) / 2, by + 10, 0xC8C0B0 | alpha(k), true);
        String label = shown >= 99.5F ? "Entrée dans le donjon…" : "Construction de votre donjon…";
        g.text(mc.font, label, cx - mc.font.width(label) / 2, by - 12, 0xA8A090 | alpha(k), true);
        // sablier : quatre losanges qui tournent
        int sx = cx;
        int sy = by + 32;
        for (int i = 0; i < 4; i++) {
            float a = t * 3.2F + i * Mth.HALF_PI;
            int ox = Math.round(Mth.cos(a) * 9F);
            int oy = Math.round(Mth.sin(a) * 9F);
            diamond(g, sx + ox, sy + oy, i == (int) (t * 4F) % 4 ? 3 : 2, color | alpha((i == (int) (t * 4F) % 4 ? 1F : 0.45F) * k));
        }
        // conseil
        String tip = "Astuce : " + TIPS[(int) (t / 4.5F) % TIPS.length].replaceFirst("^Astuce : ", "");
        float tipFade = Mth.clamp(Math.min(((t % 4.5F) / 0.4F), (4.5F - (t % 4.5F)) / 0.4F), 0F, 1F);
        int tipw = mc.font.width(tip);
        float tips = Math.min(1F, w * 0.9F / Math.max(1, tipw));
        g.pose().pushMatrix();
        g.pose().translate(cx, h - 26);
        g.pose().scale(tips, tips);
        g.text(mc.font, tip, -tipw / 2, 0, 0xA09C92 | alpha(tipFade * k), true);
        g.pose().popMatrix();
    }

    private static void diamond(GuiGraphicsExtractor g, int x, int y, int r, int col) {
        for (int i = -r; i <= r; i++) {
            int span = r - Math.abs(i);
            g.fill(x - span, y + i, x + span + 1, y + i + 1, col);
        }
    }
}
