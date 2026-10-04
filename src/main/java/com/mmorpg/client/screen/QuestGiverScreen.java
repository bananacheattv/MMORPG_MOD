package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Dialogue avec un Maitre des Quetes : quetes a rendre, en cours et disponibles. */
public class QuestGiverScreen extends MmoScreen {
    private final int npc;
    private final String npcName;
    private final List<CompoundTag> entries = new ArrayList<>();
    private final int completed;
    private String selected;
    private int scroll;

    public QuestGiverScreen(CompoundTag data) {
        super("Maître des Quêtes");
        this.npc = data.getIntOr("npc", -1);
        this.npcName = data.getStringOr("name", "Maître des Quêtes");
        this.completed = data.getIntOr("completed", 0);
        ListTag active = data.getListOrEmpty("active");
        List<CompoundTag> ready = new ArrayList<>();
        List<CompoundTag> progress = new ArrayList<>();
        for (int i = 0; i < active.size(); i++) {
            CompoundTag q = active.getCompoundOrEmpty(i);
            (q.getBooleanOr("completable", false) ? ready : progress).add(q);
        }
        entries.addAll(ready);
        ListTag available = data.getListOrEmpty("available");
        for (int i = 0; i < available.size(); i++) entries.add(available.getCompoundOrEmpty(i));
        entries.addAll(progress);
        if (!entries.isEmpty()) selected = entries.get(0).getStringOr("id", "");
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, scroll - (int) Math.signum(sy));
        return true;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, npcName.toUpperCase(java.util.Locale.ROOT), Ui.GOLD_LIGHT);
        g.centeredText(font, Component.literal("Maître des Quêtes"), left + pw / 2, top + 19, Ui.MUTED);
        int x0 = left + 8;
        int y0 = top + 32;
        int lw = 160;
        int rowH = 20;
        int visible = (top + ph - 22 - y0) / rowH;
        scroll = Math.min(scroll, Math.max(0, entries.size() - visible));
        if (entries.isEmpty()) {
            Ui.wrap(g, "Je n'ai aucune tâche à vous confier pour le moment. Revenez lorsque vous serez plus expérimenté.", x0, y0 + 4, lw, Ui.MUTED);
        }
        for (int i = 0; i < Math.min(visible, entries.size() - scroll); i++) {
            CompoundTag q = entries.get(i + scroll);
            String id = q.getStringOr("id", "");
            int ry = y0 + i * rowH;
            boolean active = q.getBooleanOr("active", false);
            boolean ready = q.getBooleanOr("completable", false);
            int color = ready ? Ui.GREEN : active ? 0xFF9C9282 : Ui.GOLD;
            Ui.frame(g, x0, ry, lw, rowH - 2, color, id.equals(selected) || hovered(x0, ry, lw, rowH - 2));
            String mark = ready ? "?" : active ? "…" : "!";
            Ui.scaledText(g, mark, x0 + 8, ry + 3, 1.4f, color, true);
            g.text(font, font.plainSubstrByWidth(q.getStringOr("name", "?"), lw - 40), x0 + 17, ry + 5, ready ? Ui.GREEN : Ui.TEXT, false);
            String lv = String.valueOf(q.getIntOr("minLevel", 1));
            g.text(font, lv, x0 + lw - 5 - font.width(lv), ry + 5, Ui.MUTED, false);
            click(x0, ry, lw, rowH - 2, () -> selected = id);
        }
        g.text(font, "Quêtes accomplies : " + completed, x0, top + ph - 14, Ui.MUTED, false);

        int dx = x0 + lw + 8;
        int dw = left + pw - 8 - dx;
        int dh = ph - 32 - 8;
        Ui.inset(g, dx, y0, dw, dh);
        CompoundTag q = entries.stream().filter(e -> e.getStringOr("id", "").equals(selected)).findFirst().orElse(null);
        if (q == null) return;
        int by = y0 + dh - 22;
        QuestPanel.render(g, q, dx, y0, dw, by - 2, mx, my);
        String id = q.getStringOr("id", "");
        if (q.getBooleanOr("completable", false)) {
            button(g, dx + 6, by, dw - 12, 16, "Rendre la quête", true, 0xFF2A7A3A,
                    () -> ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.COMPLETE, id, npc)));
        } else if (q.getBooleanOr("active", false)) {
            button(g, dx + 6, by, dw - 12, 16, "En cours…", false, 0xFF3A3436, () -> {
            });
        } else {
            button(g, dx + 6, by, dw - 12, 16, "Accepter la quête", true, 0xFF7A5A1A,
                    () -> ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.ACCEPT, id, npc)));
        }
    }
}
