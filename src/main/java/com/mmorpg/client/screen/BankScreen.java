package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.economy.Bank;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Banque d'Aeloria : solde d'or (depot, retrait) et trois pages de coffre. */
public class BankScreen extends MmoScreen {
    @Override protected String backdrop() { return "storage_header"; }

    private BlockPos pos;
    private long bank;
    private long wallet;
    private int level;

    public BankScreen(CompoundTag data) {
        super("Banque d'Aeloria");
        receive(data);
    }

    public void receive(CompoundTag data) {
        this.pos = BlockPos.of(data.getLongOr("pos", 0));
        this.bank = data.getLongOr("bank", 0);
        this.wallet = data.getLongOr("wallet", 0);
        this.level = data.getIntOr("level", 1);
    }

    private void send(int action, long amount, int page) {
        ClientNet.send(new Payloads.ServiceAction(Payloads.ServiceAction.BANK, action, amount, "", page, pos));
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "BANQUE D'AELORIA", Ui.GOLD_LIGHT);
        g.centeredText(font, Component.literal("Votre or en banque ne se perd jamais, même à la mort"), left + pw / 2, top + 19, Ui.MUTED);
        int x0 = left + 8;
        int y0 = top + 34;
        int half = (pw - 24) / 2;
        // portefeuille et solde
        Ui.inset(g, x0, y0, half, 46);
        g.item(new ItemStack(ModItems.PIECE_OR.get()), x0 + 8, y0 + 6);
        g.text(font, "PORTE-MONNAIE", x0 + 30, y0 + 6, Ui.MUTED, false);
        g.text(font, wallet + " or", x0 + 30, y0 + 20, 0xFFFFD040, true);
        Ui.inset(g, x0 + half + 8, y0, half, 46);
        g.item(new ItemStack(ModItems.PIECE_OR.get()), x0 + half + 16, y0 + 6);
        g.text(font, "SOLDE EN BANQUE", x0 + half + 38, y0 + 6, Ui.MUTED, false);
        g.text(font, bank + " or", x0 + half + 38, y0 + 20, 0xFF7CE08A, true);
        // depot et retrait
        int by = y0 + 56;
        Ui.header(g, x0, by, half, "DÉPOSER");
        Ui.header(g, x0 + half + 8, by, half, "RETIRER");
        long[] steps = {100, 1000, 10000, -1};
        String[] names = {"100", "1 000", "10 000", "Tout"};
        int bw = (half - 9) / 4;
        for (int i = 0; i < 4; i++) {
            final long amount = steps[i];
            button(g, x0 + i * (bw + 3), by + 18, bw, 18, names[i], wallet > 0, 0xFF2A6A3A, () -> send(Bank.ACT_DEPOSIT, amount, 0));
            button(g, x0 + half + 8 + i * (bw + 3), by + 18, bw, 18, names[i], bank > 0, 0xFF6A4A2A, () -> send(Bank.ACT_WITHDRAW, amount, 0));
        }
        // coffre
        int cy = by + 48;
        Ui.header(g, x0, cy, pw - 16, "COFFRE");
        int cw = (pw - 16 - 12) / 3;
        for (int i = 0; i < 3; i++) {
            final int page = i;
            boolean open = level >= Bank.PAGE_LEVEL[i];
            int cx = x0 + i * (cw + 6);
            Ui.frame(g, cx, cy + 18, cw, 38, open ? 0xFFC8A040 : 0xFF50484A, hovered(cx, cy + 18, cw, 38) && open);
            g.text(font, "Coffre " + (i + 1), cx + 8, cy + 24, open ? Ui.GOLD_LIGHT : Ui.MUTED, true);
            g.text(font, open ? "54 emplacements" : "Niveau " + Bank.PAGE_LEVEL[i] + " requis", cx + 8, cy + 38, open ? Ui.TEXT : Ui.RED, false);
            if (open) click(cx, cy + 18, cw, 38, () -> send(Bank.ACT_OPEN_PAGE, 0, page));
        }
    }
}
