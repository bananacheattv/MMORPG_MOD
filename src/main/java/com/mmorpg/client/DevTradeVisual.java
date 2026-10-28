package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.screen.TradeScreen;
import net.minecraft.client.Minecraft;

/** Scenario de QA de /trade avec deux vrais clients (TradeA et TradeB) sur un serveur : clics reels sur l'ecran, captures et verification des inventaires. */
public final class DevTradeVisual {
    private DevTradeVisual() {
    }

    private static int tick, opened = -1;
    private static boolean doneShot;
    private static int finishAt;

    private static void shot(Minecraft mc, String name) {
        net.minecraft.client.Screenshot.grab(mc.gameDirectory, "showcase_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, msg -> MMORPG.LOGGER.info("[MMORPG] Capture {}", name));
    }

    private static void click(TradeScreen screen, int x, int y) {
        screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
    }

    private static int count(Minecraft mc, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) if (mc.player.getInventory().getItem(i).is(item)) n += mc.player.getInventory().getItem(i).getCount();
        return n;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        tick++;
        mc.gui.toastManager().clear();
        boolean a = mc.getUser().getName().equals("TradeA");
        String me = a ? "TradeA" : "TradeB";
        if (tick == 60) {
            mc.options.guiScale().set(2);
            mc.getWindow().setWindowed(1280, 720);
            mc.resizeGui();
        }
        if (tick == 90) {
            mc.gui.setScreen(null);
            mc.player.connection.sendCommand("clear @s");
            mc.player.connection.sendCommand("item replace entity @s hotbar.0 with minecraft:" + (a ? "diamond 5" : "emerald 3"));
            mc.player.connection.sendCommand("item replace entity @s hotbar.1 with minecraft:bread 4");
            mc.player.connection.sendCommand("mmorpg or " + me + " " + (a ? 500 : 120));
        }
        if (tick >= 130 && opened < 0) {
            if (mc.gui.screen() instanceof TradeScreen) {
                opened = tick;
            } else if (tick % 40 == 0) {
                mc.player.connection.sendCommand(a ? "trade TradeB" : "trade accepter");
            }
        }
        if (opened < 0) return;
        int t = tick - opened;
        if (!(mc.gui.screen() instanceof TradeScreen screen)) {
            if (t > 120 && !doneShot) {                                                       // l'ecran s'est ferme apres la confirmation : on verifie l'inventaire recu
                doneShot = true;
                finishAt = tick + 30;
            }
            if (doneShot && tick == finishAt - 10) {
                boolean ok = a ? count(mc, net.minecraft.world.item.Items.EMERALD) == 3 && count(mc, net.minecraft.world.item.Items.DIAMOND) == 0
                        : count(mc, net.minecraft.world.item.Items.DIAMOND) == 5 && count(mc, net.minecraft.world.item.Items.EMERALD) == 0;
                MMORPG.LOGGER.info("[TRADE VISUAL] {} {}", me, ok ? "CLIENT_TRADE_OK" : "CLIENT_TRADE_FAIL");
                shot(mc, "trade_done_" + me);
            }
            if (doneShot && tick >= finishAt) finish(mc, a);
            return;
        }
        int pw = Math.min(screen.width - 12, 440), ph = Math.min(screen.height - 12, 272);
        int left = (screen.width - pw) / 2, top = (screen.height - ph) / 2;
        int gx = left + (pw - 9 * 19) / 2;
        int by = top + ph - 40, bw = Math.min(140, (pw - 40) / 3);
        if (t == 20) shot(mc, "trade_open_" + me);
        if (t == 30) click(screen, gx + 0 * 19 + 9, top + 140 + 3 * 17 + 3 + 8);               // hotbar 0 : diamants / emeraudes
        if (t == 36 && a) click(screen, left + 14 + 28 + 40, top + 98 + 7);                      // champ d'or
        if (t == 38 && a) {
            for (var child : screen.children()) if (child instanceof net.minecraft.client.gui.components.EditBox box && box.getMessage().getString().equals("Or")) box.setValue("200");
            click(screen, left + 10 + 122 + 14, top + 96 + 7);                                   // bouton OK
        }
        if (t == 60) shot(mc, "trade_offers_" + me);
        if (t == 70) click(screen, left + 14 + bw / 2, by + 9);                                  // Valider mon offre
        if (t == 100) shot(mc, "trade_confirm_" + me);
        if (t == 140) click(screen, left + 14 + (bw + 20) / 2, by + 9);                          // Confirmer l'echange
    }

    private static void finish(Minecraft mc, boolean a) {
        mc.options.pauseOnLostFocus = true;
        mc.options.save();
        mc.stop();
    }
}
