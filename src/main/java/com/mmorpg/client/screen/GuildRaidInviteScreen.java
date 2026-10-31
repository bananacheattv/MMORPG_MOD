package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;

/** Fenêtre d'invitation : un membre de la guilde a lancé un boss, rejoindre le combat ou refuser. */
public final class GuildRaidInviteScreen extends MmoScreen {
    private final CompoundTag data;

    public GuildRaidInviteScreen(CompoundTag data) {
        super("Boss de guilde");
        this.data = data;
    }

    @Override
    protected boolean compact() {
        return true;
    }

    @Override
    protected int panelWidth() {
        return Math.min(width - 12, 300);
    }

    @Override
    protected int panelHeight() {
        return Math.min(height - 12, 170);
    }

    private void answer(int action) {
        ClientNet.send(new Payloads.QuestAction(action, "", -1));
        net.minecraft.client.Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "BOSS DE GUILDE", Ui.GOLD_LIGHT);
        Ui.inset(g, left + 8, top + 24, pw - 16, ph - 24 - 52);
        int y = top + 36;
        Ui.wrap(g, data.getStringOr("from", "?") + " a lancé le combat contre", left + 14, y, pw - 28, 0xFFE8E0F0);
        y += 22;
        g.text(Ui.font(), data.getStringOr("boss", "?"), left + 14, y, 0xFF000000 | data.getIntOr("color", 0xFFD040), true);
        y += 22;
        Ui.wrap(g, "Voulez-vous rejoindre le combat de la guilde " + data.getStringOr("guild", "") + " ?", left + 14, y, pw - 28, 0xFFB8B0C8);
        button(g, left + 12, top + ph - 46, pw - 24, 20, "Rejoindre le combat", true, 0xFF7A5A1A, () -> answer(Payloads.QuestAction.RAID_JOIN));
        button(g, left + 12, top + ph - 22, pw - 24, 16, "Refuser", true, 0xFF3A3436, () -> answer(Payloads.QuestAction.RAID_REFUSE));
    }
}
