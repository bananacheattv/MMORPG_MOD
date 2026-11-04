package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.economy.Auction;
import com.mmorpg.economy.Mail;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Boite aux lettres : relever son courrier (or et objets) et ecrire une lettre a un joueur avec de l'or et jusqu'a quatre objets. */
public class MailScreen extends MmoScreen {
    @Override protected String backdrop() { return "auction_header"; }

    private record Row(long id, String from, boolean system, String message, long gold, long sent, List<ItemStack> items) {
    }

    private static int tab = 0;
    private BlockPos pos;
    private final List<Row> rows = new ArrayList<>();
    private long wallet;
    private int inboxPage;
    private EditBox to, message, gold;
    private final List<Integer> picked = new ArrayList<>();
    private int inventoryPage;

    public MailScreen(CompoundTag data) {
        super("Boîte aux lettres");
        receive(data);
    }

    public void previewTab(int value) {
        tab = Math.clamp(value, 0, 1);
    }

    public void receive(CompoundTag data) {
        this.pos = BlockPos.of(data.getLongOr("pos", 0));
        this.wallet = data.getLongOr("wallet", 0);
        rows.clear();
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
        ListTag list = data.getListOrEmpty("letters");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompoundOrEmpty(i);
            List<ItemStack> items = new ArrayList<>();
            ListTag its = e.getListOrEmpty("items");
            for (int k = 0; k < its.size(); k++) items.add(ItemStack.OPTIONAL_CODEC.parse(ops, its.get(k)).result().orElse(ItemStack.EMPTY));
            rows.add(new Row(e.getLongOr("id", 0), e.getStringOr("from", "?"), e.getBooleanOr("system", false), e.getStringOr("message", ""),
                    e.getLongOr("gold", 0), e.getLongOr("sent", 0), items));
        }
    }

    @Override
    protected void init() {
        super.init();
        to = new EditBox(this.font, left + 12, top + 66, 160, 16, Component.literal("Destinataire"));
        to.setHint(Component.literal("Pseudo du destinataire").withColor(0xFF807868));
        to.setMaxLength(16);
        this.addRenderableWidget(to);
        gold = new EditBox(this.font, left + 180, top + 66, 110, 16, Component.literal("Or"));
        gold.setHint(Component.literal("Or joint (facultatif)").withColor(0xFF807868));
        gold.setMaxLength(9);
        gold.setResponder(s -> {
            if (!s.matches("\\d*")) gold.setValue(s.replaceAll("\\D", ""));
        });
        this.addRenderableWidget(gold);
        message = new EditBox(this.font, left + 12, top + 92, pw - 24, 16, Component.literal("Message"));
        message.setHint(Component.literal("Message (140 caractères)").withColor(0xFF807868));
        message.setMaxLength(Mail.MESSAGE_MAX);
        this.addRenderableWidget(message);
    }

    private void send(int action, long amount, String text) {
        ClientNet.send(new Payloads.ServiceAction(Payloads.ServiceAction.MAIL, action, amount, text, 0, pos));
    }

    private static String ago(long sent) {
        long m = Math.max(0, System.currentTimeMillis() - sent) / 60_000L;
        if (m < 60) return "il y a " + Math.max(1, m) + " min";
        if (m < 1440) return "il y a " + m / 60 + " h";
        return "il y a " + m / 1440 + " j";
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "BOÎTE AUX LETTRES", Ui.GOLD_LIGHT);
        String[] names = {"Courrier reçu (" + rows.size() + ")", "Écrire une lettre"};
        for (int i = 0; i < 2; i++) {
            final int t = i;
            button(g, left + 8 + i * 123, top + 24, 120, 16, names[i], true, tab == i ? 0xFFE2B85A : 0xFF3B3350, () -> tab = t);
        }
        String goldTxt = "Or : " + wallet;
        g.item(new ItemStack(ModItems.PIECE_OR.get()), left + pw - 24 - font.width(goldTxt), top + 4);
        g.text(font, goldTxt, left + pw - 8 - font.width(goldTxt), top + 9, 0xFFFFD040, true);
        to.visible = gold.visible = message.visible = tab == 1;
        if (tab == 0) renderInbox(g);
        else renderWrite(g);
    }

    private void renderInbox(GuiGraphicsExtractor g) {
        boolean any = !rows.isEmpty();
        button(g, left + pw - 8 - 150, top + 24, 150, 16, "Tout récupérer", any, 0xFF2A6A3A, () -> send(Mail.ACT_CLAIM_ALL, 0, ""));
        int ch = 38, y0 = top + 48, fit = Math.max(1, (ph - 48 - 28) / ch);
        int pages = Math.max(1, (rows.size() + fit - 1) / fit);
        inboxPage = Math.clamp(inboxPage, 0, pages - 1);
        if (rows.isEmpty()) g.centeredText(font, Component.literal("Votre boîte aux lettres est vide."), left + pw / 2, y0 + 40, Ui.MUTED);
        for (int i = inboxPage * fit; i < Math.min(rows.size(), (inboxPage + 1) * fit); i++) {
            Row r = rows.get(i);
            int x = left + 8, y = y0 + (i - inboxPage * fit) * ch, w = pw - 16, h = ch - 2;
            boolean hov = hovered(x, y, w, h);
            Ui.frame(g, x, y, w, h, r.system ? 0xFF8A7A4A : 0xFF5A5470, hov);
            int bw = 70, ix = x + 6;
            g.text(font, font.plainSubstrByWidth((r.system ? "✦ " : "✉ ") + r.from + " · " + ago(r.sent), w - bw - 20), ix, y + 4, Ui.TEXT, false);
            if (!r.message.isEmpty()) g.text(font, font.plainSubstrByWidth(r.message, w - bw - 20), ix, y + 15, Ui.MUTED, false);
            int cx = ix;
            if (r.gold > 0) {
                g.item(new ItemStack(ModItems.PIECE_OR.get()), cx, y + h - 17);
                String gt = r.gold + " or";
                g.text(font, gt, cx + 18, y + h - 12, 0xFFFFD040, true);
                cx += 22 + font.width(gt);
            }
            for (ItemStack s : r.items) {
                if (s.isEmpty()) continue;
                g.item(s, cx, y + h - 17);
                g.itemDecorations(font, s, cx, y + h - 17);
                if (hovered(cx, y + h - 17, 16, 16)) g.setTooltipForNextFrame(font, s, mouseX, mouseY);
                cx += 18;
            }
            button(g, x + w - 6 - bw, y + (h - 16) / 2, bw, 16, "Récupérer", true, 0xFF2A6A3A, () -> send(Mail.ACT_CLAIM, r.id, ""));
        }
        int py = top + ph - 18;
        button(g, left + pw / 2 - 70, py, 40, 14, "◀", inboxPage > 0, 0xFF3B3350, () -> inboxPage--);
        g.centeredText(font, Component.literal("Page " + (inboxPage + 1) + " / " + pages), left + pw / 2, py + 3, Ui.TEXT);
        button(g, left + pw / 2 + 30, py, 40, 14, "▶", inboxPage + 1 < pages, 0xFF3B3350, () -> inboxPage++);
    }

    private void renderWrite(GuiGraphicsExtractor g) {
        g.text(font, "DESTINATAIRE", left + 12, top + 55, Ui.GOLD, false);
        g.text(font, "OR JOINT", left + 180, top + 55, Ui.GOLD, false);
        var player = Minecraft.getInstance().player;
        var bags = com.mmorpg.server.Backpacks.decode(com.mmorpg.client.ClientData.DATA.backpacks, player.level().registryAccess());
        int size = 36 + com.mmorpg.server.Backpacks.capacity(bags), invPages = (size + 35) / 36;
        inventoryPage = Math.clamp(inventoryPage, 0, invPages - 1);
        java.util.function.IntFunction<ItemStack> stackAt = slot -> slot < 36 ? player.getInventory().getItem(slot) : bags.getItem(slot - 34);
        int x0 = left + 8, cell = 18, gw = 9 * cell + 14, y0 = top + 110;
        Ui.inset(g, x0, y0, gw, 4 * cell + 26);
        g.text(font, "OBJETS À JOINDRE (" + picked.size() + "/" + Mail.MAX_ITEMS + ")", x0 + 7, y0 + 5, Ui.GOLD, false);
        if (invPages > 1) {
            button(g, x0 + gw - 38, y0 + 3, 15, 13, "‹", inventoryPage > 0, Ui.GOLD, () -> inventoryPage--);
            button(g, x0 + gw - 21, y0 + 3, 15, 13, "›", inventoryPage + 1 < invPages, Ui.GOLD, () -> inventoryPage++);
        }
        for (int i = 0; i < 36 && inventoryPage * 36 + i < size; i++) {
            int slot = inventoryPage == 0 ? (i < 27 ? i + 9 : i - 27) : inventoryPage * 36 + i;
            int sx = x0 + 7 + (i % 9) * cell, sy = y0 + 18 + (i / 9) * cell;
            ItemStack stack = stackAt.apply(slot);
            Ui.itemBox(g, font, stack, sx, sy, cell - 2);
            if (picked.contains(slot)) Ui.border(g, sx, sy, cell - 2, cell - 2, Ui.GOLD);
            if (!stack.isEmpty()) {
                click(sx, sy, cell - 2, cell - 2, () -> {
                    if (picked.contains(slot)) picked.remove((Integer) slot);
                    else if (picked.size() < Mail.MAX_ITEMS) picked.add(slot);
                });
                if (hovered(sx, sy, cell - 2, cell - 2)) g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }
        int dx = x0 + gw + 8, dw = left + pw - 8 - dx;
        Ui.inset(g, dx, y0, dw, 4 * cell + 26);
        g.text(font, "JOINTS À LA LETTRE", dx + 7, y0 + 5, Ui.GOLD, false);
        for (int i = 0; i < picked.size(); i++) {
            ItemStack s = stackAt.apply(picked.get(i));
            g.item(s, dx + 7, y0 + 20 + i * 20);
            g.itemDecorations(font, s, dx + 7, y0 + 20 + i * 20);
            g.text(font, font.plainSubstrByWidth(s.getHoverName().getString(), dw - 34), dx + 27, y0 + 25 + i * 20, Ui.TEXT, false);
        }
        long value = 0;
        try {
            value = gold.getValue().isEmpty() ? 0 : Long.parseLong(gold.getValue());
        } catch (NumberFormatException ignored) {
        }
        final long amount = value;
        String who = to.getValue().strip();
        boolean can = !who.isEmpty() && amount <= Mail.MAX_GOLD && amount <= wallet && (amount > 0 || !picked.isEmpty() || !message.getValue().isBlank());
        String label = can ? "Envoyer la lettre" : amount > wallet ? "Or insuffisant" : "Indiquez un destinataire et un contenu";
        button(g, left + 8, top + ph - 26, pw - 16, 20, label, can, 0xFF2A8A4A, () -> {
            StringBuilder sel = new StringBuilder();
            for (int slot : picked) {
                if (sel.length() > 0) sel.append(',');
                sel.append(slot).append(':').append(Auction.saleFingerprint(stackAt.apply(slot), player.level().registryAccess()));
            }
            send(Mail.ACT_SEND, amount, who + Mail.SEP + message.getValue() + Mail.SEP + sel);
            picked.clear();
            gold.setValue("");
            message.setValue("");
        });
    }
}
