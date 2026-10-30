package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Fenetre de proposition automatique d'une quete : « Accepter la quete » ou « Plus tard ». */
public final class QuestOfferScreen extends MmoScreen {
    private final CompoundTag quest;

    public QuestOfferScreen(CompoundTag quest) {
        super("Nouvelle quête");
        this.quest = quest;
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
        return Math.min(height - 12, 230);
    }

    private void answer(int action) {
        ClientNet.send(new Payloads.QuestAction(action, quest.getStringOr("id", ""), -1));
        net.minecraft.client.Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "NOUVELLE QUÊTE", Ui.GOLD_LIGHT);
        Ui.inset(g, left + 8, top + 24, pw - 16, ph - 24 - 52);
        QuestPanel.render(g, quest, left + 8, top + 24, pw - 16, top + ph - 54, mx, my);
        button(g, left + 12, top + ph - 46, pw - 24, 20, "Accepter la quête", true, 0xFF7A5A1A, () -> answer(Payloads.QuestAction.ACCEPT));
        button(g, left + 12, top + ph - 22, pw - 24, 16, "Plus tard", true, 0xFF3A3436, () -> answer(Payloads.QuestAction.DECLINE));
    }
}
