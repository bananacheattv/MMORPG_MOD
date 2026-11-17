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
    private final ListTag table;
    private CompoundTag loot;
    private boolean requested;
    private int elapsed, keysNow;
    private final com.mmorpg.client.ui.ItemPreview3D[] previews = {new com.mmorpg.client.ui.ItemPreview3D(), new com.mmorpg.client.ui.ItemPreview3D(), new com.mmorpg.client.ui.ItemPreview3D()};

    public CrateScreen(CompoundTag data) {
        super("Caisse");
        pos = BlockPos.of(data.getLongOr("pos", 0));
        CrateTier[] all = CrateTier.values();
        tier = all[Math.max(0, Math.min(all.length - 1, data.getIntOr("tier", 0)))];
        table = data.getListOrEmpty("table");
        keys = data.getIntOr("keys", 0);
        keysNow = keys;
        board = new ReelBoard(table);
    }

    @Override protected int designHeight(){return 380;}

    @Override public void tick() { ticks++; if (loot != null) elapsed++; }

    /** Resultat envoye par le serveur : la roue s'arrete sur le gain, sans quitter l'interface. */
    public void result(CompoundTag data) {
        if (data.getLongOr("pos", Long.MIN_VALUE) != pos.asLong() || !requested || loot != null) return;
        if (!data.getStringOr("error", "").isEmpty()) { requested = false; return; }
        loot = data.getCompoundOrEmpty("loot");
        keysNow = data.getIntOr("keys", 0);
        elapsed = 0;
    }

    public void previewResult() {
        if (System.getProperty("mmorpg.showcase", "").isEmpty()) throw new IllegalStateException("Private preview");
        requested = true;
        loot = new CompoundTag();
        loot.putString("item", "mmorpg:potion_soin_majeure");
        loot.putInt("count", 3);
        elapsed = 0;
    }

    private static ItemStack icon(String id) {
        Item it = ForgeRecipes.resolveItem(id);
        return new ItemStack(it == null ? net.minecraft.world.item.Items.GOLD_NUGGET : it);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float partial) {
        int color = 0xFF000000 | tier.color;
        Ui.panel(g, left, top, pw, ph);
        int cx = left + pw / 2;
        ReelBoard.rings(g, left + 14, cx - 150, top + 17, color);
        ReelBoard.rings(g, cx + 150, left + pw - 40, top + 17, color);
        ReelBoard.banner(g, font, cx, top + 8, 270, tier.label.toUpperCase(java.util.Locale.ROOT), "Clé requise : " + tier.keyLabel, color);
        int bx = left + 18, by = top + 54, bw = pw - 36, bh = ph - 54 - 86;
        boolean spinning = loot != null && elapsed < 80, finished = loot != null && !spinning;
        String hint;
        if (!requested) {
            ReelBoard.Lot hover = board.render(g, font, bx, by, bw, bh, mx, my, ticks,
                    (r, run) -> click(r[0], r[1], r[2], r[3], run));
            hint = hover != null ? hover.name() + "  " + hover.qty() + "  ·  " + hover.chance()
                    : keys > 0 ? "Survole un lot pour voir sa chance." : "Il te faut une " + tier.keyLabel + " pour ouvrir cette caisse.";
            g.centeredText(font, net.minecraft.network.chat.Component.literal(font.plainSubstrByWidth(hint, bw)), cx, by + bh + 8,
                    hover != null ? color : Ui.MUTED);
        } else {
            Ui.inset(g, bx, by, bw, bh);
            int cw = (bw - 16) / 3;
            for (int col = 0; col < 3; col++) {
                int sx = bx + 6 + col * (cw + 2);
                Ui.rounded(g, sx, by + 7, cw, bh - 14, 7, Ui.withAlpha(Ui.darker(color, .25f), 190));
                Ui.border(g, sx, by + 7, cw, bh - 14, Ui.alpha(color, .4f));
                boolean stopped = loot != null && elapsed >= 48 + col * 16;
                float p = loot == null ? 0 : Math.clamp((elapsed + partial) / (48f + col * 16), 0, 1);
                float turns = (float) ((1 - Math.pow(1 - p, 3)) * Math.PI * 20);
                ItemStack shown;
                if (loot == null) shown = icon("mmorpg:piece_or");
                else if (stopped) shown = icon(loot.getStringOr("item", "mmorpg:piece_or"));
                else {
                    int frame = (int) (turns / (Math.PI / 2)) + col * 7;
                    shown = table.isEmpty() ? icon("mmorpg:piece_or") : icon(table.getCompoundOrEmpty(Math.floorMod(frame, table.size())).getStringOr("item", "mmorpg:piece_or"));
                }
                previews[col].draw(g, shown, sx + 2, by + 9, cw - 4, bh - 18, loot == null ? (ticks + partial) * .12f : turns, stopped ? .3f : .25f);
                Ui.rounded(g, sx + cw / 2 - 9, by + bh - 12, 18, 2, 1, color);
            }
        }
        // ---- ligne d'information / resultat
        int iy = by + bh + 8;
        if (requested) {
            String title = finished ? "✦ TRÉSOR OBTENU ✦" : "La caisse s’ouvre…";
            String detail = finished ? (loot.getLongOr("gold", 0) > 0 ? loot.getLongOr("gold", 0) + " pièces d’or"
                    : loot.getIntOr("count", 1) + " × " + icon(loot.getStringOr("item", "mmorpg:piece_or")).getHoverName().getString()) : "";
            g.centeredText(font, net.minecraft.network.chat.Component.literal(font.plainSubstrByWidth(title + (detail.isEmpty() ? "" : "  ·  " + detail), bw)), cx, iy, finished ? Ui.GREEN : color);
        }
        // ---- boutons : ouvrir | cle | quantite
        int by2 = iy + 16, bh2 = 30;
        int wMain = bw * 38 / 100, wKey = bw * 38 / 100, wQty = bw - wMain - wKey - 16;
        boolean canAgain = finished && keysNow > 0;
        String label = !requested ? (keys > 0 ? "OUVRIR" : "AUCUNE CLÉ") : spinning ? "RÉVÉLER" : finished ? (canAgain ? "RÉOUVRIR" : "FERMER") : "…";
        button(g, bx, by2, wMain, bh2, label, !requested ? keys > 0 : loot != null, color, () -> {
            if (!requested) { requested = true; ClientNet.send(new Payloads.CrateOpen(pos)); }
            else if (spinning) elapsed = 80;
            else if (canAgain) { loot = null; requested = true; ClientNet.send(new Payloads.CrateOpen(pos)); }
            else onClose();
        });
        Ui.button(g, bx + wMain + 8, by2, wKey, bh2, tier.keyLabel.toUpperCase(java.util.Locale.ROOT), false, mx, my, color);
        Ui.inset(g, bx + wMain + wKey + 16, by2, wQty, bh2);
        Item keyItem = ForgeRecipes.resolveItem("mmorpg:" + tier.keyItem());
        if (keyItem != null) g.item(new ItemStack(keyItem), bx + wMain + wKey + 24, by2 + 7);
        g.text(font, "×" + keysNow, bx + wMain + wKey + 46, by2 + 11, keysNow > 0 ? Ui.GREEN : Ui.RED, true);
        if (!requested) button(g, left + pw - 18 - 110, by2 + bh2 + 6, 110, 16, board.showAll() ? "COLONNES" : "TOUS LES LOTS", true, color, board::toggleAll);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        board.scroll(-(int) Math.signum(sy));
        return true;
    }
}
