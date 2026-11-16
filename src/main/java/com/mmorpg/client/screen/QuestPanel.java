package com.mmorpg.client.screen;

import com.mmorpg.client.QuestText;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.pet.PetType;
import com.mmorpg.quest.QuestKind;
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

    private static int scroll, contentH, viewH;
    private static String scrollId = "";

    /** Fait defiler le detail (molette) ; sans effet si tout tient dans le cadre. */
    public static void scrollBy(int rows) {
        scroll = Math.max(0, Math.min(Math.max(0, contentH - viewH), scroll + rows * 12));
    }

    public static QuestKind kindOf(CompoundTag q) {
        String k = q.getStringOr("qkind", "");
        if (k.isEmpty()) return q.getBooleanOr("daily", false) ? QuestKind.JOURNALIERE : QuestKind.SECONDAIRE;
        return QuestKind.byId(k);
    }

    /** Pastille de categorie (glyphe + libelle) a la couleur de la categorie ; renvoie sa largeur. */
    public static int badge(GuiGraphicsExtractor g, Font font, QuestKind k, int x, int y) {
        String t = k.glyph + " " + k.badge;
        int bw = font.width(t) + 8;
        g.fill(x, y - 1, x + bw, y + 10, 0xFF1A1424);
        g.fill(x, y - 1, x + 2, y + 10, k.color);
        g.fill(x, y - 1, x + bw, y, Ui.alpha(k.color, 0.5f));
        g.text(font, t, x + 5, y + 1, k.color, true);
        return bw;
    }

    /** Dessine le detail ; renvoie l'ordonnee de fin du contenu. */
    public static int render(GuiGraphicsExtractor g, CompoundTag q, int x, int y, int w, int maxY, int mx, int my) {
        Font font = Minecraft.getInstance().font;
        QuestKind kind = kindOf(q);
        String qid = q.getStringOr("id", "");
        if (!qid.equals(scrollId)) {
            scrollId = qid;
            scroll = 0;
        }
        g.fill(x + 1, y + 1, x + 3, maxY, Ui.alpha(kind.color, 0.9f));
        int ty = y + 6;
        int bw = badge(g, font, kind, x + 8, ty);
        String lv = "Niv. " + q.getIntOr("minLevel", 1);
        String sub = q.getBooleanOr("weekly", false) ? "Hebdomadaire" : q.getBooleanOr("daily", false) && kind != QuestKind.JOURNALIERE ? "Journalière" : "";
        if (!sub.isEmpty()) lv = sub + " • " + lv;
        int room = w - 16 - bw - 8;
        g.text(font, font.plainSubstrByWidth(lv, Math.max(20, room)), x + 8 + bw + 6, ty + 1, Ui.MUTED, false);
        ty += 14;
        var nameLines = font.split(net.minecraft.network.chat.FormattedText.of(q.getStringOr("name", "?")), w - 18);
        for (int i = 0; i < Math.min(2, nameLines.size()); i++) {
            g.text(font, nameLines.get(i), x + 8, ty, Ui.GOLD_LIGHT, true);
            ty += 10;
        }
        ty += 3;
        int bodyTop = ty;
        viewH = Math.max(1, maxY - bodyTop);
        contentH = Math.max(0, contentH);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentH - viewH)));
        g.enableScissor(x + 3, bodyTop, x + w - 1, maxY);
        ty -= scroll;
        String loc = q.getStringOr("loreRegion", "");
        String giver = q.getStringOr("giver", "");
        if (!loc.isEmpty()) ty += Ui.wrap(g, "Région : " + loc, x + 8, ty, w - 20, Ui.MUTED);
        if (!giver.isEmpty() && !q.getBooleanOr("active", false)) ty += Ui.wrap(g, "Donneur : " + giver, x + 8, ty, w - 20, Ui.MUTED);
        if (!loc.isEmpty() || !giver.isEmpty()) ty += 3;
        ty += Ui.wrap(g, q.getStringOr("desc", ""), x + 8, ty, w - 20, Ui.TEXT) + 4;
        Ui.header(g, x + 8, ty, w - 16, "OBJECTIFS");
        ty += 13;
        ListTag objs = q.getListOrEmpty("objectives");
        boolean active = q.getBooleanOr("active", false);
        for (int i = 0; i < objs.size(); i++) {
            CompoundTag o = objs.getCompoundOrEmpty(i);
            boolean done = active && QuestText.done(o);
            Ui.diamond(g, x + 11, ty + 4, 2, done ? Ui.GREEN : kind.color);
            String prog = active ? QuestText.progress(o) : "";
            int pw2 = prog.isEmpty() ? 0 : font.width(prog) + 6;
            var lines = font.split(net.minecraft.network.chat.FormattedText.of(QuestText.objective(o)), w - 34 - pw2);
            for (int l = 0; l < lines.size(); l++) {
                g.text(font, lines.get(l), x + 17, ty + l * 10, done ? Ui.GREEN : Ui.TEXT, false);
            }
            if (active) g.text(font, prog, x + w - 8 - font.width(prog), ty, done ? Ui.GREEN : Ui.MUTED, false);
            ty += Math.max(1, lines.size()) * 10 + 1;
        }
        ty += 3;
        Ui.header(g, x + 8, ty, w - 16, "RÉCOMPENSES");
        ty += 13;
        CompoundTag rw = q.getCompoundOrEmpty("rewards");
        int rx = x + 8;
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
                rx = x + 8;
                ty += 18;
            }
            ItemStack stack = new ItemStack(item, it.getIntOr("count", 1));
            g.item(stack, rx, ty);
            g.itemDecorations(font, stack, rx, ty);
            if (Ui.in(mx, my, rx, ty, 16, 16)) g.setTooltipForNextFrame(font, stack, mx, my);
            rx += 19;
        }
        int evo = rw.getIntOr("evolution", 0);
        if (evo > 0) {
            var cls = com.mmorpg.client.ClientData.DATA.playerClass;
            String label = "✦ Évolution : " + (cls.isPlayable() ? cls.evolutions[Math.min(4, evo)] : "palier " + evo);
            if (rx > x + 8) {
                rx = x + 8;
                ty += 18;
            }
            ty += Ui.wrap(g, label, rx, ty + 4, w - 20, Ui.GOLD) - 6;
        }
        int end = ty + 18 + scroll;
        g.disableScissor();
        contentH = end - bodyTop;
        if (contentH > viewH) {
            int trackH = maxY - bodyTop - 2;
            int barH = Math.max(10, trackH * viewH / contentH);
            int barY = bodyTop + 1 + (trackH - barH) * scroll / Math.max(1, contentH - viewH);
            g.fill(x + w - 5, bodyTop + 1, x + w - 3, bodyTop + 1 + trackH, 0x66000000);
            g.fill(x + w - 5, barY, x + w - 3, barY + barH, kind.color);
        }
        return end;
    }
}
