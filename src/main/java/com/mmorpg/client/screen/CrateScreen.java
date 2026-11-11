package com.mmorpg.client.screen;

import com.mmorpg.block.crate.CrateTier;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.ReelBoard;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Interface d'une caisse : trois colonnes de lots (communs, rares, legendaires), cles possedees, bouton d'ouverture. */
public class CrateScreen extends MmoScreen {
    private final BlockPos pos;
    private final CrateTier tier;
    private final int keys;
    private final ReelBoard board;
    private int ticks;

    public CrateScreen(CompoundTag data) {
        super("Caisse");
        pos = BlockPos.of(data.getLongOr("pos", 0));
        CrateTier[] all = CrateTier.values();
        tier = all[Math.max(0, Math.min(all.length - 1, data.getIntOr("tier", 0)))];
        ListTag table = data.getListOrEmpty("table");
        keys = data.getIntOr("keys", 0);
        board = new ReelBoard(table);
    }

    @Override protected int designHeight(){return 380;}

    @Override public void tick() { ticks++; }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float partial) {
        int color = 0xFF000000 | tier.color;
        Ui.panel(g, left, top, pw, ph);
        int cx = left + pw / 2;
        ReelBoard.rings(g, left + 14, cx - 150, top + 17, color);
        ReelBoard.rings(g, cx + 150, left + pw - 40, top + 17, color);
        ReelBoard.banner(g, font, cx, top + 8, 270, tier.label.toUpperCase(java.util.Locale.ROOT), "Clé requise : " + tier.keyLabel, color);
        // ---- tableau des lots
        int bx = left + 18, by = top + 54, bw = pw - 36, bh = ph - 54 - 86;
        ReelBoard.Lot hover = board.render(g, font, bx, by, bw, bh, mx, my, ticks,
                (r, run) -> click(r[0], r[1], r[2], r[3], run));
        // ---- ligne d'information
        int iy = by + bh + 8;
        String hint = hover != null ? hover.name() + "  " + hover.qty() + "  ·  " + hover.chance()
                : keys > 0 ? "Survole un lot pour voir sa chance." : "Il te faut une " + tier.keyLabel + " pour ouvrir cette caisse.";
        g.centeredText(font, net.minecraft.network.chat.Component.literal(font.plainSubstrByWidth(hint, bw)), cx, iy,
                hover != null ? color : Ui.MUTED);
        // ---- boutons : ouvrir | cle | quantite
        int by2 = iy + 16, bh2 = 30;
        int wMain = bw * 38 / 100, wKey = bw * 38 / 100, wQty = bw - wMain - wKey - 16;
        button(g, bx, by2, wMain, bh2, keys > 0 ? "OUVRIR" : "AUCUNE CLÉ", keys > 0, color, () -> {
            ClientNet.send(new Payloads.CrateOpen(pos));
            onClose();
        });
        Ui.button(g, bx + wMain + 8, by2, wKey, bh2, tier.keyLabel.toUpperCase(java.util.Locale.ROOT), false, mx, my, color);
        Ui.inset(g, bx + wMain + wKey + 16, by2, wQty, bh2);
        Item keyItem = ForgeRecipes.resolveItem("mmorpg:" + tier.keyItem());
        if (keyItem != null) g.item(new ItemStack(keyItem), bx + wMain + wKey + 24, by2 + 7);
        g.text(font, "×" + keys, bx + wMain + wKey + 46, by2 + 11, keys > 0 ? Ui.GREEN : Ui.RED, true);
        button(g, left + pw - 18 - 110, by2 + bh2 + 6, 110, 16, board.showAll() ? "COLONNES" : "TOUS LES LOTS", true, color, board::toggleAll);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        board.scroll(-(int) Math.signum(sy));
        return true;
    }
}
