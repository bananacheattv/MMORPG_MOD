package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.ShopConfig;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.economy.ShopPricing;
import com.mmorpg.item.RpgEquipment;
import com.mmorpg.item.RpgMaterialItem;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.Rarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Boutique d'un marchand : achat d'articles et vente du butin contre de l'or. */
public class ShopScreen extends MmoScreen {
    private final int npc;
    private final String npcName;
    private final ShopConfig shop;
    private boolean sellTab;
    private int scroll;

    public ShopScreen(CompoundTag data) {
        super("Marchand");
        this.npc = data.getIntOr("npc", -1);
        this.npcName = data.getStringOr("name", "Marchand");
        this.shop = ConfigManager.parseShop(data.getStringOr("shop", "{}"));
    }

    /** Ouvre directement l'onglet de vente. */
    public void showSellTab() {
        sellTab = true;
        scroll = 0;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, scroll - (int) Math.signum(sy));
        return true;
    }

    private static int rarityColor(Item item) {
        if (item instanceof RpgEquipment eq) return eq.rpgDef().rarity().color;
        if (item instanceof RpgMaterialItem m) return m.rarity().color;
        return Rarity.COMMUN.color;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, npcName.toUpperCase(java.util.Locale.ROOT), Ui.GOLD_LIGHT);
        String goldTxt = Ui.fmt(ClientData.DATA.gold) + " or";
        g.item(new ItemStack(ModItems.PIECE_OR.get()), left + pw - 26 - font.width(goldTxt), top + 4);
        g.text(font, goldTxt, left + pw - 8 - font.width(goldTxt), top + 9, 0xFFFFD040, true);
        int tw = 90;
        button(g, left + pw / 2 - tw - 2, top + 22, tw, 16, "Acheter", true, !sellTab ? 0xFF6A4A1A : 0xFF2A2420, () -> {
            sellTab = false;
            scroll = 0;
        });
        button(g, left + pw / 2 + 2, top + 22, tw, 16, "Vendre", true, sellTab ? 0xFF6A4A1A : 0xFF2A2420, () -> {
            sellTab = true;
            scroll = 0;
        });
        if (sellTab) renderSell(g);
        else renderBuy(g);
    }

    private void renderBuy(GuiGraphicsExtractor g) {
        int x0 = left + 8;
        int y0 = top + 44;
        int rowH = 22;
        int cols = 2;
        int cw = (pw - 16 - 6) / cols;
        int visibleRows = (top + ph - 8 - y0) / rowH;
        int totalRows = (shop.buy.size() + cols - 1) / cols;
        scroll = Math.min(scroll, Math.max(0, totalRows - visibleRows));
        long gold = ClientData.DATA.gold;
        int level = ClientData.DATA.level;
        for (int i = scroll * cols; i < shop.buy.size() && (i / cols - scroll) < visibleRows; i++) {
            ShopConfig.Offer o = shop.buy.get(i);
            Item item = ForgeRecipes.resolveItem(o.item);
            if (item == null || item == Items.AIR) continue;
            int cx = x0 + (i % cols) * (cw + 6);
            int ry = y0 + (i / cols - scroll) * rowH;
            ItemStack stack = new ItemStack(item, o.count);
            int color = rarityColor(item);
            boolean levelOk = level >= o.minLevel;
            Ui.frame(g, cx, ry, cw, rowH - 2, color, hovered(cx, ry, cw, rowH - 2));
            g.item(stack, cx + 2, ry + 2);
            g.itemDecorations(font, stack, cx + 2, ry + 2);
            if (hovered(cx + 2, ry + 2, 16, 16)) g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            String name = stack.getHoverName().getString();
            g.text(font, font.plainSubstrByWidth(name, cw - 100), cx + 21, ry + 2, levelOk ? color : Ui.MUTED, false);
            String price = o.price + " or" + (levelOk ? "" : "  (niv. " + o.minLevel + ")");
            g.text(font, price, cx + 21, ry + 11, gold >= o.price && levelOk ? 0xFFFFD040 : Ui.RED, false);
            final int index = i;
            button(g, cx + cw - 72, ry + 3, 36, 14, "x1", levelOk && gold >= o.price, 0xFF2A6A3A,
                    () -> ClientNet.send(new Payloads.ShopAction(Payloads.ShopAction.BUY, index, 1, npc)));
            button(g, cx + cw - 34, ry + 3, 30, 14, "x5", levelOk && gold >= (long) o.price * 5, 0xFF2A5A4A,
                    () -> ClientNet.send(new Payloads.ShopAction(Payloads.ShopAction.BUY, index, 5, npc)));
        }
    }

    private void renderSell(GuiGraphicsExtractor g) {
        int x0 = left + 8;
        int y0 = top + 44;
        Inventory inv = Minecraft.getInstance().player.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (ShopPricing.sellPrice(inv.getItem(i), shop) > 0) slots.add(i);
        }
        int rowH = 20;
        int visible = (top + ph - 8 - y0) / rowH;
        scroll = Math.min(scroll, Math.max(0, slots.size() - visible));
        if (slots.isEmpty()) {
            g.centeredText(font, Component.literal("Vous n'avez rien que le marchand souhaite acheter."), left + pw / 2, y0 + 20, Ui.MUTED);
            g.centeredText(font, Component.literal("Il rachète les matériaux, les équipements et certains minerais."), left + pw / 2, y0 + 32, Ui.MUTED);
        }
        int w = pw - 16;
        for (int i = 0; i < Math.min(visible, slots.size() - scroll); i++) {
            int slot = slots.get(i + scroll);
            ItemStack s = inv.getItem(slot);
            int price = ShopPricing.sellPrice(s, shop);
            int ry = y0 + i * rowH;
            int color = rarityColor(s.getItem());
            Ui.frame(g, x0, ry, w, rowH - 2, color, hovered(x0, ry, w, rowH - 2));
            g.item(s, x0 + 2, ry + 1);
            g.itemDecorations(font, s, x0 + 2, ry + 1);
            if (hovered(x0 + 2, ry + 1, 16, 16)) g.setTooltipForNextFrame(font, s, mouseX, mouseY);
            String p = price + " or / u.  •  " + (price * s.getCount()) + " or";
            int priceX = x0 + w - 150 - font.width(p);
            g.text(font, font.plainSubstrByWidth(s.getHoverName().getString(), priceX - (x0 + 22) - 6), x0 + 22, ry + 5, color, false);
            g.text(font, p, priceX, ry + 5, 0xFFFFD040, false);
            button(g, x0 + w - 142, ry + 2, 60, 14, "Vendre 1", true, 0xFF6A4A1A,
                    () -> ClientNet.send(new Payloads.ShopAction(Payloads.ShopAction.SELL, slot, 1, npc)));
            button(g, x0 + w - 78, ry + 2, 74, 14, "Tout vendre", true, 0xFF7A3A1A,
                    () -> ClientNet.send(new Payloads.ShopAction(Payloads.ShopAction.SELL_ALL, slot, 0, npc)));
        }
    }
}
