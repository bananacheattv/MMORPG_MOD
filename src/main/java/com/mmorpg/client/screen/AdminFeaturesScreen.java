package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import com.mmorpg.server.FeatureCatalog;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Interrupteurs des fonctionnalites du mod (/fonctionnalites, operateurs) : categories a gauche, une ligne par fonctionnalite avec son etat
 * et un bouton de bascule. Le serveur reverifie la permission, applique le changement sur tout le reseau et le journalise.
 */
public class AdminFeaturesScreen extends MmoScreen {
    private static final int ROW = 26;

    private final List<String> cats = new ArrayList<>(FeatureCatalog.CATEGORIES.keySet());
    private CompoundTag flags = new CompoundTag();
    private int cat;
    private int scroll;
    private String message = "";
    private int messageColor = Ui.MUTED;

    public AdminFeaturesScreen(CompoundTag first) {
        super("Fonctionnalités");
        receive(first);
    }

    public void receive(CompoundTag t) {
        flags = t.getCompoundOrEmpty("flags");
        String st = t.getStringOr("status", "");
        if (!st.isEmpty()) {
            message = st;
            messageColor = t.getBooleanOr("ok", false) ? Ui.GREEN : Ui.RED;
        }
    }

    /** Vitrine de QA : ouvre une categorie donnee. */
    public AdminFeaturesScreen preview(int category) {
        cat = category;
        return this;
    }

    @Override
    protected int designHeight() {
        return 272;
    }

    private boolean on(String key) {
        return flags.getBooleanOr(key, true);
    }

    private void toggle(String key) {
        CompoundTag t = new CompoundTag();
        t.putString("action", "toggle");
        t.putString("key", key);
        t.putBoolean("value", !on(key));
        flags.putBoolean(key, !on(key));
        ClientNet.send(new Payloads.SocialAction("features", t));
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        int lx = left + 6, lw = 120;
        Ui.header(g, lx, top + 8, lw, "CATÉGORIES");
        for (int i = 0; i < cats.size(); i++) {
            int y = top + 24 + i * 20;
            boolean sel = i == cat;
            List<FeatureCatalog.Entry> es = FeatureCatalog.CATEGORIES.get(cats.get(i));
            long active = es.stream().filter(e -> on(e.key())).count();
            String count = active + "/" + es.size();
            final int idx = i;
            Ui.inset(g, lx, y, lw, 18);
            if (sel || hovered(lx, y, lw, 18)) g.fill(lx + 1, y + 1, lx + lw - 1, y + 17, sel ? 0x60E2B85A : 0x30FFFFFF);
            g.text(font, cats.get(i), lx + 6, y + 5, sel ? Ui.GOLD_LIGHT : Ui.TEXT, false);
            g.text(font, count, lx + lw - 6 - font.width(count), y + 5, active == es.size() ? Ui.GREEN : active == 0 ? Ui.RED : Ui.MUTED, false);
            click(lx, y, lw, 18, () -> {
                cat = idx;
                scroll = 0;
            });
        }
        int rx = left + 134, rw = pw - 140;
        String name = cats.get(cat);
        Ui.header(g, rx, top + 8, rw, name.toUpperCase());
        List<FeatureCatalog.Entry> es = FeatureCatalog.CATEGORIES.get(name);
        int y0 = top + 26, y1 = top + ph - 20;
        int rows = Math.max(1, (y1 - y0) / ROW);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, es.size() - rows)));
        for (int i = 0; i < rows && scroll + i < es.size(); i++) {
            FeatureCatalog.Entry e = es.get(scroll + i);
            int y = y0 + i * ROW;
            boolean on = on(e.key());
            Ui.inset(g, rx, y, rw, ROW - 3);
            g.fill(rx + 1, y + 1, rx + 4, y + ROW - 4, on ? Ui.GREEN : Ui.RED);
            g.text(font, e.label(), rx + 10, y + 4, Ui.TEXT, false);
            g.text(font, font.plainSubstrByWidth(e.desc(), rw - 110), rx + 10, y + 14, Ui.MUTED, false);
            button(g, rx + rw - 84, y + 3, 80, ROW - 9, on ? "ACTIVÉE" : "DÉSACTIVÉE", true, on ? Ui.GREEN : Ui.RED, () -> toggle(e.key()));
        }
        if (!message.isEmpty()) g.text(font, font.plainSubstrByWidth(message, rw), rx, top + ph - 12, messageColor, false);
        else g.text(font, "Effet immédiat sur tout le réseau, enregistré dans le journal d'administration.", rx, top + ph - 12, Ui.MUTED, false);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, scroll + (int) -Math.signum(sy));
        return true;
    }
}
