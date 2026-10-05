package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.crafting.ForgeRecipes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Bestiaire : monstres et boss du monde, leurs niveaux, statistiques, butin et le nombre de victoires. */
public class BestiaryScreen extends MenuScreen {
    private static String selected;
    private int scroll;
    private int category; // tous, monstres, boss
    private String family = "Toutes";
    private int dropdown;
    private int dropdownScroll;
    private List<String> familyOptions = List.of("Toutes");

    public BestiaryScreen() {
        super(Tab.BESTIAIRE);
    }

    public static String displayName(String key) {
        CompoundTag t = ClientData.mobInfo.getCompoundOrEmpty(key);
        String custom = t.getStringOr("name", "");
        return custom.isEmpty() ? Component.translatable("entity.mmorpg." + key).getString() : custom;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (dropdown != 0) {
            dropdownScroll = Math.max(0, dropdownScroll - (int) Math.signum(sy));
            return true;
        }
        scroll = Math.max(0, scroll - (int) Math.signum(sy));
        return true;
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        List<String> all = new ArrayList<>(ClientData.mobInfo.keySet());
        all.sort(java.util.Comparator.<String>comparingInt(k -> ClientData.mobInfo.getCompoundOrEmpty(k).getIntOr("min", 1))
                .thenComparing(BestiaryScreen::displayName));
        List<String> categoryKeys = all.stream().filter(k -> category == 0 ||
                ClientData.mobInfo.getCompoundOrEmpty(k).getBooleanOr("boss", false) == (category == 2)).toList();
        List<String> families = new ArrayList<>(List.of("Toutes"));
        categoryKeys.stream().map(k -> ClientData.mobInfo.getCompoundOrEmpty(k).getStringOr("family", "Autres"))
                .distinct().sorted().forEach(families::add);
        if (!families.contains(family)) family = "Toutes";
        familyOptions = families;
        List<String> keys = categoryKeys.stream().filter(k -> family.equals("Toutes") ||
                family.equals(ClientData.mobInfo.getCompoundOrEmpty(k).getStringOr("family", "Autres"))).toList();
        int x0 = left + 8;
        int y0 = contentTop;
        int lw = 150;
        Ui.header(g, x0, y0, lw, "BESTIAIRE (" + keys.size() + ")");
        Ui.frame(g, x0, y0 + 14, lw, 16, Ui.GOLD, hovered(x0, y0 + 14, lw, 16));
        g.text(font, "Type : " + new String[]{"Tous", "Monstres", "Boss"}[category] + "  ▼", x0 + 4, y0 + 18, Ui.TEXT, false);
        click(x0, y0 + 14, lw, 16, () -> { dropdown = 1; dropdownScroll = 0; });
        Ui.frame(g, x0, y0 + 32, lw, 16, Ui.GOLD, hovered(x0, y0 + 32, lw, 16));
        g.text(font, font.plainSubstrByWidth("Famille : " + family + "  ▼", lw - 8), x0 + 4, y0 + 36, Ui.TEXT, false);
        click(x0, y0 + 32, lw, 16, () -> { dropdown = 2; dropdownScroll = 0; });
        if (keys.isEmpty()) {
            g.text(font, "Aucune donnée.", x0, y0 + 54, Ui.MUTED, false);
            return;
        }
        if (selected == null || !keys.contains(selected)) selected = keys.get(0);
        int rowH = 16;
        int visible = Math.max(1, (ph - 30 - 60) / rowH);
        scroll = Math.min(scroll, Math.max(0, keys.size() - visible));
        for (int i = 0; i < Math.min(visible, keys.size() - scroll); i++) {
            String k = keys.get(i + scroll);
            CompoundTag t = ClientData.mobInfo.getCompoundOrEmpty(k);
            int ry = y0 + 52 + i * rowH;
            boolean boss = t.getBooleanOr("boss", false);
            boolean sel = k.equals(selected);
            Ui.frame(g, x0, ry, lw, rowH - 2, boss ? 0xFFFF5050 : 0xFFC0A060, sel || hovered(x0, ry, lw, rowH - 2));
            String label = (boss ? "☠ " : "") + displayName(k);
            g.text(font, font.plainSubstrByWidth(label, lw - 40), x0 + 4, ry + 3, boss ? 0xFFFF8080 : Ui.TEXT, false);
            int min = t.getIntOr("min", 1), max = t.getIntOr("max", 1);
            String lv = min == max ? String.valueOf(min) : min + "-" + max;
            g.text(font, lv, x0 + lw - 4 - font.width(lv), ry + 3, Ui.levelColor((min + max) / 2, ClientData.DATA.level), false);
            final String key = k;
            click(x0, ry, lw, rowH - 2, () -> selected = key);
        }

        int dx = x0 + lw + 10;
        int dw = left + pw - 8 - dx;
        int dh = ph - 30 - 10;
        Ui.inset(g, dx, y0, dw, dh);
        CompoundTag t = ClientData.mobInfo.getCompoundOrEmpty(selected);
        boolean boss = t.getBooleanOr("boss", false);
        Ui.scaledText(g, displayName(selected), dx + 8, y0 + 8, 1.5f, boss ? 0xFFFF6060 : Ui.GOLD_LIGHT, false);
        int ty = y0 + 26;
        int min = t.getIntOr("min", 1), max = t.getIntOr("max", 1);
        g.text(font, (boss ? "Boss — " : "Monstre — ") + "Niveau " + (min == max ? min : min + " à " + max), dx + 8, ty, Ui.TEXT, false);
        ty += 12;
        g.text(font, "PV : " + Ui.fmt(t.getFloatOr("hp", 0)) + (min == max ? "" : " → " + Ui.fmt(t.getFloatOr("hpMax", 0))), dx + 8, ty, 0xFFFF7070, false);
        ty += 11;
        g.text(font, "Attaque : " + Ui.fmt(t.getFloatOr("atk", 0)) + (min == max ? "" : " → " + Ui.fmt(t.getFloatOr("atkMax", 0))), dx + 8, ty, 0xFFFFA050, false);
        ty += 11;
        String origin = boss ? "Invocation : Autel d'Invocation" : t.getBooleanOr("spawns", false) ? "Apparaît naturellement dans le monde" : "N'apparaît pas naturellement";
        g.text(font, origin, dx + 8, ty, Ui.MUTED, false);
        ty += 11;
        int kills = ClientData.DATA.kills.getOrDefault(selected, 0);
        g.text(font, "Vaincus : " + kills, dx + 8, ty, Ui.GREEN, false);
        ty += 16;
        Ui.header(g, dx + 8, ty, dw - 16, "BUTIN");
        ty += 14;
        ListTag drops = t.getListOrEmpty("drops");
        int col = 0;
        for (int i = 0; i < drops.size(); i++) {
            CompoundTag dt = drops.getCompoundOrEmpty(i);
            Item item = ForgeRecipes.resolveItem(dt.getStringOr("item", "minecraft:air"));
            if (item == null || item == Items.AIR) continue;
            int ix = dx + 8 + col * ((dw - 16) / 2);
            ItemStack stack = new ItemStack(item);
            g.item(stack, ix, ty);
            float chance = dt.getFloatOr("chance", 0) * 100;
            String txt = (chance >= 100 ? "100" : String.format(java.util.Locale.FRANCE, "%.0f", chance)) + " %";
            g.text(font, font.plainSubstrByWidth(stack.getHoverName().getString(), (dw - 16) / 2 - 22), ix + 18, ty, Ui.TEXT, false);
            g.text(font, txt, ix + 18, ty + 9, Ui.MUTED, false);
            if (hovered(ix, ty, 16, 16)) g.setTooltipForNextFrame(font, stack, mx, my);
            col++;
            if (col >= 2) {
                col = 0;
                ty += 20;
            }
            if (ty > y0 + dh - 20) break;
        }
    }

    @Override protected void renderOverlay(GuiGraphicsExtractor g, int mx, int my, float a) {
        if (dropdown == 0) return;
        List<String> options = dropdown == 1 ? List.of("Tous", "Monstres", "Boss") : familyOptions;
        int x = left + 8, y = contentTop + (dropdown == 1 ? 14 : 32), w = 150;
        int rows = Math.min(options.size(), Math.max(1, (top + ph - y - 12) / 18));
        dropdownScroll = Math.min(dropdownScroll, Math.max(0, options.size() - rows));
        hits.clear();
        click(0, 0, width, height, () -> dropdown = 0);
        g.fill(x - 1, y - 1, x + w + 1, y + rows * 18 + 1, 0xFF15111E);
        Ui.frame(g, x - 1, y - 1, w + 2, rows * 18 + 2, Ui.GOLD, false);
        for (int i = 0; i < rows; i++) {
            final int index = dropdownScroll + i;
            String option = options.get(index);
            boolean chosen = dropdown == 1 ? category == index : family.equals(option);
            button(g, x, y + i * 18, w, 18, (chosen ? "✓ " : "") + option, true,
                    chosen ? 0xFF665020 : 0xFF282230, () -> {
                        if (dropdown == 1) { category = index; family = "Toutes"; }
                        else family = option;
                        dropdown = 0; scroll = 0;
                    });
        }
    }
}
