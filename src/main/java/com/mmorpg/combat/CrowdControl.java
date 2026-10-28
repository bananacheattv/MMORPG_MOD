package com.mmorpg.combat;
import com.mmorpg.MMORPG;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Three mob displacements in six seconds grant ten seconds of displacement immunity. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class CrowdControl {
 public static final class Window{
  long first=Long.MIN_VALUE/2,immune,last=Long.MIN_VALUE;int hits;boolean permitted;
  public boolean allow(long now){if(now==last)return permitted;last=now;if(now<immune)return permitted=false;if(now-first>120){first=now;hits=0;}if(++hits>=3){immune=now+200;hits=0;}return permitted=true;}
  public long remaining(long now){return Math.max(0,immune-now);}
 }
 private static final java.util.Map<java.util.UUID,Window> WINDOWS=new java.util.HashMap<>();
 public static boolean allow(LivingEntity target){
  if(!(target instanceof ServerPlayer p))return true;long now=p.level().getGameTime();var w=WINDOWS.computeIfAbsent(p.getUUID(),id->new Window());
  long before=w.remaining(now);boolean allowed=w.allow(now);if(before==0&&w.remaining(now)>0)p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Stabilité · Immunité aux projections : 10 s").withColor(0x80DFFF));return allowed;
 }
 @SubscribeEvent public static void knock(net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent e){if(e.getEntity() instanceof ServerPlayer p&&p.getLastHurtByMob()!=null&&!(p.getLastHurtByMob() instanceof net.minecraft.world.entity.player.Player)&&!allow(p))e.setCanceled(true);}
 @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){WINDOWS.remove(e.getEntity().getUUID());}
 @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent e){WINDOWS.clear();}
}
