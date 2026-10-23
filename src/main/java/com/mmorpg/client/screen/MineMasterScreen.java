package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.mine.MineBlocks;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Menu de selection des mines (Maitre des mines, /mine) et des plaines (portail du Spawn, /plaines) : une entree par palier de niveau, avec
 * ses informations ; une zone verrouillee se debloque en trouvant le portail cache de la precedente.
 */
public class MineMasterScreen extends MmoScreen {
    private final int npc;
    private final String npcName;
    private final String tagline;
    private final String unit;
    private final int playerLevel;
    private final boolean built;
    private final int percent;
    private final int base;
    private final List<CompoundTag> sectors = new ArrayList<>();
    private int selected;

    public MineMasterScreen(CompoundTag data) {
        super(data.getStringOr("name", "Maître des mines"));
        this.npc = data.getIntOr("npc", -1);
        this.npcName = data.getStringOr("name", "Maître des mines");
        this.tagline = data.getStringOr("tagline", "Trouvez le portail caché de chaque mine pour ouvrir la suivante");
        this.unit = data.getStringOr("unit", "Mine");
        this.base = data.getIntOr("base", 0);
        this.playerLevel = data.getIntOr("playerLevel", 1);
        this.built = data.getBooleanOr("built", true);
        this.percent = data.getIntOr("percent", 100);
        ListTag list = data.getListOrEmpty("sectors");
        for (int i = 0; i < list.size(); i++) sectors.add(list.getCompoundOrEmpty(i));
        // la zone la plus avancee que le joueur a ouverte est proposee d'office
        for (int i = 0; i < sectors.size(); i++) if (!sectors.get(i).getBooleanOr("locked", false)) selected = i;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, npcName.toUpperCase(Locale.ROOT), Ui.GOLD_LIGHT);
        g.centeredText(font, net.minecraft.network.chat.Component.literal(font.plainSubstrByWidth(tagline, pw - 16)), left + pw / 2, top + 19, Ui.MUTED);
        int x0 = left + 8;
        int y0 = top + 32;
        int lw = 160;
        int rowH = Math.min(36, (ph - 40) / Math.max(1, sectors.size()));
        for (int i = 0; i < sectors.size(); i++) {
            CompoundTag s = sectors.get(i);
            int ry = y0 + i * rowH;
            int color = 0xFF000000 | s.getIntOr("color", 0xFFC868);
            boolean open = !s.getBooleanOr("locked", false);
            Ui.frame(g, x0, ry, lw, rowH - 3, open ? color : 0xFF50505A, i == selected || hovered(x0, ry, lw, rowH - 3));
            g.item(new ItemStack(ModItems.get(s.getStringOr("icon", "lingot_verdite"))), x0 + 5, ry + Math.max(1, (rowH - 19) / 2));
            g.text(font, font.plainSubstrByWidth(unit + " " + (i + 1) + (open ? "" : "  (verrouillée)"), lw - 30), x0 + 28, ry + 5, open ? color : Ui.MUTED, false);
            g.text(font, font.plainSubstrByWidth(open ? s.getStringOr("label", "?") : "???", lw - 32), x0 + 28, ry + 15, open ? Ui.TEXT : Ui.MUTED, false);
            g.text(font, "niv. " + s.getIntOr("min", 1) + " à " + s.getIntOr("max", 1), x0 + 28, ry + 24, open ? (playerLevel >= s.getIntOr("min", 1) ? Ui.GREEN : 0xFFC0A050) : 0xFFB05050, false);
            int idx = i;
            click(x0, ry, lw, rowH - 3, () -> selected = idx);
        }
        int dx = x0 + lw + 8;
        int dw = left + pw - 8 - dx;
        int dh = ph - 32 - 8;
        Ui.inset(g, dx, y0, dw, dh);
        if (selected < 0 || selected >= sectors.size()) return;
        CompoundTag s = sectors.get(selected);
        boolean open = !s.getBooleanOr("locked", false);
        int color = 0xFF000000 | s.getIntOr("color", 0xFFC868);
        g.item(new ItemStack(ModItems.get(s.getStringOr("icon", "lingot_verdite"))), dx + 8, y0 + 6);
        g.text(font, (open ? s.getStringOr("label", "?") : "Zone inconnue").toUpperCase(Locale.ROOT), dx + 30, y0 + 10, open ? color : Ui.MUTED, true);
        int ty = y0 + 28 + Ui.wrap(g, open ? s.getStringOr("lore", "") : s.getStringOr("lockedHint", "Trouvez le portail caché de la zone précédente."), dx + 8, y0 + 28, dw - 16, Ui.MUTED);
        ty += 6;
        if (open) {
            Ui.header(g, dx + 8, ty, dw - 16, "CE QUE VOUS Y TROUVEZ");
            ty += 18;
            ListTag info = s.getListOrEmpty("info");
            for (int i = 0; i < info.size() && ty < y0 + dh - 62; i++) ty += Ui.wrap(g, info.getStringOr(i, ""), dx + 8, ty, dw - 16, i == 0 ? color : Ui.TEXT) + 3;
        }
        if (!built) {
            Ui.wrap(g, "Cette zone est en construction (" + percent + " %) : le chargement durera un peu plus longtemps.", dx + 8, y0 + dh - 62, dw - 16, 0xFFFFC060);
        }
        int sel = selected;
        String label = open ? (selected == 0 ? "Y aller : " + s.getStringOr("label", "?") : "Se rendre : " + s.getStringOr("label", "?")) : "Verrouillée";
        button(g, dx + 8, y0 + dh - 26, dw - 16, 20, font.plainSubstrByWidth(label, dw - 24), open, open ? color : 0xFF50505A, () -> {
            ClientNet.send(new Payloads.MineEnter(npc, base + sel));
            minecraft.gui.setScreen(null);
        });
    }
}
