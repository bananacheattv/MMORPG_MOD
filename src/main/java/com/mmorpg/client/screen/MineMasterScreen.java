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

/** Menu du Maitre des mines : les cinq etages, leur minerai et leurs monstres ; on descend derriere un ecran de chargement. */
public class MineMasterScreen extends MmoScreen {
    private final int npc;
    private final String npcName;
    private final int playerLevel;
    private final boolean built;
    private final int percent;
    private final List<CompoundTag> sectors = new ArrayList<>();
    private int selected;

    public MineMasterScreen(CompoundTag data) {
        super("Maître des mines");
        this.npc = data.getIntOr("npc", -1);
        this.npcName = data.getStringOr("name", "Maître des mines");
        this.playerLevel = data.getIntOr("playerLevel", 1);
        this.built = data.getBooleanOr("built", true);
        this.percent = data.getIntOr("percent", 100);
        ListTag list = data.getListOrEmpty("sectors");
        for (int i = 0; i < list.size(); i++) sectors.add(list.getCompoundOrEmpty(i));
        // l'etage le plus profond que le joueur peut rejoindre est propose d'office
        for (int i = 0; i < sectors.size(); i++) if (playerLevel >= sectors.get(i).getIntOr("need", 1)) selected = i;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, npcName.toUpperCase(Locale.ROOT), Ui.GOLD_LIGHT);
        g.centeredText(font, net.minecraft.network.chat.Component.literal("Cinq étages, cinq minerais : plus on descend, plus les monstres sont forts"), left + pw / 2, top + 19, Ui.MUTED);
        int x0 = left + 8;
        int y0 = top + 32;
        int lw = 160;
        int rowH = 36;
        for (int i = 0; i < sectors.size(); i++) {
            CompoundTag s = sectors.get(i);
            int ry = y0 + i * rowH;
            int color = 0xFF000000 | s.getIntOr("color", 0xFFC868);
            boolean open = playerLevel >= s.getIntOr("need", 1);
            Ui.frame(g, x0, ry, lw, rowH - 3, color, i == selected || hovered(x0, ry, lw, rowH - 3));
            g.item(new ItemStack(ModItems.get(MineBlocks.ORES.get(i).ingotId())), x0 + 5, ry + 8);
            g.text(font, font.plainSubstrByWidth("Étage " + (i + 1), lw - 30), x0 + 28, ry + 5, color, false);
            g.text(font, font.plainSubstrByWidth(s.getStringOr("label", "?"), lw - 32), x0 + 28, ry + 15, open ? Ui.TEXT : Ui.MUTED, false);
            g.text(font, "niv. " + s.getIntOr("min", 1) + " à " + s.getIntOr("max", 1), x0 + 28, ry + 24, open ? Ui.GREEN : 0xFFB05050, false);
            int idx = i;
            click(x0, ry, lw, rowH - 3, () -> selected = idx);
        }
        int dx = x0 + lw + 8;
        int dw = left + pw - 8 - dx;
        int dh = ph - 32 - 8;
        Ui.inset(g, dx, y0, dw, dh);
        if (selected < 0 || selected >= sectors.size()) return;
        CompoundTag s = sectors.get(selected);
        int color = 0xFF000000 | s.getIntOr("color", 0xFFC868);
        g.item(new ItemStack(ModItems.get(MineBlocks.ORES.get(selected).ingotId())), dx + 8, y0 + 6);
        g.text(font, s.getStringOr("label", "?").toUpperCase(Locale.ROOT), dx + 30, y0 + 10, color, true);
        int ty = y0 + 28 + Ui.wrap(g, s.getStringOr("lore", ""), dx + 8, y0 + 28, dw - 16, Ui.MUTED);
        ty += 6;
        Ui.header(g, dx + 8, ty, dw - 16, "CE QUE VOUS Y TROUVEZ");
        ty += 18;
        String ore = s.getStringOr("ore", "?");
        g.text(font, "Minerai : " + ore, dx + 8, ty, color, false);
        ty += 11;
        int oreLevel = s.getIntOr("oreLevel", 1);
        g.text(font, "Extraction à partir du niveau " + oreLevel + " (pioche en main)", dx + 8, ty, playerLevel >= oreLevel ? Ui.TEXT : 0xFFB05050, false);
        ty += 11;
        g.text(font, "Le minerai repousse après " + s.getIntOr("cooldown", 60) + " secondes", dx + 8, ty, Ui.MUTED, false);
        ty += 11;
        g.text(font, "Monstres de niveau " + s.getIntOr("min", 1) + " à " + s.getIntOr("max", 1) + " : plus forts en profondeur", dx + 8, ty, Ui.MUTED, false);
        ty += 11;
        Ui.wrap(g, "Le lingot arrive directement dans votre sac. Il sert à forger armes et armures à la Forge Arcanique.", dx + 8, ty + 4, dw - 16, Ui.TEXT);
        if (!built) {
            Ui.wrap(g, "Les mines sont en construction (" + percent + " %) : le chargement durera un peu plus longtemps.", dx + 8, y0 + dh - 62, dw - 16, 0xFFFFC060);
        }
        int need = s.getIntOr("need", 1);
        boolean ok = playerLevel >= need;
        int sel = selected;
        String label = selected == 0 ? "Entrer dans les mines" : "Descendre à l'étage " + (selected + 1);
        button(g, dx + 8, y0 + dh - 26, dw - 16, 20, ok ? label : "Niveau " + need + " requis", ok, 0xFF000000 | s.getIntOr("color", 0xFF7A5A1A), () -> {
            ClientNet.send(new Payloads.MineEnter(npc, sel));
            minecraft.gui.setScreen(null);
        });
    }
}
