package com.mmorpg.client.screen;

import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.dungeon.DungeonRun;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;

/** Carte du donjon : les salles déjà découvertes, les grilles d'étage, le chrono et les morts de l'expédition. */
public final class DungeonMapScreen extends MmoScreen {
    private static final String[] ROMAN = {"I", "II", "III"};
    private final CompoundTag data;
    private final long openedAt = System.currentTimeMillis();

    public DungeonMapScreen(CompoundTag data) {
        super("Carte du donjon");
        this.data = data;
    }

    @Override
    protected int panelWidth() {
        return Math.min(width - 12, 400);
    }

    @Override
    protected int panelHeight() {
        return Math.min(height - 12, 230);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, data.getStringOr("title", "Donjon").toUpperCase(java.util.Locale.ROOT), Ui.GOLD_LIGHT);
        int secs = data.getIntOr("secs", 0) + (data.getBooleanOr("done", false) ? 0 : (int) ((System.currentTimeMillis() - openedAt) / 1000));
        String info = "⏱ " + secs / 60 + ":" + (secs % 60 < 10 ? "0" : "") + secs % 60 + "   ☠ " + data.getIntOr("deaths", 0)
                + (data.getIntOr("deaths", 0) == 0 ? " (coffre bonus possible)" : "");
        g.text(Ui.font(), info, left + 12, top + 24, 0xFF000000 | Ui.MUTED, true);
        int seen = data.getIntOr("seen", 0), keys = data.getIntOr("keys", 0), opened = data.getIntOr("opened", 0), have = data.getIntOr("have", 0);
        int at = data.getIntOr("at", -1);
        int colW = (pw - 24) / 4;
        Ui.inset(g, left + 8, top + 38, pw - 16, ph - 46);
        String[] heads = {"Étage I", "Étage II", "Étage III", "Boss"};
        int[] row = new int[4];
        for (int f = 0; f < 4; f++) {
            int cx = left + 12 + f * colW;
            g.text(Ui.font(), heads[f], cx + 2, top + 44, 0xFF000000 | Ui.GOLD, true);
            if (f < 3) {
                boolean open = (opened >> f & 1) == 1;
                String st = open ? "grille ouverte" : (have >> f & 1) == 1 ? "clé en poche" : (keys >> f & 1) == 1 ? "clé à reprendre" : "grille scellée";
                g.text(Ui.font(), st, cx + 2, top + ph - 18, 0xFF000000 | (open ? Ui.GREEN : (keys >> f & 1) == 1 ? Ui.GOLD_LIGHT : Ui.RED), true);
            }
        }
        for (int i = 0; i < DungeonRun.NODES.length; i++) {
            int f = (int) DungeonRun.NODES[i][3];
            int x = left + 12 + f * colW;
            int y = top + 58 + row[f]++ * 20;
            boolean known = (seen >> i & 1) == 1;
            boolean here = i == at;
            int fill = known ? Ui.CARD_HOVER : Ui.BAR;
            Ui.rounded(g, x, y, colW - 6, 16, 4, fill);
            if (here) Ui.border(g, x, y, colW - 6, 16, 0xFF000000 | Ui.GOLD_LIGHT);
            String label = known ? (String) DungeonRun.NODES[i][0] : "???";
            g.text(Ui.font(), (here ? "▶ " : "") + label, x + 4, y + 4, known ? 0xFF000000 | Ui.TEXT : 0xFF6A6080, true);
        }
        g.text(Ui.font(), "Échap pour fermer", left + pw - 100, top + ph - 18, 0xFF000000 | Ui.MUTED, true);
    }
}
