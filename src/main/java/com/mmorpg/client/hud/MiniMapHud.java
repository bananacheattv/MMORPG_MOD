package com.mmorpg.client.hud;

import com.mmorpg.client.ClientConfig;
import com.mmorpg.client.QolClient;
import com.mmorpg.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Minimap, compteur de degats du groupe et marqueurs de ping. */
public final class MiniMapHud {
    public static final int SIZE = 84;
    private static final MapTexture MAP = new MapTexture("minimap");

    private MiniMapHud() {
    }

    public static int[] mapSize() {
        return new int[]{SIZE + 6, SIZE + 22};
    }

    public static int[] dpsSize() {
        int rows = Math.max(1, Math.min(5, QolClient.DPS.size()));
        return new int[]{128, 18 + rows * 11 + 4};
    }

    /** Dessine la minimap en coordonnees locales (0, 0). */
    public static void minimap(GuiGraphicsExtractor g, Minecraft mc) {
        var f = mc.font;
        int x0 = 3, y0 = 3;
        Ui.rounded(g, 0, 0, SIZE + 6, SIZE + 22, 5, 0xB8140E22);
        Ui.border(g, x0 - 1, y0 - 1, SIZE + 2, SIZE + 2, Ui.GOLD_DARK);
        g.fill(x0, y0, x0 + SIZE, y0 + SIZE, 0xFF0C0814);
        int px = mc.player.getBlockX(), pz = mc.player.getBlockZ();
        int half = SIZE / 2;
        MAP.draw(g, x0, y0, SIZE, SIZE, px, pz, 1.0);
        if (QolClient.hasPois()) for (QolClient.Poi p : QolClient.allPois()) {
            int dx = p.x() - px, dz = p.z() - pz;
            if (Math.abs(dx) >= half - 2 || Math.abs(dz) >= half - 2) continue;
            dot(g, x0 + half + dx, y0 + half + dz, QolClient.poiColor(p.kind()), 2);
        }
        long now = System.currentTimeMillis();
        for (QolClient.Ping p : QolClient.PINGS) {
            int dx = Math.clamp(p.x() - px, -half + 3, half - 3), dz = Math.clamp(p.z() - pz, -half + 3, half - 3);
            int r = 2 + (int) ((now / 150) % 3);
            Ui.border(g, x0 + half + dx - r, y0 + half + dz - r, r * 2 + 1, r * 2 + 1, 0xFFFFFFFF);
        }
        dot(g, x0 + half, y0 + half, 0xFFFFFFFF, 2);
        // direction du regard
        double yaw = Math.toRadians(mc.player.getYRot());
        int hx = x0 + half + (int) Math.round(-Math.sin(yaw) * 7), hy = y0 + half + (int) Math.round(Math.cos(yaw) * 7);
        g.fill(hx, hy, hx + 2, hy + 2, 0xFFFFE6A0);
        g.text(f, "N", x0 + half - 2, y0 + 1, Ui.GOLD_LIGHT, true);
        String line = QolClient.streamer() ? "Coordonnées masquées" : px + ", " + mc.player.getBlockY() + ", " + pz;
        g.centeredText(f, line, (SIZE + 6) / 2, y0 + SIZE + 5, Ui.MUTED);
    }

    private static void dot(GuiGraphicsExtractor g, int cx, int cy, int color, int r) {
        g.fill(cx - r - 1, cy - r - 1, cx + r + 2, cy + r + 2, 0xFF000000);
        g.fill(cx - r, cy - r, cx + r + 1, cy + r + 1, color);
    }

    /** Compteur de degats par seconde des membres du groupe. */
    public static void dps(GuiGraphicsExtractor g, Minecraft mc) {
        var f = mc.font;
        int[] s = dpsSize();
        Ui.rounded(g, 0, 0, s[0], s[1], 5, 0xB8140E22);
        g.text(f, "DÉGÂTS / SECONDE", 6, 5, Ui.GOLD_LIGHT, false);
        List<Map.Entry<String, QolClient.Dps>> rows = new ArrayList<>(QolClient.DPS.entrySet());
        rows.sort((a, b) -> Float.compare(b.getValue().dps, a.getValue().dps));
        float max = 1;
        for (var e : rows) max = Math.max(max, e.getValue().dps);
        int y = 17;
        int n = 0;
        for (var e : rows) {
            if (n++ >= 5) break;
            float frac = e.getValue().dps / max;
            g.fill(4, y, 4 + Math.round((s[0] - 8) * frac), y + 10, 0x7762A8FF);
            String name = QolClient.streamer() && !e.getKey().equals(mc.player.getName().getString()) ? "Joueur" : e.getKey();
            g.text(f, f.plainSubstrByWidth(name, 58), 6, y + 1, Ui.TEXT, false);
            String v = Ui.fmt(e.getValue().dps);
            g.text(f, v, s[0] - 6 - f.width(v), y + 1, Ui.GOLD_LIGHT, false);
            y += 11;
        }
        if (rows.isEmpty()) g.text(f, "En attente de combat…", 6, 18, Ui.MUTED, false);
    }

    /** Marqueurs de ping dans le monde : projection approchee a partir de l'angle de vue. */
    public static void pings(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
        if (QolClient.PINGS.isEmpty()) return;
        var f = mc.font;
        double fov = Math.toRadians(mc.options.fov().get());
        double tv = Math.tan(fov / 2), th = tv * w / (double) h;
        double yaw = Math.toRadians(mc.player.getYRot()), pitch = Math.toRadians(mc.player.getXRot());
        var eye = mc.player.getEyePosition();
        for (QolClient.Ping p : QolClient.PINGS) {
            double dx = p.x() + .5 - eye.x, dy = p.y() + .5 - eye.y, dz = p.z() + .5 - eye.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            // repere camera : avant (fx), droite (rx), haut (ux)
            double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
            double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
            double ux = Math.sin(yaw) * Math.sin(pitch), uy = Math.cos(pitch), uz = -Math.cos(yaw) * Math.sin(pitch);
            double depth = dx * fx + dy * fy + dz * fz;
            double right = dx * rx + dz * rz, up = dx * ux + dy * uy + dz * uz;
            int sx, sy;
            boolean edge = false;
            if (depth > 0.5) {
                sx = (int) (w / 2.0 + right / depth / th * (w / 2.0));
                sy = (int) (h / 2.0 - up / depth / tv * (h / 2.0));
                if (sx < 12 || sx > w - 12 || sy < 12 || sy > h - 12) edge = true;
            } else {
                sx = right >= 0 ? w - 12 : 12;
                sy = h / 2;
                edge = true;
            }
            sx = Math.clamp(sx, 12, w - 12);
            sy = Math.clamp(sy, 12, h - 12);
            long t = System.currentTimeMillis() / 120 % 6;
            int c = ClientConfig.STREAMER_MODE.get() ? 0xFFFFD040 : 0xFFFFE6A0;
            Ui.diamond(g, sx, sy, 5 + (int) (t > 3 ? 6 - t : t) / 2, c);
            String label = (QolClient.streamer() ? "Groupe" : p.name()) + " · " + Math.round(dist) + " m";
            g.centeredText(f, label, Math.clamp(sx, f.width(label) / 2 + 4, w - f.width(label) / 2 - 4), sy + 9, edge ? Ui.MUTED : 0xFFFFFFFF);
        }
    }
}
