package com.mmorpg.dungeon;
import com.mmorpg.MMORPG;
import com.mmorpg.entity.*;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;
@EventBusSubscriber(modid=MMORPG.MODID)
public final class ExpeditionHazards {
 private record Mark(ServerLevel level,Vec3 pos,long at,long seed){}
 private static final Map<UUID,Mark> MARKS=new HashMap<>();
 @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e){
  var level=e.getServer().getLevel(InstanceManager.DIM);if(level==null)return;long now=level.getGameTime();
  for(var p:level.players()){
   if(now%100==0&&p.isAlive()&&InstanceManager.containsPlayer(p))p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,300,0,true,false,false));
   if(!p.isAlive()||p.isSpectator()||p.isCreative())continue;var slot=InstanceManager.slotAt(e.getServer(),level,p.getX(),p.getZ());if(slot==null||slot.owner.isEmpty()||!slot.ready||p.getZ()<slot.z+30)continue;
   int mod=DungeonVariants.modifier(slot.variantSeed);long cycle=Math.floorMod(now+slot.variantSeed%240,240);
   if(mod==1&&cycle==0){p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,40,0));level.addFreshEntity(SkillFxEntity.create(level,FxKind.AURA,p.position(),2,0xA0B0D0,40));p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Brume dense : déplacement ralenti brièvement."));}
   if(mod==2&&cycle==0&&!MARKS.containsKey(p.getUUID())){MARKS.put(p.getUUID(),new Mark(level,p.position(),now+40,slot.variantSeed));level.addFreshEntity(SkillFxEntity.create(level,FxKind.CIRCLE,p.position(),2,0xC080FF,40));p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("⚠ Surcharge arcanique : quittez le cercle !").withColor(0xD090FF));}
  }
  for(var id:new ArrayList<>(MARKS.keySet())){var mark=MARKS.get(id);if(now<mark.at())continue;MARKS.remove(id);var p=e.getServer().getPlayerList().getPlayer(id);if(p==null||p.level()!=mark.level()||!p.isAlive())continue;var slot=InstanceManager.slotAt(e.getServer(),p.level(),p.getX(),p.getZ());if(slot==null||slot.variantSeed!=mark.seed())continue;
   level.addFreshEntity(SkillFxEntity.create(level,FxKind.BURST,mark.pos(),2,0xC080FF,18));if(p.distanceToSqr(mark.pos())<4)p.hurtServer(level,p.damageSources().magic(),2);
  }
 }
 @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent e){MARKS.clear();}
}
