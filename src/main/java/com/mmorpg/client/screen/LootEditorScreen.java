package com.mmorpg.client.screen;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mmorpg.MMORPG;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.config.LootTables;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mmorpg.client.screen.EditorChoiceScreen.Choice;

/** Editeur des butins des caisses d'aventure et des Lucky Blocks (/mmorpg butins, operateurs). */
public final class LootEditorScreen extends MmoScreen {
    private static final Gson JSON = new Gson();
    private static final java.lang.reflect.Type LIST = new TypeToken<List<LootTables.Entry>>() {
    }.getType();
    private static final String[] TABLES = LootTables.ALL.toArray(new String[0]);

    private final Map<String, List<LootTables.Entry>> tables = new LinkedHashMap<>();
    private final Map<String, Boolean> modified = new LinkedHashMap<>();
    private String current = LootTables.CRATE;
    private String status = "";
    private boolean statusOk = true;
    private int scroll;

    public LootEditorScreen(CompoundTag data) {
        super("Éditeur de butins");
        load(data, null);
    }

    /** Reponse du serveur apres un enregistrement : la table enregistree est rechargee, les autres modifications sont gardees. */
    public void receive(CompoundTag data) {
        load(data, data.getBooleanOr("ok", false) ? current : "");
        status = data.getStringOr("status", "");
        statusOk = data.getBooleanOr("ok", false);
    }

    private void load(CompoundTag data, String onlyTable) {
        CompoundTag t = data.getCompoundOrEmpty("tables");
        for (String id : TABLES) {
            if (onlyTable != null && !onlyTable.equals(id)) continue;
            List<LootTables.Entry> list = JSON.fromJson(t.getStringOr(id, "[]"), LIST);
            tables.put(id, list == null ? new ArrayList<>() : new ArrayList<>(list));
            modified.put(id, false);
        }
    }

    private List<LootTables.Entry> entries() {
        return tables.computeIfAbsent(current, k -> new ArrayList<>());
    }

    private void changed() {
        modified.put(current, true);
        status = "";
    }

    @Override
    protected int panelWidth() {
        return Math.min(width - 12, 520);
    }

    @Override
    protected int panelHeight() {
        return height - 12;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, scroll - (int) Math.signum(sy));
        return true;
    }

    private static Item item(LootTables.Entry e) {
        return e.isGold() ? ForgeRecipes.resolveItem("mmorpg:piece_or") : ForgeRecipes.resolveItem(e.item);
    }

    private static String name(LootTables.Entry e) {
        if (e.isGold()) return "Pièces d’or (bourse)";
        Item item = item(e);
        return item == null || item == Items.AIR ? e.item + " (inconnu)" : new ItemStack(item).getHoverName().getString();
    }

    private void add(String id) {
        if (entries().size() >= LootTables.MAX_ENTRIES) {
            status = "Maximum " + LootTables.MAX_ENTRIES + " butins par table.";
            statusOk = false;
            return;
        }
        for (LootTables.Entry e : entries()) {
            if (e.item.equals(id)) {
                status = "Ce butin est déjà dans la table.";
                statusOk = false;
                return;
            }
        }
        entries().add(new LootTables.Entry(id, 1, id.equals("or") ? 50 : 1, 10));
        changed();
    }

    private void chooseItem() {
        List<Choice> choices = new ArrayList<>();
        choices.add(new Choice("or", "Pièces d’or (bourse)"));
        List<Choice> vanilla = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            var key = BuiltInRegistries.ITEM.getKey(item);
            Choice c = new Choice(key.toString(), new ItemStack(item).getHoverName().getString() + "  (" + key + ")");
            if (key.getNamespace().equals(MMORPG.MODID)) choices.add(c);
            else vanilla.add(c);
        }
        choices.addAll(vanilla);
        minecraft.gui.setScreen(new EditorChoiceScreen(this, "Ajouter un butin", choices, this::add));
    }

    private void save() {
        CompoundTag t = new CompoundTag();
        t.putString("table", current);
        t.putString("json", JSON.toJson(entries()));
        ClientNet.send(new Payloads.LootEdit(t));
        status = "Enregistrement…";
        statusOk = true;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 9, "ÉDITEUR DE BUTINS", Ui.GOLD);
        // onglets des tables
        int tw = (pw - 20) / TABLES.length;
        for (int i = 0; i < TABLES.length; i++) {
            String id = TABLES[i];
            String label = LootTables.label(id) + (modified.getOrDefault(id, false) ? " *" : "");
            button(g, left + 10 + i * tw, top + 26, tw - 4, 18, label, true, id.equals(current) ? Ui.GOLD : Ui.GOLD_DARK, () -> {
                current = id;
                scroll = 0;
            });
        }
        List<LootTables.Entry> list = entries();
        int total = 0;
        for (LootTables.Entry e : list) total += e.weight;
        // en-tetes de colonnes
        int x0 = left + 10, w = pw - 20, y0 = top + 52;
        int cQty = x0 + w - 236, cWeight = x0 + w - 120, cDel = x0 + w - 18;
        g.text(font, "Butin", x0 + 22, y0, Ui.MUTED, false);
        g.text(font, "Quantité (min - max)", cQty, y0, Ui.MUTED, false);
        g.text(font, "Poids / chance", cWeight, y0, Ui.MUTED, false);
        int rowH = 22, listTop = y0 + 12, listBottom = top + ph - 58;
        int rows = Math.max(1, (listBottom - listTop) / rowH);
        scroll = Math.min(scroll, Math.max(0, list.size() - rows));
        boolean shift = minecraft.hasShiftDown();
        int step = shift ? 10 : 1;
        for (int i = 0; i < Math.min(rows, list.size() - scroll); i++) {
            LootTables.Entry e = list.get(i + scroll);
            int y = listTop + i * rowH;
            Ui.frame(g, x0, y, w, rowH - 2, Ui.GOLD_DARK, hovered(x0, y, w, rowH - 2));
            Item item = item(e);
            if (item != null) g.item(new ItemStack(item), x0 + 3, y + 2);
            g.text(font, font.plainSubstrByWidth(name(e), cQty - x0 - 30), x0 + 22, y + 6, Ui.TEXT, false);
            int qStep = e.isGold() ? step * 10 : step;
            spinner(g, cQty, y + 2, e.min, () -> { e.min = Math.max(1, e.min - qStep); changed(); },
                    () -> { e.min += qStep; e.max = Math.max(e.max, e.min); changed(); });
            spinner(g, cQty + 56, y + 2, e.max, () -> { e.max = Math.max(e.min, e.max - qStep); changed(); },
                    () -> { e.max += qStep; changed(); });
            spinner(g, cWeight, y + 2, e.weight, () -> { e.weight = Math.max(0, e.weight - step); changed(); },
                    () -> { e.weight += step; changed(); });
            float pct = total <= 0 ? 0 : e.weight * 100f / total;
            g.text(font, String.format(java.util.Locale.FRANCE, "%.1f %%", pct), cWeight + 60, y + 6, e.weight > 0 ? Ui.GOLD : Ui.MUTED, false);
            final LootTables.Entry entry = e;
            button(g, cDel, y + 2, 16, 16, "×", true, Ui.RED, () -> { list.remove(entry); changed(); });
        }
        if (list.isEmpty()) g.centeredText(font, Component.literal("Table vide : ajoutez des butins."), left + pw / 2, listTop + 10, Ui.MUTED);
        if (list.size() > rows) g.text(font, (scroll + 1) + "-" + Math.min(list.size(), scroll + rows) + " / " + list.size() + " (molette)",
                x0, listBottom + 1, Ui.MUTED, false);
        // actions
        int by = top + ph - 44, bw = (w - 12) / 4;
        button(g, x0, by, bw, 18, "Ajouter un objet…", true, Ui.GOLD, this::chooseItem);
        ItemStack held = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
        button(g, x0 + bw + 4, by, bw, 18, "Objet en main", !held.isEmpty(), Ui.GOLD,
                () -> add(BuiltInRegistries.ITEM.getKey(held.getItem()).toString()));
        button(g, x0 + 2 * (bw + 4), by, bw, 18, "Ajouter de l’or", true, Ui.GOLD, () -> add("or"));
        button(g, x0 + 3 * (bw + 4), by, bw, 18, "Enregistrer", modified.getOrDefault(current, false), Ui.GREEN, this::save);
        String help = status.isEmpty() ? "Clic sur − / + (Maj : ×10). Chance = poids / somme des poids." : status;
        g.text(font, font.plainSubstrByWidth(help, w), x0, top + ph - 19, status.isEmpty() ? Ui.MUTED : statusOk ? Ui.GREEN : Ui.RED, false);
    }

    /** Valeur entouree de boutons - et +. */
    private void spinner(GuiGraphicsExtractor g, int x, int y, int value, Runnable minus, Runnable plus) {
        button(g, x, y, 12, 16, "−", true, Ui.GOLD_DARK, minus);
        g.centeredText(font, Component.literal(String.valueOf(value)), x + 26, y + 4, Ui.TEXT);
        button(g, x + 40, y, 12, 16, "+", true, Ui.GOLD_DARK, plus);
    }
}
