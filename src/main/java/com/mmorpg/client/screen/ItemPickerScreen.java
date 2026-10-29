package com.mmorpg.client.screen;

import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.economy.ShopSorting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.function.BiConsumer;

/** Sélecteur visuel d'objets partagé par tous les menus de configuration : grille d'icônes, recherche, catégories, quantité. */
public final class ItemPickerScreen extends MmoScreen {
    /** Entrée spéciale (ex. « or ») ajoutée en tête de la grille, représentée par un objet existant. */
    public record Extra(String id, String label, Item icon) {}

    private record Entry(Item item, String id, String label, String match, int category) {}

    private static final String[] TABS = {"Tous", "Armes", "Armures", "Outils", "Potions", "Matériaux", "Autres"};
    private static List<Entry> cache;

    private final Screen parent;
    private final String heading;
    private final List<Extra> extras;
    private final boolean withQuantity;
    private final int maxQuantity;
    private final BiConsumer<String, Integer> pick;
    private EditBox search;
    private String query = "";
    private int tab, page, quantity = 1;
    private String selected = "";

    public ItemPickerScreen(Screen parent, String title, boolean withQuantity, int maxQuantity, List<Extra> extras, BiConsumer<String, Integer> pick) {
        super(title);
        this.parent = parent;
        this.heading = title;
        this.withQuantity = withQuantity;
        this.maxQuantity = Math.max(1, maxQuantity);
        this.extras = extras;
        this.pick = pick;
    }

    private static List<Entry> catalogue() {
        if (cache == null) {
            var list = new ArrayList<Entry>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR || com.mmorpg.server.RetiredItems.item(item) || com.mmorpg.server.CrateOnly.is(item)
                        || item instanceof com.mmorpg.item.NpcEditorItem || item instanceof com.mmorpg.item.SpawnEditorItem) continue;
                String id = BuiltInRegistries.ITEM.getKey(item).toString(), label = new ItemStack(item).getHoverName().getString();
                list.add(new Entry(item, id, label, (label + " " + id).toLowerCase(Locale.ROOT), ShopSorting.category(item)));
            }
            list.sort(Comparator.comparing((Entry e) -> !e.id().startsWith("mmorpg:")).thenComparingInt(Entry::category)
                    .thenComparingInt(e -> ShopSorting.level(e.item())).thenComparing(Entry::label));
            cache = List.copyOf(list);
        }
        return cache;
    }

    private List<Entry> filtered() {
        var out = new ArrayList<Entry>();
        String q = query.toLowerCase(Locale.ROOT).strip();
        for (Extra x : extras) {
            if (tab == 0 && (q.isEmpty() || (x.label() + " " + x.id()).toLowerCase(Locale.ROOT).contains(q)))
                out.add(new Entry(x.icon(), x.id(), x.label(), "", -1));
        }
        for (Entry e : catalogue()) {
            if (tab != 0 && Math.min(e.category(), 5) != tab - 1) continue;
            if (!q.isEmpty() && !e.match().contains(q)) continue;
            out.add(e);
        }
        return out;
    }

    @Override protected int designHeight() { return 340; }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, left + 10, top + 29, pw - 20, 18, Component.literal("Rechercher"));
        search.setMaxLength(60);
        search.setValue(query);
        search.setHint(Component.literal("Rechercher un objet par nom…"));
        search.setResponder(v -> { query = v; page = 0; });
        addRenderableWidget(search);
        setInitialFocus(search);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        page = Math.max(0, page - (int) Math.signum(sy));
        return true;
    }

    private void choose(Entry e) {
        if (!withQuantity) {
            pick.accept(e.id(), 1);
            minecraft.gui.setScreen(parent);
        } else {
            selected = e.id();
        }
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 9, heading, Ui.GOLD);
        int tw = (pw - 20) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            final int value = i;
            button(g, left + 10 + i * tw, top + 52, tw - 3, 16, TABS[i], true, tab == i ? Ui.GOLD : Ui.MUTED, () -> { tab = value; page = 0; });
        }
        int box = 26, gap = 3, x0 = left + 10, y0 = top + 74, w = pw - 20, h = ph - 74 - 54;
        int cols = Math.max(1, (w + gap) / (box + gap)), rows = Math.max(1, (h + gap) / (box + gap)), per = cols * rows;
        var list = filtered();
        int pages = Math.max(1, (list.size() + per - 1) / per);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < per && page * per + i < list.size(); i++) {
            Entry e = list.get(page * per + i);
            int x = x0 + i % cols * (box + gap), y = y0 + i / cols * (box + gap);
            boolean on = e.id().equals(selected), hov = hovered(x, y, box, box);
            Ui.itemBox(g, font, new ItemStack(e.item()), x, y, box);
            if (on || hov) Ui.border(g, x - 1, y - 1, box + 2, box + 2, on ? Ui.GOLD : Ui.TEXT);
            click(x, y, box, box, () -> choose(e));
            if (hov) g.setTooltipForNextFrame(font, new ItemStack(e.item()), mouseX, mouseY);
        }
        if (list.isEmpty()) g.text(font, "Aucun objet trouvé.", x0 + 2, y0 + 4, Ui.MUTED, false);
        int by = top + ph - 44;
        button(g, left + 10, by, 24, 16, "‹", page > 0, Ui.GOLD, () -> page--);
        g.centeredText(font, Component.literal((page + 1) + " / " + pages + "  ·  " + list.size() + " objet(s)"), left + pw / 2, by + 4, Ui.MUTED);
        button(g, left + pw - 34, by, 24, 16, "›", page + 1 < pages, Ui.GOLD, () -> page++);
        int by2 = top + ph - 24;
        button(g, left + 10, by2, 70, 18, "Retour", true, Ui.GOLD, this::onClose);
        if (withQuantity) {
            int step = minecraft.hasShiftDown() ? 10 : 1;
            button(g, left + 90, by2, 16, 18, "−", true, Ui.GOLD_DARK, () -> quantity = Math.max(1, quantity - step));
            g.centeredText(font, Component.literal("× " + quantity), left + 124, by2 + 5, Ui.TEXT);
            button(g, left + 142, by2, 16, 18, "+", true, Ui.GOLD_DARK, () -> quantity = Math.min(maxQuantity, quantity + step));
            String name = selected.isEmpty() ? "Clique sur un objet" : labelOf(selected);
            g.text(font, font.plainSubstrByWidth(name, pw - 330), left + 168, by2 + 5, selected.isEmpty() ? Ui.MUTED : Ui.GOLD, false);
            button(g, left + pw - 110, by2, 100, 18, "Valider", !selected.isEmpty(), Ui.GREEN, () -> {
                pick.accept(selected, quantity);
                minecraft.gui.setScreen(parent);
            });
        }
    }

    private String labelOf(String id) {
        for (Extra x : extras) if (x.id().equals(id)) return x.label();
        for (Entry e : catalogue()) if (e.id().equals(id)) return e.label();
        return id;
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }
}
