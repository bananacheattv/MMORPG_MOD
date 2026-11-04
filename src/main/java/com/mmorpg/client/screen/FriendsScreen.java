package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.*;
import com.mmorpg.network.Payloads;
import com.mmorpg.social.FriendService;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Liste d'amis : statuts, demandes en attente, ajout par pseudo. */
public final class FriendsScreen extends MmoScreen {
    private final CompoundTag data;
    private EditBox name;
    private int page;

    public FriendsScreen(CompoundTag data) { super("Amis"); this.data = data; }

    @Override protected MenuScreen.Tab navActive() { return MenuScreen.Tab.AMIS; }

    @Override protected int designHeight() { return 340; }

    @Override protected void init() {
        super.init();
        name = new EditBox(font, left + 12, top + 50, pw / 2 - 20, 18, Component.literal("Pseudo"));
        name.setMaxLength(32);
        addRenderableWidget(name);
    }

    private void send(String action, java.util.function.Consumer<CompoundTag> fill) {
        var t = new CompoundTag(); t.putString("action", action); fill.accept(t);
        ClientNet.send(new Payloads.SocialAction("friends", t));
    }

    private static String label(String state) {
        return switch (state) { case "EN_LIGNE" -> "En ligne"; case "OCCUPE" -> "Occupé"; case "ABSENT" -> "Absent"; case "INVISIBLE" -> "Invisible"; default -> "Hors ligne"; };
    }
    private static int color(String state) {
        return switch (state) { case "EN_LIGNE" -> 0xFF3FB868; case "OCCUPE" -> 0xFFC8363E; case "ABSENT" -> 0xFFE6B83A; default -> 0xFF6A6478; };
    }

    @Override protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.frame(g, left, top, pw, ph, Ui.GOLD, false);
        g.centeredText(font, Component.literal("AMIS"), left + pw / 2, top + 8, Ui.GOLD);
        g.text(font, "Ajouter un joueur connecté", left + 12, top + 36, Ui.MUTED, false);
        button(g, left + pw / 2 - 4, top + 50, 110, 18, "Envoyer la demande", !name.getValue().isBlank(), Ui.GREEN, () -> send("add", t -> t.putString("name", name.getValue())));
        String mine = data.getStringOr("mine", "EN_LIGNE");
        g.text(font, "Mon statut :", left + 12, top + 78, Ui.MUTED, false);
        int w = (pw - 100) / 4;
        for (int i = 0; i < FriendService.STATUS.size(); i++) {
            String s = FriendService.STATUS.get(i);
            button(g, left + 84 + i * w, top + 74, w - 3, 16, label(s), true, mine.equals(s) ? Ui.GOLD : Ui.MUTED, () -> send("status", t -> t.putString("value", s)));
        }
        var requests = data.getListOrEmpty("requests");
        int y = top + 98;
        for (int i = 0; i < Math.min(3, requests.size()); i++) {
            var r = requests.getCompoundOrEmpty(i); String id = r.getStringOr("id", "");
            g.text(font, font.plainSubstrByWidth("Demande : " + r.getStringOr("name", "?"), pw - 190), left + 12, y + 4, Ui.TEXT, false);
            button(g, left + pw - 168, y, 76, 16, "Accepter", true, Ui.GREEN, () -> send("accept", t -> t.putString("id", id)));
            button(g, left + pw - 88, y, 76, 16, "Refuser", true, Ui.RED, () -> send("decline", t -> t.putString("id", id)));
            y += 19;
        }
        var friends = data.getListOrEmpty("friends");
        int rows = Math.max(1, (top + ph - 60 - y) / 19);
        var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(java.time.ZoneId.systemDefault());
        for (int i = 0; i < rows && page * rows + i < friends.size(); i++) {
            var f = friends.getCompoundOrEmpty(page * rows + i); String id = f.getStringOr("id", ""), state = f.getStringOr("state", "HORS_LIGNE");
            int ry = y + 6 + i * 19;
            g.fill(left + 12, ry + 3, left + 21, ry + 12, color(state));
            long seen = f.getLongOr("seen", 0);
            String info = label(state) + (state.equals("HORS_LIGNE") && seen > 0 ? " · vu le " + fmt.format(java.time.Instant.ofEpochMilli(seen)) : "");
            g.text(font, font.plainSubstrByWidth(f.getStringOr("name", "?") + " · " + info, pw - 130), left + 28, ry + 4, state.equals("HORS_LIGNE") ? Ui.MUTED : Ui.TEXT, false);
            button(g, left + pw - 88, ry, 76, 16, "Retirer", true, Ui.RED, () -> send("remove", t -> t.putString("id", id)));
        }
        if (friends.isEmpty()) g.text(font, "Aucun ami pour l’instant.", left + 12, y + 8, Ui.MUTED, false);
        g.text(font, font.plainSubstrByWidth(data.getStringOr("status", ""), pw - 24), left + 12, top + ph - 38, Ui.GOLD, false);
        button(g, left + 12, top + ph - 23, 30, 17, "‹", page > 0, Ui.GOLD, () -> page--);
        button(g, left + 48, top + ph - 23, 30, 17, "›", (page + 1) * rows < friends.size(), Ui.GOLD, () -> page++);
        button(g, left + pw - 102, top + ph - 23, 90, 17, "Fermer", true, Ui.MUTED, this::onClose);
    }
}
