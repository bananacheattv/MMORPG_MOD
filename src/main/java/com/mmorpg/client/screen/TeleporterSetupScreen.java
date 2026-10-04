package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import com.mmorpg.world.WaypointData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Configuration d'un teleporteur : nom, categorie (ville, donjon, point de passage) et niveau minimum. */
public class TeleporterSetupScreen extends MmoScreen {
    private final BlockPos pos;
    private final String initialName;
    private int category;
    private int minLevel;
    private EditBox nameBox;

    public TeleporterSetupScreen(CompoundTag data) {
        super("Configuration du téléporteur");
        this.pos = BlockPos.of(data.getLongOr("pos", 0));
        this.initialName = data.getStringOr("name", "Téléporteur");
        this.category = data.getIntOr("category", 2);
        this.minLevel = data.getIntOr("minLevel", 1);
    }

    @Override
    protected int panelWidth() {
        return Math.min(this.width - 12, 300);
    }

    @Override
    protected int panelHeight() {
        return Math.min(this.height - 12, 170);
    }

    @Override
    protected void init() {
        super.init();
        nameBox = new EditBox(this.font, left + 16, top + 42, pw - 32, 18, Component.literal("Nom"));
        nameBox.setMaxLength(32);
        nameBox.setValue(nameBox.getValue().isEmpty() ? initialName : nameBox.getValue());
        this.addRenderableWidget(nameBox);
        this.setInitialFocus(nameBox);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "CONFIGURATION DU TÉLÉPORTEUR", Ui.GOLD_LIGHT);
        g.text(font, "Nom du lieu :", left + 16, top + 30, Ui.TEXT, false);
        g.text(font, "Catégorie :", left + 16, top + 68, Ui.TEXT, false);
        int bw = (pw - 32) / 3;
        for (int i = 0; i < 3; i++) {
            final int c = i;
            button(g, left + 16 + i * bw, top + 80, bw - 4, 16, WaypointData.CATEGORY_NAMES[i], true, category == i ? 0xFF6A4A1A : 0xFF2A2420, () -> category = c);
        }
        g.text(font, "Niveau minimum : ", left + 16, top + 106, Ui.TEXT, false);
        int lx = left + 16 + font.width("Niveau minimum : ");
        button(g, lx, top + 102, 16, 16, "-", minLevel > 1, 0xFF6A2A2A, () -> minLevel = Math.max(1, minLevel - (minecraft.hasShiftDown() ? 10 : 1)));
        g.centeredText(font, Component.literal(String.valueOf(minLevel)), lx + 34, top + 106, Ui.GOLD_LIGHT);
        button(g, lx + 52, top + 102, 16, 16, "+", minLevel < 100, 0xFF2A6A2A, () -> minLevel = Math.min(100, minLevel + (minecraft.hasShiftDown() ? 10 : 1)));
        button(g, left + pw / 2 - 60, top + ph - 28, 120, 18, "Enregistrer", true, 0xFF2A6A3A, () -> {
            ClientNet.send(new Payloads.TeleporterSetup(pos, nameBox.getValue(), category, minLevel));
            onClose();
        });
    }
}
