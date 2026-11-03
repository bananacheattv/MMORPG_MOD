package com.mmorpg.server;
@net.neoforged.fml.common.EventBusSubscriber(modid=com.mmorpg.MMORPG.MODID)
public final class DevMascotWalkChecks {
 @net.neoforged.bus.api.SubscribeEvent public static void run(net.neoforged.neoforge.event.server.ServerStartedEvent e) {
 if(!Boolean.getBoolean("mmorpg.mascotChecks"))return;
 var l=e.getServer().overworld();
 var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MascotChecks"));
 try {
 l.addNewPlayer(p);p.snapTo(12.5,280,.5,0,0);
 for(int x=-8;x<48;x++)for(int z=-8;z<9;z++) { l.setBlock(new net.minecraft.core.BlockPos(x,279,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3); for(int y=280;y<284;y++)l.removeBlock(new net.minecraft.core.BlockPos(x,y,z),false); }
 var m=com.mmorpg.registry.ModEntities.MASCOT.get().create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);m.initFor(p);m.snapTo(.5,280,.5,0,0);l.addFreshEntity(m);m.setOnGround(true);
 for(int i=0;i<160;i++){var previous=m.position();m.tickCount++;MascotGuide.follow(p,m);m.tick();if(m.position().distanceTo(previous)>.5)throw new AssertionError("Teleport during follow");}
 if(m.distanceToSqr(p)<9 || m.distanceToSqr(p)>26)throw new AssertionError("Follow distance: "+m.distanceToSqr(p));
 if(m.getX()<5)throw new AssertionError("Mascot did not walk: "+m.position());
 com.mmorpg.MMORPG.LOGGER.info("MASCOT WALK PASS");
 }catch(Throwable t){com.mmorpg.MMORPG.LOGGER.error("MASCOT WALK FAIL",t);}finally{e.getServer().halt(false);}
 }
}
