package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.mine.Compression;
import com.mmorpg.mine.MineBlocks;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModItems;
import com.mmorpg.server.ForgeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Presse de compression : une ligne par minerai (lingots et or necessaires, stock actuel) et deux boutons de compression. */
public class CompressorScreen extends MmoScreen {
    private final BlockPos pos;

    public CompressorScreen(BlockPos pos) {
        super("Presse de compression");
        this.pos = pos;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "PRESSE DE COMPRESSION", Ui.GOLD_LIGHT);
        g.centeredText(font, Component.literal("Les armures et les outils exigent des minerais compressés : " + Compression.INGOTS + " lingots pour un bloc"),
                left + pw / 2, top + 19, Ui.MUTED);
        Inventory inv = Minecraft.getInstance().player.getInventory();
        int level = ClientData.DATA.level;
        long gold = ClientData.DATA.gold;
        String goldTxt = "Or : " + gold;
        g.item(new ItemStack(ModItems.PIECE_OR.get()), left + pw - 24 - font.width(goldTxt), top + 4);
        g.text(font, goldTxt, left + pw - 8 - font.width(goldTxt), top + 9, 0xFFFFD040, true);
        int x0 = left + 8;
        int rowH = 38;
        int rw = pw - 16;
        for (int i = 0; i < MineBlocks.ORES.size(); i++) {
            MineBlocks.Ore ore = MineBlocks.ORES.get(i);
            int ry = top + 32 + i * rowH;
            int color = 0xFF000000 | ore.color();
            boolean open = level >= ore.minLevel();
            int ingots = ForgeManager.count(inv, ModItems.get(ore.ingotId()));
            int cost = Compression.gold(ore.tier());
            boolean can = open && ingots >= Compression.INGOTS && gold + coins(inv) >= cost;
            Ui.frame(g, x0, ry, rw, rowH - 3, color, hovered(x0, ry, rw, rowH - 3));
            g.item(new ItemStack(ModItems.get(ore.ingotId())), x0 + 6, ry + 9);
            g.text(font, "→", x0 + 25, ry + 13, Ui.MUTED, false);
            g.item(new ItemStack(ModItems.get("compresse_" + ore.id())), x0 + 36, ry + 9);
            g.text(font, ore.name() + " compressée", x0 + 58, ry + 4, color, true);
            g.text(font, Compression.INGOTS + " lingots + " + cost + " pièces d'or", x0 + 58, ry + 15, Ui.TEXT, false);
            String have = ingots + " lingots en poche" + (open ? "" : "  ·  niveau " + ore.minLevel() + " requis");
            g.text(font, have, x0 + 58, ry + 26, open ? (ingots >= Compression.INGOTS ? Ui.GREEN : Ui.MUTED) : Ui.RED, false);
            int by = ry + 9;
            button(g, x0 + rw - 112, by, 50, 16, "Comprimer", can, 0xFF2A6A3A,
                    () -> ClientNet.send(new Payloads.ForgeAction(Payloads.ForgeAction.COMPRESS, ore.id(), 1, pos)));
            button(g, x0 + rw - 58, by, 50, 16, "x5", can, 0xFF2A5A4A,
                    () -> ClientNet.send(new Payloads.ForgeAction(Payloads.ForgeAction.COMPRESS, ore.id(), 5, pos)));
        }
    }

    /** Les pieces d'or en poche comptent aussi (le serveur les depose avant de payer). */
    private static long coins(Inventory inv) {
        long n = 0;
        for (ItemStack s : inv) if (s.is(ModItems.PIECE_OR.get())) n += s.getCount();
        return n;
    }
}
