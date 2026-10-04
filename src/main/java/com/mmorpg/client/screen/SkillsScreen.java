package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.Keys;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Stat;
import com.mmorpg.skill.Skill;
import com.mmorpg.skill.Skills;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Arbre de competences : details, amelioration des rangs et assignation a la barre de raccourcis. */
public class SkillsScreen extends MenuScreen {
    private static String selected;

    public SkillsScreen() {
        super(Tab.COMPETENCES);
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        PlayerData d = ClientData.DATA;
        List<Skill> skills = Skills.forClass(d.playerClass);
        if (selected == null || Skills.get(selected) == null || Skills.get(selected).playerClass != d.playerClass) {
            selected = skills.isEmpty() ? null : skills.get(0).id;
        }
        int x0 = left + 8;
        int y0 = contentTop;
        int listW = 170;
        Ui.header(g, x0, y0, listW, "COMPÉTENCES — " + d.playerClass.label.toUpperCase(java.util.Locale.ROOT));
        int rowH = Math.max(14, Math.min(26, (ph - 30 - 70 - 16) / Math.max(1, skills.size())));
        boolean compact = rowH < 22;
        for (int i = 0; i < skills.size(); i++) {
            Skill s = skills.get(i);
            int ry = y0 + 14 + i * rowH;
            boolean unlocked = d.level >= s.unlockLevel;
            int rank = d.skillRank(s.id);
            boolean sel = s.id.equals(selected);
            Ui.frame(g, x0, ry, listW, rowH - 2, d.playerClass.color, sel || hovered(x0, ry, listW, rowH - 2));
            int iconSize = rowH - 6;
            Ui.icon(g, Ui.skillIcon(s.id), x0 + 2, ry + 2, iconSize, 32, unlocked ? 0xFFFFFFFF : 0xFF505050);
            int nameX = x0 + iconSize + 6;
            if (compact) {
                // une seule ligne : nom a gauche, rang ou niveau requis a droite
                String right = unlocked ? "" : "Niv. " + s.unlockLevel;
                int rightW = unlocked ? Skill.MAX_RANK * 6 : font.width(right);
                int ty = ry + (rowH - 2 - 8) / 2 + 1;
                g.text(font, font.plainSubstrByWidth(s.name, x0 + listW - 6 - rightW - nameX - 3), nameX, ty, unlocked ? Ui.TEXT : Ui.MUTED, false);
                if (unlocked) {
                    for (int p = 0; p < Skill.MAX_RANK; p++) {
                        Ui.diamond(g, x0 + listW - 6 - (Skill.MAX_RANK - 1 - p) * 6, ty + 3, 2, p < rank ? Ui.GOLD : 0xFF3A3030);
                    }
                } else {
                    g.text(font, right, x0 + listW - 5 - rightW, ty, Ui.RED, false);
                }
            } else {
                g.text(font, s.name, nameX, ry + 3, unlocked ? Ui.TEXT : Ui.MUTED, false);
                if (unlocked) {
                    for (int p = 0; p < Skill.MAX_RANK; p++) {
                        Ui.diamond(g, nameX + p * 7 + 2, ry + rowH - 9, 2, p < rank ? Ui.GOLD : 0xFF3A3030);
                    }
                    g.text(font, s.typeLabel(), x0 + listW - 4 - font.width(s.typeLabel()), ry + rowH - 12, s.ultimate ? 0xFFFF8040 : s.passive ? 0xFF80C0FF : Ui.MUTED, false);
                } else {
                    g.text(font, "Niveau " + s.unlockLevel, nameX, ry + rowH - 12, Ui.RED, false);
                }
            }
            final String id = s.id;
            click(x0, ry, listW, rowH - 2, () -> selected = id);
        }

        // ------------------------------------------------ details
        int dx = x0 + listW + 10;
        int dw = left + pw - 8 - dx;
        int dh = ph - 30 - 70;
        Ui.inset(g, dx, y0, dw, dh);
        Skill s = Skills.get(selected);
        if (s != null) {
            int rank = Math.max(1, d.skillRank(s.id));
            boolean unlocked = d.isSkillUnlocked(s);
            Ui.icon(g, Ui.skillIcon(s.id), dx + 6, y0 + 6, 32, 32, unlocked ? 0xFFFFFFFF : 0xFF606060);
            Ui.border(g, dx + 5, y0 + 5, 34, 34, Ui.GOLD_DARK);
            g.text(font, s.name, dx + 44, y0 + 7, d.playerClass.color, true);
            g.text(font, s.typeLabel() + " — Niveau " + s.unlockLevel, dx + 44, y0 + 18, Ui.MUTED, false);
            if (!s.passive) {
                String info = "Mana " + s.manaCost + "   Recharge " + Skill.sec(Math.round(s.cooldown * (1 - ClientData.stats.get(Stat.CDR) / 100f)));
                g.text(font, info, dx + 44, y0 + 28, Ui.BLUE, false);
            }
            int ty = y0 + 42;
            int by = y0 + dh - 20;
            g.text(font, "Rang " + (unlocked ? d.skillRank(s.id) : 0) + " / " + Skill.MAX_RANK, dx + 6, ty, Ui.GOLD, true);
            ty += 11;
            g.enableScissor(dx + 2, ty, dx + dw - 2, by - 2);
            ty += Ui.wrap(g, s.describe(rank), dx + 6, ty, dw - 12, Ui.TEXT);
            if (unlocked && d.skillRank(s.id) < Skill.MAX_RANK) {
                String next = s.describe(rank + 1);
                if (ty + 14 + Ui.wrapHeight(next, dw - 12) <= by - 2) {
                    ty += 4;
                    g.text(font, "Rang suivant :", dx + 6, ty, 0xFF80E080, false);
                    ty += 10;
                    Ui.wrap(g, next, dx + 6, ty, dw - 12, 0xFF9ACC9A);
                }
            }
            g.disableScissor();
            if (hovered(dx, y0 + 42, dw, by - y0 - 42) && unlocked && d.skillRank(s.id) < Skill.MAX_RANK) {
                tooltip(g, List.of(Component.literal("Rang suivant").withColor(0xFF80E080),
                        Component.literal(s.describe(rank + 1)).withColor(Ui.TEXT)));
            }
            String pts = "Points : " + ClientData.skillPoints;
            g.text(font, pts, dx + 6, by + 5, ClientData.skillPoints > 0 ? Ui.GOLD_LIGHT : Ui.MUTED, true);
            boolean canUp = unlocked && d.skillRank(s.id) < Skill.MAX_RANK && ClientData.skillPoints > 0;
            button(g, dx + dw - 96, by, 90, 16, unlocked && d.skillRank(s.id) >= Skill.MAX_RANK ? "Rang max" : "Améliorer", canUp, 0xFF3A6A2A,
                    () -> ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.UPGRADE, 0, s.id)));
        }

        // ------------------------------------------------ barre de competences
        int barY = top + ph - 62;
        Ui.header(g, x0 + listW + 10, barY - 2, 160, "BARRE DE COMPÉTENCES");
        int slotSize = 26;
        int bx = dx;
        for (int i = 0; i < PlayerData.SKILL_SLOTS; i++) {
            int sx = bx + i * (slotSize + 6);
            int sy = barY + 12;
            boolean hover = hovered(sx, sy, slotSize, slotSize);
            Ui.frame(g, sx, sy, slotSize, slotSize, Ui.GOLD, hover);
            String sid = d.skillBar[i];
            if (!sid.isEmpty() && Skills.get(sid) != null) {
                Ui.icon(g, Ui.skillIcon(sid), sx + 1, sy + 1, slotSize - 2, 32);
            }
            String key = Keys.SKILLS[i].getTranslatedKeyMessage().getString();
            g.text(font, key.length() > 3 ? key.substring(0, 3) : key, sx + 2, sy + slotSize - 9, 0xFFFFFFFF, true);
            final int slot = i;
            click(sx, sy, slotSize, slotSize,
                    () -> {
                        Skill sel = Skills.get(selected);
                        if (sel != null && !sel.passive && d.isSkillUnlocked(sel)) {
                            ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.ASSIGN, slot, sel.id));
                        }
                    },
                    () -> ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.ASSIGN, slot, "")));
            if (hover && !sid.isEmpty() && Skills.get(sid) != null) {
                tooltip(g, List.of(Component.literal(Skills.get(sid).name).withColor(Ui.GOLD),
                        Component.literal("Clic droit : retirer").withColor(Ui.MUTED)));
            }
        }
        g.text(font, "Sélectionnez une compétence puis cliquez sur un emplacement.", x0, top + ph - 14, Ui.MUTED, false);
    }
}
