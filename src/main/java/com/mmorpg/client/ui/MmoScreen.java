package com.mmorpg.client.ui;

import com.mmorpg.client.screen.MenuScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Base des ecrans du mod : rendu "immediat" (les zones cliquables sont enregistrees pendant le rendu),
 * fond assombri personnalise et panneau central orne.
 */
public abstract class MmoScreen extends Screen {
    protected record Hit(int x, int y, int w, int h, Runnable left, Runnable right) {
    }

    protected final List<Hit> hits = new ArrayList<>();
    protected int left;
    protected int top;
    protected int pw;
    protected int ph;
    protected int mouseX;
    protected int mouseY;

    protected MmoScreen(String title) {
        super(Component.literal(title));
    }

    /** Echelle du contenu (1 = interface normale) : le cadre garde la taille de l'inventaire. */
    private float uiScale = 1f;
    /** Cadre reel (unites d'interface de la fenetre, sans mise a l'echelle) : identique pour tous les ecrans. */
    private int fx, fy, fw, fh;
    private final List<Hit> navHits = new ArrayList<>();

    /** Hauteur pour laquelle le contenu de l'ecran a ete concu (bande de la barre d'onglets comprise si bandHeight() > 0). */
    protected int designHeight() {
        return UiFrame.DESIGN_H;
    }

    /** Hauteur de la barre d'onglets deja reservee en haut du panneau virtuel (MenuScreen : 54), sinon 0. */
    protected int bandHeight() {
        return 0;
    }

    /** Onglet mis en avant dans la barre de base (null : aucun). */
    protected MenuScreen.Tab navActive() {
        return null;
    }

    /** Barre de base du menu d'inventaire : toujours presente, sauf petites fenetres de dialogue. */
    protected boolean hasNav() {
        return !compact();
    }

    /** Illustration d'ambiance (pack d'art, textures/gui/illustrations/<nom>.png) peinte discretement dans le panneau. */
    protected String backdrop() {
        return null;
    }

    /** Petite fenetre de dialogue : garde sa propre taille (ni cadre d'inventaire ni mise a l'echelle). */
    protected boolean compact() {
        return false;
    }

    public final int panelWidthPublic() {
        return pw;
    }

    public final int panelHeightPublic() {
        return ph;
    }

    public final float uiScale() {
        return uiScale;
    }

    /** Cadre reel {x, y, largeur, hauteur} et hauteur de la barre : pour le test de cohérence des écrans. */
    public final int[] realFrame() {
        return new int[]{fx, fy, fw, fh, hasNav() ? UiFrame.NAV_H : 0};
    }

    private void applyScale(int realW, int realH) {
        fw = UiFrame.width(realW);
        fh = UiFrame.height(realH);
        fx = (realW - fw) / 2;
        fy = (realH - fh) / 2;
        if (compact()) {
            this.uiScale = 1f;
        } else if (hasNav()) {
            this.uiScale = Math.max(1f, (fh - UiFrame.NAV_H) / (float) Math.max(1, designHeight() - bandHeight()));
        } else {
            this.uiScale = UiFrame.scale(realH, designHeight());
        }
        this.width = (int) Math.ceil(realW / uiScale);
        this.height = (int) Math.ceil(realH / uiScale);
    }

    @Override
    public void resize(int w, int h) {
        applyScale(w, h);
        this.repositionElements();
    }

    protected int panelWidth() {
        if (compact()) return Math.min(this.width - 12, 440);
        return Math.round(fw / uiScale);
    }

    protected int panelHeight() {
        if (compact()) return Math.min(this.height - 12, UiFrame.DESIGN_H);
        if (hasNav()) return Math.round((fh - UiFrame.NAV_H) / uiScale) + bandHeight();
        return Math.round(fh / uiScale);
    }

    private int realWidth() {
        return net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    private int realHeight() {
        return net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    @Override
    protected void init() {
        applyScale(realWidth(), realHeight());
        this.pw = panelWidth();
        this.ph = panelHeight();
        if (hasNav()) {
            // le corps du panneau commence juste sous la barre reelle ; la bande virtuelle (si reservee) la recouvre
            this.left = Math.round(fx / uiScale);
            this.top = Math.round((fy + UiFrame.NAV_H) / uiScale) - bandHeight();
        } else {
            this.left = (this.width - pw) / 2;
            this.top = (this.height - ph) / 2;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        this.extractBlurredBackground(g);
        g.fill(0, 0, realWidth(), realHeight(), 0x700D071C);
        Ui.navBar(g, realWidth(), compact() ? this.top : fy);
    }

    @Override
    public final void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        hits.clear();
        navHits.clear();
        if (hasNav()) {
            // passe reelle : cadre + barre de base a la taille exacte de l'inventaire, jamais mis a l'echelle
            Ui.panel(g, fx, fy, fw, fh);
            if (backdrop() != null) {
                Ui.backdrop(backdrop());
                Ui.paintBackdropNow(g, fx, fy + UiFrame.NAV_H, fw, fh - UiFrame.NAV_H);
            }
            int rmx = Math.round(mouseX * uiScale), rmy = Math.round(mouseY * uiScale);
            MenuScreen.drawTabs(g, this.font, fx, fy, fw, navActive(), rmx, rmy, (r, action) -> navHits.add(new Hit(r[0], r[1], r[2], r[3], action, null)));
            g.fill(fx + 6, fy + 48, fx + fw - 6, fy + 49, Ui.LINE);
        }
        g.pose().pushMatrix();
        g.pose().scale(uiScale, uiScale);
        Ui.renderScale = uiScale;
        try {
            Ui.suppressPanel(hasNav());
            renderContent(g, mouseX, mouseY, a);
            Ui.suppressPanel(false);
            super.extractRenderState(g, mouseX, mouseY, a);
            renderOverlay(g, mouseX, mouseY, a);
        } finally {
            Ui.renderScale = 1f;
        }
        g.pose().popMatrix();
    }

    protected abstract void renderContent(GuiGraphicsExtractor g, int mouseX, int mouseY, float a);

    /** Dessine par-dessus les widgets vanilla (infobulles...). */
    protected void renderOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
    }

    protected void click(int x, int y, int w, int h, Runnable left) {
        hits.add(new Hit(x, y, w, h, left, null));
    }

    protected void click(int x, int y, int w, int h, Runnable left, Runnable right) {
        hits.add(new Hit(x, y, w, h, left, right));
    }

    /** Bouton personnalise cliquable. */
    protected boolean button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean enabled, int accent, Runnable action) {
        boolean hover = Ui.button(g, x, y, w, h, label, enabled, mouseX, mouseY, accent);
        if (enabled) click(x, y, w, h, action);
        return hover;
    }

    protected boolean hovered(int x, int y, int w, int h) {
        return Ui.in(mouseX, mouseY, x, y, w, h);
    }

    public static void playClick() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        double mx = event.x(), my = event.y();
        for (Hit h : navHits) {
            if (Ui.in(mx * uiScale, my * uiScale, h.x, h.y, h.w, h.h) && h.left != null) {
                playClick();
                h.left.run();
                return true;
            }
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (Ui.in(mx, my, h.x, h.y, h.w, h.h)) {
                // 26.3 (SDL) : gauche = 1, milieu = 2, droit = 3 (constantes InputConstants.MOUSE_BUTTON_*)
                Runnable r = event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT ? h.right
                        : event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT ? h.left : null;
                if (r != null) {
                    playClick();
                    r.run();
                    return true;
                }
            }
        }
        // diagnostic (debug.log) : clic hors de toute zone cliquable
        com.mmorpg.MMORPG.LOGGER.debug("[MMORPG][clic] {} : ({}, {}) bouton {} sans effet ({} zones, ecran {}x{})",
                getClass().getSimpleName(), (int) mx, (int) my, event.button(), hits.size(), width, height);
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void tooltip(GuiGraphicsExtractor g, List<Component> lines) {
        g.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
    }
}
