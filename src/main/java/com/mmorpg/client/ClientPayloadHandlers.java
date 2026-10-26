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
        event.register(com.mmorpg.network.ProxyConnect.TYPE, (payload, context) -> { });
        event.register(S2COpenScreen.TYPE, (payload, context) -> openScreen(payload));
        event.register(Payloads.AdminReply.TYPE, (payload, context) -> com.mmorpg.client.screen.AdminScreen.handle(payload));
        event.register(Payloads.SyncPlayer.TYPE, (payload, context) -> ClientData.load(payload.data()));
        event.register(Payloads.Vitals.TYPE, (payload, context) -> {
            ClientData.hp = payload.hp();
            ClientData.maxHp = payload.maxHp();
            ClientData.mana = payload.mana();
            ClientData.maxMana = payload.maxMana();
        });
        event.register(Payloads.MobInfo.TYPE, (payload, context) -> ClientData.mobInfo = payload.data().copy());
        event.register(Payloads.Notify.TYPE, (payload, context) -> HudEffects.addBanner(payload));
        event.register(Payloads.AreaEntry.TYPE, (payload, context) -> com.mmorpg.client.AreaEntryClient.start(payload));
        event.register(Payloads.BossReveal.TYPE, (payload, context) -> com.mmorpg.client.BossRevealClient.start(payload));
        event.register(Payloads.Loading.TYPE, (payload, context) -> com.mmorpg.client.LoadingClient.handle(payload));
        event.register(Payloads.DeathInfo.TYPE, (payload, context) -> com.mmorpg.client.DeathClient.info(payload));
        event.register(Payloads.DungeonMusic.TYPE, (payload, context) -> com.mmorpg.client.DungeonMusicClient.set(payload.track()));
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
            case S2COpenScreen.LOOT_EDITOR -> {
                if (mc.gui.screen() instanceof com.mmorpg.client.screen.LootEditorScreen screen) screen.receive(payload.extra());
                else if (!payload.extra().contains("status")) mc.gui.setScreen(new com.mmorpg.client.screen.LootEditorScreen(payload.extra()));
            }
            case S2COpenScreen.CLASS_SELECT -> mc.gui.setScreen(new ClassSelectScreen());
            case S2COpenScreen.TELEPORTER -> mc.gui.setScreen(new TeleporterScreen(payload.extra()));
            case S2COpenScreen.TELEPORTER_SETUP -> mc.gui.setScreen(new TeleporterSetupScreen(payload.extra()));
            case S2COpenScreen.FORGE -> mc.gui.setScreen(new ForgeScreen(BlockPos.of(payload.extra().getLongOr("pos", 0))));
            case S2COpenScreen.BANK -> {
                if (mc.gui.screen() instanceof com.mmorpg.client.screen.BankScreen screen) screen.receive(payload.extra());
                else mc.gui.setScreen(new com.mmorpg.client.screen.BankScreen(payload.extra()));
            }
            case S2COpenScreen.AUCTION -> {
                if (mc.gui.screen() instanceof com.mmorpg.client.screen.AuctionScreen screen) screen.receive(payload.extra());
                else mc.gui.setScreen(new com.mmorpg.client.screen.AuctionScreen(payload.extra()));
            }
            case S2COpenScreen.COMPRESSOR -> mc.gui.setScreen(new com.mmorpg.client.screen.CompressorScreen(BlockPos.of(payload.extra().getLongOr("pos", 0))));
            case S2COpenScreen.QUEST_GIVER -> mc.gui.setScreen(new com.mmorpg.client.screen.QuestGiverScreen(payload.extra()));
            case S2COpenScreen.SHOP -> mc.gui.setScreen(new com.mmorpg.client.screen.ShopScreen(payload.extra()));
            case S2COpenScreen.DUNGEON_MASTER -> mc.gui.setScreen(new com.mmorpg.client.screen.DungeonMasterScreen(payload.extra()));
            case S2COpenScreen.MINE_MASTER -> mc.gui.setScreen(new com.mmorpg.client.screen.MineMasterScreen(payload.extra()));
            case S2COpenScreen.SPAWN_EDITOR -> mc.gui.setScreen(new com.mmorpg.client.screen.SpawnEditorScreen(payload.extra()));
            case S2COpenScreen.GUILD -> mc.gui.setScreen(new com.mmorpg.client.screen.GuildScreen(payload.extra()));
            case S2COpenScreen.ARENA -> mc.gui.setScreen(new com.mmorpg.client.screen.ArenaScreen(payload.extra()));
            case S2COpenScreen.ARENA_READY -> mc.gui.setScreen(new com.mmorpg.client.screen.ArenaReadyScreen(payload.extra()));
            case S2COpenScreen.ADVENTURE_SUMMARY -> mc.gui.setScreen(new com.mmorpg.client.screen.AdventureSummaryScreen(payload.extra()));
            case S2COpenScreen.MERCHANT_EDITOR -> mc.gui.setScreen(new com.mmorpg.client.screen.MerchantEditorScreen(payload.extra()));
            case S2COpenScreen.CLASS_MASTER -> mc.gui.setScreen(new com.mmorpg.client.screen.ClassMasterScreen(payload.extra()));
            case S2COpenScreen.TRIAL_STATE -> {
                ClientData.trialActive = payload.extra().getBooleanOr("active", false);
                ClientData.trialClass = payload.extra().getIntOr("class", 0);
                ClientData.trialLevel = payload.extra().getIntOr("level", 1);
                ClientData.trialSince = System.nanoTime();
            }
            case S2COpenScreen.CINEMATIC -> com.mmorpg.client.cinematic.CinematicClient.start(payload.extra());
            case S2COpenScreen.CRATE -> mc.gui.setScreen(new com.mmorpg.client.screen.CrateScreen(payload.extra()));
            case S2COpenScreen.LUCKY_BLOCK -> mc.gui.setScreen(new com.mmorpg.client.screen.LuckyBlockScreen(payload.extra()));
            case S2COpenScreen.LUCKY_RESULT -> {
                if (mc.gui.screen() instanceof com.mmorpg.client.screen.LuckyBlockScreen screen) screen.result(payload.extra());
            }
            default -> MMORPG.LOGGER.warn("Ecran MMORPG inconnu : {}", payload.screen());
        }
    }
}
