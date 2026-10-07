package com.mmorpg.network;

import com.mmorpg.server.ServerHandlers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Enregistrement des paquets et aides d'envoi. Les gestionnaires client sont enregistres cote client. */
public final class Net {
    private Net() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(S2COpenScreen.TYPE, S2COpenScreen.CODEC);
        r.playToClient(Payloads.SyncPlayer.TYPE, Payloads.SyncPlayer.CODEC);
        r.playToClient(Payloads.Vitals.TYPE, Payloads.Vitals.CODEC);
        r.playToClient(Payloads.CombatText.TYPE, Payloads.CombatText.CODEC);
        r.playToClient(Payloads.CastAnimation.TYPE, Payloads.CastAnimation.CODEC);
        r.playToClient(Payloads.Notify.TYPE, Payloads.Notify.CODEC);
        r.playToClient(Payloads.MobInfo.TYPE, Payloads.MobInfo.CODEC);
        r.playToClient(Payloads.PartySync.TYPE, Payloads.PartySync.CODEC);

        r.playToServer(Payloads.SelectClass.TYPE, Payloads.SelectClass.CODEC, ServerHandlers::selectClass);
        r.playToServer(Payloads.QuestEdit.TYPE, Payloads.QuestEdit.CODEC, (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) com.mmorpg.server.QuestEditor.action(p, packet.data());
        });
        r.playToServer(Payloads.LootEdit.TYPE, Payloads.LootEdit.CODEC, (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) com.mmorpg.server.LootEditor.action(p, packet.data());
        });
        r.playToServer(Payloads.Allocate.TYPE, Payloads.Allocate.CODEC, ServerHandlers::allocate);
        r.playToServer(Payloads.SkillAction.TYPE, Payloads.SkillAction.CODEC, ServerHandlers::skillAction);
        r.playToServer(Payloads.MountAction.TYPE, Payloads.MountAction.CODEC, ServerHandlers::mountAction);
        r.playToServer(Payloads.LuckyRoll.TYPE, Payloads.LuckyRoll.CODEC, (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) com.mmorpg.server.LuckyBlockManager.roll(p, packet.pos());
        });
        r.playToServer(Payloads.CrateOpen.TYPE, Payloads.CrateOpen.CODEC, (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) com.mmorpg.server.CrateManager.open(p, packet.pos());
        });
        r.playToServer(Payloads.PetAction.TYPE, Payloads.PetAction.CODEC, ServerHandlers::petAction);
        r.playToServer(Payloads.CosmeticAction.TYPE, Payloads.CosmeticAction.CODEC, ServerHandlers::cosmeticAction);
        r.playToServer(Payloads.Teleport.TYPE, Payloads.Teleport.CODEC, ServerHandlers::teleport);
        r.playToServer(Payloads.TeleporterSetup.TYPE, Payloads.TeleporterSetup.CODEC, ServerHandlers::teleporterSetup);
        r.playToServer(Payloads.ForgeAction.TYPE, Payloads.ForgeAction.CODEC, ServerHandlers::forgeAction);
        r.playToServer(Payloads.QuestAction.TYPE, Payloads.QuestAction.CODEC, ServerHandlers::questAction);
        r.playToServer(Payloads.ShopAction.TYPE, Payloads.ShopAction.CODEC, ServerHandlers::shopAction);
        r.playToServer(Payloads.PartyAction.TYPE, Payloads.PartyAction.CODEC, ServerHandlers::partyAction);
    }

    public static void toPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void toTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }

    public static void toAll(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }
}
