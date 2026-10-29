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
        event.registerAbove(VanillaGuiLayers.HOTBAR, FishFightClient.LAYER, FishFightClient::render);
        event.registerAboveAll(com.mmorpg.client.BossRevealClient.LAYER, com.mmorpg.client.BossRevealClient::render);
        event.registerAboveAll(com.mmorpg.client.LoadingClient.LAYER, com.mmorpg.client.LoadingClient::render);
        event.registerAboveAll(com.mmorpg.client.AreaEntryClient.LAYER, com.mmorpg.client.AreaEntryClient::render);
        event.registerAboveAll(com.mmorpg.client.cinematic.CinematicClient.LAYER, com.mmorpg.client.cinematic.CinematicClient::render);
    }

    /** Elements vanilla personnalises (barre d'objets, faim, effets...) : masques ou deplaces par transformation. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        if (Minecraft.getInstance().gui.hud.isHidden() && MMORPG.MODID.equals(event.getName().getNamespace())) {
            event.setCanceled(true);
            return;
        }
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
        ArenaResultDelivery.tick(mc);
        com.mmorpg.client.hud.GuidanceArrow.update(mc);
        CosmeticFx.tick(mc);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        com.mmorpg.client.hud.GuidanceArrow.clear();
        ClientData.reset();
        HudEffects.clear();
    }
}
