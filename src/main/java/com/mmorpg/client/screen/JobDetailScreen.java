package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.job.Job;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/** Detail d'un metier : apercu (description, bonus), paliers (recompenses, titres equipables) et sources d'XP. */
public class JobDetailScreen extends MenuScreen {
    private static int page;
    private final Job job;

    public JobDetailScreen(Job job) {
        this(job, 0);
    }

    public JobDetailScreen(Job job, int startPage) {
        super(Tab.METIERS);
        this.job = job;
        page = startPage;
    }

    private static void send(Job job, int level) {
        CompoundTag t = new CompoundTag();
        t.putString("action", "title");
        t.putString("job", job == null ? "" : job.id);
        t.putInt("level", level);
        ClientNet.send(new Payloads.SocialAction("jobs", t));
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        int x0 = left + 8, y0 = contentTop, w = pw - 16, dh = ph - 54 - 8;
        Job j = job;
        long total = JobsScreen.xp(j);
        int lvl = Job.levelFor(total);
        Ui.inset(g, x0, y0, w, dh);
        JobsScreen.art(g, j, x0 + 8, y0 + 8, 70, 50);
        Ui.border(g, x0 + 8, y0 + 8, 70, 50, j.color);
        Ui.scaledText(g, j.label, x0 + 86, y0 + 8, 1.5f, j.color, false);
        button(g, x0 + w - 8 - 76, y0 + 8, 76, 16, "← Métiers", true, Ui.GOLD, () -> MenuScreen.open(Tab.METIERS));
        boolean max = lvl >= Job.MAX_LEVEL;
        long cur = max ? 0 : total - Job.totalFor(lvl), need = max ? 0 : Job.xpToNext(lvl);
        g.text(font, "Niveau " + lvl + " / " + Job.MAX_LEVEL, x0 + 86, y0 + 26, Ui.TEXT, false);
        Ui.bar(g, x0 + 86, y0 + 40, w - 94, 10, max ? 1f : (float) cur / need, j.color, Ui.darker(j.color, 0.6f),
                max ? "Niveau maximal" : Ui.fmt(cur) + " / " + Ui.fmt(need) + " XP");

        String[] names = j == Job.PECHEUR ? new String[]{"Aperçu", "Paliers", "Sources d'XP", "Journal de pêche"} : new String[]{"Aperçu", "Paliers", "Sources d'XP"};
        int tw = (w - 16 - 4 * (names.length - 1)) / names.length, ty = y0 + 64;
        if (page >= names.length) page = 0;
        for (int i = 0; i < names.length; i++) {
            final int pg = i;
            button(g, x0 + 8 + i * (tw + 4), ty, tw, 14, names[i], true, page == i ? Ui.GOLD : Ui.LINE, () -> page = pg);
        }
        int cy = ty + 20, cx = x0 + 8, cw = w - 16, bottom = y0 + dh - 4;
        switch (page) {
            case 0 -> overview(g, cx, cy, cw, bottom, lvl);
            case 1 -> rewards(g, cx, cy, cw, bottom, lvl);
            case 3 -> journal(g, cx, cy, cw, bottom, mx, my);
            default -> sources(g, cx, cy, cw, bottom);
        }
    }

    private void overview(GuiGraphicsExtractor g, int x, int y, int w, int bottom, int lvl) {
        y += Ui.wrap(g, job.description, x, y, w, Ui.MUTED) + 6;
        Ui.header(g, x, y, w, "BONUS ACTUELS");
        y += 15;
        for (String line : Job.bonusLines(job, lvl)) {
            g.text(font, font.plainSubstrByWidth(line, w), x, y, Ui.TEXT, false);
            y += 11;
        }
        Job.Reward next = job.rewards.stream().filter(r -> r.level() > lvl).findFirst().orElse(null);
        y += 3;
        String txt = next == null ? "Tous les paliers sont atteints." : "Prochain palier : niveau " + next.level() + " — « " + next.title() + " »";
        g.text(font, font.plainSubstrByWidth(txt, w), x, y, next == null ? Ui.GREEN : Ui.GOLD_LIGHT, false);
    }

    private void rewards(GuiGraphicsExtractor g, int x, int y, int w, int bottom, int lvl) {
        int rowH = Math.max(22, (bottom - y) / job.rewards.size());
        for (Job.Reward r : job.rewards) {
            boolean ok = lvl >= r.level();
            int col = ok ? job.color : 0xFF6A6478;
            Ui.frame(g, x, y, w, rowH - 2, col, hovered(x, y, w, rowH - 2));
            g.text(font, (ok ? "✔ " : "🔒 ") + "Niv. " + r.level() + " — " + r.title(), x + 5, y + 3, ok ? Ui.GOLD_LIGHT : Ui.MUTED, false);
            if (ok) {
                boolean on = r.title().equals(ClientData.DATA.pvpTitle);
                final int level = r.level();
                button(g, x + w - 4 - 60, y + 2, 60, 12, on ? "Retirer" : "Équiper", true, on ? Ui.GREEN : Ui.GOLD, () -> send(on ? null : job, on ? 0 : level));
            }
            g.text(font, font.plainSubstrByWidth(rewardLine(r), w - 10), x + 5, y + 13, ok ? Ui.TEXT : Ui.MUTED, false);
            y += rowH;
        }
    }

    private static String rewardLine(Job.Reward r) {
        StringBuilder sb = new StringBuilder();
        if (r.recipeLabel() != null) sb.append("Recette : ").append(r.recipeLabel()).append("  ·  ");
        if (r.cosmeticId() != null) {
            Cosmetic c = Cosmetic.byId(r.cosmeticId());
            sb.append("Cosmétique : ").append(c == null ? r.cosmeticId() : c.label).append("  ·  ");
        }
        if (r.itemId() != null) sb.append(r.itemCount()).append(" × ").append(net.minecraft.network.chat.Component.translatable(ModItems.get(r.itemId()).getDescriptionId()).getString()).append("  ·  ");
        if (r.gold() > 0) sb.append(r.gold()).append(" or  ·  ");
        String out = sb.toString();
        return out.endsWith("  ·  ") ? out.substring(0, out.length() - 5) : out;
    }

    private void sources(GuiGraphicsExtractor g, int x, int y, int w, int bottom) {
        Ui.header(g, x, y, w, "CE QUI DONNE DE L'EXPÉRIENCE");
        y += 15;
        List<Job.Source> list = Job.sources(job);
        int colW = (w - 8) / 2, rowH = 24;
        for (int i = 0; i < list.size(); i++) {
            Job.Source s = list.get(i);
            int cx = x + (i % 2) * (colW + 8), cy = y + (i / 2) * rowH;
            g.text(font, font.plainSubstrByWidth(s.label(), colW - 46), cx, cy, Ui.TEXT, false);
            if (s.xp() > 0) {
                String xp = "+" + s.xp() + " XP";
                g.text(font, xp, cx + colW - font.width(xp), cy, job.color, false);
            }
            g.text(font, font.plainSubstrByWidth(s.note(), colW), cx, cy + 10, Ui.MUTED, false);
        }
    }

    /** Journal de peche : une case par prise possible (poissons, coffres, dechets), grisee tant qu'elle n'a pas ete remontee. */
    private void journal(GuiGraphicsExtractor g, int x, int y, int w, int bottom, int mx, int my) {
        var all = com.mmorpg.fishing.FishCatalog.ALL;
        int found = 0;
        for (var e : all) if (ClientData.DATA.fishCaught.getOrDefault(e.id(), 0) > 0) found++;
        Ui.header(g, x, y, w, "JOURNAL DE PÊCHE — " + found + " / " + all.size() + " prises découvertes");
        y += 15;
        int cols = 9, cell = Math.min(w / cols, 36), rows = (all.size() + cols - 1) / cols;
        int gx = x + (w - cols * cell) / 2;
        cell = Math.max(24, Math.min(cell, (bottom - y - 12) / rows));
        java.util.List<net.minecraft.network.chat.Component> tip = null;
        for (int i = 0; i < all.size(); i++) {
            var e = all.get(i);
            int cx = gx + (i % cols) * cell, cy = y + (i / cols) * cell;
            int n = ClientData.DATA.fishCaught.getOrDefault(e.id(), 0);
            int col = n > 0 ? 0xFF000000 | e.rarity().rgb() : 0xFF4A4658;
            Ui.frame(g, cx, cy, cell - 2, cell - 2, col, hovered(cx, cy, cell - 2, cell - 2));
            var item = ModItems.get(e.id());
            if (item != null && n > 0) g.item(new net.minecraft.world.item.ItemStack(item), cx + (cell - 18) / 2, cy + 2);
            else g.centeredText(font, "?", cx + (cell - 2) / 2, cy + (cell - 10) / 2 - 1, 0xFF6A6478);
            if (n > 0) g.text(font, "×" + n, cx + 3, cy + cell - 12, Ui.MUTED, false);
            if (hovered(cx, cy, cell - 2, cell - 2)) {
                tip = new java.util.ArrayList<>();
                if (n > 0) {
                    tip.add(net.minecraft.network.chat.Component.translatable("item.mmorpg." + e.id()).withColor(e.rarity().rgb()));
                    tip.add(net.minecraft.network.chat.Component.literal(e.rarity().label + " · " + (e.fish() ? "Poisson" : e.kind() == com.mmorpg.fishing.FishCatalog.Kind.TRESOR ? "Coffre" : "Déchet")).withColor(0xFF9A9488));
                    tip.add(net.minecraft.network.chat.Component.literal("Remonté " + n + " fois").withColor(0xFFB9ABCF));
                    int best = ClientData.DATA.fishBest.getOrDefault(e.id(), 0);
                    if (best > 0) tip.add(net.minecraft.network.chat.Component.literal("Record : " + best + " cm").withColor(0xFFE2B85A));
                } else {
                    tip.add(net.minecraft.network.chat.Component.literal("Prise inconnue").withColor(0xFF9A9488));
                    String hab = e.habitats().size() >= com.mmorpg.fishing.FishCatalog.Habitat.values().length ? "partout"
                            : e.habitats().stream().map(hh -> hh.label).collect(java.util.stream.Collectors.joining(", "));
                    tip.add(net.minecraft.network.chat.Component.literal("Indice : " + hab + " · " + e.moment().label).withColor(0xFF7FD0F0));
                    tip.add(net.minecraft.network.chat.Component.literal("Pêcheur niveau " + e.minLevel()).withColor(0xFF9A9488));
                }
            }
        }
        if (tip != null) g.setComponentTooltipForNextFrame(font, tip, mx, my);
    }
}
