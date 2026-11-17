package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Menu de la baguette de zone (operateurs) : cree une zone de chasse a partir des deux coins choisis (identifiant, niveaux, monstres, reglages)
 * et gere les zones existantes. Le serveur reverifie la permission et les valeurs.
 */
public class HuntZoneScreen extends MmoScreen {
    private record Row(String id, String nom, int min, int max, boolean actif, int mobs) {
    }

    private final List<String> mobKeys = new ArrayList<>();
    private final List<Row> zones = new ArrayList<>();
    private final Set<String> picked = new LinkedHashSet<>();
    private boolean selection;
    private int w, d;
    private int min = 1, max = 10, cap = 24, safe, zoneScroll, mobScroll;
    private boolean harvest;
    private String message = "";
    private int messageColor = Ui.MUTED;
    private EditBox id, nom;

    public HuntZoneScreen(CompoundTag t) {
        super("Zones de chasse");
        receive(t);
    }

    public void receive(CompoundTag t) {
        selection = t.getBooleanOr("selection", false);
        w = t.getIntOr("w", 0);
        d = t.getIntOr("d", 0);
        mobKeys.clear();
        ListTag ms = t.getListOrEmpty("mobs");
        for (int i = 0; i < ms.size(); i++) mobKeys.add(ms.getStringOr(i, ""));
        zones.clear();
        ListTag zs = t.getListOrEmpty("zones");
        for (int i = 0; i < zs.size(); i++) {
            CompoundTag e = zs.getCompoundOrEmpty(i);
            zones.add(new Row(e.getStringOr("id", ""), e.getStringOr("nom", ""), e.getIntOr("min", 1), e.getIntOr("max", 1), e.getBooleanOr("actif", true), e.getIntOr("mobs", 0)));
        }
        String st = t.getStringOr("status", "");
        if (!st.isEmpty()) {
            message = st;
            messageColor = t.getBooleanOr("ok", false) ? Ui.GREEN : Ui.RED;
            if (t.getBooleanOr("ok", false) && st.contains("créée")) {
                picked.clear();
                if (id != null) {
                    id.setValue("");
                    nom.setValue("");
                }
            }
        }
    }

    /** Vitrine de QA : remplit un exemple. */
    public HuntZoneScreen preview() {
        min = 1;
        max = 18;
        picked.addAll(List.of("gobelin", "loup_sombre"));
        return this;
    }

    @Override
    protected int designHeight() {
        return 272;
    }

    @Override
    protected void init() {
        super.init();
        int rx = left + 144;
        String oldId = id == null ? "" : id.getValue(), oldNom = nom == null ? "" : nom.getValue();
        id = new EditBox(this.font, rx, top + 38, 108, 16, Component.literal("Identifiant"));
        id.setHint(Component.literal("identifiant (ex. prairies)").withColor(0xFF807868));
        id.setMaxLength(24);
        id.setValue(oldId);
        this.addRenderableWidget(id);
        nom = new EditBox(this.font, rx + 114, top + 38, pw - 144 - 114 - 6, 16, Component.literal("Nom"));
        nom.setHint(Component.literal("nom affiché (facultatif)").withColor(0xFF807868));
        nom.setMaxLength(40);
        nom.setValue(oldNom);
        this.addRenderableWidget(nom);
    }

    private void send(String action, String zoneId) {
        CompoundTag t = new CompoundTag();
        t.putString("action", action);
        t.putString("id", zoneId);
        ClientNet.send(new Payloads.SocialAction("huntzone", t));
    }

    private void create() {
        CompoundTag t = new CompoundTag();
        t.putString("action", "create");
        t.putString("id", id.getValue());
        t.putString("nom", nom.getValue());
        t.putInt("min", min);
        t.putInt("max", max);
        t.putInt("maxMobs", cap);
        t.putInt("sure", safe);
        t.putBoolean("recolte", harvest);
        ListTag l = new ListTag();
        for (String k : picked) l.add(StringTag.valueOf(k));
        t.put("mobs", l);
        ClientNet.send(new Payloads.SocialAction("huntzone", t));
    }

    private void stepper(GuiGraphicsExtractor g, int x, int y, String label, int value, int lo, int hi, int step, java.util.function.IntConsumer set) {
        g.text(font, label, x, y + 4, Ui.TEXT, false);
        int bx = x + 72;
        button(g, bx, y, 16, 14, "-", value > lo, Ui.GOLD, () -> set.accept(Math.max(lo, value - step)));
        g.centeredText(font, Component.literal(String.valueOf(value)), bx + 33, y + 4, Ui.GOLD_LIGHT);
        button(g, bx + 50, y, 16, 14, "+", value < hi, Ui.GOLD, () -> set.accept(Math.min(hi, value + step)));
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "ZONES DE CHASSE", Ui.GOLD_LIGHT);
        // ---- zones existantes
        int lx = left + 6, lw = 130;
        Ui.header(g, lx, top + 22, lw, "EXISTANTES (" + zones.size() + ")");
        int rows = 8, rh = 27;
        zoneScroll = Math.max(0, Math.min(zoneScroll, Math.max(0, zones.size() - rows)));
        if (zones.isEmpty()) g.text(font, "Aucune zone.", lx + 4, top + 40, Ui.MUTED, false);
        for (int i = 0; i < rows && zoneScroll + i < zones.size(); i++) {
            Row z = zones.get(zoneScroll + i);
            int y = top + 36 + i * rh;
            Ui.inset(g, lx, y, lw, rh - 3);
            g.fill(lx + 1, y + 1, lx + 4, y + rh - 4, z.actif ? Ui.GREEN : Ui.RED);
            g.text(font, font.plainSubstrByWidth(z.nom, lw - 12), lx + 8, y + 3, Ui.TEXT, false);
            g.text(font, "niv. " + z.min + "-" + z.max, lx + 8, y + 13, Ui.MUTED, false);
            String zid = z.id;
            button(g, lx + lw - 56, y + 11, 16, 11, "●", true, z.actif ? Ui.GREEN : Ui.RED, () -> send("toggle", zid));
            button(g, lx + lw - 38, y + 11, 16, 11, "TP", true, Ui.BLUE, () -> send("tp", zid));
            button(g, lx + lw - 20, y + 11, 16, 11, "x", true, Ui.RED, () -> send("delete", zid));
        }
        // ---- creation
        int rx = left + 144, rw = pw - 150;
        Ui.header(g, rx, top + 22, rw, selection ? "NOUVELLE ZONE · " + w + " × " + d + " blocs" : "NOUVELLE ZONE");
        if (!selection) {
            g.text(font, "Clic droit sur 2 blocs avec la baguette (coins A et B),", rx, top + 62, Ui.MUTED, false);
            g.text(font, "puis clic droit dans le vide pour rouvrir ce menu.", rx, top + 74, Ui.MUTED, false);
        }
        int y = top + 60;
        stepper(g, rx, y, "Niveau min", min, 1, 200, 1, v -> {
            min = v;
            if (max < min) max = min;
        });
        stepper(g, rx + 150, y, "Niveau max", max, 1, 200, 1, v -> max = Math.max(v, min));
        y += 18;
        stepper(g, rx, y, "Monstres max", cap, 1, 200, 2, v -> cap = v);
        stepper(g, rx + 150, y, "Zone sûre", safe, 0, 500, 5, v -> safe = v);
        y += 18;
        button(g, rx, y, 150, 14, "Récolte des cultures : " + (harvest ? "OUI" : "non"), true, harvest ? Ui.GREEN : Ui.GOLD_DARK, () -> harvest = !harvest);
        g.text(font, picked.size() + " monstre(s) choisi(s)", rx + 156, y + 4, picked.isEmpty() ? Ui.RED : Ui.GREEN, false);
        // ---- monstres
        y += 20;
        int cols = 3, cw = (rw - (cols - 1) * 3) / cols, ch = 14;
        int bottom = top + ph - 40;
        int rowsM = Math.max(1, (bottom - y) / (ch + 2));
        int totalRows = (mobKeys.size() + cols - 1) / cols;
        mobScroll = Math.max(0, Math.min(mobScroll, Math.max(0, totalRows - rowsM)));
        for (int r = 0; r < rowsM && mobScroll + r < totalRows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = (mobScroll + r) * cols + c;
                if (idx >= mobKeys.size()) break;
                String k = mobKeys.get(idx);
                boolean on = picked.contains(k);
                int bx = rx + c * (cw + 3), by = y + r * (ch + 2);
                button(g, bx, by, cw, ch, font.plainSubstrByWidth(k, cw - 8), true, on ? Ui.GREEN : Ui.GOLD_DARK, () -> {
                    if (!picked.remove(k)) picked.add(k);
                });
            }
        }
        // ---- validation
        int by = top + ph - 32;
        button(g, rx, by, 120, 18, "Créer la zone", selection && !picked.isEmpty() && !id.getValue().isBlank(), 0xFF3A8A4A, this::create);
        button(g, rx + 126, by, 80, 18, "Effacer coins", selection, Ui.GOLD_DARK, () -> send("clear", ""));
        g.text(font, font.plainSubstrByWidth(message.isEmpty() ? "Molette : défiler les monstres." : message, rw), rx, top + ph - 10, message.isEmpty() ? Ui.MUTED : messageColor, false);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        int dir = (int) -Math.signum(sy);
        if (mouseX < left + 140) zoneScroll = Math.max(0, zoneScroll + dir);
        else mobScroll = Math.max(0, mobScroll + dir);
        return true;
    }
}
