package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.screen.AeloriaPauseScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Pack d'interface Aeloria (resourcepacks/aeloria_ui, genere par tools/build_ui_pack.py) : active en permanence, il habille tous les
 * ecrans de Minecraft aux couleurs du mod. Le menu Echap est remplace par le Menu Aeloria.
 */
public final class UiPack {
    private UiPack() {
    }

    @EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
    public static final class Register {
        private Register() {
        }

        @SubscribeEvent
        public static void onPackFinders(AddPackFindersEvent event) {
            if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
            event.addPackFinders(MMORPG.id("resourcepacks/aeloria_ui"), PackType.CLIENT_RESOURCES, Component.literal("Aeloria : interface MMORPG"),
                    PackSource.BUILT_IN, true, Pack.Position.TOP);
        }
    }

    /** Le menu Echap de Minecraft devient le « Menu Aeloria » (cartes, categories, recherche). */
    @EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
    public static final class Pause {
        private Pause() {
        }

        @SubscribeEvent
        public static void onScreen(ScreenEvent.Opening event) {
            if (event.getNewScreen().getClass() == PauseScreen.class && ((PauseScreen) event.getNewScreen()).showsPauseMenu()) {
                event.setNewScreen(new AeloriaPauseScreen());
            }
        }
    }
}
