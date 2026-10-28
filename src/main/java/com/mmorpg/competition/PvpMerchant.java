package com.mmorpg.competition;
import com.mmorpg.MMORPG;
import com.mmorpg.entity.NpcEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=MMORPG.MODID)
public final class PvpMerchant {
 public static final java.util.UUID ID=java.util.UUID.nameUUIDFromBytes("aeloria:pvp-merchant".getBytes(java.nio.charset.StandardCharsets.UTF_8));
 public static void ensure(ServerPlayer p){if(!p.level().dimension().equals(com.mmorpg.hub.SpawnBuilder.DIM)||!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.MAIN))return;var level=p.level();var directory=com.mmorpg.world.NpcDirectory.get(level.getServer());if(directory.entry(ID.toString())!=null||level.getEntity(ID)!=null)return;
  var spawn=com.mmorpg.hub.SpawnData.get(level.getServer());if(!spawn.exists())return;var around=BlockPos.containing(spawn.arrival());
  for(int r=6;r<=20;r+=2)for(int a=0;a<12;a++){int x=around.getX()+(int)Math.round(Math.cos(a*Math.PI/6)*r),z=around.getZ()+(int)Math.round(Math.sin(a*Math.PI/6)*r);for(int y=around.getY()+2;y>=around.getY()-2;y--){var at=new BlockPos(x,y,z);if(!level.hasChunkAt(at)||!level.isEmptyBlock(at)||!level.isEmptyBlock(at.above())||!level.getBlockState(at.below()).isSolidRender()||!level.getEntitiesOfClass(NpcEntity.class,new net.minecraft.world.phys.AABB(at).inflate(2)).isEmpty())continue;var npc=com.mmorpg.registry.ModEntities.NPC.get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);npc.setUUID(ID);npc.setup(NpcEntity.Role.PVP,"Intendant des arènes",9,"pvp_shop");npc.setZone("Spawn");npc.snapTo(x+.5,y,z+.5,180,0);level.addFreshEntity(npc);directory.record(npc);return;}}
 }
 @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p&&p.tickCount%100==0)ensure(p);}
}
