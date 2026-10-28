package com.mmorpg.combat;
import com.mmorpg.MMORPG;
import com.mmorpg.server.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Frontal shield guard: finite reserve, continuous drain and delayed regeneration while lowered. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class ShieldGuard {
 private static final class Window{long lastAction=Long.MIN_VALUE/2,brokenUntil;}
 private static final java.util.Map<java.util.UUID,Window> WINDOWS=new java.util.HashMap<>();
 private static Window window(ServerPlayer p){return WINDOWS.computeIfAbsent(p.getUUID(),id->new Window());}
 public static boolean canStart(ServerPlayer p){return FeatureFlags.enabled(p.level().getServer(),"bouclier")&&PaladinOffhand.equipped(p)&&WeaponAccess.allowed(p,p.getMainHandItem(),false)&&PaladinOffhand.shield(p.getOffhandItem())&&RpgPlayers.get(p).shieldGuard>=20&&p.level().getGameTime()>=window(p).brokenUntil;}
 public static boolean guarding(ServerPlayer p){return p.isUsingItem()&&p.getUsedItemHand()==InteractionHand.OFF_HAND&&PaladinOffhand.shield(p.getUseItem())&&PaladinOffhand.equipped(p);}
 public static int brokenTicks(ServerPlayer p){return (int)Math.max(0,window(p).brokenUntil-p.level().getGameTime());}
 private static void broken(ServerPlayer p){var w=window(p);w.brokenUntil=p.level().getGameTime()+100;p.stopUsingItem();p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("GARDE BRISÉE · Baissez le bouclier pour récupérer").withColor(0xFF7777));p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.SHIELD_BREAK.value(),net.minecraft.sounds.SoundSource.PLAYERS,.8f,1);}
 public static boolean spend(ServerPlayer p,float amount){var d=RpgPlayers.get(p);if(d.shieldGuard<amount||brokenTicks(p)>0)return false;d.shieldGuard-=amount;window(p).lastAction=p.level().getGameTime();d.dirty=true;if(d.shieldGuard<=0)broken(p);return true;}
 public static double absorb(ServerPlayer p,DamageSource source,double damage){if(!guarding(p)||damage<=0||!(source.getEntity() instanceof net.minecraft.world.entity.LivingEntity))return damage;var origin=source.getSourcePosition();if(origin==null||origin.distanceToSqr(p.position())<.1)origin=source.getEntity().position();var direction=origin.subtract(p.position()).multiply(1,0,1).normalize();if(p.getLookAngle().multiply(1,0,1).normalize().dot(direction)<.2)return damage;
  var d=RpgPlayers.get(p);float cost=(float)(12+damage/Math.max(1,d.stats.get(com.mmorpg.rpg.Stat.MAX_HP))*100);float fraction=Math.min(1,d.shieldGuard/cost);d.shieldGuard=Math.max(0,d.shieldGuard-cost);d.dirty=true;window(p).lastAction=p.level().getGameTime();if(d.shieldGuard<=0)broken(p);else p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.SHIELD_BLOCK.value(),net.minecraft.sounds.SoundSource.PLAYERS,.6f,1);return damage*(1-.75*fraction);
 }
 @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre e){if(!(e.getEntity() instanceof ServerPlayer p)||RpgPlayers.get(p).playerClass!=com.mmorpg.rpg.PlayerClass.TANK)return;var d=RpgPlayers.get(p);var w=window(p);long now=p.level().getGameTime();float before=d.shieldGuard;
  if(guarding(p)){if(!FeatureFlags.enabled(p.level().getServer(),"bouclier")){p.stopUsingItem();return;}if(!PaladinOffhand.equipped(p)||!WeaponAccess.allowed(p,p.getMainHandItem(),false)||brokenTicks(p)>0){p.stopUsingItem();return;}d.shieldGuard=Math.max(0,d.shieldGuard-.5f);w.lastAction=now;if(d.shieldGuard<=0)broken(p);}
  else if(now-w.lastAction>=40)d.shieldGuard=Math.min(100,d.shieldGuard+.5f);
  if(before!=d.shieldGuard)d.dirty=true;
 }
 @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){WINDOWS.remove(e.getEntity().getUUID());}
 @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent e){WINDOWS.clear();}
}
