package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Choix de la destination de l'outil portail. */
public final class PortalToolScreen extends MmoScreen {
    private static final String[] KINDS = {"hub", "spawn", "mine", "plaines", "donjon", "openworld"};
    private static final String[] KIND_LABELS = {"Hub", "Spawn", "Mines", "Plaines", "Donjon", "Monde ouvert"};
    private static final String[] DIFFS = {"Normal", "Héroïque", "Mythique"};
    private int kind, region = -1, diff;
    private String dungeon = "";

    public PortalToolScreen(CompoundTag data) { super("Outil portail"); }

    @Override protected int designHeight() { return 250; }

    private static int rc0(int k) { return k == 2 || k == 3 ? 0 : -1; }
    private int regionCount() { return kind == 2 ? com.mmorpg.mine.MineLayout.SECTORS : kind == 3 ? com.mmorpg.plains.PlainsLayout.REGIONS : 0; }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.frame(g, left, top, pw, ph, Ui.GOLD, false);
        g.centeredText(font, Component.literal("OUTIL PORTAIL · DESTINATION"), left + pw / 2, top + 9, Ui.GOLD);
        int y = top + 28, w = (pw - 24 - 5 * 4) / 6;
        for (int i = 0; i < KINDS.length; i++) {
            final int k = i;
            button(g, left + 12 + i * (w + 4), y, w, 18, KIND_LABELS[i], kind != i, Ui.GOLD, () -> { kind = k; region = rc0(k); dungeon = k == 4 ? DungeonType.values()[0].id : ""; });
        }
        y += 28;
        int rc = regionCount();
        if (rc > 0) {
            g.text(font, "Région", left + 12, y, Ui.TEXT, false);
            y += 12;
            int rw = (pw - 24 - rc * 3) / rc;
            for (int i = 0; i < rc; i++) { final int r = i; button(g, left + 12 + i * (rw + 3), y, rw, 16, String.valueOf(i + 1), region != i, Ui.GOLD, () -> region = r); }
        } else if (kind == 4) {
            g.text(font, "Donjon", left + 12, y, Ui.TEXT, false);
            y += 12;
            int cw = (pw - 24 - 4) / 2;
            var types = DungeonType.values();
            for (int i = 0; i < types.length; i++) {
                final String id = types[i].id;
                button(g, left + 12 + (i % 2) * (cw + 4), y + (i / 2) * 19, cw, 16, types[i].label, !dungeon.equals(id), Ui.GOLD, () -> dungeon = id);
            }
            y += ((types.length + 1) / 2) * 19 + 22;
            {
                int dw = (pw - 24 - 8) / 3;
                for (int i = 0; i < 3; i++) { final int d = i; button(g, left + 12 + i * (dw + 4), y, dw, 16, DIFFS[i], diff != i, Ui.GOLD, () -> diff = d); }
            }
        } else {
            g.text(font, "Aucun réglage pour cette destination.", left + 12, y, Ui.MUTED, false);
        }
        button(g, left + pw / 2 - 90, top + ph - 26, 180, 18, "Valider · clic droit sur un bloc", true, 0xFF3A8A4A, () -> {
            var t = new CompoundTag();
            t.putString("kind", KINDS[kind]);
            t.putInt("region", region);
            t.putString("dungeon", dungeon);
            t.putInt("difficulty", diff);
            ClientNet.send(new Payloads.SocialAction("portaltool", t));
            onClose();
        });
    }
}
