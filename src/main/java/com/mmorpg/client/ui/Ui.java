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
    public static final int GOLD = 0xFFE2B85A;
    public static final int GOLD_LIGHT = 0xFFFFE6A0;
    public static final int GOLD_DARK = 0xFF3B3350;          // bordures et filets (violet gris, interface plate)
    public static final int LINE = 0xFF2B2538;
    public static final int CARD = 0xFF14111C;
    public static final int CARD_HOVER = 0xFF1E1929;
    public static final int BAR = 0xFF0D0B14;
    public static final int TEXT = 0xFFEDE8F5;
    public static final int MUTED = 0xFF8D84A3;
    public static final int RED = 0xFFFF6B6B;
    public static final int GREEN = 0xFF6AE06A;
    public static final int BLUE = 0xFF62A8FF;
    public static final int PANEL_TOP = 0xF40E0C16;
    public static final int PANEL_BOTTOM = 0xF40B0910;
    public static final int INSET = 0xF2080610;

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

    /** Grand panneau plat : fond sombre et bordure fine. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, PANEL_TOP, PANEL_BOTTOM);
        border(g, x, y, w, h, LINE);
    }

    /** Panneau secondaire encastre. */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, INSET);
        border(g, x, y, w, h, 0xFF221D2E);
    }

    /** Cadre fin colore (cartes, emplacements). */
    public static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, boolean highlight) {
        g.fill(x, y, x + w, y + h, highlight ? CARD_HOVER : CARD);
        border(g, x, y, w, h, highlight ? lighter(color, 0.2f) : mix(LINE, 0xFF000000 | (color & 0xFFFFFF), 0.3f));
    }

    /** Titre centre entre deux filets. */
    public static void title(GuiGraphicsExtractor g, int cx, int y, String text, int color) {
        Font f = font();
        int w = f.width(text);
        g.fill(cx - w / 2 - 30, y + 4, cx - w / 2 - 6, y + 5, LINE);
        g.fill(cx + w / 2 + 6, y + 4, cx + w / 2 + 30, y + 5, LINE);
        g.centeredText(f, Component.literal(text), cx, y, color);
    }

    /** Titre de section : barre doree puis texte, filet fin dessous. */
    public static void header(GuiGraphicsExtractor g, int x, int y, int w, String text) {
        g.fill(x, y, x + 2, y + 9, GOLD);
        g.text(font(), text, x + 6, y, GOLD, false);
        g.fill(x, y + 11, x + w, y + 12, LINE);
    }

    /** Barre de progression plate. */
    public static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int top, int bottom, String text) {
        frac = Math.max(0, Math.min(1, frac));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF0B0910);
        g.fill(x, y, x + w, y + h, 0xFF191521);
        int fw = (int) (w * frac);
        if (fw > 0) g.fillGradient(x, y, x + fw, y + h, top, bottom);
        border(g, x - 1, y - 1, w + 2, h + 2, LINE);
        if (text != null && h >= 7) {
            Font f = font();
            int tw = f.width(text);
            g.text(f, text, x + (w - tw) / 2, y + (h - 8) / 2 + (h >= 9 ? 1 : 0), 0xFFFFFFFF, true);
        }
    }

    /** Bouton plat ; renvoie vrai si survole. La couleur d'accent teinte le fond et la bordure. */
    public static boolean button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean enabled, int mx, int my, int accent) {
        boolean hover = enabled && in(mx, my, x, y, w, h);
        int rgb = accent & 0xFFFFFF;
        int fill = !enabled ? 0xFF0F0D15 : mix(CARD, 0xFF000000 | rgb, hover ? 0.32f : 0.16f);
        g.fill(x, y, x + w, y + h, fill);
        border(g, x, y, w, h, !enabled ? 0xFF1E1A28 : hover ? 0xFF000000 | rgb : mix(LINE, 0xFF000000 | rgb, 0.4f));
        Font f = font();
        int color = enabled ? (hover ? 0xFFFFFFFF : TEXT) : 0xFF5C556E;
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

    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int gg = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (gg << 8) | bl;
    }

    /** Pastille (or, niveau, nom...) ; renvoie l'abscisse de son bord gauche, pour chainer vers la gauche. */
    public static int chip(GuiGraphicsExtractor g, String text, int right, int color, int fill) {
        Font f = font();
        int tw = f.width(text) + 12;
        int x = right - tw;
        g.fill(x, 5, x + tw, 22, fill);
        g.outline(x, 5, tw, 17, withAlpha(color & 0xFFFFFF, 0x60));
        g.text(f, text, x + 6, 10, color, false);
        return x - 6;
    }

    /**
     * Barre du haut commune aux ecrans du mod : logo Aeloria a gauche, or / niveau / nom du joueur a droite. Dessinee seulement si
     * l'ecran est assez haut pour qu'elle ne gene pas le panneau ({@code panelTop} = ordonnee du haut du panneau).
     */
    public static void navBar(GuiGraphicsExtractor g, int width, int panelTop) {
        if (panelTop < 31) return;
        Font f = font();
        g.fill(0, 0, width, 27, BAR);
        g.fill(0, 27, width, 28, LINE);
        var logo = com.mmorpg.client.AeloriaFont.of("AELORIA");
        g.pose().pushMatrix();
        g.pose().translate(12, 9);
        g.pose().scale(1.2F, 1.2F);
        g.text(f, logo, 0, 0, GOLD, true);
        g.pose().popMatrix();
        com.mmorpg.rpg.PlayerData d = com.mmorpg.client.ClientData.DATA;
        int cx = width - 8;
        cx = chip(g, Minecraft.getInstance().getUser().getName(), cx, TEXT, 0xFF1B1626);
        cx = chip(g, "Niv. " + d.level, cx, BLUE, 0xFF141C2B);
        chip(g, fmt(d.gold) + " or", cx, GOLD, 0xFF2A2112);
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
