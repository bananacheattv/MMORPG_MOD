package com.mmorpg.client.ui;

import com.mmorpg.MMORPG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Boite a outils de rendu des interfaces "style MMORPG" : panneaux ornes, barres, boutons, cadres. */
public final class Ui {
    public static final int GOLD = 0xFFE8C060;
    public static final int GOLD_LIGHT = 0xFFFFE6A0;
    public static final int GOLD_DARK = 0xFF7A5A22;
    public static final int TEXT = 0xFFEDE4D0;
    public static final int MUTED = 0xFF9C9282;
    public static final int RED = 0xFFFF5A5A;
    public static final int GREEN = 0xFF6AE06A;
    public static final int BLUE = 0xFF62A8FF;
    public static final int PANEL_TOP = 0xF0201828;
    public static final int PANEL_BOTTOM = 0xF0100C16;
    public static final int INSET = 0xC0080610;

    private Ui() {
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    public static int alpha(int color, float a) {
        int al = (int) (((color >>> 24) & 0xFF) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (color & 0xFFFFFF);
    }

    public static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    public static int darker(int color, float f) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * f);
        int g = (int) (((color >> 8) & 0xFF) * f);
        int b = (int) ((color & 0xFF) * f);
        return (a << 24) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
    }

    public static int lighter(int color, float f) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        r = (int) (r + (255 - r) * f);
        g = (int) (g + (255 - g) * f);
        b = (int) (b + (255 - b) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void border(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Losange decoratif. */
    public static void diamond(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int i = -r; i <= r; i++) {
            int half = r - Math.abs(i);
            g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, color);
        }
    }

    /** Grand panneau orne : degrade sombre, double cadre dore et ornements d'angle. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x60000000);
        g.fillGradient(x, y, x + w, y + h, PANEL_TOP, PANEL_BOTTOM);
        border(g, x, y, w, h, GOLD_DARK);
        border(g, x + 1, y + 1, w - 2, h - 2, 0xFF2A2018);
        border(g, x + 2, y + 2, w - 4, h - 4, 0x80C8A050);
        for (int[] c : new int[][]{{x, y}, {x + w - 1, y}, {x, y + h - 1}, {x + w - 1, y + h - 1}}) {
            diamond(g, c[0], c[1], 4, GOLD_DARK);
            diamond(g, c[0], c[1], 2, GOLD_LIGHT);
        }
    }

    /** Panneau secondaire encastre. */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, INSET);
        border(g, x, y, w, h, 0xFF3A2E22);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x30FFFFFF);
    }

    /** Cadre fin colore (cartes, emplacements). */
    public static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, boolean highlight) {
        g.fill(x, y, x + w, y + h, highlight ? 0xD0281E30 : 0xC0140F1A);
        border(g, x, y, w, h, highlight ? lighter(color, 0.3f) : darker(color, 0.7f));
        if (highlight) border(g, x - 1, y - 1, w + 2, h + 2, alpha(color, 0.45f));
    }

    /** Bandeau de titre avec ornements. */
    public static void title(GuiGraphicsExtractor g, int cx, int y, String text, int color) {
        Font f = font();
        int w = f.width(text);
        g.fill(cx - w / 2 - 34, y + 4, cx - w / 2 - 6, y + 5, GOLD_DARK);
        g.fill(cx + w / 2 + 6, y + 4, cx + w / 2 + 34, y + 5, GOLD_DARK);
        diamond(g, cx - w / 2 - 36, y + 4, 2, GOLD);
        diamond(g, cx + w / 2 + 36, y + 4, 2, GOLD);
        g.centeredText(f, Component.literal(text), cx, y, color);
    }

    public static void header(GuiGraphicsExtractor g, int x, int y, int w, String text) {
        g.text(font(), text, x, y, GOLD, true);
        g.fillGradient(x, y + 10, x + w, y + 11, 0xFFB08A40, 0x00B08A40);
    }

    /** Barre de progression avec reflet. */
    public static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int top, int bottom, String text) {
        frac = Math.max(0, Math.min(1, frac));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF0A0808);
        g.fill(x, y, x + w, y + h, 0xFF241C20);
        int fw = (int) (w * frac);
        if (fw > 0) {
            g.fillGradient(x, y, x + fw, y + h, top, bottom);
            g.fill(x, y, x + fw, y + Math.max(1, h / 3), 0x40FFFFFF);
        }
        border(g, x - 1, y - 1, w + 2, h + 2, 0xFF5A4630);
        if (text != null && h >= 7) {
            Font f = font();
            int tw = f.width(text);
            g.text(f, text, x + (w - tw) / 2, y + (h - 8) / 2 + (h >= 9 ? 1 : 0), 0xFFFFFFFF, true);
        }
    }

    /** Bouton personnalise ; renvoie vrai si survole. */
    public static boolean button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean enabled, int mx, int my, int accent) {
        boolean hover = enabled && in(mx, my, x, y, w, h);
        int top = enabled ? (hover ? lighter(accent, 0.25f) : accent) : 0xFF3A3436;
        int bottom = enabled ? darker(accent, hover ? 0.55f : 0.42f) : 0xFF242024;
        g.fillGradient(x, y, x + w, y + h, top, bottom);
        border(g, x, y, w, h, enabled ? (hover ? GOLD_LIGHT : GOLD_DARK) : 0xFF4A4448);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x50FFFFFF);
        Font f = font();
        int color = enabled ? (hover ? 0xFFFFFFFF : TEXT) : 0xFF807878;
        int tw = f.width(label);
        if (tw > w - 6 && tw > 0) {
            float scale = Math.max(0.55f, (w - 6) / (float) tw);
            g.pose().pushMatrix();
            g.pose().translate(x + w / 2f, y + h / 2f - 4 * scale);
            g.pose().scale(scale, scale);
            g.text(f, label, -tw / 2, 0, color, true);
            g.pose().popMatrix();
        } else {
            g.centeredText(f, Component.literal(label), x + w / 2, y + (h - 8) / 2, color);
        }
        return hover;
    }

    public static int wrap(GuiGraphicsExtractor g, String text, int x, int y, int width, int color) {
        Font f = font();
        List<FormattedCharSequence> lines = f.split(FormattedText.of(text), width);
        for (FormattedCharSequence line : lines) {
            g.text(f, line, x, y, color, false);
            y += 10;
        }
        return lines.size() * 10;
    }

    public static int wrapHeight(String text, int width) {
        return font().split(FormattedText.of(text), width).size() * 10;
    }

    public static void scaledText(GuiGraphicsExtractor g, String text, float x, float y, float scale, int color, boolean centered) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        Font f = font();
        int ox = centered ? -f.width(text) / 2 : 0;
        g.text(f, text, ox, 0, color, true);
        g.pose().popMatrix();
    }

    public static void icon(GuiGraphicsExtractor g, Identifier tex, int x, int y, int size, int texSize) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, 0, size, size, texSize, texSize, texSize, texSize);
    }

    public static void icon(GuiGraphicsExtractor g, Identifier tex, int x, int y, int size, int texSize, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, 0, size, size, texSize, texSize, texSize, texSize, color);
    }

    public static Identifier skillIcon(String skill) {
        return MMORPG.id("textures/gui/skills/" + skill + ".png");
    }

    public static Identifier classIcon(String cls, boolean big) {
        return MMORPG.id("textures/gui/classes/" + cls + (big ? "_big" : "") + ".png");
    }

    public static String fmt(double v) {
        if (v >= 100000) return String.format(java.util.Locale.FRANCE, "%.0fk", v / 1000);
        if (v >= 10000) return String.format(java.util.Locale.FRANCE, "%.1fk", v / 1000);
        return String.valueOf(Math.round(v));
    }

    /** Couleur du niveau d'un monstre par rapport a celui du joueur. */
    public static int levelColor(int mobLevel, int playerLevel) {
        int d = mobLevel - playerLevel;
        if (d >= 8) return 0xFFFF3030;
        if (d >= 4) return 0xFFFF8C30;
        if (d >= -3) return 0xFFFFE050;
        if (d >= -10) return 0xFF60E060;
        return 0xFF9A9A9A;
    }
}
