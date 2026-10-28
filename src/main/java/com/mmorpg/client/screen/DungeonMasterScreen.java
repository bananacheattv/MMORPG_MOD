package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Dialogue avec le Maitre des donjons : liste des donjons, trois difficultes, entree dans son propre exemplaire. */
public class DungeonMasterScreen extends MmoScreen {
    private static final String[] NAMES = {"Normal", "Héroïque", "Mythique"};
    private static final int[] ACCENT = {0xFF2A7A3A, 0xFF7A5A1A, 0xFF8A2A5A};
    private final int npc;
    private final String npcName;
    private final int playerLevel;
    private final List<CompoundTag> dungeons = new ArrayList<>();
    private String selected = "";

    public DungeonMasterScreen(CompoundTag data) {
        super("Maître des donjons");
        this.npc = data.getIntOr("npc", -1);
        this.npcName = data.getStringOr("name", "Maître des donjons");
        this.playerLevel = data.getIntOr("playerLevel", 1);
        ListTag list = data.getListOrEmpty("dungeons");
        for (int i = 0; i < list.size(); i++) dungeons.add(list.getCompoundOrEmpty(i));
        if (!dungeons.isEmpty()) selected = dungeons.get(0).getStringOr("id", "");
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, npcName.toUpperCase(java.util.Locale.ROOT), Ui.GOLD_LIGHT);
        g.centeredText(font, Component.literal("Votre donjon, rien que pour vous et votre groupe"), left + pw / 2, top + 19, Ui.MUTED);
        int x0 = left + 8;
        int y0 = top + 32;
        int lw = 160;
        int rowH = 24;
        for (int i = 0; i < dungeons.size(); i++) {
            CompoundTag d = dungeons.get(i);
            String id = d.getStringOr("id", "");
            int ry = y0 + i * rowH;
            int color = 0xFF000000 | d.getIntOr("color", 0xFFC868);
            Ui.frame(g, x0, ry, lw, rowH - 2, color, id.equals(selected) || hovered(x0, ry, lw, rowH - 2));
            g.text(font, font.plainSubstrByWidth(d.getStringOr("label", "?"), lw - 38), x0 + 6, ry + 4, Ui.TEXT, false);
            boolean ok = playerLevel >= d.getIntOr("level", 1);
            String lv = "niv. " + d.getIntOr("level", 1);
            g.text(font, lv, x0 + 6, ry + 13, ok ? Ui.GREEN : 0xFFB05050, false);
            click(x0, ry, lw, rowH - 2, () -> selected = id);
        }
        int dx = x0 + lw + 8;
        int dw = left + pw - 8 - dx;
        int dh = ph - 32 - 8;
        Ui.inset(g, dx, y0, dw, dh);
        CompoundTag d = dungeons.stream().filter(e -> e.getStringOr("id", "").equals(selected)).findFirst().orElse(null);
        if (d == null) return;
        int color = 0xFF000000 | d.getIntOr("color", 0xFFC868);
        g.text(font, d.getStringOr("label", "?").toUpperCase(java.util.Locale.ROOT), dx + 8, y0 + 8, color, true);
        Ui.wrap(g, d.getStringOr("lore", ""), dx + 8, y0 + 24, dw - 16, Ui.MUTED);
        if(ph>260)Ui.wrap(g,"Survolez une difficulté pour lire son parcours et ses dangers. Une reprise conserve votre expédition.",dx+8,y0+56,dw-16,Ui.TEXT);
        int by = y0 + dh - 3 * 32 - 4;
        for (int diff = 0; diff < 3; diff++) {
            g.text(font,font.plainSubstrByWidth((diff>0?"Groupe conseillé · ":"")+d.getStringOr("variant"+diff,""),dw-20),dx+8,by+diff*32+22,Ui.MUTED,false);
            if(hovered(dx+8,by+diff*32,dw-16,31))g.setComponentTooltipForNextFrame(font,java.util.List.of(Component.literal(d.getStringOr("variant"+diff,"Dangers non disponibles"))),mx,my);
            int need = d.getIntOr("need" + diff, 1);
            boolean ok = playerLevel >= need;
            boolean resume = d.getBooleanOr("resume" + diff, false);
            String label = (resume ? "Reprendre — " : "Entrer — ") + NAMES[diff] + "  (niv. " + need + ")";
            int d2 = diff;
            String id = selected;
            button(g, dx + 8, by + diff * 32, dw - 16, 18, ok ? label : NAMES[diff] + " : niveau " + need + " requis", ok, ACCENT[diff], () -> {
                ClientNet.send(new Payloads.DungeonEnter(npc, id, d2));
                minecraft.gui.setScreen(null);
            });
        }
    }
}
