package com.mmorpg.competition;
import com.mmorpg.item.*;
import net.minecraft.server.level.ServerPlayer;
public final class PvpEquipment {
 public static boolean item(net.minecraft.world.item.ItemStack s){return s.getItem() instanceof RpgEquipment e&&e.rpgDef().id().startsWith("pvp_")||net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().startsWith("pvp_");}
 public static boolean usable(ServerPlayer p,net.minecraft.world.item.ItemStack stack){var d=com.mmorpg.server.RpgPlayers.get(p);return item(stack)&&stack.getItem() instanceof RpgEquipment e&&e.rpgDef().usableBy(d.playerClass,d.level);}
 public static int pieces(ServerPlayer p){int n=0;for(var slot:new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET})if(usable(p,p.getItemBySlot(slot)))n++;return n;}
 public static double outgoing(ServerPlayer p,boolean pvp){int n=pieces(p);boolean weapon=usable(p,p.getMainHandItem());return pvp?1+n*.03+(weapon?.12:0):1-n*.025-(weapon?.15:0);}
 public static double incoming(ServerPlayer p,boolean pvp){int n=pieces(p);return pvp?1-n*.05:1+n*.025;}
}
