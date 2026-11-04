package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Panneau de moderation (/moderation, operateurs) : onglets a gauche, liste a droite, champs de saisie (raison, duree) et boutons d'action en bas.
 * Toute action est reverifiee et journalisee cote serveur.
 */
public class ModerationScreen extends MmoScreen {
    private static final String[][] TABS = {{"Joueurs", "players"}, {"Sanctions", "sanctions"}, {"Signalements", "reports"}, {"Historique", "history"},
            {"Journal / rejeu", "journal"}, {"Sauvegardes", "backups"}, {"Connectés", "connected"}, {"Outils", "tools"}, {"Donjons", "dungeons"}};
    private static final int ROW = 24;

    private String tab = "players";
    private CompoundTag state = new CompoundTag();
    private final List<CompoundTag> rows = new ArrayList<>();
    private String sid = "", sname = "";
    private int selRow = -1;
    private int scroll;
    private String message = "";
    private int messageColor = Ui.MUTED;
    private EditBox q, arg, dur;
    private boolean placed;

    public ModerationScreen(CompoundTag first) {
        super("Modération");
        receive(first);
    }

    @Override
    protected int designHeight() {
        return 272;
    }

    @Override
    protected void init() {
        super.init();
        String oq = q == null ? "" : q.getValue(), oa = arg == null ? "" : arg.getValue(), od = dur == null ? "" : dur.getValue();
        q = new EditBox(this.font, left, top, 100, 14, Component.literal("Recherche"));
        q.setMaxLength(32);
        q.setValue(oq);
        q.setResponder(s -> {
            if (tab.equals("players") || tab.equals("journal")) request("state");
        });
        addRenderableWidget(q);
        arg = new EditBox(this.font, left, top, 100, 14, Component.literal("Raison"));
        arg.setMaxLength(200);
        arg.setValue(oa);
        addRenderableWidget(arg);
        dur = new EditBox(this.font, left, top, 50, 14, Component.literal("Durée"));
        dur.setMaxLength(12);
        dur.setValue(od);
        addRenderableWidget(dur);
    }

    /** Vitrine de QA. */
    public ModerationScreen preview(String tabName) {
        tab = tabName;
        return this;
    }

    // ------------------------------------------------------------------ reseau

    private CompoundTag base(String action) {
        CompoundTag t = new CompoundTag();
        t.putString("action", action);
        t.putString("tab", tab);
        t.putString("id", sid);
        t.putString("name", sname);
        t.putString("q", q == null ? "" : q.getValue().strip());
        t.putString("arg", arg == null ? "" : arg.getValue().strip());
        t.putString("dur", dur == null ? "" : dur.getValue().strip());
        return t;
    }

    private void request(String action) {
        ClientNet.send(new Payloads.SocialAction("moderation", base(action)));
    }

    private void act(String action, String... extra) {
        CompoundTag t = base(action);
        for (int i = 0; i + 1 < extra.length; i += 2) t.putString(extra[i], extra[i + 1]);
        if (selRow >= 0 && selRow < rows.size()) {
            CompoundTag r = rows.get(selRow);
            t.putInt("rid", r.getIntOr("rid", -1));
            t.putInt("tid", r.getIntOr("tid", -1));
            t.putString("file", r.getStringOr("file", ""));
            t.putString("dungeon", r.getStringOr("dungeon", ""));
        }
        ClientNet.send(new Payloads.SocialAction("moderation", t));
    }

    public void receive(CompoundTag t) {
        state = t;
        tab = t.getStringOr("tab", tab);
        rows.clear();
        ListTag l = t.getListOrEmpty("rows");
        for (int i = 0; i < l.size(); i++) rows.add(l.getCompoundOrEmpty(i));
        if (tab.equals("tools")) {
            ListTag p = t.getListOrEmpty("plan");
            for (int i = 0; i < p.size(); i++) rows.add(p.getCompoundOrEmpty(i));
        }
        if (selRow >= rows.size()) selRow = -1;
        String st = t.getStringOr("status", "");
        if (!st.isEmpty()) {
            message = st;
            messageColor = t.getBooleanOr("ok", false) ? Ui.GREEN : Ui.RED;
        }
    }

    private void switchTab(String id) {
        tab = id;
        selRow = -1;
        scroll = 0;
        message = "";
        request("state");
    }

    private void select(int i) {
        selRow = i;
        CompoundTag r = rows.get(i);
        if (!r.getStringOr("id", "").isEmpty() && !r.getStringOr("name", "").isEmpty()) {
            sid = r.getStringOr("id", "");
            sname = r.getStringOr("name", "");
            request("state");
        }
    }

    // ------------------------------------------------------------------ rendu

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        int lx = left + 6, lw = 104;
        Ui.header(g, lx, top + 8, lw, "MODÉRATION");
        for (int i = 0; i < TABS.length; i++) {
            int y = top + 24 + i * 21;
            boolean sel = TABS[i][1].equals(tab);
            Ui.inset(g, lx, y, lw, 19);
            if (sel || hovered(lx, y, lw, 19)) g.fill(lx + 1, y + 1, lx + lw - 1, y + 18, sel ? 0x60E2B85A : 0x30FFFFFF);
            g.text(font, TABS[i][0], lx + 6, y + 6, sel ? Ui.GOLD_LIGHT : Ui.TEXT, false);
            String badge = TABS[i][1].equals("reports") && state.getIntOr("openReports", 0) > 0 ? String.valueOf(state.getIntOr("openReports", 0))
                    : TABS[i][1].equals("sanctions") && state.getIntOr("jailed", 0) > 0 ? String.valueOf(state.getIntOr("jailed", 0)) : "";
            if (!badge.isEmpty()) g.text(font, badge, lx + lw - 6 - font.width(badge), y + 6, Ui.RED, false);
            final String id = TABS[i][1];
            click(lx, y, lw, 19, () -> switchTab(id));
        }
        int rx = left + 116, rw = pw - 122;
        String title = "";
        for (String[] t : TABS) if (t[1].equals(tab)) title = t[0];
        String maint = state.getStringOr("maintenance", "");
        String head = title.toUpperCase() + "  ·  " + state.getIntOr("online", 0) + " en ligne" + (maint.isEmpty() ? "" : "  ·  MAINTENANCE " + maint);
        Ui.header(g, rx, top + 8, rw, head);

        boolean search = tab.equals("players") || tab.equals("journal");
        int y0 = top + 26;
        placeBox(q, rx, y0, 90, search);
        if (search) {
            q.setHint(Component.literal(tab.equals("journal") ? "minutes (60)" : "Rechercher...").withColor(0xFF807868));
            y0 += 18;
        }
        int bottom = top + ph - 62;
        renderRows(g, rx, y0, rw, bottom);
        renderControls(g, rx, bottom + 4, rw);
    }

    private void placeBox(EditBox b, int x, int y, int w, boolean visible) {
        b.visible = visible;
        b.setX(x);
        b.setY(y);
        b.setWidth(w);
    }

    private void renderRows(GuiGraphicsExtractor g, int rx, int y0, int rw, int y1) {
        int n = Math.max(1, (y1 - y0) / ROW);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - n)));
        if (rows.isEmpty()) g.text(font, "Aucune entrée.", rx + 4, y0 + 4, Ui.MUTED, false);
        for (int i = 0; i < n && scroll + i < rows.size(); i++) {
            int idx = scroll + i;
            CompoundTag r = rows.get(idx);
            int y = y0 + i * ROW;
            boolean sel = idx == selRow || (!sid.isEmpty() && sid.equals(r.getStringOr("id", "")) && !tab.equals("history") && !tab.equals("journal"));
            Ui.inset(g, rx, y, rw, ROW - 2);
            if (sel) g.fill(rx + 1, y + 1, rx + rw - 1, y + ROW - 3, 0x50E2B85A);
            else if (hovered(rx, y, rw, ROW - 2)) g.fill(rx + 1, y + 1, rx + rw - 1, y + ROW - 3, 0x28FFFFFF);
            boolean closed = r.getBooleanOr("closed", false);
            g.text(font, font.plainSubstrByWidth(r.getStringOr("a", ""), rw / 2 - 6), rx + 5, y + 3, closed ? Ui.MUTED : Ui.TEXT, false);
            String b = r.getStringOr("b", "");
            g.text(font, font.plainSubstrByWidth(b, rw / 2 - 6), rx + rw - 5 - Math.min(font.width(b), rw / 2 - 6), y + 3, Ui.MUTED, false);
            String c = r.getStringOr("c", "");
            if (!c.isEmpty()) g.text(font, font.plainSubstrByWidth(c, rw - 10), rx + 5, y + 13, c.startsWith("silence") || c.contains("prison") ? Ui.RED : Ui.GOLD_LIGHT, false);
            click(rx, y, rw, ROW - 2, () -> select(idx));
        }
    }

    private void renderControls(GuiGraphicsExtractor g, int rx, int y, int rw) {
        boolean hasSel = !sid.isEmpty();
        boolean text = !tab.equals("dungeons");
        placeBox(arg, rx, y, rw - 58, text);
        arg.setHint(Component.literal(tab.equals("backups") ? "minutes à annuler" : tab.equals("tools") ? "texte / raison / rayon" : "raison / texte").withColor(0xFF807868));
        placeBox(dur, rx + rw - 54, y, 54, text);
        dur.setHint(Component.literal("durée").withColor(0xFF807868));
        int by = y + 18;
        String who = hasSel ? sname : "aucun joueur";
        if (!message.isEmpty()) g.text(font, font.plainSubstrByWidth(message, rw), rx, y + 18 + 2 * 15 + 2, messageColor, false);
        else g.text(font, font.plainSubstrByWidth("Joueur : " + who, rw), rx, y + 18 + 2 * 15 + 2, hasSel ? Ui.GOLD_LIGHT : Ui.MUTED, false);
        List<String[]> btns = new ArrayList<>();
        switch (tab) {
            case "tools" -> {
                btns.add(new String[]{"Maintenance", "maint"});
                btns.add(new String[]{"Fin maint.", "maint_off"});
                btns.add(new String[]{"Zone prison ici", "jailzone"});
                btns.add(new String[]{"Rayon prison", "jailrayon"});
                btns.add(new String[]{"+ Annonce", "plan_a"});
                btns.add(new String[]{"+ Commande", "plan_c"});
                btns.add(new String[]{"Suppr. tâche", "plan_del"});
            }
            case "dungeons" -> {
                btns.add(new String[]{"Entrer en setup", "dungeon_enter"});
                btns.add(new String[]{"Menu du setup", "dungeon_menu"});
            }
            case "backups" -> {
                btns.add(new String[]{"Sauvegarder", "snapshot"});
                btns.add(new String[]{"Restaurer", "restore"});
                btns.add(new String[]{"Annuler N min", "undo"});
            }
            default -> {
                boolean muted = state.getBooleanOr("sel_muted", false), jailed = state.getBooleanOr("sel_jailed", false);
                btns.add(new String[]{"Avertir", "warn"});
                btns.add(new String[]{muted ? "Rendre parole" : "Silence", muted ? "unmute" : "mute"});
                btns.add(new String[]{"Expulser", "kick"});
                btns.add(new String[]{"Bannir", "ban"});
                btns.add(new String[]{"Gracier", "pardon"});
                btns.add(new String[]{jailed ? "Libérer" : "Prison", jailed ? "unjail" : "jail"});
                btns.add(new String[]{"Rejoindre", "tpto"});
                btns.add(new String[]{"Amener", "tphere"});
                if (tab.equals("reports")) btns.add(new String[]{"Classer", "report_close"});
            }
        }
        int perRow = 5, bw = (rw - (perRow - 1) * 3) / perRow;
        for (int i = 0; i < btns.size(); i++) {
            int bx = rx + (i % perRow) * (bw + 3), yy = by + (i / perRow) * 15;
            String[] b = btns.get(i);
            boolean enabled = switch (tab) {
                case "tools" -> !b[1].equals("plan_del") || selRow >= 0;
                case "dungeons" -> b[1].equals("dungeon_menu") || selRow >= 0;
                default -> hasSel;
            };
            button(g, bx, yy, bw, 13, b[0], enabled, b[1].equals("ban") || b[1].equals("kick") ? Ui.RED : Ui.GOLD, () -> run(b[1]));
        }
    }

    private void run(String what) {
        switch (what) {
            case "maint" -> act("maint", "do", "start");
            case "maint_off" -> act("maint", "do", "off");
            case "jailzone" -> act("jailzone", "do", "set");
            case "jailrayon" -> act("jailzone", "do", "rayon");
            case "plan_a" -> act("plan_add", "type", "annonce");
            case "plan_c" -> act("plan_add", "type", "commande");
            default -> act(what);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, scroll + (int) -Math.signum(sy));
        return true;
    }
}
