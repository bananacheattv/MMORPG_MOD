package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.QolClient;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.server.Qol;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Journal de jeu : statistiques detaillees du personnage. */
public class JournalScreen extends MenuScreen {
    public JournalScreen() {
        super(Tab.JOURNAL);
    }

    @Override
    protected void init() {
        super.init();
        QolClient.action("journal");
    }

    private long c(String k) {
        return ClientData.DATA.counters.getOrDefault(k, 0L);
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        var d = ClientData.DATA;
        int x0 = left + 8, y0 = contentTop, w = (pw - 24) / 2;
        long minutes = c(Qol.MINUTES);
        String[][] left = {
                {"Temps de jeu", minutes / 60 + " h " + minutes % 60 + " min"},
                {"Niveau", String.valueOf(d.level)},
                {"Monstres tués", String.valueOf(c("kills"))},
                {"Boss vaincus", String.valueOf(c("boss_kills"))},
                {"Donjons terminés", String.valueOf(c("dungeons"))},
                {"Quêtes terminées", String.valueOf(c("quests"))},
                {"Poissons pêchés", String.valueOf(c("fish_caught"))},
        };
        String[][] right = {
                {"Dégâts infligés", Ui.fmt(c(Qol.DEALT))},
                {"Dégâts subis", Ui.fmt(c(Qol.TAKEN))},
                {"Plus gros coup", Ui.fmt(c(Qol.BEST_HIT))},
                {"Morts", String.valueOf(c(Qol.DEATHS))},
                {"Or gagné", Ui.fmt(c("gold_earned"))},
                {"Victoires PvP", String.valueOf(c("pvp_wins"))},
                {"Éliminations PvP", String.valueOf(c("pvp_kills"))},
        };
        Ui.header(g, x0, y0, w, "PROGRESSION");
        Ui.header(g, x0 + w + 8, y0, w, "COMBAT");
        for (int i = 0; i < left.length; i++) {
            row(g, x0, y0 + 20 + i * 18, w, left[i]);
            row(g, x0 + w + 8, y0 + 20 + i * 18, w, right[i]);
        }
        int ky = y0 + 20 + left.length * 18 + 6;
        Ui.header(g, x0, ky, pw - 16, "BESTIAIRE (monstres tués par type)");
        int n = 0;
        for (var e : d.kills.entrySet().stream().sorted((p, q) -> q.getValue() - p.getValue()).limit(6).toList()) {
            row(g, x0 + (n % 2) * (w + 8), ky + 20 + (n / 2) * 16, w, new String[]{e.getKey(), String.valueOf(e.getValue())});
            n++;
        }
    }

    private void row(GuiGraphicsExtractor g, int x, int y, int w, String[] r) {
        Ui.inset(g, x, y, w, 16);
        g.text(font, r[0], x + 6, y + 4, Ui.MUTED, false);
        g.text(font, r[1], x + w - 6 - font.width(r[1]), y + 4, Ui.GOLD_LIGHT, false);
    }
}
