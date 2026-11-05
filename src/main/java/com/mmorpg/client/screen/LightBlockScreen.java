package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** Reglage du niveau de lumiere (0-15) d'un bloc de lumiere invisible ; applique immediatement. */
public class LightBlockScreen extends MmoScreen {
    private final BlockPos pos;
    private int level;
    private int range;
    private boolean open;
    private static final int[] RANGES = {15, 25, 40, 60, 80, 100};

    public LightBlockScreen(CompoundTag data) {
        super("Bloc de lumière");
        this.pos = BlockPos.of(data.getLongOr("pos", 0));
        this.level = data.getIntOr("level", 15);
        this.range = data.getIntOr("range", 0);
    }

    @Override
    protected boolean compact() { return true; }

    @Override
    protected int panelWidth() { return Math.min(this.width - 12, 320); }

    @Override
    protected int panelHeight() { return Math.min(this.height - 12, 150); }

    private void set(int v) {
        level = Math.max(0, Math.min(15, v));
        CompoundTag t = new CompoundTag();
        t.putLong("pos", pos.asLong());
        t.putInt("level", level);
        t.putInt("range", range);
        ClientNet.send(new Payloads.SocialAction("lightblock", t));
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "BLOC DE LUMIÈRE", Ui.GOLD_LIGHT);
        g.text(font, "Niveau de lumière : " + level, left + 16, top + 28, Ui.TEXT, false);
        if (open) {
            int bw = (pw - 32) / 2;
            for (int i = 0; i < RANGES.length; i++) {
                final int v = i;
                button(g, left + 16 + (i % 2) * bw, top + 44 + (i / 2) * 22, bw - 3, 18, RANGES[i] + " blocs", true,
                        range == i ? 0xFFE2B85A : 0xFF3B3350, () -> { range = v; open = false; set(level); });
            }
        } else {
            int bw = (pw - 32) / 8;
            for (int i = 0; i < 16; i++) {
                final int v = i;
                button(g, left + 16 + (i % 8) * bw, top + 44 + (i / 8) * 22, bw - 3, 18, String.valueOf(i), true,
                        level == i ? 0xFFE2B85A : 0xFF3B3350, () -> set(v));
            }
            g.text(font, "Portée :", left + 16, top + 98, Ui.MUTED, false);
            button(g, left + 66, top + 93, pw - 82, 18, RANGES[Math.max(0, Math.min(5, range))] + " blocs  ▼", true, 0xFF3B3350, () -> open = true);
        }
        button(g, left + pw / 2 - 40, top + ph - 24, 80, 16, "Fermer", true, 0xFF2A6A3A, this::onClose);
    }
}
