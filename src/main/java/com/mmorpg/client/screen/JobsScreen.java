package com.mmorpg.client.screen;

import com.mmorpg.MMORPG;
import com.mmorpg.client.ClientData;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.job.Job;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** Metiers : trois grandes cartes illustrees (Mineur, Fermier, Pecheur) ; un clic ouvre le detail du metier. */
public class JobsScreen extends MenuScreen {
    private static final Job[] ORDER = {Job.MINEUR, Job.FERMIER, Job.PECHEUR};
    @Override protected String backdrop() { return "professions_background"; }

    public JobsScreen() {
        super(Tab.METIERS);
    }

    static long xp(Job j) {
        return ClientData.DATA.jobXp.getOrDefault(j.id, 0L);
    }

    /** Illustration du metier (carte 512x512 dont seule la partie haute, sans le titre grave, est affichee). */
    static void art(GuiGraphicsExtractor g, Job j, int x, int y, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, MMORPG.id("textures/gui/jobs/" + j.id + ".png"), x, y, 0, 0, w, h, 512, 363, 512, 512);
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        int x0 = left + 8, y0 = contentTop, gap = 8;
        int cw = (pw - 16 - gap * 2) / 3;
        int ih = cw * 363 / 512;
        int ch = Math.min(ph - 54 - 10, ih + 62);
        for (int i = 0; i < ORDER.length; i++) {
            Job j = ORDER[i];
            int cx = x0 + i * (cw + gap);
            boolean hover = hovered(cx, y0, cw, ch);
            Ui.frame(g, cx, y0, cw, ch, j.color, hover);
            art(g, j, cx + 4, y0 + 4, cw - 8, ih - 4);
            Ui.border(g, cx + 4, y0 + 4, cw - 8, ih - 4, hover ? Ui.GOLD : Ui.LINE);
            int ty = y0 + ih + 6;
            g.centeredText(font, net.minecraft.network.chat.Component.literal(j.label), cx + cw / 2, ty, hover ? Ui.GOLD_LIGHT : j.color);
            int lvl = Job.levelFor(xp(j));
            g.centeredText(font, net.minecraft.network.chat.Component.literal("Niveau " + lvl + " / " + Job.MAX_LEVEL), cx + cw / 2, ty + 12, Ui.TEXT);
            float frac = lvl >= Job.MAX_LEVEL ? 1f : (float) (xp(j) - Job.totalFor(lvl)) / Job.xpToNext(lvl);
            Ui.bar(g, cx + 10, ty + 26, cw - 20, 6, frac, j.color, Ui.darker(j.color, 0.6f), null);
            final Job jj = j;
            click(cx, y0, cw, ch, () -> Minecraft.getInstance().gui.setScreen(new JobDetailScreen(jj)));
        }
        g.centeredText(font, net.minecraft.network.chat.Component.literal("Cliquez sur un métier pour voir son niveau, ses paliers et ses bonus"), left + pw / 2, y0 + ch + 6, Ui.MUTED);
    }
}
