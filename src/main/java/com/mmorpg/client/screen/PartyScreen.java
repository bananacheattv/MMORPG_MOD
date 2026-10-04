package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

/** Gestion du groupe : membres, invitations, chef, exclusion. */
public class PartyScreen extends MenuScreen {
    private EditBox nameBox;

    public PartyScreen() {
        super(Tab.GROUPE);
    }

    @Override
    protected void init() {
        super.init();
        nameBox = new EditBox(this.font, left + pw - 8 - 150, contentTop + 14, 100, 16, Component.literal("Joueur"));
        nameBox.setHint(Component.literal("Pseudo du joueur").withColor(0xFF807868));
        nameBox.setMaxLength(16);
        this.addRenderableWidget(nameBox);
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        CompoundTag party = ClientData.party;
        ListTag members = party.getListOrEmpty("members");
        boolean isLeader = party.getBooleanOr("isLeader", false);
        String myName = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getName().getString() : "";
        int x0 = left + 8;
        int y0 = contentTop;
        int lw = pw - 16 - 160;
        Ui.header(g, x0, y0, lw, members.isEmpty() ? "AUCUN GROUPE" : "GROUPE (" + members.size() + "/5)");

        String invite = party.getStringOr("invite", "");
        int ty = y0 + 14;
        if (!invite.isEmpty()) {
            Ui.frame(g, x0, ty, lw, 22, 0xFF60C0FF, true);
            g.text(font, font.plainSubstrByWidth(invite + " vous invite dans son groupe", lw - 130), x0 + 5, ty + 7, Ui.TEXT, false);
            button(g, x0 + lw - 122, ty + 3, 58, 16, "Accepter", true, 0xFF2A6A3A, () -> ClientNet.send(new Payloads.PartyAction(Payloads.PartyAction.ACCEPT, "")));
            button(g, x0 + lw - 61, ty + 3, 58, 16, "Refuser", true, 0xFF6A2A2A, () -> ClientNet.send(new Payloads.PartyAction(Payloads.PartyAction.DECLINE, "")));
            ty += 26;
        }
        if (members.isEmpty()) {
            ty += 4;
            ty += Ui.wrap(g, "Jouez à plusieurs ! Les membres d'un groupe partagent l'expérience des monstres vaincus (+10 % par membre), "
                    + "progressent ensemble dans leurs quêtes et ne peuvent pas se blesser entre eux.", x0, ty, lw, Ui.TEXT) + 6;
            Ui.wrap(g, "Invitez un joueur avec le champ à droite ou la commande /groupe inviter <joueur>. Discutez avec /g <message>.", x0, ty, lw, Ui.MUTED);
        }
        for (int i = 0; i < members.size(); i++) {
            CompoundTag m = members.getCompoundOrEmpty(i);
            int ry = ty + i * 30;
            boolean online = m.getBooleanOr("online", false);
            String name = m.getStringOr("name", "Hors ligne");
            PlayerClass cls = PlayerClass.byId(m.getIntOr("class", 0));
            Ui.frame(g, x0, ry, lw, 27, online ? cls.color : 0xFF505050, false);
            if (online) Ui.icon(g, Ui.classIcon(cls.id(), false), x0 + 3, ry + 3, 21, 32);
            String title = (m.getBooleanOr("leader", false) ? "♛ " : "") + name;
            g.text(font, title, x0 + 28, ry + 4, m.getBooleanOr("leader", false) ? Ui.GOLD_LIGHT : Ui.TEXT, true);
            if (online) {
                String lv = "Niv. " + m.getIntOr("level", 1) + " " + cls.title(m.getIntOr("level", 1));
                g.text(font, font.plainSubstrByWidth(lv, lw - 150), x0 + 28, ry + 15, cls.color, false);
                Ui.bar(g, x0 + lw - 120, ry + 5, 70, 6, m.getFloatOr("hp", 1), 0xFFE84848, 0xFF801818, null);
                Ui.bar(g, x0 + lw - 120, ry + 15, 70, 5, m.getFloatOr("mana", 1), 0xFF4C90FF, 0xFF1A3A90, null);
            }
            if (isLeader && !name.equals(myName) && online) {
                button(g, x0 + lw - 46, ry + 2, 42, 11, "Chef", true, 0xFF6A5A1A, () -> ClientNet.send(new Payloads.PartyAction(Payloads.PartyAction.PROMOTE, name)));
                button(g, x0 + lw - 46, ry + 14, 42, 11, "Exclure", true, 0xFF6A2A2A, () -> ClientNet.send(new Payloads.PartyAction(Payloads.PartyAction.KICK, name)));
            }
        }
        if (!members.isEmpty()) {
            button(g, x0, top + ph - 26, 120, 16, "Quitter le groupe", true, 0xFF6A2A2A, () -> ClientNet.send(new Payloads.PartyAction(Payloads.PartyAction.LEAVE, "")));
            g.text(font, "/g <message> : chat de groupe", x0 + 128, top + ph - 21, Ui.MUTED, false);
        }

        // ------------------------------------------------ invitation
        int ix = left + pw - 8 - 150;
        Ui.header(g, ix, y0, 150, "INVITER");
        boolean canInvite = members.isEmpty() || (isLeader && members.size() < 5);
        button(g, ix + 104, y0 + 14, 46, 16, "Inviter", canInvite, 0xFF2A5A7A, () -> {
            String n = nameBox.getValue().strip();
            if (!n.isEmpty()) {
                ClientNet.send(new Payloads.PartyAction(Payloads.PartyAction.INVITE, n));
                nameBox.setValue("");
            }
        });
        // joueurs proches a inviter d'un clic
        int py = y0 + 38;
        g.text(font, "Joueurs à proximité :", ix, py, Ui.MUTED, false);
        py += 11;
        Minecraft mc = Minecraft.getInstance();
        int shown = 0;
        if (mc.level != null && mc.player != null) {
            for (var p : mc.level.players()) {
                if (p == mc.player || p.distanceToSqr(mc.player) > 48 * 48 || shown >= 8) continue;
                String n = p.getName().getString();
                boolean inParty = false;
                for (int i = 0; i < members.size(); i++) {
                    if (n.equals(members.getCompoundOrEmpty(i).getStringOr("name", ""))) inParty = true;
                }
                if (inParty) continue;
                button(g, ix, py, 150, 14, n, canInvite, 0xFF2A2420, () -> ClientNet.send(new Payloads.PartyAction(Payloads.PartyAction.INVITE, n)));
                py += 16;
                shown++;
            }
        }
        if (shown == 0) g.text(font, "Personne autour de vous.", ix, py, 0xFF6A6458, false);
    }
}
