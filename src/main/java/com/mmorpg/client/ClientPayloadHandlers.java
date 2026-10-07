package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.hud.HudEffects;
import com.mmorpg.client.screen.ClassSelectScreen;
import com.mmorpg.client.screen.ForgeScreen;
import com.mmorpg.client.screen.TeleporterScreen;
import com.mmorpg.client.screen.TeleporterSetupScreen;
import com.mmorpg.network.Payloads;
import com.mmorpg.network.S2COpenScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Gestionnaires client des paquets declares dans Net (executes sur le thread principal). */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    @SubscribeEvent
    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(S2COpenScreen.TYPE, (payload, context) -> openScreen(payload));
        event.register(Payloads.SyncPlayer.TYPE, (payload, context) -> ClientData.load(payload.data()));
        event.register(Payloads.Vitals.TYPE, (payload, context) -> {
            ClientData.hp = payload.hp();
            ClientData.maxHp = payload.maxHp();
            ClientData.mana = payload.mana();
            ClientData.maxMana = payload.maxMana();
        });
        event.register(Payloads.MobInfo.TYPE, (payload, context) -> ClientData.mobInfo = payload.data().copy());
        event.register(Payloads.Notify.TYPE, (payload, context) -> HudEffects.addBanner(payload));
        event.register(Payloads.PartySync.TYPE, (payload, context) -> ClientData.party = payload.data().copy());
        event.register(Payloads.CastAnimation.TYPE, (payload, context) ->
                com.mmorpg.client.fx.CastAnimations.start(payload.entityId(), payload.anim(), payload.duration()));
        event.register(Payloads.CombatText.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.distanceToSqr(payload.x(), payload.y(), payload.z()) < 40 * 40) {
                HudEffects.addText(payload);
            }
        });
    }

    private static void openScreen(S2COpenScreen payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        switch (payload.screen()) {
            case S2COpenScreen.QUEST_EDITOR -> {
                if (mc.gui.screen() instanceof com.mmorpg.client.screen.QuestEditorScreen screen) screen.receive(payload.extra());
                else if (!payload.extra().contains("status")) mc.gui.setScreen(new com.mmorpg.client.screen.QuestEditorScreen(payload.extra()));
            }
            case S2COpenScreen.CLASS_SELECT -> mc.gui.setScreen(new ClassSelectScreen());
            case S2COpenScreen.TELEPORTER -> mc.gui.setScreen(new TeleporterScreen(payload.extra()));
            case S2COpenScreen.TELEPORTER_SETUP -> mc.gui.setScreen(new TeleporterSetupScreen(payload.extra()));
            case S2COpenScreen.FORGE -> mc.gui.setScreen(new ForgeScreen(BlockPos.of(payload.extra().getLongOr("pos", 0))));
            case S2COpenScreen.QUEST_GIVER -> mc.gui.setScreen(new com.mmorpg.client.screen.QuestGiverScreen(payload.extra()));
            case S2COpenScreen.SHOP -> mc.gui.setScreen(new com.mmorpg.client.screen.ShopScreen(payload.extra()));
            case S2COpenScreen.LUCKY_BLOCK -> mc.gui.setScreen(new com.mmorpg.client.screen.LuckyBlockScreen(payload.extra()));
            case S2COpenScreen.LUCKY_RESULT -> {
                if (mc.gui.screen() instanceof com.mmorpg.client.screen.LuckyBlockScreen screen) screen.result(payload.extra());
            }
            default -> MMORPG.LOGGER.warn("Ecran MMORPG inconnu : {}", payload.screen());
        }
    }
}
