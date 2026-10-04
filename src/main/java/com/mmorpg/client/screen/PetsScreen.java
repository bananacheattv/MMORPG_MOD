package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.item.ItemTooltips;
import com.mmorpg.network.Payloads;
import com.mmorpg.pet.PetType;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Collection de familiers : invocation, niveau, experience et bonus. */
public class PetsScreen extends MenuScreen {
    private static PetType selected = PetType.FEU_FOLLET;

    public PetsScreen() {
        super(Tab.FAMILIERS);
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        PlayerData d = ClientData.DATA;
        int x0 = left + 8;
        int y0 = contentTop;
        int owned = 0;
        for (PetType p : PetType.values()) if (d.petXp.containsKey(p.id)) owned++;
        Ui.header(g, x0, y0, 196, "COLLECTION (" + owned + "/" + PetType.values().length + ")");
        int cell = 46;
        PetType[] pets = PetType.values();
        for (int i = 0; i < pets.length; i++) {
            PetType p = pets[i];
            int cx = x0 + (i % 4) * (cell + 4);
            int cy = y0 + 14 + (i / 4) * (cell + 18);
            boolean has = d.petXp.containsKey(p.id);
            boolean active = p.id.equals(d.activePet);
            Ui.frame(g, cx, cy, cell, cell + 14, p.rarity.color, p == selected || hovered(cx, cy, cell, cell + 14));
            if (active) Ui.border(g, cx + 1, cy + 1, cell - 2, cell + 12, 0xFF60FF60);
            g.pose().pushMatrix();
            g.pose().translate(cx + cell / 2f - 16, cy + 4);
            g.pose().scale(2f, 2f);
            g.item(new ItemStack(ModItems.PET_SPRITES.get(p).get()), 0, 0);
            g.pose().popMatrix();
            if (!has) {
                g.fill(cx + 1, cy + 1, cx + cell - 1, cy + cell + 13, 0xC0000000);
                g.centeredText(font, Component.literal("?"), cx + cell / 2, cy + 16, Ui.MUTED);
            } else {
                String lv = "Niv. " + d.petLevel(p.id);
                g.centeredText(font, Component.literal(lv), cx + cell / 2, cy + cell + 2, Ui.TEXT);
            }
            final PetType pt = p;
            click(cx, cy, cell, cell + 14, () -> selected = pt);
        }
        g.text(font, "Obtenez des œufs sur les boss", x0, y0 + 14 + 2 * (cell + 18) + 4, Ui.MUTED, false);
        g.text(font, "ou fabriquez-les à la Forge.", x0, y0 + 14 + 2 * (cell + 18) + 14, Ui.MUTED, false);

        // ------------------------------------------------ details
        int dx = left + 214;
        int dw = left + pw - 8 - dx;
        int dh = ph - 30 - 10;
        Ui.inset(g, dx, y0, dw, dh);
        PetType p = selected;
        boolean has = d.petXp.containsKey(p.id);
        g.pose().pushMatrix();
        g.pose().translate(dx + 8, y0 + 8);
        g.pose().scale(3f, 3f);
        g.item(new ItemStack(ModItems.PET_SPRITES.get(p).get()), 0, 0);
        g.pose().popMatrix();
        g.text(font, p.label, dx + 62, y0 + 10, p.rarity.color, true);
        g.text(font, p.rarity.label, dx + 62, y0 + 21, Ui.MUTED, false);
        int ty = y0 + 60;
        ty += Ui.wrap(g, p.description, dx + 8, ty, dw - 16, Ui.TEXT) + 4;
        int level = has ? d.petLevel(p.id) : 1;
        if (has) {
            int xp = d.petXp.getOrDefault(p.id, 0);
            int cur = PetType.xpForLevel(level);
            int next = level >= PetType.MAX_LEVEL ? cur : PetType.xpForLevel(level + 1);
            float frac = level >= PetType.MAX_LEVEL ? 1 : (xp - cur) / (float) Math.max(1, next - cur);
            g.text(font, "Niveau " + level + " / " + PetType.MAX_LEVEL, dx + 8, ty, Ui.GOLD, true);
            ty += 11;
            Ui.bar(g, dx + 8, ty, dw - 16, 8, frac, 0xFFC080FF, 0xFF603090, level >= PetType.MAX_LEVEL ? "MAX" : (xp - cur) + " / " + (next - cur));
            ty += 14;
        }
        g.text(font, "Bonus" + (has ? "" : " (niveau 1)") + " :", dx + 8, ty, Ui.GOLD, false);
        ty += 11;
        List<Component> lines = new ArrayList<>();
        ItemTooltips.statLines(p.bonusAt(level), 1.0, lines::add);
        for (Component c : lines) {
            g.text(font, c, dx + 8, ty, 0xFFFFFFFF, false);
            ty += 10;
        }
        int by = y0 + dh - 22;
        if (has) {
            boolean active = p.id.equals(d.activePet);
            button(g, dx + 8, by, dw - 16, 16, active ? "Renvoyer le familier" : "Invoquer", true, active ? 0xFF6A2A2A : 0xFF2A6A3A,
                    () -> ClientNet.send(new Payloads.PetAction(active ? "" : p.id)));
        } else {
            g.centeredText(font, Component.literal("Familier non obtenu"), dx + dw / 2, by + 4, Ui.RED);
        }
    }
}
