package com.mmorpg.client.ui;

import com.mmorpg.MMORPG;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Logo d'Aeloria (images detourees par tools/build_logo.py, dans textures/gui/logo). */
public final class Logo {
    private static final Identifier EMBLEM_64 = MMORPG.id("textures/gui/logo/emblem_64.png");
    private static final Identifier EMBLEM_128 = MMORPG.id("textures/gui/logo/emblem_128.png");
    private static final Identifier EMBLEM_256 = MMORPG.id("textures/gui/logo/emblem_256.png");
    private static final Identifier LOGO_256 = MMORPG.id("textures/gui/logo/logo_256.png");
    private static final Identifier LOGO_512 = MMORPG.id("textures/gui/logo/logo_512.png");
    /** Proportions du logo complet (largeur / hauteur de logo_512.png : 1420 x 632 en origine). */
    public static final double FULL_RATIO = 1420.0 / 632.0;

    private Logo() {
    }

    /** Embleme seul, {@code size} unites d'interface de cote (la taille de texture la plus proche est choisie selon l'echelle de l'ecran). */
    public static void emblem(GuiGraphicsExtractor g, int x, int y, int size) {
        double device = size * net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        Identifier tex;
        int px;
        if (device <= 80) {
            tex = EMBLEM_64;
            px = 64;
        } else if (device <= 160) {
            tex = EMBLEM_128;
            px = 128;
        } else {
            tex = EMBLEM_256;
            px = 256;
        }
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, 0, size, size, px, px, px, px);
    }

    /** Logo complet (embleme et nom), {@code width} unites de large, centre en {@code cx} ; renvoie sa hauteur. */
    public static int full(GuiGraphicsExtractor g, int cx, int y, int width) {
        int h = (int) Math.round(width / FULL_RATIO);
        double device = width * net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        boolean small = device <= 300;
        int px = small ? 256 : 512;
        int ph = (int) Math.round(px / FULL_RATIO);
        g.blit(RenderPipelines.GUI_TEXTURED, small ? LOGO_256 : LOGO_512, cx - width / 2, y, 0, 0, width, h, px, ph, px, ph);
        return h;
    }
}
