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
    public LuckyBlockScreen(CompoundTag data) { super("Lucky Block"); pos = BlockPos.of(data.getLongOr("pos", 0)); }
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
        String[] icons = {"potion_soin", "pierre_amelioration", "piece_or", "charme_chance", "coffre_cosmetique"};
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
        String[] names = {"Potions", "Amélioration", "Pièces d’or", "Charmes", "Cosmétique"};
        int[] odds = {45,25,20,8,2};
        for (int i = 0; i < icons.length; i++) {
            int iy = y + 17 + i * 23;
            g.item(new ItemStack(ForgeRecipes.resolveItem(icons[i])), right, iy);
            g.text(font, font.plainSubstrByWidth(names[i], infoW - 21), right + 20, iy, Ui.TEXT, false);
            g.text(font, odds[i] + " %", right + 20, iy + 10, Ui.GOLD, false);
        }
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
