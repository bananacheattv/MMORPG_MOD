package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Server-supplied names remain correct for players outside entity tracking range. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class PlayerListDisplay {
    @SubscribeEvent public static void name(PlayerEvent.TabListNameFormat e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        var d = RpgPlayers.get(p);
        e.setDisplayName(Component.literal("[" + d.level + "] ").withColor(0xFFD040)
            .append(Component.literal(d.playerClass.label + " ").withColor(d.playerClass.color))
            .append(p.getName().copy().withColor(0xFFFFFF)));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 100 == 0) refresh(p);
    }
    public static void refresh(ServerPlayer p) {
        p.refreshTabListName();
        var server = p.level().getServer();
        var d = RpgPlayers.get(p);
        int online = server.getPlayerList().getPlayerCount();
        var header = Component.literal("✦ ELDORIA ✦").withColor(0xFFD040)
            .append(Component.literal("\n" + online + " aventurier(s) en ligne").withColor(0xDDD5BF));
        int members = PartyManager.onlineMembers(p).size();
        var footer = Component.literal("Niveau " + d.level + " • " + d.playerClass.label).withColor(d.playerClass.color)
            .append(Component.literal("\n" + d.activeQuests.size() + " quête(s) • " + d.gold + " or • Groupe : " + members + " en ligne").withColor(0xDDD5BF));
        p.connection.send(new ClientboundTabListPacket(header, footer));
    }
}
