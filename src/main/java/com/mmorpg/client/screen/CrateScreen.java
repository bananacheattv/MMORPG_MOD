package com.mmorpg.client.screen;

import com.mmorpg.block.crate.CrateTier;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Interface d'une caisse : contenu possible (grandes icones + chances), cles possedees, bouton d'ouverture. */
public class CrateScreen extends MmoScreen {
    private final BlockPos pos;
    private final CrateTier tier;
    private final ListTag table;
    private final int keys;
    private int scroll;

    public CrateScreen(CompoundTag data) {
        super("Caisse");
        pos = BlockPos.of(data.getLongOr("pos", 0));
        CrateTier[] all = CrateTier.values();
        tier = all[Math.max(0, Math.min(all.length - 1, data.getIntOr("tier", 0)))];
        table = data.getListOrEmpty("table");
        keys = data.getIntOr("keys", 0);
    }

    @Override protected int panelWidth() { return Math.min(width - 12, 640); }
    @Override protected int panelHeight() { return Math.min(height - 12, 380); }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float partial) {
        int color = 0xFF000000 | tier.color;
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 10, "✦ " + tier.label.toUpperCase(java.util.Locale.ROOT) + " ✦", color);
        // ---- contenu (grille de grandes icones)
        int gx = left + 14, gy = top + 36, gw = pw - 210, gh = ph - 50;
        Ui.inset(g, gx, gy, gw, gh);
        g.text(font, "Contenu possible", gx + 8, gy + 6, Ui.MUTED, false);
        int cell = 64, cols = Math.max(1, (gw - 12) / cell), rows = Math.max(1, (gh - 24) / (cell + 4));
        int total = table.size();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, (total + cols - 1) / cols - rows)));
        for (int i = scroll * cols; i < Math.min(total, (scroll + rows) * cols); i++) {
            CompoundTag e = table.getCompoundOrEmpty(i);
            int c = (i - scroll * cols) % cols, r = (i - scroll * cols) / cols;
            int cx = gx + 6 + c * cell, cy = gy + 20 + r * (cell + 4);
            boolean hover = Ui.in(mx, my, cx, cy, cell - 4, cell);
            Ui.frame(g, cx, cy, cell - 4, cell, hover ? color : Ui.GOLD_DARK, hover);
            Item item = ForgeRecipes.resolveItem(e.getStringOr("item", "mmorpg:piece_or"));
            ItemStack stack = new ItemStack(item == null || item == Items.AIR ? Items.GOLD_NUGGET : item);
            g.pose().pushMatrix();
            g.pose().translate(cx + (cell - 4) / 2f - 16, cy + 6);
            g.pose().scale(2f, 2f);
            g.item(stack, 0, 0);
            g.pose().popMatrix();
            int min = e.getIntOr("min", 1), max = e.getIntOr("max", 1);
            String qty = e.getBooleanOr("gold", false) ? min + "-" + max + " or" : "×" + (min == max ? min : min + "-" + max);
            float pct = e.getIntOr("permille", 0) / 10f;
            Ui.scaledText(g, qty, cx + (cell - 4) / 2f, cy + 41, .75f, Ui.TEXT, true);
            Ui.scaledText(g, (pct < 1 ? String.format(java.util.Locale.ROOT, "%.1f", pct) : Integer.toString(Math.round(pct))) + " %",
                    cx + (cell - 4) / 2f, cy + 51, .75f, pct < 3 ? 0xFFFF9C2A : pct < 10 ? 0xFFB66CFF : 0xFF80E080, true);
            if (hover) g.setTooltipForNextFrame(font, stack, mx, my);
        }
        // ---- panneau d'ouverture
        int px = left + pw - 186, py = top + 36, pwid = 172;
        Ui.inset(g, px, py, pwid, gh);
        Item keyItem = ForgeRecipes.resolveItem("mmorpg:" + tier.keyItem());
        if (keyItem != null) {
            g.pose().pushMatrix();
            g.pose().translate(px + pwid / 2f - 24, py + 14);
            g.pose().scale(3f, 3f);
            g.item(new ItemStack(keyItem), 0, 0);
            g.pose().popMatrix();
        }
        Ui.scaledText(g, tier.keyLabel, px + pwid / 2f, py + 70, 1f, color, true);
        Ui.scaledText(g, "Vous en avez : " + keys, px + pwid / 2f, py + 86, 1f, keys > 0 ? 0xFF80E080 : 0xFFFF6060, true);
        Ui.wrap(g, "Une roue apparaît au-dessus de la caisse et s'arrête sur votre gain.", px + 10, py + 108, pwid - 20, Ui.MUTED);
        button(g, px + 10, py + gh - 44, pwid - 20, 30, keys > 0 ? "OUVRIR" : "CLÉ REQUISE", keys > 0, color, () -> {
            ClientNet.send(new Payloads.CrateOpen(pos));
            onClose();
        });
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, scroll - (int) Math.signum(sy));
        return true;
    }
}
