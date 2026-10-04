package com.mmorpg.client.screen;

import com.google.gson.JsonObject;
import com.mmorpg.client.Keys;
import com.mmorpg.client.hud.HudLayout;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Edition de l'interface en jeu : chaque element du HUD (mod et vanilla) se deplace a la souris,
 * se redimensionne a la molette et se masque au clic droit. Les changements sont enregistres a la fermeture.
 */
public class HudEditorScreen extends Screen {
    private static final int SNAP = 4;
    private static final int KEY_KP_MINUS = 86;                       // '-' du pave numerique (absent d'InputConstants)
    private static boolean grid = true;
    private static int panelX = Integer.MIN_VALUE;
    private static int panelY;

    private final Screen parent;
    private final Map<String, JsonObject> snapshot;
    private final List<int[]> guides = new ArrayList<>();
    private final List<Object[]> buttons = new ArrayList<>();
    private HudLayout.Element selected;
    private HudLayout.Element dragging;
    private float grabX;
    private float grabY;
    private boolean draggingPanel;
    private boolean cancelled;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("Modifier l'interface"));
        this.parent = parent;
        this.snapshot = HudLayout.snapshot();
    }

    private static final int PW = 268;
    private static final int PH = 86;

    @Override
    protected void init() {
        HudLayout.editing = true;
        if (panelX == Integer.MIN_VALUE) {
            panelX = (width - PW) / 2;
            panelY = height / 2 - PH / 2 - 10;
        }
        clampPanel();
    }

    private void clampPanel() {
        panelX = Math.max(0, Math.min(width - PW, panelX));
        panelY = Math.max(0, Math.min(height - PH, panelY));
    }

    @Override
    public void removed() {
        HudLayout.editing = false;
        super.removed();
    }

    @Override
    public void onClose() {
        if (cancelled) HudLayout.restore(snapshot);
        HudLayout.save();
        HudLayout.editing = false;
        // l'ecran Personnage est un ecran d'inventaire : on en rouvre un neuf plutot que de reutiliser l'ancien
        minecraft.gui.setScreen(parent instanceof CharacterScreen && minecraft.player != null ? new CharacterScreen(minecraft.player) : parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================================================================== rendu

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fill(0, 0, width, height, 0x28000000);
        if (!grid) return;
        for (int x = 0; x < width; x += 16) g.fill(x, 0, x + 1, height, 0x12FFFFFF);
        for (int y = 0; y < height; y += 16) g.fill(0, y, width, y + 1, 0x12FFFFFF);
        g.fill(width / 2, 0, width / 2 + 1, height, 0x30FFD060);
        g.fill(0, height / 2, width, height / 2 + 1, 0x30FFD060);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        HudLayout.Element hover = draggingPanel || inPanel(mouseX, mouseY) ? null : pick(mouseX, mouseY);
        for (HudLayout.Element e : HudLayout.elements()) {
            int[] r = e.rect(width, height);
            boolean sel = e == selected;
            boolean hov = e == hover;
            if (!e.visible()) {
                g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0xB0180808);
                for (int k = -r[3]; k < r[2]; k += 6) {                      // hachures
                    int x0 = Math.max(r[0], r[0] + k);
                    int y0 = r[1] + Math.max(0, -k);
                    for (int s = 0; x0 + s < r[0] + r[2] && y0 + s < r[1] + r[3]; s++) g.fill(x0 + s, y0 + s, x0 + s + 1, y0 + s + 1, 0x50FF5050);
                }
                Ui.scaledText(g, "Masqué", r[0] + r[2] / 2f, r[1] + r[3] / 2f - 3, 0.75f, 0xFFFF8080, true);
            }
            int col = sel ? Ui.GOLD_LIGHT : hov ? 0xFFFFFFFF : !e.visible() ? 0xC0FF5050 : e.vanilla() ? 0xA070B8FF : 0xA0E8C060;
            if (sel || hov) g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], sel ? 0x30FFE6A0 : 0x18FFFFFF);
            Ui.border(g, r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2, col);
            if (sel) {
                Ui.diamond(g, r[0] - 1, r[1] - 1, 2, Ui.GOLD);
                Ui.diamond(g, r[0] + r[2], r[1] - 1, 2, Ui.GOLD);
                Ui.diamond(g, r[0] - 1, r[1] + r[3], 2, Ui.GOLD);
                Ui.diamond(g, r[0] + r[2], r[1] + r[3], 2, Ui.GOLD);
            }
            label(g, e, r, sel || hov);
        }
        for (int[] gd : guides) {
            if (gd[0] == 0) g.fill(gd[1], 0, gd[1] + 1, height, 0xC060FFC0);
            else g.fill(0, gd[1], width, gd[1] + 1, 0xC060FFC0);
        }
        panel(g, mouseX, mouseY);
    }

    private void label(GuiGraphicsExtractor g, HudLayout.Element e, int[] r, boolean strong) {
        String txt = e.label + (e.scale() != 1f ? "  " + Math.round(e.scale() * 100) + " %" : "");
        float sc = strong ? 0.75f : 0.5f;
        int tw = (int) Math.ceil(font.width(txt) * sc) + 4;
        int th = (int) Math.ceil(9 * sc) + 2;
        int lx = Math.max(0, Math.min(width - tw, r[0]));
        int ly = r[1] - th - 1 >= 0 ? r[1] - th - 1 : Math.min(height - th, r[1] + r[3] + 1);
        g.fill(lx, ly, lx + tw, ly + th, strong ? 0xE0000000 : 0xA0000000);
        g.pose().pushMatrix();
        g.pose().translate(lx + 2, ly + 1.5f);
        g.pose().scale(sc, sc);
        g.text(font, txt, 0, 0, strong ? Ui.GOLD_LIGHT : 0xFFD8D0C0, false);
        g.pose().popMatrix();
    }

    private void panel(GuiGraphicsExtractor g, int mx, int my) {
        buttons.clear();
        int x = panelX;
        int y = panelY;
        Ui.panel(g, x, y, PW, PH);
        g.fillGradient(x + 2, y + 2, x + PW - 2, y + 14, 0xFF4A3618, 0xFF2A1E10);
        Ui.scaledText(g, "Édition de l'interface", x + PW / 2f, y + 4, 0.85f, Ui.GOLD_LIGHT, true);
        String[] help = {
                "Glisser : déplacer  •  Molette : taille  •  Clic droit : masquer",
                "Clic molette ou R : réinitialiser  •  Flèches : ajuster (Maj ×10)",
                "Maj en glissant : sans aimantation  •  Échap : enregistrer et quitter",
        };
        for (int i = 0; i < help.length; i++) {
            g.pose().pushMatrix();
            g.pose().translate(x + 8, y + 18 + i * 8);
            g.pose().scale(0.68f, 0.68f);
            g.text(font, help[i], 0, 0, Ui.TEXT, false);
            g.pose().popMatrix();
        }
        String sel = selected == null ? "Aucun élément sélectionné"
                : selected.label + " — " + Math.round(selected.scale() * 100) + " %" + (selected.visible() ? "" : " — masqué");
        Ui.scaledText(g, sel, x + PW / 2f, y + 45, 0.75f, selected == null ? Ui.MUTED : Ui.GOLD, true);
        int by = y + PH - 24;
        int bw = (PW - 16 - 9) / 4;
        button(g, x + 8, by, bw, 16, (grid ? "☑" : "☐") + " Grille", mx, my, () -> grid = !grid);
        button(g, x + 8 + (bw + 3), by, bw, 16, "Tout réinitialiser", mx, my, () -> {
            HudLayout.resetAll();
            selected = null;
        });
        button(g, x + 8 + 2 * (bw + 3), by, bw, 16, "Annuler", mx, my, () -> {
            cancelled = true;
            onClose();
        });
        button(g, x + 8 + 3 * (bw + 3), by, bw, 16, "Terminé", mx, my, this::onClose);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, int mx, int my, Runnable action) {
        boolean hover = Ui.in(mx, my, x, y, w, h);
        g.fillGradient(x, y, x + w, y + h, hover ? 0xFF5A4220 : 0xFF2C2218, hover ? 0xFF3A2A14 : 0xFF161012);
        Ui.border(g, x, y, w, h, hover ? Ui.GOLD : Ui.GOLD_DARK);
        float sc = Math.min(0.75f, (w - 6) / (float) Math.max(1, font.width(label)));
        Ui.scaledText(g, label, x + w / 2f, y + h / 2f - 4 * sc, sc, hover ? Ui.GOLD_LIGHT : Ui.TEXT, true);
        buttons.add(new Object[]{new int[]{x, y, w, h}, action});
    }

    // ================================================================== interactions

    private boolean inPanel(double mx, double my) {
        return Ui.in(mx, my, panelX, panelY, PW, PH);
    }

    /** Element sous la souris (le plus petit en cas de chevauchement). */
    private HudLayout.Element pick(double mx, double my) {
        HudLayout.Element best = null;
        long bestArea = Long.MAX_VALUE;
        for (HudLayout.Element e : HudLayout.elements()) {
            int[] r = e.rect(width, height);
            if (Ui.in(mx, my, r[0], r[1], Math.max(4, r[2]), Math.max(4, r[3]))) {
                long area = (long) r[2] * r[3];
                if (area < bestArea) {
                    bestArea = area;
                    best = e;
                }
            }
        }
        return best;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (inPanel(mx, my)) {
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                for (Object[] b : buttons) {
                    int[] r = (int[]) b[0];
                    if (Ui.in(mx, my, r[0], r[1], r[2], r[3])) {
                        MmoScreen.playClick();
                        ((Runnable) b[1]).run();
                        return true;
                    }
                }
                draggingPanel = true;
                grabX = (float) (mx - panelX);
                grabY = (float) (my - panelY);
            }
            return true;
        }
        HudLayout.Element e = pick(mx, my);
        com.mmorpg.MMORPG.LOGGER.debug("[MMORPG][clic] HudEditorScreen : ({}, {}) bouton {} -> {}", (int) mx, (int) my, event.button(),
                e == null ? "rien" : e.id);
        switch (event.button()) {
            case InputConstants.MOUSE_BUTTON_LEFT -> {
                selected = e;
                if (e != null) {
                    int[] r = e.rect(width, height);
                    dragging = e;
                    grabX = (float) (mx - r[0]);
                    grabY = (float) (my - r[1]);
                }
            }
            case InputConstants.MOUSE_BUTTON_RIGHT -> {
                if (e != null) {
                    e.toggleVisible();
                    selected = e;
                    MmoScreen.playClick();
                }
            }
            case InputConstants.MOUSE_BUTTON_MIDDLE -> {
                if (e != null) {
                    e.reset();
                    selected = e;
                    MmoScreen.playClick();
                }
            }
            default -> {
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingPanel) {
            panelX = (int) (event.x() - grabX);
            panelY = (int) (event.y() - grabY);
            clampPanel();
            return true;
        }
        if (dragging == null) return false;
        int[] r = dragging.rect(width, height);
        float nx = (float) (event.x() - grabX);
        float ny = (float) (event.y() - grabY);
        nx = Math.max(0, Math.min(width - r[2], nx));
        ny = Math.max(0, Math.min(height - r[3], ny));
        guides.clear();
        if (!event.hasShiftDown()) {
            nx = snap(nx, r[2], true);
            ny = snap(ny, r[3], false);
        }
        dragging.moveTo(Math.round(nx), Math.round(ny), width, height);
        return true;
    }

    /** Aimantation aux bords et au centre de l'ecran et aux bords des autres elements. */
    private float snap(float v, int size, boolean xAxis) {
        int screen = xAxis ? width : height;
        List<int[]> cands = new ArrayList<>();                        // {position du coin, ligne guide}
        cands.add(new int[]{0, 0});
        cands.add(new int[]{screen - size, screen - 1});
        cands.add(new int[]{screen / 2 - size / 2, screen / 2});
        for (HudLayout.Element o : HudLayout.elements()) {
            if (o == dragging) continue;
            int[] r = o.rect(width, height);
            int a = xAxis ? r[0] : r[1];
            int b = a + (xAxis ? r[2] : r[3]);
            cands.add(new int[]{a, a});                               // bords alignes
            cands.add(new int[]{b - size, b});
            cands.add(new int[]{b + 2, b});                           // colle a cote (2 px d'ecart)
            cands.add(new int[]{a - size - 2, a});
        }
        int[] best = null;
        float bestD = SNAP + 0.5f;
        for (int[] c : cands) {
            float d = Math.abs(c[0] - v);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        if (best == null) return v;
        guides.add(new int[]{xAxis ? 0 : 1, best[1]});
        return best[0];
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        draggingPanel = false;
        guides.clear();
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY == 0 || inPanel(x, y)) return false;
        HudLayout.Element e = pick(x, y);
        if (e == null) e = selected;
        if (e == null) return false;
        selected = e;
        e.setScale(e.scale() + (scrollY > 0 ? 0.05f : -0.05f), width, height);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (Keys.HUD_EDIT.matches(event)) {
            onClose();
            return true;
        }
        if (selected != null) {
            int step = event.hasShiftDown() ? 10 : 1;
            int[] r = selected.rect(width, height);
            switch (event.key()) {
                case InputConstants.KEY_LEFT -> selected.moveTo(r[0] - step, r[1], width, height);
                case InputConstants.KEY_RIGHT -> selected.moveTo(r[0] + step, r[1], width, height);
                case InputConstants.KEY_UP -> selected.moveTo(r[0], r[1] - step, width, height);
                case InputConstants.KEY_DOWN -> selected.moveTo(r[0], r[1] + step, width, height);
                case InputConstants.KEY_R -> selected.reset();
                case InputConstants.KEY_H, InputConstants.KEY_DELETE -> selected.toggleVisible();
                case InputConstants.KEY_ADD, InputConstants.KEY_EQUALS -> selected.setScale(selected.scale() + 0.05f, width, height);
                case KEY_KP_MINUS, InputConstants.KEY_MINUS -> selected.setScale(selected.scale() - 0.05f, width, height);
                default -> {
                    return super.keyPressed(event);
                }
            }
            return true;
        }
        return super.keyPressed(event);
    }

    /** Pour les tests automatiques. */
    public HudLayout.Element selected() {
        return selected;
    }
}
