package com.mmorpg.client.screen;

import com.mmorpg.client.ui.Ui;
import com.mmorpg.job.Job;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Metiers : niveau, barre d'XP, paliers (10 / 25 / 50), recompenses et bonus de chaque metier. */
public class JobDetailScreen extends MenuScreen {
    private final Job job;

    public JobDetailScreen(Job job) {
        super(Tab.METIERS);
        this.job = job;
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        int x0 = left + 8, y0 = contentTop;
        Job j = job;
        int dx = x0, dw = left + pw - 8 - dx, dh = ph - 54 - 8;
        Ui.inset(g, dx, y0, dw, dh);
        long total = JobsScreen.xp(j);
        int lvl = Job.levelFor(total);
        JobsScreen.art(g, j, dx + 8, y0 + 8, 70, 50);
        Ui.border(g, dx + 8, y0 + 8, 70, 50, j.color);
        button(g, dx + dw - 8 - 76, y0 + 8, 76, 16, "← Métiers", true, Ui.GOLD, () -> MenuScreen.open(Tab.METIERS));
        Ui.scaledText(g, j.label, dx + 86, y0 + 8, 1.5f, j.color, false);
        g.text(font, font.plainSubstrByWidth(j.description, dw - 94), dx + 86, y0 + 24, Ui.MUTED, false);
        boolean max = lvl >= Job.MAX_LEVEL;
        long cur = max ? 0 : total - Job.totalFor(lvl), need = max ? 0 : Job.xpToNext(lvl);
        Ui.bar(g, dx + 86, y0 + 40, dw - 94, 10, max ? 1f : (float) cur / need, j.color, Ui.darker(j.color, 0.6f),
                "Niveau " + lvl + " / " + Job.MAX_LEVEL + (max ? "  ·  maximal" : "  ·  " + Ui.fmt(cur) + " / " + Ui.fmt(need) + " XP"));

        int ty = y0 + 63;
        Ui.header(g, dx + 8, ty, dw - 16, "BONUS ACTUELS");
        ty += 14;
        int shown = (int) Math.round(Job.doubleChance(lvl) * 100);
        String dbl = switch (j) {
            case MINEUR -> "Double minerai : " + shown + " %   ·   Vitesse de minage : +" + Math.round((Job.miningSpeed(lvl) - 1) * 100) + " %";
            case FERMIER -> "Double récolte : " + shown + " %   ·   Plats à effets : " + (Job.tier(lvl) > 0 ? "palier " + Job.tier(lvl) : "palier 10");
            case PECHEUR -> "Double prise : " + shown + " %   ·   Poissons rares : " + (Job.tier(lvl) > 0 ? "actifs" : "palier 10");
        };
        g.text(font, font.plainSubstrByWidth(dbl, dw - 16), dx + 8, ty, Ui.TEXT, false);
        ty += 14;
        Ui.header(g, dx + 8, ty, dw - 16, "PALIERS");
        ty += 14;
        int ch = Math.max(30, (dy(y0, dh) - ty) / 3 - 3);
        for (Job.Milestone m : j.milestones) {
            boolean ok = lvl >= m.level();
            int col = ok ? j.color : 0xFF6A6478;
            Ui.frame(g, dx + 8, ty, dw - 16, ch, col, hovered(dx + 8, ty, dw - 16, ch));
            g.text(font, (ok ? "✔ " : "🔒 ") + "Niveau " + m.level() + " — " + m.title(), dx + 14, ty + 3, ok ? Ui.GOLD_LIGHT : Ui.MUTED, false);
            int tw = dw - 28;
            g.text(font, font.plainSubstrByWidth(m.recipe() + "  ·  " + m.cosmetic(), tw), dx + 14, ty + 12, ok ? Ui.TEXT : Ui.MUTED, false);
            if (ch >= 29) g.text(font, font.plainSubstrByWidth("Bonus : " + m.bonus(), tw), dx + 14, ty + 21, ok ? Ui.GREEN : Ui.MUTED, false);
            ty += ch + 3;
        }
    }

    private static int dy(int y0, int dh) {
        return y0 + dh - 2;
    }
}
