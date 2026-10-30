package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gestion complete d'un joueur (/adminjoueurs, operateurs) : liste des joueurs (en ligne et, avec la base du reseau, hors ligne), puis quatre pages :
 * Resume (champs principaux), Donnees (arbre de toutes les donnees RPG, modifiable), Objets (inventaire, Ender, banque, hotel des ventes) et Actions
 * (soin, sanctions, teleportation, reinitialisations...). Le serveur reverifie la permission et journalise chaque modification.
 */
public class AdminPlayersScreen extends MmoScreen {
    private enum Page {
        RESUME("Résumé"), DONNEES("Données"), OBJETS("Objets"), ACTIONS("Actions");
        final String label;

        Page(String label) {
            this.label = label;
        }
    }

    private static final int ROW = 14;
    private static final String[][] FIELDS = {
            {"Niveau", "level"}, {"Expérience", "xp"}, {"Classe", "class"}, {"Palier d'évolution", "evolution"}, {"Points d'attributs", "attrPoints"},
            {"Or", "gold"}, {"Or en banque", "@bank"}, {"Points de vie", "hp"}, {"Mana", "mana"}, {"Titre PvP", "pvpTitle"}, {"Rang (texte)", "rankLabel"},
            {"Emplacements HDV", "auctionSlots"}, {"Mines ouvertes", "minesUnlocked"}, {"Plaines ouvertes", "plainsUnlocked"}, {"Familier actif", "activePet"},
            {"Quête suivie", "trackedQuest"}};
    private static final String[][] SHORTCUTS = {
            {"Compétences", "skills"}, {"Barre de compétences", "bar"}, {"Attributs alloués", "allocated"}, {"Métiers (XP)", "jobXp"}, {"Récompenses de métiers", "jobClaimed"},
            {"Succès", "achievements"}, {"Compteurs de succès", "stats"}, {"Quêtes en cours", "activeQuests"}, {"Quêtes terminées", "completedQuests"},
            {"Quêtes journalières", "dailyQuests"}, {"Familiers (XP)", "pets"}, {"Montures", "mounts"}, {"Cosmétiques", "cosmetics"}, {"Cosmétiques équipés", "equipped"},
            {"Bestiaire (victoires)", "kills"}, {"Sacs à dos", "backpacks"}, {"Recettes de forge", "forgeRecipes"}, {"Guilde (niveau/bonus)", "guild_level"}};
    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("level", "Niveau"), Map.entry("xp", "Expérience"), Map.entry("class", "Classe"), Map.entry("attrPoints", "Points d'attributs"),
            Map.entry("allocated", "Attributs alloués"), Map.entry("skills", "Compétences"), Map.entry("bar", "Barre de compétences"), Map.entry("pets", "Familiers (XP)"),
            Map.entry("mounts", "Montures"), Map.entry("cosmetics", "Cosmétiques"), Map.entry("equipped", "Équipés"), Map.entry("kills", "Bestiaire"),
            Map.entry("jobXp", "XP des métiers"), Map.entry("jobClaimed", "Récompenses de métiers"), Map.entry("stats", "Compteurs de succès"),
            Map.entry("achievements", "Succès"), Map.entry("activeQuests", "Quêtes en cours"), Map.entry("completedQuests", "Quêtes terminées"),
            Map.entry("dailyQuests", "Quêtes journalières"), Map.entry("backpacks", "Sacs à dos"), Map.entry("gold", "Or"), Map.entry("evolution", "Évolution"),
            Map.entry("forgeRecipes", "Recettes de forge"), Map.entry("waypoints", "Points de passage"), Map.entry("pvpTitle", "Titre"), Map.entry("hp", "PV"));
    /** {libelle, action, confirmation, besoin d'etre en ligne}. */
    private static final String[][] ACTIONS = {
            {"Soigner", "heal", "", "1"}, {"Tuer", "kill", "1", "1"}, {"Expulser", "kick", "1", "1"}, {"Bannir", "ban", "1", "0"}, {"Gracier (débannir)", "pardon", "", "0"},
            {"Réduire au silence", "mute", "", "0"}, {"Rendre la parole", "unmute", "", "0"}, {"Aller le rejoindre", "tpto", "", "1"}, {"L'amener à moi", "tphere", "", "1"},
            {"Mode de jeu (valeur)", "gamemode", "", "1"}, {"Quitter son groupe", "partyleave", "1", "1"}, {"Quitter sa guilde", "guildleave", "1", "0"},
            {"Réinit. attributs", "resetattr", "1", "0"}, {"Réinit. classe", "resetclass", "1", "0"}, {"Réinit. quêtes", "resetquests", "1", "0"},
            {"Réinit. métiers", "resetjobs", "1", "0"}, {"Vider inventaire", "clearinv", "1", "1"}, {"Vider Ender", "clearender", "1", "1"},
            {"Vider la banque", "clearbank", "1", "0"}, {"TOUT réinitialiser", "resetall", "1", "0"}};

    private Page page = Page.RESUME;
    private final List<CompoundTag> players = new ArrayList<>();
    private boolean dbOn;
    private CompoundTag cur;
    private String curId = "";
    private final List<String> path = new ArrayList<>();
    private String selKey;                    // champ selectionne (dans le noeud courant, ou "@bank")
    private int listScroll;
    private int scroll;
    private String message = "";
    private int messageColor = Ui.MUTED;
    private String confirm = "";
    private long confirmUntil;
    private EditBox search, value, key;

    public AdminPlayersScreen(CompoundTag first) {
        super("Gestion des joueurs");
        receive(first);
    }

    @Override
    protected int designHeight() {
        return 272;
    }

    @Override
    protected void init() {
        super.init();
        // init() repasse au retour du selecteur d'objets (et au redimensionnement) : garder ce qui etait saisi
        String oldSearch = search == null ? "" : search.getValue(), oldValue = value == null ? "" : value.getValue(), oldKey = key == null ? "" : key.getValue();
        search = new EditBox(this.font, left + 6, top + 6, 140, 14, Component.literal("Recherche"));
        search.setHint(Component.literal("Rechercher un joueur...").withColor(0xFF807868));
        search.setMaxLength(16);
        search.setValue(oldSearch);
        search.setResponder(s -> requestList());
        this.addRenderableWidget(search);
        value = new EditBox(this.font, left, top, 100, 14, Component.literal("Valeur"));
        value.setMaxLength(2000);
        value.setValue(oldValue);
        value.visible = false;
        this.addRenderableWidget(value);
        key = new EditBox(this.font, left, top, 100, 14, Component.literal("Nom"));
        key.setMaxLength(128);
        key.setValue(oldKey);
        key.visible = false;
        this.addRenderableWidget(key);
    }

    /** Vitrine de QA : ouvre une page donnee (0 = resume, 1 = donnees, 2 = objets, 3 = actions) sur le chemin indique. */
    public AdminPlayersScreen preview(int pageIndex, String... where) {
        page = Page.values()[pageIndex];
        path.clear();
        path.addAll(List.of(where));
        return this;
    }

    // ================================================================== reseau

    private void send(CompoundTag t) {
        ClientNet.send(new Payloads.SocialAction("playeradmin", t));
    }

    private void requestList() {
        CompoundTag t = new CompoundTag();
        t.putString("action", "list");
        t.putString("q", search == null ? "" : search.getValue().strip());
        send(t);
    }

    private CompoundTag base(String action) {
        CompoundTag t = new CompoundTag();
        t.putString("action", action);
        t.putString("id", curId);
        t.putString("name", cur == null ? "?" : cur.getStringOr("name", "?"));
        return t;
    }

    private void select(String id) {
        curId = id;
        cur = null;
        path.clear();
        selKey = null;
        scroll = 0;
        message = "";
        send(base("get"));
    }

    public void receive(CompoundTag t) {
        switch (t.getStringOr("kind", "")) {
            case "list" -> {
                players.clear();
                ListTag l = t.getListOrEmpty("players");
                for (int i = 0; i < l.size(); i++) players.add(l.getCompoundOrEmpty(i));
                dbOn = t.getBooleanOr("db", false);
            }
            case "profile" -> {
                boolean same = t.getStringOr("id", "").equals(curId);
                cur = t;
                curId = t.getStringOr("id", "");
                if (!same) {
                    path.clear();
                    selKey = null;
                }
                normalize();
                String st = t.getStringOr("status", "");
                if (!st.isEmpty()) setMessage(st, true);
                if (value != null && selKey != null) value.setValue(valueOf(selKey));
            }
            default -> setMessage(t.getStringOr("status", ""), t.getBooleanOr("ok", false));
        }
    }

    private void setMessage(String m, boolean ok) {
        message = m;
        messageColor = ok ? Ui.GREEN : Ui.RED;
    }

    // ================================================================== donnees

    private Tag node(List<String> p) {
        if (cur == null) return null;
        Tag n = cur.getCompoundOrEmpty("data");
        for (String k : p) {
            if (n instanceof CompoundTag c) n = c.get(k);
            else if (n instanceof ListTag l) n = idx(k) < l.size() && idx(k) >= 0 ? l.get(idx(k)) : null;
            else if (n instanceof IntArrayTag a) return idx(k) < a.size() && idx(k) >= 0 ? net.minecraft.nbt.IntTag.valueOf(a.getAsIntArray()[idx(k)]) : null;
            else if (n instanceof LongArrayTag a) return idx(k) < a.size() && idx(k) >= 0 ? net.minecraft.nbt.LongTag.valueOf(a.getAsLongArray()[idx(k)]) : null;
            else return null;
            if (n == null) return null;
        }
        return n;
    }

    private static int idx(String k) {
        try {
            return Integer.parseInt(k);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void normalize() {
        while (!path.isEmpty() && !isContainer(node(path))) path.remove(path.size() - 1);
        if (selKey != null && !selKey.equals("@bank") && child(node(path), selKey) == null) selKey = null;
    }

    private static boolean isContainer(Tag t) {
        return t instanceof CompoundTag || t instanceof ListTag || t instanceof IntArrayTag || t instanceof LongArrayTag;
    }

    private static List<String> keys(Tag t) {
        List<String> out = new ArrayList<>();
        if (t instanceof CompoundTag c) {
            out.addAll(c.keySet());
            out.sort(String::compareToIgnoreCase);
        } else if (t instanceof ListTag l) {
            for (int i = 0; i < l.size(); i++) out.add(String.valueOf(i));
        } else if (t instanceof IntArrayTag a) {
            for (int i = 0; i < a.size(); i++) out.add(String.valueOf(i));
        } else if (t instanceof LongArrayTag a) {
            for (int i = 0; i < a.size(); i++) out.add(String.valueOf(i));
        }
        return out;
    }

    private static Tag child(Tag parent, String k) {
        if (parent instanceof CompoundTag c) return c.get(k);
        if (parent instanceof ListTag l) return idx(k) >= 0 && idx(k) < l.size() ? l.get(idx(k)) : null;
        if (parent instanceof IntArrayTag a) return idx(k) >= 0 && idx(k) < a.size() ? net.minecraft.nbt.IntTag.valueOf(a.getAsIntArray()[idx(k)]) : null;
        if (parent instanceof LongArrayTag a) return idx(k) >= 0 && idx(k) < a.size() ? net.minecraft.nbt.LongTag.valueOf(a.getAsLongArray()[idx(k)]) : null;
        return null;
    }

    private static String show(Tag t) {
        if (t == null) return "";
        if (t instanceof CompoundTag c) return "{ " + c.size() + " }";
        if (t instanceof ListTag l) return "[ " + l.size() + " ]";
        if (t instanceof IntArrayTag a) return "[ " + a.size() + " ]";
        if (t instanceof LongArrayTag a) return "[ " + a.size() + " ]";
        if (t instanceof StringTag s) return s.value();
        String s = t.toString();
        if (t instanceof NumericTag && !s.isEmpty() && Character.isLetter(s.charAt(s.length() - 1))) s = s.substring(0, s.length() - 1);
        return s;
    }

    private String valueOf(String k) {
        if (k.equals("@bank")) return String.valueOf(cur.getLongOr("bankGold", 0));
        return show(child(node(path), k));
    }

    private static String label(String k) {
        return NAMES.getOrDefault(k, k);
    }

    private List<String> fullPath(String k) {
        List<String> p = new ArrayList<>(path);
        if (k != null) p.add(k);
        return p;
    }

    private CompoundTag editRequest(String op, List<String> p) {
        CompoundTag t = base(op);
        ListTag l = new ListTag();
        for (String s : p) l.add(StringTag.valueOf(s));
        t.put("path", l);
        t.putString("value", value.getValue());
        t.putString("key", key.getValue().strip());
        return t;
    }

    private void action(String what, String arg) {
        CompoundTag t = base("act");
        t.putString("what", what);
        t.putString("arg", arg);
        send(t);
    }

    private boolean confirmed(String id) {
        long now = System.currentTimeMillis();
        if (confirm.equals(id) && now < confirmUntil) {
            confirm = "";
            return true;
        }
        confirm = id;
        confirmUntil = now + 3500;
        setMessage("Cliquez une seconde fois pour confirmer.", false);
        return false;
    }

    // ================================================================== rendu

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        value.visible = false;
        key.visible = false;
        renderList(g);
        int rx = left + 152, rw = pw - 158;
        if (cur == null) {
            Ui.header(g, rx, top + 8, rw, "GESTION DES JOUEURS");
            Ui.wrap(g, "Choisissez un joueur dans la liste. Toutes les modifications sont vérifiées par le serveur et enregistrées dans le journal "
                    + "d'administration." + (dbOn ? " Les joueurs hors ligne apparaissent aussi (base du réseau)." : " Seuls les joueurs connectés apparaissent (base du réseau désactivée)."),
                    rx, top + 26, rw, Ui.MUTED);
            if (!message.isEmpty()) g.text(font, font.plainSubstrByWidth(message, rw), rx, top + ph - 14, messageColor, false);
            return;
        }
        renderHeader(g, rx, rw);
        int tw = (rw - 9) / 4;
        for (Page p : Page.values()) {
            int tx = rx + p.ordinal() * (tw + 3);
            boolean on = p == page;
            button(g, tx, top + 26, tw, 14, p.label, true, on ? Ui.GOLD : Ui.LINE, () -> {
                page = p;
                scroll = 0;
                selKey = null;
                confirm = "";
            });
        }
        int y0 = top + 46, y1 = top + ph - 44;
        switch (page) {
            case RESUME -> renderResume(g, rx, y0, rw, y1);
            case DONNEES -> renderTree(g, rx, y0, rw, y1);
            case OBJETS -> renderItems(g, rx, y0, rw, y1);
            case ACTIONS -> renderActions(g, rx, y0, rw, y1);
        }
        if (!message.isEmpty()) g.text(font, font.plainSubstrByWidth(message, rw), rx, top + ph - 12, messageColor, false);
    }

    private void renderList(GuiGraphicsExtractor g) {
        int x = left + 6, w = 140, y0 = top + 24, rows = (ph - 30) / ROW;
        Ui.inset(g, x, y0 - 1, w, rows * ROW + 2);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, players.size() - rows)));
        for (int i = 0; i < rows && listScroll + i < players.size(); i++) {
            CompoundTag p = players.get(listScroll + i);
            int y = y0 + i * ROW;
            boolean sel = p.getStringOr("id", "").equals(curId);
            boolean hov = hovered(x, y, w, ROW);
            if (sel || hov) g.fill(x + 1, y, x + w - 1, y + ROW, sel ? 0x60E2B85A : 0x30FFFFFF);
            boolean on = p.getBooleanOr("online", false);
            g.fill(x + 4, y + 4, x + 8, y + 8, on ? Ui.GREEN : 0xFF6A6A78);
            String lv = "N" + p.getIntOr("level", 1);
            g.text(font, lv, x + w - 4 - font.width(lv), y + 3, Ui.MUTED, false);
            g.text(font, font.plainSubstrByWidth(p.getStringOr("name", "?"), w - 18 - font.width(lv)), x + 12, y + 3, sel ? Ui.GOLD_LIGHT : Ui.TEXT, false);
            String id = p.getStringOr("id", "");
            click(x, y, w, ROW, () -> select(id));
        }
        if (players.isEmpty()) g.text(font, "Aucun joueur.", x + 6, y0 + 4, Ui.MUTED, false);
    }

    private void renderHeader(GuiGraphicsExtractor g, int rx, int rw) {
        boolean on = cur.getBooleanOr("online", false);
        CompoundTag data = cur.getCompoundOrEmpty("data");
        g.text(font, cur.getStringOr("name", "?"), rx, top + 8, Ui.GOLD_LIGHT, true);
        String info = "Niv. " + data.getIntOr("level", 1) + " · " + data.getStringOr("class", "NONE");
        int x = rx + font.width(cur.getStringOr("name", "?")) + 10;
        g.text(font, info, x, top + 8, Ui.MUTED, false);
        x += font.width(info) + 8;
        g.text(font, on ? "EN LIGNE" : "HORS LIGNE", x, top + 8, on ? Ui.GREEN : 0xFF9A9AAA, false);
        if (cur.getBooleanOr("muted", false)) g.text(font, "MUET", x + font.width(on ? "EN LIGNE" : "HORS LIGNE") + 8, top + 8, Ui.RED, false);
        g.fill(rx, top + 21, rx + rw, top + 22, Ui.LINE);
    }

    private void bar(GuiGraphicsExtractor g, int rx, int rw, String hint) {
        g.text(font, hint, rx, top + ph - 40, Ui.MUTED, false);
    }

    private void placeBoxes(int rx, int rw, int boxW, boolean withKey) {
        int y = top + ph - 30;
        int x = rx;
        if (withKey) {
            key.visible = true;
            key.setX(x);
            key.setY(y);
            key.setWidth(80);
            x += 84;
        }
        value.visible = true;
        value.setX(x);
        value.setY(y);
        value.setWidth(boxW);
    }

    // ---------------------------------------------------------------- resume

    private void renderResume(GuiGraphicsExtractor g, int rx, int y0, int rw, int y1) {
        int half = rw / 2 - 3;
        int rows = Math.max(1, (y1 - y0) / ROW);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, FIELDS.length - rows)));
        for (int i = 0; i < rows && scroll + i < FIELDS.length; i++) {
            String[] f = FIELDS[scroll + i];
            int y = y0 + i * ROW;
            boolean sel = f[1].equals(selKey);
            if (sel) g.fill(rx, y, rx + half, y + ROW, 0x50E2B85A);
            else if (hovered(rx, y, half, ROW)) g.fill(rx, y, rx + half, y + ROW, 0x25FFFFFF);
            g.text(font, f[0], rx + 3, y + 3, Ui.MUTED, false);
            String v = valueOf(f[1]);
            g.text(font, font.plainSubstrByWidth(v, half - 100), rx + 100, y + 3, Ui.TEXT, false);
            String k = f[1];
            click(rx, y, half, ROW, () -> {
                selKey = k;
                path.clear();
                value.setValue(valueOf(k));
            });
        }
        // raccourcis vers l'arbre complet
        int sx = rx + half + 6;
        g.text(font, "Ouvrir une catégorie :", sx, y0 - 1, Ui.MUTED, false);
        int cw = (rw - half - 9) / 2 - 2;
        for (int i = 0; i < SHORTCUTS.length; i++) {
            int col = i % 2, row = i / 2;
            int bx = sx + col * (cw + 3), by = y0 + 10 + row * 15;
            if (by + 14 > y1) break;
            String k = SHORTCUTS[i][1];
            button(g, bx, by, cw, 13, SHORTCUTS[i][0], true, Ui.LINE, () -> {
                page = Page.DONNEES;
                path.clear();
                if (cur.getCompoundOrEmpty("data").get(k) != null && isContainer(cur.getCompoundOrEmpty("data").get(k))) path.add(k);
                else selKey = k;
                scroll = 0;
            });
        }
        if (selKey != null) {
            bar(g, rx, rw, selKey.equals("@bank") ? "Or en banque (joueur connecté à ce serveur)" : "Nouvelle valeur pour « " + label(selKey) + " »");
            placeBoxes(rx, rw, rw - 160, false);
            boolean bank = selKey.equals("@bank");
            button(g, rx + rw - 72, top + ph - 31, 72, 15, "Appliquer", true, Ui.GREEN, () -> {
                if (bank) action("bankgold", value.getValue());
                else send(editRequest("set", fullPath(selKey)));
            });
        } else {
            g.text(font, "Cliquez un champ pour le modifier.", rx, top + ph - 40, Ui.MUTED, false);
        }
    }

    // ---------------------------------------------------------------- arbre

    private void renderTree(GuiGraphicsExtractor g, int rx, int y0, int rw, int y1) {
        Tag here = node(path);
        StringBuilder crumb = new StringBuilder("Données");
        for (String p : path) crumb.append(" › ").append(label(p));
        g.text(font, font.plainSubstrByWidth(crumb.toString(), rw - 60), rx, y0 - 1, Ui.GOLD, false);
        if (!path.isEmpty()) {
            button(g, rx + rw - 54, y0 - 3, 54, 12, "◀ Retour", true, Ui.LINE, () -> {
                path.remove(path.size() - 1);
                selKey = null;
                scroll = 0;
            });
        }
        int ly = y0 + 11;
        int rows = Math.max(1, (y1 - ly) / ROW);
        List<String> ks = keys(here);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, ks.size() - rows)));
        for (int i = 0; i < rows && scroll + i < ks.size(); i++) {
            String k = ks.get(scroll + i);
            Tag c = child(here, k);
            int y = ly + i * ROW;
            boolean sel = k.equals(selKey);
            if (sel) g.fill(rx, y, rx + rw, y + ROW, 0x50E2B85A);
            else if (hovered(rx, y, rw, ROW)) g.fill(rx, y, rx + rw, y + ROW, 0x25FFFFFF);
            boolean box = isContainer(c);
            g.text(font, font.plainSubstrByWidth((box ? "▸ " : "  ") + label(k), rw / 2 - 6), rx + 3, y + 3, box ? Ui.GOLD_LIGHT : Ui.MUTED, false);
            g.text(font, font.plainSubstrByWidth(show(c), rw / 2 - 6), rx + rw / 2, y + 3, Ui.TEXT, false);
            if (box) {
                click(rx, y, rw, ROW, () -> {
                    path.add(k);
                    selKey = null;
                    scroll = 0;
                });
            } else {
                click(rx, y, rw, ROW, () -> {
                    selKey = k;
                    value.setValue(show(child(node(path), k)));
                });
            }
        }
        if (ks.isEmpty()) g.text(font, "(vide)", rx + 3, ly + 3, Ui.MUTED, false);
        boolean map = here instanceof CompoundTag, list = here instanceof ListTag;
        if (selKey != null) {
            bar(g, rx, rw, "Valeur de « " + label(selKey) + " »");
            placeBoxes(rx, rw, rw - 150, false);
            button(g, rx + rw - 72, top + ph - 31, 72, 15, "Appliquer", true, Ui.GREEN, () -> send(editRequest("set", fullPath(selKey))));
            if (map || list) {
                button(g, rx + rw - 144, top + ph - 31, 70, 15, "Supprimer", true, Ui.RED, () -> {
                    if (confirmed("del" + selKey)) {
                        send(editRequest("del", fullPath(selKey)));
                        selKey = null;
                    }
                });
                value.setWidth(rw - 150);
            }
        } else if (map || list) {
            bar(g, rx, rw, map ? "Ajouter : nom de l'entrée puis valeur" : "Ajouter une valeur à la liste");
            placeBoxes(rx, rw, map ? rw - 84 - 140 : rw - 140, map);
            button(g, rx + rw - 136, top + ph - 31, 66, 15, "Ajouter", !path.isEmpty() || map, Ui.GREEN, () -> send(editRequest("add", fullPath(null))));
            button(g, rx + rw - 68, top + ph - 31, 68, 15, "Vider", !path.isEmpty(), Ui.RED, () -> {
                if (confirmed("clear" + path)) send(editRequest("clear", fullPath(null)));
            });
        }
    }

    // ---------------------------------------------------------------- objets

    private record ItemRow(String where, int slot, long auction, String text, String button) {
    }

    private List<ItemRow> itemRows() {
        List<ItemRow> out = new ArrayList<>();
        addStacks(out, "inv", "Inventaire", cur.getListOrEmpty("inventory"));
        addStacks(out, "ender", "Ender", cur.getListOrEmpty("ender"));
        addStacks(out, "bank", "Banque", cur.getListOrEmpty("bank"));
        ListTag au = cur.getListOrEmpty("auction");
        for (int i = 0; i < au.size(); i++) {
            CompoundTag e = au.getCompoundOrEmpty(i);
            out.add(new ItemRow("auction", 0, e.getLongOr("id", -1), "HDV · " + e.getStringOr("item", "?") + " · " + e.getLongOr("price", 0) + " or", "Annuler"));
        }
        return out;
    }

    private static void addStacks(List<ItemRow> out, String where, String label, ListTag l) {
        for (int i = 0; i < l.size(); i++) {
            CompoundTag e = l.getCompoundOrEmpty(i);
            out.add(new ItemRow(where, e.getIntOr("slot", 0), 0, label + " #" + e.getIntOr("slot", 0) + " · " + e.getStringOr("item", "?") + " x" + e.getIntOr("count", 1), "Retirer"));
        }
    }

    private void renderItems(GuiGraphicsExtractor g, int rx, int y0, int rw, int y1) {
        List<ItemRow> rows = itemRows();
        int n = Math.max(1, (y1 - y0) / ROW);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - n)));
        for (int i = 0; i < n && scroll + i < rows.size(); i++) {
            ItemRow r = rows.get(scroll + i);
            int y = y0 + i * ROW;
            if (hovered(rx, y, rw, ROW)) g.fill(rx, y, rx + rw, y + ROW, 0x25FFFFFF);
            g.text(font, font.plainSubstrByWidth(r.text, rw - 60), rx + 3, y + 3, Ui.TEXT, false);
            button(g, rx + rw - 54, y + 1, 54, ROW - 2, r.button, true, Ui.RED, () -> {
                if (r.where.equals("auction")) {
                    CompoundTag t = base("act");
                    t.putString("what", "cancelauction");
                    t.putLong("auction", r.auction);
                    send(t);
                } else {
                    CompoundTag t = base("act");
                    t.putString("what", "delslot");
                    t.putString("where", r.where);
                    t.putInt("slot", r.slot);
                    send(t);
                }
            });
        }
        if (rows.isEmpty()) g.text(font, "Aucun objet (ou joueur hors ligne).", rx + 3, y0 + 3, Ui.MUTED, false);
        bar(g, rx, rw, "Donner un objet : choisis-le dans la grille, puis la quantité");
        placeBoxes(rx, rw, rw - 84 - 76, true);
        key.setWidth(40);
        value.setX(rx + 44);
        value.setWidth(rw - 44 - 158);
        button(g, rx + rw - 154, top + ph - 31, 80, 15, "Choisir…", true, Ui.GOLD, () ->
                minecraft.gui.setScreen(new ItemPickerScreen(this, "Donner un objet", true, 64, List.of(), (id, qty) -> {
                    value.setValue(id);
                    key.setValue(Integer.toString(qty));
                })));
        button(g, rx + rw - 70, top + ph - 31, 70, 15, "Donner", true, Ui.GREEN, () -> {
            CompoundTag t = base("act");
            t.putString("what", "give");
            t.putString("arg", value.getValue().strip());
            int c = 1;
            try {
                c = Integer.parseInt(key.getValue().strip());
            } catch (NumberFormatException ignored) {
            }
            t.putInt("count", c);
            send(t);
        });
        if (key.getValue().isEmpty()) key.setHint(Component.literal("Qté").withColor(0xFF807868));
    }

    // ---------------------------------------------------------------- actions

    private void renderActions(GuiGraphicsExtractor g, int rx, int y0, int rw, int y1) {
        boolean online = cur.getBooleanOr("online", false);
        int cols = 3, bw = (rw - 6) / cols, bh = 16;
        for (int i = 0; i < ACTIONS.length; i++) {
            String[] a = ACTIONS[i];
            int bx = rx + (i % cols) * (bw + 3), by = y0 + (i / cols) * (bh + 3);
            boolean needs = a[3].equals("1");
            boolean enabled = !needs || online;
            String what = a[1];
            String label = a[0];
            boolean pending = confirm.equals(what) && System.currentTimeMillis() < confirmUntil;
            int accent = a[2].equals("1") ? Ui.RED : Ui.GREEN;
            button(g, bx, by, bw, bh, pending ? "Confirmer ?" : label, enabled, accent, () -> {
                if (a[2].equals("1") && !confirmed(what)) return;
                action(what, value.getValue().strip());
            });
        }
        bar(g, rx, rw, "Raison (expulsion/bannissement), mode de jeu (survival, creative, adventure, spectator)");
        placeBoxes(rx, rw, rw, false);
        value.setWidth(rw);
    }

    // ================================================================== entrees

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        int d = (int) -Math.signum(sy) * 2;
        if (x < left + 150) listScroll = Math.max(0, listScroll + d);
        else scroll = Math.max(0, scroll + d);
        return true;
    }
}
