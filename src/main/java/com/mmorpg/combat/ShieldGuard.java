package com.mmorpg.combat;
import com.mmorpg.MMORPG;
import com.mmorpg.server.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Frontal shield guard: finite reserve, hit-only tiered cost and full recovery after exhaustion. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class ShieldGuard {
 private static final class Window{long lastAction=Long.MIN_VALUE/2,brokenUntil;final java.util.Map<java.util.UUID,Long> charged=new java.util.HashMap<>();}
 private static final java.util.Map<java.util.UUID,Window> WINDOWS=new java.util.HashMap<>();
 private static Window window(ServerPlayer p){return WINDOWS.computeIfAbsent(p.getUUID(),id->new Window());}
 public static boolean canStart(ServerPlayer p){return FeatureFlags.enabled(p.level().getServer(),"bouclier")&&PaladinOffhand.equipped(p)&&WeaponAccess.allowed(p,p.getMainHandItem(),false)&&PaladinOffhand.shield(p.getOffhandItem())&&RpgPlayers.get(p).shieldGuard>0&&!RpgPlayers.get(p).shieldGuardRecovering;}
 public static boolean guarding(ServerPlayer p){return p.isUsingItem()&&p.getUsedItemHand()==InteractionHand.OFF_HAND&&PaladinOffhand.shield(p.getUseItem())&&PaladinOffhand.equipped(p);}
 public static int hitCost(int level){return new int[]{25,20,15,10,2}[level>=100?4:Math.clamp(level/25,0,3)];}
 public static int brokenTicks(ServerPlayer p){var d=RpgPlayers.get(p);return d.shieldGuardRecovering?(int)Math.ceil((100-d.shieldGuard)*2):0;}
 private static void broken(ServerPlayer p){var d=RpgPlayers.get(p);d.shieldGuardRecovering=true;d.dirty=true;p.stopUsingItem();p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.SHIELD_BREAK.value(),net.minecraft.sounds.SoundSource.PLAYERS,.8f,1);}
 public static double absorb(ServerPlayer p,DamageSource source,double damage){return absorb(p,source,damage,0);}
 /** A strike costs guard once per attacker per 10 ticks; aura/zone pulses under 3% of max health are blocked for free. */
 public static double absorb(ServerPlayer p,DamageSource source,double damage,double maxHealth){if(!guarding(p)||damage<=0||!(source.getEntity() instanceof net.minecraft.world.entity.LivingEntity))return damage;var origin=source.getSourcePosition();if(origin==null||origin.distanceToSqr(p.position())<.1)origin=source.getEntity().position();var direction=origin.subtract(p.position()).multiply(1,0,1).normalize();if(p.getLookAngle().multiply(1,0,1).normalize().dot(direction)<.2)return damage;
  var d=RpgPlayers.get(p);var w=window(p);long now=p.level().getGameTime();var id=source.getEntity().getUUID();Long last=w.charged.get(id);
  if(last!=null&&now-last<10||maxHealth>0&&damage<maxHealth*.03)return damage*.25;
  w.charged.put(id,now);if(w.charged.size()>32)w.charged.values().removeIf(t->now-t>=10);
  float cost=hitCost(d.level);float fraction=Math.min(1,d.shieldGuard/cost);d.shieldGuard=Math.max(0,d.shieldGuard-cost);d.dirty=true;window(p).lastAction=p.level().getGameTime();if(d.shieldGuard<=0)broken(p);else p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.SHIELD_BLOCK.value(),net.minecraft.sounds.SoundSource.PLAYERS,.6f,1);return damage*(1-.75*fraction);
 }
 public static void forgetHits(ServerPlayer p){window(p).charged.clear();}
 public static void update(ServerPlayer p){update(p,p.level().getGameTime());}
 public static void update(ServerPlayer p,long now){if(RpgPlayers.get(p).playerClass!=com.mmorpg.rpg.PlayerClass.TANK)return;var d=RpgPlayers.get(p);var w=window(p);float before=d.shieldGuard;
  if(d.shieldGuard<=0)d.shieldGuardRecovering=true;
  if(guarding(p)){if(!FeatureFlags.enabled(p.level().getServer(),"bouclier")||!WeaponAccess.allowed(p,p.getMainHandItem(),false)||d.shieldGuardRecovering){p.stopUsingItem();}}
  else if(now-w.lastAction>=40){d.shieldGuard=Math.min(100,d.shieldGuard+.5f);if(d.shieldGuard>=100)d.shieldGuardRecovering=false;}
  if(before!=d.shieldGuard)d.dirty=true;
 }
 @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre e){if(e.getEntity() instanceof ServerPlayer p)update(p);}
 @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){WINDOWS.remove(e.getEntity().getUUID());}
 @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent e){WINDOWS.clear();}
}
