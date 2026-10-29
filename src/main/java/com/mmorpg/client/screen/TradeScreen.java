package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.economy.TradeManager;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Echange entre deux joueurs (/trade) : mes offres a gauche, celles du partenaire a droite, mon inventaire dessous ; validation puis confirmation. */
public class TradeScreen extends MmoScreen {
    @Override protected String backdrop() { return "auction_header"; }
    private String partner = "?";
    private String status = "";
    private int stage;
    private long confirmAt;
    private boolean myReady, theirReady, myConfirmed, theirConfirmed;
    private long myGold, theirGold, wallet;
    private final List<ItemStack> mine = new ArrayList<>();
    private final Set<Integer> mineSlots = new HashSet<>();
    private final List<ItemStack> theirs = new ArrayList<>();
    private EditBox gold;
    private boolean closedByServer;

    public TradeScreen(CompoundTag data) {
        super("Échange");
        receive(data);
    }

    private static List<ItemStack> decode(ListTag list, Set<Integer> slots) {
        List<ItemStack> out = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        if (level == null) return out;
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompoundOrEmpty(i);
            out.add(ItemStack.OPTIONAL_CODEC.parse(ops, e.get("stack")).result().orElse(ItemStack.EMPTY));
            if (slots != null) slots.add(e.getIntOr("slot", -1));
        }
        return out;
    }

    public void receive(CompoundTag data) {
        if (data.getBooleanOr("closed", false)) {
            closedByServer = true;
            onClose();
            return;
        }
        partner = data.getStringOr("partner", "?");
        status = data.getStringOr("status", "");
        stage = data.getIntOr("stage", 0);
        confirmAt = System.currentTimeMillis() + data.getLongOr("left", 0);
        myReady = data.getBooleanOr("myReady", false);
        theirReady = data.getBooleanOr("theirReady", false);
        myConfirmed = data.getBooleanOr("myConfirmed", false);
        theirConfirmed = data.getBooleanOr("theirConfirmed", false);
        myGold = data.getLongOr("myGold", 0);
        theirGold = data.getLongOr("theirGold", 0);
        wallet = data.getLongOr("wallet", 0);
        mine.clear();
        mineSlots.clear();
        mine.addAll(decode(data.getListOrEmpty("mine"), mineSlots));
        theirs.clear();
        theirs.addAll(decode(data.getListOrEmpty("theirs"), null));
    }

    @Override
    protected void init() {
        super.init();
        gold = new EditBox(this.font, left + 14, top + 98, 90, 14, Component.literal("Or"));
        gold.setHint(Component.literal("Or à donner").withColor(0xFF807868));
        gold.setMaxLength(10);
        gold.setValue(myGold > 0 ? Long.toString(myGold) : "");
        gold.setResponder(s -> {
            if (!s.matches("\\d*")) gold.setValue(s.replaceAll("\\D", ""));
        });
        this.addRenderableWidget(gold);
    }

    private void send(String act, int slot, long amount) {
        CompoundTag t = new CompoundTag();
        t.putString("act", act);
        t.putInt("slot", slot);
        t.putLong("amount", amount);
        ClientNet.send(new Payloads.SocialAction("trade", t));
    }

    private void sendGold() {
        long v = 0;
        try {
            v = gold.getValue().isEmpty() ? 0 : Long.parseLong(gold.getValue());
        } catch (NumberFormatException ignored) {
        }
        send("gold", 0, v);
    }

    private void offers(GuiGraphicsExtractor g, int x, int y, List<ItemStack> stacks, boolean editable) {
        for (int i = 0; i < TradeManager.MAX_OFFERS; i++) {
            int sx = x + (i % 4) * 22, sy = y + (i / 4) * 22;
            ItemStack st = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            Ui.frame(g, sx, sy, 20, 20, st.isEmpty() ? Ui.LINE : Ui.GOLD, hovered(sx, sy, 20, 20) && !st.isEmpty());
            g.item(st, sx + 2, sy + 2);
            g.itemDecorations(font, st, sx + 2, sy + 2);
            if (!st.isEmpty() && hovered(sx, sy, 20, 20)) g.setTooltipForNextFrame(font, st, mouseX, mouseY);
        }
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "ÉCHANGE", Ui.GOLD_LIGHT);
        int half = (pw - 30) / 2;
        int lx = left + 10, rx = left + 20 + half;
        // --- mes offres
        Ui.inset(g, lx, top + 24, half, 92);
        Ui.header(g, lx + 6, top + 29, half - 12, "VOUS");
        offers(g, lx + 8, top + 46, mine, true);
        g.item(new ItemStack(ModItems.PIECE_OR.get()), lx + 8, top + 94 - 2);
        gold.setX(lx + 28);
        gold.setY(top + 96);
        gold.visible = true;
        gold.setEditable(stage == 0);
        button(g, lx + 122, top + 96, 28, 14, "OK", stage == 0, 0xFF2A5A4A, this::sendGold);
        g.text(font, "Portefeuille : " + wallet, lx + 98, top + 50, Ui.MUTED, false);
        chip(g, lx + half - 8, top + 30, myReady, myConfirmed);
        // --- offres du partenaire
        Ui.inset(g, rx, top + 24, half, 92);
        Ui.header(g, rx + 6, top + 29, half - 12, font.plainSubstrByWidth(partner.toUpperCase(), half - 70));
        offers(g, rx + 8, top + 46, theirs, false);
        g.item(new ItemStack(ModItems.PIECE_OR.get()), rx + 8, top + 94 - 2);
        g.text(font, theirGold + " or", rx + 28, top + 99, theirGold > 0 ? 0xFFFFD040 : Ui.MUTED, true);
        chip(g, rx + half - 8, top + 30, theirReady, theirConfirmed);
        // --- inventaire
        Ui.inset(g, left + 10, top + 120, pw - 20, 88);
        Ui.header(g, left + 16, top + 125, pw - 32, stage == 0 ? "MON INVENTAIRE · cliquer pour proposer ou retirer" : "RÉCAPITULATIF");
        if (stage == 0) inventory(g);
        else summary(g);
        // --- boutons et etat
        int by = top + ph - 40;
        int bw = Math.min(140, (pw - 40) / 3);
        if (stage == 0) {
            button(g, left + 14, by, bw, 18, myReady ? "Valider (annuler)" : "Valider mon offre", true, myReady ? 0xFF6A5A2A : 0xFF2A6A3A, () -> send("ready", 0, 0));
        } else {
            long wait = Math.max(0, confirmAt - System.currentTimeMillis());
            String label = myConfirmed ? "En attente de " + partner : wait > 0 ? "Confirmer (" + ((wait + 999) / 1000) + ")" : "Confirmer l'échange";
            button(g, left + 14, by, bw + 20, 18, label, !myConfirmed && wait == 0, 0xFF2A6A3A, () -> send("confirm", 0, 0));
            button(g, left + 14 + bw + 26, by, bw - 20, 18, "Modifier", true, 0xFF6A5A2A, () -> send("back", 0, 0));
        }
        button(g, left + pw - 14 - bw, by, bw, 18, "Annuler l'échange", true, 0xFF6A3A2A, () -> send("cancel", 0, 0));
        String line = !status.isEmpty() ? status
                : stage == 1 ? "Vérifiez bien : les deux joueurs doivent confirmer."
                : myReady && !theirReady ? "En attente de la validation de " + partner + "."
                : "Rien ne change de main avant la confirmation des deux joueurs.";
        g.centeredText(font, Component.literal(font.plainSubstrByWidth(line, pw - 24)), left + pw / 2, top + ph - 16, status.isEmpty() ? Ui.MUTED : 0xFFFFB070);
    }

    private void chip(GuiGraphicsExtractor g, int right, int y, boolean ready, boolean confirmed) {
        String t = confirmed ? "CONFIRMÉ" : ready ? "VALIDÉ" : "EN COURS";
        int color = confirmed ? Ui.GREEN : ready ? Ui.GOLD : Ui.MUTED;
        int w = font.width(t);
        g.text(font, t, right - w, y, color, false);
    }

    private void inventory(GuiGraphicsExtractor g) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        int gx = left + (pw - 9 * 19) / 2;
        for (int i = 0; i < 36; i++) {
            int row = i / 9, col = i % 9;
            int slot = row < 3 ? 9 + i : i - 27;                                        // sac en haut, barre d'action en bas
            int sx = gx + col * 19, sy = top + 140 + row * 17 + (row == 3 ? 3 : 0);
            ItemStack st = player.getInventory().getItem(slot);
            boolean offered = mineSlots.contains(slot);
            Ui.frame(g, sx, sy, 18, 16, offered ? Ui.GOLD : Ui.LINE, offered || hovered(sx, sy, 18, 16));
            g.item(st, sx + 1, sy);
            g.itemDecorations(font, st, sx + 1, sy);
            if (!st.isEmpty()) {
                click(sx, sy, 18, 16, () -> send("offer", slot, 0));
                if (hovered(sx, sy, 18, 16)) g.setTooltipForNextFrame(font, st, mouseX, mouseY);
            }
        }
    }

    private void summary(GuiGraphicsExtractor g) {
        int half = (pw - 30) / 2;
        summaryColumn(g, left + 16, top + 142, half - 12, "Vous donnez", mine, myGold);
        summaryColumn(g, left + 26 + half, top + 142, half - 12, "Vous recevez", theirs, theirGold);
    }

    private void summaryColumn(GuiGraphicsExtractor g, int x, int y, int w, String title, List<ItemStack> stacks, long goldAmount) {
        g.text(font, title, x, y, Ui.GOLD, false);
        int ly = y + 11;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack st = stacks.get(i);
            if (i == 5) {
                g.text(font, "+ " + (stacks.size() - 5) + " autre(s)", x, ly, Ui.MUTED, false);
                break;
            }
            g.text(font, font.plainSubstrByWidth(st.getHoverName().getString() + " x" + st.getCount(), w), x, ly, Ui.TEXT, false);
            ly += 9;
        }
        if (stacks.isEmpty()) g.text(font, "Aucun objet", x, ly, Ui.MUTED, false);
        g.text(font, goldAmount + " or", x, y + 56, goldAmount > 0 ? 0xFFFFD040 : Ui.MUTED, true);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (gold.isFocused() && event.key() == 257) {                                     // Entree : fixer l'or
            sendGold();
            return true;
        }
        return super.keyPressed(event);
    }

    /** Fermer l'ecran (Echap) annule l'echange ; rien n'a bouge tant que les deux n'ont pas confirme. */
    @Override
    public void removed() {
        super.removed();
        if (!closedByServer) send("cancel", 0, 0);
    }
}
