package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.economy.Auction;
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

/** Hotel des ventes : parcourir et acheter, vendre l'objet en main, gerer ses annonces et recuperer l'or et les objets au comptoir. */
public class AuctionScreen extends MmoScreen {
    private record Row(long id, String seller, long price, long left, ItemStack stack) {
    }

    private static int tab = 0;
    private BlockPos pos;
    private final List<Row> rows = new ArrayList<>();
    private final List<Row> mine = new ArrayList<>();
    private int page;
    private int pages = 1;
    private int total;
    private String query = "";
    private long earnings;
    private int returns;
    private long wallet;
    private EditBox search;
    private EditBox price;

    public AuctionScreen(CompoundTag data) {
        super("Hôtel des ventes");
        receive(data);
    }

    private static List<Row> decode(ListTag list) {
        List<Row> out = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        if (level == null) return out;
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompoundOrEmpty(i);
            ItemStack st = ItemStack.OPTIONAL_CODEC.parse(ops, e.get("stack")).result().orElse(ItemStack.EMPTY);
            out.add(new Row(e.getLongOr("id", 0), e.getStringOr("seller", "?"), e.getLongOr("price", 0), e.getLongOr("left", 0), st));
        }
        return out;
    }

    public void receive(CompoundTag data) {
        this.pos = BlockPos.of(data.getLongOr("pos", 0));
        this.page = data.getIntOr("page", 0);
        this.pages = data.getIntOr("pages", 1);
        this.total = data.getIntOr("total", 0);
        this.query = data.getStringOr("query", "");
        this.earnings = data.getLongOr("earnings", 0);
        this.returns = data.getIntOr("returns", 0);
        this.wallet = data.getLongOr("wallet", 0);
        rows.clear();
        rows.addAll(decode(data.getListOrEmpty("listings")));
        mine.clear();
        mine.addAll(decode(data.getListOrEmpty("mine")));
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(this.font, left + 10, top + 46, 190, 14, Component.literal("Recherche"));
        search.setHint(Component.literal("Rechercher un objet...").withColor(0xFF807868));
        search.setMaxLength(40);
        search.setValue(query);
        this.addRenderableWidget(search);
        price = new EditBox(this.font, left + 20, top + 140, 120, 16, Component.literal("Prix"));
        price.setHint(Component.literal("Prix total (or)").withColor(0xFF807868));
        price.setMaxLength(9);
        price.setResponder(s -> {
            if (!s.matches("\\d*")) price.setValue(s.replaceAll("\\D", ""));
        });
        this.addRenderableWidget(price);
    }

    private void send(int action, long amount, String text, int pg) {
        ClientNet.send(new Payloads.ServiceAction(Payloads.ServiceAction.AUCTION, action, amount, text, pg, pos));
    }

    private static String timeLeft(long ms) {
        long h = ms / 3_600_000L;
        return h >= 1 ? h + " h" : Math.max(1, ms / 60_000L) + " min";
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "HÔTEL DES VENTES", Ui.GOLD_LIGHT);
        String[] names = {"Acheter", "Vendre", "Mes annonces"};
        int tw = 74;
        for (int i = 0; i < 3; i++) {
            final int t = i;
            button(g, left + 8 + i * (tw + 3), top + 24, tw, 16, names[i], true, tab == i ? 0xFFE2B85A : 0xFF3B3350, () -> tab = t);
        }
        String goldTxt = "Or : " + wallet;
        g.item(new ItemStack(ModItems.PIECE_OR.get()), left + pw - 24 - font.width(goldTxt), top + 4);
        g.text(font, goldTxt, left + pw - 8 - font.width(goldTxt), top + 9, 0xFFFFD040, true);
        boolean waiting = earnings > 0 || returns > 0;
        String claim = waiting ? "Comptoir : " + earnings + " or" + (returns > 0 ? " + " + returns + " objet(s)" : "") : "Comptoir vide";
        button(g, left + pw - 8 - 150, top + 24, 150, 16, claim, waiting, 0xFF2A6A3A, () -> send(Auction.ACT_CLAIM, 0, search.getValue(), page));
        search.visible = tab == 0;
        price.visible = tab == 1;
        if (tab == 0) renderBrowse(g);
        else if (tab == 1) renderSell(g);
        else renderMine(g);
    }

    private void renderBrowse(GuiGraphicsExtractor g) {
        button(g, left + 204, top + 45, 52, 16, "Chercher", true, 0xFF2A5A4A, () -> send(Auction.ACT_BROWSE, 0, search.getValue(), 0));
        g.text(font, total + " annonce(s)", left + 262, top + 49, Ui.MUTED, false);
        int rowH = 18;
        int y0 = top + 66;
        if (rows.isEmpty()) g.centeredText(font, Component.literal("Aucune annonce pour le moment."), left + pw / 2, y0 + 40, Ui.MUTED);
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int ry = y0 + i * rowH;
            Ui.frame(g, left + 8, ry, pw - 16, rowH - 2, r.stack.isEmpty() ? 0xFF50484A : 0xFF5A5470, hovered(left + 8, ry, pw - 16, rowH - 2));
            g.item(r.stack, left + 11, ry);
            g.itemDecorations(font, r.stack, left + 11, ry);
            g.text(font, font.plainSubstrByWidth(r.stack.getHoverName().getString(), 140), left + 32, ry + 5, Ui.TEXT, false);
            g.text(font, font.plainSubstrByWidth(r.seller + " · " + timeLeft(r.left), 100), left + 180, ry + 5, Ui.MUTED, false);
            String p = r.price + " or";
            g.text(font, p, left + pw - 90 - font.width(p), ry + 5, wallet >= r.price ? 0xFFFFD040 : Ui.RED, true);
            button(g, left + pw - 84, ry + 1, 70, 14, "Acheter", wallet >= r.price, 0xFF2A6A3A, () -> send(Auction.ACT_BUY, r.id, search.getValue(), page));
            if (hovered(left + 11, ry, 16, 16) && !r.stack.isEmpty()) g.setTooltipForNextFrame(font, r.stack, mouseX, mouseY);
        }
        int py = top + ph - 18;
        button(g, left + pw / 2 - 70, py, 40, 14, "◀", page > 0, 0xFF3B3350, () -> send(Auction.ACT_BROWSE, 0, query, page - 1));
        g.centeredText(font, Component.literal("Page " + (page + 1) + " / " + pages), left + pw / 2, py + 3, Ui.TEXT);
        button(g, left + pw / 2 + 30, py, 40, 14, "▶", page + 1 < pages, 0xFF3B3350, () -> send(Auction.ACT_BROWSE, 0, query, page + 1));
    }

    private void renderSell(GuiGraphicsExtractor g) {
        ItemStack held = Minecraft.getInstance().player.getMainHandItem();
        int x0 = left + 12;
        Ui.inset(g, x0, top + 50, pw - 24, 80);
        g.text(font, "OBJET EN MAIN", x0 + 8, top + 56, Ui.MUTED, false);
        if (held.isEmpty()) {
            g.text(font, "Tenez en main l'objet à vendre (l'ensemble de la pile sera mis en vente).", x0 + 8, top + 74, Ui.RED, false);
        } else {
            g.item(held, x0 + 10, top + 72);
            g.itemDecorations(font, held, x0 + 10, top + 72);
            g.text(font, held.getHoverName().getString() + " x" + held.getCount(), x0 + 34, top + 76, Ui.TEXT, true);
            if (hovered(x0 + 10, top + 72, 16, 16)) g.setTooltipForNextFrame(font, held, mouseX, mouseY);
        }
        g.text(font, "Durée : 48 h · taxe de 5 % prélevée à la vente · " + com.mmorpg.client.ClientData.DATA.auctionSlots + " annonces maximum", x0 + 8, top + 108, Ui.MUTED, false);
        long value = 0;
        try {
            value = price.getValue().isEmpty() ? 0 : Long.parseLong(price.getValue());
        } catch (NumberFormatException ignored) {
        }
        final long p = value;
        if (p > 0) g.text(font, "Vous recevrez " + Math.max(1, p - Math.round(p * Auction.TAX)) + " or", left + 150, top + 144, 0xFF7CE08A, false);
        button(g, left + 20, top + 164, 180, 18, "Mettre en vente", !held.isEmpty() && p > 0 && p <= Auction.MAX_PRICE, 0xFF2A6A3A, () -> {
            send(Auction.ACT_SELL, p, "", 0);
            price.setValue("");
        });
    }

    private void renderMine(GuiGraphicsExtractor g) {
        int rowH = 18;
        int y0 = top + 50;
        if (mine.isEmpty()) g.centeredText(font, Component.literal("Vous n'avez aucune annonce en cours."), left + pw / 2, y0 + 40, Ui.MUTED);
        for (int i = 0; i < mine.size(); i++) {
            Row r = mine.get(i);
            int ry = y0 + i * rowH;
            Ui.frame(g, left + 8, ry, pw - 16, rowH - 2, 0xFF5A5470, hovered(left + 8, ry, pw - 16, rowH - 2));
            g.item(r.stack, left + 11, ry);
            g.itemDecorations(font, r.stack, left + 11, ry);
            g.text(font, font.plainSubstrByWidth(r.stack.getHoverName().getString(), 150), left + 32, ry + 5, Ui.TEXT, false);
            g.text(font, "Reste " + timeLeft(r.left), left + 190, ry + 5, Ui.MUTED, false);
            String p = r.price + " or";
            g.text(font, p, left + pw - 100 - font.width(p), ry + 5, 0xFFFFD040, true);
            button(g, left + pw - 94, ry + 1, 80, 14, "Annuler", true, 0xFF6A3A2A, () -> send(Auction.ACT_CANCEL, r.id, "", 0));
            if (hovered(left + 11, ry, 16, 16) && !r.stack.isEmpty()) g.setTooltipForNextFrame(font, r.stack, mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (tab == 0 && search.isFocused() && event.key() == 257) {      // Entree : lancer la recherche
            send(Auction.ACT_BROWSE, 0, search.getValue(), 0);
            return true;
        }
        return super.keyPressed(event);
    }
}
