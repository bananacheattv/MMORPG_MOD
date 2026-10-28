package com.mmorpg.entity;
import com.mmorpg.MMORPG;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import java.util.*;

/** A crash-safe lifetime for engineer barricades; never removes a block edited into another material. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class WorldBarricades {
 public static final class Cell {public String dim;public int x,y,z;public long expires;}
 public static final class State {public List<Cell> cells=new ArrayList<>();}
 private static boolean pending=true;
 public static void place(ServerLevel level,List<BlockPos> positions){if(positions.isEmpty())return;
  com.mmorpg.network.SharedLedger.access(level.getServer(),"mob-barricades",State.class,State::new,s->{for(var p:positions){var c=new Cell();c.dim=level.dimension().identifier().toString();c.x=p.getX();c.y=p.getY();c.z=p.getZ();c.expires=System.currentTimeMillis()+6000;s.cells.add(c);}return null;});
  for(var p:positions)if(level.isEmptyBlock(p))level.setBlock(p,Blocks.OAK_FENCE.defaultBlockState(),2);pending=true;
 }
 private static void clean(net.minecraft.server.MinecraftServer server,boolean force){
  com.mmorpg.network.SharedLedger.access(server,"mob-barricades",State.class,State::new,s->{s.cells.removeIf(c->{if(!force&&System.currentTimeMillis()<c.expires)return false;var level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.parse(c.dim)));if(level==null)return false;var p=new BlockPos(c.x,c.y,c.z);if(level.getBlockState(p).is(Blocks.OAK_FENCE))level.removeBlock(p,false);return true;});pending=!s.cells.isEmpty();return null;});
 }
 @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e){if(pending&&e.getServer().getTickCount()%20==0&&com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS))clean(e.getServer(),false);}
 @SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent e){pending=true;}
 @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent e){if(pending&&com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS))clean(e.getServer(),true);}
}
