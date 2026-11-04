package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import com.mmorpg.progress.Progress;
import com.mmorpg.rpg.LevelSystem;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;

/** Arbre de talents secondaire et prestige. */
public class TalentsScreen extends MenuScreen {
    public TalentsScreen() {
        super(Tab.TALENTS);
    }

    private static void send(String action, String id) {
        CompoundTag t = new CompoundTag();
        t.putString("action", action);
        t.putString("id", id);
        ClientNet.send(new Payloads.SocialAction("progress", t));
    }

    private boolean confirmPrestige;

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        PlayerData d = ClientData.DATA;
        int x0 = left + 8, y0 = contentTop;
        int free = Progress.talentPointsFree(d);
        Ui.header(g, x0, y0, pw - 16, "TALENTS — points libres : " + free + " / " + Progress.talentPointsTotal(d) + "   (1 point tous les 5 niveaux)");
        String[] branches = {"Combat", "Survie", "Maîtrise"};
        int colW = (pw - 16 - 20) / 3;
        for (int b = 0; b < 3; b++) {
            int cx = x0 + b * (colW + 10);
            g.text(font, branches[b], cx + 2, y0 + 18, Ui.GOLD_LIGHT, true);
            int row = 0;
            for (Progress.Talent t : Progress.TALENTS) {
                if (!t.branch().equals(branches[b])) continue;
                int ry = y0 + 30 + row++ * 26;
                int rank = d.talents.getOrDefault(t.id(), 0);
                boolean can = free > 0 && rank < Progress.MAX_TALENT_RANK;
                Ui.frame(g, cx, ry, colW, 24, Ui.GOLD, hovered(cx, ry, colW, 24) && can);
                g.text(font, font.plainSubstrByWidth(t.name(), colW - 50), cx + 4, ry + 3, Ui.TEXT, false);
                g.text(font, font.plainSubstrByWidth(t.desc(), colW - 8), cx + 4, ry + 13, Ui.MUTED, false);
                for (int p = 0; p < Progress.MAX_TALENT_RANK; p++)
                    Ui.diamond(g, cx + colW - 6 - (Progress.MAX_TALENT_RANK - 1 - p) * 7, ry + 6, 2, p < rank ? Ui.GOLD : 0xFF3A3030);
                final String id = t.id();
                if (can) click(cx, ry, colW, 24, () -> send("talent", id));
            }
        }
        int by = y0 + 30 + 4 * 26 + 2;
        button(g, x0, by, 150, 16, "Réinitialiser (" + Progress.respecCost(d) + " or)", !d.talents.isEmpty() && d.gold >= Progress.respecCost(d), 0xFF6A3A2A, () -> send("respec", ""));
        // prestige
        int py = by + 22;
        Ui.header(g, x0, py, pw - 16, "PRESTIGE");
        String msg = d.prestige > 0 ? "Prestige accompli : +10 % PV/ATQ/MAG/DEF, +10 à chaque attribut, Drake ascendant obtenu."
                : "Niveau 100, une seule fois : retour niveau 1 (équipement et or gardés) pour +10 % PV/ATQ/MAG/DEF, +10 attributs, 30 points de talent et le Drake ascendant.";
        int h = Ui.wrap(g, msg, x0, py + 16, pw - 16 - 170, d.prestige > 0 ? Ui.GREEN : Ui.TEXT);
        if (d.prestige == 0) {
            boolean ok = d.level >= LevelSystem.MAX_LEVEL;
            if (!confirmPrestige) button(g, x0 + pw - 16 - 160, py + 16, 160, 18, "Prestige", ok, 0xFF7A5A10, () -> confirmPrestige = true);
            else button(g, x0 + pw - 16 - 160, py + 16, 160, 18, "CONFIRMER (définitif)", ok, 0xFF8A2A2A, () -> {
                confirmPrestige = false;
                send("prestige", "");
            });
        }
    }
}
