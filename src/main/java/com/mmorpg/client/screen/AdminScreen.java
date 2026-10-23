package com.mmorpg.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Menu d'administration (/adminmenu) : editeur en arbre de toutes les configurations du serveur. On choisit un fichier, on descend dans
 * ses objets et listes, on modifie les valeurs (texte, nombre, oui/non), on ajoute (copie d'une entree existante), on duplique, deplace
 * ou supprime des entrees, puis on enregistre : le serveur verifie la structure, garde l'ancienne version et recharge la configuration.
 */
public class AdminScreen extends MmoScreen {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, String> LABELS = Map.of("general", "Général", "mobs", "Monstres et boss", "quests", "Quêtes", "shop", "Boutique",
            "butins", "Tables de butin", "mannequin", "Mannequins", "cinematique", "Cinématique");
    private static final int ROW = 16;

    private final List<String> files;
    private String file = "";
    private JsonElement root;
    private final List<Object> path = new ArrayList<>();
    private boolean dirty;
    private boolean loading;
    private String msg = "";
    private int msgColor = Ui.MUTED;
    private int scroll;
    private EditBox edit;
    private EditBox filter;
    private EditBox newKey;
    private Object editKey;
    private final StringBuilder incoming = new StringBuilder();

    public AdminScreen(List<String> files) {
        super("Administration");
        this.files = files;
    }

    // ================================================================== reception des paquets

    public static void handle(Payloads.AdminReply r) {
        Minecraft mc = Minecraft.getInstance();
        if (r.kind() == Payloads.AdminReply.OPEN) {
            mc.gui.setScreen(new AdminScreen(List.of(r.text().split("\n"))));
            return;
        }
        if (!(mc.gui.screen() instanceof AdminScreen s)) return;
        switch (r.kind()) {
            case Payloads.AdminReply.FILE -> s.chunk(r);
            case Payloads.AdminReply.OK -> {
                s.msg = r.text();
                s.msgColor = Ui.GREEN;
                s.dirty = false;
                s.request(s.file);
            }
            default -> {
                s.msg = r.text();
                s.msgColor = Ui.RED;
            }
        }
    }

    private void chunk(Payloads.AdminReply r) {
        if (!r.file().equals(file)) return;
        if (r.index() == 0) incoming.setLength(0);
        incoming.append(r.text());
        if (r.index() + 1 < r.count()) return;
        try {
            root = JsonParser.parseString(incoming.toString());
            loading = false;
            normalizePath();
        } catch (Exception e) {
            msg = "Fichier illisible : " + e.getMessage();
            msgColor = Ui.RED;
        }
    }

    private void request(String name) {
        loading = true;
        ClientNet.send(new Payloads.AdminRequest(Payloads.AdminRequest.GET, name, 0, 1, ""));
    }

    private void select(String name) {
        file = name;
        root = null;
        path.clear();
        scroll = 0;
        dirty = false;
        msg = "";
        stopEdit();
        request(name);
    }

    private void save() {
        stopEdit();
        String text = GSON.toJson(root);
        int count = Math.max(1, (text.length() + 8000 - 1) / 8000);
        for (int i = 0; i < count; i++) {
            ClientNet.send(new Payloads.AdminRequest(Payloads.AdminRequest.SAVE, file, i, count, text.substring(i * 8000, Math.min(text.length(), (i + 1) * 8000))));
        }
        msg = "Enregistrement en cours...";
        msgColor = Ui.MUTED;
    }

    // ================================================================== navigation dans l'arbre

    private JsonElement at(List<Object> p) {
        JsonElement cur = root;
        for (Object k : p) {
            if (cur == null) return null;
            if (cur.isJsonObject() && k instanceof String s) cur = cur.getAsJsonObject().get(s);
            else if (cur.isJsonArray() && k instanceof Integer i && i < cur.getAsJsonArray().size()) cur = cur.getAsJsonArray().get(i);
            else return null;
        }
        return cur;
    }

    private JsonElement current() {
        return at(path);
    }

    private void normalizePath() {
        while (!path.isEmpty() && (at(path) == null || !at(path).isJsonObject() && !at(path).isJsonArray())) path.remove(path.size() - 1);
    }

    private static String label(Object key) {
        return key instanceof Integer i ? "#" + (i + 1) : String.valueOf(key);
    }

    /** Premier texte court d'une entree de liste (nom, identifiant...) pour la reconnaitre dans l'arbre. */
    private static String hint(JsonElement e) {
        if (e.isJsonPrimitive()) return "";
        if (e.isJsonObject()) {
            for (String k : new String[]{"displayName", "name", "title", "id", "item", "type", "text"}) {
                JsonElement v = e.getAsJsonObject().get(k);
                if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) return v.getAsString();
            }
        }
        return "";
    }

    private static String summary(JsonElement e) {
        if (e.isJsonObject()) return "{ " + e.getAsJsonObject().size() + " champ(s) }";
        if (e.isJsonArray()) return "[ " + e.getAsJsonArray().size() + " entrée(s) ]";
        if (e.isJsonNull()) return "(vide)";
        return e.getAsString();
    }

    private List<Object> keys(JsonElement c) {
        List<Object> out = new ArrayList<>();
        String q = filter == null ? "" : filter.getValue().toLowerCase(Locale.ROOT).strip();
        if (c.isJsonObject()) {
            for (var en : c.getAsJsonObject().entrySet()) {
                if (q.isEmpty() || en.getKey().toLowerCase(Locale.ROOT).contains(q) || (en.getValue().isJsonPrimitive() && en.getValue().getAsString().toLowerCase(Locale.ROOT).contains(q))
                        || hint(en.getValue()).toLowerCase(Locale.ROOT).contains(q)) out.add(en.getKey());
            }
        } else if (c.isJsonArray()) {
            JsonArray a = c.getAsJsonArray();
            for (int i = 0; i < a.size(); i++) {
                JsonElement v = a.get(i);
                if (q.isEmpty() || hint(v).toLowerCase(Locale.ROOT).contains(q) || (v.isJsonPrimitive() && v.getAsString().toLowerCase(Locale.ROOT).contains(q))) out.add(i);
            }
        }
        return out;
    }

    private static JsonElement child(JsonElement c, Object key) {
        return c.isJsonObject() ? c.getAsJsonObject().get((String) key) : c.getAsJsonArray().get((Integer) key);
    }

    private static void setChild(JsonElement c, Object key, JsonElement v) {
        if (c.isJsonObject()) c.getAsJsonObject().add((String) key, v);
        else c.getAsJsonArray().set((Integer) key, v);
    }

    // ================================================================== edition

    @Override
    protected int panelWidth() {
        return Math.min(this.width - 12, 600);
    }

    @Override
    protected int panelHeight() {
        return Math.min(this.height - 12, 340);
    }

    @Override
    protected void init() {
        super.init();
        edit = new EditBox(this.font, left, top, 200, 14, Component.literal("Valeur"));
        edit.setMaxLength(2000);
        edit.visible = false;
        this.addRenderableWidget(edit);
        filter = new EditBox(this.font, left + 116, top + 44, 170, 14, Component.literal("Filtre"));
        filter.setHint(Component.literal("Filtrer cette liste...").withColor(0xFF807868));
        filter.setMaxLength(40);
        filter.setResponder(s -> scroll = 0);
        this.addRenderableWidget(filter);
        newKey = new EditBox(this.font, left + 116, top + ph - 48, 140, 14, Component.literal("Nouvelle entrée"));
        newKey.setHint(Component.literal("Nom de la nouvelle entrée").withColor(0xFF807868));
        newKey.setMaxLength(60);
        this.addRenderableWidget(newKey);
        if (!file.isEmpty() && root == null) request(file);
    }

    private void startEdit(Object key, JsonElement value, int x, int y, int w) {
        stopEdit();
        editKey = key;
        edit.setX(x);
        edit.setY(y);
        edit.setWidth(Math.max(60, w));
        edit.setValue(value.getAsString());
        edit.visible = true;
        edit.setFocused(true);
        this.setFocused(edit);
    }

    private void stopEdit() {
        editKey = null;
        if (edit != null) {
            edit.visible = false;
            edit.setFocused(false);
        }
    }

    private void commitEdit() {
        JsonElement cur = current();
        if (editKey == null || cur == null) {
            stopEdit();
            return;
        }
        JsonElement old = child(cur, editKey);
        String t = edit.getValue();
        try {
            JsonPrimitive p = old.getAsJsonPrimitive();
            if (p.isNumber()) {
                String o = p.getAsString();
                boolean decimal = o.contains(".") || o.contains("e") || o.contains("E");
                setChild(cur, editKey, new JsonPrimitive(decimal ? (Number) Double.parseDouble(t.strip().replace(',', '.')) : (Number) Long.parseLong(t.strip())));
            } else {
                setChild(cur, editKey, new JsonPrimitive(t));
            }
            dirty = true;
        } catch (NumberFormatException e) {
            msg = "Valeur numérique invalide : « " + t + " »";
            msgColor = Ui.RED;
        }
        stopEdit();
    }

    private void addEntry() {
        JsonElement cur = current();
        if (cur == null) return;
        if (cur.isJsonArray()) {
            JsonArray a = cur.getAsJsonArray();
            a.add(a.isEmpty() ? new JsonPrimitive("") : a.get(a.size() - 1).deepCopy());
            dirty = true;
            scroll = Math.max(0, a.size() - 1);
        } else if (cur.isJsonObject()) {
            String name = newKey.getValue().strip();
            JsonObject o = cur.getAsJsonObject();
            if (name.isEmpty() || o.has(name)) {
                msg = name.isEmpty() ? "Saisissez le nom de la nouvelle entrée." : "Cette entrée existe déjà.";
                msgColor = Ui.RED;
                return;
            }
            JsonElement model = o.size() == 0 ? new JsonPrimitive("") : o.entrySet().iterator().next().getValue().deepCopy();
            o.add(name, model);
            newKey.setValue("");
            dirty = true;
            msg = "Entrée « " + name + " » ajoutée (copie de la première) : modifiez-la.";
            msgColor = Ui.GREEN;
        }
    }

    // ================================================================== rendu

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "ADMINISTRATION AELORIA", Ui.GOLD_LIGHT);
        button(g, left + pw - 92, top + 6, 84, 14, "Déconnexion", true, 0xFF6A3A2A, () -> {
            ClientNet.send(new Payloads.AdminRequest(Payloads.AdminRequest.LOGOUT, "", 0, 1, ""));
            onClose();
        });
        // fichiers
        int y = top + 26;
        for (String f : files) {
            final String name = f;
            button(g, left + 8, y, 100, 16, LABELS.getOrDefault(f, f), true, f.equals(file) ? 0xFFE2B85A : 0xFF3B3350, () -> select(name));
            y += 19;
        }
        button(g, left + 8, y + 4, 100, 16, "Recharger", !file.isEmpty(), 0xFF2A5A4A, () -> {
            dirty = false;
            msg = "Fichier rechargé depuis le serveur.";
            msgColor = Ui.MUTED;
            stopEdit();
            request(file);
        });
        Ui.wrap(g, "Appliqué à l'enregistrement. L'ancien fichier est conservé dans config/mmorpg/sauvegardes.", left + 8, y + 30, 98, Ui.MUTED);
        filter.visible = root != null;
        newKey.visible = false;
        int x1 = left + 116;
        int w1 = pw - 124;
        if (file.isEmpty()) {
            g.centeredText(font, Component.literal("Choisissez un fichier de configuration à gauche."), x1 + w1 / 2, top + ph / 2 - 10, Ui.MUTED);
            return;
        }
        if (root == null) {
            g.centeredText(font, Component.literal(loading ? "Chargement..." : "Aucune donnée."), x1 + w1 / 2, top + ph / 2 - 10, Ui.MUTED);
            return;
        }
        // fil d'Ariane
        int bx = x1;
        List<Object> sub = new ArrayList<>();
        for (int i = -1; i < path.size(); i++) {
            String t = i < 0 ? LABELS.getOrDefault(file, file) : label(path.get(i));
            String shown = font.plainSubstrByWidth(t, 110);
            int bw = font.width(shown) + 10;
            if (bx + bw > x1 + w1) break;
            final int depth = i + 1;
            button(g, bx, top + 26, bw, 14, shown, true, i == path.size() - 1 ? 0xFFE2B85A : 0xFF3B3350, () -> {
                stopEdit();
                while (path.size() > depth) path.remove(path.size() - 1);
                scroll = 0;
            });
            bx += bw + 3;
        }
        JsonElement cur = current();
        if (cur == null) return;
        List<Object> ks = keys(cur);
        g.text(font, ks.size() + " / " + (cur.isJsonObject() ? cur.getAsJsonObject().size() : cur.getAsJsonArray().size()) + " entrée(s)", x1 + 176, top + 47, Ui.MUTED, false);
        int listTop = top + 64;
        int listBottom = top + ph - 54;
        int visible = Math.max(1, (listBottom - listTop) / ROW);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, ks.size() - visible)));
        Ui.inset(g, x1, listTop - 2, w1, listBottom - listTop + 4);
        boolean array = cur.isJsonArray();
        int actionsW = array ? 70 : 18;
        int valueX = x1 + 150;
        int valueW = w1 - 150 - actionsW - 8;
        for (int i = 0; i < Math.min(visible, ks.size() - scroll); i++) {
            Object key = ks.get(i + scroll);
            JsonElement v = child(cur, key);
            int ry = listTop + i * ROW;
            boolean hot = hovered(x1 + 2, ry, w1 - 4, ROW - 1);
            if (hot) g.fill(x1 + 2, ry, x1 + w1 - 2, ry + ROW - 1, 0x30FFFFFF);
            String k = label(key);
            String h = array ? hint(v) : "";
            g.text(font, font.plainSubstrByWidth(array && !h.isEmpty() ? k + " " + h : k, 142), x1 + 6, ry + 4, 0xFFE8D8A8, false);
            if (v.isJsonObject() || v.isJsonArray()) {
                g.text(font, "▸ " + summary(v), valueX, ry + 4, 0xFF9FC8FF, false);
                click(x1 + 2, ry, w1 - actionsW - 6, ROW - 1, () -> {
                    stopEdit();
                    path.add(key);
                    scroll = 0;
                    filter.setValue("");
                });
            } else if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean()) {
                boolean on = v.getAsBoolean();
                g.text(font, on ? "oui" : "non", valueX, ry + 4, on ? Ui.GREEN : Ui.RED, true);
                click(x1 + 2, ry, w1 - actionsW - 6, ROW - 1, () -> {
                    setChild(cur, key, new JsonPrimitive(!on));
                    dirty = true;
                });
            } else {
                boolean editing = key.equals(editKey);
                if (!editing) {
                    boolean num = v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber();
                    g.text(font, font.plainSubstrByWidth(summary(v), valueW), valueX, ry + 4, num ? 0xFF9AE6FF : Ui.TEXT, false);
                }
                final int ex = valueX - 2;
                final int ey = ry;
                final int ew = valueW;
                click(x1 + 2, ry, w1 - actionsW - 6, ROW - 1, () -> startEdit(key, v, ex, ey - 1, ew));
                if (editing) {
                    edit.setX(valueX - 2);
                    edit.setY(ry - 1);
                    edit.setWidth(Math.max(60, valueW));
                }
            }
            int ax = x1 + w1 - actionsW - 2;
            if (array) {
                final int idx = (Integer) key;
                JsonArray arr = cur.getAsJsonArray();
                button(g, ax, ry, 16, ROW - 2, "▲", idx > 0, 0xFF3B3350, () -> {
                    java.util.Collections.swap(arr.asList(), idx, idx - 1);
                    dirty = true;
                    stopEdit();
                });
                button(g, ax + 17, ry, 16, ROW - 2, "▼", idx < arr.size() - 1, 0xFF3B3350, () -> {
                    java.util.Collections.swap(arr.asList(), idx, idx + 1);
                    dirty = true;
                    stopEdit();
                });
                button(g, ax + 34, ry, 16, ROW - 2, "+", true, 0xFF2A6A3A, () -> {
                    arr.asList().add(idx + 1, arr.get(idx).deepCopy());
                    dirty = true;
                    stopEdit();
                });
                button(g, ax + 51, ry, 16, ROW - 2, "x", true, 0xFF6A3A2A, () -> {
                    arr.remove(idx);
                    dirty = true;
                    stopEdit();
                });
            } else {
                button(g, ax + 50, ry, 16, ROW - 2, "x", true, 0xFF6A3A2A, () -> {
                    cur.getAsJsonObject().remove((String) key);
                    dirty = true;
                    stopEdit();
                });
            }
        }
        // ajout d'une entree
        int fy = top + ph - 48;
        if (cur.isJsonObject()) {
            newKey.visible = true;
            newKey.setY(fy);
            button(g, x1 + 146, fy - 1, 150, 16, "Ajouter (copie de la 1re)", true, 0xFF2A6A3A, this::addEntry);
        } else {
            button(g, x1, fy - 1, 190, 16, "Ajouter (copie de la dernière)", true, 0xFF2A6A3A, this::addEntry);
        }
        // enregistrement
        int by = top + ph - 26;
        button(g, x1, by, 86, 18, dirty ? "Enregistrer *" : "Enregistrer", dirty, 0xFF2A6A3A, this::save);
        button(g, x1 + 90, by, 70, 18, "Annuler", dirty, 0xFF6A3A2A, () -> {
            dirty = false;
            stopEdit();
            msg = "Changements annulés.";
            msgColor = Ui.MUTED;
            request(file);
        });
        String shown = msg.isEmpty() ? (dirty ? "Modifications non enregistrées" : "") : msg;
        Ui.wrap(g, shown, x1 + 168, by - 1, w1 - 172, msg.isEmpty() ? 0xFFE0B050 : msgColor);
    }

    // ================================================================== clavier, souris

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (edit != null && edit.visible && edit.isFocused()) {
            if (event.key() == 257 || event.key() == 335) {
                commitEdit();
                return true;
            }
            if (event.key() == 256) {
                stopEdit();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (edit != null && edit.visible && !edit.isMouseOver(event.x(), event.y())) {
            Object before = editKey;
            commitEdit();
            // le clic qui valide une saisie ne declenche pas aussi la ligne en dessous si elle est la meme
            if (before != null) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, scroll - (int) Math.signum(sy) * 2);
        return true;
    }
}
