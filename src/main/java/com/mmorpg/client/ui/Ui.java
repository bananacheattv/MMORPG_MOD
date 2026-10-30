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
        /** Echelle appliquee par MmoScreen : les rendus d'entites 3D ignorent la matrice du GUI et doivent etre mis a l'echelle a la main. */
    public static float renderScale = 1f;

    private static int sx(int v) { return Math.round(v * renderScale); }

    public static void entity(GuiGraphicsExtractor g, net.minecraft.client.renderer.entity.state.EntityRenderState state, float size,
                              org.joml.Vector3f translation, org.joml.Quaternionf rotation, org.joml.Quaternionf cameraAngle, int x0, int y0, int x1, int y1) {
        g.entity(state, size * renderScale, translation, rotation, cameraAngle, sx(x0), sx(y0), sx(x1), sx(y1));
    }

    public static void entityFollowsMouse(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int size, float yOffset, float mx, float my,
                                          net.minecraft.world.entity.LivingEntity e) {
        net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(g, sx(x0), sx(y0), sx(x1), sx(y1), Math.round(size * renderScale), yOffset, mx, my, e);
    }

    public static void entityFollowsAngle(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int size, float yOffset, float ax, float ay,
                                          net.minecraft.world.entity.LivingEntity e) {
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsAngle(g, sx(x0), sx(y0), sx(x1), sx(y1), Math.round(size * renderScale), yOffset, ax, ay, e);
    }
    /** Entité vivante de face, tournée de {@code yaw}/{@code pitch} (radians) : aperçu rotatif à la souris. */
    public static void entitySpin(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int size, float yOffset, float yaw, float pitch,
                                  net.minecraft.world.entity.LivingEntity e) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        float bodyO = e.yBodyRotO, body = e.yBodyRot, headO = e.yHeadRotO, head = e.yHeadRot, yO = e.yRotO, y = e.getYRot(), xO = e.xRotO, x = e.getXRot();
        e.yBodyRotO = e.yBodyRot = 180f; e.yHeadRotO = e.yHeadRot = 180f; e.yRotO = 180f; e.setYRot(180f); e.xRotO = 0f; e.setXRot(0f);
        net.minecraft.client.renderer.entity.state.EntityRenderState state;
        try {
            state = mc.getEntityRenderDispatcher().getRenderer(e).createRenderState(e, 1f);
            state.shadowPieces.clear();
            state.outlineColor = 0;
        } finally {
            e.yBodyRotO = bodyO; e.yBodyRot = body; e.yHeadRotO = headO; e.yHeadRot = head; e.yRotO = yO; e.setYRot(y); e.xRotO = xO; e.setXRot(x);
        }
        float scale = e.getScale();
        entity(g, state, size / scale, new org.joml.Vector3f(0, e.getBbHeight() / 2f + yOffset * scale, 0),
                new org.joml.Quaternionf().rotateZ((float) Math.PI).rotateX(pitch).rotateY(yaw), null, x0, y0, x1, y1);
    }

    public static final int GOLD = 0xFFE2B85A;
    public static final int GOLD_LIGHT = 0xFFFFE6A0;
    public static final int GOLD_DARK = 0xFF3B3350;          // bordures et filets (violet gris, interface plate)
    public static final int LINE = 0x887D659D;
    public static final int CARD = 0xB5221738;
    public static final int CARD_HOVER = 0xDB382550;
    public static final int BAR = 0xB9161027;
    public static final int TEXT = 0xFFEDE8F5;
    public static final int MUTED = 0xFFB9ABCF;
    public static final int RED = 0xFFFF6B6B;
    public static final int GREEN = 0xFF6AE06A;
    public static final int BLUE = 0xFF62A8FF;
    public static final int PANEL_TOP = 0xDA291D40;
    public static final int PANEL_BOTTOM = 0xD019142B;
    public static final int INSET = 0x80201832;

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
        roundedOutline(g,x,y,w,h,Math.min(7,h/3),color);
    }

    /** Rounded glass primitives, bounded to two small corner bands. */
    public static void rounded(GuiGraphicsExtractor g,int x,int y,int w,int h,int radius,int color) {
        if(w<=0||h<=0)return;
        int r=Math.max(0,Math.min(radius,Math.min(w,h)/2));
        g.fill(x,y+r,x+w,y+h-r,color);
        for(int i=0;i<r;i++){
            int dx=(int)Math.ceil(r-Math.sqrt(r*r-(r-i-.5)*(r-i-.5)));
            g.fill(x+dx,y+i,x+w-dx,y+i+1,color);
            g.fill(x+dx,y+h-i-1,x+w-dx,y+h-i,color);
        }
    }
    public static void roundedOutline(GuiGraphicsExtractor g,int x,int y,int w,int h,int radius,int color){
        if(w<2||h<2)return;
        int r=Math.max(1,Math.min(radius,Math.min(w,h)/2));
        g.fill(x+r,y,x+w-r,y+1,color);g.fill(x+r,y+h-1,x+w-r,y+h,color);
        g.fill(x,y+r,x+1,y+h-r,color);g.fill(x+w-1,y+r,x+w,y+h-r,color);
        int last=r;
        for(int i=0;i<r;i++){
            int dx=(int)Math.ceil(r-Math.sqrt(r*r-(r-i-.5)*(r-i-.5)));
            int len=Math.max(1,last-dx);last=dx;
            g.fill(x+dx,y+i,x+dx+len,y+i+1,color);g.fill(x+w-dx-len,y+i,x+w-dx,y+i+1,color);
            g.fill(x+dx,y+h-i-1,x+dx+len,y+h-i,color);g.fill(x+w-dx-len,y+h-i-1,x+w-dx,y+h-i,color);
        }
    }

    /** Losange decoratif. */
    public static void diamond(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int i = -r; i <= r; i++) {
            int half = r - Math.abs(i);
            g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, color);
        }
    }

    /** Grand panneau plat : fond sombre et bordure fine. */
    /** Objet agrandi dans une case a fond clair : les objets sombres (armes, armures) restent lisibles. */
    public static void itemBox(GuiGraphicsExtractor g, net.minecraft.client.gui.Font f, net.minecraft.world.item.ItemStack stack, int x, int y, int box) {
        g.fillGradient(x, y, x + box, y + box, 0xFF5E587A, 0xFF3C3756);
        border(g, x, y, box, box, 0xFF7A7298);
        if (stack.isEmpty()) return;
        float k = (box - 4) / 16f;
        g.pose().pushMatrix();
        g.pose().translate(x + 2, y + 2);
        g.pose().scale(k, k);
        g.item(stack, 0, 0);
        g.itemDecorations(f, stack, 0, 0);
        g.pose().popMatrix();
    }

    private static Identifier backdropTex;

    /** Illustration d'ambiance du prochain grand panneau (pack d'art) ; null pour aucune. */
    public static void backdrop(String name) {
        backdropTex = name == null ? null : MMORPG.id("textures/gui/illustrations/" + name + ".png");
    }

    /** Portrait d'une classe (atlas du pack d'art, ordre Guerrier, Mage, Archer, Paladin), recadre en "cover" centre sur le visage. */
    public static void classPortrait(GuiGraphicsExtractor g, int index, int x, int y, int w, int h) {
        final int[] rx = {0, 496, 992, 1487}, rw = {496, 496, 495, 496};
        final int tw = 1983, th = 793;
        int i = Math.max(0, Math.min(3, index));
        float k = Math.max(w / (float) rw[i], h / (float) th);
        int sw = Math.min(rw[i], Math.round(w / k)), sh = Math.min(th, Math.round(h / k));
        g.blit(RenderPipelines.GUI_TEXTURED, MMORPG.id("textures/gui/illustrations/classes_atlas.png"), x, y, rx[i] + (rw[i] - sw) / 2f, Math.max(0, Math.min(th - sh, Math.round(th * 0.34f - sh / 2f))), w, h, sw, sh, tw, th);
    }

    /** Image carree du pack d'art (portrait ou medaille a fond transparent), contenue sans etirement dans la boite donnee. */
    public static void squareArt(GuiGraphicsExtractor g, String name, int size, int x, int y, int box) {
        int s = Math.max(1, box);
        g.blit(RenderPipelines.GUI_TEXTURED, MMORPG.id("textures/gui/illustrations/" + name + ".png"), x, y, 0, 0, s, s, size, size, size, size);
    }

    /** Illustration 836x470 du pack d'art recadree en "cover" (sans etirement) dans la zone donnee. */
    public static void cover(GuiGraphicsExtractor g, String name, int x, int y, int w, int h) {
        final int tw = 836, th = 470;
        float k = Math.max(w / (float) tw, h / (float) th);
        int sw = Math.min(tw, Math.round(w / k)), sh = Math.min(th, Math.round(h / k));
        g.blit(RenderPipelines.GUI_TEXTURED, MMORPG.id("textures/gui/illustrations/" + name + ".png"), x, y, (tw - sw) / 2f, (th - sh) / 2f, w, h, sw, sh, tw, th);
    }

    private static boolean suppressPanel;

    /** Le cadre est deja peint par la base (ecrans avec barre d'onglets) : les grands panneaux des ecrans ne se redessinent pas. */
    public static void suppressPanel(boolean on) {
        suppressPanel = on;
    }

    public static void paintBackdropNow(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (backdropTex != null) paintBackdrop(g, x, y, w, h);
    }

    private static void paintBackdrop(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        Identifier tex = backdropTex;
        backdropTex = null;
        final int tw = 836, th = 470;
        int bx = x + 3, by = y + 3, bw = w - 6, bh = h - 6;
        // recadrage "cover" sans etirement
        float k = Math.max(bw / (float) tw, bh / (float) th);
        int sw = Math.min(tw, Math.round(bw / k)), sh = Math.min(th, Math.round(bh / k));
        g.blit(RenderPipelines.GUI_TEXTURED, tex, bx, by, (tw - sw) / 2f, (th - sh) / 2f, bw, bh, sw, sh, tw, th, 0x59FFFFFF);
        g.fill(bx, by, bx + bw, by + bh, 0x40080410);
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (suppressPanel && w >= 300 && h >= 180) return;
        rounded(g,x,y+3,w,h,10,0x35050010);
        rounded(g,x,y,w,h,10,PANEL_TOP);
        if (backdropTex != null && w >= 300 && h >= 180) paintBackdrop(g, x, y, w, h);
        border(g, x, y, w, h, LINE);
    }

    /** Panneau secondaire encastre. */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        rounded(g,x,y,w,h,7,INSET);
        border(g, x, y, w, h, 0xFF221D2E);
    }

    /** Cadre fin colore (cartes, emplacements). */
    public static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, boolean highlight) {
        rounded(g,x,y,w,h,7,highlight ? CARD_HOVER : CARD);
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
        rounded(g,x-1,y-1,w+2,h+2,h/2,0xA0100B20);
        rounded(g,x,y,w,h,h/2,0xA0271B39);
        int fw = (int) (w * frac);
        if (fw > 0) rounded(g,x,y,fw,h,h/2,top);
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
        boolean primary=enabled&&label.equals("ENTRER DANS AELORIA");
        int fill = !enabled ? 0xFF0F0D15 : mix(CARD, 0xFF000000 | rgb, hover ? 0.32f : 0.16f);
        rounded(g,x,y+2,w,h,Math.min(7,h/3),0x30050010);
        rounded(g,x,y,w,h,Math.min(7,h/3),primary?(hover?0xFFF7DA8C:0xFFE7BD65):withAlpha(fill,enabled?hover?235:195:125));
        border(g, x, y, w, h, !enabled ? 0xFF1E1A28 : hover ? 0xFF000000 | rgb : mix(LINE, 0xFF000000 | rgb, 0.4f));
        Font f = font();
        int color = primary?0xFF302039:enabled ? (hover ? 0xFFFFFFFF : TEXT) : 0xFF8D829E;
        int tw = f.width(label);
        if (tw > w - 6 && tw > 0) {
            float scale = Math.max(0.55f, (w - 6) / (float) tw);
            g.pose().pushMatrix();
            g.pose().translate(x + w / 2f, y + h / 2f - 4 * scale);
            g.pose().scale(scale, scale);
            g.text(f, label, -tw / 2, 0, color, true);
            g.pose().popMatrix();
        } else {
            if(primary)g.text(f,label,x+(w-tw)/2,y+(h-8)/2,color,false);
            else g.centeredText(f, Component.literal(label), x + w / 2, y + (h - 8) / 2, color);
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
        rounded(g,x,5,tw,17,6,withAlpha(fill,165));
        roundedOutline(g,x,5,tw,17,6,withAlpha(color,0x60));
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
        Logo.emblem(g, 8, 2, 24);
        var logo = com.mmorpg.client.AeloriaFont.of("AELORIA");
        g.pose().pushMatrix();
        g.pose().translate(36, 9);
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
