package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.screen.CharacterScreen;
import com.mmorpg.client.screen.MenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

/**
 * Vitrine "revue" : reproduit les plantages corriges lors de la revue du code.
 * Bouton Personnage : le relachement du clic arrive sur le nouvel ecran avant sa premiere image ;
 * chemin d'evolution : fenetre etroite et haute (4:3) a l'echelle 3, libelles longs du Guerrier.
 */
final class DevReviewChecks {
    private static int tick = -1;
    private static int failures;

    private DevReviewChecks() {
    }

    private static void check(String name, Runnable action) {
        try {
            action.run();
            MMORPG.LOGGER.info("[MMORPG] REVUE {} OK", name);
        } catch (Throwable t) {
            failures++;
            MMORPG.LOGGER.error("[MMORPG] REVUE {} ECHEC", name, t);
        }
    }

    private static MouseButtonEvent click(double x, double y) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
    }

    static void tick(Minecraft mc) {
        if (com.mmorpg.client.cinematic.CinematicClient.active()) {
            com.mmorpg.client.cinematic.CinematicClient.skip();
            return;
        }
        if (LoadingClient.active()) return;
        tick++;
        if (tick == 20) {
            mc.getWindow().setWindowed(1280, 720);
            mc.options.guiScale().set(2);
            mc.resizeGui();
        }
        if (tick == 40 && !ClientData.DATA.playerClass.isPlayable())
            ClientNet.send(new com.mmorpg.network.Payloads.SelectClass(com.mmorpg.rpg.PlayerClass.GUERRIER.ordinal()));
        if (tick == 80) MenuScreen.open(MenuScreen.Tab.QUETES);
        if (tick == 100) check("personnage_clic", () -> {
            // meme tick, sans image entre les deux : clic sur l'onglet, puis relachement recu par l'ecran Personnage
            MenuScreen.open(MenuScreen.Tab.PERSONNAGE);
            var screen = mc.gui.screen();
            if (!(screen instanceof CharacterScreen)) throw new AssertionError("ecran inattendu : " + screen);
            screen.mouseReleased(click(screen.width / 2.0, screen.height / 2.0));
            screen.mouseClicked(click(screen.width / 2.0 + 30, screen.height / 2.0 + 20), false);
            screen.mouseReleased(click(screen.width / 2.0 + 30, screen.height / 2.0 + 20));
        });
        if (tick == 110) mc.gui.setScreen(null);
        if (tick == 120) {
            mc.getWindow().setWindowed(1152, 864);
            mc.options.guiScale().set(3);
            mc.resizeGui();
        }
        if (tick == 140) MenuScreen.open(MenuScreen.Tab.PERSONNAGE);
        if (tick == 170) {
            DevShowcase.shot(mc, "revue_personnage_4_3");
            MMORPG.LOGGER.info("[MMORPG] REVUE personnage_4_3 OK (ecran {} rendu en {}x{})", mc.gui.screen() == null ? "aucun" : mc.gui.screen().getClass().getSimpleName(),
                    mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
        if (tick == 180) {
            mc.gui.setScreen(null);
            mc.getWindow().setWindowed(1280, 720);
            mc.options.guiScale().set(2);
            mc.resizeGui();
            MMORPG.LOGGER.info("[MMORPG] REVUE TERMINEE : {} echec(s)", failures);
        }
        if (tick == 190) DevShowcase.stop(mc);
    }
}
