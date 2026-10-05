package com.mmorpg.client.hud;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Compact Eldoria roster, including single-player worlds. */
public final class PlayerListHud {
    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || !mc.options.keyPlayerList.isDown()) return;
        var players = mc.getConnection().getOnlinePlayers().stream()
            .sorted(java.util.Comparator.comparing(p -> p.getProfile().name(), String.CASE_INSENSITIVE_ORDER)).limit(80).toList();
        int n = players.size(), maxRows = Math.max(1, (g.guiHeight() - 66) / 13);
        int cols = Math.max(1, (n + maxRows - 1) / maxRows), rows = (n + cols - 1) / cols;
        int colW = Math.min(220, (g.guiWidth() - 20) / cols), w = colW * cols;
        int x = (g.guiWidth() - w) / 2, y = 12;
        g.fill(x, y, x + w, y + 46 + rows * 13, 0xFF110F1A);
        Ui.frame(g, x, y, w, 46 + rows * 13, Ui.GOLD, false);
        g.centeredText(mc.font, Component.literal("✦ ELDORIA ✦"), x + w / 2, y + 5, Ui.GOLD);
        int online = mc.getConnection().getOnlinePlayers().size();
        g.centeredText(mc.font, Component.literal(online + " aventurier(s) en ligne"), x + w / 2, y + 16, Ui.MUTED);
        for (int i = 0; i < n; i++) {
            var info = players.get(i);
            var name = info.getTabListDisplayName();
            if (name == null) name = Component.literal(info.getProfile().name());
            int px = x + (i / rows) * colW + 5, py = y + 29 + i % rows * 13;
            g.text(mc.font, net.minecraft.locale.Language.getInstance().getVisualOrder(mc.font.substrByWidth(name, colW - 42)), px, py, Ui.TEXT, false);
            String ping = info.getLatency() + "ms";
            g.text(mc.font, ping, px + colW - 10 - mc.font.width(ping), py, Ui.MUTED, false);
        }
        String footer = n < online ? "80 joueurs affichés sur " + online : ClientData.DATA.activeQuests.size() + " quête(s) en cours • Bon voyage !";
        g.centeredText(mc.font, Component.literal(mc.font.plainSubstrByWidth(footer, w - 12)), x + w / 2, y + 33 + rows * 13, Ui.GOLD);
    }
}
