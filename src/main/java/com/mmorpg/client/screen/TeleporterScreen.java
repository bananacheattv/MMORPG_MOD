package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModItems;
import com.mmorpg.world.WaypointData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reseau de teleportation : liste des villes, donjons et points de passage decouverts. */
public class TeleporterScreen extends MmoScreen {
    private final List<CompoundTag> entries = new ArrayList<>();
    private final String current;
    private final boolean scroll;
    private final int total;
    private int filter = -1;
    private int offset;

    public TeleporterScreen(CompoundTag data) {
        super("Téléporteur");
        ListTag list = data.getListOrEmpty("list");
        for (int i = 0; i < list.size(); i++) entries.add(list.getCompoundOrEmpty(i));
        entries.sort(Comparator.comparingInt((CompoundTag t) -> t.getIntOr("category", 2)).thenComparing(t -> t.getStringOr("name", "")));
        this.current = data.getStringOr("current", "");
        this.scroll = data.getBooleanOr("scroll", false);
        this.total = data.getIntOr("total", entries.size());
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        offset = Math.max(0, offset - (int) Math.signum(sy));
        return true;
    }

    private static ItemStack iconFor(int category) {
        return switch (category) {
            case WaypointData.CITY -> new ItemStack(ModItems.TELEPORTEUR.get());
            case WaypointData.DUNGEON -> new ItemStack(ModItems.AUTEL_INVOCATION.get());
            default -> new ItemStack(Items.ENDER_PEARL);
        };
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "RÉSEAU DE TÉLÉPORTATION", Ui.GOLD_LIGHT);
        String sub = scroll ? "Parchemin de Téléportation (consommé au départ)" : "Vous êtes à : " + current;
        g.centeredText(font, Component.literal(sub), left + pw / 2, top + 20, scroll ? 0xFF80C0FF : Ui.TEXT);

        String[] filters = {"Tout", "Villes", "Donjons", "Passages"};
        int fw = (pw - 16) / 4;
        for (int i = 0; i < 4; i++) {
            final int f = i - 1;
            button(g, left + 8 + i * fw, top + 32, fw - 4, 16, filters[i], true, filter == f ? 0xFF6A4A1A : 0xFF2A2420, () -> {
                filter = f;
                offset = 0;
            });
        }
        List<CompoundTag> shown = new ArrayList<>();
        for (CompoundTag t : entries) {
            if (filter < 0 || t.getIntOr("category", 2) == filter) shown.add(t);
        }
        int rowH = 24;
        int listTop = top + 54;
        int visible = (ph - 54 - 22) / rowH;
        offset = Math.min(offset, Math.max(0, shown.size() - visible));
        Minecraft mc = Minecraft.getInstance();
        int level = ClientData.DATA.level;
        if (shown.isEmpty()) {
            g.centeredText(font, Component.literal("Aucune destination découverte dans cette catégorie."), left + pw / 2, listTop + 20, Ui.MUTED);
        }
        for (int i = 0; i < Math.min(visible, shown.size() - offset); i++) {
            CompoundTag t = shown.get(i + offset);
            int ry = listTop + i * rowH;
            int cat = t.getIntOr("category", 2);
            int color = cat == WaypointData.CITY ? 0xFF60C0FF : cat == WaypointData.DUNGEON ? 0xFFFF6060 : 0xFF80E080;
            boolean isCurrent = t.getBooleanOr("current", false);
            Ui.frame(g, left + 8, ry, pw - 16, rowH - 3, color, hovered(left + 8, ry, pw - 16, rowH - 3));
            g.item(iconFor(cat), left + 12, ry + 2);
            g.text(font, t.getStringOr("name", "?"), left + 32, ry + 3, isCurrent ? Ui.GOLD_LIGHT : Ui.TEXT, true);
            String catName = WaypointData.CATEGORY_NAMES[Math.max(0, Math.min(2, cat))];
            String info = catName + " • " + t.getStringOr("dim", "");
            if (t.getBooleanOr("sameDim", false) && mc.player != null) {
                double dist = Math.sqrt(mc.player.distanceToSqr(t.getIntOr("x", 0), t.getIntOr("y", 0), t.getIntOr("z", 0)));
                info += " • " + Math.round(dist) + " blocs";
            }
            g.text(font, info, left + 32, ry + 12, Ui.MUTED, false);
            int minLevel = t.getIntOr("minLevel", 1);
            String lv = "Niv. " + minLevel;
            g.text(font, lv, left + pw - 100 - font.width(lv), ry + 7, level >= minLevel ? Ui.GREEN : Ui.RED, false);
            boolean can = !isCurrent && level >= minLevel;
            String id = t.getStringOr("id", "");
            button(g, left + pw - 90, ry + 2, 76, 16, isCurrent ? "Ici" : "Voyager", can, 0xFF2A5A7A, () -> {
                ClientNet.send(new Payloads.Teleport(id));
                onClose();
            });
        }
        String footer = entries.size() + " / " + total + " lieux découverts — explorez le monde pour en activer d'autres";
        g.centeredText(font, Component.literal(footer), left + pw / 2, top + ph - 14, Ui.MUTED);
    }
}
