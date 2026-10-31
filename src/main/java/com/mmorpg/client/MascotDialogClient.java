package com.mmorpg.client;

import com.mmorpg.MMORPG;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Bulle de dialogue des PNJ de quetes (cadre sombre, nom en rouge, texte dessous) : remplace le menu de quetes. */
public final class MascotDialogClient {
    public static final Identifier LAYER = MMORPG.id("npc_dialog");
    private static String speaker = "";
    private static String text = "";
    private static long until;

    private MascotDialogClient() {
    }

    public static void say(String name, String line) {
        speaker = name.isEmpty() ? "Mascotte" : name;
        text = line;
        until = System.currentTimeMillis() + 5000 + 55L * line.length();
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        long left = until - System.currentTimeMillis();
        if (left <= 0 || text.isEmpty()) return;
        var mc = Minecraft.getInstance();
        int w = g.guiWidth(), h = g.guiHeight();
        int maxW = Math.min(300, w / 2 - 20);
        var lines = mc.font.split(AeloriaFont.of(text), maxW);
        int pad = 10;
        int lineH = 11;
        int boxW = maxW + pad * 2, boxH = pad + 20 + lines.size() * lineH + pad;
        int x = w / 2 + 24, y = Math.max(8, h / 2 - 40 - boxH);
        float a = Math.min(1F, Math.min(left, 250L) / 250F);
        int fade = ((int) (a * 255) & 0xFF) << 24;
        g.fill(x - 3, y - 3, x + boxW + 3, y + boxH + 3, 0x00404048 | fade);
        g.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0x00707078 | fade);
        g.fill(x, y, x + boxW, y + boxH, 0x00222226 | fade);
        g.fill(x + 2, y + 2, x + boxW - 2, y + boxH - 2, 0x00282830 | fade);
        g.pose().pushMatrix();
        g.pose().translate(x + pad, y + pad);
        g.pose().scale(1.6F, 1.6F);
        g.text(mc.font, AeloriaFont.of(speaker.toUpperCase(java.util.Locale.ROOT)), 0, 0, 0x00E8302A | fade, true);
        g.pose().popMatrix();
        int ty = y + pad + 22;
        for (var line : lines) {
            g.text(mc.font, line, x + pad, ty, 0x00FFFFFF | fade, true);
            ty += lineH;
        }
    }
}
