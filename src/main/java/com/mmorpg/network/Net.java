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
        event.registrar("1").optional().playToClient(ProxyConnect.TYPE, ProxyConnect.CODEC);
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(S2COpenScreen.TYPE, S2COpenScreen.CODEC);
        r.playToClient(Payloads.Guidance.TYPE,Payloads.Guidance.CODEC);
        r.playToClient(Payloads.SyncPlayer.TYPE, Payloads.SyncPlayer.CODEC);
        r.playToClient(Payloads.Vitals.TYPE, Payloads.Vitals.CODEC);
        r.playToClient(Payloads.FishFightState.TYPE, Payloads.FishFightState.CODEC);
        r.playToClient(Payloads.CombatText.TYPE, Payloads.CombatText.CODEC);
        r.playToClient(Payloads.CastAnimation.TYPE, Payloads.CastAnimation.CODEC);
        r.playToClient(Payloads.Notify.TYPE, Payloads.Notify.CODEC);
        r.playToClient(Payloads.AreaEntry.TYPE, Payloads.AreaEntry.CODEC);
        r.playToClient(Payloads.BossReveal.TYPE, Payloads.BossReveal.CODEC);
        r.playToClient(Payloads.Loading.TYPE, Payloads.Loading.CODEC);
        r.playToClient(Payloads.DeathInfo.TYPE, Payloads.DeathInfo.CODEC);
        r.playToClient(Payloads.DungeonMusic.TYPE, Payloads.DungeonMusic.CODEC);
        srv(r, Payloads.DungeonEnter.TYPE, Payloads.DungeonEnter.CODEC, (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) com.mmorpg.dungeon.DungeonMaster.enter(p, packet);
        });
        srv(r, Payloads.MineEnter.TYPE, Payloads.MineEnter.CODEC, (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) com.mmorpg.mine.MineManager.enter(p, packet);
        });
        r.playToClient(Payloads.MobInfo.TYPE, Payloads.MobInfo.CODEC);
        r.playToClient(Payloads.PartySync.TYPE, Payloads.PartySync.CODEC);

        srv(r, Payloads.SelectClass.TYPE, Payloads.SelectClass.CODEC, ServerHandlers::selectClass);
        srv(r, Payloads.QuestEdit.TYPE, Payloads.QuestEdit.CODEC, (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) com.mmorpg.server.QuestEditor.action(p, packet.data());
        });
        srv(r, Payloads.LootEdit.TYPE, Payloads.LootEdit.CODEC, (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) com.mmorpg.server.LootEditor.action(p, packet.data());
        });
        srv(r, Payloads.HubAction.TYPE, Payloads.HubAction.CODEC, (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) com.mmorpg.hub.HubManager.action(p, packet.action(), packet.a(), packet.b());
        });
        srv(r, Payloads.Allocate.TYPE, Payloads.Allocate.CODEC, ServerHandlers::allocate);
        srv(r, Payloads.SkillAction.TYPE, Payloads.SkillAction.CODEC, ServerHandlers::skillAction);
        srv(r, Payloads.MountAction.TYPE, Payloads.MountAction.CODEC, ServerHandlers::mountAction);
        srv(r, Payloads.LuckyRoll.TYPE, Payloads.LuckyRoll.CODEC, (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) com.mmorpg.server.LuckyBlockManager.roll(p, packet.pos());
        });
        srv(r, Payloads.CrateOpen.TYPE, Payloads.CrateOpen.CODEC, (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) com.mmorpg.server.CrateManager.open(p, packet.pos());
        });
        srv(r, Payloads.PetAction.TYPE, Payloads.PetAction.CODEC, ServerHandlers::petAction);
        srv(r, Payloads.CosmeticAction.TYPE, Payloads.CosmeticAction.CODEC, ServerHandlers::cosmeticAction);
        srv(r, Payloads.Teleport.TYPE, Payloads.Teleport.CODEC, ServerHandlers::teleport);
        srv(r, Payloads.TeleporterSetup.TYPE, Payloads.TeleporterSetup.CODEC, ServerHandlers::teleporterSetup);
        srv(r, Payloads.ForgeAction.TYPE, Payloads.ForgeAction.CODEC, ServerHandlers::forgeAction);
        srv(r, Payloads.AdminRequest.TYPE, Payloads.AdminRequest.CODEC, (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) com.mmorpg.admin.Admin.handle(p, packet);
        });
        r.playToClient(Payloads.AdminReply.TYPE, Payloads.AdminReply.CODEC);
        srv(r, Payloads.ServiceAction.TYPE, Payloads.ServiceAction.CODEC, ServerHandlers::serviceAction);
        srv(r, Payloads.QuestAction.TYPE, Payloads.QuestAction.CODEC, ServerHandlers::questAction);
        srv(r, Payloads.ShopAction.TYPE, Payloads.ShopAction.CODEC, ServerHandlers::shopAction);
        srv(r, Payloads.MerchantEdit.TYPE,Payloads.MerchantEdit.CODEC,(packet,ctx)->{if(ctx.player() instanceof ServerPlayer p)com.mmorpg.server.MerchantEditor.action(p,packet.data());});
        srv(r, Payloads.SocialAction.TYPE,Payloads.SocialAction.CODEC,(packet,ctx)->{if(ctx.player() instanceof ServerPlayer p){if(packet.system().equals("tuto"))com.mmorpg.server.MascotGuide.screen(p,packet.data().getStringOr("screen",""));else if(packet.system().equals("pvp"))com.mmorpg.competition.PvpProgression.action(p,packet.data());else if(packet.system().equals("jobs"))com.mmorpg.job.JobSystem.action(p,packet.data());else if(packet.system().equals("backpack"))com.mmorpg.server.Backpacks.open(p);else if(packet.system().equals("store"))com.mmorpg.store.PremiumStore.action(p,packet.data());else if(packet.system().equals("achievements"))com.mmorpg.achievement.Achievements.action(p,packet.data());else if(packet.system().equals("guild"))com.mmorpg.guild.GuildService.action(p,packet.data());else if(packet.system().equals("armorstand"))com.mmorpg.server.ArmorStandEditor.action(p,packet.data());else if(packet.system().equals("arena"))com.mmorpg.competition.ArenaManager.action(p,packet.data());else if(packet.system().equals("trade"))com.mmorpg.economy.TradeManager.action(p,packet.data());else if(packet.system().equals("playeradmin"))com.mmorpg.admin.PlayerAdmin.action(p,packet.data());else if(packet.system().equals("features"))com.mmorpg.admin.FeatureAdmin.action(p,packet.data());}});
        srv(r, Payloads.PartyAction.TYPE, Payloads.PartyAction.CODEC, ServerHandlers::partyAction);
    }

    private static <T extends CustomPacketPayload> void srv(PayloadRegistrar r, CustomPacketPayload.Type<T> type,
            net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, T> codec,
            net.neoforged.neoforge.network.handling.IPayloadHandler<T> handler) {
        r.playToServer(type, codec, com.mmorpg.anticheat.AntiCheat.gate(type, handler));
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
