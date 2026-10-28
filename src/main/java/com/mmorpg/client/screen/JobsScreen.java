package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.job.Job;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Metiers : niveau, barre d'XP, paliers (10 / 25 / 50), recompenses et bonus de chaque metier. */
public class JobsScreen extends MenuScreen {
    private static Job selected = Job.MINEUR;

    public JobsScreen() {
        super(Tab.METIERS);
    }

    private static long xp(Job j) {
        return ClientData.DATA.jobXp.getOrDefault(j.id, 0L);
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        int x0 = left + 8, y0 = contentTop, lw = 150;
        Ui.header(g, x0, y0, lw, "MÉTIERS");
        int ry = y0 + 18;
        for (Job j : Job.values()) {
            boolean sel = j == selected;
            Ui.frame(g, x0, ry, lw, 34, j.color, sel || hovered(x0, ry, lw, 34));
            g.text(font, j.label, x0 + 6, ry + 5, sel ? Ui.GOLD_LIGHT : Ui.TEXT, false);
            int lvl = Job.levelFor(xp(j));
            String lv = "Niv. " + lvl;
            g.text(font, lv, x0 + lw - 6 - font.width(lv), ry + 5, j.color, false);
            float frac = lvl >= Job.MAX_LEVEL ? 1f : (float) (xp(j) - Job.totalFor(lvl)) / Job.xpToNext(lvl);
            Ui.bar(g, x0 + 6, ry + 21, lw - 12, 6, frac, j.color, Ui.darker(j.color, 0.6f), null);
            final Job jj = j;
            click(x0, ry, lw, 34, () -> selected = jj);
            ry += 38;
        }

        int dx = x0 + lw + 10, dw = left + pw - 8 - dx, dh = ph - 30 - 10;
        Ui.inset(g, dx, y0, dw, dh);
        Job j = selected;
        long total = xp(j);
        int lvl = Job.levelFor(total);
        Ui.scaledText(g, j.label, dx + 8, y0 + 8, 1.5f, j.color, false);
        String lv = "Niveau " + lvl + " / " + Job.MAX_LEVEL;
        g.text(font, lv, dx + dw - 8 - font.width(lv), y0 + 10, Ui.GOLD_LIGHT, false);
        g.text(font, j.description, dx + 8, y0 + 26, Ui.MUTED, false);
        boolean max = lvl >= Job.MAX_LEVEL;
        long cur = max ? 0 : total - Job.totalFor(lvl), need = max ? 0 : Job.xpToNext(lvl);
        Ui.bar(g, dx + 8, y0 + 40, dw - 16, 10, max ? 1f : (float) cur / need, j.color, Ui.darker(j.color, 0.6f),
                max ? "Niveau maximal" : Ui.fmt(cur) + " / " + Ui.fmt(need) + " XP");

        int ty = y0 + 60;
        Ui.header(g, dx + 8, ty, dw - 16, "BONUS ACTUELS");
        ty += 15;
        int shown = (int) Math.round(Job.doubleChance(lvl) * 100);
        String dbl = switch (j) {
            case MINEUR -> "Double minerai : " + shown + " %   ·   Vitesse de minage : +" + Math.round((Job.miningSpeed(lvl) - 1) * 100) + " %";
            case FERMIER -> "Double récolte : " + shown + " %   ·   Plats à effets : " + (Job.tier(lvl) > 0 ? "palier " + Job.tier(lvl) : "palier 10");
            case PECHEUR -> "Double prise : " + shown + " %   ·   Poissons rares : " + (Job.tier(lvl) > 0 ? "actifs" : "palier 10");
        };
        g.text(font, font.plainSubstrByWidth(dbl, dw - 16), dx + 8, ty, Ui.TEXT, false);
        ty += 16;
        Ui.header(g, dx + 8, ty, dw - 16, "PALIERS");
        ty += 15;
        int ch = Math.max(30, (dy(y0, dh) - ty) / 3 - 3);
        for (Job.Milestone m : j.milestones) {
            boolean ok = lvl >= m.level();
            int col = ok ? j.color : 0xFF6A6478;
            Ui.frame(g, dx + 8, ty, dw - 16, ch, col, hovered(dx + 8, ty, dw - 16, ch));
            g.text(font, (ok ? "✔ " : "🔒 ") + "Niveau " + m.level() + " — " + m.title(), dx + 14, ty + 4, ok ? Ui.GOLD_LIGHT : Ui.MUTED, false);
            int tw = dw - 28;
            g.text(font, font.plainSubstrByWidth(m.recipe() + "  ·  " + m.cosmetic(), tw), dx + 14, ty + 15, ok ? Ui.TEXT : Ui.MUTED, false);
            if (ch >= 38) g.text(font, font.plainSubstrByWidth("Bonus : " + m.bonus(), tw), dx + 14, ty + 26, ok ? Ui.GREEN : Ui.MUTED, false);
            ty += ch + 3;
        }
    }

    private static int dy(int y0, int dh) {
        return y0 + dh - 6;
    }
}
