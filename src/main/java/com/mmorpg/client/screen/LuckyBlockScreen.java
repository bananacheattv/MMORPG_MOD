package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class LuckyBlockScreen extends MmoScreen {
    private final BlockPos pos;
    private CompoundTag loot;
    private boolean requested;
    private int elapsed;
    private String error = "";
    /** Butins possibles envoyes par le serveur (config/mmorpg/butins.json, modifiable avec /mmorpg butins). */
    private final net.minecraft.nbt.ListTag table;
    private final String[] icons;
    public LuckyBlockScreen(CompoundTag data) {
        super("Lucky Block");
        pos = BlockPos.of(data.getLongOr("pos", 0));
        table = data.getListOrEmpty("table");
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < table.size(); i++) ids.add(table.getCompoundOrEmpty(i).getStringOr("item", "mmorpg:piece_or"));
        icons = ids.isEmpty() ? new String[]{"potion_soin", "pierre_amelioration", "piece_or", "charme_chance", "coffre_cosmetique"} : ids.toArray(new String[0]);
    }
    @Override protected int panelWidth() { return Math.min(width - 12, 540); }
    @Override protected int panelHeight() { return Math.min(height - 12, 290); }
    public void result(CompoundTag data) {
        if (data.getLongOr("pos", Long.MIN_VALUE) != pos.asLong() || !requested || loot != null) return;
        error = data.getStringOr("error", "");
        if (error.isEmpty()) { loot = data.getCompoundOrEmpty("loot"); elapsed = 0; }
    }
    @Override public void tick() { if (loot != null) elapsed++; }
    public void start() {
        if (requested) return;
        requested = true;
        ClientNet.send(new Payloads.LuckyRoll(pos));
    }
    @Override protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float partial) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 10, "✦ LUCKY BLOCK ✦", Ui.GOLD_LIGHT);
        int rw = (pw - 38) * 3 / 5, x = left + 12, y = top + 40;
        int right = x + rw + 14, infoW = left + pw - right - 12;
        boolean spinning = loot != null && elapsed < 80;
        boolean finished = loot != null && !spinning;
        int cw = (rw - 8) / 3, rh = Math.max(64, Math.min(104, ph - 130));
        for (int col = 0; col < 3; col++) {
            int cx = x + col * (cw + 4);
            Ui.inset(g, cx, y, cw, rh);
            g.enableScissor(cx + 2, y + 2, cx + cw - 2, y + rh - 2);
            boolean stopped = loot != null && elapsed >= 48 + col * 16;
            int frame = (int) ((elapsed + partial) / 3) + col * 7;
            float offset = spinning && !stopped ? ((elapsed + partial) % 3) / 3f * 30 : 0;
            for (int row = -1; row <= 1; row++) {
                String id = stopped && row == 0 ? loot.getStringOr("item", "piece_or") : icons[Math.floorMod(frame + row, icons.length)];
                var item = new ItemStack(ForgeRecipes.resolveItem(id));
                g.pose().pushMatrix();
                g.pose().translate(cx + cw / 2f - 12, y + rh / 2f - 12 + row * 32 + offset);
                g.pose().scale(1.5f, 1.5f); g.item(item, 0, 0); g.pose().popMatrix();
            }
            g.disableScissor();
            g.fill(cx + 1, y + rh / 2 - 17, cx + cw - 1, y + rh / 2 - 16, Ui.GOLD);
            g.fill(cx + 1, y + rh / 2 + 17, cx + cw - 1, y + rh / 2 + 18, Ui.GOLD);
        }
        Ui.header(g, right, y, infoW, "BUTINS POSSIBLES");
        int maxRows = Math.max(1, (rh + 20) / 21);
        for (int i = 0; i < Math.min(table.size(), maxRows); i++) {
            CompoundTag e = table.getCompoundOrEmpty(i);
            int iy = y + 15 + i * 21;
            ItemStack stack = new ItemStack(ForgeRecipes.resolveItem(e.getStringOr("item", "mmorpg:piece_or")));
            g.item(stack, right, iy);
            int min = e.getIntOr("min", 1), max = e.getIntOr("max", 1);
            String qty = (min == max ? String.valueOf(min) : min + "-" + max) + (e.getBooleanOr("gold", false) ? " or" : " ×");
            String name = e.getBooleanOr("gold", false) ? "Pièces d’or" : stack.getHoverName().getString();
            g.text(font, font.plainSubstrByWidth(name, infoW - 21), right + 20, iy, Ui.TEXT, false);
            int pm = e.getIntOr("permille", 0);
            g.text(font, qty + "   " + (pm / 10) + "," + (pm % 10) + " %", right + 20, iy + 9, Ui.GOLD, false);
        }
        if (table.size() > maxRows) g.text(font, "+" + (table.size() - maxRows) + " autres…", right, y + 15 + maxRows * 21, Ui.MUTED, false);
        String label = !requested ? "Ouvrir" : spinning ? "Passer l’animation" : finished ? "Fermer" : "Ouverture…";
        button(g, x, y + rh + 10, rw, 20, label, !requested || loot != null, 0xFF665020, () -> {
            if (!requested) start(); else if (spinning) elapsed = 80; else onClose();
        });
        int resultY = top + ph - 49;
        Ui.inset(g, x, resultY, pw - 24, 36);
        String title = !error.isEmpty() ? error : finished ? "RÉCOMPENSE OBTENUE" : requested ? "Les rouleaux tournent…" : "Un Lucky Block, une récompense.";
        g.centeredText(font, Component.literal(font.plainSubstrByWidth(title, pw - 40)), left + pw / 2, resultY + 5, error.isEmpty() ? Ui.GOLD : Ui.RED);
        if (finished) {
            long gold = loot.getLongOr("gold", 0);
            String reward = gold > 0 ? gold + " pièces d’or" : loot.getIntOr("count", 1) + " × " + new ItemStack(ForgeRecipes.resolveItem(loot.getStringOr("item", "piece_or"))).getHoverName().getString();
            g.centeredText(font, Component.literal(font.plainSubstrByWidth(reward, pw - 40)), left + pw / 2, resultY + 20, Ui.GREEN);
        }
    }
}
