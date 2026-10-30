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
    @Override protected String backdrop() { return "auction_header"; }

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
    private int sellSlot=-1,inventoryPage;

    public AuctionScreen(CompoundTag data) {
        super("Hôtel des ventes");
        receive(data);
    }

    public void previewTab(int value){tab=Math.clamp(value,0,2);}
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
        search = new EditBox(this.font, left + 10, top + 45, 230, 16, Component.literal("Recherche"));
        search.setHint(Component.literal("Rechercher un objet...").withColor(0xFF807868));
        search.setMaxLength(40);
        search.setValue(query);
        this.addRenderableWidget(search);
        price = new EditBox(this.font, left + 12, top + 190, 150, 18, Component.literal("Prix"));
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
        price.setY(top + 190);
        if (tab == 0) renderBrowse(g);
        else if (tab == 1) renderSell(g);
        else renderMine(g);
    }

    private void bigItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int box, boolean selected) {
        Ui.itemBox(g, font, stack, x, y, box);
        if (selected) Ui.border(g, x, y, box, box, Ui.GOLD);
    }

    private void listCard(GuiGraphicsExtractor g, Row r, int x, int y, int w, int h, String line2, boolean affordable, String buttonLabel, boolean buttonOn, int buttonAccent, Runnable action) {
        boolean hov = hovered(x, y, w, h);
        Ui.frame(g, x, y, w, h, r.stack.isEmpty() ? 0xFF50484A : 0xFF5A5470, hov);
        int box = h - 4;
        bigItem(g, r.stack, x + 2, y + 2, box, false);
        int tx = x + box + 8, bw = 64;
        g.text(font, font.plainSubstrByWidth(r.stack.getHoverName().getString(), w - box - 14 - bw - 6), tx, y + 5, Ui.TEXT, false);
        g.text(font, font.plainSubstrByWidth(line2, w - box - 14 - bw - 6), tx, y + h - 14, Ui.MUTED, false);
        String p = r.price + " or";
        g.text(font, p, x + w - 6 - font.width(p), y + 5, affordable ? 0xFFFFD040 : Ui.RED, true);
        button(g, x + w - 6 - bw, y + h - 17, bw, 14, buttonLabel, buttonOn, buttonAccent, action);
        if (hov && !r.stack.isEmpty()) g.setTooltipForNextFrame(font, r.stack, mouseX, mouseY);
    }

    private void renderBrowse(GuiGraphicsExtractor g) {
        button(g, left + 244, top + 45, 56, 16, "Chercher", true, 0xFF2A5A4A, () -> send(Auction.ACT_BROWSE, 0, search.getValue(), 0));
        g.text(font, total + " annonce(s)", left + 308, top + 49, Ui.MUTED, false);
        int ch = 36, y0 = top + 66, avail = ph - 66 - 24;
        int perCol = Math.max(1, avail / ch);
        int cols = rows.size() > perCol * 2 ? 3 : 2, gap = 6, cw = (pw - 16 - gap * (cols - 1)) / cols;
        if (rows.isEmpty()) g.centeredText(font, Component.literal("Aucune annonce pour le moment."), left + pw / 2, y0 + 40, Ui.MUTED);
        for (int i = 0; i < rows.size() && i < perCol * cols; i++) {
            Row r = rows.get(i);
            int x = left + 8 + (i % cols) * (cw + gap), y = y0 + (i / cols) * ch;
            listCard(g, r, x, y, cw, ch - 2, r.seller + " · " + timeLeft(r.left), wallet >= r.price, "Acheter", wallet >= r.price, 0xFF2A6A3A,
                    () -> send(Auction.ACT_BUY, r.id, search.getValue(), page));
        }
        int py = top + ph - 18;
        button(g, left + pw / 2 - 70, py, 40, 14, "◀", page > 0, 0xFF3B3350, () -> send(Auction.ACT_BROWSE, 0, query, page - 1));
        g.centeredText(font, Component.literal("Page " + (page + 1) + " / " + pages), left + pw / 2, py + 3, Ui.TEXT);
        button(g, left + pw / 2 + 30, py, 40, 14, "▶", page + 1 < pages, 0xFF3B3350, () -> send(Auction.ACT_BROWSE, 0, query, page + 1));
    }

    private void renderSell(GuiGraphicsExtractor g) {
        var player=Minecraft.getInstance().player;
        if(sellSlot<0)sellSlot=player.getInventory().getSelectedSlot();
        var bags=com.mmorpg.server.Backpacks.decode(com.mmorpg.client.ClientData.DATA.backpacks,player.level().registryAccess());
        int inventorySize=36+com.mmorpg.server.Backpacks.capacity(bags),invPages=(inventorySize+35)/36;inventoryPage=Math.clamp(inventoryPage,0,invPages-1);if(sellSlot>=inventorySize)sellSlot=0;
        java.util.function.IntFunction<ItemStack> stackAt=slot->slot<36?player.getInventory().getItem(slot):bags.getItem(slot-34);
        ItemStack held=stackAt.apply(sellSlot);
        int x0=left+8, cell=26, gridW=9*cell+14, top0=top+46, panelH=4*cell+34;
        // colonne gauche : inventaire (grandes cases)
        Ui.inset(g,x0,top0,gridW,panelH);
        g.text(font,"CHOISIR DANS MON INVENTAIRE",x0+7,top0+6,Ui.GOLD,false);
        if(invPages>1){button(g,x0+gridW-38,top0+4,15,13,"‹",inventoryPage>0,Ui.GOLD,()->inventoryPage--);button(g,x0+gridW-21,top0+4,15,13,"›",inventoryPage+1<invPages,Ui.GOLD,()->inventoryPage++);}
        for(int i=0;i<36&&inventoryPage*36+i<inventorySize;i++){
            int slot=inventoryPage==0?(i<27?i+9:i-27):inventoryPage*36+i,sx=x0+7+(i%9)*cell,sy=top0+22+(i/9)*cell;
            ItemStack stack=stackAt.apply(slot);
            bigItem(g,stack,sx,sy,cell-2,slot==sellSlot);
            if(!stack.isEmpty()){click(sx,sy,cell-2,cell-2,()->sellSlot=slot);if(hovered(sx,sy,cell-2,cell-2))g.setTooltipForNextFrame(font,stack,mouseX,mouseY);}
        }
        // colonne droite : detail de l'objet choisi
        int dx=x0+gridW+8, dw=left+pw-8-dx;
        Ui.inset(g,dx,top0,dw,panelH);
        int pv=Math.min(64,panelH-16);
        bigItem(g,held,dx+8,top0+8,pv,false);
        int tx=dx+pv+18, tw=Math.max(40,dw-pv-26);
        g.text(font,font.plainSubstrByWidth(held.isEmpty()?"Choisissez un objet":held.getHoverName().getString(),tw),tx,top0+10,Ui.TEXT,false);
        if(!held.isEmpty())g.text(font,"Pile : "+held.getCount()+" objet(s)",tx,top0+24,Ui.MUTED,false);
        g.text(font,"Durée : 48 h",tx,top0+40,Ui.MUTED,false);
        g.text(font,"Taxe : "+Math.round(Auction.TAX*100)+" %",tx,top0+54,Ui.MUTED,false);
        g.text(font,"Toute la pile sélectionnée sera vendue.",dx+8,top0+panelH-14,Ui.MUTED,false);
        // ligne de prix : libelle, champ, raccourcis
        int py=top+190;
        g.text(font,"PRIX TOTAL (OR)",x0+4,py-12,Ui.GOLD,false);
        long value = 0;
        try {
            value = price.getValue().isEmpty() ? 0 : Long.parseLong(price.getValue());
        } catch (NumberFormatException ignored) {
        }
        final long p = value;
        int bx=x0+4+150+6;
        long[] steps={10,100,1000,10000};
        for(long st:steps){String lb="+"+(st>=1000?st/1000+"k":st);button(g,bx,py,40,18,lb,true,0xFF3B3350,()->setPrice(Math.min(Auction.MAX_PRICE,p+st)));bx+=43;}
        button(g,bx,py,48,18,"Effacer",p>0,0xFF6A3A2A,()->price.setValue(""));
        if (p > 0) g.text(font, "Vous recevrez " + Math.max(1, p - Math.round(p * Auction.TAX)) + " or", dx+8, py-12, 0xFF7CE08A, false);
        // action principale
        boolean can=!held.isEmpty() && p > 0 && p <= Auction.MAX_PRICE;
        button(g, left + 8, top + ph - 36, pw - 16, 26, "Mettre en vente", can, 0xFF2A8A4A, () -> {
            send(Auction.ACT_SELL,p,"slot:"+sellSlot+":"+Auction.saleFingerprint(held,player.level().registryAccess()),0);
            price.setValue("");
        });
        if(!can)g.centeredText(font,Component.literal(held.isEmpty()?"Choisissez un objet puis indiquez son prix.":"Indiquez un prix pour mettre en vente."),left+pw/2,top+ph-46,Ui.MUTED);
    }

    private void setPrice(long v) {
        price.setValue(Long.toString(v));
    }

    private void renderMine(GuiGraphicsExtractor g) {
        int ch = 36, y0 = top + 50;
        if (mine.isEmpty()) g.centeredText(font, Component.literal("Vous n'avez aucune annonce en cours."), left + pw / 2, y0 + 40, Ui.MUTED);
        int fit = Math.max(1, (ph - 50 - 8) / ch);
        for (int i = 0; i < mine.size() && i < fit; i++) {
            Row r = mine.get(i);
            listCard(g, r, left + 8, y0 + i * ch, pw - 16, ch - 2, "Reste " + timeLeft(r.left), true, "Annuler", true, 0xFF6A3A2A, () -> send(Auction.ACT_CANCEL, r.id, "", 0));
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (tab == 0 && search.isFocused() && event.isConfirmation()) {      // Entree : lancer la recherche
            send(Auction.ACT_BROWSE, 0, search.getValue(), 0);
            return true;
        }
        return super.keyPressed(event);
    }
}
