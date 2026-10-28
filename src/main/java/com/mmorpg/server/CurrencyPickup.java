package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/** Currency goes directly to the wallet; pickup delay, private loot and trial restrictions still apply. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class CurrencyPickup {
    public static boolean collect(ServerPlayer player,ItemEntity entity){
        if(!entity.isAlive()||entity.hasPickUpDelay()||!entity.getItem().is(ModItems.PIECE_OR.get())
                ||com.mmorpg.hub.ClassTrial.active(player)||player.isSpectator()
                ||entity.getTarget()!=null&&!entity.getTarget().equals(player.getUUID())
                ||entity.entityTags().stream().anyMatch(t->t.startsWith("aeloria_loot:")&&!t.equals(PetLoot.tag(player))))return false;
        int count=entity.getItem().getCount();if(count<=0)return false;
        RpgPlayers.addGold(player,count,true);AdventureSummary.item(player,ModItems.PIECE_OR.get(),count);
        player.take(entity,count);player.awardStat(net.minecraft.stats.Stats.ITEM_PICKED_UP.get(ModItems.PIECE_OR.get()),count);
        entity.discard();return true;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void pickup(ItemEntityPickupEvent.Pre event){
        if(!event.canPickup().isFalse()&&event.getPlayer() instanceof ServerPlayer player&&collect(player,event.getItemEntity()))event.setCanPickup(net.minecraft.util.TriState.FALSE);
    }
}
