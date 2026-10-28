package com.mmorpg.dungeon;
import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import com.mmorpg.competition.CompetitionWorld;
import java.util.*;

/** Bounded visual fixtures in the private development save. Never enabled in production. */
@net.neoforged.fml.common.EventBusSubscriber(modid=com.mmorpg.MMORPG.MODID)
public final class DevExpansionVisual {
 private static final List<Runnable> STAGES=new ArrayList<>();
 public static void install(ServerPlayer p){
  if(!Boolean.getBoolean("mmorpg.expansionVisual"))throw new IllegalStateException("Private preview only");
  var server=p.level().getServer();var level=server.getLevel(InstanceManager.DIM);String pool=InstanceManager.pool(com.mmorpg.world.DungeonBuilder.DungeonType.CRYPTE_GOBELINE,0);
  InstanceData.get(server).slots.removeIf(s->s.index==9998);var slot=new InstanceData.Slot(pool,9998,12000,101,10000,p.getUUID().toString(),server.getTickCount(),true);slot.variantSeed=25;InstanceData.get(server).slots.add(slot);var o=new BlockPos(slot.x,slot.y,slot.z);
  var ground=new CompetitionWorld.Job();for(int x=-8;x<=32;x++)for(int z=120;z<=165;z++)ground.add(o.getX()+x,100,o.getZ()+z,Blocks.POLISHED_DEEPSLATE);
  var center=o.offset(0,-1,Layout.ARENA.z());for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)for(int y=-4;y<=0;y++)ground.add(center.getX()+x,center.getY()+y,center.getZ()+z,Math.abs(x)==16||Math.abs(z)==16?Blocks.AMETHYST_BLOCK:Blocks.DEEPSLATE_TILES);
  var guild=new com.mmorpg.guild.GuildStore.Guild();guild.hallIndex=60;guild.hallLevel=3;guild.banner="VIOLET";guild.bannerPattern="ARBRE";guild.bosses=31;var hall=com.mmorpg.guild.GuildHall.build(guild);var hallLevel=server.getLevel(com.mmorpg.guild.GuildHall.DIM);
  STAGES.add(()->{ground.tick(level);if(!ground.done())return;STAGES.removeFirst();DungeonVariants.decorate(level,o,25);});
  STAGES.add(()->{hall.tick(hallLevel);if(hall.done()){com.mmorpg.guild.GuildHall.labels(hallLevel,guild);STAGES.removeFirst();}});
  p.teleportTo(level,12008,102,10131,Set.of(),-90,0,true);p.getAbilities().flying=true;p.onUpdateAbilities();
 }
 @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e){if(Boolean.getBoolean("mmorpg.expansionVisual")&&!STAGES.isEmpty())STAGES.getFirst().run();}
 @net.neoforged.bus.api.SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent e){STAGES.clear();}
}
