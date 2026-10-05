package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.fx.CosmeticFx;
import com.mmorpg.client.hud.HudEffects;
import com.mmorpg.client.hud.HudLayout;
import com.mmorpg.client.hud.MmoHud;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Evenements client : HUD, cosmetiques, remise a zero a la deconnexion. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    /** Calques vanilla actuellement deplaces (transformation a retirer apres leur dessin). */
    private static final java.util.Set<Identifier> MOVED = new java.util.HashSet<>();

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        // les PV sont affiches par le cadre du joueur : on masque coeurs et armure vanilla
        event.replaceLayer(VanillaGuiLayers.PLAYER_HEALTH, (g, delta) -> {
        });
        event.replaceLayer(VanillaGuiLayers.ARMOR_LEVEL, (g, delta) -> {
        });
        event.replaceLayer(VanillaGuiLayers.TAB_LIST, com.mmorpg.client.hud.PlayerListHud::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR, MMORPG.id("hud"), MmoHud::render);
    }

    /** Elements vanilla personnalises (barre d'objets, faim, effets...) : masques ou deplaces par transformation. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        HudLayout.Element e = HudLayout.forLayer(event.getName());
        if (e == null) return;
        if (!e.visible() && !HudLayout.editing) {
            event.setCanceled(true);
            return;
        }
        if (HudLayout.pushVanilla(event.getGuiGraphics(), e)) MOVED.add(event.getName());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLayerPost(RenderGuiLayerEvent.Post event) {
        if (MOVED.remove(event.getName())) event.getGuiGraphics().pose().popMatrix();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        MmoHud.tick(mc);
        CosmeticFx.tick(mc);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientData.reset();
        HudEffects.clear();
    }
}
