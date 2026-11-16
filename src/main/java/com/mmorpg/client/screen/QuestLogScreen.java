package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import com.mmorpg.quest.QuestKind;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Journal de quetes : quetes en cours, progression et abandon. */
public class QuestLogScreen extends MenuScreen {
    private static String selected;
    private boolean confirmAbandon;
    private int page, filter;
    private static final String[] FILTERS = {"Toutes", "Principales", "Secondaires", "Réputation", "Journalières", "À rendre", "Important", "Histoire", "Découvertes"};

    public QuestLogScreen() {
        super(Tab.QUETES);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        QuestPanel.scrollBy(-(int) Math.signum(sy));
        return true;
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        ListTag quests = new ListTag();
        ListTag source = filter == 6 ? ClientData.declined : filter == 7 ? ClientData.questExtra.getListOrEmpty("story")
                : filter == 8 ? ClientData.questExtra.getListOrEmpty("explore") : ClientData.quests;
        if (filter == 4) {
            source = new ListTag();
            source.addAll(ClientData.quests);
            source.addAll(ClientData.questExtra.getListOrEmpty("offers"));
        }
        for (int k = 0; k < QuestKind.values().length; k++) {
            for (int i = 0; i < source.size(); i++) {
                CompoundTag entry = source.getCompoundOrEmpty(i);
                QuestKind kind = QuestPanel.kindOf(entry);
                if (kind.ordinal() != k) continue;
                boolean ok = switch (filter) {
                    case 1 -> kind == QuestKind.PRINCIPALE;
                    case 2 -> kind == QuestKind.SECONDAIRE || kind == QuestKind.EVENEMENT;
                    case 3 -> kind == QuestKind.REPUTATION;
                    case 4 -> kind == QuestKind.JOURNALIERE;
                    case 5 -> entry.getBooleanOr("completable", false);
                    default -> true;
                };
                if (ok) quests.add(entry);
            }
        }
        int x0 = left + 8;
        int y0 = contentTop;
        button(g, x0, y0 + 14, 160, 16, FILTERS[filter] + "  >", true, 0xFF2A2420, () -> { filter = (filter + 1) % FILTERS.length; page = 0; });
        int lw = 160;
        Ui.header(g, x0, y0, lw, filter == 8 ? "DÉCOUVERTES (" + quests.size() + "/" + ClientData.questExtra.getIntOr("exploreTotal", 0) + ")" : "JOURNAL (" + ClientData.quests.size() + "/10)");
        if (quests.isEmpty()) {
            Ui.wrap(g, (filter == 6 ? "Aucune quête refusée. Celles que vous repoussez avec « Plus tard » arrivent ici."
                    : filter == 7 ? "L'histoire d'Aeloria commence après le tutoriel. Chaque chapitre se débloque en jouant."
                    : filter == 8 ? "Des lieux secrets se cachent dans le monde : explorez, sans aucune indication."
                    : filter == 4 ? "Plus de quête du jour : revenez demain pour de nouveaux contrats."
                    : filter == 1 ? "Aucune quête principale en cours. L'histoire avance en parlant à Flammèche et aux PNJ."
                    : filter == 2 ? "Aucune quête secondaire en cours. Parlez aux PNJ marqués d'un !."
                    : filter == 3 ? "Aucune quête de réputation en cours. Elles améliorent votre renom auprès des factions."
                    : "Aucune quête dans cette catégorie. Parlez à un Maître des Quêtes pour en obtenir."), x0, y0 + 36, lw, Ui.MUTED);
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
        int capacity = Math.max(1, (top + ph - 68 - y0) / rowH);
        int pages = Math.max(1, (quests.size() + capacity - 1) / capacity);
        page = Math.min(page, pages - 1);
        for (int i = page * capacity; i < Math.min(quests.size(), (page + 1) * capacity); i++) {
            CompoundTag q = quests.getCompoundOrEmpty(i);
            String id = q.getStringOr("id", "");
            int ry = y0 + 34 + (i - page * capacity) * rowH;
            if (ry + rowH > top + ph - 50) break;
            boolean ready = q.getBooleanOr("completable", false);
            QuestKind kind = QuestPanel.kindOf(q);
            Ui.frame(g, x0, ry, lw, rowH - 2, ready ? Ui.GREEN : kind.color, id.equals(selected) || hovered(x0, ry, lw, rowH - 2));
            g.fill(x0 + 2, ry + 2, x0 + 4, ry + rowH - 4, kind.color);
            g.text(font, kind.glyph, x0 + 8, ry + 5, kind.color, true);
            String full = (q.getBooleanOr("tracked", false) ? "\u2605 " : "") + q.getStringOr("name", "?");
            String shown = Ui.fit(full, lw - 26);
            g.text(font, shown, x0 + 19, ry + 5, ready ? Ui.GREEN : Ui.TEXT, false);
            if (!shown.equals(full) && hovered(x0, ry, lw, rowH - 2)) g.setTooltipForNextFrame(font, net.minecraft.network.chat.Component.literal(full), mx, my);
            click(x0, ry, lw, rowH - 2, () -> {
                selected = id;
                confirmAbandon = false;
            });
        }
        button(g, x0, top + ph - 46, 28, 16, "<", page > 0, 0xFF2A2420, () -> page--);
        g.text(font, (page + 1) + "/" + pages, x0 + 36, top + ph - 42, Ui.MUTED, false);
        button(g, x0 + lw - 28, top + ph - 46, 28, 16, ">", page + 1 < pages, 0xFF2A2420, () -> page++);
        int lx = x0;
        for (QuestKind k : QuestKind.values()) {
            if (k == QuestKind.EVENEMENT) continue;
            g.text(font, k.glyph, lx, top + ph - 24, k.color, true);
            String nm = k == QuestKind.JOURNALIERE ? "Jour." : k == QuestKind.REPUTATION ? "Rép." : k == QuestKind.PRINCIPALE ? "Princ." : "Second.";
            g.text(font, nm, lx + font.width(k.glyph) + 3, top + ph - 24, Ui.MUTED, false);
            lx += font.width(k.glyph) + 3 + font.width(nm) + 6;
        }
        g.text(font, "Quêtes accomplies : " + ClientData.DATA.completedQuests.size(), x0, top + ph - 13, Ui.MUTED, false);

        int dx = x0 + lw + 8;
        int dw = left + pw - 8 - dx;
        int dh = ph - 54 - 10;
        Ui.inset(g, dx, y0, dw, dh);
        CompoundTag q = null;
        for (int i = 0; i < quests.size(); i++) {
            if (quests.getCompoundOrEmpty(i).getStringOr("id", "").equals(selected)) q = quests.getCompoundOrEmpty(i);
        }
        if (q == null) {
            int side = Math.max(32, Math.min(Math.min(dw - 16, dh - 40), 128));
            Ui.squareArt(g, "quest_master", 256, dx + (dw - side) / 2, y0 + (dh - side) / 2 - 8, side);
            g.centeredText(font, net.minecraft.network.chat.Component.literal("Parlez au Maître des Quêtes"), dx + dw / 2, y0 + (dh + side) / 2 - 2, Ui.MUTED);
            return;
        }
        int by = y0 + dh - 22;
        QuestPanel.render(g, q, dx, y0, dw, by - 23, mx, my);
        String id = q.getStringOr("id", "");
        if (filter == 8) return;
        if (filter == 7) {
            button(g, dx + 6, by, dw - 12, 16, "▶ Rejouer la cinématique", true, 0xFF3A2A6A, () -> ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.STORY_REPLAY, id, -1)));
            return;
        }
        if (q.getBooleanOr("offer", false)) {
            button(g, dx + 6, by, dw - 12, 16, "Accepter la quête du jour", ClientData.quests.size() < 10, 0xFF1A5A7A, () -> ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.ACCEPT, id, -1)));
            return;
        }
        if (q.getBooleanOr("completable", false) && id.startsWith("rdaily:")) {
            button(g, dx + 6, by, dw - 12, 16, "Rendre la quête", true, 0xFF2A7A3A, () -> ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.COMPLETE, id, -1)));
            return;
        }
        if (filter == 6) {
            button(g, dx + 6, by, dw - 12, 16, "Accepter la quête", ClientData.quests.size() < 10, 0xFF7A5A1A, () -> ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.ACCEPT, id, -1)));
            return;
        }
        boolean tracked = q.getBooleanOr("tracked", false);
        button(g, dx + 6, by - 20, dw - 12, 16, tracked ? "Ne plus suivre en priorité" : "Suivre en priorité", true, 0xFF2A3420, () -> ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.TRACK, tracked ? "" : id, -1)));
        if (q.getBooleanOr("completable", false)) {
            g.centeredText(font, net.minecraft.network.chat.Component.literal(Ui.fit("À rendre : " + (q.getStringOr("giver", "").isEmpty() ? "Maître des Quêtes" : q.getStringOr("giver", "")), dw - 12)), dx + dw / 2, by + 4, Ui.GREEN);
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
