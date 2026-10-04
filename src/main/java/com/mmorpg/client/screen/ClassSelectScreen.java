package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.skill.Skill;
import com.mmorpg.skill.Skills;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Choix de la classe : quatre cartes detaillees (role, description, notes, evolutions, competences). */
public class ClassSelectScreen extends MmoScreen {
    private static final PlayerClass[] CLASSES = {PlayerClass.GUERRIER, PlayerClass.MAGE, PlayerClass.ARCHER, PlayerClass.TANK};
    private static final String[] RATINGS = {"Attaque", "Défense", "Magie", "Mobilité", "Difficulté"};
    private PlayerClass chosen;

    public ClassSelectScreen() {
        super("Choix de la classe");
    }

    @Override
    protected int panelWidth() {
        return Math.min(this.width - 8, 520);
    }

    @Override
    protected int panelHeight() {
        return Math.min(this.height - 8, 300);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "CHOISISSEZ VOTRE DESTINÉE", Ui.GOLD_LIGHT);
        int gap = 6;
        int cw = (pw - 16 - gap * 3) / 4;
        int ch = ph - 30 - 30;
        for (int i = 0; i < CLASSES.length; i++) {
            PlayerClass c = CLASSES[i];
            int cx = left + 8 + i * (cw + gap);
            int cy = top + 24;
            boolean hover = hovered(cx, cy, cw, ch);
            boolean sel = c == chosen;
            Ui.frame(g, cx, cy, cw, ch, c.color, hover || sel);
            if (sel) Ui.border(g, cx + 1, cy + 1, cw - 2, ch - 2, Ui.GOLD_LIGHT);
            g.fillGradient(cx + 1, cy + 1, cx + cw - 1, cy + 52, Ui.withAlpha(c.color, 0x70), 0x00000000);
            int by0 = cy + ch - 24;
            int iconSize = Math.max(24, Math.min(48, Math.min(cw - 20, ch / 5)));
            Ui.icon(g, Ui.classIcon(c.id(), true), cx + (cw - iconSize) / 2, cy + 5, iconSize, 64);
            int ty = cy + 8 + iconSize;
            Ui.scaledText(g, c.label.toUpperCase(java.util.Locale.ROOT), cx + cw / 2f, ty, 1.3f, c.color, true);
            ty += 13;
            int ratingsH = RATINGS.length * 10;
            int roleLines = Math.max(1, Math.min(3, (by0 - ty - ratingsH - 2) / 10));
            java.util.List<net.minecraft.util.FormattedCharSequence> role = font.split(net.minecraft.network.chat.FormattedText.of(c.role), cw - 10);
            for (int l = 0; l < Math.min(roleLines, role.size()); l++) {
                g.text(font, role.get(l), cx + 5, ty, Ui.MUTED, false);
                ty += 10;
            }
            ty += 2;
            for (int r = 0; r < RATINGS.length && ty + 9 <= by0; r++) {
                g.text(font, RATINGS[r], cx + 5, ty, Ui.TEXT, false);
                for (int p = 0; p < 5; p++) {
                    int px = cx + cw - 8 - (4 - p) * 7;
                    Ui.diamond(g, px, ty + 4, 2, p < c.ratings[r] ? (r == 4 ? 0xFFFF7050 : Ui.GOLD) : 0xFF3A3030);
                }
                ty += 10;
            }
            ty += 3;
            if (ty + 19 <= by0) {
                g.text(font, "Évolutions :", cx + 5, ty, Ui.GOLD, false);
                ty += 10;
                for (int e = 1; e < 5 && ty + 9 <= by0; e++) {
                    g.text(font, font.plainSubstrByWidth("Niv. " + PlayerClass.EVOLUTION_LEVELS[e] + " " + c.evolutions[e], cw - 10), cx + 5, ty, 0xFFC8C0B0, false);
                    ty += 9;
                }
            }
            int by = cy + ch - 22;
            button(g, cx + 6, by, cw - 12, 16, sel ? "Sélectionné" : "Choisir", true, sel ? 0xFF6A5A1A : Ui.darker(c.color, 0.8f), () -> chosen = c);
            click(cx, cy, cw, ch - 24, () -> chosen = c);
            if (hover && my < by) {
                List<Skill> skills = Skills.forClass(c);
                java.util.ArrayList<Component> tip = new java.util.ArrayList<>();
                tip.add(Component.literal(c.label).withColor(c.color));
                for (String line : c.description.split("(?<=\\.) ")) tip.add(Component.literal(line).withColor(Ui.TEXT));
                tip.add(Component.literal("Compétences :").withColor(Ui.GOLD));
                for (Skill s : skills) {
                    tip.add(Component.literal(" • " + s.name + " (niv. " + s.unlockLevel + ", " + s.typeLabel().toLowerCase(java.util.Locale.ROOT) + ")").withColor(0xFFC8C0B0));
                }
                tooltip(g, tip);
            }
        }
        int by = top + ph - 26;
        if (chosen != null) {
            button(g, left + pw / 2 - 90, by, 180, 18, "Devenir " + chosen.label, true, 0xFF2A6A2A, () -> {
                ClientNet.send(new Payloads.SelectClass(chosen.ordinal()));
                onClose();
            });
        } else {
            g.centeredText(font, Component.literal("Survolez une classe pour voir ses compétences, puis choisissez-la."), left + pw / 2, by + 5, Ui.MUTED);
        }
    }
}
