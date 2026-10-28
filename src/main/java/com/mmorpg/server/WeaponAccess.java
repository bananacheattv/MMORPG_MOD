package com.mmorpg.server;
import com.mmorpg.MMORPG;
import com.mmorpg.item.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
/** Server authority; storing and trading an incompatible weapon remains allowed. */
@net.neoforged.fml.common.EventBusSubscriber(modid=MMORPG.MODID)
public final class WeaponAccess {
 public static boolean allowed(ServerPlayer p,ItemStack stack,boolean notify){
  if(!(stack.getItem() instanceof RpgEquipment item)||item.rpgDef().slot()!=RpgItemDef.Slot.WEAPON)return true;
  var def=item.rpgDef();var d=RpgPlayers.get(p);boolean ok=def.usableBy(d.playerClass,d.level);
  if(!ok&&notify)p.sendOverlayMessage(Component.literal("Arme verrouillée · "+def.playerClass().label+" · niveau "+def.level()+" requis").withColor(0xEBA5C6));return ok;
 }
 @SubscribeEvent public static void attack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p&&!allowed(p,p.getMainHandItem(),true))e.setCanceled(true);}
 @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST) public static void damage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e){
  if(e.getSource().getEntity() instanceof ServerPlayer p){
   if(e.getSource().getDirectEntity()==p&&!e.getSource().is(com.mmorpg.combat.RpgCombat.SKILL_DAMAGE)&&!allowed(p,p.getMainHandItem(),true))e.setCanceled(true);
   if(e.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile){var required=projectile.getPersistentData().getStringOr("aeloria_weapon_class","");if(!required.isEmpty()&&!required.equals(RpgPlayers.get(p).playerClass.name()))e.setCanceled(true);}
  }
 }
}
