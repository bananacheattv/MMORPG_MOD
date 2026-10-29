package com.mmorpg.client.screen;

import com.mmorpg.achievement.Achievement;
import com.mmorpg.achievement.Achievements;
import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/** Succes par categorie et titres debloques (le titre equipe s'affiche apres le pseudo, dans le chat). */
public class AchievementsScreen extends MenuScreen {
    private static Achievement.Category selected = Achievement.Category.PROGRESSION;
    private static boolean titlesPage;
    private int scroll;
    /** Medaille du pack d'art par categorie (ordre de Achievement.Category). */
    private static final String[] MEDALS = {"medal_exploration", "medal_boss", "medal_exploration", "medal_arena", "medal_mining", "medal_crafting", "medal_harvest"};

    public AchievementsScreen() {
        super(Tab.SUCCES);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, scroll - (int) Math.signum(sy));
        return true;
    }

    private static void equip(String title) {
        var t = new CompoundTag();
        t.putString("action", "equip");
        t.putString("title", title);
        ClientNet.send(new Payloads.SocialAction("achievements", t));
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        var d = ClientData.DATA;
        int x0 = left + 8, y0 = contentTop, lw = 120;
        int done = d.achievements.size(), total = Achievement.all().size();
        Ui.header(g, x0, y0, lw, "SUCCÈS " + done + "/" + total);
        int ry = y0 + 18;
        for (var c : Achievement.Category.values()) {
            boolean sel = !titlesPage && c == selected;
            long n = Achievement.all().values().stream().filter(x -> x.category() == c && d.achievements.containsKey(x.id())).count();
            long all = Achievement.all().values().stream().filter(x -> x.category() == c).count();
            Ui.frame(g, x0, ry, lw, 20, c.color, sel || hovered(x0, ry, lw, 20));
            Ui.squareArt(g, MEDALS[c.ordinal()], 128, x0 + 3, ry + 2, 16);
            g.text(font, c.label, x0 + 23, ry + 6, sel ? Ui.GOLD_LIGHT : Ui.TEXT, false);
            String cnt = n + "/" + all;
            g.text(font, cnt, x0 + lw - 6 - font.width(cnt), ry + 6, n == all ? Ui.GREEN : Ui.MUTED, false);
            final var cc = c;
            click(x0, ry, lw, 20, () -> { selected = cc; titlesPage = false; scroll = 0; });
            ry += 23;
        }
        Ui.frame(g, x0, ry, lw, 20, Ui.GOLD, titlesPage || hovered(x0, ry, lw, 20));
        g.text(font, "Titres", x0 + 6, ry + 6, titlesPage ? Ui.GOLD_LIGHT : Ui.TEXT, false);
        click(x0, ry, lw, 20, () -> { titlesPage = true; scroll = 0; });

        int dx = x0 + lw + 10, dw = left + pw - 8 - dx, dh = ph - 54 - 10;
        Ui.inset(g, dx, y0, dw, dh);
        if (titlesPage) renderTitles(g, dx, y0, dw, dh);
        else renderList(g, dx, y0, dw, dh);
    }

    private void renderList(GuiGraphicsExtractor g, int dx, int y0, int dw, int dh) {
        var d = ClientData.DATA;
        g.text(font, selected.label, dx + 8, y0 + 6, selected.color, false);
        List<Achievement> list = new ArrayList<>();
        for (var a : Achievement.all().values()) if (a.category() == selected) list.add(a);
        int ch = 36, top = y0 + 20, visible = Math.max(1, (dh - 24) / (ch + 3));
        scroll = Math.min(scroll, Math.max(0, list.size() - visible));
        for (int i = 0; i < Math.min(visible, list.size() - scroll); i++) {
            Achievement a = list.get(i + scroll);
            int y = top + i * (ch + 3), w = dw - 16, x = dx + 8;
            boolean ok = d.achievements.containsKey(a.id());
            long value = Math.min(a.target(), d.counters.getOrDefault(a.stat(), 0L));
            Ui.frame(g, x, y, w, ch, ok ? selected.color : 0xFF6A6478, hovered(x, y, w, ch));
            g.text(font, (ok ? "✔ " : "") + a.name(), x + 6, y + 4, ok ? Ui.GOLD_LIGHT : Ui.TEXT, false);
            if (a.hasTitle()) {
                String t = "Titre : " + a.title();
                g.text(font, font.plainSubstrByWidth(t, w / 2 - 6), x + w - 6 - Math.min(font.width(t), w / 2 - 6), y + 4, ok ? 0xFFE8C36A : Ui.MUTED, false);
            }
            g.text(font, font.plainSubstrByWidth(a.description(), w - 12), x + 6, y + 15, Ui.MUTED, false);
            Ui.bar(g, x + 6, y + 26, w - 12, 6, (float) value / a.target(), ok ? Ui.GREEN : selected.color, Ui.darker(selected.color, 0.6f), null);
        }
        if (list.size() > visible) g.text(font, "↕ molette", dx + dw - 8 - font.width("↕ molette"), y0 + 6, Ui.MUTED, false);
    }

    private void renderTitles(GuiGraphicsExtractor g, int dx, int y0, int dw, int dh) {
        var d = ClientData.DATA;
        g.text(font, "Titres", dx + 8, y0 + 6, Ui.GOLD, false);
        String current = d.pvpTitle == null ? "" : d.pvpTitle;
        String cur = "Affiché : " + (current.isEmpty() ? "aucun" : "« " + current + " »");
        g.text(font, cur, dx + dw - 8 - font.width(cur), y0 + 6, Ui.GOLD_LIGHT, false);
        List<String> list = new ArrayList<>();
        list.add("");
        list.addAll(Achievements.titles(d));
        if (!current.isEmpty() && !list.contains(current)) list.add(current);        // titre PvP equipe
        int ch = 24, top = y0 + 20, visible = Math.max(1, (dh - 24) / (ch + 3));
        scroll = Math.min(scroll, Math.max(0, list.size() - visible));
        for (int i = 0; i < Math.min(visible, list.size() - scroll); i++) {
            String t = list.get(i + scroll);
            int y = top + i * (ch + 3), w = dw - 16, x = dx + 8;
            boolean on = t.equals(current);
            Ui.frame(g, x, y, w, ch, on ? Ui.GOLD : 0xFF6A6478, hovered(x, y, w, ch));
            g.text(font, t.isEmpty() ? "Aucun titre" : "« " + t + " »", x + 6, y + 8, on ? Ui.GOLD_LIGHT : Ui.TEXT, false);
            if (on) g.text(font, "✔ Équipé", x + w - 6 - font.width("✔ Équipé"), y + 8, Ui.GREEN, false);
            else click(x, y, w, ch, () -> equip(t));
        }
        if (list.size() == 1) g.text(font, "Débloquez des succès pour obtenir des titres.", dx + 8, top + ch + 8, Ui.MUTED, false);
    }
}
