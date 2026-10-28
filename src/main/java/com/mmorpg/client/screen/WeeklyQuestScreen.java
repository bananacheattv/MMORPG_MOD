package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import com.mmorpg.server.WeeklyQuests;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

/** Pancarte des quetes hebdomadaires : trois cartes (facile, difficile, tres difficile) propres au joueur, a accepter puis a remettre ici. */
public class WeeklyQuestScreen extends MmoScreen {
    private BlockPos pos = BlockPos.ZERO;
    private final List<CompoundTag> tiers = new ArrayList<>();
    private long resetSeconds;
    private int doneCount;

    public WeeklyQuestScreen(CompoundTag data) {
        super("Quêtes hebdomadaires");
        receive(data);
    }

    public void receive(CompoundTag data) {
        this.pos = BlockPos.of(data.getLongOr("pos", 0));
        this.resetSeconds = data.getLongOr("reset", 0);
        this.doneCount = data.getIntOr("doneCount", 0);
        tiers.clear();
        ListTag list = data.getListOrEmpty("tiers");
        for (int i = 0; i < list.size(); i++) tiers.add(list.getCompoundOrEmpty(i));
    }

    @Override
    protected int panelWidth() {
        return Math.min(this.width - 12, 440);
    }

    @Override
    protected int panelHeight() {
        return Math.min(this.height - 12, 252);
    }

    public void send(int action, int tier) {
        ClientNet.send(new Payloads.ServiceAction(Payloads.ServiceAction.WEEKLY, action, tier, "", 0, pos));
    }

    private static String reset(long s) {
        long d = s / 86400, h = s % 86400 / 3600, m = s % 3600 / 60;
        return d > 0 ? d + " j " + h + " h" : h > 0 ? h + " h " + m + " min" : Math.max(1, m) + " min";
    }

    private static String targetName(CompoundTag o) {
        String type = o.getStringOr("type", "kill"), target = o.getStringOr("target", "*");
        int count = o.getIntOr("count", 1);
        if (type.equals("collect")) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(target));
            return "Rapporter " + count + " × " + new net.minecraft.world.item.ItemStack(item).getHoverName().getString();
        }
        if (target.equals("boss")) return "Vaincre " + count + " boss";
        return "Vaincre " + count + " monstres";
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "QUÊTES HEBDOMADAIRES", Ui.GOLD_LIGHT);
        String sub = doneCount + " / 3 terminées · remise à zéro dans " + reset(resetSeconds);
        g.centeredText(font, net.minecraft.network.chat.Component.literal(sub), left + pw / 2, top + 22, Ui.MUTED);
        int gap = 6, cw = (pw - 16 - gap * 2) / 3, cy = top + 38, ch = ph - 38 - 34;
        for (int i = 0; i < tiers.size(); i++) card(g, tiers.get(i), left + 8 + i * (cw + gap), cy, cw, ch);
        int by = top + ph - 26;
        button(g, left + pw / 2 - 60, by, 120, 18, "Revenir au jeu", true, Ui.GOLD, this::onClose);
    }

    private void card(GuiGraphicsExtractor g, CompoundTag q, int x, int y, int w, int h) {
        int tier = q.getIntOr("tier", 0), state = q.getIntOr("state", 0);
        int accent = WeeklyQuests.TIER_COLOR[Math.clamp(tier, 0, 2)];
        boolean complete = q.getBooleanOr("completable", false);
        Ui.frame(g, x, y, w, h, state == 2 ? 0xFF3A6A3A : accent, hovered(x, y, w, h));
        g.fill(x + 1, y + 1, x + w - 1, y + 17, Ui.alpha(accent, 0.35f));
        g.centeredText(font, net.minecraft.network.chat.Component.literal(q.getStringOr("tierName", "")), x + w / 2, y + 5, Ui.TEXT);
        for (int s = 0; s <= tier; s++) Ui.diamond(g, x + w / 2 - tier * 5 + s * 10, y + 24, 3, Ui.GOLD_LIGHT);
        int ty = y + 33;
        g.text(font, font.plainSubstrByWidth(q.getStringOr("name", ""), w - 8), x + 5, ty, Ui.GOLD_LIGHT, false);
        ty += 12;
        ListTag objs = q.getListOrEmpty("objectives");
        for (int i = 0; i < objs.size(); i++) {
            CompoundTag o = objs.getCompoundOrEmpty(i);
            int count = o.getIntOr("count", 1), prog = state == 1 ? o.getIntOr("progress", 0) : state == 2 ? count : 0;
            ty += Ui.wrap(g, targetName(o), x + 5, ty, w - 10, Ui.TEXT) + 2;
            Ui.bar(g, x + 5, ty, w - 10, 9, Math.min(1f, prog / (float) count), 0xFF2A8A4A, 0xFF1A5A30, prog + "/" + count);
            ty += 14;
        }
        CompoundTag rw = q.getCompoundOrEmpty("rewards");
        int ry = y + h - 62;
        g.text(font, "Récompenses", x + 5, ry, Ui.GOLD, false);
        g.text(font, rw.getLongOr("gold", 0) + " or", x + 5, ry + 11, 0xFFFFD040, false);
        g.text(font, rw.getLongOr("xp", 0) + " XP", x + 5, ry + 22, Ui.BLUE, false);
        ListTag items = rw.getListOrEmpty("items");
        for (int i = 0; i < items.size(); i++) {
            CompoundTag it = items.getCompoundOrEmpty(i);
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(it.getStringOr("item", "minecraft:air")));
            var stack = new net.minecraft.world.item.ItemStack(item, it.getIntOr("count", 1));
            g.item(stack, x + 5 + i * 18, ry + 31);
            g.itemDecorations(font, stack, x + 5 + i * 18, ry + 31);
        }
        int by = y + h - 18;
        if (state == 2) {
            button(g, x + 5, by, w - 10, 14, "Terminée", false, 0xFF2A6A3A, () -> { });
        } else if (state == 0) {
            button(g, x + 5, by, w - 10, 14, "Accepter", true, 0xFF2A6A3A, () -> send(WeeklyQuests.ACT_ACCEPT, tier));
        } else {
            int half = (w - 14) / 2;
            button(g, x + 5, by, w - 10 - half - 4, 14, "Remettre", complete, 0xFF2A6A3A, () -> send(WeeklyQuests.ACT_CLAIM, tier));
            button(g, x + w - 5 - half, by, half, 14, "Abandonner", true, 0xFF6A3A2A, () -> send(WeeklyQuests.ACT_ABANDON, tier));
        }
    }
}
