package com.mmorpg.client.ui;

import com.mmorpg.crafting.ForgeRecipes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tableau de lots des caisses et Lucky Blocks : trois colonnes (communs, rares, legendaires, triees par chance),
 * trois cases visibles par colonne, fleches laterales, lignes de balayage. « Tous les lots » affiche la grille complete.
 */
public final class ReelBoard {
    public static final int[] COLORS = {0xFF4FA3FF, 0xFFFFD040, 0xFFB66CFF};
    private static final String[] LABELS = {"COMMUN", "RARE", "LÉGENDAIRE"};

    /** Lot survole : de quoi afficher son nom, sa quantite et sa chance. */
    public record Lot(ItemStack stack, boolean gold, int min, int max, float pct) {
        public String name() {
            return gold ? "Pièces d’or" : stack.getHoverName().getString();
        }

        public String qty() {
            return (min == max ? "×" + min : "×" + min + "-" + max);
        }

        public String chance() {
            return (pct < 1 ? String.format(java.util.Locale.ROOT, "%.1f", pct) : Integer.toString(Math.round(pct))) + " %";
        }
    }

    private final List<CompoundTag>[] cols;
    private final List<CompoundTag> sorted = new ArrayList<>();
    private final int[] off = new int[3];
    private boolean all;
    private int allScroll;

    @SuppressWarnings("unchecked")
    public ReelBoard(ListTag table) {
        cols = new List[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
        for (int i = 0; i < table.size(); i++) sorted.add(table.getCompoundOrEmpty(i));
        sorted.sort(Comparator.comparingInt((CompoundTag e) -> -e.getIntOr("permille", 0)));
        int n = sorted.size();
        for (int i = 0; i < n; i++) cols[Math.min(2, i * 3 / Math.max(1, n))].add(sorted.get(i));
        for (int c = 0; c < 3; c++) off[c] = 0;
    }

    public boolean showAll() {
        return all;
    }

    public void toggleAll() {
        all = !all;
        allScroll = 0;
    }

    public void scroll(int d) {
        if (all) allScroll = Math.max(0, allScroll + d);
        else step(d);
    }

    public void step(int d) {
        for (int c = 0; c < 3; c++) {
            int n = cols[c].size();
            if (n > 0) off[c] = Math.floorMod(off[c] + d, n);
        }
    }

    private static ItemStack stackOf(CompoundTag e) {
        Item item = ForgeRecipes.resolveItem(e.getStringOr("item", "mmorpg:piece_or"));
        return new ItemStack(item == null || item == Items.AIR ? Items.GOLD_NUGGET : item);
    }

    private static Lot lot(CompoundTag e) {
        return new Lot(stackOf(e), e.getBooleanOr("gold", false), e.getIntOr("min", 1), e.getIntOr("max", 1), e.getIntOr("permille", 0) / 10f);
    }

    private static void bigItem(GuiGraphicsExtractor g, ItemStack s, float cx, float cy, float scale) {
        g.pose().pushMatrix();
        g.pose().translate(cx - 8 * scale, cy - 8 * scale);
        g.pose().scale(scale, scale);
        g.item(s, 0, 0);
        g.pose().popMatrix();
    }

    private static void triangle(GuiGraphicsExtractor g, int x, int cy, int h, boolean pointLeft, int color) {
        for (int i = 0; i < h; i++) {
            int half = h / 2;
            int row = i;
            int w = Math.max(1, (int) (h * (1 - Math.abs(row - half) / (float) (half + 1))));
            if (pointLeft) g.fill(x + h - w, cy - half + row, x + h, cy - half + row + 1, color);
            else g.fill(x, cy - half + row, x + w, cy - half + row + 1, color);
        }
    }

    /** Bandeau de titre (nom + sous-titre) centre en haut du panneau. */
    public static void banner(GuiGraphicsExtractor g, Font font, int cx, int y, int w, String title, String sub, int color) {
        int x = cx - w / 2;
        Ui.rounded(g, x - 3, y - 3, w + 6, 38, 5, 0xFF0C0A1C);
        Ui.rounded(g, x, y, w, 32, 4, Ui.darker(color, .42f));
        Ui.border(g, x, y, w, 32, Ui.lighter(color, .35f));
        Ui.scaledText(g, title, cx, y + 4, 1.6f, 0xFFFFFFFF, true);
        Ui.scaledText(g, sub, cx, y + 21, .85f, Ui.lighter(color, .6f), true);
    }

    /** Pastilles decoratives (anneaux) de part et d'autre du bandeau. */
    public static void rings(GuiGraphicsExtractor g, int x0, int x1, int y, int color) {
        for (int x = x0, i = 0; x + 8 <= x1; x += 15, i++) {
            Ui.rounded(g, x, y, 9, 9, 4, i % 2 == 0 ? 0xFF0A0818 : Ui.lighter(color, .3f));
            Ui.rounded(g, x + 2, y + 2, 5, 5, 2, i % 2 == 0 ? 0xFF000000 : Ui.darker(color, .5f));
        }
    }

    /** Dessine le tableau dans (x, y, w, h) et renvoie le lot survole (ou null). */
    public Lot render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, int mx, int my, int ticks,
                      java.util.function.BiConsumer<int[], Runnable> hit) {
        if (all) return renderAll(g, font, x, y, w, h, mx, my);
        int cw = Math.min(124, (w - 120) / 3);
        int gap = Math.max(14, Math.min(26, (w - 3 * cw) / 6));
        int total = 3 * cw + 2 * gap;
        int sx = x + (w - total) / 2;
        int frameTop = y + 34, frameH = h - 34;
        int cs = Math.min(cw - 14, (frameH - 12) / 3 - 8);
        int pitch = cs + 10;
        int cy0 = frameTop + (frameH - (3 * cs + 2 * 10)) / 2;
        Lot hovered = null;
        for (int c = 0; c < 3; c++) {
            int cx = sx + c * (cw + gap);
            int color = COLORS[c];
            // etiquette + losange
            int lw = font.width(LABELS[c]) + 18;
            Ui.rounded(g, cx + (cw - lw) / 2, y, lw, 15, 3, 0xFF0C0A1C);
            Ui.border(g, cx + (cw - lw) / 2, y, lw, 15, color);
            g.text(font, LABELS[c], cx + (cw - lw) / 2 + 9, y + 4, color, false);
            Ui.diamond(g, cx + cw / 2, y + 25, 7, Ui.darker(color, .55f));
            Ui.diamond(g, cx + cw / 2, y + 25, 5, color);
            // colonne
            g.fill(cx, frameTop, cx + cw, frameTop + frameH, 0xFF0A0818);
            Ui.border(g, cx, frameTop, cw, frameH, color);
            g.fill(cx + 1, frameTop + 1, cx + cw - 1, frameTop + frameH - 1, Ui.withAlpha(Ui.darker(color, .22f) & 0xFFFFFF, 90));
            List<CompoundTag> list = cols[c];
            int n = list.size();
            for (int s = 0; s < 3; s++) {
                int ix = cx + (cw - cs) / 2, iy = cy0 + s * pitch;
                boolean centre = s == 1;
                CompoundTag e = n == 0 ? null : (n < 3 && s >= n ? null : list.get(Math.floorMod(off[c] + s - 1, n)));
                if (n < 3 && n > 0) e = s < n ? list.get(s) : null;
                boolean over = e != null && Ui.in(mx, my, ix, iy, cs, cs);
                int fill = centre ? 0xFF3A2A5E : 0xFF241A40;
                Ui.rounded(g, ix, iy, cs, cs, 3, over ? 0xFF4C3878 : fill);
                Ui.border(g, ix, iy, cs, cs, over || centre ? color : 0xFF6A3FC0);
                if (e == null) continue;
                bigItem(g, stackOf(e), ix + cs / 2f, iy + cs / 2f, Math.max(1f, (cs - 4) / 16f));
                if (over) hovered = lot(e);
            }
        }
        // lignes de balayage + fleches
        int ly2 = cy0 + pitch + cs / 2;
        g.fill(sx - 20, ly2, sx + total + 20, ly2 + 1, 0x60FFFFFF);
        g.fill(sx - 20, cy0 + cs / 2 + 0, sx + total + 20, cy0 + cs / 2 + 1, 0x30FFFFFF);
        int ay = cy0 + pitch + cs / 2;
        int lx = sx - 30, rx = sx + total + 14;
        boolean overL = Ui.in(mx, my, lx - 4, ay - 14, 26, 28), overR = Ui.in(mx, my, rx - 4, ay - 14, 26, 28);
        triangle(g, lx, ay, 16, true, overL ? 0xFFFFFFFF : 0xFFFFE6A0);
        triangle(g, rx, ay, 16, false, overR ? 0xFFFFFFFF : 0xFFFFE6A0);
        hit.accept(new int[]{lx - 4, ay - 14, 26, 28}, () -> step(-1));
        hit.accept(new int[]{rx - 4, ay - 14, 26, 28}, () -> step(1));
        return hovered;
    }

    private Lot renderAll(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, int mx, int my) {
        Ui.inset(g, x, y, w, h);
        int cell = 60, cols = Math.max(1, (w - 12) / cell), rows = Math.max(1, (h - 8) / (cell + 4));
        int total = sorted.size();
        allScroll = Math.max(0, Math.min(allScroll, Math.max(0, (total + cols - 1) / cols - rows)));
        int gx = x + (w - cols * cell) / 2;
        Lot hovered = null;
        for (int i = allScroll * cols; i < Math.min(total, (allScroll + rows) * cols); i++) {
            CompoundTag e = sorted.get(i);
            int c = (i - allScroll * cols) % cols, r = (i - allScroll * cols) / cols;
            int cx = gx + c * cell, cy = y + 6 + r * (cell + 4);
            boolean over = Ui.in(mx, my, cx, cy, cell - 4, cell);
            int color = COLORS[Math.min(2, i * 3 / Math.max(1, total))];
            Ui.frame(g, cx, cy, cell - 4, cell, color, over);
            bigItem(g, stackOf(e), cx + (cell - 4) / 2f, cy + 22, 2f);
            Lot l = lot(e);
            Ui.scaledText(g, l.gold() ? l.min() + "-" + l.max() + " or" : l.qty(), cx + (cell - 4) / 2f, cy + 41, .75f, Ui.TEXT, true);
            Ui.scaledText(g, l.chance(), cx + (cell - 4) / 2f, cy + 51, .75f, color, true);
            if (over) hovered = l;
        }
        return hovered;
    }
}
