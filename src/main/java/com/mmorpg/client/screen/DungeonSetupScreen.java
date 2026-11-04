package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;

/** Menu du donjon de setup (administrateur) : enregistrer le modele, editer les butins des coffres, quitter. */
public final class DungeonSetupScreen extends MmoScreen {
    private final CompoundTag data;

    public DungeonSetupScreen(CompoundTag data) {
        super("Donjon de setup");
        this.data = data;
    }

    @Override
    protected boolean compact() {
        return true;
    }

    @Override
    protected int panelWidth() {
        return Math.min(width - 12, 320);
    }

    @Override
    protected int panelHeight() {
        return Math.min(height - 12, 200);
    }

    private void act(String what) {
        CompoundTag t = new CompoundTag();
        t.putString("do", what);
        ClientNet.send(new Payloads.DungeonSetupAction(t));
        if (what.equals("loot") || what.equals("leave")) net.minecraft.client.Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "SETUP · " + data.getStringOr("label", "").toUpperCase(), Ui.GOLD_LIGHT);
        Ui.inset(g, left + 8, top + 24, pw - 16, 40);
        Ui.wrap(g, "Modifiez blocs, générateurs de monstres et coffres : le modèle enregistré est copié dans les exemplaires créés ensuite.", left + 14, top + 28, pw - 28, 0xFFB8B0C8);
        int y = top + 72, w = pw - 24;
        button(g, left + 12, y, w, 20, "Enregistrer le modèle", true, 0xFF2E6A3A, () -> act("save"));
        button(g, left + 12, y + 24, w, 20, "Butins des coffres (contenu aléatoire)", true, 0xFF7A5A1A, () -> act("loot"));
        button(g, left + 12, y + 48, w, 18, "Supprimer le modèle (génération d'origine)", data.getBooleanOr("template", false), 0xFF6A2E2E, () -> act("delete"));
        button(g, left + 12, y + 70, w, 18, "Quitter le setup", true, 0xFF3A3436, () -> act("leave"));
        String st = data.getStringOr("status", "");
        if (!st.isEmpty()) Ui.wrap(g, st, left + 12, y + 92, w, 0xFF90E0A0);
        else g.text(Ui.font(), data.getBooleanOr("template", false) ? "Un modèle est enregistré." : "Aucun modèle : génération d'origine.", left + 12, y + 94, 0xFF8C8498, false);
    }
}
