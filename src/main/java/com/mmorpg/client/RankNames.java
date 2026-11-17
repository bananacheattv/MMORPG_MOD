package com.mmorpg.client;
@net.neoforged.fml.common.EventBusSubscriber(modid=com.mmorpg.MMORPG.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class RankNames {
 @net.neoforged.bus.api.SubscribeEvent public static void name(net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender e){
  if(e.getEntity() instanceof net.minecraft.world.entity.player.Player p){var d=p.getData(com.mmorpg.registry.ModAttachments.PUBLIC);var name=com.mmorpg.store.RankTags.name(d.rankLabel,d.rankColor,p.getGameProfile().name(),false).copy();if(!d.pvpTitle.isEmpty())name.append(net.minecraft.network.chat.Component.literal(" · "+d.pvpTitle).withColor(0xE8C36A));e.setContent(name);}
 }
 /** Releve le pseudo des joueurs au-dessus de la tete (casque, cosmetiques de coiffe 3D, mascotte). */
 @net.neoforged.bus.api.SubscribeEvent public static void lift(net.neoforged.neoforge.client.event.RenderNameTagEvent.DoRender e){
  if(e.getEntityRenderState() instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState)e.getPoseStack().translate(0.0F,0.45F,0.0F);
 }
}
