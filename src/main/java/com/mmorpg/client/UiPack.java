package com.mmorpg.client;

import com.mmorpg.MMORPG;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
 * ecrans de Minecraft aux couleurs du mod. Le menu Echap recoit en plus un bandeau dore (titre en police Aeloria et ornements).
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

    @EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
    public static final class Decor {
        private Decor() {
        }

        /** Derriere les boutons du menu Echap : voile violet, bandeau du titre et filets d'or. */
        @SubscribeEvent
        public static void onBackground(ScreenEvent.Render.Background event) {
            if (!(event.getScreen() instanceof PauseScreen screen)) return;
            GuiGraphicsExtractor g = event.getGuiGraphics();
            int w = screen.width;
            int h = screen.height;
            g.fillGradient(0, 0, w, h, 0xB0140C22, 0xE0060310);
            // bandeau du haut
            int cx = w / 2;
            var title = AeloriaFont.of("AELORIA");
            int tw = screen.getFont().width(title);
            float scale = Math.min(3.2F, w * 0.6F / Math.max(1, tw));
            g.pose().pushMatrix();
            g.pose().translate(cx, 10);
            g.pose().scale(scale, scale);
            g.text(screen.getFont(), title, -tw / 2, 0, 0xFFE2B85A, true);
            g.pose().popMatrix();
            int half = Math.min(w / 4, 150);
            int y = 14 + Math.round(scale * 9);
            g.fill(cx - half, y, cx + half, y + 1, 0xFF7A5A22);
            g.fill(cx - half + 20, y + 1, cx + half - 20, y + 2, 0x80E2B85A);
            for (int sx : new int[]{cx - half, cx + half}) {
                for (int i = -3; i <= 3; i++) g.fill(sx - (3 - Math.abs(i)), y + i, sx + (3 - Math.abs(i)) + 1, y + i + 1, 0xFFE2B85A);
            }
            // pied : rappel des touches
            String hint = "M : menu du personnage     U : modifier l'interface";
            int hw = screen.getFont().width(hint);
            g.text(screen.getFont(), hint, cx - hw / 2, h - 14, 0xFF9C8A6A, true);
        }
    }
}
