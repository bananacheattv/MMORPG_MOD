package com.mmorpg.client.screen;

import com.mmorpg.client.QuestText;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.pet.PetType;
import com.mmorpg.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Rendu commun du detail d'une quete (description, objectifs, recompenses). */
public final class QuestPanel {
    private QuestPanel() {
    }

    /** Dessine le detail ; renvoie l'ordonnee de fin du contenu. */
    public static int render(GuiGraphicsExtractor g, CompoundTag q, int x, int y, int w, int maxY, int mx, int my) {
        Font font = Minecraft.getInstance().font;
        int ty = y + 6;
        g.text(font, font.plainSubstrByWidth(q.getStringOr("name", "?"), w - 12), x + 6, ty, Ui.GOLD_LIGHT, true);
        ty += 11;
        String sub = (q.getBooleanOr("daily", false) ? "Quête journalière • " : "") + "Niveau " + q.getIntOr("minLevel", 1);
        g.text(font, sub, x + 6, ty, q.getBooleanOr("daily", false) ? 0xFF60C0FF : Ui.MUTED, false);
        ty += 13;
        g.enableScissor(x + 1, ty, x + w - 1, maxY);
        int descH = Ui.wrapHeight(q.getStringOr("desc", ""), w - 12);
        int maxDesc = Math.max(20, (maxY - ty) / 3);
        Ui.wrap(g, q.getStringOr("desc", ""), x + 6, ty, w - 12, Ui.TEXT);
        ty += Math.min(descH, maxDesc) + 4;
        g.disableScissor();
        Ui.header(g, x + 6, ty, w - 12, "OBJECTIFS");
        ty += 13;
        ListTag objs = q.getListOrEmpty("objectives");
        boolean active = q.getBooleanOr("active", false);
        for (int i = 0; i < objs.size() && ty < maxY - 30; i++) {
            CompoundTag o = objs.getCompoundOrEmpty(i);
            boolean done = active && QuestText.done(o);
            Ui.diamond(g, x + 9, ty + 4, 2, done ? Ui.GREEN : Ui.GOLD_DARK);
            String line = QuestText.objective(o);
            String prog = active ? QuestText.progress(o) : "";
            g.text(font, font.plainSubstrByWidth(line, w - 30 - font.width(prog)), x + 15, ty, done ? Ui.GREEN : Ui.TEXT, false);
            if (active) g.text(font, prog, x + w - 6 - font.width(prog), ty, done ? Ui.GREEN : Ui.MUTED, false);
            ty += 10;
        }
        ty += 3;
        Ui.header(g, x + 6, ty, w - 12, "RÉCOMPENSES");
        ty += 13;
        CompoundTag rw = q.getCompoundOrEmpty("rewards");
        int rx = x + 6;
        long xp = rw.getLongOr("xp", 0);
        long gold = rw.getLongOr("gold", 0);
        if (xp > 0) {
            String t = Ui.fmt(xp) + " XP";
            g.text(font, t, rx, ty + 4, 0xFFC080FF, true);
            rx += font.width(t) + 8;
        }
        if (gold > 0) {
            g.item(new ItemStack(ModItems.PIECE_OR.get()), rx, ty);
            g.text(font, Ui.fmt(gold), rx + 17, ty + 4, 0xFFFFD040, true);
            rx += 20 + font.width(Ui.fmt(gold)) + 6;
        }
        ListTag items = rw.getListOrEmpty("items");
        for (int i = 0; i < items.size(); i++) {
            CompoundTag it = items.getCompoundOrEmpty(i);
            Item item = ForgeRecipes.resolveItem(it.getStringOr("item", "minecraft:air"));
            if (item == null || item == Items.AIR) continue;
            if (rx + 18 > x + w) {
                rx = x + 6;
                ty += 18;
            }
            ItemStack stack = new ItemStack(item, it.getIntOr("count", 1));
            g.item(stack, rx, ty);
            g.itemDecorations(font, stack, rx, ty);
            if (Ui.in(mx, my, rx, ty, 16, 16)) g.setTooltipForNextFrame(font, stack, mx, my);
            rx += 19;
        }
        String pet = rw.getStringOr("pet", "");
        if (!pet.isEmpty() && PetType.byId(pet) != null) {
            g.item(new ItemStack(ModItems.PET_EGGS.get(PetType.byId(pet)).get()), rx, ty);
        }
        return ty + 18;
    }
}
