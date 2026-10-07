package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Journal de quetes : quetes en cours, progression et abandon. */
public class QuestLogScreen extends MenuScreen {
    private static String selected;
    private boolean confirmAbandon;
    private int page, filter;
    private static final String[] FILTERS = {"Toutes", "Campagne", "Journalières", "À rendre"};

    public QuestLogScreen() {
        super(Tab.QUETES);
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        ListTag quests = new ListTag();
        for (int i = 0; i < ClientData.quests.size(); i++) {
            CompoundTag entry = ClientData.quests.getCompoundOrEmpty(i);
            boolean daily = entry.getBooleanOr("daily", false);
            if (filter == 0 || filter == 1 && !daily || filter == 2 && daily || filter == 3 && entry.getBooleanOr("completable", false)) quests.add(entry);
        }
        int x0 = left + 8;
        int y0 = contentTop;
        button(g, x0, y0 + 14, 160, 16, FILTERS[filter] + "  >", true, 0xFF2A2420, () -> { filter = (filter + 1) % FILTERS.length; page = 0; });
        int lw = 160;
        Ui.header(g, x0, y0, lw, "JOURNAL (" + ClientData.quests.size() + "/10)");
        if (quests.isEmpty()) {
            Ui.wrap(g, "Aucune quête dans cette catégorie. Parlez à un Maître des Quêtes pour en obtenir.", x0, y0 + 36, lw, Ui.MUTED);
        }
        boolean found = false;
        for (int i = 0; i < quests.size(); i++) {
            if (quests.getCompoundOrEmpty(i).getStringOr("id", "").equals(selected)) found = true;
        }
        if (!found) {
            selected = quests.isEmpty() ? null : quests.getCompoundOrEmpty(0).getStringOr("id", "");
            confirmAbandon = false;
        }
        int rowH = 20;
        int capacity = Math.max(1, (top + ph - 58 - y0) / rowH);
        int pages = Math.max(1, (quests.size() + capacity - 1) / capacity);
        page = Math.min(page, pages - 1);
        for (int i = page * capacity; i < Math.min(quests.size(), (page + 1) * capacity); i++) {
            CompoundTag q = quests.getCompoundOrEmpty(i);
            String id = q.getStringOr("id", "");
            int ry = y0 + 34 + (i - page * capacity) * rowH;
            if (ry + rowH > top + ph - 18) break;
            boolean ready = q.getBooleanOr("completable", false);
            Ui.frame(g, x0, ry, lw, rowH - 2, ready ? Ui.GREEN : Ui.GOLD, id.equals(selected) || hovered(x0, ry, lw, rowH - 2));
            g.text(font, font.plainSubstrByWidth((q.getBooleanOr("tracked", false) ? "★ " : "") + q.getStringOr("name", "?"), lw - 10), x0 + 5, ry + 5, ready ? Ui.GREEN : Ui.TEXT, false);
            click(x0, ry, lw, rowH - 2, () -> {
                selected = id;
                confirmAbandon = false;
            });
        }
        button(g, x0, top + ph - 36, 28, 16, "<", page > 0, 0xFF2A2420, () -> page--);
        g.text(font, (page + 1) + "/" + pages, x0 + 36, top + ph - 32, Ui.MUTED, false);
        button(g, x0 + lw - 28, top + ph - 36, 28, 16, ">", page + 1 < pages, 0xFF2A2420, () -> page++);
        g.text(font, "Quêtes accomplies : " + ClientData.DATA.completedQuests.size(), x0, top + ph - 14, Ui.MUTED, false);

        int dx = x0 + lw + 8;
        int dw = left + pw - 8 - dx;
        int dh = ph - 30 - 10;
        Ui.inset(g, dx, y0, dw, dh);
        CompoundTag q = null;
        for (int i = 0; i < quests.size(); i++) {
            if (quests.getCompoundOrEmpty(i).getStringOr("id", "").equals(selected)) q = quests.getCompoundOrEmpty(i);
        }
        if (q == null) return;
        int by = y0 + dh - 22;
        QuestPanel.render(g, q, dx, y0, dw, by - 23, mx, my);
        String id = q.getStringOr("id", "");
        boolean tracked = q.getBooleanOr("tracked", false);
        button(g, dx + 6, by - 20, dw - 12, 16, tracked ? "Ne plus suivre en priorité" : "Suivre en priorité", true, 0xFF2A3420, () -> ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.TRACK, tracked ? "" : id, -1)));
        if (q.getBooleanOr("completable", false)) {
            g.centeredText(font, net.minecraft.network.chat.Component.literal(font.plainSubstrByWidth("À rendre : " + (q.getStringOr("giver", "").isEmpty() ? "Maître des Quêtes" : q.getStringOr("giver", "")), dw - 12)), dx + dw / 2, by + 4, Ui.GREEN);
        } else if (confirmAbandon) {
            button(g, dx + 6, by, dw / 2 - 9, 16, "Confirmer", true, 0xFF7A2A2A, () -> {
                ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.ABANDON, id, -1));
                confirmAbandon = false;
            });
            button(g, dx + dw / 2 + 3, by, dw / 2 - 9, 16, "Annuler", true, 0xFF2A2420, () -> confirmAbandon = false);
        } else {
            button(g, dx + 6, by, dw - 12, 16, "Abandonner la quête", true, 0xFF5A2A2A, () -> confirmAbandon = true);
        }
    }
}
