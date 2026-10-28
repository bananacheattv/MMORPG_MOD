package com.mmorpg.client;
@net.neoforged.fml.common.EventBusSubscriber(modid=com.mmorpg.MMORPG.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class RankNames {
 @net.neoforged.bus.api.SubscribeEvent public static void name(net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender e){
  if(e.getEntity() instanceof net.minecraft.world.entity.player.Player p){var d=p.getData(com.mmorpg.registry.ModAttachments.PUBLIC);e.setContent(net.minecraft.network.chat.Component.literal("["+d.rankLabel+"] ").withColor(d.rankColor).append(p.getGameProfile().name()));}
 }
}
