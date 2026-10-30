package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.chat.ChatChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Scenario de QA du selecteur de canal : onglets, filtrage, non-lus et routage reel d'un message tape dans l'onglet Guilde. */
public final class DevChatVisual {
    private DevChatVisual() {
    }

    private static int tick;

    private static void shot(Minecraft mc, String name) {
        net.minecraft.client.Screenshot.grab(mc.gameDirectory, "showcase_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, msg -> MMORPG.LOGGER.info("[MMORPG] Capture {}", name));
    }

    private static void cmd(Minecraft mc, String c) {
        mc.player.connection.sendCommand(c);
    }

    private static void key(ChatScreen s, int key, int mods) {
        key(s, key, mods, 0);
    }

    private static void key(ChatScreen s, int key, int mods, int keycode) {
        KeyEvent e = new KeyEvent(key, keycode, mods);
        ScreenEvent.KeyPressed.Pre pre = new ScreenEvent.KeyPressed.Pre(s, e);
        ChatChannels.onKey(pre);
        if (!pre.isCanceled()) s.keyPressed(e);
        ChatChannels.onKeyPost(new ScreenEvent.KeyPressed.Post(s, e));
    }

    private static void click(ChatScreen s, double x, double y) {
        var ev = new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, 0));
        MMORPG.LOGGER.info("[CHAT] screen.mouseClicked={}", s.mouseClicked(ev, false));
    }

    private static EditBox box(ChatScreen s) {
        for (var c : s.children()) if (c instanceof EditBox b) return b;
        return null;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        tick++;
        mc.gui.toastManager().clear();
        if (tick == 60) {
            mc.getWindow().setWindowed(1280, 720);
            mc.options.guiScale().set(2);
            mc.resizeGui();
            ChatChannels.select(ChatChannel.GENERAL);
        }
        if (tick == 70 || tick == 110 || tick == 150) com.mmorpg.client.cinematic.CinematicClient.skip();
        if (tick == 170) mc.gui.setScreen(null);
        if (tick == 200) {
            var t = new net.minecraft.nbt.CompoundTag();
            t.putString("action", "create");
            t.putString("name", "Veilleurs");
            ClientNet.send(new com.mmorpg.network.Payloads.SocialAction("guild", t));
        }
        if (tick == 260) cmd(mc, "g Bonjour la guilde");
        if (tick == 300) mc.player.connection.sendChat("Salut tout le monde");
        if (tick == 340) cmd(mc, "g Deuxieme message de guilde");
        if (tick == 380) mc.gui.setScreen(new ChatScreen("", false));
        if (tick == 410) shot(mc, "chat_canaux_general");
        if (tick == 420 && mc.gui.screen() instanceof ChatScreen s) key(s, 79, 256);   // Alt + droite : Guilde
        if (tick == 430 && mc.gui.screen() instanceof ChatScreen s) box(s).setValue("Message tape sans /g");
        if (tick == 450) { shot(mc, "chat_canaux_guilde_saisie"); MMORPG.LOGGER.info("[CHAT] canal={} route={}", ChatChannels.active(), ChatChannels.active().route("Message tape sans /g")); }
        if (tick == 455 && mc.gui.screen() instanceof ChatScreen s) key(s, 40, 0);      // Entree : part dans /g
        if (tick == 515) mc.gui.hud.getChat().addRecentChat("/g ancien message");
        if (tick == 520) mc.gui.setScreen(new ChatScreen("", false));
        if (tick == 530 && mc.gui.screen() instanceof ChatScreen s) {   // fleche haut (historique) puis edition
            key(s, 82, 0, 1073741906);
            MMORPG.LOGGER.info("[CHAT] apres haut: valeur='{}' focus={} focusedWidget={}", box(s).getValue(), box(s).isFocused(), s.getFocused());
            s.charTyped(new net.minecraft.client.input.CharacterEvent('X'));
            MMORPG.LOGGER.info("[CHAT] apres frappe: valeur='{}'", box(s).getValue());
        }
        if (tick == 550) shot(mc, "chat_canaux_guilde");
        if (tick == 560 && mc.gui.screen() instanceof ChatScreen s) key(s, 43, 0);      // Tab : Groupe
        if (tick == 580) shot(mc, "chat_canaux_groupe");
        if (tick == 590 && mc.gui.screen() instanceof ChatScreen s) key(s, 43, 1);      // Maj+Tab : retour Guilde
        if (tick == 595 && mc.gui.screen() instanceof ChatScreen s) {   // clic reel sur l'onglet General, puis sur l'icone message
            click(s, 20, s.height - 15 - 13 + 5);
            MMORPG.LOGGER.info("[CHAT] clic onglet -> {}", ChatChannels.active());
            click(s, 10, s.height - 8);
            MMORPG.LOGGER.info("[CHAT] clic icone -> {}", ChatChannels.active());
            shot(mc, "chat_canaux_icone");
        }
        if (tick == 600) { MMORPG.LOGGER.info("[CHAT] fin canal={}", ChatChannels.active()); ChatChannels.select(ChatChannel.GENERAL); mc.gui.setScreen(null); }
        if (tick == 620) mc.stop();
    }
}
