package com.mmorpg.client.screen;

import net.minecraft.world.level.ChunkPos;
import com.mmorpg.client.QolClient;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.Map;

/** Carte du monde : zones explorees, points d'interet decouverts, deplacement a la souris et zoom a la molette. */
public class WorldMapScreen extends MmoScreen {
    private static double zoom = 1.0;
    private double cx, cz;
    private boolean centered;
    private String shownMap = "";

    public WorldMapScreen() {
        super("Carte du monde");
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (!QolClient.dim().equals(shownMap)) {   // changement de dimension/serveur : nouvelle carte, recentrage
            shownMap = QolClient.dim();
            centered = false;
        }
        if (!centered && mc.player != null) {
            cx = mc.player.getX();
            cz = mc.player.getZ();
            centered = true;
        }
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, ("CARTE · " + QolClient.mapName()).toUpperCase(java.util.Locale.ROOT), Ui.GOLD_LIGHT);
        int x0 = left + 8, y0 = top + 24, w = pw - 16, h = ph - 24 - 26;
        g.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + h + 1, Ui.GOLD_DARK);
        g.fill(x0, y0, x0 + w, y0 + h, 0xFF0C0814);
        g.enableScissor(x0, y0, x0 + w, y0 + h);
        Map<Long, int[]> chunks = QolClient.chunks();
        int step = zoom >= 2 ? 1 : zoom >= 0.25 ? 2 : 3;
        if (zoom < 0.25) {   // dezoom extreme : une case par chunk explore (aucun trou, cout proportionnel aux chunks)
            int cs = Math.max(1, (int) Math.ceil(16 * zoom));
            for (Map.Entry<Long, int[]> e : chunks.entrySet()) {
                int sx = (int) Math.round(w / 2.0 + (ChunkPos.getX(e.getKey()) * 16 - cx) * zoom), sy = (int) Math.round(h / 2.0 + (ChunkPos.getZ(e.getKey()) * 16 - cz) * zoom);
                if (sx + cs <= 0 || sy + cs <= 0 || sx >= w || sy >= h) continue;
                int c = e.getValue()[136];
                if (c != 0) g.fill(x0 + Math.max(0, sx), y0 + Math.max(0, sy), x0 + Math.min(w, sx + cs), y0 + Math.min(h, sy + cs), c);
            }
        } else
        for (int row = 0; row < h; row += step) {
            int z = (int) Math.floor(cz + (row - h / 2.0) / zoom);
            int runStart = 0, runColor = 0;
            for (int col = 0; col <= w; col += step) {
                int c = col < w ? QolClient.colorAt(chunks, (int) Math.floor(cx + (col - w / 2.0) / zoom), z) : -1;
                if (c != runColor || col >= w) {
                    if (runColor != 0 && col > runStart) g.fill(x0 + runStart, y0 + row, x0 + Math.min(col, w), y0 + row + step, runColor);
                    runStart = col;
                    runColor = c;
                }
            }
        }
        QolClient.Poi hover = null;
        for (QolClient.Poi p : QolClient.allPois()) {
            int sx = x0 + (int) Math.round(w / 2.0 + (p.x() - cx) * zoom), sy = y0 + (int) Math.round(h / 2.0 + (p.z() - cz) * zoom);
            if (sx < x0 || sx > x0 + w || sy < y0 || sy > y0 + h) continue;
            Ui.diamond(g, sx, sy, 4, 0xFF000000);
            Ui.diamond(g, sx, sy, 3, QolClient.poiColor(p.kind()));
            if (Ui.in(mx, my, sx - 4, sy - 4, 9, 9)) hover = p;
        }
        for (QolClient.Ping p : QolClient.PINGS) {
            int sx = x0 + (int) Math.round(w / 2.0 + (p.x() - cx) * zoom), sy = y0 + (int) Math.round(h / 2.0 + (p.z() - cz) * zoom);
            Ui.border(g, sx - 4, sy - 4, 9, 9, 0xFFFFFFFF);
        }
        if (mc.player != null) {
            int sx = x0 + (int) Math.round(w / 2.0 + (mc.player.getX() - cx) * zoom), sy = y0 + (int) Math.round(h / 2.0 + (mc.player.getZ() - cz) * zoom);
            g.fill(sx - 3, sy - 3, sx + 4, sy + 4, 0xFF000000);
            g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFFFFFFFF);
        }
        g.disableScissor();
        // legende
        int lx = x0, ly = y0 + h + 6;
        for (String k : new String[]{"quest", "shop", "dungeon", "mine", "exit", "arena", "boss", "party"}) {
            Ui.diamond(g, lx + 4, ly + 5, 3, QolClient.poiColor(k));
            String t = QolClient.poiLabel(k);
            g.text(font, t, lx + 11, ly + 1, Ui.MUTED, false);
            lx += font.width(t) + 20;
        }
        String coords = QolClient.streamer() ? "Coordonnées masquées" : (int) cx + ", " + (int) cz + "   zoom x" + String.format(zoom < 0.1 ? "%.3f" : "%.1f", zoom);
        g.text(font, coords, x0 + w - font.width(coords), top + 10, Ui.MUTED, false);
        if (hover != null) {
            String t = hover.name() + " (" + QolClient.poiLabel(hover.kind()) + ")";
            int tw = font.width(t) + 8;
            Ui.rounded(g, mx + 8, my - 4, tw, 14, 3, 0xE6140E22);
            g.text(font, t, mx + 12, my, Ui.TEXT, false);
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        cx -= dx / zoom;
        cz -= dy / zoom;
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        zoom = Math.clamp(zoom * (sy > 0 ? 1.25 : 0.8), 0.025, 6);
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (com.mmorpg.client.Keys.MAP.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
